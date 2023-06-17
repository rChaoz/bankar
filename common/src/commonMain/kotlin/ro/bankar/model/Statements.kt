package ro.bankar.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import ro.bankar.util.todayHere

@Serializable
data class SStatementRequest(
    val name: String?,
    val accountID: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val timeZone: TimeZone
) {
    companion object {
        const val maxNameLength = 20
    }

    fun validate() = when {
        name != null && name.length !in 1..maxNameLength -> "name"
        startDate > endDate -> "date_range"
        endDate > Clock.System.todayHere() -> "endDate"
        else -> null
    }
}

@Serializable
data class SStatement(val id: Int, val name: String?, val timestamp: Instant, val accountID: Int)