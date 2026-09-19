# iOS Parity P3.C (2/2): Add Food dialog, favorites, create meal, food DB decoding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the pre-P3 iOS food modals with Android-parity flows: the `AddFoodDialog` (source toggle, filters, explicit Search, editable name, DB serving sizes, manual override, save-as-favorite, add-to-database, add-to-meal mode), the "Add from favorites" dialogs (foods + meals, category picker, delete flows), and "Create meal"; decode all 23 nutrients and `servingSizes` from the bundled food database; enforce Android's uniqueness rules.

**Architecture:** Three new view files under `Views/Tracking/` (`AddFoodDialog.swift`, `FavoritesDialogs.swift`, `CreateMealDialog.swift`) replace `LegacyFoodModals.swift`. `FoodDatabaseService` becomes an `ObservableObject` that loads the JSON off the main thread and decodes the full Android `LocalFood` schema; `FoodSearchResult` gains USDA serving/brand fields. `DataManager` insert helpers return `Bool` for the label/name uniqueness rules. The `+ Quick Add Food` button opens `AddFoodDialog` directly; P3.D wraps it in the four-option sheet and adds the scan icon button.

**Tech Stack:** SwiftUI, Foundation JSONDecoder, URLSession (USDA), iOS 17.

**Spec:** `docs/superpowers/specs/2026-09-19-ios-parity-p3-design.md` §3 row P3.C. Gap list: `docs/superpowers/specs/2026-09-19-ios-parity-p3/audit-tracking.md` §I (30 only — sheet is P3.D), §J 32–48, §K 49–53, §L 54–57, §S 84. Android sources: `ui/dialogs/AddFoodDialog.kt`, `res/layout/dialog_add_food.xml`, `ui/fragments/TrackingFragment.kt:2004-2131`, `ui/dialogs/CreateMealDialog.kt`, `res/layout/dialog_create_meal.xml`, `data/FoodDatabaseHelper.kt:150-217`, `repository/DataRepository.kt:26-56`.

## Global Constraints

- Copy verbatim; `Toast` → `ToastCenter.shared.show`; `AlertDialog` → `.alert`/sheets (spec §1.1). Entries are stamped on the selected day via `TrackingDates.timestamp(on:)` (spec §1.3).
- Uniqueness (audit 84): favorite by `label`, user-added food by `name`, favorite meal by `label`.
- Keep the iOS 23-nutrient USDA mapping (additive); drop the `"desc — brand"` naming (brand becomes a separate second line, audit J34/40).
- Depends on P3.0 (`ToastCenter`), P3.C (1/2) (`TrackingDayView`, `TrackingDates`, `TrackingButton`).
- Build then one commit at the end; `/usr/bin/git`; commands from the parent repo root.

---

### Task 1: Food database decoding + service shape (audit J32, J33, J39)

**Files:**
- Modify: `COPDFuel/COPDFuel/Models/FoodSearchResult.swift` (add `brandOwner`, `servingSize`, `servingUnit`)
- Modify: `COPDFuel/COPDFuel/Services/FoodDatabaseService.swift` (full `DBFood` schema incl. `servingSizes`; `ObservableObject` async load; USDA fields)

**Interfaces (produced):**
- `FoodSearchResult.brandOwner: String?`, `.servingSize: Double?`, `.servingUnit: String?` (init defaults nil).
- `FoodDatabaseService: ObservableObject` with `@Published private(set) var isLoaded`, `func loadAsync()`, existing `search(query:category:foodGroup:limit:userAdded:)`, `getCategories()`, `getFoodGroups()`, `totalFoodsCount`, `searchUSDA(query:)`.

- [ ] **Step 1: Extend `FoodSearchResult`**

Add three stored properties after `servingSizes`:
```swift
    /// USDA branded foods: brand owner shown on its own line (Android FoodSearchAdapter).
    let brandOwner: String?
    /// USDA serving size + unit for the fallback serving options (AddFoodDialog.kt:461-477).
    let servingSize: Double?
    let servingUnit: String?
```
Add matching init parameters at the end `brandOwner: String? = nil, servingSize: Double? = nil, servingUnit: String? = nil` and assignments.

- [ ] **Step 2: Rewrite the top of `FoodDatabaseService.swift`**

Add `import Combine` under `import Foundation` (the project enables MemberImportVisibility; every `ObservableObject` file must import Combine explicitly). Then replace lines 10-58 (class header through `loadIfNeeded`) with:
```swift
final class FoodDatabaseService: ObservableObject {
    static let shared = FoodDatabaseService()

    private struct LoadedDB: Decodable {
        let totalFoods: Int?
        let foods: [DBFood]?
        let categories: [String]?
        let foodGroups: [String]?

        struct DBServing: Decodable {
            let label: String?
            let grams: Double?
            let amount: Double?
            let unit: String?
            let isPrimary: Bool?
            let isCustom: Bool?
        }

        /// Android FoodDatabaseHelper.LocalFood (all 23 nutrients + servingSizes).
        struct DBFood: Decodable {
            let id: Int?
            let name: String?
            let shortName: String?
            let category: String?
            let categoryGroup: String?
            let calories: Double?
            let protein: Double?
            let carbs: Double?
            let fat: Double?
            let fiber: Double?
            let sodium: Double?
            let potassium: Double?
            let calcium: Double?
            let iron: Double?
            let magnesium: Double?
            let zinc: Double?
            let selenium: Double?
            let manganese: Double?
            let water: Double?
            let vitaminA: Double?
            let vitaminC: Double?
            let vitaminD: Double?
            let vitaminE: Double?
            let vitaminK: Double?
            let saturatedFat: Double?
            let cholesterol: Double?
            let omega3: Double?
            let addedSugars: Double?
            let portionSize: Double?
            let portionUnit: String?
            let portionDesc: String?
            let servingSizes: [DBServing]?
        }
    }

    @Published private(set) var isLoaded = false

    private var cachedFoods: [LoadedDB.DBFood] = []
    private var loadAttempted = false
    private var cachedCategories: [String] = []
    private var cachedFoodGroups: [String] = []
    private var nameIndex: [String: [Int]] = [:]
    private var foodArray: [LoadedDB.DBFood] = []
    private let loadQueue = DispatchQueue(label: "copdfuel.fooddb.load", qos: .userInitiated)

    private init() {}

    /// Synchronous load (used by search paths and the report).
    func loadIfNeeded() {
        guard !loadAttempted else { return }
        loadAttempted = true
        guard let url = Bundle.main.url(forResource: "food_database", withExtension: "json", subdirectory: nil),
              let data = try? Data(contentsOf: url),
              let decoded = try? JSONDecoder().decode(LoadedDB.self, from: data),
              let foods = decoded.foods else { return }
        cachedFoods = foods
        cachedCategories = decoded.categories ?? []
        cachedFoodGroups = decoded.foodGroups ?? []
        buildSearchIndex()
        DispatchQueue.main.async { self.isLoaded = true }
    }

    /// Android AddFoodDialog loads the DB on a coroutine and shows
    /// "Loading food database..." meanwhile.
    func loadAsync() {
        if isLoaded { return }
        loadQueue.async { [weak self] in
            self?.loadIfNeeded()
        }
    }
```
(`buildSearchIndex`, `extractWords`, `getCategories`, `getFoodGroups` stay as they are.)

