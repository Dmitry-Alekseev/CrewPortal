package com.example.crewportal.data.fleet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AircraftPoolCurrentFleetTest {
    @Test
    fun `active Thai A321neo register contains all nine listed aircraft`() {
        val registrations = AircraftPool.aircraft
            .filter { it.label == "A321neo" }
            .map { it.registration }
            .toSet()

        assertEquals(
            setOf("HS-TOA", "HS-TOB", "HS-TOD", "HS-TOE", "HS-TOF", "HS-TOG", "HS-TOI", "HS-TOJ", "HS-TOL"),
            registrations
        )
        assertTrue(AircraftPool.aircraft.filter { it.label == "A321neo" }.all { it.configuration == "C16 / Y159" })
    }

    @Test
    fun `current A330 and A350 additions replace stale seed registrations`() {
        val registrations = AircraftPool.aircraft.map { it.registration }.toSet()

        assertTrue(registrations.containsAll(setOf("HS-TEW", "HS-TEX", "HS-THR", "HS-THX")))
        assertFalse("HS-TXI" in registrations)
        assertFalse("HS-TBC" in registrations)
    }
}
