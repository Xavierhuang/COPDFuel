package com.copdhealthtracker.ui.scan

/** The meals the Tracking screen groups food entries by. */
object MealCategories {
    val ALL = arrayOf("Breakfast", "Lunch", "Dinner", "Snacks")

    /** The meal to pre-select when asking where a scanned food belongs. */
    fun defaultFor(hourOfDay: Int): String = when (hourOfDay) {
        in 5..10 -> "Breakfast"
        in 11..14 -> "Lunch"
        in 17..20 -> "Dinner"
        else -> "Snacks"
    }
}
