package com.example.crewportal.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.remote.WeatherRepository
import com.example.crewportal.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun WeatherScreen(weatherRepository: WeatherRepository) {
    val scope = rememberCoroutineScope()
    var icao by remember { mutableStateOf("VTBS") }
    var searchedIcao by remember { mutableStateOf("VTBS") }
    var loading by remember { mutableStateOf(false) }
    var metar by remember { mutableStateOf("") }
    var taf by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("METAR / TAF", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Enter airport ICAO code", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = icao,
            onValueChange = { icao = it.uppercase().take(4) },
            label = { Text("ICAO code") },
            placeholder = { Text("VTBS") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    try {
                        val report = weatherRepository.getReport(icao)
                        searchedIcao = icao
                        metar = report.first
                        taf = report.second
                    } catch (_: IOException) {
                        error = "No internet connection"
                    } catch (e: Exception) {
                        error = e.message ?: "Unable to load weather data"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading && icao.length == 4,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (loading) "Loading..." else if (metar.isBlank() && taf.isBlank()) "Search" else "Refresh") }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (metar.isNotBlank()) {
            WeatherCard("Current METAR • $searchedIcao", metar)
            WeatherInterpretationCard(metar = metar, taf = taf)
        }
        if (taf.isNotBlank()) WeatherCard("TAF • $searchedIcao", taf)
    }
}

@Composable
private fun WeatherCard(title: String, text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun WeatherInterpretationCard(metar: String, taf: String) {
    val analysis = interpretWeather(metar + " " + taf)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Operational Conditions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(analysis.status, color = if (analysis.hazards.isEmpty()) SuccessGreen else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            if (analysis.hazards.isEmpty()) {
                Text("No significant weather hazards detected in current METAR/TAF text.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                analysis.hazards.forEach { Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text("Runway condition estimate: ${analysis.brakingAction}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class WeatherAnalysis(val status: String, val hazards: List<String>, val brakingAction: String)

private fun interpretWeather(raw: String): WeatherAnalysis {
    val text = raw.uppercase()
    val hazards = buildList {
        if ("TS" in text) add("Thunderstorm activity reported or forecast")
        if ("CB" in text) add("Cumulonimbus cloud reported")
        if ("+RA" in text || "HVY RA" in text) add("Heavy rain may affect visibility and runway condition")
        if ("RA" in text && "+RA" !in text) add("Rain reported or forecast")
        if ("FG" in text || "BR" in text) add("Reduced visibility due to fog/mist")
        if ("WS" in text) add("Windshear information present")
        if ("G" in text && Regex("\\d{2}G\\d{2}KT").containsMatchIn(text)) add("Gusty wind reported")
    }
    val braking = when {
        hazards.any { it.contains("Heavy rain") || it.contains("Thunderstorm") } -> "Monitor — possible reduced braking action if runway is wet"
        hazards.any { it.contains("Rain") } -> "Good to medium expected if runway wet"
        else -> "Good / dry runway expected"
    }
    return WeatherAnalysis(if (hazards.isEmpty()) "Conditions look good" else "Weather requires attention", hazards, braking)
}
