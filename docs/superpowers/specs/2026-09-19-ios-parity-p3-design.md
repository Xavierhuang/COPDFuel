# iOS → Android Parity, Phase 3 (P3): Close Every Remaining Gap

**Date:** 2026-09-19
**Scope:** Bring the native iOS app (`COPDFuel/`, SwiftUI) to full feature, content and behavior parity with the native Android app (`android/`, Kotlin) **as of Android `main` commit `5c4aa49`** (Meals rename). Android is the source of truth.
**Supersedes:** the open items of `2026-05-28-ios-android-parity-design.md` (P0, P1.A, P1.B-part-1, P1.C, P1.D landed in the iOS repo; the rest of P1, P2, and everything Android added between May and September is covered here).
**Ground truth for implementers:** the five audit reports in `2026-09-19-ios-parity-p3/` (one per feature area). Every numbered gap there is in scope unless a decision below excludes it.

---

## 1. Principles (carried over from the approved May spec, plus new decisions)

1. **Content and behavior match Android verbatim.** Screen titles, section headings, body copy, button labels, field hints, empty states, validation messages, dialog titles/messages/buttons, ordering, default values, computed values and persisted keys all match Android's `strings.xml` / layouts / Kotlin. Where Android shows a `Toast`, iOS shows a transient toast overlay with the same text (new `ToastCenter` + `.toastOverlay()` modifier). Where Android shows an `AlertDialog`, iOS shows an `.alert` / `.confirmationDialog` with the same copy.
2. **Platform idioms are not gaps.** SwiftUI `Form`/sheets for editing, SF Symbols instead of emoji/vector icons, `DatePicker` wheels instead of NumberPickers, MapKit instead of Google Places, Apple Maps instead of Google Maps, HealthKit instead of Health Connect, StoreKit instead of Play Billing, Amplify Auth instead of the hand-rolled Cognito client, `UIActivityViewController` instead of three share intents. Visual tokens (dp/sp sizes, exact hex colors) are matched where cheap, but a pixel-perfect Material clone is out of scope.
3. **Android bugs are not ported.** Specifically:
   - Exercise / oxygen / weight / water on Android are stamped with "now" even when viewing a past day (Tracking audit gap 7). iOS keeps stamping the selected day.
   - Android never persists `condition_kidney_disease` / `condition_dialysis` from the checkbox (Profile audit A8). iOS persists them.
   - Health Connect weight is stored in kg while the app treats weights as lbs (Health audit F3). iOS stores lbs.
   - The Android Profile "Terms of Use" row has no handler (A12). iOS keeps the working link.
   - `LabelReviewActivity` re-runs OCR (Scan spec §0). iOS parses once.
   - Camera instruction text not reset after a label-read exception (Scan spec §1.2.1 step 5). iOS resets it.
4. **iOS-only extras are removed when they conflict with Android content, and kept when they are harmless additive platform behavior.**
   - **Remove:** the iOS severity *formula and classification* (Android deliberately replaced it with a "save what you entered" summary — this is a medical-advice liability fix and must be mirrored); the iOS Guidelines "Dietary / Medication" segmented control and its Medication segment (medication content lives in Resources on Android); the iOS-only Resource Hub entries (NHLBI org, Cleveland-Clinic items, Emergency Resources); the "Recommended Exercises" list in Pulmonary Rehab; the week-view Summary tiles, the Day Summary 6-tile grid and the per-week "Weekly Exercise Breakdown" cards (Android removed them); Profile "Goal Weight" field (goal lives only in the weight log on Android); the Login "Forgot password?" stub (May spec 2.4); the Link-Doctor invite-code field.
   - **Keep:** the "References & sources" panel in Guidelines, rendered **once** at the bottom (App Store Review Guideline 1.4.1 asks for medical-content sources; this is an iOS platform requirement, not content drift); friendly Cognito error-message mapping; HealthKit workout-type names (Walking, Running, …); the STEPS / HEART RATE sections at the end of the report; working Terms of Use link; iOS re-runs backend sync when HIPAA flips to signed.
