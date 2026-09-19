# iOS Port Spec — Food Label Scan (from Android `labelscan` / `ui.scan`)

Source of truth is the Android **code**, not `docs/superpowers/specs/2026-09-18-food-label-scan-design.md`. Android paths are relative to `android/app/src/main/` unless noted.

## 0. Where the code deviates from the design doc (code wins)

| Design doc says | Code actually does | Ref |
|---|---|---|
| Only calories/protein/carbs/fat scanned | Also 9 "extra" nutrients (fiber, added sugars, sat fat, cholesterol, sodium, potassium, calcium, iron, vitamin D) | `labelscan/ExtraNutrient.kt:8-19` |
| QR must be GS1 Digital Link or product barcode | Any `http(s)://` QR link is followed (up to 10 redirects, https-first) and the landing page is scraped for a labelled UPC/GTIN | `labelscan/ProductLinkResolver.kt:18-33` |
| Photo Library image is checked for a QR first | Photo Library goes **straight to the review screen as a nutrition-label photo** | `ui/fragments/TrackingFragment.kt:93-104` |
| Barcode scanner limited to QR | Live QR mode uses ML Kit default = all symbologies | `ui/scan/ScanLabelActivity.kt:275` |
| "Retake" returns to camera | "Retake" == "Cancel": review cancels, camera closes; user lands back on Tracking | `LabelReviewActivity.kt:71-75` |
| Flash toggle, pinch zoom, settings link, retry overlay | None implemented | — |
| Front label name = "Scanned food" selected for editing | Name field left empty when no front photo; "Scanned food" substituted only at save time | `LabelReviewActivity.kt:161, 215` |
| Review parses once | Android re-runs OCR in review (bug) — iOS parses once and passes the result | — |

---

## 1. User flows (exact copy)

### 1.1 Entry points
**A. Tracking screen.** Button `+ Quick Add Food` → **AddFoodBottomSheet**.
**B. Inside AddFoodDialog.** 48dp icon button "Scan Label" right of the "Edit Nutrition Manually" toggle opens the same sheet; "Add Food" is a no-op there.

**AddFoodBottomSheet** (`bottom_sheet_add_food.xml`, `strings.xml:56-60`): title **"Add Food"**; tiles (icon 48dp + label): **"Add Food"**, **"Scan Label"**, **"Scan QR Code"**, **"Photo Library"**. Tap dismisses sheet then dispatches. Dispatch: ADD_FOOD → AddFoodDialog; SCAN_LABEL → camera label mode with selected date; SCAN_QR_CODE → camera QR mode with date; PHOTO_LIBRARY → system image picker (no runtime permission).

### 1.2 Camera screen — `ScanLabelActivity`
Layout: black full-screen preview; centered guide rectangle 280×360dp, 3dp white stroke, 8dp corner radius; instruction text top-center (64dp top margin, 18sp white with shadow); a text button bottom-center 120dp from bottom (either **"Skip"** or **"Scan nutrition label instead"**); a 72dp round white shutter button with 4dp gray border 32dp from bottom. Portrait only.

**Permission**: denied → toast **"Camera permission is required to scan labels"** and close. Camera failure → toast **"Camera failed to start"**.

#### 1.2.1 Label mode
1. UI: shutter + **"Skip"** visible; instruction **"Photo front of package (optional)"**.
2. Shutter → capture photo. Failure → toast **"Photo capture failed"**.
3. **First photo**: disable shutter, instruction **"Reading label…"**; run OCR; `looksLikeNutritionPanel(text)` (exceptions → false). Re-enable shutter, instruction → **"Photo Nutrition Facts panel"**, hide Skip. Then: looks like a panel → treat this photo as the nutrition label → `processLabel()`; otherwise → store as front photo, toast **"Front photo saved. Now photograph the Nutrition Facts panel."**
4. **"Skip"** → proceed to nutrition label (no front photo).
5. **Second photo** → `processLabel()`: instruction **"Reading label…"**, shutter disabled; OCR + parse.
   - If `calories`, `protein`, `carbs` and `fat` are **all** nil → toast **"Could not read nutrition label. Try again."**, shutter re-enabled, instruction back to **"Photo Nutrition Facts panel"**.
   - Else → open Review with front image (optional), parsed label, date. Product name = `extractProductName(frontLines)` when a front photo exists.
   - Exception → toast **"Could not read label: {error message}"**, shutter re-enabled, instruction reset.
