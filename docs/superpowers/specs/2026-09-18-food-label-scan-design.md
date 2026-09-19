# Food Label Scanning — Design Spec

**Date:** 2026-09-18
**Scope:** Let the Android app take photos of packaged-food labels and pre-fill the dietary section with macronutrients.
**Platform:** Android (`android/`), Kotlin, `minSdk 26`.

## Decisions made

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Recognition engine | Google ML Kit Text Recognition v2 on-device | No photo leaves the phone; no backend/BA work; no network dependency. |
| Photo capture | System camera app via `Intent(MediaStore.ACTION_IMAGE_CAPTURE`) + `FileProvider` | No `CAMERA` permission, no CameraX lifecycle code, no Play Store declaration needed. |
| Serving-size handling | Trust the label; if grams are absent, allow the meal entry but do not offer "Add to my food database" | The food database stores nutrients **per 100g**; silently guessing a gram weight corrupts reusable entries. |
| Nutrients scanned | Calories, Protein, Carbs, Fat only | Keep the existing lung-function-focused macro labels already used in Tracking and reports. |
| Front-label photo | Optional | The front label only supplies the product name; the feature works with the nutrition panel alone. |

## Out of scope (for this phase)

- Scanning micronutrients (sodium, fiber, saturated fat, added sugars, etc.) even though the tracking UI already supports them.
- Cloud OCR/LLM fallback.
- Adding a `UserAddedFood` entry automatically; we surface the option only when the label includes a gram serving size.

## Architecture

Three layers, each isolated behind a small interface:

```
┌─────────────────────────────────────────────────────────────┐
│ UI: ScanLabelDialog + LabelReviewDialog                      │
│   - Launch camera intents (front label optional, nutrition   │
│     panel required)                                          │
│   - Show parsed values as editable fields                    │
│   - Return a FoodEntry on save                               │
├─────────────────────────────────────────────────────────────┤
│ Domain: NutritionLabelParser                                 │
│   - Pure Kotlin, zero Android imports                        │
│   - Input: raw OCR text                                      │
│   - Output: ParsedLabel (macros + serving info + confidence) │
├─────────────────────────────────────────────────────────────┤
│ Infra: MlKitLabelOcr                                         │
│   - Interface LabelOcr                                       │
│   - ML Kit text recognition                                  │
└─────────────────────────────────────────────────────────────┘
```

### Why this split

The risky part of this feature is parsing, not ML Kit. Nutrition Facts panels follow a regular structure but OCR output creates four recurring failure modes:

1. **Daily Value trap:** `Total Fat 8g 10%` — a naive regex grabs `10` (the %DV) instead of `8g`. The parser must prefer values that carry a unit.
2. **Two-column panels:** `Per 1 slice | Per 2 slices` or `Per serving | Per 100g`. The parser must detect the column headers and read one consistent column.
3. **OCR noise:** `l`/`1`, `O`/`0`, `S`/`5`, line splits (`Total`/`Carbohydrate`), and merged lines.
4. **Serving size:** `Serving Size 2/3 cup (55g)` — the gram weight in parentheses drives the per-100g conversion and is sometimes missing.

Putting parsing in a pure-Kotlin function lets us test these cases in milliseconds without a device.

## Components

### 1. `LabelOcr` interface + `MlKitLabelOcr`

```kotlin
interface LabelOcr {
    suspend fun recognize(uri: Uri): String
}
```

- Uses ML Kit `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)`.
- Returns concatenated text lines with line breaks preserved.
- Single coroutine dispatcher; caller provides `Uri`.

### 2. `NutritionLabelParser`

Pure Kotlin. Input: `String`. Output:

```kotlin
data class ParsedLabel(
    val productName: String? = null,
    val servingSize: ServingSize? = null,
    val calories: ValueWithConfidence? = null,
    val protein: ValueWithConfidence? = null,
    val carbs: ValueWithConfidence? = null,
    val fat: ValueWithConfidence? = null,
    val rawLines: List<String> = emptyList()
)

data class ServingSize(
    val description: String,
    val grams: Double? // null if the label only says "2 slices" with no gram weight
)

data class ValueWithConfidence(
    val value: Double,
    val unit: String, // "g", "mg", "kcal"
    val confidence: Confidence
)

enum class Confidence { HIGH, MEDIUM, LOW }
```

