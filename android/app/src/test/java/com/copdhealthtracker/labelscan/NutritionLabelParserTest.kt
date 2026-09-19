package com.copdhealthtracker.labelscan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionLabelParserTest {

    @Test
    fun `parses standard single-column label`() {
        val text = """
            Nutrition Facts
            Serving Size 2/3 cup (55g)
            Calories 230
            Total Fat 8g
            Total Carbohydrate 37g
            Protein 3g
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals("2/3 cup (55g)", result.servingSize?.description)
        assertEquals(55.0, result.servingSize?.grams)
        assertEquals(230.0, result.calories?.value)
        assertEquals(8.0, result.fat?.value)
        assertEquals(37.0, result.carbs?.value)
        assertEquals(3.0, result.protein?.value)
        assertEquals(Confidence.HIGH, result.fat?.confidence)
    }

    @Test
    fun `prefers unit over percent daily value`() {
        val text = """
            Total Fat 8g 10%
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals(8.0, result.fat?.value)
        assertEquals("g", result.fat?.unit)
    }

    @Test
    fun `handles missing unit with low confidence`() {
        val text = """
            Protein 5
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals(5.0, result.protein?.value)
        assertEquals(Confidence.LOW, result.protein?.confidence)
    }

    @Test
    fun `fixes common ocr substitutions`() {
        val text = """
            Calories 1OO
            Tota1 Fat 8g
            Prote1n 5g
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals(100.0, result.calories?.value)
        assertEquals(8.0, result.fat?.value)
        assertEquals(5.0, result.protein?.value)
    }

    @Test
    fun `serving size without grams has null grams`() {
        val text = """
            Serving Size 2 slices
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals("2 slices", result.servingSize?.description)
        assertNull(result.servingSize?.grams)
    }
}
