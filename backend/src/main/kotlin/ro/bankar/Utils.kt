package ro.bankar

import kotlinx.serialization.Serializable
import org.h2.security.SHA256
import org.jetbrains.exposed.sql.Table
import java.security.SecureRandom

private val RANDOM = SecureRandom()
private val CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9')

/**
 * Used to respond with just a status
 * @param status "success", if request succeeded, or an error code
 */
@Serializable
data class StatusResponse(val status: String) {
    companion object {
        val Success = StatusResponse("success")
    }
}

/**
 * Used to respond with a status, that defaults to "invalid", and a parameter name, to indicate that the request contained an invalid parameter value.
 * @param status defaults to "invalid"
 * @param param request parameter that is invalid
 */
@Serializable
data class InvalidParamResponse(val status: String = "invalid", val param: String)

/**
 * Used to indicate that a resource was not found
 */
@Serializable
data class NotFoundResponse(val status: String = "not_found", val resource: String)

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

fun generateNumeric(numDigits: Int) = buildString(numDigits) { repeat(numDigits) { append(RANDOM.nextInt(10)) } }

/**
 * Column to store currency amount. Equivalent to `decimal(name, 20, 2)`.
 */
fun Table.amount(name: String) = decimal(name, 20, 2)

/**
 * Column to store currency. Equivalent to `varchar(name, 4)`.
 */
fun Table.currency(name: String) = varchar(name, 4)