- [ ] **Step 3: Map every nutrient + serving sizes in `search`**

Replace the `FoodSearchResult(...)` construction for DB foods (lines 195-210) with:
```swift
                result: FoodSearchResult(
                    id: "\(food.id ?? 0)",
                    name: name,
                    shortName: short,
                    category: food.category ?? "",
                    categoryGroup: food.categoryGroup ?? "",
                    calories: food.calories ?? 0,
                    protein: food.protein ?? 0,
                    carbs: food.carbs ?? 0,
                    fat: food.fat ?? 0,
                    fiber: food.fiber ?? 0,
                    sodium: food.sodium ?? 0,
                    potassium: food.potassium ?? 0,
                    calcium: food.calcium ?? 0,
                    iron: food.iron ?? 0,
                    magnesium: food.magnesium ?? 0,
                    zinc: food.zinc ?? 0,
                    selenium: food.selenium ?? 0,
                    manganese: food.manganese ?? 0,
                    water: food.water ?? 0,
                    vitaminA: food.vitaminA ?? 0,
                    vitaminC: food.vitaminC ?? 0,
                    vitaminD: food.vitaminD ?? 0,
                    vitaminE: food.vitaminE ?? 0,
                    vitaminK: food.vitaminK ?? 0,
                    saturatedFat: food.saturatedFat ?? 0,
                    cholesterol: food.cholesterol ?? 0,
                    omega3: food.omega3 ?? 0,
                    addedSugars: food.addedSugars ?? 0,
                    portionDesc: food.portionDesc ?? "100g",
                    isUserAdded: false,
                    servingSizes: (food.servingSizes ?? []).map {
                        FoodServingOption(label: $0.label ?? "", grams: $0.grams ?? 100, isCustom: $0.isCustom ?? false)
                    }
                ),
```
And the user-added construction (lines 169-184) with the full field list:
```swift
                    result: FoodSearchResult(
                        id: "u-\(u.id.uuidString)",
                        name: u.name, shortName: u.shortName, category: u.category, categoryGroup: u.categoryGroup,
                        calories: u.calories, protein: u.protein, carbs: u.carbs, fat: u.fat,
                        fiber: u.fiber, sodium: u.sodium, potassium: u.potassium, calcium: u.calcium, iron: u.iron,
                        magnesium: u.magnesium, zinc: u.zinc, selenium: u.selenium, manganese: u.manganese,
                        water: u.water, vitaminA: u.vitaminA, vitaminC: u.vitaminC, vitaminD: u.vitaminD,
                        vitaminE: u.vitaminE, vitaminK: u.vitaminK, saturatedFat: u.saturatedFat,
                        cholesterol: u.cholesterol, omega3: u.omega3, addedSugars: u.addedSugars,
                        portionDesc: u.portionDesc, isUserAdded: true
                    ),
```
Delete the two dead duplicates `scoreFoodFast(_:words:hasQuery:)` (223-263), `scoreUserAddedFast(_:words:hasQuery:)` (265-290) and `matchesUserAdded(_:words:category:foodGroup:hasQuery:)` (293-314); keep the `searchWords:` variants.

- [ ] **Step 4: USDA result shape**

In `searchUSDA`, replace `let portion = ...` and the `FoodSearchResult(` `name:`/`portionDesc:` lines so that:
```swift
            let portion = "\(Int(servingSize))\(servingUnit)"
            ...
                name: name,
                shortName: name,
                category: "USDA",
                categoryGroup: brand ?? "",
                ... (nutrients unchanged) ...
                portionDesc: portion,
                isUserAdded: false,
                servingSizes: [],
                brandOwner: brand,
                servingSize: servingSize,
                servingUnit: servingUnit
```

---

### Task 2: `DataManager` uniqueness rules (audit 84)

**Files:**
- Modify: `COPDFuel/COPDFuel/Services/DataManager.swift`

- [ ] **Step 1: Make the three inserts return `Bool`**

