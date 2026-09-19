# Signing the release build for Google Play

Google Play requires that every uploaded app bundle (AAB) is **signed**. Your project is set up to sign the release build with a keystore you create and register in `local.properties`.

---

## 1. Create a keystore (one-time)

Run this in a terminal. Use a **secure password** and store it somewhere safe (e.g. password manager). You need this key for all future updates.

```bash
keytool -genkey -v -keystore copdfuel-upload.keystore -alias copdfuel -keyalg RSA -keysize 2048 -validity 10000
```

- **Keystore password:** choose a strong password (you’ll use it as `RELEASE_STORE_PASSWORD`).
- **Key password:** use the same for simplicity (you’ll use it as `RELEASE_KEY_PASSWORD`).
- Fill in name, org, etc. (they can be generic if you prefer).

Save the `.keystore` file in a safe place **outside** the project (e.g. `~/keystores/copdfuel-upload.keystore`). Do **not** commit it to git.

---

## 2. Add signing config to local.properties

Open **`android/local.properties`** (create it from `local.properties.example` if needed). Add these lines with **your** path and passwords:

```properties
RELEASE_STORE_FILE=/absolute/path/to/copdfuel-upload.keystore
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=copdfuel
RELEASE_KEY_PASSWORD=your_key_password
```

- **RELEASE_STORE_FILE:** Full path to the `.keystore` file (e.g. `/Users/you/keystores/copdfuel-upload.keystore`). On Windows use backslashes or a path like `C:\\path\\to\\copdfuel-upload.keystore`.
- **RELEASE_KEY_ALIAS:** The alias you used when creating the keystore (e.g. `copdfuel`).

`local.properties` is in `.gitignore`; do not commit it.

---

## 3. Build a signed release bundle

From the project root:

```bash
cd android ; ./gradlew bundleRelease
```

The signed AAB is at:

**`android/app/build/outputs/bundle/release/app-release.aab`**

Upload this file in Play Console (Test and release > Internal testing > Create new release > Upload).

---

## 4. Google Play App Signing

If you use **Google Play App Signing** (recommended):

- The key you created above is your **upload key**. You sign the AAB with it and upload to Play.
- Google keeps the **app signing key** and re-signs the app for users.
- If you have not set up Play App Signing yet, the first time you upload a signed bundle, Play may prompt you to enroll. You can then upload your upload certificate (from your keystore) or let Play use the first upload as the certificate.

**Back up your keystore and passwords.** If you lose them, you cannot sign updates for this app with the same key.
