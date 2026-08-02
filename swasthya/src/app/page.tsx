import { AlertTriangle, Share2, Watch, HeartPulse } from 'lucide-react';

export default function Home() {
  return (
    <main>
      <header style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 className="title" style={{ marginBottom: 0 }}>Swasthya</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--color-primary)' }}>
          <Watch size={20} />
          <span style={{ fontSize: '14px', fontWeight: 500 }}>Connected</span>
        </div>
      </header>

      {/* Health Score Circular Dial */}
      <div className="card" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '32px 20px' }}>
        <div style={{
          width: '180px',
          height: '180px',
          borderRadius: '50%',
          border: '12px solid var(--color-primary-light)',
          borderTopColor: 'var(--color-primary)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          transform: 'rotate(-45deg)', // just a visual effect
          marginBottom: '16px'
        }}>
          <div style={{ transform: 'rotate(45deg)', textAlign: 'center' }}>
            <div style={{ fontSize: '48px', fontWeight: 700, color: 'var(--color-primary)', lineHeight: 1 }}>85</div>
            <div style={{ fontSize: '14px', color: 'var(--color-text-muted)' }}>Health Score</div>
          </div>
        </div>
        <p style={{ textAlign: 'center', color: 'var(--color-text-muted)', fontSize: '15px' }}>
          Great job! Your heart rate and daily activity look excellent today.
        </p>
      </div>

      {/* Quick Actions */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '20px' }}>
        <button className="btn-danger">
          <AlertTriangle size={18} />
          Emergency
        </button>
        <button className="btn-secondary">
          <Share2 size={18} />
          Share Data
        </button>
      </div>

      {/* AI Recommendations */}
      <div className="card">
        <h2 className="title" style={{ fontSize: '18px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <HeartPulse size={20} color="var(--color-primary)" />
          AI Insights
        </h2>
        <p className="subtitle" style={{ marginBottom: '12px' }}>Based on your recent check-ins and smartwatch data.</p>
        
        <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <li style={{ padding: '12px', backgroundColor: 'var(--color-background)', borderRadius: 'var(--radius-md)', fontSize: '14px' }}>
            <strong>Hydration:</strong> You're slightly below your water intake goal. Try to drink 2 more glasses today.
          </li>
          <li style={{ padding: '12px', backgroundColor: 'var(--color-background)', borderRadius: 'var(--radius-md)', fontSize: '14px' }}>
            <strong>Activity:</strong> A 15-minute walk this evening will help you hit your daily movement target.
          </li>
        </ul>
      </div>
    </main>
  );
}
