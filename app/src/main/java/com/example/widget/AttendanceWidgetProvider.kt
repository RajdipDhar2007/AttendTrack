package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.AttendTrackApp
import com.example.MainActivity
import com.example.R
import com.example.data.model.OverallAttendanceStat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Home-screen widget showing overall attendance, safe bunks and classes needed. */
class AttendanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stat = loadStat()
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, buildViews(context, stat)) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val GOOD = Color.parseColor("#FF4ADE80")
        private val WARN = Color.parseColor("#FFFB7185")

        private suspend fun loadStat(): OverallAttendanceStat? = try {
            AttendTrackApp.instance.repository.getOverallStat().first()
        } catch (_: Exception) {
            null
        }

        /** Re-render every placed widget. Safe to call from any thread. */
        fun refresh(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, AttendanceWidgetProvider::class.java))
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                val stat = loadStat()
                ids.forEach { manager.updateAppWidget(it, buildViews(appContext, stat)) }
            }
        }

        private fun buildViews(context: Context, stat: OverallAttendanceStat?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_attendance)

            if (stat == null || stat.totalEffective == 0) {
                views.setTextViewText(R.id.widget_percent, "--")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
                views.setTextViewText(R.id.widget_status, "No classes marked yet")
                views.setTextViewText(R.id.widget_detail, "Tap to open AttendTrack")
                views.setTextColor(R.id.widget_percent, Color.WHITE)
            } else {
                val ok = stat.percentage >= stat.targetPercentage
                views.setTextViewText(R.id.widget_percent, String.format("%.1f%%", stat.percentage))
                views.setTextColor(R.id.widget_percent, if (ok) GOOD else WARN)
                views.setProgressBar(R.id.widget_progress, 100, stat.percentage.toInt().coerceIn(0, 100), false)
                views.setTextViewText(
                    R.id.widget_status,
                    if (ok) "You can safely bunk ${stat.safeBunks} class${if (stat.safeBunks == 1) "" else "es"}"
                    else "Attend the next ${stat.classesNeeded} class${if (stat.classesNeeded == 1) "" else "es"} to hit ${stat.targetPercentage.toInt()}%"
                )
                views.setTextViewText(
                    R.id.widget_detail,
                    "${stat.totalPresent} present · ${stat.totalAbsent} missed · target ${stat.targetPercentage.toInt()}%"
                )
            }

            val launch = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, launch)
            return views
        }
    }
}