```swift
    /// Android DataRepository.insertFavoriteFood: false when a favorite with the same label exists.
    @discardableResult
    func addFavoriteFood(_ food: FavoriteFood) -> Bool {
        guard !favoriteFoods.contains(where: { $0.label == food.label }) else { return false }
        favoriteFoods.append(food)
        saveFavoriteFoods()
        return true
    }

    /// Android DataRepository.insertUserAddedFood: false when a food with the same name exists.
    @discardableResult
    func addUserAddedFood(_ food: UserAddedFood) -> Bool {
        guard !userAddedFoods.contains(where: { $0.name == food.name }) else { return false }
        userAddedFoods.append(food)
        saveUserAddedFoods()
        return true
    }

    /// Android DataRepository.insertFavoriteMeal: false when a meal with the same label exists.
    @discardableResult
    func addFavoriteMeal(_ meal: FavoriteMeal) -> Bool {
        guard !favoriteMeals.contains(where: { $0.label == meal.label }) else { return false }
        favoriteMeals.append(meal)
        saveFavoriteMeals()
        return true
    }
```
(Replace the existing three functions; `removeFavoriteFood`/`removeFavoriteMeal` stay.)

---

### Task 3: `AddFoodDialog` (audit J)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Tracking/AddFoodDialog.swift`

**Interfaces:**
- Produces: `AddFoodDialog(selectedDate: Date, onAddToMeal: ((FoodEntry) -> Void)? = nil)`. When `onAddToMeal` is non-nil: title "Add food to meal", positive "Add to meal", favorite/database buttons hidden, toast "Added to meal".
- P3.D adds `scanEntry: FoodEntry?` prefill (`prefillFromScan`) and the scan icon button.

- [ ] **Step 1: Create the file**

