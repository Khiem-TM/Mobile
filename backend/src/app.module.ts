import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ThrottlerModule, ThrottlerGuard } from '@nestjs/throttler';
import { APP_GUARD } from '@nestjs/core';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { AuthModule } from './modules/auth/auth.module';
import { UsersModule } from './modules/users/users.module';
import { FoodsModule } from './modules/foods/foods.module';
import { MealLogsModule } from './modules/meal-logs/meal-logs.module';
import { BodyMetricsModule } from './modules/body-metrics/body-metrics.module';
import { ActivityLogsModule } from './modules/activity-logs/activity-logs.module';
import { StreaksModule } from './modules/streaks/streaks.module';
import { TrainingModule } from './modules/training/training.module';
import { DashboardModule } from './modules/dashboard/dashboard.module';
import { NotificationsModule } from './modules/notifications/notifications.module';
import { AdminModule } from './modules/admin/admin.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),

    ThrottlerModule.forRoot([
      { name: 'short', ttl: 1000, limit: 10 },
      { name: 'medium', ttl: 60000, limit: 100 },
    ]),

    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => {
        const dbHost = configService.get<string>('DB_HOST', 'localhost');
        const dbPort = configService.get<number>('DB_PORT', 5432);
        const dbUser = configService.get<string>('DB_USER', 'postgres');
        const dbPass = configService.get<string>('DB_PASS', '123456');
        const dbName = configService.get<string>('DB_NAME', 'calories_tracker');

        console.log(`Connecting to ${dbUser}@${dbHost}:${dbPort}/${dbName}`);

        const isSsl = configService.get<string>('DB_SSL', 'false') === 'true';

        return {
          type: 'postgres' as const,
          host: dbHost,
          port: dbPort,
          username: dbUser,
          password: dbPass,
          database: dbName,
          autoLoadEntities: true,
          synchronize: configService.get<string>('NODE_ENV', 'development') !== 'production',
          ...(isSsl && { ssl: { rejectUnauthorized: false } }),
        };
      },
    }),

    AuthModule,
    UsersModule,
    FoodsModule,
    MealLogsModule,
    BodyMetricsModule,
    ActivityLogsModule,
    StreaksModule,
    TrainingModule,
    DashboardModule,
    NotificationsModule,
    AdminModule,
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
