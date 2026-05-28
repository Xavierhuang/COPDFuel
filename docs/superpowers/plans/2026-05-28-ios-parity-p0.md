# iOS Parity — Phase P0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the iOS-side HIPAA/PHI compliance gaps and expand the iOS food data model to match Android's 23-nutrient schema. This is the foundation for P1 (Tracking, Report, share-feature work).

**Architecture:** Add a single `HipaaGate` service that wraps `HipaaConsentStorage` with imperative + declarative consent checks. Gate `RootView`'s backend sync and `ProfileView`'s Share Report through it. Persist revocation date and render a distinct "Revoked" UI state. Expand `FoodEntry`, `FavoriteFood`, `FavoriteMeal`/`FavoriteMealItem`, `UserAddedFood`, and `FoodSearchResult` with the 19 additional nutrient fields Android tracks; backfill missing keys to `0.0` so old persisted data still decodes.

**Tech Stack:** SwiftUI, Swift `Codable`, `UserDefaults`. Verification via `xcodebuild -scheme COPDFuel build` (no existing XCTest target; logic is verified by build success + manual simulator smoke tests at the phase boundary).

**Source spec:** `docs/superpowers/specs/2026-05-28-ios-android-parity-design.md` § P0.

**Working directory for all file paths:** `/Users/weijiahuang/Desktop/client's project/COPD-2`

---

## File Structure (created/modified during P0)

| File | Action | Responsibility |
|------|--------|----------------|
| `COPDFuel/COPDFuel/Services/HipaaGate.swift` | **Create** | Centralized HIPAA consent gate — silent check + interactive prompt builder. |
| `COPDFuel/COPDFuel/Services/HipaaConsentStorage.swift` | Modify | Add `revokedDate` persistence and `getRevokedDate()` accessor. |
| `COPDFuel/COPDFuel/Views/HipaaAuthorizationView.swift` | Modify | Render distinct revoked-state banner above the resign form. |
| `COPDFuel/COPDFuel/Views/RootView.swift` | Modify | Skip `registerAndSyncIfNeeded` if HIPAA not signed; re-run on consent change. |
| `COPDFuel/COPDFuel/Views/ProfileView.swift` | Modify | Gate Share Report through `HipaaGate`. |
| `COPDFuel/COPDFuel/Models/FoodEntry.swift` | Modify | Add 19 nutrient fields (defaulted to `0.0`). |
| `COPDFuel/COPDFuel/Models/FavoriteFood.swift` | Modify | Same — expand to 23-nutrient schema; update `toFoodEntry()`. |
| `COPDFuel/COPDFuel/Models/FavoriteMeal.swift` | Modify | Expand `FavoriteMealItem` to 23 nutrients; recompute totals. |
| `COPDFuel/COPDFuel/Models/UserAddedFood.swift` | Modify | Add the 16 micronutrient + extended fat/sugar fields. |
| `COPDFuel/COPDFuel/Models/FoodSearchResult.swift` | Modify | Add the 16 fields + `servingSizes` placeholder for P1. |
| `COPDFuel/COPDFuel/Views/TrackingView.swift` | Modify | Pass all 23 nutrients from search-result → entry, and on manual-entry save. |

---

## Pre-flight

- [ ] **Step 0.1: Confirm we're at the latest design-doc commit on this branch.**

Run:
```bash
git log --oneline -1 -- docs/superpowers/specs/2026-05-28-ios-android-parity-design.md
```
Expected: shows commit `625ee5b Add iOS-Android parity design doc` (or later).

- [ ] **Step 0.2: Baseline build to confirm the project compiles before any edits.**

Run:
```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet build 2>&1 | tail -20
```
Expected: `** BUILD SUCCEEDED **`. If it fails, stop and investigate before continuing — every later task uses this as its pass-gate.

---

## Task 1: Add `HipaaGate` service

**Files:**
- Create: `COPDFuel/COPDFuel/Services/HipaaGate.swift`

- [ ] **Step 1.1: Create `HipaaGate.swift` with the full implementation.**

```swift
//
//  HipaaGate.swift
//  COPDFuel
//
//  Central HIPAA consent gate. Mirrors Android utils/HipaaGate.kt:
//    - hasConsent(): silent check; safe to call from any thread
//    - requireConsent(action:): if signed, returns .granted; otherwise
//      returns .needsConsent so the caller can present the standard prompt
//
//  Call sites that need to interactively prompt the user use the
//  `HipaaPromptState` value to drive a SwiftUI alert + sheet (see
//  ProfileView.swift for the share-report example).
//

import Foundation
import SwiftUI

@MainActor
enum HipaaGate {
    /// Silent check — true iff the user has a current, non-revoked HIPAA consent on file.
    static func hasConsent() -> Bool {
        HipaaConsentStorage.shared.hasValidConsent()
    }

    /// Returns `.granted` when consent is on file, or `.needsConsent` carrying
    /// the verb to render in the prompt body (e.g. "share your report",
    /// "link to a doctor"). Mirrors the Android `requireConsent(context, action)`
    /// contract; the iOS variant is non-presenting so SwiftUI call sites stay
    /// declarative.
    static func requireConsent(action: String) -> RequireConsentResult {
        if hasConsent() {
            return .granted
        }
        return .needsConsent(HipaaPromptState(action: action))
    }
}

enum RequireConsentResult {
    case granted
    case needsConsent(HipaaPromptState)
}

/// Drives the standard "HIPAA Authorization Required" alert. Bind an
/// `@State var hipaaPrompt: HipaaPromptState?` in any view that gates an
/// action, then attach `.hipaaConsentAlert($hipaaPrompt)`.
struct HipaaPromptState: Identifiable, Equatable {
    let id = UUID()
    let action: String

    var title: String { "HIPAA Authorization Required" }
    var message: String { "You must sign the HIPAA authorization form before you can \(action)." }
}

extension View {
    /// Standard SwiftUI presentation for a HipaaPromptState.
    /// "Sign Now" opens HipaaAuthorizationView in a sheet; "Cancel" clears the binding.
    func hipaaConsentAlert(_ prompt: Binding<HipaaPromptState?>) -> some View {
        modifier(HipaaConsentAlertModifier(prompt: prompt))
    }
}

private struct HipaaConsentAlertModifier: ViewModifier {
    @Binding var prompt: HipaaPromptState?
    @State private var showSignSheet = false

    func body(content: Content) -> some View {
        content
            .alert(
                prompt?.title ?? "HIPAA Authorization Required",
                isPresented: Binding(
                    get: { prompt != nil },
                    set: { if !$0 { prompt = nil } }
                ),
                presenting: prompt
            ) { _ in
                Button("Sign Now") {
                    prompt = nil
                    showSignSheet = true
                }
                Button("Cancel", role: .cancel) {
                    prompt = nil
                }
            } message: { state in
                Text(state.message)
            }
            .sheet(isPresented: $showSignSheet) {
                HipaaAuthorizationView()
            }
    }
}
```

- [ ] **Step 1.2: Add the new file to the Xcode project target.**

Open `COPDFuel/COPDFuel.xcodeproj` in Xcode. In the Project Navigator, right-click the `Services` group → "Add Files to COPDFuel…", select `HipaaGate.swift`, ensure the `COPDFuel` target's checkbox is ticked, click Add.

(Alternative without opening Xcode: edit `COPDFuel/COPDFuel.xcodeproj/project.pbxproj` to add the file reference + build-file entry. Adding via Xcode UI is reliable and fast.)

