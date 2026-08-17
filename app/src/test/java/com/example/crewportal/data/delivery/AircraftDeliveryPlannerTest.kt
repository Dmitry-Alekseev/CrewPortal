package com.example.crewportal.data.delivery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AircraftDeliveryPlannerTest {
    @Test
    fun narrowBodyUsesTwoPilotsAndEightToTwelveHourRest() {
        val plan = AircraftDeliveryPlanner.build(
            AircraftDeliveryRequest(LocalDate.of(2026, 9, 10), "TG9000", "A321neo", "HS-TNA")
        )

        assertEquals(2, plan.flightCrewSize)
        assertTrue(plan.stopMinutes in 480..720)
        assertTrue(plan.rows.any { it.dutyType == "DEADHEAD" && it.departureIata == "BKK" && it.arrivalIata == "DXB" })
        assertTrue(plan.rows.any { it.dutyType == "DEADHEAD" && it.departureIata == "DXB" && it.arrivalIata == "HAM" })
        assertFalse(plan.rows.any { it.departureIata == "HAM" && it.arrivalIata == "XFW" })
        assertTrue(plan.rows.any { it.departureIcao == "EDHI" })
        assertTrue(plan.rows.any { it.arrivalIata == "BKK" && it.isAircraftDelivery })
    }

    @Test
    fun wideBodyUsesFourPilotsAndAlternatesUserRole() {
        val plan = AircraftDeliveryPlanner.build(
            AircraftDeliveryRequest(LocalDate.of(2026, 9, 12), "TG9002", "A330-900neo", "HS-TNB")
        )
        val ferry = plan.rows.filter { it.isAircraftDelivery }

        assertEquals(4, plan.flightCrewSize)
        assertTrue(plan.stopMinutes in 120..240)
        assertEquals(2, ferry.size)
        assertEquals(1, ferry.count { it.flightTimeCreditEligible })
        assertEquals(1, ferry.count { !it.flightTimeCreditEligible })
    }
}
