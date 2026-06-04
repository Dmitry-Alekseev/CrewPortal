package com.example.crewportal.data.repository

import android.content.Context
import com.example.crewportal.data.airport.AirportAssignmentPool
import com.example.crewportal.data.fleet.AircraftPool
import com.example.crewportal.data.local.FlightDao
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.roster.RosterGenerator
import com.example.crewportal.data.roster.RosterChangeEngine
import com.example.crewportal.util.NotificationHelper
import com.example.crewportal.util.RosterNotificationScheduler
import com.example.crewportal.util.canRegister
import com.example.crewportal.util.hasArrived
import com.example.crewportal.util.parseLocalDateTime
import com.example.crewportal.util.shouldShowRegistrationButton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import org.json.JSONObject

class FlightRepository(
    private val context: Context,
    private val flightDao: FlightDao,
    private val preferencesRepository: PreferencesRepository
) {
    fun observeFlights(): Flow<List<FlightEntity>> = flightDao.observeAll()
    fun observeCompleted(): Flow<List<FlightEntity>> = flightDao.observeCompleted()
    fun observeFlight(id: String): Flow<FlightEntity?> = flightDao.observeById(id)

    suspend fun loadScheduleFromAssetsIfNeeded() {
        if (flightDao.count() > 0) return
        val currentMonthRoster = RosterGenerator.generateForMonth(YearMonth.now())
        flightDao.insertAll(currentMonthRoster)
        RosterNotificationScheduler.scheduleRoster(context, currentMonthRoster)
    }

    suspend fun refreshBuiltInRosterOnAppUpdate(versionName: String) {
        val installedVersion = preferencesRepository.installedAppVersion.first()
        if (installedVersion != versionName) {
            // App updates must never overwrite or hide the active local/generated roster.
            // Keep the review/accept state; only refresh the installed version marker and reopen
            // the one-time generator test for the new build.
            preferencesRepository.setInstalledAppVersion(versionName)
            preferencesRepository.setSecretRosterGeneratorUsed(false)
        }

        restoreGeneratedRosterStateIfNeeded()
        // Crew Portal 2.1.x: do not auto-generate the next roster on app start/update.
        // The hidden 5-tap flow remains the only manual test trigger.
    }

    private suspend fun restoreGeneratedRosterStateIfNeeded() {
        val nextMonth = YearMonth.from(LocalDate.now()).plusMonths(1)
        val prefix = "%04d-%02d".format(nextMonth.year, nextMonth.monthValue)
        val hasGeneratedNextMonth = flightDao.getAllOnce().any { it.departureDateTime.startsWith(prefix) }

        if (hasGeneratedNextMonth && !preferencesRepository.nextMonthRosterPrepared.first()) {
            preferencesRepository.setNextMonthRosterPrepared(true)
        }
    }

    private suspend fun prepareNextMonthRosterIfDue() {
        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today)
        val triggerDate = currentMonth.atEndOfMonth().minusDays(6)
        if (today.isBefore(triggerDate)) return

        val nextMonth = currentMonth.plusMonths(1)
        val prefix = "%04d-%02d".format(nextMonth.year, nextMonth.monthValue)
        val snapshot = flightDao.getAllOnce()
        val alreadyGenerated = snapshot.any { it.departureDateTime.startsWith(prefix) }
        if (!alreadyGenerated) {
            val generated = RosterGenerator.generateForMonth(nextMonth)
            flightDao.insertAll(generated)
            RosterNotificationScheduler.scheduleRoster(context, snapshot + generated)
            preferencesRepository.resetNextMonthRosterDecision()
            preferencesRepository.setNextMonthRosterPrepared(true)
            NotificationHelper.show(
                context,
                "Roster ready for review",
                "Generated roster for ${nextMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${nextMonth.year} is ready in Calendar.",
                2_008_000 + nextMonth.monthValue
            )
        } else if (!preferencesRepository.nextMonthRosterPrepared.first()) {
            preferencesRepository.setNextMonthRosterPrepared(true)
        }
    }

    suspend fun reloadScheduleFromAssets() {
        loadScheduleFromAssets(clearExisting = true, preserveExistingState = true)
    }

    private suspend fun loadScheduleFromAssets(clearExisting: Boolean, preserveExistingState: Boolean = false) {
        val generated = RosterGenerator.generateForMonth(YearMonth.now())
        if (clearExisting) flightDao.clearAll()
        flightDao.insertAll(generated)
        RosterNotificationScheduler.scheduleRoster(context, generated)
    }

    suspend fun syncRosterFromGitHub(): Boolean {
        // Crew Portal 2.1: roster JSON is no longer an active source.
        // Manual refresh only updates local completion/registration/assignment state.
        refreshCompletedFlights(showNotifications = true)
        return true
    }

    private suspend fun loadScheduleFromJson(json: String, clearExisting: Boolean) {
        loadScheduleFromJson(json, clearExisting, preserveExistingState = false)
    }

    private suspend fun loadScheduleFromJson(json: String, clearExisting: Boolean, preserveExistingState: Boolean) {
        val existingById = if (preserveExistingState) flightDao.getAllOnce().associateBy { it.id } else emptyMap()
        if (clearExisting) flightDao.clearAll()
        val root = JSONObject(json)
        val array = root.getJSONArray("flights")
        val flights = buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val id = item.getString("id")
                val previous = existingById[id]
                val incomingRegistration = item.optString("registration", "TBA")
                val incomingGate = item.optString("gate", "Pending")
                val incomingStand = item.optString("stand", "Pending")
                val incomingTerminal = item.optString("terminal", "Pending")
                add(
                    FlightEntity(
                        id = id,
                        airline = item.getString("airline"),
                        flightNumber = item.getString("flightNumber"),
                        aircraftLabel = item.getString("aircraftLabel"),
                        aircraftFullName = item.getString("aircraftFullName"),
                        registration = when {
                            previous != null && previous.registration != "TBA" -> previous.registration
                            incomingRegistration.isNotBlank() -> incomingRegistration
                            else -> "TBA"
                        },
                        status = item.getString("status"),
                        departureIata = item.getString("departureIata"),
                        departureIcao = item.getString("departureIcao"),
                        departureCity = item.getString("departureCity"),
                        departureAirport = item.getString("departureAirport"),
                        arrivalIata = item.getString("arrivalIata"),
                        arrivalIcao = item.getString("arrivalIcao"),
                        arrivalCity = item.getString("arrivalCity"),
                        arrivalAirport = item.getString("arrivalAirport"),
                        departureDateTime = item.getString("departureDateTime"),
                        arrivalDateTime = item.getString("arrivalDateTime"),
                        durationMinutes = item.getInt("durationMinutes"),
                        dutyType = item.optString("dutyType", "FLIGHT"),
                        dutyNote = item.optString("dutyNote", ""),
                        isRegistered = previous?.isRegistered ?: item.optBoolean("isRegistered", false),
                        isCompleted = previous?.isCompleted ?: item.optBoolean("isCompleted", false),
                        isFlightTimeAdded = previous?.isFlightTimeAdded ?: item.optBoolean("isFlightTimeAdded", false),
                        registrationNotified = previous?.registrationNotified ?: item.optBoolean("registrationNotified", false),
                        changeNotified = item.optBoolean("changeNotified", previous?.changeNotified ?: false),
                        gate = when {
                            previous != null && previous.gate != "Pending" -> previous.gate
                            incomingGate.isNotBlank() -> incomingGate
                            else -> "Pending"
                        },
                        stand = when {
                            previous != null && previous.stand != "Pending" -> previous.stand
                            incomingStand.isNotBlank() -> incomingStand
                            else -> "Pending"
                        },
                        terminal = when {
                            previous != null && previous.terminal != "Pending" -> previous.terminal
                            incomingTerminal.isNotBlank() -> incomingTerminal
                            else -> "Pending"
                        },
                        airportAssignmentNotified = previous?.airportAssignmentNotified ?: item.optBoolean("airportAssignmentNotified", false)
                    )
                )
            }
        }
        flightDao.insertAll(flights)
        RosterNotificationScheduler.scheduleRoster(context, flights)
    }

    suspend fun registerFlight(id: String) {
        val flight = flightDao.getAllOnce().firstOrNull { it.id == id } ?: return
        assignAircraftIfNeeded(flight)
        val updatedFlight = flightDao.getAllOnce().firstOrNull { it.id == id } ?: flight
        propagateSameDutyAircraftRegistration(updatedFlight)
        val all = flightDao.getAllOnce()
        if (canAssignAirportPosition(updatedFlight, all)) assignAirportPositionIfNeeded(updatedFlight)
        flightDao.markRegistered(id)
        NotificationHelper.show(
            context,
            "Registration completed",
            "${flight.flightNumber} ${flight.departureIata}-${flight.arrivalIata}: crew registration completed successfully.",
            id.hashCode()
        )
    }

    suspend fun refreshCompletedFlights(showNotifications: Boolean = false) {
        val initialSnapshot = flightDao.getAllOnce()
        val automaticChange = RosterChangeEngine.applyChangeIfDue(initialSnapshot)
        val rosterSnapshot = if (automaticChange != null) {
            flightDao.clearAll()
            flightDao.insertAll(automaticChange.updatedRoster)
            RosterNotificationScheduler.scheduleRoster(context, automaticChange.updatedRoster)
            if (showNotifications) {
                NotificationHelper.show(
                    context,
                    automaticChange.notificationTitle,
                    automaticChange.notificationBody,
                    automaticChange.notificationId
                )
            }
            automaticChange.updatedRoster
        } else {
            initialSnapshot
        }
        RosterNotificationScheduler.scheduleRoster(context, rosterSnapshot)
        normalizeSameDutyAircraftRegistrations(rosterSnapshot)
        rosterSnapshot.forEach { flight ->
            if (flight.dutyType == "FLIGHT" && shouldShowRegistrationButton(flight.departureIata, flight.durationMinutes) && canRegister(flight.departureDateTime, flight.isCompleted, flight.departureIata)) {
                assignAircraftIfNeeded(flight)
                if (!flight.registrationNotified) {
                    flightDao.markRegistrationNotified(flight.id)
                    if (showNotifications) {
                        NotificationHelper.show(
                            context,
                            "Registration is open",
                            "${flight.flightNumber} ${flight.departureIata}-${flight.arrivalIata}: aircraft assigned, check-in is now available.",
                            flight.id.hashCode()
                        )
                    }
                }
            }
            if (flight.dutyType == "FLIGHT" && canAssignAirportPosition(flight, rosterSnapshot)) {
                val assignment = assignAirportPositionIfNeeded(flight)
                if (!flight.airportAssignmentNotified) {
                    flightDao.markAirportAssignmentNotified(flight.id)
                    if (showNotifications) {
                        NotificationHelper.show(
                            context,
                            "Airport assignment updated",
                            "${flight.flightNumber} ${flight.departureIata}-${flight.arrivalIata}: ${assignment.displayValue}.",
                            flight.id.hashCode() + 30_000
                        )
                    }
                }
            }
            if (flight.dutyType == "FLIGHT" && !flight.isFlightTimeAdded && hasArrived(flight.arrivalDateTime)) {
                flightDao.markCompletedAndAdded(flight.id)
                preferencesRepository.addFlightTime(flight.durationMinutes, flight.aircraftLabel)
                if (showNotifications) {
                    NotificationHelper.show(
                        context,
                        "Flight completed",
                        "${flight.flightNumber} completed. ${flight.durationMinutes / 60}h ${flight.durationMinutes % 60}m added to your flight time.",
                        flight.id.hashCode() + 10_000
                    )
                }
            }
        }
    }

    suspend fun generateJuneRosterTest() {
        if (preferencesRepository.secretRosterGeneratorUsed.first()) return
        val targetMonth = YearMonth.now().plusMonths(1)
        val generated = RosterGenerator.generateForMonth(targetMonth)
        val prefix = "%04d-%02d".format(targetMonth.year, targetMonth.monthValue)
        val current = flightDao.getAllOnce()
        val preserved = current.filterNot { it.departureDateTime.startsWith(prefix) }
        val merged = (preserved + generated).sortedBy { it.departureDateTime }
        flightDao.clearAll()
        flightDao.insertAll(merged)
        RosterNotificationScheduler.scheduleRoster(context, merged)
        preferencesRepository.setNextMonthRosterPrepared(true)
        preferencesRepository.setNextMonthRosterDecision(reviewed = false, enhancedTarget = false)
        preferencesRepository.setSecretRosterGeneratorUsed(true)
        NotificationHelper.show(
            context,
            "Generated roster prepared",
            "${targetMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${targetMonth.year} roster is ready for calendar review.",
            2_100_000 + targetMonth.monthValue
        )
    }

    suspend fun simulateRosterChange() {
        val next = flightDao.getAllOnce().firstOrNull { it.dutyType == "FLIGHT" && !it.isCompleted } ?: return
        NotificationHelper.show(
            context,
            "Roster updated",
            "${next.flightNumber} ${next.departureIata}-${next.arrivalIata}: departure time monitoring active. Reason: ${com.example.crewportal.util.disruptionReason(next.departureIata + next.arrivalIata)}.",
            next.id.hashCode() + 20_000
        )
    }


    private fun canAssignAirportPosition(flight: FlightEntity, roster: List<FlightEntity>): Boolean {
        if (flight.dutyType != "FLIGHT" || flight.isCompleted) return false
        if (!shouldHaveAirportAssignment(flight, roster)) return false
        val departure = parseLocalDateTime(flight.departureDateTime)
        val now = LocalDateTime.now()
        return !now.isBefore(departure.minusHours(3))
    }

    private fun shouldHaveAirportAssignment(flight: FlightEntity, roster: List<FlightEntity>): Boolean {
        if (flight.departureIata == "BKK") return true
        val departure = parseLocalDateTime(flight.departureDateTime)
        val sameDutyInbound = roster.any { other ->
            other.id != flight.id &&
                other.dutyType == "FLIGHT" &&
                parseLocalDateTime(other.departureDateTime).toLocalDate() == departure.toLocalDate() &&
                parseLocalDateTime(other.departureDateTime).isBefore(departure) &&
                other.arrivalIata == flight.departureIata &&
                other.departureIata == flight.arrivalIata &&
                other.aircraftLabel == flight.aircraftLabel
        }
        if (sameDutyInbound) return false

        // Long-haul departures after a real layover/night stop still receive airport assignment.
        return flight.durationMinutes >= 360
    }

    private suspend fun assignAirportPositionIfNeeded(flight: FlightEntity): com.example.crewportal.data.airport.AirportAssignment {
        if ((flight.gate != "Pending" || flight.stand != "Pending") && flight.terminal != "Pending") {
            return com.example.crewportal.data.airport.AirportAssignment(flight.gate, flight.stand, flight.terminal)
        }
        val assignment = AirportAssignmentPool.assign(
            departureIata = flight.departureIata,
            aircraftLabel = flight.aircraftLabel,
            durationMinutes = flight.durationMinutes,
            seed = flight.id + flight.departureDateTime
        )
        flightDao.assignAirportPosition(flight.id, assignment.gate, assignment.stand, assignment.terminal)
        return assignment
    }

    private suspend fun assignAircraftIfNeeded(flight: FlightEntity) {
        if (flight.registration != "TBA" || flight.dutyType != "FLIGHT") return
        val all = flightDao.getAllOnce()
        val departureDate = parseLocalDateTime(flight.departureDateTime).toLocalDate()
        val paired = all.firstOrNull { other ->
            other.id != flight.id && other.dutyType == "FLIGHT" && other.registration != "TBA" &&
                parseLocalDateTime(other.departureDateTime).toLocalDate() == departureDate &&
                other.departureIata == flight.arrivalIata && other.arrivalIata == flight.departureIata &&
                other.aircraftLabel == flight.aircraftLabel
        }
        if (paired != null) {
            flightDao.assignRegistration(flight.id, paired.registration)
            return
        }

        val routeClass = when {
            flight.durationMinutes >= 360 || flight.aircraftLabel.startsWith("A350") -> "LONG"
            flight.durationMinutes >= 180 || flight.aircraftLabel.startsWith("A330") -> "MEDIUM"
            else -> "SHORT"
        }
        val assigned = AircraftPool.assignFor(flight.aircraftLabel, routeClass, flight.id)
        flightDao.assignRegistration(flight.id, assigned.registration)
    }

    private suspend fun normalizeSameDutyAircraftRegistrations(roster: List<FlightEntity>) {
        roster.filter { it.dutyType == "FLIGHT" && it.registration != "TBA" }
            .forEach { assigned -> propagateSameDutyAircraftRegistration(assigned) }
    }

    private suspend fun propagateSameDutyAircraftRegistration(source: FlightEntity) {
        if (source.dutyType != "FLIGHT" || source.registration == "TBA") return
        val all = flightDao.getAllOnce()
        val sourceDeparture = parseLocalDateTime(source.departureDateTime)
        all.filter { candidate ->
            candidate.id != source.id &&
                candidate.dutyType == "FLIGHT" &&
                candidate.registration == "TBA" &&
                candidate.aircraftLabel == source.aircraftLabel &&
                parseLocalDateTime(candidate.departureDateTime).toLocalDate() == sourceDeparture.toLocalDate() &&
                isSameDutyTurnaroundPair(source, candidate)
        }.forEach { paired ->
            flightDao.assignRegistration(paired.id, source.registration)
        }
    }

    private fun isSameDutyTurnaroundPair(first: FlightEntity, second: FlightEntity): Boolean {
        val firstDeparture = parseLocalDateTime(first.departureDateTime)
        val secondDeparture = parseLocalDateTime(second.departureDateTime)
        val minutesBetween = java.time.temporal.ChronoUnit.MINUTES.between(firstDeparture, secondDeparture).let { kotlin.math.abs(it) }
        return minutesBetween <= 12 * 60 &&
            ((first.arrivalIata == second.departureIata && first.departureIata == second.arrivalIata) ||
                (second.arrivalIata == first.departureIata && second.departureIata == first.arrivalIata))
    }
}