- [ ] **Step 1.3: Build to verify the new file compiles cleanly.**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet build 2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`. If the build fails with "Cannot find 'HipaaGate' in scope" or similar, the file wasn't added to the target — return to 1.2.

- [ ] **Step 1.4: Commit.**

```bash
git add "COPDFuel/COPDFuel/Services/HipaaGate.swift" "COPDFuel/COPDFuel.xcodeproj/project.pbxproj"
git commit -m "iOS: add HipaaGate central consent helper

Mirrors Android utils/HipaaGate.kt with two entry points: silent
hasConsent() and a SwiftUI-declarative requireConsent(action:) that
returns a HipaaPromptState the caller renders via the
.hipaaConsentAlert(...) modifier. Sign Now opens HipaaAuthorizationView
in a sheet."
```

---

## Task 2: Persist HIPAA revocation date + accessor

**Files:**
- Modify: `COPDFuel/COPDFuel/Services/HipaaConsentStorage.swift`

- [ ] **Step 2.1: Add a `consentRevokedDateKey` constant and a `getRevokedDate()` accessor; stamp the date in `revokeConsent()`.**

Edit `COPDFuel/COPDFuel/Services/HipaaConsentStorage.swift`:

Replace the existing private keys block (lines 14–20) with:
```swift
    private let consentSignedKey = "hipaa_consent_signed"
    private let consentDateKey = "hipaa_consent_date"
    private let consentRevokedKey = "hipaa_consent_revoked"
    private let consentRevokedDateKey = "hipaa_consent_revoked_date"
    private let consentNameKey = "hipaa_consent_name"
    private let consentDobKey = "hipaa_consent_dob"
    private let consentExpiryTypeKey = "hipaa_consent_expiry_type"
    private let consentExpiryDateKey = "hipaa_consent_expiry_date"
```

Add this method directly after `getConsentDate()` (line 59):
```swift
    func getRevokedDate() -> Date? {
        guard isRevoked() else { return nil }
        let millis = UserDefaults.standard.double(forKey: consentRevokedDateKey)
        return millis > 0 ? Date(timeIntervalSince1970: millis) : nil
    }
```

Replace the existing `revokeConsent()` (lines 74–78) with:
```swift
    func revokeConsent() {
        UserDefaults.standard.set(false, forKey: consentSignedKey)
        UserDefaults.standard.set(true, forKey: consentRevokedKey)
        UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: consentRevokedDateKey)
        refreshStatus()
    }
```

- [ ] **Step 2.2: Build.**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet build 2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 2.3: Commit.**

```bash
git add "COPDFuel/COPDFuel/Services/HipaaConsentStorage.swift"
git commit -m "iOS HIPAA: persist revocation date

Adds hipaa_consent_revoked_date pref and getRevokedDate() accessor.
revokeConsent() now stamps Date().timeIntervalSince1970 so the
authorization screen can render \"Revoked on <date>\" matching
Android's HipaaConsentStorage.kt."
```

---

## Task 3: Render distinct "Revoked" state in `HipaaAuthorizationView`

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/HipaaAuthorizationView.swift`

The current view branches on `hasValidConsent()` vs not-signed. Add a revoked banner shown above the resign form when `isRevoked()` is true.

- [ ] **Step 3.1: Read the file to locate the existing branch point.**

```bash
grep -n "hasValidConsent\|isRevoked\|consentStatus" "COPDFuel/COPDFuel/Views/HipaaAuthorizationView.swift"
```

Expected: shows the conditional that currently renders signed-state vs form. (The audit identified this around line 43–49.)

- [ ] **Step 3.2: Add a `revokedBanner` computed view and render it above the form in the revoked branch.**

In `HipaaAuthorizationView.swift`, locate the existing body branch that decides between "Authorization Signed" view and the entry form (the place that uses `hasValidConsent()`). Update the form branch to first inspect `isRevoked()` and prepend a banner.

Insert this computed view inside the view's body scope (above any existing computed sub-views):

```swift
    @ViewBuilder
    private var revokedBanner: some View {
        if HipaaConsentStorage.shared.isRevoked(),
           let date = HipaaConsentStorage.shared.getRevokedDate() {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundColor(.orange)
                    .font(.title3)
                VStack(alignment: .leading, spacing: 4) {
                    Text("Authorization revoked")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Text("Revoked on \(date.formatted(date: .abbreviated, time: .omitted)). You may sign again below to re-authorize.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
            }
            .padding(12)
            .background(Color.orange.opacity(0.12))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(Color.orange.opacity(0.4), lineWidth: 1)
            )
            .cornerRadius(10)
            .padding(.horizontal)
            .padding(.top, 8)
        }
    }
```

Then in the form-rendering branch (the one shown when `!hasValidConsent()`), insert `revokedBanner` as the **first** child of its top-level `VStack`/`Form`/`ScrollView`. (The exact insertion point depends on the surrounding structure; place it where it will render above the "Date:" / legalese block.)

- [ ] **Step 3.3: Build.**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet build 2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 3.4: Commit.**

```bash
git add "COPDFuel/COPDFuel/Views/HipaaAuthorizationView.swift"
git commit -m "iOS HIPAA: render revoked-state banner

When the user has revoked a prior consent and reopens the HIPAA
authorization view, show an orange banner with the revocation date
above the resign form. Matches Android's three-state rendering
(signed / revoked / not signed)."
```

---

## Task 4: Gate backend PHI sync behind HIPAA consent

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/RootView.swift`

The current `registerAndSyncIfNeeded()` (lines 56–73) unconditionally pushes weights/meds/oxygen/exercise/water/foods to the backend on sign-in. Android gates this at `MainActivity.kt:72-77`.

- [ ] **Step 4.1: Add a HIPAA-consent gate at the top of `registerAndSyncIfNeeded` and observe consent changes to retry sync.**

Edit `COPDFuel/COPDFuel/Views/RootView.swift`. Replace the existing struct body with:

```swift
struct RootView: View {
    @ObservedObject private var auth = AuthService.shared
    @ObservedObject private var hipaaStorage = HipaaConsentStorage.shared

    var body: some View {
        Group {
            if auth.isLoading {
                splashContent
            } else if auth.isSignedIn {
                MainTabView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .onAppear { Task { await registerAndSyncIfNeeded() } }
                    .onChange(of: hipaaStorage.consentStatus) { _, newStatus in
                        // When the user signs HIPAA after sign-in, run the
                        // sync we deferred. Mirrors Android's
                        // MainActivity behavior where post-consent sync
                        // happens on activity resume.
                        if case .signed = newStatus {
                            Task { await registerAndSyncIfNeeded() }
                        }
                    }
            } else {
                NavigationStack {
                    LoginView()
                }
            }
        }
    }

