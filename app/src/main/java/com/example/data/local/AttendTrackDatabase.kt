package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassType
import com.example.data.model.Exam
import com.example.data.model.ExamType
import com.example.data.model.Holiday
import com.example.data.model.Profile
import com.example.data.model.StudyGoal
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.data.model.Teacher
import com.example.data.model.TimetableSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Database(
    entities = [
        Profile::class,
        Subject::class,
        Teacher::class,
        TimetableSlot::class,
        AttendanceRecord::class,
        Exam::class,
        Holiday::class,
        StudyGoal::class,
        StudySession::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AttendTrackDatabase : RoomDatabase() {

    abstract fun dao(): AttendTrackDao

    companion object {
        @Volatile
        private var INSTANCE: AttendTrackDatabase? = null

        fun getInstance(context: Context): AttendTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AttendTrackDatabase::class.java,
                    "attendtrack_db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.dao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: AttendTrackDao) {
                // No demo data: start with a blank profile so onboarding is shown first
                dao.saveProfile(com.example.data.model.Profile(id = 1, hasCompletedOnboarding = false))
            }
        }
    }
}
