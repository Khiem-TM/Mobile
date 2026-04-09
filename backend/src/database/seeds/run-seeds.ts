import 'reflect-metadata';
import { DataSource } from 'typeorm';
import * as dotenv from 'dotenv';
import { seedFoods } from './food.seed';
import { seedExercises } from './exercise.seed';

dotenv.config();

const dataSource = new DataSource({
  type: 'postgres',
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT) || 5432,
  username: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
  entities: [__dirname + '/../../modules/**/*.entity{.ts,.js}'],
  synchronize: false,
});

async function runSeeds() {
  try {
    await dataSource.initialize();
    console.log('[Seed] Database connected.');

    await seedFoods(dataSource);
    await seedExercises(dataSource);

    console.log('[Seed] All seeds completed successfully.');
  } catch (err) {
    console.error('[Seed] Error:', err);
    process.exit(1);
  } finally {
    await dataSource.destroy();
  }
}

runSeeds();
