import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../api/client'
import type { ReportDto } from '../../types'

/** Podgląd raportu dla rodziców — widok print-friendly (drukuj / zapisz jako PDF). */
export function ReportPreviewPage() {
  const { reportId } = useParams<{ reportId: string }>()

  const reportQuery = useQuery({
    queryKey: ['report', reportId],
    queryFn: () => api.get<ReportDto>(`/api/reports/${reportId}`),
  })

  if (reportQuery.isLoading) return <div className="page-loading">Wczytywanie…</div>
  const report = reportQuery.data
  if (!report) return <div className="page-loading">Nie znaleziono raportu.</div>

  const c = report.content
  const generatedAt = new Date(report.generatedAt).toLocaleDateString('pl-PL')

  return (
    <div className="report-preview">
      <div className="report-toolbar no-print">
        <Link className="minibtn" to="/reports">
          ← Wróć
        </Link>
        <button className="btn-primary" onClick={() => window.print()}>
          🖨 Drukuj / zapisz PDF
        </button>
      </div>

      <article className="report-doc">
        <h1>Raport rozwoju dziecka</h1>
        <p className="rmeta">
          {c.studentName} · {c.ageGroupName} · {c.periodName}
        </p>

        {c.missingCount > 0 && (
          <p className="rmissing no-print">
            Uwaga: {c.missingCount} umiejętności nie zostało ocenionych i nie ma ich w raporcie.
          </p>
        )}

        <h2 className="good">Co dziecko już potrafi ✓</h2>
        {c.mastered.length === 0 && <p>—</p>}
        {c.mastered.map((area) => (
          <p key={area.areaName}>
            <strong>{area.areaName}:</strong> {area.skills.join(', ')}.
          </p>
        ))}

        <h2 className="work">Nad czym pracujemy</h2>
        {c.workingOn.length === 0 && <p>Wszystkie oceniane umiejętności opanowane — gratulacje!</p>}
        <ul>
          {c.workingOn.map((item) => (
            <li key={item.title}>
              {item.title} <span className="rarea">({item.areaName.toLowerCase()})</span>
              {item.recommendation && <div className="rec">Zalecenie: {item.recommendation}</div>}
              {item.note && <div className="rec">Obserwacja: {item.note}</div>}
            </li>
          ))}
        </ul>

        {c.generalNote && (
          <>
            <h2>Obserwacje nauczyciela</h2>
            <p>{c.generalNote}</p>
          </>
        )}

        <div className="sig">
          <span>Data: {generatedAt}</span>
          <span>Nauczyciel: {c.teacherName}</span>
        </div>
      </article>
    </div>
  )
}
