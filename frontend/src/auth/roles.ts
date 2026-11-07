import type { Role } from './session'

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Administrateur',
  ANALYST: 'Analyste',
  VIEWER: 'Lecteur',
}

export function canWrite(role: Role): boolean {
  return role === 'ADMIN' || role === 'ANALYST'
}