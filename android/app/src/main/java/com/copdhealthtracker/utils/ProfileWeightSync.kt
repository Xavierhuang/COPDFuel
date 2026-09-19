package com.copdhealthtracker.utils

import android.content.Context
import androidx.preference.PreferenceManager
import com.copdhealthtracker.data.model.WeightEntry
import com.copdhealthtracker.repository.DataRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Keeps SharedPreferences profile "weight" aligned with the latest non-goal [com.copdhealthtracker.data.model.WeightEntry].
 */
object ProfileWeightSync {

    fun writeWeightToPrefs(context: Context, weightLbs: Double) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
            .putString("weight", formatWeight(weightLbs))
            .putString(
                "last_updated",
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
            )
            .apply()
    }

    suspend fun syncPrefsFromRepositoryCurrentWeight(context: Context, repository: DataRepository) {
        val latest = repository.getCurrentWeight().first() ?: return
        writeWeightToPrefs(context, latest.weight)
    }

    /** Sync prefs from DB when tracking is newer; seed DB from prefs when DB has no current weight. */
    suspend fun syncProfileAndDbWeight(context: Context, repository: DataRepository) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val latest = repository.getCurrentWeight().first()
        val prefW = prefs.getString("weight", null)?.toDoubleOrNull()
        when {
            latest != null -> {
                if (prefW == null || abs(latest.weight - prefW) > 0.05) {
                    writeWeightToPrefs(context, latest.weight)
                }
            }
            prefW != null && prefW > 0 -> {
                repository.insertWeight(WeightEntry(weight = prefW, isGoal = false))
            }
        }
    }

    private fun formatWeight(lbs: Double): String {
        val rounded = kotlin.math.round(lbs * 10) / 10
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }
}
