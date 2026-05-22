package com.example.crewportal.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.ColumnScope
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.profile_photo),
                    contentDescription = "Profile photo",
                    modifier = Modifier.size(96.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.padding(start = 16.dp)) {
                    Text("Dmitrii Alekseev", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Captain", color = TextMuted)
                    Text("Thai Airways", color = TextMuted)
                    Text("Home Base: BKK", color = TextMuted)
                }
            }
        }

        InfoCard("Personal Card") {
            ProfileRow("Date of Birth", "14 July 1998")
            ProfileRow("Total Flight Time", formatTotalMinutes(totalMinutes))
            ProfileRow("PIC Time", formatTotalMinutes(picMinutes))
        }

        InfoCard("Type Ratings") {
            Text("• Airbus A320 Family")
            Text("• Airbus A330")
            Text("• Airbus A350")
        }

        InfoCard("Documents & Qualifications") {
            QualificationRow("Medical / VLEK", "Last: February 2026", "Next: August 2026", "VALID")
            QualificationRow("ASP Land", "Completed: 12 February 2026", "Next: 12 August 2026", "VALID")
            QualificationRow("ASP Water", "Completed: 14 February 2026", "Next: 14 August 2026", "VALID")
            QualificationRow("Simulator Session", "Completed: 18 February 2026", "Next: 18 August 2026", "VALID")
        }

        InfoCard("Expiry Dashboard") {
            ProfileRow("Medical certificate", "Due Aug 2026")
            ProfileRow("Simulator check", "Due Aug 2026")
            ProfileRow("Safety procedures", "Due Aug 2026")
            Spacer(Modifier.height(4.dp))
            Text("All qualifications are valid. Reminder threshold: 60 days before expiry.", color = TextMuted)
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = TextMuted, modifier = Modifier.weight(1f))
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
        Text(last, color = TextMuted)
        Text(next, color = ThaiPurple)
    }
}
