package com.example.crewportal.data.roster

import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.airport.CrewHotelDirectory
import com.example.crewportal.data.crew.InstructorRole
import com.example.crewportal.data.leave.LeaveDatabase
import com.example.crewportal.data.qualification.PilotQualificationSchedule
import com.example.crewportal.data.qualification.ScheduledQualificationDay
import com.example.crewportal.data.qualification.A380QualificationPolicy
import com.example.crewportal.data.route.RouteCatalog
import com.example.crewportal.util.arrivalLocalDateTime
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

    private fun layoverAircraftChoice(route: LayoverRoute, date: java.time.LocalDate, random: Random): AircraftChoice {
        val routePolicy = RouteCatalog.byIata(route.iata)
        val choices = if (routePolicy.a380Eligible && A380QualificationPolicy.canBeAutoAssigned(date)) {
            listOf(
                AircraftChoice("A330", "Airbus A330-343", 15),
                AircraftChoice("A350", "Airbus A350-941", 55),
                AircraftChoice("A380", "Airbus A380-841", 30)
            )
        } else {
            listOf(
                AircraftChoice("A330", "Airbus A330-343", 25),
                AircraftChoice("A350", "Airbus A350-941", 75)
            )
        }
        return weightedChoice(random, choices)
    }

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
            "DEL", "MNL", "DPS" -> listOf(route.outboundTime to 45, "12:30:00" to 20, "17:10:00" to 20, "21:30:00" to 15)
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
        TurnaroundRoute("MNL", "RPLL", "Manila", "Ninoy Aquino Intl", "TG620", "TG621", "12:30:00", 200, 95, 205, "A330", "Airbus A330-343"),
        TurnaroundRoute("DPS", "WADD", "Denpasar", "Ngurah Rai Intl", "TG431", "TG432", "08:50:00", 260, 90, 265, "A330", "Airbus A330-343"),
        TurnaroundRoute("HKG", "VHHH", "Hong Kong", "Hong Kong Intl", "TG600", "TG601", "08:10:00", 175, 105, 180, "A330", "Airbus A330-343")
    )

    private val layoverRoutes = listOf(
        LayoverRoute("IST", "LTFM", "Istanbul", "Istanbul Airport", "TG935", "TG936", "22:40:00", 605, 2, "10:15:00", 580, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("IST"), "Istanbul layover"),
        LayoverRoute("FRA", "EDDF", "Frankfurt", "Frankfurt Main", "TG920", "TG921", "23:20:00", 690, 2, "13:45:00", 675, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("FRA"), "Long-haul augmented crew"),
        LayoverRoute("SVO", "UUEE", "Moscow", "Sheremetyevo Intl", "TG974", "TG975", "21:55:00", 605, 2, "16:20:00", 590, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("SVO"), "Moscow layover"),
        LayoverRoute("LHR", "EGLL", "London", "Heathrow", "TG910", "TG911", "00:55:00", 760, 2, "12:30:00", 705, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("LHR"), "Long-haul augmented crew"),
        LayoverRoute("LED", "ULLI", "Saint Petersburg", "Pulkovo", "TG986", "TG987", "21:30:00", 650, 2, "15:10:00", 625, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("LED"), "Saint Petersburg layover"),
        LayoverRoute("NRT", "RJAA", "Tokyo", "Narita Intl", "TG642", "TG643", "23:50:00", 360, 2, "11:45:00", 410, "A330", "Airbus A330-343", CrewHotelDirectory.hotelFor("NRT"), "Tokyo layover"),
        LayoverRoute("ICN", "RKSI", "Seoul", "Incheon Intl", "TG658", "TG659", "23:10:00", 325, 2, "10:50:00", 360, "A330", "Airbus A330-343", CrewHotelDirectory.hotelFor("ICN"), "Seoul layover"),
        LayoverRoute("DPS", "WADD", "Denpasar", "Ngurah Rai Intl", "TG431", "TG432", "20:30:00", 260, 1, "18:15:00", 265, "A330", "Airbus A330-343", CrewHotelDirectory.hotelFor("DPS"), "Denpasar overnight layover"),
        LayoverRoute("LAX", "KLAX", "Los Angeles", "Los Angeles Intl", "TG800", "TG801", "20:30:00", 1020, 3, "13:00:00", 930, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("LAX"), "Los Angeles ultra long-haul layover"),
        LayoverRoute("SFO", "KSFO", "San Francisco", "San Francisco Intl", "TG802", "TG803", "21:00:00", 995, 3, "13:30:00", 925, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("SFO"), "San Francisco ultra long-haul layover"),
        LayoverRoute("SEA", "KSEA", "Seattle", "Seattle-Tacoma Intl", "TG804", "TG805", "22:00:00", 910, 3, "14:00:00", 850, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("SEA"), "Seattle ultra long-haul layover"),
        LayoverRoute("JFK", "KJFK", "New York", "John F. Kennedy Intl", "TG806", "TG807", "20:00:00", 1050, 3, "12:00:00", 970, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("JFK"), "New York ultra long-haul layover"),
        LayoverRoute("IAD", "KIAD", "Washington", "Washington Dulles Intl", "TG808", "TG809", "20:20:00", 1050, 3, "12:20:00", 980, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("IAD"), "Washington ultra long-haul layover"),
        LayoverRoute("ORD", "KORD", "Chicago", "Chicago O'Hare Intl", "TG810", "TG811", "20:40:00", 1025, 3, "12:40:00", 955, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("ORD"), "Chicago ultra long-haul layover"),
        LayoverRoute("DFW", "KDFW", "Dallas", "Dallas Fort Worth Intl", "TG812", "TG813", "21:10:00", 1060, 3, "13:10:00", 985, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("DFW"), "Dallas ultra long-haul layover"),
        LayoverRoute("BOS", "KBOS", "Boston", "Boston Logan Intl", "TG814", "TG815", "20:10:00", 1045, 3, "12:10:00", 970, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("BOS"), "Boston ultra long-haul layover"),
        LayoverRoute("MIA", "KMIA", "Miami", "Miami Intl", "TG816", "TG817", "19:50:00", 1100, 3, "11:50:00", 1020, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("MIA"), "Miami ultra long-haul layover"),
        LayoverRoute("ATL", "KATL", "Atlanta", "Hartsfield-Jackson Atlanta Intl", "TG818", "TG819", "20:15:00", 1075, 3, "12:15:00", 995, "A350", "Airbus A350-941", CrewHotelDirectory.hotelFor("ATL"), "Atlanta ultra long-haul layover")
    )

    fun generateForMonth(month: YearMonth, seed: Long = stableSeed(month)): List<FlightEntity> {
        val random = Random(seed)
        val flights = mutableListOf<FlightEntity>()
        val occupied = BooleanArray(month.lengthOfMonth() + 1)
        val qualificationEvents = PilotQualificationSchedule.eventsForMonth(month)
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
                .filter { it.dutyType !in setOf("OFF", "STAY") }
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

        fun addGroundDuty(
            day: Int,
            dutyType: String,
            title: String,
            startTime: String,
            endTime: String,
            note: String,
            eventGroupId: String = "",
            eventDayIndex: Int = 0,
            eventTotalDays: Int = 0
        ) {
            if (!canUse(day, 1)) return
            val start = dt(day, startTime)
            val end = dt(day, endTime)
            if (!hasMinimumRest(start, end)) return
            flights += FlightEntity(
                id = "${date(day)}-${dutyType}-${title.replace(" ", "-")}",
                airline = "THAI",
                flightNumber = title,
                aircraftLabel = dutyType,
                aircraftFullName = title,
                registration = "—",
                status = dutyType,
                departureIata = "BKK",
                departureIcao = "VTBS",
                departureCity = "Bangkok",
                departureAirport = "Thai Airways Training Center",
                arrivalIata = "BKK",
                arrivalIcao = "VTBS",
                arrivalCity = "Bangkok",
                arrivalAirport = "Thai Airways Training Center",
                departureDateTime = start.format(formatter),
                arrivalDateTime = end.format(formatter),
                durationMinutes = 0,
                dutyType = dutyType,
                dutyNote = note,
                eventGroupId = eventGroupId,
                eventDayIndex = eventDayIndex,
                eventTotalDays = eventTotalDays,
                flightTimeCreditEligible = false
            )
            mark(day, 1)
        }

        fun addMandatoryQualificationEvents() {
            qualificationEvents.filter { it.dutyType != "LINE_CHECK" }.groupBy { it.eventGroupId }.values.forEach { rawGroup ->
                val group = rawGroup.sortedBy { it.dayIndex }
                val template = group.first()
                val span = template.totalDays.coerceAtLeast(1)
                val preferredStart = template.date.minusDays((template.dayIndex - 1).toLong())
                val candidateStarts = buildList {
                    add(preferredStart)
                    (1..10).forEach { offset ->
                        add(preferredStart.minusDays(offset.toLong()))
                        add(preferredStart.plusDays(offset.toLong()))
                    }
                }.filter { start ->
                    val end = start.plusDays((span - 1).toLong())
                    !end.isBefore(month.atDay(1)) && !start.isAfter(month.atEndOfMonth())
                }
                val actualStart = candidateStarts.firstOrNull { start ->
                    (0 until span).all { offset ->
                        val eventDate = start.plusDays(offset.toLong())
                        LeaveDatabase.leaveFor(eventDate) == null &&
                            (YearMonth.from(eventDate) != month || !occupied[eventDate.dayOfMonth])
                    }
                } ?: return@forEach
                (1..span).forEach qualificationDay@ { index ->
                    val actualDate = actualStart.plusDays((index - 1).toLong())
                    if (YearMonth.from(actualDate) != month) return@qualificationDay
                    val event = group.firstOrNull { it.dayIndex == index } ?: template.copy(
                        title = template.title.substringBefore(" — Day") + " — Day $index/$span",
                        note = template.note.substringBefore(" • Day") + " • Day $index/$span",
                        dayIndex = index
                    )
                    val (startTime, endTime) = if (event.dutyType == "MEDICAL") "09:00:00" to "18:00:00" else "10:00:00" to "16:00:00"
                    addGroundDuty(
                        day = actualDate.dayOfMonth,
                        dutyType = event.dutyType,
                        title = event.title,
                        startTime = startTime,
                        endTime = endTime,
                        note = event.note,
                        eventGroupId = event.eventGroupId,
                        eventDayIndex = index,
                        eventTotalDays = span
                    )
                }
            }
        }

        fun addTurnaround(
            day: Int,
            route: TurnaroundRoute,
            outboundTime: String = route.outboundTime,
            lineCheckEvent: ScheduledQualificationDay? = null
        ) {
            val aircraft = routeAircraftChoice(route, random)
            val routePolicy = RouteCatalog.byIata(route.iata)
            val durationSeed = "$seed-${date(day)}-${route.outboundFlight}-${random.nextInt()}"
            val outboundMinutes = routePolicy.outboundMinutesFor("$durationSeed-OUT")
            val inboundMinutes = routePolicy.inboundMinutesFor("$durationSeed-IN")
            val outboundDeparture = dt(day, outboundTime)
            val outboundArrival = arrivalLocalDateTime(outboundDeparture, "BKK", route.iata, outboundMinutes)
            val inboundDeparture = outboundArrival.plusMinutes(route.turnaroundMinutes.toLong())
            val inboundArrival = arrivalLocalDateTime(inboundDeparture, route.iata, "BKK", inboundMinutes)
            val d = date(day)
            val lineCheck = lineCheckEvent != null
            // Alternate assignments on each six-month cycle: active captain instructor, then
            // third-seat instructor/observer. The result is deterministic for roster review.
            val instructorRole = lineCheckEvent?.let { event ->
                val halfYearIndex = event.date.year * 2 + if (event.date.monthValue <= 6) 0 else 1
                if (halfYearIndex % 2 == 0) InstructorRole.CAPTAIN_INSTRUCTOR else InstructorRole.OBSERVER_INSTRUCTOR
            }.orEmpty()
            val instructorObserver = InstructorRole.isObserver(instructorRole)
            val lineCheckNote = if (lineCheck) "Line Check • ${InstructorRole.note(instructorRole)}" else "Turnaround duty"
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
                durationMinutes = outboundMinutes,
                dutyType = "FLIGHT",
                dutyNote = lineCheckNote,
                eventGroupId = lineCheckEvent?.eventGroupId.orEmpty(),
                eventDayIndex = lineCheckEvent?.dayIndex ?: 0,
                eventTotalDays = lineCheckEvent?.totalDays ?: 0,
                lineCheckRole = instructorRole,
                flightTimeCreditEligible = !instructorObserver
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
                durationMinutes = inboundMinutes,
                dutyType = "FLIGHT",
                dutyNote = if (lineCheck) "Line Check return • ${InstructorRole.note(instructorRole)}" else "Turnaround return",
                eventGroupId = lineCheckEvent?.eventGroupId.orEmpty(),
                eventDayIndex = lineCheckEvent?.dayIndex ?: 0,
                eventTotalDays = lineCheckEvent?.totalDays ?: 0,
                lineCheckRole = instructorRole,
                flightTimeCreditEligible = !instructorObserver
            )
            if (!instructorObserver) plannedBlock += outboundMinutes + inboundMinutes
            recentRouteIatas += route.iata
            if (recentRouteIatas.size > 5) recentRouteIatas.removeAt(0)
            mark(day, 1)
        }

        fun addLayover(day: Int, route: LayoverRoute) {
            val routePolicy = RouteCatalog.byIata(route.iata)
            val aircraft = layoverAircraftChoice(route, month.atDay(day), random)
            val a380FirstOfficer = aircraft.label.startsWith("A380") && month.atDay(day).isBefore(A380QualificationPolicy.captainEffectiveDate)
            val roleNote = if (a380FirstOfficer) " • Assigned First Officer • A380 consolidation" else ""
            val durationSeed = "$seed-${date(day)}-${route.outboundFlight}-${random.nextInt()}"
            val outboundMinutes = routePolicy.outboundMinutesFor("$durationSeed-OUT")
            val inboundMinutes = routePolicy.inboundMinutesFor("$durationSeed-IN")
            val outboundDeparture = dt(day, route.outboundTime)
            val outboundArrival = arrivalLocalDateTime(outboundDeparture, "BKK", route.iata, outboundMinutes)
            val returnDay = day + route.returnOffsetDays
            val returnDeparture = dt(returnDay, route.returnTime)
            val returnArrival = arrivalLocalDateTime(returnDeparture, route.iata, "BKK", inboundMinutes)
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
                durationMinutes = outboundMinutes,
                dutyType = "FLIGHT",
                dutyNote = route.note + roleNote
            )
            for (stayOffset in 1..route.returnOffsetDays) {
                val stayDay = day + stayOffset
                val stayEnd = if (stayDay == returnDay) returnDeparture.minusMinutes(1).format(formatter) else dtString(stayDay, "23:59:00")
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
                    arrivalDateTime = stayEnd,
                    durationMinutes = 0,
                    dutyType = "STAY",
                    dutyNote = route.hotel
                )
            }
            flights += FlightEntity(
                id = "${date(returnDay)}-${route.inboundFlight}-${route.iata}-BKK",
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
                departureDateTime = returnDeparture.format(formatter),
                arrivalDateTime = returnArrival.format(formatter),
                durationMinutes = inboundMinutes,
                dutyType = "FLIGHT",
                dutyNote = "${route.city} return" + roleNote
            )
            plannedBlock += outboundMinutes + inboundMinutes
            recentRouteIatas += route.iata
            if (recentRouteIatas.size > 5) recentRouteIatas.removeAt(0)
            mark(day, route.spanDays)
        }


        fun addTashkentThursdayPattern(day: Int) {
            flights += TashkentRotationFactory.thursdayStaySundayReturn(month.atDay(day))
            plannedBlock += 800
            recentRouteIatas += "TAS"
            if (recentRouteIatas.size > 5) recentRouteIatas.removeAt(0)
            mark(day, 4)
        }

        fun addTashkentSundayPattern(day: Int) {
            flights += TashkentRotationFactory.sundaySameDayDeadhead(month.atDay(day))
            plannedBlock += 395
            recentRouteIatas += "TAS"
            if (recentRouteIatas.size > 5) recentRouteIatas.removeAt(0)
            mark(day, 1)
        }

        fun tashkentThursdayDutyEnd(day: Int): LocalDateTime =
            arrivalLocalDateTime(dt(day + 3, "13:45:00"), "TAS", "BKK", 405)

        fun tashkentSundayDutyEnd(day: Int): LocalDateTime {
            val tasArrival = arrivalLocalDateTime(dt(day, "09:20:00"), "BKK", "TAS", 395)
            val deadheadDeparture = tasArrival.plusMinutes(125)
            return arrivalLocalDateTime(deadheadDeparture, "TAS", "BKK", 405)
        }

        // Leave is reserved before qualification events and ordinary flying. The UI renders the
        // persisted Leave records directly, so no duplicate OFF/Flight rows are created here.
        LeaveDatabase.leaveForMonth(month).forEach { leave ->
            var cursor = maxOf(leave.start, month.atDay(1))
            val end = minOf(leave.end, month.atEndOfMonth())
            while (!cursor.isAfter(end)) {
                occupied[cursor.dayOfMonth] = true
                cursor = cursor.plusDays(1)
            }
        }

        // The date-bounded A380 programme is inserted before ordinary generation so the return
        // from Toulouse and its recovery days cannot be overwritten by a November flight.
        flights += A380TransitionProgram.rowsForMonth(month)
        A380TransitionProgram.reservedDates(month).forEach { reserved -> occupied[reserved.dayOfMonth] = true }

        // October's full-time type-rating programme supersedes ordinary recurrent events. Other
        // months retain the normal six-month qualification schedule.
        if (month != YearMonth.of(2026, 10)) addMandatoryQualificationEvents()

        qualificationEvents.filter { it.dutyType == "LINE_CHECK" }.forEach { event ->
            val preferred = event.date.dayOfMonth
            val candidateDays = buildList {
                add(preferred)
                (1..7).forEach { offset -> add(preferred - offset); add(preferred + offset) }
            }.filter { it in 1..month.lengthOfMonth() }
            val route = turnaroundRoutes.first { it.iata == "HKG" }
            val routePolicy = RouteCatalog.byIata(route.iata)
            val dayForCheck = candidateDays.firstOrNull { candidate ->
                if (!canPlaceDuty(candidate, 1)) return@firstOrNull false
                val start = dt(candidate, route.outboundTime)
                val outboundEnd = arrivalLocalDateTime(start, "BKK", route.iata, routePolicy.outboundMaxMinutes)
                val returnStart = outboundEnd.plusMinutes(route.turnaroundMinutes.toLong())
                val end = arrivalLocalDateTime(returnStart, route.iata, "BKK", routePolicy.inboundMaxMinutes)
                hasMinimumRest(start, end)
            }
            if (dayForCheck != null) addTurnaround(dayForCheck, route, route.outboundTime, event)
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
            val allowLayover = remaining > 650 && day <= month.lengthOfMonth() - 2 && random.nextInt(100) < 42
            if (allowLayover) {
                val candidates = layoverRoutes.shuffled(random).filter { route ->
                    val start = dt(day, route.outboundTime)
                    val end = arrivalLocalDateTime(
                        dt(day + route.returnOffsetDays, route.returnTime),
                        route.iata,
                        "BKK",
                        route.inboundMinutes
                    )
                    canPlaceDuty(day, route.spanDays) && route.blockMinutes <= remaining + 240 && hasMinimumRest(start, end)
                }
                val preferredCandidates = candidates.filter { it.iata !in recentRouteIatas.takeLast(3) }
                val selectedLayover = preferredCandidates.firstOrNull() ?: candidates.firstOrNull()
                val tashkentEligible = "TAS" !in recentRouteIatas && dayOfWeek == java.time.DayOfWeek.THURSDAY &&
                    canPlaceDuty(day, 4) && hasMinimumRest(dt(day, "09:20:00"), tashkentThursdayDutyEnd(day))
                val selectTashkent = tashkentEligible && random.nextInt(candidates.size + 1) == 0
                if (selectTashkent) {
                    addTashkentThursdayPattern(day)
                    day += 5 + random.nextInt(2)
                    continue
                } else if (selectedLayover != null) {
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
            val tashkentEligible = "TAS" !in recentRouteIatas && remaining > 360 && dayOfWeek == java.time.DayOfWeek.SUNDAY &&
                canPlaceDuty(day, 1) && hasMinimumRest(dt(day, "09:20:00"), tashkentSundayDutyEnd(day))
            if (tashkentEligible && random.nextInt(shortCandidates.size + 1) == 0) {
                addTashkentSundayPattern(day)
                day += 2 + random.nextInt(3)
                continue
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
        val generated = flights.sortedBy { it.departureDateTime }
        val validationErrors = RosterConflictValidator.errors(month, generated)
        check(validationErrors.isEmpty()) { "Generated roster conflicts: ${validationErrors.joinToString()}" }
        return generated
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

    /** Same month + ruleset always produces the same reviewable roster. */
    private fun stableSeed(month: YearMonth): Long =
        (month.year * 100L + month.monthValue) * 7_919L + GENERATOR_RULESET_VERSION

    private const val GENERATOR_RULESET_VERSION = 3_001L
}
