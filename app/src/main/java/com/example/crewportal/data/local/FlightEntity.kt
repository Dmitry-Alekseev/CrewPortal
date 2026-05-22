package com.example.crewportal.data.local

import androidx.room.Entity
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
    val isRegistered: Boolean = false,
    val isCompleted: Boolean = false,
    val isFlightTimeAdded: Boolean = false
)
