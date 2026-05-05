import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication, ValidationPipe, ClassSerializerInterceptor } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { DataSource } from 'typeorm';
import request from 'supertest';
import { AppModule } from '../src/app.module';
import { ResponseInterceptor } from '../src/common/interceptors/response.interceptor';
import { HttpExceptionFilter, AllExceptionsFilter } from '../src/common/filters/http-exception.filter';

const SUFFIX = Date.now();
const TEST_EMAIL = `e2e_${SUFFIX}@test.com`;
const TEST_PASS = 'Test@1234';
const TEST_NAME = `E2E ${SUFFIX}`;

let app: INestApplication;
let accessToken: string;
let foodId: string;

// ─── Setup ───────────────────────────────────────────────────────────────────

beforeAll(async () => {
  const moduleFixture: TestingModule = await Test.createTestingModule({
    imports: [AppModule],
  }).compile();

  app = moduleFixture.createNestApplication();
  const reflector = app.get(Reflector);
  app.useGlobalFilters(new AllExceptionsFilter(), new HttpExceptionFilter());
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true, forbidNonWhitelisted: true }));
  app.useGlobalInterceptors(new ClassSerializerInterceptor(reflector), new ResponseInterceptor());
  await app.init();
}, 60_000);

afterAll(async () => {
  try {
    const ds = app.get(DataSource);
    await ds.query(`DELETE FROM users WHERE email = $1`, [TEST_EMAIL]);
  } catch (_) {}
  await app.close();
}, 15_000);

// ─── Auth ─────────────────────────────────────────────────────────────────────

describe('Auth', () => {
  it('POST /auth/register → 201 + returns access_token', async () => {
    const res = await request(app.getHttpServer())
      .post('/auth/register')
      .send({ email: TEST_EMAIL, password: TEST_PASS, display_name: TEST_NAME });

    expect(res.status).toBe(201);
    expect(res.body.data).toHaveProperty('access_token');

    // Bypass email verification (no SMTP in test env)
    const ds = app.get(DataSource);
    await ds.query(`UPDATE users SET is_verified = true WHERE email = $1`, [TEST_EMAIL]);
  });

  it('POST /auth/register duplicate → 400', async () => {
    const res = await request(app.getHttpServer())
      .post('/auth/register')
      .send({ email: TEST_EMAIL, password: TEST_PASS, display_name: TEST_NAME });
    expect(res.status).toBe(400);
  });

  it('POST /auth/login → 200 + returns access_token', async () => {
    const res = await request(app.getHttpServer())
      .post('/auth/login')
      .send({ email: TEST_EMAIL, password: TEST_PASS });

    expect([200, 201]).toContain(res.status);
    expect(res.body.data).toHaveProperty('access_token');
    accessToken = res.body.data.access_token;
  });

  it('POST /auth/login wrong password → 401', async () => {
    const res = await request(app.getHttpServer())
      .post('/auth/login')
      .send({ email: TEST_EMAIL, password: 'WrongPass99!' });
    expect(res.status).toBe(401);
  });

  it('GET /users/me no token → 401', async () => {
    const res = await request(app.getHttpServer()).get('/users/me');
    expect(res.status).toBe(401);
  });
});

// ─── Users ───────────────────────────────────────────────────────────────────

describe('Users', () => {
  it('GET /users/me → 200', async () => {
    const res = await request(app.getHttpServer())
      .get('/users/me')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
    expect(res.body.data).toHaveProperty('email', TEST_EMAIL);
  });

  it('PUT /users/me/health-profile → 200', async () => {
    const res = await request(app.getHttpServer())
      .put('/users/me/health-profile')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({
        heightCm: 170,
        initialWeightKg: 65,
        weightGoalKg: 60,
        activityLevel: 'moderately_active',
        gender: 'male',
        birthDate: '1999-01-01',
        waterGoalMl: 2000,
      });
    expect(res.status).toBe(200);
  });

  it('GET /users/me/health-profile → 200', async () => {
    const res = await request(app.getHttpServer())
      .get('/users/me/health-profile')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
  });
});

// ─── Foods ───────────────────────────────────────────────────────────────────

describe('Foods', () => {
  it('GET /foods → 200 (public) with pagination shape', async () => {
    const res = await request(app.getHttpServer()).get('/foods');
    expect(res.status).toBe(200);
    expect(res.body.data).toHaveProperty('items');
    expect(res.body.data).toHaveProperty('total');
    if (res.body.data.items.length > 0) foodId = res.body.data.items[0].id;
  });

  it('GET /foods?search=rice → 200', async () => {
    const res = await request(app.getHttpServer()).get('/foods?search=rice');
    expect(res.status).toBe(200);
    expect(res.body.data).toHaveProperty('items');
  });
});

// ─── Meal Logs ───────────────────────────────────────────────────────────────

