package com.copdhealthtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.copdhealthtracker.data.model.FavoriteMeal
import com.copdhealthtracker.data.model.FavoriteMealItem
import com.copdhealthtracker.data.model.FoodEntry
import com.copdhealthtracker.databinding.DialogCreateMealBinding
import kotlinx.coroutines.launch

class CreateMealDialog(
    private val onSaveMeal: suspend (FavoriteMeal, List<FavoriteMealItem>) -> Boolean
) : DialogFragment() {

    private var _binding: DialogCreateMealBinding? = null
    private val binding get() = _binding!!
    private val items = mutableListOf<FoodEntry>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreateMealBinding.inflate(layoutInflater)
        val mealCategories = arrayOf("Breakfast", "Lunch", "Dinner", "Snacks")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mealCategories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.mealCategorySpinner.adapter = adapter

        binding.addFoodToMealButton.setOnClickListener { openAddFoodToMeal() }
        binding.saveMealButton.setOnClickListener { saveMeal() }
        refreshItemsDisplay()

        return AlertDialog.Builder(requireContext())
            .setTitle("Create meal")
            .setView(binding.root)
            .setNegativeButton("Cancel") { _, _ -> dismiss() }
            .create()
    }

    private fun openAddFoodToMeal() {
        val addFoodDialog = AddFoodDialog(
            onSave = {},
            onAddToMeal = { entry ->
                items.add(entry)
                refreshItemsDisplay()
            }
        )
        addFoodDialog.show(parentFragmentManager, "AddFoodToMeal")
    }

    private fun refreshItemsDisplay() {
        binding.mealItemsContainer.removeAllViews()
        if (items.isEmpty()) {
            val hint = TextView(requireContext()).apply {
                text = "Tap \"Add food to meal\" to add items"
                setPadding(24, 24, 24, 24)
                setTextColor(0xff6b7280.toInt())
            }
            binding.mealItemsContainer.addView(hint)
        } else {
            items.forEachIndexed { index, entry ->
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(12, 8, 12, 8)
                }
                val summary = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    text = "${entry.name} - ${entry.quantity} (${entry.calories.toInt()} cal)"
                    textSize = 14f
                }
                val deleteBtn = android.widget.Button(requireContext()).apply {
                    text = "Remove"
                    setOnClickListener {
                        items.removeAt(index)
                        refreshItemsDisplay()
                    }
                }
                row.addView(summary)
                row.addView(deleteBtn)
                binding.mealItemsContainer.addView(row)
            }
        }
    }

    private fun saveMeal() {
        val label = binding.mealNameEdit.text.toString().trim()
        if (label.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a meal name", Toast.LENGTH_SHORT).show()
            return
        }
        if (items.isEmpty()) {
            Toast.makeText(requireContext(), "Add at least one food to the meal", Toast.LENGTH_SHORT).show()
            return
        }
        val categoryIndex = binding.mealCategorySpinner.selectedItemPosition
        val mealCategories = arrayOf("Breakfast", "Lunch", "Dinner", "Snacks")
        val mealCategory = mealCategories[categoryIndex]
        val meal = FavoriteMeal(label = label, mealCategory = mealCategory)
        val mealItems = items.map { e ->
            FavoriteMealItem(
                mealId = 0,
                name = e.name,
                quantity = e.quantity,
                calories = e.calories,
                protein = e.protein,
                carbs = e.carbs,
                fat = e.fat,
                fiber = e.fiber,
                sodium = e.sodium,
                potassium = e.potassium,
                calcium = e.calcium,
                iron = e.iron,
                magnesium = e.magnesium,
                zinc = e.zinc,
                selenium = e.selenium,
                manganese = e.manganese,
                water = e.water,
                vitaminA = e.vitaminA,
                vitaminC = e.vitaminC,
                vitaminD = e.vitaminD,
                vitaminE = e.vitaminE,
                vitaminK = e.vitaminK,
                saturatedFat = e.saturatedFat,
                cholesterol = e.cholesterol,
                omega3 = e.omega3,
                addedSugars = e.addedSugars
            )
        }
        lifecycleScope.launch {
            val inserted = onSaveMeal(meal, mealItems)
            if (inserted) {
                Toast.makeText(requireContext(), "Meal saved as favorite", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(requireContext(), "A meal with this name already exists. Use a different name.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
