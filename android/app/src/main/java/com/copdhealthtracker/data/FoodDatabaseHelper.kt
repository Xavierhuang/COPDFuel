package com.copdhealthtracker.data

import android.content.Context
import com.copdhealthtracker.data.model.FoodSearchResult
import com.copdhealthtracker.data.model.ServingSize
import com.copdhealthtracker.data.model.UserAddedFood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Helper class to load and search the local food database from fooddata.xlsx
 * Contains 18,563 foods from NCC Food Database with full nutritional data
 */
class FoodDatabaseHelper(private val context: Context) {
    
    private var foodsJson: JSONObject? = null
    private var foodsList: List<LocalFood>? = null
    private var userAddedFoodsList: List<LocalFood> = emptyList()
    private var categories: List<String> = emptyList()
    private var foodGroups: List<String> = emptyList()

    /**
     * Set user-added foods so they appear in text search results.
     * Call after loadDatabase() when opening the Add Food dialog.
     */
    fun setUserAddedFoods(foods: List<UserAddedFood>) {
        userAddedFoodsList = foods.map { u ->
            LocalFood(
                id = -u.id.toInt(),
                name = u.name,
                shortName = u.shortName,
                category = u.category,
                categoryGroup = u.categoryGroup,
                calories = u.calories,
                protein = u.protein,
                carbs = u.carbs,
                fat = u.fat,
                fiber = u.fiber,
                sodium = u.sodium,
                potassium = u.potassium,
                calcium = u.calcium,
                iron = u.iron,
                magnesium = u.magnesium,
                zinc = u.zinc,
                selenium = u.selenium,
                manganese = u.manganese,
                water = u.water,
                vitaminA = u.vitaminA,
                vitaminC = u.vitaminC,
                vitaminD = u.vitaminD,
                vitaminE = u.vitaminE,
                vitaminK = u.vitaminK,
                saturatedFat = u.saturatedFat,
                cholesterol = u.cholesterol,
                omega3 = u.omega3,
                addedSugars = u.addedSugars,
                portionSize = u.portionSize,
                portionUnit = u.portionUnit,
                portionDesc = u.portionDesc,
                servingSizes = listOf(
                    LocalServingSize("100g", 100.0, 1.0, "g", true, false)
                )
            )
        }
    }
    
    data class LocalServingSize(
        val label: String,
        val grams: Double,
        val amount: Double,
        val unit: String,
        val isPrimary: Boolean,
        val isCustom: Boolean
    )
    
    data class LocalFood(
        val id: Int,
        val name: String,
        val shortName: String,
        val category: String,
        val categoryGroup: String,
        
        // Macros per 100g
        val calories: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
        val fiber: Double,
        
        // Minerals per 100g
        val sodium: Double,
        val potassium: Double,
        val calcium: Double,
        val iron: Double,
        val magnesium: Double,
        val zinc: Double,
        val selenium: Double,
        val manganese: Double,
        
        // Water per 100g
        val water: Double,
        
        // Vitamins per 100g
        val vitaminA: Double,
        val vitaminC: Double,
        val vitaminD: Double,
        val vitaminE: Double,
        val vitaminK: Double,
        
        // Fats per 100g
        val saturatedFat: Double,
        val cholesterol: Double,
        val omega3: Double,
        
        // Sugars per 100g
        val addedSugars: Double,
        
        // Serving info
        val portionSize: Double,
        val portionUnit: String,
        val portionDesc: String,
        val servingSizes: List<LocalServingSize>
    )
    
