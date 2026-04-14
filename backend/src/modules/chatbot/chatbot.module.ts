import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ChatbotController } from './chatbot.controller';
import { ChatbotService } from './chatbot.service';
import { ChatSession } from './entities/chat-session.entity';
import { ChatMessage } from './entities/chat-message.entity';
import { MealLogsModule } from '../meal-logs/meal-logs.module';
import { BodyMetricsModule } from '../body-metrics/body-metrics.module';
import { TrainingModule } from '../training/training.module';
import { UsersModule } from '../users/users.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([ChatSession, ChatMessage]),
    MealLogsModule,
    BodyMetricsModule,
    TrainingModule,
    UsersModule,
  ],
  controllers: [ChatbotController],
  providers: [ChatbotService],
})
export class ChatbotModule {}
