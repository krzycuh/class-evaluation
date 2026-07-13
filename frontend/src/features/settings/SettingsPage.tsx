import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import { useApp } from '../../AppContext'
import type { AgeGroup, PeriodStatus, StudentWithProgress } from '../../types'
import { ChangePasswordForm } from './ChangePasswordForm'
import { GroupsSection } from './GroupsSection'
import { TeachersSection } from './TeachersSection'

export function SettingsPage() {
  const { user, classGroup, period, periods } = useApp()
  const queryClient = useQueryClient()
  const isAdmin = user.role === 'ADMIN'
  const [error, setError] = useState<string>()

  const studentsQuery = useQuery({
    queryKey: ['students', classGroup.id, period.id],
    queryFn: () =>
      api.get<StudentWithProgress[]>(`/api/class-groups/${classGroup.id}/students?periodId=${period.id}`),
  })
  const ageGroupsQuery = useQuery({
    queryKey: ['age-groups'],
    queryFn: () => api.get<AgeGroup[]>('/api/age-groups'),
  })

  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [birthDate, setBirthDate] = useState('')

  const invalidateStudents = () =>
    queryClient.invalidateQueries({ queryKey: ['students', classGroup.id, period.id] })

  const addStudent = useMutation({
    mutationFn: () =>
      api.post(`/api/class-groups/${classGroup.id}/students`, { firstName, lastName, birthDate }),
    onSuccess: () => {
      setFirstName('')
      setLastName('')
      setBirthDate('')
      setError(undefined)
      invalidateStudents()
    },
    onError: (e) => setError(e.message),
  })

  const changeAgeGroup = useMutation({
    mutationFn: ({ studentId, ageGroupId }: { studentId: string; ageGroupId: string }) =>
      api.patch(`/api/students/${studentId}`, { ageGroupId }),
    onSuccess: invalidateStudents,
    onError: (e) => setError(e.message),
  })

  const removeStudent = useMutation({
    mutationFn: (studentId: string) => api.delete(`/api/students/${studentId}`),
    onSuccess: invalidateStudents,
    onError: (e) => setError(e.message),
  })

  const togglePeriod = useMutation({
    mutationFn: ({ id, status }: { id: string; status: PeriodStatus }) =>
      api.patch(`/api/periods/${id}`, { status }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['periods'] }),
    onError: (e) => setError(e.message),
  })

  const ageGroups = ageGroupsQuery.data ?? []

  return (
    <div className="settings-page">
      <div className="page-head">
        <h1>Ustawienia</h1>
      </div>

      {error && <p className="form-error">{error}</p>}

      <section className="settings-section">
        <h2>Uczniowie — {classGroup.name}</h2>
        <div className="settings-list">
          {(studentsQuery.data ?? []).map((s) => (
            <div key={s.id} className="settings-row">
              <span className="n">
                {s.firstName} {s.lastName}
              </span>
              <select
                aria-label={`Grupa wiekowa: ${s.firstName} ${s.lastName}`}
                value={s.ageGroupId}
                onChange={(e) => changeAgeGroup.mutate({ studentId: s.id, ageGroupId: e.target.value })}
              >
                {ageGroups.map((g) => (
                  <option key={g.id} value={g.id}>
                    {g.name}
                  </option>
                ))}
              </select>
              <button
                className="minibtn danger"
                onClick={() => {
                  if (window.confirm(`Zarchiwizować ucznia ${s.firstName} ${s.lastName}? Oceny zostaną zachowane.`)) {
                    removeStudent.mutate(s.id)
                  }
                }}
              >
                Archiwizuj
              </button>
            </div>
          ))}
        </div>

        <h3>Dodaj ucznia</h3>
        <div className="add-student-form">
          <input placeholder="Imię" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
          <input placeholder="Nazwisko" value={lastName} onChange={(e) => setLastName(e.target.value)} />
          <input
            type="date"
            aria-label="Data urodzenia"
            value={birthDate}
            onChange={(e) => setBirthDate(e.target.value)}
          />
          <button
            className="btn-primary"
            disabled={!firstName.trim() || !lastName.trim() || !birthDate || addStudent.isPending}
            onClick={() => addStudent.mutate()}
          >
            Dodaj
          </button>
        </div>
        <p className="hint">Grupa wiekowa zostanie podpowiedziana z daty urodzenia — można ją potem zmienić.</p>
      </section>

      <section className="settings-section">
        <h2>Semestry</h2>
        <div className="settings-list">
          {periods.map((p) => (
            <div key={p.id} className="settings-row">
              <span className="n">
                {p.name} {p.schoolYear}
              </span>
              <span className={`status-pill ${p.status === 'OPEN' ? 'ready' : 'missing'}`}>
                {p.status === 'OPEN' ? 'otwarty' : 'zamknięty'}
              </span>
              {isAdmin && (
                <button
                  className="minibtn"
                  onClick={() =>
                    togglePeriod.mutate({ id: p.id, status: p.status === 'OPEN' ? 'CLOSED' : 'OPEN' })
                  }
                >
                  {p.status === 'OPEN' ? 'Zamknij' : 'Otwórz'}
                </button>
              )}
            </div>
          ))}
        </div>
        {!isAdmin && <p className="hint">Semestry otwiera i zamyka administrator.</p>}
      </section>

      {isAdmin && <TeachersSection currentUserId={user.id} />}
      {isAdmin && <GroupsSection />}

      <section className="settings-section">
        <h2>Konto</h2>
        <p>
          Zalogowano jako <strong>{user.displayName}</strong> ({user.email}) — rola{' '}
          {user.role === 'ADMIN' ? 'administrator' : 'nauczyciel'}.
        </p>
        <ChangePasswordForm />
      </section>
    </div>
  )
}
