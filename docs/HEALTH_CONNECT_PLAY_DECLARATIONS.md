# Health Connect – Play Console declarations (purpose and user benefit)

Use the text below in **Google Play Console** for each Health Connect permission. Paste into the "Describe your app's use of the [permission]" or "Justification" field. Each declaration states the purpose of the permission, how the data is used, and the benefit to the user, as required by Play policy.

---

## Why Google likely rejected the app before

Google enforces the [Health and fitness permissions](https://support.google.com/googleplay/android-developer/answer/12991134) and [Health apps](https://support.google.com/googleplay/android-developer/answer/14738291) policy. Rejections for Health Connect apps are usually due to one or more of:

1. **Missing or weak permission justifications** – Each Health Connect permission must have a clear explanation of **why** the app needs it and **how it benefits the user**. Vague text (e.g. "needed for app functionality") is not enough.
2. **No in-app disclosure before requesting permissions** – Users must see a clear explanation of what data is requested and why **before** the Health Connect permission screen. Without this, the app can be rejected.
3. **Store listing does not describe app and Health Connect use** – The short and full description must state that the app is for COPD health tracking and that users can optionally import data from Health Connect (and which data types). If the listing does not match the permissions, review fails.
4. **Data safety form incomplete** – Health data collected or used by the app must be declared in the **Data safety** section (Policy > App content > Data safety). If health data is not declared there, it can trigger rejection.
5. **Privacy policy missing or not linked** – A privacy policy URL is required. It must describe what health data is collected, how it is used and shared, and that it is not used for advertising or sold. The policy must be linked in the app and in the Play Console store listing.

---

## Checklist before resubmitting

Do all of the following, then upload a **new AAB** and submit for review.

| # | Task | Where | Status |
|---|------|-------|--------|
| 1 | **Health apps declaration** – Paste the five permission paragraphs (sections 1–5 below) into the "Health data permissions" form for each permission. Save and complete the Health apps flow. | Play Console > Policy > App content > Health apps > Manage > … > Health data permissions | [ ] |
| 2 | **Store listing** – Short and full description state: COPD health tracking app; optional import of oxygen, weight, exercise, steps, heart rate from Health Connect; view and track in app; optional share with doctor. Add the example sentence from "App functionality" section below if needed. | Play Console > Grow > Store presence > Main store listing | [ ] |
| 3 | **Data safety** – Declare that the app collects or uses **health data** (e.g. health and fitness). Indicate: collected/used for app functionality; optional (user chooses to import); not shared for ads; not sold. Add any other data types the app collects (e.g. email, name) per the form. | Play Console > Policy > App content > Data safety | [ ] |
| 4 | **Privacy policy** – Ensure a privacy policy is published at a stable URL. It must state: what health data is collected (including from Health Connect); how it is used and stored; that it can be shared with a linked healthcare provider with consent; that it is not used for advertising or sold; how users can request deletion. Link the same URL in the app (Profile > Privacy Policy) and in Play Console store listing (Privacy policy field). | Your hosted privacy policy; `PRIVACY_POLICY_URL` in build.gradle; Play Console store listing | [ ] |
| 5 | **In-app rationale** – Confirm the app shows a dialog **before** requesting Health Connect permissions, explaining which data types are requested and why (no ads, no selling). This is already implemented in TrackingFragment with `tracking_import_from_device_rationale_title` and `tracking_import_from_device_rationale_message`. | Build and test the app; trigger "Import from device" in Tracking | [ ] |
| 6 | **New AAB** – Build a new release AAB that includes the in-app rationale and any other fixes. Upload to Play Console and submit for review. | Play Console > Release > Production (or your track) | [ ] |

---

## How to paste in Play Console

1. **Open Play Console**  
   Go to [play.google.com/console](https://play.google.com/console) and open your app (COPD Fuel).

2. **Go to App content and Health Connect**  
   - In the left menu: **Policy** > **App content**.  
   - Open the **Health apps** declaration (click **Start** or **Manage** if you already started).  
   - If Play shows a separate **Health Connect** or **Health and fitness permissions** section (e.g. under App content or when you submit a new version), open that.  
   - You may see either: **(A)** one form that lists each permission with its own text field, or **(B)** a single "Health Connect" justification field.

3. **Paste the declaration for each permission**  
   - **If you see one field per permission:**  
     For each of the five data types, paste the **full paragraph** from the matching section below:
     - **Oxygen saturation** → use section **1** (lines under "## 1. Oxygen saturation").
     - **Weight** → use section **2**.
     - **Exercise** → use section **3**.
     - **Steps** → use section **4**.
     - **Heart rate** → use section **5**.
   - **If you see one combined Health Connect field:**  
     Paste all five purpose/benefit paragraphs in order (Oxygen → Weight → Exercise → Steps → Heart rate), separated by line breaks or bullets.

4. **Save and submit**  
   Save the form. If you are submitting a new version, complete the release and submit for review.

---

## 1. Oxygen saturation (READ_OXYGEN_SATURATION)

COPD Fuel uses the READ_OXYGEN_SATURATION permission to read blood oxygen (SpO2) data from Health Connect when the user chooses to import health data from their device (e.g. a connected watch or pulse oximeter). We use this data only to display the user's SpO2 history inside the app and to help people with COPD monitor their oxygen levels as part of their daily management. The user must explicitly grant this permission; it is optional. We do not use this data for advertising or sell it. If the user has linked their account to a healthcare provider, they can choose to share this data with their care team through our secure doctor portal.

---

## 2. Weight (READ_WEIGHT)

COPD Fuel uses the READ_WEIGHT permission to read weight data from Health Connect when the user chooses to import health data from their device (e.g. a connected scale or phone). We use this data only to display the user's weight history inside the app and to help people with COPD track weight alongside other respiratory health metrics. The user must explicitly grant this permission; it is optional. We do not use this data for advertising or sell it. If the user has linked their account to a healthcare provider, they can choose to share this data with their care team through our secure doctor portal.

---

## 3. Exercise (READ_EXERCISE)

COPD Fuel uses the READ_EXERCISE permission to read exercise session data from Health Connect when the user chooses to import health data from their device (e.g. a connected watch or fitness app). We use this data only to display the user's exercise and activity history inside the app and to help people with COPD track pulmonary rehab and daily activity. The user must explicitly grant this permission; it is optional. We do not use this data for advertising or sell it. If the user has linked their account to a healthcare provider, they can choose to share this data with their care team through our secure doctor portal.

---

## 4. Steps (READ_STEPS)

COPD Fuel uses the READ_STEPS permission to read step count data from Health Connect when the user chooses to import health data from their device (e.g. a connected watch or phone). We use this data only to display the user's daily steps inside the app and to help people with COPD track daily activity and mobility. The user must explicitly grant this permission; it is optional. We do not use this data for advertising or sell it.

---

## 5. Heart rate (READ_HEART_RATE)

COPD Fuel uses the READ_HEART_RATE permission to read heart rate data from Health Connect when the user chooses to import health data from their device (e.g. a connected watch). We use this data only to display the user's heart rate history inside the app alongside oxygen and exercise data, supporting a full picture of cardiovascular activity for COPD management. The user must explicitly grant this permission; it is optional. We do not use this data for advertising or sell it.

---

## App functionality (store listing and in-app)

- **App description:** In Play Console store listing, ensure the short and full description clearly state:
  - COPD Fuel is a **health tracking app for people with COPD** (and their caregivers).
  - It lets users **track** oxygen, weight, medications, exercise, nutrition, hydration, and more in one place.
  - **Optional:** Users can **import** oxygen, weight, exercise, steps, and heart rate from **Health Connect** (e.g. from a watch or Samsung Health) to view and track in the app and optionally share with their doctor.
  - Example sentence to add to the full description: "You can optionally connect Health Connect to import oxygen, weight, exercise, steps, and heart rate from your watch or phone so you can view and track them in one place and share with your care team if you choose."
- **In-app:** The app shows a rationale dialog before requesting Health Connect permissions, explaining which data types are requested and why. Do not remove any of the five Health Connect permissions from the manifest; the app uses all of them for the import feature.
