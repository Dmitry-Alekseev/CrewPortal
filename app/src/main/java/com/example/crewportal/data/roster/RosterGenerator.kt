package com.example.crewportal.data.roster

import com.example.crewportal.data.local.FlightEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object RosterGenerator {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun generateJune2026(): List<FlightEntity> {
        val flights = mutableListOf<FlightEntity>()
        fun dt(date: String, time: String): String = LocalDateTime.parse("${date}T$time", formatter).format(formatter)
        fun addOff(date: String) {
            flights += FlightEntity(
                id = "$date-OFF", airline = "THAI", flightNumber = "OFF", aircraftLabel = "OFF", aircraftFullName = "Day Off", registration = "—", status = "OFF",
                departureIata = "BKK", departureIcao = "VTBS", departureCity = "Bangkok", departureAirport = "Suvarnabhumi Intl",
                arrivalIata = "BKK", arrivalIcao = "VTBS", arrivalCity = "Bangkok", arrivalAirport = "Suvarnabhumi Intl",
                departureDateTime = dt(date, "00:00:00"), arrivalDateTime = dt(date, "23:59:00"), durationMinutes = 0, dutyType = "OFF", dutyNote = "Generated roster day off"
            )
        }
        fun addFlight(date: String, fn: String, dep: String, arr: String, depCity: String, arrCity: String, depIcao: String, arrIcao: String, depTime: String, mins: Int, ac: String, acFull: String, note: String = "") {
            val depDt = LocalDateTime.parse("${date}T$depTime", formatter)
            val arrDt = depDt.plusMinutes(mins.toLong())
            flights += FlightEntity(
                id = "$date-$fn-$dep-$arr", airline = "THAI", flightNumber = fn, aircraftLabel = ac, aircraftFullName = acFull, registration = "TBA", status = "SCHEDULED",
                departureIata = dep, departureIcao = depIcao, departureCity = depCity, departureAirport = airportName(dep),
                arrivalIata = arr, arrivalIcao = arrIcao, arrivalCity = arrCity, arrivalAirport = airportName(arr),
                departureDateTime = depDt.format(formatter), arrivalDateTime = arrDt.format(formatter), durationMinutes = mins, dutyType = "FLIGHT", dutyNote = note
            )
        }
        fun addReserve(date: String) {
            flights += FlightEntity(
                id = "$date-HOTEL-RESERVE", airline = "THAI", flightNumber = "HOTEL RESERVE", aircraftLabel = "RES", aircraftFullName = "Hotel Reserve", registration = "—", status = "RESERVE",
                departureIata = "BKK", departureIcao = "VTBS", departureCity = "Bangkok", departureAirport = "Hyatt Regency Bangkok Suvarnabhumi Airport",
                arrivalIata = "BKK", arrivalIcao = "VTBS", arrivalCity = "Bangkok", arrivalAirport = "Hyatt Regency Bangkok Suvarnabhumi Airport",
                departureDateTime = dt(date, "08:00:00"), arrivalDateTime = dt(date, "20:00:00"), durationMinutes = 0, dutyType = "RESERVE", dutyNote = "Hotel reserve, Hyatt Regency Bangkok Suvarnabhumi Airport"
            )
        }

        (1..30).forEach { addOff("2026-06-%02d".format(it)) }
        // Replace selected generated days with realistic duties; total planned block around 73-78h.
        flights.removeAll { it.id.startsWith("2026-06-02") }
        addFlight("2026-06-02", "TG221", "BKK", "HKT", "Bangkok", "Phuket", "VTBS", "VTSP", "08:10:00", 85, "A321neo", "Airbus A321-251NX")
        addFlight("2026-06-02", "TG222", "HKT", "BKK", "Phuket", "Bangkok", "VTSP", "VTBS", "11:05:00", 90, "A321neo", "Airbus A321-251NX")

        flights.removeAll { it.id.startsWith("2026-06-04") || it.id.startsWith("2026-06-05") || it.id.startsWith("2026-06-06") || it.id.startsWith("2026-06-07") }
        addFlight("2026-06-04", "TG684", "BKK", "TAS", "Bangkok", "Tashkent", "VTBS", "UTTT", "09:20:00", 395, "A330", "Airbus A330-343", "Tashkent Thursday layover")
        addReserve("2026-06-05")
        addOff("2026-06-06")
        addFlight("2026-06-07", "TG685", "TAS", "BKK", "Tashkent", "Bangkok", "UTTT", "VTBS", "13:45:00", 405, "A330", "Airbus A330-343", "Tashkent Sunday return")

        flights.removeAll { it.id.startsWith("2026-06-09") }
        addFlight("2026-06-09", "TG403", "BKK", "SIN", "Bangkok", "Singapore", "VTBS", "WSSS", "08:00:00", 150, "A321neo", "Airbus A321-251NX")
        addFlight("2026-06-09", "TG404", "SIN", "BKK", "Singapore", "Bangkok", "WSSS", "VTBS", "12:35:00", 150, "A321neo", "Airbus A321-251NX")

        flights.removeAll { it.id.startsWith("2026-06-11") || it.id.startsWith("2026-06-12") || it.id.startsWith("2026-06-13") }
        addFlight("2026-06-11", "TG920", "BKK", "FRA", "Bangkok", "Frankfurt", "VTBS", "EDDF", "23:20:00", 690, "A350", "Airbus A350-941", "Long-haul augmented crew")
        addReserve("2026-06-12")
        addFlight("2026-06-13", "TG921", "FRA", "BKK", "Frankfurt", "Bangkok", "EDDF", "VTBS", "13:45:00", 675, "A350", "Airbus A350-941", "Long-haul return")

        flights.removeAll { it.id.startsWith("2026-06-17") }
        addFlight("2026-06-17", "TG557", "BKK", "CXR", "Bangkok", "Nha Trang", "VTBS", "VVCR", "08:25:00", 100, "A321neo", "Airbus A321-251NX")
        addFlight("2026-06-17", "TG558", "CXR", "BKK", "Nha Trang", "Bangkok", "VVCR", "VTBS", "11:35:00", 105, "A321neo", "Airbus A321-251NX")

        flights.removeAll { it.id.startsWith("2026-06-20") || it.id.startsWith("2026-06-21") || it.id.startsWith("2026-06-22") }
        addFlight("2026-06-20", "TG935", "BKK", "IST", "Bangkok", "Istanbul", "VTBS", "LTFM", "22:40:00", 605, "A350", "Airbus A350-941", "Layover, Grand Hyatt Istanbul")
        addReserve("2026-06-21")
        addFlight("2026-06-22", "TG936", "IST", "BKK", "Istanbul", "Bangkok", "LTFM", "VTBS", "10:15:00", 580, "A350", "Airbus A350-941", "Layover return")

        flights.removeAll { it.id.startsWith("2026-06-25") }
        addFlight("2026-06-25", "TG201", "BKK", "HKT", "Bangkok", "Phuket", "VTBS", "VTSP", "07:55:00", 85, "A321neo", "Airbus A321-251NX")
        addFlight("2026-06-25", "TG202", "HKT", "BKK", "Phuket", "Bangkok", "VTSP", "VTBS", "10:45:00", 90, "A321neo", "Airbus A321-251NX")

        flights.removeAll { it.id.startsWith("2026-06-28") }
        addFlight("2026-06-28", "TG687", "BKK", "TAS", "Bangkok", "Tashkent", "VTBS", "UTTT", "08:30:00", 395, "A330", "Airbus A330-343", "Sunday Tashkent outbound, passenger return planned")

        return flights.sortedBy { it.departureDateTime }
    }

    private fun airportName(iata: String): String = when (iata) {
        "BKK" -> "Suvarnabhumi Intl"
        "HKT" -> "Phuket Intl"
        "CXR" -> "Cam Ranh Intl"
        "SIN" -> "Changi Intl"
        "FRA" -> "Frankfurt Main"
        "IST" -> "Istanbul Airport"
        "TAS" -> "Tashkent Intl"
        else -> "$iata Airport"
    }
}
