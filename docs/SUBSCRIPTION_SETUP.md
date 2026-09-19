# Subscription Model Setup (Google Play)

Your app already has subscription code: **BillingManager**, **PaywallActivity**, and the product ID **copdfuel_premium_monthly**. To make it work end-to-end, you need to create and configure the subscription in Google Play Console.

---

## 1. Google Play Console – Create the subscription product

1. Open [Google Play Console](https://play.google.com/console) and select your app (COPD Fuel).
2. In the left menu go to **Monetize** > **Subscriptions** (or **Monetize** > **Products** > **Subscriptions**).
3. Click **Create subscription**.
4. **Product ID:** Enter exactly:
   ```
   copdfuel_premium_monthly
   ```
   This must match `SUBSCRIPTION_PRODUCT_ID` in `android/app/build.gradle`. If you use a different ID in Play Console, change it in `build.gradle` and rebuild.
5. **Name:** e.g. "COPD Fuel Premium (Monthly)" – shown in Play billing.
6. **Description:** Short description for the subscription (e.g. "Full access to COPD Fuel premium features").
7. Click **Save** and continue.

---

## 2. Set base plan and price

1. Under the new subscription, add a **Base plan** (e.g. "Monthly").
2. **Base plan ID:** e.g. `monthly` (internal ID; users do not see it).
3. **Billing period:** Monthly (or choose yearly if you add a second plan).
4. **Price:** Set the price in each country/region (or use "Set default price" then adjust).
5. **Free trial (optional):** e.g. 7-day or 1-month free trial.
6. **Grace period (optional):** How long to keep access after payment fails before pausing/canceling.
7. **Account hold (optional):** Whether to allow a short hold before canceling when payment fails.
8. Save the base plan and activate the subscription.

---

## 3. Activate the subscription

1. In the subscription’s detail page, set status to **Active**.
2. Until the subscription is **Active**, the app will not be able to sell it (product details may be empty or purchase will fail).

---

## 4. App side (already done)

Your project already has:

| Piece | Location |
|-------|----------|
| Product ID | `android/app/build.gradle` → `SUBSCRIPTION_PRODUCT_ID` = `copdfuel_premium_monthly` |
| Billing logic | `BillingManager.kt` – connect, query product, launch flow, acknowledge, restore |
| Paywall screen | `PaywallActivity.kt` – Subscribe and Restore buttons |
| Entry point | Profile – "Upgrade to Premium" / paywall when not premium |

Ensure the app is uploaded to Play Console (at least to an internal or closed test track) so the subscription product can be linked to the app. Subscriptions only work for builds that are published to a track (internal/closed/open/production).

---

## 5. Testing subscriptions

1. **License testers:** In Play Console go to **Setup** > **License testing**. Add the Gmail accounts that will test purchases. For those accounts, purchases are simulated and not charged.
2. **Internal testing:** Upload a build to the **Internal testing** track and install from the Play Console link. Only license testers (and testers you add) can install.
3. **Test purchase:** In the app, open Profile, tap Upgrade/Premium, then Subscribe. Complete the test purchase. Use **Restore** to verify that an existing subscription is recognized.

---

## 6. Optional – Server-side verification (recommended for production)

Right now the app treats "premium" based on the device: if Play Billing says the user has an active subscription, the app sets premium locally. For stronger control (e.g. syncing premium across devices or with a backend), you can verify purchases on your server:

1. **Real-time developer notifications (RTDN):** In Play Console, **Monetize** > **Monetization setup** (or app **Settings**), set a **Google Cloud Pub/Sub topic** for real-time notifications. When a user subscribes, renews, or cancels, Google sends a message to that topic. Your backend subscribes to the topic and updates your database (e.g. "user X is premium until date Y").
2. **Google Play Developer API:** Your backend can call the [Purchases.subscriptions](https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptions) API with the user’s purchase token (obtained from the app after purchase) to check whether the subscription is active. You need a **service account** with access to the Play Console and the API enabled.

Implementing RTDN or the Developer API is optional for launch but recommended for production and for granting premium on other platforms (e.g. doctor portal) based on Play subscription status.

---

## 7. Checklist

- [ ] Subscription created in Play Console with product ID **copdfuel_premium_monthly**
- [ ] Base plan added (e.g. monthly) with price and optional trial
- [ ] Subscription set to **Active**
- [ ] App uploaded to at least Internal testing track
- [ ] License testers added; test Subscribe and Restore on a device
- [ ] (Optional) PRIVACY_POLICY_URL and Delete data URL updated to your live URLs in store listing and app

---

## 8. Changing the product ID or adding more plans

- **Different product ID:** Change `SUBSCRIPTION_PRODUCT_ID` in `android/app/build.gradle` to match the new ID, then rebuild.
- **Multiple plans (e.g. monthly and yearly):** Create two subscriptions in Play Console (e.g. `copdfuel_premium_monthly` and `copdfuel_premium_yearly`). In the app you can either:
  - Query both product IDs in `BillingManager` and show both options on the paywall, or
  - Keep only one product ID in the app and use a single subscription with multiple base plans (monthly and yearly) under the same product; then in code use the first available offer or let the user choose by offer token.

Your current code uses a single product ID and the first offer token, which is correct for one subscription with one base plan.
