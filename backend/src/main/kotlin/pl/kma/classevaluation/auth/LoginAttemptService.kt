package pl.kma.classevaluation.auth

import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Prosty limit prób logowania per (e-mail + IP), w pamięci procesu.
 * Wystarczający dla aplikacji jednoinstancyjnej.
 */
@Service
class LoginAttemptService(
    private val maxAttempts: Int = 10,
    private val window: Duration = Duration.ofMinutes(15),
) {
    private val attempts = ConcurrentHashMap<String, MutableList<Instant>>()

    fun isBlocked(key: String): Boolean {
        val cutoff = Instant.now().minus(window)
        val list = attempts[key] ?: return false
        synchronized(list) {
            list.removeIf { it.isBefore(cutoff) }
            return list.size >= maxAttempts
        }
    }

    fun recordFailure(key: String) {
        val list = attempts.computeIfAbsent(key) { mutableListOf() }
        synchronized(list) { list.add(Instant.now()) }
    }

    fun reset(key: String) {
        attempts.remove(key)
    }
}
