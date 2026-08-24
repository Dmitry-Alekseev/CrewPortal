package com.example.crewportal.data.qualification

import com.example.crewportal.data.local.FlightEntity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Date-bounded transition policy for the 2026 A380 type-rating programme.
 *
 * The rating becomes valid after the final simulator check on 31 October. Until the end of
 * 2026 the generated roster assigns the user as First Officer for consolidation flying. From
 * 1 January 2027 the normal captain role and captain logbook/PIC rules resume automatically.
 */
object A380QualificationPolicy {
    val ratingEffectiveDate: LocalDate = LocalDate.of(2026, 11, 1)
    val captainEffectiveDate: LocalDate = LocalDate.of(2027, 1, 1)

    fun hasTypeRating(onDate: LocalDate = LocalDate.now()): Boolean = !onDate.isBefore(ratingEffectiveDate)

    fun canBeAutoAssigned(onDate: LocalDate): Boolean = hasTypeRating(onDate)

    fun userIsFirstOfficer(flight: FlightEntity): Boolean {
        if (!flight.aircraftLabel.contains("A380", ignoreCase = true)) return false
        val departureDate = LocalDateTime.parse(flight.departureDateTime).toLocalDate()
        return !departureDate.isBefore(ratingEffectiveDate) && departureDate.isBefore(captainEffectiveDate)
    }
}
