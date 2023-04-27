package ro.bankar.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
class SUser(
    val email: String,
    val tag: String,
    val phone: String,

    val firstName: String,
    val middleName: String?,
    val lastName: String,

    val joinDate: LocalDate,
    val address1: String,
    val address2: String?,
)

/**
 * Data sent by the client when signing up.
 */
@Serializable
data class SNewUser (
    val email: String,
    val tag: String,
    val phone: String,
    val password: String,

    val firstName: String,
    val middleName: String? = null,
    val lastName: String,

    val countryCode: String,
    val state: String,
    val city: String,
    val address1: String,
    val address2: String? = null,
) {
    companion object {
        // E-mail regex
        val emailRegex = Regex("""^[\w-.]+@([\w-]+\.)+[\w-]{2,4}$""")
        // Phone number regex
        val phoneRegex = Regex("""^\+\d{8,11}$""")
        // Tag regex
        val tagLengthRange = 4..25
        val tagRegex = Regex("""^[a-z][a-z0-9._-]{${tagLengthRange.first - 1},${tagLengthRange.last - 1}}$""")

        // Regex for password: between 8 and 32 characters (inclusive), at least one uppercase letter, at least one
        // lowercase letter, at least one digit, at least one special character
        val passwordRegex = Regex("""^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\da-zA-Z]).{8,32}$""")
        // Regex for a valid name
        val nameRegex = Regex("""^[\p{L}- ]{2,20}$""")
    }

    /**
     * Validate all fields. In case of invalid field, returns the name of the field, as a String.
     * Otherwise, if all fields are valid, return `null`.
     */
    fun validate(countryData: SCountries): String? {
        val country = countryData.find { it.code == countryCode } ?: return "country"
        if (state !in country.states) return "state"

        return when {
            // Check login information
            !emailRegex.matches(email) -> "email"
            !phoneRegex.matches(phone) -> "phone"
            !tagRegex.matches(tag) -> "tag"
            !passwordRegex.matches(password) -> "password"
            // Check name
            !nameRegex.matches(firstName.trim()) -> "firstName"
            middleName != null && !nameRegex.matches(middleName.trim()) -> "middleName"
            !nameRegex.matches(lastName.trim()) -> "lastName"
            // Check address
            city.trim().length !in 2..30 -> "city"
            address1.trim().length !in 5..200 -> "address1"
            address2 != null && address2.trim().length !in 5..200 -> "address2"
            else -> null
        }
    }
}

@Serializable
class SInitialLoginData(
    val id: String,
    val password: String,
)

@Serializable
data class SFinalLoginData(
    val smsCode: String,
)