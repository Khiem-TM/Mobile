import { useQuery } from '@tanstack/react-query';
import { Cloud, Database, Flame, HardDrive, MessageSquare, RefreshCw, Server, ShieldCheck } from 'lucide-react';
import { ErrorState } from '../components/ErrorState';
import { LoadingState } from '../components/LoadingState';
import { StatusBadge, statusVariant } from '../components/StatusBadge';
import { get } from '../lib/api';
import type { ApiErrorShape, HealthResponse } from '../types';

const serviceIcons = {
  db: Database,
  redis: HardDrive,
  kafka: MessageSquare,
  rag: Server,
  firebase: Flame,
  cloudinary: Cloud,
};

export function SystemPage() {
  const healthQuery = useQuery({
    queryKey: ['admin-health'],
    queryFn: () => get<HealthResponse>('/admin/health'),
    refetchInterval: 60_000,
  });

  if (healthQuery.isLoading) return <LoadingState label="Đang kiểm tra hệ thống" />;
  if (healthQuery.error) return <ErrorState message={(healthQuery.error as ApiErrorShape).message} onRetry={() => healthQuery.refetch()} />;

  const services = healthQuery.data?.services ?? {};

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="page-title">Hệ thống</h1>
          <p className="mt-1 text-sm text-muted">Trạng thái Backend, DB, Redis, Kafka, RAG, Firebase và Cloudinary</p>
        </div>
        <button className="btn-secondary" onClick={() => healthQuery.refetch()} type="button">
          <RefreshCw className="h-4 w-4" />
          Làm mới
        </button>
      </div>

      <section className="card p-5">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-primary-soft text-primary">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <div>
              <h2 className="text-lg font-extrabold text-text">Tổng trạng thái</h2>
              <p className="text-sm text-muted">Health endpoint `/admin/health`</p>
            </div>
          </div>
          <StatusBadge variant={statusVariant(healthQuery.data?.status)}>{healthQuery.data?.status ?? 'unknown'}</StatusBadge>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {Object.entries(services).map(([name, service]) => {
          const Icon = serviceIcons[name as keyof typeof serviceIcons] ?? Server;
          return (
            <article className="card p-5" key={name}>
              <div className="mb-4 flex items-start justify-between gap-3">
                <div className="flex min-w-0 items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-faint text-primary">
                    <Icon className="h-5 w-5" />
                  </div>
                  <div className="min-w-0">
                    <h3 className="truncate text-lg font-extrabold capitalize text-text">{name}</h3>
                    <p className="truncate text-sm text-muted">{service.brokers ?? service.message ?? 'Operational check'}</p>
                  </div>
                </div>
                <StatusBadge variant={statusVariant(service.status)}>{service.status ?? '-'}</StatusBadge>
              </div>
              {service.statusCode ? <p className="font-mono text-xs text-muted">HTTP {service.statusCode}</p> : null}
              {service.message ? <p className="mt-2 text-sm text-danger">{service.message}</p> : null}
            </article>
          );
        })}
      </section>
    </div>
  );
}
