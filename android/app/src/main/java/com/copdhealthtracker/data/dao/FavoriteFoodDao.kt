package com.copdhealthtracker.data.dao

import androidx.room.*
import com.copdhealthtracker.data.model.FavoriteFood
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteFoodDao {
    @Query("SELECT * FROM favorite_foods ORDER BY label ASC")
    fun getAllFavorites(): Flow<List<FavoriteFood>>

    @Query("SELECT * FROM favorite_foods WHERE LOWER(TRIM(label)) = LOWER(TRIM(:label)) LIMIT 1")
    suspend fun getByLabel(label: String): FavoriteFood?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteFood): Long

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteFood)
}