Parsing strategy:

1. **Normalize** the text: collapse repeated whitespace, fix common OCR substitutions (`O`→`0` inside numbers, `l`→`1`), lowercase.
2. **Detect columns** by looking for header tokens such as `Per serving`, `Per 100g`, `Per 1 slice`, `Per 2 slices`, `Amount per serving`. If two columns are found, select the `Per serving` / `Amount per serving` column.
3. **Extract serving size** from a line matching `Serving Size ...`. Pull gram weight from parentheses using `\((\d+(\.\d+)?)\s*g\)`.
4. **Extract nutrients** by scanning lines for keywords:
   - `Calories` → `kcal`
   - `Protein` → `g`
   - `Total Carbohydrate` / `Total Carbs` / `Carbohydrate` → `g`
   - `Total Fat` / `Total Lipid` → `g`
5. **Prefer unit-bearing values.** For each nutrient line, collect all numeric tokens. Prefer the token followed by `g` or `mg`. If only a %DV number exists, skip it. If no unit is found, keep the first number and mark `Confidence.LOW`.
6. **Confidence rules:**
   - `HIGH`: unit is explicit and the keyword appears unambiguously.
   - `MEDIUM`: value found but on a noisy or merged line, or the keyword is a near match.
   - `LOW`: number found without a unit, or in a two-column panel where the column selection is uncertain.

### 3. `ScanLabelDialog`

A full-screen dialog (or fragment) that guides the user:

1. **Instruction step**
   - Title: "Scan food label"
   - Body: "Take a clear photo of the Nutrition Facts panel. You can also photograph the front of the package to capture the product name."
   - Buttons: "Scan nutrition label" (required), "Scan front label" (optional), "Cancel".
2. **Camera launch**
   - Use `ActivityResultContracts.TakePicture()` with a `FileProvider` URI.
   - Front label: stores the resulting URI for product-name extraction.
   - Nutrition label: proceeds to OCR.
3. **OCR step**
   - Show indeterminate progress with text "Reading label…"
   - Run `MlKitLabelOcr.recognize()` and then `NutritionLabelParser.parse()`.
4. **Navigation**
   - On success, open `LabelReviewDialog`.
   - On failure (no text, no nutrients found), show a retry message: "We couldn't read this label. Try again with better lighting and the label filling the frame."

### 4. `LabelReviewDialog`

Displays the parsed values as editable fields:

- Food name (pre-filled from front label if available; otherwise "Scanned food" selected for editing)
- Serving size (read-only, parsed text)
- Grams per serving (editable; pre-filled if parsed)
- Calories
- Protein
- Carbs
- Fat

Fields with `Confidence.LOW` are highlighted with an amber border and a small note: "Please check this value."

Actions:

- **"Add to today's log"** — returns a `FoodEntry` scaled by the entered serving amount (default 1 serving).
- **"Save to my foods"** — enabled only when a gram weight is present; creates a `UserAddedFood` entry scaled to per-100g, then returns the meal entry.
- **"Retake"** — goes back to `ScanLabelDialog`.
- **"Cancel"** — discards.

### 5. Integration with `AddFoodDialog`

Add a new button in `dialog_add_food.xml` next to the manual-entry toggle: **"Scan label"**. Tapping it launches `ScanLabelDialog` from `AddFoodDialog`. When the review dialog returns:

- If the user chose **"Add to today's log"**, pre-populate the manual section in `AddFoodDialog` with the scanned macros and product name. The user can still edit before pressing Save.
- If the user also chose **"Save to my foods"**, call the existing `insertUserAddedFood` callback so the food becomes searchable.

This reuses all existing save/validate paths in `AddFoodDialog`; no new persistence code is required.

## Data flow

