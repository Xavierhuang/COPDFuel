# iOS → Android Feature, Content, and UX Parity

**Date:** 2026-05-28
**Scope:** Bring the native iOS app (`COPDFuel/`, Swift/SwiftUI) to full parity with the native Android app (`android/`, Kotlin). Android is the source of truth.
**Target:** Features + content + UI/UX feel (option **C** of the parity matrix).
**Platform equivalents are NOT gaps:** HealthKit ↔ Health Connect; StoreKit ↔ Google Play Billing; MapKit ↔ Google Places / Geocoding; UserDefaults ↔ SharedPreferences; Core Data ↔ Room; URLSession ↔ OkHttp.

The work is divided into three phases (P0, P1, P2). Each phase is independently shippable. Within a phase, items are grouped by feature area and can be executed in parallel.

---

## P0 — Compliance & Data-Correctness (must-fix)

These items expose the user to HIPAA/PHI risk or create data-model drift that blocks every other section. They MUST land before any P1 work that touches share, backend sync, or the food log.

### 0.1 — Build iOS `HipaaGate`

**Files (new):**
- `COPDFuel/COPDFuel/Services/HipaaGate.swift`

**Behavior matches `android/.../utils/HipaaGate.kt`:**
- `static func hasConsent() -> Bool` — silent check against `HipaaConsentStorage`.
- `@MainActor static func requireConsent(action: String, present: PresentationContext) -> Bool` — returns `true` if signed; otherwise presents an alert titled **"HIPAA Authorization Required"** with body **"You must sign the HIPAA authorization form before you can <action>."** and buttons **"Sign Now"** (opens `HipaaAuthorizationView` as a sheet) and **"Cancel"**, then returns `false`.

Because SwiftUI doesn't have UIKit's `AlertDialog.Builder`, expose two flavors:
- A SwiftUI view modifier `.requiresHipaaConsent(action:, isPresented:)` for declarative use in views.
- A static function for imperative call sites in services.

### 0.2 — Gate backend PHI sync behind HIPAA consent

**File:** `COPDFuel/COPDFuel/Views/RootView.swift` (lines 56–73, `registerAndSyncIfNeeded`)

Wrap the existing sync calls in `if HipaaGate.hasConsent() { ... }`. Match Android `MainActivity.kt:72-77` behavior exactly: sign-in succeeds, but no PHI uploads happen until HIPAA is signed. Once the user signs HIPAA later, sync runs on next app-foreground.

### 0.3 — Gate Share Report behind HIPAA consent

**File:** `COPDFuel/COPDFuel/Views/ProfileView.swift` (`shareReportOrShowPaywall`, `shareReport`)

Call `HipaaGate.requireConsent(action: "share your report", ...)` before generating the report. If not signed, present the HIPAA modal and abort the share.

### 0.4 — Persist HIPAA revocation date & render Revoked state

**Files:**
- `COPDFuel/COPDFuel/Services/HipaaConsentStorage.swift` — add `revokedDate: Date?` persistence under key `"revoked_date"`. Add `getRevokedDate() -> Date?` accessor.
- `COPDFuel/COPDFuel/Views/HipaaAuthorizationView.swift` — when `consentStatus == .revoked`, render a banner ("Authorization revoked on <date>. You may sign again below.") above the (already-rendered) form. Distinguish visually from the "Not signed" first-time entry.

### 0.5 — Expand iOS Food data model to 23 nutrients

**Files (model):**
- `COPDFuel/COPDFuel/Models/FoodEntry.swift`
- `COPDFuel/COPDFuel/Models/FavoriteFood.swift`
- `COPDFuel/COPDFuel/Models/FavoriteMeal.swift` (and a new `FavoriteMealItem.swift`)
- `COPDFuel/COPDFuel/Models/UserAddedFood.swift`
- `COPDFuel/COPDFuel/Models/FoodSearchResult.swift`

**Add fields** (matching Android `data/model/FoodEntry.kt`): `fiber, sodium, potassium, calcium, iron, magnesium, zinc, selenium, manganese, water, vitaminA, vitaminC, vitaminD, vitaminE, vitaminK, saturatedFat, cholesterol, omega3, addedSugars` — all `Double` defaulting to `0.0` so older saved entries still decode.

