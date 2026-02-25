# How to Build and Run the COPD Fuel Android App

## Prerequisites

1. **Java Development Kit (JDK) 17 or higher**
   - Download from: https://www.oracle.com/java/technologies/downloads/
   - Or use OpenJDK: https://adoptium.net/
   - Verify installation: `java -version` (should show version 17+)

2. **Android Studio** (Recommended)
   - Download from: https://developer.android.com/studio
   - Includes Android SDK, Gradle, and all necessary tools

3. **Android SDK**
   - API Level 34 (Android 14) - for compileSdk
   - API Level 24 (Android 7.0) - minimum SDK
   - Install via Android Studio SDK Manager

## Method 1: Using Android Studio (Recommended)

### Step 1: Open the Project
1. Launch Android Studio
2. Click **File → Open**
3. Navigate to: `/Users/weijiahuang/Downloads/COPD/my-copd-app/android`
4. Click **OK**

### Step 2: Sync Gradle
1. Android Studio will automatically detect the Gradle files
2. If prompted, click **Sync Now** or **Sync Project with Gradle Files**
3. Wait for Gradle to download dependencies (first time may take several minutes)

### Step 3: Set Up Emulator or Connect Device

**Option A: Use Android Emulator**
1. Click **Tools → Device Manager**
2. Click **Create Device**
3. Select a device (e.g., Pixel 5)
4. Select a system image (API 34 recommended)
5. Click **Finish**

**Option B: Use Physical Device**
1. Enable **Developer Options** on your Android device:
   - Go to **Settings → About Phone**
   - Tap **Build Number** 7 times
2. Enable **USB Debugging**:
   - Go to **Settings → Developer Options**
   - Enable **USB Debugging**
3. Connect device via USB
4. Accept the USB debugging prompt on your device

### Step 4: Build and Run
1. Select your device/emulator from the device dropdown (top toolbar)
2. Click the **Run** button (green play icon) or press `Shift + F10`
3. The app will build and install on your device/emulator

## Method 2: Using Command Line

### Step 1: Navigate to Project Directory
```bash
cd /Users/weijiahuang/Downloads/COPD/my-copd-app/android
```

### Step 2: Make Gradle Wrapper Executable (if needed)
```bash
chmod +x gradlew
```

### Step 3: Build the APK
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing configuration)
./gradlew assembleRelease
```

The APK will be generated at:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

### Step 4: Install on Device
```bash
# Connect your device via USB and enable USB debugging
# Then install the debug APK:
adb install app/build/outputs/apk/debug/app-debug.apk

# Or use Gradle to build and install directly:
./gradlew installDebug
```

### Step 5: Run the App
```bash
# Launch the app on connected device
adb shell am start -n com.copdhealthtracker/.MainActivity
```

## Troubleshooting

### Issue: Gradle Sync Failed
**Solution:**
- Check internet connection
- Verify JDK 17+ is installed: `java -version`
- In Android Studio: **File → Invalidate Caches / Restart**

### Issue: Build Errors
**Solution:**
- Clean the project: `./gradlew clean`
- Rebuild: `./gradlew build`
- Check that all dependencies are downloaded

### Issue: "SDK not found"
**Solution:**
- Open Android Studio
- Go to **Tools → SDK Manager**
- Install Android SDK Platform 34
- Install Android SDK Build-Tools

### Issue: "Cannot resolve symbol" errors
**Solution:**
- Sync Gradle: **File → Sync Project with Gradle Files**
- Rebuild: **Build → Rebuild Project**

### Issue: App crashes on launch
**Solution:**
- Check Logcat in Android Studio for error messages
- Verify the device/emulator meets minimum SDK (API 24)
- Check that all required permissions are granted

## Build Variants

- **Debug**: For development and testing
  - Includes debugging symbols
  - Not optimized
  - Signed with debug keystore

- **Release**: For production
  - Optimized and minified
  - Requires signing configuration
  - Smaller APK size

## Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/copdhealthtracker/
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/          # Room database
│   │   │   ├── repository/    # Data access layer
│   │   │   ├── ui/            # UI components
│   │   │   └── utils/         # Utilities
│   │   └── res/               # Resources
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradlew                    # Gradle wrapper
```

## Additional Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Check for lint issues
./gradlew lint

# View connected devices
adb devices

# View app logs
adb logcat | grep copdhealthtracker
```

## Next Steps

Once the app is running:
1. Navigate through the bottom tabs (Home, Guidelines, Tracking, Recipes, Resources, Profile)
2. Try adding food entries, exercises, oxygen readings, and weight
3. Edit your profile information
4. Explore the guidelines and resources

## Need Help?

- Check Android Studio's **Build** output for detailed error messages
- Review **Logcat** for runtime errors
- Ensure all dependencies are properly synced
- Verify your development environment meets all prerequisites
