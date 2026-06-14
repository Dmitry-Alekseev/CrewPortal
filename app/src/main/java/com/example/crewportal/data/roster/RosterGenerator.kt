package com.example.crewportal.data.roster

import com.example.crewportal.data.local.FlightEntity
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Random

object RosterGenerator {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun generateJune2026(): List<FlightEntity> = generateForMonth(YearMonth.of(2026, 6))

    private data class TurnaroundRoute(
        val iata: String,
        val icao: String,
        val city: String,
        val airport: String,
        val outboundFlight: String,
        val inboundFlight: String,
        val outboundTime: String,
        val outboundMinutes: Int,
        val turnaroundMinutes: Int,
        val inboundMinutes: Int,
        val aircraftLabel: String,
        val aircraftFullName: String
    ) {
        val blockMinutes: Int get() = outboundMinutes + inboundMinutes
    }

    private data class LayoverRoute(
        val iata: String,
        val icao: String,
        val city: String,
        val airport: String,
        val outboundFlight: String,
        val inboundFlight: String,
        val outboundTime: String,
        val outboundMinutes: Int,
        val returnOffsetDays: Int,
        val returnTime: String,
        val inboundMinutes: Int,
        val aircraftLabel: String,
        val aircraftFullName: String,
        val hotel: String,
        val note: String
    ) {
        val spanDays: Int get() = returnOffsetDays + 1
        val blockMinutes: Int get() = outboundMinutes + inboundMinutes
    }

    private data class AircraftChoice(
        val label: String,
        val fullName: String,
        val weight: Int
    )

    private fun weightedChoice(random: Random, choices: List<AircraftChoice>): AircraftChoice {
        val totalWeight = choices.sumOf { it.weight }.coerceAtLeast(1)
        var roll = random.nextInt(totalWeight)
        choices.forEach { choice ->
            roll -= choice.weight
            if (roll < 0) return choice
        }
        return choices.last()
    }

    private fun narrowbodyChoices(): List<AircraftChoice> = listOf(
        AircraftChoice("A320", "Airbus A320-214", 80),
        AircraftChoice("A321neo", "Airbus A321-251NX", 20)
    )

    private fun widebodyChoices(): List<AircraftChoice> = listOf(
        AircraftChoice("A330", "Airbus A330-343", 45),
        AircraftChoice("A350", "Airbus A350-941", 55)
    )

    private fun anyAirbusChoices(): List<AircraftChoice> = listOf(
        AircraftChoice("A320", "Airbus A320-214", 30),
        AircraftChoice("A321neo", "Airbus A321-251NX", 20),
        AircraftChoice("A330", "Airbus A330-343", 25),
        AircraftChoice("A350", "Airbus A350-941", 25)
    )

    private fun routeAircraftChoice(route: TurnaroundRoute, random: Random): AircraftChoice = when (route.iata) {
        // Phuket and domestic/regional Thai "villages" stay narrow-body only.
        "HKT", "CNX", "KBV" -> weightedChoice(random, narrowbodyChoices())

        // KUL/SIN alternate between narrow-body and wide-body in real operations.
        "KUL", "SIN" -> if (random.nextBoolean()) weightedChoice(random, narrowbodyChoices()) else weightedChoice(random, widebodyChoices())

        // India may receive any Airbus type in the app scope.
        "DEL" -> weightedChoice(random, anyAirbusChoices())

        // Smaller regional routes stay close to narrow-body operations.
        "SGN", "HAN", "REP", "DAC" -> weightedChoice(random, narrowbodyChoices())

        // Keep existing route intent for other routes.
        else -> AircraftChoice(route.aircraftLabel, route.aircraftFullName, 1)
    }

