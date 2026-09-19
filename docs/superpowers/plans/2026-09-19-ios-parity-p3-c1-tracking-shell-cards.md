# iOS Parity P3.C (1/2): Tracking shell, day/week/month views, health cards, import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the iOS Tracking tab structure to Android `TrackingFragment` / `fragment_tracking.xml`: header band, Day/Week/Month toggle, Android day-view order (day nav, profile banner, macro targets, Fat/Minerals/Vitamins sections, collapsible meal sections, action buttons, inline hydration card, "COPD Fuel" device-import section with oxygen/exercise/steps/heart-rate cards, weight and medications cards), week/month Trend Reports, and the exercise/oxygen/weight/medication dialogs with Android validation toasts. Health import gets the 30-day window, dedupe and Android messages; profile weight sync is ported.

**Architecture:** `Views/TrackingView.swift` (2031 lines) is split into `Views/Tracking/*.swift`. `TrackingView` owns the date/mode state and renders one of `TrackingDayView`, `TrackingWeekView`, `TrackingMonthView`. Cards are small structs that read `DataManager.shared` directly. New `Services/ProfileWeightSync.swift` ports Android `ProfileWeightSync.kt`. `DataManager` gains the three dedupe helpers; `HealthKitService` gains a live per-day steps read. The old food/favorites/meal modals are moved untouched into `Views/Tracking/LegacyFoodModals.swift` and replaced in P3.C (2/2). `DailyTrackingSummary.swift` is deleted; its month-journal cards move to `TrackingMonthView.swift`.

**Tech Stack:** SwiftUI, HealthKit, iOS 17. No test target; verify by build + copy greps.

**Spec:** `docs/superpowers/specs/2026-09-19-ios-parity-p3-design.md` §2.2, §2.5, §2.6, §3 row P3.C. Gap list: `docs/superpowers/specs/2026-09-19-ios-parity-p3/audit-tracking.md` §A–H (structure, nav, week, month, banner, macros, nutrient sections, meal lists), §M–S (hydration, exercise, oxygen, weight, steps/HR/import, medications, data layer) and §T (removals). §I–L (add-food entry, AddFoodDialog, favorites, create meal) are P3.C (2/2).

## Global Constraints

- Android `main` `5c4aa49` is the source of truth; copy verbatim; `Toast` → `ToastCenter.shared.show`; `AlertDialog` → `.alert` (spec §1.1).
- Android bug NOT ported: entries are stamped on the **selected day** (spec §1.3). Use `TrackingDates.timestamp(on: selectedDate)` (today → now; other day → that day at the current clock time).
- Pref keys read here: `weight` (fallback `profile_weight` until P3.E migrates), `activity_level`, `protein_target` (read as Double), `last_updated` (spec §1.5, audit 85).
- Remove: "Today's Summary"/"Day Summary" tile grid, week/month Summary tiles, per-week `WeeklyExerciseCard`s, iOS Steps/Heart-Rate `TrackingCard`s, "APPLE HEALTH" section, import period picker and inline result panel, oxygen "Normal range" note, weight "Recording for <date>" section, `profile.goalWeight` writes, `GlobalWeightCard` "Goal / Not set" + "Last entry" rows, medication swipe-only actions (audit §T).
- Keep: HealthKit workout-type names (audit 79).
- Depends on P3.0 (`ToastCenter`, `TrackingView(selectedTab:)`), P3.A (`HeaderSection`), P3.B (`AddMedicationDialog`, `ExerciseJournalEntry`/`ExerciseJournalStore`, `Medication.date`, sorted medication getters).
- Build after each task group with `xcodebuild -project COPDFuel/COPDFuel.xcodeproj -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -derivedDataPath "$HOME/Library/Developer/Xcode/DerivedData/COPDFuel-cli" build`; one commit at the end. Use `/usr/bin/git`; commands run from the parent repo root.

---

### Task 1: Data-layer additions (dedupe, weight sync, date stamping, live steps)

**Files:**
- Create: `COPDFuel/COPDFuel/Services/ProfileWeightSync.swift`
- Create: `COPDFuel/COPDFuel/Services/TrackingDates.swift`
- Modify: `COPDFuel/COPDFuel/Services/DataManager.swift` (oxygen/heart-rate/steps helpers; `deleteExerciseEntry` not needed)
- Modify: `COPDFuel/COPDFuel/Services/HealthKitService.swift` (add `readStepsForCalendarDay`)

**Interfaces (produced):**
- `ProfileWeightSync.writeWeightToPrefs(_ lbs: Double)`, `.syncPrefsFromRepositoryCurrentWeight()`, `.syncProfileAndDbWeight()`, `.profileWeightString() -> String?`, `.formatWeight(_:) -> String`.
- `TrackingDates.timestamp(on day: Date) -> Date`, `TrackingDates.startOfWeek(_:) -> Date`.
- `DataManager.addOxygenReadingIfAbsent(_:)`, `.addHeartRateEntryIfAbsent(_:)`, `.replaceStepsForDayFromImport(day:count:)`.
- `HealthKitService.readStepsForCalendarDay(_ day: Date) async -> Int?` (nil when unavailable or the query fails).

- [ ] **Step 1: `Services/ProfileWeightSync.swift`**

```swift
//
//  ProfileWeightSync.swift
//  COPDFuel
//
//  Port of Android utils/ProfileWeightSync.kt: keeps the profile pref
//  "weight" aligned with the latest non-goal WeightEntry.
//  Until P3.E migrates Profile to the flat keys, the value is mirrored
//  to the legacy iOS key "profile_weight" as well.
//

import Foundation

enum ProfileWeightSync {
    private static let defaults = UserDefaults.standard

    /// Android `writeWeightToPrefs`: "weight" = 1-decimal string (integers
    /// without ".0"), "last_updated" = "MMM d, yyyy".
    static func writeWeightToPrefs(_ lbs: Double) {
        let f = DateFormatter()
        f.dateFormat = "MMM d, yyyy"
        let text = formatWeight(lbs)
        defaults.set(text, forKey: "weight")
        defaults.set(text, forKey: "profile_weight")
        defaults.set(f.string(from: Date()), forKey: "last_updated")
    }

    /// Android `syncPrefsFromRepositoryCurrentWeight`.
    static func syncPrefsFromRepositoryCurrentWeight() {
        guard let latest = DataManager.shared.getCurrentWeight() else { return }
        writeWeightToPrefs(latest)
    }

    /// Android `syncProfileAndDbWeight`: prefs follow the DB when they
    /// differ by more than 0.05; an empty DB is seeded from prefs.
    static func syncProfileAndDbWeight() {
        let dm = DataManager.shared
        let latest = dm.getCurrentWeight()
        let prefW = profileWeightString().flatMap { Double($0) }
        if let latest {
            if prefW == nil || abs(latest - (prefW ?? 0)) > 0.05 {
                writeWeightToPrefs(latest)
            }
        } else if let prefW, prefW > 0 {
            dm.addWeightEntry(WeightEntry(weight: prefW, type: .current))
        }
    }

    /// Android pref "weight"; falls back to the pre-P3.E iOS key.
    static func profileWeightString() -> String? {
        if let w = defaults.string(forKey: "weight"), !w.isEmpty { return w }
        if let w = defaults.string(forKey: "profile_weight"), !w.isEmpty { return w }
        return nil
    }

    static func formatWeight(_ lbs: Double) -> String {
        let rounded = (lbs * 10).rounded() / 10
        if rounded == rounded.rounded(.towardZero) {
            return String(Int(rounded))
        }
        return String(rounded)
    }
}
```

- [ ] **Step 2: `Services/TrackingDates.swift`**

```swift
//
//  TrackingDates.swift
//  COPDFuel
//
//  Date helpers shared by the Tracking views.
//

import Foundation

enum TrackingDates {
    /// Entries logged while viewing another day are stamped on that day
    /// at the current clock time (spec §1.3: Android's "always now" is a bug).
    static func timestamp(on day: Date) -> Date {
        let cal = Calendar.current
        if cal.isDateInToday(day) { return Date() }
        let now = cal.dateComponents([.hour, .minute, .second], from: Date())
        return cal.date(bySettingHour: now.hour ?? 0, minute: now.minute ?? 0, second: now.second ?? 0, of: day) ?? day
    }

    /// Android `getStartOfWeek`: first weekday of the current locale, 00:00.
    static func startOfWeek(_ date: Date) -> Date {
        let cal = Calendar.current
        let comps = cal.dateComponents([.yearForWeekOfYear, .weekOfYear], from: date)
        return cal.date(from: comps) ?? cal.startOfDay(for: date)
    }

    static func startOfMonth(_ date: Date) -> Date {
        let cal = Calendar.current
        return cal.date(from: cal.dateComponents([.year, .month], from: date)) ?? date
    }

    static func endOfMonth(_ date: Date) -> Date {
        let cal = Calendar.current
        let next = cal.date(byAdding: .month, value: 1, to: startOfMonth(date)) ?? date
        return next.addingTimeInterval(-1)
    }

    static func daysInMonth(_ date: Date) -> Int {
        Calendar.current.range(of: .day, in: .month, for: date)?.count ?? 30
    }
}
```

- [ ] **Step 3: `DataManager` dedupe helpers**

