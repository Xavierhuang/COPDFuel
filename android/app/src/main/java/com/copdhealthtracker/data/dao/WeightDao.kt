package com.copdhealthtracker.data.dao

import androidx.room.*
import com.copdhealthtracker.data.model.WeightEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY date DESC")
    fun getAllWeights(): Flow<List<WeightEntry>>
    
    @Query("SELECT * FROM weight_entries WHERE isGoal = 0 ORDER BY date DESC LIMIT 1")
    fun getCurrentWeight(): Flow<WeightEntry?>
    
    @Query("SELECT * FROM weight_entries WHERE isGoal = 1 ORDER BY date DESC LIMIT 1")
    fun getGoalWeight(): Flow<WeightEntry?>
    
    @Query("SELECT * FROM weight_entries WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun getWeightsByDateRange(startDate: Long, endDate: Long): Flow<List<WeightEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightEntry): Long
    
    @Delete
    suspend fun deleteWeight(weight: WeightEntry)
}
