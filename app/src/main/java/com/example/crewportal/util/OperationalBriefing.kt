package com.example.crewportal.util

import kotlin.math.roundToInt

data class FuelEstimate(
    val taxiKg: Int,
    val tripKg: Int,
    val contingencyKg: Int,
    val alternateKg: Int,
    val finalReserveKg: Int,
    val extraKg: Int
) {
    val totalKg: Int get() = taxiKg + tripKg + contingencyKg + alternateKg + finalReserveKg + extraKg
}

fun estimatedFuel(durationMinutes: Int, aircraftLabel: String): FuelEstimate {
    val burnPerHour = when {
        aircraftLabel.startsWith("A350") -> 5800
        aircraftLabel.startsWith("A330") -> 5200
        aircraftLabel.startsWith("A321") -> 2700
        else -> 2500
    }
    val trip = ((durationMinutes / 60.0) * burnPerHour).roundToInt()
    val taxi = if (durationMinutes >= 360) 900 else 450
    val contingency = (trip * 0.05).roundToInt()
    val alternate = when {
        durationMinutes >= 360 -> 3200
        durationMinutes >= 180 -> 2200
        else -> 1200
    }
    val finalReserve = if (aircraftLabel.startsWith("A350") || aircraftLabel.startsWith("A330")) 1800 else 1100
    val extra = if (durationMinutes >= 360) 1500 else 500
    return FuelEstimate(taxi, trip, contingency, alternate, finalReserve, extra)
}

fun notamSummary(arrivalIata: String): String = when (arrivalIata) {
    "IST" -> "Review LTFM taxiway works and flow management messages."
    "FRA" -> "Possible ATFM regulation during peak arrival waves."
    "HKT" -> "Runway inspection windows may affect late evening arrivals."
    "SIN" -> "Standard Changi arrival procedures. No critical local NOTAM in company summary."
    else -> "No critical NOTAMs in company summary. Official briefing required."
}

fun disruptionReason(route: String): String = when {
    route.contains("IST") -> "Late inbound aircraft and slot coordination"
    route.contains("HKT") -> "Weather monitoring and airport flow control"
    else -> "Operational schedule optimization"
}
