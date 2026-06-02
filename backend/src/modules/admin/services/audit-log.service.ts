import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { AuditLog, AuditLogStatus } from '../entities/audit-log.entity';

export interface AdminAuditContext {
  actorUserId?: string | null;
  actorEmail?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
}

export interface AuditLogInput {
  actorUserId?: string | null;
  actorEmail?: string | null;
  action: string;
  targetType: string;
  targetId?: string | null;
  status?: AuditLogStatus;
  ipAddress?: string | null;
  userAgent?: string | null;
  metadata?: Record<string, unknown> | null;
  errorMessage?: string | null;
}

@Injectable()
export class AuditLogService {
  constructor(
    @InjectRepository(AuditLog)
    private readonly repo: Repository<AuditLog>,
  ) {}

  async record(input: AuditLogInput): Promise<void> {
    await this.repo.save(
      this.repo.create({
        actorUserId: input.actorUserId ?? null,
        actorEmail: input.actorEmail ?? null,
        action: input.action,
        targetType: input.targetType,
        targetId: input.targetId ?? null,
        status: input.status ?? 'success',
        ipAddress: input.ipAddress ?? null,
        userAgent: input.userAgent ?? null,
        metadata: input.metadata ?? null,
        errorMessage: input.errorMessage ?? null,
      }),
    );
  }

  async recordFromContext(
    context: AdminAuditContext | undefined,
    input: Omit<AuditLogInput, 'actorUserId' | 'actorEmail' | 'ipAddress' | 'userAgent'>,
  ): Promise<void> {
    await this.record({
      ...input,
      actorUserId: context?.actorUserId ?? null,
      actorEmail: context?.actorEmail ?? null,
      ipAddress: context?.ipAddress ?? null,
      userAgent: context?.userAgent ?? null,
    });
  }
}
