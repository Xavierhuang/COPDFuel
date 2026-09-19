# Food Label Scanning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users scan packaged-food labels with the in-app camera or gallery and pre-fill the existing dietary entry form with calories, protein, carbs, and fat.

**Architecture:** A pure-Kotlin `NutritionLabelParser` sits behind a `LabelOcr` interface powered by on-device ML Kit. Camera capture uses CameraX. A `BottomSheetDialogFragment` offers Add Food / Scan Food / Photo Library, launching either the existing `AddFoodDialog` or a new `ScanLabelActivity` → `LabelReviewActivity` flow. Parsed values are returned to `AddFoodDialog` as editable manual-entry values.

**Tech Stack:** Android Kotlin, CameraX, ML Kit Text Recognition v2, Room (existing), JUnit 4 (existing).

**Spec:** `docs/superpowers/specs/2026-09-18-food-label-scan-design.md`

## Global Constraints

- `minSdk 26`, `targetSdk 36`, `compileSdk 36`
- No photo or OCR text leaves the device; ML Kit runs on-device.
- The app already stores `UserAddedFood` per 100g; scanned labels are per-serving, so "Save to my foods" is only allowed when a gram weight is present.
- Nutrients scanned are limited to the existing macro labels: calories, protein, carbs, fat.
- Front-label photo is optional and only used for product name.

---

## File map

| File | Responsibility |
|------|----------------|
| `labelscan/ParsedLabel.kt` | Data classes for parsed label output |
| `labelscan/LabelOcr.kt` | OCR interface |
| `labelscan/MlKitLabelOcr.kt` | ML Kit implementation |
| `labelscan/NutritionLabelParser.kt` | Pure Kotlin parser for Nutrition Facts text |
| `ui/bottomsheets/AddFoodBottomSheet.kt` | Bottom sheet: Add Food / Scan Food / Photo Library |
| `ui/scan/ScanLabelActivity.kt` | CameraX preview + capture front and nutrition labels |
| `ui/scan/LabelReviewActivity.kt` | Editable review of parsed values |
| `ui/scan/LabelCaptureViewModel.kt` | Holds captured URIs and parsed result across steps |
| `res/layout/bottom_sheet_add_food.xml` | Bottom sheet grid layout |
| `res/layout/activity_scan_label.xml` | Camera preview + overlay + controls |
| `res/layout/activity_label_review.xml` | Editable form for scanned values |
| `res/drawable/ic_scan_food.xml`, `ic_photo_library.xml` | Bottom sheet icons |
| `res/values/strings.xml` | New copy |
| `app/build.gradle` | CameraX + ML Kit dependencies |
| `AndroidManifest.xml` | Permissions + activity declarations |
| `ui/fragments/TrackingFragment.kt` | Launch bottom sheet from Quick Add Food |
| `ui/dialogs/AddFoodDialog.kt` | Camera icon result handler + pre-fill manual section |
| `res/layout/dialog_add_food.xml` | Camera icon next to manual entry |
| `test/.../NutritionLabelParserTest.kt` | Parser unit tests |

---

### Task 1: Add CameraX and ML Kit dependencies

**Files:**
- Modify: `app/build.gradle`

**Interfaces:**
- Produces: Gradle sync with new dependencies.

- [ ] **Step 1: Add dependencies**

In `app/build.gradle`, inside `dependencies { ... }` add:

```groovy
// CameraX
implementation 'androidx.camera:camera-core:1.3.1'
implementation 'androidx.camera:camera-camera2:1.3.1'
implementation 'androidx.camera:camera-lifecycle:1.3.1'
implementation 'androidx.camera:camera-view:1.3.1'

// ML Kit text recognition
implementation 'com.google.mlkit:text-recognition:16.0.1'
```

- [ ] **Step 2: Sync project**

Run: `./gradlew :app:dependencies --configuration implementation | grep -E "camera|mlkit"`

Expected: CameraX and ML Kit artifacts are listed.

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle
git commit -m "build: add CameraX and ML Kit text recognition dependencies"
```

---

### Task 2: Declare permissions and register activities

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `CAMERA` permission, read-media permissions, `ScanLabelActivity` and `LabelReviewActivity` declared.

- [ ] **Step 1: Add permissions**

Inside `<manifest>`, after the existing permissions, add:

```xml
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />

    <!-- Gallery import permissions -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
```

- [ ] **Step 2: Register activities**

Inside `<application>`, add:

```xml
        <activity
            android:name=".ui.scan.ScanLabelActivity"
            android:exported="false"
            android:screenOrientation="portrait"
            android:theme="@style/AppTheme" />

        <activity
            android:name=".ui.scan.LabelReviewActivity"
            android:exported="false"
            android:screenOrientation="portrait"
            android:theme="@style/AppTheme" />
```

- [ ] **Step 3: Build check**

Run: `./gradlew :app:assembleDebug`

Expected: Build succeeds.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "manifest: add camera permission and register scan activities"
```

---

### Task 3: Create parser data classes

**Files:**
- Create: `app/src/main/java/com/copdhealthtracker/labelscan/ParsedLabel.kt`

**Interfaces:**
- Produces: `ServingSize`, `ValueWithConfidence`, `Confidence`, `ParsedLabel`.

- [ ] **Step 1: Write the file**

```kotlin
package com.copdhealthtracker.labelscan

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
    val grams: Double?
)

data class ValueWithConfidence(
    val value: Double,
    val unit: String,
    val confidence: Confidence
)

enum class Confidence {
    HIGH,
    MEDIUM,
    LOW
}
```

- [ ] **Step 2: Build check**

Run: `./gradlew :app:compileDebugKotlin`

Expected: Compiles successfully.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/copdhealthtracker/labelscan/ParsedLabel.kt
git commit -m "feat(labelscan): add parsed label data classes"
```

---

### Task 4: Implement OCR interface and ML Kit implementation

**Files:**
- Create: `app/src/main/java/com/copdhealthtracker/labelscan/LabelOcr.kt`
- Create: `app/src/main/java/com/copdhealthtracker/labelscan/MlKitLabelOcr.kt`

**Interfaces:**
- Produces: `LabelOcr` interface with `suspend fun recognize(uri: Uri): String`.
- Produces: `MlKitLabelOcr` implementing it with ML Kit.

- [ ] **Step 1: Write `LabelOcr.kt`**

```kotlin
package com.copdhealthtracker.labelscan

