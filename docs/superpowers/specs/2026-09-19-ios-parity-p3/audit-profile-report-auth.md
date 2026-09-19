# Profile, Report, Paywall, Auth, HIPAA, Health Import, Account — Android → iOS gap list (audit 2026-09-19)

`A/` = `android/app/src/main/java/com/copdhealthtracker/`, `R/` = `android/app/src/main/res/`, `I/` = `COPDFuel/COPDFuel/`.

---

## (A) PROFILE TAB

### A1. Screen structure / ordering — DIFFERS
Android (`R/layout/fragment_profile.xml`, title "My Profile"): Personal (Age, Sex, Weight, Height, Body Mass Index (BMI), Last Updated) → Activity Level → "Health Conditions with COPD" (checkboxes + oxygen + BIPAP inside the same card) → Premium row → Privacy Policy row → Terms of Use row → HIPAA row → "Share Health Report" card → "Share with your doctor" card → Doctor info card (Doctor Name, Doctor Phone, Emergency Contact Name, Emergency Contact Phone) → "Sign out" button → "Delete my account" button.
iOS: different order and section titles; Delete before Sign out.

### A2. Field editing model — DIFFERS
- Android: tap row → `AlertDialog` "Edit {Title}" with hint "Enter {Title}", Save/Cancel; on Save writes pref + `last_updated`, toast "{Title} updated successfully!" (`ProfileFragment.kt:612-647`). Empty value ignored.
- iOS: inline `TextField`s saving on every keystroke; no toasts.
- Android **Sex** is free text. iOS Picker ["", "Male", "Female", "Other"] — acceptable platform idiom; keep picker but add "Prefer not to say"? No: keep Android's free-text semantics by using a TextField.
- Android **Height** two-NumberPicker dialog "Edit Height": feet 3–8, inches 0–11, default 5'6", saved as `5'6"`, toast "Height updated successfully!". iOS: two free text fields "ft"/"in" — acceptable, but must not save when both blank.
- Android displays "N/A" for unset values; Weight displayed as "{weight} lbs".

### A3. Persisted keys — PARTIAL MISMATCH
Android flat prefs: `age`, `sex`, `weight` (String lbs), `height` (String `5'6"`), `last_updated` ("MMM d, yyyy"), `doctor_name`, `doctor_phone`, `emergency_contact_name`, `emergency_contact_phone`, `condition_kidney_disease`, `condition_high_cholesterol`, `condition_pulmonary_hypertension`, `condition_dialysis`, `condition_none`, `condition_other` (bool), `condition_other_text`, `activity_level`, `uses_oxygen`, `oxygen_lpm`, `uses_bipap`, `bipap_machine_type` ("BIPAP"/"NIV"), `bipap_setting`, `protein_target` (Float), `protein_target_low` (Int), `protein_target_high` (Int).
iOS: age/sex/height/goalWeight/doctor*/emergency* in a JSON blob `userProfile` (`I/Models/UserProfile.swift`, `DataManager.swift:22,300-304`); weight is `profile_weight`. Others match. **Migrate to flat keys** (P3 spec §2.4).

### A4. Weight ↔ Tracking DB sync — MISSING on iOS
`ProfileWeightSync.syncProfileAndDbWeight` on every profile load (`ProfileFragment.kt:653-661`; `A/utils/ProfileWeightSync.kt:35-49`): if DB has a current (non-goal) weight differing from pref by >0.05 → pref := DB weight (1 decimal, integers without ".0") + `last_updated`; if DB empty and pref >0 → insert `WeightEntry(weight=pref, isGoal=false)`. Editing weight in Profile inserts a `WeightEntry` (`:631-638`). Tracking Add Weight writes the pref (`TrackingFragment.kt:2255`); Health import re-syncs (`:2220`).

### A5. Goal weight — iOS-only extra in Profile (remove; goal lives in weight log).

### A6. BMI — row "Body Mass Index (BMI)" always visible, "N/A" when not computable; formula `(lbs / in²) * 703`, `%.1f`; height parsed by splitting on non-digits: ≥2 numbers → ft*12+in; single number <25 → feet, else inches (`ProfileFragment.kt:752-769`).

