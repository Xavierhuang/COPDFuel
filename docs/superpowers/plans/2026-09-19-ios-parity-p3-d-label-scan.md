# iOS Parity P3.D (Food label scan) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the Android food-label scan feature to iOS: a pure-Swift `LabelScanKit` package (parser, GTIN, product-link resolver, USDA GTIN lookup, meal categories) with the six Android unit-test files ported 1:1 and green under `swift test`; Vision OCR; the camera screen (label mode with optional front photo, QR/barcode mode with live lookup); the review screen; Photo Library entry; and the Tracking / Add Food hooks (four-option sheet, scan icon button, save-to-my-foods).

**Architecture:** `COPDFuel/LabelScanKit/` is a local SwiftPM package (Foundation only, no UIKit/Vision) linked into the app target through a local package reference in `project.pbxproj`. The app target adds `Services/LabelScan/LabelOCR.swift` (Vision), `Views/Scan/AddFoodOptionsSheet.swift`, `Views/Scan/ScanLabelView.swift` (AVFoundation camera + one state machine), `Views/Scan/LabelReviewView.swift`, `Views/Scan/PhotoLibraryLabelPicker.swift`. A repo-root `Info.plist` (merged with the generated one via `INFOPLIST_FILE`) carries the camera usage string, the `pepsico.info` ATS exception and the `USDA_FDC_API_KEY` placeholder. Toasts via `ToastCenter`. Regexes use `NSRegularExpression` because the parser relies on look-behind.

**Tech Stack:** SwiftPM (tools 5.9), XCTest, Vision (`VNRecognizeTextRequest`), AVFoundation (`AVCaptureSession`, `AVCapturePhotoOutput`, `AVCaptureMetadataOutput`), PhotosUI (`PhotosPicker`), URLSession.

**Spec:** `docs/superpowers/specs/2026-09-19-ios-parity-p3-design.md` §2.3, §3 row P3.D, §4 (tests). Port spec: `docs/superpowers/specs/2026-09-19-ios-parity-p3/audit-scan-port-spec.md` (§0–§6; code wins over the design doc). Android sources: `labelscan/*.kt`, `ui/scan/*.kt`, `ui/bottomsheets/AddFoodBottomSheet.kt`, `res/values/strings.xml:59-97`, tests under `app/src/test/java/com/copdhealthtracker/labelscan/` and `ui/scan/`.

## Global Constraints

- Copy verbatim from `strings.xml` / the Kotlin (toasts via `ToastCenter`).
- Android bugs NOT ported (spec §1.3): review parses once (parser output is passed to the review; only the Photo Library path runs OCR in the review); the camera instruction text is reset after a label-read exception.
- Package code is Foundation-only so `swift test --package-path COPDFuel/LabelScanKit` runs on macOS; `Package.swift` declares both iOS 17 and macOS 13.
- MemberImportVisibility: app files with `ObservableObject`/`@Published` import `Combine`.
- Depends on P3.0 (`ToastCenter`), P3.C (1/2) (`TrackingDayView`, `TrackingDates`, `TrackingButton`), P3.C (2/2) (`AddFoodDialog`, `DataManager.addUserAddedFood -> Bool`).
- Build (`xcodebuild … build`) + `swift test` before the single commit at the end. `/usr/bin/git`; commands from the parent repo root.

---

### Task 1: `LabelScanKit` package skeleton + models + `MealCategories` (+ tests)

**Files:**
- Create: `COPDFuel/LabelScanKit/Package.swift`
- Create: `COPDFuel/LabelScanKit/Sources/LabelScanKit/Models.swift`
- Create: `COPDFuel/LabelScanKit/Sources/LabelScanKit/MealCategories.swift`
- Create: `COPDFuel/LabelScanKit/Sources/LabelScanKit/RegexSupport.swift`
- Create: `COPDFuel/LabelScanKit/Tests/LabelScanKitTests/MealCategoriesTests.swift`

**Interfaces (produced):**
- `Confidence`, `ValueWithConfidence(value:unit:confidence:)`, `ServingSize(description:grams:)`, `ExtraNutrient` (`.fiber … .vitaminD`, `unit`, `usdaIds`, `keywords`, `ExtraNutrient.usdaVitaminDIU = 1110`, `.mcgPerIUVitaminD = 0.025`), `ParsedLabel(productName:servingSize:calories:protein:carbs:fat:extras:rawLines:)`, `OcrLine(text:height:top:)`.
- `MealCategories.all`, `MealCategories.defaultFor(hourOfDay:)`.
- `Regex` helpers (internal): `Regex.firstMatch(_ pattern:in:) -> [String]?` (group values, index 0 = whole), `Regex.allMatches(_:in:) -> [[String]]`, `Regex.matches(_:_:)`, `Regex.escape(_:)`, `Regex.replaceAll(_:in:with:)`.

- [ ] **Step 1: `Package.swift`**

```swift
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "LabelScanKit",
    platforms: [.iOS(.v17), .macOS(.v13)],
    products: [
        .library(name: "LabelScanKit", targets: ["LabelScanKit"])
    ],
    targets: [
        .target(name: "LabelScanKit"),
        .testTarget(name: "LabelScanKitTests", dependencies: ["LabelScanKit"])
    ]
)
```

- [ ] **Step 2: `Sources/LabelScanKit/Models.swift`** (port of ParsedLabel.kt, ExtraNutrient.kt, OcrLine.kt)

```swift
import Foundation

public enum Confidence: Equatable { case high, medium, low }

public struct ValueWithConfidence: Equatable {
    public let value: Double
    public let unit: String
    public let confidence: Confidence
    public init(value: Double, unit: String, confidence: Confidence) {
        self.value = value; self.unit = unit; self.confidence = confidence
    }
    func with(confidence: Confidence) -> ValueWithConfidence {
        ValueWithConfidence(value: value, unit: unit, confidence: confidence)
    }
}

public struct ServingSize: Equatable {
    public let description: String
    public let grams: Double?
    public init(description: String, grams: Double?) { self.description = description; self.grams = grams }
}

/// Nutrients beyond the four macros that a scan can fill in (ExtraNutrient.kt).
public enum ExtraNutrient: CaseIterable, Hashable {
    case fiber, addedSugars, saturatedFat, cholesterol, sodium, potassium, calcium, iron, vitaminD

    public static let usdaVitaminDIU = 1110
    public static let mcgPerIUVitaminD = 0.025

    public var unit: String {
        switch self {
        case .fiber, .addedSugars, .saturatedFat: return "g"
        case .cholesterol, .sodium, .potassium, .calcium, .iron: return "mg"
        case .vitaminD: return "mcg"
        }
    }

    /// FoodData Central nutrient ids, most preferred first.
    public var usdaIds: [Int] {
        switch self {
        case .fiber: return [1079]
        case .addedSugars: return [1235]
        case .saturatedFat: return [1258]
        case .cholesterol: return [1253]
        case .sodium: return [1093]
        case .potassium: return [1092]
        case .calcium: return [1087]
        case .iron: return [1089]
        case .vitaminD: return [1114, 1110]
        }
    }

    /// How the row is printed on a Nutrition Facts panel, most specific first.
    public var keywords: [String] {
        switch self {
        case .fiber: return ["dietary fiber", "fiber"]
        case .addedSugars: return ["added sugars", "added sugar"]
        case .saturatedFat: return ["saturated fat", "sat. fat", "sat fat"]
        case .cholesterol: return ["cholesterol", "cholest."]
        case .sodium: return ["sodium"]
        case .potassium: return ["potassium", "potas."]
        case .calcium: return ["calcium"]
        case .iron: return ["iron"]
        case .vitaminD: return ["vitamin d", "vit. d", "vit d"]
        }
    }
}

public struct ParsedLabel: Equatable {
    public var productName: String?
    public var servingSize: ServingSize?
    public var calories: ValueWithConfidence?
    public var protein: ValueWithConfidence?
    public var carbs: ValueWithConfidence?
    public var fat: ValueWithConfidence?
    /// Only nutrients that were actually found; a missing row is absent, not zero.
    public var extras: [ExtraNutrient: ValueWithConfidence]
    public var rawLines: [String]

    public init(productName: String? = nil, servingSize: ServingSize? = nil,
                calories: ValueWithConfidence? = nil, protein: ValueWithConfidence? = nil,
                carbs: ValueWithConfidence? = nil, fat: ValueWithConfidence? = nil,
                extras: [ExtraNutrient: ValueWithConfidence] = [:], rawLines: [String] = []) {
        self.productName = productName; self.servingSize = servingSize
        self.calories = calories; self.protein = protein; self.carbs = carbs; self.fat = fat
        self.extras = extras; self.rawLines = rawLines
    }
}

/// One line of recognized text. `height` and `top` are in image pixels.
public struct OcrLine: Equatable {
    public let text: String
    public let height: Int
    public let top: Int
    public init(text: String, height: Int, top: Int) { self.text = text; self.height = height; self.top = top }
}
```

- [ ] **Step 3: `Sources/LabelScanKit/MealCategories.swift`**

```swift
import Foundation

/// The meals the Tracking screen groups food entries by (MealCategories.kt).
public enum MealCategories {
    public static let all = ["Breakfast", "Lunch", "Dinner", "Snacks"]

    /// The meal to pre-select when asking where a scanned food belongs.
    public static func defaultFor(hourOfDay: Int) -> String {
        switch hourOfDay {
        case 5...10: return "Breakfast"
        case 11...14: return "Lunch"
        case 17...20: return "Dinner"
        default: return "Snacks"
        }
    }
}
```

- [ ] **Step 4: `Sources/LabelScanKit/RegexSupport.swift`**

```swift
import Foundation

/// Thin NSRegularExpression wrapper (the parser needs look-behind, which the
/// Swift Regex literal does not guarantee on all toolchains).
enum Regex {
    private static var cache: [String: NSRegularExpression] = [:]
    private static let lock = NSLock()

    static func compiled(_ pattern: String, caseInsensitive: Bool = false) -> NSRegularExpression {
        let key = (caseInsensitive ? "i:" : "c:") + pattern
        lock.lock(); defer { lock.unlock() }
        if let r = cache[key] { return r }
        let r = try! NSRegularExpression(pattern: pattern, options: caseInsensitive ? [.caseInsensitive] : [])
        cache[key] = r
        return r
    }

    /// Group values of the first match (index 0 = whole match; missing groups → "").
    static func firstMatch(_ pattern: String, in text: String, caseInsensitive: Bool = false) -> [String]? {
        let r = compiled(pattern, caseInsensitive: caseInsensitive)
        let ns = text as NSString
        guard let m = r.firstMatch(in: text, range: NSRange(location: 0, length: ns.length)) else { return nil }
        return groups(m, in: ns)
    }

    static func allMatches(_ pattern: String, in text: String, caseInsensitive: Bool = false) -> [[String]] {
        let r = compiled(pattern, caseInsensitive: caseInsensitive)
        let ns = text as NSString
        return r.matches(in: text, range: NSRange(location: 0, length: ns.length)).map { groups($0, in: ns) }
    }

    /// Whole-string match (Kotlin `Regex.matches`).
    static func matches(_ pattern: String, _ text: String, caseInsensitive: Bool = false) -> Bool {
        guard let m = firstMatch("^(?:" + pattern + ")$", in: text, caseInsensitive: caseInsensitive) else { return false }
        return m[0] == text
    }

    static func replaceAll(_ pattern: String, in text: String, with template: String) -> String {
        let r = compiled(pattern)
        return r.stringByReplacingMatches(in: text, range: NSRange(location: 0, length: (text as NSString).length), withTemplate: template)
    }

    static func escape(_ literal: String) -> String { NSRegularExpression.escapedPattern(for: literal) }

    /// Range (in `text`) of the first match, for "text after the keyword".
    static func firstRange(_ pattern: String, in text: String, caseInsensitive: Bool = false) -> Range<String.Index>? {
        let r = compiled(pattern, caseInsensitive: caseInsensitive)
        guard let m = r.firstMatch(in: text, range: NSRange(location: 0, length: (text as NSString).length)) else { return nil }
        return Range(m.range, in: text)
    }

    private static func groups(_ m: NSTextCheckingResult, in ns: NSString) -> [String] {
        (0..<m.numberOfRanges).map { i in
            let r = m.range(at: i)
            return r.location == NSNotFound ? "" : ns.substring(with: r)
        }
    }
}
```

- [ ] **Step 5: `Tests/LabelScanKitTests/MealCategoriesTests.swift`** (MealCategoriesTest.kt)

