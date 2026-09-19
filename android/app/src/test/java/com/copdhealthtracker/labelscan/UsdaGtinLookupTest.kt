package com.copdhealthtracker.labelscan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsdaGtinLookupTest {

    // Real FoodData Central search result for Quaker protein oatmeal (label: 220 kcal per 60 g cup).
    // The search endpoint reports branded nutrients per 100 g, not per serving.
    private fun quakerJson(unit: String = "g", gtinUpc: String = "030000570630") = """
        {
          "foods": [{
            "description": "MAPLE & BROWN SUGAR PROTEIN INSTANT OATMEAL, MAPLE & BROWN SUGAR",
            "brandName": "QUAKER",
            "gtinUpc": "$gtinUpc",
            "servingSize": 60.0,
            "servingSizeUnit": "$unit",
            "foodNutrients": [
              {"nutrientId": 1008, "value": 367},
              {"nutrientId": 1003, "value": 16.7},
              {"nutrientId": 1005, "value": 66.7},
              {"nutrientId": 1004, "value": 5.83},
              {"nutrientId": 1079, "value": 6.7},
              {"nutrientId": 1087, "value": 67.0},
              {"nutrientId": 1089, "value": 2.67},
              {"nutrientId": 1092, "value": 300},
              {"nutrientId": 1093, "value": 483},
              {"nutrientId": 1110, "value": 7.0, "unitName": "IU"},
              {"nutrientId": 1235, "value": 18.3},
              {"nutrientId": 1253, "value": 8.0},
              {"nutrientId": 1258, "value": 0.83},
              {"nutrientId": 2000, "value": 20.0}
            ]
          }]
        }
    """.trimIndent()

    @Test
    fun `scales per-100g USDA nutrients to one serving`() {
        val result = UsdaGtinLookup().mapResponse(quakerJson())

        assertEquals("Quaker Maple & Brown Sugar Protein Instant Oatmeal", result?.productName)
        assertEquals(220.2, result?.calories?.value)
        assertEquals(10.0, result?.protein?.value)
        assertEquals(40.0, result?.carbs?.value)
        assertEquals(3.5, result?.fat?.value)
        assertEquals(60.0, result?.servingSize?.grams)
    }

    @Test
    fun `treats GRM serving unit as grams`() {
        val result = UsdaGtinLookup().mapResponse(quakerJson(unit = "GRM"))

        assertEquals(60.0, result?.servingSize?.grams)
        assertEquals(220.2, result?.calories?.value)
    }

    @Test
    fun `accepts a hit whose gtin differs only by leading zeros`() {
        val result = UsdaGtinLookup().mapResponse(quakerJson(gtinUpc = "00030000570630"), "030000570630")

        assertNotNull(result)
    }

    @Test
    fun `rejects a hit for a different product`() {
        val result = UsdaGtinLookup().mapResponse(quakerJson(gtinUpc = "072036708748"), "030000570630")

        assertNull(result)
    }

    @Test
    fun `tries 12, 13 and 14 digit forms of a scanned code`() {
        val candidates = UsdaGtinLookup.gtinCandidates("0030000570630")

        assertEquals("0030000570630", candidates.first())
        assertTrue(candidates.contains("030000570630"))
        assertTrue(candidates.contains("00030000570630"))
    }

    @Test
    fun `maps the extra nutrients per serving`() {
        val extras = UsdaGtinLookup().mapResponse(quakerJson())!!.extras

        assertEquals(4.0, extras[ExtraNutrient.FIBER]?.value)
        assertEquals(11.0, extras[ExtraNutrient.ADDED_SUGARS]?.value)
        assertEquals(0.5, extras[ExtraNutrient.SATURATED_FAT]?.value)
        assertEquals(4.8, extras[ExtraNutrient.CHOLESTEROL]?.value)
        assertEquals(289.8, extras[ExtraNutrient.SODIUM]?.value)
        assertEquals(180.0, extras[ExtraNutrient.POTASSIUM]?.value)
        assertEquals(40.2, extras[ExtraNutrient.CALCIUM]?.value)
        assertEquals(1.6, extras[ExtraNutrient.IRON]?.value)
    }

    @Test
    fun `converts vitamin D from IU to micrograms`() {
        // 7 IU per 100 g -> 4.2 IU per 60 g serving -> 0.105 mcg
        val vitaminD = UsdaGtinLookup().mapResponse(quakerJson())!!.extras[ExtraNutrient.VITAMIN_D]

        assertEquals(0.1, vitaminD?.value)
        assertEquals("mcg", vitaminD?.unit)
    }

    @Test
    fun `leaves out nutrients USDA does not report`() {
        val json = """{"foods":[{"description":"Plain","servingSize":100.0,"servingSizeUnit":"g",
            "foodNutrients":[{"nutrientId":1008,"value":100}]}]}"""

        assertTrue(UsdaGtinLookup().mapResponse(json)!!.extras.isEmpty())
    }

    @Test
    fun `drops a flavour that only repeats the description`() {
        assertEquals(
            "Harris Teeter Maple & Brown Sugar Instant Oatmeal Cups",
            UsdaGtinLookup.productName("MAPLE & BROWN SUGAR INSTANT OATMEAL CUPS, MAPLE & BROWN SUGAR", "HARRIS TEETER")
        )
    }

    @Test
    fun `keeps a second part that adds information`() {
        assertEquals(
            "Quaker Instant Oatmeal, Apples & Cinnamon",
            UsdaGtinLookup.productName("INSTANT OATMEAL, APPLES & CINNAMON", "QUAKER")
        )
    }

    @Test
    fun `does not repeat a brand the description already names`() {
        assertEquals(
            "Quaker Instant Oatmeal Maple & Brown Sugar 1.69 Oz",
            UsdaGtinLookup.productName("Quaker Instant Oatmeal Maple & Brown Sugar 1.69 Oz", "Quaker")
        )
    }

    @Test
    fun `capitalises possessives and joined names sensibly`() {
        assertEquals("Kellogg's Frosted Flakes", UsdaGtinLookup.productName("FROSTED FLAKES", "KELLOGG'S"))
        assertEquals("M&M's Peanut Candies", UsdaGtinLookup.productName("PEANUT CANDIES", "M&M'S"))
    }

    @Test
    fun `works without a brand`() {
        assertEquals("Plain Rolled Oats", UsdaGtinLookup.productName("PLAIN ROLLED OATS", null))
        assertEquals("Scanned food", UsdaGtinLookup.productName("", ""))
    }
}
