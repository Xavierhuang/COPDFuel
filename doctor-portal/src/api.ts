import { fetchAuthSession } from 'aws-amplify/auth';
import { config } from './config';

async function getToken(): Promise<string> {
  const session = await fetchAuthSession();
  const token = session.tokens?.idToken?.toString();
  if (!token) throw new Error('Not authenticated');
  return token;
}

export async function apiGet<T>(path: string): Promise<T> {
  const token = await getToken();
  const res = await fetch(`${config.apiBaseUrl}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(err.error || res.statusText);
  }
  return res.json();
}

export interface Patient {
  userId: string;
  email?: string;
  role?: string;
}

export interface PatientOverview {
  patientId: string;
  latestWeight: { weight: number; date: number } | null;
  goalWeight: number | null;
  summary: {
    weights: number;
    medications: number;
    oxygen: number;
    exercises: number;
    water: number;
    foods: number;
  };
}

export function getPatients(): Promise<{ patients: Patient[] }> {
  return apiGet<{ patients: Patient[] }>('/patients');
}

export function getPatientOverview(patientId: string): Promise<PatientOverview> {
  return apiGet<PatientOverview>(`/patients/${patientId}/overview`);
}
