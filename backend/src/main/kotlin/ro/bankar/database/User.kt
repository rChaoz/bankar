package ro.bankar.database

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDate
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.update
import ro.bankar.generateSalt
import ro.bankar.generateToken
import ro.bankar.model.SDefaultBankAccount
import ro.bankar.model.SDirection
import ro.bankar.model.SFriend
import ro.bankar.model.SFriendRequest
import ro.bankar.model.SNewUser
import ro.bankar.model.SPublicUser
import ro.bankar.model.SUser
import ro.bankar.sha256
import ro.bankar.util.nowUTC
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
         * Gets a user by tag
         */
        fun findByTag(tag: String) = find { Users.tag eq tag }.singleOrNull()

        /**
         * Get a user by tag, e-mail or phone
         */
        fun findByAnything(id: String) = find { (Users.email eq id) or (Users.tag eq id) or (Users.phone eq id) }.firstOrNull()

        /**
         * Get a user by session token
         */
        fun findBySessionToken(token: String) =
            find { (Users.sessionToken eq token) and (Users.sessionTokenExpiration greater Clock.System.nowUTC()) }.firstOrNull()
    }

    var email by Users.email
    var tag by Users.tag
    var phone by Users.phone
    var disabled by Users.disabled
    var defaultAccount by BankAccount optionalReferencedOn Users.defaultAccount
    var alwaysUseDefaultAccount by Users.alwaysUseDefaultAccount
    var notificationToken by Users.notificationToken

    private var passwordHash by Users.passwordHash
    private var passwordSalt by Users.passwordSalt
    private var sessionToken by Users.sessionToken
    private var sessionTokenExpiration by Users.sessionTokenExpiration

    var firstName by Users.firstName
    var middleName by Users.middleName
    var lastName by Users.lastName
    val fullName
        get() = "$firstName ${if (middleName != null) "$middleName " else ""}$lastName"
    var dateOfBirth by Users.dateOfBirth

    var countryCode by Users.countryCode
    var state by Users.state
    var city by Users.city
    var address by Users.address

    var joinDate by Users.joinDate
    var about by Users.about
    private var _avatar by Users.avatar
    private var _avatarSet = false
    var avatar: ByteArray? = null
        @Synchronized get() = if (_avatarSet) field else _avatar?.inputStream?.readBytes().also { field = it; _avatarSet = true }
        @Synchronized set(value) {
            field = value
            // Required because, on set, Exposed will compare the new value with old to test whether it should update
            // (which fails due to input stream not resettable, if the data has already been read)
            _avatar = null
            _avatar = value?.let { ExposedBlob(it) }
            _avatarSet = true
        }

    val bankAccounts by BankAccount referrersOn BankAccounts.userID
    val bankAccountIds get() = bankAccounts.map(BankAccount::id)
    val assetAccounts by AssetAccount referrersOn AssetAccounts.user

    var friends by User.via(FriendPairs.sourceUser, FriendPairs.targetUser)
    var friendRequests by User.via(FriendRequests.targetUser, FriendRequests.sourceUser)

    val sentTransferRequests by TransferRequest referrersOn TransferRequests.sourceUser
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
     * Update data from given object
     */
    fun update(data: SNewUser) {
        email = data.email
        // No tag/phone update, those are separate
        firstName = data.firstName.trim()
        middleName = data.middleName?.trim()
        lastName = data.lastName.trim()
        dateOfBirth = data.dateOfBirth

        countryCode = data.countryCode
        state = data.state
        city = data.city.trim()
        address = data.address.trim()
    }

    /**
     * Sign out and disable this account
     */
    fun disable() {
        clearSession()
        disabled = true
    }

    /**
     * Adds an incoming friends request for this user, from target user
     */
    fun addFriendRequest(from: User) = FriendRequests.insert {
        it[sourceUser] = from.id
        it[targetUser] = this@User.id
    }

    /**
     * Adds another user as a friend
     */
    fun addFriend(other: User) = FriendPairs.insert {
        it[sourceUser] = this@User.id
        it[targetUser] = other.id
    }

    /**
     * Removes user from friend list
     */
    fun removeFriend(other: User) {
        friends = SizedCollection(friends.filter { it.id != other.id })
    }

    fun hasFriend(other: User) = friends.any { it.id == other.id }

    /**
     * Send a message to a user
     */
    fun sendMessage(recipient: User, message: String) {
        UserMessage.create(this, recipient, message)
    }

    /**
     * Returns the conversation with another user
     */
    fun getConversationWith(otherUser: User) = UserMessage.getConversationBetween(this, otherUser)

    /**
     * Returns a serializable user
     */
    fun serializable() = SUser(
        email, tag, phone, firstName, middleName, lastName, dateOfBirth, countryCode, state,
        city, address, joinDate, about, avatar
    )

    /**
     * Returns this user's public data as serializable
     */
    fun publicSerializable(isFriend: Boolean) = SPublicUser(
        tag,
        firstName,
        middleName,
        lastName,
        countryCode,
        joinDate,
        about,
        avatar,
        isFriend
    )

    /**
     * Converts this user's public data into a serializable friend request
     */
    fun friendRequestSerializable(direction: SDirection) = SFriendRequest(
        tag,
        firstName,
        middleName,
        lastName,
        countryCode,
        joinDate,
        about,
        avatar,
        direction
    )

    /**
     * Converts this user's public data into a serializable object containing public and friend-related information
     */
    fun friendSerializable(user: User): SFriend {
        val lastOpened = FriendPairs.getLastOpenedConversation(user, this)
        val unreadCount = UserMessage.getConversationBetweenSince(user, this, lastOpened).count().toInt()
        val lastMessage = UserMessage.getConversationBetween(user, this).firstOrNull()
        return SFriend(
            tag,
            firstName,
            middleName,
            lastName,
            countryCode,
            joinDate,
            about,
            avatar,
            lastMessage?.serializable(user),
            unreadCount
        )
    }

    /**
     * Obtains the default account information as a serializable object
     */
    fun defaultAccountSerializable() = SDefaultBankAccount(defaultAccount?.id?.value, alwaysUseDefaultAccount)

    /**
     * Sets the default account information
     */
    fun setDefaultAccount(account: BankAccount?, alwaysUse: Boolean) {
        defaultAccount = account
        alwaysUseDefaultAccount = alwaysUse
    }

    fun updateLastOpenedConversationWith(otherUser: User) = FriendPairs.updateLastOpenedConversation(this, otherUser)
}