    private var splashContent: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [Color(hex: "ffffff"), Color(hex: "f8fafc")]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            VStack(spacing: 20) {
                HStack(spacing: 0) {
                    Text("COPD")
                        .font(.system(size: 56, weight: .bold))
                        .foregroundColor(Color(hex: "f97316"))
                    Text(" Fuel")
                        .font(.system(size: 56, weight: .bold))
                        .foregroundColor(Color(hex: "2563eb"))
                }
                Text("Your COPD Health Companion")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(Color(hex: "6b7280"))
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: Color(hex: "2563eb")))
                    .padding(.top, 24)
            }
        }
    }

    private func registerAndSyncIfNeeded() async {
        // HIPAA gate: do not transmit any PHI to the backend until the
        // user has signed the HIPAA Authorization. Mirrors Android
        // MainActivity.kt:72-77.
        guard await MainActor.run(body: { HipaaGate.hasConsent() }) else {
            return
        }
        do {
            let token = try await auth.getIdToken()
            try await COPDAPIClient.shared.putMe(token: token)
            let dm = DataManager.shared
            try await COPDAPIClient.shared.sync(
                token: token,
                weights: dm.weightEntries,
                medications: dm.medications,
                oxygen: dm.oxygenReadings,
                exercises: dm.exerciseEntries,
                water: dm.waterEntries,
                foods: dm.foodEntries
            )
        } catch {
            // Non-fatal; user can retry by reopening app
        }
    }
}
```

- [ ] **Step 4.2: Build.**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet build 2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`. If you see an error about `consentStatus` not conforming to `Equatable`, that's fine — `HipaaConsentStorage.ConsentStatus` already conforms (`enum ConsentStatus: Equatable` on line 24 of HipaaConsentStorage.swift).

- [ ] **Step 4.3: Commit.**

```bash
git add "COPDFuel/COPDFuel/Views/RootView.swift"
git commit -m "iOS RootView: gate backend PHI sync behind HIPAA consent

registerAndSyncIfNeeded now returns early when HipaaGate.hasConsent()
is false. The view observes HipaaConsentStorage.consentStatus and
retries the sync the moment the user signs HIPAA, matching the
Android MainActivity post-consent retry pattern."
```

---

## Task 5: Gate Share Report behind HIPAA consent

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/ProfileView.swift`

The current `shareReportOrShowPaywall()` and `shareReport()` (around lines 442–454) gate on premium only. Android additionally gates every share path with `HipaaGate.requireConsent(...)`.

- [ ] **Step 5.1: Add a `@State private var hipaaPrompt: HipaaPromptState?` to `ProfileContentView` and route share through HipaaGate first.**

In `COPDFuel/COPDFuel/Views/ProfileView.swift`, inside `struct ProfileContentView`, add the state near the other `@State` declarations:

```swift
    @State private var hipaaPrompt: HipaaPromptState?
```

Locate the `shareReportOrShowPaywall()` function (around line 442) and replace its body with:

```swift
    private func shareReportOrShowPaywall() {
        // Gate 1: premium
        guard store.isPremium else {
            DispatchQueue.main.async { showPaywall = true }
            return
        }
        // Gate 2: HIPAA consent (matches Android ProfileFragment.shareViaEmail etc.)
        switch HipaaGate.requireConsent(action: "share your report") {
        case .granted:
            shareReport()
        case .needsConsent(let prompt):
            hipaaPrompt = prompt
        }
    }
```

Attach the prompt modifier near the existing `.sheet(isPresented:)` chain at the top of `ProfileView`'s body. Find the existing sheet modifiers on `ProfileView` (around lines 31–39):

```swift
        .sheet(isPresented: $showPaywall) {
            PaywallView()
        }
        .sheet(isPresented: $showHipaaView) {
            HipaaAuthorizationView()
        }
        .sheet(isPresented: $showShareSheet) {
            ShareSheet(activityItems: [reportText])
        }
```

These live on the parent `ProfileView`, but the `hipaaPrompt` state is on `ProfileContentView`. Move the alert binding to where the state lives. Inside `ProfileContentView.body`, after the existing `onAppearModifier` (around line 336–337), attach the alert. Replace this line:

```swift
        onAppearModifier
```

with:

```swift
        onAppearModifier
            .hipaaConsentAlert($hipaaPrompt)
```

(`hipaaConsentAlert` was defined in Task 1's `HipaaGate.swift`. The `.hipaaConsentAlert(...)` modifier renders the standard "HIPAA Authorization Required" alert when `hipaaPrompt != nil` and shows `HipaaAuthorizationView` in a sheet on Sign Now.)

- [ ] **Step 5.2: Build.**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet build 2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 5.3: Commit.**

```bash
git add "COPDFuel/COPDFuel/Views/ProfileView.swift"
git commit -m "iOS Share Report: gate behind HIPAA consent

Share Health Report now checks HipaaGate.requireConsent before
invoking the share sheet. Users without HIPAA on file see the
standard \"HIPAA Authorization Required\" alert with a Sign Now
shortcut, matching Android ProfileFragment.shareViaEmail/shareViaText
gating."
```

---

## Task 6: Expand `FoodEntry` to the 23-nutrient schema

**Files:**
- Modify: `COPDFuel/COPDFuel/Models/FoodEntry.swift`

Android `data/model/FoodEntry.kt` carries 23 nutrient columns. iOS currently has 4. Add the missing 19 as defaulted properties so old persisted JSON still decodes (missing keys → 0.0).

- [ ] **Step 6.1: Replace the contents of `FoodEntry.swift` with the expanded struct.**

```swift
//
//  FoodEntry.swift
//  COPDFuel
//
//  Food tracking data model — schema mirrors Android FoodEntry.kt
//  (23 nutrients). All extended fields default to 0.0 so older
//  persisted entries (which only stored cal/protein/carbs/fat) decode
//  cleanly via Codable's missing-key handling.
//

import Foundation

struct FoodEntry: Identifiable, Codable {
    let id: UUID
    let date: Date
    let mealType: String   // Breakfast, Lunch, Dinner, Snacks
    let foodName: String
    let quantity: String

    // Macros
    let calories: Double?
    let protein: Double?
    let carbs: Double?
    let fat: Double?
    let fiber: Double
    // Minerals
    let sodium: Double
    let potassium: Double
    let calcium: Double
    let iron: Double
    let magnesium: Double
    let zinc: Double
    let selenium: Double
    let manganese: Double
    // Water
    let water: Double
    // Vitamins
    let vitaminA: Double
    let vitaminC: Double
    let vitaminD: Double
    let vitaminE: Double
    let vitaminK: Double
    // Fats / sterols / sugars
    let saturatedFat: Double
    let cholesterol: Double
    let omega3: Double
    let addedSugars: Double

    init(
        id: UUID = UUID(),
        date: Date = Date(),
        mealType: String,
        foodName: String,
        quantity: String,
        calories: Double? = nil,
        protein: Double? = nil,
        carbs: Double? = nil,
        fat: Double? = nil,
        fiber: Double = 0,
        sodium: Double = 0,
        potassium: Double = 0,
        calcium: Double = 0,
        iron: Double = 0,
        magnesium: Double = 0,
        zinc: Double = 0,
        selenium: Double = 0,
        manganese: Double = 0,
        water: Double = 0,
        vitaminA: Double = 0,
        vitaminC: Double = 0,
        vitaminD: Double = 0,
        vitaminE: Double = 0,
        vitaminK: Double = 0,
        saturatedFat: Double = 0,
        cholesterol: Double = 0,
        omega3: Double = 0,
        addedSugars: Double = 0
    ) {
        self.id = id
        self.date = date
        self.mealType = mealType
        self.foodName = foodName
        self.quantity = quantity
        self.calories = calories
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
        self.fiber = fiber
        self.sodium = sodium
        self.potassium = potassium
        self.calcium = calcium
        self.iron = iron
        self.magnesium = magnesium
        self.zinc = zinc
        self.selenium = selenium
        self.manganese = manganese
        self.water = water
        self.vitaminA = vitaminA
        self.vitaminC = vitaminC
        self.vitaminD = vitaminD
        self.vitaminE = vitaminE
        self.vitaminK = vitaminK
        self.saturatedFat = saturatedFat
        self.cholesterol = cholesterol
        self.omega3 = omega3
        self.addedSugars = addedSugars
    }

