package com.example.crewportal.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    flightRepository: FlightRepository,
    onLogout: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Application", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Crew Portal 1.0", color = TextMuted)
                Text("Schedule source: local JSON file", color = TextMuted)
                Text("Weather source: aviationweather.gov", color = TextMuted)
            }
        }
        Button(
            onClick = { scope.launch { flightRepository.reloadScheduleFromAssets() } },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reload Schedule from JSON") }

        Button(
            onClick = { scope.launch { onLogout() } },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sign Out") }
    }
}
