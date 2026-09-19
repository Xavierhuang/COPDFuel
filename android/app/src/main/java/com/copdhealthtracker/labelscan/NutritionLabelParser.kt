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
        val chars = line.toCharArray()
        val lastIndex = chars.lastIndex
        for (i in chars.indices) {
            when (chars[i].lowercaseChar()) {
                'o' -> if (i > 0 && chars[i - 1].isDigit()) chars[i] = '0'
                'l' -> if (i < lastIndex && chars[i + 1].isDigit()) chars[i] = '1'
                's' -> if (i > 0 && chars[i - 1].isDigit()) chars[i] = '5'
                '1' -> {
                    // Treat a lone 1 inside a word as 'i' (e.g. Prote1n) and a 1 at
                    // the end of a word as 'l' (e.g. Tota1).
                    val prevLetter = i > 0 && chars[i - 1].isLetter()
                    val nextLetter = i < lastIndex && chars[i + 1].isLetter()
                    val atWordEnd = i == lastIndex || chars[i + 1].isWhitespace()
                    if (prevLetter && nextLetter) {
                        chars[i] = 'i'
                    } else if (prevLetter && atWordEnd) {
                        chars[i] = 'l'
                    }
                }
            }
        }
        return String(chars)
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    private fun extractServingSize(text: String): ServingSize? {
        val regex = Regex("serving size\\s+(.+?)(?=\\n|\$)", RegexOption.IGNORE_CASE)
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
