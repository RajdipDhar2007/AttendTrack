package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ClassAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val slotId = intent.getLongExtra(AlarmScheduler.EXTRA_SLOT_ID, -1L)
        val subjectId = intent.getLongExtra(AlarmScheduler.EXTRA_SUBJECT_ID, -1L)
        val teacherId = intent.getLongExtra(AlarmScheduler.EXTRA_TEACHER_ID, -1L)
        val subjectName = intent.getStringExtra(AlarmScheduler.EXTRA_SUBJECT_NAME) ?: "Upcoming Class"
        val classType = intent.getStringExtra(AlarmScheduler.EXTRA_CLASS_TYPE) ?: "THEORY"
        val room = intent.getStringExtra(AlarmScheduler.EXTRA_ROOM) ?: ""
        val startTime = intent.getStringExtra(AlarmScheduler.EXTRA_START_TIME) ?: ""
        val teacherName = intent.getStringExtra(AlarmScheduler.EXTRA_TEACHER_NAME) ?: ""

        val todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            slotId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Action 1: Present
        val presentIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_ATTENDANCE
            putExtra(NotificationActionReceiver.EXTRA_STATUS, "PRESENT")
            putExtra(NotificationActionReceiver.EXTRA_SLOT_ID, slotId)
            putExtra(NotificationActionReceiver.EXTRA_SUBJECT_ID, subjectId)
            putExtra(NotificationActionReceiver.EXTRA_TEACHER_ID, teacherId)
            putExtra(NotificationActionReceiver.EXTRA_CLASS_TYPE, classType)
            putExtra(NotificationActionReceiver.EXTRA_DATE, todayDate)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, slotId.toInt())
        }
        val presentPending = PendingIntent.getBroadcast(
            context,
            (slotId * 10 + 1).toInt(),
            presentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Action 2: Absent
        val absentIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_ATTENDANCE
            putExtra(NotificationActionReceiver.EXTRA_STATUS, "ABSENT")
            putExtra(NotificationActionReceiver.EXTRA_SLOT_ID, slotId)
            putExtra(NotificationActionReceiver.EXTRA_SUBJECT_ID, subjectId)
            putExtra(NotificationActionReceiver.EXTRA_TEACHER_ID, teacherId)
            putExtra(NotificationActionReceiver.EXTRA_CLASS_TYPE, classType)
            putExtra(NotificationActionReceiver.EXTRA_DATE, todayDate)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, slotId.toInt())
        }
        val absentPending = PendingIntent.getBroadcast(
            context,
            (slotId * 10 + 2).toInt(),
            absentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Action 3: Cancel
        val cancelIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_ATTENDANCE
            putExtra(NotificationActionReceiver.EXTRA_STATUS, "CANCELLED")
            putExtra(NotificationActionReceiver.EXTRA_SLOT_ID, slotId)
            putExtra(NotificationActionReceiver.EXTRA_SUBJECT_ID, subjectId)
            putExtra(NotificationActionReceiver.EXTRA_TEACHER_ID, teacherId)
            putExtra(NotificationActionReceiver.EXTRA_CLASS_TYPE, classType)
            putExtra(NotificationActionReceiver.EXTRA_DATE, todayDate)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, slotId.toInt())
        }
        val cancelPending = PendingIntent.getBroadcast(
            context,
            (slotId * 10 + 3).toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val roomInfo = if (room.isNotBlank()) " | Room $room" else ""
        val teacherInfo = if (teacherName.isNotBlank()) " | $teacherName" else ""
        val displayStart = com.example.data.model.TimetableSlot.formatTo12Hour(startTime)
        val subtitle = "Starts at $displayStart$roomInfo$teacherInfo"

        val notification = NotificationCompat.Builder(context, "class_alerts_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("[$classType] $subjectName in 5 mins")
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Class starting soon: $subjectName ($classType).\n$subtitle\nMark your attendance directly below:"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Present", presentPending)
            .addAction(0, "Absent", absentPending)
            .addAction(0, "Cancelled", cancelPending)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(slotId.toInt(), notification)
    }
}
