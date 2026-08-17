package com.example.crewportal.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.crewportal.data.local.FlightEntity
import java.time.LocalDateTime

object RosterNotificationScheduler {
    fun scheduleRoster(context: Context, flights: List<FlightEntity>) {
        flights.filter { it.dutyType == "FLIGHT" && !it.isCompleted }.forEach { flight ->
            scheduleFlightEvent(context, flight, "registration", flight.departureDateTime, -24 * 60, "Registration open", "${flight.flightNumber} ${flight.departureIata}-${flight.arrivalIata}: registration window is open.", "details/${flight.id}")
            if (shouldScheduleAirportAssignment(flight, flights)) {
                scheduleFlightEvent(context, flight, "gate", flight.departureDateTime, -3 * 60, "Airport assignment", "${flight.flightNumber} ${flight.departureIata}-${flight.arrivalIata}: gate / stand assignment is available.", "details/${flight.id}")
            } else {
                cancelFlightEvent(context, flight, "gate")
            }
            scheduleFlightEvent(context, flight, "report", flight.departureDateTime, -120, "Duty reminder", "${flight.flightNumber}: report time is approaching.", "details/${flight.id}")
        }
    }

    private fun shouldScheduleAirportAssignment(flight: FlightEntity, roster: List<FlightEntity>): Boolean {
        if (flight.departureIata == "BKK") return true
        val departure = LocalDateTime.parse(flight.departureDateTime)
        val sameDutyReturn = roster.any { other ->
            other.id != flight.id &&
                other.dutyType == "FLIGHT" &&
                LocalDateTime.parse(other.departureDateTime).toLocalDate() == departure.toLocalDate() &&
                LocalDateTime.parse(other.departureDateTime).isBefore(departure) &&
                other.arrivalIata == flight.departureIata &&
                other.departureIata == flight.arrivalIata &&
                other.aircraftLabel == flight.aircraftLabel
        }
        if (sameDutyReturn) return false
        return flight.durationMinutes >= 360
    }

    private fun cancelFlightEvent(context: Context, flight: FlightEntity, suffix: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = (flight.id + suffix).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, RosterAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleFlightEvent(
        context: Context,
        flight: FlightEntity,
        suffix: String,
        localDateTime: String,
        offsetMinutes: Int,
        title: String,
        message: String,
        destination: String
    ) {
        val triggerAt = airportLocalToEpochMillis(localDateTime, flight.departureIata) + offsetMinutes * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = (flight.id + suffix).hashCode()
        val intent = Intent(context, RosterAlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("notificationId", requestCode)
            putExtra("destination", destination)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private fun airportLocalToEpochMillis(localDateTime: String, iata: String): Long {
        return airportLocalEpochMillis(localDateTime, iata)
            ?: LocalDateTime.parse(localDateTime).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
