package pl.kma.classevaluation.projects

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class ProjectKind { TRIP, CONTEST, OTHER }

enum class ProjectScope { PRESCHOOL, CLASS_GROUP }

enum class ProjectStatus { PLANNED, IN_PROGRESS, DONE, CANCELLED }

@Table("projects")
data class Project(
    @Id val id: UUID? = null,
    val title: String,
    val description: String? = null,
    val kind: ProjectKind,
    val scope: ProjectScope,
    val classGroupId: UUID? = null,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val status: ProjectStatus = ProjectStatus.PLANNED,
    val createdBy: UUID? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

@Table("project_tasks")
data class ProjectTask(
    @Id val id: UUID? = null,
    val projectId: UUID,
    val title: String,
    val dueOn: LocalDate? = null,
    val done: Boolean = false,
    val sortOrder: Int = 0,
)

interface ProjectRepository : CrudRepository<Project, UUID> {
    @Query(
        """
        SELECT * FROM projects
         WHERE scope = 'PRESCHOOL' OR class_group_id = :classGroupId
         ORDER BY starts_on, title
        """,
    )
    fun findVisible(classGroupId: UUID): List<Project>
}

interface ProjectTaskRepository : CrudRepository<ProjectTask, UUID> {
    @Query("SELECT * FROM project_tasks WHERE project_id = :projectId ORDER BY sort_order")
    fun findByProjectIdOrdered(projectId: UUID): List<ProjectTask>

    @Query("SELECT max(sort_order) FROM project_tasks WHERE project_id = :projectId")
    fun maxSortOrder(projectId: UUID): Int?
}
