import type { EventScope, ProjectKind, ProjectStatus } from '../../types'

export const WEEKDAYS = ['Pn', 'Wt', 'Śr', 'Cz', 'Pt', 'So', 'Nd']

export const MONTHS = [
  'Styczeń', 'Luty', 'Marzec', 'Kwiecień', 'Maj', 'Czerwiec',
  'Lipiec', 'Sierpień', 'Wrzesień', 'Październik', 'Listopad', 'Grudzień',
]

export const SCOPE_LABEL: Record<EventScope, string> = {
  NATIONAL: 'Ogólnopolskie',
  PRESCHOOL: 'Przedszkole',
  CLASS_GROUP: 'Grupa',
  STUDENT: 'Uczeń',
}

export const KIND_EMOJI: Record<ProjectKind, string> = { TRIP: '🚌', CONTEST: '🏆', OTHER: '📌' }

export const KIND_LABEL: Record<ProjectKind, string> = { TRIP: 'Wycieczka', CONTEST: 'Konkurs', OTHER: 'Inny' }

export const STATUS_LABEL: Record<ProjectStatus, string> = {
  PLANNED: 'Planowany',
  IN_PROGRESS: 'W trakcie',
  DONE: 'Zakończony',
  CANCELLED: 'Odwołany',
}

/** Kolor projektów na kalendarzu (wydarzenia mają kolor kategorii). */
export const PROJECT_COLOR = '#de9a3c'

/** Data lokalna w formacie YYYY-MM-DD (bez przesunięć strefowych). */
export function iso(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

/** 'YYYY-MM-DD' → 'DD.MM' */
export function fmtShort(isoDate: string): string {
  const [, m, d] = isoDate.split('-')
  return `${d}.${m}`
}

/** 'YYYY-MM-DD' → 'DD.MM.YYYY' */
export function fmtFull(isoDate: string): string {
  const [y, m, d] = isoDate.split('-')
  return `${d}.${m}.${y}`
}

/** Zakres dat wpisu: '17.03' albo '02.03 – 06.03'. */
export function fmtRange(startsOn: string, endsOn: string): string {
  return startsOn === endsOn ? fmtShort(startsOn) : `${fmtShort(startsOn)} – ${fmtShort(endsOn)}`
}
