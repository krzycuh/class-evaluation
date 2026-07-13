import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { api } from '../../api/client'

/** Zmiana własnego hasła — dostępna dla każdej zalogowanej osoby. */
export function ChangePasswordForm() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [repeatPassword, setRepeatPassword] = useState('')
  const [error, setError] = useState<string>()
  const [saved, setSaved] = useState(false)

  const change = useMutation({
    mutationFn: () => api.post('/api/auth/password', { currentPassword, newPassword }),
    onSuccess: () => {
      setCurrentPassword('')
      setNewPassword('')
      setRepeatPassword('')
      setError(undefined)
      setSaved(true)
    },
    onError: (e) => {
      setSaved(false)
      setError(e.message)
    },
  })

  const mismatch = repeatPassword.length > 0 && newPassword !== repeatPassword

  return (
    <div>
      <h3>Zmień hasło</h3>
      {error && <p className="form-error">{error}</p>}
      {mismatch && <p className="form-error">Hasła nie są takie same.</p>}
      {saved && <p className="hint">Hasło zostało zmienione.</p>}
      <div className="add-student-form">
        <input
          type="password"
          placeholder="Obecne hasło"
          autoComplete="current-password"
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
        />
        <input
          type="password"
          placeholder="Nowe hasło (min. 8 znaków)"
          autoComplete="new-password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
        />
        <input
          type="password"
          placeholder="Powtórz nowe hasło"
          autoComplete="new-password"
          value={repeatPassword}
          onChange={(e) => setRepeatPassword(e.target.value)}
        />
        <button
          className="btn-primary"
          disabled={
            !currentPassword || newPassword.length < 8 || newPassword !== repeatPassword || change.isPending
          }
          onClick={() => change.mutate()}
        >
          Zmień
        </button>
      </div>
    </div>
  )
}
