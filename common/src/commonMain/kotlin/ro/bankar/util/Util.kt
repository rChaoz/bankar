package ro.bankar.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import java.time.format.DateTimeFormatter

fun Clock.nowHere() = now().toLocalDateTime(TimeZone.currentSystemDefault())

fun Clock.nowUTC() = now().toLocalDateTime(TimeZone.UTC)

fun Clock.todayHere() = todayIn(TimeZone.currentSystemDefault())

fun Instant.here() = toLocalDateTime(TimeZone.currentSystemDefault())

// Formatting
private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")!!
private val longDateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")!!
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")!!

fun LocalDate.format(long: Boolean = false) = toJavaLocalDate().format(if (long) longDateFormatter else dateFormatter)!!

fun LocalTime.format() = toJavaLocalTime().format(timeFormatter)!!

fun LocalDateTime.format(long: Boolean = false, vague: Boolean = false) =
    if (long) "${date.format(true)} • ${time.format()}"
    else if (date == Clock.System.todayHere()) time.format()
    else if (vague) date.format()
    else "${date.format()} • ${time.format()}"

fun formatIBAN(iban: String) = buildString(iban.length * 5 / 4 + 1) {
    for ((index, char) in iban.withIndex()) {
        append(char)
        if (index % 4 == 3 && index != iban.lastIndex) append(' ')
    }
}