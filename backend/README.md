# COPD Fuel – Backend (Lambda API)

Single Lambda handler for the COPD API. Deploy this as the `copd-api-handler` function and point all API Gateway routes to it.

## Routes

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | / | No | Health check; returns `{"message":"COPD API"}`. |
| PUT | /me | JWT | Create/update current user in copd-users (call after first sign-in). |
| POST | /register | JWT | Same as PUT /me. |
| GET | /me | JWT | Current user and linked doctors. |
| DELETE | /me | JWT | Delete current user's data (all DynamoDB tables); used by "Delete my account" in the app. |
| POST | /sync | JWT | Upsert health data (weights, medications, oxygen, exercises, water, foods). |
| POST | /consent | JWT | Record consent to share with a doctor/practice. |
| POST | /link-doctor | JWT | Link patient to doctor (body: doctorId, practiceId, or inviteCode). |
| GET | /patients | JWT (doctor) | List linked patients. |
| GET | /patients/{patientId}/overview | JWT (doctor) | Patient summary (only if linked). |

## Environment variables (Lambda)

Set these in the Lambda function configuration:

- `USERS_TABLE` – default `copd-users`
- `PRACTICES_TABLE` – default `copd-practices`
- `LINKS_TABLE` – default `copd-patient-doctor-links`
- `CONSENTS_TABLE` – default `copd-consents`
- `HEALTH_DATA_TABLE` – default `copd-health-data`

## DynamoDB tables

Create these in the same region as the Lambda (see `docs/AWS_API_AND_AUTH_SETUP.md`):

- **copd-users** – PK: `userId` (String). Already created.
- **copd-practices** – PK: `practiceId` (String).
- **copd-patient-doctor-links** – PK: `patientId` (String), SK: `doctorId` (String).
- **copd-consents** – PK: `patientId` (String), SK: `consentedAt` (String).
- **copd-health-data** – PK: `patientId` (String), SK: `sk` (String), e.g. `WEIGHT#date#id`.

## Deploy

1. **Zip the Lambda code** (from your project root):

   ```bash
   cd backend/copd-api
   npm install
   zip -r ../copd-api.zip index.mjs package.json node_modules
   ```

   This creates `backend/copd-api.zip`.

2. **Upload in AWS Lambda:**
   - Open **AWS Lambda** in the same region as your API (e.g. us-east-2).
   - Open the **copd-api-handler** function.
   - **Code** tab → **Upload from** → **.zip file**.
   - Choose `copd-api.zip` (from your `backend/` folder).
   - Click **Save**.
   - Handler should stay **index.handler** (Lambda uses `index.mjs`).
   - **Environment variables**: ensure table names are set if you use non-defaults (see above).

3. **API Gateway:** Add any new routes (e.g. DELETE /me) and attach the JWT authorizer. See **docs/AWS_API_AND_AUTH_SETUP.md**.

## POST /sync body shape (from app)

Send the same shapes as the Android Room entities (ids optional; dates in ms):

```json
{
  "weights": [ { "weight": 165, "isGoal": false, "date": 1730000000000 }, { "weight": 150, "isGoal": true, "date": 1730000000000 } ],
  "medications": [ { "name": "Symbicort", "dosage": "2 puffs", "type": "daily", "date": 1730000000000 } ],
  "oxygen": [ { "level": 96, "date": 1730000000000 } ],
  "exercises": [ { "type": "Walking", "minutes": 20, "date": 1730000000000 } ],
  "water": [ { "amount": 8, "date": 1730000000000 } ],
  "foods": [ { "name": "Oatmeal", "mealCategory": "Breakfast", "quantity": "1 cup", "calories": 150, "protein": 5, "carbs": 27, "fat": 3, "date": 1730000000000 } ]
}
```

Any of the top-level arrays can be omitted or empty.
