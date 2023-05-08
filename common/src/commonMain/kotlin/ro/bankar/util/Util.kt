package ro.bankar.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

fun Clock.nowHere() = now().toLocalDateTime(TimeZone.currentSystemDefault())

fun Clock.nowUTC() = now().toLocalDateTime(TimeZone.UTC)

fun Clock.todayHere() = todayIn(TimeZone.currentSystemDefault())

fun Instant.here() = toLocalDateTime(TimeZone.currentSystemDefault())