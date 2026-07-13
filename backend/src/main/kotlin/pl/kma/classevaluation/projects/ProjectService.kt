package pl.kma.classevaluation.projects

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kma.classevaluation.auth.CurrentUser
import pl.kma.classevaluation.auth.Role
import pl.kma.classevaluation.common.BadRequestException
import pl.kma.classevaluation.common.NotFoundException
import pl.kma.classevaluation.students.StudentService
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class ProjectService(
    private val projects: ProjectRepository,
    private val tasks: ProjectTaskRepository,
    private val studentService: StudentService,
    private val currentUser: CurrentUser,
) {

    fun listVisible(classGroupId: UUID): List<Project> {
        studentService.requireClassGroupAccess(classGroupId)
        return projects.findVisible(classGroupId)
    }

    fun tasksOf(projectId: UUID): List<ProjectTask> = tasks.findByProjectIdOrdered(projectId)

    fun getProject(projectId: UUID): Project {
        val project = projects.findById(projectId)
            .orElseThrow { NotFoundException("Nie znaleziono projektu") }
        // PRESCHOOL widzą wszyscy; projekt grupy tylko właścicielka grupy (lub admin)
        if (project.scope == ProjectScope.CLASS_GROUP) {
            studentService.requireClassGroupAccess(project.classGroupId!!)
        }
        return project
    }

    fun canEdit(project: Project): Boolean {
        val user = currentUser.get()
        return when (project.scope) {
            ProjectScope.PRESCHOOL -> user.role == Role.ADMIN || project.createdBy == user.id
            ProjectScope.CLASS_GROUP -> runCatching { studentService.requireClassGroupAccess(project.classGroupId!!) }.isSuccess
        }
    }

    private fun requireEditRights(project: Project) {
        if (!canEdit(project)) throw AccessDeniedException("Brak uprawnień do edycji projektu")
    }

    private fun validate(scope: ProjectScope, classGroupId: UUID?, startsOn: LocalDate, endsOn: LocalDate) {
        if (endsOn.isBefore(startsOn)) throw BadRequestException("Data końca przed datą początku")
        when (scope) {
            ProjectScope.PRESCHOOL -> if (classGroupId != null) throw BadRequestException("Projekt przedszkolny nie wskazuje grupy")
            ProjectScope.CLASS_GROUP -> {
                if (classGroupId == null) throw BadRequestException("Projekt grupy wymaga wskazania grupy")
                studentService.requireClassGroupAccess(classGroupId)
            }
        }
    }

    @Transactional
    fun createProject(
        title: String,
        description: String?,
        kind: ProjectKind,
        scope: ProjectScope,
        classGroupId: UUID?,
        startsOn: LocalDate,
        endsOn: LocalDate,
    ): Project {
        validate(scope, classGroupId, startsOn, endsOn)
        return projects.save(
            Project(
                title = title.trim(),
                description = description,
                kind = kind,
                scope = scope,
                classGroupId = classGroupId,
                startsOn = startsOn,
                endsOn = endsOn,
                createdBy = currentUser.get().id,
            ),
        )
    }

    @Transactional
    fun updateProject(
        projectId: UUID,
        title: String?,
        description: String?,
        kind: ProjectKind?,
        startsOn: LocalDate?,
        endsOn: LocalDate?,
        status: ProjectStatus?,
    ): Project {
        val project = getProject(projectId)
        requireEditRights(project)
        val updated = project.copy(
            title = title?.trim() ?: project.title,
            description = description ?: project.description,
            kind = kind ?: project.kind,
            startsOn = startsOn ?: project.startsOn,
            endsOn = endsOn ?: project.endsOn,
            status = status ?: project.status,
            updatedAt = Instant.now(),
        )
        if (updated.endsOn.isBefore(updated.startsOn)) throw BadRequestException("Data końca przed datą początku")
        return projects.save(updated)
    }

    @Transactional
    fun deleteProject(projectId: UUID) {
        val project = getProject(projectId)
        requireEditRights(project)
        projects.deleteById(project.id!!)
    }

    @Transactional
    fun addTask(projectId: UUID, title: String, dueOn: LocalDate?): ProjectTask {
        val project = getProject(projectId)
        requireEditRights(project)
        return tasks.save(
            ProjectTask(
                projectId = project.id!!,
                title = title.trim(),
                dueOn = dueOn,
                sortOrder = (tasks.maxSortOrder(project.id) ?: 0) + 1,
            ),
        )
    }

    @Transactional
    fun updateTask(projectId: UUID, taskId: UUID, title: String?, dueOn: LocalDate?, done: Boolean?): ProjectTask {
        val project = getProject(projectId)
        requireEditRights(project)
        val task = tasks.findById(taskId)
            .filter { it.projectId == project.id }
            .orElseThrow { NotFoundException("Nie znaleziono zadania") }
        return tasks.save(
            task.copy(
                title = title?.trim() ?: task.title,
                dueOn = dueOn ?: task.dueOn,
                done = done ?: task.done,
            ),
        )
    }

    @Transactional
    fun deleteTask(projectId: UUID, taskId: UUID) {
        val project = getProject(projectId)
        requireEditRights(project)
        val task = tasks.findById(taskId)
            .filter { it.projectId == project.id }
            .orElseThrow { NotFoundException("Nie znaleziono zadania") }
        tasks.deleteById(task.id!!)
    }
}
