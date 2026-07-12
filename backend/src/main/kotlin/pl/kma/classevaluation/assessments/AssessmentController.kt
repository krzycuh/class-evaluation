package pl.kma.classevaluation.assessments

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

data class UpsertAssessmentRequest(
    val value: AssessmentValue? = null,
    val note: String? = null,
)

data class UpsertNoteRequest(val content: String = "")

data class CreatePeriodRequest(
    @field:NotBlank val schoolYear: String,
    @field:NotBlank val name: String,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
)

data class UpdatePeriodRequest(val status: PeriodStatus)

@RestController
@RequestMapping("/api")
class AssessmentController(private val service: AssessmentService) {

    @GetMapping("/students/{id}/assessment")
    fun studentAssessment(@PathVariable id: UUID, @RequestParam periodId: UUID): StudentAssessmentView =
        service.getStudentAssessment(id, periodId)

    @PutMapping("/students/{id}/assessments/{skillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun upsert(
        @PathVariable id: UUID,
        @PathVariable skillId: UUID,
        @RequestParam periodId: UUID,
        @RequestBody body: UpsertAssessmentRequest,
    ) = service.upsertAssessment(id, skillId, periodId, body.value, body.note)

    @PutMapping("/students/{id}/period-note")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun upsertNote(
        @PathVariable id: UUID,
        @RequestParam periodId: UUID,
        @RequestBody body: UpsertNoteRequest,
    ) = service.upsertGeneralNote(id, periodId, body.content)

    @GetMapping("/periods")
    fun periods(): List<AssessmentPeriod> = service.listPeriods()

    @PostMapping("/periods")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPeriod(@RequestBody @Valid body: CreatePeriodRequest): AssessmentPeriod =
        service.createPeriod(body.schoolYear, body.name, body.startsOn, body.endsOn)

    @PatchMapping("/periods/{id}")
    fun updatePeriod(@PathVariable id: UUID, @RequestBody body: UpdatePeriodRequest): AssessmentPeriod =
        service.updatePeriodStatus(id, body.status)
}
