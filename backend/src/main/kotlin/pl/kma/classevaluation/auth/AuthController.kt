package pl.kma.classevaluation.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.validation.annotation.Validated
import pl.kma.classevaluation.common.BadRequestException
import pl.kma.classevaluation.common.TooManyRequestsException
import java.util.UUID

data class LoginRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val password: String,
)

data class ChangePasswordRequest(
    @field:NotBlank val currentPassword: String,
    @field:NotBlank @field:Size(min = 8, message = "hasło musi mieć co najmniej 8 znaków") val newPassword: String,
)

data class UserDto(val id: UUID, val email: String, val displayName: String, val role: Role, val active: Boolean)

internal fun User.toDto() = UserDto(id!!, email, displayName, role, active)

@RestController
@RequestMapping("/api/auth")
@Validated
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val securityContextRepository: SecurityContextRepository,
    private val loginAttempts: LoginAttemptService,
    private val currentUser: CurrentUser,
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @PostMapping("/login")
    fun login(
        @RequestBody @Validated body: LoginRequest,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): UserDto {
        val key = "${body.email.lowercase()}|${request.remoteAddr}"
        if (loginAttempts.isBlocked(key)) {
            throw TooManyRequestsException("Zbyt wiele nieudanych prób logowania. Spróbuj za kilkanaście minut.")
        }

        val authentication = try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.email.trim(), body.password),
            )
        } catch (e: BadCredentialsException) {
            loginAttempts.recordFailure(key)
            throw e
        }
        loginAttempts.reset(key)

        // ochrona przed session fixation + zapis kontekstu do sesji
        request.getSession(true)
        request.changeSessionId()
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, request, response)

        return currentUser.get().toDto()
    }

    @GetMapping("/me")
    fun me(): UserDto = currentUser.get().toDto()

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(@RequestBody @Validated body: ChangePasswordRequest) {
        val user = currentUser.get()
        if (!passwordEncoder.matches(body.currentPassword, user.passwordHash)) {
            throw BadRequestException("Obecne hasło jest nieprawidłowe")
        }
        users.save(user.copy(passwordHash = passwordEncoder.encode(body.newPassword)))
    }
}
