# COPD Health Tracker – Doctor Portal

Web app for doctors to view linked patients and their health overview. Uses the same Cognito user pool as the mobile apps; doctor accounts must have `custom:role` = `doctor` (set in Cognito or via PUT /me after first sign-in).

## Setup

1. `npm install`
2. Copy `.env.example` to `.env` and set:
   - `VITE_API_BASE_URL` – API Gateway base URL (default in code is the pilot URL).
   - `VITE_PRIVACY_POLICY_URL` – Public URL of your privacy policy (linked in footer).
3. Ensure Cognito app client allows this origin in "Allowed callback URLs" and "Sign out URLs" if using Hosted UI; for username/password sign-in only, no callback URL is required.

## Run

- `npm run dev` – development
- `npm run build` then `npm run preview` – production build and preview

## Routes

- `/login` – Sign in (email + password).
- `/` – List of linked patients (GET /patients).
- `/patients/:patientId` – Patient overview (GET /patients/:patientId/overview).

Footer links to your privacy policy when `VITE_PRIVACY_POLICY_URL` is set.