**Files (storage):**
- `COPDFuel/COPDFuel/Services/DataManager.swift` — bump the serialization/migration to read the new fields if present, write them always, default missing fields to `0.0`.
- `COPDFuel/COPDFuel/Services/FoodDatabaseService.swift` — extend `FoodSearchResult` and the food-DB loader to parse all 23 columns from the source. The Android loader at `android/.../data/FoodDatabaseHelper.kt` is the canonical reference.

**Files (UI consumers):**
- `COPDFuel/COPDFuel/Views/TrackingView.swift` — `saveSelectedResult` and `ManualFoodEntryView` must populate all 23 fields (not just the four).
- The favorites & meal-items paths must round-trip all fields.

---

## P1 — Major Functional Gaps

### 1.1 — Tracking

#### 1.1.1 Exercise types: add Running and Strength Training

**File:** `COPDFuel/COPDFuel/Views/TrackingView.swift`, `ExerciseTrackingModal`

Replace `let exerciseTypes = ["Walking", "Cycling", "Swimming", "Yoga", "Breathing Exercises", "Other"]` with the Android list: `["Walking", "Running", "Cycling", "Swimming", "Yoga", "Strength Training", "Breathing Exercises", "Other"]`. Match the spinner ordering.

#### 1.1.2 USDA online food search

**Files:**
- `COPDFuel/COPDFuel/Services/FoodDatabaseService.swift` — add `func searchUSDA(query: String) async throws -> [FoodSearchResult]` using `URLSession` against `https://api.nal.usda.gov/fdc/v1/foods/search` with the same nutrient-ID mapping Android uses (1008/1003/1005/1004 + the micronutrient IDs the new model needs).
- `COPDFuel/COPDFuel/Config/AppConfig.swift` — add `usdaApiKey` (mirrors Android `BuildConfig.USDA_FDC_API_KEY`). Read from `Info.plist` build config so the key isn't checked in.
- `COPDFuel/COPDFuel/Views/TrackingView.swift`, `FoodTrackingModal` — add a segmented control "Local / USDA" above the search field, matching Android `dialog_add_food.xml` toggle. When USDA is selected, hide the category/food-group filters (just like Android).

#### 1.1.3 Create-Meal flow

**Files (new):**
- `COPDFuel/COPDFuel/Views/CreateMealSheet.swift` — sheet with: meal name field, meal-category picker (Breakfast/Lunch/Dinner/Snacks), a list of added items with remove buttons, an "Add food to meal" button that pushes the existing `FoodTrackingModal` configured in `addToMeal` mode (i.e., onSave appends to the meal instead of writing a `FoodEntry`).
- Update `TrackingView.swift` to expose a "Create Meal" button next to "Add from Favorites" (mirrors Android `binding.createMealButton`).
- Update `FoodTrackingModal` to accept an `onAddToMeal: ((FoodEntry) -> Void)?` callback path. When non-nil, the title becomes "Add food to meal" and the primary CTA is "Add to meal" (matching `AddFoodDialog.kt:94-117`).

#### 1.1.4 Serving-size picker in food add

**File:** `COPDFuel/COPDFuel/Views/TrackingView.swift`, `AddFoodConfirmView`

Replace the single "Amount" field with a serving picker like Android `AddFoodDialog.setupDefaultServingSize` / `selectFoodResult`:
- Build a `[ServingOption]` from the selected `FoodSearchResult`'s `servingSizes` (extend `FoodSearchResult` to carry these; populated from the local DB rows).
- Picker shows labels like `"slice — 28g"`, `"100g"`, `"g (enter amount)"`.
- The amount field multiplies the picker's `grams/100` factor.
- Nutrition display recomputes live on either change.

#### 1.1.5 Medication Discontinue action

**Files:**
- `COPDFuel/COPDFuel/Models/Medication.swift` — add `isDiscontinued: Bool` (default false), `discontinuedDate: Date?`.
- `COPDFuel/COPDFuel/Services/DataManager.swift` — add `discontinueMedication(_ med: Medication)` that stamps `discontinuedDate = Date()` and persists.
- `COPDFuel/COPDFuel/Views/MedicationListView.swift` — three sections now (Daily, Exacerbation, Discontinued — last only shown if non-empty). Each active row has both a "Discontinue" button (icon: `nosign`, orange tint) and a Delete (swipe + confirmation alert). Discontinue triggers a confirmation alert: **"Discontinue Medication"** / **"Mark \"<name>\" as discontinued? It will be moved to the discontinued list with today's date."** / Buttons **"Discontinue" / "Cancel"** (exact wording from `MedicationsDialogFragment.kt:60-66`).
- Add Delete confirmation: **"Remove Medication"** / **"Permanently remove \"<name>\" from the list?"** / **"Remove" / "Cancel"** (matches `MedicationsDialogFragment.kt:77-83`).

