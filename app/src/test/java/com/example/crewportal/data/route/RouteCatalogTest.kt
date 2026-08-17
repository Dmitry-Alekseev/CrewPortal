package com.example.crewportal.data.route

import com.example.crewportal.data.airport.AirportGeoDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.Assert.assertTrue

class RouteCatalogTest {
    @Test
    fun `Saint Petersburg keeps long haul block times and offline coordinates`() {
        val route = RouteCatalog.byIata("LED")

        assertEquals(650, route.outboundMinutes)
        assertEquals(625, route.inboundMinutes)
        assertNotNull(AirportGeoDirectory.byIata("LED"))
    }

    @Test
    fun `London keeps long haul block times`() {
        val route = RouteCatalog.byIata("LHR")

        assertEquals(760, route.outboundMinutes)
        assertEquals(705, route.inboundMinutes)
    }

    @Test
    fun `known non catalog airport does not fall back to two hours thirty`() {
        val route = RouteCatalog.byIata("AMS")

        assertTrue(route.outboundMinutes != 150)
        assertTrue(route.inboundMinutes != 150)
    }
}
