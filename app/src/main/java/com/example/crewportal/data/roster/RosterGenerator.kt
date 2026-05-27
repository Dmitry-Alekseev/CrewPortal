package com.example.crewportal.data.roster

import com.example.crewportal.data.local.FlightEntity
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object RosterGenerator {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun generateJune2026(): List<FlightEntity> = generateForMonth(YearMonth.of(2026, 6))

    fun generateForMonth(month: YearMonth): List<FlightEntity> {
        val flights = mutableListOf<FlightEntity>()
        val daysInMonth = month.lengthOfMonth()

        fun date(day: Int): String = "%04d-%02d-%02d".format(month.year, month.monthValue, day)
        fun dt(day: Int, time: String): String = LocalDateTime.parse("${date(day)}T$time", formatter).format(formatter)
        fun dt(date: String, time: String): String = LocalDateTime.parse("${date}T$time", formatter).format(formatter)

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
                departureDateTime = dt(d, "00:00:00"),
                arrivalDateTime = dt(d, "23:59:00"),
                durationMinutes = 0,
                dutyType = "OFF",
                dutyNote = "Day off"
            )
        }

        fun removeOffFor(vararg days: Int) {
            days.forEach { day -> flights.removeAll { it.id == "${date(day)}-OFF" } }
        }

        fun addFlight(
            day: Int,
            fn: String,
            dep: String,
            arr: String,
            depCity: String,
            arrCity: String,
            depIcao: String,
            arrIcao: String,
            depTime: String,
            mins: Int,
            ac: String,
            acFull: String,
            note: String = ""
        ) {
            val depDt = LocalDateTime.parse("${date(day)}T$depTime", formatter)
            val arrDt = depDt.plusMinutes(mins.toLong())
            flights += FlightEntity(
                id = "${date(day)}-$fn-$dep-$arr",
                airline = "THAI",
                flightNumber = fn,
                aircraftLabel = ac,
                aircraftFullName = acFull,
                registration = "TBA",
                status = "SCHEDULED",
                departureIata = dep,
                departureIcao = depIcao,
                departureCity = depCity,
                departureAirport = airportName(dep),
                arrivalIata = arr,
                arrivalIcao = arrIcao,
                arrivalCity = arrCity,
                arrivalAirport = airportName(arr),
                departureDateTime = depDt.format(formatter),
                arrivalDateTime = arrDt.format(formatter),
                durationMinutes = mins,
                dutyType = "FLIGHT",
                dutyNote = note
            )
        }

        fun addStay(day: Int, iata: String, icao: String, city: String, hotel: String) {
            flights += FlightEntity(
                id = "${date(day)}-STAY-$iata",
                airline = "THAI",
                flightNumber = "STAY AT $iata",
                aircraftLabel = "STAY",
                aircraftFullName = "Layover stay",
                registration = "—",
                status = "STAY",
                departureIata = iata,
                departureIcao = icao,
                departureCity = city,
                departureAirport = hotel,
                arrivalIata = iata,
                arrivalIcao = icao,
                arrivalCity = city,
                arrivalAirport = hotel,
                departureDateTime = dt(day, "00:00:00"),
                arrivalDateTime = dt(day, "23:59:00"),
                durationMinutes = 0,
                dutyType = "STAY",
                dutyNote = hotel
            )
        }

        fun addReserve(day: Int) {
            flights += FlightEntity(
                id = "${date(day)}-HOTEL-RESERVE",
                airline = "THAI",
                flightNumber = "HOTEL RESERVE",
                aircraftLabel = "RES",
                aircraftFullName = "Hotel Reserve",
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
                departureDateTime = dt(day, "08:00:00"),
                arrivalDateTime = dt(day, "20:00:00"),
                durationMinutes = 0,
                dutyType = "RESERVE",
                dutyNote = "Hotel reserve, Hyatt Regency Bangkok Suvarnabhumi Airport"
            )
        }

        (1..daysInMonth).forEach { addOff(it) }

        fun apply(vararg days: Int, block: () -> Unit) {
            removeOffFor(*days)
            block()
        }

        // Balanced 80-hour monthly plan. It deliberately spreads work through the month,
        // avoids long blocks of OFF days, and stays close to the 80h target without JSON.
        if (daysInMonth >= 24) {
            apply(1) {
                addFlight(1, "TG201", "BKK", "HKT", "Bangkok", "Phuket", "VTBS", "VTSP", "07:45:00", 85, "A320", "Airbus A320-214")
                addFlight(1, "TG202", "HKT", "BKK", "Phuket", "Bangkok", "VTSP", "VTBS", "10:35:00", 90, "A320", "Airbus A320-214")
            }
            apply(3) {
                addFlight(3, "TG403", "BKK", "SIN", "Bangkok", "Singapore", "VTBS", "WSSS", "08:00:00", 150, "A321neo", "Airbus A321-251NX")
                addFlight(3, "TG404", "SIN", "BKK", "Singapore", "Bangkok", "WSSS", "VTBS", "12:35:00", 150, "A321neo", "Airbus A321-251NX")
            }
            apply(5, 6, 7, 8) {
                addFlight(5, "TG684", "BKK", "TAS", "Bangkok", "Tashkent", "VTBS", "UTTT", "09:20:00", 395, "A330", "Airbus A330-343", "Tashkent layover")
                addStay(6, "TAS", "UTTT", "Tashkent", "Hyatt Regency Tashkent")
                addStay(7, "TAS", "UTTT", "Tashkent", "Hyatt Regency Tashkent")
                addFlight(8, "TG685", "TAS", "BKK", "Tashkent", "Bangkok", "UTTT", "VTBS", "13:45:00", 405, "A330", "Airbus A330-343", "Tashkent return")
            }
            apply(10) {
                addFlight(10, "TG415", "BKK", "KUL", "Bangkok", "Kuala Lumpur", "VTBS", "WMKK", "09:05:00", 145, "A320", "Airbus A320-214")
                addFlight(10, "TG416", "KUL", "BKK", "Kuala Lumpur", "Bangkok", "WMKK", "VTBS", "13:10:00", 145, "A320", "Airbus A320-214")
            }
            apply(12, 13, 14) {
                addFlight(12, "TG920", "BKK", "FRA", "Bangkok", "Frankfurt", "VTBS", "EDDF", "23:20:00", 690, "A350", "Airbus A350-941", "Long-haul augmented crew")
                addStay(13, "FRA", "EDDF", "Frankfurt", "JW Marriott Hotel Frankfurt")
                addFlight(14, "TG921", "FRA", "BKK", "Frankfurt", "Bangkok", "EDDF", "VTBS", "13:45:00", 675, "A350", "Airbus A350-941", "Long-haul return")
            }
            apply(16) {
                addFlight(16, "TG331", "BKK", "DEL", "Bangkok", "Delhi", "VTBS", "VIDP", "08:20:00", 265, "A330", "Airbus A330-343")
                addFlight(16, "TG332", "DEL", "BKK", "Delhi", "Bangkok", "VIDP", "VTBS", "14:00:00", 260, "A330", "Airbus A330-343")
            }
            apply(20, 21, 22) {
                addFlight(20, "TG935", "BKK", "IST", "Bangkok", "Istanbul", "VTBS", "LTFM", "22:40:00", 605, "A350", "Airbus A350-941", "Layover, Grand Hyatt Istanbul")
                addStay(21, "IST", "LTFM", "Istanbul", "Grand Hyatt Istanbul")
                addFlight(22, "TG936", "IST", "BKK", "Istanbul", "Bangkok", "LTFM", "VTBS", "10:15:00", 580, "A350", "Airbus A350-941", "Layover return")
            }
            apply(24) {
                addFlight(24, "TG221", "BKK", "HKT", "Bangkok", "Phuket", "VTBS", "VTSP", "08:10:00", 85, "A320", "Airbus A320-214")
                addFlight(24, "TG222", "HKT", "BKK", "Phuket", "Bangkok", "VTSP", "VTBS", "11:05:00", 90, "A320", "Airbus A320-214")
            }
            if (daysInMonth >= 27) apply(27) { addReserve(27) }
        }

        return flights.sortedBy { it.departureDateTime }
    }

    private fun airportName(iata: String): String = when (iata) {
        "BKK" -> "Suvarnabhumi Intl"
        "HKT" -> "Phuket Intl"
        "CXR" -> "Cam Ranh Intl"
        "SIN" -> "Changi Intl"
        "KUL" -> "KLIA"
        "DEL" -> "Indira Gandhi Intl"
        "FRA" -> "Frankfurt Main"
        "IST" -> "Istanbul Airport"
        "TAS" -> "Tashkent Intl"
        "SVO" -> "Sheremetyevo Intl"
        else -> "$iata Airport"
    }
}