### A7. Last Updated — row "Last Updated" always visible ("N/A" if unset).

### A8. Health Conditions — Android title "Health Conditions with COPD", subtitle "Please check all that apply (affects protein target):". Labels: "Kidney disease (including chronic kidney disease Stage 2)", "High cholesterol", "Pulmonary hypertension", "Currently on dialysis", "None of the above", "Other (please specify)", free text hint "Please specify other conditions" (multi-line). Behavior: "None" unchecks all others; "Other" unchecks None and clears text when unchecked. (Android bug: never persists kidney/dialysis — iOS keeps persisting them. Android does not uncheck None when kidney/cholesterol/PH/dialysis are checked — iOS may keep the stricter mutual exclusion.)

### A9. Activity level — title "Activity Level", prompt "Which option best describes your current activity level?"; radio labels + hints:
- "Low activity (limited movement or mostly sedentary)" + hint "1.0-1.2 g protein per kg body weight/day"
- "Moderate activity (regular daily activities with some exercise)"
- "Participating in pulmonary rehabilitation" + hint "1.2-1.4 g protein per kg body weight/day"
- "High activity (active lifestyle or regular exercise)"
- "Currently experiencing a COPD exacerbation or flare up" + hint "1.6-1.8 g protein per kg body weight/day"
- "Kidney disease (including chronic kidney disease Stage 2)" + hint "1.0-1.2 g protein per kg body weight/day"
- "Currently on dialysis" + hint "1.2-1.8 g protein per kg body weight/day"
Protein target card "Your Daily Protein Target" value `"$low - $high g/day$conditionNote"` where note is " (COPD exacerbation)" / " (kidney disease)" / " (dialysis)"; placeholders "-- g/day (set your weight first)" and "-- g/day (select your activity level above)" (`ProfileFragment.kt:442-516`). Multipliers/kg/midpoint match already.

### A10. Supplemental oxygen / BIPAP — "Do you use supplemental oxygen?" Yes/No; "What is your LPM?" hint "Enter LPM"; "Are you on a BIPAP or NIV?" Yes/No; "Which machine?" BIPAP/NIV; "What is your setting?" hint "Enter setting". Selecting "No" clears the dependent values. Nothing pre-selected until pref exists.

### A11. Premium row — "Subscribe to Premium" / "Premium active", always tappable → Paywall (auto-finishes if premium).

### A12. Privacy Policy / Terms of Use rows — Android Terms row dead (bug). iOS keeps both working (rows titled "Privacy Policy", "Terms of Use").

### A13. HIPAA row — label "HIPAA Authorization (PHI Disclosure)" + status "Signed on MMM d, yyyy" / "Revoked on MMM d, yyyy" / "Not signed" (`strings.xml:11-13`). iOS shows "Revoked" without date — add date.

### A14. Share Health Report — card title "Share Health Report", body "Share a comprehensive report of your health data including tracking information, severity assessment, and exacerbation plan with your healthcare provider or caregiver.", button "Share My Health Report". Non-premium → toast "Subscribe to Premium to share your health report." then Paywall. Premium → options dialog "Share Health Report" with "Share via Email" / "Share via Text Message" / "Share via Other Apps" / Cancel; each gated by `HipaaGate.requireConsent("share your report")`. iOS: single share sheet with subject "COPD Fuel Report - MMM d, yyyy" (platform equivalent — keep single sheet but add the card body copy, button label, and the non-premium toast). Generation error toast "Error generating report: {msg}".

### A15. Link to doctor — card "Share with your doctor" / "Share your health data with your doctor or practice." / checkbox "I agree to share my health data with this practice." / button "Link to doctor". Flow: no HIPAA → toast "You must complete the HIPAA Authorization before linking to a doctor." and open HIPAA; checkbox unchecked → toast "Please agree to share your data with the practice"; no token → "Please sign in first"; then `POST /consent {practiceId:"default", doctorId:"default", consentType:"share_with_doctor"}` → on success `POST /link-doctor {practiceId:"default"}` → toasts "Linked to doctor" / "{e.message}" or "Link failed" / "Consent failed". No doctor code input. iOS has an invite-code screen that only calls `linkDoctor(inviteCode)` — replace with the Android flow.

