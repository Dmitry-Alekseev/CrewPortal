package com.example.crewportal.data.roster

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.crewportal.data.local.AppDatabase
import com.example.crewportal.data.repository.FleetRepository
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.LeaveRepository
import com.example.crewportal.data.repository.PreferencesRepository
import java.util.concurrent.TimeUnit

class NextRosterWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val db = AppDatabase.get(applicationContext)
        val preferences = PreferencesRepository(applicationContext)
        val fleet = FleetRepository(db.fleetAircraftDao())
        fleet.initialize()
        // WorkManager can start in a fresh process, so load the same persisted leave source
        // used by the UI before generating a roster. Approved leave always wins over duty.
        LeaveRepository(db.leavePeriodDao()).initialize()
        FlightRepository(applicationContext, db.flightDao(), preferences, fleet).prepareNextMonthRosterIfDue()
    }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}

object NextRosterScheduler {
    private const val PERIODIC_WORK = "crew-portal-next-roster-daily"
    private const val IMMEDIATE_WORK = "crew-portal-next-roster-startup"

    fun schedule(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<NextRosterWorker>(1, TimeUnit.DAYS).build()
        )
        manager.enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<NextRosterWorker>().build()
        )
    }
}
