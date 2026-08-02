import Database from 'better-sqlite3';
import path from 'path';

// Define the database file path in the project root
const dbPath = path.resolve(process.cwd(), 'database.db');

// Create or open the database
const db = new Database(dbPath, { verbose: console.log });

// Initialize tables if they don't exist
db.exec(`
  CREATE TABLE IF NOT EXISTS checkins (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,
    weight REAL,
    bloodPressure TEXT,
    notes TEXT
  );

  CREATE TABLE IF NOT EXISTS medicines (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    dosage TEXT,
    time TEXT
  );
`);

export default db;
