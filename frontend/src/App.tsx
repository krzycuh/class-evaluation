import { useQuery } from '@tanstack/react-query'
import { Navigate, Route, Routes } from 'react-router-dom'
import { api, ApiError } from './api/client'
import { AppProvider } from './AppContext'
import { Layout } from './layout/Layout'
import { LoginPage } from './features/auth/LoginPage'
import { StudentsPage } from './features/students/StudentsPage'
import { AssessmentPage } from './features/assessment/AssessmentPage'
import { SkillsPage } from './features/skills/SkillsPage'
import { ReportsPage } from './features/reports/ReportsPage'
import { ReportPreviewPage } from './features/reports/ReportPreviewPage'
import { SettingsPage } from './features/settings/SettingsPage'
import type { UserDto } from './types'

export default function App() {
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: () => api.get<UserDto>('/api/auth/me'),
    retry: (failureCount, error) =>
      !(error instanceof ApiError && error.status === 401) && failureCount < 2,
  })

  if (meQuery.isLoading) return <div className="page-loading">Wczytywanie…</div>

  if (!meQuery.data) return <LoginPage />

  return (
    <AppProvider user={meQuery.data}>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/students" replace />} />
          <Route path="/students" element={<StudentsPage />} />
          <Route path="/students/:studentId/assess" element={<AssessmentPage />} />
          <Route path="/skills" element={<SkillsPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
        <Route path="/reports/:reportId" element={<ReportPreviewPage />} />
        <Route path="*" element={<Navigate to="/students" replace />} />
      </Routes>
    </AppProvider>
  )
}
