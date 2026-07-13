package pl.kma.classevaluation.projects

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
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

data class ProjectTaskDto(
    val id: UUID,
    val title: String,
    val dueOn: LocalDate?,
    val done: Boolean,
    val sortOrder: Int,
)

data class ProjectDto(
    val id: UUID,
    val title: String,
    val description: String?,
    val kind: ProjectKind,
    val scope: ProjectScope,
    val classGroupId: UUID?,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val status: ProjectStatus,
    val doneTasks: Int,
    val totalTasks: Int,
    val canEdit: Boolean,
    val tasks: List<ProjectTaskDto>? = null,
)

data class CreateProjectRequest(
    @field:NotBlank val title: String,
    val description: String? = null,
    val kind: ProjectKind,
    val scope: ProjectScope,
    val classGroupId: UUID? = null,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
)

data class UpdateProjectRequest(
    val title: String? = null,
    val description: String? = null,
    val kind: ProjectKind? = null,
    val startsOn: LocalDate? = null,
    val endsOn: LocalDate? = null,
    val status: ProjectStatus? = null,
)

data class CreateProjectTaskRequest(
    @field:NotBlank val title: String,
    val dueOn: LocalDate? = null,
)

data class UpdateProjectTaskRequest(
    val title: String? = null,
    val dueOn: LocalDate? = null,
    val done: Boolean? = null,
)

private fun ProjectTask.toDto() = ProjectTaskDto(
    id = id!!,
    title = title,
    dueOn = dueOn,
    done = done,
    sortOrder = sortOrder,
)

@RestController
@RequestMapping("/api")
class ProjectController(private val service: ProjectService) {

    @GetMapping("/projects")
    fun list(@RequestParam classGroupId: UUID): List<ProjectDto> =
        service.listVisible(classGroupId).map { toDto(it, withTasks = false) }

    @GetMapping("/projects/{id}")
    fun get(@PathVariable id: UUID): ProjectDto = toDto(service.getProject(id), withTasks = true)

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody @Valid body: CreateProjectRequest): ProjectDto =
        toDto(
            service.createProject(
                body.title, body.description, body.kind, body.scope,
                body.classGroupId, body.startsOn, body.endsOn,
            ),
            withTasks = true,
        )

    @PatchMapping("/projects/{id}")
    fun update(@PathVariable id: UUID, @RequestBody body: UpdateProjectRequest): ProjectDto =
        toDto(
            service.updateProject(id, body.title, body.description, body.kind, body.startsOn, body.endsOn, body.status),
            withTasks = true,
        )

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = service.deleteProject(id)

    @PostMapping("/projects/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    fun addTask(@PathVariable id: UUID, @RequestBody @Valid body: CreateProjectTaskRequest): ProjectTaskDto =
        service.addTask(id, body.title, body.dueOn).toDto()

    @PatchMapping("/projects/{id}/tasks/{taskId}")
    fun updateTask(
        @PathVariable id: UUID,
        @PathVariable taskId: UUID,
        @RequestBody body: UpdateProjectTaskRequest,
    ): ProjectTaskDto = service.updateTask(id, taskId, body.title, body.dueOn, body.done).toDto()

    @DeleteMapping("/projects/{id}/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTask(@PathVariable id: UUID, @PathVariable taskId: UUID) = service.deleteTask(id, taskId)

    private fun toDto(project: Project, withTasks: Boolean): ProjectDto {
        val tasks = service.tasksOf(project.id!!)
        return ProjectDto(
            id = project.id,
            title = project.title,
            description = project.description,
            kind = project.kind,
            scope = project.scope,
            classGroupId = project.classGroupId,
            startsOn = project.startsOn,
            endsOn = project.endsOn,
            status = project.status,
            doneTasks = tasks.count { it.done },
            totalTasks = tasks.size,
            canEdit = service.canEdit(project),
            tasks = if (withTasks) tasks.map { it.toDto() } else null,
        )
    }
}
