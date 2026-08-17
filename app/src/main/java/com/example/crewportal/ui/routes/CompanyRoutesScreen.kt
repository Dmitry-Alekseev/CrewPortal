package com.example.crewportal.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.route.RouteCatalog
import com.example.crewportal.util.formatMinutes

@Composable
fun CompanyRoutesScreen(ru: Boolean) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(if (ru) "Маршрутная сеть" else "Company Routes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (ru) "Единый справочник маршрутов Crew Portal" else "Shared Crew Portal route catalog",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(RouteCatalog.routes, key = { it.destinationIata }) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(item.code, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("Aircraft: ${item.aircraft}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Operation: ${item.operationType}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Block BKK-${item.destinationIata}: ${formatMinutes(item.outboundMinMinutes)}–${formatMinutes(item.outboundMaxMinutes)} • ${item.destinationIata}-BKK: ${formatMinutes(item.inboundMinMinutes)}–${formatMinutes(item.inboundMaxMinutes)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(if (item.autoGenerationEnabled) "Auto-generation: enabled" else "Auto-generation: manual only", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
