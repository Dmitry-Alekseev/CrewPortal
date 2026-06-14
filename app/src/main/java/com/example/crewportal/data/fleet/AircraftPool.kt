package com.example.crewportal.data.fleet

import kotlin.math.abs

data class FleetAircraft(
    val registration: String,
    val label: String,
    val fullName: String,
    val routeClass: String,
    val configuration: String,
    val engineType: String,
    val age: String,
    val status: String = "Active"
)

object AircraftPool {
    val aircraft = listOf(
        // Airbus A320 family — active narrow-body pool.
        FleetAircraft("HS-TXA", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "13y"),
        FleetAircraft("HS-TXB", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "13y"),
        FleetAircraft("HS-TXC", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "13y"),
        FleetAircraft("HS-TXD", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "13y"),
        FleetAircraft("HS-TXE", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "13y"),
        FleetAircraft("HS-TXF", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "12y"),
        FleetAircraft("HS-TXG", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "12y"),
        FleetAircraft("HS-TXH", "A320", "Airbus A320-232", "SHORT", "C12 / Y144", "IAE V2527-A5", "12y"),
        FleetAircraft("HS-TXJ", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "11y"),
        FleetAircraft("HS-TXK", "A320", "Airbus A320-232", "SHORT", "C12 / Y144", "IAE V2527-A5", "11y"),
        FleetAircraft("HS-TXL", "A320", "Airbus A320-232", "SHORT", "C12 / Y144", "IAE V2527-A5", "11y"),
        FleetAircraft("HS-TXM", "A320", "Airbus A320-232", "SHORT", "C12 / Y144", "IAE V2527-A5", "10y"),
        FleetAircraft("HS-TXN", "A320", "Airbus A320-232", "SHORT", "C12 / Y144", "IAE V2527-A5", "10y"),
        FleetAircraft("HS-TXO", "A320", "Airbus A320-232", "SHORT", "C12 / Y144", "IAE V2527-A5", "10y"),
        FleetAircraft("HS-TXP", "A320", "Airbus A320-232", "SHORT", "C12 / Y144", "IAE V2527-A5", "10y"),
        FleetAircraft("HS-TXQ", "A320", "Airbus A320-232", "SHORT", "C12 / Y144", "IAE V2527-A5", "10y"),
        FleetAircraft("HS-TXR", "A320", "Airbus A320-232", "SHORT", "C12 / Y144", "IAE V2527-A5", "10y"),
        FleetAircraft("HS-TXS", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "10y"),
        FleetAircraft("HS-TXT", "A320", "Airbus A320-232", "SHORT", "C12 / Y144", "IAE V2527-A5", "9y"),
        FleetAircraft("HS-TXU", "A320", "Airbus A320-232", "SHORT", "C12 / Y144", "IAE V2527-A5", "9y"),

        // Airbus A321neo — TO-series only.
        FleetAircraft("HS-TOA", "A321neo", "Airbus A321-251NX", "SHORT / MEDIUM", "C16 / Y175", "CFM LEAP-1A32", "0y"),
        FleetAircraft("HS-TOB", "A321neo", "Airbus A321-251NX", "SHORT / MEDIUM", "C16 / Y175", "CFM LEAP-1A32", "0y"),
        FleetAircraft("HS-TOD", "A321neo", "Airbus A321-251NX", "SHORT / MEDIUM", "C16 / Y175", "CFM LEAP-1A32", "0y"),
        FleetAircraft("HS-TOE", "A321neo", "Airbus A321-251NX", "SHORT / MEDIUM", "C16 / Y175", "CFM LEAP-1A32", "0y"),
        FleetAircraft("HS-TOF", "A321neo", "Airbus A321-251NX", "SHORT / MEDIUM", "C16 / Y175", "CFM LEAP-1A32", "0y"),

        // Airbus A330-300 — active assignment pool. HS-TEW deliberately excluded.
        FleetAircraft("HS-TBC", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "15y"),
        FleetAircraft("HS-TBD", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "15y"),
        FleetAircraft("HS-TBE", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "15y"),
        FleetAircraft("HS-TBF", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "15y"),
        FleetAircraft("HS-TBG", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "15y"),
        FleetAircraft("HS-TEN", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "14y"),
        FleetAircraft("HS-TEO", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "14y"),
        FleetAircraft("HS-TEP", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "14y"),
        FleetAircraft("HS-TER", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "13y"),
        FleetAircraft("HS-TES", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "13y"),
        FleetAircraft("HS-TET", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "13y"),
        FleetAircraft("HS-TEU", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "12y"),
        FleetAircraft("HS-TEV", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "12y"),

        // Airbus A350-900.
        FleetAircraft("HS-THB", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "9y"),
        FleetAircraft("HS-THC", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "9y"),
        FleetAircraft("HS-THD", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "9y"),
        FleetAircraft("HS-THE", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "9y"),
        FleetAircraft("HS-THF", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "8y"),
        FleetAircraft("HS-THG", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "8y"),
        FleetAircraft("HS-THH", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "8y"),
        FleetAircraft("HS-THJ", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "7y"),
        FleetAircraft("HS-THK", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "7y"),
        FleetAircraft("HS-THL", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "7y"),
        FleetAircraft("HS-THM", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "7y"),
        FleetAircraft("HS-THN", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "6y"),
        FleetAircraft("HS-THO", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "6y"),
        FleetAircraft("HS-THP", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "6y"),
        FleetAircraft("HS-THQ", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "5y"),
        FleetAircraft("HS-THS", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "5y"),
        FleetAircraft("HS-THT", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "5y"),
        FleetAircraft("HS-THU", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "4y"),
        FleetAircraft("HS-THV", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "4y"),
        FleetAircraft("HS-THY", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "3y"),
        FleetAircraft("HS-THZ", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "3y"),
        FleetAircraft("HS-TXI", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "0y")
    )

    fun assignFor(aircraftLabel: String, routeClass: String, flightId: String): FleetAircraft {
        val requiredLabel = normalizeLabel(aircraftLabel)
        val sameType = aircraft.filter { it.label == requiredLabel }
        val compatible = sameType.filter { item ->
            when (routeClass) {
                "LONG" -> item.routeClass == "LONG" || item.routeClass.contains("MEDIUM")
                "MEDIUM" -> item.routeClass.contains("MEDIUM") || item.routeClass == "LONG"
                else -> item.routeClass.contains("SHORT")
            }
        }.ifEmpty { sameType }

        // Never fall back across aircraft families. If a route says A330, a TH* A350 reg must not be returned.
        val safePool = compatible.ifEmpty { aircraft.filter { it.label == requiredLabel } }.ifEmpty { aircraft.filter { it.label == "A320" } }
        return safePool[abs(flightId.hashCode()) % safePool.size]
    }

    private fun normalizeLabel(label: String): String = when {
        label.startsWith("A350", ignoreCase = true) -> "A350-900"
        label.startsWith("A330", ignoreCase = true) -> "A330-300"
        label.startsWith("A321", ignoreCase = true) -> "A321neo"
        label.startsWith("A320", ignoreCase = true) -> "A320"
        else -> label
    }

    fun byRegistration(registration: String): FleetAircraft? = aircraft.firstOrNull { it.registration == registration }
}
