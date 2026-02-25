/**
 * COPD Health Tracker API – single Lambda handler for HTTP API (API Gateway v2).
 * Routes: GET /, GET /me, POST /sync, POST /consent, POST /link-doctor, GET /patients, GET /patients/:patientId/overview.
 * Set env: USERS_TABLE, PRACTICES_TABLE, LINKS_TABLE, CONSENTS_TABLE, HEALTH_DATA_TABLE.
 */

import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import {
  DynamoDBDocumentClient,
  GetCommand,
  PutCommand,
  QueryCommand,
  ScanCommand,
} from '@aws-sdk/lib-dynamodb';

const client = new DynamoDBClient({});
const doc = DynamoDBDocumentClient.from(client);

const USERS_TABLE = process.env.USERS_TABLE || 'copd-users';
const PRACTICES_TABLE = process.env.PRACTICES_TABLE || 'copd-practices';
const LINKS_TABLE = process.env.LINKS_TABLE || 'copd-patient-doctor-links';
const CONSENTS_TABLE = process.env.CONSENTS_TABLE || 'copd-consents';
const HEALTH_DATA_TABLE = process.env.HEALTH_DATA_TABLE || 'copd-health-data';

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'Content-Type,Authorization',
  'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
};

function json(statusCode, body, headers = {}) {
  return {
    statusCode,
    headers: { 'Content-Type': 'application/json', ...CORS_HEADERS, ...headers },
    body: JSON.stringify(body),
  };
}

function getClaims(event) {
  const auth = event.requestContext?.authorizer?.jwt?.claims;
  if (auth) return auth;
  const header = event.headers?.authorization || event.headers?.Authorization;
  if (!header || !header.startsWith('Bearer ')) return null;
  try {
    const token = header.slice(7);
    const payload = JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
    return { sub: payload.sub, email: payload.email, 'cognito:username': payload['cognito:username'] };
  } catch {
    return null;
  }
}

function getUserId(event) {
  const claims = getClaims(event);
  return claims?.sub || null;
}

function getRole(claims) {
  return claims?.['custom:role'] || claims?.role || 'patient';
}

