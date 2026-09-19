# iOS Parity P3.E (Profile, Report generator, API client, Link doctor, Delete account) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the iOS Profile tab to Android `ProfileFragment` + `fragment_profile.xml` (order, labels, edit dialogs with toasts, always-visible BMI / Last Updated, radio activity levels with hints, protein-target notes, oxygen/BIPAP questions, premium/privacy/terms/HIPAA rows, Share Health Report card, Share with your doctor card, doctor-info rows, Sign out, Delete my account), move profile storage to Android's flat pref keys with a one-shot migration, port `ReportGenerator.kt` 1:1, and give `COPDAPIClient` the Android endpoints/payloads (`DELETE /me`, `/consent`, `/link-doctor`) with JSON error surfacing.

**Architecture:** New `Services/ProfileStore.swift` owns the flat keys, BMI/height helpers and the `userProfile` blob migration; `DataManager` drops `profile`/`UserProfile`. `ProfileView.swift` is rewritten as one scrolling page of cards (Android has no toolbar) with `.alert`-based "Edit {Title}" dialogs and a wheel-picker height sheet. `ReportGenerator.swift` is rewritten section-by-section from the Kotlin, keeping the iOS-only STEPS / HEART RATE section at the end. Health import parity (audit §F) already landed in P3.C (1/2).

**Tech Stack:** SwiftUI, StoreKit (existing `StoreManager`), Amplify Auth (existing `AuthService`), URLSession.

**Spec:** `docs/superpowers/specs/2026-09-19-ios-parity-p3-design.md` §1.5 (keys), §2.4 (flat profile storage), §2.5, §3 row P3.E. Gap list: `docs/superpowers/specs/2026-09-19-ios-parity-p3/audit-profile-report-auth.md` §A (A1–A17), §B (B1–B14), §D6, §G. Android sources: `ui/fragments/ProfileFragment.kt`, `res/layout/fragment_profile.xml`, `utils/ReportGenerator.kt`, `res/values/strings.xml:11-16,49-52`.

## Global Constraints

