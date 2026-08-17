package com.example.crewportal.data.roster

import com.example.crewportal.data.local.FlightEntity
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class RosterMetricsTest {
    @Test
    fun `monthly block total includes only requested duty types`() {
        val month = YearMonth.of(2026, 8)
        val roster = listOf(
            duty("flight", "2026-08-02T09:20:00", "FLIGHT", 395, completed = true),
            duty("sim", "2026-08-10T10:00:00", "SIMULATOR", 360, completed = false),
            duty("deadhead", "2026-08-02T16:00:00", "DEADHEAD", 405, completed = true),
            duty("other-month", "2026-09-01T09:00:00", "FLIGHT", 120, completed = true)
        )

        assertEquals(395, RosterMetrics.blockMinutes(roster, month))
        assertEquals(755, RosterMetrics.blockMinutes(roster, month, setOf("FLIGHT", "SIMULATOR")))
        assertEquals(395, RosterMetrics.blockMinutes(roster, month, setOf("FLIGHT", "SIMULATOR"), completedOnly = true))
    }

    private fun duty(
        id: String,
        departure: String,
        type: String,
        minutes: Int,
        completed: Boolean
    ) = FlightEntity(
        id = id,
        airline = "THAI",
        flightNumber = id,
        aircraftLabel = "A330",
        aircraftFullName = "Airbus A330-343",
        registration = "TBA",
        status = "SCHEDULED",
        departureIata = "BKK",
        departureIcao = "VTBS",
        departureCity = "Bangkok",
        departureAirport = "Suvarnabhumi Intl",
        arrivalIata = "TAS",
        arrivalIcao = "UTTT",
        arrivalCity = "Tashkent",
        arrivalAirport = "Islam Karimov",
        departureDateTime = departure,
        arrivalDateTime = departure,
        durationMinutes = minutes,
        dutyType = type,
        isCompleted = completed
    )
}
