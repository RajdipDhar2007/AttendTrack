package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AttendTrackDatabase
import com.example.data.model.TimetableSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AlarmScheduler {

    const val ACTION_CLASS_REMINDER = "com.example.attendtrack.ACTION_CLASS_REMINDER"
    const val ACTION_END_OF_DAY_REMINDER = "com.example.attendtrack.ACTION_END_OF_DAY_REMINDER"

    const val EXTRA_SLOT_ID = "extra_slot_id"
    const val EXTRA_SUBJECT_NAME = "extra_subject_name"
    const val EXTRA_CLASS_TYPE = "extra_class_type"
    const val EXTRA_ROOM = "extra_room"
    const val EXTRA_START_TIME = "extra_start_time"
    const val EXTRA_TEACHER_NAME = "extra_teacher_name"
    const val EXTRA_SUBJECT_ID = "extra_subject_id"
    const val EXTRA_TEACHER_ID = "extra_teacher_id"

    private const val REQUEST_CODE_END_OF_DAY = 99999

    /**
     * Schedules alarms for all classes remaining today (5 mins before each class starts).
     */
    fun scheduleAllUpcomingClassAlarms(context: Context) {
        if (!NotificationPreferences.isClassRemindersEnabled(context)) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AttendTrackDatabase.getInstance(context)
                val dao = db.dao()
                val today = LocalDate.now()
                val dayOfWeek = today.dayOfWeek.value
                val todaySlots = dao.getSlotsForDaySync(dayOfWeek)
                val subjects = mutableMapOf<Long, String>()
                val teachers = mutableMapOf<Long, String>()

                todaySlots.forEach { slot ->
                    val sub = dao.getSubjectById(slot.subjectId)
                    if (sub != null) subjects[sub.id] = sub.name
                    val teach = slot.teacherId?.let { dao.getTeacherById(it) }
                    if (teach != null) teachers[teach.id] = teach.name
                    scheduleSlotAlarm(
                        context = context,
                        slot = slot,
                        subjectName = subjects[slot.subjectId] ?: "Class",
                        teacherName = teachers[slot.teacherId] ?: "",
                        targetDate = today
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    fun scheduleSlotAlarm(
        context: Context,
        slot: TimetableSlot,
        subjectName: String,
        teacherName: String,
        targetDate: LocalDate
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 5 minutes before class start time
        val slotTime = slot.getStartLocalTime()
        val alarmDateTime = LocalDateTime.of(targetDate, slotTime).minusMinutes(5)
        val now = LocalDateTime.now()

        if (alarmDateTime.isBefore(now)) {
            // Class has already passed or started
            return
        }

        val triggerEpochMillis = alarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_CLASS_REMINDER
            putExtra(EXTRA_SLOT_ID, slot.id)
            putExtra(EXTRA_SUBJECT_ID, slot.subjectId)
            putExtra(EXTRA_TEACHER_ID, slot.teacherId ?: -1L)
            putExtra(EXTRA_SUBJECT_NAME, subjectName)
            putExtra(EXTRA_CLASS_TYPE, slot.classType.name)
            putExtra(EXTRA_ROOM, slot.room)
            putExtra(EXTRA_START_TIME, slot.startTime)
            putExtra(EXTRA_TEACHER_NAME, teacherName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slot.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(alarmManager, triggerEpochMillis, pendingIntent)
    }

    /**
     * Cancels scheduled alarms for all slots today.
     */
    fun cancelAllClassAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AttendTrackDatabase.getInstance(context)
                val dao = db.dao()
                val slots = dao.getAllSlotsSync()
                slots.forEach { slot ->
                    val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
                        action = ACTION_CLASS_REMINDER
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        slot.id.toInt(),
                        intent,
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (pendingIntent != null) {
                        alarmManager.cancel(pendingIntent)
                        pendingIntent.cancel()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Schedules the daily end-of-day attendance review reminder.
     */
    fun scheduleEndOfDayReminder(context: Context) {
        if (!NotificationPreferences.isEndOfDayReminderEnabled(context)) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val hour = NotificationPreferences.getEndOfDayHour(context)
        val minute = NotificationPreferences.getEndOfDayMinute(context)

        val now = LocalDateTime.now()
        var scheduledTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, minute))

        // If today's time has already elapsed, schedule for tomorrow
        if (scheduledTime.isBefore(now) || scheduledTime.isEqual(now)) {
            scheduledTime = scheduledTime.plusDays(1)
        }

        val triggerEpochMillis = scheduledTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, EndOfDayAlarmReceiver::class.java).apply {
            action = ACTION_END_OF_DAY_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_END_OF_DAY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(alarmManager, triggerEpochMillis, pendingIntent)
    }

    /**
     * Cancels the end-of-day reminder alarm.
     */
    fun cancelEndOfDayReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, EndOfDayAlarmReceiver::class.java).apply {
            action = ACTION_END_OF_DAY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_END_OF_DAY,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Sends an immediate test notification for an upcoming scheduled class
     * with quick action buttons (Present, Absent, Cancelled).
     */
    fun triggerTestClassNotification(context: Context) {
        val nowStr = LocalTime.now().plusMinutes(5).format(DateTimeFormatter.ofPattern("hh:mm a"))
        val testIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_CLASS_REMINDER
            putExtra(EXTRA_SLOT_ID, 9991L)
            putExtra(EXTRA_SUBJECT_ID, 1L)
            putExtra(EXTRA_TEACHER_ID, 1L)
            putExtra(EXTRA_SUBJECT_NAME, "Database Systems (Demo)")
            putExtra(EXTRA_CLASS_TYPE, "THEORY")
            putExtra(EXTRA_ROOM, "CS-302")
            putExtra(EXTRA_START_TIME, nowStr)
            putExtra(EXTRA_TEACHER_NAME, "Dr. Sarah Adams")
        }
        context.sendBroadcast(testIntent)
    }

    /**
     * Sends an immediate test end-of-day reminder notification.
     */
    fun triggerTestEndOfDayNotification(context: Context) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            EndOfDayAlarmReceiver.NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, EndOfDayAlarmReceiver.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ 2 Unmarked Classes Today (Test)")
            .setContentText("Don't forget to mark your attendance before the day ends!")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You have 2 scheduled classes today with pending attendance.\n\nTap below to open your Dashboard, review your subjects, and protect your 75% target threshold!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "Open Dashboard", openAppPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(EndOfDayAlarmReceiver.NOTIFICATION_ID, notification)
    }

    private fun setAlarmSafely(
        alarmManager: AlarmManager,
        triggerEpochMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            } catch (_: Exception) {
            }
        }
    }
}
