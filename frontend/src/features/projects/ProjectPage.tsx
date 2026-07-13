import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../../api/client'
import type { Project, ProjectKind, ProjectStatus } from '../../types'
import { fmtFull, iso, KIND_EMOJI, KIND_LABEL, STATUS_LABEL } from '../calendar/calendarUtils'

export function ProjectPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [editing, setEditing] = useState(false)
  const [newTask, setNewTask] = useState('')
  const [newTaskDue, setNewTaskDue] = useState('')
  const descTimer = useRef<number>()

  const queryKey = ['project', projectId]
  const projectQuery = useQuery({
    queryKey,
    queryFn: () => api.get<Project>(`/api/projects/${projectId}`),
  })
  const project = projectQuery.data

  function invalidate() {
    queryClient.invalidateQueries({ queryKey })
    queryClient.invalidateQueries({ queryKey: ['projects'] })
    queryClient.invalidateQueries({ queryKey: ['calendar'] })
  }

  const patchProject = useMutation({
    mutationFn: (body: Partial<Pick<Project, 'title' | 'description' | 'kind' | 'startsOn' | 'endsOn' | 'status'>>) =>
      api.patch<Project>(`/api/projects/${projectId}`, body),
    onSuccess: invalidate,
  })

  const addTask = useMutation({
    mutationFn: () => api.post(`/api/projects/${projectId}/tasks`, { title: newTask, dueOn: newTaskDue || null }),
    onSuccess: () => {
      setNewTask('')
      setNewTaskDue('')
      invalidate()
    },
  })

  const patchTask = useMutation({
    mutationFn: ({ taskId, done }: { taskId: string; done: boolean }) =>
      api.patch(`/api/projects/${projectId}/tasks/${taskId}`, { done }),
    onSuccess: invalidate,
  })

  const deleteTask = useMutation({
    mutationFn: (taskId: string) => api.delete(`/api/projects/${projectId}/tasks/${taskId}`),
    onSuccess: invalidate,
  })

  const deleteProject = useMutation({
    mutationFn: () => api.delete(`/api/projects/${projectId}`),
    onSuccess: () => {
      invalidate()
      navigate('/projects')
    },
  })

  if (projectQuery.isLoading) return <div className="page-loading">Wczytywanie…</div>
  if (!project) return <div className="page-loading">Nie znaleziono projektu.</div>

  const tasks = project.tasks ?? []
  const pct = project.totalTasks === 0 ? 0 : Math.round((100 * project.doneTasks) / project.totalTasks)
  const today = iso(new Date())
  const readOnly = !project.canEdit

  return (
    <div className="project-page">
      <div className="assess-header">
        <button className="backbtn" onClick={() => navigate('/projects')} aria-label="Wróć do projektów">
          ←
        </button>
        <div className="assess-student">
          <strong>
            {KIND_EMOJI[project.kind]} {project.title}
          </strong>
          <span>
            {fmtFull(project.startsOn)} – {fmtFull(project.endsOn)}
            {project.scope === 'PRESCHOOL' ? ' · całe przedszkole' : ''}
          </span>
        </div>
        {readOnly ? (
          <span className="status-pill gen">{STATUS_LABEL[project.status]}</span>
        ) : (
          <select
            className="status-select"
            value={project.status}
            aria-label="Status projektu"
            onChange={(e) => patchProject.mutate({ status: e.target.value as ProjectStatus })}
          >
            {(Object.keys(STATUS_LABEL) as ProjectStatus[]).map((s) => (
              <option key={s} value={s}>
                {STATUS_LABEL[s]}
              </option>
            ))}
          </select>
        )}
      </div>

      {project.totalTasks > 0 && (
        <div className="project-progress">
          <span className="progress">
            <span style={{ width: `${pct}%` }} />
          </span>
          <span className="count">
            {project.doneTasks}/{project.totalTasks}
          </span>
        </div>
      )}

      <section className="cal-section">
        <h2>
          Zadania
          {!readOnly && (
            <button className="iconbtn" aria-label="Edytuj projekt" onClick={() => setEditing((v) => !v)}>
              ✎
            </button>
          )}
        </h2>

        {editing && !readOnly && (
          <ProjectEditForm
            project={project}
            pending={patchProject.isPending}
            onSave={(body) => {
              patchProject.mutate(body)
              setEditing(false)
            }}
            onDelete={() => {
              if (window.confirm('Usunąć projekt razem z zadaniami?')) deleteProject.mutate()
            }}
          />
        )}

        {tasks.length === 0 && <p className="empty-state">Brak zadań — dodaj kroki, z których wyliczy się postęp.</p>}
        <ul className="tasklist project-tasks">
          {tasks.map((t) => (
            <li key={t.id}>
              <label>
                <input
                  type="checkbox"
                  checked={t.done}
                  disabled={readOnly}
                  onChange={(e) => patchTask.mutate({ taskId: t.id, done: e.target.checked })}
                />
                <span className={t.done ? 'done' : ''}>{t.title}</span>
              </label>
              {t.dueOn && (
                <span className={`duebadge${!t.done && t.dueOn < today ? ' late' : ''}`}>
                  do {fmtFull(t.dueOn)}
                  {!t.done && t.dueOn < today ? ' ⚠️' : ''}
                </span>
              )}
              {!readOnly && (
                <button className="iconbtn" aria-label="Usuń zadanie" onClick={() => deleteTask.mutate(t.id)}>
                  ✕
                </button>
              )}
            </li>
          ))}
        </ul>

        {!readOnly && (
          <div className="addtask">
            <input
              value={newTask}
              placeholder="Nowe zadanie, np. zebrać zgody rodziców"
              onChange={(e) => setNewTask(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && newTask.trim()) addTask.mutate()
              }}
            />
            <input
              type="date"
              value={newTaskDue}
              aria-label="Termin zadania"
              onChange={(e) => setNewTaskDue(e.target.value)}
            />
            <button className="minibtn" disabled={!newTask.trim() || addTask.isPending} onClick={() => addTask.mutate()}>
              Dodaj
            </button>
          </div>
        )}
      </section>

      <div className="generalnote">
        <label htmlFor="project-note">Notatki</label>
        <textarea
          id="project-note"
          defaultValue={project.description ?? ''}
          disabled={readOnly}
          placeholder="Kontakty, ustalenia, rzeczy do zapamiętania…"
          onChange={(e) => {
            window.clearTimeout(descTimer.current)
            const description = e.target.value
            descTimer.current = window.setTimeout(() => patchProject.mutate({ description }), 600)
          }}
        />
      </div>
    </div>
  )
}

