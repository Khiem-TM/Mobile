import axios, { AxiosError, type AxiosRequestConfig } from 'axios';
import { clearStoredSession, getStoredSession } from './session';
import type { ApiErrorShape } from '../types';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:3005';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20_000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = getStoredSession()?.access_token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => {
    if (response.status === 204) return undefined;
    const body = response.data;
    if (body && typeof body === 'object' && 'success' in body && 'data' in body) {
      return body.data;
    }
    return body;
  },
  (error: AxiosError) => {
    const normalized = normalizeApiError(error);
    if (normalized.statusCode === 401) {
      clearStoredSession();
      window.dispatchEvent(new Event('admin-session-expired'));
    }
    return Promise.reject(normalized);
  },
);

export function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.get<unknown, T>(url, config);
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.post<unknown, T>(url, data, config);
}

export function patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.patch<unknown, T>(url, data, config);
}

export function del<T = void>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.delete<unknown, T>(url, config);
}

export function compactParams(params: Record<string, unknown>) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}

function normalizeApiError(error: AxiosError): ApiErrorShape {
  const statusCode = error.response?.status;
  const data = error.response?.data as
    | { message?: string | string[]; error?: string; statusCode?: number }
    | undefined;
  const rawMessage = data?.message ?? data?.error ?? error.message;
  return {
    message: Array.isArray(rawMessage) ? rawMessage.join(', ') : rawMessage || 'Không thể kết nối máy chủ',
    statusCode: data?.statusCode ?? statusCode,
    details: error.response?.data,
  };
}
