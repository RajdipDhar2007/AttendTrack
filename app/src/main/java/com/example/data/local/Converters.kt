package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassType
import com.example.data.model.ExamType

class Converters {
    @TypeConverter
    fun fromClassType(value: ClassType): String = value.name

    @TypeConverter
    fun toClassType(value: String): ClassType = try {
        ClassType.valueOf(value)
    } catch (_: Exception) {
        ClassType.THEORY
    }

    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus): String = value.name

    @TypeConverter
    fun toAttendanceStatus(value: String): AttendanceStatus = try {
        AttendanceStatus.valueOf(value)
    } catch (_: Exception) {
        AttendanceStatus.PRESENT
    }

    @TypeConverter
    fun fromExamType(value: ExamType): String = value.name

    @TypeConverter
    fun toExamType(value: String): ExamType = try {
        ExamType.valueOf(value)
    } catch (_: Exception) {
        ExamType.MID_TERM
    }
}
