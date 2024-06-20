package ro.bankar

import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import ro.bankar.model.SNewUser
import ro.bankar.plugins.init

internal fun resetDatabase() {
    if (TransactionManager.defaultDatabase != null) transaction { exec("DROP ALL OBJECTS") }
    Database.init()
}

internal val testUserA = SNewUser(
    email = "sample@email.com",
    tag = "test-user",
    phone = "+40123456789",
    password = "Str0ngP@ss",
    firstName = "Test",
    middleName = null,
    lastName = "User",
    dateOfBirth = LocalDate(2002, 2, 3),
    countryCode = "RO",
    state = "Bucuresti",
    city = "Bucuresti",
    address = "the address"
)

internal val testUserB = SNewUser(
    email = "sample2@email.com",
    tag = "test-user2",
    phone = "+40123456780",
    password = "Str0ngP@ss",
    firstName = "Another",
    middleName = null,
    lastName = "Tester",
    dateOfBirth = LocalDate(2001, 3, 4),
    countryCode = "RO",
    state = "Bucuresti",
    city = "Bucuresti",
    address = "the address 2"
)