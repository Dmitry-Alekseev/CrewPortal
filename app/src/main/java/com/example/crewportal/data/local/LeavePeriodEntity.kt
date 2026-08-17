package com.example.crewportal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leave_periods")
data class LeavePeriodEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val note: String
)
