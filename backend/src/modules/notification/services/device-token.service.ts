import { Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
import { DeviceToken } from '../entities/device-token.entity';

@Injectable()
export class DeviceTokenService {
  private readonly logger = new Logger(DeviceTokenService.name);

  constructor(
    @InjectRepository(DeviceToken)
    private readonly repo: Repository<DeviceToken>,
  ) {}

  /** Lưu/cập nhật token. Token unique => nếu đã tồn tại, gán lại cho user hiện tại. */
  async upsert(
    userId: string,
    token: string,
    platform: 'android' = 'android',
  ): Promise<void> {
    const existing = await this.repo.findOne({ where: { token } });
    if (existing) {
      if (existing.userId !== userId || existing.platform !== platform) {
        existing.userId = userId;
        existing.platform = platform;
        await this.repo.save(existing);
      }
      return;
    }
    await this.repo.save(this.repo.create({ userId, token, platform }));
  }

  async remove(token: string): Promise<void> {
    await this.repo.delete({ token });
  }

  async getTokensForUser(userId: string): Promise<string[]> {
    const rows = await this.repo.find({
      where: { userId },
      select: ['token'],
    });
    return rows.map((r) => r.token);
  }

  /** Xoá các token chết do FCM báo (registration-token-not-registered, ...). */
  async pruneTokens(tokens: string[]): Promise<void> {
    if (!tokens.length) return;
    await this.repo.delete({ token: In(tokens) });
    this.logger.log(`Pruned ${tokens.length} dead device token(s).`);
  }
}
