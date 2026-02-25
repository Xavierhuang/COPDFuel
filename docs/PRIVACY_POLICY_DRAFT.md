# Privacy Policy – Draft

**Use this as a draft. Replace all [bracketed] placeholders with your actual information, then have a lawyer review before publishing.**  
Publish the final version on a webpage (e.g. your website or a static page) and link it from the app and doctor portal using `PRIVACY_POLICY_URL` (Android) and `VITE_PRIVACY_POLICY_URL` (doctor portal). See **Publishing** at the end of this file.

---

## Privacy Statement (summary)

We respect your privacy and are committed to protecting your personal and health information. Our app may collect basic personal details (such as name and email), health and wellness data (including activity, nutrition, and goals), and limited device information to provide personalized insights, improve performance, and support your experience. If you choose to connect third-party services like Apple Health or Google Fit, we only access the data you authorize. We do not sell your personal or health information, and we use appropriate security measures to protect your data. You may request access, correction, or deletion of your information at any time by contacting us.

---

## Privacy Policy

**Last updated:** February 15, 2025

### 1. Who we are

Ingenious Medical Solutions, LLC, operating the COPD Health Tracker application ("we," "our," or "us").

### 2. What we collect (including PHI)

- **Account information:** Email address and password (managed by our identity provider). We do not store your password on our servers.
- **Basic personal details:** Name and other contact details you provide.
- **Health and tracking data (PHI):** Information you enter in the app, including weight, medications, oxygen readings, exercise, food and nutrition, hydration, activity, and goals. This may constitute Protected Health Information (PHI) under HIPAA when shared with your healthcare provider.
- **Third-party health data:** If you connect Apple Health, Google Fit, or similar services, we only access the data you explicitly authorize.
- **Limited device information:** Information needed to provide and improve the app (e.g. device type, OS version). We use this to support your experience and improve performance.

### 3. How we use your information

- To provide the COPD Health Tracker app and its features and personalized insights.
- To improve app performance and your experience.
- When you choose to link your account to a healthcare provider (e.g. your doctor or practice), we share the health data you have agreed to share with that provider for your care. We do this only after you give explicit consent in the app.

### 4. Where your data is stored

Your data is stored securely in the cloud using Amazon Web Services (AWS), with encryption in transit (HTTPS) and encryption at rest. We use only services that support our compliance obligations.

### 5. Sharing of your information

We share your health data only with the healthcare practice(s) you explicitly link to in the app, and only after you consent. We do **not** sell your personal or health information. We do **not** use your health data for advertising or marketing. We may disclose information if required by law (e.g. court order or lawful process).

### 6. Security

We use industry-standard security measures, including encryption, access controls, and secure authentication. Only authorized users (you and, when you have linked, your chosen practice) can access your data. We require business associate agreements with practices that receive your data and with our infrastructure provider.

### 7. Your rights

You may request:

- **Access** – A copy of the personal and health data we hold about you.
- **Correction** – Correction of inaccurate data.
- **Deletion** – Deletion of your account and associated data, subject to any legal or contractual retention requirements.

To make a request, contact us at support@copdfuel.com. We will respond within 30 days. If you have linked to a practice, you may also need to contact them for data they hold.

### 8. Data retention

We retain your account and health data while your account is active. After you request account deletion, we delete or anonymize your data within 30 days, except where we must retain it by law or under our agreement with your healthcare provider. For inactive accounts (no login for 2 years), we may delete the account after sending you notice. When you request account deletion, we will delete or anonymize your data as described in our data retention and deletion policy, except where we must retain it by law.

### 9. Breach notification

If we discover a breach of your unsecured health information, we will notify you and any affected healthcare provider as required by applicable law, without unreasonable delay (generally within 60 days).

### 10. Changes to this policy

We may update this policy from time to time. We will post the updated policy and, for material changes, we will notify you by email or a notice in the app. Your continued use of the app after the effective date of changes constitutes acceptance of the updated policy.

### 11. Contact

For privacy questions, access/correction/deletion requests, or to report a concern:

- **Email:** support@copdfuel.com

---

*This policy is not legal advice. Have a healthcare or privacy attorney review it before publication.*

---

## Publishing

1. Replace all [bracketed] placeholders (date, company name, contact email, address, retention options).
2. Have a lawyer review the final text.
3. Publish the policy on a stable webpage and note the URL.
4. **Android app:** In `android/app/build.gradle`, set:
   - `buildConfigField "String", "PRIVACY_POLICY_URL", "\"https://your-domain.com/privacy\""`
   - The app already links to this URL from Profile (Privacy Policy item).
5. **Doctor portal:** Set the environment variable (or `.env`):
   - `VITE_PRIVACY_POLICY_URL=https://your-domain.com/privacy`
   - The portal footer already shows a "Privacy Policy" link when this is set.
