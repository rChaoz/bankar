package ro.bankar.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable

@Serializable
data class SStatementRequest(
    val name: String?,
    val accountID: Int,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    companion object {
        val nameLengthRange = 1..20
    }

    fun validate() = when {
        name != null && name.length !in nameLengthRange -> "name"
        startDate > endDate -> "date_range"
        endDate > Clock.System.todayIn(TimeZone.UTC) -> "endDate"
        else -> null
    }
}

@Serializable
data class SStatement(val name: String?, val dateTime: LocalDateTime, val accountID: Int, val downloadName: String)