```swift
import XCTest
@testable import LabelScanKit

final class MealCategoriesTests: XCTestCase {
    func testListsTheFourMealsTheTrackingScreenGroupsBy() {
        XCTAssertEqual(["Breakfast", "Lunch", "Dinner", "Snacks"], MealCategories.all)
    }

    func testSuggestsAMealFromTheTimeOfDay() {
        XCTAssertEqual("Breakfast", MealCategories.defaultFor(hourOfDay: 7))
        XCTAssertEqual("Lunch", MealCategories.defaultFor(hourOfDay: 12))
        XCTAssertEqual("Dinner", MealCategories.defaultFor(hourOfDay: 18))
    }

    func testSuggestsSnacksBetweenMealsAndLateAtNight() {
        XCTAssertEqual("Snacks", MealCategories.defaultFor(hourOfDay: 15))
        XCTAssertEqual("Snacks", MealCategories.defaultFor(hourOfDay: 22))
        XCTAssertEqual("Snacks", MealCategories.defaultFor(hourOfDay: 2))
    }
}
```

- [ ] **Step 6: Run** `swift test --package-path COPDFuel/LabelScanKit 2>&1 | tail -5` → expected `Executed 3 tests, with 0 failures`.

---

### Task 2: `ProductNames` + `NutritionLabelParser` (+ tests)

**Files:**
- Create: `COPDFuel/LabelScanKit/Sources/LabelScanKit/ProductNames.swift`
- Create: `COPDFuel/LabelScanKit/Sources/LabelScanKit/NutritionLabelParser.swift`
- Create: `COPDFuel/LabelScanKit/Tests/LabelScanKitTests/NutritionLabelParserTests.swift`
- Create: `COPDFuel/LabelScanKit/Tests/LabelScanKitTests/FrontLabelNameTests.swift`

- [ ] **Step 1: Write the failing tests first — `NutritionLabelParserTests.swift`** (every case from NutritionLabelParserTest.kt, inputs verbatim)

```swift
import XCTest
@testable import LabelScanKit

final class NutritionLabelParserTests: XCTestCase {

    func testParsesStandardSingleColumnLabel() {
        let text = """
        Nutrition Facts
        Serving Size 2/3 cup (55g)
        Calories 230
        Total Fat 8g
        Total Carbohydrate 37g
        Protein 3g
        """
        let result = NutritionLabelParser.parse(text)
        XCTAssertEqual("2/3 cup (55g)", result.servingSize?.description)
        XCTAssertEqual(55.0, result.servingSize?.grams)
        XCTAssertEqual(230.0, result.calories?.value)
        XCTAssertEqual(8.0, result.fat?.value)
        XCTAssertEqual(37.0, result.carbs?.value)
        XCTAssertEqual(3.0, result.protein?.value)
        XCTAssertEqual(.high, result.fat?.confidence)
    }

    func testPrefersUnitOverPercentDailyValue() {
        let result = NutritionLabelParser.parse("Total Fat 8g 10%")
        XCTAssertEqual(8.0, result.fat?.value)
        XCTAssertEqual("g", result.fat?.unit)
    }

    func testHandlesMissingUnitWithLowConfidence() {
        let result = NutritionLabelParser.parse("Protein 5")
        XCTAssertEqual(5.0, result.protein?.value)
        XCTAssertEqual(.low, result.protein?.confidence)
    }

    func testFixesCommonOcrSubstitutions() {
        let result = NutritionLabelParser.parse("Calories 1OO\nTota1 Fat 8g\nProte1n 5g")
        XCTAssertEqual(100.0, result.calories?.value)
        XCTAssertEqual(8.0, result.fat?.value)
        XCTAssertEqual(5.0, result.protein?.value)
    }

    func testServingSizeWithoutGramsHasNilGrams() {
        let result = NutritionLabelParser.parse("Serving Size 2 slices")
        XCTAssertEqual("2 slices", result.servingSize?.description)
        XCTAssertNil(result.servingSize?.grams)
    }

    func testParsesAbbreviatedTotalCarb() {
        let result = NutritionLabelParser.parse("Sodium 290mg 12%\nTotal Carb. 40g 15%\nDietary Fiber 4g")
        XCTAssertEqual(40.0, result.carbs?.value)
        XCTAssertEqual(.high, result.carbs?.confidence)
    }

    func testReadsServingGramsWhenOcrManglesTheClosingParen() {
        XCTAssertEqual(60.0, NutritionLabelParser.parse("Serving size 1 package (60g|").servingSize?.grams)
    }

    func testCaloriesWithoutAUnitIsHighConfidence() {
        let result = NutritionLabelParser.parse("Calories 220")
        XCTAssertEqual(220.0, result.calories?.value)
        XCTAssertEqual(.high, result.calories?.confidence)
    }

    func testProductNameIsFirstLineOfFrontLabel() {
        XCTAssertEqual("Quaker Oatmeal", NutritionLabelParser.extractProductName("\nQuaker Oatmeal\nMaple Brown Sugar"))
    }

    func testNutritionPanelIsNotUsedAsProductName() {
        XCTAssertNil(NutritionLabelParser.extractProductName("Nutrition Facts\nServing size 1 package (60g)\nCalories 220"))
    }

    // Rows as printed on a Quaker protein oatmeal cup; vitamins and minerals share lines.
    private let fullLabel = """
    Nutrition Facts
    Serving size 1 package (60g)
    Calories 220
    Total Fat 3.5g 4%
    Saturated Fat 0.5g 4%
    Trans Fat 0g
    Cholesterol 5mg 2%
    Sodium 290mg 12%
    Total Carb. 40g 15%
    Dietary Fiber 4g 14%
    Total Sugars 12g
    Incl. 11g Added Sugars 22%
    Protein 10g 17%
    Vitamin D 0.1mcg 0% Calcium 40mg 4%
    Iron 1.6mg 8% Potassium 180mg 4%
    """

    func testReadsTheExtraNutrientRows() {
        let extras = NutritionLabelParser.parse(fullLabel).extras
        XCTAssertEqual(4.0, extras[.fiber]?.value)
        XCTAssertEqual(11.0, extras[.addedSugars]?.value)
        XCTAssertEqual(0.5, extras[.saturatedFat]?.value)
        XCTAssertEqual(5.0, extras[.cholesterol]?.value)
        XCTAssertEqual(290.0, extras[.sodium]?.value)
    }

    func testReadsEachNutrientWhenSeveralShareALine() {
        let extras = NutritionLabelParser.parse(fullLabel).extras
        XCTAssertEqual(0.1, extras[.vitaminD]?.value)
        XCTAssertEqual(40.0, extras[.calcium]?.value)
        XCTAssertEqual(1.6, extras[.iron]?.value)
        XCTAssertEqual(180.0, extras[.potassium]?.value)
    }

    func testExtraRowsDoNotDisturbTheMacros() {
        let result = NutritionLabelParser.parse(fullLabel)
        XCTAssertEqual(3.5, result.fat?.value)
        XCTAssertEqual(40.0, result.carbs?.value)
        XCTAssertEqual(10.0, result.protein?.value)
        XCTAssertEqual(220.0, result.calories?.value)
    }

    func testMissingRowsAreLeftOutRatherThanZero() {
        XCTAssertTrue(NutritionLabelParser.parse("Calories 100\nProtein 3g").extras.isEmpty)
    }

    func testReadsALeadingLetterOAsZeroInDecimals() {
        XCTAssertEqual(0.5, NutritionLabelParser.parse("Saturated Fat O.5g 4%").extras[.saturatedFat]?.value)
    }

    func testConvertsAValuePrintedInTheOtherMassUnit() {
        let extras = NutritionLabelParser.parse("Sodium 0.29g").extras
        XCTAssertEqual(290.0, extras[.sodium]?.value)
        XCTAssertEqual("mg", extras[.sodium]?.unit)
    }

    func testIgnoresNutrientWordsInsideTheIngredientsList() {
        let text = """
        Ingredients: whole grain oats, sugar, calcium carbonate, salt, reduced iron.
        Calcium 40mg 4%
        Iron 1.6mg 8%
        """
        let extras = NutritionLabelParser.parse(text).extras
        XCTAssertEqual(40.0, extras[.calcium]?.value)
        XCTAssertEqual(1.6, extras[.iron]?.value)
    }

    func testAPercentDailyValueIsNeverReadAsAnAmount() {
        XCTAssertNil(NutritionLabelParser.parse("Incl 1fg Added Sugars 22%").extras[.addedSugars])
    }

    func testPolyunsaturatedFatIsNotMistakenForSaturatedFat() {
        let text = "Polyunsaturated Fat 1g\nMonounsaturatedFat 1g\nSaturated Fat 0.5g"
        XCTAssertEqual(0.5, NutritionLabelParser.parse(text).extras[.saturatedFat]?.value)
    }

    func testParsesTheTextActuallyRecognizedFromAPhotographedOatmealCup() {
        // Verbatim ML Kit output for a Quaker protein oatmeal cup, misreads included.
        let text = """
        Nutrition Factse
        1 serving per container
        Serving size 1 package (60g|s
        Amount per serving
        Calories 220
        Total Fat 3.5g
        Saturated Fat 0.5g
        Trans Fat 0g
        Polyunsaturated Fat 1g
        MonounsaturatedFat 1g
        Cholesterol 5mg
        AWT Sodium 290mg
        Total Carb. 40g
        Dietary Fiber 4g
        % Daily Value
        FEANOR STWEN
        4%
        29%
        12%
        15%
        14%
        Total Sugars 12g
        Incl 1fg Added Sugars 22%
        Protein 10g
        17%
        """
        let result = NutritionLabelParser.parse(text)
        XCTAssertEqual(60.0, result.servingSize?.grams)
        XCTAssertEqual(220.0, result.calories?.value)
        XCTAssertEqual(3.5, result.fat?.value)
        XCTAssertEqual(40.0, result.carbs?.value)
        XCTAssertEqual(10.0, result.protein?.value)
        XCTAssertEqual(4.0, result.extras[.fiber]?.value)
        XCTAssertEqual(0.5, result.extras[.saturatedFat]?.value)
        XCTAssertEqual(5.0, result.extras[.cholesterol]?.value)
        XCTAssertEqual(290.0, result.extras[.sodium]?.value)
        XCTAssertNil(result.extras[.addedSugars])
        XCTAssertNil(result.extras[.potassium])
    }

    func testReadsANutrientTheRecognizerGluedOntoAnotherWord() {
        XCTAssertEqual(10.0, NutritionLabelParser.parse("DALORIESProtein 10g").protein?.value)
    }

    func testReadsServingGramsFollowedByRecognizerJunk() {
        XCTAssertEqual(60.0, NutritionLabelParser.parse("Serving size 1 package (60ges").servingSize?.grams)
    }

    func testParsesASecondRealScanOfTheOatmealCup() {
        // Verbatim ML Kit output from the phone, 2026-09-18 22:49.
        let text = """
        WEREY
        Helps
        SATISFY
        0 NET WI
        Nutrition Facts
        1 serving per container
        Serving size 1 package (60ges
        Amount per serving
        Calories 220:
        Total Fat 3.5g
        Saturated Fat 0.5g
        Trans Fat Og
        Polyunsaturated Fat 1g
        Monounsaturated Fat 1g
        % Daily Value AOR, SOY LETAN
        Cholesterol 5mg
        Sodium 290mg
        Total Carb. 40g
        Dietary Fiber 4g
        Total Sugars 12g
        Incl. 11g Added Sugars
        DALORIESProtein 10g
        CONTAINS MILK NNST
        2%
        12%
        INGREDEIS: MWME RINV
        15%
        14%
        22% We're har ibh
        17% Please have pacag a
        """
        let result = NutritionLabelParser.parse(text)
        XCTAssertEqual(60.0, result.servingSize?.grams)
        XCTAssertEqual(220.0, result.calories?.value)
        XCTAssertEqual(10.0, result.protein?.value)
        XCTAssertEqual(40.0, result.carbs?.value)
        XCTAssertEqual(3.5, result.fat?.value)
        XCTAssertEqual(0.5, result.extras[.saturatedFat]?.value)
        XCTAssertEqual(11.0, result.extras[.addedSugars]?.value)
        XCTAssertEqual(4.0, result.extras[.fiber]?.value)
        XCTAssertEqual(290.0, result.extras[.sodium]?.value)
    }
}
```

- [ ] **Step 2: `FrontLabelNameTests.swift`** (FrontLabelNameTest.kt)

