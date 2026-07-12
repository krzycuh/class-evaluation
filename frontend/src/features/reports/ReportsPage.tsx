import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import { useApp } from '../../AppContext'
import type { ClassReportRow, ReportDto } from '../../types'

export function ReportsPage() {
  const { classGroup, period } = useApp()
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const rowsQuery = useQuery({
    queryKey: ['reports', classGroup.id, period.id],
    queryFn: () => api.get<ClassReportRow[]>(`/api/class-groups/${classGroup.id}/reports?periodId=${period.id}`),
  })

  const generate = useMutation({
    mutationFn: (studentId: string) => api.post<ReportDto>(`/api/students/${studentId}/reports?periodId=${period.id}`),
    onSuccess: (report) => {
      queryClient.invalidateQueries({ queryKey: ['reports', classGroup.id, period.id] })
      navigate(`/reports/${report.id}`)
    },
  })

  const generateAll = useMutation({
    mutationFn: async (rows: ClassReportRow[]) => {
      for (const row of rows) {
        await api.post(`/api/students/${row.studentId}/reports?periodId=${period.id}`)
      }
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['reports', classGroup.id, period.id] }),
  })

  if (rowsQuery.isLoading) return <div className="page-loading">Wczytywanie…</div>

  const rows = rowsQuery.data ?? []
  const ready = rows.filter((r) => r.totalSkills > 0 && r.assessedCount >= r.totalSkills && !r.reportId)

  function generateWithConfirm(row: ClassReportRow) {
    const missing = row.totalSkills - row.assessedCount
    if (missing > 0) {
      const ok = window.confirm(
        `${row.firstName} ${row.lastName}: ${missing} umiejętności bez oceny — pominąć je w raporcie?`,
      )
      if (!ok) return
    }
    if (row.reportId) {
      const ok = window.confirm('Raport już istnieje. Wygenerować ponownie (nadpisze poprzedni)?')
      if (!ok) return
    }
    generate.mutate(row.studentId)
  }

  return (
    <div className="reports-page">
      <div className="page-head">
        <h1>Raporty</h1>
        <p className="hint">
          {period.name} {period.schoolYear}
        </p>
      </div>

      {rows.length === 0 && <p className="empty-state">Brak uczniów w grupie.</p>}

      <div className="report-list">
        {rows.map((row) => {
          const pct = row.totalSkills === 0 ? 0 : Math.round((100 * row.assessedCount) / row.totalSkills)
          const complete = row.totalSkills > 0 && row.assessedCount >= row.totalSkills
          return (
            <div key={row.studentId} className="reportrow">
              <span className="n">
                {row.firstName} {row.lastName}
              </span>
              {row.reportId ? (
                <span className="status-pill gen">wygenerowany</span>
              ) : complete ? (
                <span className="status-pill ready">gotowe 100%</span>
              ) : (
                <span className="status-pill missing">braki · {pct}%</span>
              )}
              {row.reportId && (
                <Link className="minibtn" to={`/reports/${row.reportId}`}>
                  Podgląd
                </Link>
              )}
              {!complete && !row.reportId && (
                <Link className="minibtn" to={`/students/${row.studentId}/assess`}>
                  Uzupełnij
                </Link>
              )}
              <button className="minibtn" disabled={generate.isPending} onClick={() => generateWithConfirm(row)}>
                {row.reportId ? 'Generuj ponownie' : 'Generuj'}
              </button>
            </div>
          )
        })}
      </div>

      {ready.length > 0 && (
        <div className="assess-footer">
          <button className="btn-ghost" disabled={generateAll.isPending} onClick={() => generateAll.mutate(ready)}>
            {generateAll.isPending ? 'Generowanie…' : `Generuj wszystkie gotowe (${ready.length})`}
          </button>
        </div>
      )}
    </div>
  )
}
