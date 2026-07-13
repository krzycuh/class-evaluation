import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import { useApp } from '../../AppContext'
import type { CalendarEventDetails, EventCategory, EventScope, StudentWithProgress } from '../../types'

interface DraftTask {
  title: string
  done: boolean
}

/** Panel tworzenia / edycji wydarzenia z notatkami i checklistą organizera. */
export function EventEditor(props: {
  categories: EventCategory[]
  initial: CalendarEventDetails | null
  defaultDate: string
  onClose: () => void
}) {
  const { user, classGroup, period } = useApp()
  const queryClient = useQueryClient()
  const initial = props.initial

  const [title, setTitle] = useState(initial?.title ?? '')
  const [categoryId, setCategoryId] = useState(initial?.categoryId ?? props.categories[0]?.id ?? '')
  const [scope, setScope] = useState<EventScope>(initial?.scope ?? 'CLASS_GROUP')
  const [studentId, setStudentId] = useState(initial?.studentId ?? '')
  const [startsOn, setStartsOn] = useState(initial?.startsOn ?? props.defaultDate)
  const [endsOn, setEndsOn] = useState(initial?.endsOn ?? props.defaultDate)
  const [yearly, setYearly] = useState(initial?.yearlyRecurring ?? false)
  const [description, setDescription] = useState(initial?.description ?? '')
  const [tasks, setTasks] = useState<DraftTask[]>(initial?.tasks.map((t) => ({ title: t.title, done: t.done })) ?? [])
  const [error, setError] = useState('')

  const studentsQuery = useQuery({
    queryKey: ['students', classGroup.id, period.id],
    queryFn: () =>
      api.get<StudentWithProgress[]>(`/api/class-groups/${classGroup.id}/students?periodId=${period.id}`),
    enabled: scope === 'STUDENT',
  })

  const save = useMutation({
    mutationFn: async () => {
      const groupScoped = scope === 'CLASS_GROUP' || scope === 'STUDENT'
      const payload = {
        title,
        description: description || null,
        categoryId,
        scope,
        classGroupId: groupScoped ? classGroup.id : null,
        studentId: scope === 'STUDENT' ? studentId : null,
        startsOn,
        endsOn: endsOn < startsOn ? startsOn : endsOn,
        yearlyRecurring: yearly,
      }
      const saved = initial
        ? await api.patch<CalendarEventDetails>(`/api/events/${initial.id}`, payload)
        : await api.post<CalendarEventDetails>('/api/events', payload)
      await api.put(`/api/events/${saved.id}/tasks`, {
        tasks: tasks.filter((t) => t.title.trim()).map((t) => ({ title: t.title, done: t.done })),
      })
      return saved
    },
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['calendar'] })
      queryClient.invalidateQueries({ queryKey: ['event', saved.id] })
      props.onClose()
    },
    onError: (e) => setError(e instanceof Error ? e.message : 'Błąd zapisu'),
  })

  const remove = useMutation({
    mutationFn: () => api.delete(`/api/events/${initial!.id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['calendar'] })
      props.onClose()
    },
    onError: (e) => setError(e instanceof Error ? e.message : 'Błąd usuwania'),
  })

  function submit() {
    if (!title.trim()) return setError('Podaj tytuł wydarzenia.')
    if (!categoryId) return setError('Wybierz kategorię.')
    if (scope === 'STUDENT' && !studentId) return setError('Wybierz ucznia.')
    setError('')
    save.mutate()
  }

  return (
    <div className="editpanel cal-editor">
      <h3>{initial ? 'Edycja wydarzenia' : 'Nowe wydarzenie'}</h3>

      <label htmlFor="ev-title">Tytuł</label>
      <input id="ev-title" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="np. Dzień Zielony" />

      <label htmlFor="ev-category">Kategoria</label>
      <select id="ev-category" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
        {props.categories.map((c) => (
          <option key={c.id} value={c.id}>{c.name}</option>
        ))}
      </select>

      <label htmlFor="ev-scope">Zasięg</label>
      <select id="ev-scope" value={scope} onChange={(e) => setScope(e.target.value as EventScope)}>
        <option value="CLASS_GROUP">Grupa ({classGroup.name})</option>
        <option value="STUDENT">Konkretny uczeń</option>
        <option value="PRESCHOOL">Całe przedszkole</option>
        {user.role === 'ADMIN' && <option value="NATIONAL">Ogólnopolskie</option>}
      </select>

      {scope === 'STUDENT' && (
        <>
          <label htmlFor="ev-student">Uczeń</label>
          <select id="ev-student" value={studentId} onChange={(e) => setStudentId(e.target.value)}>
            <option value="">— wybierz —</option>
            {(studentsQuery.data ?? []).map((s) => (
              <option key={s.id} value={s.id}>
                {s.firstName} {s.lastName}
              </option>
            ))}
          </select>
        </>
      )}

      <div className="daterange">
        <div>
          <label htmlFor="ev-start">Od</label>
          <input id="ev-start" type="date" value={startsOn} onChange={(e) => setStartsOn(e.target.value)} />
        </div>
        <div>
          <label htmlFor="ev-end">Do</label>
          <input id="ev-end" type="date" value={endsOn} onChange={(e) => setEndsOn(e.target.value)} />
        </div>
      </div>

      <label className="checkline">
        <input type="checkbox" checked={yearly} onChange={(e) => setYearly(e.target.checked)} />
        Powtarzaj co rok (święto stałodatowe)
      </label>

      <label htmlFor="ev-desc">Notatki</label>
      <textarea
        id="ev-desc"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        placeholder="Scenariusz dnia, pomysły, rzeczy do zapamiętania…"
        rows={3}
      />

      <label>Checklista organizera</label>
      {tasks.map((t, i) => (
        <div key={i} className="taskedit">
          <input
            type="checkbox"
            checked={t.done}
            aria-label="Zrobione"
            onChange={(e) => setTasks((prev) => prev.map((x, j) => (j === i ? { ...x, done: e.target.checked } : x)))}
          />
          <input
            value={t.title}
            placeholder="np. kupić bibułę"
            onChange={(e) => setTasks((prev) => prev.map((x, j) => (j === i ? { ...x, title: e.target.value } : x)))}
          />
          <button
            className="iconbtn"
            aria-label="Usuń zadanie"
            onClick={() => setTasks((prev) => prev.filter((_, j) => j !== i))}
          >
            ✕
          </button>
        </div>
      ))}
      <button className="minibtn" onClick={() => setTasks((prev) => [...prev, { title: '', done: false }])}>
        + dodaj zadanie
      </button>

      {error && <p className="form-error">{error}</p>}

      <div className="editactions">
        {initial && (
          <button
            className="minibtn danger"
            disabled={remove.isPending}
            onClick={() => {
              if (window.confirm('Usunąć to wydarzenie?')) remove.mutate()
            }}
          >
            Usuń
          </button>
        )}
        <button className="btn-ghost" onClick={props.onClose}>Anuluj</button>
        <button className="btn-primary" disabled={save.isPending} onClick={submit}>
          {save.isPending ? 'Zapisywanie…' : 'Zapisz'}
        </button>
      </div>
    </div>
  )
}
