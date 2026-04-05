import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { ThrottlerModule, ThrottlerGuard } from '@nestjs/throttler';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ConfigModule, ConfigService } from '@nestjs/config';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),

    // Rate limiting config
    ThrottlerModule.forRoot([
      { name: 'short', ttl: 1000, limit: 10 },
      { name: 'medium', ttl: 60000, limit: 100 },
    ]),

    // Database config
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

        return {
          type: 'postgres' as const,
          host: dbHost,
          port: dbPort,
          username: dbUser,
          password: dbPass,
          database: dbName,
          autoLoadEntities: true,
          synchronize: true,
        };
      },
    }),
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
