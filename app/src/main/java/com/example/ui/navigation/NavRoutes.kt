package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Onboarding : Screen("onboarding", "Welcome")
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Timetable : Screen("timetable", "Timetable", Icons.Default.ViewTimeline)
    object SubjectWise : Screen("subject_wise", "Subjects", Icons.Default.School)
    object Monthly : Screen("monthly", "Monthly", Icons.Default.CalendarMonth)
    object MarkCalendar : Screen("mark_calendar", "Mark", Icons.Default.CheckCircle)
    object BunkCalc : Screen("bunk_calculator", "Bunk Calc", Icons.Default.Calculate)
    object Exams : Screen("exams", "Countdown", Icons.Default.EventNote)
    object AiAssistant : Screen("ai_assistant", "AI Copilot", Icons.Default.Psychology)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object StudyPlanner : Screen("study_planner", "Study Planner", Icons.Default.MenuBook)
}

val BottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Timetable,
    Screen.SubjectWise,
    Screen.Exams,
    Screen.AiAssistant
)
