package com.example.crewportal.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.R
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.ui.theme.SuccessGreen
import com.example.crewportal.ui.theme.TextMuted
import com.example.crewportal.ui.theme.ThaiPurple
import com.example.crewportal.util.formatTotalMinutes

@Composable
fun ProfileScreen(preferencesRepository: PreferencesRepository) {
    val totalMinutes by preferencesRepository.totalMinutes.collectAsState(initial = 240000)
    val picMinutes by preferencesRepository.picMinutes.collectAsState(initial = 90000)
    val a320Minutes by preferencesRepository.a320Minutes.collectAsState(initial = 180000)
    val a330Minutes by preferencesRepository.a330Minutes.collectAsState(initial = 36000)
    val a350Minutes by preferencesRepository.a350Minutes.collectAsState(initial = 24000)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.profile_photo),
                    contentDescription = "Profile photo",
                    modifier = Modifier.size(96.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.padding(start = 16.dp)) {
                    Text("Dmitrii Alekseev", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Captain", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Thai Airways", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Home Base: BKK", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        InfoCard("Personal Card") {
            ProfileRow("Date of Birth", "14 July 1998")
            ProfileRow("Medical Certificate", "Class 1")
            ProfileRow("Total Flight Time", formatTotalMinutes(totalMinutes))
            ProfileRow("PIC Time", formatTotalMinutes(picMinutes))
        }

        InfoCard("Pilot Licenses") {
            QualificationRow("PPL(A)", "Issued: 18 March 2023", "Private Pilot Licence record", "VALID")
            QualificationRow("CPL(A)", "Issued: 27 October 2023", "Commercial Pilot Licence record", "VALID")
            QualificationRow("ATPL(A)", "Issued: 14 September 2024", "Airline Transport Pilot Licence record", "VALID")
            QualificationRow("IR(A)", "Issued: 22 November 2023", "Instrument Rating", "VALID")
            QualificationRow("MEP(L)", "Issued: 10 December 2023", "Multi-engine piston land rating", "VALID")
            QualificationRow("MCC", "Completed: 18 January 2024", "Multi-Crew Cooperation course", "VALID")
            QualificationRow("UPRT", "Completed: 09 February 2024", "Upset Prevention and Recovery Training", "VALID")
            QualificationRow("ICAO English", "Level 5: March 2026", "Language proficiency record", "VALID")
            QualificationRow("Radio Telephony", "Issued: 04 April 2023", "Aeronautical radio operator privilege", "VALID")
        }

        InfoCard("Type Ratings") {
            QualificationRow("Airbus A320 Family", "Issued: 16 May 2022", "Time on type: ${formatTotalMinutes(a320Minutes)}", "VALID")
            QualificationRow("Airbus A330", "Issued: 11 February 2025", "Time on type: ${formatTotalMinutes(a330Minutes)}", "VALID")
            QualificationRow("Airbus A350", "Issued: 28 September 2025", "Time on type: ${formatTotalMinutes(a350Minutes)}", "VALID")
        }

        InfoCard("Documents & Qualifications") {
            QualificationRow("Class 1 Medical Certificate", "Completed: 10 February 2026", "Next: 10 August 2026", "VALID")
            QualificationRow("Safety & Emergency Procedures — Land", "Completed: 06 April 2026", "Next: 06 October 2026", "VALID")
            QualificationRow("Safety & Emergency Procedures — Water", "Completed: 08 April 2026", "Next: 08 October 2026", "VALID")
            QualificationRow("Simulator Recurrent Session", "Completed: 18 January 2026", "Next: 18 July 2026", "VALID")
            QualificationRow("Line Check", "Completed: 06 May 2026", "Next: 06 August 2026", "VALID")
        }

        InfoCard("Leave & Availability") {
            ProfileRow("Annual leave", "50 / 80 days assigned")
            ProfileRow("Personal leave", "0 / 40 days used")
            ProfileRow("Sick leave", "No records")
            Text("Open Leave Management from More to request personal leave or manage sick leave.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        InfoCard("Expiry Dashboard") {
            ProfileRow("Medical certificate", "Due Aug 2026")
            ProfileRow("Line check", "Due Aug 2026")
            ProfileRow("Simulator recurrent", "Due Jul 2026")
            ProfileRow("Emergency procedures", "Due Oct 2026")
            Spacer(Modifier.height(4.dp))
            Text("All qualifications are valid. Reminder threshold: 60 days before expiry.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun ProfileRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QualificationRow(title: String, last: String, next: String, status: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(status, color = SuccessGreen, fontWeight = FontWeight.Bold)
        }
        Text(last, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(next, color = MaterialTheme.colorScheme.primary)
    }
}
