package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.DataObject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import org.json.JSONObject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.receiver.AlarmScheduler
import com.example.receiver.NotificationPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppPalette
import com.example.data.model.Profile
import com.example.data.model.ThemeMode
import com.example.data.remote.CloudBackupResult
import com.example.ui.components.FooterCredit
import com.example.ui.components.GlassCard
import com.example.ui.theme.AttendDanger
import com.example.ui.theme.AttendSuccess
import com.example.ui.theme.AttendThemeState
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
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: AttendTrackViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDarkTheme.current
    val palette = LocalAppPalette.current
    val backgroundBrush = when (palette) {
        AppPalette.AURORA -> getAuroraGradient(isDark)
        AppPalette.SUNSET -> getSunsetGradient(isDark)
        AppPalette.OCEAN -> getOceanGradient(isDark)
        AppPalette.NEON -> getNeonGradient(isDark)
        AppPalette.FOREST -> getForestGradient(isDark)
        AppPalette.MONO -> getMonoGradient(isDark)
    }

    val profile by viewModel.profile.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showResetSemesterDialog by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }

    // Cloud Backup & Restore Dialog states
    var isBackingUpToCloud by remember { mutableStateOf(false) }
    var cloudBackupResult by remember { mutableStateOf<CloudBackupResult?>(null) }
    var showCloudRestoreDialog by remember { mutableStateOf(false) }
    var cloudRestoreInput by remember { mutableStateOf("") }
    var cleanReplaceRestore by remember { mutableStateOf(true) }
    var isRestoringFromCloud by remember { mutableStateOf(false) }
    var cloudRestoreFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Direct JSON Export / Import Dialog states
    var showJsonExportDialog by remember { mutableStateOf<String?>(null) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var jsonImportText by remember { mutableStateOf("") }
    var cleanReplaceJsonImport by remember { mutableStateOf(true) }
    var jsonImportFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it)
                val text = stream?.bufferedReader()?.use { reader -> reader.readText() }
                if (!text.isNullOrBlank()) {
                    jsonImportText = text
                    jsonImportFeedback = null
                }
            } catch (e: Exception) {
                jsonImportFeedback = Pair(false, "Could not open file: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    val isOnline = viewModel.isCloudConnected()
    val latestBackupInfo = remember(cloudBackupResult) { viewModel.getLatestCloudBackupInfo() }

    var classRemindersEnabled by remember {
        mutableStateOf(NotificationPreferences.isClassRemindersEnabled(context))
    }
    var endOfDayEnabled by remember {
        mutableStateOf(NotificationPreferences.isEndOfDayReminderEnabled(context))
    }

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
                    text = "App Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Personalization, themes, cloud sync & backup",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Student Profile Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Student Identity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { showEditProfileDialog = true }) {
                                Text("Edit")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = profile?.name?.ifBlank { "Student" } ?: "Student",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${profile?.institution} • ${profile?.department}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${profile?.year}, ${profile?.semester} | Sec: ${profile?.section} (${profile?.group})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Theme & Aesthetic Customizer (6 Palettes)
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Theme Palettes (6 Styles)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Palette choices
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AppPalette.values()) { pal ->
                                val isSelected = AttendThemeState.palette == pal
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                        .clickable { AttendThemeState.palette = pal }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = pal.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dark / Light / AMOLED mode selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Theme Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.AMOLED).forEach { m ->
                                    val isSelected = AttendThemeState.mode == m
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                                            .clickable { AttendThemeState.mode = m }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = m.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Super Animative Background Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Animative Background FX",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Fluid chromatic nebula, stardust constellations & tap ripple physics",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            Switch(
                                checked = AttendThemeState.animationsEnabled,
                                onCheckedChange = { AttendThemeState.animationsEnabled = it }
                            )
                        }
                    }
                }
            }

            // Attendance Reminders & Local Push Notifications
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Attendance Reminders & Alerts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Automated local push notifications on device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggle 1: Class Alerts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Upcoming Class Alerts",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Receive notifications 5 minutes before scheduled classes with quick Present / Absent / Cancelled actions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = classRemindersEnabled,
                                onCheckedChange = { enabled ->
                                    classRemindersEnabled = enabled
                                    NotificationPreferences.setClassRemindersEnabled(context, enabled)
                                    if (enabled) {
                                        AlarmScheduler.scheduleAllUpcomingClassAlarms(context)
                                        Toast.makeText(context, "Class reminders enabled", Toast.LENGTH_SHORT).show()
                                    } else {
                                        AlarmScheduler.cancelAllClassAlarms(context)
                                        Toast.makeText(context, "Class reminders disabled", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Toggle 2: End of Day Reminder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "End-of-Day Attendance Reminder",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Daily evening alert (8:00 PM) to check for unmarked classes before the day ends",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = endOfDayEnabled,
                                onCheckedChange = { enabled ->
                                    endOfDayEnabled = enabled
                                    NotificationPreferences.setEndOfDayReminderEnabled(context, enabled)
                                    if (enabled) {
                                        AlarmScheduler.scheduleEndOfDayReminder(context)
                                        Toast.makeText(context, "End-of-day reminder enabled", Toast.LENGTH_SHORT).show()
                                    } else {
                                        AlarmScheduler.cancelEndOfDayReminder(context)
                                        Toast.makeText(context, "End-of-day reminder disabled", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Test Notification Buttons
                        Text(
                            text = "Test Local Notifications",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    AlarmScheduler.triggerTestClassNotification(context)
                                    Toast.makeText(context, "Sent upcoming class alert with actions", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Class Alert", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    AlarmScheduler.triggerTestEndOfDayNotification(context)
                                    Toast.makeText(context, "Sent end-of-day review reminder", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Day-End", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Cloud-Based Backup & Restore (JSON Export / Import)
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Cloud Backup & Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Complete JSON snapshot of all app data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                                }
                            }

                            // Cloud connectivity pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isOnline) AttendSuccess.copy(alpha = 0.15f) else AttendWarning.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isOnline) AttendSuccess else AttendWarning)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isOnline) "Cloud Ready" else "Vault Cache",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOnline) AttendSuccess else AttendWarning
                                    )
                                }
                            }
                        }

                        // Last backup info banner
                        latestBackupInfo?.let { info ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Latest Cloud Backup: ${info.backupCode}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${info.formattedDate} • ${info.subjectCount} subjects, ${info.attendanceRecordCount} records",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(info.backupCode))
                                            Toast.makeText(context, "Copied backup code: ${info.backupCode}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Primary Cloud Action Buttons: Cloud Backup & Cloud Restore
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Cloud Backup Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable(enabled = !isBackingUpToCloud) {
                                        scope.launch {
                                            isBackingUpToCloud = true
                                            val res = viewModel.backupToCloud()
                                            isBackingUpToCloud = false
                                            cloudBackupResult = res
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isBackingUpToCloud) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBackingUpToCloud) "Uploading..." else "Cloud Backup",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Cloud Restore Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                    .clickable {
                                        cloudRestoreFeedback = null
                                        showCloudRestoreDialog = true
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cloud Restore", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary Action Buttons: Direct Export JSON & Import JSON
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                    .clickable {
                                        scope.launch {
                                            val json = viewModel.exportJson()
                                            showJsonExportDialog = json
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export JSON", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                    .clickable {
                                        jsonImportFeedback = null
                                        jsonImportText = ""
                                        showJsonImportDialog = true
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import JSON", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Danger Zone: Reset Operations
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AttendDanger)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Academic Reset Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AttendDanger)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResetSemesterDialog = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Reset Semester Attendance", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Clears attendance marks while keeping timetable and subjects", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResetAllDialog = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Full Data Wipe", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AttendDanger)
                                Text("Resets timetable, exams, subjects, and student profile", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AttendDanger)
                        }
                    }
                }
            }

            // Branded About Card & Footer
            item {
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.attendtrack_icon_1791670151551),
                                contentDescription = "AttendTrack Logo",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "AttendTrack",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "A Rajdip Dhar project",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "v2.0 • Powered by Gemini AI & Google Search",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FooterCredit()
                }
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // ========================== DIALOGS ==========================

        // 1. Cloud Backup Result Modal
        cloudBackupResult?.let { res ->
            AlertDialog(
                onDismissRequest = { cloudBackupResult = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = AttendSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cloud Backup Created!", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Your complete academic database, custom subject target percentages, timetable, and preferences are safely packaged in JSON.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Highlighted Backup Code Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "YOUR CLOUD RECOVERY CODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = res.backupCode ?: "AT-BACKUP",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Use this code to restore on any device anytime",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        res.errorMessage?.let { note ->
                            Text(text = note, style = MaterialTheme.typography.bodySmall, color = AttendWarning)
                        }

                        // Summary badges
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Subjects", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("Included", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Attendance", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("Included", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Preferences", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("Saved", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            res.backupCode?.let {
                                clipboardManager.setText(AnnotatedString(it))
                                Toast.makeText(context, "Copied Cloud Code: $it", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Code")
                        }

                        TextButton(onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_SUBJECT, "AttendTrack Cloud Backup (${res.backupCode})")
                                putExtra(Intent.EXTRA_TEXT, res.jsonContent)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share or Save Backup JSON"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share / Drive")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cloudBackupResult = null }) {
                        Text("Done")
                    }
                }
            )
        }

        // 2. Cloud Restore Modal
        if (showCloudRestoreDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isRestoringFromCloud) {
                        showCloudRestoreDialog = false
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore from Cloud", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Enter your Cloud Backup Recovery Code (e.g., AT-XXXXXX), a cloud JSON URL, or paste the raw JSON bundle:",
                            style = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = cloudRestoreInput,
                            onValueChange = { cloudRestoreInput = it },
                            label = { Text("Backup Code / Cloud URL / JSON") },
                            placeholder = { Text("e.g. AT-XXXXXX") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clip = clipboardManager.getText()?.text ?: ""
                                    if (clip.isNotBlank()) cloudRestoreInput = clip
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                                }
                            }
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = cleanReplaceRestore,
                                onCheckedChange = { cleanReplaceRestore = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clean Replace (Recommended to ensure accurate foreign keys & slots)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Feedback message (error or success)
                        cloudRestoreFeedback?.let { (success, msg) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (success) AttendSuccess.copy(alpha = 0.15f) else AttendDanger.copy(alpha = 0.15f))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (success) AttendSuccess else AttendDanger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (success) AttendSuccess else AttendDanger
                                    )
                                }
                            }
                        }

                        if (isRestoringFromCloud) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Connecting to Cloud & Restoring...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = cloudRestoreInput.isNotBlank() && !isRestoringFromCloud,
                        onClick = {
                            scope.launch {
                                isRestoringFromCloud = true
                                val res = viewModel.restoreFromCloud(cloudRestoreInput, cleanReplaceRestore)
                                isRestoringFromCloud = false
                                cloudRestoreFeedback = Pair(res.success, res.message)
                                if (res.success) {
                                    Toast.makeText(context, "Restore complete!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("Restore Now", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCloudRestoreDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 3. Direct JSON Export Modal
        showJsonExportDialog?.let { json ->
            AlertDialog(
                onDismissRequest = { showJsonExportDialog = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exported JSON Bundle", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Snapshot contains profile, subjects, custom targets, rooms, attendance records, exams, and aesthetic preferences:",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = json,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy JSON")
                        }

                        TextButton(onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_SUBJECT, "AttendTrack Full Backup JSON")
                                putExtra(Intent.EXTRA_TEXT, json)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Save or Send JSON"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJsonExportDialog = null }) {
                        Text("Close")
                    }
                }
            )
        }

        // 4. Direct JSON Import Modal
        if (showJsonImportDialog) {
            AlertDialog(
                onDismissRequest = { showJsonImportDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import JSON File / Text", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Import raw JSON backup or student schedule directly from a file or paste it below:",
                            style = MaterialTheme.typography.bodySmall
                        )

                        // Action buttons: File picker & Template loader
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { jsonFilePickerLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Choose .json File", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = {
                                    jsonImportText = com.example.data.model.StudentScheduleTemplates.RAJDIP_DHAR_JSON
                                    jsonImportFeedback = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DataObject, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Load Sample", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Live detected student summary
                        val detectedStudent = remember(jsonImportText) {
                            try {
                                if (jsonImportText.isNotBlank()) {
                                    val root = JSONObject(jsonImportText)
                                    val st = root.optJSONObject("student")
                                    val name = st?.optString("name", "") ?: ""
                                    val inst = st?.optString("institute", "") ?: st?.optString("institution", "") ?: ""
                                    val dept = st?.optString("department", "") ?: ""
                                    val sec = st?.optString("section", "") ?: ""
                                    val grp = st?.optString("group", "") ?: ""
                                    val sched = root.optJSONObject("schedule")
                                    val dayCount = sched?.length() ?: 0
                                    if (name.isNotBlank()) "Detected: $name • $dept (Sec $sec, $grp)\n$inst ($dayCount days schedule)" else null
                                } else null
                            } catch (_: Exception) { null }
                        }

                        detectedStudent?.let { summary ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = jsonImportText,
                            onValueChange = {
                                jsonImportText = it
                                jsonImportFeedback = null
                            },
                            label = { Text("Paste JSON Here") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clip = clipboardManager.getText()?.text ?: ""
                                    if (clip.isNotBlank()) jsonImportText = clip
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste from clipboard")
                                }
                            }
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = cleanReplaceJsonImport,
                                onCheckedChange = { cleanReplaceJsonImport = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clean Replace (Overwrites existing subjects & records)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        jsonImportFeedback?.let { (success, msg) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (success) AttendSuccess.copy(alpha = 0.15f) else AttendDanger.copy(alpha = 0.15f))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (success) AttendSuccess else AttendDanger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (success) AttendSuccess else AttendDanger
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = jsonImportText.isNotBlank(),
                        onClick = {
                            scope.launch {
                                val res = viewModel.importJson(jsonImportText, cleanReplaceJsonImport)
                                jsonImportFeedback = Pair(res.success, res.message)
                                if (res.success) {
                                    Toast.makeText(context, "JSON imported successfully!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("Validate & Restore", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJsonImportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 5. Edit Profile Dialog (with Strict 4-Year Curriculum Dropdowns)
        if (showEditProfileDialog) {
            val curr = profile ?: Profile()
            var editName by remember { mutableStateOf(curr.name) }
            var editInst by remember { mutableStateOf(curr.institution) }
            var editDept by remember { mutableStateOf(curr.department) }
            var editSec by remember { mutableStateOf(curr.section) }
            var editYear by remember { mutableStateOf(curr.year.ifBlank { "2nd Year" }) }
            var editSem by remember { mutableStateOf(curr.semester.ifBlank { "Semester 4" }) }
            var editTarget by remember { mutableFloatStateOf(curr.targetPercentage) }

            var yearExpanded by remember { mutableStateOf(false) }
            var semExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                title = { Text("Edit Student Profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, singleLine = true)
                        OutlinedTextField(value = editInst, onValueChange = { editInst = it }, label = { Text("Institution") }, singleLine = true)
                        OutlinedTextField(value = editDept, onValueChange = { editDept = it }, label = { Text("Department") }, singleLine = true)

                        // Strict 4-Year Curriculum Pickers
                        Text("Academic Curriculum (Strict 4-Year Mapping)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Year
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = editYear,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Year") },
                                    trailingIcon = {
                                        IconButton(onClick = { yearExpanded = !yearExpanded }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { yearExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = yearExpanded,
                                    onDismissRequest = { yearExpanded = false }
                                ) {
                                    com.example.data.model.AcademicCurriculum.YEARS.forEach { yr ->
                                        DropdownMenuItem(
                                            text = { Text(yr) },
                                            onClick = {
                                                editYear = yr
                                                val valid = com.example.data.model.AcademicCurriculum.getValidSemestersForYear(yr)
                                                if (editSem !in valid) editSem = valid.first()
                                                yearExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Semester
                            val validSemesters = com.example.data.model.AcademicCurriculum.getValidSemestersForYear(editYear)
                            Box(modifier = Modifier.weight(1.1f)) {
                                OutlinedTextField(
                                    value = editSem,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Semester") },
                                    trailingIcon = {
                                        IconButton(onClick = { semExpanded = !semExpanded }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { semExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = semExpanded,
                                    onDismissRequest = { semExpanded = false }
                                ) {
                                    validSemesters.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s) },
                                            onClick = {
                                                editSem = s
                                                semExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(value = editSec, onValueChange = { editSec = it }, label = { Text("Section / Group") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                        Text("Target: ${editTarget.toInt()}%", fontWeight = FontWeight.Bold)
                        Slider(value = editTarget, onValueChange = { editTarget = it }, valueRange = 50f..95f, steps = 8)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateProfile(
                            curr.copy(
                                name = editName,
                                institution = editInst,
                                department = editDept,
                                year = editYear,
                                semester = editSem,
                                section = editSec,
                                targetPercentage = editTarget
                            )
                        )
                        showEditProfileDialog = false
                    }) {
                        Text("Save Profile")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 6. Reset Semester Dialog
        if (showResetSemesterDialog) {
            AlertDialog(
                onDismissRequest = { showResetSemesterDialog = false },
                title = { Text("Reset Semester Attendance?") },
                text = { Text("This will clear all past attendance records for the current semester. Your timetable and subjects will be kept intact.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetSemester()
                        showResetSemesterDialog = false
                        Toast.makeText(context, "Semester attendance reset", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Reset", color = AttendDanger)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetSemesterDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 7. Reset All Dialog
        if (showResetAllDialog) {
            AlertDialog(
                onDismissRequest = { showResetAllDialog = false },
                title = { Text("Full Data Wipe?") },
                text = { Text("Are you sure? This will permanently delete all attendance marks, timetable schedules, exams, and subjects.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetAllData()
                        showResetAllDialog = false
                        Toast.makeText(context, "All data reset", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Wipe Everything", color = AttendDanger)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetAllDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
