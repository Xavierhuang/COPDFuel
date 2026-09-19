package com.copdhealthtracker.labelscan

object NutritionLabelParser {

    // On a real Quaker cup the name lines are 50-100% of the tallest line and the slanted
    // "flavor with other natural flavors" line is 41%.
    private const val PROMINENT_LINE_RATIO = 0.45
    private const val MAX_NAME_LINES = 6
    private val SMALL_PRINT = listOf(
        "net wt", "per serving", "servings per", "natural flavor", "naturally flavored",
        "artificial flavor", "artificially flavored", "with other"
    )

    fun parse(text: String): ParsedLabel {
        val rawLines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val normalized = rawLines.joinToString("\n") { normalizeLine(it) }

        val servingSize = extractServingSize(normalized)

        return ParsedLabel(
            productName = null,
            servingSize = servingSize,
            // US labels print calories with no unit, so a bare number is not a weak read.
            calories = extractValue(normalized, "calories", "kcal")?.copy(confidence = Confidence.HIGH),
            protein = extractValue(normalized, "protein", "g"),
            // "total carb" also covers "Total Carb.", "Total Carbs" and "Total Carbohydrate".
            carbs = extractValue(normalized, listOf("total carb", "carbohydrate"), "g"),
            fat = extractValue(normalized, listOf("total fat", "total lipid"), "g"),
            extras = ExtraNutrient.values().mapNotNull { extra ->
                extractValue(normalized, extra.keywords, extra.unit)?.let { extra to it }
            }.toMap(),
            rawLines = rawLines
        )
    }

    /** True when the text is a Nutrition Facts panel rather than the front of a package. */
    fun looksLikeNutritionPanel(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("nutrition facts") || lower.contains("serving size") ||
            (lower.contains("calories") && lower.contains("total fat"))
    }

    fun extractProductName(frontText: String): String? =
        extractProductName(frontText.lines().filter { it.isNotBlank() }.map { OcrLine(it.trim(), 0, 0) })

    /**
     * Names a product from a photo of the front of its package. The brand, product and flavour are
     * printed large; taglines, weights and claims are small, so keep the prominent lines only.
     */
    fun extractProductName(frontLines: List<OcrLine>): String? {
        if (looksLikeNutritionPanel(frontLines.joinToString("\n") { it.text })) return null
        val lines = frontLines.filter { line -> line.text.count { it.isLetter() } >= 2 }
        if (lines.isEmpty()) return null

        val tallest = lines.maxOf { it.height }
        if (tallest <= 0) return lines.first().text.trim()

        val name = lines
            .filter { it.height >= tallest * PROMINENT_LINE_RATIO }
            .filterNot { line -> SMALL_PRINT.any { line.text.contains(it, ignoreCase = true) } }
            .sortedBy { it.top }
            .take(MAX_NAME_LINES)
            .joinToString(" ") { ProductNames.readable(it.text.trim()) }
        return name.ifBlank { null }
    }

    private fun normalizeLine(line: String): String {
        val chars = line.toCharArray()
        val lastIndex = chars.lastIndex
        for (i in chars.indices) {
            when (chars[i].lowercaseChar()) {
                'o' -> {
                    val afterDigit = i > 0 && chars[i - 1].isDigit()
                    // "O.5g": a lone O in front of a decimal point is a zero.
                    val leadsDecimal = i + 2 <= lastIndex && chars[i + 1] == '.' && chars[i + 2].isDigit() &&
                        (i == 0 || !chars[i - 1].isLetter())
                    if (afterDigit || leadsDecimal) chars[i] = '0'
                }
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
        // OCR often misreads what follows the grams ("(60g|", "(60ges"). An opening paren is enough
        // to trust the number; otherwise require that the g ends the token.
        val grams = (Regex("\\(\\s*(\\d+(?:\\.\\d+)?)\\s*g", RegexOption.IGNORE_CASE).find(description)
            ?: Regex("(\\d+(?:\\.\\d+)?)\\s*g(?![a-z])", RegexOption.IGNORE_CASE).find(description))
            ?.groupValues?.get(1)?.toDoubleOrNull()
        return ServingSize(description, grams)
    }

    private fun extractValue(text: String, keyword: String, unit: String): ValueWithConfidence? {
        return extractValue(text, listOf(keyword), unit)
    }

    private fun extractValue(text: String, keywords: List<String>, unit: String): ValueWithConfidence? {
        val lines = text.lines()
        // Prefer the keyword at the start of a word. The recognizer sometimes glues a row onto the
        // previous word ("DALORIESProtein 10g"), so fall back to a match inside a word, but never
        // after "un": "Polyunsaturated Fat" is not "Saturated Fat".
        for (prefix in listOf("(?<![a-z])", "(?<!un)")) {
            for (keyword in keywords) {
                val pattern = Regex(prefix + Regex.escape(keyword), RegexOption.IGNORE_CASE)
                // The ingredients list can mention a nutrient ("calcium carbonate") before its row
                // does, so keep looking until a line actually carries an amount.
                for (line in lines) {
                    val match = pattern.find(line) ?: continue
                    val afterKeyword = line.substring(match.range.last + 1)
                    val result = parseNumericValue(line, afterKeyword, unit)
                    if (result != null) {
                        return result
                    }
                }
            }
        }
        return null
    }

    // Grams per unit, so a value printed in one mass unit can be stored in another.
    private val massUnits = mapOf("mcg" to 1e-6, "\u00b5g" to 1e-6, "ug" to 1e-6, "mg" to 1e-3, "g" to 1.0)

    private fun parseNumericValue(line: String, afterKeyword: String, unit: String): ValueWithConfidence? {
        val units = if (unit in massUnits) massUnits.keys.joinToString("|") else unit
        val unitRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*($units)(?![a-z])", RegexOption.IGNORE_CASE)

        // Vitamins and minerals often share a line ("Vitamin D 0.1mcg 0% Calcium 40mg 4%"), so
        // prefer the amount right after the keyword. Some rows put it first ("Incl. 11g Added
        // Sugars"), so fall back to the whole line.
        val unitMatch = unitRegex.find(afterKeyword) ?: unitRegex.find(line)
        if (unitMatch != null) {
            val value = unitMatch.groupValues[1].toDoubleOrNull() ?: return null
            val detectedUnit = unitMatch.groupValues[2].lowercase()
            val factor = (massUnits[detectedUnit] ?: 1.0) / (massUnits[unit] ?: 1.0)
            return ValueWithConfidence(Math.round(value * factor * 1000) / 1000.0, unit, Confidence.HIGH)
        }

        // Fallback: a bare number after the keyword. One followed by % is a Daily Value, not an amount.
        val numberMatch = Regex("(?<![\\d.])(\\d+(?:\\.\\d+)?)(?![\\d.]|\\s*%)").find(afterKeyword)
        if (numberMatch != null) {
            val value = numberMatch.groupValues[1].toDoubleOrNull() ?: return null
            return ValueWithConfidence(value, unit, Confidence.LOW)
        }

        return null
    }
}
