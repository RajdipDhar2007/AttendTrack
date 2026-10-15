package com.example.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.BunkCalculatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExamSchedulerScreen
import com.example.ui.screens.MonthlyAttendanceScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyPlannerScreen
import com.example.ui.screens.SubjectAttendanceScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.viewmodel.AttendTrackViewModel

@Composable
fun AttendTrackAppNav(
    viewModel: AttendTrackViewModel
) {
    val profile by viewModel.profile.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    // If onboarding not completed, start with Onboarding
    val isReady = profile != null
    val hasCompleted = profile?.hasCompletedOnboarding ?: false

    if (isReady && !hasCompleted) {
        OnboardingScreen(
            viewModel = viewModel,
            onComplete = {
                currentScreen = Screen.Dashboard
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        bottomBar = {
            BouncyBottomNav(
                currentScreen = currentScreen,
                onSelectScreen = { currentScreen = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Minimal Top Bar for auxiliary destinations (Monthly, Exams, Settings)
            TopBarAuxiliary(
                currentScreen = currentScreen,
                onNavigate = { currentScreen = it }
            )

            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = 500f)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = 500f))
                },
                label = "screen_transition"
            ) { target ->
                when (target) {
                    Screen.Dashboard -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTimetable = { currentScreen = Screen.Timetable },
                        onNavigateToSubjectWise = { currentScreen = Screen.SubjectWise },
                        onNavigateToBunkCalc = { currentScreen = Screen.BunkCalc },
                        onNavigateToExams = { currentScreen = Screen.Exams },
                        onNavigateToStudyPlanner = { currentScreen = Screen.StudyPlanner }
                    )
                    Screen.Timetable -> TimetableScreen(viewModel = viewModel)
                    Screen.SubjectWise -> SubjectAttendanceScreen(viewModel = viewModel)
                    Screen.BunkCalc -> BunkCalculatorScreen(viewModel = viewModel)
                    Screen.AiAssistant -> AiAssistantScreen(viewModel = viewModel)
                    Screen.Monthly -> MonthlyAttendanceScreen(viewModel = viewModel)
                    Screen.Exams -> ExamSchedulerScreen(viewModel = viewModel)
                    Screen.Settings -> SettingsScreen(viewModel = viewModel)
                    Screen.StudyPlanner -> StudyPlannerScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.Dashboard }
                    )
                    else -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTimetable = { currentScreen = Screen.Timetable },
                        onNavigateToSubjectWise = { currentScreen = Screen.SubjectWise },
                        onNavigateToBunkCalc = { currentScreen = Screen.BunkCalc },
                        onNavigateToExams = { currentScreen = Screen.Exams },
                        onNavigateToStudyPlanner = { currentScreen = Screen.StudyPlanner }
                    )
                }
            }
        }
    }
}

@Composable
fun TopBarAuxiliary(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "AttendTrack",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigate(Screen.Dashboard) }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { onNavigate(Screen.StudyPlanner) }) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Study Planner & Goals",
                    tint = if (currentScreen == Screen.StudyPlanner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = { onNavigate(Screen.BunkCalc) }) {
                Icon(
                    imageVector = Screen.BunkCalc.icon ?: Icons.Default.CalendarMonth,
                    contentDescription = "Bunk Calculator",
                    tint = if (currentScreen == Screen.BunkCalc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = { onNavigate(Screen.Monthly) }) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Monthly Analytics",
                    tint = if (currentScreen == Screen.Monthly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = { onNavigate(Screen.Settings) }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (currentScreen == Screen.Settings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun BouncyBottomNav(
    currentScreen: Screen,
    onSelectScreen: (Screen) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItems.forEach { item ->
                val isSelected = currentScreen == item
                val activeColor = MaterialTheme.colorScheme.primary

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectScreen(item)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("nav_item_${item.route}")
                ) {
                    item.icon?.let { icon ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = item.title,
                                tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
