package com.example.crewportal.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.leave.LeaveDatabase
import com.example.crewportal.data.leave.LeavePeriod
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.util.displayMonth
import com.example.crewportal.util.displayTime
import com.example.crewportal.util.formatMinutes
import com.example.crewportal.util.parseLocalDateTime
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(flightRepository: FlightRepository, preferencesRepository: PreferencesRepository, onDutyClick: (String) -> Unit = {}) {
    val duties by flightRepository.observeFlights().collectAsState(initial = emptyList())
    val language by preferencesRepository.appLanguage.collectAsState(initial = "en")
    val nextPrepared by preferencesRepository.nextMonthRosterPrepared.collectAsState(initial = false)
    val nextReviewed by preferencesRepository.nextMonthRosterReviewed.collectAsState(initial = false)
    val ru = language == "ru"
    val today = LocalDate.now()
    val currentMonth = YearMonth.from(today)
    var showTargetDialog by remember { mutableStateOf(false) }
    var viewGeneratedMonth by remember { mutableStateOf(false) }
    var monthGridView by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val nextMonth = currentMonth.plusMonths(1)
    val nextMonthHasRoster = duties.any { YearMonth.from(parseLocalDateTime(it.departureDateTime).toLocalDate()) == nextMonth }
    val canPreviewNextMonth = nextPrepared && nextMonthHasRoster
    val targetYearMonth = if (canPreviewNextMonth && viewGeneratedMonth) nextMonth else currentMonth
    val filtered = duties.filter {
        val date = parseLocalDateTime(it.departureDateTime).toLocalDate()
        YearMonth.from(date) == targetYearMonth
    }
    val grouped = filtered.groupBy { parseLocalDateTime(it.departureDateTime).toLocalDate() }.toSortedMap()
    val leaveGrouped = LeaveDatabase.leaveForMonth(targetYearMonth).flatMap { period ->
        generateSequence(maxOf(period.start, targetYearMonth.atDay(1))) { it.plusDays(1) }
            .takeWhile { !it.isAfter(minOf(period.end, targetYearMonth.atEndOfMonth())) }
            .map { it to period }
            .toList()
    }.groupBy({ it.first }, { it.second })
    val allDates = (grouped.keys + leaveGrouped.keys).toSortedSet()

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text(if (ru) "Подтверждение графика" else "Roster confirmation") },
            text = { Text(if (ru) "Выберите плановую норму для следующего месяца." else "Choose monthly target for the generated roster.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch { preferencesRepository.setNextMonthRosterDecision(reviewed = true, enhancedTarget = false) }
                    showTargetDialog = false
                }) { Text(if (ru) "80 часов" else "Standard 80h") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    scope.launch { preferencesRepository.setNextMonthRosterDecision(reviewed = true, enhancedTarget = true) }
                    showTargetDialog = false
                }) { Text(if (ru) "90 часов" else "Enhanced 90h") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(if (ru) "Календарь ростера" else "Roster Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text((if (ru) "Месяц: " else "Roster month: ") + displayMonth(targetYearMonth.atDay(1)), color = MaterialTheme.colorScheme.onSurfaceVariant)
            CalendarViewSegmentedControl(
                monthGridView = monthGridView,
                ru = ru,
                onList = { monthGridView = false },
                onMonth = { monthGridView = true },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
            if (canPreviewNextMonth && !viewGeneratedMonth) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (ru) "Расписание на ${displayMonth(nextMonth.atDay(1))} готово" else "${displayMonth(nextMonth.atDay(1))} roster is ready",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (nextReviewed) {
                                if (ru) "Следующий месяц доступен для просмотра. Текущий ростер остаётся активным." else "The next month is available as a preview. The current roster remains active."
                            } else {
                                if (ru) "Откройте календарь следующего месяца, проверьте график и подтвердите ознакомление." else "Open the next month calendar, review the roster and confirm it."
                            },
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Button(onClick = { viewGeneratedMonth = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (ru) "Показать следующий месяц" else "Show next month")
                        }
                    }
                }
            } else if (canPreviewNextMonth && viewGeneratedMonth) {
                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (nextReviewed) {
                            if (ru) "Следующий месяц открыт как preview" else "Next month preview"
                        } else {
                            if (ru) "Новый ростер открыт для ознакомления" else "New roster is open for review"
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(onClick = { viewGeneratedMonth = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (ru) "Вернуться к текущему месяцу" else "Back to current month")
                    }
                }
            }
        }
        if (monthGridView) {
            item { CalendarMonthGrid(targetYearMonth, grouped, leaveGrouped, ru, onDutyClick) }
        } else {
            items(allDates.toList()) { date -> CalendarDayCard(date, grouped[date].orEmpty(), leaveGrouped[date].orEmpty(), ru) }
        }
        if (canPreviewNextMonth && !nextReviewed && viewGeneratedMonth) {
            item {
                Button(onClick = { showTargetDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (ru) "Я ознакомился с графиком" else "I have reviewed this roster")
                }
            }
        }
    }
}