export const handler = async (event) => {
  const method = event.requestContext?.http?.method || event.httpMethod || 'GET';
  const path = event.requestContext?.http?.path || event.path || '/';
  const pathParams = event.pathParameters || {};
  const body = event.body ? (typeof event.body === 'string' ? JSON.parse(event.body) : event.body) : {};

  if (method === 'OPTIONS') {
    return { statusCode: 204, headers: CORS_HEADERS, body: '' };
  }

  const pathNorm = path.replace(/^\/patients\/[^/]+/, '/patients/:patientId');
  const route = `${method} ${pathNorm}`;

  try {
    if (route === 'GET /') {
      return json(200, { message: 'COPD API' });
    }

    if (route === 'PUT /me' || route === 'POST /register') {
      const userId = getUserId(event);
      if (!userId) return json(401, { error: 'Unauthorized' });
      const claims = getClaims(event);
      const email = claims?.email || body.email || '';
      const role = body.role || claims?.['custom:role'] || 'patient';
      const practiceId = body.practiceId || claims?.['custom:practiceId'] || '';
      await doc.send(new PutCommand({
        TableName: USERS_TABLE,
        Item: {
          userId,
          email,
          role,
          practiceId: role === 'doctor' ? practiceId : '',
          createdAt: new Date().toISOString(),
        },
      }));
      return json(200, { ok: true, userId });
    }

    if (route === 'GET /me') {
      const userId = getUserId(event);
      if (!userId) return json(401, { error: 'Unauthorized' });
      const user = await doc.send(new GetCommand({
        TableName: USERS_TABLE,
        Key: { userId },
      })).then(r => r.Item);
      const links = await doc.send(new QueryCommand({
        TableName: LINKS_TABLE,
        KeyConditionExpression: 'patientId = :pid',
        ExpressionAttributeNames: { '#s': 'status' },
        ExpressionAttributeValues: { ':pid': userId, ':active': 'active' },
        FilterExpression: '#s = :active',
      })).then(r => r.Items || []);
      return json(200, {
        userId,
        email: user?.email,
        role: user?.role || 'patient',
        linkedDoctors: links.map(l => ({ doctorId: l.doctorId, practiceId: l.practiceId })),
      });
    }

    if (route === 'POST /sync') {
      const userId = getUserId(event);
      if (!userId) return json(401, { error: 'Unauthorized' });
      const { weights, medications, oxygen, exercises, water, foods } = body;
      const now = Date.now();
      const table = HEALTH_DATA_TABLE;
      const writes = [];

      if (Array.isArray(weights)) {
        weights.forEach((w, i) => {
          writes.push(doc.send(new PutCommand({
            TableName: table,
            Item: {
              patientId: userId,
              sk: `WEIGHT#${w.date || now}#${w.id || i}`,
              type: 'weight',
              weight: w.weight,
              isGoal: !!w.isGoal,
              date: w.date || now,
            },
          })));
        });
      }
      if (Array.isArray(medications)) {
        medications.forEach((m, i) => {
          writes.push(doc.send(new PutCommand({
            TableName: table,
            Item: {
              patientId: userId,
              sk: `MED#${m.date || now}#${m.id || i}`,
              type: 'medication',
              name: m.name,
              dosage: m.dosage,
              frequency: m.frequency || '',
              medType: m.type || 'daily',
              date: m.date || now,
            },
          })));
        });
      }
      if (Array.isArray(oxygen)) {
        oxygen.forEach((o, i) => {
          writes.push(doc.send(new PutCommand({
            TableName: table,
            Item: {
              patientId: userId,
              sk: `OXYGEN#${o.date || now}#${o.id || i}`,
              type: 'oxygen',
              level: o.level,
              date: o.date || now,
            },
          })));
        });
      }
      if (Array.isArray(exercises)) {
        exercises.forEach((e, i) => {
          writes.push(doc.send(new PutCommand({
            TableName: table,
            Item: {
              patientId: userId,
              sk: `EXERCISE#${e.date || now}#${e.id || i}`,
              type: 'exercise',
              exerciseType: e.type,
              minutes: e.minutes,
              date: e.date || now,
            },
          })));
        });
      }
      if (Array.isArray(water)) {
        water.forEach((w, i) => {
          writes.push(doc.send(new PutCommand({
            TableName: table,
            Item: {
              patientId: userId,
              sk: `WATER#${w.date || now}#${w.id || i}`,
              type: 'water',
              amount: w.amount,
              date: w.date || now,
            },
          })));
        });
      }
      if (Array.isArray(foods)) {
        foods.forEach((f, i) => {
          writes.push(doc.send(new PutCommand({
            TableName: table,
            Item: {
              patientId: userId,
              sk: `FOOD#${f.date || now}#${f.id || i}`,
              type: 'food',
              name: f.name,
              mealCategory: f.mealCategory,
              quantity: f.quantity,
              calories: f.calories ?? 0,
              protein: f.protein ?? 0,
              carbs: f.carbs ?? 0,
              fat: f.fat ?? 0,
              date: f.date || now,
            },
          })));
        });
      }

      await Promise.all(writes);
      return json(200, { synced: true });
    }

    if (route === 'POST /consent') {
      const userId = getUserId(event);
      if (!userId) return json(401, { error: 'Unauthorized' });
      const { practiceId, doctorId, consentType } = body;
      const consentedAt = new Date().toISOString();
      await doc.send(new PutCommand({
        TableName: CONSENTS_TABLE,
        Item: {
          patientId: userId,
          consentedAt,
          practiceId: practiceId || '',
          doctorId: doctorId || '',
          consentType: consentType || 'share_with_doctor',
        },
      }));
      return json(200, { consented: true, consentedAt });
    }

    if (route === 'POST /link-doctor') {
      const userId = getUserId(event);
      if (!userId) return json(401, { error: 'Unauthorized' });
      const { doctorId, practiceId, inviteCode } = body;
      const linkId = practiceId || doctorId || inviteCode;
      if (!linkId) return json(400, { error: 'doctorId, practiceId, or inviteCode required' });
      const consentAt = new Date().toISOString();
      await doc.send(new PutCommand({
        TableName: LINKS_TABLE,
        Item: {
          patientId: userId,
          doctorId: doctorId || linkId,
          practiceId: practiceId || '',
          status: 'active',
          consentAt,
          inviteCode: inviteCode || '',
        },
      }));
      return json(200, { linked: true });
    }

    if (route === 'GET /patients') {
      const doctorId = getUserId(event);
      if (!doctorId) return json(401, { error: 'Unauthorized' });
      const items = await doc.send(new ScanCommand({
        TableName: LINKS_TABLE,
        FilterExpression: 'doctorId = :did AND #s = :active',
        ExpressionAttributeNames: { '#s': 'status' },
        ExpressionAttributeValues: { ':did': doctorId, ':active': 'active' },
      })).then(r => r.Items || []);
      const patientIds = [...new Set(items.map(i => i.patientId))];
      const patients = await Promise.all(patientIds.map(pid =>
        doc.send(new GetCommand({ TableName: USERS_TABLE, Key: { userId: pid } })).then(r => r.Item)
      ));
      return json(200, {
        patients: patients.filter(Boolean).map(p => ({
          userId: p.userId,
          email: p.email,
          role: p.role,
        })),
      });
    }

    if (route === 'GET /patients/:patientId/overview') {
      const doctorId = getUserId(event);
      const patientId = pathParams.patientId;
      if (!doctorId || !patientId) return json(401, { error: 'Unauthorized' });
      const link = await doc.send(new GetCommand({
        TableName: LINKS_TABLE,
        Key: { patientId, doctorId },
      })).then(r => r.Item);
      if (!link || link.status !== 'active') return json(403, { error: 'Not linked to this patient' });
      const health = await doc.send(new QueryCommand({
        TableName: HEALTH_DATA_TABLE,
        KeyConditionExpression: 'patientId = :pid',
        ExpressionAttributeValues: { ':pid': patientId },
        Limit: 500,
      })).then(r => r.Items || []);
      const weights = health.filter(i => i.type === 'weight');
      const latestWeight = weights.sort((a, b) => (b.date || 0) - (a.date || 0))[0];
      const goalWeight = health.filter(i => i.type === 'weight' && i.isGoal).sort((a, b) => (b.date || 0) - (a.date || 0))[0];
      return json(200, {
        patientId,
        latestWeight: latestWeight ? { weight: latestWeight.weight, date: latestWeight.date } : null,
        goalWeight: goalWeight ? goalWeight.weight : null,
        summary: {
          weights: weights.length,
          medications: health.filter(i => i.type === 'medication').length,
          oxygen: health.filter(i => i.type === 'oxygen').length,
          exercises: health.filter(i => i.type === 'exercise').length,
          water: health.filter(i => i.type === 'water').length,
          foods: health.filter(i => i.type === 'food').length,
        },
      });
    }

    return json(404, { error: 'Not found' });
  } catch (err) {
    // HIPAA: do not log event, body, or any PHI; log only high-level error type
    console.error('API error', err.message);
    return json(500, { error: 'Internal server error' });
  }
};
