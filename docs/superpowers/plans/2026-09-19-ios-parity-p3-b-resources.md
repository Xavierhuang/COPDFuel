# iOS Parity P3.B (Resources tab) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the iOS Resources tab so all five tools (Severity Eval, COPD Exacerb., Pulm. Rehab, Resp. Care, Resource Hub) match Android `ResourcesFragment.kt` + `MedicationTypes.kt` verbatim, and move the two Resources logs (exercise journal, doctor instructions) onto Android's pref keys and JSON schema so Tracking → Month and the report read the same data.

**Architecture:** `Views/ResourcesView.swift` (1561 lines) is split into `Views/Resources/*.swift`, one file per tool plus a shell. Two small models with `enum …Store` helpers (`Models/ExerciseJournalEntry.swift`, `Models/InstructionEntry.swift`) own the Android-schema JSON persistence and one-shot migrations from the old iOS keys; every reader (`ResourcesView`, `DailyTrackingSummary`, `ReportGenerator`) goes through them. The iOS severity formula (`Services/SeverityCalculator.swift`) is deleted; the tool becomes Android's "save what you entered" summary. All Android `Toast`s use `ToastCenter.shared.show` (from P3.0). `Medication` gains Android's `date` so lists sort newest first.

**Tech Stack:** Swift 5 / SwiftUI, iOS 17, `xcodebuild`. No app test target; verification = build + copy greps.

**Spec:** `docs/superpowers/specs/2026-09-19-ios-parity-p3-design.md` §3 row P3.B, §1.4 (remove severity formula; remove iOS-only hub entries), §1.5 (keys). Gap list: `docs/superpowers/specs/2026-09-19-ios-parity-p3/audit-resources.md` (every numbered row §0–§5 and §7). Android sources for verbatim copy: `android/app/src/main/java/com/copdhealthtracker/ui/fragments/ResourcesFragment.kt`, `android/app/src/main/java/com/copdhealthtracker/ui/resources/MedicationTypes.kt`.

## Global Constraints

- Android `main` `5c4aa49` is the source of truth; copy verbatim (spec §1.1). Where Android shows a `Toast`, call `ToastCenter.shared.show(text)`; where Android shows an `AlertDialog`, use `.alert` / `.sheet` with the same copy.
- Persisted keys must be Android's: `exercise_journal_log` (JSON String), `doctor_instructions_log` (JSON String), `severity_fev1|hospitalizations|exacerbations|oxygen|result|description|assessment_date` (Strings), contacts `doctor_name`, `doctor_phone`, `emergency_contact_name`, `emergency_contact_phone`, `emergency_contact2_name`, `emergency_contact2_phone`, `insurance_provider` (spec §1.5). Old iOS keys get a one-shot migration then are removed.
- Remove: iOS severity formula and classification, the global disclaimer under the tool chips, "Recommended Exercises", the 8-card `MedicationGuideView`, hub entries NHLBI org / iOS educational items / Emergency Resources, the exercise-journal "Unit" label, always-visible instructions Cancel (spec §1.4, audit §7).
- Keep: the "Weekly Exercise Breakdown" removal belongs to P3.C (Tracking). This plan only adapts `DailyTrackingSummary` to the new journal model.
- Depends on P3.0 (`ToastCenter`, `.toastOverlay()`) and P3.A (`HeaderSection`, `GuidelineCard` are internal types in `HomeView.swift` / `GuidelinesView.swift` and are reused here).
- One commit in the `COPDFuel` repo at the end, after `xcodebuild -project COPDFuel/COPDFuel.xcodeproj -scheme COPDFuel -destination 'generic/platform=iOS Simulator' build` succeeds. Shell commands run from the parent repo root; use `/usr/bin/git`.
- Xcode synchronized folders: new/moved `.swift` files need no `project.pbxproj` edit.

---

### Task 1: Android-schema journal + instructions models with migrations

**Files:**
- Create: `COPDFuel/COPDFuel/Models/ExerciseJournalEntry.swift`
- Create: `COPDFuel/COPDFuel/Models/InstructionEntry.swift`
- Modify: `COPDFuel/COPDFuel/Views/DailyTrackingSummary.swift:25-32` (loader) and `:646-700` (`MonthlyEntryCard` time text)
- Modify: `COPDFuel/COPDFuel/Services/ReportGenerator.swift:234-263` (instructions) and `:441-462` (journal)
- Modify: `COPDFuel/COPDFuel/Views/ResourcesView.swift` — delete the old `InstructionEntry` struct (`:590-600`), `ExerciseJournalEntry` struct (`:1157-1169`) and `ActionPlan` struct (`:817-833`) so the new model files are the only definitions. (The whole file is replaced in Task 3; deleting the three structs now keeps the build green after this task.)

**Interfaces:**
- Produces: `ExerciseJournalEntry` (fields `id: String`, `savedAt: Int64`, `dayMillis: Int64`, `timeText: String`, `warmUp`, `exercise`, `sets: Int`, `reps: Int`, `weight: Double`, `weightUnit`, `activity`; computed `day: Date`, `savedAtDate: Date`; `static func timeText(for: Date) -> String`), `ExerciseJournalStore.load() -> [ExerciseJournalEntry]`, `ExerciseJournalStore.save(_:)`.
- Produces: `InstructionEntry` (`id: String`, `savedAt: Int64`, `text: String`, computed `savedAtDate`), `DoctorInstructionsStore.load()`, `DoctorInstructionsStore.save(_:)`.

- [ ] **Step 1: Create `Models/ExerciseJournalEntry.swift`**

```swift
//
//  ExerciseJournalEntry.swift
//  COPDFuel
//
//  One Exercise Journal row. Persisted as Android's JSON schema under the
//  Android pref key `exercise_journal_log` (ResourcesFragment.kt:1088-1148)
//  so Tracking → Month and the report read the same data.
//

import Foundation

struct ExerciseJournalEntry: Codable, Identifiable, Equatable {
    var id: String
    var savedAt: Int64        // epoch milliseconds
    var dayMillis: Int64      // epoch milliseconds, local midnight of the chosen day
    var timeText: String      // "h:mm AM" / "h:mm PM"
    var warmUp: String
    var exercise: String
    var sets: Int
    var reps: Int
    var weight: Double
    var weightUnit: String
    var activity: String

    var day: Date { Date(timeIntervalSince1970: Double(dayMillis) / 1000) }
    var savedAtDate: Date { Date(timeIntervalSince1970: Double(savedAt) / 1000) }

    init(
        id: String = UUID().uuidString,
        savedAt: Int64,
        dayMillis: Int64,
        timeText: String,
        warmUp: String,
        exercise: String,
        sets: Int,
        reps: Int,
        weight: Double,
        weightUnit: String,
        activity: String
    ) {
        self.id = id
        self.savedAt = savedAt
        self.dayMillis = dayMillis
        self.timeText = timeText
        self.warmUp = warmUp
        self.exercise = exercise
        self.sets = sets
        self.reps = reps
        self.weight = weight
        self.weightUnit = weightUnit
        self.activity = activity
    }

    /// Tolerant decoding mirroring Android's `optString/optLong/…` defaults.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString
        savedAt = try c.decodeIfPresent(Int64.self, forKey: .savedAt) ?? 0
        dayMillis = try c.decodeIfPresent(Int64.self, forKey: .dayMillis) ?? 0
        timeText = try c.decodeIfPresent(String.self, forKey: .timeText) ?? ""
        warmUp = try c.decodeIfPresent(String.self, forKey: .warmUp) ?? ""
        exercise = try c.decodeIfPresent(String.self, forKey: .exercise) ?? ""
        sets = try c.decodeIfPresent(Int.self, forKey: .sets) ?? 0
        reps = try c.decodeIfPresent(Int.self, forKey: .reps) ?? 0
        weight = try c.decodeIfPresent(Double.self, forKey: .weight) ?? 0
        weightUnit = try c.decodeIfPresent(String.self, forKey: .weightUnit) ?? "lbs"
        activity = try c.decodeIfPresent(String.self, forKey: .activity) ?? ""
    }

    /// Android `formatTime()`: "3:05 PM" (no leading zero, upper-case AM/PM).
    static func timeText(for date: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "h:mm a"
        return f.string(from: date)
    }

    static func millis(_ date: Date) -> Int64 {
        Int64((date.timeIntervalSince1970 * 1000).rounded())
    }
}

enum ExerciseJournalStore {
    static let key = "exercise_journal_log"
    private static let legacyKey = "exerciseJournalLog"

    static func load() -> [ExerciseJournalEntry] {
        migrateLegacyIfNeeded()
        guard let raw = UserDefaults.standard.string(forKey: key),
              let data = raw.data(using: .utf8),
              let list = try? JSONDecoder().decode([ExerciseJournalEntry].self, from: data) else {
            return []
        }
        return list
    }

    static func save(_ entries: [ExerciseJournalEntry]) {
        guard let data = try? JSONEncoder().encode(entries),
              let raw = String(data: data, encoding: .utf8) else { return }
        UserDefaults.standard.set(raw, forKey: key)
    }

    /// One-shot: the pre-P3 iOS Codable schema (UUID id, Date savedAt/day/time)
    /// stored as Data under `exerciseJournalLog` → Android schema.
    private static func migrateLegacyIfNeeded() {
        let defaults = UserDefaults.standard
        guard defaults.string(forKey: key) == nil,
              let data = defaults.data(forKey: legacyKey) else { return }
        struct Legacy: Codable {
            let id: UUID; let savedAt: Date; let day: Date; let time: Date
            let warmUp: String; let exercise: String; let sets: Int; let reps: Int
            let weight: Double; let weightUnit: String; let activity: String
        }
        if let old = try? JSONDecoder().decode([Legacy].self, from: data) {
            let cal = Calendar.current
            let migrated = old.map { e in
                ExerciseJournalEntry(
                    id: e.id.uuidString,
                    savedAt: ExerciseJournalEntry.millis(e.savedAt),
                    dayMillis: ExerciseJournalEntry.millis(cal.startOfDay(for: e.day)),
                    timeText: ExerciseJournalEntry.timeText(for: e.time),
                    warmUp: e.warmUp, exercise: e.exercise, sets: e.sets, reps: e.reps,
                    weight: e.weight, weightUnit: e.weightUnit, activity: e.activity
                )
            }
            save(migrated)
        }
        defaults.removeObject(forKey: legacyKey)
    }
}
```

- [ ] **Step 2: Create `Models/InstructionEntry.swift`**

```swift
//
//  InstructionEntry.swift
//  COPDFuel
//
//  "Additional Instructions from Your Doctor" log entry. Persisted as
//  Android's JSON schema under `doctor_instructions_log`
//  (ResourcesFragment.kt:618-665); read by the report.
//

import Foundation

struct InstructionEntry: Codable, Identifiable, Equatable {
    var id: String
    var savedAt: Int64   // epoch milliseconds
    var text: String

    var savedAtDate: Date { Date(timeIntervalSince1970: Double(savedAt) / 1000) }

    init(id: String = UUID().uuidString,
         savedAt: Int64 = Int64((Date().timeIntervalSince1970 * 1000).rounded()),
         text: String) {
        self.id = id
        self.savedAt = savedAt
        self.text = text
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString
        savedAt = try c.decodeIfPresent(Int64.self, forKey: .savedAt) ?? 0
        text = try c.decodeIfPresent(String.self, forKey: .text) ?? ""
    }
}

enum DoctorInstructionsStore {
    static let key = "doctor_instructions_log"

    static func load() -> [InstructionEntry] {
        migrateLegacyIfNeeded()
        guard let raw = UserDefaults.standard.string(forKey: key),
              let data = raw.data(using: .utf8),
              let list = try? JSONDecoder().decode([InstructionEntry].self, from: data) else {
            return []
        }
        return list
    }

    static func save(_ entries: [InstructionEntry]) {
        guard let data = try? JSONEncoder().encode(entries),
              let raw = String(data: data, encoding: .utf8) else { return }
        UserDefaults.standard.set(raw, forKey: key)
    }

    /// One-shot migrations, newest iOS format first:
    /// 1. `doctorInstructionsLog` (Data, Codable {UUID, Date, text})
    /// 2. `actionPlans` (Data, pre-P1 multi-plan list; only `additionalInstructions` survives)
    /// 3. `action_plan_instructions` (single String — Android's own legacy path,
    ///    ResourcesFragment.kt:636-651)
    private static func migrateLegacyIfNeeded() {
        let defaults = UserDefaults.standard
        guard defaults.string(forKey: key) == nil else { return }
        var migrated: [InstructionEntry] = []

        if let data = defaults.data(forKey: "doctorInstructionsLog") {
            struct Legacy: Codable { let id: UUID; let savedAt: Date; let text: String }
            if let old = try? JSONDecoder().decode([Legacy].self, from: data) {
                migrated = old.map {
                    InstructionEntry(id: $0.id.uuidString,
                                     savedAt: Int64(($0.savedAt.timeIntervalSince1970 * 1000).rounded()),
                                     text: $0.text)
                }
            }
        } else if let data = defaults.data(forKey: "actionPlans") {
            struct LegacyPlan: Codable { let date: Date; let additionalInstructions: String }
            if let plans = try? JSONDecoder().decode([LegacyPlan].self, from: data) {
                migrated = plans
                    .filter { !$0.additionalInstructions.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
                    .sorted { $0.date < $1.date }
                    .map { InstructionEntry(savedAt: Int64(($0.date.timeIntervalSince1970 * 1000).rounded()),
                                            text: $0.additionalInstructions) }
            }
        }

        if migrated.isEmpty,
           let legacy = defaults.string(forKey: "action_plan_instructions"),
           !legacy.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            migrated = [InstructionEntry(text: legacy)]
        }

        if !migrated.isEmpty { save(migrated) }
        defaults.removeObject(forKey: "doctorInstructionsLog")
        defaults.removeObject(forKey: "actionPlans")
        defaults.removeObject(forKey: "carePlanZones")
    }
}
```

