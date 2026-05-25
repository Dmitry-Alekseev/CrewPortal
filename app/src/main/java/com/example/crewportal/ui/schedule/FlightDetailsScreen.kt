package com.example.crewportal.ui.schedule

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.airport.AirportDatabase
import com.example.crewportal.data.airport.AirportInfo
import com.example.crewportal.data.crew.CrewPool
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.fleet.AircraftPool
import com.example.crewportal.data.mel.MelDatabase
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.ui.theme.SuccessGreen
import com.example.crewportal.ui.theme.ThaiPurple
import com.example.crewportal.ui.theme.TextMuted
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailsScreen(flightId: String, flightRepository: FlightRepository, onBack: () -> Unit, onMelClick: (String) -> Unit) {
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
                Text("${item.departureCity} → ${item.arrivalCity}", color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (item.dutyType != "FLIGHT") {
                    InfoCard(if (item.dutyType == "OFF") "Day Off" else "${item.dutyType} Details") {
                        DetailRow("Date", displayDate(item.departureDateTime), displayDaySafe(item.departureDateTime))
                        DetailRow("Time", "${displayTime(item.departureDateTime)}-${displayTime(item.arrivalDateTime)}", "Local time")
                        DetailRow("Location", if (item.dutyType == "RESERVE") item.departureAirport else "Not applicable", item.departureCity)
                        DetailRow("Note", item.dutyNote.ifBlank { if (item.dutyType == "OFF") "No assigned duty" else "Hotel reserve at airport crew hotel" }, "Company roster item")
                    }
                    return@Column
                }

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

                AircraftTechnicalStatusCard(item, onMelClick)

                if (shouldShowAirportAssignment(item)) {
                    InfoCard("Airport Assignment") {
                        DetailRow("Gate", if (item.gate == "Pending") "Pending" else item.gate, "Assigned about 3 hours before departure")
                        DetailRow("Stand", if (item.stand == "Pending") "Pending" else item.stand, "Used when no gate is assigned")
                        DetailRow("Terminal", if (item.terminal == "Pending") "Pending" else item.terminal, "Airport operations synchronized")
                        DetailRow("Updated", if (item.gate != "Pending" || item.stand != "Pending") "Available" else "Pending", "Company portal / airport data sync")
                    }
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

                InfoCard("Flight Briefing Package") {
                    DetailRow("Route", "${item.departureIata} - ${item.arrivalIata}", "Distance approx. ${briefingDistanceNm(item.durationMinutes)} NM")
                    DetailRow("Aircraft", item.aircraftLabel, if (item.registration == "TBA") "Registration pending" else item.registration)
                    DetailRow("OFP Status", "Available", "Company briefing package synchronized")
                    DetailRow("Weather Status", "Updated", "METAR / TAF available from Weather tab")
                    DetailRow("NOTAM Status", "Company briefing required", notamSummary(item.arrivalIata))
                    DetailRow("Alternate", alternateFor(item.arrivalIata), "Dispatch alternate placeholder")
                    DetailRow("Cruise Level", cruiseLevel(item.durationMinutes), "Estimated planning level")
                    DetailRow("ETOPS", etopsText(item.durationMinutes), if (longHaul) "Extended-range briefing required" else "Standard operation")
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

                CrewRestPlanCard(item, longHaul)
                DutyLimitMonitorCard(item)

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
                        DetailRow("Hotel", layoverHotelFor(item.arrivalIata), "Transport by company shuttle")
                        DetailRow("Pickup", "TBA by local station", "Shown after station update")
                    } else if (item.dutyType == "RESERVE") {
                        DetailRow("Hotel", "Hyatt Regency Bangkok Suvarnabhumi Airport", "Airport reserve accommodation")
                    } else {
                        DetailRow("Layover", "Not applicable", "Turnaround operation")
                    }
                }

                if (item.isRegistered) {
                    Button(onClick = {}, enabled = true, colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), modifier = Modifier.fillMaxWidth()) { Text("Registered") }
                    Text("Registration completed successfully", color = SuccessGreen)
                } else if (canRegister(item.departureDateTime, item.isCompleted)) {
                    OutlinedButton(onClick = { scope.launch { flightRepository.registerFlight(item.id) } }, modifier = Modifier.fillMaxWidth()) { Text("Register") }
                } else if (!item.isCompleted) {
                    Text("Registration and aircraft tail assignment open 24 hours before departure", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AircraftTechnicalStatusCard(flight: FlightEntity, onMelClick: (String) -> Unit) {
    val aircraft = if (flight.registration == "TBA") null else AircraftPool.byRegistration(flight.registration)
    val melCount = if (flight.registration == "TBA") 0 else MelDatabase.forAircraft(flight.registration).size
    InfoCard("Aircraft Technical Status") {
        DetailRow("Registration", if (flight.registration == "TBA") "Assigned 24h prior" else flight.registration, aircraft?.fullName ?: flight.aircraftFullName)
        DetailRow("Status", if (melCount > 0) "Serviceable with MEL" else "Serviceable", "Open MEL items: $melCount")
        DetailRow("Last maintenance", "BKK Line Maintenance", "Latest station technical status synchronized")
        DetailRow("Next planned check", if (flight.durationMinutes >= 360) "After long-haul rotation" else "Next BKK night stop", "Maintenance planning data")
        if (flight.registration != "TBA") {
            Button(onClick = { onMelClick(flight.registration) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open MEL")
            }
        }
    }
}

@Composable
private fun CrewRestPlanCard(flight: FlightEntity, longHaul: Boolean) {
    InfoCard("Crew Rest Plan") {
        if (!longHaul) {
            DetailRow("Crew rest", "Not required", "Standard short/medium sector")
        } else {
            DetailRow("Crew", "Augmented", "Captain, First Officer and relief crew")
            DetailRow("Rest Group A", "Captain + First Officer", "Planned rest: cruise middle segment")
            DetailRow("Rest Group B", "Relief crew", "Planned rest: early/late cruise segment")
            DetailRow("Cabin crew rest", "Assigned by cabin manager", "Long-haul crew rest plan active")
        }
    }
}

@Composable
private fun DutyLimitMonitorCard(flight: FlightEntity) {
    val fdp = dutyMinutes(flight.departureDateTime, flight.arrivalDateTime, flight.durationMinutes)
    val limit = if (flight.durationMinutes >= 360) 13 * 60 else 11 * 60
    InfoCard("Duty Limits / Fatigue Monitor") {
        DetailRow("FDP", formatMinutes(fdp), "Limit ${formatMinutes(limit)}")
        DetailRow("Status", if (fdp <= limit) "OK" else "Review required", if (fdp <= limit) "Within planned duty limit" else "Close to limit")
        DetailRow("Monthly monitor", "Active", "Flight time counters updated after completed sectors")
        DetailRow("Rest", "OK", "Next duty rest check passed")
    }
}

@Composable
private fun RouteMapCard(flight: FlightEntity) {
    val context = LocalContext.current
    val departurePoint = routeMapPoint(flight.departureIata) ?: MapPoint(13.69f, 100.75f)
    val arrivalPoint = routeMapPoint(flight.arrivalIata) ?: MapPoint(11.99f, 109.22f)
    val departureGeo = departurePoint.toGeoPoint()
    val arrivalGeo = arrivalPoint.toGeoPoint()
    val centerGeo = GeoPoint(
        (departurePoint.latitude + arrivalPoint.latitude) / 2.0,
        (departurePoint.longitude + arrivalPoint.longitude) / 2.0
    )

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    InfoCard("Route Map") {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            factory = { androidContext ->
                Configuration.getInstance().userAgentValue = androidContext.packageName
                MapView(androidContext).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    minZoomLevel = 2.0
                    maxZoomLevel = 12.0
                    controller.setZoom(routeMapZoom(flight.durationMinutes))
                    controller.setCenter(centerGeo)
                }
            },
            update = { mapView ->
                mapView.overlays.clear()

                val route = Polyline().apply {
                    setPoints(listOf(departureGeo, arrivalGeo))
                    color = android.graphics.Color.rgb(91, 0, 130)
                    width = 7f
                    title = "${flight.departureIata}-${flight.arrivalIata}"
                }

                val departureMarker = Marker(mapView).apply {
                    position = departureGeo
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = letterMarkerDrawable(mapView.context, "A")
                    title = "A • ${flight.departureIata}"
                    snippet = flight.departureCity
                }

                val arrivalMarker = Marker(mapView).apply {
                    position = arrivalGeo
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = letterMarkerDrawable(mapView.context, "B")
                    title = "B • ${flight.arrivalIata}"
                    snippet = flight.arrivalCity
                }

                mapView.controller.setZoom(routeMapZoom(flight.durationMinutes))
                mapView.controller.setCenter(centerGeo)
                mapView.overlays.add(route)
                mapView.overlays.add(departureMarker)
                mapView.overlays.add(arrivalMarker)
                mapView.invalidate()
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = flight.departureIata,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${briefingDistanceNm(flight.durationMinutes)} NM",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = flight.arrivalIata,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}



private fun letterMarkerDrawable(context: Context, letter: String): BitmapDrawable {
    val size = 84
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(91, 0, 130)
        style = Paint.Style.FILL
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 42f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val radius = size / 2f - 6f
    canvas.drawCircle(size / 2f, size / 2f, radius, fill)
    canvas.drawCircle(size / 2f, size / 2f, radius, stroke)
    val y = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(letter, size / 2f, y, textPaint)
    return BitmapDrawable(context.resources, bitmap)
}

private fun displayDaySafe(dateTime: String): String = com.example.crewportal.util.displayDay(dateTime)

private data class MapPoint(
    val latitude: Float,
    val longitude: Float
)

private fun MapPoint.toGeoPoint(): GeoPoint = GeoPoint(latitude.toDouble(), longitude.toDouble())

private fun routeMapZoom(durationMinutes: Int): Double {
    return when {
        durationMinutes >= 600 -> 3.0
        durationMinutes >= 360 -> 4.0
        durationMinutes >= 180 -> 5.0
        else -> 6.0
    }
}

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
        "CNX" -> MapPoint(18.77f, 98.96f)
        "SYD" -> MapPoint(-33.94f, 151.18f)
        else -> null
    }
}

private fun shouldShowAirportAssignment(flight: FlightEntity): Boolean {
    return flight.departureIata == "BKK" || flight.durationMinutes >= 360
}

private fun layoverHotelFor(iata: String): String {
    return when (iata.uppercase()) {
        "BKK" -> "Hyatt Regency Bangkok Suvarnabhumi Airport"
        "IST" -> "Grand Hyatt Istanbul"
        "FRA" -> "JW Marriott Hotel Frankfurt"
        "SIN" -> "Pan Pacific Singapore"
        "HKT" -> "The Slate Phuket"
        "CXR" -> "Meliá Vinpearl Cam Ranh Beach Resort"
        "NRT", "HND" -> "The Prince Gallery Tokyo Kioicho"
        "LHR" -> "Sofitel London St James"
        "ZRH" -> "Zurich Marriott Hotel"
        "MUC" -> "Hilton Munich City"
        "SYD" -> "Hilton Sydney"
        "MEL" -> "Grand Hyatt Melbourne"
        else -> "Company contracted crew hotel"
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
                Text(if (active) "●" else "○", color = if (active) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 10.dp))
                Text(label, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = if (active) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(main, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (sub.isNotBlank()) Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
