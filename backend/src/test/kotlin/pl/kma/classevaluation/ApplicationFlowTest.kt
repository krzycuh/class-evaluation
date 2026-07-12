package pl.kma.classevaluation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = ["app.admin.password=test-password-123"])
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ApplicationFlowTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    private val admin = user("admin@example.com").roles("ADMIN")

    private fun getJson(uri: String): JsonNode {
        val response = mockMvc.perform(get(uri).with(admin))
            .andExpect(status().isOk)
            .andReturn().response.getContentAsString(Charsets.UTF_8)
        return objectMapper.readTree(response)
    }

    @Test
    @Order(1)
    fun `login works and rejects bad credentials`() {
        mockMvc.perform(
            post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"admin@example.com","password":"test-password-123"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("ADMIN"))

        mockMvc.perform(
            post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"admin@example.com","password":"wrong"}"""),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @Order(2)
    fun `anonymous requests are rejected`() {
        mockMvc.perform(get("/api/class-groups"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @Order(3)
    fun `full flow - student, assessment, report`() {
        val classGroupId = getJson("/api/class-groups")[0]["id"].asText()
        val periodId = getJson("/api/periods").let { periods ->
            periods.first { it["name"].asText() == "Semestr I" }["id"].asText()
        }

        // uczeń 4-latek (grupa wiekowa podpowiedziana z daty urodzenia)
        val studentResponse = mockMvc.perform(
            post("/api/class-groups/$classGroupId/students").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Ania","lastName":"Kowalska","birthDate":"2022-03-10"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn().response.getContentAsString(Charsets.UTF_8)
        val studentId = objectMapper.readTree(studentResponse)["id"].asText()

        // widok oceny zawiera obszary i umiejętności dla grupy wiekowej
        val view = getJson("/api/students/$studentId/assessment?periodId=$periodId")
        assertThat(view["areas"].size()).isGreaterThan(0)
        val firstSkill = view["areas"][0]["skills"][0]
        val skillId = firstSkill["skillId"].asText()
        assertThat(firstSkill["value"]?.isNull ?: true).isTrue()

        // ocena: potrafi + notatka
        mockMvc.perform(
            put("/api/students/$studentId/assessments/$skillId?periodId=$periodId").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"value":"MASTERED","note":"świetnie sobie radzi"}"""),
        )
            .andExpect(status().isNoContent)

        // druga umiejętność: jeszcze nie
        val skillId2 = view["areas"][0]["skills"][1]["skillId"].asText()
        mockMvc.perform(
            put("/api/students/$studentId/assessments/$skillId2?periodId=$periodId").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"value":"NOT_YET"}"""),
        )
            .andExpect(status().isNoContent)

        // notatka ogólna
        mockMvc.perform(
            put("/api/students/$studentId/period-note?periodId=$periodId").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"Chętnie pomaga innym dzieciom."}"""),
        )
            .andExpect(status().isNoContent)

        // postęp widoczny na liście uczniów
        val students = getJson("/api/class-groups/$classGroupId/students?periodId=$periodId")
        val row = students.first { it["id"].asText() == studentId }
        assertThat(row["assessedCount"].asInt()).isEqualTo(2)
        assertThat(row["totalSkills"].asInt()).isGreaterThan(2)

        // raport: sekcje potrafi / pracujemy + snapshot notatki
        val reportResponse = mockMvc.perform(
            post("/api/students/$studentId/reports?periodId=$periodId").with(admin).with(csrf()),
        )
            .andExpect(status().isCreated)
            .andReturn().response.getContentAsString(Charsets.UTF_8)
        val report = objectMapper.readTree(reportResponse)
        assertThat(report["content"]["mastered"].size()).isEqualTo(1)
        assertThat(report["content"]["workingOn"].size()).isEqualTo(1)
        assertThat(report["content"]["workingOn"][0]["recommendation"].asText()).isNotBlank()
        assertThat(report["content"]["generalNote"].asText()).contains("pomaga")

        val fetched = getJson("/api/reports/${report["id"].asText()}")
        assertThat(fetched["content"]["studentName"].asText()).isEqualTo("Ania Kowalska")

        // dezaktywacja ucznia
        mockMvc.perform(delete("/api/students/$studentId").with(admin).with(csrf()))
            .andExpect(status().isNoContent)
    }

    @Test
    @Order(4)
    fun `closed period blocks writes`() {
        val classGroupId = getJson("/api/class-groups")[0]["id"].asText()
        val period = getJson("/api/periods").first { it["name"].asText() == "Semestr II" }
        val periodId = period["id"].asText()

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/periods/$periodId").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CLOSED"}"""),
        )
            .andExpect(status().isOk)

        val studentResponse = mockMvc.perform(
            post("/api/class-groups/$classGroupId/students").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Bartek","lastName":"Nowak","birthDate":"2021-05-20"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn().response.getContentAsString(Charsets.UTF_8)
        val studentId = objectMapper.readTree(studentResponse)["id"].asText()

        val view = getJson("/api/students/$studentId/assessment?periodId=$periodId")
        val skillId = view["areas"][0]["skills"][0]["skillId"].asText()

        mockMvc.perform(
            put("/api/students/$studentId/assessments/$skillId?periodId=$periodId").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"value":"MASTERED"}"""),
        )
            .andExpect(status().isConflict)
    }

    @Test
    @Order(5)
    fun `teacher role cannot modify skills configuration`() {
        val teacher = user("teacher@example.com").roles("TEACHER")
        mockMvc.perform(
            post("/api/skills").with(teacher).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"areaId":"d0000000-0000-0000-0000-000000000001","title":"X"}"""),
        )
            .andExpect(status().isForbidden)
    }
}
