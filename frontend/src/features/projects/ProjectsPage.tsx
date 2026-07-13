import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import { useApp } from '../../AppContext'
import type { Project, ProjectKind } from '../../types'
import { fmtRange, KIND_EMOJI, KIND_LABEL, STATUS_LABEL } from '../calendar/calendarUtils'

export function ProjectsPage() {
  const { classGroup } = useApp()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [adding, setAdding] = useState(false)
  const [title, setTitle] = useState('')
  const [kind, setKind] = useState<ProjectKind>('TRIP')
  const [preschoolWide, setPreschoolWide] = useState(false)
  const [startsOn, setStartsOn] = useState('')
  const [endsOn, setEndsOn] = useState('')
  const [error, setError] = useState('')

  const projectsQuery = useQuery({
    queryKey: ['projects', classGroup.id],
    queryFn: () => api.get<Project[]>(`/api/projects?classGroupId=${classGroup.id}`),
  })

  const create = useMutation({
    mutationFn: () =>
      api.post<Project>('/api/projects', {
        title,
        kind,
        scope: preschoolWide ? 'PRESCHOOL' : 'CLASS_GROUP',
        classGroupId: preschoolWide ? null : classGroup.id,
        startsOn,
        endsOn: endsOn && endsOn >= startsOn ? endsOn : startsOn,
      }),
    onSuccess: (project) => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      queryClient.invalidateQueries({ queryKey: ['calendar'] })
      navigate(`/projects/${project.id}`)
    },
    onError: (e) => setError(e instanceof Error ? e.message : 'Błąd zapisu'),
  })

  function submit() {
    if (!title.trim()) return setError('Podaj nazwę projektu.')
    if (!startsOn) return setError('Podaj datę rozpoczęcia.')
    setError('')
    create.mutate()
  }

  if (projectsQuery.isLoading) return <div className="page-loading">Wczytywanie…</div>

  const projects = projectsQuery.data ?? []

  return (
    <div className="projects-page">
      <div className="page-head projects-head">
        <div>
          <h1>Projekty</h1>
          <p className="hint">Wycieczki, konkursy i inne przedsięwzięcia z checklistą i postępem.</p>
        </div>
        <Link className="minibtn" to="/calendar">📅 Kalendarz</Link>
      </div>

      {projects.length === 0 && !adding && (
        <p className="empty-state">Brak projektów. Dodaj pierwszy — np. wycieczkę albo konkurs.</p>
      )}

      <ul className="project-list">
        {projects.map((p) => {
          const pct = p.totalTasks === 0 ? 0 : Math.round((100 * p.doneTasks) / p.totalTasks)
          return (
            <li key={p.id}>
              <Link className="project-card" to={`/projects/${p.id}`}>
                <span className="dot">{KIND_EMOJI[p.kind]}</span>
                <span className="info">
                  <span className="name">{p.title}</span>
                  <span className="meta">
                    <span className="agebadge">{fmtRange(p.startsOn, p.endsOn)}</span>
                    {p.scope === 'PRESCHOOL' && <span className="agebadge">przedszkole</span>}
                    {p.totalTasks > 0 && (
                      <span className="progress">
                        <span style={{ width: `${pct}%` }} />
                      </span>
                    )}
                  </span>
                </span>
                <span className="pstatus">
                  <span className={`status-pill ${p.status === 'DONE' ? 'ready' : p.status === 'IN_PROGRESS' ? 'gen' : 'missing'}`}>
                    {STATUS_LABEL[p.status]}
                  </span>
                  {p.totalTasks > 0 && (
                    <span className="count">
                      {p.doneTasks}/{p.totalTasks}
                    </span>
                  )}
                </span>
              </Link>
            </li>
          )
        })}
      </ul>

      {adding ? (
        <div className="editpanel">
          <h3>Nowy projekt</h3>
          <label htmlFor="pr-title">Nazwa</label>
          <input id="pr-title" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="np. Wycieczka do ZOO" />

          <label htmlFor="pr-kind">Rodzaj</label>
          <select id="pr-kind" value={kind} onChange={(e) => setKind(e.target.value as ProjectKind)}>
            {(Object.keys(KIND_LABEL) as ProjectKind[]).map((k) => (
              <option key={k} value={k}>
                {KIND_EMOJI[k]} {KIND_LABEL[k]}
              </option>
            ))}
          </select>

          <div className="daterange">
            <div>
              <label htmlFor="pr-start">Od</label>
              <input id="pr-start" type="date" value={startsOn} onChange={(e) => setStartsOn(e.target.value)} />
            </div>
            <div>
              <label htmlFor="pr-end">Do</label>
              <input id="pr-end" type="date" value={endsOn} onChange={(e) => setEndsOn(e.target.value)} />
            </div>
          </div>

          <label className="checkline">
            <input type="checkbox" checked={preschoolWide} onChange={(e) => setPreschoolWide(e.target.checked)} />
            Projekt całego przedszkola (widoczny dla wszystkich grup)
          </label>

          {error && <p className="form-error">{error}</p>}

          <div className="editactions">
            <button className="btn-ghost" onClick={() => setAdding(false)}>Anuluj</button>
            <button className="btn-primary" disabled={create.isPending} onClick={submit}>
              {create.isPending ? 'Zapisywanie…' : 'Utwórz projekt'}
            </button>
          </div>
        </div>
      ) : (
        <div className="addrow">
          <button onClick={() => setAdding(true)}>+ Nowy projekt</button>
        </div>
      )}
    </div>
  )
}
