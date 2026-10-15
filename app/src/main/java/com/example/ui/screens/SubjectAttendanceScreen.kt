package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassType
import com.example.data.model.Subject
import com.example.data.model.SubjectAttendanceStat
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveAnimatedBackground
import com.example.ui.theme.AttendDanger
import com.example.ui.theme.AttendSuccess
import com.example.ui.theme.AttendWarning
import com.example.ui.viewmodel.AttendTrackViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@Composable
fun SubjectAttendanceScreen(
    viewModel: AttendTrackViewModel
) {
    val subjectStats by viewModel.subjectStatsByType.collectAsState()
    var filterType by remember { mutableStateOf<ClassType?>(null) } // null = All

    var subjectToEditTarget by remember { mutableStateOf<Subject?>(null) }
    var targetSliderVal by remember { mutableFloatStateOf(75f) }

    var showAddSubjectDialog by remember { mutableStateOf(false) }

    // Dialog popup for marking attendance with instant undo capability
    var attendanceActionSubject by remember { mutableStateOf<SubjectAttendanceStat?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.undoMessage.collectLatest { msg ->
            if (msg.startsWith("Marked as")) {
                val res = snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (res == SnackbarResult.ActionPerformed) {
                    viewModel.undoLastAttendanceMark()
                }
            } else {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val filteredList = remember(subjectStats, filterType) {
        subjectStats.filter { stat ->
            filterType == null || stat.classType == filterType
        }
    }

    InteractiveAnimatedBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subject-Wise Tracking",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any card to mark, edit, or undo attendance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips: All, Theory, Lab
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Pair(null, "All Classes"),
                    Pair(ClassType.THEORY, "Lectures / Theory"),
                    Pair(ClassType.LAB, "Labs Only")
                ).forEach { (type, label) ->
                    val isSelected = filterType == type
                    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                    val textCol = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .clickable { filterType = type }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textCol
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text(
                                text = "No subjects match current filter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = filteredList,
                        key = { "${it.subject.id}_${it.teacher?.id ?: 0}_${it.classType.name}" }
                    ) { item ->
                        SubjectStatCard(
                            stat = item,
                            onEditTarget = {
                                subjectToEditTarget = item.subject
                                targetSliderVal = item.targetPercentage
                            },
                            onOpenActionDialog = {
                                attendanceActionSubject = item
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }
            }
        }

        // Add Subject FAB
        FloatingActionButton(
            onClick = { showAddSubjectDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 20.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Subject", tint = Color.White)
        }

        // Attendance Action Popup Dialog with Slidebars marked with number and Undo option
        attendanceActionSubject?.let { target ->
            var attendedCountSlider by remember(target.subject.id, target.classType) { mutableFloatStateOf(1f) }
            var missedCountSlider by remember(target.subject.id, target.classType) { mutableFloatStateOf(1f) }

            AlertDialog(
                onDismissRequest = { attendanceActionSubject = null },
                title = {
                    Column {
                        Text(
                            text = "${target.subject.code} ${target.subject.name}".trim(),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        val safePercentage = if (target.percentage.isNaN() || target.percentage.isInfinite()) 0f else target.percentage
                        Text(
                            text = "${target.classType.name} Register • Current: ${String.format("%.1f%%", safePercentage)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Set number of lectures and mark attendance:",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // 1. Mark as Attended (Present) with Slidebar marked with numbers
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AttendSuccess.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = AttendSuccess, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Mark as Attended (Present)",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AttendSuccess
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AttendSuccess)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${attendedCountSlider.toInt()} ${if (attendedCountSlider.toInt() == 1) "Lecture" else "Lectures"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Text(
                                    text = "Slide to choose number of attended classes (1 to 10):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )

                                Slider(
                                    value = attendedCountSlider,
                                    onValueChange = { attendedCountSlider = it },
                                    valueRange = 1f..10f,
                                    steps = 8,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = AttendSuccess,
                                        activeTrackColor = AttendSuccess,
                                        inactiveTrackColor = AttendSuccess.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Number tick markings
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    (1..10).forEach { num ->
                                        Text(
                                            text = "$num",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = if (attendedCountSlider.toInt() == num) FontWeight.ExtraBold else FontWeight.Normal,
                                            color = if (attendedCountSlider.toInt() == num) AttendSuccess else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val count = attendedCountSlider.toInt()
                                        viewModel.markMultipleAttendance(
                                            date = LocalDate.now(),
                                            subjectId = target.subject.id,
                                            teacherId = target.teacher?.id,
                                            classType = target.classType,
                                            status = AttendanceStatus.PRESENT,
                                            count = count
                                        )
                                        attendanceActionSubject = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AttendSuccess),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add +${attendedCountSlider.toInt()} Attended",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 2. Mark as Missed (Absent) with Slidebar marked with numbers
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AttendDanger.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = AttendDanger, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Mark as Missed (Absent)",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AttendDanger
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AttendDanger)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${missedCountSlider.toInt()} ${if (missedCountSlider.toInt() == 1) "Lecture" else "Lectures"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Text(
                                    text = "Slide to choose number of missed classes (1 to 10):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )

                                Slider(
                                    value = missedCountSlider,
                                    onValueChange = { missedCountSlider = it },
                                    valueRange = 1f..10f,
                                    steps = 8,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = AttendDanger,
                                        activeTrackColor = AttendDanger,
                                        inactiveTrackColor = AttendDanger.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Number tick markings
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    (1..10).forEach { num ->
                                        Text(
                                            text = "$num",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = if (missedCountSlider.toInt() == num) FontWeight.ExtraBold else FontWeight.Normal,
                                            color = if (missedCountSlider.toInt() == num) AttendDanger else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val count = missedCountSlider.toInt()
                                        viewModel.markMultipleAttendance(
                                            date = LocalDate.now(),
                                            subjectId = target.subject.id,
                                            teacherId = target.teacher?.id,
                                            classType = target.classType,
                                            status = AttendanceStatus.ABSENT,
                                            count = count
                                        )
                                        attendanceActionSubject = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AttendDanger),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add +${missedCountSlider.toInt()} Missed",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 3. Undo Last Recorded Choice
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .clickable {
                                    viewModel.undoLastAttendanceMark()
                                    attendanceActionSubject = null
                                }
                                .padding(vertical = 10.dp, horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Undo Last Recorded Choice", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("Revert previous marking for this session", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { attendanceActionSubject = null }) {
                        Text("Close")
                    }
                }
            )
        }

        // Edit Subject Target Dialog
        if (subjectToEditTarget != null) {
            AlertDialog(
                onDismissRequest = { subjectToEditTarget = null },
                title = { Text("Target for ${subjectToEditTarget?.name}") },
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
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        subjectToEditTarget?.let { sub ->
                            viewModel.updateSubject(sub.copy(customTargetPercentage = targetSliderVal))
                        }
                        subjectToEditTarget = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { subjectToEditTarget = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Add Subject Dialog
        if (showAddSubjectDialog) {
            var newName by remember { mutableStateOf("") }
            var newCode by remember { mutableStateOf("") }
            var newTarget by remember { mutableFloatStateOf(75f) }

            AlertDialog(
                onDismissRequest = { showAddSubjectDialog = false },
                title = { Text("Add Academic Subject") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Subject Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newCode,
                            onValueChange = { newCode = it },
                            label = { Text("Subject Code (e.g. CS401)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Target: ${newTarget.toInt()}%", fontWeight = FontWeight.Bold)
                        Slider(
                            value = newTarget,
                            onValueChange = { newTarget = it },
                            valueRange = 50f..95f,
                            steps = 8
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addSubject(
                                name = newName,
                                code = newCode,
                                colorHex = "#6366F1",
                                target = newTarget
                            )
                            showAddSubjectDialog = false
                        }
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSubjectDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }
}

@Composable
fun SubjectStatCard(
    stat: SubjectAttendanceStat,
    onEditTarget: () -> Unit,
    onOpenActionDialog: () -> Unit
) {
    val isGoalMet = stat.percentage >= stat.targetPercentage
    val statusColor = if (isGoalMet) AttendSuccess else AttendDanger

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenActionDialog)
    ) {
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
                                    if (stat.classType == ClassType.LAB) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (stat.classType == ClassType.LAB) Icons.Default.Science else Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = if (stat.classType == ClassType.LAB) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stat.classType.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (stat.classType == ClassType.LAB) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (stat.subject.code.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stat.subject.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stat.subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    stat.teacher?.let { t ->
                        Text(
                            text = "Prof. ${t.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                // Score block
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%.1f%%", stat.percentage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onEditTarget)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Goal: ${stat.targetPercentage.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Goal",
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = { (stat.percentage / 100f).coerceIn(0f, 1f) },
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sub text: Safe Bunk vs Needed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stat.presentCount} attended / ${stat.effectiveCount} taken (${stat.cancelledCount} canc.)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                if (stat.safeBunks > 0) {
                    Text(
                        text = "${stat.safeBunks} safe bunk(s)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (stat.neededToTarget > 0) {
                    Text(
                        text = "Attend ${stat.neededToTarget} to recover",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AttendDanger
                    )
                } else {
                    Text(
                        text = "On Track",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AttendSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tap for options action bar (Visual indicator)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tap to Mark, Edit or Undo Attendance",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