- [ ] **Step 3: Delete the three old structs from `Views/ResourcesView.swift`**

Remove `struct InstructionEntry: Codable, Identifiable, Equatable { … }` (lines 590-600), `struct ActionPlan: Codable, Identifiable { … }` (817-833) and `struct ExerciseJournalEntry: Codable, Identifiable, Equatable { … }` (1157-1169). (Task 3 replaces the file entirely; this keeps duplicate-type errors out of the intermediate build.)

- [ ] **Step 4: Point `DailyTrackingSummary` at the store**

Replace lines 25-32:
```swift
    private func loadJournalEntries() {
        journalEntries = ExerciseJournalStore.load()
    }
```
In `MonthlyEntryCard` (from line 646): delete the `timeFormatter` static (lines 654-658) and change line 678 to:
```swift
            Text("\(Self.dayFormatter.string(from: entry.day))  •  \(entry.timeText)")
```
`entry.day` is now a computed `Date`; the `$0.savedAt > $1.savedAt` sort (line 434) and `entry.sets/reps/weight/weightUnit` uses compile unchanged.

- [ ] **Step 5: Point `ReportGenerator` at the stores**

Replace `loadInstructionEntries()` (lines 257-263) with:
```swift
    private func loadInstructionEntries() -> [InstructionEntry] {
        DoctorInstructionsStore.load()
    }
```
and in `carePlanSection()` change line 240 to `s += "  • [\(f.string(from: entry.savedAtDate))] \(entry.text)\n"` (the sort on line 239 compares `Int64` now and compiles as-is).

Replace `exerciseJournalSummary()` (lines 441-462) with:
```swift
    private func exerciseJournalSummary() -> String {
        var s = "EXERCISE JOURNAL:\n"
        let entries = ExerciseJournalStore.load().sorted { $0.savedAt > $1.savedAt }
        if entries.isEmpty {
            s += "  No journal entries saved.\n"
        } else {
            let dayF = DateFormatter()
            dayF.dateFormat = "MMM d, yyyy"
            for e in entries {
                let name = e.exercise.isEmpty ? "(unnamed)" : e.exercise
                s += "  • \(dayF.string(from: e.day)) \(e.timeText) — \(name): sets \(e.sets), reps \(e.reps), weight \(String(format: "%.1f", e.weight)) \(e.weightUnit)\n"
            }
        }
        s += "\n"
        return s
    }
```
(The 1:1 Android EXERCISE JOURNAL format with the 30-day window is P3.E; this step only moves the read onto the shared model.)

- [ ] **Step 6: Build**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -derivedDataPath "$HOME/Library/Developer/Xcode/DerivedData/COPDFuel-cli" -quiet build 2>&1 | grep -E "error:|warning: unused" | head; echo "exit=${PIPESTATUS[0]}"
```
Expected `exit=0`. The old `ExerciseJournalView` in `ResourcesView.swift` still constructs `ExerciseJournalEntry(id: UUID(), savedAt: Date(), day:…)` and will fail to compile; that is expected only if you build before Task 3 — so **skip this build if you proceed straight to Task 3**, otherwise temporarily comment out `ExerciseJournalView.saveEntry()`'s body. Recommended: go straight on to Task 2 and 3 and build at Task 3 Step 4.

---

### Task 2: `Medication.date` + newest-first ordering (audit 2c.5)

**Files:**
- Modify: `COPDFuel/COPDFuel/Models/Medication.swift`
- Modify: `COPDFuel/COPDFuel/Services/DataManager.swift:153-163`

- [ ] **Step 1: Add `date`**

In `Medication.swift`: after `let type: MedicationType` add `let date: Date` with a doc comment `/// Creation timestamp (Android Medication.date); lists sort by it newest first.` Add `date: Date = Date(),` to the memberwise `init` parameter list (after `type:`) and `self.date = date`. In `init(from:)` add:
```swift
        date = try c.decodeIfPresent(Date.self, forKey: .date) ?? Date(timeIntervalSince1970: 0)
```
(Legacy rows without a date sort last, which preserves their prior relative order at the end of the list.)

- [ ] **Step 2: Sort in `DataManager`**

Replace the three getters:
```swift
    func getDailyMedications() -> [Medication] {
        medications.filter { $0.type == .daily && !$0.isDiscontinued }
            .sorted { $0.date > $1.date }
    }

    func getExacerbationMedications() -> [Medication] {
        medications.filter { $0.type == .exacerbation && !$0.isDiscontinued }
            .sorted { $0.date > $1.date }
    }

    func getDiscontinuedMedications() -> [Medication] {
        medications.filter { $0.isDiscontinued }
            .sorted { ($0.discontinuedDate ?? .distantPast) > ($1.discontinuedDate ?? .distantPast) }
    }
```

---

### Task 3: Resources shell + Severity Eval (audit §0, §1)

**Files:**
- Move: `COPDFuel/COPDFuel/Views/ResourcesView.swift` → `COPDFuel/COPDFuel/Views/Resources/ResourcesView.swift` (then replace contents)
- Create: `COPDFuel/COPDFuel/Views/Resources/SeveritySummaryView.swift`
- Delete: `COPDFuel/COPDFuel/Services/SeverityCalculator.swift`
- Modify: `COPDFuel/COPDFuel/Services/ReportGenerator.swift:194-215` (severity key reads)

**Interfaces:**
- Consumes: `HeaderSection` (HomeView.swift), `ToastCenter`.
- Produces: `ResourcesView` (tab root). Tools switch on `selectedTool` 0…4 → `SeveritySummaryView`, `ActionPlanView` (Task 4), `PulmonaryRehabView` (Task 5), `MedicationGuideView` (Task 6), `ResourceHubView` (Task 7).

- [ ] **Step 1: Move the file, delete the calculator**

```bash
cd COPDFuel && mkdir -p COPDFuel/Views/Resources \
 && /usr/bin/git mv COPDFuel/Views/ResourcesView.swift COPDFuel/Views/Resources/ResourcesView.swift \
 && /usr/bin/git rm -q COPDFuel/Services/SeverityCalculator.swift && /usr/bin/git status --short | head
```

- [ ] **Step 2: Write `Views/Resources/ResourcesView.swift` (whole file)**

```swift
//
//  ResourcesView.swift
//  COPDFuel
//
//  Resources tab shell: green "COPD Fuel" header band, yellow
//  "COPD Management Tools" banner, five equal-width tool cards, and the
//  selected tool's content. Mirrors Android fragment_resources.xml +
//  ResourcesFragment.kt:45-137.
//

import SwiftUI

struct ResourcesView: View {
    @State private var selectedTool = 0

    private let tools: [(label: String, symbol: String)] = [
        ("Severity\nEval", "cross.case.fill"),
        ("COPD\nExacerb.", "doc.text.fill"),
        ("Pulm.\nRehab", "lungs.fill"),
        ("Resp.\nCare", "pills.fill"),
        ("Resource\nHub", "books.vertical.fill")
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    HeaderSection()
                    banner
                    toolRow
                    Group {
                        switch selectedTool {
                        case 0: SeveritySummaryView()
                        case 1: ActionPlanView()
                        case 2: PulmonaryRehabView()
                        case 3: MedicationGuideView()
                        default: ResourceHubView()
                        }
                    }
                    .padding(20)
                }
            }
            .edgesIgnoringSafeArea(.top)
            .toolbar(.hidden, for: .navigationBar)
        }
    }

    private var banner: some View {
        VStack(spacing: 4) {
            Text("COPD Management Tools")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(Color(hex: "92400e"))
            Text("Specialized tools to help you manage your COPD effectively")
                .font(.system(size: 14))
                .foregroundColor(Color(hex: "92400e"))
        }
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity)
        .padding(16)
        .background(Color(hex: "fef3c7"))
    }

    private var toolRow: some View {
        HStack(spacing: 8) {
            ForEach(tools.indices, id: \.self) { index in
                let selected = selectedTool == index
                Button(action: { selectedTool = index }) {
                    VStack(spacing: 6) {
                        Image(systemName: tools[index].symbol)
                            .font(.system(size: 22))
                        Text(tools[index].label)
                            .font(.system(size: 11, weight: .semibold))
                            .multilineTextAlignment(.center)
                            .lineLimit(2)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .foregroundColor(selected ? Color(hex: "2563eb") : Color(hex: "6b7280"))
                    .background(selected ? Color(hex: "eff6ff") : Color(hex: "f8fafc"))
                    .cornerRadius(10)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(selected ? Color(hex: "2563eb") : Color(hex: "e5e7eb"), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 12)
    }
}

// MARK: - Shared Resources styling helpers

/// Android `cardBackgroundBlue` (#eff6ff) info card, 16sp body.
struct ResourcesInfoCard: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 16))
            .foregroundColor(Color(hex: "4b5563"))
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: "eff6ff"))
            .cornerRadius(12)
    }
}

/// Android tool page title: 24sp bold #1f2937.
struct ResourcesTitle: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 24, weight: .bold))
            .foregroundColor(Color(hex: "1f2937"))
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Android section heading inside a tool: 22sp bold colorPrimary.
struct ResourcesHeading: View {
    let text: String
    var italic = false
    var body: some View {
        Text(text)
            .font(.system(size: 22, weight: .bold))
            .italic(italic)
            .foregroundColor(Color(hex: "2563eb"))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 8)
    }
}

/// Android sub-heading: 18sp bold colorPrimary.
struct ResourcesSubheading: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 18, weight: .bold))
            .foregroundColor(Color(hex: "2563eb"))
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Full-width filled button (Android primary/colorSuccess buttons).
struct ResourcesFilledButton: View {
    let title: String
    var background: Color = Color(hex: "2563eb")
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(background)
                .cornerRadius(8)
        }
    }
}

/// Opens a URL in Safari with the filled-button look.
struct ResourcesLinkButton: View {
    let title: String
    let url: String
    var background: Color = Color(hex: "2563eb")
    var body: some View {
        Link(destination: URL(string: url)!) {
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(background)
                .cornerRadius(8)
        }
    }
}
```

- [ ] **Step 3: Create `Views/Resources/SeveritySummaryView.swift`**

