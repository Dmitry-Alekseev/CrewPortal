package com.example.crewportal.data.roster

import com.example.crewportal.data.leave.LeaveDatabase
import com.example.crewportal.data.qualification.A380QualificationPolicy
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class A380TransitionProgramTest {
    @Test
    fun `october separates company and personal leave then reserves training`() {
        assertEquals("ANNUAL_LEAVE", LeaveDatabase.leaveFor(LocalDate.of(2026, 10, 8))?.type)
        assertEquals("PERSONAL_LEAVE", LeaveDatabase.leaveFor(LocalDate.of(2026, 10, 9))?.type)
        assertEquals("PERSONAL_LEAVE", LeaveDatabase.leaveFor(LocalDate.of(2026, 10, 14))?.type)

        val roster = RosterGenerator.generateForMonth(YearMonth.of(2026, 10))
        assertTrue(roster.any { it.dutyType == "DEADHEAD" && it.departureIata == "BKK" && it.arrivalIata == "IST" })
        assertTrue(roster.any { it.dutyType == "DEADHEAD" && it.departureIata == "IST" && it.arrivalIata == "TLS" })
        assertTrue(roster.any { it.flightNumber == "A380 Theoretical Examination" && it.departureDateTime.startsWith("2026-10-29") })
        assertTrue(roster.any { it.flightNumber == "A380 Simulator Training Session" && it.departureDateTime.startsWith("2026-10-30") })
        assertTrue(roster.any { it.flightNumber == "A380 Final Practical Examination" && it.departureDateTime.startsWith("2026-10-31") })
        assertFalse(roster.any { it.dutyType == "FLIGHT" })
    }

    @Test
    fun `november starts with passenger return and recovery`() {
        val roster = RosterGenerator.generateForMonth(YearMonth.of(2026, 11))
        assertTrue(roster.any { it.dutyType == "DEADHEAD" && it.departureIata == "TLS" && it.arrivalIata == "IST" })
        assertTrue(roster.any { it.dutyType == "DEADHEAD" && it.departureIata == "IST" && it.arrivalIata == "BKK" })
        assertTrue(roster.any { it.dutyType == "OFF" && it.departureDateTime.startsWith("2026-11-02") })
        assertTrue(roster.none { it.dutyType == "FLIGHT" && it.departureDateTime.startsWith("2026-11-01") })
    }

    @Test
    fun `rating and captain transition use fixed effective dates`() {
        assertFalse(A380QualificationPolicy.hasTypeRating(LocalDate.of(2026, 10, 31)))
        assertTrue(A380QualificationPolicy.hasTypeRating(LocalDate.of(2026, 11, 1)))
        assertEquals(LocalDate.of(2027, 1, 1), A380QualificationPolicy.captainEffectiveDate)
    }
}
