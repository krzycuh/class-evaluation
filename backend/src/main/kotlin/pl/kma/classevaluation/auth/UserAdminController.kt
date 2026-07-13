package pl.kma.classevaluation.auth

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pl.kma.classevaluation.common.BadRequestException
import pl.kma.classevaluation.common.ConflictException
import pl.kma.classevaluation.common.NotFoundException
import java.util.UUID

data class CreateTeacherRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val displayName: String,
)

data class UpdateUserRequest(
    val displayName: String? = null,
    val active: Boolean? = null,
)

/** Odpowiedź z hasłem startowym — pokazywane adminowi jednorazowo, nigdzie nie zapisywane. */
data class CreatedTeacherResponse(val user: UserDto, val initialPassword: String)

data class PasswordResetResponse(val newPassword: String)

/** Zarządzanie kontami nauczycielek — dostęp tylko dla ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/users")
@Validated
class UserAdminController(
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val currentUser: CurrentUser,
) {

    @GetMapping
    fun list(): List<UserDto> = users.findAllOrdered().map { it.toDto() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun create(@RequestBody @Valid body: CreateTeacherRequest): CreatedTeacherResponse {
        val email = body.email.trim().lowercase()
        if (users.findByEmail(email) != null) throw ConflictException("Konto z tym adresem e-mail już istnieje")
        val password = Passwords.generate()
        val user = users.save(
            User(
                email = email,
                passwordHash = passwordEncoder.encode(password),
                displayName = body.displayName.trim(),
                role = Role.TEACHER,
            ),
        )
        return CreatedTeacherResponse(user.toDto(), password)
    }

    @PatchMapping("/{id}")
    @Transactional
    fun update(@PathVariable id: UUID, @RequestBody body: UpdateUserRequest): UserDto {
        val user = users.findById(id).orElseThrow { NotFoundException("Nie znaleziono użytkownika") }
        if (body.active == false && user.id == currentUser.get().id) {
            throw BadRequestException("Nie można dezaktywować własnego konta")
        }
        return users.save(
            user.copy(
                displayName = body.displayName?.trim()?.takeIf { it.isNotEmpty() } ?: user.displayName,
                active = body.active ?: user.active,
            ),
        ).toDto()
    }

    @PostMapping("/{id}/password-reset")
    @Transactional
    fun resetPassword(@PathVariable id: UUID): PasswordResetResponse {
        val user = users.findById(id).orElseThrow { NotFoundException("Nie znaleziono użytkownika") }
        val password = Passwords.generate()
        users.save(user.copy(passwordHash = passwordEncoder.encode(password)))
        return PasswordResetResponse(password)
    }
}
