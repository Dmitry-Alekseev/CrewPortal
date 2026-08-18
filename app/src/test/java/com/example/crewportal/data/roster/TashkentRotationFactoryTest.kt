package com.example.crewportal.data.roster

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TashkentRotationFactoryTest {
    @Test
    fun `Thursday rotation stays Friday Saturday and operates Sunday return`() {
        val rotation = TashkentRotationFactory.thursdayStaySundayReturn(LocalDate.parse("2026-08-06"))

        assertEquals(listOf("FLIGHT", "STAY", "STAY", "FLIGHT"), rotation.map { it.dutyType })
        assertEquals(listOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), rotation.map { LocalDateTime.parse(it.departureDateTime).dayOfWeek })
        assertEquals(listOf("2026-08-07", "2026-08-08"), rotation.filter { it.dutyType == "STAY" }.map { it.departureDateTime.take(10) })
        assertEquals("TG685", rotation.last().flightNumber)
        assertEquals("Scheduled flight", rotation.last().dutyNote)
        assertEquals("2026-08-09T22:30:00", rotation.last().arrivalDateTime)
        assertEquals(800, rotation.filter { it.dutyType == "FLIGHT" }.sumOf { it.durationMinutes })
    }

    @Test
    fun `Sunday arrival has same day deadhead departure`() {
        val sunday = LocalDate.parse("2026-08-02")
        val rotation = TashkentRotationFactory.sundaySameDayDeadhead(sunday)
        val outbound = rotation.first()
        val deadhead = rotation.last()

        assertEquals("2026-08-02T13:55:00", outbound.arrivalDateTime)
        assertEquals(sunday, LocalDateTime.parse(deadhead.departureDateTime).toLocalDate())
        assertEquals("DEADHEAD", deadhead.dutyType)
        assertEquals("2026-08-02T16:00:00", deadhead.departureDateTime)
        assertEquals("2026-08-03T00:45:00", deadhead.arrivalDateTime)
        assertTrue(deadhead.durationMinutes == 0)
        assertEquals(395, rotation.filter { it.dutyType == "FLIGHT" }.sumOf { it.durationMinutes })
    }
}