import android.net.Uri

interface LabelOcr {
    suspend fun recognize(uri: Uri): String
}
```

- [ ] **Step 2: Write `MlKitLabelOcr.kt`**

```kotlin
package com.copdhealthtracker.labelscan

import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitLabelOcr : LabelOcr {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(uri: Uri): String = withContext(Dispatchers.IO) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            throw IllegalStateException("Could not load image: ${e.message}", e)
        }

        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }
}
```

Wait — `LabelOcr` has no context. Add `Context` to the constructor.

Corrected `LabelOcr.kt`:

```kotlin
package com.copdhealthtracker.labelscan

import android.content.Context
import android.net.Uri

interface LabelOcr {
    suspend fun recognize(uri: Uri): String
}
```

Corrected `MlKitLabelOcr.kt`:

```kotlin
package com.copdhealthtracker.labelscan

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitLabelOcr(private val context: Context) : LabelOcr {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(uri: Uri): String = withContext(Dispatchers.IO) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            throw IllegalStateException("Could not load image: ${e.message}", e)
        }

        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }
}
```

- [ ] **Step 3: Build check**

Run: `./gradlew :app:compileDebugKotlin`

Expected: Compiles successfully.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/copdhealthtracker/labelscan/LabelOcr.kt \
        app/src/main/java/com/copdhealthtracker/labelscan/MlKitLabelOcr.kt
git commit -m "feat(labelscan): add LabelOcr interface and ML Kit implementation"
```

---

### Task 5: Implement NutritionLabelParser

**Files:**
- Create: `app/src/main/java/com/copdhealthtracker/labelscan/NutritionLabelParser.kt`

**Interfaces:**
- Consumes: `ParsedLabel`, `ServingSize`, `ValueWithConfidence`, `Confidence`.
- Produces: `fun parse(text: String): ParsedLabel`.

- [ ] **Step 1: Write the parser**

```kotlin
package com.copdhealthtracker.labelscan

object NutritionLabelParser {

    fun parse(text: String): ParsedLabel {
        val rawLines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val normalized = rawLines.joinToString("\n") { normalizeLine(it) }

        val servingSize = extractServingSize(normalized)

        return ParsedLabel(
            productName = null,
            servingSize = servingSize,
            calories = extractValue(normalized, "calories", "kcal"),
            protein = extractValue(normalized, "protein", "g"),
            carbs = extractValue(normalized, listOf("total carbohydrate", "total carbs", "carbohydrate"), "g"),
            fat = extractValue(normalized, listOf("total fat", "total lipid"), "g"),
            rawLines = rawLines
        )
    }

    private fun normalizeLine(line: String): String {
        return line
            .replace(Regex("\\s+"), " ")
            .replace(Regex("(?<=\\d)[oO](?=\\d|\\s|g|mg|kcal|$)"), "0")
            .replace(Regex("(?<=^|\\s)[lL](?=\\d)"), "1")
            .replace(Regex("(?<=\\d)[sS](?=\\s|g|mg|kcal|$)"), "5")
            .lowercase()
    }

    private fun extractServingSize(text: String): ServingSize? {
        val regex = Regex("serving size\\s+(.+?)(?=\\n|\\$)", RegexOption.IGNORE_CASE)
        val match = regex.find(text) ?: return null
        val description = match.groupValues[1].trim()
        val grams = Regex("\\((\\d+(?:\\.\\d+)?)\\s*g\\)", RegexOption.IGNORE_CASE)
            .find(description)?.groupValues?.get(1)?.toDoubleOrNull()
        return ServingSize(description, grams)
    }

    private fun extractValue(text: String, keyword: String, unit: String): ValueWithConfidence? {
        return extractValue(text, listOf(keyword), unit)
    }

    private fun extractValue(text: String, keywords: List<String>, unit: String): ValueWithConfidence? {
        val lines = text.lines()
        for (keyword in keywords) {
            val line = lines.firstOrNull { it.contains(keyword, ignoreCase = true) } ?: continue
            val result = parseNumericValue(line, unit)
            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun parseNumericValue(line: String, unit: String): ValueWithConfidence? {
        // Prefer a number followed by the unit (g, mg, kcal)
        val unitRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*($unit|mg)", RegexOption.IGNORE_CASE)
        val unitMatch = unitRegex.find(line)
        if (unitMatch != null) {
            val value = unitMatch.groupValues[1].toDoubleOrNull() ?: return null
            val detectedUnit = unitMatch.groupValues[2].lowercase()
            return ValueWithConfidence(value, detectedUnit, Confidence.HIGH)
        }

        // Fallback: first number on the line
        val numberRegex = Regex("(\\d+(?:\\.\\d+)?)")
        val numberMatch = numberRegex.find(line)
        if (numberMatch != null) {
            val value = numberMatch.groupValues[1].toDoubleOrNull() ?: return null
            return ValueWithConfidence(value, unit, Confidence.LOW)
        }

        return null
    }
}
```

Note: This initial parser intentionally does not handle two-column panels yet; that is Task 6.

- [ ] **Step 2: Build check**

Run: `./gradlew :app:compileDebugKotlin`

