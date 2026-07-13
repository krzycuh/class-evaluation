package pl.kma.classevaluation.calendar

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class EventScope { NATIONAL, PRESCHOOL, CLASS_GROUP, STUDENT }

@Table("event_categories")
data class EventCategory(
    @Id val id: UUID? = null,
    val name: String,
    val color: String,
    val sortOrder: Int = 0,
    val active: Boolean = true,
)

@Table("calendar_events")
data class CalendarEvent(
    @Id val id: UUID? = null,
    val title: String,
    val description: String? = null,
    val categoryId: UUID,
    val scope: EventScope,
    val classGroupId: UUID? = null,
    val studentId: UUID? = null,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val yearlyRecurring: Boolean = false,
    val createdBy: UUID? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

@Table("event_tasks")
data class EventTask(
    @Id val id: UUID? = null,
    val eventId: UUID,
    val title: String,
    val done: Boolean = false,
    val sortOrder: Int = 0,
)

interface EventCategoryRepository : CrudRepository<EventCategory, UUID> {
    @Query("SELECT * FROM event_categories ORDER BY sort_order, name")
    fun findAllOrdered(): List<EventCategory>
}

interface CalendarEventRepository : CrudRepository<CalendarEvent, UUID> {
    @Query(
        """
        SELECT * FROM calendar_events
         WHERE scope IN ('NATIONAL', 'PRESCHOOL') OR class_group_id = :classGroupId
        """,
    )
    fun findVisible(classGroupId: UUID): List<CalendarEvent>
}

interface EventTaskRepository : CrudRepository<EventTask, UUID> {
    @Query("SELECT * FROM event_tasks WHERE event_id = :eventId ORDER BY sort_order")
    fun findByEventIdOrdered(eventId: UUID): List<EventTask>

    @Modifying
    @Query("DELETE FROM event_tasks WHERE event_id = :eventId")
    fun deleteByEventId(eventId: UUID)
}
