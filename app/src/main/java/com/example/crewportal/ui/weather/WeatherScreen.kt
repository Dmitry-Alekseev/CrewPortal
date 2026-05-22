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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.remote.WeatherRepository
import com.example.crewportal.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun WeatherScreen(weatherRepository: WeatherRepository) {
    val scope = rememberCoroutineScope()
    var icao by remember { mutableStateOf("VTBS") }
    var loading by remember { mutableStateOf(false) }
    var metar by remember { mutableStateOf("") }
    var taf by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("METAR / TAF", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Enter airport ICAO code", color = TextMuted)
        OutlinedTextField(
            value = icao,
            onValueChange = { icao = it.uppercase().take(4) },
            label = { Text("ICAO code") },
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
        ) { Text(if (loading) "Loading..." else "Search") }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (metar.isNotBlank()) WeatherCard("METAR", metar)
        if (taf.isNotBlank()) WeatherCard("TAF", taf)
    }
}

@Composable
private fun WeatherCard(title: String, text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(text)
        }
    }
}
