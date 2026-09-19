# iOS Parity P3.0 (shell) + P3.A (Splash, Home, Guidelines, Meals) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land the first two P3 commits in the `COPDFuel` iOS repo: a checkpoint of the never-committed P1 work plus the shared `ToastCenter`, portrait lock and tab-bar fixes (P3.0), then verbatim Android parity for the splash screen, Home, Guidelines and Meals tabs (P3.A).

**Architecture:** Plain SwiftUI edits. One new service file (`Services/ToastCenter.swift`) provides the Android `Toast` equivalent that every later phase uses. `MainTabView` owns the tab selection and passes it to Home and Tracking. Home/Guidelines/Meals are content rewrites of three existing view files with no new state or persistence. The Xcode project uses synchronized folders, so new `.swift` files are picked up without editing `project.pbxproj`; only the orientation keys are edited there.

**Tech Stack:** Swift 5 / SwiftUI, iOS 17 deployment target, Xcode 16 `xcodebuild`, git. No test target exists in the app project; verification is the build plus copy greps (spec §4).

**Spec:** `docs/superpowers/specs/2026-09-19-ios-parity-p3-design.md` (§2.1, §2.2, §3 rows P3.0 and P3.A). Gap lists: `docs/superpowers/specs/2026-09-19-ios-parity-p3/audit-home-guidelines-meals-programs.md` §A1, A3, A4, A5, B, C, D.

## Global Constraints

- Android `main` commit `5c4aa49` is the source of truth; iOS copy, ordering and defaults match Android verbatim (spec §1.1).
- Platform idioms are not gaps: SF Symbols replace emoji tab icons; `Color(hex:)` values match Android hex where cheap; pixel-perfect Material is out of scope (spec §1.2).
- Keep the Guidelines "References & sources" panel, rendered **once** at the bottom (spec §1.4 keep list; App Store Review 1.4.1).
- Remove: the Guidelines "Dietary / Medication" segmented control and its whole Medication segment (spec §1.4 remove list).
- Each phase is one commit in the `COPDFuel` repo and must build first with:
  `xcodebuild -project COPDFuel/COPDFuel.xcodeproj -scheme COPDFuel -destination 'generic/platform=iOS Simulator' build` (spec §1.6, §4).
- All shell commands below run from the parent repo root `/Users/weijiahuang/Desktop/client's project/COPD-2` unless a `cd` is shown. The iOS repo is the nested git repo at `COPDFuel/` (no remote; commits stay local).
- Use `/usr/bin/git` for git commands (another agent session may be committing in the parent repo; do not touch the parent repo in this plan).

---

### Task 1: P3.0 checkpoint commit of the uncommitted P1 work

The iOS repo has a large uncommitted working tree from the P1.B-part-2 work (modified `TrackingView`, `ResourcesView`, `ProfileView`, `DailyTrackingSummary`, `HipaaAuthorizationView`, `COPDFuelApp`, deleted template files) **plus** files that were never added to git at all (`Models/*`, `Services/AuthService|COPDAPIClient|FoodDatabaseService|HealthKitService|ReportGenerator|SeverityCalculator|StoreManager.swift`, `Views/LoginView|PaywallView|SignaturePadView.swift`, `Config/AppConfig.swift`, `Resources/food_database.json`, `amplify_outputs.json`, `COPDFuel.entitlements`, `COPDFuel.storekit`, shared scheme, `Package.resolved`). Commit all of it as-is before changing anything. `amplify_outputs.json` holds only the Cognito region/pool IDs (public identifiers, same values Android ships in source); `AppConfig.swift` reads the USDA key from Info.plist and contains no key.

**Files:**
- Create: `COPDFuel/.gitignore` additions (file exists, untracked, currently one rule)
- Commit: everything else in `COPDFuel/` working tree

- [ ] **Step 1: Confirm no secrets are about to be committed**

Run:
```bash
cd "COPDFuel" && grep -rniE "api_key|apikey|secret|password" COPDFuel/Config/AppConfig.swift COPDFuel/amplify_outputs.json
```
Expected: only the `USDA_FDC_API_KEY` Info.plist *lookup* line in `AppConfig.swift` and the `password_policy` block (min length rules) in `amplify_outputs.json`. No literal key values. If a literal key appears, stop and report.

- [ ] **Step 2: Ignore editor/IDE junk**

Append to `COPDFuel/.gitignore` (keep the existing rule):
```
# Editor / IDE state
.cursor/
.lingcode/
xcuserdata/
```
Note: `COPDFuel.xcodeproj/xcuserdata/weijiahuang.xcuserdatad/...` is already tracked; leave it tracked (the ignore rule only stops new files). Do not add `amplify_outputs.json` to the ignore list; the app cannot build without it.

- [ ] **Step 3: Stage and inspect**

Run:
```bash
cd "COPDFuel" && /usr/bin/git add -A && /usr/bin/git status --short | grep -v '^[MADR] ' ; /usr/bin/git diff --cached --stat | tail -3
```
Expected: the first command prints nothing (no unstaged/untracked lines left); the stat line ends with roughly `40 files changed`, insertions in the thousands, and the deletions of `ContentView.swift`, `Item.swift`, `healthy_vs_copd_lungs.png`.

