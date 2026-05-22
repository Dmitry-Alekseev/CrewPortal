package com.example.crewportal.ui.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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

@Composable
fun CompanyContactsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Company Contacts",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Internal operational contacts — Thailand numbers",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        InfoCard("Operations Control Centre") {
            ContactRow("OCC Duty Manager", "+66 2 356 9041")
            ContactRow("Crew Control", "+66 2 356 9042")
            ContactRow("Dispatch Desk", "+66 2 356 9043")
        }

        InfoCard("Flight Planning") {
            ContactRow("Flight Planning Office", "+66 2 356 9120")
            ContactRow("Operational Planning", "+66 2 356 9121")
            ContactRow("ATC Flight Plan Support", "+66 2 356 9122")
        }

        InfoCard("Fuel / Ground Operations") {
            ContactRow("BKK Fuel Coordination", "+66 2 132 6410")
            ContactRow("BKK Ramp Supervisor", "+66 2 132 6420")
            ContactRow("Turnaround Coordinator", "+66 2 132 6421")
        }

        InfoCard("BKK ATC Reference") {
            ContactRow("Bangkok Delivery", "121.95 MHz")
            ContactRow("Bangkok Ground", "121.70 MHz")
            ContactRow("Bangkok Tower", "118.10 MHz")
            ContactRow("Bangkok Departure", "119.70 MHz")
        }

        InfoCard("Station Contacts") {
            ContactRow("Suvarnabhumi Crew Hotel Desk", "+66 2 132 6701")
            ContactRow("BKK Crew Transport", "+66 2 132 6702")
            ContactRow("BKK Station Manager", "+66 2 132 6703")
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ContactRow(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, fontWeight = FontWeight.SemiBold)
    }
}
