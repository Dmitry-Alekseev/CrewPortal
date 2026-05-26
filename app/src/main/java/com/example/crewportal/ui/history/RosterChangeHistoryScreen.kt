package com.example.crewportal.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.util.displayDate

@Composable
fun RosterChangeHistoryScreen(flightRepository: FlightRepository) {
    val flights by flightRepository.observeFlights().collectAsState(initial = emptyList())
    val events = buildList {
        flights.filter { it.changeNotified || it.dutyNote.contains("Roster change", ignoreCase = true) }.forEach { add("Roster change" to it) }
    }.sortedByDescending { it.second.departureDateTime }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Roster Change History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Operational roster changes only", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (events.isEmpty()) item { HistoryCard("No roster changes", "No operational roster changes recorded yet.") }
        items(events.take(30)) { (title, flight) -> HistoryCard(title, lineFor(flight)) }
    }
}

@Composable
private fun HistoryCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun lineFor(flight: FlightEntity): String {
    val route = if (flight.dutyType == "FLIGHT") "${flight.flightNumber} ${flight.departureIata}-${flight.arrivalIata}" else flight.flightNumber
    val note = flight.dutyNote.ifBlank { flight.status }
    return "${displayDate(flight.departureDateTime)} • $route • $note"
}