6. Review result is forwarded to the caller and the camera screen closes.

#### 1.2.2 QR / barcode mode
1. UI: shutter and Skip hidden; **"Scan nutrition label instead"** button visible; instruction **"Point camera at the product barcode or QR code"**.
2. Live frame analysis (frames dropped while processing or a lookup is active). All symbologies.
3. Per frame with barcodes:
   - `gtin = first barcode where extractGtin(rawValue) != nil`
   - `link = first barcode where isWebLink(rawValue)`
   - if gtin → lookup-active, instruction **"Looking up product…"**, `lookupGtin(gtin)`.
   - else if link and link not in the "unresolvable" set → lookup-active, **"Looking up product…"**, `resolveLinkThenLookup(link)`.
   - else if any barcode detected → instruction **"This code has no product number. Point at the striped barcode instead."**
4. `resolveLinkThenLookup`: `ProductLinkResolver().resolveGtin(link)` (any error → nil). Found → `lookupGtin`. Not found → add link to unresolvable set, lookup-active=false, instruction **"This code has no product number. Point at the striped barcode instead."**
5. `lookupGtin`:
   - API key blank → instruction **"Product lookup is not set up (missing USDA API key)."** + toast **"USDA API key not configured"**; reset lookup-active.
   - lookup non-nil → open Review with the parsed label + date.
   - nil → lookup-active=false, instruction back to **"Point camera at the product barcode or QR code"**, toast **"Product not found. Try scanning the label."**
   - error → same reset, toast **"Lookup failed: {error message}"**.
6. **"Scan nutrition label instead"** → switch to label mode with the same date.

### 1.3 Photo Library
Picked image → **Review screen directly** as a nutrition-label photo, no front photo, no barcode detection. OCR error → toast **"Could not read label"** and the review stays open with empty fields. No "nothing found" check on this path.

### 1.4 Review screen — `LabelReviewActivity`
- Title **"Review Scanned Label"**
- Field hints: **"Food name"**, **"Grams per serving"**, **"Calories"**, **"Protein (g)"**, **"Carbs (g)"**, **"Fat (g)"**
- Serving line: **"Serving size: %1$s"** with the parsed description, or **"Serving size: Not detected"**
- Low-confidence note under a macro field: **"Please check this value."** (orange)
- Expander: **"More nutrients  ▾"** collapsed / **"More nutrients  ▴"** expanded (two spaces before the arrow)
- Extra hints: **"Fiber (g)"**, **"Added sugars (g)"**, **"Saturated fat (g)"**, **"Cholesterol (mg)"**, **"Sodium (mg)"**, **"Potassium (mg)"**, **"Calcium (mg)"**, **"Iron (mg)"**, **"Vitamin D (mcg)"**
- Buttons row 1 (filled, equal width): **"Add to today's log"** | **"Save to My Foods"**. Row 2 (outlined): **"Retake"** | **"Cancel"**.
- Meal dialog (both save buttons): title **"Add to which meal?"**, single-choice **Breakfast / Lunch / Dinner / Snacks**, positive **"Add"**, negative **"Cancel"**.
- Retake / Cancel / back → cancelled.

### 1.5 Result handling
- **Tracking path**: insert entry; if save-to-my-foods → `saveScannedFoodToDatabase(entry)` (§4.4). No confirmation toast.
- **AddFoodDialog path**: `prefillFromScan(entry)`: sets food name, shows the manual-entry section ("Hide Manual Entry"), calories as `Int`, protein/carbs/fat as `"%.1f"`. Save-to-my-foods flag and extras are ignored on this path.

---

## 2. Nutrition label parser — `labelscan/NutritionLabelParser.kt` (port 1:1; port tests from `app/src/test/java/com/copdhealthtracker/labelscan/NutritionLabelParserTest.kt`, `FrontLabelNameTest.kt`)

