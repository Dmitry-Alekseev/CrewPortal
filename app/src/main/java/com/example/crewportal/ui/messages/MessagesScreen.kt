package com.example.crewportal.ui.messages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.util.parseLocalDateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private data class CompanyMessageUi(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val date: LocalDateTime
)

private val CorporateBlue = Color(0xFF1F3A5F)
private val CorporateAccent = Color(0xFF2F5F88)
private val DeleteRed = Color(0xFF8B2F2F)

@Composable
fun MessagesScreen(flightRepository: FlightRepository, preferencesRepository: PreferencesRepository, ru: Boolean) {
    val scope = rememberCoroutineScope()
    val nextPrepared by preferencesRepository.nextMonthRosterPrepared.collectAsState(initial = false)
    val nextReviewed by preferencesRepository.nextMonthRosterReviewed.collectAsState(initial = false)
    val enhancedTarget by preferencesRepository.enhancedRosterTarget.collectAsState(initial = false)
    val readIds by preferencesRepository.readCompanyMessageIds.collectAsState(initial = emptySet())
    val deletedIds by preferencesRepository.deletedCompanyMessageIds.collectAsState(initial = emptySet())
    val flights by flightRepository.observeFlights().collectAsState(initial = emptyList())
    var showRead by remember { mutableStateOf(false) }
    val selectedForDelete = remember { mutableStateListOf<String>() }

    val allMessages = buildList {
        if (nextPrepared) {
            add(
                CompanyMessageUi(
                    id = "next-roster-ready",
                    title = if (ru) "Новый ростер готов" else "New roster available",
                    body = if (ru) {
                        if (nextReviewed) "Следующий месяц уже подтверждён. Он доступен как preview, текущий ростер остаётся активным." else "Расписание на следующий месяц готово. Откройте Calendar или Roster, проверьте график и подтвердите ознакомление."
                    } else {
                        if (nextReviewed) "The next month roster has been reviewed. It remains available as preview while the current roster stays active." else "Your next month roster is ready. Open Calendar or Roster, review the draft and acknowledge it."
                    },
                    category = if (ru) "Изменение расписания" else "Roster change",
                    date = LocalDateTime.now().minusMinutes(20)
                )
            )
        }
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
        flights
            .filter { it.dutyNote.contains("Manual operational roster change") && it.departureIata == "BKK" }
            .distinctBy { it.id }
            .forEach { duty ->
                add(
                    CompanyMessageUi(
                        id = "manual-change-${duty.id}",
                        title = if (ru) "Оперативное изменение ростера" else "Operational roster change",
                        body = if (ru) "${duty.flightNumber} BKK-${duty.arrivalIata} добавлен или изменён. Изменение уже отображается в Roster и Calendar." else "${duty.flightNumber} BKK-${duty.arrivalIata} has been added or changed. It is already visible in Roster and Calendar.",
                        category = if (ru) "Изменение расписания" else "Roster change",
                        date = parseLocalDateTime(duty.departureDateTime)
                    )
                )
            }
    }.filterNot { it.id in deletedIds }

    val unread = allMessages.filterNot { it.id in readIds }
    val read = allMessages.filter { it.id in readIds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (ru) "Сообщения" else "Messages", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            if (ru) "Важные сообщения компании: изменения ростера, payslip и сроки проверок." else "Important company messages: roster changes, payslips and validity reminders.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SegmentedMessageButton(
                text = if (ru) "Входящие" else "Inbox",
                selected = !showRead,
                modifier = Modifier.weight(1f),
                onClick = { showRead = false; selectedForDelete.clear() }
            )
            SegmentedMessageButton(
                text = if (ru) "Прочитанные" else "Read",
                selected = showRead,
                modifier = Modifier.weight(1f),
                onClick = { showRead = true; selectedForDelete.clear() }
            )
        }

        if (showRead) {
            if (selectedForDelete.isNotEmpty()) {
                Button(
                    onClick = {
                        val ids = selectedForDelete.toList()
                        selectedForDelete.clear()
                        scope.launch { preferencesRepository.deleteCompanyMessages(ids) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeleteRed, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (ru) "Удалить выбранные (${selectedForDelete.size})" else "Delete selected (${selectedForDelete.size})") }
            }
            if (read.isEmpty()) {
                EmptyMessage(if (ru) "Прочитанных сообщений нет" else "No read messages")
            } else {
                read.sortedByDescending { it.date }.forEach { message ->
                    ReadMessageCard(
                        message = message,
                        selected = selectedForDelete.contains(message.id),
                        selectionMode = selectedForDelete.isNotEmpty(),
                        onToggleSelection = {
                            if (selectedForDelete.contains(message.id)) selectedForDelete.remove(message.id) else selectedForDelete.add(message.id)
                        }
                    )
                }
            }
        } else {
            if (unread.isEmpty()) {
                EmptyMessage(if (ru) "Новых важных сообщений нет" else "No important new messages")
            } else {
                unread.sortedByDescending { it.date }.forEach { message ->
                    UnreadMessageCard(message, ru) {
                        scope.launch {
                            if (message.id == "90h-extra-duty-pending") {
                                flightRepository.publishExtraDutyForSelectedTarget()
                            }
                            preferencesRepository.markCompanyMessageRead(message.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedMessageButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) CorporateBlue else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) { Text(text) }
}

@Composable
private fun EmptyMessage(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UnreadMessageCard(message: CompanyMessageUi, ru: Boolean, onAcknowledge: () -> Unit) {
    MessageCardBase(message = message, unread = true) {
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onAcknowledge,
            colors = ButtonDefaults.buttonColors(containerColor = CorporateAccent, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (ru) "Ознакомился" else "Acknowledge")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadMessageCard(message: CompanyMessageUi, selected: Boolean, selectionMode: Boolean, onToggleSelection: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = { if (selectionMode) onToggleSelection() },
            onLongClick = onToggleSelection
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
    ) {
        MessageContent(message, selectedPrefix = if (selected) "✓ " else "")
    }
}

@Composable
private fun MessageCardBase(message: CompanyMessageUi, unread: Boolean = false, bottomContent: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (unread) Color(0xFFEAF2F8) else MaterialTheme.colorScheme.surface)
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
            Text(message.category, style = MaterialTheme.typography.labelMedium, color = CorporateAccent)
        }
        Text(
            message.date.format(DateTimeFormatter.ofPattern("dd MMM • HH:mm", Locale.ENGLISH)).uppercase(Locale.ENGLISH),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
