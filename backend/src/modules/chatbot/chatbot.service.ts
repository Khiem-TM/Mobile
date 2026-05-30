import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { randomUUID } from 'crypto';
import axios from 'axios';
import { ChatSession } from './entities/chat-session.entity';
import { ChatMessage } from './entities/chat-message.entity';
import { AiContextService } from './ai-context.service';

const RAG_FALLBACK_REPLY = 'Xin lỗi, trợ lý đang bận. Bạn vui lòng thử lại sau ít phút nhé.';

type ConversationTurn = {
  role: 'user' | 'assistant';
  content: string;
};

export type ChatSourceDto = {
  title: string;
  document_id: string;
  chunk_index?: number | null;
};

export type ChatStreamEvent =
  | { event: 'meta'; data: { intent: string; sources: ChatSourceDto[] } }
  | { event: 'delta'; data: { text: string } }
  | {
      event: 'done';
      data: {
        message: ChatMessage;
        intent: string;
        sources: ChatSourceDto[];
        disclaimer?: string | null;
      };
    }
  | { event: 'error'; data: { message: string } };

type PreparedChatRequest = {
  isFirstMessage: boolean;
  payload: Record<string, unknown>;
  requestId: string;
};

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
    private readonly aiContextService: AiContextService,
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
    const prepared = await this.prepareChatRequest(userId, sessionId, userMessage);
    const reply = await this.callRag(prepared.payload, prepared.requestId);

    const assistantMsg = await this.persistExchange(
      sessionId,
      userMessage,
      reply,
      prepared.isFirstMessage,
    );

    return assistantMsg;
  }

  async *streamMessage(
    userId: string,
    sessionId: string,
    userMessage: string,
  ): AsyncGenerator<ChatStreamEvent> {
    const prepared = await this.prepareChatRequest(userId, sessionId, userMessage);
    let fullReply = '';
    let receivedDelta = false;
    let intent = 'smalltalk';
    let sources: ChatSourceDto[] = [];
    let disclaimer: string | null = null;
    let upstream: any;
    let completed = false;

    try {
      const res = await axios.post(`${this.ragServiceUrl}/chat/stream`, prepared.payload, {
        headers: {
          'X-Internal-Secret': this.ragSecret,
          'X-Request-Id': prepared.requestId,
          Accept: 'text/event-stream',
        },
        responseType: 'stream',
        timeout: 60_000,
        validateStatus: () => true,
      });

      if (res.status >= 400) {
        const body = await this.readStreamText(res.data);
        throw new Error(
          `HTTP ${res.status}${res.statusText ? ` ${res.statusText}` : ''}${body ? `: ${body}` : ''}`,
        );
      }

      upstream = res.data;
      for await (const event of this.parseSseStream(upstream)) {
        if (event.event === 'meta') {
          intent = typeof event.data?.intent === 'string' ? event.data.intent : intent;
          sources = Array.isArray(event.data?.sources) ? event.data.sources : [];
          yield { event: 'meta', data: { intent, sources } };
        } else if (event.event === 'delta') {
          const text = typeof event.data?.text === 'string' ? event.data.text : '';
          if (!text) continue;
          receivedDelta = true;
          fullReply += text;
          yield { event: 'delta', data: { text } };
        } else if (event.event === 'done') {
          intent = typeof event.data?.intent === 'string' ? event.data.intent : intent;
          disclaimer = typeof event.data?.disclaimer === 'string' ? event.data.disclaimer : null;
          if (Array.isArray(event.data?.sources)) sources = event.data.sources;
        } else if (event.event === 'error') {
          throw new Error(event.data?.message ?? 'RAG stream failed');
        }
      }

      completed = true;
      const replyToSave = fullReply.trim() || RAG_FALLBACK_REPLY;
      const assistantMsg = await this.persistExchange(
        sessionId,
        userMessage,
        replyToSave,
        prepared.isFirstMessage,
      );
      yield { event: 'done', data: { message: assistantMsg, intent, sources, disclaimer } };
    } catch (err: any) {
      const message = 'Không thể kết nối trợ lý AI. Vui lòng thử lại sau ít phút.';
      this.logger.warn(
        `RAG stream failed (requestId=${prepared.requestId}, url=${this.ragServiceUrl}/chat/stream): ${this.describeRagError(err)}`,
      );

      if (!receivedDelta) {
        const syncFallbackReply = await this.callRag(prepared.payload, prepared.requestId);
        if (syncFallbackReply && syncFallbackReply !== RAG_FALLBACK_REPLY) {
          const assistantMsg = await this.persistExchange(
            sessionId,
            userMessage,
            syncFallbackReply,
            prepared.isFirstMessage,
          );
          yield { event: 'delta', data: { text: syncFallbackReply } };
          yield {
            event: 'done',
            data: { message: assistantMsg, intent, sources, disclaimer: null },
          };
          return;
        }

        const assistantMsg = await this.persistExchange(
          sessionId,
          userMessage,
          RAG_FALLBACK_REPLY,
          prepared.isFirstMessage,
        );
        yield { event: 'error', data: { message } };
        yield {
          event: 'done',
          data: { message: assistantMsg, intent, sources, disclaimer: null },
        };
      } else {
        yield { event: 'error', data: { message } };
      }
    } finally {
      if (!completed && upstream?.destroy) {
        upstream.destroy();
      }
    }
  }

  /**
   * Gọi RAG /chat với timeout 30s + 1 lần retry cho lỗi mạng/5xx.
   * Trả về reply; nếu thất bại hẳn, trả message fallback thân thiện (không ném lỗi
   * để không chặn UX chat).
   */
  private async callRag(
    payload: Record<string, unknown>,
    requestId: string,
  ): Promise<string> {
    const maxAttempts = 2;
    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        const res = await axios.post(`${this.ragServiceUrl}/chat`, payload, {
          headers: {
            'X-Internal-Secret': this.ragSecret,
            'X-Request-Id': requestId,
          },
          timeout: 30_000,
        });
        return res.data?.reply ?? '';
      } catch (err: any) {
        const status = err?.response?.status;
        const retryable = !status || status >= 500; // không retry 4xx
        this.logger.warn(
          `RAG call failed (attempt ${attempt}/${maxAttempts}, url=${this.ragServiceUrl}/chat): ${this.describeRagError(err)}`,
        );
        if (attempt < maxAttempts && retryable) {
          await new Promise((r) => setTimeout(r, 400 * attempt));
          continue;
        }
        return RAG_FALLBACK_REPLY;
      }
    }
    return '';
  }

  private async prepareChatRequest(
    userId: string,
    sessionId: string,
    userMessage: string,
  ): Promise<PreparedChatRequest> {
    const session = await this.sessionRepo.findOne({
      where: { id: sessionId, user_id: userId },
    });
    if (!session) throw new NotFoundException('Chat session not found');

    const history = await this.messageRepo.find({
      where: { session_id: sessionId },
      order: { created_at: 'ASC' },
      take: 20,
    });

    const conversationHistory: ConversationTurn[] = history.map((msg) => ({
      role: msg.role as 'user' | 'assistant',
      content: msg.content,
    }));

    const userContext = await this.aiContextService
      .buildUserContext(userId)
      .catch((e) => {
        this.logger.warn(`buildUserContext failed: ${e?.message}`);
        return undefined;
      });

    const requestId = randomUUID();
    return {
      isFirstMessage: history.length === 0,
      requestId,
      payload: {
        user_id: userId,
        session_id: sessionId,
        message: userMessage,
        conversation_history: conversationHistory,
        user_context: userContext,
        request_id: requestId,
      },
    };
  }

  private async persistExchange(
    sessionId: string,
    userMessage: string,
    assistantReply: string,
    isFirstMessage: boolean,
  ): Promise<ChatMessage> {
    await this.messageRepo.save(
      this.messageRepo.create({ session_id: sessionId, role: 'user', content: userMessage }),
    );
    const assistantMsg = await this.messageRepo.save(
      this.messageRepo.create({ session_id: sessionId, role: 'assistant', content: assistantReply }),
    );

    const sessionUpdate: Partial<ChatSession> = {
      last_message: assistantReply.slice(0, 120),
    };
    if (isFirstMessage) {
      sessionUpdate.title = userMessage.length > 60
        ? userMessage.slice(0, 60) + '…'
        : userMessage;
    }
    await this.sessionRepo.update(sessionId, sessionUpdate);
    return assistantMsg;
  }

  private async *parseSseStream(
    stream: AsyncIterable<Buffer | string>,
  ): AsyncGenerator<{ event: string; data: any }> {
    let buffer = '';
    let eventName = 'message';
    let dataLines: string[] = [];

    const dispatch = () => {
      if (dataLines.length === 0) {
        eventName = 'message';
        return null;
      }
      const rawData = dataLines.join('\n');
      const event = eventName;
      eventName = 'message';
      dataLines = [];
      try {
        return { event, data: JSON.parse(rawData) };
      } catch {
        return { event, data: rawData };
      }
    };

    for await (const chunk of stream) {
      buffer += typeof chunk === 'string' ? chunk : chunk.toString('utf8');
      buffer = buffer.replace(/\r\n/g, '\n');
      let newlineIndex = buffer.indexOf('\n');
      while (newlineIndex >= 0) {
        const line = buffer.slice(0, newlineIndex);
        buffer = buffer.slice(newlineIndex + 1);
        if (line === '') {
          const parsed = dispatch();
          if (parsed) yield parsed;
        } else if (line.startsWith('event:')) {
          eventName = line.slice(6).trim();
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).trimStart());
        }
        newlineIndex = buffer.indexOf('\n');
      }
    }

    if (buffer.length > 0) {
      if (buffer.startsWith('event:')) eventName = buffer.slice(6).trim();
      else if (buffer.startsWith('data:')) dataLines.push(buffer.slice(5).trimStart());
    }
    const parsed = dispatch();
    if (parsed) yield parsed;
  }

  private async readStreamText(stream: any, maxChars = 2_000): Promise<string> {
    if (!stream) return '';
    if (typeof stream === 'string') return stream.slice(0, maxChars);
    if (typeof stream !== 'object' || !stream[Symbol.asyncIterator]) {
      try {
        return JSON.stringify(stream).slice(0, maxChars);
      } catch {
        return String(stream).slice(0, maxChars);
      }
    }

    let text = '';
    try {
      for await (const chunk of stream as AsyncIterable<Buffer | string>) {
        text += typeof chunk === 'string' ? chunk : chunk.toString('utf8');
        if (text.length >= maxChars) break;
      }
    } catch (e: any) {
      const detail = this.describeRagError(e);
      return text ? `${text.slice(0, maxChars)} [stream read failed: ${detail}]` : `[stream read failed: ${detail}]`;
    }
    return text.slice(0, maxChars);
  }

  private describeRagError(err: any): string {
    if (!err) return 'unknown error';

    const parts: string[] = [];
    const status = err?.response?.status;
    const statusText = err?.response?.statusText;
    const code = err?.code;
    const name = err?.name;
    const message = err?.message;
    const causeMessage = err?.cause?.message;

    if (status) parts.push(`status=${status}${statusText ? ` ${statusText}` : ''}`);
    if (code) parts.push(`code=${code}`);
    if (name && name !== 'Error') parts.push(`name=${name}`);
    if (message) parts.push(`message=${message}`);
    if (causeMessage) parts.push(`cause=${causeMessage}`);

    if (parts.length > 0) return parts.join(', ');

    if (typeof err === 'string') return err || 'empty string error';
    try {
      const serialized = JSON.stringify(err);
      if (serialized && serialized !== '{}') return serialized;
    } catch {
      // fall through to String()
    }
    const fallback = String(err);
    return fallback && fallback !== '[object Object]' ? fallback : 'unknown stream error';
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