### 2.1 Data types
```
ParsedLabel { productName: String?, servingSize: ServingSize?, calories/protein/carbs/fat: ValueWithConfidence?,
              extras: [ExtraNutrient: ValueWithConfidence] (only found ones), rawLines: [String] }
ServingSize { description: String, grams: Double? }
ValueWithConfidence { value: Double, unit: String, confidence: HIGH|MEDIUM|LOW }   // MEDIUM never produced
OcrLine { text: String, height: Int (px), top: Int (px) }
ExtraNutrient(unit, usdaIds, keywords) in this order:
  FIBER("g", [1079], ["dietary fiber","fiber"])
  ADDED_SUGARS("g", [1235], ["added sugars","added sugar"])
  SATURATED_FAT("g", [1258], ["saturated fat","sat. fat","sat fat"])
  CHOLESTEROL("mg", [1253], ["cholesterol","cholest."])
  SODIUM("mg", [1093], ["sodium"])
  POTASSIUM("mg", [1092], ["potassium","potas."])
  CALCIUM("mg", [1087], ["calcium"])
  IRON("mg", [1089], ["iron"])
  VITAMIN_D("mcg", [1114, 1110], ["vitamin d","vit. d","vit d"])   // 1110 is IU; MCG_PER_IU = 0.025
```

### 2.2 OCR line production (`MlKitLabelOcr.kt:24-52`)
- Lines = every text block's lines, in recognizer order. `recognize()` = line texts joined with `"\n"`.
- `height` = length of the line's **left edge** (top-left→bottom-left) when 4 corners exist, else bounding-box height; `top` = bounding-box top.
- iOS: `VNRecognizedTextObservation` — `height = hypot(bottomLeft − topLeft) × image px`, `top = (1 − boundingBox.maxY) × imageHeight`. Sort observations top-to-bottom, then left-to-right before joining.

### 2.3 `parse(text)`
1. `rawLines` = `text` split on newlines, trimmed, empties dropped.
2. `normalized` = rawLines mapped through `normalizeLine` joined by `"\n"`.
3. `servingSize = extractServingSize(normalized)`.
4. `calories = extractValue(normalized, ["calories"], "kcal")` then **force confidence HIGH**.
5. `protein = extractValue(["protein"], "g")`; `carbs = extractValue(["total carb", "carbohydrate"], "g")`; `fat = extractValue(["total fat", "total lipid"], "g")`.
6. `extras` = for each ExtraNutrient in enum order: `extractValue(keywords, unit)`; include only non-nil.
7. `productName` = nil.

### 2.4 `normalizeLine(line)` — single left-to-right in-place pass over the chars (replacements affect later checks), then collapse `\s+` → single space, then **lowercase**.
For each index `i`, switch on `lowercased(chars[i])`:
- `'o'`: → `'0'` if (prev char is a digit) OR (`chars[i+1] == '.'` and `chars[i+2]` is digit and (`i == 0` or prev is not a letter)).
- `'l'`: → `'1'` if next char is a digit.
- `'s'`: → `'5'` if prev char is a digit.
- `'1'`: if prev is letter AND next is letter → `'i'`; else if prev is letter AND (`i` is last OR next is whitespace) → `'l'`.
`isLetter/isDigit` are Unicode-aware.

### 2.5 `extractServingSize(normalizedText)`
- Regex `serving size\s+(.+?)(?=\n|$)` (case-insensitive) on the whole text; no match → nil. `description` = group 1 trimmed (already lowercased).
- `grams`: first try `\(\s*(\d+(?:\.\d+)?)\s*g`; else `(\d+(?:\.\d+)?)\s*g(?![a-z])`; group 1 → Double, else nil.

### 2.6 `extractValue(text, keywords, unit)`
```
lines = text.split("\n")
for prefix in ["(?<![a-z])", "(?<!un)"]:
  for keyword in keywords (in order):
    pattern = prefix + escape(keyword), case-insensitive
    for line in lines (in order):
      m = first match in line; if none continue
      afterKeyword = line[m.end...]
      r = parseNumericValue(line, afterKeyword, unit); if r != nil return r
return nil
```

### 2.7 `parseNumericValue(line, afterKeyword, unit)`
- `massUnits` (ordered) = `mcg→1e-6, µg→1e-6, ug→1e-6, mg→1e-3, g→1.0`.
- `unitsAlt` = if `unit` is a mass unit: `"mcg|µg|ug|mg|g"`, else `unit` itself (`"kcal"`).
- `unitRegex` = `(\d+(?:\.\d+)?)\s*(unitsAlt)(?![a-z])`, case-insensitive.
- Try `unitRegex` on `afterKeyword` first, then on the whole `line`.
  - Match → `factor = (massUnits[detected] ?? 1) / (massUnits[unit] ?? 1)`; `value = floor(v*factor*1000 + 0.5)/1000`; unit = requested unit; **HIGH**.