    private fun routeTimeChoice(route: TurnaroundRoute, random: Random): String {
        val options = when (route.iata) {
            "SIN" -> listOf("08:00:00" to 35, "12:40:00" to 25, "16:40:00" to 30, "19:20:00" to 10)
            "KUL" -> listOf("09:05:00" to 35, "13:20:00" to 25, "16:10:00" to 25, "20:15:00" to 15)
            "HKT" -> listOf("07:45:00" to 40, "11:30:00" to 25, "15:45:00" to 25, "19:10:00" to 10)
            "CNX", "KBV" -> listOf("07:55:00" to 45, "10:15:00" to 25, "14:35:00" to 20, "18:20:00" to 10)
            "DEL", "MNL" -> listOf(route.outboundTime to 45, "12:30:00" to 20, "17:10:00" to 20, "21:30:00" to 15)
            else -> listOf(route.outboundTime to 55, "10:40:00" to 20, "14:20:00" to 15, "18:30:00" to 10)
        }
        val total = options.sumOf { it.second }
        var roll = random.nextInt(total)
        options.forEach { option ->
            roll -= option.second
            if (roll < 0) return option.first
        }
        return route.outboundTime
    }

    private val turnaroundRoutes = listOf(
        TurnaroundRoute("HKT", "VTSP", "Phuket", "Phuket Intl", "TG201", "TG202", "07:45:00", 85, 70, 90, "A320", "Airbus A320-214"),
        TurnaroundRoute("SIN", "WSSS", "Singapore", "Changi Intl", "TG403", "TG404", "08:00:00", 150, 125, 150, "A321neo", "Airbus A321-251NX"),
        TurnaroundRoute("KUL", "WMKK", "Kuala Lumpur", "KLIA", "TG415", "TG416", "09:05:00", 145, 95, 145, "A320", "Airbus A320-214"),
        TurnaroundRoute("CNX", "VTCC", "Chiang Mai", "Chiang Mai Intl", "TG102", "TG103", "07:55:00", 80, 65, 80, "A320", "Airbus A320-214"),
        TurnaroundRoute("KBV", "VTSG", "Krabi", "Krabi Intl", "TG249", "TG250", "10:15:00", 80, 75, 85, "A320", "Airbus A320-214"),
        TurnaroundRoute("SGN", "VVTS", "Ho Chi Minh City", "Tan Son Nhat Intl", "TG550", "TG551", "07:35:00", 95, 85, 100, "A320", "Airbus A320-214"),
        TurnaroundRoute("HAN", "VVNB", "Hanoi", "Noi Bai Intl", "TG560", "TG561", "14:20:00", 110, 90, 115, "A320", "Airbus A320-214"),
        TurnaroundRoute("REP", "VDSR", "Siem Reap", "Siem Reap Angkor Intl", "TG2588", "TG2589", "09:35:00", 70, 80, 75, "A320", "Airbus A320-214"),
        TurnaroundRoute("DEL", "VIDP", "Delhi", "Indira Gandhi Intl", "TG331", "TG332", "08:20:00", 265, 75, 260, "A330", "Airbus A330-343"),
        TurnaroundRoute("DAC", "VGHS", "Dhaka", "Hazrat Shahjalal Intl", "TG321", "TG322", "10:40:00", 150, 85, 155, "A320", "Airbus A320-214"),
        TurnaroundRoute("MNL", "RPLL", "Manila", "Ninoy Aquino Intl", "TG620", "TG621", "12:30:00", 200, 95, 205, "A330", "Airbus A330-343")
    )

