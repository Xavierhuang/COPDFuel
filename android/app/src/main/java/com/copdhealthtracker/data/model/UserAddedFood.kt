package com.copdhealthtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-created food stored locally so it appears in food search next time.
 * All nutrient values are per 100g to match the main food database.
 */
@Entity(tableName = "user_added_foods")
data class UserAddedFood(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val shortName: String = name,
    val category: String = "User added",
    val categoryGroup: String = "User added",

    // Macros per 100g
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,

    // Minerals per 100g
    val sodium: Double = 0.0,
    val potassium: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val magnesium: Double = 0.0,
    val zinc: Double = 0.0,
    val selenium: Double = 0.0,
    val manganese: Double = 0.0,

    // Water per 100g
    val water: Double = 0.0,

    // Vitamins per 100g
    val vitaminA: Double = 0.0,
    val vitaminC: Double = 0.0,
    val vitaminD: Double = 0.0,
    val vitaminE: Double = 0.0,
    val vitaminK: Double = 0.0,

    // Fats per 100g
    val saturatedFat: Double = 0.0,
    val cholesterol: Double = 0.0,
    val omega3: Double = 0.0,
    val addedSugars: Double = 0.0,

    val portionSize: Double = 100.0,
    val portionUnit: String = "g",
    val portionDesc: String = "100g"
)
