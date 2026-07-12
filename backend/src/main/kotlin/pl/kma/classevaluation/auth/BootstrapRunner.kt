package pl.kma.classevaluation.auth

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.kma.classevaluation.assessments.AssessmentPeriod
import pl.kma.classevaluation.assessments.AssessmentPeriodRepository
import pl.kma.classevaluation.assessments.PeriodStatus
import pl.kma.classevaluation.students.ClassGroup
import pl.kma.classevaluation.students.ClassGroupRepository
import java.security.SecureRandom
import java.time.LocalDate

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val admin: Admin = Admin(),
    val bootstrap: Bootstrap = Bootstrap(),
) {
    data class Admin(
        val email: String = "admin@example.com",
        val password: String = "",
        val displayName: String = "Administrator",
    )

    data class Bootstrap(val classGroupName: String = "Moja grupa")
}

/**
 * Przy pierwszym uruchomieniu (pusta tabela users) zakłada konto administratora,
 * grupę przedszkolną i dwa semestry bieżącego roku szkolnego.
 */
@Component
@EnableConfigurationProperties(AppProperties::class)
class BootstrapRunner(
    private val users: UserRepository,
    private val classGroups: ClassGroupRepository,
    private val periods: AssessmentPeriodRepository,
    private val passwordEncoder: PasswordEncoder,
    private val props: AppProperties,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (users.count() > 0) return

        val password = props.admin.password.ifBlank { generatePassword() }
        if (props.admin.password.isBlank()) {
            log.warn("Nie ustawiono APP_ADMIN_PASSWORD — wygenerowano hasło administratora: {}", password)
            log.warn("Zaloguj się jako {} i zapisz to hasło. Ustaw APP_ADMIN_PASSWORD, aby użyć własnego.", props.admin.email)
        }

        val admin = users.save(
            User(
                email = props.admin.email.lowercase(),
                passwordHash = passwordEncoder.encode(password),
                displayName = props.admin.displayName,
                role = Role.ADMIN,
            ),
        )

        val today = LocalDate.now()
        val startYear = if (today.monthValue >= 9) today.year else today.year - 1
        val schoolYear = "$startYear/${startYear + 1}"

        classGroups.save(
            ClassGroup(
                name = props.bootstrap.classGroupName,
                schoolYear = schoolYear,
                ownerUserId = admin.id!!,
            ),
        )

        periods.save(
            AssessmentPeriod(
                schoolYear = schoolYear,
                name = "Semestr I",
                startsOn = LocalDate.of(startYear, 9, 1),
                endsOn = LocalDate.of(startYear + 1, 1, 31),
                status = PeriodStatus.OPEN,
            ),
        )
        periods.save(
            AssessmentPeriod(
                schoolYear = schoolYear,
                name = "Semestr II",
                startsOn = LocalDate.of(startYear + 1, 2, 1),
                endsOn = LocalDate.of(startYear + 1, 6, 30),
                status = PeriodStatus.OPEN,
            ),
        )

        log.info("Utworzono konto administratora {}, grupę '{}' i semestry roku {}", admin.email, props.bootstrap.classGroupName, schoolYear)
    }

    private fun generatePassword(): String {
        val chars = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        return (1..14).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}
