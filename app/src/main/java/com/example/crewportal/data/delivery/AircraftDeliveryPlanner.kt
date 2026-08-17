package com.example.crewportal.data.delivery

import com.example.crewportal.data.airport.AirportDatabase
import com.example.crewportal.data.fleet.AircraftTypeCatalog
import com.example.crewportal.data.local.DutyType
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.util.arrivalLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class AircraftDeliveryRequest(
    val deliveryDate: LocalDate,
    val deliveryFlightNumber: String,
    val aircraftLabel: String,
    val registration: String
)

data class AircraftDeliveryPlan(
    val rows: List<FlightEntity>,
    val intermediateIata: String,
    val flightCrewSize: Int,
    val stopMinutes: Int
)

/**
 * Builds the complete delivery duty from four user inputs. The result is deterministic, so
 * reopening or retrying the same request cannot silently produce another route or crew plan.
 *
 * Commercial positioning uses a curated BKK-DXB-HAM Emirates pattern based on the public
 * timetable. It deliberately remains an offline fallback: operational control must verify the
 * live booking before travel, but loss of network access never prevents roster creation.
 * The EDDH-EDHI movement is intentionally not persisted because it is implicit in reporting
 * instructions and would overload the roster.
 */
object AircraftDeliveryPlanner {
    private data class TechnicalStop(
        val iata: String,
        val firstLegMinutes: Int,
        val secondLegMinutes: Int
    )

    private val technicalStops = listOf(
        TechnicalStop("GYD", 285, 385),
        TechnicalStop("DWC", 395, 380),
        TechnicalStop("DOH", 385, 405),
        TechnicalStop("MCT", 420, 365)
    )

    fun build(request: AircraftDeliveryRequest): AircraftDeliveryPlan {
        require(request.aircraftLabel in AircraftTypeCatalog.deliveryTypes) { "Unsupported delivery aircraft type" }
        require(request.deliveryFlightNumber.isNotBlank()) { "Delivery flight number is required" }

        val registration = request.registration.trim().uppercase()
        require(Regex("^HS-[A-Z0-9]{2,6}$").matches(registration)) { "Registration must use HS- plus 2-6 letters/digits" }
        val spec = AircraftTypeCatalog.byLabel(request.aircraftLabel)
        val seed = "${request.deliveryDate}|${request.deliveryFlightNumber}|${request.aircraftLabel}|$registration".hashCode()
        val stop = technicalStops[Math.floorMod(seed, technicalStops.size)]
        val fourPilots = request.aircraftLabel.startsWith("A330") || request.aircraftLabel.startsWith("A350")
        val crewSize = if (fourPilots) 4 else 2
        val stopMinutes = if (fourPilots) {
            120 + Math.floorMod(seed / 7, 121) // 2h00-4h00 fuel/technical stop.
        } else {
            480 + Math.floorMod(seed / 11, 241) // 8h00-12h00 mandatory crew rest.
        }
        val userOperatesFirst = !fourPilots || Math.floorMod(seed / 13, 2) == 0

        val positioningDate = request.deliveryDate.minusDays(2)
        val bkkDeparture = LocalDateTime.of(positioningDate, LocalTime.of(9, 55))
        val dxbArrival = arrivalLocalDateTime(bkkDeparture, "BKK", "DXB", 415)
        val dxbDeparture = dxbArrival.plusMinutes(110)
        val hamArrival = arrivalLocalDateTime(dxbDeparture, "DXB", "HAM", 390)

        val ferryDeparture = LocalDateTime.of(request.deliveryDate, LocalTime.of(8, 30))
        val firstArrival = arrivalLocalDateTime(ferryDeparture, "XFW", stop.iata, stop.firstLegMinutes)
        val secondDeparture = firstArrival.plusMinutes(stopMinutes.toLong())
        val finalArrival = arrivalLocalDateTime(secondDeparture, stop.iata, "BKK", stop.secondLegMinutes)

        val positioningNote = "Passenger positioning to Hamburg • Emirates public timetable reference • Verify live booking • EDDH-EDHI transfer omitted"
        val crewPlan = if (fourPilots) {
            "4 pilots • 2-4h refuel stop • User: leg 1 ${role(userOperatesFirst)}, leg 2 ${role(!userOperatesFirst)}"
        } else {
            "2 pilots • ${minutesText(stopMinutes)} crew rest at ${stop.iata} • User operating both legs"
        }

        val rows = buildList {
            add(
                flight(
                    id = "${request.deliveryDate}-${registration}-POSITION-EK375",
                    airline = "Emirates",
                    flightNumber = "EK375",
                    aircraftLabel = "Passenger",
                    aircraftFullName = "Passenger positioning",
                    registration = "BOOKED",
                    from = "BKK",
                    to = "DXB",
                    departure = bkkDeparture,
                    arrival = dxbArrival,
                    durationMinutes = 415,
                    dutyType = DutyType.DEADHEAD.value,
                    note = positioningNote,
                    flightTimeCreditEligible = false
                )
            )
            add(
                flight(
                    id = "${request.deliveryDate}-${registration}-POSITION-EK61",
                    airline = "Emirates",
                    flightNumber = "EK61",
                    aircraftLabel = "Passenger",
                    aircraftFullName = "Passenger positioning",
                    registration = "BOOKED",
                    from = "DXB",
                    to = "HAM",
                    departure = dxbDeparture,
                    arrival = hamArrival,
                    durationMinutes = 390,
                    dutyType = DutyType.DEADHEAD.value,
                    note = positioningNote,
                    flightTimeCreditEligible = false
                )
            )
            add(
                flight(
                    id = "${request.deliveryDate}-${registration}-${request.deliveryFlightNumber}-1",
                    airline = "THAI",
                    flightNumber = "${request.deliveryFlightNumber}-1",
                    aircraftLabel = spec.label,
                    aircraftFullName = spec.fullName,
                    registration = registration,
                    from = "XFW",
                    to = stop.iata,
                    departure = ferryDeparture,
                    arrival = firstArrival,
                    durationMinutes = stop.firstLegMinutes,
                    dutyType = DutyType.FLIGHT.value,
                    note = "Aircraft Delivery • EDHI departure • $crewPlan • User ${role(userOperatesFirst)}",
                    isAircraftDelivery = true,
                    deliveryAircraftType = spec.label,
                    flightTimeCreditEligible = userOperatesFirst
                )
            )
            add(
                groundStop(
                    id = "${request.deliveryDate}-${registration}-${stop.iata}-STOP",
                    iata = stop.iata,
                    start = firstArrival,
                    end = secondDeparture,
                    crewSize = crewSize,
                    stopMinutes = stopMinutes
                )
            )
            add(
                flight(
                    id = "${request.deliveryDate}-${registration}-${request.deliveryFlightNumber}-2",
                    airline = "THAI",
                    flightNumber = "${request.deliveryFlightNumber}-2",
                    aircraftLabel = spec.label,
                    aircraftFullName = spec.fullName,
                    registration = registration,
                    from = stop.iata,
                    to = "BKK",
                    departure = secondDeparture,
                    arrival = finalArrival,
                    durationMinutes = stop.secondLegMinutes,
                    dutyType = DutyType.FLIGHT.value,
                    note = "Aircraft Delivery • Final leg to BKK • $crewPlan • User ${role(!userOperatesFirst || !fourPilots)}",
                    isAircraftDelivery = true,
                    deliveryAircraftType = spec.label,
                    flightTimeCreditEligible = !userOperatesFirst || !fourPilots
                )
            )
        }

        return AircraftDeliveryPlan(rows, stop.iata, crewSize, stopMinutes)
    }

