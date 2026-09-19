package com.copdhealthtracker.health

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.copdhealthtracker.data.model.ExerciseEntry
import com.copdhealthtracker.data.model.HeartRateEntry
import com.copdhealthtracker.data.model.OxygenReading
import com.copdhealthtracker.data.model.WeightEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import kotlin.reflect.KClass

/** All permissions needed to import health data from Health Connect (Samsung watch, etc.). */
val HEALTH_CONNECT_IMPORT_PERMISSIONS: Set<String> = setOf(
    HealthPermission.getReadPermission(OxygenSaturationRecord::class),
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class)
)

/** Result of importing all supported health data from Health Connect. */
data class HealthConnectImportResult(
    val oxygen: List<OxygenReading>,
    val weight: List<WeightEntry>,
    val exercise: List<ExerciseEntry>,
    val steps: List<com.copdhealthtracker.data.model.StepsEntry>,
    val heartRate: List<HeartRateEntry>
) {
    val totalCount: Int get() = oxygen.size + weight.size + exercise.size + steps.size + heartRate.size
}

/**
 * Syncs health data from the device (e.g. Samsung watch, Wear OS) into the app
 * via Android Health Connect. Requires Health Connect to be installed and
 * the user to have granted read permission for the relevant data types.
 */
object HealthConnectSync {

    private const val MIN_SDK_FOR_HEALTH_CONNECT = 26

