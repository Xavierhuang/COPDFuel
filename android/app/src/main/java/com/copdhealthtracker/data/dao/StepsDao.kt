package com.copdhealthtracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.copdhealthtracker.data.model.StepsEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface StepsDao {
    @Query("SELECT * FROM steps_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getStepsByDateRange(startDate: Long, endDate: Long): Flow<List<StepsEntry>>

    /** Same calendar day as stored in [StepsEntry.date] (start-of-day millis). */
    @Query("DELETE FROM steps_entries WHERE date = :dayStartMillis")
    suspend fun deleteStepsForDayStart(dayStartMillis: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(entry: StepsEntry): Long
}
