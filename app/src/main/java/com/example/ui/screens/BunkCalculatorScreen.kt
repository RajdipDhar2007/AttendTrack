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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FooterCredit
import com.example.ui.components.GlassCard
import com.example.ui.components.AnimatedCountText
import com.example.ui.components.bounceClick
import com.example.ui.components.staggeredEntrance
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.example.ui.theme.AttendDanger
import com.example.ui.theme.AttendSuccess
import com.example.ui.theme.AttendWarning
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.getAuroraGradient
import com.example.ui.theme.getForestGradient
import com.example.ui.theme.getMonoGradient
import com.example.ui.theme.getNeonGradient
import com.example.ui.theme.getOceanGradient
import com.example.ui.theme.getSunsetGradient
import com.example.ui.viewmodel.AttendTrackViewModel

@Composable
fun BunkCalculatorScreen(
    viewModel: AttendTrackViewModel
) {
    val isDark = LocalIsDarkTheme.current
    val palette = LocalAppPalette.current
    val backgroundBrush = when (palette) {
        com.example.data.model.AppPalette.AURORA -> getAuroraGradient(isDark)
        com.example.data.model.AppPalette.SUNSET -> getSunsetGradient(isDark)
        com.example.data.model.AppPalette.OCEAN -> getOceanGradient(isDark)
        com.example.data.model.AppPalette.NEON -> getNeonGradient(isDark)
        com.example.data.model.AppPalette.FOREST -> getForestGradient(isDark)
        com.example.data.model.AppPalette.MONO -> getMonoGradient(isDark)
    }

    val overallStat by viewModel.overallStat.collectAsState()
    val subjectStats by viewModel.subjectStats.collectAsState()
    val selectedSubjectId by viewModel.simSelectedSubjectId.collectAsState()
    val simAttend by viewModel.simUpcomingAttend.collectAsState()
    val simBunk by viewModel.simUpcomingBunk.collectAsState()
    val whatIfResult by viewModel.whatIfResult.collectAsState()

    val currentTarget = whatIfResult.targetPercentage

    com.example.ui.components.InteractiveAnimatedBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Bunk & Recovery Calculator",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mathematical prediction & what-if simulator",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Subject Selector Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        val isSelected = selectedSubjectId == null
                        val bg by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f), tween(250), label = "chip_bg")
                        val textCol = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .bounceClick { viewModel.setSimSubject(null) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("All Subjects (Overall)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = textCol)
                        }
                    }

                    items(
                        items = subjectStats,
                        key = { it.subject.id }
                    ) { stat ->
                        val isSelected = selectedSubjectId == stat.subject.id
                        val bg by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f), tween(250), label = "chip_bg")
                        val textCol = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        val chipText = if (stat.subject.code.isNotBlank()) "${stat.subject.code} • ${stat.subject.name}" else stat.subject.name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .bounceClick { viewModel.setSimSubject(if (isSelected) null else stat.subject.id) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(chipText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = textCol)
                        }
                    }
                }
            }

            // Status Verdict Card
            item {
                val stat = if (selectedSubjectId == null) null else subjectStats.firstOrNull { it.subject.id == selectedSubjectId }
                val safeBunks = stat?.safeBunks ?: overallStat.safeBunks
                val needed = stat?.neededToTarget ?: overallStat.classesNeeded
                val currentPct = stat?.percentage ?: overallStat.percentage
                val subjectTitle = stat?.let {
                    if (it.subject.code.isNotBlank()) "${it.subject.code} • ${it.subject.name}" else it.subject.name
                } ?: "Overall Academic Attendance"

                GlassCard(modifier = Modifier.fillMaxWidth().staggeredEntrance(1)) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subjectTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                stat?.let { s ->
                                    Text(
                                        text = "Attended: ${s.presentCount}  •  Missed: ${s.absentCount}  •  Cancelled: ${s.cancelledCount}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                } ?: run {
                                    Text(
                                        text = "Attended: ${overallStat.totalPresent}  •  Missed: ${overallStat.totalAbsent}  •  Cancelled: ${overallStat.totalCancelled}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                            }
                            Text(
                                text = "${String.format("%.1f%%", currentPct)} vs Goal ${currentTarget.toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (currentPct >= currentTarget) AttendSuccess else AttendDanger
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (safeBunks > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(AttendSuccess.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AttendSuccess)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "$safeBunks Safe Bunks Available!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AttendSuccess
                                    )
                                    Text(
                                        text = "You can skip $safeBunks upcoming class(es) and remain above ${currentTarget.toInt()}%.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(AttendDanger.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AttendDanger)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "$needed Consecutive Classes Needed",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AttendDanger
                                    )
                                    Text(
                                        text = "You cannot bunk any classes. Attend the next $needed class(es) to reach ${currentTarget.toInt()}%.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive What-If Simulator Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth().staggeredEntrance(2)) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "What-If Simulator",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Simulate how your attendance changes as you attend or miss upcoming lectures.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Slider 1: Upcoming Classes to Attend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Attend Upcoming Classes", style = MaterialTheme.typography.labelLarge)
                            Text(text = "+$simAttend classes", fontWeight = FontWeight.Bold, color = AttendSuccess)
                        }
                        Slider(
                            value = simAttend.toFloat(),
                            onValueChange = { viewModel.setSimAttend(it.toInt()) },
                            valueRange = 0f..25f,
                            steps = 24
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Slider 2: Upcoming Classes to Bunk
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Bunk / Skip Upcoming Classes", style = MaterialTheme.typography.labelLarge)
                            Text(text = "+$simBunk classes", fontWeight = FontWeight.Bold, color = AttendDanger)
                        }
                        Slider(
                            value = simBunk.toFloat(),
                            onValueChange = { viewModel.setSimBunk(it.toInt()) },
                            valueRange = 0f..25f,
                            steps = 24
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Projected Outcome Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (whatIfResult.isTargetMet) AttendSuccess.copy(alpha = 0.15f)
                                    else AttendDanger.copy(alpha = 0.15f)
                                )
                                .padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "PROJECTED ATTENDANCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = if (whatIfResult.isTargetMet) AttendSuccess else AttendDanger
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                AnimatedCountText(
                                    value = whatIfResult.projectedPercentage,
                                    decimals = 1,
                                    suffix = "%",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (whatIfResult.isTargetMet) AttendSuccess else AttendDanger
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val diffStr = if (whatIfResult.diffFromCurrent >= 0) "+${String.format("%.1f", whatIfResult.diffFromCurrent)}%" else "${String.format("%.1f", whatIfResult.diffFromCurrent)}%"
                                Text(
                                    text = "$diffStr change (${whatIfResult.projectedPresent} attended / ${whatIfResult.projectedTotal} total)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (whatIfResult.isTargetMet) "Target Met! You stay safely above ${currentTarget.toInt()}%."
                                    else "Target Missed! You will drop below ${currentTarget.toInt()}%.",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (whatIfResult.isTargetMet) AttendSuccess else AttendDanger
                                )
                            }
                        }
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
    }
}
