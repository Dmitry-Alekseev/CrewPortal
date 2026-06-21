package com.example.crewportal.ui.leave

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.leave.LeaveDatabase
import com.example.crewportal.data.leave.LeavePeriod
import com.example.crewportal.ui.theme.SuccessGreen
import com.example.crewportal.ui.theme.ThaiPurple
import com.example.crewportal.util.formatMinutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LeaveManagementScreen() {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var month by remember { mutableStateOf(YearMonth.now()) }
    val days = remember(month) { (1..month.lengthOfMonth()).map { month.atDay(it) } }
    val balance = LeaveDatabase.balance()
    var selectedStart by remember { mutableStateOf<LocalDate?>(null) }
    var selectedEnd by remember { mutableStateOf<LocalDate?>(null) }
    var sickOpen by remember { mutableStateOf(false) }
    var sickStart by remember { mutableStateOf<LocalDate?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SnackbarHost(hostState = snackbar) }
        item {
            Text("Leave Management", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Annual leave, personal leave and sick leave status", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            InfoCard("Leave Balance") {
                BalanceRow("Company assigned annual leave", "${balance.annualUsed} / ${balance.annualTotal} days")
                BalanceRow("Personal leave", "${balance.personalUsed} / ${balance.personalTotal} days")
                BalanceRow("Sick leave records", "${balance.sickDays} days")
                BalanceRow("Adjusted target this month", formatMinutes(LeaveDatabase.adjustedMonthlyTargetMinutes(month)))
            }
        }

        item {
            InfoCard("Assigned Leave") {
                LeaveDatabase.assignedLeave.forEach { LeaveRow(it) }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month") }
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next month") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LegendChip("Company", Color(0xFFFFD54F).copy(alpha = 0.55f))
                LegendChip("Personal", Color(0xFF4FC3F7).copy(alpha = 0.45f))
                LegendChip("Sick", Color(0xFFEF5350).copy(alpha = 0.35f))
            }
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                userScrollEnabled = false,
                modifier = Modifier.fillMaxWidth().height(330.dp)
            ) {
                items(days) { date ->
                    val leave = LeaveDatabase.leaveFor(date)
                    val selected = when {
                        selectedStart != null && selectedEnd != null -> !date.isBefore(selectedStart) && !date.isAfter(selectedEnd)
                        selectedStart != null -> date == selectedStart
                        else -> false
                    }
                    val color = when {
                        selected -> ThaiPurple.copy(alpha = 0.35f)
                        leave?.type == "ANNUAL_LEAVE" -> Color(0xFFFFD54F).copy(alpha = 0.55f)
                        leave?.type == "PERSONAL_LEAVE" -> Color(0xFF4FC3F7).copy(alpha = 0.45f)
                        leave?.type == "SICK_LEAVE" -> Color(0xFFEF5350).copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }
                    Box(
                        modifier = Modifier
                            .padding(3.dp)
                            .background(color, RoundedCornerShape(10.dp))
                            .clickable {
                                if (selectedStart == null || selectedEnd != null) {
                                    selectedStart = date
                                    selectedEnd = null
                                } else {
                                    val start = selectedStart ?: date
                                    if (date.isBefore(start)) {
                                        selectedStart = date
                                        selectedEnd = start
                                    } else {
                                        selectedEnd = date
                                    }
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(date.dayOfMonth.toString(), fontWeight = if (leave != null || selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        item {
            InfoCard("Personal Leave Request") {
                Text("Select at least 2 days. Requests must be submitted 7 days before the start date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Selected: ${selectedText(selectedStart, selectedEnd)}", fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = {
                        val start = selectedStart
                        val end = selectedEnd
                        if (start == null || end == null || end.isBefore(start.plusDays(1))) {
                            scope.launch { snackbar.showSnackbar("Select at least 2 days") }
                        } else if (start.isBefore(LocalDate.now().plusDays(7))) {
                            scope.launch { snackbar.showSnackbar("Leave must be requested at least 7 days before start") }
                        } else {
                            scope.launch {
                                snackbar.showSnackbar("Leave request submitted")
                                delay(1200)
                                LeaveDatabase.addPersonalLeave(
                                    LeavePeriod(
                                        id = "personal-${start}-${end}",
                                        type = "PERSONAL_LEAVE",
                                        title = "Personal Leave",
                                        start = start,
                                        end = end,
                                        status = "APPROVED",
                                        note = "Approved by crew planning"
                                    )
                                )
                                snackbar.showSnackbar("Leave request approved")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) { Text("Submit request") }
                LeaveDatabase.approvedPersonalLeave.forEach { LeaveRow(it) }
            }
        }
        item {
            InfoCard("Sick Leave") {
                Text(
                    if (sickOpen) "Sick leave currently open from ${formatDate(sickStart ?: LocalDate.now())}" else "No active sick leave",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            sickOpen = true
                            sickStart = LocalDate.now()
                            scope.launch { snackbar.showSnackbar("Sick leave opened") }
                        },
                        enabled = !sickOpen,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text("Open") }
                    OutlinedButton(
                        onClick = {
                            val start = sickStart ?: LocalDate.now()
                            val daysCount = java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.now()).toInt() + 1
                            sickOpen = false
                            sickStart = null
                            scope.launch { snackbar.showSnackbar("Sick leave closed: $daysCount day(s)") }
                        },
                        enabled = sickOpen,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) { Text("Close") }
                }
            }
        }




    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun BalanceRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LeaveRow(period: LeavePeriod) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(period.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(period.status, color = SuccessGreen, fontWeight = FontWeight.Bold)
        }
        Text("${formatDate(period.start)} — ${formatDate(period.end)} • ${period.days} days", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (period.note.isNotBlank()) Text(period.note, color = MaterialTheme.colorScheme.primary)
    }
}

private fun selectedText(start: LocalDate?, end: LocalDate?): String = when {
    start == null -> "No dates selected"
    end == null -> "${formatDate(start)} — select end date"
    else -> "${formatDate(start)} — ${formatDate(end)}"
}

private fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)).uppercase(Locale.ENGLISH)


@Composable
private fun LegendChip(label: String, color: Color) {
    Box(Modifier.background(color, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
