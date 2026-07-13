import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { ClassGroup, TeacherAssignment, UserDto } from '../../types'

function currentSchoolYear(): string {
  const now = new Date()
  const startYear = now.getMonth() + 1 >= 9 ? now.getFullYear() : now.getFullYear() - 1
  return `${startYear}/${startYear + 1}`
}

function nextSchoolYear(schoolYear: string): string {
  const startYear = Number(schoolYear.split('/')[0])
  return Number.isNaN(startYear) ? '' : `${startYear + 1}/${startYear + 2}`
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
      queryClient.invalidateQueries({ queryKey: ['class-group-assignments'] })
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

/** Przejście grupy na nowy rok szkolny — dzieci i nauczycielki przechodzą do nowej grupy. */
function GroupRollover({ group, onDone, onError }: { group: ClassGroup; onDone: () => void; onError: (m: string) => void }) {
  const queryClient = useQueryClient()
  const [schoolYear, setSchoolYear] = useState(nextSchoolYear(group.schoolYear))

  const rollover = useMutation({
    mutationFn: () => api.post<ClassGroup>(`/api/class-groups/${group.id}/rollover`, { schoolYear }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['class-groups'] })
      queryClient.invalidateQueries({ queryKey: ['class-group-assignments'] })
      queryClient.invalidateQueries({ queryKey: ['periods'] })
      onDone()
    },
    onError: (e) => onError(e.message),
  })

  return (
    <div className="group-rollover">
      <p className="hint">
        Powstanie grupa „{group.name}" w nowym roku z tymi samymi nauczycielkami.
        Dzieci przejdą do niej z przeliczoną grupą wiekową, a historia ocen zostanie
        przy semestrach starego roku. Semestry nowego roku dodadzą się same.
      </p>
      <div className="add-student-form">
        <input
          aria-label="Nowy rok szkolny"
          value={schoolYear}
          onChange={(e) => setSchoolYear(e.target.value)}
        />
        <button
          className="btn-primary"
          disabled={!/^\d{4}\/\d{4}$/.test(schoolYear) || rollover.isPending}
          onClick={() => {
            if (window.confirm(`Przenieść grupę „${group.name}" na rok ${schoolYear}?`)) {
              rollover.mutate()
            }
          }}
        >
          Przenieś
        </button>
      </div>
    </div>
  )
}

/** Sekcja admina: grupy — tworzenie, przypisania nauczycielek, nowy rok szkolny. */
export function GroupsSection() {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string>()
  const [name, setName] = useState('')
  const [schoolYear, setSchoolYear] = useState(currentSchoolYear())
  const [expanded, setExpanded] = useState<{ id: string; panel: 'teachers' | 'rollover' }>()

  const groupsQuery = useQuery({
    queryKey: ['class-groups'],
    queryFn: () => api.get<ClassGroup[]>('/api/class-groups'),
  })
  const usersQuery = useQuery({
    queryKey: ['users'],
    queryFn: () => api.get<UserDto[]>('/api/users'),
  })
  const assignmentsQuery = useQuery({
    queryKey: ['class-group-assignments'],
    queryFn: () => api.get<TeacherAssignment[]>('/api/class-groups/assignments'),
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

  const users = usersQuery.data ?? []
  const teacherNames = (groupId: string) =>
    (assignmentsQuery.data ?? [])
      .filter((a) => a.classGroupId === groupId)
      .map((a) => users.find((u) => u.id === a.userId)?.displayName)
      .filter(Boolean)
      .join(', ')

  const togglePanel = (id: string, panel: 'teachers' | 'rollover') =>
    setExpanded(expanded?.id === id && expanded.panel === panel ? undefined : { id, panel })

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
                <span className="sub">
                  {' '}
                  {g.schoolYear}
                  {teacherNames(g.id) && ` · ${teacherNames(g.id)}`}
                </span>
              </span>
              <button className="minibtn" onClick={() => togglePanel(g.id, 'teachers')}>
                {expanded?.id === g.id && expanded.panel === 'teachers' ? 'Zwiń' : 'Nauczycielki'}
              </button>
              <button className="minibtn" onClick={() => togglePanel(g.id, 'rollover')}>
                {expanded?.id === g.id && expanded.panel === 'rollover' ? 'Zwiń' : 'Nowy rok'}
              </button>
            </div>
            {expanded?.id === g.id && expanded.panel === 'teachers' && (
              <GroupTeachers group={g} users={users} onError={setError} />
            )}
            {expanded?.id === g.id && expanded.panel === 'rollover' && (
              <GroupRollover group={g} onDone={() => setExpanded(undefined)} onError={setError} />
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
