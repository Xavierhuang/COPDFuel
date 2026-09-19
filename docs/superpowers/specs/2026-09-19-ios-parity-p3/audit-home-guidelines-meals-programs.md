# App shell, Home, Guidelines, Meals, Programs Near Me — Android → iOS gap list (audit 2026-09-19)

Android: `android/app/src/main/` — `AndroidManifest.xml`, `java/com/copdhealthtracker/{MainActivity,SplashActivity}.kt`, `ui/fragments/{HomeFragment,GuidelinesFragment,RecipesFragment,ProgramsNearMeFragment}.kt`, `res/layout/{activity_main,activity_splash,fragment_home,fragment_guidelines,fragment_recipes,fragment_programs_near_me,item_guideline,item_strategy,item_food,item_symptom,item_program_card}.xml`, `res/menu/bottom_navigation.xml`, `res/values/{strings,colors,styles}.xml`.
iOS: `COPDFuel/COPDFuel/` — `COPDFuelApp.swift`, `MainTabView.swift`, `Views/{RootView,HomeView,GuidelinesView,RecipesView,ProgramsNearMeView}.swift`, `COPDFuel.xcodeproj/project.pbxproj`.

None of these Android screens reference `@string` — all copy is hard-coded in layouts/Kotlin. `app_name` = "COPD Fuel"; iOS `CFBundleDisplayName` matches.

---

# A. App shell & navigation

## A1. Splash screen — DIFFERS
Android `SplashActivity.kt:19-24` + `activity_splash.xml` + `styles.xml:8-14` + `drawable/splash_gradient.xml`:
- Fixed 2000 ms delay, then Main if signed in, else Login.
- Background: linear gradient angle 0, `#3b82f6` → `#2dd4bf`.
- Centered 120dp launcher icon (contentDescription "COPD Fuel Logo"), 24dp below it:
- "COPD Fuel" 32sp bold, white
- "Breathe Easier with Better Nutrition" 18sp white, alpha 0.9

iOS `RootView.swift:39-64` (`splashContent`): no minimum display time; gradient `#ffffff` → `#f8fafc`; no logo (unused `Assets.xcassets/splashscreen_logo.imageset` exists); two-tone "COPD"/" Fuel" at 56pt; subtitle "Your COPD Health Companion"; extra spinner.

## A2. Launch/auth gating — matches
Both gate post-login register+sync on HIPAA consent; iOS also re-runs sync when consent flips to signed (keep).

## A3. Bottom tab bar
Android `activity_main.xml` custom bar (menu xml is dead):

| # | Android label | Android icon | iOS label (`MainTabView.swift`) | iOS symbol |
|---|---|---|---|---|
| 0 | Home | 🏠 | Home | house.fill |
| 1 | Guidelines | 📚 | Guidelines | book.fill |
| 2 | Tracking | 📊 | Tracking | chart.pie.fill |
| 3 | **Meals** | 🍛 | **"Recipes"** | heart.fill |
| 4 | Resources | 🔧 | Resources | cube.fill |
| 5 | Profile | 👤 | Profile | person.fill |

- GAP: iOS tab 3 label must be "Meals". Symbol mapping for parity: 🍛 → `fork.knife`, 🔧 → `wrench.fill`, 📊 → `chart.bar.fill`.
- Active label `#2563eb` 12sp / inactive gray 11sp. iOS `.accentColor(.blue)`; use `Color(hex: 0x2563eb)`.

## A4. Cross-tab navigation hooks
- Home "Explore Guidelines" → tab 1: matches.
- `MainActivity.switchToProfile()` ← `TrackingFragment.kt:1594-1600` ("set up protein target"). iOS `TrackingView` pushes `ProfileView()` instead — GAP: pass the `selectedTab` binding into `TrackingView` and set 5.

## A5. Manifest / project settings
- Android locks every activity to portrait. iOS `project.pbxproj:297,336` allows all orientations — GAP: portrait only (`UIInterfaceOrientationPortrait`; iPad keeps portrait + upside down if iPad stays targeted).
- No `NSCameraUsageDescription` / `NSPhotoLibraryUsageDescription` yet (needed by scan feature).

---

# B. HOME tab

