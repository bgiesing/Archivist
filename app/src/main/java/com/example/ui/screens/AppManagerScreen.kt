package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppItem
import com.example.data.model.ArchiveRuleSettings
import com.example.data.model.FilterOption
import com.example.data.model.StorageInfo
import com.example.data.model.ViewMode
import com.example.ui.MainViewModel
import com.example.ui.components.AppDetailBottomSheet
import com.example.ui.components.AppGridItem
import com.example.ui.components.AppListItem
import com.example.ui.components.FormatUtils
import com.example.ui.components.PermissionStatusBanner
import com.example.ui.components.SortBottomSheet
import com.example.ui.theme.ExpressiveEmerald
import com.example.ui.theme.ExpressiveEmeraldContainer
import com.example.ui.theme.ExpressiveShapes
import com.example.ui.theme.ExpressiveViolet
import com.example.ui.theme.ExpressiveVioletContainer
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.StatusArchived
import com.example.ui.theme.StatusArchivedContainer
import com.example.ui.theme.StatusArchivedOnContainer
import com.example.ui.theme.StatusEligible
import com.example.ui.theme.StatusEligibleContainer
import com.example.ui.theme.StatusEligibleOnContainer
import com.example.ui.theme.dpadFocusable
import com.example.ui.theme.expressiveBounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val shizukuState by viewModel.shizukuState.collectAsState()
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    var showSortSheet by remember { mutableStateOf(false) }

    // Dynamic Pop-In Header State:
    // When scrolling DOWN -> header collapses.
    // As soon as scrolling UP -> header immediately pops back into view!
    var isHeaderVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -12f) {
                    isHeaderVisible = false
                } else if (available.y > 12f) {
                    isHeaderVisible = true
                }
                return Offset.Zero
            }
        }
    }

    // Sort Bottom Sheet
    if (showSortSheet) {
        SortBottomSheet(
            currentSort = settings.sortOption,
            onSortSelected = { viewModel.setSortOption(it) },
            onDismiss = { showSortSheet = false }
        )
    }

    // App Detail Bottom Sheet
    uiState.selectedAppForDetail?.let { selectedApp ->
        AppDetailBottomSheet(
            app = selectedApp,
            onDismiss = { viewModel.selectAppForDetail(null) },
            onTogglePin = { viewModel.togglePin(it) },
            onArchive = { viewModel.archiveApp(it) },
            onUnarchive = { viewModel.requestUnarchiveApp(it) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Archivist",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                },
                actions = {
                    // Segmented View Mode Toggle
                    Surface(
                        shape = ExpressiveShapes.Pill,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.setViewMode(ViewMode.LIST) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(ExpressiveShapes.Pill)
                                    .background(
                                        if (settings.viewMode == ViewMode.LIST) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    )
                                    .expressiveBounceClick { viewModel.setViewMode(ViewMode.LIST) }
                                    .testTag("view_mode_list")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewList,
                                    contentDescription = "List view",
                                    tint = if (settings.viewMode == ViewMode.LIST) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.setViewMode(ViewMode.GRID) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(ExpressiveShapes.Pill)
                                    .background(
                                        if (settings.viewMode == ViewMode.GRID) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    )
                                    .expressiveBounceClick { viewModel.setViewMode(ViewMode.GRID) }
                                    .testTag("view_mode_grid")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Grid view",
                                    tint = if (settings.viewMode == ViewMode.GRID) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Sort button
                    IconButton(
                        onClick = { showSortSheet = true },
                        modifier = Modifier
                            .size(38.dp)
                            .expressiveBounceClick { showSortSheet = true }
                            .testTag("sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort apps",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Refresh button
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier
                            .size(38.dp)
                            .expressiveBounceClick { viewModel.refresh() }
                            .testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh list",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Animated Pop-In Search & Filter Header:
            AnimatedVisibility(
                visible = isHeaderVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchBarSection(
                        searchQuery = uiState.searchQuery,
                        onSearchChange = { viewModel.setSearchQuery(it) }
                    )

                    FilterChipsSection(
                        allApps = uiState.allApps,
                        selectedFilter = settings.filterOption,
                        onSelectFilter = { viewModel.setFilterOption(it) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Scanning applications...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                AnimatedContent(
                    targetState = settings.viewMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.fillMaxSize(),
                    label = "ViewModeAnimation"
                ) { mode ->
                    when (mode) {
                        ViewMode.LIST -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isWideScreen) {
                                    // 2-Column Side-by-Side Storage & Status Panels on Tablets & Foldables
                                    item(key = "header_panels_side_by_side") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Max)
                                                .padding(bottom = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                uiState.storageInfo?.let { storage ->
                                                    StorageOverviewSection(
                                                        storage = storage,
                                                        allApps = uiState.allApps,
                                                        settings = settings,
                                                        onFilterSelected = { viewModel.setFilterOption(it) },
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                PermissionStatusBanner(
                                                    shizukuState = shizukuState,
                                                    hasUsageStats = uiState.hasUsageStatsPermission,
                                                    onRequestShizuku = { viewModel.requestShizukuPermission() },
                                                    onRefresh = { viewModel.refresh() },
                                                    forceExpanded = true,
                                                    showExpandToggle = false,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Stacked layout for compact phone screens
                                    uiState.storageInfo?.let { storage ->
                                        item(key = "header_storage") {
                                            StorageOverviewSection(
                                                storage = storage,
                                                allApps = uiState.allApps,
                                                settings = settings,
                                                onFilterSelected = { viewModel.setFilterOption(it) }
                                            )
                                        }
                                    }

                                    item(key = "header_permissions") {
                                        PermissionStatusBanner(
                                            shizukuState = shizukuState,
                                            hasUsageStats = uiState.hasUsageStatsPermission,
                                            onRequestShizuku = { viewModel.requestShizukuPermission() },
                                            onRefresh = { viewModel.refresh() }
                                        )
                                    }
                                }

                                // Empty state
                                if (uiState.filteredApps.isEmpty()) {
                                    item(key = "empty_state") {
                                        EmptyStateSection(
                                            searchQuery = uiState.searchQuery,
                                            filterName = settings.filterOption.displayName
                                        )
                                    }
                                } else {
                                    // App List Items
                                    items(uiState.filteredApps, key = { it.packageName }) { app ->
                                        AppListItem(
                                            app = app,
                                            onSelectApp = { viewModel.selectAppForDetail(it) },
                                            onTogglePin = { viewModel.togglePin(it) },
                                            onArchive = { viewModel.archiveApp(it) },
                                            onUnarchive = { viewModel.requestUnarchiveApp(it) }
                                        )
                                    }
                                }
                            }
                        }
                        ViewMode.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 165.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isWideScreen) {
                                    // 2-Column Side-by-Side Storage & Status Panels on Tablets & Foldables
                                    item(span = { GridItemSpan(maxLineSpan) }, key = "header_panels_side_by_side") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Max)
                                                .padding(bottom = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                uiState.storageInfo?.let { storage ->
                                                    StorageOverviewSection(
                                                        storage = storage,
                                                        allApps = uiState.allApps,
                                                        settings = settings,
                                                        onFilterSelected = { viewModel.setFilterOption(it) }
                                                    )
                                                }
                                            }
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                PermissionStatusBanner(
                                                    shizukuState = shizukuState,
                                                    hasUsageStats = uiState.hasUsageStatsPermission,
                                                    onRequestShizuku = { viewModel.requestShizukuPermission() },
                                                    onRefresh = { viewModel.refresh() },
                                                    forceExpanded = true,
                                                    showExpandToggle = false
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    uiState.storageInfo?.let { storage ->
                                        item(span = { GridItemSpan(maxLineSpan) }, key = "header_storage") {
                                            StorageOverviewSection(
                                                storage = storage,
                                                allApps = uiState.allApps,
                                                settings = settings,
                                                onFilterSelected = { viewModel.setFilterOption(it) }
                                            )
                                        }
                                    }

                                    item(span = { GridItemSpan(maxLineSpan) }, key = "header_permissions") {
                                        PermissionStatusBanner(
                                            shizukuState = shizukuState,
                                            hasUsageStats = uiState.hasUsageStatsPermission,
                                            onRequestShizuku = { viewModel.requestShizukuPermission() },
                                            onRefresh = { viewModel.refresh() }
                                        )
                                    }
                                }

                                if (uiState.filteredApps.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }, key = "empty_state") {
                                        EmptyStateSection(
                                            searchQuery = uiState.searchQuery,
                                            filterName = settings.filterOption.displayName
                                        )
                                    }
                                } else {
                                    items(uiState.filteredApps, key = { it.packageName }) { app ->
                                        AppGridItem(
                                            app = app,
                                            onSelectApp = { viewModel.selectAppForDetail(it) },
                                            onTogglePin = { viewModel.togglePin(it) },
                                            onArchive = { viewModel.archiveApp(it) },
                                            onUnarchive = { viewModel.requestUnarchiveApp(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBarSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_input"),
        placeholder = {
            Text(
                text = "Search apps or packages...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = ExpressiveShapes.Pill,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun FilterChipsSection(
    allApps: List<AppItem>,
    selectedFilter: FilterOption,
    onSelectFilter: (FilterOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterOption.entries.forEach { option ->
            val isSelected = option == selectedFilter
            val count = when (option) {
                FilterOption.ALL -> allApps.size
                FilterOption.ACTIVE -> allApps.count { !it.isArchived }
                FilterOption.ARCHIVED -> allApps.count { it.isArchived }
                FilterOption.PINNED -> allApps.count { it.isPinned }
                FilterOption.ELIGIBLE -> allApps.count { it.isEligibleForAutoArchive }
            }

            FilterChip(
                selected = isSelected,
                onClick = { onSelectFilter(option) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Surface(
                            shape = ExpressiveShapes.Pill,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                },
                shape = ExpressiveShapes.Pill,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.expressiveBounceClick { onSelectFilter(option) }
            )
        }
    }
}

/**
 * Material 3 Expressive Storage Overview Hero Card:
 * Inspired by official Google Material 3 Expressive showcases:
 * - Chunky rounded hero shape
 * - Space Grotesk hero typography
 * - Scalloped 12-lobed Rosette badge
 * - Segmented expressive storage capsule
 * - Interactive quick-filter pill badges
 */
@Composable
private fun StorageOverviewSection(
    storage: StorageInfo,
    allApps: List<AppItem>,
    settings: ArchiveRuleSettings,
    onFilterSelected: (FilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val archivedApps = allApps.filter { it.isArchived }
    val eligibleApps = allApps.filter { it.isEligibleForAutoArchive }
    val totalReclaimable = eligibleApps.sumOf { it.reclaimableBytes }

    Card(
        shape = ExpressiveShapes.HeroCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Big Expressive Headline & Scalloped Rosette
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DEVICE STORAGE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${String.format("%.0f", storage.freePercent)}%",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FREE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpressiveEmerald,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", storage.freeGB)} GB of ${String.format("%.0f", storage.totalGB)} GB space available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Scalloped 12-pointed Rosette shape badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(ExpressiveShapes.Rosette)
                        .background(ExpressiveVioletContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Storage status",
                        tint = ExpressiveViolet,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-segment Expressive Storage Capsule
            val usedFraction = (storage.usedPercent / 100f).coerceIn(0.01f, 0.99f)
            val freeFraction = 1f - usedFraction

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(ExpressiveShapes.Pill)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(usedFraction)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Box(
                        modifier = Modifier
                            .weight(freeFraction)
                            .fillMaxSize()
                            .background(ExpressiveEmerald.copy(alpha = 0.6f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Expressive Quick-Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Archived Pill
                Surface(
                    shape = ExpressiveShapes.Pill,
                    color = StatusArchivedContainer,
                    modifier = Modifier
                        .weight(1f)
                        .expressiveBounceClick { onFilterSelected(FilterOption.ARCHIVED) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = StatusArchivedOnContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${archivedApps.size} Archived",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusArchivedOnContainer
                        )
                    }
                }

                // Eligible Reclaimable Pill
                Surface(
                    shape = ExpressiveShapes.Pill,
                    color = StatusEligibleContainer,
                    modifier = Modifier
                        .weight(1.3f)
                        .expressiveBounceClick { onFilterSelected(FilterOption.ELIGIBLE) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = StatusEligibleOnContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${FormatUtils.formatBytes(totalReclaimable)} Reclaimable",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusEligibleOnContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateSection(searchQuery: String, filterName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(ExpressiveShapes.Rosette)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No matching applications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) "Try a different search query" else "No apps found for '$filterName'",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
