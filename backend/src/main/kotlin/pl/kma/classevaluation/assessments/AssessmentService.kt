package pl.kma.classevaluation.assessments

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kma.classevaluation.auth.CurrentUser
import pl.kma.classevaluation.common.BadRequestException
import pl.kma.classevaluation.common.ConflictException
import pl.kma.classevaluation.common.NotFoundException
import pl.kma.classevaluation.students.StudentService
import java.time.LocalDate
import java.util.UUID

data class AssessmentSkillView(
    val skillId: UUID,
    val title: String,
    val description: String?,
    val parentRecommendation: String?,
    val value: AssessmentValue?,
    val note: String?,
)

data class AssessmentAreaView(
    val areaId: UUID,
    val areaName: String,
    val skills: List<AssessmentSkillView>,
)

data class StudentAssessmentView(
    val studentId: UUID,
    val firstName: String,
    val lastName: String,
    val ageGroupId: UUID,
    val ageGroupName: String,
    val periodId: UUID,
    val periodName: String,
    val periodStatus: PeriodStatus,
    val generalNote: String,
    val areas: List<AssessmentAreaView>,
) {
    val assessedCount: Int get() = areas.sumOf { a -> a.skills.count { it.value != null } }
    val totalSkills: Int get() = areas.sumOf { it.skills.size }
}

