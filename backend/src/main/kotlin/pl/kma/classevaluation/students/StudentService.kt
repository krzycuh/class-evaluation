package pl.kma.classevaluation.students

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kma.classevaluation.auth.CurrentUser
import pl.kma.classevaluation.auth.Role
import pl.kma.classevaluation.common.BadRequestException
import pl.kma.classevaluation.common.NotFoundException
import java.time.LocalDate
import java.util.UUID

data class StudentWithProgress(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val ageGroupId: UUID,
    val ageGroupName: String,
    val assessedCount: Int,
    val totalSkills: Int,
)

@Service
class StudentService(
    private val students: StudentRepository,
    private val classGroups: ClassGroupRepository,
    private val ageGroups: AgeGroupRepository,
    private val currentUser: CurrentUser,
    private val jdbc: JdbcClient,
) {

    fun listClassGroups(): List<ClassGroup> {
        val user = currentUser.get()
        return if (user.role == Role.ADMIN) classGroups.findAllOrdered()
        else classGroups.findByOwnerUserId(user.id!!)
    }

    fun requireClassGroupAccess(classGroupId: UUID): ClassGroup {
        val group = classGroups.findById(classGroupId)
            .orElseThrow { NotFoundException("Nie znaleziono grupy") }
        val user = currentUser.get()
        if (user.role != Role.ADMIN && group.ownerUserId != user.id) {
            throw AccessDeniedException("Brak dostępu do grupy")
        }
        return group
    }

    fun requireStudentAccess(studentId: UUID): Student {
        val student = students.findById(studentId)
            .orElseThrow { NotFoundException("Nie znaleziono ucznia") }
        requireClassGroupAccess(student.classGroupId)
        return student
    }

    fun listStudentsWithProgress(classGroupId: UUID, periodId: UUID): List<StudentWithProgress> {
        requireClassGroupAccess(classGroupId)
        return jdbc.sql(
            """
            SELECT st.id, st.first_name, st.last_name, st.birth_date, st.age_group_id,
                   ag.name AS age_group_name,
                   (SELECT count(*) FROM skills sk
                      JOIN skill_age_groups sag ON sag.skill_id = sk.id
                      JOIN development_areas da ON da.id = sk.area_id
                     WHERE sag.age_group_id = st.age_group_id AND sk.active AND da.active) AS total_skills,
                   (SELECT count(*) FROM assessments a
                      JOIN skills sk2 ON sk2.id = a.skill_id
                      JOIN development_areas da2 ON da2.id = sk2.area_id
                     WHERE a.student_id = st.id AND a.period_id = :periodId
                       AND a.value IS NOT NULL AND sk2.active AND da2.active
                       AND EXISTS (SELECT 1 FROM skill_age_groups sag2
                                    WHERE sag2.skill_id = sk2.id AND sag2.age_group_id = st.age_group_id)) AS assessed_count
              FROM students st
              JOIN age_groups ag ON ag.id = st.age_group_id
             WHERE st.class_group_id = :classGroupId AND st.active
             ORDER BY st.last_name, st.first_name
            """,
        )
            .param("classGroupId", classGroupId)
            .param("periodId", periodId)
            .query { rs, _ ->
                StudentWithProgress(
                    id = rs.getObject("id", UUID::class.java),
                    firstName = rs.getString("first_name"),
                    lastName = rs.getString("last_name"),
                    birthDate = rs.getObject("birth_date", LocalDate::class.java),
                    ageGroupId = rs.getObject("age_group_id", UUID::class.java),
                    ageGroupName = rs.getString("age_group_name"),
                    assessedCount = rs.getInt("assessed_count"),
                    totalSkills = rs.getInt("total_skills"),
                )
            }
            .list()
    }

    @Transactional
    fun createStudent(classGroupId: UUID, firstName: String, lastName: String, birthDate: LocalDate, ageGroupId: UUID?): Student {
        val group = requireClassGroupAccess(classGroupId)
        val resolvedAgeGroup = ageGroupId ?: suggestAgeGroup(birthDate, group.schoolYear)
        if (ageGroups.findById(resolvedAgeGroup).isEmpty) throw BadRequestException("Nieznana grupa wiekowa")
        return students.save(
            Student(
                classGroupId = classGroupId,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                birthDate = birthDate,
                ageGroupId = resolvedAgeGroup,
            ),
        )
    }

    @Transactional
    fun updateStudent(studentId: UUID, firstName: String?, lastName: String?, birthDate: LocalDate?, ageGroupId: UUID?, active: Boolean?): Student {
        val student = requireStudentAccess(studentId)
        if (ageGroupId != null && ageGroups.findById(ageGroupId).isEmpty) throw BadRequestException("Nieznana grupa wiekowa")
        return students.save(
            student.copy(
                firstName = firstName?.trim() ?: student.firstName,
                lastName = lastName?.trim() ?: student.lastName,
                birthDate = birthDate ?: student.birthDate,
                ageGroupId = ageGroupId ?: student.ageGroupId,
                active = active ?: student.active,
            ),
        )
    }

    @Transactional
    fun deactivateStudent(studentId: UUID) {
        val student = requireStudentAccess(studentId)
        students.save(student.copy(active = false))
    }

    /** Wiek na 1 września danego roku szkolnego → grupa wiekowa o najbliższym min_age_years. */
    fun suggestAgeGroup(birthDate: LocalDate, schoolYear: String): UUID {
        val startYear = schoolYear.substringBefore('/').toIntOrNull()
            ?: throw BadRequestException("Błędny rok szkolny: $schoolYear")
        val reference = LocalDate.of(startYear, 9, 1)
        val age = java.time.Period.between(birthDate, reference).years
        val groups = ageGroups.findAllOrdered()
        val match = groups.lastOrNull { it.minAgeYears <= age } ?: groups.first()
        return match.id!!
    }
}
