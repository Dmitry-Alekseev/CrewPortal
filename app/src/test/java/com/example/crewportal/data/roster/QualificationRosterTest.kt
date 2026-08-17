package com.example.crewportal.data.roster

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QualificationRosterTest {
    @Test
    fun `simulator is a linked three-day non-credit block`() {
        val roster = RosterGenerator.generateForMonth(YearMonth.of(2026, 7))
        val simulator = roster.filter { it.dutyType == "SIMULATOR" }.sortedBy { it.eventDayIndex }

        assertEquals(listOf(1, 2, 3), simulator.map { it.eventDayIndex })
        assertEquals(1, simulator.map { it.eventGroupId }.distinct().size)
        assertTrue(simulator.all { it.eventTotalDays == 3 && !it.flightTimeCreditEligible && it.durationMinutes == 0 })
        simulator.zipWithNext().forEach { (a, b) ->
            assertEquals(date(a.departureDateTime).plusDays(1), date(b.departureDateTime))
        }
        assertFalse(RosterConflictValidator.errors(YearMonth.of(2026, 7), roster).isNotEmpty())
    }

    @Test
    fun `medical is two days and instructor line check has no operating credit`() {
        val roster = RosterGenerator.generateForMonth(YearMonth.of(2026, 8))
        val medical = roster.filter { it.dutyType == "MEDICAL" }.sortedBy { it.eventDayIndex }
        val lineCheck = roster.filter { it.lineCheckRole == "INSTRUCTOR" }

        assertEquals(listOf(1, 2), medical.map { it.eventDayIndex })
        assertEquals(1, medical.map { it.eventGroupId }.distinct().size)
        assertTrue(medical.all { !it.flightTimeCreditEligible && it.durationMinutes == 0 })
        assertTrue(lineCheck.isNotEmpty())
        assertTrue(lineCheck.all { it.dutyType == "FLIGHT" && !it.flightTimeCreditEligible })
        assertTrue(RosterConflictValidator.errors(YearMonth.of(2026, 8), roster).isEmpty())
    }

    private fun date(value: String): LocalDate = LocalDateTime.parse(value).toLocalDate()
}
