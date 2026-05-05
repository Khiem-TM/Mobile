import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { PassportModule } from '@nestjs/passport';
import { JwtModule } from '@nestjs/jwt';
import { ScheduleModule } from '@nestjs/schedule';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { SupportModule } from '../support/support.module';
import { FoodModule } from '../food/food.module';
import { TrainModule } from '../train/train.module';
// entities
import { User } from './entities/user.entity';
import { UserHealthProfile } from './entities/user-health-profile.entity';
import { RefreshToken } from './entities/refresh-token.entity';
import { EmailVerification } from './entities/email-verification.entity';
import { PasswordReset } from './entities/password-reset.entity';
import { Streak } from './entities/streak.entity';
import { Notification } from './entities/notification.entity';
// controllers
import { AuthController } from './controllers/auth.controller';
import { UsersController } from './controllers/users.controller';
import { DashboardController } from './controllers/dashboard.controller';
import { NotificationsController } from './controllers/notifications.controller';
import { StreaksController } from './controllers/streaks.controller';
// services
import { AuthService } from './services/auth.service';
import { UsersService } from './services/users.service';
import { DashboardService } from './services/dashboard.service';
import { NotificationsService } from './services/notifications.service';
import { StreaksService } from './services/streaks.service';
import { StreaksScheduler } from './services/streaks.scheduler';
// strategies
import { JwtStrategy } from './strategies/jwt.strategy';
import { GoogleStrategy } from './strategies/google.strategy';
// repositories
import { STREAKS_REPOSITORY } from './user.constants';
import { StreaksRepository } from './repositories/streaks.repository';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      User, UserHealthProfile, RefreshToken,
      EmailVerification, PasswordReset, Streak, Notification,
    ]),
    PassportModule,
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        secret: config.get<string>('JWT_SECRET', 'khiemhehe'),
        signOptions: { expiresIn: '15m' },
      }),
    }),
    ScheduleModule.forRoot(),
    SupportModule,
    forwardRef(() => FoodModule),
    forwardRef(() => TrainModule),
  ],
  controllers: [
    AuthController, UsersController, DashboardController,
    NotificationsController, StreaksController,
  ],
  providers: [
    AuthService, UsersService, DashboardService,
    NotificationsService, StreaksService, StreaksScheduler,
    JwtStrategy, GoogleStrategy,
    { provide: STREAKS_REPOSITORY, useClass: StreaksRepository },
  ],
  exports: [UsersService, AuthService, StreaksService, NotificationsService],
})
export class UserModule {}
