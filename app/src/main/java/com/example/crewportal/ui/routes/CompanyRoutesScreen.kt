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

private data class CompanyRoute(val route: String, val code: String, val aircraft: String, val type: String)

private val routes = listOf(
    CompanyRoute("Bangkok — Phuket", "BKK-HKT", "A320 Family", "Domestic"),
    CompanyRoute("Bangkok — Chiang Mai", "BKK-CNX", "A320 Family", "Domestic"),
    CompanyRoute("Bangkok — Krabi", "BKK-KBV", "A320 Family", "Domestic"),
    CompanyRoute("Bangkok — Nha Trang", "BKK-CXR", "A321neo", "Regional"),
    CompanyRoute("Bangkok — Singapore", "BKK-SIN", "A321neo / A350", "Regional"),
    CompanyRoute("Bangkok — Kuala Lumpur", "BKK-KUL", "A321neo", "Regional"),
    CompanyRoute("Bangkok — Ho Chi Minh City", "BKK-SGN", "A321neo", "Regional"),
    CompanyRoute("Bangkok — Hanoi", "BKK-HAN", "A321neo", "Regional"),
    CompanyRoute("Bangkok — Hong Kong", "BKK-HKG", "A330 / A350", "Regional"),
    CompanyRoute("Bangkok — Taipei", "BKK-TPE", "A330 / A350", "Regional"),
    CompanyRoute("Bangkok — Tokyo Narita", "BKK-NRT", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Tokyo Haneda", "BKK-HND", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Osaka", "BKK-KIX", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Seoul", "BKK-ICN", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Delhi", "BKK-DEL", "A330 / A350", "Medium"),
    CompanyRoute("Bangkok — Mumbai", "BKK-BOM", "A330 / A350", "Medium"),
    CompanyRoute("Bangkok — Dubai", "BKK-DXB", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Istanbul", "BKK-IST", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Frankfurt", "BKK-FRA", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Munich", "BKK-MUC", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Zurich", "BKK-ZRH", "A350", "Long-haul"),
    CompanyRoute("Bangkok — London", "BKK-LHR", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Paris", "BKK-CDG", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Sydney", "BKK-SYD", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Melbourne", "BKK-MEL", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Perth", "BKK-PER", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Auckland", "BKK-AKL", "A350", "Long-haul"),
    CompanyRoute("Bangkok — Tashkent", "BKK-TAS", "A330", "Simulated"),
    CompanyRoute("Bangkok — Saint Petersburg", "BKK-LED", "A350", "Simulated"),
    CompanyRoute("Bangkok — Moscow Sheremetyevo", "BKK-SVO", "A350", "Simulated"),
    CompanyRoute("Bangkok — Novosibirsk", "BKK-OVB", "A330 / A350", "Simulated"),
    CompanyRoute("Bangkok — Yekaterinburg", "BKK-SVX", "A330 / A350", "Simulated"),
    CompanyRoute("Bangkok — Ulan-Ude", "BKK-UUD", "A330", "Simulated"),
    CompanyRoute("Bangkok — Vladivostok", "BKK-VVO", "A330", "Simulated"),
    CompanyRoute("Bangkok — Irkutsk", "BKK-IKT", "A330", "Simulated"),
    CompanyRoute("Bangkok — Khabarovsk", "BKK-KHV", "A330", "Simulated")
)

@Composable
fun CompanyRoutesScreen(ru: Boolean) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(if (ru) "Маршрутная сеть" else "Company Routes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(if (ru) "Основные и симуляционные маршруты экипажного портала" else "Main and simulated routes available in Crew Portal", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(routes) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.route, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(item.code, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("Aircraft: ${item.aircraft}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Operation: ${item.type}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