### A16. Sign out — matches.

### A17. Delete account — dialog title "Delete my account", message "This will permanently delete your account and all health data from our servers. You can also request deletion by emailing support@copdfuel.com. Are you sure?", buttons "Delete account" / "Cancel". Performs `DELETE /me`, then signOut, toast "Account deleted"; failure toast `{e.message}` or "Delete failed"; no token → "Please sign in first". iOS only calls `Amplify.Auth.deleteUser()` — add `deleteMe` to `COPDAPIClient`, call it first, then attempt `deleteUser()` (ignore failure), then sign out.

---

## (B) REPORT GENERATOR — `A/utils/ReportGenerator.kt` vs `I/Services/ReportGenerator.swift`

**Port the Android generator 1:1 (read the Kotlin file line by line).** Key points:

### B1. Header — matches ("========================================" / "       COPD FUEL REPORT" / "========================================" / blank / "Report Generated: MMM d, yyyy 'at' h:mm a" / blank). iOS must not insert extra blank lines between sections.

### B2. PATIENT PROFILE (A:61-180)
```
Age: {age|Not set}
Sex: ...
Weight: {weight} lbs | Not set
Height: ...
BMI: {x.x}|N/A                     ← only if weight AND height non-empty
<blank>
Activity Level: <long label>       ← full radio labels (A:90-99), "Not set"
Daily Protein Target: L - H g/day  ← only if low>0 && high>0
<blank>
Health Conditions:
  - None reported                  ← if condition_none
  - Kidney disease / - High cholesterol / - Pulmonary hypertension / - Currently on dialysis / - Other: {text}
  - None specified                 ← if none of the flags
<blank>
Supplemental Oxygen: Yes|No
  LPM: x
BIPAP/NIV: Yes|No
  Machine Type: x / Setting: x
<blank>
Healthcare Contacts:
  Doctor: / Doctor Phone: / Emergency Contact: / Emergency Phone:
  Second Emergency Contact: {emergency_contact2_name|Not set}
  Second Emergency Phone: {emergency_contact2_phone|Not set}
  Insurance Provider: {insurance_provider|Not set}
<blank>
Profile Last Updated: x            ← if non-empty
<blank>
```
Remove iOS "Goal Weight" line, "Not computed", "Not calculated (...)", short labels.

### B3. TRACKING SUMMARY (A:182-224) — heading "TRACKING SUMMARY"; reads `severity_fev1`, `severity_hospitalizations`, `severity_exacerbations`, `severity_oxygen`, `severity_result`, `severity_description`, `severity_assessment_date`. If result present:
```
Saved Summary:
  FEV1 Percentage: {fev1|Unknown}
  Hospitalizations (past year): ...
  Exacerbations (past year): ...
  Uses Supplemental Oxygen: ...
<blank>
Note: {severity_result}
<blank>
Details: {severity_description}
<blank>
Saved On: {date}
```
else "No tracking summary has been saved." / "You can save a summary in the Resources tab for your personal tracking.". Always followed by blank + "Note: This content is for personal tracking only and is not medical advice." + blank.

### B4. CARE PLAN NOTES (TEMPLATE) (A:226-251) — heading, blank, "Use this section to write down a plan and questions to discuss with your clinician." / "This template is not medical advice and is not a substitute for professional care." / blank / then "Saved Notes:" + `action_plan_instructions` (on iOS: the concatenated `doctor_instructions_log` texts, newest first, one per line) + blank, or "No notes saved." + blank. Remove the iOS template-recommendations block.

### B5. MEDICATIONS (A:253-290) — "Daily Medications:" → "  No daily medications added" or per med "  - {name}" / "    Dosage: {dosage}" / "    Frequency: {frequency}"; blank; "Exacerbation Medications:" same; blank. Discontinued excluded, no discontinued section. Order newest first.

### B6. TRACKING DATA window — end = now; start = (now − 30 days) truncated to local midnight. "Period: MMM d, yyyy - MMM d, yyyy".

