package pl.kma.classevaluation.calendar

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kma.classevaluation.auth.CurrentUser
import pl.kma.classevaluation.auth.Role
import pl.kma.classevaluation.common.BadRequestException
import pl.kma.classevaluation.common.NotFoundException
import pl.kma.classevaluation.projects.ProjectRepository
import pl.kma.classevaluation.projects.ProjectTaskRepository
import pl.kma.classevaluation.students.StudentRepository
import pl.kma.classevaluation.students.StudentService
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** Pozycja wspólnego feedu kalendarza: wydarzenie, projekt lub zadanie projektu. */
data class CalendarItem(
    val type: String,
    val id: UUID,
    val title: String,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val scope: EventScope? = null,
    val categoryName: String? = null,
    val categoryColor: String? = null,
    val studentName: String? = null,
    val yearlyRecurring: Boolean? = null,
    val kind: pl.kma.classevaluation.projects.ProjectKind? = null,
    val status: pl.kma.classevaluation.projects.ProjectStatus? = null,
    val doneTasks: Int? = null,
    val totalTasks: Int? = null,
    val projectId: UUID? = null,
    val done: Boolean? = null,
)

@Service
class CalendarService(
    private val events: CalendarEventRepository,
    private val eventTasks: EventTaskRepository,
    private val categories: EventCategoryRepository,
    private val projects: ProjectRepository,
    private val projectTasks: ProjectTaskRepository,
    private val studentService: StudentService,
    private val students: StudentRepository,
    private val currentUser: CurrentUser,
) {

    fun listCategories(): List<EventCategory> = categories.findAllOrdered()

    fun feed(classGroupId: UUID, from: LocalDate, to: LocalDate): List<CalendarItem> {
        studentService.requireClassGroupAccess(classGroupId)
        if (to.isBefore(from)) throw BadRequestException("Zakres dat od-do jest odwrócony")

        val categoryById = categories.findAll().associateBy { it.id }
        val items = mutableListOf<CalendarItem>()

        for (event in events.findVisible(classGroupId)) {
            val category = categoryById[event.categoryId]
            val studentName = event.studentId?.let { id ->
                students.findById(id).map { "${it.firstName} ${it.lastName}" }.orElse(null)
            }
            for ((occStart, occEnd) in occurrencesInRange(event, from, to)) {
                items += CalendarItem(
                    type = "EVENT",
                    id = event.id!!,
                    title = event.title,
                    startsOn = occStart,
                    endsOn = occEnd,
                    scope = event.scope,
                    categoryName = category?.name,
                    categoryColor = category?.color,
                    studentName = studentName,
                    yearlyRecurring = event.yearlyRecurring,
                )
            }
        }

        for (project in projects.findVisible(classGroupId)) {
            val tasks = projectTasks.findByProjectIdOrdered(project.id!!)
            if (project.startsOn <= to && project.endsOn >= from) {
                items += CalendarItem(
                    type = "PROJECT",
                    id = project.id,
                    title = project.title,
                    startsOn = project.startsOn,
                    endsOn = project.endsOn,
                    kind = project.kind,
                    status = project.status,
                    doneTasks = tasks.count { it.done },
                    totalTasks = tasks.size,
                )
            }
            for (task in tasks) {
                val due = task.dueOn ?: continue
                if (due in from..to) {
                    items += CalendarItem(
                        type = "PROJECT_TASK",
                        id = task.id!!,
                        title = task.title,
                        startsOn = due,
                        endsOn = due,
                        projectId = project.id,
                        done = task.done,
                    )
                }
            }
        }

        return items.sortedWith(compareBy({ it.startsOn }, { it.type }, { it.title }))
    }

    /**
     * Wystąpienia wydarzenia przecinające zakres [from, to]. Wydarzenie
     * powtarzane co rok jest rzutowane na każdy rok zakresu (29 lutego
     * spada na 28 w latach nieprzestępnych); pozostałe zwracane wprost.
     */
    private fun occurrencesInRange(event: CalendarEvent, from: LocalDate, to: LocalDate): List<Pair<LocalDate, LocalDate>> {
        if (!event.yearlyRecurring) {
            return if (event.startsOn <= to && event.endsOn >= from) listOf(event.startsOn to event.endsOn) else emptyList()
        }
        val durationDays = event.endsOn.toEpochDay() - event.startsOn.toEpochDay()
        return ((from.year - 1)..to.year).mapNotNull { year ->
            val start = withYear(event.startsOn, year)
            val end = start.plusDays(durationDays)
            if (start <= to && end >= from) start to end else null
        }
    }

    private fun withYear(date: LocalDate, year: Int): LocalDate {
        val day = minOf(date.dayOfMonth, YearMonth.of(year, date.month).lengthOfMonth())
        return LocalDate.of(year, date.month, day)
    }

    fun getEvent(eventId: UUID): CalendarEvent {
        val event = events.findById(eventId)
            .orElseThrow { NotFoundException("Nie znaleziono wydarzenia") }
        if (event.scope == EventScope.CLASS_GROUP || event.scope == EventScope.STUDENT) {
            studentService.requireClassGroupAccess(event.classGroupId!!)
        }
        return event
    }

    fun tasksOf(eventId: UUID): List<EventTask> = eventTasks.findByEventIdOrdered(eventId)

    fun canEdit(event: CalendarEvent): Boolean = canEdit(event.scope, event.classGroupId, event.createdBy)

    private fun canEdit(scope: EventScope, classGroupId: UUID?, createdBy: UUID?): Boolean {
        val user = currentUser.get()
        return when (scope) {
            EventScope.NATIONAL -> user.role == Role.ADMIN
            EventScope.PRESCHOOL -> user.role == Role.ADMIN || createdBy == user.id
            EventScope.CLASS_GROUP, EventScope.STUDENT ->
                runCatching { studentService.requireClassGroupAccess(classGroupId!!) }.isSuccess
        }
    }

    private fun requireEditRights(event: CalendarEvent) {
        if (!canEdit(event)) throw AccessDeniedException("Brak uprawnień do edycji wydarzenia")
    }

    private fun validate(scope: EventScope, classGroupId: UUID?, studentId: UUID?, startsOn: LocalDate, endsOn: LocalDate, categoryId: UUID) {
        if (endsOn.isBefore(startsOn)) throw BadRequestException("Data końca przed datą początku")
        if (categories.findById(categoryId).isEmpty) throw BadRequestException("Nieznana kategoria")
        when (scope) {
            EventScope.NATIONAL, EventScope.PRESCHOOL -> {
                if (classGroupId != null || studentId != null) throw BadRequestException("Wydarzenie o tym zasięgu nie wskazuje grupy ani ucznia")
            }
            EventScope.CLASS_GROUP -> {
                if (classGroupId == null || studentId != null) throw BadRequestException("Wydarzenie grupy wymaga wskazania samej grupy")
                studentService.requireClassGroupAccess(classGroupId)
            }
            EventScope.STUDENT -> {
                if (classGroupId == null || studentId == null) throw BadRequestException("Wydarzenie ucznia wymaga wskazania grupy i ucznia")
                val student = studentService.requireStudentAccess(studentId)
                if (student.classGroupId != classGroupId) throw BadRequestException("Uczeń nie należy do wskazanej grupy")
            }
        }
    }

    @Transactional
    fun createEvent(
        title: String,
        description: String?,
        categoryId: UUID,
        scope: EventScope,
        classGroupId: UUID?,
        studentId: UUID?,
        startsOn: LocalDate,
        endsOn: LocalDate,
        yearlyRecurring: Boolean,
    ): CalendarEvent {
        val user = currentUser.get()
        if (scope == EventScope.NATIONAL && user.role != Role.ADMIN) {
            throw AccessDeniedException("Wydarzenia ogólnopolskie tworzy administrator")
        }
        validate(scope, classGroupId, studentId, startsOn, endsOn, categoryId)
        return events.save(
            CalendarEvent(
                title = title.trim(),
                description = description,
                categoryId = categoryId,
                scope = scope,
                classGroupId = classGroupId,
                studentId = studentId,
                startsOn = startsOn,
                endsOn = endsOn,
                yearlyRecurring = yearlyRecurring,
                createdBy = user.id,
            ),
        )
    }

    @Transactional
    fun updateEvent(
        eventId: UUID,
        title: String?,
        description: String?,
        categoryId: UUID?,
        scope: EventScope?,
        classGroupId: UUID?,
        studentId: UUID?,
        startsOn: LocalDate?,
        endsOn: LocalDate?,
        yearlyRecurring: Boolean?,
    ): CalendarEvent {
        val event = getEvent(eventId)
        requireEditRights(event)
        val newScope = scope ?: event.scope
        // przy zmianie zasięgu FK grupy/ucznia przyjmowane z żądania w całości
        val newGroupId = if (scope != null) classGroupId else (classGroupId ?: event.classGroupId)
        val newStudentId = if (scope != null) studentId else (studentId ?: event.studentId)
        val updated = event.copy(
            title = title?.trim() ?: event.title,
            description = description ?: event.description,
            categoryId = categoryId ?: event.categoryId,
            scope = newScope,
            classGroupId = newGroupId,
            studentId = newStudentId,
            startsOn = startsOn ?: event.startsOn,
            endsOn = endsOn ?: event.endsOn,
            yearlyRecurring = yearlyRecurring ?: event.yearlyRecurring,
            updatedAt = Instant.now(),
        )
        validate(updated.scope, updated.classGroupId, updated.studentId, updated.startsOn, updated.endsOn, updated.categoryId)
        if (!canEdit(updated.scope, updated.classGroupId, updated.createdBy)) {
            throw AccessDeniedException("Brak uprawnień do nadania tego zasięgu")
        }
        return events.save(updated)
    }

    @Transactional
    fun deleteEvent(eventId: UUID) {
        val event = getEvent(eventId)
        requireEditRights(event)
        events.deleteById(event.id!!)
    }

    @Transactional
    fun replaceTasks(eventId: UUID, tasks: List<Pair<String, Boolean>>): List<EventTask> {
        val event = getEvent(eventId)
        requireEditRights(event)
        eventTasks.deleteByEventId(event.id!!)
        return tasks.mapIndexed { index, (taskTitle, done) ->
            eventTasks.save(EventTask(eventId = event.id, title = taskTitle.trim(), done = done, sortOrder = index + 1))
        }
    }

    @Transactional
    fun toggleTask(eventId: UUID, taskId: UUID, done: Boolean): EventTask {
        val event = getEvent(eventId)
        requireEditRights(event)
        val task = eventTasks.findById(taskId)
            .filter { it.eventId == event.id }
            .orElseThrow { NotFoundException("Nie znaleziono zadania") }
        return eventTasks.save(task.copy(done = done))
    }
}