5. **Persisted keys align with Android** wherever another screen or the report reads them, with one-shot migrations from the old iOS keys. Affected: profile fields (flat `age`, `sex`, `weight`, `height`, `doctor_name`, `doctor_phone`, `emergency_contact_name`, `emergency_contact_phone` replace the `userProfile` JSON blob and `profile_weight`), `emergency_contact2_name`, `emergency_contact2_phone`, `insurance_provider`, severity keys (`severity_exacerbations`, `severity_assessment_date` string, `severity_description`), `exercise_journal_log` (Android JSON schema, String), `doctor_instructions_log` (Android JSON schema), HIPAA keys (`hipaa_revoked`, `hipaa_revoked_date`, `hipaa_consent_printed_name`, ms-vs-s is internal and stays seconds), `protein_target` read as Double.
6. **Every phase must build** (`xcodebuild -scheme COPDFuel -destination 'generic/platform=iOS Simulator'`) and the pure-logic package tests must pass (`swift test`) before it is committed to the `COPDFuel` repo.

---

## 2. Architecture additions

### 2.1 `ToastCenter`
`COPDFuel/COPDFuel/Services/ToastCenter.swift`: `@MainActor final class ToastCenter: ObservableObject` with `static let shared`, `@Published var message: String?`, `func show(_ text: String)` (auto-clears after 2.5 s, replacing any current toast). `View.toastOverlay()` modifier installed once in `MainTabView` and once in each full-screen cover that hosts its own window-level UI (scan flow, HIPAA sheet, paywall). Renders a capsule at the bottom, above the tab bar, matching Android's `Toast.LENGTH_SHORT` feel.

### 2.2 Tab-selection binding
`MainTabView` owns `@State selectedTab`. `HomeView` already receives it; `TrackingView` receives it too so "Set Up" (profile banner) and the "Set up" protein link switch to the Profile tab (index 5) instead of pushing.

### 2.3 `LabelScanKit` local Swift package
`COPDFuel/LabelScanKit/` (Package.swift, `Sources/LabelScanKit`, `Tests/LabelScanKitTests`). Pure Foundation code, no UIKit/Vision: `ParsedLabel`, `ExtraNutrient`, `OcrLine`, `NutritionLabelParser`, `ProductNames`, `GTIN` (extract/check-digit/candidates/sameGtin), `ProductLinkResolver` (injectable fetch), `USDAGtinLookup` (mapResponse/productName; injectable fetch), `MealCategories`. The Android unit tests (`NutritionLabelParserTest`, `FrontLabelNameTest`, `MlKitBarcodeScannerTest`, `ProductLinkResolverTest`, `UsdaGtinLookupTest`, `MealCategoriesTest`) are ported 1:1 to XCTest and run with `swift test --package-path COPDFuel/LabelScanKit`. The app links the package via a local package reference added to `project.pbxproj`.

Camera / OCR / barcode live in the app target: `Services/LabelScan/LabelOCR.swift` (Vision `VNRecognizeTextRequest`, `.accurate`, `usesLanguageCorrection = false`), `Views/Scan/ScanLabelView.swift` (AVCaptureSession + AVCapturePhotoOutput + AVCaptureMetadataOutput, single state machine `.qr / .frontPhoto / .nutritionPhoto`), `Views/Scan/LabelReviewView.swift`, `Views/Scan/AddFoodOptionsSheet.swift`. Info.plist gains `NSCameraUsageDescription`; ATS gains the `pepsico.info` insecure-HTTP exception (mirrors Android `network_security_config.xml`).

### 2.4 Flat profile storage
`DataManager` gains `ProfileStore` accessors over `UserDefaults` using Android's key names, and a one-shot migration `migrateUserProfileBlobIfNeeded()` that copies the `userProfile` JSON blob and `profile_weight` into the flat keys, then removes the blob. `ReportGenerator`, `ProfileView`, `TrackingView` and `ProfileWeightSync` read the flat keys.

### 2.5 `ProfileWeightSync`
`Services/ProfileWeightSync.swift`: port of Android's three functions (`syncPrefsFromWeight`, `syncPrefsFromRepositoryCurrentWeight`, `syncProfileAndDbWeight`) with the same formatting rule (1 decimal, integers without ".0") and `last_updated` stamping. Called from the weight modal, after Health import, and on Profile appear.

### 2.6 Health import dedupe
`DataManager` gains `addOxygenReadingIfAbsent(at:)`, `addHeartRateEntryIfAbsent(at:)`, `replaceStepsForDayFromImport(day:count:)`. `HealthKitImportView` uses a fixed 30-day window and Android's messages (adapted "Health Connect" → "Apple Health"). Live per-day steps read from HealthKit for the selected day when authorized, falling back to stored steps.

---

## 3. Phases

Each phase is one commit in the `COPDFuel` repo. Phases A, B, D, E, G touch disjoint files and can run in parallel; C must precede D (D wires into the new Tracking quick-add sheet) and E must precede F (F reuses the flat profile keys).

