package com.example.crewportal.util

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private val cardDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
private val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
private val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

fun parseLocalDateTime(value: String): LocalDateTime = LocalDateTime.parse(value, isoFormatter)
fun displayDate(value: String): String = parseLocalDateTime(value).format(cardDateFormatter).uppercase(Locale.ENGLISH)
fun displayShortDate(value: String): String = parseLocalDateTime(value).format(shortDateFormatter).uppercase(Locale.ENGLISH)
fun displayDay(value: String): String = parseLocalDateTime(value).format(dayFormatter).uppercase(Locale.ENGLISH)
fun displayMonth(date: LocalDate): String = date.format(monthFormatter)
fun displayTime(value: String): String = parseLocalDateTime(value).format(timeFormatter)
fun formatMinutes(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"
fun formatTotalMinutes(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"

fun canRegister(departureDateTime: String, completed: Boolean): Boolean {
    if (completed) return false
    val now = LocalDateTime.now()
    val departure = parseLocalDateTime(departureDateTime)
    val minutesToDeparture = Duration.between(now, departure).toMinutes()
    return minutesToDeparture in 0..(24 * 60)
}

fun hasArrived(arrivalDateTime: String): Boolean {
    return !LocalDateTime.now().isBefore(parseLocalDateTime(arrivalDateTime))
}

fun reportDateTime(departureDateTime: String, durationMinutes: Int): LocalDateTime {
    val reportOffset = if (durationMinutes >= 240) 90L else 60L
    return parseLocalDateTime(departureDateTime).minusMinutes(reportOffset)
}

fun dutyEndDateTime(arrivalDateTime: String): LocalDateTime = parseLocalDateTime(arrivalDateTime).plusMinutes(30)

fun dutyMinutes(departureDateTime: String, arrivalDateTime: String, durationMinutes: Int): Int {
    return Duration.between(reportDateTime(departureDateTime, durationMinutes), dutyEndDateTime(arrivalDateTime)).toMinutes().toInt()
}

fun restStatus(previousArrival: String?, nextDeparture: String?): String {
    if (previousArrival == null || nextDeparture == null) return "OK"
    val rest = Duration.between(dutyEndDateTime(previousArrival), reportDateTime(nextDeparture, 0)).toMinutes()
    return if (rest >= 12 * 60) "OK" else "INSUFFICIENT REST"
}

fun briefingDistanceNm(durationMinutes: Int): Int = (durationMinutes * 7.2).toInt().coerceAtLeast(220)
fun cruiseLevel(durationMinutes: Int): String = when {
    durationMinutes >= 540 -> "FL390"
    durationMinutes >= 240 -> "FL370"
    else -> "FL330"
}
fun etopsText(durationMinutes: Int): String = if (durationMinutes >= 360) "Required" else "Not required"
fun alternateFor(arrivalIata: String): String = when (arrivalIata) {
    "HKT" -> "KBV"
    "CXR" -> "SGN"
    "SIN" -> "KUL"
    "HKG" -> "MFM"
    "IST" -> "SAW / ESB"
    "FRA" -> "MUC / DUS"
    "MEL" -> "SYD / ADL"
    "CDG" -> "AMS / BRU"
    else -> "Company dispatch"
}
