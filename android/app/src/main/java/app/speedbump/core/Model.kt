package app.speedbump.core

/** The apps Speedbump acts on. No other package's content is ever read. */
enum class TargetApp(val packageName: String, val sectionName: String) {
    INSTAGRAM("com.instagram.android", "Instagram Reels"),
    YOUTUBE("com.google.android.youtube", "YouTube Shorts"),
    WECHAT("com.tencent.mm", "WeChat Channels");

    companion object {
        fun fromPackage(name: CharSequence?): TargetApp? = entries.firstOrNull { it.packageName == name?.toString() }
    }
}

enum class FrictionMode { WAIT_ONE_MINUTE, AIME_PROBLEM }

enum class MetricKind { LIKES, VIEWS, FOLLOWERS }

data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val area: Long get() = (right - left).coerceAtLeast(0).toLong() * (bottom - top).coerceAtLeast(0)
}

/** A plain copy of an accessibility node, so all decisions can be unit tested off-device. */
data class UiNode(
    val id: String? = null,
    val className: String? = null,
    val text: String? = null,
    val desc: String? = null,
    val selected: Boolean = false,
    val visible: Boolean = true,
    val clickable: Boolean = false,
    val bounds: Box = Box(0, 0, 0, 0),
    val children: List<UiNode> = emptyList(),
) {
    fun walkVisible(): Sequence<UiNode> = sequence {
        if (visible) {
            yield(this@UiNode)
            children.forEach { yieldAll(it.walkVisible()) }
        }
    }
}

enum class Screen { SHORT_FORM_VIEWER, DIRECT_MESSAGES, OTHER }

data class Detection(
    val screen: Screen,
    /** The feed tab itself is open, so this can't be a reel opened from a chat. */
    val feedTabSelected: Boolean = false,
    /** Primary app navigation is showing, so the user has left any sent-reel flow. */
    val mainNavVisible: Boolean = false,
    /** A hash of the on-screen item's author, when the app exposes one. Never the name itself. */
    val contentKey: String? = null,
)
