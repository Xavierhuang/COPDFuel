# Account Deletion Process (Internal)

**Purpose:** How users can request account deletion and how to process requests. Aligns with the privacy policy (support@copdfuel.com, response within 30 days).

---

## 1. How users can request deletion

- **Email:** Users may email **support@copdfuel.com** with the subject "Delete my account" or "Account deletion request." They should use the email address associated with their account.
- **In-app:** If "Delete my account" is available in the app (Profile), the user can confirm there; the app calls the API to delete their data and they are signed out. You may still need to remove the user from Cognito if the API does not do it (see below).

---

## 2. When you receive an email request

1. **Confirm identity** – Verify the request is from the account owner (e.g. reply to the same email, or send a confirmation link to the account email). Do not delete based on a request from a different address without additional verification.
2. **Find the user** – In AWS Cognito (User Pools), find the user by email to get their **User sub** (this is the `userId` used in DynamoDB).
3. **Run the deletion process** – Follow Section 3 below (DynamoDB + Cognito).
4. **Confirm in writing** – Reply to the user within 30 days: "Your account and associated health data have been deleted. You will no longer be able to sign in. If you have any questions, contact support@copdfuel.com." If you retained anything (e.g. for legal or BAA reasons), say what and why.
5. **Document** – Keep a brief record (date received, date completed, email used) for compliance; do not store PHI in the record.

---

## 3. Manual deletion steps (DynamoDB + Cognito)

Use this when processing an email request, or when the in-app "Delete my account" has run but Cognito deletion is done separately.

**Prerequisite:** You need the user's **userId** (Cognito sub). Get it from Cognito User Pools (find user by email; the sub is in the user detail).

### 3.1 DynamoDB

Delete all data for that `userId`:

1. **copd-users** – Delete item with PK `userId`.
2. **copd-consents** – Query by `patientId` = `userId`; delete each returned item.
3. **copd-patient-doctor-links** – Query by `patientId` = `userId`; delete each item. (If the user was a doctor, also query by `doctorId` = `userId` and delete those links.)
4. **copd-health-data** – Query by `patientId` = `userId`; delete each item (may be many; use pagination if needed).

You can do this in AWS Console (DynamoDB → Tables → Explore items → query/delete) or with a one-off script. Do not log PHI.

### 3.2 Cognito

- In **Cognito** → User Pools → your pool → Users, find the user (by email or sub) and **Delete user**. This prevents them from signing in again.

---

## 4. If you use the DELETE /me API

The API (DELETE /me) deletes the caller's data from DynamoDB and, if configured, deletes the user from Cognito. After a successful call:

- The user's app data is removed.
- If the Lambda has Cognito configured, the user is also removed from the user pool; the app will sign them out.
- If Cognito is not configured in the Lambda, run the Cognito step in Section 3.2 manually for that user (using the same userId/sub).

---

## 5. Response time

Per the privacy policy, respond to deletion requests within **30 days**. Aim to complete the deletion and send confirmation as soon as practicable.

---

*This process is internal. Have legal counsel review retention and deletion obligations under HIPAA and your BAAs.*
