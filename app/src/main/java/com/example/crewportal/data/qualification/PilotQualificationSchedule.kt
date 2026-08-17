package com.example.crewportal.data.qualification

import java.time.LocalDate
import java.time.YearMonth

data class QualificationValidity(
    val title: String,
    val completed: LocalDate,
    val nextDue: LocalDate,
    val cadenceMonths: Long
)

data class ScheduledQualificationDay(
    val date: LocalDate,
    val dutyType: String,
    val title: String,
    val note: String,
    val eventGroupId: String,
    val dayIndex: Int,
    val totalDays: Int,
    val lineCheckRole: String = ""
)

/**
 * One source of truth for Profile validity dates and fixed generator events.
 * Recurrence is based on the project's existing 2026 records: medical, simulator and line
 * check repeat every six months. Profile and roster generation both read these records.
 */
object PilotQualificationSchedule {
    val medical = QualificationValidity(
        title = "Class 1 Medical Certificate",
        completed = LocalDate.of(2026, 8, 11),
        nextDue = LocalDate.of(2027, 2, 11),
        cadenceMonths = 6
    )
    val simulator = QualificationValidity(
        title = "Simulator Recurrent Session",
        completed = LocalDate.of(2026, 7, 18),
        nextDue = LocalDate.of(2027, 1, 18),
        cadenceMonths = 6
    )
    val lineCheck = QualificationValidity(
        title = "Line Check",
        completed = LocalDate.of(2026, 8, 6),
        nextDue = LocalDate.of(2027, 2, 6),
        cadenceMonths = 6
    )
    val sepLand = QualificationValidity(
        title = "Safety & Emergency Procedures — Land",
        completed = LocalDate.of(2026, 4, 6),
        nextDue = LocalDate.of(2026, 10, 6),
        cadenceMonths = 6
    )
    val sepWater = QualificationValidity(
        title = "Safety & Emergency Procedures — Water",
        completed = LocalDate.of(2026, 4, 8),
        nextDue = LocalDate.of(2026, 10, 8),
        cadenceMonths = 6
    )

    /** Fixed service events are returned before ordinary flight generation reserves dates. */
    fun eventsForMonth(month: YearMonth): List<ScheduledQualificationDay> = buildList {
        addAll(recurringMultiDay(
            month = month,
            firstStart = LocalDate.of(2026, 7, 16),
            cadenceMonths = 6,
            dutyType = "SIMULATOR",
            title = "Simulator Training",
            totalDays = 3,
            note = "Six-month simulator recurrent training"
        ))
        addAll(recurringMultiDay(
            month = month,
            firstStart = LocalDate.of(2026, 8, 10),
            cadenceMonths = 6,
            dutyType = "MEDICAL",
            title = "Medical Examination",
            totalDays = 2,
            note = "Class 1 medical examination"
        ))
        addAll(recurringSingleDay(
            month = month,
            firstDate = LocalDate.of(2026, 8, 6),
            cadenceMonths = 6,
            dutyType = "LINE_CHECK",
            title = "Line Check",
            note = "Line check on an operating pairing",
            lineCheckRole = "INSTRUCTOR"
        ))
        addAll(recurringSingleDay(month, LocalDate.of(2026, 10, 6), 6, "SAFETY", "SEP Land", "Safety & Emergency Procedures — Land"))
        addAll(recurringSingleDay(month, LocalDate.of(2026, 10, 8), 6, "SAFETY", "SEP Water", "Safety & Emergency Procedures — Water"))
    }.sortedBy { it.date }

    private fun recurringMultiDay(
        month: YearMonth,
        firstStart: LocalDate,
        cadenceMonths: Long,
        dutyType: String,
        title: String,
        totalDays: Int,
        note: String
    ): List<ScheduledQualificationDay> {
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        var occurrence = firstStart
        while (occurrence.plusDays((totalDays - 1).toLong()).isBefore(monthStart)) {
            occurrence = occurrence.plusMonths(cadenceMonths)
        }
        val result = mutableListOf<ScheduledQualificationDay>()
        while (!occurrence.isAfter(monthEnd)) {
            val groupId = "$dutyType-${occurrence}"
            (0 until totalDays).forEach { offset ->
                val date = occurrence.plusDays(offset.toLong())
                if (YearMonth.from(date) == month) {
                    result += ScheduledQualificationDay(
                        date = date,
                        dutyType = dutyType,
                        title = "$title — Day ${offset + 1}/$totalDays",
                        note = "$note • Day ${offset + 1}/$totalDays",
                        eventGroupId = groupId,
                        dayIndex = offset + 1,
                        totalDays = totalDays
                    )
                }
            }
            occurrence = occurrence.plusMonths(cadenceMonths)
        }
        return result
    }

    private fun recurringSingleDay(
        month: YearMonth,
        firstDate: LocalDate,
        cadenceMonths: Long,
        dutyType: String,
        title: String,
        note: String,
        lineCheckRole: String = ""
    ): List<ScheduledQualificationDay> {
        var occurrence = firstDate
        while (occurrence.isBefore(month.atDay(1))) occurrence = occurrence.plusMonths(cadenceMonths)
        val result = mutableListOf<ScheduledQualificationDay>()
        while (!occurrence.isAfter(month.atEndOfMonth())) {
            result += ScheduledQualificationDay(
                date = occurrence,
                dutyType = dutyType,
                title = title,
                note = note,
                eventGroupId = "$dutyType-${occurrence}",
                dayIndex = 1,
                totalDays = 1,
                lineCheckRole = lineCheckRole
            )
            occurrence = occurrence.plusMonths(cadenceMonths)
        }
        return result
    }
}
