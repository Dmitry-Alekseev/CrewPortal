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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crewportal.R
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
import com.example.crewportal.util.nowAtAirport
import com.example.crewportal.util.reportDateTime
import com.example.crewportal.util.shouldShowRegistrationButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private sealed class RosterItem {
    abstract val sortDateTime: LocalDateTime
    data class FlightDuty(val flight: FlightEntity) : RosterItem() {
        override val sortDateTime: LocalDateTime = parseLocalDateTime(flight.departureDateTime)
    }
    data class LeaveDuty(val period: LeavePeriod, val date: LocalDate) : RosterItem() {
        override val sortDateTime: LocalDateTime = date.atStartOfDay()
    }
    data class RestPeriod(
        val start: LocalDateTime,
        val end: LocalDateTime,
        val nextDutyLabel: String,
        val offDays: Int,
        val leaveDays: Int,
        val layoverDays: Int
    ) : RosterItem() {
        override val sortDateTime: LocalDateTime = end.minusMinutes(1)
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
    val enhancedTarget by preferencesRepository.enhancedRosterTarget.collectAsState(initial = false)
    val ru = language == "ru"
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val now = nowAtAirport("BKK")
    val currentMonth = YearMonth.from(now)
    val nextMonth = currentMonth.plusMonths(1)
    val nextMonthHasRoster = nextPrepared && flights.any { YearMonth.from(parseLocalDateTime(it.departureDateTime).toLocalDate()) == nextMonth }
    var showNextMonthPreview by remember { mutableStateOf(false) }
    var hiddenOperationalTapCount by remember { mutableStateOf(0) }
    var showOperationalChangeDialog by remember { mutableStateOf(false) }
    val targetMonth = if (showNextMonthPreview && nextMonthHasRoster) nextMonth else currentMonth
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { flightRepository.refreshCompletedFlights(showNotifications = false) }

    val refreshRoster: () -> Unit = {
        if (!isRefreshing) {
            scope.launch {
                isRefreshing = true
                withContext(Dispatchers.IO) { flightRepository.refreshCompletedFlights(showNotifications = false) }
                isRefreshing = false
                snackbarHostState.showSnackbar(if (ru) "Ростер обновлён" else "Roster updated successfully")
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = refreshRoster)

    if (showOperationalChangeDialog) {
        OperationalRosterChangeDialog(
            ru = ru,
            onDismiss = { showOperationalChangeDialog = false },
            onSubmit = { date, reportTime, outboundFlight, destination, aircraft, registration, pattern, returnFlight, returnDate, returnTime, replaceExisting ->
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        flightRepository.addOperationalRosterChange(
                            date = date,
                            reportTime = reportTime,
                            outboundFlight = outboundFlight,
                            destinationIata = destination,
                            aircraftLabel = aircraft,
                            registration = registration,
                            pattern = pattern,
                            returnFlight = returnFlight,
                            returnDate = returnDate,
                            returnTime = returnTime,
                            replaceExisting = replaceExisting
                        )
                    }
                    showOperationalChangeDialog = false
                    snackbarHostState.showSnackbar(if (ok) { if (ru) "Оперативное изменение внесено" else "Operational roster change added" } else { if (ru) "Не удалось внести изменение" else "Unable to add change" })
                }
            }
        )
    }

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
            CompanySyncLine(ru = ru, onSecretTap = {
                hiddenOperationalTapCount += 1
                if (hiddenOperationalTapCount >= 5) {
                    hiddenOperationalTapCount = 0
                    showOperationalChangeDialog = true
                }
            })
            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(if (ru) "Тёмная тема" else "Dark theme", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Switch(checked = darkTheme, onCheckedChange = { value -> scope.launch { preferencesRepository.setDarkTheme(value) } })
            }
            Spacer(Modifier.height(8.dp))
            MonthlyProgressCard(flights = flights, month = targetMonth, selected90 = enhancedTarget, ru = ru)
            if (nextMonthHasRoster) {
                Spacer(Modifier.height(8.dp))
                if (showNextMonthPreview) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showNextMonthPreview = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F3A5F), contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (ru) "Показать текущий месяц" else "Show current month")
                        }
                        OutlinedButton(onClick = { showNextMonthPreview = true }, enabled = false, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                            Text(if (ru) "Следующий месяц" else "Next month")
                        }
                    }
                } else {
                    OutlinedButton(onClick = { showNextMonthPreview = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(if (ru) "Показать следующий месяц" else "Show next month")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TodayDutyCard(flights = flights, onDutyClick = onDutyClick, ru = ru)
        }

        val displayItems = buildRosterItems(flights, targetMonth, now)

        items(displayItems, key = { item ->
            when (item) {
                is RosterItem.FlightDuty -> item.flight.id
                is RosterItem.LeaveDuty -> item.period.id + item.date.toString()
                is RosterItem.RestPeriod -> "REST-" + item.end.toString() + item.nextDutyLabel
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
                            ru = ru
                        )
                    } else {
                        DutyCard(flight = duty, onClick = if (duty.dutyType == "OFF") null else ({ onDutyClick(duty.id) }))
                    }
                }
                is RosterItem.LeaveDuty -> LeaveRosterCard(item.period, item.date)
                is RosterItem.RestPeriod -> RestPeriodCard(item, ru)
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
private fun CompanySyncLine(ru: Boolean, onSecretTap: () -> Unit) {
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
        modifier = Modifier.alpha(alpha).clickable { onSecretTap() }
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
        if (YearMonth.from(date) != month) return@mapNotNull null
        if (leaveByDate.containsKey(date)) return@mapNotNull null
        // Keep historical roster records in the database, but remove anything that is
        // already over from the active Roster list. This applies to flights and also
        // non-flight entries such as OFF, RESERVE and STAY.
        if (duty.isCompleted || !arrival.isAfter(now)) return@mapNotNull null
        RosterItem.FlightDuty(duty)
    }
    val leaveItems = leaveByDate.map { (date, leave) -> RosterItem.LeaveDuty(leave, date) }
        .filter { YearMonth.from(it.date) == month && it.date.plusDays(1).atStartOfDay().isAfter(now) }
    return insertRestPeriods((flightItems + leaveItems).sortedBy { it.sortDateTime })
}

private fun insertRestPeriods(items: List<RosterItem>): List<RosterItem> {
    val result = mutableListOf<RosterItem>()
    var previousWorkDuty: FlightEntity? = null

    items.forEach { item ->
        val currentWorkDuty = (item as? RosterItem.FlightDuty)?.flight?.takeIf { it.isOperationalDuty() }
        val previousDuty = previousWorkDuty
        if (currentWorkDuty != null && previousDuty != null) {
            val restStart = dutyEndForRest(previousDuty)
            val restEnd = dutyStartForRest(currentWorkDuty)
            val restMinutes = ChronoUnit.MINUTES.between(restStart, restEnd)
            val connectedSameDuty = previousDuty.isConnectedTurnaroundSector(currentWorkDuty)
            if (!connectedSameDuty && restMinutes >= 8 * 60) {
                result += RosterItem.RestPeriod(
                    start = restStart,
                    end = restEnd,
                    nextDutyLabel = currentWorkDuty.restLabel(),
                    offDays = items.countRestDutyDays(restStart, restEnd, "OFF"),
                    leaveDays = items.countLeaveDays(restStart, restEnd),
                    layoverDays = items.countRestDutyDays(restStart, restEnd, "STAY")
                )
            }
        }
        result += item
        if (currentWorkDuty != null) previousWorkDuty = currentWorkDuty
    }
    return result
}

private fun FlightEntity.isOperationalDuty(): Boolean = dutyType != "OFF" && dutyType != "STAY"

private fun FlightEntity.restLabel(): String = when (dutyType) {
    "FLIGHT" -> "$flightNumber $departureIata-$arrivalIata"
    else -> flightNumber.ifBlank { dutyType }
}

private fun FlightEntity.isConnectedTurnaroundSector(next: FlightEntity): Boolean {
    if (dutyType != "FLIGHT" || next.dutyType != "FLIGHT") return false
    val thisDepartureDate = parseLocalDateTime(departureDateTime).toLocalDate()
    val nextDepartureDate = parseLocalDateTime(next.departureDateTime).toLocalDate()
    return thisDepartureDate == nextDepartureDate && arrivalIata == next.departureIata
}

private fun dutyStartForRest(flight: FlightEntity): LocalDateTime {
    val departure = parseLocalDateTime(flight.departureDateTime)
    return if (flight.dutyType == "FLIGHT") departure.minusMinutes(90) else departure
}

private fun dutyEndForRest(flight: FlightEntity): LocalDateTime {
    val arrival = parseLocalDateTime(flight.arrivalDateTime)
    return if (flight.dutyType == "FLIGHT") arrival.plusMinutes(30) else arrival
}

private fun List<RosterItem>.countRestDutyDays(start: LocalDateTime, end: LocalDateTime, dutyType: String): Int = count { item ->
    val duty = (item as? RosterItem.FlightDuty)?.flight ?: return@count false
    duty.dutyType == dutyType && parseLocalDateTime(duty.departureDateTime).isAfter(start.minusMinutes(1)) && parseLocalDateTime(duty.arrivalDateTime).isBefore(end.plusMinutes(1))
}

private fun List<RosterItem>.countLeaveDays(start: LocalDateTime, end: LocalDateTime): Int = count { item ->
    val leave = item as? RosterItem.LeaveDuty ?: return@count false
    val leaveDateTime = leave.date.atStartOfDay()
    leaveDateTime.isAfter(start.minusMinutes(1)) && leaveDateTime.isBefore(end.plusMinutes(1))
}

@Composable
private fun RestPeriodCard(rest: RosterItem.RestPeriod, ru: Boolean) {
    val minutes = ChronoUnit.MINUTES.between(rest.start, rest.end).toInt().coerceAtLeast(0)
    val details = buildList {
        if (rest.offDays > 0) add(if (ru) "OFF: ${rest.offDays}" else "OFF: ${rest.offDays}")
        if (rest.leaveDays > 0) add(if (ru) "отпуск: ${rest.leaveDays}" else "leave: ${rest.leaveDays}")
        if (rest.layoverDays > 0) add(if (ru) "layover: ${rest.layoverDays}" else "layover: ${rest.layoverDays}")
    }.joinToString(" • ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("☾", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(
                    if (ru) "Отдых перед следующей duty" else "Rest before next duty",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    formatRestMinutes(minutes) + " → " + rest.nextDutyLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (details.isNotBlank()) {
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                rest.end.format(DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.ENGLISH)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatRestMinutes(totalMinutes: Int): String {
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60
    return buildString {
        if (days > 0) append("${days}d ")
        append("${hours}h")
        if (minutes > 0) append(" ${minutes}m")
    }.trim()
}

@Composable
private fun MonthlyProgressCard(flights: List<FlightEntity>, month: YearMonth, selected90: Boolean, ru: Boolean) {
    val monthDate = month.atDay(1)
    val monthPrefix = monthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val monthLabel = monthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
    val monthFlights = flights.filter { (it.dutyType == "FLIGHT" || it.dutyType == "SIMULATOR") && it.departureDateTime.startsWith(monthPrefix) && LeaveDatabase.leaveFor(parseLocalDateTime(it.departureDateTime).toLocalDate()) == null }
    val planned = monthFlights.sumOf { it.durationMinutes }
    val completed = monthFlights.filter { it.isCompleted }.sumOf { it.durationMinutes }
    val adjustedTarget = LeaveDatabase.adjustedMonthlyTargetMinutes(month)
    val selectedTarget = if (selected90) 90 * 60 else 80 * 60
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
            Text("Adjusted target ${formatMinutes(adjustedTarget)} • Selected ${formatMinutes(selectedTarget)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Leave: ${LeaveDatabase.leaveDaysInMonth(month)} days", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val now = nowAtAirport("BKK")
    val today = now.toLocalDate()
    val leave = LeaveDatabase.leaveFor(today)
    val todayFlights = flights.filter { it.dutyType == "FLIGHT" && parseLocalDateTime(it.departureDateTime).toLocalDate() == today }.sortedBy { it.departureDateTime }
    val reserveDuty = flights.filter { it.dutyType == "RESERVE" }
        .firstOrNull { duty ->
            val start = parseLocalDateTime(duty.departureDateTime)
            val end = parseLocalDateTime(duty.arrivalDateTime)
            !now.isBefore(start) && now.isBefore(end)
        }
        ?: flights.filter { it.dutyType == "RESERVE" && parseLocalDateTime(it.departureDateTime).toLocalDate() == today }
            .filter { parseLocalDateTime(it.arrivalDateTime).isAfter(now) }
            .minByOrNull { it.departureDateTime }
    val currentOrNext = todayFlights.firstOrNull { parseLocalDateTime(it.arrivalDateTime).isAfter(now) }
    val previous = todayFlights.lastOrNull { parseLocalDateTime(it.arrivalDateTime).isBefore(now) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (ru) "Сегодня" else "Today’s Duty", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            when {
                leave != null -> {
                    Text(leave.title, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${formatDate(leave.start)} — ${formatDate(leave.end)} • Day ${java.time.temporal.ChronoUnit.DAYS.between(leave.start, today).toInt() + 1} of ${leave.days}", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f))
                }
                reserveDuty != null -> {
                    val start = parseLocalDateTime(reserveDuty.departureDateTime)
                    val end = parseLocalDateTime(reserveDuty.arrivalDateTime)
                    val active = !now.isBefore(start) && now.isBefore(end)
                    Column(Modifier.clickable { onDutyClick(reserveDuty.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (active) "Reserve active" else "Reserve today", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${displayTime(reserveDuty.departureDateTime)}-${displayTime(reserveDuty.arrivalDateTime)} • ${reserveDuty.departureAirport}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(reserveDuty.dutyNote.ifBlank { "Standby duty" }, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f))
                    }
                }
                currentOrNext == null -> {
                    Text(if (ru) "Сегодня duty нет" else "No duty today", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (ru) "Следующая duty появится после 00:00 в дату ростера." else "Next duty will appear after 00:00 on its roster date.", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f))
                }
                now.isBefore(parseLocalDateTime(currentOrNext.departureDateTime)) && previous != null -> {
                    Column(Modifier.clickable { onDutyClick(currentOrNext.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Turnaround", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Next sector ${currentOrNext.flightNumber} ${currentOrNext.departureIata}-${currentOrNext.arrivalIata}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Departure ${displayTime(currentOrNext.departureDateTime)}", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f))
                    }
                }
                now.isBefore(parseLocalDateTime(currentOrNext.departureDateTime)) -> {
                    Column(Modifier.clickable { onDutyClick(currentOrNext.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Pre-flight", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${currentOrNext.flightNumber} / ${currentOrNext.departureIata}-${currentOrNext.arrivalIata}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Report ${reportDateTime(currentOrNext.departureDateTime, currentOrNext.durationMinutes).format(DateTimeFormatter.ofPattern("HH:mm"))} • Departure ${displayTime(currentOrNext.departureDateTime)}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                now.isBefore(parseLocalDateTime(currentOrNext.arrivalDateTime)) -> {
                    Column(Modifier.clickable { onDutyClick(currentOrNext.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("In flight", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${currentOrNext.flightNumber} ${currentOrNext.departureIata}-${currentOrNext.arrivalIata}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Estimated arrival ${displayTime(currentOrNext.arrivalDateTime)}", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f))
                    }
                }
            }
        }
    }
}

@Composable
fun FlightCard(flight: FlightEntity, onClick: () -> Unit, flightRepository: FlightRepository, ru: Boolean) {
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
                    modifier = Modifier.padding(start = 10.dp).background(ThaiPurple.copy(alpha = 0.92f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.weight(1f))
                Text(if (flight.isCompleted) { if (ru) "ВЫПОЛНЕНО" else "COMPLETED" } else if (flight.isRegistered) { if (ru) "ЗАРЕГИСТРИРОВАН" else "REGISTERED" } else { if (ru) "ПЛАН" else flight.status }, color = SuccessGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(0.95f)) {
                    Text(flight.departureIata, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(
                        compactCityName(flight.departureCity),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = if (flight.departureCity == "Kuala Lumpur") 12.sp else 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        airportShortName(flight.departureIata, flight.departureAirport),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.1f)) {
                    Text(scheduleTimeLine(flight), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    Text("${displayDate(flight.departureDateTime)} • ${displayDay(flight.departureDateTime)}", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
                    Text("✈", color = ThaiPurple, style = MaterialTheme.typography.headlineMedium)
                    Text(formatMinutes(flight.durationMinutes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(0.95f), horizontalAlignment = Alignment.End) {
                    Text(flight.arrivalIata, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(
                        compactCityName(flight.arrivalCity),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = if (flight.arrivalCity == "Kuala Lumpur") 12.sp else 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        airportShortName(flight.arrivalIata, flight.arrivalAirport),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
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
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White)
                ) { Text(if (ru) "Зарегистрирован" else "Registered") }
            } else if (shouldShowRegistrationButton(flight.departureIata, flight.durationMinutes) && canRegister(flight.departureDateTime, flight.isCompleted, flight.departureIata)) {
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
        isStay -> "Stay in ${flight.departureCity}"
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
            if (!isOff) Text("Location: ${if (isStay) flight.departureAirport else airportShortName(flight.departureIata, flight.departureAirport)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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


private fun compactCityName(name: String): String = when (name) {
    "Kuala Lumpur" -> "Kuala Lumpur"
    else -> name
}

private fun airportShortName(iata: String, name: String): String = com.example.crewportal.data.airport.AirportDatabase.shortAirportName(iata, name)

private fun scheduleTimeLine(flight: FlightEntity): String = displayTime(flight.departureDateTime)

private fun airportAssignmentLine(flight: FlightEntity): String {
    if (!shouldShowAirportAssignment(flight)) return ""
    val now = nowAtAirport(flight.departureIata)
    val due = !now.isBefore(parseLocalDateTime(flight.departureDateTime).minusHours(3))
    return when {
        flight.gate != "Pending" && flight.gate != "—" -> "Gate: ${flight.gate} • Terminal: ${flight.terminal}"
        flight.stand != "Pending" && flight.stand != "—" -> "Stand: ${flight.stand} • Terminal: ${flight.terminal}"
        due -> "Gate / Stand: synchronizing"
        else -> "Gate / Stand: assigned 3h prior"
    }
}

private fun shouldShowAirportAssignment(flight: FlightEntity): Boolean {
    return flight.departureIata == "BKK" || flight.durationMinutes >= 360
}

private fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)).uppercase(Locale.ENGLISH)

@Composable
private fun OperationalRosterChangeDialog(
    ru: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        date: LocalDate,
        reportTime: String,
        outboundFlight: String,
        destination: String,
        aircraft: String,
        registration: String?,
        pattern: String,
        returnFlight: String,
        returnDate: LocalDate?,
        returnTime: String?,
        replaceExisting: Boolean
    ) -> Unit
) {
    var dateText by remember { mutableStateOf(LocalDate.now().plusDays(2).toString()) }
    var reportTime by remember { mutableStateOf("10:00") }
    var outboundFlight by remember { mutableStateOf("TG999") }
    var destination by remember { mutableStateOf("DPS") }
    var aircraft by remember { mutableStateOf("A321neo") }
    var registration by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf<String?>(null) }
    var returnFlight by remember { mutableStateOf("TG998") }
    var returnDateText by remember { mutableStateOf(LocalDate.now().plusDays(3).toString()) }
    var returnTime by remember { mutableStateOf("12:00") }
    var replaceExisting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ru) "Operational roster change" else "Operational roster change") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(if (ru) "1. Рейс туда" else "1. Outbound flight", fontWeight = FontWeight.Bold)
                TextField(value = dateText, onValueChange = { dateText = it }, label = { Text("Date YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
                TextField(value = reportTime, onValueChange = { reportTime = it }, label = { Text("Report time HH:mm") }, modifier = Modifier.fillMaxWidth())
                TextField(value = outboundFlight, onValueChange = { outboundFlight = it.uppercase(Locale.ENGLISH) }, label = { Text("Outbound flight") }, modifier = Modifier.fillMaxWidth())
                TextField(value = destination, onValueChange = { destination = it.uppercase(Locale.ENGLISH).take(3) }, label = { Text("Destination IATA: DPS, SIN, KUL, TAS...") }, modifier = Modifier.fillMaxWidth())
                TextField(value = aircraft, onValueChange = { aircraft = it }, label = { Text("Aircraft type: A320 / A321neo / A330 / A350") }, modifier = Modifier.fillMaxWidth())
                TextField(value = registration, onValueChange = { registration = it.uppercase(Locale.ENGLISH) }, label = { Text("Registration optional, empty = assigned 24h prior") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = replaceExisting, onCheckedChange = { replaceExisting = it })
                    Text(if (ru) "Заменить существующие duty в эти даты" else "Replace existing duties on affected dates")
                }
                Text(if (ru) "2. Тип duty" else "2. Duty pattern", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { pattern = "TURNAROUND" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (pattern == "TURNAROUND") Color(0xFF1F3A5F) else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (pattern == "TURNAROUND") Color.White else MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) { Text("Turnaround") }
                    Button(
                        onClick = { pattern = "LAYOVER" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (pattern == "LAYOVER") Color(0xFF1F3A5F) else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (pattern == "LAYOVER") Color.White else MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) { Text("Layover") }
                }
                if (pattern != null) {
                    Text(if (ru) "3. Рейс обратно" else "3. Return flight", fontWeight = FontWeight.Bold)
                    TextField(value = returnFlight, onValueChange = { returnFlight = it.uppercase(Locale.ENGLISH) }, label = { Text("Return flight") }, modifier = Modifier.fillMaxWidth())
                    if (pattern == "LAYOVER") {
                        TextField(value = returnDateText, onValueChange = { returnDateText = it }, label = { Text("Return date YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
                        TextField(value = returnTime, onValueChange = { returnTime = it }, label = { Text("Return departure HH:mm") }, modifier = Modifier.fillMaxWidth())
                        Text(if (ru) "Отель/STAY будет подобран автоматически по destination." else "Layover hotel/STAY will be selected automatically by destination.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(if (ru) "Оборот будет рассчитан автоматически около 1–1.5h." else "Turnaround time will be calculated automatically around 1–1.5h.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val parsedDate = LocalDate.parse(dateText)
                        val selectedPattern = pattern ?: throw IllegalArgumentException("Select turnaround or layover")
                        val parsedReturn = if (selectedPattern == "LAYOVER") LocalDate.parse(returnDateText) else parsedDate
                        onSubmit(
                            parsedDate,
                            reportTime,
                            outboundFlight,
                            destination,
                            aircraft,
                            registration.takeIf { it.isNotBlank() },
                            selectedPattern,
                            returnFlight,
                            parsedReturn,
                            if (selectedPattern == "LAYOVER") returnTime else null,
                            replaceExisting
                        )
                    } catch (_: Exception) {
                        error = if (ru) "Проверьте дату, время и тип duty" else "Check date, time and duty pattern"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F3A5F), contentColor = Color.White)
            ) { Text(if (ru) "Отправить" else "Submit") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(if (ru) "Отмена" else "Cancel") }
        }
    )
}
