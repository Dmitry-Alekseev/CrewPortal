package com.example.crewportal.data.repository

import android.content.Context
import com.example.crewportal.data.airport.AirportAssignmentPool
import com.example.crewportal.data.fleet.AircraftPool
import com.example.crewportal.data.local.FlightDao
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.roster.RosterGenerator
import com.example.crewportal.util.NotificationHelper
import com.example.crewportal.util.RosterNotificationScheduler
import com.example.crewportal.util.canRegister
import com.example.crewportal.util.hasArrived
import com.example.crewportal.util.parseLocalDateTime
import com.example.crewportal.util.shouldShowRegistrationButton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class FlightRepository(
    private val context: Context,
    private val flightDao: FlightDao,
    private val preferencesRepository: PreferencesRepository
) {
    private val httpClient = OkHttpClient()
    private val githubRosterUrl = "https://raw.githubusercontent.com/Dmitry-Alekseev/CrewPortal/main/roster/current_roster.json"

    fun observeFlights(): Flow<List<FlightEntity>> = flightDao.observeAll()
    fun observeCompleted(): Flow<List<FlightEntity>> = flightDao.observeCompleted()
    fun observeFlight(id: String): Flow<FlightEntity?> = flightDao.observeById(id)

    suspend fun loadScheduleFromAssetsIfNeeded() {
        if (flightDao.count() > 0) return
        loadScheduleFromAssets(clearExisting = false)
    }

    suspend fun refreshBuiltInRosterOnAppUpdate(versionName: String) {
        val installedVersion = preferencesRepository.installedAppVersion.first()
        if (installedVersion != versionName) {
            loadScheduleFromAssets(clearExisting = true, preserveExistingState = true)
            preferencesRepository.setInstalledAppVersion(versionName)
        }
    }

    suspend fun reloadScheduleFromAssets() {
        loadScheduleFromAssets(clearExisting = true, preserveExistingState = true)
    }

    private suspend fun loadScheduleFromAssets(clearExisting: Boolean, preserveExistingState: Boolean = false) {
        val json = context.assets.open("schedule.json").bufferedReader().use { it.readText() }
        loadScheduleFromJson(json, clearExisting, preserveExistingState)
    }

    suspend fun syncRosterFromGitHub(): Boolean {
        return try {
            val request = Request.Builder().url(githubRosterUrl).build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || body.isBlank()) return false
            loadScheduleFromJson(body, clearExisting = true, preserveExistingState = true)
            refreshCompletedFlights()
            true
        } catch (_: Exception) {
            false
        }
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
        if (canAssignAirportPosition(flight)) assignAirportPositionIfNeeded(flight)
        flightDao.markRegistered(id)
        NotificationHelper.show(
            context,
            "Registration completed",
            "${flight.flightNumber} ${flight.departureIata}-${flight.arrivalIata}: crew registration completed successfully.",
            id.hashCode()
        )
    }

    suspend fun refreshCompletedFlights() {
        val rosterSnapshot = flightDao.getAllOnce()
        RosterNotificationScheduler.scheduleRoster(context, rosterSnapshot)
        rosterSnapshot.forEach { flight ->
            if (flight.dutyType == "FLIGHT" && shouldShowRegistrationButton(flight.departureIata, flight.durationMinutes) && canRegister(flight.departureDateTime, flight.isCompleted)) {
                assignAircraftIfNeeded(flight)
                if (!flight.registrationNotified) {
                    flightDao.markRegistrationNotified(flight.id)
                    NotificationHelper.show(
                        context,
                        "Registration is open",
                        "${flight.flightNumber} ${flight.departureIata}-${flight.arrivalIata}: aircraft assigned, check-in is now available.",
                        flight.id.hashCode()
                    )
                }
            }
            if (flight.dutyType == "FLIGHT" && canAssignAirportPosition(flight)) {
                val assignment = assignAirportPositionIfNeeded(flight)
                if (!flight.airportAssignmentNotified) {
                    flightDao.markAirportAssignmentNotified(flight.id)
                    NotificationHelper.show(
                        context,
                        "Airport assignment updated",
                        "${flight.flightNumber} ${flight.departureIata}-${flight.arrivalIata}: ${assignment.displayValue}.",
                        flight.id.hashCode() + 30_000
                    )
                }
            }
            if (flight.dutyType == "FLIGHT" && !flight.isFlightTimeAdded && hasArrived(flight.arrivalDateTime)) {
                flightDao.markCompletedAndAdded(flight.id)
                preferencesRepository.addFlightTime(flight.durationMinutes, flight.aircraftLabel)
                NotificationHelper.show(
                    context,
                    "Flight completed",
                    "${flight.flightNumber} completed. ${flight.durationMinutes / 60}h ${flight.durationMinutes % 60}m added to your flight time.",
                    flight.id.hashCode() + 10_000
                )
            }
        }
    }

    suspend fun generateJuneRosterTest() {
        val generated = RosterGenerator.generateJune2026()
        val current = flightDao.getAllOnce()
        val preserved = current.filterNot { it.departureDateTime.startsWith("2026-06") }
        val merged = (preserved + generated).sortedBy { it.departureDateTime }
        flightDao.clearAll()
        flightDao.insertAll(merged)
        RosterNotificationScheduler.scheduleRoster(context, merged)
        NotificationHelper.show(
            context,
            "Generated roster applied",
            "June 2026 generated roster test has been loaded without changing May roster.",
            2_006_000
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


    private fun canAssignAirportPosition(flight: FlightEntity): Boolean {
        if (flight.dutyType != "FLIGHT" || flight.isCompleted) return false
        val departure = parseLocalDateTime(flight.departureDateTime)
        val now = LocalDateTime.now()
        return !now.isBefore(departure.minusHours(3))
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
}
