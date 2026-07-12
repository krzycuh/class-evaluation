import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { useApp } from '../AppContext'

export function Layout() {
  const { user, classGroup, period, periods, setPeriodId } = useApp()
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  async function logout() {
    await api.post('/api/auth/logout')
    queryClient.clear()
    navigate('/')
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
          <strong>{classGroup.name}</strong>
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
        <button className="avatar" onClick={logout} title={`${user.displayName} — wyloguj`}>
          {initials}
        </button>
      </header>

      <main className="content">
        <Outlet />
      </main>

      <nav className="bottomnav" aria-label="Nawigacja główna">
        <NavLink to="/students">
          <span className="ico">👧</span>Uczniowie
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
