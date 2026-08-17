package com.example.crewportal.data.roster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.YearMonth

class RosterGeneratorDeterminismTest {
    @Test
    fun sameMonthAndSeedProduceSameRoster() {
        val month = YearMonth.of(2026, 11)
        assertEquals(
            RosterGenerator.generateForMonth(month, seed = 42L),
            RosterGenerator.generateForMonth(month, seed = 42L)
        )
    }

    @Test
    fun explicitDifferentSeedsCanProduceDifferentRoster() {
        val month = YearMonth.of(2026, 11)
        assertNotEquals(
            RosterGenerator.generateForMonth(month, seed = 42L),
            RosterGenerator.generateForMonth(month, seed = 43L)
        )
    }
}
