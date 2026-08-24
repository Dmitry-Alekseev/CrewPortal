package com.example.crewportal.data.route

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class A380RouteCompatibilityTest {
    @Test
    fun `american airports are available to company route configurator`() {
        val expected = setOf("LAX", "SFO", "SEA", "JFK", "IAD", "ORD", "DFW", "BOS", "MIA", "ATL")
        assertTrue(expected.all { iata -> RouteCatalog.routes.any { it.destinationIata == iata && it.autoGenerationEnabled } })
    }

    @Test
    fun `a380 requires both airport approval and range`() {
        assertTrue(RouteCatalog.byIata("LAX").a380Eligible)
        assertTrue(RouteCatalog.byIata("JFK").a380Eligible)
        assertFalse(RouteCatalog.byIata("SEA").a380Eligible)
        assertFalse(RouteCatalog.byIata("MIA").a380Eligible)
    }
}
