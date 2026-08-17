package com.example.crewportal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FlightEntity::class, LogbookEntryEntity::class, FleetAircraftEntity::class, LeavePeriodEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
    abstract fun logbookEntryDao(): LogbookEntryDao
    abstract fun fleetAircraftDao(): FleetAircraftDao
    abstract fun leavePeriodDao(): LeavePeriodDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "crew_portal.db"
            ).addMigrations(MIGRATION_3_4).build().also { INSTANCE = it }
        }
    }
}
