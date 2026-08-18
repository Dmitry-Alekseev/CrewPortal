package com.example.crewportal.data.roster

import com.example.crewportal.data.leave.LeaveDatabase
import com.example.crewportal.data.leave.LeavePeriod
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertFalse
import org.junit.Test

class RosterLeaveGenerationTest {
    @Test
    fun `next month generator never places duty inside persisted approved leave`() {
        val leave = LeavePeriod(
            id = "test-next-month-leave",
            type = "PERSONAL_LEAVE",
            title = "Personal Leave",
            start = LocalDate.of(2027, 3, 10),
            end = LocalDate.of(2027, 3, 16)
        )
        LeaveDatabase.addPersonalLeave(leave)
        try {
            val roster = RosterGenerator.generateForMonth(YearMonth.of(2027, 3))
            assertFalse(roster.any { row ->
                val start = LocalDateTime.parse(row.departureDateTime).toLocalDate()
                val end = LocalDateTime.parse(row.arrivalDateTime).toLocalDate()
                !end.isBefore(leave.start) && !start.isAfter(leave.end)
            })
        } finally {
            LeaveDatabase.approvedPersonalLeave.removeAll { it.id == leave.id }
        }
    }
}
