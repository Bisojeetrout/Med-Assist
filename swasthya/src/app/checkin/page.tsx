"use client";

import { useState } from 'react';
import { Check } from 'lucide-react';

export default function CheckIn() {
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitted(true);
    setTimeout(() => setSubmitted(false), 3000);
  };

  return (
    <main>
      <h1 className="title" style={{ marginBottom: '24px' }}>Daily Check-in</h1>
      
      <div className="card">
        <h2 className="title" style={{ fontSize: '18px' }}>Body Measurements</h2>
        <p className="subtitle">Log your daily vitals for tracking.</p>
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Weight (kg)</label>
            <input type="number" step="0.1" className="form-input" placeholder="e.g. 70.5" required />
          </div>
          
          <div className="form-group">
            <label className="form-label">Blood Pressure (mmHg)</label>
            <input type="text" className="form-input" placeholder="e.g. 120/80" required />
          </div>

          <div className="form-group">
            <label className="form-label">Notes / How do you feel?</label>
            <textarea className="form-input" rows={3} placeholder="Any symptoms or notes..."></textarea>
          </div>
          
          <button type="submit" className="btn-primary" style={{ marginTop: '8px' }}>
            {submitted ? <><Check size={20} /> Saved Successfully</> : "Save Vitals"}
          </button>
        </form>
      </div>
    </main>
  );
}