### B7. NUTRITION SUMMARY (A:334-426)
```
NUTRITION SUMMARY:
  No food entries recorded in this period.     ← empty → blank, return
  Days Logged: n
  Total Entries: n
<blank>
  Daily Averages:
    Calories: %.0f kcal
    Protein: %.1f g
    Carbohydrates: %.1f g
    Fat: %.1f g
    Fiber: %.1f g
    Sodium: %.0f mg
    Potassium: %.0f mg
    Water: %.1f oz
<blank>
  Protein Target Progress:              ← if protein_target > 0
    Daily Target: {int} g
    Average Intake: %.1f g
    Achievement: %.0f% of target
<blank>
  Individual Food Entries:
    EEE MMM d, yyyy:                    ← days DESC
      - {name}[ [mealCategory]][ (quantity)]    ← entries by time ASC; meal/qty only if non-blank
        %.0f cal, P %.1fg, C %.1fg, F %.1fg
<blank>
```
(Water average = water-entry oz per day logged, as Android.)

### B8. EXERCISE SUMMARY (A:428-480) — empty → "  No exercise entries recorded in this period."; "  Total Exercise Sessions: n", "  Days with Exercise: n", "  Total Minutes: n", "  Average per Session: %.1f minutes", blank, "  By Exercise Type:" / "    {type}: n sessions, m minutes total" (first-seen order), blank, "  Individual Exercise Sessions:" / "    EEE MMM d, yyyy:" (DESC) / "      - {type}: m min" (ASC), blank.

### B9. EXERCISE JOURNAL (A:492-564) — reads String pref `exercise_journal_log` (JSON array `id, savedAt, dayMillis, timeText, warmUp, exercise, sets, reps, weight, weightUnit, activity`); filters `dayMillis in [start, end)`; empty → "  No exercise journal entries recorded in this period."; grouped by day DESC "  EEE MMM d, yyyy:"; entries by `savedAt` ASC: "    - {exercise|(unnamed)}[ @ {timeText}]", "      Sets: s, Reps: r" (if either >0), "      Weight: %.1f {unit}" (if >0), "      Activity: …", "      Warm-up: …"; blank. iOS currently reads the wrong key/schema.

### B10. OXYGEN (A:566-602) — "OXYGEN READINGS SUMMARY:", empty "  No oxygen readings recorded in this period."; "  Total Readings: n", "  Average Level: %.1f%", "  Minimum Level: n%", "  Maximum Level: n%", blank, "  Distribution:", "    Below 88% (Critical): n readings", "    88-92% (Low Normal): n readings", "    Above 92% (Normal): n readings", blank.

### B11. WEIGHT (A:604-638) — "WEIGHT SUMMARY:", empty "  No weight entries recorded in this period."; "  Entries Recorded: n", "  Starting Weight: %.1f lbs", "  Current Weight: %.1f lbs", "  Change: +%.1f lbs" ("+" when ≥0), goal from DB: "  Goal Weight: %.1f lbs", "  Distance to Goal: %.1f lbs to lose|to gain|(at goal)" where `toGoal = current − goal` (>0 → "to lose"). iOS direction is inverted — fix.

### B12. WATER (A:640-678) — "WATER INTAKE SUMMARY:", empty "  No water intake recorded in this period."; "  Days Tracked: n", "  Total Intake: n oz", "  Daily Average: n oz", "  Daily Goal: 64 oz", "  Days Goal Met: n / d".

### B13. STEPS / HEART RATE — iOS-only; keep appended after Water.

### B14. Footer (A:49-56) — blank, "========================================", "         END OF REPORT", "========================================", blank, then three lines: "This report was generated by COPD Fuel for personal tracking." / "It does not provide medical advice, diagnosis, or treatment recommendations." / "If you have questions about your health, consult a licensed clinician."

---

## (C) PAYWALL & BILLING

