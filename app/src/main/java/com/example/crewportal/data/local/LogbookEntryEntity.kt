package com.example.crewportal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Electronic pilot record based on EASA FCL.050/AMC1 fields.
 * A certified entry is immutable in the UI; amendments are stored by re-certifying a draft.
 */
@Entity(tableName = "electronic_logbook_entries")
data class LogbookEntryEntity(
    @PrimaryKey val flightId: String,
    val date: String,
    val flightNumber: String,
    val departurePlace: String,
    val departureTime: String,
    val arrivalPlace: String,
    val arrivalTime: String,
    val aircraftType: String,
    val registration: String,
    val picName: String,
    val pilotFunction: String,
    val totalTimeMinutes: Int,
    val picMinutes: Int,
    val sicMinutes: Int,
    val nightMinutes: Int,
    val ifrMinutes: Int,
    val instrumentMinutes: Int,
    val takeoffsDay: Int,
    val takeoffsNight: Int,
    val landingsDay: Int,
    val landingsNight: Int,
    val approaches: Int,
    val remarks: String,
    val signatureName: String,
    val certifiedAtEpochMillis: Long?,
    val updatedAtEpochMillis: Long
)
