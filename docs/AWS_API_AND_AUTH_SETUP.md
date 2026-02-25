# API Gateway routes + Cognito authorizer + DynamoDB tables

Do this after the Lambda code is deployed so the COPD API and auth work end-to-end.

---

## 1. Create remaining DynamoDB tables

In **DynamoDB** (same region as Lambda, e.g. **us-east-2**), create these tables. Encryption at rest: leave default.

| Table name | Partition key | Sort key |
|------------|----------------|----------|
| `copd-practices` | `practiceId` (String) | – |
| `copd-patient-doctor-links` | `patientId` (String) | `doctorId` (String) |
| `copd-consents` | `patientId` (String) | `consentedAt` (String) |
| `copd-health-data` | `patientId` (String) | `sk` (String) |

You already have `copd-users`.

---

## 2. Add API Gateway routes (all to same Lambda)

In **API Gateway** → **copd-health-api** → **Develop** → **Routes**:

Add these routes and point each to the **copd-api-handler** Lambda integration:

| Method | Path | Integration |
|--------|------|-------------|
| GET | /me | copd-api-handler |
| PUT | /me | copd-api-handler |
| POST | /register | copd-api-handler |
| POST | /sync | copd-api-handler |
| POST | /consent | copd-api-handler |
| POST | /link-doctor | copd-api-handler |
| GET | /patients | copd-api-handler |
| GET | /patients/{patientId}/overview | copd-api-handler |

**How to add a route (console):**

1. **Routes** → **Create**.
2. **Method**: e.g. GET. **Path**: e.g. `/me`.
3. **Integration**: Attach to existing integration **copd-api-handler**.
4. **Create**.

Repeat for each row. For `/patients/{patientId}/overview`, the path is literally `/patients/{patientId}/overview` (API Gateway will map the segment to `pathParameters.patientId`).

---

## 3. Create Cognito JWT authorizer

1. In **API Gateway** → **copd-health-api** → **Develop** → **Authorization**.
2. **Create and attach an authorization** (or **Manage authorizers** then create).
3. **Authorizer type**: **JWT**.
4. **Identity source**: `$request.header.Authorization` (or leave default if it shows that).
5. **Issuer URL (JWT)**:  
   `https://cognito-idp.us-east-2.amazonaws.com/us-east-2_qscPZox2h`  
   (use your **User pool ID** in the path; replace region if you use another region).
6. **Audience**: leave empty, or add your **App client ID** (`45i90gg5jtm20ddvq3rs6d0k8b`) if you want to restrict to that client.
7. **Create**.

---

## 4. Attach authorizer to protected routes

For each route that requires login (everything except `GET /`):

1. **Routes** → select the route (e.g. **GET /me**).
2. **Authorization**: open **Edit** or the authorization dropdown.
3. **Authorization**: select the **JWT authorizer** you created.
4. **Save**.

Do this for: **GET /me**, **PUT /me**, **POST /register**, **POST /sync**, **POST /consent**, **POST /link-doctor**, **GET /patients**, **GET /patients/{patientId}/overview**.  
Leave **GET /** without authorization so the health check works without a token.

---

## 5. Lambda permission for API Gateway

API Gateway must be allowed to invoke the Lambda. If you created the integration from the console, this is usually added automatically. If you get 403 when calling a route:

1. **Lambda** → **copd-api-handler** → **Configuration** → **Permissions**.
2. Ensure there is a **resource-based policy** that allows **api-gateway** (or **execute-api**) to **InvokeFunction** for **Source ARN** like `arn:aws:execute-api:us-east-2:ACCOUNT_ID:d2gfwfsr2a/*`.

You can add it from **API Gateway** → **Integrations** → select the Lambda integration → **Manage integration** and re-save, or add the permission manually in Lambda.

---

## 6. Optional: create user on first login

When a user signs in with Cognito for the first time, you may want to create their row in **copd-users**. Options:

The Lambda already implements **PUT /me** and **POST /register**: they upsert `copd-users` from the JWT (`sub`, `email`) and optional body (`role`, `practiceId` for doctors). Have the app call **PUT /me** or **POST /register** once after first sign-in.

---

## Quick checklist

- [ ] Tables `copd-practices`, `copd-patient-doctor-links`, `copd-consents`, `copd-health-data` created.
- [ ] Routes **GET /me**, **POST /sync**, **POST /consent**, **POST /link-doctor**, **GET /patients**, **GET /patients/{patientId}/overview** added and linked to **copd-api-handler**.
- [ ] JWT authorizer created with Cognito issuer (and optional audience).
- [ ] Authorizer attached to all routes except **GET /**.
- [ ] Lambda has API Gateway invoke permission.
- [ ] App calls **PUT /me** (or **POST /register**) after first sign-in to create/update `copd-users` row.
