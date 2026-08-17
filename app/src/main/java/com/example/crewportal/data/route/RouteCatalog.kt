package com.example.crewportal.data.route

import com.example.crewportal.data.airport.AirportDatabase
import com.example.crewportal.data.airport.AirportGeoDirectory
import com.example.crewportal.data.airport.CrewHotelDirectory

data class RouteDefinition(
    val destinationIata: String,
    val destinationIcao: String,
    val destinationCity: String,
    val destinationAirport: String,
    val outboundMinMinutes: Int,
    val outboundMaxMinutes: Int,
    val inboundMinMinutes: Int,
    val inboundMaxMinutes: Int,
    val aircraft: String,
    val operationType: String,
    val hotel: String,
    val autoGenerationEnabled: Boolean
) {
    val code: String get() = "BKK-$destinationIata"
    val displayName: String get() = "Bangkok — $destinationCity"
    val outboundMinutes: Int get() = midpoint(outboundMinMinutes, outboundMaxMinutes)
    val inboundMinutes: Int get() = midpoint(inboundMinMinutes, inboundMaxMinutes)

    fun outboundMinutesFor(seed: String): Int = selectFiveMinuteStep(outboundMinMinutes, outboundMaxMinutes, seed.hashCode())
    fun inboundMinutesFor(seed: String): Int = selectFiveMinuteStep(inboundMinMinutes, inboundMaxMinutes, seed.hashCode())

    private fun midpoint(min: Int, max: Int): Int = (((min + max) / 2) / 5) * 5
    private fun selectFiveMinuteStep(min: Int, max: Int, seed: Int): Int {
        val slots = ((max - min) / 5).coerceAtLeast(0) + 1
        return min + Math.floorMod(seed, slots) * 5
    }
}

/** Shared route metadata for screens and manual roster changes. */
object RouteCatalog {
    private val autoGenerationIatas = setOf(
        "HKT", "SIN", "KUL", "CNX", "KBV", "SGN", "HAN", "REP", "DEL", "DAC", "MNL", "DPS",
        "HKG", "IST", "FRA", "SVO", "LHR", "NRT", "ICN", "LED", "TAS"
    )

    val routes = listOf(
        route("HKT", 85, 90, "A320 Family", "Domestic"), route("CNX", 75, 80, "A320 Family", "Domestic"),
        route("KBV", 80, 85, "A320 Family", "Domestic"), route("CXR", 105, 110, "A321neo", "Regional"),
        route("SIN", 150, 150, "A321neo / A350", "Regional"), route("KUL", 145, 145, "A321neo", "Regional"),
        route("SGN", 95, 100, "A321neo", "Regional"), route("HAN", 110, 115, "A321neo", "Regional"),
        route("HKG", 175, 180, "A330 / A350", "Regional"), route("TPE", 220, 225, "A330 / A350", "Regional"),
        route("NRT", 360, 410, "A350", "Long-haul"), route("HND", 365, 410, "A350", "Long-haul"),
        route("KIX", 330, 370, "A350", "Long-haul"), route("ICN", 325, 360, "A350", "Long-haul"),
        route("DEL", 265, 260, "A330 / A350", "Medium"), route("BOM", 285, 275, "A330 / A350", "Medium"),
        route("DXB", 390, 370, "A350", "Long-haul"), route("IST", 605, 580, "A350", "Long-haul"),
        route("FRA", 690, 675, "A350", "Long-haul"), route("MUC", 680, 665, "A350", "Long-haul"),
        route("ZRH", 700, 680, "A350", "Long-haul"), route("LHR", 760, 705, "A350", "Long-haul"),
        route("CDG", 735, 700, "A350", "Long-haul"), route("SYD", 560, 590, "A350", "Long-haul"),
        route("MEL", 575, 600, "A350", "Long-haul"), route("PER", 405, 430, "A350", "Long-haul"),
        route("AKL", 690, 720, "A350", "Long-haul"), route("TAS", 395, 405, "A330", "Long-haul"),
        route("LED", 650, 625, "A350", "Long-haul"), route("SVO", 605, 590, "A350", "Long-haul"),
        route("OVB", 420, 410, "A330 / A350", "Long-haul"), route("SVX", 500, 480, "A330 / A350", "Long-haul"),
        route("UUD", 355, 345, "A330", "Long-haul"), route("VVO", 390, 380, "A330", "Long-haul"),
        route("IKT", 370, 360, "A330", "Long-haul"), route("KHV", 410, 400, "A330", "Long-haul"),
        route("DPS", 260, 265, "A330", "Regional"), route("MNL", 200, 205, "A330", "Regional"),
        route("REP", 70, 75, "A320 Family", "Regional"), route("DAC", 150, 155, "A320 Family", "Regional")
    )

    fun byIata(iata: String): RouteDefinition {
        val code = iata.uppercase()
        return routes.firstOrNull { it.destinationIata == code }
            ?: estimatedRoute(code)
    }

    /**
     * Unknown-but-supported airports no longer silently become a 2h30 sector. Estimate a
     * conservative scheduled block from great-circle distance; explicit catalog values remain
     * authoritative for every company route (including LED and LHR).
     */
    private fun estimatedRoute(iata: String): RouteDefinition {
        val distanceNm = AirportGeoDirectory.distanceNm("BKK", iata)
        val stillAirMinutes = distanceNm?.let { ((it / 470.0) * 60.0).toInt() }
        val outbound = stillAirMinutes?.plus(45)?.coerceIn(75, 900) ?: 360
        val inbound = stillAirMinutes?.plus(30)?.coerceIn(75, 900) ?: 345
        return route(iata, outbound, inbound, "Company assigned", "Manual estimate")
    }

    private fun route(iata: String, outbound: Int, inbound: Int, aircraft: String, operation: String): RouteDefinition {
        val airport = AirportDatabase.byIata(iata) ?: AirportDatabase.search(iata).firstOrNull()
        val outboundRange = when (iata) {
            "MNL" -> 195..210
            "DPS" -> 250..270
            else -> (outbound - 10).coerceAtLeast(60)..(outbound + 10)
        }
        val inboundRange = when (iata) {
            "MNL" -> 200..215
            "DPS" -> 255..275
            else -> (inbound - 10).coerceAtLeast(60)..(inbound + 10)
        }
        return RouteDefinition(
            destinationIata = iata,
            destinationIcao = airport?.icao.orEmpty(),
            destinationCity = airport?.city ?: iata,
            destinationAirport = airport?.let { AirportDatabase.shortAirportName(it.iata, it.name) } ?: "$iata Airport",
            outboundMinMinutes = outboundRange.first,
            outboundMaxMinutes = outboundRange.last,
            inboundMinMinutes = inboundRange.first,
            inboundMaxMinutes = inboundRange.last,
            aircraft = aircraft,
            operationType = operation,
            hotel = CrewHotelDirectory.hotelFor(iata),
            autoGenerationEnabled = iata in autoGenerationIatas
        )
    }
}
