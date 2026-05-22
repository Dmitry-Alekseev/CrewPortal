package com.example.crewportal.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.messages.CompanyMessages
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.ui.theme.TextMuted
import com.example.crewportal.ui.theme.ThaiPurple
import com.example.crewportal.util.canRegister
import com.example.crewportal.util.displayDate

@Composable
fun NotificationsScreen(flightRepository: FlightRepository) {
    val flights by flightRepository.observeFlights().collectAsState(initial = emptyList())
    val openFlights = flights.filter { it.dutyType == "FLIGHT" && canRegister(it.departureDateTime, it.isCompleted) }
    val completed = flights.filter { it.isCompleted }.takeLast(5)
    val airportAssigned = flights.filter { it.dutyType == "FLIGHT" && (it.gate != "Pending" || it.stand != "Pending") && !it.isCompleted }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Notifications", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Crew portal messages and operational alerts", color = TextMuted)
        }
        if (openFlights.isEmpty() && completed.isEmpty()) {
            item { NotificationCard("Roster synchronized", "Company Crew Portal synchronization successful. No urgent actions.") }
        }
        openFlights.forEach { flight ->
            item { NotificationCard("Registration window opened", "${flight.flightNumber} ${flight.departureIata}-${flight.arrivalIata}: check-in available. Aircraft: ${if (flight.registration == "TBA") "pending assignment" else flight.registration}.") }
        }
        airportAssigned.forEach { flight ->
            val position = when {
                flight.gate != "Pending" && flight.gate != "—" -> "Gate ${flight.gate}"
                flight.stand != "Pending" && flight.stand != "—" -> "Stand ${flight.stand}"
                else -> "Pending"
            }
            item { NotificationCard("Airport assignment updated", "${flight.flightNumber} ${flight.departureIata}-${flight.arrivalIata}: $position, ${flight.terminal}.") }
        }
        completed.forEach { flight ->
            item { NotificationCard("Flight time updated", "${flight.flightNumber} on ${displayDate(flight.departureDateTime)} completed. Block time added automatically.") }
        }
        CompanyMessages.messages.forEach { message ->
            item { NotificationCard("Company message", message) }
        }
        item { NotificationCard("Medical status", "VLEK valid. Next commission due in August 2026.") }
        item { NotificationCard("Training status", "ASP Land, ASP Water and simulator session valid until August 2026.") }
    }
}

@Composable
private fun NotificationCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = ThaiPurple, fontWeight = FontWeight.Bold)
            Text(body, color = TextMuted)
        }
    }
}
