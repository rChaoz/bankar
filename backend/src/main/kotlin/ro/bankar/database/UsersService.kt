package ro.bankar.database

import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.*
import ro.bankar.generateSalt
import ro.bankar.generateToken
import ro.bankar.sha256
import kotlin.time.Duration.Companion.days

@Serializable
data class User(
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
) {
    val fullName = if (middleName == null) "$firstName $lastName" else "$firstName $middleName $lastName"
}

private object UsersTable : IntIdTable(columnName = "user_id") {
    val email = varchar("email", 50).uniqueIndex()
    val tag = varchar("tag", 30).uniqueIndex()
    val phone = varchar("phone", 15).uniqueIndex()

    val passwordHash = binary("password_hash", 256)
    val passwordSalt = binary("password_salt", 256)
    val sessionToken = varchar("session_token", 20).nullable()
    val sessionTokenExpiration = datetime("session_token_exp").defaultExpression(CurrentDateTime)

    val disabled = bool("disabled").default(false)
    val firstName = varchar("first_name", 20)
    val middleName = varchar("middle_name", 20).nullable()
    val lastName = varchar("last_name", 20)

    val joinDate = date("join_date").defaultExpression(CurrentDate)
    val address1 = varchar("address1", 200)
    val address2 = varchar("address2", 200).nullable()

    val about = varchar("about", 300).default("")
    val avatar = blob("avatar").nullable()
}

object UsersService : TableService(UsersTable) {
    /**
     * Create a new User in the database and signs him with e-mail and password
     * @see signInInitial
     */
    suspend fun createUser(user: User, password: String) = dbQuery {
        UsersTable.insert {
            it[email] = user.email
            it[tag] = user.tag
            it[phone] = user.phone
            val salt = generateSalt()
            it[passwordHash] = password.sha256(salt)
            it[passwordSalt] = salt
            it[firstName] = user.firstName
            if (user.middleName != null) it[middleName] = user.middleName
            it[lastName] = user.lastName
            it[address1] = user.address1
            if (user.address2 != null) it[address2] = user.address2
        }
        signInInitial(user.email, password)
    }

    /**
     * First step of the login - with username and password
     * @param id The e-mail/phone/tag of the user
     * @param password Plain password of the user
     * @return The ID of the user, used to complete the authentication with [signInFinal]
     */
    suspend fun signInInitial(id: String, password: String): Int? = dbQuery q@{
        with(UsersTable) {
            val row = select { (email eq id) or (tag eq id) or (phone eq id) }.singleOrNull() ?: return@q null
            val hash = password.sha256(row[passwordSalt])
            if (!hash.contentEquals(row[passwordHash])) return@q null
            return@q row[UsersTable.id].value
        }
    }

    /**
     * Final step of the login - creates a session token for the user to login with
     * @return The session token
     */
    suspend fun signInFinal(id: Int): String = dbQuery {
        val token = generateToken()
        UsersTable.update({ UsersTable.id eq id}) {
            it[sessionToken] = token
            CurrentDateTime
            it[sessionTokenExpiration] = dateTimeParam((Clock.System.now() + 90.days).toLocalDateTime(TimeZone.UTC))
        }
        token
    }

    /**
     * Gets the user with the given session token. Fails (returns null) if the session token is expired.
     */
    suspend fun getUser(token: String): User? = dbQuery {
        with(UsersTable) {
            val row = select { (sessionToken eq token) and (sessionTokenExpiration greater CurrentDateTime) }.singleOrNull() ?: return@dbQuery null
            User(
                row[email],
                row[tag],
                row[phone],
                row[disabled],
                row[firstName],
                row[middleName],
                row[lastName],
                row[joinDate],
                row[address1],
                row[address2],
            )
        }
    }
}