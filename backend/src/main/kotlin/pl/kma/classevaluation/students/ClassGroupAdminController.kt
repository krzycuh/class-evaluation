package pl.kma.classevaluation.students

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pl.kma.classevaluation.assessments.AssessmentPeriod
import pl.kma.classevaluation.assessments.AssessmentPeriodRepository
import pl.kma.classevaluation.auth.UserRepository
import pl.kma.classevaluation.common.BadRequestException
import pl.kma.classevaluation.common.ConflictException
import pl.kma.classevaluation.common.NotFoundException
import java.time.LocalDate
import java.util.UUID

data class CreateClassGroupRequest(
    @field:NotBlank val name: String,
    @field:NotBlank @field:Pattern(regexp = """\d{4}/\d{4}""", message = "rok szkolny w formacie 2026/2027")
    val schoolYear: String,
)

data class UpdateClassGroupRequest(
    val name: String? = null,
    val schoolYear: String? = null,
)

data class AssignTeachersRequest(val teacherIds: List<UUID>)

data class RolloverRequest(
    @field:NotBlank @field:Pattern(regexp = """\d{4}/\d{4}""", message = "rok szkolny w formacie 2027/2028")
    val schoolYear: String,
    val name: String? = null,
)

data class TeacherAssignment(val classGroupId: UUID, val userId: UUID)

/**
 * Zarządzanie grupami i przypisaniami nauczycielek. Zapisy tylko dla ADMIN
 * (SecurityConfig); odczyt przypisań także dla nauczycielek danej grupy.
 */
@RestController
@RequestMapping("/api/class-groups")
@Validated
class ClassGroupAdminController(
    private val classGroups: ClassGroupRepository,
    private val users: UserRepository,
    private val students: StudentRepository,
    private val periods: AssessmentPeriodRepository,
    private val studentService: StudentService,
    private val jdbc: JdbcClient,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun create(@RequestBody @Valid body: CreateClassGroupRequest): ClassGroup =
        classGroups.save(ClassGroup(name = body.name.trim(), schoolYear = body.schoolYear))

    @PatchMapping("/{id}")
    @Transactional
    fun update(@PathVariable id: UUID, @RequestBody body: UpdateClassGroupRequest): ClassGroup {
        val group = classGroups.findById(id).orElseThrow { NotFoundException("Nie znaleziono grupy") }
        if (body.schoolYear != null && !body.schoolYear.matches(Regex("""\d{4}/\d{4}"""))) {
            throw BadRequestException("Rok szkolny w formacie 2026/2027")
        }
        return classGroups.save(
            group.copy(
                name = body.name?.trim()?.takeIf { it.isNotEmpty() } ?: group.name,
                schoolYear = body.schoolYear ?: group.schoolYear,
            ),
        )
    }

    @GetMapping("/{id}/teachers")
    fun teachers(@PathVariable id: UUID): List<UUID> {
        studentService.requireClassGroupAccess(id)
        return classGroups.findTeacherIds(id)
    }

    @PutMapping("/{id}/teachers")
    @Transactional
    fun assignTeachers(@PathVariable id: UUID, @RequestBody body: AssignTeachersRequest): List<UUID> {
        if (classGroups.findById(id).isEmpty) throw NotFoundException("Nie znaleziono grupy")
        val ids = body.teacherIds.distinct()
        ids.forEach { userId ->
            if (users.findById(userId).isEmpty) throw BadRequestException("Nieznany użytkownik: $userId")
        }
        classGroups.clearTeachers(id)
        ids.forEach { classGroups.assignTeacher(id, it) }
        return classGroups.findTeacherIds(id)
    }

    /** Zestawienie „kto ma dostęp do czego" — wszystkie przypisania (tylko ADMIN, SecurityConfig). */
    @GetMapping("/assignments")
    fun assignments(): List<TeacherAssignment> =
        jdbc.sql("SELECT class_group_id, user_id FROM class_group_teachers")
            .query { rs, _ ->
                TeacherAssignment(
                    classGroupId = rs.getObject("class_group_id", UUID::class.java),
                    userId = rs.getObject("user_id", UUID::class.java),
                )
            }
            .list()

    /**
     * Przejście grupy na nowy rok szkolny: nowa grupa z tymi samymi nauczycielkami,
     * aktywne dzieci przenoszone z przeliczeniem grupy wiekowej, semestry nowego
     * roku dokładane, jeśli ich brak. Historia ocen zostaje przy dzieciach
     * (oceny są per semestr, nie per grupa).
     */
    @PostMapping("/{id}/rollover")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun rollover(@PathVariable id: UUID, @RequestBody @Valid body: RolloverRequest): ClassGroup {
        val group = classGroups.findById(id).orElseThrow { NotFoundException("Nie znaleziono grupy") }
        if (body.schoolYear <= group.schoolYear) {
            throw BadRequestException("Nowy rok szkolny musi być późniejszy niż ${group.schoolYear}")
        }
        val name = body.name?.trim()?.takeIf { it.isNotEmpty() } ?: group.name
        if (classGroups.findAllOrdered().any { it.name == name && it.schoolYear == body.schoolYear }) {
            throw ConflictException("Grupa '$name' w roku ${body.schoolYear} już istnieje")
        }

        val newGroup = classGroups.save(ClassGroup(name = name, schoolYear = body.schoolYear))
        classGroups.findTeacherIds(id).forEach { classGroups.assignTeacher(newGroup.id!!, it) }

        students.findActiveByClassGroupId(id).forEach { student ->
            students.save(
                student.copy(
                    classGroupId = newGroup.id!!,
                    ageGroupId = studentService.suggestAgeGroup(student.birthDate, body.schoolYear),
                ),
            )
        }

        if (!periods.existsBySchoolYear(body.schoolYear)) {
            val startYear = body.schoolYear.substringBefore('/').toInt()
            periods.save(
                AssessmentPeriod(
                    schoolYear = body.schoolYear,
                    name = "Semestr I",
                    startsOn = LocalDate.of(startYear, 9, 1),
                    endsOn = LocalDate.of(startYear + 1, 1, 31),
                ),
            )
            periods.save(
                AssessmentPeriod(
                    schoolYear = body.schoolYear,
                    name = "Semestr II",
                    startsOn = LocalDate.of(startYear + 1, 2, 1),
                    endsOn = LocalDate.of(startYear + 1, 6, 30),
                ),
            )
        }

        return newGroup
    }
}
