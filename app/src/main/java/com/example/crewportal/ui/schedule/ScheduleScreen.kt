package com.example.crewportal.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.airport.AirportDatabase
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.ui.theme.SuccessGreen
import com.example.crewportal.ui.theme.TextMuted
import com.example.crewportal.ui.theme.ThaiPurple
import com.example.crewportal.util.canRegister
import com.example.crewportal.util.displayDate
import com.example.crewportal.util.displayDay
import com.example.crewportal.util.displayTime
import com.example.crewportal.util.dutyEndDateTime
import com.example.crewportal.util.dutyMinutes
import com.example.crewportal.util.formatMinutes
import com.example.crewportal.util.parseLocalDateTime
import com.example.crewportal.util.reportDateTime
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleScreen(
    flightRepository: FlightRepository,
    onFlightClick: (String) -> Unit
) {
    val flights by flightRepository.observeFlights().collectAsState(initial = emptyList())
    var showUtc by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { flightRepository.refreshCompletedFlights() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Schedule", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (showUtc) "UTC times • Company roster synchronized" else "Local times • Company roster synchronized",
                    color = TextMuted,
                    modifier = Modifier.weight(1f).padding(end = 18.dp)
                )
                Text("UTC", color = TextMuted, modifier = Modifier.padding(end = 14.dp))
                Switch(checked = showUtc, onCheckedChange = { showUtc = it })
            }
            Spacer(Modifier.height(8.dp))
            MonthlyProgressCard(flights = flights)
            Spacer(Modifier.height(8.dp))
            TodayDutyCard(flights = flights, onFlightClick = onFlightClick)
        }
        items(flights, key = { it.id }) { flight ->
            if (flight.dutyType == "FLIGHT") {
                FlightCard(flight = flight, onClick = { onFlightClick(flight.id) }, flightRepository = flightRepository, showUtc = showUtc)
            } else {
                DutyCard(flight = flight)
            }
        }
    }
}

@Composable
private fun MonthlyProgressCard(flights: List<FlightEntity>) {
    val juneFlights = flights.filter { it.dutyType == "FLIGHT" && it.departureDateTime.startsWith("2026-06") }
    val planned = juneFlights.sumOf { it.durationMinutes }
    val completed = juneFlights.filter { it.isCompleted }.sumOf { it.durationMinutes }
    val target = 80 * 60
    val limit = 90 * 60
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Monthly Flight Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("June 2026 • Planned ${formatMinutes(planned)} • Completed ${formatMinutes(completed)}", color = TextMuted)
            LinearProgressIndicator(progress = (planned.toFloat() / target.toFloat()).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
            Text("Target ${formatMinutes(target)} • Limit ${formatMinutes(limit)}", color = TextMuted)
        }
    }
}

@Composable
private fun TodayDutyCard(flights: List<FlightEntity>, onFlightClick: (String) -> Unit) {
    val now = LocalDateTime.now()
    val next = flights.firstOrNull { parseLocalDateTime(it.arrivalDateTime).isAfter(now) }
    Card(colors = CardDefaults.cardColors(containerColor = ThaiPurple), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Today’s Duty", color = Color.White, fontWeight = FontWeight.Bold)
            if (next == null) {
                Text("No upcoming duty", color = Color.White)
            } else if (next.dutyType == "FLIGHT") {
                Text("${next.flightNumber} / ${next.departureIata}-${next.arrivalIata}", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Report ${reportDateTime(next.departureDateTime, next.durationMinutes).format(DateTimeFormatter.ofPattern("HH:mm"))} • Departure ${displayTime(next.departureDateTime)}", color = Color.White)
                Text("Aircraft: ${if (next.registration == "TBA") "Assigned 24h prior" else next.registration}", color = Color.White)
                Text(airportAssignmentLine(next), color = Color.White)
            } else {
                Text("${next.dutyType}: ${next.dutyNote}", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("${displayTime(next.departureDateTime)}-${displayTime(next.arrivalDateTime)}", color = Color.White)
            }
        }
    }
}

@Composable
fun FlightCard(flight: FlightEntity, onClick: () -> Unit, flightRepository: FlightRepository, showUtc: Boolean) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(flight.airline, color = ThaiPurple, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("   ${flight.flightNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = " ${flight.aircraftLabel} ",
                    color = Color.White,
                    modifier = Modifier.padding(start = 10.dp).background(ThaiPurple, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (flight.isCompleted) "COMPLETED" else if (flight.isRegistered) "REGISTERED" else flight.status,
                    color = SuccessGreen,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(flight.departureIata, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(flight.departureCity, style = MaterialTheme.typography.titleMedium)
                    Text(flight.departureAirport, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (showUtc) AirportDatabase.utcText(flight.departureDateTime, flight.departureIata) else displayTime(flight.departureDateTime),
                        style = if (showUtc) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (showUtc) "Local ${displayDate(flight.departureDateTime)} • ${displayDay(flight.departureDateTime)}" else displayDate(flight.departureDateTime),
                        color = TextMuted
                    )
                    Text("✈", color = ThaiPurple, style = MaterialTheme.typography.headlineMedium)
                    Text(formatMinutes(flight.durationMinutes), color = TextMuted)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(flight.arrivalIata, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(flight.arrivalCity, style = MaterialTheme.typography.titleMedium)
                    Text(flight.arrivalAirport, color = TextMuted)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AIRCRAFT", color = TextMuted, style = MaterialTheme.typography.labelLarge)
                    Text(flight.aircraftFullName, style = MaterialTheme.typography.titleMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text("REGISTRATION", color = TextMuted, style = MaterialTheme.typography.labelLarge)
                    Text(if (flight.registration == "TBA") "Assigned 24h prior" else flight.registration, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(airportAssignmentLine(flight), color = TextMuted)
            Text("Duty time: ${formatMinutes(dutyMinutes(flight.departureDateTime, flight.arrivalDateTime, flight.durationMinutes))}", color = TextMuted)
            if (flight.isRegistered) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Registered") }
            } else if (canRegister(flight.departureDateTime, flight.isCompleted)) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { scope.launch { flightRepository.registerFlight(flight.id) } }, modifier = Modifier.fillMaxWidth()) { Text("Register") }
            }
        }
    }
}

@Composable
fun DutyCard(flight: FlightEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(flight.dutyType, color = ThaiPurple, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(flight.status, color = TextMuted, fontWeight = FontWeight.Bold)
            }
            Text("${displayDate(flight.departureDateTime)} • ${displayTime(flight.departureDateTime)}-${displayTime(flight.arrivalDateTime)}")
            Text(flight.dutyNote.ifBlank { "Hotel standby duty" }, color = TextMuted)
            Text("Location: ${flight.departureAirport}", color = TextMuted)
        }
    }
}


private fun airportAssignmentLine(flight: FlightEntity): String {
    return when {
        flight.gate != "Pending" && flight.gate != "—" -> "Gate: ${flight.gate} • Terminal: ${flight.terminal}"
        flight.stand != "Pending" && flight.stand != "—" -> "Stand: ${flight.stand} • Terminal: ${flight.terminal}"
        else -> "Gate / Stand: assigned 3h prior"
    }
}
