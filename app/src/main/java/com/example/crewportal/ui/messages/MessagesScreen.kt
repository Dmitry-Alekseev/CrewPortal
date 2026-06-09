package com.example.crewportal.ui.messages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.repository.PreferencesRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class CompanyMessageUi(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val date: LocalDateTime
)

@Composable
fun MessagesScreen(preferencesRepository: PreferencesRepository, ru: Boolean) {
    val enhancedTarget by preferencesRepository.enhancedRosterTarget.collectAsState(initial = false)
    var showRead by remember { mutableStateOf(false) }
    val readMessages = remember { mutableStateListOf<CompanyMessageUi>() }
    val selectedForDelete = remember { mutableStateListOf<String>() }

    val unread = buildList {
        if (enhancedTarget) {
            add(
                CompanyMessageUi(
                    id = "90h-extra-duty-pending",
                    title = if (ru) "Дополнительная duty — ожидает публикации" else "Additional duty — pending publication",
                    body = if (ru) "Выбран режим 90h. Дополнительная duty будет опубликована отдельным company message после финального назначения." else "90h target selected. Extra duty will be published through a company message after final assignment.",
                    category = if (ru) "Изменение расписания" else "Roster change",
                    date = LocalDateTime.now().minusHours(3)
                )
            )
        }
        add(
            CompanyMessageUi(
                id = "validity-reminder-sample",
                title = if (ru) "Проверка сроков квалификаций" else "Qualification validity check",
                body = if (ru) "Messages будет использоваться для напоминаний о медкомиссии, тренажёре и проверках за 14 дней до окончания срока." else "Messages will be used for medical, simulator and check validity reminders 14 days before expiry.",
                category = if (ru) "Сроки" else "Validity",
                date = LocalDateTime.now().minusDays(1)
            )
        )
    }.filterNot { unreadMessage -> readMessages.any { it.id == unreadMessage.id } }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (ru) "Сообщения" else "Messages", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            if (ru) "Важные сообщения компании: изменения ростера, payslip и сроки проверок." else "Important company messages: roster changes, payslips and validity reminders.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { showRead = false; selectedForDelete.clear() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showRead) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (!showRead) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) { Text(if (ru) "Входящие" else "Inbox") }
            Button(
                onClick = { showRead = true; selectedForDelete.clear() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showRead) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (showRead) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) { Text(if (ru) "Прочитанные" else "Read") }
        }

        if (showRead) {
            if (selectedForDelete.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        readMessages.removeAll { it.id in selectedForDelete }
                        selectedForDelete.clear()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (ru) "Удалить выбранные" else "Delete selected") }
            }
            if (readMessages.isEmpty()) {
                EmptyMessage(if (ru) "Прочитанных сообщений нет" else "No read messages")
            } else {
                readMessages.sortedByDescending { it.date }.forEach { message ->
                    ReadMessageCard(message, selectedForDelete.contains(message.id)) {
                        if (selectedForDelete.contains(message.id)) selectedForDelete.remove(message.id) else selectedForDelete.add(message.id)
                    }
                }
            }
        } else {
            if (unread.isEmpty()) {
                EmptyMessage(if (ru) "Новых важных сообщений нет" else "No important new messages")
            } else {
                unread.sortedByDescending { it.date }.forEach { message ->
                    UnreadMessageCard(message, ru) { readMessages.add(0, message.copy(date = LocalDateTime.now())) }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UnreadMessageCard(message: CompanyMessageUi, ru: Boolean, onAcknowledge: () -> Unit) {
    MessageCardBase(message = message) {
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAcknowledge, modifier = Modifier.fillMaxWidth()) {
            Text(if (ru) "Ознакомился" else "Acknowledge")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadMessageCard(message: CompanyMessageUi, selected: Boolean, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongPress),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
    ) {
        MessageContent(message, selectedPrefix = if (selected) "✓ " else "")
    }
}

@Composable
private fun MessageCardBase(message: CompanyMessageUi, bottomContent: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MessageHeader(message, selectedPrefix = "")
            Text(message.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            bottomContent()
        }
    }
}

@Composable
private fun MessageContent(message: CompanyMessageUi, selectedPrefix: String) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MessageHeader(message, selectedPrefix)
        Text(message.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MessageHeader(message: CompanyMessageUi, selectedPrefix: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(selectedPrefix + message.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(message.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(message.date.format(DateTimeFormatter.ofPattern("dd MMM HH:mm")).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
