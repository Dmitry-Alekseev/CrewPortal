package com.example.crewportal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LeavePeriodDao {
    @Query("SELECT * FROM leave_periods ORDER BY startDate")
    fun observeAll(): Flow<List<LeavePeriodEntity>>

    @Query("SELECT * FROM leave_periods ORDER BY startDate")
    suspend fun getAllOnce(): List<LeavePeriodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(period: LeavePeriodEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSeed(periods: List<LeavePeriodEntity>)
}
