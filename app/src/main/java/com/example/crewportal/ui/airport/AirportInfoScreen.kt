package com.example.crewportal.ui.airport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.airport.AirportDatabase
import com.example.crewportal.data.airport.AirportInfo

@Composable
fun AirportInfoScreen() {
    var query by remember { mutableStateOf("") }
    val airports = remember(query) { AirportDatabase.search(query) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Airport Info", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Search by ICAO, IATA, city or airport name", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.uppercase() },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search airport") },
                placeholder = { Text("VTBS, WSSS, EDDF, LTFM, BKK, Frankfurt...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
        }

        if (airports.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No airport found", fontWeight = FontWeight.Bold)
                        Text("Try ICAO code, IATA code, city or airport name.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(airports) { airport -> AirportCard(airport) }
        }
    }
}

@Composable
private fun AirportCard(airport: AirportInfo) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("${airport.iata} / ${airport.icao}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${airport.name} • ${airport.city}, ${airport.country}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(AirportDatabase.localOffsetText(airport.iata), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            Detail("METAR / TAF", airport.metarIcao)
            Detail("Elevation", "${airport.elevationFt} ft")
            Detail("Runways", airport.runways)
            Detail("Terminal / company notes", airport.terminalNotes)
            Detail("Operational notes", airport.operationalNotes)
            Detail("ATC / station notes", airport.atcNotes)
            Text("Use ${airport.metarIcao} in Weather search for current METAR / TAF.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value)
    }
}
