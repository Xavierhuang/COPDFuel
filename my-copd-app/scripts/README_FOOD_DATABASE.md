# Food Database Setup

## Overview

This project includes a comprehensive nutrition database created from Ingenious Medical Solutions data files (Client #001PK00000bwCmi). All Excel data has been successfully imported into a SQLite database for efficient querying in the React Native app.

## Database Statistics

- **Database File:** `assets/foodData.db`
- **Size:** 368.89 MB
- **Total Foods:** 18,563
- **Total Records:** 129,826 rows across 7 tables

## Database Schema

### 1. `foods` (18,563 rows)
Core food information table.

**Columns:**
- `food_id` (INTEGER PRIMARY KEY)
- `keylist` (TEXT) - Food identifier code
- `food_description` (TEXT) - Full food name
- `short_description` (TEXT) - Abbreviated name
- `food_type` (TEXT) - Category

### 2. `densities` (24,304 rows)
Food density measurements for volume-to-weight conversions.

**Columns:**
- `id` (INTEGER PRIMARY KEY)
- `food_id` (INTEGER)
- `form` (TEXT) - e.g., "whole pieces", "chopped"
- `grams_per_cubic_inch` (REAL)
- `refers_to_form` (TEXT)

### 3. `food_specific_units` (27,373 rows)
Food-specific measurement units (e.g., "1 cup", "1 slice").

**Columns:**
- `id` (INTEGER PRIMARY KEY)
- `food_id` (INTEGER)
- `unit_abbreviation` (TEXT)
- `unit_description` (TEXT)
- `grams_per_unit` (REAL)

### 4. `grain_equivalents` (18,563 rows)
Grain serving equivalents per 100 grams.

**Columns:**
- `food_id` (INTEGER PRIMARY KEY)
- `total_grains` (REAL)
- `whole_grains` (REAL)
- `non_whole_grains` (REAL)

### 5. `usda_links` (3,897 rows)
Links to USDA food codes.

**Columns:**
- `food_id` (INTEGER PRIMARY KEY)
- `food_type_reference` (TEXT)
- `usda_code` (TEXT)

### 6. `nutrients_per_100g` (18,563 rows)
Complete nutrition data per 100 grams (185 nutrient columns stored as JSON).

**Columns:**
- `food_id` (INTEGER PRIMARY KEY)
- `data` (TEXT) - JSON object with all nutrient values

### 7. `nutrients_per_portion` (18,563 rows)
Complete nutrition data per common portion size (406 columns stored as JSON).

**Columns:**
- `food_id` (INTEGER PRIMARY KEY)
- `portion_size` (REAL)
- `portion_unit` (TEXT)
- `data` (TEXT) - JSON object with all nutrient values

## Scripts

### `importFoodData.js`
Analyzes all Excel files and creates a JSON summary of their structure.

**Usage:**
```bash
node scripts/importFoodData.js
```

### `createFoodDatabase.js`
Creates the initial database and imports core food data (foods, densities, units, grains, USDA links).

**Usage:**
```bash
node scripts/createFoodDatabase.js
```

### `addNutrientsData.js`
Adds comprehensive nutrient data tables to the database.

**Usage:**
```bash
node scripts/addNutrientsData.js
```

## Source Data Files

The following Excel files were processed (not included in git due to size):

1. **Densities 2025.xlsx** (1.04 MB)
2. **Food Specific Units 2025.xlsx** (1.22 MB)
3. **Grain in Ounce Equivalents Data Per 100 Grams 2025.xlsx** (1.04 MB)
4. **NCC Food Group Serving Count System Data Per 100 Grams 2025.xlsx** (10.04 MB)
5. **NCC Food Links to USDA Codes 2025.xlsx** (0.27 MB)
6. **Nutrients Per 100 Grams 2025.xlsx** (19.49 MB)
7. **Nutrients Per Common Portion Size 2025.xlsx** (25.84 MB)

Total source size: ~58 MB of Excel files → 369 MB SQLite database (includes indices and JSON storage)

## Usage in React Native

### Installing Dependencies

```bash
npm install expo-sqlite
```

### Example: Querying Foods

```typescript
import * as SQLite from 'expo-sqlite';

const db = SQLite.openDatabaseSync('foodData.db');

// Search for foods
const searchFoods = (query: string) => {
  return db.getAllSync(
    'SELECT food_id, food_description, short_description FROM foods WHERE food_description LIKE ? LIMIT 50',
    [`%${query}%`]
  );
};

// Get food details with nutrients
const getFoodWithNutrients = (foodId: number) => {
  const food = db.getFirstSync(
    'SELECT * FROM foods WHERE food_id = ?',
    [foodId]
  );
  
  const nutrients = db.getFirstSync(
    'SELECT * FROM nutrients_per_100g WHERE food_id = ?',
    [foodId]
  );
  
  return {
    ...food,
    nutrients: JSON.parse(nutrients.data)
  };
};

// Get portion size info
const getPortionInfo = (foodId: number) => {
  const portion = db.getFirstSync(
    'SELECT * FROM nutrients_per_portion WHERE food_id = ?',
    [foodId]
  );
  
  return {
    portionSize: portion.portion_size,
    portionUnit: portion.portion_unit,
    nutrients: JSON.parse(portion.data)
  };
};
```

## Notes

- The database file (`assets/foodData.db`) is excluded from git due to its size (369 MB)
- To regenerate the database, run the scripts in order: `createFoodDatabase.js` → `addNutrientsData.js`
- Nutrient data is stored as JSON for flexibility with 185-406 columns
- All queries are indexed on `food_id` and `keylist` for optimal performance

## Data Source

Ingenious Medical Solutions, LLC  
Client #001PK00000bwCmi  
Data Year: 2025

