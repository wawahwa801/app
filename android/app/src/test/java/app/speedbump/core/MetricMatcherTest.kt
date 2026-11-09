package app.speedbump.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricMatcherTest {
    private val all = MetricKind.entries.toSet()
    private val box = Box(0, 0, 100, 40)

    @Test fun classifiesCounts() {
        assertEquals(MetricKind.LIKES, MetricMatcher.classify("1,204 likes"))
        assertEquals(MetricKind.LIKES, MetricMatcher.classify("Liked by sam and others"))
        assertEquals(MetricKind.VIEWS, MetricMatcher.classify("2.3M views"))
        assertEquals(MetricKind.VIEWS, MetricMatcher.classify("88 plays"))
        assertEquals(MetricKind.FOLLOWERS, MetricMatcher.classify("10.5K followers"))
        assertEquals(MetricKind.FOLLOWERS, MetricMatcher.classify("312 following"))
    }

    @Test fun leavesOrdinaryTextAlone() {
        assertNull(MetricMatcher.classify("I have 5 likes on this idea"))
        assertNull(MetricMatcher.classify("views from the top"))
        assertNull(MetricMatcher.classify("Following"))
        assertNull(MetricMatcher.classify("42"))
    }

    @Test fun followButtonLabelIsNotMasked() {
        val root = UiNode(children = listOf(UiNode(text = "Following", clickable = true, bounds = box)))
        assertTrue(MetricMatcher.find(root, all).isEmpty())
    }

    @Test fun masksNumberBesideItsLabel() {
        val number = UiNode(text = "1,024", bounds = box)
        val root = UiNode(children = listOf(number, UiNode(text = "followers", bounds = box)))
        assertEquals(listOf(MetricHit(MetricKind.FOLLOWERS, box)), MetricMatcher.find(root, all))
    }

    @Test fun respectsToggles() {
        val root = UiNode(children = listOf(UiNode(text = "12 likes", bounds = box), UiNode(text = "9 views", bounds = box)))
        assertEquals(listOf(MetricHit(MetricKind.VIEWS, box)), MetricMatcher.find(root, setOf(MetricKind.VIEWS)))
    }

    @Test fun clickableLikeButtonWithCountInDescriptionIsNotCovered() {
        val root = UiNode(children = listOf(UiNode(desc = "Like number is 5", clickable = true, bounds = box)))
        assertTrue(MetricMatcher.find(root, all).isEmpty())
    }
}