```swift
//
//  AddFoodDialog.swift
//  COPDFuel
//
//  Port of Android AddFoodDialog.kt + dialog_add_food.xml: meal category,
//  Local/USDA source, Category/Food Group filters, explicit Search, result
//  list, editable Food Name, Amount, Serving Size, nutrition preview,
//  manual entry override, Save as favorite, Add to database, and the
//  add-to-meal mode used by Create meal.
//

import SwiftUI

struct AddFoodDialog: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var dataManager = DataManager.shared
    @ObservedObject private var foodDB = FoodDatabaseService.shared

    let selectedDate: Date
    var onAddToMeal: ((FoodEntry) -> Void)? = nil

    enum Source { case local, usda }
    enum FilterType { case category, foodGroup }
    struct ServingOption: Identifiable, Equatable {
        let label: String
        let grams: Double
        let multiplier: Double
        var id: String { label }
    }

    static let mealCategories = ["Breakfast", "Lunch", "Dinner", "Snacks"]

    @State private var mealCategory = "Breakfast"
    @State private var source: Source = .local
    @State private var filterType: FilterType = .category
    @State private var categoryFilter = "All Categories"
    @State private var foodGroupFilter = "All Food Groups"
    @State private var query = ""
    @State private var status = "Loading food database..."
    @State private var isSearching = false
    @State private var results: [FoodSearchResult] = []
    @State private var showResults = false

    @State private var selectedFood: FoodSearchResult?
    @State private var foodName = ""
    @State private var amount = "1"
    @State private var servingOptions: [ServingOption] = [
        ServingOption(label: "1 serving", grams: 100, multiplier: 1),
        ServingOption(label: "100g", grams: 100, multiplier: 1)
    ]
    @State private var selectedServing = "1 serving"

    @State private var showManual = false
    @State private var manualCalories = ""
    @State private var manualProtein = ""
    @State private var manualCarbs = ""
    @State private var manualFat = ""

    @State private var showingFavoritePrompt = false
    @State private var favoriteLabel = ""
    @State private var pendingFavoriteEntry: FoodEntry?

    private var isAddToMealMode: Bool { onAddToMeal != nil }
    private var localCount: Int { foodDB.totalFoodsCount + dataManager.userAddedFoods.count }
    private var currentServing: ServingOption? { servingOptions.first { $0.label == selectedServing } }
    private var amountValue: Double { Double(amount.trimmingCharacters(in: .whitespaces)) ?? 1.0 }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    fieldLabel("Meal Category")
                    Picker("Meal Category", selection: $mealCategory) {
                        ForEach(Self.mealCategories, id: \.self) { Text($0).tag($0) }
                    }
                    .pickerStyle(.segmented)

                    sectionTitle("Search Food Database")
                    sourceToggle
                    if source == .local && foodDB.isLoaded { filterBlock }
                    searchRow
                    Text(status)
                        .font(.system(size: 12))
                        .foregroundColor(Color(hex: "6b7280"))
                    if showResults { resultsList }

                    sectionTitle("Food Details")
                    if let selectedFood, !selectedFood.name.isEmpty {
                        Text(selectedFood.name)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(Color(hex: "2563eb"))
                    }
                    fieldLabel("Food Name")
                    TextField("Enter food name", text: $foodName).textFieldStyle(.roundedBorder)
                    HStack(alignment: .top, spacing: 12) {
                        VStack(alignment: .leading, spacing: 4) {
                            fieldLabel("Amount")
                            TextField("1", text: $amount)
                                .keyboardType(.decimalPad)
                                .textFieldStyle(.roundedBorder)
                        }
                        .frame(width: 100)
                        VStack(alignment: .leading, spacing: 4) {
                            fieldLabel("Serving Size")
                            Picker("Serving Size", selection: $selectedServing) {
                                ForEach(servingOptions) { Text($0.label).tag($0.label) }
                            }
                            .pickerStyle(.menu)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }

                    fieldLabel("Nutrition (per serving)")
                    nutritionPreview

                    if showManual { manualSection }
                    Button(showManual ? "Hide Manual Entry" : "Edit Nutrition Manually") { showManual.toggle() }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Color(hex: "2563eb"))

                    if !isAddToMealMode {
                        TrackingButton(title: "Save as favorite (quick add later)", background: Color(hex: "6b7280"), action: saveAsFavoriteTapped)
                        TrackingButton(title: "Add to database (searchable next time)", background: Color(hex: "059669"), action: addToDatabase)
                    }
                }
                .padding(20)
            }
            .navigationTitle(isAddToMealMode ? "Add food to meal" : "Add Food")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isAddToMealMode ? "Add to meal" : "Save", action: positiveTapped)
                }
            }
            .onAppear {
                foodDB.loadAsync()
                if foodDB.isLoaded { status = "Search from local database (\(localCount) foods)" }
            }
            .onChange(of: foodDB.isLoaded) { _, loaded in
                if loaded && source == .local { status = "Search from local database (\(localCount) foods)" }
            }
            .alert("Save as favorite", isPresented: $showingFavoritePrompt) {
                TextField("e.g. My usual breakfast", text: $favoriteLabel)
                Button("Save", action: saveFavorite)
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Give this meal a short name so you can add it quickly later.")
            }
        }
        .toastOverlay()
    }

    // MARK: Pieces

    private func sectionTitle(_ text: String) -> some View {
        Text(text).font(.system(size: 16, weight: .bold)).foregroundColor(Color(hex: "1f2937")).padding(.top, 8)
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text).font(.system(size: 14, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
    }

    /// databaseToggle: clears results and resets the status line.
    private var sourceToggle: some View {
        Picker("Source", selection: $source) {
            Text("Local (18,563 foods)").tag(Source.local)
            Text("USDA Online").tag(Source.usda)
        }
        .pickerStyle(.segmented)
        .onChange(of: source) { _, new in
            results = []
            showResults = false
            status = new == .local
                ? "Search from local database (\(foodDB.isLoaded ? localCount : 0) foods)"
                : "Search USDA FoodData Central online"
        }
    }

    private var filterBlock: some View {
        VStack(alignment: .leading, spacing: 6) {
            fieldLabel("Filter by")
            Picker("Filter by", selection: $filterType) {
                Text("Category").tag(FilterType.category)
                Text("Food Group").tag(FilterType.foodGroup)
            }
            .pickerStyle(.segmented)
            .onChange(of: filterType) { _, _ in performSearch() }
            if filterType == .category {
                Picker("Category", selection: $categoryFilter) {
                    ForEach(["All Categories"] + foodDB.getCategories(), id: \.self) { Text($0).tag($0) }
                }
                .pickerStyle(.menu)
                .onChange(of: categoryFilter) { _, _ in performSearch() }
            } else {
                Picker("Food Group", selection: $foodGroupFilter) {
                    ForEach(["All Food Groups"] + foodDB.getFoodGroups(), id: \.self) { Text($0).tag($0) }
                }
                .pickerStyle(.menu)
                .onChange(of: foodGroupFilter) { _, _ in performSearch() }
            }
        }
    }

    private var searchRow: some View {
        HStack(spacing: 8) {
            TextField("e.g., chicken breast, apple", text: $query)
                .textFieldStyle(.roundedBorder)
                .submitLabel(.search)
                .onSubmit(performSearch)
            Button("Search", action: performSearch)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(Color(hex: "2563eb"))
                .cornerRadius(8)
                .disabled(isSearching)
        }
    }

    /// item_food_search_result: name; brand/categoryGroup (hidden if blank);
    /// "N cal | P: x.xg | C: x.xg | F: x.xg".
    private var resultsList: some View {
        VStack(spacing: 6) {
            ForEach(results) { r in
                Button(action: { select(r) }) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(r.name)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(Color(hex: "1f2937"))
                        let second = r.brandOwner ?? r.categoryGroup
                        if !second.isEmpty {
                            Text(second).font(.system(size: 12)).foregroundColor(Color(hex: "6b7280"))
                        }
                        Text(String(format: "%d cal | P: %.1fg | C: %.1fg | F: %.1fg", Int(r.calories), r.protein, r.carbs, r.fat))
                            .font(.system(size: 12))
                            .foregroundColor(Color(hex: "4b5563"))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(10)
                    .background(Color(hex: "f8fafc"))
                    .cornerRadius(8)
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxHeight: 320)
    }

    private var nutritionPreview: some View {
        let food = selectedFood
        let mult = (currentServing?.multiplier ?? 1) * amountValue
        return HStack(spacing: 8) {
            previewBox("Calories", food.map { "\(Int($0.calories * mult))" } ?? "0")
            previewBox("Protein", food.map { String(format: "%.1fg", $0.protein * mult) } ?? "0g")
            previewBox("Carbs", food.map { String(format: "%.1fg", $0.carbs * mult) } ?? "0g")
            previewBox("Fat", food.map { String(format: "%.1fg", $0.fat * mult) } ?? "0g")
        }
    }

    private func previewBox(_ label: String, _ value: String) -> some View {
        VStack(spacing: 2) {
            Text(label).font(.system(size: 11)).foregroundColor(Color(hex: "6b7280"))
            Text(value).font(.system(size: 14, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
        }
        .frame(maxWidth: .infinity)
        .padding(8)
        .background(Color(hex: "f8fafc"))
        .cornerRadius(8)
    }

    private var manualSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Or edit nutrition manually:").font(.system(size: 14, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
            manualField("Calories", $manualCalories)
            manualField("Protein (g)", $manualProtein)
            manualField("Carbs (g)", $manualCarbs)
            manualField("Fat (g)", $manualFat)
        }
    }

    private func manualField(_ label: String, _ text: Binding<String>) -> some View {
        HStack {
            Text(label).font(.system(size: 14)).frame(width: 110, alignment: .leading)
            TextField("0", text: text).keyboardType(.decimalPad).textFieldStyle(.roundedBorder)
        }
    }

    // MARK: Search (performSearch :322-371)

    private func performSearch() {
        let q = query.trimmingCharacters(in: .whitespaces)
        if q.isEmpty && categoryFilter == "All Categories" && foodGroupFilter == "All Food Groups" {
            ToastCenter.shared.show("Please enter a search term or select a filter")
            return
        }
        status = "Searching..."
        isSearching = true
        Task {
            var found: [FoodSearchResult] = []
            var failure: String?
            if source == .local {
                let category = (filterType == .category && categoryFilter != "All Categories") ? categoryFilter : nil
                let group = (filterType == .foodGroup && foodGroupFilter != "All Food Groups") ? foodGroupFilter : nil
                found = foodDB.search(query: q, category: category, foodGroup: group, limit: Int.max, userAdded: dataManager.userAddedFoods)
            } else {
                do {
                    found = try await foodDB.searchUSDA(query: q)
                } catch FoodDatabaseService.USDASearchError.missingAPIKey {
                    found = []   // Android: blank key → empty list → "No results found"
                } catch {
                    failure = error.localizedDescription
                }
            }
            await MainActor.run {
                isSearching = false
                if let failure {
                    status = "Search failed: \(failure)"
                    showResults = false
                } else if found.isEmpty {
                    status = "No results found"
                    showResults = false
                } else {
                    let filterInfo: String
                    if filterType == .category && categoryFilter != "All Categories" {
                        filterInfo = " in category: \(categoryFilter)"
                    } else if filterType == .foodGroup && foodGroupFilter != "All Food Groups" {
                        filterInfo = " in group: \(foodGroupFilter)"
                    } else {
                        filterInfo = ""
                    }
                    status = "\(found.count) results from \(source == .local ? "local" : "USDA")\(filterInfo) - tap to select"
                    results = found
                    showResults = true
                }
            }
        }
    }

    // MARK: Selection (selectFoodResult :444-506)

    private func select(_ result: FoodSearchResult) {
        foodName = result.name
        selectedFood = result
        if !result.servingSizes.isEmpty {
            servingOptions = result.servingSizes.map { ss in
                ServingOption(label: ss.isCustom ? "g (enter amount)" : "\(ss.label) - \(Int(ss.grams))g",
                              grams: ss.grams, multiplier: ss.grams / 100.0)
            }
        } else {
            let desc = result.servingUnit ?? "1 serving"
            let grams = result.servingSize ?? 100.0
            var options = [ServingOption(label: "\(desc) - \(Int(grams))g", grams: grams, multiplier: grams / 100.0)]
            if grams != 100.0 { options.append(ServingOption(label: "100g", grams: 100, multiplier: 1)) }
            options.append(ServingOption(label: "g (enter amount)", grams: 1, multiplier: 0.01))
            servingOptions = options
        }
        selectedServing = servingOptions[0].label
        amount = "1"
        let primary = servingOptions[0].multiplier
        manualCalories = "\(Int(result.calories * primary))"
        manualProtein = String(format: "%.1f", result.protein * primary)
        manualCarbs = String(format: "%.1f", result.carbs * primary)
        manualFat = String(format: "%.1f", result.fat * primary)
        showResults = false
        status = "Selected: \(result.name)"
        ToastCenter.shared.show("Food selected")
    }

    // MARK: Entry construction (buildCurrentFoodEntry :533-589)

    private func buildEntry(date: Date) -> FoodEntry? {
        let name = foodName.trimmingCharacters(in: .whitespacesAndNewlines)
        if name.isEmpty { ToastCenter.shared.show("Please enter food name"); return nil }
        let amt = amountValue
        if amt <= 0 { ToastCenter.shared.show("Please enter a valid amount"); return nil }
        let serving = currentServing
        let servingLabel = (serving.map { $0.grams > 0 ? "\(Int($0.grams))g" : "1 serving" }) ?? "1 serving"
        let multiplier = serving?.multiplier ?? 1.0
        let servingGrams = serving?.grams ?? 100.0
        let totalGrams = Int(servingGrams * amt)
        let quantity = amt == 1.0 ? servingLabel : "\(Int(amt)) x \(servingLabel) (\(totalGrams)g total)"

        if showManual || selectedFood == nil {
            return FoodEntry(date: date, mealType: mealCategory, foodName: name, quantity: quantity,
                             calories: Double(manualCalories) ?? 0, protein: Double(manualProtein) ?? 0,
                             carbs: Double(manualCarbs) ?? 0, fat: Double(manualFat) ?? 0)
        }
        let f = selectedFood!
        let k = multiplier * amt
        return FoodEntry(date: date, mealType: mealCategory, foodName: name, quantity: quantity,
                         calories: f.calories * k, protein: f.protein * k, carbs: f.carbs * k, fat: f.fat * k,
                         fiber: f.fiber * k, sodium: f.sodium * k, potassium: f.potassium * k, calcium: f.calcium * k,
                         iron: f.iron * k, magnesium: f.magnesium * k, zinc: f.zinc * k, selenium: f.selenium * k,
                         manganese: f.manganese * k, water: f.water * k, vitaminA: f.vitaminA * k, vitaminC: f.vitaminC * k,
                         vitaminD: f.vitaminD * k, vitaminE: f.vitaminE * k, vitaminK: f.vitaminK * k,
                         saturatedFat: f.saturatedFat * k, cholesterol: f.cholesterol * k, omega3: f.omega3 * k,
                         addedSugars: f.addedSugars * k)
    }

    private func positiveTapped() {
        if let onAddToMeal {
            guard let entry = buildEntry(date: Date()) else { return }
            onAddToMeal(entry)
            ToastCenter.shared.show("Added to meal")
            dismiss()
        } else {
            guard let entry = buildEntry(date: TrackingDates.timestamp(on: selectedDate)) else { return }
            dataManager.addFoodEntry(entry)
            ToastCenter.shared.show("Food saved successfully")
            dismiss()
        }
    }

    // MARK: Save as favorite (:737-794)

    private func saveAsFavoriteTapped() {
        guard let entry = buildEntry(date: Date()) else { return }
        pendingFavoriteEntry = entry
        favoriteLabel = ""
        showingFavoritePrompt = true
    }

    private func saveFavorite() {
        guard let e = pendingFavoriteEntry else { return }
        let label = favoriteLabel.trimmingCharacters(in: .whitespacesAndNewlines)
        if label.isEmpty { ToastCenter.shared.show("Please enter a name"); return }
        let favorite = FavoriteFood(
            label: label, name: e.foodName, mealCategory: e.mealType, quantity: e.quantity,
            calories: e.calories ?? 0, protein: e.protein ?? 0, carbs: e.carbs ?? 0, fat: e.fat ?? 0,
            fiber: e.fiber, sodium: e.sodium, potassium: e.potassium, calcium: e.calcium, iron: e.iron,
            magnesium: e.magnesium, zinc: e.zinc, selenium: e.selenium, manganese: e.manganese, water: e.water,
            vitaminA: e.vitaminA, vitaminC: e.vitaminC, vitaminD: e.vitaminD, vitaminE: e.vitaminE, vitaminK: e.vitaminK,
            saturatedFat: e.saturatedFat, cholesterol: e.cholesterol, omega3: e.omega3, addedSugars: e.addedSugars)
        if dataManager.addFavoriteFood(favorite) {
            ToastCenter.shared.show("Saved as favorite")
        } else {
            ToastCenter.shared.show("Already in favorites. Use a different name or delete the existing one first.")
        }
        dismiss()
    }

    // MARK: Add to database (:611-735)

    private func addToDatabase() {
        let name = foodName.trimmingCharacters(in: .whitespacesAndNewlines)
        if name.isEmpty { ToastCenter.shared.show("Enter a food name first"); return }
        let userFood: UserAddedFood
        if let f = selectedFood {
            userFood = UserAddedFood(
                name: name, shortName: name,
                calories: f.calories, protein: f.protein, carbs: f.carbs, fat: f.fat, fiber: f.fiber,
                sodium: f.sodium, potassium: f.potassium, calcium: f.calcium, iron: f.iron, magnesium: f.magnesium,
                zinc: f.zinc, selenium: f.selenium, manganese: f.manganese, water: f.water, vitaminA: f.vitaminA,
                vitaminC: f.vitaminC, vitaminD: f.vitaminD, vitaminE: f.vitaminE, vitaminK: f.vitaminK,
                saturatedFat: f.saturatedFat, cholesterol: f.cholesterol, omega3: f.omega3, addedSugars: f.addedSugars)
        } else {
            let grams = currentServing?.grams ?? 100.0
            let amt = amountValue
            let factor = (amt > 0 && grams > 0) ? amt * grams / 100.0 : 1.0
            func per100(_ text: String) -> Double {
                let total = Double(text) ?? 0
                return factor > 0 ? total / factor * 100.0 : total
            }
            userFood = UserAddedFood(name: name, shortName: name,
                                     calories: per100(manualCalories), protein: per100(manualProtein),
                                     carbs: per100(manualCarbs), fat: per100(manualFat))
        }
        if dataManager.addUserAddedFood(userFood) {
            ToastCenter.shared.show("Added to database. You can search for it next time.")
        } else {
            ToastCenter.shared.show("This food is already in your database.")
        }
    }
}
```
Note on the manual-entry per-100 g conversion: Android divides the total by `factor` and multiplies by 100 (`totalCal / factor * 100.0`, `:675`); ported as written even though it yields per-10,000 g for a 100 g serving — spec §1.1 says match Android behaviour (flag to the clinician/product owner in the P3 summary as an Android bug candidate).

