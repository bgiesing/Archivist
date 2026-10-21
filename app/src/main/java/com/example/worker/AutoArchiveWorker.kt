package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.db.ArchiveLogEntity
import com.example.data.db.ArchivistDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AppScannerRepository
import com.example.data.shizuku.ShizukuManager
import kotlinx.coroutines.flow.first

class AutoArchiveWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "AutoArchiveWorker started.")
        val db = ArchivistDatabase.getInstance(appContext)
        val dao = db.dao()
        val prefs = UserPreferencesRepository(appContext)
        val settings = prefs.settingsFlow.first()

        val shizukuManager = ShizukuManager(appContext)
        shizukuManager.checkStatus()

        if (!shizukuManager.isAuthorized()) {
            Log.w(TAG, "AutoArchiveWorker: Shizuku is not authorized or running. Skipping auto-archive.")
            dao.insertLog(
                ArchiveLogEntity(
                    packageName = "system.auto_archive",
                    appName = "Auto-Archiver",
                    actionType = "AUTO_ARCHIVE_SKIPPED",
                    bytesReclaimed = 0L,
                    success = false,
                    message = "Shizuku not authorized or binder dead when worker executed."
                )
            )
            return Result.success()
        }

        val scanner = AppScannerRepository(appContext, dao)
        val apps = scanner.scanApps(settings)
        val eligibleApps = apps.filter { it.isEligibleForAutoArchive && it.isArchivable && !it.isSystemApp }

        if (eligibleApps.isEmpty()) {
            Log.i(TAG, "AutoArchiveWorker: No apps eligible for auto-archive.")
            return Result.success()
        }

        var totalReclaimed = 0L
        var archivedCount = 0

        for (app in eligibleApps) {
            val result = shizukuManager.archivePackage(app.packageName)
            val success = result.success
            val reclaimed = if (success) app.reclaimableBytes else 0L

            if (success) {
                totalReclaimed += reclaimed
                archivedCount++
            }

            val cleanMessage = if (success) {
                "Auto-archived to save space"
            } else {
                cleanErrorMessage(result.output)
            }

            dao.insertLog(
                ArchiveLogEntity(
                    packageName = app.packageName,
                    appName = app.appName,
                    actionType = "AUTO_ARCHIVE",
                    bytesReclaimed = reclaimed,
                    success = success,
                    message = cleanMessage
                )
            )
        }

        Log.i(TAG, "AutoArchiveWorker: Archived $archivedCount apps, reclaimed $totalReclaimed bytes.")

        if (archivedCount > 0 && settings.notificationsEnabled) {
            sendSummaryNotification(archivedCount, totalReclaimed)
        }

        return Result.success()
    }

    private fun sendSummaryNotification(archivedCount: Int, bytesReclaimed: Long) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channelId = "archivist_auto_channel"
        val channel = NotificationChannel(
            channelId,
            "Auto-Archiving Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when Archivist automatically frees up storage"
        }
        notificationManager.createNotificationChannel(channel)

        val reclaimedFormatted = formatBytes(bytesReclaimed)
        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Archived $archivedCount unused apps")
            .setContentText("Reclaimed $reclaimedFormatted of storage automatically.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format("%.2f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }

    private fun cleanErrorMessage(output: String): String {
        return when {
            output.contains("System apps cannot be archived", ignoreCase = true) ->
                "System apps cannot be archived by Android."
            output.contains("does not have a main activity", ignoreCase = true) ->
                "Cannot be archived: App lacks a main launcher activity."
            output.contains("not found", ignoreCase = true) ->
                "Package not found or already uninstalled."
            output.contains("Failure [", ignoreCase = true) -> {
                val inner = output.substringAfter(":", "").substringBefore("]").trim()
                if (inner.isNotEmpty()) inner else output
            }
            else -> output
        }
    }

    companion object {
        private const val TAG = "AutoArchiveWorker"
        private const val NOTIFICATION_ID = 2002
    }
}
