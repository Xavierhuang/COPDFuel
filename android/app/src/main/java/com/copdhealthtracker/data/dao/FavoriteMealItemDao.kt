package com.copdhealthtracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.copdhealthtracker.data.model.FavoriteMealItem

@Dao
interface FavoriteMealItemDao {
    @Query("SELECT * FROM favorite_meal_items WHERE mealId = :mealId ORDER BY id ASC")
    suspend fun getByMealId(mealId: Long): List<FavoriteMealItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FavoriteMealItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FavoriteMealItem>)

    @Query("DELETE FROM favorite_meal_items WHERE mealId = :mealId")
    suspend fun deleteByMealId(mealId: Long)
}
