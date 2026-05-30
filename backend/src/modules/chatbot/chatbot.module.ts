import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ChatbotController } from './chatbot.controller';
import { ChatbotService } from './chatbot.service';
import { AiContextService } from './ai-context.service';
import { ChatSession } from './entities/chat-session.entity';
import { ChatMessage } from './entities/chat-message.entity';
import { UserModule } from '../user/user.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([ChatSession, ChatMessage]),
    forwardRef(() => UserModule), // dùng UsersService + DashboardService để build UserContext
  ],
  controllers: [ChatbotController],
  providers: [ChatbotService, AiContextService],
  exports: [ChatbotService],
})
export class ChatbotModule {}
