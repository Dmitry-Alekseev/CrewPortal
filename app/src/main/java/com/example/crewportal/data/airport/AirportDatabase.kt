package com.example.crewportal.data.airport

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AirportInfo(
    val iata: String,
    val icao: String,
    val name: String,
    val city: String,
    val country: String,
    val utcOffsetHours: Int
)

object AirportDatabase {
    private val airports = listOf(
        AirportInfo("BKK", "VTBS", "Suvarnabhumi Intl", "Bangkok", "Thailand", 7),
        AirportInfo("HKT", "VTSP", "Phuket Intl", "Phuket", "Thailand", 7),
        AirportInfo("CXR", "VVCR", "Cam Ranh Intl", "Nha Trang", "Vietnam", 7),
        AirportInfo("SIN", "WSSS", "Changi Intl", "Singapore", "Singapore", 8),
        AirportInfo("HKG", "VHHH", "Hong Kong Intl", "Hong Kong", "Hong Kong", 8),
        AirportInfo("IST", "LTFM", "Istanbul Airport", "Istanbul", "Türkiye", 3),
        AirportInfo("FRA", "EDDF", "Frankfurt Main", "Frankfurt", "Germany", 2),
        AirportInfo("MEL", "YMML", "Melbourne Intl", "Melbourne", "Australia", 10),
        AirportInfo("CDG", "LFPG", "Charles de Gaulle", "Paris", "France", 2),
        AirportInfo("NRT", "RJAA", "Narita Intl", "Tokyo", "Japan", 9),
        AirportInfo("KUL", "WMKK", "Kuala Lumpur Intl", "Kuala Lumpur", "Malaysia", 8),
        AirportInfo("DEL", "VIDP", "Indira Gandhi Intl", "Delhi", "India", 5),
        AirportInfo("CNX", "VTCC", "Chiang Mai Intl", "Chiang Mai", "Thailand", 7),
        AirportInfo("SYD", "YSSY", "Sydney Kingsford Smith", "Sydney", "Australia", 10)
    ).associateBy { it.iata }

    fun byIata(iata: String): AirportInfo? = airports[iata.uppercase(Locale.ENGLISH)]

    fun utcText(localDateTime: String, iata: String): String {
        val airport = byIata(iata) ?: return "UTC time unavailable"
        val local = LocalDateTime.parse(localDateTime)
        val utc = local.atOffset(ZoneOffset.ofHours(airport.utcOffsetHours)).withOffsetSameInstant(ZoneOffset.UTC)
        return utc.format(DateTimeFormatter.ofPattern("dd MMM HH:mm 'UTC'", Locale.ENGLISH)).uppercase(Locale.ENGLISH)
    }

    fun utcClockText(localDateTime: String, iata: String): String {
        val airport = byIata(iata) ?: return "UTC"
        val local = LocalDateTime.parse(localDateTime)
        val utc = local.atOffset(ZoneOffset.ofHours(airport.utcOffsetHours)).withOffsetSameInstant(ZoneOffset.UTC)
        return utc.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH))
    }

    fun localOffsetText(iata: String): String {
        val offset = byIata(iata)?.utcOffsetHours ?: return "UTC offset unavailable"
        return if (offset >= 0) "UTC+$offset" else "UTC$offset"
    }
}
