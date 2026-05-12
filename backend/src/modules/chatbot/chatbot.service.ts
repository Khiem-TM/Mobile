import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import ModelClient, { isUnexpected } from '@azure-rest/ai-inference';
import { AzureKeyCredential } from '@azure/core-auth';
import { ChatSession } from './entities/chat-session.entity';
import { ChatMessage } from './entities/chat-message.entity';
import { MealLogsService } from '../food/services/meal-logs.service';
import { BodyMetricsService } from '../train/services/body-metrics.service';
import { TrainingService } from '../train/services/training.service';
import { UsersService } from '../user/services/users.service';
import { RedisService } from '../support/redis/redis.service';

const CHATBOT_CTX_TTL = 600; // 10 minutes

@Injectable()
export class ChatbotService {
  private readonly client: ReturnType<typeof ModelClient>;
  private readonly model = 'openai/gpt-4o-mini';
  private readonly endpoint = 'https://models.github.ai/inference';

  constructor(
    @InjectRepository(ChatSession)
    private readonly sessionRepo: Repository<ChatSession>,
    @InjectRepository(ChatMessage)
    private readonly messageRepo: Repository<ChatMessage>,
    private readonly mealLogsService: MealLogsService,
    private readonly bodyMetricsService: BodyMetricsService,
    private readonly trainingService: TrainingService,
    private readonly usersService: UsersService,
    private readonly redisService: RedisService,
  ) {
    this.client = ModelClient(
      this.endpoint,
      new AzureKeyCredential(process.env.GITHUB_TOKEN || ''),
    );
  }

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

    // Aggregate user context — cached per user per day to avoid 4 DB queries per message
    const today = new Date().toISOString().split('T')[0];
    const ctxKey = `chatbot:ctx:${userId}:${today}`;
    let ctx = await this.redisService.getJson<{
      bodyMetric: any; healthProfile: any; dailySummary: any; recentWorkouts: any[];
    }>(ctxKey);

    if (!ctx) {
      const [bodyMetric, healthProfile, dailySummary, recentWorkouts] =
        await Promise.all([
          this.bodyMetricsService.getLatest(userId).catch(() => null),
          this.usersService.getHealthProfile(userId).catch(() => null),
          this.mealLogsService.getDailySummary(userId, today).catch(() => null),
          this.trainingService.getWorkoutHistory(userId, 7).catch(() => []),
        ]);
      ctx = { bodyMetric, healthProfile, dailySummary, recentWorkouts };
      await this.redisService.setJson(ctxKey, ctx, CHATBOT_CTX_TTL);
    }

    const systemContent = this.buildSystemPrompt(ctx);

    // Load last 20 messages as history
    const history = await this.messageRepo.find({
      where: { session_id: sessionId },
      order: { created_at: 'ASC' },
      take: 20,
    });

    const isFirstMessage = history.length === 0;

    const messages: Array<{ role: string; content: string }> = [
      { role: 'system', content: systemContent },
      ...history.map((msg) => ({
        role: msg.role === 'user' ? 'user' : 'assistant',
        content: msg.content,
      })),
      { role: 'user', content: userMessage },
    ];

    const response = await this.client.path('/chat/completions').post({
      body: { model: this.model, messages, max_tokens: 800 },
    });

    if (isUnexpected(response)) {
      console.error('[Chatbot] API error:', response.body.error);
      throw new Error(response.body.error?.message ?? 'AI service error');
    }

    const reply = response.body.choices[0]?.message?.content ?? '';

    // Persist user message + assistant reply
    await this.messageRepo.save(
      this.messageRepo.create({ session_id: sessionId, role: 'user', content: userMessage }),
    );
    const assistantMsg = await this.messageRepo.save(
      this.messageRepo.create({ session_id: sessionId, role: 'assistant', content: reply }),
    );

    // Update session title (from first user message) and last_message preview
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

  private buildSystemPrompt(ctx: {
    bodyMetric: any;
    healthProfile: any;
    dailySummary: any;
    recentWorkouts: any[];
  }): string {
    const lines: string[] = [
      'Bạn là trợ lý sức khỏe và dinh dưỡng cá nhân thông minh.',
      'Hãy đưa ra lời khuyên dựa trên dữ liệu thực tế của người dùng bên dưới.',
      'Trả lời bằng tiếng Việt, ngắn gọn và thực tế.',
      '',
      '=== DỮ LIỆU NGƯỜI DÙNG ===',
    ];

    if (ctx.healthProfile) {
      const p = ctx.healthProfile;
      lines.push(`Giới tính: ${p.gender ?? 'chưa rõ'}`);
      lines.push(`Chiều cao: ${p.heightCm ?? 'chưa rõ'} cm`);
      lines.push(`Cân nặng ban đầu: ${p.initialWeightKg ?? 'chưa rõ'} kg`);
      lines.push(`Mức độ hoạt động: ${p.activityLevel ?? 'chưa rõ'}`);
      lines.push(`Mục tiêu cân nặng: ${p.weightGoalKg ?? 'chưa đặt'} kg`);
      lines.push(`Mục tiêu calo/ngày: ${p.dailyCaloriesGoal ?? p.caloriesGoal ?? 'chưa đặt'} kcal`);
      lines.push(`Chế độ ăn: ${p.dietType ?? 'không có'}`);
      if (p.goalType) lines.push(`Mục tiêu tập luyện: ${p.goalType}`);
      if (p.proteinGoalG) lines.push(`Mục tiêu đạm: ${p.proteinGoalG}g, béo: ${p.fatGoalG ?? 0}g, tinh bột: ${p.carbsGoalG ?? 0}g`);
    }

    if (ctx.bodyMetric) {
      const m = ctx.bodyMetric;
      lines.push(`Cân nặng hiện tại: ${m.weightKg ?? 'chưa đo'} kg`);
      lines.push(`BMI: ${m.bmi ?? 'chưa tính'}`);
      lines.push(`TDEE: ${m.tdee ?? 'chưa tính'} kcal`);
    }

    if (ctx.dailySummary) {
      const s = ctx.dailySummary;
      lines.push(`Hôm nay đã ăn: ${s.total_calories ?? 0} kcal, đạm ${s.total_protein ?? 0}g, béo ${s.total_fat ?? 0}g, tinh bột ${s.total_carbs ?? 0}g`);
    }

    if (ctx.recentWorkouts?.length) {
      const names = ctx.recentWorkouts
        .flatMap((w: any) => w.details?.map((d: any) => d.exercise?.name) ?? [])
        .filter(Boolean)
        .slice(0, 5);
      if (names.length) lines.push(`Bài tập gần đây: ${names.join(', ')}`);
    }

    lines.push('');
    lines.push('Hãy trả lời câu hỏi tiếp theo dựa trên thông tin trên.');
    return lines.join('\n');
  }
}
