package pl.kma.classevaluation.auth

import java.security.SecureRandom

/** Generator haseł startowych (bez znaków mylących: l/I/1, O/0). */
object Passwords {
    private const val CHARS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun generate(length: Int = 14): String =
        (1..length).map { CHARS[random.nextInt(CHARS.length)] }.joinToString("")
}
