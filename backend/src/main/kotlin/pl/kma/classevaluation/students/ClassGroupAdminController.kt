package pl.kma.classevaluation.students

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
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
import pl.kma.classevaluation.auth.UserRepository
import pl.kma.classevaluation.common.BadRequestException
import pl.kma.classevaluation.common.NotFoundException
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
    private val studentService: StudentService,
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
}
