package com.example.crewportal.ui.mel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.mel.MelDatabase
import com.example.crewportal.data.mel.MelDefect
import com.example.crewportal.ui.theme.SuccessGreen

@Composable
fun MelScreen(registration: String) {
    val defects = MelDatabase.forAircraft(registration)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("MEL / Deferred Defects", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(registration, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (defects.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No active MEL items", fontWeight = FontWeight.Bold, color = SuccessGreen)
                    Text("No deferred defects are currently recorded for this aircraft in the local company database.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            defects.forEach { MelCard(it) }
        }
        Text("Simulation data based on public MMEL/MEL structure examples. Not for operational use.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MelCard(item: MelDefect) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(item.defectId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("CAT ${item.category}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text("MEL ${item.melItem} • ${item.ata}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(item.description, fontWeight = FontWeight.SemiBold)
            Detail("Status", item.status)
            Detail("Reported", item.reportedDate)
            Detail("Limitation", item.operationalLimitation)
            Detail("Rectification due", item.rectificationDue)
            Detail("Planned action", item.plannedAction)
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value)
    }
}
