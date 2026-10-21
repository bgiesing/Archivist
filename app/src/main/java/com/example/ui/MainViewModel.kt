package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ArchivistApp
import com.example.data.db.ArchiveLogEntity
import com.example.data.db.PinnedAppEntity
import com.example.data.model.AppItem
import com.example.data.model.ArchiveDryRunResult
import com.example.data.model.ArchiveRuleSettings
import com.example.data.model.FilterOption
import com.example.data.model.InactivityUnit
import com.example.data.model.ShizukuState
import com.example.data.model.SortOption
import com.example.data.model.StorageConditionType
import com.example.data.model.StorageInfo
import com.example.data.model.ThemeMode
import com.example.data.model.ViewMode
import com.example.data.model.WorkerIntervalUnit
import com.example.worker.WorkerScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val allApps: List<AppItem> = emptyList(),
    val filteredApps: List<AppItem> = emptyList(),
    val storageInfo: StorageInfo? = null,
    val hasUsageStatsPermission: Boolean = false,
    val selectedAppForDetail: AppItem? = null,
    val dryRunResult: ArchiveDryRunResult? = null,
    val isExecutingAction: Boolean = false,
    val showOnboardingWizard: Boolean = false,
    val actionStatusMessage: String? = null,
    val pendingRestoreApp: AppItem? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ArchivistApp
    private val scanner = app.scannerRepository
    private val dao = app.database.dao()
    private val prefs = app.preferencesRepository
    val shizukuManager = app.shizukuManager

    val shizukuState: StateFlow<ShizukuState> = shizukuManager.state

    private val _isSettingsLoaded = MutableStateFlow(false)
    val isSettingsLoaded: StateFlow<Boolean> = _isSettingsLoaded.asStateFlow()

    val settings: StateFlow<ArchiveRuleSettings> = prefs.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ArchiveRuleSettings()
    )

    val activityLogs: StateFlow<List<ArchiveLogEntity>> = dao.getAllLogs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        // Observe settings changes: immediately re-evaluate eligibility and re-apply filters!
        viewModelScope.launch {
            settings.collect { currentSettings ->
                _isSettingsLoaded.value = true
                val currentStorage = _uiState.value.storageInfo ?: scanner.getDeviceStorageInfo()
                if (_uiState.value.allApps.isNotEmpty()) {
                    val updatedEligibility = scanner.evaluateEligibility(_uiState.value.allApps, currentSettings, currentStorage)
                    _uiState.value = _uiState.value.copy(allApps = updatedEligibility)
                }
                reapplyFilterAndSort(currentSettings, _uiState.value.searchQuery)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            shizukuManager.checkStatus()
            val hasUsage = scanner.hasUsageStatsPermission()
            val storage = scanner.getDeviceStorageInfo()
            val apps = scanner.scanApps(settings.value)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                allApps = apps,
                storageInfo = storage,
                hasUsageStatsPermission = hasUsage
            )
            reapplyFilterAndSort(settings.value, _uiState.value.searchQuery)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        reapplyFilterAndSort(settings.value, query)
    }

    fun setFilterOption(filter: FilterOption) {
        viewModelScope.launch {
            prefs.updateFilterOption(filter)
        }
    }

    fun setSortOption(sort: SortOption) {
        viewModelScope.launch {
            prefs.updateSortOption(sort)
        }
    }

    fun setViewMode(viewMode: ViewMode) {
        viewModelScope.launch {
            prefs.updateViewMode(viewMode)
        }
    }

    fun selectAppForDetail(appItem: AppItem?) {
        _uiState.value = _uiState.value.copy(selectedAppForDetail = appItem)
    }

    fun togglePin(appItem: AppItem) {
        viewModelScope.launch {
            val newPinned = !appItem.isPinned
            if (newPinned) {
                dao.insertPinnedApp(PinnedAppEntity(packageName = appItem.packageName, appName = appItem.appName))
                _userMessage.emit("Pinned ${appItem.appName}. It will never be auto-archived.")
            } else {
                dao.deletePinnedApp(appItem.packageName)
                _userMessage.emit("Unpinned ${appItem.appName}.")
            }

            val updatedAll = _uiState.value.allApps.map {
                if (it.packageName == appItem.packageName) {
                    it.copy(isPinned = newPinned, isEligibleForAutoArchive = if (newPinned) false else it.isEligibleForAutoArchive)
                } else it
            }
            _uiState.value = _uiState.value.copy(allApps = updatedAll)
            reapplyFilterAndSort(settings.value, _uiState.value.searchQuery)
        }
    }

    fun archiveApp(appItem: AppItem) {
        viewModelScope.launch {
            if (appItem.isSystemApp) {
                _userMessage.emit("${appItem.appName} is a system app and cannot be archived by Android.")
                return@launch
            }
            if (!appItem.isArchivable) {
                _userMessage.emit("${appItem.appName} cannot be archived (no launcher activity).")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isExecutingAction = true)
            val result = shizukuManager.archivePackage(appItem.packageName)
            val success = result.success
            val reclaimed = if (success) appItem.reclaimableBytes else 0L

            val cleanMsg = when {
                result.output.contains("System apps cannot be archived", ignoreCase = true) ->
                    "System apps cannot be archived by Android."
                result.output.contains("does not have a main activity", ignoreCase = true) ->
                    "Cannot be archived: App lacks a main launcher activity."
                result.output.contains("Failure [", ignoreCase = true) -> {
                    val inner = result.output.substringAfter(":", "").substringBefore("]").trim()
                    if (inner.isNotEmpty()) inner else result.output
                }
                else -> result.output
            }

            dao.insertLog(
                ArchiveLogEntity(
                    packageName = appItem.packageName,
                    appName = appItem.appName,
                    actionType = "MANUAL_ARCHIVE",
                    bytesReclaimed = reclaimed,
                    success = success,
                    message = if (success) "Manually archived" else cleanMsg
                )
            )

            _uiState.value = _uiState.value.copy(isExecutingAction = false)

            if (success) {
                _userMessage.emit("Successfully archived ${appItem.appName}!")
                refresh()
            } else {
                _userMessage.emit("Archive failed: $cleanMsg")
            }
        }
    }

    fun requestUnarchiveApp(appItem: AppItem) {
        if (settings.value.dontShowRestoreExplanation) {
            unarchiveApp(appItem)
        } else {
            _uiState.value = _uiState.value.copy(pendingRestoreApp = appItem)
        }
    }

    fun dismissRestoreDialog() {
        _uiState.value = _uiState.value.copy(pendingRestoreApp = null)
    }

    fun confirmRestoreAndRedirect(appItem: AppItem, dontShowAgain: Boolean) {
        _uiState.value = _uiState.value.copy(pendingRestoreApp = null)
        if (dontShowAgain) {
            viewModelScope.launch {
                prefs.updateDontShowRestoreExplanation(true)
            }
        }
        unarchiveApp(appItem)
    }

    fun unarchiveApp(appItem: AppItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExecutingAction = true)
            val result = shizukuManager.unarchivePackage(appItem.packageName)

            // Allow the system / installer service a moment to process the restoration
            kotlinx.coroutines.delay(1200)

            // Verify if the package was actually restored
            val isStillArchived = scanner.isPackageArchived(appItem.packageName)

            dao.insertLog(
                ArchiveLogEntity(
                    packageName = appItem.packageName,
                    appName = appItem.appName,
                    actionType = "MANUAL_UNARCHIVE",
                    bytesReclaimed = 0L,
                    success = !isStillArchived,
                    message = if (!isStillArchived) "Successfully restored" else result.output
                )
            )

            _uiState.value = _uiState.value.copy(isExecutingAction = false)

            if (!isStillArchived) {
                _userMessage.emit("Successfully restored ${appItem.appName}!")
                refresh()
            } else {
                // If not restored silently, open App Info where Android 15 presents the system "Restore" button
                _userMessage.emit("Restoration requested. Opening App Info to confirm restore...")
                try {
                    val settingsIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${appItem.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    getApplication<Application>().startActivity(settingsIntent)
                } catch (e: Exception) {
                    _userMessage.emit("Restore requested for ${appItem.appName}. Tap the app icon to complete restoration.")
                }
                refresh()
            }
        }
    }

    fun updateAutoArchiveEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.updateAutoArchiveEnabled(enabled)
            WorkerScheduler.scheduleOrUpdate(getApplication(), settings.value.copy(autoArchiveEnabled = enabled))
            _userMessage.emit(if (enabled) "Auto-Archiving enabled" else "Auto-Archiving disabled")
        }
    }

    fun updateInactivity(amount: Int, unit: InactivityUnit) {
        viewModelScope.launch {
            prefs.updateInactivityThreshold(amount, unit)
        }
    }

    fun updateMinSize(bytes: Long) {
        viewModelScope.launch {
            prefs.updateMinAppSizeBytes(bytes)
        }
    }

    fun updateStorageCondition(type: StorageConditionType, thresholdPercent: Int, thresholdGb: Int) {
        viewModelScope.launch {
            prefs.updateStorageCondition(type, thresholdPercent, thresholdGb)
        }
    }

    fun updateWorkerSchedule(amount: Long, unit: WorkerIntervalUnit) {
        viewModelScope.launch {
            prefs.updateWorkerSchedule(amount, unit)
            val updated = settings.value.copy(workerIntervalAmount = amount, workerIntervalUnit = unit)
            WorkerScheduler.scheduleOrUpdate(getApplication(), updated)
            _userMessage.emit("Schedule updated: Every $amount ${unit.label}")
        }
    }

    fun updateIncludeSystemApps(include: Boolean) {
        viewModelScope.launch {
            prefs.updateIncludeSystemApps(include)
            refresh()
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.updateNotificationsEnabled(enabled)
            _userMessage.emit(if (enabled) "Notifications enabled" else "Notifications disabled")
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            prefs.updateThemeMode(themeMode)
        }
    }

    fun updateAmoledBlack(enabled: Boolean) {
        viewModelScope.launch {
            prefs.updateAmoledBlack(enabled)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefs.updateOnboardingCompleted(true)
            _uiState.value = _uiState.value.copy(showOnboardingWizard = false)
        }
    }

    fun openOnboardingWizard() {
        _uiState.value = _uiState.value.copy(showOnboardingWizard = true)
    }

    fun closeOnboardingWizard() {
        _uiState.value = _uiState.value.copy(showOnboardingWizard = false)
    }

    fun runCheckNow() {
        viewModelScope.launch {
            WorkerScheduler.triggerOneTimeCheck(getApplication())
            _userMessage.emit("Immediate auto-archive check triggered")
        }
    }

    fun generateDryRun() {
        val currentSettings = settings.value
        val storage = _uiState.value.storageInfo ?: scanner.getDeviceStorageInfo()
        val storageConditionMet = when (currentSettings.storageConditionType) {
            StorageConditionType.ALWAYS -> true
            StorageConditionType.BELOW_PERCENTAGE -> storage.freePercent <= currentSettings.lowStorageThresholdPercent
            StorageConditionType.BELOW_FREE_SPACE -> storage.freeGB <= currentSettings.lowStorageThresholdGB
        }

        val conditionDescription = when (currentSettings.storageConditionType) {
            StorageConditionType.ALWAYS -> "Rule 'Always' active (storage threshold ignored)"
            StorageConditionType.BELOW_PERCENTAGE -> {
                if (storageConditionMet) {
                    "Free space (%.1f%%) is below threshold (%d%%) - Rule MATCHED".format(storage.freePercent, currentSettings.lowStorageThresholdPercent)
                } else {
                    "Free space (%.1f%%) is above threshold (%d%%) - Rule NOT MET (no auto-archive)".format(storage.freePercent, currentSettings.lowStorageThresholdPercent)
                }
            }
            StorageConditionType.BELOW_FREE_SPACE -> {
                if (storageConditionMet) {
                    "Free space (%.1f GB) is below threshold (%d GB) - Rule MATCHED".format(storage.freeGB, currentSettings.lowStorageThresholdGB)
                } else {
                    "Free space (%.1f GB) is above threshold (%d GB) - Rule NOT MET (no auto-archive)".format(storage.freeGB, currentSettings.lowStorageThresholdGB)
                }
            }
        }

        val eligible = _uiState.value.allApps.filter { it.isEligibleForAutoArchive }
        val totalBytes = eligible.sumOf { it.reclaimableBytes }

        _uiState.value = _uiState.value.copy(
            dryRunResult = ArchiveDryRunResult(
                eligibleApps = eligible,
                totalReclaimableBytes = totalBytes,
                storageConditionMet = storageConditionMet,
                conditionDescription = conditionDescription
            )
        )
    }

    fun dismissDryRun() {
        _uiState.value = _uiState.value.copy(dryRunResult = null)
    }

    fun clearLogs() {
        viewModelScope.launch {
            dao.clearAllLogs()
            _userMessage.emit("Archive logs cleared.")
        }
    }

    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    private fun reapplyFilterAndSort(currentSettings: ArchiveRuleSettings, query: String) {
        val all = _uiState.value.allApps
        val trimmed = query.trim().lowercase()

        val filtered = all.filter { app ->
            val matchesQuery = if (trimmed.isEmpty()) true else {
                app.appName.lowercase().contains(trimmed) || app.packageName.lowercase().contains(trimmed)
            }

            val matchesCategory = when (currentSettings.filterOption) {
                FilterOption.ALL -> true
                FilterOption.ACTIVE -> !app.isArchived
                FilterOption.ARCHIVED -> app.isArchived
                FilterOption.PINNED -> app.isPinned
                FilterOption.ELIGIBLE -> app.isEligibleForAutoArchive
            }

            matchesQuery && matchesCategory
        }

        val sorted = when (currentSettings.sortOption) {
            SortOption.NAME_ASC -> filtered.sortedBy { it.appName.lowercase() }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.appName.lowercase() }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.totalBytes }
            SortOption.SIZE_ASC -> filtered.sortedBy { it.totalBytes }
            SortOption.LAST_USED_ASC -> filtered.sortedBy { it.lastUsedTimestamp }
            SortOption.LAST_USED_DESC -> filtered.sortedByDescending { it.lastUsedTimestamp }
        }

        _uiState.value = _uiState.value.copy(filteredApps = sorted)
    }
}
