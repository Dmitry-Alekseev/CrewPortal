package com.example.crewportal.data.crew

/** Stable values persisted in FlightEntity.lineCheckRole without a Room schema change. */
object InstructorRole {
    const val NONE = ""
    const val CAPTAIN_INSTRUCTOR = "CAPTAIN_INSTRUCTOR"
    const val OBSERVER_INSTRUCTOR = "INSTRUCTOR_OBSERVER"

    fun isInstructor(value: String): Boolean = value.isNotBlank()

    fun isObserver(value: String): Boolean =
        value == OBSERVER_INSTRUCTOR || value == "INSTRUCTOR" // legacy 3.0 rows

    fun isCaptainInstructor(value: String): Boolean = value == CAPTAIN_INSTRUCTOR

    fun note(value: String): String = when {
        isCaptainInstructor(value) -> "Captain instructor • operating commander"
        isObserver(value) -> "Line pilot instructor / observer • third crew member"
        else -> ""
    }
}
