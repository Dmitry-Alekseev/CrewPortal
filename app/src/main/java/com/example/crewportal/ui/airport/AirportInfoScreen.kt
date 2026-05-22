package com.example.crewportal.ui.airport

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.airport.AirportDatabase
import com.example.crewportal.data.airport.AirportInfo

@Composable
fun AirportInfoScreen() {
    val airports = AirportDatabase.all()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Airport Info", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Runways, timezone, station notes and briefing data", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(airports) { airport -> AirportCard(airport) }
    }
}

@Composable
private fun AirportCard(airport: AirportInfo) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${airport.iata} / ${airport.icao}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${airport.name} • ${airport.city}, ${airport.country}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Detail("Timezone", AirportDatabase.localOffsetText(airport.iata))
            Detail("Runways", runwaysFor(airport.iata))
            Detail("Station notes", stationNotes(airport.iata))
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value)
    }
}

private fun runwaysFor(iata: String): String = when (iata) {
    "BKK" -> "01L/19R, 01R/19L"
    "HKT" -> "09/27"
    "CXR" -> "02/20"
    "SIN" -> "02L/20R, 02C/20C, 02R/20L"
    "IST" -> "16L/R, 17L/R, 18/36"
    "FRA" -> "07/25 complex, 18"
    else -> "See company briefing"
}

private fun stationNotes(iata: String): String = when (iata) {
    "BKK" -> "Home base. Crew transport and line maintenance available."
    "HKT", "CNX" -> "Domestic turnaround / layover station."
    "IST", "FRA", "CDG", "MEL", "SYD" -> "Long-haul station. Hotel pickup by local handler."
    else -> "Standard company handling available."
}