---

### Task 4: "Add from favorites" dialogs (audit K)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Tracking/FavoritesDialogs.swift`

**Interfaces:**
- Produces: `FavoritesDialog(selectedDate:)` (sheet). Presents the picker and delete flows internally.

- [ ] **Step 1: Create the file**

```swift
//
//  FavoritesDialogs.swift
//  COPDFuel
//
//  "Add from favorites" (TrackingFragment.showFavoritesDialog,
//  showMealCategoryPicker, showMealCategoryPickerForMeal,
//  showDeleteFavoriteOrMealDialog).
//

import SwiftUI

struct FavoritesDialog: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var dataManager = DataManager.shared
    let selectedDate: Date

    private enum Pick: Identifiable {
        case food(FavoriteFood), meal(FavoriteMeal)
        var id: String {
            switch self {
            case .food(let f): return "f-\(f.id.uuidString)"
            case .meal(let m): return "m-\(m.id.uuidString)"
            }
        }
    }

    @State private var pick: Pick?
    @State private var chosenCategory = "Breakfast"
    @State private var showingDeleteList = false
    @State private var deleteFood: FavoriteFood?
    @State private var deleteMeal: FavoriteMeal?

    var body: some View {
        NavigationStack {
            List {
                ForEach(dataManager.favoriteFoods) { fav in
                    Button("\(fav.label) (\(fav.mealCategory))") {
                        chosenCategory = fav.mealCategory
                        pick = .food(fav)
                    }
                    .foregroundColor(Color(hex: "1f2937"))
                }
                ForEach(dataManager.favoriteMeals) { meal in
                    Button("Meal: \(meal.label) (\(meal.mealCategory))") {
                        chosenCategory = meal.mealCategory
                        pick = .meal(meal)
                    }
                    .foregroundColor(Color(hex: "1f2937"))
                }
                Section {
                    Button("Delete a favorite or meal") { deleteTapped() }
                        .foregroundColor(Color(hex: "2563eb"))
                }
            }
            .navigationTitle("Add from favorites")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
            }
            .sheet(item: $pick) { p in categoryPicker(for: p) }
            .sheet(isPresented: $showingDeleteList) { deleteList }
            .alert("Delete favorite?", isPresented: Binding(
                get: { deleteFood != nil }, set: { if !$0 { deleteFood = nil } }
            ), presenting: deleteFood) { fav in
                Button("Delete", role: .destructive) {
                    dataManager.removeFavoriteFood(fav)
                    ToastCenter.shared.show("Removed from favorites")
                }
                Button("Cancel", role: .cancel) {}
            } message: { fav in
                Text("Remove \"\(fav.label)\" from your favorites? This does not remove it from your food log.")
            }
            .alert("Delete meal?", isPresented: Binding(
                get: { deleteMeal != nil }, set: { if !$0 { deleteMeal = nil } }
            ), presenting: deleteMeal) { meal in
                Button("Delete", role: .destructive) {
                    dataManager.removeFavoriteMeal(meal)
                    ToastCenter.shared.show("Meal removed from favorites")
                }
                Button("Cancel", role: .cancel) {}
            } message: { meal in
                Text("Remove meal \"\(meal.label)\" from your favorites? This does not remove past log entries.")
            }
        }
        .toastOverlay()
    }

    /// "Add to which meal?" / "Add meal to which category?" single-choice.
    private func categoryPicker(for p: Pick) -> some View {
        let title: String
        switch p {
        case .food: title = "Add to which meal?"
        case .meal: title = "Add meal to which category?"
        }
        return NavigationStack {
            VStack(alignment: .leading, spacing: 12) {
                Picker(title, selection: $chosenCategory) {
                    ForEach(AddFoodDialog.mealCategories, id: \.self) { Text($0).tag($0) }
                }
                .pickerStyle(.inline)
                .labelsHidden()
                Spacer()
            }
            .padding(20)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { pick = nil } }
                ToolbarItem(placement: .confirmationAction) { Button("Add") { add(p) } }
            }
        }
        .presentationDetents([.medium])
        .toastOverlay()
    }

    private func add(_ p: Pick) {
        let date = TrackingDates.timestamp(on: selectedDate)
        switch p {
        case .food(let fav):
            dataManager.addFoodEntry(fav.toFoodEntry(mealType: chosenCategory, date: date))
            ToastCenter.shared.show("Added to \(chosenCategory): \(fav.label)")
        case .meal(let meal):
            if meal.items.isEmpty {
                ToastCenter.shared.show("This meal has no items.")
                pick = nil
                return
            }
            for item in meal.items {
                dataManager.addFoodEntry(item.toFoodEntry(mealType: chosenCategory, date: date))
            }
            ToastCenter.shared.show("Added \(meal.items.count) items to \(chosenCategory): \(meal.label)")
        }
        pick = nil
        dismiss()
    }

    private func deleteTapped() {
        if dataManager.favoriteFoods.isEmpty && dataManager.favoriteMeals.isEmpty {
            ToastCenter.shared.show("No favorites or meals to delete.")
        } else {
            showingDeleteList = true
        }
    }

    private var deleteList: some View {
        NavigationStack {
            List {
                ForEach(dataManager.favoriteFoods) { fav in
                    Button("\(fav.label) (\(fav.mealCategory))") { showingDeleteList = false; deleteFood = fav }
                        .foregroundColor(Color(hex: "1f2937"))
                }
                ForEach(dataManager.favoriteMeals) { meal in
                    Button("Meal: \(meal.label) (\(meal.mealCategory))") { showingDeleteList = false; deleteMeal = meal }
                        .foregroundColor(Color(hex: "1f2937"))
                }
            }
            .navigationTitle("Delete a favorite or meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showingDeleteList = false } }
            }
        }
    }
}

extension FavoriteMealItem {
    /// Android FavoriteMealItem.toFoodEntry(dateMillis, category): all 23 nutrients.
    func toFoodEntry(mealType: String, date: Date) -> FoodEntry {
        FoodEntry(date: date, mealType: mealType, foodName: name, quantity: quantity,
                  calories: calories, protein: protein, carbs: carbs, fat: fat, fiber: fiber,
                  sodium: sodium, potassium: potassium, calcium: calcium, iron: iron, magnesium: magnesium,
                  zinc: zinc, selenium: selenium, manganese: manganese, water: water, vitaminA: vitaminA,
                  vitaminC: vitaminC, vitaminD: vitaminD, vitaminE: vitaminE, vitaminK: vitaminK,
                  saturatedFat: saturatedFat, cholesterol: cholesterol, omega3: omega3, addedSugars: addedSugars)
    }
}
```
The "No favorites yet…" toast (audit 50) lives in the caller: `TrackingDayView` checks emptiness before presenting (Task 6).

