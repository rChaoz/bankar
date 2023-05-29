package ro.bankar.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ro.bankar.util.todayHere

@Serializable
data class SStatementRequest(
    val name: String?,
    val accountID: Int,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    companion object {
        val maxNameLength = 20
    }

    fun validate() = when {
        name != null && name.length !in 1..maxNameLength -> "name"
        startDate > endDate -> "date_range"
        endDate > Clock.System.todayHere() -> "endDate"
        else -> null
    }
}

@Serializable
data class SStatement(val id: Int, val name: String?, val dateTime: LocalDateTime, val accountID: Int)