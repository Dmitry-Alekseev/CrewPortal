package com.example.crewportal.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey val id: String,
    val airline: String,
    val flightNumber: String,
    val aircraftLabel: String,
    val aircraftFullName: String,
    val registration: String,
    val status: String,
    val departureIata: String,
    val departureIcao: String,
    val departureCity: String,
    val departureAirport: String,
    val arrivalIata: String,
    val arrivalIcao: String,
    val arrivalCity: String,
    val arrivalAirport: String,
    val departureDateTime: String,
    val arrivalDateTime: String,
    val durationMinutes: Int,
    val dutyType: String = "FLIGHT",
    val dutyNote: String = "",
    val isRegistered: Boolean = false,
    val isCompleted: Boolean = false,
    val isFlightTimeAdded: Boolean = false,
    val registrationNotified: Boolean = false,
    val changeNotified: Boolean = false,
    val gate: String = "Pending",
    val stand: String = "Pending",
    val terminal: String = "Pending",
    val airportAssignmentNotified: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") val departureEpochMillis: Long? = null,
    @ColumnInfo(defaultValue = "NULL") val arrivalEpochMillis: Long? = null,
    @ColumnInfo(defaultValue = "0") val isAircraftDelivery: Boolean = false,
    @ColumnInfo(defaultValue = "''") val deliveryAircraftType: String = "",
    @ColumnInfo(defaultValue = "0") val deliveryProcessed: Boolean = false
)
