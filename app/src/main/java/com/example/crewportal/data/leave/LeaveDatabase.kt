package com.example.crewportal.data.leave

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class LeavePeriod(
    val id: String,
    val type: String,
    val title: String,
    val start: LocalDate,
    val end: LocalDate,
    val status: String = "APPROVED",
    val note: String = ""
) {
    val days: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1
    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}

data class LeaveBalance(
    val annualUsed: Int,
    val annualTotal: Int,
    val personalUsed: Int,
    val personalTotal: Int,
    val sickDays: Int
)

object LeaveDatabase {
    val assignedLeave = listOf(
        LeavePeriod(
            id = "annual-mar-2026",
            type = "ANNUAL_LEAVE",
            title = "Annual Leave",
            start = LocalDate.of(2026, 3, 12),
            end = LocalDate.of(2026, 4, 5),
            note = "Company assigned annual leave"
        ),
        LeavePeriod(
            id = "annual-sep-2026",
            type = "ANNUAL_LEAVE",
            title = "Annual Leave",
            start = LocalDate.of(2026, 9, 14),
            end = LocalDate.of(2026, 10, 8),
            note = "Company assigned annual leave"
        )
    )

    // User-requested leave is created through Leave Management.
    // Demo personal/sick records are intentionally empty in 2.0.0 so the current test roster is not overwritten.
    val approvedPersonalLeave = emptyList<LeavePeriod>()

    val closedSickLeaves = emptyList<LeavePeriod>()

    fun allApproved(): List<LeavePeriod> = assignedLeave + approvedPersonalLeave + closedSickLeaves

    fun leaveFor(date: LocalDate): LeavePeriod? = allApproved().firstOrNull { it.contains(date) }

    fun leaveForMonth(month: YearMonth): List<LeavePeriod> {
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        return allApproved().filter { !it.end.isBefore(first) && !it.start.isAfter(last) }
    }

    fun leaveDaysInMonth(month: YearMonth): Int {
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        return allApproved().sumOf { period ->
            val s = maxOf(period.start, first)
            val e = minOf(period.end, last)
            if (e.isBefore(s)) 0 else ChronoUnit.DAYS.between(s, e).toInt() + 1
        }
    }

    fun adjustedMonthlyTargetMinutes(month: YearMonth, baseTargetMinutes: Int = 80 * 60): Int {
        val totalDays = month.lengthOfMonth()
        val unavailableDays = leaveDaysInMonth(month).coerceAtMost(totalDays)
        val activeDays = totalDays - unavailableDays
        return ((baseTargetMinutes.toDouble() * activeDays.toDouble()) / totalDays.toDouble()).toInt()
    }

    fun balance(): LeaveBalance {
        val annualUsed = assignedLeave.sumOf { it.days }.coerceAtMost(40)
        val personalUsed = approvedPersonalLeave.sumOf { it.days }.coerceAtMost(40)
        val sickUsed = closedSickLeaves.sumOf { it.days }
        return LeaveBalance(
            annualUsed = annualUsed,
            annualTotal = 40,
            personalUsed = personalUsed,
            personalTotal = 40,
            sickDays = sickUsed
        )
    }
}
