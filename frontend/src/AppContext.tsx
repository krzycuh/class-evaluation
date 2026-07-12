import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from './api/client'
import type { AssessmentPeriod, ClassGroup, UserDto } from './types'

/** Globalny kontekst: zalogowany użytkownik, wybrana grupa i semestr. */
interface AppContextValue {
  user: UserDto
  classGroup: ClassGroup
  classGroups: ClassGroup[]
  period: AssessmentPeriod
  periods: AssessmentPeriod[]
  setPeriodId: (id: string) => void
  setClassGroupId: (id: string) => void
}

const AppContext = createContext<AppContextValue | null>(null)

export function useApp(): AppContextValue {
  const value = useContext(AppContext)
  if (!value) throw new Error('useApp poza AppProvider')
  return value
}

export function AppProvider({ user, children }: { user: UserDto; children: ReactNode }) {
  const groupsQuery = useQuery({
    queryKey: ['class-groups'],
    queryFn: () => api.get<ClassGroup[]>('/api/class-groups'),
  })
  const periodsQuery = useQuery({
    queryKey: ['periods'],
    queryFn: () => api.get<AssessmentPeriod[]>('/api/periods'),
  })

  const [classGroupId, setClassGroupId] = useState<string>(() => localStorage.getItem('classGroupId') ?? '')
  const [periodId, setPeriodId] = useState<string>(() => localStorage.getItem('periodId') ?? '')

  useEffect(() => {
    if (classGroupId) localStorage.setItem('classGroupId', classGroupId)
  }, [classGroupId])
  useEffect(() => {
    if (periodId) localStorage.setItem('periodId', periodId)
  }, [periodId])

  const value = useMemo(() => {
    const classGroups = groupsQuery.data ?? []
    const periods = periodsQuery.data ?? []
    const classGroup = classGroups.find((g) => g.id === classGroupId) ?? classGroups[0]
    const period =
      periods.find((p) => p.id === periodId) ??
      periods.find((p) => p.status === 'OPEN') ??
      periods[0]
    if (!classGroup || !period) return null
    return { user, classGroup, classGroups, period, periods, setPeriodId, setClassGroupId }
  }, [user, groupsQuery.data, periodsQuery.data, classGroupId, periodId])

  if (groupsQuery.isLoading || periodsQuery.isLoading) {
    return <div className="page-loading">Wczytywanie…</div>
  }
  if (!value) {
    return (
      <div className="page-loading">
        Brak skonfigurowanej grupy lub semestru. Skontaktuj się z administratorem.
      </div>
    )
  }
  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}
