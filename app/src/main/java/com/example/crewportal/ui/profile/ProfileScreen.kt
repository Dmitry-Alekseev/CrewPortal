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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.R
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.ui.theme.TextMuted
import com.example.crewportal.util.formatTotalMinutes

@Composable
fun ProfileScreen(preferencesRepository: PreferencesRepository) {
    val totalMinutes by preferencesRepository.totalMinutes.collectAsState(initial = 240000)
    val picMinutes by preferencesRepository.picMinutes.collectAsState(initial = 90000)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.profile_photo),
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
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

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileRow("Date of Birth", "14 July 1998")
                ProfileRow("Total Flight Time", formatTotalMinutes(totalMinutes))
                ProfileRow("PIC Time", formatTotalMinutes(picMinutes))
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Type Ratings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("• Airbus A320 Family")
                Text("• Airbus A330")
                Text("• Airbus A350")
            }
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
