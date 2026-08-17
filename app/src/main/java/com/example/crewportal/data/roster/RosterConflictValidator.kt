package com.example.crewportal.data.roster

import com.example.crewportal.data.leave.LeaveDatabase
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.util.airportLocalEpochMillis
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** Final guard for linked mandatory events before a generated roster is persisted. */
object RosterConflictValidator {
    private val groundQualificationTypes = setOf("SIMULATOR", "MEDICAL", "SAFETY")
    private const val MIN_REST_MILLIS = 12 * 60 * 60 * 1000L

    fun errors(month: YearMonth, roster: List<FlightEntity>): List<String> = buildList {
        roster.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys.forEach {
            add("Duplicate roster id: $it")
        }

        val linkedGroups = roster.filter { it.eventGroupId.isNotBlank() }.groupBy { it.eventGroupId }
        linkedGroups.forEach { (groupId, rows) ->
            val groundRows = rows.filter { it.dutyType in groundQualificationTypes }
            if (groundRows.isNotEmpty()) validateGroundGroup(month, groupId, groundRows, roster, this)

            val lineRows = rows.filter { it.lineCheckRole.isNotBlank() }
            if (lineRows.isNotEmpty()) {
                if (lineRows.any { it.dutyType != "FLIGHT" }) add("Line Check $groupId is not attached to an operating flight")
                if (lineRows.any { it.lineCheckRole == "INSTRUCTOR" && it.flightTimeCreditEligible }) {
                    add("Instructor Line Check $groupId incorrectly receives operating block credit")
                }
            }
        }

        roster.filter { duty ->
            duty.dutyType !in setOf("OFF", "STAY") && LeaveDatabase.leaveFor(localDate(duty)) != null
        }.forEach { add("${it.id} overlaps approved leave") }
    }

    private fun validateGroundGroup(
        month: YearMonth,
        groupId: String,
        rows: List<FlightEntity>,
        roster: List<FlightEntity>,
        errors: MutableList<String>
    ) {
        val ordered = rows.sortedBy { it.eventDayIndex }
        if (ordered.any { it.flightTimeCreditEligible || it.durationMinutes != 0 }) {
            errors += "Ground qualification $groupId incorrectly receives flight-time credit"
        }
        if (ordered.map { it.eventDayIndex }.distinct().size != ordered.size) {
            errors += "Duplicate day in qualification group $groupId"
        }
        ordered.zipWithNext().forEach { (first, second) ->
            if (second.eventDayIndex != first.eventDayIndex + 1 || localDate(second) != localDate(first).plusDays(1)) {
                errors += "Qualification group $groupId is not continuous"
            }
        }
        if (ordered.any { it.eventTotalDays <= 0 || it.eventDayIndex !in 1..it.eventTotalDays }) {
            errors += "Invalid day metadata in qualification group $groupId"
        }

        val groupDates = ordered.map(::localDate).toSet()
        roster.filter { it.eventGroupId != groupId && it.dutyType in setOf("FLIGHT", "RESERVE") }
            .filter { localDate(it) in groupDates }
            .forEach { errors += "${it.id} is scheduled inside qualification group $groupId" }

        val start = ordered.minOfOrNull(::startEpoch) ?: return
        val end = ordered.maxOfOrNull(::endEpoch) ?: return
        roster.asSequence()
            .filter { it.eventGroupId != groupId && it.dutyType !in setOf("OFF", "STAY") }
            .filter { YearMonth.from(localDate(it)) == month }
            .forEach { other ->
                val otherStart = startEpoch(other)
                val otherEnd = endEpoch(other)
                val enoughRest = when {
                    otherEnd <= start -> start - otherEnd >= MIN_REST_MILLIS
                    end <= otherStart -> otherStart - end >= MIN_REST_MILLIS
                    else -> false
                }
                if (!enoughRest) errors += "Minimum rest is not met around $groupId and ${other.id}"
            }
    }

    private fun localDate(duty: FlightEntity): LocalDate = LocalDateTime.parse(duty.departureDateTime).toLocalDate()

    private fun startEpoch(duty: FlightEntity): Long = duty.departureEpochMillis
        ?: airportLocalEpochMillis(duty.departureDateTime, duty.departureIata)
        ?: error("Unknown departure zone for ${duty.id}")

    private fun endEpoch(duty: FlightEntity): Long = duty.arrivalEpochMillis
        ?: airportLocalEpochMillis(duty.arrivalDateTime, duty.arrivalIata)
        ?: error("Unknown arrival zone for ${duty.id}")
}
