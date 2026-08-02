"use client";

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Activity, LayoutDashboard, Pill, User } from 'lucide-react';

export default function Navigation() {
  const pathname = usePathname();

  return (
    <nav className="bottom-nav">
      <Link href="/" className={`nav-item ${pathname === '/' ? 'active' : ''}`}>
        <LayoutDashboard className="nav-icon" />
        <span>Home</span>
      </Link>
      <Link href="/checkin" className={`nav-item ${pathname === '/checkin' ? 'active' : ''}`}>
        <Activity className="nav-icon" />
        <span>Check-in</span>
      </Link>
      <Link href="/medicines" className={`nav-item ${pathname === '/medicines' ? 'active' : ''}`}>
        <Pill className="nav-icon" />
        <span>Medicines</span>
      </Link>
      <Link href="/profile" className={`nav-item ${pathname === '/profile' ? 'active' : ''}`}>
        <User className="nav-icon" />
        <span>Profile</span>
      </Link>
    </nav>
  );
}
