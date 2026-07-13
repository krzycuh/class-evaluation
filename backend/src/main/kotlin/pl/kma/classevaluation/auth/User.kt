package pl.kma.classevaluation.auth

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import java.time.Instant
import java.util.UUID

enum class Role { TEACHER, ADMIN }

@Table("users")
data class User(
    @Id val id: UUID? = null,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val role: Role,
    val active: Boolean = true,
    val mustChangePassword: Boolean = false,
    val createdAt: Instant = Instant.now(),
)

interface UserRepository : CrudRepository<User, UUID> {
    @Query("SELECT * FROM users WHERE lower(email) = lower(:email)")
    fun findByEmail(email: String): User?

    @Query("SELECT * FROM users ORDER BY display_name, email")
    fun findAllOrdered(): List<User>
}
