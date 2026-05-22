package com.example.crewportal.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crewportal.data.airport.AirportDatabase
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.ui.theme.SuccessGreen
import com.example.crewportal.ui.theme.TextMuted
import com.example.crewportal.ui.theme.ThaiPurple
import com.example.crewportal.util.canRegister
import com.example.crewportal.util.displayDate
import com.example.crewportal.util.displayDay
import com.example.crewportal.util.displayTime
import com.example.crewportal.util.dutyMinutes
import com.example.crewportal.util.formatMinutes
import com.example.crewportal.util.parseLocalDateTime
import com.example.crewportal.util.reportDateTime
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleScreen(
    flightRepository: FlightRepository,
    preferencesRepository: PreferencesRepository,
    onDutyClick: (String) -> Unit
) {
    val flights by flightRepository.observeFlights().collectAsState(initial = emptyList())
    val darkTheme by preferencesRepository.darkTheme.collectAsState(initial = false)
    var showUtc by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { flightRepository.refreshCompletedFlights() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Roster", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Company roster synchronized", color = TextMuted)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("Dark theme", color = TextMuted, modifier = Modifier.weight(1f))
                Switch(
                    checked = darkTheme,
                    onCheckedChange = { value -> scope.launch { preferencesRepository.setDarkTheme(value) } }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    if (showUtc) "Local time with UTC reference" else "Local time",
                    color = TextMuted,
                    modifier = Modifier.weight(1f)
                )
                Text("UTC", color = TextMuted, modifier = Modifier.padding(end = 10.dp))
                Switch(checked = showUtc, onCheckedChange = { showUtc = it })
            }
            Spacer(Modifier.height(8.dp))
            MonthlyProgressCard(flights = flights)
            Spacer(Modifier.height(8.dp))
            TodayDutyCard(flights = flights, onDutyClick = onDutyClick)
        }

        items(flights, key = { it.id }) { duty ->
            if (duty.dutyType == "FLIGHT") {
                FlightCard(
                    flight = duty,
                    onClick = { onDutyClick(duty.id) },
                    flightRepository = flightRepository,
                    showUtc = showUtc
                )
            } else {
                DutyCard(flight = duty, onClick = { onDutyClick(duty.id) })
            }
        }
    }
}

@Composable
private fun MonthlyProgressCard(flights: List<FlightEntity>) {
    val monthPrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val monthLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val monthFlights = flights.filter { it.dutyType == "FLIGHT" && it.departureDateTime.startsWith(monthPrefix) }
    val planned = monthFlights.sumOf { it.durationMinutes }
    val completed = monthFlights.filter { it.isCompleted }.sumOf { it.durationMinutes }
    val target = 80 * 60
    val limit = 90 * 60

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Monthly Flight Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("$monthLabel • Planned ${formatMinutes(planned)} • Completed ${formatMinutes(completed)}", color = TextMuted)
            LinearProgressIndicator(progress = (planned.toFloat() / target.toFloat()).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
            Text("Target ${formatMinutes(target)} • Limit ${formatMinutes(limit)}", color = TextMuted)
        }
    }
}

@Composable
private fun TodayDutyCard(flights: List<FlightEntity>, onDutyClick: (String) -> Unit) {
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    val current = flights.firstOrNull { duty ->
        val dutyDate = parseLocalDateTime(duty.departureDateTime).toLocalDate()
        duty.dutyType != "OFF" && dutyDate == today && parseLocalDateTime(duty.arrivalDateTime).isAfter(now)
    }

    Card(colors = CardDefaults.cardColors(containerColor = ThaiPurple), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Today’s Duty", color = Color.White, fontWeight = FontWeight.Bold)
            if (current == null) {
                Text("No duty today", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Next duty will appear after 00:00 on its roster date.", color = Color.White.copy(alpha = 0.82f))
            } else if (current.dutyType == "FLIGHT") {
                Column(Modifier.clickable { onDutyClick(current.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${current.flightNumber} / ${current.departureIata}-${current.arrivalIata}", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Report ${reportDateTime(current.departureDateTime, current.durationMinutes).format(DateTimeFormatter.ofPattern("HH:mm"))} • Departure ${displayTime(current.departureDateTime)}", color = Color.White)
                    Text("Aircraft: ${if (current.registration == "TBA") "Assigned 24h prior" else current.registration}", color = Color.White)
                    Text(airportAssignmentLine(current), color = Color.White)
                }
            } else {
                Column(Modifier.clickable { onDutyClick(current.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(current.dutyType, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${displayTime(current.departureDateTime)}-${displayTime(current.arrivalDateTime)}", color = Color.White)
                    Text(current.dutyNote.ifBlank { "No operational flight duty" }, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

@Composable
fun FlightCard(flight: FlightEntity, onClick: () -> Unit, flightRepository: FlightRepository, showUtc: Boolean) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AirlineBadge(flight.airline)
                Text("  ${flight.flightNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = " ${flight.aircraftLabel} ",
                    color = Color.White,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .background(ThaiPurple, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
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
                Column(Modifier.weight(0.9f)) {
                    Text(flight.departureIata, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(flight.departureCity, style = MaterialTheme.typography.titleMedium)
                    Text(flight.departureAirport, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.35f)) {
                    Text(
                        text = scheduleTimeLine(flight, showUtc),
                        style = if (showUtc) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        fontSize = if (showUtc) 18.sp else MaterialTheme.typography.headlineMedium.fontSize
                    )
                    Text("${displayDate(flight.departureDateTime)} • ${displayDay(flight.departureDateTime)}", color = TextMuted, maxLines = 1, softWrap = false)
                    Text("✈", color = ThaiPurple, style = MaterialTheme.typography.headlineMedium)
                    Text(formatMinutes(flight.durationMinutes), color = TextMuted)
                }
                Column(Modifier.weight(0.9f), horizontalAlignment = Alignment.End) {
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
private fun AirlineBadge(airline: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(ThaiPurple.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color(0xFFFFC400), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✤", color = ThaiPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(" $airline", color = ThaiPurple, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DutyCard(flight: FlightEntity, onClick: () -> Unit) {
    val isOff = flight.dutyType == "OFF"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(if (isOff) "OFF" else flight.dutyType, color = ThaiPurple, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(flight.status, color = TextMuted, fontWeight = FontWeight.Bold)
            }
            Text("${displayDate(flight.departureDateTime)} • ${displayTime(flight.departureDateTime)}-${displayTime(flight.arrivalDateTime)}")
            Text(flight.dutyNote.ifBlank { if (isOff) "Day off" else "Hotel standby duty" }, color = TextMuted)
            if (!isOff) Text("Location: ${flight.departureAirport}", color = TextMuted)
        }
    }
}

private fun scheduleTimeLine(flight: FlightEntity, showUtc: Boolean): String {
    val local = displayTime(flight.departureDateTime)
    return if (showUtc) "$local • ${AirportDatabase.utcClockText(flight.departureDateTime, flight.departureIata)} UTC" else local
}

private fun airportAssignmentLine(flight: FlightEntity): String {
    return when {
        flight.gate != "Pending" && flight.gate != "—" -> "Gate: ${flight.gate} • Terminal: ${flight.terminal}"
        flight.stand != "Pending" && flight.stand != "—" -> "Stand: ${flight.stand} • Terminal: ${flight.terminal}"
        else -> "Gate / Stand: assigned 3h prior"
    }
}
