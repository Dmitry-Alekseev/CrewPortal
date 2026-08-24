package com.example.crewportal.data.repository

import android.content.Context
import com.example.crewportal.data.leave.LeaveDatabase
import com.example.crewportal.data.leave.LeavePeriod
import com.example.crewportal.data.local.LeavePeriodDao
import com.example.crewportal.data.local.LeavePeriodEntity
import com.example.crewportal.data.roster.LeaveRosterSyncScheduler
import java.time.LocalDate

class LeaveRepository(
    private val context: Context,
    private val dao: LeavePeriodDao
) {
    suspend fun initialize(scheduleExistingApprovalSync: Boolean = false) {
        dao.insertSeed(LeaveDatabase.seedPeriods().map { it.toEntity() })
        refreshCache()
        if (scheduleExistingApprovalSync && LeaveDatabase.approvedPersonalLeave.isNotEmpty()) {
            LeaveRosterSyncScheduler.schedule(context, replaceExisting = false)
        }
    }

    suspend fun addPersonalLeave(period: LeavePeriod) {
        dao.upsert(period.toEntity())
        refreshCache()
        // Approval is persisted immediately, while the roster rewrite is durable background work.
        // WorkManager keeps the five-minute company-planning delay even if the screen/app closes.
        LeaveRosterSyncScheduler.schedule(context)
    }

    private suspend fun refreshCache() {
        val assignedIds = LeaveDatabase.seedPeriods().mapTo(mutableSetOf()) { it.id }
        val personal = dao.getAllOnce()
            .filterNot { it.id in assignedIds }
            .filter { it.status.equals("APPROVED", ignoreCase = true) }
            .map { it.toModel() }
        LeaveDatabase.replacePersistedPersonalLeave(personal)
    }
}

private fun LeavePeriod.toEntity() = LeavePeriodEntity(id, type, title, start.toString(), end.toString(), status, note)
private fun LeavePeriodEntity.toModel() = LeavePeriod(
    id, type, title, LocalDate.parse(startDate), LocalDate.parse(endDate), status, note
)