- [ ] **Step 4: Commit**

```bash
cd "COPDFuel" && /usr/bin/git commit -q -m "iOS parity P1.B (2/2) checkpoint: tracking, resources, profile, HIPAA work plus never-committed services, models, auth, paywall, config

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>" && /usr/bin/git log --oneline -1
```
Expected: one new commit on top of `417cd34`.

---

### Task 2: `ToastCenter` + `.toastOverlay()` (spec §2.1)

**Files:**
- Create: `COPDFuel/COPDFuel/Services/ToastCenter.swift`

**Interfaces:**
- Produces: `ToastCenter.shared.show(_ text: String)` (`@MainActor`), `View.toastOverlay()`. Every later phase calls `ToastCenter.shared.show("...")` wherever Android calls `Toast.makeText(...)`.

- [ ] **Step 1: Create the file**

```swift
//
//  ToastCenter.swift
//  COPDFuel
//
//  Android `Toast.LENGTH_SHORT` equivalent. One shared instance; `show`
//  replaces any toast currently on screen and auto-clears after 2.5 s.
//  Install once per window-level container with `.toastOverlay()`
//  (MainTabView, and full-screen covers that host their own UI).
//

import SwiftUI

@MainActor
final class ToastCenter: ObservableObject {
    static let shared = ToastCenter()

    @Published private(set) var message: String?
    private var clearTask: Task<Void, Never>?

    private init() {}

    func show(_ text: String) {
        clearTask?.cancel()
        message = text
        clearTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(2.5))
            guard !Task.isCancelled else { return }
            self?.message = nil
        }
    }
}

private struct ToastOverlayModifier: ViewModifier {
    @ObservedObject private var center = ToastCenter.shared

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let message = center.message {
                    Text(message)
                        .font(.system(size: 14))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(Color.black.opacity(0.85))
                        .clipShape(Capsule())
                        .padding(.horizontal, 32)
                        .padding(.bottom, 72) // sits above the tab bar
                        .transition(.opacity)
                        .allowsHitTesting(false)
                }
            }
            .animation(.easeInOut(duration: 0.2), value: center.message)
    }
}

extension View {
    /// Renders the shared `ToastCenter` message as a bottom capsule.
    func toastOverlay() -> some View {
        modifier(ToastOverlayModifier())
    }
}
```

- [ ] **Step 2: Build-check later in Task 5** (no separate build here; synchronized folder picks the file up).

---

### Task 3: Tab bar labels/symbols, accent color, toast install, Tracking → Profile tab hop (audit A3, A4; spec §2.2)

**Files:**
- Modify: `COPDFuel/COPDFuel/MainTabView.swift` (whole file)
- Modify: `COPDFuel/COPDFuel/Views/TrackingView.swift` — struct header near line 10-14 and the "Set up" `NavigationLink` near line 824

**Interfaces:**
- Produces: `TrackingView(selectedTab: Binding<Int>)`. Profile tab index is `5`.

- [ ] **Step 1: Replace `MainTabView.swift`**

```swift
//
//  MainTabView.swift
//  COPDFuel
//
//  Main tab navigation. Labels and order mirror Android activity_main.xml:
//  Home, Guidelines, Tracking, Meals, Resources, Profile.
//

import SwiftUI

struct MainTabView: View {
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView(selectedTab: $selectedTab)
                .tabItem { Label("Home", systemImage: "house.fill") }
                .tag(0)

            GuidelinesView()
                .tabItem { Label("Guidelines", systemImage: "book.fill") }
                .tag(1)

            TrackingView(selectedTab: $selectedTab)
                .tabItem { Label("Tracking", systemImage: "chart.bar.fill") }
                .tag(2)

            RecipesView()
                .tabItem { Label("Meals", systemImage: "fork.knife") }
                .tag(3)

            ResourcesView()
                .tabItem { Label("Resources", systemImage: "wrench.fill") }
                .tag(4)

            ProfileView()
                .tabItem { Label("Profile", systemImage: "person.fill") }
                .tag(5)
        }
        .tabViewStyle(.tabBarOnly)
        .tint(Color(hex: "2563eb"))
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .toastOverlay()
    }
}
```

- [ ] **Step 2: Give `TrackingView` the binding**

In `COPDFuel/COPDFuel/Views/TrackingView.swift`, directly under `struct TrackingView: View {` (line 10) and before `@StateObject private var dataManager = DataManager.shared` (line 11), insert:
```swift
    /// Tab selection owned by MainTabView; set to 5 to open Profile
    /// (Android MainActivity.switchToProfile()).
    @Binding var selectedTab: Int
```

- [ ] **Step 3: Replace the profile push with a tab switch**

Find the block near line 824:
```swift
            NavigationLink(destination: ProfileView()) {
                Text("Set up")
```
Change `NavigationLink(destination: ProfileView()) {` to `Button(action: { selectedTab = 5 }) {`. Leave the label contents and closing brace unchanged.

