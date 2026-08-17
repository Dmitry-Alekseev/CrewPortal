package com.example.crewportal.util

import com.example.crewportal.data.airport.AirportDatabase
import java.time.LocalDateTime
import java.time.ZoneId

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
    val departureZone = airportZone(departureIata) ?: return departureLocal.plusMinutes(blockMinutes.toLong())
    val arrivalZone = airportZone(arrivalIata) ?: return departureLocal.plusMinutes(blockMinutes.toLong())
    return departureLocal
        .atZone(departureZone)
        .plusMinutes(blockMinutes.toLong())
        .withZoneSameInstant(arrivalZone)
        .toLocalDateTime()
}

private fun airportZone(iata: String): ZoneId? = AirportDatabase.byIata(iata)?.zoneId?.let(ZoneId::of)

/** Converts an airport-local legacy timestamp to a stable UTC instant. */
fun airportLocalEpochMillis(localDateTime: String, iata: String): Long? {
    val zone = airportZone(iata) ?: return null
    return LocalDateTime.parse(localDateTime).atZone(zone).toInstant().toEpochMilli()
}
