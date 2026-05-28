# iOS Parity — Phase P1.A Implementation Plan

> Spec source: `docs/superpowers/specs/2026-05-28-ios-android-parity-design.md` §§ 1.2 (Home), 1.3 (Profile), 1.7.1, 1.7.2 (HIPAA copy), 1.7.4 (Sign-up cleanup).

**Goal:** Close 11 of the smaller P1 gaps in one sweep: wire up Home's broken button + drop two dead Home sections, fix Profile's missing activity-level branches / protein calc / None toggle / oxygen toggle / Last Updated row, port the missing HIPAA legal clauses and the plain-language consent body, and clear all sign-up form state on "Use a different email".

**Architecture:** Plain SwiftUI edits — no new services. Profile additions persist to `UserDefaults` with the same keys Android uses (`condition_none`, `uses_oxygen`, `last_updated`, `protein_target`, `protein_target_low`, `protein_target_high`), so the Report Generator (P1.F) can read them without further changes.

**Verification:** Single `xcodebuild -scheme COPDFuel` at the end. Commit at logical group boundaries (Home, Profile, HIPAA copy, Sign-up).

---

## Task 1 — Wire Home "Explore Guidelines" button + remove dead sections

**Files:**
- Modify: `COPDFuel/COPDFuel/MainTabView.swift` — lift `selectedTab` to a binding the Home view can mutate.
- Modify: `COPDFuel/COPDFuel/Views/HomeView.swift` — accept the binding, wire the Explore button, delete `HealthyFoodSection` + `FooterSection` + `FooterLink`.

### 1.1 MainTabView — pass a tab-selection binding into HomeView

Change the `TabView` so `HomeView` receives `$selectedTab` and the Explore button can set it to `1` (Guidelines).

### 1.2 HomeView — accept binding, wire Explore button, delete dead UI

`HomeView` becomes:
```swift
struct HomeView: View {
    @Binding var selectedTab: Int

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                HeaderSection()
                HeroSection(selectedTab: $selectedTab)
                UnderstandingCOPDSection()
            }
        }
        .edgesIgnoringSafeArea(.top)
    }
}
```
- Delete `HealthyFoodSection` and `FooterSection`+`FooterLink` structs.
- Update `HeroSection` to accept `@Binding var selectedTab: Int` and replace the empty button action with `selectedTab = 1`.

### 1.3 Commit

`iOS Home: wire Explore Guidelines, remove dead footer + healthy-food heading`

---

## Task 2 — Profile activity-level + protein-target expansion

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/ProfileView.swift`.

### 2.1 Expand `activityLevels` to the 7 Android options

Add `(high, "High Activity")`, `(exacerbation, "COPD Exacerbation")`, `(kidney_disease, "Kidney Disease")`, `(dialysis, "On Dialysis")` to the array.

### 2.2 Replace `computeProteinTarget()` with the Android formula

Mirror `ProfileFragment.updateProteinTarget()` (Android lines 442-516):
- low: 1.0–1.2
- moderate, pulmonary_rehab: 1.2–1.4 (Android uses 1.2-1.4; the original iOS code mistakenly used 1.5-1.7 for pulm_rehab — fix to match Android)
- high, exacerbation: 1.6–1.8
- kidney_disease: 1.0–1.2
- dialysis: 1.2–1.8

Persist `protein_target_low`, `protein_target_high`, and `protein_target` (midpoint) to `UserDefaults` so Tracking macros + Report read them.

### 2.3 Add "None" exclusive condition toggle

Add `@State private var hasNoConditions = false`, render `Toggle("None", isOn: $hasNoConditions)` in the Health Conditions section. On change to `true`, clear all other condition toggles. On any other toggle change → `false` set `hasNoConditions = false`. Persist under `condition_none`.

Also rename the persistence keys to match Android:
- `condition_kidney` → `condition_kidney_disease`
- `condition_cholesterol` → `condition_high_cholesterol`
- `condition_other` (string) → split into bool `condition_other` + string `condition_other_text`

Migrate on load: read either old or new keys.

### 2.4 Add Supplemental-Oxygen Yes/No toggle

Add `@State private var usesOxygen = false`, render `Toggle("Uses Supplemental Oxygen", isOn: $usesOxygen)` before the LPM field. Show LPM field only when `usesOxygen == true`. Persist under `uses_oxygen`.

### 2.5 Add "Last Updated" row

Add `@State private var lastUpdated = ""`. Render at end of Personal Information: `Text("Last updated: \(lastUpdated)")` (only if non-empty). Every `saveProfile`, `saveConditions`, and onChange of activity-level / oxygen / BIPAP writes today's date (`MMM d, yyyy`) to `last_updated` and updates the state.

### 2.6 Commit

`iOS Profile: full activity-level set, expanded protein calc, None toggle, oxygen Yes/No, Last Updated`

---

## Task 3 — HIPAA legal text + plain-language consent body

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/HipaaAuthorizationView.swift`.

