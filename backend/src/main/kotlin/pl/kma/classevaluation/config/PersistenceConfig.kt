package pl.kma.classevaluation.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback
import pl.kma.classevaluation.auth.User
import pl.kma.classevaluation.assessments.AssessmentPeriod
import pl.kma.classevaluation.calendar.CalendarEvent
import pl.kma.classevaluation.calendar.EventCategory
import pl.kma.classevaluation.calendar.EventTask
import pl.kma.classevaluation.projects.Project
import pl.kma.classevaluation.projects.ProjectTask
import pl.kma.classevaluation.skills.DevelopmentArea
import pl.kma.classevaluation.skills.Skill
import pl.kma.classevaluation.students.ClassGroup
import pl.kma.classevaluation.students.Student
import java.util.UUID

/**
 * Identyfikatory UUID nadajemy po stronie aplikacji tuż przed INSERT-em —
 * Spring Data JDBC traktuje encję z id == null jako nową.
 */
@Configuration
class PersistenceConfig {

    @Bean
    fun userIdCallback() = BeforeConvertCallback<User> { e ->
        if (e.id == null) e.copy(id = UUID.randomUUID()) else e
    }

    @Bean
    fun classGroupIdCallback() = BeforeConvertCallback<ClassGroup> { e ->
        if (e.id == null) e.copy(id = UUID.randomUUID()) else e
    }

    @Bean
    fun studentIdCallback() = BeforeConvertCallback<Student> { e ->
        if (e.id == null) e.copy(id = UUID.randomUUID()) else e
    }

    @Bean
    fun developmentAreaIdCallback() = BeforeConvertCallback<DevelopmentArea> { e ->
        if (e.id == null) e.copy(id = UUID.randomUUID()) else e
    }

    @Bean
    fun skillIdCallback() = BeforeConvertCallback<Skill> { e ->
        if (e.id == null) e.copy(id = UUID.randomUUID()) else e
    }

    @Bean
    fun periodIdCallback() = BeforeConvertCallback<AssessmentPeriod> { e ->
        if (e.id == null) e.copy(id = UUID.randomUUID()) else e
    }

    @Bean
    fun eventCategoryIdCallback() = BeforeConvertCallback<EventCategory> { e ->
        if (e.id == null) e.copy(id = UUID.randomUUID()) else e
    }

    @Bean
    fun calendarEventIdCallback() = BeforeConvertCallback<CalendarEvent> { e ->
        if (e.id == null) e.copy(id = UUID.randomUUID()) else e
    }

    @Bean
    fun eventTaskIdCallback() = BeforeConvertCallback<EventTask> { e ->
        if (e.id == null) e.copy(id = UUID.randomUUID()) else e
    }

    @Bean
    fun projectIdCallback() = BeforeConvertCallback<Project> { e ->
        if (e.id == null) e.copy(id = UUID.randomUUID()) else e
    }

    @Bean
    fun projectTaskIdCallback() = BeforeConvertCallback<ProjectTask> { e ->
        if (e.id == null) e.copy(id = UUID.randomUUID()) else e
    }
}
