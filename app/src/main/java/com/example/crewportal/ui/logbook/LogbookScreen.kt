package com.example.crewportal.ui.logbook

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.ui.theme.TextMuted
import com.example.crewportal.util.displayDate
import com.example.crewportal.util.formatMinutes
import java.io.File

@Composable
fun LogbookScreen(flightRepository: FlightRepository) {
    val flights by flightRepository.observeCompleted().collectAsState(initial = emptyList())
    val context = LocalContext.current
    LaunchedEffect(Unit) { flightRepository.refreshCompletedFlights(showNotifications = false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Logbook", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Completed flights and export tools", color = TextMuted)
        Button(
            onClick = { exportLogbook(context, flights) },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            enabled = flights.isNotEmpty()
        ) { Text("Export Logbook CSV") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(flights, key = { it.id }) { flight ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(flight.flightNumber, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(formatMinutes(flight.durationMinutes))
                        }
                        Text("${flight.departureIata} → ${flight.arrivalIata}")
                        Text("${displayDate(flight.departureDateTime)} • ${flight.aircraftLabel} • ${flight.registration}", color = TextMuted)
                        Text("Role: Captain", color = TextMuted)
                    }
                }
            }
            if (flights.isEmpty()) {
                item { Text("No completed flights yet", color = TextMuted, modifier = Modifier.padding(top = 24.dp)) }
            }
        }
    }
}

private fun exportLogbook(context: android.content.Context, flights: List<FlightEntity>) {
    val csv = buildString {
        appendLine("Date,Flight,Route,Aircraft,Registration,Block Time,Role")
        flights.forEach { flight ->
            appendLine("${displayDate(flight.departureDateTime)},${flight.flightNumber},${flight.departureIata}-${flight.arrivalIata},${flight.aircraftLabel},${flight.registration},${formatMinutes(flight.durationMinutes)},Captain")
        }
    }
    val file = File(context.cacheDir, "crew_logbook_export.csv")
    file.writeText(csv)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Logbook CSV"))
}
