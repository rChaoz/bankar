package ro.bankar

import org.h2.security.SHA256
import java.security.SecureRandom

private val RANDOM = SecureRandom()
private val CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9')

/**
 * Generates 32 bytes (256 bits) of salt
 */
fun generateSalt() = ByteArray(32).also { RANDOM.nextBytes(it) }

/**
 * Calculates the SHA-256 of the String, using the given salt
 */
fun String.sha256(salt: ByteArray): ByteArray = SHA256.getHashWithSalt(this.toByteArray(), salt)

/**
 * Generates a 20-character long alphanumeric secure random token
 */
fun generateToken() = buildString(20) { repeat(20) { append(CHARS[RANDOM.nextInt(CHARS.size)]) } }