Add after `addOxygenReading`:
```swift
    /// Android DataRepository.insertReadingFromHealthConnectImport: skip
    /// when a reading with the identical timestamp already exists.
    func addOxygenReadingIfAbsent(_ reading: OxygenReading) {
        guard !oxygenReadings.contains(where: { $0.date == reading.date }) else { return }
        addOxygenReading(reading)
    }
```
Add after `addHeartRateEntry`:
```swift
    func addHeartRateEntryIfAbsent(_ entry: HeartRateEntry) {
        guard !heartRateEntries.contains(where: { $0.date == entry.date }) else { return }
        addHeartRateEntry(entry)
    }
```
Add after `addStepsEntry`:
```swift
    /// Android DataRepository.replaceStepsForDayFromImport: one row per
    /// calendar day so re-imports do not stack.
    func replaceStepsForDayFromImport(day: Date, count: Int) {
        let cal = Calendar.current
        stepsEntries.removeAll { cal.isDate($0.date, inSameDayAs: day) }
        stepsEntries.append(StepsEntry(date: cal.startOfDay(for: day), count: count))
        saveStepsEntries()
    }
```
Also add `func deleteExerciseEntry(id: UUID)` is **not** required (Android has no per-row delete on the Tracking card).

- [ ] **Step 4: `HealthKitService.readStepsForCalendarDay`**

Add after `importData`:
```swift
    /// Android HealthConnectSync.readStepsForCalendarDay: cumulative step
    /// count for one local calendar day. nil when HealthKit is unavailable
    /// or the query fails (caller falls back to stored entries).
    func readStepsForCalendarDay(_ day: Date) async -> Int? {
        guard isAvailable, let type = HKQuantityType.quantityType(forIdentifier: .stepCount) else { return nil }
        let cal = Calendar.current
        let start = cal.startOfDay(for: day)
        guard let end = cal.date(byAdding: .day, value: 1, to: start) else { return nil }
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: .strictStartDate)
        return await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(quantityType: type, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, error in
                if error != nil { continuation.resume(returning: nil); return }
                let count = stats?.sumQuantity().map { Int($0.doubleValue(for: .count())) } ?? 0
                continuation.resume(returning: count)
            }
            healthStore.execute(query)
        }
    }
```

---

### Task 2: Move the legacy food modals aside, delete the old files

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Tracking/LegacyFoodModals.swift`
- Delete: `COPDFuel/COPDFuel/Views/TrackingView.swift`, `COPDFuel/COPDFuel/Views/DailyTrackingSummary.swift`

- [ ] **Step 1: Copy, verbatim, into `Views/Tracking/LegacyFoodModals.swift`** (with `import SwiftUI` at the top) these structs/enums from `Views/TrackingView.swift`: `FoodSource` (lines 1007-1012), `FoodTrackingModal` (1014-1326), `ManualFoodEntryView` (1328-1402), `AddFoodConfirmView` (1404-1494), `FavoritesFoodSheet` (893-965), `FavoriteMealSheet` (1845-1942), `CreateFavoriteMealView` (1944-2030). Add this header comment: `// Pre-P3.C food modals. Replaced wholesale by P3.C (2/2) (AddFoodDialog, FavoritesDialogs). Do not extend.`

- [ ] **Step 2: Copy, verbatim, into the new `Views/Tracking/TrackingMonthView.swift` (Task 6)** these structs from `Views/DailyTrackingSummary.swift`: `ExerciseJournalMonthlyCard` (512-571) and `MonthlyEntryCard` (646-end). Everything else in that file (`DailyTrackingSummary`, `SummaryCard`, `WeekDayCard`, `WeeklyExerciseCard`, `TrackingViewMode`) is dropped; `TrackingViewMode` is re-declared in Task 3.

- [ ] **Step 3: Delete the two old files**

```bash
cd COPDFuel && /usr/bin/git rm -q COPDFuel/Views/TrackingView.swift COPDFuel/Views/DailyTrackingSummary.swift && /usr/bin/git status --short | head
```

---

### Task 3: `TrackingView` shell + day navigation + profile banner (audit A, B, E)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Tracking/TrackingView.swift`
- Create: `COPDFuel/COPDFuel/Views/Tracking/TrackingDayView.swift`

- [ ] **Step 1: `TrackingView.swift`**

```swift
//
//  TrackingView.swift
//  COPDFuel
//
//  Tracking tab shell: green "COPD Fuel" header, Day/Week/Month toggle,
//  and one of the three mode views. Mirrors fragment_tracking.xml +
//  TrackingFragment.setupViewToggle/showDayView/showWeekView/showMonthView.
//

import SwiftUI

enum TrackingViewMode: CaseIterable {
    case day, week, month
    var label: String {
        switch self {
        case .day: return "Day"
        case .week: return "Week"
        case .month: return "Month"
        }
    }
}

struct TrackingView: View {
    /// Tab selection owned by MainTabView; set to 5 to open Profile
    /// (Android MainActivity.switchToProfile()).
    @Binding var selectedTab: Int

    @State private var selectedDate = Date()
    @State private var viewMode: TrackingViewMode = .day

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    HeaderSection()
                    viewToggle
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                    switch viewMode {
                    case .day:
                        TrackingDayView(selectedDate: $selectedDate, selectedTab: $selectedTab)
                    case .week:
                        TrackingWeekView(selectedDate: $selectedDate)
                    case .month:
                        TrackingMonthView(selectedDate: $selectedDate)
                    }
                }
            }
            .background(Color(hex: "f8fafc"))
            .edgesIgnoringSafeArea(.top)
            .toolbar(.hidden, for: .navigationBar)
        }
    }

    /// Active #22c55e / white, inactive #E5E7EB / #374151.
    private var viewToggle: some View {
        HStack(spacing: 8) {
            ForEach(TrackingViewMode.allCases, id: \.self) { mode in
                let active = viewMode == mode
                Button(action: { viewMode = mode }) {
                    Text(mode.label)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(active ? .white : Color(hex: "374151"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(active ? Color(hex: "22c55e") : Color(hex: "E5E7EB"))
                        .cornerRadius(8)
                }
            }
        }
    }
}

// MARK: - Shared card styling

/// White rounded card used by every Tracking section.
struct TrackingCardContainer<Content: View>: View {
    @ViewBuilder let content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 8) { content }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.06), radius: 3, x: 0, y: 1)
    }
}

/// Filled full-width button used by the log/import buttons.
struct TrackingButton: View {
    let title: String
    var background: Color = Color(hex: "2563eb")
    var foreground: Color = .white
    var height: CGFloat = 44
    var fontSize: CGFloat = 14
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: fontSize, weight: .bold))
                .foregroundColor(foreground)
                .frame(maxWidth: .infinity)
                .frame(height: height)
                .background(background)
                .cornerRadius(8)
        }
    }
}
```

- [ ] **Step 2: `TrackingDayView.swift`**

```swift
//
//  TrackingDayView.swift
//  COPDFuel
//
//  Day view in Android order (fragment_tracking.xml day_view_content):
//  day nav → profile banner → Macronutrient Targets → Fat Breakdown →
//  Minerals → Vitamins and Fiber → meal sections → action buttons →
//  Hydration Tracker → "COPD Fuel" device section (import, oxygen,
//  exercise, steps, heart rate) → Weight + Medications.
//

import SwiftUI

struct TrackingDayView: View {
    @Binding var selectedDate: Date
    @Binding var selectedTab: Int
    @ObservedObject private var dataManager = DataManager.shared

    @State private var showingDatePicker = false
    @State private var profileIncomplete = false
    @State private var showingAddFood = false
    @State private var showingFavorites = false
    @State private var showingCreateMeal = false

    private var foods: [FoodEntry] { dataManager.getFoodEntries(for: selectedDate) }

    var body: some View {
        VStack(spacing: 16) {
            dayNavigation
            if profileIncomplete { profileBanner }
            MacroTargetsCard(foods: foods, onSetUp: { selectedTab = 5 })
            NutrientTargetsSections(foods: foods)
            MealSectionsView(selectedDate: selectedDate)
            actionButtons
            HydrationCard(selectedDate: selectedDate)
            HealthImportSection(selectedDate: selectedDate)
            HStack(alignment: .top, spacing: 12) {
                WeightCard(selectedDate: selectedDate)
                MedicationsCard()
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 24)
        .onAppear(perform: checkProfileSetup)
        .sheet(isPresented: $showingDatePicker) { datePickerSheet }
        .sheet(isPresented: $showingAddFood) { FoodTrackingModal(selectedDate: selectedDate) }
        .sheet(isPresented: $showingFavorites) { FavoritesFoodSheet(selectedDate: selectedDate) }
        .sheet(isPresented: $showingCreateMeal) { CreateFavoriteMealView() }
    }

    /// TrackingFragment.checkProfileSetup(): banner when "weight" or
    /// "activity_level" is empty; re-checked on appear.
    private func checkProfileSetup() {
        let activity = UserDefaults.standard.string(forKey: "activity_level") ?? ""
        profileIncomplete = ProfileWeightSync.profileWeightString() == nil || activity.isEmpty
    }

    // MARK: Day navigation (TrackingFragment.updateDateButtonText)

    private var dateLabel: String {
        let cal = Calendar.current
        if cal.isDateInToday(selectedDate) { return "Today" }
        if cal.isDateInYesterday(selectedDate) { return "Yesterday" }
        if cal.isDateInTomorrow(selectedDate) { return "Tomorrow" }
        let f = DateFormatter()
        f.dateFormat = "EEEE"
        return f.string(from: selectedDate)
    }

    private var dateText: String {
        let f = DateFormatter()
        f.dateFormat = "MMMM d, yyyy"
        return f.string(from: selectedDate)
    }

    private var dayNavigation: some View {
        HStack(spacing: 8) {
            navButton("<") { shiftDay(-1) }
            VStack(spacing: 2) {
                Text(dateLabel)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(Color(hex: "1f2937"))
                Button(dateText) { showingDatePicker = true }
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "2563eb"))
            }
            .frame(maxWidth: .infinity)
            navButton(">") { shiftDay(1) }
        }
    }

    private func navButton(_ glyph: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(glyph)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color(hex: "374151"))
                .frame(width: 48, height: 48)
                .background(Color(hex: "E5E7EB"))
                .cornerRadius(8)
        }
    }

    private func shiftDay(_ offset: Int) {
        if let d = Calendar.current.date(byAdding: .day, value: offset, to: selectedDate) {
            selectedDate = d
        }
    }

    private var datePickerSheet: some View {
        NavigationStack {
            DatePicker("Date", selection: $selectedDate, displayedComponents: .date)
                .datePickerStyle(.graphical)
                .padding()
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) { Button("Done") { showingDatePicker = false } }
                }
        }
        .presentationDetents([.medium])
    }

    // MARK: Profile banner (fragment_tracking.xml profile_setup_banner)

    private var profileBanner: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Set Up Your Protein Target")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color(hex: "92400e"))
                Text("Enter your weight and activity level in Profile to get a personalized protein target.")
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "b45309"))
            }
            Spacer()
            Button(action: { selectedTab = 5 }) {
                Text("Set Up")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color(hex: "f59e0b"))
                    .cornerRadius(8)
            }
        }
        .padding(16)
        .background(Color(hex: "fef3c7"))
        .cornerRadius(12)
    }

    // MARK: Action buttons (:2565-2587)

    private var actionButtons: some View {
        VStack(spacing: 8) {
            TrackingButton(title: "+ Quick Add Food", background: Color(hex: "3b82f6"), height: 56, fontSize: 16) {
                showingAddFood = true
            }
            TrackingButton(title: "Add from favorites", background: Color(hex: "E5E7EB"), foreground: Color(hex: "374151")) {
                showingFavorites = true
            }
            TrackingButton(title: "Create meal (add to favorites)", background: Color(hex: "E5E7EB"), foreground: Color(hex: "374151")) {
                showingCreateMeal = true
            }
        }
    }
}
```

