package com.example.crewportal.data.mel

import org.json.JSONObject

data class MelDefect(
    val aircraftRegistration: String,
    val defectId: String,
    val melItem: String,
    val ata: String,
    val category: String,
    val status: String,
    val reportedDate: String,
    val description: String,
    val operationalLimitation: String,
    val rectificationDue: String,
    val plannedAction: String
)

object MelDatabase {
    const val githubMelUrl: String = "https://raw.githubusercontent.com/Dmitry-Alekseev/CrewPortal/main/mel/current_mel.json"

    private val defects = listOf(
        MelDefect("HS-TXH", "TG-MEL-2026-5827", "21-52-01", "ATA 21 Air Conditioning", "C", "Deferred", "22 MAY 2026", "Cabin temperature zone trim air valve position indication intermittent.", "Affected zone to be monitored by crew. Remaining temperature control channels available.", "01 JUN 2026", "BKK overnight troubleshooting and valve position feedback check."),
        MelDefect("HS-TXH", "TG-MEL-2026-7394", "33-45-02", "ATA 33 Lights", "C", "Deferred", "23 MAY 2026", "One passenger information sign legend light inoperative.", "Affected sign placarded. Cabin crew briefing required.", "02 JUN 2026", "Replace lamp module at BKK line maintenance."),
        MelDefect("HS-TXI", "TG-MEL-2026-2648", "34-41-01", "ATA 34 Navigation", "C", "Deferred", "21 MAY 2026", "Weather radar predictive windshear caution message intermittent during test.", "Weather radar returns available. Predictive function deferred under dispatch conditions.", "31 MAY 2026", "Avionics BITE download and sensor connector inspection."),
        MelDefect("HS-TXA", "TG-MEL-2026-9185", "25-23-01", "ATA 25 Equipment/Furnishings", "D", "Deferred", "18 MAY 2026", "One passenger seat recline mechanism locked in upright position.", "Seat placarded or blocked if required by cabin configuration.", "17 JUN 2026", "Seat mechanism replacement during scheduled A-check."),
        MelDefect("HS-TXB", "TG-MEL-2026-4072", "24-38-01", "ATA 24 Electrical Power", "C", "Deferred", "19 MAY 2026", "Galley power outlet group intermittent on aft galley.", "Affected galley load limited. Cabin crew informed.", "29 MAY 2026", "Galley circuit breaker and connector inspection at BKK."),
        MelDefect("HS-TEO", "TG-MEL-2026-6519", "23-12-01", "ATA 23 Communications", "C", "Deferred", "17 MAY 2026", "VHF 3 data mode unavailable.", "Voice communications normal. Remaining communication systems operative.", "27 MAY 2026", "Replace VHF control module during BKK maintenance slot."),
        MelDefect("HS-TEP", "TG-MEL-2026-3264", "30-21-01", "ATA 30 Ice/Rain Protection", "B", "Deferred", "24 MAY 2026", "Wing anti-ice valve indication slow to transit during post-flight test.", "Maintenance procedure compliance required before dispatch.", "27 MAY 2026", "Valve actuator inspection planned before next long-sector assignment."),
        MelDefect("HS-THB", "TG-MEL-2026-8751", "35-21-01", "ATA 35 Oxygen", "C", "Deferred", "15 MAY 2026", "Crew oxygen pressure indication channel 2 deferred after maintenance test anomaly.", "Primary indication normal. Required preflight check to be completed.", "25 MAY 2026", "Sensor replacement scheduled at BKK hangar."),
        MelDefect("HS-THF", "TG-MEL-2026-1936", "52-71-01", "ATA 52 Doors", "C", "Deferred", "24 MAY 2026", "Bulk cargo door indication requires repeat close verification.", "Maintenance release required after close verification procedure.", "03 JUN 2026", "Door proximity sensor adjustment at BKK."),
        MelDefect("HS-THK", "TG-MEL-2026-7048", "46-13-01", "ATA 46 Information Systems", "D", "Deferred", "12 MAY 2026", "EFB aircraft interface synchronization unavailable on right side mount.", "Crew EFB standalone operation available. Company briefing remains accessible.", "11 JUN 2026", "Data interface unit software reload."),
        MelDefect("HS-THN", "TG-MEL-2026-4592", "49-11-01", "ATA 49 APU", "C", "Deferred", "23 MAY 2026", "APU bleed availability message intermittent on first start of day.", "Ground air availability to be confirmed where required.", "02 JUN 2026", "APU control unit BITE review at BKK."),
        MelDefect("HS-TEV", "TG-MEL-2026-6275", "32-42-01", "ATA 32 Landing Gear", "C", "Deferred", "20 MAY 2026", "Brake temperature indication channel on wheel 4 intermittent.", "Brake cooling procedures and maintenance verification before release.", "30 MAY 2026", "Sensor harness inspection during overnight stop.")
    )

    fun forAircraft(registration: String): List<MelDefect> =
        defects.filter { it.aircraftRegistration.equals(registration, ignoreCase = true) }

    fun all(): List<MelDefect> = defects

    fun fromJson(json: String): List<MelDefect> {
        val root = JSONObject(json)
        val array = root.getJSONArray("defects")
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    MelDefect(
                        aircraftRegistration = item.getString("aircraftRegistration"),
                        defectId = item.getString("defectId"),
                        melItem = item.getString("melItem"),
                        ata = item.getString("ata"),
                        category = item.getString("category"),
                        status = item.getString("status"),
                        reportedDate = item.getString("reportedDate"),
                        description = item.getString("description"),
                        operationalLimitation = item.getString("operationalLimitation"),
                        rectificationDue = item.getString("rectificationDue"),
                        plannedAction = item.getString("plannedAction")
                    )
                )
            }
        }
    }
}