---

### Task 5: "Create meal" dialog (audit L)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Tracking/CreateMealDialog.swift`

- [ ] **Step 1: Create the file**

```swift
//
//  CreateMealDialog.swift
//  COPDFuel
//
//  CreateMealDialog.kt + dialog_create_meal.xml.
//

import SwiftUI

struct CreateMealDialog: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var dataManager = DataManager.shared
    @State private var mealName = ""
    @State private var mealCategory = "Breakfast"
    @State private var items: [FoodEntry] = []
    @State private var showingAddFood = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Meal name").font(.system(size: 14, weight: .bold))
                    TextField("e.g. My usual breakfast", text: $mealName).textFieldStyle(.roundedBorder)
                    Text("Meal category").font(.system(size: 14, weight: .bold))
                    Picker("Meal category", selection: $mealCategory) {
                        ForEach(AddFoodDialog.mealCategories, id: \.self) { Text($0).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    Text("Foods in this meal").font(.system(size: 14, weight: .bold))
                    if items.isEmpty {
                        Text("Tap \"Add food to meal\" to add items")
                            .font(.system(size: 14))
                            .foregroundColor(Color(hex: "6b7280"))
                            .padding(12)
                    } else {
                        ForEach(Array(items.enumerated()), id: \.element.id) { index, entry in
                            HStack {
                                Text("\(entry.foodName) - \(entry.quantity) (\(Int(entry.calories ?? 0)) cal)")
                                    .font(.system(size: 14))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                Button("Remove") { items.remove(at: index) }
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(Color(hex: "dc2626"))
                            }
                            .padding(.horizontal, 12).padding(.vertical, 8)
                        }
                    }
                    TrackingButton(title: "Add food to meal") { showingAddFood = true }
                    TrackingButton(title: "Save meal as favorite", background: Color(hex: "10b981"), action: saveMeal)
                }
                .padding(20)
            }
            .navigationTitle("Create meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
            }
            .sheet(isPresented: $showingAddFood) {
                AddFoodDialog(selectedDate: Date()) { entry in items.append(entry) }
            }
        }
        .toastOverlay()
    }

    private func saveMeal() {
        let label = mealName.trimmingCharacters(in: .whitespacesAndNewlines)
        if label.isEmpty { ToastCenter.shared.show("Enter a meal name"); return }
        if items.isEmpty { ToastCenter.shared.show("Add at least one food to the meal"); return }
        let mealItems = items.map { e in
            FavoriteMealItem(name: e.foodName, quantity: e.quantity,
                             calories: e.calories ?? 0, protein: e.protein ?? 0, carbs: e.carbs ?? 0, fat: e.fat ?? 0,
                             fiber: e.fiber, sodium: e.sodium, potassium: e.potassium, calcium: e.calcium, iron: e.iron,
                             magnesium: e.magnesium, zinc: e.zinc, selenium: e.selenium, manganese: e.manganese,
                             water: e.water, vitaminA: e.vitaminA, vitaminC: e.vitaminC, vitaminD: e.vitaminD,
                             vitaminE: e.vitaminE, vitaminK: e.vitaminK, saturatedFat: e.saturatedFat,
                             cholesterol: e.cholesterol, omega3: e.omega3, addedSugars: e.addedSugars)
        }
        if dataManager.addFavoriteMeal(FavoriteMeal(label: label, mealCategory: mealCategory, items: mealItems)) {
            ToastCenter.shared.show("Meal saved as favorite")
            dismiss()
        } else {
            ToastCenter.shared.show("A meal with this name already exists. Use a different name.")
        }
    }
}
```

