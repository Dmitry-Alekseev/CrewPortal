package com.example.crewportal.data.repository

import android.content.Context
import android.util.Log
import com.example.crewportal.data.airport.AirportAssignmentPool
import com.example.crewportal.data.delivery.AircraftDeliveryPlanner
import com.example.crewportal.data.delivery.AircraftDeliveryRequest
import com.example.crewportal.data.crew.InstructorRole
import com.example.crewportal.data.fleet.AircraftTypeCatalog
import com.example.crewportal.data.local.FlightDao
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.leave.LeaveDatabase
import com.example.crewportal.data.qualification.A380QualificationPolicy
import com.example.crewportal.data.roster.RosterGenerator
import com.example.crewportal.data.roster.RosterChangeEngine
import com.example.crewportal.data.route.RouteCatalog
import com.example.crewportal.util.NotificationHelper
import com.example.crewportal.util.RosterNotificationScheduler
import com.example.crewportal.util.arrivalLocalDateTime
import com.example.crewportal.util.airportLocalEpochMillis
import com.example.crewportal.util.canRegister
import com.example.crewportal.util.hasArrived
import com.example.crewportal.util.nowAtAirport
import com.example.crewportal.util.parseLocalDateTime
import com.example.crewportal.util.shouldShowRegistrationButton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import org.json.JSONObject

