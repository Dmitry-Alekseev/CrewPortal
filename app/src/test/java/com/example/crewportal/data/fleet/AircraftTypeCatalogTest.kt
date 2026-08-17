package com.example.crewportal.data.fleet

import org.junit.Assert.assertEquals
import org.junit.Test

class AircraftTypeCatalogTest {
    @Test
    fun a330NeoHasDedicatedTypeAndEngine() {
        val type = AircraftTypeCatalog.byLabel("A330neo")
        assertEquals("Airbus A330-941", type.fullName)
        assertEquals("Rolls-Royce Trent 7000", type.engineType)
    }
}