- [ ] **Step 4: Confirm no other constructor call sites**

Run: `grep -rn "TrackingView()" COPDFuel/COPDFuel --include='*.swift'`
Expected: no output (only `MainTabView` constructs it, now with the binding). If a preview or other caller exists, pass `selectedTab: .constant(2)`.

---

### Task 4: Portrait-only orientation (audit A5)

**Files:**
- Modify: `COPDFuel/COPDFuel.xcodeproj/project.pbxproj` lines 297 and 336 (Debug and Release build settings of the app target)

- [ ] **Step 1: Replace both orientation lines**

Both lines currently read:
```
				INFOPLIST_KEY_UISupportedInterfaceOrientations = "UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown";
```
Replace each (use Edit with `replace_all: true`, the two lines are byte-identical) with these three lines:
```
				INFOPLIST_KEY_UIRequiresFullScreen = YES;
				INFOPLIST_KEY_UISupportedInterfaceOrientations = UIInterfaceOrientationPortrait;
				INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad = "UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown";
```
`UIRequiresFullScreen` is required because the target still builds for iPad (`TARGETED_DEVICE_FAMILY = "1,2"`); without it iPad multitasking demands all four orientations and the build warns.

- [ ] **Step 2: Verify**

Run: `grep -c "INFOPLIST_KEY_UISupportedInterfaceOrientations = UIInterfaceOrientationPortrait;" COPDFuel/COPDFuel.xcodeproj/project.pbxproj`
Expected: `2`.

---

### Task 5: Build and commit P3.0

- [ ] **Step 1: Build**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath "$HOME/Library/Developer/Xcode/DerivedData/COPDFuel-cli" \
  -quiet build 2>&1 | tail -20; echo "exit=${PIPESTATUS[0]}"
```
Expected: `exit=0`, no `error:` lines. (A dedicated derived-data path avoids fighting the IDE's own package resolution; the SwiftPM cache under `~/Library/Caches/org.swift.swiftpm` is shared so the AWS SDK is not re-downloaded.)

If the build fails on `TrackingView` because a `#Preview` or another struct constructs `TrackingView()`, fix per Task 3 Step 4 and rebuild.

- [ ] **Step 2: Commit**

```bash
cd "COPDFuel" && /usr/bin/git add -A && /usr/bin/git commit -q -m "iOS parity P3.0: ToastCenter, Meals tab label + Android icon mapping, portrait-only, Tracking Set-up switches to Profile tab

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>" && /usr/bin/git log --oneline -1
```

---

### Task 6: Splash screen parity (audit A1)

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/RootView.swift` — the `body` `Group` condition and the whole `splashContent`

Android `SplashActivity`: fixed 2000 ms, then Main or Login. Gradient angle 0 (left → right) `#3b82f6` → `#2dd4bf`; centered 120dp logo, 24dp gap, "COPD Fuel" 32sp bold white, "Breathe Easier with Better Nutrition" 18sp white at 90 % alpha. iOS logo asset: `splashscreen_logo` (exists, 1x only, fine).

- [ ] **Step 1: Add the minimum display timer**

Under `@ObservedObject private var hipaaStorage = HipaaConsentStorage.shared` add:
```swift
    /// Android SplashActivity shows the splash for a fixed 2000 ms.
    @State private var splashMinimumElapsed = false
```
Change `if auth.isLoading {` to `if auth.isLoading || !splashMinimumElapsed {`.

On the outer `Group { ... }` (after its closing brace, before `}` of `body`), add:
```swift
        .task {
            try? await Task.sleep(for: .seconds(2))
            splashMinimumElapsed = true
        }
```

- [ ] **Step 2: Replace `splashContent`**

```swift
    private var splashContent: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [Color(hex: "3b82f6"), Color(hex: "2dd4bf")]),
                startPoint: .leading,
                endPoint: .trailing
            )
            .ignoresSafeArea()
            VStack(spacing: 24) {
                Image("splashscreen_logo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 120, height: 120)
                    .accessibilityLabel("COPD Fuel Logo")
                VStack(spacing: 8) {
                    Text("COPD Fuel")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.white)
                    Text("Breathe Easier with Better Nutrition")
                        .font(.system(size: 18))
                        .foregroundColor(.white.opacity(0.9))
                        .multilineTextAlignment(.center)
                }
            }
            .padding(.horizontal, 32)
        }
    }
```
The spinner and the two-tone 56 pt wordmark are removed.

---

### Task 7: Home tab parity (audit B1–B8)

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/HomeView.swift` (whole file except the `Color(hex:)` extension at the bottom, which stays as-is)
- Modify: `COPDFuel/COPDFuel/Assets.xcassets/healthy_vs_copd_lungs.imageset/` (one PNG + `Contents.json`)

- [ ] **Step 1: Asset hygiene**

The imageset holds three identical PNGs named `healthy_vs_copd_lungs 1/2/3.png`; keep one.
```bash
cd "COPDFuel/COPDFuel/Assets.xcassets/healthy_vs_copd_lungs.imageset" \
 && /usr/bin/git mv "healthy_vs_copd_lungs 1.png" healthy_vs_copd_lungs.png \
 && /usr/bin/git rm -q "healthy_vs_copd_lungs 2.png" "healthy_vs_copd_lungs 3.png" && ls
