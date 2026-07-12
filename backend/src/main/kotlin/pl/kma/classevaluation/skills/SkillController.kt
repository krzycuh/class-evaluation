package pl.kma.classevaluation.skills

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pl.kma.classevaluation.common.NotFoundException
import java.util.UUID

data class SkillDto(
    val id: UUID,
    val areaId: UUID,
    val title: String,
    val description: String?,
    val parentRecommendation: String?,
    val sortOrder: Int,
    val active: Boolean,
    val ageGroupIds: List<UUID>,
)

data class AreaWithSkillsDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val sortOrder: Int,
    val active: Boolean,
    val skills: List<SkillDto>,
)

data class SaveAreaRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
    val sortOrder: Int? = null,
    val active: Boolean? = null,
)

data class CreateSkillRequest(
    val areaId: UUID,
    @field:NotBlank val title: String,
    val description: String? = null,
    val parentRecommendation: String? = null,
    val ageGroupIds: List<UUID> = emptyList(),
)

data class UpdateSkillRequest(
    val areaId: UUID? = null,
    val title: String? = null,
    val description: String? = null,
    val parentRecommendation: String? = null,
    val sortOrder: Int? = null,
    val active: Boolean? = null,
    val ageGroupIds: List<UUID>? = null,
)

private fun Skill.toDto() = SkillDto(
    id = id!!,
    areaId = areaId,
    title = title,
    description = description,
    parentRecommendation = parentRecommendation,
    sortOrder = sortOrder,
    active = active,
    ageGroupIds = ageGroups.map { it.ageGroupId },
)

@RestController
@RequestMapping("/api")
class SkillController(
    private val areas: DevelopmentAreaRepository,
    private val skills: SkillRepository,
) {

    @GetMapping("/development-areas")
    fun list(@RequestParam(defaultValue = "false") includeInactive: Boolean): List<AreaWithSkillsDto> =
        areas.findAllOrdered()
            .filter { includeInactive || it.active }
            .map { area ->
                AreaWithSkillsDto(
                    id = area.id!!,
                    name = area.name,
                    description = area.description,
                    sortOrder = area.sortOrder,
                    active = area.active,
                    skills = skills.findByAreaId(area.id)
                        .filter { includeInactive || it.active }
                        .map { it.toDto() },
                )
            }

    @PostMapping("/development-areas")
    @ResponseStatus(HttpStatus.CREATED)
    fun createArea(@RequestBody @Valid body: SaveAreaRequest): DevelopmentArea {
        val maxOrder = areas.findAllOrdered().maxOfOrNull { it.sortOrder } ?: 0
        return areas.save(
            DevelopmentArea(
                name = body.name.trim(),
                description = body.description,
                sortOrder = body.sortOrder ?: (maxOrder + 1),
            ),
        )
    }

    @PatchMapping("/development-areas/{id}")
    fun updateArea(@PathVariable id: UUID, @RequestBody body: SaveAreaRequest): DevelopmentArea {
        val area = areas.findById(id).orElseThrow { NotFoundException("Nie znaleziono obszaru") }
        return areas.save(
            area.copy(
                name = body.name.trim(),
                description = body.description ?: area.description,
                sortOrder = body.sortOrder ?: area.sortOrder,
                active = body.active ?: area.active,
            ),
        )
    }

    @PostMapping("/skills")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun createSkill(@RequestBody @Valid body: CreateSkillRequest): SkillDto {
        areas.findById(body.areaId).orElseThrow { NotFoundException("Nie znaleziono obszaru") }
        val skill = skills.save(
            Skill(
                areaId = body.areaId,
                title = body.title.trim(),
                description = body.description,
                parentRecommendation = body.parentRecommendation,
                sortOrder = (skills.maxSortOrder(body.areaId) ?: 0) + 1,
                ageGroups = body.ageGroupIds.map { SkillAgeGroup(it) }.toSet(),
            ),
        )
        return skill.toDto()
    }

    @PatchMapping("/skills/{id}")
    @Transactional
    fun updateSkill(@PathVariable id: UUID, @RequestBody body: UpdateSkillRequest): SkillDto {
        val skill = skills.findById(id).orElseThrow { NotFoundException("Nie znaleziono umiejętności") }
        if (body.areaId != null) {
            areas.findById(body.areaId).orElseThrow { NotFoundException("Nie znaleziono obszaru") }
        }
        val saved = skills.save(
            skill.copy(
                areaId = body.areaId ?: skill.areaId,
                title = body.title?.trim() ?: skill.title,
                description = body.description ?: skill.description,
                parentRecommendation = body.parentRecommendation ?: skill.parentRecommendation,
                sortOrder = body.sortOrder ?: skill.sortOrder,
                active = body.active ?: skill.active,
                ageGroups = body.ageGroupIds?.map { SkillAgeGroup(it) }?.toSet() ?: skill.ageGroups,
            ),
        )
        return saved.toDto()
    }
}
