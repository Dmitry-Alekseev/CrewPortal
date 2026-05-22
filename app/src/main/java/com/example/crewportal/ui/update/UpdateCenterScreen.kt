package com.example.crewportal.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.data.update.AppUpdateInfo
import com.example.crewportal.data.update.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UpdateCenterScreen(
    flightRepository: FlightRepository,
    preferencesRepository: PreferencesRepository
) {
    val context = LocalContext.current
    val updateRepository = remember { UpdateRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val language by preferencesRepository.appLanguage.collectAsState(initial = "en")
    val ru = language == "ru"
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    if (updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text(if (ru) "Доступно обновление" else "Update available") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Crew Portal ${info.latestVersion}", fontWeight = FontWeight.Bold)
                    info.changelog.forEach { Text("• $it") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    updateRepository.openDownload(info.apkUrl)
                    updateInfo = null
                }) { Text(if (ru) "Скачать APK" else "Download APK") }
            },
            dismissButton = { TextButton(onClick = { updateInfo = null }) { Text(if (ru) "Позже" else "Later") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SnackbarHost(snackbarHostState)
        Text(if (ru) "Центр обновлений" else "Update Center", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Dmitry-Alekseev/CrewPortal", color = MaterialTheme.colorScheme.onSurfaceVariant)

        InfoCard(if (ru) "Приложение" else "Application") {
            Text("Current version: 1.7.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Latest version source: GitHub app_update.json", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = {
                scope.launch {
                    val info = withContext(Dispatchers.IO) { updateRepository.checkForUpdate() }
                    if (info == null) snackbarHostState.showSnackbar(if (ru) "Данные об обновлении недоступны" else "Update information unavailable") else updateInfo = info
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Проверить обновления" else "Check app update") }
        }

        InfoCard(if (ru) "Ростер" else "Roster") {
            Text("Source: roster/current_roster.json", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { flightRepository.syncRosterFromGitHub() }
                    snackbarHostState.showSnackbar(if (ok) { if (ru) "Ростер обновлён" else "Roster updated successfully" } else { if (ru) "GitHub недоступен" else "GitHub unavailable" })
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Обновить ростер" else "Refresh roster") }
        }

        InfoCard("MEL") {
            Text("Source: mel/current_mel.json", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (ru) "MEL обновляется при открытии экрана борта." else "MEL data is refreshed when an aircraft MEL screen is opened.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        InfoCard("Change log — 1.7.0") {
            listOf(
                "Update Center added",
                "MEL database can sync from GitHub",
                "Roster refresh preserves registration and completed state",
                "Flight briefing package reorganized",
                "Airport info and fatigue monitor added",
                "Aircraft technical status integrated with MEL"
            ).forEach { Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
