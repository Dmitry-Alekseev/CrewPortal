package com.example.crewportal.ui.schedule

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crewportal.R
import com.example.crewportal.data.airport.AirportDatabase
import com.example.crewportal.data.leave.LeaveDatabase
import com.example.crewportal.data.leave.LeavePeriod
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.ui.theme.SuccessGreen
import com.example.crewportal.ui.theme.ThaiPurple
import com.example.crewportal.util.canRegister
import com.example.crewportal.util.displayDate
import com.example.crewportal.util.displayDay
import com.example.crewportal.util.displayTime
import com.example.crewportal.util.dutyMinutes
import com.example.crewportal.util.formatMinutes
import com.example.crewportal.util.parseLocalDateTime
import com.example.crewportal.util.reportDateTime
import com.example.crewportal.util.shouldShowRegistrationButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed class RosterItem {
    abstract val sortDateTime: LocalDateTime
    data class FlightDuty(val flight: FlightEntity) : RosterItem() {
        override val sortDateTime: LocalDateTime = parseLocalDateTime(flight.departureDateTime)
    }
    data class LeaveDuty(val period: LeavePeriod, val date: LocalDate) : RosterItem() {
        override val sortDateTime: LocalDateTime = date.atStartOfDay()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ScheduleScreen(
    flightRepository: FlightRepository,
    preferencesRepository: PreferencesRepository,
    onDutyClick: (String) -> Unit,
    onMelClick: (String) -> Unit
) {
    val flights by flightRepository.observeFlights().collectAsState(initial = emptyList())
    val darkTheme by preferencesRepository.darkTheme.collectAsState(initial = false)
    val language by preferencesRepository.appLanguage.collectAsState(initial = "en")
    val nextPrepared by preferencesRepository.nextMonthRosterPrepared.collectAsState(initial = false)
    val nextReviewed by preferencesRepository.nextMonthRosterReviewed.collectAsState(initial = false)
    val enhancedTarget by preferencesRepository.enhancedRosterTarget.collectAsState(initial = false)
    val ru = language == "ru"
    var showUtc by remember { mutableStateOf(false) }
    var showNextRoster by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val now = LocalDateTime.now()
    val targetMonth = if (showNextRoster && nextReviewed) YearMonth.now().plusMonths(1) else YearMonth.now()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { flightRepository.refreshCompletedFlights() }

    val refreshRoster: () -> Unit = {
        if (!isRefreshing) {
            scope.launch {
                isRefreshing = true
                val synced = withContext(Dispatchers.IO) { flightRepository.syncRosterFromGitHub() }
                if (!synced) flightRepository.refreshCompletedFlights()
                isRefreshing = false
                snackbarHostState.showSnackbar(if (ru) "Ростер обновлён" else "Roster updated successfully")
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = refreshRoster)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            SnackbarHost(hostState = snackbarHostState)
            Spacer(Modifier.height(12.dp))
            Text(if (ru) "Ростер" else "Roster", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            CompanySyncLine(ru = ru)
            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(if (ru) "Тёмная тема" else "Dark theme", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Switch(checked = darkTheme, onCheckedChange = { value -> scope.launch { preferencesRepository.setDarkTheme(value) } })
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(if (showUtc) "Local time with UTC reference" else "Local time", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("UTC", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 10.dp))
                Switch(checked = showUtc, onCheckedChange = { showUtc = it })
            }

            Spacer(Modifier.height(8.dp))
            MonthlyProgressCard(flights = flights, month = targetMonth, ru = ru)
            Spacer(Modifier.height(8.dp))
            TodayDutyCard(flights = flights, onDutyClick = onDutyClick, ru = ru)
            if (nextPrepared && nextReviewed) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showNextRoster = !showNextRoster }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (showNextRoster) {
                            if (ru) "Вернуться к текущему ростеру" else "Back to current roster"
                        } else {
                            if (ru) "Показать ростер следующего месяца" else "Show next month roster"
                        }
                    )
                }
                Text(
                    if (enhancedTarget) {
                        if (ru) "Выбран усиленный план: 90 часов" else "Enhanced target selected: 90h"
                    } else {
                        if (ru) "Выбран стандартный план: 80 часов" else "Standard target selected: 80h"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val displayItems = buildRosterItems(flights, targetMonth, now)

        items(displayItems, key = { item ->
            when (item) {
                is RosterItem.FlightDuty -> item.flight.id
                is RosterItem.LeaveDuty -> item.period.id + item.date.toString()
            }
        }) { item ->
            when (item) {
                is RosterItem.FlightDuty -> {
                    val duty = item.flight
                    if (duty.dutyType == "FLIGHT") {
                        FlightCard(
                            flight = duty,
                            onClick = { onDutyClick(duty.id) },
                            flightRepository = flightRepository,
                            showUtc = showUtc,
                            ru = ru
                        )
                    } else {
                        DutyCard(flight = duty, onClick = if (duty.dutyType == "OFF") null else ({ onDutyClick(duty.id) }))
                    }
                }
                is RosterItem.LeaveDuty -> LeaveRosterCard(item.period, item.date)
            }
        }
            item { Spacer(Modifier.height(18.dp)) }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun CompanySyncLine(ru: Boolean) {
    val transition = rememberInfiniteTransition(label = "syncPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(520), repeatMode = RepeatMode.Reverse),
        label = "syncAlpha"
    )
    Text(
        if (ru) "Синхронизировано с сетью компании" else "Company network synchronized",
        color = SuccessGreen,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.alpha(alpha)
    )
}

private fun buildRosterItems(flights: List<FlightEntity>, month: YearMonth, now: LocalDateTime): List<RosterItem> {
    val leaveByDate = LeaveDatabase.leaveForMonth(month).flatMap { period ->
        generateSequence(maxOf(period.start, month.atDay(1))) { it.plusDays(1) }
            .takeWhile { !it.isAfter(minOf(period.end, month.atEndOfMonth())) }
            .map { date -> date to period }
            .toList()
    }.toMap()

    val flightItems = flights.mapNotNull { duty ->
        val departure = parseLocalDateTime(duty.departureDateTime)
        val arrival = parseLocalDateTime(duty.arrivalDateTime)
        val date = departure.toLocalDate()
        if (YearMonth.from(date) != month || !arrival.isAfter(now)) return@mapNotNull null
        if (leaveByDate.containsKey(date)) return@mapNotNull null
        RosterItem.FlightDuty(duty)
    }
    val leaveItems = leaveByDate.map { (date, leave) -> RosterItem.LeaveDuty(leave, date) }
        .filter { !it.date.atTime(23, 59).isBefore(now) }
    return (flightItems + leaveItems).sortedBy { it.sortDateTime }
}

@Composable
private fun MonthlyProgressCard(flights: List<FlightEntity>, month: YearMonth, ru: Boolean) {
    val monthDate = month.atDay(1)
    val monthPrefix = monthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val monthLabel = monthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
    val monthFlights = flights.filter { it.dutyType == "FLIGHT" && it.departureDateTime.startsWith(monthPrefix) && LeaveDatabase.leaveFor(parseLocalDateTime(it.departureDateTime).toLocalDate()) == null }
    val planned = monthFlights.sumOf { it.durationMinutes }
    val completed = monthFlights.filter { it.isCompleted }.sumOf { it.durationMinutes }
    val adjustedTarget = LeaveDatabase.adjustedMonthlyTargetMinutes(month)
    val limit = 90 * 60
    val progress = (completed.toFloat() / adjustedTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (ru) "Месячный налёт" else "Monthly Flight Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(monthLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricBlock("Planned", formatMinutes(planned), Modifier.weight(1f))
                MetricBlock("Completed", formatMinutes(completed), Modifier.weight(1f))
            }
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
            Text("Adjusted target ${formatMinutes(adjustedTarget)} • Limit ${formatMinutes(limit)} • Leave ${LeaveDatabase.leaveDaysInMonth(month)} days", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(Locale.ENGLISH), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TodayDutyCard(flights: List<FlightEntity>, onDutyClick: (String) -> Unit, ru: Boolean) {
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    val leave = LeaveDatabase.leaveFor(today)
    val todayFlights = flights.filter { it.dutyType == "FLIGHT" && parseLocalDateTime(it.departureDateTime).toLocalDate() == today }.sortedBy { it.departureDateTime }
    val currentOrNext = todayFlights.firstOrNull { parseLocalDateTime(it.arrivalDateTime).isAfter(now) }
    val previous = todayFlights.lastOrNull { parseLocalDateTime(it.arrivalDateTime).isBefore(now) }

    Card(colors = CardDefaults.cardColors(containerColor = ThaiPurple), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (ru) "Сегодня" else "Today’s Duty", color = Color.White, fontWeight = FontWeight.Bold)
            when {
                leave != null -> {
                    Text(leave.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${formatDate(leave.start)} — ${formatDate(leave.end)} • Day ${java.time.temporal.ChronoUnit.DAYS.between(leave.start, today).toInt() + 1} of ${leave.days}", color = Color.White.copy(alpha = 0.86f))
                }
                currentOrNext == null -> {
                    Text(if (ru) "Сегодня duty нет" else "No duty today", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (ru) "Следующая duty появится после 00:00 в дату ростера." else "Next duty will appear after 00:00 on its roster date.", color = Color.White.copy(alpha = 0.82f))
                }
                now.isBefore(parseLocalDateTime(currentOrNext.departureDateTime)) && previous != null -> {
                    Column(Modifier.clickable { onDutyClick(currentOrNext.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Turnaround", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Next sector ${currentOrNext.flightNumber} ${currentOrNext.departureIata}-${currentOrNext.arrivalIata}", color = Color.White)
                        Text("Departure ${displayTime(currentOrNext.departureDateTime)}", color = Color.White.copy(alpha = 0.88f))
                    }
                }
                now.isBefore(parseLocalDateTime(currentOrNext.departureDateTime)) -> {
                    Column(Modifier.clickable { onDutyClick(currentOrNext.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Pre-flight", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${currentOrNext.flightNumber} / ${currentOrNext.departureIata}-${currentOrNext.arrivalIata}", color = Color.White)
                        Text("Report ${reportDateTime(currentOrNext.departureDateTime, currentOrNext.durationMinutes).format(DateTimeFormatter.ofPattern("HH:mm"))} • Departure ${displayTime(currentOrNext.departureDateTime)}", color = Color.White)
                    }
                }
                now.isBefore(parseLocalDateTime(currentOrNext.arrivalDateTime)) -> {
                    Column(Modifier.clickable { onDutyClick(currentOrNext.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("In flight", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${currentOrNext.flightNumber} ${currentOrNext.departureIata}-${currentOrNext.arrivalIata}", color = Color.White)
                        Text("Estimated arrival ${displayTime(currentOrNext.arrivalDateTime)}", color = Color.White.copy(alpha = 0.88f))
                    }
                }
            }
        }
    }
}

@Composable
fun FlightCard(flight: FlightEntity, onClick: () -> Unit, flightRepository: FlightRepository, showUtc: Boolean, ru: Boolean) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AirlineBadge(flight.airline)
                Text("  ${flight.flightNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = " ${flight.aircraftLabel} ",
                    color = Color.White,
                    modifier = Modifier.padding(start = 10.dp).background(ThaiPurple, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.weight(1f))
                Text(if (flight.isCompleted) { if (ru) "ВЫПОЛНЕНО" else "COMPLETED" } else if (flight.isRegistered) { if (ru) "ЗАРЕГИСТРИРОВАН" else "REGISTERED" } else { if (ru) "ПЛАН" else flight.status }, color = SuccessGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(0.9f)) {
                    Text(flight.departureIata, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(flight.departureCity, style = MaterialTheme.typography.titleMedium)
                    Text(flight.departureAirport, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.35f)) {
                    Text(scheduleTimeLine(flight, showUtc), style = if (showUtc) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, fontSize = if (showUtc) 18.sp else MaterialTheme.typography.headlineMedium.fontSize)
                    Text("${displayDate(flight.departureDateTime)} • ${displayDay(flight.departureDateTime)}", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
                    Text("✈", color = ThaiPurple, style = MaterialTheme.typography.headlineMedium)
                    Text(formatMinutes(flight.durationMinutes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(0.9f), horizontalAlignment = Alignment.End) {
                    Text(flight.arrivalIata, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(flight.arrivalCity, style = MaterialTheme.typography.titleMedium)
                    Text(flight.arrivalAirport, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (ru) "ВС" else "AIRCRAFT", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    Text(flight.aircraftFullName, style = MaterialTheme.typography.titleMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (ru) "БОРТ" else "REGISTRATION", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    Text(if (flight.registration == "TBA") { if (ru) "За 24 часа" else "Assigned 24h prior" } else flight.registration, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            BriefingLine(flight = flight, ru = ru)
            airportAssignmentLine(flight).takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text((if (ru) "Время duty: " else "Duty time: ") + formatMinutes(dutyMinutes(flight.departureDateTime, flight.arrivalDateTime, flight.durationMinutes)), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (flight.isRegistered) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = {}, enabled = true, colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Зарегистрирован" else "Registered") }
            } else if (shouldShowRegistrationButton(flight.departureIata, flight.durationMinutes) && canRegister(flight.departureDateTime, flight.isCompleted)) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { scope.launch { flightRepository.registerFlight(flight.id) } }, modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Регистрация" else "Register") }
            }
        }
    }
}

@Composable
private fun BriefingLine(flight: FlightEntity, ru: Boolean) {
    val departure = parseLocalDateTime(flight.departureDateTime)
    val arrival = parseLocalDateTime(flight.arrivalDateTime)
    val briefing = departure.minusMinutes(90).format(DateTimeFormatter.ofPattern("HH:mm"))
    val debriefing = arrival.plusMinutes(30).format(DateTimeFormatter.ofPattern("HH:mm"))
    val longHaul = flight.durationMinutes >= 360
    val shortReturnHome = flight.durationMinutes < 360 && flight.arrivalIata == "BKK" && flight.departureIata != "BKK"

    if (!shortReturnHome) {
        Text(
            (if (ru) "Явка: " else "Briefing: ") + briefing,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (longHaul || flight.arrivalIata == "BKK") {
        Text(
            (if (ru) "Разбор: " else "Debriefing: ") + debriefing,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AirlineBadge(airline: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)).border(1.dp, ThaiPurple.copy(alpha = 0.45f), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Image(painter = painterResource(R.drawable.thai_logo), contentDescription = "Thai Airways logo", modifier = Modifier.size(width = 82.dp, height = 28.dp))
    }
}

@Composable
fun DutyCard(flight: FlightEntity, onClick: (() -> Unit)?) {
    val isOff = flight.dutyType == "OFF"
    val isStay = flight.dutyType == "STAY"
    val title = when {
        isOff -> "OFF"
        isStay -> flight.flightNumber
        else -> flight.dutyType
    }
    Card(modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, color = if (isOff) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(flight.status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
            Text("${displayDate(flight.departureDateTime)} • ${displayTime(flight.departureDateTime)}-${displayTime(flight.arrivalDateTime)}")
            Text(flight.dutyNote.ifBlank { if (isOff) "Day off" else if (isStay) "Layover stay" else "Hotel standby duty" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!isOff) Text("Location: ${flight.departureAirport}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LeaveRosterCard(period: LeavePeriod, date: LocalDate) {
    Card(shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(period.title.uppercase(Locale.ENGLISH), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (period.type == "SICK_LEAVE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Text(period.status, color = SuccessGreen, fontWeight = FontWeight.Bold)
            }
            Text(formatDate(date))
            Text(period.note.ifBlank { "Roster blocked by approved leave" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun scheduleTimeLine(flight: FlightEntity, showUtc: Boolean): String {
    val local = displayTime(flight.departureDateTime)
    return if (showUtc) "$local • ${AirportDatabase.utcClockText(flight.departureDateTime, flight.departureIata)} UTC" else local
}

private fun airportAssignmentLine(flight: FlightEntity): String {
    if (!shouldShowAirportAssignment(flight)) return ""
    return when {
        flight.gate != "Pending" && flight.gate != "—" -> "Gate: ${flight.gate} • Terminal: ${flight.terminal}"
        flight.stand != "Pending" && flight.stand != "—" -> "Stand: ${flight.stand} • Terminal: ${flight.terminal}"
        else -> "Gate / Stand: assigned 3h prior"
    }
}

private fun shouldShowAirportAssignment(flight: FlightEntity): Boolean {
    return flight.departureIata == "BKK" || flight.durationMinutes >= 360
}

private fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)).uppercase(Locale.ENGLISH)
