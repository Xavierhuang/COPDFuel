package com.copdhealthtracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.copdhealthtracker.data.model.FavoriteMeal
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteMealDao {
    @Query("SELECT * FROM favorite_meals ORDER BY label ASC")
    fun getAllMeals(): Flow<List<FavoriteMeal>>

    @Query("SELECT * FROM favorite_meals WHERE id = :mealId LIMIT 1")
    suspend fun getById(mealId: Long): FavoriteMeal?

    @Query("SELECT * FROM favorite_meals WHERE LOWER(TRIM(label)) = LOWER(TRIM(:label)) LIMIT 1")
    suspend fun getByLabel(label: String): FavoriteMeal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: FavoriteMeal): Long

    @Query("DELETE FROM favorite_meals WHERE id = :mealId")
    suspend fun deleteById(mealId: Long)
}