- Else fallback on `afterKeyword` only: `(?<![\d.])(\d+(?:\.\d+)?)(?![\d.]|\s*%)` → **LOW**.
- Else nil.

### 2.8 `looksLikeNutritionPanel(text)` — lowercased text contains `"nutrition facts"` OR `"serving size"` OR (`"calories"` AND `"total fat"`).

### 2.9 `extractProductName(frontLines: [OcrLine])` and `extractProductName(String)`
- If `looksLikeNutritionPanel(joined texts)` → nil.
- `lines` = those whose text has ≥ 2 letters; empty → nil.
- `tallest = max(height)`; if `tallest <= 0` → return `lines.first.text.trimmed` as-is.
- Else keep lines with `height >= tallest * 0.45`, drop any containing (case-insensitive) one of `["net wt","per serving","servings per","natural flavor","naturally flavored","artificial flavor","artificially flavored","with other"]`, sort by `top` ascending, take first 6, map `ProductNames.readable(text.trimmed)`, join with `" "`; blank → nil.
- String overload: non-blank lines → `OcrLine(trimmed, 0, 0)`.

### 2.10 `ProductNames.readable(text)` — If text has any lowercase char → unchanged. Else for each char: `previous = text[i-1]` or `' '`; `startsWord = !previous.isLetter && previous != '\'' && previous != '\u{2019}'`; append uppercased if startsWord else lowercased.

### 2.11 Test fixtures to port — read `android/app/src/test/java/com/copdhealthtracker/labelscan/NutritionLabelParserTest.kt` and `FrontLabelNameTest.kt` and port every test case verbatim (inputs and expectations), including the two verbatim ML Kit dumps.

---

## 3. Barcode / GTIN / link / USDA

### 3.1 Symbologies — iOS: `AVCaptureMetadataOutput` / `VNDetectBarcodesRequest` with `.qr, .ean13, .ean8, .upce, .code128, .code39, .dataMatrix, .aztec, .pdf417`. Vision/AVFoundation report UPC-A as EAN-13 with a leading 0 — handled by candidates.

### 3.2 `extractGtin(value)` (`MlKitBarcodeScanner.kt:42-61`)
1. nil/blank → nil.
2. GS1 Digital Link: regex `https?://[^/]+/gtin/(\d+)` (case-insensitive) → group 1 (no check-digit validation).
3. `trimmed = value.trim()`; if all digits → return it iff length ∈ {8, 12, 13, 14}, else nil.
4. Otherwise: all matches of `(?<!\d)\d{12,14}(?!\d)` in `trimmed`, keep those with a valid check digit, return the **longest** (first on ties), else nil.

### 3.3 `hasValidCheckDigit(code)` — `code.count >= 8` and all digits; take all digits except the last, **reverse**, weight index-even ×3 / index-odd ×1, sum; valid iff `(10 − sum % 10) % 10 == lastDigit`.

### 3.4 `ProductLinkResolver`
- `isWebLink(v)`: non-nil and trimmed matches `^https?://\S+$` case-insensitive.
- `resolveGtin(link)`: `current = link.trim()`; repeat **10** times: if `extractGtin(current)` → return it; `hop = fetchPreferringHttps(current)`; if `300...399` and `Location` present → `current = URL(Location, relativeTo: current).absoluteString`; else if `200` → return `findGtinInHtml(body ?? "")`; else return nil. After the loop → `extractGtin(current)`.
- `fetchPreferringHttps(url)`: if url doesn't start with `http://` → fetch as is. Else try `"https" + url.dropFirst(4)`; on network error fall back to the plain `http://` url.
- `httpFetch`: **no automatic redirects**; timeout **8 s**; header `User-Agent: Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36`; returns `(status, Location header, body)`; body read only on 200, capped at **200,000 chars**.
- `findGtinInHtml(html)`: regex `(?i)(?:gtin\d{0,2}|upc)(?:<[^>]*>|[^0-9<]){0,40}?(\d{12,14})(?!\d)`; first group-1 with a valid check digit; else nil.
- Cleartext: allow plain http **only** for `pepsico.info` (+subdomains) via ATS exception `NSExceptionDomains → pepsico.info { NSExceptionAllowsInsecureHTTPLoads = YES, NSIncludesSubdomains = YES }`.
- Tests: port `ProductLinkResolverTest.kt` (7-hop Quaker chain resolves to `00030000570630`; https tried before http; `app.scanlife.com` never fetched over http). Inject the fetch function.

