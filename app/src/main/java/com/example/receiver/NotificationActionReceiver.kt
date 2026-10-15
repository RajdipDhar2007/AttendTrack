package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.local.AttendTrackDatabase
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassType
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_ATTENDANCE = "com.example.attendtrack.ACTION_MARK_ATTENDANCE"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_SLOT_ID = "extra_slot_id"
        const val EXTRA_SUBJECT_ID = "extra_subject_id"
        const val EXTRA_TEACHER_ID = "extra_teacher_id"
        const val EXTRA_CLASS_TYPE = "extra_class_type"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_ATTENDANCE) return

        val statusStr = intent.getStringExtra(EXTRA_STATUS) ?: return
        val slotId = intent.getLongExtra(EXTRA_SLOT_ID, -1L)
        val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1L)
        val teacherIdRaw = intent.getLongExtra(EXTRA_TEACHER_ID, -1L)
        val teacherId = if (teacherIdRaw > 0) teacherIdRaw else null
        val classTypeStr = intent.getStringExtra(EXTRA_CLASS_TYPE) ?: "THEORY"
        val date = intent.getStringExtra(EXTRA_DATE) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        val status = try {
            AttendanceStatus.valueOf(statusStr)
        } catch (_: Exception) {
            AttendanceStatus.PRESENT
        }

        val classType = try {
            ClassType.valueOf(classTypeStr)
        } catch (_: Exception) {
            ClassType.THEORY
        }

        // Cancel the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(notificationId)

        // Write directly to Room DB on IO dispatcher
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AttendTrackDatabase.getInstance(context)
                val repo = AttendanceRepository(db.dao())
                repo.markAttendance(
                    date = date,
                    slotId = if (slotId > 0) slotId else null,
                    subjectId = subjectId,
                    teacherId = teacherId,
                    classType = classType,
                    status = status,
                    note = "Marked via Quick Notification"
                )
                withContext(Dispatchers.Main) {
                    val statusLabel = when (status) {
                        AttendanceStatus.PRESENT -> "Marked Present"
                        AttendanceStatus.ABSENT -> "Marked Absent"
                        AttendanceStatus.CANCELLED -> "Marked Cancelled"
                    }
                    Toast.makeText(context, "AttendTrack: $statusLabel", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
