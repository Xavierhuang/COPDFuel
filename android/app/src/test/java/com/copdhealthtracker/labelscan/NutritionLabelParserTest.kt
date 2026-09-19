package com.copdhealthtracker.labelscan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `parses abbreviated total carb`() {
        val text = """
            Sodium 290mg 12%
            Total Carb. 40g 15%
            Dietary Fiber 4g
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals(40.0, result.carbs?.value)
        assertEquals(Confidence.HIGH, result.carbs?.confidence)
    }

    @Test
    fun `reads serving grams when ocr mangles the closing paren`() {
        val text = """
            Serving size 1 package (60g|
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals(60.0, result.servingSize?.grams)
    }

    @Test
    fun `calories without a unit is high confidence`() {
        val text = """
            Calories 220
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals(220.0, result.calories?.value)
        assertEquals(Confidence.HIGH, result.calories?.confidence)
    }

    @Test
    fun `product name is first line of front label`() {
        val text = """

            Quaker Oatmeal
            Maple Brown Sugar
        """.trimIndent()

        assertEquals("Quaker Oatmeal", NutritionLabelParser.extractProductName(text))
    }

    @Test
    fun `nutrition panel is not used as product name`() {
        val text = """
            Nutrition Facts
            Serving size 1 package (60g)
            Calories 220
        """.trimIndent()

        assertNull(NutritionLabelParser.extractProductName(text))
    }

    // Rows as printed on a Quaker protein oatmeal cup; vitamins and minerals share lines.
    private val fullLabel = """
        Nutrition Facts
        Serving size 1 package (60g)
        Calories 220
        Total Fat 3.5g 4%
        Saturated Fat 0.5g 4%
        Trans Fat 0g
        Cholesterol 5mg 2%
        Sodium 290mg 12%
        Total Carb. 40g 15%
        Dietary Fiber 4g 14%
        Total Sugars 12g
        Incl. 11g Added Sugars 22%
        Protein 10g 17%
        Vitamin D 0.1mcg 0% Calcium 40mg 4%
        Iron 1.6mg 8% Potassium 180mg 4%
    """.trimIndent()

    @Test
    fun `reads the extra nutrient rows`() {
        val extras = NutritionLabelParser.parse(fullLabel).extras

        assertEquals(4.0, extras[ExtraNutrient.FIBER]?.value)
        assertEquals(11.0, extras[ExtraNutrient.ADDED_SUGARS]?.value)
        assertEquals(0.5, extras[ExtraNutrient.SATURATED_FAT]?.value)
        assertEquals(5.0, extras[ExtraNutrient.CHOLESTEROL]?.value)
        assertEquals(290.0, extras[ExtraNutrient.SODIUM]?.value)
    }

    @Test
    fun `reads each nutrient when several share a line`() {
        val extras = NutritionLabelParser.parse(fullLabel).extras

        assertEquals(0.1, extras[ExtraNutrient.VITAMIN_D]?.value)
        assertEquals(40.0, extras[ExtraNutrient.CALCIUM]?.value)
        assertEquals(1.6, extras[ExtraNutrient.IRON]?.value)
        assertEquals(180.0, extras[ExtraNutrient.POTASSIUM]?.value)
    }

    @Test
    fun `extra rows do not disturb the macros`() {
        val result = NutritionLabelParser.parse(fullLabel)

        assertEquals(3.5, result.fat?.value)
        assertEquals(40.0, result.carbs?.value)
        assertEquals(10.0, result.protein?.value)
        assertEquals(220.0, result.calories?.value)
    }

    @Test
    fun `missing rows are left out rather than zero`() {
        val extras = NutritionLabelParser.parse("Calories 100\nProtein 3g").extras

        assertTrue(extras.isEmpty())
    }

    @Test
    fun `reads a leading letter O as zero in decimals`() {
        val extras = NutritionLabelParser.parse("Saturated Fat O.5g 4%").extras

        assertEquals(0.5, extras[ExtraNutrient.SATURATED_FAT]?.value)
    }

    @Test
    fun `converts a value printed in the other mass unit`() {
        val extras = NutritionLabelParser.parse("Sodium 0.29g").extras

        assertEquals(290.0, extras[ExtraNutrient.SODIUM]?.value)
        assertEquals("mg", extras[ExtraNutrient.SODIUM]?.unit)
    }

    @Test
    fun `ignores nutrient words inside the ingredients list`() {
        val text = """
            Ingredients: whole grain oats, sugar, calcium carbonate, salt, reduced iron.
            Calcium 40mg 4%
            Iron 1.6mg 8%
        """.trimIndent()

        val extras = NutritionLabelParser.parse(text).extras

        assertEquals(40.0, extras[ExtraNutrient.CALCIUM]?.value)
        assertEquals(1.6, extras[ExtraNutrient.IRON]?.value)
    }

    @Test
    fun `a percent daily value is never read as an amount`() {
        // The recognizer misread "11g" as "1fg", leaving only the 22% on the row.
        val extras = NutritionLabelParser.parse("Incl 1fg Added Sugars 22%").extras

        assertNull(extras[ExtraNutrient.ADDED_SUGARS])
    }

    @Test
    fun `polyunsaturated fat is not mistaken for saturated fat`() {
        val text = """
            Polyunsaturated Fat 1g
            MonounsaturatedFat 1g
            Saturated Fat 0.5g
        """.trimIndent()

        assertEquals(0.5, NutritionLabelParser.parse(text).extras[ExtraNutrient.SATURATED_FAT]?.value)
    }

    @Test
    fun `parses the text actually recognized from a photographed oatmeal cup`() {
        // Verbatim ML Kit output for a Quaker protein oatmeal cup, misreads included.
        val text = """
            Nutrition Factse
            1 serving per container
            Serving size 1 package (60g|s
            Amount per serving
            Calories 220
            Total Fat 3.5g
            Saturated Fat 0.5g
            Trans Fat 0g
            Polyunsaturated Fat 1g
            MonounsaturatedFat 1g
            Cholesterol 5mg
            AWT Sodium 290mg
            Total Carb. 40g
            Dietary Fiber 4g
            % Daily Value
            FEANOR STWEN
            4%
            29%
            12%
            15%
            14%
            Total Sugars 12g
            Incl 1fg Added Sugars 22%
            Protein 10g
            17%
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals(60.0, result.servingSize?.grams)
        assertEquals(220.0, result.calories?.value)
        assertEquals(3.5, result.fat?.value)
        assertEquals(40.0, result.carbs?.value)
        assertEquals(10.0, result.protein?.value)
        assertEquals(4.0, result.extras[ExtraNutrient.FIBER]?.value)
        assertEquals(0.5, result.extras[ExtraNutrient.SATURATED_FAT]?.value)
        assertEquals(5.0, result.extras[ExtraNutrient.CHOLESTEROL]?.value)
        assertEquals(290.0, result.extras[ExtraNutrient.SODIUM]?.value)
        assertNull(result.extras[ExtraNutrient.ADDED_SUGARS])
        assertNull(result.extras[ExtraNutrient.POTASSIUM])
    }

    @Test
    fun `reads a nutrient the recognizer glued onto another word`() {
        // From a real scan: the "220 CALORIES" badge next to the panel ran into the Protein row.
        assertEquals(10.0, NutritionLabelParser.parse("DALORIESProtein 10g").protein?.value)
    }

    @Test
    fun `reads serving grams followed by recognizer junk`() {
        assertEquals(60.0, NutritionLabelParser.parse("Serving size 1 package (60ges").servingSize?.grams)
    }

    @Test
    fun `parses a second real scan of the oatmeal cup`() {
        // Verbatim ML Kit output from the phone, 2026-09-18 22:49.
        val text = """
            WEREY
            Helps
            SATISFY
            0 NET WI
            Nutrition Facts
            1 serving per container
            Serving size 1 package (60ges
            Amount per serving
            Calories 220:
            Total Fat 3.5g
            Saturated Fat 0.5g
            Trans Fat Og
            Polyunsaturated Fat 1g
            Monounsaturated Fat 1g
            % Daily Value AOR, SOY LETAN
            Cholesterol 5mg
            Sodium 290mg
            Total Carb. 40g
            Dietary Fiber 4g
            Total Sugars 12g
            Incl. 11g Added Sugars
            DALORIESProtein 10g
            CONTAINS MILK NNST
            2%
            12%
            INGREDEIS: MWME RINV
            15%
            14%
            22% We're har ibh
            17% Please have pacag a
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals(60.0, result.servingSize?.grams)
        assertEquals(220.0, result.calories?.value)
        assertEquals(10.0, result.protein?.value)
        assertEquals(40.0, result.carbs?.value)
        assertEquals(3.5, result.fat?.value)
        assertEquals(0.5, result.extras[ExtraNutrient.SATURATED_FAT]?.value)
        assertEquals(11.0, result.extras[ExtraNutrient.ADDED_SUGARS]?.value)
        assertEquals(4.0, result.extras[ExtraNutrient.FIBER]?.value)
        assertEquals(290.0, result.extras[ExtraNutrient.SODIUM]?.value)
    }
}
