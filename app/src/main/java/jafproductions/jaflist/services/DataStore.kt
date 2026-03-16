package jafproductions.jaflist.services

import android.content.Context
import jafproductions.jaflist.models.AppData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR
}

class DataStore(private val context: Context) {

    private val firebaseService = FirebaseService.getInstance(context)
    private val dataFile = File(context.filesDir, "jaflist_data.json")

    private val _appData = MutableStateFlow(load())
    val appData: StateFlow<AppData> = _appData.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var debounceJob: Job? = null

    fun load(): AppData {
        return try {
            if (!dataFile.exists()) return AppData.empty
            val jsonString = dataFile.readText()
            firebaseService.gson.fromJson(jsonString, AppData::class.java) ?: AppData.empty
        } catch (e: Exception) {
            AppData.empty
        }
    }

    /** Saves to local file AND uploads to cloud. Caller must set lastModified before calling. */
    fun save(appData: AppData) {
        _appData.value = appData
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(500)
            writeLocalFile(appData)
            _syncStatus.value = SyncStatus.SYNCING
            try {
                firebaseService.upload(appData)
                _syncStatus.value = SyncStatus.SYNCED
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.OFFLINE
            }
        }
    }

    /** Saves to local file only. Does NOT update lastModified or upload. Use for UI-only state (e.g. expand/collapse). */
    fun saveLocalOnly(appData: AppData) {
        _appData.value = appData
        writeLocalFile(appData)
    }

    fun saveImmediately(appData: AppData) {
        debounceJob?.cancel()
        _appData.value = appData
        writeLocalFile(appData)
        scope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            try {
                firebaseService.upload(appData)
                _syncStatus.value = SyncStatus.SYNCED
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.OFFLINE
            }
        }
    }

    suspend fun initializeCloud() {
        withContext(Dispatchers.IO) {
            if (firebaseService.currentUser == null) return@withContext
            _syncStatus.value = SyncStatus.SYNCING
            try {
                val remoteData = firebaseService.download()
                if (remoteData == null) {
                    // No cloud data yet — upload local data to seed Firestore for other devices
                    val localData = _appData.value
                    if (localData.lastModified.after(Date(0))) {
                        firebaseService.upload(localData)
                    }
                } else {
                    val localData = _appData.value
                    val newerData = if (remoteData.lastModified.after(localData.lastModified)) {
                        remoteData
                    } else {
                        localData
                    }
                    _appData.value = newerData
                    writeLocalFile(newerData)
                    if (localData.lastModified.after(remoteData.lastModified)) {
                        firebaseService.upload(localData)
                    }
                }
                _syncStatus.value = SyncStatus.SYNCED
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.OFFLINE
            }
        }
    }

    private fun writeLocalFile(appData: AppData) {
        try {
            val jsonString = firebaseService.gson.toJson(appData)
            dataFile.writeText(jsonString)
        } catch (e: Exception) {
            // Silently fail local write; sync status will reflect issues
        }
    }

    fun close() {
        scope.coroutineContext[Job]?.cancel()
    }
}
