package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyGoal
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.data.model.SubjectStudyStat
import com.example.ui.components.FooterCredit
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveAnimatedBackground
import com.example.ui.theme.AttendDanger
import com.example.ui.theme.AttendSuccess
import com.example.ui.theme.AttendWarning
import com.example.ui.viewmodel.AttendTrackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlannerScreen(
    viewModel: AttendTrackViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allSubjects by viewModel.allSubjects.collectAsState()
    val allGoals by viewModel.allStudyGoals.collectAsState()
    val allSessions by viewModel.allStudySessions.collectAsState()
    val subjectStudyStats by viewModel.subjectStudyStats.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Goals & Subjects, 1 = Study Logs, 2 = Focus Timer
    var selectedSubjectFilterId by remember { mutableStateOf<Long?>(null) } // null = All Subjects

    // Dialog States
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showLogSessionDialog by remember { mutableStateOf(false) }
    var targetSubjectForAction by remember { mutableStateOf<Subject?>(null) }
    var targetGoalForSession by remember { mutableStateOf<StudyGoal?>(null) }

    // Aggregate statistics
    val totalTargetHours = allGoals.sumOf { it.targetHours.toDouble() }.toFloat()
    val totalStudiedHours = allGoals.sumOf { it.studiedHours.toDouble() }.toFloat()
    val completedGoalsCount = allGoals.count { it.isCompleted }

    InteractiveAnimatedBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("study_planner_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Study Planner",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Set subject goals & track study hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        targetSubjectForAction = allSubjects.firstOrNull()
                        targetGoalForSession = null
                        showAddGoalDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_study_goal_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Goal")
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview Progress Card
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                Column {
                                    Text(
                                        text = "Weekly Study Target",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = String.format("%.1f hrs studied", totalStudiedHours),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Target: ${String.format("%.1f", totalTargetHours)} hrs across ${allSubjects.size} subjects",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$completedGoalsCount/${allGoals.size}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Goals Done",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val overallStudyFraction = if (totalTargetHours > 0f) (totalStudiedHours / totalTargetHours).coerceIn(0f, 1f) else 0f
                            LinearProgressIndicator(
                                progress = { overallStudyFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        targetSubjectForAction = allSubjects.firstOrNull()
                                        targetGoalForSession = null
                                        showLogSessionDialog = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("log_hours_quick_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Log Study Hours", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { selectedTab = 2 },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Focus Timer", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Tab Row (0: Goals & Subjects, 1: Session Logs, 2: Focus Timer)
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Subject Goals", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Logs & History", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Focus Stopwatch", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // TAB 0: Subject Goals & Filtering
                        item {
                            // Subject Filter Chips
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedSubjectFilterId == null,
                                        onClick = { selectedSubjectFilterId = null },
                                        label = { Text("All Subjects (${allSubjects.size})") },
                                        colors = FilterChipDefaults.filterChipColors()
                                    )
                                }
                                items(allSubjects) { sub ->
                                    val isSel = selectedSubjectFilterId == sub.id
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedSubjectFilterId = if (isSel) null else sub.id },
                                        label = { Text(sub.code.ifBlank { sub.name }) }
                                    )
                                }
                            }
                        }

                        val filteredSubjects = if (selectedSubjectFilterId == null) {
                            allSubjects
                        } else {
                            allSubjects.filter { it.id == selectedSubjectFilterId }
                        }

                        if (filteredSubjects.isEmpty()) {
                            item {
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No subjects configured yet.",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Add subjects in the Subjects tab to set study goals.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredSubjects) { subject ->
                                val stat = subjectStudyStats.firstOrNull { it.subject.id == subject.id }
                                val subjectGoals = allGoals.filter { it.subjectId == subject.id }

                                SubjectStudyCard(
                                    subject = subject,
                                    stat = stat,
                                    goals = subjectGoals,
                                    onAddGoal = {
                                        targetSubjectForAction = subject
                                        targetGoalForSession = null
                                        showAddGoalDialog = true
                                    },
                                    onLogHours = {
                                        targetSubjectForAction = subject
                                        targetGoalForSession = null
                                        showLogSessionDialog = true
                                    },
                                    onToggleGoal = { goal ->
                                        viewModel.toggleStudyGoalCompleted(goal)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (!goal.isCompleted) "Awesome! Completed: ${goal.title} 🎉" else "Marked ${goal.title} as active"
                                            )
                                        }
                                    },
                                    onDeleteGoal = { goal ->
                                        viewModel.deleteStudyGoal(goal)
                                        scope.launch { snackbarHostState.showSnackbar("Deleted goal: ${goal.title}") }
                                    },
                                    onLogForGoal = { goal ->
                                        targetSubjectForAction = subject
                                        targetGoalForSession = goal
                                        showLogSessionDialog = true
                                    }
                                )
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: Study Session History / Logs
                        if (allSessions.isEmpty()) {
                            item {
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "No Study Sessions Logged Yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tap 'Log Study Hours' or use the Focus Timer to track your study sessions.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(allSessions) { session ->
                                val subj = allSubjects.firstOrNull { it.id == session.subjectId }
                                StudySessionLogItem(
                                    session = session,
                                    subject = subj,
                                    onDelete = {
                                        viewModel.deleteStudySession(session)
                                        scope.launch { snackbarHostState.showSnackbar("Removed study session log") }
                                    }
                                )
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: Live Focus Timer / Stopwatch
                        item {
                            FocusStopwatchCard(
                                subjects = allSubjects,
                                onSaveSession = { subjectId, minutes, topic ->
                                    viewModel.logStudySession(
                                        subjectId = subjectId,
                                        goalId = null,
                                        durationMinutes = minutes,
                                        topicCovered = topic,
                                        notes = "Logged via Focus Stopwatch"
                                    )
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Logged $minutes min focus study session! 🎯")
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        FooterCredit()
                    }
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // Dialog: Add Study Goal
    if (showAddGoalDialog) {
        AddStudyGoalDialog(
            subjects = allSubjects,
            initialSubject = targetSubjectForAction ?: allSubjects.firstOrNull(),
            onDismiss = { showAddGoalDialog = false },
            onSave = { subjectId, title, targetHours, deadlineDate, priority, notes ->
                viewModel.addStudyGoal(
                    subjectId = subjectId,
                    title = title,
                    targetHours = targetHours,
                    deadlineDate = deadlineDate,
                    priority = priority,
                    notes = notes
                )
                showAddGoalDialog = false
                scope.launch { snackbarHostState.showSnackbar("Goal added: $title 🚀") }
            }
        )
    }

    // Dialog: Log Study Hours / Session
    if (showLogSessionDialog) {
        LogStudySessionDialog(
            subjects = allSubjects,
            initialSubject = targetSubjectForAction ?: allSubjects.firstOrNull(),
            linkedGoal = targetGoalForSession,
            onDismiss = { showLogSessionDialog = false },
            onSave = { subjectId, goalId, durationMinutes, topicCovered, notes, date ->
                viewModel.logStudySession(
                    subjectId = subjectId,
                    goalId = goalId,
                    durationMinutes = durationMinutes,
                    topicCovered = topicCovered,
                    notes = notes,
                    date = date
                )
                showLogSessionDialog = false
                scope.launch {
                    val hrsStr = if (durationMinutes >= 60) "${durationMinutes / 60}h ${durationMinutes % 60}m" else "${durationMinutes}m"
                    snackbarHostState.showSnackbar("Logged $hrsStr of study time! Great job! ⭐")
                }
            }
        )
    }
}

@Composable
private fun SubjectStudyCard(
    subject: Subject,
    stat: SubjectStudyStat?,
    goals: List<StudyGoal>,
    onAddGoal: () -> Unit,
    onLogHours: () -> Unit,
    onToggleGoal: (StudyGoal) -> Unit,
    onDeleteGoal: (StudyGoal) -> Unit,
    onLogForGoal: (StudyGoal) -> Unit
) {
    val studiedHours = stat?.totalStudiedHours ?: 0f
    val targetHours = stat?.totalTargetHours ?: 0f
    val progress = stat?.progressPercentage ?: 0f

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_study_card_${subject.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            // Subject Title & Quick Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (subject.code.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = subject.code,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format("%.1f", studiedHours)} / ${String.format("%.1f", targetHours)}h",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progress >= 100f) AttendSuccess else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Goals list for this subject
            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No goals set yet for this subject. Set a goal to stay focused!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    goals.forEach { goal ->
                        GoalItemRow(
                            goal = goal,
                            onToggle = { onToggleGoal(goal) },
                            onDelete = { onDeleteGoal(goal) },
                            onLogForGoal = { onLogForGoal(goal) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card Bottom Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddGoal,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Set Goal", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onLogHours,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Log Hours", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GoalItemRow(
    goal: StudyGoal,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onLogForGoal: () -> Unit
) {
    val priorityColor = when (goal.priority.lowercase()) {
        "high" -> AttendDanger
        "medium" -> AttendWarning
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (goal.isCompleted) AttendSuccess.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox icon
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (goal.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Goal Done",
                    tint = if (goal.isCompleted) AttendSuccess else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (goal.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = goal.priority,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = priorityColor
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", goal.studiedHours)} / ${String.format("%.1f", goal.targetHours)} hrs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (goal.deadlineDate.isNotBlank()) {
                        Text(
                            text = "Due: ${goal.deadlineDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Quick add hours for this goal
            IconButton(
                onClick = onLogForGoal,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Log hours for this goal",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Goal",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun StudySessionLogItem(
    session: StudySession,
    subject: Subject?,
    onDelete: () -> Unit
) {
    val hrs = session.durationMinutes / 60
    val mins = session.durationMinutes % 60
    val durationText = if (hrs > 0 && mins > 0) "${hrs}h ${mins}m" else if (hrs > 0) "${hrs}h" else "${mins}m"

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    val codeStr = if (!subject?.code.isNullOrBlank()) "${subject?.code} • " else ""
                    Text(
                        text = "$codeStr${subject?.name ?: "Subject"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (session.topicCovered.isNotBlank()) {
                        Text(
                            text = session.topicCovered,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        text = session.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Session",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusStopwatchCard(
    subjects: List<Subject>,
    onSaveSession: (subjectId: Long, minutes: Int, topic: String) -> Unit
) {
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var topicText by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    val hrs = elapsedSeconds / 3600
    val mins = (elapsedSeconds % 3600) / 60
    val secs = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d:%02d", hrs, mins, secs)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Live Focus Stopwatch",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Time your study session and save directly to your hours",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Big Timer Display
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(3.dp, if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isRunning) "FOCUSING" else if (elapsedSeconds > 0) "PAUSED" else "READY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) AttendSuccess else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subject selector
            if (subjects.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subjects) { sub ->
                        val isSel = selectedSubject?.id == sub.id
                        FilterChip(
                            selected = isSel,
                            onClick = { if (!isRunning) selectedSubject = sub },
                            label = { Text(sub.code.ifBlank { sub.name }) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = topicText,
                onValueChange = { topicText = it },
                label = { Text("Topic / Chapter studied") },
                placeholder = { Text("e.g. Binary Search Trees") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stopwatch Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isRunning) {
                    Button(
                        onClick = { isRunning = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (elapsedSeconds > 0) "Resume" else "Start Focus")
                    }
                } else {
                    OutlinedButton(
                        onClick = { isRunning = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause")
                    }
                }

                if (elapsedSeconds > 0) {
                    Button(
                        onClick = {
                            isRunning = false
                            val minutesLogged = (elapsedSeconds / 60).toInt().coerceAtLeast(1)
                            selectedSubject?.let { sub ->
                                onSaveSession(sub.id, minutesLogged, topicText.ifBlank { "Focus Session" })
                            }
                            elapsedSeconds = 0L
                            topicText = ""
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedSubject != null
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Log")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStudyGoalDialog(
    subjects: List<Subject>,
    initialSubject: Subject?,
    onDismiss: () -> Unit,
    onSave: (subjectId: Long, title: String, targetHours: Float, deadlineDate: String, priority: String, notes: String) -> Unit
) {
    val context = LocalContext.current
    var selectedSubject by remember { mutableStateOf(initialSubject ?: subjects.firstOrNull()) }
    var title by remember { mutableStateOf("") }
    var targetHoursSlider by remember { mutableStateOf(10f) }
    var deadlineDate by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Medium") }
    var notes by remember { mutableStateOf("") }

    var subjectExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Subject Study Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Subject Dropdown
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSubject?.name ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        subjects.forEach { s ->
                            val codePrefix = if (s.code.isNotBlank()) "${s.code} - " else ""
                            DropdownMenuItem(
                                text = { Text("$codePrefix${s.name}") },
                                onClick = {
                                    selectedSubject = s
                                    subjectExpanded = false
                                }
                            )
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title *") },
                    placeholder = { Text("e.g. Master Binary Trees & AVL Rotations") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Target Hours Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target Study Hours:", style = MaterialTheme.typography.bodySmall)
                        Text("${targetHoursSlider.toInt()} Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = targetHoursSlider,
                        onValueChange = { targetHoursSlider = it },
                        valueRange = 1f..50f,
                        steps = 48
                    )
                }

                // Deadline Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = deadlineDate,
                        onValueChange = { deadlineDate = it },
                        label = { Text("Target Deadline") },
                        placeholder = { Text("YYYY-MM-DD (Optional)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = {
                        val today = LocalDate.now()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                deadlineDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                            },
                            today.year,
                            today.monthValue - 1,
                            today.dayOfMonth
                        ).show()
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Priority Selection
                Column {
                    Text("Priority Level:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Low", "Medium", "High").forEach { pri ->
                            FilterChip(
                                selected = selectedPriority == pri,
                                onClick = { selectedPriority = pri },
                                label = { Text(pri) }
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Topics (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedSubject?.let { s ->
                        if (title.isNotBlank()) {
                            onSave(s.id, title, targetHoursSlider, deadlineDate, selectedPriority, notes)
                        }
                    }
                },
                enabled = selectedSubject != null && title.isNotBlank()
            ) {
                Text("Save Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogStudySessionDialog(
    subjects: List<Subject>,
    initialSubject: Subject?,
    linkedGoal: StudyGoal?,
    onDismiss: () -> Unit,
    onSave: (subjectId: Long, goalId: Long?, durationMinutes: Int, topicCovered: String, notes: String, date: String) -> Unit
) {
    val context = LocalContext.current
    var selectedSubject by remember { mutableStateOf(initialSubject ?: subjects.firstOrNull()) }
    var durationMinutes by remember { mutableIntStateOf(60) }
    var topicCovered by remember { mutableStateOf(linkedGoal?.title ?: "") }
    var notes by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }

    var subjectExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Study Hours", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Subject Dropdown
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSubject?.name ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        subjects.forEach { s ->
                            val codePrefix = if (s.code.isNotBlank()) "${s.code} - " else ""
                            DropdownMenuItem(
                                text = { Text("$codePrefix${s.name}") },
                                onClick = {
                                    selectedSubject = s
                                    subjectExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quick Duration Chips
                Column {
                    Text("Time Spent: ${durationMinutes / 60}h ${durationMinutes % 60}m ($durationMinutes min)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30, 45, 60, 90, 120, 180).forEach { mins ->
                            val label = if (mins < 60) "${mins}m" else "${mins / 60}h${if (mins % 60 > 0) " 30m" else ""}"
                            item {
                                FilterChip(
                                    selected = durationMinutes == mins,
                                    onClick = { durationMinutes = mins },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                // Topic / Chapters covered
                OutlinedTextField(
                    value = topicCovered,
                    onValueChange = { topicCovered = it },
                    label = { Text("Topic / Chapter covered") },
                    placeholder = { Text("e.g. Chapter 4 Practice Problems") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dateString,
                        onValueChange = { dateString = it },
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = {
                        val today = LocalDate.now()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                dateString = String.format("%04d-%02d-%02d", y, m + 1, d)
                            },
                            today.year,
                            today.monthValue - 1,
                            today.dayOfMonth
                        ).show()
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Key Takeaways / Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedSubject?.let { s ->
                        onSave(s.id, linkedGoal?.id, durationMinutes, topicCovered, notes, dateString)
                    }
                },
                enabled = selectedSubject != null && durationMinutes > 0
            ) {
                Text("Log Hours")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
