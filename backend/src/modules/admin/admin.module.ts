import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { User } from '../users/entities/user.entity';
import { Food } from '../foods/entities/food.entity';
import { Exercise } from '../training/entities/exercise.entity';
import { WorkoutSession } from '../training/entities/workout-session.entity';
import { Blog } from './entities/blog.entity';
import { AdminService } from './admin.service';
import { AdminController } from './admin.controller';
import { AdminAuthController } from './admin-auth.controller';
import { JwtStrategy } from '../auth/strategies/jwt.strategy';

@Module({
  imports: [
    TypeOrmModule.forFeature([User, Food, Exercise, WorkoutSession, Blog]),
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
