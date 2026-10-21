package com.example.data.repository

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import android.util.Log
import com.example.data.db.ArchivistDao
import com.example.data.model.AppItem
import com.example.data.model.ArchiveRuleSettings
import com.example.data.model.StorageConditionType
import com.example.data.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppScannerRepository(
    private val context: Context,
    private val dao: ArchivistDao
) {

    /**
     * Checks if Usage Stats permission (Settings -> Usage Access) is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Returns real device storage statistics via StatFs
     */
    fun getDeviceStorageInfo(): StorageInfo {
        return try {
            val path = Environment.getDataDirectory()
            val statFs = StatFs(path.path)
            val blockSize = statFs.blockSizeLong
            val totalBlocks = statFs.blockCountLong
            val availableBlocks = statFs.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

            StorageInfo(totalBytes = totalBytes, freeBytes = freeBytes, usedBytes = usedBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read device storage info", e)
            StorageInfo(totalBytes = 64L * 1024 * 1024 * 1024, freeBytes = 16L * 1024 * 1024 * 1024, usedBytes = 48L * 1024 * 1024 * 1024)
        }
    }

    /**
     * Reads the most accurate last-used timestamp for all packages by aggregating across
     * YEARLY, MONTHLY, WEEKLY, and DAILY UsageStats intervals, taking the maximum of
     * lastTimeUsed and lastTimeVisible.
     */
    fun getAccurateLastUsedMap(): Map<String, Long> {
        if (!hasUsageStatsPermission()) return emptyMap()
        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyMap()

        val lastUsedMap = mutableMapOf<String, Long>()
        val now = System.currentTimeMillis()
        // Android UsageStatsDatabase typically stores yearly data for up to ~2 years (730 days)
        val twoYearsAgo = now - (730L * 24 * 60 * 60 * 1000L)
        val sixMonthsAgo = now - (180L * 24 * 60 * 60 * 1000L)
        val fourWeeksAgo = now - (28L * 24 * 60 * 60 * 1000L)
        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000L)

        fun updateFromStats(statsList: List<android.app.usage.UsageStats>?) {
            statsList?.forEach { stat ->
                val pkg = stat.packageName
                // Check both lastTimeUsed and lastTimeVisible (API 29+)
                val visibleTime = try {
                    stat.lastTimeVisible
                } catch (_: NoSuchMethodError) {
                    0L
                } catch (_: Exception) {
                    0L
                }
                val time = maxOf(stat.lastTimeUsed, visibleTime)
                if (time > 0L) {
                    val currentMax = lastUsedMap[pkg] ?: 0L
                    if (time > currentMax) {
                        lastUsedMap[pkg] = time
                    }
                }
            }
        }

        try {
            // 1. Query yearly interval (catches apps used many months or up to 2 years ago)
            updateFromStats(usageManager.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, twoYearsAgo, now))
            // 2. Query monthly interval (granular stats over past 6 months)
            updateFromStats(usageManager.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, sixMonthsAgo, now))
            // 3. Query weekly interval (granular stats over past 4 weeks)
            updateFromStats(usageManager.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, fourWeeksAgo, now))
            // 4. Query daily interval (granular stats over past 7 days)
            updateFromStats(usageManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, sevenDaysAgo, now))
            // 5. Query and aggregate across the 2-year window as cross-check
            val aggregated = usageManager.queryAndAggregateUsageStats(twoYearsAgo, now)
            aggregated?.values?.let { updateFromStats(it.toList()) }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying accurate usage stats", e)
        }

        return lastUsedMap
    }

    /**
     * Re-evaluates eligibility for auto-archive across an existing list of apps instantly
     */
    fun evaluateEligibility(apps: List<AppItem>, rules: ArchiveRuleSettings, storage: StorageInfo): List<AppItem> {
        val meetsStorageCondition = when (rules.storageConditionType) {
            StorageConditionType.ALWAYS -> true
            StorageConditionType.BELOW_PERCENTAGE -> storage.freePercent <= rules.lowStorageThresholdPercent
            StorageConditionType.BELOW_FREE_SPACE -> storage.freeGB <= rules.lowStorageThresholdGB
        }

        val inactivityThresholdMs = rules.inactivityUnit.toMillis(rules.inactivityAmount)
        val now = System.currentTimeMillis()

        return apps.map { app ->
            val meetsInactivity = if (app.lastUsedTimestamp > 0L) {
                (now - app.lastUsedTimestamp) >= inactivityThresholdMs
            } else {
                // If an app has NEVER been used, it is completely unused.
                // We only require that it wasn't installed within a small grace period (48 hours)
                // so a user has time to launch a brand new app.
                // Beyond 48 hours, never-opened apps are always eligible for archive rules.
                val installAge = if (app.firstInstallTimestamp > 0L) now - app.firstInstallTimestamp else Long.MAX_VALUE
                val minInstallGracePeriodMs = minOf(inactivityThresholdMs, 48L * 60 * 60 * 1000L)
                installAge >= minInstallGracePeriodMs
            }
            val totalAppAndCache = app.appBytes + app.cacheBytes
            val meetsSize = totalAppAndCache >= rules.minAppSizeBytes

            val isEligible = app.isArchivable &&
                    !app.isSystemApp &&
                    !app.isArchived &&
                    !app.isPinned &&
                    meetsStorageCondition &&
                    meetsInactivity &&
                    meetsSize

            app.copy(isEligibleForAutoArchive = isEligible)
        }
    }

    /**
     * Scans all installed & archived apps, correlates usage and storage, and evaluates rules.
     */
    suspend fun scanApps(rules: ArchiveRuleSettings): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val pinnedPackages = try {
            dao.getPinnedPackageNames().toSet()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching pinned packages", e)
            emptySet()
        }

        // Query comprehensive, multi-interval usage stats (catches apps active up to 2 years ago)
        val now = System.currentTimeMillis()
        val usageStatsMap = getAccurateLastUsedMap()

        val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        val myUserHandle = Process.myUserHandle()
        val defaultStorageUuid = StorageManager.UUID_DEFAULT
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps

        val deviceStorage = getDeviceStorageInfo()
        val meetsStorageCondition = when (rules.storageConditionType) {
            StorageConditionType.ALWAYS -> true
            StorageConditionType.BELOW_PERCENTAGE -> deviceStorage.freePercent <= rules.lowStorageThresholdPercent
            StorageConditionType.BELOW_FREE_SPACE -> deviceStorage.freeGB <= rules.lowStorageThresholdGB
        }

        val inactivityThresholdMs = rules.inactivityUnit.toMillis(rules.inactivityAmount)

        // Android 15 MATCH_ARCHIVED_PACKAGES flag
        val flags = PackageManager.PackageInfoFlags.of(PackageManager.MATCH_ARCHIVED_PACKAGES.toLong())
        val installedPackages = try {
            pm.getInstalledPackages(flags)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed packages with MATCH_ARCHIVED_PACKAGES", e)
            pm.getInstalledPackages(0)
        }

        val resultList = mutableListOf<AppItem>()

        for (pkgInfo in installedPackages) {
            val pkgName = pkgInfo.packageName

            // Skip this app itself
            if (pkgName == context.packageName) continue

            val appInfo = pkgInfo.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            // In Android 15, system apps can NEVER be archived under any circumstances (even if updated).
            // Skip system apps if system apps are excluded.
            if (isSystem && !rules.includeSystemApps) {
                continue
            }

            // Check if app is archived (Android 15 API 35 `isArchived` on ApplicationInfo)
            val isArchived = try {
                appInfo.isArchived
            } catch (_: NoSuchMethodError) {
                false
            } catch (_: Exception) {
                false
            }

            // CRITICAL: In Android 15, only user apps with a main launcher activity can be archived.
            // System apps can NEVER be archived.
            val hasLauncherActivity = try {
                launcherApps?.getActivityList(pkgName, myUserHandle)?.isNotEmpty() == true
            } catch (_: Exception) {
                val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkgName)
                pm.queryIntentActivities(mainIntent, 0).isNotEmpty()
            }
            val isArchivable = !isSystem && (hasLauncherActivity || isArchived)

            // If an app cannot be archived and isn't already archived, do NOT show it unless system apps are explicitly enabled
            if (!isArchivable && !rules.includeSystemApps) {
                continue
            }

            val appName = try {
                appInfo.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkgName
            }

            val icon = try {
                appInfo.loadIcon(pm)
            } catch (_: Exception) {
                null
            }

            // Query app storage stats
            var appBytes = 0L
            var dataBytes = 0L
            var cacheBytes = 0L

            if (storageStatsManager != null) {
                try {
                    val stats = storageStatsManager.queryStatsForPackage(defaultStorageUuid, pkgName, myUserHandle)
                    appBytes = stats.appBytes
                    dataBytes = stats.dataBytes
                    cacheBytes = stats.cacheBytes
                } catch (e: Exception) {
                    try {
                        val sourceDir = appInfo.sourceDir
                        if (sourceDir != null) {
                            appBytes = java.io.File(sourceDir).length()
                        }
                    } catch (_: Exception) {}
                }
            }

            // Accurate last-used timestamp from multi-interval UsageStats
            val recordedUsage = usageStatsMap[pkgName] ?: 0L
            val lastUsed = if (recordedUsage > 0L) recordedUsage else 0L

            val isPinned = pinnedPackages.contains(pkgName)

            // Inactivity duration for auto-archive eligibility:
            // Measured from the actual last-used timestamp if available.
            // If never used, all apps installed > 48h ago meet the inactivity condition.
            val meetsInactivity = if (lastUsed > 0L) {
                (now - lastUsed) >= inactivityThresholdMs
            } else {
                val installAge = if (pkgInfo.firstInstallTime > 0L) now - pkgInfo.firstInstallTime else Long.MAX_VALUE
                val minInstallGracePeriodMs = minOf(inactivityThresholdMs, 48L * 60 * 60 * 1000L)
                installAge >= minInstallGracePeriodMs
            }
            val totalAppAndCache = appBytes + cacheBytes
            val meetsSize = totalAppAndCache >= rules.minAppSizeBytes

            val isEligible = isArchivable && !isSystem && !isArchived && !isPinned && meetsStorageCondition && meetsInactivity && meetsSize

            resultList.add(
                AppItem(
                    packageName = pkgName,
                    appName = appName,
                    icon = icon,
                    appBytes = appBytes,
                    dataBytes = dataBytes,
                    cacheBytes = cacheBytes,
                    lastUsedTimestamp = lastUsed,
                    firstInstallTimestamp = pkgInfo.firstInstallTime,
                    isArchived = isArchived,
                    isPinned = isPinned,
                    isSystemApp = isSystem,
                    isArchivable = isArchivable,
                    isEligibleForAutoArchive = isEligible
                )
            )
        }

        resultList
    }

    /**
     * Checks if a specific package is currently in the archived state.
     */
    fun isPackageArchived(packageName: String): Boolean {
        return try {
            val flags = PackageManager.PackageInfoFlags.of(PackageManager.MATCH_ARCHIVED_PACKAGES.toLong())
            val pkg = context.packageManager.getPackageInfo(packageName, flags)
            val appInfo = pkg.applicationInfo ?: return false
            try {
                appInfo.isArchived
            } catch (_: NoSuchMethodError) {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "AppScannerRepository"
    }
}
