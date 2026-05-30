import {
  Controller,
  Get,
  Post,
  Delete,
  Param,
  Body,
  UseGuards,
  HttpCode,
  HttpStatus,
  Res,
} from '@nestjs/common';
import {
  ApiTags,
  ApiBearerAuth,
  ApiOperation,
} from '@nestjs/swagger';
import { JwtAuthGuard } from '../../common/guards/jwt.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/interfaces/jwt-payload.interface';
import { ChatbotService } from './chatbot.service';
import { SendMessageDto } from './dto/chat.dto';
import type { Response } from 'express';

@ApiTags('chatbot')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('chatbot')
export class ChatbotController {
  constructor(private readonly chatbotService: ChatbotService) {}

  @ApiOperation({ summary: 'Create a new chat session' })
  @Post('sessions')
  createSession(@CurrentUser() user: JwtPayload) {
    return this.chatbotService.createSession(user.sub);
  }

  @ApiOperation({ summary: 'List user chat sessions (last 10)' })
  @Get('sessions')
  getSessions(@CurrentUser() user: JwtPayload) {
    return this.chatbotService.getSessions(user.sub);
  }

  @ApiOperation({ summary: 'Get messages for a chat session' })
  @Get('sessions/:id/messages')
  getMessages(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.chatbotService.getMessages(user.sub, id);
  }

  @ApiOperation({ summary: 'Get a chat session with message history' })
  @Get('sessions/:id')
  getSession(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.chatbotService.getSession(user.sub, id);
  }

  @ApiOperation({ summary: 'Delete a chat session and all its messages' })
  @Delete('sessions/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteSession(@CurrentUser() user: JwtPayload, @Param('id') id: string) {
    return this.chatbotService.deleteSession(user.sub, id);
  }

  @ApiOperation({ summary: 'Send a message and receive AI response' })
  @Post('sessions/:id/messages')
  sendMessage(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: SendMessageDto,
  ) {
    return this.chatbotService.sendMessage(user.sub, id, dto.content);
  }

  @ApiOperation({ summary: 'Send a message and stream AI response tokens' })
  @Post('sessions/:id/messages/stream')
  async streamMessage(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: SendMessageDto,
    @Res() res: Response,
  ) {
    res.status(HttpStatus.OK);
    res.setHeader('Content-Type', 'text/event-stream; charset=utf-8');
    res.setHeader('Cache-Control', 'no-cache, no-transform');
    res.setHeader('Connection', 'keep-alive');
    res.flushHeaders?.();

    let closed = false;
    res.on('close', () => {
      closed = true;
    });

    const writeEvent = (event: string, data: unknown) => {
      if (closed || res.writableEnded) return false;
      res.write(`event: ${event}\n`);
      res.write(`data: ${JSON.stringify(data)}\n\n`);
      return true;
    };

    try {
      for await (const event of this.chatbotService.streamMessage(user.sub, id, dto.content)) {
        if (!writeEvent(event.event, event.data)) break;
      }
    } catch (e: any) {
      writeEvent('error', { message: e?.message ?? 'Không thể kết nối trợ lý AI.' });
    } finally {
      if (!res.writableEnded) res.end();
    }
  }
}
