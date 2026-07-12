package pl.kma.classevaluation.reports

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kma.classevaluation.assessments.AssessmentService
import pl.kma.classevaluation.assessments.AssessmentValue
import pl.kma.classevaluation.auth.CurrentUser
import pl.kma.classevaluation.common.NotFoundException
import pl.kma.classevaluation.students.StudentService
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

/** Zamrożona treść raportu — snapshot ocen i zaleceń z chwili generowania. */
data class ReportContent(
    val studentName: String,
    val ageGroupName: String,
    val periodName: String,
    val teacherName: String,
    val mastered: List<MasteredArea>,
    val workingOn: List<WorkingOnItem>,
    val generalNote: String,
    val missingCount: Int,
) {
    data class MasteredArea(val areaName: String, val skills: List<String>)
    data class WorkingOnItem(
        val areaName: String,
        val title: String,
        val recommendation: String?,
        val note: String?,
    )
}

data class ReportDto(
    val id: UUID,
    val studentId: UUID,
    val periodId: UUID,
    val generatedAt: Instant,
    val content: ReportContent,
)

data class ClassReportRow(
    val studentId: UUID,
    val firstName: String,
    val lastName: String,
    val assessedCount: Int,
    val totalSkills: Int,
    val reportId: UUID?,
    val generatedAt: Instant?,
)

@Service
class ReportService(
    private val assessmentService: AssessmentService,
    private val studentService: StudentService,
    private val currentUser: CurrentUser,
    private val jdbc: JdbcClient,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun generate(studentId: UUID, periodId: UUID): ReportDto {
        val view = assessmentService.getStudentAssessment(studentId, periodId)
        val user = currentUser.get()

        val content = ReportContent(
            studentName = "${view.firstName} ${view.lastName}",
            ageGroupName = view.ageGroupName,
            periodName = view.periodName,
            teacherName = user.displayName,
            mastered = view.areas
                .map { area ->
                    ReportContent.MasteredArea(
                        areaName = area.areaName,
                        skills = area.skills.filter { it.value == AssessmentValue.MASTERED }.map { it.title },
                    )
                }
                .filter { it.skills.isNotEmpty() },
            workingOn = view.areas.flatMap { area ->
                area.skills
                    .filter { it.value == AssessmentValue.NOT_YET || it.value == AssessmentValue.IN_PROGRESS }
                    .map {
                        ReportContent.WorkingOnItem(
                            areaName = area.areaName,
                            title = it.title,
                            recommendation = it.parentRecommendation,
                            note = it.note,
                        )
                    }
            },
            generalNote = view.generalNote,
            missingCount = view.totalSkills - view.assessedCount,
        )

        val id = jdbc.sql(
            """
            INSERT INTO reports (id, student_id, period_id, generated_at, generated_by, content)
            VALUES (gen_random_uuid(), :studentId, :periodId, now(), :userId, :content::jsonb)
            ON CONFLICT (student_id, period_id)
            DO UPDATE SET generated_at = now(), generated_by = EXCLUDED.generated_by, content = EXCLUDED.content
            RETURNING id
            """,
        )
            .param("studentId", studentId)
            .param("periodId", periodId)
            .param("userId", user.id)
            .param("content", objectMapper.writeValueAsString(content))
            .query(UUID::class.java)
            .single()

        return get(id)
    }

    fun get(reportId: UUID): ReportDto {
        data class Row(val id: UUID, val studentId: UUID, val periodId: UUID, val generatedAt: Instant, val json: String)

        val row = jdbc.sql("SELECT id, student_id, period_id, generated_at, content FROM reports WHERE id = :id")
            .param("id", reportId)
            .query { rs, _ ->
                Row(
                    id = rs.getObject("id", UUID::class.java),
                    studentId = rs.getObject("student_id", UUID::class.java),
                    periodId = rs.getObject("period_id", UUID::class.java),
                    generatedAt = rs.getObject("generated_at", OffsetDateTime::class.java).toInstant(),
                    json = rs.getString("content"),
                )
            }
            .optional()
            .orElseThrow { NotFoundException("Nie znaleziono raportu") }

        // kontrola dostępu przez ucznia, którego dotyczy raport
        studentService.requireStudentAccess(row.studentId)

        return ReportDto(
            id = row.id,
            studentId = row.studentId,
            periodId = row.periodId,
            generatedAt = row.generatedAt,
            content = objectMapper.readValue(row.json),
        )
    }

    fun listForClassGroup(classGroupId: UUID, periodId: UUID): List<ClassReportRow> {
        return studentService.listStudentsWithProgress(classGroupId, periodId).map { s ->
            data class Meta(val id: UUID, val at: Instant)

            val meta = jdbc.sql(
                "SELECT id, generated_at FROM reports WHERE student_id = :studentId AND period_id = :periodId",
            )
                .param("studentId", s.id)
                .param("periodId", periodId)
                .query { rs, _ ->
                    Meta(
                        rs.getObject("id", UUID::class.java),
                        rs.getObject("generated_at", OffsetDateTime::class.java).toInstant(),
                    )
                }
                .optional()
                .orElse(null)

            ClassReportRow(
                studentId = s.id,
                firstName = s.firstName,
                lastName = s.lastName,
                assessedCount = s.assessedCount,
                totalSkills = s.totalSkills,
                reportId = meta?.id,
                generatedAt = meta?.at,
            )
        }
    }
}
