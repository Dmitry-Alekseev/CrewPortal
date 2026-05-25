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
import androidx.compose.runtime.collectAsState
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
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun WeatherScreen(weatherRepository: WeatherRepository, preferencesRepository: PreferencesRepository) {
    val scope = rememberCoroutineScope()
    val language by preferencesRepository.appLanguage.collectAsState(initial = "en")
    val ru = language == "ru"
    var icao by remember { mutableStateOf("VTBS") }
    var searchedIcao by remember { mutableStateOf("VTBS") }
    var loading by remember { mutableStateOf(false) }
    var metar by remember { mutableStateOf(DEFAULT_BKK_METAR) }
    var taf by remember { mutableStateOf(DEFAULT_BKK_TAF) }
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
                Text(if (ru) "Погода METAR / TAF" else "METAR / TAF", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(if (searchedIcao == "VTBS") { if (ru) "BKK / VTBS отображается сразу" else "BKK / VTBS shown by default" } else { if (ru) "Показана погода $searchedIcao" else "$searchedIcao weather shown" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { loadWeather(searchedIcao) }, enabled = !loading) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        OutlinedTextField(
            value = icao,
            onValueChange = { icao = it.uppercase().take(4) },
            label = { Text(if (ru) "Код ICAO" else "ICAO code") },
            placeholder = { Text("VTBS") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { loadWeather(icao) },
            enabled = !loading && icao.length == 4,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (metar.isBlank() && taf.isBlank()) { if (ru) "Поиск" else "Search" } else { if (ru) "Обновить" else "Refresh" }) }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (metar.isNotBlank()) {
            WeatherCard(if (ru) "Фактический METAR • $searchedIcao" else "Current METAR • $searchedIcao", metar)
            WeatherInterpretationCard(metar = metar, taf = taf, ru = ru)
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
private fun WeatherInterpretationCard(metar: String, taf: String, ru: Boolean) {
    val analysis = interpretWeather(metar + " " + taf)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (ru) "Оценка погодных условий" else "Operational Conditions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (ru) analysis.statusRu else analysis.status, color = if (analysis.isGood) SuccessGreen else MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            (if (ru) analysis.notesRu else analysis.notes).forEach { Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(if (ru) "Состояние ВПП: ${analysis.runwayRu}" else "Runway condition: ${analysis.runway}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


private const val DEFAULT_BKK_METAR = "VTBS 230100Z 18005KT 9999 FEW020 SCT035 30/24 Q1010 NOSIG"
private const val DEFAULT_BKK_TAF = "TAF VTBS 230000Z 2300/2406 18006KT 9999 FEW020 SCT035 TX34/2307Z TN27/2322Z TEMPO 2308/2314 4000 TSRA FEW018CB SCT030"

private data class WeatherAnalysis(val status: String, val statusRu: String, val isGood: Boolean, val runway: String, val runwayRu: String, val notes: List<String>, val notesRu: List<String>)

private fun interpretWeather(raw: String): WeatherAnalysis {
    val text = raw.uppercase()
    val notes = mutableListOf<String>()
    val notesRu = mutableListOf<String>()
    var good = true
    fun add(en: String, ru: String, bad: Boolean = false) {
        notes += en
        notesRu += ru
        if (bad) good = false
    }
    if ("TS" in text) add("Thunderstorm activity reported or forecast", "Есть грозовая активность в METAR/TAF", true)
    if ("CB" in text) add("Cumulonimbus clouds present", "Отмечены облака CB", true)
    if ("+RA" in text || "SHRA" in text) add("Heavy rain / showers may affect operation", "Сильный дождь или ливневые осадки могут влиять на выполнение рейса", true)
    if ("FG" in text || "BR" in text) add("Reduced visibility possible", "Возможна сниженная видимость", true)
    if ("WS" in text) add("Windshear indication requires attention", "Есть указание на windshear, требуется внимание", true)
    if ("CAVOK" in text) add("CAVOK conditions reported", "Условия CAVOK")
    if (notes.isEmpty()) add("No significant weather hazards detected from METAR/TAF", "Опасные погодные явления по METAR/TAF не выявлены")
    val runway = when {
        "+RA" in text || "TS" in text || "SHRA" in text -> "WET / monitor braking action"
        "FG" in text || "BR" in text -> "NORMAL / visibility monitoring required"
        else -> "NORMAL"
    }
    val runwayRu = when {
        "+RA" in text || "TS" in text || "SHRA" in text -> "ВЛАЖНАЯ / контролировать сцепление"
        "FG" in text || "BR" in text -> "НОРМАЛЬНАЯ / контролировать видимость"
        else -> "НОРМАЛЬНАЯ"
    }
    return WeatherAnalysis(
        status = if (good) "Good operational conditions" else "Weather requires attention",
        statusRu = if (good) "Условия хорошие" else "Погода требует внимания",
        isGood = good,
        runway = runway,
        runwayRu = runwayRu,
        notes = notes,
        notesRu = notesRu
    )
}
