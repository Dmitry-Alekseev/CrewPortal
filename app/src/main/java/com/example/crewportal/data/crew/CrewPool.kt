package com.example.crewportal.data.crew

import kotlin.math.abs

data class FlightCrew(
    val captain: String,
    val firstOfficer: String,
    val reliefCaptain: String? = null,
    val reliefFirstOfficer: String? = null,
    val cabinManager: String,
    val cabinCrewCount: Int
)

object CrewPool {
    private val captains = listOf(
        "Dmitrii Alekseev", "Anan Wongchai", "Nikolai Petrov", "Surasak Chaiyaporn",
        "Michael Anderson", "Pavel Sokolov", "Kittisak Boonmee", "Alexey Morozov"
    )
    private val firstOfficers = listOf(
        "Nattapong Srisai", "Sergey Volkov", "Daniel Weber", "Piyawat Kanda",
        "Ivan Romanov", "Thanakorn Prasert", "Mateo Ricci", "Artem Kuznetsov"
    )
    private val cabinManagers = listOf(
        "Pimchanok S.", "Siriporn K.", "Narumon P.", "Achara W.", "Kanyarat T.", "Wipada C."
    )

    fun forFlight(flightId: String, longHaul: Boolean): FlightCrew {
        val seed = abs(flightId.hashCode())
        val captain = "Dmitrii Alekseev"
        val fo = firstOfficers[(seed / 3) % firstOfficers.size]
        val cm = cabinManagers[(seed / 7) % cabinManagers.size]
        return if (longHaul) {
            FlightCrew(
                captain = captain,
                firstOfficer = fo,
                reliefCaptain = captains.filter { it != "Dmitrii Alekseev" }[(seed / 11) % (captains.size - 1)],
                reliefFirstOfficer = firstOfficers[(seed / 13) % firstOfficers.size],
                cabinManager = cm,
                cabinCrewCount = 10 + seed % 4
            )
        } else {
            FlightCrew(captain, fo, null, null, cm, 4 + seed % 3)
        }
    }
}
