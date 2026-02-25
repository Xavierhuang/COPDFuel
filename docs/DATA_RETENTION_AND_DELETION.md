# Data Retention and Deletion Policy (Internal) + Implementation

**Purpose:** Define how long you keep PHI and how users (and practices) can request deletion, and how you implement it.

---

## 1. Retention policy (define and publish)

**Suggested wording for your privacy policy and internal policy:**

- We retain your account and health data **while your account is active**.
- After you **request account deletion**, we delete or anonymize your data within [e.g. 30] days, except:
  - Where we must retain it **by law** (e.g. certain medical records laws), or
  - Where we must retain it **under our BAA** with a practice (e.g. they may require a copy for their records).
- For **inactive accounts** (no login for [e.g. 2] years), we may [delete the account after notice / retain for X years then delete] – choose one and state it clearly.

**Internal:** Document this in a short "Data retention policy" and keep it with your other compliance docs.

---

## 2. How users can request deletion

- **In the privacy policy:** State that users can request deletion by contacting [your email] or [in-app "Delete my account" if you add it].
- **Process:** When you receive a request:
  1. Confirm the requester’s identity (e.g. same email as account, or secure link).
  2. Run the deletion process (see below).
  3. Confirm in writing that the account and associated data have been deleted (or what was retained and why).
  4. Respond within [e.g. 30] days.

---

## 3. How to implement deletion

You need to remove the user’s data from:

1. **Cognito** – Delete the user from the user pool (so they can no longer sign in).
2. **DynamoDB** – Delete all items for that user:
   - `copd-users`: delete item with PK = `userId`
   - `copd-consents`: query by `patientId` = `userId`, delete each item
   - `copd-patient-doctor-links`: query by `patientId` = `userId`, delete each item
   - `copd-health-data`: query by `patientId` = `userId`, delete each item  
   (If the user was a doctor, also remove them from any links where they are `doctorId`; usually you’d query `copd-patient-doctor-links` for that doctor and delete those rows.)

**Options:**

- **Manual (pilot):** Use AWS Console or a one-off script: look up the user by email in Cognito to get `userId` (sub), then delete from Cognito and from each DynamoDB table as above. Document the steps so anyone authorized can run them.
- **API (recommended for scale):** Add an endpoint (e.g. **DELETE /me** or **POST /account/delete**) that:
  - Requires a valid JWT.
  - Deletes the caller’s data from all tables and then deletes the user from Cognito (using Cognito AdminDeleteUser).  
  Call it only after the user confirms (e.g. in-app "Delete my account" with a second confirmation). Restrict this endpoint to the account owner (JWT sub = userId).

---

## 4. Practice requests

If a **practice** asks you to delete a patient’s data (e.g. patient left the practice): treat it like a user deletion request. Confirm authority and identity where appropriate, then run the same deletion process. Document the request and the date of deletion.

---

## 5. Checklist

- [ ] Retention period and deletion rules written down (internal policy + privacy policy).
- [ ] Privacy policy states how users can request deletion and response time.
- [ ] Deletion process implemented (manual script or DELETE /me) and tested.
- [ ] Practice contact process for deletion requests documented.

---

*Have legal counsel review retention and deletion obligations under HIPAA and your BAAs.*
