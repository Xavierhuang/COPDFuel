# Tracking tab — Android → iOS gap list (audit 2026-09-19)

**Files audited:**
- Android root `A = android/app/src/main`: `java/com/copdhealthtracker/ui/fragments/TrackingFragment.kt` (FRAG), `ui/viewmodel/TrackingViewModel.kt` (VM), `ui/dialogs/AddFoodDialog.kt` (AFD), `AddExerciseDialog.kt` (AED), `AddOxygenDialog.kt` (AOD), `AddWeightDialog.kt` (AWD), `CreateMealDialog.kt` (CMD), `MedicationsDialogFragment.kt` (MDF), `AddMedicationDialog.kt` (AMD), `ui/bottomsheets/AddFoodBottomSheet.kt` (BS), `repository/DataRepository.kt` (REPO), `data/FoodDatabaseHelper.kt` (DBH), `data/dao/*.kt`, `data/model/*.kt`, `utils/ProfileWeightSync.kt` (PWS), `ui/adapters/FoodSearchAdapter.kt`; layouts `res/layout/fragment_tracking.xml` (LAY), `dialog_add_food.xml`, `dialog_add_exercise.xml`, `dialog_add_oxygen.xml`, `dialog_add_weight.xml`, `dialog_create_meal.xml`, `dialog_medications.xml`, `dialog_add_medication.xml`, `bottom_sheet_add_food.xml`, `item_food_search_result.xml`, `res/values/strings.xml`.
- iOS root `I = COPDFuel/COPDFuel`: `Views/TrackingView.swift` (TV, uncommitted version, 2031 lines), `Views/DailyTrackingSummary.swift` (DTS), `Views/MedicationListView.swift` (MLV), `Services/DataManager.swift` (DM), `Services/FoodDatabaseService.swift` (FDS), `Services/HealthKitService.swift` (HK), `Models/*.swift`, `Config/AppConfig.swift`.

Verified: `I/Resources/food_database.json` is byte-identical to `A/assets/food_database.json` (18,563 foods, every food has `servingSizes`).

---

