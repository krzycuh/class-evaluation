import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../../api/client'
import { useApp } from '../../AppContext'
import type { AssessmentValue, StudentAssessmentView } from '../../types'

type SaveState = 'saved' | 'saving' | 'error'

export function AssessmentPage() {
  const { studentId } = useParams<{ studentId: string }>()
  const { period, classGroup } = useApp()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [saveState, setSaveState] = useState<SaveState>('saved')
  const [openAreas, setOpenAreas] = useState<Record<string, boolean>>({})

  const queryKey = ['assessment', studentId, period.id]
  const viewQuery = useQuery({
    queryKey,
    queryFn: () => api.get<StudentAssessmentView>(`/api/students/${studentId}/assessment?periodId=${period.id}`),
  })

  const view = viewQuery.data
  const readOnly = view?.periodStatus === 'CLOSED'

  // pierwszy obszar domyślnie rozwinięty
  useEffect(() => {
    if (view && Object.keys(openAreas).length === 0 && view.areas.length > 0) {
      setOpenAreas({ [view.areas[0].areaId]: true })
    }
  }, [view]) // eslint-disable-line react-hooks/exhaustive-deps

  const upsert = useMutation({
    mutationFn: ({ skillId, value, note }: { skillId: string; value: AssessmentValue | null; note: string | null }) =>
      api.put(`/api/students/${studentId}/assessments/${skillId}?periodId=${period.id}`, { value, note }),
    onMutate: async ({ skillId, value, note }) => {
      setSaveState('saving')
      await queryClient.cancelQueries({ queryKey })
      queryClient.setQueryData<StudentAssessmentView>(queryKey, (old) =>
        old
          ? {
              ...old,
              areas: old.areas.map((a) => ({
                ...a,
                skills: a.skills.map((s) =>
                  s.skillId === skillId ? { ...s, value: value ?? undefined, note: note ?? undefined } : s,
                ),
              })),
            }
          : old,
      )
    },
    onSuccess: () => setSaveState('saved'),
    onError: () => {
      setSaveState('error')
      queryClient.invalidateQueries({ queryKey })
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['students', classGroup.id, period.id] })
    },
  })

  const noteTimer = useRef<number>()
  const generalNote = useMutation({
    mutationFn: (content: string) => api.put(`/api/students/${studentId}/period-note?periodId=${period.id}`, { content }),
    onSuccess: () => setSaveState('saved'),
    onError: () => setSaveState('error'),
  })

  const counts = useMemo(() => {
    if (!view) return { assessed: 0, total: 0 }
    const skills = view.areas.flatMap((a) => a.skills)
    return { assessed: skills.filter((s) => s.value).length, total: skills.length }
  }, [view])

  if (viewQuery.isLoading) return <div className="page-loading">Wczytywanie…</div>
  if (!view) return <div className="page-loading">Nie znaleziono ucznia.</div>

  function toggleValue(skillId: string, current: AssessmentValue | undefined, next: AssessmentValue, note?: string) {
    if (readOnly) return
    const value = current === next ? null : next
    upsert.mutate({ skillId, value, note: note ?? null })
  }

  return (
    <div className="assessment-page">
      <div className="assess-header">
        <button className="backbtn" onClick={() => navigate('/students')} aria-label="Wróć do listy">
          ←
        </button>
        <div className="assess-student">
          <strong>
            {view.firstName} {view.lastName}
          </strong>
          <span>
            {view.ageGroupName} · {view.periodName}
          </span>
        </div>
        <span className={`savebadge ${saveState}`}>
          {counts.assessed}/{counts.total} ·{' '}
          {saveState === 'saving' ? 'Zapisywanie…' : saveState === 'error' ? 'Błąd zapisu!' : 'Zapisano ✓'}
        </span>
      </div>

      {readOnly && <p className="closed-banner">Semestr zamknięty — oceny tylko do odczytu.</p>}

      {view.areas.map((area) => {
        const assessed = area.skills.filter((s) => s.value).length
        const open = !!openAreas[area.areaId]
        return (
          <section key={area.areaId} className={`area${open ? ' open' : ''}`}>
            <button
              className="area-head"
              aria-expanded={open}
              onClick={() => setOpenAreas((prev) => ({ ...prev, [area.areaId]: !prev[area.areaId] }))}
            >
              <span className="arrow">▸</span> {area.areaName}
              <span className="score">
                {assessed}/{area.skills.length}
              </span>
            </button>
            {open && (
              <div className="area-items">
                {area.skills.map((skill) => (
                  <SkillRow
                    key={skill.skillId}
                    title={skill.title}
                    description={skill.description}
                    recommendation={skill.parentRecommendation}
                    value={skill.value}
                    note={skill.note}
                    disabled={readOnly}
                    onValue={(next) => toggleValue(skill.skillId, skill.value, next, skill.note)}
                    onNote={(note) =>
                      upsert.mutate({ skillId: skill.skillId, value: skill.value ?? null, note: note || null })
                    }
                  />
                ))}
              </div>
            )}
          </section>
        )
      })}

      <div className="generalnote">
        <label htmlFor="general-note">Notatka ogólna o dziecku</label>
        <textarea
          id="general-note"
          defaultValue={view.generalNote}
          disabled={readOnly}
          placeholder="Obserwacje, mocne strony, na co zwrócić uwagę…"
          onChange={(e) => {
            setSaveState('saving')
            window.clearTimeout(noteTimer.current)
            const content = e.target.value
            noteTimer.current = window.setTimeout(() => generalNote.mutate(content), 600)
          }}
        />
      </div>

      <div className="assess-footer">
        <Link className="btn-ghost" to="/reports">
          Przejdź do raportów
        </Link>
      </div>
    </div>
  )
}

