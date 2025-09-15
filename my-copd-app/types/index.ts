export interface COPDUserProfile {
  weight: number;
  goalWeight: number;
  sex: 'male' | 'female' | 'other';
  oxygenUse: boolean;
  pulseOx: number[];
}

export interface HealthLog {
  date: string;
  weight: number;
  pulseOx: number;
  notes: string;
} 