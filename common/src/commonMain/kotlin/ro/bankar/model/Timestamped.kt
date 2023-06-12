package ro.bankar.model

import kotlinx.datetime.LocalDateTime

sealed interface STimestamped : Comparable<STimestamped> {
    val dateTime: LocalDateTime

    override fun compareTo(other: STimestamped) = dateTime.compareTo(other.dateTime)
}