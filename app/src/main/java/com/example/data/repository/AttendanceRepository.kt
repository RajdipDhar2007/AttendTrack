package com.example.data.repository

import com.example.data.local.AttendTrackDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassType
import com.example.data.model.Exam
import com.example.data.model.ExamType
import com.example.data.model.Holiday
import com.example.data.model.ImportResult
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class AttendanceRepository(private val dao: AttendTrackDao, private val context: android.content.Context? = null) {

    private val seedMutex = Mutex()

    fun saveScheduleNotes(notes: String) {
        context?.getSharedPreferences("attend_notes_prefs", android.content.Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("schedule_notes", notes)
            ?.apply()
    }

    fun getScheduleNotes(): String {
        return context?.getSharedPreferences("attend_notes_prefs", android.content.Context.MODE_PRIVATE)
            ?.getString("schedule_notes", "") ?: ""
    }

    val profileFlow: Flow<Profile?> = dao.getProfileFlow()
    val allSubjects: Flow<List<Subject>> = dao.getAllSubjects().map { list ->
        deduplicateSubjectsInMemory(list)
    }
    val allTeachers: Flow<List<Teacher>> = dao.getAllTeachers()
    val allSlots: Flow<List<TimetableSlot>> = dao.getAllSlots()
    val allAttendance: Flow<List<AttendanceRecord>> = dao.getAllAttendanceRecords()
    val allExams: Flow<List<Exam>> = dao.getAllExams()
    val allHolidays: Flow<List<Holiday>> = dao.getAllHolidays()
    val allStudyGoals: Flow<List<StudyGoal>> = dao.getAllStudyGoals()
    val allStudySessions: Flow<List<StudySession>> = dao.getAllStudySessions()

    fun getUpcomingExams(): Flow<List<Exam>> {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return dao.getUpcomingExams(todayStr)
    }

    fun getStudyGoalsForSubject(subjectId: Long): Flow<List<StudyGoal>> = dao.getStudyGoalsForSubject(subjectId)
    fun getStudySessionsForSubject(subjectId: Long): Flow<List<StudySession>> = dao.getStudySessionsForSubject(subjectId)

    suspend fun insertStudyGoal(goal: StudyGoal): Long = dao.insertStudyGoal(goal)
    suspend fun updateStudyGoal(goal: StudyGoal) = dao.updateStudyGoal(goal)
    suspend fun deleteStudyGoal(goal: StudyGoal) = dao.deleteStudyGoal(goal)

    suspend fun logStudySession(
        subjectId: Long,
        goalId: Long?,
        durationMinutes: Int,
        topicCovered: String,
        notes: String,
        date: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    ): Long {
        val session = StudySession(
            subjectId = subjectId,
            goalId = goalId,
            date = date,
            durationMinutes = durationMinutes,
            topicCovered = topicCovered,
            notes = notes
        )
        val id = dao.insertStudySession(session)
        if (goalId != null) {
            val goal = dao.getStudyGoalById(goalId)
            if (goal != null) {
                val addedHours = durationMinutes.toFloat() / 60f
                val newStudied = goal.studiedHours + addedHours
                val isCompleted = newStudied >= goal.targetHours
                dao.updateStudyGoal(goal.copy(studiedHours = newStudied, isCompleted = isCompleted))
            }
        }
        return id
    }

    suspend fun deleteStudySession(session: StudySession) {
        if (session.goalId != null) {
            val goal = dao.getStudyGoalById(session.goalId)
            if (goal != null) {
                val subtracted = session.durationMinutes.toFloat() / 60f
                val newStudied = max(0f, goal.studiedHours - subtracted)
                val isCompleted = newStudied >= goal.targetHours
                dao.updateStudyGoal(goal.copy(studiedHours = newStudied, isCompleted = isCompleted))
            }
        }
        dao.deleteStudySession(session)
    }

    fun getSubjectStudyStats(): Flow<List<SubjectStudyStat>> {
        return combine(allSubjects, allStudyGoals, allStudySessions) { subjects, goals, sessions ->
            subjects.map { sub ->
                val subGoals = goals.filter { it.subjectId == sub.id }
                val subSessions = sessions.filter { it.subjectId == sub.id }
                val targetHours = subGoals.sumOf { it.targetHours.toDouble() }.toFloat()
                val sessionHours = subSessions.sumOf { (it.durationMinutes.toDouble() / 60.0) }.toFloat()
                val goalStudiedHours = subGoals.sumOf { it.studiedHours.toDouble() }.toFloat()
                val totalStudied = max(sessionHours, goalStudiedHours)
                val progress = if (targetHours > 0f) ((totalStudied / targetHours) * 100f).coerceIn(0f, 100f) else if (totalStudied > 0) 100f else 0f

                SubjectStudyStat(
                    subject = sub,
                    totalTargetHours = targetHours,
                    totalStudiedHours = totalStudied,
                    progressPercentage = progress,
                    goalsCount = subGoals.size,
                    completedGoalsCount = subGoals.count { it.isCompleted },
                    sessionsCount = subSessions.size
                )
            }
        }
    }

    suspend fun getProfile(): Profile? = dao.getProfile()
    suspend fun saveProfile(profile: Profile) = dao.saveProfile(profile)

    companion object {
        fun normalizeSubjectName(rawName: String): String {
            return rawName.trim()
                .replace(Regex("(?i)\\s+(Laboratory|Lab|Theory|Lecture|Tutorial|Practical)$"), "")
                .replace(Regex("(?i)\\[(Laboratory|Lab|Theory|Lecture|Tutorial|Practical)\\]$"), "")
                .replace(Regex("(?i)\\((Laboratory|Lab|Theory|Lecture|Tutorial|Practical)\\)$"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun getCanonicalSubjectKey(s: Subject): String {
            val normName = normalizeSubjectName(s.name)
                .lowercase()
                .replace(Regex("[^a-z0-9]"), "")
            if (normName.isNotBlank() && normName != "subject") {
                return normName
            }
            val codeClean = s.code.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
            val baseCode = if (codeClean.length >= 6 && codeClean.takeLast(2) == "51") {
                codeClean.dropLast(2) + "01"
            } else {
                codeClean
            }
            if (baseCode.isNotBlank()) {
                return baseCode
            }
            return s.id.toString()
        }

        fun getSubjectCodeKey(s: Subject): String? {
            val codeClean = s.code.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
            if (codeClean.length < 4) return null
            return if (codeClean.length >= 6 && codeClean.takeLast(2) == "51") codeClean.dropLast(2) + "01" else codeClean
        }

        /** Groups subjects so each real subject appears once (same name key OR same code key). */
        fun groupSubjects(list: List<Subject>): List<List<Subject>> {
            val groups = mutableListOf<MutableList<Subject>>()
            for (sub in list) {
                val nk = getCanonicalSubjectKey(sub)
                val ck = getSubjectCodeKey(sub)
                val hit = groups.firstOrNull { g ->
                    g.any { o -> getCanonicalSubjectKey(o) == nk || (ck != null && getSubjectCodeKey(o) == ck) }
                }
                if (hit != null) hit.add(sub) else groups.add(mutableListOf(sub))
            }
            return groups
        }

        fun deduplicateSubjectsInMemory(list: List<Subject>): List<Subject> {
            val result = mutableListOf<Subject>()
            for (group in groupSubjects(list)) {
                val canonical = group.minByOrNull { sub ->
                    var score = 0L
                    if (sub.code.isBlank()) score += 10000L
                    if (sub.name.contains("Lab", ignoreCase = true) || sub.name.contains("Laboratory", ignoreCase = true)) score += 5000L
                    if (sub.code.endsWith("51") || sub.code.endsWith("52")) score += 2000L
                    score + sub.id
                } ?: group.first()
                result.add(canonical)
            }
            return result
        }

        // Math Formulas for Bunks and Targets
        fun calculateSafeBunks(present: Int, absent: Int, target: Float): Int {
            val effective = present + absent
            if (effective == 0) return 0
            val t = (target / 100.0).coerceIn(0.01, 0.99)
            // Current % >= target %
            // present / (effective + X) >= t
            // present >= t * effective + t * X
            // t * X <= present - t * effective
            // X <= (present - t * effective) / t
            val diff = present - (t * effective)
            return if (diff >= 0) {
                floor(diff / t).toInt().coerceAtLeast(0)
            } else {
                0
            }
        }

        fun calculateClassesNeeded(present: Int, absent: Int, target: Float): Int {
            val effective = present + absent
            if (effective == 0) return 0
            val t = (target / 100.0).coerceIn(0.01, 0.99)
            // (present + Y) / (effective + Y) >= t
            // present + Y >= t * effective + t * Y
            // Y * (1 - t) >= t * effective - present
            // Y >= (t * effective - present) / (1 - t)
            val needed = (t * effective) - present
            return if (needed > 0) {
                ceil(needed / (1.0 - t)).toInt().coerceAtLeast(0)
            } else {
                0
            }
        }
    }

    suspend fun cleanAndDeduplicateSubjects() {
        try {
            val allSubs = dao.getAllSubjectsSync()
            if (allSubs.isEmpty()) return
            groupSubjects(allSubs).forEach { group ->
                if (group.size > 1) {
                    val canonical = group.minByOrNull { sub ->
                        var score = 0L
                        if (sub.code.isBlank()) score += 10000L
                        if (sub.name.contains("Lab", ignoreCase = true) || sub.name.contains("Laboratory", ignoreCase = true)) score += 5000L
                        if (sub.code.endsWith("51") || sub.code.endsWith("52")) score += 2000L
                        score + sub.id
                    } ?: group.first()
                    group.filter { it.id != canonical.id }.forEach { duplicate ->
                        dao.remapSlotsSubject(duplicate.id, canonical.id)
                        dao.remapAttendanceSubject(duplicate.id, canonical.id)
                        dao.remapExamsSubject(duplicate.id, canonical.id)
                        dao.remapStudyGoalsSubject(duplicate.id, canonical.id)
                        dao.remapStudySessionsSubject(duplicate.id, canonical.id)
                        dao.deleteSubjectById(duplicate.id)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun insertSubject(subject: Subject): Long {
        cleanAndDeduplicateSubjects()
        val allSubs = dao.getAllSubjectsSync()
        val key = getCanonicalSubjectKey(subject)
        val matched = allSubs.firstOrNull { getCanonicalSubjectKey(it) == key }
        return if (matched != null) {
            val updated = matched.copy(
                name = if (subject.name.isNotBlank()) subject.name else matched.name,
                code = if (subject.code.isNotBlank()) subject.code else matched.code,
                colorHex = if (subject.colorHex.isNotBlank()) subject.colorHex else matched.colorHex,
                customTargetPercentage = subject.customTargetPercentage ?: matched.customTargetPercentage
            )
            dao.updateSubject(updated)
            matched.id
        } else {
            dao.insertSubject(subject)
        }
    }
    suspend fun updateSubject(subject: Subject) = dao.updateSubject(subject)
    suspend fun deleteSubject(subject: Subject) = dao.deleteSubject(subject)

    suspend fun insertTeacher(teacher: Teacher): Long = dao.insertTeacher(teacher)
    suspend fun updateTeacher(teacher: Teacher) = dao.updateTeacher(teacher)
    suspend fun deleteTeacher(teacher: Teacher) = dao.deleteTeacher(teacher)

    suspend fun insertSlot(slot: TimetableSlot): Long = dao.insertSlot(slot)
    suspend fun updateSlot(slot: TimetableSlot) = dao.updateSlot(slot)
    suspend fun deleteSlot(slot: TimetableSlot) = dao.deleteSlot(slot)

    suspend fun insertExam(exam: Exam): Long = dao.insertExam(exam)
    suspend fun updateExam(exam: Exam) = dao.updateExam(exam)
    suspend fun deleteExam(exam: Exam) = dao.deleteExam(exam)

    suspend fun insertHoliday(holiday: Holiday): Long = dao.insertHoliday(holiday)
    suspend fun deleteHoliday(holiday: Holiday) = dao.deleteHoliday(holiday)

    // Mark attendance
    suspend fun markAttendance(
        date: String,
        slotId: Long?,
        subjectId: Long,
        teacherId: Long?,
        classType: ClassType,
        status: AttendanceStatus,
        note: String = ""
    ): Long {
        // 1. If slotId is negative, it refers to an existing ad-hoc / backfilled record (-slotId)
        if (slotId != null && slotId < 0) {
            val existing = dao.getAttendanceById(-slotId)
            if (existing != null) {
                val updated = existing.copy(status = status, note = note)
                dao.insertOrUpdateAttendance(updated)
                return updated.id
            }
        } else if (slotId != null && slotId > 0) {
            val existing = dao.getAttendanceForSlotAndDate(slotId, date)
            if (existing != null) {
                val updated = existing.copy(status = status, note = note)
                dao.insertOrUpdateAttendance(updated)
                return updated.id
            } else {
                val record = AttendanceRecord(
                    date = date,
                    slotId = slotId,
                    subjectId = subjectId,
                    teacherId = teacherId,
                    classType = classType,
                    status = status,
                    note = note
                )
                return dao.insertOrUpdateAttendance(record)
            }
        }

        // 2. Ad-hoc / backfill attendance (slotId is null)
        val existingAdHoc = dao.getAttendanceForDateSync(date).firstOrNull {
            (it.slotId == null || it.slotId <= 0) &&
            it.subjectId == subjectId &&
            it.classType == classType
        }
        if (existingAdHoc != null) {
            val updated = existingAdHoc.copy(status = status, note = note)
            dao.insertOrUpdateAttendance(updated)
            return updated.id
        }

        val record = AttendanceRecord(
            date = date,
            slotId = null,
            subjectId = subjectId,
            teacherId = teacherId,
            classType = classType,
            status = status,
            note = note
        )
        return dao.insertOrUpdateAttendance(record)
    }

    suspend fun getAttendanceRecordById(id: Long): AttendanceRecord? = dao.getAttendanceById(id)

    suspend fun deleteAttendanceRecord(id: Long) = dao.deleteAttendanceById(id)

    // Combined Flow for Today's Schedule with details & current marking (including ad-hoc/backfilled records)
    fun getTodayScheduleWithDetails(date: LocalDate): Flow<List<SlotWithDetails>> {
        val dayOfWeek = date.dayOfWeek.value
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        return combine(
            dao.getSlotsForDay(dayOfWeek),
            dao.getAllSubjects(),
            dao.getAllTeachers(),
            dao.getAttendanceForDate(dateStr)
        ) { slots, subjects, teachers, records ->
            val subMap = subjects.associateBy { it.id }
            val teachMap = teachers.associateBy { it.id }

            val recordsBySlotId = records.filter { it.slotId != null && it.slotId > 0 }.associateBy { it.slotId!! }
            val usedRecordIds = mutableSetOf<Long>()

            val scheduledItems = slots.mapNotNull { slot ->
                val sub = subMap[slot.subjectId] ?: return@mapNotNull null
                val teach = slot.teacherId?.let { teachMap[it] }
                val ta = slot.technicalAssistantId?.let { teachMap[it] }

                val rec = recordsBySlotId[slot.id]
                if (rec != null) {
                    usedRecordIds.add(rec.id)
                }

                SlotWithDetails(
                    slot = slot,
                    subject = sub,
                    teacher = teach,
                    technicalAssistant = ta,
                    todayRecord = rec
                )
            }

            // Ad-hoc / backfilled attendance records on this date not matching any scheduled slot
            val adHocRecords = records.filter { it.id !in usedRecordIds && (it.slotId == null || it.slotId <= 0) }

            val adHocItems = adHocRecords.mapNotNull { rec ->
                val sub = subMap[rec.subjectId] ?: return@mapNotNull null
                val teach = rec.teacherId?.let { teachMap[it] }
                val virtualSlot = TimetableSlot(
                    id = -rec.id, // negative id to denote ad-hoc / backfilled record
                    dayOfWeek = dayOfWeek,
                    subjectId = rec.subjectId,
                    teacherId = rec.teacherId,
                    startTime = "09:00",
                    endTime = "10:00",
                    room = "Backfilled / Ad-hoc",
                    classType = rec.classType
                )
                SlotWithDetails(
                    slot = virtualSlot,
                    subject = sub,
                    teacher = teach,
                    technicalAssistant = null,
                    todayRecord = rec
                )
            }

            scheduledItems + adHocItems
        }
    }

    fun getScheduleForDate(date: LocalDate): Flow<List<SlotWithDetails>> = getTodayScheduleWithDetails(date)

    // Subject-wise attendance calculation: exactly one aggregated entry per subject to avoid duplicate subject listings
    fun getSubjectStats(): Flow<List<SubjectAttendanceStat>> = buildSubjectStats(splitByClassType = false)

    // Same as getSubjectStats, but a subject that has both lectures and labs yields one entry per class type
    fun getSubjectStatsByType(): Flow<List<SubjectAttendanceStat>> = buildSubjectStats(splitByClassType = true)

    private fun buildSubjectStats(splitByClassType: Boolean): Flow<List<SubjectAttendanceStat>> {
        return combine(
            dao.getAllSubjects(),
            dao.getAllTeachers(),
            dao.getAllAttendanceRecords(),
            dao.getProfileFlow(),
            dao.getAllSlots()
        ) { rawSubjects, teachers, records, profile, slots ->
            val teachMap = teachers.associateBy { it.id }
            val defaultTarget = profile?.targetPercentage ?: 75f

            // Group subjects by canonical key to guarantee exactly ONE entry per subject
            groupSubjects(rawSubjects).flatMap { subGroup ->
                val groupIds = subGroup.map { it.id }.toSet()
                val typesInGroup = if (splitByClassType) {
                    (slots.filter { it.subjectId in groupIds }.map { it.classType } +
                        records.filter { it.subjectId in groupIds }.map { it.classType }).toSet()
                } else emptySet()
                val splitTypes: List<ClassType?> =
                    if (typesInGroup.size > 1) typesInGroup.toList() else listOf(null)

                splitTypes.map { onlyType ->
                val canonicalSubject = subGroup.minByOrNull { sub ->
                    var score = 0L
                    if (sub.code.isBlank()) score += 10000L
                    if (sub.name.contains("Lab", ignoreCase = true) || sub.name.contains("Laboratory", ignoreCase = true)) score += 5000L
                    if (sub.code.endsWith("51") || sub.code.endsWith("52")) score += 2000L
                    score + sub.id
                } ?: subGroup.first()

                val subjectIds = subGroup.map { it.id }.toSet()

                val recs = records.filter { it.subjectId in subjectIds && (onlyType == null || it.classType == onlyType) }
                val subSlots = slots.filter { it.subjectId in subjectIds && (onlyType == null || it.classType == onlyType) }

                val present = recs.count { it.status == AttendanceStatus.PRESENT }
                val absent = recs.count { it.status == AttendanceStatus.ABSENT }
                val cancelled = recs.count { it.status == AttendanceStatus.CANCELLED }
                val effective = present + absent
                val total = recs.size

                val percentage = if (effective > 0) {
                    (present.toFloat() / effective.toFloat()) * 100f
                } else {
                    100f // 100% initial when 0 classes taken
                }

                val target = canonicalSubject.customTargetPercentage
                    ?: subGroup.firstNotNullOfOrNull { it.customTargetPercentage }
                    ?: defaultTarget
                val safeBunks = calculateSafeBunks(present, absent, target)
                val needed = calculateClassesNeeded(present, absent, target)

                val isLab = canonicalSubject.name.contains("Lab", ignoreCase = true) ||
                        (subSlots.isNotEmpty() && subSlots.all { it.classType == ClassType.LAB }) ||
                        (recs.isNotEmpty() && recs.all { it.classType == ClassType.LAB })
                val classType = onlyType ?: if (isLab) ClassType.LAB else ClassType.THEORY

                val teacherId = subSlots.firstOrNull { it.teacherId != null }?.teacherId
                    ?: recs.firstOrNull { it.teacherId != null }?.teacherId
                val teacher = teacherId?.let { teachMap[it] }

                SubjectAttendanceStat(
                    subject = canonicalSubject,
                    teacher = teacher,
                    classType = classType,
                    presentCount = present,
                    absentCount = absent,
                    cancelledCount = cancelled,
                    totalCount = total,
                    effectiveCount = effective,
                    percentage = percentage,
                    targetPercentage = target,
                    safeBunks = safeBunks,
                    neededToTarget = needed
                )
                }
            }.sortedWith(
                compareBy({ it.subject.code.ifBlank { "ZZZ" } }, { it.subject.name }, { it.classType.ordinal })
            )
        }
    }

    // Overall Attendance Stat Flow
    fun getOverallStat(): Flow<OverallAttendanceStat> {
        return combine(
            dao.getAllAttendanceRecords(),
            dao.getProfileFlow()
        ) { records, profile ->
            val defaultTarget = profile?.targetPercentage ?: 75f
            val present = records.count { it.status == AttendanceStatus.PRESENT }
            val absent = records.count { it.status == AttendanceStatus.ABSENT }
            val cancelled = records.count { it.status == AttendanceStatus.CANCELLED }
            val effective = present + absent

            val percentage = if (effective > 0) {
                (present.toFloat() / effective.toFloat()) * 100f
            } else {
                100f
            }

            val safeBunks = calculateSafeBunks(present, absent, defaultTarget)
            val needed = calculateClassesNeeded(present, absent, defaultTarget)

            // Calculate streak: consecutive unique days with at least one PRESENT and 0 ABSENT
            val (currentStreak, bestStreak) = calculateStreaks(records)

            OverallAttendanceStat(
                totalPresent = present,
                totalAbsent = absent,
                totalCancelled = cancelled,
                totalEffective = effective,
                percentage = percentage,
                targetPercentage = defaultTarget,
                safeBunks = safeBunks,
                classesNeeded = needed,
                currentStreak = currentStreak,
                bestStreak = bestStreak
            )
        }
    }

    // Streak calculation
    private fun calculateStreaks(records: List<AttendanceRecord>): Pair<Int, Int> {
        if (records.isEmpty()) return Pair(0, 0)

        val recordsByDate = records.groupBy { it.date }.toSortedMap()
        var currentStreak = 0
        var bestStreak = 0
        var tempStreak = 0

        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        recordsByDate.forEach { (_, recList) ->
            val hasPresent = recList.any { it.status == AttendanceStatus.PRESENT }
            val hasAbsent = recList.any { it.status == AttendanceStatus.ABSENT }

            if (hasPresent && !hasAbsent) {
                tempStreak++
                if (tempStreak > bestStreak) bestStreak = tempStreak
            } else {
                tempStreak = 0
            }
        }

        // Check backwards from today or yesterday for active streak
        var checkDate = today
        // If nothing today, start from yesterday
        val todayStr = today.format(formatter)
        if (!recordsByDate.containsKey(todayStr)) {
            checkDate = today.minusDays(1)
        }

        while (true) {
            val dateStr = checkDate.format(formatter)
            val recList = recordsByDate[dateStr] ?: break
            val hasPresent = recList.any { it.status == AttendanceStatus.PRESENT }
            val hasAbsent = recList.any { it.status == AttendanceStatus.ABSENT }

            if (hasPresent && !hasAbsent) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }

        return Pair(currentStreak, max(bestStreak, currentStreak))
    }


    // Comprehensive Data & Cloud Backup / Restore via JSON
    suspend fun exportToJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("appName", "AttendTrack")
        root.put("projectCredit", "A Rajdip Dhar project")
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Preferences
        val prefsObj = JSONObject().apply {
            put("palette", com.example.ui.theme.AttendThemeState.palette.name)
            put("themeMode", com.example.ui.theme.AttendThemeState.mode.name)
            put("animationsEnabled", com.example.ui.theme.AttendThemeState.animationsEnabled)
        }
        root.put("preferences", prefsObj)

        // 2. Profile
        val profile = dao.getProfile()
        profile?.let { p ->
            val pObj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("institution", p.institution)
                put("department", p.department)
                put("section", p.section)
                put("group", p.group)
                put("sessionYear", p.sessionYear)
                put("semester", p.semester)
                put("year", p.year)
                put("targetPercentage", p.targetPercentage.toDouble())
                put("isPinEnabled", p.isPinEnabled)
                put("pinCode", p.pinCode)
                put("hasCompletedOnboarding", p.hasCompletedOnboarding)
            }
            root.put("profile", pObj)
        }

        // 3. Subjects (including customTargetPercentage and code)
        val subjects = dao.getAllSubjectsSync()
        val subjectsArray = JSONArray()
        subjects.forEach { s ->
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("code", s.code)
                put("colorHex", s.colorHex)
                if (s.customTargetPercentage != null) {
                    put("customTargetPercentage", s.customTargetPercentage.toDouble())
                }
            }
            subjectsArray.put(sObj)
        }
        root.put("subjects", subjectsArray)

        // 4. Teachers & Technical Assistants
        val teachers = dao.getAllTeachersSync()
        val teachersArray = JSONArray()
        teachers.forEach { t ->
            val tObj = JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("email", t.email)
                put("phone", t.phone)
                put("cabin", t.cabin)
                put("role", t.role)
                put("specialization", t.specialization)
            }
            teachersArray.put(tObj)
        }
        root.put("teachers", teachersArray)

        // 5. Timetable Slots (with room, teachers, TAs)
        val slots = dao.getAllSlotsSync()
        val slotsArray = JSONArray()
        slots.forEach { slot ->
            val slotObj = JSONObject().apply {
                put("id", slot.id)
                put("dayOfWeek", slot.dayOfWeek)
                put("subjectId", slot.subjectId)
                if (slot.teacherId != null) put("teacherId", slot.teacherId)
                if (slot.technicalAssistantId != null) put("technicalAssistantId", slot.technicalAssistantId)
                put("technicalAssistantName", slot.technicalAssistantName)
                put("classType", slot.classType.name)
                put("room", slot.room)
                put("startTime", slot.startTime)
                put("endTime", slot.endTime)
            }
            slotsArray.put(slotObj)
        }
        root.put("timetableSlots", slotsArray)

        // 6. Attendance Records (including backfilled past & future classes)
        val attendance = dao.getAllAttendanceRecordsSync()
        val attendanceArray = JSONArray()
        attendance.forEach { a ->
            val aObj = JSONObject().apply {
                put("id", a.id)
                put("date", a.date)
                if (a.slotId != null) put("slotId", a.slotId)
                put("subjectId", a.subjectId)
                if (a.teacherId != null) put("teacherId", a.teacherId)
                put("classType", a.classType.name)
                put("status", a.status.name)
                put("note", a.note)
            }
            attendanceArray.put(aObj)
        }
        root.put("attendanceRecords", attendanceArray)

        // 7. Exams
        val exams = dao.getAllExamsSync()
        val examsArray = JSONArray()
        exams.forEach { e ->
            val eObj = JSONObject().apply {
                put("id", e.id)
                put("subjectId", e.subjectId)
                put("title", e.title)
                put("date", e.date)
                put("time", e.time)
                put("room", e.room)
                put("examType", e.examType.name)
            }
            examsArray.put(eObj)
        }
        root.put("exams", examsArray)

        // 8. Holidays
        val holidays = dao.getAllHolidaysSync()
        val holidaysArray = JSONArray()
        holidays.forEach { h ->
            val hObj = JSONObject().apply {
                put("id", h.id)
                put("date", h.date)
                put("title", h.title)
            }
            holidaysArray.put(hObj)
        }
        root.put("holidays", holidaysArray)

        // 9. Study Goals
        val goals = dao.getAllStudyGoalsSync()
        val goalsArray = JSONArray()
        goals.forEach { g ->
            val gObj = JSONObject().apply {
                put("id", g.id)
                put("subjectId", g.subjectId)
                put("title", g.title)
                put("targetHours", g.targetHours)
                put("studiedHours", g.studiedHours)
                put("deadlineDate", g.deadlineDate)
                put("isCompleted", g.isCompleted)
                put("priority", g.priority)
                put("notes", g.notes)
            }
            goalsArray.put(gObj)
        }
        root.put("studyGoals", goalsArray)

        // 10. Study Sessions
        val sessions = dao.getAllStudySessionsSync()
        val sessionsArray = JSONArray()
        sessions.forEach { s ->
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("subjectId", s.subjectId)
                if (s.goalId != null) put("goalId", s.goalId)
                put("date", s.date)
                put("durationMinutes", s.durationMinutes)
                put("topicCovered", s.topicCovered)
                put("notes", s.notes)
            }
            sessionsArray.put(sObj)
        }
        root.put("studySessions", sessionsArray)

        return root.toString(2)
    }

    suspend fun importFromJson(jsonString: String, cleanReplace: Boolean = true): ImportResult {
        return try {
            val root = JSONObject(jsonString)

            // Check if this is student schedule JSON format (has "student" or "schedule")
            if (root.has("student") || root.has("schedule")) {
                return importStudentScheduleJson(root, cleanReplace)
            }

            // Step 1: Validate 4-Year Curriculum Rules if Profile exists
            if (root.has("profile")) {
                val p = root.getJSONObject("profile")
                val year = p.optString("year", "")
                val semester = p.optString("semester", "")

                if (year.isNotBlank() && semester.isNotBlank()) {
                    val validSems = com.example.data.model.AcademicCurriculum.getValidSemestersForYear(year)
                    if (!validSems.contains(semester)) {
                        return ImportResult(
                            success = false,
                            message = "Curriculum validation rejected: '$semester' is not allowed for '$year' in a 4-year program. Valid semesters for $year are: ${validSems.joinToString(", ")}."
                        )
                    }
                }
            }

            // Step 2: Clean existing data if cleanReplace is true
            if (cleanReplace) {
                dao.clearAllAttendance()
                dao.clearAllExams()
                dao.clearTimetable()
                dao.clearSubjects()
                dao.clearTeachers()
                dao.clearHolidays()
            }

            // Step 3: Restore Preferences
            if (root.has("preferences")) {
                try {
                    val prefs = root.getJSONObject("preferences")
                    val palName = prefs.optString("palette", "")
                    if (palName.isNotBlank()) {
                        com.example.data.model.AppPalette.values().firstOrNull { it.name == palName }?.let {
                            com.example.ui.theme.AttendThemeState.palette = it
                        }
                    }
                    val modeName = prefs.optString("themeMode", "")
                    if (modeName.isNotBlank()) {
                        com.example.data.model.ThemeMode.values().firstOrNull { it.name == modeName }?.let {
                            com.example.ui.theme.AttendThemeState.mode = it
                        }
                    }
                    if (prefs.has("animationsEnabled")) {
                        com.example.ui.theme.AttendThemeState.animationsEnabled = prefs.optBoolean("animationsEnabled", true)
                    }
                } catch (_: Exception) {}
            }

            // Step 4: Restore Profile
            if (root.has("profile")) {
                val p = root.getJSONObject("profile")
                val current = dao.getProfile() ?: Profile()
                dao.saveProfile(
                    current.copy(
                        name = p.optString("name", current.name),
                        institution = p.optString("institution", current.institution),
                        department = p.optString("department", current.department),
                        section = p.optString("section", current.section),
                        group = p.optString("group", current.group),
                        sessionYear = p.optString("sessionYear", current.sessionYear),
                        semester = p.optString("semester", current.semester),
                        year = p.optString("year", current.year),
                        targetPercentage = p.optDouble("targetPercentage", current.targetPercentage.toDouble()).toFloat(),
                        isPinEnabled = p.optBoolean("isPinEnabled", current.isPinEnabled),
                        pinCode = p.optString("pinCode", current.pinCode),
                        hasCompletedOnboarding = p.optBoolean("hasCompletedOnboarding", true)
                    )
                )
            }

            // Step 5: Restore Teachers
            var teachersCount = 0
            if (root.has("teachers")) {
                val teachersArray = root.getJSONArray("teachers")
                val teachersList = mutableListOf<Teacher>()
                for (i in 0 until teachersArray.length()) {
                    val t = teachersArray.getJSONObject(i)
                    teachersList.add(
                        Teacher(
                            id = t.optLong("id", 0L),
                            name = t.optString("name", "Faculty"),
                            email = t.optString("email", ""),
                            phone = t.optString("phone", ""),
                            cabin = t.optString("cabin", ""),
                            role = t.optString("role", "Faculty"),
                            specialization = t.optString("specialization", "")
                        )
                    )
                }
                if (teachersList.isNotEmpty()) {
                    dao.insertTeachers(teachersList)
                    teachersCount = teachersList.size
                }
            }

            // Step 6: Restore Subjects (including custom target percentages)
            var subjectsCount = 0
            if (root.has("subjects")) {
                val subjectsArray = root.getJSONArray("subjects")
                val subjectsList = mutableListOf<Subject>()
                for (i in 0 until subjectsArray.length()) {
                    val s = subjectsArray.getJSONObject(i)
                    val customTarget = if (s.has("customTargetPercentage") && !s.isNull("customTargetPercentage")) {
                        s.optDouble("customTargetPercentage").toFloat()
                    } else null

                    subjectsList.add(
                        Subject(
                            id = s.optLong("id", 0L),
                            name = s.optString("name", "Subject"),
                            code = s.optString("code", ""),
                            colorHex = s.optString("colorHex", "#6366F1"),
                            customTargetPercentage = customTarget
                        )
                    )
                }
                if (subjectsList.isNotEmpty()) {
                    dao.insertSubjects(subjectsList)
                    subjectsCount = subjectsList.size
                }
            }

            // Step 7: Restore Timetable Slots
            var slotsCount = 0
            if (root.has("timetableSlots")) {
                val slotsArray = root.getJSONArray("timetableSlots")
                val slotsList = mutableListOf<TimetableSlot>()
                for (i in 0 until slotsArray.length()) {
                    val sl = slotsArray.getJSONObject(i)
                    val teacherId = if (sl.has("teacherId") && !sl.isNull("teacherId")) sl.optLong("teacherId") else null
                    val taId = if (sl.has("technicalAssistantId") && !sl.isNull("technicalAssistantId")) sl.optLong("technicalAssistantId") else null
                    val classType = try {
                        ClassType.valueOf(sl.optString("classType", "THEORY"))
                    } catch (_: Exception) {
                        ClassType.THEORY
                    }

                    slotsList.add(
                        TimetableSlot(
                            id = sl.optLong("id", 0L),
                            dayOfWeek = sl.optInt("dayOfWeek", 1),
                            subjectId = sl.optLong("subjectId", 1L),
                            teacherId = teacherId,
                            technicalAssistantId = taId,
                            technicalAssistantName = sl.optString("technicalAssistantName", ""),
                            classType = classType,
                            room = sl.optString("room", ""),
                            startTime = sl.optString("startTime", "09:00"),
                            endTime = sl.optString("endTime", "10:00")
                        )
                    )
                }
                if (slotsList.isNotEmpty()) {
                    dao.insertSlots(slotsList)
                    slotsCount = slotsList.size
                }
            }

            // Step 8: Restore Attendance Records (including backfilled past & future)
            var attendanceCount = 0
            if (root.has("attendanceRecords")) {
                val attArray = root.getJSONArray("attendanceRecords")
                val attList = mutableListOf<AttendanceRecord>()
                for (i in 0 until attArray.length()) {
                    val a = attArray.getJSONObject(i)
                    val slotId = if (a.has("slotId") && !a.isNull("slotId")) a.optLong("slotId") else null
                    val teacherId = if (a.has("teacherId") && !a.isNull("teacherId")) a.optLong("teacherId") else null
                    val classType = try {
                        ClassType.valueOf(a.optString("classType", "THEORY"))
                    } catch (_: Exception) {
                        ClassType.THEORY
                    }
                    val status = try {
                        AttendanceStatus.valueOf(a.optString("status", "PRESENT"))
                    } catch (_: Exception) {
                        AttendanceStatus.PRESENT
                    }

                    attList.add(
                        AttendanceRecord(
                            id = a.optLong("id", 0L),
                            date = a.optString("date", "2026-10-10"),
                            slotId = slotId,
                            subjectId = a.optLong("subjectId", 1L),
                            teacherId = teacherId,
                            classType = classType,
                            status = status,
                            note = a.optString("note", "")
                        )
                    )
                }
                if (attList.isNotEmpty()) {
                    dao.insertAttendanceRecords(attList)
                    attendanceCount = attList.size
                }
            }

            // Step 9: Restore Exams
            var examsCount = 0
            if (root.has("exams")) {
                val examsArray = root.getJSONArray("exams")
                val examsList = mutableListOf<Exam>()
                for (i in 0 until examsArray.length()) {
                    val e = examsArray.getJSONObject(i)
                    val examType = try {
                        com.example.data.model.ExamType.valueOf(e.optString("examType", "MID_TERM"))
                    } catch (_: Exception) {
                        com.example.data.model.ExamType.MID_TERM
                    }

                    examsList.add(
                        Exam(
                            id = e.optLong("id", 0L),
                            subjectId = e.optLong("subjectId", 1L),
                            title = e.optString("title", "Exam"),
                            date = e.optString("date", "2026-11-01"),
                            time = e.optString("time", "10:00"),
                            room = e.optString("room", ""),
                            examType = examType
                        )
                    )
                }
                if (examsList.isNotEmpty()) {
                    dao.insertExams(examsList)
                    examsCount = examsList.size
                }
            }

            // Step 10: Restore Holidays
            if (root.has("holidays")) {
                val holArray = root.getJSONArray("holidays")
                val holList = mutableListOf<Holiday>()
                for (i in 0 until holArray.length()) {
                    val h = holArray.getJSONObject(i)
                    holList.add(
                        Holiday(
                            id = h.optLong("id", 0L),
                            date = h.optString("date", "2026-11-25"),
                            title = h.optString("title", "Holiday")
                        )
                    )
                }
                if (holList.isNotEmpty()) {
                    dao.insertHolidays(holList)
                }
            }

            // Step 11: Restore Study Goals
            var studyGoalsCount = 0
            if (root.has("studyGoals")) {
                val goalsArray = root.getJSONArray("studyGoals")
                val goalsList = mutableListOf<StudyGoal>()
                for (i in 0 until goalsArray.length()) {
                    val g = goalsArray.getJSONObject(i)
                    goalsList.add(
                        StudyGoal(
                            id = g.optLong("id", 0L),
                            subjectId = g.optLong("subjectId", 1L),
                            title = g.optString("title", "Study Goal"),
                            targetHours = g.optDouble("targetHours", 5.0).toFloat(),
                            studiedHours = g.optDouble("studiedHours", 0.0).toFloat(),
                            deadlineDate = g.optString("deadlineDate", ""),
                            isCompleted = g.optBoolean("isCompleted", false),
                            priority = g.optString("priority", "Medium"),
                            notes = g.optString("notes", "")
                        )
                    )
                }
                if (goalsList.isNotEmpty()) {
                    dao.insertStudyGoals(goalsList)
                    studyGoalsCount = goalsList.size
                }
            }

            // Step 12: Restore Study Sessions
            var studySessionsCount = 0
            if (root.has("studySessions")) {
                val sessionsArray = root.getJSONArray("studySessions")
                val sessionsList = mutableListOf<StudySession>()
                for (i in 0 until sessionsArray.length()) {
                    val s = sessionsArray.getJSONObject(i)
                    val goalId = if (s.has("goalId") && !s.isNull("goalId")) s.optLong("goalId") else null
                    sessionsList.add(
                        StudySession(
                            id = s.optLong("id", 0L),
                            subjectId = s.optLong("subjectId", 1L),
                            goalId = goalId,
                            date = s.optString("date", "2026-10-10"),
                            durationMinutes = s.optInt("durationMinutes", 60),
                            topicCovered = s.optString("topicCovered", ""),
                            notes = s.optString("notes", "")
                        )
                    )
                }
                if (sessionsList.isNotEmpty()) {
                    dao.insertStudySessions(sessionsList)
                    studySessionsCount = sessionsList.size
                }
            }

            ImportResult(
                success = true,
                message = "Restore successful! Imported $subjectsCount subjects, $attendanceCount attendance records, $slotsCount timetable slots, $examsCount exams, and $studyGoalsCount study goals.",
                subjectsCount = subjectsCount,
                recordsCount = attendanceCount,
                slotsCount = slotsCount,
                examsCount = examsCount,
                studyGoalsCount = studyGoalsCount,
                studySessionsCount = studySessionsCount
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Failed to parse backup JSON: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Imports structured student timetable and schedule JSON.
     * Supports:
     * {
     *   "student": { "name": "...", "institute": "...", "department": "...", "program": "...", "year": "...", "semester": "...", "section": "...", "group": "...", "session": "..." },
     *   "notes": [ ... ],
     *   "schedule": {
     *     "Monday": [ { "type": "...", "subject_code": "...", "subject_name": "...", "teacher": { ... }, "classroom": "...", "start_time": "9:00 AM", "end_time": "10:00 AM", "technical_assistants": [ ... ], "lab_tab": "Gr. 1" } ], ...
     *   }
     * }
     */
    suspend fun importStudentScheduleJson(root: JSONObject, cleanReplace: Boolean = true): ImportResult {
        return try {
            val studentObj = root.optJSONObject("student")
            val studentName = studentObj?.optString("name", "Rajdip Dhar")?.trim() ?: "Rajdip Dhar"
            val institution = (studentObj?.optString("institute", null)
                ?: studentObj?.optString("institution", "Heritage Institute of Technology, Kolkata"))?.trim() ?: "Heritage Institute of Technology, Kolkata"
            val department = studentObj?.optString("department", "Computer Science Engineering")?.trim() ?: "Computer Science Engineering"
            val year = studentObj?.optString("year", "1st Year")?.trim() ?: "1st Year"
            val semester = studentObj?.optString("semester", "1st Semester")?.trim() ?: "1st Semester"
            val section = studentObj?.optString("section", "B")?.trim() ?: "B"
            val group = studentObj?.optString("group", "Gr. 1")?.trim() ?: "Gr. 1"
            val sessionYear = (studentObj?.optString("session", null)
                ?: studentObj?.optString("sessionYear", "2026-2027"))?.trim() ?: "2026-2027"
            val targetPercentage = studentObj?.optDouble("targetPercentage", 75.0)?.toFloat() ?: 75f

            // Extract notes
            val notesArr = root.optJSONArray("notes")
            if (notesArr != null && notesArr.length() > 0) {
                val list = mutableListOf<String>()
                for (i in 0 until notesArr.length()) {
                    val n = notesArr.optString(i, "").trim()
                    if (n.isNotBlank()) list.add(n)
                }
                if (list.isNotEmpty()) {
                    saveScheduleNotes(list.joinToString("\n• ", prefix = "• "))
                }
            }

            if (cleanReplace) {
                dao.clearAllAttendance()
                dao.clearAllExams()
                dao.clearTimetable()
                dao.clearSubjects()
                dao.clearTeachers()
                dao.clearHolidays()
                dao.clearAllStudyGoals()
                dao.clearAllStudySessions()
            }

            // Save Student Profile
            val existingProfile = dao.getProfile() ?: Profile()
            dao.saveProfile(
                existingProfile.copy(
                    id = 1,
                    name = studentName,
                    institution = institution,
                    department = department,
                    section = section,
                    group = group,
                    sessionYear = sessionYear,
                    semester = semester,
                    year = year,
                    targetPercentage = targetPercentage,
                    hasCompletedOnboarding = true
                )
            )

            val colorPalette = listOf(
                "#4F46E5", "#0284C7", "#D97706", "#10B981",
                "#6366F1", "#F59E0B", "#EC4899", "#8B5CF6",
                "#14B8A6", "#64748B", "#E11D48", "#3B82F6",
                "#06B6D4", "#84CC16", "#F97316"
            )
            var colorIdx = 0

            val subjectMap = mutableMapOf<String, Long>() // code or name -> subjectId
            val teacherMap = mutableMapOf<String, Long>() // name -> teacherId

            if (!cleanReplace) {
                dao.getAllSubjectsSync().forEach {
                    val key = getCanonicalSubjectKey(it)
                    subjectMap[key] = it.id
                    if (it.code.isNotBlank()) subjectMap[it.code.uppercase()] = it.id
                    subjectMap[it.name.lowercase()] = it.id
                }
                dao.getAllTeachersSync().forEach {
                    teacherMap[it.name.lowercase()] = it.id
                }
            }

            val dayKeyMap = mapOf(
                "monday" to 1, "mon" to 1,
                "tuesday" to 2, "tue" to 2,
                "wednesday" to 3, "wed" to 3,
                "thursday" to 4, "thu" to 4,
                "friday" to 5, "fri" to 5,
                "saturday" to 6, "sat" to 6,
                "sunday" to 7, "sun" to 7
            )

            fun parseTimeTo24H(raw: String): String {
                val s = raw.trim().uppercase()
                return try {
                    if (s.endsWith("AM") || s.endsWith("PM")) {
                        val isPm = s.endsWith("PM")
                        val clean = s.replace("AM", "").replace("PM", "").trim()
                        val parts = clean.split(":")
                        var h = parts[0].trim().toInt()
                        val m = if (parts.size > 1) parts[1].trim().toInt() else 0
                        if (isPm && h < 12) h += 12
                        if (!isPm && h == 12) h = 0
                        String.format("%02d:%02d", h, m)
                    } else if (s.contains(":")) {
                        val parts = s.split(":")
                        val h = parts[0].trim().toInt()
                        val m = parts[1].trim().toInt()
                        String.format("%02d:%02d", h, m)
                    } else {
                        s
                    }
                } catch (_: Exception) {
                    raw
                }
            }

            val scheduleObj = root.optJSONObject("schedule") ?: JSONObject()
            var slotsCount = 0

            val dayKeys = scheduleObj.keys()
            while (dayKeys.hasNext()) {
                val dayKey = dayKeys.next()
                val dow = dayKeyMap[dayKey.lowercase()] ?: continue
                val periodsArr = scheduleObj.optJSONArray(dayKey) ?: continue

                for (pIdx in 0 until periodsArr.length()) {
                    val period = periodsArr.getJSONObject(pIdx)
                    val pType = period.optString("type", "Lecture").trim()

                    if (pType.equals("Break", ignoreCase = true) ||
                        period.optString("subject_name", "").equals("Break", ignoreCase = true)
                    ) {
                        continue
                    }

                    val rawCode = period.optString("subject_code", "").trim()
                    val codeStr = if (rawCode.isNotBlank() && rawCode != "null") rawCode else ""

                    val rawName = period.optString("subject_name", "").trim()
                    val subjectName = when {
                        rawName.isNotBlank() && rawName != "null" -> rawName
                        pType.isNotBlank() -> pType
                        else -> "Subject"
                    }

                    val effectiveCode = when {
                        codeStr.isNotBlank() -> codeStr
                        subjectName.contains("Life Skills", ignoreCase = true) -> "LS101"
                        subjectName.contains("Library", ignoreCase = true) -> "LIB"
                        subjectName.contains("Remedial", ignoreCase = true) -> "REM"
                        subjectName.contains("Mentoring", ignoreCase = true) -> "MEN"
                        else -> subjectName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").take(6)
                    }

                    val canonicalName = normalizeSubjectName(subjectName)
                    val canonicalSub = Subject(name = canonicalName, code = effectiveCode)
                    val subLookupKey = getCanonicalSubjectKey(canonicalSub)
                    val subjectId = subjectMap.getOrPut(subLookupKey) {
                        val color = colorPalette[colorIdx++ % colorPalette.size]
                        val newSub = Subject(
                            name = canonicalName,
                            code = effectiveCode,
                            colorHex = color,
                            customTargetPercentage = targetPercentage
                        )
                        val insertedId = dao.insertSubject(newSub)
                        subjectMap[subLookupKey] = insertedId
                        if (effectiveCode.isNotBlank()) subjectMap[effectiveCode.uppercase()] = insertedId
                        subjectMap[canonicalName.lowercase()] = insertedId
                        insertedId
                    }

                    var teacherId: Long? = null
                    if (period.has("teacher") && !period.isNull("teacher")) {
                        val tObj = period.getJSONObject("teacher")
                        val tName = tObj.optString("name", "").trim()
                        val tInitials = tObj.optString("initials", "").trim()
                        val tDept = tObj.optString("department", "").trim()

                        val effectiveTName = when {
                            tName.isNotBlank() && tName != "null" -> tName
                            tInitials == "BB" -> "BB (Life Skills)"
                            tInitials.isNotBlank() && tInitials != "null" -> tInitials
                            else -> null
                        }

                        if (effectiveTName != null) {
                            teacherId = teacherMap.getOrPut(effectiveTName.lowercase()) {
                                val role = if (pType.equals("Lab", ignoreCase = true)) "Lab Faculty" else "Faculty"
                                val newT = Teacher(
                                    name = effectiveTName,
                                    role = role,
                                    specialization = if (tDept.isNotBlank() && tDept != "null") tDept else "",
                                    cabin = period.optString("classroom", "").takeIf { it != "null" } ?: ""
                                )
                                dao.insertTeacher(newT)
                            }
                        }
                    }

                    var firstTaId: Long? = null
                    val taNamesList = mutableListOf<String>()
                    if (period.has("technical_assistants") && !period.isNull("technical_assistants")) {
                        val taArr = period.getJSONArray("technical_assistants")
                        for (k in 0 until taArr.length()) {
                            val taObj = taArr.getJSONObject(k)
                            val taName = taObj.optString("name", "").trim()
                            val taInitials = taObj.optString("initials", "").trim()
                            val taDept = taObj.optString("department", "").trim()

                            val resolvedTaName = when {
                                taName.isNotBlank() && taName != "null" -> taName
                                taInitials.isNotBlank() && taInitials != "null" -> taInitials
                                else -> null
                            }

                            if (resolvedTaName != null) {
                                taNamesList.add(resolvedTaName)
                                val taId = teacherMap.getOrPut(resolvedTaName.lowercase()) {
                                    val newTa = Teacher(
                                        name = resolvedTaName,
                                        role = "Technical Assistant",
                                        specialization = if (taDept.isNotBlank() && taDept != "null") taDept else "",
                                        cabin = period.optString("classroom", "").takeIf { it != "null" } ?: ""
                                    )
                                    dao.insertTeacher(newTa)
                                }
                                if (firstTaId == null) firstTaId = taId
                            }
                        }
                    }

                    val labTab = period.optString("lab_tab", "").trim().takeIf { it != "null" && it.isNotBlank() }
                    val taCombinedStr = when {
                        taNamesList.isNotEmpty() && labTab != null -> "${taNamesList.joinToString(", ")} ($labTab)"
                        taNamesList.isNotEmpty() -> taNamesList.joinToString(", ")
                        labTab != null -> "Group: $labTab"
                        else -> ""
                    }

                    val classroom = period.optString("classroom", "").trim().takeIf { it != "null" } ?: ""
                    val isLab = pType.equals("Lab", ignoreCase = true)
                    val classType = if (isLab) ClassType.LAB else ClassType.THEORY

                    var rawStart = period.optString("start_time", "").trim()
                    var rawEnd = period.optString("end_time", "").trim()
                    if (rawStart.isBlank() || rawStart == "null" || rawEnd.isBlank() || rawEnd == "null") {
                        val timeSpan = period.optString("time", "").trim()
                        if (timeSpan.contains("-") || timeSpan.contains("–")) {
                            val split = timeSpan.split(Regex("[-–]"))
                            if (split.size >= 2) {
                                if (rawStart.isBlank() || rawStart == "null") rawStart = split[0].trim()
                                if (rawEnd.isBlank() || rawEnd == "null") rawEnd = split[1].trim()
                            }
                        }
                    }
                    val startTime = parseTimeTo24H(rawStart.ifBlank { "09:00 AM" })
                    val endTime = parseTimeTo24H(rawEnd.ifBlank { "10:00 AM" })

                    dao.insertSlot(
                        TimetableSlot(
                            dayOfWeek = dow,
                            subjectId = subjectId,
                            teacherId = teacherId,
                            technicalAssistantId = firstTaId,
                            technicalAssistantName = taCombinedStr,
                            classType = classType,
                            room = classroom,
                            startTime = startTime,
                            endTime = endTime
                        )
                    )
                    slotsCount++
                }
            }

            val today = LocalDate.now()
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            // Attendance starts empty: the user marks it themselves (no auto-generated past records)
            val attendanceRecordsCount = 0

            val allInsertedSubjects = dao.getAllSubjectsSync()
            val examsCount = 0 // no placeholder exams; the user adds their own

            ImportResult(
                success = true,
                message = "Successfully imported schedule for $studentName ($institution)! Imported ${allInsertedSubjects.size} subjects, $slotsCount timetable slots, and ${teacherMap.size} faculty/assistants.",
                subjectsCount = allInsertedSubjects.size,
                recordsCount = attendanceRecordsCount,
                slotsCount = slotsCount,
                examsCount = examsCount
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Failed to parse student schedule JSON: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    suspend fun resetSemesterAttendance() {
        dao.clearAllAttendance()
    }

    suspend fun resetAllData() {
        dao.clearAllAttendance()
        dao.clearAllExams()
        dao.clearTimetable()
        dao.clearSubjects()
        dao.clearTeachers()
        dao.clearHolidays()
        dao.saveProfile(Profile(id = 1, hasCompletedOnboarding = false))
    }

    /**
     * Seeds the exact academic schedule, professors, technical assistants,
     * classrooms, and student profile for Rajdip Dhar (Heritage Institute of Technology, Kolkata).
     */
    suspend fun seedRajdipDharSchedule(force: Boolean = false) {
        val currentProfile = dao.getProfile()
        if (!force && currentProfile != null && currentProfile.name == "Rajdip Dhar") {
            return // Already seeded
        }

        dao.clearAllAttendance()
        dao.clearAllExams()
        dao.clearTimetable()
        dao.clearSubjects()
        dao.clearTeachers()
        dao.clearHolidays()
        dao.clearAllStudyGoals()
        dao.clearAllStudySessions()

        // 1. Student Profile
        dao.saveProfile(
            Profile(
                id = 1,
                name = "Rajdip Dhar",
                institution = "Heritage Institute of Technology, Kolkata",
                department = "Computer Science Engineering",
                section = "B",
                group = "Gr. 1",
                sessionYear = "2026-2027",
                semester = "1st Semester",
                year = "1st Year",
                targetPercentage = 75f,
                hasCompletedOnboarding = true
            )
        )
        saveScheduleNotes("• Wednesday and Friday labs are 4 hours long; Tuesday 3-5 PM is a Library period for Gr. 1.\n• 'BB' (Life Skills) and 'New Technical Assistant' (NTA) have no full name in the timetable.")

        // 2. Academic Subjects
        val subECE1001 = dao.insertSubject(
            Subject(
                name = "Introduction to Electronic Devices and Circuits",
                code = "ECE1001",
                colorHex = "#4F46E5",
                customTargetPercentage = 75f
            )
        )
        val subMTH1101 = dao.insertSubject(
            Subject(
                name = "Mathematics-I",
                code = "MTH1101",
                colorHex = "#0284C7",
                customTargetPercentage = 75f
            )
        )
        val subPHY1001 = dao.insertSubject(
            Subject(
                name = "Physics-I",
                code = "PHY1001",
                colorHex = "#D97706",
                customTargetPercentage = 75f
            )
        )
        val subHUM1002 = dao.insertSubject(
            Subject(
                name = "Universal Human Values and Professional Ethics",
                code = "HUM1002",
                colorHex = "#10B981",
                customTargetPercentage = 75f
            )
        )
        val subMEC1051 = dao.insertSubject(
            Subject(
                name = "Workshop / Manufacturing Practice",
                code = "MEC1051",
                colorHex = "#F59E0B",
                customTargetPercentage = 75f
            )
        )
        val subMEC1052 = dao.insertSubject(
            Subject(
                name = "Engineering Graphics and Design",
                code = "MEC1052",
                colorHex = "#8B5CF6",
                customTargetPercentage = 75f
            )
        )
        val subLS = dao.insertSubject(
            Subject(
                name = "Life Skills",
                code = "LS101",
                colorHex = "#14B8A6",
                customTargetPercentage = 75f
            )
        )
        val subLIB = dao.insertSubject(
            Subject(
                name = "Library",
                code = "LIB",
                colorHex = "#64748B",
                customTargetPercentage = 75f
            )
        )
        val subREM = dao.insertSubject(
            Subject(
                name = "Remedial",
                code = "REM",
                colorHex = "#E11D48",
                customTargetPercentage = 75f
            )
        )
        val subMEN = dao.insertSubject(
            Subject(
                name = "Mentoring",
                code = "MEN",
                colorHex = "#3B82F6",
                customTargetPercentage = 75f
            )
        )

        // 3. Teachers & Technical Assistants
        val tTD = dao.insertTeacher(
            Teacher(name = "Dr. Tania Das", role = "Faculty", specialization = "ECE • Electronic Devices & Circuits", cabin = "ECE Dept", email = "tania.das@heritageit.edu")
        )
        val tSB = dao.insertTeacher(
            Teacher(name = "Dr. Samarpita Bhattacharya", role = "Faculty", specialization = "Mathematics • Mathematics-I", cabin = "Mathematics Dept", email = "samarpita.b@heritageit.edu")
        )
        val tSTM = dao.insertTeacher(
            Teacher(name = "Dr. Soumitra Maity", role = "Faculty", specialization = "Physics • Physics-I", cabin = "Physics Dept", email = "soumitra.m@heritageit.edu")
        )
        val tMDC = dao.insertTeacher(
            Teacher(name = "Dr Madhumita Chanda", role = "Faculty", specialization = "Humanities • Professional Ethics", cabin = "Humanities Dept", email = "madhumita.c@heritageit.edu")
        )
        val tAP = dao.insertTeacher(
            Teacher(name = "Dr. Ashesh Paul", role = "Faculty", specialization = "Mathematics • Mathematics-I", cabin = "Mathematics Dept", email = "ashesh.p@heritageit.edu")
        )
        val tSH = dao.insertTeacher(
            Teacher(name = "Prof. Sudip Kumar Halder", role = "Faculty", specialization = "Mechanical Engineering • Workshop Practice", cabin = "ME Dept", email = "sudip.h@heritageit.edu")
        )
        val tBB = dao.insertTeacher(
            Teacher(name = "BB (Life Skills)", role = "Life Skills Trainer", specialization = "Life Skills & Communication", cabin = "ICT311")
        )
        val tDM = dao.insertTeacher(
            Teacher(name = "Dr. Dibyendu Mal", role = "Faculty", specialization = "Physics • Physics-I & Lab", cabin = "Physics Dept", email = "dibyendu.m@heritageit.edu")
        )
        val tAM = dao.insertTeacher(
            Teacher(name = "Dr. Arindam Mandal", role = "Faculty", specialization = "Mechanical Engineering • Graphics & Design", cabin = "ME Dept", email = "arindam.m@heritageit.edu")
        )
        val tSPR = dao.insertTeacher(
            Teacher(name = "Dr Saptaparna Roy", role = "Faculty", specialization = "Humanities • Universal Human Values", cabin = "Humanities Dept", email = "saptaparna.r@heritageit.edu")
        )
        val tVKS = dao.insertTeacher(
            Teacher(name = "Prof. Vishnu Kumar Singh", role = "Faculty", specialization = "Mechanical Engineering • Graphics & Design", cabin = "ME Dept", email = "vishnu.s@heritageit.edu")
        )

        // Technical Assistants
        val taMS = dao.insertTeacher(
            Teacher(name = "Ms. Madhabi Samanta", role = "Technical Assistant", specialization = "ECE Laboratory", cabin = "ICT502")
        )
        val taAS = dao.insertTeacher(
            Teacher(name = "Mr. Arijit Sen", role = "Technical Assistant", specialization = "ECE Laboratory", cabin = "ICT502")
        )
        val taAPB = dao.insertTeacher(
            Teacher(name = "Mr. Anupam Biswas", role = "Technical Assistant", specialization = "Workshop Practice", cabin = "CME B06")
        )
        val taRR = dao.insertTeacher(
            Teacher(name = "Mr. Ritesh Roy", role = "Technical Assistant", specialization = "Workshop Practice", cabin = "CME B06")
        )
        val taMB = dao.insertTeacher(
            Teacher(name = "Mrs. Moumita Bera", role = "Technical Assistant", specialization = "Physics Laboratory", cabin = "CB106A")
        )
        val taNTA = dao.insertTeacher(
            Teacher(name = "New Technical Assistant (NTA)", role = "Technical Assistant", specialization = "Physics Laboratory", cabin = "CB106A")
        )
        val taSG = dao.insertTeacher(
            Teacher(name = "Mr. Sudipta Goswami", role = "Technical Assistant", specialization = "Engineering Graphics Lab", cabin = "CME 118")
        )

        // 4. Timetable Slots
        // MONDAY (Day 1)
        dao.insertSlot(TimetableSlot(dayOfWeek = 1, subjectId = subECE1001, teacherId = tTD, classType = ClassType.THEORY, room = "CB601", startTime = "09:00", endTime = "10:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 1, subjectId = subMTH1101, teacherId = tSB, classType = ClassType.THEORY, room = "CB601", startTime = "10:00", endTime = "11:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 1, subjectId = subPHY1001, teacherId = tSTM, classType = ClassType.THEORY, room = "CB601", startTime = "11:00", endTime = "12:00"))
        // 12:00 - 13:00 Break
        dao.insertSlot(TimetableSlot(dayOfWeek = 1, subjectId = subHUM1002, teacherId = tMDC, classType = ClassType.THEORY, room = "ICT304", startTime = "13:00", endTime = "14:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 1, subjectId = subMTH1101, teacherId = tSB, classType = ClassType.THEORY, room = "ICT304", startTime = "14:00", endTime = "15:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 1, subjectId = subECE1001, teacherId = tTD, technicalAssistantId = taMS, technicalAssistantName = "Ms. Madhabi Samanta, Mr. Arijit Sen (Gr. 1)", classType = ClassType.LAB, room = "ICT502", startTime = "15:00", endTime = "17:00"))

        // TUESDAY (Day 2)
        dao.insertSlot(TimetableSlot(dayOfWeek = 2, subjectId = subMTH1101, teacherId = tAP, classType = ClassType.THEORY, room = "ICT304", startTime = "09:00", endTime = "10:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 2, subjectId = subMEC1051, teacherId = tSH, classType = ClassType.THEORY, room = "ICT304", startTime = "10:00", endTime = "11:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 2, subjectId = subECE1001, teacherId = tTD, classType = ClassType.THEORY, room = "ICT304", startTime = "11:00", endTime = "12:00"))
        // 12:00 - 13:00 Break
        dao.insertSlot(TimetableSlot(dayOfWeek = 2, subjectId = subMTH1101, teacherId = tSB, classType = ClassType.THEORY, room = "ICT304", startTime = "13:00", endTime = "14:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 2, subjectId = subLS, teacherId = tBB, classType = ClassType.THEORY, room = "ICT311", startTime = "14:00", endTime = "15:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 2, subjectId = subLIB, teacherId = null, classType = ClassType.THEORY, room = "Library (Gr. 1)", startTime = "15:00", endTime = "17:00"))

        // WEDNESDAY (Day 3)
        // 4-Hour Lab (09:00 - 13:00)
        dao.insertSlot(TimetableSlot(dayOfWeek = 3, subjectId = subMEC1051, teacherId = tSH, technicalAssistantId = taAPB, technicalAssistantName = "Mr. Anupam Biswas, Mr. Ritesh Roy (Gr. 1)", classType = ClassType.LAB, room = "CME B06", startTime = "09:00", endTime = "13:00"))
        // 13:00 - 14:00 Break
        dao.insertSlot(TimetableSlot(dayOfWeek = 3, subjectId = subPHY1001, teacherId = tSTM, classType = ClassType.THEORY, room = "ICT304", startTime = "14:00", endTime = "15:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 3, subjectId = subECE1001, teacherId = tTD, classType = ClassType.THEORY, room = "ICT304", startTime = "15:00", endTime = "16:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 3, subjectId = subREM, teacherId = null, classType = ClassType.THEORY, room = "ICT304", startTime = "16:00", endTime = "17:00"))

        // THURSDAY (Day 4)
        // 3-Hour Lab (09:00 - 12:00)
        dao.insertSlot(TimetableSlot(dayOfWeek = 4, subjectId = subPHY1001, teacherId = tDM, technicalAssistantId = taMB, technicalAssistantName = "Mrs. Moumita Bera, New Technical Assistant (Gr. 1)", classType = ClassType.LAB, room = "CB106A", startTime = "09:00", endTime = "12:00"))
        // 12:00 - 13:00 Break
        dao.insertSlot(TimetableSlot(dayOfWeek = 4, subjectId = subPHY1001, teacherId = tDM, classType = ClassType.THEORY, room = "ICT305", startTime = "13:00", endTime = "14:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 4, subjectId = subMEC1052, teacherId = tAM, classType = ClassType.THEORY, room = "ICT306", startTime = "14:00", endTime = "15:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 4, subjectId = subHUM1002, teacherId = tSPR, classType = ClassType.THEORY, room = "ICT305", startTime = "15:00", endTime = "16:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 4, subjectId = subMEN, teacherId = null, classType = ClassType.THEORY, room = "ICT305", startTime = "16:00", endTime = "17:00"))

        // FRIDAY (Day 5)
        dao.insertSlot(TimetableSlot(dayOfWeek = 5, subjectId = subPHY1001, teacherId = tDM, classType = ClassType.THEORY, room = "ICT304", startTime = "09:00", endTime = "10:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 5, subjectId = subHUM1002, teacherId = tSPR, classType = ClassType.THEORY, room = "ICT304", startTime = "10:00", endTime = "11:00"))
        dao.insertSlot(TimetableSlot(dayOfWeek = 5, subjectId = subMTH1101, teacherId = tAP, classType = ClassType.THEORY, room = "ICT304", startTime = "11:00", endTime = "12:00"))
        // 12:00 - 13:00 Break
        // 4-Hour Lab (13:00 - 17:00)
        dao.insertSlot(TimetableSlot(dayOfWeek = 5, subjectId = subMEC1052, teacherId = tVKS, technicalAssistantId = taSG, technicalAssistantName = "Mr. Sudipta Goswami (Gr. 1)", classType = ClassType.LAB, room = "CME 118", startTime = "13:00", endTime = "17:00"))

        // 5. Prepopulate past attendance (Past 2 weeks)
        val today = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val allSeededSlots = dao.getAllSlotsSync()
        for (offset in 14 downTo 1) {
            val date = today.minusDays(offset.toLong())
            val dow = date.dayOfWeek.value
            val daySlots = allSeededSlots.filter { it.dayOfWeek == dow }
            daySlots.forEachIndexed { idx, slot ->
                val status = when {
                    (offset + idx) % 7 == 0 -> AttendanceStatus.ABSENT
                    (offset + idx) % 11 == 0 -> AttendanceStatus.CANCELLED
                    else -> AttendanceStatus.PRESENT
                }
                dao.insertOrUpdateAttendance(
                    AttendanceRecord(
                        date = date.format(fmt),
                        slotId = slot.id,
                        subjectId = slot.subjectId,
                        teacherId = slot.teacherId,
                        classType = slot.classType,
                        status = status
                    )
                )
            }
        }

        // 6. Upcoming Exams
        dao.insertExam(
            Exam(
                subjectId = subECE1001,
                title = "ECE1001 Mid-Semester Exam",
                date = today.plusDays(8).format(fmt),
                time = "10:00",
                room = "CB601",
                examType = ExamType.MID_TERM
            )
        )
        dao.insertExam(
            Exam(
                subjectId = subMTH1101,
                title = "Mathematics-I Mid-Term Examination",
                date = today.plusDays(12).format(fmt),
                time = "10:00",
                room = "CB601",
                examType = ExamType.MID_TERM
            )
        )
        dao.insertExam(
            Exam(
                subjectId = subPHY1001,
                title = "Physics-I Laboratory Viva & Evaluation",
                date = today.plusDays(15).format(fmt),
                time = "14:00",
                room = "CB106A",
                examType = ExamType.LAB_VIVA
            )
        )

        // 7. Study Goals & Sessions
        val g1 = dao.insertStudyGoal(
            StudyGoal(
                subjectId = subECE1001,
                title = "Master Semiconductor Diodes & BJT Characteristics",
                targetHours = 8.0f,
                studiedHours = 5.5f,
                deadlineDate = today.plusDays(7).format(fmt),
                priority = "High",
                notes = "Focus on diode IV curves, small-signal models, and biasing circuits."
            )
        )
        val g2 = dao.insertStudyGoal(
            StudyGoal(
                subjectId = subMTH1101,
                title = "Differential Calculus, Rolle's & Mean Value Theorems",
                targetHours = 10.0f,
                studiedHours = 6.0f,
                deadlineDate = today.plusDays(10).format(fmt),
                priority = "High",
                notes = "Practice expansion of functions using Taylor and Maclaurin series."
            )
        )
        dao.insertStudySession(
            StudySession(
                subjectId = subECE1001,
                goalId = g1,
                date = today.minusDays(1).format(fmt),
                durationMinutes = 75,
                topicCovered = "Zener Diode Regulators and Rectifier Filters",
                notes = "Solved 4 circuit problems from Dr. Tania Das's class lecture notes."
            )
        )
    }

    /**
     * Builds a comprehensive, real-time snapshot of the student's entire database
     * at this exact moment. Gives Gemini full live context of subjects, bunks,
     * attendance percentage, timetable slots, faculty, today's schedule, and exams.
     */
    suspend fun getLiveDatabaseSnapshot(): String {
        val now = java.time.LocalDateTime.now()
        val today = now.toLocalDate()
        val timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"))
        val dateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val dayName = today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

        val profile = dao.getProfile()
        val subjects = dao.getAllSubjectsSync()
        val teachers = dao.getAllTeachersSync()
        val slots = dao.getAllSlotsSync()
        val records = dao.getAllAttendanceRecordsSync()
        val exams = dao.getAllExamsSync()
        val studyGoals = dao.getAllStudyGoalsSync()

        val subMap = subjects.associateBy { it.id }
        val teachMap = teachers.associateBy { it.id }
        val defaultTarget = profile?.targetPercentage ?: 75f

        val sb = StringBuilder()
        sb.append("=== ATTENDTRACK CURRENT DATABASE SNAPSHOT (LIVE) ===\n")
        sb.append("Current System Time: $dateStr ($dayName), $timeStr\n\n")

        // 1. Profile Info
        sb.append("--- STUDENT PROFILE ---\n")
        if (profile != null) {
            sb.append("Name: ${profile.name.ifBlank { "Student" }}\n")
            sb.append("College/University: ${profile.institution.ifBlank { "Not specified" }}\n")
            sb.append("Department: ${profile.department.ifBlank { "General" }}\n")
            sb.append("Academic Year: ${profile.year} • ${profile.semester}\n")
            sb.append("Section: ${profile.section} (Group: ${profile.group})\n")
            sb.append("Attendance Target Threshold: ${profile.targetPercentage.toInt()}%\n")
        } else {
            sb.append("Profile: Default / Unconfigured (Target: 75%)\n")
        }
        val notes = getScheduleNotes()
        if (notes.isNotBlank()) {
            sb.append("\n--- TIMETABLE & LAB SCHEDULE NOTES ---\n")
            sb.append(notes)
            sb.append("\n")
        }
        sb.append("\n")

        // 2. Overall Attendance
        val totalPresent = records.count { it.status == AttendanceStatus.PRESENT }
        val totalAbsent = records.count { it.status == AttendanceStatus.ABSENT }
        val totalCancelled = records.count { it.status == AttendanceStatus.CANCELLED }
        val totalEffective = totalPresent + totalAbsent
        val overallPct = if (totalEffective > 0) (totalPresent.toFloat() / totalEffective.toFloat()) * 100f else 100f
        val overallSafeBunks = calculateSafeBunks(totalPresent, totalAbsent, defaultTarget)
        val overallNeeded = calculateClassesNeeded(totalPresent, totalAbsent, defaultTarget)

        sb.append("--- OVERALL SEMESTER ATTENDANCE STATS ---\n")
        sb.append("Total Attended (Present): $totalPresent\n")
        sb.append("Total Missed (Absent): $totalAbsent\n")
        sb.append("Total Cancelled Classes: $totalCancelled\n")
        sb.append("Total Classes Conducted (Effective): $totalEffective\n")
        sb.append("Overall Attendance Rate: ${String.format("%.1f%%", overallPct)}\n")
        sb.append("Required Target: ${defaultTarget.toInt()}%\n")
        sb.append("Status: ${if (overallPct >= defaultTarget) "ON TRACK (SAFE)" else "DEFICIT (CRITICAL)"}\n")
        if (overallSafeBunks > 0) {
            sb.append("Overall Safe Bunks Available: $overallSafeBunks lectures\n")
        } else {
            sb.append("Classes Needed to Reach Target: $overallNeeded consecutive classes\n")
        }
        sb.append("\n")

        // 3. Subject-wise breakdown
        sb.append("--- SUBJECT-WISE ATTENDANCE BREAKDOWN ---\n")
        data class SubKey(val subjectId: Long, val teacherId: Long?, val classType: ClassType)
        val groupedRecords = records.groupBy { SubKey(it.subjectId, it.teacherId, it.classType) }

        val allKeys = mutableSetOf<SubKey>()
        subjects.forEach { s ->
            allKeys.add(SubKey(s.id, null, ClassType.THEORY))
            allKeys.add(SubKey(s.id, null, ClassType.LAB))
        }
        allKeys.addAll(groupedRecords.keys)

        if (subjects.isEmpty()) {
            sb.append("No subjects enrolled yet.\n")
        } else {
            val statsList = allKeys.mapNotNull { key ->
                val sub = subMap[key.subjectId] ?: return@mapNotNull null
                val teach = key.teacherId?.let { teachMap[it] }
                val recs = groupedRecords[key] ?: emptyList()
                val pres = recs.count { it.status == AttendanceStatus.PRESENT }
                val abs = recs.count { it.status == AttendanceStatus.ABSENT }
                val canc = recs.count { it.status == AttendanceStatus.CANCELLED }
                val eff = pres + abs
                val pct = if (eff > 0) (pres.toFloat() / eff.toFloat()) * 100f else 100f
                val tgt = sub.customTargetPercentage ?: defaultTarget
                val safeB = calculateSafeBunks(pres, abs, tgt)
                val need = calculateClassesNeeded(pres, abs, tgt)

                // Only include if records exist or if it's the primary default entry for the subject
                if (recs.isNotEmpty() || (key.teacherId == null && (key.classType == ClassType.THEORY || subjects.any { it.id == sub.id }))) {
                    "$sub [${key.classType.name}]" to Triple(sub, teach, object {
                        val classType = key.classType
                        val present = pres
                        val absent = abs
                        val cancelled = canc
                        val effective = eff
                        val percentage = pct
                        val target = tgt
                        val safeBunks = safeB
                        val needed = need
                    })
                } else null
            }

            statsList.forEach { (_, data) ->
                val (sub, teacher, s) = data
                val tName = teacher?.name ?: "Assigned Faculty"
                val codeStr = if (sub.code.isNotBlank()) " (${sub.code})" else ""
                sb.append("• Subject: ${sub.name}$codeStr [${s.classType.name}]\n")
                sb.append("  - Teacher: Prof. $tName\n")
                sb.append("  - Present: ${s.present} | Absent: ${s.absent} | Cancelled: ${s.cancelled} | Total Taken: ${s.effective}\n")
                sb.append("  - Current Rate: ${String.format("%.1f%%", s.percentage)} (Goal: ${s.target.toInt()}%)\n")
                if (s.safeBunks > 0) {
                    sb.append("  - Bunk Allowance: CAN SAFELY BUNK ${s.safeBunks} MORE CLASS(ES)\n")
                } else if (s.needed > 0) {
                    sb.append("  - Warning: BELOW TARGET. MUST ATTEND ${s.needed} CONSECUTIVE CLASSES TO RESTORE SAFE ATTENDANCE\n")
                } else {
                    sb.append("  - Status: Exactly meeting goal\n")
                }
            }
        }
        sb.append("\n")

        // 4. Today's Scheduled Lectures & Status
        val dayOfWeekVal = today.dayOfWeek.value
        val todaySlots = slots.filter { it.dayOfWeek == dayOfWeekVal }.sortedBy { it.startTime }
        val todayRecords = dao.getAttendanceForDateSync(dateStr)
        val todayRecordMap = todayRecords.filter { it.slotId != null }.associateBy { it.slotId!! }

        sb.append("--- TODAY'S SCHEDULE ($dayName, $dateStr) ---\n")
        if (todaySlots.isEmpty()) {
            sb.append("No classes scheduled for today ($dayName). Day is free / off.\n")
        } else {
            todaySlots.forEach { slot ->
                val sub = subMap[slot.subjectId]?.name ?: "Subject #${slot.subjectId}"
                val t = teachMap[slot.teacherId]?.name ?: ""
                val ta = teachMap[slot.technicalAssistantId]?.name ?: slot.technicalAssistantName
                val room = if (slot.room.isNotBlank()) " in Room ${slot.room}" else ""
                val rec = todayRecordMap[slot.id]
                val statusText = when (rec?.status) {
                    AttendanceStatus.PRESENT -> "MARKED AS ATTENDED (PRESENT)"
                    AttendanceStatus.ABSENT -> "MARKED AS MISSED (ABSENT)"
                    AttendanceStatus.CANCELLED -> "MARKED AS CANCELLED"
                    null -> "PENDING / NOT RECORDED YET"
                }

                sb.append("• [${slot.getDisplayTimeSpan()}] $sub [${slot.classType}]$room\n")
                if (t.isNotBlank()) sb.append("  Faculty: Prof. $t\n")
                if (ta.isNotBlank()) sb.append("  Technical Assistant: $ta\n")
                sb.append("  Status: $statusText\n")
            }
        }
        sb.append("\n")

        // 5. Full Weekly Timetable
        sb.append("--- WEEKLY TIMETABLE OVERVIEW ---\n")
        (1..7).forEach { day ->
            val dName = java.time.DayOfWeek.of(day).name.lowercase().replaceFirstChar { it.uppercase() }
            val dSlots = slots.filter { it.dayOfWeek == day }.sortedBy { it.startTime }
            if (dSlots.isNotEmpty()) {
                val listStr = dSlots.joinToString("; ") { sl ->
                    val sName = subMap[sl.subjectId]?.name ?: "Subject"
                    "${sl.getDisplayTimeSpan()} $sName [${sl.classType}]"
                }
                sb.append("$dName: $listStr\n")
            }
        }
        sb.append("\n")

        // 6. Upcoming Exams
        sb.append("--- UPCOMING EXAMS & QUIZZES ---\n")
        val futureExams = exams.filter { it.date >= dateStr }.sortedBy { it.date }
        if (futureExams.isEmpty()) {
            sb.append("No upcoming exams registered in calendar.\n")
        } else {
            futureExams.forEach { ex ->
                val sName = subMap[ex.subjectId]?.name ?: "Subject"
                val roomStr = if (ex.room.isNotBlank()) " (Room ${ex.room})" else ""
                sb.append("• ${ex.title} for $sName on ${ex.date} at ${ex.time}$roomStr [Type: ${ex.examType}]\n")
            }
        }
        sb.append("\n")

        // 7. Faculty Directory
        sb.append("--- FACULTY & TEACHERS DIRECTORY ---\n")
        if (teachers.isEmpty()) {
            sb.append("No faculty members added.\n")
        } else {
            teachers.forEach { t ->
                val cabin = if (t.cabin.isNotBlank()) "Cabin: ${t.cabin}" else ""
                val role = t.role.ifBlank { "Faculty" }
                val spec = if (t.specialization.isNotBlank()) " | Spec: ${t.specialization}" else ""
                sb.append("• ${t.name} ($role) $cabin$spec\n")
            }
        }
        sb.append("\n=== END OF DATABASE SNAPSHOT ===\n")

        return sb.toString()
    }
}
