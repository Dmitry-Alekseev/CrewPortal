package com.example.crewportal.data.airport

import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AirportInfo(
    val iata: String,
    val icao: String,
    val name: String,
    val city: String,
    val country: String,
    val utcOffsetMinutes: Int,
    val elevationFt: Int,
    val runways: String,
    val terminalNotes: String,
    val operationalNotes: String,
    val atcNotes: String = "Company briefing required",
    val metarIcao: String = icao,
    val zoneId: String = AirportTimeZoneDirectory.zoneIdFor(iata, utcOffsetMinutes)
)

/** Region-based zones keep DST-sensitive airports correct; the offset is only a legacy fallback. */
object AirportTimeZoneDirectory {
    private val zones = buildMap {
        listOf("BKK", "HKT", "CNX", "KBV", "HDY", "CEI", "UBP", "UTH", "KKC").forEach { put(it, "Asia/Bangkok") }
        put("SIN", "Asia/Singapore")
        listOf("KUL", "PEN").forEach { put(it, "Asia/Kuala_Lumpur") }
        listOf("SGN", "HAN", "CXR").forEach { put(it, "Asia/Ho_Chi_Minh") }
        listOf("REP", "PNH").forEach { put(it, "Asia/Phnom_Penh") }
        put("VTE", "Asia/Vientiane"); put("RGN", "Asia/Yangon"); put("DPS", "Asia/Makassar"); put("MNL", "Asia/Manila")
        put("HKG", "Asia/Hong_Kong"); listOf("TPE", "KHH").forEach { put(it, "Asia/Taipei") }
        listOf("NRT", "HND", "KIX", "NGO", "FUK", "CTS").forEach { put(it, "Asia/Tokyo") }
        listOf("ICN", "PUS").forEach { put(it, "Asia/Seoul") }
        listOf("PEK", "PVG", "CAN", "KMG", "TFU").forEach { put(it, "Asia/Shanghai") }
        listOf("DEL", "BOM", "BLR", "MAA", "HYD", "CCU", "AMD").forEach { put(it, "Asia/Kolkata") }
        put("DAC", "Asia/Dhaka"); put("KTM", "Asia/Kathmandu"); put("CMB", "Asia/Colombo")
        listOf("ISB", "LHE", "KHI").forEach { put(it, "Asia/Karachi") }
        put("DXB", "Asia/Dubai"); put("DOH", "Asia/Qatar"); put("KWI", "Asia/Kuwait"); put("MCT", "Asia/Muscat")
        listOf("JED", "MED").forEach { put(it, "Asia/Riyadh") }
        put("IST", "Europe/Istanbul"); listOf("FRA", "MUC").forEach { put(it, "Europe/Berlin") }
        put("ZRH", "Europe/Zurich"); put("LHR", "Europe/London"); put("CDG", "Europe/Paris"); put("BRU", "Europe/Brussels")
        put("CPH", "Europe/Copenhagen"); put("ARN", "Europe/Stockholm"); put("OSL", "Europe/Oslo")
        listOf("FCO", "MXP").forEach { put(it, "Europe/Rome") }; put("AMS", "Europe/Amsterdam")
        put("TAS", "Asia/Tashkent"); listOf("LED", "SVO").forEach { put(it, "Europe/Moscow") }
        put("OVB", "Asia/Novosibirsk"); put("SVX", "Asia/Yekaterinburg"); listOf("UUD", "IKT").forEach { put(it, "Asia/Irkutsk") }
        listOf("VVO", "KHV").forEach { put(it, "Asia/Vladivostok") }
        put("SYD", "Australia/Sydney"); put("MEL", "Australia/Melbourne"); put("PER", "Australia/Perth")
        put("BNE", "Australia/Brisbane"); put("AKL", "Pacific/Auckland")
    }

    fun zoneIdFor(iata: String, fallbackOffsetMinutes: Int): String = zones[iata.uppercase(Locale.ENGLISH)]
        ?: ZoneOffset.ofTotalSeconds(fallbackOffsetMinutes * 60).id
}

