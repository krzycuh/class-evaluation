package pl.kma.classevaluation.students

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

data class CreateStudentRequest(
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:Past val birthDate: LocalDate,
    val ageGroupId: UUID? = null,
)

data class UpdateStudentRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: LocalDate? = null,
    val ageGroupId: UUID? = null,
    val active: Boolean? = null,
)

@RestController
@RequestMapping("/api")
class StudentController(
    private val service: StudentService,
    private val ageGroups: AgeGroupRepository,
) {

    @GetMapping("/class-groups")
    fun classGroups(): List<ClassGroup> = service.listClassGroups()

    @GetMapping("/class-groups/{id}/students")
    fun students(@PathVariable id: UUID, @RequestParam periodId: UUID): List<StudentWithProgress> =
        service.listStudentsWithProgress(id, periodId)

    @PostMapping("/class-groups/{id}/students")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@PathVariable id: UUID, @RequestBody @Valid body: CreateStudentRequest): Student =
        service.createStudent(id, body.firstName, body.lastName, body.birthDate, body.ageGroupId)

    @PatchMapping("/students/{id}")
    fun update(@PathVariable id: UUID, @RequestBody body: UpdateStudentRequest): Student =
        service.updateStudent(id, body.firstName, body.lastName, body.birthDate, body.ageGroupId, body.active)

    @DeleteMapping("/students/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivate(@PathVariable id: UUID) = service.deactivateStudent(id)

    @GetMapping("/age-groups")
    fun ageGroups(): List<AgeGroup> = ageGroups.findAllOrdered()
}
