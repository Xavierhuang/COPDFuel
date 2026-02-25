import { Outlet } from 'react-router-dom';
import { config } from './config';

interface LayoutProps {
  user: { userId: string };
  onSignOut: () => void;
}

export default function Layout({ onSignOut }: LayoutProps) {
  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header style={{ background: '#1a1a1a', color: '#fff', padding: '1rem 1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <a href="/" style={{ color: '#fff', fontWeight: 600 }}>COPD Health Tracker - Doctor Portal</a>
        <button type="button" onClick={onSignOut} style={{ background: 'transparent', color: '#fff', border: '1px solid #666', padding: '0.4rem 0.8rem', cursor: 'pointer', borderRadius: 4 }}>Sign out</button>
      </header>
      <main style={{ flex: 1, padding: '1.5rem' }}>
        <Outlet />
      </main>
      <footer style={{ padding: '1rem 1.5rem', borderTop: '1px solid #ddd', fontSize: '0.875rem', color: '#666' }}>
        {config.privacyPolicyUrl ? (
          <a href={config.privacyPolicyUrl} target="_blank" rel="noopener noreferrer">Privacy Policy</a>
        ) : (
          <span>COPD Health Tracker Doctor Portal. Set VITE_PRIVACY_POLICY_URL to link your privacy policy.</span>
        )}
      </footer>
    </div>
  );
}
