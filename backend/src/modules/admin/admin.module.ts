import { forwardRef } from '@nestjs/common';
import { UserModule } from '../user/user.module';
import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { User } from '../user/entities/user.entity';
import { Food } from '../food/entities/food.entity';
import { Exercise } from '../train/entities/exercise.entity';
import { TrainingSession } from '../train/entities/training-session.entity';
import { Blog } from '../blog/entities/blog.entity';
import { AdminService } from './admin.service';
import { AdminController } from './admin.controller';
import { AdminAuthController } from './admin-auth.controller';
import { JwtStrategy } from '../user/strategies/jwt.strategy';
import { SupportModule } from '../support/support.module';

@Module({
  imports: [
    forwardRef(() => UserModule),
    SupportModule,
    TypeOrmModule.forFeature([User, Food, Exercise, TrainingSession, Blog]),
    PassportModule,
    JwtModule.register({
      secret: process.env.JWT_SECRET || 'khiemhehe',
      signOptions: { expiresIn: '7d' },
    }),
  ],
  controllers: [AdminController, AdminAuthController],
  providers: [AdminService, JwtStrategy],
})
export class AdminModule {}
