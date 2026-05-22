package com.example.crewportal.data.repository

import android.content.Context
import com.example.crewportal.data.local.FlightDao
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.util.hasArrived
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class FlightRepository(
    private val context: Context,
    private val flightDao: FlightDao,
    private val preferencesRepository: PreferencesRepository
) {
    fun observeFlights(): Flow<List<FlightEntity>> = flightDao.observeAll()
    fun observeCompleted(): Flow<List<FlightEntity>> = flightDao.observeCompleted()
    fun observeFlight(id: String): Flow<FlightEntity?> = flightDao.observeById(id)

    suspend fun loadScheduleFromAssetsIfNeeded() {
        if (flightDao.count() > 0) return
        loadScheduleFromAssets(clearExisting = false)
    }

    suspend fun reloadScheduleFromAssets() {
        loadScheduleFromAssets(clearExisting = true)
    }

    private suspend fun loadScheduleFromAssets(clearExisting: Boolean) {
        if (clearExisting) flightDao.clearAll()
        val json = context.assets.open("schedule.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val array = root.getJSONArray("flights")
        val flights = buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    FlightEntity(
                        id = item.getString("id"),
                        airline = item.getString("airline"),
                        flightNumber = item.getString("flightNumber"),
                        aircraftLabel = item.getString("aircraftLabel"),
                        aircraftFullName = item.getString("aircraftFullName"),
                        registration = item.getString("registration"),
                        status = item.getString("status"),
                        departureIata = item.getString("departureIata"),
                        departureIcao = item.getString("departureIcao"),
                        departureCity = item.getString("departureCity"),
                        departureAirport = item.getString("departureAirport"),
                        arrivalIata = item.getString("arrivalIata"),
                        arrivalIcao = item.getString("arrivalIcao"),
                        arrivalCity = item.getString("arrivalCity"),
                        arrivalAirport = item.getString("arrivalAirport"),
                        departureDateTime = item.getString("departureDateTime"),
                        arrivalDateTime = item.getString("arrivalDateTime"),
                        durationMinutes = item.getInt("durationMinutes")
                    )
                )
            }
        }
        flightDao.insertAll(flights)
    }

    suspend fun registerFlight(id: String) = flightDao.markRegistered(id)

    suspend fun refreshCompletedFlights() {
        flightDao.getAllOnce().forEach { flight ->
            if (!flight.isFlightTimeAdded && hasArrived(flight.arrivalDateTime)) {
                flightDao.markCompletedAndAdded(flight.id)
                preferencesRepository.addFlightTime(flight.durationMinutes)
            }
        }
    }
}