describe('Meal Logs', () => {
  const today = new Date().toISOString().split('T')[0];

  it('POST /meal-logs → 201', async () => {
    const body: Record<string, unknown> = {
      log_date: today,
      meal_type: 'lunch',
      items: foodId ? [{ food_id: foodId, quantity: 1, unit: 'serving' }] : [],
    };
    const res = await request(app.getHttpServer())
      .post('/meal-logs')
      .set('Authorization', `Bearer ${accessToken}`)
      .send(body);
    expect([200, 201]).toContain(res.status);
  });

  it('GET /meal-logs?date=today → 200 (array)', async () => {
    const res = await request(app.getHttpServer())
      .get(`/meal-logs?date=${today}`)
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body.data)).toBe(true);
  });
});

// ─── Activity Logs ────────────────────────────────────────────────────────────

describe('Activity Logs', () => {
  const today = new Date().toISOString().split('T')[0];

  it('PATCH /activity-logs/steps → 200', async () => {
    const res = await request(app.getHttpServer())
      .patch('/activity-logs/steps')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ logDate: today, steps: 5000 });
    expect(res.status).toBe(200);
  });

  it('PATCH /activity-logs/water → 200', async () => {
    const res = await request(app.getHttpServer())
      .patch('/activity-logs/water')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ logDate: today, waterMl: 1500 });
    expect(res.status).toBe(200);
  });

  it('PATCH /activity-logs/calories-burned → 200', async () => {
    const res = await request(app.getHttpServer())
      .patch('/activity-logs/calories-burned')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ logDate: today, caloriesBurned: 300, activeMinutes: 30 });
    expect(res.status).toBe(200);
  });

  it('GET /activity-logs?date=today → 200 with correct data', async () => {
    const res = await request(app.getHttpServer())
      .get(`/activity-logs?date=${today}`)
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
    expect(res.body.data).toMatchObject({ steps: 5000, waterMl: 1500 });
  });

  it('GET /activity-logs/range → 200', async () => {
    const res = await request(app.getHttpServer())
      .get(`/activity-logs/range?fromDate=${today}&toDate=${today}`)
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
  });
});

// ─── Body Metrics ─────────────────────────────────────────────────────────────

describe('Body Metrics', () => {
  it('POST /body-metrics → 200/201', async () => {
    const res = await request(app.getHttpServer())
      .post('/body-metrics')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ weightKg: 65.5, bodyFatPct: 18 });
    expect([200, 201]).toContain(res.status);
  });

  it('GET /body-metrics/latest → 200', async () => {
    const res = await request(app.getHttpServer())
      .get('/body-metrics/latest')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
  });

  it('GET /body-metrics/summary → 200 + has currentWeight', async () => {
    const res = await request(app.getHttpServer())
      .get('/body-metrics/summary')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
    expect(res.body.data).toHaveProperty('currentWeight');
  });

  it('GET /body-metrics (history) → 200 (array)', async () => {
    const res = await request(app.getHttpServer())
      .get('/body-metrics')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body.data)).toBe(true);
  });
});

// ─── Training ────────────────────────────────────────────────────────────────

describe('Training', () => {
  it('GET /training/exercises → 200 (public)', async () => {
    const res = await request(app.getHttpServer()).get('/training/exercises');
    expect(res.status).toBe(200);
  });

  it('GET /training/tips → 200 (public)', async () => {
    const res = await request(app.getHttpServer()).get('/training/tips');
    expect(res.status).toBe(200);
  });

  it('GET /training/goals → 200', async () => {
    const res = await request(app.getHttpServer())
      .get('/training/goals')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
  });

  it('GET /training/history → 200', async () => {
    const res = await request(app.getHttpServer())
      .get('/training/history')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
  });

  it('GET /training/exercises/favorites → 200', async () => {
    const res = await request(app.getHttpServer())
      .get('/training/exercises/favorites')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
  });
});

// ─── Blog ────────────────────────────────────────────────────────────────────

describe('Blog', () => {
  it('GET /blogs → 200 (public)', async () => {
    const res = await request(app.getHttpServer()).get('/blogs');
    expect(res.status).toBe(200);
  });

  it('GET /user/blogs → 200', async () => {
    const res = await request(app.getHttpServer())
      .get('/user/blogs')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
  });
});

// ─── Notifications ────────────────────────────────────────────────────────────

describe('Notifications', () => {
  it('GET /notifications → 200', async () => {
    const res = await request(app.getHttpServer())
      .get('/notifications')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
  });

  it('PATCH /notifications/read-all → 200', async () => {
    const res = await request(app.getHttpServer())
      .patch('/notifications/read-all')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
  });
});

// ─── Streaks ─────────────────────────────────────────────────────────────────

describe('Streaks', () => {
  it('GET /streaks → 200', async () => {
    const res = await request(app.getHttpServer())
      .get('/streaks')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
  });
});

// ─── Dashboard ───────────────────────────────────────────────────────────────

describe('Dashboard', () => {
  it('GET /dashboard → 200', async () => {
    const res = await request(app.getHttpServer())
      .get('/dashboard')
      .set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
  });
});