    private fun role(operating: Boolean): String = if (operating) "OPERATING PILOT" else "PASSENGER / IN-FLIGHT REST"

    private fun minutesText(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"

    private fun flight(
        id: String,
        airline: String,
        flightNumber: String,
        aircraftLabel: String,
        aircraftFullName: String,
        registration: String,
        from: String,
        to: String,
        departure: LocalDateTime,
        arrival: LocalDateTime,
        durationMinutes: Int,
        dutyType: String,
        note: String,
        isAircraftDelivery: Boolean = false,
        deliveryAircraftType: String = "",
        flightTimeCreditEligible: Boolean
    ): FlightEntity {
        val departureAirport = requireNotNull(AirportDatabase.byIata(from)) { "Missing airport $from" }
        val arrivalAirport = requireNotNull(AirportDatabase.byIata(to)) { "Missing airport $to" }
        return FlightEntity(
            id = id,
            airline = airline,
            flightNumber = flightNumber,
            aircraftLabel = aircraftLabel,
            aircraftFullName = aircraftFullName,
            registration = registration,
            status = "SCHEDULED",
            departureIata = departureAirport.iata,
            departureIcao = departureAirport.icao,
            departureCity = departureAirport.city,
            departureAirport = departureAirport.name,
            arrivalIata = arrivalAirport.iata,
            arrivalIcao = arrivalAirport.icao,
            arrivalCity = arrivalAirport.city,
            arrivalAirport = arrivalAirport.name,
            departureDateTime = departure.toString(),
            arrivalDateTime = arrival.toString(),
            durationMinutes = durationMinutes,
            dutyType = dutyType,
            dutyNote = note,
            isAircraftDelivery = isAircraftDelivery,
            deliveryAircraftType = deliveryAircraftType,
            rosterSource = "DELIVERY",
            flightTimeCreditEligible = flightTimeCreditEligible
        )
    }

    private fun groundStop(
        id: String,
        iata: String,
        start: LocalDateTime,
        end: LocalDateTime,
        crewSize: Int,
        stopMinutes: Int
    ): FlightEntity {
        val airport = requireNotNull(AirportDatabase.byIata(iata))
        val rest = crewSize == 2
        return FlightEntity(
            id = id,
            airline = "THAI",
            flightNumber = if (rest) "CREW REST" else "TECHNICAL STOP",
            aircraftLabel = if (rest) "REST" else "REFUEL",
            aircraftFullName = if (rest) "Delivery crew rest" else "Fuel and technical turnaround",
            registration = "N/A",
            status = if (rest) "REST" else "GROUND",
            departureIata = airport.iata,
            departureIcao = airport.icao,
            departureCity = airport.city,
            departureAirport = airport.name,
            arrivalIata = airport.iata,
            arrivalIcao = airport.icao,
            arrivalCity = airport.city,
            arrivalAirport = airport.name,
            departureDateTime = start.toString(),
            arrivalDateTime = end.toString(),
            durationMinutes = stopMinutes,
            dutyType = if (rest) DutyType.CREW_REST.value else DutyType.TECHNICAL_STOP.value,
            dutyNote = if (rest) "Mandatory 8-12h rest for two-pilot delivery crew" else "2-4h fuel and technical stop; four-pilot augmented crew continues",
            rosterSource = "DELIVERY",
            flightTimeCreditEligible = false
        )
    }
}
