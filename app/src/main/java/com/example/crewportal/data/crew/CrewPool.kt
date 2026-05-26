package com.example.crewportal.data.crew

import kotlin.math.abs

data class FlightCrew(
    val captain: String,
    val firstOfficer: String,
    val reliefCaptain: String? = null,
    val reliefFirstOfficer: String? = null,
    val cabinManager: String,
    val cabinCrewCount: Int
)

object CrewPool {
    /**
     * User is always shown as the Captain in own roster.
     * Other flight crew are selected deterministically from Thai crew pools by flightId.
     */
    private val thaiCaptains = listOf(
        "Capt. Nattapong Srisai",
        "Capt. Kittisak Boonmee",
        "Capt. Surasak Chaiyaporn",
        "Capt. Anan Wongchai",
        "Capt. Chaiwat Rattanakul",
        "Capt. Preecha Wongsuwan",
        "Capt. Teerapat Kanda",
        "Capt. Narongchai Prasert",
        "Capt. Somchai Charoen",
        "Capt. Kritsada Sutham",
        "Capt. Thanawat Panyadee",
        "Capt. Wichai Kulsiri",
        "Capt. Pongsakorn Maneerat",
        "Capt. Supachai Thongchai",
        "Capt. Arthit Sornchai",
        "Capt. Theerachai Kittipong",
        "Capt. Prasert Limsakul",
        "Capt. Chanon Phromchai",
        "Capt. Sakda Boonyarit",
        "Capt. Worapong Jitprasert"
    )

    private val thaiFirstOfficers = listOf(
        "FO Piyawat Kanda",
        "FO Thanakorn Prasert",
        "FO Naphat Sutham",
        "FO Chakkrit Wongsai",
        "FO Korawit Boonchu",
        "FO Peerawat Charoen",
        "FO Panupong Srisawat",
        "FO Supakit Thongdee",
        "FO Ratchanon Kittikul",
        "FO Thanasit Phromsiri",
        "FO Jirawat Maneewong",
        "FO Kavin Rattanaporn",
        "FO Sarawut Chaiyasit",
        "FO Pongsathon Lertchai",
        "FO Nawin Srisuwan",
        "FO Worasit Chantarang",
        "FO Thanapon Kitprasert",
        "FO Chayapon Wattanakul",
        "FO Sittichai Boonmee",
        "FO Nopparat Siripong"
    )

    private val thaiInstructors = listOf(
        "Capt. Somchai Thongchai — Line Instructor",
        "Capt. Niran Sutham — Line Instructor",
        "Capt. Pongsak Maneerat — Line Instructor",
        "Capt. Kriangsak Wongsuwan — Line Instructor",
        "Capt. Pramote Chaiyaporn — Line Instructor",
        "Capt. Vichai Rattanakul — Line Instructor",
        "Capt. Suriya Boonyarit — Line Instructor",
        "Capt. Chatchai Phromchai — Line Instructor",
        "Capt. Tanes Kulsiri — Line Instructor",
        "Capt. Apichart Srisawat — Line Instructor"
    )

    private val cabinManagers = listOf(
        "Pimchanok S.",
        "Siriporn K.",
        "Narumon P.",
        "Achara W.",
        "Kanyarat T.",
        "Wipada C.",
        "Chanida R.",
        "Ploypailin M.",
        "Sudarat N.",
        "Benjawan T."
    )

    fun forFlight(flightId: String, longHaul: Boolean): FlightCrew {
        val seed = abs(flightId.hashCode())
        val captain = "Dmitrii Alekseev"
        val fo = thaiFirstOfficers[(seed / 3) % thaiFirstOfficers.size]
        val cm = cabinManagers[(seed / 7) % cabinManagers.size]

        return if (longHaul) {
            FlightCrew(
                captain = captain,
                firstOfficer = fo,
                reliefCaptain = thaiCaptains[(seed / 11) % thaiCaptains.size],
                reliefFirstOfficer = thaiFirstOfficers[(seed / 13) % thaiFirstOfficers.size],
                cabinManager = cm,
                cabinCrewCount = 10 + seed % 4
            )
        } else {
            FlightCrew(
                captain = captain,
                firstOfficer = fo,
                reliefCaptain = null,
                reliefFirstOfficer = null,
                cabinManager = cm,
                cabinCrewCount = 4
            )
        }
    }

    fun lineInstructorForFlight(flightId: String): String {
        val seed = abs(flightId.hashCode())
        return thaiInstructors[(seed / 5) % thaiInstructors.size]
    }
}
