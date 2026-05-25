package com.example.crewportal.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
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
fun SettingsScreen(
    flightRepository: FlightRepository,
    preferencesRepository: PreferencesRepository,
    onLogout: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val updateRepository = remember { UpdateRepository(context.applicationContext) }
    val language by preferencesRepository.appLanguage.collectAsState(initial = "en")
    val ru = language == "ru"
    val snackbarHostState = remember { SnackbarHostState() }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var updateChecked by remember { mutableStateOf(false) }

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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SnackbarHost(snackbarHostState)
        Text(if (ru) "Настройки" else "Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (ru) "Язык" else "Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (ru) "Текущий язык интерфейса: русский" else "Current interface language: English",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (language == "en") Button(onClick = { scope.launch { preferencesRepository.setAppLanguage("en") } }) { Text("English") }
                    else OutlinedButton(onClick = { scope.launch { preferencesRepository.setAppLanguage("en") } }) { Text("English") }

                    if (language == "ru") Button(onClick = { scope.launch { preferencesRepository.setAppLanguage("ru") } }) { Text("Русский") }
                    else OutlinedButton(onClick = { scope.launch { preferencesRepository.setAppLanguage("ru") } }) { Text("Русский") }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (ru) "Приложение" else "Application", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Crew Portal 1.8.1", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (ru) "Пакет обновления: CrewPortal-1.8.1.apk" else "Update package: CrewPortal-1.8.1.apk", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (ru) "Ростер: сеть компании + локальная база" else "Roster sync: company network + local database", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (ru) "MEL: сеть компании + локальная база по бортам" else "MEL: company network + local defects database by aircraft", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (ru) "Карта: OpenStreetMap / osmdroid" else "Map source: OpenStreetMap / osmdroid", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (ru) "Синхронизация" else "Synchronization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (ru) "Подключение к сети компании активно" else "Company network connection available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { flightRepository.syncRosterFromGitHub() }
                        snackbarHostState.showSnackbar(if (ok) { if (ru) "Ростер обновлён" else "Roster updated successfully" } else { if (ru) "Сеть компании недоступна, выполнено локальное обновление" else "Company network unavailable, local refresh completed" })
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Обновить ростер" else "Refresh roster") }

                Button(onClick = {
                    scope.launch {
                        updateChecked = true
                        val info = withContext(Dispatchers.IO) { updateRepository.checkForUpdate() }
                        if (info == null) snackbarHostState.showSnackbar(if (ru) "Обновления не найдены" else "No update information found") else updateInfo = info
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Проверить обновления приложения" else "Check app updates") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Change Log — 1.8.1", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("• Leave Management and sick leave controls added.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Monthly target now adjusts to leave and sick leave.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Today’s Duty now supports pre-flight, in-flight, turnaround and leave states.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Weather interpretation and runway condition estimate added.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• New launcher icon and roster UI polish.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Button(onClick = { scope.launch { flightRepository.simulateRosterChange() } }, modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Имитировать изменение ростера" else "Simulate Roster Change") }
        Button(onClick = { scope.launch { onLogout() } }, modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Выйти" else "Sign Out") }
    }
}
