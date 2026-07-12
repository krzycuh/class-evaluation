export type Role = 'TEACHER' | 'ADMIN'

export interface UserDto {
  id: string
  email: string
  displayName: string
  role: Role
}

export interface ClassGroup {
  id: string
  name: string
  schoolYear: string
  ownerUserId: string
}

export interface AgeGroup {
  id: string
  name: string
  minAgeYears: number
  sortOrder: number
}

export interface StudentWithProgress {
  id: string
  firstName: string
  lastName: string
  birthDate: string
  ageGroupId: string
  ageGroupName: string
  assessedCount: number
  totalSkills: number
}

export interface Student {
  id: string
  classGroupId: string
  firstName: string
  lastName: string
  birthDate: string
  ageGroupId: string
  active: boolean
}

export type PeriodStatus = 'OPEN' | 'CLOSED'

export interface AssessmentPeriod {
  id: string
  schoolYear: string
  name: string
  startsOn: string
  endsOn: string
  status: PeriodStatus
}

export type AssessmentValue = 'MASTERED' | 'NOT_YET' | 'IN_PROGRESS'

export interface AssessmentSkillView {
  skillId: string
  title: string
  description?: string
  parentRecommendation?: string
  value?: AssessmentValue
  note?: string
}

export interface AssessmentAreaView {
  areaId: string
  areaName: string
  skills: AssessmentSkillView[]
}

export interface StudentAssessmentView {
  studentId: string
  firstName: string
  lastName: string
  ageGroupId: string
  ageGroupName: string
  periodId: string
  periodName: string
  periodStatus: PeriodStatus
  generalNote: string
  areas: AssessmentAreaView[]
  assessedCount: number
  totalSkills: number
}

export interface SkillDto {
  id: string
  areaId: string
  title: string
  description?: string
  parentRecommendation?: string
  sortOrder: number
  active: boolean
  ageGroupIds: string[]
}

export interface AreaWithSkills {
  id: string
  name: string
  description?: string
  sortOrder: number
  active: boolean
  skills: SkillDto[]
}

export interface ClassReportRow {
  studentId: string
  firstName: string
  lastName: string
  assessedCount: number
  totalSkills: number
  reportId?: string
  generatedAt?: string
}

export interface ReportContent {
  studentName: string
  ageGroupName: string
  periodName: string
  teacherName: string
  mastered: { areaName: string; skills: string[] }[]
  workingOn: { areaName: string; title: string; recommendation?: string; note?: string }[]
  generalNote: string
  missingCount: number
}

export interface ReportDto {
  id: string
  studentId: string
  periodId: string
  generatedAt: string
  content: ReportContent
}
