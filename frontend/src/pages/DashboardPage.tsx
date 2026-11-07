import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'

interface Me {
  userId: string
  organizationId: string
  role: string
  authorities: string[]
}

const ROLE_LABELS = {
  ADMIN: 'Administrateur',
  ANALYST: 'Analyste',
  VIEWER: 'Lecteur',
} as const

export default function DashboardPage() {
  const { session, logout } = useAuth()
  const [me, setMe] = useState<Me | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    api<Me>('/api/me')
      .then((result) => {
        if (!cancelled) setMe(result)
      })
      .catch(() => {
        if (!cancelled) setError("Impossible de joindre l'API.")
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <>
      <header className="topbar">
        <strong>ChurnGuard</strong>
        <span className="muted">{session ? ROLE_LABELS[session.role] : ''}</span>
        <button type="button" className="secondary" onClick={logout}>
          Se déconnecter
        </button>
      </header>
      <main className="page">
        <h1>Tableau de bord</h1>
        {error && <p className="error">{error}</p>}
        {me && (
          <p className="muted">
            Connecté avec le rôle <strong>{me.role}</strong>, vérifié par le serveur.
          </p>
        )}
      </main>
    </>
  )
}