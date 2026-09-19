package com.copdhealthtracker.labelscan

import org.junit.Assert.assertEquals
import org.junit.Test

class UsdaGtinLookupTest {

    @Test
    fun `maps USDA food nutrients to ParsedLabel`() {
        val json = """
            {
              "foods": [{
                "description": "Test Cereal",
                "servingSize": 55.0,
                "servingSizeUnit": "g",
                "foodNutrients": [
                  {"nutrientId": 1008, "value": 210.0},
                  {"nutrientId": 1003, "value": 5.0},
                  {"nutrientId": 1005, "value": 42.0},
                  {"nutrientId": 1004, "value": 3.0}
                ]
              }]
            }
        """.trimIndent()

        val result = UsdaGtinLookup().mapResponse(json)

        assertEquals("Test Cereal", result?.productName)
        assertEquals(210.0, result?.calories?.value)
        assertEquals(5.0, result?.protein?.value)
        assertEquals(42.0, result?.carbs?.value)
        assertEquals(3.0, result?.fat?.value)
        assertEquals(55.0, result?.servingSize?.grams)
    }
}
