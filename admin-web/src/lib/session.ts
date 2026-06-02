import type { AdminSession } from '../types';

export const ADMIN_SESSION_KEY = 'calories_tracker_admin_session';

export function getStoredSession(): AdminSession | null {
  try {
    const raw = localStorage.getItem(ADMIN_SESSION_KEY);
    return raw ? (JSON.parse(raw) as AdminSession) : null;
  } catch {
    return null;
  }
}

export function setStoredSession(session: AdminSession) {
  localStorage.setItem(ADMIN_SESSION_KEY, JSON.stringify(session));
}

export function clearStoredSession() {
  localStorage.removeItem(ADMIN_SESSION_KEY);
}