@Service
class AssessmentService(
    private val periods: AssessmentPeriodRepository,
    private val studentService: StudentService,
    private val currentUser: CurrentUser,
    private val jdbc: JdbcClient,
) {

    fun requirePeriod(periodId: UUID): AssessmentPeriod =
        periods.findById(periodId).orElseThrow { NotFoundException("Nie znaleziono okresu oceny") }

    private fun requireOpenPeriod(periodId: UUID): AssessmentPeriod {
        val period = requirePeriod(periodId)
        if (period.status == PeriodStatus.CLOSED) {
            throw ConflictException("Okres '${period.name} ${period.schoolYear}' jest zamknięty — oceny tylko do odczytu")
        }
        return period
    }

    fun getStudentAssessment(studentId: UUID, periodId: UUID): StudentAssessmentView {
        val student = studentService.requireStudentAccess(studentId)
        val period = requirePeriod(periodId)

        data class Row(
            val areaId: UUID, val areaName: String, val areaOrder: Int,
            val skill: AssessmentSkillView, val skillOrder: Int,
        )

        val rows = jdbc.sql(
            """
            SELECT da.id AS area_id, da.name AS area_name, da.sort_order AS area_order,
                   sk.id AS skill_id, sk.title, sk.description, sk.parent_recommendation, sk.sort_order AS skill_order,
                   a.value, a.note
              FROM skills sk
              JOIN development_areas da ON da.id = sk.area_id
              JOIN skill_age_groups sag ON sag.skill_id = sk.id
              LEFT JOIN assessments a ON a.skill_id = sk.id AND a.student_id = :studentId AND a.period_id = :periodId
             WHERE sag.age_group_id = :ageGroupId AND sk.active AND da.active
             ORDER BY da.sort_order, da.name, sk.sort_order, sk.title
            """,
        )
            .param("studentId", studentId)
            .param("periodId", periodId)
            .param("ageGroupId", student.ageGroupId)
            .query { rs, _ ->
                Row(
                    areaId = rs.getObject("area_id", UUID::class.java),
                    areaName = rs.getString("area_name"),
                    areaOrder = rs.getInt("area_order"),
                    skillOrder = rs.getInt("skill_order"),
                    skill = AssessmentSkillView(
                        skillId = rs.getObject("skill_id", UUID::class.java),
                        title = rs.getString("title"),
                        description = rs.getString("description"),
                        parentRecommendation = rs.getString("parent_recommendation"),
                        value = rs.getString("value")?.let { AssessmentValue.valueOf(it) },
                        note = rs.getString("note"),
                    ),
                )
            }
            .list()

        val generalNote = jdbc.sql(
            "SELECT content FROM student_period_notes WHERE student_id = :studentId AND period_id = :periodId",
        )
            .param("studentId", studentId)
            .param("periodId", periodId)
            .query(String::class.java)
            .optional()
            .orElse("")

        val ageGroupName = jdbc.sql("SELECT name FROM age_groups WHERE id = :id")
            .param("id", student.ageGroupId)
            .query(String::class.java)
            .single()

        return StudentAssessmentView(
            studentId = student.id!!,
            firstName = student.firstName,
            lastName = student.lastName,
            ageGroupId = student.ageGroupId,
            ageGroupName = ageGroupName,
            periodId = period.id!!,
            periodName = "${period.name} ${period.schoolYear}",
            periodStatus = period.status,
            generalNote = generalNote,
            areas = rows
                .groupBy { Triple(it.areaId, it.areaName, it.areaOrder) }
                .map { (key, groupRows) ->
                    AssessmentAreaView(
                        areaId = key.first,
                        areaName = key.second,
                        skills = groupRows.sortedBy { it.skillOrder }.map { it.skill },
                    )
                },
        )
    }

    @Transactional
    fun upsertAssessment(studentId: UUID, skillId: UUID, periodId: UUID, value: AssessmentValue?, note: String?) {
        val student = studentService.requireStudentAccess(studentId)
        requireOpenPeriod(periodId)

        val applies = jdbc.sql(
            """
            SELECT count(*) FROM skills sk
              JOIN skill_age_groups sag ON sag.skill_id = sk.id
             WHERE sk.id = :skillId AND sk.active AND sag.age_group_id = :ageGroupId
            """,
        )
            .param("skillId", skillId)
            .param("ageGroupId", student.ageGroupId)
            .query(Int::class.java)
            .single()
        if (applies == 0) throw BadRequestException("Umiejętność nie dotyczy grupy wiekowej tego ucznia")

        val normalizedNote = note?.trim()?.ifBlank { null }
        if (value == null && normalizedNote == null) {
            jdbc.sql("DELETE FROM assessments WHERE student_id = :studentId AND skill_id = :skillId AND period_id = :periodId")
                .param("studentId", studentId)
                .param("skillId", skillId)
                .param("periodId", periodId)
                .update()
            return
        }

        jdbc.sql(
            """
            INSERT INTO assessments (id, student_id, skill_id, period_id, value, note, updated_by, updated_at)
            VALUES (gen_random_uuid(), :studentId, :skillId, :periodId, :value, :note, :userId, now())
            ON CONFLICT (student_id, skill_id, period_id)
            DO UPDATE SET value = EXCLUDED.value, note = EXCLUDED.note,
                          updated_by = EXCLUDED.updated_by, updated_at = now()
            """,
        )
            .param("studentId", studentId)
            .param("skillId", skillId)
            .param("periodId", periodId)
            .param("value", value?.name)
            .param("note", normalizedNote)
            .param("userId", currentUser.get().id)
            .update()
    }

    @Transactional
    fun upsertGeneralNote(studentId: UUID, periodId: UUID, content: String) {
        studentService.requireStudentAccess(studentId)
        requireOpenPeriod(periodId)
        jdbc.sql(
            """
            INSERT INTO student_period_notes (id, student_id, period_id, content, updated_at)
            VALUES (gen_random_uuid(), :studentId, :periodId, :content, now())
            ON CONFLICT (student_id, period_id)
            DO UPDATE SET content = EXCLUDED.content, updated_at = now()
            """,
        )
            .param("studentId", studentId)
            .param("periodId", periodId)
            .param("content", content)
            .update()
    }

    fun listPeriods(): List<AssessmentPeriod> = periods.findAllOrdered()

    @Transactional
    fun createPeriod(schoolYear: String, name: String, startsOn: LocalDate, endsOn: LocalDate): AssessmentPeriod {
        if (!endsOn.isAfter(startsOn)) throw BadRequestException("Data końca musi być po dacie początku")
        return periods.save(AssessmentPeriod(schoolYear = schoolYear.trim(), name = name.trim(), startsOn = startsOn, endsOn = endsOn))
    }

    @Transactional
    fun updatePeriodStatus(periodId: UUID, status: PeriodStatus): AssessmentPeriod {
        val period = requirePeriod(periodId)
        return periods.save(period.copy(status = status))
    }
}
