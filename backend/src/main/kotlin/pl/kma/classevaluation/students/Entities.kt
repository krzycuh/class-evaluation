package pl.kma.classevaluation.students

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
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
    @Query(
        """
        SELECT g.* FROM class_groups g
          JOIN class_group_teachers t ON t.class_group_id = g.id
         WHERE t.user_id = :userId
         ORDER BY g.school_year DESC, g.name
        """,
    )
    fun findByTeacher(userId: UUID): List<ClassGroup>

    @Query("SELECT * FROM class_groups ORDER BY school_year DESC, name")
    fun findAllOrdered(): List<ClassGroup>

    @Query("SELECT count(*) > 0 FROM class_group_teachers WHERE class_group_id = :classGroupId AND user_id = :userId")
    fun isAssigned(classGroupId: UUID, userId: UUID): Boolean

    @Query("SELECT user_id FROM class_group_teachers WHERE class_group_id = :classGroupId")
    fun findTeacherIds(classGroupId: UUID): List<UUID>

    @Modifying
    @Query("INSERT INTO class_group_teachers (class_group_id, user_id) VALUES (:classGroupId, :userId) ON CONFLICT DO NOTHING")
    fun assignTeacher(classGroupId: UUID, userId: UUID)

    @Modifying
    @Query("DELETE FROM class_group_teachers WHERE class_group_id = :classGroupId")
    fun clearTeachers(classGroupId: UUID)
}

interface StudentRepository : CrudRepository<Student, UUID> {
    @Query("SELECT * FROM students WHERE class_group_id = :classGroupId AND active")
    fun findActiveByClassGroupId(classGroupId: UUID): List<Student>
}

interface AgeGroupRepository : CrudRepository<AgeGroup, UUID> {
    @Query("SELECT * FROM age_groups ORDER BY sort_order")
    fun findAllOrdered(): List<AgeGroup>
}