    suspend fun loadDatabase() = withContext(Dispatchers.IO) {
        if (foodsList != null) return@withContext
        
        try {
            val inputStream = context.assets.open("food_database.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            foodsJson = json
            
            // Parse categories
            val categoriesArray = json.getJSONArray("categories")
            categories = (0 until categoriesArray.length()).map { categoriesArray.getString(it) }
            
            // Parse food groups
            val foodGroupsArray = json.getJSONArray("foodGroups")
            foodGroups = (0 until foodGroupsArray.length()).map { foodGroupsArray.getString(it) }
            
            // Parse foods
            val foodsArray = json.getJSONArray("foods")
            val foods = mutableListOf<LocalFood>()
            
            for (i in 0 until foodsArray.length()) {
                val foodObj = foodsArray.getJSONObject(i)
                
                // Parse serving sizes array
                val servingSizesList = mutableListOf<LocalServingSize>()
                val servingSizesArray = foodObj.optJSONArray("servingSizes")
                if (servingSizesArray != null) {
                    for (j in 0 until servingSizesArray.length()) {
                        val ssObj = servingSizesArray.getJSONObject(j)
                        servingSizesList.add(LocalServingSize(
                            label = ssObj.optString("label", ""),
                            grams = ssObj.optDouble("grams", 100.0),
                            amount = ssObj.optDouble("amount", 1.0),
                            unit = ssObj.optString("unit", "g"),
                            isPrimary = ssObj.optBoolean("isPrimary", false),
                            isCustom = ssObj.optBoolean("isCustom", false)
                        ))
                    }
                }
                
                foods.add(LocalFood(
                    id = foodObj.optInt("id", 0),
                    name = foodObj.optString("name", ""),
                    shortName = foodObj.optString("shortName", ""),
                    category = foodObj.optString("category", ""),
                    categoryGroup = foodObj.optString("categoryGroup", ""),
                    
                    // Macros
                    calories = foodObj.optDouble("calories", 0.0),
                    protein = foodObj.optDouble("protein", 0.0),
                    carbs = foodObj.optDouble("carbs", 0.0),
                    fat = foodObj.optDouble("fat", 0.0),
                    fiber = foodObj.optDouble("fiber", 0.0),
                    
                    // Minerals
                    sodium = foodObj.optDouble("sodium", 0.0),
                    potassium = foodObj.optDouble("potassium", 0.0),
                    calcium = foodObj.optDouble("calcium", 0.0),
                    iron = foodObj.optDouble("iron", 0.0),
                    magnesium = foodObj.optDouble("magnesium", 0.0),
                    zinc = foodObj.optDouble("zinc", 0.0),
                    selenium = foodObj.optDouble("selenium", 0.0),
                    manganese = foodObj.optDouble("manganese", 0.0),
                    
                    // Water
                    water = foodObj.optDouble("water", 0.0),
                    
                    // Vitamins
                    vitaminA = foodObj.optDouble("vitaminA", 0.0),
                    vitaminC = foodObj.optDouble("vitaminC", 0.0),
                    vitaminD = foodObj.optDouble("vitaminD", 0.0),
                    vitaminE = foodObj.optDouble("vitaminE", 0.0),
                    vitaminK = foodObj.optDouble("vitaminK", 0.0),
                    
                    // Fats
                    saturatedFat = foodObj.optDouble("saturatedFat", 0.0),
                    cholesterol = foodObj.optDouble("cholesterol", 0.0),
                    omega3 = foodObj.optDouble("omega3", 0.0),
                    
                    // Sugars
                    addedSugars = foodObj.optDouble("addedSugars", 0.0),
                    
                    // Serving info
                    portionSize = foodObj.optDouble("portionSize", 100.0),
                    portionUnit = foodObj.optString("portionUnit", "g"),
                    portionDesc = foodObj.optString("portionDesc", ""),
                    servingSizes = servingSizesList
                ))
            }
            
            foodsList = foods
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getCategories(): List<String> = categories
    
    fun getFoodGroups(): List<String> = foodGroups
    
    /**
     * Search foods by name (case-insensitive).
     * Searches both the main database and user-added foods.
     * Returns up to 20 results with all nutrients.
     */
    fun searchFoods(query: String, limit: Int = 20): List<FoodSearchResult> {
        val queryLower = query.lowercase().trim()
        if (queryLower.isEmpty()) return emptyList()

        val queryWords = queryLower.split(" ").filter { it.isNotEmpty() }
        fun matches(food: LocalFood): Boolean {
            val nameLower = food.name.lowercase()
            val shortNameLower = food.shortName.lowercase()
            return queryWords.all { word ->
                nameLower.contains(word) || shortNameLower.contains(word)
            }
        }

        val fromMain = (foodsList ?: emptyList()).filter { matches(it) }.take(limit)
        val fromUser = userAddedFoodsList.filter { matches(it) }.take(limit)
        val combined = (fromUser + fromMain).distinct().take(limit)
        return combined.map { foodToSearchResult(it) }
    }
    
    /**
     * Search foods by category
     */
    fun searchByCategory(categoryGroup: String, limit: Int = 50): List<FoodSearchResult> {
        val foods = foodsList ?: return emptyList()
        
        return foods
            .filter { it.categoryGroup.equals(categoryGroup, ignoreCase = true) }
            .take(limit)
            .map { food -> foodToSearchResult(food) }
    }
    
    /**
     * Search foods by food group
     */
    fun searchByFoodGroup(foodGroup: String, limit: Int = 50): List<FoodSearchResult> {
        val foods = foodsList ?: return emptyList()
        
        return foods
            .filter { it.category.equals(foodGroup, ignoreCase = true) }
            .take(limit)
            .map { food -> foodToSearchResult(food) }
    }
    
    private fun foodToSearchResult(food: LocalFood): FoodSearchResult {
        return FoodSearchResult(
            fdcId = food.id,
            description = food.name,
            brandOwner = food.categoryGroup,
            
            // Macros
            calories = food.calories,
            protein = food.protein,
            carbs = food.carbs,
            fat = food.fat,
            fiber = food.fiber,
            
            // Minerals
            sodium = food.sodium,
            potassium = food.potassium,
            calcium = food.calcium,
            iron = food.iron,
            magnesium = food.magnesium,
            zinc = food.zinc,
            selenium = food.selenium,
            manganese = food.manganese,
            
            // Water
            water = food.water,
            
            // Vitamins
            vitaminA = food.vitaminA,
            vitaminC = food.vitaminC,
            vitaminD = food.vitaminD,
            vitaminE = food.vitaminE,
            vitaminK = food.vitaminK,
            
            // Fats
            saturatedFat = food.saturatedFat,
            cholesterol = food.cholesterol,
            omega3 = food.omega3,
            
            // Sugars
            addedSugars = food.addedSugars,
            
            // Serving info
            servingSize = food.portionSize,
            servingUnit = if (food.portionDesc.isNotEmpty()) food.portionDesc else "${food.portionSize.toInt()}${food.portionUnit}",
            servingSizes = food.servingSizes.map { ss ->
                ServingSize(
                    label = ss.label,
                    grams = ss.grams,
                    amount = ss.amount,
                    unit = ss.unit,
                    isPrimary = ss.isPrimary,
                    isCustom = ss.isCustom
                )
            }
        )
    }
    
    fun getTotalFoodsCount(): Int = (foodsList?.size ?: 0) + userAddedFoodsList.size
}
