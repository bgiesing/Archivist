package com.example

import android.app.Application
import com.example.data.db.ArchivistDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AppScannerRepository
import com.example.data.shizuku.ShizukuManager
import com.example.worker.WorkerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ArchivistApp : Application() {

    val database by lazy { ArchivistDatabase.getInstance(this) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }
    val shizukuManager by lazy { ShizukuManager(this) }
    val scannerRepository by lazy { AppScannerRepository(this, database.dao()) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Reschedule or update worker if needed
        applicationScope.launch {
            val settings = preferencesRepository.settingsFlow.first()
            if (settings.autoArchiveEnabled) {
                WorkerScheduler.scheduleOrUpdate(this@ArchivistApp, settings)
            }
        }
    }
}
