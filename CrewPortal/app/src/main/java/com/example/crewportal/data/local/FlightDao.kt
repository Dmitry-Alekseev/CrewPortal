package com.example.crewportal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {
    @Query("SELECT * FROM flights ORDER BY departureDateTime ASC")
    fun observeAll(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE isCompleted = 1 AND dutyType = 'FLIGHT' ORDER BY departureDateTime DESC")
    fun observeCompleted(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<FlightEntity?>

    @Query("SELECT * FROM flights ORDER BY departureDateTime ASC")
    suspend fun getAllOnce(): List<FlightEntity>

    @Query("SELECT COUNT(*) FROM flights")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(flights: List<FlightEntity>)

    @Query("DELETE FROM flights")
    suspend fun clearAll()

    @Query("UPDATE flights SET isRegistered = 1 WHERE id = :id")
    suspend fun markRegistered(id: String)

    @Query("UPDATE flights SET registration = :registration WHERE id = :id")
    suspend fun assignRegistration(id: String, registration: String)

    @Query("UPDATE flights SET registrationNotified = 1 WHERE id = :id")
    suspend fun markRegistrationNotified(id: String)

    @Query("UPDATE flights SET gate = :gate, stand = :stand, terminal = :terminal WHERE id = :id")
    suspend fun assignAirportPosition(id: String, gate: String, stand: String, terminal: String)

    @Query("UPDATE flights SET airportAssignmentNotified = 1 WHERE id = :id")
    suspend fun markAirportAssignmentNotified(id: String)

    @Query("UPDATE flights SET isCompleted = 1, isFlightTimeAdded = 1 WHERE id = :id")
    suspend fun markCompletedAndAdded(id: String)
}
