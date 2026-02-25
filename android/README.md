# COPD Fuel - Native Android Kotlin App

This is a complete native Android application written in Kotlin for tracking COPD health data.

## Project Structure

### Data Layer
- **Models**: `data/model/` - Room entities for FoodEntry, ExerciseEntry, OxygenReading, WeightEntry, Medication
- **DAO**: `data/dao/` - Data Access Objects for database operations
- **Database**: `data/AppDatabase.kt` - Room database configuration
- **Repository**: `repository/DataRepository.kt` - Repository pattern for data access

### UI Layer
- **MainActivity**: Main activity with bottom navigation
- **Fragments**: 
  - HomeFragment - Welcome screen
  - GuidelinesFragment - COPD guidelines
  - TrackingFragment - Track food, exercise, oxygen, weight
  - RecipesFragment - Healthy recipes
  - ResourcesFragment - COPD tools and resources
  - ProfileFragment - User profile information

### ViewModels
- **TrackingViewModel**: Manages tracking data with LiveData/Flow

### Utils
- **AppApplication**: Application class with database and repository initialization

## Features

1. **Food Tracking**: Track meals with calories, protein, carbs, fat
2. **Exercise Tracking**: Log exercise activities
3. **Oxygen Saturation**: Track oxygen levels
4. **Weight Tracking**: Monitor current and goal weight
5. **Medication Management**: Track daily and exacerbation medications
6. **Profile**: Store user information, doctor contacts

## Building

1. Ensure Java 17+ is installed
2. Open project in Android Studio
3. Sync Gradle files
4. Build and run

## Dependencies

- Room Database for local storage
- Material Design Components
- ViewBinding for type-safe view access
- Coroutines for async operations
- Navigation Component (ready for future use)
