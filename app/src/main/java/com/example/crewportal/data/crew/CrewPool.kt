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
     * The user is normally shown as Captain. A380 consolidation sectors before 2027 explicitly
     * place the user in the First Officer seat, while observer duties use a third-seat role.
     */
    private val thaiCaptains = listOf(
        "Nattapong Srisai",
        "Kittisak Boonmee",
        "Surasak Chaiyaporn",
        "Anan Wongchai",
        "Chaiwat Rattanakul",
        "Preecha Wongsuwan",
        "Teerapat Kanda",
        "Narongchai Prasert",
        "Somchai Charoen",
        "Kritsada Sutham",
        "Thanawat Panyadee",
        "Wichai Kulsiri",
        "Pongsakorn Maneerat",
        "Supachai Thongchai",
        "Arthit Sornchai",
        "Theerachai Kittipong",
        "Prasert Limsakul",
        "Chanon Phromchai",
        "Sakda Boonyarit",
        "Worapong Jitprasert"
    )

    private val thaiFirstOfficers = listOf(
        "Piyawat Kanda",
        "Thanakorn Prasert",
        "Naphat Sutham",
        "Chakkrit Wongsai",
        "Korawit Boonchu",
        "Peerawat Charoen",
        "Panupong Srisawat",
        "Supakit Thongdee",
        "Ratchanon Kittikul",
        "Thanasit Phromsiri",
        "Jirawat Maneewong",
        "Kavin Rattanaporn",
        "Sarawut Chaiyasit",
        "Pongsathon Lertchai",
        "Nawin Srisuwan",
        "Worasit Chantarang",
        "Thanapon Kitprasert",
        "Chayapon Wattanakul",
        "Sittichai Boonmee",
        "Nopparat Siripong"
    )

    private val thaiInstructors = listOf(
        "Somchai Thongchai — Line Instructor",
        "Niran Sutham — Line Instructor",
        "Pongsak Maneerat — Line Instructor",
        "Kriangsak Wongsuwan — Line Instructor",
        "Pramote Chaiyaporn — Line Instructor",
        "Vichai Rattanakul — Line Instructor",
        "Suriya Boonyarit — Line Instructor",
        "Chatchai Phromchai — Line Instructor",
        "Tanes Kulsiri — Line Instructor",
        "Apichart Srisawat — Line Instructor"
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

    fun forFlight(
        flightId: String,
        longHaul: Boolean,
        userAsObserver: Boolean = false,
        userAsFirstOfficer: Boolean = false
    ): FlightCrew {
        val seed = abs(flightId.hashCode())
        val captain = if (userAsObserver || userAsFirstOfficer) thaiCaptains[seed % thaiCaptains.size] else "Dmitrii Alekseev"
        val fo = if (userAsFirstOfficer) "Dmitrii Alekseev" else thaiFirstOfficers[(seed / 3) % thaiFirstOfficers.size]
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
