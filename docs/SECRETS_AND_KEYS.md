# Secrets and API Keys

API keys were removed from the codebase to resolve GitHub secret detection alerts. Configure them locally and rotate any keys that were ever committed.

## Action required: rotate exposed keys

These keys were previously in the repo and may have been exposed:

1. **Google Places API key** (Places API, Geocoding) – used in Android app and my-copd-app for "Programs Near Me".
2. **USDA FoodData Central API key** – used in Android app and my-copd-app for food search.

**You must:**

- **Google Cloud Console:** Go to [APIs & Services > Credentials](https://console.cloud.google.com/apis/credentials). Delete or regenerate the exposed API key. Create a new key and restrict it (HTTP referrer, app bundle ID, or IP) to limit abuse.
- **USDA FDC:** Log in at [fdc.nal.usda.gov](https://fdc.nal.usda.gov) (API key section). Revoke the old key and create a new one.

Use the **new** keys only in local config (below); never commit them.

---

## Where to configure keys (no secrets in repo)

### Android app

1. Copy `android/local.properties.example` to `android/local.properties` (or create `android/local.properties`).
2. Add (with your real keys after rotating):
   ```
   GOOGLE_PLACES_API_KEY=your_new_google_places_key
   USDA_FDC_API_KEY=your_new_usda_key
   ```
3. `local.properties` is in `.gitignore`; it will not be committed.

Without these keys: Programs Near Me uses sample data only; USDA food search in Add Food is disabled (local database still works).

### my-copd-app (Expo)

1. Copy `my-copd-app/.env.example` to `my-copd-app/.env`.
2. Set (with your new keys):
   ```
   EXPO_PUBLIC_GOOGLE_PLACES_API_KEY=your_new_google_places_key
   EXPO_PUBLIC_USDA_API_KEY=your_new_usda_key
   ```
3. `.env` is in `.gitignore`; it will not be committed.

Without these keys: Programs Near Me uses sample data; food search will show "USDA API key not configured" if USDA is used.

---

## What stays in the repo (OK to be public)

- **API base URL** (e.g. `https://d2gfwfsr2a.execute-api.us-east-2.amazonaws.com`) – this is the public endpoint; access is controlled by Cognito JWT.
- **Cognito User Pool ID** in docs – identifies the pool; authentication still requires valid user credentials.

If GitHub or another scanner still flags the API base URL or pool ID, you can move those to config as well; the main fix was removing the two API keys above.
