package app.speedbump.core

object Friction {
    const val WAIT_SECONDS = 60
    const val WRONG_ANSWER_LOCK_SECONDS = 10

    /** AIME answers are integers from 000 to 999; leading zeros are optional. */
    fun isCorrect(input: String, answer: Int): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.length > 3 || !trimmed.all { it in '0'..'9' }) return false
        return trimmed.toInt() == answer
    }
}
