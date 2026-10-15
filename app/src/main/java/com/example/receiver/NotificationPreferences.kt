package com.example.receiver

import android.content.Context
import android.content.SharedPreferences

object NotificationPreferences {
    private const val PREFS_NAME = "attendtrack_notification_prefs"
    private const val KEY_CLASS_REMINDERS_ENABLED = "key_class_reminders_enabled"
    private const val KEY_END_OF_DAY_REMINDER_ENABLED = "key_end_of_day_reminder_enabled"
    private const val KEY_END_OF_DAY_HOUR = "key_end_of_day_hour"
    private const val KEY_END_OF_DAY_MINUTE = "key_end_of_day_minute"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isClassRemindersEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CLASS_REMINDERS_ENABLED, true)
    }

    fun setClassRemindersEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CLASS_REMINDERS_ENABLED, enabled).apply()
    }

    fun isEndOfDayReminderEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_END_OF_DAY_REMINDER_ENABLED, true)
    }

    fun setEndOfDayReminderEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_END_OF_DAY_REMINDER_ENABLED, enabled).apply()
    }

    fun getEndOfDayHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_END_OF_DAY_HOUR, 20) // 8:00 PM default
    }

    fun getEndOfDayMinute(context: Context): Int {
        return getPrefs(context).getInt(KEY_END_OF_DAY_MINUTE, 0)
    }

    fun setEndOfDayTime(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt(KEY_END_OF_DAY_HOUR, hour)
            .putInt(KEY_END_OF_DAY_MINUTE, minute)
            .apply()
    }
}
