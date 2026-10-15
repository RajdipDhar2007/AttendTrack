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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassType
import com.example.data.model.SlotWithDetails
import com.example.data.model.Subject
import com.example.data.model.Teacher
import com.example.ui.components.FooterCredit
import com.example.ui.components.GlassCard
import com.example.ui.components.AnimatedCountText
import com.example.ui.components.AnimatedProgressBar
import com.example.ui.components.InteractiveAnimatedBackground
import com.example.ui.components.bounceClick
import com.example.ui.components.staggeredEntrance
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.example.ui.theme.AttendDanger
import com.example.ui.theme.AttendSuccess
import com.example.ui.theme.AttendWarning
import com.example.ui.viewmodel.AttendTrackViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyAttendanceScreen(
    viewModel: AttendTrackViewModel
) {
    val subjectStats by viewModel.subjectStats.collectAsState()
    val subjects by viewModel.allSubjects.collectAsState()
    val teachers by viewModel.allTeachers.collectAsState()

    var selectedYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showBackfillDialog by remember { mutableStateOf(false) }

    val monthTitle = selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val scheduleForDate by viewModel.getScheduleForDate(selectedDate).collectAsState(initial = emptyList())

    InteractiveAnimatedBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Monthly Attendance & Backfill",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Calendar matrix, past day backfilling & future planning",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Month Selector Header
            item {
                GlassCard(modifier = Modifier.fillMaxWidth().staggeredEntrance(1)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            selectedYearMonth = selectedYearMonth.minusMonths(1)
                            selectedDate = selectedYearMonth.atDay(1)
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            selectedYearMonth = selectedYearMonth.plusMonths(1)
                            selectedDate = selectedYearMonth.atDay(1)
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }
                }
            }

            // Interactive Month Calendar Grid
            item {
                GlassCard(modifier = Modifier.fillMaxWidth().staggeredEntrance(2)) {
                    Column {
                        Text(
                            text = "Interactive Attendance Matrix",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap any date to backfill past records or schedule future classes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Days of week header (Mon - Sun)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { dayLabel ->
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val daysInMonth = selectedYearMonth.lengthOfMonth()
                        val firstDayOfWeek = selectedYearMonth.atDay(1).dayOfWeek.value // 1..7 (Mon=1)
                        val totalSlots = (firstDayOfWeek - 1) + daysInMonth
                        val numRows = (totalSlots + 6) / 7

                        for (row in 0 until numRows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNum = cellIndex - (firstDayOfWeek - 1) + 1

                                    if (dayNum in 1..daysInMonth) {
                                        val cellDate = selectedYearMonth.atDay(dayNum)
                                        val isSelected = cellDate.isEqual(selectedDate)
                                        val isToday = cellDate.isEqual(LocalDate.now())
                                        val cellBg by animateColorAsState(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                else -> Color.Transparent
                                            },
                                            tween(250),
                                            label = "cell_bg"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(cellBg)
                                                .bounceClick(pressedScale = 0.85f) { selectedDate = cellDate },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isToday -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Date Breakdown & Backfill Action Section
            item {
                val isToday = selectedDate.isEqual(LocalDate.now())
                val isPast = selectedDate.isBefore(LocalDate.now())

                GlassCard(modifier = Modifier.fillMaxWidth().staggeredEntrance(3)) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
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
                                    text = if (isPast) "Backfill past classes on this day" else "Manage classes on this day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            // Button to log an ad-hoc class for this date
                            TextButton(onClick = { showBackfillDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Class", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (scheduleForDate.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No timetable slots found for this weekday",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap '+ Log Class' above to record or backfill attendance for any subject.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        } else {
                            scheduleForDate.forEach { item ->
                                val currentStatus = item.todayRecord?.status
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            val schedSubjTitle = if (item.subject.code.isNotBlank()) "${item.subject.code} • ${item.subject.name}" else item.subject.name
                                            Text(
                                                text = "$schedSubjTitle [${item.slot.classType}]",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val teacherStr = item.teacher?.let { "Prof. ${it.name} • " } ?: ""
                                            Text(
                                                text = "$teacherStr${item.slot.getDisplayTimeSpan()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(badgeText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = badgeColor)
                                                }
                                            }

                                            // If ad-hoc / backfilled class, allow quick delete
                                            if (item.slot.id < 0 && item.todayRecord != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                IconButton(
                                                    onClick = { viewModel.deleteAttendanceRecord(item.todayRecord.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete backfilled class",
                                                        tint = AttendDanger.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Mark buttons: Attendance, Missed, Cancelled
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        QuickStatusPill(
                                            label = "Attendance",
                                            isSelected = currentStatus == AttendanceStatus.PRESENT,
                                            activeColor = AttendSuccess,
                                            icon = Icons.Default.Check,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                viewModel.toggleOrMarkAttendance(
                                                    date = selectedDate,
                                                    slot = item.slot,
                                                    subjectId = item.subject.id,
                                                    teacherId = item.teacher?.id,
                                                    classType = item.slot.classType,
                                                    status = AttendanceStatus.PRESENT,
                                                    currentRecord = item.todayRecord
                                                )
                                            }
                                        )
                                        QuickStatusPill(
                                            label = "Missed",
                                            isSelected = currentStatus == AttendanceStatus.ABSENT,
                                            activeColor = AttendDanger,
                                            icon = Icons.Default.Close,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                viewModel.toggleOrMarkAttendance(
                                                    date = selectedDate,
                                                    slot = item.slot,
                                                    subjectId = item.subject.id,
                                                    teacherId = item.teacher?.id,
                                                    classType = item.slot.classType,
                                                    status = AttendanceStatus.ABSENT,
                                                    currentRecord = item.todayRecord
                                                )
                                            }
                                        )
                                        QuickStatusPill(
                                            label = "Cancelled",
                                            isSelected = currentStatus == AttendanceStatus.CANCELLED,
                                            activeColor = Color.Gray,
                                            icon = Icons.Default.Cancel,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                viewModel.toggleOrMarkAttendance(
                                                    date = selectedDate,
                                                    slot = item.slot,
                                                    subjectId = item.subject.id,
                                                    teacherId = item.teacher?.id,
                                                    classType = item.slot.classType,
                                                    status = AttendanceStatus.CANCELLED,
                                                    currentRecord = item.todayRecord
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Distribution Chart Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth().staggeredEntrance(3)) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Semester Subject Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${subjectStats.size} Subjects",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (subjectStats.isEmpty()) {
                            Text(
                                text = "No academic subjects found.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            subjectStats.forEach { stat ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            val subjCode = if (stat.subject.code.isNotBlank()) "${stat.subject.code} • " else ""
                                            Text(
                                                text = "$subjCode${stat.subject.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Attended: ${stat.presentCount}  •  Missed: ${stat.absentCount}  •  Cancelled: ${stat.cancelledCount}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                            )
                                        }
                                        AnimatedCountText(
                                            value = stat.percentage,
                                            decimals = 1,
                                            suffix = "%",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (stat.percentage >= stat.targetPercentage) AttendSuccess else AttendDanger
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AnimatedProgressBar(
                                        fraction = stat.percentage / 100f,
                                        fillColor = if (stat.percentage >= stat.targetPercentage) AttendSuccess else AttendDanger
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Month Retention Insight
            item {
                GlassCard(modifier = Modifier.fillMaxWidth().staggeredEntrance(4)) {
                    Column {
                        Text(
                            text = "Monthly Retention & Policy Rule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Cancelled classes and declared holidays are strictly excluded from the attendance calculation formula, ensuring your academic percentage remains unpenalized.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FooterCredit()
                }
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Backfill Dialog for Selected Date
        if (showBackfillDialog) {
            com.example.ui.components.BackfillClassDialog(
                date = selectedDate,
                subjects = subjects,
                teachers = teachers,
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
    }
}

@Composable
fun QuickStatusPill(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (isSelected) activeColor else activeColor.copy(alpha = 0.14f), tween(250), label = "pill_bg")
    val contentCol by animateColorAsState(if (isSelected) Color.White else activeColor, tween(250), label = "pill_content")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .bounceClick(pressedScale = 0.88f, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = contentCol, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = contentCol)
        }
    }
}
