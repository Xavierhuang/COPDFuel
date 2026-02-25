import React, { useState } from 'react';
import { View, StyleSheet, SafeAreaView, StatusBar, Text, ScrollView, TouchableOpacity, Image, Alert, TextInput, Modal } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Header } from './components/Header';
import { Hero } from './components/Hero';
import { IntroductionSection } from './components/IntroductionSection';
import { Footer } from './components/Footer';
import { BottomNavigation } from './components/BottomNavigation';
import { ProgramsNearMe } from './components/ProgramsNearMe';

export default function App() {
  const [activeTab, setActiveTab] = useState('home');
  const [selectedTool, setSelectedTool] = useState('severity');
  const [selectedFEV1, setSelectedFEV1] = useState('Select FEV1 percentage');
  const [selectedHospitalizations, setSelectedHospitalizations] = useState('Select number');
  const [selectedFlareups, setSelectedFlareups] = useState('Select number');
  const [selectedOxygen, setSelectedOxygen] = useState('Select level');
  const [selectedOxygenLiters, setSelectedOxygenLiters] = useState('Select LPM');
  const [showFEV1Dropdown, setShowFEV1Dropdown] = useState(false);
  const [showHospitalizationDropdown, setShowHospitalizationDropdown] = useState(false);
  const [showFlareupDropdown, setShowFlareupDropdown] = useState(false);
  const [showOxygenDropdown, setShowOxygenDropdown] = useState(false);
  const [showOxygenLitersDropdown, setShowOxygenLitersDropdown] = useState(false);
  const [expandedMedicationCategory, setExpandedMedicationCategory] = useState(null);
  
  // Food tracking states
  const [showAddFoodModal, setShowAddFoodModal] = useState(false);
  const [selectedMealCategory, setSelectedMealCategory] = useState('');
  const [foodName, setFoodName] = useState('');
  const [foodQuantity, setFoodQuantity] = useState('');
  const [foodCalories, setFoodCalories] = useState('');
  const [foodProtein, setFoodProtein] = useState('');
  const [foodCarbs, setFoodCarbs] = useState('');
  const [foodFat, setFoodFat] = useState('');
  
  // Food search states
  const [foodSearchQuery, setFoodSearchQuery] = useState('');
  const [foodSearchResults, setFoodSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showFoodSearch, setShowFoodSearch] = useState(false);
  
  // Date tracking state
  const [currentDate, setCurrentDate] = useState(new Date());
  
  // Saved foods for each meal (organized by date)
  const [savedFoods, setSavedFoods] = useState({
    Breakfast: [],
    Lunch: [],
    Dinner: [],
    Snacks: []
  });
  
  // All foods organized by date
  const [foodsByDate, setFoodsByDate] = useState({});
  
  // Exercise tracking states
  const [showExerciseModal, setShowExerciseModal] = useState(false);
  const [selectedExerciseType, setSelectedExerciseType] = useState('');
  const [exerciseMinutes, setExerciseMinutes] = useState('');
  const [customExerciseType, setCustomExerciseType] = useState('');
  const [dailyExercises, setDailyExercises] = useState([]);
  
  // Oxygen saturation tracking states
  const [showOxygenModal, setShowOxygenModal] = useState(false);
  const [oxygenLevel, setOxygenLevel] = useState('');
  const [dailyOxygenReadings, setDailyOxygenReadings] = useState([]);
  
  // Weight tracking states
  const [showWeightModal, setShowWeightModal] = useState(false);
  const [weightType, setWeightType] = useState(''); // 'current' or 'goal'
  const [currentWeight, setCurrentWeight] = useState('');
  const [goalWeight, setGoalWeight] = useState('');
  const [weightInput, setWeightInput] = useState('');
  const [doctorName, setDoctorName] = useState('');
  const [doctorPhone, setDoctorPhone] = useState('');
  const [emergencyContactName, setEmergencyContactName] = useState('');
  const [emergencyContactPhone, setEmergencyContactPhone] = useState('');
  const [dailyMedications, setDailyMedications] = useState([]);
  const [exacerbationMedications, setExacerbationMedications] = useState([]);
  const [showAddMedicationModal, setShowAddMedicationModal] = useState(false);
  const [medicationType, setMedicationType] = useState('daily'); // 'daily' or 'exacerbation'
  const [newMedication, setNewMedication] = useState({
    name: '',
    dosage: '',
    frequency: ''
  });
  const [currentScreen, setCurrentScreen] = useState('main');
  const [savedActionPlans, setSavedActionPlans] = useState([]);
  const [additionalInstructions, setAdditionalInstructions] = useState('');
  
  // Profile states
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [profileField, setProfileField] = useState('');
  const [profileInput, setProfileInput] = useState('');
  const [age, setAge] = useState('');
  const [sex, setSex] = useState('');
  const [height, setHeight] = useState('');
  const [lastUpdated, setLastUpdated] = useState('');

  const toggleFEV1Dropdown = () => {
    setShowFEV1Dropdown(!showFEV1Dropdown);
    setShowHospitalizationDropdown(false);
    setShowFlareupDropdown(false);
    setShowOxygenDropdown(false);
  };

  const toggleHospitalizationDropdown = () => {
    setShowHospitalizationDropdown(!showHospitalizationDropdown);
    setShowFEV1Dropdown(false);
    setShowFlareupDropdown(false);
    setShowOxygenDropdown(false);
  };

  const toggleFlareupDropdown = () => {
    setShowFlareupDropdown(!showFlareupDropdown);
    setShowFEV1Dropdown(false);
    setShowHospitalizationDropdown(false);
    setShowOxygenDropdown(false);
  };

  const toggleOxygenDropdown = () => {
    setShowOxygenDropdown(!showOxygenDropdown);
    setShowFEV1Dropdown(false);
    setShowHospitalizationDropdown(false);
    setShowFlareupDropdown(false);
    setShowOxygenLitersDropdown(false);
  };

  const toggleOxygenLitersDropdown = () => {
    setShowOxygenLitersDropdown(!showOxygenLitersDropdown);
    setShowFEV1Dropdown(false);
    setShowHospitalizationDropdown(false);
    setShowFlareupDropdown(false);
    setShowOxygenDropdown(false);
  };

  // Food tracking functions
  const openAddFoodModal = (category: string) => {
    console.log('Opening food modal for category:', category);
    console.log('Current showAddFoodModal state:', showAddFoodModal);
    setSelectedMealCategory(category);
    setShowAddFoodModal(true);
    console.log('Modal state should now be true');
  };

  const closeAddFoodModal = () => {
    setShowAddFoodModal(false);
    setSelectedMealCategory('');
    setFoodName('');
    setFoodQuantity('');
    setFoodCalories('');
    setFoodProtein('');
    setFoodCarbs('');
    setFoodFat('');
  };

  // Food search function using USDA FoodData Central API (closest free alternative to NCCDB)
  const searchFoodDatabase = async (query: string) => {
    if (!query.trim()) return;
    
    setIsSearching(true);
    try {
      // Using USDA FoodData Central API - includes:
      // - Foundation Foods (nutrient-dense foods like NCCDB)
      // - SR Legacy (similar to NCCDB's standard reference)
      // - Branded Foods (8,000+ like NCCDB)
      // - Survey Foods (FNDDS - what people actually eat)
      
      // Using your personal USDA API key for higher rate limits
      const response = await fetch(
        `https://api.nal.usda.gov/fdc/v1/foods/search?query=${encodeURIComponent(query)}&pageSize=15&dataType=Foundation,SR%20Legacy,Branded&api_key=[REDACTED_USDA_API_KEY]`
      );
      
      if (!response.ok) {
        throw new Error('Failed to search foods');
      }
      
      const data = await response.json();
      const foods = data.foods.map((food: any) => ({
        id: food.fdcId,
        description: food.description,
        brandOwner: food.brandOwner,
        ingredients: food.ingredients,
        nutrients: food.foodNutrients.reduce((acc: any, nutrient: any) => {
          const nutrientName = nutrient.nutrientName.toLowerCase();
          if (nutrientName.includes('energy') || nutrientName.includes('calorie')) {
            acc.calories = nutrient.value;
          } else if (nutrientName.includes('protein')) {
            acc.protein = nutrient.value;
          } else if (nutrientName.includes('carbohydrate')) {
            acc.carbs = nutrient.value;
          } else if (nutrientName.includes('total lipid') || nutrientName.includes('fat')) {
            acc.fat = nutrient.value;
          }
          return acc;
        }, {})
      }));
      
      setFoodSearchResults(foods);
    } catch (error) {
      console.error('Food search error:', error);
      
      // Fallback with common foods if API fails
      const fallbackFoods = [
        {
          id: 'fallback-1',
          description: `${query} (estimated)`,
          brandOwner: 'Estimated Values',
          nutrients: {
            calories: query.toLowerCase().includes('chicken') ? 165 : 
                     query.toLowerCase().includes('apple') ? 52 : 
                     query.toLowerCase().includes('rice') ? 130 : 100,
            protein: query.toLowerCase().includes('chicken') ? 20 : 
                    query.toLowerCase().includes('apple') ? 0.3 : 
                    query.toLowerCase().includes('rice') ? 2.7 : 2,
            carbs: query.toLowerCase().includes('chicken') ? 0 : 
                  query.toLowerCase().includes('apple') ? 14 : 
                  query.toLowerCase().includes('rice') ? 28 : 15,
            fat: query.toLowerCase().includes('chicken') ? 3.6 : 
                query.toLowerCase().includes('apple') ? 0.2 : 
                query.toLowerCase().includes('rice') ? 0.3 : 1
          }
        }
      ];
      
      setFoodSearchResults(fallbackFoods);
      Alert.alert('API Limit Reached', 'Using estimated values. For accurate data, try again later or enter manually.');
    } finally {
      setIsSearching(false);
    }
  };

  const selectFoodFromSearch = (food: any) => {
    setFoodName(food.description);
    setFoodCalories(food.nutrients.calories?.toString() || '');
    setFoodProtein(food.nutrients.protein?.toString() || '');
    setFoodCarbs(food.nutrients.carbs?.toString() || '');
    setFoodFat(food.nutrients.fat?.toString() || '');
    setShowFoodSearch(false);
    setFoodSearchQuery('');
    setFoodSearchResults([]);
  };

  const saveFoodItem = () => {
    console.log('Save button clicked!');
    console.log('Current values:', {
      foodName: foodName,
      foodQuantity: foodQuantity,
      foodCalories: foodCalories,
      foodProtein: foodProtein,
      foodCarbs: foodCarbs,
      foodFat: foodFat
    });
    
    if (!foodName.trim()) {
      Alert.alert('Error', 'Please enter a food name');
      return;
    }
    
    if (!foodQuantity.trim()) {
      Alert.alert(
        '⚠️ Quantity Required', 
        'Please enter the quantity you ate.\n\nExamples:\n• "1 cup"\n• "100g"\n• "1 piece"\n• "4 oz"',
        [{ text: 'OK', style: 'default' }]
      );
      return;
    }
    
    // Create food item object
    const foodItem = {
      id: Date.now().toString(),
      name: foodName,
      quantity: foodQuantity,
      calories: foodCalories,
      protein: foodProtein,
      carbs: foodCarbs,
      fat: foodFat,
      timestamp: new Date().toLocaleTimeString()
    };
    
    // Add food to the appropriate meal category for current date
    const dateKey = formatDateKey(currentDate);
    
    setSavedFoods(prevFoods => ({
      ...prevFoods,
      [selectedMealCategory]: [...prevFoods[selectedMealCategory], foodItem]
    }));
    
    // Also save to the date-organized structure
    setFoodsByDate(prevFoodsByDate => ({
      ...prevFoodsByDate,
      [dateKey]: {
        ...prevFoodsByDate[dateKey],
        [selectedMealCategory]: [...(prevFoodsByDate[dateKey]?.[selectedMealCategory] || []), foodItem]
      }
    }));
    
    console.log('Saving food item:', {
      category: selectedMealCategory,
      foodItem: foodItem
    });
    
    // Show success alert with detailed information
    Alert.alert(
      '✅ Food Added Successfully!',
      `${foodName} (${foodQuantity}) has been added to ${selectedMealCategory}\n\n` +
      `Nutrition Info:\n` +
      `• Calories: ${foodCalories || 'N/A'}\n` +
      `• Protein: ${foodProtein || 'N/A'}g\n` +
      `• Carbs: ${foodCarbs || 'N/A'}g\n` +
      `• Fat: ${foodFat || 'N/A'}g`,
      [
        {
          text: 'Great!',
          style: 'default',
        }
      ]
    );
    closeAddFoodModal();
  };

  // Weight tracking functions
  const openWeightModal = (type) => {
    setWeightType(type);
    setWeightInput('');
    setShowWeightModal(true);
  };

  const closeWeightModal = () => {
    setShowWeightModal(false);
    setWeightType('');
    setWeightInput('');
  };

  const saveWeight = () => {
    if (!weightInput.trim()) {
      Alert.alert('Error', 'Please enter a weight value');
      return;
    }

    const weight = parseFloat(weightInput);
    if (isNaN(weight) || weight <= 0) {
      Alert.alert('Error', 'Please enter a valid weight');
      return;
    }

    if (weightType === 'current') {
      setCurrentWeight(weightInput);
      Alert.alert('Success', `Current weight set to ${weightInput} lbs`);
    } else if (weightType === 'goal') {
      setGoalWeight(weightInput);
      Alert.alert('Success', `Goal weight set to ${weightInput} lbs`);
    }

    closeWeightModal();
  };

  // Profile functions
  const openProfileModal = (field: string) => {
    setProfileField(field);
    setProfileInput('');
    setShowProfileModal(true);
  };

  const closeProfileModal = () => {
    setShowProfileModal(false);
    setProfileField('');
    setProfileInput('');
  };

  const saveProfileField = () => {
    if (!profileInput.trim()) {
      Alert.alert('Error', 'Please enter a value');
      return;
    }

    switch (profileField) {
      case 'age':
        const ageValue = parseInt(profileInput);
        if (isNaN(ageValue) || ageValue <= 0 || ageValue > 120) {
          Alert.alert('Error', 'Please enter a valid age (1-120)');
          return;
        }
        setAge(profileInput);
        break;
      case 'sex':
        setSex(profileInput);
        break;
      case 'height':
        setHeight(profileInput);
        break;
    }

    setLastUpdated(new Date().toLocaleDateString());
    Alert.alert('Success', `${profileField} updated successfully!`);
    closeProfileModal();
  };

  const calculateBMI = () => {
    if (!currentWeight || !height) {
      return 'N/A';
    }

    try {
      const weightInKg = parseFloat(currentWeight) * 0.453592; // Convert lbs to kg
      const heightStr = height.replace(/[^\d.]/g, '');

      // Prevent empty or just period strings
      if (!heightStr || heightStr === '.' || heightStr === '..') {
        return 'N/A';
      }

      const heightInM = parseFloat(heightStr) * 0.0254; // Convert inches to meters

      if (!isNaN(heightInM) && heightInM > 0 && !isNaN(weightInKg) && weightInKg > 0) {
        const bmi = weightInKg / (heightInM * heightInM);
        const result = bmi.toFixed(1);

        // Extra validation to ensure we return a valid number
        if (result && !isNaN(parseFloat(result))) {
          return result;
        }
      }
    } catch (error) {
      console.error('BMI calculation error:', error);
    }

    return 'N/A';
  };

  // Calculate daily nutrition totals
  // Date navigation functions
  const formatDateKey = (date: Date) => {
    return date.toISOString().split('T')[0]; // YYYY-MM-DD format
  };

  const formatDisplayDate = (date: Date) => {
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    
    if (formatDateKey(date) === formatDateKey(today)) {
      return 'Today';
    } else if (formatDateKey(date) === formatDateKey(yesterday)) {
      return 'Yesterday';
    } else {
      return date.toLocaleDateString('en-US', { 
        weekday: 'short', 
        month: 'short', 
        day: 'numeric' 
      });
    }
  };

  const goToPreviousDay = () => {
    const prevDate = new Date(currentDate);
    prevDate.setDate(prevDate.getDate() - 1);
    setCurrentDate(prevDate);
    loadFoodsForDate(prevDate);
  };

  const goToNextDay = () => {
    const nextDate = new Date(currentDate);
    nextDate.setDate(nextDate.getDate() + 1);
    setCurrentDate(nextDate);
    loadFoodsForDate(nextDate);
  };

  const loadFoodsForDate = (date: Date) => {
    const dateKey = formatDateKey(date);
    const foodsForDate = foodsByDate[dateKey] || {
      Breakfast: [],
      Lunch: [],
      Dinner: [],
      Snacks: []
    };
    setSavedFoods(foodsForDate);
  };

  // Exercise tracking functions
  const openExerciseModal = () => {
    setShowExerciseModal(true);
    setSelectedExerciseType('');
    setExerciseMinutes('');
    setCustomExerciseType('');
  };

  const closeExerciseModal = () => {
    setShowExerciseModal(false);
    setSelectedExerciseType('');
    setExerciseMinutes('');
    setCustomExerciseType('');
  };

  const saveExercise = () => {
    const exerciseType = selectedExerciseType === 'other' ? customExerciseType : selectedExerciseType;
    
    if (!exerciseType.trim() || !exerciseMinutes.trim()) {
      Alert.alert('Error', 'Please select exercise type and enter minutes');
      return;
    }

    const minutes = parseFloat(exerciseMinutes);
    if (isNaN(minutes) || minutes <= 0) {
      Alert.alert('Error', 'Please enter valid minutes');
      return;
    }

    const exercise = {
      id: Date.now().toString(),
      type: exerciseType,
      minutes: minutes,
      timestamp: new Date().toLocaleTimeString(),
      date: formatDateKey(currentDate)
    };

    setDailyExercises(prev => [...prev, exercise]);
    
    Alert.alert(
      '🏃‍♂️ Exercise Logged!',
      `${exerciseType} for ${minutes} minutes has been recorded for ${formatDisplayDate(currentDate)}`
    );
    
    closeExerciseModal();
  };

  const getTotalExerciseMinutes = () => {
    return dailyExercises.reduce((total, exercise) => total + exercise.minutes, 0);
  };

  // Phone health data import function
  const importFromPhoneHealth = async () => {
    try {
      Alert.alert(
        '📱 Import Health Data',
        'Choose data source:',
        [
          {
            text: 'Cancel',
            style: 'cancel'
          },
          {
            text: 'Apple Health',
            onPress: () => importAppleHealthData()
          },
          {
            text: 'Google Fit',
            onPress: () => importGoogleFitData()
          },
          {
            text: 'Manual Entry',
            onPress: () => showQuickExerciseOptions()
          }
        ]
      );
    } catch (error) {
      console.error('Health data import error:', error);
      Alert.alert('Error', 'Unable to access health data. Please enter manually.');
    }
  };

  const importAppleHealthData = async () => {
    // Simulate Apple Health data import
    Alert.alert(
      '🍎 Apple Health Integration',
      'This would connect to Apple HealthKit to import:\n\n• Steps taken today\n• Workout sessions\n• Active minutes\n• Heart rate data\n\nFor now, here are some sample readings:',
      [
        {
          text: 'Import Walking (45 min)',
          onPress: () => {
            setSelectedExerciseType('Walking');
            setExerciseMinutes('45');
            Alert.alert('Success', 'Walking data imported from Apple Health!');
          }
        },
        {
          text: 'Import Running (20 min)',
          onPress: () => {
            setSelectedExerciseType('Running/Jogging');
            setExerciseMinutes('20');
            Alert.alert('Success', 'Running data imported from Apple Health!');
          }
        },
        {
          text: 'Import Weight Training (30 min)',
          onPress: () => {
            setSelectedExerciseType('Weight Training');
            setExerciseMinutes('30');
            Alert.alert('Success', 'Weight training data imported from Apple Health!');
          }
        },
        {
          text: 'Cancel',
          style: 'cancel'
        }
      ]
    );
  };

  const importGoogleFitData = async () => {
    // Simulate Google Fit data import
    Alert.alert(
      '🏃‍♂️ Google Fit Integration',
      'This would connect to Google Fit to import:\n\n• Daily step count\n• Workout activities\n• Active minutes\n• Calories burned\n\nFor now, here are some sample readings:',
      [
        {
          text: 'Import Walking (35 min)',
          onPress: () => {
            setSelectedExerciseType('Walking');
            setExerciseMinutes('35');
            Alert.alert('Success', 'Walking data imported from Google Fit!');
          }
        },
        {
          text: 'Import Cycling (25 min)',
          onPress: () => {
            setSelectedExerciseType('other');
            setCustomExerciseType('Cycling');
            setExerciseMinutes('25');
            Alert.alert('Success', 'Cycling data imported from Google Fit!');
          }
        },
        {
          text: 'Import Weight Training (40 min)',
          onPress: () => {
            setSelectedExerciseType('Weight Training');
            setExerciseMinutes('40');
            Alert.alert('Success', 'Weight training data imported from Google Fit!');
          }
        },
        {
          text: 'Cancel',
          style: 'cancel'
        }
      ]
    );
  };

  const showQuickExerciseOptions = () => {
    Alert.alert(
      '⚡ Quick Exercise Entry',
      'Select a common exercise duration:',
      [
        {
          text: '15 minutes',
          onPress: () => setExerciseMinutes('15')
        },
        {
          text: '30 minutes',
          onPress: () => setExerciseMinutes('30')
        },
        {
          text: '45 minutes',
          onPress: () => setExerciseMinutes('45')
        },
        {
          text: '60 minutes',
          onPress: () => setExerciseMinutes('60')
        },
        {
          text: 'Cancel',
          style: 'cancel'
        }
      ]
    );
  };

  // Oxygen saturation tracking functions
  const openOxygenModal = () => {
    setShowOxygenModal(true);
    setOxygenLevel('');
  };

  const closeOxygenModal = () => {
    setShowOxygenModal(false);
    setOxygenLevel('');
  };

  const saveOxygenReading = () => {
    if (!oxygenLevel.trim()) {
      Alert.alert('Error', 'Please enter oxygen saturation level');
      return;
    }

    const level = parseFloat(oxygenLevel);
    if (isNaN(level) || level < 0 || level > 100) {
      Alert.alert('Error', 'Please enter a valid oxygen saturation level (0-100%)');
      return;
    }

    const reading = {
      id: Date.now().toString(),
      level: level,
      timestamp: new Date().toLocaleTimeString(),
      date: formatDateKey(currentDate)
    };

    setDailyOxygenReadings(prev => [...prev, reading]);
    
    // Provide feedback based on oxygen level and reading count
    const readingCount = dailyOxygenReadings.length + 1;
    let message = `Oxygen saturation of ${level}% recorded for ${formatDisplayDate(currentDate)}`;
    
    if (readingCount > 1) {
      message += `\n\nThis is your ${readingCount}${readingCount === 2 ? 'nd' : readingCount === 3 ? 'rd' : 'th'} reading today.`;
    }
    
    if (level < 90) {
      message += '\n\n⚠️ Note: Levels below 90% may require medical attention.';
    } else if (level >= 95) {
      message += '\n\n✅ Great! This is a healthy oxygen level.';
    } else if (level >= 88) {
      message += '\n\n📊 This level is common for COPD patients.';
    }
    
    Alert.alert('🫁 Oxygen Level Recorded!', message);
    closeOxygenModal();
  };

  const getLatestOxygenReading = () => {
    if (dailyOxygenReadings.length === 0) return 'N/A';
    const latest = dailyOxygenReadings[dailyOxygenReadings.length - 1];
    return `${latest.level}%`;
  };

  const calculateDailyTotals = () => {
    const allFoods = [
      ...savedFoods.Breakfast,
      ...savedFoods.Lunch,
      ...savedFoods.Dinner,
      ...savedFoods.Snacks
    ];

    return allFoods.reduce((totals, food) => {
      totals.calories += parseFloat(food.calories) || 0;
      totals.protein += parseFloat(food.protein) || 0;
      totals.carbs += parseFloat(food.carbs) || 0;
      totals.fat += parseFloat(food.fat) || 0;
      return totals;
    }, { calories: 0, protein: 0, carbs: 0, fat: 0 });
  };

  const openAddMedicationModal = (type) => {
    setMedicationType(type);
    setNewMedication({ name: '', dosage: '', frequency: '' });
    setShowAddMedicationModal(true);
  };

  const saveMedication = async () => {
    if (!newMedication.name || !newMedication.dosage || !newMedication.frequency) {
      Alert.alert('Error', 'Please fill in all medication fields');
      return;
    }

    const medication = {
      id: Date.now().toString(),
      ...newMedication
    };

    console.log('Saving medication:', medication);

    if (medicationType === 'daily') {
      const updatedMedications = [...dailyMedications, medication];
      console.log('Updated daily medications:', updatedMedications);
      setDailyMedications(updatedMedications);
      await AsyncStorage.setItem('dailyMedications', JSON.stringify(updatedMedications));
    } else {
      const updatedMedications = [...exacerbationMedications, medication];
      console.log('Updated exacerbation medications:', updatedMedications);
      setExacerbationMedications(updatedMedications);
      await AsyncStorage.setItem('exacerbationMedications', JSON.stringify(updatedMedications));
    }

    setShowAddMedicationModal(false);
    setNewMedication({ name: '', dosage: '', frequency: '' });
  };

  const deleteMedication = async (id, type) => {
    console.log('deleteMedication called with id:', id, 'type:', type);
    console.log('Current daily medications:', dailyMedications);
    console.log('Current exacerbation medications:', exacerbationMedications);
    
    // Simple delete without confirmation for testing
    try {
      if (type === 'daily') {
        console.log('Before filter - daily medications:', dailyMedications);
        const updatedMedications = dailyMedications.filter(med => {
          console.log('Comparing med.id:', med.id, 'with target id:', id, 'match:', med.id !== id);
          return med.id !== id;
        });
        console.log('After filter - updated medications:', updatedMedications);
        setDailyMedications(updatedMedications);
        await AsyncStorage.setItem('dailyMedications', JSON.stringify(updatedMedications));
        console.log('Daily medication deleted successfully');
      } else {
        console.log('Before filter - exacerbation medications:', exacerbationMedications);
        const updatedMedications = exacerbationMedications.filter(med => {
          console.log('Comparing med.id:', med.id, 'with target id:', id, 'match:', med.id !== id);
          return med.id !== id;
        });
        console.log('After filter - updated medications:', updatedMedications);
        setExacerbationMedications(updatedMedications);
        await AsyncStorage.setItem('exacerbationMedications', JSON.stringify(updatedMedications));
        console.log('Exacerbation medication deleted successfully');
      }
    } catch (error) {
      console.error('Error deleting medication:', error);
      Alert.alert('Error', 'Failed to delete medication. Please try again.');
    }
  };

  const loadMedications = async () => {
    try {
      const dailyData = await AsyncStorage.getItem('dailyMedications');
      const exacerbationData = await AsyncStorage.getItem('exacerbationMedications');
      
      if (dailyData) {
        setDailyMedications(JSON.parse(dailyData));
      }
      if (exacerbationData) {
        setExacerbationMedications(JSON.parse(exacerbationData));
      }
    } catch (error) {
      console.error('Error loading medications:', error);
    }
  };

  const saveActionPlan = async () => {
    try {
      const actionPlan = {
        id: Date.now().toString(),
        timestamp: new Date().toISOString(),
        dateCreated: new Date().toLocaleDateString(),
        timeCreated: new Date().toLocaleTimeString(),
        contacts: {
          doctorName,
          doctorPhone,
          emergencyContactName,
          emergencyContactPhone
        },
        medications: {
          daily: dailyMedications,
          exacerbation: exacerbationMedications
        },
        additionalInstructions
      };

      const updatedPlans = [...savedActionPlans, actionPlan];
      setSavedActionPlans(updatedPlans);
      await AsyncStorage.setItem('savedActionPlans', JSON.stringify(updatedPlans));
      
      Alert.alert('Success', 'Action plan saved successfully!');
      console.log('Action plan saved:', actionPlan);
    } catch (error) {
      console.error('Error saving action plan:', error);
      Alert.alert('Error', 'Failed to save action plan. Please try again.');
    }
  };

  const loadSavedActionPlans = async () => {
    try {
      const savedData = await AsyncStorage.getItem('savedActionPlans');
      if (savedData) {
        setSavedActionPlans(JSON.parse(savedData));
      }
    } catch (error) {
      console.error('Error loading saved action plans:', error);
    }
  };

  // Load medications and saved action plans when component mounts
  React.useEffect(() => {
    loadMedications();
    loadSavedActionPlans();
  }, []);

  const renderContent = () => {
    switch (activeTab) {
      case 'home':
        return (
          <IntroductionSection />
        );
      case 'guidelines':
        return (
          <ScrollView 
            style={styles.guidelinesContainer} 
            showsVerticalScrollIndicator={false}
            contentContainerStyle={styles.guidelinesContentContainer}
          >
            {/* Main Guidelines Section */}
            <View style={styles.guidelinesSection}>
              <Text style={styles.guidelinesTitle}>COPD Dietary Guidelines</Text>
              <Text style={styles.guidelinesIntro}>
                Proper nutrition plays a vital role in managing COPD symptoms and improving overall health.
              </Text>
              
              {/* Key Guidelines */}
              <View style={styles.guidelineItem}>
                <Text style={styles.guidelineTitle}>Maintain a Healthy Weight</Text>
                <Text style={styles.guidelineText}>
                  Being underweight can reduce respiratory muscle strength, while excess weight can make breathing more difficult. Aim for a healthy weight through balanced nutrition.
                </Text>
              </View>
              
              <View style={styles.guidelineItem}>
                <Text style={styles.guidelineTitle}>Stay Hydrated</Text>
                <Text style={styles.guidelineText}>
                  Drinking plenty of fluids helps keep mucus thin and easier to clear from the lungs. Aim for 6-8 glasses of water daily unless otherwise advised by your doctor.
                </Text>
              </View>
              
              <View style={styles.guidelineItem}>
                <Text style={styles.guidelineTitle}>Eat Smaller, More Frequent Meals</Text>
                <Text style={styles.guidelineText}>
                  Large meals can make breathing uncomfortable by pushing against your diaphragm. Smaller, more frequent meals can help prevent this discomfort.
                </Text>
              </View>
              
              <View style={styles.guidelineItem}>
                <Text style={styles.guidelineTitle}>Monitor Salt Intake</Text>
                <Text style={styles.guidelineText}>
                  Excess sodium can cause fluid retention, which may make breathing more difficult. Choose fresh foods and herbs over salt for flavoring.
                </Text>
              </View>
              
              <View style={styles.guidelineItem}>
                <Text style={styles.guidelineTitle}>Include Antioxidant-Rich Foods</Text>
                <Text style={styles.guidelineText}>
                  Foods high in antioxidants can help reduce inflammation in the airways. Fresh fruits and vegetables are excellent sources.
                </Text>
              </View>
              
              <View style={styles.guidelineItem}>
                <Text style={styles.guidelineTitle}>Consider Supplements</Text>
                <Text style={styles.guidelineText}>
                  Consult with your healthcare provider about supplements like vitamin D, calcium, and omega-3 fatty acids, which may benefit COPD patients.
                </Text>
              </View>
              
              <Text style={styles.consultText}>
                Always consult with your healthcare provider before making significant changes to your diet.
              </Text>
            </View>

            {/* Preventing Exacerbations Section */}
            <View style={styles.exacerbationsSection}>
              <Text style={styles.sectionTitle}>Preventing COPD Exacerbations</Text>
              <Text style={styles.sectionSubtitle}>Strategies to reduce flare-ups and maintain lung function</Text>
              
              <Text style={styles.exacerbationsText}>
                To prevent COPD exacerbations, it is crucial to take various precautions. Besides performing breathing exercises and engaging in physical activity, maintaining a healthy diet is extremely important for lung function. Specifically, the amount of protein in one's diet can significantly impact lung health. Proper nutrition, combined with regular exercise and respiratory therapies, can help manage COPD and improve patients' quality of life.
              </Text>
              
              <View style={styles.strategyGrid}>
                <View style={styles.strategyItem}>
                  <Text style={styles.strategyTitle}>Nutrition Plan</Text>
                  <Text style={styles.strategyText}>
                    Focus on protein-rich foods like lean meats, fish, eggs, and plant proteins. Adequate protein intake helps maintain respiratory muscle strength.
                  </Text>
                </View>
                
                <View style={styles.strategyItem}>
                  <Text style={styles.strategyTitle}>Breathing Exercises</Text>
                  <Text style={styles.strategyText}>
                    Regular breathing exercises like pursed-lip breathing and diaphragmatic breathing can improve lung function and oxygen levels.
                  </Text>
                </View>
                
                <View style={styles.strategyItem}>
                  <Text style={styles.strategyTitle}>Physical Activity</Text>
                  <Text style={styles.strategyText}>
                    Regular, moderate exercise improves cardiovascular health and strengthens respiratory muscles. Consult with your healthcare provider for an appropriate exercise plan.
                  </Text>
                </View>
              </View>
            </View>

            {/* Protein Section */}
            <View style={styles.proteinSection}>
              <Text style={styles.sectionTitle}>The Importance of Protein</Text>
              <Text style={styles.proteinText}>
                Research shows that COPD patients with adequate protein intake have better outcomes. Protein helps maintain respiratory muscle mass and function, which can decline in COPD patients. Aim for 1.2-1.5 grams of protein per kilogram of body weight daily, distributed throughout your meals.
              </Text>
              <Text style={styles.exploreText}>Explore Protein-Rich Foods</Text>
            </View>

            {/* Recommended Foods Section */}
            <View style={styles.foodsSection}>
              <Text style={styles.sectionTitle}>Recommended Foods for COPD</Text>
              <Text style={styles.foodsIntro}>
                Making smart food choices can help manage COPD symptoms and improve your overall health.
              </Text>
              
              <View style={styles.foodCategory}>
                <Text style={styles.foodCategoryTitle}>Foods to Embrace</Text>
                <View style={styles.foodItem}>
                  <Text style={styles.checkmark}>+</Text>
                  <View style={styles.foodContent}>
                    <Text style={styles.foodName}>Fresh Fruits and Vegetables</Text>
                    <Text style={styles.foodDescription}>
                      Rich in antioxidants and fiber, they help reduce inflammation and support immune function.
                    </Text>
                  </View>
                </View>
                
                <View style={styles.foodItem}>
                  <Text style={styles.checkmark}>+</Text>
                  <View style={styles.foodContent}>
                    <Text style={styles.foodName}>Lean Proteins</Text>
                    <Text style={styles.foodDescription}>
                      Fish, poultry, beans, and tofu provide essential amino acids without excess calories.
                    </Text>
                  </View>
                </View>
                
                <View style={styles.foodItem}>
                  <Text style={styles.checkmark}>+</Text>
                  <View style={styles.foodContent}>
                    <Text style={styles.foodName}>Whole Grains</Text>
                    <Text style={styles.foodDescription}>
                      Brown rice, whole wheat bread, and oats provide sustained energy and important nutrients.
                    </Text>
                  </View>
                </View>
                
                <View style={styles.foodItem}>
                  <Text style={styles.checkmark}>+</Text>
                  <View style={styles.foodContent}>
                    <Text style={styles.foodName}>Healthy Fats</Text>
                    <Text style={styles.foodDescription}>
                      Olive oil, avocados, nuts, and fatty fish contain omega-3s that may help reduce inflammation.
                    </Text>
                  </View>
                </View>
                
                <View style={styles.foodItem}>
                  <Text style={styles.checkmark}>+</Text>
                  <View style={styles.foodContent}>
                    <Text style={styles.foodName}>Dairy or Fortified Alternatives</Text>
                    <Text style={styles.foodDescription}>
                      Good sources of calcium and vitamin D for bone health, especially important if taking steroids.
                    </Text>
                  </View>
                </View>
              </View>
              
              <View style={styles.foodCategory}>
                <Text style={styles.foodCategoryTitle}>Foods to Limit or Avoid</Text>
                <View style={styles.foodItem}>
                  <Text style={styles.xmark}>-</Text>
                  <View style={styles.foodContent}>
                    <Text style={styles.foodName}>Processed Foods</Text>
                    <Text style={styles.foodDescription}>
                      Often high in sodium, preservatives, and artificial ingredients that may worsen inflammation.
                    </Text>
                  </View>
                </View>
                
                <View style={styles.foodItem}>
                  <Text style={styles.xmark}>-</Text>
                  <View style={styles.foodContent}>
                    <Text style={styles.foodName}>Gas-Producing Foods</Text>
                    <Text style={styles.foodDescription}>
                      Beans, cabbage, and carbonated beverages can cause bloating that makes breathing uncomfortable.
                    </Text>
                  </View>
                </View>
                
                <View style={styles.foodItem}>
                  <Text style={styles.xmark}>-</Text>
                  <View style={styles.foodContent}>
                    <Text style={styles.foodName}>Excessive Salt</Text>
                    <Text style={styles.foodDescription}>
                      Can lead to fluid retention, making it harder to breathe and potentially raising blood pressure.
                    </Text>
                  </View>
                </View>
                
                <View style={styles.foodItem}>
                  <Text style={styles.xmark}>-</Text>
                  <View style={styles.foodContent}>
                    <Text style={styles.foodName}>Cold Foods</Text>
                    <Text style={styles.foodDescription}>
                      Very cold foods and beverages can trigger bronchospasm in some people with COPD.
                    </Text>
                  </View>
                </View>
                
                <View style={styles.foodItem}>
                  <Text style={styles.xmark}>-</Text>
                  <View style={styles.foodContent}>
                    <Text style={styles.foodName}>Excessive Dairy</Text>
                    <Text style={styles.foodDescription}>
                      May increase mucus production in some individuals, though evidence is mixed.
                    </Text>
                  </View>
                </View>
              </View>
            </View>
          </ScrollView>
        );
      case 'tracking':
        return (
          <ScrollView 
            style={styles.trackingContainer} 
            showsVerticalScrollIndicator={false}
            contentContainerStyle={styles.trackingContentContainer}
          >
            {/* Tracking Header with Date Navigation */}
            <View style={styles.trackingHeader}>
              <TouchableOpacity style={styles.dateNavButton} onPress={goToPreviousDay}>
                <Text style={styles.dateNavArrow}>&lt;</Text>
              </TouchableOpacity>
              
              <View style={styles.dateContainer}>
                <Text style={styles.trackingTitle}>{formatDisplayDate(currentDate)}</Text>
                <Text style={styles.trackingDate}>
                  {currentDate.toLocaleDateString('en-US', { 
                    month: 'long', 
                    day: 'numeric', 
                    year: 'numeric' 
                  })}
                </Text>
              </View>
              
              <TouchableOpacity style={styles.dateNavButton} onPress={goToNextDay}>
                <Text style={styles.dateNavArrow}>&gt;</Text>
              </TouchableOpacity>
            </View>

            {/* Nutrition Targets Section */}
            <View style={styles.targetsSection}>
              <View style={styles.targetsHeader}>
                <Text style={styles.targetsTitle}>NUTRITION TARGETS</Text>
                <TouchableOpacity>
                  <Text style={styles.targetsArrow}>&gt;</Text>
                </TouchableOpacity>
              </View>
              
              <View style={styles.targetsGrid}>
                <View style={styles.targetCard}>
                  <Text style={styles.targetLabel}>Energy</Text>
                  <Text style={styles.targetValue}>{calculateDailyTotals().calories.toFixed(1)} / 1,800 kcal</Text>
                  <View style={styles.targetProgress}>
                    <View style={[styles.targetProgressBar, { width: `${Math.min(100, (calculateDailyTotals().calories / 1800) * 100)}%` }]} />
                  </View>
                  <Text style={styles.targetPercentage}>{Math.round((calculateDailyTotals().calories / 1800) * 100)}%</Text>
                </View>

                <View style={styles.targetCard}>
                  <Text style={styles.targetLabel}>Protein</Text>
                  <Text style={styles.targetValue}>{calculateDailyTotals().protein.toFixed(1)} / 90.0 g</Text>
                  <View style={styles.targetProgress}>
                    <View style={[styles.targetProgressBar, { width: `${Math.min(100, (calculateDailyTotals().protein / 90) * 100)}%` }]} />
                  </View>
                  <Text style={styles.targetPercentage}>{Math.round((calculateDailyTotals().protein / 90) * 100)}%</Text>
                </View>

                <View style={styles.targetCard}>
                  <Text style={styles.targetLabel}>Carbs</Text>
                  <Text style={styles.targetValue}>{calculateDailyTotals().carbs.toFixed(1)} / 225.0 g</Text>
                  <View style={styles.targetProgress}>
                    <View style={[styles.targetProgressBar, { width: `${Math.min(100, (calculateDailyTotals().carbs / 225) * 100)}%` }]} />
                  </View>
                  <Text style={styles.targetPercentage}>{Math.round((calculateDailyTotals().carbs / 225) * 100)}%</Text>
                </View>

                <View style={styles.targetCard}>
                  <Text style={styles.targetLabel}>Fat</Text>
                  <Text style={styles.targetValue}>{calculateDailyTotals().fat.toFixed(1)} / 60.0 g</Text>
                  <View style={styles.targetProgress}>
                    <View style={[styles.targetProgressBar, { width: `${Math.min(100, (calculateDailyTotals().fat / 60) * 100)}%` }]} />
                  </View>
                  <Text style={styles.targetPercentage}>{Math.round((calculateDailyTotals().fat / 60) * 100)}%</Text>
                </View>
              </View>
            </View>

            <View style={styles.weightSection}>
              <Text style={styles.weightTitle}>WEIGHT TRACKING</Text>
              <View style={styles.weightCards}>
                <TouchableOpacity style={styles.weightCard} onPress={() => openWeightModal('current')}>
                  <Text style={styles.weightLabel}>Current Weight</Text>
                  <Text style={styles.weightValue}>{currentWeight && currentWeight.trim() && currentWeight !== '.' ? `${currentWeight} lbs` : 'N/A'}</Text>
                  <Text style={styles.weightArrow}>&gt;</Text>
                </TouchableOpacity>
                <TouchableOpacity style={styles.weightCard} onPress={() => openWeightModal('goal')}>
                  <Text style={styles.weightLabel}>Goal Weight</Text>
                  <Text style={styles.weightValue}>{goalWeight && goalWeight.trim() && goalWeight !== '.' ? `${goalWeight} lbs` : 'N/A'}</Text>
                  <Text style={styles.weightArrow}>&gt;</Text>
                </TouchableOpacity>
              </View>
              {currentWeight && goalWeight && (
                <View style={styles.weightProgress}>
                  <Text style={styles.weightProgressLabel}>Progress to Goal</Text>
                  <View style={styles.weightProgressBar}>
                    <View style={[styles.weightProgressFill, {
                      width: (() => {
                        if (!currentWeight || !goalWeight) return '0%';

                        const current = parseFloat(currentWeight);
                        const goal = parseFloat(goalWeight);

                        if (isNaN(current) || isNaN(goal) || current <= 0) return '0%';

                        const percentage = ((current - goal) / current) * 100;
                        if (isNaN(percentage)) return '0%';

                        return `${Math.min(100, Math.max(0, percentage))}%`;
                      })()
                    }]} />
                  </View>
                  <Text style={styles.weightProgressText}>
                    {(() => {
                      if (!currentWeight || !goalWeight) return 'Set both weights to see progress';

                      const current = parseFloat(currentWeight);
                      const goal = parseFloat(goalWeight);

                      if (isNaN(current) || isNaN(goal) || current <= 0 || goal <= 0) {
                        return 'Set valid weights to see progress';
                      }

                      const difference = Math.abs(current - goal);
                      if (isNaN(difference)) return 'Set valid weights to see progress';

                      const diffText = difference.toFixed(1);
                      if (!diffText || diffText === '.' || isNaN(parseFloat(diffText))) {
                        return 'Set valid weights to see progress';
                      }

                      return current > goal
                        ? `${diffText} lbs to lose`
                        : `${diffText} lbs to gain`;
                    })()}
                  </Text>
                </View>
              )}
            </View>

            {/* Highlighted Nutrients Section */}
            <View style={styles.nutrientsSection}>
              <Text style={styles.nutrientsTitle}>HIGHLIGHTED NUTRIENTS</Text>
              
              <View style={styles.nutrientsGrid}>
                <View style={styles.nutrientCard}>
                  <Text style={styles.nutrientLabel}>Fiber</Text>
                  <Text style={styles.nutrientPercentage}>0%</Text>
                </View>
                <View style={styles.nutrientCard}>
                  <Text style={styles.nutrientLabel}>Iron</Text>
                  <Text style={styles.nutrientPercentage}>0%</Text>
                </View>
                <View style={styles.nutrientCard}>
                  <Text style={styles.nutrientLabel}>Calcium</Text>
                  <Text style={styles.nutrientPercentage}>0%</Text>
                </View>
                <View style={styles.nutrientCard}>
                  <Text style={styles.nutrientLabel}>Vitamin A</Text>
                  <Text style={styles.nutrientPercentage}>0%</Text>
                </View>
                <View style={styles.nutrientCard}>
                  <Text style={styles.nutrientLabel}>Vitamin C</Text>
                  <Text style={styles.nutrientPercentage}>0%</Text>
                </View>
                <View style={styles.nutrientCard}>
                  <Text style={styles.nutrientLabel}>B12</Text>
                  <Text style={styles.nutrientPercentage}>0%</Text>
                </View>
                <View style={styles.nutrientCard}>
                  <Text style={styles.nutrientLabel}>Folate</Text>
                  <Text style={styles.nutrientPercentage}>0%</Text>
                </View>
                <View style={styles.nutrientCard}>
                  <Text style={styles.nutrientLabel}>Potassium</Text>
                  <Text style={styles.nutrientPercentage}>0%</Text>
                </View>
              </View>
            </View>

            {/* Water Intake Section */}
            <View style={styles.waterSection}>
              <View style={styles.waterHeader}>
                <Text style={styles.waterLabel}>Water</Text>
                <Text style={styles.waterAmount}>0 / 64 fl oz</Text>
                <TouchableOpacity>
                  <Text style={styles.waterArrow}>v</Text>
                </TouchableOpacity>
              </View>
              <View style={styles.waterProgress}>
                <View style={[styles.waterProgressBar, { width: '0%' }]} />
              </View>
            </View>


            {/* Meal Categories Section */}
            <View style={styles.mealsSection}>
              <View style={styles.mealCategory}>
                <TouchableOpacity 
                  style={styles.mealCategoryHeader} 
                  onPress={() => {
                    console.log('Breakfast button pressed!');
                    openAddFoodModal('Breakfast');
                  }}
                  activeOpacity={0.7}
                >
                  <Text style={styles.mealCategoryIcon}>+</Text>
                  <Text style={styles.mealCategoryName}>Breakfast</Text>
                  <Text style={styles.mealCategoryArrow}>v</Text>
                </TouchableOpacity>
                
                {/* Display saved foods for Breakfast */}
                {savedFoods.Breakfast.map((food) => (
                  <View key={food.id} style={styles.foodItem}>
                    <View style={styles.foodIcon}>
                      <Text style={styles.foodIconText}>🍳</Text>
                    </View>
                    <View style={styles.foodDetails}>
                      <Text style={styles.foodItemName}>{food.name}</Text>
                      <Text style={styles.foodItemQuantity}>{food.quantity}</Text>
                      <View style={styles.nutritionGrid}>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Calories</Text>
                          <Text style={styles.nutritionValue}>{food.calories || 'N/A'}</Text>
                        </View>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Protein</Text>
                          <Text style={styles.nutritionValue}>{food.protein || 'N/A'}g</Text>
                        </View>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Carbs</Text>
                          <Text style={styles.nutritionValue}>{food.carbs || 'N/A'}g</Text>
                        </View>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Fat</Text>
                          <Text style={styles.nutritionValue}>{food.fat || 'N/A'}g</Text>
                        </View>
                      </View>
                    </View>
                  </View>
                ))}
              </View>

              <View style={styles.mealCategory}>
                <TouchableOpacity style={styles.mealCategoryHeader} onPress={() => openAddFoodModal('Lunch')}>
                  <Text style={styles.mealCategoryIcon}>+</Text>
                  <Text style={styles.mealCategoryName}>Lunch</Text>
                  <Text style={styles.mealCategoryArrow}>v</Text>
                </TouchableOpacity>
                
                {/* Display saved foods for Lunch */}
                {savedFoods.Lunch.map((food) => (
                  <View key={food.id} style={styles.foodItem}>
                    <View style={styles.foodIcon}>
                      <Text style={styles.foodIconText}>🥪</Text>
                    </View>
                    <View style={styles.foodDetails}>
                      <Text style={styles.foodItemName}>{food.name}</Text>
                      <Text style={styles.foodItemQuantity}>{food.quantity}</Text>
                      <View style={styles.nutritionGrid}>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Calories</Text>
                          <Text style={styles.nutritionValue}>{food.calories || 'N/A'}</Text>
                        </View>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Protein</Text>
                          <Text style={styles.nutritionValue}>{food.protein || 'N/A'}g</Text>
                        </View>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Carbs</Text>
                          <Text style={styles.nutritionValue}>{food.carbs || 'N/A'}g</Text>
                        </View>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Fat</Text>
                          <Text style={styles.nutritionValue}>{food.fat || 'N/A'}g</Text>
                        </View>
                      </View>
                    </View>
                  </View>
                ))}
              </View>

              <View style={styles.mealCategory}>
                <TouchableOpacity style={styles.mealCategoryHeader} onPress={() => openAddFoodModal('Dinner')}>
                  <Text style={styles.mealCategoryIcon}>+</Text>
                  <Text style={styles.mealCategoryName}>Dinner</Text>
                  <Text style={styles.mealCategoryArrow}>v</Text>
                </TouchableOpacity>
                
                {/* Display saved foods for Dinner */}
                {savedFoods.Dinner.map((food) => (
                  <View key={food.id} style={styles.foodItem}>
                    <View style={styles.foodIcon}>
                      <Text style={styles.foodIconText}>🍽️</Text>
                    </View>
                    <View style={styles.foodDetails}>
                      <Text style={styles.foodItemName}>{food.name}</Text>
                      <Text style={styles.foodItemQuantity}>{food.quantity}</Text>
                      <View style={styles.nutritionGrid}>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Calories</Text>
                          <Text style={styles.nutritionValue}>{food.calories || 'N/A'}</Text>
                        </View>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Protein</Text>
                          <Text style={styles.nutritionValue}>{food.protein || 'N/A'}g</Text>
                        </View>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Carbs</Text>
                          <Text style={styles.nutritionValue}>{food.carbs || 'N/A'}g</Text>
                        </View>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Fat</Text>
                          <Text style={styles.nutritionValue}>{food.fat || 'N/A'}g</Text>
                        </View>
                      </View>
                    </View>
                  </View>
                ))}
              </View>

              <View style={styles.mealCategory}>
                <TouchableOpacity style={styles.mealCategoryHeader} onPress={() => openAddFoodModal('Snacks')}>
                  <Text style={styles.mealCategoryIcon}>+</Text>
                  <Text style={styles.mealCategoryName}>Snacks</Text>
                  <Text style={styles.mealCategoryArrow}>v</Text>
                </TouchableOpacity>
                
                {/* Display saved foods for Snacks */}
                {savedFoods.Snacks.map((food) => (
                  <View key={food.id} style={styles.foodItem}>
                    <View style={styles.foodIcon}>
                      <Text style={styles.foodIconText}>🍿</Text>
                    </View>
                    <View style={styles.foodDetails}>
                      <Text style={styles.foodItemName}>{food.name}</Text>
                      <Text style={styles.foodItemQuantity}>{food.quantity}</Text>
                      <View style={styles.nutritionGrid}>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Calories</Text>
                          <Text style={styles.nutritionValue}>{food.calories || 'N/A'}</Text>
                        </View>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Protein</Text>
                          <Text style={styles.nutritionValue}>{food.protein || 'N/A'}g</Text>
                        </View>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Carbs</Text>
                          <Text style={styles.nutritionValue}>{food.carbs || 'N/A'}g</Text>
                        </View>
                        <View style={styles.nutritionItem}>
                          <Text style={styles.nutritionLabel}>Fat</Text>
                          <Text style={styles.nutritionValue}>{food.fat || 'N/A'}g</Text>
                        </View>
                      </View>
                    </View>
                  </View>
                ))}
              </View>
            </View>

            {/* COPD-Specific Tracking Section */}
            <View style={styles.copdTrackingSection}>
              <Text style={styles.copdTrackingTitle}>COPD HEALTH TRACKING</Text>
              
              <View style={styles.copdMetrics}>
                <View style={styles.copdMetricCard}>
                  <Text style={styles.copdMetricLabel}>Oxygen Saturation</Text>
                  <Text style={styles.copdMetricValue}>{getLatestOxygenReading()}</Text>
                  <TouchableOpacity style={styles.copdMetricButton} onPress={openOxygenModal}>
                    <Text style={styles.copdMetricButtonText}>Log Reading</Text>
                  </TouchableOpacity>
                  
                  {/* Display today's oxygen readings */}
                  {dailyOxygenReadings.length > 0 && (
                    <View style={styles.oxygenReadingsList}>
                      <Text style={styles.readingsTitle}>Today's Readings ({dailyOxygenReadings.length})</Text>
                      {dailyOxygenReadings.map((reading) => (
                        <View key={reading.id} style={styles.readingItem}>
                          <Text style={styles.readingLevel}>{reading.level}%</Text>
                          <Text style={styles.readingTime}>{reading.timestamp}</Text>
                        </View>
                      ))}
                    </View>
                  )}
                </View>

                <View style={styles.copdMetricCard}>
                  <Text style={styles.copdMetricLabel}>Exercise Minutes</Text>
                  <Text style={styles.copdMetricValue}>{getTotalExerciseMinutes()} min</Text>
                  <TouchableOpacity style={styles.copdMetricButton} onPress={openExerciseModal}>
                    <Text style={styles.copdMetricButtonText}>Log Exercise</Text>
                  </TouchableOpacity>
                  
                  {/* Display logged exercises */}
                  {dailyExercises.length > 0 && (
                    <View style={styles.exerciseList}>
                      {dailyExercises.map((exercise) => (
                        <View key={exercise.id} style={styles.exerciseItem}>
                          <Text style={styles.exerciseType}>{exercise.type}</Text>
                          <Text style={styles.exerciseMinutes}>{exercise.minutes} min</Text>
                        </View>
                      ))}
                    </View>
                  )}
                </View>

              </View>
            </View>

            {/* Quick Add Button */}
            <View style={styles.quickAddSection}>
              <TouchableOpacity style={styles.quickAddButton}>
                <Text style={styles.quickAddIcon}>+</Text>
                <Text style={styles.quickAddText}>Quick Add Food</Text>
              </TouchableOpacity>
            </View>
          </ScrollView>
        );
      case 'recipes':
        return (
          <ScrollView style={styles.recipesContainer} showsVerticalScrollIndicator={false}>
            {/* Recipes Header */}
            <View style={styles.recipesHeader}>
              <Text style={styles.recipesTitle}>COPD-Friendly Recipes</Text>
              <Text style={styles.recipesSubtitle}>
                Nutritious and delicious recipes that are easy to prepare and gentle on your respiratory system.
              </Text>
            </View>

            {/* Recipe Grid */}
            <View style={styles.recipeGrid}>
              {/* Mediterranean Bowl */}
              <View style={styles.recipeCard}>
                <View style={styles.recipeImagePlaceholder}>
                  <Text style={styles.recipeImageText}>🍲</Text>
                </View>
                <View style={styles.recipeContent}>
                  <Text style={styles.recipeName}>Mediterranean Bowl</Text>
                  <Text style={styles.recipeDescription}>
                    A nutrient-rich bowl with quinoa, roasted vegetables, chickpeas, and a light lemon-herb dressing.
                  </Text>
                  <View style={styles.recipeTags}>
                    <View style={styles.tag}>
                      <Text style={styles.tagText}>High Protein</Text>
                    </View>
                    <View style={styles.tag}>
                      <Text style={styles.tagText}>Anti-inflammatory</Text>
                    </View>
                  </View>
                </View>
              </View>

              {/* Berry Smoothie Bowl */}
              <View style={styles.recipeCard}>
                <View style={styles.recipeImagePlaceholder}>
                  <Text style={styles.recipeImageText}>🥤</Text>
                </View>
                <View style={styles.recipeContent}>
                  <Text style={styles.recipeName}>Berry Smoothie Bowl</Text>
                  <Text style={styles.recipeDescription}>
                    A refreshing blend of mixed berries, banana, yogurt, and a touch of honey, topped with granola.
                  </Text>
                  <View style={styles.recipeTags}>
                    <View style={styles.tag}>
                      <Text style={styles.tagText}>Antioxidant-Rich</Text>
                    </View>
                    <View style={styles.tag}>
                      <Text style={styles.tagText}>Easy to Digest</Text>
                    </View>
                  </View>
                </View>
              </View>

              {/* Hearty Vegetable Soup */}
              <View style={styles.recipeCard}>
                <View style={styles.recipeImagePlaceholder}>
                  <Text style={styles.recipeImageText}>🥣</Text>
                </View>
                <View style={styles.recipeContent}>
                  <Text style={styles.recipeName}>Hearty Vegetable Soup</Text>
                  <Text style={styles.recipeDescription}>
                    A warming soup with seasonal vegetables, herbs, and a small amount of lean protein.
                  </Text>
                  <View style={styles.recipeTags}>
                    <View style={styles.tag}>
                      <Text style={styles.tagText}>Hydrating</Text>
                    </View>
                    <View style={styles.tag}>
                      <Text style={styles.tagText}>Nutrient-Dense</Text>
                    </View>
                  </View>
                </View>
              </View>

              {/* Baked Salmon with Vegetables */}
              <View style={styles.recipeCard}>
                <View style={styles.recipeImagePlaceholder}>
                  <Text style={styles.recipeImageText}>🐟</Text>
                </View>
                <View style={styles.recipeContent}>
                  <Text style={styles.recipeName}>Baked Salmon with Vegetables</Text>
                  <Text style={styles.recipeDescription}>
                    Omega-3 rich salmon baked with a variety of colorful vegetables and light seasoning.
                  </Text>
                  <View style={styles.recipeTags}>
                    <View style={styles.tag}>
                      <Text style={styles.tagText}>Omega-3 Rich</Text>
                    </View>
                    <View style={styles.tag}>
                      <Text style={styles.tagText}>High Protein</Text>
                    </View>
                  </View>
                </View>
              </View>

              {/* Overnight Oats */}
              <View style={styles.recipeCard}>
                <View style={styles.recipeImagePlaceholder}>
                  <Text style={styles.recipeImageText}>🥣</Text>
                </View>
                <View style={styles.recipeContent}>
                  <Text style={styles.recipeName}>Overnight Oats</Text>
                  <Text style={styles.recipeDescription}>
                    Easy-to-prepare breakfast with oats, milk or yogurt, and your choice of fruits and nuts.
                  </Text>
                  <View style={styles.recipeTags}>
                    <View style={styles.tag}>
                      <Text style={styles.tagText}>Energy-Boosting</Text>
                    </View>
                    <View style={styles.tag}>
                      <Text style={styles.tagText}>Fiber-Rich</Text>
                    </View>
                  </View>
                </View>
              </View>

              {/* Rainbow Salad */}
              <View style={styles.recipeCard}>
                <View style={styles.recipeImagePlaceholder}>
                  <Text style={styles.recipeImageText}>🥗</Text>
                </View>
                <View style={styles.recipeContent}>
                  <Text style={styles.recipeName}>Rainbow Salad</Text>
                  <Text style={styles.recipeDescription}>
                    A colorful mix of fresh vegetables with a light vinaigrette and optional grilled chicken.
                  </Text>
                  <View style={styles.recipeTags}>
                    <View style={styles.tag}>
                      <Text style={styles.tagText}>Vitamin-Rich</Text>
                    </View>
                    <View style={styles.tag}>
                      <Text style={styles.tagText}>Low-Calorie</Text>
                    </View>
                  </View>
                </View>
              </View>
            </View>

            {/* View All Recipes Button */}
            <View style={styles.viewAllContainer}>
              <TouchableOpacity style={styles.viewAllButton}>
                <Text style={styles.viewAllButtonText}>View All Recipes</Text>
              </TouchableOpacity>
            </View>
          </ScrollView>
        );
      case 'copd-tools':
        return (
          <ScrollView style={styles.toolsContainer} showsVerticalScrollIndicator={false}>
            {/* Tools Header */}
            <View style={styles.toolsHeader}>
              <Text style={styles.toolsTitle}>COPD Management Tools</Text>
              <Text style={styles.toolsSubtitle}>
                Specialized tools to help you manage your COPD effectively
              </Text>
            </View>

            {/* Quick Tools Grid */}
            <ScrollView 
              horizontal 
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.quickToolsGrid}
            >
                             <TouchableOpacity 
                 style={[styles.toolCard, selectedTool === 'severity' && styles.toolCardActive]}
                 onPress={() => setSelectedTool('severity')}
               >
                 <Text style={styles.toolIcon}>📊</Text>
                 <Text style={styles.toolName}>Severity{'\n'}Assessment</Text>
               </TouchableOpacity>
               
               <TouchableOpacity 
                 style={[styles.toolCard, selectedTool === 'exacerbation' && styles.toolCardActive]}
                 onPress={() => setSelectedTool('exacerbation')}
               >
                 <Text style={styles.toolIcon}>📋</Text>
                 <Text style={styles.toolName}>Exacerbation{'\n'}Plan</Text>
               </TouchableOpacity>
               
               <TouchableOpacity 
                 style={[styles.toolCard, selectedTool === 'pulmonary' && styles.toolCardActive]}
                 onPress={() => setSelectedTool('pulmonary')}
               >
                 <Text style={styles.toolIcon}>🏥</Text>
                 <Text style={styles.toolName}>Pulmonary{'\n'}Rehab</Text>
               </TouchableOpacity>
               
               <TouchableOpacity 
                 style={[styles.toolCard, selectedTool === 'medication' && styles.toolCardActive]}
                 onPress={() => setSelectedTool('medication')}
               >
                 <Text style={styles.toolIcon}>💊</Text>
                 <Text style={styles.toolName}>Medication{'\n'}Guide</Text>
               </TouchableOpacity>
               
               <TouchableOpacity 
                 style={[styles.toolCard, selectedTool === 'resources' && styles.toolCardActive]}
                 onPress={() => setSelectedTool('resources')}
               >
                 <Text style={styles.toolIcon}>📚</Text>
                 <Text style={styles.toolName}>Resource{'\n'}Hub</Text>
               </TouchableOpacity>
            </ScrollView>

            {/* Dynamic Content Based on Selected Tool */}
            {selectedTool === 'severity' && (
              <View style={styles.toolContent}>
                <Text style={styles.sectionTitle}>COPD Severity Assessment</Text>
                <Text style={styles.toolDescription}>
                  This tool provides an estimate of COPD severity based on your answers. It is not a substitute for professional medical assessment. Always consult with your healthcare provider for an accurate diagnosis and treatment plan.
                </Text>
                
                <View style={styles.assessmentForm}>
                  <View style={styles.formField}>
                    <Text style={styles.fieldLabel}>What is your latest FEV1 percentage? (If known)</Text>
                    <TouchableOpacity style={styles.selectField} onPress={toggleFEV1Dropdown}>
                      <Text style={styles.selectPlaceholder}>{selectedFEV1}</Text>
                      <Text style={styles.dropdownArrow}>{showFEV1Dropdown ? '^' : 'v'}</Text>
                    </TouchableOpacity>
                    {showFEV1Dropdown && (
                      <View style={styles.dropdownList}>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedFEV1('80% or higher'); setShowFEV1Dropdown(false); }}>
                          <Text style={styles.dropdownItemText}>80% or higher</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedFEV1('50-79%'); setShowFEV1Dropdown(false); }}>
                          <Text style={styles.dropdownItemText}>50-79%</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedFEV1('30-49%'); setShowFEV1Dropdown(false); }}>
                          <Text style={styles.dropdownItemText}>30-49%</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedFEV1('Less than 30%'); setShowFEV1Dropdown(false); }}>
                          <Text style={styles.dropdownItemText}>Less than 30%</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedFEV1('I don\'t know'); setShowFEV1Dropdown(false); }}>
                          <Text style={styles.dropdownItemText}>I don't know</Text>
                        </TouchableOpacity>
                      </View>
                    )}
                  </View>
                  
                  <View style={styles.formField}>
                    <Text style={styles.fieldLabel}>How many times have you been hospitalized for COPD in the past year?</Text>
                    <TouchableOpacity style={styles.selectField} onPress={toggleHospitalizationDropdown}>
                      <Text style={styles.selectPlaceholder}>{selectedHospitalizations}</Text>
                      <Text style={styles.dropdownArrow}>{showHospitalizationDropdown ? '^' : 'v'}</Text>
                    </TouchableOpacity>
                    {showHospitalizationDropdown && (
                      <View style={styles.dropdownList}>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedHospitalizations('0'); setShowHospitalizationDropdown(false); }}>
                          <Text style={styles.dropdownItemText}>0</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedHospitalizations('1'); setShowHospitalizationDropdown(false); }}>
                          <Text style={styles.dropdownItemText}>1</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedHospitalizations('2'); setShowHospitalizationDropdown(false); }}>
                          <Text style={styles.dropdownItemText}>2</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedHospitalizations('3 or more'); setShowHospitalizationDropdown(false); }}>
                          <Text style={styles.dropdownItemText}>3 or more</Text>
                        </TouchableOpacity>
                      </View>
                    )}
                  </View>
                  
                  <View style={styles.formField}>
                    <Text style={styles.fieldLabel}>How many COPD flare-ups (exacerbations) have you had in the past year?</Text>
                    <TouchableOpacity style={styles.selectField} onPress={toggleFlareupDropdown}>
                      <Text style={styles.selectPlaceholder}>{selectedFlareups}</Text>
                      <Text style={styles.dropdownArrow}>{showFlareupDropdown ? '^' : 'v'}</Text>
                    </TouchableOpacity>
                    {showFlareupDropdown && (
                      <View style={styles.dropdownList}>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedFlareups('0'); setShowFlareupDropdown(false); }}>
                          <Text style={styles.dropdownItemText}>0</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedFlareups('1'); setShowFlareupDropdown(false); }}>
                          <Text style={styles.dropdownItemText}>1</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedFlareups('2'); setShowFlareupDropdown(false); }}>
                          <Text style={styles.dropdownItemText}>2</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedFlareups('3 or more'); setShowFlareupDropdown(false); }}>
                          <Text style={styles.dropdownItemText}>3 or more</Text>
                        </TouchableOpacity>
                      </View>
                    )}
                  </View>
                  
                  <View style={styles.formField}>
                    <Text style={styles.fieldLabel}>Do you use supplemental oxygen?</Text>
                    <TouchableOpacity style={styles.selectField} onPress={toggleOxygenDropdown}>
                      <Text style={styles.selectPlaceholder}>{selectedOxygen}</Text>
                      <Text style={styles.dropdownArrow}>{showOxygenDropdown ? '^' : 'v'}</Text>
                    </TouchableOpacity>
                    {showOxygenDropdown && (
                      <View style={styles.dropdownList}>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedOxygen('Yes'); setShowOxygenDropdown(false); }}>
                          <Text style={styles.dropdownItemText}>Yes</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedOxygen('No'); setShowOxygenDropdown(false); setSelectedOxygenLiters('Select LPM'); }}>
                          <Text style={styles.dropdownItemText}>No</Text>
                        </TouchableOpacity>
                      </View>
                    )}
                  </View>
                  
                  {selectedOxygen === 'Yes' && (
                    <View style={styles.formField}>
                      <Text style={styles.fieldLabel}>What is your LPM?</Text>
                      <TouchableOpacity style={styles.selectField} onPress={toggleOxygenLitersDropdown}>
                        <Text style={styles.selectPlaceholder}>{selectedOxygenLiters}</Text>
                        <Text style={styles.dropdownArrow}>{showOxygenLitersDropdown ? '^' : 'v'}</Text>
                      </TouchableOpacity>
                      {showOxygenLitersDropdown && (
                        <View style={styles.dropdownList}>
                          <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedOxygenLiters('1 LPM'); setShowOxygenLitersDropdown(false); }}>
                            <Text style={styles.dropdownItemText}>1 LPM</Text>
                          </TouchableOpacity>
                          <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedOxygenLiters('2 LPM'); setShowOxygenLitersDropdown(false); }}>
                            <Text style={styles.dropdownItemText}>2 LPM</Text>
                          </TouchableOpacity>
                          <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedOxygenLiters('3 LPM'); setShowOxygenLitersDropdown(false); }}>
                            <Text style={styles.dropdownItemText}>3 LPM</Text>
                          </TouchableOpacity>
                          <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedOxygenLiters('4 LPM'); setShowOxygenLitersDropdown(false); }}>
                            <Text style={styles.dropdownItemText}>4 LPM</Text>
                          </TouchableOpacity>
                          <TouchableOpacity style={styles.dropdownItem} onPress={() => { setSelectedOxygenLiters('5+ LPM'); setShowOxygenLitersDropdown(false); }}>
                            <Text style={styles.dropdownItemText}>5+ LPM</Text>
                          </TouchableOpacity>
                        </View>
                      )}
                    </View>
                  )}
                </View>
                
                <TouchableOpacity style={styles.assessmentButton}>
                  <Text style={styles.assessmentButtonText}>Assess COPD Severity</Text>
                </TouchableOpacity>
              </View>
            )}

            {selectedTool === 'exacerbation' && (
              <View style={styles.toolContent}>
                <Text style={styles.sectionTitle}>COPD Exacerbation Action Plan</Text>
                <Text style={styles.toolDescription}>
                  This action plan should be created in partnership with your healthcare provider. Use this template to document your personalized plan for managing COPD flare-ups.
                </Text>
                
                {/* Important Contacts Section */}
                <View style={styles.contactsSection}>
                  <Text style={styles.subsectionTitle}>Important Contacts</Text>
                  
                  <View style={styles.contactField}>
                    <Text style={styles.contactLabel}>Doctor's Name</Text>
                    <TextInput
                      style={styles.contactInput}
                      value={doctorName}
                      onChangeText={setDoctorName}
                      placeholder="Dr. Smith"
                      placeholderTextColor="#d1d5db"
                    />
                  </View>
                  
                  <View style={styles.contactField}>
                    <Text style={styles.contactLabel}>Doctor's Phone</Text>
                    <TextInput
                      style={styles.contactInput}
                      value={doctorPhone}
                      onChangeText={setDoctorPhone}
                      placeholder="(555) 123-4567"
                      placeholderTextColor="#d1d5db"
                      keyboardType="phone-pad"
                    />
                  </View>
                  
                  <View style={styles.contactField}>
                    <Text style={styles.contactLabel}>Emergency Contact Name</Text>
                    <TextInput
                      style={styles.contactInput}
                      value={emergencyContactName}
                      onChangeText={setEmergencyContactName}
                      placeholder="Jane Doe"
                      placeholderTextColor="#d1d5db"
                    />
                  </View>
                  
                  <View style={styles.contactField}>
                    <Text style={styles.contactLabel}>Emergency Contact Phone</Text>
                    <TextInput
                      style={styles.contactInput}
                      value={emergencyContactPhone}
                      onChangeText={setEmergencyContactPhone}
                      placeholder="(555) 987-6543"
                      placeholderTextColor="#d1d5db"
                      keyboardType="phone-pad"
                    />
                  </View>
                </View>

                {/* Medication Plan Section */}
                <View style={styles.medicationSection}>
                  <Text style={styles.subsectionTitle}>Medication Plan</Text>
                  
                  <View style={styles.medicationCategory}>
                    <Text style={styles.medicationCategoryTitle}>Daily Medications</Text>
                    <TouchableOpacity 
                      style={styles.addMedicationButton}
                      onPress={() => openAddMedicationModal('daily')}
                    >
                      <Text style={styles.addMedicationText}>+ Add Medication</Text>
                    </TouchableOpacity>
                    
                    {dailyMedications.length > 0 && (
                      <View style={styles.medicationHeader}>
                        <Text style={styles.medicationHeaderText}>Medication name</Text>
                        <Text style={styles.medicationHeaderText}>Dosage</Text>
                        <Text style={styles.medicationHeaderText}>How often</Text>
                        <Text style={styles.medicationHeaderText}>Action</Text>
                      </View>
                    )}
                    
                    {dailyMedications.length === 0 ? (
                      <Text style={styles.noMedicationText}>No daily medications added yet.</Text>
                    ) : (
                      dailyMedications.map((medication) => (
                        <View key={medication.id} style={styles.medicationRow}>
                          <Text style={styles.medicationCell}>{medication.name}</Text>
                          <Text style={styles.medicationCell}>{medication.dosage}</Text>
                          <Text style={styles.medicationCell}>{medication.frequency}</Text>
                          <TouchableOpacity 
                            style={styles.deleteButton}
                            onPress={() => {
                              console.log('Delete button pressed for medication:', medication.id);
                              Alert.alert('Test', 'This is a test alert');
                              deleteMedication(medication.id, 'daily');
                            }}
                            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                          >
                            <Text style={styles.deleteButtonText}>X</Text>
                          </TouchableOpacity>
                        </View>
                      ))
                    )}
                  </View>
                  
                  <View style={styles.medicationCategory}>
                    <Text style={styles.medicationCategoryTitle}>Exacerbation Medications</Text>
                    <TouchableOpacity 
                      style={styles.addMedicationButton}
                      onPress={() => openAddMedicationModal('exacerbation')}
                    >
                      <Text style={styles.addMedicationText}>+ Add Medication</Text>
                    </TouchableOpacity>
                    
                    {exacerbationMedications.length > 0 && (
                      <View style={styles.medicationHeader}>
                        <Text style={styles.medicationHeaderText}>Medication name</Text>
                        <Text style={styles.medicationHeaderText}>Dosage</Text>
                        <Text style={styles.medicationHeaderText}>How often</Text>
                        <Text style={styles.medicationHeaderText}>Action</Text>
                      </View>
                    )}
                    
                    {exacerbationMedications.length === 0 ? (
                      <Text style={styles.noMedicationText}>No exacerbation medications added yet.</Text>
                    ) : (
                      exacerbationMedications.map((medication) => (
                        <View key={medication.id} style={styles.medicationRow}>
                          <Text style={styles.medicationCell}>{medication.name}</Text>
                          <Text style={styles.medicationCell}>{medication.dosage}</Text>
                          <Text style={styles.medicationCell}>{medication.frequency}</Text>
                          <TouchableOpacity 
                            style={styles.deleteButton}
                            onPress={() => {
                              console.log('Delete button pressed for exacerbation medication:', medication.id);
                              deleteMedication(medication.id, 'exacerbation');
                            }}
                            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                          >
                            <Text style={styles.deleteButtonText}>X</Text>
                          </TouchableOpacity>
                        </View>
                      ))
                    )}
                  </View>
                </View>

                {/* Action Plan Zones Section */}
                <View style={styles.zonesSection}>
                  <Text style={styles.subsectionTitle}>Action Plan Zones</Text>
                  
                  <View style={[styles.zoneCard, { borderLeftColor: '#10b981' }]}>
                    <View style={styles.zoneHeader}>
                      <View style={[styles.zoneIndicator, { backgroundColor: '#10b981' }]} />
                      <Text style={styles.zoneTitle}>Green Zone: I'm Doing Well</Text>
                    </View>
                    <View style={styles.zoneSymptoms}>
                      <Text style={styles.zoneSymptom}>• Usual activity and exercise level</Text>
                      <Text style={styles.zoneSymptom}>• Usual amounts of cough and phlegm/mucus</Text>
                      <Text style={styles.zoneSymptom}>• Sleep well at night</Text>
                      <Text style={styles.zoneSymptom}>• Appetite is good</Text>
                    </View>
                    <Text style={styles.zoneAction}>Action: Take daily medications as prescribed</Text>
                  </View>
                  
                  <View style={[styles.zoneCard, { borderLeftColor: '#f59e0b' }]}>
                    <View style={styles.zoneHeader}>
                      <View style={[styles.zoneIndicator, { backgroundColor: '#f59e0b' }]} />
                      <Text style={styles.zoneTitle}>Yellow Zone: I'm Having a Bad Day</Text>
                    </View>
                    <View style={styles.zoneSymptoms}>
                      <Text style={styles.zoneSymptom}>• More breathless than usual</Text>
                      <Text style={styles.zoneSymptom}>• I have less energy for my daily activities</Text>
                      <Text style={styles.zoneSymptom}>• Increased or thicker phlegm/mucus</Text>
                      <Text style={styles.zoneSymptom}>• Using quick relief inhaler/nebulizer more often</Text>
                      <Text style={styles.zoneSymptom}>• Swelling of ankles more than usual</Text>
                      <Text style={styles.zoneSymptom}>• More coughing than usual</Text>
                      <Text style={styles.zoneSymptom}>• I feel like I have a cold</Text>
                      <Text style={styles.zoneSymptom}>• I'm not sleeping well</Text>
                      <Text style={styles.zoneSymptom}>• My appetite is not good</Text>
                    </View>
                    <Text style={styles.zoneAction}>Action: Continue daily medication and start exacerbation medications as prescribed</Text>
                  </View>
                  
                  <View style={[styles.zoneCard, { borderLeftColor: '#ef4444' }]}>
                    <View style={styles.zoneHeader}>
                      <View style={[styles.zoneIndicator, { backgroundColor: '#ef4444' }]} />
                      <Text style={styles.zoneTitle}>Red Zone: I Need Urgent Medical Care</Text>
                    </View>
                    <View style={styles.zoneSymptoms}>
                      <Text style={styles.zoneSymptom}>• Severe shortness of breath, even at rest</Text>
                      <Text style={styles.zoneSymptom}>• Not able to do any activity because of breathing</Text>
                      <Text style={styles.zoneSymptom}>• Not able to sleep because of breathing</Text>
                      <Text style={styles.zoneSymptom}>• Fever or shaking chills</Text>
                      <Text style={styles.zoneSymptom}>• Feeling confused or very drowsy</Text>
                      <Text style={styles.zoneSymptom}>• Chest pains</Text>
                      <Text style={styles.zoneSymptom}>• Coughing up blood</Text>
                    </View>
                    <Text style={styles.zoneAction}>Action: Call 911 or have someone take you to the emergency room</Text>
                  </View>
                </View>

                {/* Additional Instructions Section */}
                <View style={styles.instructionsSection}>
                  <Text style={styles.subsectionTitle}>Additional Instructions from Your Doctor</Text>
                  <TextInput
                    style={styles.instructionsTextInput}
                    value={additionalInstructions}
                    onChangeText={setAdditionalInstructions}
                    placeholder="Enter any specific instructions from your healthcare provider..."
                    placeholderTextColor="#d1d5db"
                    multiline={true}
                    numberOfLines={4}
                  />
                </View>

                {/* Action Buttons */}
                <View style={styles.actionButtons}>
                  <TouchableOpacity style={styles.saveButton} onPress={saveActionPlan}>
                    <Text style={styles.saveButtonText}>Save Action Plan</Text>
                  </TouchableOpacity>
                </View>
              </View>
            )}


            {selectedTool === 'pulmonary' && (
              <ScrollView style={styles.pulmonaryContainer} showsVerticalScrollIndicator={false} nestedScrollEnabled={true}>
                {/* Pulmonary Rehabilitation Header */}
                <View style={styles.pulmonaryHeader}>
                  <Text style={styles.pulmonaryTitle}>Pulmonary Rehabilitation</Text>
                  <Text style={styles.pulmonarySubtitle}>
                    Pulmonary rehabilitation is a comprehensive program that combines exercise, education, and support to help people with COPD breathe better, get stronger, and improve their quality of life. Always consult with your healthcare provider before starting any exercise program.
                  </Text>
                </View>

                {/* Benefits Section */}
                <View style={styles.benefitsSection}>
                  <Text style={styles.sectionTitle}>Benefits of Pulmonary Rehabilitation</Text>
                  
                  <View style={styles.benefitCard}>
                    <Text style={styles.benefitTitle}>Improved Exercise Capacity</Text>
                    <Text style={styles.benefitText}>Pulmonary rehabilitation can help you walk further and perform daily activities with less breathlessness.</Text>
                  </View>
                  
                  <View style={styles.benefitCard}>
                    <Text style={styles.benefitTitle}>Better Quality of Life</Text>
                    <Text style={styles.benefitText}>Many people report feeling better overall and having more energy for the activities they enjoy.</Text>
                  </View>
                  
                  <View style={styles.benefitCard}>
                    <Text style={styles.benefitTitle}>Reduced Hospital Admissions</Text>
                    <Text style={styles.benefitText}>Regular participation in pulmonary rehabilitation can reduce your risk of COPD exacerbations requiring hospitalization.</Text>
                  </View>
                  
                  <View style={styles.benefitCard}>
                    <Text style={styles.benefitTitle}>Increased Strength</Text>
                    <Text style={styles.benefitText}>Strengthening exercises help counter muscle loss that often occurs with COPD and improve your ability to perform daily tasks.</Text>
                  </View>
                  
                  <View style={styles.benefitCard}>
                    <Text style={styles.benefitTitle}>Better Breathing Control</Text>
                    <Text style={styles.benefitText}>Learning proper breathing techniques helps you manage breathlessness during activities and reduce anxiety.</Text>
                  </View>
                  
                  <View style={styles.benefitCard}>
                    <Text style={styles.benefitTitle}>Social Support</Text>
                    <Text style={styles.benefitText}>Meeting others with similar conditions provides emotional support and motivation to maintain your exercise program.</Text>
                  </View>
                </View>

                {/* Finding Programs Section */}
                <View style={styles.findingProgramsSection}>
                  <Text style={styles.sectionTitle}>Finding a Pulmonary Rehabilitation Program</Text>
                  <Text style={styles.findingProgramsText}>
                    Pulmonary rehabilitation programs are typically offered at hospitals, outpatient clinics, or community centers. To find a program near you:
                  </Text>
                  
                  <View style={styles.programSteps}>
                    <Text style={styles.programStep}>• Ask your pulmonologist or primary care physician for a referral</Text>
                    <Text style={styles.programStep}>• Contact your local hospital or lung health association</Text>
                    <Text style={styles.programStep}>• Check with your insurance provider for covered programs</Text>
                    <Text style={styles.programStep}>• Visit the American Lung Association website for program directories</Text>
                  </View>
                  
                  <TouchableOpacity style={styles.findProgramsButton} onPress={() => setCurrentScreen('programs')}>
                    <Text style={styles.findProgramsButtonText}>Find Programs Near Me</Text>
                  </TouchableOpacity>
                </View>

                {/* Home Exercise Program Section */}
                <View style={styles.homeExerciseSection}>
                  <Text style={styles.sectionTitle}>Home Exercise Program</Text>
                  <Text style={styles.homeExerciseIntro}>
                    While a supervised pulmonary rehabilitation program is ideal, these exercises can be performed at home to complement your program or when a formal program isn't available.
                  </Text>

                  {/* Breathing Exercises */}
                  <View style={styles.exerciseCategory}>
                    <Text style={styles.exerciseCategoryTitle}>Breathing Exercises</Text>
                    <Text style={styles.exerciseCategorySubtitle}>Techniques to improve breathing efficiency and control</Text>
                    
                    <View style={styles.exerciseCard}>
                      <Text style={styles.exerciseName}>Pursed-Lip Breathing</Text>
                      <Text style={styles.exerciseDescription}>
                        Breathe in through your nose for 2 counts, then breathe out slowly through pursed lips for 4 counts. This helps control breathlessness and slows your breathing rate.
                      </Text>
                      <Text style={styles.exerciseFrequency}>Recommended frequency: 5-10 minutes, 4-5 times daily</Text>
                    </View>
                    
                    <View style={styles.exerciseCard}>
                      <Text style={styles.exerciseName}>Diaphragmatic Breathing</Text>
                      <Text style={styles.exerciseDescription}>
                        Place one hand on your chest and the other on your abdomen. Breathe in through your nose, feeling your abdomen rise. Breathe out through pursed lips while gently pressing on your abdomen.
                      </Text>
                      <Text style={styles.exerciseFrequency}>Recommended frequency: 5-10 minutes, 3-4 times daily</Text>
                    </View>
                    
                    <View style={styles.exerciseCard}>
                      <Text style={styles.exerciseName}>Segmental Breathing</Text>
                      <Text style={styles.exerciseDescription}>
                        Focus on directing air to different parts of your lungs by placing hands on specific areas of your chest or sides while breathing deeply.
                      </Text>
                      <Text style={styles.exerciseFrequency}>Recommended frequency: 5 minutes, 2-3 times daily</Text>
                    </View>
                  </View>

                  {/* Endurance Training */}
                  <View style={styles.exerciseCategory}>
                    <Text style={styles.exerciseCategoryTitle}>Endurance Training</Text>
                    <Text style={styles.exerciseCategorySubtitle}>Activities to improve cardiovascular fitness and stamina</Text>
                    
                    <View style={styles.exerciseCard}>
                      <Text style={styles.exerciseName}>Walking</Text>
                      <Text style={styles.exerciseDescription}>
                        Start with short distances and gradually increase. Use pursed-lip breathing while walking. Stop and rest if you become too breathless.
                      </Text>
                      <Text style={styles.exerciseFrequency}>Recommended frequency: Start with 5-10 minutes daily, gradually increase to 20-30 minutes</Text>
                    </View>
                    
                    <View style={styles.exerciseCard}>
                      <Text style={styles.exerciseName}>Stationary Cycling</Text>
                      <Text style={styles.exerciseDescription}>
                        Adjust resistance to a comfortable level. Maintain good posture and use pursed-lip breathing.
                      </Text>
                      <Text style={styles.exerciseFrequency}>Recommended frequency: Start with 5-10 minutes daily, gradually increase to 15-20 minutes</Text>
                    </View>
                    
                    <View style={styles.exerciseCard}>
                      <Text style={styles.exerciseName}>Swimming/Water Exercises</Text>
                      <Text style={styles.exerciseDescription}>
                        The buoyancy of water supports your body, making movement easier. The humidity can also help your breathing.
                      </Text>
                      <Text style={styles.exerciseFrequency}>Recommended frequency: 20-30 minutes, 2-3 times weekly</Text>
                    </View>
                  </View>

                  {/* Strength Training */}
                  <View style={styles.exerciseCategory}>
                    <Text style={styles.exerciseCategoryTitle}>Strength Training</Text>
                    <Text style={styles.exerciseCategorySubtitle}>Exercises to strengthen respiratory and peripheral muscles</Text>
                    
                    <View style={styles.exerciseCard}>
                      <Text style={styles.exerciseName}>Upper Body Strengthening</Text>
                      <Text style={styles.exerciseDescription}>
                        Use light weights or resistance bands for arm raises, bicep curls, and shoulder presses. Focus on proper breathing throughout.
                      </Text>
                      <Text style={styles.exerciseFrequency}>Recommended frequency: 8-12 repetitions, 2-3 sets, 2-3 times weekly</Text>
                    </View>
                    
                    <View style={styles.exerciseCard}>
                      <Text style={styles.exerciseName}>Lower Body Strengthening</Text>
                      <Text style={styles.exerciseDescription}>
                        Perform chair stands, leg extensions, and calf raises to strengthen legs. These help with daily activities like standing and walking.
                      </Text>
                      <Text style={styles.exerciseFrequency}>Recommended frequency: 8-12 repetitions, 2-3 sets, 2-3 times weekly</Text>
                    </View>
                    
                    <View style={styles.exerciseCard}>
                      <Text style={styles.exerciseName}>Core Strengthening</Text>
                      <Text style={styles.exerciseDescription}>
                        Seated abdominal contractions and gentle back extensions help improve posture and breathing mechanics.
                      </Text>
                      <Text style={styles.exerciseFrequency}>Recommended frequency: 8-12 repetitions, 2-3 sets, 2-3 times weekly</Text>
                    </View>
                  </View>

                  <View style={styles.exerciseWarning}>
                    <Text style={styles.exerciseWarningText}>
                      Important: Always start slowly and progress gradually. Stop any exercise that causes severe shortness of breath, chest pain, or dizziness. Keep your rescue inhaler nearby during exercise.
                    </Text>
                  </View>
                </View>

                {/* Track Progress Section */}
                <View style={styles.trackProgressSection}>
                  <Text style={styles.sectionTitle}>Track Your Progress</Text>
                  <Text style={styles.trackProgressText}>
                    Keeping track of your exercise sessions helps you see your progress and stay motivated. Consider tracking:
                  </Text>
                  
                  <View style={styles.trackingItems}>
                    <Text style={styles.trackingItem}>• Exercise duration and frequency</Text>
                    <Text style={styles.trackingItem}>• Distance walked or steps taken</Text>
                    <Text style={styles.trackingItem}>• Breathlessness levels before, during, and after exercise</Text>
                    <Text style={styles.trackingItem}>• How you feel overall after each session</Text>
                  </View>
                  
                  <TouchableOpacity style={styles.startJournalButton}>
                    <Text style={styles.startJournalButtonText}>Start Exercise Journal</Text>
                  </TouchableOpacity>
                </View>
              </ScrollView>
            )}

            {selectedTool === 'medication' && (
              <ScrollView style={styles.medicationContainer} showsVerticalScrollIndicator={false} nestedScrollEnabled={true}>
                {/* Medication Guide Header */}
                <View style={styles.medicationHeader}>
                  <Text style={styles.medicationTitle}>COPD Medication Guide</Text>
                  <Text style={styles.medicationSubtitle}>
                    This guide provides general information about COPD medications. Your doctor will prescribe medications based on your specific needs. Always follow your healthcare provider's instructions about your medications.
                  </Text>
                </View>

                {/* Quick Navigation */}
                <View style={styles.medicationNavigation}>
                  <Text style={styles.navigationTitle}>Featured COPD Inhalers</Text>
                  <View style={styles.navigationButtons}>
                    <TouchableOpacity style={styles.navButton}>
                      <Text style={styles.navButtonText}>Symbicort Guide</Text>
                    </TouchableOpacity>
                    <TouchableOpacity style={styles.navButton}>
                      <Text style={styles.navButtonText}>Breztri Guide</Text>
                    </TouchableOpacity>
                  </View>
                </View>

                {/* Medication Categories */}
                <View style={styles.medicationCategories}>
                  <Text style={styles.sectionTitle}>Medication Types</Text>
                  
                  <View style={styles.categoryGrid}>
                    <TouchableOpacity 
                      style={styles.categoryCard}
                      onPress={() => setExpandedMedicationCategory(expandedMedicationCategory === 'bronchodilators' ? null : 'bronchodilators')}
                    >
                      <Text style={styles.categoryIcon}>💨</Text>
                      <Text style={styles.categoryTitle}>Bronchodilators</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={styles.categoryCard}
                      onPress={() => setExpandedMedicationCategory(expandedMedicationCategory === 'ics' ? null : 'ics')}
                    >
                      <Text style={styles.categoryIcon}>🌬️</Text>
                      <Text style={styles.categoryTitle}>Inhaled Corticosteroids</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={styles.categoryCard}
                      onPress={() => setExpandedMedicationCategory(expandedMedicationCategory === 'combination' ? null : 'combination')}
                    >
                      <Text style={styles.categoryIcon}>🔄</Text>
                      <Text style={styles.categoryTitle}>Combination Inhalers</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={styles.categoryCard}
                      onPress={() => setExpandedMedicationCategory(expandedMedicationCategory === 'pde4' ? null : 'pde4')}
                    >
                      <Text style={styles.categoryIcon}>🧬</Text>
                      <Text style={styles.categoryTitle}>Phosphodiesterase-4 Inhibitors</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={styles.categoryCard}
                      onPress={() => setExpandedMedicationCategory(expandedMedicationCategory === 'antibiotics' ? null : 'antibiotics')}
                    >
                      <Text style={styles.categoryIcon}>🦠</Text>
                      <Text style={styles.categoryTitle}>Antibiotics</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={styles.categoryCard}
                      onPress={() => setExpandedMedicationCategory(expandedMedicationCategory === 'systemic' ? null : 'systemic')}
                    >
                      <Text style={styles.categoryIcon}>💊</Text>
                      <Text style={styles.categoryTitle}>Systemic Corticosteroids</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={styles.categoryCard}
                      onPress={() => setExpandedMedicationCategory(expandedMedicationCategory === 'methylxanthines' ? null : 'methylxanthines')}
                    >
                      <Text style={styles.categoryIcon}>⚪</Text>
                      <Text style={styles.categoryTitle}>Methylxanthines</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={styles.categoryCard}
                      onPress={() => setExpandedMedicationCategory(expandedMedicationCategory === 'mucolytics' ? null : 'mucolytics')}
                    >
                      <Text style={styles.categoryIcon}>💧</Text>
                      <Text style={styles.categoryTitle}>Mucolytics/Expectorants</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={styles.categoryCard}
                      onPress={() => setExpandedMedicationCategory(expandedMedicationCategory === 'biologics' ? null : 'biologics')}
                    >
                      <Text style={styles.categoryIcon}>💉</Text>
                      <Text style={styles.categoryTitle}>Biologics Medications for COPD</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={styles.categoryCard}
                      onPress={() => setExpandedMedicationCategory(expandedMedicationCategory === 'nebulizer' ? null : 'nebulizer')}
                    >
                      <Text style={styles.categoryIcon}>🫧</Text>
                      <Text style={styles.categoryTitle}>Nebulizer Medication</Text>
                    </TouchableOpacity>
                  </View>

                  {/* Bronchodilators Detailed Content */}
                  {expandedMedicationCategory === 'bronchodilators' && (
                    <View style={styles.medicationDetailCard}>
                      <Text style={styles.medicationDetailTitle}>1. Bronchodilators</Text>
                      <Text style={styles.medicationDetailDescription}>
                        These relax the muscles around the airways, making breathing easier.
                      </Text>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationSubTitle}>🔹 Short-acting bronchodilators (SABAs & SAMAs) – quick relief</Text>
                        
                        <Text style={styles.medicationTypeLabel}>SABA (Short-acting β2-agonist):</Text>
                        <Text style={styles.medicationItem}>• Albuterol (Ventolin, ProAir, Proventil)</Text>
                        <Text style={styles.medicationItem}>• Levalbuterol (Xopenex)</Text>
                        
                        <Text style={styles.medicationTypeLabel}>SAMA (Short-acting muscarinic antagonist):</Text>
                        <Text style={styles.medicationItem}>• Ipratropium (Atrovent)</Text>
                      </View>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationSubTitle}>🔹 Long-acting bronchodilators (LABAs & LAMAs) – maintenance</Text>
                        
                        <Text style={styles.medicationTypeLabel}>LABA (Long-acting β2-agonist):</Text>
                        <Text style={styles.medicationItem}>• Salmeterol (Serevent)</Text>
                        <Text style={styles.medicationItem}>• Formoterol (Foradil, Perforomist)</Text>
                        <Text style={styles.medicationItem}>• Indacaterol (Arcapta)</Text>
                        <Text style={styles.medicationItem}>• Olodaterol (Striverdi)</Text>
                        
                        <Text style={styles.medicationTypeLabel}>LAMA (Long-acting muscarinic antagonist):</Text>
                        <Text style={styles.medicationItem}>• Tiotropium (Spiriva)</Text>
                        <Text style={styles.medicationItem}>• Aclidinium (Tudorza)</Text>
                        <Text style={styles.medicationItem}>• Umeclidinium (Incruse)</Text>
                        <Text style={styles.medicationItem}>• Glycopyrrolate (Seebri Neohaler)</Text>
                      </View>
                    </View>
                  )}

                  {/* Inhaled Corticosteroids Detailed Content */}
                  {expandedMedicationCategory === 'ics' && (
                    <View style={styles.medicationDetailCard}>
                      <Text style={styles.medicationDetailTitle}>Inhaled Corticosteroids (ICS)</Text>
                      <Text style={styles.medicationDetailDescription}>
                        Used in patients with frequent exacerbations or asthma-COPD overlap.
                      </Text>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationItem}>• Fluticasone</Text>
                        <Text style={styles.medicationItem}>• Budesonide</Text>
                        <Text style={styles.medicationItem}>• Beclomethasone</Text>
                        <Text style={styles.medicationItem}>• Mometasone</Text>
                      </View>
                    </View>
                  )}

                  {/* Combination Inhalers Detailed Content */}
                  {expandedMedicationCategory === 'combination' && (
                    <View style={styles.medicationDetailCard}>
                      <Text style={styles.medicationDetailTitle}>Combination Inhalers</Text>
                      <Text style={styles.medicationDetailDescription}>
                        These combine different classes for better symptom control.
                      </Text>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationTypeLabel}>LABA + LAMA:</Text>
                        <Text style={styles.medicationItem}>• Anoro Ellipta (umeclidinium + vilanterol)</Text>
                        <Text style={styles.medicationItem}>• Stiolto Respimat (tiotropium + olodaterol)</Text>
                      </View>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationTypeLabel}>LABA + ICS (inhaled corticosteroid):</Text>
                        <Text style={styles.medicationItem}>• Advair (fluticasone + salmeterol)</Text>
                        <Text style={styles.medicationItem}>• Symbicort (budesonide + formoterol)</Text>
                        <Text style={styles.medicationItem}>• Breo Ellipta (fluticasone + vilanterol)</Text>
                      </View>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationTypeLabel}>Triple Therapy (LABA + LAMA + ICS):</Text>
                        <Text style={styles.medicationItem}>• Trelegy Ellipta (fluticasone + umeclidinium + vilanterol)</Text>
                        <Text style={styles.medicationItem}>• Breztri Aerosphere (budesonide + glycopyrrolate + formoterol)</Text>
                      </View>
                    </View>
                  )}

                  {/* Phosphodiesterase-4 Inhibitors Detailed Content */}
                  {expandedMedicationCategory === 'pde4' && (
                    <View style={styles.medicationDetailCard}>
                      <Text style={styles.medicationDetailTitle}>Phosphodiesterase-4 (PDE4) Inhibitors</Text>
                      <Text style={styles.medicationDetailDescription}>
                        Used for severe COPD with chronic bronchitis and frequent exacerbations.
                      </Text>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationItem}>• Roflumilast (Daliresp)</Text>
                      </View>
                    </View>
                  )}

                  {/* Methylxanthines Detailed Content */}
                  {expandedMedicationCategory === 'methylxanthines' && (
                    <View style={styles.medicationDetailCard}>
                      <Text style={styles.medicationDetailTitle}>Methylxanthines</Text>
                      <Text style={styles.medicationDetailDescription}>
                        Less common due to side effects. Theophylline – weak bronchodilator, used rarely now.
                      </Text>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationItem}>• Theophylline</Text>
                      </View>
                    </View>
                  )}

                  {/* Systemic Corticosteroids Detailed Content */}
                  {expandedMedicationCategory === 'systemic' && (
                    <View style={styles.medicationDetailCard}>
                      <Text style={styles.medicationDetailTitle}>Systemic Corticosteroids</Text>
                      <Text style={styles.medicationDetailDescription}>
                        Used short-term during exacerbations.
                      </Text>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationItem}>• Prednisone</Text>
                        <Text style={styles.medicationItem}>• Methylprednisolone</Text>
                      </View>
                    </View>
                  )}

                  {/* Antibiotics Detailed Content */}
                  {expandedMedicationCategory === 'antibiotics' && (
                    <View style={styles.medicationDetailCard}>
                      <Text style={styles.medicationDetailTitle}>Antibiotics</Text>
                      <Text style={styles.medicationDetailDescription}>
                        Used during exacerbations caused by bacterial infections.
                      </Text>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationItem}>• Azithromycin (sometimes used long-term to reduce exacerbations)</Text>
                        <Text style={styles.medicationItem}>• Amoxicillin-clavulanate, Doxycycline, etc.</Text>
                      </View>
                    </View>
                  )}

                  {/* Mucolytics/Expectorants Detailed Content */}
                  {expandedMedicationCategory === 'mucolytics' && (
                    <View style={styles.medicationDetailCard}>
                      <Text style={styles.medicationDetailTitle}>Mucolytics / Expectorants</Text>
                      <Text style={styles.medicationDetailDescription}>
                        Reduce mucus thickness and help with coughing it out.
                      </Text>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationItem}>• N-acetylcysteine (NAC)</Text>
                        <Text style={styles.medicationItem}>• Carbocysteine</Text>
                      </View>
                    </View>
                  )}

                  {/* Biologics Medications Detailed Content */}
                  {expandedMedicationCategory === 'biologics' && (
                    <View style={styles.medicationDetailCard}>
                      <Text style={styles.medicationDetailTitle}>Biologic Medications for COPD</Text>
                      <Text style={styles.medicationDetailDescription}>
                        Used in select COPD patients, especially those with:
                      </Text>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationItem}>• High blood eosinophil counts</Text>
                        <Text style={styles.medicationItem}>• Frequent exacerbations despite optimal inhaler therapy</Text>
                        <Text style={styles.medicationItem}>• Asthma-COPD overlap syndrome (ACOS)</Text>
                      </View>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationSubTitle}>🔹 1. Mepolizumab (Nucala)</Text>
                        <Text style={styles.medicationItem}>• Target: IL-5</Text>
                        <Text style={styles.medicationItem}>• Use: For eosinophilic inflammation</Text>
                        <Text style={styles.medicationItem}>• Dosing: 100 mg subcutaneous every 4 weeks</Text>
                        <Text style={styles.medicationItem}>• Evidence in COPD: Shown to reduce exacerbations in eosinophilic COPD in select patients.</Text>
                      </View>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationSubTitle}>🔹 2. Dupilumab (Dupixent)</Text>
                        <Text style={styles.medicationItem}>• Target: IL-4 and IL-13 pathways (type 2 inflammation)</Text>
                        <Text style={styles.medicationItem}>• Use: Asthma, atopic dermatitis, and now also FDA-approved for COPD (as of 2023)</Text>
                        <Text style={styles.medicationItem}>• Dosing: 300 mg every 2 weeks</Text>
                        <Text style={styles.medicationItem}>• COPD Use: For moderate-to-severe COPD with type 2 inflammation (e.g., elevated eosinophils); reduces exacerbations and improves lung function.</Text>
                      </View>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationSubTitle}>⚠️ Who qualifies for biologics in COPD?</Text>
                        <Text style={styles.medicationDetailDescription}>
                          Biologics are not for all COPD patients — they are usually considered after maximal inhaled therapy (LABA/LAMA/ICS) fails and if:
                        </Text>
                        <Text style={styles.medicationItem}>• Blood eosinophil count ≥ 100–300 cells/µL (depending on drug)</Text>
                        <Text style={styles.medicationItem}>• Frequent exacerbations (≥2/year or ≥1 leading to hospitalization)</Text>
                        <Text style={styles.medicationItem}>• No active smoking (some studies limit inclusion to former smokers)</Text>
                        <Text style={styles.medicationItem}>• Evidence of type 2 inflammation (eosinophils, IgE, comorbid asthma)</Text>
                      </View>
                    </View>
                  )}

                  {/* Nebulizer Medication Detailed Content */}
                  {expandedMedicationCategory === 'nebulizer' && (
                    <View style={styles.medicationDetailCard}>
                      <Text style={styles.medicationDetailTitle}>Nebulizer Medication</Text>

                      <View style={styles.medicationSubSection}>
                        <Text style={styles.medicationSubTitle}>Omalizumab (Xolair)</Text>
                        <Text style={styles.medicationItem}>• Target: IgE</Text>
                        <Text style={styles.medicationItem}>• Use: Allergic asthma, potentially asthma-COPD overlap</Text>
                        <Text style={styles.medicationItem}>• COPD Use: Not routinely used unless there's a strong allergic component or overlap with allergic asthma.</Text>
                      </View>
                    </View>
                  )}
                </View>


                {/* Watch Videos Button */}
                <View style={styles.videoSection}>
                  <TouchableOpacity style={styles.watchVideosButton}>
                    <Text style={styles.watchVideosButtonText}>Watch Inhaler Technique Videos</Text>
                  </TouchableOpacity>
                </View>
              </ScrollView>
            )}

            {selectedTool === 'resources' && (
              <ScrollView style={styles.resourcesContainer} showsVerticalScrollIndicator={false} nestedScrollEnabled={true}>
                {/* Resource Hub Header */}
                <View style={styles.resourcesHeader}>
                  <Text style={styles.resourcesTitle}>COPD Resource Hub</Text>
                  <Text style={styles.resourcesSubtitle}>
                    This resource hub provides links to trusted organizations, educational materials, and support groups to help you better understand and manage your COPD.
                  </Text>
                </View>

                {/* COPD Organizations Section */}
                <View style={styles.organizationsSection}>
                  <Text style={styles.sectionTitle}>COPD Organizations</Text>
                  
                  <View style={styles.organizationCard}>
                    <Text style={styles.organizationName}>American Lung Association</Text>
                    <Text style={styles.organizationDescription}>
                      Provides education, advocacy and research to improve lung health and prevent lung disease.
                    </Text>
                    <View style={styles.organizationActions}>
                      <TouchableOpacity style={styles.visitButton}>
                        <Text style={styles.visitButtonText}>Visit Website</Text>
                      </TouchableOpacity>
                      <Text style={styles.helplineText}>Helpline: 1-800-LUNGUSA</Text>
                    </View>
                  </View>

                  <View style={styles.organizationCard}>
                    <Text style={styles.organizationName}>COPD Foundation</Text>
                    <Text style={styles.organizationDescription}>
                      Dedicated to improving the lives of those affected by COPD through research, education, early diagnosis, and enhanced therapy.
                    </Text>
                    <View style={styles.organizationActions}>
                      <TouchableOpacity style={styles.visitButton}>
                        <Text style={styles.visitButtonText}>Visit Website</Text>
                      </TouchableOpacity>
                      <Text style={styles.helplineText}>Helpline: 1-866-316-COPD</Text>
                    </View>
                  </View>

                  <View style={styles.organizationCard}>
                    <Text style={styles.organizationName}>Global Initiative for Chronic Obstructive Lung Disease (GOLD)</Text>
                    <Text style={styles.organizationDescription}>
                      Works to improve prevention and treatment of COPD through a global network.
                    </Text>
                    <View style={styles.organizationActions}>
                      <TouchableOpacity style={styles.visitButton}>
                        <Text style={styles.visitButtonText}>Visit Website</Text>
                      </TouchableOpacity>
                    </View>
                  </View>

                  <View style={styles.organizationCard}>
                    <Text style={styles.organizationName}>National Heart, Lung, and Blood Institute</Text>
                    <Text style={styles.organizationDescription}>
                      Provides information on COPD diagnosis, management, and research.
                    </Text>
                    <View style={styles.organizationActions}>
                      <TouchableOpacity style={styles.visitButton}>
                        <Text style={styles.visitButtonText}>Visit Website</Text>
                      </TouchableOpacity>
                    </View>
                  </View>
                </View>

                {/* Educational Resources Section */}
                <View style={styles.educationalSection}>
                  <Text style={styles.sectionTitle}>Educational Resources</Text>
                  
                  <View style={styles.educationalGrid}>
                    <View style={styles.educationalCard}>
                      <Text style={styles.educationalTitle}>Understanding COPD</Text>
                      <Text style={styles.educationalType}>Guide</Text>
                      <Text style={styles.educationalDescription}>
                        Comprehensive guide to COPD causes, symptoms, diagnosis, and treatments.
                      </Text>
                      <TouchableOpacity style={styles.accessButton}>
                        <Text style={styles.accessButtonText}>Access Resource →</Text>
                      </TouchableOpacity>
                    </View>

                    <View style={styles.educationalCard}>
                      <Text style={styles.educationalTitle}>Living Well with COPD</Text>
                      <Text style={styles.educationalType}>Video Series</Text>
                      <Text style={styles.educationalDescription}>
                        Practical tips for managing daily life with COPD.
                      </Text>
                      <TouchableOpacity style={styles.accessButton}>
                        <Text style={styles.accessButtonText}>Access Resource →</Text>
                      </TouchableOpacity>
                    </View>

                    <View style={styles.educationalCard}>
                      <Text style={styles.educationalTitle}>COPD and Nutrition</Text>
                      <Text style={styles.educationalType}>Webinar</Text>
                      <Text style={styles.educationalDescription}>
                        How diet affects COPD symptoms and overall health.
                      </Text>
                      <TouchableOpacity style={styles.accessButton}>
                        <Text style={styles.accessButtonText}>Access Resource →</Text>
                      </TouchableOpacity>
                    </View>

                    <View style={styles.educationalCard}>
                      <Text style={styles.educationalTitle}>Breathing Techniques for COPD</Text>
                      <Text style={styles.educationalType}>Tutorial</Text>
                      <Text style={styles.educationalDescription}>
                        Learn effective breathing exercises to help manage breathlessness.
                      </Text>
                      <TouchableOpacity style={styles.accessButton}>
                        <Text style={styles.accessButtonText}>Access Resource →</Text>
                      </TouchableOpacity>
                    </View>

                    <View style={styles.educationalCard}>
                      <Text style={styles.educationalTitle}>Understanding Your COPD Medications</Text>
                      <Text style={styles.educationalType}>Guide</Text>
                      <Text style={styles.educationalDescription}>
                        Detailed information about different types of COPD medications and how they work.
                      </Text>
                      <TouchableOpacity style={styles.accessButton}>
                        <Text style={styles.accessButtonText}>Access Resource →</Text>
                      </TouchableOpacity>
                    </View>

                    <View style={styles.educationalCard}>
                      <Text style={styles.educationalTitle}>Pulmonary Rehabilitation: What to Expect</Text>
                      <Text style={styles.educationalType}>Article</Text>
                      <Text style={styles.educationalDescription}>
                        Overview of pulmonary rehabilitation programs and their benefits.
                      </Text>
                      <TouchableOpacity style={styles.accessButton}>
                        <Text style={styles.accessButtonText}>Access Resource →</Text>
                      </TouchableOpacity>
                    </View>
                  </View>
                </View>

                {/* Support Groups Section */}
                <View style={styles.supportGroupsSection}>
                  <Text style={styles.sectionTitle}>Support Groups</Text>
                  
                  <View style={styles.supportCard}>
                    <Text style={styles.supportName}>COPD Connect</Text>
                    <Text style={styles.supportType}>Online</Text>
                    <Text style={styles.supportDescription}>
                      Online forum for people with COPD to share experiences and advice.
                    </Text>
                    <TouchableOpacity style={styles.joinButton}>
                      <Text style={styles.joinButtonText}>Join Group →</Text>
                    </TouchableOpacity>
                  </View>

                  <View style={styles.supportCard}>
                    <Text style={styles.supportName}>Better Breathers Club</Text>
                    <Text style={styles.supportType}>In-person & Virtual</Text>
                    <Text style={styles.supportDescription}>
                      In-person and virtual support groups organized by the American Lung Association.
                    </Text>
                    <TouchableOpacity style={styles.joinButton}>
                      <Text style={styles.joinButtonText}>Join Group →</Text>
                    </TouchableOpacity>
                  </View>

                  <View style={styles.supportCard}>
                    <Text style={styles.supportName}>COPD Foundation 360social</Text>
                    <Text style={styles.supportType}>Online</Text>
                    <Text style={styles.supportDescription}>
                      Online community platform for individuals with COPD and their caregivers.
                    </Text>
                    <TouchableOpacity style={styles.joinButton}>
                      <Text style={styles.joinButtonText}>Join Group →</Text>
                    </TouchableOpacity>
                  </View>
                </View>

                {/* COVID-19 Section */}
                <View style={styles.covidSection}>
                  <Text style={styles.sectionTitle}>COVID-19 and COPD</Text>
                  <Text style={styles.covidSubtitle}>Special Considerations for COPD Patients</Text>
                  <Text style={styles.covidDescription}>
                    People with COPD may be at higher risk for severe illness from COVID-19. It's important to take extra precautions and stay updated with the latest guidance.
                  </Text>
                  
                  <View style={styles.covidGuidelines}>
                    <Text style={styles.covidGuideline}>• Continue taking your COPD medications as prescribed</Text>
                    <Text style={styles.covidGuideline}>• Maintain at least a 30-day supply of your medications</Text>
                    <Text style={styles.covidGuideline}>• Follow recommendations for vaccination</Text>
                    <Text style={styles.covidGuideline}>• Practice physical distancing and wear masks when appropriate</Text>
                    <Text style={styles.covidGuideline}>• Have an emergency action plan in case you develop COVID-19 symptoms</Text>
                  </View>
                  
                  <TouchableOpacity style={styles.covidButton}>
                    <Text style={styles.covidButtonText}>View Complete COVID-19 Guidance for COPD Patients →</Text>
                  </TouchableOpacity>
                </View>

                {/* Mobile Apps Section */}
                <View style={styles.appsSection}>
                  <Text style={styles.sectionTitle}>Recommended Mobile Apps</Text>
                  
                  <View style={styles.appCard}>
                    <Text style={styles.appName}>COPD Pocket Consultant Guide</Text>
                    <Text style={styles.appDescription}>
                      Developed by the COPD Foundation, this app provides tools for tracking symptoms, medications, and includes an action plan for exacerbations.
                    </Text>
                    <View style={styles.appButtons}>
                      <TouchableOpacity style={styles.appStoreButton}>
                        <Text style={styles.appStoreButtonText}>iOS App Store</Text>
                      </TouchableOpacity>
                      <TouchableOpacity style={styles.appStoreButton}>
                        <Text style={styles.appStoreButtonText}>Google Play</Text>
                      </TouchableOpacity>
                    </View>
                  </View>

                  <View style={styles.appCard}>
                    <Text style={styles.appName}>Breathe Well: COPD Management</Text>
                    <Text style={styles.appDescription}>
                      Track your symptoms, medications, and oxygen levels. Set medication reminders and log your pulmonary rehabilitation exercises.
                    </Text>
                    <View style={styles.appButtons}>
                      <TouchableOpacity style={styles.appStoreButton}>
                        <Text style={styles.appStoreButtonText}>iOS App Store</Text>
                      </TouchableOpacity>
                      <TouchableOpacity style={styles.appStoreButton}>
                        <Text style={styles.appStoreButtonText}>Google Play</Text>
                      </TouchableOpacity>
                    </View>
                  </View>
                </View>
              </ScrollView>
            )}
          </ScrollView>
        );
      case 'profile':
        return (
          <ScrollView style={styles.profileContainer} showsVerticalScrollIndicator={false}>
            <View style={styles.profileHeader}>
              <Text style={styles.profileTitle}>My Profile</Text>
            </View>
            <View style={styles.profileSection}>
              <TouchableOpacity style={styles.profileItem} onPress={() => openProfileModal('age')}>
                <Text style={styles.profileLabel}>Age</Text>
                <View style={styles.profileValueContainer}>
                  <Text style={styles.profileValue}>{age && age.trim() && age !== '.' ? age : 'N/A'}</Text>
                  <Text style={styles.profileArrow}>&gt;</Text>
                </View>
              </TouchableOpacity>
              <TouchableOpacity style={styles.profileItem} onPress={() => openProfileModal('sex')}>
                <Text style={styles.profileLabel}>Sex</Text>
                <View style={styles.profileValueContainer}>
                  <Text style={styles.profileValue}>{sex && sex.trim() && sex !== '.' ? sex : 'N/A'}</Text>
                  <Text style={styles.profileArrow}>&gt;</Text>
                </View>
              </TouchableOpacity>
              <TouchableOpacity style={styles.profileItem} onPress={() => openWeightModal('current')}>
                <Text style={styles.profileLabel}>Weight</Text>
                <View style={styles.profileValueContainer}>
                  <Text style={styles.profileValue}>{currentWeight && currentWeight.trim() && currentWeight !== '.' ? `${currentWeight} lbs` : 'N/A'}</Text>
                  <Text style={styles.profileArrow}>&gt;</Text>
                </View>
              </TouchableOpacity>
              <TouchableOpacity style={styles.profileItem} onPress={() => openProfileModal('height')}>
                <Text style={styles.profileLabel}>Height</Text>
                <View style={styles.profileValueContainer}>
                  <Text style={styles.profileValue}>{height && height.trim() && height !== '.' ? height : 'N/A'}</Text>
                  <Text style={styles.profileArrow}>&gt;</Text>
                </View>
              </TouchableOpacity>
              <View style={styles.profileItem}>
                <Text style={styles.profileLabel}>Body Mass Index (BMI)</Text>
                <View style={styles.profileValueContainer}>
                  <Text style={styles.profileValue}>{calculateBMI()}</Text>
                </View>
              </View>
              {lastUpdated && (
                <View style={styles.profileItem}>
                  <Text style={styles.profileLabel}>Last Updated</Text>
                  <View style={styles.profileValueContainer}>
                    <Text style={styles.profileValue}>{lastUpdated && lastUpdated.trim() && lastUpdated !== '.' ? lastUpdated : 'N/A'}</Text>
                  </View>
                </View>
              )}
            </View>
          </ScrollView>
        );
      default:
        return null;
    }
  };

  if (currentScreen === 'programs') {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="dark-content" backgroundColor="#ffffff" />
        <View style={styles.screenHeader}>
          <TouchableOpacity 
            style={styles.backButton}
            onPress={() => setCurrentScreen('main')}
          >
            <Text style={styles.backButtonText}>← Back</Text>
          </TouchableOpacity>
          <Text style={styles.screenTitle}>Find Programs Near Me</Text>
        </View>
        <ProgramsNearMe />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#ffffff" />
      <Header />
      {renderContent()}
      <BottomNavigation activeTab={activeTab} setActiveTab={setActiveTab} />

      {/* Global Modals */}
      {/* Add Medication Modal */}
      <Modal
        visible={showAddMedicationModal}
        animationType="slide"
        transparent={true}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>
              Add {medicationType === 'daily' ? 'Daily' : 'Exacerbation'} Medication
            </Text>

            <View style={styles.modalField}>
              <Text style={styles.modalLabel}>Medication Name</Text>
              <TextInput
                style={styles.modalInput}
                value={newMedication.name}
                onChangeText={(text) => setNewMedication({...newMedication, name: text})}
                placeholder="e.g., Albuterol"
                placeholderTextColor="#d1d5db"
              />
            </View>

            <View style={styles.modalField}>
              <Text style={styles.modalLabel}>Dosage</Text>
              <TextInput
                style={styles.modalInput}
                value={newMedication.dosage}
                onChangeText={(text) => setNewMedication({...newMedication, dosage: text})}
                placeholder="e.g., 2 puffs"
                placeholderTextColor="#d1d5db"
              />
            </View>

            <View style={styles.modalField}>
              <Text style={styles.modalLabel}>How Often</Text>
              <TextInput
                style={styles.modalInput}
                value={newMedication.frequency}
                onChangeText={(text) => setNewMedication({...newMedication, frequency: text})}
                placeholder="e.g., Twice daily"
                placeholderTextColor="#d1d5db"
              />
            </View>

            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={styles.modalCancelButton}
                onPress={() => setShowAddMedicationModal(false)}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.modalSaveButton}
                onPress={saveMedication}
              >
                <Text style={styles.modalSaveText}>Save Medication</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

            {/* Add Food Modal */}
            <Modal
              visible={showAddFoodModal}
              animationType="slide"
              transparent={true}
              onShow={() => console.log('Food modal is now showing')}
              onDismiss={() => console.log('Food modal dismissed')}
            >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Add Food to {selectedMealCategory}</Text>

                  <View style={styles.modalField}>
                    <Text style={styles.modalLabel}>Food Name *</Text>
                    <View style={styles.foodInputContainer}>
                      <TextInput
                        style={styles.modalInput}
                        value={foodName}
                        onChangeText={setFoodName}
                        placeholder="e.g., Grilled Chicken Breast"
                        placeholderTextColor="#d1d5db"
                      />
                      <TouchableOpacity 
                        style={styles.searchButton}
                        onPress={() => setShowFoodSearch(true)}
                      >
                        <Text style={styles.searchButtonText}>Search DB</Text>
                      </TouchableOpacity>
                    </View>
                  </View>

            <View style={styles.modalField}>
              <Text style={styles.modalLabel}>Quantity *</Text>
              <TextInput
                style={styles.modalInput}
                value={foodQuantity}
                onChangeText={setFoodQuantity}
                placeholder="e.g., 1 cup, 100g, 1 piece"
                placeholderTextColor="#d1d5db"
              />
            </View>

            <View style={styles.modalField}>
              <Text style={styles.modalLabel}>Calories (optional)</Text>
              <TextInput
                style={styles.modalInput}
                value={foodCalories}
                onChangeText={setFoodCalories}
                placeholder="e.g., 250"
                placeholderTextColor="#d1d5db"
                keyboardType="numeric"
              />
            </View>

            <View style={styles.modalField}>
              <Text style={styles.modalLabel}>Protein (g) (optional)</Text>
              <TextInput
                style={styles.modalInput}
                value={foodProtein}
                onChangeText={setFoodProtein}
                placeholder="e.g., 25"
                placeholderTextColor="#d1d5db"
                keyboardType="numeric"
              />
            </View>

            <View style={styles.modalField}>
              <Text style={styles.modalLabel}>Carbs (g) (optional)</Text>
              <TextInput
                style={styles.modalInput}
                value={foodCarbs}
                onChangeText={setFoodCarbs}
                placeholder="e.g., 15"
                placeholderTextColor="#d1d5db"
                keyboardType="numeric"
              />
            </View>

            <View style={styles.modalField}>
              <Text style={styles.modalLabel}>Fat (g) (optional)</Text>
              <TextInput
                style={styles.modalInput}
                value={foodFat}
                onChangeText={setFoodFat}
                placeholder="e.g., 8"
                placeholderTextColor="#d1d5db"
                keyboardType="numeric"
              />
            </View>

            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={styles.modalCancelButton}
                onPress={closeAddFoodModal}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.modalSaveButton}
                onPress={saveFoodItem}
              >
                <Text style={styles.modalSaveText}>Add Food</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* Weight Modal */}
      <Modal
        visible={showWeightModal}
        animationType="slide"
        transparent={true}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>
              {weightType === 'current' ? 'Set Current Weight' : 'Set Goal Weight'}
            </Text>
            <View style={styles.modalField}>
              <Text style={styles.modalLabel}>Weight (lbs)</Text>
              <TextInput
                style={styles.modalInput}
                value={weightInput}
                onChangeText={setWeightInput}
                placeholder="e.g., 150"
                placeholderTextColor="#d1d5db"
                keyboardType="numeric"
              />
            </View>
            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={styles.modalCancelButton}
                onPress={closeWeightModal}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.modalSaveButton}
                onPress={saveWeight}
              >
                <Text style={styles.modalSaveText}>Save Weight</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
            </Modal>

            {/* Food Search Modal */}
            <Modal
              visible={showFoodSearch}
              animationType="slide"
              transparent={true}
            >
              <View style={styles.modalOverlay}>
                <View style={styles.modalContent}>
                  <Text style={styles.modalTitle}>Search Food Database</Text>
                  
                  <View style={styles.modalField}>
                    <Text style={styles.modalLabel}>Search for food</Text>
                    <TextInput
                      style={styles.modalInput}
                      value={foodSearchQuery}
                      onChangeText={setFoodSearchQuery}
                      placeholder="e.g., chicken breast, apple, rice"
                      placeholderTextColor="#d1d5db"
                      onSubmitEditing={() => searchFoodDatabase(foodSearchQuery)}
                    />
                  </View>

                  <View style={styles.modalButtons}>
                    <TouchableOpacity 
                      style={styles.modalCancelButton}
                      onPress={() => setShowFoodSearch(false)}
                    >
                      <Text style={styles.modalCancelText}>Cancel</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={styles.modalSaveButton}
                      onPress={() => searchFoodDatabase(foodSearchQuery)}
                      disabled={isSearching}
                    >
                      <Text style={styles.modalSaveText}>
                        {isSearching ? 'Searching...' : 'Search'}
                      </Text>
                    </TouchableOpacity>
                  </View>

                  {/* Search Results */}
                  {foodSearchResults.length > 0 && (
                    <View style={styles.searchResults}>
                      <Text style={styles.searchResultsTitle}>Select a food:</Text>
                      <ScrollView style={styles.searchResultsList}>
                        {foodSearchResults.map((food) => (
                          <TouchableOpacity 
                            key={food.id}
                            style={styles.searchResultItem}
                            onPress={() => selectFoodFromSearch(food)}
                          >
                            <Text style={styles.searchResultName}>{food.description}</Text>
                            {food.brandOwner && (
                              <Text style={styles.searchResultBrand}>{food.brandOwner}</Text>
                            )}
                            <View style={styles.searchResultNutrients}>
                              <Text style={styles.searchResultNutrient}>
                                Cal: {food.nutrients.calories || 'N/A'}
                              </Text>
                              <Text style={styles.searchResultNutrient}>
                                Protein: {food.nutrients.protein || 'N/A'}g
                              </Text>
                            </View>
                          </TouchableOpacity>
                        ))}
                      </ScrollView>
                    </View>
                  )}
                </View>
              </View>
            </Modal>

            {/* Exercise Modal */}
            <Modal
              visible={showExerciseModal}
              animationType="slide"
              transparent={true}
            >
              <View style={styles.modalOverlay}>
                <View style={styles.modalContent}>
                  <Text style={styles.modalTitle}>Log Exercise</Text>
                  
                  <View style={styles.modalField}>
                    <Text style={styles.modalLabel}>Exercise Type</Text>
                    <View style={styles.exerciseTypeGrid}>
                      <TouchableOpacity 
                        style={[styles.exerciseTypeButton, selectedExerciseType === 'Walking' && styles.exerciseTypeButtonSelected]}
                        onPress={() => setSelectedExerciseType('Walking')}
                      >
                        <Text style={styles.exerciseTypeIcon}>🚶‍♂️</Text>
                        <Text style={styles.exerciseTypeText}>Walking</Text>
                      </TouchableOpacity>
                      
                      <TouchableOpacity 
                        style={[styles.exerciseTypeButton, selectedExerciseType === 'Running/Jogging' && styles.exerciseTypeButtonSelected]}
                        onPress={() => setSelectedExerciseType('Running/Jogging')}
                      >
                        <Text style={styles.exerciseTypeIcon}>🏃‍♂️</Text>
                        <Text style={styles.exerciseTypeText}>Running</Text>
                      </TouchableOpacity>
                      
                      <TouchableOpacity 
                        style={[styles.exerciseTypeButton, selectedExerciseType === 'Swimming' && styles.exerciseTypeButtonSelected]}
                        onPress={() => setSelectedExerciseType('Swimming')}
                      >
                        <Text style={styles.exerciseTypeIcon}>🏊‍♂️</Text>
                        <Text style={styles.exerciseTypeText}>Swimming</Text>
                      </TouchableOpacity>
                      
                      <TouchableOpacity 
                        style={[styles.exerciseTypeButton, selectedExerciseType === 'Hiking' && styles.exerciseTypeButtonSelected]}
                        onPress={() => setSelectedExerciseType('Hiking')}
                      >
                        <Text style={styles.exerciseTypeIcon}>🥾</Text>
                        <Text style={styles.exerciseTypeText}>Hiking</Text>
                      </TouchableOpacity>
                      
                      <TouchableOpacity 
                        style={[styles.exerciseTypeButton, selectedExerciseType === 'Weight Training' && styles.exerciseTypeButtonSelected]}
                        onPress={() => setSelectedExerciseType('Weight Training')}
                      >
                        <Text style={styles.exerciseTypeIcon}>🏋️‍♂️</Text>
                        <Text style={styles.exerciseTypeText}>Weight Training</Text>
                      </TouchableOpacity>
                      
                      <TouchableOpacity 
                        style={[styles.exerciseTypeButton, selectedExerciseType === 'other' && styles.exerciseTypeButtonSelected]}
                        onPress={() => setSelectedExerciseType('other')}
                      >
                        <Text style={styles.exerciseTypeIcon}>💪</Text>
                        <Text style={styles.exerciseTypeText}>Other</Text>
                      </TouchableOpacity>
                    </View>
                  </View>

                  {selectedExerciseType === 'other' && (
                    <View style={styles.modalField}>
                      <Text style={styles.modalLabel}>Custom Exercise Type</Text>
                      <TextInput
                        style={styles.modalInput}
                        value={customExerciseType}
                        onChangeText={setCustomExerciseType}
                        placeholder="e.g., Yoga, Cycling, Dancing"
                        placeholderTextColor="#d1d5db"
                      />
                    </View>
                  )}

                  <View style={styles.modalField}>
                    <Text style={styles.modalLabel}>Duration (minutes)</Text>
                    <TextInput
                      style={styles.modalInput}
                      value={exerciseMinutes}
                      onChangeText={setExerciseMinutes}
                      placeholder="e.g., 30"
                      placeholderTextColor="#d1d5db"
                      keyboardType="numeric"
                    />
                  </View>

                  <TouchableOpacity style={styles.phoneDataButton} onPress={importFromPhoneHealth}>
                    <Text style={styles.phoneDataButtonText}>📱 Import from Phone Health (Optional)</Text>
                  </TouchableOpacity>

                  <View style={styles.modalButtons}>
                    <TouchableOpacity 
                      style={styles.modalCancelButton}
                      onPress={closeExerciseModal}
                    >
                      <Text style={styles.modalCancelText}>Cancel</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={styles.modalSaveButton}
                      onPress={saveExercise}
                    >
                      <Text style={styles.modalSaveText}>Log Exercise</Text>
                    </TouchableOpacity>
                  </View>
                </View>
              </View>
            </Modal>

            {/* Oxygen Saturation Modal */}
            <Modal
              visible={showOxygenModal}
              animationType="slide"
              transparent={true}
            >
              <View style={styles.modalOverlay}>
                <View style={styles.modalContent}>
                  <Text style={styles.modalTitle}>Log Oxygen Saturation</Text>
                  
                  <View style={styles.modalField}>
                    <Text style={styles.modalLabel}>Oxygen Saturation Level (%)</Text>
                    <TextInput
                      style={styles.modalInput}
                      value={oxygenLevel}
                      onChangeText={setOxygenLevel}
                      placeholder="e.g., 95"
                      placeholderTextColor="#d1d5db"
                      keyboardType="numeric"
                    />
                    <Text style={styles.oxygenHelpText}>
                      Normal range: 95-100% | COPD patients: Often 88-92%
                    </Text>
                  </View>

                  <View style={styles.oxygenGuidelines}>
                    <Text style={styles.guidelinesTitle}>Reference Levels:</Text>
                    <View style={styles.oxygenRange}>
                      <Text style={styles.oxygenRangeGood}>90-100%: Normal Ranges</Text>
                      <Text style={styles.oxygenRangeLow}>Below 90%: Seek medical attention</Text>
                    </View>
                  </View>

                  <View style={styles.modalButtons}>
                    <TouchableOpacity 
                      style={styles.modalCancelButton}
                      onPress={closeOxygenModal}
                    >
                      <Text style={styles.modalCancelText}>Cancel</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                      style={styles.modalSaveButton}
                      onPress={saveOxygenReading}
                    >
                      <Text style={styles.modalSaveText}>Save Reading</Text>
                    </TouchableOpacity>
                  </View>
                </View>
              </View>
            </Modal>

            {/* Profile Modal */}
      <Modal
        visible={showProfileModal}
        animationType="slide"
        transparent={true}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>
              {profileField === 'age' ? 'Set Age' :
               profileField === 'sex' ? 'Set Sex' :
               profileField === 'height' ? 'Set Height' : 'Update Profile'}
            </Text>
            <View style={styles.modalField}>
              <Text style={styles.modalLabel}>
                {profileField === 'age' ? 'Age (years)' :
                 profileField === 'sex' ? 'Sex' :
                 profileField === 'height' ? 'Height (e.g., 5\'4" or 64 inches)' : 'Value'}
              </Text>
              <TextInput
                style={styles.modalInput}
                value={profileInput}
                onChangeText={setProfileInput}
                placeholder={
                  profileField === 'age' ? 'e.g., 43' :
                  profileField === 'sex' ? 'e.g., Female' :
                  profileField === 'height' ? 'e.g., 5\'4"' : 'Enter value'
                }
                placeholderTextColor="#d1d5db"
                keyboardType={profileField === 'age' ? 'numeric' : 'default'}
              />
            </View>
            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={styles.modalCancelButton}
                onPress={closeProfileModal}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.modalSaveButton}
                onPress={saveProfileField}
              >
                <Text style={styles.modalSaveText}>Save</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  screenHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  backButton: {
    padding: 8,
  },
  backButtonText: {
    fontSize: 16,
    color: '#3b82f6',
    fontWeight: '600',
  },
  screenTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#1f2937',
    marginLeft: 16,
  },
  // COPD Tools Styles
  toolsContainer: {
    flex: 1,
    backgroundColor: '#ffffff',
    paddingBottom: 100,
  },
  toolsHeader: {
    padding: 20,
    backgroundColor: '#fef3c7',
    alignItems: 'center',
  },
  toolsTitle: {
    fontSize: 28,
    fontWeight: '800',
    color: '#92400e',
    marginBottom: 16,
    textAlign: 'center',
  },
  toolsSubtitle: {
    fontSize: 18,
    color: '#4b5563',
    textAlign: 'center',
    maxWidth: 600,
    lineHeight: 26,
  },
  quickToolsGrid: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    paddingVertical: 20,
    gap: 16,
  },
  toolCard: {
    width: 120,
    backgroundColor: '#ffffff',
    padding: 12,
    borderRadius: 12,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  toolIcon: {
    fontSize: 20,
    marginBottom: 6,
  },
  toolName: {
    fontSize: 12,
    fontWeight: '600',
    color: '#1f2937',
    textAlign: 'center',
    lineHeight: 16,
  },
  toolCardActive: {
    backgroundColor: '#f0f9ff',
    borderWidth: 2,
    borderColor: '#3b82f6',
  },
  toolContent: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  toolDescription: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
    marginBottom: 24,
    textAlign: 'justify',
  },
  severityLevels: {
    gap: 16,
    marginBottom: 24,
  },
  severityCard: {
    backgroundColor: '#f8fafc',
    padding: 16,
    borderRadius: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#3b82f6',
  },
  severityIcon: {
    fontSize: 24,
    marginBottom: 8,
  },
  severityTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#1f2937',
    marginBottom: 8,
  },
  severityText: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
  },
  actionSteps: {
    gap: 16,
    marginBottom: 24,
  },
  stepCard: {
    backgroundColor: '#f8fafc',
    padding: 16,
    borderRadius: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#dc2626',
  },
  stepNumber: {
    fontSize: 24,
    fontWeight: '800',
    color: '#dc2626',
    marginBottom: 8,
  },
  stepTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#1f2937',
    marginBottom: 8,
  },
  stepText: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
  },
  rehabBenefits: {
    gap: 16,
    marginBottom: 24,
  },
  benefitCard: {
    backgroundColor: '#f0f9ff',
    padding: 16,
    borderRadius: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#3b82f6',
  },
  benefitTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#1e40af',
    marginBottom: 8,
  },
  benefitText: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
  },
  medicationTypes: {
    gap: 16,
    marginBottom: 24,
  },
  medicationCard: {
    backgroundColor: '#fef3c7',
    padding: 16,
    borderRadius: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#f59e0b',
  },
  medicationIcon: {
    fontSize: 24,
    marginBottom: 8,
  },
  medicationTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#92400e',
    marginBottom: 8,
  },
  medicationText: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
  },
  resourceCategories: {
    gap: 16,
    marginBottom: 24,
  },
  resourceCard: {
    backgroundColor: '#f0fdf4',
    padding: 16,
    borderRadius: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#10b981',
  },
  resourceIcon: {
    fontSize: 24,
    marginBottom: 8,
  },
  resourceTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#047857',
    marginBottom: 8,
  },
  resourceText: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
  },
  assessmentButton: {
    backgroundColor: '#3b82f6',
    paddingHorizontal: 32,
    paddingVertical: 16,
    borderRadius: 8,
    alignSelf: 'center',
  },
  assessmentButtonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
  },
  assessmentForm: {
    marginBottom: 24,
  },
  selectField: {
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    padding: 12,
    marginTop: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  selectPlaceholder: {
    fontSize: 16,
    color: '#9ca3af',
    flex: 1,
  },
  dropdownArrow: {
    fontSize: 12,
    color: '#6b7280',
    marginLeft: 8,
  },
  dropdownList: {
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderTopWidth: 0,
    borderBottomLeftRadius: 8,
    borderBottomRightRadius: 8,
    marginTop: -1,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  dropdownItem: {
    padding: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  dropdownItemText: {
    fontSize: 16,
    color: '#374151',
  },
  // Exacerbation Plan Styles
  subsectionTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: '#1f2937',
    marginBottom: 16,
  },
  contactsSection: {
    marginBottom: 24,
  },
  contactField: {
    marginBottom: 16,
  },
  contactLabel: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 8,
  },
  contactInput: {
    backgroundColor: '#f9fafb',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    color: '#374151',
  },
  contactInputPlaceholder: {
    color: '#9ca3af',
    fontStyle: 'italic',
  },
  medicationSection: {
    marginBottom: 24,
  },
  medicationCategory: {
    marginBottom: 24,
  },
  medicationCategoryTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#1f2937',
    marginBottom: 16,
  },
  addMedicationButton: {
    backgroundColor: '#10b981',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 6,
    alignSelf: 'flex-start',
    marginBottom: 16,
  },
  addMedicationText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
  },
  medicationHeader: {
    flexDirection: 'row',
    backgroundColor: '#f3f4f6',
    padding: 12,
    borderRadius: 6,
    marginBottom: 8,
  },
  medicationHeaderText: {
    flex: 1,
    fontSize: 14,
    fontWeight: '600',
    color: '#374151',
    textAlign: 'center',
  },
  noMedicationText: {
    fontSize: 16,
    color: '#9ca3af',
    fontStyle: 'italic',
    textAlign: 'center',
    padding: 20,
  },
  zonesSection: {
    marginBottom: 24,
  },
  zoneCard: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 20,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    borderLeftWidth: 6,
  },
  zoneHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  zoneIndicator: {
    width: 20,
    height: 20,
    borderRadius: 10,
    marginRight: 12,
  },
  zoneTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#1f2937',
    flex: 1,
  },
  zoneSymptoms: {
    marginBottom: 16,
  },
  zoneSymptom: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
    marginBottom: 8,
  },
  zoneAction: {
    fontSize: 16,
    fontWeight: '600',
    color: '#059669',
    backgroundColor: '#d1fae5',
    padding: 12,
    borderRadius: 6,
    textAlign: 'center',
  },
  instructionsSection: {
    marginBottom: 24,
  },
  instructionsInput: {
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    padding: 16,
    minHeight: 100,
    justifyContent: 'center',
  },
  instructionsTextInput: {
    backgroundColor: '#f9fafb',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    padding: 16,
    minHeight: 100,
    fontSize: 16,
    color: '#374151',
    textAlignVertical: 'top',
  },
  instructionsPlaceholder: {
    fontSize: 16,
    color: '#9ca3af',
    fontStyle: 'italic',
  },
  actionButtons: {
    flexDirection: 'row',
    gap: 16,
  },
  saveButton: {
    flex: 1,
    backgroundColor: '#2563eb',
    paddingVertical: 16,
    borderRadius: 8,
    alignItems: 'center',
  },
  saveButtonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
  },
  // Medication Table Styles
  medicationRow: {
    flexDirection: 'row',
    paddingVertical: 12,
    paddingHorizontal: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
    alignItems: 'center',
  },
  medicationCell: {
    flex: 1,
    fontSize: 14,
    color: '#4b5563',
    textAlign: 'center',
  },
  deleteButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#ef4444',
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 8,
  },
  deleteButtonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  // Modal Styles
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 24,
    width: '90%',
    maxWidth: 400,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: '#1f2937',
    marginBottom: 20,
    textAlign: 'center',
  },
  modalField: {
    marginBottom: 16,
  },
  modalLabel: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 8,
  },
  modalInput: {
    backgroundColor: '#f9fafb',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    color: '#374151',
  },
  modalButtons: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 20,
  },
  modalCancelButton: {
    flex: 1,
    backgroundColor: '#6b7280',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  modalCancelText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  modalSaveButton: {
    flex: 1,
    backgroundColor: '#10b981',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  modalSaveText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  // Food Search Styles
  foodInputContainer: {
    flexDirection: 'row',
    gap: 8,
  },
  searchButton: {
    backgroundColor: '#2563eb',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 6,
    justifyContent: 'center',
  },
  searchButtonText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '600',
  },
  searchResults: {
    marginTop: 16,
    maxHeight: 300,
  },
  searchResultsTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 8,
  },
  searchResultsList: {
    maxHeight: 250,
  },
  searchResultItem: {
    padding: 12,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 8,
    marginBottom: 8,
    backgroundColor: '#f9fafb',
  },
  searchResultName: {
    fontSize: 14,
    fontWeight: '600',
    color: '#111827',
    marginBottom: 4,
  },
  searchResultBrand: {
    fontSize: 12,
    color: '#6b7280',
    marginBottom: 4,
  },
  searchResultNutrients: {
    flexDirection: 'row',
    gap: 12,
  },
  searchResultNutrient: {
    fontSize: 12,
    color: '#374151',
    backgroundColor: '#e5e7eb',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  // Exercise Modal Styles
  exerciseTypeGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
    marginTop: 8,
  },
  exerciseTypeButton: {
    width: '45%',
    padding: 16,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#e5e7eb',
    backgroundColor: '#f9fafb',
    alignItems: 'center',
  },
  exerciseTypeButtonSelected: {
    borderColor: '#2563eb',
    backgroundColor: '#eff6ff',
  },
  exerciseTypeIcon: {
    fontSize: 24,
    marginBottom: 8,
  },
  exerciseTypeText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#374151',
  },
  phoneDataButton: {
    backgroundColor: '#6366f1',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 8,
    alignItems: 'center',
    marginVertical: 16,
  },
  phoneDataButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
  },
  exerciseList: {
    marginTop: 12,
    maxHeight: 100,
  },
  exerciseItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 4,
    paddingHorizontal: 8,
    backgroundColor: '#f3f4f6',
    borderRadius: 6,
    marginVertical: 2,
  },
  exerciseType: {
    fontSize: 12,
    color: '#374151',
    fontWeight: '500',
  },
  exerciseMinutes: {
    fontSize: 12,
    color: '#059669',
    fontWeight: '600',
  },
  // Oxygen Saturation Styles
  oxygenReadingsList: {
    marginTop: 12,
    maxHeight: 120,
  },
  readingsTitle: {
    fontSize: 12,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 8,
  },
  readingItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 6,
    paddingHorizontal: 8,
    backgroundColor: '#f3f4f6',
    borderRadius: 6,
    marginVertical: 2,
  },
  readingLevel: {
    fontSize: 14,
    color: '#059669',
    fontWeight: '700',
  },
  readingTime: {
    fontSize: 12,
    color: '#6b7280',
  },
  oxygenHelpText: {
    fontSize: 12,
    color: '#6b7280',
    marginTop: 4,
    fontStyle: 'italic',
  },
  oxygenGuidelines: {
    marginVertical: 16,
    padding: 12,
    backgroundColor: '#f8fafc',
    borderRadius: 8,
  },
  oxygenRange: {
    marginTop: 8,
  },
  oxygenRangeGood: {
    fontSize: 12,
    color: '#059669',
    marginVertical: 2,
  },
  oxygenRangeOk: {
    fontSize: 12,
    color: '#d97706',
    marginVertical: 2,
  },
  oxygenRangeLow: {
    fontSize: 12,
    color: '#dc2626',
    marginVertical: 2,
  },
  emergencyButton: {
    backgroundColor: '#dc2626',
    paddingHorizontal: 32,
    paddingVertical: 16,
    borderRadius: 8,
    alignSelf: 'center',
  },
  emergencyButtonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
  },
  medicationButton: {
    backgroundColor: '#f59e0b',
    paddingHorizontal: 32,
    paddingVertical: 16,
    borderRadius: 8,
    alignSelf: 'center',
  },
  medicationButtonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
  },
  resourceButton: {
    backgroundColor: '#10b981',
    paddingHorizontal: 32,
    paddingVertical: 16,
    borderRadius: 8,
    alignSelf: 'center',
  },
  resourceButtonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
  },
  // Profile Styles
  profileContainer: {
    flex: 1,
    backgroundColor: '#ffffff',
    paddingBottom: 100,
  },
  profileHeader: {
    padding: 20,
    backgroundColor: '#f0f9ff',
    alignItems: 'center',
  },
  profileTitle: {
    fontSize: 28,
    fontWeight: '800',
    color: '#1e40af',
    marginBottom: 16,
    textAlign: 'center',
  },
  healthFormSection: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  formSectionTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: '#1f2937',
    marginBottom: 16,
  },
  formField: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  fieldLabel: {
    fontSize: 16,
    color: '#4b5563',
    fontWeight: '500',
  },
  fieldValue: {
    fontSize: 16,
    color: '#1f2937',
    fontWeight: '600',
  },
  addReadingButton: {
    backgroundColor: '#3b82f6',
    paddingHorizontal: 32,
    paddingVertical: 16,
    borderRadius: 8,
    alignSelf: 'center',
    marginTop: 20,
  },
  addReadingButtonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
  },
  findProgramsButton: {
    backgroundColor: '#1e40af',
    paddingHorizontal: 32,
    paddingVertical: 16,
    borderRadius: 8,
    alignSelf: 'center',
  },
  findProgramsButtonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
  },
  copdImage: {
    width: '100%',
    height: 700,
    marginBottom: 20,
    alignSelf: 'center',
  },
  // Saved Action Plans Styles
  savedPlansSection: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  sectionSubtitle: {
    fontSize: 16,
    color: '#6b7280',
    marginBottom: 20,
    lineHeight: 24,
  },
  noPlansContainer: {
    alignItems: 'center',
    padding: 40,
    backgroundColor: '#f9fafb',
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#e5e7eb',
    borderStyle: 'dashed',
  },
  noPlansText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 8,
  },
  noPlansSubtext: {
    fontSize: 14,
    color: '#6b7280',
    textAlign: 'center',
    lineHeight: 20,
  },
  plansList: {
    gap: 16,
  },
  planCard: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 20,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  planHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
    paddingBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  planDate: {
    fontSize: 18,
    fontWeight: '700',
    color: '#1f2937',
  },
  planTime: {
    fontSize: 14,
    color: '#6b7280',
  },
  planDetails: {
    marginBottom: 16,
  },
  planDetailRow: {
    flexDirection: 'row',
    marginBottom: 8,
  },
  planDetailLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#374151',
    width: 120,
  },
  planDetailValue: {
    fontSize: 14,
    color: '#6b7280',
    flex: 1,
  },
  planActions: {
    flexDirection: 'row',
    gap: 12,
  },
  viewPlanButton: {
    flex: 1,
    backgroundColor: '#3b82f6',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  viewPlanButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
  },
  deletePlanButton: {
    flex: 1,
    backgroundColor: '#ef4444',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  deletePlanButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
  },
  // Guidelines Styles
  guidelinesContainer: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  guidelinesContentContainer: {
    paddingBottom: 150, // Extra space for bottom navigation
  },
  guidelinesSection: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  guidelinesTitle: {
    fontSize: 28,
    fontWeight: '800',
    color: '#1f2937',
    marginBottom: 16,
    textAlign: 'center',
  },
  guidelinesIntro: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
    marginBottom: 24,
    textAlign: 'center',
  },
  guidelineItem: {
    backgroundColor: '#f8fafc',
    padding: 20,
    borderRadius: 12,
    marginBottom: 16,
    borderLeftWidth: 4,
    borderLeftColor: '#3b82f6',
  },
  guidelineTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#1f2937',
    marginBottom: 8,
  },
  guidelineText: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
  },
  consultText: {
    fontSize: 14,
    color: '#6b7280',
    fontStyle: 'italic',
    textAlign: 'center',
    marginTop: 20,
    padding: 16,
    backgroundColor: '#fef3c7',
    borderRadius: 8,
  },
  // Additional Guidelines Styles
  exacerbationsSection: {
    padding: 20,
    backgroundColor: '#f0f9ff',
    marginTop: 20,
  },
  sectionTitle: {
    fontSize: 24,
    fontWeight: '700',
    color: '#1e40af',
    marginBottom: 8,
  },
  sectionSubtitle: {
    fontSize: 16,
    color: '#6b7280',
    marginBottom: 16,
  },
  exacerbationsText: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
    marginBottom: 20,
  },
  strategyGrid: {
    gap: 16,
  },
  strategyItem: {
    backgroundColor: '#ffffff',
    padding: 16,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e0e7ff',
  },
  strategyTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#1e40af',
    marginBottom: 8,
  },
  strategyText: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
  },
  proteinSection: {
    padding: 20,
    backgroundColor: '#f0fdf4',
    marginTop: 20,
  },
  proteinText: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
    marginBottom: 16,
  },
  exploreText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#059669',
    textAlign: 'center',
  },
  foodsSection: {
    padding: 20,
    backgroundColor: '#ffffff',
    marginTop: 20,
  },
  foodsIntro: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
    marginBottom: 20,
  },
  foodCategory: {
    marginBottom: 24,
  },
  foodCategoryTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: '#1f2937',
    marginBottom: 16,
  },
  foodItem: {
    flexDirection: 'row',
    marginBottom: 16,
    alignItems: 'flex-start',
  },
  checkmark: {
    fontSize: 20,
    color: '#10b981',
    marginRight: 12,
    marginTop: 2,
  },
  xmark: {
    fontSize: 20,
    color: '#ef4444',
    marginRight: 12,
    marginTop: 2,
  },
  foodContent: {
    flex: 1,
  },
  foodName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 4,
  },
  foodDescription: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
  },
  // Tracking Container
  trackingContainer: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  trackingContentContainer: {
    paddingBottom: 150, // Extra space for bottom navigation
  },
  // Tracking Page Styles
  trackingHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  dateContainer: {
    flex: 1,
    alignItems: 'center',
  },
  trackingTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1f2937',
  },
  trackingDate: {
    fontSize: 14,
    color: '#6b7280',
    marginTop: 2,
  },
  dateNavButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#f3f4f6',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  dateNavArrow: {
    fontSize: 18,
    color: '#374151',
    fontWeight: '600',
  },
  trackingActions: {
    flexDirection: 'row',
    gap: 12,
  },
  trackingActionButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#f3f4f6',
    justifyContent: 'center',
    alignItems: 'center',
  },
  trackingActionIcon: {
    fontSize: 16,
    color: '#6b7280',
  },
  targetsSection: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  targetsHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  targetsTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    letterSpacing: 0.5,
  },
  targetsArrow: {
    fontSize: 18,
    color: '#6b7280',
  },
  targetsGrid: {
    gap: 12,
  },
  targetCard: {
    backgroundColor: '#f8fafc',
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  targetLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#374151',
    marginBottom: 4,
  },
  targetValue: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 8,
  },
  targetProgress: {
    height: 6,
    backgroundColor: '#e5e7eb',
    borderRadius: 3,
    marginBottom: 4,
    overflow: 'hidden',
  },
  targetProgressBar: {
    height: '100%',
    backgroundColor: '#3b82f6',
    borderRadius: 3,
  },
  targetPercentage: {
    fontSize: 12,
    color: '#6b7280',
    textAlign: 'right',
  },
  nutrientsSection: {
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  nutrientsTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    letterSpacing: 0.5,
    marginBottom: 16,
  },
  nutrientsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  nutrientCard: {
    backgroundColor: '#ffffff',
    borderRadius: 8,
    padding: 12,
    width: '22%',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  nutrientLabel: {
    fontSize: 12,
    color: '#6b7280',
    marginBottom: 4,
    textAlign: 'center',
  },
  nutrientPercentage: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1f2937',
  },
  waterSection: {
    padding: 20,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  waterHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  waterLabel: {
    fontSize: 16,
    fontWeight: '500',
    color: '#374151',
  },
  waterAmount: {
    fontSize: 16,
    color: '#6b7280',
  },
  waterArrow: {
    fontSize: 16,
    color: '#6b7280',
  },
  waterProgress: {
    height: 8,
    backgroundColor: '#e5e7eb',
    borderRadius: 4,
    overflow: 'hidden',
  },
  waterProgressBar: {
    height: '100%',
    backgroundColor: '#3b82f6',
    borderRadius: 4,
  },
  mealsSection: {
    backgroundColor: '#ffffff',
  },
  mealCategory: {
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  mealCategoryHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
  },
  mealCategoryIcon: {
    fontSize: 18,
    color: '#3b82f6',
    marginRight: 12,
    width: 20,
    textAlign: 'center',
  },
  mealCategoryName: {
    flex: 1,
    fontSize: 16,
    color: '#374151',
  },
  mealCategoryArrow: {
    fontSize: 16,
    color: '#6b7280',
  },
  // Food Item Display Styles
  foodItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    backgroundColor: '#f9fafb',
    marginHorizontal: 16,
    marginVertical: 4,
    borderRadius: 8,
    borderLeftWidth: 3,
    borderLeftColor: '#10b981',
  },
  foodIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  foodIconText: {
    fontSize: 18,
  },
  foodDetails: {
    flex: 1,
  },
  foodItemName: {
    fontSize: 14,
    fontWeight: '600',
    color: '#111827',
    marginBottom: 2,
  },
  foodItemQuantity: {
    fontSize: 12,
    color: '#6b7280',
  },
  foodNutrition: {
    alignItems: 'flex-end',
  },
  foodCalories: {
    fontSize: 16,
    fontWeight: '700',
    color: '#059669',
  },
  foodCaloriesLabel: {
    fontSize: 10,
    color: '#6b7280',
  },
  nutritionGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 8,
    gap: 8,
  },
  nutritionItem: {
    backgroundColor: '#ffffff',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    minWidth: 70,
  },
  nutritionLabel: {
    fontSize: 10,
    color: '#6b7280',
    fontWeight: '500',
  },
  nutritionValue: {
    fontSize: 12,
    color: '#111827',
    fontWeight: '600',
  },
  copdTrackingSection: {
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  copdTrackingTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    letterSpacing: 0.5,
    marginBottom: 16,
  },
  copdMetrics: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  copdMetricCard: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 16,
    width: '47%',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  copdMetricLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#374151',
    marginBottom: 8,
  },
  copdMetricValue: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1f2937',
    marginBottom: 12,
  },
  copdMetricButton: {
    backgroundColor: '#10b981',
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 6,
    alignItems: 'center',
  },
  copdMetricButtonText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '500',
  },
  quickAddSection: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  quickAddButton: {
    backgroundColor: '#3b82f6',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 16,
    paddingHorizontal: 24,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  quickAddIcon: {
    fontSize: 20,
    color: '#ffffff',
    marginRight: 8,
  },
  quickAddText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  // Weight Tracking Styles
  weightSection: {
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  weightTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    letterSpacing: 0.5,
    marginBottom: 16,
  },
  weightCards: {
    flexDirection: 'row',
    gap: 12,
  },
  weightCard: {
    flex: 1,
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  weightLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#374151',
    marginBottom: 8,
  },
  weightValue: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1f2937',
    marginBottom: 8,
  },
  weightArrow: {
    fontSize: 16,
    color: '#6b7280',
    textAlign: 'right',
  },
  weightProgress: {
    marginTop: 16,
    padding: 16,
    backgroundColor: '#ffffff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  weightProgressLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#374151',
    marginBottom: 8,
  },
  weightProgressBar: {
    height: 8,
    backgroundColor: '#e5e7eb',
    borderRadius: 4,
    marginBottom: 8,
    overflow: 'hidden',
  },
  weightProgressFill: {
    height: '100%',
    backgroundColor: '#10b981',
    borderRadius: 4,
  },
  weightProgressText: {
    fontSize: 12,
    color: '#6b7280',
    textAlign: 'center',
  },
  // Profile Styles
  profileContainer: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  profileHeader: {
    padding: 20,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  profileTitle: {
    fontSize: 24,
    fontWeight: '700',
    color: '#111827',
    textAlign: 'center',
  },
  profileSection: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  profileItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 16,
    paddingHorizontal: 0,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  profileLabel: {
    fontSize: 16,
    color: '#374151',
    fontWeight: '500',
  },
  profileValueContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  profileValue: {
    fontSize: 16,
    color: '#6b7280',
    marginRight: 8,
  },
  profileArrow: {
    fontSize: 18,
    color: '#9ca3af',
  },
  // Pulmonary Rehabilitation Styles
  pulmonaryContainer: {
    maxHeight: 400,
    backgroundColor: '#ffffff',
    paddingBottom: 20,
  },
  pulmonaryHeader: {
    padding: 20,
    backgroundColor: '#f8fafc',
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  pulmonaryTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1f2937',
    marginBottom: 12,
  },
  pulmonarySubtitle: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
  },
  benefitsSection: {
    padding: 20,
  },
  findingProgramsSection: {
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  findingProgramsText: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
    marginBottom: 16,
  },
  programSteps: {
    marginBottom: 20,
  },
  programStep: {
    fontSize: 16,
    color: '#374151',
    lineHeight: 24,
    marginBottom: 8,
  },
  homeExerciseSection: {
    padding: 20,
  },
  homeExerciseIntro: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
    marginBottom: 20,
  },
  exerciseCategory: {
    marginBottom: 30,
  },
  exerciseCategoryTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1f2937',
    marginBottom: 8,
  },
  exerciseCategorySubtitle: {
    fontSize: 16,
    color: '#6b7280',
    marginBottom: 16,
  },
  exerciseCard: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  exerciseName: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 8,
  },
  exerciseDescription: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
    marginBottom: 12,
  },
  exerciseFrequency: {
    fontSize: 14,
    color: '#059669',
    fontWeight: '500',
    backgroundColor: '#ecfdf5',
    padding: 8,
    borderRadius: 6,
  },
  exerciseWarning: {
    backgroundColor: '#fef3c7',
    borderWidth: 1,
    borderColor: '#f59e0b',
    borderRadius: 8,
    padding: 16,
    marginTop: 20,
  },
  exerciseWarningText: {
    fontSize: 14,
    color: '#92400e',
    fontWeight: '500',
    lineHeight: 20,
  },
  trackProgressSection: {
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  trackProgressText: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
    marginBottom: 16,
  },
  trackingItems: {
    marginBottom: 20,
  },
  trackingItem: {
    fontSize: 16,
    color: '#374151',
    lineHeight: 24,
    marginBottom: 8,
  },
  startJournalButton: {
    backgroundColor: '#3b82f6',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
  },
  startJournalButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  // Medication Guide Styles
  medicationContainer: {
    maxHeight: 400,
    backgroundColor: '#ffffff',
    paddingBottom: 20,
  },
  medicationHeader: {
    padding: 20,
    backgroundColor: '#f8fafc',
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  medicationTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1f2937',
    marginBottom: 12,
  },
  medicationSubtitle: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
  },
  medicationNavigation: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  navigationTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 12,
  },
  navigationButtons: {
    flexDirection: 'row',
    gap: 12,
  },
  navButton: {
    backgroundColor: '#3b82f6',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 6,
    flex: 1,
    alignItems: 'center',
  },
  navButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '500',
  },
  medicationCategories: {
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  categoryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  categoryCard: {
    backgroundColor: '#ffffff',
    borderRadius: 8,
    padding: 12,
    alignItems: 'center',
    width: '30%',
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  categoryIcon: {
    fontSize: 24,
    marginBottom: 8,
  },
  categoryTitle: {
    fontSize: 12,
    fontWeight: '500',
    color: '#374151',
    textAlign: 'center',
  },
  medicationDetailCard: {
    backgroundColor: '#f9fafb',
    borderRadius: 12,
    padding: 20,
    marginTop: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  medicationDetailTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: '#1f2937',
    marginBottom: 8,
  },
  medicationDetailDescription: {
    fontSize: 16,
    color: '#6b7280',
    marginBottom: 16,
    lineHeight: 24,
  },
  medicationSubSection: {
    marginBottom: 16,
  },
  medicationSubTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 12,
  },
  medicationTypeLabel: {
    fontSize: 15,
    fontWeight: '600',
    color: '#4b5563',
    marginTop: 8,
    marginBottom: 4,
  },
  medicationItem: {
    fontSize: 15,
    color: '#6b7280',
    marginLeft: 8,
    marginBottom: 4,
    lineHeight: 22,
  },
  inhalerGuideSection: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  guideSubtitle: {
    fontSize: 16,
    color: '#6b7280',
    marginBottom: 20,
    lineHeight: 24,
  },
  inhalerCard: {
    backgroundColor: '#f8fafc',
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  inhalerName: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1f2937',
    marginBottom: 16,
  },
  inhalerDetails: {
    marginBottom: 16,
  },
  detailTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 8,
  },
  detailText: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
    marginBottom: 4,
  },
  boldText: {
    fontWeight: '600',
    color: '#1f2937',
  },
  tipsSection: {
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  tipsList: {
    marginTop: 12,
  },
  tipItem: {
    fontSize: 16,
    color: '#374151',
    lineHeight: 24,
    marginBottom: 8,
  },
  techniqueSection: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  techniqueIntro: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
    marginBottom: 20,
  },
  techniqueCard: {
    backgroundColor: '#f8fafc',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  techniqueTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 8,
  },
  techniqueSubtitle: {
    fontSize: 14,
    color: '#6b7280',
    marginBottom: 12,
    fontStyle: 'italic',
  },
  techniqueSteps: {
    marginBottom: 12,
  },
  techniqueStep: {
    fontSize: 14,
    color: '#374151',
    lineHeight: 20,
    marginBottom: 4,
  },
  techniqueNote: {
    fontSize: 14,
    color: '#059669',
    fontStyle: 'italic',
    backgroundColor: '#ecfdf5',
    padding: 8,
    borderRadius: 6,
  },
  videoSection: {
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  watchVideosButton: {
    backgroundColor: '#10b981',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
  },
  watchVideosButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  // Resource Hub Styles
  resourcesContainer: {
    maxHeight: 400,
    backgroundColor: '#ffffff',
    paddingBottom: 20,
  },
  resourcesHeader: {
    padding: 20,
    backgroundColor: '#f8fafc',
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  resourcesTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1f2937',
    marginBottom: 12,
  },
  resourcesSubtitle: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
  },
  organizationsSection: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  organizationCard: {
    backgroundColor: '#f8fafc',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  organizationName: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 8,
  },
  organizationDescription: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
    marginBottom: 12,
  },
  organizationActions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  visitButton: {
    backgroundColor: '#3b82f6',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 6,
  },
  visitButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '500',
  },
  helplineText: {
    fontSize: 14,
    color: '#059669',
    fontWeight: '500',
  },
  educationalSection: {
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  educationalGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  educationalCard: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 16,
    width: '48%',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  educationalTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 4,
  },
  educationalType: {
    fontSize: 12,
    color: '#6b7280',
    fontStyle: 'italic',
    marginBottom: 8,
  },
  educationalDescription: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
    marginBottom: 12,
  },
  accessButton: {
    backgroundColor: '#10b981',
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 6,
    alignItems: 'center',
  },
  accessButtonText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '500',
  },
  supportGroupsSection: {
    padding: 20,
    backgroundColor: '#ffffff',
  },
  supportCard: {
    backgroundColor: '#f8fafc',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  supportName: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 4,
  },
  supportType: {
    fontSize: 14,
    color: '#6b7280',
    fontStyle: 'italic',
    marginBottom: 8,
  },
  supportDescription: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
    marginBottom: 12,
  },
  joinButton: {
    backgroundColor: '#8b5cf6',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 6,
    alignSelf: 'flex-start',
  },
  joinButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '500',
  },
  covidSection: {
    padding: 20,
    backgroundColor: '#fef3c7',
    borderWidth: 1,
    borderColor: '#f59e0b',
    borderRadius: 12,
    margin: 20,
  },
  covidSubtitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#92400e',
    marginBottom: 8,
  },
  covidDescription: {
    fontSize: 14,
    color: '#92400e',
    lineHeight: 20,
    marginBottom: 16,
  },
  covidGuidelines: {
    marginBottom: 16,
  },
  covidGuideline: {
    fontSize: 14,
    color: '#92400e',
    lineHeight: 20,
    marginBottom: 4,
  },
  covidButton: {
    backgroundColor: '#f59e0b',
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 6,
    alignItems: 'center',
  },
  covidButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '500',
  },
  appsSection: {
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  appCard: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  appName: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 8,
  },
  appDescription: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
    marginBottom: 12,
  },
  appButtons: {
    flexDirection: 'row',
    gap: 12,
  },
  appStoreButton: {
    backgroundColor: '#1f2937',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 6,
    flex: 1,
    alignItems: 'center',
  },
  appStoreButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '500',
  },
  // Recipes Styles
  recipesContainer: {
    flex: 1,
    backgroundColor: '#ffffff',
    paddingBottom: 100,
  },
  recipesHeader: {
    padding: 20,
    backgroundColor: '#f8fafc',
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  recipesTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1f2937',
    marginBottom: 12,
  },
  recipesSubtitle: {
    fontSize: 16,
    color: '#4b5563',
    lineHeight: 24,
  },
  recipeGrid: {
    padding: 20,
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 16,
  },
  recipeCard: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    width: '47%',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    overflow: 'hidden',
  },
  recipeImagePlaceholder: {
    height: 120,
    backgroundColor: '#f3f4f6',
    justifyContent: 'center',
    alignItems: 'center',
  },
  recipeImageText: {
    fontSize: 32,
  },
  recipeContent: {
    padding: 16,
  },
  recipeName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 8,
  },
  recipeDescription: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 20,
    marginBottom: 12,
  },
  recipeTime: {
    fontSize: 12,
    color: '#6b7280',
    marginBottom: 8,
  },
  recipeButton: {
    backgroundColor: '#3b82f6',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 6,
    alignItems: 'center',
  },
  recipeButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '500',
  },
});
