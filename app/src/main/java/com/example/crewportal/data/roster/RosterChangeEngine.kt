package com.example.crewportal.data.roster

import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.util.arrivalLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.random.Random

data class RosterChangeResult(
    val updatedRoster: List<FlightEntity>,
    val notificationTitle: String,
    val notificationBody: String,
    val notificationId: Int
)

object RosterChangeEngine {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Deterministically publishes only the number of changes scheduled up to [today]. It changes
     * future, unregistered turnaround sectors and returns a complete replacement snapshot; the
     * repository owns the database transaction-like clear/insert and notification side effects.
     */
    fun applyChangeIfDue(roster: List<FlightEntity>, today: LocalDate = LocalDate.now()): RosterChangeResult? {
        val month = YearMonth.from(today)
        val monthPrefix = "%04d-%02d".format(month.year, month.monthValue)
        val monthFlights = roster.filter { it.departureDateTime.startsWith(monthPrefix) }
        val targetChanges = monthlyChangeTarget(month)
        val allowedChangesToday = changeScheduleDays(month, targetChanges).count { it <= today.dayOfMonth }
        val alreadyChanged = monthFlights.count { it.changeNotified && it.dutyType == "FLIGHT" } / 2
        if (alreadyChanged >= allowedChangesToday) return null

        val pair = findTurnaroundPair(monthFlights, today) ?: return null
        val outbound = pair.first
        val inbound = pair.second
        val replacement = chooseReplacementRoute(outbound, month) ?: return null
        val departureDate = LocalDateTime.parse(outbound.departureDateTime, formatter).toLocalDate()
        val oldDepartureDate = LocalDateTime.parse(outbound.departureDateTime, formatter).toLocalDate()
        val possibleOffSwapDay = findFutureOffDay(monthFlights, today, oldDepartureDate)
        if (possibleOffSwapDay != null && abs((outbound.id + month + "off-swap").hashCode()) % 3 == 0) {
            val reason = changeReason(month, oldDepartureDate.dayOfMonth)
            val newOutboundDepartureSwap = LocalDateTime.parse("${possibleOffSwapDay}T${replacement.outboundTime}", formatter)
            val newOutboundArrivalSwap = arrivalLocalDateTime(newOutboundDepartureSwap, "BKK", replacement.iata, replacement.outboundMinutes)
            val newInboundDepartureSwap = newOutboundArrivalSwap.plusMinutes(replacement.turnaroundMinutes.toLong())
            val newInboundArrivalSwap = arrivalLocalDateTime(newInboundDepartureSwap, replacement.iata, "BKK", replacement.inboundMinutes)
            val offDuty = outbound.copy(
                id = "${oldDepartureDate}-OFF-ROSTER-CHANGE",
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
                departureDateTime = oldDepartureDate.atTime(0, 0).format(formatter),
                arrivalDateTime = oldDepartureDate.atTime(23, 59).format(formatter),
                durationMinutes = 0,
                dutyType = "OFF",
                dutyNote = "Roster change: ${outbound.flightNumber}/${inbound.flightNumber} removed and replaced by OFF. Reason: $reason",
                isRegistered = false,
                isCompleted = false,
                isFlightTimeAdded = false,
                registrationNotified = false,
                changeNotified = true,
                gate = "Pending",
                stand = "Pending",
                terminal = "Pending",
                airportAssignmentNotified = false
            )
            val swapOutbound = outbound.copy(
                id = "${possibleOffSwapDay}-${replacement.outboundFlight}-BKK-${replacement.iata}",
                flightNumber = replacement.outboundFlight,
                status = "CHANGED",
                departureIata = "BKK",
                departureIcao = "VTBS",
                departureCity = "Bangkok",
                departureAirport = "Suvarnabhumi Intl",
                arrivalIata = replacement.iata,
                arrivalIcao = replacement.icao,
                arrivalCity = replacement.city,
                arrivalAirport = replacement.airport,
                departureDateTime = newOutboundDepartureSwap.format(formatter),
                arrivalDateTime = newOutboundArrivalSwap.format(formatter),
                durationMinutes = replacement.outboundMinutes,
                dutyNote = "Roster change: OFF converted to ${replacement.outboundFlight} BKK-${replacement.iata}. Reason: $reason",
                registration = "TBA",
                isRegistered = false,
                isCompleted = false,
                isFlightTimeAdded = false,
                registrationNotified = false,
                changeNotified = true,
                gate = "Pending",
                stand = "Pending",
                terminal = "Pending",
                airportAssignmentNotified = false
            )
            val swapInbound = inbound.copy(
                id = "${possibleOffSwapDay}-${replacement.inboundFlight}-${replacement.iata}-BKK",
                flightNumber = replacement.inboundFlight,
                status = "CHANGED",
                departureIata = replacement.iata,
                departureIcao = replacement.icao,
                departureCity = replacement.city,
                departureAirport = replacement.airport,
                arrivalIata = "BKK",
                arrivalIcao = "VTBS",
                arrivalCity = "Bangkok",
                arrivalAirport = "Suvarnabhumi Intl",
                departureDateTime = newInboundDepartureSwap.format(formatter),
                arrivalDateTime = newInboundArrivalSwap.format(formatter),
                durationMinutes = replacement.inboundMinutes,
                dutyNote = "Roster change: OFF converted to ${replacement.inboundFlight} ${replacement.iata}-BKK. Reason: $reason",
                registration = "TBA",
                isRegistered = false,
                isCompleted = false,
                isFlightTimeAdded = false,
                registrationNotified = false,
                changeNotified = true,
                gate = "Pending",
                stand = "Pending",
                terminal = "Pending",
                airportAssignmentNotified = false
            )
            val updated = roster
                .filterNot { it.id == outbound.id || it.id == inbound.id || it.departureDateTime.startsWith(possibleOffSwapDay.toString()) && it.dutyType == "OFF" }
                .plus(listOf(offDuty, swapOutbound, swapInbound))
                .sortedBy { it.departureDateTime }
            val body = "${outbound.flightNumber}/${inbound.flightNumber} moved from ${oldDepartureDate.dayOfMonth} to ${possibleOffSwapDay.dayOfMonth} ${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} with route ${replacement.outboundFlight}/${replacement.inboundFlight} BKK-${replacement.iata}-BKK."
            return RosterChangeResult(updated, "Roster change published", body, ("roster-offswap-${month}-${oldDepartureDate}-${possibleOffSwapDay}".hashCode()))
        }

        val newOutboundDeparture = LocalDateTime.parse("${departureDate}T${replacement.outboundTime}", formatter)
        val newOutboundArrival = arrivalLocalDateTime(newOutboundDeparture, "BKK", replacement.iata, replacement.outboundMinutes)
        val newInboundDeparture = newOutboundArrival.plusMinutes(replacement.turnaroundMinutes.toLong())
        val newInboundArrival = arrivalLocalDateTime(newInboundDeparture, replacement.iata, "BKK", replacement.inboundMinutes)

        val newOutbound = outbound.copy(
            id = "${departureDate}-${replacement.outboundFlight}-BKK-${replacement.iata}",
            flightNumber = replacement.outboundFlight,
            status = "CHANGED",
            departureIata = "BKK",
            departureIcao = "VTBS",
            departureCity = "Bangkok",
            departureAirport = "Suvarnabhumi Intl",
            arrivalIata = replacement.iata,
            arrivalIcao = replacement.icao,
            arrivalCity = replacement.city,
            arrivalAirport = replacement.airport,
            departureDateTime = newOutboundDeparture.format(formatter),
            arrivalDateTime = newOutboundArrival.format(formatter),
            durationMinutes = replacement.outboundMinutes,
            dutyNote = "Roster change: ${outbound.flightNumber} ${outbound.departureIata}-${outbound.arrivalIata} replaced by ${replacement.outboundFlight} BKK-${replacement.iata}. Reason: ${changeReason(month, departureDate.dayOfMonth)}",
            registration = "TBA",
            isRegistered = false,
            registrationNotified = false,
            changeNotified = true,
            gate = "Pending",
            stand = "Pending",
            terminal = "Pending",
            airportAssignmentNotified = false
        )
        val newInbound = inbound.copy(
            id = "${departureDate}-${replacement.inboundFlight}-${replacement.iata}-BKK",
            flightNumber = replacement.inboundFlight,
            status = "CHANGED",
            departureIata = replacement.iata,
            departureIcao = replacement.icao,
            departureCity = replacement.city,
            departureAirport = replacement.airport,
            arrivalIata = "BKK",
            arrivalIcao = "VTBS",
            arrivalCity = "Bangkok",
            arrivalAirport = "Suvarnabhumi Intl",
            departureDateTime = newInboundDeparture.format(formatter),
            arrivalDateTime = newInboundArrival.format(formatter),
            durationMinutes = replacement.inboundMinutes,
            dutyNote = "Roster change: ${inbound.flightNumber} ${inbound.departureIata}-${inbound.arrivalIata} replaced by ${replacement.inboundFlight} ${replacement.iata}-BKK. Reason: ${changeReason(month, departureDate.dayOfMonth)}",
            registration = "TBA",
            isRegistered = false,
            registrationNotified = false,
            changeNotified = true,
            gate = "Pending",
            stand = "Pending",
            terminal = "Pending",
            airportAssignmentNotified = false
        )

        val updated = roster
            .filterNot { it.id == outbound.id || it.id == inbound.id }
            .plus(listOf(newOutbound, newInbound))
            .sortedBy { it.departureDateTime }

        val body = "${outbound.flightNumber}/${inbound.flightNumber} ${outbound.departureIata}-${outbound.arrivalIata}-${inbound.arrivalIata} changed to ${newOutbound.flightNumber}/${newInbound.flightNumber} BKK-${replacement.iata}-BKK on ${departureDate.dayOfMonth} ${month.month.name.lowercase().replaceFirstChar { it.uppercase() }}."
        return RosterChangeResult(
            updatedRoster = updated,
            notificationTitle = "Roster change published",
            notificationBody = body,
            notificationId = ("roster-change-${month}-${departureDate}-${replacement.iata}".hashCode())
        )
    }

