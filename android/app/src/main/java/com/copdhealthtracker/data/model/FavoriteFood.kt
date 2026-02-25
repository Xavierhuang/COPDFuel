package com.copdhealthtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_foods")
data class FavoriteFood(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val name: String,
    val mealCategory: String,
    val quantity: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    val sodium: Double = 0.0,
    val potassium: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val magnesium: Double = 0.0,
    val zinc: Double = 0.0,
    val selenium: Double = 0.0,
    val manganese: Double = 0.0,
    val water: Double = 0.0,
    val vitaminA: Double = 0.0,
    val vitaminC: Double = 0.0,
    val vitaminD: Double = 0.0,
    val vitaminE: Double = 0.0,
    val vitaminK: Double = 0.0,
    val saturatedFat: Double = 0.0,
    val cholesterol: Double = 0.0,
    val omega3: Double = 0.0,
    val addedSugars: Double = 0.0
) {
    fun toFoodEntry(dateMillis: Long, mealCategoryOverride: String? = null): FoodEntry = FoodEntry(
        name = name,
        mealCategory = mealCategoryOverride ?: mealCategory,
        quantity = quantity,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        fiber = fiber,
        sodium = sodium,
        potassium = potassium,
        calcium = calcium,
        iron = iron,
        magnesium = magnesium,
        zinc = zinc,
        selenium = selenium,
        manganese = manganese,
        water = water,
        vitaminA = vitaminA,
        vitaminC = vitaminC,
        vitaminD = vitaminD,
        vitaminE = vitaminE,
        vitaminK = vitaminK,
        saturatedFat = saturatedFat,
        cholesterol = cholesterol,
        omega3 = omega3,
        addedSugars = addedSugars,
        date = dateMillis
    )
}
