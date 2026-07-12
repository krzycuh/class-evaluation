import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'
import { useApp } from '../../AppContext'
import type { StudentWithProgress } from '../../types'

export function StudentsPage() {
  const { classGroup, period } = useApp()
  const [search, setSearch] = useState('')
  const [onlyIncomplete, setOnlyIncomplete] = useState(false)

  const studentsQuery = useQuery({
    queryKey: ['students', classGroup.id, period.id],
    queryFn: () =>
      api.get<StudentWithProgress[]>(`/api/class-groups/${classGroup.id}/students?periodId=${period.id}`),
  })

  const students = (studentsQuery.data ?? []).filter((s) => {
    const matchesSearch = `${s.firstName} ${s.lastName}`.toLowerCase().includes(search.toLowerCase())
    const matchesFilter = !onlyIncomplete || s.assessedCount < s.totalSkills
    return matchesSearch && matchesFilter
  })

  const total = studentsQuery.data?.length ?? 0
  const incomplete = (studentsQuery.data ?? []).filter((s) => s.assessedCount < s.totalSkills).length

  if (studentsQuery.isLoading) return <div className="page-loading">Wczytywanie…</div>

  return (
    <div className="students-page">
      <div className="searchrow">
        <input
          type="search"
          placeholder="Szukaj ucznia…"
          aria-label="Szukaj ucznia"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>
      <div className="filterrow">
        <button className={onlyIncomplete ? '' : 'active'} onClick={() => setOnlyIncomplete(false)}>
          Wszyscy ({total})
        </button>
        <button className={onlyIncomplete ? 'active' : ''} onClick={() => setOnlyIncomplete(true)}>
          Do uzupełnienia ({incomplete})
        </button>
      </div>

      {total === 0 && (
        <p className="empty-state">
          Brak uczniów w grupie. Dodaj dzieci w <Link to="/settings">Ustawieniach</Link>.
        </p>
      )}

      <ul className="student-list">
        {students.map((s) => {
          const done = s.totalSkills > 0 && s.assessedCount >= s.totalSkills
          const pct = s.totalSkills === 0 ? 0 : Math.round((100 * s.assessedCount) / s.totalSkills)
          return (
            <li key={s.id}>
              <Link className="student-card" to={`/students/${s.id}/assess`}>
                <span className="dot">
                  {s.firstName[0]}
                  {s.lastName[0]}
                </span>
                <span className="info">
                  <span className="name">
                    {s.firstName} {s.lastName}
                  </span>
                  <span className="meta">
                    <span className="agebadge">{s.ageGroupName}</span>
                    <span className="progress">
                      <span style={{ width: `${pct}%` }} />
                    </span>
                  </span>
                </span>
                <span className={`count${done ? ' done' : ''}`}>
                  {s.assessedCount}/{s.totalSkills}
                  {done ? ' ✓' : ''}
                </span>
              </Link>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