```
Expected listing: `Contents.json  healthy_vs_copd_lungs.png`. Then overwrite `Contents.json` with:
```json
{
  "images" : [
    {
      "filename" : "healthy_vs_copd_lungs.png",
      "idiom" : "universal"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
```

- [ ] **Step 2: Replace everything in `HomeView.swift` above the `// Color extension for hex colors` comment**

```swift
//
//  HomeView.swift
//  COPDFuel
//
//  Home screen. Copy, section order and card set mirror Android
//  fragment_home.xml verbatim: header, hero, Understanding COPD
//  (What is COPD?, Common Symptoms, Risk Factors, When to See a Doctor),
//  footer.
//

import SwiftUI

struct HomeView: View {
    @Binding var selectedTab: Int

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                HeaderSection()
                HeroSection(selectedTab: $selectedTab)
                UnderstandingCOPDSection()
                HomeFooterSection()
            }
        }
        .edgesIgnoringSafeArea(.top)
    }
}

struct HeaderSection: View {
    var body: some View {
        VStack {
            HStack(spacing: 0) {
                Text("COPD")
                    .font(.system(size: 28, weight: .bold))
                    .italic()
                    .foregroundColor(Color(hex: "f97316"))
                Text(" Fuel")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(Color(hex: "2563eb"))
            }
            .padding(.top, 60)
            .padding(.bottom, 16)
        }
        .frame(maxWidth: .infinity)
        .background(Color(hex: "dcfce7"))
    }
}

struct HeroSection: View {
    @Binding var selectedTab: Int

    var body: some View {
        VStack(spacing: 24) {
            Text("Breathe Easier with Better Nutrition")
                .font(.system(size: 32, weight: .bold))
                .italic()
                .foregroundColor(Color(hex: "1e40af"))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 20)

            Text("Personalized dietary guidance for managing COPD and improving your quality of life through proper nutrition.")
                .font(.system(size: 18))
                .foregroundColor(Color(hex: "4b5563"))
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .padding(.horizontal, 20)

            Button(action: { selectedTab = 1 }) {
                Text("Explore Guidelines")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 32)
                    .padding(.vertical, 16)
                    .background(Color(hex: "2563eb"))
                    .cornerRadius(8)
            }
        }
        .padding(.vertical, 48)
        .frame(maxWidth: .infinity)
        .background(Color(hex: "eff6ff"))
    }
}

struct UnderstandingCOPDSection: View {
    var body: some View {
        VStack(spacing: 32) {
            VStack(spacing: 16) {
                Text("Understanding COPD")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(Color(hex: "1f2937"))

                Text("Learn about Chronic Obstructive Pulmonary Disease and its impact on health.")
                    .font(.system(size: 18))
                    .foregroundColor(Color(hex: "6b7280"))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }
            .padding(.top, 48)

            VStack(spacing: 32) {
                HomeInfoCard(title: "What is COPD?") {
                    Text("COPD (Chronic Obstructive Pulmonary Disease) is a chronic lung disease that includes conditions such as Emphysema and Bronchiectasis. It causes obstructed airflow from the lungs, making it difficult to breathe.")
                        .font(.system(size: 16))
                        .foregroundColor(Color(hex: "4b5563"))
                        .lineSpacing(4)

                    Text("Approximately 16 million adults have been diagnosed with COPD, and many more may have the disease without a formal diagnosis. COPD is the third leading cause of death globally.")
                        .font(.system(size: 16))
                        .foregroundColor(Color(hex: "4b5563"))
                        .lineSpacing(4)

                    // Android: 200dp centerCrop image inside a white
                    // radius-12 card.
                    Image("healthy_vs_copd_lungs")
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(maxWidth: .infinity)
                        .frame(height: 200)
                        .clipped()
                        .background(Color.white)
                        .cornerRadius(12)
                        .accessibilityLabel("Healthy vs COPD lungs illustration")
                        .padding(.top, 8)
                }

                HomeInfoCard(title: "Common Symptoms") {
                    VStack(alignment: .leading, spacing: 12) {
                        SymptomRow(text: "Shortness of breath, especially during physical activities")
                        SymptomRow(text: "Chronic cough that may produce mucus")
                        SymptomRow(text: "Wheezing")
                        SymptomRow(text: "Chest tightness")
                        SymptomRow(text: "Frequent respiratory infections")
                        SymptomRow(text: "Lack of energy")
                        SymptomRow(text: "Unintended weight loss (in later stages)")
                        SymptomRow(text: "Swelling in ankles, feet or legs")
                    }
                }

                HomeInfoCard(title: "Risk Factors") {
                    Text("Smoking, exposure to air pollution, genetics, and occupational dust or chemicals can increase the risk of developing COPD.")
                        .font(.system(size: 16))
                        .foregroundColor(Color(hex: "4b5563"))
                        .lineSpacing(4)
                }

                HomeInfoCard(title: "When to See a Doctor") {
                    Text("See your doctor if you have persistent cough, shortness of breath, wheezing, or frequent respiratory infections. Early diagnosis and treatment can help manage COPD effectively.")
                        .font(.system(size: 16))
                        .foregroundColor(Color(hex: "4b5563"))
                        .lineSpacing(4)
                }
            }
            .padding(.horizontal, 20)
        }
        .padding(.vertical, 48)
        .background(Color.white)
    }
}

/// Blue rounded card with a 22pt semibold title, used by every
/// "Understanding COPD" card.
struct HomeInfoCard<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(title)
                .font(.system(size: 22, weight: .semibold))
                .foregroundColor(Color(hex: "1e40af"))
            content
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "eff6ff"))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
    }
}

struct SymptomRow: View {
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            // Android: plain 20dp #bfdbfe circle, no glyph.
            Circle()
                .fill(Color(hex: "bfdbfe"))
                .frame(width: 20, height: 20)

            Text(text)
                .font(.system(size: 16))
                .foregroundColor(Color(hex: "4b5563"))
                .lineSpacing(2)
        }
    }
}

/// Android fragment_home.xml footer: dark band, wordmark, tagline,
/// divider, copyright/disclaimer line.
struct HomeFooterSection: View {
    var body: some View {
        VStack(spacing: 16) {
            Text("COPD Fuel")
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(.white)

            Text("Helping you breathe easier with personalized nutritional guidance tailored for COPD management.")
                .font(.system(size: 16))
                .foregroundColor(Color(hex: "d1d5db"))
                .multilineTextAlignment(.center)

            Rectangle()
                .fill(Color(hex: "e5e7eb"))
                .frame(height: 1)

            Text("(c) 2024 COPD Fuel. All rights reserved. The information provided is not medical advice.")
                .font(.system(size: 14))
                .foregroundColor(Color(hex: "9ca3af"))
                .multilineTextAlignment(.center)
        }
        .padding(48)
        .frame(maxWidth: .infinity)
        .background(Color(hex: "1f2937"))
    }
}
```
Keep the existing `extension Color { init(hex: String) ... }` block unchanged at the end of the file.

- [ ] **Step 3: Quick copy check**

Run: `grep -c "Risk Factors\|When to See a Doctor\|(c) 2024 COPD Fuel" COPDFuel/COPDFuel/Views/HomeView.swift`
Expected: `3`.

---

### Task 8: Guidelines tab parity (audit C1–C8)

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/GuidelinesView.swift` (whole file replaced)

Removed on purpose: the "Dietary / Medication" `Picker`, `medicationSection`, `MedicationCategory`, `MedicationCategoryCard`, `MedicationCategoryDetail`, all ten `*Detail` structs, the GOLD reference link in the medication header, the "Featured COPD Inhalers" block (P3.B re-creates it in Resources from Android copy with the `https://www.symbicort.com/` URL), the iOS-only sentence "The points below summarize…", the duplicated references panel, and the `.navigationTitle("Guidelines")` (Android has no toolbar title). Nothing outside this file references the removed types (verified by grep on 2026-09-19).

Order (Android `fragment_guidelines.xml`): title + intro → six guideline cards → consult box → "Preventing COPD Exacerbations" blue band → "The Importance of Protein" → "Recommended Foods for COPD" → references panel (kept, once).

- [ ] **Step 1: Replace the whole file**

```swift
//
//  GuidelinesView.swift
//  COPDFuel
//
//  COPD Dietary Guidelines. Single scrolling page whose copy, order and
//  card styling mirror Android GuidelinesFragment / fragment_guidelines.xml.
//  The "References & sources" panel at the bottom is an iOS-only
//  App Review (1.4.1) requirement and is intentionally kept.
//

import SwiftUI

struct GuidelinesView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                // Header: centered 28sp bold title + 16sp centered intro.
                VStack(spacing: 8) {
                    Text("COPD Dietary Guidelines")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(Color(hex: "1f2937"))
                        .multilineTextAlignment(.center)
                    Text("Proper nutrition plays a vital role in managing COPD symptoms and improving overall health.")
                        .font(.system(size: 16))
                        .foregroundColor(Color(hex: "6b7280"))
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 20)
                .padding(.top, 24)

                // Six guideline cards (item_guideline.xml style).
                VStack(spacing: 16) {
                    GuidelineCard(
                        title: "Maintain a Healthy Weight",
                        detail: "Being underweight can reduce respiratory muscle strength, while excess weight can make breathing more difficult. Aim for a healthy weight through balanced nutrition."
                    )
                    GuidelineCard(
                        title: "Stay Hydrated",
                        detail: "Drinking plenty of fluids helps keep mucus thin and easier to clear from the lungs. Aim for 6-8 glasses of water daily unless otherwise advised by your doctor."
                    )
                    GuidelineCard(
                        title: "Eat Smaller, More Frequent Meals",
                        detail: "Large meals can make breathing uncomfortable by pushing against your diaphragm. Smaller, more frequent meals can help prevent this discomfort."
                    )
                    GuidelineCard(
                        title: "Monitor Salt Intake",
                        detail: "Excess sodium can cause fluid retention, which may make breathing more difficult. Choose fresh foods and herbs over salt for flavoring."
                    )
                    GuidelineCard(
                        title: "Include Antioxidant-Rich Foods",
                        detail: "Foods high in antioxidants can help reduce inflammation in the airways. Fresh fruits and vegetables are excellent sources."
                    )
                    GuidelineCard(
                        title: "Consider Supplements",
                        detail: "Consult with your healthcare provider about supplements like vitamin D, calcium, and omega-3 fatty acids, which may benefit COPD patients."
                    )
                }
                .padding(.horizontal, 20)

                // Consult notice: yellow box right after the six guidelines.
                Text("Always consult with your healthcare provider before making significant changes to your diet.")
                    .font(.system(size: 14))
                    .italic()
                    .foregroundColor(Color(hex: "6b7280"))
                    .multilineTextAlignment(.center)
                    .padding(16)
                    .frame(maxWidth: .infinity)
                    .background(Color(hex: "fef3c7"))
                    .cornerRadius(8)
                    .padding(.horizontal, 20)

                PreventingExacerbationsSection()

                // The Importance of Protein
                VStack(alignment: .leading, spacing: 8) {
                    Text("The Importance of Protein")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color(hex: "1e40af"))
                    (
                        Text("Research shows that COPD patients with adequate protein intake have better outcomes. Protein helps maintain respiratory muscle mass and function, which can decline in COPD patients. ")
                        + Text("Aim for 20-30 g protein per meal.").fontWeight(.bold)
                    )
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "4b5563"))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)

                RecommendedFoodsSection()

                DietaryReferencesPanel()
                    .padding(.bottom, 24)
            }
        }
    }
}

/// item_guideline.xml: #f8fafc card, radius 12, 4dp #2563eb left accent
/// bar, 18sp bold title, 14sp body.
struct GuidelineCard: View {
    let title: String
    let detail: String

    var body: some View {
        HStack(spacing: 0) {
            Rectangle()
                .fill(Color(hex: "2563eb"))
                .frame(width: 4)
            VStack(alignment: .leading, spacing: 6) {
                Text(title)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(Color(hex: "1f2937"))
                Text(detail)
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "4b5563"))
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Color(hex: "f8fafc"))
        .cornerRadius(12)
    }
}

/// Blue #eff6ff band with 24sp #1e40af title, subtitle, framing paragraph
/// and three white strategy cards stroked #e0e7ff.
private struct PreventingExacerbationsSection: View {
    private let strategies: [(String, String)] = [
        ("Nutrition Plan", "Focus on protein-rich foods like lean meats, fish, eggs, and plant proteins. Adequate protein intake helps maintain respiratory muscle strength."),
        ("Breathing Exercises", "Regular breathing exercises like pursed-lip breathing and diaphragmatic breathing can improve lung function and oxygen levels."),
        ("Physical Activity", "Regular, moderate exercise improves cardiovascular health and strengthens respiratory muscles. Consult with your healthcare provider for an appropriate exercise plan.")
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Preventing COPD Exacerbations")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(Color(hex: "1e40af"))
            Text("Strategies to reduce flare-ups and maintain lung function")
                .font(.system(size: 14))
                .foregroundColor(Color(hex: "6b7280"))
            Text("To prevent COPD exacerbations, it is crucial to take various precautions. Besides performing breathing exercises and engaging in physical activity, maintaining a healthy diet is extremely important for lung function. Specifically, the amount of protein in one's diet can significantly impact lung health. Proper nutrition, combined with regular exercise and respiratory therapies, can help manage COPD and improve patients' quality of life.")
                .font(.system(size: 14))
                .foregroundColor(Color(hex: "4b5563"))

            VStack(spacing: 12) {
                ForEach(strategies, id: \.0) { item in
                    VStack(alignment: .leading, spacing: 6) {
                        Text(item.0)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(Color(hex: "1e40af"))
                        Text(item.1)
                            .font(.system(size: 14))
                            .foregroundColor(Color(hex: "4b5563"))
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white)
                    .cornerRadius(10)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Color(hex: "e0e7ff"), lineWidth: 1)
                    )
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "eff6ff"))
    }
}

// MARK: - Recommended Foods for COPD
// Android "Foods to Embrace" (+ marker, #2563eb) / "Foods to Limit or
// Avoid" (- marker, #cc0000); rows have no background.

private struct RecommendedFoodsSection: View {
    private let embrace: [(String, String)] = [
        ("Fresh Fruits and Vegetables", "Rich in antioxidants and fiber, they help reduce inflammation and support immune function."),
        ("Lean Proteins", "Fish, poultry, beans, and tofu provide essential amino acids without excess calories."),
        ("Whole Grains", "Brown rice, whole wheat bread, and oats provide sustained energy and important nutrients."),
        ("Healthy Fats", "Olive oil, avocados, nuts, and fatty fish contain omega-3s that may help reduce inflammation."),
        ("Dairy or Fortified Alternatives", "Good sources of calcium and vitamin D for bone health, especially important if taking steroids.")
    ]
    private let limit: [(String, String)] = [
        ("Processed Foods", "Often high in sodium, preservatives, and artificial ingredients that may worsen inflammation."),
        ("Gas-Producing Foods", "Beans, cabbage, and carbonated beverages can cause bloating that makes breathing uncomfortable."),
        ("Excessive Salt", "Can lead to fluid retention, making it harder to breathe and potentially raising blood pressure."),
        ("Cold Foods", "Very cold foods and beverages may trigger coughing or breathing difficulties in some individuals.")
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Recommended Foods for COPD")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(Color(hex: "1f2937"))
                Text("Making smart food choices can help manage COPD symptoms and improve your overall health.")
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "6b7280"))
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Text("Foods to Embrace")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
            VStack(spacing: 10) {
                ForEach(embrace, id: \.0) { item in
                    FoodMarkerRow(marker: "+", markerColor: Color(hex: "2563eb"), title: item.0, detail: item.1)
                }
            }

            Text("Foods to Limit or Avoid")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))
                .padding(.top, 4)
            VStack(spacing: 10) {
                ForEach(limit, id: \.0) { item in
                    FoodMarkerRow(marker: "-", markerColor: Color(hex: "cc0000"), title: item.0, detail: item.1)
                }
            }
        }
        .padding(.horizontal, 20)
    }
}

private struct FoodMarkerRow: View {
    let marker: String
    let markerColor: Color
    let title: String
    let detail: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Text(marker)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(markerColor)
                .frame(width: 24, alignment: .center)
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Color(hex: "1f2937"))
                Text(detail)
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "4b5563"))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - References (kept: App Store Review Guideline 1.4.1)

/// Authoritative citations for App Review. Links open in Safari.
private struct DietaryReferencesPanel: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("References & sources")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(Color(hex: "1e3a8a"))
            Text("Tap a link to review evidence-based COPD and nutrition guidance from these organizations.")
                .font(.system(size: 13))
                .foregroundColor(Color(hex: "475569"))

            VStack(alignment: .leading, spacing: 10) {
                GuidelineSourceLink(
                    title: "GOLD — Global Initiative for Chronic Obstructive Lung Disease",
                    subtitle: "Global strategy documents and patient-facing summaries",
                    url: URL(string: "https://goldcopd.org")!
                )
                GuidelineSourceLink(
                    title: "NIH NHLBI — COPD (National Heart, Lung, and Blood Institute)",
                    subtitle: "U.S. government overview of COPD care and lifestyle",
                    url: URL(string: "https://www.nhlbi.nih.gov/health/copd")!
                )
                GuidelineSourceLink(
                    title: "American Lung Association — COPD",
                    subtitle: "Education on living with COPD, including nutrition and wellness",
                    url: URL(string: "https://www.lung.org/lung-health-diseases/lung-disease-lookup/copd")!
                )
                GuidelineSourceLink(
                    title: "MedlinePlus — COPD (U.S. National Library of Medicine)",
                    subtitle: "Trusted consumer health information",
                    url: URL(string: "https://medlineplus.gov/copd.html")!
                )
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "eff6ff"))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(hex: "bfdbfe"), lineWidth: 1)
        )
        .padding(.horizontal, 20)
    }
}

private struct GuidelineSourceLink: View {
    let title: String
    let subtitle: String
    let url: URL

    var body: some View {
        Link(destination: url) {
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: "link.circle.fill")
                    .font(.system(size: 20))
                    .foregroundColor(Color(hex: "2563eb"))
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Color(hex: "1e40af"))
                        .multilineTextAlignment(.leading)
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundColor(Color(hex: "64748b"))
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 8)
                Image(systemName: "arrow.up.right.square")
                    .font(.system(size: 15))
                    .foregroundColor(Color(hex: "2563eb"))
            }
        }
    }
}
```

- [ ] **Step 2: Copy/removal check**

Run:
```bash
grep -c "DietaryReferencesPanel()" COPDFuel/COPDFuel/Views/GuidelinesView.swift; grep -c "MedicationCategory\|Featured COPD Inhalers\|The points below summarize\|navigationTitle" COPDFuel/COPDFuel/Views/GuidelinesView.swift
```
Expected: `1` then `0`.

---

### Task 9: Meals tab parity (audit D1–D3)

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/RecipesView.swift` — `RecipesView.body`, `RecipeSectionCard`, `NumberedRecipeCard`; delete the `cornerRadius(_:corners:)` extension and `RoundedCorner` shape (unused elsewhere, verified 2026-09-19). Keep the `Recipe` / `RecipeSection` models and the `sections` data untouched (titles, numbering, bullets and image names already match Android).

- [ ] **Step 1: Replace `RecipesView.body`**

```swift
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("COPD-Friendly Meals")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Color(hex: "2563eb"))
                    Text("Nutritious and delicious meal ideas that are easy to prepare and gentle on your respiratory system.")
                        .font(.system(size: 14))
                        .foregroundColor(Color(hex: "4b5563"))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.top, 24)

                ForEach(sections) { section in
                    RecipeSectionCard(section: section)
                        .padding(.horizontal, 20)
                }
            }
            .padding(.bottom, 24)
        }
    }