#### 1.1.6 Wire up Steps & Heart Rate tracker cards

**File:** `COPDFuel/COPDFuel/Views/TrackingView.swift`

Remove the empty `action: {}` from the Steps and Heart Rate cards (lines 139–151). Replace with a sheet that shows the last 7 days of imported data (since these are read-only from HealthKit, the sheet just shows recent values + a deep link to "Import from Apple Health" if empty). Mirrors Android's day-view behavior of showing imported step/HR counts inline.

#### 1.1.7 Wire macro targets to profile-calculated protein

**File:** `COPDFuel/COPDFuel/Views/TrackingView.swift`, `DayMacroTargetsView`

Replace the hard-coded `proteinTarget = 65.0` with `UserDefaults.standard.double(forKey: "protein_target")`. If 0, fall back to 65. Energy/carbs/fat targets stay constant for now (Android also hardcodes those).

### 1.2 — Home

#### 1.2.1 Wire "Explore Guidelines" button

**File:** `COPDFuel/COPDFuel/Views/HomeView.swift` (line 69)

Replace the empty `action: {}` with a tab-switch. Since SwiftUI tab selection lives in `MainTabView`, lift the selected-tab state to an `@AppStorage("selectedTab")` or pass a binding down to `HomeView`. The button sets the tab to the Guidelines index.

#### 1.2.2 Remove the dead web footer

**File:** `COPDFuel/COPDFuel/Views/HomeView.swift` (lines 202–268)

Delete the `FooterSection` and `FooterLink` structs entirely, plus the `FooterSection()` call inside the main `VStack`. The 15 link rows are non-functional and have no Android counterpart.

#### 1.2.3 Remove the empty "Healthy food" heading section

**File:** `COPDFuel/COPDFuel/Views/HomeView.swift` (lines 85–98)

Delete `HealthyFoodSection` and its call site. It's a heading with no content underneath and has no Android counterpart.

### 1.3 — Profile

#### 1.3.1 Add the 4 missing activity-level options

**File:** `COPDFuel/COPDFuel/Views/ProfileView.swift`

Replace the `activityLevels` array with the Android-canonical 7:
```swift
private let activityLevels = [
    ("", "Select activity level"),
    ("low", "Low Activity"),
    ("moderate", "Moderate Activity"),
    ("pulmonary_rehab", "Pulmonary Rehab"),
    ("high", "High Activity"),
    ("exacerbation", "COPD Exacerbation"),
    ("kidney_disease", "Kidney Disease"),
    ("dialysis", "On Dialysis"),
]
```

#### 1.3.2 Expand protein-target calculation

**File:** `COPDFuel/COPDFuel/Views/ProfileView.swift`, `computeProteinTarget()`

Replace with the Android `updateProteinTarget()` switch (lines 467–504). Save the computed midpoint to `UserDefaults.standard.set(target, forKey: "protein_target")` so Tracking can read it. Also save `protein_target_low` and `protein_target_high` for the report generator.

#### 1.3.3 Add "None" exclusive health-conditions toggle

**File:** `COPDFuel/COPDFuel/Views/ProfileView.swift`

Add a `Toggle("None", isOn: $hasNoConditions)`. On change to `true`, set all other condition toggles to `false`. On change of any other condition to `true`, set `hasNoConditions = false`. Persist under `condition_none`.

#### 1.3.4 Add Supplemental-Oxygen Yes/No control

**File:** `COPDFuel/COPDFuel/Views/ProfileView.swift`, "Supplemental Oxygen" section

Add `Toggle("Uses Supplemental Oxygen", isOn: $usesOxygen)`. Show the LPM field only when `usesOxygen == true`. Persist under `uses_oxygen` (matches Android pref key).

#### 1.3.5 Show "Last Updated" timestamp

**File:** `COPDFuel/COPDFuel/Views/ProfileView.swift`

