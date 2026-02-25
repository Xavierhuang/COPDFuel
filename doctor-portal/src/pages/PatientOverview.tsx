import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getPatientOverview, type PatientOverview as PatientOverviewType } from '../api';

export default function PatientOverview() {
  const { patientId } = useParams<{ patientId: string }>();
  const [data, setData] = useState<PatientOverviewType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!patientId) return;
    getPatientOverview(patientId)
      .then(setData)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load overview'))
      .finally(() => setLoading(false));
  }, [patientId]);

  if (!patientId) return <p>Invalid patient.</p>;
  if (loading) return <p>Loading...</p>;
  if (error) return <p style={{ color: '#c00' }}>{error}</p>;
  if (!data) return null;

  const formatDate = (ts: number) => new Date(ts).toLocaleDateString();

  return (
    <div>
      <p><Link to="/">Back to patients</Link></p>
      <h2 style={{ marginTop: 0 }}>Patient overview</h2>
      <p style={{ color: '#666' }}>Patient ID: {data.patientId}</p>
      <section style={{ marginBottom: '1.5rem' }}>
        <h3 style={{ marginBottom: 8 }}>Weight</h3>
        <p>
          Latest: {data.latestWeight ? `${data.latestWeight.weight} (${formatDate(data.latestWeight.date)})` : '—'}
          {data.goalWeight != null && ` | Goal: ${data.goalWeight}`}
        </p>
      </section>
      <section>
        <h3 style={{ marginBottom: 8 }}>Data summary</h3>
        <ul style={{ margin: 0, paddingLeft: '1.25rem' }}>
          <li>Weights: {data.summary.weights}</li>
          <li>Medications: {data.summary.medications}</li>
          <li>Oxygen: {data.summary.oxygen}</li>
          <li>Exercises: {data.summary.exercises}</li>
          <li>Water: {data.summary.water}</li>
          <li>Foods: {data.summary.foods}</li>
        </ul>
      </section>
    </div>
  );
}