object AirportDatabase {
    private val airportList = listOf(
        AirportInfo("BKK", "VTBS", "Suvarnabhumi Intl", "Bangkok", "Thailand", 420, 5, "01L/19R, 01R/19L", "Main hub. Domestic and international operations.", "Line maintenance, crew transport and long-haul handling available.", "BKK Delivery / Ground / Tower / Departure"),
        AirportInfo("HKT", "VTSP", "Phuket Intl", "Phuket", "Thailand", 420, 82, "09/27", "Domestic/international Thai station.", "Common short-haul turnaround and layover station."),
        AirportInfo("CNX", "VTCC", "Chiang Mai Intl", "Chiang Mai", "Thailand", 420, 1036, "18/36", "Domestic Thai station.", "Northern Thailand turnaround / layover operation."),
        AirportInfo("KBV", "VTSG", "Krabi Intl", "Krabi", "Thailand", 420, 82, "14/32", "Domestic Thai station.", "Resort destination, mainly short-haul turnaround."),
        AirportInfo("HDY", "VTSS", "Hat Yai Intl", "Hat Yai", "Thailand", 420, 90, "08/26", "Domestic Thai station.", "Southern Thailand turnaround station."),
        AirportInfo("CEI", "VTCT", "Chiang Rai Intl", "Chiang Rai", "Thailand", 420, 1280, "03/21", "Domestic Thai station.", "Northern Thailand turnaround station."),
        AirportInfo("UBP", "VTUU", "Ubon Ratchathani", "Ubon Ratchathani", "Thailand", 420, 406, "05/23", "Domestic Thai station.", "Regional turnaround station."),
        AirportInfo("UTH", "VTUD", "Udon Thani Intl", "Udon Thani", "Thailand", 420, 579, "12/30", "Domestic Thai station.", "Regional turnaround station."),
        AirportInfo("KKC", "VTUK", "Khon Kaen", "Khon Kaen", "Thailand", 420, 670, "03/21", "Domestic Thai station.", "Regional turnaround station."),

        AirportInfo("SIN", "WSSS", "Changi Intl", "Singapore", "Singapore", 480, 22, "02L/20R, 02C/20C, 02R/20L", "Major regional station.", "High-density regional operation; METAR/TAF via WSSS."),
        AirportInfo("KUL", "WMKK", "Kuala Lumpur Intl", "Kuala Lumpur", "Malaysia", 480, 69, "14L/32R, 14R/32L, 15/33", "Regional international station.", "Company handling available."),
        AirportInfo("PEN", "WMKP", "Penang Intl", "Penang", "Malaysia", 480, 11, "04/22", "Regional station.", "Short/medium-haul turnaround station."),
        AirportInfo("SGN", "VVTS", "Tan Son Nhat Intl", "Ho Chi Minh City", "Vietnam", 420, 33, "07L/25R, 07R/25L", "Vietnam station.", "Busy regional station; expect ATC flow management."),
        AirportInfo("HAN", "VVNB", "Noi Bai Intl", "Hanoi", "Vietnam", 420, 39, "11L/29R, 11R/29L", "Vietnam station.", "Regional international operation."),
        AirportInfo("CXR", "VVCR", "Cam Ranh Intl", "Nha Trang", "Vietnam", 420, 40, "02/20", "Resort station.", "Regional leisure operation."),
        AirportInfo("REP", "VDSR", "Siem Reap Angkor Intl", "Siem Reap", "Cambodia", 420, 60, "05/23", "Cambodia regional station.", "Short-haul international turnaround station."),
        AirportInfo("PNH", "VDPP", "Phnom Penh Intl", "Phnom Penh", "Cambodia", 420, 40, "05/23", "Cambodia station.", "Regional turnaround station."),
        AirportInfo("VTE", "VLVT", "Wattay Intl", "Vientiane", "Laos", 420, 564, "13/31", "Laos station.", "Regional turnaround station."),
        AirportInfo("RGN", "VYYY", "Yangon Intl", "Yangon", "Myanmar", 390, 109, "03/21", "Myanmar station.", "Regional turnaround station."),
        AirportInfo("DPS", "WADD", "Ngurah Rai Intl", "Denpasar", "Indonesia", 480, 14, "09/27", "Bali regional station.", "Medium-haul turnaround or layover station."),
        AirportInfo("MNL", "RPLL", "Ninoy Aquino Intl", "Manila", "Philippines", 480, 75, "06/24, 13/31", "Philippines station.", "Regional medium-haul station."),

        AirportInfo("HKG", "VHHH", "Hong Kong Intl", "Hong Kong", "Hong Kong", 480, 28, "07L/25R, 07C/25C, 07R/25L", "Major regional station.", "High-density regional and cargo traffic."),
        AirportInfo("TPE", "RCTP", "Taoyuan Intl", "Taipei", "Taiwan", 480, 106, "05L/23R, 05R/23L", "Taiwan station.", "Regional long sector operation."),
        AirportInfo("KHH", "RCKH", "Kaohsiung Intl", "Kaohsiung", "Taiwan", 480, 31, "09/27", "Taiwan station.", "Regional turnaround station."),
        AirportInfo("NRT", "RJAA", "Narita Intl", "Tokyo", "Japan", 540, 141, "16L/34R, 16R/34L", "Japan long-haul/regional station.", "Company handling and hotel transport by local station."),
        AirportInfo("HND", "RJTT", "Tokyo Haneda", "Tokyo", "Japan", 540, 21, "04/22, 05/23, 16L/34R, 16R/34L", "Japan station.", "Busy terminal environment; company briefing required."),
        AirportInfo("KIX", "RJBB", "Kansai Intl", "Osaka", "Japan", 540, 17, "06L/24R, 06R/24L", "Japan station.", "Medium/long-haul station."),
        AirportInfo("NGO", "RJGG", "Chubu Centrair", "Nagoya", "Japan", 540, 15, "18/36", "Japan station.", "Medium-haul station."),
        AirportInfo("FUK", "RJFF", "Fukuoka", "Fukuoka", "Japan", 540, 32, "16/34", "Japan station.", "Regional Japan operation."),
        AirportInfo("CTS", "RJCC", "New Chitose", "Sapporo", "Japan", 540, 82, "01L/19R, 01R/19L", "Japan station.", "Seasonal weather monitoring required."),
        AirportInfo("ICN", "RKSI", "Incheon Intl", "Seoul", "South Korea", 540, 23, "15L/33R, 15R/33L, 16L/34R, 16R/34L", "Korea station.", "High-density Northeast Asia station."),
        AirportInfo("PUS", "RKPK", "Gimhae Intl", "Busan", "South Korea", 540, 6, "18L/36R, 18R/36L", "Korea station.", "Terrain and procedure briefing required."),

        AirportInfo("PEK", "ZBAA", "Beijing Capital", "Beijing", "China", 480, 116, "18L/36R, 18R/36L, 01/19", "China station.", "Busy hub operation; flow restrictions possible."),
        AirportInfo("PVG", "ZSPD", "Shanghai Pudong", "Shanghai", "China", 480, 13, "16L/34R, 16R/34L, 17L/35R, 17R/35L", "China station.", "Major long/regional station."),
        AirportInfo("CAN", "ZGGG", "Guangzhou Baiyun", "Guangzhou", "China", 480, 50, "01L/19R, 01R/19L, 02L/20R", "China station.", "Southern China regional hub."),
        AirportInfo("KMG", "ZPPP", "Kunming Changshui", "Kunming", "China", 480, 6903, "03/21, 04/22", "China station.", "High elevation airport; performance briefing required."),
        AirportInfo("TFU", "ZUTF", "Chengdu Tianfu", "Chengdu", "China", 480, 1448, "01/19, 02/20, 11/29", "China station.", "Western China station."),

        AirportInfo("DEL", "VIDP", "Indira Gandhi Intl", "Delhi", "India", 330, 777, "10/28, 11R/29L, 09/27", "India station.", "High traffic density and seasonal visibility monitoring."),
        AirportInfo("BOM", "VABB", "Chhatrapati Shivaji Intl", "Mumbai", "India", 330, 37, "09/27, 14/32", "India station.", "Busy terminal and ATC environment."),
        AirportInfo("BLR", "VOBL", "Kempegowda Intl", "Bengaluru", "India", 330, 3000, "09L/27R, 09R/27L", "India station.", "South India operation."),
        AirportInfo("MAA", "VOMM", "Chennai Intl", "Chennai", "India", 330, 52, "07/25, 12/30", "India station.", "South India coastal weather monitoring."),
        AirportInfo("HYD", "VOHS", "Rajiv Gandhi Intl", "Hyderabad", "India", 330, 2024, "09L/27R, 09R/27L", "India station.", "South/Central India operation."),
        AirportInfo("CCU", "VECC", "Netaji Subhas Chandra Bose Intl", "Kolkata", "India", 330, 16, "01L/19R, 01R/19L", "India station.", "Eastern India operation."),
        AirportInfo("AMD", "VAAH", "Sardar Vallabhbhai Patel Intl", "Ahmedabad", "India", 330, 189, "05/23", "India station.", "Western India operation."),
        AirportInfo("DAC", "VGHS", "Hazrat Shahjalal Intl", "Dhaka", "Bangladesh", 360, 27, "14/32", "Bangladesh station.", "Dense regional traffic; weather monitoring required."),
        AirportInfo("KTM", "VNKT", "Tribhuvan Intl", "Kathmandu", "Nepal", 345, 4390, "02/20", "Nepal station.", "Terrain and performance briefing required."),
        AirportInfo("CMB", "VCBI", "Bandaranaike Intl", "Colombo", "Sri Lanka", 330, 30, "04/22", "Sri Lanka station.", "Indian Ocean regional operation."),
        AirportInfo("ISB", "OPIS", "Islamabad Intl", "Islamabad", "Pakistan", 300, 1761, "10L/28R, 10R/28L", "Pakistan station.", "Station security and weather briefing required."),
        AirportInfo("LHE", "OPLA", "Allama Iqbal Intl", "Lahore", "Pakistan", 300, 712, "18L/36R, 18R/36L", "Pakistan station.", "Regional long sector operation."),
        AirportInfo("KHI", "OPKC", "Jinnah Intl", "Karachi", "Pakistan", 300, 100, "07L/25R, 07R/25L", "Pakistan station.", "Regional long sector operation."),

        AirportInfo("DXB", "OMDB", "Dubai Intl", "Dubai", "United Arab Emirates", 240, 62, "12L/30R, 12R/30L", "Middle East station.", "High-density international operation."),
        AirportInfo("DOH", "OTHH", "Hamad Intl", "Doha", "Qatar", 180, 13, "16L/34R, 16R/34L", "Middle East station.", "Long/medium-haul connection station."),
        AirportInfo("KWI", "OKKK", "Kuwait Intl", "Kuwait City", "Kuwait", 180, 206, "15L/33R, 15R/33L", "Middle East station.", "Hot weather performance briefing may be required."),
        AirportInfo("MCT", "OOMS", "Muscat Intl", "Muscat", "Oman", 240, 48, "08L/26R, 08R/26L", "Middle East station.", "Regional Middle East operation."),
        AirportInfo("JED", "OEJN", "King Abdulaziz Intl", "Jeddah", "Saudi Arabia", 180, 48, "16L/34R, 16C/34C, 16R/34L", "Saudi Arabia station.", "Seasonal religious traffic; station briefing required."),
        AirportInfo("MED", "OEMA", "Prince Mohammad bin Abdulaziz Intl", "Medina", "Saudi Arabia", 180, 2151, "17/35", "Saudi Arabia station.", "Station briefing required."),

        AirportInfo("IST", "LTFM", "Istanbul Airport", "Istanbul", "Türkiye", 180, 325, "16L/34R, 16R/34L, 17L/35R, 17R/35L, 18/36", "Europe/Türkiye long-haul station.", "Large airport, gate/stand and taxi planning important."),
        AirportInfo("FRA", "EDDF", "Frankfurt Main", "Frankfurt", "Germany", 120, 364, "07L/25R, 07C/25C, 07R/25L, 18", "Europe long-haul station.", "Major Star Alliance hub; company handling available."),
        AirportInfo("MUC", "EDDM", "Munich", "Munich", "Germany", 120, 1487, "08L/26R, 08R/26L", "Europe station.", "Long-haul station with local handler."),
        AirportInfo("ZRH", "LSZH", "Zurich", "Zurich", "Switzerland", 120, 1416, "10/28, 14/32, 16/34", "Europe station.", "Alpine weather monitoring where applicable."),
        AirportInfo("LHR", "EGLL", "Heathrow", "London", "United Kingdom", 60, 83, "09L/27R, 09R/27L", "Europe long-haul station.", "Slot-controlled airport; ground delay monitoring required."),
        AirportInfo("CDG", "LFPG", "Charles de Gaulle", "Paris", "France", 120, 392, "08L/26R, 08R/26L, 09L/27R, 09R/27L", "Europe long-haul station.", "Large multi-terminal operation."),
        AirportInfo("BRU", "EBBR", "Brussels", "Brussels", "Belgium", 120, 184, "01/19, 07L/25R, 07R/25L", "Europe station.", "Long-haul station."),
        AirportInfo("CPH", "EKCH", "Copenhagen", "Copenhagen", "Denmark", 120, 17, "04L/22R, 04R/22L, 12/30", "Europe station.", "Northern Europe long-haul station."),
        AirportInfo("ARN", "ESSA", "Stockholm Arlanda", "Stockholm", "Sweden", 120, 137, "01L/19R, 01R/19L, 08/26", "Europe station.", "Northern Europe long-haul station."),
        AirportInfo("OSL", "ENGM", "Oslo Gardermoen", "Oslo", "Norway", 120, 681, "01L/19R, 01R/19L", "Europe station.", "Winter operations briefing may be required."),
        AirportInfo("FCO", "LIRF", "Rome Fiumicino", "Rome", "Italy", 120, 13, "07/25, 16L/34R, 16R/34L", "Europe station.", "Southern Europe long-haul station."),
        AirportInfo("MXP", "LIMC", "Milan Malpensa", "Milan", "Italy", 120, 768, "17L/35R, 17R/35L", "Europe station.", "Northern Italy long-haul station."),
        AirportInfo("AMS", "EHAM", "Amsterdam Schiphol", "Amsterdam", "Netherlands", 120, -11, "04/22, 06/24, 09/27, 18L/36R, 18C/36C, 18R/36L", "Europe station.", "Large multi-runway airport; future network use."),


        AirportInfo("TAS", "UTTT", "Islam Karimov", "Tashkent", "Uzbekistan", 300, 1417, "08L/26R, 08R/26L", "Central Asia station.", "Medium-haul station; layover handling by local station."),
        AirportInfo("LED", "ULLI", "Pulkovo", "Saint Petersburg", "Russia", 180, 78, "10L/28R, 10R/28L", "Russia station.", "Long-haul seasonal/special network station; winter operations briefing required."),
        AirportInfo("SVO", "UUEE", "Sheremetyevo", "Moscow", "Russia", 180, 622, "06L/24R, 06C/24C, 06R/24L", "Russia station.", "Long-haul station; winter operations and de-icing planning where applicable."),
        AirportInfo("OVB", "UNNT", "Tolmachevo", "Novosibirsk", "Russia", 420, 365, "07/25, 16/34", "Russia station.", "Siberia station; cold weather briefing required in winter season."),
        AirportInfo("SVX", "USSS", "Koltsovo", "Yekaterinburg", "Russia", 300, 764, "08L/26R, 08R/26L", "Russia station.", "Ural station; winter and runway condition monitoring required."),
        AirportInfo("UUD", "UIUU", "Baikal", "Ulan-Ude", "Russia", 480, 1690, "08/26", "Russia station.", "Eastern Siberia station; cold weather performance briefing may be required."),
        AirportInfo("VVO", "UHWW", "Vladivostok", "Vladivostok", "Russia", 600, 46, "07L/25R, 07R/25L", "Russia station.", "Far East station; coastal weather monitoring required."),
        AirportInfo("IKT", "UIII", "Irkutsk", "Irkutsk", "Russia", 480, 1675, "12/30", "Russia station.", "Baikal region station; terrain and winter operations briefing required."),
        AirportInfo("KHV", "UHHH", "Khabarovsk Novy", "Khabarovsk", "Russia", 600, 244, "05L/23R, 05R/23L", "Russia station.", "Far East station; winter operations briefing required."),

        AirportInfo("SYD", "YSSY", "Sydney Kingsford Smith", "Sydney", "Australia", 600, 21, "16L/34R, 16R/34L, 07/25", "Australia long-haul station.", "Long-haul station, curfew and slot awareness required."),
        AirportInfo("MEL", "YMML", "Melbourne Intl", "Melbourne", "Australia", 600, 434, "16/34, 09/27", "Australia long-haul station.", "Long-haul station; crew transport by local handler."),
        AirportInfo("PER", "YPPH", "Perth Intl", "Perth", "Australia", 480, 67, "03/21, 06/24", "Australia station.", "Medium/long-haul station."),
        AirportInfo("BNE", "YBBN", "Brisbane", "Brisbane", "Australia", 600, 13, "01L/19R, 01R/19L", "Australia station.", "Long-haul station."),
        AirportInfo("AKL", "NZAA", "Auckland Intl", "Auckland", "New Zealand", 720, 23, "05R/23L, 05L/23R", "New Zealand station.", "Future network use / long-haul station.")
    )

