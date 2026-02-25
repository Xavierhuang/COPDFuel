package com.copdhealthtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mealCategory: String, // Breakfast, Lunch, Dinner, Snacks
    val quantity: String,
    
    // Macros
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    
    // Minerals
    val sodium: Double = 0.0,
    val potassium: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val magnesium: Double = 0.0,
    val zinc: Double = 0.0,
    val selenium: Double = 0.0,
    val manganese: Double = 0.0,
    
    // Water
    val water: Double = 0.0,
    
    // Vitamins
    val vitaminA: Double = 0.0,
    val vitaminC: Double = 0.0,
    val vitaminD: Double = 0.0,
    val vitaminE: Double = 0.0,
    val vitaminK: Double = 0.0,
    
    // Fats
    val saturatedFat: Double = 0.0,
    val cholesterol: Double = 0.0,
    val omega3: Double = 0.0,
    
    // Sugars
    val addedSugars: Double = 0.0,
    
    val date: Long = System.currentTimeMillis()
)
