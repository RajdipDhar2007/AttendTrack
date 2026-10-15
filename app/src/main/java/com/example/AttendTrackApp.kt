package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.AttendTrackDatabase
import com.example.data.repository.AttendanceRepository
import com.example.receiver.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AttendTrackApp : Application() {

    lateinit var database: AttendTrackDatabase
        private set

    lateinit var repository: AttendanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AttendTrackDatabase.getInstance(this)
        repository = AttendanceRepository(database.dao(), this)

        createNotificationChannels()
        AlarmScheduler.scheduleAllUpcomingClassAlarms(this)
        AlarmScheduler.scheduleEndOfDayReminder(this)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            // Fresh install: blank profile so the onboarding/login screen is shown first (no demo data)
            if (repository.getProfile() == null) {
                repository.saveProfile(com.example.data.model.Profile(id = 1, hasCompletedOnboarding = false))
            }
            repository.cleanAndDeduplicateSubjects()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val classChannel = NotificationChannel(
                "class_alerts_channel",
                "Upcoming Class Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pre-class reminder alerts with quick attendance action buttons"
                enableVibration(true)
            }
            manager.createNotificationChannel(classChannel)

            val dailyChannel = NotificationChannel(
                "daily_reminder_channel",
                "Daily End-of-Day Attendance Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily evening reminder to review and log attendance before day ends"
                enableVibration(true)
            }
            manager.createNotificationChannel(dailyChannel)
        }
    }

    companion object {
        lateinit var instance: AttendTrackApp
            private set
    }
}