```
(The `NavigationStack` and `.navigationTitle("Recipes")` are removed; Android has no toolbar title.)

- [ ] **Step 2: Replace `RecipeSectionCard` and `NumberedRecipeCard`; delete the corner helpers**

```swift
/// Section heading inside a #eff6ff radius-12 card, 18sp bold #2563eb,
/// followed by the numbered item cards.
struct RecipeSectionCard: View {
    let section: RecipeSection

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(section.heading)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color(hex: "2563eb"))
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(hex: "eff6ff"))
                .cornerRadius(12)

            VStack(spacing: 12) {
                ForEach(Array(section.recipes.enumerated()), id: \.element.id) { index, recipe in
                    NumberedRecipeCard(number: index + 1, recipe: recipe)
                }
            }
        }
    }
}

/// item card: #f8fafc, radius 8, padding 16/12, 160dp centerCrop image
/// inset inside the padding (square corners), "N. Title" 15sp bold,
/// bullets "  • text" 14sp #4b5563.
struct NumberedRecipeCard: View {
    let number: Int
    let recipe: Recipe

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(recipe.imageName)
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(maxWidth: .infinity)
                .frame(height: 160)
                .clipped()

            Text("\(number). \(recipe.title)")
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(Color(hex: "1f2937"))

            VStack(alignment: .leading, spacing: 4) {
                ForEach(recipe.nutrients, id: \.self) { nutrient in
                    Text("  • \(nutrient)")
                        .font(.system(size: 14))
                        .foregroundColor(Color(hex: "4b5563"))
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "f8fafc"))
        .cornerRadius(8)
    }
}
```
Delete `extension View { func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) ... }` and `struct RoundedCorner: Shape { ... }` at the end of the file.

- [ ] **Step 3: Check**

Run: `grep -c "COPD-Friendly Meals" COPDFuel/COPDFuel/Views/RecipesView.swift; grep -c "RoundedCorner\|navigationTitle" COPDFuel/COPDFuel/Views/RecipesView.swift`
Expected: `1` then `0`.

---

### Task 10: Build, copy check, commit P3.A

- [ ] **Step 1: Build** (same command as Task 5 Step 1). Expected `exit=0`.

- [ ] **Step 2: Copy check against Android**

```bash
for s in "Breathe Easier with Better Nutrition" "Risk Factors" "When to See a Doctor" "Helping you breathe easier with personalized nutritional guidance" "Always consult with your healthcare provider before making significant changes" "Preventing COPD Exacerbations" "COPD-Friendly Meals" "Lunch & Dinner Ideas"; do
  a=$(grep -rl --include='*.xml' --include='*.kt' -F "$s" android/app/src/main | wc -l | tr -d ' ')
  i=$(grep -rl --include='*.swift' -F "$s" COPDFuel/COPDFuel | wc -l | tr -d ' ')
  echo "$a android / $i ios  <- $s"
