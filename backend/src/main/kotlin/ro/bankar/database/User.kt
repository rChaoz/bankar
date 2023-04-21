package ro.bankar.database

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDate
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.or
import ro.bankar.generateSalt
import ro.bankar.generateToken
import ro.bankar.sha256
import java.lang.RuntimeException
import kotlin.time.Duration.Companion.days

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
data class SSignupData (
    val email: String,
    val tag: String,
    val phone: String,
    val password: String,

    val firstName: String,
    val middleName: String? = null,
    val lastName: String,

    val address1: String,
    val address2: String? = null,
) {
    companion object {
        // E-mail regex
        private val emailRegex = Regex("""^[\w-.]+@([\w-]+\.)+[\w-]{2,4}$""")
        // Phone number regex
        private val phoneRegex = Regex("""^\+\d{11}$""")
        // Tag regex
        private val tagRegex = Regex("""^[a-z0-9._-]{4,25}$""")
        // Regex for password: at least 8 characters, at least one uppercase letter, at least one lowercase letter, at least one digit, at least one special character
        private val passwordRegex = Regex("""^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\da-zA-Z]).{8,}$""")
        // Regex for a valid name
        private val nameRegex = Regex("""^[a-zA-Z- ]{2,20}$""")
    }

    /**
     * Validate all fields. In case of invalid field, returns the name of the field, as a String.
     */
    fun validate() = when {
        // Check login information
        !emailRegex.matches(email) -> "email"
        !phoneRegex.matches(phone) -> "phone"
        !tagRegex.matches(tag) -> "tag"
        !passwordRegex.matches(password) -> "password"
        // Check name
        !nameRegex.matches(firstName) -> "firstName"
        middleName != null && !nameRegex.matches(middleName) -> "middleName"
        !nameRegex.matches(lastName) -> "lastName"
        // Check address
        address1.length < 5 -> "address1"
        address2 != null && address2.length < 5 -> "address2"
        else -> null
    }
}

@Serializable
class SPublicUser(
    val tag: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val joinDate: LocalDate,
)

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users) {
        /**
         * Creates a new user from a serialized user signup object
         */
        fun createUser(userData: SSignupData) = generateSalt().let { salt ->
            User.new {
                email = userData.email
                tag = userData.tag
                phone = userData.phone

                passwordHash = userData.password.sha256(salt)
                passwordSalt = salt

                firstName = userData.firstName
                middleName = userData.middleName
                lastName = userData.lastName
                address1 = userData.address1
                address2 = userData.address2
            }
        }

        /**
         * Checks if user with e-mail, phone or number already exists
         */
        fun checkRegistered(data: SSignupData) =
            find { (Users.phone eq data.phone) or (Users.tag eq data.tag) or (Users.email eq data.email) }.firstOrNull()?.let {
                when {
                    it.phone == data.phone -> "phone"
                    it.tag == data.tag -> "tag"
                    it.email == data.email -> "email"
                    else -> throw RuntimeException("impossible")
                }
            }

        /**
         * Get a user by tag, e-mail or phone
         */
        fun findByAnything(id: String) = find { (Users.email eq id) or (Users.tag eq id) or (Users.phone eq id) }.firstOrNull()

        /**
         * Get a user by session token
         */
        fun findBySessionToken(token: String) = find { (Users.sessionToken eq token) and (Users.sessionTokenExpiration greater CurrentDateTime) }.firstOrNull()
    }

    var email by Users.email
    var tag by Users.tag
    var phone by Users.phone
    var disabled by Users.disabled

    private var passwordHash by Users.passwordHash
    private var passwordSalt by Users.passwordSalt
    private var sessionToken by Users.sessionToken
    private var sessionTokenExpiration by Users.sessionTokenExpiration

    var firstName by Users.firstName
    var middleName by Users.middleName
    var lastName by Users.lastName

    var joinDate by Users.joinDate
    var address1 by Users.address1
    var address2 by Users.address2

    val bankAccounts by BankAccount referrersOn BankAccounts.userID
    val assetAccounts by AssetAccount referrersOn AssetAccounts.user

    var friends by User.via(FriendPairs.sourceUser, FriendPairs.targetUser)
    var friendRequests by User.via(FriendRequests.sourceUser, FriendRequests.targetUser)

    val sendTransferRequests by TransferRequest referrersOn TransferRequests.sourceUser
    val receivedTransferRequests by TransferRequest referrersOn TransferRequests.targetUser

    /**
     * First step in authentication - verify that the password matches.
     * @return if password patches
     */
    fun verifyPassword(password: String) = password.sha256(passwordSalt).contentEquals(passwordHash)

    /**
     * Second step in authentication - generate session token for the user
     */
    fun createSessionToken() = generateToken().also {
        sessionToken = it
        sessionTokenExpiration = (Clock.System.now() + 90.days).toLocalDateTime(TimeZone.UTC)
    }

    /**
     * When any action is performed, the session token expiration is updated.
     */
    fun updateTokenExpiration() {
        sessionTokenExpiration = (Clock.System.now() + 90.days).toLocalDateTime(TimeZone.UTC)
    }

    /**
     * Returns a serializable user
     */
    fun serializable() = SUser(email, tag, phone, firstName, middleName, lastName, joinDate, address1, address2)
}

/**
 * Converts a list of Users to a list of serializable objects containing only the public information about the user.
 */
fun SizedIterable<User>.serializable() = map { SPublicUser(it.tag, it.firstName, it.middleName, it.lastName, it.joinDate) }

internal object Users : IntIdTable(columnName = "user_id") {
    val email = varchar("email", 50).uniqueIndex()
    val tag = varchar("tag", 25).uniqueIndex()
    val phone = varchar("phone", 15).uniqueIndex()
    val disabled = bool("disabled").default(false)

    val passwordHash = binary("password_hash", 256)
    val passwordSalt = binary("password_salt", 256)
    val sessionToken = varchar("session_token", 20).nullable()
    val sessionTokenExpiration = datetime("session_token_exp").defaultExpression(CurrentDateTime)

    val firstName = varchar("first_name", 20)
    val middleName = varchar("middle_name", 20).nullable()
    val lastName = varchar("last_name", 20)

    val joinDate = date("join_date").defaultExpression(CurrentDate)
    val address1 = varchar("address1", 200)
    val address2 = varchar("address2", 200).nullable()

    val about = varchar("about", 300).default("")
    val avatar = blob("avatar").nullable()
}

internal object FriendRequests : Table() {
    val sourceUser = reference("source_user_id", Users)
    val targetUser = reference("target_user_id", Users)
    override val primaryKey = PrimaryKey(FriendPairs.sourceUser, FriendPairs.targetUser)
}

internal object FriendPairs : Table() {
    val sourceUser = reference("source_user_id", Users)
    val targetUser = reference("target_user_id", Users)
    override val primaryKey = PrimaryKey(sourceUser, targetUser)
}