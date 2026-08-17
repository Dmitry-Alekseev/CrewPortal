package com.example.crewportal.data.repository

import com.example.crewportal.data.leave.LeaveDatabase
import com.example.crewportal.data.leave.LeavePeriod
import com.example.crewportal.data.local.LeavePeriodDao
import com.example.crewportal.data.local.LeavePeriodEntity
import java.time.LocalDate

class LeaveRepository(private val dao: LeavePeriodDao) {
    suspend fun initialize() {
        dao.insertSeed(LeaveDatabase.assignedLeave.map { it.toEntity() })
        refreshCache()
    }

    suspend fun addPersonalLeave(period: LeavePeriod) {
        dao.upsert(period.toEntity())
        refreshCache()
    }

    private suspend fun refreshCache() {
        val assignedIds = LeaveDatabase.assignedLeave.mapTo(mutableSetOf()) { it.id }
        val personal = dao.getAllOnce().filterNot { it.id in assignedIds }.map { it.toModel() }
        LeaveDatabase.replacePersistedPersonalLeave(personal)
    }
}

private fun LeavePeriod.toEntity() = LeavePeriodEntity(id, type, title, start.toString(), end.toString(), status, note)
private fun LeavePeriodEntity.toModel() = LeavePeriod(
    id, type, title, LocalDate.parse(startDate), LocalDate.parse(endDate), status, note
)
