import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { CreatedTeacher, UserDto } from '../../types'

/** Sekcja admina: konta nauczycielek — lista, dodawanie, dezaktywacja, reset hasła. */
export function TeachersSection({ currentUserId }: { currentUserId: string }) {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string>()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  // hasło startowe / po resecie — widoczne tylko do odświeżenia strony
  const [revealedPassword, setRevealedPassword] = useState<{ email: string; password: string }>()

  const usersQuery = useQuery({
    queryKey: ['users'],
    queryFn: () => api.get<UserDto[]>('/api/users'),
  })

  const invalidateUsers = () => queryClient.invalidateQueries({ queryKey: ['users'] })

  const addTeacher = useMutation({
    mutationFn: () => api.post<CreatedTeacher>('/api/users', { email, displayName }),
    onSuccess: (created) => {
      setDisplayName('')
      setEmail('')
      setError(undefined)
      setRevealedPassword({ email: created.user.email, password: created.initialPassword })
      invalidateUsers()
    },
    onError: (e) => setError(e.message),
  })

  const toggleActive = useMutation({
    mutationFn: (u: UserDto) => api.patch(`/api/users/${u.id}`, { active: !u.active }),
    onSuccess: () => {
      setError(undefined)
      invalidateUsers()
    },
    onError: (e) => setError(e.message),
  })

  const resetPassword = useMutation({
    mutationFn: (u: UserDto) => api.post<{ newPassword: string }>(`/api/users/${u.id}/password-reset`),
    onSuccess: (result, u) => {
      setError(undefined)
      setRevealedPassword({ email: u.email, password: result.newPassword })
    },
    onError: (e) => setError(e.message),
  })

  return (
    <section className="settings-section">
      <h2>Nauczycielki</h2>
      {error && <p className="form-error">{error}</p>}
      {revealedPassword && (
        <p className="password-reveal">
          Hasło dla <strong>{revealedPassword.email}</strong>: <code>{revealedPassword.password}</code>
          <br />
          Przekaż je nauczycielce — nie będzie widoczne ponownie.
        </p>
      )}

      <div className="settings-list">
        {(usersQuery.data ?? []).map((u) => (
          <div key={u.id} className="settings-row">
            <span className="n">
              {u.displayName}
              <span className="sub"> {u.email}</span>
            </span>
            <span className={`status-pill ${u.active ? 'ready' : 'missing'}`}>
              {u.role === 'ADMIN' ? 'admin' : u.active ? 'aktywna' : 'nieaktywna'}
            </span>
            <button className="minibtn" onClick={() => resetPassword.mutate(u)}>
              Reset hasła
            </button>
            {u.id !== currentUserId && (
              <button
                className={`minibtn ${u.active ? 'danger' : ''}`}
                onClick={() => {
                  if (
                    !u.active ||
                    window.confirm(`Dezaktywować konto ${u.displayName}? Grupy i oceny zostaną zachowane.`)
                  ) {
                    toggleActive.mutate(u)
                  }
                }}
              >
                {u.active ? 'Dezaktywuj' : 'Aktywuj'}
              </button>
            )}
          </div>
        ))}
      </div>

      <h3>Dodaj nauczycielkę</h3>
      <div className="add-student-form">
        <input
          placeholder="Imię i nazwisko"
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
        />
        <input
          type="email"
          placeholder="E-mail"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <button
          className="btn-primary"
          disabled={!displayName.trim() || !email.trim() || addTeacher.isPending}
          onClick={() => addTeacher.mutate()}
        >
          Dodaj
        </button>
      </div>
      <p className="hint">Hasło startowe zostanie wygenerowane i pokazane jednorazowo po dodaniu.</p>
    </section>
  )
}
