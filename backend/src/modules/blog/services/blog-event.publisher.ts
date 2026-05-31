import { Inject, Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { ClientKafka } from '@nestjs/microservices';

export const KAFKA_CLIENT = 'KAFKA_CLIENT';

/**
 * Phát event Blog lên Kafka (fire-and-forget). Gắn cờ KAFKA_ENABLED để dev
 * không cần broker vẫn chạy: khi tắt, emit là no-op (không chặn request user).
 */
@Injectable()
export class BlogEventPublisher implements OnModuleInit {
  private readonly logger = new Logger(BlogEventPublisher.name);
  private readonly enabled: boolean;

  constructor(
    @Inject(KAFKA_CLIENT) private readonly client: ClientKafka,
    config: ConfigService,
  ) {
    this.enabled = config.get<string>('KAFKA_ENABLED', 'true') !== 'false';
  }

  async onModuleInit(): Promise<void> {
    if (!this.enabled) {
      this.logger.warn('KAFKA_ENABLED=false -> Blog không phát event (no-op).');
      return;
    }
    try {
      await this.client.connect();
      this.logger.log('Kafka producer (blog) đã kết nối.');
    } catch (e) {
      this.logger.warn(`Kết nối Kafka producer thất bại: ${(e as Error).message}`);
    }
  }

  emit(topic: string, message: unknown): void {
    if (!this.enabled) return;
    try {
      this.client.emit(topic, message).subscribe({
        error: (e) => this.logger.warn(`Emit ${topic} lỗi: ${e.message}`),
      });
    } catch (e) {
      this.logger.warn(`Emit ${topic} ném lỗi: ${(e as Error).message}`);
    }
  }
}
