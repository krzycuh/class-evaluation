import { useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'
import { useApp } from '../../AppContext'
import type { CalendarEventDetails, CalendarItem, EventCategory, EventScope } from '../../types'
import { EventEditor } from './EventEditor'
import {
  fmtFull, fmtRange, iso, KIND_EMOJI, MONTHS, PROJECT_COLOR, SCOPE_LABEL, WEEKDAYS,
} from './calendarUtils'

type FilterKey = EventScope | 'PROJECTS'

const FILTERS: { key: FilterKey; label: string }[] = [
  { key: 'NATIONAL', label: '🇵🇱 Ogólnopolskie' },
  { key: 'PRESCHOOL', label: '🏫 Przedszkole' },
  { key: 'CLASS_GROUP', label: '👥 Grupa' },
  { key: 'STUDENT', label: '👧 Uczeń' },
  { key: 'PROJECTS', label: '🚌 Projekty' },
]

function filterKey(item: CalendarItem): FilterKey {
  return item.type === 'EVENT' ? item.scope! : 'PROJECTS'
}

function itemColor(item: CalendarItem): string {
  return item.type === 'EVENT' ? (item.categoryColor ?? PROJECT_COLOR) : PROJECT_COLOR
}

export function CalendarPage() {
  const { classGroup } = useApp()
  const today = new Date()
  const [cursor, setCursor] = useState({ year: today.getFullYear(), month: today.getMonth() })
  const [selectedDay, setSelectedDay] = useState(iso(today))
  const [hidden, setHidden] = useState<Set<FilterKey>>(new Set())
  const [editor, setEditor] = useState<{ open: boolean; event: CalendarEventDetails | null }>({ open: false, event: null })

  const monthStart = new Date(cursor.year, cursor.month, 1)
  const monthEnd = new Date(cursor.year, cursor.month + 1, 0)
  const from = iso(monthStart)
  const to = iso(monthEnd)

  const feedQuery = useQuery({
    queryKey: ['calendar', classGroup.id, from, to],
    queryFn: () => api.get<CalendarItem[]>(`/api/calendar?classGroupId=${classGroup.id}&from=${from}&to=${to}`),
  })
  const categoriesQuery = useQuery({
    queryKey: ['event-categories'],
    queryFn: () => api.get<EventCategory[]>('/api/event-categories'),
  })

  const items = useMemo(
    () => (feedQuery.data ?? []).filter((it) => !hidden.has(filterKey(it))),
    [feedQuery.data, hidden],
  )

  // mapa dzień → wpisy (wpis wielodniowy trafia do każdego dnia zakresu)
  const byDay = useMemo(() => {
    const map = new Map<string, CalendarItem[]>()
    for (const item of items) {
      const start = item.startsOn < from ? from : item.startsOn
      const end = item.endsOn > to ? to : item.endsOn
      const d = new Date(start + 'T00:00:00')
      while (iso(d) <= end) {
        const key = iso(d)
        map.set(key, [...(map.get(key) ?? []), item])
        d.setDate(d.getDate() + 1)
      }
    }
    return map
  }, [items, from, to])

  const multiDay = useMemo(
    () => items.filter((it) => it.endsOn > it.startsOn && it.type !== 'PROJECT_TASK'),
    [items],
  )
  const dayItems = byDay.get(selectedDay) ?? []

  function shiftMonth(delta: number) {
    const next = new Date(cursor.year, cursor.month + delta, 1)
    setCursor({ year: next.getFullYear(), month: next.getMonth() })
    setSelectedDay(iso(next))
  }

  // Gest przesunięcia (swipe) w poziomie zmienia miesiąc. Próg 60 px i wymóg
  // wyraźnej przewagi ruchu poziomego nad pionowym nie kolidują z przewijaniem.
  const touchStart = useRef<{ x: number; y: number } | null>(null)

  function onTouchStart(e: React.TouchEvent) {
    touchStart.current = e.touches.length === 1
      ? { x: e.touches[0].clientX, y: e.touches[0].clientY }
      : null
  }

  function onTouchEnd(e: React.TouchEvent) {
    const start = touchStart.current
    touchStart.current = null
    if (!start) return
    const dx = e.changedTouches[0].clientX - start.x
    const dy = e.changedTouches[0].clientY - start.y
    if (Math.abs(dx) > 60 && Math.abs(dx) > 2 * Math.abs(dy)) {
      shiftMonth(dx < 0 ? 1 : -1)
    }
  }

  function toggleFilter(key: FilterKey) {
    setHidden((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  // siatka: puste komórki do poniedziałku + dni miesiąca
  const leading = (monthStart.getDay() + 6) % 7
  const cells: (string | null)[] = [
    ...Array.from({ length: leading }, () => null),
    ...Array.from({ length: monthEnd.getDate() }, (_, i) => iso(new Date(cursor.year, cursor.month, i + 1))),
  ]

  return (
    <div className="calendar-page">
      <div className="cal-toolbar">
        <button className="iconbtn" onClick={() => shiftMonth(-1)} aria-label="Poprzedni miesiąc">‹</button>
        <h1>
          {MONTHS[cursor.month]} {cursor.year}
        </h1>
        <button className="iconbtn" onClick={() => shiftMonth(1)} aria-label="Następny miesiąc">›</button>
        <Link className="minibtn" to="/projects">Projekty</Link>
        <button className="minibtn add" onClick={() => setEditor({ open: true, event: null })}>
          + Wydarzenie
        </button>
      </div>

      <div className="filterrow cal-filters">
        {FILTERS.map((f) => (
          <button
            key={f.key}
            className={hidden.has(f.key) ? '' : 'active'}
            aria-pressed={!hidden.has(f.key)}
            onClick={() => toggleFilter(f.key)}
          >
            {f.label}
          </button>
        ))}
      </div>

      <div
        className="cal-grid"
        role="grid"
        aria-label="Kalendarz miesiąca"
        onTouchStart={onTouchStart}
        onTouchEnd={onTouchEnd}
      >
        {WEEKDAYS.map((d) => (
          <span key={d} className="cal-wd">{d}</span>
        ))}
        {cells.map((day, i) =>
          day === null ? (
            <span key={`x${i}`} className="cal-cell empty" />
          ) : (
            <button
              key={day}
              className={`cal-cell${day === selectedDay ? ' sel' : ''}${day === iso(today) ? ' today' : ''}`}
              onClick={() => setSelectedDay(day)}
            >
              <span className="num">{Number(day.slice(8))}</span>
              <span className="dots">
                {(byDay.get(day) ?? []).slice(0, 3).map((it, j) => (
                  <span key={j} style={{ background: itemColor(it) }} />
                ))}
              </span>
            </button>
          ),
        )}
      </div>

      {feedQuery.isLoading && <div className="page-loading">Wczytywanie…</div>}

      {multiDay.length > 0 && (
        <section className="cal-section">
          <h2>W tym miesiącu</h2>
          {multiDay.map((it) => (
            <CalendarBar key={`${it.type}${it.id}${it.startsOn}`} item={it} />
          ))}
        </section>
      )}

      <section className="cal-section">
        <h2>{fmtFull(selectedDay)}</h2>
        {dayItems.length === 0 && <p className="empty-state">Brak wpisów tego dnia.</p>}
        {dayItems.map((it) =>
          it.type === 'EVENT' ? (
            <EventAgendaRow
              key={`${it.id}${it.startsOn}`}
              item={it}
              onEdit={(details) => setEditor({ open: true, event: details })}
            />
          ) : (
            <CalendarBar key={`${it.type}${it.id}`} item={it} />
          ),
        )}
      </section>

      {editor.open && (
        <EventEditor
          categories={(categoriesQuery.data ?? []).filter((c) => c.active)}
          initial={editor.event}
          defaultDate={selectedDay}
          onClose={() => setEditor({ open: false, event: null })}
        />
      )}
    </div>
  )
}

/** Pasek projektu / zadania projektu / wpisu wielodniowego. */
function CalendarBar({ item }: { item: CalendarItem }) {
  const queryClient = useQueryClient()
  const toggleTask = useMutation({
    mutationFn: (done: boolean) => api.patch(`/api/projects/${item.projectId}/tasks/${item.id}`, { done }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['calendar'] }),
  })

  if (item.type === 'PROJECT_TASK') {
    return (
      <div className="cal-bar">
        <input
          type="checkbox"
          checked={item.done ?? false}
          aria-label={`Zadanie: ${item.title}`}
          onChange={(e) => toggleTask.mutate(e.target.checked)}
        />
        <Link to={`/projects/${item.projectId}`} className="t">{item.title}</Link>
        <span className="range">do {fmtRange(item.startsOn, item.endsOn)}</span>
      </div>
    )
  }

  if (item.type === 'PROJECT') {
    const pct = item.totalTasks ? Math.round((100 * (item.doneTasks ?? 0)) / item.totalTasks) : 0
    return (
      <Link to={`/projects/${item.id}`} className="cal-bar link">
        <span className="chip" style={{ background: PROJECT_COLOR }} />
        <span className="t">
          {KIND_EMOJI[item.kind ?? 'OTHER']} {item.title}
        </span>
        <span className="range">{fmtRange(item.startsOn, item.endsOn)}</span>
        {(item.totalTasks ?? 0) > 0 && (
          <span className="progress cal-progress">
            <span style={{ width: `${pct}%` }} />
          </span>
        )}
      </Link>
    )
  }

  return (
    <div className="cal-bar">
      <span className="chip" style={{ background: itemColor(item) }} />
      <span className="t">{item.title}</span>
      <span className="range">{fmtRange(item.startsOn, item.endsOn)}</span>
    </div>
  )
}

/** Wiersz agendy dnia dla wydarzenia — rozwijany do notatek i checklisty. */
function EventAgendaRow({ item, onEdit }: { item: CalendarItem; onEdit: (details: CalendarEventDetails) => void }) {
  const [open, setOpen] = useState(false)
  const queryClient = useQueryClient()

  const detailsQuery = useQuery({
    queryKey: ['event', item.id],
    queryFn: () => api.get<CalendarEventDetails>(`/api/events/${item.id}`),
    enabled: open,
  })
  const details = detailsQuery.data

  const toggleTask = useMutation({
    mutationFn: ({ taskId, done }: { taskId: string; done: boolean }) =>
      api.patch(`/api/events/${item.id}/tasks/${taskId}`, { done }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['event', item.id] }),
  })

  return (
    <div className="cal-event">
      <button className="cal-bar wide" aria-expanded={open} onClick={() => setOpen((v) => !v)}>
        <span className="chip" style={{ background: itemColor(item) }} />
        <span className="t">
          {item.title}
          {item.studentName ? ` — ${item.studentName}` : ''}
        </span>
        <span className="scopebadge">{SCOPE_LABEL[item.scope!]}</span>
        {item.endsOn !== item.startsOn && <span className="range">{fmtRange(item.startsOn, item.endsOn)}</span>}
      </button>
      {open && details && (
        <div className="cal-event-body">
          {details.description && <p className="desc-text">{details.description}</p>}
          {details.tasks.length > 0 && (
            <ul className="tasklist">
              {details.tasks.map((t) => (
                <li key={t.id}>
                  <label>
                    <input
                      type="checkbox"
                      checked={t.done}
                      disabled={!details.canEdit}
                      onChange={(e) => toggleTask.mutate({ taskId: t.id, done: e.target.checked })}
                    />
                    <span className={t.done ? 'done' : ''}>{t.title}</span>
                  </label>
                </li>
              ))}
            </ul>
          )}
          {details.yearlyRecurring && <p className="hint">Powtarza się co rok.</p>}
          {details.canEdit && (
            <button className="minibtn" onClick={() => onEdit(details)}>
              ✎ Edytuj
            </button>
          )}
        </div>
      )}
      {open && detailsQuery.isLoading && <div className="cal-event-body">Wczytywanie…</div>}
    </div>
  )
}
