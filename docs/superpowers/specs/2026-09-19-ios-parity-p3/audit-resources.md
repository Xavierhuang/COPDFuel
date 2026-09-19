# RESOURCES TAB — Android → iOS parity gap list (audit 2026-09-19)

**Android source of truth**
- `android/app/src/main/java/com/copdhealthtracker/ui/fragments/ResourcesFragment.kt` (2470 lines; all copy is hardcoded — no `strings.xml` lookups)
- `android/app/src/main/java/com/copdhealthtracker/ui/resources/MedicationTypes.kt`
- `android/app/src/main/res/layout/fragment_resources.xml`, `item_tool_card.xml`, `item_guideline.xml`, `dialog_add_medication.xml`
- `android/app/src/main/java/com/copdhealthtracker/ui/dialogs/AddMedicationDialog.kt`, `data/dao/MedicationDao.kt:12`

**iOS as on disk (uncommitted working tree)**
- `COPDFuel/COPDFuel/Views/ResourcesView.swift` (1561 lines), `Views/DailyTrackingSummary.swift`, `Services/SeverityCalculator.swift`, `Views/MedicationListView.swift`, `Views/ProgramsNearMeView.swift` (header only), `MainTabView.swift:39-42`

---

## 0. Tab shell / header / tool selector

| # | Android | iOS now |
|---|---|---|
| 0.1 | Bottom-nav label "Resources" (`res/menu/bottom_navigation.xml:22`) | `MainTabView.swift:41` `Label("Resources", ...)` — **matches** |
| 0.2 | Green header band "COPD" (orange) + " Fuel" (primary), 28sp bold (`fragment_resources.xml:20-50`) | Missing. iOS shows `.navigationTitle("Resources")` (`ResourcesView.swift:80`) |
| 0.3 | Yellow banner: title "COPD Management Tools" (22sp bold brownText), subtitle "Specialized tools to help you manage your COPD effectively" (`fragment_resources.xml:53-80`) | Missing |
| 0.4 | 5 equal-width tool cards in one non-scrolling row, labels with hard line breaks: "Severity\nEval", "COPD\nExacerb.", "Pulm.\nRehab", "Resp.\nCare", "Resource\nHub"; custom drawables `ic_tool_severity/_exacerbation/_pulmonary/_medication/_resources`; selected = primary tint + `tool_card_background_selected`, unselected = textTertiary (`ResourcesFragment.kt:45-51, 76-123`, `item_tool_card.xml`) | `ResourcesView.swift:19-25, 32-51`: horizontal ScrollView, 80x70 chips, labels "Severity Assessment", "Exacerbation", "Pulmonary Rehab", "Medication Guide", "Resource Hub"; SF Symbols `cross.case`, `doc.text`, `lungs`, `pills`, `books.vertical`; selected = blue fill/white text |
| 0.5 | Selected tool index restored across config change via `KEY_SELECTED_TOOL_INDEX` (`:64-73`) | `@State selectedTool` — fine for SwiftUI |
| 0.6 | No global disclaimer under chips | **iOS-only**: `ResourcesView.swift:53-58` "Resources are for informational and personal tracking purposes only. Nothing here constitutes medical advice, diagnosis, or treatment." |
| 0.7 | Content container padding 20dp; Android also cancels flow collectors on tab switch (`:125-137`) | n/a |

---

## 1. Severity Eval (tool 0) — `buildSeverityContent` `:139-244`

**Android is a "save what you entered" form with NO severity formula. iOS computes a classification. This whole sub-feature diverges.**