done
```
Expected: every line has non-zero counts on both sides. (`&` in "Lunch & Dinner Ideas" appears as `&amp;` in Android XML; if that line shows 0 on the Android side, re-run that one string as `Lunch &amp; Dinner Ideas` and treat a hit as pass.)

- [ ] **Step 3: Commit**

```bash
cd "COPDFuel" && /usr/bin/git add -A && /usr/bin/git commit -q -m "iOS parity P3.A: Android splash, Home risk/doctor cards + footer, single-page Guidelines, Meals styling

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>" && /usr/bin/git log --oneline -1
```

- [ ] **Step 4: Manual smoke (simulator)**

Boot the iPhone 17 Pro simulator, launch, confirm: gradient splash with logo for ~2 s → login → Home scrolls through four cards and the dark footer → Guidelines shows no segmented control and ends with the references panel → Meals tab is labelled "Meals" with the fork/knife icon → Tracking's "Set up" banner button switches to the Profile tab → rotating the simulator stays portrait.

---

## Out of scope here (each gets its own plan, per spec §3)

- P3.B Resources (5 tools, medication guides, devices, resource hub)
- P3.C Tracking (largest phase; needs `ToastCenter` and the `selectedTab` binding from this plan)
- P3.D Label scan (`LabelScanKit` package + camera flow; follows P3.C)
- P3.E Profile / Report / Health import / Link doctor / Delete account
- P3.F Auth + HIPAA + Paywall (follows P3.E for the flat profile keys)
- P3.G Programs Near Me
