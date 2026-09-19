package com.copdhealthtracker.utils

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.copdhealthtracker.HipaaAuthorizationActivity

/**
 * Single source of truth for HIPAA consent enforcement on data-sharing paths.
 *
 * RULE: every code path that shares, exports, or transmits patient data
 * outside the device must call [requireConsent] (interactive) or [hasConsent]
 * (silent) before proceeding. If consent is missing, the share MUST NOT fire.
 *
 * See project memory: "HIPAA consent required before any data sharing".
 */
object HipaaGate {

    /**
     * Returns true if the user has signed valid HIPAA consent.
     * If not, shows a dialog explaining and offers to open the consent form,
     * then returns false. Caller MUST early-return when this returns false.
     *
     * @param actionLabel Human-readable action used in the dialog message,
     *                    e.g. "share your report" or "link your doctor".
     */
    fun requireConsent(context: Context, actionLabel: String = "share your health data"): Boolean {
        val storage = HipaaConsentStorage(context)
        if (storage.hasValidConsent()) return true
        AlertDialog.Builder(context)
            .setTitle("HIPAA Authorization Required")
            .setMessage("You must sign the HIPAA authorization form before you can $actionLabel.")
            .setPositiveButton("Sign Now") { _, _ ->
                context.startActivity(Intent(context, HipaaAuthorizationActivity::class.java))
            }
            .setNegativeButton("Cancel", null)
            .show()
        return false
    }

    /**
     * Silent variant for non-interactive paths (e.g. auto-sync on login).
     * Returns true if consent is present; false otherwise. No UI is shown.
     */
    fun hasConsent(context: Context): Boolean =
        HipaaConsentStorage(context).hasValidConsent()
}
