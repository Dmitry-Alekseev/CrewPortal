package com.example.crewportal.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.ui.theme.SuccessGreen
import com.example.crewportal.ui.theme.TextMuted
import com.example.crewportal.ui.theme.ThaiPurple
import com.example.crewportal.ui.theme.ThaiPurpleLight
import com.example.crewportal.util.canRegister
import com.example.crewportal.util.displayDate
import com.example.crewportal.util.displayDay
import com.example.crewportal.util.displayTime
import com.example.crewportal.util.formatMinutes
import kotlinx.coroutines.launch

@Composable
fun ScheduleScreen(
    flightRepository: FlightRepository,
    onFlightClick: (String) -> Unit
) {
    val flights by flightRepository.observeFlights().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) { flightRepository.refreshCompletedFlights() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Schedule", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("All times local", color = TextMuted)
            Spacer(Modifier.height(8.dp))
        }
        items(flights, key = { it.id }) { flight ->
            FlightCard(flight = flight, onClick = { onFlightClick(flight.id) }, flightRepository = flightRepository)
        }
    }
}

@Composable
fun FlightCard(
    flight: FlightEntity,
    onClick: () -> Unit,
    flightRepository: FlightRepository
) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(flight.airline, color = ThaiPurple, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("   ${flight.flightNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    color = if (flight.isCompleted || flight.isRegistered) SuccessGreen else SuccessGreen,
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
                    Text(displayTime(flight.departureDateTime), style = MaterialTheme.typography.headlineMedium)
                    Text(displayDate(flight.departureDateTime), color = TextMuted)
                    Text(displayDay(flight.departureDateTime), color = TextMuted)
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
                    Text(flight.registration, style = MaterialTheme.typography.titleMedium)
                }
            }
            if (flight.isRegistered) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Registered") }
            } else if (canRegister(flight.departureDateTime, flight.isCompleted)) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { scope.launch { flightRepository.registerFlight(flight.id) } },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Register") }
            }
        }
    }
}