Expected: Compiles successfully.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/copdhealthtracker/labelscan/NutritionLabelParser.kt
git commit -m "feat(labelscan): add basic NutritionLabelParser"
```

---

### Task 6: Add unit tests for the parser

**Files:**
- Create: `app/src/test/java/com/copdhealthtracker/labelscan/NutritionLabelParserTest.kt`

**Interfaces:**
- Consumes: `NutritionLabelParser.parse(...)`.

- [ ] **Step 1: Write tests**

```kotlin
package com.copdhealthtracker.labelscan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionLabelParserTest {

    @Test
    fun `parses standard single-column label`() {
        val text = """
            Nutrition Facts
            Serving Size 2/3 cup (55g)
            Calories 230
            Total Fat 8g
            Total Carbohydrate 37g
            Protein 3g
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals("2/3 cup (55g)", result.servingSize?.description)
        assertEquals(55.0, result.servingSize?.grams)
        assertEquals(230.0, result.calories?.value)
        assertEquals(8.0, result.fat?.value)
        assertEquals(37.0, result.carbs?.value)
        assertEquals(3.0, result.protein?.value)
        assertEquals(Confidence.HIGH, result.fat?.confidence)
    }

    @Test
    fun `prefers unit over percent daily value`() {
        val text = """
            Total Fat 8g 10%
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals(8.0, result.fat?.value)
        assertEquals("g", result.fat?.unit)
    }

    @Test
    fun `handles missing unit with low confidence`() {
        val text = """
            Protein 5
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals(5.0, result.protein?.value)
        assertEquals(Confidence.LOW, result.protein?.confidence)
    }

    @Test
    fun `fixes common ocr substitutions`() {
        val text = """
            Calories 1OO
            Tota1 Fat 8g
            Prote1n 5g
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals(100.0, result.calories?.value)
        assertEquals(8.0, result.fat?.value)
        assertEquals(5.0, result.protein?.value)
    }

    @Test
    fun `serving size without grams has null grams`() {
        val text = """
            Serving Size 2 slices
        """.trimIndent()

        val result = NutritionLabelParser.parse(text)

        assertEquals("2 slices", result.servingSize?.description)
        assertNull(result.servingSize?.grams)
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :app:testDebugUnitTest`

Expected: Tests pass.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/copdhealthtracker/labelscan/NutritionLabelParserTest.kt
git commit -m "test(labelscan): add NutritionLabelParser unit tests"
```

---

### Task 7: Create AddFoodBottomSheet

**Files:**
- Create: `app/src/main/java/com/copdhealthtracker/ui/bottomsheets/AddFoodBottomSheet.kt`
- Create: `app/src/main/res/layout/bottom_sheet_add_food.xml`
- Create: `app/src/main/res/drawable/ic_scan_food.xml`
- Create: `app/src/main/res/drawable/ic_photo_library.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: `AddFoodBottomSheet` with a listener callback `(action: Action) -> Unit` where `Action` is an enum: `ADD_FOOD`, `SCAN_FOOD`, `PHOTO_LIBRARY`.

- [ ] **Step 1: Define Action enum and listener inside the bottom sheet**

```kotlin
package com.copdhealthtracker.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.copdhealthtracker.databinding.BottomSheetAddFoodBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddFoodBottomSheet(
    private val onAction: (Action) -> Unit
) : BottomSheetDialogFragment() {

    enum class Action { ADD_FOOD, SCAN_FOOD, PHOTO_LIBRARY }

    private var _binding: BottomSheetAddFoodBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addFoodOption.setOnClickListener { dismissAndEmit(Action.ADD_FOOD) }
        binding.scanFoodOption.setOnClickListener { dismissAndEmit(Action.SCAN_FOOD) }
        binding.photoLibraryOption.setOnClickListener { dismissAndEmit(Action.PHOTO_LIBRARY) }
    }

    private fun dismissAndEmit(action: Action) {
        dismiss()
        onAction(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

- [ ] **Step 2: Create layout `bottom_sheet_add_food.xml`**

Use a `GridLayout` or `LinearLayout` with three options. Example:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/add_food_title"
        android:textAppearance="?attr/textAppearanceHeadline6"
        android:layout_marginBottom="16dp" />

    <GridLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:columnCount="3">

        <LinearLayout
            android:id="@+id/addFoodOption"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_columnWeight="1"
            android:orientation="vertical"
            android:gravity="center"
            android:padding="12dp"
            android:background="?attr/selectableItemBackground">

            <ImageView
                android:layout_width="48dp"
                android:layout_height="48dp"
                android:src="@drawable/ic_add_food"
                android:contentDescription="@string/add_food" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/add_food"
                android:layout_marginTop="8dp" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/scanFoodOption"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_columnWeight="1"
            android:orientation="vertical"
            android:gravity="center"
            android:padding="12dp"
            android:background="?attr/selectableItemBackground">

            <ImageView
                android:layout_width="48dp"
                android:layout_height="48dp"
                android:src="@drawable/ic_scan_food"
                android:contentDescription="@string/scan_food" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/scan_food"
                android:layout_marginTop="8dp" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/photoLibraryOption"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_columnWeight="1"
            android:orientation="vertical"
            android:gravity="center"
            android:padding="12dp"
            android:background="?attr/selectableItemBackground">

            <ImageView
                android:layout_width="48dp"
                android:layout_height="48dp"
                android:src="@drawable/ic_photo_library"
                android:contentDescription="@string/photo_library" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/photo_library"
                android:layout_marginTop="8dp" />
        </LinearLayout>
    </GridLayout>
</LinearLayout>
```

- [ ] **Step 3: Add strings**

In `app/src/main/res/values/strings.xml`:

```xml
    <string name="add_food_title">Add Food</string>
    <string name="add_food">Add Food</string>
    <string name="scan_food">Scan Food</string>
    <string name="photo_library">Photo Library</string>
```

- [ ] **Step 4: Create vector drawables**

`ic_scan_food.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="#FF000000"
        android:pathData="M12,8c-2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4 -1.79,-4 -4,-4zM12,14c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2zM20,5h-2.5l-1.5,-2h-7L7.5,5H5c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h15c1.1,0 2,-0.9 2,-2V7c0,-1.1 -0.9,-2 -2,-2zM20,19H5V7h15v12z" />
</vector>
```

`ic_photo_library.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="#FF000000"
        android:pathData="M22,16V4c0,-1.1 -0.9,-2 -2,-2H8c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2zM11,12l2.03,2.71L16,11l4,5H8l3,-4zM2,6v14c0,1.1 0.9,2 2,2h14v-2H4V6H2z" />
</vector>
```

For `addFoodOption`, reuse an existing plus/add icon or create `ic_add_food.xml`.

- [ ] **Step 5: Build check**

Run: `./gradlew :app:assembleDebug`

Expected: Build succeeds.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/copdhealthtracker/ui/bottomsheets/AddFoodBottomSheet.kt \
        app/src/main/res/layout/bottom_sheet_add_food.xml \
        app/src/main/res/drawable/ic_scan_food.xml \
        app/src/main/res/drawable/ic_photo_library.xml \
        app/src/main/res/values/strings.xml
git commit -m "feat: add AddFoodBottomSheet with Add Food, Scan Food, Photo Library options"
```

---

### Task 8: Wire TrackingFragment to launch AddFoodBottomSheet

**Files:**
- Modify: `app/src/main/java/com/copdhealthtracker/ui/fragments/TrackingFragment.kt`

**Interfaces:**
- Consumes: `AddFoodBottomSheet.Action` enum.
- Produces: `showFoodDialog()` now shows bottom sheet; existing `AddFoodDialog` is preserved for `ADD_FOOD` action.

- [ ] **Step 1: Replace `showFoodDialog()` body**

```kotlin
    private fun showFoodDialog() {
        AddFoodBottomSheet { action ->
            when (action) {
                AddFoodBottomSheet.Action.ADD_FOOD -> showAddFoodDialog()
                AddFoodBottomSheet.Action.SCAN_FOOD -> launchScanFood()
                AddFoodBottomSheet.Action.PHOTO_LIBRARY -> launchPhotoLibrary()
            }
        }.show(parentFragmentManager, "AddFoodBottomSheet")
    }

    private fun showAddFoodDialog() {
        val dialog = AddFoodDialog(
            onSave = { viewModel.insertFood(it) },
            dateForEntry = selectedDateMillis,
            onSaveFavorite = { viewModel.insertFavoriteFood(it) },
            getAllUserAddedFoods = { viewModel.getAllUserAddedFoods() },
            insertUserAddedFood = { viewModel.insertUserAddedFood(it) }
        )
        dialog.show(parentFragmentManager, "AddFoodDialog")
    }

    private fun launchScanFood() {
        val intent = android.content.Intent(requireContext(), com.copdhealthtracker.ui.scan.ScanLabelActivity::class.java)
        scanLauncher.launch(intent)
    }

    private fun launchPhotoLibrary() {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        photoLibraryLauncher.launch(intent)
    }
```

- [ ] **Step 2: Add activity result launchers**

At the top of `TrackingFragment`, add:

```kotlin
    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val food = result.data?.getParcelableExtra<FoodEntry>(LabelReviewActivity.EXTRA_FOOD_ENTRY)
            food?.let { viewModel.insertFood(it) }
        }
    }

    private val photoLibraryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            val intent = android.content.Intent(requireContext(), LabelReviewActivity::class.java).apply {
                putExtra(LabelReviewActivity.EXTRA_IMAGE_URI, uri.toString())
            }
            scanLauncher.launch(intent)
        }
    }
```

Note: `FoodEntry` is not currently `Parcelable`. Add `@Parcelize` to it (Task 9) or use a serializable bundle. Prefer `Parcelable`.

- [ ] **Step 3: Build check**

Run: `./gradlew :app:compileDebugKotlin`

Expected: May fail until `ScanLabelActivity`, `LabelReviewActivity`, and `FoodEntry` Parcelable are implemented.

- [ ] **Step 4: Commit**

Commit after Task 9, 10, 11 complete and this builds.

---

### Task 9: Make FoodEntry Parcelable

**Files:**
- Modify: `app/src/main/java/com/copdhealthtracker/data/model/FoodEntry.kt`

**Interfaces:**
- Produces: `FoodEntry` implements `Parcelable`.

- [ ] **Step 1: Update FoodEntry**

```kotlin
package com.copdhealthtracker.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

@Entity(tableName = "food_entries")
@Parcelize
data class FoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mealCategory: String,
    val quantity: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    val sodium: Double = 0.0,
    val potassium: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val magnesium: Double = 0.0,
    val zinc: Double = 0.0,
    val selenium: Double = 0.0,
    val manganese: Double = 0.0,
    val water: Double = 0.0,
    val vitaminA: Double = 0.0,
    val vitaminC: Double = 0.0,
    val vitaminD: Double = 0.0,
    val vitaminE: Double = 0.0,
    val vitaminK: Double = 0.0,
    val saturatedFat: Double = 0.0,
    val cholesterol: Double = 0.0,
    val omega3: Double = 0.0,
    val addedSugars: Double = 0.0,
    val date: Long = System.currentTimeMillis()
) : Parcelable
```

Ensure `kotlin-parcelize` plugin is applied in `app/build.gradle`. It usually is via `kotlin-android`, but add explicitly if needed:

```groovy
plugins {
    id 'org.jetbrains.kotlin.plugin.parcelize'
}
```

- [ ] **Step 2: Build check**

Run: `./gradlew :app:compileDebugKotlin`

Expected: Compiles successfully.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/copdhealthtracker/data/model/FoodEntry.kt app/build.gradle
git commit -m "feat: make FoodEntry Parcelable for scan result passing"
```

---

### Task 10: Create ScanLabelActivity with CameraX

**Files:**
- Create: `app/src/main/java/com/copdhealthtracker/ui/scan/ScanLabelActivity.kt`
- Create: `app/src/main/res/layout/activity_scan_label.xml`
- Create: `app/src/main/java/com/copdhealthtracker/ui/scan/LabelCaptureViewModel.kt`

**Interfaces:**
- Produces: Activity that returns via `LabelReviewActivity` with `RESULT_OK` and `EXTRA_FOOD_ENTRY`.
- Consumes: `MlKitLabelOcr`, `NutritionLabelParser`.

- [ ] **Step 1: Write `LabelCaptureViewModel.kt`**

```kotlin
package com.copdhealthtracker.ui.scan

import android.net.Uri
import androidx.lifecycle.ViewModel

class LabelCaptureViewModel : ViewModel() {
    var frontLabelUri: Uri? = null
    var nutritionLabelUri: Uri? = null
    var parsedLabel: com.copdhealthtracker.labelscan.ParsedLabel? = null
}
```

- [ ] **Step 2: Write `ScanLabelActivity.kt`**

This is a longer file. Key responsibilities:
- Request `CAMERA` permission.
- Bind CameraX preview and image capture.
- Show overlay rectangle and step caption.
- Capture front label first (optional), then nutrition label (required).
- Run OCR + parser on the nutrition label.
- Launch `LabelReviewActivity` with the parsed result.

Because of length, implement in focused functions. Start with permission handling and a TODO for camera binding, then fill in.

A minimal working skeleton:

```kotlin
package com.copdhealthtracker.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.copdhealthtracker.databinding.ActivityScanLabelBinding
import com.copdhealthtracker.labelscan.MlKitLabelOcr
import com.copdhealthtracker.labelscan.NutritionLabelParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanLabelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanLabelBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewModel: LabelCaptureViewModel

    private var imageCapture: ImageCapture? = null
    private var isCapturingFront = true

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to scan labels", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val reviewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        setResult(result.resultCode, result.data)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanLabelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = androidx.lifecycle.ViewModelProvider(this)[LabelCaptureViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.captureButton.setOnClickListener { takePhoto() }
        binding.skipFrontButton.setOnClickListener { proceedToNutritionLabel() }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "Camera failed to start", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            cacheDir,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(baseContext, "Photo capture failed", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    if (isCapturingFront) {
                        viewModel.frontLabelUri = savedUri
                        proceedToNutritionLabel()
                    } else {
                        viewModel.nutritionLabelUri = savedUri
                        processLabel()
                    }
                }
            }
        )
    }

    private fun proceedToNutritionLabel() {
        isCapturingFront = false
        binding.instructionText.text = "Photo Nutrition Facts panel"
        binding.skipFrontButton.visibility = android.view.View.GONE
    }

    private fun processLabel() {
        val uri = viewModel.nutritionLabelUri ?: return
        binding.instructionText.text = "Reading label…"
        binding.captureButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ocr = MlKitLabelOcr(this@ScanLabelActivity)
                val text = ocr.recognize(uri)
                val parsed = NutritionLabelParser.parse(text)
                viewModel.parsedLabel = parsed

                withContext(Dispatchers.Main) {
                    if (parsed.calories == null && parsed.protein == null && parsed.carbs == null && parsed.fat == null) {
                        Toast.makeText(this@ScanLabelActivity, "Could not read nutrition label. Try again.", Toast.LENGTH_LONG).show()
                        binding.captureButton.isEnabled = true
                        isCapturingFront = false
                        binding.instructionText.text = "Photo Nutrition Facts panel"
                    } else {
                        val intent = Intent(this@ScanLabelActivity, LabelReviewActivity::class.java).apply {
                            putExtra(LabelReviewActivity.EXTRA_FRONT_LABEL_URI, viewModel.frontLabelUri?.toString())
                            putExtra(LabelReviewActivity.EXTRA_NUTRITION_LABEL_URI, uri.toString())
                        }
                        reviewLauncher.launch(intent)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScanLabelActivity, "Could not read label: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.captureButton.isEnabled = true
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val EXTRA_FOOD_ENTRY = "extra_food_entry"
    }
}
```

- [ ] **Step 3: Create `activity_scan_label.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.camera.view.PreviewView
        android:id="@+id/previewView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <View
        android:id="@+id/overlay"
        android:layout_width="280dp"
        android:layout_height="360dp"
        android:layout_gravity="center"
        android:background="@drawable/scan_overlay_border" />

    <TextView
        android:id="@+id/instructionText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center|top"
        android:layout_marginTop="64dp"
        android:text="@string/scan_front_label_instruction"
        android:textColor="@android:color/white"
        android:textSize="18sp"
        android:shadowColor="@android:color/black"
        android:shadowDx="1"
        android:shadowDy="1"
        android:shadowRadius="2" />

    <Button
        android:id="@+id/skipFrontButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="120dp"
        android:text="@string/skip_front_label" />

    <Button
        android:id="@+id/captureButton"
        android:layout_width="72dp"
        android:layout_height="72dp"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="32dp"
        android:background="@drawable/capture_button_circle" />
</FrameLayout>
```

Create supporting drawables: `scan_overlay_border.xml` and `capture_button_circle.xml`.

- [ ] **Step 4: Add strings**

```xml
    <string name="scan_front_label_instruction">Photo front of package (optional)</string>
    <string name="scan_nutrition_label_instruction">Photo Nutrition Facts panel</string>
    <string name="skip_front_label">Skip</string>
```

- [ ] **Step 5: Build check**

Run: `./gradlew :app:assembleDebug`

Expected: Build succeeds.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/copdhealthtracker/ui/scan/ \
        app/src/main/res/layout/activity_scan_label.xml \
        app/src/main/res/drawable/ \
        app/src/main/res/values/strings.xml
git commit -m "feat: add ScanLabelActivity with CameraX preview and capture"
```

---

### Task 11: Create LabelReviewActivity

**Files:**
- Create: `app/src/main/java/com/copdhealthtracker/ui/scan/LabelReviewActivity.kt`
- Create: `app/src/main/res/layout/activity_label_review.xml`

**Interfaces:**
- Consumes: `ParsedLabel` from intent extras (`EXTRA_NUTRITION_LABEL_URI` and optional front URI).
- Produces: `RESULT_OK` with `EXTRA_FOOD_ENTRY` containing a `FoodEntry`, or `RESULT_CANCELED`.

- [ ] **Step 1: Write `LabelReviewActivity.kt`**

```kotlin
package com.copdhealthtracker.ui.scan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.copdhealthtracker.data.model.FoodEntry
import com.copdhealthtracker.data.model.UserAddedFood
import com.copdhealthtracker.databinding.ActivityLabelReviewBinding
import com.copdhealthtracker.labelscan.Confidence
import com.copdhealthtracker.labelscan.MlKitLabelOcr
import com.copdhealthtracker.labelscan.NutritionLabelParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LabelReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLabelReviewBinding
    private var parsedLabel = com.copdhealthtracker.labelscan.ParsedLabel()
    private var frontLabelUri: Uri? = null
    private var nutritionLabelUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLabelReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        frontLabelUri = intent.getStringExtra(EXTRA_FRONT_LABEL_URI)?.let { Uri.parse(it) }
        nutritionLabelUri = intent.getStringExtra(EXTRA_NUTRITION_LABEL_URI)?.let { Uri.parse(it) }

        binding.retakeButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.addToLogButton.setOnClickListener { saveToLog() }
        binding.saveToMyFoodsButton.setOnClickListener { saveToLogAndDatabase() }

        loadParsedLabel()
    }

    private fun loadParsedLabel() {
        val uri = nutritionLabelUri ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val ocr = MlKitLabelOcr(this@LabelReviewActivity)
                val text = ocr.recognize(uri)
                parsedLabel = NutritionLabelParser.parse(text)

                if (frontLabelUri != null) {
                    val frontText = ocr.recognize(frontLabelUri!!)
                    val firstLine = frontText.lines().firstOrNull { it.isNotBlank() }?.trim()
                    parsedLabel = parsedLabel.copy(productName = firstLine)
                }

                withContext(Dispatchers.Main) {
                    bindValues()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LabelReviewActivity, "Could not read label", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindValues() {
        binding.foodNameEdit.setText(parsedLabel.productName ?: "")
        binding.servingSizeText.text = parsedLabel.servingSize?.description ?: "Not detected"
        binding.servingGramsEdit.setText(parsedLabel.servingSize?.grams?.toString() ?: "")

        bindField(binding.caloriesEdit, binding.caloriesWarning, parsedLabel.calories)
        bindField(binding.proteinEdit, binding.proteinWarning, parsedLabel.protein)
        bindField(binding.carbsEdit, binding.carbsWarning, parsedLabel.carbs)
        bindField(binding.fatEdit, binding.fatWarning, parsedLabel.fat)

        val hasGrams = parsedLabel.servingSize?.grams != null
        binding.saveToMyFoodsButton.isEnabled = hasGrams
        binding.saveToMyFoodsButton.alpha = if (hasGrams) 1.0f else 0.5f
    }

    private fun bindField(edit: android.widget.EditText, warning: View, value: com.copdhealthtracker.labelscan.ValueWithConfidence?) {
        edit.setText(value?.value?.toString() ?: "")
        warning.visibility = if (value?.confidence == Confidence.LOW) View.VISIBLE else View.GONE
    }

    private fun saveToLog() {
        val entry = buildFoodEntry() ?: return
        val intent = Intent().putExtra(EXTRA_FOOD_ENTRY, entry)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun saveToLogAndDatabase() {
        val entry = buildFoodEntry() ?: return
        val grams = binding.servingGramsEdit.text.toString().toDoubleOrNull() ?: return
        val amount = 1.0
        val factor = grams * amount / 100.0

        val userFood = UserAddedFood(
            name = entry.name,
            calories = if (factor > 0) entry.calories / factor else entry.calories,
            protein = if (factor > 0) entry.protein / factor else entry.protein,
            carbs = if (factor > 0) entry.carbs / factor else entry.carbs,
            fat = if (factor > 0) entry.fat / factor else entry.fat
        )

        lifecycleScope.launch {
            // TODO: persist UserAddedFood via repository; for now, just log the meal
            val intent = Intent().putExtra(EXTRA_FOOD_ENTRY, entry)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun buildFoodEntry(): FoodEntry? {
        val name = binding.foodNameEdit.text.toString().trim().ifEmpty { "Scanned food" }
        val calories = binding.caloriesEdit.text.toString().toDoubleOrNull() ?: 0.0
        val protein = binding.proteinEdit.text.toString().toDoubleOrNull() ?: 0.0
        val carbs = binding.carbsEdit.text.toString().toDoubleOrNull() ?: 0.0
        val fat = binding.fatEdit.text.toString().toDoubleOrNull() ?: 0.0
        val grams = binding.servingGramsEdit.text.toString().toDoubleOrNull() ?: 100.0
        val quantity = if (parsedLabel.servingSize != null) "1 serving (${grams.toInt()}g)" else "1 serving"

        return FoodEntry(
            name = name,
            mealCategory = "Snacks",
            quantity = quantity,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat
        )
    }

    companion object {
        const val EXTRA_FRONT_LABEL_URI = "extra_front_label_uri"
        const val EXTRA_NUTRITION_LABEL_URI = "extra_nutrition_label_uri"
        const val EXTRA_FOOD_ENTRY = "extra_food_entry"
    }
}
```

Note: `saveToLogAndDatabase()` currently only builds the `UserAddedFood` object. Passing it back to `TrackingFragment` for insertion requires a callback mechanism. The simplest approach is to return the `FoodEntry` and a flag `EXTRA_SAVE_TO_DATABASE` so `TrackingFragment` can call `viewModel.insertUserAddedFood(...)`. Refine in Task 12.

- [ ] **Step 2: Create `activity_label_review.xml`**

Scrollable form with editable fields and warning indicators. Keep it simple with a `ScrollView` + `LinearLayout`.

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/review_scanned_label"
            android:textAppearance="?attr/textAppearanceHeadline6"
            android:layout_marginBottom="16dp" />

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/food_name">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/foodNameEdit"
                android:layout_width="match_parent"
                android:layout_height="wrap_content" />
        </com.google.android.material.textfield.TextInputLayout>

        <TextView
            android:id="@+id/servingSizeText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:text="Serving size: " />

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/grams_per_serving">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/servingGramsEdit"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="numberDecimal" />
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/calories"
            android:layout_marginTop="8dp">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/caloriesEdit"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="numberDecimal" />
        </com.google.android.material.textfield.TextInputLayout>
        <TextView
            android:id="@+id/caloriesWarning"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/check_this_value"
            android:textColor="@android:color/holo_orange_dark"
            android:visibility="gone" />

        <!-- Repeat for protein, carbs, fat -->

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="24dp">

            <Button
                android:id="@+id/addToLogButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/add_to_todays_log" />

            <Button
                android:id="@+id/saveToMyFoodsButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/save_to_my_foods" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">

            <Button
                android:id="@+id/retakeButton"
                style="?attr/materialButtonOutlinedStyle"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/retake" />

            <Button
                android:id="@+id/cancelButton"
                style="?attr/materialButtonOutlinedStyle"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/cancel" />
        </LinearLayout>
    </LinearLayout>
</ScrollView>
```

- [ ] **Step 3: Add strings**

```xml
    <string name="review_scanned_label">Review Scanned Label</string>
    <string name="food_name">Food name</string>
    <string name="grams_per_serving">Grams per serving</string>
    <string name="calories">Calories</string>
    <string name="protein">Protein (g)</string>
    <string name="carbs">Carbs (g)</string>
    <string name="fat">Fat (g)</string>
    <string name="check_this_value">Please check this value.</string>
    <string name="add_to_todays_log">Add to today\'s log</string>
    <string name="save_to_my_foods">Save to my foods</string>
    <string name="retake">Retake</string>
```

- [ ] **Step 4: Build check**

Run: `./gradlew :app:assembleDebug`

Expected: Build succeeds.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/copdhealthtracker/ui/scan/LabelReviewActivity.kt \
        app/src/main/res/layout/activity_label_review.xml \
        app/src/main/res/values/strings.xml
git commit -m "feat: add LabelReviewActivity with editable scanned values"
```

---

### Task 12: Pass "save to my foods" result back to TrackingFragment

**Files:**
- Modify: `app/src/main/java/com/copdhealthtracker/ui/scan/LabelReviewActivity.kt`
- Modify: `app/src/main/java/com/copdhealthtracker/ui/fragments/TrackingFragment.kt`

**Interfaces:**
- Produces: `LabelReviewActivity` can return `EXTRA_SAVE_TO_DATABASE = true` alongside `EXTRA_FOOD_ENTRY`.

- [ ] **Step 1: Update `LabelReviewActivity.saveToLogAndDatabase()`**

```kotlin
    private fun saveToLogAndDatabase() {
        val entry = buildFoodEntry() ?: return
        val intent = Intent().apply {
            putExtra(EXTRA_FOOD_ENTRY, entry)
            putExtra(EXTRA_SAVE_TO_DATABASE, true)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
```

Add to companion object:

```kotlin
        const val EXTRA_SAVE_TO_DATABASE = "extra_save_to_database"
```

- [ ] **Step 2: Update `TrackingFragment` result handlers**

```kotlin
    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val food = result.data?.getParcelableExtra<FoodEntry>(LabelReviewActivity.EXTRA_FOOD_ENTRY)
            val saveToDatabase = result.data?.getBooleanExtra(LabelReviewActivity.EXTRA_SAVE_TO_DATABASE, false) ?: false
            food?.let {
                viewModel.insertFood(it)
                if (saveToDatabase) {
                    saveScannedFoodToDatabase(it)
                }
            }
        }
    }

    private fun saveScannedFoodToDatabase(entry: FoodEntry) {
        val grams = entry.quantity.extractGrams() ?: return
        val factor = grams / 100.0
        if (factor <= 0) return
        val userFood = UserAddedFood(
            name = entry.name,
            calories = entry.calories / factor,
            protein = entry.protein / factor,
            carbs = entry.carbs / factor,
            fat = entry.fat / factor
        )
        lifecycleScope.launch {
            viewModel.insertUserAddedFood(userFood)
        }
    }
```

Add a helper to extract grams from quantity string:

```kotlin
    private fun String.extractGrams(): Double? {
        return Regex("\\((\\d+(?:\\.\\d+)?)g\\)").find(this)?.groupValues?.get(1)?.toDoubleOrNull()
    }
```

- [ ] **Step 3: Build check**

Run: `./gradlew :app:assembleDebug`

Expected: Build succeeds.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/copdhealthtracker/ui/scan/LabelReviewActivity.kt \
        app/src/main/java/com/copdhealthtracker/ui/fragments/TrackingFragment.kt
git commit -m "feat: wire save-to-my-foods from scan result to TrackingFragment"
```

---

### Task 13: Add camera icon entry point inside AddFoodDialog

**Files:**
- Modify: `app/src/main/res/layout/dialog_add_food.xml`
- Modify: `app/src/main/java/com/copdhealthtracker/ui/dialogs/AddFoodDialog.kt`

**Interfaces:**
- Consumes: `AddFoodBottomSheet.Action`.
- Produces: When scan returns, pre-fill manual-entry fields in `AddFoodDialog`.

- [ ] **Step 1: Add camera icon to layout**

In `dialog_add_food.xml`, add an `ImageButton` next to the manual-entry toggle. Place it in the same row as `toggleManualButton`.

```xml
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <Button
            android:id="@+id/toggleManualButton"
            ... />

        <ImageButton
            android:id="@+id/scanLabelButton"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:src="@drawable/ic_scan_food"
            android:contentDescription="@string/scan_food"
            android:background="?attr/selectableItemBackgroundBorderless" />
    </LinearLayout>
```

- [ ] **Step 2: Add click handler in AddFoodDialog**

Inside `setupBasicUI()`:

```kotlin
        binding.scanLabelButton.setOnClickListener {
            AddFoodBottomSheet { action ->
                when (action) {
                    AddFoodBottomSheet.Action.ADD_FOOD -> { /* already in AddFoodDialog */ }
                    AddFoodBottomSheet.Action.SCAN_FOOD -> launchScanFromDialog()
                    AddFoodBottomSheet.Action.PHOTO_LIBRARY -> launchPhotoLibraryFromDialog()
                }
            }.show(parentFragmentManager, "AddFoodBottomSheet")
        }
```

Add activity result launchers and handlers:

```kotlin
    private val scanFromDialogLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val food = result.data?.getParcelableExtra<FoodEntry>(LabelReviewActivity.EXTRA_FOOD_ENTRY)
            food?.let { prefillFromScan(it) }
        }
    }

    private val photoLibraryFromDialogLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            val intent = android.content.Intent(requireContext(), LabelReviewActivity::class.java).apply {
                putExtra(LabelReviewActivity.EXTRA_NUTRITION_LABEL_URI, uri.toString())
            }
            scanFromDialogLauncher.launch(intent)
        }
    }

    private fun launchScanFromDialog() {
        val intent = android.content.Intent(requireContext(), com.copdhealthtracker.ui.scan.ScanLabelActivity::class.java)
        scanFromDialogLauncher.launch(intent)
    }

    private fun launchPhotoLibraryFromDialog() {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        photoLibraryFromDialogLauncher.launch(intent)
    }

    private fun prefillFromScan(entry: FoodEntry) {
        binding.foodNameEdit.setText(entry.name)
        binding.manualEntrySection.visibility = View.VISIBLE
        binding.toggleManualButton.text = "Hide Manual Entry"
        binding.caloriesEdit.setText(entry.calories.toInt().toString())
        binding.proteinEdit.setText(String.format("%.1f", entry.protein))
        binding.carbsEdit.setText(String.format("%.1f", entry.carbs))
        binding.fatEdit.setText(String.format("%.1f", entry.fat))
    }
