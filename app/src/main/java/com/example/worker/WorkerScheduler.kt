package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.model.ArchiveRuleSettings
import java.util.concurrent.TimeUnit

object WorkerScheduler {

    const val PERIODIC_WORK_TAG = "archivist_auto_archive_periodic"
    const val ONE_TIME_WORK_TAG = "archivist_auto_archive_onetime"

    fun scheduleOrUpdate(context: Context, settings: ArchiveRuleSettings) {
        val workManager = WorkManager.getInstance(context)

        if (!settings.autoArchiveEnabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK_TAG)
            return
        }

        // Interval converted to hours (WorkManager periodic minimum is 15 minutes, standard 1-24 hours)
        val hours = settings.workerIntervalUnit.toHours(settings.workerIntervalAmount).coerceAtLeast(1L)

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<AutoArchiveWorker>(
            repeatInterval = hours,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork
        )
    }

    fun triggerOneTimeCheck(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val oneTimeWork = OneTimeWorkRequestBuilder<AutoArchiveWorker>()
            .addTag(ONE_TIME_WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            oneTimeWork
        )
    }
}