---

### Task 4: Macro targets card + nutrient sections (audit F, G)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Tracking/MacroTargetsCard.swift`

- [ ] **Step 1: Create the file**

```swift
//
//  MacroTargetsCard.swift
//  COPDFuel
//
//  "Macronutrient Targets" card + "Fat Breakdown" / "Minerals" /
//  "Vitamins and Fiber" sections. Mirrors TrackingFragment.updateMacroTargets
//  (:1525-1607) and updateNutrientTargets (:1609-1753).
//

import SwiftUI

struct MacroTargetsCard: View {
    let foods: [FoodEntry]
    let onSetUp: () -> Void

    private static let energyTarget = 2000.0
    private static let carbsTarget = 250.0
    private static let fatTarget = 65.0

    /// Gate = "weight" & "activity_level" non-empty AND protein_target > 0.
    private var proteinTarget: Double {
        let activity = UserDefaults.standard.string(forKey: "activity_level") ?? ""
        guard ProfileWeightSync.profileWeightString() != nil, !activity.isEmpty else { return 0 }
        return UserDefaults.standard.double(forKey: "protein_target")
    }

    var body: some View {
        let energy = foods.reduce(0.0) { $0 + ($1.calories ?? 0) }
        let protein = foods.reduce(0.0) { $0 + ($1.protein ?? 0) }
        let carbs = foods.reduce(0.0) { $0 + ($1.carbs ?? 0) }
        let fat = foods.reduce(0.0) { $0 + ($1.fat ?? 0) }
        let target = proteinTarget

        TrackingCardContainer {
            Text("Macronutrient Targets")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color(hex: "22c55e"))

            MacroRow(label: String(format: "Energy - %.1f / %d kcal", energy, Int(Self.energyTarget)),
                     percent: Int(energy / Self.energyTarget * 100),
                     normal: "22c55e", over: "f97316")

            if target > 0 {
                MacroRow(label: String(format: "Protein - %.1f / %d g", protein, Int(target)),
                         percent: Int(protein / target * 100),
                         normal: "22c55e", over: "f97316")
            } else {
                // Android: "Protein - x.x g", percent text "Set up" (#2563eb), progress 0, tap → Profile.
                Button(action: onSetUp) {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(String(format: "Protein - %.1f g", protein))
                                .font(.system(size: 14))
                                .foregroundColor(Color(hex: "1f2937"))
                            Spacer()
                            Text("Set up")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(Color(hex: "2563eb"))
                        }
                        ProgressView(value: 0).tint(Color(hex: "22c55e"))
                    }
                }
                .buttonStyle(.plain)
            }

            MacroRow(label: String(format: "Net Carbs - %.1f / %d g", carbs, Int(Self.carbsTarget)),
                     percent: Int(carbs / Self.carbsTarget * 100),
                     normal: "3b82f6", over: "f97316")
            MacroRow(label: String(format: "Fat - %.1f / %d g", fat, Int(Self.fatTarget)),
                     percent: Int(fat / Self.fatTarget * 100),
                     normal: "f97316", over: "ef4444")
        }
    }
}

/// Label 14sp + uncapped "N%" (colour flips when > 100) + bar capped at 100.
struct MacroRow: View {
    let label: String
    let percent: Int
    let normal: String
    let over: String

    var body: some View {
        let color = Color(hex: percent > 100 ? over : normal)
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label)
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "1f2937"))
                Spacer()
                Text("\(percent)%")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(color)
            }
            ProgressView(value: Double(min(percent, 100)) / 100).tint(color)
        }
    }
}

// MARK: - Fat Breakdown / Minerals / Vitamins and Fiber

struct NutrientTargetsSections: View {
    let foods: [FoodEntry]

    private struct Row { let label: String; let total: Double; let target: Double; let limit: Bool; let bar: String? }

    private func sum(_ keyPath: KeyPath<FoodEntry, Double>) -> Double {
        foods.reduce(0.0) { $0 + $1[keyPath: keyPath] }
    }

    var body: some View {
        VStack(spacing: 16) {
            section(title: "Fat Breakdown", titleColor: "f97316", defaultBar: "f97316", rows: [
                Row(label: "Saturated", total: sum(\.saturatedFat), target: 20, limit: true, bar: nil),
                Row(label: "Cholesterol", total: sum(\.cholesterol), target: 300, limit: true, bar: nil),
                Row(label: "Omega-3", total: sum(\.omega3), target: 1.6, limit: false, bar: "22c55e"),
                Row(label: "Added Sugars", total: sum(\.addedSugars), target: 50, limit: true, bar: nil)
            ])
            section(title: "Minerals", titleColor: "3b82f6", defaultBar: "3b82f6", rows: [
                Row(label: "Calcium", total: sum(\.calcium), target: 1000, limit: false, bar: nil),
                Row(label: "Iron", total: sum(\.iron), target: 18, limit: false, bar: nil),
                Row(label: "Magnesium", total: sum(\.magnesium), target: 420, limit: false, bar: nil),
                Row(label: "Zinc", total: sum(\.zinc), target: 11, limit: false, bar: nil),
                Row(label: "Potassium", total: sum(\.potassium), target: 4700, limit: false, bar: nil),
                Row(label: "Sodium", total: sum(\.sodium), target: 2300, limit: true, bar: nil),
                Row(label: "Selenium", total: sum(\.selenium), target: 55, limit: false, bar: nil),
                Row(label: "Manganese", total: sum(\.manganese), target: 2.3, limit: false, bar: nil)
            ])
            section(title: "Vitamins and Fiber", titleColor: "8b5cf6", defaultBar: "8b5cf6", rows: [
                Row(label: "Fiber", total: sum(\.fiber), target: 25, limit: false, bar: "22c55e"),
                Row(label: "Vitamin A", total: sum(\.vitaminA), target: 900, limit: false, bar: nil),
                Row(label: "Vitamin C", total: sum(\.vitaminC), target: 90, limit: false, bar: nil),
                Row(label: "Vitamin E", total: sum(\.vitaminE), target: 15, limit: false, bar: nil),
                Row(label: "Vitamin D", total: sum(\.vitaminD), target: 20, limit: false, bar: nil),
                Row(label: "Vitamin K", total: sum(\.vitaminK), target: 120, limit: false, bar: nil),
                Row(label: "Water", total: sum(\.water), target: 3700, limit: false, bar: "3b82f6")
            ])
        }
    }

    /// Two-column grid; percent capped at 200 for the text, 100 for the bar;
    /// limit rows go #ef4444 above 100 % else #f97316.
    private func section(title: String, titleColor: String, defaultBar: String, rows: [Row]) -> some View {
        TrackingCardContainer {
            Text(title)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: titleColor))
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 16), GridItem(.flexible())], spacing: 10) {
                ForEach(rows, id: \.label) { row in
                    let percent = min(200, Int(row.total / row.target * 100))
                    let barHex = row.limit ? (percent > 100 ? "ef4444" : "f97316") : (row.bar ?? defaultBar)
                    VStack(alignment: .leading, spacing: 3) {
                        HStack {
                            Text(row.label)
                                .font(.system(size: 13))
                                .foregroundColor(Color(hex: "1f2937"))
                            Spacer()
                            Text("\(percent)%")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(Color(hex: "1f2937"))
                        }
                        ProgressView(value: Double(min(percent, 100)) / 100)
                            .tint(Color(hex: barHex))
                    }
                }
            }
        }
    }
}
```

---

### Task 5: Collapsible meal sections + food cards (audit H)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Tracking/MealSectionsView.swift`

- [ ] **Step 1: Create the file**