---

### Task 6: Wire the day view, delete the legacy modals, build, commit

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/Tracking/TrackingDayView.swift` (three sheet lines + favorites guard)
- Delete: `COPDFuel/COPDFuel/Views/Tracking/LegacyFoodModals.swift`

- [ ] **Step 1: Replace the three `.sheet` lines in `TrackingDayView`**

```swift
        .sheet(isPresented: $showingAddFood) { AddFoodDialog(selectedDate: selectedDate) }
        .sheet(isPresented: $showingFavorites) { FavoritesDialog(selectedDate: selectedDate) }
        .sheet(isPresented: $showingCreateMeal) { CreateMealDialog() }
```
and change the "Add from favorites" button action to:
```swift
                if dataManager.favoriteFoods.isEmpty && dataManager.favoriteMeals.isEmpty {
                    ToastCenter.shared.show("No favorites yet. Add a food (or create a meal) and save as favorite.")
                } else {
                    showingFavorites = true
                }
```

- [ ] **Step 2: Delete the legacy file**

```bash
cd COPDFuel && /usr/bin/git rm -q COPDFuel/Views/Tracking/LegacyFoodModals.swift && grep -rn "FoodTrackingModal\|FavoritesFoodSheet\|FavoriteMealSheet\|CreateFavoriteMealView\|AddFoodConfirmView\|ManualFoodEntryView\|FoodSource" COPDFuel --include='*.swift'
```
Expected: no grep output.

- [ ] **Step 3: Build**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -derivedDataPath "$HOME/Library/Developer/Xcode/DerivedData/COPDFuel-cli" -quiet build 2>&1 | grep -E "error:" | head -20; echo "exit=${PIPESTATUS[0]}"
```
Expected `exit=0`. Likely first-pass issue: `ReportGenerator` or `ProfileView` calling `addFavoriteFood`/`addUserAddedFood` and ignoring the result is fine (`@discardableResult`).

