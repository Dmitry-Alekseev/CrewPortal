package com.example.crewportal.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/** Preserves the v3 flight roster while adding persistent operational modules. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE flights ADD COLUMN departureEpochMillis INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE flights ADD COLUMN arrivalEpochMillis INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE flights ADD COLUMN isAircraftDelivery INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE flights ADD COLUMN deliveryAircraftType TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE flights ADD COLUMN deliveryProcessed INTEGER NOT NULL DEFAULT 0")

        db.execSQL(
            """CREATE TABLE IF NOT EXISTS electronic_logbook_entries (
                flightId TEXT NOT NULL PRIMARY KEY,
                date TEXT NOT NULL,
                flightNumber TEXT NOT NULL,
                departurePlace TEXT NOT NULL,
                departureTime TEXT NOT NULL,
                arrivalPlace TEXT NOT NULL,
                arrivalTime TEXT NOT NULL,
                aircraftType TEXT NOT NULL,
                registration TEXT NOT NULL,
                picName TEXT NOT NULL,
                pilotFunction TEXT NOT NULL,
                totalTimeMinutes INTEGER NOT NULL,
                picMinutes INTEGER NOT NULL,
                sicMinutes INTEGER NOT NULL,
                nightMinutes INTEGER NOT NULL,
                ifrMinutes INTEGER NOT NULL,
                instrumentMinutes INTEGER NOT NULL,
                takeoffsDay INTEGER NOT NULL,
                takeoffsNight INTEGER NOT NULL,
                landingsDay INTEGER NOT NULL,
                landingsNight INTEGER NOT NULL,
                approaches INTEGER NOT NULL,
                remarks TEXT NOT NULL,
                signatureName TEXT NOT NULL,
                certifiedAtEpochMillis INTEGER,
                updatedAtEpochMillis INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS fleet_aircraft (
                registration TEXT NOT NULL PRIMARY KEY,
                label TEXT NOT NULL,
                fullName TEXT NOT NULL,
                routeClass TEXT NOT NULL,
                configuration TEXT NOT NULL,
                engineType TEXT NOT NULL,
                age TEXT NOT NULL,
                status TEXT NOT NULL,
                deliveredAtEpochMillis INTEGER,
                sourceFlightId TEXT
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS leave_periods (
                id TEXT NOT NULL PRIMARY KEY,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                startDate TEXT NOT NULL,
                endDate TEXT NOT NULL,
                status TEXT NOT NULL,
                note TEXT NOT NULL
            )""".trimIndent()
        )
    }
}

/** Adds generator provenance and linked qualification-event metadata without rewriting roster rows. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE flights ADD COLUMN rosterSource TEXT NOT NULL DEFAULT 'AUTO_GENERATED'")
        db.execSQL("ALTER TABLE flights ADD COLUMN eventGroupId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE flights ADD COLUMN eventDayIndex INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE flights ADD COLUMN eventTotalDays INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE flights ADD COLUMN lineCheckRole TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE flights ADD COLUMN flightTimeCreditEligible INTEGER NOT NULL DEFAULT 1")
        db.execSQL("UPDATE flights SET rosterSource = 'DELIVERY' WHERE isAircraftDelivery = 1")
        db.execSQL("UPDATE flights SET rosterSource = 'OPERATIONAL_CHANGE' WHERE dutyNote LIKE '%Manual operational roster change%'")
        Log.d("CrewRoster", "Room migration 4 to 5 completed")
    }
}