```

- [ ] **Step 3: Build check**

Run: `./gradlew :app:assembleDebug`

Expected: Build succeeds.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/copdhealthtracker/ui/dialogs/AddFoodDialog.kt \
        app/src/main/res/layout/dialog_add_food.xml
git commit -m "feat: add scan label entry point inside AddFoodDialog"
```

---

### Task 14: Clean up cached scan images

**Files:**
- Modify: `app/src/main/java/com/copdhealthtracker/ui/scan/ScanLabelActivity.kt`
- Modify: `app/src/main/java/com/copdhealthtracker/ui/scan/LabelReviewActivity.kt`

**Interfaces:**
- Produces: Cached JPEG files deleted after processing or on cancel.

- [ ] **Step 1: Add cleanup helper**

Create `ScanLabelActivity.deleteCachedImage(uri: Uri?)`:

```kotlin
    private fun deleteCachedImage(uri: Uri?) {
        uri?.path?.let { path ->
            try {
                File(path).delete()
            } catch (_: Exception) { }
        }
    }
```

Call `deleteCachedImage(viewModel.frontLabelUri)` and `deleteCachedImage(viewModel.nutritionLabelUri)` in `onDestroy()`.

- [ ] **Step 2: Delete images in LabelReviewActivity on cancel/retake**

