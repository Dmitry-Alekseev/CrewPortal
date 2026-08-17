package com.example.crewportal.util

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class AirportTimeTest {
    @Test
    fun `BKK to TAS arrival uses both airport offsets`() {
        val arrival = arrivalLocalDateTime(
            departureLocal = LocalDateTime.parse("2026-08-06T09:20:00"),
            departureIata = "BKK",
            arrivalIata = "TAS",
            blockMinutes = 395
        )

        assertEquals(LocalDateTime.parse("2026-08-06T13:55:00"), arrival)
    }

    @Test
    fun `unknown airport preserves legacy local addition`() {
        val arrival = arrivalLocalDateTime(
            departureLocal = LocalDateTime.parse("2026-08-06T09:20:00"),
            departureIata = "BKK",
            arrivalIata = "ZZZ",
            blockMinutes = 60
        )

        assertEquals(LocalDateTime.parse("2026-08-06T10:20:00"), arrival)
    }

    @Test
    fun `London arrival uses seasonal ZoneId instead of fixed offset`() {
        val summer = arrivalLocalDateTime(
            LocalDateTime.parse("2026-08-17T08:00:00"), "BKK", "LHR", 760
        )
        val winter = arrivalLocalDateTime(
            LocalDateTime.parse("2026-12-17T08:00:00"), "BKK", "LHR", 760
        )

        assertEquals(LocalDateTime.parse("2026-08-17T14:40:00"), summer)
        assertEquals(LocalDateTime.parse("2026-12-17T13:40:00"), winter)
    }
}
