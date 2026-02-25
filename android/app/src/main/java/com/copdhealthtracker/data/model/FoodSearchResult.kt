package com.copdhealthtracker.data.model

data class ServingSize(
    val label: String,
    val grams: Double,
    val amount: Double = 1.0,
    val unit: String = "serving",
    val isPrimary: Boolean = false,
    val isCustom: Boolean = false
)

data class FoodSearchResult(
    val fdcId: Int,
    val description: String,
    val brandOwner: String?,
    
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
    
    // Sugars per 100g
    val addedSugars: Double = 0.0,
    
    // Serving info
    val servingSize: Double?,
    val servingUnit: String?,
    val servingSizes: List<ServingSize> = emptyList()
)