function ProjectEditForm(props: {
  project: Project
  pending: boolean
  onSave: (body: { title: string; kind: ProjectKind; startsOn: string; endsOn: string }) => void
  onDelete: () => void
}) {
  const [title, setTitle] = useState(props.project.title)
  const [kind, setKind] = useState<ProjectKind>(props.project.kind)
  const [startsOn, setStartsOn] = useState(props.project.startsOn)
  const [endsOn, setEndsOn] = useState(props.project.endsOn)

  return (
    <div className="editpanel">
      <label htmlFor="pe-title">Nazwa</label>
      <input id="pe-title" value={title} onChange={(e) => setTitle(e.target.value)} />

      <label htmlFor="pe-kind">Rodzaj</label>
      <select id="pe-kind" value={kind} onChange={(e) => setKind(e.target.value as ProjectKind)}>
        {(Object.keys(KIND_LABEL) as ProjectKind[]).map((k) => (
          <option key={k} value={k}>
            {KIND_EMOJI[k]} {KIND_LABEL[k]}
          </option>
        ))}
      </select>

      <div className="daterange">
        <div>
          <label htmlFor="pe-start">Od</label>
          <input id="pe-start" type="date" value={startsOn} onChange={(e) => setStartsOn(e.target.value)} />
        </div>
        <div>
          <label htmlFor="pe-end">Do</label>
          <input id="pe-end" type="date" value={endsOn} onChange={(e) => setEndsOn(e.target.value)} />
        </div>
      </div>

      <div className="editactions">
        <button className="minibtn danger" onClick={props.onDelete}>
          Usuń projekt
        </button>
        <button
          className="btn-primary"
          disabled={props.pending || !title.trim()}
          onClick={() => props.onSave({ title, kind, startsOn, endsOn: endsOn < startsOn ? startsOn : endsOn })}
        >
          Zapisz
        </button>
      </div>
    </div>
  )
}
