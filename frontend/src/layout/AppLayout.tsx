import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ROLE_LABELS } from '../auth/roles'

export default function AppLayout() {
  const { session, logout } = useAuth()

  return (
    <>
      <header className="topbar">
        <strong>ChurnGuard</strong>
        <nav className="nav" aria-label="Navigation principale">
          <NavLink to="/" end>Tableau de bord</NavLink>
          <NavLink to="/data">Données</NavLink>
        </nav>
        <span className="muted">{session ? ROLE_LABELS[session.role] : ''}</span>
        <button type="button" className="secondary" onClick={logout}>
          Se déconnecter
        </button>
      </header>
      <Outlet />
    </>
  )
}