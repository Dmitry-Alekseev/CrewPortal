package com.example.crewportal.data.roster

import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.airport.CrewHotelDirectory
import com.example.crewportal.util.arrivalLocalDateTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Single source of truth for the two published TAS rotation patterns. */
internal object TashkentRotationFactory {
    private const val OUTBOUND_MINUTES = 395
    private const val INBOUND_MINUTES = 405
    private const val TURNAROUND_MINUTES = 125
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /** Thursday operating outbound, Friday/Saturday hotel stay, Sunday operating return. */
    fun thursdayStaySundayReturn(thursday: LocalDate): List<FlightEntity> {
        require(thursday.dayOfWeek == DayOfWeek.THURSDAY) { "TAS stay rotation must start on Thursday" }
        val outboundDeparture = thursday.atTime(9, 20)
        val outboundArrival = arrivalLocalDateTime(outboundDeparture, "BKK", "TAS", OUTBOUND_MINUTES)
        val sunday = thursday.plusDays(3)
        val returnDeparture = sunday.atTime(13, 45)
        val returnArrival = arrivalLocalDateTime(returnDeparture, "TAS", "BKK", INBOUND_MINUTES)

        return buildList {
            add(operatingOutbound(outboundDeparture, outboundArrival))
            add(stay(thursday.plusDays(1)))
            add(stay(thursday.plusDays(2)))
            add(operatingReturn(returnDeparture, returnArrival))
        }
    }

    /** Sunday operating arrival in TAS followed by a same-Sunday deadhead departure to BKK. */
    fun sundaySameDayDeadhead(sunday: LocalDate): List<FlightEntity> {
        require(sunday.dayOfWeek == DayOfWeek.SUNDAY) { "TAS deadhead rotation must start on Sunday" }
        val outboundDeparture = sunday.atTime(9, 20)
        val outboundArrival = arrivalLocalDateTime(outboundDeparture, "BKK", "TAS", OUTBOUND_MINUTES)
        val deadheadDeparture = outboundArrival.plusMinutes(TURNAROUND_MINUTES.toLong())
        val deadheadArrival = arrivalLocalDateTime(deadheadDeparture, "TAS", "BKK", INBOUND_MINUTES)

        return listOf(
            operatingOutbound(outboundDeparture, outboundArrival),
            FlightEntity(
                id = "${sunday}-TG685-TAS-BKK-DH",
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
                arrivalAirport = "Suvarnabhumi Intl",
                departureDateTime = deadheadDeparture.format(formatter),
                arrivalDateTime = deadheadArrival.format(formatter),
                durationMinutes = 0,
                dutyType = "DEADHEAD",
                dutyNote = "Passenger return to Bangkok, no layover"
            )
        )
    }

    private fun operatingOutbound(
        departure: LocalDateTime,
        arrival: LocalDateTime
    ) = FlightEntity(
        id = "${departure.toLocalDate()}-TG684-BKK-TAS",
        airline = "THAI",
        flightNumber = "TG684",
        aircraftLabel = "A330",
        aircraftFullName = "Airbus A330-343",
        registration = "TBA",
        status = "SCHEDULED",
        departureIata = "BKK",
        departureIcao = "VTBS",
        departureCity = "Bangkok",
        departureAirport = "Suvarnabhumi Intl",
        arrivalIata = "TAS",
        arrivalIcao = "UTTT",
        arrivalCity = "Tashkent",
        arrivalAirport = "Islam Karimov",
        departureDateTime = departure.format(formatter),
        arrivalDateTime = arrival.format(formatter),
        durationMinutes = OUTBOUND_MINUTES,
        dutyType = "FLIGHT",
        dutyNote = "Scheduled flight"
    )

    private fun operatingReturn(departure: LocalDateTime, arrival: LocalDateTime) = FlightEntity(
        id = "${departure.toLocalDate()}-TG685-TAS-BKK",
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
        arrivalAirport = "Suvarnabhumi Intl",
        departureDateTime = departure.format(formatter),
        arrivalDateTime = arrival.format(formatter),
        durationMinutes = INBOUND_MINUTES,
        dutyType = "FLIGHT",
        dutyNote = "Scheduled flight"
    )

    private fun stay(date: LocalDate) = FlightEntity(
        id = "$date-STAY-TAS",
        airline = "THAI",
        flightNumber = "Stay in Tashkent",
        aircraftLabel = "STAY",
        aircraftFullName = "Layover stay",
        registration = "—",
        status = "STAY",
        departureIata = "TAS",
        departureIcao = "UTTT",
        departureCity = "Tashkent",
        departureAirport = CrewHotelDirectory.hotelFor("TAS"),
        arrivalIata = "TAS",
        arrivalIcao = "UTTT",
        arrivalCity = "Tashkent",
        arrivalAirport = CrewHotelDirectory.hotelFor("TAS"),
        departureDateTime = date.atStartOfDay().format(formatter),
        arrivalDateTime = date.atTime(LocalTime.of(23, 59)).format(formatter),
        durationMinutes = 0,
        dutyType = "STAY",
        dutyNote = CrewHotelDirectory.hotelFor("TAS")
    )
}