### 3.1 Add plain-language `healthInfoConsentBody` view

Render before `hipaaFullText`, in a separate card. Body verbatim from `android/.../res/values/strings.xml` `health_info_consent_body`:

```
By signing, you allow COPD Fuel, owned by Ingenious Medical Solutions, LLC, to share your personal health information with the people you choose. This typically includes your doctor, but may also include family members, caregivers, or other people you select.

What information is shared?
Your COPD-related health data — including diagnoses, medications, oxygen and exercise tracking, dietary information, and clinical notes from your account.

Why is it being shared?
To support your care, communication with your care team, and ongoing health management.

Important Things to Know:
• Voluntary — You don't have to sign this form.
• You Can Change Your Mind — You may revoke this Authorization at any time. Once your information has been shared, it may not be possible to recall it.
• Expiration — This Authorization lasts as long as your COPD Fuel account is active, unless you revoke it earlier.
• Privacy Note — Once your information is disclosed, it may no longer be protected by HIPAA.
```

### 3.2 Update `hipaaFullText` clauses to Android-canonical content

Replace section bodies with Android wording. Important additions:

- **Section 1** — bullet recipients + closing sentence "I may identify specific individuals within the app or through a separate written designation."
- **Section 2** — preamble "including related records, data, communications, or documentation (check all that apply)" + parenthetical "(oxygen therapy, inhalers, respiratory devices, etc.)" on medical equipment usage data.
- **Section 4** — add: support email; "and may be subject to redisclosure"; "I have the right to receive a copy of this signed Authorization in electronic form."; "Revocation will not apply to uses or disclosures made before the effective date of revocation. COPD Fuel may rely on this Authorization until a valid revocation is received."
- **Section 5** — append: "It will terminate upon my written revocation or upon closure or deactivation of my account."
- **Section 6** — append: "COPD Fuel will not be liable for unauthorized access resulting from my sharing or failing to safeguard my login credentials."

### 3.3 Commit

`iOS HIPAA: plain-language consent body + missing legal clauses`

---

## Task 4 — Sign-up "Use a different email" clears all stale fields

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/SignUpView.swift`.

### 4.1 Expand the button action

Replace the existing action with:
```swift
needsConfirmation = false
confirmationCode = ""
errorMessage = nil
name = ""
email = ""
password = ""
confirmPassword = ""
```

### 4.2 Commit

`iOS SignUp: "Use a different email" clears all stale form state`

---

## Task 5 — Build verification

```bash
cd "/Users/weijiahuang/Desktop/client's project/COPD-2"
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel \
  -destination 'generic/platform=iOS Simulator' -quiet build
```
Expected exit code `0`.

---

## Out of scope here (deferred to other P1 sub-plans)
- Tracking changes (1.1) → P1.B
- Recipes/Guidelines content (1.5) → P1.C
- Programs Near Me (1.4) → P1.D
- Paywall/Billing (1.6) → P1.E
- Report Generator (1.6.4) → P1.F (depends on Profile keys this plan introduces)
