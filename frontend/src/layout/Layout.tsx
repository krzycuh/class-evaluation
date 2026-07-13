import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { api } from '../api/client'
import { useApp } from '../AppContext'

export function Layout() {
  const { user, classGroup, classGroups, period, periods, setPeriodId, setClassGroupId } = useApp()
  const [menuOpen, setMenuOpen] = useState(false)

  async function logout() {
    try {
      await api.post('/api/auth/logout')
    } finally {
      // Twardy przeładunek zamiast nawigacji SPA: gwarantuje wyczyszczenie
      // całego stanu (react-query, konteksty) po unieważnieniu sesji.
      window.location.assign('/')
    }
  }

  const initials = user.displayName
    .split(' ')
    .map((part) => part[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)

  return (
    <div className="shell">
      <header className="appbar">
        <div className="appbar-title">
          {classGroups.length > 1 ? (
            <select
              className="group-select"
              value={classGroup.id}
              onChange={(e) => setClassGroupId(e.target.value)}
              aria-label="Wybierz grupę"
            >
              {classGroups.map((g) => (
                <option key={g.id} value={g.id}>
                  {g.name}
                </option>
              ))}
            </select>
          ) : (
            <strong>{classGroup.name}</strong>
          )}
          <span>rok {classGroup.schoolYear}</span>
        </div>
        <select
          className="period-select"
          value={period.id}
          onChange={(e) => setPeriodId(e.target.value)}
          aria-label="Wybierz semestr"
        >
          {periods.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name} {p.schoolYear} {p.status === 'CLOSED' ? '(zamknięty)' : ''}
            </option>
          ))}
        </select>
        <div className="avatar-wrap">
          <button
            className="avatar"
            onClick={() => setMenuOpen((v) => !v)}
            title={user.displayName}
            aria-haspopup="menu"
            aria-expanded={menuOpen}
          >
            {initials}
          </button>
          {menuOpen && (
            <>
              <div className="menu-backdrop" onClick={() => setMenuOpen(false)} />
              <div className="avatar-menu" role="menu">
                <div className="avatar-menu-user">
                  <strong>{user.displayName}</strong>
                  <span>{user.email}</span>
                </div>
                <button role="menuitem" onClick={logout}>
                  Wyloguj
                </button>
              </div>
            </>
          )}
        </div>
      </header>

      <main className="content">
        <Outlet />
      </main>

      <nav className="bottomnav" aria-label="Nawigacja główna">
        <NavLink to="/students">
          <span className="ico">👧</span>Uczniowie
        </NavLink>
        <NavLink to="/calendar">
          <span className="ico">📅</span>Kalendarz
        </NavLink>
        <NavLink to="/reports">
          <span className="ico">📄</span>Raporty
        </NavLink>
        <NavLink to="/skills">
          <span className="ico">⭐</span>Umiejętności
        </NavLink>
        <NavLink to="/settings">
          <span className="ico">⚙️</span>Ustawienia
        </NavLink>
      </nav>
    </div>
  )
}
