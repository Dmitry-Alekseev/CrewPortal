package com.example.crewportal.data.fleet

import kotlin.math.abs

data class FleetAircraft(
    val registration: String,
    val label: String,
    val fullName: String,
    val routeClass: String,
    val configuration: String,
    val status: String = "Active"
)

object AircraftPool {
    val aircraft = listOf(
        FleetAircraft("HS-TXA", "A320", "Airbus A320-232", "SHORT", "C12 / Y150"),
        FleetAircraft("HS-TXB", "A320", "Airbus A320-232", "SHORT", "C12 / Y150"),
        FleetAircraft("HS-TXC", "A320", "Airbus A320-232", "SHORT", "C12 / Y150"),
        FleetAircraft("HS-TXD", "A320", "Airbus A320-232", "SHORT", "C12 / Y150"),
        FleetAircraft("HS-TXE", "A320", "Airbus A320-232", "SHORT", "C12 / Y150"),
        FleetAircraft("HS-TXF", "A320", "Airbus A320-232", "SHORT", "C12 / Y150"),
        FleetAircraft("HS-TXG", "A320", "Airbus A320-232", "SHORT", "C12 / Y150"),
        FleetAircraft("HS-TXH", "A321neo", "Airbus A321-271NX", "SHORT", "C16 / Y175", "New cabin"),
        FleetAircraft("HS-TXI", "A321neo", "Airbus A321-271NX", "SHORT", "C16 / Y175", "New cabin"),
        FleetAircraft("HS-TXJ", "A320", "Airbus A320-232", "SHORT", "C12 / Y150"),
        FleetAircraft("HS-TXS", "A320", "Airbus A320-232", "SHORT", "C12 / Y150"),
        FleetAircraft("HS-TEO", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263"),
        FleetAircraft("HS-TEP", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263"),
        FleetAircraft("HS-TEQ", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263"),
        FleetAircraft("HS-TER", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263"),
        FleetAircraft("HS-TEU", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263"),
        FleetAircraft("HS-TEV", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263"),
        FleetAircraft("HS-TEW", "A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263"),
        FleetAircraft("HS-THB", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289"),
        FleetAircraft("HS-THC", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289"),
        FleetAircraft("HS-THD", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289"),
        FleetAircraft("HS-THE", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289"),
        FleetAircraft("HS-THF", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289"),
        FleetAircraft("HS-THG", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289"),
        FleetAircraft("HS-THH", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289"),
        FleetAircraft("HS-THJ", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289"),
        FleetAircraft("HS-THK", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289"),
        FleetAircraft("HS-THL", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289"),
        FleetAircraft("HS-THM", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289"),
        FleetAircraft("HS-THN", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289"),
        FleetAircraft("HS-THO", "A350-900", "Airbus A350-941", "LONG", "C32 / Y289")
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
