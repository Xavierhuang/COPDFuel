package com.copdhealthtracker.labelscan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

class UsdaGtinLookup {

    suspend fun lookup(gtin: String, apiKey: String): ParsedLabel? = withContext(Dispatchers.IO) {
        // FoodData Central stores some codes as 12 digits and others zero-padded to 14, and a
        // search only matches the exact stored form, so try each form of the scanned code.
        for (candidate in gtinCandidates(gtin)) {
            val url = "https://api.nal.usda.gov/fdc/v1/foods/search?query=${URLEncoder.encode(candidate, "UTF-8")}&dataType=Branded&pageSize=1&api_key=$apiKey"
            val response = URL(url).readText()
            mapResponse(response, gtin)?.let { return@withContext it }
        }
        null
    }

    fun mapResponse(response: String, expectedGtin: String? = null): ParsedLabel? {
        val json = JSONObject(response)
        val foods = json.optJSONArray("foods") ?: return null
        if (foods.length() == 0) return null

        val food = foods.getJSONObject(0)
        // The search is full-text, so make sure the hit really is the scanned product.
        if (expectedGtin != null && !sameGtin(food.optString("gtinUpc", ""), expectedGtin)) return null

        val description = productName(food.optString("description", ""), food.optString("brandName", ""))
        val servingSize = food.optDouble("servingSize", 100.0)
        val servingUnit = food.optString("servingSizeUnit", "g")
        val isGrams = servingUnit.lowercase() in listOf("g", "grm", "gram", "grams")
        val servingDesc = "${formatAmount(servingSize)} ${if (isGrams) "g" else servingUnit.lowercase()}"

        // Branded search results report nutrients per 100 g (or 100 ml), not per serving.
        val perServing = servingSize / 100.0

        var calories = 0.0
        var protein = 0.0
        var carbs = 0.0
        var fat = 0.0

        val per100gById = mutableMapOf<Int, Double>()

        val nutrients = food.optJSONArray("foodNutrients")
        if (nutrients != null) {
            for (i in 0 until nutrients.length()) {
                val nutrient = nutrients.getJSONObject(i)
                val nutrientId = nutrient.optInt("nutrientId", 0)
                per100gById[nutrientId] = nutrient.optDouble("value", 0.0)
                val value = roundToTenth(nutrient.optDouble("value", 0.0) * perServing)
                when (nutrientId) {
                    1008 -> calories = value
                    1003 -> protein = value
                    1005 -> carbs = value
                    1004 -> fat = value
                }
            }
        }

        // Only nutrients USDA actually reports, so a missing value is not shown as zero.
        val extras = ExtraNutrient.values().mapNotNull { extra ->
            val id = extra.usdaIds.firstOrNull { it in per100gById } ?: return@mapNotNull null
            var amount = per100gById.getValue(id) * perServing
            if (id == ExtraNutrient.USDA_VITAMIN_D_IU) amount *= ExtraNutrient.MCG_PER_IU_VITAMIN_D
            extra to ValueWithConfidence(roundToTenth(amount), extra.unit, Confidence.HIGH)
        }.toMap()

        return ParsedLabel(
            productName = description,
            servingSize = ServingSize(servingDesc, if (isGrams) servingSize else null),
            calories = ValueWithConfidence(calories, "kcal", Confidence.HIGH),
            protein = ValueWithConfidence(protein, "g", Confidence.HIGH),
            carbs = ValueWithConfidence(carbs, "g", Confidence.HIGH),
            fat = ValueWithConfidence(fat, "g", Confidence.HIGH),
            extras = extras
        )
    }

    private fun roundToTenth(value: Double): Double = (value * 10).roundToInt() / 10.0

    private fun formatAmount(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    companion object {
        /**
         * USDA descriptions are upper case, often repeat the flavour ("X OATMEAL, MAPLE & BROWN
         * SUGAR") and leave the brand in a separate field. Builds the name a person would expect.
         */
        fun productName(description: String, brandName: String?): String {
            val parts = mutableListOf<String>()
            for (part in description.split(",").map { it.trim() }.filter { it.isNotEmpty() }) {
                val alreadySaid = parts.joinToString(" ").lowercase().split(Regex("\\W+")).toSet()
                val words = part.lowercase().split(Regex("\\W+")).filter { it.isNotEmpty() }
                if (parts.isEmpty() || !alreadySaid.containsAll(words)) parts.add(part)
            }
            val name = ProductNames.readable(parts.joinToString(", "))
            val brand = ProductNames.readable(brandName.orEmpty().trim())
            return when {
                name.isEmpty() && brand.isEmpty() -> "Scanned food"
                brand.isEmpty() || name.contains(brand, ignoreCase = true) -> name
                name.isEmpty() -> brand
                else -> "$brand $name"
            }
        }

        /** The scanned code first, then its 12, 13 and 14 digit forms. */
        fun gtinCandidates(gtin: String): List<String> {
            val core = gtin.trimStart('0')
            return (listOf(gtin) + listOf(12, 13, 14).filter { it >= core.length }.map { core.padStart(it, '0') })
                .distinct()
        }

        fun sameGtin(a: String, b: String): Boolean =
            a.isNotBlank() && a.trimStart('0') == b.trimStart('0')
    }
}
