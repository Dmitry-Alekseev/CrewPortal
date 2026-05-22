package com.example.crewportal.data.airport

import kotlin.math.abs

data class AirportAssignment(
    val gate: String,
    val stand: String,
    val terminal: String
) {
    val displayValue: String
        get() = when {
            gate != "—" && gate != "Pending" -> "Gate $gate"
            stand != "—" && stand != "Pending" -> "Stand $stand"
            else -> "Pending"
        }
}

object AirportAssignmentPool {
    fun assign(departureIata: String, aircraftLabel: String, durationMinutes: Int, seed: String): AirportAssignment {
        val longHaul = durationMinutes >= 360 || aircraftLabel.startsWith("A350")
        val wideBody = longHaul || aircraftLabel.startsWith("A330")
        val key = abs(seed.hashCode())
        val airport = departureIata.uppercase()
        val useGate = when {
            longHaul -> true
            wideBody -> key % 10 < 8
            airport == "BKK" && durationMinutes < 120 -> key % 10 < 6
            else -> key % 10 < 7
        }

        val terminal = when (airport) {
            "BKK" -> "Main Terminal"
            "HKT" -> if (durationMinutes < 120) "Domestic Terminal" else "International Terminal"
            "SIN" -> "Terminal 1"
            "IST" -> "Main Terminal"
            "FRA" -> "Terminal 1"
            "NRT", "HND" -> "Terminal 1"
            "ICN" -> "Terminal 1"
            "DEL", "BOM" -> "Terminal 3"
            else -> "Main Terminal"
        }

        return if (useGate) {
            AirportAssignment(gate = gateFor(airport, wideBody, key), stand = "—", terminal = terminal)
        } else {
            AirportAssignment(gate = "—", stand = standFor(airport, wideBody, key), terminal = terminal)
        }
    }

    private fun gateFor(airport: String, wideBody: Boolean, key: Int): String {
        val gates = when (airport) {
            "BKK" -> if (wideBody) listOf("E4", "E6", "E8", "F2", "F4", "G3", "G5") else listOf("A2", "A4", "B3", "C2", "D1", "D4")
            "HKT" -> listOf("4", "5", "7", "9", "11")
            "SIN" -> if (wideBody) listOf("C18", "C20", "D34", "D36") else listOf("A12", "B4", "C15", "D30")
            "IST" -> listOf("B5", "D8", "F2", "F6", "G9")
            "FRA" -> listOf("B24", "B28", "C13", "C16", "Z52")
            "NRT" -> listOf("35", "42", "45", "56")
            "HND" -> listOf("108", "110", "112", "145")
            "ICN" -> listOf("11", "17", "22", "28")
            else -> listOf("A1", "A3", "B2", "C4", "D6")
        }
        return gates[key % gates.size]
    }

    private fun standFor(airport: String, wideBody: Boolean, key: Int): String {
        val stands = when (airport) {
            "BKK" -> if (wideBody) listOf("501", "503", "506", "512") else listOf("207", "211", "304", "307", "312", "401")
            "HKT" -> listOf("21", "23", "25", "31", "33")
            "SIN" -> listOf("R12", "R14", "R18")
            "IST" -> listOf("705", "712", "724", "731")
            "FRA" -> listOf("V94", "V96", "V101")
            else -> listOf("201", "203", "305", "407")
        }
        return stands[key % stands.size]
    }
}
