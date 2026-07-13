package pl.kma.classevaluation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
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
class CalendarProjectsFlowTest {

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

    private fun classGroupId(): String = getJson("/api/class-groups")[0]["id"].asText()

    @Test
    fun `seeded national holidays recur yearly in the feed`() {
        val groupId = classGroupId()
        val feed = getJson("/api/calendar?classGroupId=$groupId&from=2027-01-01&to=2027-01-31")
        val holiday = feed.first { it["title"].asText() == "Dzień Babci" }
        assertThat(holiday["startsOn"].asText()).isEqualTo("2027-01-21")
        assertThat(holiday["scope"].asText()).isEqualTo("NATIONAL")
        assertThat(holiday["categoryColor"].asText()).isNotBlank()
    }

    @Test
    fun `group event with organizer checklist - full flow`() {
        val groupId = classGroupId()
        val categoryId = getJson("/api/event-categories")
            .first { it["name"].asText() == "Tydzień tematyczny" }["id"].asText()

        // tydzień tematyczny grupy
        val created = mockMvc.perform(
            post("/api/events").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Tydzień książki","categoryId":"$categoryId","scope":"CLASS_GROUP",
                     "classGroupId":"$groupId","startsOn":"2026-03-02","endsOn":"2026-03-06",
                     "description":"Codziennie czytamy inną bajkę."}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.canEdit").value(true))
            .andReturn().response.getContentAsString(Charsets.UTF_8)
        val eventId = objectMapper.readTree(created)["id"].asText()

        // checklista organizera
        val tasksResponse = mockMvc.perform(
            put("/api/events/$eventId/tasks").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"tasks":[{"title":"Wybrać książki"},{"title":"Plakat dla rodziców","done":true}]}"""),
        )
            .andExpect(status().isOk)
            .andReturn().response.getContentAsString(Charsets.UTF_8)
        val tasks = objectMapper.readTree(tasksResponse)
        assertThat(tasks.size()).isEqualTo(2)

        // odhaczenie pierwszego zadania
        mockMvc.perform(
            patch("/api/events/$eventId/tasks/${tasks[0]["id"].asText()}").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"done":true}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.done").value(true))

        // szczegóły z checklistą
        val details = getJson("/api/events/$eventId")
        assertThat(details["tasks"].size()).isEqualTo(2)
        assertThat(details["tasks"][0]["done"].asBoolean()).isTrue()

        // wydarzenie widoczne w feedzie miesiąca
        val feed = getJson("/api/calendar?classGroupId=$groupId&from=2026-03-01&to=2026-03-31")
        val item = feed.first { it["id"].asText() == eventId }
        assertThat(item["endsOn"].asText()).isEqualTo("2026-03-06")

        // edycja i usunięcie
        mockMvc.perform(
            patch("/api/events/$eventId").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Tydzień książki i bajki"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Tydzień książki i bajki"))

        mockMvc.perform(delete("/api/events/$eventId").with(admin).with(csrf()))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/events/$eventId").with(admin))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `event scope must be consistent with group and student`() {
        val categoryId = getJson("/api/event-categories")[0]["id"].asText()
        mockMvc.perform(
            post("/api/events").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Błędne","categoryId":"$categoryId","scope":"CLASS_GROUP",
                     "startsOn":"2026-03-02","endsOn":"2026-03-02"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `project with tasks drives progress and calendar feed`() {
        val groupId = classGroupId()

        val created = mockMvc.perform(
            post("/api/projects").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Wycieczka do ZOO","kind":"TRIP","scope":"CLASS_GROUP",
                     "classGroupId":"$groupId","startsOn":"2026-05-04","endsOn":"2026-05-20"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PLANNED"))
            .andReturn().response.getContentAsString(Charsets.UTF_8)
        val projectId = objectMapper.readTree(created)["id"].asText()

        // zadania: jedno z terminem, jedno bez
        val task1 = mockMvc.perform(
            post("/api/projects/$projectId/tasks").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Zebrać zgody rodziców","dueOn":"2026-05-10"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn().response.getContentAsString(Charsets.UTF_8)
        val task1Id = objectMapper.readTree(task1)["id"].asText()

        mockMvc.perform(
            post("/api/projects/$projectId/tasks").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Rezerwacja autokaru"}"""),
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            patch("/api/projects/$projectId/tasks/$task1Id").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"done":true}"""),
        )
            .andExpect(status().isOk)

        // postęp z checklisty
        val project = getJson("/api/projects/$projectId")
        assertThat(project["doneTasks"].asInt()).isEqualTo(1)
        assertThat(project["totalTasks"].asInt()).isEqualTo(2)
        assertThat(project["tasks"].size()).isEqualTo(2)

        // projekt i zadanie z terminem w feedzie kalendarza
        val feed = getJson("/api/calendar?classGroupId=$groupId&from=2026-05-01&to=2026-05-31")
        val projectItem = feed.first { it["type"].asText() == "PROJECT" && it["id"].asText() == projectId }
        assertThat(projectItem["doneTasks"].asInt()).isEqualTo(1)
        val taskItem = feed.first { it["type"].asText() == "PROJECT_TASK" && it["id"].asText() == task1Id }
        assertThat(taskItem["startsOn"].asText()).isEqualTo("2026-05-10")

        // zmiana statusu i usunięcie
        mockMvc.perform(
            patch("/api/projects/$projectId").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"IN_PROGRESS"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))

        mockMvc.perform(delete("/api/projects/$projectId").with(admin).with(csrf()))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/projects/$projectId").with(admin))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `teacher role cannot manage event categories`() {
        val teacher = user("teacher@example.com").roles("TEACHER")
        mockMvc.perform(
            post("/api/event-categories").with(teacher).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"X"}"""),
        )
            .andExpect(status().isForbidden)
    }
}
