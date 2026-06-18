package com.example.crewportal.data.mel

import com.example.crewportal.data.fleet.AircraftPool
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.random.Random

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
    const val githubMelUrl: String = ""

    private data class MelTemplate(
        val melItem: String,
        val ata: String,
        val category: String,
        val description: String,
        val operationalLimitation: String,
        val plannedAction: String,
        val families: Set<String> = setOf("A320", "A330", "A350")
    )

    fun forAircraft(registration: String, month: YearMonth = YearMonth.now()): List<MelDefect> =
        generateForAircraft(registration, month)

    fun all(): List<MelDefect> = AircraftPool.aircraft.flatMap { generateForAircraft(it.registration, YearMonth.now()) }

    private fun generateForAircraft(registration: String, month: YearMonth): List<MelDefect> {
        if (registration.isBlank() || registration == "TBA" || registration == "—") return emptyList()
        val aircraft = AircraftPool.byRegistration(registration)
        val family = when {
            aircraft?.label?.contains("A350") == true -> "A350"
            aircraft?.label?.contains("A330") == true -> "A330"
            else -> "A320"
        }
        val random = Random((registration + month.toString() + "mel2.1").hashCode())
        val count = itemCountFor(family, random.nextInt(100))
        if (count == 0) return emptyList()
        val pool = templates.filter { family in it.families }.shuffled(random)
        val reportedBase = month.atDay(random.nextInt(1, minOf(18, month.lengthOfMonth()) + 1))
        return pool.take(count).mapIndexed { index, item ->
            val reported = reportedBase.minusDays(index.toLong() * 2)
            val due = reported.plusDays(when (item.category) { "B" -> 3L; "C" -> 10L; else -> 30L })
            MelDefect(
                aircraftRegistration = registration,
                defectId = "TG-MEL-${month.year}-${abs((registration + item.melItem + month).hashCode()) % 9000 + 1000}",
                melItem = item.melItem,
                ata = item.ata,
                category = item.category,
                status = "Deferred",
                reportedDate = reported.format(dateFormatter).uppercase(),
                description = item.description,
                operationalLimitation = item.operationalLimitation,
                rectificationDue = due.format(dateFormatter).uppercase(),
                plannedAction = item.plannedAction
            )
        }
    }

    private fun itemCountFor(family: String, roll: Int): Int = when (family) {
        // Keep some aircraft clean, but avoid long runs of empty MEL across different registrations.
        "A350" -> when {
            roll < 42 -> 0
            roll < 82 -> 1
            roll < 96 -> 2
            else -> 3
        }
        "A330" -> when {
            roll < 30 -> 0
            roll < 68 -> 1
            roll < 90 -> 2
            roll < 98 -> 3
            else -> 4
        }
        else -> when {
            roll < 34 -> 0
            roll < 72 -> 1
            roll < 94 -> 2
            else -> 3
        }
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH)

    private val templates = listOf(
        MelTemplate("25-11-01", "ATA 25 Equipment/Furnishings", "D", "Passenger seat recline mechanism inoperative; seat locked upright.", "Seat placarded or blocked in accordance with cabin configuration.", "Replace recline actuator during scheduled cabin maintenance."),
        MelTemplate("25-11-02", "ATA 25 Equipment/Furnishings", "D", "Passenger seat armrest release mechanism inoperative.", "Affected seat placarded. Cabin crew advised.", "Inspect armrest latch and replace release cable."),
        MelTemplate("25-11-03", "ATA 25 Equipment/Furnishings", "D", "Tray table latch defective; tray table secured closed.", "Passenger seat placarded if required. Cabin crew briefing required.", "Replace tray latch assembly at next cabin maintenance slot."),
        MelTemplate("25-11-04", "ATA 25 Equipment/Furnishings", "D", "Passenger footrest inoperative in selected premium seat.", "Comfort item deferred. Seat remains available unless otherwise placarded.", "Inspect footrest motor and control switch."),
        MelTemplate("25-21-01", "ATA 25 Passenger Service Unit", "D", "Passenger reading light inoperative at selected seat row.", "Cabin crew advised. Remaining cabin lighting available.", "Replace PSU lamp module."),
        MelTemplate("25-21-02", "ATA 25 Passenger Service Unit", "D", "Passenger air outlet control stiff/inoperative at selected row.", "Passenger comfort item deferred. No operational restriction.", "Replace gasper outlet control assembly."),
        MelTemplate("25-21-03", "ATA 25 Passenger Service Unit", "D", "Passenger call button inoperative at selected seat.", "Cabin crew advised; alternate call method available.", "Replace PSU switch module."),
        MelTemplate("25-41-01", "ATA 25 Cabin Interior", "D", "Overhead bin latch stiff and placarded for crew attention.", "Bin usable after crew verification; cabin crew advised.", "Adjust latch mechanism at overnight stop."),
        MelTemplate("25-51-01", "ATA 25 Cargo Compartment", "C", "Minor cargo compartment lining damage inspected.", "Damage within dispatch limits; loading team advised to avoid affected panel area.", "Replace damaged lining panel at BKK maintenance."),
        MelTemplate("23-31-01", "ATA 23 Communications", "C", "Cabin interphone handset at aft station intermittent.", "Remaining cabin interphone stations operative. Cabin crew briefing required.", "Replace handset cradle and perform interphone test."),
        MelTemplate("23-12-01", "ATA 23 Communications", "C", "VHF 3 data mode unavailable.", "Voice communication normal. Remaining communication systems operative.", "Replace VHF data interface module."),
        MelTemplate("23-71-01", "ATA 23 SATCOM", "C", "SATCOM data service intermittent.", "Dispatch permitted with alternate communication capability available.", "SATCOM BITE download and antenna coupler check.", setOf("A330", "A350")),
        MelTemplate("24-38-01", "ATA 24 Electrical Power", "C", "Galley power outlet group intermittent on aft galley.", "Affected galley load limited. Cabin crew informed.", "Inspect galley power connector and circuit protection."),
        MelTemplate("24-51-01", "ATA 24 Electrical Power", "C", "One cabin USB charging group inoperative.", "Passenger convenience item deferred. Cabin crew advised.", "Replace cabin power supply module."),
        MelTemplate("31-31-01", "ATA 31 Indicating/Recording", "C", "Cockpit printer inoperative.", "Electronic documentation available. Crew to use alternate reporting procedure.", "Replace printer unit and perform operational test."),
        MelTemplate("31-62-01", "ATA 31 Indicating/Recording", "C", "One display brightness control has limited range.", "Display remains readable. Crew awareness required.", "Inspect brightness potentiometer/control panel."),
        MelTemplate("33-11-01", "ATA 33 Lights", "C", "Taxi light inoperative.", "Dispatch permitted under applicable company and route procedures.", "Replace taxi light assembly at BKK."),
        MelTemplate("33-14-01", "ATA 33 Lights", "C", "Logo light inoperative.", "No operational limitation except exterior lighting note.", "Replace logo light unit."),
        MelTemplate("33-21-01", "ATA 33 Cabin Lights", "D", "One cabin ceiling light zone partially inoperative.", "Cabin lighting remains adequate. Cabin crew advised.", "Replace cabin light power supply."),
        MelTemplate("33-45-02", "ATA 33 Signs", "C", "One passenger information sign legend light inoperative.", "Affected sign placarded. Cabin crew briefing required.", "Replace sign lamp module."),
        MelTemplate("34-41-01", "ATA 34 Navigation", "C", "Weather radar automatic tilt mode unavailable; manual tilt available.", "Manual mode to be used. Crew awareness required.", "Radar control unit BITE and software reload."),
        MelTemplate("34-56-01", "ATA 34 Surveillance", "C", "One ADS-B transponder control channel deferred.", "Remaining transponder capability available. Dispatch per route requirements.", "Replace transponder control panel."),
        MelTemplate("35-21-01", "ATA 35 Oxygen", "C", "Crew oxygen pressure indication secondary channel deferred after test anomaly.", "Primary indication normal. Required preflight check to be completed.", "Replace pressure sensor channel."),
        MelTemplate("38-11-01", "ATA 38 Water/Waste", "D", "Lavatory unavailable; locked and placarded.", "Dispatch permitted subject to passenger load and cabin configuration.", "Troubleshoot flush motor and service lavatory."),
        MelTemplate("38-12-01", "ATA 38 Water/Waste", "D", "Lavatory sink water supply unavailable.", "Lavatory placarded as required. Cabin crew advised.", "Inspect lavatory water shutoff valve."),
        MelTemplate("38-31-01", "ATA 38 Water/Waste", "D", "Waste bin flap spring defective in lavatory.", "Placard installed. Cabin crew monitoring required.", "Replace waste flap spring assembly."),
        MelTemplate("44-21-01", "ATA 44 Cabin Systems", "D", "IFE screen inoperative at selected seat.", "Passenger entertainment item deferred. Seat remains available.", "Replace seat display unit."),
        MelTemplate("44-21-02", "ATA 44 Cabin Systems", "D", "IFE zone controller reset required intermittently.", "Cabin crew advised. Remaining IFE zones available.", "Controller software reload at maintenance base.", setOf("A330", "A350")),
        MelTemplate("46-13-01", "ATA 46 Information Systems", "D", "EFB aircraft interface synchronization unavailable on right side mount.", "Crew EFB standalone operation available. Company briefing remains accessible.", "Data interface unit software reload.", setOf("A330", "A350")),
        MelTemplate("49-11-01", "ATA 49 APU", "C", "APU inoperative.", "External power and ground air required where applicable. Dispatch per station capability.", "APU fault isolation and start system inspection."),
        MelTemplate("49-21-01", "ATA 49 APU", "C", "APU bleed unavailable; APU electrical generation available.", "External air required for engine start or cabin conditioning where applicable.", "Inspect APU bleed valve and control circuit."),
        MelTemplate("21-52-01", "ATA 21 Air Conditioning", "C", "Cabin temperature zone trim air valve position indication intermittent.", "Affected zone to be monitored by crew. Remaining temperature control channels available.", "Valve position feedback check and connector inspection."),
        MelTemplate("21-31-01", "ATA 21 Air Conditioning", "C", "One pack automatic mode limitation; manual control available.", "Crew to monitor cabin temperature and system status.", "Pack controller troubleshooting at overnight stop."),
        MelTemplate("25-33-01", "ATA 25 Galley", "D", "One galley oven inoperative.", "Cabin crew advised. Catering service adjusted.", "Replace oven insert."),
        MelTemplate("25-33-02", "ATA 25 Galley", "D", "Coffee maker inoperative in forward galley.", "Cabin service adjusted. No flight operational restriction.", "Replace coffee maker unit."),
        MelTemplate("25-33-03", "ATA 25 Galley", "D", "Galley chiller cooling performance degraded.", "Affected unit not used for temperature-sensitive items.", "Chiller condenser inspection."),
        MelTemplate("52-71-01", "ATA 52 Doors", "C", "Bulk cargo door indication requires repeat close verification.", "Maintenance release required after close verification procedure.", "Door proximity sensor adjustment."),
        MelTemplate("53-12-01", "ATA 53 Fuselage", "C", "Minor dent on lower fuselage access panel inspected.", "Damage assessed within structural limits. No operational restriction.", "Panel replacement at scheduled check."),
        MelTemplate("53-15-01", "ATA 53 Fuselage", "C", "Paint erosion noted on radome surface; inspected.", "Within permitted limits. Weather radar operation normal.", "Radome repaint/blend repair planned."),
        MelTemplate("57-41-01", "ATA 57 Wings", "C", "Static wick missing within permitted dispatch limits.", "Aircraft released in accordance with maintenance assessment.", "Install replacement static wick."),
        MelTemplate("71-21-01", "ATA 71 Power Plant", "C", "Engine fan cowl latch witness mark requires monitoring.", "Latch inspected and secured. Walkaround attention required.", "Latch adjustment at next maintenance stop."),
        MelTemplate("32-42-01", "ATA 32 Landing Gear", "C", "Brake temperature indication channel intermittent on one wheel.", "Brake cooling procedures and maintenance verification before release.", "Sensor harness inspection during overnight stop."),
        MelTemplate("30-21-01", "ATA 30 Ice/Rain Protection", "B", "Wing anti-ice valve indication slow to transit during post-flight test.", "Maintenance procedure compliance required before dispatch.", "Valve actuator inspection planned before next long-sector assignment."),
        MelTemplate("30-41-01", "ATA 30 Ice/Rain Protection", "C", "One windshield wiper low-speed setting unavailable.", "Remaining speed setting available. Dispatch subject to weather limitations.", "Replace wiper control relay."),
        MelTemplate("46-51-01", "ATA 46 Information Systems", "D", "Cabin maintenance terminal offline.", "No flight deck operational limitation. Maintenance uses alternate access.", "Software reload and network check.", setOf("A350")),
        MelTemplate("44-31-01", "ATA 44 Cabin Network", "D", "Passenger Wi-Fi access point unavailable in one cabin zone.", "Passenger connectivity item deferred.", "Replace wireless access point.", setOf("A350", "A330")),
        MelTemplate("25-76-01", "ATA 25 Crew Rest", "C", "One crew rest seat recline function inoperative.", "Crew rest planning adjusted; remaining approved rest facilities available.", "Replace crew rest seat actuator.", setOf("A330", "A350"))
    )

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