```swift
//
//  SeveritySummaryView.swift
//  COPDFuel
//
//  Resources › Severity Eval. Android ResourcesFragment.buildSeverityContent
//  (:139-244) is a "save what you entered" summary with NO severity formula;
//  the iOS classification was removed on purpose (spec §1.4).
//

import SwiftUI

struct SeveritySummaryView: View {
    static let fev1Options = ["Select FEV1 percentage", "80% or higher", "50-79%", "30-49%", "Less than 30%", "I don't know"]
    static let countOptions = ["Select number", "0", "1", "2", "3 or more"]
    static let oxygenOptions = ["Select level", "Yes", "No"]

    @State private var fev1 = SeveritySummaryView.fev1Options[0]
    @State private var hospitalizations = SeveritySummaryView.countOptions[0]
    @State private var flareUps = SeveritySummaryView.countOptions[0]
    @State private var oxygen = SeveritySummaryView.oxygenOptions[0]
    @State private var summaryText: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ResourcesTitle(text: "COPD Severity Assessment")

            ResourcesInfoCard(text: "This screen summarizes information you enter for your own tracking. It does not provide medical advice, diagnosis, or treatment recommendations.")

            SeverityPickerRow(
                question: "What is your latest FEV1 (Forced Expiratory Volume in one second) percentage? (If known)",
                options: Self.fev1Options, selection: $fev1)
            SeverityPickerRow(
                question: "How many times have you been hospitalized for COPD in the past year?",
                options: Self.countOptions, selection: $hospitalizations)
            SeverityPickerRow(
                question: "How many COPD flare-ups (exacerbations) have you had in the past year?",
                options: Self.countOptions, selection: $flareUps)
            SeverityPickerRow(
                question: "Do you use supplemental oxygen?",
                options: Self.oxygenOptions, selection: $oxygen)

            ResourcesFilledButton(title: "Save Summary", action: saveSummary)

            if let summaryText {
                ResourcesInfoCard(text: summaryText)
            }
        }
    }

    /// ResourcesFragment.kt:199-229 — no validation, no computation.
    private func saveSummary() {
        let oxygenLine = oxygen == "Yes" ? "Yes" : "No"
        let lines = [
            "FEV1: \(fev1)",
            "Hospitalizations (past year): \(hospitalizations)",
            "Flare-ups (past year): \(flareUps)",
            "Uses supplemental oxygen: \(oxygenLine)"
        ]
        let text = "Saved summary:\n\n" + lines.joined(separator: "\n")

        let dateF = DateFormatter()
        dateF.dateFormat = "MMM d, yyyy"
        let d = UserDefaults.standard
        d.set(fev1, forKey: "severity_fev1")
        d.set(hospitalizations, forKey: "severity_hospitalizations")
        d.set(flareUps, forKey: "severity_exacerbations")
        d.set(oxygen, forKey: "severity_oxygen")
        d.set("Tracking summary (not a medical assessment)", forKey: "severity_result")
        d.set("Saved for your personal tracking and to discuss with your clinician.", forKey: "severity_description")
        d.set(dateF.string(from: Date()), forKey: "severity_assessment_date")
        // Retire the pre-P3 iOS-only keys.
        d.removeObject(forKey: "severity_flareups")
        d.removeObject(forKey: "severity_saved_at")

        summaryText = text
    }
}

/// item_guideline-style row (gray card, 4dp primary bar, 18sp bold question,
/// 14sp value) whose value opens the option list (Android AlertDialog.setItems).
struct SeverityPickerRow: View {
    let question: String
    let options: [String]
    @Binding var selection: String

    var body: some View {
        HStack(spacing: 0) {
            Rectangle().fill(Color(hex: "2563eb")).frame(width: 4)
            VStack(alignment: .leading, spacing: 6) {
                Text(question)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(Color(hex: "1f2937"))
                Menu {
                    ForEach(options, id: \.self) { option in
                        Button(option) { selection = option }
                    }
                } label: {
                    HStack(spacing: 6) {
                        Text(selection)
                            .font(.system(size: 14))
                            .foregroundColor(Color(hex: "2563eb"))
                        Image(systemName: "chevron.down")
                            .font(.system(size: 12))
                            .foregroundColor(Color(hex: "2563eb"))
                    }
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Color(hex: "f8fafc"))
        .cornerRadius(12)
    }
}
```

- [ ] **Step 4: Update the report's severity reads (lockstep with the new keys)**

In `ReportGenerator.swift` replace lines 194-199 with:
```swift
        let fev1 = defaults.string(forKey: "severity_fev1") ?? ""
        let hosp = defaults.string(forKey: "severity_hospitalizations") ?? ""
        let flares = defaults.string(forKey: "severity_exacerbations") ?? ""
        let usesOxygenAnswer = defaults.string(forKey: "severity_oxygen") ?? ""
        let severity = defaults.string(forKey: "severity_result") ?? ""
        let description = defaults.string(forKey: "severity_description") ?? ""
        let savedOn = defaults.string(forKey: "severity_assessment_date") ?? ""
```
and lines 214-215 with:
```swift
            if !severity.isEmpty { s += "Note: \(severity)\n" }
            if !description.isEmpty { s += "Details: \(description)\n" }
            if !savedOn.isEmpty { s += "Saved On: \(savedOn)\n" }
```
Also delete the stale comment "(see SeverityCalculator)" on line 191 (change to "(see SeveritySummaryView)").

- [ ] **Step 5: Build** — the file still contains the old `ActionPlanView`, `PulmonaryRehabView`, `MedicationGuideView`, `ResourceHubView`? No: Step 2 replaced the whole file, so those types no longer exist until Tasks 4-7 add them. **Do not build until Task 7 is done**, or temporarily stub the four views (`struct ActionPlanView: View { var body: some View { EmptyView() } }` etc.) if you want an intermediate build. Recommended: continue to Task 4.

---

### Task 4: COPD Exacerb. / Action Plan (audit §2)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Resources/ActionPlanView.swift`