    // Codable migration: missing keys → 0.0 (handles entries persisted
    // before the schema expansion).
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(UUID.self, forKey: .id)
        date = try c.decode(Date.self, forKey: .date)
        mealType = try c.decode(String.self, forKey: .mealType)
        foodName = try c.decode(String.self, forKey: .foodName)
        quantity = try c.decode(String.self, forKey: .quantity)
        calories = try c.decodeIfPresent(Double.self, forKey: .calories)
        protein = try c.decodeIfPresent(Double.self, forKey: .protein)
        carbs = try c.decodeIfPresent(Double.self, forKey: .carbs)
        fat = try c.decodeIfPresent(Double.self, forKey: .fat)
        fiber = try c.decodeIfPresent(Double.self, forKey: .fiber) ?? 0
        sodium = try c.decodeIfPresent(Double.self, forKey: .sodium) ?? 0
        potassium = try c.decodeIfPresent(Double.self, forKey: .potassium) ?? 0
        calcium = try c.decodeIfPresent(Double.self, forKey: .calcium) ?? 0
        iron = try c.decodeIfPresent(Double.self, forKey: .iron) ?? 0
        magnesium = try c.decodeIfPresent(Double.self, forKey: .magnesium) ?? 0
        zinc = try c.decodeIfPresent(Double.self, forKey: .zinc) ?? 0
        selenium = try c.decodeIfPresent(Double.self, forKey: .selenium) ?? 0
        manganese = try c.decodeIfPresent(Double.self, forKey: .manganese) ?? 0
        water = try c.decodeIfPresent(Double.self, forKey: .water) ?? 0
        vitaminA = try c.decodeIfPresent(Double.self, forKey: .vitaminA) ?? 0
        vitaminC = try c.decodeIfPresent(Double.self, forKey: .vitaminC) ?? 0
        vitaminD = try c.decodeIfPresent(Double.self, forKey: .vitaminD) ?? 0
        vitaminE = try c.decodeIfPresent(Double.self, forKey: .vitaminE) ?? 0
        vitaminK = try c.decodeIfPresent(Double.self, forKey: .vitaminK) ?? 0
        saturatedFat = try c.decodeIfPresent(Double.self, forKey: .saturatedFat) ?? 0
        cholesterol = try c.decodeIfPresent(Double.self, forKey: .cholesterol) ?? 0
        omega3 = try c.decodeIfPresent(Double.self, forKey: .omega3) ?? 0
        addedSugars = try c.decodeIfPresent(Double.self, forKey: .addedSugars) ?? 0
    }
}
```

- [ ] **Step 6.2: Build to verify call sites still compile (the extended init keeps the old 4-macro signature working via defaults).**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet build 2>&1 | tail -20
```
Expected: `** BUILD SUCCEEDED **`. If a call site of `FoodEntry(...)` breaks because it was using positional args, fix that site to use named args at the same time.

- [ ] **Step 6.3: Commit.**

```bash
git add "COPDFuel/COPDFuel/Models/FoodEntry.swift"
git commit -m "iOS FoodEntry: expand to Android 23-nutrient schema

Adds fiber, sodium, potassium, calcium, iron, magnesium, zinc,
selenium, manganese, water, vitamins A/C/D/E/K, saturated fat,
cholesterol, omega-3, and added sugars. All new fields default to
0.0 and decode safely when absent from older persisted JSON."
```

---

## Task 7: Expand `FavoriteFood` to the same schema

**Files:**
- Modify: `COPDFuel/COPDFuel/Models/FavoriteFood.swift`

- [ ] **Step 7.1: Replace `FavoriteFood.swift` with the expanded struct.**

```swift
//
//  FavoriteFood.swift
//  COPDFuel
//
//  Saved favorite food for quick logging — 23-nutrient schema
//  matching FoodEntry / Android FavoriteFood.kt. Backward compatible
//  with older saved favorites (missing keys default to 0.0).
//

import Foundation

struct FavoriteFood: Identifiable, Codable, Equatable {
    let id: UUID
    var label: String
    var name: String
    var mealCategory: String
    var quantity: String

    // Macros
    var calories: Double
    var protein: Double
    var carbs: Double
    var fat: Double
    var fiber: Double
    // Minerals
    var sodium: Double
    var potassium: Double
    var calcium: Double
    var iron: Double
    var magnesium: Double
    var zinc: Double
    var selenium: Double
    var manganese: Double
    // Water
    var water: Double
    // Vitamins
    var vitaminA: Double
    var vitaminC: Double
    var vitaminD: Double
    var vitaminE: Double
    var vitaminK: Double
    // Fats / sterols / sugars
    var saturatedFat: Double
    var cholesterol: Double
    var omega3: Double
    var addedSugars: Double

    init(
        id: UUID = UUID(),
        label: String,
        name: String,
        mealCategory: String,
        quantity: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double = 0,
        sodium: Double = 0,
        potassium: Double = 0,
        calcium: Double = 0,
        iron: Double = 0,
        magnesium: Double = 0,
        zinc: Double = 0,
        selenium: Double = 0,
        manganese: Double = 0,
        water: Double = 0,
        vitaminA: Double = 0,
        vitaminC: Double = 0,
        vitaminD: Double = 0,
        vitaminE: Double = 0,
        vitaminK: Double = 0,
        saturatedFat: Double = 0,
        cholesterol: Double = 0,
        omega3: Double = 0,
        addedSugars: Double = 0
    ) {
        self.id = id
        self.label = label
        self.name = name
        self.mealCategory = mealCategory
        self.quantity = quantity
        self.calories = calories
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
        self.fiber = fiber
        self.sodium = sodium
        self.potassium = potassium
        self.calcium = calcium
        self.iron = iron
        self.magnesium = magnesium
        self.zinc = zinc
        self.selenium = selenium
        self.manganese = manganese
        self.water = water
        self.vitaminA = vitaminA
        self.vitaminC = vitaminC
        self.vitaminD = vitaminD
        self.vitaminE = vitaminE
        self.vitaminK = vitaminK
        self.saturatedFat = saturatedFat
        self.cholesterol = cholesterol
        self.omega3 = omega3
        self.addedSugars = addedSugars
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(UUID.self, forKey: .id)
        label = try c.decode(String.self, forKey: .label)
        name = try c.decode(String.self, forKey: .name)
        mealCategory = try c.decode(String.self, forKey: .mealCategory)
        quantity = try c.decode(String.self, forKey: .quantity)
        calories = try c.decode(Double.self, forKey: .calories)
        protein = try c.decode(Double.self, forKey: .protein)
        carbs = try c.decode(Double.self, forKey: .carbs)
        fat = try c.decode(Double.self, forKey: .fat)
        fiber = try c.decodeIfPresent(Double.self, forKey: .fiber) ?? 0
        sodium = try c.decodeIfPresent(Double.self, forKey: .sodium) ?? 0
        potassium = try c.decodeIfPresent(Double.self, forKey: .potassium) ?? 0
        calcium = try c.decodeIfPresent(Double.self, forKey: .calcium) ?? 0
        iron = try c.decodeIfPresent(Double.self, forKey: .iron) ?? 0
        magnesium = try c.decodeIfPresent(Double.self, forKey: .magnesium) ?? 0
        zinc = try c.decodeIfPresent(Double.self, forKey: .zinc) ?? 0
        selenium = try c.decodeIfPresent(Double.self, forKey: .selenium) ?? 0
        manganese = try c.decodeIfPresent(Double.self, forKey: .manganese) ?? 0
        water = try c.decodeIfPresent(Double.self, forKey: .water) ?? 0
        vitaminA = try c.decodeIfPresent(Double.self, forKey: .vitaminA) ?? 0
        vitaminC = try c.decodeIfPresent(Double.self, forKey: .vitaminC) ?? 0
        vitaminD = try c.decodeIfPresent(Double.self, forKey: .vitaminD) ?? 0
        vitaminE = try c.decodeIfPresent(Double.self, forKey: .vitaminE) ?? 0
        vitaminK = try c.decodeIfPresent(Double.self, forKey: .vitaminK) ?? 0
        saturatedFat = try c.decodeIfPresent(Double.self, forKey: .saturatedFat) ?? 0
        cholesterol = try c.decodeIfPresent(Double.self, forKey: .cholesterol) ?? 0
        omega3 = try c.decodeIfPresent(Double.self, forKey: .omega3) ?? 0
        addedSugars = try c.decodeIfPresent(Double.self, forKey: .addedSugars) ?? 0
    }

    /// Produces a fully-populated FoodEntry from this favorite.
    /// Carries all 23 nutrients so logging a favorite preserves the
    /// information the report generator and protein-progress views
    /// need (matches Android FavoriteFoodDao behavior).
    func toFoodEntry(mealType: String? = nil, date: Date = Date()) -> FoodEntry {
        FoodEntry(
            date: date,
            mealType: mealType ?? mealCategory,
            foodName: name,
            quantity: quantity,
            calories: calories,
            protein: protein,
            carbs: carbs,
            fat: fat,
            fiber: fiber,
            sodium: sodium,
            potassium: potassium,
            calcium: calcium,
            iron: iron,
            magnesium: magnesium,
            zinc: zinc,
            selenium: selenium,
            manganese: manganese,
            water: water,
            vitaminA: vitaminA,
            vitaminC: vitaminC,
            vitaminD: vitaminD,
            vitaminE: vitaminE,
            vitaminK: vitaminK,
            saturatedFat: saturatedFat,
            cholesterol: cholesterol,
            omega3: omega3,
            addedSugars: addedSugars
        )
    }
}
```