### 3.5 `UsdaGtinLookup`
- `gtinCandidates(gtin)`: `core = gtin` with leading zeros stripped; `[gtin] + [12,13,14].filter{ $0 >= core.count }.map{ core left-padded with "0" }`, de-duplicated, order preserved.
- `lookup(gtin, apiKey)`: for each candidate: `GET https://api.nal.usda.gov/fdc/v1/foods/search?query={candidate}&dataType=Branded&pageSize=1&api_key={key}`; `mapResponse(body, expectedGtin: gtin)`; first non-nil wins.
- `sameGtin(a, b)`: `a` non-blank and both with leading zeros stripped are equal.
- `mapResponse(json, expectedGtin?)`: `foods` empty → nil; take `foods[0]`; if expected given and `!sameGtin(food.gtinUpc ?? "", expected)` → nil. `productName = productName(description ?? "", brandName ?? "")`. `servingSize = food.servingSize ?? 100.0`; `unit = food.servingSizeUnit ?? "g"`; `isGrams = unit.lowercased() ∈ {"g","grm","gram","grams"}`; `servingDesc = "\(formatAmount(servingSize)) \(isGrams ? "g" : unit.lowercased())"` (integers without decimals). `perServing = servingSize / 100`. For each nutrient: `per100g[id] = v`; `scaled = roundToTenth(v * perServing)`; `1008→calories, 1003→protein, 1005→carbs, 1004→fat`. Extras: for each `ExtraNutrient`: `id = first usdaId present`; `amount = per100g[id] * perServing`; if `id == 1110` multiply by `0.025`; `roundToTenth`; HIGH. Return `ParsedLabel(productName, ServingSize(servingDesc, isGrams ? servingSize : nil), macros as HIGH, extras)`. `roundToTenth(x) = floor(x*10 + 0.5)/10`.
- `productName(description, brandName)`: split description on `,`, trim, drop empties; keep a part only if `parts` is empty or the part's words (lowercased, split on `\W+`) are **not all** already among the words of the parts kept so far; `name = readable(parts.joined(", "))`; `brand = readable(brandName.trimmed)`; both empty → **"Scanned food"**; brand empty or `name` contains `brand` (case-insensitive) → `name`; name empty → `brand`; else `"\(brand) \(name)"`.
- Tests: port `UsdaGtinLookupTest.kt` verbatim (Quaker fixture → "Quaker Maple & Brown Sugar Protein Instant Oatmeal", 220.2 kcal, 10.0 / 40.0 / 3.5, 60 g; extras 4.0 / 11.0 / 0.5 / 4.8 / 289.8 / 180.0 / 40.2 / 1.6; vitamin D 7 IU/100g → 0.1 mcg; name cases).

### 3.6 iOS mapping — `FoodDatabaseService.searchUSDA` already hits the same endpoint with `AppConfig.usdaApiKey` (Info.plist `USDA_FDC_API_KEY`).

---

## 4. Review screen — fields, save semantics

### 4.1 Inputs — a `ParsedLabel` (QR/USDA or OCR path) plus optional front-photo product name and a `date`.

### 4.2 Binding
| Field | Prefill | Editable | Note |
|---|---|---|---|
| Food name | `productName ?? ""` | text | |
| Serving size line | "Serving size: {description}" or "Serving size: Not detected" | read-only | |
| Grams per serving | `grams` or "" | decimal | |
| Calories / Protein (g) / Carbs (g) / Fat (g) | value or "" | decimal | "Please check this value." iff confidence == LOW |
| More nutrients (9 fields, enum order) | `extras[n].value` or "" | decimal | auto-expanded iff `extras` non-empty |
| "Save to My Foods" | enabled iff grams field parses as Double (live) | | |

