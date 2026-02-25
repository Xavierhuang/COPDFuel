# HIPAA Compliance Checklist – COPD Health Tracker (Doctor Pilot)

Use this checklist to stay compliant when you operate as a **business associate** for the doctor pilot. Have a healthcare attorney review before going live.

---

## 1. Business Associate Agreement (BAA)

- [ ] **AWS BAA** – Signed and accepted in AWS Artifact (account-specific). Keep a copy on file.
- [ ] **Practice BAAs** – Each pilot practice (covered entity) must sign a BAA with you before you receive or process their patients’ PHI. Use their template or have counsel draft one. Store signed copies securely.
- [ ] BAA must cover: permitted uses of PHI, safeguards, breach notification, no re-use/re-sale of PHI, return/destruction of PHI when the relationship ends.

---

## 2. Technical Safeguards (Your Stack)

- [ ] **HTTPS only** – All API traffic over TLS (API Gateway enforces this).
- [ ] **Encryption at rest** – DynamoDB tables use default encryption (AWS managed keys). Do not disable.
- [ ] **Access control** – Only authenticated users (Cognito JWT). Doctors see only patients linked via `copd-patient-doctor-links`; Lambda enforces this on every doctor endpoint.
- [ ] **No PHI in logs** – Lambda must never log request/response bodies or any identifiers that could be used to identify a patient (only log high-level errors, e.g. `err.message`). CloudWatch log retention: set to a reasonable period (e.g. 30–90 days) and do not log PHI.
- [ ] **HIPAA-eligible services only** – Use only AWS services designated as HIPAA-eligible and within the scope of the AWS BAA (Cognito, API Gateway, Lambda, DynamoDB as used in this project are eligible when configured per AWS docs).
- [ ] **MFA for doctor accounts** – In Cognito, encourage or require MFA for users who act as doctors (e.g. enable MFA and require it for users with `custom:role = doctor` or a separate doctor user pool).

---

## 3. Administrative Safeguards

- [ ] **Privacy policy** – Publish a privacy policy that clearly states: what PHI you collect (e.g. weight, medications, oxygen, exercise, food, hydration); that data is stored in the cloud (AWS); that when the user links to a doctor, data is shared with that practice; how you protect data (encryption, access control); and how users can request access, correction, or deletion. Link it in the app and doctor portal.
- [ ] **Breach notification procedure** – Document: (1) who is responsible for assessing a suspected breach, (2) how you will notify affected individuals and the covered entity (practice) within required timeframes, (3) how you will document the incident. HIPAA requires notification without unreasonable delay (generally within 60 days). Have contact info for each practice’s privacy/security contact.
- [ ] **Data retention and deletion** – Define how long you keep PHI (e.g. while the account is active plus X years after last activity, or as required by the BAA). Document how a user or practice can request deletion; implement a process (e.g. delete from DynamoDB and Cognito) and respond within a reasonable time.
- [ ] **Minimum necessary** – Collect and use only the PHI necessary for the pilot (tracking and sharing with the linked doctor). Do not use PHI for marketing, ads, or unrelated purposes.

### How to do this (Administrative Safeguards)

1. **Privacy policy**  
   Use **docs/PRIVACY_POLICY_TEMPLATE.md**: replace all [bracketed] placeholders (your name, contact email, retention wording), have a lawyer review, then publish the final text on a webpage (e.g. your site or a static page). Add a link in the app (e.g. Settings or sign-up screen) and in the doctor portal (e.g. footer) to that URL.

2. **Breach notification procedure**  
   Use **docs/BREACH_NOTIFICATION_PROCEDURE.md**: fill in the roles (who assesses breaches, who is technical contact), add your pilot practices to the contact table, and keep the doc in a secure place your team can access. Train anyone who might handle an incident.

