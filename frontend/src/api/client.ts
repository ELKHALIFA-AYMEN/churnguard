import { tokenStore, UNAUTHORIZED_EVENT } from '../auth/session'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function readMessage(response: Response): Promise<string> {
  try {
    const body: unknown = await response.json()
    if (typeof body === 'object' && body !== null && 'detail' in body) {
      const { detail } = body as { detail: unknown }
      if (typeof detail === 'string') return detail
    }
  } catch {
    // corps absent ou non JSON : message générique ci-dessous
  }
  return `Erreur ${response.status}`
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  if (!path.startsWith('/api/')) {
    throw new Error('Only same-origin /api/ paths are allowed')
  }

  const headers = new Headers(init.headers)
  const token = tokenStore.get()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (typeof init.body === 'string' && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  let response: Response
  try {
    response = await fetch(path, { ...init, headers })
  } catch {
    throw new ApiError(0, 'Serveur injoignable')
  }

  if (response.status === 401 && token) {
    tokenStore.clear()
    window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
  }
  if (!response.ok) {
    throw new ApiError(response.status, await readMessage(response))
  }
  return (await response.json()) as T
}