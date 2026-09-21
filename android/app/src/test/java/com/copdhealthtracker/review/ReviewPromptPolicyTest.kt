package com.copdhealthtracker.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptPolicyTest {
    private val day = 86_400_000L
    private val installed = 1_800_000_000_000L

    private fun eligibleState() = ReviewPromptPolicy.State(
        installedAtMillis = installed, logCount = 5, lastPromptedAtMillis = null, lastPromptedVersion = null
    )

    @Test
    fun `prompts once installed a week with five logs`() {
        assertTrue(ReviewPromptPolicy.shouldPrompt(eligibleState(), nowMillis = installed + 7 * day, appVersion = "1.0.9"))
    }

    @Test
    fun `waits until the app has been installed seven days`() {
        assertFalse(ReviewPromptPolicy.shouldPrompt(eligibleState(), nowMillis = installed + 7 * day - 1, appVersion = "1.0.9"))
    }

    @Test
    fun `never prompts before an install date is known`() {
        val state = eligibleState().copy(installedAtMillis = null)
        assertFalse(ReviewPromptPolicy.shouldPrompt(state, nowMillis = installed + 30 * day, appVersion = "1.0.9"))
    }

    @Test
    fun `waits for five saved logs`() {
        val state = eligibleState().copy(logCount = 4)
        assertFalse(ReviewPromptPolicy.shouldPrompt(state, nowMillis = installed + 30 * day, appVersion = "1.0.9"))
    }

    @Test
    fun `leaves a hundred and twenty days between prompts`() {
        val state = eligibleState().copy(lastPromptedAtMillis = installed + 10 * day, lastPromptedVersion = "1.0.8")
        assertFalse(ReviewPromptPolicy.shouldPrompt(state, nowMillis = installed + 130 * day - 1, appVersion = "1.0.9"))
        assertTrue(ReviewPromptPolicy.shouldPrompt(state, nowMillis = installed + 130 * day, appVersion = "1.0.9"))
    }

    @Test
    fun `prompts only once per app version`() {
        val state = eligibleState().copy(lastPromptedAtMillis = installed + 10 * day, lastPromptedVersion = "1.0.9")
        assertFalse(ReviewPromptPolicy.shouldPrompt(state, nowMillis = installed + 400 * day, appVersion = "1.0.9"))
    }
}
