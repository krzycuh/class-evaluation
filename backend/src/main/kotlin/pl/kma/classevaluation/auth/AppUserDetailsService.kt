package pl.kma.classevaluation.auth

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.security.core.userdetails.User as SpringUser

@Service
class AppUserDetailsService(private val users: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = users.findByEmail(username)
            ?: throw UsernameNotFoundException("Nieznany użytkownik")
        return SpringUser(
            user.email,
            user.passwordHash,
            listOf(SimpleGrantedAuthority("ROLE_${user.role}")),
        )
    }
}

/** Dostęp do zalogowanego użytkownika (rekord z bazy) w serwisach. */
@Component
class CurrentUser(private val users: UserRepository) {

    fun get(): User {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw UsernameNotFoundException("Brak uwierzytelnienia")
        return users.findByEmail(auth.name)
            ?: throw UsernameNotFoundException("Nieznany użytkownik")
    }

    fun isAdmin(): Boolean = get().role == Role.ADMIN
}