    fun monthlyChangeTarget(month: YearMonth): Int = 1 + abs("${month.year}-${month.monthValue}-crew-change-target".hashCode()) % 5

    private fun changeScheduleDays(month: YearMonth, target: Int): List<Int> {
        val random = Random(month.toString().hashCode())
        val daysInMonth = month.lengthOfMonth()
        return (1..target).map { index ->
            val base = ((index.toDouble() / (target + 1)) * daysInMonth).toInt().coerceIn(4, daysInMonth - 2)
            (base + random.nextInt(-2, 3)).coerceIn(3, daysInMonth - 1)
        }.distinct().sorted()
    }

    private fun findTurnaroundPair(monthFlights: List<FlightEntity>, today: LocalDate): Pair<FlightEntity, FlightEntity>? {
        val future = monthFlights
            .filter { flight ->
                flight.dutyType == "FLIGHT" &&
                    !flight.isCompleted &&
                    !flight.isRegistered &&
                    !flight.changeNotified &&
                    LocalDateTime.parse(flight.departureDateTime, formatter).toLocalDate().isAfter(today.plusDays(1))
            }
            .sortedBy { it.departureDateTime }

        return future
            .filter { it.departureIata == "BKK" && it.durationMinutes in 70..180 }
            .mapNotNull { outbound ->
                val outDate = LocalDateTime.parse(outbound.departureDateTime, formatter).toLocalDate()
                val inbound = future.firstOrNull { candidate ->
                    candidate.id != outbound.id &&
                        candidate.departureIata == outbound.arrivalIata &&
                        candidate.arrivalIata == "BKK" &&
                        candidate.aircraftLabel == outbound.aircraftLabel &&
                        LocalDateTime.parse(candidate.departureDateTime, formatter).toLocalDate() == outDate
                }
                inbound?.let { outbound to it }
            }
            .firstOrNull()
    }

