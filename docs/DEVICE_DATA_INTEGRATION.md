# Device Data Integration (Watch / Health Connect)

COPD Fuel can import health data from your phone or wearable (e.g. Samsung watch, Wear OS) using **Android Health Connect**.

## What Gets Imported

The app imports as much data as Health Connect has for the **last 30 days**:

- **Oxygen saturation (SpO2)** – into Oxygen Saturation in Tracking
- **Weight** – into Weight in Tracking
- **Exercise sessions** – into Exercise in Tracking (as “Exercise” with duration in minutes)
- **Steps** – stored by day and shown in Tracking (Steps row under Oxygen/Exercise for the selected day)

One tap on **Import from device** requests permission for all of these and then imports everything available.

## Requirements

- **Android 8.0 (API 26) or later** (the app’s minimum SDK is 26 so Health Connect is supported on all installs).
- **Health Connect** must be installed (on Android 13 and below it’s a separate app from the Play Store; on Android 14+ it may be built in).
- Your **watch or phone** must write SpO2 data into Health Connect (e.g. Samsung Health syncing to Health Connect, or a Wear OS app that writes SpO2).

## How to Use

1. On the **Tracking** tab, open the **Oxygen Saturation** card.
2. Tap **Import from device**.
3. The app opens the **Health Connect permission screen** in-app. Grant access for **Oxygen saturation**, **Weight**, **Exercise**, and **Steps** (read).
4. After you grant access, the app imports the last 30 days of data for all four types. A toast shows how many of each were imported. Oxygen, weight, and exercise appear in their cards; steps appear in the **Steps** row below the Oxygen/Exercise cards for the selected day.

## Why is steps 0 after import?

Steps can show 0 for a few reasons:

1. **Samsung Health is not sharing steps into Health Connect** – In **Samsung Health**, open **Settings** (gear) → **Health Connect** (or **Connected services** → Health Connect). Allow Samsung Health to **share** **Steps** / **Activity** / **Physical activity** with Health Connect. Then open **Samsung Health** again or use **Sync now** if available. Without this, COPD Fuel reads an empty Health Connect dataset for steps. After linking, sync can take a few minutes. The app reads **daily step totals** from Health Connect first (how Samsung usually exposes steps), then falls back to raw step records.

2. **Viewing "today"** – Health Connect often has steps only for **completed days**. If you import and then look at **today**, today’s steps might not be in Health Connect yet. Use the **date picker** on the Tracking screen to select **yesterday** or another recent day to see imported steps.

3. **Steps permission not granted** – When the app asked for Health Connect access, **Steps** must be allowed. If it was denied, the app gets no step data. Open Health Connect > App permissions, find COPD Fuel, and enable **Steps** (read).

4. **Correct date range** – The app imports the **last 30 days**. Steps are stored by day; the Steps value on Tracking is for the **currently selected date**. Change the date to a day when you had steps and had already synced to Health Connect.

## Why is exercise 0 after import?

Exercise in Health Connect is **workout sessions** (start and end time), not step count. You get exercise records only when a workout is explicitly logged, for example:

- You start and stop a workout in **Samsung Health** or on your watch (e.g. "Running", "Walking", "Other workout").
- Another app (e.g. Strava, Google Fit) writes workout sessions to Health Connect.

If you only track steps and never log a workout, you will see steps but 0 exercise. To get exercise data:

1. In **Samsung Health**: go to **Settings > Health Connect** and allow Samsung Health to **share** "Exercise" or "Exercise sessions" with Health Connect.
2. Log workouts on your watch or in Samsung Health (start/stop a workout) so that sessions appear in Health Connect.

## If Import Fails

- **"Health Connect is not available"** – Install or update Health Connect from the Play Store and try again.
- **"Grant oxygen data access..."** – Open **Settings > Apps > Health Connect > App permissions**, find COPD Fuel, and allow **Oxygen saturation**, **Weight**, **Exercise**, and **Steps** (read).
- **"No health data found"** – Ensure your watch or phone is syncing with Health Connect (e.g. connect Samsung Health to Health Connect in Settings). Then try again.

## Technical Notes

- Integration is in `HealthConnectSync` (`android/.../health/HealthConnectSync.kt`). It uses `readRecords()` for oxygen, weight, and exercise. For **steps**, it uses `aggregateGroupByPeriod` (daily totals) first, then `readRecords(StepsRecord)` as a fallback. Steps are stored per calendar day.
- **In-app permission:** When the user taps **Import from device**, the app launches the Health Connect permission UI and requests read permission for all four record types. After the user grants, the full import runs; if they deny, a toast directs them to Health Connect settings.
- Steps are stored in `steps_entries` and shown in the Tracking day view. Oxygen, weight, and exercise use the existing Tracking cards and lists.