function SkillRow(props: {
  title: string
  description?: string
  recommendation?: string
  value?: AssessmentValue
  note?: string
  disabled: boolean
  onValue: (value: AssessmentValue) => void
  onNote: (note: string) => void
}) {
  const [showDesc, setShowDesc] = useState(false)
  const [showNote, setShowNote] = useState(!!props.note)
  const noteTimer = useRef<number>()

  return (
    <div className="skill">
      <div className="row1">
        <span className="stitle">{props.title}</span>
        {(props.description || props.recommendation) && (
          <button
            className="iconbtn"
            aria-label="Opis umiejętności"
            aria-expanded={showDesc}
            onClick={() => setShowDesc((v) => !v)}
          >
            ⓘ
          </button>
        )}
        <button className="iconbtn" aria-label="Notatka" aria-expanded={showNote} onClick={() => setShowNote((v) => !v)}>
          ✎
        </button>
      </div>
      {showDesc && (
        <div className="desc">
          {props.description && (
            <p>
              <strong>Jak sprawdzić:</strong> {props.description}
            </p>
          )}
          {props.recommendation && (
            <p>
              <strong>Zalecenie dla rodziców:</strong> {props.recommendation}
            </p>
          )}
        </div>
      )}
      <div className="togglerow">
        <button
          className={`tg yes${props.value === 'MASTERED' ? ' sel' : ''}`}
          disabled={props.disabled}
          onClick={() => props.onValue('MASTERED')}
        >
          ✓ Potrafi
        </button>
        <button
          className={`tg no${props.value === 'NOT_YET' ? ' sel' : ''}`}
          disabled={props.disabled}
          onClick={() => props.onValue('NOT_YET')}
        >
          ○ Jeszcze nie
        </button>
      </div>
      {showNote && (
        <textarea
          className="skillnote"
          placeholder="Notatka do tej umiejętności…"
          defaultValue={props.note}
          disabled={props.disabled}
          onChange={(e) => {
            window.clearTimeout(noteTimer.current)
            const note = e.target.value
            noteTimer.current = window.setTimeout(() => props.onNote(note), 600)
          }}
        />
      )}
    </div>
  )
}
