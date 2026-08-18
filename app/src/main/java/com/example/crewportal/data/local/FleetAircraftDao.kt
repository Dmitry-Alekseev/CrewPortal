package com.example.crewportal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FleetAircraftDao {
    @Query("SELECT * FROM fleet_aircraft ORDER BY label, registration")
    fun observeAll(): Flow<List<FleetAircraftEntity>>

    @Query("SELECT * FROM fleet_aircraft ORDER BY label, registration")
    suspend fun getAllOnce(): List<FleetAircraftEntity>

    @Query("SELECT COUNT(*) FROM fleet_aircraft")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSeed(aircraft: List<FleetAircraftEntity>)

    @Query("DELETE FROM fleet_aircraft WHERE sourceFlightId IS NULL AND registration NOT IN (:activeRegistrations)")
    suspend fun deleteRetiredSeedAircraft(activeRegistrations: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(aircraft: FleetAircraftEntity)
}