**Interfaces:**
- Consumes: `DataManager.shared` (`medications`, `addMedication`, `deleteMedication`, `getDailyMedications`, `getExacerbationMedications`), `DoctorInstructionsStore`, `ToastCenter`, `ResourcesTitle`, `ResourcesInfoCard`, `ResourcesHeading`, `ResourcesFilledButton`.
- Produces: `ActionPlanView`, `AddMedicationDialog(defaultType:onSave:)` (also usable by P3.C's Medications dialog).

- [ ] **Step 1: Create the file**

```swift
//
//  ActionPlanView.swift
//  COPDFuel
//
//  Resources › COPD Exacerb. Mirrors ResourcesFragment.buildActionPlanContent
//  (:246-800): contacts ×7, medication plan, action-plan zones, doctor
//  instructions log.
//

import SwiftUI

struct ActionPlanView: View {
    @ObservedObject private var dataManager = DataManager.shared

    // Contacts (persisted only on "Save Contacts", ResourcesFragment.kt:355-373)
    @State private var doctorName = ""
    @State private var doctorPhone = ""
    @State private var emergencyName = ""
    @State private var emergencyPhone = ""
    @State private var emergency2Name = ""
    @State private var emergency2Phone = ""
    @State private var insuranceProvider = ""

    // Medication plan
    @State private var addMedicationType: Medication.MedicationType?
    @State private var medicationToDelete: Medication?

    // Instructions log
    @State private var instructionsLog: [InstructionEntry] = []
    @State private var isEditingInstructions = false
    @State private var newInstructionText = ""
    @State private var instructionToDelete: InstructionEntry?

    private static let savedFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "MMM d, yyyy h:mm a"
        return f
    }()

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            ResourcesTitle(text: "COPD Exacerbation Action Plan")
            ResourcesInfoCard(text: "This care plan should be created in partnership with your healthcare provider. Use this template to document your personalized plan for managing COPD flare-ups.")
            contactsSection
            medicationPlanSection
            zonesSection
            instructionsSection
        }
        .onAppear(perform: load)
        .sheet(item: $addMedicationType) { type in
            AddMedicationDialog(defaultType: type) { dataManager.addMedication($0) }
        }
        .alert("Delete medication?", isPresented: Binding(
            get: { medicationToDelete != nil },
            set: { if !$0 { medicationToDelete = nil } }
        ), presenting: medicationToDelete) { med in
            Button("Delete", role: .destructive) { dataManager.deleteMedication(med) }
            Button("Cancel", role: .cancel) {}
        } message: { med in
            Text("\(med.name) will be removed.")
        }
        .alert("Delete instruction?", isPresented: Binding(
            get: { instructionToDelete != nil },
            set: { if !$0 { instructionToDelete = nil } }
        ), presenting: instructionToDelete) { entry in
            Button("Delete", role: .destructive) {
                instructionsLog.removeAll { $0.id == entry.id }
                DoctorInstructionsStore.save(instructionsLog)
            }
            Button("Cancel", role: .cancel) {}
        } message: { _ in
            Text("This entry will be permanently removed.")
        }
    }

    private func load() {
        let d = UserDefaults.standard
        doctorName = d.string(forKey: "doctor_name") ?? ""
        doctorPhone = d.string(forKey: "doctor_phone") ?? ""
        emergencyName = d.string(forKey: "emergency_contact_name") ?? ""
        emergencyPhone = d.string(forKey: "emergency_contact_phone") ?? ""
        emergency2Name = d.string(forKey: "emergency_contact2_name") ?? ""
        emergency2Phone = d.string(forKey: "emergency_contact2_phone") ?? ""
        insuranceProvider = d.string(forKey: "insurance_provider") ?? ""
        instructionsLog = DoctorInstructionsStore.load()
    }

    // MARK: Contacts (:305-373)

    private var contactsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            ResourcesHeading(text: "Important Contacts")
            ContactField(label: "Doctor's Name", hint: "Dr. Smith", text: $doctorName)
            ContactField(label: "Doctor's Phone", hint: "(555) 123-4567", text: $doctorPhone, keyboard: .phonePad)
            ContactField(label: "Emergency Contact Name", hint: "Jane Doe", text: $emergencyName)
            ContactField(label: "Emergency Contact Phone", hint: "(555) 987-6543", text: $emergencyPhone, keyboard: .phonePad)
            ContactField(label: "Second Emergency Contact Name", hint: "John Doe", text: $emergency2Name)
            ContactField(label: "Second Emergency Contact Phone", hint: "(555) 222-3333", text: $emergency2Phone, keyboard: .phonePad)
            ContactField(label: "Insurance Provider (name only, no ID numbers)", hint: "e.g. Medicare, Humana", text: $insuranceProvider)
            ResourcesFilledButton(title: "Save Contacts") {
                let d = UserDefaults.standard
                d.set(doctorName, forKey: "doctor_name")
                d.set(doctorPhone, forKey: "doctor_phone")
                d.set(emergencyName, forKey: "emergency_contact_name")
                d.set(emergencyPhone, forKey: "emergency_contact_phone")
                d.set(emergency2Name, forKey: "emergency_contact2_name")
                d.set(emergency2Phone, forKey: "emergency_contact2_phone")
                d.set(insuranceProvider, forKey: "insurance_provider")
                ToastCenter.shared.show("Contacts saved")
            }
        }
    }

    // MARK: Medication plan (:375-516)

    private var medicationPlanSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            ResourcesHeading(text: "Medication Plan")

            ResourcesSubheading(text: "Daily Medications")
            medicationList(dataManager.getDailyMedications(), emptyText: "No daily medications added yet.")
            ResourcesFilledButton(title: "+ Add Daily Medication") { addMedicationType = .daily }

            ResourcesSubheading(text: "Exacerbation Medications")
                .padding(.top, 8)
            medicationList(dataManager.getExacerbationMedications(), emptyText: "No exacerbation medications added yet.")
            ResourcesFilledButton(title: "+ Add Exacerbation Medication") { addMedicationType = .exacerbation }
        }
    }

    @ViewBuilder
    private func medicationList(_ meds: [Medication], emptyText: String) -> some View {
        if meds.isEmpty {
            Text(emptyText)
                .font(.system(size: 14))
                .foregroundColor(Color(hex: "6b7280"))
        } else {
            ForEach(meds) { med in
                HStack(alignment: .top) {
                    Text("\(med.name) - \(med.dosage) (\(med.frequency))")
                        .font(.system(size: 14))
                        .foregroundColor(Color(hex: "1f2937"))
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button("Delete") { medicationToDelete = med }
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.red)
                }
                .padding(12)
                .background(Color(hex: "f8fafc"))
                .cornerRadius(8)
            }
        }
    }

    // MARK: Zones (:518-612)

    private var zonesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            ResourcesHeading(text: "Action Plan Zones")
            ZoneCard(
                title: "Green Zone: I'm Doing Well",
                symptoms: [
                    "Usual activity and exercise level",
                    "Usual amounts of cough and phlegm/mucus",
                    "Sleep well at night",
                    "Appetite is good"
                ],
                action: "Take daily medications as prescribed",
                light: "99cc00", dark: "669900")
            ZoneCard(
                title: "Yellow Zone: I'm Having a Bad Day",
                symptoms: [
                    "More breathless than usual",
                    "I have less energy for my daily activities",
                    "Increased or thicker phlegm/mucus",
                    "Using quick relief inhaler/nebulizer more often",
                    "Swelling of ankles more than usual",
                    "More coughing than usual",
                    "I feel like I have a cold",
                    "I'm not sleeping well",
                    "My appetite is not good"
                ],
                action: "Use your clinician-provided plan. If symptoms worsen, contact your clinician.",
                light: "ffbb33", dark: "ff8800")
            ZoneCard(
                title: "Red Zone: I Need Urgent Medical Care",
                symptoms: [
                    "Severe shortness of breath, even at rest",
                    "Not able to do any activity because of breathing",
                    "Not able to sleep because of breathing",
                    "Fever or shaking chills",
                    "Feeling confused or very drowsy",
                    "Chest pains",
                    "Coughing up blood"
                ],
                action: "Seek urgent medical care immediately (use local emergency services).",
                light: "ff4444", dark: "cc0000")
        }
    }

    // MARK: Instructions log (:614-800)

    private var instructionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            ResourcesHeading(text: "Additional Instructions from Your Doctor")
            if isEditingInstructions {
                ZStack(alignment: .topLeading) {
                    if newInstructionText.isEmpty {
                        Text("Enter new instructions from your doctor...")
                            .font(.system(size: 16))
                            .foregroundColor(Color(hex: "9ca3af"))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 12)
                    }
                    TextEditor(text: $newInstructionText)
                        .font(.system(size: 16))
                        .frame(minHeight: 110)
                        .padding(4)
                        .scrollContentBackground(.hidden)
                }
                .background(Color(hex: "f8fafc"))
                .cornerRadius(8)

                ResourcesFilledButton(title: "Save") {
                    let trimmed = newInstructionText.trimmingCharacters(in: .whitespacesAndNewlines)
                    if trimmed.isEmpty {
                        ToastCenter.shared.show("Instructions cannot be empty")
                        return
                    }
                    instructionsLog.append(InstructionEntry(text: trimmed))
                    DoctorInstructionsStore.save(instructionsLog)
                    ToastCenter.shared.show("Instructions saved")
                    newInstructionText = ""
                    isEditingInstructions = false
                }
                if !instructionsLog.isEmpty {
                    ResourcesFilledButton(title: "Cancel", background: Color(hex: "6b7280")) {
                        newInstructionText = ""
                        isEditingInstructions = false
                    }
                }
            } else {
                ResourcesFilledButton(title: "+ Add New Instruction") {
                    newInstructionText = ""
                    isEditingInstructions = true
                }
                if instructionsLog.isEmpty {
                    Text("No instructions saved yet.")
                        .font(.system(size: 14))
                        .foregroundColor(Color(hex: "6b7280"))
                } else {
                    ForEach(instructionsLog.sorted { $0.savedAt > $1.savedAt }) { entry in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text("Saved: \(Self.savedFormatter.string(from: entry.savedAtDate))")
                                    .font(.system(size: 12))
                                    .foregroundColor(Color(hex: "6b7280"))
                                Spacer()
                                Button("Delete") { instructionToDelete = entry }
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(.red)
                            }
                            Text(entry.text)
                                .font(.system(size: 14))
                                .foregroundColor(Color(hex: "1f2937"))
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .padding(12)
                        .background(Color(hex: "f8fafc"))
                        .cornerRadius(8)
                    }
                }
            }
        }
    }
}

/// Label 16sp bold + text field with Android hint.
struct ContactField: View {
    let label: String
    let hint: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            TextField(hint, text: $text)
                .keyboardType(keyboard)
                .padding(10)
                .background(Color(hex: "f8fafc"))
                .cornerRadius(6)
                .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color(hex: "e5e7eb"), lineWidth: 1))
        }
    }
}

/// Zone card: title, symptom lines "  symptom" (no bullet), bold
/// "Action: …" on the holo light colour with holo dark text (:527-556).
struct ZoneCard: View {
    let title: String
    let symptoms: [String]
    let action: String
    let light: String
    let dark: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color(hex: dark))
            ForEach(symptoms, id: \.self) { s in
                Text("  \(s)")
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "1f2937"))
            }
            Text("Action: \(action)")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(Color(hex: dark))
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(hex: light))
                .cornerRadius(6)
                .padding(.top, 4)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "f8fafc"))
        .cornerRadius(12)
    }
}

/// AddMedicationDialog.kt:22-64 + dialog_add_medication.xml.
struct AddMedicationDialog: View {
    @Environment(\.dismiss) private var dismiss
    let defaultType: Medication.MedicationType
    let onSave: (Medication) -> Void

    @State private var type: Medication.MedicationType = .daily
    @State private var name = ""
    @State private var dosage = ""
    @State private var frequency = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Type").font(.system(size: 14, weight: .bold))
                    Picker("Type", selection: $type) {
                        Text("Daily").tag(Medication.MedicationType.daily)
                        Text("Exacerbation").tag(Medication.MedicationType.exacerbation)
                    }
                    .pickerStyle(.segmented)

                    Text("Medication Name").font(.system(size: 14, weight: .bold))
                    TextField("Enter name", text: $name).textFieldStyle(.roundedBorder)

                    Text("Dosage").font(.system(size: 14, weight: .bold))
                    TextField("e.g., 2 puffs", text: $dosage).textFieldStyle(.roundedBorder)

                    Text("Frequency").font(.system(size: 14, weight: .bold))
                    TextField("e.g., twice daily", text: $frequency).textFieldStyle(.roundedBorder)
                }
                .padding(20)
            }
            .navigationTitle("Add Medication")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
                        if trimmedName.isEmpty {
                            ToastCenter.shared.show("Enter medication name")
                            return
                        }
                        onSave(Medication(
                            name: trimmedName,
                            dosage: dosage.trimmingCharacters(in: .whitespacesAndNewlines),
                            frequency: frequency.trimmingCharacters(in: .whitespacesAndNewlines),
                            type: type))
                        ToastCenter.shared.show("Medication saved")
                        dismiss()
                    }
                }
            }
            .onAppear { type = defaultType }
        }
        .toastOverlay()
    }
}

extension Medication.MedicationType: Identifiable {
    var id: String { rawValue }
}
```

---

### Task 5: Pulm. Rehab + Exercise Journal (audit §3, §3x)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Resources/PulmonaryRehabView.swift`
- Create: `COPDFuel/COPDFuel/Views/Resources/ExerciseJournalView.swift`

**Interfaces:**
- Consumes: `GuidelineCard(title:detail:)` (GuidelinesView.swift), `ProgramsNearMeView()`, `ExerciseJournalStore`, `ExerciseJournalEntry`, `ToastCenter`, `Resources*` helpers.

- [ ] **Step 1: Create `PulmonaryRehabView.swift`**

```swift
//
//  PulmonaryRehabView.swift
//  COPDFuel
//
//  Resources › Pulm. Rehab. Section order and copy mirror
//  ResourcesFragment.buildPulmonaryContent (:802-1534): title → intro →
//  benefits → finding a program → button → home exercise program → track
//  your progress → exercise journal → full-width image.
//

import SwiftUI

struct PulmonaryRehabView: View {
    private struct HomeExercise { let name: String; let detail: String; let frequency: String }

    private let breathing: [HomeExercise] = [
        HomeExercise(name: "Pursed-Lip Breathing",
                     detail: "Breathe in through your nose for 2 counts, then breathe out slowly through pursed lips for 4 counts. This helps control breathlessness and slows your breathing rate.",
                     frequency: "Recommended frequency: 5-10 minutes, 4-5 times daily"),
        HomeExercise(name: "Diaphragmatic Breathing",
                     detail: "Place one hand on your chest and the other on your abdomen. Breathe in through your nose, feeling your abdomen rise. Breathe out through pursed lips while gently pressing on your abdomen.",
                     frequency: "Recommended frequency: 5-10 minutes, 3-4 times daily"),
        HomeExercise(name: "Segmental Breathing",
                     detail: "Focus on directing air to different parts of your lungs by placing hands on specific areas of your chest or sides while breathing deeply.",
                     frequency: "Recommended frequency: 5 minutes, 2-3 times daily")
    ]
    private let endurance: [HomeExercise] = [
        HomeExercise(name: "Walking",
                     detail: "Start with short distances and gradually increase. Use pursed-lip breathing while walking. Stop and rest if you become too breathless.",
                     frequency: "Recommended frequency: Start with 5-10 minutes daily, gradually increase to 20-30 minutes"),
        HomeExercise(name: "Stationary Cycling",
                     detail: "Adjust resistance to a comfortable level. Maintain good posture and use pursed-lip breathing.",
                     frequency: "Recommended frequency: Start with 5-10 minutes daily, gradually increase to 15-20 minutes"),
        HomeExercise(name: "Swimming/Water Exercises",
                     detail: "The buoyancy of water supports your body, making movement easier. The humidity can also help your breathing.",
                     frequency: "Recommended frequency: 20-30 minutes, 2-3 times weekly")
    ]
    private let strength: [HomeExercise] = [
        HomeExercise(name: "Upper Body Strengthening",
                     detail: "Use light weights or resistance bands for arm raises, bicep curls, and shoulder presses. Focus on proper breathing throughout.",
                     frequency: "Recommended frequency: 8-12 repetitions, 2-3 sets, 2-3 times weekly"),
        HomeExercise(name: "Lower Body Strengthening",
                     detail: "Perform chair stands, leg extensions, and calf raises to strengthen legs. These help with daily activities like standing and walking.",
                     frequency: "Recommended frequency: 8-12 repetitions, 2-3 sets, 2-3 times weekly"),
        HomeExercise(name: "Core Strengthening",
                     detail: "Seated abdominal contractions and gentle back extensions help improve posture and breathing mechanics.",
                     frequency: "Recommended frequency: 8-12 repetitions, 2-3 sets, 2-3 times weekly")
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            ResourcesTitle(text: "Pulmonary Rehabilitation")
            Text("Pulmonary rehabilitation is a comprehensive program that combines exercise, education, and support to help people with COPD breathe better, get stronger, and improve their quality of life. Always consult with your healthcare provider before starting any exercise program.")
                .font(.system(size: 16))
                .foregroundColor(Color(hex: "4b5563"))

            ResourcesHeading(text: "Benefits of Pulmonary Rehabilitation")
            GuidelineCard(title: "Improved Exercise Capacity", detail: "Pulmonary rehabilitation can help you walk further and perform daily activities with less breathlessness.")
            GuidelineCard(title: "Better Quality of Life", detail: "Many people report feeling better overall and having more energy for the activities they enjoy.")
            GuidelineCard(title: "Reduced Hospital Admissions", detail: "Regular participation in pulmonary rehabilitation can reduce your risk of COPD exacerbations requiring hospitalization.")
            GuidelineCard(title: "Increased Strength", detail: "Strengthening exercises help counter muscle loss that often occurs with COPD and improve your ability to perform daily tasks.")
            GuidelineCard(title: "Better Breathing Control", detail: "Learning proper breathing techniques helps you manage breathlessness during activities and reduce anxiety.")
            GuidelineCard(title: "Social Support", detail: "Meeting others with similar conditions provides emotional support and motivation to maintain your exercise program.")

            ResourcesHeading(text: "Finding a Pulmonary Rehabilitation Program")
            VStack(alignment: .leading, spacing: 6) {
                Text("Pulmonary rehabilitation programs are typically offered at hospitals, outpatient clinics, or community centers. To find a program near you:")
                Text("• Ask your pulmonologist or primary care physician for a referral")
                Text("• Contact your local hospital or lung health association")
                Text("• Check with your insurance provider for covered programs")
                Text("• Visit the American Lung Association website for program directories")
            }
            .font(.system(size: 16))
            .foregroundColor(Color(hex: "4b5563"))

            NavigationLink(destination: ProgramsNearMeView()) {
                Text("Find Programs Near Me")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color(hex: "2563eb"))
                    .cornerRadius(8)
            }

            ResourcesHeading(text: "Home Exercise Program")
            Text("While a supervised pulmonary rehabilitation program is ideal, these exercises can be performed at home to complement your program or when a formal program isn't available.")
                .font(.system(size: 16))
                .foregroundColor(Color(hex: "4b5563"))

            exerciseGroup(title: "Breathing Exercises", caption: "Techniques to improve breathing efficiency and control", items: breathing)
            exerciseGroup(title: "Endurance Training", caption: "Activities to improve cardiovascular fitness and stamina", items: endurance)
            exerciseGroup(title: "Strength Training", caption: "Exercises to strengthen respiratory and peripheral muscles", items: strength)

            Text("Important: Always start slowly and progress gradually. Stop any exercise that causes severe shortness of breath, chest pain, or dizziness. Keep your rescue inhaler nearby during exercise.")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(Color(hex: "cc0000"))
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(hex: "f3f4f6"))
                .cornerRadius(8)

            ResourcesHeading(text: "Track Your Progress")
            Text("Keeping track of your exercise sessions helps you see your progress and stay motivated. Consider tracking:\n\n• Exercise duration and frequency\n• Distance walked or steps taken\n• Breathlessness levels before, during, and after exercise\n• How you feel overall after each session")
                .font(.system(size: 16))
                .foregroundColor(Color(hex: "4b5563"))

            ExerciseJournalView()

            Image("pulmonary_rehab_exercises")
                .resizable()
                .scaledToFit()
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
        }
    }

    private func exerciseGroup(title: String, caption: String, items: [HomeExercise]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            ResourcesSubheading(text: title)
            Text(caption)
                .font(.system(size: 14))
                .foregroundColor(Color(hex: "6b7280"))
            ForEach(items, id: \.name) { item in
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.name)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(Color(hex: "1f2937"))
                    Text(item.detail)
                        .font(.system(size: 14))
                        .foregroundColor(Color(hex: "4b5563"))
                    Text(item.frequency)
                        .font(.system(size: 14))
                        .italic()
                        .foregroundColor(Color(hex: "6b7280"))
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(hex: "f8fafc"))
                .cornerRadius(8)
            }
        }
    }
}
```

- [ ] **Step 2: Create `ExerciseJournalView.swift`**

```swift
//
//  ExerciseJournalView.swift
//  COPDFuel
//
//  Exercise Journal inside Pulm. Rehab (ResourcesFragment.kt:1088-1509).
//  Storage: ExerciseJournalStore (Android key + schema).
//

import SwiftUI

struct ExerciseJournalView: View {
    @State private var journal: [ExerciseJournalEntry] = []
    @State private var isEditing = false
    @State private var entryToDelete: ExerciseJournalEntry?

    @State private var formDay = Date()
    @State private var formTime = Date()
    @State private var formWarmUp = ""
    @State private var formExercise = ""
    @State private var formSets = ""
    @State private var formReps = ""
    @State private var formWeight = ""
    @State private var formWeightUnit = "lbs"
    @State private var formActivity = ""

    private static let dayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "MMM d, yyyy"
        return f
    }()

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ResourcesHeading(text: "Exercise Journal")
            Text("Daily log of warm-ups, exercises, sets, reps, weights, and activity. All fields are optional — fill in only what applies.")
                .font(.system(size: 14))
                .foregroundColor(Color(hex: "6b7280"))

            if isEditing { form } else { list }
        }
        .onAppear { journal = ExerciseJournalStore.load() }
        .alert("Delete entry?", isPresented: Binding(
            get: { entryToDelete != nil },
            set: { if !$0 { entryToDelete = nil } }
        ), presenting: entryToDelete) { entry in
            Button("Delete", role: .destructive) {
                journal.removeAll { $0.id == entry.id }
                ExerciseJournalStore.save(journal)
            }
            Button("Cancel", role: .cancel) {}
        } message: { _ in
            Text("This journal entry will be permanently removed.")
        }
    }

    // MARK: Form (:1192-1386)

    private var form: some View {
        VStack(alignment: .leading, spacing: 10) {
            DatePicker("Day", selection: $formDay, displayedComponents: .date)
                .font(.system(size: 14, weight: .bold))
            DatePicker("Time", selection: $formTime, displayedComponents: .hourAndMinute)
                .font(.system(size: 14, weight: .bold))
            JournalField(label: "Warm Up", hint: "e.g., 5 min walk, stretching", text: $formWarmUp)
            JournalField(label: "Exercise", hint: "e.g., Bench press, Squats", text: $formExercise)
            JournalField(label: "Sets", hint: "e.g., 3", text: $formSets, keyboard: .numberPad)
            JournalField(label: "Reps", hint: "e.g., 10", text: $formReps, keyboard: .numberPad)
            HStack(alignment: .bottom, spacing: 12) {
                JournalField(label: "Weight", hint: "e.g., 25", text: $formWeight, keyboard: .decimalPad)
                Picker("", selection: $formWeightUnit) {
                    Text("lbs").tag("lbs")
                    Text("kg").tag("kg")
                }
                .pickerStyle(.segmented)
                .frame(width: 110)
                .padding(.bottom, 2)
            }
            JournalField(label: "Activity", hint: "e.g., Strength training, Cardio", text: $formActivity)

            ResourcesFilledButton(title: "Save Entry") {
                let cal = Calendar.current
                let entry = ExerciseJournalEntry(
                    savedAt: ExerciseJournalEntry.millis(Date()),
                    dayMillis: ExerciseJournalEntry.millis(cal.startOfDay(for: formDay)),
                    timeText: ExerciseJournalEntry.timeText(for: formTime),
                    warmUp: formWarmUp.trimmingCharacters(in: .whitespacesAndNewlines),
                    exercise: formExercise.trimmingCharacters(in: .whitespacesAndNewlines),
                    sets: Int(formSets.trimmingCharacters(in: .whitespaces)) ?? 0,
                    reps: Int(formReps.trimmingCharacters(in: .whitespaces)) ?? 0,
                    weight: Double(formWeight.trimmingCharacters(in: .whitespaces)) ?? 0.0,
                    weightUnit: formWeightUnit,
                    activity: formActivity.trimmingCharacters(in: .whitespacesAndNewlines))
                journal.append(entry)
                ExerciseJournalStore.save(journal)
                ToastCenter.shared.show("Entry saved")
                resetForm()
                isEditing = false
            }
            ResourcesFilledButton(title: "Cancel", background: Color(hex: "6b7280")) {
                resetForm()
                isEditing = false
            }
        }
    }

    private func resetForm() {
        formDay = Date(); formTime = Date()
        formWarmUp = ""; formExercise = ""; formSets = ""; formReps = ""
        formWeight = ""; formWeightUnit = "lbs"; formActivity = ""
    }

    // MARK: List (:1388-1509)

    private var list: some View {
        let cutoff = ExerciseJournalEntry.millis(
            Calendar.current.date(byAdding: .day, value: -7, to: Calendar.current.startOfDay(for: Date())) ?? Date())
        let recent = journal.filter { $0.dayMillis >= cutoff }.sorted { $0.savedAt > $1.savedAt }

        return VStack(alignment: .leading, spacing: 10) {
            ResourcesFilledButton(title: "+ Start New Entry") {
                resetForm()
                isEditing = true
            }
            Text("Showing entries from the last 7 days. View all in Tracking → Month.")
                .font(.system(size: 12))
                .foregroundColor(Color(hex: "6b7280"))

            if recent.isEmpty {
                Text(journal.isEmpty ? "No journal entries yet." : "No journal entries in the last 7 days.")
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "6b7280"))
            } else {
                ForEach(recent) { entry in
                    ExerciseJournalEntryCard(entry: entry, dayText: Self.dayFormatter.string(from: entry.day)) {
                        entryToDelete = entry
                    }
                }
            }
        }
    }
}

private struct JournalField: View {
    let label: String
    let hint: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.system(size: 14, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
            TextField(hint, text: $text)
                .keyboardType(keyboard)
                .padding(10)
                .background(Color(hex: "f8fafc"))
                .cornerRadius(6)
                .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color(hex: "e5e7eb"), lineWidth: 1))
        }
    }
}

/// Card: "{MMM d, yyyy} • {timeText}" + Delete; exercise 18sp bold; stats
/// "{n} sets • {n} reps • {w} {unit}"; Activity / Warm Up; or
/// "(no details recorded)" (:1447-1503).
struct ExerciseJournalEntryCard: View {
    let entry: ExerciseJournalEntry
    let dayText: String
    let onDelete: () -> Void

    private var statsLine: String {
        var parts: [String] = []
        if entry.sets > 0 { parts.append("\(entry.sets) sets") }
        if entry.reps > 0 { parts.append("\(entry.reps) reps") }
        if entry.weight > 0 {
            let w = entry.weight.truncatingRemainder(dividingBy: 1) == 0
                ? "\(Int(entry.weight))" : String(format: "%g", entry.weight)
            parts.append("\(w) \(entry.weightUnit)")
        }
        return parts.joined(separator: " • ")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("\(dayText) • \(entry.timeText)")
                    .font(.system(size: 12))
                    .foregroundColor(Color(hex: "6b7280"))
                Spacer()
                Button("Delete", action: onDelete)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.red)
            }
            if !entry.exercise.isEmpty {
                Text(entry.exercise).font(.system(size: 18, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
            }
            if !statsLine.isEmpty {
                Text(statsLine).font(.system(size: 14)).foregroundColor(Color(hex: "1f2937"))
            }
            if !entry.activity.isEmpty {
                Text("Activity: \(entry.activity)").font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
            }
            if !entry.warmUp.isEmpty {
                Text("Warm Up: \(entry.warmUp)").font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
            }
            if entry.exercise.isEmpty && statsLine.isEmpty && entry.activity.isEmpty && entry.warmUp.isEmpty {
                Text("(no details recorded)").font(.system(size: 14)).foregroundColor(Color(hex: "6b7280"))
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "f8fafc"))
        .cornerRadius(8)
    }
}
```

---

### Task 6: Resp. Care / Medication Guide (audit §4)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Resources/MedicationTypes.swift` (data; transcribed from `MedicationTypes.kt:9-352`)
- Create: `COPDFuel/COPDFuel/Views/Resources/MedicationGuideView.swift`

**Interfaces:**
- Produces: `MedicationTypeInfo` (`id`, `gridTitle`, `title`, `symbol`, `sections() -> [(heading: String, body: String)]`), `MedicationTypes.all`, `MedicationTypes.disclaimer`.

- [ ] **Step 1: Create `MedicationTypes.swift`** — every string below is the Kotlin text with its `+` concatenations joined; do not reflow or edit wording.

```swift
//
//  MedicationTypes.swift
//  COPDFuel
//
//  Patient-education guides for the ten tiles in Resources › Resp. Care ›
//  Medication Types. Transcribed verbatim from Android MedicationTypes.kt.
//  General information only; wording must be reviewed by a clinician.
//

import Foundation

struct MedicationTypeInfo: Identifiable {
    let id: String
    let gridTitle: String
    let title: String
    let symbol: String
    let examples: [String]
    let whatItIs: String
    let whatItsFor: String
    let howItWorks: String
    let commonForms: String
    let howToUse: String
    let commonSideEffects: String
    let warnings: String
    let interactions: String

    /// Heading and body of each part of the guide, in reading order.
    func sections() -> [(heading: String, body: String)] {
        [
            ("Examples", examples.map { "• " + $0 }.joined(separator: "\n")),
            ("What it is", whatItIs),
            ("What it's for", whatItsFor),
            ("How it works", howItWorks),
            ("Common forms", commonForms),
            ("How to use", howToUse),
            ("Common side effects", commonSideEffects),
            ("Warnings", warnings),
            ("Interactions", interactions),
            ("Important", MedicationTypes.disclaimer)
        ]
    }
}

enum MedicationTypes {
    static let disclaimer = "This guide is general education, not medical advice. It does not list every use, side effect, warning or interaction, and brand names are examples only. Always follow your own prescription and the leaflet that comes with your medicine. Ask your doctor or pharmacist before starting, stopping or changing any medicine. If you have severe trouble breathing, chest pain, or swelling of the face, lips or throat, call 911."

    static let all: [MedicationTypeInfo] = [
        MedicationTypeInfo(
            id: "bronchodilators", gridTitle: "Bronchodilator", title: "Bronchodilators", symbol: "wind",
            examples: [
                "Short-acting (SABAs): Albuterol, Levalbuterol",
                "Short-acting (SAMAs): Ipratropium",
                "Long-acting (LABAs): Salmeterol, Formoterol, Indacaterol",
                "Long-acting (LAMAs): Tiotropium, Aclidinium, Umeclidinium"
            ],
            whatItIs: "Inhaled medicines that relax the muscles wrapped around your airways. They are the main treatment for COPD symptoms. Short-acting types work within minutes; long-acting types last 12 to 24 hours.",
            whatItsFor: "Short-acting (rescue) inhalers relieve sudden shortness of breath, wheezing and chest tightness. Long-acting (maintenance) inhalers are taken every day to keep symptoms under control and lower the chance of flare-ups.",
            howItWorks: "Beta-agonists (SABAs and LABAs) switch on receptors that tell airway muscle to relax. Anticholinergics (SAMAs and LAMAs) block the nerve signals that tell it to tighten. Either way the airways widen and air moves more easily.",
            commonForms: "Metered-dose inhalers (often used with a spacer), dry powder inhalers, soft mist inhalers, and liquid for a nebulizer.",
            howToUse: "Use your rescue inhaler when you need it, as prescribed, and keep it with you. Take long-acting inhalers at the same time every day, even when you feel well; they are not for sudden symptoms. Ask your pharmacist or nurse to check your inhaler technique.",
            commonSideEffects: "Beta-agonists: shakiness, a fast or pounding heartbeat, nervousness, headache, muscle cramps. Anticholinergics: dry mouth, cough, constipation, trouble passing urine.",
            warnings: "Tell your clinician if you need your rescue inhaler more often than usual or it is not helping; this can be an early sign of a flare-up. Get urgent help for chest pain, a very fast or irregular heartbeat, breathing that gets worse right after a dose, eye pain or blurred vision, or being unable to pass urine.",
            interactions: "Beta-blockers (including some glaucoma eye drops) can reduce the effect. Some antidepressants (MAOIs, tricyclics) and stimulants can add to heart effects. Water pills (diuretics) may lower potassium further. Other anticholinergic medicines can add to dry mouth and urinary problems."
        ),
        MedicationTypeInfo(
            id: "ics", gridTitle: "Inhaled Corticosteroids", title: "Inhaled Corticosteroids", symbol: "humidity.fill",
            examples: ["Fluticasone", "Budesonide", "Beclomethasone", "Mometasone"],
            whatItIs: "Steroid (anti-inflammatory) medicines that are breathed straight into the lungs. They are different from the anabolic steroids misused in sport.",
            whatItsFor: "Lowering the number of flare-ups in people who have them often, who have a raised eosinophil count, or who also have asthma. In COPD they are prescribed together with a long-acting bronchodilator, not on their own, and they are not for quick relief.",
            howItWorks: "They calm inflammation and swelling in the lining of the airways. The benefit builds up over days to weeks of regular use.",
            commonForms: "Metered-dose inhalers, dry powder inhalers, and budesonide liquid for a nebulizer. In COPD they most often come inside a combination inhaler.",
            howToUse: "Take every day as prescribed, even when you feel well. Rinse your mouth, gargle and spit after every dose. Use a spacer with a metered-dose inhaler if you have one. Do not stop suddenly without talking to your clinician.",
            commonSideEffects: "Hoarse voice, sore throat, cough, and oral thrush (white patches in the mouth). Rinsing after each dose makes these less likely.",
            warnings: "Inhaled steroids raise the risk of pneumonia in people with COPD: report fever, more or discoloured mucus, or worse breathlessness. High doses over a long time can thin the bones, raise the risk of cataracts and glaucoma, and cause easy bruising. Keep up with eye and bone checks if your clinician advises them.",
            interactions: "Some medicines raise steroid levels in the body, including ritonavir and cobicistat (HIV medicines), ketoconazole and itraconazole (antifungals) and clarithromycin. Tell your clinician and pharmacist if you take any of these."
        ),
        MedicationTypeInfo(
            id: "combination", gridTitle: "Combination Inhalers", title: "Combination Inhalers", symbol: "arrow.triangle.2.circlepath",
            examples: [
                "LABA + LAMA: Anoro Ellipta, Stiolto Respimat",
                "LABA + ICS: Advair, Symbicort, Breo Ellipta",
                "Triple Therapy: Trelegy Ellipta, Breztri Aerosphere"
            ],
            whatItIs: "One inhaler that holds two or three maintenance medicines: two long-acting bronchodilators, a bronchodilator with an inhaled steroid, or all three.",
            whatItsFor: "Daily, long-term control when a single medicine is not enough, and to reduce flare-ups. Having one device instead of several makes the routine simpler.",
            howItWorks: "A LABA and a LAMA relax the airway muscles in two different ways, so together they open the airways more than either alone. An inhaled steroid adds protection against flare-ups by reducing airway inflammation.",
            commonForms: "Dry powder inhalers (Ellipta, Diskus), metered-dose inhalers (Symbicort, Breztri, Bevespi) and soft mist inhalers (Respimat).",
            howToUse: "Take at the same time every day, once or twice daily depending on the product. They are not for sudden symptoms, so keep your rescue inhaler with you. Rinse your mouth and spit if yours contains a steroid. Do not take extra doses.",
            commonSideEffects: "The side effects of the medicines inside: shakiness, fast heartbeat, headache, dry mouth, hoarse voice, throat irritation and oral thrush.",
            warnings: "Do not use a second inhaler containing the same type of medicine (another LABA or LAMA) unless your clinician tells you to. Products with a steroid carry the pneumonia risk described under Inhaled Corticosteroids. Get help for chest pain, an irregular heartbeat, eye pain, trouble passing urine, or breathing that worsens right after a dose.",
            interactions: "As for the individual medicines: beta-blockers, some antidepressants (MAOIs, tricyclics), water pills, other anticholinergic medicines, and, for steroid-containing inhalers, ritonavir, cobicistat, ketoconazole, itraconazole and clarithromycin."
        ),
        MedicationTypeInfo(
            id: "pde4", gridTitle: "Phosphodiesterase-4 Inhibitors", title: "Phosphodiesterase-4 (PDE4) Inhibitors", symbol: "pills.fill",
            examples: [
                "Roflumilast (Daliresp) - a once-daily tablet",
                "Ensifentrine (Ohtuvayre) - a nebulized PDE3 and PDE4 inhibitor"
            ],
            whatItIs: "Non-steroid medicines that reduce inflammation in the lungs by blocking an enzyme called phosphodiesterase-4. They are add-on treatments, not rescue medicines.",
            whatItsFor: "Roflumilast is used for severe COPD with chronic bronchitis (daily cough and mucus) and a history of flare-ups, to help reduce further flare-ups. Ensifentrine is a maintenance treatment for COPD symptoms.",
            howItWorks: "Blocking PDE4 inside inflammatory cells lowers the release of substances that drive swelling and mucus in the airways. Ensifentrine also blocks PDE3, which helps relax the airways.",
            commonForms: "Roflumilast is a tablet. Ensifentrine is a liquid used in a standard jet nebulizer.",
            howToUse: "Take roflumilast once a day, with or without food; some people start on a lower dose for the first weeks. Use ensifentrine as prescribed through your nebulizer. Keep taking your other COPD inhalers unless told otherwise.",
            commonSideEffects: "Roflumilast: diarrhea, nausea, reduced appetite, weight loss, headache, trouble sleeping, dizziness and back pain. Stomach effects often ease after the first weeks.",
            warnings: "Tell your clinician straight away about new or worse anxiety, depression, trouble sleeping, or thoughts of self-harm. Weigh yourself regularly and report unplanned weight loss. Roflumilast is not suitable for people with moderate or severe liver disease.",
            interactions: "Rifampin, phenobarbital, carbamazepine and phenytoin can make roflumilast less effective. Erythromycin, ketoconazole, fluvoxamine, cimetidine and some birth control pills can raise its level and side effects."
        ),
        MedicationTypeInfo(
            id: "antibiotics", gridTitle: "Antibiotics", title: "Antibiotics", symbol: "cross.case.fill",
            examples: [
                "Azithromycin (Z-pack)",
                "Amoxicillin-clavulanate (Augmentin)",
                "Doxycycline",
                "Levofloxacin"
            ],
            whatItIs: "Medicines that treat infections caused by bacteria. They do not work against viruses such as colds or the flu.",
            whatItsFor: "Treating bacterial chest infections and flare-ups, often signalled by more mucus, a change in its colour, and more breathlessness. Some people with frequent flare-ups are prescribed long-term azithromycin to help prevent them.",
            howItWorks: "They kill bacteria or stop them multiplying, giving your body the chance to clear the infection. Azithromycin also has a mild anti-inflammatory effect in the airways.",
            commonForms: "Tablets, capsules and liquids taken by mouth. In hospital they may be given through a vein.",
            howToUse: "Take exactly as prescribed and finish the course unless your clinician tells you to stop. Space doses evenly. Do not save leftovers or share them. Take doxycycline with a full glass of water while upright, and keep it 2 hours apart from dairy, antacids and iron.",
            commonSideEffects: "Nausea, diarrhea, stomach upset, rash and yeast infections. Doxycycline can make your skin burn more easily in the sun.",
            warnings: "Hives, swelling of the face or throat, or trouble breathing is an allergic emergency: call 911. Report severe or bloody diarrhea, even weeks later. Azithromycin and levofloxacin can affect heart rhythm. Stop levofloxacin and call your clinician for tendon pain, numbness or tingling, or sudden severe chest, back or belly pain.",
            interactions: "Many antibiotics increase the effect of warfarin. Antacids, calcium, iron and magnesium block absorption of doxycycline and levofloxacin. Some should not be combined with other medicines that affect heart rhythm. Ciprofloxacin, clarithromycin and erythromycin raise theophylline levels."
        ),
        MedicationTypeInfo(
            id: "systemic", gridTitle: "Systemic Corticosteroids", title: "Systemic Corticosteroids", symbol: "pill.fill",
            examples: [
                "Prednisone",
                "Methylprednisolone",
                "Dexamethasone",
                "Used short-term during exacerbations"
            ],
            whatItIs: "Steroid medicines taken as tablets or liquid, or given by injection, so they act throughout the whole body rather than only in the lungs.",
            whatItsFor: "Treating COPD flare-ups. A short course, usually around five days, can speed recovery and improve breathing. They are generally not used long term for COPD because of side effects.",
            howItWorks: "They strongly reduce inflammation everywhere in the body, including the swollen airways that make a flare-up so hard to breathe through.",
            commonForms: "Tablets and liquids by mouth; injections or a drip in hospital.",
            howToUse: "Take in the morning with food, exactly as directed, and follow your action plan. If you have taken steroids for more than a few weeks, do not stop suddenly; your clinician will reduce the dose gradually.",
            commonSideEffects: "Bigger appetite, trouble sleeping, mood changes, stomach upset, fluid retention and higher blood sugar.",
            warnings: "Repeated or long courses can cause thin bones, muscle weakness, cataracts, thin skin, high blood pressure, diabetes and a higher risk of infection. Call your clinician for black or bloody stools, severe mood changes, signs of infection or changes in vision. If you have diabetes, check your blood sugar more often.",
            interactions: "Anti-inflammatory painkillers such as ibuprofen and naproxen raise the risk of stomach bleeding. Diabetes medicines may need adjusting. They can affect warfarin, lower potassium further with water pills, and live vaccines may need to be delayed."
        ),
        MedicationTypeInfo(
            id: "methylxanthines", gridTitle: "Methylxanthine", title: "Methylxanthines", symbol: "capsule.fill",
            examples: [
                "Theophylline (Theo-24, Elixophyllin)",
                "Older class of bronchodilators",
                "Used less frequently due to side effects"
            ],
            whatItIs: "An older type of bronchodilator taken by mouth. It is chemically related to caffeine.",
            whatItsFor: "An add-on for people whose symptoms are not controlled by inhalers, or who cannot use them. It is prescribed much less often today.",
            howItWorks: "It relaxes the airway muscles, may strengthen the breathing muscles, and has a mild anti-inflammatory effect.",
            commonForms: "Extended-release tablets and capsules, and liquid. A related medicine, aminophylline, can be given through a vein in hospital.",
            howToUse: "Take at the same times each day and in the same way with respect to food. Swallow extended-release forms whole. Have blood tests when asked, and do not switch brands without advice. Limit coffee, tea, cola and energy drinks.",
            commonSideEffects: "Nausea, stomach upset, heartburn, headache, trouble sleeping, feeling jittery, and passing more urine.",
            warnings: "The helpful dose is close to the harmful dose. Get urgent help for repeated vomiting, a fast or irregular heartbeat, confusion or a seizure. Tell your clinician if you stop or start smoking, have a fever, or develop heart or liver problems, because these change the level in your blood.",
            interactions: "Many medicines change theophylline levels. Ciprofloxacin, erythromycin, clarithromycin, cimetidine, fluvoxamine and allopurinol raise it. Rifampin, carbamazepine, phenytoin, phenobarbital, St John's wort and smoking lower it. Caffeine adds to side effects."
        ),
        MedicationTypeInfo(
            id: "mucolytics", gridTitle: "Mucolytics/Expectorants", title: "Mucolytics/Expectorants", symbol: "drop.fill",
            examples: [
                "N-acetylcysteine (NAC)",
                "Carbocysteine",
                "Guaifenesin",
                "Help thin and loosen mucus"
            ],
            whatItIs: "Medicines that change mucus. Mucolytics make it thinner and less sticky; expectorants add water to it so it is easier to cough up.",
            whatItsFor: "Thick, sticky mucus that is hard to clear, especially with chronic bronchitis. Taken regularly, some mucolytics may slightly lower the number of flare-ups in certain people.",
            howItWorks: "Mucolytics break the chemical bonds that make mucus thick. Expectorants increase the fluid in airway secretions so your airways' natural clearing action and coughing can move it.",
            commonForms: "Tablets, capsules, effervescent tablets and liquids. Acetylcysteine also comes as a solution for a nebulizer.",
            howToUse: "Take as directed and drink plenty of fluids unless you have been told to limit them. They work best alongside airway clearance techniques or devices. With nebulized acetylcysteine you may be told to use a bronchodilator first.",
            commonSideEffects: "Nausea, stomach upset, vomiting and diarrhea. Nebulized acetylcysteine can cause cough, throat irritation, a runny nose and an unpleasant smell.",
            warnings: "Nebulized acetylcysteine can tighten the airways in some people; stop and use your rescue inhaler if breathing worsens. Tell your clinician if you have had a stomach ulcer. Many cough and cold products mix guaifenesin with decongestants that can raise blood pressure, so read labels.",
            interactions: "Acetylcysteine taken with nitroglycerin can cause low blood pressure and headache. Cough suppressants can work against these medicines by stopping you clearing mucus. Check combination cold products with your pharmacist."
        ),
        MedicationTypeInfo(
            id: "biologics", gridTitle: "Biologics", title: "Biologics", symbol: "syringe.fill",
            examples: [
                "Dupilumab (Dupixent) (taken once every 2 weeks)",
                "Mepolizumab (Nucala) (taken once every 4 weeks) - for eosinophilic COPD",
                "Newer targeted therapies"
            ],
            whatItIs: "Injected medicines made from antibodies that block one specific part of the immune system. They are a newer option for a particular type of COPD.",
            whatItsFor: "An add-on for adults whose COPD is not controlled by inhaled triple therapy and who have a raised eosinophil count (a type of white blood cell). Your clinician uses blood tests to see whether one may help.",
            howItWorks: "Dupilumab blocks the signals of two messengers, interleukin-4 and interleukin-13. Mepolizumab blocks interleukin-5, which eosinophils depend on. Both reduce the kind of inflammation that drives flare-ups in this type of COPD.",
            commonForms: "A prefilled pen or syringe injected under the skin, every 2 or 4 weeks depending on the medicine. It can be given at a clinic, or at home after training.",
            howToUse: "Store in the refrigerator and follow the leaflet for warming and injecting. Change the injection site each time. Keep taking your inhalers, and do not stop steroid medicines suddenly. They do not treat sudden breathing problems.",
            commonSideEffects: "Redness, swelling or pain where injected, headache, back or joint pain, cold-like symptoms, and with dupilumab, red or irritated eyes.",
            warnings: "Serious allergic reactions can happen: get emergency help for hives, swelling of the face or throat, faintness or trouble breathing. Report new or worsening eye problems. Existing parasitic (worm) infections should be treated first. Shingles has been reported with mepolizumab.",
            interactions: "No major interactions with other medicines are known. Live vaccines should be avoided while taking dupilumab. Tell every clinician and pharmacist that you are on a biologic."
        ),
        MedicationTypeInfo(
            id: "nebulizer", gridTitle: "Nebulizer Medications", title: "Nebulizer Medications", symbol: "bubbles.and.sparkles.fill",
            examples: [
                "Albuterol nebulizer solution",
                "Ipratropium nebulizer solution",
                "Budesonide (Pulmicort Respules)",
                "Combination: Albuterol + Ipratropium (DuoNeb)"
            ],
            whatItIs: "Liquid medicines that a nebulizer machine turns into a fine mist, breathed in through a mouthpiece or mask over several minutes.",
            whatItsFor: "People who find inhalers hard to use because of weak breath, arthritis, poor coordination or memory problems, and for treatment during a flare-up when taking a deep breath is difficult.",
            howItWorks: "The machine breaks the liquid into tiny droplets that reach the lungs with normal, relaxed breathing. The medicine then works just as its inhaler version does.",
            commonForms: "Single-dose plastic vials used with a jet, mesh or ultrasonic nebulizer, with a mouthpiece or a face mask.",
            howToUse: "Wash your hands, sit upright, and breathe normally until the mist stops, usually 5 to 15 minutes. Rinse your mouth after a steroid. Wash and air-dry the parts after each use, disinfect them regularly, and replace parts and filters as the maker advises. Only mix medicines if your pharmacist says it is safe.",
            commonSideEffects: "The same as the medicine in inhaler form: shakiness and a fast heartbeat with albuterol, dry mouth with ipratropium, hoarse voice and thrush with budesonide.",
            warnings: "A dirty nebulizer can cause lung infections, so clean it as instructed. Mist from ipratropium leaking into the eyes can cause blurred vision or trigger glaucoma; use a mouthpiece or a well-fitting mask. Tell your clinician if you need treatments more often than usual.",
            interactions: "The same as for the inhaled forms. Do not double up with an inhaler containing the same type of medicine unless told to, and check with your pharmacist before mixing two solutions in one cup."
        )
    ]
}
```

- [ ] **Step 2: Create `MedicationGuideView.swift`**

```swift
//
//  MedicationGuideView.swift
//  COPDFuel
//
//  Resources › Resp. Care. Mirrors ResourcesFragment.buildMedicationContent
//  (:1537-1969): description, featured inhalers, 10-tile Medication Types
//  grid with a pop-up guide, technique videos, respiratory devices, note.
//

import SwiftUI

struct MedicationGuideView: View {
    @State private var selectedInfo: MedicationTypeInfo?

    private struct Device { let name: String; let classification: String; let purpose: String }
    private struct DeviceCategory { let title: String; let caption: String; let devices: [Device] }

    private let deviceCategories: [DeviceCategory] = [
        DeviceCategory(
            title: "1. Airway Clearance Devices",
            caption: "Devices used to loosen, mobilize, and help remove mucus from the lungs.",
            devices: [
                Device(name: "Aerobika",
                       classification: "Oscillating Positive Expiratory Pressure (OPEP) device",
                       purpose: "Uses pressure and vibrations when you breathe out to loosen and clear sticky mucus from the lungs"),
                Device(name: "Flutter valve",
                       classification: "Oscillating Positive Expiratory Pressure (OPEP) device",
                       purpose: "When you exhale into the flutter valve, your breath lifts and drops a steel ball inside, creating vibrations that shake the mucus loose from one's airways"),
                Device(name: "High-frequency chest wall oscillation vest",
                       classification: "High-frequency chest wall oscillation (HFCWO) device",
                       purpose: "High-Frequency Chest Wall Oscillation (HFCWO) vest therapy uses rapid air pulses to squeeze and vibrate your chest, which thins and shakes stubborn mucus loose from your airway walls so you can cough it out.")
            ]),
        DeviceCategory(
            title: "2. Oxygen Therapy Devices",
            caption: "Devices used to improve oxygen levels in patients with hypoxemia.",
            devices: [
                Device(name: "Oxygen concentrator",
                       classification: "Oxygen delivery device",
                       purpose: "Provides supplemental oxygen to help support a patient's oxygen levels")
            ]),
        DeviceCategory(
            title: "3. BiPAP Devices",
            caption: "Devices used to provide pressure-supported breathing assistance.",
            devices: [
                Device(name: "BiPAP",
                       classification: "Bilevel Positive Airway Pressure device",
                       purpose: "Delivers two levels of air pressure, with higher pressure during inhalation called IPAP and lower pressure during exhalation called EPAP, to keep the airways open, improve ventilation, and reduce the work of breathing for the lungs.")
            ]),
        DeviceCategory(
            title: "4. Noninvasive Ventilation (NIV)",
            caption: "Devices used to deliver breathing support through a mask rather than an invasive airway.",
            devices: [
                Device(name: "NIV",
                       classification: "Noninvasive positive pressure ventilation (NIPPV) device",
                       purpose: "Helps support breathing and improves gas exchange without intubation. Used to prevent unintended breath stacking.")
            ])
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            ResourcesTitle(text: "COPD Medication Guide")
            ResourcesInfoCard(text: "This guide provides general information about COPD medications. Your doctor will prescribe medications based on your specific needs. Always follow your healthcare provider's instructions about your medications.")

            ResourcesHeading(text: "Featured COPD Inhalers")
            HStack(spacing: 12) {
                ResourcesLinkButton(title: "Symbicort Guide", url: "https://www.symbicort.com/")
                ResourcesLinkButton(title: "Breztri Guide", url: "https://www.breztri.com/")
            }

            ResourcesHeading(text: "Medication Types", italic: true)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3), spacing: 8) {
                ForEach(MedicationTypes.all) { info in
                    Button(action: { selectedInfo = info }) {
                        VStack(spacing: 8) {
                            Image(systemName: info.symbol)
                                .font(.system(size: 30))
                                .foregroundColor(Color(hex: "2563eb"))
                                .frame(height: 36)
                            Text(info.gridTitle)
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(Color(hex: "1f2937"))
                                .multilineTextAlignment(.center)
                                .lineLimit(2)
                                .frame(height: 30)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .padding(.horizontal, 4)
                        .background(Color.white)
                        .cornerRadius(10)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color(hex: "e5e7eb"), lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
            }

            ResourcesLinkButton(
                title: "Watch Inhaler Technique Videos",
                url: "https://www.copdfoundation.org/Learn-More/Educational-Materials-Resources/Educational-Video-Series.aspx",
                background: Color(hex: "10b981"))

            Rectangle().fill(Color(hex: "e5e7eb")).frame(height: 1)

            ResourcesHeading(text: "Respiratory Support and Airway Clearance Devices")
            ResourcesInfoCard(text: "Common devices used to support breathing, deliver oxygen, and clear mucus from the airways. Talk to your healthcare provider about which devices may be appropriate for your care.")

            ForEach(deviceCategories, id: \.title) { category in
                VStack(alignment: .leading, spacing: 10) {
                    ResourcesSubheading(text: category.title)
                    Text(category.caption)
                        .font(.system(size: 14))
                        .foregroundColor(Color(hex: "6b7280"))
                    ForEach(category.devices, id: \.name) { device in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(device.name)
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(Color(hex: "1f2937"))
                            Text("Classification: \(device.classification)")
                                .font(.system(size: 14))
                                .foregroundColor(Color(hex: "4b5563"))
                            Text("Purpose: \(device.purpose)")
                                .font(.system(size: 14))
                                .foregroundColor(Color(hex: "4b5563"))
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(hex: "f8fafc"))
                        .cornerRadius(8)
                    }
                }
            }

            Text("Important: Always consult with your healthcare provider before starting, stopping, or changing any medication. This guide is for informational purposes only.")
                .font(.system(size: 12))
                .italic()
                .foregroundColor(Color(hex: "6b7280"))
        }
        .sheet(item: $selectedInfo) { info in
            MedicationGuideSheet(info: info)
        }
    }
}

/// Android AlertDialog titled `Info.title`; sections joined by a blank line,
/// each heading bold; single "Close" button (:1715-1751).
struct MedicationGuideSheet: View {
    @Environment(\.dismiss) private var dismiss
    let info: MedicationTypeInfo

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    ForEach(Array(info.sections().enumerated()), id: \.offset) { _, section in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(section.heading)
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(Color(hex: "1f2937"))
                            Text(section.body)
                                .font(.system(size: 16))
                                .foregroundColor(Color(hex: "1f2937"))
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                }
                .padding(20)
            }
            .navigationTitle(info.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("Close") { dismiss() } }
            }
        }
    }
}
```

---

### Task 7: Resource Hub (audit §5)

**Files:**
- Create: `COPDFuel/COPDFuel/Views/Resources/ResourceHubView.swift`

- [ ] **Step 1: Create the file**

```swift
//
//  ResourceHubView.swift
//  COPDFuel
//
//  Resources › Resource Hub. Mirrors ResourcesFragment.buildResourceHubContent
//  (:1971-2445): COPD Organizations → Educational Resources → Support
//  Groups → COVID-19 and COPD → Quit Smoking with COPD.
//

import SwiftUI

struct ResourceHubView: View {
    private struct Org { let name: String; let detail: String; let helpline: String?; let url: String }
    private struct Card { let title: String; let type: String; let detail: String; let url: String }
    private struct CessationMedication { let name: String; let howItWorks: String; let sideEffects: String }

    private let organizations: [Org] = [
        Org(name: "American Lung Association",
            detail: "Provides education, advocacy and research to improve lung health and prevent lung disease.",
            helpline: "Helpline: 1-800-LUNGUSA", url: "https://www.lung.org"),
        Org(name: "COPD Foundation",
            detail: "Dedicated to improving the lives of those affected by COPD through research, education, early diagnosis, and enhanced therapy.",
            helpline: "Helpline: 1-866-316-COPD", url: "https://www.copdfoundation.org"),
        Org(name: "Global Initiative for Chronic Obstructive Lung Disease (GOLD)",
            detail: "Works to improve prevention and treatment of COPD through a global network.",
            helpline: nil, url: "https://goldcopd.org")
    ]
    private let educational: [Card] = [
        Card(title: "Short, Comprehensive COPD Videos", type: "Videos",
             detail: "Animations, PSAs, and videos from NHLBI on COPD risk factors, signs and symptoms, treatment options, and more.",
             url: "https://www.nhlbi.nih.gov/health-topics/education-and-awareness/copd-learn-more-breathe-better/copd-videos"),
        Card(title: "Living Well with COPD", type: "Website",
             detail: "Practical tips for managing daily life with COPD.",
             url: "https://www.livingwellwithcopd.com/"),
        Card(title: "COPD and Nutrition", type: "Guide",
             detail: "How nutrition supports breathing and overall health in COPD.",
             url: "https://www.lung.org/lung-health-diseases/lung-disease-lookup/copd/living-with-copd/nutrition")
    ]
    private let supportGroups: [Card] = [
        Card(title: "Right2Breathe", type: "Online",
             detail: "The Right2Breathe Pulmonary Chat is a free online chat and live video program where medical experts provide education and answer questions about living with COPD.",
             url: "https://right2breathe.org/"),
        Card(title: "Better Breathers Club", type: "In-person & Virtual",
             detail: "In-person and virtual support groups organized by the American Lung Association.",
             url: "https://www.lung.org/help-support/better-breathers-club/better-breathers-club-meetings"),
        Card(title: "COPD360social", type: "Online",
             detail: "Online community platform for individuals with COPD and their caregivers.",
             url: "https://www.copdfoundation.org/COPD360social/Community/Get-Involved.aspx")
    ]
    private let covidTips = [
        "Continue taking your COPD medications as prescribed",
        "Maintain at least a 30-day supply of your medications",
        "Follow recommendations for vaccination",
        "Practice physical distancing and wear masks when appropriate",
        "Have an emergency action plan in case you develop COVID-19 symptoms"
    ]
    private let cessationMedications: [CessationMedication] = [
        CessationMedication(name: "NRT (Nicotine Replacement)",
                   howItWorks: "Provides nicotine without the toxic smoke. Best used as a \"Combo\": Patch (steady) + Gum/Lozenge (rescue).",
                   sideEffects: "Skin irritation, vivid dreams, or jaw soreness."),
        CessationMedication(name: "Varenicline (Chantix)",
                   howItWorks: "Blocks the \"pleasure\" receptors in the brain and reduces withdrawal. Currently the most effective monotherapy.",
                   sideEffects: "Nausea (mitigated by food/water) and vivid dreams."),
        CessationMedication(name: "Bupropion (Zyban)",
                   howItWorks: "Originally an antidepressant, it reduces the urge to smoke. Good for those with co-occurring depression.",
                   sideEffects: "Dry mouth and insomnia."),
        CessationMedication(name: "Cytisinicline",
                   howItWorks: "New for 2026. A plant-based pill similar to Varenicline but shown in recent trials to be highly effective and well-tolerated in COPD patients.",
                   sideEffects: "Mild nausea or headache.")
    ]
    private let impactPoints = [
        "Better Inhaler Efficacy: When you stop smoking, the inflammation in your airways begins to subside. This allows your bronchodilators (like Albuterol or Spiriva) to reach deeper into the lungs and work more effectively.",
        "Reduced \"Mucus Plugs\": Smoking paralyzes the cilia (tiny hairs) that clear mucus. Quitting \"wakes them up,\" helping you clear phlegm more easily and reducing the risk of infections.",
        "Stabilized Lung Function: While lung damage from COPD is permanent, medications stop the \"accelerated decline.\" You go from losing lung function at a smoker's pace back to a natural aging pace."
    ]
    private let considerationPoints = [
        "Depression & Anxiety: COPD is physically and mentally taxing. Because some quit-smoking meds (like Bupropion or Varenicline) affect brain chemistry, it's vital to monitor your mood. Recent large-scale studies (including those published in early 2026) have confirmed these are generally safe for COPD patients but should be managed by your doctor.",
        "The \"Quit-Cough\": It sounds counterintuitive, but many COPD patients cough more for the first week after quitting. This is actually a sign of your lungs cleaning themselves out. Don't let it discourage you!",
        "Paced Quitting: For those not ready to stop today, \"Reduce to Quit\" programs using NRT can help you slowly lower your daily cigarette count, making the final quit day less of a shock to the system."
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            ResourcesTitle(text: "COPD Resource Hub")
            ResourcesInfoCard(text: "This resource hub provides links to trusted organizations, educational materials, and support groups to help you better understand and manage your COPD.")

            ResourcesHeading(text: "COPD Organizations")
            ForEach(organizations, id: \.name) { org in
                hubCard {
                    Text(org.name).font(.system(size: 18, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
                    Text(org.detail).font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
                    if let helpline = org.helpline {
                        Text(helpline).font(.system(size: 14, weight: .semibold)).foregroundColor(Color(hex: "1f2937"))
                    }
                    ResourcesLinkButton(title: "Visit Website", url: org.url)
                }
            }

            ResourcesHeading(text: "Educational Resources")
            ForEach(educational, id: \.title) { card in
                hubCard {
                    Text(card.title).font(.system(size: 18, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
                    typeTag(card.type)
                    Text(card.detail).font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
                    ResourcesLinkButton(title: "Access Resource", url: card.url)
                }
            }

            ResourcesHeading(text: "Support Groups")
            ForEach(supportGroups, id: \.title) { card in
                hubCard {
                    Text(card.title).font(.system(size: 18, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
                    typeTag(card.type)
                    Text(card.detail).font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
                    ResourcesLinkButton(title: "Visit Website", url: card.url)
                }
            }

            ResourcesHeading(text: "COVID-19 and COPD")
            ResourcesSubheading(text: "Special Considerations for COPD Patients")
            Text("People with COPD may be at higher risk for severe illness from COVID-19. It's important to take extra precautions and stay updated with the latest guidance.")
                .font(.system(size: 16)).foregroundColor(Color(hex: "4b5563"))
            bullets(covidTips)

            ResourcesHeading(text: "Quit Smoking with COPD")
            Text("When you have COPD, quitting smoking is more than just a lifestyle change—it's a medical intervention. Because COPD often comes with high nicotine dependence and increased rates of depression, medications are frequently the \"bridge\" needed to make a quit attempt successful.")
                .font(.system(size: 16)).foregroundColor(Color(hex: "4b5563"))

            ResourcesSubheading(text: "1. Smoking Cessation Medications")
            Text("For 2026, there are three primary paths for medication, plus a promising newcomer.")
                .font(.system(size: 14)).foregroundColor(Color(hex: "6b7280"))
            ForEach(cessationMedications, id: \.name) { med in
                hubCard {
                    Text(med.name).font(.system(size: 16, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
                    Text("How it works: \(med.howItWorks)").font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
                    Text("Common side effects: \(med.sideEffects)").font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
                }
            }

            ResourcesSubheading(text: "2. Impact on COPD Symptoms")
            Text("Medications don't just help you quit; they indirectly improve your COPD management by removing the constant irritation of smoke.")
                .font(.system(size: 16)).foregroundColor(Color(hex: "4b5563"))
            bullets(impactPoints)

            ResourcesSubheading(text: "3. Important Considerations for COPD")
            bullets(considerationPoints)
        }
    }

    private func hubCard<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8, content: content)
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: "f8fafc"))
            .cornerRadius(12)
    }

    private func typeTag(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 12, weight: .semibold))
            .foregroundColor(Color(hex: "1e40af"))
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(Color(hex: "dbeafe"))
            .cornerRadius(12)
    }

    /// Android renders "  • $tip" with an 18dp hanging indent (:2296-2306).
    private func bullets(_ items: [String]) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            ForEach(items, id: \.self) { tip in
                HStack(alignment: .top, spacing: 6) {
                    Text("•")
                    Text(tip)
                }
                .font(.system(size: 16))
                .foregroundColor(Color(hex: "4b5563"))
            }
        }
    }
}
```

---

### Task 8: Build, copy checks, commit

- [ ] **Step 1: Build**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -derivedDataPath "$HOME/Library/Developer/Xcode/DerivedData/COPDFuel-cli" -quiet build 2>&1 | grep -E "error:" | head -20; echo "exit=${PIPESTATUS[0]}"
```
Expected `exit=0`. Likely first-pass errors and their fixes:
- `'ExerciseJournalEntry' has no member 'time'` in `DailyTrackingSummary.swift` → Task 1 Step 4 missed the `MonthlyEntryCard` line.
- `Cannot find 'GuidelineCard' in scope` → P3.A not yet applied (it defines `GuidelineCard`); apply P3.A first.
- Duplicate `ContactField`/`ExerciseJournalEntryCard` → the old `ResourcesView.swift` was not replaced.

- [ ] **Step 2: Copy checks (Android ↔ iOS)**

```bash
for s in "Severity\\\\nEval" "COPD Management Tools" "Saved summary:" "Second Emergency Contact Name" "Insurance Provider (name only, no ID numbers)" "No daily medications added yet." "Enter new instructions from your doctor..." "Home Exercise Program" "Pursed-Lip Breathing" "Track Your Progress" "Watch Inhaler Technique Videos" "High-frequency chest wall oscillation vest" "Helpline: 1-800-LUNGUSA" "Quit Smoking with COPD" "Cytisinicline"; do
  a=$(grep -rl -F "$s" android/app/src/main/java/com/copdhealthtracker/ui | wc -l | tr -d ' ')
  i=$(grep -rl -F "$s" COPDFuel/COPDFuel/Views/Resources | wc -l | tr -d ' ')
  echo "$a android / $i ios  <- $s"
done
grep -c "This guide is general education, not medical advice" COPDFuel/COPDFuel/Views/Resources/MedicationTypes.swift
```
Expected: every row non-zero on both sides (the first row's `\n` label appears as `\\n` in both sources; if it prints 0/0, grep `Severity` alone), and the last command prints `1`.

- [ ] **Step 3: Removal checks**

```bash
grep -rn "SeverityCalculator\|COPDSeverity\|Recommended Exercises\|Emergency Resources\|National Heart, Lung, and Blood Institute\|doctorInstructionsLog\|exerciseJournalLog\|severity_flareups\|severity_saved_at" COPDFuel/COPDFuel --include='*.swift' | grep -v "legacyKey\|Legacy\|removeObject\|forKey: \"doctorInstructionsLog\"\|forKey: \"exerciseJournalLog\""
```
Expected: no output (the only remaining mentions are inside the two `Store` migrations).

- [ ] **Step 4: Commit**

```bash
cd COPDFuel && /usr/bin/git add -A && /usr/bin/git commit -q -m "iOS parity P3.B: Resources tab rebuilt from Android (severity summary, action plan contacts x7 + logs, full pulmonary rehab program, 10 medication guides + devices, resource hub); journal/instructions on Android keys

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>" && /usr/bin/git log --oneline -1
```

- [ ] **Step 5: Manual smoke (simulator)**

Resources tab: five cards in one row with two-line labels; Severity Eval saves and shows "Saved summary:"; Action Plan saves contacts with a toast, adds/deletes a medication via the dialog (empty name shows the toast and keeps the dialog open), saves an instruction (blank shows toast); Pulm. Rehab scrolls through the home program to the journal and ends with the full-width image; "Find Programs Near Me" pushes the programs screen; Resp. Care tile tap opens the guide sheet with ten sections and a Close button; Resource Hub links open Safari. Tracking → Month still shows journal entries saved from Resources.
