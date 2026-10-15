package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.AttendanceRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AttendTrack", appName)
  }

  @Test
  fun testSafeBunkCalculation() {
    // 18 attended out of 20 total -> 90%. Target 75%.
    // Safe bunks should be 4
    val safeBunks = AttendanceRepository.calculateSafeBunks(18, 2, 75f)
    assertEquals(4, safeBunks)

    // 10 attended out of 20 total -> 50%. Target 75%.
    // Safe bunks should be 0, needed should be 20
    val bunksWhenLow = AttendanceRepository.calculateSafeBunks(10, 10, 75f)
    assertEquals(0, bunksWhenLow)
    val needed = AttendanceRepository.calculateClassesNeeded(10, 10, 75f)
    assertEquals(20, needed)
  }

  @Test
  fun testAcademicCurriculumMapping() {
    val y1Semesters = com.example.data.model.AcademicCurriculum.getValidSemestersForYear("1st Year")
    assertEquals(listOf("Semester 1", "Semester 2"), y1Semesters)

    val y2Semesters = com.example.data.model.AcademicCurriculum.getValidSemestersForYear("2nd Year")
    assertEquals(listOf("Semester 3", "Semester 4"), y2Semesters)

    val y3Semesters = com.example.data.model.AcademicCurriculum.getValidSemestersForYear("3rd Year")
    assertEquals(listOf("Semester 5", "Semester 6"), y3Semesters)

    val y4Semesters = com.example.data.model.AcademicCurriculum.getValidSemestersForYear("4th Year")
    assertEquals(listOf("Semester 7", "Semester 8"), y4Semesters)
  }

  @Test
  fun testRajdipDharDatabaseSeedingAndSnapshot() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AttendTrackDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = AttendanceRepository(db.dao())
    repo.seedRajdipDharSchedule(force = true)

    val profile = repo.getProfile()
    org.junit.Assert.assertNotNull(profile)
    assertEquals("Rajdip Dhar", profile?.name)
    assertEquals("Heritage Institute of Technology, Kolkata", profile?.institution)
    assertEquals("Computer Science Engineering", profile?.department)
    assertEquals("Gr. 1", profile?.group)

    val snapshot = repo.getLiveDatabaseSnapshot()
    org.junit.Assert.assertTrue(snapshot.contains("Rajdip Dhar"))
    org.junit.Assert.assertTrue(snapshot.contains("Heritage Institute of Technology, Kolkata"))
    org.junit.Assert.assertTrue(snapshot.contains("Introduction to Electronic Devices and Circuits"))
    org.junit.Assert.assertTrue(snapshot.contains("Dr. Tania Das"))
    org.junit.Assert.assertTrue(snapshot.contains("MTH1101"))

    db.close()
  }

  @Test
  fun testImportRajdipDharJson() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AttendTrackDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = AttendanceRepository(db.dao(), context)

    val res = repo.importFromJson(com.example.data.model.StudentScheduleTemplates.RAJDIP_DHAR_JSON, cleanReplace = true)
    org.junit.Assert.assertTrue("Import should succeed: ${res.message}", res.success)

    val profile = repo.getProfile()
    org.junit.Assert.assertNotNull(profile)
    assertEquals("Rajdip Dhar", profile?.name)
    assertEquals("Heritage Institute of Technology, Kolkata", profile?.institution)
    assertEquals("Computer Science Engineering", profile?.department)
    assertEquals("B", profile?.section)
    assertEquals("Gr. 1", profile?.group)

    val slots = db.dao().getAllSlotsSync()
    org.junit.Assert.assertTrue("Slots count should be > 20, was: ${slots.size}", slots.size >= 20)

    val snapshot = repo.getLiveDatabaseSnapshot()
    org.junit.Assert.assertTrue(snapshot.contains("Rajdip Dhar"))
    org.junit.Assert.assertTrue(snapshot.contains("CB601"))
    org.junit.Assert.assertTrue(snapshot.contains("CB106A"))
    org.junit.Assert.assertTrue(snapshot.contains("TIMETABLE & LAB SCHEDULE NOTES"))
    org.junit.Assert.assertFalse("Snapshot must not contain streak", snapshot.contains("Attendance Streak"))

    db.close()
  }

  @Test
  fun test12HourTimeFormatting() {
    assertEquals("9:00 AM", com.example.data.model.TimetableSlot.formatTo12Hour("09:00"))
    assertEquals("10:00 AM", com.example.data.model.TimetableSlot.formatTo12Hour("10:00"))
    assertEquals("12:00 PM", com.example.data.model.TimetableSlot.formatTo12Hour("12:00"))
    assertEquals("1:00 PM", com.example.data.model.TimetableSlot.formatTo12Hour("13:00"))
    assertEquals("3:00 PM", com.example.data.model.TimetableSlot.formatTo12Hour("15:00"))
    assertEquals("5:00 PM", com.example.data.model.TimetableSlot.formatTo12Hour("17:00"))
    assertEquals("9:00 AM", com.example.data.model.TimetableSlot.formatTo12Hour("9:00 AM"))
    assertEquals("1:00 PM", com.example.data.model.TimetableSlot.formatTo12Hour("01:00 PM"))
    assertEquals("1:00 PM", com.example.data.model.TimetableSlot.formatTo12Hour("1:00 PM"))

    val slot = com.example.data.model.TimetableSlot(
        dayOfWeek = 1,
        subjectId = 1,
        startTime = "14:00",
        endTime = "16:00"
    )
    assertEquals("2:00 PM", slot.getDisplayStartTime())
    assertEquals("4:00 PM", slot.getDisplayEndTime())
    assertEquals("2:00 PM - 4:00 PM", slot.getDisplayTimeSpan())
  }
}

