import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import axios from 'axios';
import { ChatSession } from './entities/chat-session.entity';
import { ChatMessage } from './entities/chat-message.entity';

@Injectable()
export class ChatbotService {
  private readonly logger = new Logger(ChatbotService.name);
  private readonly ragServiceUrl = process.env.RAG_SERVICE_URL ?? 'http://localhost:8001';
  private readonly ragSecret = process.env.RAG_INTERNAL_SECRET ?? 'dev-secret';

  constructor(
    @InjectRepository(ChatSession)
    private readonly sessionRepo: Repository<ChatSession>,
    @InjectRepository(ChatMessage)
    private readonly messageRepo: Repository<ChatMessage>,
  ) {}

  async createSession(userId: string): Promise<ChatSession> {
    const session = this.sessionRepo.create({ user_id: userId });
    return this.sessionRepo.save(session);
  }

  async getSessions(userId: string): Promise<ChatSession[]> {
    return this.sessionRepo.find({
      where: { user_id: userId },
      order: { created_at: 'DESC' },
      take: 20,
    });
  }

  async getSession(userId: string, sessionId: string): Promise<ChatSession> {
    const session = await this.sessionRepo.findOne({
      where: { id: sessionId, user_id: userId },
      relations: ['messages'],
      order: { messages: { created_at: 'ASC' } },
    });
    if (!session) throw new NotFoundException('Chat session not found');
    return session;
  }

  async getMessages(userId: string, sessionId: string): Promise<ChatMessage[]> {
    const session = await this.sessionRepo.findOne({
      where: { id: sessionId, user_id: userId },
    });
    if (!session) throw new NotFoundException('Chat session not found');
    return this.messageRepo.find({
      where: { session_id: sessionId },
      order: { created_at: 'ASC' },
    });
  }

  async deleteSession(userId: string, sessionId: string): Promise<void> {
    const session = await this.sessionRepo.findOne({
      where: { id: sessionId, user_id: userId },
    });
    if (!session) throw new NotFoundException('Chat session not found');
    await this.sessionRepo.delete(sessionId);
  }

  async sendMessage(
    userId: string,
    sessionId: string,
    userMessage: string,
  ): Promise<ChatMessage> {
    const session = await this.sessionRepo.findOne({
      where: { id: sessionId, user_id: userId },
    });
    if (!session) throw new NotFoundException('Chat session not found');

    // Load last 10 conversation turns to pass as history context
    const history = await this.messageRepo.find({
      where: { session_id: sessionId },
      order: { created_at: 'ASC' },
      take: 20,
    });

    const isFirstMessage = history.length === 0;

    const conversationHistory = history.map((msg) => ({
      role: msg.role as 'user' | 'assistant',
      content: msg.content,
    }));

    // Delegate to RAG service
    const ragResponse = await axios.post(
      `${this.ragServiceUrl}/chat`,
      {
        user_id: userId,
        session_id: sessionId,
        message: userMessage,
        conversation_history: conversationHistory,
      },
      {
        headers: { 'X-Internal-Secret': this.ragSecret },
        timeout: 60_000,
      },
    );

    const reply: string = ragResponse.data.reply ?? '';

    // Persist user message + assistant reply
    await this.messageRepo.save(
      this.messageRepo.create({ session_id: sessionId, role: 'user', content: userMessage }),
    );
    const assistantMsg = await this.messageRepo.save(
      this.messageRepo.create({ session_id: sessionId, role: 'assistant', content: reply }),
    );

    // Update session title (first message) and last_message preview
    const sessionUpdate: Partial<ChatSession> = {
      last_message: reply.slice(0, 120),
    };
    if (isFirstMessage) {
      sessionUpdate.title = userMessage.length > 60
        ? userMessage.slice(0, 60) + '…'
        : userMessage;
    }
    await this.sessionRepo.update(sessionId, sessionUpdate);

    return assistantMsg;
  }

  /** Fire-and-forget: ask RAG service to re-embed this user's data. */
  triggerUserEmbed(userId: string): void {
    axios
      .post(
        `${this.ragServiceUrl}/embed/user/${userId}`,
        {},
        { headers: { 'X-Internal-Secret': this.ragSecret }, timeout: 5_000 },
      )
      .catch((err) =>
        this.logger.warn(`RAG embed trigger failed for user ${userId}: ${err.message}`),
      );
  }
}