- [ ] **Step 7.2: Build.**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet build 2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 7.3: Commit.**

```bash
git add "COPDFuel/COPDFuel/Models/FavoriteFood.swift"
git commit -m "iOS FavoriteFood: expand to 23-nutrient schema

Now carries every nutrient FoodEntry tracks, and toFoodEntry()
propagates all of them so the report generator and macro-target
views see real numbers when a favorite is logged. Old saved
favorites decode with the new fields defaulted to 0.0."
```

---

## Task 8: Expand `FavoriteMealItem` to the same schema

**Files:**
- Modify: `COPDFuel/COPDFuel/Models/FavoriteMeal.swift`

- [ ] **Step 8.1: Replace `FavoriteMeal.swift` with the expanded structs.**

```swift
//
//  FavoriteMeal.swift
//  COPDFuel
//
//  Saved favorite meal (collection of items) — items carry the full
//  23-nutrient schema so totals and per-item logging preserve macros
//  and micros. Mirrors Android FavoriteMeal + FavoriteMealItem.
//

import Foundation

struct FavoriteMeal: Identifiable, Codable {
    let id: UUID
    var label: String
    var mealCategory: String
    var items: [FavoriteMealItem]

    init(id: UUID = UUID(), label: String, mealCategory: String, items: [FavoriteMealItem] = []) {
        self.id = id
        self.label = label
        self.mealCategory = mealCategory
        self.items = items
    }

    var totalCalories: Double { items.reduce(0) { $0 + $1.calories } }
    var totalProtein: Double  { items.reduce(0) { $0 + $1.protein } }
    var totalCarbs: Double    { items.reduce(0) { $0 + $1.carbs } }
    var totalFat: Double      { items.reduce(0) { $0 + $1.fat } }
    var totalFiber: Double    { items.reduce(0) { $0 + $1.fiber } }
    var totalSodium: Double   { items.reduce(0) { $0 + $1.sodium } }
    var totalPotassium: Double { items.reduce(0) { $0 + $1.potassium } }
}

struct FavoriteMealItem: Identifiable, Codable {
    let id: UUID
    var name: String
    var quantity: String

    var calories: Double
    var protein: Double
    var carbs: Double
    var fat: Double
    var fiber: Double
    var sodium: Double
    var potassium: Double
    var calcium: Double
    var iron: Double
    var magnesium: Double
    var zinc: Double
    var selenium: Double
    var manganese: Double
    var water: Double
    var vitaminA: Double
    var vitaminC: Double
    var vitaminD: Double
    var vitaminE: Double
    var vitaminK: Double
    var saturatedFat: Double
    var cholesterol: Double
    var omega3: Double
    var addedSugars: Double

    init(
        id: UUID = UUID(),
        name: String,
        quantity: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double = 0,
        sodium: Double = 0,
        potassium: Double = 0,
        calcium: Double = 0,
        iron: Double = 0,
        magnesium: Double = 0,
        zinc: Double = 0,
        selenium: Double = 0,
        manganese: Double = 0,
        water: Double = 0,
        vitaminA: Double = 0,
        vitaminC: Double = 0,
        vitaminD: Double = 0,
        vitaminE: Double = 0,
        vitaminK: Double = 0,
        saturatedFat: Double = 0,
        cholesterol: Double = 0,
        omega3: Double = 0,
        addedSugars: Double = 0
    ) {
        self.id = id
        self.name = name
        self.quantity = quantity
        self.calories = calories
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
        self.fiber = fiber
        self.sodium = sodium
        self.potassium = potassium
        self.calcium = calcium
        self.iron = iron
        self.magnesium = magnesium
        self.zinc = zinc
        self.selenium = selenium
        self.manganese = manganese
        self.water = water
        self.vitaminA = vitaminA
        self.vitaminC = vitaminC
        self.vitaminD = vitaminD
        self.vitaminE = vitaminE
        self.vitaminK = vitaminK
        self.saturatedFat = saturatedFat
        self.cholesterol = cholesterol
        self.omega3 = omega3
        self.addedSugars = addedSugars
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(UUID.self, forKey: .id)
        name = try c.decode(String.self, forKey: .name)
        quantity = try c.decode(String.self, forKey: .quantity)
        calories = try c.decode(Double.self, forKey: .calories)
        protein = try c.decode(Double.self, forKey: .protein)
        carbs = try c.decode(Double.self, forKey: .carbs)
        fat = try c.decode(Double.self, forKey: .fat)
        fiber = try c.decodeIfPresent(Double.self, forKey: .fiber) ?? 0
        sodium = try c.decodeIfPresent(Double.self, forKey: .sodium) ?? 0
        potassium = try c.decodeIfPresent(Double.self, forKey: .potassium) ?? 0
        calcium = try c.decodeIfPresent(Double.self, forKey: .calcium) ?? 0
        iron = try c.decodeIfPresent(Double.self, forKey: .iron) ?? 0
        magnesium = try c.decodeIfPresent(Double.self, forKey: .magnesium) ?? 0
        zinc = try c.decodeIfPresent(Double.self, forKey: .zinc) ?? 0
        selenium = try c.decodeIfPresent(Double.self, forKey: .selenium) ?? 0
        manganese = try c.decodeIfPresent(Double.self, forKey: .manganese) ?? 0
        water = try c.decodeIfPresent(Double.self, forKey: .water) ?? 0
        vitaminA = try c.decodeIfPresent(Double.self, forKey: .vitaminA) ?? 0
        vitaminC = try c.decodeIfPresent(Double.self, forKey: .vitaminC) ?? 0
        vitaminD = try c.decodeIfPresent(Double.self, forKey: .vitaminD) ?? 0
        vitaminE = try c.decodeIfPresent(Double.self, forKey: .vitaminE) ?? 0
        vitaminK = try c.decodeIfPresent(Double.self, forKey: .vitaminK) ?? 0
        saturatedFat = try c.decodeIfPresent(Double.self, forKey: .saturatedFat) ?? 0
        cholesterol = try c.decodeIfPresent(Double.self, forKey: .cholesterol) ?? 0
        omega3 = try c.decodeIfPresent(Double.self, forKey: .omega3) ?? 0
        addedSugars = try c.decodeIfPresent(Double.self, forKey: .addedSugars) ?? 0
    }
}
```

