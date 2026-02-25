const XLSX = require('xlsx');
const fs = require('fs');
const path = require('path');
const sqlite3 = require('better-sqlite3');

const dataDir = '/Users/weijiahuang/Downloads/Ingenious Medical Solutions, LLC, Client #001PK00000bwCmi';
const dbPath = path.join(__dirname, '../assets/foodData.db');

console.log('🚀 Adding Nutrients Data to Database...');

const db = sqlite3(dbPath);

// Create nutrients tables with dynamic columns
console.log('\n📋 Creating nutrients tables...');

// First, analyze the nutrients files to get column names
const nutrientsPerFile = path.join(dataDir, 'Ingenious Medical Solutions LLC #001PK00000bwCmi_Nutrients Per 100 Grams 2025.xlsx');
const nutrientsPortionFile = path.join(dataDir, 'Ingenious Medical Solutions LLC #001PK00000bwCmi_Nutrients Per Common Portion Size 2025.xlsx');

console.log('📊 Analyzing Nutrients Per 100 Grams...');
const workbook100 = XLSX.readFile(nutrientsPerFile);
const sheet100 = workbook100.Sheets[workbook100.SheetNames[0]];
const data100 = XLSX.utils.sheet_to_json(sheet100, { defval: null, limit: 1 });

console.log('📊 Analyzing Nutrients Per Portion...');
const workbookPortion = XLSX.readFile(nutrientsPortionFile);
const sheetPortion = workbookPortion.Sheets[workbookPortion.SheetNames[0]];
const dataPortion = XLSX.utils.sheet_to_json(sheetPortion, { defval: null, limit: 1 });

console.log(`   185 columns in Nutrients Per 100g`);
console.log(`   406 columns in Nutrients Per Portion`);

// Create table for nutrients per 100g
db.exec(`DROP TABLE IF EXISTS nutrients_per_100g`);
db.exec(`DROP TABLE IF EXISTS nutrients_per_portion`);

// We'll store all nutrient data as JSON for flexibility
db.exec(`
  CREATE TABLE nutrients_per_100g (
    food_id INTEGER PRIMARY KEY,
    data TEXT NOT NULL,
    FOREIGN KEY (food_id) REFERENCES foods(food_id)
  )
`);

db.exec(`
  CREATE TABLE nutrients_per_portion (
    food_id INTEGER PRIMARY KEY,
    portion_size REAL,
    portion_unit TEXT,
    data TEXT NOT NULL,
    FOREIGN KEY (food_id) REFERENCES foods(food_id)
  )
`);

console.log('✅ Tables created');

// Import nutrients per 100g
console.log('\n📥 Importing Nutrients Per 100 Grams...');
const fullData100 = XLSX.utils.sheet_to_json(sheet100, { defval: null });

const insert100 = db.prepare(`
  INSERT OR REPLACE INTO nutrients_per_100g (food_id, data) VALUES (?, ?)
`);

const insertMany100 = db.transaction((rows) => {
  let count = 0;
  for (const row of rows) {
    const foodId = row['Food ID'];
    // Remove metadata columns and store rest as JSON
    const { 'Food ID': _, 'Keylist': __, 'Food Description': ___, 'Short Food Description': ____, 'Food Type': _____, ...nutrients } = row;
    insert100.run(foodId, JSON.stringify(nutrients));
    count++;
    if (count % 1000 === 0) {
      process.stdout.write(`\r   Progress: ${count.toLocaleString()} / ${rows.length.toLocaleString()}`);
    }
  }
  process.stdout.write(`\r   ✅ Imported ${count.toLocaleString()} rows\n`);
});

insertMany100(fullData100);

// Import nutrients per portion
console.log('\n📥 Importing Nutrients Per Common Portion Size...');
const fullDataPortion = XLSX.utils.sheet_to_json(sheetPortion, { defval: null });

const insertPortion = db.prepare(`
  INSERT OR REPLACE INTO nutrients_per_portion (food_id, portion_size, portion_unit, data) VALUES (?, ?, ?, ?)
`);

const insertManyPortion = db.transaction((rows) => {
  let count = 0;
  for (const row of rows) {
    const foodId = row['Food ID'];
    const portionSize = row['Common Portion Size Amount'];
    const portionUnit = row['Common Portion Size Unit'];
    
    // Remove metadata columns and store rest as JSON
    const { 
      'Food ID': _, 
      'Keylist': __, 
      'Food Description': ___, 
      'Short Food Description': ____, 
      'Common Portion Size Amount': _____,
      'Common Portion Size Unit': ______,
      ...nutrients 
    } = row;
    
    insertPortion.run(foodId, portionSize, portionUnit, JSON.stringify(nutrients));
    count++;
    if (count % 1000 === 0) {
      process.stdout.write(`\r   Progress: ${count.toLocaleString()} / ${rows.length.toLocaleString()}`);
    }
  }
  process.stdout.write(`\r   ✅ Imported ${count.toLocaleString()} rows\n`);
});

insertManyPortion(fullDataPortion);

// Get database stats
console.log('\n' + '='.repeat(80));
console.log('UPDATED DATABASE STATISTICS');
console.log('='.repeat(80));

const tables = ['foods', 'densities', 'food_specific_units', 'grain_equivalents', 'usda_links', 'nutrients_per_100g', 'nutrients_per_portion'];
tables.forEach(table => {
  const result = db.prepare(`SELECT COUNT(*) as count FROM ${table}`).get();
  console.log(`  ${table}: ${result.count.toLocaleString()} rows`);
});

// Get database size
const stats = fs.statSync(dbPath);
console.log(`\n  Database size: ${(stats.size / (1024 * 1024)).toFixed(2)} MB`);

db.close();

console.log('\n🎉 Nutrients data added successfully!');
console.log(`💾 Final database: ${dbPath}`);

