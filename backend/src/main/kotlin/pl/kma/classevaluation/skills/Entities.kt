package pl.kma.classevaluation.skills

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import java.util.UUID

@Table("development_areas")
data class DevelopmentArea(
    @Id val id: UUID? = null,
    val name: String,
    val description: String? = null,
    val sortOrder: Int = 0,
    val active: Boolean = true,
)

@Table("skill_age_groups")
data class SkillAgeGroup(val ageGroupId: UUID)

@Table("skills")
data class Skill(
    @Id val id: UUID? = null,
    val areaId: UUID,
    val title: String,
    val description: String? = null,
    val parentRecommendation: String? = null,
    val sortOrder: Int = 0,
    val active: Boolean = true,
    @MappedCollection(idColumn = "skill_id")
    val ageGroups: Set<SkillAgeGroup> = emptySet(),
)

interface DevelopmentAreaRepository : CrudRepository<DevelopmentArea, UUID> {
    @Query("SELECT * FROM development_areas ORDER BY sort_order, name")
    fun findAllOrdered(): List<DevelopmentArea>
}

interface SkillRepository : CrudRepository<Skill, UUID> {
    @Query("SELECT * FROM skills WHERE area_id = :areaId ORDER BY sort_order, title")
    fun findByAreaId(areaId: UUID): List<Skill>

    @Query("SELECT max(sort_order) FROM skills WHERE area_id = :areaId")
    fun maxSortOrder(areaId: UUID): Int?
}
