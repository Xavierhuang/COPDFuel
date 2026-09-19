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
    private var nameIndex: MutableMap<String, MutableList<Int>> = mutableMapOf()
    private var indexedFoods: List<LocalFood> = emptyList()

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
            indexedFoods = foods
            buildSearchIndex()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildSearchIndex() {
        nameIndex.clear()

        indexedFoods.forEachIndexed { index, food ->
            val nameLower = food.name.lowercase()
            val shortNameLower = food.shortName.lowercase()

            val words = extractWords(nameLower)
            val shortWords = extractWords(shortNameLower)
            val allWords = (words + shortWords).distinct()

            for (word in allWords) {
                if (!nameIndex.containsKey(word)) {
                    nameIndex[word] = mutableListOf()
                }
                nameIndex[word]?.add(index)
            }
        }
    }

    private fun extractWords(text: String): List<String> {
        val separators = charArrayOf(' ', ',', '-', '_', '/', '(', ')', '[', ']', '{', '}')
        return text.split(*separators)
            .filter { it.length > 1 }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun getCategories(): List<String> = categories

    fun getFoodGroups(): List<String> = foodGroups

    /**
     * Search foods by name (case-insensitive).
     * Searches both the main database and user-added foods.
     * Returns up to 20 results with all nutrients.
     */
    fun searchFoods(
        query: String,
        categoryFilter: String? = null,
        foodGroupFilter: String? = null,
        limit: Int = 20
    ): List<FoodSearchResult> {
        val queryLower = query.lowercase().trim()
        val searchWords = if (queryLower.isNotEmpty()) {
            queryLower.split(" ").filter { it.isNotEmpty() && it.length > 1 }
        } else {
            emptyList()
        }
        val hasQuery = searchWords.isNotEmpty()

        // If no query and no filters, return empty
        if (!hasQuery && categoryFilter == null && foodGroupFilter == null) {
            return emptyList()
        }

        // Get matching indices from search index if there's a query
        var matchingIndices: Set<Int>? = null
        if (hasQuery) {
            for (word in searchWords) {
                val indices = nameIndex[word]
                if (indices != null) {
                    val wordSet = indices.toSet()
                    matchingIndices = if (matchingIndices == null) {
                        wordSet
                    } else {
                        matchingIndices?.intersect(wordSet)
                    }
                } else {
                    matchingIndices = emptySet()
                    break
                }
            }
        }

        // Collect candidate foods with scores
        val candidates = mutableListOf<Pair<LocalFood, Int>>()

        // Add main database foods
        val foodsToScore = if (hasQuery && matchingIndices != null) {
            matchingIndices.mapNotNull { indexedFoods.getOrNull(it) }
        } else if (!hasQuery) {
            indexedFoods
        } else {
            emptyList()
        }

        for (food in foodsToScore) {
            // Apply category filter
            if (categoryFilter != null && categoryFilter != "All Categories" &&
                !food.categoryGroup.equals(categoryFilter, ignoreCase = true)) {
                continue
            }

            // Apply food group filter
            if (foodGroupFilter != null && foodGroupFilter != "All Food Groups" &&
                !food.category.equals(foodGroupFilter, ignoreCase = true)) {
                continue
            }

            val score = scoreFood(food, searchWords, hasQuery)
            if (score > 0 || !hasQuery) {
                candidates.add(Pair(food, score))
            }
        }

        // Add user-added foods
        for (userFood in userAddedFoodsList) {
            // Apply category filter
            if (categoryFilter != null && categoryFilter != "All Categories" &&
                !userFood.categoryGroup.equals(categoryFilter, ignoreCase = true)) {
                continue
            }

            // Apply food group filter
            if (foodGroupFilter != null && foodGroupFilter != "All Food Groups" &&
                !userFood.category.equals(foodGroupFilter, ignoreCase = true)) {
                continue
            }

            val nameLower = userFood.name.lowercase()
            val shortLower = userFood.shortName.lowercase()

            // Check if matches query
            if (hasQuery) {
                val matches = searchWords.all { word ->
                    nameLower.contains(word) || shortLower.contains(word)
                }
                if (!matches) continue
            }

            val score = scoreUserAddedFood(userFood, searchWords, hasQuery)
            candidates.add(Pair(userFood, score))
        }

        // Sort by score (higher is better)
        candidates.sortByDescending { it.second }

        // Return limited results
        return candidates.take(limit).map { foodToSearchResult(it.first) }
    }

    /**
     * Score a food from the main database for relevance
     * Matches iOS scoring logic
     */
    private fun scoreFood(food: LocalFood, searchWords: List<String>, hasQuery: Boolean): Int {
        val fullName = food.name.lowercase()

        if (!hasQuery) {
            // When no query, return a base score (shorter names get higher score)
            return maxOf(0, 200 - fullName.length)
        }

        var score = 0

        for (word in searchWords) {
            when {
                fullName == word -> score += 1000
                fullName.startsWith(word) -> score += 200
                fullName.contains(word) -> {
                    val index = fullName.indexOf(word)
                    score += when {
                        index < 10 -> 80
                        index < 20 -> 50
                        else -> 30
                    }
                }
                else -> score += 10
            }
        }

        // Bonus for shorter names (they tend to be more basic foods)
        score += maxOf(0, 100 - fullName.length)

        // Bonus for fresh/raw foods, penalty for processed
        if (hasQuery) {
            when {
                fullName.contains("fresh") || fullName.contains("ripe") || fullName.contains("raw") -> score += 50
                fullName.contains("dried") || fullName.contains("chips") ||
                        fullName.contains("fried") || fullName.contains("canned") -> score -= 30
            }
        }

        return score
    }

    /**
     * Score user-added food for relevance
     */
    private fun scoreUserAddedFood(food: LocalFood, searchWords: List<String>, hasQuery: Boolean): Int {
        val name = food.name.lowercase()

        if (!hasQuery) {
            return maxOf(0, 200 - name.length)
        }

        var score = 0

        for (word in searchWords) {
            when {
                name == word -> score += 1000
                name.startsWith(word) -> score += 200
                name.contains(word) -> score += 50
                else -> score += 10
            }
        }

        // User-added foods get a small bonus to appear in results
        score += 25
        score += maxOf(0, 100 - name.length)

        return score
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