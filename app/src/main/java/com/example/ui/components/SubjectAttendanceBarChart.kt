package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubjectAttendanceStat
import com.example.ui.theme.AttendDanger
import com.example.ui.theme.AttendSuccess
import com.example.ui.theme.AttendWarning

enum class ChartDisplayMode {
    BAR_CHART,
    LIST_VIEW
}

@Composable
fun SubjectAttendanceBarChart(
    stats: List<SubjectAttendanceStat>,
    defaultTargetPercentage: Float = 75f,
    onNavigateToSubjectWise: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) {
        GlassCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No Subject Attendance Recorded Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Attendance per subject will be visualized here as a bar chart.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var displayMode by remember { mutableStateOf(ChartDisplayMode.BAR_CHART) }
    var selectedStat by remember { mutableStateOf<SubjectAttendanceStat?>(null) }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            // Header: Title, Target Badge, and Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Subject Attendance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Goal: ${defaultTargetPercentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = "Bar chart analytics with live status threshold",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }

                // Toggle Bar Chart / List View
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (displayMode == ChartDisplayMode.BAR_CHART) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { displayMode = ChartDisplayMode.BAR_CHART }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Bar Chart View",
                            tint = if (displayMode == ChartDisplayMode.BAR_CHART) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (displayMode == ChartDisplayMode.LIST_VIEW) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { displayMode = ChartDisplayMode.LIST_VIEW }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = "List View",
                            tint = if (displayMode == ChartDisplayMode.LIST_VIEW) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Legend (Recharts style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendChip(color = AttendSuccess, label = "≥ ${defaultTargetPercentage.toInt()}% On Track")
                LegendChip(color = AttendWarning, label = "Near Goal")
                LegendChip(color = AttendDanger, label = "< Target")
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (displayMode) {
                ChartDisplayMode.BAR_CHART -> {
                    // Recharts styled interactive SVG-like bar chart
                    RechartsBarChartCanvas(
                        stats = stats,
                        targetPercentage = defaultTargetPercentage,
                        selectedStat = selectedStat,
                        onSelectStat = { selectedStat = if (selectedStat == it) null else it }
                    )
                }
                ChartDisplayMode.LIST_VIEW -> {
                    // Detailed horizontal ranking view
                    HorizontalRankingList(
                        stats = stats,
                        targetPercentage = defaultTargetPercentage,
                        onSelectStat = { selectedStat = if (selectedStat == it) null else it }
                    )
                }
            }

            // Interactive Tooltip / Inspection Card (Recharts style inspection tooltip)
            AnimatedVisibility(
                visible = selectedStat != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedStat?.let { item ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                            .testTag("chart_tooltip_card")
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val codePrefix = if (item.subject.code.isNotBlank()) "${item.subject.code} • " else ""
                                    Text(
                                        text = "$codePrefix${item.subject.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.presentCount} attended / ${item.effectiveCount} conducted classes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val isGood = item.percentage >= item.targetPercentage
                                    val statusColor = if (isGood) AttendSuccess else if (item.percentage >= item.targetPercentage - 10f) AttendWarning else AttendDanger
                                    Text(
                                        text = String.format("%.1f%%", item.percentage),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = statusColor
                                    )
                                    Text(
                                        text = if (isGood) "${item.safeBunks} safe bunks" else "${item.neededToTarget} needed",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = onNavigateToSubjectWise,
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "View in Subjects & Mark Attendance →",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun RechartsBarChartCanvas(
    stats: List<SubjectAttendanceStat>,
    targetPercentage: Float,
    selectedStat: SubjectAttendanceStat?,
    onSelectStat: (SubjectAttendanceStat) -> Unit
) {
    val chartHeight = 160.dp
    val scrollState = rememberScrollState()

    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val targetLineColor = AttendWarning.copy(alpha = 0.6f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight + 40.dp)
        ) {
            // Y-Axis labels (100%, 75%, 50%, 0%)
            Column(
                modifier = Modifier
                    .height(chartHeight)
                    .width(36.dp)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(text = "100%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(text = "75%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AttendWarning.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                Text(text = "50%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(text = "25%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(text = "0%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }

            // Main Bar Canvas with horizontal scroll if subjects > 5
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .drawBehind {
                        val canvasH = chartHeight.toPx()
                        val canvasW = size.width

                        // Draw Grid Lines (0%, 25%, 50%, 75%, 100%)
                        val levels = listOf(0.0f, 0.25f, 0.50f, 0.75f, 1.0f)
                        for (lvl in levels) {
                            val y = canvasH * (1.0f - lvl)
                            drawLine(
                                color = gridLineColor,
                                start = Offset(0f, y),
                                end = Offset(canvasW, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Recharts Target Reference Line (Dashed)
                        val targetRatio = (targetPercentage / 100f).coerceIn(0f, 1f)
                        val targetY = canvasH * (1.0f - targetRatio)
                        drawLine(
                            color = targetLineColor,
                            start = Offset(0f, targetY),
                            end = Offset(canvasW, targetY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Spacer(modifier = Modifier.width(4.dp))

                    stats.forEach { stat ->
                        val isSelected = selectedStat?.subject?.id == stat.subject.id
                        val pct = stat.percentage.coerceIn(0f, 100f)

                        val animatedHeightFraction by animateFloatAsState(
                            targetValue = pct / 100f,
                            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                            label = "bar_height"
                        )

                        val isAbove = pct >= targetPercentage
                        val isNear = pct >= targetPercentage - 10f
                        val barColor = if (isAbove) AttendSuccess else if (isNear) AttendWarning else AttendDanger

                        Column(
                            modifier = Modifier
                                .widthIn(min = 44.dp, max = 56.dp)
                                .fillMaxHeight()
                                .clickable { onSelectStat(stat) }
                                .testTag("bar_item_${stat.subject.id}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Top percentage badge
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) barColor else Color.Transparent)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = String.format("%.0f%%", pct),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else barColor
                                )
                            }

                            // The Bar column (Rounded top, like Recharts radius={[6, 6, 0, 0]})
                            Box(
                                modifier = Modifier
                                    .height(chartHeight)
                                    .width(28.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // Background track
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                )

                                // Active Fill Bar
                                val fillHeight = (chartHeight * animatedHeightFraction).coerceAtLeast(4.dp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(fillHeight)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    barColor,
                                                    barColor.copy(alpha = 0.7f)
                                                )
                                            )
                                        )
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(2.dp, Color.White, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            } else Modifier
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // X-Axis Subject Code Label
                            val label = if (stat.subject.code.isNotBlank()) stat.subject.code else stat.subject.name.take(4)
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
private fun HorizontalRankingList(
    stats: List<SubjectAttendanceStat>,
    targetPercentage: Float,
    onSelectStat: (SubjectAttendanceStat) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stats.forEach { stat ->
            val pct = stat.percentage.coerceIn(0f, 100f)
            val isGood = pct >= targetPercentage
            val isNear = pct >= targetPercentage - 10f
            val statusColor = if (isGood) AttendSuccess else if (isNear) AttendWarning else AttendDanger

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { onSelectStat(stat) }
                    .padding(12.dp)
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
                        if (stat.subject.code.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stat.subject.code,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = stat.subject.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${stat.presentCount}/${stat.effectiveCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = String.format("%.1f%%", pct),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar with threshold marker
                LinearProgressIndicator(
                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