    private fun findFutureOffDay(monthFlights: List<FlightEntity>, today: LocalDate, oldDate: LocalDate): LocalDate? {
        return monthFlights
            .filter { it.dutyType == "OFF" && !it.changeNotified }
            .map { LocalDateTime.parse(it.departureDateTime, formatter).toLocalDate() }
            .filter { it.isAfter(today.plusDays(2)) && it != oldDate }
            .sorted()
            .firstOrNull()
    }

    private fun chooseReplacementRoute(outbound: FlightEntity, month: YearMonth): RouteOption? {
        val currentDestination = outbound.arrivalIata
        val seed = (outbound.id + month.toString()).hashCode()
        return routeOptions.filterNot { it.iata == currentDestination }.shuffled(Random(seed)).firstOrNull()
    }

    private fun changeReason(month: YearMonth, day: Int): String {
        val reasons = listOf(
            "aircraft rotation optimization",
            "network capacity adjustment",
            "commercial schedule update",
            "operational slot adjustment",
            "fleet planning update"
        )
        return reasons[abs("$month-$day-reason".hashCode()) % reasons.size]
    }

    private data class RouteOption(
        val iata: String,
        val icao: String,
        val city: String,
        val airport: String,
        val outboundFlight: String,
        val inboundFlight: String,
        val outboundTime: String,
        val outboundMinutes: Int,
        val turnaroundMinutes: Int,
        val inboundMinutes: Int
    )

    private val routeOptions = listOf(
        RouteOption("CNX", "VTCC", "Chiang Mai", "Chiang Mai Intl", "TG104", "TG105", "08:20:00", 80, 60, 85),
        RouteOption("HDY", "VTSS", "Hat Yai", "Hat Yai Intl", "TG235", "TG236", "09:05:00", 95, 65, 100),
        RouteOption("KBV", "VTSG", "Krabi", "Krabi Intl", "TG249", "TG250", "10:10:00", 85, 65, 90),
        RouteOption("SGN", "VVTS", "Ho Chi Minh City", "Tan Son Nhat Intl", "TG550", "TG551", "07:45:00", 105, 70, 105),
        RouteOption("HAN", "VVNB", "Hanoi", "Noi Bai Intl", "TG560", "TG561", "08:50:00", 115, 75, 120),
        RouteOption("PEN", "WMKP", "Penang", "Penang Intl", "TG425", "TG426", "11:00:00", 105, 70, 110)
    )
}
