package com.example.crewportal.data.fleet

import org.junit.Assert.assertEquals
import org.junit.Test

class AircraftTypeCatalogTest {
    @Test
    fun a330NeoHasDedicatedTypeAndEngine() {
        val type = AircraftTypeCatalog.byLabel("A330neo")
        assertEquals("A330-900neo", type.label)
        assertEquals("Airbus A330-941", type.fullName)
        assertEquals("Rolls-Royce Trent 7000", type.engineType)
    }

    @Test
    fun deliveryCatalogKeepsBothA330NeoVariantsDistinct() {
        assertEquals("Airbus A330-841", AircraftTypeCatalog.byLabel("A330-800neo").fullName)
        assertEquals("Airbus A330-941", AircraftTypeCatalog.byLabel("A330-900neo").fullName)
    }
}