```swift
import XCTest
@testable import LabelScanKit

final class FrontLabelNameTests: XCTestCase {
    // Real ML Kit output for the front of a Quaker protein oatmeal cup, in the order reported.
    private let quakerFront = [
        OcrLine(text: "QUAKER", height: 213, top: 1774),
        OcrLine(text: "-ESTP 1877-", height: 58, top: 1984),
        OcrLine(text: "PROTEM", height: 40, top: 1468),
        OcrLine(text: "per serving", height: 38, top: 1520),
        OcrLine(text: "INSTANT OATMEAL", height: 107, top: 2095),
        OcrLine(text: "PROTEIN", height: 160, top: 2235),
        OcrLine(text: "MAPLE &", height: 113, top: 2451),
        OcrLine(text: "BROWN SUGAR", height: 131, top: 2588),
        OcrLine(text: "KANOR WITH OTHER NATURAL FLAVORN", height: 88, top: 2756)
    ]

    func testBuildsTheNameFromTheProminentLinesOfThePackageFront() {
        XCTAssertEqual("Quaker Instant Oatmeal Protein Maple & Brown Sugar", NutritionLabelParser.extractProductName(quakerFront))
    }

    func testReadsTheProminentLinesTopToBottomWhateverOrderTheyArriveIn() {
        XCTAssertEqual("Quaker Instant Oatmeal Protein Maple & Brown Sugar", NutritionLabelParser.extractProductName(Array(quakerFront.reversed())))
    }

    func testLeavesOutLargeSmallPrintPhrases() {
        let lines = [
            OcrLine(text: "CHEERIOS", height: 200, top: 100),
            OcrLine(text: "NET WT 8.9 OZ", height: 150, top: 900),
            OcrLine(text: "NATURALLY FLAVORED", height: 140, top: 500),
            OcrLine(text: "Honey Nut", height: 130, top: 300)
        ]
        XCTAssertEqual("Cheerios Honey Nut", NutritionLabelParser.extractProductName(lines))
    }

    func testANutritionPanelPhotographedAsTheFrontGivesNoName() {
        let lines = [OcrLine(text: "Nutrition Facts", height: 120, top: 10), OcrLine(text: "Serving size 1 package (60g)", height: 40, top: 140)]
        XCTAssertNil(NutritionLabelParser.extractProductName(lines))
    }

    func testFallsBackToTheFirstLineWhenSizesAreUnknown() {
        let lines = [OcrLine(text: "Quaker Oatmeal", height: 0, top: 0), OcrLine(text: "Maple Brown Sugar", height: 0, top: 0)]
        XCTAssertEqual("Quaker Oatmeal", NutritionLabelParser.extractProductName(lines))
    }

    func testNothingReadableGivesNoName() {
        XCTAssertNil(NutritionLabelParser.extractProductName([OcrLine]()))
    }
}
```

- [ ] **Step 3: `ProductNames.swift`** (ProductNames.kt)

```swift
import Foundation

enum ProductNames {
    /// Re-cases shouty text ("QUAKER" -> "Quaker"); names already in mixed case are left alone.
    static func readable(_ text: String) -> String {
        if text.contains(where: { $0.isLowercase }) { return text }
        var result = ""
        var previous: Character = " "
        for c in text {
            // A letter starts a word unless it follows a letter or an apostrophe (KELLOGG'S, M&M'S).
            let startsWord = !previous.isLetter && previous != "'" && previous != "\u{2019}"
            result += startsWord ? c.uppercased() : c.lowercased()
            previous = c
        }
        return result
    }
}
```

- [ ] **Step 4: `NutritionLabelParser.swift`** (NutritionLabelParser.kt, line for line)

```swift
import Foundation

public enum NutritionLabelParser {

    // On a real Quaker cup the name lines are 50-100% of the tallest line and the slanted
    // "flavor with other natural flavors" line is 41%.
    private static let prominentLineRatio = 0.45
    private static let maxNameLines = 6
    private static let smallPrint = [
        "net wt", "per serving", "servings per", "natural flavor", "naturally flavored",
        "artificial flavor", "artificially flavored", "with other"
    ]

    public static func parse(_ text: String) -> ParsedLabel {
        let rawLines = text.components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
        let normalized = rawLines.map(normalizeLine).joined(separator: "\n")

        var extras: [ExtraNutrient: ValueWithConfidence] = [:]
        for extra in ExtraNutrient.allCases {
            if let v = extractValue(normalized, keywords: extra.keywords, unit: extra.unit) { extras[extra] = v }
        }
        return ParsedLabel(
            productName: nil,
            servingSize: extractServingSize(normalized),
            // US labels print calories with no unit, so a bare number is not a weak read.
            calories: extractValue(normalized, keywords: ["calories"], unit: "kcal")?.with(confidence: .high),
            protein: extractValue(normalized, keywords: ["protein"], unit: "g"),
            // "total carb" also covers "Total Carb.", "Total Carbs" and "Total Carbohydrate".
            carbs: extractValue(normalized, keywords: ["total carb", "carbohydrate"], unit: "g"),
            fat: extractValue(normalized, keywords: ["total fat", "total lipid"], unit: "g"),
            extras: extras,
            rawLines: rawLines
        )
    }

    /// True when the text is a Nutrition Facts panel rather than the front of a package.
    public static func looksLikeNutritionPanel(_ text: String) -> Bool {
        let lower = text.lowercased()
        return lower.contains("nutrition facts") || lower.contains("serving size") ||
            (lower.contains("calories") && lower.contains("total fat"))
    }

    public static func extractProductName(_ frontText: String) -> String? {
        extractProductName(frontText.components(separatedBy: .newlines)
            .filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
            .map { OcrLine(text: $0.trimmingCharacters(in: .whitespaces), height: 0, top: 0) })
    }

    /// Names a product from a photo of the front of its package. The brand, product and flavour are
    /// printed large; taglines, weights and claims are small, so keep the prominent lines only.
    public static func extractProductName(_ frontLines: [OcrLine]) -> String? {
        if looksLikeNutritionPanel(frontLines.map { $0.text }.joined(separator: "\n")) { return nil }
        let lines = frontLines.filter { line in line.text.filter { $0.isLetter }.count >= 2 }
        guard !lines.isEmpty else { return nil }

        let tallest = lines.map { $0.height }.max() ?? 0
        if tallest <= 0 { return lines[0].text.trimmingCharacters(in: .whitespaces) }

        let name = lines
            .filter { Double($0.height) >= Double(tallest) * prominentLineRatio }
            .filter { line in !smallPrint.contains { line.text.lowercased().contains($0) } }
            .sorted { $0.top < $1.top }
            .prefix(maxNameLines)
            .map { ProductNames.readable($0.text.trimmingCharacters(in: .whitespaces)) }
            .joined(separator: " ")
        return name.trimmingCharacters(in: .whitespaces).isEmpty ? nil : name
    }

    static func normalizeLine(_ line: String) -> String {
        var chars = Array(line)
        let lastIndex = chars.count - 1
        for i in chars.indices {
            switch chars[i].lowercased() {
            case "o":
                let afterDigit = i > 0 && chars[i - 1].isNumber
                // "O.5g": a lone O in front of a decimal point is a zero.
                let leadsDecimal = i + 2 <= lastIndex && chars[i + 1] == "." && chars[i + 2].isNumber &&
                    (i == 0 || !chars[i - 1].isLetter)
                if afterDigit || leadsDecimal { chars[i] = "0" }
            case "l":
                if i < lastIndex && chars[i + 1].isNumber { chars[i] = "1" }
            case "s":
                if i > 0 && chars[i - 1].isNumber { chars[i] = "5" }
            case "1":
                // Treat a lone 1 inside a word as 'i' (e.g. Prote1n) and a 1 at
                // the end of a word as 'l' (e.g. Tota1).
                let prevLetter = i > 0 && chars[i - 1].isLetter
                let nextLetter = i < lastIndex && chars[i + 1].isLetter
                let atWordEnd = i == lastIndex || chars[i + 1].isWhitespace
                if prevLetter && nextLetter {
                    chars[i] = "i"
                } else if prevLetter && atWordEnd {
                    chars[i] = "l"
                }
            default:
                break
            }
        }
        return Regex.replaceAll("\\s+", in: String(chars), with: " ").lowercased()
    }

    private static func extractServingSize(_ text: String) -> ServingSize? {
        guard let m = Regex.firstMatch("serving size\\s+(.+?)(?=\\n|$)", in: text, caseInsensitive: true) else { return nil }
        let description = m[1].trimmingCharacters(in: .whitespaces)
        // OCR often misreads what follows the grams ("(60g|", "(60ges"). An opening paren is enough
        // to trust the number; otherwise require that the g ends the token.
        let gramsText = Regex.firstMatch("\\(\\s*(\\d+(?:\\.\\d+)?)\\s*g", in: description, caseInsensitive: true)?[1]
            ?? Regex.firstMatch("(\\d+(?:\\.\\d+)?)\\s*g(?![a-z])", in: description, caseInsensitive: true)?[1]
        return ServingSize(description: description, grams: gramsText.flatMap { Double($0) })
    }

    private static func extractValue(_ text: String, keywords: [String], unit: String) -> ValueWithConfidence? {
        let lines = text.components(separatedBy: "\n")
        // Prefer the keyword at the start of a word. The recognizer sometimes glues a row onto the
        // previous word ("DALORIESProtein 10g"), so fall back to a match inside a word, but never
        // after "un": "Polyunsaturated Fat" is not "Saturated Fat".
        for prefix in ["(?<![a-z])", "(?<!un)"] {
            for keyword in keywords {
                let pattern = prefix + Regex.escape(keyword)
                // The ingredients list can mention a nutrient ("calcium carbonate") before its row
                // does, so keep looking until a line actually carries an amount.
                for line in lines {
                    guard let range = Regex.firstRange(pattern, in: line, caseInsensitive: true) else { continue }
                    let afterKeyword = String(line[range.upperBound...])
                    if let result = parseNumericValue(line: line, afterKeyword: afterKeyword, unit: unit) {
                        return result
                    }
                }
            }
        }
        return nil
    }

    // Grams per unit, so a value printed in one mass unit can be stored in another.
    private static let massUnits: [String: Double] = ["mcg": 1e-6, "\u{00b5}g": 1e-6, "ug": 1e-6, "mg": 1e-3, "g": 1.0]
    private static let massUnitAlternation = ["mcg", "\u{00b5}g", "ug", "mg", "g"].joined(separator: "|")

    private static func parseNumericValue(line: String, afterKeyword: String, unit: String) -> ValueWithConfidence? {
        let units = massUnits[unit] != nil ? massUnitAlternation : unit
        let unitPattern = "(\\d+(?:\\.\\d+)?)\\s*(\(units))(?![a-z])"

        // Vitamins and minerals often share a line ("Vitamin D 0.1mcg 0% Calcium 40mg 4%"), so
        // prefer the amount right after the keyword. Some rows put it first ("Incl. 11g Added
        // Sugars"), so fall back to the whole line.
        if let m = Regex.firstMatch(unitPattern, in: afterKeyword, caseInsensitive: true)
            ?? Regex.firstMatch(unitPattern, in: line, caseInsensitive: true) {
            guard let value = Double(m[1]) else { return nil }
            let detectedUnit = m[2].lowercased()
            let factor = (massUnits[detectedUnit] ?? 1.0) / (massUnits[unit] ?? 1.0)
            return ValueWithConfidence(value: (value * factor * 1000).rounded() / 1000.0, unit: unit, confidence: .high)
        }

        // Fallback: a bare number after the keyword. One followed by % is a Daily Value, not an amount.
        if let m = Regex.firstMatch("(?<![\\d.])(\\d+(?:\\.\\d+)?)(?![\\d.]|\\s*%)", in: afterKeyword) {
            guard let value = Double(m[1]) else { return nil }
            return ValueWithConfidence(value: value, unit: unit, confidence: .low)
        }
        return nil
    }
}
```

- [ ] **Step 5: Run** `swift test --package-path COPDFuel/LabelScanKit 2>&1 | tail -5` → `Executed 33 tests, with 0 failures`. If a parser case fails, compare against the Kotlin line named in the comment above it; do not change the test.

---

### Task 3: `GTIN`, `ProductLinkResolver`, `USDAGtinLookup` (+ tests)

**Files:**
- Create: `COPDFuel/LabelScanKit/Sources/LabelScanKit/GTIN.swift`
- Create: `COPDFuel/LabelScanKit/Sources/LabelScanKit/ProductLinkResolver.swift`
- Create: `COPDFuel/LabelScanKit/Sources/LabelScanKit/USDAGtinLookup.swift`
- Create: `COPDFuel/LabelScanKit/Tests/LabelScanKitTests/GTINTests.swift`, `ProductLinkResolverTests.swift`, `USDAGtinLookupTests.swift`

