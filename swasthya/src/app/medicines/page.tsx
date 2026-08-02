"use client";

import { Camera, Plus } from 'lucide-react';

export default function Medicines() {
  return (
    <main>
      <h1 className="title" style={{ marginBottom: '24px' }}>Medicines</h1>
      
      <div style={{ display: 'flex', gap: '12px', marginBottom: '24px' }}>
        <button className="btn-primary" style={{ flex: 1 }}>
          <Plus size={18} />
          Add Manual
        </button>
        <button className="btn-secondary" style={{ flex: 1 }}>
          <Camera size={18} />
          Scan with AI
        </button>
      </div>

      <h2 className="title" style={{ fontSize: '18px', marginBottom: '16px' }}>Current Prescriptions</h2>
      
      <div className="card" style={{ padding: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--color-text-main)' }}>Vitamin D3</h3>
          <p style={{ fontSize: '14px', color: 'var(--color-text-muted)' }}>1 Pill • After Breakfast</p>
        </div>
        <div style={{ color: 'var(--color-primary)', fontWeight: 600 }}>Daily</div>
      </div>

      <div className="card" style={{ padding: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--color-text-main)' }}>Amlodipine</h3>
          <p style={{ fontSize: '14px', color: 'var(--color-text-muted)' }}>5mg • After Dinner</p>
        </div>
        <div style={{ color: 'var(--color-primary)', fontWeight: 600 }}>Daily</div>
      </div>
    </main>
  );
}
