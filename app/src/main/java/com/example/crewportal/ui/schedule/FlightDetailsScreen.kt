package com.example.crewportal.ui.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.airport.AirportDatabase
import com.example.crewportal.data.airport.AirportGeoDirectory
import com.example.crewportal.data.airport.AirportCoordinate
import com.example.crewportal.data.airport.AirportInfo
import com.example.crewportal.data.crew.CrewPool
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.fleet.AircraftPool
import com.example.crewportal.data.mel.MelDatabase
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.LogbookRepository
import com.example.crewportal.data.airport.CrewHotelDirectory
import com.example.crewportal.ui.theme.SuccessGreen
import com.example.crewportal.util.alternateFor
import com.example.crewportal.util.briefingDistanceNm
import com.example.crewportal.util.canRegister
import com.example.crewportal.util.cruiseLevel
import com.example.crewportal.util.displayDate
import com.example.crewportal.util.displayTime
import com.example.crewportal.util.dutyEndDateTime
import com.example.crewportal.util.dutyMinutes
import com.example.crewportal.util.estimatedFuel
import com.example.crewportal.util.etopsText
import com.example.crewportal.util.formatMinutes
import com.example.crewportal.util.hasArrived
import com.example.crewportal.util.notamSummary
import com.example.crewportal.util.nowAtAirport
import com.example.crewportal.util.reportDateTime
import com.example.crewportal.util.shouldShowRegistrationButton
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailsScreen(
    flightId: String,
    flightRepository: FlightRepository,
    logbookRepository: LogbookRepository,
    onBack: () -> Unit,
    onMelClick: (String) -> Unit
) {
    val flight by flightRepository.observeFlight(flightId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { flightRepository.refreshCompletedFlights(showNotifications = false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Flight Details") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } })
        }
    ) { padding ->
        val item = flight
        if (item == null) {
            Text("Flight not found", modifier = Modifier.padding(padding).padding(24.dp))
        } else {
            val longHaul = item.durationMinutes >= 360
            val augmentedCrew = item.durationMinutes > 10 * 60
            val crew = CrewPool.forFlight(item.id, augmentedCrew)
            val fuel = estimatedFuel(item.durationMinutes, item.aircraftLabel)
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(item.flightNumber, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("${item.departureCity} → ${item.arrivalCity}", color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (item.dutyType != "FLIGHT") {
                    InfoCard(when (item.dutyType) { "OFF" -> "Day Off"; "STAY" -> "Stay in ${AirportDatabase.cityName(item.departureIata, item.departureCity)}"; else -> "${item.dutyType} Details" }) {
                        DetailRow("Date", displayDate(item.departureDateTime), displayDaySafe(item.departureDateTime))
                        DetailRow("Time", "${displayTime(item.departureDateTime)}-${displayTime(item.arrivalDateTime)}", "Local time")
                        DetailRow("Location", when (item.dutyType) { "RESERVE", "STAY" -> item.departureAirport; else -> "Not applicable" }, item.departureCity)
                        DetailRow("Note", item.dutyNote.ifBlank { if (item.dutyType == "OFF") "No assigned duty" else "Company accommodation / reserve" }, "Company roster item")
                    }
                    return@Column
                }

                RouteMapCard(item)
                StatusTimelineCard(item)

                InfoCard("Flight Information") {
                    DetailRow("Departure", "${item.departureIata} / ${AirportDatabase.shortAirportName(item.departureIata, item.departureAirport)}", "${displayDate(item.departureDateTime)}, ${displayTime(item.departureDateTime)} local")
                    DetailRow("Arrival", "${item.arrivalIata} / ${AirportDatabase.shortAirportName(item.arrivalIata, item.arrivalAirport)}", "${displayDate(item.arrivalDateTime)}, ${displayTime(item.arrivalDateTime)} local")
                    DetailRow("Aircraft", item.aircraftFullName, item.aircraftLabel)
                    DetailRow("Registration", if (item.registration == "TBA") "Assigned 24h prior" else item.registration, "Released with crew registration window")
                    DetailRow("Block Time", formatMinutes(item.durationMinutes), "Scheduled block time")
                    DetailRow("Status", if (item.isCompleted) "Completed" else if (item.isRegistered) "Registered" else "Scheduled", "Company portal synchronized")
                }

                ElectronicLogbookCard(item, logbookRepository)

                AircraftTechnicalStatusCard(item, onMelClick)

                if (shouldShowAirportAssignment(item)) {
                    InfoCard("Airport Assignment") {
                        DetailRow("Gate", if (item.gate == "Pending") "Pending" else item.gate, "Assigned about 3 hours before departure")
                        DetailRow("Stand", if (item.stand == "Pending") "Pending" else item.stand, "Used when no gate is assigned")
                        DetailRow("Terminal", if (item.terminal == "Pending") "Pending" else item.terminal, "Airport operations synchronized")
                        DetailRow("Updated", if (item.gate != "Pending" || item.stand != "Pending") "Available" else "Pending", "Company portal / airport data sync")
                    }
                }

                InfoCard("Airport Database") {
                    AirportDatabase.byIata(item.departureIata)?.let { airport: AirportInfo -> DetailRow("Departure Airport", "${airport.iata} / ${airport.icao}", "${airport.city}, ${airport.country} • ${AirportDatabase.localOffsetText(airport.iata)}") }
                    AirportDatabase.byIata(item.arrivalIata)?.let { airport: AirportInfo -> DetailRow("Arrival Airport", "${airport.iata} / ${airport.icao}", "${airport.city}, ${airport.country} • ${AirportDatabase.localOffsetText(airport.iata)}") }
                }

                InfoCard("Duty Day") {
                    DetailRow("Report Time", reportDateTime(item.departureDateTime, item.durationMinutes).format(DateTimeFormatter.ofPattern("HH:mm")), "${if (longHaul) "International" else "Domestic"} report policy")
                    DetailRow("Duty End", dutyEndDateTime(item.arrivalDateTime).format(DateTimeFormatter.ofPattern("HH:mm")), "30 minutes after arrival")
                    DetailRow("Flight Duty Period", formatMinutes(dutyMinutes(item.departureDateTime, item.arrivalDateTime, item.durationMinutes)), "Calculated from report to duty end")
                    DetailRow("Rest Status", "OK", "Minimum rest requirement satisfied")
                }

                InfoCard("Flight Briefing Package") {
                    DetailRow("Route", "${item.departureIata} - ${item.arrivalIata}", "Distance approx. ${briefingDistanceNm(item.durationMinutes)} NM")
                    DetailRow("Aircraft", item.aircraftLabel, if (item.registration == "TBA") "Registration pending" else item.registration)
                    DetailRow("OFP Status", "Available", "Company briefing package synchronized")
                    DetailRow("Weather Status", "Updated", "METAR / TAF available from Weather tab")
                    DetailRow("NOTAM Status", "Company briefing required", notamSummary(item.arrivalIata))
                    DetailRow("Alternate", alternateFor(item.arrivalIata), "Dispatch alternate placeholder")
                    DetailRow("Cruise Level", cruiseLevel(item.durationMinutes), "Estimated planning level")
                    DetailRow("ETOPS", etopsText(item.durationMinutes), if (longHaul) "Extended-range briefing required" else "Standard operation")
                }

                InfoCard("Estimated Fuel Briefing") {
                    DetailRow("Taxi Fuel", "${fuel.taxiKg} kg", "Estimated")
                    DetailRow("Trip Fuel", "${fuel.tripKg} kg", "Based on block time and type")
                    DetailRow("Contingency", "${fuel.contingencyKg} kg", "5% placeholder")
                    DetailRow("Alternate", "${fuel.alternateKg} kg", "Planning estimate")
                    DetailRow("Final Reserve", "${fuel.finalReserveKg} kg", "Planning estimate")
                    DetailRow("Extra", "${fuel.extraKg} kg", "Company discretionary placeholder")
                    DetailRow("Block Fuel", "${fuel.totalKg} kg", "Estimated briefing data only")
                }

                InfoCard("NOTAM Summary") {
                    DetailRow("Company Summary", notamSummary(item.arrivalIata), "Official NOTAM briefing required before departure")
                }


                CrewRestPlanCard(item, augmentedCrew)
                DutyLimitMonitorCard(item)

                InfoCard("Crew List") {
                    DetailRow("Captain", crew.captain, "Operating commander")
                    DetailRow("First Officer", crew.firstOfficer, "Operating pilot")
                    if (crew.reliefCaptain != null) DetailRow("Relief Captain", crew.reliefCaptain, "Augmented crew")
                    if (crew.reliefFirstOfficer != null) DetailRow("Relief First Officer", crew.reliefFirstOfficer, "Augmented crew")
                    if (item.dutyNote.contains("Line pilot instructor", ignoreCase = true)) {
                        DetailRow("Line Pilot Instructor", "Dmitrii Alekseev", "Observer / checking pilot, not operating commander")
                    } else if (item.dutyNote.contains("Line Check", ignoreCase = true)) {
                        DetailRow("Line Instructor", CrewPool.lineInstructorForFlight(item.id), "Line check supervision")
                    }
                    DetailRow("Cabin Manager", crew.cabinManager, "Thai cabin crew database")
                    DetailRow("Cabin Crew", "${crew.cabinCrewCount} crew members", "Assigned by cabin crew roster module")
                }

                InfoCard("Layover / Turnaround") {
                    if (longHaul) {
                        DetailRow("Hotel", layoverHotelFor(item.arrivalIata), "Transport by company shuttle")
                        DetailRow("Pickup", "TBA by local station", "Shown after station update")
                    } else if (item.dutyType == "RESERVE") {
                        DetailRow("Hotel", "Hyatt Regency Bangkok Suvarnabhumi Airport", "Airport reserve accommodation")
                    } else {
                        DetailRow("Layover", "Not applicable", "Turnaround operation")
                    }
                }

                if (item.isRegistered) {
                    Button(onClick = {}, enabled = true, colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), modifier = Modifier.fillMaxWidth()) { Text("Registered") }
                    Text("Registration completed successfully", color = SuccessGreen)
                } else if (shouldShowRegistrationButton(item.departureIata, item.durationMinutes) && canRegister(item.departureDateTime, item.isCompleted, item.departureIata)) {
                    OutlinedButton(onClick = { scope.launch { flightRepository.registerFlight(item.id) } }, modifier = Modifier.fillMaxWidth()) { Text("Register") }
                } else if (!item.isCompleted) {
                    Text("Registration and aircraft tail assignment open 24 hours before departure", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AircraftTechnicalStatusCard(flight: FlightEntity, onMelClick: (String) -> Unit) {
    val aircraft = if (flight.registration == "TBA") null else AircraftPool.byRegistration(flight.registration)
    val melCount = if (flight.registration == "TBA") 0 else MelDatabase.forAircraft(flight.registration).size
    InfoCard("Aircraft Technical Status") {
        DetailRow("Registration", if (flight.registration == "TBA") "Assigned 24h prior" else flight.registration, aircraft?.fullName ?: flight.aircraftFullName)
        DetailRow("Status", if (melCount > 0) "Serviceable with MEL" else "Serviceable", "Open MEL items: $melCount")
        DetailRow("Last maintenance", "BKK Line Maintenance", "Latest station technical status synchronized")
        DetailRow("Next planned check", if (flight.durationMinutes >= 360) "After long-haul rotation" else "Next BKK night stop", "Maintenance planning data")
        if (flight.registration != "TBA") {
            Button(
                onClick = { onMelClick(flight.registration) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Open MEL")
            }
        }
    }
}

@Composable
private fun CrewRestPlanCard(flight: FlightEntity, longHaul: Boolean) {
    InfoCard("Crew Rest Plan") {
        if (!longHaul) {
            DetailRow("Crew rest", "Not required", "Standard short/medium sector")
        } else {
            DetailRow("Crew", "Augmented", "Captain, First Officer and relief crew")
            DetailRow("Rest Group A", "Captain + First Officer", "Planned rest: cruise middle segment")
            DetailRow("Rest Group B", "Relief crew", "Planned rest: early/late cruise segment")
            DetailRow("Cabin crew rest", "Assigned by cabin manager", "Long-haul crew rest plan active")
        }
    }
}

@Composable
private fun DutyLimitMonitorCard(flight: FlightEntity) {
    val fdp = dutyMinutes(flight.departureDateTime, flight.arrivalDateTime, flight.durationMinutes)
    val limit = if (flight.durationMinutes >= 360) 13 * 60 else 11 * 60
    InfoCard("Duty Limits / Fatigue Monitor") {
        DetailRow("FDP", formatMinutes(fdp), "Limit ${formatMinutes(limit)}")
        DetailRow("Status", if (fdp <= limit) "OK" else "Review required", if (fdp <= limit) "Within planned duty limit" else "Close to limit")
        DetailRow("Monthly monitor", "Active", "Flight time counters updated after completed sectors")
        DetailRow("Rest", "OK", "Next duty rest check passed")
    }
}

@Composable
private fun RouteMapCard(flight: FlightEntity) {
    val departurePoint = AirportGeoDirectory.byIata(flight.departureIata)
    val arrivalPoint = AirportGeoDirectory.byIata(flight.arrivalIata)

    InfoCard("Route Map") {
        if (departurePoint == null || arrivalPoint == null) {
            Text(
                "Offline route diagram ${flight.departureIata}-${flight.arrivalIata} • coordinates pending",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@InfoCard
        }
        OfflineRouteMap(departurePoint, arrivalPoint)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = flight.departureIata,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${AirportGeoDirectory.distanceNm(flight.departureIata, flight.arrivalIata) ?: briefingDistanceNm(flight.durationMinutes)} NM • offline",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = flight.arrivalIata,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun OfflineRouteMap(departure: AirportCoordinate, arrival: AirportCoordinate) {
    val routeColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val landColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    Canvas(Modifier.fillMaxWidth().height(280.dp)) {
        drawRect(backgroundColor)
        for (longitude in -120..120 step 60) {
            val x = ((longitude + 180.0) / 360.0 * size.width).toFloat()
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
        }
        for (latitude in -60..60 step 30) {
            val y = ((90.0 - latitude) / 180.0 * size.height).toFloat()
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
        }

        offlineLandShapes.forEach { outline ->
            val path = Path()
            outline.forEachIndexed { index, point ->
                val projected = project(point, size.width, size.height)
                if (index == 0) path.moveTo(projected.x, projected.y) else path.lineTo(projected.x, projected.y)
            }
            path.close()
            drawPath(path, landColor)
        }

        val start = project(departure, size.width, size.height)
        val end = project(arrival, size.width, size.height)
        val arc = Path().apply {
            moveTo(start.x, start.y)
            val control = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f - size.height * 0.15f)
            quadraticBezierTo(control.x, control.y, end.x, end.y)
        }
        drawPath(arc, routeColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
        drawCircle(Color.White, radius = 10f, center = start)
        drawCircle(routeColor, radius = 7f, center = start)
        drawCircle(Color.White, radius = 10f, center = end)
        drawCircle(routeColor, radius = 7f, center = end)
    }
}

private fun displayDaySafe(dateTime: String): String = com.example.crewportal.util.displayDay(dateTime)

private fun project(point: AirportCoordinate, width: Float, height: Float): Offset = Offset(
    x = (((point.longitude + 180.0) / 360.0) * width).toFloat(),
    y = (((90.0 - point.latitude) / 180.0) * height).toFloat()
)

private val offlineLandShapes = listOf(
    listOf(AirportCoordinate(72.0, -168.0), AirportCoordinate(70.0, -52.0), AirportCoordinate(15.0, -82.0), AirportCoordinate(8.0, -104.0), AirportCoordinate(30.0, -118.0)),
    listOf(AirportCoordinate(12.0, -81.0), AirportCoordinate(-56.0, -68.0), AirportCoordinate(-20.0, -36.0), AirportCoordinate(8.0, -60.0)),
    listOf(AirportCoordinate(72.0, -10.0), AirportCoordinate(72.0, 170.0), AirportCoordinate(8.0, 145.0), AirportCoordinate(0.0, 42.0), AirportCoordinate(37.0, -10.0)),
    listOf(AirportCoordinate(35.0, -17.0), AirportCoordinate(-35.0, 18.0), AirportCoordinate(-35.0, 52.0), AirportCoordinate(12.0, 51.0)),
    listOf(AirportCoordinate(-10.0, 112.0), AirportCoordinate(-44.0, 113.0), AirportCoordinate(-39.0, 154.0), AirportCoordinate(-12.0, 153.0))
)

private fun shouldShowAirportAssignment(flight: FlightEntity): Boolean {
    return flight.departureIata == "BKK" || flight.durationMinutes >= 360
}

private fun layoverHotelFor(iata: String): String {
    return CrewHotelDirectory.hotelFor(iata)
}

@Composable
private fun StatusTimelineCard(flight: FlightEntity) {
    val registrationOpen = flight.isRegistered || (shouldShowRegistrationButton(flight.departureIata, flight.durationMinutes) && canRegister(flight.departureDateTime, flight.isCompleted, flight.departureIata))
    val gateStandAvailable = flight.gate != "Pending" || flight.stand != "Pending"
    val reportPassed = !nowAtAirport(flight.departureIata).isBefore(reportDateTime(flight.departureDateTime, flight.durationMinutes))
    val stages = listOf(
        "Registration Open" to registrationOpen,
        "Aircraft Assigned" to (flight.registration != "TBA"),
        "Registered" to flight.isRegistered,
        "Gate / Stand Assigned" to gateStandAvailable,
        "Report Time" to reportPassed,
        "Arrived" to hasArrived(flight.arrivalDateTime, flight.arrivalIata),
        "Flight Time Added" to flight.isFlightTimeAdded
    )
    InfoCard("Flight Status Timeline") {
        stages.forEach { (label, active) ->
            Row(Modifier.fillMaxWidth()) {
                Text(if (active) "●" else "○", color = if (active) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 10.dp))
                Text(label, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = if (active) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChecklistCard(longHaul: Boolean) {
    val base = listOf("Check roster", "Review aircraft assignment", "Check METAR / TAF", "Check NOTAM", "Review briefing package", "Confirm crew report time", "Complete registration")
    val extra = if (longHaul) listOf("Review ETOPS briefing", "Check alternates", "Review augmented crew rest plan") else emptyList()
    val items = base + extra
    val checked = remember { mutableStateListOf<Boolean>().apply { repeat(items.size) { add(false) } } }
    InfoCard("Pre-flight Checklist") {
        items.forEachIndexed { index, label ->
            Row(Modifier.fillMaxWidth()) {
                Checkbox(checked = checked[index], onCheckedChange = { checked[index] = it })
                Text(label, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, main: String, sub: String) {
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(main, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (sub.isNotBlank()) Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
