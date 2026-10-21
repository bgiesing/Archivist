package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.RestoreExplanationDialog
import com.example.ui.screens.AppManagerScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.ArchivistTheme
import com.example.ui.theme.dpadFocusable

enum class Screen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    APPS("Apps", Icons.Filled.Apps, Icons.Outlined.Apps),
    RULES("Rules", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    LOGS("History", Icons.Filled.History, Icons.Outlined.History)
}

@Composable
fun ArchivistMainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val isSettingsLoaded by viewModel.isSettingsLoaded.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.APPS) }
    val snackbarHostState = remember { SnackbarHostState() }
    var isSplashFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (currentScreen != Screen.APPS) {
        BackHandler {
            currentScreen = Screen.APPS
        }
    }

    ArchivistTheme(
        themeMode = settings.themeMode,
        amoledBlack = settings.amoledBlack
    ) {
        // Full screen animated Splash on every launch, holding until animation finishes & settings load
        val showSplash = !isSplashFinished || !isSettingsLoaded

        if (showSplash) {
            SplashScreen(
                onSplashFinished = { isSplashFinished = true }
            )
        } else if (!settings.onboardingCompleted || uiState.showOnboardingWizard) {
            SetupWizardScreen(viewModel = viewModel)
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWideScreen = maxWidth >= 600.dp

                    if (isWideScreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                        ) {
                            NavigationRail(
                                modifier = Modifier.fillMaxHeight(),
                                containerColor = MaterialTheme.colorScheme.surface,
                                header = null
                            ) {
                                Screen.entries.forEach { screen ->
                                    val isSelected = currentScreen == screen
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = { currentScreen = screen },
                                        icon = {
                                            NavigationItemIcon(
                                                screen = screen,
                                                isSelected = isSelected,
                                                eligibleCount = uiState.allApps.count { it.isEligibleForAutoArchive },
                                                logCount = activityLogs.size
                                            )
                                        },
                                        label = { Text(screen.title) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier
                                            .dpadFocusable(RoundedCornerShape(12.dp))
                                            .testTag("nav_rail_${screen.name.lowercase()}")
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                ScreenContent(
                                    screen = currentScreen,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp)
                                )
                            }
                        }
                    } else {
                        Scaffold(
                            bottomBar = {
                                NavigationBar(
                                    modifier = Modifier.testTag("bottom_nav_bar")
                                ) {
                                    Screen.entries.forEach { screen ->
                                        val isSelected = currentScreen == screen
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = { currentScreen = screen },
                                            icon = {
                                                NavigationItemIcon(
                                                    screen = screen,
                                                    isSelected = isSelected,
                                                    eligibleCount = uiState.allApps.count { it.isEligibleForAutoArchive },
                                                    logCount = activityLogs.size
                                                )
                                            },
                                            label = { Text(screen.title) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier
                                                .dpadFocusable(RoundedCornerShape(12.dp))
                                                .testTag("nav_item_${screen.name.lowercase()}")
                                        )
                                    }
                                }
                            },
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            modifier = Modifier.fillMaxSize()
                        ) { innerPadding ->
                            ScreenContent(
                                screen = currentScreen,
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }

                // Android 15 Restore explanation modal dialog
                uiState.pendingRestoreApp?.let { appToRestore ->
                    RestoreExplanationDialog(
                        appItem = appToRestore,
                        onDismiss = { viewModel.dismissRestoreDialog() },
                        onConfirm = { dontShowAgain ->
                            viewModel.confirmRestoreAndRedirect(appToRestore, dontShowAgain)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationItemIcon(
    screen: Screen,
    isSelected: Boolean,
    eligibleCount: Int,
    logCount: Int
) {
    val icon = if (isSelected) screen.selectedIcon else screen.unselectedIcon

    if (screen == Screen.RULES && eligibleCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text("$eligibleCount")
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = screen.title)
        }
    } else {
        Icon(imageVector = icon, contentDescription = screen.title)
    }
}

@Composable
private fun ScreenContent(
    screen: Screen,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { current ->
        when (current) {
            Screen.APPS -> AppManagerScreen(viewModel = viewModel)
            Screen.RULES -> RulesScreen(viewModel = viewModel)
            Screen.LOGS -> LogsScreen(viewModel = viewModel)
        }
    }
}
