package ro.bankar.database

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
import ro.bankar.model.SNewUser
import ro.bankar.model.SPublicUser
import ro.bankar.model.SUser
import ro.bankar.sha256
import kotlin.time.Duration.Companion.days

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users) {
        /**
         * Creates a new user from a serialized user signup object
         */
        fun createUser(userData: SNewUser) = generateSalt().let { salt ->
            User.new {
                email = userData.email
                tag = userData.tag
                phone = userData.phone

                passwordHash = userData.password.sha256(salt)
                passwordSalt = salt

                firstName = userData.firstName.trim()
                middleName = userData.middleName?.trim()
                lastName = userData.lastName.trim()
                dateOfBirth = userData.dateOfBirth

                countryCode = userData.countryCode
                state = userData.state
                city = userData.city.trim()
                address = userData.address.trim()
            }
        }

        /**
         * Checks if user with e-mail, phone or number already exists
         */
        fun checkRegistered(data: SNewUser) =
            find { (Users.phone eq data.phone) or (Users.tag eq data.tag) or (Users.email eq data.email) }.firstOrNull()?.let {
                when {
                    it.phone == data.phone -> "phone"
                    it.tag == data.tag -> "tag"
                    it.email == data.email -> "email"
                    else -> throw RuntimeException("impossible")
                }
            }

        /**
         * Checks if tag is taken
         */
        fun isTagTaken(tag: String) = !find { Users.tag eq tag }.empty()

        /**
         * Checks if email is taken
         */
        fun isEmailTaken(email: String) = !find { Users.email eq email }.empty()

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
    var dateOfBirth by Users.dateOfBirth

    var countryCode by Users.countryCode
    var state by Users.state
    var city by Users.city
    var address by Users.address

    var joinDate by Users.joinDate
    var about by Users.about
    var avatar by Users.avatar

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
     * Remove existing session token, used for signing out
     */
    fun clearSession() {
        sessionToken = null
        sessionTokenExpiration = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    /**
     * Returns a serializable user
     */
    fun serializable() = SUser(email, tag, phone, firstName, middleName, lastName, dateOfBirth, countryCode, state,
        city, address, joinDate, about, avatar?.inputStream?.readBytes())
}

/**
 * Converts a list of Users to a list of serializable objects containing only the public information about the user.
 */
fun SizedIterable<User>.serializable() = map { SPublicUser(it.tag, it.firstName, it.middleName, it.lastName, it.countryCode, it.joinDate, it.about, it.avatar?.bytes) }

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
    val dateOfBirth = date("date_of_birth")

    val countryCode = varchar("country_code", 2)
    val state = varchar("state", 30)
    val city = varchar("city", 30)
    val address = varchar("address", 300)

    val joinDate = date("join_date").defaultExpression(CurrentDate)
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