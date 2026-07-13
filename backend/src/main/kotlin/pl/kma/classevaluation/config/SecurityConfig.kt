package pl.kma.classevaluation.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.csrf.CsrfTokenRequestHandler
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
import org.springframework.web.filter.OncePerRequestFilter
import java.util.function.Supplier

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder =
        PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun securityContextRepository(): SecurityContextRepository =
        HttpSessionSecurityContextRepository()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf {
                it.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                it.csrfTokenRequestHandler(SpaCsrfTokenRequestHandler())
            }
            .addFilterAfter(CsrfCookieFilter(), BasicAuthenticationFilter::class.java)
            .authorizeHttpRequests {
                it.requestMatchers("/api/auth/login").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/development-areas/**", "/api/skills/**", "/api/periods/**", "/api/event-categories/**").hasRole("ADMIN")
                it.requestMatchers(HttpMethod.PATCH, "/api/development-areas/**", "/api/skills/**", "/api/periods/**", "/api/event-categories/**").hasRole("ADMIN")
                // zarządzanie kontami i grupami — tylko admin; POST /api/class-groups/{id}/students zostaje dla nauczycielek
                it.requestMatchers("/api/users/**").hasRole("ADMIN")
                it.requestMatchers(HttpMethod.POST, "/api/class-groups").hasRole("ADMIN")
                it.requestMatchers(HttpMethod.PATCH, "/api/class-groups/*").hasRole("ADMIN")
                it.requestMatchers(HttpMethod.PUT, "/api/class-groups/*/teachers").hasRole("ADMIN")
                it.requestMatchers("/api/**").authenticated()
                it.anyRequest().denyAll()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .logout {
                it.logoutUrl("/api/auth/logout")
                it.logoutSuccessHandler(HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                it.invalidateHttpSession(true)
                it.deleteCookies("JSESSIONID")
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }

        return http.build()
    }
}

/**
 * Rekomendowana przez dokumentację Spring Security konfiguracja CSRF dla SPA:
 * token w cookie XSRF-TOKEN (czytelnym dla JS), odsyłany nagłówkiem X-XSRF-TOKEN.
 */
class SpaCsrfTokenRequestHandler : CsrfTokenRequestHandler {
    private val plain = CsrfTokenRequestAttributeHandler()
    private val xor = XorCsrfTokenRequestAttributeHandler()

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, csrfToken: Supplier<CsrfToken>) {
        xor.handle(request, response, csrfToken)
        csrfToken.get()
    }

    override fun resolveCsrfTokenValue(request: HttpServletRequest, csrfToken: CsrfToken): String? {
        val headerValue = request.getHeader(csrfToken.headerName)
        return (if (headerValue != null) plain else xor).resolveCsrfTokenValue(request, csrfToken)
    }
}

/** Wymusza zapis cookie XSRF-TOKEN przy każdej odpowiedzi. */
class CsrfCookieFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val csrfToken = request.getAttribute(CsrfToken::class.java.name) as CsrfToken?
        csrfToken?.token
        filterChain.doFilter(request, response)
    }
}
