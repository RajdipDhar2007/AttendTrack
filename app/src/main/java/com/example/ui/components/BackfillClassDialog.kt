package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassType
import com.example.data.model.Subject
import com.example.data.model.Teacher
import com.example.ui.theme.AttendDanger
import com.example.ui.theme.AttendSuccess
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackfillClassDialog(
    date: LocalDate,
    subjects: List<Subject>,
    teachers: List<Teacher>,
    onDismiss: () -> Unit,
    onSave: (subjectId: Long, teacherId: Long?, classType: ClassType, status: AttendanceStatus) -> Unit
) {
    val uniqueSubjects = remember(subjects) {
        com.example.data.repository.AttendanceRepository.deduplicateSubjectsInMemory(subjects)
    }
    var selectedSubjectId by remember(uniqueSubjects) {
        mutableStateOf(uniqueSubjects.firstOrNull()?.id ?: 0L)
    }
    var selectedTeacherId by remember { mutableStateOf<Long?>(null) }
    var selectedClassType by remember { mutableStateOf(ClassType.THEORY) }
    var selectedStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }

    var subMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Backfill Attendance for ${date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Subject Dropdown
                ExposedDropdownMenuBox(
                    expanded = subMenuExpanded,
                    onExpandedChange = { subMenuExpanded = it }
                ) {
                    val selSub = uniqueSubjects.firstOrNull { it.id == selectedSubjectId } ?: uniqueSubjects.firstOrNull()
                    val selSubTitle = if (selSub != null) {
                        if (selSub.code.isNotBlank()) "${selSub.code} • ${selSub.name}" else selSub.name
                    } else "Select Subject"

                    OutlinedTextField(
                        value = selSubTitle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subMenuExpanded,
                        onDismissRequest = { subMenuExpanded = false }
                    ) {
                        uniqueSubjects.forEach { s ->
                            val sTitle = if (s.code.isNotBlank()) "${s.code} • ${s.name}" else s.name
                            DropdownMenuItem(
                                text = { Text(sTitle) },
                                onClick = {
                                    selectedSubjectId = s.id
                                    subMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Class Type Selector (Theory / Lab)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedClassType == ClassType.THEORY) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                            .clickable { selectedClassType = ClassType.THEORY }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "THEORY",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedClassType == ClassType.THEORY) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedClassType == ClassType.LAB) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                            .clickable { selectedClassType = ClassType.LAB }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "PRACTICAL LAB",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedClassType == ClassType.LAB) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Attendance Status Selector (Attendance, Missed, Cancelled)
                Text("Select Status:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusOptions = listOf(
                        Triple(AttendanceStatus.PRESENT, "Attendance", AttendSuccess to Icons.Default.Check),
                        Triple(AttendanceStatus.ABSENT, "Missed", AttendDanger to Icons.Default.Close),
                        Triple(AttendanceStatus.CANCELLED, "Cancelled", Color.Gray to Icons.Default.Cancel)
                    )

                    statusOptions.forEach { (status, label, styling) ->
                        val (col, icon) = styling
                        val isSelected = selectedStatus == status
                        val bg = if (isSelected) col else col.copy(alpha = 0.14f)
                        val textCol = if (isSelected) Color.White else col

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { selectedStatus = status }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = icon, contentDescription = label, tint = textCol, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = textCol)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (selectedSubjectId != 0L) {
                    onSave(selectedSubjectId, selectedTeacherId, selectedClassType, selectedStatus)
                }
            }) {
                Text("Save Attendance", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
