package com.example.crewportal.ui.emergency

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

@Composable
fun EmergencyReferenceScreen() {
    val items = listOf(
        "Evacuation" to "Cabin readiness, crew coordination, passenger management and post-evacuation accountability.",
        "Smoke / Fire / Fumes" to "Identify source, communicate with cabin, prepare diversion assessment and monitor systems.",
        "Depressurization" to "Oxygen, communication, descent profile and cabin status monitoring.",
        "Ditching" to "Landing preparation, cabin briefing, survival equipment and evacuation direction.",
        "Rejected Takeoff" to "Stop decision support, communication, brake cooling and cabin condition check.",
        "Medical Emergency" to "Cabin crew report, medical kit/AED status, doctor onboard and diversion decision support.",
        "Unruly Passenger" to "Cabin escalation, cockpit security, station support and authority notification."
    )
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Emergency / SEP Reference", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Quick training-style reference for recurrent preparation", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(items) { (title, body) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
