const XLSX = require('xlsx');
const fs = require('fs');
const path = require('path');
const sqlite3 = require('better-sqlite3');

const dataDir = '/Users/weijiahuang/Downloads/Ingenious Medical Solutions, LLC, Client #001PK00000bwCmi';
const dbPath = path.join(__dirname, '../assets/foodData.db');

console.log('🚀 Creating Food Database...');
console.log('📂 Source:', dataDir);
console.log('💾 Database:', dbPath);

// Create database
const db = sqlite3(dbPath);

// Create tables
console.log('\n📋 Creating tables...');

db.exec(`
  CREATE TABLE IF NOT EXISTS foods (
    food_id INTEGER PRIMARY KEY,
    keylist TEXT,
    food_description TEXT,
    short_description TEXT,
    food_type TEXT
  );
  
  CREATE INDEX IF NOT EXISTS idx_foods_keylist ON foods(keylist);
  CREATE INDEX IF NOT EXISTS idx_foods_description ON foods(food_description);

  CREATE TABLE IF NOT EXISTS densities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    food_id INTEGER,
    form TEXT,
    grams_per_cubic_inch REAL,
    refers_to_form TEXT,
    FOREIGN KEY (food_id) REFERENCES foods(food_id)
  );

  CREATE TABLE IF NOT EXISTS food_specific_units (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    food_id INTEGER,
    unit_abbreviation TEXT,
    unit_description TEXT,
    grams_per_unit REAL,
    FOREIGN KEY (food_id) REFERENCES foods(food_id)
  );

  CREATE TABLE IF NOT EXISTS grain_equivalents (
    food_id INTEGER PRIMARY KEY,
    total_grains REAL,
    whole_grains REAL,
    non_whole_grains REAL,
    FOREIGN KEY (food_id) REFERENCES foods(food_id)
  );

  CREATE TABLE IF NOT EXISTS usda_links (
    food_id INTEGER PRIMARY KEY,
    food_type_reference TEXT,
    usda_code TEXT,
    FOREIGN KEY (food_id) REFERENCES foods(food_id)
  );
`);

console.log('✅ Tables created');

// Function to import data
function importFile(fileName, tableName, columnMapping) {
  console.log(`\n📥 Importing ${fileName}...`);
  
  const filePath = path.join(dataDir, fileName);
  const workbook = XLSX.readFile(filePath);
  const worksheet = workbook.Sheets[workbook.SheetNames[0]];
  const data = XLSX.utils.sheet_to_json(worksheet, { defval: null });
  
  if (data.length === 0) {
    console.log('   ⚠️  No data found');
    return;
  }
  
  const columns = Object.keys(columnMapping);
  const placeholders = columns.map(() => '?').join(', ');
  const sql = `INSERT OR REPLACE INTO ${tableName} (${columns.join(', ')}) VALUES (${placeholders})`;
  
  const insert = db.prepare(sql);
  
  const insertMany = db.transaction((rows) => {
    for (const row of rows) {
      const values = columns.map(col => {
        const excelCol = columnMapping[col];
        return row[excelCol];
      });
      insert.run(values);
    }
  });
  
  try {
    insertMany(data);
    console.log(`   ✅ Imported ${data.length.toLocaleString()} rows`);
  } catch (error) {
    console.log(`   ❌ Error: ${error.message}`);
  }
}

// Import core foods data first
console.log('\n' + '='.repeat(80));
console.log('IMPORTING FOOD DATA');
console.log('='.repeat(80));

importFile(
  'Ingenious Medical Solutions LLC #001PK00000bwCmi_Nutrients Per 100 Grams 2025.xlsx',
  'foods',
  {
    food_id: 'Food ID',
    keylist: 'Keylist',
    food_description: 'Food Description',
    short_description: 'Short Food Description',
    food_type: 'Food Type'
  }
);

// Import densities
importFile(
  'Ingenious Medical Solutions LLC #001PK00000bwCmi_Densities 2025.xlsx',
  'densities',
  {
    food_id: 'Food ID',
    form: 'Form',
    grams_per_cubic_inch: 'Grams Per Cubic Inch',
    refers_to_form: 'Refers to Form'
  }
);

// Import food specific units
importFile(
  'Ingenious Medical Solutions LLC #001PK00000bwCmi_Food Specific Units 2025.xlsx',
  'food_specific_units',
  {
    food_id: 'Food ID',
    unit_abbreviation: 'Food Specific Unit Abbreviation',
    unit_description: 'Food Specific Unit Description',
    grams_per_unit: 'Grams Per Food Specific Unit'
  }
);

// Import grain equivalents
importFile(
  'Ingenious Medical Solutions LLC #001PK00000bwCmi_Grain in Ounce Equivalents Data Per 100 Grams 2025.xlsx',
  'grain_equivalents',
  {
    food_id: 'Food ID',
    total_grains: 'Total Grains (ounce equivalents)',
    whole_grains: 'Whole Grains (ounce equivalents)',
    non_whole_grains: 'Non-Whole Grains (ounce equivalents)'
  }
);

// Import USDA links
importFile(
  'Ingenious Medical Solutions LLC #001PK00000bwCmi_NCC Food Links to USDA Codes 2025.xlsx',
  'usda_links',
  {
    food_id: 'Food ID',
    food_type_reference: 'Food Type Reference',
    usda_code: 'USDA Food Code'
  }
);

// Get database stats
console.log('\n' + '='.repeat(80));
console.log('DATABASE STATISTICS');
console.log('='.repeat(80));

const tables = ['foods', 'densities', 'food_specific_units', 'grain_equivalents', 'usda_links'];
tables.forEach(table => {
  const result = db.prepare(`SELECT COUNT(*) as count FROM ${table}`).get();
  console.log(`  ${table}: ${result.count.toLocaleString()} rows`);
});

// Get database size
const stats = fs.statSync(dbPath);
console.log(`\n  Database size: ${(stats.size / (1024 * 1024)).toFixed(2)} MB`);

db.close();

console.log('\n🎉 Database created successfully!');
console.log(`💾 Location: ${dbPath}`);