- [ ] **Step 8.2: Build.**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet build 2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 8.3: Commit.**

```bash
git add "COPDFuel/COPDFuel/Models/FavoriteMeal.swift"
git commit -m "iOS FavoriteMeal/Item: expand to 23-nutrient schema

Each saved meal item now carries every nutrient FoodEntry tracks
plus seven aggregate totals (cal/protein/carbs/fat/fiber/sodium/
potassium) on FavoriteMeal. Backward-compatible decoding for older
persisted meals."
```

---

## Task 9: Expand `UserAddedFood` to the same schema

**Files:**
- Modify: `COPDFuel/COPDFuel/Models/UserAddedFood.swift`

Per-100g schema like Android `UserAddedFood.kt`.

- [ ] **Step 9.1: Replace `UserAddedFood.swift` with the expanded struct.**

```swift
//
//  UserAddedFood.swift
//  COPDFuel
//
//  User-created food for search database (per 100g). Schema mirrors
//  Android UserAddedFood.kt — all 23 nutrients.
//

import Foundation

struct UserAddedFood: Identifiable, Codable {
    let id: UUID
    var name: String
    var shortName: String
    var category: String
    var categoryGroup: String

    // Per-100g nutrients
    var calories: Double
    var protein: Double
    var carbs: Double
    var fat: Double
    var fiber: Double
    var sodium: Double
    var potassium: Double
    var calcium: Double
    var iron: Double
    var magnesium: Double
    var zinc: Double
    var selenium: Double
    var manganese: Double
    var water: Double
    var vitaminA: Double
    var vitaminC: Double
    var vitaminD: Double
    var vitaminE: Double
    var vitaminK: Double
    var saturatedFat: Double
    var cholesterol: Double
    var omega3: Double
    var addedSugars: Double

    // Default portion metadata (kept from prior schema for UI display)
    var portionSize: Double
    var portionUnit: String
    var portionDesc: String

    init(
        id: UUID = UUID(),
        name: String,
        shortName: String? = nil,
        category: String = "User added",
        categoryGroup: String = "User added",
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double = 0,
        sodium: Double = 0,
        potassium: Double = 0,
        calcium: Double = 0,
        iron: Double = 0,
        magnesium: Double = 0,
        zinc: Double = 0,
        selenium: Double = 0,
        manganese: Double = 0,
        water: Double = 0,
        vitaminA: Double = 0,
        vitaminC: Double = 0,
        vitaminD: Double = 0,
        vitaminE: Double = 0,
        vitaminK: Double = 0,
        saturatedFat: Double = 0,
        cholesterol: Double = 0,
        omega3: Double = 0,
        addedSugars: Double = 0,
        portionSize: Double = 100,
        portionUnit: String = "g",
        portionDesc: String = "100g"
    ) {
        self.id = id
        self.name = name
        self.shortName = shortName ?? name
        self.category = category
        self.categoryGroup = categoryGroup
        self.calories = calories
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
        self.fiber = fiber
        self.sodium = sodium
        self.potassium = potassium
        self.calcium = calcium
        self.iron = iron
        self.magnesium = magnesium
        self.zinc = zinc
        self.selenium = selenium
        self.manganese = manganese
        self.water = water
        self.vitaminA = vitaminA
        self.vitaminC = vitaminC
        self.vitaminD = vitaminD
        self.vitaminE = vitaminE
        self.vitaminK = vitaminK
        self.saturatedFat = saturatedFat
        self.cholesterol = cholesterol
        self.omega3 = omega3
        self.addedSugars = addedSugars
        self.portionSize = portionSize
        self.portionUnit = portionUnit
        self.portionDesc = portionDesc
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(UUID.self, forKey: .id)
        name = try c.decode(String.self, forKey: .name)
        shortName = try c.decodeIfPresent(String.self, forKey: .shortName) ?? name
        category = try c.decodeIfPresent(String.self, forKey: .category) ?? "User added"
        categoryGroup = try c.decodeIfPresent(String.self, forKey: .categoryGroup) ?? "User added"
        calories = try c.decode(Double.self, forKey: .calories)
        protein = try c.decode(Double.self, forKey: .protein)
        carbs = try c.decode(Double.self, forKey: .carbs)
        fat = try c.decode(Double.self, forKey: .fat)
        fiber = try c.decodeIfPresent(Double.self, forKey: .fiber) ?? 0
        sodium = try c.decodeIfPresent(Double.self, forKey: .sodium) ?? 0
        potassium = try c.decodeIfPresent(Double.self, forKey: .potassium) ?? 0
        calcium = try c.decodeIfPresent(Double.self, forKey: .calcium) ?? 0
        iron = try c.decodeIfPresent(Double.self, forKey: .iron) ?? 0
        magnesium = try c.decodeIfPresent(Double.self, forKey: .magnesium) ?? 0
        zinc = try c.decodeIfPresent(Double.self, forKey: .zinc) ?? 0
        selenium = try c.decodeIfPresent(Double.self, forKey: .selenium) ?? 0
        manganese = try c.decodeIfPresent(Double.self, forKey: .manganese) ?? 0
        water = try c.decodeIfPresent(Double.self, forKey: .water) ?? 0
        vitaminA = try c.decodeIfPresent(Double.self, forKey: .vitaminA) ?? 0
        vitaminC = try c.decodeIfPresent(Double.self, forKey: .vitaminC) ?? 0
        vitaminD = try c.decodeIfPresent(Double.self, forKey: .vitaminD) ?? 0
        vitaminE = try c.decodeIfPresent(Double.self, forKey: .vitaminE) ?? 0
        vitaminK = try c.decodeIfPresent(Double.self, forKey: .vitaminK) ?? 0
        saturatedFat = try c.decodeIfPresent(Double.self, forKey: .saturatedFat) ?? 0
        cholesterol = try c.decodeIfPresent(Double.self, forKey: .cholesterol) ?? 0
        omega3 = try c.decodeIfPresent(Double.self, forKey: .omega3) ?? 0
        addedSugars = try c.decodeIfPresent(Double.self, forKey: .addedSugars) ?? 0
        portionSize = try c.decodeIfPresent(Double.self, forKey: .portionSize) ?? 100
        portionUnit = try c.decodeIfPresent(String.self, forKey: .portionUnit) ?? "g"
        portionDesc = try c.decodeIfPresent(String.self, forKey: .portionDesc) ?? "100g"
    }
}
```