    private val airportsByIata = airportList.associateBy { it.iata }
    private val airportsByIcao = airportList.associateBy { it.icao }

    fun byIata(iata: String): AirportInfo? = airportsByIata[iata.uppercase(Locale.ENGLISH)]

    fun byIcao(icao: String): AirportInfo? = airportsByIcao[icao.uppercase(Locale.ENGLISH)]

    fun search(query: String): List<AirportInfo> {
        val q = query.trim().uppercase(Locale.ENGLISH)
        if (q.isBlank()) return all()
        return airportList.filter { airport ->
            airport.iata.contains(q) ||
                airport.icao.contains(q) ||
                airport.city.uppercase(Locale.ENGLISH).contains(q) ||
                airport.name.uppercase(Locale.ENGLISH).contains(q) ||
                airport.country.uppercase(Locale.ENGLISH).contains(q)
        }.sortedWith(compareBy<AirportInfo> { if (it.icao.startsWith(q) || it.iata.startsWith(q)) 0 else 1 }.thenBy { it.iata })
    }

    fun all(): List<AirportInfo> = airportList.sortedBy { it.iata }

    /** Prefer authoritative airport metadata over legacy roster text such as "crew hotel". */
    fun cityName(iata: String, fallback: String = iata): String =
        byIata(iata)?.city ?: fallback.ifBlank { iata.uppercase(Locale.ENGLISH) }