```swift
//
//  MealSectionsView.swift
//  COPDFuel
//
//  Breakfast / Lunch / Dinner / Snacks sections, collapsed by default
//  (TrackingFragment.toggleMealSection, updateMealSummary, displayFoodItems).
//

import SwiftUI

struct MealSectionsView: View {
    let selectedDate: Date
    @ObservedObject private var dataManager = DataManager.shared
    @State private var expanded: Set<String> = []
    @State private var foodToDelete: FoodEntry?

    private let categories = ["Breakfast", "Lunch", "Dinner", "Snacks"]

    var body: some View {
        VStack(spacing: 12) {
            ForEach(categories, id: \.self) { category in
                let items = dataManager.getFoodEntries(for: selectedDate).filter { $0.mealType == category }
                mealSection(category, items: items)
            }
        }
        .alert("Delete Food", isPresented: Binding(
            get: { foodToDelete != nil }, set: { if !$0 { foodToDelete = nil } }
        ), presenting: foodToDelete) { food in
            Button("Delete", role: .destructive) {
                dataManager.deleteFoodEntry(id: food.id)
                ToastCenter.shared.show("Food deleted")
            }
            Button("Cancel", role: .cancel) {}
        } message: { food in
            Text("Are you sure you want to delete \"\(food.foodName)\"?")
        }
    }

    private func summary(_ items: [FoodEntry]) -> String {
        if items.isEmpty { return "No items logged" }
        let cal = Int(items.reduce(0.0) { $0 + ($1.calories ?? 0) })
        let p = Int(items.reduce(0.0) { $0 + ($1.protein ?? 0) })
        let c = Int(items.reduce(0.0) { $0 + ($1.carbs ?? 0) })
        let f = Int(items.reduce(0.0) { $0 + ($1.fat ?? 0) })
        return "\(cal) kcal, \(p)g protein, \(c)g carbs, \(f)g fat"
    }

    private func mealSection(_ category: String, items: [FoodEntry]) -> some View {
        let isOpen = expanded.contains(category)
        return TrackingCardContainer {
            Button(action: {
                if isOpen { expanded.remove(category) } else { expanded.insert(category) }
            }) {
                HStack(spacing: 10) {
                    Text("+")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color(hex: "2563eb"))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(category)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(Color(hex: "1f2937"))
                        Text(summary(items))
                            .font(.system(size: 12))
                            .foregroundColor(Color(hex: "4b5563"))
                    }
                    Spacer()
                    Text(isOpen ? "^" : "v")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(Color(hex: "6b7280"))
                }
            }
            .buttonStyle(.plain)

            if isOpen {
                if items.isEmpty {
                    Text("No items logged")
                        .font(.system(size: 14))
                        .foregroundColor(Color(hex: "4b5563"))
                        .padding(.top, 4)
                } else {
                    ForEach(items) { food in
                        FoodItemCard(food: food) { foodToDelete = food }
                    }
                }
            }
        }
    }
}

/// displayFoodItems card: icon, NAME (uppercase bold 14), quantity 12,
/// four gray boxes label 10 / value 12 bold, delete icon.
struct FoodItemCard: View {
    let food: FoodEntry
    let onDelete: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            Image(systemName: "fork.knife")
                .font(.system(size: 22))
                .foregroundColor(Color(hex: "f97316"))
                .frame(width: 40, height: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(food.foodName.uppercased())
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Color(hex: "1f2937"))
                Text(food.quantity)
                    .font(.system(size: 12))
                    .foregroundColor(Color(hex: "4b5563"))
                    .padding(.bottom, 6)
                HStack(spacing: 4) {
                    nutrientBox("Calories", "\(Int(food.calories ?? 0))")
                    nutrientBox("Protein", String(format: "%.1fg", food.protein ?? 0))
                    nutrientBox("Carbs", String(format: "%.1fg", food.carbs ?? 0))
                    nutrientBox("Fat", String(format: "%.1fg", food.fat ?? 0))
                }
            }
            Button(action: onDelete) {
                Image(systemName: "trash")
                    .font(.system(size: 16))
                    .foregroundColor(Color(hex: "6b7280"))
                    .frame(width: 28, height: 28)
            }
            .accessibilityLabel("Delete food")
        }
        .padding(12)
        .background(Color.white)
        .cornerRadius(12)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "e5e7eb"), lineWidth: 1))
    }

    private func nutrientBox(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(label).font(.system(size: 10)).foregroundColor(Color(hex: "4b5563"))
            Text(value).font(.system(size: 12, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "f8fafc"))
    }
}
```

---

### Task 6: Week and month views (audit C, D)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Tracking/TrackingWeekView.swift`
- Create: `COPDFuel/COPDFuel/Views/Tracking/TrackingMonthView.swift` (+ the two cards moved in Task 2 Step 2)

- [ ] **Step 1: `TrackingWeekView.swift`**

```swift
//
//  TrackingWeekView.swift
//  COPDFuel
//
//  Week view: nav, "Weekly Summary", Trend Report, Weight Tracking,
//  Daily Breakdown (TrackingFragment :305-676).
//

import SwiftUI

struct TrackingWeekView: View {
    @Binding var selectedDate: Date
    @ObservedObject private var dataManager = DataManager.shared

    private var weekStart: Date { TrackingDates.startOfWeek(selectedDate) }
    private var weekEnd: Date {
        let cal = Calendar.current
        let last = cal.date(byAdding: .day, value: 6, to: weekStart) ?? weekStart
        return cal.date(bySettingHour: 23, minute: 59, second: 59, of: last) ?? last
    }

    private var weekLabel: String {
        let thisWeek = TrackingDates.startOfWeek(Date())
        let lastWeek = TrackingDates.startOfWeek(Calendar.current.date(byAdding: .weekOfYear, value: -1, to: Date()) ?? Date())
        if weekStart == thisWeek { return "This Week" }
        if weekStart == lastWeek { return "Last Week" }
        return "Week \(Calendar.current.component(.weekOfYear, from: weekStart))"
    }

    /// Same month → "MMM d - d, yyyy", else "MMM d - MMM d, yyyy".
    private var weekRangeText: String {
        let cal = Calendar.current
        let end = cal.date(byAdding: .day, value: 6, to: weekStart) ?? weekStart
        let startF = DateFormatter(); startF.dateFormat = "MMM d"
        let endF = DateFormatter()
        endF.dateFormat = cal.component(.month, from: weekStart) == cal.component(.month, from: end) ? "d, yyyy" : "MMM d, yyyy"
        return "\(startF.string(from: weekStart)) - \(endF.string(from: end))"
    }

    var body: some View {
        let foods = dataManager.getFoodEntries(from: weekStart, to: weekEnd)
        let weights = dataManager.weightEntries
            .filter { $0.type == .current && $0.date >= weekStart && $0.date <= weekEnd }
            .sorted { $0.date < $1.date }

        VStack(spacing: 16) {
            TrackingPeriodNav(label: weekLabel, range: weekRangeText,
                              onPrev: { shift(-1) }, onNext: { shift(1) })
            Text("Weekly Summary")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color(hex: "22c55e"))
                .frame(maxWidth: .infinity, alignment: .leading)
            TrendReportCard(title: "Trend Report", foods: foods, dayCount: 7, dayKey: { $0 })
            WeightTrackingSection(weights: weights, startLabel: "Start of Week:", endLabel: "End of Week:", changeLabel: "Weekly Change:")
            Text("Daily Breakdown")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
                .frame(maxWidth: .infinity, alignment: .leading)
            dailyBreakdown(foods: foods)
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 24)
    }

    private func shift(_ offset: Int) {
        if let d = Calendar.current.date(byAdding: .weekOfYear, value: offset, to: selectedDate) { selectedDate = d }
    }

    private func dailyBreakdown(foods: [FoodEntry]) -> some View {
        let cal = Calendar.current
        let proteinTarget = Int(UserDefaults.standard.double(forKey: "protein_target"))
        let dateF = DateFormatter(); dateF.dateFormat = "MMM d"
        let dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
        return VStack(spacing: 8) {
            ForEach(0..<7, id: \.self) { i in
                let day = cal.date(byAdding: .day, value: i, to: weekStart) ?? weekStart
                let dayFoods = foods.filter { cal.isDate($0.date, inSameDayAs: day) }
                WeekDayCard(
                    dayName: dayNames[(cal.component(.weekday, from: day) - 1) % 7],
                    dateText: dateF.string(from: day),
                    calories: Int(dayFoods.reduce(0.0) { $0 + ($1.calories ?? 0) }),
                    protein: Int(dayFoods.reduce(0.0) { $0 + ($1.protein ?? 0) }),
                    carbs: Int(dayFoods.reduce(0.0) { $0 + ($1.carbs ?? 0) }),
                    fat: Int(dayFoods.reduce(0.0) { $0 + ($1.fat ?? 0) }),
                    hasData: !dayFoods.isEmpty,
                    proteinTarget: proteinTarget)
            }
        }
    }
}

/// "<" label / range ">" row shared by week and month.
struct TrackingPeriodNav: View {
    let label: String
    let range: String?
    let onPrev: () -> Void
    let onNext: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            navButton("<", onPrev)
            VStack(spacing: 2) {
                Text(label)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(Color(hex: "1f2937"))
                if let range {
                    Text(range)
                        .font(.system(size: 14))
                        .foregroundColor(Color(hex: "4b5563"))
                }
            }
            .frame(maxWidth: .infinity)
            navButton(">", onNext)
        }
    }

    private func navButton(_ glyph: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(glyph)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color(hex: "374151"))
                .frame(width: 48, height: 48)
                .background(Color(hex: "E5E7EB"))
                .cornerRadius(8)
        }
    }
}

/// Trend Report (weekly :419-489, monthly :768-835): averages over days
/// WITH data, total calories, days logged, days met protein goal.
struct TrendReportCard: View {
    let title: String
    let foods: [FoodEntry]
    let dayCount: Int
    /// Maps a food's date to its day bucket (identity; kept for clarity).
    let dayKey: (Date) -> Date

    var body: some View {
        let cal = Calendar.current
        let proteinTarget = Int(UserDefaults.standard.double(forKey: "protein_target"))
        let byDay = Dictionary(grouping: foods) { cal.startOfDay(for: dayKey($0.date)) }
        let daysLogged = byDay.count
        var totalCal = 0.0, totalP = 0.0, totalC = 0.0, totalF = 0.0, daysMet = 0
        for (_, dayFoods) in byDay {
            let p = dayFoods.reduce(0.0) { $0 + ($1.protein ?? 0) }
            totalCal += dayFoods.reduce(0.0) { $0 + ($1.calories ?? 0) }
            totalP += p
            totalC += dayFoods.reduce(0.0) { $0 + ($1.carbs ?? 0) }
            totalF += dayFoods.reduce(0.0) { $0 + ($1.fat ?? 0) }
            if proteinTarget > 0 && p >= Double(proteinTarget) { daysMet += 1 }
        }
        let avg: (Double) -> Int = { daysLogged > 0 ? Int($0 / Double(daysLogged)) : 0 }

        return VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: "166534"))
            HStack(spacing: 8) {
                statColumn("\(avg(totalCal))", "Avg Calories", "22c55e")
                statColumn("\(avg(totalP))g", "Avg Protein", "3b82f6")
                statColumn("\(avg(totalC))g", "Avg Carbs", "f59e0b")
                statColumn("\(avg(totalF))g", "Avg Fat", "ef4444")
            }
            infoRow("Total Calories:", "\(Int(totalCal)) kcal")
            infoRow("Days Logged:", "\(daysLogged) / \(dayCount)")
            infoRow("Days Met Protein Goal:", proteinTarget > 0 ? "\(daysMet) / \(dayCount)" : "Set protein target")
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "f0fdf4"))
        .cornerRadius(12)
    }

    private func statColumn(_ value: String, _ label: String, _ hex: String) -> some View {
        VStack(spacing: 2) {
            Text(value).font(.system(size: 20, weight: .bold)).foregroundColor(Color(hex: hex))
            Text(label).font(.system(size: 11)).foregroundColor(Color(hex: "4b5563"))
        }
        .frame(maxWidth: .infinity)
    }

    private func infoRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label).font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
            Spacer()
            Text(value).font(.system(size: 14, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
        }
    }
}

/// "Weight Tracking" (:374-417 / :723-766): hidden when no current weights.
struct WeightTrackingSection: View {
    let weights: [WeightEntry]   // current only, ascending by date
    let startLabel: String
    let endLabel: String
    let changeLabel: String

    var body: some View {
        if let start = weights.first, let end = weights.last {
            let change = end.weight - start.weight
            let changed = start.id != end.id
            let changeText = changed ? String(format: change >= 0 ? "+%.1f lbs" : "%.1f lbs", change) : "No change"
            let changeHex = !changed ? "6b7280" : (change < 0 ? "22c55e" : (change > 0 ? "ef4444" : "6b7280"))
            TrackingCardContainer {
                Text("Weight Tracking")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color(hex: "1f2937"))
                row(startLabel, String(format: "%.1f lbs", start.weight), "1f2937")
                row(endLabel, String(format: "%.1f lbs", end.weight), "1f2937")
                row(changeLabel, changeText, changeHex)
            }
        }
    }

    private func row(_ label: String, _ value: String, _ hex: String) -> some View {
        HStack {
            Text(label).font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
            Spacer()
            Text(value).font(.system(size: 14, weight: .bold)).foregroundColor(Color(hex: hex))
        }
    }
}

/// createDayCard (:521-653).
struct WeekDayCard: View {
    let dayName: String
    let dateText: String
    let calories: Int
    let protein: Int
    let carbs: Int
    let fat: Int
    let hasData: Bool
    let proteinTarget: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(dayName).font(.system(size: 16, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
                Spacer()
                Text(dateText).font(.system(size: 12)).foregroundColor(Color(hex: "4b5563"))
            }
            if hasData {
                HStack(spacing: 0) {
                    column("\(calories)", "kcal", "22c55e")
                    column("\(protein)g", "Protein", proteinTarget > 0 && protein >= proteinTarget ? "22c55e" : "3b82f6")
                    column("\(carbs)g", "Carbs", "f59e0b")
                    column("\(fat)g", "Fat", "ef4444")
                }
                if proteinTarget > 0 {
                    let met = protein >= proteinTarget
                    Text(met ? "Protein goal met" : "Protein: \(Int(Double(protein) / Double(proteinTarget) * 100))% of target")
                        .font(.system(size: 11))
                        .foregroundColor(Color(hex: met ? "10b981" : "4b5563"))
                }
            } else {
                Text("No food logged")
                    .font(.system(size: 13))
                    .foregroundColor(Color(hex: "4b5563"))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(hasData ? Color.white : Color(hex: "F9FAFB"))
        .cornerRadius(8)
        .shadow(color: .black.opacity(0.05), radius: 2, x: 0, y: 1)
    }

    private func column(_ value: String, _ label: String, _ hex: String) -> some View {
        VStack(spacing: 1) {
            Text(value).font(.system(size: 16, weight: .bold)).foregroundColor(Color(hex: hex))
            Text(label).font(.system(size: 10)).foregroundColor(Color(hex: "4b5563"))
        }
        .frame(maxWidth: .infinity)
    }
}
```

