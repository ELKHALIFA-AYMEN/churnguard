export type Role = 'ADMIN' | 'ANALYST' | 'VIEWER'

export interface Session {
  token: string
  userId: string
  organizationId: string
  role: Role
  expiresAt: number
}

const STORAGE_KEY = 'churnguard.token'

export const UNAUTHORIZED_EVENT = 'churnguard:unauthorized'

export const tokenStore = {
  get: () => sessionStorage.getItem(STORAGE_KEY),
  set: (token: string) => sessionStorage.setItem(STORAGE_KEY, token),
  clear: () => sessionStorage.removeItem(STORAGE_KEY),
}

const ROLES: readonly string[] = ['ADMIN', 'ANALYST', 'VIEWER']

/**
 * Lit le contenu du jeton pour l'AFFICHAGE uniquement (rôle, expiration).
 * Le navigateur ne vérifie pas la signature : c'est le serveur qui décide des droits.
 */
export function parseToken(token: string): Session | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null

    const claims: unknown = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    if (typeof claims !== 'object' || claims === null) return null

    const { sub, org, role, exp } = claims as Record<string, unknown>
    if (typeof sub !== 'string' || typeof org !== 'string') return null
    if (typeof role !== 'string' || !ROLES.includes(role)) return null
    if (typeof exp !== 'number') return null

    const expiresAt = exp * 1000
    if (expiresAt <= Date.now()) return null

    return { token, userId: sub, organizationId: org, role: role as Role, expiresAt }
  } catch {
    return null
  }
}