### C1. Product IDs `copdfuel_premium_monthly` / `copdfuel_premium_yearly` — match.
### C2. Copy (`R/layout/activity_paywall.xml`): title "COPD Fuel Premium"; subtitle "1 week free, then $9.99/month or $99/year. Unlock full health report sharing and support COPD Fuel."; buttons "1 week free, then $9.99/month" (filled), "1 week free, then $99/year (save more)" (outlined), "Restore purchases"; disclaimer "Subscriptions automatically renew unless cancelled at least 24 hours before the end of the current period."; "Terms of Use | Privacy Policy". iOS: use StoreKit `displayPrice` inside the same templates ("1 week free, then {price}/month", "1 week free, then {price}/year (save more)"); set the `.storekit` yearly price to 99.00 so the test price reads $99.
### C3. Subscribe error feedback — Android toasts "Subscription not available. Try again later." / "Subscription offer not found." / "Could not start purchase. Code: N". iOS sets `StoreManager.errorMessage` but never shows it — render it as a toast.
### C4. Restore — toast "Premium restored." + close / "No subscription found." (iOS: same text via toast, dismiss on restored).
### C5. Auto-close on premium — matches. C6. Trigger points — match.

---

## (D) AUTH & ONBOARDING

### D1. Cognito config / API base — match.
### D3. Login (`R/layout/activity_login.xml`): "COPD Fuel", "Sign in to sync your health data", hints "Email"/"Password", "Sign in" button, "Create account" text button. Both empty → "Enter email and password"; failures show message or "Sign in failed"; button disabled during request. iOS: remove "Welcome back" and the Forgot-password stub (May spec 2.4); keep eye toggle and friendly Cognito messages; button enabled always (validation on tap).
### D4. Sign-up (`R/layout/activity_sign_up.xml`): title "Create account", subtitle "Use your email and a password to sign up." → "We sent a verification code to %1$s. Enter it below."; fields "Name (optional)", "Email", "Password", "Confirm password", hidden "Verification code"; button "Sign up" → "Confirm"; "Use a different email" (hidden until confirmation; clears only the code, keeps typed values). Validation order: "Email is required." → "Email is not valid." → "Password is required." → "Password must be at least 8 characters." → "Passwords do not match."; code: "Verification code is required." / "Verification code must be 6 digits.". Name attr default = email local-part or "User". After confirm → auto sign-in → main.
### D5. Post-login sync gating — matches (iOS extra re-run kept).
### D6. API client — add `deleteMe()` (`DELETE /me`); `consent(practiceId:doctorId:consentType:)` and `linkDoctor(practiceId:)` payloads per A15; medication `date` from the record; error surfacing parses JSON `error` field else "Error {code}".

---

## (E) HIPAA AUTHORIZATION