```
User taps "Scan label" in AddFoodDialog
        │
        ▼
ScanLabelDialog (instruction)
        │
        ▼
System camera → front label photo (optional)
        │
        ▼
System camera → nutrition label photo (required)
        │
        ▼
MlKitLabelOcr.recognize(uri) → raw text
        │
        ▼
NutritionLabelParser.parse(text) → ParsedLabel
        │
        ▼
LabelReviewDialog (editable fields, LOW confidence highlighted)
        │
        ├─ Retake ───────────────────────┐
        │                                  │
        ▼                                  │
AddFoodDialog manual section ◄───────────┘
        │
        ▼
Existing Save path → FoodEntry
```

## Error handling

| Scenario | Behavior |
|----------|----------|
| No text detected | Retry message, offer retake |
| Text detected but no nutrient keywords found | Retry message, suggest cropping closer to the nutrition panel |
| Macros found but serving size not found | Still proceed; review screen prompts user to enter serving grams if they want to save to "My foods" |
| Serving size found but no gram weight | Disable "Save to my foods"; meal entry still works |
| Low-confidence macro | Highlight field in amber; user must review before save |
| OCR throws (ML Kit error) | Toast: "Could not read label. Please try again." |

## Security / HIPAA

- No image or OCR text is uploaded. ML Kit bundled text recognition runs entirely on-device.
- The captured photos are written to the app's private `cacheDir` via `FileProvider`; delete them after OCR completes or when the dialog is dismissed, whichever comes last.
- Because no PHI leaves the device for this feature, no BAA amendment is needed.

## Dependencies

Add to `app/build.gradle`:

```groovy
implementation 'com.google.mlkit:text-recognition:16.0.1'
```

APK impact: ~4 MB. This is acceptable relative to the existing 24.2 MB `assets/food_database.json`.

No `CAMERA` permission is added.

## File changes

### New files

- `app/src/main/java/com/copdhealthtracker/labelscan/LabelOcr.kt`
- `app/src/main/java/com/copdhealthtracker/labelscan/MlKitLabelOcr.kt`
- `app/src/main/java/com/copdhealthtracker/labelscan/NutritionLabelParser.kt`
- `app/src/main/java/com/copdhealthtracker/labelscan/ParsedLabel.kt`
- `app/src/main/java/com/copdhealthtracker/ui/dialogs/ScanLabelDialog.kt`
- `app/src/main/java/com/copdhealthtracker/ui/dialogs/LabelReviewDialog.kt`
- `app/src/main/res/layout/dialog_scan_label.xml`
- `app/src/main/res/layout/dialog_label_review.xml`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/test/java/com/copdhealthtracker/labelscan/NutritionLabelParserTest.kt`

### Modified files

- `app/build.gradle` — add ML Kit dependency, ensure `testImplementation` already present.
- `app/src/main/AndroidManifest.xml` — add `FileProvider`.
- `app/src/main/java/com/copdhealthtracker/ui/dialogs/AddFoodDialog.kt` — add "Scan label" entry point and result handler.
- `app/src/main/res/layout/dialog_add_food.xml` — add "Scan label" button.
- `app/src/main/res/values/strings.xml` — add scan-related strings.

## Testing

Introduce the first unit test source set (`app/src/test`) because the parser is pure Kotlin.

Test fixtures (canned OCR text) should cover:

1. Standard single-column US Nutrition Facts panel.
2. Two-column panel (`Per 1 slice` / `Per 2 slices`).
3. Label with only %DV and no unit for some macros (should skip %DV).
4. Label with serving size in grams.
5. Label with serving size but no gram weight.
6. OCR-noisy text (`Tota1 Fat 8g`, `Prote1n`, `Calories 1OO`).
7. Front-label product name extraction.

## Open questions / future work

- **Micronutrient scanning:** The Tracking screen already renders sodium, fiber, saturated fat, cholesterol, added sugars, and potassium bars. When desired, extending the parser and review dialog to those fields is additive — no architecture change needed.
- **Auto-add to database:** For this phase we keep it manual (user taps "Save to my foods"). We can make it automatic once the parser has proven reliable in production.
- **iOS parity:** This spec is Android-only. A future iOS parity pass should use `Vision` text recognition and share the same parser logic (rewritten in Swift or via Kotlin Multiplatform).
