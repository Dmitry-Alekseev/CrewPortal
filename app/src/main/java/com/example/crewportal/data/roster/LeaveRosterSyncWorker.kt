package com.example.crewportal.data.roster

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.crewportal.data.local.AppDatabase
import com.example.crewportal.data.repository.FleetRepository
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.LeaveRepository
import com.example.crewportal.data.repository.PreferencesRepository
import java.util.concurrent.TimeUnit

/** Applies persisted approved leave to the physical roster after crew-planning approval delay. */
class LeaveRosterSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val db = AppDatabase.get(applicationContext)
        val preferences = PreferencesRepository(applicationContext)
        val fleet = FleetRepository(db.fleetAircraftDao())
        fleet.initialize()
        LeaveRepository(applicationContext, db.leavePeriodDao()).initialize()
        FlightRepository(applicationContext, db.flightDao(), preferences, fleet)
            .reconcileRosterWithApprovedLeave(showNotification = true)
    }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}

object LeaveRosterSyncScheduler {
    private const val WORK_NAME = "crew-portal-approved-leave-roster-sync"
    private const val APPROVAL_DELAY_MINUTES = 5L

    fun schedule(context: Context, replaceExisting: Boolean = true) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<LeaveRosterSyncWorker>()
                .setInitialDelay(APPROVAL_DELAY_MINUTES, TimeUnit.MINUTES)
                .build()
        )
    }
}
