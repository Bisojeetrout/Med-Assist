"use client";

import { UploadCloud, FileText } from 'lucide-react';

export default function Reports() {
  return (
    <main>
      <h1 className="title" style={{ marginBottom: '24px' }}>Medical Reports</h1>
      
      <div className="card" style={{ 
        border: '2px dashed var(--color-border)', 
        boxShadow: 'none', 
        display: 'flex', 
        flexDirection: 'column', 
        alignItems: 'center', 
        padding: '32px 20px',
        cursor: 'pointer'
      }}>
        <UploadCloud size={48} color="var(--color-primary)" style={{ marginBottom: '16px' }} />
        <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '8px' }}>Upload New Report</h3>
        <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', textAlign: 'center' }}>
          Tap to upload PDFs or images of your medical reports for AI analysis.
        </p>
      </div>

      <h2 className="title" style={{ fontSize: '18px', marginBottom: '16px', marginTop: '32px' }}>Past Reports</h2>
      
      <div className="card" style={{ padding: '16px', display: 'flex', alignItems: 'center', gap: '16px' }}>
        <div style={{ padding: '12px', backgroundColor: 'var(--color-primary-light)', borderRadius: 'var(--radius-md)' }}>
          <FileText size={24} color="var(--color-primary)" />
        </div>
        <div style={{ flex: 1 }}>
          <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--color-text-main)' }}>Blood Test Results</h3>
          <p style={{ fontSize: '14px', color: 'var(--color-text-muted)' }}>Analyzed on Oct 12, 2023</p>
        </div>
      </div>
    </main>
  );
}
