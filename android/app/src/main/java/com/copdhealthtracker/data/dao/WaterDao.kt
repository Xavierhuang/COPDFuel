package com.copdhealthtracker.data.dao

import androidx.room.*
import com.copdhealthtracker.data.model.WaterEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_entries ORDER BY date DESC")
    fun getAllWaterEntries(): Flow<List<WaterEntry>>
    
    @Query("SELECT * FROM water_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getWaterEntriesByDateRange(startDate: Long, endDate: Long): Flow<List<WaterEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterEntry(entry: WaterEntry): Long
    
    @Delete
    suspend fun deleteWaterEntry(entry: WaterEntry)
    
    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteWaterEntryById(id: Long)
}