- [ ] **Step 9.2: Build.**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet build 2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 9.3: Commit.**

```bash
git add "COPDFuel/COPDFuel/Models/UserAddedFood.swift"
git commit -m "iOS UserAddedFood: expand to 23-nutrient per-100g schema

Custom foods saved by the user now hold the same micronutrient
schema the local DB foods do, so re-logging them populates the full
report rather than degrading to four macros."
```

---

## Task 10: Expand `FoodSearchResult` to the same schema

**Files:**
- Modify: `COPDFuel/COPDFuel/Models/FoodSearchResult.swift`

This is the in-memory result of a food-DB lookup; not persisted (so no decoder migration needed). `servingSizes` is added as an empty placeholder for the P1 serving-picker task — leaving the field on the model now lets later branches populate it without re-touching the model.

- [ ] **Step 10.1: Replace `FoodSearchResult.swift` with the expanded struct.**

```swift
//
//  FoodSearchResult.swift
//  COPDFuel
//
//  Result from food database or user-added food for search.
//  Mirrors Android FoodSearchResult.kt's nutrient set so search
//  hits can populate FoodEntry's full schema.
//

import Foundation

struct FoodServingOption: Equatable {
    let label: String
    let grams: Double
    let isCustom: Bool
}

struct FoodSearchResult: Identifiable {
    let id: String
    let name: String
    let shortName: String
    let category: String
    let categoryGroup: String
    let calories: Double
    let protein: Double
    let carbs: Double
    let fat: Double
    let fiber: Double
    let sodium: Double
    let potassium: Double
    let calcium: Double
    let iron: Double
    let magnesium: Double
    let zinc: Double
    let selenium: Double
    let manganese: Double
    let water: Double
    let vitaminA: Double
    let vitaminC: Double
    let vitaminD: Double
    let vitaminE: Double
    let vitaminK: Double
    let saturatedFat: Double
    let cholesterol: Double
    let omega3: Double
    let addedSugars: Double
    let portionDesc: String
    let isUserAdded: Bool
    /// Per-result serving options sourced from the food DB. Empty
    /// when no per-row servings are defined; the food modal falls
    /// back to "1 serving"/"100g"/"g (enter amount)" in that case.
    /// Populated in P1 (serving-picker task).
    let servingSizes: [FoodServingOption]

    init(
        id: String,
        name: String,
        shortName: String,
        category: String,
        categoryGroup: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double = 0,
        sodium: Double = 0,
        potassium: Double = 0,
        calcium: Double = 0,
        iron: Double = 0,
        magnesium: Double = 0,
        zinc: Double = 0,
        selenium: Double = 0,
        manganese: Double = 0,
        water: Double = 0,
        vitaminA: Double = 0,
        vitaminC: Double = 0,
        vitaminD: Double = 0,
        vitaminE: Double = 0,
        vitaminK: Double = 0,
        saturatedFat: Double = 0,
        cholesterol: Double = 0,
        omega3: Double = 0,
        addedSugars: Double = 0,
        portionDesc: String,
        isUserAdded: Bool,
        servingSizes: [FoodServingOption] = []
    ) {
        self.id = id
        self.name = name
        self.shortName = shortName
        self.category = category
        self.categoryGroup = categoryGroup
        self.calories = calories
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
        self.fiber = fiber
        self.sodium = sodium
        self.potassium = potassium
        self.calcium = calcium
        self.iron = iron
        self.magnesium = magnesium
        self.zinc = zinc
        self.selenium = selenium
        self.manganese = manganese
        self.water = water
        self.vitaminA = vitaminA
        self.vitaminC = vitaminC
        self.vitaminD = vitaminD
        self.vitaminE = vitaminE
        self.vitaminK = vitaminK
        self.saturatedFat = saturatedFat
        self.cholesterol = cholesterol
        self.omega3 = omega3
        self.addedSugars = addedSugars
        self.portionDesc = portionDesc
        self.isUserAdded = isUserAdded
        self.servingSizes = servingSizes
    }
}
```

- [ ] **Step 10.2: Build.**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet build 2>&1 | tail -40
```
Expected: `** BUILD SUCCEEDED **`. If `FoodDatabaseService.swift` fails because it constructs `FoodSearchResult` with positional args, fix those call sites by switching to named args + relying on defaults — the new init keeps positional ordering for the previously-required fields, so most call sites still work, but any that listed only `id, name, shortName, category, categoryGroup, calories, protein, carbs, fat, fiber, sodium, potassium, portionDesc, isUserAdded` will need to switch to named args (the new fields come before `portionDesc` in declaration order).

- [ ] **Step 10.3: If build fails in `FoodDatabaseService.swift`, patch the call sites to use named args.**

Open the failing call site reported by `xcodebuild`. For each construction:
```swift
FoodSearchResult(id: ..., name: ..., shortName: ..., category: ..., categoryGroup: ..., calories: ..., protein: ..., carbs: ..., fat: ..., fiber: ..., sodium: ..., potassium: ..., portionDesc: ..., isUserAdded: ...)
```
Either:
- Leave as is if Swift still resolves it via the defaults (the new fields are in between).
- Or switch to fully named arguments — the safest is to make every argument explicitly named.

Re-run the build. Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 10.4: Commit.**

```bash
git add "COPDFuel/COPDFuel/Models/FoodSearchResult.swift" "COPDFuel/COPDFuel/Services/FoodDatabaseService.swift"
git commit -m "iOS FoodSearchResult: expand to 23-nutrient schema + serving slot

Search results now carry every nutrient the FoodEntry schema needs
plus an empty servingSizes placeholder the P1 serving-picker will
populate. Adjusts call sites in FoodDatabaseService that built
results positionally."
```

(If `FoodDatabaseService.swift` was not touched by the build fix, drop it from the `git add` line.)

---

## Task 11: Propagate the expanded schema through `TrackingView` food-save paths

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/TrackingView.swift`

After Tasks 6–10 the structs hold all 23 nutrients, but `TrackingView.saveSelectedResult` (around line 1149) and `ManualFoodEntryView`'s Save action (around line 1222) only populate 4. Wire the rest through.

- [ ] **Step 11.1: Update `saveSelectedResult` to propagate every nutrient × multiplier.**

In `TrackingView.swift`, locate `private func saveSelectedResult(_ result: FoodSearchResult)` (around line 1149). Replace its body with:

