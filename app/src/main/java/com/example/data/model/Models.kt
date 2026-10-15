package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class ClassType {
    THEORY,
    LAB
}

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    CANCELLED
}

enum class ExamType {
    MID_TERM,
    FINAL,
    LAB_VIVA,
    QUIZ,
    PRACTICAL
}

enum class AppPalette(val displayName: String) {
    AURORA("Aurora Glow"),
    SUNSET("Sunset Velvet"),
    OCEAN("Ocean Deep"),
    NEON("Cyber Neon"),
    FOREST("Emerald Forest"),
    MONO("Mono Slate")
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("Follow System"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED Pure Black")
}

// 4-Year Academic Curriculum Mapping: Strict Semesters per Year
object AcademicCurriculum {
    val YEARS = listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
    val YEAR_SEMESTER_MAP = mapOf(
        "1st Year" to listOf("Semester 1", "Semester 2"),
        "2nd Year" to listOf("Semester 3", "Semester 4"),
        "3rd Year" to listOf("Semester 5", "Semester 6"),
        "4th Year" to listOf("Semester 7", "Semester 8")
    )

    fun getValidSemestersForYear(year: String): List<String> {
        return YEAR_SEMESTER_MAP[year] ?: listOf("Semester 1", "Semester 2")
    }

    fun getYearForSemester(semester: String): String {
        return when (semester) {
            "Semester 1", "Semester 2" -> "1st Year"
            "Semester 3", "Semester 4" -> "2nd Year"
            "Semester 5", "Semester 6" -> "3rd Year"
            "Semester 7", "Semester 8" -> "4th Year"
            else -> "1st Year"
        }
    }
}

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val institution: String = "",
    val department: String = "",
    val section: String = "",
    val group: String = "",
    val sessionYear: String = "2024-2028",
    val semester: String = "Semester 4",
    val year: String = "2nd Year",
    val targetPercentage: Float = 75f,
    val avatarUri: String? = null,
    val isPinEnabled: Boolean = false,
    val pinCode: String = "",
    val hasCompletedOnboarding: Boolean = false
)

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val colorHex: String = "#6366F1",
    val customTargetPercentage: Float? = null // null means inherit profile target
)

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val cabin: String = "",
    val role: String = "Faculty", // Faculty, Professor, Technical Assistant, Lab Instructor
    val specialization: String = ""
)

@Entity(
    tableName = "timetable_slots",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Teacher::class,
            parentColumns = ["id"],
            childColumns = ["teacherId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("subjectId"), Index("teacherId")]
)
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday (java.time.DayOfWeek.value)
    val subjectId: Long,
    val teacherId: Long? = null,
    val technicalAssistantId: Long? = null,
    val technicalAssistantName: String = "", // e.g. "Mr. David Miller (TA)"
    val classType: ClassType = ClassType.THEORY,
    val room: String = "",
    val startTime: String = "09:00", // "HH:mm"
    val endTime: String = "10:00"    // "HH:mm"
) {
    fun getDisplayStartTime(): String = formatTo12Hour(startTime)
    fun getDisplayEndTime(): String = formatTo12Hour(endTime)
    fun getDisplayTimeSpan(): String = "${getDisplayStartTime()} - ${getDisplayEndTime()}"

    fun getStartLocalTime(): LocalTime = try {
        val s = startTime.trim().uppercase()
        if (s.endsWith("AM") || s.endsWith("PM")) {
            val isPm = s.endsWith("PM")
            val clean = s.replace("AM", "").replace("PM", "").trim()
            val parts = clean.split(":")
            var h = parts[0].trim().toInt()
            val m = if (parts.size > 1) parts[1].trim().toInt() else 0
            if (isPm && h < 12) h += 12
            if (!isPm && h == 12) h = 0
            LocalTime.of(h, m)
        } else {
            LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
        }
    } catch (_: Exception) {
        LocalTime.of(9, 0)
    }

    fun getEndLocalTime(): LocalTime = try {
        val s = endTime.trim().uppercase()
        if (s.endsWith("AM") || s.endsWith("PM")) {
            val isPm = s.endsWith("PM")
            val clean = s.replace("AM", "").replace("PM", "").trim()
            val parts = clean.split(":")
            var h = parts[0].trim().toInt()
            val m = if (parts.size > 1) parts[1].trim().toInt() else 0
            if (isPm && h < 12) h += 12
            if (!isPm && h == 12) h = 0
            LocalTime.of(h, m)
        } else {
            LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))
        }
    } catch (_: Exception) {
        LocalTime.of(10, 0)
    }

    companion object {
        fun formatTo12Hour(raw: String): String {
            val s = raw.trim()
            if (s.isEmpty()) return ""
            val upper = s.uppercase()
            // If it already has AM/PM, normalize it cleanly (e.g. "9:00 AM" or "09:00AM" -> "9:00 AM")
            if (upper.endsWith("AM") || upper.endsWith("PM")) {
                val isPm = upper.endsWith("PM")
                val clean = upper.replace("AM", "").replace("PM", "").trim()
                val parts = clean.split(":")
                val h = parts[0].trim().toIntOrNull() ?: return s
                val m = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 0 else 0
                val displayH = when {
                    h == 0 -> 12
                    h > 12 -> h - 12
                    else -> h
                }
                val amPm = if (isPm || h >= 12) "PM" else "AM"
                return String.format("%d:%02d %s", displayH, m, amPm)
            }
            // If it is in 24-hour format "HH:mm" or "H:mm"
            if (s.contains(":")) {
                val parts = s.split(":")
                val h = parts[0].trim().toIntOrNull() ?: return s
                val m = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 0 else 0
                val isPm = h >= 12
                val displayH = when {
                    h == 0 -> 12
                    h > 12 -> h - 12
                    else -> h
                }
                val amPm = if (isPm) "PM" else "AM"
                return String.format("%d:%02d %s", displayH, m, amPm)
            }
            return s
        }
    }
}

