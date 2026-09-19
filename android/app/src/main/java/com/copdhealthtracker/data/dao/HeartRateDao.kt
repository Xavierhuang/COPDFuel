package com.copdhealthtracker.data.dao

import androidx.room.*
import com.copdhealthtracker.data.model.HeartRateEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HeartRateDao {
    @Query("SELECT * FROM heart_rate_entries ORDER BY date DESC")
    fun getAllHeartRates(): Flow<List<HeartRateEntry>>

    @Query("SELECT * FROM heart_rate_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getHeartRatesByDateRange(startDate: Long, endDate: Long): Flow<List<HeartRateEntry>>

    @Query("SELECT COUNT(*) FROM heart_rate_entries WHERE date = :dateMillis")
    suspend fun countAtInstant(dateMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRate(entry: HeartRateEntry): Long

    @Delete
    suspend fun deleteHeartRate(entry: HeartRateEntry)
}