@Composable
private fun CalendarViewSegmentedControl(
    monthGridView: Boolean,
    ru: Boolean,
    onList: () -> Unit,
    onMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CalendarSegment(
            text = if (ru) "Список" else "List",
            selected = !monthGridView,
            onClick = onList,
            modifier = Modifier.weight(1f)
        )
        CalendarSegment(
            text = if (ru) "Месяц" else "Month",
            selected = monthGridView,
            onClick = onMonth,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CalendarSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(15.dp)
    val color = if (selected) Color(0xFF52627A) else Color.Transparent
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable(onClick = onClick),
        shape = shape,
        color = color,
        tonalElevation = if (selected) 1.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = contentColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    dutiesByDate: Map<LocalDate, List<FlightEntity>>,
    leavesByDate: Map<LocalDate, List<LeavePeriod>>,
    ru: Boolean,
    onDutyClick: (String) -> Unit
) {
    val firstDay = month.atDay(1)
    val leadingBlanks = firstDay.dayOfWeek.value - 1
    val days = buildList<LocalDate?> {
        repeat(leadingBlanks) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
        while (size % 7 != 0) add(null)
    }
    val weekdays = if (ru) listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС") else listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                weekdays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { date ->
                        CalendarGridCell(
                            date = date,
                            duties = if (date == null) emptyList() else dutiesByDate[date].orEmpty(),
                            leaves = if (date == null) emptyList() else leavesByDate[date].orEmpty(),
                            ru = ru,
                            onDutyClick = onDutyClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (ru) "✈ рейс · 🏨 отдых · R резерв · L отпуск" else "✈ flight · 🏨 stay · R reserve · L leave",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarGridCell(
    date: LocalDate?,
    duties: List<FlightEntity>,
    leaves: List<LeavePeriod>,
    ru: Boolean,
    onDutyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (date == null) {
        Box(modifier.height(72.dp))
        return
    }

    val flightDuty = duties.firstOrNull { it.dutyType == "FLIGHT" }
    val clickableDuty = flightDuty ?: duties.firstOrNull { it.dutyType != "OFF" } ?: duties.firstOrNull()
    val hasFlight = duties.any { it.dutyType == "FLIGHT" }
    val hasStay = duties.any { it.dutyType == "STAY" }
    val hasReserve = duties.any { it.dutyType == "RESERVE" }
    val hasOff = duties.any { it.dutyType == "OFF" }
    val hasLeave = leaves.isNotEmpty()
    val today = date == LocalDate.now()
    val containerColor = when {
        hasLeave -> Color(0xFF4FC3F7).copy(alpha = 0.22f)
        hasFlight -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        hasStay -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        hasReserve -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
        hasOff -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.48f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Surface(
        modifier = modifier
            .height(72.dp)
            .then(if (clickableDuty != null) Modifier.clickable { onDutyClick(clickableDuty.id) } else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = if (today) 2.dp else 0.dp
    ) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (today) FontWeight.Bold else FontWeight.SemiBold,
                color = if (today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when {
                    hasLeave -> "L"
                    hasFlight -> "✈"
                    hasStay -> "🏨"
                    hasReserve -> "R"
                    hasOff -> if (ru) "В" else "OFF"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (hasFlight && flightDuty != null) {
                Text(
                    text = "${flightDuty.departureIata}-${flightDuty.arrivalIata}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
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
                val label = if (duty.dutyType == "FLIGHT") "${duty.flightNumber} ${duty.departureIata}-${duty.arrivalIata}" else if (duty.dutyType == "OFF") { if (ru) "ВЫХОДНОЙ" else "OFF" } else if (duty.dutyType == "RESERVE") { if (ru) "РЕЗЕРВ" else "RESERVE" } else if (duty.dutyType == "STAY") { if (ru) "ОТДЫХ В ${duty.departureCity.uppercase()}" else "Stay in ${duty.departureCity}" } else duty.dutyType
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
