package com.copdhealthtracker.data.dao

import androidx.room.*
import com.copdhealthtracker.data.model.ExerciseEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise_entries ORDER BY date DESC")
    fun getAllExercises(): Flow<List<ExerciseEntry>>
    
    @Query("SELECT * FROM exercise_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExercisesByDateRange(startDate: Long, endDate: Long): Flow<List<ExerciseEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntry): Long
    
    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntry)
}
