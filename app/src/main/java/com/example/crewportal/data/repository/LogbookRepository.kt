package com.example.crewportal.data.repository

import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.local.LogbookEntryDao
import com.example.crewportal.data.local.LogbookEntryEntity
import kotlinx.coroutines.flow.Flow

class LogbookRepository(private val dao: LogbookEntryDao) {
    fun observeForFlight(flightId: String): Flow<LogbookEntryEntity?> = dao.observeByFlightId(flightId)
    fun observeAll(): Flow<List<LogbookEntryEntity>> = dao.observeAll()

    fun prefilled(flight: FlightEntity): LogbookEntryEntity = LogbookEntryEntity(
        flightId = flight.id,
        date = flight.departureDateTime.take(10),
        flightNumber = flight.flightNumber,
        departurePlace = flight.departureIata,
        departureTime = flight.departureDateTime.substringAfter('T').take(5),
        arrivalPlace = flight.arrivalIata,
        arrivalTime = flight.arrivalDateTime.substringAfter('T').take(5),
        aircraftType = flight.aircraftFullName,
        registration = flight.registration,
        picName = "Dmitrii Alekseev",
        pilotFunction = "PIC",
        totalTimeMinutes = flight.durationMinutes,
        picMinutes = flight.durationMinutes,
        sicMinutes = 0,
        nightMinutes = 0,
        ifrMinutes = flight.durationMinutes,
        instrumentMinutes = 0,
        takeoffsDay = 1,
        takeoffsNight = 0,
        landingsDay = 1,
        landingsNight = 0,
        approaches = 1,
        remarks = "",
        signatureName = "",
        certifiedAtEpochMillis = null,
        updatedAtEpochMillis = System.currentTimeMillis()
    )

    suspend fun saveDraft(entry: LogbookEntryEntity) {
        require(entry.certifiedAtEpochMillis == null) { "Certified entries cannot be edited" }
        dao.upsert(entry.copy(updatedAtEpochMillis = System.currentTimeMillis()))
    }

    suspend fun certify(entry: LogbookEntryEntity) {
        require(entry.signatureName.isNotBlank()) { "Signature name is required" }
        require(entry.registration.isNotBlank() && entry.registration != "TBA") { "Aircraft registration is required" }
        require(entry.totalTimeMinutes > 0) { "Total time must be greater than zero" }
        val now = System.currentTimeMillis()
        dao.upsert(entry.copy(certifiedAtEpochMillis = now, updatedAtEpochMillis = now))
    }
}
