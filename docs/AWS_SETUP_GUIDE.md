# AWS Setup Guide – COPD Health Tracker

Step-by-step setup for the doctor-pilot backend. Do these in order.

---

## 1. Create an AWS Account (if you don’t have one)

1. Go to [aws.amazon.com](https://aws.amazon.com) and click **Create an AWS Account**.
2. Use an email and payment method (you only pay for what you use; free tier covers a lot for a pilot).
3. Complete identity verification and choose a **support plan** (e.g. Basic).
4. **Do not use the root user for daily work.** Use it only for account-level tasks (e.g. BAA, enabling MFA on root).

---

## 2. Sign the AWS Business Associate Agreement (BAA) – required for HIPAA

1. Log in to the **AWS Console** as root (or an admin).
2. Open **AWS Artifact**: in the top search bar type **Artifact** and open it.
3. In the left menu choose **Agreements**.
4. Find **AWS Customer BAA** (Business Associate Agreement). Click it.
5. Review and **Accept** the agreement.
6. Keep a copy for your records (download if offered).

Without this, you are not in a proper HIPAA relationship with AWS for PHI.

---

## 3. Turn On MFA on the Root User (recommended)

1. Click your account name (top right) → **Security credentials**.
2. Under **Multi-factor authentication (MFA)** click **Assign MFA device**.
3. Use a virtual MFA app (e.g. Google Authenticator, Authy) and complete setup.

---

## 4. Create an IAM User for Development (recommended)

Do not use the root user to create resources. Create one IAM user (e.g. `copd-backend-admin`) for backend work.

1. In the console search bar type **IAM** and open **IAM**.
2. Left menu: **Users** → **Create user**.
3. User name: e.g. `copd-backend-admin`. Click **Next**.
4. **Set permissions**: choose **Attach policies directly**, then attach:
   - `AdministratorAccess` (for a quick pilot), **or**
   - For production: create a custom policy that only allows Cognito, API Gateway, Lambda, DynamoDB, CloudWatch, and the specific actions you need.
5. Click **Next** → **Create user**.
6. Open the user → **Security credentials** tab → **Create access key**.
7. Choose **Command Line Interface (CLI)**; confirm and **Create access key**.
8. **Save the Access Key ID and Secret Access Key** somewhere safe (e.g. password manager). You won’t see the secret again.

Use this user (and its keys) for AWS CLI and any automation. Never commit keys to git.

---

## 5. Install and Configure the AWS CLI (optional but useful)

1. Install CLI v2: [Install or update the AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html).
2. Configure a profile:
   ```bash
   aws configure --profile copd-backend
   ```
   Enter the Access Key ID and Secret Access Key from step 4; choose a default region (e.g. `us-east-1`).
3. Test:
   ```bash
   aws sts get-caller-identity --profile copd-backend
   ```

---

## 6. Choose an AWS Region

Pick one region and use it for all pilot resources (Cognito, API Gateway, Lambda, DynamoDB). Examples:

- **us-east-1** (N. Virginia) – most services, often default.
- **us-west-2** (Oregon) – also broad support.

Use the same region everywhere so services can talk without cross-region setup.

---

## 7. Create a Cognito User Pool (auth for app and doctors)

1. In the console search for **Cognito** → **Create user pool**.
2. **Sign-in options**: choose **Email** (or **Email and username** if you prefer). Click **Next**.
3. **Security requirements**: set password policy (e.g. min 8 chars); enable **MFA** as **Optional** (required for doctor accounts can be enforced later). Click **Next**.
4. **Sign-up experience**: leave self-registration on if patients will sign up from the app. Add custom attributes if needed:
   - e.g. `custom:role` (String) for `patient` | `doctor`
   - e.g. `custom:practiceId` (String) for doctors
   Click **Next**.
5. **Message delivery**: use **Send email with Cognito** (or SES later for production). Click **Next**.
6. **Integrations**: keep **Create a new IAM role** for Cognito to send email. Click **Next**.
7. **User pool name**: e.g. `copd-health-tracker-users`. Click **Next** → **Create user pool**.
8. Note down:
   - **User pool ID** (e.g. `us-east-1_xxxxxxxxx`)
   - **Region**

Create an **App client** for the mobile app:

1. In the user pool → **App integration** → **App clients and analytics** → **Create app client**.
2. Name: e.g. `copd-mobile-app`. **Authentication flows**: enable **ALLOW_USER_PASSWORD_AUTH** and **ALLOW_REFRESH_TOKEN_AUTH** (or **ALLOW_USER_SRP_AUTH** if you use SRP). **Create app client**.
3. Note **Client ID** (and Client secret if you chose to generate one).

You’ll use User pool ID + Client ID in the Android and iOS apps.

**Your COPD Health Tracker credentials (save these):**

| Item | Value |
|------|--------|
| Region | `us-east-2` (Ohio) |
| User pool ID | `us-east-2_qscPZox2h` |
| App client ID | `45i90gg5jtm20ddvq3rs6d0k8b` |
| Discovery/issuer URL | `https://cognito-idp.us-east-2.amazonaws.com/us-east-2_qscPZox2h` |

When you add login to the Android/iOS apps, you’ll configure the SDK with the User pool ID and Client ID, and add your app’s callback URL (e.g. `copdhealthtracker://callback`) to the Cognito app client’s **Allowed callback URLs** in the console.

**iOS (COPDFuel):** The Cognito “Quick setup guide” for **iOS** uses AppAuth-iOS (Carthage), the same issuer and client ID above, and a redirect URI. For the real app, use a custom URL scheme (e.g. `copdhealthtracker://callback`) as `redirectURI` and add it to the app client’s Allowed callback URLs; set `logoutURL` to your app’s sign-out redirect (e.g. the same scheme). The sample’s `https://d84l1y8p4kdic.cloudfront.net` is the Hosted UI default—replace it with your app’s scheme when you implement login.

---

## 8. Create DynamoDB Tables (data store)

Create tables in the same region as Cognito. Encryption at rest is on by default for DynamoDB.

**Option A – Console**

1. Search **DynamoDB** → **Create table**.
2. Create these one by one (names and keys only for now; add attributes when you write Lambda):

| Table name            | Partition key      | Sort key (optional) | Notes                    |
|-----------------------|--------------------|----------------------|--------------------------|
| `copd-users`          | `userId` (String)   | –                    | User profile, role       |
| `copd-practices`      | `practiceId` (String) | –                  | Practice info            |
| `copd-patient-doctor-links` | `patientId` (String) | `doctorId` (String) | Who can see whom         |
| `copd-consents`       | `patientId` (String) | `consentedAt` (String) | Consent records      |
| `copd-health-data`    | `patientId` (String) | `sk` (String)      | e.g. `WEIGHT#date#id`     |

3. For each table: **Table settings** → leave **Default encryption** (AWS owned key or AWS managed key) enabled.

**Option B – AWS CLI (same region)**

```bash
aws dynamodb create-table --table-name copd-users \
  --attribute-definitions AttributeName=userId,AttributeType=S \
  --key-schema AttributeName=userId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --profile copd-backend --region us-east-1
```

Repeat for other tables with their partition/sort keys; use `PAY_PER_REQUEST` for pilot to avoid managing capacity.

---

## 9. Create Lambda Function and API Gateway (API)

Start with one Lambda and one HTTP API so you have a working endpoint.

**Lambda**

1. Search **Lambda** → **Create function**.
2. Name: e.g. `copd-api-handler`. Runtime: **Node.js 20.x** or **Python 3.12**.
3. Create a new role with basic Lambda permissions. Click **Create function**.
4. In the function: **Configuration** → **Permissions** → open the role. Attach policies so the role can:
   - `dynamodb:GetItem`, `PutItem`, `Query`, `Scan`, `UpdateItem`, `DeleteItem` on your `copd-*` tables.
   - (Later) `cognito-idp:AdminGetUser` if you need to look up user attributes.
5. In **Code**: replace the default handler with a simple response (e.g. `{ "statusCode": 200, "body": "{\"message\":\"COPD API\"}" }`). **Deploy**.

**API Gateway**

1. Search **API Gateway** → **Create API** → **HTTP API** → **Build**.
2. Name: e.g. `copd-health-api`. Click **Next**.
3. **Integrations**: **Add integration** → **Lambda** → select `copd-api-handler`, give it a name. Click **Next**.
4. **Configure routes**: Route: `GET /`, Integration: your Lambda. Click **Next**.
5. **Stages**: leave **$default**. Click **Next** → **Create**.
6. Note the **Invoke URL** (e.g. `https://xxxxxxxxxx.execute-api.us-east-2.amazonaws.com`).

**Your API base URL:** `https://d2gfwfsr2a.execute-api.us-east-2.amazonaws.com` (save this for the app and doctor portal).

Test in a browser or with curl: `curl https://d2gfwfsr2a.execute-api.us-east-2.amazonaws.com/` – you should see `{"message":"COPD API"}`.

---

## 10. Lock Down and Harden (before any PHI)

- **Lambda**: Ensure the handler never logs request/response bodies that could contain PHI. Log only IDs and high-level events.
- **API Gateway**: Use **Cognito user pool** as authorizer for routes that touch PHI (add authorizer in API Gateway and require JWT).
- **DynamoDB**: Confirm encryption at rest is enabled (default). Use IAM (no static keys in code).
- **Environment**: Store API URL and Cognito User pool ID / Client ID in app config (environment variables or config file), not hardcoded.

---

## 11. Next Steps (after setup)

- Implement **sync** and **consent** endpoints in Lambda (see `AWS_BACKEND_ARCHITECTURE.md`).
- Add **Cognito authorizer** to API Gateway for `/sync`, `/me`, `/patients`, etc.
- In the Android and iOS apps: add login screen, call Cognito to get JWT, then call your API with the JWT.
- Build the **doctor portal** (e.g. React/Vue) that signs in with Cognito and calls `GET /patients` and `GET /patients/:id/overview`.

---

## What's next (after setup)

You now have: BAA, IAM user, Cognito (user pool + app client), DynamoDB `copd-users`, Lambda `copd-api-handler`, and API Gateway with **GET /** and an Invoke URL.

**Next phases (in order):**

1. **Implement the API** – In `copd-api-handler`, add routes/handlers for:
   - `POST /sync` – receive health data from the app (weights, meds, oxygen, etc.).
   - `GET /me` – return current user and linked doctors (requires Cognito JWT).
   - `POST /consent` – record patient consent to share with a doctor.
   - `POST /link-doctor` – link patient to doctor (e.g. by invite code).
   - `GET /patients` – (doctor only) list linked patients.
   - `GET /patients/:patientId/overview` – (doctor only) patient summary.
   Use the data model and API shape in `docs/AWS_BACKEND_ARCHITECTURE.md`.

2. **Secure the API** – Add a **Cognito user pool authorizer** in API Gateway for the routes above so only signed-in users (and the right role) can call them.

3. **Add login to the apps** – In Android and iOS, add sign-in/sign-up with Cognito (User pool ID + Client ID), then call the API with the JWT in the `Authorization` header.

4. **Doctor portal** – Build a small web app (e.g. React) that signs in with Cognito (doctor role), then calls `GET /patients` and `GET /patients/:id/overview` using your API base URL.

5. **Optional** – Create the remaining DynamoDB tables (`copd-practices`, `copd-patient-doctor-links`, `copd-consents`, `copd-health-data`) when you implement the sync and doctor endpoints.

---

## HIPAA and compliance

For the doctor pilot you operate as a business associate. After the technical setup, complete the steps in **docs/HIPAA_COMPLIANCE_CHECKLIST.md**: practice BAAs, privacy policy, breach procedure, data retention/deletion, in-app consent before linking to a doctor, and MFA for doctor accounts. The checklist is not legal advice; have a healthcare attorney review.

---

## Quick Checklist

- [ ] AWS account created
- [ ] BAA accepted in AWS Artifact
- [ ] MFA on root user
- [ ] IAM user for development (no root for daily use)
- [ ] AWS CLI configured (optional)
- [ ] One region chosen (e.g. us-east-1)
- [ ] Cognito user pool + app client created (IDs saved)
- [ ] DynamoDB tables created with encryption at rest
- [ ] Lambda created and role has DynamoDB (and Cognito if needed) permissions
- [ ] API Gateway HTTP API created and linked to Lambda; invoke URL saved
- [ ] Ready to implement auth and PHI endpoints per architecture doc
- [ ] HIPAA: complete **docs/HIPAA_COMPLIANCE_CHECKLIST.md** (practice BAAs, privacy policy, breach procedure, consent, MFA for doctors)