## B1. Brand header — matches text; Android "COPD" is bold **italic** `#f97316` 28sp, " Fuel" bold `#2563eb`, band `#dcfce7`. iOS 32pt, no italic.
## B2. Hero — title "Breathe Easier with Better Nutrition" (Android bold italic `#1e40af` 32sp; iOS heavy, no italic); subtitle verbatim; "Explore Guidelines" button matches.
## B3. "Understanding COPD" header — matches ("Learn about Chronic Obstructive Pulmonary Disease and its impact on health.").
## B4. Card "What is COPD?" — text matches; image `healthy_vs_copd_lungs` Android 200dp centerCrop in a white card radius 12, contentDescription "Healthy vs COPD lungs illustration". iOS 180pt, radius 8, no wrapper, no accessibility label. Asset hygiene: iOS imageset holds three identical "healthy_vs_copd_lungs 1/2/3.png" — keep one file named `healthy_vs_copd_lungs.png` and fix Contents.json.
## B5. Card "Common Symptoms" — 8 bullets match. Marker: Android plain 20dp `#bfdbfe` circle; iOS adds "•" glyph.
## B6. Card "Risk Factors" — MISSING on iOS. Title "Risk Factors"; body:
> Smoking, exposure to air pollution, genetics, and occupational dust or chemicals can increase the risk of developing COPD.
## B7. Card "When to See a Doctor" — MISSING. Body:
> See your doctor if you have persistent cough, shortness of breath, wheezing, or frequent respiratory infections. Early diagnosis and treatment can help manage COPD effectively.
## B8. Footer — MISSING. Dark band `#1f2937`, padding 48dp:
- "COPD Fuel" 28sp bold white
- "Helping you breathe easier with personalized nutritional guidance tailored for COPD management." 16sp `#d1d5db`, centered
- 1dp divider `#e5e7eb`
- "(c) 2024 COPD Fuel. All rights reserved. The information provided is not medical advice." 14sp `#9ca3af`, centered

---

# C. GUIDELINES tab

## C1. Structure — iOS extras
- Android: single scrolling page, no toolbar title, no tabs.
- iOS: `.navigationTitle("Guidelines")`; segmented "Dietary"/"Medication" picker; whole `medicationSection` + `MedicationCategory` detail views (`:44-136, 432-734`) — **remove** (medication content belongs to Resources per Android).
- iOS `DietaryReferencesPanel` rendered twice (`:156`, `:241`) — keep **once** at the bottom (App Review 1.4.1).
- iOS extra sentence `:149` "The points below summarize common guidance…" — remove.

## C2. Section order — Android: title+intro → 6 guideline cards → **consult box** → Preventing COPD Exacerbations (blue band `#eff6ff`) → The Importance of Protein → Recommended Foods (Embrace, Limit). iOS puts consult text last — reorder.

## C3. Header — "COPD Dietary Guidelines" 28sp bold `#1f2937` **centered**; intro "Proper nutrition plays a vital role in managing COPD symptoms and improving overall health." 16sp centered.
## C4. Six guideline cards — text matches. Style: `#f8fafc` card radius 12 with **4dp `#2563eb` left accent bar**, title 18sp bold, body 14sp.
## C5. Consult notice — "Always consult with your healthcare provider before making significant changes to your diet." 14sp italic `#6b7280`, centered, in a yellow `#fef3c7` rounded 8dp box, right after the 6 guidelines.
## C6. "Preventing COPD Exacerbations" — text matches; blue `#eff6ff` band, title 24sp bold `#1e40af`; strategies as white cards with 1dp `#e0e7ff` stroke, title 16sp bold `#1e40af`.
## C7. "The Importance of Protein" — matches; title `#1e40af`.
## C8. "Recommended Foods for COPD" — text matches. Sub-headings both 18sp bold `#1f2937` (no green/red coloring). Markers: "+" in `#2563eb`, "-" (ASCII hyphen) in `#cc0000`, 20sp bold, rows without background.

---

# D. MEALS tab

## D1. iOS nav title "Recipes" → no nav title on Android (if kept, "Meals"). Title "COPD-Friendly Meals" (24sp bold `#2563eb`). Subtitle "Nutritious and delicious meal ideas that are easy to prepare and gentle on your respiratory system." (14sp `#4b5563`).
## D2. Sections — headings match ("Breakfast Ideas", "Lunch & Dinner Ideas", "Snack Ideas"); each heading sits inside a `#eff6ff` rounded-12 card, 18sp bold `#2563eb`.
## D3. Items — all 17 titles, numbering, bullets, image names match. Item card `#f8fafc` radius 8, padding 16/12, image 160dp centerCrop inset inside padding (square corners), title "N. Title" 15sp bold `#1f2937`, bullets "  • text" 14sp `#4b5563`.
## D4. Dead Android code (`RecipeAdapter`, `Recipe.kt`, `item_recipe*.xml`) — nothing to port.