- [ ] **Step 2: `TrackingMonthView.swift`** (append the moved `ExerciseJournalMonthlyCard` and `MonthlyEntryCard` structs verbatim at the end of this file; `MonthlyEntryCard` already uses `entry.timeText` after P3.B)

```swift
//
//  TrackingMonthView.swift
//  COPDFuel
//
//  Month view: "Monthly Summary" title, nav, Monthly Trend Report,
//  Weight Tracking, static "Weekly Breakdown" header, Exercise Journal
//  summary, static "Weekly Exercise Breakdown" header, "All Entries This
//  Month" (TrackingFragment :678-1062).
//

import SwiftUI

struct TrackingMonthView: View {
    @Binding var selectedDate: Date
    @ObservedObject private var dataManager = DataManager.shared
    @State private var journal: [ExerciseJournalEntry] = []

    private var monthStart: Date { TrackingDates.startOfMonth(selectedDate) }
    private var monthEnd: Date { TrackingDates.endOfMonth(selectedDate) }
    private var daysInMonth: Int { TrackingDates.daysInMonth(selectedDate) }

    private var monthLabel: String {
        let f = DateFormatter(); f.dateFormat = "MMM d, yyyy"
        return "\(f.string(from: monthStart)) – \(f.string(from: monthEnd))"
    }

    var body: some View {
        let foods = dataManager.getFoodEntries(from: monthStart, to: monthEnd)
        let weights = dataManager.weightEntries
            .filter { $0.type == .current && $0.date >= monthStart && $0.date <= monthEnd }
            .sorted { $0.date < $1.date }
        let monthEntries = journal.filter { $0.day >= monthStart && $0.day <= monthEnd }

        VStack(spacing: 16) {
            Text("Monthly Summary")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color(hex: "22c55e"))
                .frame(maxWidth: .infinity, alignment: .leading)
            TrackingPeriodNav(label: monthLabel, range: nil, onPrev: { shift(-1) }, onNext: { shift(1) })
            TrendReportCard(title: "Monthly Trend Report", foods: foods, dayCount: daysInMonth, dayKey: { $0 })
            WeightTrackingSection(weights: weights, startLabel: "Start of Month:", endLabel: "End of Month:", changeLabel: "Monthly Change:")

            sectionHeader("Weekly Breakdown")

            sectionHeader("Exercise Journal")
            if monthEntries.isEmpty {
                Text("No exercise journal entries for this month.")
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "4b5563"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                ExerciseJournalMonthlyCard(entries: monthEntries, daysInMonth: daysInMonth, calendar: Calendar.current)
            }

            sectionHeader("Weekly Exercise Breakdown")
            if !monthEntries.isEmpty {
                Text("All Entries This Month")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Color(hex: "1f2937"))
                    .frame(maxWidth: .infinity, alignment: .leading)
                ForEach(monthEntries.sorted { $0.savedAt > $1.savedAt }) { entry in
                    MonthlyEntryCard(entry: entry)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 24)
        .onAppear { journal = ExerciseJournalStore.load() }
    }

    private func shift(_ offset: Int) {
        if let d = Calendar.current.date(byAdding: .month, value: offset, to: selectedDate) { selectedDate = d }
    }

    private func sectionHeader(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 16, weight: .bold))
            .foregroundColor(Color(hex: "1f2937"))
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// (append moved structs ExerciseJournalMonthlyCard and MonthlyEntryCard here)
```

---

### Task 7: Hydration card (audit M)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Tracking/HydrationCard.swift`

- [ ] **Step 1: Create the file**

