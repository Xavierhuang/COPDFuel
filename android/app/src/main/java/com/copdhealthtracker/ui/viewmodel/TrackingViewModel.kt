package com.copdhealthtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copdhealthtracker.data.model.*
import com.copdhealthtracker.repository.DataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import com.copdhealthtracker.data.model.Medication

class TrackingViewModel(private val repository: DataRepository) : ViewModel() {
    
    fun getAllFoods(): Flow<List<FoodEntry>> = repository.getAllFoods()
    
    fun getTodayFoods(): Flow<List<FoodEntry>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return repository.getFoodsByDateRange(startOfDay, endOfDay)
    }
    
    fun getFoodsForDate(dateMillis: Long): Flow<List<FoodEntry>> {
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        return repository.getFoodsByDateRange(startOfDay, endOfDay)
    }
    
    fun getDailyMedications(): Flow<List<Medication>> = repository.getMedicationsByType("daily")
    fun getExacerbationMedications(): Flow<List<Medication>> = repository.getMedicationsByType("exacerbation")
    
    fun insertMedication(medication: Medication) {
        viewModelScope.launch {
            repository.insertMedication(medication)
        }
    }
    
    fun getTodayExercises(): Flow<List<ExerciseEntry>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return repository.getExercisesByDateRange(startOfDay, endOfDay)
    }
    
    fun getExercisesForDate(dateMillis: Long): Flow<List<ExerciseEntry>> {
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        return repository.getExercisesByDateRange(startOfDay, endOfDay)
    }
    
    fun insertFood(food: FoodEntry) {
        viewModelScope.launch {
            repository.insertFood(food)
        }
    }
    
    fun deleteFood(food: FoodEntry) {
        viewModelScope.launch {
            repository.deleteFood(food)
        }
    }
    
    fun deleteFoodById(id: Long) {
        viewModelScope.launch {
            repository.deleteFoodById(id)
        }
    }
    
    fun getFavoriteFoods(): Flow<List<FavoriteFood>> = repository.getAllFavoriteFoods()
    
    suspend fun insertFavoriteFood(favorite: FavoriteFood): Boolean = repository.insertFavoriteFood(favorite)
    
    fun deleteFavoriteFood(favorite: FavoriteFood) {
        viewModelScope.launch {
            repository.deleteFavoriteFood(favorite)
        }
    }

    suspend fun getAllUserAddedFoods(): List<UserAddedFood> = repository.getAllUserAddedFoods()

    suspend fun insertUserAddedFood(food: UserAddedFood): Boolean = repository.insertUserAddedFood(food)

    fun getFavoriteMeals(): Flow<List<FavoriteMeal>> = repository.getAllFavoriteMeals()

    suspend fun getFavoriteMealWithItems(mealId: Long): Pair<FavoriteMeal, List<FavoriteMealItem>>? =
        repository.getFavoriteMealWithItems(mealId)

    suspend fun insertFavoriteMeal(meal: FavoriteMeal, items: List<FavoriteMealItem>): Boolean =
        repository.insertFavoriteMeal(meal, items)

    fun deleteFavoriteMeal(meal: FavoriteMeal) {
        viewModelScope.launch {
            repository.deleteFavoriteMeal(meal)
        }
    }
    
    fun insertExercise(exercise: ExerciseEntry) {
        viewModelScope.launch {
            repository.insertExercise(exercise)
        }
    }
    
    fun insertOxygenReading(reading: OxygenReading) {
        viewModelScope.launch {
            repository.insertReading(reading)
        }
    }
    
    fun insertWeight(weight: WeightEntry) {
        viewModelScope.launch {
            repository.insertWeight(weight)
        }
    }
    
    fun getOxygenReadingsForDate(dateMillis: Long): Flow<List<OxygenReading>> {
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        return repository.getOxygenReadingsByDateRange(startOfDay, endOfDay)
    }
    
    fun getWeightsForDate(dateMillis: Long): Flow<List<WeightEntry>> {
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        return repository.getWeightsByDateRange(startOfDay, endOfDay)
    }

    /** Latest goal weight (any date) - so goal always shows in day view. */
    fun getLatestGoalWeight(): Flow<WeightEntry?> = repository.getGoalWeight()

    /** Latest current weight (any date) - so last entry always shows in day view. */
    fun getLatestCurrentWeight(): Flow<WeightEntry?> = repository.getCurrentWeight()
    
    // Weekly data methods
    fun getFoodsForWeek(weekStartMillis: Long): Flow<List<FoodEntry>> {
        val calendar = Calendar.getInstance().apply { timeInMillis = weekStartMillis }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 7)
        val endOfWeek = calendar.timeInMillis
        return repository.getFoodsByDateRange(startOfWeek, endOfWeek)
    }
    
    fun getExercisesForWeek(weekStartMillis: Long): Flow<List<ExerciseEntry>> {
        val calendar = Calendar.getInstance().apply { timeInMillis = weekStartMillis }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 7)
        val endOfWeek = calendar.timeInMillis
        return repository.getExercisesByDateRange(startOfWeek, endOfWeek)
    }
    
    fun getOxygenReadingsForWeek(weekStartMillis: Long): Flow<List<OxygenReading>> {
        val calendar = Calendar.getInstance().apply { timeInMillis = weekStartMillis }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 7)
        val endOfWeek = calendar.timeInMillis
        return repository.getOxygenReadingsByDateRange(startOfWeek, endOfWeek)
    }
    
    fun getWeightsForWeek(weekStartMillis: Long): Flow<List<WeightEntry>> {
        val calendar = Calendar.getInstance().apply { timeInMillis = weekStartMillis }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 7)
        val endOfWeek = calendar.timeInMillis
        return repository.getWeightsByDateRange(startOfWeek, endOfWeek)
    }
    
    // Monthly data methods
    fun getFoodsForMonth(year: Int, month: Int): Flow<List<FoodEntry>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis
        return repository.getFoodsByDateRange(startOfMonth, endOfMonth)
    }
    
    fun getExercisesForMonth(year: Int, month: Int): Flow<List<ExerciseEntry>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis
        return repository.getExercisesByDateRange(startOfMonth, endOfMonth)
    }
    
    fun getOxygenReadingsForMonth(year: Int, month: Int): Flow<List<OxygenReading>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis
        return repository.getOxygenReadingsByDateRange(startOfMonth, endOfMonth)
    }
    
    fun getWeightsForMonth(year: Int, month: Int): Flow<List<WeightEntry>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis
        return repository.getWeightsByDateRange(startOfMonth, endOfMonth)
    }
    
    // Water tracking methods
    fun getWaterEntriesForDate(dateMillis: Long): Flow<List<WaterEntry>> {
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        return repository.getWaterEntriesByDateRange(startOfDay, endOfDay)
    }
    
    fun insertWaterEntry(entry: WaterEntry) {
        viewModelScope.launch {
            repository.insertWaterEntry(entry)
        }
    }
    
    fun deleteWaterEntry(entry: WaterEntry) {
        viewModelScope.launch {
            repository.deleteWaterEntry(entry)
        }
    }
}
