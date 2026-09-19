package com.copdhealthtracker.ui.scan

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.copdhealthtracker.R

/**
 * Editable review screen for values parsed from a nutrition label or QR lookup.
 *
 * This is a placeholder so the AddFoodDialog integration can compile.
 * The full implementation will display parsed calories, protein, carbs, and fat
 * and return a FoodEntry to the caller.
 */
class LabelReviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label_review)
    }

    companion object {
        const val EXTRA_FRONT_LABEL_URI = "extra_front_label_uri"
        const val EXTRA_NUTRITION_LABEL_URI = "extra_nutrition_label_uri"
        const val EXTRA_PARSED_LABEL = "extra_parsed_label"
        const val EXTRA_FOOD_ENTRY = "extra_food_entry"
    }
}