    fun shortAirportName(iata: String, fallbackName: String = ""): String {
        val code = iata.uppercase(Locale.ENGLISH)
        return when (code) {
            "BKK" -> "Suvarnabhumi"
            "KUL" -> "KLIA"
            "TAS" -> "Islam Karimov"
            "SVO" -> "Sheremetyevo"
            "IST" -> "Istanbul Airport"
            "FRA" -> "Frankfurt Main"
            "LHR" -> "Heathrow"
            "HKT" -> "Phuket"
            "CNX" -> "Chiang Mai"
            "KBV" -> "Krabi"
            "SIN" -> "Changi"
            "DEL" -> "Indira Gandhi"
            "NRT" -> "Narita"
            "ICN" -> "Incheon"
            "DPS" -> "Ngurah Rai"
            "SGN" -> "Tan Son Nhat"
            "HAN" -> "Noi Bai"
            "REP" -> "Siem Reap Angkor"
            "DAC" -> "Hazrat Shahjalal"
            "MNL" -> "Ninoy Aquino"
            else -> cleanAirportName(fallbackName.ifBlank { byIata(code)?.name.orEmpty() })
        }
    }

    private fun cleanAirportName(name: String): String = name
        .replace(" International Airport", "")
        .replace(" International", "")
        .replace(" Intl", "")
        .replace(" Airport", "")
        .replace(" Main", "")
        .trim()

