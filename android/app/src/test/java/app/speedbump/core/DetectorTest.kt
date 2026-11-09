package app.speedbump.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorTest {
    private val full = Box(0, 0, 1000, 2000)
    private fun root(vararg children: UiNode) = UiNode(bounds = full, children = children.toList())

    @Test fun instagramReelViewerIsShortForm() {
        val d = Detector.detect(TargetApp.INSTAGRAM, root(UiNode(id = "clips_viewer_view_pager", bounds = full)), null)
        assertEquals(Screen.SHORT_FORM_VIEWER, d.screen)
    }

    @Test fun instagramReelThumbnailInFeedIsNotShortForm() {
        val thumbnail = UiNode(id = "clips_preview", bounds = Box(0, 0, 300, 400))
        val d = Detector.detect(TargetApp.INSTAGRAM, root(thumbnail), null)
        assertEquals(Screen.OTHER, d.screen)
    }

    @Test fun instagramChatIsDirectMessages() {
        val d = Detector.detect(TargetApp.INSTAGRAM, root(UiNode(id = "row_thread_composer_edittext")), null)
        assertEquals(Screen.DIRECT_MESSAGES, d.screen)
    }

    @Test fun reelsTabSelectedIsMarked() {
        val tab = UiNode(id = "clips_tab", selected = true)
        val d = Detector.detect(TargetApp.INSTAGRAM, root(tab), null)
        assertEquals(Screen.SHORT_FORM_VIEWER, d.screen)
        assertTrue(d.feedTabSelected)
    }

    @Test fun unselectedReelsTabDoesNotBlock() {
        val tab = UiNode(id = "clips_tab", desc = "Reels", selected = false, bounds = Box(0, 1900, 200, 2000))
        val d = Detector.detect(TargetApp.INSTAGRAM, root(tab), null)
        assertEquals(Screen.OTHER, d.screen)
    }

    @Test fun invisibleViewerIsIgnored() {
        val hidden = UiNode(id = "clips_viewer_view_pager", visible = false, bounds = full)
        assertEquals(Screen.OTHER, Detector.detect(TargetApp.INSTAGRAM, root(hidden), null).screen)
    }

    @Test fun youtubeShortsPlayerIsShortForm() {
        val d = Detector.detect(TargetApp.YOUTUBE, root(UiNode(id = "reel_recycler", bounds = full)), null)
        assertEquals(Screen.SHORT_FORM_VIEWER, d.screen)
    }

    @Test fun youtubeLongFormVideoIsNeverBlocked() {
        val tree = root(
            UiNode(id = "watch_player", bounds = Box(0, 0, 1000, 600)),
            UiNode(id = "title", text = "Shorts are ruining my attention span"),
            UiNode(id = "reel_item_container", bounds = Box(0, 800, 300, 1300)),
            UiNode(desc = "Shorts", selected = false),
            UiNode(desc = "Home", selected = true),
        )
        assertEquals(Screen.OTHER, Detector.detect(TargetApp.YOUTUBE, tree, null).screen)
    }

    @Test fun youtubeFullscreenLongFormIsNeverBlocked() {
        val tree = root(UiNode(id = "watch_player", bounds = full))
        assertEquals(Screen.OTHER, Detector.detect(TargetApp.YOUTUBE, tree, null).screen)
    }

    @Test fun youtubeShortsTabSelectedBlocks() {
        val tree = root(UiNode(desc = "Shorts", selected = true))
        assertEquals(Screen.SHORT_FORM_VIEWER, Detector.detect(TargetApp.YOUTUBE, tree, null).screen)
    }

    @Test fun wechatChannelsIsDetectedByActivity() {
        val channels = "com.tencent.mm.plugin.finder.ui.FinderHomeUI"
        assertEquals(Screen.SHORT_FORM_VIEWER, Detector.detect(TargetApp.WECHAT, root(), channels).screen)
    }

    @Test fun wechatChatIsNotBlocked() {
        val chat = "com.tencent.mm.ui.LauncherUI"
        assertEquals(Screen.OTHER, Detector.detect(TargetApp.WECHAT, root(), chat).screen)
    }

    @Test fun commentListIsNotTheReelPager() {
        assertFalse(Detector.isViewerPager(TargetApp.INSTAGRAM, "sticky_header_list", "androidx.recyclerview.widget.RecyclerView"))
        assertTrue(Detector.isViewerPager(TargetApp.INSTAGRAM, "clips_viewer_view_pager", null))
    }
}
