package com.example.crewportal.data.local

/** Stable persisted duty values. Keep [value] unchanged or provide a database migration. */
enum class DutyType(val value: String) {
    FLIGHT("FLIGHT"),
    AIRCRAFT_DELIVERY("AIRCRAFT_DELIVERY"),
    DEADHEAD("DEADHEAD"),
    CREW_REST("CREW_REST"),
    TECHNICAL_STOP("TECHNICAL_STOP"),
    STAY("STAY"),
    OFF("OFF"),
    RESERVE("RESERVE"),
    SIMULATOR("SIMULATOR"),
    MEDICAL("MEDICAL"),
    SAFETY("SAFETY");

    companion object {
        fun fromPersisted(value: String): DutyType? = entries.firstOrNull { it.value == value }
    }
}

val FlightEntity.typedDuty: DutyType?
    get() = DutyType.fromPersisted(dutyType)
