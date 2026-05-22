package com.example.crewportal.ui.schedule

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.crewportal.data.airport.AirportDatabase
import com.example.crewportal.data.airport.AirportInfo
import com.example.crewportal.data.crew.CrewPool
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.ui.theme.SuccessGreen
import com.example.crewportal.ui.theme.TextMuted
import com.example.crewportal.ui.theme.ThaiPurple
import com.example.crewportal.util.alternateFor
import com.example.crewportal.util.briefingDistanceNm
import com.example.crewportal.util.canRegister
import com.example.crewportal.util.cruiseLevel
import com.example.crewportal.util.displayDate
import com.example.crewportal.util.displayTime
import com.example.crewportal.util.dutyEndDateTime
import com.example.crewportal.util.dutyMinutes
import com.example.crewportal.util.estimatedFuel
import com.example.crewportal.util.etopsText
import com.example.crewportal.util.formatMinutes
import com.example.crewportal.util.hasArrived
import com.example.crewportal.util.notamSummary
import com.example.crewportal.util.reportDateTime
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailsScreen(flightId: String, flightRepository: FlightRepository, onBack: () -> Unit) {
    val flight by flightRepository.observeFlight(flightId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { flightRepository.refreshCompletedFlights() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Flight Details") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } })
        }
    ) { padding ->
        val item = flight
        if (item == null) {
            Text("Flight not found", modifier = Modifier.padding(padding).padding(24.dp))
        } else {
            val longHaul = item.durationMinutes >= 360
            val crew = CrewPool.forFlight(item.id, longHaul)
            val fuel = estimatedFuel(item.durationMinutes, item.aircraftLabel)
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(item.flightNumber, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("${item.departureCity} → ${item.arrivalCity}", color = TextMuted)

                RouteMapCard(item)
                StatusTimelineCard(item)

                InfoCard("Flight Information") {
                    DetailRow("Departure", "${item.departureIata} / ${item.departureAirport}", "${displayDate(item.departureDateTime)}, ${displayTime(item.departureDateTime)} • ${AirportDatabase.utcText(item.departureDateTime, item.departureIata)}")
                    DetailRow("Arrival", "${item.arrivalIata} / ${item.arrivalAirport}", "${displayDate(item.arrivalDateTime)}, ${displayTime(item.arrivalDateTime)} • ${AirportDatabase.utcText(item.arrivalDateTime, item.arrivalIata)}")
                    DetailRow("Aircraft", item.aircraftFullName, item.aircraftLabel)
                    DetailRow("Registration", if (item.registration == "TBA") "Assigned 24h prior" else item.registration, "Released with crew registration window")
                    DetailRow("Block Time", formatMinutes(item.durationMinutes), "Local/UTC toggle data available")
                    DetailRow("Status", if (item.isCompleted) "Completed" else if (item.isRegistered) "Registered" else "Scheduled", "Company portal synchronized")
                }

                InfoCard("Airport Assignment") {
                    DetailRow("Gate", if (item.gate == "Pending") "Pending" else item.gate, "Assigned about 3 hours before departure")
                    DetailRow("Stand", if (item.stand == "Pending") "Pending" else item.stand, "Used when no gate is assigned")
                    DetailRow("Terminal", if (item.terminal == "Pending") "Pending" else item.terminal, "Airport operations synchronized")
                    DetailRow("Updated", if (item.gate != "Pending" || item.stand != "Pending") "Available" else "Pending", "Company portal / airport data sync")
                }

                InfoCard("Airport Database") {
                    AirportDatabase.byIata(item.departureIata)?.let { airport: AirportInfo -> DetailRow("Departure Airport", "${airport.iata} / ${airport.icao}", "${airport.city}, ${airport.country} • ${AirportDatabase.localOffsetText(airport.iata)}") }
                    AirportDatabase.byIata(item.arrivalIata)?.let { airport: AirportInfo -> DetailRow("Arrival Airport", "${airport.iata} / ${airport.icao}", "${airport.city}, ${airport.country} • ${AirportDatabase.localOffsetText(airport.iata)}") }
                }

                InfoCard("Duty Day") {
                    DetailRow("Report Time", reportDateTime(item.departureDateTime, item.durationMinutes).format(DateTimeFormatter.ofPattern("HH:mm")), "${if (longHaul) "International" else "Domestic"} report policy")
                    DetailRow("Duty End", dutyEndDateTime(item.arrivalDateTime).format(DateTimeFormatter.ofPattern("HH:mm")), "30 minutes after arrival")
                    DetailRow("Flight Duty Period", formatMinutes(dutyMinutes(item.departureDateTime, item.arrivalDateTime, item.durationMinutes)), "Calculated from report to duty end")
                    DetailRow("Rest Status", "OK", "Minimum rest requirement satisfied")
                }

                InfoCard("Flight Briefing") {
                    DetailRow("Route", "${item.departureIata} - ${item.arrivalIata}", "Distance approx. ${briefingDistanceNm(item.durationMinutes)} NM")
                    DetailRow("Alternate", alternateFor(item.arrivalIata), "Dispatch alternate placeholder")
                    DetailRow("Cruise Level", cruiseLevel(item.durationMinutes), "Estimated planning level")
                    DetailRow("ETOPS", etopsText(item.durationMinutes), if (longHaul) "Extended-range briefing required" else "Standard operation")
                    DetailRow("Remarks", if (longHaul) "Augmented crew operation" else "Standard crew operation", "Operational briefing data")
                }

                InfoCard("Estimated Fuel Briefing") {
                    DetailRow("Taxi Fuel", "${fuel.taxiKg} kg", "Estimated")
                    DetailRow("Trip Fuel", "${fuel.tripKg} kg", "Based on block time and type")
                    DetailRow("Contingency", "${fuel.contingencyKg} kg", "5% placeholder")
                    DetailRow("Alternate", "${fuel.alternateKg} kg", "Planning estimate")
                    DetailRow("Final Reserve", "${fuel.finalReserveKg} kg", "Planning estimate")
                    DetailRow("Extra", "${fuel.extraKg} kg", "Company discretionary placeholder")
                    DetailRow("Block Fuel", "${fuel.totalKg} kg", "Estimated briefing data only")
                }

                InfoCard("NOTAM Summary") {
                    DetailRow("Company Summary", notamSummary(item.arrivalIata), "Official NOTAM briefing required before departure")
                }

                ChecklistCard(longHaul = longHaul)

                InfoCard("Crew List") {
                    DetailRow("Captain", crew.captain, "Operating commander")
                    DetailRow("First Officer", crew.firstOfficer, "Operating pilot")
                    if (crew.reliefCaptain != null) DetailRow("Relief Captain", crew.reliefCaptain, "Augmented crew")
                    if (crew.reliefFirstOfficer != null) DetailRow("Relief First Officer", crew.reliefFirstOfficer, "Augmented crew")
                    DetailRow("Cabin Manager", crew.cabinManager, "Thai cabin crew database")
                    DetailRow("Cabin Crew", "${crew.cabinCrewCount} crew members", "Assigned by cabin crew roster module")
                }

                InfoCard("Layover / Turnaround") {
                    if (longHaul) {
                        DetailRow("Hotel", "Company Crew Hotel", "Transport by company shuttle")
                        DetailRow("Pickup", "TBA by local station", "Shown after station update")
                    } else {
                        DetailRow("Layover", "Not applicable", "Turnaround operation")
                    }
                }

                if (item.isRegistered) {
                    Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Registered") }
                    Text("Registration completed successfully", color = SuccessGreen)
                } else if (canRegister(item.departureDateTime, item.isCompleted)) {
                    OutlinedButton(onClick = { scope.launch { flightRepository.registerFlight(item.id) } }, modifier = Modifier.fillMaxWidth()) { Text("Register") }
                } else if (!item.isCompleted) {
                    Text("Registration and aircraft tail assignment open 24 hours before departure", color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun RouteMapCard(flight: FlightEntity) {
    val departurePoint = routeMapPoint(flight.departureIata) ?: MapPoint(13.69f, 100.75f)
    val arrivalPoint = routeMapPoint(flight.arrivalIata) ?: MapPoint(11.99f, 109.22f)
    val html = remember(flight.departureIata, flight.arrivalIata) {
        routeMapHtml(
            departureIata = flight.departureIata,
            arrivalIata = flight.arrivalIata,
            departurePoint = departurePoint,
            arrivalPoint = arrivalPoint
        )
    }

    InfoCard("Route Map") {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    loadDataWithBaseURL(
                        "https://www.openstreetmap.org/",
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(
                    "https://www.openstreetmap.org/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = flight.departureIata,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${briefingDistanceNm(flight.durationMinutes)} NM",
                color = TextMuted
            )

            Text(
                text = flight.arrivalIata,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun routeMapHtml(
    departureIata: String,
    arrivalIata: String,
    departurePoint: MapPoint,
    arrivalPoint: MapPoint
): String {
    val centerLat = (departurePoint.latitude + arrivalPoint.latitude) / 2f
    val centerLon = (departurePoint.longitude + arrivalPoint.longitude) / 2f

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    background: #f7f3fb;
                    font-family: Arial, sans-serif;
                }
                .airport-label {
                    background: #4b0082;
                    color: white;
                    border-radius: 14px;
                    padding: 4px 8px;
                    font-size: 12px;
                    font-weight: bold;
                    border: 2px solid white;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.25);
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                const dep = [${departurePoint.latitude}, ${departurePoint.longitude}];
                const arr = [${arrivalPoint.latitude}, ${arrivalPoint.longitude}];

                const map = L.map('map', {
                    zoomControl: false,
                    attributionControl: true,
                    dragging: true,
                    scrollWheelZoom: false,
                    doubleClickZoom: false,
                    boxZoom: false,
                    keyboard: false
                }).setView([${centerLat}, ${centerLon}], 5);

                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 12,
                    attribution: '&copy; OpenStreetMap'
                }).addTo(map);

                const route = L.polyline([dep, arr], {
                    color: '#4b0082',
                    weight: 4,
                    opacity: 0.95
                }).addTo(map);

                const depIcon = L.divIcon({
                    className: 'airport-label',
                    html: '${departureIata}',
                    iconSize: [52, 26],
                    iconAnchor: [26, 13]
                });

                const arrIcon = L.divIcon({
                    className: 'airport-label',
                    html: '${arrivalIata}',
                    iconSize: [52, 26],
                    iconAnchor: [26, 13]
                });

                L.marker(dep, { icon: depIcon }).addTo(map);
                L.marker(arr, { icon: arrIcon }).addTo(map);
                map.fitBounds(route.getBounds(), { padding: [35, 35] });
            </script>
        </body>
        </html>
    """.trimIndent()
}


private data class MapPoint(
    val latitude: Float,
    val longitude: Float
)

private fun routeMapPoint(iata: String): MapPoint? {
    return when (iata.uppercase()) {
        "BKK" -> MapPoint(13.69f, 100.75f)
        "HKT" -> MapPoint(8.11f, 98.31f)
        "CXR" -> MapPoint(11.99f, 109.22f)
        "SIN" -> MapPoint(1.36f, 103.99f)
        "HKG" -> MapPoint(22.31f, 113.92f)
        "IST" -> MapPoint(41.28f, 28.75f)
        "FRA" -> MapPoint(50.04f, 8.56f)
        "MEL" -> MapPoint(-37.67f, 144.84f)
        "CDG" -> MapPoint(49.01f, 2.55f)
        "NRT" -> MapPoint(35.77f, 140.39f)
        "KUL" -> MapPoint(2.75f, 101.71f)
        "DEL" -> MapPoint(28.56f, 77.10f)
        else -> null
    }
}


@Composable
private fun StatusTimelineCard(flight: FlightEntity) {
    val stages = listOf(
        "Roster Published" to true,
        "Aircraft Assigned" to (flight.registration != "TBA"),
        "Registration Open" to canRegister(flight.departureDateTime, flight.isCompleted),
        "Gate / Stand Assigned" to (flight.gate != "Pending" || flight.stand != "Pending"),
        "Registered" to flight.isRegistered,
        "Report Time" to false,
        "Arrived" to hasArrived(flight.arrivalDateTime),
        "Flight Time Added" to flight.isFlightTimeAdded
    )
    InfoCard("Flight Status Timeline") {
        stages.forEach { (label, active) ->
            Row(Modifier.fillMaxWidth()) {
                Text(if (active) "●" else "○", color = if (active) SuccessGreen else TextMuted, modifier = Modifier.padding(end = 10.dp))
                Text(label, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = if (active) SuccessGreen else TextMuted)
            }
        }
    }
}

@Composable
private fun ChecklistCard(longHaul: Boolean) {
    val base = listOf("Check roster", "Review aircraft assignment", "Check METAR / TAF", "Check NOTAM", "Review briefing package", "Confirm crew report time", "Complete registration")
    val extra = if (longHaul) listOf("Review ETOPS briefing", "Check alternates", "Review augmented crew rest plan") else emptyList()
    val items = base + extra
    val checked = remember { mutableStateListOf<Boolean>().apply { repeat(items.size) { add(false) } } }
    InfoCard("Pre-flight Checklist") {
        items.forEachIndexed { index, label ->
            Row(Modifier.fillMaxWidth()) {
                Checkbox(checked = checked[index], onCheckedChange = { checked[index] = it })
                Text(label, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, main: String, sub: String) {
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(label.uppercase(), color = TextMuted, style = MaterialTheme.typography.labelMedium)
            Text(main, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (sub.isNotBlank()) Text(sub, color = TextMuted)
        }
    }
}
