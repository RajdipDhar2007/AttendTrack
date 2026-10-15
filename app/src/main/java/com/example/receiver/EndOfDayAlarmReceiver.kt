package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AttendTrackDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class EndOfDayAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // If disabled in preferences, don't show notification
        if (!NotificationPreferences.isEndOfDayReminderEnabled(context)) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AttendTrackDatabase.getInstance(context)
                val dao = db.dao()

                val today = LocalDate.now()
                val dayOfWeek = today.dayOfWeek.value
                val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                val scheduledSlots = dao.getSlotsForDaySync(dayOfWeek)
                val markedRecords = dao.getAttendanceForDateSync(todayStr)
                val markedSlotIds = markedRecords.mapNotNull { it.slotId }.toSet()

                val unmarkedSlots = scheduledSlots.filter { it.id !in markedSlotIds }
                val unmarkedCount = unmarkedSlots.size

                val notificationTitle: String
                val notificationBody: String
                val bigSummary: String

                when {
                    unmarkedCount > 0 -> {
                        notificationTitle = "⚠️ $unmarkedCount Unmarked Class${if (unmarkedCount > 1) "es" else ""} Today"
                        notificationBody = "Don't forget to mark your attendance before the day ends!"
                        bigSummary = "You had $unmarkedCount scheduled class${if (unmarkedCount > 1) "es" else ""} today with missing attendance records.\n\nTap to mark them now and keep your minimum attendance percentage protected!"
                    }
                    scheduledSlots.isNotEmpty() -> {
                        notificationTitle = "✅ Daily Attendance Complete!"
                        notificationBody = "All ${scheduledSlots.size} classes today are accounted for."
                        bigSummary = "Awesome job! You have logged all ${scheduledSlots.size} scheduled classes for today.\n\nReview your attendance goals and tomorrow's timetable in AttendTrack."
                    }
                    else -> {
                        notificationTitle = "📅 End of Day Reminder"
                        notificationBody = "Keep your attendance stats up to date."
                        bigSummary = "No timetable classes were scheduled today. Check your study goals, prepare for upcoming exams, or log any extra sessions in AttendTrack!"
                    }
                }

                withContext(Dispatchers.Main) {
                    showNotification(
                        context = context,
                        title = notificationTitle,
                        shortText = notificationBody,
                        bigText = bigSummary
                    )
                }

                // Reschedule for next day
                AlarmScheduler.scheduleEndOfDayReminder(context)
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        title: String,
        shortText: String,
        bigText: String
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "Open Dashboard", openAppPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 88888
        const val CHANNEL_ID = "daily_reminder_channel"
    }
}
