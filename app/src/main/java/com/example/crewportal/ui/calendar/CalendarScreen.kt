package com.example.crewportal.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.leave.LeaveDatabase
import com.example.crewportal.data.leave.LeavePeriod
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.util.displayDay
import com.example.crewportal.util.displayMonth
import com.example.crewportal.util.displayShortDate
import com.example.crewportal.util.displayTime
import com.example.crewportal.util.formatMinutes
import com.example.crewportal.util.parseLocalDateTime
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(flightRepository: FlightRepository, preferencesRepository: PreferencesRepository) {
    val duties by flightRepository.observeFlights().collectAsState(initial = emptyList())
    val language by preferencesRepository.appLanguage.collectAsState(initial = "en")
    val ru = language == "ru"
    val nextMonthReviewed by preferencesRepository.nextMonthRosterReviewed.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    var showNextMonth by remember { mutableStateOf(false) }
    var showRosterDecisionDialog by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val targetMonth = if (showNextMonth) today.plusMonths(1) else today
    val canPreviewNext = today.dayOfMonth >= today.lengthOfMonth() - 6
    val filtered = duties.filter {
        val date = parseLocalDateTime(it.departureDateTime).toLocalDate()
        date.year == targetMonth.year && date.month == targetMonth.month
    }
    val grouped = filtered.groupBy { parseLocalDateTime(it.departureDateTime).toLocalDate() }.toSortedMap()
    val leaveGrouped = LeaveDatabase.leaveForMonth(java.time.YearMonth.from(targetMonth)).flatMap { period ->
        generateSequence(maxOf(period.start, java.time.YearMonth.from(targetMonth).atDay(1))) { it.plusDays(1) }
            .takeWhile { !it.isAfter(minOf(period.end, java.time.YearMonth.from(targetMonth).atEndOfMonth())) }
            .map { it to period }
            .toList()
    }.groupBy({ it.first }, { it.second })
    val allDates = (grouped.keys + leaveGrouped.keys).toSortedSet()

    if (showRosterDecisionDialog) {
        AlertDialog(
            onDismissRequest = { showRosterDecisionDialog = false },
            title = { Text(if (ru) "Ростер просмотрен?" else "Roster reviewed?") },
            text = { Text(if (ru) "Подтвердите ознакомление. Выберите стандартную норму 80 часов или усиленный месяц до 90 часов." else "Confirm that you reviewed the next roster. Choose standard 80h target or enhanced month up to 90h.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch { preferencesRepository.setNextMonthRosterDecision(reviewed = true, enhancedTarget = false) }
                    showRosterDecisionDialog = false
                }) { Text("80h") }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch { preferencesRepository.setNextMonthRosterDecision(reviewed = true, enhancedTarget = true) }
                    showRosterDecisionDialog = false
                }) { Text("90h") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(if (ru) "Календарь ростера" else "Roster Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text((if (ru) "Месяц: " else "Roster month: ") + displayMonth(targetMonth), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (canPreviewNext || showNextMonth) {
                OutlinedButton(onClick = { showNextMonth = !showNextMonth }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(if (showNextMonth) { if (ru) "Вернуться к текущему месяцу" else "Back to current month" } else { if (ru) "Показать следующий месяц" else "Show next month roster" })
                }
            }
        }
        items(allDates.toList()) { date -> CalendarDayCard(date, grouped[date].orEmpty(), leaveGrouped[date].orEmpty(), ru) }
        if (showNextMonth) {
            item {
                if (nextMonthReviewed) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (ru) "Ростер подтверждён" else "Roster reviewed", fontWeight = FontWeight.Bold)
                            Text(if (ru) "Следующий месяц доступен в Roster." else "The next month roster is now available in Roster.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Button(
                        onClick = { showRosterDecisionDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp)
                    ) {
                        Text(if (ru) "Я ознакомился с ростером" else "I have reviewed this roster")
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCard(date: LocalDate, duties: List<FlightEntity>, leaves: List<LeavePeriod>, ru: Boolean) {
    val isPast = date.isBefore(LocalDate.now())
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM", java.util.Locale.ENGLISH)).uppercase(), fontWeight = FontWeight.Bold, color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    Text(date.format(java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH)).uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val block = duties.filter { it.dutyType == "FLIGHT" }.sumOf { it.durationMinutes }
                if (block > 0) Text((if (ru) "Налёт " else "Block ") + formatMinutes(block), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            leaves.distinctBy { it.id }.forEach { leave ->
                val chipColor = when (leave.type) {
                    "ANNUAL_LEAVE" -> Color(0xFFFFD54F).copy(alpha = 0.55f)
                    "PERSONAL_LEAVE" -> Color(0xFF4FC3F7).copy(alpha = 0.45f)
                    "SICK_LEAVE" -> Color(0xFFEF5350).copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(Modifier.fillMaxWidth().background(chipColor, RoundedCornerShape(10.dp)).padding(10.dp)) {
                    Column {
                        Text(localizedLeaveTitle(leave, ru), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (ru) localizedLeaveNote(leave) else leave.note.ifBlank { "Approved leave" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            val visibleDuties = if (leaves.isNotEmpty()) emptyList() else duties
            visibleDuties.forEach { duty ->
                val label = if (duty.dutyType == "FLIGHT") "${duty.flightNumber} ${duty.departureIata}-${duty.arrivalIata}" else if (duty.dutyType == "OFF") { if (ru) "ВЫХОДНОЙ" else "OFF" } else if (duty.dutyType == "RESERVE") { if (ru) "РЕЗЕРВ" else "RESERVE" } else if (duty.dutyType == "STAY") duty.flightNumber else duty.dutyType
                val chipColor = when (duty.dutyType) {
                    "FLIGHT" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    "RESERVE" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                    "TRAINING" -> Color(0xFFFFB74D).copy(alpha = 0.26f)
                    "OFF" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.62f)
                    "STAY" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(Modifier.fillMaxWidth().background(chipColor, RoundedCornerShape(10.dp)).padding(10.dp)) {
                    Column {
                        Text(label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("${displayTime(duty.departureDateTime)}-${displayTime(duty.arrivalDateTime)} • ${duty.dutyNote.ifBlank { duty.aircraftLabel }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}


private fun localizedLeaveTitle(leave: LeavePeriod, ru: Boolean): String {
    if (!ru) return leave.title.uppercase()
    return when (leave.type) {
        "ANNUAL_LEAVE" -> "ОСНОВНОЙ ОТПУСК"
        "PERSONAL_LEAVE" -> "ЛИЧНЫЙ ОТПУСК"
        "SICK_LEAVE" -> "БОЛЬНИЧНЫЙ"
        else -> leave.title.uppercase()
    }
}

private fun localizedLeaveNote(leave: LeavePeriod): String = when (leave.type) {
    "ANNUAL_LEAVE" -> "Назначенный отпуск компании"
    "PERSONAL_LEAVE" -> "Одобренный личный отпуск"
    "SICK_LEAVE" -> "Больничный"
    else -> "Одобрено"
}
