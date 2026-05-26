package com.example.crewportal.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RosterAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Crew Portal"
        val message = intent.getStringExtra("message") ?: "Roster event"
        val id = intent.getIntExtra("notificationId", title.hashCode())
        val destination = intent.getStringExtra("destination") ?: "alerts"
        NotificationHelper.show(context, title, message, id, destination)
    }
}
