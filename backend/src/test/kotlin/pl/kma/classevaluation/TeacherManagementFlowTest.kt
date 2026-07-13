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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/** Konta nauczycielek, przypisania do grup i izolacja dostępu między grupami. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = ["app.admin.password=test-password-123"])
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TeacherManagementFlowTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        lateinit var teacherAId: String
        lateinit var teacherAPassword: String
        lateinit var teacherBId: String
        lateinit var groupAId: String
        lateinit var groupBId: String
    }

    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    private val admin = user("admin@example.com").roles("ADMIN")
    private val teacherA = user("ania.teacher@example.com").roles("TEACHER")
    private val teacherB = user("basia.teacher@example.com").roles("TEACHER")

    private fun postJson(uri: String, body: String, principal: org.springframework.test.web.servlet.request.RequestPostProcessor = admin): JsonNode {
        val response = mockMvc.perform(
            post(uri).with(principal).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().is2xxSuccessful)
            .andReturn().response.getContentAsString(Charsets.UTF_8)
        return objectMapper.readTree(response)
    }

    private fun getJson(uri: String, principal: org.springframework.test.web.servlet.request.RequestPostProcessor = admin): JsonNode {
        val response = mockMvc.perform(get(uri).with(principal))
            .andExpect(status().isOk)
            .andReturn().response.getContentAsString(Charsets.UTF_8)
        return objectMapper.readTree(response)
    }

    @Test
    @Order(1)
    fun `admin creates teachers and gets one-time initial password`() {
        val created = postJson("/api/users", """{"email":"Ania.Teacher@example.com","displayName":"Ania Nowak"}""")
        teacherAId = created["user"]["id"].asText()
        teacherAPassword = created["initialPassword"].asText()
        assertThat(created["user"]["email"].asText()).isEqualTo("ania.teacher@example.com")
        assertThat(created["user"]["role"].asText()).isEqualTo("TEACHER")
        assertThat(teacherAPassword).hasSizeGreaterThanOrEqualTo(12)

        val createdB = postJson("/api/users", """{"email":"basia.teacher@example.com","displayName":"Basia Kowal"}""")
        teacherBId = createdB["user"]["id"].asText()

        // duplikat e-maila odrzucony
        mockMvc.perform(
            post("/api/users").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"ania.teacher@example.com","displayName":"Duplikat"}"""),
        )
            .andExpect(status().isConflict)
    }

    @Test
    @Order(2)
    fun `teacher can login with initial password`() {
        mockMvc.perform(
            post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"ania.teacher@example.com","password":"$teacherAPassword"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("TEACHER"))
    }

    @Test
    @Order(3)
    fun `admin creates groups and assigns teachers`() {
        groupAId = postJson("/api/class-groups", """{"name":"Motylki","schoolYear":"2026/2027"}""")["id"].asText()
        groupBId = postJson("/api/class-groups", """{"name":"Biedronki","schoolYear":"2026/2027"}""")["id"].asText()

        postJson("/api/class-groups/$groupAId/teachers", """{"teacherIds":["$teacherAId"]}""")
        val assignedB = postJson("/api/class-groups/$groupBId/teachers", """{"teacherIds":["$teacherBId"]}""")
        assertThat(assignedB.map { it.asText() }).containsExactly(teacherBId)
    }

    @Test
    @Order(4)
    fun `teacher sees only assigned groups and cannot touch others`() {
        val groupsOfA = getJson("/api/class-groups", teacherA)
        assertThat(groupsOfA.map { it["id"].asText() }).containsExactly(groupAId)

        // bezpośredni dostęp do cudzej grupy → 403
        val periodId = getJson("/api/periods")[0]["id"].asText()
        mockMvc.perform(get("/api/class-groups/$groupBId/students?periodId=$periodId").with(teacherA))
            .andExpect(status().isForbidden)
        mockMvc.perform(
            post("/api/class-groups/$groupBId/students").with(teacherA).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Ola","lastName":"Obca","birthDate":"2022-01-15"}"""),
        )
            .andExpect(status().isForbidden)

        // admin widzi wszystkie grupy
        val groupsOfAdmin = getJson("/api/class-groups")
        assertThat(groupsOfAdmin.map { it["id"].asText() }).contains(groupAId, groupBId)
    }

    @Test
    @Order(5)
    fun `two teachers assigned to one group work on the same data`() {
        postJson("/api/class-groups/$groupAId/teachers", """{"teacherIds":["$teacherAId","$teacherBId"]}""")

        val periodId = getJson("/api/periods")[0]["id"].asText()
        val student = postJson(
            "/api/class-groups/$groupAId/students",
            """{"firstName":"Janek","lastName":"Wspólny","birthDate":"2022-06-01"}""",
            teacherA,
        )
        val studentsSeenByB = getJson("/api/class-groups/$groupAId/students?periodId=$periodId", teacherB)
        assertThat(studentsSeenByB.map { it["id"].asText() }).contains(student["id"].asText())
    }

    @Test
    @Order(6)
    fun `teacher role cannot manage users or groups`() {
        mockMvc.perform(get("/api/users").with(teacherA))
            .andExpect(status().isForbidden)
        mockMvc.perform(
            post("/api/users").with(teacherA).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"x@example.com","displayName":"X"}"""),
        )
            .andExpect(status().isForbidden)
        mockMvc.perform(
            post("/api/class-groups").with(teacherA).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Nowa","schoolYear":"2026/2027"}"""),
        )
            .andExpect(status().isForbidden)
        mockMvc.perform(
            put("/api/class-groups/$groupAId/teachers").with(teacherA).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"teacherIds":[]}"""),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @Order(7)
    fun `password change requires correct current password`() {
        mockMvc.perform(
            post("/api/auth/password").with(teacherA).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword":"zle-haslo","newPassword":"nowe-haslo-123"}"""),
        )
            .andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/api/auth/password").with(teacherA).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword":"$teacherAPassword","newPassword":"nowe-haslo-123"}"""),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"ania.teacher@example.com","password":"nowe-haslo-123"}"""),
        )
            .andExpect(status().isOk)
    }

    @Test
    @Order(8)
    fun `deactivated account cannot login or use existing session`() {
        // admin resetuje hasło Basi, potem ją dezaktywuje
        val reset = postJson("/api/users/$teacherBId/password-reset", "")
        val basiaPassword = reset["newPassword"].asText()

        mockMvc.perform(
            patch("/api/users/$teacherBId").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"active":false}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.active").value(false))

        mockMvc.perform(
            post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"basia.teacher@example.com","password":"$basiaPassword"}"""),
        )
            .andExpect(status().isUnauthorized)

        // istniejąca sesja też traci dostęp (CurrentUser odrzuca nieaktywnych)
        mockMvc.perform(get("/api/class-groups").with(teacherB))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @Order(9)
    fun `admin cannot deactivate own account`() {
        val adminId = getJson("/api/auth/me")["id"].asText()
        mockMvc.perform(
            patch("/api/users/$adminId").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"active":false}"""),
        )
            .andExpect(status().isBadRequest)
    }
}
