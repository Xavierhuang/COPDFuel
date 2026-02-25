package com.copdhealthtracker.data.dao

import androidx.room.*
import com.copdhealthtracker.data.model.OxygenReading
import kotlinx.coroutines.flow.Flow

@Dao
interface OxygenDao {
    @Query("SELECT * FROM oxygen_readings ORDER BY date DESC")
    fun getAllReadings(): Flow<List<OxygenReading>>
    
    @Query("SELECT * FROM oxygen_readings WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getReadingsByDateRange(startDate: Long, endDate: Long): Flow<List<OxygenReading>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: OxygenReading): Long
    
    @Delete
    suspend fun deleteReading(reading: OxygenReading)
}