3. **Data retention and deletion**  
   Use **docs/DATA_RETENTION_AND_DELETION.md**: decide retention (e.g. "while account active, delete within 30 days of request"), add that to your privacy policy and to a short internal "Data retention policy." For deletion: either run the manual process described there (Cognito + DynamoDB) for each request, or add a **DELETE /me** (or **POST /account/delete**) API that deletes the authenticated user’s data and Cognito user, then add "Delete my account" in the app that calls it after confirmation.

4. **Minimum necessary**  
   Add a one-line internal policy: "We collect and use PHI only for providing the app and sharing with the user’s chosen practice; we do not use PHI for marketing, advertising, or unrelated purposes." Enforce by not adding analytics or ads that use PHI, and by not sharing data except as described in the privacy policy.

---

## 4. In-App and In-Portal Behavior

- [ ] **Consent before sharing** – Before a patient links to a doctor, the app must show clear consent: e.g. “I agree to share my COPD Health Tracker data with [Practice name]. I have read the Privacy Policy.” Record consent (e.g. via `POST /consent`) with a timestamp before completing `POST /link-doctor`.
- [ ] **Disclosure** – Users should be told in the app that when they sign in and link to a doctor, their health data is stored in the cloud and shared with that practice.
- [ ] **Secure auth** – Passwords and sessions via Cognito; JWT in `Authorization` header only; no PHI in URLs or query params.

---

## 5. Documentation and Audits

- [ ] **Safeguards documentation** – Keep a short document (e.g. this checklist plus a one-pager) describing your technical and administrative safeguards. Practices or auditors may ask for it.
- [ ] **Subcontractors** – AWS is your infrastructure provider and is covered by your AWS BAA. If you add other vendors that handle PHI (e.g. email, analytics), you need BAAs or written assurances from them.
- [ ] **Training** – Anyone on your team who handles PHI or manages the system should complete basic HIPAA training and understand breach notification and minimum necessary.

---

## 6. Quick Reference – What You Have Today

| Item | Status |
|------|--------|
| AWS BAA | Signed (Artifact) |
| HTTPS / encryption at rest | API Gateway + DynamoDB defaults |
| Access control (JWT + link check) | Lambda enforces per-route |
| No PHI in Lambda logs | Only `err.message` logged |
| Practice BAAs | You must obtain with each pilot practice |
| Privacy policy | You must publish (see outline below) |
| Breach procedure | You must document and follow |
| Retention/deletion | You must define and implement |
| In-app consent before link | Implement in app (call POST /consent then POST /link-doctor) |
| MFA for doctors | Configure in Cognito (recommended) |

---

## Privacy Policy Outline (for your lawyer to finalize)

- **Who we are** – COPD Health Tracker / [Your entity name].
- **What we collect** – Account info (email, password via Cognito); health data you enter (weight, medications, oxygen, exercise, food, hydration, etc.).
- **How we use it** – To provide the app and, if you link to a doctor, to share your data with that practice for your care.
- **Where it is stored** – Securely on AWS (encryption in transit and at rest).
- **Sharing** – We share your health data only with the practice(s) you explicitly link to, after you consent.
- **Security** – Encryption, access controls, no use of your data for advertising or selling.
- **Your rights** – Access, correction, deletion; how to request (e.g. email/contact form); timeline for response.
- **Retention** – How long we keep data; what happens when you delete your account or request deletion.
- **Breach** – We will notify you and affected practices as required by law if a breach occurs.
- **Changes** – We may update the policy; how we will notify you (e.g. in-app or email).
- **Contact** – Your contact info for privacy questions and requests.

---

## After You Complete This

1. Get BAAs signed with each pilot practice before going live.
2. Publish the privacy policy and link it in the app and doctor portal.
3. Document breach procedure and retention/deletion; train anyone who handles PHI.
4. Enable MFA for doctor accounts in Cognito.
5. In the app, implement the consent screen and call `POST /consent` before `POST /link-doctor`.

This checklist is not legal advice. Work with a healthcare attorney for BAAs, privacy policy, and breach procedures.