**Interfaces (produced):**
- `GTIN.extractGtin(_ value: String?) -> String?`, `GTIN.hasValidCheckDigit(_:) -> Bool`.
- `ProductLinkResolver.Hop(status:location:body:)`, `ProductLinkResolver(fetch:)` (`typealias Fetch = (String) async throws -> Hop`, default `ProductLinkResolver.httpFetch`), `resolveGtin(_ link: String) async throws -> String?`, `static findGtinInHtml(_:) -> String?`, `static isWebLink(_:) -> Bool`.
- `USDAGtinLookup(fetch:)` (`(URL) async throws -> String`, default URLSession), `lookup(gtin:apiKey:) async throws -> ParsedLabel?`, `mapResponse(_ json: String, expectedGtin: String? = nil) -> ParsedLabel?`, `static gtinCandidates(_:) -> [String]`, `static sameGtin(_:_:)`, `static productName(_ description: String, _ brandName: String?) -> String`.

- [ ] **Step 1: `GTINTests.swift`** (MlKitBarcodeScannerTest.kt)

```swift
import XCTest
@testable import LabelScanKit

final class GTINTests: XCTestCase {
    func testExtractsGTINFromGS1DigitalLink() {
        XCTAssertEqual("014200000036", GTIN.extractGtin("https://id.gs1.org/gtin/014200000036"))
    }
    func testExtractsGTINFromRawUPC() {
        XCTAssertEqual("036000291452", GTIN.extractGtin("036000291452"))
    }
    func testExtractsGTINEmbeddedInAProductPageLink() {
        // Real SmartLabel address for Quaker protein oatmeal
        XCTAssertEqual("030000570630", GTIN.extractGtin("https://smartlabel.pepsico.info/030000570630-0001-en-US/index.html"))
    }
    func testIgnoresLongNumbersInLinksThatAreNotValidGTINs() {
        XCTAssertNil(GTIN.extractGtin("https://example.com/order/123456789013"))
    }
    func testReturnsNilForNonProductQR() {
        XCTAssertNil(GTIN.extractGtin("https://example.com/coupon"))
    }
}
```

- [ ] **Step 2: `ProductLinkResolverTests.swift`** (ProductLinkResolverTest.kt)

```swift
import XCTest
@testable import LabelScanKit

final class ProductLinkResolverTests: XCTestCase {
    func testFindsTheUPCPrintedOnASmartLabelPage() {
        XCTAssertEqual("030000570630", ProductLinkResolver.findGtinInHtml(#"<div><span class="Upc"> UPC </span> 030000570630</div>"#))
    }
    func testFindsASchemaOrgGtinInPageMarkup() {
        XCTAssertEqual("0030000570630", ProductLinkResolver.findGtinInHtml(#"<script type="application/ld+json">{"@type":"Product","gtin13":"0030000570630"}</script>"#))
    }
    func testIgnoresNumbersThatAreNotLabelledAsAProductCode() {
        XCTAssertNil(ProductLinkResolver.findGtinInHtml("<p>Call 1-800-367-6287 or order 030000570630 today</p>"))
    }
    func testIgnoresLabelledNumbersWithABadCheckDigit() {
        XCTAssertNil(ProductLinkResolver.findGtinInHtml("<span>UPC</span> 030000570631"))
    }
    func testOnlyWebLinksAreResolvable() {
        XCTAssertTrue(ProductLinkResolver.isWebLink("https://qrs.ly/abc123"))
        XCTAssertTrue(ProductLinkResolver.isWebLink("HTTP://scn.by/x"))
        XCTAssertFalse(ProductLinkResolver.isWebLink("WIFI:S:home;T:WPA;P:secret;;"))
        XCTAssertFalse(ProductLinkResolver.isWebLink(nil))
    }

    // The real redirect chain behind the QR on a Quaker oatmeal cup (http://pepsico.info/490lz7).
    private let quakerChain: [String: ProductLinkResolver.Hop] = [
        "http://pepsico.info/490lz7": .init(status: 301, location: "https://pepsi.scb.ai/490lz7", body: nil),
        "https://pepsi.scb.ai/490lz7": .init(status: 302, location: "http://app.scanlife.com/resolver/shorturl/490lz7", body: nil),
        "https://app.scanlife.com/resolver/shorturl/490lz7": .init(status: 302, location: "http://app.scanlife.com/resolver/shorturl/490lz7?proxy=false", body: nil),
        "https://app.scanlife.com/resolver/shorturl/490lz7?proxy=false": .init(status: 302, location: "http://app.scanlife.com/resolver/dw/490lz7?proxy=false", body: nil),
        "https://app.scanlife.com/resolver/dw/490lz7?proxy=false": .init(status: 302, location: "http://app.scanlife.com/resolver/codeexec?barcode=f51a843&rd=1", body: nil),
        "https://app.scanlife.com/resolver/codeexec?barcode=f51a843&rd=1": .init(status: 302, location: "https://menu.myproduct.info/5125a12d/index.html?cname=00030000570630_32865706304_BEM_Quaker_BR&scantime=2026-09-19T05%3A09%3A09Z", body: nil)
    ]

    private final class Recorder { var requested: [String] = [] }

    private func fakeFetch(_ recorder: Recorder) -> ProductLinkResolver.Fetch {
        { url in
            recorder.requested.append(url)
            guard let hop = self.quakerChain[url] else { throw URLError(.cannotFindHost) }
            return hop
        }
    }

    func testFollowsTheRealSevenHopSmartLabelChainToTheUPC() async throws {
        let recorder = Recorder()
        let gtin = try await ProductLinkResolver(fetch: fakeFetch(recorder)).resolveGtin("http://pepsico.info/490lz7")
        XCTAssertEqual("00030000570630", gtin)
    }

    func testFallsBackToPlainHttpWhenTheHttpsVersionOfALinkFails() async throws {
        let recorder = Recorder()
        _ = try await ProductLinkResolver(fetch: fakeFetch(recorder)).resolveGtin("http://pepsico.info/490lz7")
        // pepsico.info has an expired certificate, so https is tried first and http second.
        XCTAssertEqual(["https://pepsico.info/490lz7", "http://pepsico.info/490lz7"], Array(recorder.requested.prefix(2)))
        // app.scanlife.com is linked over http but works over https, so it is never fetched in the clear.
        XCTAssertTrue(recorder.requested.allSatisfy { !$0.hasPrefix("http://app.scanlife.com") })
    }
}
```

- [ ] **Step 3: `USDAGtinLookupTests.swift`** (UsdaGtinLookupTest.kt)

```swift
import XCTest
@testable import LabelScanKit

final class USDAGtinLookupTests: XCTestCase {
    // Real FoodData Central search result for Quaker protein oatmeal (label: 220 kcal per 60 g cup).
    // The search endpoint reports branded nutrients per 100 g, not per serving.
    private func quakerJson(unit: String = "g", gtinUpc: String = "030000570630") -> String {
        """
        {
          "foods": [{
            "description": "MAPLE & BROWN SUGAR PROTEIN INSTANT OATMEAL, MAPLE & BROWN SUGAR",
            "brandName": "QUAKER",
            "gtinUpc": "\(gtinUpc)",
            "servingSize": 60.0,
            "servingSizeUnit": "\(unit)",
            "foodNutrients": [
              {"nutrientId": 1008, "value": 367},
              {"nutrientId": 1003, "value": 16.7},
              {"nutrientId": 1005, "value": 66.7},
              {"nutrientId": 1004, "value": 5.83},
              {"nutrientId": 1079, "value": 6.7},
              {"nutrientId": 1087, "value": 67.0},
              {"nutrientId": 1089, "value": 2.67},
              {"nutrientId": 1092, "value": 300},
              {"nutrientId": 1093, "value": 483},
              {"nutrientId": 1110, "value": 7.0, "unitName": "IU"},
              {"nutrientId": 1235, "value": 18.3},
              {"nutrientId": 1253, "value": 8.0},
              {"nutrientId": 1258, "value": 0.83},
              {"nutrientId": 2000, "value": 20.0}
            ]
          }]
        }
        """
    }

    func testScalesPer100gUSDANutrientsToOneServing() {
        let result = USDAGtinLookup().mapResponse(quakerJson())
        XCTAssertEqual("Quaker Maple & Brown Sugar Protein Instant Oatmeal", result?.productName)
        XCTAssertEqual(220.2, result?.calories?.value)
        XCTAssertEqual(10.0, result?.protein?.value)
        XCTAssertEqual(40.0, result?.carbs?.value)
        XCTAssertEqual(3.5, result?.fat?.value)
        XCTAssertEqual(60.0, result?.servingSize?.grams)
    }

    func testTreatsGRMServingUnitAsGrams() {
        let result = USDAGtinLookup().mapResponse(quakerJson(unit: "GRM"))
        XCTAssertEqual(60.0, result?.servingSize?.grams)
        XCTAssertEqual(220.2, result?.calories?.value)
    }

    func testAcceptsAHitWhoseGtinDiffersOnlyByLeadingZeros() {
        XCTAssertNotNil(USDAGtinLookup().mapResponse(quakerJson(gtinUpc: "00030000570630"), expectedGtin: "030000570630"))
    }

    func testRejectsAHitForADifferentProduct() {
        XCTAssertNil(USDAGtinLookup().mapResponse(quakerJson(gtinUpc: "072036708748"), expectedGtin: "030000570630"))
    }

    func testTries12_13And14DigitFormsOfAScannedCode() {
        let candidates = USDAGtinLookup.gtinCandidates("0030000570630")
        XCTAssertEqual("0030000570630", candidates.first)
        XCTAssertTrue(candidates.contains("030000570630"))
        XCTAssertTrue(candidates.contains("00030000570630"))
    }

    func testMapsTheExtraNutrientsPerServing() {
        let extras = USDAGtinLookup().mapResponse(quakerJson())!.extras
        XCTAssertEqual(4.0, extras[.fiber]?.value)
        XCTAssertEqual(11.0, extras[.addedSugars]?.value)
        XCTAssertEqual(0.5, extras[.saturatedFat]?.value)
        XCTAssertEqual(4.8, extras[.cholesterol]?.value)
        XCTAssertEqual(289.8, extras[.sodium]?.value)
        XCTAssertEqual(180.0, extras[.potassium]?.value)
        XCTAssertEqual(40.2, extras[.calcium]?.value)
        XCTAssertEqual(1.6, extras[.iron]?.value)
    }

    func testConvertsVitaminDFromIUToMicrograms() {
        // 7 IU per 100 g -> 4.2 IU per 60 g serving -> 0.105 mcg
        let vitaminD = USDAGtinLookup().mapResponse(quakerJson())!.extras[.vitaminD]
        XCTAssertEqual(0.1, vitaminD?.value)
        XCTAssertEqual("mcg", vitaminD?.unit)
    }

    func testLeavesOutNutrientsUSDADoesNotReport() {
        let json = #"{"foods":[{"description":"Plain","servingSize":100.0,"servingSizeUnit":"g","foodNutrients":[{"nutrientId":1008,"value":100}]}]}"#
        XCTAssertTrue(USDAGtinLookup().mapResponse(json)!.extras.isEmpty)
    }

    func testDropsAFlavourThatOnlyRepeatsTheDescription() {
        XCTAssertEqual("Harris Teeter Maple & Brown Sugar Instant Oatmeal Cups",
                       USDAGtinLookup.productName("MAPLE & BROWN SUGAR INSTANT OATMEAL CUPS, MAPLE & BROWN SUGAR", "HARRIS TEETER"))
    }

    func testKeepsASecondPartThatAddsInformation() {
        XCTAssertEqual("Quaker Instant Oatmeal, Apples & Cinnamon",
                       USDAGtinLookup.productName("INSTANT OATMEAL, APPLES & CINNAMON", "QUAKER"))
    }

    func testDoesNotRepeatABrandTheDescriptionAlreadyNames() {
        XCTAssertEqual("Quaker Instant Oatmeal Maple & Brown Sugar 1.69 Oz",
                       USDAGtinLookup.productName("Quaker Instant Oatmeal Maple & Brown Sugar 1.69 Oz", "Quaker"))
    }

    func testCapitalisesPossessivesAndJoinedNamesSensibly() {
        XCTAssertEqual("Kellogg's Frosted Flakes", USDAGtinLookup.productName("FROSTED FLAKES", "KELLOGG'S"))
        XCTAssertEqual("M&M's Peanut Candies", USDAGtinLookup.productName("PEANUT CANDIES", "M&M'S"))
    }

    func testWorksWithoutABrand() {
        XCTAssertEqual("Plain Rolled Oats", USDAGtinLookup.productName("PLAIN ROLLED OATS", nil))
        XCTAssertEqual("Scanned food", USDAGtinLookup.productName("", ""))
    }
}
```

