package app.speedbump.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrictionTest {
    @Test fun acceptsLeadingZeros() {
        assertTrue(Friction.isCorrect("042", 42))
        assertTrue(Friction.isCorrect("42", 42))
        assertTrue(Friction.isCorrect(" 0 ", 0))
    }

    @Test fun rejectsOthers() {
        assertFalse(Friction.isCorrect("", 42))
        assertFalse(Friction.isCorrect("43", 42))
        assertFalse(Friction.isCorrect("0042", 42))
        assertFalse(Friction.isCorrect("-42", 42))
        assertFalse(Friction.isCorrect("4x", 42))
    }
}
