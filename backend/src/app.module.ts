import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ThrottlerModule, ThrottlerGuard } from '@nestjs/throttler';
import { APP_GUARD } from '@nestjs/core';
import Redis from 'ioredis';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { SupportModule } from './modules/support/support.module';
import { UserModule } from './modules/user/user.module';
import { FoodModule } from './modules/food/food.module';
import { TrainModule } from './modules/train/train.module';
import { BlogModule } from './modules/blog/blog.module';
import { AdminModule } from './modules/admin/admin.module';
import { ChatbotModule } from './modules/chatbot/chatbot.module';
import { NotificationModule } from './modules/notification/notification.module';

function parseBoolean(value: string | undefined, defaultValue = false): boolean {
  if (value === undefined) return defaultValue;
  return ['true', '1', 'yes', 'on'].includes(value.toLowerCase());
}

function parsePort(value: string | undefined, defaultValue: number): number {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : defaultValue;
}

function getDatabaseUrlHost(databaseUrl: string): string | undefined {
  try {
    return new URL(databaseUrl).hostname;
  } catch {
    return undefined;
  }
}

function isLocalDatabaseHost(host: string | undefined): boolean {
  if (!host) return false;
  return ['localhost', '127.0.0.1', '::1', 'postgres'].includes(host);
}

function shouldUseDatabaseSsl(
  configService: ConfigService,
  databaseUrl?: string,
): boolean {
  const dbSsl = configService.get<string>('DB_SSL');
  if (dbSsl !== undefined) return parseBoolean(dbSsl);

  if (!databaseUrl) return false;
  const host = getDatabaseUrlHost(databaseUrl);
  return !isLocalDatabaseHost(host);
}

function describeDatabaseUrl(databaseUrl: string): string {
  try {
    const url = new URL(databaseUrl);
    return `${url.hostname}${url.port ? `:${url.port}` : ''}${url.pathname}`;
  } catch {
    return 'DATABASE_URL';
  }
}

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),

    ThrottlerModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        throttlers: [
          { name: 'short', ttl: 1000, limit: 10 },
          { name: 'medium', ttl: 60000, limit: 100 },
        ],
        // Dynamically require redis storage so the package is optional
        storage: (() => {
          try {
            // eslint-disable-next-line @typescript-eslint/no-var-requires
            const { ThrottlerStorageRedisService } = require('nestjs-throttler-storage-redis');
            return new ThrottlerStorageRedisService(
              new Redis({
                host: config.get<string>('REDIS_HOST', 'localhost'),
                port: config.get<number>('REDIS_PORT', 6379),
                lazyConnect: true,
              }),
            );
          } catch (e) {
            // fall back to in-memory storage if package is not installed
            return undefined;
          }
        })(),
      }),
    }),

    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => {
        const databaseUrl = configService.get<string>('DATABASE_URL');
        const synchronize = parseBoolean(
          configService.get<string>('DB_SYNCHRONIZE'),
          false,
        );
        const isSsl = shouldUseDatabaseSsl(configService, databaseUrl);
        const baseConfig = {
          type: 'postgres' as const,
          autoLoadEntities: true,
          synchronize,
          ...(isSsl && { ssl: { rejectUnauthorized: false } }),
        };

        if (databaseUrl) {
          console.log(
            `Connecting to ${describeDatabaseUrl(databaseUrl)} via DATABASE_URL (synchronize=${synchronize})`,
          );
          return {
            ...baseConfig,
            url: databaseUrl,
          };
        }

        const dbHost = configService.get<string>('DB_HOST', 'localhost');
        const dbPort = parsePort(configService.get<string>('DB_PORT'), 5432);
        const dbUser = configService.get<string>('DB_USER', 'postgres');
        const dbPass = configService.get<string>('DB_PASS', '123456');
        const dbName = configService.get<string>('DB_NAME', 'calories_tracker');

        console.log(
          `Connecting to ${dbHost}:${dbPort}/${dbName} via DB_* env (synchronize=${synchronize})`,
        );

        return {
          ...baseConfig,
          host: dbHost,
          port: dbPort,
          username: dbUser,
          password: dbPass,
          database: dbName,
        };
      },
    }),

    SupportModule,
    UserModule,
    FoodModule,
    TrainModule,
    BlogModule,
    AdminModule,
    ChatbotModule,
    NotificationModule,
  ],
  controllers: [AppController],
  providers: [
    AppService,
    {
      provide: APP_GUARD,
      useClass: ThrottlerGuard,
    },
  ],
})
export class AppModule {}
