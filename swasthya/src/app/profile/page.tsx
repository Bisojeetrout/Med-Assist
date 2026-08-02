"use client";

import { User, Settings, FileText, LogOut } from 'lucide-react';
import Link from 'next/link';

export default function Profile() {
  return (
    <main>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '32px' }}>
        <div style={{ 
          width: '64px', height: '64px', borderRadius: '50%', 
          backgroundColor: 'var(--color-primary-light)', color: 'var(--color-primary)',
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <User size={32} />
        </div>
        <div>
          <h1 className="title" style={{ fontSize: '20px', marginBottom: '4px' }}>Alex Doe</h1>
          <p className="subtitle" style={{ marginBottom: 0 }}>alex.doe@example.com</p>
        </div>
      </div>

      <div className="card" style={{ padding: '0' }}>
        <ul style={{ listStyle: 'none' }}>
          <li style={{ borderBottom: '1px solid var(--color-border)' }}>
            <Link href="/reports" style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '16px 20px', color: 'var(--color-text-main)' }}>
              <FileText size={20} color="var(--color-primary)" />
              <span style={{ fontWeight: 500 }}>Medical Reports</span>
            </Link>
          </li>
          <li style={{ borderBottom: '1px solid var(--color-border)' }}>
            <a href="#" style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '16px 20px', color: 'var(--color-text-main)' }}>
              <Settings size={20} color="var(--color-text-muted)" />
              <span style={{ fontWeight: 500 }}>Settings</span>
            </a>
          </li>
          <li>
            <Link href="/login" style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '16px 20px', color: 'var(--color-accent)' }}>
              <LogOut size={20} />
              <span style={{ fontWeight: 500 }}>Sign Out</span>
            </Link>
          </li>
        </ul>
      </div>
    </main>
  );
}