```swift
    private func saveSelectedResult(_ result: FoodSearchResult) {
        let qty = quantity.isEmpty ? "1" : quantity
        let mult = Double(qty) ?? 1.0
        let entry = FoodEntry(
            date: selectedDate,
            mealType: selectedMeal,
            foodName: result.name,
            quantity: "\(qty) \(result.portionDesc)",
            calories: result.calories * mult,
            protein: result.protein * mult,
            carbs: result.carbs * mult,
            fat: result.fat * mult,
            fiber: result.fiber * mult,
            sodium: result.sodium * mult,
            potassium: result.potassium * mult,
            calcium: result.calcium * mult,
            iron: result.iron * mult,
            magnesium: result.magnesium * mult,
            zinc: result.zinc * mult,
            selenium: result.selenium * mult,
            manganese: result.manganese * mult,
            water: result.water * mult,
            vitaminA: result.vitaminA * mult,
            vitaminC: result.vitaminC * mult,
            vitaminD: result.vitaminD * mult,
            vitaminE: result.vitaminE * mult,
            vitaminK: result.vitaminK * mult,
            saturatedFat: result.saturatedFat * mult,
            cholesterol: result.cholesterol * mult,
            omega3: result.omega3 * mult,
            addedSugars: result.addedSugars * mult
        )
        dataManager.addFoodEntry(entry)
        if addToFavorites {
            let fav = FavoriteFood(
                label: result.name,
                name: result.name,
                mealCategory: selectedMeal,
                quantity: "\(qty) \(result.portionDesc)",
                calories: result.calories * mult,
                protein: result.protein * mult,
                carbs: result.carbs * mult,
                fat: result.fat * mult,
                fiber: result.fiber * mult,
                sodium: result.sodium * mult,
                potassium: result.potassium * mult,
                calcium: result.calcium * mult,
                iron: result.iron * mult,
                magnesium: result.magnesium * mult,
                zinc: result.zinc * mult,
                selenium: result.selenium * mult,
                manganese: result.manganese * mult,
                water: result.water * mult,
                vitaminA: result.vitaminA * mult,
                vitaminC: result.vitaminC * mult,
                vitaminD: result.vitaminD * mult,
                vitaminE: result.vitaminE * mult,
                vitaminK: result.vitaminK * mult,
                saturatedFat: result.saturatedFat * mult,
                cholesterol: result.cholesterol * mult,
                omega3: result.omega3 * mult,
                addedSugars: result.addedSugars * mult
            )
            dataManager.addFavoriteFood(fav)
        }
    }
```

- [ ] **Step 11.2: Update `ManualFoodEntryView`'s Save action to keep manual-entry behavior unchanged.**

The manual-entry view collects only macros; that's expected (matches Android). No edit is required here — the default `0` for the 19 new fields is correct for manual entries. Verify by reading the existing Save block (around lines 1222-1249) — confirm it still compiles after Task 6's `FoodEntry` change. If the call uses positional args, switch to named args:

```swift
                    let entry = FoodEntry(
                        date: selectedDate,
                        mealType: selectedMeal,
                        foodName: foodName,
                        quantity: quantity,
                        calories: cal,
                        protein: prot,
                        carbs: c,
                        fat: f
                    )
```

(That call is already named; no edit needed if it builds.)

- [ ] **Step 11.3: Build.**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet build 2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 11.4: Commit.**

```bash
git add "COPDFuel/COPDFuel/Views/TrackingView.swift"
git commit -m "iOS Tracking: propagate 23-nutrient schema on food save

saveSelectedResult now multiplies every nutrient on the search
result by the quantity multiplier and stores all of them on
FoodEntry (and on the optional FavoriteFood). Manual entry is
unchanged — defaults fill the unused fields with zero, matching
Android's behavior."
```

---

## Task 12: Phase-boundary verification

- [ ] **Step 12.1: Full clean build.**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -quiet clean build 2>&1 | tail -10
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 12.2: Run the app in the iOS Simulator and walk the P0 acceptance checklist.**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'platform=iOS Simulator,name=iPhone 15' -quiet build 2>&1 | tail -5
open -a Simulator
```

Then in the simulator, manually verify:

1. **HIPAA sync gate:** Sign in (or use an existing account). With no HIPAA consent signed, foreground the app. Open the Xcode console (View → Debug Area) — confirm **no `/sync` or `/me` requests** appear in the API logs. (If you don't have request logging, set a breakpoint in `COPDAPIClient.shared.sync` and confirm it isn't hit.)
2. **HIPAA share gate:** From Profile, tap "Share Health Report" without prior HIPAA. Expect a "HIPAA Authorization Required" alert with **Sign Now** / **Cancel**. Tap Sign Now → confirm the HIPAA authorization sheet appears.
3. **HIPAA sign + retry sync:** Sign HIPAA on the sheet. After dismissal, confirm `COPDAPIClient.shared.sync` is now invoked (breakpoint or log).
4. **Revocation UI:** In `HipaaAuthorizationView`, revoke the consent (the existing revoke flow). Reopen the screen → expect the orange "Authorization revoked on <date>" banner above the resign form.
5. **Food schema round-trip:** Log a food via the local DB search (any item). Open the same entry in the Day view → confirm cal/protein/carbs/fat display correctly. Quit and relaunch the simulator app → confirm the entry is still present and macros are unchanged.

Mark each as ✅ or note the gap.

- [ ] **Step 12.3: Tag the P0 completion.**

```bash
git tag -a ios-parity-p0 -m "iOS parity Phase 0 complete: HIPAA gate + 23-nutrient food schema"
git log --oneline ios-parity-p0~12..ios-parity-p0
```
Expected: lists the 11 commits from Tasks 1–11.

---

## Self-Review (post-write)

### Spec coverage
- **Spec 0.1 HipaaGate** → Task 1 ✓
- **Spec 0.2 RootView gate** → Task 4 ✓
- **Spec 0.3 ProfileView share gate** → Task 5 ✓
- **Spec 0.4 Revocation persistence + Revoked-state UI** → Tasks 2, 3 ✓
- **Spec 0.5 Food data model expansion** → Tasks 6–11 ✓ (covers FoodEntry, FavoriteFood, FavoriteMeal/Item, UserAddedFood, FoodSearchResult, TrackingView consumers)

### Items deferred to P1 (called out explicitly in spec)
- `DataManager` serialization changes are not in P0 — `DataManager` stores `[FoodEntry]` and `[FavoriteFood]` via `JSONEncoder/Decoder`, so the Codable migrations on the models handle the round-trip automatically. P1's food-DB loader will write all 23 columns when reading the local DB.
- USDA online search, expanded local-DB loader columns, and serving-picker UI are all P1.

### Type consistency
- `HipaaGate.requireConsent(action:)` returns `RequireConsentResult`; call site in Task 5 destructures `.granted` / `.needsConsent(let prompt)`. ✓
- `HipaaPromptState` is `Identifiable` and `Equatable`; `@State` binding accepts `Binding<HipaaPromptState?>`. ✓
- `FoodEntry.calories/protein/carbs/fat` remain `Double?` (preserves existing nullable semantics); the 19 added fields are non-optional `Double` with defaults. ✓
- `FavoriteFood.toFoodEntry` passes optionals through unwrapped because `FavoriteFood`'s macros are non-optional `Double` — that's intentional and unchanged. ✓

### Placeholder scan
- All steps include concrete code, exact file paths, and exact commands. ✓
- No "implement appropriately" / "add error handling" handwaving. ✓

---

## What ships after P0

- iOS no longer transmits PHI before HIPAA consent (Spec 0.2).
- iOS no longer lets users share PHI before HIPAA consent (Spec 0.3).
- The HIPAA screen distinguishes Signed / Revoked / Not Signed states (Spec 0.4).
- iOS `FoodEntry`, `FavoriteFood`, `FavoriteMeal`, `UserAddedFood`, and `FoodSearchResult` all carry Android's full nutrient schema — unblocking P1 (Tracking USDA, Tracking serving picker, Tracking macro targets, Report Generator).
- Older saved entries continue to decode (defaulted-to-zero migration).
- The plan is shaped so each commit independently builds, which lets a reviewer bisect any future regression to a single task.