Add a final read-only row at the bottom of Personal Information: `Text("Last updated: \(lastUpdated)")` reading from `UserDefaults.standard.string(forKey: "last_updated")`. Every `saveProfile()` and `saveConditions()` and onChange of activity/oxygen/BIPAP fields should also write today's date.

### 1.4 — Programs Near Me

#### 1.4.1 Manual address input

**File:** `COPDFuel/COPDFuel/Views/ProgramsNearMeView.swift`

Add a TextField "Enter city, address, or ZIP" and a "Search" button. On submit, use `CLGeocoder().geocodeAddressString(...)` (MapKit equivalent of Android's Geocoding API call) to convert text → `CLLocationCoordinate2D`, then `performSearch(query:near:)` against that region. Show the geocoded formatted address as confirmation, like Android shows `formatted_address`.

#### 1.4.2 Sample-data fallback

**File:** `COPDFuel/COPDFuel/Views/ProgramsNearMeView.swift` (or a new `SamplePrograms.swift`)

Port the 5 sample programs verbatim from `ProgramsNearMeFragment.kt:90-161` (City General, Bay Area Respiratory, Golden Gate Pulmonary, Community Health, Stanford). Show them when: location denied, MapKit returns zero results, or the user taps "Show Sample Data".

#### 1.4.3 Enrich result cards

**File:** `COPDFuel/COPDFuel/Views/ProgramsNearMeView.swift`, `NearbyProgramCard` & `NearbyProgram`

Add fields and UI for: `distance: String`, `rating: Float`, `specialties: [String]`, `hours: String`. Render distance ("2.3 miles"), 1-to-5 stars from `rating`, the specialties as tag pills (mirroring Android `specialty_tag_background`), and the hours line. For MapKit results, derive distance from `MKMapItem.location.distance(from:)`; for sample data, use the literal strings; for rating/specialties/hours from MapKit, leave blank if unavailable (the Android code also shows "Hours not available" / "Hospital, Healthcare" defaults).

#### 1.4.4 "Show Sample Data" button + text filter

Add a `Button("Show Sample Data")` and a search-within-results TextField that filters `results` by name/city/specialty (mirrors Android `filterPrograms`).

#### 1.4.5 Call & Directions confirmation dialogs

Wrap the existing `Link(destination: tel://...)` and Directions button in confirmation alerts matching Android's `showCallDialog` / `showDirectionsDialog` copy:
- Call: title "Call Program", body "Would you like to call <name> at <phone>?", buttons "Call" / "Cancel".
- Directions: title "Get Directions", body "Would you like to get directions to <name>?", button "Maps" / "Cancel".

### 1.5 — Recipes & Guidelines

#### 1.5.1 Recipes — 3-section structure + numbered ordering

**File:** `COPDFuel/COPDFuel/Views/RecipesView.swift`

Restructure to render three sections — **Breakfast Ideas**, **Lunch & Dinner Ideas**, **Snack Ideas** — each with the Android numbered ordering (`1.`, `2.`, …) prefixed to titles.

#### 1.5.2 Recipes — adopt Android source-of-truth content

Replace the per-recipe titles, descriptions, and tags with Android's canonical content from `RecipesFragment.kt` (Greek Yogurt + Berries + Walnuts; Scrambled Eggs + Spinach + Whole-grain Toast; etc.). Drop the iOS prose `description` field; use Android's `name` + nutrient bullets (each rendered with `•`).

#### 1.5.3 Remove iOS-only "Tips for Cooking" section

Delete the iOS-only 5-tip "Tips for Cooking with COPD" block. (Android has no equivalent; spec direction is Android-as-truth.)

#### 1.5.4 Guidelines — add "Preventing COPD Exacerbations" framing

**File:** `COPDFuel/COPDFuel/Views/GuidelinesView.swift`

Replace the bare "Strategies" header above the 3 strategy rows with:
- Title: "Preventing COPD Exacerbations"
- Subtitle: "Strategies to reduce flare-ups and maintain lung function"
- Body paragraph: `"To prevent COPD exacerbations, it is crucial to take various precautions. Besides performing breathing exercises and engaging in physical activity, maintaining a healthy diet is extremely important for lung function. Specifically, the amount of protein in one's diet can significantly impact lung health. Proper nutrition, combined with regular exercise and respiratory therapies, can help manage COPD and improve patients' quality of life."`
- Then the 3 existing strategy rows.

#### 1.5.5 Guidelines — add "The Importance of Protein" section

Add a new section after the Strategies block:
- Title: "The Importance of Protein"
- Body: `"Research shows that COPD patients with adequate protein intake have better outcomes. Protein helps maintain respiratory muscle mass and function, which can decline in COPD patients."`
- Bold inline ending: `"Aim for 20-30 g protein per meal."`

#### 1.5.6 Guidelines — add "Recommended Foods for COPD" section

Add after Importance of Protein:
- Header: "Recommended Foods for COPD"
- Intro: "Making smart food choices can help manage COPD symptoms and improve your overall health."
- Subsection "Foods to Embrace" with 5 items rendered with a green `+` marker:
  - "Fresh Fruits and Vegetables — Rich in antioxidants and fiber, they help reduce inflammation and support immune function."
  - "Lean Proteins — Fish, poultry, beans, and tofu provide essential amino acids without excess calories."
  - "Whole Grains — Brown rice, whole wheat bread, and oats provide sustained energy and important nutrients."
  - "Healthy Fats — Olive oil, avocados, nuts, and fatty fish contain omega-3s that may help reduce inflammation."
  - "Dairy or Fortified Alternatives — Good sources of calcium and vitamin D for bone health, especially important if taking steroids."
- Subsection "Foods to Limit or Avoid" with 4 items rendered with a red `–` marker:
  - "Processed Foods — Often high in sodium, preservatives, and artificial ingredients that may worsen inflammation."
  - "Gas-Producing Foods — Beans, cabbage, and carbonated beverages can cause bloating that makes breathing uncomfortable."
  - "Excessive Salt — Can lead to fluid retention, making it harder to breathe and potentially raising blood pressure."
  - "Cold Foods — Very cold foods and beverages may trigger coughing or breathing difficulties in some individuals."

#### 1.5.7 Wire Symbicort / Breztri buttons

**File:** `COPDFuel/COPDFuel/Views/GuidelinesView.swift`, Medication tab

Replace the empty `action: {}` on each button with a sheet showing the corresponding inhaler guide content. Source-of-truth content for the two guides lives on Android `ResourcesFragment.kt`; port it into a new `InhalerGuideView(brand: .symbicort | .breztri)`.

### 1.6 — Paywall, Billing & Report

#### 1.6.1 Advertise free trial on Paywall

**Files:**
- `COPDFuel/COPDFuel/Views/PaywallView.swift` — Update subtitle to "1 week free, then $9.99/month or $99/year. Unlock full health report sharing and support COPD Fuel." (matches Android `paywall_subtitle`).
- Each Subscribe button shows "1 week free, then <price>".
- `COPDFuel/COPDFuel/COPDFuel.storekit` — Add introductory free-trial offer to the yearly product to match the marketing copy.

#### 1.6.2 Restore feedback

**File:** `COPDFuel/COPDFuel/Views/PaywallView.swift`, `restore` action

After `AppStore.sync()`, check `StoreManager.shared.isPremium`:
- True → alert "Premium restored." then dismiss.
- False → alert "No subscription found.".

#### 1.6.3 Email subject for shared report

**File:** `COPDFuel/COPDFuel/Views/ProfileView.swift`, `shareReport`

Construct the share with both the report text **and** a subject. Pass a `LinkPresentationMetadata` or use the `[String: Any]` trick: include `["activityItemsConfiguration"]` with `mailComposeViewController` subject set to "COPD Fuel Report - <MMM d, yyyy>". Simplest reliable approach: wrap report text in a custom `UIActivityItemSource` that implements `subjectForActivityType` returning the dated subject. Mail/Messages will use it; other apps ignore.

#### 1.6.4 Expand iOS Report generator

**File:** `COPDFuel/COPDFuel/Services/ReportGenerator.swift`

Add all missing sections by porting `android/.../utils/ReportGenerator.kt` (~1:1 mapping):
- **PATIENT PROFILE:** Age, Sex, Weight, Height, BMI, Activity Level (with description), Daily Protein Target (low–high), Health Conditions checklist (kidney/cholesterol/pulm-hyper/dialysis/none/other-text), Supplemental Oxygen Y/N + LPM, BIPAP/NIV Y/N + machine + setting, Doctor name/phone, Emergency contact name/phone, Profile Last Updated.
- **TRACKING SUMMARY:** Severity-assessment fields saved by the Resources Severity Calculator (FEV1 %, hospitalizations, exacerbations, uses oxygen, severity result, description, saved date) + the Android disclaimer.
- **CARE PLAN NOTES (TEMPLATE):** `action_plan_instructions` free-text + the Android boilerplate template.
- **MEDICATIONS:** Daily + exacerbation lists (already present); also include Discontinued.
- **TRACKING DATA (Last 30 Days):**
  - NUTRITION SUMMARY: days logged, total entries, daily averages for all macros + micros (calories/protein/carbs/fat/fiber/sodium/potassium/water), **Protein Target Progress** (target/avg/% achievement), Individual Food Entries grouped by day.
  - EXERCISE SUMMARY: total sessions, days exercised, total minutes, avg/session, by-type breakdown, individual sessions per day.
  - EXERCISE JOURNAL: load from a new `UserDefaults` key `"exercise_journal_log"` (analogous to the Android pref) and emit entries with name/time/sets/reps/weight/activity/warm-up.
  - OXYGEN: total, average, min, max, distribution buckets <88 / 88–92 / >92.
  - WEIGHT: count, starting, current, change (signed), goal, distance-to-goal.
  - WATER INTAKE: days tracked, total oz, daily avg, 64-oz goal, days-met-goal.
  - STEPS & HEART RATE: keep the iOS-existing sections.
- **FOOTER:** replace the single sentence with the Android 3-sentence disclaimer:
  `"This report is for personal tracking and information sharing only. COPD Fuel does not provide medical advice, diagnosis, or treatment recommendations. If you have questions about your health, consult a licensed clinician."`

### 1.7 — Onboarding & Auth

#### 1.7.1 HIPAA copy gaps in legal text

**File:** `COPDFuel/COPDFuel/Views/HipaaAuthorizationView.swift`

Update the long legalese block to add the missing clauses from `android/.../res/values/hipaa_authorization_text.xml`:
- Section 1: enumerate recipients as bullets ("My physician or other health care provider / A family member or relative / A caregiver or other person I choose") + closing sentence "I may identify specific individuals within the app or through a separate written designation."
- Section 2: enumerate PHI categories as bullets, add preamble "including related records, data, communications, or documentation (check all that apply)", add parenthetical "(oxygen therapy, inhalers, respiratory devices, etc.)" to medical equipment usage data.
- Section 4: add "I may revoke this Authorization at any time through the COPD Fuel platform or by emailing support@copdfuel.com." Restore "and may be subject to redisclosure" in the redisclosure sentence. Add "I have the right to receive a copy of this signed Authorization in electronic form." Add "Revocation will not apply to uses or disclosures made before the effective date of revocation. COPD Fuel may rely on this Authorization until a valid revocation is received."
- Section 5: add ending "It will terminate upon my written revocation or upon closure or deactivation of my account."
- Section 6: add "COPD Fuel will not be liable for unauthorized access resulting from my sharing or failing to safeguard my login credentials."

#### 1.7.2 Add the plain-language consent body

**File:** `COPDFuel/COPDFuel/Views/HipaaAuthorizationView.swift`

Add a second prose block before the legal text reading the Android `@string/health_info_consent_body` content (the "By signing, you allow COPD Fuel, owned by Ingenious Medical Solutions, LLC… What information is shared? … Why is it being shared? … Important Things to Know: Voluntary / You Can Change Your Mind / Expiration / Privacy Note" body). Use Android `strings.xml:17-18` verbatim.

#### 1.7.3 Reachability: ensure HipaaGate covers all entry points

This is covered by P0 items 0.1–0.3 plus existing Profile entry. After P0 lands, every gated action (sync, share, link doctor) routes through HipaaGate, so the HIPAA modal is reachable from natural user flows.

#### 1.7.4 "Use a different email" cleanup

**File:** `COPDFuel/COPDFuel/Views/SignUpView.swift` (lines 125-136)

In the action handler, additionally clear `name`, `email`, `password`, `signUpEmail`, `signUpPassword` (mirrors `SignUpActivity.kt:30-32`).

---

## P2 — Polish

### 2.1 Delete dead SplashView

**File:** `COPDFuel/COPDFuel/Views/SplashView.swift`

The file is never referenced — `COPDFuelApp` mounts `RootView` directly. Delete the file and remove from Xcode project. (Confirmed nothing else imports it.)

### 2.2 Signature Pad improvements

**File:** `COPDFuel/COPDFuel/Views/SignaturePadView.swift`

- Replace `addLine(to:)` with `addQuadCurve(to: midpoint, controlPoint: previousPoint)` for Bezier smoothing matching `SignaturePad.kt:65-71`.
- Increase stroke width to 4pt and use the brand primary color (`Color("BrandPrimary")`).
- Add `func renderSignatureImage() -> UIImage?` that draws the current path into a `UIGraphicsImageRenderer` — useful for embedding the signature image in the report (parity with Android `getSignatureBitmap()`).

### 2.3 DOB picker improvements (optional)

**File:** `COPDFuel/COPDFuel/Views/HipaaAuthorizationView.swift`

Either keep the stock `DatePicker(.wheel)` (acceptable iOS idiom — `A` decision) OR build a custom MM/DD/YYYY-text-input dialog matching Android. **Decision: keep stock**, since `A` (platform-idiomatic equivalents) was approved. Only enforce the 1900 floor by setting `in: Calendar.current.date(from: DateComponents(year: 1900, month: 1, day: 1))!...Date()`.

### 2.4 Login screen — Forgot Password disposition

**File:** `COPDFuel/COPDFuel/Views/LoginView.swift` (lines 45-49, 175-181)

Remove the "Forgot password?" button entirely (it currently opens a "not wired up" alert). Android has no equivalent. Implementing real password reset is out of scope.

### 2.5 Consistent style polish

A pass over font sizing, padding, and color usage in any iOS view touched during P1, ensuring it matches the Android visual rhythm where the audit flagged drift (e.g., uniform card background `Color(.systemGray6)`, headline weight on section titles).

---

## Architecture Notes

### Persistence keys: align iOS `UserDefaults` keys to Android `SharedPreferences` keys

iOS already uses Android-style keys in most places. Audit pass to confirm/align:
- `condition_kidney_disease` (Android), `condition_kidney` (iOS) — **rename iOS to match Android**. Add a one-shot migration in `DataManager.init` that reads old key and writes new key.
- `condition_cholesterol` (iOS), `condition_high_cholesterol` (Android) — **rename iOS to match Android**.
- `condition_other` (iOS, stores text), Android has `condition_other` (boolean) + `condition_other_text` (string) — **split iOS into two keys**.
- Other keys (`uses_oxygen`, `oxygen_lpm`, `uses_bipap`, `bipap_machine_type`, `bipap_setting`, `activity_level`, `protein_target`, `last_updated`) already match.

### iOS HipaaGate implementation pattern

```swift
@MainActor
enum HipaaGate {
    static func hasConsent() -> Bool {
        HipaaConsentStorage.shared.hasValidConsent()
    }

    static func requireConsent(action: String) -> RequireConsentResult {
        if hasConsent() { return .granted }
        return .needsConsent(action: action)
    }
}

enum RequireConsentResult {
    case granted
    case needsConsent(action: String)
}
```

Call sites present the modal themselves via a `@State var hipaaPrompt: HipaaPromptState?`. Avoids global UIWindow manipulation and keeps SwiftUI declarative.

### Backend sync gating

`RootView.registerAndSyncIfNeeded` becomes:
```swift
guard HipaaGate.hasConsent() else { return }
// existing sync calls
```
Additionally, set a listener so that when the user signs HIPAA from any screen, sync is triggered. Use a `NotificationCenter.Notification.Name(rawValue: "HipaaConsentChanged")` posted by `HipaaConsentStorage` on save/revoke, observed by `RootView`.

### Food model migration

The expanded `FoodEntry` struct adds 19 new `Double` properties. Decoding old data (where keys are absent) must succeed. Decision: declare all new fields with default values (`= 0.0`) and use a custom `init(from:)` that catches missing-key errors per property. Same for `FavoriteFood`/`FavoriteMeal`/`UserAddedFood`.

For the food database SQLite/JSON source file (`fooddata.xlsx`-derived), share the same data file as Android. The Android `assets/fooddata.json` lives at `android/app/src/main/assets/`. iOS should bundle the same JSON (copied into `COPDFuel/COPDFuel/Resources/` and added to the Xcode target). `FoodDatabaseService` loader updated to read all 23 columns.

---

## Phasing & Sequencing

**Phase 0 (P0) — Must ship together as a single PR/branch:**
- 0.1 HipaaGate → 0.2 Sync gate → 0.3 Share gate → 0.4 Revocation persistence → 0.5 Food model expansion.
- Reason: 0.5 is a data-model change that cascades to P1 Tracking and P1 Report. Without it, those P1 items can't be implemented cleanly.

**Phase 1 (P1) — Can be split into parallelizable sub-PRs:**
- **Branch A:** Tracking (1.1.1–1.1.7).
- **Branch B:** Profile (1.3.1–1.3.5) — required input for Report (1.6.4).
- **Branch C:** Home (1.2.1–1.2.3) — small, independent.
- **Branch D:** Programs Near Me (1.4.1–1.4.5).
- **Branch E:** Recipes & Guidelines content (1.5.1–1.5.7).
- **Branch F:** Paywall/Billing (1.6.1–1.6.3).
- **Branch G:** Report Generator (1.6.4) — depends on B for protein target & profile fields.
- **Branch H:** HIPAA copy (1.7.1, 1.7.2, 1.7.4).

**Phase 2 (P2) — Polish, ship any time after P1:**
- 2.1 SplashView delete, 2.2 Signature Pad, 2.3 DOB floor, 2.4 remove Forgot Password, 2.5 style polish.

---

## Out of Scope (explicit non-goals)

- Real password reset implementation (deferred; P2 just removes the stub button).
- Server-side receipt validation (both platforms are client-only today).
- Doctor portal changes.
- React Native `my-copd-app/` codebase (treated as legacy; not synced).
- Pixel-perfect SwiftUI → Material Design replication of color/spacing tokens. Visual idioms remain platform-native; *content and behavior* match Android.
- Switching iOS away from Amplify Auth to a hand-rolled Cognito client (Android's choice). Both produce equivalent behavior against the same user pool; spec C does not require library parity.

---

## Verification Plan (per phase)

Each phase ships only when verified:

**P0 verification:**
- Manually sign out → sign in → confirm no `/sync/*` requests fire until HIPAA is signed.
- Manually tap "Share Health Report" without HIPAA → confirm gate alert appears.
- Toggle revoke → confirm UI shows "Revoked on <date>" and the form re-renders for resign.
- Create a food entry with full macros → quit/reopen app → confirm all 23 fields persist.

**P1 verification (per area):**
- Tracking: log a meal manually + via USDA online + create a meal + restore a favorite → confirm all 23 fields round-trip. Add an exercise of type Strength Training. Discontinue a medication and confirm it appears in the Discontinued section.
- Home: tap "Explore Guidelines" → Guidelines tab opens. Confirm footer and "Healthy food" heading are gone.
- Profile: change activity to "On Dialysis" → confirm protein target shows 1.2–1.8 × weight_kg range. Toggle "None" → confirm other condition toggles disable.
- Programs: type "Boston, MA" → confirm map repositions and results refresh. Deny location → confirm sample data appears. Confirm distance/stars/specialties/hours render.
- Recipes: confirm 3 numbered sections present, recipe names match Android verbatim, no iOS-only Tips section.
- Guidelines: confirm Preventing-Exacerbations, Importance-of-Protein (with bolded last sentence), and Recommended Foods (Embrace/Limit with +/– markers) sections present.
- Paywall: open paywall offline → confirm Try Again button works. Tap Restore with no prior purchase → confirm "No subscription found." alert.
- Report: generate report with realistic 30-day data → diff against an Android-generated report on the same data → confirm structure and content match.
- HIPAA legal text: diff iOS rendered text against `hipaa_authorization_text.xml` → confirm all clauses present.

**P2 verification:**
- SplashView file gone, project still builds.
- Signature Pad strokes are smooth, signature image embeds in the report.

---

## Open Questions (deferred to writing-plans)

- Should `HipaaGate` use a global `NotificationCenter` post on consent change, or rely on SwiftUI's `@ObservedObject` from `HipaaConsentStorage`? (Leaning `@ObservedObject` to keep things SwiftUI-native; falls back to Notification only for non-View callers like backend sync.)
- USDA API key handling on iOS: ship via `xcconfig` file in `Config/`? (Matches Android `local.properties` pattern.)
- For the food-DB JSON copy, should iOS read the same `fooddata.json` Android uses, or maintain a separate generated file? (Recommendation: single source — copy on build via Run Script phase, or check in two copies and accept the duplication.)
