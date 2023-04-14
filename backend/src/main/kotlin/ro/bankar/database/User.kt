package ro.bankar.database

import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.*
import ro.bankar.generateSalt
import ro.bankar.generateToken
import ro.bankar.sha256
import kotlin.time.Duration.Companion.days

@Serializable
class SUser(
    val email: String,
    val tag: String,
    val phone: String,

    val disabled: Boolean,
    val firstName: String,
    val middleName: String?,
    val lastName: String,

    val joinDate: LocalDate,
    val address1: String,
    val address2: String?,
)

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users) {
        /**
         * Creates a new user from a serialized user object, and a given password.
         */
        fun createUser(sUser: SUser, password: String) = generateSalt().let { salt ->
            User.new {
                email = sUser.email
                tag = sUser.tag
                phone = sUser.phone

                passwordHash = password.sha256(salt)
                passwordSalt = salt

                firstName = sUser.firstName
                middleName = sUser.middleName
                lastName = sUser.lastName
                address1 = sUser.address1
                address2 = sUser.address2
            }
        }

        /**
         * Get a user by tag, e-mail or phone
         */
        fun findByAnything(id: String) = find { (Users.email eq id) or (Users.tag eq id) or (Users.phone eq id) }
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

    /**
     * First step in authentication - verify that the password matches.
     * @return if password patches
     */
    fun verifyPassword(password: String) = password.sha256(passwordSalt).contentEquals(passwordHash)

    /**
     * Second step in authentication - generate session token for the user
     */
    fun newSessionToken() = generateToken().also {
        sessionToken = it
        sessionTokenExpiration = (Clock.System.now() + 90.days).toLocalDateTime(TimeZone.UTC)
    }

    /**
     * Returns a serializable user
     */
    fun serializable() = SUser(email, tag, phone, disabled, firstName, middleName, lastName, joinDate, address1, address2)
}

internal object Users : IntIdTable(columnName = "user_id") {
    val email = varchar("email", 50).uniqueIndex()
    val tag = varchar("tag", 30).uniqueIndex()
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