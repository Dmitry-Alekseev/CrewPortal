package com.example.crewportal.data.mel

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
    private val defects = listOf(
        MelDefect("HS-TXH", "TG-MEL-2026-0147", "21-52-01", "ATA 21 Air Conditioning", "C", "Deferred", "22 MAY 2026", "Cabin temperature zone trim air valve position indication intermittent.", "Dispatch permitted in accordance with MEL procedures. Affected zone monitored by crew.", "01 JUN 2026", "BKK night stop troubleshooting and valve position feedback check."),
        MelDefect("HS-TXH", "TG-MEL-2026-0151", "33-45-02", "ATA 33 Lights", "C", "Deferred", "23 MAY 2026", "One passenger information sign legend light inoperative.", "Placard affected sign. Cabin crew briefing required.", "02 JUN 2026", "Replace lamp module at BKK line maintenance."),
        MelDefect("HS-TXI", "TG-MEL-2026-0149", "34-41-01", "ATA 34 Navigation", "C", "Deferred", "21 MAY 2026", "Weather radar predictive windshear caution message intermittent during test.", "Weather radar serviceable for normal returns. Predictive function deferred as per MEL dispatch condition.", "31 MAY 2026", "Avionics BITE download and sensor connector inspection."),
        MelDefect("HS-TXA", "TG-MEL-2026-0132", "25-23-01", "ATA 25 Equipment/Furnishings", "D", "Deferred", "18 MAY 2026", "One passenger seat recline mechanism locked in upright position.", "Seat placarded or blocked if required by cabin configuration.", "17 JUN 2026", "Seat mechanism replacement during scheduled A-check."),
        MelDefect("HS-TXB", "TG-MEL-2026-0139", "24-38-01", "ATA 24 Electrical Power", "C", "Deferred", "19 MAY 2026", "Galley power outlet group intermittent on aft galley.", "Affected galley load limited. Cabin crew informed.", "29 MAY 2026", "Galley circuit breaker and connector inspection at BKK."),
        MelDefect("HS-TEO", "TG-MEL-2026-0128", "23-12-01", "ATA 23 Communications", "C", "Deferred", "17 MAY 2026", "VHF 3 data mode unavailable.", "Voice communications normal. Dispatch permitted with remaining communication systems operative.", "27 MAY 2026", "Replace VHF control module during BKK maintenance slot."),
        MelDefect("HS-TEP", "TG-MEL-2026-0156", "30-21-01", "ATA 30 Ice/Rain Protection", "B", "Deferred", "24 MAY 2026", "Wing anti-ice valve indication slow to transit during post-flight test.", "Dispatch subject to MEL restrictions and maintenance procedure compliance.", "27 MAY 2026", "Valve actuator inspection planned before next long-sector assignment."),
        MelDefect("HS-THB", "TG-MEL-2026-0117", "35-21-01", "ATA 35 Oxygen", "C", "Deferred", "15 MAY 2026", "Crew oxygen pressure indication channel 2 deferred after maintenance test anomaly.", "Primary indication normal. Dispatch permitted by MEL with required preflight check.", "25 MAY 2026", "Sensor replacement scheduled at BKK hangar."),
        MelDefect("HS-THF", "TG-MEL-2026-0162", "52-71-01", "ATA 52 Doors", "C", "Deferred", "24 MAY 2026", "Bulk cargo door indication requires repeat close verification.", "Maintenance release required after close verification procedure.", "03 JUN 2026", "Door proximity sensor adjustment at BKK."),
        MelDefect("HS-THK", "TG-MEL-2026-0109", "46-13-01", "ATA 46 Information Systems", "D", "Deferred", "12 MAY 2026", "EFB aircraft interface synchronization unavailable on right side mount.", "Crew EFB standalone operation available. Company briefing package remains accessible.", "11 JUN 2026", "Data interface unit software reload."),
        MelDefect("HS-THN", "TG-MEL-2026-0158", "49-11-01", "ATA 49 APU", "C", "Deferred", "23 MAY 2026", "APU bleed availability message intermittent on first start of day.", "Dispatch permitted with operational restrictions and ground air availability confirmed where required.", "02 JUN 2026", "APU control unit BITE review at BKK."),
        MelDefect("HS-TEV", "TG-MEL-2026-0141", "32-42-01", "ATA 32 Landing Gear", "C", "Deferred", "20 MAY 2026", "Brake temperature indication channel on wheel 4 intermittent.", "Brake cooling and dispatch procedures per MEL. Maintenance verification before release.", "30 MAY 2026", "Sensor harness inspection during overnight stop.")
    )

    fun forAircraft(registration: String): List<MelDefect> =
        defects.filter { it.aircraftRegistration.equals(registration, ignoreCase = true) }

    fun all(): List<MelDefect> = defects
}