| # | Android (`ResourcesFragment.kt`) | iOS (`ResourcesView.swift` / `SeverityCalculator.swift`) |
|---|---|---|
| 1.1 | Title "COPD Severity Assessment" 24sp bold (`:145-151`) | `:96` "COPD Severity Assessment" `.title3` — **matches text** |
| 1.2 | Disclaimer card (cardBackgroundBlue): "This screen summarizes information you enter for your own tracking. It does not provide medical advice, diagnosis, or treatment recommendations." (`:154-161`) | Missing (iOS has the different global disclaimer 0.6, and a post-result caption `:147-151` "For personal tracking only. This is not a medical diagnosis. Consult your healthcare provider for a clinical evaluation." — iOS-only) |
| 1.3 | Four picker rows rendered with `item_guideline` card (gray card, 4dp primary left bar, title 18sp bold, value 14sp). Tapping value opens `AlertDialog.setItems` list titled with the question (`:171-188`) | `:102-117` `Form` with 4 free-text numeric `TextField`s: sections "FEV1 Percentage"/"Enter FEV1 %", "Hospitalizations"/"Number of hospitalizations", "Flare-ups"/"Number of flare-ups", "Oxygen Level"/"Oxygen level" |
| 1.4 | Q1 label: "What is your latest FEV1 (Forced Expiratory Volume in one second) percentage? (If known)"; options exactly: "Select FEV1 percentage" (default/placeholder), "80% or higher", "50-79%", "30-49%", "Less than 30%", "I don't know" (`:163, 190`) | Missing |
| 1.5 | Q2 label: "How many times have you been hospitalized for COPD in the past year?"; options: "Select number", "0", "1", "2", "3 or more" (`:164, 191`) | Missing |
| 1.6 | Q3 label: "How many COPD flare-ups (exacerbations) have you had in the past year?"; same number options (`:192`) | Missing |
| 1.7 | Q4 label: "Do you use supplemental oxygen?"; options: "Select level", "Yes", "No" (`:165, 193`) | Missing |
| 1.8 | Button text "Save Summary" (primary bg, white text) (`:195-197`) | `:120-129` "Calculate Severity" |
| 1.9 | On tap: NO validation, NO computation. Shows result view (cardBackgroundBlue, 16sp) with text `"Saved summary:\n\n" + lines`: "FEV1: {fev1Str}", "Hospitalizations (past year): {hospStr}", "Flare-ups (past year): {flareStr}", "Uses supplemental oxygen: {Yes|No}" (oxygen line prints "No" unless exactly "Yes" — i.e. also "No" when left as "Select level"). Unselected questions print their placeholder text, e.g. "FEV1: Select FEV1 percentage" (`:199-216`). Numeric mappings fev1→80/65/40/15 and hosp/flares ints are computed but unused (`:199-209`) — do not port a formula | `:131-146` shows `COPDSeverity.rawValue` ("Mild COPD"/"Moderate COPD"/"Severe COPD"/"Very Severe COPD") in severity colour + `description` text. `SeverityCalculator.swift:40-63` formula — **iOS-only; must be removed for parity** |
| 1.10 | Persisted keys (default SharedPreferences, all Strings): `severity_fev1`, `severity_hospitalizations`, `severity_exacerbations`, `severity_oxygen` (raw option strings incl. placeholders), `severity_result` = "Tracking summary (not a medical assessment)", `severity_description` = "Saved for your personal tracking and to discuss with your clinician.", `severity_assessment_date` = `SimpleDateFormat("MMM d, yyyy")` string (`:219-229`). Consumed by `utils/ReportGenerator.kt:191-197` | `:170-176` writes `severity_fev1`, `severity_hospitalizations`, **`severity_flareups`** (Android: `severity_exacerbations`), `severity_oxygen`, `severity_result` = computed rawValue, **`severity_saved_at`** as `Date` (Android: `severity_assessment_date` string); no `severity_description`. iOS `ReportGenerator.swift:194-199` reads the iOS keys — must be updated in lockstep |
| 1.11 | Result view hidden until Save; stays visible after (`:232-243`) | equivalent (`showResult`) |
| 1.12 | Form values are local vars — NOT prefilled from prefs on re-entry | iOS keeps values as `@State` — minor iOS-only |

---

## 2. COPD Exacerb. / Action Plan (tool 1) — `buildActionPlanContent` `:246-800`

