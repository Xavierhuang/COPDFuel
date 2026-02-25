const XLSX = require('xlsx');
const fs = require('fs');
const path = require('path');

// Path to the combined Excel file (fooddata.xlsx)
const excelPath = '/Users/weijiahuang/Downloads/COPD/fooddata.xlsx';
const outputPath = '/Users/weijiahuang/Downloads/COPD/android/app/src/main/assets/food_database.json';

console.log('Creating food database JSON with ALL data...');
console.log('Source:', excelPath);
console.log('Output:', outputPath);

// Read Excel file
console.log('\nReading Excel file...');
const workbook = XLSX.readFile(excelPath);
const worksheet = workbook.Sheets[workbook.SheetNames[0]];
const data = XLSX.utils.sheet_to_json(worksheet, { defval: null });

console.log(`Found ${data.length.toLocaleString()} foods`);

// Volume conversion constants (cubic inches)
const CUP_CUBIC_INCHES = 14.4375;  // 1 cup = 14.4375 cubic inches
const TBSP_CUBIC_INCHES = 0.9023;  // 1 tbsp = 0.9023 cubic inches
const TSP_CUBIC_INCHES = 0.3008;   // 1 tsp = 0.3008 cubic inches
const OZ_GRAMS = 28.3495;          // 1 oz = 28.3495 grams

// Extract unique categories and food groups
const categoriesSet = new Set();
const foodGroupsSet = new Set();

// Process foods
const foods = data.map((row, index) => {
  const categoryGroup = row['NCC Food Group Category'] || '';
  const category = row['NCC Food Group'] || '';
  
  if (categoryGroup) categoriesSet.add(categoryGroup);
  if (category) foodGroupsSet.add(category);
  
  // Build serving sizes array
  const servingSizes = [];
  
  // 1. Common Portion Size (primary serving)
  const commonPortionAmount = row['Common Portion Size Amount'];
  const commonPortionUnit = row['Common Portion Size Unit'];
  const commonPortionDesc = row['Common Portion Size Description'];
  const commonPortionGrams = row['Common Portion Size Gram Weight'];
  
  if (commonPortionGrams && commonPortionGrams > 0) {
    const label = commonPortionDesc || 
      (commonPortionAmount && commonPortionUnit ? `${commonPortionAmount} ${commonPortionUnit}` : '1 serving');
    
    servingSizes.push({
      label: label,
      grams: parseFloat(commonPortionGrams.toFixed(2)),
      amount: commonPortionAmount || 1,
      unit: commonPortionUnit || 'serving',
      isPrimary: true
    });
  }
  
  // 2. Food Specific Unit (secondary serving)
  const specificUnitAbbr = row['Food Specific Unit Abbreviation'];
  const specificUnitDesc = row['Food Specific Unit Description'];
  const specificUnitGrams = row['Grams Per Food Specific Unit'];
  
  if (specificUnitGrams && specificUnitGrams > 0 && specificUnitDesc) {
    // Only add if different from common portion
    const isDifferent = !commonPortionDesc || 
      (Math.abs(specificUnitGrams - commonPortionGrams) > 0.5);
    
    if (isDifferent) {
      servingSizes.push({
        label: specificUnitDesc,
        grams: parseFloat(specificUnitGrams.toFixed(2)),
        amount: 1,
        unit: specificUnitAbbr || 'unit'
      });
    }
  }
  
  // 3. Volume-based servings (if density data available)
  const gramsPerCubicInch = row['Grams Per Cubic Inch'];
  if (gramsPerCubicInch && gramsPerCubicInch > 0) {
    // Cup
    const gramsPerCup = gramsPerCubicInch * CUP_CUBIC_INCHES;
    servingSizes.push({
      label: `cup - ${Math.round(gramsPerCup)}g`,
      grams: parseFloat(gramsPerCup.toFixed(2)),
      amount: 1,
      unit: 'cup'
    });
    
    // Tablespoon
    const gramsPerTbsp = gramsPerCubicInch * TBSP_CUBIC_INCHES;
    servingSizes.push({
      label: `tbsp - ${Math.round(gramsPerTbsp)}g`,
      grams: parseFloat(gramsPerTbsp.toFixed(2)),
      amount: 1,
      unit: 'tbsp'
    });
    
    // Teaspoon
    const gramsPerTsp = gramsPerCubicInch * TSP_CUBIC_INCHES;
    servingSizes.push({
      label: `tsp - ${Math.round(gramsPerTsp)}g`,
      grams: parseFloat(gramsPerTsp.toFixed(2)),
      amount: 1,
      unit: 'tsp'
    });
  }
  
  // 4. Ounce
  servingSizes.push({
    label: `oz - ${Math.round(OZ_GRAMS)}g`,
    grams: OZ_GRAMS,
    amount: 1,
    unit: 'oz'
  });
  
  // 5. 100g standard
  servingSizes.push({
    label: '100g',
    grams: 100,
    amount: 100,
    unit: 'g'
  });
  
  // 6. Custom grams
  servingSizes.push({
    label: 'g',
    grams: 1,
    amount: 1,
    unit: 'g',
    isCustom: true
  });
  
  // Get the primary serving size for default display
  const primaryServing = servingSizes.find(s => s.isPrimary) || servingSizes[0];
  
  return {
    id: row['Food ID'] || index,
    name: row['Food Description'] || '',
    shortName: row['Short Food Description'] || '',
    keylist: row['Keylist'] || '',
    foodType: row['Food Type'] || '',
    category: category,
    categoryGroup: categoryGroup,
    
    // ALL Nutrition per 100g
    water: parseFloat((row['Water (g)'] || 0).toFixed(2)),
    calories: parseFloat((row['Energy (kcal)'] || 0).toFixed(1)),
    caloriesKj: parseFloat((row['Energy (kj)'] || 0).toFixed(1)),
    protein: parseFloat((row['Total Protein (g)'] || 0).toFixed(2)),
    fat: parseFloat((row['Total Fat (g)'] || 0).toFixed(2)),
    carbs: parseFloat((row['Total Carbohydrate (g)'] || 0).toFixed(2)),
    fiber: parseFloat((row['Total Dietary Fiber (g)'] || 0).toFixed(2)),
    
    // Minerals
    calcium: parseFloat((row['Calcium (mg)'] || 0).toFixed(2)),
    iron: parseFloat((row['Iron (mg)'] || 0).toFixed(2)),
    magnesium: parseFloat((row['Magnesium (mg)'] || 0).toFixed(2)),
    potassium: parseFloat((row['Potassium (mg)'] || 0).toFixed(2)),
    sodium: parseFloat((row['Sodium (mg)'] || 0).toFixed(1)),
    zinc: parseFloat((row['Zinc (mg)'] || 0).toFixed(2)),
    manganese: parseFloat((row['Manganese (mg)'] || 0).toFixed(3)),
    selenium: parseFloat((row['Selenium (mcg)'] || 0).toFixed(2)),
    
    // Vitamins
    vitaminC: parseFloat((row['Vitamin C (ascorbic acid) (mg)'] || 0).toFixed(2)),
    vitaminA: parseFloat((row['Total Vitamin A Activity (International Units) (IU)'] || 0).toFixed(2)),
    vitaminD: parseFloat((row['Vitamin D (calciferol) (mcg)'] || 0).toFixed(3)),
    vitaminD2: parseFloat((row['Vitamin D2 (ergocalciferol) (mcg)'] || 0).toFixed(3)),
    vitaminD3: parseFloat((row['Vitamin D3 (cholecalciferol) (mcg)'] || 0).toFixed(3)),
    vitaminE: parseFloat((row['Vitamin E (International Units) (IU)'] || 0).toFixed(3)),
    vitaminK: parseFloat((row['Vitamin K (phylloquinone) (mcg)'] || 0).toFixed(3)),
    
    // Fatty Acids
    saturatedFat: parseFloat((row['Total Saturated Fatty Acids (SFA) (g)'] || 0).toFixed(3)),
    monounsaturatedFat: parseFloat((row['Total Monounsaturated Fatty Acids (MUFA) (g)'] || 0).toFixed(3)),
    polyunsaturatedFat: parseFloat((row['Total Polyunsaturated Fatty Acids (PUFA) (g)'] || 0).toFixed(3)),
    omega3: parseFloat((row['Omega-3 Fatty Acids (g)'] || 0).toFixed(3)),
    cholesterol: parseFloat((row['Cholesterol (mg)'] || 0).toFixed(2)),
    
    // Sugars
    addedSugars: parseFloat((row['Added Sugars (by Total Sugars) (g)'] || 0).toFixed(2)),
    
    // Primary serving info (for backward compatibility)
    portionSize: primaryServing.grams,
    portionUnit: primaryServing.unit,
    portionDesc: primaryServing.label,
    portionAmount: commonPortionAmount || 1,
    
    // Form and density for calculations
    form: row['Form'] || '',
    gramsPerCubicInch: gramsPerCubicInch || null,
    refersToForm: row['Refers to Form'] || '',
    
    // Food type reference
    foodTypeRef: row['Food Type Reference'] || '',
    foodTypeRefDesc: row['Food Type Reference Description'] || '',
    
    // All serving sizes
    servingSizes: servingSizes
  };
});

