package com.copdhealthtracker.review

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.copdhealthtracker.BuildConfig
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Asks Google Play for its native in-app review prompt after a successful
 * Tracking save, throttled by [ReviewPromptPolicy]. Play decides whether the
 * prompt really appears and never reports back, so "prompted" here means
 * "asked". The prompt only shows for builds installed from Play.
 */
object ReviewPrompter {
    private const val PREFS = "review_prompt"
    private const val KEY_LOG_COUNT = "log_count"
    private const val KEY_LAST_PROMPTED_AT = "last_prompted_at"
    private const val KEY_LAST_PROMPTED_VERSION = "last_prompted_version"

    /** Toast.LENGTH_SHORT is 2 s; ask once the "saved" toast has gone. */
    private const val DELAY_MILLIS = 2_500L

    private val handler = Handler(Looper.getMainLooper())

    /** Call after a Tracking entry is saved successfully. */
    fun logSaved(activity: FragmentActivity) {
        val prefs = prefs(activity)
        prefs.edit().putInt(KEY_LOG_COUNT, prefs.getInt(KEY_LOG_COUNT, 0) + 1).apply()
        if (!shouldPrompt(activity)) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ requestReview(activity) }, DELAY_MILLIS)
    }

    /**
     * Skips (and leaves the user eligible for the next save) unless the
     * activity is in front with no dialog up — the prompt must never land on
     * the paywall, the HIPAA form, the scanner or another dialog.
     */
    private fun requestReview(activity: FragmentActivity) {
        if (!isScreenClear(activity) || !shouldPrompt(activity)) return
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (!request.isSuccessful || !isScreenClear(activity)) return@addOnCompleteListener
            prefs(activity).edit()
                .putLong(KEY_LAST_PROMPTED_AT, System.currentTimeMillis())
                .putString(KEY_LAST_PROMPTED_VERSION, BuildConfig.VERSION_NAME)
                .apply()
            manager.launchReviewFlow(activity, request.result)
        }
    }

    private fun shouldPrompt(context: Context): Boolean {
        val prefs = prefs(context)
        val state = ReviewPromptPolicy.State(
            installedAtMillis = installedAtMillis(context),
            logCount = prefs.getInt(KEY_LOG_COUNT, 0),
            lastPromptedAtMillis = prefs.getLong(KEY_LAST_PROMPTED_AT, 0L).takeIf { it > 0L },
            lastPromptedVersion = prefs.getString(KEY_LAST_PROMPTED_VERSION, null)
        )
        return ReviewPromptPolicy.shouldPrompt(state, System.currentTimeMillis(), BuildConfig.VERSION_NAME)
    }

    private fun installedAtMillis(context: Context): Long? = try {
        context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
    } catch (e: Exception) {
        null
    }

    private fun isScreenClear(activity: FragmentActivity): Boolean =
        !activity.isFinishing && !activity.isDestroyed &&
            activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            !hasDialog(activity.supportFragmentManager)

    /** Tracking's dialogs live in the nav host's child manager, so look all the way down. */
    private fun hasDialog(manager: FragmentManager): Boolean =
        manager.fragments.any { it.isAdded && (it is DialogFragment || hasDialog(it.childFragmentManager)) }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
