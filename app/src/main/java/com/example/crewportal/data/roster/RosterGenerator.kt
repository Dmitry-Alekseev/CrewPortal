package com.example.crewportal.data.roster

import com.example.crewportal.data.local.FlightEntity
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object RosterGenerator {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun generateJune2026(): List<FlightEntity> = generateForMonth(YearMonth.of(2026, 6))

    fun generateForMonth(month: YearMonth): List<FlightEntity> {
        val flights = mutableListOf<FlightEntity>()
        val occupied = mutableSetOf<String>()
        val random = Random(System.currentTimeMillis())
        val daysInMonth = month.lengthOfMonth()

        fun date(day: Int): String = "%04d-%02d-%02d".format(month.year, month.monthValue, day)
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
                dutyNote = "Generated roster day off"
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
            val d = date(day)
            val depDt = LocalDateTime.parse("${d}T$depTime", formatter)
            val arrDt = depDt.plusMinutes(mins.toLong())
            flights += FlightEntity(
                id = "$d-$fn-$dep-$arr",
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
            val d = date(day)
            flights += FlightEntity(
                id = "$d-STAY-$iata",
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
                departureDateTime = dt(d, "00:00:00"),
                arrivalDateTime = dt(d, "23:59:00"),
                durationMinutes = 0,
                dutyType = "STAY",
                dutyNote = hotel
            )
        }

        fun addReserve(day: Int) {
            val d = date(day)
            flights += FlightEntity(
                id = "$d-HOTEL-RESERVE",
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
                departureDateTime = dt(d, "08:00:00"),
                arrivalDateTime = dt(d, "20:00:00"),
                durationMinutes = 0,
                dutyType = "RESERVE",
                dutyNote = "Hotel reserve, Hyatt Regency Bangkok Suvarnabhumi Airport"
            )
        }

        (1..daysInMonth).forEach { addOff(it) }

        data class Pattern(val name: String, val days: Set<Int>, val minutes: Int, val add: () -> Unit)
        fun apply(pattern: Pattern): Boolean {
            val dayKeys = pattern.days.map { date(it) }
            if (dayKeys.any { it in occupied }) return false
            dayKeys.forEach { occupied += it }
            removeOffFor(*pattern.days.toIntArray())
            pattern.add()
            return true
        }

        val patterns = mutableListOf(
            Pattern("HKT", setOf(2), 175) {
                addFlight(2, "TG221", "BKK", "HKT", "Bangkok", "Phuket", "VTBS", "VTSP", "08:10:00", 85, "A321neo", "Airbus A321-251NX")
                addFlight(2, "TG222", "HKT", "BKK", "Phuket", "Bangkok", "VTSP", "VTBS", "11:05:00", 90, "A321neo", "Airbus A321-251NX")
            },
            Pattern("SIN", setOf(9), 300) {
                addFlight(9, "TG403", "BKK", "SIN", "Bangkok", "Singapore", "VTBS", "WSSS", "08:00:00", 150, "A321neo", "Airbus A321-251NX")
                addFlight(9, "TG404", "SIN", "BKK", "Singapore", "Bangkok", "WSSS", "VTBS", "12:35:00", 150, "A321neo", "Airbus A321-251NX")
            },
            Pattern("CXR", setOf(17), 205) {
                addFlight(17, "TG557", "BKK", "CXR", "Bangkok", "Nha Trang", "VTBS", "VVCR", "08:25:00", 100, "A321neo", "Airbus A321-251NX")
                addFlight(17, "TG558", "CXR", "BKK", "Nha Trang", "Bangkok", "VVCR", "VTBS", "11:35:00", 105, "A321neo", "Airbus A321-251NX")
            },
            Pattern("KUL", setOf(24), 290) {
                addFlight(24, "TG415", "BKK", "KUL", "Bangkok", "Kuala Lumpur", "VTBS", "WMKK", "09:05:00", 145, "A321neo", "Airbus A321-251NX")
                addFlight(24, "TG416", "KUL", "BKK", "Kuala Lumpur", "Bangkok", "WMKK", "VTBS", "13:10:00", 145, "A321neo", "Airbus A321-251NX")
            },
            Pattern("FRA", setOf(11, 12, 13), 1365) {
                addFlight(11, "TG920", "BKK", "FRA", "Bangkok", "Frankfurt", "VTBS", "EDDF", "23:20:00", 690, "A350", "Airbus A350-941", "Long-haul augmented crew")
                addStay(12, "FRA", "EDDF", "Frankfurt", "JW Marriott Hotel Frankfurt")
                addFlight(13, "TG921", "FRA", "BKK", "Frankfurt", "Bangkok", "EDDF", "VTBS", "13:45:00", 675, "A350", "Airbus A350-941", "Long-haul return")
            },
            Pattern("IST", setOf(20, 21, 22), 1185) {
                addFlight(20, "TG935", "BKK", "IST", "Bangkok", "Istanbul", "VTBS", "LTFM", "22:40:00", 605, "A350", "Airbus A350-941", "Layover, Grand Hyatt Istanbul")
                addStay(21, "IST", "LTFM", "Istanbul", "Grand Hyatt Istanbul")
                addFlight(22, "TG936", "IST", "BKK", "Istanbul", "Bangkok", "LTFM", "VTBS", "10:15:00", 580, "A350", "Airbus A350-941", "Layover return")
            },
            Pattern("TAS", setOf(4, 5, 6, 7), 800) {
                addFlight(4, "TG684", "BKK", "TAS", "Bangkok", "Tashkent", "VTBS", "UTTT", "09:20:00", 395, "A330", "Airbus A330-343", "Tashkent layover")
                addStay(5, "TAS", "UTTT", "Tashkent", "Hyatt Regency Tashkent")
                addStay(6, "TAS", "UTTT", "Tashkent", "Hyatt Regency Tashkent")
                addFlight(7, "TG685", "TAS", "BKK", "Tashkent", "Bangkok", "UTTT", "VTBS", "13:45:00", 405, "A330", "Airbus A330-343", "Tashkent return")
            },
            Pattern("HKT2", setOf(25), 175) {
                addFlight(25, "TG201", "BKK", "HKT", "Bangkok", "Phuket", "VTBS", "VTSP", "07:55:00", 85, "A321neo", "Airbus A321-251NX")
                addFlight(25, "TG202", "HKT", "BKK", "Phuket", "Bangkok", "VTSP", "VTBS", "10:45:00", 90, "A321neo", "Airbus A321-251NX")
            },
            Pattern("SVO", setOf(27, 28, 29), 1180) {
                addFlight(27, "TG892", "BKK", "SVO", "Bangkok", "Moscow", "VTBS", "UUEE", "23:10:00", 590, "A350", "Airbus A350-941", "Long-haul layover")
                addStay(28, "SVO", "UUEE", "Moscow", "Hyatt Regency Moscow Petrovsky Park")
                addFlight(29, "TG893", "SVO", "BKK", "Moscow", "Bangkok", "UUEE", "VTBS", "12:20:00", 590, "A350", "Airbus A350-941", "Long-haul return")
            }
        )

        val shuffled = patterns.shuffled(random)
        var total = 0
        shuffled.forEach { pattern ->
            val maxTarget = 78 * 60
            val minTarget = 70 * 60
            if (total < minTarget && total + pattern.minutes <= maxTarget) {
                if (apply(pattern)) total += pattern.minutes
            }
        }

        if (total < 70 * 60) {
            patterns.filter { it.name in setOf("HKT", "SIN", "CXR", "KUL", "HKT2") }.shuffled(random).forEach { pattern ->
                if (total < 70 * 60 && total + pattern.minutes <= 80 * 60) {
                    if (apply(pattern)) total += pattern.minutes
                }
            }
        }

        if (random.nextInt(100) < 35) {
            listOf(15, 18, 26).filter { it <= daysInMonth }.shuffled(random).take(1).forEach { day ->
                val key = date(day)
                if (key !in occupied) {
                    occupied += key
                    removeOffFor(day)
                    addReserve(day)
                }
            }
        }

        return flights.sortedBy { it.departureDateTime }
    }

    private fun airportName(iata: String): String = when (iata) {
        "BKK" -> "Suvarnabhumi Intl"
        "HKT" -> "Phuket Intl"
        "CXR" -> "Cam Ranh Intl"
        "SIN" -> "Changi Intl"
        "KUL" -> "Kuala Lumpur Intl"
        "FRA" -> "Frankfurt Main"
        "IST" -> "Istanbul Airport"
        "TAS" -> "Tashkent Intl"
        "SVO" -> "Sheremetyevo Intl"
        else -> "$iata Airport"
    }
}
