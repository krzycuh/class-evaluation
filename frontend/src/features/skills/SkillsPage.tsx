import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import { useApp } from '../../AppContext'
import type { AgeGroup, AreaWithSkills, SkillDto } from '../../types'

interface SkillForm {
  id?: string
  areaId: string
  title: string
  description: string
  parentRecommendation: string
  ageGroupIds: string[]
  active: boolean
}

export function SkillsPage() {
  const { user } = useApp()
  const queryClient = useQueryClient()
  const isAdmin = user.role === 'ADMIN'
  const [openAreas, setOpenAreas] = useState<Record<string, boolean>>({})
  const [form, setForm] = useState<SkillForm | null>(null)
  const [newAreaName, setNewAreaName] = useState('')
  const [error, setError] = useState<string>()

  const areasQuery = useQuery({
    queryKey: ['areas'],
    queryFn: () => api.get<AreaWithSkills[]>('/api/development-areas?includeInactive=true'),
  })
  const ageGroupsQuery = useQuery({
    queryKey: ['age-groups'],
    queryFn: () => api.get<AgeGroup[]>('/api/age-groups'),
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['areas'] })
    queryClient.invalidateQueries({ queryKey: ['assessment'] })
    queryClient.invalidateQueries({ queryKey: ['students'] })
  }

  const saveSkill = useMutation({
    mutationFn: (f: SkillForm) => {
      const body = {
        areaId: f.areaId,
        title: f.title,
        description: f.description || null,
        parentRecommendation: f.parentRecommendation || null,
        ageGroupIds: f.ageGroupIds,
        active: f.active,
      }
      return f.id ? api.patch(`/api/skills/${f.id}`, body) : api.post('/api/skills', body)
    },
    onSuccess: () => {
      setForm(null)
      setError(undefined)
      invalidate()
    },
    onError: (e) => setError(e.message),
  })

  const addArea = useMutation({
    mutationFn: (name: string) => api.post('/api/development-areas', { name }),
    onSuccess: () => {
      setNewAreaName('')
      invalidate()
    },
    onError: (e) => setError(e.message),
  })

  const ageGroups = ageGroupsQuery.data ?? []

  if (areasQuery.isLoading) return <div className="page-loading">Wczytywanie…</div>

  function editSkill(skill: SkillDto) {
    setForm({
      id: skill.id,
      areaId: skill.areaId,
      title: skill.title,
      description: skill.description ?? '',
      parentRecommendation: skill.parentRecommendation ?? '',
      ageGroupIds: skill.ageGroupIds,
      active: skill.active,
    })
  }

  function newSkill(areaId: string) {
    setForm({ areaId, title: '', description: '', parentRecommendation: '', ageGroupIds: [], active: true })
  }

  return (
    <div className="skills-page">
      <div className="page-head">
        <h1>Umiejętności</h1>
        {!isAdmin && <p className="hint">Podgląd — konfigurację zmienia administrator.</p>}
      </div>

      {error && <p className="form-error">{error}</p>}

      {(areasQuery.data ?? []).map((area) => {
        const open = openAreas[area.id] ?? true
        return (
          <section key={area.id} className={`area${open ? ' open' : ''}${area.active ? '' : ' inactive'}`}>
            <button
              className="area-head"
              aria-expanded={open}
              onClick={() => setOpenAreas((prev) => ({ ...prev, [area.id]: !open }))}
            >
              <span className="arrow">▸</span> {area.name}
              {!area.active && <span className="inactive-badge">nieaktywny</span>}
              <span className="score">{area.skills.length}</span>
            </button>
            {open && (
              <div className="area-items">
                {area.skills.map((skill) => (
                  <div key={skill.id} className={`cfg-skill${skill.active ? '' : ' inactive'}`}>
                    <span className="t">{skill.title}</span>
                    <span className="ages">
                      {skill.ageGroupIds
                        .map((id) => ageGroups.find((g) => g.id === id))
                        .filter(Boolean)
                        .sort((a, b) => a!.sortOrder - b!.sortOrder)
                        .map((g) => (
                          <span key={g!.id}>{g!.minAgeYears}l</span>
                        ))}
                    </span>
                    {isAdmin && (
                      <button className="iconbtn" aria-label={`Edytuj: ${skill.title}`} onClick={() => editSkill(skill)}>
                        ✏️
                      </button>
                    )}
                  </div>
                ))}
                {isAdmin && (
                  <div className="addrow">
                    <button onClick={() => newSkill(area.id)}>+ Dodaj umiejętność</button>
                  </div>
                )}
              </div>
            )}
          </section>
        )
      })}

      {isAdmin && (
        <div className="add-area-row">
          <input
            placeholder="Nazwa nowego obszaru…"
            value={newAreaName}
            onChange={(e) => setNewAreaName(e.target.value)}
          />
          <button
            className="btn-ghost"
            disabled={!newAreaName.trim() || addArea.isPending}
            onClick={() => addArea.mutate(newAreaName.trim())}
          >
            + Obszar
          </button>
        </div>
      )}

      {form && (
        <div className="editpanel" role="dialog" aria-label="Edycja umiejętności">
          <h3>{form.id ? 'Edycja umiejętności' : 'Nowa umiejętność'}</h3>
          <label htmlFor="sk-title">Tytuł</label>
          <input id="sk-title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          <label htmlFor="sk-desc">Opis (jak sprawdzić)</label>
          <textarea
            id="sk-desc"
            rows={2}
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
          <label htmlFor="sk-rec">Zalecenie dla rodziców</label>
          <textarea
            id="sk-rec"
            rows={2}
            value={form.parentRecommendation}
            onChange={(e) => setForm({ ...form, parentRecommendation: e.target.value })}
          />
          <label>Grupy wiekowe</label>
          <div className="agepick">
            {ageGroups.map((g) => {
              const selected = form.ageGroupIds.includes(g.id)
              return (
                <button
                  key={g.id}
                  className={selected ? 'sel' : ''}
                  aria-pressed={selected}
                  onClick={() =>
                    setForm({
                      ...form,
                      ageGroupIds: selected
                        ? form.ageGroupIds.filter((id) => id !== g.id)
                        : [...form.ageGroupIds, g.id],
                    })
                  }
                >
                  {g.name}
                </button>
              )
            })}
          </div>
          <label htmlFor="sk-area">Obszar</label>
          <select id="sk-area" value={form.areaId} onChange={(e) => setForm({ ...form, areaId: e.target.value })}>
            {(areasQuery.data ?? []).map((a) => (
              <option key={a.id} value={a.id}>
                {a.name}
              </option>
            ))}
          </select>
          <label className="checkline">
            <input
              type="checkbox"
              checked={form.active}
              onChange={(e) => setForm({ ...form, active: e.target.checked })}
            />
            Aktywna (nieaktywne nie pojawiają się przy ocenianiu)
          </label>
          <div className="editactions">
            <button
              className="btn-primary"
              disabled={!form.title.trim() || form.ageGroupIds.length === 0 || saveSkill.isPending}
              onClick={() => saveSkill.mutate(form)}
            >
              Zapisz
            </button>
            <button className="btn-ghost" onClick={() => setForm(null)}>
              Anuluj
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
