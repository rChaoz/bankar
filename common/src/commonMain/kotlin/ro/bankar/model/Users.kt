package ro.bankar.model

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
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
    val address: String,
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
    val dateOfBirth: LocalDate,

    val countryCode: String,
    val state: String,
    val city: String,
    val address: String,
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

        // Valid age range (in years)
        val ageRange = 13..110

        // Valid lengths for city & address
        val cityLengthRange = 2..30
        val addressLengthRange = 5..300
    }

    /**
     * Validate all fields. In case of invalid field, returns the name of the field, as a String.
     * Otherwise, if all fields are valid, return `null`.
     */
    fun validate(countryData: SCountries): String? {
        val country = countryData.find { it.code == countryCode } ?: return "country"
        if (state !in country.states) return "state"

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        return when {
            // Check login information
            !emailRegex.matches(email) -> "email"
            !phoneRegex.matches(phone) -> "phone"
            !tagRegex.matches(tag) -> "tag"
            !passwordRegex.matches(password) -> "password"
            // Check name & date of birth
            !nameRegex.matches(firstName) -> "firstName"
            middleName != null && !nameRegex.matches(middleName.trim()) -> "middleName"
            !nameRegex.matches(lastName) -> "lastName"
            dateOfBirth !in (today - DatePeriod(ageRange.last))..(today - DatePeriod(ageRange.first)) -> "dateOfBirth"
            // Check address
            city.length !in cityLengthRange -> "city"
            address.length !in addressLengthRange -> "address"
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