package ro.bankar.model

import kotlinx.datetime.Instant

sealed interface STimestamped : Comparable<STimestamped> {
    val timestamp: Instant

    override fun compareTo(other: STimestamped) = timestamp.compareTo(other.timestamp)
}