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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.mel.MelDatabase
import com.example.crewportal.data.mel.MelDefect
import com.example.crewportal.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun MelScreen(registration: String) {
    var allDefects by remember { mutableStateOf(MelDatabase.all()) }
    var syncStatus by remember { mutableStateOf("Local MEL database loaded") }

    LaunchedEffect(registration) {
        val remote = withContext(Dispatchers.IO) { loadMelFromGitHub() }
        if (remote != null) {
            allDefects = remote
            syncStatus = "Company MEL database synchronized"
        }
    }

    val defects = allDefects.filter { it.aircraftRegistration.equals(registration, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("MEL / Deferred Defects", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(registration, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(syncStatus, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)

        if (defects.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No active MEL items", fontWeight = FontWeight.Bold, color = SuccessGreen)
                    Text("No deferred defects are currently recorded for this aircraft.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            defects.forEach { MelCard(it) }
        }
    }
}

private fun loadMelFromGitHub(): List<MelDefect>? {
    return try {
        val client = OkHttpClient()
        val response = client.newCall(Request.Builder().url(MelDatabase.githubMelUrl).build()).execute()
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful || body.isBlank()) null else MelDatabase.fromJson(body)
    } catch (_: Exception) {
        null
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
