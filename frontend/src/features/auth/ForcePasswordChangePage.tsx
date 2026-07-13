import { api } from '../../api/client'
import type { UserDto } from '../../types'
import { ChangePasswordForm } from '../settings/ChangePasswordForm'

/** Blokuje aplikację do czasu zmiany hasła startowego (users.must_change_password). */
export function ForcePasswordChangePage({ user }: { user: UserDto }) {
  async function logout() {
    try {
      await api.post('/api/auth/logout')
    } finally {
      window.location.assign('/')
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo" aria-hidden>
          🔑
        </div>
        <h1>Ustaw własne hasło</h1>
        <p className="hint">
          Cześć, {user.displayName}! Logujesz się hasłem startowym — zanim zaczniesz,
          ustaw własne. Obecne hasło to te, które otrzymałaś od administratora.
        </p>
        <ChangePasswordForm />
        <button className="btn-ghost" onClick={logout}>
          Wyloguj
        </button>
      </div>
    </div>
  )
}
