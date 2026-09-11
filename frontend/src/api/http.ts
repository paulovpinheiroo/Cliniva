import { sessionStore } from '@/lib/session'

const VITE_API_URL = (import.meta.env.VITE_API_URL as string | undefined) ?? ''

const BASE_URL = (VITE_API_URL || (import.meta.env.PROD ? 'https://cliniva-backend.fly.dev/api' : '/api')).replace(/\/$/, '')

export class ApiError extends Error {
  status: number
  erros: string[]

  constructor(status: number, mensagem: string, erros: string[] = []) {
    super(mensagem)
    this.name = 'ApiError'
    this.status = status
    this.erros = erros
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = sessionStore.getToken()
  const clinicaId = sessionStore.getClinicaId()

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(clinicaId ? { 'X-Clinica': clinicaId } : {}),
    ...options.headers,
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  })

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type') ?? ''
  const body = contentType.includes('application/json') ? await response.json() : null

  if (!response.ok) {
    throw new ApiError(
      body?.status ?? response.status,
      body?.mensagem ?? body?.error ?? `Erro inesperado (${response.status})`,
      body?.erros ?? [],
    )
  }

  return body as T
}

export const http = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) => request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) => request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: <T>(path: string, body: unknown) => request<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}