```swift
//
//  HydrationCard.swift
//  COPDFuel
//
//  Inline "Hydration Tracker" card (fragment_tracking.xml :2610-2740,
//  TrackingFragment.addWater / showCustomWaterDialog / updateWaterDisplay).
//

import SwiftUI

struct HydrationCard: View {
    let selectedDate: Date
    @ObservedObject private var dataManager = DataManager.shared
    @State private var showingCustom = false
    @State private var customAmount = ""

    private static let goalOz = 64
    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter(); f.dateFormat = "h:mm a"; return f
    }()

    var body: some View {
        let entries = dataManager.getWaterEntries(for: selectedDate).sorted { $0.date > $1.date }
        let total = entries.reduce(0) { $0 + $1.amount }
        let percent = min(100, Int(Double(total) / Double(Self.goalOz) * 100))

        TrackingCardContainer {
            HStack {
                Image(systemName: "drop.fill").foregroundColor(Color(hex: "3b82f6"))
                Text("Hydration Tracker")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color(hex: "1f2937"))
                Spacer()
                Text("\(total) oz")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(Color(hex: "3b82f6"))
            }
            HStack(spacing: 8) {
                ProgressView(value: Double(percent) / 100)
                    .tint(Color(hex: percent >= 100 ? "22c55e" : "3b82f6"))
                    .scaleEffect(x: 1, y: 3, anchor: .center)
                Text("/ \(Self.goalOz) oz")
                    .font(.system(size: 12))
                    .foregroundColor(Color(hex: "4b5563"))
            }
            .padding(.vertical, 4)
            HStack(spacing: 8) {
                quickButton("+8 oz") { addWater(8) }
                quickButton("+12 oz") { addWater(12) }
                quickButton("+16 oz") { addWater(16) }
                Button(action: { customAmount = ""; showingCustom = true }) {
                    Text("+")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color(hex: "3b82f6"))
                        .frame(width: 44, height: 40)
                        .background(Color(hex: "dbeafe"))
                        .cornerRadius(8)
                }
            }
            if !entries.isEmpty {
                Text("Today's Drinks (\(entries.count))")
                    .font(.system(size: 12))
                    .foregroundColor(Color(hex: "4b5563"))
                    .padding(.top, 4)
                ForEach(entries) { entry in
                    HStack(spacing: 8) {
                        Text("\(entry.amount) oz")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(Color(hex: "2563eb"))
                        Spacer()
                        Text(Self.timeFormatter.string(from: entry.date))
                            .font(.system(size: 12))
                            .foregroundColor(Color(hex: "4b5563"))
                        Button(action: { dataManager.deleteWaterEntry(id: entry.id) }) {
                            Image(systemName: "trash")
                                .font(.system(size: 14))
                                .foregroundColor(Color(hex: "6b7280"))
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color(hex: "f8fafc"))
                    .cornerRadius(6)
                }
            }
        }
        .alert("Add Water", isPresented: $showingCustom) {
            TextField("Enter amount in oz", text: $customAmount)
                .keyboardType(.numberPad)
            Button("Add") {
                if let amount = Int(customAmount.trimmingCharacters(in: .whitespaces)), amount > 0 {
                    addWater(amount)
                } else {
                    ToastCenter.shared.show("Please enter a valid amount")
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("How many ounces did you drink?")
        }
    }

    private func quickButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(Color(hex: "1d4ed8"))
                .frame(maxWidth: .infinity)
                .frame(height: 40)
                .background(Color(hex: "dbeafe"))
                .cornerRadius(8)
        }
    }

    private func addWater(_ oz: Int) {
        dataManager.addWaterEntry(WaterEntry(date: TrackingDates.timestamp(on: selectedDate), amount: oz))
        ToastCenter.shared.show("+\(oz) oz added")
    }
}
```

---

### Task 8: Exercise, oxygen, steps, heart-rate cards + dialogs (audit N, O, Q77–78)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Tracking/HealthCards.swift`

- [ ] **Step 1: Create the file**

```swift
//
//  HealthCards.swift
//  COPDFuel
//
//  Oxygen Saturation, Exercise Minutes, Steps and Heart Rate cards with
//  the Add Exercise / Add Oxygen Reading dialogs (TrackingFragment
//  :1233-1427, AddExerciseDialog.kt, AddOxygenDialog.kt).
//

import SwiftUI

// MARK: - Oxygen

struct OxygenCard: View {
    let selectedDate: Date
    @ObservedObject private var dataManager = DataManager.shared
    @State private var showingDialog = false

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter(); f.dateFormat = "h:mm:ss a"; return f
    }()

    var body: some View {
        let readings = dataManager.getOxygenReadings(for: selectedDate).sorted { $0.date > $1.date }
        let latest = readings.max { $0.date < $1.date }

        TrackingCardContainer {
            Text("Oxygen Saturation")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            Text(latest.map { "\($0.oxygenLevel)%" } ?? "N/A")
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            TrackingButton(title: "Log Reading", background: Color(hex: "10b981")) { showingDialog = true }
            if !readings.isEmpty {
                Text("Today's Readings (\(readings.count))")
                    .font(.system(size: 12))
                    .foregroundColor(Color(hex: "4b5563"))
                    .padding(.top, 4)
                ForEach(readings) { r in
                    HStack {
                        Text("\(r.oxygenLevel)%")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(Color(hex: "10b981"))
                        Spacer()
                        Text(Self.timeFormatter.string(from: r.date))
                            .font(.system(size: 12))
                            .foregroundColor(Color(hex: "4b5563"))
                    }
                    .padding(.horizontal, 12).padding(.vertical, 8)
                    .background(Color(hex: "f8fafc")).cornerRadius(6)
                }
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { showingDialog = true }
        .sheet(isPresented: $showingDialog) { AddOxygenDialog(selectedDate: selectedDate) }
    }
}

/// AddOxygenDialog.kt + dialog_add_oxygen.xml.
struct AddOxygenDialog: View {
    @Environment(\.dismiss) private var dismiss
    let selectedDate: Date
    @State private var level = ""

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 8) {
                Text("Oxygen Saturation Level (%)").font(.system(size: 14, weight: .bold))
                TextField("Enter oxygen level (0-100)", text: $level)
                    .keyboardType(.numberPad)
                    .textFieldStyle(.roundedBorder)
                Spacer()
            }
            .padding(20)
            .navigationTitle("Add Oxygen Reading")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Save", action: save) }
            }
        }
        .presentationDetents([.medium])
        .toastOverlay()
    }

    private func save() {
        let text = level.trimmingCharacters(in: .whitespaces)
        if text.isEmpty {
            ToastCenter.shared.show("Please enter oxygen level"); return
        }
        guard let value = Int(text), (0...100).contains(value) else {
            ToastCenter.shared.show("Please enter a valid oxygen level (0-100)"); return
        }
        DataManager.shared.addOxygenReading(OxygenReading(date: TrackingDates.timestamp(on: selectedDate), oxygenLevel: value))
        ToastCenter.shared.show("Oxygen reading saved successfully")
        dismiss()
    }
}

// MARK: - Exercise

struct ExerciseCard: View {
    let selectedDate: Date
    @ObservedObject private var dataManager = DataManager.shared
    @State private var showingDialog = false

    var body: some View {
        let exercises = dataManager.getExerciseEntries(for: selectedDate)
        let total = exercises.reduce(0) { $0 + $1.minutes }

        TrackingCardContainer {
            Text("Exercise Minutes")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            Text("\(total) min")
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            TrackingButton(title: "Log Exercise", background: Color(hex: "10b981")) { showingDialog = true }
            ForEach(exercises) { ex in
                HStack {
                    Text(ex.exerciseType)
                        .font(.system(size: 14))
                        .foregroundColor(Color(hex: "1f2937"))
                    Spacer()
                    Text("\(ex.minutes) min")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(Color(hex: "10b981"))
                }
                .padding(.horizontal, 12).padding(.vertical, 8)
                .background(Color(hex: "f8fafc")).cornerRadius(6)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { showingDialog = true }
        .sheet(isPresented: $showingDialog) { AddExerciseDialog(selectedDate: selectedDate) }
    }
}

/// AddExerciseDialog.kt + dialog_add_exercise.xml.
struct AddExerciseDialog: View {
    @Environment(\.dismiss) private var dismiss
    let selectedDate: Date

    static let exerciseTypes = ["Walking", "Running", "Cycling", "Swimming", "Yoga", "Strength Training", "Breathing Exercises", "Other"]

    @State private var type = "Walking"
    @State private var customType = ""
    @State private var minutes = ""

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 8) {
                Text("Exercise Type").font(.system(size: 14, weight: .bold))
                Picker("Exercise Type", selection: $type) {
                    ForEach(Self.exerciseTypes, id: \.self) { Text($0).tag($0) }
                }
                .pickerStyle(.menu)
                if type == "Other" {
                    Text("Custom Exercise Type").font(.system(size: 14, weight: .bold)).padding(.top, 8)
                    TextField("Enter exercise type", text: $customType).textFieldStyle(.roundedBorder)
                }
                Text("Minutes").font(.system(size: 14, weight: .bold)).padding(.top, 8)
                TextField("Enter minutes", text: $minutes)
                    .keyboardType(.numberPad)
                    .textFieldStyle(.roundedBorder)
                Spacer()
            }
            .padding(20)
            .navigationTitle("Add Exercise")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Save", action: save) }
            }
        }
        .presentationDetents([.medium])
        .toastOverlay()
    }

    private func save() {
        var exerciseType = type
        if type == "Other" {
            let custom = customType.trimmingCharacters(in: .whitespacesAndNewlines)
            if custom.isEmpty { ToastCenter.shared.show("Please enter exercise type"); return }
            exerciseType = custom
        }
        let minutesText = minutes.trimmingCharacters(in: .whitespaces)
        if minutesText.isEmpty { ToastCenter.shared.show("Please enter minutes"); return }
        guard let mins = Int(minutesText), mins > 0 else {
            ToastCenter.shared.show("Please enter a valid number of minutes"); return
        }
        DataManager.shared.addExerciseEntry(ExerciseEntry(
            date: TrackingDates.timestamp(on: selectedDate),
            exerciseType: exerciseType,
            minutes: mins,
            customType: type == "Other" ? exerciseType : nil))
        ToastCenter.shared.show("Exercise saved successfully")
        dismiss()
    }
}

// MARK: - Steps

struct StepsCard: View {
    let selectedDate: Date
    @ObservedObject private var dataManager = DataManager.shared
    @State private var liveSteps: Int?

    var body: some View {
        let stored = dataManager.getStepsTotal(for: selectedDate)
        let total = (liveSteps ?? 0) > 0 ? (liveSteps ?? 0) : stored
        TrackingCardContainer {
            Text("Steps")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            Text(total > 0 ? "\(total)" : "N/A")
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
        }
        .task(id: selectedDate) {
            liveSteps = await HealthKitService.shared.readStepsForCalendarDay(selectedDate)
        }
    }
}

// MARK: - Heart rate

struct HeartRateCard: View {
    let selectedDate: Date
    @ObservedObject private var dataManager = DataManager.shared

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter(); f.dateFormat = "h:mm a"; return f
    }()

    var body: some View {
        let rates = dataManager.getHeartRateEntries(for: selectedDate).sorted { $0.date > $1.date }
        TrackingCardContainer {
            Text("Heart Rate")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            Text(rates.first.map { "\($0.bpm) bpm" } ?? "N/A")
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            ForEach(rates.prefix(10)) { r in
                HStack {
                    Text("\(r.bpm) bpm").font(.system(size: 12)).foregroundColor(Color(hex: "1f2937"))
                    Spacer()
                    Text(Self.timeFormatter.string(from: r.date)).font(.system(size: 10)).foregroundColor(Color(hex: "4b5563"))
                }
            }
        }
    }
}
```