### A. Screen structure / header / view toggle
**Matches:** Day/Week/Month toggle; "This Week"/"Last Week"/"Week N" labels; week start = locale first weekday.
1. LAY:14-46 header "COPD" (#f97316) + " Fuel" (#2563eb), 28sp bold, on #dcfce7. iOS: `.navigationTitle("Tracking")`.
2. FRAG:263-303 week/month modes hide ALL day content. iOS shows the action cards in every mode — wrap them in the day branch.
3. LAY:1046-1091 profile banner is inside day view only. iOS shows it in all modes.
4. Android day-view order: toggle → day nav → profile banner → Macronutrient Targets → Fat Breakdown → Minerals → Vitamins and Fiber → Breakfast/Lunch/Dinner/Snacks → "+ Quick Add Food" / "Add from favorites" / "Create meal (add to favorites)" → Hydration Tracker card → "COPD Fuel" section (Import All from Device, "How does this work?", Oxygen + Exercise cards, Steps + Heart Rate cards) → Weight + Medications cards. Reorder iOS to match.
5. Toggle styling: active #22c55e/white, inactive #E5E7EB/#374151 (cosmetic).

### B. Day navigation / date header
6. FRAG:1111-1127: `dateLabel` = "Today" / "Yesterday" / "Tomorrow" / else weekday `EEEE` (18sp bold); below it a button showing `MMMM d, yyyy` that opens a date picker; "<" / ">" 48dp buttons step ±1 day. iOS: only a compact `DatePicker`.
7. Entry timestamps: Android exercise/oxygen/weight/water use "now" even when viewing a past day (Android bug). **Decision: iOS keeps stamping the selected day** (P3 spec §1.3).

### C. Week view
**Matches:** week label logic; day cards "No food logged" when empty; weight change color.
8. Range text: same month → `"MMM d - d, yyyy"`, else `"MMM d - MMM d, yyyy"`. iOS `"MMM d – MMM d"`.
9. Section title "Weekly Summary" (18sp #22c55e). iOS "Week: M/d/yy – M/d/yy".
10. **Trend Report card missing** (LAY:240-460, FRAG:419-489): bg #f0fdf4, title "Trend Report" (#166534); "Avg Calories" (green, int), "Avg Protein" ("Ng" blue), "Avg Carbs" ("Ng" amber #f59e0b), "Avg Fat" ("Ng" red) — averages over days *with data only*; "Total Calories:" `"N kcal"`; "Days Logged:" `"N / 7"`; "Days Met Protein Goal:" `"N / 7"` or `"Set protein target"` when `protein_target` == 0. Remove iOS Meals/Exercise/Oxygen/Water SummaryCards.
11. Weight section FRAG:374-417: header "Weight Tracking", rows "Start of Week:", "End of Week:", "Weekly Change:"; value `"${weight} lbs"`; change `"+%.1f lbs"`/`"%.1f lbs"`, or literal "No change" (gray #6b7280) when start == end entry. Hidden when no current weights.
12. Day cards FRAG:521-653: header "Daily Breakdown"; card shows 4 columns value+label: `"$calories"`/"kcal" (#22c55e), `"${protein}g"`/"Protein" (#3b82f6, or #22c55e when protein ≥ target), `"${carbs}g"`/"Carbs" (#f59e0b), `"${fat}g"`/"Fat" (#ef4444); then, if target > 0, "Protein goal met" (green) or `"Protein: N% of target"`. No-data card bg #F9FAFB, text "No food logged".

### D. Month view
**Matches:** Exercise Journal summary card; "No exercise journal entries for this month."; "All Entries This Month".
13. Month label = `"MMM d, yyyy – MMM d, yyyy"` (first–last day); "Monthly Summary" title above the nav.
14. **Monthly Trend Report card missing** (LAY:653-976, FRAG:768-835): same 4 averages, "Total Calories:", "Days Logged:" `"N / daysInMonth"`, "Days Met Protein Goal:" `"N / daysInMonth"` or "Set protein target"; weight section "Weight Tracking" / "Start of Month:" / "End of Month:" / "Monthly Change:" / "No change".
15. Android renders only static headers "Weekly Breakdown" and "Weekly Exercise Breakdown" with nothing under them except "All Entries This Month". Remove iOS per-week `WeeklyExerciseCard`s.
16. Journal storage: Android key `exercise_journal_log` JSON. iOS migrates to the same key/schema (P3 spec §1.5).

### E. Profile setup banner
17. Visible when prefs `weight` **or** `activity_level` is empty; re-checked on appear.
18. Copy: "Set Up Your Protein Target" (bold #92400e) / "Enter your weight and activity level in Profile to get a personalized protein target." (#b45309), bg #fef3c7, button "Set Up" (#f59e0b).
19. Action switches to the Profile tab.

### F. Macronutrient Targets
**Matches:** targets Energy 2000 / Carbs 250 / Fat 65; bar capped at 100%.
20. Title "Macronutrient Targets" (#22c55e).
21. Labels: `"Energy - %.1f / 2000 kcal"`, `"Protein - %.1f / N g"`, `"Net Carbs - %.1f / 250 g"`, `"Fat - %.1f / 65 g"` (hyphen, one decimal).
22. Percent text uncapped (e.g. 340%).
23. Colors: Energy green→orange over; Protein green→orange; Carbs **blue #3b82f6**→orange; Fat **orange #f97316**→**red #ef4444**.
24. Protein when profile incomplete or `protein_target` ≤ 0: label `"Protein - %.1f g"` (no target), percent text literal **"Set up"** in #2563eb, progress 0, tap → Profile tab. Gate = `weight` & `activity_level` non-empty AND `protein_target` > 0.

### G. Fat Breakdown / Minerals / Vitamins and Fiber (entire sections missing on iOS)
25. FRAG:1609-1753, LAY:1297-2261. Three white sections after the macro card, each two-column, rows = label 13sp + `"N%"` bold (percent capped at **200**) + 6dp bar (capped 100):
   - "Fat Breakdown" (title #f97316): Saturated 20 g (limit), Cholesterol 300 mg (limit), Omega-3 1.6 g (bar #22c55e), Added Sugars 50 g (limit). Limit rows: >100% → #ef4444 else #f97316.
   - "Minerals" (title #3b82f6, bars #3b82f6): Calcium 1000 mg, Iron 18 mg, Magnesium 420 mg, Zinc 11 mg | Potassium 4700 mg, Sodium 2300 mg (limit coloring), Selenium 55 mcg, Manganese 2.3 mg.
   - "Vitamins and Fiber" (title #8b5cf6, bars #8b5cf6): Fiber 25 g (bar #22c55e), Vitamin A 900 mcg, Vitamin C 90 mg, Vitamin E 15 mg | Vitamin D 20 mcg, Vitamin K 120 mcg, Water 3700 ml (bar #3b82f6).
   Row label format on Android: `"Saturated: 3.2/20 g"` style — read FRAG:1609-1753 for exact label strings.

### H. Meal category lists (Breakfast / Lunch / Dinner / Snacks)
**Matches:** four categories; "No items logged" empty text; per-item delete.
26. Sections **collapsed by default**; header tap toggles content and arrow "v"/"^". No "Meals" wrapper header.
27. Header summary always shown: `"N kcal, Ng protein, Ng carbs, Ng fat"` (ints) or "No items logged".
28. Item card: food icon, **name uppercased** bold 14sp, quantity 12sp, four gray boxes label/value "Calories"=int, "Protein"/"Carbs"/"Fat"=`"%.1fg"`.
29. Delete: dialog title "Delete Food", message `Are you sure you want to delete "NAME"?`, Delete/Cancel, then toast "Food deleted".

### I. Add-food entry point and scan launch points
30. Button "+ Quick Add Food" (#3b82f6, 56dp, bold 16sp) → bottom sheet title "Add Food", four tiles with icons: "Add Food" → AddFoodDialog; "Scan Label" → camera label mode with selected date; "Scan QR Code" → camera QR mode; "Photo Library" → image pick → review. Result: insert returned `FoodEntry`; if save-to-database, `saveScannedFoodToDatabase` (FRAG:1975-2002) parses grams from quantity with regex `\((\d+(?:\.\d+)?)g\)`, divides by grams/100 and inserts a `UserAddedFood` with 14 fields.
31. Inside AddFoodDialog: scan icon button (48dp) beside "Edit Nutrition Manually" opens the same sheet; scan result `prefillFromScan` sets food name, reveals manual section ("Hide Manual Entry"), fills calories(int)/protein/carbs/fat(%.1f).

### J. AddFoodDialog — search, source, filters, serving sizes, manual entry, favorite, add-to-database
**Matches:** meal picker; Local/USDA source toggle; Category/Food Group filter + "All Categories"/"All Food Groups"; local search algorithm; USDA endpoint; amount × serving multiplier math; "g (enter amount)"; nutrition preview.
32. **Local DB decoder drops most nutrients** (FDS `DBFood` has only cal/protein/fat/carbs/fiber/sodium/potassium). Android DBH:170-217 loads all 23. Same for user-added results.
33. **Local DB serving sizes never decoded**. Android DBH:153-168 parses `{label, grams, amount, unit, isPrimary, isCustom}` and builds options `"${label} - ${grams.toInt()}g"` / `"g (enter amount)"` with multiplier grams/100. iOS fallback uses multiplier 1.0 (wrong when portion ≠ 100 g). Label separator " - ".
34. USDA serving fallback AFD:461-477: options `"${servingUnit} - ${servingGrams}g"` (mult servingGrams/100), then "100g" only if servingGrams ≠ 100, then "g (enter amount)". Android keeps `brandOwner` as a separate second line.
35. Default serving options before any selection: "1 serving" (100 g) and "100g".
36. Source toggle labels: "Local (18,563 foods)" / "USDA Online". Status text: "Search from local database (N foods)" (N = DB + user-added) / "Search USDA FoodData Central online"; toggling clears results.
37. Filter container hidden in USDA mode.
38. Search trigger: explicit "Search" button + keyboard search; hint "e.g., chicken breast, apple". Empty query with no filter → toast "Please enter a search term or select a filter". Status line: "Searching..." (button disabled), "No results found", `"N results from local|USDA in category: X|in group: Y - tap to select"`, `"Search failed: msg"`. Filter changes re-run search even with empty query (browse a whole category).
39. Loading state: "Loading food database..." until async load; load the JSON asynchronously.
40. Result row: name; second line brand/categoryGroup (hidden if blank); third `"N cal | P: x.xg | C: x.xg | F: x.xg"`.
41. After selecting a result: **food name is an editable field** prefilled with description ("Food Name", hint "Enter food name"); status "Selected: NAME"; label "Food Details"; toast "Food selected"; manual fields prefilled with primary-serving values.
42. "Edit Nutrition Manually" / "Hide Manual Entry" toggle on the same screen (header "Or edit nutrition manually:", fields Calories / Protein (g) / Carbs (g) / Fat (g), hints "0"). When visible, manual values override the selected food and micros are zeroed.
43. Validation toasts: "Please enter food name"; "Please enter a valid amount" (amount ≤ 0); success "Food saved successfully". Buttons "Save"/"Cancel".
44. Quantity string: amount == 1 → `"${grams}g"` (or "1 serving" if grams ≤ 0); else `"${amount.toInt()} x ${grams}g (${totalGrams}g total)"`.
45. Manual entry: Android manual mode has no free-text Quantity; conversion for add-to-database uses `amount × grams / 100`. "Save as favorite" works in manual mode too.
46. "Save as favorite (quick add later)" button (gray #6b7280): dialog "Save as favorite" / "Give this meal a short name so you can add it quickly later." / hint "e.g. My usual breakfast" / Save/Cancel; empty → "Please enter a name"; duplicate **label** → "Already in favorites. Use a different name or delete the existing one first."; else "Saved as favorite".
47. "Add to database (searchable next time)" button (#059669): requires name ("Enter a food name first"); from a selected result copies per-100g values; from manual converts by factor; duplicate **name** → "This food is already in your database."; success "Added to database. You can search for it next time."
48. Add-to-meal mode: title "Add food to meal", positive "Add to meal", favorite/database buttons hidden, toast "Added to meal".

### K. Favorites ("Add from favorites")
**Matches:** favorites store 23 nutrients; `toFoodEntry` copies them; category override on add.
49. Single button "Add from favorites" (gray #E5E7EB) → one dialog "Add from favorites" listing foods as `"${label} (${mealCategory})"` then meals as `"Meal: ${label} (${mealCategory})"`; neutral button "Delete a favorite or meal"; "Cancel".
50. Empty → toast "No favorites yet. Add a food (or create a meal) and save as favorite."
51. Category picker dialog "Add to which meal?", single-choice Breakfast/Lunch/Dinner/Snacks **defaulting to the favorite's own mealCategory**, Add/Cancel, then toast `"Added to $cat: $label"`. Meals: "Add meal to which category?" default `meal.mealCategory`, toast `"Added N items to $cat: $label"`, "This meal has no items." guard.
52. Delete flow: "Delete a favorite or meal" list → confirm "Delete favorite?" / `Remove "label" from your favorites? This does not remove it from your food log.` → toast "Removed from favorites"; meals "Delete meal?" / `Remove meal "label" from your favorites? This does not remove past log entries.` → "Meal removed from favorites"; empty → "No favorites or meals to delete."
53. `FavoriteMealSheet.addMealToLog` copies only 4 macros — must copy all 23.

### L. Create meal
54. Button text "Create meal (add to favorites)".
55. "Add food to meal" opens **AddFoodDialog in add-to-meal mode** (full DB search/serving math).
56. Copy: dialog title "Create meal"; "Meal name" hint "e.g. My usual breakfast"; "Meal category"; "Foods in this meal" with hint `Tap "Add food to meal" to add items`; item row `"${name} - ${quantity} (${cal} cal)"` + "Remove"; buttons "Add food to meal", "Save meal as favorite".
57. Validation toasts "Enter a meal name", "Add at least one food to the meal"; duplicate label → "A meal with this name already exists. Use a different name."; success "Meal saved as favorite".

### M. Hydration / water
**Matches:** 64 oz goal; quick amounts 8/12/16; custom oz; entries list with time + delete; progress green at ≥100% else blue.
58. Everything **inline in one card** on the day view: water icon + "Hydration Tracker" (16sp bold) + total `"N oz"` (18sp bold #3b82f6) right; 12dp progress + `"/ 64 oz"`; buttons "+8 oz", "+12 oz", "+16 oz" (#dbeafe bg / #1d4ed8 text, bold) and "+" (#3b82f6, 20sp).
59. Custom dialog: title "Add Water", message "How many ounces did you drink?", hint "Enter amount in oz", Add/Cancel; invalid → toast "Please enter a valid amount".
60. Every add toasts `"+N oz added"`.
61. Entries header "Today's Drinks (N)"; amount text colored primary bold.

### N. Exercise
**Matches:** type list; "Other" reveals custom field; dialog title "Add Exercise"; row `type` + "N min" green.
62. Card always visible: "Exercise Minutes", big value `"N min"` (28sp bold; "0 min"), button "Log Exercise" (#10b981), list below; card tap also opens dialog.
63. Labels/hints: "Exercise Type", "Custom Exercise Type" hint "Enter exercise type", "Minutes" hint "Enter minutes".
64. Validation toasts: "Please enter exercise type", "Please enter minutes", "Please enter a valid number of minutes" (rejects ≤ 0); success "Exercise saved successfully".
65. iOS `ExerciseEntry.customType` is iOS-only; Android stores the custom text in `type` (keep iOS field but store custom text in `type` too).

### O. Oxygen
**Matches:** integer SpO2; day list newest first; latest reading headline.
66. Card always visible: "Oxygen Saturation", value `"N%"` of the latest reading (by date) or "N/A", button "Log Reading" (#10b981), label "Today's Readings (N)" (hidden when none), rows `"N%"` **green** bold + time `"h:mm:ss a"`.
67. Dialog: title "Add Oxygen Reading", label "Oxygen Saturation Level (%)", hint "Enter oxygen level (0-100)". Remove iOS "Normal range" note.
68. Validation toasts "Please enter oxygen level", "Please enter a valid oxygen level (0-100)" (0 and 100 accepted); success "Oxygen reading saved successfully".

### P. Weight (incl. profile weight sync)
**Matches:** Current/Goal radio; lbs; latest current + latest goal shown regardless of date; week/month start-end-change.
69. Dialog: title "Add Weight"; radios "Current Weight" (checked) / "Goal Weight"; label "Weight (lbs)"; hint "Enter weight" → "Enter current weight"/"Enter goal weight". Toasts "Please enter weight", "Please enter a valid weight"; success "Weight saved successfully". Remove iOS "Recording for <date>" section.
70. **Profile sync on save**: after saving a non-goal weight, write pref `weight` (1 decimal, integer when whole) and `last_updated` = "MMM d, yyyy". Also sync after Health import.
71. Card: "Weight" + value `"${weight} lbs"` (24sp bold) or "N/A"; `"Goal: X lbs"` (#10b981) **hidden when no goal**; "Tap to log" (#2563eb); then a **per-selected-day list** newest first: `"Goal"`/`"Current"` (bold, green/primary) + `"  X lbs"`, second line time `"h:mm a"`. Goal comes only from the latest goal entry (drop `profile.goalWeight`).
72. Keep `%.1f` formatting.
73. Profile editing weight inserts a current `WeightEntry`; `syncProfileAndDbWeight` on profile load.

### Q. Steps / Heart Rate / device health import
**Matches:** imports oxygen, weight, exercise (≥1 min), steps per day, heart rate.
74. Section header "COPD Fuel" (14sp bold), outlined button "Import All from Device", link "How does this work?" → dialog "Import All from Device" with the explainer (strings.xml:39, adapted to Apple Health).
75. Import flow: rationale dialog (strings:40/41 adapted) before permission; fixed **30 days**; zero → toast (strings:46 adapted); success toast `"Imported: %d oxygen, %d weight, %d exercise, %d steps, %d heart rate from your device."`; no exercise but other data → extra toast (strings:47 adapted); denied → toast (strings:43 adapted). Remove iOS period picker and inline result panel.
76. **Dedupe on import**: oxygen/HR skipped when same timestamp exists; steps replaced for that day.
77. Steps card always visible "Steps"; value = live HealthKit read for the selected day when authorized, else stored sum; "N/A" when 0. Remove iOS-only Steps/Heart Rate TrackingCards that open the import sheet.
78. Heart rate card: "Heart Rate", value `"N bpm"` (latest by date) or "N/A"; list of **10 most recent** rows `"N bpm"` + `"h:mm a"`.
79. HealthKit workout names — keep (iOS improvement).

### R. Medications
**Matches:** model; confirm dialogs "Discontinue Medication" / "Remove Medication"; discontinued row date; add dialog title, Type Daily/Exacerbation.
80. Tracking card: "Medications" (16sp bold), count `"N daily"` (24sp bold), "Tap to manage" (#2563eb), inline "Daily Medications" label + rows `"${name} - ${dosage}"` (gray bg) and "Exacerbation Medications" label + rows (labels hidden when empty).
81. Manage screen: dialog titled "Medications" with "Close"; sections "Daily Medications", "Exacerbation Medications", empty text **"None added. Tap Add Medication below."**, visible **"Add Medication"** button, section "**Discontinued Medications**" (hidden when empty). Rows `"${name} – ${dosage} (${frequency})"` with two always-visible icon buttons: discontinue (orange, "Discontinue medication") and delete (gray, "Remove medication"); discontinued rows gray with `"\nDiscontinued: <date>"` (or "Unknown date") and delete only.
82. Ordering: active lists **newest first** (`date DESC`); discontinued by `discontinuedDate DESC`. Add `date` (creation timestamp) to iOS `Medication`.
83. Add dialog: labels "Type", "Medication Name" hint "Enter name", "Dosage" hint "e.g., 2 puffs", "Frequency" hint "e.g., twice daily"; **only name is required** (toast "Enter medication name"); success toast "Medication saved".

### S. Data-layer / persistence notes
84. Uniqueness rules: favorite by `label`, user-added food by `name`, favorite meal by `label`.
85. Pref keys read by Tracking: `weight`, `activity_level`, `protein_target` (Float), `exercise_journal_log`.
86. Food list ordering within a meal section: insertion order is acceptable.

### T. iOS-only extras (remove unless the P3 spec says keep)
- "Today's Summary"/"Day Summary" 6-tile grid and week/month Summary tiles.
- Per-week `WeeklyExerciseCard`s in month view.
- Separate TrackingCards for Steps and Heart Rate; "APPLE HEALTH" section; import period picker; inline import result panel. (Keep HealthKit workout name mapping.)
- Oxygen "Normal range: 95-100%" note; weight "Recording for <date>" section; goal weight written to profile; GlobalWeightCard "Goal / Not set" and "Last entry" rows.
- Manual entry free-text "Quantity" field; "Add to favorites" toggle in confirm view; categoryGroup chip on search rows.
- Separate "Favorite Meals" sheet and inline item entry in `CreateFavoriteMealView`.
- Medication swipe-only actions.
