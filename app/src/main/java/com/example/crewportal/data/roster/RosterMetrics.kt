package com.example.crewportal.data.roster

import com.example.crewportal.data.local.FlightEntity
import java.time.YearMonth

/** Shared roster totals used by Roster, Calendar-adjacent metrics and Payroll. */
object RosterMetrics {
    fun dutiesForMonth(roster: List<FlightEntity>, month: YearMonth): List<FlightEntity> {
        val prefix = "%04d-%02d".format(month.year, month.monthValue)
        return roster.filter { it.departureDateTime.startsWith(prefix) }
    }

    /**
     * Adds the authoritative block duration stored on each selected duty.
     * DEADHEAD, STAY, OFF and RESERVE never enter the total unless explicitly supplied in
     * [includedDutyTypes]. Completion is a separate filter so planned and achieved totals use
     * exactly the same arithmetic.
     */
    fun blockMinutes(
        roster: List<FlightEntity>,
        month: YearMonth,
        includedDutyTypes: Set<String> = setOf("FLIGHT"),
        completedOnly: Boolean = false
    ): Int = dutiesForMonth(roster, month)
        .asSequence()
        .filter { it.dutyType in includedDutyTypes }
        .filter { !completedOnly || it.isCompleted }
        .sumOf { it.durationMinutes }
}
