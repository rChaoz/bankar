package ro.bankar.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import ro.bankar.amount

class AssetAccount(id: EntityID<Int>) : IntEntity(id) {
    @Serializable
    enum class Type { CRYPTO, STOCKS }

    companion object : IntEntityClass<AssetAccount>(AssetAccounts)

    var user by User referencedOn AssetAccounts.user
    var type by AssetAccounts.type
    var balance by AssetAccounts.balance
    var asset by AssetAccounts.asset
}

internal object AssetAccounts : IntIdTable() {

    val user = reference("user_id", Users)
    val type = enumeration<AssetAccount.Type>("type")
    val balance = amount("balance")
    val asset = varchar("asset", 7)
}