- [ ] **Step 4: Copy checks**

```bash
for s in "Local (18,563 foods)" "USDA Online" "Search from local database (" "Please enter a search term or select a filter" "e.g., chicken breast, apple" "No results found" "- tap to select" "Enter food name" "Or edit nutrition manually:" "Edit Nutrition Manually" "Hide Manual Entry" "Save as favorite (quick add later)" "Add to database (searchable next time)" "Give this meal a short name so you can add it quickly later." "Already in favorites. Use a different name or delete the existing one first." "This food is already in your database." "Food saved successfully" "Add from favorites" "Delete a favorite or meal" "Add to which meal?" "Add meal to which category?" "This meal has no items." "Create meal" "Save meal as favorite" "Add at least one food to the meal" "A meal with this name already exists. Use a different name."; do
  a=$(grep -rl -F "$s" android/app/src/main/java/com/copdhealthtracker/ui android/app/src/main/res/layout | wc -l | tr -d ' ')
  i=$(grep -rl -F "$s" COPDFuel/COPDFuel/Views/Tracking | wc -l | tr -d ' ')
  echo "$a android / $i ios  <- $s"
done
```
Expected: non-zero on both sides for every row.

- [ ] **Step 5: Commit**

```bash
cd COPDFuel && /usr/bin/git add -A && /usr/bin/git commit -q -m "iOS parity P3.C (2/2): Android AddFoodDialog (source/filter/search/servings/manual/favorite/database/add-to-meal), favorites + meal dialogs, full 23-nutrient + servingSizes DB decoding, uniqueness rules

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>" && /usr/bin/git log --oneline -1
```

- [ ] **Step 6: Manual smoke (simulator)**

"+ Quick Add Food": status shows "Search from local database (18,563 foods)"; tapping Search with nothing typed toasts; "chicken" + Search lists results with "N cal | P: … " lines; selecting fills Food Name, serving options end with "g (enter amount)", preview updates with Amount; "Save as favorite…" prompts for a label and a second save with the same label toasts the duplicate message; Save toasts "Food saved successfully" and the Lunch section summary updates; "Add from favorites" lists "label (Lunch)" and adds with the "Added to …" toast; "Create meal" → "Add food to meal" → "Add to meal" toast → "Save meal as favorite".
