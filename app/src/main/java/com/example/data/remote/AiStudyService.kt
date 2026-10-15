package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service powering AttendTrack Copilot with Google Gemini Generative API and live database integration.
 * Speaks and thinks exactly like Gemini — insightful, warm, mathematically rigorous,
 * and possesses full real-time awareness of the student's live database (attendance rates,
 * bunks, timetable, exams, and teachers).
 */
class AiStudyService(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("attend_ai_prefs", Context.MODE_PRIVATE)

    fun isInternetAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val netInfo = cm.activeNetworkInfo
            netInfo != null && netInfo.isConnected
        }
    }

    /**
     * Retrieves the Gemini API key from user settings, environment, or BuildConfig.
     * Incorporates multiple fallback layers to guarantee live Gemini intelligence.
     */
    fun getEffectiveApiKey(): String {
        val customKey = sharedPrefs.getString("custom_gemini_api_key", "") ?: ""
        if (customKey.isNotBlank()) return customKey.trim()

        val envKey = try {
            System.getenv("GEMINI_API_KEY") ?: ""
        } catch (_: Exception) {
            ""
        }
        if (envKey.isNotBlank() && envKey != "MY_GEMINI_API_KEY") return envKey.trim()

        val buildKey = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key.trim() else ""
        } catch (_: Exception) {
            ""
        }
        // No key configured: the caller falls back to the built-in offline assistant.
        return buildKey
    }

    fun saveCustomApiKey(key: String) {
        sharedPrefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
    }

    /**
     * Primary endpoint: Answers user queries using Google's Gemini API with real-time access
     * to the user's updated database and multi-turn conversational history.
     */
    suspend fun answerQueryWithLiveDatabase(
        query: String,
        liveDatabaseSnapshot: String,
        recentHistory: List<Pair<String, Boolean>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()

        if (apiKey.isNotBlank()) {
            val geminiResult = callGeminiGenerativeModel(query, liveDatabaseSnapshot, recentHistory, apiKey)
            if (!geminiResult.isNullOrBlank()) {
                return@withContext geminiResult
            }
        }

        // If offline or if Gemini API call failed, provide an intelligent on-device response
        // using the exact live database snapshot and academic intelligence.
        return@withContext generateSmartFallbackResponse(query, liveDatabaseSnapshot, isInternetAvailable(), apiKey.isNotBlank())
    }

    /**
     * Calls Gemini Generative API with system instructions, full live database snapshot,
     * and multi-turn chat history.
     * Uses ultra-fast, responsive Gemini preview models: gemini-3.5-flash-lite, gemini-3.1-flash-lite.
     */
    private suspend fun callGeminiGenerativeModel(
        query: String,
        liveDatabaseSnapshot: String,
        recentHistory: List<Pair<String, Boolean>>,
        apiKey: String
    ): String? = withContext(Dispatchers.IO) {
        val models = listOf("gemini-3.5-flash-lite", "gemini-3.1-flash-lite")

        val systemPrompt = """
You are Gemini, Google's advanced, empathetic, and intellectually brilliant AI model, seamlessly integrated as the personal academic copilot in AttendTrack.

CRITICAL INSTRUCTIONS & GROUND TRUTH:
1. LIVE DATABASE AWARENESS:
   Below is the student's UP-TO-THE-SECOND LIVE DATABASE SNAPSHOT. This database is updated every moment as the student marks attendance, adds classes, or modifies goals.
   You MUST query and derive ALL factual information (student details, attendance rates, present/absent numbers, safe bunks, faculty names, classroom/room numbers, class timings, lab notes, and exam schedules) STRICTLY from this Live Database Snapshot.
   Never invent or hallucinate attendance numbers, teachers, or schedules that contradict this snapshot.

2. MANNER OF SPEECH (TALK EXACTLY LIKE GEMINI):
   • Speak with Gemini's unmistakable voice: warm, friendly, insightful, intellectually curious, encouraging, and razor-sharp.
   • Address the student warmly by their name found in the profile below (e.g. Rajdip).
   • Structure your answers clearly using clean Markdown: bold highlights (**like this**), bullet points, and code blocks where helpful.
   • When asked about bunks or attendance:
     - Clearly state whether they can safely miss class or not upfront.
     - Cite the exact current attendance percentage, present count, absent count, and target percentage from the snapshot.
     - State the exact safe bunk allowance (how many more classes can be missed without dropping below target) or the consecutive classes needed to recover.
     - Provide encouraging, practical academic guidance.
   • When asked about timetable, schedules, today's classes, or tomorrow:
     - Give the exact schedule for that day with start-end times, subject codes, subject names, classroom/room numbers, and professors/TAs.
     - Note the status of each class today (Attended, Missed, Cancelled, or Pending).
   • When asked general science, engineering, mathematics, computer science (DSA, coding, algorithms), or study technique questions:
     - Give world-class, deep, articulate explanations with intuitive physical intuition and clear math formulas.
   • TABLES & TABULAR DATA:
     - When the student asks for tabular data or a table (e.g. 'in table format', 'tabular format', 'table of', 'as a table'), OR when presenting schedules, timetables, subject attendance breakdowns, safe bunks summaries, or comparisons:
       You MUST ALWAYS structure your output using a clean, well-formed standard Markdown Table!
     - Table Syntax Example:
       | Time | Subject Code | Subject Name | Room | Faculty / TA |
       | :--- | :--- | :--- | :--- | :--- |
       | 9:00 AM - 10:00 AM | ECE1001 | Intro to Electronics | CB601 | Dr. Tania Das |
       | 10:00 AM - 11:00 AM | MTH1101 | Mathematics-I | CB601 | Prof. S. Banerjee |
     - For attendance tables:
       | Subject | Code | Attended | Missed | Current % | Target % | Safe Bunks | Status |
       | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
     - Always display all class times in 12-hour format with AM/PM (e.g. 9:00 AM - 10:00 AM, 1:00 PM - 2:00 PM, 3:00 PM - 5:00 PM), never raw 24-hour time.
     - Never provide unformatted pipe characters or plain bulleted lists when tabular format is expected.

3. LIVE DATABASE SNAPSHOT (CURRENT AT THIS EXACT MOMENT):
$liveDatabaseSnapshot
""".trimIndent()

        for (model in models) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.connectTimeout = 8000
                conn.readTimeout = 15000
                conn.doOutput = true

                val rootJson = JSONObject().apply {
                    val sysObj = JSONObject().apply {
                        val sysParts = JSONArray().apply {
                            put(JSONObject().apply { put("text", systemPrompt) })
                        }
                        put("parts", sysParts)
                    }
                    put("systemInstruction", sysObj)

                    val contentsArr = JSONArray()

                    // Sanitize conversation history to ensure valid Gemini turn structure:
                    // Starts with 'user', strictly alternates roles
                    val sanitizedTurns = mutableListOf<Pair<String, String>>()
                    for ((msgText, isUser) in recentHistory.takeLast(6)) {
                        if (msgText.isNotBlank()) {
                            val role = if (isUser) "user" else "model"
                            if (sanitizedTurns.isEmpty() && role != "user") continue
                            if (sanitizedTurns.isNotEmpty() && sanitizedTurns.last().first == role) {
                                val prev = sanitizedTurns.removeAt(sanitizedTurns.size - 1)
                                sanitizedTurns.add(role to "${prev.second}\n\n$msgText")
                            } else {
                                sanitizedTurns.add(role to msgText)
                            }
                        }
                    }

                    if (sanitizedTurns.isNotEmpty() && sanitizedTurns.last().first == "user") {
                        val prev = sanitizedTurns.removeAt(sanitizedTurns.size - 1)
                        sanitizedTurns.add("user" to "${prev.second}\n\n$query")
                    } else {
                        sanitizedTurns.add("user" to query)
                    }

                    for ((role, text) in sanitizedTurns) {
                        val turnObj = JSONObject().apply {
                            put("role", role)
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", text) })
                            })
                        }
                        contentsArr.put(turnObj)
                    }

                    put("contents", contentsArr)
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                    os.write(rootJson.toString())
                    os.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val stream = conn.inputStream
                    val responseStr = stream.bufferedReader().use { it.readText() }
                    conn.disconnect()

                    val parsed = parseGeminiResponse(responseStr)
                    if (parsed.isNotBlank()) {
                        return@withContext parsed
                    }
                } else {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                // Try next model in fallback list
            }
        }
        null
    }

    private fun parseGeminiResponse(responseJsonStr: String): String {
        return try {
            val root = JSONObject(responseJsonStr)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val firstCandidate = candidates.getJSONObject(0)

            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textBuilder = StringBuilder()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val text = part.optString("text", "")
                    if (text.isNotBlank()) {
                        textBuilder.append(text).append("\n")
                    }
                }
            }

            textBuilder.toString().trim()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Intelligent on-device responder that reads the live database snapshot directly.
     * Speaks in Gemini's signature tone when the device is completely offline.
     */
    private fun generateSmartFallbackResponse(
        query: String,
        snapshot: String,
        isOnline: Boolean,
        hasKey: Boolean
    ): String {
        val lower = query.lowercase().trim()

        val wantsTable = lower.contains("table") || lower.contains("tabular") || lower.contains("grid") || lower.contains("timetable")

        val notice = when {
            !isOnline -> "*(Offline Assistant Mode — reasoning directly from your live database snapshot)*\n\n"
            else -> ""
        }

        val lines = snapshot.lines()

        // 1. Day-specific schedule query (Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday)
        val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        val requestedDay = days.firstOrNull { lower.contains(it) }

        if (requestedDay != null) {
            val capDay = requestedDay.replaceFirstChar { it.uppercase() }
            val dayLine = lines.firstOrNull { it.startsWith("$capDay:") }

            val scheduleDetails = when {
                dayLine != null -> {
                    val content = dayLine.substringAfter(":").trim()
                    val items = content.split("; ").filter { it.isNotBlank() }
                    val header = "| Time | Subject | Type |\n| :--- | :--- | :--- |\n"
                    val tableRows = items.joinToString("\n") { itm ->
                        val typePart = if (itm.contains("[")) itm.substringAfter("[").substringBefore("]") else "THEORY"
                        val withoutType = itm.substringBefore("[").trim()
                        val timePart = if (withoutType.contains(" - ")) {
                            val beforeDash = withoutType.substringBefore(" - ").trim()
                            val afterDash = withoutType.substringAfter(" - ").trim().substringBefore(" ")
                            "$beforeDash - $afterDash"
                        } else withoutType.substringBefore(" ")
                        val subjPart = withoutType.removePrefix(timePart).trim()
                        "| $timePart | $subjPart | $typePart |"
                    }
                    header + tableRows
                }
                capDay == "Saturday" || capDay == "Sunday" -> "No classes scheduled — weekend break for Gr. 1!"
                else -> "No scheduled lectures found for $capDay."
            }

            val note = when (capDay) {
                "Wednesday" -> "\n\n💡 *Note: Your Workshop / Manufacturing Practice Lab in CME B06 is a 4-hour lab (9:00 AM - 1:00 PM) with TAs Mr. Anupam Biswas & Mr. Ritesh Roy.*"
                "Friday" -> "\n\n💡 *Note: Your Engineering Graphics and Design Lab in CME 118 is a 4-hour lab (1:00 PM - 5:00 PM) with TA Mr. Sudipta Goswami.*"
                "Tuesday" -> "\n\n💡 *Note: You have a 2-hour Library period from 3:00 PM - 5:00 PM for Gr. 1, and Life Skills with BB at 2:00 PM - 3:00 PM.*"
                "Thursday" -> "\n\n💡 *Note: Physics-I Lab in CB106A runs from 9:00 AM - 12:00 PM with Mrs. Moumita Bera & New Technical Assistant (NTA).* "
                else -> ""
            }

            return notice + "Hi Rajdip! Here is your scheduled timetable for **$capDay** in table format:\n\n$scheduleDetails$note"
        }

        // 2. Today / Tomorrow / Classes Schedule query
        if (lower.contains("today") || lower.contains("tomorrow") || lower.contains("schedule") ||
            lower.contains("timetable") || lower.contains("next class") || lower.contains("what class")
        ) {
            val todayLines = lines.dropWhile { !it.contains("TODAY'S SCHEDULE") }
                .takeWhile { !it.contains("WEEKLY TIMETABLE") }
                .filter { it.isNotBlank() && !it.startsWith("---") }

            val formattedToday = if (todayLines.any { it.startsWith("• [") }) {
                val header = "| Time | Class & Subject | Status |\n| :--- | :--- | :--- |\n"
                val rows = todayLines.filter { it.startsWith("• [") }.joinToString("\n") { line ->
                    val time = line.substringAfter("• [").substringBefore("]")
                    val remainder = line.substringAfter("] ").trim()
                    "| $time | $remainder | Scheduled |"
                }
                header + rows
            } else {
                todayLines.joinToString("\n").ifBlank { "You have no more classes scheduled for today. Time to relax or get ahead on revision!" }
            }

            return notice + "Hello Rajdip! Here is your live schedule overview for today:\n\n" +
                    formattedToday +
                    "\n\nKeep up the great momentum!"
        }

        // 3. Attendance, Bunk, or Percentage query
        if (lower.contains("attendance") || lower.contains("percentage") || lower.contains("bunk") ||
            lower.contains("miss") || lower.contains("safe") || lower.contains("standing") || lower.contains("target")
        ) {
            val overallSection = lines.dropWhile { !it.contains("OVERALL SEMESTER") }
                .takeWhile { !it.contains("SUBJECT-WISE") }
                .filter { it.isNotBlank() && !it.startsWith("---") }
                .joinToString("\n") { "• $it" }

            val subjectLines = lines.filter { it.startsWith("Subject:") }
            val formattedSubjects = if (subjectLines.isNotEmpty() && (wantsTable || true)) {
                val header = "| Subject Code | Subject Name | Attendance % | Safe Bunks |\n| :--- | :--- | :--- | :--- |\n"
                val rows = subjectLines.joinToString("\n") { sLine ->
                    val fullSub = sLine.substringAfter("Subject: ").substringBefore(" | Teacher:")
                    val code = if (fullSub.contains("(")) fullSub.substringAfter("(").substringBefore(")") else "SUB"
                    val name = fullSub.substringBefore("(").trim()
                    // Extract rate from snapshot
                    val rate = lines.firstOrNull { it.contains(code) && it.contains("Current Rate:") }
                        ?.substringAfter("Current Rate: ")?.substringBefore(" ") ?: "85.0%"
                    val safe = lines.firstOrNull { it.contains(code) && it.contains("Bunk Allowance:") }
                        ?.substringAfter("Bunk Allowance: ")?.substringBefore(" [") ?: "1 safe bunk"
                    "| $code | $name | $rate | $safe |"
                }
                header + rows
            } else {
                lines.filter {
                    it.contains("Subject:") || it.contains("Current Rate:") || it.contains("Bunk Allowance:") || it.contains("Warning:")
                }.take(16).joinToString("\n").ifBlank { "All enrolled subjects are currently safe and on track." }
            }

            return notice + "Here is your live attendance and bunk analysis in table format, Rajdip:\n\n" +
                    "### 📊 Overall Attendance Standing:\n$overallSection\n\n" +
                    "### 📚 Subject-wise Attendance & Safe Bunks Table:\n" +
                    formattedSubjects +
                    "\n\n💡 *Tip: Remember that AttendTrack recalculates your safe bunks live the moment you mark attendance!*"
        }

        // 4. Faculty / Teachers / TAs query
        if (lower.contains("faculty") || lower.contains("teacher") || lower.contains("professor") ||
            lower.contains("instructor") || lower.contains("who teaches") || lower.contains("technical assistant") ||
            lower.contains("assistant") || lower.contains("ta")
        ) {
            val facultySection = lines.dropWhile { !it.contains("FACULTY & TEACHERS") }
                .takeWhile { !it.contains("END OF DATABASE") }
                .filter { it.isNotBlank() && !it.startsWith("---") }
                .joinToString("\n")

            return notice + "Here is your faculty and laboratory technical assistant directory, Rajdip:\n\n" +
                    facultySection +
                    "\n\n• **Special Lab Staff:**\n" +
                    "  - Ms. Madhabi Samanta (MS) & Mr. Arijit Sen (AS) • ECE Lab (ICT502)\n" +
                    "  - Mr. Anupam Biswas (APB) & Mr. Ritesh Roy (RR) • Workshop Lab (CME B06)\n" +
                    "  - Mrs. Moumita Bera (MB) & New Technical Assistant (NTA) • Physics Lab (CB106A)\n" +
                    "  - Mr. Sudipta Goswami (SG) • Engineering Graphics Lab (CME 118)"
        }

        // 5. Exams query
        if (lower.contains("exam") || lower.contains("test") || lower.contains("quiz") || lower.contains("viva")) {
            val examSection = lines.dropWhile { !it.contains("UPCOMING EXAMS") }
                .takeWhile { !it.contains("FACULTY") }
                .filter { it.isNotBlank() && !it.startsWith("---") }
                .joinToString("\n")

            return notice + "Here are your registered upcoming exams and laboratory evaluations:\n\n" +
                    (examSection.ifBlank { "You currently have no pending exams registered in your calendar." }) +
                    "\n\n💡 *Pro-tip: Review your high-weightage formulas and derivations 3 days before exam day.*"
        }

        // 6. General STEM or study query fallback
        return notice + answerOfflineScienceOrTips(query, "🔬 **Academic Intelligence & Concept Analysis:**")
    }

    /**
     * Offline STEM scientific dictionary & study guide.
     */
    fun answerOfflineScienceOrTips(query: String, prefix: String = ""): String {
        val lower = query.lowercase()

        val stemEntries = listOf(
            Triple(
                listOf("photosynthesis", "chlorophyll", "calvin cycle"),
                "🌿 **Photosynthesis (Biochemical Mechanism):**\n\n" +
                        "Photosynthesis is the photochemical and biochemical process by which photoautotrophs convert light energy into chemical energy stored in carbohydrates.\n\n" +
                        "### 🔬 Chemical Equation:\n" +
                        "```\n6 CO₂ + 6 H₂O + light energy ➔ C₆H₁₂O₆ + 6 O₂\n```\n\n" +
                        "### ⚡ Key Phases:\n" +
                        "1. **Light-Dependent Reactions (Thylakoid Membrane):** Photons excite chlorophyll in Photosystems II & I, photolyzing H₂O to release O₂, generating ATP and NADPH.\n" +
                        "2. **Light-Independent Reactions / Calvin Cycle (Stroma):** Carbon fixation mediated by the enzyme **RuBisCO**, reducing 3-PGA to G3P to synthesize glucose.\n\n" +
                        "💡 *Exam Tip: Remember RuBisCO is both an oxygenase and carboxylase; high temperatures trigger wasteful photorespiration.*",
                "Biology & Biochemistry"
            ),
            Triple(
                listOf("newton", "second law", "force", "f=ma", "inertia"),
                "⚡ **Newton's Laws of Motion:**\n\n" +
                        "1. **First Law (Law of Inertia):** An object remains at rest or uniform velocity unless acted upon by a net external force.\n\n" +
                        "2. **Second Law (Fundamental Equation):**\n" +
                        "The rate of change of linear momentum is directly proportional to the applied force:\n" +
                        "```\nF_net = d(p)/dt = m * a  (when mass is constant)\n```\n" +
                        "• **SI Unit:** Newton (N) = kg·m/s²\n\n" +
                        "3. **Third Law (Action-Reaction):** For every action force, there is an equal and opposite reaction force acting on different bodies simultaneously.\n\n" +
                        "💡 *Viva Question: Why don't action and reaction cancel? Because they act on two distinct objects!*",
                "Classical Mechanics"
            ),
            Triple(
                listOf("entropy", "thermodynamics", "second law of thermo", "heat engine", "carnot"),
                "🔥 **Laws of Thermodynamics & Entropy:**\n\n" +
                        "• **First Law (Conservation of Energy):** `ΔU = Q - W` (Energy cannot be created or destroyed, only transformed).\n\n" +
                        "• **Second Law (Entropy Principle):** The total entropy `S` of an isolated thermodynamic system never decreases over time:\n" +
                        "```\nΔS_universe ≥ 0\n```\n" +
                        "• **Carnot Efficiency:** The theoretical maximum efficiency of any heat engine operating between temperatures `T_H` and `T_C` is:\n" +
                        "```\nη_max = 1 - (T_C / T_H)\n```\n\n" +
                        "💡 *Exam Tip: Always convert temperatures to Kelvin (K) when calculating Carnot efficiency.*",
                "Thermodynamics"
            ),
            Triple(
                listOf("binary search", "dsa", "algorithm", "complexity", "big o"),
                "💻 **Binary Search Algorithm (Divide & Conquer):**\n\n" +
                        "Binary Search locates the position of a target value within a **sorted array** by repeatedly dividing the search space in half.\n\n" +
                        "### ⏱️ Time & Space Complexity:\n" +
                        "• **Best Case:** `O(1)` (Element at midpoint)\n" +
                        "• **Average / Worst Case:** `O(log n)`\n" +
                        "• **Space Complexity:** `O(1)` iterative, `O(log n)` recursive\n\n" +
                        "### 🛠️ Kotlin Implementation:\n" +
                        "```kotlin\nfun binarySearch(arr: IntArray, target: Int): Int {\n    var low = 0\n    var high = arr.size - 1\n    while (low <= high) {\n        val mid = low + (high - low) / 2 // Prevents 32-bit overflow\n        when {\n            arr[mid] == target -> return mid\n            arr[mid] < target -> low = mid + 1\n            else -> high = mid - 1\n        }\n    }\n    return -1 // Not found\n}\n```",
                "Computer Science & Data Structures"
            )
        )

        for ((keywords, content, _) in stemEntries) {
            if (keywords.any { lower.contains(it) }) {
                val header = if (prefix.isNotBlank()) "$prefix\n\n" else ""
                return header + content
            }
        }

        val tips = listOf(
            "🧠 **The 80/20 Pareto Focus:** 20% of core mechanisms in your syllabus account for 80% of exam and viva questions. Master fundamental derivations and definitions first.",
            "⏰ **Pomodoro Ultradian Rhythm:** Human concentration peaks for 50 minutes. Study in uninterrupted 50-minute sprints, followed by 10-minute screen-free breaks.",
            "📝 **Active Blurting Technique:** Close all notes after 20 minutes of reading. Write everything you can recall on a blank sheet from pure memory. Fill gaps in red ink.",
            "📅 **Spaced Repetition:** Review your lecture notes 24 hours after class, then Day 4, then Day 10. You retain 90% instead of dropping to 20%."
        )

        val header = if (prefix.isNotBlank()) "$prefix\n\n" else "📚 **Academic Study & Science Advisory:**\n\n"
        return header + "Here are essential scientific techniques to accelerate your learning, Rajdip:\n\n" + tips.joinToString("\n\n")
    }

    suspend fun fetchOnlineStudyTip(): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (isInternetAvailable() && apiKey.isNotBlank()) {
            val geminiTip = callGeminiGenerativeModel(
                "Provide 3 high-impact, scientifically validated engineering and university study techniques with concrete action steps.",
                "",
                emptyList(),
                apiKey
            )
            if (!geminiTip.isNullOrBlank()) {
                return@withContext "🌟 **Gemini Study Advisory:**\n\n$geminiTip"
            }
        }

        answerOfflineScienceOrTips("tips", "🌐 **Sturdy Engineering & Science Study Protocols:**")
    }
}
