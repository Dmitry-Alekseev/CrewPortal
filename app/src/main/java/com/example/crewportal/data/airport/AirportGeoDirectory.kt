package com.example.crewportal.data.airport

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

data class AirportCoordinate(val latitude: Double, val longitude: Double)

/**
 * Offline coordinates used by route briefing maps and fallback block-time estimation.
 * Keeping this data inside the APK avoids map/API keys, tile-server blocks and network-only maps.
 */
object AirportGeoDirectory {
    private val coordinates = mapOf(
        "BKK" to AirportCoordinate(13.69, 100.75),
        "HKT" to AirportCoordinate(8.11, 98.31), "CNX" to AirportCoordinate(18.77, 98.96),
        "KBV" to AirportCoordinate(8.10, 98.99), "HDY" to AirportCoordinate(6.93, 100.39),
        "CEI" to AirportCoordinate(19.95, 99.88), "UBP" to AirportCoordinate(15.25, 104.87),
        "UTH" to AirportCoordinate(17.39, 102.79), "KKC" to AirportCoordinate(16.47, 102.78),
        "SIN" to AirportCoordinate(1.36, 103.99), "KUL" to AirportCoordinate(2.75, 101.71),
        "PEN" to AirportCoordinate(5.30, 100.28), "SGN" to AirportCoordinate(10.82, 106.66),
        "HAN" to AirportCoordinate(21.22, 105.81), "CXR" to AirportCoordinate(11.99, 109.22),
        "PNH" to AirportCoordinate(11.55, 104.84), "VTE" to AirportCoordinate(17.99, 102.56),
        "RGN" to AirportCoordinate(16.91, 96.13), "REP" to AirportCoordinate(13.41, 103.81),
        "DPS" to AirportCoordinate(-8.75, 115.17),
        "MNL" to AirportCoordinate(14.51, 121.02), "HKG" to AirportCoordinate(22.31, 113.92),
        "TPE" to AirportCoordinate(25.08, 121.23), "KHH" to AirportCoordinate(22.58, 120.35),
        "NRT" to AirportCoordinate(35.77, 140.39), "HND" to AirportCoordinate(35.55, 139.78),
        "KIX" to AirportCoordinate(34.43, 135.24), "NGO" to AirportCoordinate(34.86, 136.81),
        "FUK" to AirportCoordinate(33.59, 130.45), "CTS" to AirportCoordinate(42.78, 141.69),
        "ICN" to AirportCoordinate(37.46, 126.44), "PUS" to AirportCoordinate(35.18, 128.94),
        "PEK" to AirportCoordinate(40.08, 116.58), "PVG" to AirportCoordinate(31.14, 121.80),
        "CAN" to AirportCoordinate(23.39, 113.30), "KMG" to AirportCoordinate(25.10, 102.93),
        "TFU" to AirportCoordinate(30.31, 104.44), "DEL" to AirportCoordinate(28.56, 77.10),
        "BOM" to AirportCoordinate(19.09, 72.87), "BLR" to AirportCoordinate(13.20, 77.71),
        "MAA" to AirportCoordinate(12.99, 80.17), "HYD" to AirportCoordinate(17.24, 78.43),
        "CCU" to AirportCoordinate(22.65, 88.45), "AMD" to AirportCoordinate(23.07, 72.63),
        "DAC" to AirportCoordinate(23.84, 90.40), "KTM" to AirportCoordinate(27.70, 85.36),
        "CMB" to AirportCoordinate(7.18, 79.88), "ISB" to AirportCoordinate(33.55, 72.83),
        "LHE" to AirportCoordinate(31.52, 74.40), "KHI" to AirportCoordinate(24.91, 67.16),
        "DXB" to AirportCoordinate(25.25, 55.36), "DOH" to AirportCoordinate(25.27, 51.61),
        "KWI" to AirportCoordinate(29.23, 47.97), "MCT" to AirportCoordinate(23.59, 58.28),
        "JED" to AirportCoordinate(21.68, 39.16), "MED" to AirportCoordinate(24.55, 39.71),
        "IST" to AirportCoordinate(41.28, 28.75), "FRA" to AirportCoordinate(50.04, 8.56),
        "MUC" to AirportCoordinate(48.35, 11.79), "ZRH" to AirportCoordinate(47.46, 8.55),
        "LHR" to AirportCoordinate(51.47, -0.45), "CDG" to AirportCoordinate(49.01, 2.55),
        "BRU" to AirportCoordinate(50.90, 4.48), "CPH" to AirportCoordinate(55.62, 12.66),
        "ARN" to AirportCoordinate(59.65, 17.92), "OSL" to AirportCoordinate(60.20, 11.10),
        "FCO" to AirportCoordinate(41.80, 12.24), "MXP" to AirportCoordinate(45.63, 8.72),
        "AMS" to AirportCoordinate(52.31, 4.76), "TAS" to AirportCoordinate(41.26, 69.28),
        "LED" to AirportCoordinate(59.80, 30.26), "SVO" to AirportCoordinate(55.97, 37.41),
        "OVB" to AirportCoordinate(55.01, 82.65), "SVX" to AirportCoordinate(56.74, 60.80),
        "UUD" to AirportCoordinate(51.81, 107.44), "VVO" to AirportCoordinate(43.40, 132.15),
        "IKT" to AirportCoordinate(52.27, 104.39), "KHV" to AirportCoordinate(48.53, 135.19),
        "SYD" to AirportCoordinate(-33.94, 151.18), "MEL" to AirportCoordinate(-37.67, 144.84),
        "PER" to AirportCoordinate(-31.94, 115.97), "BNE" to AirportCoordinate(-27.38, 153.12),
        "AKL" to AirportCoordinate(-37.01, 174.79)
    )

    fun byIata(iata: String): AirportCoordinate? = coordinates[iata.trim().uppercase()]

    fun distanceNm(fromIata: String, toIata: String): Int? {
        val from = byIata(fromIata) ?: return null
        val to = byIata(toIata) ?: return null
        val fromLat = Math.toRadians(from.latitude)
        val toLat = Math.toRadians(to.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)
        val centralAngle = acos(
            (sin(fromLat) * sin(toLat) + cos(fromLat) * cos(toLat) * cos(deltaLon))
                .coerceIn(-1.0, 1.0)
        )
        return (centralAngle * 3_440.065).toInt()
    }
}
