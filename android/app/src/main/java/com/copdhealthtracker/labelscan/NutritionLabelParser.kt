package com.copdhealthtracker.labelscan

object NutritionLabelParser {

    fun parse(text: String): ParsedLabel {
        val rawLines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val normalized = rawLines.joinToString("\n") { normalizeLine(it) }

        val servingSize = extractServingSize(normalized)

        return ParsedLabel(
            productName = null,
            servingSize = servingSize,
            calories = extractValue(normalized, "calories", "kcal"),
            protein = extractValue(normalized, "protein", "g"),
            carbs = extractValue(normalized, listOf("total carbohydrate", "total carbs", "carbohydrate"), "g"),
            fat = extractValue(normalized, listOf("total fat", "total lipid"), "g"),
            rawLines = rawLines
        )
    }

    private fun normalizeLine(line: String): String {
        return line
            .replace(Regex("\\s+"), " ")
            .replace(Regex("(?<=\\d)[oO](?=\\d|\\s|g|mg|kcal|$)"), "0")
            .replace(Regex("(?<=^|\\s)[lL](?=\\d)"), "1")
            .replace(Regex("(?<=\\d)[sS](?=\\s|g|mg|kcal|$)"), "5")
            .lowercase()
    }

    private fun extractServingSize(text: String): ServingSize? {
        val regex = Regex("serving size\\s+(.+?)(?=\\n|\\$)", RegexOption.IGNORE_CASE)
        val match = regex.find(text) ?: return null
        val description = match.groupValues[1].trim()
        val grams = Regex("\\((\\d+(?:\\.\\d+)?)\\s*g\\)", RegexOption.IGNORE_CASE)
            .find(description)?.groupValues?.get(1)?.toDoubleOrNull()
        return ServingSize(description, grams)
    }

    private fun extractValue(text: String, keyword: String, unit: String): ValueWithConfidence? {
        return extractValue(text, listOf(keyword), unit)
    }

    private fun extractValue(text: String, keywords: List<String>, unit: String): ValueWithConfidence? {
        val lines = text.lines()
        for (keyword in keywords) {
            val line = lines.firstOrNull { it.contains(keyword, ignoreCase = true) } ?: continue
            val result = parseNumericValue(line, unit)
            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun parseNumericValue(line: String, unit: String): ValueWithConfidence? {
        // Prefer a number followed by the unit (g, mg, kcal)
        val unitRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*($unit|mg)", RegexOption.IGNORE_CASE)
        val unitMatch = unitRegex.find(line)
        if (unitMatch != null) {
            val value = unitMatch.groupValues[1].toDoubleOrNull() ?: return null
            val detectedUnit = unitMatch.groupValues[2].lowercase()
            return ValueWithConfidence(value, detectedUnit, Confidence.HIGH)
        }

        // Fallback: first number on the line
        val numberRegex = Regex("(\\d+(?:\\.\\d+)?)")
        val numberMatch = numberRegex.find(line)
        if (numberMatch != null) {
            val value = numberMatch.groupValues[1].toDoubleOrNull() ?: return null
            return ValueWithConfidence(value, unit, Confidence.LOW)
        }

        return null
    }
}
