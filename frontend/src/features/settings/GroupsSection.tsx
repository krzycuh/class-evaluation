import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { ClassGroup, UserDto } from '../../types'

function currentSchoolYear(): string {
  const now = new Date()
  const startYear = now.getMonth() + 1 >= 9 ? now.getFullYear() : now.getFullYear() - 1
  return `${startYear}/${startYear + 1}`
}

/** Edycja przypisań nauczycielek jednej grupy (checkboxy). */
function GroupTeachers({ group, users, onError }: { group: ClassGroup; users: UserDto[]; onError: (m: string) => void }) {
  const queryClient = useQueryClient()
  const teachersQuery = useQuery({
    queryKey: ['class-group-teachers', group.id],
    queryFn: () => api.get<string[]>(`/api/class-groups/${group.id}/teachers`),
  })

  const save = useMutation({
    mutationFn: (teacherIds: string[]) =>
      api.put<string[]>(`/api/class-groups/${group.id}/teachers`, { teacherIds }),
    onSuccess: (ids) => {
      queryClient.setQueryData(['class-group-teachers', group.id], ids)
      queryClient.invalidateQueries({ queryKey: ['class-groups'] })
    },
    onError: (e) => onError(e.message),
  })

  const assigned = teachersQuery.data ?? []

  return (
    <div className="group-teachers">
      {users.map((u) => (
        <label key={u.id} className={u.active ? '' : 'inactive'}>
          <input
            type="checkbox"
            checked={assigned.includes(u.id)}
            disabled={teachersQuery.isLoading || save.isPending}
            onChange={(e) => {
              const next = e.target.checked ? [...assigned, u.id] : assigned.filter((id) => id !== u.id)
              save.mutate(next)
            }}
          />
          {u.displayName}
          {!u.active && ' (nieaktywna)'}
        </label>
      ))}
    </div>
  )
}

/** Sekcja admina: grupy — tworzenie, zmiana nazwy, przypisania nauczycielek. */
export function GroupsSection() {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string>()
  const [name, setName] = useState('')
  const [schoolYear, setSchoolYear] = useState(currentSchoolYear())
  const [expandedId, setExpandedId] = useState<string>()

  const groupsQuery = useQuery({
    queryKey: ['class-groups'],
    queryFn: () => api.get<ClassGroup[]>('/api/class-groups'),
  })
  const usersQuery = useQuery({
    queryKey: ['users'],
    queryFn: () => api.get<UserDto[]>('/api/users'),
  })

  const addGroup = useMutation({
    mutationFn: () => api.post<ClassGroup>('/api/class-groups', { name, schoolYear }),
    onSuccess: () => {
      setName('')
      setError(undefined)
      queryClient.invalidateQueries({ queryKey: ['class-groups'] })
    },
    onError: (e) => setError(e.message),
  })

  return (
    <section className="settings-section">
      <h2>Grupy</h2>
      {error && <p className="form-error">{error}</p>}

      <div className="settings-list">
        {(groupsQuery.data ?? []).map((g) => (
          <div key={g.id}>
            <div className="settings-row">
              <span className="n">
                {g.name}
                <span className="sub"> {g.schoolYear}</span>
              </span>
              <button
                className="minibtn"
                onClick={() => setExpandedId(expandedId === g.id ? undefined : g.id)}
              >
                {expandedId === g.id ? 'Zwiń' : 'Nauczycielki'}
              </button>
            </div>
            {expandedId === g.id && (
              <GroupTeachers group={g} users={usersQuery.data ?? []} onError={setError} />
            )}
          </div>
        ))}
      </div>

      <h3>Dodaj grupę</h3>
      <div className="add-student-form">
        <input placeholder="Nazwa grupy" value={name} onChange={(e) => setName(e.target.value)} />
        <input
          placeholder="Rok szkolny"
          aria-label="Rok szkolny"
          value={schoolYear}
          onChange={(e) => setSchoolYear(e.target.value)}
        />
        <button
          className="btn-primary"
          disabled={!name.trim() || !/^\d{4}\/\d{4}$/.test(schoolYear) || addGroup.isPending}
          onClick={() => addGroup.mutate()}
        >
          Dodaj
        </button>
      </div>
      <p className="hint">Nauczycielka widzi tylko grupy, do których jest przypisana.</p>
    </section>
  )
}