    private val layoverRoutes = listOf(
        LayoverRoute("IST", "LTFM", "Istanbul", "Istanbul Airport", "TG935", "TG936", "22:40:00", 605, 2, "10:15:00", 580, "A350", "Airbus A350-941", "Grand Hyatt Istanbul", "Layover, Grand Hyatt Istanbul"),
        LayoverRoute("FRA", "EDDF", "Frankfurt", "Frankfurt Main", "TG920", "TG921", "23:20:00", 690, 2, "13:45:00", 675, "A350", "Airbus A350-941", "JW Marriott Hotel Frankfurt", "Long-haul augmented crew"),
        LayoverRoute("SVO", "UUEE", "Moscow", "Sheremetyevo Intl", "TG974", "TG975", "21:55:00", 605, 2, "16:20:00", 590, "A350", "Airbus A350-941", "Hyatt Regency Moscow Petrovsky Park", "Moscow layover"),
        LayoverRoute("LHR", "EGLL", "London", "Heathrow", "TG910", "TG911", "00:55:00", 760, 2, "12:30:00", 705, "A350", "Airbus A350-941", "Sofitel London Heathrow", "Long-haul augmented crew"),
        LayoverRoute("NRT", "RJAA", "Tokyo", "Narita Intl", "TG642", "TG643", "23:50:00", 360, 2, "11:45:00", 410, "A330", "Airbus A330-343", "Hilton Tokyo Narita Airport", "Tokyo layover"),
        LayoverRoute("ICN", "RKSI", "Seoul", "Incheon Intl", "TG658", "TG659", "23:10:00", 325, 2, "10:50:00", 360, "A330", "Airbus A330-343", "Grand Hyatt Incheon", "Seoul layover"),
        LayoverRoute("DPS", "WADD", "Denpasar", "Ngurah Rai Intl", "TG431", "TG432", "08:50:00", 260, 1, "16:10:00", 265, "A330", "Airbus A330-343", "Hyatt Regency Bali", "Denpasar layover")
    )

