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
    var showOneTimeGeneratorConfirmation by remember { mutableStateOf(false) }
    val secretUsed by preferencesRepository.secretRosterGeneratorUsed.collectAsState(initial = false)

    if (showOneTimeGeneratorConfirmation) {
        AlertDialog(
            onDismissRequest = { showOneTimeGeneratorConfirmation = false },
            title = { Text(if (ru) "Сгенерировать следующий месяц?" else "Generate next month?") },
            text = {
                Text(
                    if (ru) "Это одноразовый тестовый запуск до штатной даты. После успешной генерации скрытая команда навсегда отключится на этом устройстве."
                    else "This is a one-time early QA run before the normal generation date. After success, the hidden command is permanently disabled on this device."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showOneTimeGeneratorConfirmation = false
                    scope.launch {
                        val generated = withContext(Dispatchers.IO) { flightRepository.generateNextMonthRosterOnce() }
                        snackbarHostState.showSnackbar(
                            if (generated) {
                                if (ru) "Следующий месяц сгенерирован; команда отключена" else "Next month generated; one-time command disabled"
                            } else {
                                if (ru) "Команда уже использована или ростер существует" else "Command already used or next roster already exists"
                            }
                        )
                    }
                }) { Text(if (ru) "Сгенерировать" else "Generate") }
            },
            dismissButton = { TextButton(onClick = { showOneTimeGeneratorConfirmation = false }) { Text(if (ru) "Отмена" else "Cancel") } }
        )
    }
    
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
                                    snackbarHostState.showSnackbar(if (ru) "Одноразовая команда уже использована" else "One-time command has already been used")
                                } else {
                                    showOneTimeGeneratorConfirmation = true
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
                Text("Change Log — 3.0.1", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("• Approved leave rewrites conflicting roster duties after about five minutes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Next-month preview now shows only the return-to-previous-month action.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Instructor observer and operating captain instructor are separate roles; instructor e-logbook is hidden.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Tashkent flights use ordinary flight labels.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Active Thai Airways Airbus registrations were reconciled, including nine A321neo aircraft.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