    fun utcText(localDateTime: String, iata: String): String {
        val airport = byIata(iata) ?: return "UTC time unavailable"
        val local = LocalDateTime.parse(localDateTime)
        val utc = local.atZone(ZoneId.of(airport.zoneId)).withZoneSameInstant(ZoneOffset.UTC)
        return utc.format(DateTimeFormatter.ofPattern("dd MMM HH:mm 'UTC'", Locale.ENGLISH)).uppercase(Locale.ENGLISH)
    }

    fun utcClockText(localDateTime: String, iata: String): String {
        val airport = byIata(iata) ?: return "UTC"
        val local = LocalDateTime.parse(localDateTime)
        val utc = local.atZone(ZoneId.of(airport.zoneId)).withZoneSameInstant(ZoneOffset.UTC)
        return utc.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH))
    }

    fun localOffsetText(iata: String): String {
        val airport = byIata(iata) ?: return "UTC offset unavailable"
        val minutes = ZoneId.of(airport.zoneId).rules.getOffset(Instant.now()).totalSeconds / 60
        val sign = if (minutes >= 0) "+" else "-"
        val absMinutes = kotlin.math.abs(minutes)
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        return if (mins == 0) "UTC$sign$hours" else "UTC$sign$hours:${mins.toString().padStart(2, '0')}"
    }
}
