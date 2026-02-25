# HIPAA Compliance: Client vs Developer Responsibilities

**Purpose:** Clear split of responsibilities so the client (practice/business owner) and the developer (you, technical lead) know who does what. Share this with your client and keep a copy on file.

---

## Summary

| Who | Responsibility |
|-----|----------------|
| **Client** | Legal, business, and administrative HIPAA obligations: BAAs, privacy policy, breach process, retention decisions, training, and any legal review. |
| **Developer** | Technical implementation: secure infrastructure, app and API behavior, no PHI in logs, access control, and features you agree to build (e.g. consent flow, sync, delete account). |

---

## What the client should do (business / legal / administrative)

The client is the one offering the app to patients and/or acting as the covered entity or engaging you as a business associate. She should own the following.

### 1. Business Associate Agreements (BAAs)

- **AWS BAA** – Ensure the AWS account used for the app has the BAA accepted in AWS Artifact. (You can confirm it’s in place; she should keep the executed copy.)
- **Practice BAAs** – If the app is used with one or more medical practices, **the client** (or her company) must get a signed BAA with each practice before PHI is shared. She can use the practice’s template or have her lawyer draft one. You do not sign BAAs with practices; she does.
- **Other vendors** – If she adds services that touch PHI (e.g. email, support tools), she is responsible for getting BAAs or written assurances from those vendors.

**You can:** Remind her that BAAs are required before go-live; point to `docs/HIPAA_COMPLIANCE_CHECKLIST.md` section 1.

---

### 2. Privacy policy

- **Draft** – You’ve provided a template: `docs/PRIVACY_POLICY_TEMPLATE.md`.
- **Client’s tasks:**  
  - Replace all [bracketed] placeholders (entity name, contact email, dates, retention wording).  
  - Have a **lawyer** (healthcare/privacy) review the final text.  
  - **Publish** the final policy on a stable, public URL (e.g. her website or a simple static page).  
  - **Tell you the URL** so you can add the link in the app and doctor portal (you handle the technical part of linking).

**You do:** Add the privacy policy URL in the app (e.g. Settings or sign-up) and in the doctor portal footer once she provides it. In this project: (1) Android: set `PRIVACY_POLICY_URL` in `android/app/build.gradle` (buildConfigField) and rebuild; the Profile screen already has a "Privacy Policy" row that opens it. (2) Doctor portal: set `VITE_PRIVACY_POLICY_URL` in `.env` and rebuild.

---

### 3. Breach notification procedure

- **Template** – `docs/BREACH_NOTIFICATION_PROCEDURE.md` describes roles and steps.
- **Client’s tasks:**  
  - Fill in who is responsible for assessing a breach, who is the technical contact (could be you), and who notifies individuals and practices.  
  - Add each pilot practice’s privacy/security contact to the contact table in that doc.  
  - Keep the procedure in a secure place her team can access.  
  - **Train** anyone who might handle an incident (including herself and any staff).

**You can:** Help define “who does the technical part” (e.g. you help determine scope of a breach); you do not own the decision to notify or the actual notification—she does.

---

### 4. Data retention and deletion policy

- **Template** – `docs/DATA_RETENTION_AND_DELETION.md` describes options.
- **Client’s tasks:**  
  - **Decide** retention (e.g. “while account is active; delete within 30 days of request”).  
  - Add that wording to the privacy policy and to a short internal “Data retention policy.”  
  - Define how users (and practices, if applicable) can request deletion (e.g. in-app, email, or form).  
  - Respond to deletion requests within the timeline she commits to (e.g. 30 days).

**You do:** Implement the technical side: e.g. manual process you run when she forwards a request, and/or a “Delete my account” feature that calls an API you build (see that doc). You do not decide the policy; she does.

---

### 5. Training and internal policies

- **Client’s tasks:**  
  - Ensure anyone on her side who handles PHI or manages the product completes basic HIPAA training.  
  - Adopt a short “minimum necessary” policy (e.g. we only collect and use PHI for the app and sharing with the user’s chosen practice; no marketing/ads on PHI).  
  - Enforce that in how the product is used and in any contracts (e.g. BAAs).

**You do:** No PHI in logs; no use of PHI for ads or analytics; technical access control as implemented. You don’t run her training; she does.

---

### 6. MFA for doctor accounts (shared)

- **Client:** Decides that doctor accounts must use MFA (recommended).
- **You:** Can enable MFA in Cognito and document the steps (or do it if she asks). She should communicate to doctors that they must use MFA.

---

## What you (the developer) do (technical)

You are responsible for building and operating the system in a HIPAA-aware way. The checklist in `docs/HIPAA_COMPLIANCE_CHECKLIST.md` reflects this; in short:

- **Infrastructure:** HTTPS only; encryption at rest (e.g. DynamoDB defaults); use only HIPAA-eligible AWS services within the scope of the AWS BAA.
- **Access control:** Authentication (e.g. Cognito); doctors only see patients they’re linked to; Lambda/API enforces this.
- **No PHI in logs:** No request/response bodies or identifiers in logs; only high-level errors (e.g. `err.message`).
- **App behavior:** Consent before linking to a doctor (e.g. “I agree to share my data with [Practice]”), then `POST /consent` and `POST /link-doctor`; optional “Delete my account” that calls your deletion API after the client has defined the policy.
- **Documentation:** Keep a short technical summary of safeguards (e.g. the checklist plus a one-pager) for the client or auditors.

You do **not** sign BAAs with practices, publish the privacy policy, decide retention wording, or send breach notifications—the client does those.

---

## Simple handoff to send your client

You can send her something like this (customize names/dates):

---

**Subject: Your action items for HIPAA compliance**

I’m responsible for the technical side of the app (security, infrastructure, and features). You’re responsible for the legal and administrative side. Here’s what I need from you before we go live:

1. **BAAs** – Get a signed BAA with each practice that will use the app. The AWS BAA for our hosting is already in place; you keep a copy of that and of each practice BAA.

2. **Privacy policy** – I’ve left a draft in our project (`docs/PRIVACY_POLICY_TEMPLATE.md`). Please fill in the [placeholders], have a lawyer review it, then publish it on your website (or a page you control) and send me the final URL so I can add the link in the app and doctor portal.

3. **Breach procedure** – Fill in `docs/BREACH_NOTIFICATION_PROCEDURE.md` (who assesses breaches, who notifies, and each practice’s contact). Train anyone on your side who might handle an incident.

4. **Data retention and deletion** – Decide how long you keep data and how users can request deletion. Put that in your privacy policy and tell me so we can align the “Delete my account” flow and any manual process.

5. **Training** – Ensure everyone on your team who touches PHI or the product completes basic HIPAA training.

I’ll handle: secure infrastructure, no PHI in logs, access control, consent flow in the app, and linking the privacy policy once you send the URL. If you want MFA for doctor accounts, say so and I’ll enable it in the system.

A more detailed split of responsibilities is in `docs/CLIENT_VS_DEVELOPER_RESPONSIBILITIES.md` in the project.

---

## Bottom line

- **Client:** BAAs, privacy policy (draft → lawyer → publish), breach procedure and contacts, retention/deletion policy and training. She owns “what we promise” and “who we tell.”
- **You:** Build and run the system securely; implement consent and deletion flows; add the privacy policy link; no PHI in logs; access control. You own “how it works” and “how it’s built.”

This is not legal advice. She should rely on a healthcare attorney for BAAs, privacy policy, and breach procedures.
