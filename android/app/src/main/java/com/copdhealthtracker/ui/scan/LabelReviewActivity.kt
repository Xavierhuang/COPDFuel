package com.copdhealthtracker.ui.scan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.copdhealthtracker.R
import com.copdhealthtracker.data.model.FoodEntry
import com.copdhealthtracker.databinding.ActivityLabelReviewBinding
import com.copdhealthtracker.labelscan.Confidence
import com.copdhealthtracker.labelscan.MlKitLabelOcr
import com.copdhealthtracker.labelscan.NutritionLabelParser
import com.copdhealthtracker.labelscan.ParsedLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LabelReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLabelReviewBinding
    private var parsedLabel: ParsedLabel = ParsedLabel()
    private var frontLabelUri: Uri? = null
    private var nutritionLabelUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLabelReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                var parsed = NutritionLabelParser.parse(text)

                if (frontLabelUri != null) {
                    val frontText = ocr.recognize(frontLabelUri!!)
                    val firstLine = frontText.lines().firstOrNull { it.isNotBlank() }?.trim()
                    parsed = parsed.copy(productName = firstLine)
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

        val hasGrams = binding.servingGramsEdit.text.toString().toDoubleOrNull() != null
        binding.saveToMyFoodsButton.isEnabled = hasGrams
        binding.saveToMyFoodsButton.alpha = if (hasGrams) 1.0f else 0.5f
    }

    private fun bindField(edit: EditText, warning: View, value: com.copdhealthtracker.labelscan.ValueWithConfidence?) {
        edit.setText(value?.value?.toString() ?: "")
        warning.visibility = if (value?.confidence == Confidence.LOW) View.VISIBLE else View.GONE
    }

    private fun saveToLog(saveToDatabase: Boolean) {
        val entry = buildFoodEntry() ?: return
        deleteCachedImages()
        val intent = Intent().apply {
            putExtra(EXTRA_FOOD_ENTRY, entry)
            putExtra(EXTRA_SAVE_TO_DATABASE, saveToDatabase)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun buildFoodEntry(): FoodEntry? {
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
            mealCategory = "Snacks",
            quantity = quantity,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            date = date
        )
    }

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
        const val EXTRA_FRONT_LABEL_URI = "extra_front_label_uri"
        const val EXTRA_NUTRITION_LABEL_URI = "extra_nutrition_label_uri"
        const val EXTRA_PARSED_LABEL = "extra_parsed_label"
        const val EXTRA_FOOD_ENTRY = "extra_food_entry"
        const val EXTRA_SAVE_TO_DATABASE = "extra_save_to_database"
        const val EXTRA_DATE = "extra_date"
    }
}
