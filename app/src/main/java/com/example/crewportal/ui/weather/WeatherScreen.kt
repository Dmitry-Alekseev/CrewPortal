package com.example.crewportal.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

    fun loadWeather(code: String) {
        scope.launch {
            loading = true
            error = null
            try {
                val clean = code.uppercase().take(4)
                val report = weatherRepository.getReport(clean)
                searchedIcao = clean
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
    }

    LaunchedEffect(Unit) {
        loadWeather("VTBS")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("METAR / TAF", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("BKK / VTBS loads automatically", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { loadWeather(searchedIcao) }, enabled = !loading) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        OutlinedTextField(
            value = icao,
            onValueChange = { icao = it.uppercase().take(4) },
            label = { Text("ICAO code") },
            placeholder = { Text("VTBS") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { loadWeather(icao) },
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
            Spacer(Modifier.height(10.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurface, lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun WeatherInterpretationCard(metar: String, taf: String) {
    val analysis = interpretWeather(metar + " " + taf)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Operational Conditions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(analysis.status, color = if (analysis.isGood) SuccessGreen else MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            analysis.notes.forEach { Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("Runway condition: ${analysis.runway}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class WeatherAnalysis(val status: String, val isGood: Boolean, val runway: String, val notes: List<String>)

private fun interpretWeather(raw: String): WeatherAnalysis {
    val text = raw.uppercase()
    val notes = mutableListOf<String>()
    var good = true
    if ("TS" in text) { notes += "Thunderstorm activity reported or forecast"; good = false }
    if ("CB" in text) { notes += "Cumulonimbus clouds present"; good = false }
    if ("+RA" in text || "SHRA" in text) { notes += "Heavy rain / showers may affect operation"; good = false }
    if ("FG" in text || "BR" in text) { notes += "Reduced visibility possible"; good = false }
    if ("WS" in text) { notes += "Windshear indication requires attention"; good = false }
    if ("CAVOK" in text) notes += "CAVOK conditions reported"
    if (notes.isEmpty()) notes += "No significant weather hazards detected from METAR/TAF"
    val runway = when {
        "+RA" in text || "TS" in text || "SHRA" in text -> "WET / monitor braking action"
        "FG" in text || "BR" in text -> "NORMAL / visibility monitoring required"
        else -> "NORMAL"
    }
    return WeatherAnalysis(if (good) "Good operational conditions" else "Weather requires attention", good, runway, notes)
}
