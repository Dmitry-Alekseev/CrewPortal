package com.example.crewportal.data.repository

import com.example.crewportal.data.fleet.AircraftPool
import com.example.crewportal.data.fleet.AircraftTypeCatalog
import com.example.crewportal.data.local.FleetAircraftDao
import com.example.crewportal.data.local.FleetAircraftEntity
import kotlinx.coroutines.flow.Flow

class FleetRepository(private val dao: FleetAircraftDao) {
    fun observeFleet(): Flow<List<FleetAircraftEntity>> = dao.observeAll()

    suspend fun initialize() {
        dao.insertSeed(AircraftPool.aircraft.map { item ->
            FleetAircraftEntity(
                item.registration, item.label, item.fullName, item.routeClass,
                item.configuration, item.engineType, item.age, item.status
            )
        })
    }

    suspend fun assignFor(aircraftLabel: String, routeClass: String, flightId: String): FleetAircraftEntity? {
        initialize()
        val all = dao.getAllOnce()
        val required = normalizeLabel(aircraftLabel)
        val sameType = all.filter { normalizeLabel(it.label) == required }
        val compatible = sameType.filter { item ->
            when (routeClass) {
                "LONG" -> item.routeClass.contains("LONG")
                "MEDIUM" -> item.routeClass.contains("MEDIUM") || item.routeClass.contains("LONG")
                else -> item.routeClass.contains("SHORT")
            }
        }.ifEmpty { sameType }
        val pool = compatible.ifEmpty { all.filter { it.label == "A320" } }
        if (sameType.isEmpty()) return null // A future/new type is unavailable until its delivery is completed.
        return pool[Math.floorMod(flightId.hashCode(), pool.size)]
    }

    suspend fun addDeliveredAircraft(
        registration: String,
        aircraftLabel: String,
        sourceFlightId: String,
        deliveredAtEpochMillis: Long
    ) {
        val normalizedRegistration = registration.trim().uppercase()
        require(HS_REGISTRATION.matches(normalizedRegistration)) { "Registration must start with HS- and contain 2-6 letters/digits" }
        val spec = AircraftTypeCatalog.byLabel(aircraftLabel)
        dao.upsert(
            FleetAircraftEntity(
                registration = normalizedRegistration,
                label = spec.label,
                fullName = spec.fullName,
                routeClass = spec.routeClass,
                configuration = spec.configuration,
                engineType = spec.engineType,
                age = "0y",
                status = "Active • Delivered",
                deliveredAtEpochMillis = deliveredAtEpochMillis,
                sourceFlightId = sourceFlightId
            )
        )
    }

    companion object {
        val HS_REGISTRATION = Regex("^HS-[A-Z0-9]{2,6}$")

        private fun normalizeLabel(label: String): String = when {
            label.contains("A330-8", true) -> "A330-800neo"
            label.equals("A330neo", true) || label.contains("A330-9", true) -> "A330-900neo"
            label.startsWith("A350", true) -> "A350-900"
            label.startsWith("A330", true) -> "A330-300"
            label.startsWith("A321", true) -> "A321neo"
            label.contains("NEO", true) && label.startsWith("A320", true) -> "A320neo"
            label.startsWith("A320", true) -> "A320"
            else -> label
        }
    }
}