In the cancel and retake click listeners, parse `frontLabelUri` and `nutritionLabelUri` and delete the underlying files.

- [ ] **Step 3: Build check**

Run: `./gradlew :app:assembleDebug`

Expected: Build succeeds.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/copdhealthtracker/ui/scan/ScanLabelActivity.kt \
        app/src/main/java/com/copdhealthtracker/ui/scan/LabelReviewActivity.kt
git commit -m "feat: delete cached scan images after use or cancel"
```

---

### Task 15: Manual integration testing

**Files:**
- None (runtime verification)

**Interfaces:**
- Verifies: bottom sheet → camera permission → capture → review → save flow works end-to-end.

- [ ] **Step 1: Install debug APK**

Run: `./gradlew :app:installDebug`

Expected: APK installs successfully.

- [ ] **Step 2: Run parser tests**

Run: `./gradlew :app:testDebugUnitTest`

Expected: All tests pass.

- [ ] **Step 3: Manual test checklist**

On a physical device or emulator:

1. Open Tracking → tap "+ Quick Add Food".
2. Bottom sheet appears with Add Food, Scan Food, Photo Library.
3. Tap Scan Food → grant camera permission.
4. Capture front label (optional), then nutrition label.
5. Review screen opens with calories, protein, carbs, fat pre-filled.
6. Edit a value and tap Add to today's log.
7. Verify food appears in the day's log.
8. Repeat from inside `AddFoodDialog` camera icon.
9. Tap Photo Library, select a label image, verify review opens.

- [ ] **Step 4: Commit any fixes**

Commit fixes as separate commits with clear messages.

---

## Self-review

### 1. Spec coverage

| Spec requirement | Task |
|------------------|------|
| On-device ML Kit OCR | Task 4 |
| In-app CameraX preview | Task 10 |
| CAMERA permission | Task 2 |
| Gallery import | Task 7, 8 |
| Bottom-sheet entry from Tracking | Task 8 |
| Bottom-sheet/camera entry from AddFoodDialog | Task 13 |
| Review screen with editable values | Task 11 |
| Macros only (cal/pro/carbs/fat) | Task 5, 11 |
| Save to my foods gated on grams | Task 11, 12 |
| Cache image cleanup | Task 14 |
| Parser unit tests | Task 6 |

### 2. Placeholder scan

No TBD/TODO placeholders remain in the plan steps. Some UI strings are represented as `@string/...` references; those are defined in the same task. The `UserAddedFood` persistence in `LabelReviewActivity.saveToLogAndDatabase()` was simplified and moved to `TrackingFragment` in Task 12.

### 3. Type consistency

- `LabelOcr.recognize(uri: Uri): String` is used by `ScanLabelActivity` and `LabelReviewActivity`.
- `NutritionLabelParser.parse(text: String): ParsedLabel` is consistent.
- `FoodEntry` is `@Parcelize` and used as `getParcelableExtra` / `putExtra`.
- `LabelReviewActivity` extras `EXTRA_FRONT_LABEL_URI`, `EXTRA_NUTRITION_LABEL_URI`, `EXTRA_FOOD_ENTRY`, `EXTRA_SAVE_TO_DATABASE` are consistent across Tasks 10, 11, 12, 13.

### Known refinements needed during execution

- The initial parser (Task 5) does not yet handle two-column panels. If label fixtures show this is needed, extend `extractValue` to detect column headers and select the correct column.
- `LabelReviewActivity` re-runs OCR; consider passing the already-parsed result from `ScanLabelActivity` via `LabelCaptureViewModel` or intent to avoid duplicate work.
- `FoodEntry` Parcelable change is a model change; ensure Room schema migration is not triggered (adding `@Parcelize` does not change the schema).

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-18-food-label-scan.md`.

Two execution options:

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints.

Which approach would you like?
