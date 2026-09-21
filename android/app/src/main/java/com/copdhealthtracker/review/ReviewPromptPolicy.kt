package com.copdhealthtracker.review

/**
 * When the app may ask Google Play for its native in-app review prompt. Play
 * enforces its own quota and never says whether the prompt appeared, so this
 * is our own throttle on top.
 */
object ReviewPromptPolicy {
    const val MIN_DAYS_SINCE_INSTALL = 7
    const val MIN_LOGS = 5
    const val MIN_DAYS_BETWEEN_PROMPTS = 120

    private const val DAY_MILLIS = 86_400_000L

    data class State(
        val installedAtMillis: Long?,
        val logCount: Int,
        val lastPromptedAtMillis: Long?,
        val lastPromptedVersion: String?
    )

    fun shouldPrompt(state: State, nowMillis: Long, appVersion: String): Boolean {
        val installedAt = state.installedAtMillis ?: return false
        if (nowMillis - installedAt < MIN_DAYS_SINCE_INSTALL * DAY_MILLIS) return false
        if (state.logCount < MIN_LOGS) return false
        if (state.lastPromptedVersion == appVersion) return false
        val lastPromptedAt = state.lastPromptedAtMillis
        if (lastPromptedAt != null && nowMillis - lastPromptedAt < MIN_DAYS_BETWEEN_PROMPTS * DAY_MILLIS) return false
        return true
    }
}
