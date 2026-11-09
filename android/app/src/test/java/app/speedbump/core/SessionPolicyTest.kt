package app.speedbump.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionPolicyTest {
    private val dms = Detection(Screen.DIRECT_MESSAGES)
    private val viewer = Detection(Screen.SHORT_FORM_VIEWER)
    private val reelsTab = Detection(Screen.SHORT_FORM_VIEWER, feedTabSelected = true)
    private val home = Detection(Screen.OTHER, mainNavVisible = true)
    private val sheet = Detection(Screen.OTHER)

    private fun instagram() = SessionPolicy(allowSentItems = true, advanceThresholdPx = 500)
    private fun SessionPolicy.see(d: Detection, enabled: Boolean = true, unlocked: Boolean = false, now: Long = 0) =
        observe(d, enabled, unlocked, now)

    @Test fun reelSentInChatPlays() {
        val p = instagram()
        assertEquals(Decision.NONE, p.see(dms))
        assertEquals(Decision.NONE, p.see(viewer))
        assertEquals(Decision.NONE, p.see(viewer))
    }

    @Test fun swipingPastSentReelIsBlocked() {
        val p = instagram()
        p.see(dms); p.see(viewer)
        p.onViewerScrolled(deltaY = 900, fromIndex = 0)
        assertEquals(Decision.SHOW_WALL, p.see(viewer))
    }

    @Test fun pagerIndexChangeIsBlocked() {
        val p = instagram()
        p.see(dms); p.see(viewer)
        p.onViewerScrolled(deltaY = -1, fromIndex = 0)
        p.onViewerScrolled(deltaY = -1, fromIndex = 1)
        assertEquals(Decision.SHOW_WALL, p.see(viewer))
    }

    @Test fun smallWiggleKeepsSentReelPlaying() {
        val p = instagram()
        p.see(dms); p.see(viewer)
        p.onViewerScrolled(deltaY = 40, fromIndex = 0)
        p.onViewerScrolled(deltaY = -40, fromIndex = 0)
        assertEquals(Decision.NONE, p.see(viewer))
    }

    @Test fun differentAuthorIsTreatedAsMovingOn() {
        val p = instagram()
        p.see(dms)
        assertEquals(Decision.NONE, p.see(viewer.copy(contentKey = "alice")))
        assertEquals(Decision.SHOW_WALL, p.see(viewer.copy(contentKey = "bob")))
    }

    @Test fun commentSheetOverSentReelDoesNotBlockOnReturn() {
        val p = instagram()
        p.see(dms); p.see(viewer)
        assertEquals(Decision.NONE, p.see(sheet))
        assertEquals(Decision.NONE, p.see(viewer))
    }

    @Test fun backToChatThenAnotherSentReelPlays() {
        val p = instagram()
        p.see(dms); p.see(viewer)
        p.onViewerScrolled(900, 0)
        assertEquals(Decision.SHOW_WALL, p.see(viewer))
        p.see(dms)
        assertEquals(Decision.NONE, p.see(viewer))
    }

    @Test fun reelsTabIsBlockedEvenStraightFromDms() {
        val p = instagram()
        p.see(dms)
        assertEquals(Decision.SHOW_WALL, p.see(reelsTab))
    }

    @Test fun reelFromHomeFeedIsBlocked() {
        val p = instagram()
        p.see(home)
        assertEquals(Decision.SHOW_WALL, p.see(viewer))
    }

    @Test fun visitingHomeForgetsTheChat() {
        val p = instagram()
        p.see(dms); p.see(home)
        assertEquals(Decision.SHOW_WALL, p.see(viewer))
    }

    @Test fun unlockedSectionIsOpen() {
        assertEquals(Decision.NONE, instagram().see(viewer, unlocked = true))
    }

    @Test fun disabledSectionIsOpen() {
        assertEquals(Decision.NONE, instagram().see(viewer, enabled = false))
    }

    @Test fun stillInSectionAfterGivingUpGoesHome() {
        val p = instagram()
        p.onGaveUp(nowMs = 1000)
        assertEquals(Decision.LEAVE_TO_HOME, p.see(viewer, now = 2000))
        assertEquals(Decision.SHOW_WALL, p.see(viewer, now = 4000))
    }

    @Test fun sentReelSurvivesServiceRestart() {
        val before = instagram()
        before.see(dms)
        val afterChat = SessionPolicy(true, 500, before.state)
        assertEquals(Decision.NONE, afterChat.see(viewer))
        val afterViewer = SessionPolicy(true, 500, afterChat.state)
        assertEquals(Decision.NONE, afterViewer.see(viewer))
        afterViewer.onViewerScrolled(900, 0)
        assertEquals(Decision.SHOW_WALL, SessionPolicy(true, 500, afterViewer.state).see(viewer))
    }

    @Test fun appsWithoutSentExemptionBlockAfterDmScreen() {
        val p = SessionPolicy(allowSentItems = false, advanceThresholdPx = 500)
        p.see(dms)
        assertEquals(Decision.SHOW_WALL, p.see(viewer))
    }
}
