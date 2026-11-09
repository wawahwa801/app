package app.speedbump.core

enum class Decision { NONE, SHOW_WALL, LEAVE_TO_HOME }

/** Saved between service restarts, so Android rebinding the service can't interrupt a sent reel. */
data class PolicyState(
    val cameFromDms: Boolean = false,
    val watchingSentItem: Boolean = false,
    val baselineKey: String? = null,
    val baselineIndex: Int? = null,
    val travelPx: Long = 0,
    val gaveUpAt: Long? = null,
)

/**
 * Decides when a wall appears, per app. On Instagram, a reel opened from a chat
 * is allowed until the user moves on from it to any other reel.
 */
class SessionPolicy(
    private val allowSentItems: Boolean,
    private val advanceThresholdPx: Int,
    initial: PolicyState = PolicyState(),
) {
    private var cameFromDms = initial.cameFromDms
    private var watchingSentItem = initial.watchingSentItem
    private var baselineKey = initial.baselineKey
    private var baselineIndex = initial.baselineIndex
    private var travelPx = initial.travelPx
    private var gaveUpAt = initial.gaveUpAt

    val state: PolicyState
        get() = PolicyState(cameFromDms, watchingSentItem, baselineKey, baselineIndex, travelPx, gaveUpAt)

    fun observe(detection: Detection, enabled: Boolean, unlocked: Boolean, nowMs: Long): Decision {
        when (detection.screen) {
            Screen.DIRECT_MESSAGES -> {
                cameFromDms = true
                endSentItem()
                return Decision.NONE
            }
            Screen.OTHER -> {
                // Sheets and dialogs over a sent reel also land here, so only app navigation
                // ends the sent-reel flow.
                if (detection.mainNavVisible) {
                    cameFromDms = false
                    endSentItem()
                }
                return Decision.NONE
            }
            Screen.SHORT_FORM_VIEWER -> Unit
        }
        if (isAllowedSentItem(detection)) return Decision.NONE
        if (!enabled || unlocked) return Decision.NONE
        val gaveUp = gaveUpAt
        return if (gaveUp != null && nowMs - gaveUp < GIVE_UP_WINDOW_MS) Decision.LEAVE_TO_HOME else Decision.SHOW_WALL
    }

    /** Records movement in the viewer's pager, from an accessibility scroll event. */
    fun onViewerScrolled(deltaY: Int, fromIndex: Int) {
        if (!watchingSentItem) return
        if (fromIndex >= 0) {
            val baseline = baselineIndex
            if (baseline == null) baselineIndex = fromIndex else if (baseline != fromIndex) return endSentItem()
        }
        // Events without scroll data report -1 or 0.
        if (kotlin.math.abs(deltaY) > 1) travelPx += kotlin.math.abs(deltaY)
        if (travelPx >= advanceThresholdPx) endSentItem()
    }

    fun onGaveUp(nowMs: Long) {
        gaveUpAt = nowMs
    }

    private fun isAllowedSentItem(detection: Detection): Boolean {
        if (!allowSentItems || detection.feedTabSelected) {
            cameFromDms = false
            endSentItem()
            return false
        }
        if (!watchingSentItem) {
            if (!cameFromDms) return false
            cameFromDms = false
            watchingSentItem = true
            baselineKey = detection.contentKey
            return true
        }
        val key = detection.contentKey
        if (key != null) {
            if (baselineKey == null) {
                baselineKey = key
            } else if (baselineKey != key) {
                endSentItem()
                return false
            }
        }
        return true
    }

    private fun endSentItem() {
        watchingSentItem = false
        baselineKey = null
        baselineIndex = null
        travelPx = 0
    }

    private companion object {
        const val GIVE_UP_WINDOW_MS = 1_500L
    }
}
