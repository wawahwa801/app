package app.speedbump.core

/**
 * Works out what's on screen. Apps rename view ids often, so rules match id
 * fragments and only count a "clips"/"reel" view if it fills most of the screen,
 * which keeps thumbnails and shelves in ordinary feeds from triggering a block.
 */
object Detector {
    private const val FULL_SCREEN_FRACTION = 0.6

    fun detect(app: TargetApp, root: UiNode, activityClass: String?): Detection = when (app) {
        TargetApp.INSTAGRAM -> instagram(root)
        TargetApp.YOUTUBE -> youtube(root)
        TargetApp.WECHAT -> wechat(activityClass)
    }

    /** True only for the pager that moves between reels, never for a comment or share list. */
    fun isViewerPager(app: TargetApp, sourceId: String?, className: String?): Boolean {
        val id = sourceId.orEmpty()
        return when (app) {
            TargetApp.INSTAGRAM -> "clips_viewer" in id || "clips" in id && "pager" in id ||
                (id.isEmpty() && className.orEmpty().contains("ViewPager"))
            else -> false
        }
    }

    private fun instagram(root: UiNode): Detection {
        val nodes = root.walkVisible().toList()
        val fullScreen = root.bounds.area * FULL_SCREEN_FRACTION
        val tabSelected = nodes.any { n ->
            n.selected && (n.id.orEmpty().let { "clips_tab" in it || "tab_clips" in it } || n.desc == "Reels")
        }
        val viewer = nodes.any { n ->
            val id = n.id.orEmpty()
            "clips_viewer" in id || "root_clips" in id || ("clips" in id && n.bounds.area >= fullScreen)
        }
        val mainNav = nodes.any { n ->
            val id = n.id.orEmpty()
            id == "tab_bar" || "feed_tab" in id || "profile_tab" in id
        }
        val inDms = nodes.any { n ->
            val id = n.id.orEmpty()
            "direct_thread" in id || "thread_composer" in id || "direct_inbox" in id || "inbox_refreshable" in id
        }
        val author = nodes.firstOrNull { n ->
            val id = n.id.orEmpty()
            "clips" in id && ("author_username" in id || "media_author" in id) && !n.text.isNullOrBlank()
        }?.text
        return Detection(
            screen = when {
                viewer || tabSelected -> Screen.SHORT_FORM_VIEWER
                inDms -> Screen.DIRECT_MESSAGES
                else -> Screen.OTHER
            },
            feedTabSelected = tabSelected,
            mainNavVisible = mainNav,
            contentKey = author?.hashCode()?.toString(),
        )
    }

    private fun youtube(root: UiNode): Detection {
        val nodes = root.walkVisible().toList()
        val fullScreen = root.bounds.area * FULL_SCREEN_FRACTION
        val tabSelected = nodes.any { it.selected && it.desc == "Shorts" }
        // Long-form playback uses watch_* views, and Shorts shelves are far smaller than the
        // screen, so neither can satisfy this rule.
        val viewer = nodes.any { n ->
            val id = n.id.orEmpty()
            "reel_recycler" in id || "reel_player_page" in id ||
                (id.startsWith("reel_") && n.bounds.area >= fullScreen)
        }
        return Detection(
            screen = if (viewer || tabSelected) Screen.SHORT_FORM_VIEWER else Screen.OTHER,
            feedTabSelected = tabSelected,
        )
    }

    // WeChat obfuscates its view ids, but Channels ("Finder" internally) has its own activities.
    private fun wechat(activityClass: String?): Detection = Detection(
        screen = if (activityClass.orEmpty().startsWith("com.tencent.mm.plugin.finder.")) {
            Screen.SHORT_FORM_VIEWER
        } else {
            Screen.OTHER
        },
    )
}