// Build final JSON
const database = {
  totalFoods: foods.length,
  categories: Array.from(categoriesSet).sort(),
  foodGroups: Array.from(foodGroupsSet).sort(),
  foods: foods
};

// Write to file
console.log('\nWriting JSON file...');
fs.writeFileSync(outputPath, JSON.stringify(database));

// Get file size
const stats = fs.statSync(outputPath);
const sizeMB = (stats.size / (1024 * 1024)).toFixed(2);

console.log(`\nDatabase created successfully!`);
console.log(`  Total foods: ${foods.length.toLocaleString()}`);
console.log(`  Categories: ${database.categories.length}`);
console.log(`  Food groups: ${database.foodGroups.length}`);
console.log(`  File size: ${sizeMB} MB`);
console.log(`  Output: ${outputPath}`);

// Show sample food with all data
console.log('\nSample food (scrambled egg):');
const sampleFood = foods.find(f => f.name.toLowerCase().includes('scrambled egg, plain')) || foods[6317];
console.log(`  Name: ${sampleFood.name}`);
console.log(`  Calories: ${sampleFood.calories} kcal`);
console.log(`  Protein: ${sampleFood.protein}g`);
console.log(`  Fat: ${sampleFood.fat}g`);
console.log(`  Carbs: ${sampleFood.carbs}g`);
console.log(`  Fiber: ${sampleFood.fiber}g`);
console.log(`  Sodium: ${sampleFood.sodium}mg`);
console.log(`  Cholesterol: ${sampleFood.cholesterol}mg`);
console.log(`  Vitamin A: ${sampleFood.vitaminA} IU`);
console.log(`  Vitamin C: ${sampleFood.vitaminC}mg`);
console.log(`  Calcium: ${sampleFood.calcium}mg`);
console.log(`  Iron: ${sampleFood.iron}mg`);
console.log(`  Serving sizes: ${sampleFood.servingSizes.length}`);
