package com.example.crewportal.data.roster

import com.example.crewportal.data.airport.AirportDatabase
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.util.arrivalLocalDateTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/**
 * One-time October 2026 A380 type-rating programme requested by Crew Planning.
 *
 * The scenario is deliberately isolated from the ordinary random generator. It reserves every
 * day from positioning on 15 October through the final check, then reserves the passenger return
 * and recovery rest on 1-3 November. Delete this object and its generator hook after the 2026
 * transition has passed; no persistent schema depends on it.
 */
object A380TransitionProgram {
    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)
    private const val GROUP_ID = "A380-TYPE-RATING-TLS-2026"

    fun appliesTo(month: YearMonth): Boolean = month == october || month == november

    fun reservedDates(month: YearMonth): Set<LocalDate> = when (month) {
        october -> (15..31).mapTo(mutableSetOf()) { october.atDay(it) }
        november -> (1..3).mapTo(mutableSetOf()) { november.atDay(it) }
        else -> emptySet()
    }

    fun rowsForMonth(month: YearMonth): List<FlightEntity> = when (month) {
        october -> octoberRows()
        november -> novemberReturnRows()
        else -> emptyList()
    }

    private fun octoberRows(): List<FlightEntity> = buildList {
        val bkkDeparture = LocalDateTime.of(2026, 10, 15, 9, 30)
        val istArrival = arrivalLocalDateTime(bkkDeparture, "BKK", "IST", 600)
        add(deadhead("2026-10-15-TK69-BKK-IST-A380-TRAINING", "TK69", "BKK", "IST", bkkDeparture, istArrival, 600, "Passenger positioning to A380 type-rating course"))

        val istDeparture = LocalDateTime.of(2026, 10, 15, 18, 30)
        val tlsArrival = arrivalLocalDateTime(istDeparture, "IST", "TLS", 210)
        add(deadhead("2026-10-15-TK1805-IST-TLS-A380-TRAINING", "TK1805", "IST", "TLS", istDeparture, tlsArrival, 210, "Passenger connection to Toulouse • A380 training"))

        val trainingDays = (16..28).map(october::atDay).filter { it.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
        trainingDays.forEachIndexed { index, date ->
            add(ground(date, "TRAINING", "A380 Type Rating Training", "09:00", "17:00", "Toulouse Airbus Training Centre • Ground school and systems • Training day ${index + 1}", index + 1, 12))
        }

        listOf(17, 18, 24, 25).forEach { day ->
            add(off(october.atDay(day), "A380 training weekend • No training duty"))
        }

        add(ground(october.atDay(29), "EXAM", "A380 Theoretical Examination", "09:00", "14:00", "Toulouse Airbus Training Centre • Type-rating theory examination", 10, 12))
        add(ground(october.atDay(30), "SIMULATOR", "A380 Simulator Training Session", "09:00", "17:00", "Full-flight simulator training session before final check", 11, 12))
        add(ground(october.atDay(31), "SIMULATOR", "A380 Final Practical Examination", "09:00", "17:00", "Final A380 simulator skill test • Rating effective after successful completion", 12, 12))
    }.sortedBy { it.departureDateTime }

    private fun novemberReturnRows(): List<FlightEntity> = buildList {
        val tlsDeparture = LocalDateTime.of(2026, 11, 1, 11, 0)
        val istArrival = arrivalLocalDateTime(tlsDeparture, "TLS", "IST", 210)
        add(deadhead("2026-11-01-TK1804-TLS-IST-A380-RETURN", "TK1804", "TLS", "IST", tlsDeparture, istArrival, 210, "Passenger return after A380 type-rating course"))

        val istDeparture = LocalDateTime.of(2026, 11, 1, 20, 0)
        val bkkArrival = arrivalLocalDateTime(istDeparture, "IST", "BKK", 580)
        add(deadhead("2026-11-01-TK58-IST-BKK-A380-RETURN", "TK58", "IST", "BKK", istDeparture, bkkArrival, 580, "Passenger return to home base after A380 qualification"))
        add(off(november.atDay(2), "Post-training arrival recovery • No operating duty"))
        add(off(november.atDay(3), "Post-training recovery day • No operating duty"))
    }.sortedBy { it.departureDateTime }

    private fun deadhead(
        id: String,
        flightNumber: String,
        from: String,
        to: String,
        departure: LocalDateTime,
        arrival: LocalDateTime,
        durationMinutes: Int,
        note: String
    ): FlightEntity {
        val fromAirport = requireNotNull(AirportDatabase.byIata(from))
        val toAirport = requireNotNull(AirportDatabase.byIata(to))
        return FlightEntity(
            id = id,
            airline = "Turkish Airlines",
            flightNumber = flightNumber,
            aircraftLabel = "DEADHEAD",
            aircraftFullName = "Passenger positioning",
            registration = "—",
            status = "SCHEDULED",
            departureIata = fromAirport.iata,
            departureIcao = fromAirport.icao,
            departureCity = fromAirport.city,
            departureAirport = fromAirport.name,
            arrivalIata = toAirport.iata,
            arrivalIcao = toAirport.icao,
            arrivalCity = toAirport.city,
            arrivalAirport = toAirport.name,
            departureDateTime = departure.toString(),
            arrivalDateTime = arrival.toString(),
            durationMinutes = durationMinutes,
            dutyType = "DEADHEAD",
            dutyNote = note,
            rosterSource = "A380_TRANSITION_PROGRAM",
            eventGroupId = GROUP_ID,
            flightTimeCreditEligible = false
        )
    }

    private fun ground(
        date: LocalDate,
        dutyType: String,
        title: String,
        start: String,
        end: String,
        note: String,
        dayIndex: Int,
        totalDays: Int
    ): FlightEntity {
        val airport = requireNotNull(AirportDatabase.byIata("TLS"))
        return FlightEntity(
            id = "$date-${title.uppercase().replace(Regex("[^A-Z0-9]+"), "-").trim('-')}",
            airline = "AIRBUS TRAINING",
            flightNumber = title,
            aircraftLabel = "A380-800",
            aircraftFullName = "Airbus A380-841 Type Rating",
            registration = "SIM",
            status = "TRAINING",
            departureIata = airport.iata,
            departureIcao = airport.icao,
            departureCity = airport.city,
            departureAirport = "Airbus Training Centre Toulouse",
            arrivalIata = airport.iata,
            arrivalIcao = airport.icao,
            arrivalCity = airport.city,
            arrivalAirport = "Airbus Training Centre Toulouse",
            departureDateTime = LocalDateTime.of(date, LocalTime.parse(start)).toString(),
            arrivalDateTime = LocalDateTime.of(date, LocalTime.parse(end)).toString(),
            durationMinutes = 0,
            dutyType = dutyType,
            dutyNote = note,
            rosterSource = "A380_TRANSITION_PROGRAM",
            eventGroupId = GROUP_ID,
            eventDayIndex = dayIndex,
            eventTotalDays = totalDays,
            flightTimeCreditEligible = false
        )
    }

    private fun off(date: LocalDate, note: String): FlightEntity = FlightEntity(
        id = "$date-A380-TRAINING-OFF",
        airline = "THAI",
        flightNumber = "OFF",
        aircraftLabel = "OFF",
        aircraftFullName = "Day Off",
        registration = "—",
        status = "OFF",
        departureIata = "TLS",
        departureIcao = "LFBO",
        departureCity = "Toulouse",
        departureAirport = "NH Toulouse Airport",
        arrivalIata = "TLS",
        arrivalIcao = "LFBO",
        arrivalCity = "Toulouse",
        arrivalAirport = "NH Toulouse Airport",
        departureDateTime = date.atStartOfDay().toString(),
        arrivalDateTime = date.atTime(23, 59).toString(),
        durationMinutes = 0,
        dutyType = "OFF",
        dutyNote = note,
        rosterSource = "A380_TRANSITION_PROGRAM",
        eventGroupId = GROUP_ID,
        flightTimeCreditEligible = false
    )
}
