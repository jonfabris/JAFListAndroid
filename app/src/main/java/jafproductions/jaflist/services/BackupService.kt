package jafproductions.jaflist.services

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupInfo(
    val file: File,
    val date: Date
) {
    val id: String get() = file.absolutePath

    val displayName: String get() {
        val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return formatter.format(date)
    }
}

class BackupService(private val context: Context) {

    companion object {
        private const val MAX_BACKUPS = 8
        private const val BACKUP_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000
        private const val PREFS_NAME = "jaflist_backup_prefs"
        private const val LAST_BACKUP_KEY = "lastBackupDate"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun performAutoBackupIfNeeded(dataFile: File) {
        val lastBackupMs = prefs.getLong(LAST_BACKUP_KEY, 0L)
        if (System.currentTimeMillis() - lastBackupMs < BACKUP_INTERVAL_MS) return
        if (!dataFile.exists()) return
        createBackup(dataFile)
    }

    fun createBackup(dataFile: File) {
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val timestamp = formatter.format(Date())
        val backupFile = File(context.filesDir, "jaflist_backup_$timestamp.json")

        try {
            dataFile.copyTo(backupFile, overwrite = true)
            prefs.edit().putLong(LAST_BACKUP_KEY, System.currentTimeMillis()).apply()
            pruneOldBackups()
        } catch (e: Exception) {
            // Silently fail
        }
    }

    fun listBackups(): List<BackupInfo> {
        return context.filesDir.listFiles()
            ?.filter { it.name.startsWith("jaflist_backup_") && it.name.endsWith(".json") }
            ?.map { BackupInfo(file = it, date = Date(it.lastModified())) }
            ?.sortedByDescending { it.date }
            ?: emptyList()
    }

    fun restore(backup: BackupInfo, dataFile: File) {
        backup.file.copyTo(dataFile, overwrite = true)
    }

    private fun pruneOldBackups() {
        val backups = listBackups()
        if (backups.size <= MAX_BACKUPS) return
        backups.drop(MAX_BACKUPS).forEach { it.file.delete() }
    }
}
