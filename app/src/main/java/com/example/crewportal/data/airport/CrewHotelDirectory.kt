package com.example.crewportal.data.airport

/** Shared crew-hotel directory for generated, manual and detail-screen layovers. */
object CrewHotelDirectory {
    private val hotels = mapOf(
        "BKK" to "Hyatt Regency Bangkok Suvarnabhumi Airport",
        "IST" to "Grand Hyatt Istanbul",
        "FRA" to "JW Marriott Hotel Frankfurt",
        "SVO" to "Hyatt Regency Moscow Petrovsky Park",
        "LHR" to "Sofitel London Heathrow",
        "NRT" to "Hilton Tokyo Narita Airport",
        "HND" to "The Prince Gallery Tokyo Kioicho",
        "ICN" to "Grand Hyatt Incheon",
        "SIN" to "Crowne Plaza Changi Airport",
        "HKT" to "The Slate Phuket",
        "KBV" to "Sofitel Krabi Phokeethra Golf & Spa Resort",
        "KUL" to "Sama-Sama Hotel KLIA",
        "DPS" to "Hyatt Regency Bali",
        "TAS" to "Hyatt Regency Tashkent",
        "LED" to "Cosmos Saint-Petersburg Pulkovo Airport Hotel",
        "MNL" to "Conrad Manila",
        "DEL" to "JW Marriott Aerocity",
        "CXR" to "Meliá Vinpearl Cam Ranh Beach Resort",
        "ZRH" to "Zurich Marriott Hotel",
        "MUC" to "Hilton Munich City",
        "SYD" to "Hilton Sydney",
        "MEL" to "Grand Hyatt Melbourne"
    )

    fun hotelFor(iata: String): String {
        val code = iata.uppercase()
        return hotels[code] ?: "Company contracted hotel — ${AirportDatabase.cityName(code, code)}"
    }
}
