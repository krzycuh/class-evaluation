package pl.kma.classevaluation.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback
import pl.kma.classevaluation.auth.User
import pl.kma.classevaluation.assessments.AssessmentPeriod
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
}
