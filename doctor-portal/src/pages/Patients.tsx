import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getPatients, type Patient } from '../api';

export default function Patients() {
  const [patients, setPatients] = useState<Patient[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getPatients()
      .then((res) => setPatients(res.patients || []))
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load patients'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>Loading patients...</p>;
  if (error) return <p style={{ color: '#c00' }}>{error}</p>;
  if (patients.length === 0) return <p>No linked patients yet. Patients link to you from the app using your invite code or practice.</p>;

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>Your patients</h2>
      <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {patients.map((p) => (
          <li key={p.userId} style={{ marginBottom: 8 }}>
            <Link to={`/patients/${p.userId}`} style={{ display: 'block', padding: '0.75rem 1rem', background: '#fff', borderRadius: 6, border: '1px solid #eee' }}>
              {p.email || p.userId}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
