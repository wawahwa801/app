package app.speedbump.service

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.speedbump.core.Box
import app.speedbump.core.Decision
import app.speedbump.core.Detector
import app.speedbump.core.MetricMatcher
import app.speedbump.core.Screen
import app.speedbump.core.SessionPolicy
import app.speedbump.core.TargetApp
import app.speedbump.core.UiNode
import app.speedbump.ui.SettingsStore
import app.speedbump.ui.WallActivity

class SpeedbumpService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val policies = mutableMapOf<TargetApp, SessionPolicy>()
    private val activityClasses = mutableMapOf<TargetApp, String>()
    private lateinit var store: SettingsStore
    private lateinit var overlay: MetricOverlay

    private val evaluateRunnable = Runnable { evaluate() }
    private val watchForegroundRunnable = object : Runnable {
        override fun run() {
            // The service only gets events from target apps, so leaving one is noticed by polling.
            if (!overlay.isShowing) return
            if (TargetApp.fromPackage(rootInActiveWindow?.packageName) == null) {
                overlay.clear()
            } else {
                handler.postDelayed(this, FOREGROUND_POLL_MS)
            }
        }
    }

    override fun onServiceConnected() {
        store = SettingsStore(this)
        overlay = MetricOverlay(this)
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val app = TargetApp.fromPackage(event.packageName) ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val name = event.className?.toString().orEmpty()
                // Sheets and toasts also raise this event, and only activities change the screen.
                if (name.startsWith(app.packageName) && (name.endsWith("UI") || name.endsWith("Activity"))) {
                    activityClasses[app] = name
                }
                scheduleEvaluate(0)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val source = event.source
                val sourceId = source?.viewIdResourceName?.substringAfter(":id/")
                if (Detector.isViewerPager(app, sourceId, event.className?.toString())) {
                    policy(app).onViewerScrolled(event.scrollDeltaY, event.fromIndex)
                    savePolicy(app)
                    scheduleEvaluate(0)
                } else {
                    overlay.clear()
                    scheduleEvaluate(SCROLL_SETTLE_MS)
                }
            }
            else -> scheduleEvaluate(CONTENT_DEBOUNCE_MS)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        overlay.clear()
        super.onDestroy()
    }

    fun onWallUnlocked() {
        val settings = store.load()
        handler.postDelayed(evaluateRunnable, settings.unlockMinutes * 60_000L + 200)
    }

    fun onWallGaveUp(app: TargetApp) {
        handler.postDelayed({
            policy(app).onGaveUp(System.currentTimeMillis())
            savePolicy(app)
            performGlobalAction(GLOBAL_ACTION_BACK)
        }, WALL_CLOSE_MS)
    }

    private fun scheduleEvaluate(delayMs: Long) {
        handler.removeCallbacks(evaluateRunnable)
        handler.postDelayed(evaluateRunnable, delayMs)
    }

    private fun policy(app: TargetApp) = policies.getOrPut(app) {
        SessionPolicy(
            allowSentItems = app == TargetApp.INSTAGRAM,
            advanceThresholdPx = resources.displayMetrics.heightPixels / 4,
            initial = store.loadPolicyState(app),
        )
    }

    private fun savePolicy(app: TargetApp) = store.savePolicyState(app, policy(app).state)

    private fun evaluate() {
        if (WallActivity.isShowing) return
        val rootInfo = rootInActiveWindow ?: return overlay.clear()
        val app = TargetApp.fromPackage(rootInfo.packageName) ?: return overlay.clear()
        val root = rootInfo.snapshot(maxDepth = 60)
        val detection = Detector.detect(app, root, activityClasses[app])
        val settings = store.load()
        val now = System.currentTimeMillis()

        val decision = policy(app).observe(detection, app in settings.blocked, store.isUnlocked(app, now), now)
        savePolicy(app)
        when (decision) {
            Decision.SHOW_WALL -> {
                overlay.clear()
                startActivity(WallActivity.intent(this, app))
                return
            }
            Decision.LEAVE_TO_HOME -> {
                overlay.clear()
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }
            Decision.NONE -> Unit
        }

        // Metrics are never masked in chats, where a message could read "5 likes".
        if (app == TargetApp.INSTAGRAM && detection.screen != Screen.DIRECT_MESSAGES && settings.hiddenMetrics.isNotEmpty()) {
            overlay.show(MetricMatcher.find(root, settings.hiddenMetrics).map { it.bounds })
            if (overlay.isShowing) {
                handler.removeCallbacks(watchForegroundRunnable)
                handler.postDelayed(watchForegroundRunnable, FOREGROUND_POLL_MS)
            }
        } else {
            overlay.clear()
        }
    }

    companion object {
        private const val CONTENT_DEBOUNCE_MS = 120L
        private const val SCROLL_SETTLE_MS = 90L
        private const val FOREGROUND_POLL_MS = 500L
        private const val WALL_CLOSE_MS = 250L

        var instance: SpeedbumpService? = null
            private set
    }
}

private fun AccessibilityNodeInfo.snapshot(maxDepth: Int): UiNode {
    val rect = Rect().also(::getBoundsInScreen)
    val kids = if (maxDepth == 0) emptyList() else (0 until childCount).mapNotNull { getChild(it)?.snapshot(maxDepth - 1) }
    return UiNode(
        id = viewIdResourceName?.substringAfter(":id/"),
        className = className?.toString(),
        text = text?.toString(),
        desc = contentDescription?.toString(),
        selected = isSelected,
        visible = isVisibleToUser,
        clickable = isClickable,
        bounds = Box(rect.left, rect.top, rect.right, rect.bottom),
        children = kids,
    )
}

/** Paints over counts. Its window ignores all touches, so nothing in Instagram stops working. */
private class MetricOverlay(private val service: AccessibilityService) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var view: MaskView? = null

    val isShowing: Boolean get() = view != null

    fun show(boxes: List<Box>) {
        if (boxes.isEmpty()) return clear()
        val existing = view
        if (existing != null) {
            existing.boxes = boxes
            existing.invalidate()
            return
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        val created = MaskView(service).also { it.boxes = boxes }
        windowManager.addView(created, params)
        view = created
    }

    fun clear() {
        view?.let(windowManager::removeView)
        view = null
    }
}

private class MaskView(service: AccessibilityService) : View(service) {
    var boxes: List<Box> = emptyList()
    private val location = IntArray(2)
    private val paint = Paint().apply {
        val night = service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        color = if (night == Configuration.UI_MODE_NIGHT_YES) Color.BLACK else Color.WHITE
    }

    override fun onDraw(canvas: Canvas) {
        getLocationOnScreen(location)
        boxes.forEach {
            canvas.drawRect(
                (it.left - location[0]).toFloat(),
                (it.top - location[1]).toFloat(),
                (it.right - location[0]).toFloat(),
                (it.bottom - location[1]).toFloat(),
                paint,
            )
        }
    }
}
