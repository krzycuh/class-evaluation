package pl.kma.classevaluation.reports

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class ReportController(private val service: ReportService) {

    @PostMapping("/students/{id}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun generate(@PathVariable id: UUID, @RequestParam periodId: UUID): ReportDto =
        service.generate(id, periodId)

    @GetMapping("/reports/{id}")
    fun get(@PathVariable id: UUID): ReportDto = service.get(id)

    @GetMapping("/class-groups/{id}/reports")
    fun listForClassGroup(@PathVariable id: UUID, @RequestParam periodId: UUID): List<ClassReportRow> =
        service.listForClassGroup(id, periodId)
}
