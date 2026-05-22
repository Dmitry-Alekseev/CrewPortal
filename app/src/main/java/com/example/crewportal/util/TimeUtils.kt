package com.example.crewportal.util

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private val cardDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
private val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

fun parseLocalDateTime(value: String): LocalDateTime = LocalDateTime.parse(value, isoFormatter)
fun displayDate(value: String): String = parseLocalDateTime(value).format(cardDateFormatter).uppercase(Locale.ENGLISH)
fun displayDay(value: String): String = parseLocalDateTime(value).format(dayFormatter).uppercase(Locale.ENGLISH)
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
