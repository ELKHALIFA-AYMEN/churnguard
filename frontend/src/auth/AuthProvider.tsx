import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api } from '../api/client'
import { AuthContext } from './AuthContext'
import { parseToken, tokenStore, UNAUTHORIZED_EVENT } from './session'
import type { Session } from './session'

function loadSession(): Session | null {
  const stored = tokenStore.get()
  if (!stored) return null
  const session = parseToken(stored)
  if (!session) tokenStore.clear()
  return session
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(loadSession)

  const logout = useCallback(() => {
    tokenStore.clear()
    setSession(null)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const response = await api<{ accessToken: string }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
    const parsed = parseToken(response.accessToken)
    if (!parsed) throw new Error('Invalid token received')
    tokenStore.set(response.accessToken)
    setSession(parsed)
  }, [])

  useEffect(() => {
    window.addEventListener(UNAUTHORIZED_EVENT, logout)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, logout)
  }, [logout])

  useEffect(() => {
    if (!session) return
    const timer = window.setTimeout(logout, Math.max(session.expiresAt - Date.now(), 0))
    return () => window.clearTimeout(timer)
  }, [session, logout])

  const value = useMemo(() => ({ session, login, logout }), [session, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}