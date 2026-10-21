package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ArchiveRuleSettings
import com.example.data.model.FilterOption
import com.example.data.model.InactivityUnit
import com.example.data.model.SortOption
import com.example.data.model.StorageConditionType
import com.example.data.model.ThemeMode
import com.example.data.model.ViewMode
import com.example.data.model.WorkerIntervalUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "archivist_settings")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val AUTO_ARCHIVE_ENABLED = booleanPreferencesKey("auto_archive_enabled")
        val INACTIVITY_AMOUNT = intPreferencesKey("inactivity_amount")
        val INACTIVITY_UNIT = stringPreferencesKey("inactivity_unit")
        val MIN_APP_SIZE_BYTES = longPreferencesKey("min_app_size_bytes")
        val STORAGE_CONDITION_TYPE = stringPreferencesKey("storage_condition_type")
        val LOW_STORAGE_THRESHOLD_PERCENT = intPreferencesKey("low_storage_threshold_percent")
        val LOW_STORAGE_THRESHOLD_GB = intPreferencesKey("low_storage_threshold_gb")
        val WORKER_INTERVAL_AMOUNT = longPreferencesKey("worker_interval_amount")
        val WORKER_INTERVAL_UNIT = stringPreferencesKey("worker_interval_unit")
        val INCLUDE_SYSTEM_APPS = booleanPreferencesKey("include_system_apps")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val FILTER_OPTION = stringPreferencesKey("filter_option")
        val DONT_SHOW_RESTORE_EXPLANATION = booleanPreferencesKey("dont_show_restore_explanation")
    }

    val settingsFlow: Flow<ArchiveRuleSettings> = context.dataStore.data.map { prefs ->
        val autoEnabled = prefs[Keys.AUTO_ARCHIVE_ENABLED] ?: false
        val inactAmount = prefs[Keys.INACTIVITY_AMOUNT] ?: 3
        val inactUnit = try {
            InactivityUnit.valueOf(prefs[Keys.INACTIVITY_UNIT] ?: InactivityUnit.MONTHS.name)
        } catch (_: Exception) {
            InactivityUnit.MONTHS
        }
        val minSize = prefs[Keys.MIN_APP_SIZE_BYTES] ?: (50 * 1024 * 1024L) // 50MB
        val storageCond = try {
            StorageConditionType.valueOf(prefs[Keys.STORAGE_CONDITION_TYPE] ?: StorageConditionType.ALWAYS.name)
        } catch (_: Exception) {
            StorageConditionType.ALWAYS
        }
        val lowStoragePercent = prefs[Keys.LOW_STORAGE_THRESHOLD_PERCENT] ?: 15
        val lowStorageGb = prefs[Keys.LOW_STORAGE_THRESHOLD_GB] ?: 10
        val workerIntAmount = prefs[Keys.WORKER_INTERVAL_AMOUNT] ?: 24L
        val workerIntUnit = try {
            WorkerIntervalUnit.valueOf(prefs[Keys.WORKER_INTERVAL_UNIT] ?: WorkerIntervalUnit.HOURS.name)
        } catch (_: Exception) {
            WorkerIntervalUnit.HOURS
        }
        val includeSys = prefs[Keys.INCLUDE_SYSTEM_APPS] ?: false
        val notifEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false
        val tMode = try {
            ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
        val amoled = prefs[Keys.AMOLED_BLACK] ?: false
        val onboarded = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600 || context.resources.configuration.screenWidthDp >= 600
        val defaultViewMode = if (isTablet) ViewMode.GRID else ViewMode.LIST
        val vMode = try {
            ViewMode.valueOf(prefs[Keys.VIEW_MODE] ?: defaultViewMode.name)
        } catch (_: Exception) {
            defaultViewMode
        }
        val sOption = try {
            SortOption.valueOf(prefs[Keys.SORT_OPTION] ?: SortOption.LAST_USED_ASC.name)
        } catch (_: Exception) {
            SortOption.LAST_USED_ASC
        }
        val fOption = try {
            FilterOption.valueOf(prefs[Keys.FILTER_OPTION] ?: FilterOption.ALL.name)
        } catch (_: Exception) {
            FilterOption.ALL
        }

        ArchiveRuleSettings(
            autoArchiveEnabled = autoEnabled,
            inactivityAmount = inactAmount,
            inactivityUnit = inactUnit,
            minAppSizeBytes = minSize,
            storageConditionType = storageCond,
            lowStorageThresholdPercent = lowStoragePercent,
            lowStorageThresholdGB = lowStorageGb,
            workerIntervalAmount = workerIntAmount,
            workerIntervalUnit = workerIntUnit,
            includeSystemApps = includeSys,
            notificationsEnabled = notifEnabled,
            themeMode = tMode,
            amoledBlack = amoled,
            onboardingCompleted = onboarded,
            viewMode = vMode,
            sortOption = sOption,
            filterOption = fOption,
            dontShowRestoreExplanation = prefs[Keys.DONT_SHOW_RESTORE_EXPLANATION] ?: false
        )
    }

    suspend fun updateAutoArchiveEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_ARCHIVE_ENABLED] = enabled }
    }

    suspend fun updateInactivityThreshold(amount: Int, unit: InactivityUnit) {
        context.dataStore.edit {
            it[Keys.INACTIVITY_AMOUNT] = amount
            it[Keys.INACTIVITY_UNIT] = unit.name
        }
    }

    suspend fun updateMinAppSizeBytes(bytes: Long) {
        context.dataStore.edit { it[Keys.MIN_APP_SIZE_BYTES] = bytes }
    }

    suspend fun updateStorageCondition(type: StorageConditionType, thresholdPercent: Int, thresholdGB: Int) {
        context.dataStore.edit {
            it[Keys.STORAGE_CONDITION_TYPE] = type.name
            it[Keys.LOW_STORAGE_THRESHOLD_PERCENT] = thresholdPercent
            it[Keys.LOW_STORAGE_THRESHOLD_GB] = thresholdGB
        }
    }

    suspend fun updateWorkerSchedule(amount: Long, unit: WorkerIntervalUnit) {
        context.dataStore.edit {
            it[Keys.WORKER_INTERVAL_AMOUNT] = amount
            it[Keys.WORKER_INTERVAL_UNIT] = unit.name
        }
    }

    suspend fun updateIncludeSystemApps(include: Boolean) {
        context.dataStore.edit { it[Keys.INCLUDE_SYSTEM_APPS] = include }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    suspend fun updateAmoledBlack(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AMOLED_BLACK] = enabled }
    }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun updateViewMode(viewMode: ViewMode) {
        context.dataStore.edit { it[Keys.VIEW_MODE] = viewMode.name }
    }

    suspend fun updateSortOption(sortOption: SortOption) {
        context.dataStore.edit { it[Keys.SORT_OPTION] = sortOption.name }
    }

    suspend fun updateFilterOption(filterOption: FilterOption) {
        context.dataStore.edit { it[Keys.FILTER_OPTION] = filterOption.name }
    }

    suspend fun updateDontShowRestoreExplanation(dontShow: Boolean) {
        context.dataStore.edit { it[Keys.DONT_SHOW_RESTORE_EXPLANATION] = dontShow }
    }
}
