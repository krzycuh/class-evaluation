package pl.kma.classevaluation.calendar

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
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
import pl.kma.classevaluation.common.NotFoundException
import java.time.LocalDate
import java.util.UUID

data class EventTaskDto(val id: UUID, val title: String, val done: Boolean, val sortOrder: Int)

data class EventDetailsDto(
    val id: UUID,
    val title: String,
    val description: String?,
    val categoryId: UUID,
    val scope: EventScope,
    val classGroupId: UUID?,
    val studentId: UUID?,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val yearlyRecurring: Boolean,
    val canEdit: Boolean,
    val tasks: List<EventTaskDto>,
)

data class CreateEventRequest(
    @field:NotBlank val title: String,
    val description: String? = null,
    val categoryId: UUID,
    val scope: EventScope,
    val classGroupId: UUID? = null,
    val studentId: UUID? = null,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val yearlyRecurring: Boolean = false,
)

data class UpdateEventRequest(
    val title: String? = null,
    val description: String? = null,
    val categoryId: UUID? = null,
    val scope: EventScope? = null,
    val classGroupId: UUID? = null,
    val studentId: UUID? = null,
    val startsOn: LocalDate? = null,
    val endsOn: LocalDate? = null,
    val yearlyRecurring: Boolean? = null,
)

data class TaskItem(@field:NotBlank val title: String, val done: Boolean = false)

data class ReplaceTasksRequest(val tasks: List<TaskItem> = emptyList())

data class ToggleTaskRequest(val done: Boolean)

data class SaveCategoryRequest(
    @field:NotBlank val name: String,
    val color: String? = null,
    val sortOrder: Int? = null,
    val active: Boolean? = null,
)

private fun EventTask.toDto() = EventTaskDto(id = id!!, title = title, done = done, sortOrder = sortOrder)

@RestController
@RequestMapping("/api")
class CalendarController(
    private val service: CalendarService,
    private val categories: EventCategoryRepository,
) {

    @GetMapping("/calendar")
    fun feed(
        @RequestParam classGroupId: UUID,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ): List<CalendarItem> = service.feed(classGroupId, from, to)

    @GetMapping("/events/{id}")
    fun event(@PathVariable id: UUID): EventDetailsDto = details(service.getEvent(id))

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody @Valid body: CreateEventRequest): EventDetailsDto =
        details(
            service.createEvent(
                body.title, body.description, body.categoryId, body.scope,
                body.classGroupId, body.studentId, body.startsOn, body.endsOn, body.yearlyRecurring,
            ),
        )

    @PatchMapping("/events/{id}")
    fun update(@PathVariable id: UUID, @RequestBody body: UpdateEventRequest): EventDetailsDto =
        details(
            service.updateEvent(
                id, body.title, body.description, body.categoryId, body.scope,
                body.classGroupId, body.studentId, body.startsOn, body.endsOn, body.yearlyRecurring,
            ),
        )

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = service.deleteEvent(id)

    @PutMapping("/events/{id}/tasks")
    fun replaceTasks(@PathVariable id: UUID, @RequestBody @Valid body: ReplaceTasksRequest): List<EventTaskDto> =
        service.replaceTasks(id, body.tasks.map { it.title to it.done }).map { it.toDto() }

    @PatchMapping("/events/{id}/tasks/{taskId}")
    fun toggleTask(
        @PathVariable id: UUID,
        @PathVariable taskId: UUID,
        @RequestBody body: ToggleTaskRequest,
    ): EventTaskDto = service.toggleTask(id, taskId, body.done).toDto()

    @GetMapping("/event-categories")
    fun listCategories(): List<EventCategory> = service.listCategories()

    @PostMapping("/event-categories")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(@RequestBody @Valid body: SaveCategoryRequest): EventCategory {
        val maxOrder = categories.findAllOrdered().maxOfOrNull { it.sortOrder } ?: 0
        return categories.save(
            EventCategory(
                name = body.name.trim(),
                color = body.color ?: "#64766f",
                sortOrder = body.sortOrder ?: (maxOrder + 1),
            ),
        )
    }

    @PatchMapping("/event-categories/{id}")
    fun updateCategory(@PathVariable id: UUID, @RequestBody body: SaveCategoryRequest): EventCategory {
        val category = categories.findById(id).orElseThrow { NotFoundException("Nie znaleziono kategorii") }
        return categories.save(
            category.copy(
                name = body.name.trim(),
                color = body.color ?: category.color,
                sortOrder = body.sortOrder ?: category.sortOrder,
                active = body.active ?: category.active,
            ),
        )
    }

    private fun details(event: CalendarEvent) = EventDetailsDto(
        id = event.id!!,
        title = event.title,
        description = event.description,
        categoryId = event.categoryId,
        scope = event.scope,
        classGroupId = event.classGroupId,
        studentId = event.studentId,
        startsOn = event.startsOn,
        endsOn = event.endsOn,
        yearlyRecurring = event.yearlyRecurring,
        canEdit = service.canEdit(event),
        tasks = service.tasksOf(event.id).map { it.toDto() },
    )
}
