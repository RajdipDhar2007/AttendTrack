package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassType
import com.example.data.model.SlotWithDetails
import com.example.ui.components.CircularAttendanceRing
import com.example.ui.components.ConfettiView
import com.example.ui.components.FooterCredit
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveAnimatedBackground
import com.example.ui.components.SubjectAttendanceBarChart
import com.example.ui.theme.AttendDanger
import com.example.ui.theme.AttendSuccess
import com.example.ui.theme.AttendWarning
import com.example.ui.viewmodel.AttendTrackViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    viewModel: AttendTrackViewModel,
    onNavigateToTimetable: () -> Unit,
    onNavigateToSubjectWise: () -> Unit,
    onNavigateToBunkCalc: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToStudyPlanner: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val overallStat by viewModel.overallStat.collectAsState()
    val activeClass by viewModel.activeClass.collectAsState()
    val nextClass by viewModel.nextClass.collectAsState()
    val subjectStats by viewModel.subjectStats.collectAsState()
    val allStudyGoals by viewModel.allStudyGoals.collectAsState()

    // Interactive Date Selection for Past Backfill & Future Marking
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    val isToday = selectedDate.isEqual(today)
    val isPast = selectedDate.isBefore(today)
    val isFuture = selectedDate.isAfter(today)

    // Schedule for the currently selected date
    val selectedSchedule by viewModel.getScheduleForDate(selectedDate).collectAsState(initial = emptyList<SlotWithDetails>())

    val allSubjects by viewModel.allSubjects.collectAsState()
    val allTeachers by viewModel.allTeachers.collectAsState()
    var showBackfillDialog by remember { mutableStateOf(false) }

    var showTargetDialog by remember { mutableStateOf(false) }
    var targetSliderVal by remember { mutableFloatStateOf(overallStat.targetPercentage) }
    var showConfetti by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.undoMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(overallStat.percentage) {
        if (overallStat.percentage >= overallStat.targetPercentage && overallStat.totalEffective > 0) {
            showConfetti = true
        }
    }

    // Friendly motivational quotes for students
    val friendlyMessages = listOf(
        "\"Consistency beats talent when talent doesn't work hard.\" 💪",
        "\"One class at a time — your degree is in the making!\" 🎓",
        "\"Stay curious, ask questions, and own your semester!\" ✨",
        "\"Attending today gives you the freedom to relax tomorrow.\" 🚀"
    )
    val friendlyTip = remember(today.dayOfYear) {
        friendlyMessages[today.dayOfYear % friendlyMessages.size]
    }

    InteractiveAnimatedBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Student Friendly Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hey, ${profile?.name?.ifBlank { "Friend" } ?: "Friend"}! 👋",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${profile?.year ?: "2nd Year"} • ${profile?.semester ?: "Semester 4"} (${profile?.department ?: "Engineering"})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // Friendly student encouragement banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = friendlyTip,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }

            // Low attendance warning banner if below target
            if (overallStat.totalEffective > 0 && overallStat.percentage < overallStat.targetPercentage) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AttendDanger.copy(alpha = 0.15f))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AttendDanger,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Attendance Alert",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AttendDanger
                                )
                                Text(
                                    text = "Currently at ${String.format("%.1f%%", overallStat.percentage)} (Goal: ${overallStat.targetPercentage.toInt()}%). Attend the next ${overallStat.classesNeeded} class(es) to get back on track!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // Overall Attendance Ring Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToSubjectWise
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Overall Academic Standing",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        targetSliderVal = overallStat.targetPercentage
                                        showTargetDialog = true
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Target",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit Goal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        CircularAttendanceRing(
                            percentage = overallStat.percentage,
                            targetPercentage = overallStat.targetPercentage
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CounterBadge("Attended", overallStat.totalPresent.toString(), AttendSuccess)
                            CounterBadge("Missed", overallStat.totalAbsent.toString(), AttendDanger)
                            CounterBadge("Cancelled", overallStat.totalCancelled.toString(), Color.Gray)
                            if (overallStat.safeBunks > 0) {
                                CounterBadge("Safe Bunks", overallStat.safeBunks.toString(), MaterialTheme.colorScheme.primary)
                            } else {
                                CounterBadge("Needed", overallStat.classesNeeded.toString(), AttendWarning)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Access Pill to Bunk Calculator
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .clickable(onClick = onNavigateToBunkCalc)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open Bunk Calculator & What-If Simulator",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Recharts-inspired Subject Attendance Bar Chart Component
            item {
                SubjectAttendanceBarChart(
                    stats = subjectStats,
                    defaultTargetPercentage = overallStat.targetPercentage,
                    onNavigateToSubjectWise = onNavigateToSubjectWise
                )
            }

            // Study Planner & Goals Overview Card
            item {
                val totalTargetHours = allStudyGoals.sumOf { it.targetHours.toDouble() }.toFloat()
                val totalStudiedHours = allStudyGoals.sumOf { it.studiedHours.toDouble() }.toFloat()
                val completedGoals = allStudyGoals.count { it.isCompleted }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToStudyPlanner
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Study Planner & Goals",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "NEW",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${String.format("%.1f", totalStudiedHours)} / ${String.format("%.1f", totalTargetHours)} hrs • $completedGoals/${allStudyGoals.size} goals completed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open Study Planner",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (totalTargetHours > 0f) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val studyProgress = (totalStudiedHours / totalTargetHours).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { studyProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // Live Class Countdown Banner
            activeClass?.let { current ->
                item {
                    val minutesLeft = current.remainingSeconds / 60
                    val secondsLeft = current.remainingSeconds % 60
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(AttendSuccess)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "CLASS IN PROGRESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = AttendSuccess,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Text(
                                    text = String.format("%02d:%02d left", minutesLeft, secondsLeft),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val activeSubjTitle = if (current.subject.code.isNotBlank()) "${current.subject.code} - ${current.subject.name}" else current.subject.name
                            Text(
                                text = "$activeSubjTitle [${current.slot.classType}]",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            val roomStr = if (current.slot.room.isNotBlank()) "Room: ${current.slot.room}" else ""
                            val teacherStr = current.teacher?.let { " • ${it.name}" } ?: ""
                            Text(
                                text = "$roomStr$teacherStr (${current.slot.getDisplayTimeSpan()})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { current.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            // Up Next Class
            if (activeClass == null && nextClass != null) {
                item {
                    nextClass?.let { next ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "UP NEXT AT ${next.slot.getDisplayStartTime()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val nextSubjTitle = if (next.subject.code.isNotBlank()) "${next.subject.code} - ${next.subject.name}" else next.subject.name
                                    Text(
                                        text = "$nextSubjTitle [${next.slot.classType}]",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (next.slot.room.isNotBlank()) "Room: ${next.slot.room}" else "Scheduled slot",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // INTERACTIVE DATE PICKER & BACKFILL / FUTURE MARKING STRIP
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isToday) "Today's Schedule" else if (isPast) "Backfill Past Attendance" else "Mark Future Classes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else if (isPast) AttendWarning.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isToday) "TODAY" else if (isPast) "PAST DATE" else "FUTURE DATE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else if (isPast) AttendWarning else MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Date Picker Dialog button
                            IconButton(onClick = {
                                val dp = DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                    },
                                    selectedDate.year,
                                    selectedDate.monthValue - 1,
                                    selectedDate.dayOfMonth
                                )
                                dp.show()
                            }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Any Date", tint = MaterialTheme.colorScheme.primary)
                            }

                            if (!isToday) {
                                TextButton(onClick = { selectedDate = today }) {
                                    Text("Go to Today", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick day carousel (-3 days to +3 days)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items((-3..3).toList()) { offset ->
                            val dateItem = today.plusDays(offset.toLong())
                            val isSelected = dateItem.isEqual(selectedDate)
                            val isTodayItem = dateItem.isEqual(today)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    )
                                    .clickable { selectedDate = dateItem }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dateItem.format(DateTimeFormatter.ofPattern("EEE")),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = dateItem.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isTodayItem) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Schedule List for the selected Date
            if (selectedSchedule.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(14.dp)
                        ) {
                            Text(
                                text = "No classes scheduled for this day (${selectedDate.format(DateTimeFormatter.ofPattern("EEE"))})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use Timetable to configure classes for ${selectedDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(selectedSchedule) { item ->
                    TodayScheduleItemCard(
                        item = item,
                        onMarkStatus = { status ->
                            viewModel.markAttendance(
                                date = selectedDate,
                                slot = item.slot,
                                subjectId = item.subject.id,
                                teacherId = item.teacher?.id,
                                classType = item.slot.classType,
                                status = status
                            )
                        }
                    )
                }
            }

            // Quick backfill / add class for this date
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { showBackfillDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPast) "Backfill / Log Past Class" else if (isFuture) "Pre-Mark Future Class" else "Log Unscheduled / Extra Class",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Footer branding
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FooterCredit()
                }
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Target Dialog
        if (showTargetDialog) {
            AlertDialog(
                onDismissRequest = { showTargetDialog = false },
                title = { Text("Update Overall Attendance Target") },
                text = {
                    Column {
                        Text(
                            text = "Target: ${targetSliderVal.toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = targetSliderVal,
                            onValueChange = { targetSliderVal = it },
                            valueRange = 50f..95f,
                            steps = 8
                        )
                        Text(
                            text = "Safe bunks and recovery targets calibrate dynamically.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateTargetPercentage(targetSliderVal)
                        showTargetDialog = false
                    }) {
                        Text("Save Goal")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTargetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Backfill Dialog for Selected Date
        if (showBackfillDialog) {
            com.example.ui.components.BackfillClassDialog(
                date = selectedDate,
                subjects = allSubjects,
                teachers = allTeachers,
                onDismiss = { showBackfillDialog = false },
                onSave = { subId, teachId, classType, status ->
                    viewModel.markAttendance(
                        date = selectedDate,
                        slot = null,
                        subjectId = subId,
                        teacherId = teachId,
                        classType = classType,
                        status = status
                    )
                    showBackfillDialog = false
                }
            )
        }

        // Confetti celebration
        ConfettiView(trigger = showConfetti) {
            showConfetti = false
        }

        // Snackbar host for undo
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) { snackData ->
            Snackbar(
                action = {
                    TextButton(onClick = { viewModel.undoLastAttendanceMark() }) {
                        Text("UNDO", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            ) {
                Text(snackData.visuals.message)
            }
        }
    }
}

@Composable
fun CounterBadge(label: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
    }
}

@Composable
fun TodayScheduleItemCard(
    item: SlotWithDetails,
    onMarkStatus: (AttendanceStatus) -> Unit
) {
    val currentStatus = item.todayRecord?.status

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (item.slot.classType == ClassType.LAB) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.slot.classType.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (item.slot.classType == ClassType.LAB) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.slot.getDisplayTimeSpan(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val itemSubjTitle = if (item.subject.code.isNotBlank()) "${item.subject.code} • ${item.subject.name}" else item.subject.name
                    Text(
                        text = itemSubjTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val roomText = if (item.slot.room.isNotBlank()) "Room: ${item.slot.room}" else ""
                    val teacherText = item.teacher?.let { " • Faculty: ${it.name}" } ?: ""
                    if (roomText.isNotBlank() || teacherText.isNotBlank()) {
                        Text(
                            text = "$roomText$teacherText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    // Lab Technical Assistant display
                    val taName = item.technicalAssistant?.name ?: item.slot.technicalAssistantName
                    if (taName.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lab TA: $taName",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Current marking badge if marked
                currentStatus?.let { status ->
                    val (badgeBg, badgeText, badgeColor) = when (status) {
                        AttendanceStatus.PRESENT -> Triple(AttendSuccess.copy(alpha = 0.2f), "Attended", AttendSuccess)
                        AttendanceStatus.ABSENT -> Triple(AttendDanger.copy(alpha = 0.2f), "Missed", AttendDanger)
                        AttendanceStatus.CANCELLED -> Triple(Color.Gray.copy(alpha = 0.2f), "Cancelled", Color.LightGray)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Present, Absent, Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    text = "Present",
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    activeColor = AttendSuccess,
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkStatus(AttendanceStatus.PRESENT) }
                )
                QuickActionButton(
                    text = "Absent",
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    activeColor = AttendDanger,
                    icon = Icons.Default.Close,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkStatus(AttendanceStatus.ABSENT) }
                )
                QuickActionButton(
                    text = "Cancel",
                    isSelected = currentStatus == AttendanceStatus.CANCELLED,
                    activeColor = Color.Gray,
                    icon = Icons.Default.Cancel,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkStatus(AttendanceStatus.CANCELLED) }
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) activeColor else activeColor.copy(alpha = 0.12f)
    val textAndIconColor = if (isSelected) Color.White else activeColor

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = textAndIconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textAndIconColor
            )
        }
    }
}
