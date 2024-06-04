package ro.bankar

import org.jetbrains.exposed.sql.Table
import ro.bankar.banking.Currency
import java.security.MessageDigest
import java.security.SecureRandom

private val random = SecureRandom()
private val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')

/**
 * Generates 32 bytes (256 bits) of salt
 */
fun generateSalt() = ByteArray(32).also { random.nextBytes(it) }

/**
 * Calculates the SHA-256 of the String, using the given salt
 */
fun String.sha256(salt: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").apply {
    update(salt)
    update(this@sha256.toByteArray())
}.digest()

/**
 * Generates a 32-character long alphanumeric secure random token
 */
fun generateToken() = buildString(32) { repeat(32) { append(chars[random.nextInt(chars.size)]) } }

fun generateNumeric(numDigits: Int) = buildString(numDigits) { repeat(numDigits) { append(random.nextInt(10)) } }

/**
 * Column to store currency amount. Equivalent to `decimal(name, 20, 2)`.
 */
fun Table.amount(name: String) = decimal(name, 20, 2)

/**
 * Column to store currency.
 */
fun Table.currency(name: String) = enumerationByName<Currency>(name, 20)