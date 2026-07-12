package pl.kma.classevaluation.common

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

class NotFoundException(message: String) : RuntimeException(message)
class ConflictException(message: String) : RuntimeException(message)
class BadRequestException(message: String) : RuntimeException(message)
class TooManyRequestsException(message: String) : RuntimeException(message)

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun notFound(e: NotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message ?: "Nie znaleziono")

    @ExceptionHandler(ConflictException::class)
    fun conflict(e: ConflictException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "Konflikt")

    @ExceptionHandler(BadRequestException::class)
    fun badRequest(e: BadRequestException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.message ?: "Błędne żądanie")

    @ExceptionHandler(TooManyRequestsException::class)
    fun tooMany(e: TooManyRequestsException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.message ?: "Zbyt wiele prób")

    @ExceptionHandler(AuthenticationException::class)
    fun unauthorized(e: AuthenticationException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Błędny e-mail lub hasło")

    @ExceptionHandler(AccessDeniedException::class)
    fun forbidden(e: AccessDeniedException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Brak dostępu")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(e: MethodArgumentNotValidException): ProblemDetail {
        val detail = e.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail)
    }
}
