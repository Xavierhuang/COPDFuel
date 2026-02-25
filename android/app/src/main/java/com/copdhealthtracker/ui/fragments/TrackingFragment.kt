package com.copdhealthtracker.ui.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.copdhealthtracker.R
import com.copdhealthtracker.data.model.FavoriteFood
import com.copdhealthtracker.data.model.FavoriteMeal
import com.copdhealthtracker.data.model.FoodEntry
import com.copdhealthtracker.data.model.WaterEntry
import com.copdhealthtracker.databinding.FragmentTrackingBinding
import com.copdhealthtracker.ui.dialogs.*
import com.copdhealthtracker.ui.viewmodel.TrackingViewModel
import com.copdhealthtracker.ui.viewmodel.TrackingViewModelFactory
import com.copdhealthtracker.utils.AppApplication
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TrackingFragment : Fragment() {
    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TrackingViewModel
    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var selectedWeekStartMillis: Long = getStartOfWeek(System.currentTimeMillis())
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private var dateDependentJob: Job? = null
    private var weekDependentJob: Job? = null
    private var monthDependentJob: Job? = null
    private var currentView: ViewType = ViewType.DAY
    
    private enum class ViewType { DAY, WEEK, MONTH }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val repository = (requireContext().applicationContext as AppApplication).repository
        viewModel = ViewModelProvider(this, TrackingViewModelFactory(repository))[TrackingViewModel::class.java]
        
        setupViews()
        setupViewToggle()
        observeData()
        updateDateButtonText()
        updateWeekNavigationText()
        updateMonthLabel()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh profile setup check when returning to this fragment
        checkProfileSetup()
    }
    
    private fun setupViews() {
        // Date navigation
        binding.datePickerButton.setOnClickListener { showDatePicker() }
        binding.prevDayButton.setOnClickListener { navigateDay(-1) }
        binding.nextDayButton.setOnClickListener { navigateDay(1) }
        
        // Meal category headers - toggle expand/collapse and add food
        binding.breakfastHeader.setOnClickListener { 
            toggleMealSection(binding.breakfastContent, binding.breakfastArrow)
        }
        binding.lunchHeader.setOnClickListener { 
            toggleMealSection(binding.lunchContent, binding.lunchArrow)
        }
        binding.dinnerHeader.setOnClickListener { 
            toggleMealSection(binding.dinnerContent, binding.dinnerArrow)
        }
        binding.snacksHeader.setOnClickListener { 
            toggleMealSection(binding.snacksContent, binding.snacksArrow)
        }
        
        // COPD Health Tracking buttons
        binding.logOxygenButton.setOnClickListener { showOxygenDialog() }
        binding.logExerciseButton.setOnClickListener { showExerciseDialog() }
        
        // Quick Add Food and Add from favorites
        binding.quickAddFoodButton.setOnClickListener { showFoodDialog() }
        binding.addFromFavoritesButton.setOnClickListener { showFavoritesDialog() }
        binding.createMealButton.setOnClickListener { showCreateMealDialog() }
        
        // Hydration tracking buttons
        binding.hydrationAdd8oz.setOnClickListener { addWater(8) }
        binding.hydrationAdd12oz.setOnClickListener { addWater(12) }
        binding.hydrationAdd16oz.setOnClickListener { addWater(16) }
        binding.hydrationAddCustom.setOnClickListener { showCustomWaterDialog() }
        
        // Weight and Medications cards
        binding.weightCard.setOnClickListener { showWeightDialog() }
        binding.medicationCard.setOnClickListener {
            MedicationsDialogFragment().show(parentFragmentManager, "MedicationsDialog")
        }
        
        // Also make the oxygen and exercise cards clickable
        binding.oxygenCard.setOnClickListener { showOxygenDialog() }
        binding.exerciseCard.setOnClickListener { showExerciseDialog() }
        
        // Profile setup banner button
        binding.btnSetupProfile.setOnClickListener { navigateToProfile() }
        
        // Check if profile is complete and show/hide banner
        checkProfileSetup()
    }
    
    private fun setupViewToggle() {
        // Day/Week/Month view toggle buttons
        binding.btnDayView.setOnClickListener {
            if (currentView != ViewType.DAY) {
                currentView = ViewType.DAY
                updateViewToggleButtons()
                showDayView()
            }
        }
        
        binding.btnWeekView.setOnClickListener {
            if (currentView != ViewType.WEEK) {
                currentView = ViewType.WEEK
                updateViewToggleButtons()
                showWeekView()
            }
        }
        
        binding.btnMonthView.setOnClickListener {
            if (currentView != ViewType.MONTH) {
                currentView = ViewType.MONTH
                updateViewToggleButtons()
                showMonthView()
            }
        }
        
        // Week navigation
        binding.prevWeekButton.setOnClickListener { navigateWeek(-1) }
        binding.nextWeekButton.setOnClickListener { navigateWeek(1) }
        
        // Month navigation
        binding.prevMonthButton.setOnClickListener { navigateMonth(-1) }
        binding.nextMonthButton.setOnClickListener { navigateMonth(1) }
    }
    
    private fun updateViewToggleButtons() {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.colorSuccess)
        val activeTextColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val inactiveColor = android.graphics.Color.parseColor("#E5E7EB")
        val inactiveTextColor = android.graphics.Color.parseColor("#374151")
        
        // Reset all buttons to inactive
        binding.btnDayView.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
        binding.btnDayView.setTextColor(inactiveTextColor)
        binding.btnWeekView.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
        binding.btnWeekView.setTextColor(inactiveTextColor)
        binding.btnMonthView.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
        binding.btnMonthView.setTextColor(inactiveTextColor)
        
        // Set active button
        when (currentView) {
            ViewType.DAY -> {
                binding.btnDayView.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
                binding.btnDayView.setTextColor(activeTextColor)
            }
            ViewType.WEEK -> {
                binding.btnWeekView.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
                binding.btnWeekView.setTextColor(activeTextColor)
            }
            ViewType.MONTH -> {
                binding.btnMonthView.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
                binding.btnMonthView.setTextColor(activeTextColor)
            }
        }
    }
    
    private fun showDayView() {
        binding.dayNavigation.visibility = View.VISIBLE
        binding.weekNavigation.visibility = View.GONE
        binding.weeklySummarySection.visibility = View.GONE
        binding.monthlySummarySection.visibility = View.GONE
        binding.dayViewContent.visibility = View.VISIBLE
        
        // Cancel other observers and restart day observers
        weekDependentJob?.cancel()
        monthDependentJob?.cancel()
        dateDependentJob?.cancel()
        startDateDependentObservers()
    }
    
    private fun showWeekView() {
        binding.dayNavigation.visibility = View.GONE
        binding.weekNavigation.visibility = View.VISIBLE
        binding.weeklySummarySection.visibility = View.VISIBLE
        binding.monthlySummarySection.visibility = View.GONE
        binding.dayViewContent.visibility = View.GONE
        
        // Cancel other observers and start weekly observers
        dateDependentJob?.cancel()
        monthDependentJob?.cancel()
        weekDependentJob?.cancel()
        startWeeklyObservers()
    }
    
    private fun showMonthView() {
        binding.dayNavigation.visibility = View.GONE
        binding.weekNavigation.visibility = View.GONE
        binding.weeklySummarySection.visibility = View.GONE
        binding.monthlySummarySection.visibility = View.VISIBLE
        binding.dayViewContent.visibility = View.GONE
        
        // Cancel other observers and start monthly observers
        dateDependentJob?.cancel()
        weekDependentJob?.cancel()
        monthDependentJob?.cancel()
        startMonthlyObservers()
    }
    
    private fun navigateWeek(offset: Int) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedWeekStartMillis }
        cal.add(Calendar.WEEK_OF_YEAR, offset)
        selectedWeekStartMillis = cal.timeInMillis
        updateWeekNavigationText()
        weekDependentJob?.cancel()
        startWeeklyObservers()
    }
    
    private fun getStartOfWeek(dateMillis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    
    private fun updateWeekNavigationText() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedWeekStartMillis }
        val today = Calendar.getInstance()
        val thisWeekStart = getStartOfWeek(today.timeInMillis)
        val lastWeekStart = getStartOfWeek(today.apply { add(Calendar.WEEK_OF_YEAR, -1) }.timeInMillis)
        
        val weekLabel = when (selectedWeekStartMillis) {
            thisWeekStart -> "This Week"
            lastWeekStart -> "Last Week"
            else -> {
                val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
                "Week $weekOfYear"
            }
        }
        
        binding.weekLabel.text = weekLabel
        
        // Calculate week date range
        val startCal = Calendar.getInstance().apply { timeInMillis = selectedWeekStartMillis }
        val endCal = Calendar.getInstance().apply { 
            timeInMillis = selectedWeekStartMillis 
            add(Calendar.DAY_OF_MONTH, 6)
        }
        
        val startFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val endFormat = if (startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH)) {
            SimpleDateFormat("d, yyyy", Locale.getDefault())
        } else {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        }
        
        binding.weekDateRange.text = "${startFormat.format(startCal.time)} - ${endFormat.format(endCal.time)}"
    }
    
    private fun startWeeklyObservers() {
        weekDependentJob = lifecycleScope.launch {
            // Observe both foods and weights for the week
            launch {
                viewModel.getFoodsForWeek(selectedWeekStartMillis).collect { foods ->
                    updateWeeklySummary(foods)
                }
            }
            launch {
                viewModel.getWeightsForWeek(selectedWeekStartMillis).collect { weights ->
                    updateWeeklyWeightData(weights)
                }
            }
        }
    }
    
    private fun updateWeeklyWeightData(weights: List<com.copdhealthtracker.data.model.WeightEntry>) {
        // Filter to only current weights (not goals)
        val currentWeights = weights.filter { !it.isGoal }.sortedBy { it.date }
        
        if (currentWeights.isEmpty()) {
            binding.weeklyWeightSection.visibility = View.GONE
            return
        }
        
        binding.weeklyWeightSection.visibility = View.VISIBLE
        
        val startWeight = currentWeights.firstOrNull()
        val endWeight = currentWeights.lastOrNull()
        
        if (startWeight != null) {
            binding.weeklyWeightStart.text = "${startWeight.weight} lbs"
        } else {
            binding.weeklyWeightStart.text = "-- lbs"
        }
        
        if (endWeight != null) {
            binding.weeklyWeightEnd.text = "${endWeight.weight} lbs"
        } else {
            binding.weeklyWeightEnd.text = "-- lbs"
        }
        
        // Calculate change
        if (startWeight != null && endWeight != null && startWeight != endWeight) {
            val change = endWeight.weight - startWeight.weight
            val changeStr = if (change >= 0) "+%.1f lbs".format(change) else "%.1f lbs".format(change)
            binding.weeklyWeightChange.text = changeStr
            
            // Color code: green for loss, red for gain (can be adjusted based on goals)
            val changeColor = when {
                change < 0 -> "#22c55e" // green for loss
                change > 0 -> "#ef4444" // red for gain
                else -> "#6b7280" // gray for no change
            }
            binding.weeklyWeightChange.setTextColor(android.graphics.Color.parseColor(changeColor))
        } else {
            binding.weeklyWeightChange.text = "No change"
            binding.weeklyWeightChange.setTextColor(android.graphics.Color.parseColor("#6b7280"))
        }
    }
    
    private fun updateWeeklySummary(foods: List<FoodEntry>) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val proteinTarget = prefs.getFloat("protein_target", 0f).toInt()
        
        // Group foods by day
        val foodsByDay = mutableMapOf<Int, MutableList<FoodEntry>>()
        for (i in 0..6) {
            foodsByDay[i] = mutableListOf()
        }
        
        val weekStartCal = Calendar.getInstance().apply { timeInMillis = selectedWeekStartMillis }
        
        foods.forEach { food ->
            val daysDiff = ((food.date - selectedWeekStartMillis) / (24 * 60 * 60 * 1000)).toInt()
            if (daysDiff in 0..6) {
                foodsByDay[daysDiff]?.add(food)
            }
        }
        
        // Calculate totals and averages
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0
        var daysLogged = 0
        var daysMetProteinGoal = 0
        
        val dailyCalories = mutableListOf<Int>()
        val dailyProtein = mutableListOf<Int>()
        val dailyCarbs = mutableListOf<Int>()
        val dailyFat = mutableListOf<Int>()
        
        for (i in 0..6) {
            val dayFoods = foodsByDay[i] ?: emptyList()
            val dayCals = dayFoods.sumOf { it.calories }
            val dayProt = dayFoods.sumOf { it.protein }
            val dayCarb = dayFoods.sumOf { it.carbs }
            val dayFt = dayFoods.sumOf { it.fat }
            
            dailyCalories.add(dayCals.toInt())
            dailyProtein.add(dayProt.toInt())
            dailyCarbs.add(dayCarb.toInt())
            dailyFat.add(dayFt.toInt())
            
            if (dayFoods.isNotEmpty()) {
                daysLogged++
                totalCalories += dayCals
                totalProtein += dayProt
                totalCarbs += dayCarb
                totalFat += dayFt
                
                if (proteinTarget > 0 && dayProt >= proteinTarget) {
                    daysMetProteinGoal++
                }
            }
        }
        
        // Calculate averages (only for days with data)
        val avgCalories = if (daysLogged > 0) (totalCalories / daysLogged).toInt() else 0
        val avgProtein = if (daysLogged > 0) (totalProtein / daysLogged).toInt() else 0
        val avgCarbs = if (daysLogged > 0) (totalCarbs / daysLogged).toInt() else 0
        val avgFat = if (daysLogged > 0) (totalFat / daysLogged).toInt() else 0
        
        // Update trend report UI
        binding.weeklyAvgCalories.text = avgCalories.toString()
        binding.weeklyAvgProtein.text = "${avgProtein}g"
        binding.weeklyAvgCarbs.text = "${avgCarbs}g"
        binding.weeklyAvgFat.text = "${avgFat}g"
        binding.weeklyTotalCalories.text = "${totalCalories.toInt()} kcal"
        binding.weeklyDaysLogged.text = "$daysLogged / 7"
        binding.weeklyProteinGoalDays.text = if (proteinTarget > 0) "$daysMetProteinGoal / 7" else "Set protein target"
        
        // Build daily breakdown cards
        binding.weeklyDaysContainer.removeAllViews()
        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        
        for (i in 0..6) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = selectedWeekStartMillis
                add(Calendar.DAY_OF_MONTH, i)
            }
            
            // Adjust day index based on first day of week
            val dayIndex = (weekStartCal.get(Calendar.DAY_OF_WEEK) - 1 + i) % 7
            val dayName = dayNames[dayIndex]
            val dateStr = dateFormat.format(dayCal.time)
            
            val dayCard = createDayCard(
                dayName = dayName,
                dateStr = dateStr,
                calories = dailyCalories[i],
                protein = dailyProtein[i],
                carbs = dailyCarbs[i],
                fat = dailyFat[i],
                hasData = (foodsByDay[i]?.isNotEmpty() == true),
                proteinTarget = proteinTarget
            )
            binding.weeklyDaysContainer.addView(dayCard)
        }
    }
    
    private fun createDayCard(
        dayName: String,
        dateStr: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        hasData: Boolean,
        proteinTarget: Int
    ): View {
        val density = resources.displayMetrics.density
        
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
            radius = 8 * density
            cardElevation = 2 * density
            setCardBackgroundColor(
                if (hasData) ContextCompat.getColor(requireContext(), R.color.backgroundWhite)
                else android.graphics.Color.parseColor("#F9FAFB")
            )
        }
        
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (16 * density).toInt(),
                (12 * density).toInt(),
                (16 * density).toInt(),
                (12 * density).toInt()
            )
        }
        
        // Header row with day name and date
        val headerRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        val dayText = TextView(requireContext()).apply {
            text = dayName
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val dateText = TextView(requireContext()).apply {
            text = dateStr
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
        }
        
        headerRow.addView(dayText)
        headerRow.addView(dateText)
        content.addView(headerRow)
        
        if (hasData) {
            // Nutrition summary row
            val nutritionRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (8 * density).toInt()
                }
            }
            
            // Calories
            val calColumn = createNutrientColumn("$calories", "kcal", "#22c55e")
            // Protein
            val proteinColor = if (proteinTarget > 0 && protein >= proteinTarget) "#22c55e" else "#3b82f6"
            val protColumn = createNutrientColumn("${protein}g", "Protein", proteinColor)
            // Carbs
            val carbColumn = createNutrientColumn("${carbs}g", "Carbs", "#f59e0b")
            // Fat
            val fatColumn = createNutrientColumn("${fat}g", "Fat", "#ef4444")
            
            nutritionRow.addView(calColumn)
            nutritionRow.addView(protColumn)
            nutritionRow.addView(carbColumn)
            nutritionRow.addView(fatColumn)
            content.addView(nutritionRow)
            
            // Protein target indicator
            if (proteinTarget > 0) {
                val proteinPercent = ((protein.toDouble() / proteinTarget) * 100).toInt()
                val targetText = TextView(requireContext()).apply {
                    text = if (protein >= proteinTarget) {
                        "Protein goal met"
                    } else {
                        "Protein: $proteinPercent% of target"
                    }
                    textSize = 11f
                    setTextColor(
                        if (protein >= proteinTarget) 
                            ContextCompat.getColor(requireContext(), R.color.colorSuccess)
                        else 
                            ContextCompat.getColor(requireContext(), R.color.textSecondary)
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (6 * density).toInt()
                    }
                }
                content.addView(targetText)
            }
        } else {
            // No data message
            val noDataText = TextView(requireContext()).apply {
                text = "No food logged"
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (8 * density).toInt()
                }
            }
            content.addView(noDataText)
        }
        
        card.addView(content)
        return card
    }
    
    private fun createNutrientColumn(value: String, label: String, color: String): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            
            addView(TextView(requireContext()).apply {
                text = value
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor(color))
                gravity = android.view.Gravity.CENTER
            })
            
            addView(TextView(requireContext()).apply {
                text = label
                textSize = 10f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                gravity = android.view.Gravity.CENTER
            })
        }
    }
    
    // Monthly view methods
    private fun navigateMonth(offset: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
        }
        cal.add(Calendar.MONTH, offset)
        selectedYear = cal.get(Calendar.YEAR)
        selectedMonth = cal.get(Calendar.MONTH)
        updateMonthLabel()
        monthDependentJob?.cancel()
        startMonthlyObservers()
    }
    
    private fun updateMonthLabel() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
        }
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.monthLabel.text = monthFormat.format(cal.time)
    }
    
    private fun startMonthlyObservers() {
        monthDependentJob = lifecycleScope.launch {
            // Observe both foods and weights for the month
            launch {
                viewModel.getFoodsForMonth(selectedYear, selectedMonth).collect { foods ->
                    updateMonthlySummary(foods)
                }
            }
            launch {
                viewModel.getWeightsForMonth(selectedYear, selectedMonth).collect { weights ->
                    updateMonthlyWeightData(weights)
                }
            }
        }
    }
    
    private fun updateMonthlyWeightData(weights: List<com.copdhealthtracker.data.model.WeightEntry>) {
        // Filter to only current weights (not goals)
        val currentWeights = weights.filter { !it.isGoal }.sortedBy { it.date }
        
        if (currentWeights.isEmpty()) {
            binding.monthlyWeightSection.visibility = View.GONE
            return
        }
        
        binding.monthlyWeightSection.visibility = View.VISIBLE
        
        val startWeight = currentWeights.firstOrNull()
        val endWeight = currentWeights.lastOrNull()
        
        if (startWeight != null) {
            binding.monthlyWeightStart.text = "${startWeight.weight} lbs"
        } else {
            binding.monthlyWeightStart.text = "-- lbs"
        }
        
        if (endWeight != null) {
            binding.monthlyWeightEnd.text = "${endWeight.weight} lbs"
        } else {
            binding.monthlyWeightEnd.text = "-- lbs"
        }
        
        // Calculate change
        if (startWeight != null && endWeight != null && startWeight != endWeight) {
            val change = endWeight.weight - startWeight.weight
            val changeStr = if (change >= 0) "+%.1f lbs".format(change) else "%.1f lbs".format(change)
            binding.monthlyWeightChange.text = changeStr
            
            // Color code: green for loss, red for gain (can be adjusted based on goals)
            val changeColor = when {
                change < 0 -> "#22c55e" // green for loss
                change > 0 -> "#ef4444" // red for gain
                else -> "#6b7280" // gray for no change
            }
            binding.monthlyWeightChange.setTextColor(android.graphics.Color.parseColor(changeColor))
        } else {
            binding.monthlyWeightChange.text = "No change"
            binding.monthlyWeightChange.setTextColor(android.graphics.Color.parseColor("#6b7280"))
        }
    }
    
    private fun updateMonthlySummary(foods: List<FoodEntry>) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val proteinTarget = prefs.getFloat("protein_target", 0f).toInt()
        
        // Get number of days in the selected month
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Group foods by day
        val foodsByDay = mutableMapOf<Int, MutableList<FoodEntry>>()
        for (i in 1..daysInMonth) {
            foodsByDay[i] = mutableListOf()
        }
        
        foods.forEach { food ->
            val foodCal = Calendar.getInstance().apply { timeInMillis = food.date }
            val dayOfMonth = foodCal.get(Calendar.DAY_OF_MONTH)
            if (dayOfMonth in 1..daysInMonth) {
                foodsByDay[dayOfMonth]?.add(food)
            }
        }
        
        // Calculate totals and averages
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0
        var daysLogged = 0
        var daysMetProteinGoal = 0
        
        // Track weekly data for breakdown
        val weeklyData = mutableListOf<WeekData>()
        var weekStartDay = 1
        var weekCalories = 0.0
        var weekProtein = 0.0
        var weekCarbs = 0.0
        var weekFat = 0.0
        var weekDaysLogged = 0
        
        for (day in 1..daysInMonth) {
            val dayFoods = foodsByDay[day] ?: emptyList()
            val dayCals = dayFoods.sumOf { it.calories }
            val dayProt = dayFoods.sumOf { it.protein }
            val dayCarb = dayFoods.sumOf { it.carbs }
            val dayFt = dayFoods.sumOf { it.fat }
            
            if (dayFoods.isNotEmpty()) {
                daysLogged++
                totalCalories += dayCals
                totalProtein += dayProt
                totalCarbs += dayCarb
                totalFat += dayFt
                weekDaysLogged++
                weekCalories += dayCals
                weekProtein += dayProt
                weekCarbs += dayCarb
                weekFat += dayFt
                
                if (proteinTarget > 0 && dayProt >= proteinTarget) {
                    daysMetProteinGoal++
                }
            }
            
            // Check if week is complete (every 7 days or end of month)
            val dayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, day)
            }
            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)
            
            if (dayOfWeek == Calendar.SATURDAY || day == daysInMonth) {
                weeklyData.add(WeekData(
                    startDay = weekStartDay,
                    endDay = day,
                    calories = weekCalories.toInt(),
                    protein = weekProtein.toInt(),
                    carbs = weekCarbs.toInt(),
                    fat = weekFat.toInt(),
                    daysLogged = weekDaysLogged
                ))
                weekStartDay = day + 1
                weekCalories = 0.0
                weekProtein = 0.0
                weekCarbs = 0.0
                weekFat = 0.0
                weekDaysLogged = 0
            }
        }
        
        // Calculate averages (only for days with data)
        val avgCalories = if (daysLogged > 0) (totalCalories / daysLogged).toInt() else 0
        val avgProtein = if (daysLogged > 0) (totalProtein / daysLogged).toInt() else 0
        val avgCarbs = if (daysLogged > 0) (totalCarbs / daysLogged).toInt() else 0
        val avgFat = if (daysLogged > 0) (totalFat / daysLogged).toInt() else 0
        
        // Update monthly trend report UI
        binding.monthlyAvgCalories.text = avgCalories.toString()
        binding.monthlyAvgProtein.text = "${avgProtein}g"
        binding.monthlyAvgCarbs.text = "${avgCarbs}g"
        binding.monthlyAvgFat.text = "${avgFat}g"
        binding.monthlyTotalCalories.text = "${totalCalories.toInt()} kcal"
        binding.monthlyDaysLogged.text = "$daysLogged / $daysInMonth"
        binding.monthlyProteinGoalDays.text = if (proteinTarget > 0) "$daysMetProteinGoal / $daysInMonth" else "Set protein target"
        
        // Build weekly breakdown cards
        binding.monthlyWeeksContainer.removeAllViews()
        
        weeklyData.forEachIndexed { index, week ->
            val weekCard = createWeekSummaryCard(
                weekNumber = index + 1,
                startDay = week.startDay,
                endDay = week.endDay,
                calories = week.calories,
                protein = week.protein,
                carbs = week.carbs,
                fat = week.fat,
                daysLogged = week.daysLogged,
                proteinTarget = proteinTarget
            )
            binding.monthlyWeeksContainer.addView(weekCard)
        }
    }
    
    private data class WeekData(
        val startDay: Int,
        val endDay: Int,
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int,
        val daysLogged: Int
    )
    
    private fun createWeekSummaryCard(
        weekNumber: Int,
        startDay: Int,
        endDay: Int,
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        daysLogged: Int,
        proteinTarget: Int
    ): View {
        val density = resources.displayMetrics.density
        val hasData = daysLogged > 0
        
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
            radius = 8 * density
            cardElevation = 2 * density
            setCardBackgroundColor(
                if (hasData) ContextCompat.getColor(requireContext(), R.color.backgroundWhite)
                else android.graphics.Color.parseColor("#F9FAFB")
            )
        }
        
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (16 * density).toInt(),
                (12 * density).toInt(),
                (16 * density).toInt(),
                (12 * density).toInt()
            )
        }
        
        // Header row with week name
        val headerRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
        }
        val monthName = monthFormat.format(cal.time)
        
        val weekText = TextView(requireContext()).apply {
            text = "Week $weekNumber"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val dateText = TextView(requireContext()).apply {
            text = "$monthName $startDay - $endDay"
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
        }
        
        headerRow.addView(weekText)
        headerRow.addView(dateText)
        content.addView(headerRow)
        
        if (hasData) {
            // Days logged info
            val daysText = TextView(requireContext()).apply {
                text = "$daysLogged days logged"
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (4 * density).toInt()
                }
            }
            content.addView(daysText)
            
            // Nutrition summary row
            val nutritionRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (8 * density).toInt()
                }
            }
            
            // Avg Calories
            val avgCal = if (daysLogged > 0) calories / daysLogged else 0
            val avgProt = if (daysLogged > 0) protein / daysLogged else 0
            val avgCarb = if (daysLogged > 0) carbs / daysLogged else 0
            val avgFt = if (daysLogged > 0) fat / daysLogged else 0
            
            val calColumn = createNutrientColumn("$avgCal", "Avg Cal", "#22c55e")
            val protColumn = createNutrientColumn("${avgProt}g", "Avg Prot", "#3b82f6")
            val carbColumn = createNutrientColumn("${avgCarb}g", "Avg Carb", "#f59e0b")
            val fatColumn = createNutrientColumn("${avgFt}g", "Avg Fat", "#ef4444")
            
            nutritionRow.addView(calColumn)
            nutritionRow.addView(protColumn)
            nutritionRow.addView(carbColumn)
            nutritionRow.addView(fatColumn)
            content.addView(nutritionRow)
            
            // Total calories for the week
            val totalText = TextView(requireContext()).apply {
                text = "Total: $calories kcal"
                textSize = 11f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (6 * density).toInt()
                }
            }
            content.addView(totalText)
        } else {
            // No data message
            val noDataText = TextView(requireContext()).apply {
                text = "No food logged"
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (8 * density).toInt()
                }
            }
            content.addView(noDataText)
        }
        
        card.addView(content)
        return card
    }
    
    private fun checkProfileSetup() {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val weight = prefs.getString("weight", null)
        val activityLevel = prefs.getString("activity_level", null)
        val hasCompletedProfile = !weight.isNullOrEmpty() && !activityLevel.isNullOrEmpty()
        
        binding.profileSetupBanner.visibility = if (hasCompletedProfile) View.GONE else View.VISIBLE
    }
    
    private fun toggleMealSection(contentView: LinearLayout, arrowView: TextView) {
        if (contentView.visibility == View.VISIBLE) {
            contentView.visibility = View.GONE
            arrowView.text = "v"
        } else {
            contentView.visibility = View.VISIBLE
            arrowView.text = "^"
        }
    }
    
    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDateMillis = cal.timeInMillis
                updateDateButtonText()
                dateDependentJob?.cancel()
                startDateDependentObservers()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun navigateDay(offset: Int) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        cal.add(Calendar.DAY_OF_MONTH, offset)
        selectedDateMillis = cal.timeInMillis
        updateDateButtonText()
        dateDependentJob?.cancel()
        startDateDependentObservers()
    }
    
    private fun updateDateButtonText() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        
        val dateLabel = when {
            isSameDay(cal, today) -> "Today"
            isSameDay(cal, yesterday) -> "Yesterday"
            isSameDay(cal, tomorrow) -> "Tomorrow"
            else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time)
        }
        
        val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(cal.time)
        binding.dateLabel.text = dateLabel
        binding.datePickerButton.text = dateStr
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun observeData() {
        startDateDependentObservers()
        
        // Medications count and list
        lifecycleScope.launch {
            combine(
                viewModel.getDailyMedications(),
                viewModel.getExacerbationMedications()
            ) { daily, exacerbation -> Pair(daily, exacerbation) }.collect { (dailyMeds, exacerbationMeds) ->
                binding.medicationCount.text = "${dailyMeds.size} daily"
                
                // Display daily medications list
                binding.dailyMedsList.removeAllViews()
                if (dailyMeds.isNotEmpty()) {
                    binding.dailyMedsLabel.visibility = View.VISIBLE
                    dailyMeds.forEach { med ->
                        val medRow = TextView(requireContext()).apply {
                            text = "${med.name} - ${med.dosage}"
                            textSize = 16f
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
                            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.backgroundGray))
                            setPadding(
                                (10 * resources.displayMetrics.density).toInt(),
                                (8 * resources.displayMetrics.density).toInt(),
                                (10 * resources.displayMetrics.density).toInt(),
                                (8 * resources.displayMetrics.density).toInt()
                            )
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = (4 * resources.displayMetrics.density).toInt()
                            }
                        }
                        binding.dailyMedsList.addView(medRow)
                    }
                } else {
                    binding.dailyMedsLabel.visibility = View.GONE
                }
                
                // Display exacerbation medications list
                binding.exacerbationMedsList.removeAllViews()
                if (exacerbationMeds.isNotEmpty()) {
                    binding.exacerbationMedsLabel.visibility = View.VISIBLE
                    exacerbationMeds.forEach { med ->
                        val medRow = TextView(requireContext()).apply {
                            text = "${med.name} - ${med.dosage}"
                            textSize = 16f
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
                            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.backgroundGray))
                            setPadding(
                                (10 * resources.displayMetrics.density).toInt(),
                                (8 * resources.displayMetrics.density).toInt(),
                                (10 * resources.displayMetrics.density).toInt(),
                                (8 * resources.displayMetrics.density).toInt()
                            )
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = (4 * resources.displayMetrics.density).toInt()
                            }
                        }
                        binding.exacerbationMedsList.addView(medRow)
                    }
                } else {
                    binding.exacerbationMedsLabel.visibility = View.GONE
                }
            }
        }
    }
    
    private fun startDateDependentObservers() {
        dateDependentJob = lifecycleScope.launch {
            // Food items by category
            launch {
                viewModel.getFoodsForDate(selectedDateMillis).collect { foods ->
                    val breakfast = foods.filter { it.mealCategory == "Breakfast" }
                    val lunch = foods.filter { it.mealCategory == "Lunch" }
                    val dinner = foods.filter { it.mealCategory == "Dinner" }
                    val snacks = foods.filter { it.mealCategory == "Snacks" }
                    
                    displayFoodItems(binding.breakfastContent, breakfast, "breakfast")
                    displayFoodItems(binding.lunchContent, lunch, "lunch")
                    displayFoodItems(binding.dinnerContent, dinner, "dinner")
                    displayFoodItems(binding.snacksContent, snacks, "snacks")
                    
                    // Update meal summaries
                    updateMealSummary(binding.breakfastSummary, breakfast)
                    updateMealSummary(binding.lunchSummary, lunch)
                    updateMealSummary(binding.dinnerSummary, dinner)
                    updateMealSummary(binding.snacksSummary, snacks)
                    
                    // Update macro and micronutrient targets display
                    updateMacroTargets(foods)
                    updateNutrientTargets(foods)
                }
            }
            
            // Exercise minutes
            launch {
                viewModel.getExercisesForDate(selectedDateMillis).collect { exercises ->
                    val totalMinutes = exercises.sumOf { it.minutes }
                    binding.exerciseValue.text = "$totalMinutes min"
                    
                    // Display exercise list
                    binding.exerciseList.removeAllViews()
                    exercises.forEach { exercise ->
                        val exerciseRow = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = (8 * resources.displayMetrics.density).toInt()
                            }
                            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.backgroundGray))
                            setPadding(
                                (12 * resources.displayMetrics.density).toInt(),
                                (8 * resources.displayMetrics.density).toInt(),
                                (12 * resources.displayMetrics.density).toInt(),
                                (8 * resources.displayMetrics.density).toInt()
                            )
                        }
                        
                        val nameText = TextView(requireContext()).apply {
                            text = exercise.type
                            textSize = 14f
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                        }
                        
                        val minutesText = TextView(requireContext()).apply {
                            text = "${exercise.minutes} min"
                            textSize = 14f
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.colorSuccess))
                            setTypeface(null, android.graphics.Typeface.BOLD)
                        }
                        
                        exerciseRow.addView(nameText)
                        exerciseRow.addView(minutesText)
                        binding.exerciseList.addView(exerciseRow)
                    }
                }
            }
            
            // Oxygen - get latest reading and list all readings
            launch {
                viewModel.getOxygenReadingsForDate(selectedDateMillis).collect { readings ->
                    if (readings.isNotEmpty()) {
                        val latest = readings.maxByOrNull { it.date }
                        binding.oxygenValue.text = "${latest?.level ?: "N/A"}%"
                        
                        // Show readings label and list
                        binding.oxygenReadingsLabel.visibility = View.VISIBLE
                        binding.oxygenReadingsLabel.text = "Today's Readings (${readings.size})"
                        
                        // Display oxygen readings list
                        binding.oxygenList.removeAllViews()
                        val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
                        readings.sortedByDescending { it.date }.forEach { reading ->
                            val readingRow = LinearLayout(requireContext()).apply {
                                orientation = LinearLayout.HORIZONTAL
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    topMargin = (6 * resources.displayMetrics.density).toInt()
                                }
                                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.backgroundGray))
                                setPadding(
                                    (12 * resources.displayMetrics.density).toInt(),
                                    (8 * resources.displayMetrics.density).toInt(),
                                    (12 * resources.displayMetrics.density).toInt(),
                                    (8 * resources.displayMetrics.density).toInt()
                                )
                            }
                            
                            val levelText = TextView(requireContext()).apply {
                                text = "${reading.level}%"
                                textSize = 14f
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorSuccess))
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1f
                                )
                            }
                            
                            val timeText = TextView(requireContext()).apply {
                                text = timeFormat.format(reading.date)
                                textSize = 12f
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                            }
                            
                            readingRow.addView(levelText)
                            readingRow.addView(timeText)
                            binding.oxygenList.addView(readingRow)
                        }
                    } else {
                        binding.oxygenValue.text = "N/A"
                        binding.oxygenReadingsLabel.visibility = View.GONE
                        binding.oxygenList.removeAllViews()
                    }
                }
            }
            
            // Weight - card shows global latest goal and current (so they appear for any selected day)
            launch {
                combine(
                    viewModel.getLatestCurrentWeight(),
                    viewModel.getLatestGoalWeight()
                ) { current, goal -> Pair(current, goal) }.collect { (latestCurrent, latestGoal) ->
                    if (latestCurrent != null) {
                        binding.weightValue.text = "${latestCurrent.weight} lbs"
                    } else {
                        binding.weightValue.text = "N/A"
                    }
                    if (latestGoal != null) {
                        binding.goalWeightLabel.visibility = View.VISIBLE
                        binding.goalWeightLabel.text = "Goal: ${latestGoal.weight} lbs"
                    } else {
                        binding.goalWeightLabel.visibility = View.GONE
                    }
                }
            }

            // Weight - list of entries for the selected day only
            launch {
                viewModel.getWeightsForDate(selectedDateMillis).collect { weights ->
                    binding.weightList.removeAllViews()
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    weights.sortedByDescending { it.date }.forEach { entry ->
                        val typeLabel = if (entry.isGoal) "Goal" else "Current"
                        val labelColor = if (entry.isGoal) R.color.colorSuccess else R.color.colorPrimary
                        val timeStr = timeFormat.format(entry.date)
                        
                        val weightContainer = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.backgroundGray))
                            setPadding(
                                (8 * resources.displayMetrics.density).toInt(),
                                (6 * resources.displayMetrics.density).toInt(),
                                (8 * resources.displayMetrics.density).toInt(),
                                (6 * resources.displayMetrics.density).toInt()
                            )
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = (4 * resources.displayMetrics.density).toInt()
                            }
                        }
                        
                        // First line: Type + Weight
                        val mainText = TextView(requireContext()).apply {
                            textSize = 12f
                            setSingleLine(true)
                        }
                        val mainFullText = "$typeLabel  ${entry.weight} lbs"
                        val mainSpannable = android.text.SpannableString(mainFullText)
                        mainSpannable.setSpan(
                            android.text.style.ForegroundColorSpan(ContextCompat.getColor(requireContext(), labelColor)),
                            0,
                            typeLabel.length,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        mainSpannable.setSpan(
                            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                            0,
                            typeLabel.length,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        mainSpannable.setSpan(
                            android.text.style.ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.textPrimary)),
                            typeLabel.length,
                            mainFullText.length,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        mainText.text = mainSpannable
                        
                        // Second line: Time (aligned right)
                        val timeText = TextView(requireContext()).apply {
                            text = timeStr
                            textSize = 10f
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            gravity = android.view.Gravity.END
                        }
                        
                        weightContainer.addView(mainText)
                        weightContainer.addView(timeText)
                        binding.weightList.addView(weightContainer)
                    }
                }
            }
            
            // Water intake tracking
            launch {
                viewModel.getWaterEntriesForDate(selectedDateMillis).collect { entries ->
                    updateWaterDisplay(entries)
                }
            }
        }
    }
    
    private fun updateMealSummary(summaryView: TextView, foods: List<FoodEntry>) {
        if (foods.isEmpty()) {
            summaryView.text = "No items logged"
            return
        }
        
        val totalCalories = foods.sumOf { it.calories }.toInt()
        val totalProtein = foods.sumOf { it.protein }.toInt()
        val totalCarbs = foods.sumOf { it.carbs }.toInt()
        val totalFat = foods.sumOf { it.fat }.toInt()
        
        summaryView.text = "$totalCalories kcal, ${totalProtein}g protein, ${totalCarbs}g carbs, ${totalFat}g fat"
    }
    
    private fun updateMacroTargets(foods: List<FoodEntry>) {
        // Get personalized targets from SharedPreferences
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        
        // Default targets
        val energyTarget = 2000.0   // kcal
        val carbsTarget = 250.0     // g
        val fatTarget = 65.0        // g
        
        // Check if user has set up their profile (weight and activity level)
        val weight = prefs.getString("weight", null)
        val activityLevel = prefs.getString("activity_level", null)
        val hasCompletedProfile = !weight.isNullOrEmpty() && !activityLevel.isNullOrEmpty()
        
        // Get personalized protein target (calculated based on weight and activity level in Profile)
        val proteinTarget = if (hasCompletedProfile) {
            prefs.getFloat("protein_target", 0f).toDouble()
        } else {
            0.0  // Not set yet
        }
        
        // Calculate totals
        val totalEnergy = foods.sumOf { it.calories }
        val totalProtein = foods.sumOf { it.protein }
        val totalCarbs = foods.sumOf { it.carbs }
        val totalFat = foods.sumOf { it.fat }
        
        // Calculate percentages
        val energyPercent = ((totalEnergy / energyTarget) * 100).toInt()
        val carbsPercent = ((totalCarbs / carbsTarget) * 100).toInt()
        val fatPercent = ((totalFat / fatTarget) * 100).toInt()
        
        // Update Energy
        binding.energyLabel.text = "Energy - ${String.format("%.1f", totalEnergy)} / ${energyTarget.toInt()} kcal"
        binding.energyPercent.text = "${energyPercent}%"
        binding.energyProgress.progress = energyPercent.coerceAtMost(100)
        updateProgressColor(binding.energyPercent, binding.energyProgress, energyPercent, "#22c55e", "#f97316")
        
        // Update Protein (personalized target or prompt to set up)
        if (hasCompletedProfile && proteinTarget > 0) {
            val proteinPercent = ((totalProtein / proteinTarget) * 100).toInt()
            binding.proteinLabel.text = "Protein - ${String.format("%.1f", totalProtein)} / ${proteinTarget.toInt()} g"
            binding.proteinPercent.text = "${proteinPercent}%"
            binding.proteinProgress.progress = proteinPercent.coerceAtMost(100)
            updateProgressColor(binding.proteinPercent, binding.proteinProgress, proteinPercent, "#22c55e", "#f97316")
        } else {
            // User hasn't set up profile - show prompt instead of percentage
            binding.proteinLabel.text = "Protein - ${String.format("%.1f", totalProtein)} g"
            binding.proteinPercent.text = "Set up"
            binding.proteinPercent.setTextColor(android.graphics.Color.parseColor("#2563eb"))
            binding.proteinProgress.progress = 0
            
            // Make protein row clickable to go to Profile
            binding.proteinLabel.setOnClickListener { navigateToProfile() }
            binding.proteinPercent.setOnClickListener { navigateToProfile() }
        }
        
        // Update Carbs
        binding.carbsLabel.text = "Net Carbs - ${String.format("%.1f", totalCarbs)} / ${carbsTarget.toInt()} g"
        binding.carbsPercent.text = "${carbsPercent}%"
        binding.carbsProgress.progress = carbsPercent.coerceAtMost(100)
        updateProgressColor(binding.carbsPercent, binding.carbsProgress, carbsPercent, "#3b82f6", "#f97316")
        
        // Update Fat
        binding.fatLabel.text = "Fat - ${String.format("%.1f", totalFat)} / ${fatTarget.toInt()} g"
        binding.fatPercent.text = "${fatPercent}%"
        binding.fatProgress.progress = fatPercent.coerceAtMost(100)
        updateProgressColor(binding.fatPercent, binding.fatProgress, fatPercent, "#f97316", "#ef4444")
    }
    
    private fun navigateToProfile() {
        // Navigate to Profile tab to set up protein target
        val activity = requireActivity()
        if (activity is com.copdhealthtracker.MainActivity) {
            activity.switchToProfile()
        }
    }
    
    private fun updateProgressColor(percentView: TextView, progressBar: ProgressBar, percent: Int, normalColor: String, overColor: String) {
        val color = if (percent > 100) overColor else normalColor
        percentView.setTextColor(android.graphics.Color.parseColor(color))
        progressBar.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(color))
    }
    
    private fun updateNutrientTargets(foods: List<FoodEntry>) {
        // Daily recommended values (can be customized later in profile)
        // Fat Breakdown targets
        val saturatedFatTarget = 20.0   // g (limit)
        val cholesterolTarget = 300.0   // mg (limit)
        val omega3Target = 1.6          // g
        val addedSugarsTarget = 50.0    // g (limit)
        
        // Minerals targets
        val calciumTarget = 1000.0      // mg
        val ironTarget = 18.0           // mg
        val magnesiumTarget = 420.0     // mg
        val zincTarget = 11.0           // mg
        val potassiumTarget = 4700.0    // mg
        val sodiumTarget = 2300.0       // mg (limit)
        val seleniumTarget = 55.0       // mcg
        val manganeseTarget = 2.3       // mg
        
        // Vitamins and Fiber targets
        val fiberTarget = 25.0          // g
        val vitaminATarget = 900.0      // mcg RAE
        val vitaminCTarget = 90.0       // mg
        val vitaminDTarget = 20.0       // mcg
        val vitaminETarget = 15.0       // mg
        val vitaminKTarget = 120.0      // mcg
        val waterTarget = 3700.0        // ml (3.7L)
        
        // Sum up all nutrients from foods
        val totalSaturatedFat = foods.sumOf { it.saturatedFat }
        val totalCholesterol = foods.sumOf { it.cholesterol }
        val totalOmega3 = foods.sumOf { it.omega3 }
        val totalAddedSugars = foods.sumOf { it.addedSugars }
        
        val totalCalcium = foods.sumOf { it.calcium }
        val totalIron = foods.sumOf { it.iron }
        val totalMagnesium = foods.sumOf { it.magnesium }
        val totalZinc = foods.sumOf { it.zinc }
        val totalPotassium = foods.sumOf { it.potassium }
        val totalSodium = foods.sumOf { it.sodium }
        val totalSelenium = foods.sumOf { it.selenium }
        val totalManganese = foods.sumOf { it.manganese }
        
        val totalFiber = foods.sumOf { it.fiber }
        val totalVitaminA = foods.sumOf { it.vitaminA }
        val totalVitaminC = foods.sumOf { it.vitaminC }
        val totalVitaminD = foods.sumOf { it.vitaminD }
        val totalVitaminE = foods.sumOf { it.vitaminE }
        val totalVitaminK = foods.sumOf { it.vitaminK }
        val totalWater = foods.sumOf { it.water }
        
        // Calculate percentages
        val saturatedFatPercent = ((totalSaturatedFat / saturatedFatTarget) * 100).toInt().coerceAtMost(200)
        val cholesterolPercent = ((totalCholesterol / cholesterolTarget) * 100).toInt().coerceAtMost(200)
        val omega3Percent = ((totalOmega3 / omega3Target) * 100).toInt().coerceAtMost(200)
        val addedSugarsPercent = ((totalAddedSugars / addedSugarsTarget) * 100).toInt().coerceAtMost(200)
        
        val calciumPercent = ((totalCalcium / calciumTarget) * 100).toInt().coerceAtMost(200)
        val ironPercent = ((totalIron / ironTarget) * 100).toInt().coerceAtMost(200)
        val magnesiumPercent = ((totalMagnesium / magnesiumTarget) * 100).toInt().coerceAtMost(200)
        val zincPercent = ((totalZinc / zincTarget) * 100).toInt().coerceAtMost(200)
        val potassiumPercent = ((totalPotassium / potassiumTarget) * 100).toInt().coerceAtMost(200)
        val sodiumPercent = ((totalSodium / sodiumTarget) * 100).toInt().coerceAtMost(200)
        val seleniumPercent = ((totalSelenium / seleniumTarget) * 100).toInt().coerceAtMost(200)
        val manganesePercent = ((totalManganese / manganeseTarget) * 100).toInt().coerceAtMost(200)
        
        val fiberPercent = ((totalFiber / fiberTarget) * 100).toInt().coerceAtMost(200)
        val vitaminAPercent = ((totalVitaminA / vitaminATarget) * 100).toInt().coerceAtMost(200)
        val vitaminCPercent = ((totalVitaminC / vitaminCTarget) * 100).toInt().coerceAtMost(200)
        val vitaminDPercent = ((totalVitaminD / vitaminDTarget) * 100).toInt().coerceAtMost(200)
        val vitaminEPercent = ((totalVitaminE / vitaminETarget) * 100).toInt().coerceAtMost(200)
        val vitaminKPercent = ((totalVitaminK / vitaminKTarget) * 100).toInt().coerceAtMost(200)
        val waterPercent = ((totalWater / waterTarget) * 100).toInt().coerceAtMost(200)
        
        // Update Fat Breakdown UI
        binding.saturatedFatPercent.text = "${saturatedFatPercent}%"
        binding.saturatedFatProgress.progress = saturatedFatPercent.coerceAtMost(100)
        updateLimitProgressColor(binding.saturatedFatProgress, saturatedFatPercent)
        
        binding.cholesterolPercent.text = "${cholesterolPercent}%"
        binding.cholesterolProgress.progress = cholesterolPercent.coerceAtMost(100)
        updateLimitProgressColor(binding.cholesterolProgress, cholesterolPercent)
        
        binding.omega3Percent.text = "${omega3Percent}%"
        binding.omega3Progress.progress = omega3Percent.coerceAtMost(100)
        
        binding.addedSugarsPercent.text = "${addedSugarsPercent}%"
        binding.addedSugarsProgress.progress = addedSugarsPercent.coerceAtMost(100)
        updateLimitProgressColor(binding.addedSugarsProgress, addedSugarsPercent)
        
        // Update Minerals UI
        binding.calciumPercent.text = "${calciumPercent}%"
        binding.calciumProgress.progress = calciumPercent.coerceAtMost(100)
        
        binding.ironPercent.text = "${ironPercent}%"
        binding.ironProgress.progress = ironPercent.coerceAtMost(100)
        
        binding.magnesiumPercent.text = "${magnesiumPercent}%"
        binding.magnesiumProgress.progress = magnesiumPercent.coerceAtMost(100)
        
        binding.zincPercent.text = "${zincPercent}%"
        binding.zincProgress.progress = zincPercent.coerceAtMost(100)
        
        binding.potassiumPercent.text = "${potassiumPercent}%"
        binding.potassiumProgress.progress = potassiumPercent.coerceAtMost(100)
        
        binding.sodiumPercent.text = "${sodiumPercent}%"
        binding.sodiumProgress.progress = sodiumPercent.coerceAtMost(100)
        updateLimitProgressColor(binding.sodiumProgress, sodiumPercent)
        
        binding.seleniumPercent.text = "${seleniumPercent}%"
        binding.seleniumProgress.progress = seleniumPercent.coerceAtMost(100)
        
        binding.manganesePercent.text = "${manganesePercent}%"
        binding.manganeseProgress.progress = manganesePercent.coerceAtMost(100)
        
        // Update Vitamins and Fiber UI
        binding.fiberPercent.text = "${fiberPercent}%"
        binding.fiberProgress.progress = fiberPercent.coerceAtMost(100)
        
        binding.vitaminAPercent.text = "${vitaminAPercent}%"
        binding.vitaminAProgress.progress = vitaminAPercent.coerceAtMost(100)
        
        binding.vitaminCPercent.text = "${vitaminCPercent}%"
        binding.vitaminCProgress.progress = vitaminCPercent.coerceAtMost(100)
        
        binding.vitaminDPercent.text = "${vitaminDPercent}%"
        binding.vitaminDProgress.progress = vitaminDPercent.coerceAtMost(100)
        
        binding.vitaminEPercent.text = "${vitaminEPercent}%"
        binding.vitaminEProgress.progress = vitaminEPercent.coerceAtMost(100)
        
        binding.vitaminKPercent.text = "${vitaminKPercent}%"
        binding.vitaminKProgress.progress = vitaminKPercent.coerceAtMost(100)
        
        binding.waterPercent.text = "${waterPercent}%"
        binding.waterProgress.progress = waterPercent.coerceAtMost(100)
    }
    
    private fun updateLimitProgressColor(progressBar: ProgressBar, percent: Int) {
        // For nutrients with limits (not goals), turn red when over 100%
        val color = if (percent > 100) "#ef4444" else "#f97316"
        progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(color)
        )
    }
    
    private fun displayFoodItems(container: LinearLayout, foods: List<FoodEntry>, category: String) {
        container.removeAllViews()
        
        if (foods.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "No items logged"
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
            }
            container.addView(emptyText)
            return
        }
        
        foods.forEach { food ->
            val cardView = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (8 * resources.displayMetrics.density).toInt()
                }
                radius = 12 * resources.displayMetrics.density
                cardElevation = 2 * resources.displayMetrics.density
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.cardBackground))
            }
            
            val cardContent = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(
                    (12 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt()
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            
            // Food icon
            val iconView = android.widget.ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (40 * resources.displayMetrics.density).toInt(),
                    (40 * resources.displayMetrics.density).toInt()
                ).apply {
                    marginEnd = (12 * resources.displayMetrics.density).toInt()
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                setImageResource(R.drawable.ic_food_item)
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            }
            
            // Info column
            val infoColumn = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            
            // Delete button
            val deleteButton = android.widget.ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (28 * resources.displayMetrics.density).toInt(),
                    (28 * resources.displayMetrics.density).toInt()
                ).apply {
                    marginStart = (8 * resources.displayMetrics.density).toInt()
                }
                setImageResource(R.drawable.ic_delete)
                setPadding(
                    (2 * resources.displayMetrics.density).toInt(),
                    (2 * resources.displayMetrics.density).toInt(),
                    (2 * resources.displayMetrics.density).toInt(),
                    (2 * resources.displayMetrics.density).toInt()
                )
                isClickable = true
                isFocusable = true
                contentDescription = "Delete food"
                
                // Add ripple effect background
                val attrs = intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                val ta = requireContext().obtainStyledAttributes(attrs)
                background = ta.getDrawable(0)
                ta.recycle()
                
                setOnClickListener {
                    // Show confirmation dialog
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Delete Food")
                        .setMessage("Are you sure you want to delete \"${food.name}\"?")
                        .setPositiveButton("Delete") { _, _ ->
                            viewModel.deleteFood(food)
                            android.widget.Toast.makeText(requireContext(), "Food deleted", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            
            // Food name (bold, uppercase)
            val nameText = TextView(requireContext()).apply {
                text = food.name.uppercase()
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            
            // Quantity/description
            val quantityText = TextView(requireContext()).apply {
                text = food.quantity
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                setPadding(0, (2 * resources.displayMetrics.density).toInt(), 0, (8 * resources.displayMetrics.density).toInt())
            }
            
            // Nutrition row
            val nutritionRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            
            // Create nutrition boxes
            val nutritionItems = listOf(
                "Calories" to food.calories.toInt().toString(),
                "Protein" to String.format("%.1fg", food.protein),
                "Carbs" to String.format("%.1fg", food.carbs),
                "Fat" to String.format("%.1fg", food.fat)
            )
            
            nutritionItems.forEach { (label, value) ->
                val nutrientBox = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        marginEnd = (4 * resources.displayMetrics.density).toInt()
                    }
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.backgroundGray))
                    setPadding(
                        (6 * resources.displayMetrics.density).toInt(),
                        (4 * resources.displayMetrics.density).toInt(),
                        (6 * resources.displayMetrics.density).toInt(),
                        (4 * resources.displayMetrics.density).toInt()
                    )
                }
                
                val labelText = TextView(requireContext()).apply {
                    text = label
                    textSize = 10f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                }
                
                val valueText = TextView(requireContext()).apply {
                    text = value
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                
                nutrientBox.addView(labelText)
                nutrientBox.addView(valueText)
                nutritionRow.addView(nutrientBox)
            }
            
            infoColumn.addView(nameText)
            infoColumn.addView(quantityText)
            infoColumn.addView(nutritionRow)
            
            cardContent.addView(iconView)
            cardContent.addView(infoColumn)
            cardContent.addView(deleteButton)
            cardView.addView(cardContent)
            container.addView(cardView)
        }
    }
    
    private fun showFoodDialog() {
        val dialog = AddFoodDialog(
            onSave = { viewModel.insertFood(it) },
            dateForEntry = selectedDateMillis,
            onSaveFavorite = { viewModel.insertFavoriteFood(it) },
            getAllUserAddedFoods = { viewModel.getAllUserAddedFoods() },
            insertUserAddedFood = { viewModel.insertUserAddedFood(it) }
        )
        dialog.show(parentFragmentManager, "AddFoodDialog")
    }

    private fun showFavoritesDialog() {
        lifecycleScope.launch {
            val favorites = viewModel.getFavoriteFoods().first()
            val meals = viewModel.getFavoriteMeals().first()
            if (favorites.isEmpty() && meals.isEmpty()) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "No favorites yet. Add a food (or create a meal) and save as favorite.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val singleLabels = favorites.map { "${it.label} (${it.mealCategory})" }
            val mealLabels = meals.map { "Meal: ${it.label} (${it.mealCategory})" }
            val allLabels = singleLabels + mealLabels
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add from favorites")
                .setItems(allLabels.toTypedArray()) { _, which ->
                    if (which < favorites.size) {
                        showMealCategoryPicker(favorites[which])
                    } else {
                        val meal = meals[which - favorites.size]
                        showMealCategoryPickerForMeal(meal)
                    }
                }
                .setNeutralButton("Delete a favorite or meal") { _, _ ->
                    showDeleteFavoriteOrMealDialog()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showMealCategoryPickerForMeal(meal: FavoriteMeal) {
        lifecycleScope.launch {
            val pair = viewModel.getFavoriteMealWithItems(meal.id) ?: return@launch
            val (_, items) = pair
            if (items.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "This meal has no items.", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            val mealCategories = arrayOf("Breakfast", "Lunch", "Dinner", "Snacks")
            val defaultIndex = mealCategories.indexOf(meal.mealCategory).coerceAtLeast(0)
            var selectedIndex = defaultIndex
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add meal to which category?")
                .setSingleChoiceItems(mealCategories, defaultIndex) { _, which ->
                    selectedIndex = which
                }
                .setPositiveButton("Add") { _, _ ->
                    val chosenCategory = mealCategories[selectedIndex]
                    items.forEach { item ->
                        viewModel.insertFood(item.toFoodEntry(selectedDateMillis, chosenCategory))
                    }
                    android.widget.Toast.makeText(requireContext(), "Added ${items.size} items to $chosenCategory: ${meal.label}", android.widget.Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showCreateMealDialog() {
        val dialog = CreateMealDialog { meal, items ->
            viewModel.insertFavoriteMeal(meal, items)
        }
        dialog.show(parentFragmentManager, "CreateMealDialog")
    }

    private fun showDeleteFavoriteOrMealDialog() {
        lifecycleScope.launch {
            val favorites = viewModel.getFavoriteFoods().first()
            val meals = viewModel.getFavoriteMeals().first()
            if (favorites.isEmpty() && meals.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "No favorites or meals to delete.", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            val singleLabels = favorites.map { "${it.label} (${it.mealCategory})" }
            val mealLabels = meals.map { "Meal: ${it.label} (${it.mealCategory})" }
            val allLabels = (singleLabels + mealLabels).toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete a favorite or meal")
                .setItems(allLabels) { _, which ->
                    if (which < favorites.size) {
                        val favorite = favorites[which]
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Delete favorite?")
                            .setMessage("Remove \"${favorite.label}\" from your favorites? This does not remove it from your food log.")
                            .setPositiveButton("Delete") { _, _ ->
                                viewModel.deleteFavoriteFood(favorite)
                                android.widget.Toast.makeText(requireContext(), "Removed from favorites", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    } else {
                        val meal = meals[which - favorites.size]
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Delete meal?")
                            .setMessage("Remove meal \"${meal.label}\" from your favorites? This does not remove past log entries.")
                            .setPositiveButton("Delete") { _, _ ->
                                viewModel.deleteFavoriteMeal(meal)
                                android.widget.Toast.makeText(requireContext(), "Meal removed from favorites", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showMealCategoryPicker(favorite: FavoriteFood) {
        val mealCategories = arrayOf("Breakfast", "Lunch", "Dinner", "Snacks")
        val defaultIndex = mealCategories.indexOf(favorite.mealCategory).coerceAtLeast(0)
        var selectedIndex = defaultIndex
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add to which meal?")
            .setSingleChoiceItems(mealCategories, defaultIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Add") { _, _ ->
                val chosenCategory = mealCategories[selectedIndex]
                viewModel.insertFood(favorite.toFoodEntry(selectedDateMillis, chosenCategory))
                android.widget.Toast.makeText(requireContext(), "Added to $chosenCategory: ${favorite.label}", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showExerciseDialog() {
        val dialog = AddExerciseDialog { exerciseEntry ->
            viewModel.insertExercise(exerciseEntry)
        }
        dialog.show(parentFragmentManager, "AddExerciseDialog")
    }
    
    private fun showOxygenDialog() {
        val dialog = AddOxygenDialog { reading ->
            viewModel.insertOxygenReading(reading)
        }
        dialog.show(parentFragmentManager, "AddOxygenDialog")
    }
    
    private fun showWeightDialog() {
        val dialog = AddWeightDialog { weightEntry ->
            viewModel.insertWeight(weightEntry)
        }
        dialog.show(parentFragmentManager, "AddWeightDialog")
    }
    
    private fun addWater(amountOz: Int) {
        val entry = WaterEntry(amount = amountOz)
        viewModel.insertWaterEntry(entry)
        android.widget.Toast.makeText(requireContext(), "+$amountOz oz added", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun showCustomWaterDialog() {
        val ctx = requireContext()
        val input = android.widget.EditText(ctx).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Enter amount in oz"
            setPadding(48, 32, 48, 32)
        }
        
        android.app.AlertDialog.Builder(ctx)
            .setTitle("Add Water")
            .setMessage("How many ounces did you drink?")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val amountStr = input.text.toString()
                val amount = amountStr.toIntOrNull()
                if (amount != null && amount > 0) {
                    addWater(amount)
                } else {
                    android.widget.Toast.makeText(ctx, "Please enter a valid amount", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateWaterDisplay(entries: List<WaterEntry>) {
        val totalOz = entries.sumOf { it.amount }
        val goalOz = 64 // Default goal: 64 oz (8 cups)
        val percent = ((totalOz.toDouble() / goalOz) * 100).toInt().coerceAtMost(100)
        
        binding.hydrationTotalDisplay.text = "$totalOz oz"
        binding.hydrationGoalDisplay.text = "/ $goalOz oz"
        binding.hydrationProgress.progress = percent
        
        // Update progress bar color based on progress
        val progressColor = when {
            percent >= 100 -> "#22c55e" // Green when goal met
            percent >= 50 -> "#3b82f6"  // Blue for good progress
            else -> "#3b82f6"           // Blue default
        }
        binding.hydrationProgress.progressTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(progressColor)
        )
        
        // Show/hide entries list
        binding.hydrationEntriesList.removeAllViews()
        if (entries.isNotEmpty()) {
            binding.hydrationEntriesList.visibility = View.VISIBLE
            
            // Add header
            val headerText = TextView(requireContext()).apply {
                text = "Today's Drinks (${entries.size})"
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (8 * resources.displayMetrics.density).toInt()
                }
            }
                binding.hydrationEntriesList.addView(headerText)
            
            // Add entries (most recent first)
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            entries.sortedByDescending { it.date }.forEach { entry ->
                val entryRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.backgroundGray))
                    setPadding(
                        (12 * resources.displayMetrics.density).toInt(),
                        (8 * resources.displayMetrics.density).toInt(),
                        (12 * resources.displayMetrics.density).toInt(),
                        (8 * resources.displayMetrics.density).toInt()
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (4 * resources.displayMetrics.density).toInt()
                    }
                }
                
                val amountText = TextView(requireContext()).apply {
                    text = "${entry.amount} oz"
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                
                val timeText = TextView(requireContext()).apply {
                    text = timeFormat.format(entry.date)
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                }
                
                val deleteBtn = android.widget.ImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_delete)
                    layoutParams = LinearLayout.LayoutParams(
                        (24 * resources.displayMetrics.density).toInt(),
                        (24 * resources.displayMetrics.density).toInt()
                    ).apply {
                        marginStart = (8 * resources.displayMetrics.density).toInt()
                    }
                    isClickable = true
                    isFocusable = true
                    
                    val attrs = intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                    val ta = requireContext().obtainStyledAttributes(attrs)
                    background = ta.getDrawable(0)
                    ta.recycle()
                    
                    setOnClickListener {
                        viewModel.deleteWaterEntry(entry)
                    }
                }
                
                entryRow.addView(amountText)
                entryRow.addView(timeText)
                entryRow.addView(deleteBtn)
                binding.hydrationEntriesList.addView(entryRow)
            }
        } else {
            binding.hydrationEntriesList.visibility = View.GONE
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        dateDependentJob?.cancel()
        weekDependentJob?.cancel()
        monthDependentJob?.cancel()
        _binding = null
    }
}
