package pl.kma.classevaluation.students

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Table("class_groups")
data class ClassGroup(
    @Id val id: UUID? = null,
    val name: String,
    val schoolYear: String,
    val ownerUserId: UUID,
)

@Table("students")
data class Student(
    @Id val id: UUID? = null,
    val classGroupId: UUID,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val ageGroupId: UUID,
    val active: Boolean = true,
    val createdAt: Instant = Instant.now(),
)

@Table("age_groups")
data class AgeGroup(
    @Id val id: UUID? = null,
    val name: String,
    val minAgeYears: Int,
    val sortOrder: Int,
)

interface ClassGroupRepository : CrudRepository<ClassGroup, UUID> {
    fun findByOwnerUserId(ownerUserId: UUID): List<ClassGroup>

    @Query("SELECT * FROM class_groups ORDER BY school_year DESC, name")
    fun findAllOrdered(): List<ClassGroup>
}

interface StudentRepository : CrudRepository<Student, UUID>

interface AgeGroupRepository : CrudRepository<AgeGroup, UUID> {
    @Query("SELECT * FROM age_groups ORDER BY sort_order")
    fun findAllOrdered(): List<AgeGroup>
}
