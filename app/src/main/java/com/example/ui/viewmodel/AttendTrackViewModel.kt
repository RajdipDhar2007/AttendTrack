package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AttendTrackApp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassType
import com.example.data.model.Exam
import com.example.data.model.ExamType
import com.example.data.model.OverallAttendanceStat
import com.example.data.model.Profile
import com.example.data.model.SlotWithDetails
import com.example.data.model.StudyGoal
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.data.model.SubjectAttendanceStat
import com.example.data.model.SubjectStudyStat
import com.example.data.model.Teacher
import com.example.data.model.TimetableSlot
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

data class ActiveClassInfo(
    val slot: TimetableSlot,
    val subject: Subject,
    val teacher: Teacher?,
    val remainingSeconds: Long,
    val progress: Float // 0f to 1f
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class WhatIfResult(
    val projectedPresent: Int,
    val projectedAbsent: Int,
    val projectedTotal: Int,
    val projectedPercentage: Float,
    val targetPercentage: Float,
    val diffFromCurrent: Float,
    val isTargetMet: Boolean
)

class AttendTrackViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: AttendanceRepository = (application as AttendTrackApp).repository

    val profile: StateFlow<Profile?> = repo.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSubjects: StateFlow<List<Subject>> = repo.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTeachers: StateFlow<List<Teacher>> = repo.allTeachers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSlots: StateFlow<List<TimetableSlot>> = repo.allSlots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExams: StateFlow<List<Exam>> = repo.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingExams: StateFlow<List<Exam>> = repo.getUpcomingExams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overallStat: StateFlow<OverallAttendanceStat> = repo.getOverallStat()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallAttendanceStat())

    val subjectStats: StateFlow<List<SubjectAttendanceStat>> = repo.getSubjectStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjectStatsByType: StateFlow<List<SubjectAttendanceStat>> = repo.getSubjectStatsByType()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = repo.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStudyGoals: StateFlow<List<StudyGoal>> = repo.allStudyGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStudySessions: StateFlow<List<StudySession>> = repo.allStudySessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjectStudyStats: StateFlow<List<SubjectStudyStat>> = repo.getSubjectStudyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's schedule
    val todayDate: LocalDate = LocalDate.now()
    val todaySchedule: StateFlow<List<SlotWithDetails>> = repo.getTodayScheduleWithDetails(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly timetable selected day: 1 = Mon .. 7 = Sun
    private val _selectedTimetableDay = MutableStateFlow(todayDate.dayOfWeek.value)
    val selectedTimetableDay: StateFlow<Int> = _selectedTimetableDay.asStateFlow()

    fun setSelectedTimetableDay(day: Int) {
        _selectedTimetableDay.value = day
    }

    // Active ongoing class countdown
    private val _activeClass = MutableStateFlow<ActiveClassInfo?>(null)
    val activeClass: StateFlow<ActiveClassInfo?> = _activeClass.asStateFlow()

    // Next upcoming class today
    private val _nextClass = MutableStateFlow<SlotWithDetails?>(null)
    val nextClass: StateFlow<SlotWithDetails?> = _nextClass.asStateFlow()

    // Undo snackbar message flow
    private val _undoMessage = MutableSharedFlow<String>()
    val undoMessage = _undoMessage.asSharedFlow()
    private var lastRecordedAction: AttendanceRecord? = null
    private var lastBatchRecordIds: List<Long> = emptyList()

    // What-If Simulator state
    private val _simSelectedSubjectId = MutableStateFlow<Long?>(null) // null for overall
    val simSelectedSubjectId: StateFlow<Long?> = _simSelectedSubjectId.asStateFlow()

    private val _simUpcomingAttend = MutableStateFlow(5)
    val simUpcomingAttend: StateFlow<Int> = _simUpcomingAttend.asStateFlow()

    private val _simUpcomingBunk = MutableStateFlow(1)
    val simUpcomingBunk: StateFlow<Int> = _simUpcomingBunk.asStateFlow()

    fun setSimSubject(subjectId: Long?) { _simSelectedSubjectId.value = subjectId }
    fun setSimAttend(count: Int) { _simUpcomingAttend.value = count }
    fun setSimBunk(count: Int) { _simUpcomingBunk.value = count }

    val whatIfResult: StateFlow<WhatIfResult> = combine(
        overallStat,
        subjectStats,
        _simSelectedSubjectId,
        _simUpcomingAttend,
        _simUpcomingBunk
    ) { overall, subs, selSubId, addAttend, addBunk ->
        val currentPresent: Int
        val currentAbsent: Int
        val target: Float

        if (selSubId == null) {
            currentPresent = overall.totalPresent
            currentAbsent = overall.totalAbsent
            target = overall.targetPercentage
        } else {
            val matching = subs.filter { it.subject.id == selSubId }
            currentPresent = matching.sumOf { it.presentCount }
            currentAbsent = matching.sumOf { it.absentCount }
            target = matching.firstOrNull()?.targetPercentage ?: overall.targetPercentage
        }

        val projPresent = currentPresent + addAttend
        val projAbsent = currentAbsent + addBunk
        val projTotal = projPresent + projAbsent
        val currentEffective = currentPresent + currentAbsent
        val currentPct = if (currentEffective > 0) (currentPresent.toFloat() / currentEffective) * 100f else 100f
        val projPct = if (projTotal > 0) (projPresent.toFloat() / projTotal) * 100f else 100f

        WhatIfResult(
            projectedPresent = projPresent,
            projectedAbsent = projAbsent,
            projectedTotal = projTotal,
            projectedPercentage = projPct,
            targetPercentage = target,
            diffFromCurrent = projPct - currentPct,
            isTargetMet = projPct >= target
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WhatIfResult(0, 0, 0, 100f, 75f, 0f, true)
    )

    // AI Assistant Chat Messages
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello! I am your AttendTrack Academic Assistant. Ask me about your timetable, safe bunks, exam dates, or recovery plans!",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping: StateFlow<Boolean> = _isAiTyping.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.getProfile() == null) {
                repo.saveProfile(Profile(id = 1, hasCompletedOnboarding = false))
            }
            repo.cleanAndDeduplicateSubjects()
        }
        startClassCountdownTimer()
    }

    // Loop every second to update ongoing class and next class
    private fun startClassCountdownTimer() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = LocalTime.now()
                val schedule = todaySchedule.value

                var currentFound: ActiveClassInfo? = null
                var nextFound: SlotWithDetails? = null

                for (item in schedule) {
                    val start = item.slot.getStartLocalTime()
                    val end = item.slot.getEndLocalTime()

                    if (now.isAfter(start) && now.isBefore(end)) {
                        val totalSecs = Duration.between(start, end).seconds.coerceAtLeast(1)
                        val elapsedSecs = Duration.between(start, now).seconds
                        val remainingSecs = Duration.between(now, end).seconds
                        val progress = (elapsedSecs.toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)

                        currentFound = ActiveClassInfo(
                            slot = item.slot,
                            subject = item.subject,
                            teacher = item.teacher,
                            remainingSeconds = remainingSecs,
                            progress = progress
                        )
                    } else if (now.isBefore(start) && nextFound == null) {
                        nextFound = item
                    }
                }

                _activeClass.value = currentFound
                _nextClass.value = nextFound

                delay(1000)
            }
        }
    }

    // Mark attendance
    fun markAttendance(
        date: LocalDate,
        slot: TimetableSlot?,
        subjectId: Long,
        teacherId: Long?,
        classType: ClassType,
        status: AttendanceStatus,
        note: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val id = repo.markAttendance(
                date = dateStr,
                slotId = slot?.id,
                subjectId = subjectId,
                teacherId = teacherId,
                classType = classType,
                status = status,
                note = note
            )
            lastRecordedAction = AttendanceRecord(
                id = id,
                date = dateStr,
                slotId = slot?.id,
                subjectId = subjectId,
                teacherId = teacherId,
                classType = classType,
                status = status,
                note = note
            )
            lastBatchRecordIds = listOf(id)
            val statusName = when (status) {
                AttendanceStatus.PRESENT -> "Attended"
                AttendanceStatus.ABSENT -> "Missed"
                AttendanceStatus.CANCELLED -> "Cancelled"
            }
            _undoMessage.emit("Marked as $statusName")
        }
    }

    fun toggleOrMarkAttendance(
        date: LocalDate,
        slot: TimetableSlot?,
        subjectId: Long,
        teacherId: Long?,
        classType: ClassType,
        status: AttendanceStatus,
        currentRecord: AttendanceRecord?,
        note: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (currentRecord != null && currentRecord.status == status) {
                // Tapping already active status unmarks the class
                repo.deleteAttendanceRecord(currentRecord.id)
                lastRecordedAction = null
                _undoMessage.emit("Attendance unmarked")
            } else {
                val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val id = repo.markAttendance(
                    date = dateStr,
                    slotId = slot?.id,
                    subjectId = subjectId,
                    teacherId = teacherId,
                    classType = classType,
                    status = status,
                    note = note
                )
                lastRecordedAction = AttendanceRecord(
                    id = id,
                    date = dateStr,
                    slotId = slot?.id,
                    subjectId = subjectId,
                    teacherId = teacherId,
                    classType = classType,
                    status = status,
                    note = note
                )
                lastBatchRecordIds = listOf(id)
                val statusName = when (status) {
                    AttendanceStatus.PRESENT -> "Attended"
                    AttendanceStatus.ABSENT -> "Missed"
                    AttendanceStatus.CANCELLED -> "Cancelled"
                }
                _undoMessage.emit("Marked as $statusName")
            }
        }
    }

    fun deleteAttendanceRecord(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteAttendanceRecord(id)
            _undoMessage.emit("Class record removed")
        }
    }

    // Mark multiple classes for a subject via slider count
    fun markMultipleAttendance(
        date: LocalDate,
        subjectId: Long,
        teacherId: Long?,
        classType: ClassType,
        status: AttendanceStatus,
        count: Int,
        note: String = ""
    ) {
        if (count <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val createdIds = mutableListOf<Long>()
            for (i in 0 until count) {
                val id = repo.markAttendance(
                    date = dateStr,
                    slotId = null,
                    subjectId = subjectId,
                    teacherId = teacherId,
                    classType = classType,
                    status = status,
                    note = note
                )
                createdIds.add(id)
            }
            lastBatchRecordIds = createdIds
            lastRecordedAction = AttendanceRecord(
                id = createdIds.last(),
                date = dateStr,
                slotId = null,
                subjectId = subjectId,
                teacherId = teacherId,
                classType = classType,
                status = status,
                note = note
            )
            val label = if (status == AttendanceStatus.PRESENT) "Attended" else "Missed"
            _undoMessage.emit("Marked $count class(es) as $label")
        }
    }

    fun undoLastAttendanceMark() {
        val batchIds = lastBatchRecordIds
        val single = lastRecordedAction
        if (batchIds.isEmpty() && single == null) return

        viewModelScope.launch(Dispatchers.IO) {
            if (batchIds.isNotEmpty()) {
                batchIds.forEach { id ->
                    repo.deleteAttendanceRecord(id)
                }
            } else if (single != null) {
                repo.deleteAttendanceRecord(single.id)
            }
            lastBatchRecordIds = emptyList()
            lastRecordedAction = null
            _undoMessage.emit("Attendance marking undone")
        }
    }

    // Profile updates
    fun updateProfile(profile: Profile) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.saveProfile(profile)
        }
    }

    fun updateTargetPercentage(target: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val curr = repo.getProfile() ?: Profile()
            repo.saveProfile(curr.copy(targetPercentage = target))
        }
    }

    // Subject operations
    fun addSubject(name: String, code: String, colorHex: String, target: Float?) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertSubject(
                Subject(
                    name = name.trim(),
                    code = code.trim().uppercase(),
                    colorHex = colorHex,
                    customTargetPercentage = target
                )
            )
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateSubject(subject)
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteSubject(subject)
        }
    }

    // Teacher operations
    fun addTeacher(name: String, email: String, cabin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertTeacher(Teacher(name = name.trim(), email = email.trim(), cabin = cabin.trim()))
        }
    }

    fun updateTeacher(teacher: Teacher) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateTeacher(teacher)
        }
    }

    fun deleteTeacher(teacher: Teacher) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteTeacher(teacher)
        }
    }

    // Timetable slot operations
    fun addSlot(slot: TimetableSlot) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertSlot(slot)
        }
    }

    fun updateSlot(slot: TimetableSlot) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateSlot(slot)
        }
    }

    fun deleteSlot(slot: TimetableSlot) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteSlot(slot)
        }
    }

    // Exam operations
    fun addExam(exam: Exam) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertExam(exam)
        }
    }

    fun updateExam(exam: Exam) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateExam(exam)
        }
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteExam(exam)
        }
    }

    // Study Planner operations
    fun addStudyGoal(
        subjectId: Long,
        title: String,
        targetHours: Float,
        deadlineDate: String,
        priority: String,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertStudyGoal(
                StudyGoal(
                    subjectId = subjectId,
                    title = title.trim(),
                    targetHours = targetHours,
                    deadlineDate = deadlineDate.trim(),
                    priority = priority,
                    notes = notes.trim()
                )
            )
        }
    }

    fun updateStudyGoal(goal: StudyGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateStudyGoal(goal)
        }
    }

    fun toggleStudyGoalCompleted(goal: StudyGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateStudyGoal(goal.copy(isCompleted = !goal.isCompleted))
        }
    }

    fun deleteStudyGoal(goal: StudyGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteStudyGoal(goal)
        }
    }

    fun logStudySession(
        subjectId: Long,
        goalId: Long?,
        durationMinutes: Int,
        topicCovered: String,
        notes: String,
        date: String = LocalDate.now().toString()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.logStudySession(
                subjectId = subjectId,
                goalId = goalId,
                durationMinutes = durationMinutes,
                topicCovered = topicCovered.trim(),
                notes = notes.trim(),
                date = date
            )
        }
    }

    fun deleteStudySession(session: StudySession) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteStudySession(session)
        }
    }

    // Reset Operations
    fun resetSemester() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.resetSemesterAttendance()
        }
    }

    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.resetAllData()
        }
    }

    // Cloud & JSON Export / Import
    private val cloudBackupService = com.example.data.remote.CloudBackupService(application)

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        repo.exportToJson()
    }

    suspend fun importJson(content: String, cleanReplace: Boolean = true): com.example.data.model.ImportResult = withContext(Dispatchers.IO) {
        repo.importFromJson(content, cleanReplace)
    }

    suspend fun backupToCloud(): com.example.data.remote.CloudBackupResult = withContext(Dispatchers.IO) {
        val json = repo.exportToJson()
        cloudBackupService.uploadToCloud(json)
    }

    suspend fun restoreFromCloud(keyOrUrlOrJson: String, cleanReplace: Boolean = true): com.example.data.model.ImportResult = withContext(Dispatchers.IO) {
        val (fetchSuccess, payload) = cloudBackupService.fetchFromCloud(keyOrUrlOrJson)
        if (!fetchSuccess) {
            return@withContext com.example.data.model.ImportResult(success = false, message = payload)
        }
        repo.importFromJson(payload, cleanReplace)
    }

    fun getLatestCloudBackupInfo(): com.example.data.remote.CloudBackupInfo? {
        return cloudBackupService.getLatestCloudBackupInfo()
    }

    fun isCloudConnected(): Boolean {
        return cloudBackupService.isOnline()
    }

    fun getScheduleForDate(date: LocalDate): kotlinx.coroutines.flow.Flow<List<SlotWithDetails>> {
        return repo.getScheduleForDate(date)
    }

    private val studyService = com.example.data.remote.AiStudyService(application)

    // AI Assistant Copilot with Real-Time Database Access and Gemini Conversational Intelligence
    fun sendChatMessage(query: String) {
        if (query.isBlank()) return
        val userMsg = ChatMessage(text = query.trim(), isUser = true)
        val historyBeforeThis = _chatMessages.value
        _chatMessages.value = historyBeforeThis + userMsg

        viewModelScope.launch(Dispatchers.IO) {
            _isAiTyping.value = true
            try {
                // Fetch the live, up-to-the-second snapshot of the complete database
                val liveSnapshot = repo.getLiveDatabaseSnapshot()

                // Conversational history pairs (text to isUser)
                val historyPairs = historyBeforeThis.map { it.text to it.isUser }

                val response = studyService.answerQueryWithLiveDatabase(
                    query = query.trim(),
                    liveDatabaseSnapshot = liveSnapshot,
                    recentHistory = historyPairs
                )
                _chatMessages.value = _chatMessages.value + ChatMessage(text = response, isUser = false)
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    text = "I encountered an error while processing: ${e.localizedMessage ?: "Unknown network issue"}. Please verify your connection or Gemini API key.",
                    isUser = false
                )
            } finally {
                _isAiTyping.value = false
            }
        }
    }

    fun saveCustomGeminiApiKey(key: String) {
        studyService.saveCustomApiKey(key)
    }

    fun getEffectiveGeminiApiKey(): String {
        return studyService.getEffectiveApiKey()
    }
}

