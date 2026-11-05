package com.example.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassType
import com.example.data.model.OverallAttendanceStat
import com.example.data.model.Profile
import com.example.data.model.Subject
import com.example.data.model.SubjectAttendanceStat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Renders the attendance report (summary, per-subject table, full class log) as a PDF. */
object AttendancePdfExporter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private const val BOTTOM = PAGE_H - 50f

    private val PRIMARY = Color.parseColor("#4F46E5")
    private val GOOD = Color.parseColor("#15803D")
    private val BAD = Color.parseColor("#B91C1C")
    private val MUTED = Color.parseColor("#6B7280")

    private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    /** Writes the report to [uri]. Must be called off the main thread. */
    fun write(
        context: Context,
        uri: Uri,
        profile: Profile?,
        overall: OverallAttendanceStat,
        stats: List<SubjectAttendanceStat>,
        records: List<AttendanceRecord>,
        subjects: List<Subject>
    ) {
        val doc = PdfDocument()
        val writer = PageWriter(doc)
        try {
            drawSummary(writer, profile, overall)
            drawSubjectTable(writer, stats)
            drawLog(writer, records, subjects)
            writer.finish()
            val stream = context.contentResolver.openOutputStream(uri, "w")
                ?: throw IllegalStateException("Could not open the selected file")
            stream.use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    private class PageWriter(private val doc: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var number = 0
        var y = MARGIN
        val canvas get() = page!!.canvas

        fun newPage() {
            finish()
            number++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, number).create())
            y = MARGIN
        }

        fun ensure(height: Float, onNewPage: (() -> Unit)? = null) {
            if (page == null) newPage()
            if (y + height > BOTTOM) {
                newPage()
                onNewPage?.invoke()
            }
        }

        fun finish() {
            page?.let {
                val p = paint(9f, color = MUTED)
                it.canvas.drawText("AttendTrack · page $number", MARGIN, PAGE_H - 24f, p)
                doc.finishPage(it)
            }
            page = null
        }
    }

    private fun drawSummary(w: PageWriter, profile: Profile?, overall: OverallAttendanceStat) {
        w.ensure(200f)
        w.canvas.drawText("AttendTrack", MARGIN, w.y + 22f, paint(26f, true, PRIMARY))
        w.y += 30f
        w.canvas.drawText("Attendance Report", MARGIN, w.y + 14f, paint(14f, false, MUTED))
        w.y += 20f
        w.canvas.drawText(
            "Generated ${LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy"))}",
            MARGIN, w.y + 10f, paint(10f, false, MUTED)
        )
        w.y += 28f

        if (profile != null) {
            val lines = listOf(
                profile.name,
                listOf(profile.institution, profile.department).filter { it.isNotBlank() }.joinToString(" · "),
                listOf(profile.year, profile.semester, profile.section, profile.group, profile.sessionYear)
                    .filter { it.isNotBlank() }.joinToString(" · ")
            ).filter { it.isNotBlank() }
            lines.forEachIndexed { i, line ->
                w.canvas.drawText(line, MARGIN, w.y + 12f, paint(if (i == 0) 13f else 11f, i == 0))
                w.y += 17f
            }
            w.y += 10f
        }

        val ok = overall.percentage >= overall.targetPercentage
        val headline = if (overall.totalEffective == 0) "--" else String.format("%.1f%%", overall.percentage)
        w.canvas.drawText(headline, MARGIN, w.y + 38f, paint(40f, true, if (ok) GOOD else BAD))
        w.canvas.drawText("overall attendance (target ${overall.targetPercentage.toInt()}%)", MARGIN + 130f, w.y + 36f, paint(11f, false, MUTED))
        w.y += 56f

        val facts = listOf(
            "Present" to overall.totalPresent.toString(),
            "Missed" to overall.totalAbsent.toString(),
            "Cancelled" to overall.totalCancelled.toString(),
            "Safe bunks" to overall.safeBunks.toString(),
            "Need to attend" to overall.classesNeeded.toString(),
            "Best streak" to overall.bestStreak.toString()
        )
        val colW = (PAGE_W - 2 * MARGIN) / facts.size
        facts.forEachIndexed { i, (label, value) ->
            val x = MARGIN + i * colW
            w.canvas.drawText(value, x, w.y + 18f, paint(18f, true))
            w.canvas.drawText(label, x, w.y + 32f, paint(9f, false, MUTED))
        }
        w.y += 52f
    }

    private fun drawSubjectTable(w: PageWriter, stats: List<SubjectAttendanceStat>) {
        val heading = {
            w.canvas.drawText("Subject-wise attendance", MARGIN, w.y + 14f, paint(14f, true, PRIMARY))
            w.y += 26f
            drawRow(w, listOf("Subject", "Type", "Present", "Missed", "Cancel.", "%"), header = true)
        }
        w.ensure(60f)
        heading()
        if (stats.isEmpty()) {
            w.canvas.drawText("No subjects yet.", MARGIN, w.y + 12f, paint(11f, false, MUTED))
            w.y += 24f
            return
        }
        stats.forEach { s ->
            w.ensure(20f, heading)
            val title = if (s.subject.code.isBlank()) s.subject.name else "${s.subject.code} ${s.subject.name}"
            drawRow(
                w,
                listOf(
                    title,
                    if (s.classType == ClassType.LAB) "Lab" else "Theory",
                    s.presentCount.toString(),
                    s.absentCount.toString(),
                    s.cancelledCount.toString(),
                    if (s.effectiveCount == 0) "--" else String.format("%.1f", s.percentage)
                ),
                percentColor = if (s.effectiveCount == 0) null else if (s.percentage >= s.targetPercentage) GOOD else BAD
            )
        }
        w.y += 16f
    }

    private fun drawLog(w: PageWriter, records: List<AttendanceRecord>, subjects: List<Subject>) {
        val names = subjects.associateBy { it.id }
        val heading = {
            w.canvas.drawText("Class log", MARGIN, w.y + 14f, paint(14f, true, PRIMARY))
            w.y += 26f
            drawRow(w, listOf("Date", "Subject", "Type", "Status"), header = true, log = true)
        }
        w.ensure(60f)
        heading()
        val sorted = records.sortedWith(compareByDescending<AttendanceRecord> { it.date }.thenBy { it.id })
        if (sorted.isEmpty()) {
            w.canvas.drawText("No classes have been marked yet.", MARGIN, w.y + 12f, paint(11f, false, MUTED))
            w.y += 24f
            return
        }
        sorted.forEach { r ->
            w.ensure(18f, heading)
            val subject = names[r.subjectId]
            val status = when (r.status) {
                AttendanceStatus.PRESENT -> "Present"
                AttendanceStatus.ABSENT -> "Missed"
                AttendanceStatus.CANCELLED -> "Cancelled"
            }
            drawRow(
                w,
                listOf(r.date, subject?.name ?: "Unknown", if (r.classType == ClassType.LAB) "Lab" else "Theory", status),
                log = true,
                statusColor = when (r.status) {
                    AttendanceStatus.PRESENT -> GOOD
                    AttendanceStatus.ABSENT -> BAD
                    AttendanceStatus.CANCELLED -> MUTED
                }
            )
        }
    }

    private fun drawRow(
        w: PageWriter,
        cells: List<String>,
        header: Boolean = false,
        log: Boolean = false,
        percentColor: Int? = null,
        statusColor: Int? = null
    ) {
        val xs = if (log) listOf(MARGIN, MARGIN + 80f, MARGIN + 340f, MARGIN + 410f)
        else listOf(MARGIN, MARGIN + 270f, MARGIN + 325f, MARGIN + 385f, MARGIN + 440f, MARGIN + 495f)
        val firstWidth = (if (log) 255f else 255f)
        val base = paint(if (header) 10f else 10.5f, header, if (header) MUTED else Color.BLACK)
        val rowH = 17f
        cells.forEachIndexed { i, text ->
            val p = Paint(base)
            if (i == cells.lastIndex) {
                (if (log) statusColor else percentColor)?.let { p.color = it; p.typeface = Typeface.DEFAULT_BOLD }
            }
            val maxW = if (i == 1 && log) 250f else if (i == 0 && !log) firstWidth else Float.MAX_VALUE
            w.canvas.drawText(fit(text, p, maxW), xs[i], w.y + 12f, p)
        }
        w.y += rowH
        if (header) {
            val line = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f }
            w.canvas.drawLine(MARGIN, w.y - 3f, PAGE_W - MARGIN, w.y - 3f, line)
        }
    }

    private fun fit(text: String, p: Paint, maxWidth: Float): String {
        if (p.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && p.measureText(text, 0, end) + p.measureText("…") > maxWidth) end--
        return text.substring(0, end) + "…"
    }
}
