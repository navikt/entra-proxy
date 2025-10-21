package no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions

import java.time.Instant
import java.time.Instant.now
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneId.systemDefault
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

object TimeExtensions {

    val ALLTID = LocalDate.now().plusYears(100)
    val IMORGEN get() = LocalDate.now().plusDays(1)
    val IGÅR get() = LocalDate.now().minusDays(1)

    fun Instant.isBeforeNow() = isBefore(now())
    fun Instant.diffFromNow() = java.time.Duration.between(now(), this).toKotlinDuration().format()
    fun LocalDate.toInstant(): Instant = atStartOfDay(systemDefault()).toInstant()

    private fun LocalDate.månederSidenIdag() =
        LocalDate.now().let {
            assert(isBefore(it)) { "Datoen $this er ikke før dagens dato $it" }
            Period.between(this, it).let { it.years * 12 + it.months } + if (it.dayOfMonth > dayOfMonth) 1 else 0
        }

    fun java.time.Duration.format() = this.toKotlinDuration().format()

    fun Duration.format(): String {
        val days = inWholeDays
        val hours = inWholeHours % 24
        val minutes = inWholeMinutes % 60
        val seconds = inWholeSeconds % 60

        return buildString {
            if (days > 0) append("$days ${if (days == 1L) "dag" else "dager"} ")
            if (hours > 0) append("$hours ${if (hours == 1L) "time" else "timer"} ")
            if (minutes > 0) append("$minutes ${if (minutes == 1L) "minutt" else "minutter"} ")
            if (seconds > 0) append("$seconds ${if (seconds == 1L) "sekund" else "sekunder"}")
        }.trim()
    }

    fun Long.local(fmt: String = "yyyy-MM-dd HH:mm:ss") = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(this),
        ZoneId.of("Europe/Oslo")
    )
        .format(DateTimeFormatter.ofPattern(fmt))

    fun LocalDate.intervallSiden() =
        when (månederSidenIdag()) {
            in 0..6 -> "0-6"
            in 7..12 -> "7-12"
            in 13..24 -> "13-24"
            else -> ">24"
        }
}