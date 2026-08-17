package com.example.crewportal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogbookEntryDao {
    @Query("SELECT * FROM electronic_logbook_entries WHERE flightId = :flightId LIMIT 1")
    fun observeByFlightId(flightId: String): Flow<LogbookEntryEntity?>

    @Query("SELECT * FROM electronic_logbook_entries ORDER BY date DESC, departureTime DESC")
    fun observeAll(): Flow<List<LogbookEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: LogbookEntryEntity)
}
