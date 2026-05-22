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
        FleetAircraft("HS-TXA", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "13y 11m"),
        FleetAircraft("HS-TXB", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "13y 9m"),
        FleetAircraft("HS-TXC", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "13y 6m"),
        FleetAircraft("HS-TXD", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "13y 3m"),
        FleetAircraft("HS-TXE", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "13y 1m"),
        FleetAircraft("HS-TXF", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "12y 10m"),
        FleetAircraft("HS-TXG", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "12y 7m"),
        FleetAircraft("HS-TXH", "A321neo", "Airbus A321-251NX", "SHORT", "C16 / Y175", "CFM LEAP-1A32", "0y 5m", "New cabin"),
        FleetAircraft("HS-TXI", "A321neo", "Airbus A321-251NX", "SHORT", "C16 / Y175", "CFM LEAP-1A32", "0y 4m", "New cabin"),
        FleetAircraft("HS-TXJ", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "11y 8m"),
        FleetAircraft("HS-TXS", "A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3", "10y 11m"),
        FleetAircraft("HS-TEO", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "14y 2m"),
        FleetAircraft("HS-TEP", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "14y 0m"),
        FleetAircraft("HS-TEQ", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "13y 8m"),
        FleetAircraft("HS-TER", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "13y 6m"),
        FleetAircraft("HS-TEU", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "12y 11m"),
        FleetAircraft("HS-TEV", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "12y 8m"),
        FleetAircraft("HS-TEW", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60", "12y 5m"),
        FleetAircraft("HS-THB", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "9y 10m"),
        FleetAircraft("HS-THC", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "9y 7m"),
        FleetAircraft("HS-THD", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "9y 4m"),
        FleetAircraft("HS-THE", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "9y 0m"),
        FleetAircraft("HS-THF", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "8y 9m"),
        FleetAircraft("HS-THG", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "8y 6m"),
        FleetAircraft("HS-THH", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "8y 2m"),
        FleetAircraft("HS-THJ", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "7y 11m"),
        FleetAircraft("HS-THK", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "7y 7m"),
        FleetAircraft("HS-THL", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "7y 4m"),
        FleetAircraft("HS-THM", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "7y 1m"),
        FleetAircraft("HS-THN", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "6y 9m"),
        FleetAircraft("HS-THO", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84", "6y 5m"),
    )

    fun assignFor(aircraftLabel: String, routeClass: String, flightId: String): FleetAircraft {
        val compatible = aircraft.filter { item ->
            val classOk = when (routeClass) {
                "LONG" -> item.routeClass == "LONG"
                "MEDIUM" -> item.routeClass == "MEDIUM" || item.routeClass == "LONG"
                else -> item.routeClass == "SHORT"
            }
            val labelOk = aircraftLabel == item.label ||
                (aircraftLabel.startsWith("A350") && item.label == "A350-900") ||
                (aircraftLabel.startsWith("A330") && item.label == "A330-300") ||
                (aircraftLabel.startsWith("A321") && item.label == "A321neo") ||
                (aircraftLabel.startsWith("A320") && item.label == "A320")
            classOk && labelOk
        }.ifEmpty { aircraft.filter { it.routeClass == routeClass }.ifEmpty { aircraft } }
        return compatible[abs(flightId.hashCode()) % compatible.size]
    }

    fun byRegistration(registration: String): FleetAircraft? = aircraft.firstOrNull { it.registration == registration }
}
