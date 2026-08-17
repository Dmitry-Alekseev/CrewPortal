package com.example.crewportal.util

import com.example.crewportal.data.airport.AirportDatabase
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Converts a scheduled airport-local departure into the airport-local arrival time.
 *
 * Roster timestamps intentionally remain ISO local date-times because that is the existing
 * persistence format. The airport codes provide the missing offsets at calculation boundaries,
 * so a BKK-TAS sector is calculated as one instant even though the displayed clocks differ.
 * Unknown airports keep the legacy local-time behaviour instead of making old records unreadable.
 */
fun arrivalLocalDateTime(
    departureLocal: LocalDateTime,
    departureIata: String,
    arrivalIata: String,
    blockMinutes: Int
): LocalDateTime {
    val departureOffset = airportOffset(departureIata) ?: return departureLocal.plusMinutes(blockMinutes.toLong())
    val arrivalOffset = airportOffset(arrivalIata) ?: return departureLocal.plusMinutes(blockMinutes.toLong())
    return departureLocal
        .atOffset(departureOffset)
        .plusMinutes(blockMinutes.toLong())
        .withOffsetSameInstant(arrivalOffset)
        .toLocalDateTime()
}

private fun airportOffset(iata: String): ZoneOffset? = AirportDatabase.byIata(iata)
    ?.utcOffsetMinutes
    ?.let { ZoneOffset.ofTotalSeconds(it * 60) }

/** Converts an airport-local legacy timestamp to a stable UTC instant. */
fun airportLocalEpochMillis(localDateTime: String, iata: String): Long? {
    val offset = airportOffset(iata) ?: return null
    return LocalDateTime.parse(localDateTime).atOffset(offset).toInstant().toEpochMilli()
}
