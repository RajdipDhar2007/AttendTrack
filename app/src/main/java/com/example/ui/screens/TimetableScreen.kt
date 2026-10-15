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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.DataObject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import org.json.JSONObject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.ClassType
import com.example.data.model.Subject
import com.example.data.model.Teacher
import com.example.data.model.TimetableSlot
import com.example.ui.components.FooterCredit
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.getAuroraGradient
import com.example.ui.theme.getForestGradient
import com.example.ui.theme.getMonoGradient
import com.example.ui.theme.getNeonGradient
import com.example.ui.theme.getOceanGradient
import com.example.ui.theme.getSunsetGradient
import com.example.ui.viewmodel.AttendTrackViewModel
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
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

    val selectedDay by viewModel.selectedTimetableDay.collectAsState()
    val allSlots by viewModel.allSlots.collectAsState()
    val allSubjects by viewModel.allSubjects.collectAsState()
    val allTeachers by viewModel.allTeachers.collectAsState()

    var showSlotDialog by remember { mutableStateOf(false) }
    var slotToEdit by remember { mutableStateOf<TimetableSlot?>(null) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var jsonInputText by remember { mutableStateOf("") }
    var cleanReplaceImport by remember { mutableStateOf(true) }
    var importStatusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it)
                val text = stream?.bufferedReader()?.use { reader -> reader.readText() }
                if (!text.isNullOrBlank()) {
                    jsonInputText = text
                    importStatusMessage = null
                }
            } catch (e: Exception) {
                importStatusMessage = Pair(false, "Failed to read JSON file: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    val daysOfWeek = listOf(
        Pair(1, "Mon"),
        Pair(2, "Tue"),
        Pair(3, "Wed"),
        Pair(4, "Thu"),
        Pair(5, "Fri"),
        Pair(6, "Sat"),
        Pair(7, "Sun")
    )

    val currentDaySlots = allSlots
        .filter { it.dayOfWeek == selectedDay }
        .sortedBy { it.startTime }

    com.example.ui.components.InteractiveAnimatedBackground {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Weekly Timetable",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage class schedule and lab sessions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                FilledTonalButton(
                    onClick = {
                        importStatusMessage = null
                        showJsonImportDialog = true
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = "Import JSON",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import JSON")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day Selector Tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(daysOfWeek) { (dayNum, label) ->
                    val isSelected = dayNum == selectedDay
                    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg)
                            .clickable { viewModel.setSelectedTimetableDay(dayNum) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slots List
            if (currentDaySlots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text(
                                text = "No classes on this day",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button below to add lectures or labs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentDaySlots) { slot ->
                        val sub = allSubjects.firstOrNull { it.id == slot.subjectId }
                        val teach = allTeachers.firstOrNull { it.id == slot.teacherId }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    // Time bar
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(76.dp)
                                    ) {
                                        Text(
                                            text = slot.getDisplayStartTime(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = slot.getDisplayEndTime(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (slot.classType == ClassType.LAB) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = slot.classType.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (slot.classType == ClassType.LAB) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            if (slot.room.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Room ${slot.room}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        val subTitle = if (sub != null) {
                                            if (sub.code.isNotBlank()) "${sub.code} • ${sub.name}" else sub.name
                                        } else "Unknown Subject"
                                        Text(
                                            text = subTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        teach?.let { t ->
                                            Text(
                                                text = "Faculty: ${t.name}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                            )
                                        }
                                        val taName = slot.technicalAssistantName
                                        if (taName.isNotBlank()) {
                                            Text(
                                                text = "TA: $taName",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(onClick = {
                                        slotToEdit = slot
                                        showSlotDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Slot",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteSlot(slot) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Slot",
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Add Slot Floating Action Button
        FloatingActionButton(
            onClick = {
                slotToEdit = null
                showSlotDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 20.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Timetable Slot", tint = Color.White)
        }

        // Slot Editor Dialog
        if (showSlotDialog) {
            TimetableSlotEditorDialog(
                initialSlot = slotToEdit,
                defaultDay = selectedDay,
                subjects = allSubjects,
                teachers = allTeachers,
                onDismiss = { showSlotDialog = false },
                onSave = { slot ->
                    if (slotToEdit != null) {
                        viewModel.updateSlot(slot)
                    } else {
                        viewModel.addSlot(slot)
                    }
                    showSlotDialog = false
                }
            )
        }

        // JSON Schedule Import Dialog
        if (showJsonImportDialog) {
            AlertDialog(
                onDismissRequest = { showJsonImportDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Timetable JSON")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Import your college schedule JSON directly from a file or paste it below. Automatically extracts subjects, teachers, labs, and student profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )

                        // File picker & template shortcuts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    jsonFilePickerLauncher.launch("*/*")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose .json File", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = {
                                    jsonInputText = com.example.data.model.StudentScheduleTemplates.RAJDIP_DHAR_JSON
                                    importStatusMessage = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DataObject,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Load Sample JSON", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Live detected student info
                        val detectedStudent = remember(jsonInputText) {
                            try {
                                if (jsonInputText.isNotBlank()) {
                                    val root = JSONObject(jsonInputText)
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
                            value = jsonInputText,
                            onValueChange = {
                                jsonInputText = it
                                importStatusMessage = null
                            },
                            placeholder = { Text("Paste or load timetable JSON here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Replace existing schedule",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = cleanReplaceImport,
                                onCheckedChange = { cleanReplaceImport = it }
                            )
                        }

                        importStatusMessage?.let { (success, msg) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (success) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                    )
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = jsonInputText.isNotBlank(),
                        onClick = {
                            scope.launch {
                                val res = viewModel.importJson(jsonInputText, cleanReplaceImport)
                                importStatusMessage = Pair(res.success, res.message)
                                if (res.success) {
                                    Toast.makeText(context, "Schedule imported successfully!", Toast.LENGTH_SHORT).show()
                                    jsonInputText = ""
                                    showJsonImportDialog = false
                                }
                            }
                        }
                    ) {
                        Text("Import Schedule", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJsonImportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableSlotEditorDialog(
    initialSlot: TimetableSlot?,
    defaultDay: Int,
    subjects: List<Subject>,
    teachers: List<Teacher>,
    onDismiss: () -> Unit,
    onSave: (TimetableSlot) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(initialSlot?.subjectId ?: subjects.firstOrNull()?.id ?: 0L) }
    var selectedTeacherId by remember { mutableStateOf(initialSlot?.teacherId ?: teachers.firstOrNull()?.id) }
    var taName by remember { mutableStateOf(initialSlot?.technicalAssistantName ?: "") }
    var classType by remember { mutableStateOf(initialSlot?.classType ?: ClassType.THEORY) }
    var room by remember { mutableStateOf(initialSlot?.room ?: "LH-101") }
    var startTime by remember { mutableStateOf(initialSlot?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(initialSlot?.endTime ?: "10:00") }
    var dayOfWeek by remember { mutableIntStateOf(initialSlot?.dayOfWeek ?: defaultDay) }

    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var teacherMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSlot != null) "Edit Class Slot" else "Add New Class Slot") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Class Type Theory vs Lab
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = classType == ClassType.THEORY,
                        onClick = { classType = ClassType.THEORY },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("THEORY")
                    }
                    SegmentedButton(
                        selected = classType == ClassType.LAB,
                        onClick = { classType = ClassType.LAB },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("LAB")
                    }
                }

                // Subject Picker
                ExposedDropdownMenuBox(
                    expanded = subjectMenuExpanded,
                    onExpandedChange = { subjectMenuExpanded = it }
                ) {
                    val selSub = subjects.firstOrNull { it.id == selectedSubjectId }
                    OutlinedTextField(
                        value = selSub?.name ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subjectMenuExpanded,
                        onDismissRequest = { subjectMenuExpanded = false }
                    ) {
                        subjects.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    selectedSubjectId = s.id
                                    subjectMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Teacher Picker
                ExposedDropdownMenuBox(
                    expanded = teacherMenuExpanded,
                    onExpandedChange = { teacherMenuExpanded = it }
                ) {
                    val selTeach = teachers.firstOrNull { it.id == selectedTeacherId }
                    OutlinedTextField(
                        value = selTeach?.name ?: "No Teacher Assigned",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Teacher / Faculty") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = teacherMenuExpanded,
                        onDismissRequest = { teacherMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedTeacherId = null
                                teacherMenuExpanded = false
                            }
                        )
                        teachers.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name) },
                                onClick = {
                                    selectedTeacherId = t.id
                                    teacherMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Lab Technical Assistant input if LAB
                if (classType == ClassType.LAB) {
                    OutlinedTextField(
                        value = taName,
                        onValueChange = { taName = it },
                        label = { Text("Technical Assistant / Lab TA") },
                        placeholder = { Text("e.g. Ms. Anita Roy (TA)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Room input
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Lab Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Time Pickers
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        TimetableSlot(
                            id = initialSlot?.id ?: 0L,
                            dayOfWeek = dayOfWeek,
                            subjectId = selectedSubjectId,
                            teacherId = selectedTeacherId,
                            technicalAssistantName = taName.trim(),
                            classType = classType,
                            room = room.trim(),
                            startTime = startTime.trim(),
                            endTime = endTime.trim()
                        )
                    )
                }
            ) {
                Text("Save Slot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
