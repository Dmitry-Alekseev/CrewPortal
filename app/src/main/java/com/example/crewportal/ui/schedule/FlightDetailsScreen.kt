package com.example.crewportal.ui.schedule

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import kotlin.math.max
import kotlin.math.min

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
    val bounds = remember(flight.departureIata, flight.arrivalIata) {
        routeMapBounds(departurePoint, arrivalPoint)
    }

    InfoCard("Route Map") {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            drawNativeRouteMap(
                departureIata = flight.departureIata,
                arrivalIata = flight.arrivalIata,
                departurePoint = departurePoint,
                arrivalPoint = arrivalPoint,
                bounds = bounds
            )
        }

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

private data class MapPoint(
    val latitude: Float,
    val longitude: Float
)

private data class MapBounds(
    val minLat: Float,
    val maxLat: Float,
    val minLon: Float,
    val maxLon: Float
)

private fun routeMapBounds(departure: MapPoint, arrival: MapPoint): MapBounds {
    val rawMinLat = min(departure.latitude, arrival.latitude)
    val rawMaxLat = max(departure.latitude, arrival.latitude)
    val rawMinLon = min(departure.longitude, arrival.longitude)
    val rawMaxLon = max(departure.longitude, arrival.longitude)

    val latSpan = max(rawMaxLat - rawMinLat, 6f)
    val lonSpan = max(rawMaxLon - rawMinLon, 8f)
    val paddingLat = latSpan * 0.35f
    val paddingLon = lonSpan * 0.35f

    return MapBounds(
        minLat = rawMinLat - paddingLat,
        maxLat = rawMaxLat + paddingLat,
        minLon = rawMinLon - paddingLon,
        maxLon = rawMaxLon + paddingLon
    )
}

private fun DrawScope.project(point: MapPoint, bounds: MapBounds): Offset {
    val mapPadding = 24f
    val width = size.width - mapPadding * 2f
    val height = size.height - mapPadding * 2f
    val x = mapPadding + ((point.longitude - bounds.minLon) / (bounds.maxLon - bounds.minLon)) * width
    val y = mapPadding + ((bounds.maxLat - point.latitude) / (bounds.maxLat - bounds.minLat)) * height
    return Offset(x, y)
}

private fun DrawScope.drawNativeRouteMap(
    departureIata: String,
    arrivalIata: String,
    departurePoint: MapPoint,
    arrivalPoint: MapPoint,
    bounds: MapBounds
) {
    drawRect(Color(0xFFEAF4FF))

    repeat(5) { index ->
        val fraction = (index + 1) / 6f
        val x = size.width * fraction
        val y = size.height * fraction
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
    }

    landPolygons.forEach { polygon ->
        val visible = polygon.any { point ->
            point.latitude in bounds.minLat..bounds.maxLat && point.longitude in bounds.minLon..bounds.maxLon
        }
        if (visible) {
            val path = Path()
            polygon.forEachIndexed { index, point ->
                val projected = project(point, bounds)
                if (index == 0) path.moveTo(projected.x, projected.y) else path.lineTo(projected.x, projected.y)
            }
            path.close()
            drawPath(path = path, color = Color(0xFFD9E4C7))
            drawPath(path = path, color = Color(0xFFB8C7A1), style = Stroke(width = 2f))
        }
    }

    val dep = project(departurePoint, bounds)
    val arr = project(arrivalPoint, bounds)

    drawLine(
        color = ThaiPurple,
        start = dep,
        end = arr,
        strokeWidth = 6f
    )

    drawCircle(color = ThaiPurple, radius = 13f, center = dep)
    drawCircle(color = ThaiPurple, radius = 13f, center = arr)
    drawCircle(color = Color.White, radius = 6f, center = dep)
    drawCircle(color = Color.White, radius = 6f, center = arr)

    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.rgb(55, 42, 69)
        textSize = 31f
        fakeBoldText = true
    }
    val subPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.rgb(92, 82, 105)
        textSize = 23f
    }

    drawContext.canvas.nativeCanvas.drawText(departureIata, dep.x + 16f, dep.y - 18f, paint)
    drawContext.canvas.nativeCanvas.drawText(arrivalIata, arr.x + 16f, arr.y - 18f, paint)
    drawContext.canvas.nativeCanvas.drawText("OFFLINE ROUTE MAP", 18f, size.height - 18f, subPaint)
}

private val landPolygons = listOf(
    listOf(
        MapPoint(32f, 66f), MapPoint(35f, 78f), MapPoint(30f, 92f), MapPoint(22f, 96f),
        MapPoint(16f, 90f), MapPoint(7f, 80f), MapPoint(8f, 70f), MapPoint(20f, 62f)
    ),
    listOf(
        MapPoint(30f, 92f), MapPoint(29f, 106f), MapPoint(23f, 113f), MapPoint(15f, 110f),
        MapPoint(7f, 105f), MapPoint(1f, 101f), MapPoint(6f, 96f), MapPoint(16f, 96f)
    ),
    listOf(
        MapPoint(5f, 95f), MapPoint(2f, 107f), MapPoint(-7f, 114f), MapPoint(-9f, 103f),
        MapPoint(-4f, 95f)
    ),
    listOf(
        MapPoint(72f, -12f), MapPoint(70f, 42f), MapPoint(58f, 58f), MapPoint(45f, 45f),
        MapPoint(36f, 30f), MapPoint(38f, 8f), MapPoint(48f, -6f), MapPoint(60f, -15f)
    ),
    listOf(
        MapPoint(42f, 26f), MapPoint(38f, 45f), MapPoint(27f, 56f), MapPoint(17f, 52f),
        MapPoint(16f, 40f), MapPoint(24f, 32f), MapPoint(33f, 27f)
    ),
    listOf(
        MapPoint(-10f, 112f), MapPoint(-13f, 142f), MapPoint(-25f, 154f), MapPoint(-40f, 145f),
        MapPoint(-38f, 116f), MapPoint(-24f, 110f)
    ),
    listOf(
        MapPoint(46f, 128f), MapPoint(44f, 146f), MapPoint(34f, 146f), MapPoint(30f, 132f),
        MapPoint(36f, 126f)
    )
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
