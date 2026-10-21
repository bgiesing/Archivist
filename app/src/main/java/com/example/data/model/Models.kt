package com.example.data.model

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val appBytes: Long = 0L,       // APK + split APKs
    val dataBytes: Long = 0L,      // User data
    val cacheBytes: Long = 0L,     // Cache
    val lastUsedTimestamp: Long = 0L,
    val firstInstallTimestamp: Long = 0L,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val isSystemApp: Boolean = false,
    val isArchivable: Boolean = true,
    val isEligibleForAutoArchive: Boolean = false
) {
    val totalBytes: Long get() = appBytes + dataBytes + cacheBytes
    val reclaimableBytes: Long get() = if (isArchived) 0L else (appBytes + cacheBytes)
}

enum class InactivityUnit(val label: String, val toMillisMultiplier: Long, val maxRange: Int) {
    DAYS("Days", 24 * 60 * 60 * 1000L, 90),
    WEEKS("Weeks", 7 * 24 * 60 * 60 * 1000L, 52),
    MONTHS("Months", 30 * 24 * 60 * 60 * 1000L, 36),
    YEARS("Years", 365 * 24 * 60 * 60 * 1000L, 5);

    fun toMillis(amount: Int): Long = amount * toMillisMultiplier
}

enum class StorageConditionType(val title: String, val subtitle: String) {
    ALWAYS("Always", "Archive whenever inactivity and size rules match"),
    BELOW_PERCENTAGE("Low Storage (%)", "Archive only when free storage drops below a percentage"),
    BELOW_FREE_SPACE("Low Storage (GB)", "Archive only when free storage drops below specific GB")
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("Follow System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class WorkerIntervalUnit(val label: String, val toHoursMultiplier: Long) {
    HOURS("Hours", 1L),
    DAYS("Days", 24L);

    fun toHours(amount: Long): Long = amount * toHoursMultiplier
}

enum class ViewMode {
    LIST,
    GRID
}

enum class SortOption(val displayName: String) {
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    SIZE_DESC("App Size (Largest)"),
    SIZE_ASC("App Size (Smallest)"),
    LAST_USED_ASC("Least Recently Used"),
    LAST_USED_DESC("Most Recently Used")
}

enum class FilterOption(val displayName: String) {
    ALL("All Apps"),
    ACTIVE("Active"),
    ARCHIVED("Archived"),
    PINNED("Pinned"),
    ELIGIBLE("Eligible to Archive")
}

data class ArchiveRuleSettings(
    val autoArchiveEnabled: Boolean = false,
    val inactivityAmount: Int = 3,
    val inactivityUnit: InactivityUnit = InactivityUnit.MONTHS,
    val minAppSizeBytes: Long = 50 * 1024 * 1024L, // 50 MB
    val storageConditionType: StorageConditionType = StorageConditionType.ALWAYS,
    val lowStorageThresholdPercent: Int = 15, // 15% free space
    val lowStorageThresholdGB: Int = 10, // 10 GB free space
    val workerIntervalAmount: Long = 24L,
    val workerIntervalUnit: WorkerIntervalUnit = WorkerIntervalUnit.HOURS,
    val includeSystemApps: Boolean = false,
    val notificationsEnabled: Boolean = false, // off by default until permission granted
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
    val sortOption: SortOption = SortOption.LAST_USED_ASC,
    val filterOption: FilterOption = FilterOption.ALL,
    val dontShowRestoreExplanation: Boolean = false
)

data class StorageInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long
) {
    val freeGB: Double get() = freeBytes / (1024.0 * 1024.0 * 1024.0)
    val totalGB: Double get() = totalBytes / (1024.0 * 1024.0 * 1024.0)
    val freePercent: Float get() = if (totalBytes > 0) ((freeBytes.toFloat() / totalBytes) * 100f) else 0f
    val usedPercent: Float get() = if (totalBytes > 0) ((usedBytes.toFloat() / totalBytes) * 100f) else 0f
}

data class ArchiveDryRunResult(
    val eligibleApps: List<AppItem>,
    val totalReclaimableBytes: Long,
    val storageConditionMet: Boolean,
    val conditionDescription: String
)

sealed interface ShizukuState {
    data object Checking : ShizukuState
    data object NotInstalled : ShizukuState
    data object Dead : ShizukuState
    data object RunningUnauthorized : ShizukuState
    data class Authorized(val version: Int) : ShizukuState
}
