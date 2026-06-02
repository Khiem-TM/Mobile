import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { post } from './api';
import { clearStoredSession, getStoredSession, setStoredSession } from './session';
import type { AdminSession } from '../types';

interface AuthContextValue {
  session: AdminSession | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AdminSession | null>(() => getStoredSession());

  const logout = useCallback(() => {
    clearStoredSession();
    setSession(null);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const nextSession = await post<AdminSession>('/admin/auth/login', { email, password });
    setStoredSession(nextSession);
    setSession(nextSession);
  }, []);

  useEffect(() => {
    const handleExpired = () => logout();
    window.addEventListener('admin-session-expired', handleExpired);
    return () => window.removeEventListener('admin-session-expired', handleExpired);
  }, [logout]);

  const value = useMemo(
    () => ({
      session,
      isAuthenticated: Boolean(session?.access_token),
      login,
      logout,
    }),
    [login, logout, session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
