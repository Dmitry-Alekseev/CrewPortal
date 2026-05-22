package com.example.crewportal.ui.fleet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.example.crewportal.data.fleet.AircraftPool
import com.example.crewportal.ui.theme.TextMuted
import com.example.crewportal.ui.theme.ThaiPurple

@Composable
fun FleetScreen() {
    val fleet = AircraftPool.aircraft.groupBy { it.label }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Airbus Fleet", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Local operational aircraft database", color = TextMuted)
        }
        fleet.forEach { (type, aircraft) ->
            item { Text(type, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ThaiPurple) }
            items(aircraft) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(item.registration, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(item.status, color = ThaiPurple, fontWeight = FontWeight.Bold)
                        }
                        Text(item.fullName)
                        Text("Configuration: ${item.configuration}", color = TextMuted)
                        Text("Engine type: ${item.engineType}", color = TextMuted)
                        Text("Age: ${item.age}", color = TextMuted)
                        Text("Operation class: ${item.routeClass}", color = TextMuted)
                    }
                }
            }
        }
    }
}