- [ ] **Step 4: `GTIN.swift`** (MlKitBarcodeScanner.kt companion)

```swift
import Foundation

public enum GTIN {
    public static func extractGtin(_ value: String?) -> String? {
        guard let value, !value.trimmingCharacters(in: .whitespaces).isEmpty else { return nil }

        // GS1 Digital Link: https://id.gs1.org/gtin/014200000036
        if let m = Regex.firstMatch("https?://[^/]+/gtin/(\\d+)", in: value, caseInsensitive: true) { return m[1] }

        // Raw numeric GTIN/UPC/EAN (8, 12, 13, or 14 digits), as read from a striped barcode
        let trimmed = value.trimmingCharacters(in: .whitespaces)
        if trimmed.allSatisfy({ $0.isNumber }) {
            return [8, 12, 13, 14].contains(trimmed.count) ? trimmed : nil
        }

        // Product page links (e.g. SmartLabel) often embed the GTIN. Only trust a digit run
        // whose GS1 check digit is valid, so order numbers and dates are not mistaken for one.
        let candidates = Regex.allMatches("(?<!\\d)\\d{12,14}(?!\\d)", in: trimmed).map { $0[0] }.filter(hasValidCheckDigit)
        return candidates.max { $0.count < $1.count }   // longest; first on ties (max keeps the first maximal element)
    }

    public static func hasValidCheckDigit(_ code: String) -> Bool {
        guard code.count >= 8, code.allSatisfy({ $0.isNumber }) else { return false }
        let digits = code.compactMap { $0.wholeNumberValue }
        let sum = digits.dropLast().reversed().enumerated().reduce(0) { acc, pair in
            acc + (pair.offset % 2 == 0 ? pair.element * 3 : pair.element)
        }
        return (10 - sum % 10) % 10 == digits.last
    }
}
```
Note on ties: Swift's `max(by:)` returns the last maximal element when several compare equal, but Kotlin `maxByOrNull` returns the first. Implement explicitly instead of `max(by:)`:
```swift
        var best: String? = nil
        for c in candidates where best == nil || c.count > best!.count { best = c }
        return best
```

- [ ] **Step 5: `ProductLinkResolver.swift`** (ProductLinkResolver.kt)

```swift
import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

/// Package QR codes (e.g. SmartLabel) usually hold a short link that redirects to a product page.
/// Follows that link and returns the product's GTIN/UPC from the final address or the page itself.
public struct ProductLinkResolver {
    /// One HTTP response: a redirect carries `location`, a page carries `body`.
    public struct Hop {
        public let status: Int
        public let location: String?
        public let body: String?
        public init(status: Int, location: String?, body: String?) {
            self.status = status; self.location = location; self.body = body
        }
    }

    public typealias Fetch = (String) async throws -> Hop

    // The QR on a Quaker cup takes 7 redirects to reach its product page.
    private static let maxHops = 10
    private static let timeoutSeconds: TimeInterval = 8
    private static let maxPageChars = 200_000
    private static let userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private let fetch: Fetch

    public init(fetch: @escaping Fetch = ProductLinkResolver.httpFetch) { self.fetch = fetch }

    public func resolveGtin(_ link: String) async throws -> String? {
        var current = link.trimmingCharacters(in: .whitespaces)
        for _ in 0..<Self.maxHops {
            if let gtin = GTIN.extractGtin(current) { return gtin }
            let hop = try await fetchPreferringHttps(current)
            if (300...399).contains(hop.status), let location = hop.location {
                current = URL(string: location, relativeTo: URL(string: current))?.absoluteString ?? location
            } else if hop.status == 200 {
                return Self.findGtinInHtml(hop.body ?? "")
            } else {
                return nil
            }
        }
        return GTIN.extractGtin(current)
    }

    // Short-link services still hand out http:// addresses. Most also answer over https, so try
    // that first; plain http only succeeds for hosts allowed by the ATS exception (pepsico.info).
    private func fetchPreferringHttps(_ url: String) async throws -> Hop {
        guard url.lowercased().hasPrefix("http://") else { return try await fetch(url) }
        do {
            return try await fetch("https" + url.dropFirst(4))
        } catch {
            return try await fetch(url)
        }
    }

    // A number only counts when the page labels it as a product code and its check digit is valid.
    private static let labelledCode = "(?i)(?:gtin\\d{0,2}|upc)(?:<[^>]*>|[^0-9<]){0,40}?(\\d{12,14})(?!\\d)"

    public static func findGtinInHtml(_ html: String) -> String? {
        Regex.allMatches(labelledCode, in: html).map { $0[1] }.first(where: GTIN.hasValidCheckDigit)
    }

    public static func isWebLink(_ value: String?) -> Bool {
        guard let value else { return false }
        return Regex.matches("https?://\\S+", value.trimmingCharacters(in: .whitespaces), caseInsensitive: true)
    }

    // MARK: Default fetch: no automatic redirects, 8 s timeout, Android UA, body capped.

    private final class NoRedirectDelegate: NSObject, URLSessionTaskDelegate {
        func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse,
                        newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) {
            completionHandler(nil)
        }
    }

    private static let session: URLSession = {
        let c = URLSessionConfiguration.ephemeral
        c.timeoutIntervalForRequest = timeoutSeconds
        c.timeoutIntervalForResource = timeoutSeconds
        return URLSession(configuration: c, delegate: NoRedirectDelegate(), delegateQueue: nil)
    }()

    public static func httpFetch(_ url: String) async throws -> Hop {
        guard let u = URL(string: url) else { throw URLError(.badURL) }
        var request = URLRequest(url: u)
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw URLError(.badServerResponse) }
        let location = http.value(forHTTPHeaderField: "Location")
        var body: String? = nil
        if http.statusCode == 200 {
            let text = String(decoding: data, as: UTF8.self)
            body = text.count > maxPageChars ? String(text.prefix(maxPageChars)) : text
        }
        return Hop(status: http.statusCode, location: location, body: body)
    }
}
```

- [ ] **Step 6: `USDAGtinLookup.swift`** (UsdaGtinLookup.kt)

```swift
import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

public struct USDAGtinLookup {
    public typealias Fetch = (URL) async throws -> String
    private let fetch: Fetch

    public init(fetch: @escaping Fetch = { url in
        let (data, _) = try await URLSession.shared.data(from: url)
        return String(decoding: data, as: UTF8.self)
    }) {
        self.fetch = fetch
    }

    public func lookup(gtin: String, apiKey: String) async throws -> ParsedLabel? {
        // FoodData Central stores some codes as 12 digits and others zero-padded to 14, and a
        // search only matches the exact stored form, so try each form of the scanned code.
        for candidate in Self.gtinCandidates(gtin) {
            let q = candidate.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? candidate
            guard let url = URL(string: "https://api.nal.usda.gov/fdc/v1/foods/search?query=\(q)&dataType=Branded&pageSize=1&api_key=\(apiKey)") else { continue }
            let response = try await fetch(url)
            if let parsed = mapResponse(response, expectedGtin: gtin) { return parsed }
        }
        return nil
    }

    public func mapResponse(_ response: String, expectedGtin: String? = nil) -> ParsedLabel? {
        guard let data = response.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let foods = json["foods"] as? [[String: Any]], let food = foods.first else { return nil }

        // The search is full-text, so make sure the hit really is the scanned product.
        if let expectedGtin, !Self.sameGtin(food["gtinUpc"] as? String ?? "", expectedGtin) { return nil }

        let description = Self.productName(food["description"] as? String ?? "", food["brandName"] as? String ?? "")
        let servingSize = (food["servingSize"] as? NSNumber)?.doubleValue ?? 100.0
        let servingUnit = food["servingSizeUnit"] as? String ?? "g"
        let isGrams = ["g", "grm", "gram", "grams"].contains(servingUnit.lowercased())
        let servingDesc = "\(Self.formatAmount(servingSize)) \(isGrams ? "g" : servingUnit.lowercased())"

        // Branded search results report nutrients per 100 g (or 100 ml), not per serving.
        let perServing = servingSize / 100.0

        var calories = 0.0, protein = 0.0, carbs = 0.0, fat = 0.0
        var per100gById: [Int: Double] = [:]
        for nutrient in food["foodNutrients"] as? [[String: Any]] ?? [] {
            let id = (nutrient["nutrientId"] as? NSNumber)?.intValue ?? 0
            let raw = (nutrient["value"] as? NSNumber)?.doubleValue ?? 0.0
            per100gById[id] = raw
            let value = Self.roundToTenth(raw * perServing)
            switch id {
            case 1008: calories = value
            case 1003: protein = value
            case 1005: carbs = value
            case 1004: fat = value
            default: break
            }
        }

        // Only nutrients USDA actually reports, so a missing value is not shown as zero.
        var extras: [ExtraNutrient: ValueWithConfidence] = [:]
        for extra in ExtraNutrient.allCases {
            guard let id = extra.usdaIds.first(where: { per100gById[$0] != nil }) else { continue }
            var amount = per100gById[id]! * perServing
            if id == ExtraNutrient.usdaVitaminDIU { amount *= ExtraNutrient.mcgPerIUVitaminD }
            extras[extra] = ValueWithConfidence(value: Self.roundToTenth(amount), unit: extra.unit, confidence: .high)
        }

        return ParsedLabel(
            productName: description,
            servingSize: ServingSize(description: servingDesc, grams: isGrams ? servingSize : nil),
            calories: ValueWithConfidence(value: calories, unit: "kcal", confidence: .high),
            protein: ValueWithConfidence(value: protein, unit: "g", confidence: .high),
            carbs: ValueWithConfidence(value: carbs, unit: "g", confidence: .high),
            fat: ValueWithConfidence(value: fat, unit: "g", confidence: .high),
            extras: extras
        )
    }

    private static func roundToTenth(_ value: Double) -> Double { (value * 10).rounded() / 10.0 }

    private static func formatAmount(_ value: Double) -> String {
        value == value.rounded(.towardZero) ? String(Int64(value)) : String(value)
    }

    /// USDA descriptions are upper case, often repeat the flavour ("X OATMEAL, MAPLE & BROWN
    /// SUGAR") and leave the brand in a separate field. Builds the name a person would expect.
    public static func productName(_ description: String, _ brandName: String?) -> String {
        let nonWord = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "_")).inverted
        var parts: [String] = []
        for part in description.split(separator: ",").map({ $0.trimmingCharacters(in: .whitespaces) }).filter({ !$0.isEmpty }) {
            let alreadySaid = Set(parts.joined(separator: " ").lowercased().components(separatedBy: nonWord))
            let words = part.lowercased().components(separatedBy: nonWord).filter { !$0.isEmpty }
            if parts.isEmpty || !words.allSatisfy({ alreadySaid.contains($0) }) { parts.append(part) }
        }
        let name = ProductNames.readable(parts.joined(separator: ", "))
        let brand = ProductNames.readable((brandName ?? "").trimmingCharacters(in: .whitespaces))
        if name.isEmpty && brand.isEmpty { return "Scanned food" }
        if brand.isEmpty || name.range(of: brand, options: .caseInsensitive) != nil { return name }
        if name.isEmpty { return brand }
        return "\(brand) \(name)"
    }

    /// The scanned code first, then its 12, 13 and 14 digit forms.
    public static func gtinCandidates(_ gtin: String) -> [String] {
        let core = String(gtin.drop(while: { $0 == "0" }))
        var out = [gtin]
        for length in [12, 13, 14] where length >= core.count {
            out.append(String(repeating: "0", count: length - core.count) + core)
        }
        var seen = Set<String>()
        return out.filter { seen.insert($0).inserted }
    }

    public static func sameGtin(_ a: String, _ b: String) -> Bool {
        !a.trimmingCharacters(in: .whitespaces).isEmpty &&
            a.drop(while: { $0 == "0" }) == b.drop(while: { $0 == "0" })
    }
}
```

- [ ] **Step 7: Run** `swift test --package-path COPDFuel/LabelScanKit 2>&1 | tail -5` → `Executed 57 tests, with 0 failures`.

---

### Task 4: Link the package + Info.plist keys

**Files:**
- Modify: `COPDFuel/COPDFuel.xcodeproj/project.pbxproj`
- Create: `COPDFuel/Info.plist` (repo root of the iOS repo, outside the synchronized `COPDFuel/COPDFuel/` folder)

