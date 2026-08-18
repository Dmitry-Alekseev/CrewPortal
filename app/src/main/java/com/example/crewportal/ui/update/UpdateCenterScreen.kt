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
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.BuildConfig
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
    val currentVersionCode = BuildConfig.VERSION_CODE
    val currentVersionName = BuildConfig.VERSION_NAME

    if (updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text(if (ru) "Доступно обновление" else "Update available") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Crew Portal ${info.latestVersion}", fontWeight = FontWeight.Bold)
                    (if (ru && info.changelogRu.isNotEmpty()) info.changelogRu else info.changelog).forEach { Text("• $it") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    updateRepository.openDownload(info.apkUrl)
                    updateInfo = null
                }) { Text(if (ru) "Скачать" else "Download") }
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
        Text(if (ru) "Синхронизация с сетью компании" else "Company network synchronization", color = MaterialTheme.colorScheme.onSurfaceVariant)

        InfoCard(if (ru) "Приложение" else "Application") {
            Text(if (ru) "Текущая версия: ${currentVersionName}" else "Current version: ${currentVersionName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (ru) "Служба обновлений: доступна" else "Application update service: available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = {
                scope.launch {
                    val info = withContext(Dispatchers.IO) { updateRepository.checkForUpdate() }
                    when {
                        info == null -> Toast.makeText(context, if (ru) "Служба обновлений временно недоступна" else "Update service temporarily unavailable", Toast.LENGTH_SHORT).show()
                        info.versionCode <= currentVersionCode -> Toast.makeText(context, if (ru) "Обновлений нет" else "Crew Portal is up to date", Toast.LENGTH_SHORT).show()
                        else -> updateInfo = info
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Проверить обновления" else "Check app update") }
        }

        InfoCard(if (ru) "Ростер" else "Roster") {
            Text(if (ru) "Источник: локальный генератор" else "Source: local roster generator", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (ru) "JSON-ростер больше не используется как источник расписания." else "Roster JSON is no longer used as a schedule source.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { flightRepository.syncRosterFromGitHub() }
                    snackbarHostState.showSnackbar(if (ok) { if (ru) "Ростер обновлён локально" else "Roster refreshed locally" } else { if (ru) "Сеть компании недоступна" else "Company network unavailable" })
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Обновить статусы ростера" else "Refresh roster status") }
        }

        InfoCard("MEL") {
            Text(if (ru) "Источник: база MEL по типам ВС" else "Source: aircraft technical database", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (ru) "MEL формируется по типу ВС, борту и текущему месяцу." else "MEL is generated by aircraft type, registration and current month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        InfoCard(if (ru) "Журнал изменений — 3.0.1" else "Change log — 3.0.1") {
            listOf(
                if (ru) "Одобренный отпуск через пять минут удаляет конфликтующие рейсы и дежурства и пересчитывает норму." else "Approved leave removes conflicting flights and duties after about five minutes and recalculates the target.",
                if (ru) "Генератор следующего месяца блокирует все сохранённые даты отпуска." else "Next-month generation blocks every persisted approved-leave date.",
                if (ru) "Добавлены отдельные роли КВС-инструктора и инструктора-наблюдателя; e-logbook на них скрыт." else "Captain instructor and instructor observer are separate roles; instructor e-logbook is hidden.",
                if (ru) "Подписи специальных TAS rotation убраны." else "Special TAS rotation labels were removed.",
                if (ru) "База действующих Airbus Thai Airways обновлена, включая девять A321neo." else "The active Thai Airways Airbus fleet was reconciled, including nine A321neo aircraft."
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
