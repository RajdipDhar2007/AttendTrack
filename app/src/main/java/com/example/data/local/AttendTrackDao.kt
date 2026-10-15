package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClassType
import com.example.data.model.Exam
import com.example.data.model.Holiday
import com.example.data.model.Profile
import com.example.data.model.StudyGoal
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.data.model.Teacher
import com.example.data.model.TimetableSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendTrackDao {

    // Profile
    @Query("SELECT * FROM profiles WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<Profile?>

    @Query("SELECT * FROM profiles WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: Profile)

    // Subjects
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects ORDER BY id ASC")
    suspend fun getAllSubjectsSync(): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun deleteSubjectById(subjectId: Long)

    @Query("UPDATE timetable_slots SET subjectId = :newSubjectId WHERE subjectId = :oldSubjectId")
    suspend fun remapSlotsSubject(oldSubjectId: Long, newSubjectId: Long)

    @Query("UPDATE attendance_records SET subjectId = :newSubjectId WHERE subjectId = :oldSubjectId")
    suspend fun remapAttendanceSubject(oldSubjectId: Long, newSubjectId: Long)

    @Query("UPDATE exams SET subjectId = :newSubjectId WHERE subjectId = :oldSubjectId")
    suspend fun remapExamsSubject(oldSubjectId: Long, newSubjectId: Long)

    @Query("UPDATE study_goals SET subjectId = :newSubjectId WHERE subjectId = :oldSubjectId")
    suspend fun remapStudyGoalsSubject(oldSubjectId: Long, newSubjectId: Long)

    @Query("UPDATE study_sessions SET subjectId = :newSubjectId WHERE subjectId = :oldSubjectId")
    suspend fun remapStudySessionsSubject(oldSubjectId: Long, newSubjectId: Long)

    // Teachers
    @Query("SELECT * FROM teachers ORDER BY name ASC")
    fun getAllTeachers(): Flow<List<Teacher>>

    @Query("SELECT * FROM teachers ORDER BY id ASC")
    suspend fun getAllTeachersSync(): List<Teacher>

    @Query("SELECT * FROM teachers WHERE id = :id LIMIT 1")
    suspend fun getTeacherById(id: Long): Teacher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<Teacher>)

    @Update
    suspend fun updateTeacher(teacher: Teacher)

    @Delete
    suspend fun deleteTeacher(teacher: Teacher)

    // Timetable Slots
    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllSlots(): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots ORDER BY id ASC")
    suspend fun getAllSlotsSync(): List<TimetableSlot>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getSlotsForDaySync(dayOfWeek: Int): List<TimetableSlot>

    @Query("SELECT * FROM timetable_slots WHERE id = :slotId LIMIT 1")
    suspend fun getSlotById(slotId: Long): TimetableSlot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<TimetableSlot>)

    @Update
    suspend fun updateSlot(slot: TimetableSlot)

    @Delete
    suspend fun deleteSlot(slot: TimetableSlot)

    // Attendance Records
    @Query("SELECT * FROM attendance_records ORDER BY date DESC, id DESC")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY id ASC")
    suspend fun getAllAttendanceRecordsSync(): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    suspend fun getAttendanceForDateSync(date: String): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE date = :date AND slotId = :slotId LIMIT 1")
    suspend fun getAttendanceForSlotAndDate(slotId: Long, date: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE id = :id LIMIT 1")
    suspend fun getAttendanceById(id: Long): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE date = :date AND subjectId = :subjectId LIMIT 1")
    suspend fun getAttendanceForSubjectAndDate(subjectId: Long, date: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE date = :date AND subjectId = :subjectId AND classType = :classType LIMIT 1")
    suspend fun getAttendanceForSubjectDateClassType(subjectId: Long, date: String, classType: ClassType): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId")
    fun getAttendanceForSubject(subjectId: Long): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>)

    @Delete
    suspend fun deleteAttendance(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteAttendanceById(id: Long)

    // Exams
    @Query("SELECT * FROM exams ORDER BY date ASC, time ASC")
    fun getAllExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams ORDER BY id ASC")
    suspend fun getAllExamsSync(): List<Exam>

    @Query("SELECT * FROM exams WHERE date >= :today ORDER BY date ASC, time ASC")
    fun getUpcomingExams(today: String): Flow<List<Exam>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<Exam>)

    @Update
    suspend fun updateExam(exam: Exam)

    @Delete
    suspend fun deleteExam(exam: Exam)

    // Holidays
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidays(): Flow<List<Holiday>>

    @Query("SELECT * FROM holidays ORDER BY id ASC")
    suspend fun getAllHolidaysSync(): List<Holiday>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: Holiday): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidays(holidays: List<Holiday>)

    @Delete
    suspend fun deleteHoliday(holiday: Holiday)

    // Reset Operations
    @Query("DELETE FROM attendance_records")
    suspend fun clearAllAttendance()

    @Query("DELETE FROM exams")
    suspend fun clearAllExams()

    @Query("DELETE FROM timetable_slots")
    suspend fun clearTimetable()

    @Query("DELETE FROM subjects")
    suspend fun clearSubjects()

    @Query("DELETE FROM teachers")
    suspend fun clearTeachers()

    @Query("DELETE FROM holidays")
    suspend fun clearHolidays()

    // Study Goals
    @Query("SELECT * FROM study_goals ORDER BY isCompleted ASC, priority DESC, id DESC")
    fun getAllStudyGoals(): Flow<List<StudyGoal>>

    @Query("SELECT * FROM study_goals ORDER BY id ASC")
    suspend fun getAllStudyGoalsSync(): List<StudyGoal>

    @Query("SELECT * FROM study_goals WHERE subjectId = :subjectId ORDER BY isCompleted ASC, priority DESC")
    fun getStudyGoalsForSubject(subjectId: Long): Flow<List<StudyGoal>>

    @Query("SELECT * FROM study_goals WHERE id = :id LIMIT 1")
    suspend fun getStudyGoalById(id: Long): StudyGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyGoal(goal: StudyGoal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyGoals(goals: List<StudyGoal>)

    @Update
    suspend fun updateStudyGoal(goal: StudyGoal)

    @Delete
    suspend fun deleteStudyGoal(goal: StudyGoal)

    @Query("DELETE FROM study_goals WHERE id = :id")
    suspend fun deleteStudyGoalById(id: Long)

    // Study Sessions
    @Query("SELECT * FROM study_sessions ORDER BY date DESC, timestamp DESC")
    fun getAllStudySessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions ORDER BY id ASC")
    suspend fun getAllStudySessionsSync(): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE subjectId = :subjectId ORDER BY date DESC, timestamp DESC")
    fun getStudySessionsForSubject(subjectId: Long): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySessions(sessions: List<StudySession>)

    @Delete
    suspend fun deleteStudySession(session: StudySession)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteStudySessionById(id: Long)

    @Query("DELETE FROM study_goals")
    suspend fun clearAllStudyGoals()

    @Query("DELETE FROM study_sessions")
    suspend fun clearAllStudySessions()
}