- [ ] **Step 1: `Info.plist`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>NSCameraUsageDescription</key>
	<string>COPD Fuel uses the camera to scan nutrition labels and product barcodes.</string>
	<key>USDA_FDC_API_KEY</key>
	<string>$(USDA_FDC_API_KEY)</string>
	<key>NSAppTransportSecurity</key>
	<dict>
		<key>NSExceptionDomains</key>
		<dict>
			<key>pepsico.info</key>
			<dict>
				<key>NSExceptionAllowsInsecureHTTPLoads</key>
				<true/>
				<key>NSIncludesSubdomains</key>
				<true/>
			</dict>
		</dict>
	</dict>
</dict>
</plist>
```

- [ ] **Step 2: pbxproj build settings** — in both app-target configurations (the blocks containing `INFOPLIST_KEY_UIRequiresFullScreen = YES;`, lines ≈297 and ≈338) add:
```
				INFOPLIST_FILE = Info.plist;
				USDA_FDC_API_KEY = "";
```
(`GENERATE_INFOPLIST_FILE = YES` stays; Xcode merges the file with the generated keys.) Do not add `NSCameraUsageDescription` as an `INFOPLIST_KEY_` too.

- [ ] **Step 3: pbxproj local package reference** — read the existing remote-package objects to mirror their shape (`grep -n "XCRemoteSwiftPackageReference\|XCSwiftPackageProductDependency\|packageReferences\|packageProductDependencies\|productRef" COPDFuel/COPDFuel.xcodeproj/project.pbxproj`). Then add, with three fresh 24-hex ids (`uuidgen | tr -d - | cut -c1-24 | tr a-f A-F`), call them `REF`, `DEP`, `FILE`:
  1. In `/* Begin PBXBuildFile section */`: `FILE /* LabelScanKit in Frameworks */ = {isa = PBXBuildFile; productRef = DEP /* LabelScanKit */; };`
  2. In the app target's `PBXFrameworksBuildPhase` `files = (` list: `FILE /* LabelScanKit in Frameworks */,`
  3. In the `PBXNativeTarget` for `COPDFuel`: `packageProductDependencies = ( … existing …, DEP /* LabelScanKit */, );` (add the key if absent).
  4. In `PBXProject`: `packageReferences = ( … existing remote refs …, REF /* XCLocalSwiftPackageReference "LabelScanKit" */, );`
  5. New section before `/* Begin XCRemoteSwiftPackageReference section */`:
```
/* Begin XCLocalSwiftPackageReference section */
		REF /* XCLocalSwiftPackageReference "LabelScanKit" */ = {
			isa = XCLocalSwiftPackageReference;
			relativePath = LabelScanKit;
		};
/* End XCLocalSwiftPackageReference section */
```
  6. In `/* Begin XCSwiftPackageProductDependency section */`: `DEP /* LabelScanKit */ = { isa = XCSwiftPackageProductDependency; productName = LabelScanKit; };`

- [ ] **Step 4: Verify the project still parses** — `xcodebuild -project COPDFuel/COPDFuel.xcodeproj -list 2>&1 | head -5` prints the scheme; `plutil -lint COPDFuel/COPDFuel.xcodeproj/project.pbxproj` prints OK. Then `xcodebuild … -resolvePackageDependencies -derivedDataPath "$HOME/Library/Developer/Xcode/DerivedData/COPDFuel-cli"` succeeds (LabelScanKit listed under "Resolved source packages").

---

### Task 5: Vision OCR + camera + review + photo library (app target)

**Files:**
- Create: `COPDFuel/COPDFuel/Services/LabelScan/LabelOCR.swift`
- Create: `COPDFuel/COPDFuel/Views/Scan/AddFoodOptionsSheet.swift`
- Create: `COPDFuel/COPDFuel/Views/Scan/ScanLabelView.swift`
- Create: `COPDFuel/COPDFuel/Views/Scan/LabelReviewView.swift`
- Create: `COPDFuel/COPDFuel/Views/Scan/PhotoLibraryLabelPicker.swift`

**Interfaces (produced):**
- `LabelOCR.recognizeLines(_ image: UIImage) async throws -> [OcrLine]`, `LabelOCR.recognize(_ image: UIImage) async throws -> String`.
- `AddFoodOptionsSheet(onAddFood:onScanLabel:onScanQr:onPhotoLibrary:)`.
- `ScanLabelView(mode: .label | .qr, date: Date, onComplete: (FoodEntry, Bool) -> Void)` presented as `fullScreenCover`.
- `LabelReviewView(parsed: ParsedLabel, date: Date, onComplete:, onCancel:)` and `LabelReviewView(nutritionImage: UIImage, date:, onComplete:, onCancel:)` (Photo Library path, OCR inside).
- `ScanResultHandler.handle(entry:saveToMyFoods:)` (Tracking path: insert + per-100 g `UserAddedFood`).

- [ ] **Step 1: `Services/LabelScan/LabelOCR.swift`**

```swift
//
//  LabelOCR.swift
//  COPDFuel
//
//  Vision port of MlKitLabelOcr.kt: every recognized line with its height
//  (length of the line's left edge, in image pixels) and top, sorted
//  top-to-bottom then left-to-right.
//

import UIKit
import Vision
import LabelScanKit

enum LabelOCR {
    static func recognize(_ image: UIImage) async throws -> String {
        try await recognizeLines(image).map { $0.text }.joined(separator: "\n")
    }

    static func recognizeLines(_ image: UIImage) async throws -> [OcrLine] {
        guard let cgImage = image.cgImage else { throw NSError(domain: "LabelOCR", code: 1, userInfo: [NSLocalizedDescriptionKey: "Could not load image"]) }
        let width = Double(cgImage.width), height = Double(cgImage.height)
        return try await withCheckedThrowingContinuation { continuation in
            let request = VNRecognizeTextRequest { request, error in
                if let error { continuation.resume(throwing: error); return }
                let observations = request.results as? [VNRecognizedTextObservation] ?? []
                let lines: [(OcrLine, Double)] = observations.compactMap { obs in
                    guard let text = obs.topCandidates(1).first?.string, !text.isEmpty else { return nil }
                    let dx = (obs.bottomLeft.x - obs.topLeft.x) * width
                    let dy = (obs.bottomLeft.y - obs.topLeft.y) * height
                    let lineHeight = Int(hypot(dx, dy).rounded())
                    let top = Int(((1 - obs.boundingBox.maxY) * height).rounded())
                    return (OcrLine(text: text, height: lineHeight, top: top), obs.boundingBox.minX)
                }
                let sorted = lines.sorted { a, b in a.0.top != b.0.top ? a.0.top < b.0.top : a.1 < b.1 }.map { $0.0 }
                continuation.resume(returning: sorted)
            }
            request.recognitionLevel = .accurate
            request.usesLanguageCorrection = false
            let handler = VNImageRequestHandler(cgImage: cgImage, orientation: CGImagePropertyOrientation(image.imageOrientation), options: [:])
            DispatchQueue.global(qos: .userInitiated).async {
                do { try handler.perform([request]) } catch { continuation.resume(throwing: error) }
            }
        }
    }
}

private extension CGImagePropertyOrientation {
    init(_ o: UIImage.Orientation) {
        switch o {
        case .up: self = .up
        case .down: self = .down
        case .left: self = .left
        case .right: self = .right
        case .upMirrored: self = .upMirrored
        case .downMirrored: self = .downMirrored
        case .leftMirrored: self = .leftMirrored
        case .rightMirrored: self = .rightMirrored
        @unknown default: self = .up
        }
    }
}
```

- [ ] **Step 2: `Views/Scan/AddFoodOptionsSheet.swift`** (bottom_sheet_add_food.xml + AddFoodBottomSheet.kt)

```swift
//
//  AddFoodOptionsSheet.swift
//  COPDFuel
//
//  "+ Quick Add Food" options (AddFoodBottomSheet): Add Food, Scan Label,
//  Scan QR Code, Photo Library. Tap dismisses the sheet then dispatches.
//

import SwiftUI

struct AddFoodOptionsSheet: View {
    @Environment(\.dismiss) private var dismiss
    let onAddFood: () -> Void
    let onScanLabel: () -> Void
    let onScanQr: () -> Void
    let onPhotoLibrary: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("Add Food")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            HStack(spacing: 12) {
                tile("Add Food", "plus.circle.fill", onAddFood)
                tile("Scan Label", "doc.text.viewfinder", onScanLabel)
                tile("Scan QR Code", "qrcode.viewfinder", onScanQr)
                tile("Photo Library", "photo.on.rectangle", onPhotoLibrary)
            }
        }
        .padding(20)
        .presentationDetents([.height(190)])
    }

    private func tile(_ label: String, _ symbol: String, _ action: @escaping () -> Void) -> some View {
        Button(action: { dismiss(); DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { action() } }) {
            VStack(spacing: 8) {
                Image(systemName: symbol)
                    .font(.system(size: 36))
                    .foregroundColor(Color(hex: "2563eb"))
                    .frame(height: 48)
                Text(label)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(Color(hex: "1f2937"))
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}
```

- [ ] **Step 3: `Views/Scan/LabelReviewView.swift`** (activity_label_review.xml + LabelReviewActivity.kt)

```swift
//
//  LabelReviewView.swift
//  COPDFuel
//
//  "Review Scanned Label" (LabelReviewActivity). Parsed values arrive
//  from the camera/QR path already parsed; the Photo Library path passes
//  the image and OCR runs here.
//

import SwiftUI
import LabelScanKit

struct LabelReviewView: View {
    @Environment(\.dismiss) private var dismiss
    let date: Date
    let onComplete: (FoodEntry, Bool) -> Void
    let onCancel: () -> Void

    private let initialParsed: ParsedLabel?
    private let nutritionImage: UIImage?

    @State private var foodName = ""
    @State private var servingLine = "Serving size: Not detected"
    @State private var grams = ""
    @State private var calories = ""
    @State private var protein = ""
    @State private var carbs = ""
    @State private var fat = ""
    @State private var lowCalories = false
    @State private var lowProtein = false
    @State private var lowCarbs = false
    @State private var lowFat = false
    @State private var extras: [ExtraNutrient: String] = [:]
    @State private var showMore = false
    @State private var mealPrompt: Bool? = nil     // nil = hidden; value = saveToMyFoods
    @State private var mealChoice = "Breakfast"

    init(parsed: ParsedLabel, date: Date, onComplete: @escaping (FoodEntry, Bool) -> Void, onCancel: @escaping () -> Void) {
        self.initialParsed = parsed; self.nutritionImage = nil
        self.date = date; self.onComplete = onComplete; self.onCancel = onCancel
    }

    init(nutritionImage: UIImage, date: Date, onComplete: @escaping (FoodEntry, Bool) -> Void, onCancel: @escaping () -> Void) {
        self.initialParsed = nil; self.nutritionImage = nutritionImage
        self.date = date; self.onComplete = onComplete; self.onCancel = onCancel
    }

    private static let extraLabels: [(ExtraNutrient, String)] = [
        (.fiber, "Fiber (g)"), (.addedSugars, "Added sugars (g)"), (.saturatedFat, "Saturated fat (g)"),
        (.cholesterol, "Cholesterol (mg)"), (.sodium, "Sodium (mg)"), (.potassium, "Potassium (mg)"),
        (.calcium, "Calcium (mg)"), (.iron, "Iron (mg)"), (.vitaminD, "Vitamin D (mcg)")
    ]

    private var hasGrams: Bool { Double(grams.trimmingCharacters(in: .whitespaces)) != nil }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    field("Food name", $foodName, keyboard: .default)
                    Text(servingLine).font(.system(size: 14)).foregroundColor(Color(hex: "4b5563"))
                    field("Grams per serving", $grams)
                    macroField("Calories", $calories, low: lowCalories)
                    macroField("Protein (g)", $protein, low: lowProtein)
                    macroField("Carbs (g)", $carbs, low: lowCarbs)
                    macroField("Fat (g)", $fat, low: lowFat)

                    Button(showMore ? "More nutrients  ▴" : "More nutrients  ▾") { showMore.toggle() }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Color(hex: "2563eb"))
                    if showMore {
                        ForEach(Self.extraLabels, id: \.0) { nutrient, label in
                            field(label, Binding(get: { extras[nutrient] ?? "" }, set: { extras[nutrient] = $0 }))
                        }
                    }

                    HStack(spacing: 12) {
                        TrackingButton(title: "Add to today's log") { mealPrompt = false }
                        TrackingButton(title: "Save to My Foods") { mealPrompt = true }
                            .disabled(!hasGrams)
                            .opacity(hasGrams ? 1 : 0.5)
                    }
                    HStack(spacing: 12) {
                        outlined("Retake") { cancel() }
                        outlined("Cancel") { cancel() }
                    }
                }
                .padding(20)
            }
            .navigationTitle("Review Scanned Label")
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(true)
            .task { await load() }
            .sheet(isPresented: Binding(get: { mealPrompt != nil }, set: { if !$0 { mealPrompt = nil } })) {
                mealPicker
            }
        }
        .interactiveDismissDisabled()
        .toastOverlay()
    }

    private func field(_ hint: String, _ text: Binding<String>, keyboard: UIKeyboardType = .decimalPad) -> some View {
        TextField(hint, text: text)
            .keyboardType(keyboard)
            .textFieldStyle(.roundedBorder)
    }

    private func macroField(_ hint: String, _ text: Binding<String>, low: Bool) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            field(hint, text)
            if low {
                Text("Please check this value.").font(.system(size: 12)).foregroundColor(Color(hex: "f97316"))
            }
        }
    }

    private func outlined(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(Color(hex: "2563eb"))
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(hex: "2563eb"), lineWidth: 1))
        }
    }

    /// "Add to which meal?" single choice, default by hour (MealCategories.defaultFor).
    private var mealPicker: some View {
        NavigationStack {
            VStack {
                Picker("Add to which meal?", selection: $mealChoice) {
                    ForEach(MealCategories.all, id: \.self) { Text($0).tag($0) }
                }
                .pickerStyle(.inline)
                .labelsHidden()
                Spacer()
            }
            .padding(20)
            .navigationTitle("Add to which meal?")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { mealPrompt = nil } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        let save = mealPrompt ?? false
                        mealPrompt = nil
                        finish(mealCategory: mealChoice, saveToMyFoods: save)
                    }
                }
            }
            .onAppear { mealChoice = MealCategories.defaultFor(hourOfDay: Calendar.current.component(.hour, from: Date())) }
        }
        .presentationDetents([.medium])
    }

    // MARK: Load (bindValues :160-184; Photo Library path runs OCR here)

    @MainActor
    private func load() async {
        if let parsed = initialParsed {
            bind(parsed)
        } else if let image = nutritionImage {
            do {
                let text = try await LabelOCR.recognize(image)
                bind(NutritionLabelParser.parse(text))
            } catch {
                ToastCenter.shared.show("Could not read label")
            }
        }
    }

    private func bind(_ parsed: ParsedLabel) {
        foodName = parsed.productName ?? ""
        if let desc = parsed.servingSize?.description, !desc.trimmingCharacters(in: .whitespaces).isEmpty {
            servingLine = "Serving size: \(desc)"
        } else {
            servingLine = "Serving size: Not detected"
        }
        grams = parsed.servingSize?.grams.map { String($0) } ?? ""
        (calories, lowCalories) = text(parsed.calories)
        (protein, lowProtein) = text(parsed.protein)
        (carbs, lowCarbs) = text(parsed.carbs)
        (fat, lowFat) = text(parsed.fat)
        for (nutrient, value) in parsed.extras { extras[nutrient] = String(value.value) }
        showMore = !parsed.extras.isEmpty
    }

    private func text(_ v: ValueWithConfidence?) -> (String, Bool) {
        (v.map { String($0.value) } ?? "", v?.confidence == .low)
    }

    // MARK: Save (buildFoodEntry :214-243)

    private func finish(mealCategory: String, saveToMyFoods: Bool) {
        let trimmedName = foodName.trimmingCharacters(in: .whitespaces)
        let gramsValue = Double(grams.trimmingCharacters(in: .whitespaces))
        let quantity = gramsValue.map { "1 serving (\(Int($0))g)" } ?? "1 serving"
        func extra(_ n: ExtraNutrient) -> Double { Double(extras[n] ?? "") ?? 0 }
        let entry = FoodEntry(
            date: date, mealType: mealCategory,
            foodName: trimmedName.isEmpty ? "Scanned food" : trimmedName, quantity: quantity,
            calories: Double(calories) ?? 0, protein: Double(protein) ?? 0, carbs: Double(carbs) ?? 0, fat: Double(fat) ?? 0,
            fiber: extra(.fiber), sodium: extra(.sodium), potassium: extra(.potassium), calcium: extra(.calcium),
            iron: extra(.iron), vitaminD: extra(.vitaminD), saturatedFat: extra(.saturatedFat),
            cholesterol: extra(.cholesterol), addedSugars: extra(.addedSugars))
        onComplete(entry, saveToMyFoods)
        dismiss()
    }

    private func cancel() {
        onCancel()
        dismiss()
    }
}