### 2a. Migration + title + disclaimer
| # | Android | iOS |
|---|---|---|
| 2a.1 | One-time migration from `action_plans_list` JSON array (last element's `doctorName`, `doctorPhone`, `emergencyName`, `emergencyPhone`, `instructions`) → legacy keys, then remove key (`:257-281`) | `:459-483` migrates from `actionPlans`/`carePlanZones` Codable keys instead. Local-only data; different legacy formats are acceptable |
| 2a.2 | Title "COPD Exacerbation Action Plan" (`:284`) | `:274` **matches** |
| 2a.3 | Disclaimer "This care plan should be created in partnership with your healthcare provider. Use this template to document your personalized plan for managing COPD flare-ups." (`:294`) | `:282` **matches** |

### 2b. Important Contacts (`:305-373`)
| # | Android | iOS |
|---|---|---|
| 2b.1 | Heading "Important Contacts" | `:295` **matches** |
| 2b.2 | 7 fields, label (16sp bold) + EditText (hint), in this order & with these pref keys/hints: "Doctor's Name" `doctor_name` "Dr. Smith"; "Doctor's Phone" `doctor_phone` "(555) 123-4567"; "Emergency Contact Name" `emergency_contact_name` "Jane Doe"; "Emergency Contact Phone" `emergency_contact_phone` "(555) 987-6543"; **"Second Emergency Contact Name" `emergency_contact2_name` "John Doe"; "Second Emergency Contact Phone" `emergency_contact2_phone` "(555) 222-3333"; "Insurance Provider (name only, no ID numbers)" `insurance_provider` "e.g. Medicare, Humana"** (`:346-353`) | `:296-299` only the first 4 fields. **Missing 3 fields + keys** (Android `ReportGenerator.kt:161-163` reads them) |
| 2b.3 | Values persisted only on "Save Contacts" tap; Toast "Contacts saved" (`:355-373`) | `@AppStorage` writes on every keystroke; Save button shows alert "Contacts Saved"/OK. Copy differs; Android is a toast |

### 2c. Medication Plan (`:375-516`)
| # | Android | iOS |
|---|---|---|
| 2c.1 | Heading "Medication Plan"; sub-headings "Daily Medications", "Exacerbation Medications" | **matches** |
| 2c.2 | Empty texts "No daily medications added yet." / "No exacerbation medications added yet." (`:494, 509`) | **matches** |
| 2c.3 | Row text single line `"${name} - ${dosage} (${frequency})"` 14sp + red bold "Delete" 12sp (`:397-403`) | `CarePlanMedicationRow :585-609` renders two lines — format differs |
| 2c.4 | Delete confirm: title "Delete medication?", message "{name} will be removed.", buttons "Delete"/"Cancel" (`:410-417`) | `:645-650` title "Delete Medication?" (capital M) |
| 2c.5 | List query `WHERE type=? AND isDiscontinued=0 ORDER BY date DESC` → newest first (`MedicationDao.kt:12`) | `DataManager.swift:153-159` insertion order (oldest first) |
| 2c.6 | Buttons "+ Add Daily Medication", "+ Add Exacerbation Medication" (literal plus, no icon) (`:443, 476`) | icon `plus.circle.fill` + "Add Daily Medication"… |
| 2c.7 | `AddMedicationDialog`: title "Add Medication"; layout order: label "Type" + spinner ["Daily","Exacerbation"] preselected from `defaultType`; label "Medication Name" hint "Enter name"; label "Dosage" hint "e.g., 2 puffs"; label "Frequency" hint "e.g., twice daily"; buttons "Save"/"Cancel"; empty name → Toast "Enter medication name" and dialog stays open; on save Toast "Medication saved" (`AddMedicationDialog.kt:22-64`) | `AddMedicationSheet :611-657`: fields "Name (e.g., Spiriva)", "Dosage (e.g., 18 mcg)", "Frequency (e.g., Once daily)", Picker "Type" last; Save disabled when name empty; no toast |

### 2d. Action Plan Zones (`:518-612`)
| # | Android | iOS |
|---|---|---|
| 2d.1 | Heading "Action Plan Zones" | **matches** |
| 2d.2 | Green: title "Green Zone: I'm Doing Well"; 4 symptoms; action "Take daily medications as prescribed" | **matches** (text) |
| 2d.3 | Yellow: title "Yellow Zone: I'm Having a Bad Day"; 9 symptoms; action "Use your clinician-provided plan. If symptoms worsen, contact your clinician." | **matches** (text) |
| 2d.4 | Red: title "Red Zone: I Need Urgent Medical Care"; 7 symptoms; action "Seek urgent medical care immediately (use local emergency services)." | **matches** (text) |
| 2d.5 | Symptom lines rendered as `"  $symptom"` (two-space indent, no bullet); action line `"Action: $action"` bold on holo_*_light bg with holo_*_dark text (`:527-556`) | `StaticZoneCard :548-583`: `"• \(s)"` bullets, SF icon in header. Cosmetic |

### 2e. Additional Instructions log (`:614-800`)
| # | Android | iOS |
|---|---|---|
| 2e.1 | Heading "Additional Instructions from Your Doctor" | **matches** |
| 2e.2 | Storage key `doctor_instructions_log` = JSON array of `{id, savedAt(ms), text}`; legacy `action_plan_instructions` string migrated into first entry (`:627-680`) | key `doctorInstructionsLog` (Codable). Align to Android key + JSON schema (report reads it) |
| 2e.3 | Edit mode: EditText hint "Enter new instructions from your doctor...", minLines 4 (`:687-698`) | `TextEditor` with no placeholder — **missing hint** |
| 2e.4 | "Save": blank → Toast "Instructions cannot be empty", stays in edit mode; else append, Toast "Instructions saved", exit edit mode (`:711-728`) | blank → silently cancels; success → alert "Instruction Saved" |
| 2e.5 | "Cancel" button shown **only if** `instructionsLog.isNotEmpty()` (`:731-746`) | Cancel always shown |
| 2e.6 | View mode: button "+ Add New Instruction"; empty text "No instructions saved yet." (`:748-771`) | icon + "Add New Instruction"; empty text **matches** |
| 2e.7 | Entries sorted `savedAt` desc; card header `"Saved: " + SimpleDateFormat("MMM d, yyyy h:mm a")` e.g. "Saved: Sep 19, 2026 3:05 PM"; red bold "Delete" (`:682, 795-806`) | sorted desc **matches**; date "Sep 19, 2026 at 3:05 PM" (extra " at ") |
| 2e.8 | Delete confirm: title "Delete instruction?", message "This entry will be permanently removed.", "Delete"/"Cancel" (`:813-822`) | title "Delete Instruction?" (capital I) |

---

## 3. Pulm. Rehab (tool 2) — `buildPulmonaryContent` `:802-1534`

**Section order on Android:** Title → intro → Benefits (6) → "Finding a Pulmonary Rehabilitation Program" → button → "Home Exercise Program" (intro, Breathing ×3, Endurance ×3, Strength ×3, red warning) → "Track Your Progress" → Exercise Journal → full-width image.
**iOS order (`:704-813`):** Title → intro → image (cropped 180pt) → Benefits → "Recommended Exercises" (iOS-only) → "Finding a Program" → button → Journal. **Reorder required.**

| # | Android | iOS |
|---|---|---|
| 3.1 | Title "Pulmonary Rehabilitation"; intro "Pulmonary rehabilitation is a comprehensive program that combines exercise, education, and support to help people with COPD breathe better, get stronger, and improve their quality of life. Always consult with your healthcare provider before starting any exercise program." (`:810-822`) | **matches** |
| 3.2 | Heading "Benefits of Pulmonary Rehabilitation" (`:826`) | **matches** |
| 3.3 | Benefit 1 "Improved Exercise Capacity" / "Pulmonary rehabilitation can help you walk further and perform daily activities with less breathlessness." | **matches** |
| 3.4 | Benefit 2 "Better Quality of Life" / "Many people report feeling better overall and having more energy for the activities they enjoy." | **matches** |
| 3.5 | Benefit 3 "Reduced Hospital Admissions" / "Regular participation in pulmonary rehabilitation can reduce your risk of COPD exacerbations requiring hospitalization." (`:836`) | truncated on iOS |
| 3.6 | Benefit 4 "Increased Strength" / "Strengthening exercises help counter muscle loss that often occurs with COPD and improve your ability to perform daily tasks." (`:837`) | truncated |
| 3.7 | Benefit 5 "Better Breathing Control" / "Learning proper breathing techniques helps you manage breathlessness during activities and reduce anxiety." (`:838`) | truncated |
| 3.8 | Benefit 6 "Social Support" / "Meeting others with similar conditions provides emotional support and motivation to maintain your exercise program." (`:839`) | truncated |
| 3.9 | Heading "Finding a Pulmonary Rehabilitation Program" (`:860`) | "Finding a Program" |
| 3.10 | Paragraph "Pulmonary rehabilitation programs are typically offered at hospitals, outpatient clinics, or community centers. To find a program near you:" followed by 4 "• " bullets (`:868-872`) | bullets match; lead-in sentence **missing** |
| 3.11 | Button "Find Programs Near Me" → pushes `ProgramsNearMeFragment` (`:880-891`) | `NavigationLink(destination: ProgramsNearMeView())` — **matches** |
| 3.12 | Heading "Home Exercise Program" + intro "While a supervised pulmonary rehabilitation program is ideal, these exercises can be performed at home to complement your program or when a formal program isn't available." (`:894-907`) | **Missing** |
| 3.13 | Sub-heading "Breathing Exercises" (18sp bold primary) + caption "Techniques to improve breathing efficiency and control" (`:910-923`) | Missing |
| 3.14 | "Pursed-Lip Breathing" / "Breathe in through your nose for 2 counts, then breathe out slowly through pursed lips for 4 counts. This helps control breathlessness and slows your breathing rate." / italic "Recommended frequency: 5-10 minutes, 4-5 times daily" (`:926`) | Missing |
| 3.15 | "Diaphragmatic Breathing" / "Place one hand on your chest and the other on your abdomen. Breathe in through your nose, feeling your abdomen rise. Breathe out through pursed lips while gently pressing on your abdomen." / "Recommended frequency: 5-10 minutes, 3-4 times daily" (`:927`) | Missing |
| 3.16 | "Segmental Breathing" / "Focus on directing air to different parts of your lungs by placing hands on specific areas of your chest or sides while breathing deeply." / "Recommended frequency: 5 minutes, 2-3 times daily" (`:928`) | Missing |
| 3.17 | Sub-heading "Endurance Training" + "Activities to improve cardiovascular fitness and stamina" (`:966-979`) | Missing |
| 3.18 | "Walking" / "Start with short distances and gradually increase. Use pursed-lip breathing while walking. Stop and rest if you become too breathless." / "Recommended frequency: Start with 5-10 minutes daily, gradually increase to 20-30 minutes" (`:982`) | Missing |
| 3.19 | "Stationary Cycling" / "Adjust resistance to a comfortable level. Maintain good posture and use pursed-lip breathing." / "Recommended frequency: Start with 5-10 minutes daily, gradually increase to 15-20 minutes" (`:983`) | Missing |
| 3.20 | "Swimming/Water Exercises" / "The buoyancy of water supports your body, making movement easier. The humidity can also help your breathing." / "Recommended frequency: 20-30 minutes, 2-3 times weekly" (`:984`) | Missing |
| 3.21 | Sub-heading "Strength Training" + "Exercises to strengthen respiratory and peripheral muscles" (`:1012-1025`) | Missing |
| 3.22 | "Upper Body Strengthening" / "Use light weights or resistance bands for arm raises, bicep curls, and shoulder presses. Focus on proper breathing throughout." / "Recommended frequency: 8-12 repetitions, 2-3 sets, 2-3 times weekly" (`:1028`) | Missing |
| 3.23 | "Lower Body Strengthening" / "Perform chair stands, leg extensions, and calf raises to strengthen legs. These help with daily activities like standing and walking." / same frequency (`:1029`) | Missing |
| 3.24 | "Core Strengthening" / "Seated abdominal contractions and gentle back extensions help improve posture and breathing mechanics." / same frequency (`:1030`) | Missing |
| 3.25 | Red bold warning on gray bg: "Important: Always start slowly and progress gradually. Stop any exercise that causes severe shortness of breath, chest pain, or dizziness. Keep your rescue inhaler nearby during exercise." (`:1057-1065`) | Missing |
| 3.26 | Heading "Track Your Progress" + "Keeping track of your exercise sessions helps you see your progress and stay motivated. Consider tracking:\n\n• Exercise duration and frequency\n• Distance walked or steps taken\n• Breathlessness levels before, during, and after exercise\n• How you feel overall after each session" (`:1068-1085`) | Missing |
| 3.27 | Image `pulmonary_rehab_exercises` at the END, full-bleed, `FIT_XY`, adjustViewBounds (`:1512-1533`) | image after intro, cropped. Position and crop differ |
| 3.28 | — | **iOS-only** `:740-753` "Recommended Exercises" list — replace with 3.12-3.26 |

### 3x. Exercise Journal (`:1088-1509`) vs `ExerciseJournalView :815-1069`
| # | Android | iOS |
|---|---|---|
| 3x.1 | Heading "Exercise Journal"; caption "Daily log of warm-ups, exercises, sets, reps, weights, and activity. All fields are optional — fill in only what applies." (`:1190-1201`) | **matches** |
| 3x.2 | Storage key `exercise_journal_log`, JSON array `{id, savedAt(ms), dayMillis, timeText("h:mm AM/PM"), warmUp, exercise, sets, reps, weight, weightUnit("lbs"), activity}` (`:1108-1160`); read also by `TrackingFragment.kt:862` and `ReportGenerator.kt:506` | key `exerciseJournalLog`, Codable with `day: Date`, `time: Date`. Align to Android key + JSON schema (report reads it) |
| 3x.3 | Form: label "Day" + button `MMM d, yyyy` → DatePickerDialog; label "Time" + button `h:mm AM` → TimePickerDialog; defaults = now (`:1207-1276`) | `DatePicker("Day", .date)`, `DatePicker("Time", .hourAndMinute)` — equivalent |
| 3x.4 | Field hints: Warm Up "e.g., 5 min walk, stretching"; Exercise "e.g., Bench press, Squats"; Sets "e.g., 3" (number); Reps "e.g., 10" (number); Weight "e.g., 25" (decimal) + unit Spinner ["lbs","kg"] with **no label**; Activity "e.g., Strength training, Cardio" (`:1300-1338`) | placeholders truncated; iOS adds a "Unit" label |
| 3x.5 | Save: no validation; `sets/reps` `toIntOrNull() ?: 0`, `weight` `toDoubleOrNull() ?: 0.0`; Toast "Entry saved"; exit form (`:1348-1370`) | parsing matches; feedback alert "Entry Saved" |
| 3x.6 | Cancel button always shown in form (`:1373-1386`) | **matches** |
| 3x.7 | List: button "+ Start New Entry"; caption "Showing entries from the last 7 days. View all in Tracking → Month." (`:1388-1409`) | icon + "Start New Entry"; caption **matches** |
| 3x.8 | Filter `dayMillis >= startOfToday - 7 days`, sorted `savedAt` desc (`:1203-1212`) | **matches** |
| 3x.9 | Empty: "No journal entries yet." if none at all, else "No journal entries in the last 7 days." (`:1411-1423`) | **matches** |
| 3x.10 | Card header `"{MMM d, yyyy} • {timeText}"`; Delete; exercise 18sp bold; stats `"{n} sets • {n} reps • {w} {unit}"` (int if whole); "Activity: …"; "Warm Up: …"; "(no details recorded)" when all blank (`:1447-1503`) | **matches** |
| 3x.11 | Delete confirm: title "Delete entry?", message "This journal entry will be permanently removed." (`:1461-1471`) | title "Delete Entry?" (capital E) |

---

## 4. Resp. Care / Medication Guide (tool 3) — `buildMedicationContent` `:1537-1948` + `MedicationTypes.kt`

**iOS `MedicationGuideView :1131-1256` is a completely different, much shorter screen. Everything below except 4.1 is missing on iOS. The `GuidelinesView.swift:70-98` "Featured COPD Inhalers" block is in the wrong tab and uses a different Symbicort URL.**

| # | Android | iOS |
|---|---|---|
| 4.1 | Title "COPD Medication Guide" (`:1547`) | **matches** |
| 4.2 | Description card: "This guide provides general information about COPD medications. Your doctor will prescribe medications based on your specific needs. Always follow your healthcare provider's instructions about your medications." (`:1556`) | differs |
| 4.3 | Heading "Featured COPD Inhalers"; two half-width buttons "Symbicort Guide" → `https://www.symbicort.com/`, "Breztri Guide" → `https://www.breztri.com/` (`:1569-1619`) | Missing in Resources (exists in Guidelines with different Symbicort URL) |
| 4.4 | Heading "Medication Types" (22sp **bold-italic**, primary) (`:1622-1628`) | Missing |
| 4.5 | 3-column grid of 10 `MaterialCardView` tiles (36dp icon + 11sp bold 2-line title), this order and these grid titles: "Bronchodilator", "Inhaled Corticosteroids", "Combination Inhalers", "Phosphodiesterase-4 Inhibitors", "Antibiotics", "Systemic Corticosteroids", "Methylxanthine", "Mucolytics/Expectorants", "Biologics", "Nebulizer Medications" (`:1631-1643`) | iOS has 8 inline cards in different order, not tappable, no Methylxanthine or Nebulizer |
| 4.6 | Tap tile → `AlertDialog` titled `Info.title`, message = sections joined by blank line, each heading bold + "\n" + body; bullet lines ("• ") get hanging indent; single button "Close" (`:1715-1751`) | Missing |
| 4.7 | Section headings in order for every guide: "Examples", "What it is", "What it's for", "How it works", "Common forms", "How to use", "Common side effects", "Warnings", "Interactions", "Important" (`MedicationTypes.kt:34-45`) | Missing |
| 4.8 | Shared "Important" body = `DISCLAIMER` (`MedicationTypes.kt:9-14`): "This guide is general education, not medical advice. It does not list every use, side effect, warning or interaction, and brand names are examples only. Always follow your own prescription and the leaflet that comes with your medicine. Ask your doctor or pharmacist before starting, stopping or changing any medicine. If you have severe trouble breathing, chest pain, or swelling of the face, lips or throat, call 911." | Missing |
| 4.9 | Dialog titles (`Info.title`) differ from grid titles for 3 items: "Bronchodilators", "Phosphodiesterase-4 (PDE4) Inhibitors", "Methylxanthines" (`MedicationTypes.kt:50-52, 130, 214`) | — |
| 4.10 | Guide bodies — all 10 × 9 sections must be copied verbatim from `MedicationTypes.kt:48-352`. Per-guide line refs: bronchodilators `:49-83`; ics `:84-113`; combination `:114-146`; pde4 `:147-180`; antibiotics `:181-215`; systemic `:216-246`; methylxanthines `:247-276`; mucolytics `:277-307`; biologics `:308-338`; nebulizer `:339-370`. **Port by reading the Kotlin file and transcribing every string exactly.** | Missing |
| 4.11 | Green (`colorSuccess`) full-width button "Watch Inhaler Technique Videos" → `https://www.copdfoundation.org/Learn-More/Educational-Materials-Resources/Educational-Video-Series.aspx` (`:1770-1786`) | Missing |
| 4.12 | 1dp divider, then heading "Respiratory Support and Airway Clearance Devices" + card "Common devices used to support breathing, deliver oxygen, and clear mucus from the airways. Talk to your healthcare provider about which devices may be appropriate for your care." (`:1789-1824`) | Missing |
| 4.13 | Category "1. Airway Clearance Devices" / "Devices used to loosen, mobilize, and help remove mucus from the lungs." with device cards (name / "Classification: …" / "Purpose: …"): **Aerobika** — "Oscillating Positive Expiratory Pressure (OPEP) device" — "Uses pressure and vibrations when you breathe out to loosen and clear sticky mucus from the lungs"; **Flutter valve** — "Oscillating Positive Expiratory Pressure (OPEP) device" — "When you exhale into the flutter valve, your breath lifts and drops a steel ball inside, creating vibrations that shake the mucus loose from one's airways"; **High-frequency chest wall oscillation vest** — "High-frequency chest wall oscillation (HFCWO) device" — "High-Frequency Chest Wall Oscillation (HFCWO) vest therapy uses rapid air pulses to squeeze and vibrate your chest, which thins and shakes stubborn mucus loose from your airway walls so you can cough it out." (`:1894-1917`) | Missing |
| 4.14 | "2. Oxygen Therapy Devices" / "Devices used to improve oxygen levels in patients with hypoxemia." — **Oxygen concentrator** — "Oxygen delivery device" — "Provides supplemental oxygen to help support a patient's oxygen levels" (`:1920-1931`) | Missing |
| 4.15 | "3. BiPAP Devices" / "Devices used to provide pressure-supported breathing assistance." — **BiPAP** — "Bilevel Positive Airway Pressure device" — "Delivers two levels of air pressure, with higher pressure during inhalation called IPAP and lower pressure during exhalation called EPAP, to keep the airways open, improve ventilation, and reduce the work of breathing for the lungs." (`:1934-1945`) | Missing |
| 4.16 | "4. Noninvasive Ventilation (NIV)" / "Devices used to deliver breathing support through a mask rather than an invasive airway." — **NIV** — "Noninvasive positive pressure ventilation (NIPPV) device" — "Helps support breathing and improves gas exchange without intubation. Used to prevent unintended breath stacking." (`:1948-1959`) | Missing |
| 4.17 | Bottom italic 12sp textTertiary note: "Important: Always consult with your healthcare provider before starting, stopping, or changing any medication. This guide is for informational purposes only." (`:1962-1969`) | Missing |

---

## 5. Resource Hub (tool 4) — `buildResourceHubContent` `:1971-2445`

**Android order:** COPD Organizations → Educational Resources → Support Groups → COVID-19 and COPD → Quit Smoking with COPD. **iOS order (`:1260-1312`):** National Organizations → Support Groups & Communities → Educational Resources → Emergency Resources.

| # | Android | iOS |
|---|---|---|
| 5.1 | Title "COPD Resource Hub" (`:1981`) | **matches** |
| 5.2 | Description card "This resource hub provides links to trusted organizations, educational materials, and support groups to help you better understand and manage your COPD." (`:1990`) | differs |
| 5.3 | Heading "COPD Organizations" (`:2005`) | "National Organizations" |
| 5.4 | Org 1 "American Lung Association" / "Provides education, advocacy and research to improve lung health and prevent lung disease." / "Helpline: 1-800-LUNGUSA" / button "Visit Website" → https://www.lung.org (`:2012`) | desc differs; no helpline; no "Visit Website" button |
| 5.5 | Org 2 "COPD Foundation" / "Dedicated to improving the lives of those affected by COPD through research, education, early diagnosis, and enhanced therapy." / "Helpline: 1-866-316-COPD" / https://www.copdfoundation.org (`:2013`) | desc differs; no helpline |
| 5.6 | Org 3 "Global Initiative for Chronic Obstructive Lung Disease (GOLD)" / "Works to improve prevention and treatment of COPD through a global network." / no helpline / https://goldcopd.org (`:2014`) | desc differs |
| 5.7 | — | **iOS-only** "National Heart, Lung, and Blood Institute" org entry — remove |
| 5.8 | Heading "Educational Resources" (`:2059`) then 3 cards (title / type tag / desc / button "Access Resource"): "Short, Comprehensive COPD Videos" / "Videos" / "Animations, PSAs, and videos from NHLBI on COPD risk factors, signs and symptoms, treatment options, and more." → `https://www.nhlbi.nih.gov/health-topics/education-and-awareness/copd-learn-more-breathe-better/copd-videos` (`:2066-2107`) | Missing; iOS has 3 different items — replace |
| 5.9 | "Living Well with COPD" / "Website" / "Practical tips for managing daily life with COPD." → `https://www.livingwellwithcopd.com/` (`:2110-2158`) | Missing |
| 5.10 | "COPD and Nutrition" / "Guide" / "How nutrition supports breathing and overall health in COPD." → `https://www.lung.org/lung-health-diseases/lung-disease-lookup/copd/living-with-copd/nutrition` (`:2161-2209`) | Missing |
| 5.11 | Heading "Support Groups" (`:2213`); cards show name / type tag / desc / "Visit Website" | "Support Groups & Communities"; no type tag |
| 5.12 | "Right2Breathe" / "Online" / "The Right2Breathe Pulmonary Chat is a free online chat and live video program where medical experts provide education and answer questions about living with COPD." → `https://right2breathe.org/` (`:2221`) | leading clause dropped; type tag missing |
| 5.13 | "Better Breathers Club" / "In-person & Virtual" / "In-person and virtual support groups organized by the American Lung Association." → `https://www.lung.org/help-support/better-breathers-club/better-breathers-club-meetings` (`:2222`) | URL differs; type tag missing |
| 5.14 | "COPD360social" / "Online" / "Online community platform for individuals with COPD and their caregivers." → `https://www.copdfoundation.org/COPD360social/Community/Get-Involved.aspx` (`:2223`) | desc + URL match; type tag missing |
| 5.15 | Heading "COVID-19 and COPD"; sub-heading "Special Considerations for COPD Patients"; paragraph "People with COPD may be at higher risk for severe illness from COVID-19. It's important to take extra precautions and stay updated with the latest guidance."; 5 bullets: "Continue taking your COPD medications as prescribed", "Maintain at least a 30-day supply of your medications", "Follow recommendations for vaccination", "Practice physical distancing and wear masks when appropriate", "Have an emergency action plan in case you develop COVID-19 symptoms" (`:2266-2306`) | **Missing entirely** |
| 5.16 | Heading "Quit Smoking with COPD"; intro "When you have COPD, quitting smoking is more than just a lifestyle change—it's a medical intervention. Because COPD often comes with high nicotine dependence and increased rates of depression, medications are frequently the \"bridge\" needed to make a quit attempt successful." (`:2323-2337`) | **Missing entirely** |
| 5.17 | "1. Smoking Cessation Medications" + "For 2026, there are three primary paths for medication, plus a promising newcomer." + 4 cards (name / "How it works: …" / "Common side effects: …"): **NRT (Nicotine Replacement)** — "Provides nicotine without the toxic smoke. Best used as a \"Combo\": Patch (steady) + Gum/Lozenge (rescue)." — "Skin irritation, vivid dreams, or jaw soreness."; **Varenicline (Chantix)** — "Blocks the \"pleasure\" receptors in the brain and reduces withdrawal. Currently the most effective monotherapy." — "Nausea (mitigated by food/water) and vivid dreams."; **Bupropion (Zyban)** — "Originally an antidepressant, it reduces the urge to smoke. Good for those with co-occurring depression." — "Dry mouth and insomnia."; **Cytisinicline** — "New for 2026. A plant-based pill similar to Varenicline but shown in recent trials to be highly effective and well-tolerated in COPD patients." — "Mild nausea or headache." (`:2340-2387`) | Missing |
| 5.18 | "2. Impact on COPD Symptoms" + "Medications don't just help you quit; they indirectly improve your COPD management by removing the constant irritation of smoke." + 3 bullets: "Better Inhaler Efficacy: When you stop smoking, the inflammation in your airways begins to subside. This allows your bronchodilators (like Albuterol or Spiriva) to reach deeper into the lungs and work more effectively."; "Reduced \"Mucus Plugs\": Smoking paralyzes the cilia (tiny hairs) that clear mucus. Quitting \"wakes them up,\" helping you clear phlegm more easily and reducing the risk of infections."; "Stabilized Lung Function: While lung damage from COPD is permanent, medications stop the \"accelerated decline.\" You go from losing lung function at a smoker's pace back to a natural aging pace." (`:2390-2419`) | Missing |
| 5.19 | "3. Important Considerations for COPD" + 3 bullets: "Depression & Anxiety: COPD is physically and mentally taxing. Because some quit-smoking meds (like Bupropion or Varenicline) affect brain chemistry, it's vital to monitor your mood. Recent large-scale studies (including those published in early 2026) have confirmed these are generally safe for COPD patients but should be managed by your doctor."; "The \"Quit-Cough\": It sounds counterintuitive, but many COPD patients cough more for the first week after quitting. This is actually a sign of your lungs cleaning themselves out. Don't let it discourage you!"; "Paced Quitting: For those not ready to stop today, \"Reduce to Quit\" programs using NRT can help you slowly lower your daily cigarette count, making the final quit day less of a shock to the system." (`:2422-2442`) | Missing |
| 5.20 | Bullets rendered `"  • $tip"` with 18dp hanging indent (`:2296-2306`) | n/a |
| 5.21 | — | **iOS-only** "Emergency Resources" section — remove |

---

## 6. Daily Tracking Summary (iOS `DailyTrackingSummary.swift`) — exercise-journal cross-check

| # | Android (`TrackingFragment.kt`) | iOS |
|---|---|---|
| 6.1 | Month view heading "Exercise Journal"; empty "No exercise journal entries for this month." (`:913`) | **matches** |
| 6.2 | Summary stats "Entries", "Active Days {n} / {daysInMonth}", "Total Sets"/"Total Reps" only if > 0, "Exercises performed:" + "• {ex}" (`:946-970`) | **matches** |
| 6.3 | No weekly cards on Android (`:973` "Weekly breakdown removed") | **iOS-only** "Weekly Exercise Breakdown" — remove |
| 6.4 | "All Entries This Month" shown only when entries exist; sorted `savedAt` desc; card header `"{MMM d}  •  {timeText}"` (`:977-1057`) | **matches** |
| 6.5 | Month range `[monthStart, nextMonthStart)` on `dayMillis` (`:900`) | equivalent |

## 7. iOS-only extras (summary) — remove unless listed as "keep" in the P3 spec
- Global disclaimer under chips (0.6); `SeverityCalculator.swift` formula + result caption (1.9); "Recommended Exercises" (3.28); `MedicationGuideView` 8 short cards (4.5); Resource Hub NHLBI org (5.7), iOS Educational items (5.8), Emergency Resources (5.21); Weekly Exercise Breakdown (6.3); `GuidelinesView.swift:70-98` Featured COPD Inhalers block (move to Resources; Symbicort URL `https://www.symbicort.com/`); Exercise journal "Unit" label (3x.4); Instructions Cancel always visible (2e.5).