@Entity(
    tableName = "attendance_records",
    indices = [
        Index("date"),
        Index("subjectId"),
        Index("teacherId"),
        Index(value = ["date", "slotId"], unique = false)
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val slotId: Long? = null,
    val subjectId: Long,
    val teacherId: Long? = null,
    val classType: ClassType = ClassType.THEORY,
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val note: String = ""
)

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val date: String, // "YYYY-MM-DD"
    val time: String = "10:00", // "HH:mm"
    val room: String = "",
    val examType: ExamType = ExamType.MID_TERM
)

@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val title: String
)

data class SlotWithDetails(
    val slot: TimetableSlot,
    val subject: Subject,
    val teacher: Teacher?,
    val technicalAssistant: Teacher? = null,
    val todayRecord: AttendanceRecord? = null
)

data class SubjectAttendanceStat(
    val subject: Subject,
    val teacher: Teacher?,
    val classType: ClassType,
    val presentCount: Int,
    val absentCount: Int,
    val cancelledCount: Int,
    val totalCount: Int,
    val effectiveCount: Int, // present + absent
    val percentage: Float,
    val targetPercentage: Float,
    val safeBunks: Int,
    val neededToTarget: Int
)

data class OverallAttendanceStat(
    val totalPresent: Int = 0,
    val totalAbsent: Int = 0,
    val totalCancelled: Int = 0,
    val totalEffective: Int = 0,
    val percentage: Float = 0f,
    val targetPercentage: Float = 75f,
    val safeBunks: Int = 0,
    val classesNeeded: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

data class ImportResult(
    val success: Boolean,
    val message: String,
    val subjectsCount: Int = 0,
    val recordsCount: Int = 0,
    val slotsCount: Int = 0,
    val examsCount: Int = 0,
    val studyGoalsCount: Int = 0,
    val studySessionsCount: Int = 0
)

@Entity(
    tableName = "study_goals",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class StudyGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val targetHours: Float,
    val studiedHours: Float = 0f,
    val deadlineDate: String = "", // "YYYY-MM-DD"
    val isCompleted: Boolean = false,
    val priority: String = "Medium", // High, Medium, Low
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("goalId")]
)
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val goalId: Long? = null,
    val date: String, // "YYYY-MM-DD"
    val durationMinutes: Int, // e.g. 60
    val topicCovered: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SubjectStudyStat(
    val subject: Subject,
    val totalTargetHours: Float,
    val totalStudiedHours: Float,
    val progressPercentage: Float,
    val goalsCount: Int,
    val completedGoalsCount: Int,
    val sessionsCount: Int
)