/// TrackingFragment.saveScannedFoodToDatabase (:1975-2002) + scan result insert.
enum ScanResultHandler {
    static func handle(entry: FoodEntry, saveToMyFoods: Bool) {
        let dm = DataManager.shared
        dm.addFoodEntry(entry)
        guard saveToMyFoods,
              let m = Regex_extractGrams(entry.quantity), m > 0 else { return }
        let factor = m / 100.0
        guard factor > 0 else { return }
        dm.addUserAddedFood(UserAddedFood(
            name: entry.foodName,
            calories: (entry.calories ?? 0) / factor, protein: (entry.protein ?? 0) / factor,
            carbs: (entry.carbs ?? 0) / factor, fat: (entry.fat ?? 0) / factor,
            fiber: entry.fiber / factor, sodium: entry.sodium / factor, potassium: entry.potassium / factor,
            calcium: entry.calcium / factor, iron: entry.iron / factor, vitaminD: entry.vitaminD / factor,
            saturatedFat: entry.saturatedFat / factor, cholesterol: entry.cholesterol / factor,
            addedSugars: entry.addedSugars / factor))
    }

    /// `\((\d+(?:\.\d+)?)g\)` → grams.
    private static func Regex_extractGrams(_ quantity: String) -> Double? {
        guard let r = try? NSRegularExpression(pattern: "\\((\\d+(?:\\.\\d+)?)g\\)"),
              let m = r.firstMatch(in: quantity, range: NSRange(quantity.startIndex..., in: quantity)),
              let range = Range(m.range(at: 1), in: quantity) else { return nil }
        return Double(quantity[range])
    }
}
```
(Rename `Regex_extractGrams` to `extractGrams` — the name above is only to avoid clashing with the package-internal `Regex` helper, which is not visible to the app anyway; use `extractGrams`.)

- [ ] **Step 4: `Views/Scan/PhotoLibraryLabelPicker.swift`**

```swift
//
//  PhotoLibraryLabelPicker.swift
//  COPDFuel
//
//  Photo Library entry: the picked image goes straight to the review
//  screen as a nutrition-label photo (TrackingFragment.photoLibraryLauncher).
//

import SwiftUI
import PhotosUI

struct PhotoLibraryLabelPicker: View {
    let date: Date
    let onComplete: (FoodEntry, Bool) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var selection: PhotosPickerItem?
    @State private var image: UIImage?

    var body: some View {
        Group {
            if let image {
                LabelReviewView(nutritionImage: image, date: date, onComplete: onComplete, onCancel: { dismiss() })
            } else {
                PhotosPicker(selection: $selection, matching: .images) { EmptyView() }
                    .photosPickerStyle(.inline)
                    .ignoresSafeArea()
            }
        }
        .onChange(of: selection) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self), let ui = UIImage(data: data) {
                    await MainActor.run { image = ui }
                } else {
                    await MainActor.run { dismiss() }
                }
            }
        }
    }
}
```
(If `.photosPickerStyle(.inline)` is unavailable on the minimum target, present `PhotosPicker` as a labelled button inside a `NavigationStack` with a Cancel item instead.)

- [ ] **Step 5: `Views/Scan/ScanLabelView.swift`** (activity_scan_label.xml + ScanLabelActivity.kt)

```swift
//
//  ScanLabelView.swift
//  COPDFuel
//
//  Camera screen (ScanLabelActivity). Label mode: optional front photo →
//  Nutrition Facts photo → parse → review. QR mode: live barcode/QR
//  analysis → GTIN → USDA lookup (or product-link resolution) → review.
//

import AVFoundation
import Combine
import SwiftUI
import LabelScanKit

struct ScanLabelView: View {
    enum Mode { case label, qr }

    let mode: Mode
    let date: Date
    let onComplete: (FoodEntry, Bool) -> Void

    @Environment(\.dismiss) private var dismiss
    @StateObject private var camera = ScanCameraController()
    @State private var reviewParsed: ParsedLabel?

    var body: some View {
        ZStack {
            CameraPreview(session: camera.session).ignoresSafeArea()
            Color.black.opacity(camera.isReady ? 0 : 1).ignoresSafeArea()

            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.white, lineWidth: 3)
                .frame(width: 280, height: 360)

            VStack {
                Text(camera.instruction)
                    .font(.system(size: 18))
                    .foregroundColor(.white)
                    .shadow(color: .black.opacity(0.8), radius: 3)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                    .padding(.top, 64)
                Spacer()
                if camera.mode == .qr {
                    Button("Scan nutrition label instead") { camera.switchToLabelMode() }
                        .foregroundColor(.white)
                        .padding(.bottom, 120)
                } else {
                    if camera.showSkip {
                        Button("Skip") { camera.proceedToNutritionLabel() }
                            .foregroundColor(.white)
                            .padding(.bottom, 8)
                    }
                    Button(action: { camera.takePhoto() }) {
                        Circle()
                            .fill(Color.white)
                            .frame(width: 72, height: 72)
                            .overlay(Circle().stroke(Color.gray, lineWidth: 4))
                    }
                    .disabled(!camera.shutterEnabled)
                    .opacity(camera.shutterEnabled ? 1 : 0.5)
                    .padding(.bottom, 32)
                }
            }

            VStack {
                HStack {
                    Button("Close") { dismiss() }
                        .foregroundColor(.white)
                        .padding()
                    Spacer()
                }
                Spacer()
            }
        }
        .onAppear { camera.start(mode: mode, date: date) }
        .onDisappear { camera.stop() }
        .onChange(of: camera.reviewParsed) { _, parsed in reviewParsed = parsed }
        .onChange(of: camera.permissionDenied) { _, denied in if denied { dismiss() } }
        .fullScreenCover(item: $reviewParsed) { parsed in
            LabelReviewView(parsed: parsed, date: date, onComplete: { entry, save in
                onComplete(entry, save)
                dismiss()
            }, onCancel: {
                // Retake == Cancel on Android: the camera closes too.
                dismiss()
            })
        }
        .toastOverlay()
    }
}

extension ParsedLabel: Identifiable {
    public var id: String { (productName ?? "") + rawLines.joined() + String(calories?.value ?? -1) }
}

// MARK: - Preview layer

struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession
    func makeUIView(context: Context) -> PreviewView {
        let v = PreviewView()
        v.videoPreviewLayer.session = session
        v.videoPreviewLayer.videoGravity = .resizeAspectFill
        return v
    }
    func updateUIView(_ uiView: PreviewView, context: Context) {}
    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var videoPreviewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    }
}

// MARK: - Controller (ScanLabelActivity state machine)

@MainActor
final class ScanCameraController: NSObject, ObservableObject, AVCapturePhotoCaptureDelegate, AVCaptureMetadataOutputObjectsDelegate {
    @Published var mode: ScanLabelView.Mode = .label
    @Published var instruction = "Photo front of package (optional)"
    @Published var showSkip = true
    @Published var shutterEnabled = true
    @Published var isReady = false
    @Published var permissionDenied = false
    @Published var reviewParsed: ParsedLabel?

    let session = AVCaptureSession()
    private let photoOutput = AVCapturePhotoOutput()
    private let metadataOutput = AVCaptureMetadataOutput()
    private var date = Date()
    private var isCapturingFront = true
    private var frontImage: UIImage?
    private var isQrLookupActive = false
    private var unresolvableLinks = Set<String>()
    private var configured = false

