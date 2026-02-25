package com.copdhealthtracker.repository

import com.copdhealthtracker.data.AppDatabase
import com.copdhealthtracker.data.model.*
import kotlinx.coroutines.flow.Flow

class DataRepository(private val database: AppDatabase) {
    
    // Food operations
    fun getAllFoods(): Flow<List<FoodEntry>> = database.foodDao().getAllFoods()
    
    fun getFoodsByDateRange(startDate: Long, endDate: Long): Flow<List<FoodEntry>> =
        database.foodDao().getFoodsByDateRange(startDate, endDate)
    
    fun getFoodsByCategory(category: String, startDate: Long, endDate: Long): Flow<List<FoodEntry>> =
        database.foodDao().getFoodsByCategory(category, startDate, endDate)
    
    suspend fun insertFood(food: FoodEntry): Long = database.foodDao().insertFood(food)
    
    suspend fun deleteFood(food: FoodEntry) = database.foodDao().deleteFood(food)
    
    suspend fun deleteFoodById(id: Long) = database.foodDao().deleteFoodById(id)
    
    fun getAllFavoriteFoods(): Flow<List<FavoriteFood>> = database.favoriteFoodDao().getAllFavorites()
    
    suspend fun insertFavoriteFood(favorite: FavoriteFood): Boolean {
        if (database.favoriteFoodDao().getByLabel(favorite.label) != null) return false
        database.favoriteFoodDao().insertFavorite(favorite)
        return true
    }
    
    suspend fun deleteFavoriteFood(favorite: FavoriteFood) = database.favoriteFoodDao().deleteFavorite(favorite)

    suspend fun getAllUserAddedFoods(): List<UserAddedFood> = database.userAddedFoodDao().getAll()

    suspend fun insertUserAddedFood(food: UserAddedFood): Boolean {
        if (database.userAddedFoodDao().getByName(food.name) != null) return false
        database.userAddedFoodDao().insert(food)
        return true
    }

    fun getAllFavoriteMeals(): Flow<List<FavoriteMeal>> = database.favoriteMealDao().getAllMeals()

    suspend fun getFavoriteMealWithItems(mealId: Long): Pair<FavoriteMeal, List<FavoriteMealItem>>? {
        val meal = database.favoriteMealDao().getById(mealId) ?: return null
        val items = database.favoriteMealItemDao().getByMealId(mealId)
        return Pair(meal, items)
    }

    suspend fun insertFavoriteMeal(meal: FavoriteMeal, items: List<FavoriteMealItem>): Boolean {
        if (database.favoriteMealDao().getByLabel(meal.label) != null) return false
        val mealId = database.favoriteMealDao().insert(meal)
        val itemsWithMealId = items.map { it.copy(mealId = mealId) }
        database.favoriteMealItemDao().insertAll(itemsWithMealId)
        return true
    }

    suspend fun deleteFavoriteMeal(meal: FavoriteMeal) = database.favoriteMealDao().deleteById(meal.id)

    // Exercise operations
    fun getAllExercises(): Flow<List<ExerciseEntry>> = database.exerciseDao().getAllExercises()
    
    fun getExercisesByDateRange(startDate: Long, endDate: Long): Flow<List<ExerciseEntry>> =
        database.exerciseDao().getExercisesByDateRange(startDate, endDate)
    
    suspend fun insertExercise(exercise: ExerciseEntry): Long = database.exerciseDao().insertExercise(exercise)
    
    suspend fun deleteExercise(exercise: ExerciseEntry) = database.exerciseDao().deleteExercise(exercise)
    
    // Oxygen operations
    fun getAllReadings(): Flow<List<OxygenReading>> = database.oxygenDao().getAllReadings()
    
    fun getReadingsByDateRange(startDate: Long, endDate: Long): Flow<List<OxygenReading>> =
        database.oxygenDao().getReadingsByDateRange(startDate, endDate)
    
    fun getOxygenReadingsByDateRange(startDate: Long, endDate: Long): Flow<List<OxygenReading>> =
        database.oxygenDao().getReadingsByDateRange(startDate, endDate)
    
    suspend fun insertReading(reading: OxygenReading): Long = database.oxygenDao().insertReading(reading)
    
    suspend fun deleteReading(reading: OxygenReading) = database.oxygenDao().deleteReading(reading)
    
    // Weight operations
    fun getAllWeights(): Flow<List<WeightEntry>> = database.weightDao().getAllWeights()
    
    fun getCurrentWeight(): Flow<WeightEntry?> = database.weightDao().getCurrentWeight()
    
    fun getGoalWeight(): Flow<WeightEntry?> = database.weightDao().getGoalWeight()
    
    fun getWeightsByDateRange(startDate: Long, endDate: Long): Flow<List<WeightEntry>> =
        database.weightDao().getWeightsByDateRange(startDate, endDate)
    
    suspend fun insertWeight(weight: WeightEntry): Long = database.weightDao().insertWeight(weight)
    
    suspend fun deleteWeight(weight: WeightEntry) = database.weightDao().deleteWeight(weight)
    
    // Medication operations
    fun getAllMedications(): Flow<List<Medication>> = database.medicationDao().getAllMedications()
    
    fun getMedicationsByType(type: String): Flow<List<Medication>> =
        database.medicationDao().getMedicationsByType(type)
    
    suspend fun insertMedication(medication: Medication): Long = database.medicationDao().insertMedication(medication)
    
    suspend fun deleteMedication(medication: Medication) = database.medicationDao().deleteMedication(medication)
    
    suspend fun deleteMedicationById(id: Long) = database.medicationDao().deleteMedicationById(id)
    
    // Water operations
    fun getAllWaterEntries(): Flow<List<WaterEntry>> = database.waterDao().getAllWaterEntries()
    
    fun getWaterEntriesByDateRange(startDate: Long, endDate: Long): Flow<List<WaterEntry>> =
        database.waterDao().getWaterEntriesByDateRange(startDate, endDate)
    
    suspend fun insertWaterEntry(entry: WaterEntry): Long = database.waterDao().insertWaterEntry(entry)
    
    suspend fun deleteWaterEntry(entry: WaterEntry) = database.waterDao().deleteWaterEntry(entry)
    
    suspend fun deleteWaterEntryById(id: Long) = database.waterDao().deleteWaterEntryById(id)
}
