package ro.bankar.model

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.serialization.Serializable
import ro.bankar.banking.SCountries
import ro.bankar.util.todayHere

/**
 * Object containing regexes & rules to validate user information
 */
object SUserValidation {
    // Tag
    val tagLengthRange = 4..25
    val tagRegex = Regex("""^[a-z][a-z0-9._-]{${tagLengthRange.first - 1},${tagLengthRange.last - 1}}$""")

    // Name
    val nameRegex = Regex("""^[\p{L}- ]{2,20}$""")

    // E-mail
    val emailRegex = Regex("""^[\w-.]+@([\w-]+\.)+[\w-]{2,4}$""")

    // Phone number
    val phoneRegex = Regex("""^\+\d{9,13}$""")

    // Password: between 8 and 32 characters (inclusive), at least one uppercase letter, at least one
    // lowercase letter, at least one digit, at least one special character
    val passwordRegex = Regex("""^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\da-zA-Z]).{8,32}$""")

    // Valid age range (in years)
    val ageRange = 13..110

    // Valid lengths for city & address
    val cityLengthRange = 2..30
    val addressLengthRange = 5..300

    // Max length for about field
    const val aboutMaxLength = 300

    // Avatar image size
    const val avatarSize = 512
}

/**
 * Common data shared between all serializable User classes
 */
sealed class SCommonUserBase {
    abstract val tag: String

    abstract val firstName: String
    abstract val middleName: String?
    abstract val lastName: String

    abstract val countryCode: String

    open fun validate(data: SCountries) = with(SUserValidation) {
        when {
            !tagRegex.matches(tag) -> "tag"

            !nameRegex.matches(firstName) -> "firstName"
            middleName.let { it != null && !nameRegex.matches(it) } -> "middleName"
            !nameRegex.matches(lastName) -> "lastName"

            data.none { it.code == countryCode } -> "countryCode"
            else -> null
        }
    }

    val fullName get() = "$firstName ${middleName?.let { "$middleName " } ?: ""}$lastName"
}

/**
 * Common data shared between SNewUser and SUser
 */
sealed class SUserBase : SCommonUserBase() {
    abstract val email: String
    abstract val phone: String

    abstract val dateOfBirth: LocalDate

    abstract val state: String
    abstract val city: String
    abstract val address: String

    override fun validate(data: SCountries) = super.validate(data) ?: with(SUserValidation) {
        val today = Clock.System.todayHere()
        when {
            !email.matches(emailRegex) -> "email"
            !phone.matches(phoneRegex) -> "phone"
            dateOfBirth !in (today - DatePeriod(ageRange.last))..(today - DatePeriod(ageRange.first)) -> "dateOfBirth"
            state !in data.first { it.code == countryCode }.states -> "state"
            city.length !in cityLengthRange -> "city"
            address.length !in addressLengthRange -> "address"
            else -> null
        }
    }
}

/**
 * Data sent by the user when signing up or when updating their profile
 */
@Serializable
class SNewUser(
    override val email: String,
    override val tag: String,
    override val phone: String,
    val password: String,

    override val firstName: String,
    override val middleName: String? = null,
    override val lastName: String,
    override val dateOfBirth: LocalDate,

    override val countryCode: String,
    override val state: String,
    override val city: String,
    override val address: String,
) : SUserBase() {
    override fun validate(data: SCountries) = super.validate(data) ?: if (!SUserValidation.passwordRegex.matches(password)) "password" else null
}

/**
 * Profile data that the user can see on his own profile page
 */
@Serializable
class SUser(
    override val email: String,
    override val tag: String,
    override val phone: String,

    override val firstName: String,
    override val middleName: String?,
    override val lastName: String,
    override val dateOfBirth: LocalDate,

    override val countryCode: String,
    override val state: String,
    override val city: String,
    override val address: String,

    val joinDate: LocalDate,
    val about: String,
    /**
     * JPEG compressed image
     */
    val avatar: ByteArray?
) : SUserBase() // No validate because this type is never sent by the client

/**
 * Verify that the given data is a valid JPEG image with the correct size (as specified by [SUserValidation.avatarSize])
 */
expect fun validateImage(imageData: ByteArray): Boolean

/**
 * Data sent by the client to update their about information/profile picture
 */
@Serializable
class SUserProfileUpdate(
    val about: String?,
    val avatar: ByteArray?,
) {
    fun validate() = when {
        about != null && about.length > SUserValidation.aboutMaxLength -> "about"
        avatar != null && !validateImage(avatar) -> "avatar"
        else -> null
    }
}

@Serializable
sealed class SPublicUserBase: SCommonUserBase() {
    abstract val joinDate: LocalDate
    abstract val about: String
    /**
     * JPEG compressed image
     */
    abstract val avatar: ByteArray?

    // No validate because this type is never sent by the client
}

/**
 * Data a user receives about other non-friended users, such as users that have sent transfer requests
 */
@Serializable
class SPublicUser(
    override val tag: String,

    override val firstName: String,
    override val middleName: String?,
    override val lastName: String,

    override val countryCode: String,
    override val joinDate: LocalDate,
    override val about: String,
    override val avatar: ByteArray?,

    // True if this user is in the user's friend list
    val isFriend: Boolean
) : SPublicUserBase()

@Serializable
class SFriendRequest(
    override val tag: String,

    override val firstName: String,
    override val middleName: String?,
    override val lastName: String,

    override val countryCode: String,
    override val joinDate: LocalDate,
    override val about: String,
    override val avatar: ByteArray?,

    val direction: SDirection
): SPublicUserBase()

/**
 * Data a user receives about a friend
 */
@Serializable
class SFriend(
    override val tag: String,

    override val firstName: String,
    override val middleName: String?,
    override val lastName: String,

    override val countryCode: String,
    override val joinDate: LocalDate,
    override val about: String,
    override val avatar: ByteArray?,

    // Information about chatting, to provide message count badges and more
    val lastMessage: SUserMessage?,
    val unreadMessageCount: Int,
): SPublicUserBase()

/**
 * Data sent by client to login
 */
@Serializable
data class SInitialLoginData(
    val id: String,
    val password: String,
)

@Serializable
data class SPasswordData(
    val password: String
)

/**
 * Data sent by client to complete login/signup operation
 */
@Serializable
data class SSMSCodeData(
    val smsCode: String,
)