    func start(mode: ScanLabelView.Mode, date: Date) {
        self.mode = mode
        self.date = date
        configureUi()
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            configureAndRun()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                Task { @MainActor in
                    if granted { self.configureAndRun() } else { self.deny() }
                }
            }
        default:
            deny()
        }
    }

    private func deny() {
        ToastCenter.shared.show("Camera permission is required to scan labels")
        permissionDenied = true
    }

    func stop() {
        let s = session
        DispatchQueue.global(qos: .userInitiated).async { if s.isRunning { s.stopRunning() } }
    }

    private func configureUi() {
        if mode == .qr {
            instruction = "Point camera at the product barcode or QR code"
            showSkip = false
        } else {
            instruction = isCapturingFront ? "Photo front of package (optional)" : "Photo Nutrition Facts panel"
            showSkip = isCapturingFront
        }
    }

    private func configureAndRun() {
        if !configured {
            configured = true
            session.beginConfiguration()
            session.sessionPreset = .photo
            guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
                  let input = try? AVCaptureDeviceInput(device: device), session.canAddInput(input) else {
                session.commitConfiguration()
                ToastCenter.shared.show("Camera failed to start")
                return
            }
            session.addInput(input)
            if session.canAddOutput(photoOutput) { session.addOutput(photoOutput) }
            if session.canAddOutput(metadataOutput) {
                session.addOutput(metadataOutput)
                metadataOutput.setMetadataObjectsDelegate(self, queue: .main)
            }
            session.commitConfiguration()
        }
        applyMetadataTypes()
        let s = session
        DispatchQueue.global(qos: .userInitiated).async {
            s.startRunning()
            Task { @MainActor in self.isReady = true }
        }
    }

    /// QR mode: all symbologies (ML Kit default). Label mode: none.
    private func applyMetadataTypes() {
        guard metadataOutput.connections.first != nil || session.outputs.contains(metadataOutput) else { return }
        metadataOutput.metadataObjectTypes = mode == .qr
            ? [.qr, .ean13, .ean8, .upce, .code128, .code39, .dataMatrix, .aztec, .pdf417].filter { metadataOutput.availableMetadataObjectTypes.contains($0) }
            : []
    }

    func switchToLabelMode() {
        mode = .label
        isCapturingFront = true
        isQrLookupActive = false
        configureUi()
        applyMetadataTypes()
    }

    // MARK: Label mode (:160-253)

    func takePhoto() {
        let settings = AVCapturePhotoSettings()
        photoOutput.capturePhoto(with: settings, delegate: self)
    }

    nonisolated func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        let image = photo.fileDataRepresentation().flatMap { UIImage(data: $0) }
        Task { @MainActor in
            guard error == nil, let image else {
                ToastCenter.shared.show("Photo capture failed")
                return
            }
            if isCapturingFront { await handleFirstPhoto(image) } else { await processLabel(image) }
        }
    }

    // People often photograph the Nutrition Facts panel first. Treat that photo as the
    // nutrition label instead of silently keeping it as the optional front photo.
    private func handleFirstPhoto(_ image: UIImage) async {
        shutterEnabled = false
        instruction = "Reading label…"
        let isNutritionPanel: Bool
        do { isNutritionPanel = NutritionLabelParser.looksLikeNutritionPanel(try await LabelOCR.recognize(image)) }
        catch { isNutritionPanel = false }
        shutterEnabled = true
        proceedToNutritionLabel()
        if isNutritionPanel {
            await processLabel(image)
        } else {
            frontImage = image
            ToastCenter.shared.show("Front photo saved. Now photograph the Nutrition Facts panel.")
        }
    }

    func proceedToNutritionLabel() {
        isCapturingFront = false
        instruction = "Photo Nutrition Facts panel"
        showSkip = false
    }

    private func processLabel(_ image: UIImage) async {
        instruction = "Reading label…"
        shutterEnabled = false
        do {
            let text = try await LabelOCR.recognize(image)
            var parsed = NutritionLabelParser.parse(text)
            if parsed.calories == nil && parsed.protein == nil && parsed.carbs == nil && parsed.fat == nil {
                ToastCenter.shared.show("Could not read nutrition label. Try again.")
                shutterEnabled = true
                instruction = "Photo Nutrition Facts panel"
                return
            }
            if let frontImage {
                let frontLines = (try? await LabelOCR.recognizeLines(frontImage)) ?? []
                parsed.productName = NutritionLabelParser.extractProductName(frontLines)
            }
            reviewParsed = parsed
        } catch {
            ToastCenter.shared.show("Could not read label: \(error.localizedDescription)")
            shutterEnabled = true
            instruction = "Photo Nutrition Facts panel"   // spec §1.3: reset (Android bug not ported)
        }
    }

    // MARK: QR mode (:255-354)

    nonisolated func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        let values = metadataObjects.compactMap { ($0 as? AVMetadataMachineReadableCodeObject)?.stringValue }
        Task { @MainActor in self.handleBarcodes(values) }
    }

    private func handleBarcodes(_ values: [String]) {
        guard mode == .qr, !isQrLookupActive, !values.isEmpty else { return }
        let gtin = values.lazy.compactMap { GTIN.extractGtin($0) }.first
        let link = values.first { ProductLinkResolver.isWebLink($0) }
        if let gtin {
            isQrLookupActive = true
            instruction = "Looking up product…"
            Task { await lookupGtin(gtin) }
        } else if let link, !unresolvableLinks.contains(link) {
            // Package QR codes (e.g. SmartLabel) are usually short links to a product page.
            isQrLookupActive = true
            instruction = "Looking up product…"
            Task { await resolveLinkThenLookup(link) }
        } else {
            instruction = "This code has no product number. Point at the striped barcode instead."
        }
    }

    private func resolveLinkThenLookup(_ link: String) async {
        let gtin = (try? await ProductLinkResolver().resolveGtin(link)) ?? nil
        if let gtin {
            await lookupGtin(gtin)
        } else {
            unresolvableLinks.insert(link)
            isQrLookupActive = false
            instruction = "This code has no product number. Point at the striped barcode instead."
        }
    }

    private func lookupGtin(_ gtin: String) async {
        let apiKey = AppConfig.usdaApiKey
        if apiKey.trimmingCharacters(in: .whitespaces).isEmpty {
            instruction = "Product lookup is not set up (missing USDA API key)."
            ToastCenter.shared.show("USDA API key not configured")
            isQrLookupActive = false
            return
        }
        do {
            if let parsed = try await USDAGtinLookup().lookup(gtin: gtin, apiKey: apiKey) {
                reviewParsed = parsed
            } else {
                isQrLookupActive = false
                instruction = "Point camera at the product barcode or QR code"
                ToastCenter.shared.show("Product not found. Try scanning the label.")
            }
        } catch {
            isQrLookupActive = false
            instruction = "Point camera at the product barcode or QR code"
            ToastCenter.shared.show("Lookup failed: \(error.localizedDescription)")
        }
    }
}
```
Note: Android's `lookupGtin` leaves `isQrLookupActive = true` on the missing-key path (the screen stays parked on the message); iOS resets it so the user can switch to label mode, which is the only interactive way forward — keep the message and toast verbatim.

---

### Task 6: Wire into Tracking and Add Food; build; test; commit

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/Tracking/TrackingDayView.swift` (quick-add sheet + scan covers)
- Modify: `COPDFuel/COPDFuel/Views/Tracking/AddFoodDialog.swift` (scan icon button + `prefillFromScan`)

- [ ] **Step 1: `TrackingDayView`** — add state:
```swift
    @State private var showingAddFoodOptions = false
    @State private var scanMode: ScanLabelView.Mode?
    @State private var showingPhotoLibrary = false
```
Change the "+ Quick Add Food" action to `showingAddFoodOptions = true`, and add modifiers:
```swift
        .sheet(isPresented: $showingAddFoodOptions) {
            AddFoodOptionsSheet(
                onAddFood: { showingAddFood = true },
                onScanLabel: { scanMode = .label },
                onScanQr: { scanMode = .qr },
                onPhotoLibrary: { showingPhotoLibrary = true })
        }
        .fullScreenCover(item: $scanMode) { mode in
            ScanLabelView(mode: mode, date: selectedDate) { entry, save in
                ScanResultHandler.handle(entry: entry, saveToMyFoods: save)
            }
        }
        .fullScreenCover(isPresented: $showingPhotoLibrary) {
            PhotoLibraryLabelPicker(date: selectedDate) { entry, save in
                ScanResultHandler.handle(entry: entry, saveToMyFoods: save)
                showingPhotoLibrary = false
            }
        }
```
and `extension ScanLabelView.Mode: Identifiable { var id: Int { self == .label ? 0 : 1 } }` in `ScanLabelView.swift`. Scan entries use `date: TrackingDates.timestamp(on: selectedDate)` — pass that instead of `selectedDate` to both covers.

- [ ] **Step 2: `AddFoodDialog`** — next to the "Edit Nutrition Manually" button add a 48-pt icon button:
```swift
                    HStack {
                        Button(showManual ? "Hide Manual Entry" : "Edit Nutrition Manually") { showManual.toggle() }
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(Color(hex: "2563eb"))
                        Spacer()
                        Button(action: { showingScanOptions = true }) {
                            Image(systemName: "doc.text.viewfinder")
                                .font(.system(size: 22))
                                .foregroundColor(Color(hex: "2563eb"))
                                .frame(width: 48, height: 48)
                        }
                        .accessibilityLabel("Scan Label")
                    }
```
with state `@State private var showingScanOptions = false`, `@State private var scanMode: ScanLabelView.Mode?`, `@State private var showingPhotoLibrary = false`, the same three presentation modifiers as Step 1 but with completion `prefillFromScan($0)` (the `Bool` is ignored on this path), and:
```swift
    /// AddFoodDialog.prefillFromScan (:815-823).
    private func prefillFromScan(_ entry: FoodEntry) {
        foodName = entry.foodName
        showManual = true
        manualCalories = "\(Int(entry.calories ?? 0))"
        manualProtein = String(format: "%.1f", entry.protein ?? 0)
        manualCarbs = String(format: "%.1f", entry.carbs ?? 0)
        manualFat = String(format: "%.1f", entry.fat ?? 0)
    }
```
In `AddFoodOptionsSheet` for this dialog pass `onAddFood: {}` (no-op, "already in AddFoodDialog").

- [ ] **Step 3: Package tests + app build**

```bash
swift test --package-path COPDFuel/LabelScanKit 2>&1 | tail -3
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -derivedDataPath "$HOME/Library/Developer/Xcode/DerivedData/COPDFuel-cli" -quiet build 2>&1 | grep -E "error:" | head -20; echo "exit=${PIPESTATUS[0]}"
```
Expected: `Executed 57 tests, with 0 failures` and `exit=0`. Likely first-pass issues: `import LabelScanKit` unresolved → Task 4 Step 3 ids/sections wrong (compare with the existing remote-package entries); `ParsedLabel: Identifiable` retroactive conformance warning is acceptable; `Combine` missing in `ScanLabelView.swift`.

- [ ] **Step 4: Copy check**

```bash
for s in "Add Food" "Scan Label" "Scan QR Code" "Photo Library" "Review Scanned Label" "Photo front of package (optional)" "Photo Nutrition Facts panel" "Point camera at the product barcode or QR code" "Looking up product" "This code has no product number. Point at the striped barcode instead." "Product lookup is not set up (missing USDA API key)." "Scan nutrition label instead" "Reading label" "Front photo saved. Now photograph the Nutrition Facts panel." "Grams per serving" "More nutrients" "Please check this value." "Add to today's log" "Save to My Foods" "Add to which meal?" "Serving size: " "Could not read nutrition label. Try again." "Product not found. Try scanning the label." "Camera permission is required to scan labels"; do
  a=$(grep -rl -F "$s" android/app/src/main/res/values/strings.xml android/app/src/main/java/com/copdhealthtracker/ui/scan | wc -l | tr -d ' ')
  i=$(grep -rl -F "$s" COPDFuel/COPDFuel/Views/Scan COPDFuel/COPDFuel/Views/Tracking | wc -l | tr -d ' ')
  echo "$a android / $i ios  <- $s"
done
```
Expected: non-zero on both sides (the Android side of "Add to today's log" is `Add to today\'s log` in XML; treat a 0 there as pass if the iOS side is 1).

- [ ] **Step 5: Commit**

```bash
cd COPDFuel && /usr/bin/git add -A && /usr/bin/git commit -q -m "iOS parity P3.D: food label scan — LabelScanKit package (parser, GTIN, link resolver, USDA lookup, meal categories; 57 ported tests), Vision OCR, camera label/QR flow, review screen, photo library, Tracking + Add Food hooks

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>" && /usr/bin/git log --oneline -1
```

- [ ] **Step 6: Manual smoke (simulator; the camera is unavailable there)**

Tracking → "+ Quick Add Food" shows the four tiles; "Photo Library" → pick a label photo (add one to the simulator via Photos drag-and-drop) → "Review Scanned Label" fills calories/protein/carbs/fat, serving line and grams; "More nutrients  ▾" expands when extras were found; "Add to today's log" → "Add to which meal?" defaults by hour → entry appears in that meal section with quantity "1 serving (60g)"; "Save to My Foods" (enabled only with grams) also makes the food searchable in Add Food; "Scan Label" on the simulator toasts the camera message and closes. On a device: QR on a Quaker cup resolves through the pepsico.info link to the USDA product (requires `USDA_FDC_API_KEY`).
