package com.copdhealthtracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.copdhealthtracker.data.model.UserAddedFood

@Dao
interface UserAddedFoodDao {
    @Query("SELECT * FROM user_added_foods ORDER BY name ASC")
    suspend fun getAll(): List<UserAddedFood>

    @Query("SELECT * FROM user_added_foods WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getByName(name: String): UserAddedFood?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: UserAddedFood): Long
}
