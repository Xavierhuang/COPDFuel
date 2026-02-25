package com.copdhealthtracker.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Stores HIPAA Authorization consent and revocation in SharedPreferences.
 */
class HipaaConsentStorage(context: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun hasValidConsent(): Boolean =
        prefs.getBoolean(KEY_CONSENT_SIGNED, false) && !prefs.getBoolean(KEY_REVOKED, false)

    fun getConsentDate(): Long = prefs.getLong(KEY_CONSENT_DATE, 0L)
    fun getConsentPrintedName(): String = prefs.getString(KEY_CONSENT_PRINTED_NAME, "").orEmpty()
    fun getConsentDateOfBirth(): String = prefs.getString(KEY_CONSENT_DOB, "").orEmpty()
    fun getConsentExpiryType(): String = prefs.getString(KEY_CONSENT_EXPIRY_TYPE, "").orEmpty()
    fun getConsentExpiryDate(): Long = prefs.getLong(KEY_CONSENT_EXPIRY_DATE, 0L)
    fun isRevoked(): Boolean = prefs.getBoolean(KEY_REVOKED, false)
    fun getRevokedDate(): Long = prefs.getLong(KEY_REVOKED_DATE, 0L)

    fun saveConsent(printedName: String, dateOfBirth: String, expiryType: String, expiryDateMillis: Long) {
        prefs.edit()
            .putBoolean(KEY_CONSENT_SIGNED, true)
            .putLong(KEY_CONSENT_DATE, System.currentTimeMillis())
            .putString(KEY_CONSENT_PRINTED_NAME, printedName.trim())
            .putString(KEY_CONSENT_DOB, dateOfBirth.trim())
            .putString(KEY_CONSENT_EXPIRY_TYPE, expiryType)
            .putLong(KEY_CONSENT_EXPIRY_DATE, expiryDateMillis)
            .putBoolean(KEY_REVOKED, false)
            .remove(KEY_REVOKED_DATE)
            .apply()
    }

    fun revokeConsent() {
        prefs.edit()
            .putBoolean(KEY_REVOKED, true)
            .putLong(KEY_REVOKED_DATE, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val PREFIX = "hipaa_"
        private const val KEY_CONSENT_SIGNED = "${PREFIX}consent_signed"
        private const val KEY_CONSENT_DATE = "${PREFIX}consent_date"
        private const val KEY_CONSENT_PRINTED_NAME = "${PREFIX}consent_printed_name"
        private const val KEY_CONSENT_DOB = "${PREFIX}consent_dob"
        private const val KEY_CONSENT_EXPIRY_TYPE = "${PREFIX}consent_expiry_type"
        private const val KEY_CONSENT_EXPIRY_DATE = "${PREFIX}consent_expiry_date"
        private const val KEY_REVOKED = "${PREFIX}revoked"
        private const val KEY_REVOKED_DATE = "${PREFIX}revoked_date"
    }
}
