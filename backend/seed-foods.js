const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

const client = new Client({
  host: 'localhost',
  port: 5433,
  user: 'postgres',
  password: '123456',
  database: 'calories_tracker',
});

async function seed() {
  try {
    await client.connect();
    console.log('Connected to database');

    const sqlPath = path.join(__dirname, 'src/database/seeds/seed_vn_foods.sql');
    const sql = fs.readFileSync(sqlPath, 'utf8');

    console.log('Executing SQL script...');
    await client.query(sql);
    console.log('Seeding completed successfully!');
  } catch (err) {
    console.error('Error during seeding:', err);
  } finally {
    await client.end();
  }
}

seed();
