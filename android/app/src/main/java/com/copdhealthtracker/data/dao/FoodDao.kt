package com.copdhealthtracker.data.dao

import androidx.room.*
import com.copdhealthtracker.data.model.FoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries ORDER BY date DESC")
    fun getAllFoods(): Flow<List<FoodEntry>>
    
    @Query("SELECT * FROM food_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getFoodsByDateRange(startDate: Long, endDate: Long): Flow<List<FoodEntry>>
    
    @Query("SELECT * FROM food_entries WHERE mealCategory = :category AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getFoodsByCategory(category: String, startDate: Long, endDate: Long): Flow<List<FoodEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntry): Long
    
    @Delete
    suspend fun deleteFood(food: FoodEntry)
    
    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteFoodById(id: Long)
}
