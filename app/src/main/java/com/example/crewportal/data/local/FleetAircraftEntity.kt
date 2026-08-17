package com.example.crewportal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fleet_aircraft")
data class FleetAircraftEntity(
    @PrimaryKey val registration: String,
    val label: String,
    val fullName: String,
    val routeClass: String,
    val configuration: String,
    val engineType: String,
    val age: String,
    val status: String,
    val deliveredAtEpochMillis: Long? = null,
    val sourceFlightId: String? = null
)
