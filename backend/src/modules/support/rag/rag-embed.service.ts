import { Injectable, Logger } from '@nestjs/common';
import axios from 'axios';

@Injectable()
export class RagEmbedService {
  private readonly logger = new Logger(RagEmbedService.name);
  private readonly ragServiceUrl = process.env.RAG_SERVICE_URL ?? 'http://localhost:8001';
  private readonly ragSecret = process.env.RAG_INTERNAL_SECRET ?? 'dev-secret';

  /** Fire-and-forget: queue a re-embed of all user data in the RAG service. */
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
