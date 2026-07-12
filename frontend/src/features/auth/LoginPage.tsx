import { useState, type FormEvent } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../../api/client'
import type { UserDto } from '../../types'

export function LoginPage() {
  const queryClient = useQueryClient()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string>()
  const [pending, setPending] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setPending(true)
    setError(undefined)
    try {
      // GET przed logowaniem zapewnia cookie XSRF-TOKEN dla pierwszego POST-a
      await api.get('/api/health')
      const user = await api.post<UserDto>('/api/auth/login', { email, password })
      queryClient.setQueryData(['me'], user)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Nie udało się zalogować')
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={submit}>
        <div className="login-logo" aria-hidden>
          🧸
        </div>
        <h1>Ocena Przedszkolaka</h1>
        <label htmlFor="email">E-mail</label>
        <input
          id="email"
          type="email"
          autoComplete="username"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <label htmlFor="password">Hasło</label>
        <input
          id="password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {error && <p className="form-error">{error}</p>}
        <button className="btn-primary" type="submit" disabled={pending}>
          {pending ? 'Logowanie…' : 'Zaloguj się'}
        </button>
      </form>
    </div>
  )
}
