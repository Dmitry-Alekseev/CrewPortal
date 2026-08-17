package com.example.crewportal.data.fleet

data class AircraftTypeSpec(
    val label: String,
    val fullName: String,
    val routeClass: String,
    val configuration: String,
    val engineType: String
)

/** Single source of truth for selectable and deliverable aircraft types. */
object AircraftTypeCatalog {
    val types = listOf(
        AircraftTypeSpec("A320", "Airbus A320-214", "SHORT", "C12 / Y144", "CFM56-5B4/3"),
        AircraftTypeSpec("A320neo", "Airbus A320-251N", "SHORT / MEDIUM", "C12 / Y150", "CFM LEAP-1A26"),
        AircraftTypeSpec("A321neo", "Airbus A321-251NX", "SHORT / MEDIUM", "C16 / Y175", "CFM LEAP-1A32"),
        AircraftTypeSpec("A330-300", "Airbus A330-343", "MEDIUM", "C31 / Y263", "Rolls-Royce Trent 772B-60"),
        AircraftTypeSpec("A330-800neo", "Airbus A330-841", "MEDIUM / LONG", "C32 / Y220", "Rolls-Royce Trent 7000"),
        AircraftTypeSpec("A330-900neo", "Airbus A330-941", "MEDIUM / LONG", "C32 / Y260", "Rolls-Royce Trent 7000"),
        AircraftTypeSpec("A350-900", "Airbus A350-941", "LONG", "C32 / Y289", "Rolls-Royce Trent XWB-84")
    )

    /** Restricted list shown by the simplified Aircraft Delivery form in Crew Portal 3.0. */
    val deliveryTypes = listOf("A330-800neo", "A330-900neo", "A350-900", "A320neo", "A321neo")

    fun byLabel(label: String): AircraftTypeSpec {
        val normalized = when {
            label.contains("A330-8", true) -> "A330-800neo"
            label.equals("A330neo", true) || label.contains("A330-9", true) -> "A330-900neo"
            label.startsWith("A350", true) -> "A350-900"
            label.startsWith("A330", true) -> "A330-300"
            label.startsWith("A321", true) -> "A321neo"
            label.contains("NEO", true) && label.startsWith("A320", true) -> "A320neo"
            else -> "A320"
        }
        return types.first { it.label == normalized }
    }
}
