const XLSX = require('xlsx');
const fs = require('fs');
const path = require('path');

// Path to the Excel files  
const dataDir = '/Users/weijiahuang/Downloads/Ingenious Medical Solutions, LLC, Client #001PK00000bwCmi';

console.log('🚀 Starting food data import...');
console.log('📂 Data directory:', dataDir);

// List all Excel files
const files = fs.readdirSync(dataDir)
  .filter(f => f.endsWith('.xlsx') && !f.startsWith('.~'))
  .sort();

console.log('\n📄 Found Excel files:');
files.forEach((file, index) => {
  const stats = fs.statSync(path.join(dataDir, file));
  const sizeMB = (stats.size / (1024 * 1024)).toFixed(2);
  console.log(`  ${index + 1}. ${file} (${sizeMB} MB)`);
});

// Function to read and analyze Excel file structure
function analyzeExcelFile(filePath) {
  console.log(`\n📊 Analyzing: ${path.basename(filePath)}`);
  
  const workbook = XLSX.readFile(filePath);
  const sheetName = workbook.SheetNames[0];
  const worksheet = workbook.Sheets[sheetName];
  
  // Convert to JSON
  const data = XLSX.utils.sheet_to_json(worksheet, { defval: null });
  
  if (data.length === 0) {
    console.log('   ⚠️  Empty sheet');
    return null;
  }
  
  // Get column names
  const columns = Object.keys(data[0]);
  console.log(`   📋 Columns (${columns.length}):`, columns.slice(0, 5).join(', '), columns.length > 5 ? '...' : '');
  console.log(`   📊 Rows: ${data.length.toLocaleString()}`);
  
  // Show sample data
  console.log('   🔍 Sample row:');
  Object.entries(data[0]).slice(0, 3).forEach(([key, value]) => {
    console.log(`      ${key}: ${value}`);
  });
  
  return {
    fileName: path.basename(filePath),
    sheetName,
    columns,
    rowCount: data.length,
    sampleData: data.slice(0, 2)
  };
}

// Analyze each file
console.log('\n' + '='.repeat(80));
console.log('ANALYZING ALL FILES');
console.log('='.repeat(80));

const fileAnalysis = {};

files.forEach(file => {
  const filePath = path.join(dataDir, file);
  try {
    const analysis = analyzeExcelFile(filePath);
    if (analysis) {
      fileAnalysis[file] = analysis;
    }
  } catch (error) {
    console.log(`   ❌ Error: ${error.message}`);
  }
});

// Save analysis to JSON
const outputPath = path.join(__dirname, 'foodDataAnalysis.json');
fs.writeFileSync(outputPath, JSON.stringify(fileAnalysis, null, 2));
console.log(`\n✅ Analysis saved to: ${outputPath}`);
console.log('\n🎉 Analysis complete!');

