# COPD Fuel – AWS Backend Architecture

Recommended backend for the doctor pilot: **AWS**, with HIPAA-eligible services and a single BAA.

---

## Why This Stack

- **HIPAA**: Use only AWS services that are HIPAA-eligible and covered by the AWS BAA.
- **One platform**: Patient app (Android/iOS), sync API, and doctor portal all use the same account and BAA.
- **Pilot-friendly**: Start with managed services (Cognito, API Gateway, Lambda, DynamoDB/RDS) and add a doctor portal later.

---

## High-Level Architecture

```
[Android App]  ──┐
[iOS App]     ──┼──► [API Gateway] ──► [Lambda] ──► [DynamoDB]
[Doctor Portal] ─┘         │                │
                            │                └──► [Cognito] (auth)
                            └── HTTPS only, no PHI in logs
```

- **Cognito**: User pools for patients and doctors (or one pool with custom attributes `role = patient | doctor`).
- **API Gateway**: REST or HTTP API; auth via Cognito JWT.
- **Lambda**: Business logic, validation, and strict “doctor sees only their patients” access control.
- **DynamoDB** (or **RDS**): Store users, practices, patient–doctor links, consent, and synced health data.

---

## Recommended AWS Services (HIPAA-Eligible)

| Use Case | Service | Notes |
|----------|---------|--------|
| Auth | **Amazon Cognito** | User pools; MFA for doctor accounts; in BAA scope when used per AWS docs. |
| API | **API Gateway (REST or HTTP API)** | Cognito authorizer; HTTPS only. |
| Compute | **AWS Lambda** | Run sync and portal APIs; no PHI in logs. |
| Database | **Amazon DynamoDB** or **RDS (PostgreSQL)** | DynamoDB: simpler for pilot, single-table or few tables. RDS: if you prefer SQL and complex queries. Both BAA-eligible. |
| Storage (if needed) | **S3** | Encrypted bucket for exports/attachments only if required. |
| Doctor portal hosting | **Amplify Hosting** or **S3 + CloudFront** | Static site; calls same API. |

Use only HIPAA-eligible services and enable encryption (e.g. DynamoDB encryption at rest, RDS encryption, S3 default encryption). Sign and comply with the **AWS BAA**.

---

## Data Model (Minimal for Pilot)

### Core entities

- **User**  
  `userId` (Cognito sub), `email`, `role` (patient | doctor), `createdAt`.  
  Optional: `practiceId` for doctors.

- **Practice**  
  `practiceId`, `name`, `address`, optional `baaSignedAt`.  
  One practice can have many doctors.

- **PatientDoctorLink**  
  `patientId`, `doctorId` (or `practiceId`), `inviteCode` (optional), `consentAt`, `status` (pending | active | revoked).  
  Ensures a doctor only sees patients linked to them.

- **Consent**  
  `patientId`, `practiceId` or `doctorId`, `consentType` (e.g. "share_with_doctor"), `consentedAt`, optional `ip`/`userAgent` for audit.

- **Health data (synced from app)**  
  One table (or one per domain) keyed by `patientId` + sort key (e.g. `date#type#id`):
  - **Weights**: weight, isGoal, date.
  - **Medications**: name, dosage, type (daily/exacerbation).
  - **Oxygen**: level, date.
  - **Exercise**: type, minutes, date.
  - **Water**: amount, date.
  - **Food**: reuse your existing meal/category structure.

Store only what the app already has; use the same units and enums as the apps to avoid mapping bugs.

---

## API Shape (REST, suggested)

- **Auth**  
  - Sign up / Sign in via Cognito (or via API that uses Cognito behind the scenes).  
  - JWT in `Authorization` for all authenticated requests.

- **Patient**  
  - `POST /sync` – upsert health data (weights, meds, oxygen, exercise, water, food).  
  - `GET /me` – current user and linked doctors.  
  - `POST /consent` – record consent when linking to a doctor.  
  - `POST /link-doctor` – link to doctor (e.g. by invite code), after consent.

- **Doctor**  
  - `GET /patients` – list only linked patients (filter by `doctorId` from JWT).  
  - `GET /patients/:patientId/overview` – summary for one patient (only if linked).  
  - `GET /patients/:patientId/export` – optional report/export for that patient.  
  - `POST /invite` – generate invite code for a new patient (optional for pilot).

Every Lambda that touches PHI must **verify** that the requesting doctor is linked to that patient (using `PatientDoctorLink`).

---

## App Changes (High Level)

1. **Auth**  
   - Add optional “Create account / Sign in” (Cognito) so sync is tied to a stable `userId`.  
   - Keep “use offline only” possible: sync only if user is logged in and has consented to share with a doctor (if in pilot).

2. **Consent**  
   - Before linking to a doctor: show “Share my health data with [Practice name]” and record consent via `POST /consent` and store consent timestamp.

3. **Sync**  
   - When logged in and (for pilot) linked to a doctor: periodically or on-event send health data to `POST /sync` (delta or full, idempotent by `date#type#id`).

4. **Doctor portal**  
   - Separate web app (React/Vue/etc.) or simple static + API.  
   - Login with Cognito (doctor role).  
   - Calls `GET /patients` and `GET /patients/:id/overview` (and export if implemented).

---

## Security Checklist

- [ ] HTTPS only (API Gateway).
- [ ] Cognito JWT validation in Lambda; never trust client for `doctorId`/`patientId` beyond identity.
- [ ] Every doctor endpoint checks `PatientDoctorLink` before returning PHI.
- [ ] Encryption at rest (DynamoDB/RDS, S3 if used).
- [ ] No PHI in CloudWatch log payloads (log only ids and high-level events).
- [ ] MFA for doctor accounts in Cognito.
- [ ] Signed BAA with AWS; only use HIPAA-eligible services.

For a full HIPAA compliance checklist (BAAs, privacy policy, breach procedure, retention, in-app consent), see **docs/HIPAA_COMPLIANCE_CHECKLIST.md**.

---

## Implementation Order

1. **AWS account and BAA** – Enable and sign AWS BAA; create a dedicated account or OU for this project if helpful.
2. **Cognito** – User pool(s), roles (patient/doctor), and test users.
3. **DynamoDB (or RDS)** – Tables for User, Practice, PatientDoctorLink, Consent, and health sync.
4. **Lambda + API Gateway** – Auth (Cognito authorizer), `POST /sync`, `GET /me`, `POST /consent`, `POST /link-doctor`.
5. **App** – Auth screens, consent screen, sync client (only when logged in and linked).
6. **Doctor portal** – Login + `GET /patients` + `GET /patients/:id/overview`.
7. **Invite flow** (optional) – Doctor generates code, patient enters in app and consents.

This document is the single source of truth for “what to build” so the app and backend stay aligned.
