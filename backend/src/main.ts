import { NestFactory, Reflector } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { AppModule } from './app.module';
import { BLOG_NOTIFICATIONS_TOPIC } from './modules/notification/events/notification-events';
import { ClassSerializerInterceptor, ValidationPipe } from '@nestjs/common';
import { json, urlencoded } from 'express';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { ResponseInterceptor } from './common/interceptors/response.interceptor';
import { HttpExceptionFilter, AllExceptionsFilter } from './common/filters/http-exception.filter';
import { join } from 'path';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);

  app.useStaticAssets(join(process.cwd(), 'public', 'seed-images'), {
    prefix: '/seed-images/',
  });

  const allowedOrigins = process.env.ALLOWED_ORIGINS
    ? process.env.ALLOWED_ORIGINS.split(',').map((o) => o.trim())
    : ['http://localhost:3000', 'http://localhost:8080', 'http://localhost:19006'];

  app.enableCors({
    origin: process.env.NODE_ENV === 'production' ? allowedOrigins : true,
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    credentials: true,
  });

  app.use(json({ limit: '50mb' }));
  app.use(urlencoded({ extended: true, limit: '50mb' }));

  app.useGlobalFilters(new AllExceptionsFilter(), new HttpExceptionFilter());

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      transform: true,
      forbidNonWhitelisted: true,
    }),
  );

  const reflector = app.get(Reflector);
  app.useGlobalInterceptors(
    new ClassSerializerInterceptor(reflector),
    new ResponseInterceptor(),
  );

  const config = new DocumentBuilder()
    .setTitle('Calories Tracker API')
    .setDescription('Calories & Fitness Tracker API for Mobile and Web')
    .setVersion('1.0')
    .addBearerAuth(
      { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' },
      'access-token',
    )
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api/docs', app, document);

  // Kafka consumer (Notification Service). Tắt bằng KAFKA_ENABLED=false để dev không cần broker.
  const kafkaEnabled = (process.env.KAFKA_ENABLED ?? 'true') !== 'false';
  if (kafkaEnabled) {
    const brokers = (process.env.KAFKA_BROKERS || 'localhost:9094').split(',');
    const clientId = process.env.KAFKA_CLIENT_ID || 'vitalai-backend';

    // Pre-create topic (waitForLeaders) để consumer KRaft không gặp race
    // "This server does not host this topic-partition" lúc cold-start.
    try {
      const { Kafka } = await import('kafkajs');
      const admin = new Kafka({ clientId: `${clientId}-admin`, brokers }).admin();
      await admin.connect();
      await admin.createTopics({
        waitForLeaders: true,
        topics: [{ topic: BLOG_NOTIFICATIONS_TOPIC, numPartitions: 1 }],
      });
      await admin.disconnect();
    } catch (e) {
      console.warn('Kafka topic pre-create warning:', (e as Error).message);
    }

    app.connectMicroservice<MicroserviceOptions>({
      transport: Transport.KAFKA,
      options: {
        client: { clientId, brokers },
        consumer: { groupId: 'notification-consumer' },
      },
    });
    try {
      await app.startAllMicroservices();
      console.log('Kafka microservice (notification-consumer) started');
    } catch (e) {
      console.error('Kafka microservice failed to start:', (e as Error).message);
    }
  }

  const port = process.env.PORT ?? 3000;
  await app.listen(port, '0.0.0.0');
  console.log(`Application is running on: http://0.0.0.0:${port}`);
  console.log(`Swagger docs: http://localhost:${port}/api/docs`);
}

void bootstrap();
