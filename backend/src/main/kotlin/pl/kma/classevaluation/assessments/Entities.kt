package pl.kma.classevaluation.assessments

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import java.time.LocalDate
import java.util.UUID

enum class PeriodStatus { OPEN, CLOSED }

enum class AssessmentValue { MASTERED, NOT_YET, IN_PROGRESS }

@Table("assessment_periods")
data class AssessmentPeriod(
    @Id val id: UUID? = null,
    val schoolYear: String,
    val name: String,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val status: PeriodStatus = PeriodStatus.OPEN,
)

interface AssessmentPeriodRepository : CrudRepository<AssessmentPeriod, UUID> {
    @Query("SELECT * FROM assessment_periods ORDER BY starts_on DESC")
    fun findAllOrdered(): List<AssessmentPeriod>

    @Query("SELECT count(*) > 0 FROM assessment_periods WHERE school_year = :schoolYear")
    fun existsBySchoolYear(schoolYear: String): Boolean
}