---

### Task 9: Weight card + dialog, medications card + dialog (audit P, R)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Tracking/WeightCard.swift`
- Create: `COPDFuel/COPDFuel/Views/Tracking/MedicationsCard.swift`
- Rewrite: `COPDFuel/COPDFuel/Views/MedicationListView.swift` (becomes `MedicationsDialog`)

- [ ] **Step 1: `WeightCard.swift`**

```swift
//
//  WeightCard.swift
//  COPDFuel
//
//  "Weight" card (global latest current + latest goal, per-day entry list)
//  and Add Weight dialog (TrackingFragment :1346-1364, :1429-1500,
//  showWeightDialog; AddWeightDialog.kt; dialog_add_weight.xml).
//

import SwiftUI

struct WeightCard: View {
    let selectedDate: Date
    @ObservedObject private var dataManager = DataManager.shared
    @State private var showingDialog = false

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter(); f.dateFormat = "h:mm a"; return f
    }()

    var body: some View {
        let current = dataManager.getCurrentWeight()
        let goal = dataManager.getGoalWeight()
        let dayEntries = dataManager.weightEntries
            .filter { Calendar.current.isDate($0.date, inSameDayAs: selectedDate) }
            .sorted { $0.date > $1.date }

        TrackingCardContainer {
            Text("Weight")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            Text(current.map { String(format: "%.1f lbs", $0) } ?? "N/A")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            if let goal {
                Text(String(format: "Goal: %.1f lbs", goal))
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "10b981"))
            }
            Text("Tap to log")
                .font(.system(size: 12))
                .foregroundColor(Color(hex: "2563eb"))
            ForEach(dayEntries) { entry in
                let isGoal = entry.type == .goal
                VStack(alignment: .leading, spacing: 2) {
                    (Text(isGoal ? "Goal" : "Current")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(Color(hex: isGoal ? "10b981" : "2563eb"))
                     + Text(String(format: "  %.1f lbs", entry.weight))
                        .font(.system(size: 12))
                        .foregroundColor(Color(hex: "1f2937")))
                    Text(Self.timeFormatter.string(from: entry.date))
                        .font(.system(size: 10))
                        .foregroundColor(Color(hex: "4b5563"))
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .padding(.horizontal, 8).padding(.vertical, 6)
                .background(Color(hex: "f8fafc")).cornerRadius(6)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { showingDialog = true }
        .sheet(isPresented: $showingDialog) { AddWeightDialog(selectedDate: selectedDate) }
    }
}

struct AddWeightDialog: View {
    @Environment(\.dismiss) private var dismiss
    let selectedDate: Date
    @State private var type: WeightEntry.WeightType = .current
    @State private var weight = ""
    @State private var hint = "Enter weight"

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 8) {
                Picker("Type", selection: $type) {
                    Text("Current Weight").tag(WeightEntry.WeightType.current)
                    Text("Goal Weight").tag(WeightEntry.WeightType.goal)
                }
                .pickerStyle(.segmented)
                .onChange(of: type) { _, new in
                    hint = new == .current ? "Enter current weight" : "Enter goal weight"
                }
                Text("Weight (lbs)").font(.system(size: 14, weight: .bold)).padding(.top, 8)
                TextField(hint, text: $weight)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(.roundedBorder)
                Spacer()
            }
            .padding(20)
            .navigationTitle("Add Weight")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Save", action: save) }
            }
        }
        .presentationDetents([.medium])
        .toastOverlay()
    }

    private func save() {
        let text = weight.trimmingCharacters(in: .whitespaces).replacingOccurrences(of: ",", with: ".")
        if text.isEmpty { ToastCenter.shared.show("Please enter weight"); return }
        guard let value = Double(text), value > 0 else {
            ToastCenter.shared.show("Please enter a valid weight"); return
        }
        DataManager.shared.addWeightEntry(WeightEntry(date: TrackingDates.timestamp(on: selectedDate), weight: value, type: type))
        if type == .current { ProfileWeightSync.writeWeightToPrefs(value) }
        ToastCenter.shared.show("Weight saved successfully")
        dismiss()
    }
}
```

- [ ] **Step 2: `MedicationsCard.swift`**

```swift
//
//  MedicationsCard.swift
//  COPDFuel
//
//  "Medications" card on the Tracking day view (TrackingFragment :1137-1204).
//

import SwiftUI

struct MedicationsCard: View {
    @ObservedObject private var dataManager = DataManager.shared
    @State private var showingDialog = false

    var body: some View {
        let daily = dataManager.getDailyMedications()
        let exacerbation = dataManager.getExacerbationMedications()

        TrackingCardContainer {
            Text("Medications")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            Text("\(daily.count) daily")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            Text("Tap to manage")
                .font(.system(size: 12))
                .foregroundColor(Color(hex: "2563eb"))
            if !daily.isEmpty {
                Text("Daily Medications").font(.system(size: 14, weight: .bold)).foregroundColor(Color(hex: "1f2937")).padding(.top, 4)
                ForEach(daily) { med in medRow(med) }
            }
            if !exacerbation.isEmpty {
                Text("Exacerbation Medications").font(.system(size: 14, weight: .bold)).foregroundColor(Color(hex: "1f2937")).padding(.top, 4)
                ForEach(exacerbation) { med in medRow(med) }
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { showingDialog = true }
        .sheet(isPresented: $showingDialog) { MedicationsDialog() }
    }

    private func medRow(_ med: Medication) -> some View {
        Text("\(med.name) - \(med.dosage)")
            .font(.system(size: 16))
            .foregroundColor(Color(hex: "1f2937"))
            .padding(.horizontal, 10).padding(.vertical, 8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: "f8fafc"))
    }
}
```

- [ ] **Step 3: Rewrite `Views/MedicationListView.swift` as `MedicationsDialog`**

```swift
//
//  MedicationListView.swift
//  COPDFuel
//
//  "Medications" management dialog (Android MedicationsDialogFragment +
//  dialog_medications.xml): Daily / Exacerbation / Discontinued sections,
//  always-visible discontinue + remove icon buttons with confirmations,
//  "Add Medication" button, "Close".
//

import SwiftUI

struct MedicationsDialog: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var dataManager = DataManager.shared
    @State private var showingAdd = false
    @State private var medToDiscontinue: Medication?
    @State private var medToDelete: Medication?

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter(); f.dateFormat = "MMM d, yyyy"; return f
    }()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    section("Daily Medications", dataManager.getDailyMedications())
                    section("Exacerbation Medications", dataManager.getExacerbationMedications())
                    TrackingButton(title: "Add Medication") { showingAdd = true }
                    let discontinued = dataManager.getDiscontinuedMedications()
                    if !discontinued.isEmpty {
                        Text("Discontinued Medications").font(.system(size: 16, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
                        ForEach(discontinued) { med in
                            HStack(alignment: .top, spacing: 8) {
                                Text("\(med.name) – \(med.dosage) (\(med.frequency))\nDiscontinued: \(med.discontinuedDate.map { Self.dateFormatter.string(from: $0) } ?? "Unknown date")")
                                    .font(.system(size: 14))
                                    .foregroundColor(Color(hex: "6b7280"))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                deleteButton(med)
                            }
                            .padding(.vertical, 6)
                        }
                    }
                }
                .padding(20)
            }
            .navigationTitle("Medications")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("Close") { dismiss() } }
            }
            .sheet(isPresented: $showingAdd) {
                AddMedicationDialog(defaultType: .daily) { dataManager.addMedication($0) }
            }
            .alert("Discontinue Medication", isPresented: Binding(
                get: { medToDiscontinue != nil }, set: { if !$0 { medToDiscontinue = nil } }
            ), presenting: medToDiscontinue) { med in
                Button("Discontinue") { dataManager.discontinueMedication(med) }
                Button("Cancel", role: .cancel) {}
            } message: { med in
                Text("Mark \"\(med.name)\" as discontinued? It will be moved to the discontinued list with today's date.")
            }
            .alert("Remove Medication", isPresented: Binding(
                get: { medToDelete != nil }, set: { if !$0 { medToDelete = nil } }
            ), presenting: medToDelete) { med in
                Button("Remove", role: .destructive) { dataManager.deleteMedication(med) }
                Button("Cancel", role: .cancel) {}
            } message: { med in
                Text("Permanently remove \"\(med.name)\" from the list?")
            }
        }
        .toastOverlay()
    }

    private func section(_ title: String, _ meds: [Medication]) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.system(size: 16, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
            if meds.isEmpty {
                Text("None added. Tap Add Medication below.")
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "6b7280"))
                    .padding(.vertical, 8)
            } else {
                ForEach(meds) { med in
                    HStack(spacing: 8) {
                        Text("\(med.name) – \(med.dosage) (\(med.frequency))")
                            .font(.system(size: 14))
                            .foregroundColor(Color(hex: "1f2937"))
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Button(action: { medToDiscontinue = med }) {
                            Image(systemName: "nosign")
                                .font(.system(size: 18))
                                .foregroundColor(Color(hex: "ff8800"))
                                .frame(width: 32, height: 32)
                        }
                        .accessibilityLabel("Discontinue medication")
                        deleteButton(med)
                    }
                    .padding(.vertical, 6)
                }
            }
        }
    }

    private func deleteButton(_ med: Medication) -> some View {
        Button(action: { medToDelete = med }) {
            Image(systemName: "trash")
                .font(.system(size: 18))
                .foregroundColor(Color(hex: "6b7280"))
                .frame(width: 32, height: 32)
        }
        .accessibilityLabel("Remove medication")
    }
}
```
The old `MedicationListView`, `MedicationRow`, `DiscontinuedMedicationRow` and the `MedicationTrackingModal` (formerly in TrackingView.swift) are gone. Grep: `grep -rn "MedicationListView\|MedicationTrackingModal" COPDFuel/COPDFuel --include='*.swift'` → expected no output.

