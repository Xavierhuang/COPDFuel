package com.copdhealthtracker.labelscan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

class UsdaGtinLookup {

    suspend fun lookup(gtin: String, apiKey: String): ParsedLabel? = withContext(Dispatchers.IO) {
        val url = "https://api.nal.usda.gov/fdc/v1/foods/search?query=${URLEncoder.encode(gtin, "UTF-8")}&dataType=Branded&pageSize=1&api_key=$apiKey"
        val response = try {
            URL(url).readText()
        } catch (e: Exception) {
            return@withContext null
        }
        mapResponse(response)
    }

    fun mapResponse(response: String): ParsedLabel? {
        val json = JSONObject(response)
        val foods = json.optJSONArray("foods") ?: return null
        if (foods.length() == 0) return null

        val food = foods.getJSONObject(0)
        val description = food.optString("description", "Scanned food")
        val servingSize = food.optDouble("servingSize", 100.0)
        val servingUnit = food.optString("servingSizeUnit", "g")
        val servingDesc = "$servingSize $servingUnit"

        var calories = 0.0
        var protein = 0.0
        var carbs = 0.0
        var fat = 0.0

        val nutrients = food.optJSONArray("foodNutrients")
        if (nutrients != null) {
            for (i in 0 until nutrients.length()) {
                val nutrient = nutrients.getJSONObject(i)
                val nutrientId = nutrient.optInt("nutrientId", 0)
                val value = nutrient.optDouble("value", 0.0)
                when (nutrientId) {
                    1008 -> calories = value
                    1003 -> protein = value
                    1005 -> carbs = value
                    1004 -> fat = value
                }
            }
        }

        return ParsedLabel(
            productName = description,
            servingSize = ServingSize(servingDesc, if (servingUnit == "g") servingSize else null),
            calories = ValueWithConfidence(calories, "kcal", Confidence.HIGH),
            protein = ValueWithConfidence(protein, "g", Confidence.HIGH),
            carbs = ValueWithConfidence(carbs, "g", Confidence.HIGH),
            fat = ValueWithConfidence(fat, "g", Confidence.HIGH)
        )
    }
}