---

# E. PROGRAMS NEAR ME

## E1. Entry / back — matches (push from Resources, system back).
## E2. Header title "Find Programs Near Me" — matches.
## E3. Page intro — MISSING: "Pulmonary Rehabilitation Programs" (24sp bold, centered) + "Find programs near you to help manage your COPD" (16sp `#4b5563`, centered).
## E4. "Choose Your Location" block — MISSING (iOS has a different query field):
- Heading "Choose Your Location" (18sp bold, centered).
- Option card "Use Current Location" (`#f8fafc` radius 12, 2dp `#e5e7eb` stroke; location icon `#2563eb`): subtitle cycles "Tap to get your current location" → "Getting your location..." → "Location found" (green check) / "Could not get location" / "Error getting location". Tap: get location, or request permission.
- Option card "Search by Address" (search icon): subtitle "Enter any city, state, or address" → "Searching..." → geocoded formatted address (+ green check) on success; reset on failure. Display-only row.
- Manual input band (`#fef3c7`): text field hint "Enter city, state, or address..." + button "Search" (white on `#f59e0b`). Empty → toast "Please enter a location".
- Status band (`#eff6ff`, location icon): "Location permission granted" / "Location permission denied. Use manual search." (14sp bold `#1e40af`).
- Remove the iOS-only "Search nearby (e.g., pulmonary rehab)" query field; MKLocalSearch uses the fixed query "pulmonary rehabilitation" (and "hospital" as fallback) near the resolved location.
## E5. Results filter — hint "Search by name, city, or specialty..." with leading search icon; filters name / city / specialty.
## E6. Loading state — MISSING: spinner + "Searching for programs near you..." while searching; list cleared during load.
## E7. Results count "N program(s) found" — matches.
## E8. Program card (`item_program_card.xml`):
- Name.
- Stars: only `floor(rating)` filled `#fbbf24` stars, then rating "%.1f" 16sp bold, then distance prefixed "- " (e.g. "- 2.3 miles").
- Distance: sample data uses fixed strings "2.3 miles", "3.7 miles", "5.1 miles", "8.2 miles", "12.5 miles"; live results haversine → "###m" or "%.1f km". iOS must show sample distances even without location.
- Address row with location icon.
- Phone row (phone icon + number) — MISSING as a row.
- Hours row (clock icon) always shown ("Hours available" / "Hours not available" for live results).
- "Specialties:" label — MISSING. Tags `#dbeafe` pill radius 20, text `#1e40af` 14sp. Live-result specialties `["Hospital", "Healthcare"]`.
- Buttons full-width pair "Call" (green `#10b981`, phone icon) and "Directions" (blue `#2563eb`). Missing phone: text "Phone not available" and toast "Phone number not available" on tap.
## E9. Empty state — "No programs found." (18sp), "Try adjusting your search terms." (16sp), red button "Show Sample Data" (`#dc2626`), shown when the filtered list is empty. Remove iOS's always-visible "Sample" button.
## E10. Footer — MISSING: "Don't see a program near you? Contact your healthcare provider for recommendations." (14sp centered, gray band).
## E11. Map — iOS-only extra (240pt MapKit map). Remove for parity.
## E12. Dialogs — Call: "Call Program" / "Would you like to call {name} at {phone}?" / "Call"/"Cancel" matches. Directions: "Get Directions" / "Would you like to get directions to {name}?" — positive button "Google Maps" on Android → iOS "Maps" (Apple Maps; platform equivalent).
## E13. Search/fallback behavior:
- Sample-data fallback on: permission denied, location null/failure, geocode not found / error, empty or failed search.
- Toasts: "Please enter a location", "Location not found", "Could not search: {msg}", "Could not find location", "Error: {msg}", "Phone number not available". (Skip the Google-API-specific "Address search not available…" message.)
- On-appear permission check/request: matches.
- Sample programs: all 5 (City General Hospital Pulmonary Rehabilitation, Bay Area Respiratory Care Center, Golden Gate Pulmonary Clinic, Community Health Center - Pulmonary Program, Stanford Pulmonary Rehabilitation) match verbatim in iOS already.