    fun isAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < MIN_SDK_FOR_HEALTH_CONNECT) return false
        return try {
            HealthConnectClient.getOrCreate(context)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Step total for one **local** calendar day from Health Connect (data from Samsung Health,
     * Google Fit, watch, etc. that syncs into HC). Uses the same merge as [readAllFromDevice].
     * Requires [StepsRecord] read permission; otherwise returns [Result.failure].
     */
    suspend fun readStepsForCalendarDay(context: Context, dayMillis: Long): Result<Int> =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < MIN_SDK_FOR_HEALTH_CONNECT) {
                return@withContext Result.failure(
                    UnsupportedOperationException("Health Connect requires Android 8.0 (API 26) or later")
                )
            }
            if (!isAvailable(context)) {
                return@withContext Result.failure(IllegalStateException("Health Connect unavailable"))
            }
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val granted = client.permissionController.getGrantedPermissions()
                if (HealthPermission.getReadPermission(StepsRecord::class) !in granted) {
                    return@withContext Result.failure(SecurityException("Steps read not granted"))
                }
                val zone = ZoneId.systemDefault()
                val localDay = Instant.ofEpochMilli(dayMillis).atZone(zone).toLocalDate()
                val start = localDay.atStartOfDay(zone).toInstant()
                val end = localDay.plusDays(1).atStartOfDay(zone).toInstant()
                val entries = readSteps(client, start, end)
                Result.success(entries.sumOf { it.count })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Reads all supported health data (oxygen, weight, exercise, steps) from Health Connect
     * for the given time range. Call only after the app has been granted the relevant permissions.
     */
    suspend fun readAllFromDevice(
        context: Context,
        startTimeMillis: Long,
        endTimeMillis: Long
    ): Result<HealthConnectImportResult> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < MIN_SDK_FOR_HEALTH_CONNECT) {
            return@withContext Result.failure(
                UnsupportedOperationException("Health Connect requires Android 8.0 (API 26) or later")
            )
        }
        try {
            val client = HealthConnectClient.getOrCreate(context)
            val start = Instant.ofEpochMilli(startTimeMillis)
            val end = Instant.ofEpochMilli(endTimeMillis)

            val oxygenResult = readOxygen(client, start, end)
            val weightResult = readWeight(client, start, end)
            val exerciseResult = readExercise(client, start, end)
            val stepsResult = readSteps(client, start, end)
            val heartRateResult = readHeartRate(client, start, end)

            Result.success(
                HealthConnectImportResult(
                    oxygen = oxygenResult,
                    weight = weightResult,
                    exercise = exerciseResult,
                    steps = stepsResult,
                    heartRate = heartRateResult
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [HealthConnectClient.readRecords] returns at most [ReadRecordsRequest] default page size (1000)
     * records per call. Heart rate and steps can exceed that over a multi-week range; without paging,
     * recent data is missing (first page is oldest when ascending).
     */
    private suspend fun <T : Record> readRecordsPaged(
        client: HealthConnectClient,
        recordType: KClass<T>,
        start: Instant,
        end: Instant
    ): List<T> {
        val filter = TimeRangeFilter.between(start, end)
        val all = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = recordType,
                    timeRangeFilter = filter,
                    pageToken = pageToken
                )
            )
            all.addAll(response.records)
            pageToken = response.pageToken
        } while (pageToken != null)
        return all
    }

    private suspend fun readOxygen(client: HealthConnectClient, start: Instant, end: Instant): List<OxygenReading> {
        val records = readRecordsPaged(client, OxygenSaturationRecord::class, start, end)
        return records.map { record ->
            OxygenReading(
                level = record.percentage.value.toInt().coerceIn(0, 100),
                date = record.time.toEpochMilli()
            )
        }
    }

    private suspend fun readWeight(client: HealthConnectClient, start: Instant, end: Instant): List<WeightEntry> {
        val records = readRecordsPaged(client, WeightRecord::class, start, end)
        return records.map { record ->
            val kg = record.weight.inKilograms
            WeightEntry(
                weight = kg,
                isGoal = false,
                date = record.time.toEpochMilli()
            )
        }
    }

    private suspend fun readExercise(client: HealthConnectClient, start: Instant, end: Instant): List<ExerciseEntry> {
        val records = readRecordsPaged(client, ExerciseSessionRecord::class, start, end)
        return records.map { record ->
            val durationMinutes = java.time.Duration.between(record.startTime, record.endTime).toMinutes().toInt().coerceAtLeast(1)
            val type = "Exercise"
            ExerciseEntry(
                type = type,
                minutes = durationMinutes,
                date = record.startTime.toEpochMilli()
            )
        }
    }

    /**
     * Samsung Health (and others) may expose steps through **aggregates**, raw [StepsRecord], or both.
     * Aggregates can still be **partial** for a day (e.g. one source only) while summed records match
     * the Health Connect UI total. We merge both paths and keep the **higher** per-day count so we do
     * not short-circuit on a low aggregate while richer records exist.
     */
    private suspend fun readSteps(client: HealthConnectClient, start: Instant, end: Instant): List<com.copdhealthtracker.data.model.StepsEntry> {
        val byDay = mutableMapOf<Long, Int>()
        for (e in readStepsAggregatedByDay(client, start, end)) {
            byDay[e.date] = maxOf(byDay[e.date] ?: 0, e.count)
        }
        for (e in readStepsFromRecords(client, start, end)) {
            byDay[e.date] = maxOf(byDay[e.date] ?: 0, e.count)
        }
        return byDay.map { (date, count) ->
            com.copdhealthtracker.data.model.StepsEntry(count = count, date = date)
        }
    }

    private suspend fun readStepsAggregatedByDay(
        client: HealthConnectClient,
        start: Instant,
        end: Instant
    ): List<com.copdhealthtracker.data.model.StepsEntry> {
        val zone = ZoneId.systemDefault()
        val startLdt = LocalDateTime.ofInstant(start, zone)
        val endLdt = LocalDateTime.ofInstant(end, zone)
        if (!startLdt.isBefore(endLdt)) return emptyList()
        return try {
            val request = AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startLdt, endLdt),
                timeRangeSlicer = Period.ofDays(1)
            )
            val buckets = client.aggregateGroupByPeriod(request)
            buckets.mapNotNull { bucket ->
                if (StepsRecord.COUNT_TOTAL !in bucket.result) return@mapNotNull null
                val total = bucket.result[StepsRecord.COUNT_TOTAL] ?: return@mapNotNull null
                if (total <= 0L) return@mapNotNull null
                val dayStart = bucket.startTime.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
                val count = total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                com.copdhealthtracker.data.model.StepsEntry(count = count, date = dayStart)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun readStepsFromRecords(client: HealthConnectClient, start: Instant, end: Instant): List<com.copdhealthtracker.data.model.StepsEntry> {
        val stepRecords = readRecordsPaged(client, StepsRecord::class, start, end)
        val stepsByDay = mutableMapOf<Long, Int>()
        val zone = ZoneId.systemDefault()
        for (record in stepRecords) {
            val dayStart = record.startTime.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
            val count = record.count.toInt()
            stepsByDay[dayStart] = (stepsByDay[dayStart] ?: 0) + count
        }
        return stepsByDay.map { (date, count) ->
            com.copdhealthtracker.data.model.StepsEntry(count = count, date = date)
        }
    }

    private suspend fun readHeartRate(client: HealthConnectClient, start: Instant, end: Instant): List<HeartRateEntry> {
        val hrRecords = readRecordsPaged(client, HeartRateRecord::class, start, end)
        val results = mutableListOf<HeartRateEntry>()
        for (record in hrRecords) {
            for (sample in record.samples) {
                results.add(
                    HeartRateEntry(
                        bpm = sample.beatsPerMinute.toInt(),
                        date = sample.time.toEpochMilli()
                    )
                )
            }
        }
        return results
    }
}