| Phase | Area | Audit report | Main files |
|---|---|---|---|
| P3.0 | Checkpoint of the uncommitted P1.B-part-2 work; `ToastCenter`; portrait-only; tab label "Meals" + SF Symbol mapping (`fork.knife`, `wrench.fill`, `chart.bar.fill`) | shell §A3, A5 | `MainTabView.swift`, `project.pbxproj`, new `ToastCenter.swift` |
| P3.A | Splash, Home, Guidelines, Meals | `audit-home-guidelines-meals-programs.md` §A1, B, C, D | `RootView.swift`, `HomeView.swift`, `GuidelinesView.swift`, `RecipesView.swift` |
| P3.B | Resources tab: header/tool row, Severity summary, Action Plan (contacts ×7, medication plan, instructions log), Pulmonary Rehab (full home program + journal), Medication Guide (10 guides + devices), Resource Hub | `audit-resources.md` | `ResourcesView.swift` (split into `Resources/*.swift`), new `MedicationTypes.swift`, `DailyTrackingSummary.swift` (journal parts) |
| P3.C | Tracking: header + day nav, day-view order, macro card, Fat/Minerals/Vitamins sections, collapsible meal sections, "+ Quick Add Food" sheet (4 options; scan options call into P3.D), AddFood dialog parity (source labels, explicit Search, filter browsing, editable name, serving options from DB, manual override, save-as-favorite prompt, add-to-database, add-to-meal mode), favorites dialog, create meal, hydration inline card, exercise/oxygen/weight/medication cards + dialogs + validation toasts, week/month Trend Reports, `FoodDatabaseService` 23-nutrient + servingSizes decoding, duplicate-name rules | `audit-tracking.md` | `TrackingView.swift` (split into `Tracking/*.swift`), `FoodDatabaseService.swift`, `DataManager.swift`, `MedicationListView.swift`, `DailyTrackingSummary.swift` |
| P3.D | Label scan feature | `audit-scan-port-spec.md` | `LabelScanKit/`, `Services/LabelScan/`, `Views/Scan/`, `project.pbxproj`, Info.plist keys |
| P3.E | Profile (order, labels, hints, protein-target notes, always-visible BMI/Last Updated, weight sync, contacts incl. 2nd emergency + insurance), Report generator 1:1, Health import (30-day, dedupe, messages, live steps), Link doctor (`/consent` → `/link-doctor`), Delete account (`DELETE /me`) | `audit-profile-report-auth.md` §A, B, F, G, D6 | `ProfileView.swift`, `ReportGenerator.swift`, `COPDAPIClient.swift`, `HealthKitService.swift`, `DataManager.swift` |
| P3.F | Auth + HIPAA + Paywall: login/sign-up copy & validation, HIPAA screen order/copy/expiry validation/Clear bug/revoked-date/key alignment, paywall copy + error surfacing + restore copy | `audit-profile-report-auth.md` §C, D, E | `LoginView.swift`, `SignUpView.swift`, `HipaaAuthorizationView.swift`, `HipaaConsentStorage.swift`, `PaywallView.swift`, `StoreManager.swift` |
| P3.G | Programs Near Me: intro, "Choose Your Location" cards + status band, loading state, card layout (stars, "- distance", phone row, Specialties label, Call/Directions buttons), empty state + "Show Sample Data", footer, fallback matrix, toasts | `audit-home-guidelines-meals-programs.md` §E | `ProgramsNearMeView.swift` |

---

## 4. Verification

- Build: `xcodebuild -project COPDFuel/COPDFuel.xcodeproj -scheme COPDFuel -destination 'generic/platform=iOS Simulator' build` succeeds after each phase.
- Tests: `swift test --package-path COPDFuel/LabelScanKit` passes (parser, GTIN, link resolver, USDA mapping, meal categories).
- Manual smoke on the iPhone 17 simulator per phase: launch → login → each tab renders; Tracking day/week/month; add food via search, favorites, meal, manual; Resources tool switching; Profile edit + report share text; HIPAA sign/revoke; scan flow with Photo Library (camera is unavailable in the simulator).
- Copy check: a grep of representative Android strings (`strings.xml` and the hardcoded fragments) against the iOS sources for every heading/button listed in the audit reports.

## 5. Out of scope

Doctor portal, backend, React Native `my-copd-app/`, App Store submission, real password reset, server receipt validation, pixel-perfect Material styling, Google Places/Geocoding/Maps SDKs on iOS.