    fun generateForMonth(month: YearMonth): List<FlightEntity> {
        val random = Random((month.year * 100 + month.monthValue).toLong() * 7919L)
        val flights = mutableListOf<FlightEntity>()
        val occupied = BooleanArray(month.lengthOfMonth() + 1)
        val recentRouteIatas = mutableListOf<String>()
        var plannedBlock = 0
        val targetBlock = 78 * 60 + random.nextInt(5) * 30 // 78h00..80h00, then may slightly overshoot.

        fun date(day: Int): String = "%04d-%02d-%02d".format(month.year, month.monthValue, day)
        fun dt(day: Int, time: String): LocalDateTime = LocalDateTime.parse("${date(day)}T$time", formatter)
        fun dtString(day: Int, time: String): String = dt(day, time).format(formatter)
        fun canUse(day: Int, span: Int): Boolean = day >= 1 && day + span - 1 <= month.lengthOfMonth() && (day until day + span).all { !occupied[it] }
        fun canPlaceDuty(day: Int, span: Int): Boolean {
            if (!canUse(day, span)) return false
            val beforeTwo = day > 2 && occupied[day - 1] && occupied[day - 2]
            val afterTwo = day + span + 1 <= month.lengthOfMonth() && occupied[day + span] && occupied[day + span + 1]
            return !beforeTwo && !afterTwo
        }
        fun hasMinimumRest(start: LocalDateTime, end: LocalDateTime): Boolean {
            return flights
                .filter { it.dutyType == "FLIGHT" || it.dutyType == "RESERVE" || it.dutyType == "DEADHEAD" }
                .all { existing ->
                    val existingStart = parseDateTime(existing.departureDateTime)
                    val existingEnd = parseDateTime(existing.arrivalDateTime)
                    when {
                        !end.isAfter(existingStart) -> ChronoUnit.MINUTES.between(end, existingStart) >= 12 * 60
                        !existingEnd.isAfter(start) -> ChronoUnit.MINUTES.between(existingEnd, start) >= 12 * 60
                        else -> false
                    }
                }
        }
        fun mark(day: Int, span: Int) { (day until day + span).forEach { occupied[it] = true } }

        fun addOff(day: Int) {
            val d = date(day)
            flights += FlightEntity(
                id = "$d-OFF",
                airline = "THAI",
                flightNumber = "OFF",
                aircraftLabel = "OFF",
                aircraftFullName = "Day Off",
                registration = "—",
                status = "OFF",
                departureIata = "BKK",
                departureIcao = "VTBS",
                departureCity = "Bangkok",
                departureAirport = "Suvarnabhumi Intl",
                arrivalIata = "BKK",
                arrivalIcao = "VTBS",
                arrivalCity = "Bangkok",
                arrivalAirport = "Suvarnabhumi Intl",
                departureDateTime = dtString(day, "00:00:00"),
                arrivalDateTime = dtString(day, "23:59:00"),
                durationMinutes = 0,
                dutyType = "OFF",
                dutyNote = "Day off"
            )
        }

        fun addReserve(day: Int) {
            val d = date(day)
            val nightReserve = random.nextBoolean() && day < month.lengthOfMonth()
            val reserveStartOptions = listOf("06:00:00", "08:00:00", "10:00:00")
            val startTime = if (nightReserve) "20:00:00" else reserveStartOptions[random.nextInt(reserveStartOptions.size)]
            val endDateTime = if (nightReserve) dt(day, startTime).plusHours(12) else when (startTime) {
                "06:00:00" -> dt(day, "18:00:00")
                "10:00:00" -> dt(day, "22:00:00")
                else -> dt(day, "20:00:00")
            }
            val reserveLabel = if (nightReserve) "NIGHT RESERVE" else "HOTEL RESERVE"
            flights += FlightEntity(
                id = "$d-${reserveLabel.replace(" ", "-")}",
                airline = "THAI",
                flightNumber = reserveLabel,
                aircraftLabel = "RES",
                aircraftFullName = if (nightReserve) "Night Reserve" else "Hotel Reserve",
                registration = "—",
                status = "RESERVE",
                departureIata = "BKK",
                departureIcao = "VTBS",
                departureCity = "Bangkok",
                departureAirport = "Hyatt Regency Bangkok Suvarnabhumi Airport",
                arrivalIata = "BKK",
                arrivalIcao = "VTBS",
                arrivalCity = "Bangkok",
                arrivalAirport = "Hyatt Regency Bangkok Suvarnabhumi Airport",
                departureDateTime = dt(day, startTime).format(formatter),
                arrivalDateTime = endDateTime.format(formatter),
                durationMinutes = 0,
                dutyType = "RESERVE",
                dutyNote = if (nightReserve) "Night standby reserve, Hyatt Regency Bangkok Suvarnabhumi Airport" else "Hotel reserve, Hyatt Regency Bangkok Suvarnabhumi Airport"
            )
            mark(day, 1)
        }

        fun addTurnaround(day: Int, route: TurnaroundRoute, outboundTime: String = route.outboundTime) {
            val aircraft = routeAircraftChoice(route, random)
            val outboundDeparture = dt(day, outboundTime)
            val outboundArrival = outboundDeparture.plusMinutes(route.outboundMinutes.toLong())
            val inboundDeparture = outboundArrival.plusMinutes(route.turnaroundMinutes.toLong())
            val inboundArrival = inboundDeparture.plusMinutes(route.inboundMinutes.toLong())
            val d = date(day)
            flights += FlightEntity(
                id = "$d-${route.outboundFlight}-BKK-${route.iata}",
                airline = "THAI",
                flightNumber = route.outboundFlight,
                aircraftLabel = aircraft.label,
                aircraftFullName = aircraft.fullName,
                registration = "TBA",
                status = "SCHEDULED",
                departureIata = "BKK",
                departureIcao = "VTBS",
                departureCity = "Bangkok",
                departureAirport = airportName("BKK"),
                arrivalIata = route.iata,
                arrivalIcao = route.icao,
                arrivalCity = route.city,
                arrivalAirport = route.airport,
                departureDateTime = outboundDeparture.format(formatter),
                arrivalDateTime = outboundArrival.format(formatter),
                durationMinutes = route.outboundMinutes,
                dutyType = "FLIGHT",
                dutyNote = "Turnaround duty"
            )
            flights += FlightEntity(
                id = "$d-${route.inboundFlight}-${route.iata}-BKK",
                airline = "THAI",
                flightNumber = route.inboundFlight,
                aircraftLabel = aircraft.label,
                aircraftFullName = aircraft.fullName,
                registration = "TBA",
                status = "SCHEDULED",
                departureIata = route.iata,
                departureIcao = route.icao,
                departureCity = route.city,
                departureAirport = route.airport,
                arrivalIata = "BKK",
                arrivalIcao = "VTBS",
                arrivalCity = "Bangkok",
                arrivalAirport = airportName("BKK"),
                departureDateTime = inboundDeparture.format(formatter),
                arrivalDateTime = inboundArrival.format(formatter),
                durationMinutes = route.inboundMinutes,
                dutyType = "FLIGHT",
                dutyNote = "Turnaround return"
            )
            plannedBlock += route.blockMinutes
            recentRouteIatas += route.iata
            if (recentRouteIatas.size > 5) recentRouteIatas.removeAt(0)
            mark(day, 1)
        }

        fun addLayover(day: Int, route: LayoverRoute) {
            val outboundDeparture = dt(day, route.outboundTime)
            val outboundArrival = outboundDeparture.plusMinutes(route.outboundMinutes.toLong())
            val returnDay = day + route.returnOffsetDays
            val returnDeparture = dt(returnDay, route.returnTime)
            val returnArrival = returnDeparture.plusMinutes(route.inboundMinutes.toLong())
            val d = date(day)
            flights += FlightEntity(
                id = "$d-${route.outboundFlight}-BKK-${route.iata}",
                airline = "THAI",
                flightNumber = route.outboundFlight,
                aircraftLabel = route.aircraftLabel,
                aircraftFullName = route.aircraftFullName,
                registration = "TBA",
                status = "SCHEDULED",
                departureIata = "BKK",
                departureIcao = "VTBS",
                departureCity = "Bangkok",
                departureAirport = airportName("BKK"),
                arrivalIata = route.iata,
                arrivalIcao = route.icao,
                arrivalCity = route.city,
                arrivalAirport = route.airport,
                departureDateTime = outboundDeparture.format(formatter),
                arrivalDateTime = outboundArrival.format(formatter),
                durationMinutes = route.outboundMinutes,
                dutyType = "FLIGHT",
                dutyNote = route.note
            )
            for (stayOffset in 1 until route.returnOffsetDays) {
                val stayDay = day + stayOffset
                flights += FlightEntity(
                    id = "${date(stayDay)}-STAY-${route.iata}",
                    airline = "THAI",
                    flightNumber = "Stay in ${route.city}",
                    aircraftLabel = "STAY",
                    aircraftFullName = "Layover stay",
                    registration = "—",
                    status = "STAY",
                    departureIata = route.iata,
                    departureIcao = route.icao,
                    departureCity = route.city,
                    departureAirport = route.hotel,
                    arrivalIata = route.iata,
                    arrivalIcao = route.icao,
                    arrivalCity = route.city,
                    arrivalAirport = route.hotel,
                    departureDateTime = dtString(stayDay, "00:00:00"),
                    arrivalDateTime = dtString(stayDay, "23:59:00"),
                    durationMinutes = 0,
                    dutyType = "STAY",
                    dutyNote = route.hotel
                )
            }
            flights += FlightEntity(
                id = "${date(returnDay)}-${route.inboundFlight}-${route.iata}-BKK",
                airline = "THAI",
                flightNumber = route.inboundFlight,
                aircraftLabel = route.aircraftLabel,
                aircraftFullName = route.aircraftFullName,
                registration = "TBA",
                status = "SCHEDULED",
                departureIata = route.iata,
                departureIcao = route.icao,
                departureCity = route.city,
                departureAirport = route.airport,
                arrivalIata = "BKK",
                arrivalIcao = "VTBS",
                arrivalCity = "Bangkok",
                arrivalAirport = airportName("BKK"),
                departureDateTime = returnDeparture.format(formatter),
                arrivalDateTime = returnArrival.format(formatter),
                durationMinutes = route.inboundMinutes,
                dutyType = "FLIGHT",
                dutyNote = "${route.city} return"
            )
            plannedBlock += route.blockMinutes
            recentRouteIatas += route.iata
            if (recentRouteIatas.size > 5) recentRouteIatas.removeAt(0)
            mark(day, route.spanDays)
        }


        fun addTashkentThursdayPattern(day: Int) {
            val outboundDeparture = dt(day, "09:20:00")
            val outboundArrival = outboundDeparture.plusMinutes(395)
            val returnDay = day + 3
            val returnDeparture = dt(returnDay, "13:45:00")
            val returnArrival = returnDeparture.plusMinutes(405)
            val d = date(day)
            flights += FlightEntity(
                id = "$d-TG684-BKK-TAS",
                airline = "THAI",
                flightNumber = "TG684",
                aircraftLabel = "A330",
                aircraftFullName = "Airbus A330-343",
                registration = "TBA",
                status = "SCHEDULED",
                departureIata = "BKK",
                departureIcao = "VTBS",
                departureCity = "Bangkok",
                departureAirport = airportName("BKK"),
                arrivalIata = "TAS",
                arrivalIcao = "UTTT",
                arrivalCity = "Tashkent",
                arrivalAirport = "Islam Karimov",
                departureDateTime = outboundDeparture.format(formatter),
                arrivalDateTime = outboundArrival.format(formatter),
                durationMinutes = 395,
                dutyType = "FLIGHT",
                dutyNote = "Tashkent special rotation"
            )
            for (stayDay in (day + 1)..(day + 2)) {
                flights += FlightEntity(
                    id = "${date(stayDay)}-STAY-TAS",
                    airline = "THAI",
                    flightNumber = "Stay in Tashkent",
                    aircraftLabel = "STAY",
                    aircraftFullName = "Layover stay",
                    registration = "—",
                    status = "STAY",
                    departureIata = "TAS",
                    departureIcao = "UTTT",
                    departureCity = "Tashkent",
                    departureAirport = "Hyatt Regency Tashkent",
                    arrivalIata = "TAS",
                    arrivalIcao = "UTTT",
                    arrivalCity = "Tashkent",
                    arrivalAirport = "Hyatt Regency Tashkent",
                    departureDateTime = dtString(stayDay, "00:00:00"),
                    arrivalDateTime = dtString(stayDay, "23:59:00"),
                    durationMinutes = 0,
                    dutyType = "STAY",
                    dutyNote = "Hyatt Regency Tashkent"
                )
            }
            flights += FlightEntity(
                id = "${date(returnDay)}-TG685-TAS-BKK",
                airline = "THAI",
                flightNumber = "TG685",
                aircraftLabel = "A330",
                aircraftFullName = "Airbus A330-343",
                registration = "TBA",
                status = "SCHEDULED",
                departureIata = "TAS",
                departureIcao = "UTTT",
                departureCity = "Tashkent",
                departureAirport = "Islam Karimov",
                arrivalIata = "BKK",
                arrivalIcao = "VTBS",
                arrivalCity = "Bangkok",
                arrivalAirport = airportName("BKK"),
                departureDateTime = returnDeparture.format(formatter),
                arrivalDateTime = returnArrival.format(formatter),
                durationMinutes = 405,
                dutyType = "FLIGHT",
                dutyNote = "Tashkent operating return"
            )
            plannedBlock += 800
            recentRouteIatas += "TAS"
            if (recentRouteIatas.size > 5) recentRouteIatas.removeAt(0)
            mark(day, 4)
        }

        fun addTashkentSundayPattern(day: Int) {
            val outboundDeparture = dt(day, "09:20:00")
            val outboundArrival = outboundDeparture.plusMinutes(395)
            val deadheadDeparture = outboundArrival.plusMinutes(125)
            val deadheadArrival = deadheadDeparture.plusMinutes(405)
            val d = date(day)
            flights += FlightEntity(
                id = "$d-TG684-BKK-TAS",
                airline = "THAI",
                flightNumber = "TG684",
                aircraftLabel = "A330",
                aircraftFullName = "Airbus A330-343",
                registration = "TBA",
                status = "SCHEDULED",
                departureIata = "BKK",
                departureIcao = "VTBS",
                departureCity = "Bangkok",
                departureAirport = airportName("BKK"),
                arrivalIata = "TAS",
                arrivalIcao = "UTTT",
                arrivalCity = "Tashkent",
                arrivalAirport = "Islam Karimov",
                departureDateTime = outboundDeparture.format(formatter),
                arrivalDateTime = outboundArrival.format(formatter),
                durationMinutes = 395,
                dutyType = "FLIGHT",
                dutyNote = "Tashkent Sunday special rotation"
            )
            flights += FlightEntity(
                id = "$d-TG685-TAS-BKK-DH",
                airline = "THAI",
                flightNumber = "Deadhead TG685",
                aircraftLabel = "POS",
                aircraftFullName = "Passenger positioning",
                registration = "—",
                status = "DEADHEAD",
                departureIata = "TAS",
                departureIcao = "UTTT",
                departureCity = "Tashkent",
                departureAirport = "Islam Karimov",
                arrivalIata = "BKK",
                arrivalIcao = "VTBS",
                arrivalCity = "Bangkok",
                arrivalAirport = airportName("BKK"),
                departureDateTime = deadheadDeparture.format(formatter),
                arrivalDateTime = deadheadArrival.format(formatter),
                durationMinutes = 0,
                dutyType = "DEADHEAD",
                dutyNote = "Passenger return to Bangkok, no layover"
            )
            plannedBlock += 395
            recentRouteIatas += "TAS"
            if (recentRouteIatas.size > 5) recentRouteIatas.removeAt(0)
            mark(day, 1)
        }

        var day = 1 + random.nextInt(2)
        var guard = 0
        while (day <= month.lengthOfMonth() && plannedBlock < targetBlock && guard < 80) {
            guard++
            if (occupied[day]) {
                day++
                continue
            }
            val remaining = targetBlock - plannedBlock
            val dayOfWeek = month.atDay(day).dayOfWeek
            if ("TAS" !in recentRouteIatas && remaining > 600 && dayOfWeek == java.time.DayOfWeek.THURSDAY && canPlaceDuty(day, 4) && hasMinimumRest(dt(day, "09:20:00"), dt(day + 3, "13:45:00").plusMinutes(405)) && random.nextInt(100) < 30) {
                addTashkentThursdayPattern(day)
                day += 5 + random.nextInt(2)
                continue
            }
            if ("TAS" !in recentRouteIatas && remaining > 360 && dayOfWeek == java.time.DayOfWeek.SUNDAY && canPlaceDuty(day, 1) && hasMinimumRest(dt(day, "09:20:00"), dt(day, "09:20:00").plusMinutes(925)) && random.nextInt(100) < 18) {
                addTashkentSundayPattern(day)
                day += 2 + random.nextInt(3)
                continue
            }
            val allowLayover = remaining > 650 && day <= month.lengthOfMonth() - 2 && random.nextInt(100) < 42
            if (allowLayover) {
                val candidates = layoverRoutes.shuffled(random).filter { route ->
                    val start = dt(day, route.outboundTime)
                    val end = dt(day + route.returnOffsetDays, route.returnTime).plusMinutes(route.inboundMinutes.toLong())
                    canPlaceDuty(day, route.spanDays) && route.blockMinutes <= remaining + 240 && hasMinimumRest(start, end)
                }
                val preferredCandidates = candidates.filter { it.iata !in recentRouteIatas.takeLast(3) }
                val selectedLayover = preferredCandidates.firstOrNull() ?: candidates.firstOrNull()
                if (selectedLayover != null) {
                    addLayover(day, selectedLayover)
                    day += selectedLayover.spanDays + 1 + random.nextInt(3)
                    continue
                }
            }
            val shortCandidates = turnaroundRoutes.shuffled(random).map { route -> route to routeTimeChoice(route, random) }.filter { (route, departureTime) ->
                val start = dt(day, departureTime)
                val end = start.plusMinutes((route.outboundMinutes + route.turnaroundMinutes + route.inboundMinutes).toLong())
                canPlaceDuty(day, 1) && route.blockMinutes <= remaining + 180 && hasMinimumRest(start, end)
            }
            val preferredShort = shortCandidates.filter { (route, _) -> route.iata !in recentRouteIatas.takeLast(3) }
            val selectedShort = preferredShort.firstOrNull() ?: shortCandidates.firstOrNull()
            if (selectedShort != null) {
                addTurnaround(day, selectedShort.first, selectedShort.second)
                day += 1 + random.nextInt(4)
            } else {
                day++
            }
        }

        // Top up with short turnarounds if long-haul selections left the month below a realistic target.
        fun hasNeighbourDuty(day: Int): Boolean {
            val previous = day > 1 && occupied[day - 1]
            val next = day < month.lengthOfMonth() && occupied[day + 1]
            return previous || next
        }
        val openDays = (1..month.lengthOfMonth()).filter { !occupied[it] }
        val topUpDays = openDays.filter { !hasNeighbourDuty(it) }.shuffled(random) + openDays.filter { hasNeighbourDuty(it) }.shuffled(random)
        for (candidateDay in topUpDays) {
            if (plannedBlock >= 78 * 60) break
            if (!canPlaceDuty(candidateDay, 1)) continue
            val routeCandidates = turnaroundRoutes
                .map { route -> route to routeTimeChoice(route, random) }
                .filter { (route, departureTime) ->
                    val start = dt(candidateDay, departureTime)
                    val end = start.plusMinutes((route.outboundMinutes + route.turnaroundMinutes + route.inboundMinutes).toLong())
                    route.blockMinutes <= (82 * 60 - plannedBlock) && hasMinimumRest(start, end)
                }
                .shuffled(random)
            val route = routeCandidates.filter { (candidateRoute, _) -> candidateRoute.iata !in recentRouteIatas.takeLast(3) }.firstOrNull()
                ?: routeCandidates.firstOrNull()
                ?: continue
            addTurnaround(candidateDay, route.first, route.second)
        }

        // Add 1-2 reserve duties on empty days, avoiding direct crowding where possible.
        val reserveCount = 1 + random.nextInt(2)
        val reserveDays = (1..month.lengthOfMonth()).filter { !occupied[it] }.shuffled(random).take(reserveCount)
        reserveDays.forEach { addReserve(it) }

        (1..month.lengthOfMonth()).forEach { if (!occupied[it]) addOff(it) }
        return flights.sortedBy { it.departureDateTime }
    }

    private fun parseDateTime(value: String): LocalDateTime = LocalDateTime.parse(value, formatter)

    private fun airportName(iata: String): String = when (iata) {
        "BKK" -> "Suvarnabhumi Intl"
        "HKT" -> "Phuket Intl"
        "CNX" -> "Chiang Mai Intl"
        "KBV" -> "Krabi Intl"
        "CXR" -> "Cam Ranh Intl"
        "SIN" -> "Changi Intl"
        "KUL" -> "KLIA"
        "DEL" -> "Indira Gandhi Intl"
        "FRA" -> "Frankfurt Main"
        "IST" -> "Istanbul Airport"
        "TAS" -> "Islam Karimov"
        "SVO" -> "Sheremetyevo Intl"
        "LHR" -> "Heathrow"
        "NRT" -> "Narita Intl"
        "ICN" -> "Incheon Intl"
        "DPS" -> "Ngurah Rai Intl"
        "SGN" -> "Tan Son Nhat Intl"
        "HAN" -> "Noi Bai Intl"
        "REP" -> "Siem Reap Angkor Intl"
        "DAC" -> "Hazrat Shahjalal Intl"
        "MNL" -> "Ninoy Aquino Intl"
        else -> "$iata Airport"
    }
}
