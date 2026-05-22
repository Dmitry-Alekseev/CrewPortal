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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.util.displayDay
import com.example.crewportal.util.displayMonth
import com.example.crewportal.util.displayShortDate
import com.example.crewportal.util.displayTime
import com.example.crewportal.util.formatMinutes
import com.example.crewportal.util.parseLocalDateTime
import java.time.LocalDate

@Composable
fun CalendarScreen(flightRepository: FlightRepository) {
    val duties by flightRepository.observeFlights().collectAsState(initial = emptyList())
    var showNextMonth by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val targetMonth = if (showNextMonth) today.plusMonths(1) else today
    val canPreviewNext = today.dayOfMonth >= today.lengthOfMonth() - 6
    val filtered = duties.filter {
        val date = parseLocalDateTime(it.departureDateTime).toLocalDate()
        date.year == targetMonth.year && date.month == targetMonth.month
    }
    val grouped = filtered.groupBy { parseLocalDateTime(it.departureDateTime).toLocalDate() }.toSortedMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Roster Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Roster month: ${displayMonth(targetMonth)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (canPreviewNext || showNextMonth) {
                OutlinedButton(onClick = { showNextMonth = !showNextMonth }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(if (showNextMonth) "Back to current month" else "Show next month roster")
                }
            }
        }
        items(grouped.entries.toList()) { entry -> CalendarDayCard(entry.key, entry.value) }
    }
}

@Composable
private fun CalendarDayCard(date: LocalDate, duties: List<FlightEntity>) {
    val isPast = date.isBefore(LocalDate.now())
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(displayShortDate(duties.first().departureDateTime), fontWeight = FontWeight.Bold, color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    Text(displayDay(duties.first().departureDateTime), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val block = duties.filter { it.dutyType == "FLIGHT" }.sumOf { it.durationMinutes }
                if (block > 0) Text("Block ${formatMinutes(block)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            duties.forEach { duty ->
                val label = if (duty.dutyType == "FLIGHT") "${duty.flightNumber} ${duty.departureIata}-${duty.arrivalIata}" else if (duty.dutyType == "OFF") "OFF" else duty.dutyType
                val chipColor = when (duty.dutyType) {
                    "FLIGHT" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    "RESERVE" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                    "TRAINING" -> Color(0xFFFFB74D).copy(alpha = 0.26f)
                    "OFF" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.62f)
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
