package com.copdhealthtracker.ui.scan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.copdhealthtracker.R
import com.copdhealthtracker.data.model.FoodEntry
import com.copdhealthtracker.databinding.ActivityLabelReviewBinding
import com.copdhealthtracker.databinding.ItemReviewNutrientBinding
import com.copdhealthtracker.labelscan.Confidence
import com.copdhealthtracker.labelscan.ExtraNutrient
import com.copdhealthtracker.labelscan.MlKitLabelOcr
import com.copdhealthtracker.labelscan.NutritionLabelParser
import com.copdhealthtracker.labelscan.ParsedLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class LabelReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLabelReviewBinding
    private var parsedLabel: ParsedLabel = ParsedLabel()
    private var frontLabelUri: Uri? = null
    private var nutritionLabelUri: Uri? = null
    private val extraEdits = mutableMapOf<ExtraNutrient, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLabelReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge is enforced on Android 15+, so keep the form clear of the status bar,
        // navigation bar and keyboard, and use dark status icons on this white screen.
        WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = true
        val basePadding = binding.root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(
                basePadding + bars.left,
                basePadding + bars.top,
                basePadding + bars.right,
                basePadding + bars.bottom
            )
            insets
        }

        // Product names are long ("Quaker Maple & Brown Sugar Protein Instant Oatmeal"), so wrap
        // instead of scrolling sideways. For a text input this only takes effect when set in code.
        binding.foodNameEdit.setHorizontallyScrolling(false)
        binding.foodNameEdit.maxLines = 3

        setUpMoreNutrients()

        frontLabelUri = intent.getStringExtra(EXTRA_FRONT_LABEL_URI)?.let { Uri.parse(it) }
        nutritionLabelUri = intent.getStringExtra(EXTRA_NUTRITION_LABEL_URI)?.let { Uri.parse(it) }

        binding.retakeButton.setOnClickListener {
            deleteCachedImages()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.cancelButton.setOnClickListener {
            deleteCachedImages()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.addToLogButton.setOnClickListener { saveToLog(false) }
        binding.saveToMyFoodsButton.setOnClickListener { saveToLog(true) }

        onBackPressedDispatcher.addCallback(this) {
            deleteCachedImages()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        loadParsedLabel()
    }

    private fun loadParsedLabel() {
        val parsedExtra = intent.getSerializableExtra(EXTRA_PARSED_LABEL) as? ParsedLabel
        if (parsedExtra != null) {
            parsedLabel = parsedExtra
            bindValues()
            return
        }

        val uri = nutritionLabelUri ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val ocr = MlKitLabelOcr(this@LabelReviewActivity)
                val text = ocr.recognize(uri)
                Log.d(TAG, "Recognized label text:\n$text")
                var parsed = NutritionLabelParser.parse(text)

                if (frontLabelUri != null) {
                    val frontLines = ocr.recognizeLines(frontLabelUri!!)
                    parsed = parsed.copy(productName = NutritionLabelParser.extractProductName(frontLines))
                }

                parsedLabel = parsed

                withContext(Dispatchers.Main) {
                    bindValues()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LabelReviewActivity, "Could not read label", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setUpMoreNutrients() {
        val labels = mapOf(
            ExtraNutrient.FIBER to R.string.nutrient_fiber,
            ExtraNutrient.ADDED_SUGARS to R.string.nutrient_added_sugars,
            ExtraNutrient.SATURATED_FAT to R.string.nutrient_saturated_fat,
            ExtraNutrient.CHOLESTEROL to R.string.nutrient_cholesterol,
            ExtraNutrient.SODIUM to R.string.nutrient_sodium,
            ExtraNutrient.POTASSIUM to R.string.nutrient_potassium,
            ExtraNutrient.CALCIUM to R.string.nutrient_calcium,
            ExtraNutrient.IRON to R.string.nutrient_iron,
            ExtraNutrient.VITAMIN_D to R.string.nutrient_vitamin_d
        )
        ExtraNutrient.values().forEach { nutrient ->
            val item = ItemReviewNutrientBinding.inflate(layoutInflater, binding.moreNutrientsContainer, true)
            // Every row comes from the same layout, so give each field its own id; otherwise
            // restoring saved state would copy one value into all of them.
            item.nutrientEdit.id = View.generateViewId()
            item.nutrientLayout.id = View.generateViewId()
            item.nutrientLayout.hint = getString(labels.getValue(nutrient))
            extraEdits[nutrient] = item.nutrientEdit
        }
        binding.moreNutrientsHeader.setOnClickListener {
            showMoreNutrients(binding.moreNutrientsContainer.visibility != View.VISIBLE)
        }
    }

    private fun showMoreNutrients(show: Boolean) {
        binding.moreNutrientsContainer.visibility = if (show) View.VISIBLE else View.GONE
        binding.moreNutrientsHeader.setText(if (show) R.string.more_nutrients_hide else R.string.more_nutrients_show)
    }

    private fun bindValues() {
        binding.foodNameEdit.setText(parsedLabel.productName ?: "")

        val servingDesc = parsedLabel.servingSize?.description
        binding.servingSizeText.text = if (servingDesc.isNullOrBlank()) {
            getString(R.string.serving_size_label, "Not detected")
        } else {
            getString(R.string.serving_size_label, servingDesc)
        }
        binding.servingGramsEdit.setText(parsedLabel.servingSize?.grams?.toString() ?: "")

        bindField(binding.caloriesEdit, binding.caloriesWarning, parsedLabel.calories)
        bindField(binding.proteinEdit, binding.proteinWarning, parsedLabel.protein)
        bindField(binding.carbsEdit, binding.carbsWarning, parsedLabel.carbs)
        bindField(binding.fatEdit, binding.fatWarning, parsedLabel.fat)

        extraEdits.forEach { (nutrient, edit) ->
            edit.setText(parsedLabel.extras[nutrient]?.value?.toString() ?: "")
        }
        showMoreNutrients(parsedLabel.extras.isNotEmpty())

        val hasGrams = binding.servingGramsEdit.text.toString().toDoubleOrNull() != null
        binding.saveToMyFoodsButton.isEnabled = hasGrams
        binding.saveToMyFoodsButton.alpha = if (hasGrams) 1.0f else 0.5f
    }

    private fun bindField(edit: EditText, warning: View, value: com.copdhealthtracker.labelscan.ValueWithConfidence?) {
        edit.setText(value?.value?.toString() ?: "")
        warning.visibility = if (value?.confidence == Confidence.LOW) View.VISIBLE else View.GONE
    }

    // Ask which meal the food belongs to before saving, like adding from favorites does.
    private fun saveToLog(saveToDatabase: Boolean) {
        val meals = MealCategories.ALL
        var selectedIndex = meals.indexOf(MealCategories.defaultFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)))
        AlertDialog.Builder(this)
            .setTitle(R.string.add_to_which_meal)
            .setSingleChoiceItems(meals, selectedIndex) { _, which -> selectedIndex = which }
            .setPositiveButton(R.string.add) { _, _ -> finishWithEntry(meals[selectedIndex], saveToDatabase) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun finishWithEntry(mealCategory: String, saveToDatabase: Boolean) {
        val entry = buildFoodEntry(mealCategory) ?: return
        deleteCachedImages()
        val intent = Intent().apply {
            putExtra(EXTRA_FOOD_ENTRY, entry)
            putExtra(EXTRA_SAVE_TO_DATABASE, saveToDatabase)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun buildFoodEntry(mealCategory: String): FoodEntry? {
        val name = binding.foodNameEdit.text.toString().trim().ifEmpty { "Scanned food" }
        val calories = binding.caloriesEdit.text.toString().toDoubleOrNull() ?: 0.0
        val protein = binding.proteinEdit.text.toString().toDoubleOrNull() ?: 0.0
        val carbs = binding.carbsEdit.text.toString().toDoubleOrNull() ?: 0.0
        val fat = binding.fatEdit.text.toString().toDoubleOrNull() ?: 0.0
        val grams = binding.servingGramsEdit.text.toString().toDoubleOrNull()
        val quantity = if (grams != null) "1 serving (${grams.toInt()}g)" else "1 serving"
        val date = intent.getLongExtra(EXTRA_DATE, System.currentTimeMillis())

        return FoodEntry(
            name = name,
            mealCategory = mealCategory,
            quantity = quantity,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            fiber = extraValue(ExtraNutrient.FIBER),
            sodium = extraValue(ExtraNutrient.SODIUM),
            potassium = extraValue(ExtraNutrient.POTASSIUM),
            calcium = extraValue(ExtraNutrient.CALCIUM),
            iron = extraValue(ExtraNutrient.IRON),
            vitaminD = extraValue(ExtraNutrient.VITAMIN_D),
            saturatedFat = extraValue(ExtraNutrient.SATURATED_FAT),
            cholesterol = extraValue(ExtraNutrient.CHOLESTEROL),
            addedSugars = extraValue(ExtraNutrient.ADDED_SUGARS),
            date = date
        )
    }

    private fun extraValue(nutrient: ExtraNutrient): Double =
        extraEdits[nutrient]?.text?.toString()?.toDoubleOrNull() ?: 0.0

    private fun deleteCachedImages() {
        deleteCachedImage(frontLabelUri)
        deleteCachedImage(nutritionLabelUri)
    }

    private fun deleteCachedImage(uri: Uri?) {
        uri?.path?.let { path ->
            try {
                File(path).delete()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val TAG = "LabelReviewActivity"
        const val EXTRA_FRONT_LABEL_URI = "extra_front_label_uri"
        const val EXTRA_NUTRITION_LABEL_URI = "extra_nutrition_label_uri"
        const val EXTRA_PARSED_LABEL = "extra_parsed_label"
        const val EXTRA_FOOD_ENTRY = "extra_food_entry"
        const val EXTRA_SAVE_TO_DATABASE = "extra_save_to_database"
        const val EXTRA_DATE = "extra_date"
    }
}
