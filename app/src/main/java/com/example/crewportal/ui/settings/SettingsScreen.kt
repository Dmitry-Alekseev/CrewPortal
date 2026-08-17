package com.example.crewportal.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.BuildConfig
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.data.update.AppUpdateInfo
import com.example.crewportal.data.update.UpdateRepository
import com.example.crewportal.ui.theme.CorporateBlue
import com.example.crewportal.ui.theme.CorporateBlueAccent
import com.example.crewportal.ui.theme.CorporateNeutral
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
    var versionTapCount by remember { mutableStateOf(0) }
    val secretUsed by preferencesRepository.secretRosterGeneratorUsed.collectAsState(initial = false)
    
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
                Button(
                    onClick = {
                        updateRepository.openDownload(info.apkUrl)
                        updateInfo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CorporateBlueAccent, contentColor = Color.White)
                ) { Text(if (ru) "Скачать APK" else "Download APK") }
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
                    Button(
                        onClick = { scope.launch { preferencesRepository.setAppLanguage("en") } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (language == "en") CorporateBlue else CorporateNeutral,
                            contentColor = if (language == "en") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("English") }

                    Button(
                        onClick = { scope.launch { preferencesRepository.setAppLanguage("ru") } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (language == "ru") CorporateBlue else CorporateNeutral,
                            contentColor = if (language == "ru") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Русский") }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (ru) "Приложение" else "Application", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Crew Portal ${BuildConfig.VERSION_NAME}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable {
                        versionTapCount += 1
                        if (versionTapCount >= 5) {
                            versionTapCount = 0
                            scope.launch {
                                if (secretUsed) {
                                    flightRepository.deleteNextMonthRosterDraft()
                                    snackbarHostState.showSnackbar(if (ru) "Черновик следующего месяца удалён" else "Next month draft deleted")
                                } else {
                                    flightRepository.generateJuneRosterTest()
                                    snackbarHostState.showSnackbar(if (ru) "Ростер следующего месяца сгенерирован" else "Next month roster generated")
                                }
                            }
                        }
                    }
                )
                Text(if (ru) "Пакет обновления: CrewPortal-${BuildConfig.VERSION_NAME}.apk" else "Update package: CrewPortal-${BuildConfig.VERSION_NAME}.apk", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (ru) "Генератор ростера: локальная подготовка следующего месяца" else "Roster generator: local next-month preparation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (ru) "Ростер: локальная база и генератор" else "Roster: local database and generator", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (ru) "MEL: локальная база по типам ВС и бортам" else "MEL: local aircraft technical database", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (ru) "Карта: OpenStreetMap / osmdroid" else "Map source: OpenStreetMap / osmdroid", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (ru) "Синхронизация" else "Synchronization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (ru) "Локальное обновление статусов ростера" else "Local roster status refresh", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { flightRepository.refreshCompletedFlights(showNotifications = true) }
                            snackbarHostState.showSnackbar(if (ru) "Локальный ростер обновлён" else "Local roster refreshed")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CorporateNeutral, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (ru) "Обновить статусы" else "Refresh status") }

                Button(onClick = {
                    scope.launch {
                        updateChecked = true
                        val info = withContext(Dispatchers.IO) { updateRepository.checkForUpdate() }
                        when {
                            info == null -> Toast.makeText(context, if (ru) "Служба обновлений недоступна" else "Update service unavailable", Toast.LENGTH_SHORT).show()
                            info.versionCode <= BuildConfig.VERSION_CODE -> Toast.makeText(context, if (ru) "Обновлений нет" else "Crew Portal is up to date", Toast.LENGTH_SHORT).show()
                            else -> updateInfo = info
                        }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = CorporateBlueAccent, contentColor = Color.White), modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Проверить обновления приложения" else "Check app updates") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Change Log — 2.2.10", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("• Route maps use the public OpenFreeMap vector basemap without an API key.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Medical, simulator and line-check validity dates now use a six-month cycle.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Generator reserves linked training/check events before ordinary flights.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Route times are persisted from directional five-minute min/max ranges.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Version metadata updated to 2.2.10.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Button(
            onClick = { scope.launch { flightRepository.simulateRosterChange() } },
            colors = ButtonDefaults.buttonColors(containerColor = CorporateNeutral, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (ru) "Имитировать изменение ростера" else "Simulate Roster Change") }
        Button(
            onClick = { scope.launch { onLogout() } },
            colors = ButtonDefaults.buttonColors(containerColor = CorporateNeutral, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (ru) "Выйти" else "Sign Out") }
    }
}