---

### Task 10: "COPD Fuel" device-import section (audit Q74–76, F1–F4)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Tracking/HealthImportSection.swift`

- [ ] **Step 1: Create the file**

```swift
//
//  HealthImportSection.swift
//  COPDFuel
//
//  "COPD Fuel" section of the day view: "Import All from Device",
//  "How does this work?", then the Oxygen/Exercise and Steps/Heart Rate
//  cards. Import flow mirrors TrackingFragment.importOxygenFromDevice /
//  runHealthImportAfterPermissionGranted with the Android strings adapted
//  from Health Connect to Apple Health.
//

import SwiftUI
import UIKit

enum HealthImportStrings {
    static let howItWorksTitle = "Import All from Device"
    static let howItWorks = "This brings in readings your iPhone or Apple Watch has already recorded, so you don't have to type them in.\n\n1. Your watch or health app saves its readings to Apple Health, the iPhone's shared health store.\n\n2. When you tap Import All from Device, COPD Fuel asks for your permission, then reads the last 30 days of oxygen saturation (SpO2), weight, exercise sessions, steps and heart rate.\n\n3. Those readings are added to your history here in Tracking.\n\nWe use this data only to display and track it in the app. You can optionally share it with your doctor. We do not use it for advertising or sell it. You can remove access anytime in Settings → Health → Data Access & Devices.\n\nNothing imported? Open your watch or health app, make sure it syncs to Apple Health, then try again. Exercise comes from logged workouts, not from steps."
    static let rationaleTitle = "Why COPD Fuel needs Apple Health access"
    static let rationale = "COPD Fuel is a health tracking app for people with COPD. To import your data from your watch or phone (e.g. Apple Watch, Apple Health), we need read access to:\n\n• Oxygen saturation (SpO2) – to show your oxygen readings in the app\n• Weight – to show your weight history\n• Exercise – to show your workout sessions\n• Steps – to show your daily steps\n• Heart rate – to show your heart rate history\n\nWe use this data only to display and track it in the app. You can optionally share it with your doctor. We do not use it for advertising or sell it. You can revoke access anytime in Settings → Health → Data Access & Devices."
    static let unavailable = "Apple Health is not available on this device."
    static let grant = "Grant access in Settings → Health → Data Access & Devices → COPD Fuel to import data from your device."
    static let none = "No health data found in the last 30 days. If you use an Apple Watch or another health app, make sure it syncs to Apple Health, then try again."
    static let exerciseHint = "Exercise in Apple Health comes from logged workouts (start/stop on Apple Watch or a fitness app), not steps. Log workouts to see them here."
    static func success(_ r: HealthKitImportResult) -> String {
        "Imported: \(r.oxygen.count) oxygen, \(r.weight.count) weight, \(r.exercise.count) exercise, \(r.steps.count) steps, \(r.heartRate.count) heart rate from your device."
    }
}

struct HealthImportSection: View {
    let selectedDate: Date
    @State private var showingHowItWorks = false
    @State private var showingRationale = false
    @State private var isImporting = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("COPD Fuel")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            Button(action: importTapped) {
                Text(isImporting ? "Importing..." : "Import All from Device")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Color(hex: "2563eb"))
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(hex: "2563eb"), lineWidth: 1))
            }
            .disabled(isImporting)
            Button("How does this work?") { showingHowItWorks = true }
                .font(.system(size: 14))
                .foregroundColor(Color(hex: "2563eb"))

            HStack(alignment: .top, spacing: 12) {
                OxygenCard(selectedDate: selectedDate)
                ExerciseCard(selectedDate: selectedDate)
            }
            HStack(alignment: .top, spacing: 12) {
                StepsCard(selectedDate: selectedDate)
                HeartRateCard(selectedDate: selectedDate)
            }
        }
        .alert(HealthImportStrings.howItWorksTitle, isPresented: $showingHowItWorks) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(HealthImportStrings.howItWorks)
        }
        .alert(HealthImportStrings.rationaleTitle, isPresented: $showingRationale) {
            Button("OK") { Task { await runImport() } }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(HealthImportStrings.rationale)
        }
    }

    private func importTapped() {
        guard HealthKitService.shared.isAvailable else {
            ToastCenter.shared.show(HealthImportStrings.unavailable)
            return
        }
        showingRationale = true
    }

    /// Fixed 30-day window; dedupe per DataRepository; profile weight sync.
    @MainActor
    private func runImport() async {
        isImporting = true
        defer { isImporting = false }
        let hk = HealthKitService.shared
        do {
            try await hk.requestAuthorization()
            let end = Date()
            let start = end.addingTimeInterval(-30 * 24 * 60 * 60)
            let result = try await hk.importData(from: start, to: end)
            if result.totalCount == 0 {
                ToastCenter.shared.show(HealthImportStrings.none)
                return
            }
            let dm = DataManager.shared
            result.oxygen.forEach { dm.addOxygenReadingIfAbsent($0) }
            result.heartRate.forEach { dm.addHeartRateEntryIfAbsent($0) }
            result.weight.forEach { dm.addWeightEntry($0) }
            result.exercise.forEach { dm.addExerciseEntry($0) }
            result.steps.forEach { dm.replaceStepsForDayFromImport(day: $0.date, count: $0.count) }
            ProfileWeightSync.syncPrefsFromRepositoryCurrentWeight()
            ToastCenter.shared.show(HealthImportStrings.success(result))
            if result.exercise.isEmpty {
                try? await Task.sleep(for: .seconds(2.6))
                ToastCenter.shared.show(HealthImportStrings.exerciseHint)
            }
        } catch {
            ToastCenter.shared.show(HealthImportStrings.grant)
            if let url = URL(string: "x-apple-health://") { UIApplication.shared.open(url) }
        }
    }
}
```

---

### Task 11: Build, checks, commit

- [ ] **Step 1: Build**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -derivedDataPath "$HOME/Library/Developer/Xcode/DerivedData/COPDFuel-cli" -quiet build 2>&1 | grep -E "error:" | head -20; echo "exit=${PIPESTATUS[0]}"
```
Expected `exit=0`. Likely first-pass issues: `LegacyFoodModals.swift` references `TrackingViewMode`? (no) — references `FoodDatabaseService`, `AppConfig`, `FavoriteMealItem` (all exist). `MonthlyEntryCard` still declares a `timeFormatter` static if P3.B's Task 1 Step 4 was skipped — delete it.

- [ ] **Step 2: Copy checks**

```bash
for s in "Set Up Your Protein Target" "Macronutrient Targets" "Net Carbs - " "Vitamins and Fiber" "No items logged" "Are you sure you want to delete" "+ Quick Add Food" "Create meal (add to favorites)" "Hydration Tracker" "How many ounces did you drink?" "Today's Drinks (" "Import All from Device" "How does this work?" "Oxygen Saturation Level (%)" "Please enter a valid oxygen level (0-100)" "Exercise Minutes" "Please enter a valid number of minutes" "Tap to log" "Weight saved successfully" "Tap to manage" "None added. Tap Add Medication below." "Days Met Protein Goal:" "Set protein target" "Monthly Trend Report" "Weekly Exercise Breakdown"; do
  a=$(grep -rl -F "$s" android/app/src/main/java/com/copdhealthtracker/ui android/app/src/main/res/layout | wc -l | tr -d ' ')
  i=$(grep -rl -F "$s" COPDFuel/COPDFuel/Views/Tracking COPDFuel/COPDFuel/Views/MedicationListView.swift | wc -l | tr -d ' ')
  echo "$a android / $i ios  <- $s"
done
```
Expected: non-zero on both sides for every row.

- [ ] **Step 3: Removal checks**

```bash
grep -rn "Today's Summary\|Day Summary\|WeeklyExerciseCard\|APPLE HEALTH\|Normal range: 95-100%\|Recording for \|goalWeight\|SummaryCard(" COPDFuel/COPDFuel/Views --include='*.swift'
```
Expected: no output (the `goalWeight` hits inside `Views/ProfileView.swift` are P3.E's job; if that file matches, it is acceptable here).

- [ ] **Step 4: Commit**

```bash
cd COPDFuel && /usr/bin/git add -A && /usr/bin/git commit -q -m "iOS parity P3.C (1/2): Tracking rebuilt to Android structure — day nav, macro + nutrient sections, collapsible meals, hydration card, oxygen/exercise/steps/HR cards, weight + medications, device import with dedupe, week/month trend reports, profile weight sync

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>" && /usr/bin/git log --oneline -1
```

- [ ] **Step 5: Manual smoke (simulator)**

Tracking: header band + green Day toggle; "<"/">" step days with "Today"/"Yesterday"; banner appears when Profile weight/activity are empty and "Set Up" jumps to the Profile tab; "Set up" on the Protein row does the same; meal sections start collapsed and show "N kcal, Ng protein…" summaries; "+8 oz" toasts "+8 oz added"; "Log Reading" with 101 toasts the range error; "Log Exercise" with "Other" and empty custom type toasts; Weight card tap → Add Weight → Save writes the profile weight; Medications card → dialog with icon buttons; "How does this work?" shows the Apple Health explainer; Week and Month show the Trend Report with "Set protein target" when no target.
