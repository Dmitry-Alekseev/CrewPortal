package com.example.crewportal.ui.schedule

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.airport.AirportDatabase
import com.example.crewportal.data.crew.CrewPool
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.ui.theme.SuccessGreen
import com.example.crewportal.ui.theme.TextMuted
import com.example.crewportal.ui.theme.ThaiPurple
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
import com.example.crewportal.util.reportDateTime
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailsScreen(flightId: String, flightRepository: FlightRepository, onBack: () -> Unit) {
    val flight by flightRepository.observeFlight(flightId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { flightRepository.refreshCompletedFlights() }

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
            val crew = CrewPool.forFlight(item.id, longHaul)
            val fuel = estimatedFuel(item.durationMinutes, item.aircraftLabel)
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(item.flightNumber, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("${item.departureCity} → ${item.arrivalCity}", color = TextMuted)

                RouteMapCard(item)
                StatusTimelineCard(item)

                InfoCard("Flight Information") {
                    DetailRow("Departure", "${item.departureIata} / ${item.departureAirport}", "${displayDate(item.departureDateTime)}, ${displayTime(item.departureDateTime)} • ${AirportDatabase.utcText(item.departureDateTime, item.departureIata)}")
                    DetailRow("Arrival", "${item.arrivalIata} / ${item.arrivalAirport}", "${displayDate(item.arrivalDateTime)}, ${displayTime(item.arrivalDateTime)} • ${AirportDatabase.utcText(item.arrivalDateTime, item.arrivalIata)}")
                    DetailRow("Aircraft", item.aircraftFullName, item.aircraftLabel)
                    DetailRow("Registration", if (item.registration == "TBA") "Assigned 24h prior" else item.registration, "Released with crew registration window")
                    DetailRow("Block Time", formatMinutes(item.durationMinutes), "Local/UTC toggle data available")
                    DetailRow("Status", if (item.isCompleted) "Completed" else if (item.isRegistered) "Registered" else "Scheduled", "Company portal synchronized")
                }

                InfoCard("Airport Assignment") {
                    DetailRow("Gate", if (item.gate == "Pending") "Pending" else item.gate, "Assigned about 3 hours before departure")
                    DetailRow("Stand", if (item.stand == "Pending") "Pending" else item.stand, "Used when no gate is assigned")
                    DetailRow("Terminal", if (item.terminal == "Pending") "Pending" else item.terminal, "Airport operations synchronized")
                    DetailRow("Updated", if (item.gate != "Pending" || item.stand != "Pending") "Available" else "Pending", "Company portal / airport data sync")
                }

                InfoCard("Airport Database") {
                    AirportDatabase.byIata(item.departureIata)?.let { airport -> DetailRow("Departure Airport", "${airport.iata} / ${airport.icao}", "${airport.city}, ${airport.country} • ${AirportDatabase.localOffsetText(airport.iata)}") }
                    AirportDatabase.byIata(item.arrivalIata)?.let { airport -> DetailRow("Arrival Airport", "${airport.iata} / ${airport.icao}", "${airport.city}, ${airport.country} • ${AirportDatabase.localOffsetText(airport.iata)}") }
                }

                InfoCard("Duty Day") {
                    DetailRow("Report Time", reportDateTime(item.departureDateTime, item.durationMinutes).format(DateTimeFormatter.ofPattern("HH:mm")), "${if (longHaul) "International" else "Domestic"} report policy")
                    DetailRow("Duty End", dutyEndDateTime(item.arrivalDateTime).format(DateTimeFormatter.ofPattern("HH:mm")), "30 minutes after arrival")
                    DetailRow("Flight Duty Period", formatMinutes(dutyMinutes(item.departureDateTime, item.arrivalDateTime, item.durationMinutes)), "Calculated from report to duty end")
                    DetailRow("Rest Status", "OK", "Minimum rest requirement satisfied")
                }

                InfoCard("Flight Briefing") {
                    DetailRow("Route", "${item.departureIata} - ${item.arrivalIata}", "Distance approx. ${briefingDistanceNm(item.durationMinutes)} NM")
                    DetailRow("Alternate", alternateFor(item.arrivalIata), "Dispatch alternate placeholder")
                    DetailRow("Cruise Level", cruiseLevel(item.durationMinutes), "Estimated planning level")
                    DetailRow("ETOPS", etopsText(item.durationMinutes), if (longHaul) "Extended-range briefing required" else "Standard operation")
                    DetailRow("Remarks", if (longHaul) "Augmented crew operation" else "Standard crew operation", "Operational briefing data")
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

                ChecklistCard(longHaul = longHaul)

                InfoCard("Crew List") {
                    DetailRow("Captain", crew.captain, "Operating commander")
                    DetailRow("First Officer", crew.firstOfficer, "Operating pilot")
                    if (crew.reliefCaptain != null) DetailRow("Relief Captain", crew.reliefCaptain, "Augmented crew")
                    if (crew.reliefFirstOfficer != null) DetailRow("Relief First Officer", crew.reliefFirstOfficer, "Augmented crew")
                    DetailRow("Cabin Manager", crew.cabinManager, "Thai cabin crew database")
                    DetailRow("Cabin Crew", "${crew.cabinCrewCount} crew members", "Assigned by cabin crew roster module")
                }

                InfoCard("Layover / Turnaround") {
                    if (longHaul) {
                        DetailRow("Hotel", "Company Crew Hotel", "Transport by company shuttle")
                        DetailRow("Pickup", "TBA by local station", "Shown after station update")
                    } else {
                        DetailRow("Layover", "Not applicable", "Turnaround operation")
                    }
                }

                if (item.isRegistered) {
                    Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Registered") }
                    Text("Registration completed successfully", color = SuccessGreen)
                } else if (canRegister(item.departureDateTime, item.isCompleted)) {
                    OutlinedButton(onClick = { scope.launch { flightRepository.registerFlight(item.id) } }, modifier = Modifier.fillMaxWidth()) { Text("Register") }
                } else if (!item.isCompleted) {
                    Text("Registration and aircraft tail assignment open 24 hours before departure", color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun RouteMapCard(flight: FlightEntity) {
    InfoCard("Route Map") {
        Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
            val y = size.height / 2
            val start = Offset(40f, y)
            val end = Offset(size.width - 40f, y)
            drawLine(color = ThaiPurple, start = start, end = end, strokeWidth = 6f)
            drawCircle(color = ThaiPurple, radius = 12f, center = start)
            drawCircle(color = ThaiPurple, radius = 12f, center = end)
            drawCircle(color = Color.White, radius = 6f, center = start)
            drawCircle(color = Color.White, radius = 6f, center = end)
        }
        Row(Modifier.fillMaxWidth()) {
            Text(flight.departureIata, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${briefingDistanceNm(flight.durationMinutes)} NM", color = TextMuted)
            Text(flight.arrivalIata, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusTimelineCard(flight: FlightEntity) {
    val stages = listOf(
        "Roster Published" to true,
        "Aircraft Assigned" to (flight.registration != "TBA"),
        "Registration Open" to canRegister(flight.departureDateTime, flight.isCompleted),
        "Gate / Stand Assigned" to (flight.gate != "Pending" || flight.stand != "Pending"),
        "Registered" to flight.isRegistered,
        "Report Time" to false,
        "Arrived" to hasArrived(flight.arrivalDateTime),
        "Logbook Updated" to flight.isFlightTimeAdded
    )
    InfoCard("Flight Status Timeline") {
        stages.forEach { (label, active) ->
            Row(Modifier.fillMaxWidth()) {
                Text(if (active) "●" else "○", color = if (active) SuccessGreen else TextMuted, modifier = Modifier.padding(end = 10.dp))
                Text(label, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = if (active) SuccessGreen else TextMuted)
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
            Text(label.uppercase(), color = TextMuted, style = MaterialTheme.typography.labelMedium)
            Text(main, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (sub.isNotBlank()) Text(sub, color = TextMuted)
        }
    }
}