class FlightRepository(
    private val context: Context,
    private val flightDao: FlightDao,
    private val preferencesRepository: PreferencesRepository,
    private val fleetRepository: FleetRepository
) {
    fun observeFlights(): Flow<List<FlightEntity>> = flightDao.observeAll()
    fun observeCompleted(): Flow<List<FlightEntity>> = flightDao.observeCompleted()
    fun observeFlight(id: String): Flow<FlightEntity?> = flightDao.observeById(id)

    suspend fun loadScheduleFromAssetsIfNeeded() {
        if (flightDao.count() > 0) {
            Log.d(TAG, "Current roster generation skipped: persisted roster already exists")
            return
        }
        val currentMonthRoster = RosterGenerator.generateForMonth(currentBangkokMonth())
        flightDao.insertAll(normalizeInstants(currentMonthRoster))
        Log.d(TAG, "Current month roster generated: ${currentBangkokMonth()}")
        RosterNotificationScheduler.scheduleRoster(context, currentMonthRoster)
    }

    suspend fun refreshBuiltInRosterOnAppUpdate(versionName: String) {
        // Seed only a genuinely empty installation. Any existing row means the published local
        // roster is authoritative and must not be regenerated for a newer generator/TAS ruleset.
        loadScheduleFromAssetsIfNeeded()
        backfillUtcInstants()
        // Presentation-only cleanup for already published 3.0 rows; no date, time, pairing or
        // completion state is regenerated or replaced.
        flightDao.normalizeLegacyTashkentLabels()
        val installedVersion = preferencesRepository.installedAppVersion.first()
        if (installedVersion != versionName) {
            // App updates must never overwrite or hide the active local/generated roster.
            // Keep the review/accept state and the permanent one-time test marker. The hidden
            // generator must not become available again merely because the app was updated.
            preferencesRepository.setInstalledAppVersion(versionName)
        }

        restoreGeneratedRosterStateIfNeeded()
        // Daily WorkManager scheduling calls prepareNextMonthRosterIfDue(). The date gate and
        // existence check below make that background operation idempotent.
    }

    private suspend fun restoreGeneratedRosterStateIfNeeded() {
        val nextMonth = currentBangkokMonth().plusMonths(1)
        val prefix = "%04d-%02d".format(nextMonth.year, nextMonth.monthValue)
        val hasGeneratedNextMonth = flightDao.getAllOnce().any { it.departureDateTime.startsWith(prefix) }

        if (hasGeneratedNextMonth && !preferencesRepository.nextMonthRosterPrepared.first()) {
            preferencesRepository.setNextMonthRosterPrepared(true)
        }
    }

    suspend fun prepareNextMonthRosterIfDue() {
        val today = nowAtAirport("BKK").toLocalDate()
        val currentMonth = YearMonth.from(today)
        val triggerDate = currentMonth.atDay(minOf(27, currentMonth.lengthOfMonth()))
        if (today.isBefore(triggerDate)) return

        val nextMonth = currentMonth.plusMonths(1)
        val prefix = "%04d-%02d".format(nextMonth.year, nextMonth.monthValue)
        val snapshot = flightDao.getAllOnce()
        val alreadyGenerated = snapshot.any { it.departureDateTime.startsWith(prefix) }
        if (!alreadyGenerated) {
            val generated = RosterGenerator.generateForMonth(nextMonth)
            flightDao.insertAll(normalizeInstants(generated))
            Log.d(TAG, "Next month roster generated: $nextMonth")
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
            Log.d(TAG, "Next month generation skipped: roster already exists for $nextMonth")
            preferencesRepository.setNextMonthRosterPrepared(true)
        }
    }

    suspend fun reloadScheduleFromAssets() {
        loadScheduleFromAssets(clearExisting = true, preserveExistingState = true)
    }

    private suspend fun loadScheduleFromAssets(clearExisting: Boolean, preserveExistingState: Boolean = false) {
        val generated = RosterGenerator.generateForMonth(currentBangkokMonth())
        if (clearExisting) flightDao.replaceAll(normalizeInstants(generated))
        else flightDao.insertAll(normalizeInstants(generated))
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
        if (clearExisting) flightDao.replaceAll(normalizeInstants(flights))
        else flightDao.insertAll(normalizeInstants(flights))
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

    /**
     * Advances persisted roster state in one pass: optional roster change, aircraft/airport
     * assignment, completion, then one-time flight-time credit. Every transition is guarded by
     * flags on [FlightEntity], so repeated refreshes remain idempotent.
     */
    suspend fun refreshCompletedFlights(showNotifications: Boolean = false) {
        val initialSnapshot = flightDao.getAllOnce()
        val automaticChange = RosterChangeEngine.applyChangeIfDue(initialSnapshot, nowAtAirport("BKK").toLocalDate())
        val rosterSnapshot = if (automaticChange != null) {
            flightDao.replaceAll(normalizeInstants(automaticChange.updatedRoster))
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
            if (flight.dutyType == "FLIGHT" && !flight.isFlightTimeAdded && hasArrived(flight.arrivalDateTime, flight.arrivalIata)) {
                flightDao.markCompletedAndAdded(flight.id)
                if (flight.flightTimeCreditEligible) {
                    preferencesRepository.addFlightTime(
                        flight.durationMinutes,
                        flight.aircraftLabel,
                        addPicTime = !A380QualificationPolicy.userIsFirstOfficer(flight)
                    )
                }
                if (showNotifications) {
                    NotificationHelper.show(
                        context,
                        "Flight completed",
                        if (flight.flightTimeCreditEligible) "${flight.flightNumber} completed. ${flight.durationMinutes / 60}h ${flight.durationMinutes % 60}m added to your flight time."
                        else "${flight.flightNumber} completed as instructor/checking duty; operating block time was not credited.",
                        flight.id.hashCode() + 10_000
                    )
                }
            }
            if (flight.isAircraftDelivery && flight.arrivalIata == "BKK" && !flight.deliveryProcessed && hasArrived(flight.arrivalDateTime, flight.arrivalIata)) {
                fleetRepository.addDeliveredAircraft(
                    registration = flight.registration,
                    aircraftLabel = flight.deliveryAircraftType.ifBlank { flight.aircraftLabel },
                    sourceFlightId = flight.id,
                    deliveredAtEpochMillis = flight.arrivalEpochMillis ?: System.currentTimeMillis()
                )
                flightDao.markDeliveryProcessed(flight.id)
                if (showNotifications) {
                    NotificationHelper.show(
                        context,
                        "Aircraft added to fleet",
                        "${flight.registration} • ${flight.deliveryAircraftType.ifBlank { flight.aircraftLabel }} is now active in the airline fleet database.",
                        flight.id.hashCode() + 40_000
                    )
                }
            }
        }
    }

    /**
     * One-time QA escape hatch for preparing next month before the normal day-27 gate.
     * The permanent flag is written only after a successful insert and is never reset by update,
     * draft deletion or process restart.
     */
    suspend fun generateNextMonthRosterOnce(): Boolean {
        if (preferencesRepository.secretRosterGeneratorUsed.first()) return false
        val targetMonth = currentBangkokMonth().plusMonths(1)
        val prefix = "%04d-%02d".format(targetMonth.year, targetMonth.monthValue)
        val current = flightDao.getAllOnce()
        if (current.any { it.departureDateTime.startsWith(prefix) }) return false
        val generated = RosterGenerator.generateForMonth(targetMonth)
        val preserved = current.filterNot { it.departureDateTime.startsWith(prefix) }
        val merged = (preserved + generated).sortedBy { it.departureDateTime }
        flightDao.replaceAll(normalizeInstants(merged))
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
        return true
    }


    suspend fun deleteNextMonthRosterDraft() {
        val targetMonth = currentBangkokMonth().plusMonths(1)
        val prefix = "%04d-%02d".format(targetMonth.year, targetMonth.monthValue)
        val current = flightDao.getAllOnce()
        val preserved = current.filterNot { it.departureDateTime.startsWith(prefix) }
        flightDao.replaceAll(normalizeInstants(preserved))
        RosterNotificationScheduler.scheduleRoster(context, preserved)
        preferencesRepository.resetNextMonthRosterDecision()
        preferencesRepository.setNextMonthRosterPrepared(false)
        NotificationHelper.show(
            context,
            "Next roster draft cleared",
            "${targetMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${targetMonth.year} draft roster was removed. Current active roster was not changed.",
            2_100_500 + targetMonth.monthValue
        )
    }

    /**
     * Makes persisted approved leave authoritative over published and generated roster rows.
     * Any future duty whose local calendar span intersects leave is removed; Leave remains in its
     * own Room table and is rendered by Roster/Calendar as the single source of truth. This also
     * makes monthly block and payroll recalculate from the rewritten roster immediately.
     */
    suspend fun reconcileRosterWithApprovedLeave(showNotification: Boolean = false): Int {
        val approved = LeaveDatabase.allApproved()
        if (approved.isEmpty()) return 0
        val today = nowAtAirport("BKK").toLocalDate()
        val snapshot = flightDao.getAllOnce()
        val removable = snapshot.filter { duty ->
            if (duty.isCompleted) return@filter false
            val dutyStart = parseLocalDateTime(duty.departureDateTime).toLocalDate()
            val dutyEnd = parseLocalDateTime(duty.arrivalDateTime).toLocalDate()
            if (dutyEnd.isBefore(today)) return@filter false
            approved.any { leave ->
                !dutyEnd.isBefore(leave.start) && !dutyStart.isAfter(leave.end)
            }
        }.map { it.id }
        if (removable.isEmpty()) return 0

        flightDao.deleteByIds(removable)
        val updated = flightDao.getAllOnce()
        RosterNotificationScheduler.scheduleRoster(context, updated)
        if (showNotification) {
            NotificationHelper.show(
                context,
                "Approved leave added to roster",
                "Crew Planning replaced ${removable.size} conflicting roster item(s). Monthly target and payroll have been recalculated.",
                2_310_005
            )
        }
        return removable.size
    }

    /**
     * Applies a complete Airbus delivery plan as one roster state transition. Ordinary generated
     * duties on affected dates are replaced automatically because the simplified 3.0 form has no
     * replacement switch. Qualification groups remain protected and make the operation fail
     * atomically instead of being partially removed.
     */
    suspend fun addAircraftDeliveryPlan(request: AircraftDeliveryRequest): Boolean {
        val plan = AircraftDeliveryPlanner.build(request)
        val affectedDates = plan.rows.flatMap { row ->
            listOf(
                parseLocalDateTime(row.departureDateTime).toLocalDate(),
                parseLocalDateTime(row.arrivalDateTime).toLocalDate()
            )
        }.toSet()
        val existing = flightDao.getAllOnce()
        val protectedQualification = existing.any { item ->
            parseLocalDateTime(item.departureDateTime).toLocalDate() in affectedDates &&
                item.eventGroupId.isNotBlank() &&
                (item.dutyType in setOf("SIMULATOR", "MEDICAL", "SAFETY", "TRAINING", "EXAM") || item.lineCheckRole.isNotBlank())
        }
        if (protectedQualification) return false

        val merged = (
            existing.filterNot { parseLocalDateTime(it.departureDateTime).toLocalDate() in affectedDates } +
                plan.rows
            ).sortedBy { it.departureDateTime }
        flightDao.replaceAll(normalizeInstants(merged))
        RosterNotificationScheduler.scheduleRoster(context, merged)
        NotificationHelper.show(
            context,
            "Aircraft Delivery planned",
            "XFW-${plan.intermediateIata}-BKK • ${plan.flightCrewSize} pilots • ${plan.stopMinutes / 60}h ${plan.stopMinutes % 60}m stop • ${request.registration}",
            ("delivery-${request.deliveryDate}-${request.registration}").hashCode()
        )
        return true
    }



    suspend fun setNextMonthRosterDecision(reviewed: Boolean, enhancedTarget: Boolean) {
        preferencesRepository.setNextMonthRosterDecision(reviewed = reviewed, enhancedTarget = enhancedTarget)
        if (enhancedTarget) {
            NotificationHelper.show(
                context,
                "90h target selected",
                "Additional duty will be published through Messages after final assignment.",
                2_200_900
            )
        }
    }

    suspend fun publishExtraDutyForSelectedTarget(): Boolean {
        val all = flightDao.getAllOnce()
        val now = nowAtAirport("BKK")
        val currentMonth = YearMonth.from(now.toLocalDate())
        val nextMonth = currentMonth.plusMonths(1)
        val nextPrefix = "%04d-%02d".format(nextMonth.year, nextMonth.monthValue)
        val targetMonth = if (preferencesRepository.nextMonthRosterPrepared.first() && all.any { it.departureDateTime.startsWith(nextPrefix) }) nextMonth else currentMonth
        val marker = "EXTRA-90H-${targetMonth.year}-${targetMonth.monthValue}"
        if (all.any { it.id.contains(marker) }) return true

        val earliestDay = if (targetMonth == currentMonth) now.toLocalDate().plusDays(2).dayOfMonth else 1
        var selectedDate: LocalDate? = null
        var selectedExtra: List<FlightEntity>? = null
        for (day in earliestDay..targetMonth.lengthOfMonth()) {
            val date = targetMonth.atDay(day)
            if (LeaveDatabase.leaveFor(date) != null) continue
            val duties = all.filter { parseLocalDateTime(it.departureDateTime).toLocalDate() == date }
            if (duties.isNotEmpty() && duties.any { it.dutyType !in setOf("OFF", "RESERVE") }) continue
            val proposed = normalizeInstants(buildManualDuty(
                date = date,
                reportTime = "10:00",
                outboundFlight = "TG${700 + day}",
                destinationIata = "SIN",
                aircraftLabel = "A321neo",
                registration = null,
                pattern = "TURNAROUND",
                returnFlight = "TG${701 + day}",
                returnDate = date,
                returnTime = null,
                note = "90h additional duty • Operational roster change • $marker",
                rosterSource = "COMPANY_EXTRA_DUTY"
            ))
            if (!hasTwelveHourRestAround(proposed, all)) continue
            selectedDate = date
            selectedExtra = proposed
            break
        }
        val date = selectedDate ?: return false
        val extra = selectedExtra ?: return false
        val candidate = date.dayOfMonth
        val removable = all.filter { parseLocalDateTime(it.departureDateTime).toLocalDate() == date && it.dutyType in setOf("OFF", "RESERVE") }.map { it.id }
        if (removable.isNotEmpty()) flightDao.deleteByIds(removable)
        flightDao.insertAll(extra)
        Log.d(TAG, "Extra 90h duty inserted: ${date}")
        val updated = flightDao.getAllOnce()
        RosterNotificationScheduler.scheduleRoster(context, updated)
        NotificationHelper.show(
            context,
            "Additional duty published",
            "A 90h target additional duty has been added to ${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}.",
            2_200_910 + candidate
        )
        return true
    }

    private fun hasTwelveHourRestAround(candidate: List<FlightEntity>, roster: List<FlightEntity>): Boolean {
        val candidateStart = candidate.minOfOrNull { dutyStartEpoch(it) } ?: return false
        val candidateEnd = candidate.maxOfOrNull { dutyEndEpoch(it) } ?: return false
        return roster.asSequence()
            .filter { it.dutyType !in setOf("OFF", "RESERVE", "STAY") }
            .all { existing ->
                val existingStart = dutyStartEpoch(existing)
                val existingEnd = dutyEndEpoch(existing)
                when {
                    existingEnd <= candidateStart -> candidateStart - existingEnd >= 12 * 60 * 60 * 1000L
                    candidateEnd <= existingStart -> existingStart - candidateEnd >= 12 * 60 * 60 * 1000L
                    else -> false
                }
            }
    }

    private fun dutyStartEpoch(duty: FlightEntity): Long {
        val base = duty.departureEpochMillis ?: airportLocalEpochMillis(duty.departureDateTime, duty.departureIata) ?: Long.MIN_VALUE
        return if (duty.dutyType == "FLIGHT") base - 90 * 60 * 1000L else base
    }

    private fun dutyEndEpoch(duty: FlightEntity): Long {
        val base = duty.arrivalEpochMillis ?: airportLocalEpochMillis(duty.arrivalDateTime, duty.arrivalIata) ?: Long.MAX_VALUE
        return if (duty.dutyType == "FLIGHT") base + 30 * 60 * 1000L else base
    }

    /**
     * Replaces eligible duties on the affected dates and persists a manual turnaround/layover.
     * [replaceExisting] authorizes removal of operating duties; otherwise only OFF/RESERVE/STAY
     * records are replaced. [instructorRole] distinguishes an operating captain instructor from
     * a third-seat instructor/observer without changing the persisted Room format.
     */
    suspend fun addOperationalRosterChange(
        date: LocalDate,
        reportTime: String,
        outboundFlight: String,
        destinationIata: String,
        aircraftLabel: String,
        registration: String?,
        pattern: String,
        returnFlight: String,
        returnDate: LocalDate?,
        returnTime: String?,
        replaceExisting: Boolean,
        instructorRole: String = InstructorRole.NONE,
        isAircraftDelivery: Boolean = false
    ): Boolean {
        val normalizedPattern = pattern.uppercase()
        if (normalizedPattern == "OFF") return addManualDayOff(date, replaceExisting)
        val affectedDates = mutableSetOf(date)
        if (normalizedPattern == "LAYOVER") {
            val ret = returnDate ?: date.plusDays(1)
            var d = date.plusDays(1)
            while (!d.isAfter(ret)) {
                affectedDates += d
                d = d.plusDays(1)
            }
        }
        val existing = flightDao.getAllOnce()
        val protectedQualification = existing.any { item ->
            val itemDate = parseLocalDateTime(item.departureDateTime).toLocalDate()
            itemDate in affectedDates && item.eventGroupId.isNotBlank() &&
                (item.dutyType in setOf("SIMULATOR", "MEDICAL", "SAFETY", "TRAINING", "EXAM") || item.lineCheckRole.isNotBlank())
        }
        if (protectedQualification) return false
        val removable = existing.filter { item ->
            val itemDate = parseLocalDateTime(item.departureDateTime).toLocalDate()
            itemDate in affectedDates && (replaceExisting || item.dutyType in setOf("OFF", "RESERVE", "STAY"))
        }.mapTo(mutableSetOf()) { it.id }
        if (isAircraftDelivery) {
            require(registration != null && FleetRepository.HS_REGISTRATION.matches(registration.uppercase())) {
                "Aircraft delivery registration must use HS- prefix"
            }
        }
        val created = buildManualDuty(
            date = date,
            reportTime = reportTime,
            outboundFlight = outboundFlight.ifBlank { "TG999" },
            destinationIata = destinationIata.uppercase(),
            aircraftLabel = aircraftLabel,
            registration = registration?.takeIf { it.isNotBlank() },
            pattern = normalizedPattern,
            returnFlight = returnFlight.ifBlank { "TG998" },
            returnDate = returnDate ?: date.plusDays(if (normalizedPattern == "LAYOVER") 1L else 0L),
            returnTime = returnTime?.takeIf { it.isNotBlank() },
            note = listOf("Manual operational roster change", InstructorRole.note(instructorRole))
                .filter { it.isNotBlank() }
                .joinToString(" • "),
            instructorRole = instructorRole,
            isAircraftDelivery = isAircraftDelivery
        )
        val merged = (existing.filterNot { it.id in removable } + created).sortedBy { it.departureDateTime }
        flightDao.replaceAll(normalizeInstants(merged))
        val updated = flightDao.getAllOnce()
        RosterNotificationScheduler.scheduleRoster(context, updated)
        NotificationHelper.show(
            context,
            "Operational roster change",
            if (isAircraftDelivery) "Aircraft delivery ${registration.orEmpty()} BKK-${destinationIata.uppercase()} was added. The aircraft joins Fleet after arrival."
            else "${outboundFlight.ifBlank { "TG999" }} BKK-${destinationIata.uppercase()} was added/changed and is now visible in Roster and Calendar.",
            ("manual-${date}-${outboundFlight}-${destinationIata}").hashCode()
        )
        return true
    }

    /** Adds a user-requested OFF row without forcing the flight-only fields in the UI. */
    private suspend fun addManualDayOff(date: LocalDate, replaceExisting: Boolean): Boolean {
        val existing = flightDao.getAllOnce()
        val onDate = existing.filter { parseLocalDateTime(it.departureDateTime).toLocalDate() == date }
        val protectedQualification = onDate.any { item ->
            item.eventGroupId.isNotBlank() &&
                (item.dutyType in setOf("SIMULATOR", "MEDICAL", "SAFETY", "TRAINING", "EXAM") || item.lineCheckRole.isNotBlank())
        }
        if (protectedQualification) return false
        if (!replaceExisting && onDate.any { it.dutyType !in setOf("OFF", "RESERVE", "STAY") }) return false

        val removableIds = onDate
            .filter { replaceExisting || it.dutyType in setOf("OFF", "RESERVE", "STAY") }
            .mapTo(mutableSetOf()) { it.id }
        val off = FlightEntity(
            id = "$date-OFF-MANUAL",
            airline = "THAI",
            flightNumber = "OFF",
            aircraftLabel = "OFF",
            aircraftFullName = "Day Off",
            registration = "—",
            status = "OFF",
            departureIata = "BKK",
            departureIcao = "VTBS",
            departureCity = "Bangkok",
            departureAirport = "Suvarnabhumi Intl",
            arrivalIata = "BKK",
            arrivalIcao = "VTBS",
            arrivalCity = "Bangkok",
            arrivalAirport = "Suvarnabhumi Intl",
            departureDateTime = date.atStartOfDay().toString(),
            arrivalDateTime = date.atTime(23, 59).toString(),
            durationMinutes = 0,
            dutyType = "OFF",
            dutyNote = "Manual operational roster change • Day off",
            rosterSource = "OPERATIONAL_CHANGE",
            flightTimeCreditEligible = false
        )
        val merged = (existing.filterNot { it.id in removableIds } + off).sortedBy { it.departureDateTime }
        flightDao.replaceAll(normalizeInstants(merged))
        RosterNotificationScheduler.scheduleRoster(context, merged)
        NotificationHelper.show(
            context,
            "Operational roster change",
            "Day off on $date was added to Roster and Calendar.",
            ("manual-off-$date").hashCode()
        )
        return true
    }

    private data class ManualRoute(
        val iata: String,
        val icao: String,
        val city: String,
        val airport: String,
        val outboundMinutes: Int,
        val inboundMinutes: Int,
        val hotel: String
    )

    private fun manualRoute(iata: String, seed: String): ManualRoute = RouteCatalog.byIata(iata).let { route ->
        ManualRoute(
            route.destinationIata,
            route.destinationIcao,
            route.destinationCity,
            route.destinationAirport,
            route.outboundMinutesFor("$seed-OUT"),
            route.inboundMinutesFor("$seed-IN"),
            route.hotel
        )
    }

    private fun aircraftFullName(label: String): String = AircraftTypeCatalog.byLabel(label).fullName

    private fun buildManualDuty(
        date: LocalDate,
        reportTime: String,
        outboundFlight: String,
        destinationIata: String,
        aircraftLabel: String,
        registration: String?,
        pattern: String,
        returnFlight: String,
        returnDate: LocalDate,
        returnTime: String?,
        note: String,
        instructorRole: String = InstructorRole.NONE,
        isAircraftDelivery: Boolean = false,
        rosterSource: String = "OPERATIONAL_CHANGE"
    ): List<FlightEntity> {
        val route = manualRoute(destinationIata, "$date-$outboundFlight-$returnFlight")
        val report = LocalDateTime.of(date, LocalTime.parse(if (reportTime.length == 5) "$reportTime:00" else reportTime))
        val outDeparture = report.plusMinutes(90)
        val outArrival = arrivalLocalDateTime(outDeparture, "BKK", route.iata, route.outboundMinutes)
        val reg = registration ?: "TBA"
        val full = aircraftFullName(aircraftLabel)
        val instructorObserver = InstructorRole.isObserver(instructorRole)
        val source = if (isAircraftDelivery) "DELIVERY" else rosterSource
        val outbound = FlightEntity(
            id = "${date}-${outboundFlight}-BKK-${route.iata}-MANUAL",
            airline = "THAI",
            flightNumber = outboundFlight,
            aircraftLabel = aircraftLabel,
            aircraftFullName = full,
            registration = reg,
            status = "SCHEDULED",
            departureIata = "BKK",
            departureIcao = "VTBS",
            departureCity = "Bangkok",
            departureAirport = "Suvarnabhumi Intl",
            arrivalIata = route.iata,
            arrivalIcao = route.icao,
            arrivalCity = route.city,
            arrivalAirport = route.airport,
            departureDateTime = outDeparture.toString(),
            arrivalDateTime = outArrival.toString(),
            durationMinutes = route.outboundMinutes,
            dutyType = "FLIGHT",
            dutyNote = if (isAircraftDelivery) "$note • Aircraft Delivery / Ferry" else note + if (pattern == "LAYOVER") " • Layover" else " • Turnaround",
            isAircraftDelivery = isAircraftDelivery,
            deliveryAircraftType = if (isAircraftDelivery) aircraftLabel else "",
            rosterSource = source,
            lineCheckRole = instructorRole,
            flightTimeCreditEligible = !instructorObserver
        )
        if (isAircraftDelivery) return listOf(outbound)
        val returnDeparture = if (pattern == "LAYOVER") {
            val time = returnTime?.let { if (it.length == 5) "$it:00" else it } ?: "12:00:00"
            LocalDateTime.of(returnDate, LocalTime.parse(time))
        } else {
            outArrival.plusMinutes(90)
        }
        val returnLeg = FlightEntity(
            id = "${returnDate}-${returnFlight}-${route.iata}-BKK-MANUAL",
            airline = "THAI",
            flightNumber = returnFlight,
            aircraftLabel = aircraftLabel,
            aircraftFullName = full,
            registration = reg,
            status = "SCHEDULED",
            departureIata = route.iata,
            departureIcao = route.icao,
            departureCity = route.city,
            departureAirport = route.airport,
            arrivalIata = "BKK",
            arrivalIcao = "VTBS",
            arrivalCity = "Bangkok",
            arrivalAirport = "Suvarnabhumi Intl",
            departureDateTime = returnDeparture.toString(),
            arrivalDateTime = arrivalLocalDateTime(returnDeparture, route.iata, "BKK", route.inboundMinutes).toString(),
            durationMinutes = route.inboundMinutes,
            dutyType = "FLIGHT",
            dutyNote = note + if (pattern == "LAYOVER") " • Return after layover" else " • Turnaround return",
            rosterSource = source,
            lineCheckRole = instructorRole,
            flightTimeCreditEligible = !instructorObserver
        )
        if (pattern != "LAYOVER") return listOf(outbound, returnLeg)
        val stayItems = buildList {
            var d = date.plusDays(1)
            while (!d.isAfter(returnDate)) {
                val stayEnd = if (d == returnDate) returnDeparture.minusMinutes(1).toString() else d.atTime(23, 59).toString()
                add(
                    FlightEntity(
                        id = "${d}-STAY-${route.iata}-MANUAL",
                        airline = "THAI",
                        flightNumber = "Stay in ${route.city}",
                        aircraftLabel = "STAY",
                        aircraftFullName = "Layover stay",
                        registration = "—",
                        status = "STAY",
                        departureIata = route.iata,
                        departureIcao = route.icao,
                        departureCity = route.city,
                        departureAirport = route.hotel,
                        arrivalIata = route.iata,
                        arrivalIcao = route.icao,
                        arrivalCity = route.city,
                        arrivalAirport = route.hotel,
                        departureDateTime = d.atStartOfDay().toString(),
                        arrivalDateTime = stayEnd,
                        durationMinutes = 0,
                        dutyType = "STAY",
                        dutyNote = route.hotel,
                        rosterSource = source,
                        flightTimeCreditEligible = false
                    )
                )
                d = d.plusDays(1)
            }
        }
        return listOf(outbound) + stayItems + listOf(returnLeg)
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
        val now = nowAtAirport(flight.departureIata)
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

    private companion object {
        const val TAG = "CrewRoster"
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
        val assigned = fleetRepository.assignFor(flight.aircraftLabel, routeClass, flight.id)
        if (assigned != null) flightDao.assignRegistration(flight.id, assigned.registration)
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

    private fun currentBangkokMonth(): YearMonth = YearMonth.from(nowAtAirport("BKK"))

    private fun normalizeInstants(flights: List<FlightEntity>): List<FlightEntity> = flights.map { flight ->
        flight.copy(
            departureEpochMillis = flight.departureEpochMillis
                ?: airportLocalEpochMillis(flight.departureDateTime, flight.departureIata),
            arrivalEpochMillis = flight.arrivalEpochMillis
                ?: airportLocalEpochMillis(flight.arrivalDateTime, flight.arrivalIata)
        )
    }

    private suspend fun backfillUtcInstants() {
        flightDao.getAllOnce()
            .filter { it.departureEpochMillis == null || it.arrivalEpochMillis == null }
            .forEach { flight ->
                val departure = airportLocalEpochMillis(flight.departureDateTime, flight.departureIata)
                val arrival = airportLocalEpochMillis(flight.arrivalDateTime, flight.arrivalIata)
                if (departure != null && arrival != null) {
                    flightDao.updateUtcInstants(flight.id, departure, arrival)
                }
            }
    }
}
