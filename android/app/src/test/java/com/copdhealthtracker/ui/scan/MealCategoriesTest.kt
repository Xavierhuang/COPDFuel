package com.copdhealthtracker.ui.scan

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MealCategoriesTest {

    @Test
    fun `lists the four meals the tracking screen groups by`() {
        assertArrayEquals(arrayOf("Breakfast", "Lunch", "Dinner", "Snacks"), MealCategories.ALL)
    }

    @Test
    fun `suggests a meal from the time of day`() {
        assertEquals("Breakfast", MealCategories.defaultFor(hourOfDay = 7))
        assertEquals("Lunch", MealCategories.defaultFor(hourOfDay = 12))
        assertEquals("Dinner", MealCategories.defaultFor(hourOfDay = 18))
    }

    @Test
    fun `suggests snacks between meals and late at night`() {
        assertEquals("Snacks", MealCategories.defaultFor(hourOfDay = 15))
        assertEquals("Snacks", MealCategories.defaultFor(hourOfDay = 22))
        assertEquals("Snacks", MealCategories.defaultFor(hourOfDay = 2))
    }
}
