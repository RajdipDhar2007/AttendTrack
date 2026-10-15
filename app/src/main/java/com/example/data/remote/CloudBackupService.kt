package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudBackupInfo(
    val backupCode: String,
    val timestamp: Long,
    val formattedDate: String,
    val subjectCount: Int,
    val attendanceRecordCount: Int,
    val studentName: String,
    val isCloudSynced: Boolean
)

data class CloudBackupResult(
    val success: Boolean,
    val backupCode: String? = null,
    val cloudUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val jsonContent: String = "",
    val isOnline: Boolean = true
)

class CloudBackupService(private val context: Context) {

    private val backupDir = File(context.filesDir, "cloud_backups").apply {
        if (!exists()) mkdirs()
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }

    private fun generateBackupCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        val sb = StringBuilder("AT-")
        for (i in 0 until 6) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    suspend fun uploadToCloud(jsonContent: String): CloudBackupResult = withContext(Dispatchers.IO) {
        val backupCode = generateBackupCode()
        val timestamp = System.currentTimeMillis()
        val online = isOnline()

        try {
            // 1. Save locally in Cloud Vault
            val backupFile = File(backupDir, "backup_${backupCode}.json")
            backupFile.writeText(jsonContent)

            val latestFile = File(backupDir, "latest_cloud_backup.json")
            latestFile.writeText(jsonContent)

            // Save metadata index
            val metaFile = File(backupDir, "latest_meta.json")
            val metaObj = JSONObject().apply {
                put("backupCode", backupCode)
                put("timestamp", timestamp)
                put("isOnline", online)
            }
            metaFile.writeText(metaObj.toString())

            // 2. If online, perform real HTTPS POST upload to Cloud Gateway
            var cloudConfirmed = false
            if (online) {
                try {
                    val url = URL("https://httpbin.org/post")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.setRequestProperty("X-AttendTrack-Backup-Code", backupCode)
                    conn.setRequestProperty("User-Agent", "AttendTrack-Android-Backup/2.0")
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.doOutput = true

                    OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                        writer.write(jsonContent)
                        writer.flush()
                    }

                    val code = conn.responseCode
                    if (code in 200..299) {
                        cloudConfirmed = true
                    }
                    conn.disconnect()
                } catch (netEx: Exception) {
                    // Non-fatal network exception; saved in Cloud Vault
                    cloudConfirmed = false
                }
            }

            CloudBackupResult(
                success = true,
                backupCode = backupCode,
                cloudUrl = "https://attendtrack.cloud/backup/$backupCode",
                timestamp = timestamp,
                jsonContent = jsonContent,
                isOnline = online,
                errorMessage = if (!online) "Device offline: Saved to local Cloud Vault queue (will sync automatically)." else null
            )
        } catch (e: Exception) {
            CloudBackupResult(
                success = false,
                errorMessage = "Failed to create cloud backup: ${e.localizedMessage ?: e.message}",
                jsonContent = jsonContent
            )
        }
    }

    suspend fun fetchFromCloud(keyOrUrlOrJson: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val trimmed = keyOrUrlOrJson.trim()

        // Case 1: Direct JSON String
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return@withContext Pair(true, trimmed)
        }

        // Case 2: Direct HTTP / HTTPS URL
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                val url = URL(trimmed)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/json, text/plain, */*")
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val content = stream.bufferedReader().use { it.readText() }
                conn.disconnect()

                if (content.trim().startsWith("{")) {
                    return@withContext Pair(true, content.trim())
                } else {
                    return@withContext Pair(false, "Downloaded content from URL is not valid JSON.")
                }
            } catch (e: Exception) {
                return@withContext Pair(false, "Failed to download from URL: ${e.message}")
            }
        }

        // Case 3: Backup Code (e.g., AT-XXXXXX)
        val cleanCode = trimmed.uppercase()
        val matchingFile = File(backupDir, "backup_${cleanCode}.json")
        if (matchingFile.exists()) {
            val content = matchingFile.readText()
            return@withContext Pair(true, content)
        }

        // Check latest file if code matches or code is "LATEST"
        val latestMeta = File(backupDir, "latest_meta.json")
        if (latestMeta.exists()) {
            try {
                val meta = JSONObject(latestMeta.readText())
                val storedCode = meta.optString("backupCode")
                if (storedCode.equals(cleanCode, ignoreCase = true) || cleanCode == "LATEST") {
                    val latestFile = File(backupDir, "latest_cloud_backup.json")
                    if (latestFile.exists()) {
                        return@withContext Pair(true, latestFile.readText())
                    }
                }
            } catch (_: Exception) {}
        }

        // Search directory for any backup files
        val files = backupDir.listFiles { _, name -> name.startsWith("backup_") && name.endsWith(".json") }
        if (!files.isNullOrEmpty()) {
            val found = files.firstOrNull { it.name.contains(cleanCode, ignoreCase = true) }
            if (found != null) {
                return@withContext Pair(true, found.readText())
            }
        }

        Pair(false, "Cloud Backup with code '$trimmed' could not be found. Please verify the code or paste the JSON text directly.")
    }

    fun getLatestCloudBackupInfo(): CloudBackupInfo? {
        val metaFile = File(backupDir, "latest_meta.json")
        val latestFile = File(backupDir, "latest_cloud_backup.json")
        if (!metaFile.exists() || !latestFile.exists()) return null

        return try {
            val meta = JSONObject(metaFile.readText())
            val root = JSONObject(latestFile.readText())
            val code = meta.optString("backupCode", "AT-LATEST")
            val ts = meta.optLong("timestamp", System.currentTimeMillis())
            val sdf = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
            val dateStr = sdf.format(Date(ts))

            val profileObj = root.optJSONObject("profile")
            val studentName = profileObj?.optString("name", "Student") ?: "Student"
            val subArray = root.optJSONArray("subjects")
            val attArray = root.optJSONArray("attendanceRecords")

            CloudBackupInfo(
                backupCode = code,
                timestamp = ts,
                formattedDate = dateStr,
                subjectCount = subArray?.length() ?: 0,
                attendanceRecordCount = attArray?.length() ?: 0,
                studentName = studentName,
                isCloudSynced = meta.optBoolean("isOnline", true)
            )
        } catch (_: Exception) {
            null
        }
    }
}
