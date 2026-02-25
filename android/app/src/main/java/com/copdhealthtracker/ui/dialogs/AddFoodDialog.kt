package com.copdhealthtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.copdhealthtracker.data.FoodDatabaseHelper
import com.copdhealthtracker.data.model.FavoriteFood
import com.copdhealthtracker.data.model.FoodEntry
import com.copdhealthtracker.data.model.FoodSearchResult
import com.copdhealthtracker.data.model.UserAddedFood
import com.copdhealthtracker.databinding.DialogAddFoodBinding
import com.copdhealthtracker.ui.adapters.FoodSearchAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class AddFoodDialog(
    private val onSave: (FoodEntry) -> Unit,
    private val dateForEntry: Long? = null,
    private val onSaveFavorite: (suspend (FavoriteFood) -> Boolean)? = null,
    private val getAllUserAddedFoods: (suspend () -> List<UserAddedFood>)? = null,
    private val insertUserAddedFood: (suspend (UserAddedFood) -> Boolean)? = null,
    private val onAddToMeal: ((FoodEntry) -> Unit)? = null
) : DialogFragment() {
    
    private var _binding: DialogAddFoodBinding? = null
    private val binding get() = _binding!!
    private lateinit var searchAdapter: FoodSearchAdapter
    private lateinit var foodDatabaseHelper: FoodDatabaseHelper
    
    private val usdaApiKey: String
        get() = com.copdhealthtracker.BuildConfig.USDA_FDC_API_KEY
    
    private var useLocalDatabase = true
    private var selectedCategory = "All Categories"
    
    // Store selected food result with all nutrients (per 100g)
    private var selectedFood: FoodSearchResult? = null
    private var currentServingSize = ""
    private var servingSizes = mutableListOf<ServingOption>()
    
    data class ServingOption(
        val label: String,
        val grams: Double,
        val multiplier: Double = 1.0
    )
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddFoodBinding.inflate(layoutInflater)
        
        // Initialize food database helper
        foodDatabaseHelper = FoodDatabaseHelper(requireContext())
        
        // Load local database and user-added foods
        lifecycleScope.launch {
            foodDatabaseHelper.loadDatabase()
            getAllUserAddedFoods?.invoke()?.let { foodDatabaseHelper.setUserAddedFoods(it) }
            setupCategoryFilter()
        }
        
        // Setup meal category spinner
        val mealCategories = arrayOf("Breakfast", "Lunch", "Dinner", "Snacks")
        val mealAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mealCategories)
        mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.mealCategorySpinner.adapter = mealAdapter
        
        // Setup search adapter
        searchAdapter = FoodSearchAdapter { result ->
            selectFoodResult(result)
        }
        binding.searchResultsList.layoutManager = LinearLayoutManager(requireContext())
        binding.searchResultsList.adapter = searchAdapter
        
        // Setup database toggle
        binding.databaseToggle.setOnCheckedChangeListener { _, checkedId ->
            useLocalDatabase = checkedId == binding.radioLocal.id
            binding.categoryFilterContainer.visibility = if (useLocalDatabase) View.VISIBLE else View.GONE
            binding.searchStatus.text = if (useLocalDatabase) {
                "Search from local database (${foodDatabaseHelper.getTotalFoodsCount()} foods)"
            } else {
                "Search USDA FoodData Central online"
            }
            // Clear previous results
            binding.searchResultsList.visibility = View.GONE
            searchAdapter.submitList(emptyList())
        }
        
        // Setup search button
        binding.searchButton.setOnClickListener {
            performSearch()
        }
        
        // Setup search on enter key
        binding.foodSearchEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
        
        // Setup amount field listener to recalculate nutrition
        binding.amountEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateNutritionDisplay()
            }
        })
        
        // Setup serving size spinner listener
        binding.servingSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (servingSizes.isNotEmpty() && position < servingSizes.size) {
                    currentServingSize = servingSizes[position].label
                    updateNutritionDisplay()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Setup toggle manual entry button
        binding.toggleManualButton.setOnClickListener {
            if (binding.manualEntrySection.visibility == View.GONE) {
                binding.manualEntrySection.visibility = View.VISIBLE
                binding.toggleManualButton.text = "Hide Manual Entry"
            } else {
                binding.manualEntrySection.visibility = View.GONE
                binding.toggleManualButton.text = "Edit Nutrition Manually"
            }
        }
        
        // Initialize with default serving size
        setupDefaultServingSize()
        
        val isAddToMealMode = onAddToMeal != null
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (isAddToMealMode) "Add food to meal" else "Add Food")
            .setView(binding.root)
            .setPositiveButton(if (isAddToMealMode) "Add to meal" else "Save", null)
            .setNegativeButton("Cancel") { _, _ ->
                dismiss()
            }
            .create()
        
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (isAddToMealMode) {
                    if (validateAndAddToMeal()) dismiss()
                } else {
                    if (validateAndSave()) dismiss()
                }
            }
            binding.saveAsFavoriteButton.visibility = if (onSaveFavorite != null && !isAddToMealMode) View.VISIBLE else View.GONE
            binding.saveAsFavoriteButton.setOnClickListener { saveAsFavorite() }
            binding.addToDatabaseButton.visibility = if (insertUserAddedFood != null && !isAddToMealMode) View.VISIBLE else View.GONE
            binding.addToDatabaseButton.setOnClickListener { addCurrentFoodToDatabase() }
        }
        
        return dialog
    }
    
    private fun setupDefaultServingSize() {
        servingSizes = mutableListOf(
            ServingOption("1 serving", 100.0, 1.0),
            ServingOption("100g", 100.0, 1.0)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, servingSizes.map { it.label })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.servingSizeSpinner.adapter = adapter
    }
    
    private fun setupCategoryFilter() {
        val categories = mutableListOf("All Categories")
        categories.addAll(foodDatabaseHelper.getCategories())
        
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categoryFilterSpinner.adapter = categoryAdapter
        
        binding.categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategory = "All Categories"
            }
        }
        
        // Update status text
        binding.searchStatus.text = "Search from local database (${foodDatabaseHelper.getTotalFoodsCount()} foods)"
    }
    
    private fun performSearch() {
        val query = binding.foodSearchEdit.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.searchStatus.text = "Searching..."
        binding.searchButton.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val results = if (useLocalDatabase) {
                    searchLocalDatabase(query)
                } else {
                    searchUSDADatabase(query)
                }
                
                if (results.isEmpty()) {
                    binding.searchStatus.text = "No results found"
                    binding.searchResultsList.visibility = View.GONE
                } else {
                    val source = if (useLocalDatabase) "local" else "USDA"
                    binding.searchStatus.text = "${results.size} results from $source - tap to select"
                    searchAdapter.submitList(results)
                    binding.searchResultsList.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                binding.searchStatus.text = "Search failed: ${e.message}"
                binding.searchResultsList.visibility = View.GONE
            } finally {
                binding.searchButton.isEnabled = true
            }
        }
    }
    
    private suspend fun searchLocalDatabase(query: String): List<FoodSearchResult> = withContext(Dispatchers.Default) {
        val results = if (selectedCategory != "All Categories") {
            // First filter by category, then search
            val categoryResults = foodDatabaseHelper.searchByCategory(selectedCategory, 100)
            val queryLower = query.lowercase()
            categoryResults.filter { 
                it.description.lowercase().contains(queryLower) 
            }.take(20)
        } else {
            foodDatabaseHelper.searchFoods(query, 20)
        }
        results
    }
    
    private suspend fun searchUSDADatabase(query: String): List<FoodSearchResult> = withContext(Dispatchers.IO) {
        if (usdaApiKey.isBlank()) return@withContext emptyList()
        val url = "https://api.nal.usda.gov/fdc/v1/foods/search?query=${java.net.URLEncoder.encode(query, "UTF-8")}&pageSize=15&dataType=Foundation,SR%20Legacy,Branded&api_key=$usdaApiKey"

        val response = URL(url).readText()
        val json = JSONObject(response)
        val foods = json.optJSONArray("foods") ?: return@withContext emptyList()
        
        val results = mutableListOf<FoodSearchResult>()
        
        for (i in 0 until foods.length()) {
            val food = foods.getJSONObject(i)
            
            var calories = 0.0
            var protein = 0.0
            var carbs = 0.0
            var fat = 0.0
            
            val nutrients = food.optJSONArray("foodNutrients")
            if (nutrients != null) {
                for (j in 0 until nutrients.length()) {
                    val nutrient = nutrients.getJSONObject(j)
                    val nutrientId = nutrient.optInt("nutrientId", 0)
                    val value = nutrient.optDouble("value", 0.0)
                    
                    when (nutrientId) {
                        1008 -> calories = value  // Energy (kcal)
                        1003 -> protein = value   // Protein
                        1005 -> carbs = value     // Carbohydrate
                        1004 -> fat = value       // Total lipid (fat)
                    }
                }
            }
            
            results.add(
                FoodSearchResult(
                    fdcId = food.optInt("fdcId", 0),
                    description = food.optString("description", "Unknown"),
                    brandOwner = food.optString("brandOwner", null),
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    servingSize = food.optDouble("servingSize", 100.0),
                    servingUnit = food.optString("servingSizeUnit", "g")
                )
            )
        }
        
        results
    }
    
    private fun selectFoodResult(result: FoodSearchResult) {
        // Fill in the form with the selected food data
        binding.foodNameEdit.setText(result.description)
        
        // Store selected food with all nutrients (per 100g)
        selectedFood = result
        
        // Use serving sizes from database if available
        if (result.servingSizes.isNotEmpty()) {
            servingSizes = result.servingSizes.map { ss ->
                // Calculate multiplier: nutrition is per 100g, so multiplier = grams / 100
                ServingOption(
                    label = if (ss.isCustom) "g (enter amount)" else "${ss.label} - ${ss.grams.toInt()}g",
                    grams = ss.grams,
                    multiplier = ss.grams / 100.0
                )
            }.toMutableList()
        } else {
            // Fallback for USDA or other sources without serving sizes
            val servingDesc = result.servingUnit ?: "1 serving"
            val servingGrams = result.servingSize ?: 100.0
            
            servingSizes = mutableListOf(
                ServingOption("$servingDesc - ${servingGrams.toInt()}g", servingGrams, servingGrams / 100.0)
            )
            
            // Add 100g option if different
            if (servingGrams != 100.0) {
                servingSizes.add(ServingOption("100g", 100.0, 1.0))
            }
            
            // Add gram option for custom amounts
            servingSizes.add(ServingOption("g (enter amount)", 1.0, 0.01))
        }
        
        // Update serving size spinner
        val servingLabels = servingSizes.map { it.label }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, servingLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.servingSizeSpinner.adapter = adapter
        
        currentServingSize = servingSizes[0].label
        
        // Reset amount to 1
        binding.amountEdit.setText("1")
        
        // Update nutrition display
        updateNutritionDisplay()
        
        // Update manual entry fields (for the primary serving)
        val primaryMultiplier = if (servingSizes.isNotEmpty()) servingSizes[0].multiplier else 1.0
        binding.caloriesEdit.setText((result.calories * primaryMultiplier).toInt().toString())
        binding.proteinEdit.setText(String.format("%.1f", result.protein * primaryMultiplier))
        binding.carbsEdit.setText(String.format("%.1f", result.carbs * primaryMultiplier))
        binding.fatEdit.setText(String.format("%.1f", result.fat * primaryMultiplier))
        
        // Hide search results after selection
        binding.searchResultsList.visibility = View.GONE
        binding.searchStatus.text = "Selected: ${result.description}"
        binding.selectedFoodLabel.text = result.description
        
        Toast.makeText(requireContext(), "Food selected", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateNutritionDisplay() {
        val food = selectedFood ?: return
        val amount = binding.amountEdit.text.toString().toDoubleOrNull() ?: 1.0
        
        // Find the selected serving option
        val selectedIndex = binding.servingSizeSpinner.selectedItemPosition
        val multiplier = if (servingSizes.isNotEmpty() && selectedIndex < servingSizes.size) {
            servingSizes[selectedIndex].multiplier
        } else {
            1.0
        }
        
        // Calculate nutrition based on amount and serving size
        val totalCalories = (food.calories * multiplier * amount).toInt()
        val totalProtein = food.protein * multiplier * amount
        val totalCarbs = food.carbs * multiplier * amount
        val totalFat = food.fat * multiplier * amount
        
        // Update nutrition display
        binding.nutritionCalories.text = totalCalories.toString()
        binding.nutritionProtein.text = String.format("%.1fg", totalProtein)
        binding.nutritionCarbs.text = String.format("%.1fg", totalCarbs)
        binding.nutritionFat.text = String.format("%.1fg", totalFat)
    }
    
    private fun buildCurrentFoodEntry(dateMillis: Long): FoodEntry? {
        val selectedPosition = binding.mealCategorySpinner.selectedItemPosition
        val mealCategories = arrayOf("Breakfast", "Lunch", "Dinner", "Snacks")
        val category = mealCategories[selectedPosition]
        val name = binding.foodNameEdit.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(requireContext(), "Please enter food name", Toast.LENGTH_SHORT).show()
            return null
        }
        val amount = binding.amountEdit.text.toString().toDoubleOrNull() ?: 1.0
        if (amount <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return null
        }
        val selectedIndex = binding.servingSizeSpinner.selectedItemPosition
        val servingOption = if (servingSizes.isNotEmpty() && selectedIndex < servingSizes.size) servingSizes[selectedIndex] else null
        val servingLabel = if (servingOption != null) {
            val grams = servingOption.grams
            if (grams > 0) "${grams.toInt()}g" else "1 serving"
        } else "1 serving"
        val multiplier = servingOption?.multiplier ?: 1.0
        val food = selectedFood
        val calories: Double; val protein: Double; val carbs: Double; val fat: Double; val fiber: Double
        val sodium: Double; val potassium: Double; val calcium: Double; val iron: Double; val magnesium: Double
        val zinc: Double; val selenium: Double; val manganese: Double; val water: Double
        val vitaminA: Double; val vitaminC: Double; val vitaminD: Double; val vitaminE: Double; val vitaminK: Double
        val saturatedFat: Double; val cholesterol: Double; val omega3: Double; val addedSugars: Double
        if (binding.manualEntrySection.visibility == View.VISIBLE || food == null) {
            calories = binding.caloriesEdit.text.toString().toDoubleOrNull() ?: 0.0
            protein = binding.proteinEdit.text.toString().toDoubleOrNull() ?: 0.0
            carbs = binding.carbsEdit.text.toString().toDoubleOrNull() ?: 0.0
            fat = binding.fatEdit.text.toString().toDoubleOrNull() ?: 0.0
            fiber = 0.0; sodium = 0.0; potassium = 0.0; calcium = 0.0; iron = 0.0; magnesium = 0.0; zinc = 0.0
            selenium = 0.0; manganese = 0.0; water = 0.0; vitaminA = 0.0; vitaminC = 0.0; vitaminD = 0.0; vitaminE = 0.0; vitaminK = 0.0
            saturatedFat = 0.0; cholesterol = 0.0; omega3 = 0.0; addedSugars = 0.0
        } else {
            val factor = multiplier * amount
            calories = food.calories * factor; protein = food.protein * factor; carbs = food.carbs * factor; fat = food.fat * factor
            fiber = food.fiber * factor; sodium = food.sodium * factor; potassium = food.potassium * factor; calcium = food.calcium * factor
            iron = food.iron * factor; magnesium = food.magnesium * factor; zinc = food.zinc * factor; selenium = food.selenium * factor
            manganese = food.manganese * factor; water = food.water * factor; vitaminA = food.vitaminA * factor; vitaminC = food.vitaminC * factor
            vitaminD = food.vitaminD * factor; vitaminE = food.vitaminE * factor; vitaminK = food.vitaminK * factor
            saturatedFat = food.saturatedFat * factor; cholesterol = food.cholesterol * factor; omega3 = food.omega3 * factor; addedSugars = food.addedSugars * factor
        }
        val servingGrams = servingOption?.grams ?: 100.0
        val totalGrams = (servingGrams * amount).toInt()
        val quantityDisplay = if (amount == 1.0) servingLabel else "${amount.toInt()} x $servingLabel (${totalGrams}g total)"
        return FoodEntry(
            name = name, mealCategory = category, quantity = quantityDisplay,
            calories = calories, protein = protein, carbs = carbs, fat = fat, fiber = fiber,
            sodium = sodium, potassium = potassium, calcium = calcium, iron = iron, magnesium = magnesium,
            zinc = zinc, selenium = selenium, manganese = manganese, water = water,
            vitaminA = vitaminA, vitaminC = vitaminC, vitaminD = vitaminD, vitaminE = vitaminE, vitaminK = vitaminK,
            saturatedFat = saturatedFat, cholesterol = cholesterol, omega3 = omega3, addedSugars = addedSugars,
            date = dateMillis
        )
    }

    private fun validateAndSave(): Boolean {
        val dateMillis = dateForEntry ?: System.currentTimeMillis()
        buildCurrentFoodEntry(dateMillis)?.let { entry ->
            onSave(entry)
            Toast.makeText(requireContext(), "Food saved successfully", Toast.LENGTH_SHORT).show()
            dismiss()
            return true
        }
        return false
    }

    private fun validateAndAddToMeal(): Boolean {
        buildCurrentFoodEntry(System.currentTimeMillis())?.let { entry ->
            onAddToMeal?.invoke(entry)
            Toast.makeText(requireContext(), "Added to meal", Toast.LENGTH_SHORT).show()
            return true
        }
        return false
    }

    private fun addCurrentFoodToDatabase() {
        val name = binding.foodNameEdit.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a food name first", Toast.LENGTH_SHORT).show()
            return
        }
        val food = selectedFood
        val per100gCal: Double
        val per100gProtein: Double
        val per100gCarbs: Double
        val per100gFat: Double
        val per100gFiber: Double
        val per100gSodium: Double
        val per100gPotassium: Double
        val per100gCalcium: Double
        val per100gIron: Double
        val per100gMagnesium: Double
        val per100gZinc: Double
        val per100gSelenium: Double
        val per100gManganese: Double
        val per100gWater: Double
        val per100gVitA: Double
        val per100gVitC: Double
        val per100gVitD: Double
        val per100gVitE: Double
        val per100gVitK: Double
        val per100gSatFat: Double
        val per100gChol: Double
        val per100gOmega3: Double
        val per100gAddedSugars: Double
        if (food != null) {
            per100gCal = food.calories
            per100gProtein = food.protein
            per100gCarbs = food.carbs
            per100gFat = food.fat
            per100gFiber = food.fiber
            per100gSodium = food.sodium
            per100gPotassium = food.potassium
            per100gCalcium = food.calcium
            per100gIron = food.iron
            per100gMagnesium = food.magnesium
            per100gZinc = food.zinc
            per100gSelenium = food.selenium
            per100gManganese = food.manganese
            per100gWater = food.water
            per100gVitA = food.vitaminA
            per100gVitC = food.vitaminC
            per100gVitD = food.vitaminD
            per100gVitE = food.vitaminE
            per100gVitK = food.vitaminK
            per100gSatFat = food.saturatedFat
            per100gChol = food.cholesterol
            per100gOmega3 = food.omega3
            per100gAddedSugars = food.addedSugars
        } else {
            val amount = binding.amountEdit.text.toString().toDoubleOrNull() ?: 1.0
            val selectedIndex = binding.servingSizeSpinner.selectedItemPosition
            val servingOption = if (servingSizes.isNotEmpty() && selectedIndex < servingSizes.size) servingSizes[selectedIndex] else null
            val grams = servingOption?.grams ?: 100.0
            val factor = if (amount > 0 && grams > 0) amount * grams / 100.0 else 1.0
            val totalCal = binding.caloriesEdit.text.toString().toDoubleOrNull() ?: 0.0
            val totalProtein = binding.proteinEdit.text.toString().toDoubleOrNull() ?: 0.0
            val totalCarbs = binding.carbsEdit.text.toString().toDoubleOrNull() ?: 0.0
            val totalFat = binding.fatEdit.text.toString().toDoubleOrNull() ?: 0.0
            per100gCal = if (factor > 0) totalCal / factor * 100.0 else totalCal
            per100gProtein = if (factor > 0) totalProtein / factor * 100.0 else totalProtein
            per100gCarbs = if (factor > 0) totalCarbs / factor * 100.0 else totalCarbs
            per100gFat = if (factor > 0) totalFat / factor * 100.0 else totalFat
            per100gFiber = 0.0
            per100gSodium = 0.0
            per100gPotassium = 0.0
            per100gCalcium = 0.0
            per100gIron = 0.0
            per100gMagnesium = 0.0
            per100gZinc = 0.0
            per100gSelenium = 0.0
            per100gManganese = 0.0
            per100gWater = 0.0
            per100gVitA = 0.0
            per100gVitC = 0.0
            per100gVitD = 0.0
            per100gVitE = 0.0
            per100gVitK = 0.0
            per100gSatFat = 0.0
            per100gChol = 0.0
            per100gOmega3 = 0.0
            per100gAddedSugars = 0.0
        }
        val userFood = UserAddedFood(
            name = name,
            shortName = name,
            calories = per100gCal,
            protein = per100gProtein,
            carbs = per100gCarbs,
            fat = per100gFat,
            fiber = per100gFiber,
            sodium = per100gSodium,
            potassium = per100gPotassium,
            calcium = per100gCalcium,
            iron = per100gIron,
            magnesium = per100gMagnesium,
            zinc = per100gZinc,
            selenium = per100gSelenium,
            manganese = per100gManganese,
            water = per100gWater,
            vitaminA = per100gVitA,
            vitaminC = per100gVitC,
            vitaminD = per100gVitD,
            vitaminE = per100gVitE,
            vitaminK = per100gVitK,
            saturatedFat = per100gSatFat,
            cholesterol = per100gChol,
            omega3 = per100gOmega3,
            addedSugars = per100gAddedSugars
        )
        lifecycleScope.launch {
            val inserted = insertUserAddedFood?.invoke(userFood) ?: false
            if (inserted) {
                getAllUserAddedFoods?.invoke()?.let { foodDatabaseHelper.setUserAddedFoods(it) }
                Toast.makeText(requireContext(), "Added to database. You can search for it next time.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "This food is already in your database.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAsFavorite() {
        val entry = buildCurrentFoodEntry(System.currentTimeMillis()) ?: return
        val labelInput = android.widget.EditText(requireContext()).apply {
            hint = "e.g. My usual breakfast"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Save as favorite")
            .setMessage("Give this meal a short name so you can add it quickly later.")
            .setView(labelInput)
            .setPositiveButton("Save") { _, _ ->
                val label = labelInput.text.toString().trim()
                if (label.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val favorite = FavoriteFood(
                    label = label,
                    name = entry.name,
                    mealCategory = entry.mealCategory,
                    quantity = entry.quantity,
                    calories = entry.calories,
                    protein = entry.protein,
                    carbs = entry.carbs,
                    fat = entry.fat,
                    fiber = entry.fiber,
                    sodium = entry.sodium,
                    potassium = entry.potassium,
                    calcium = entry.calcium,
                    iron = entry.iron,
                    magnesium = entry.magnesium,
                    zinc = entry.zinc,
                    selenium = entry.selenium,
                    manganese = entry.manganese,
                    water = entry.water,
                    vitaminA = entry.vitaminA,
                    vitaminC = entry.vitaminC,
                    vitaminD = entry.vitaminD,
                    vitaminE = entry.vitaminE,
                    vitaminK = entry.vitaminK,
                    saturatedFat = entry.saturatedFat,
                    cholesterol = entry.cholesterol,
                    omega3 = entry.omega3,
                    addedSugars = entry.addedSugars
                )
                lifecycleScope.launch {
                    val inserted = onSaveFavorite?.invoke(favorite) ?: true
                    if (inserted) {
                        Toast.makeText(requireContext(), "Saved as favorite", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Already in favorites. Use a different name or delete the existing one first.", Toast.LENGTH_SHORT).show()
                    }
                    dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