/**
 * Converts an user's friend list to a serializable list.
 */
fun SizedIterable<User>.friendsSerializable(user: User) = map { it.friendSerializable(user) }

internal object Users : IntIdTable(columnName = "user_id") {
    val email = varchar("email", 50).uniqueIndex()
    val tag = varchar("tag", 25).uniqueIndex()
    val phone = varchar("phone", 15).uniqueIndex()
    val disabled = bool("disabled").default(false)

    val defaultAccount = reference("default_account", BankAccounts, onDelete = ReferenceOption.SET_NULL).nullable()
    val alwaysUseDefaultAccount = bool("always_use_default_account").default(false)
    val notificationToken = varchar("notification_token", 200).nullable()

    val passwordHash = binary("password_hash", 256)
    val passwordSalt = binary("password_salt", 256)
    val sessionToken = varchar("session_token", 32).nullable()
    val sessionTokenExpiration = datetime("session_token_exp").clientDefault { Clock.System.nowUTC() }

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
    override val primaryKey = PrimaryKey(sourceUser, targetUser)
}

internal object FriendPairs : Table() {
    val sourceUser = reference("source_user_id", Users)
    val targetUser = reference("target_user_id", Users)
    val lastOpenedConversation = timestamp("last_opened_conversation").defaultExpression(CurrentTimestamp())
    override val primaryKey = PrimaryKey(sourceUser, targetUser)

    fun getLastOpenedConversation(user: User, otherUser: User) =
        select { (sourceUser eq user.id) and (targetUser eq otherUser.id) }.first()[lastOpenedConversation]

    fun updateLastOpenedConversation(user: User, otherUser: User) =
        update(where = { (sourceUser eq user.id) and (targetUser eq otherUser.id) }) { it[lastOpenedConversation] = Clock.System.now() }
}