- Copy verbatim; `Toast` → `ToastCenter.shared.show`; `AlertDialog` → `.alert` (spec §1.1). Platform idioms kept: single `UIActivityViewController` share sheet with subject `"COPD Fuel Report - MMM d, yyyy"` (audit A14), wheel pickers for height, a `Toggle` for the link-doctor checkbox.
- Android bugs NOT ported (spec §1.3): iOS persists `condition_kidney_disease` / `condition_dialysis`; the Terms of Use row works.
- iOS-only kept (spec §1.4): friendly Cognito messages via `AuthService.userMessage(for:)`; STEPS / HEART RATE report section; re-run sync on HIPAA sign.
- Removed: Profile "Goal Weight" field (goal lives only in the weight log; the blob's `goalWeight` is migrated into a goal `WeightEntry` once), the invite-code Link Doctor screen, inline "Last updated" only-when-set row (now always visible as "Last Updated" / "N/A").
- Keys (spec §1.5): `age`, `sex`, `weight`, `height`, `last_updated`, `doctor_name`, `doctor_phone`, `emergency_contact_name`, `emergency_contact_phone`, `emergency_contact2_name`, `emergency_contact2_phone`, `insurance_provider`, `activity_level`, `protein_target` (Double), `protein_target_low`/`_high` (Int), `condition_*`, `uses_oxygen`, `oxygen_lpm`, `uses_bipap`, `bipap_machine_type`, `bipap_setting`, `severity_*` (P3.B), `exercise_journal_log` / `doctor_instructions_log` (P3.B).
- MemberImportVisibility: any file with `ObservableObject`/`@Published` imports `Combine`.
- Depends on P3.0 (`ToastCenter`), P3.B (`DoctorInstructionsStore`, `ExerciseJournalStore`, severity keys), P3.C (1/2) (`ProfileWeightSync`, `TrackingView.swift` removed so no other `dataManager.profile` reader remains).
- Build after Task 4 and at the end; one commit at the end. `/usr/bin/git`; commands from the parent repo root.

---

### Task 1: `ProfileStore` + blob migration; drop `UserProfile`

**Files:**
- Create: `COPDFuel/COPDFuel/Services/ProfileStore.swift`
- Delete: `COPDFuel/COPDFuel/Models/UserProfile.swift`
- Modify: `COPDFuel/COPDFuel/Services/DataManager.swift` (remove `profileKey`, `profile`, `saveProfile`, the profile load block, `isProfileComplete`; call the migration in `init`)
- Modify: `COPDFuel/COPDFuel/Services/ProfileWeightSync.swift` (drop the `profile_weight` mirror + fallback)

**Interfaces (produced):**
- `ProfileStore.get(_ key: String) -> String?` (nil when missing/empty), `.set(_ value: String, key: String)`, `.touchLastUpdated() -> String`, `.bmiText(weight:height:) -> String?`, `.parseHeightToInches(_:) -> Double?`, `.parseHeightToFeetInches(_:) -> (Int, Int)?`, `.migrateUserProfileBlobIfNeeded()`, `.activityLabel(for code: String) -> String`.

- [ ] **Step 1: Create `Services/ProfileStore.swift`**

```swift
//
//  ProfileStore.swift
//  COPDFuel
//
//  Flat profile storage on Android's SharedPreferences key names
//  (ProfileFragment.kt), plus the one-shot migration from the pre-P3
//  iOS `userProfile` JSON blob and `profile_weight`.
//

import Foundation

enum ProfileStore {
    private static let defaults = UserDefaults.standard

    static let activityLevels: [(code: String, label: String, hint: String?)] = [
        ("low", "Low activity (limited movement or mostly sedentary)", "1.0-1.2 g protein per kg body weight/day"),
        ("moderate", "Moderate activity (regular daily activities with some exercise)", nil),
        ("pulmonary_rehab", "Participating in pulmonary rehabilitation", "1.2-1.4 g protein per kg body weight/day"),
        ("high", "High activity (active lifestyle or regular exercise)", nil),
        ("exacerbation", "Currently experiencing a COPD exacerbation or flare up", "1.6-1.8 g protein per kg body weight/day"),
        ("kidney_disease", "Kidney disease (including chronic kidney disease Stage 2)", "1.0-1.2 g protein per kg body weight/day"),
        ("dialysis", "Currently on dialysis", "1.2-1.8 g protein per kg body weight/day")
    ]

    /// ReportGenerator.kt:90-99.
    static func activityLabel(for code: String) -> String {
        activityLevels.first { $0.code == code }?.label ?? "Not set"
    }

    static func get(_ key: String) -> String? {
        guard let v = defaults.string(forKey: key), !v.isEmpty else { return nil }
        return v
    }

    static func set(_ value: String, key: String) {
        defaults.set(value, forKey: key)
    }

    /// Writes "last_updated" = "MMM d, yyyy" and returns it.
    @discardableResult
    static func touchLastUpdated() -> String {
        let f = DateFormatter()
        f.dateFormat = "MMM d, yyyy"
        let stamp = f.string(from: Date())
        defaults.set(stamp, forKey: "last_updated")
        return stamp
    }

    // MARK: BMI / height (ProfileFragment.kt:752-769, 593-610)

    static func bmiText(weight: String?, height: String?) -> String? {
        guard let weight, let height, !weight.isEmpty, !height.isEmpty,
              weight != "Not set", height != "Not set",
              let lbs = Double(weight), lbs > 0,
              let inches = parseHeightToInches(height), inches > 0 else { return nil }
        return String(format: "%.1f", (lbs / (inches * inches)) * 703)
    }

    static func parseHeightToInches(_ s: String) -> Double? {
        let numbers = s.trimmingCharacters(in: .whitespaces)
            .components(separatedBy: CharacterSet(charactersIn: "0123456789.").inverted)
            .filter { !$0.isEmpty }
            .compactMap { Double($0) }
        if numbers.count >= 2 { return numbers[0] * 12 + numbers[1] }
        if numbers.count == 1 { return numbers[0] < 25 ? numbers[0] * 12 : numbers[0] }
        return nil
    }

    static func parseHeightToFeetInches(_ s: String) -> (Int, Int)? {
        let numbers = s.trimmingCharacters(in: .whitespaces)
            .components(separatedBy: CharacterSet.decimalDigits.inverted)
            .filter { !$0.isEmpty }
            .compactMap { Int($0) }
        if numbers.count >= 2 { return (numbers[0], numbers[1]) }
        if numbers.count == 1 { return numbers[0] > 24 ? (numbers[0] / 12, numbers[0] % 12) : (numbers[0], 0) }
        return nil
    }

    // MARK: Migration (spec §2.4)

    /// Copies the pre-P3 `userProfile` blob and `profile_weight` into the
    /// flat keys (never overwriting a flat value that already exists), turns
    /// the blob's goalWeight into a goal WeightEntry when none exists, then
    /// removes the old keys.
    static func migrateUserProfileBlobIfNeeded() {
        struct LegacyProfile: Decodable {
            let age: String?; let sex: String?; let height: String?; let goalWeight: String?
            let doctorName: String?; let doctorPhone: String?
            let emergencyContactName: String?; let emergencyContactPhone: String?
        }
        func copy(_ value: String?, to key: String) {
            guard let value, !value.isEmpty, get(key) == nil else { return }
            defaults.set(value, forKey: key)
        }
        if let data = defaults.data(forKey: "userProfile"),
           let legacy = try? JSONDecoder().decode(LegacyProfile.self, from: data) {
            copy(legacy.age, to: "age")
            copy(legacy.sex, to: "sex")
            copy(legacy.height, to: "height")
            copy(legacy.doctorName, to: "doctor_name")
            copy(legacy.doctorPhone, to: "doctor_phone")
            copy(legacy.emergencyContactName, to: "emergency_contact_name")
            copy(legacy.emergencyContactPhone, to: "emergency_contact_phone")
            if let goal = legacy.goalWeight.flatMap({ Double($0) }), goal > 0,
               DataManager.shared.getGoalWeight() == nil {
                DataManager.shared.addWeightEntry(WeightEntry(weight: goal, type: .goal))
            }
            defaults.removeObject(forKey: "userProfile")
        }
        if let w = defaults.string(forKey: "profile_weight"), !w.isEmpty {
            copy(w, to: "weight")
            defaults.removeObject(forKey: "profile_weight")
        }
    }
}
```
Note: `migrateUserProfileBlobIfNeeded` touches `DataManager.shared`, so call it **after** `loadAllData()` in `DataManager.init` (not before), or from `ProfileView.onAppear` — call it from `DataManager.init` at the end of `loadAllData()`.

- [ ] **Step 2: `DataManager`** — delete `private let profileKey = "userProfile"`, `@Published var profile: UserProfile = UserProfile()`, `func saveProfile(_:)`, the "Load profile" block in `loadAllData()`, and `func isProfileComplete()`. At the end of `loadAllData()` add `ProfileStore.migrateUserProfileBlobIfNeeded()`.

- [ ] **Step 3: delete `Models/UserProfile.swift`**

```bash
cd COPDFuel && /usr/bin/git rm -q COPDFuel/Models/UserProfile.swift && grep -rn "UserProfile\|dataManager.profile\|\.saveProfile(" COPDFuel --include='*.swift'
```
Expected: only `Views/ProfileView.swift` and `Services/ReportGenerator.swift` (both rewritten below).

- [ ] **Step 4: `ProfileWeightSync`** — in `writeWeightToPrefs` delete the `defaults.set(text, forKey: "profile_weight")` line; in `profileWeightString()` delete the `profile_weight` fallback (return only the `weight` value).

---

### Task 2: `COPDAPIClient` endpoints + error surfacing (audit D6, A15, A17)

**Files:**
- Modify: `COPDFuel/COPDFuel/Services/COPDAPIClient.swift`

- [ ] **Step 1: Replace `consent`, `linkDoctor` and add `deleteMe`**

```swift
    /// ProfileFragment.setupLinkDoctor: POST /consent {practiceId, doctorId, consentType}.
    func consent(token: String, practiceId: String = "default", doctorId: String = "default",
                 consentType: String = "share_with_doctor") async throws {
        let body: [String: Any] = ["practiceId": practiceId, "doctorId": doctorId, "consentType": consentType]
        try await request(method: "POST", path: "/consent", token: token, body: body)
    }

    /// POST /link-doctor {practiceId}.
    func linkDoctor(token: String, practiceId: String = "default") async throws {
        try await request(method: "POST", path: "/link-doctor", token: token, body: ["practiceId": practiceId])
    }

    /// DELETE /me (ProfileFragment.performDeleteAccount).
    func deleteMe(token: String) async throws {
        try await request(method: "DELETE", path: "/me", token: token, body: nil)
    }
```

- [ ] **Step 2: Medication `date` from the record + JSON error surfacing**

In `sync`, change the medications payload `"date": Int(Date().timeIntervalSince1970 * 1000)` to `"date": Int($0.date.timeIntervalSince1970 * 1000)`.

Replace the tail of `request` (from `let (_, response)`) with:
```swift
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw APIError.badResponse }
        if http.statusCode < 200 || http.statusCode >= 300 {
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let message = json["error"] as? String, !message.isEmpty {
                throw APIError.server(message)
            }
            throw APIError.httpStatus(http.statusCode)
        }
```
and add `case server(String)` to `APIError` with `case .server(let message): return message`.

---

### Task 3: `ProfileView` rewrite (audit A1–A17)

**Files:**
- Rewrite: `COPDFuel/COPDFuel/Views/ProfileView.swift` (keep `ReportActivityItemSource` and `ShareSheet` at the bottom verbatim; delete `ProfileContentView`, `LinkDoctorView`, `ProfileAccountSection`)

- [ ] **Step 1: Replace everything above `// MARK: - Activity item with pre-filled subject`**

```swift
//
//  ProfileView.swift
//  COPDFuel
//
//  Profile tab. Structure, copy, edit dialogs and flows mirror Android
//  ProfileFragment.kt + fragment_profile.xml. Storage: flat UserDefaults
//  keys via ProfileStore (Android SharedPreferences names).
//

import SwiftUI

struct ProfileView: View {
    @ObservedObject private var dataManager = DataManager.shared
    @ObservedObject private var hipaaStorage = HipaaConsentStorage.shared
    @ObservedObject private var store = StoreManager.shared
    @ObservedObject private var auth = AuthService.shared

    // Personal + contacts (display values; "" → "N/A")
    @State private var age = ""
    @State private var sex = ""
    @State private var weight = ""
    @State private var height = ""
    @State private var lastUpdated = ""
    @State private var doctorName = ""
    @State private var doctorPhone = ""
    @State private var emergencyName = ""
    @State private var emergencyPhone = ""

    // Activity / conditions / devices
    @State private var activityLevel = ""
    @State private var proteinTargetText = "-- g/day (set your weight first)"
    @State private var kidney = false
    @State private var cholesterol = false
    @State private var pulmonaryHypertension = false
    @State private var dialysis = false
    @State private var none = false
    @State private var other = false
    @State private var otherText = ""
    @State private var usesOxygen: Bool?
    @State private var oxygenLpm = ""
    @State private var usesBipap: Bool?
    @State private var machineType = ""
    @State private var bipapSetting = ""
    @State private var suppressSaves = true

    // Dialogs
    private struct EditPrompt: Identifiable { let id = UUID(); let key: String; let title: String; let numeric: Bool; let decimal: Bool }
    @State private var editPrompt: EditPrompt?
    @State private var editValue = ""
    @State private var showingHeightPicker = false
    @State private var heightFeet = 5
    @State private var heightInches = 6
    @State private var showPaywall = false
    @State private var showHipaa = false
    @State private var showShareSheet = false
    @State private var reportText = ""
    @State private var hipaaPrompt: HipaaPromptState?
    @State private var linkConsentChecked = false
    @State private var isLinking = false
    @State private var showDeleteConfirm = false
    @State private var isDeleting = false

    private var reportActivityItem: ReportActivityItemSource {
        let f = DateFormatter(); f.dateFormat = "MMM d, yyyy"
        return ReportActivityItemSource(text: reportText, subject: "COPD Fuel Report - \(f.string(from: Date()))")
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    Text("My Profile")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Color(hex: "1f2937"))
                        .frame(maxWidth: .infinity, alignment: .leading)
                    personalCard
                    activityCard
                    conditionsCard
                    rowsCard
                    shareReportCard
                    linkDoctorCard
                    doctorInfoCard
                    TrackingButton(title: "Sign out", background: Color(hex: "6b7280")) { signOut() }
                    TrackingButton(title: "Delete my account", background: Color(hex: "dc2626")) { showDeleteConfirm = true }
                }
                .padding(16)
            }
            .background(Color(hex: "f8fafc"))
            .toolbar(.hidden, for: .navigationBar)
        }
        .onAppear {
            ProfileWeightSync.syncProfileAndDbWeight()
            load()
        }
        .sheet(isPresented: $showPaywall) { PaywallView() }
        .sheet(isPresented: $showHipaa) { HipaaAuthorizationView() }
        .sheet(isPresented: $showShareSheet) { ShareSheet(activityItems: [reportActivityItem]) }
        .sheet(isPresented: $showingHeightPicker) { heightPickerSheet }
        .alert(editPrompt.map { "Edit \($0.title)" } ?? "", isPresented: Binding(
            get: { editPrompt != nil }, set: { if !$0 { editPrompt = nil } }
        ), presenting: editPrompt) { prompt in
            TextField("Enter \(prompt.title)", text: $editValue)
                .keyboardType(prompt.decimal ? .decimalPad : (prompt.numeric ? .numberPad : (prompt.key.hasSuffix("phone") ? .phonePad : .default)))
            Button("Save") { saveEdited(prompt) }
            Button("Cancel", role: .cancel) {}
        }
        .alert("Delete my account", isPresented: $showDeleteConfirm) {
            Button("Delete account", role: .destructive) { performDeleteAccount() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will permanently delete your account and all health data from our servers. You can also request deletion by emailing support@copdfuel.com. Are you sure?")
        }
        .hipaaConsentAlert($hipaaPrompt)
    }

    // MARK: Cards

    private func card<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10, content: content)
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.06), radius: 3, x: 0, y: 1)
    }

    private func display(_ v: String) -> String { (v.isEmpty || v == "Not set") ? "N/A" : v }

    /// Tappable "label   value >" row (fragment_profile.xml).
    private func editRow(_ title: String, value: String, key: String, numeric: Bool = false, decimal: Bool = false) -> some View {
        Button(action: {
            editValue = (value == "N/A") ? "" : value
            editPrompt = EditPrompt(key: key, title: title, numeric: numeric, decimal: decimal)
        }) {
            HStack {
                Text(title).font(.system(size: 16)).foregroundColor(Color(hex: "1f2937"))
                Spacer()
                Text(value).font(.system(size: 16)).foregroundColor(Color(hex: "4b5563"))
                Text(">").font(.system(size: 16, weight: .bold)).foregroundColor(Color(hex: "9ca3af"))
            }
            .padding(.vertical, 8)
        }
        .buttonStyle(.plain)
    }

    private func staticRow(_ title: String, value: String) -> some View {
        HStack {
            Text(title).font(.system(size: 16)).foregroundColor(Color(hex: "1f2937"))
            Spacer()
            Text(value).font(.system(size: 16)).foregroundColor(Color(hex: "4b5563"))
        }
        .padding(.vertical, 8)
    }

    private var personalCard: some View {
        card {
            editRow("Age", value: display(age), key: "age", numeric: true)
            editRow("Sex", value: display(sex), key: "sex")
            editRow("Weight", value: weight.isEmpty || weight == "Not set" ? "N/A" : "\(weight) lbs", key: "weight", decimal: true)
            Button(action: openHeightPicker) {
                HStack {
                    Text("Height").font(.system(size: 16)).foregroundColor(Color(hex: "1f2937"))
                    Spacer()
                    Text(display(height)).font(.system(size: 16)).foregroundColor(Color(hex: "4b5563"))
                    Text(">").font(.system(size: 16, weight: .bold)).foregroundColor(Color(hex: "9ca3af"))
                }
                .padding(.vertical, 8)
            }
            .buttonStyle(.plain)
            staticRow("Body Mass Index (BMI)", value: ProfileStore.bmiText(weight: weight, height: height) ?? "N/A")
            staticRow("Last Updated", value: lastUpdated.isEmpty ? "N/A" : lastUpdated)
        }
    }

    private var activityCard: some View {
        card {
            Text("Activity Level").font(.system(size: 18, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
            Text("Which option best describes your current activity level?")
                .font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
            ForEach(ProfileStore.activityLevels, id: \.code) { level in
                Button(action: { setActivity(level.code) }) {
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: activityLevel == level.code ? "largecircle.fill.circle" : "circle")
                            .foregroundColor(Color(hex: "2563eb"))
                            .padding(.top, 2)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(level.label).font(.system(size: 15)).foregroundColor(Color(hex: "1f2937"))
                            if let hint = level.hint {
                                Text(hint).font(.system(size: 12)).foregroundColor(Color(hex: "6b7280"))
                            }
                        }
                    }
                    .padding(.vertical, 4)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text("Your Daily Protein Target").font(.system(size: 16, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
                Text(proteinTargetText).font(.system(size: 16)).foregroundColor(Color(hex: "2563eb"))
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: "eff6ff"))
            .cornerRadius(8)
        }
    }

    private func checkbox(_ title: String, _ isOn: Binding<Bool>, onChange: @escaping (Bool) -> Void) -> some View {
        Toggle(isOn: isOn) {
            Text(title).font(.system(size: 15)).foregroundColor(Color(hex: "1f2937"))
        }
        .toggleStyle(.switch)
        .onChange(of: isOn.wrappedValue) { _, new in
            if !suppressSaves { onChange(new) }
        }
    }

    private func yesNo(_ question: String, _ value: Binding<Bool?>, onChange: @escaping (Bool) -> Void) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(question).font(.system(size: 15, weight: .semibold)).foregroundColor(Color(hex: "1f2937"))
            HStack(spacing: 16) {
                ForEach([true, false], id: \.self) { option in
                    Button(action: { value.wrappedValue = option; onChange(option) }) {
                        HStack(spacing: 6) {
                            Image(systemName: value.wrappedValue == option ? "largecircle.fill.circle" : "circle")
                                .foregroundColor(Color(hex: "2563eb"))
                            Text(option ? "Yes" : "No").font(.system(size: 15)).foregroundColor(Color(hex: "1f2937"))
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(.top, 6)
    }

    private var conditionsCard: some View {
        card {
            Text("Health Conditions with COPD").font(.system(size: 18, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
            Text("Please check all that apply (affects protein target):").font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
            checkbox("Kidney disease (including chronic kidney disease Stage 2)", $kidney) { v in
                UserDefaults.standard.set(v, forKey: "condition_kidney_disease"); if v { none = false }; touch()
            }
            checkbox("High cholesterol", $cholesterol) { v in
                UserDefaults.standard.set(v, forKey: "condition_high_cholesterol"); if v { none = false }; touch()
            }
            checkbox("Pulmonary hypertension", $pulmonaryHypertension) { v in
                UserDefaults.standard.set(v, forKey: "condition_pulmonary_hypertension"); if v { none = false }; touch()
            }
            checkbox("Currently on dialysis", $dialysis) { v in
                UserDefaults.standard.set(v, forKey: "condition_dialysis"); if v { none = false }; touch()
            }
            checkbox("None of the above", $none) { v in
                UserDefaults.standard.set(v, forKey: "condition_none")
                if v { kidney = false; cholesterol = false; pulmonaryHypertension = false; dialysis = false; other = false }
                touch()
            }
            checkbox("Other (please specify)", $other) { v in
                UserDefaults.standard.set(v, forKey: "condition_other")
                if !v { otherText = ""; UserDefaults.standard.set("", forKey: "condition_other_text") }
                if v { none = false }
                touch()
            }
            if other {
                TextField("Please specify other conditions", text: $otherText, axis: .vertical)
                    .lineLimit(2...4)
                    .textFieldStyle(.roundedBorder)
                    .onSubmit { UserDefaults.standard.set(otherText, forKey: "condition_other_text"); touch() }
                    .onChange(of: otherText) { _, new in
                        if !suppressSaves { UserDefaults.standard.set(new, forKey: "condition_other_text") }
                    }
            }

            yesNo("Do you use supplemental oxygen?", $usesOxygen) { yes in
                UserDefaults.standard.set(yes, forKey: "uses_oxygen")
                if !yes { oxygenLpm = ""; UserDefaults.standard.set("", forKey: "oxygen_lpm") }
                touch()
            }
            if usesOxygen == true {
                Text("What is your LPM?").font(.system(size: 15)).foregroundColor(Color(hex: "1f2937"))
                TextField("Enter LPM", text: $oxygenLpm)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(.roundedBorder)
                    .onChange(of: oxygenLpm) { _, new in
                        if !suppressSaves { UserDefaults.standard.set(new, forKey: "oxygen_lpm"); touch() }
                    }
            }

            yesNo("Are you on a BIPAP or NIV?", $usesBipap) { yes in
                UserDefaults.standard.set(yes, forKey: "uses_bipap")
                if !yes {
                    machineType = ""; bipapSetting = ""
                    UserDefaults.standard.set("", forKey: "bipap_machine_type")
                    UserDefaults.standard.set("", forKey: "bipap_setting")
                }
                touch()
            }
            if usesBipap == true {
                Text("Which machine?").font(.system(size: 15)).foregroundColor(Color(hex: "1f2937"))
                HStack(spacing: 16) {
                    ForEach(["BIPAP", "NIV"], id: \.self) { m in
                        Button(action: { machineType = m; UserDefaults.standard.set(m, forKey: "bipap_machine_type"); touch() }) {
                            HStack(spacing: 6) {
                                Image(systemName: machineType == m ? "largecircle.fill.circle" : "circle").foregroundColor(Color(hex: "2563eb"))
                                Text(m).font(.system(size: 15)).foregroundColor(Color(hex: "1f2937"))
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
                Text("What is your setting?").font(.system(size: 15)).foregroundColor(Color(hex: "1f2937"))
                TextField("Enter setting", text: $bipapSetting)
                    .textFieldStyle(.roundedBorder)
                    .onChange(of: bipapSetting) { _, new in
                        if !suppressSaves { UserDefaults.standard.set(new, forKey: "bipap_setting"); touch() }
                    }
            }
        }
    }

    private func linkRow(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Text(title).font(.system(size: 16)).foregroundColor(Color(hex: "1f2937"))
                Spacer()
                Text(">").font(.system(size: 16, weight: .bold)).foregroundColor(Color(hex: "9ca3af"))
            }
            .padding(.vertical, 10)
        }
        .buttonStyle(.plain)
    }

    private var hipaaStatus: String {
        let f = DateFormatter(); f.dateFormat = "MMM d, yyyy"
        if hipaaStorage.hasValidConsent(), let d = hipaaStorage.getConsentDate() { return "Signed on \(f.string(from: d))" }
        if hipaaStorage.isRevoked(), let d = hipaaStorage.getRevokedDate() { return "Revoked on \(f.string(from: d))" }
        return "Not signed"
    }

    private var rowsCard: some View {
        card {
            linkRow(store.isPremium ? "Premium active" : "Subscribe to Premium") { showPaywall = true }
            Divider()
            linkRow("Privacy Policy") { open(AppConfig.privacyPolicyURL) }
            Divider()
            linkRow("Terms of Use") { open(AppConfig.termsOfUseURL) }
            Divider()
            Button(action: { showHipaa = true }) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("HIPAA Authorization (PHI Disclosure)").font(.system(size: 16)).foregroundColor(Color(hex: "1f2937"))
                        Text(hipaaStatus).font(.system(size: 13)).foregroundColor(Color(hex: "4b5563"))
                    }
                    Spacer()
                    Text(">").font(.system(size: 16, weight: .bold)).foregroundColor(Color(hex: "9ca3af"))
                }
                .padding(.vertical, 10)
            }
            .buttonStyle(.plain)
        }
    }

    private var shareReportCard: some View {
        card {
            Text("Share Health Report").font(.system(size: 18, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
            Text("Share a comprehensive report of your health data including tracking information, severity assessment, and exacerbation plan with your healthcare provider or caregiver.")
                .font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
            TrackingButton(title: "Share My Health Report") { shareTapped() }
        }
    }

    private var linkDoctorCard: some View {
        card {
            Text("Share with your doctor").font(.system(size: 18, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
            Text("Share your health data with your doctor or practice.").font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
            Toggle(isOn: $linkConsentChecked) {
                Text("I agree to share my health data with this practice.").font(.system(size: 14)).foregroundColor(Color(hex: "1f2937"))
            }
            TrackingButton(title: isLinking ? "Linking..." : "Link to doctor") { linkDoctor() }
                .disabled(isLinking)
        }
    }

    private var doctorInfoCard: some View {
        card {
            editRow("Doctor Name", value: display(doctorName), key: "doctor_name")
            editRow("Doctor Phone", value: display(doctorPhone), key: "doctor_phone")
            editRow("Emergency Contact Name", value: display(emergencyName), key: "emergency_contact_name")
            editRow("Emergency Contact Phone", value: display(emergencyPhone), key: "emergency_contact_phone")
        }
    }

    private var heightPickerSheet: some View {
        NavigationStack {
            HStack(spacing: 0) {
                Picker("Feet", selection: $heightFeet) { ForEach(3...8, id: \.self) { Text("\($0)").tag($0) } }
                    .pickerStyle(.wheel).frame(maxWidth: .infinity)
                Text("'").font(.system(size: 24))
                Picker("Inches", selection: $heightInches) { ForEach(0...11, id: \.self) { Text("\($0)").tag($0) } }
                    .pickerStyle(.wheel).frame(maxWidth: .infinity)
                Text("\"").font(.system(size: 24))
            }
            .padding()
            .navigationTitle("Edit Height")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showingHeightPicker = false } }
                ToolbarItem(placement: .confirmationAction) { Button("Save") { saveHeight() } }
            }
        }
        .presentationDetents([.medium])
        .toastOverlay()
    }

    // MARK: Load / save (ProfileFragment.applyProfileUiFromPrefs, editField, updateProteinTarget)

    private func load() {
        suppressSaves = true
        let d = UserDefaults.standard
        age = ProfileStore.get("age") ?? ""
        sex = ProfileStore.get("sex") ?? ""
        weight = ProfileStore.get("weight") ?? ""
        height = ProfileStore.get("height") ?? ""
        lastUpdated = ProfileStore.get("last_updated") ?? ""
        doctorName = ProfileStore.get("doctor_name") ?? ""
        doctorPhone = ProfileStore.get("doctor_phone") ?? ""
        emergencyName = ProfileStore.get("emergency_contact_name") ?? ""
        emergencyPhone = ProfileStore.get("emergency_contact_phone") ?? ""
        kidney = d.bool(forKey: "condition_kidney_disease")
        cholesterol = d.bool(forKey: "condition_high_cholesterol")
        pulmonaryHypertension = d.bool(forKey: "condition_pulmonary_hypertension")
        dialysis = d.bool(forKey: "condition_dialysis")
        none = d.bool(forKey: "condition_none")
        other = d.bool(forKey: "condition_other")
        otherText = d.string(forKey: "condition_other_text") ?? ""
        usesOxygen = d.object(forKey: "uses_oxygen") == nil ? nil : d.bool(forKey: "uses_oxygen")
        oxygenLpm = d.string(forKey: "oxygen_lpm") ?? ""
        usesBipap = d.object(forKey: "uses_bipap") == nil ? nil : d.bool(forKey: "uses_bipap")
        machineType = d.string(forKey: "bipap_machine_type") ?? ""
        bipapSetting = d.string(forKey: "bipap_setting") ?? ""
        activityLevel = d.string(forKey: "activity_level") ?? ""
        updateProteinTarget()
        DispatchQueue.main.async { suppressSaves = false }
    }

    private func touch() { lastUpdated = ProfileStore.touchLastUpdated() }

    private func setActivity(_ code: String) {
        activityLevel = code
        UserDefaults.standard.set(code, forKey: "activity_level")
        updateProteinTarget()
        touch()
    }

    /// ProfileFragment.updateProteinTarget (:442-516).
    private func updateProteinTarget() {
        let d = UserDefaults.standard
        guard let lbs = ProfileStore.get("weight").flatMap({ Double($0) }), lbs > 0 else {
            proteinTargetText = "-- g/day (set your weight first)"
            d.set(0.0, forKey: "protein_target")
            return
        }
        let kg = lbs / 2.205
        let level = d.string(forKey: "activity_level") ?? ""
        if level.isEmpty {
            proteinTargetText = "-- g/day (select your activity level above)"
            d.set(0.0, forKey: "protein_target")
            return
        }
        let low: Double, high: Double, note: String
        switch level {
        case "low": (low, high, note) = (1.0, 1.2, "")
        case "moderate", "pulmonary_rehab": (low, high, note) = (1.2, 1.4, "")
        case "high": (low, high, note) = (1.6, 1.8, "")
        case "exacerbation": (low, high, note) = (1.6, 1.8, " (COPD exacerbation)")
        case "kidney_disease": (low, high, note) = (1.0, 1.2, " (kidney disease)")
        case "dialysis": (low, high, note) = (1.2, 1.8, " (dialysis)")
        default: (low, high, note) = (1.2, 1.4, "")
        }
        let proteinLow = Int(kg * low)
        let proteinHigh = Int(kg * high)
        d.set(Double((proteinLow + proteinHigh) / 2), forKey: "protein_target")
        d.set(proteinLow, forKey: "protein_target_low")
        d.set(proteinHigh, forKey: "protein_target_high")
        proteinTargetText = "\(proteinLow) - \(proteinHigh) g/day\(note)"
    }

    /// editField (:612-647): empty value ignored; weight also inserts a current WeightEntry.
    private func saveEdited(_ prompt: EditPrompt) {
        let value = editValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty else { return }
        ProfileStore.set(value, key: prompt.key)
        touch()
        if prompt.key == "weight", let lbs = Double(value), lbs > 0 {
            dataManager.addWeightEntry(WeightEntry(weight: lbs, type: .current))
        }
        ProfileWeightSync.syncProfileAndDbWeight()
        load()
        ToastCenter.shared.show("\(prompt.title) updated successfully!")
    }

    private func openHeightPicker() {
        if let parsed = ProfileStore.parseHeightToFeetInches(height) {
            heightFeet = min(max(parsed.0, 3), 8)
            heightInches = min(max(parsed.1, 0), 11)
        } else {
            heightFeet = 5; heightInches = 6
        }
        showingHeightPicker = true
    }

    private func saveHeight() {
        ProfileStore.set("\(heightFeet)'\(heightInches)\"", key: "height")
        touch()
        showingHeightPicker = false
        load()
        ToastCenter.shared.show("Height updated successfully!")
    }

    private func open(_ urlString: String) {
        if let url = URL(string: urlString) { UIApplication.shared.open(url) }
    }

    // MARK: Share (setupShareButton / shareVia*)

    private func shareTapped() {
        guard store.isPremium else {
            ToastCenter.shared.show("Subscribe to Premium to share your health report.")
            showPaywall = true
            return
        }
        switch HipaaGate.requireConsent(action: "share your report") {
        case .granted:
            reportText = ReportGenerator().generateFullReport()
            showShareSheet = true
        case .needsConsent(let prompt):
            hipaaPrompt = prompt
        }
    }

    // MARK: Link doctor (setupLinkDoctor)

    private func linkDoctor() {
        guard hipaaStorage.hasValidConsent() else {
            ToastCenter.shared.show("You must complete the HIPAA Authorization before linking to a doctor.")
            showHipaa = true
            return
        }
        guard linkConsentChecked else {
            ToastCenter.shared.show("Please agree to share your data with the practice")
            return
        }
        isLinking = true
        Task {
            defer { Task { @MainActor in isLinking = false } }
            let token: String
            do { token = try await auth.getIdToken() } catch {
                await MainActor.run { ToastCenter.shared.show("Please sign in first") }
                return
            }
            do {
                try await COPDAPIClient.shared.consent(token: token)
            } catch {
                await MainActor.run { ToastCenter.shared.show(error.localizedDescription.isEmpty ? "Consent failed" : error.localizedDescription) }
                return
            }
            do {
                try await COPDAPIClient.shared.linkDoctor(token: token)
                await MainActor.run { ToastCenter.shared.show("Linked to doctor") }
            } catch {
                await MainActor.run { ToastCenter.shared.show(error.localizedDescription.isEmpty ? "Link failed" : error.localizedDescription) }
            }
        }
    }

    // MARK: Sign out / delete (setupSignOut, performDeleteAccount)

    private func signOut() {
        Task {
            do { try await auth.signOut() } catch {
                await MainActor.run { ToastCenter.shared.show(AuthService.userMessage(for: error)) }
            }
        }
    }

    private func performDeleteAccount() {
        isDeleting = true
        Task {
            defer { Task { @MainActor in isDeleting = false } }
            let token: String
            do { token = try await auth.getIdToken() } catch {
                await MainActor.run { ToastCenter.shared.show("Please sign in first") }
                return
            }
            do {
                try await COPDAPIClient.shared.deleteMe(token: token)
                try? await auth.deleteUser()   // Cognito user; server row already gone
                try? await auth.signOut()
                await MainActor.run { ToastCenter.shared.show("Account deleted") }
            } catch {
                await MainActor.run { ToastCenter.shared.show(error.localizedDescription.isEmpty ? "Delete failed" : error.localizedDescription) }
            }
        }
    }
}
```
`TrackingButton` comes from P3.C (1/2) `Views/Tracking/TrackingView.swift`. The `.alert` with `presenting:` + `TextField` is supported on iOS 16+.

---

### Task 4: `ReportGenerator` 1:1 port (audit B1–B14)

**Files:**
- Rewrite: `COPDFuel/COPDFuel/Services/ReportGenerator.swift` (keep the existing `stepsAndHeartRateSummary(steps:heartRates:)` function body verbatim as the last section; everything else replaced)

- [ ] **Step 1: Replace the file (retain `stepsAndHeartRateSummary` from the old file at the end)**

```swift
//
//  ReportGenerator.swift
//  COPDFuel
//
//  Line-for-line port of Android utils/ReportGenerator.kt. The only
//  addition is the iOS STEPS / HEART RATE section after Water (kept per
//  P3 spec §1.4).
//

import Foundation

struct ReportGenerator {
    private let dataManager = DataManager.shared
    private let defaults = UserDefaults.standard

    private let dateFormat: DateFormatter = { let f = DateFormatter(); f.dateFormat = "MMM d, yyyy"; return f }()
    private let dateTimeFormat: DateFormatter = { let f = DateFormatter(); f.dateFormat = "MMM d, yyyy 'at' h:mm a"; return f }()
    private let perDayFormat: DateFormatter = { let f = DateFormatter(); f.dateFormat = "EEE MMM d, yyyy"; return f }()
    private let calendar = Calendar.current

    func generateFullReport() -> String {
        var sb = ""
        sb += "========================================\n"
        sb += "       COPD FUEL REPORT\n"
        sb += "========================================\n"
        sb += "\n"
        sb += "Report Generated: \(dateTimeFormat.string(from: Date()))\n"
        sb += "\n"
        sb += profileSection()
        sb += severitySection()
        sb += exacerbationPlanSection()
        sb += medicationsSection()
        sb += trackingSection()
        sb += "\n"
        sb += "========================================\n"
        sb += "         END OF REPORT\n"
        sb += "========================================\n"
        sb += "\n"
        sb += "This report was generated by COPD Fuel for personal tracking.\n"
        sb += "It does not provide medical advice, diagnosis, or treatment recommendations.\n"
        sb += "If you have questions about your health, consult a licensed clinician.\n"
        return sb
    }

    private func pref(_ key: String) -> String? { defaults.string(forKey: key) }
    private func orNotSet(_ v: String?) -> String { (v == nil || v!.isEmpty) ? "Not set" : v! }

    // MARK: PATIENT PROFILE (Kotlin :61-180)

    private func profileSection() -> String {
        var sb = ""
        sb += "----------------------------------------\n"
        sb += "PATIENT PROFILE\n"
        sb += "----------------------------------------\n"
        sb += "\n"
        let age = pref("age"), sex = pref("sex"), weight = pref("weight"), height = pref("height"), lastUpdated = pref("last_updated")
        sb += "Age: \(age ?? "Not set")\n"
        sb += "Sex: \(sex ?? "Not set")\n"
        sb += "Weight: \((weight == nil || weight!.isEmpty) ? "Not set" : "\(weight!) lbs")\n"
        sb += "Height: \(height ?? "Not set")\n"
        if let weight, let height, !weight.isEmpty, !height.isEmpty {
            sb += "BMI: \(ProfileStore.bmiText(weight: weight, height: height) ?? "N/A")\n"
        }
        sb += "\n"
        sb += "Activity Level: \(ProfileStore.activityLabel(for: pref("activity_level") ?? ""))\n"
        let low = defaults.integer(forKey: "protein_target_low"), high = defaults.integer(forKey: "protein_target_high")
        if low > 0 && high > 0 { sb += "Daily Protein Target: \(low) - \(high) g/day\n" }
        sb += "\n"
        sb += "Health Conditions:\n"
        let kidney = defaults.bool(forKey: "condition_kidney_disease")
        let chol = defaults.bool(forKey: "condition_high_cholesterol")
        let ph = defaults.bool(forKey: "condition_pulmonary_hypertension")
        let dialysis = defaults.bool(forKey: "condition_dialysis")
        let none = defaults.bool(forKey: "condition_none")
        let other = defaults.bool(forKey: "condition_other")
        let otherText = pref("condition_other_text") ?? ""
        if none {
            sb += "  - None reported\n"
        } else {
            if kidney { sb += "  - Kidney disease\n" }
            if chol { sb += "  - High cholesterol\n" }
            if ph { sb += "  - Pulmonary hypertension\n" }
            if dialysis { sb += "  - Currently on dialysis\n" }
            if other && !otherText.isEmpty { sb += "  - Other: \(otherText)\n" }
            if !kidney && !chol && !ph && !dialysis && !other { sb += "  - None specified\n" }
        }
        sb += "\n"
        let usesOxygen = defaults.bool(forKey: "uses_oxygen")
        sb += "Supplemental Oxygen: \(usesOxygen ? "Yes" : "No")\n"
        if usesOxygen, let lpm = pref("oxygen_lpm"), !lpm.isEmpty { sb += "  LPM: \(lpm)\n" }
        let usesBipap = defaults.bool(forKey: "uses_bipap")
        sb += "BIPAP/NIV: \(usesBipap ? "Yes" : "No")\n"
        if usesBipap {
            if let m = pref("bipap_machine_type"), !m.isEmpty { sb += "  Machine Type: \(m)\n" }
            if let s = pref("bipap_setting"), !s.isEmpty { sb += "  Setting: \(s)\n" }
        }
        sb += "\n"
        sb += "Healthcare Contacts:\n"
        sb += "  Doctor: \(orNotSet(pref("doctor_name")))\n"
        sb += "  Doctor Phone: \(orNotSet(pref("doctor_phone")))\n"
        sb += "  Emergency Contact: \(orNotSet(pref("emergency_contact_name")))\n"
        sb += "  Emergency Phone: \(orNotSet(pref("emergency_contact_phone")))\n"
        sb += "  Second Emergency Contact: \(orNotSet(pref("emergency_contact2_name")))\n"
        sb += "  Second Emergency Phone: \(orNotSet(pref("emergency_contact2_phone")))\n"
        sb += "  Insurance Provider: \(orNotSet(pref("insurance_provider")))\n"
        sb += "\n"
        if let lastUpdated, !lastUpdated.isEmpty { sb += "Profile Last Updated: \(lastUpdated)\n" }
        sb += "\n"
        return sb
    }

    // MARK: TRACKING SUMMARY (:182-224)

    private func severitySection() -> String {
        var sb = ""
        sb += "----------------------------------------\n"
        sb += "TRACKING SUMMARY\n"
        sb += "----------------------------------------\n"
        sb += "\n"
        if let result = pref("severity_result") {
            sb += "Saved Summary:\n"
            sb += "  FEV1 Percentage: \(pref("severity_fev1") ?? "Unknown")\n"
            sb += "  Hospitalizations (past year): \(pref("severity_hospitalizations") ?? "Unknown")\n"
            sb += "  Exacerbations (past year): \(pref("severity_exacerbations") ?? "Unknown")\n"
            sb += "  Uses Supplemental Oxygen: \(pref("severity_oxygen") ?? "Unknown")\n"
            sb += "\n"
            sb += "Note: \(result)\n"
            if let d = pref("severity_description"), !d.isEmpty { sb += "\nDetails: \(d)\n" }
            if let d = pref("severity_assessment_date"), !d.isEmpty { sb += "\nSaved On: \(d)\n" }
        } else {
            sb += "No tracking summary has been saved.\n"
            sb += "You can save a summary in the Resources tab for your personal tracking.\n"
        }
        sb += "\n"
        sb += "Note: This content is for personal tracking only and is not medical advice.\n"
        sb += "\n"
        return sb
    }

    // MARK: CARE PLAN NOTES (:226-251) — iOS uses the instructions log (audit B4)

    private func exacerbationPlanSection() -> String {
        var sb = ""
        sb += "----------------------------------------\n"
        sb += "CARE PLAN NOTES (TEMPLATE)\n"
        sb += "----------------------------------------\n"
        sb += "\n"
        sb += "Use this section to write down a plan and questions to discuss with your clinician.\n"
        sb += "This template is not medical advice and is not a substitute for professional care.\n"
        sb += "\n"
        let notes = DoctorInstructionsStore.load().sorted { $0.savedAt > $1.savedAt }.map { $0.text }
        if !notes.isEmpty {
            sb += "Saved Notes:\n"
            sb += notes.joined(separator: "\n") + "\n"
            sb += "\n"
        } else {
            sb += "No notes saved.\n"
            sb += "\n"
        }
        return sb
    }

    // MARK: MEDICATIONS (:253-290)

    private func medicationsSection() -> String {
        var sb = ""
        sb += "----------------------------------------\n"
        sb += "MEDICATIONS\n"
        sb += "----------------------------------------\n"
        sb += "\n"
        sb += "Daily Medications:\n"
        sb += medicationLines(dataManager.getDailyMedications(), empty: "  No daily medications added\n")
        sb += "\n"
        sb += "Exacerbation Medications:\n"
        sb += medicationLines(dataManager.getExacerbationMedications(), empty: "  No exacerbation medications added\n")
        sb += "\n"
        return sb
    }

    private func medicationLines(_ meds: [Medication], empty: String) -> String {
        if meds.isEmpty { return empty }
        return meds.map { "  - \($0.name)\n    Dosage: \($0.dosage)\n    Frequency: \($0.frequency)\n" }.joined()
    }

    // MARK: TRACKING DATA (:292-332)

    private func trackingSection() -> String {
        var sb = ""
        sb += "----------------------------------------\n"
        sb += "TRACKING DATA (Last 30 Days)\n"
        sb += "----------------------------------------\n"
        sb += "\n"
        let end = Date()
        let start = calendar.startOfDay(for: calendar.date(byAdding: .day, value: -30, to: end) ?? end)
        sb += "Period: \(dateFormat.string(from: start)) - \(dateFormat.string(from: end))\n"
        sb += "\n"
        sb += nutritionSummary(start, end)
        sb += exerciseSummary(start, end)
        sb += exerciseJournalSection(start, end)
        sb += oxygenSummary(start, end)
        sb += weightSummary(start, end)
        sb += waterSummary(start, end)
        sb += stepsAndHeartRateSummary(
            steps: dataManager.getStepsEntries(from: start, to: end),
            heartRates: dataManager.getHeartRateEntries(from: start, to: end))
        return sb
    }

    private func f0(_ v: Double) -> String { String(format: "%.0f", v) }
    private func f1(_ v: Double) -> String { String(format: "%.1f", v) }
    private func dayKey(_ d: Date) -> Date { calendar.startOfDay(for: d) }

    // MARK: NUTRITION SUMMARY (:334-426)

    private func nutritionSummary(_ start: Date, _ end: Date) -> String {
        var sb = "NUTRITION SUMMARY:\n"
        let foods = dataManager.getFoodEntries(from: start, to: end)
        if foods.isEmpty {
            sb += "  No food entries recorded in this period.\n\n"
            return sb
        }
        let byDay = Dictionary(grouping: foods) { dayKey($0.date) }
        let daysLogged = Double(byDay.count)
        let totalCal = foods.reduce(0.0) { $0 + ($1.calories ?? 0) }
        let totalP = foods.reduce(0.0) { $0 + ($1.protein ?? 0) }
        let totalC = foods.reduce(0.0) { $0 + ($1.carbs ?? 0) }
        let totalF = foods.reduce(0.0) { $0 + ($1.fat ?? 0) }
        let totalFiber = foods.reduce(0.0) { $0 + $1.fiber }
        let totalSodium = foods.reduce(0.0) { $0 + $1.sodium }
        let totalPotassium = foods.reduce(0.0) { $0 + $1.potassium }
        let totalWater = foods.reduce(0.0) { $0 + $1.water }
        sb += "  Days Logged: \(byDay.count)\n"
        sb += "  Total Entries: \(foods.count)\n"
        sb += "\n"
        sb += "  Daily Averages:\n"
        sb += "    Calories: \(f0(totalCal / daysLogged)) kcal\n"
        sb += "    Protein: \(f1(totalP / daysLogged)) g\n"
        sb += "    Carbohydrates: \(f1(totalC / daysLogged)) g\n"
        sb += "    Fat: \(f1(totalF / daysLogged)) g\n"
        sb += "    Fiber: \(f1(totalFiber / daysLogged)) g\n"
        sb += "    Sodium: \(f0(totalSodium / daysLogged)) mg\n"
        sb += "    Potassium: \(f0(totalPotassium / daysLogged)) mg\n"
        sb += "    Water: \(f1(totalWater / daysLogged)) oz\n"
        sb += "\n"
        let target = defaults.double(forKey: "protein_target")
        if target > 0 {
            let avg = totalP / daysLogged
            sb += "  Protein Target Progress:\n"
            sb += "    Daily Target: \(Int(target)) g\n"
            sb += "    Average Intake: \(f1(avg)) g\n"
            sb += "    Achievement: \(f0(avg / target * 100))% of target\n"
            sb += "\n"
        }
        sb += "  Individual Food Entries:\n"
        for day in byDay.keys.sorted(by: >) {
            sb += "    \(perDayFormat.string(from: day)):\n"
            for f in (byDay[day] ?? []).sorted(by: { $0.date < $1.date }) {
                let meal = f.mealType.trimmingCharacters(in: .whitespaces).isEmpty ? "" : " [\(f.mealType)]"
                let qty = f.quantity.trimmingCharacters(in: .whitespaces).isEmpty ? "" : " (\(f.quantity))"
                sb += "      - \(f.foodName)\(meal)\(qty)\n"
                sb += "        \(f0(f.calories ?? 0)) cal, P \(f1(f.protein ?? 0))g, C \(f1(f.carbs ?? 0))g, F \(f1(f.fat ?? 0))g\n"
            }
        }
        sb += "\n"
        return sb
    }

    // MARK: EXERCISE SUMMARY (:428-480)

    private func exerciseSummary(_ start: Date, _ end: Date) -> String {
        var sb = "EXERCISE SUMMARY:\n"
        let exercises = dataManager.getExerciseEntries(from: start, to: end)
        if exercises.isEmpty {
            sb += "  No exercise entries recorded in this period.\n\n"
            return sb
        }
        let totalMinutes = exercises.reduce(0) { $0 + $1.minutes }
        let daysExercised = Set(exercises.map { dayKey($0.date) }).count
        sb += "  Total Exercise Sessions: \(exercises.count)\n"
        sb += "  Days with Exercise: \(daysExercised)\n"
        sb += "  Total Minutes: \(totalMinutes)\n"
        sb += "  Average per Session: \(f1(Double(totalMinutes) / Double(exercises.count))) minutes\n"
        sb += "\n"
        sb += "  By Exercise Type:\n"
        var typeOrder: [String] = []
        var byType: [String: [ExerciseEntry]] = [:]
        for e in exercises {
            if byType[e.exerciseType] == nil { typeOrder.append(e.exerciseType) }
            byType[e.exerciseType, default: []].append(e)
        }
        for type in typeOrder {
            let entries = byType[type] ?? []
            sb += "    \(type): \(entries.count) sessions, \(entries.reduce(0) { $0 + $1.minutes }) minutes total\n"
        }
        sb += "\n"
        sb += "  Individual Exercise Sessions:\n"
        let byDay = Dictionary(grouping: exercises) { dayKey($0.date) }
        for day in byDay.keys.sorted(by: >) {
            sb += "    \(perDayFormat.string(from: day)):\n"
            for e in (byDay[day] ?? []).sorted(by: { $0.date < $1.date }) {
                sb += "      - \(e.exerciseType): \(e.minutes) min\n"
            }
        }
        sb += "\n"
        return sb
    }

    // MARK: EXERCISE JOURNAL (:529-564)

    private func exerciseJournalSection(_ start: Date, _ end: Date) -> String {
        var sb = "EXERCISE JOURNAL:\n"
        let window = ExerciseJournalStore.load().filter { $0.day >= start && $0.day < end }
        if window.isEmpty {
            sb += "  No exercise journal entries recorded in this period.\n\n"
            return sb
        }
        let byDay = Dictionary(grouping: window) { dayKey($0.day) }
        for day in byDay.keys.sorted(by: >) {
            sb += "  \(perDayFormat.string(from: day)):\n"
            for e in (byDay[day] ?? []).sorted(by: { $0.savedAt < $1.savedAt }) {
                let time = e.timeText.trimmingCharacters(in: .whitespaces).isEmpty ? "" : " @ \(e.timeText)"
                let name = e.exercise.trimmingCharacters(in: .whitespaces).isEmpty ? "(unnamed)" : e.exercise
                sb += "    - \(name)\(time)\n"
                if e.sets > 0 || e.reps > 0 { sb += "      Sets: \(e.sets), Reps: \(e.reps)\n" }
                if e.weight > 0 { sb += "      Weight: \(f1(e.weight)) \(e.weightUnit)\n" }
                if !e.activity.trimmingCharacters(in: .whitespaces).isEmpty { sb += "      Activity: \(e.activity)\n" }
                if !e.warmUp.trimmingCharacters(in: .whitespaces).isEmpty { sb += "      Warm-up: \(e.warmUp)\n" }
            }
        }
        sb += "\n"
        return sb
    }

    // MARK: OXYGEN (:566-602)

    private func oxygenSummary(_ start: Date, _ end: Date) -> String {
        var sb = "OXYGEN READINGS SUMMARY:\n"
        let readings = dataManager.getOxygenReadings(from: start, to: end)
        if readings.isEmpty {
            sb += "  No oxygen readings recorded in this period.\n\n"
            return sb
        }
        let levels = readings.map { $0.oxygenLevel }
        let avg = Double(levels.reduce(0, +)) / Double(levels.count)
        sb += "  Total Readings: \(readings.count)\n"
        sb += "  Average Level: \(f1(avg))%\n"
        sb += "  Minimum Level: \(levels.min() ?? 0)%\n"
        sb += "  Maximum Level: \(levels.max() ?? 0)%\n"
        sb += "\n"
        sb += "  Distribution:\n"
        sb += "    Below 88% (Critical): \(levels.filter { $0 < 88 }.count) readings\n"
        sb += "    88-92% (Low Normal): \(levels.filter { (88...92).contains($0) }.count) readings\n"
        sb += "    Above 92% (Normal): \(levels.filter { $0 > 92 }.count) readings\n"
        sb += "\n"
        return sb
    }

    // MARK: WEIGHT (:604-638)

    private func weightSummary(_ start: Date, _ end: Date) -> String {
        var sb = "WEIGHT SUMMARY:\n"
        let weights = dataManager.weightEntries
            .filter { $0.type == .current && $0.date >= start && $0.date <= end }
            .sorted { $0.date < $1.date }
        if weights.isEmpty {
            sb += "  No weight entries recorded in this period.\n\n"
            return sb
        }
        let startW = weights.first!.weight, endW = weights.last!.weight
        let change = endW - startW
        sb += "  Entries Recorded: \(weights.count)\n"
        sb += "  Starting Weight: \(f1(startW)) lbs\n"
        sb += "  Current Weight: \(f1(endW)) lbs\n"
        sb += "  Change: \(change >= 0 ? "+" : "")\(f1(change)) lbs\n"
        if let goal = dataManager.getGoalWeight() {
            let toGoal = endW - goal
            sb += "  Goal Weight: \(f1(goal)) lbs\n"
            sb += "  Distance to Goal: \(f1(abs(toGoal))) lbs \(toGoal > 0 ? "to lose" : (toGoal < 0 ? "to gain" : "(at goal)"))\n"
        }
        sb += "\n"
        return sb
    }

    // MARK: WATER (:640-678)

    private func waterSummary(_ start: Date, _ end: Date) -> String {
        var sb = "WATER INTAKE SUMMARY:\n"
        let entries = dataManager.waterEntries.filter { $0.date >= start && $0.date <= end }
        if entries.isEmpty {
            sb += "  No water intake recorded in this period.\n\n"
            return sb
        }
        let byDay = Dictionary(grouping: entries) { dayKey($0.date) }
        let daysTracked = byDay.count
        let totalOz = entries.reduce(0) { $0 + $1.amount }
        let goal = 64
        let daysMet = byDay.values.filter { $0.reduce(0) { $0 + $1.amount } >= goal }.count
        sb += "  Days Tracked: \(daysTracked)\n"
        sb += "  Total Intake: \(totalOz) oz\n"
        sb += "  Daily Average: \(daysTracked > 0 ? totalOz / daysTracked : 0) oz\n"
        sb += "  Daily Goal: \(goal) oz\n"
        sb += "  Days Goal Met: \(daysMet) / \(daysTracked)\n"
        sb += "\n"
        return sb
    }

    // MARK: STEPS / HEART RATE — iOS-only, kept (spec §1.4)

    // (paste the existing `private func stepsAndHeartRateSummary(steps:heartRates:) -> String` here unchanged)
}
```

- [ ] **Step 2: Build**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -derivedDataPath "$HOME/Library/Developer/Xcode/DerivedData/COPDFuel-cli" -quiet build 2>&1 | grep -E "error:" | head -20; echo "exit=${PIPESTATUS[0]}"
```
Expected `exit=0`. Likely first-pass issue: `getOxygenReadings(from:to:)` exists; `getStepsEntries(from:to:)` exists; if `RootView` referenced `dataManager.profile` (it does not), fix it.

---

### Task 5: Checks, commit

- [ ] **Step 1: Copy checks**

```bash
for s in "My Profile" "Body Mass Index (BMI)" "Which option best describes your current activity level?" "Your Daily Protein Target" "-- g/day (set your weight first)" "-- g/day (select your activity level above)" "Health Conditions with COPD" "Please check all that apply (affects protein target):" "Do you use supplemental oxygen?" "What is your LPM?" "Are you on a BIPAP or NIV?" "Which machine?" "What is your setting?" "HIPAA Authorization (PHI Disclosure)" "Share My Health Report" "Subscribe to Premium to share your health report." "I agree to share my health data with this practice." "Please agree to share your data with the practice" "Linked to doctor" "Delete my account" "You can also request deletion by emailing support@copdfuel.com" "Account deleted" "updated successfully!" "Edit Height"; do
  a=$(grep -rl -F "$s" android/app/src/main/java/com/copdhealthtracker android/app/src/main/res | wc -l | tr -d ' ')
  i=$(grep -c -F "$s" COPDFuel/COPDFuel/Views/ProfileView.swift)
  echo "$a android / $i ios  <- $s"
done
for s in "PATIENT PROFILE" "Second Emergency Contact:" "Insurance Provider:" "Profile Last Updated:" "TRACKING SUMMARY" "No tracking summary has been saved." "CARE PLAN NOTES (TEMPLATE)" "Saved Notes:" "No daily medications added" "TRACKING DATA (Last 30 Days)" "Individual Food Entries:" "By Exercise Type:" "EXERCISE JOURNAL:" "Warm-up:" "OXYGEN READINGS SUMMARY:" "Below 88% (Critical):" "WEIGHT SUMMARY:" "Distance to Goal:" "WATER INTAKE SUMMARY:" "Days Goal Met:" "END OF REPORT"; do
  a=$(grep -c -F "$s" android/app/src/main/java/com/copdhealthtracker/utils/ReportGenerator.kt)
  i=$(grep -c -F "$s" COPDFuel/COPDFuel/Services/ReportGenerator.swift)
  echo "$a android / $i ios  <- $s"
done
```
Expected: non-zero on both sides for every row.

- [ ] **Step 2: Removal checks**

```bash
grep -rn "UserProfile\|profile_weight\|goalWeight\|LinkDoctorView\|inviteCode\|Goal Weight (lbs)" COPDFuel/COPDFuel --include='*.swift' | grep -v "ProfileStore.swift"
```
Expected: no output (the migration in `ProfileStore.swift` is the only remaining mention).

- [ ] **Step 3: Commit**

```bash
cd COPDFuel && /usr/bin/git add -A && /usr/bin/git commit -q -m "iOS parity P3.E: Profile rebuilt to Android (edit dialogs, BMI/Last Updated, radio activity levels, oxygen/BIPAP, premium/legal/HIPAA rows, share + link-doctor cards, delete account), flat profile keys with migration, 1:1 report generator, API client endpoints

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>" && /usr/bin/git log --oneline -1
```

- [ ] **Step 4: Manual smoke (simulator)**

Profile: tapping Age opens "Edit Age" with the "Enter Age" field; saving toasts "Age updated successfully!" and updates "Last Updated"; Weight save also shows in Tracking's Weight card; Height opens the wheel sheet defaulting 5'6"; choosing "Currently on dialysis" shows the "(dialysis)" note in the protein target; "Share My Health Report" without Premium toasts and opens the paywall; "Link to doctor" without HIPAA toasts and opens the HIPAA sheet; "Delete my account" shows the Android confirmation copy; generated report text starts with "COPD FUEL REPORT" and contains "Second Emergency Contact:".
