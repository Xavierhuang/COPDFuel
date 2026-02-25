package com.copdhealthtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_meals")
data class FavoriteMeal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val mealCategory: String
)
