import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Activity, Lock, Mail } from 'lucide-react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { useAuth } from '../lib/auth';
import type { ApiErrorShape } from '../types';

const schema = z.object({
  email: z.string().email('Email không hợp lệ'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu'),
});

type LoginValues = z.infer<typeof schema>;

export function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [apiError, setApiError] = useState('');
  const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? '/';

  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register,
  } = useForm<LoginValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  });

  if (isAuthenticated) return <Navigate replace to="/" />;

  const onSubmit = handleSubmit(async (values) => {
    setApiError('');
    try {
      await login(values.email, values.password);
      navigate(from, { replace: true });
    } catch (error) {
      setApiError((error as ApiErrorShape).message ?? 'Đăng nhập thất bại');
    }
  });

  return (
    <main className="flex min-h-screen items-center justify-center bg-background p-4">
      <section className="w-full max-w-md rounded-lg border border-border bg-surface p-7 shadow-float">
        <div className="mb-8 flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-primary text-white">
            <Activity className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-extrabold tracking-normal text-text">Calories Tracker</h1>
            <p className="text-xs font-extrabold uppercase tracking-wide text-primary">Hệ thống quản trị</p>
          </div>
        </div>
        <form className="space-y-4" onSubmit={onSubmit}>
          <label className="block">
            <span className="mb-1.5 block text-sm font-bold text-text">Email admin</span>
            <span className="relative block">
              <Mail className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
              <input className="input pl-10" placeholder="admin@calories.local" type="email" {...register('email')} />
            </span>
            {errors.email ? <span className="mt-1 block text-xs font-semibold text-danger">{errors.email.message}</span> : null}
          </label>
          <label className="block">
            <span className="mb-1.5 block text-sm font-bold text-text">Mật khẩu</span>
            <span className="relative block">
              <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
              <input className="input pl-10" type="password" {...register('password')} />
            </span>
            {errors.password ? <span className="mt-1 block text-xs font-semibold text-danger">{errors.password.message}</span> : null}
          </label>
          {apiError ? (
            <div className="rounded-md border border-danger/25 bg-danger-soft/45 px-3 py-2 text-sm font-semibold text-danger">
              {apiError}
            </div>
          ) : null}
          <button className="btn-primary w-full" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Đang đăng nhập' : 'Đăng nhập'}
          </button>
        </form>
      </section>
    </main>
  );
}