### 4.3 Meal picker & entry construction (`MealCategories.kt`)
- `MealCategories.ALL = ["Breakfast","Lunch","Dinner","Snacks"]`; default by current hour: 5–10 Breakfast, 11–14 Lunch, 17–20 Dinner, else Snacks (port `MealCategoriesTest.kt`).
- On "Add": `name = trimmed name, or "Scanned food" if empty`; macros `Double(text) ?? 0`; `grams = Double(gramsText)`; `quantity = grams != nil ? "1 serving (\(Int(grams))g)" : "1 serving"`; extras → `fiber, sodium, potassium, calcium, iron, vitaminD, saturatedFat, cholesterol, addedSugars`; other nutrients 0; `mealCategory`, `date`.

### 4.4 "Save to My Foods" (`TrackingFragment.kt:1975-2002`)
```
grams = first match of \((\d+(?:\.\d+)?)g\) in entry.quantity → else return
factor = grams / 100 ; if factor <= 0 return
UserAddedFood(name: entry.name, category/categoryGroup "User added", per-100g = entry.x / factor for the 14 fields)
insertUserAddedFood — no insert if a food with the same name already exists; no toast on the scan path
```

---

## 5. Permissions & photo library
- iOS: add `INFOPLIST_KEY_NSCameraUsageDescription` ("COPD Fuel uses the camera to scan nutrition labels and product barcodes."). `PhotosPicker` needs no permission. Add `USDA_FDC_API_KEY` Info.plist entry (empty placeholder in the pbxproj; real key via xcconfig/local override). Denied camera → the Android message and dismiss.
- On-device OCR/barcode only; only the GTIN digits (and the QR URL, for link resolution) go to the network. Keep captured images in memory only.

---

## 6. iOS architecture
- `COPDFuel/LabelScanKit/` Swift package (pure Foundation): `ParsedLabel`, `ExtraNutrient`, `OcrLine`, `NutritionLabelParser`, `ProductNames`, `GTIN`, `ProductLinkResolver`, `USDAGtinLookup`, `MealCategories`; XCTest ports of the 6 Android test files.
- App target: `Services/LabelScan/LabelOCR.swift` (Vision, `.accurate`, `usesLanguageCorrection = false`), `Views/Scan/AddFoodOptionsSheet.swift`, `Views/Scan/ScanLabelView.swift` (AVCaptureSession + AVCapturePhotoOutput + AVCaptureMetadataOutput; single state machine `.qr / .frontPhoto / .nutritionPhoto`), `Views/Scan/LabelReviewView.swift`.
- `URLSession` with a delegate that refuses redirects (`completionHandler(nil)`), `timeoutIntervalForRequest = 8`, the User-Agent header, body truncated to 200k chars.
- Toasts via `ToastCenter`; meal picker via `.confirmationDialog`-style sheet with a `Picker` preselected by `MealCategories.defaultFor(hour)` and "Add"/"Cancel".
- Results: closure `onComplete(FoodEntry, saveToMyFoods: Bool)` → `DataManager.addFoodEntry` (+ per-100g `UserAddedFood` with duplicate-name check) then dismiss; from the AddFood dialog, prefill the manual fields and show them.

## Android file index
- `labelscan/ExtraNutrient.kt`, `LabelOcr.kt`, `OcrLine.kt`, `ParsedLabel.kt`, `NutritionLabelParser.kt`, `ProductNames.kt`, `MlKitLabelOcr.kt`, `MlKitBarcodeScanner.kt`, `ProductLinkResolver.kt`, `UsdaGtinLookup.kt`
- `ui/scan/ScanLabelActivity.kt`, `LabelReviewActivity.kt`, `MealCategories.kt`, `LabelCaptureViewModel.kt`
- `ui/bottomsheets/AddFoodBottomSheet.kt`; `ui/fragments/TrackingFragment.kt` (78-104, 170, 1932-2002); `ui/dialogs/AddFoodDialog.kt` (52-71, 216-225, 796-823); `repository/DataRepository.kt:36-40`
- `res/layout/activity_scan_label.xml`, `activity_label_review.xml`, `bottom_sheet_add_food.xml`, `item_review_nutrient.xml`; `res/values/strings.xml:56-97`; `res/xml/network_security_config.xml`
- Tests: `android/app/src/test/java/com/copdhealthtracker/labelscan/*.kt`, `ui/scan/MealCategoriesTest.kt`
