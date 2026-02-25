package com.copdhealthtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.copdhealthtracker.data.dao.*
import com.copdhealthtracker.data.model.*

@Database(
    entities = [FoodEntry::class, ExerciseEntry::class, OxygenReading::class, WeightEntry::class, Medication::class, WaterEntry::class, FavoriteFood::class, UserAddedFood::class, FavoriteMeal::class, FavoriteMealItem::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun favoriteFoodDao(): FavoriteFoodDao
    abstract fun userAddedFoodDao(): UserAddedFoodDao
    abstract fun favoriteMealDao(): FavoriteMealDao
    abstract fun favoriteMealItemDao(): FavoriteMealItemDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun oxygenDao(): OxygenDao
    abstract fun weightDao(): WeightDao
    abstract fun medicationDao(): MedicationDao
    abstract fun waterDao(): WaterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 1 to 2 - add new nutrient columns
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns with default values
                database.execSQL("ALTER TABLE food_entries ADD COLUMN fiber REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN sodium REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN potassium REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN calcium REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN iron REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN magnesium REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN zinc REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN vitaminA REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN vitaminC REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN vitaminD REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN vitaminE REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN vitaminK REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN saturatedFat REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN cholesterol REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN omega3 REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN addedSugars REAL NOT NULL DEFAULT 0.0")
            }
        }
        
        // Migration from version 2 to 3 - add selenium, manganese, water columns
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE food_entries ADD COLUMN selenium REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN manganese REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE food_entries ADD COLUMN water REAL NOT NULL DEFAULT 0.0")
            }
        }
        
        // Migration from version 3 to 4 - add water_entries table
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS water_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount INTEGER NOT NULL,
                        date INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorite_foods (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        name TEXT NOT NULL,
                        mealCategory TEXT NOT NULL,
                        quantity TEXT NOT NULL,
                        calories REAL NOT NULL,
                        protein REAL NOT NULL,
                        carbs REAL NOT NULL,
                        fat REAL NOT NULL,
                        fiber REAL NOT NULL DEFAULT 0.0,
                        sodium REAL NOT NULL DEFAULT 0.0,
                        potassium REAL NOT NULL DEFAULT 0.0,
                        calcium REAL NOT NULL DEFAULT 0.0,
                        iron REAL NOT NULL DEFAULT 0.0,
                        magnesium REAL NOT NULL DEFAULT 0.0,
                        zinc REAL NOT NULL DEFAULT 0.0,
                        selenium REAL NOT NULL DEFAULT 0.0,
                        manganese REAL NOT NULL DEFAULT 0.0,
                        water REAL NOT NULL DEFAULT 0.0,
                        vitaminA REAL NOT NULL DEFAULT 0.0,
                        vitaminC REAL NOT NULL DEFAULT 0.0,
                        vitaminD REAL NOT NULL DEFAULT 0.0,
                        vitaminE REAL NOT NULL DEFAULT 0.0,
                        vitaminK REAL NOT NULL DEFAULT 0.0,
                        saturatedFat REAL NOT NULL DEFAULT 0.0,
                        cholesterol REAL NOT NULL DEFAULT 0.0,
                        omega3 REAL NOT NULL DEFAULT 0.0,
                        addedSugars REAL NOT NULL DEFAULT 0.0
                    )
                """)
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_added_foods (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        shortName TEXT NOT NULL,
                        category TEXT NOT NULL,
                        categoryGroup TEXT NOT NULL,
                        calories REAL NOT NULL,
                        protein REAL NOT NULL,
                        carbs REAL NOT NULL,
                        fat REAL NOT NULL,
                        fiber REAL NOT NULL DEFAULT 0.0,
                        sodium REAL NOT NULL DEFAULT 0.0,
                        potassium REAL NOT NULL DEFAULT 0.0,
                        calcium REAL NOT NULL DEFAULT 0.0,
                        iron REAL NOT NULL DEFAULT 0.0,
                        magnesium REAL NOT NULL DEFAULT 0.0,
                        zinc REAL NOT NULL DEFAULT 0.0,
                        selenium REAL NOT NULL DEFAULT 0.0,
                        manganese REAL NOT NULL DEFAULT 0.0,
                        water REAL NOT NULL DEFAULT 0.0,
                        vitaminA REAL NOT NULL DEFAULT 0.0,
                        vitaminC REAL NOT NULL DEFAULT 0.0,
                        vitaminD REAL NOT NULL DEFAULT 0.0,
                        vitaminE REAL NOT NULL DEFAULT 0.0,
                        vitaminK REAL NOT NULL DEFAULT 0.0,
                        saturatedFat REAL NOT NULL DEFAULT 0.0,
                        cholesterol REAL NOT NULL DEFAULT 0.0,
                        omega3 REAL NOT NULL DEFAULT 0.0,
                        addedSugars REAL NOT NULL DEFAULT 0.0,
                        portionSize REAL NOT NULL DEFAULT 100.0,
                        portionUnit TEXT NOT NULL DEFAULT 'g',
                        portionDesc TEXT NOT NULL DEFAULT '100g'
                    )
                """)
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorite_meals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        mealCategory TEXT NOT NULL
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorite_meal_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mealId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        quantity TEXT NOT NULL,
                        calories REAL NOT NULL,
                        protein REAL NOT NULL,
                        carbs REAL NOT NULL,
                        fat REAL NOT NULL,
                        fiber REAL NOT NULL DEFAULT 0.0,
                        sodium REAL NOT NULL DEFAULT 0.0,
                        potassium REAL NOT NULL DEFAULT 0.0,
                        calcium REAL NOT NULL DEFAULT 0.0,
                        iron REAL NOT NULL DEFAULT 0.0,
                        magnesium REAL NOT NULL DEFAULT 0.0,
                        zinc REAL NOT NULL DEFAULT 0.0,
                        selenium REAL NOT NULL DEFAULT 0.0,
                        manganese REAL NOT NULL DEFAULT 0.0,
                        water REAL NOT NULL DEFAULT 0.0,
                        vitaminA REAL NOT NULL DEFAULT 0.0,
                        vitaminC REAL NOT NULL DEFAULT 0.0,
                        vitaminD REAL NOT NULL DEFAULT 0.0,
                        vitaminE REAL NOT NULL DEFAULT 0.0,
                        vitaminK REAL NOT NULL DEFAULT 0.0,
                        saturatedFat REAL NOT NULL DEFAULT 0.0,
                        cholesterol REAL NOT NULL DEFAULT 0.0,
                        omega3 REAL NOT NULL DEFAULT 0.0,
                        addedSugars REAL NOT NULL DEFAULT 0.0,
                        FOREIGN KEY(mealId) REFERENCES favorite_meals(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_meal_items_mealId ON favorite_meal_items(mealId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "copd_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
