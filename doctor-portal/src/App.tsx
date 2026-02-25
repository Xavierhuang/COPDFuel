import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { getCurrentUser, signOut } from 'aws-amplify/auth';
import Login from './pages/Login';
import Patients from './pages/Patients';
import PatientOverview from './pages/PatientOverview';
import Layout from './Layout';

export default function App() {
  const [authChecked, setAuthChecked] = useState(false);
  const [user, setUser] = useState<{ userId: string } | null>(null);

  useEffect(() => {
    getCurrentUser()
      .then((u) => setUser({ userId: u.userId }))
      .catch(() => setUser(null))
      .finally(() => setAuthChecked(true));
  }, []);

  const handleSignOut = async () => {
    await signOut();
    setUser(null);
  };

  if (!authChecked) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>Loading...</div>
    );
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login onSuccess={() => window.location.reload()} />} />
        <Route path="/" element={user ? <Layout user={user} onSignOut={handleSignOut} /> : <Navigate to="/login" replace />}>
          <Route index element={<Patients />} />
          <Route path="patients/:patientId" element={<PatientOverview />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
