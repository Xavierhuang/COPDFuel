package com.copdhealthtracker.labelscan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrontLabelNameTest {

    // Real ML Kit output for the front of a Quaker protein oatmeal cup, in the order reported.
    private val quakerFront = listOf(
        OcrLine("QUAKER", 213, 1774),
        OcrLine("-ESTP 1877-", 58, 1984),
        OcrLine("PROTEM", 40, 1468),
        OcrLine("per serving", 38, 1520),
        OcrLine("INSTANT OATMEAL", 107, 2095),
        OcrLine("PROTEIN", 160, 2235),
        OcrLine("MAPLE &", 113, 2451),
        OcrLine("BROWN SUGAR", 131, 2588),
        OcrLine("KANOR WITH OTHER NATURAL FLAVORN", 88, 2756)
    )

    @Test
    fun `builds the name from the prominent lines of the package front`() {
        assertEquals(
            "Quaker Instant Oatmeal Protein Maple & Brown Sugar",
            NutritionLabelParser.extractProductName(quakerFront)
        )
    }

    @Test
    fun `reads the prominent lines top to bottom whatever order they arrive in`() {
        assertEquals(
            "Quaker Instant Oatmeal Protein Maple & Brown Sugar",
            NutritionLabelParser.extractProductName(quakerFront.reversed())
        )
    }

    @Test
    fun `leaves out large small-print phrases`() {
        val lines = listOf(
            OcrLine("CHEERIOS", 200, 100),
            OcrLine("NET WT 8.9 OZ", 150, 900),
            OcrLine("NATURALLY FLAVORED", 140, 500),
            OcrLine("Honey Nut", 130, 300)
        )

        assertEquals("Cheerios Honey Nut", NutritionLabelParser.extractProductName(lines))
    }

    @Test
    fun `a nutrition panel photographed as the front gives no name`() {
        val lines = listOf(OcrLine("Nutrition Facts", 120, 10), OcrLine("Serving size 1 package (60g)", 40, 140))

        assertNull(NutritionLabelParser.extractProductName(lines))
    }

    @Test
    fun `falls back to the first line when sizes are unknown`() {
        val lines = listOf(OcrLine("Quaker Oatmeal", 0, 0), OcrLine("Maple Brown Sugar", 0, 0))

        assertEquals("Quaker Oatmeal", NutritionLabelParser.extractProductName(lines))
    }

    @Test
    fun `nothing readable gives no name`() {
        assertNull(NutritionLabelParser.extractProductName(emptyList<OcrLine>()))
    }
}