### E1. Title "HIPAA Authorization for PHI Disclosure". Order: legal `hipaa_full_text` → "Health Information Consent" title → `health_info_consent_body` → checkbox → "Patient Signature (full name)" → "Date of Birth" (tap field) → "Sign with your finger or stylus" + pad + "Clear" → "Expires:" radios → confirm text → "Date" label + today ("MMMM d, yyyy") → error → "Agree & Sign" → "Revoke authorization".
### E2. `health_info_consent_body` — verbatim (`strings.xml:18`):
```
By signing, you allow COPD Fuel, owned by Ingenious Medical Solutions, LLC, to share your medical records with the person or organization you choose.

What information is shared?

Your complete medical record.

Any other specific health details you choose to list.

Why is it being shared?

To help with your medical treatment.

Because you requested it.

Important Things to Know:

It's Voluntary: You do not have to sign this to receive medical care.

You Can Change Your Mind: You can cancel this permission in writing at any time.

Expiration: This permission will end on the date you select below (or until you withdraw).

Privacy Note: Once your records are shared, they may no longer be protected by federal privacy rules.
```
### E3. `hipaa_full_text` — iOS intro must NOT add ", owned by Ingenious Medical Solutions, LLC,"; section 4 must be the six Android bullets: "This Authorization is voluntary. I am not required to sign it." / "I may refuse to sign this Authorization." / "I may revoke this Authorization at any time through the COPD Fuel platform or by emailing support@copdfuel.com. Revocation will not apply to uses or disclosures made before the effective date of revocation." / "COPD Fuel may rely on this Authorization until a valid revocation is received." / "Once my PHI is disclosed to the person(s) I designate, it may no longer be protected under the HIPAA Privacy Rule and may be subject to redisclosure." / "I have the right to receive a copy of this signed Authorization in electronic form." (Read `R/values/hipaa_authorization_text.xml` for the full text.)
### E4. Copy: checkbox "I have read and agree to the disclosure of my PHI as described in this Authorization."; confirm "By tapping "Agree & Sign", I confirm I am the patient and that the date below is correct."; submit "Agree & Sign"; name hint "Patient Signature (full name)"; DOB hint "Date of Birth"; "Sign with your finger or stylus"; "Clear".
### E5. Expiry radios: "1 Year from Today" (default), "On" + date field hint "MM / DD / YYYY" (visible only when "On" selected; default today+1y), "Until Withdrawn". Stored `expiryType` "1_year"/"on_date"/"until_withdrawn"; `expiryDate` = now+1y / chosen / 0. Missing date on "On" → "Please enter an expiry date.".
### E6. DOB picker — wheel picker acceptable (1900…today), result "MM/dd/yyyy".
### E7. Validation messages (in-page error): agree unchecked → the checkbox text; name → "Patient Signature (full name)"; DOB → "Date of birth is required."; signature → "Please draw your signature above."; expiry → "Please enter an expiry date.".
### E8. Save: toast "Authorization saved." and close. Revoke: dialog "Revoke authorization" / "Revoke your HIPAA authorization? You can sign again later if needed." / OK/Cancel → toast "Authorization revoked." and close. Signed state: same page with the form hidden and "Date: Signed on MMMM d, yyyy".
### E9. Revoked state — same as unsigned form (no banner).
### E10. Signature pad — iOS "Clear" never clears the strokes (bug): route Clear through the pad's `clear()`.
### E11. Storage keys — align to Android names: `hipaa_consent_signed`, `hipaa_consent_date`, `hipaa_consent_printed_name`, `hipaa_consent_dob`, `hipaa_consent_expiry_type`, `hipaa_consent_expiry_date`, `hipaa_revoked`, `hipaa_revoked_date`; revoke keeps `signed=true` + `revoked=true`; `hasValidConsent = signed && !revoked`; re-sign clears revoked. One-shot migration from the old iOS keys (`hipaa_consent_revoked`, `hipaa_consent_revoked_date`, `hipaa_consent_name`).
### E12. HipaaGate — matches.

---

## (F) HEALTH IMPORT

### F1. Entry: Tracking section header "COPD Fuel", outlined button "Import All from Device", link "How does this work?" → dialog "Import All from Device" with `tracking_import_how_it_works_message` (`strings.xml:39`, adapt "Health Connect" → "Apple Health"; read the file for the verbatim text).
### F2. Flow: rationale dialog "Why COPD Fuel needs Health Connect access" (`strings.xml:40-41`; adapt to "Apple Health") OK/Cancel → permission → import fixed **30 days**; zero records → toast "No health data found in the last 30 days. If you use Samsung Health or a watch, connect it to Health Connect in Settings so data can sync, then try again." (adapt: "If you use an Apple Watch or another health app, make sure it syncs to Apple Health, then try again."); success toast "Imported: %1$d oxygen, %2$d weight, %3$d exercise, %4$d steps, %5$d heart rate from your device."; if no exercise but other data → "Exercise in Health Connect comes from logged workouts (start/stop on watch or Samsung Health), not steps. Log workouts to see them here." (adapt to Apple Health/Apple Watch); denied → "Grant access in Health Connect settings to import data from your device." (adapt: "Grant access in Settings → Health → Data Access & Devices → COPD Fuel to import data from your device."). Remove the iOS period picker and the inline result panel.
### F3. What is read — matches (keep workout-type names).
### F4. Dedupe: oxygen skipped if a reading with the identical timestamp exists; heart rate same; steps **replace** the day's rows; weight and exercise inserted unconditionally. After import, `ProfileWeightSync.syncPrefsFromRepositoryCurrentWeight`.
### F5. Live steps: Tracking shows the selected day's steps directly from HealthKit when authorized, falling back to stored steps.

---

## (G) ACCOUNT / SETTINGS — no dedicated settings screen on either platform.
