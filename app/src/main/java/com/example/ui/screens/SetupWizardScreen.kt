@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.ui.theme.ExpressiveEmerald
import com.example.ui.theme.ExpressiveShapes
import com.example.ui.theme.ExpressiveViolet
import com.example.ui.theme.dpadFocusable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.InactivityUnit
import com.example.data.model.ShizukuState
import com.example.ui.MainViewModel
import com.example.ui.theme.StatusArchived
import com.example.ui.theme.StatusEligible

@Composable
fun SetupWizardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    val shizukuState by viewModel.shizukuState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var hasNotificationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        viewModel.updateNotificationsEnabled(granted)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isWideScreen) 32.dp else 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Animated Step Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0 until totalSteps) {
                    val isActive = i <= currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            // Animated Content for Each Wizard Step (Wide 2-Column or Compact Vertical)
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                label = "WizardStepAnimation"
            ) { step ->
                if (isWideScreen) {
                    TwoColumnWizardStep(
                        step = step,
                        viewModel = viewModel,
                        shizukuState = shizukuState,
                        hasUsageStats = uiState.hasUsageStatsPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onNotificationRequest = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                } else {
                    when (step) {
                        0 -> AnimatedWelcomeStep()
                        1 -> AnimatedShizukuStep(
                            shizukuState = shizukuState,
                            onRequestPermission = { viewModel.requestShizukuPermission() },
                            onRecheck = { viewModel.refresh() }
                        )
                        2 -> AnimatedUsageStep(
                            hasAccess = uiState.hasUsageStatsPermission,
                            onGrant = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            },
                            onRecheck = { viewModel.refresh() }
                        )
                        3 -> AnimatedNotificationStep(
                            hasPermission = hasNotificationPermission,
                            onRequest = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        )
                        4 -> AnimatedRulesConfigurationStep(
                            settings = settings,
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Bottom Navigation Buttons safely above tablet taskbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    TextButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.dpadFocusable(RoundedCornerShape(12.dp))
                    ) {
                        Text("Back", fontSize = 16.sp)
                    }
                } else {
                    TextButton(
                        onClick = { viewModel.completeOnboarding() },
                        modifier = Modifier.dpadFocusable(RoundedCornerShape(12.dp))
                    ) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (currentStep < totalSteps - 1) {
                    Button(
                        onClick = { currentStep++ },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .dpadFocusable(RoundedCornerShape(16.dp))
                            .testTag("wizard_next_button")
                    ) {
                        Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Button(
                        onClick = { viewModel.completeOnboarding() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .dpadFocusable(RoundedCornerShape(16.dp))
                            .testTag("wizard_finish_button")
                    ) {
                        Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Launch Archivist", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeroBadge(
    icon: ImageVector,
    tint: Color,
    badgeLabel: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = ExpressiveShapes.Pill,
            color = tint.copy(alpha = 0.15f),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = badgeLabel,
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(ExpressiveShapes.Rosette)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
private fun TwoColumnWizardStep(
    step: Int,
    viewModel: MainViewModel,
    shizukuState: ShizukuState,
    hasUsageStats: Boolean,
    hasNotificationPermission: Boolean,
    onNotificationRequest: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Hero Visual & Branding / Step Intro
        Card(
            shape = ExpressiveShapes.HeroCard,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (step) {
                    0 -> StepHeroBadge(icon = Icons.Default.Archive, tint = MaterialTheme.colorScheme.primary, badgeLabel = "WELCOME")
                    1 -> StepHeroBadge(icon = Icons.Default.Key, tint = ExpressiveViolet, badgeLabel = "STEP 1 OF 4")
                    2 -> StepHeroBadge(icon = Icons.Default.QueryStats, tint = ExpressiveEmerald, badgeLabel = "STEP 2 OF 4")
                    3 -> StepHeroBadge(icon = Icons.Default.Notifications, tint = Color(0xFFF59E0B), badgeLabel = "STEP 3 OF 4")
                    4 -> StepHeroBadge(icon = Icons.Default.Tune, tint = MaterialTheme.colorScheme.primary, badgeLabel = "STEP 4 OF 4")
                }

                Spacer(modifier = Modifier.height(20.dp))

                val (title, subtitle) = when (step) {
                    0 -> "Welcome to Archivist" to "Reclaim gigabytes of space while keeping your app accounts, saved files, and drawer icons intact."
                    1 -> "Privileged Execution" to "Android 15's native package archiver requires elevated privileges. Shizuku provides this securely without needing full device root."
                    2 -> "Usage Tracking Access" to "Archivist reads accurate app activity to intelligently recommend apps you haven't opened in months or years."
                    3 -> "Archive Notifications" to "Receive a friendly summary whenever the background archiver successfully frees up storage."
                    else -> "Smart Archive Defaults" to "Configure default inactivity rules and storage conditions for automated maintenance."
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Right Column: Interactive Step Controls
        Card(
            shape = ExpressiveShapes.Card,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FunFeatureCard(
                                icon = Icons.Default.AutoAwesome,
                                title = "Zero Data Loss",
                                subtitle = "Android 15 native archiving keeps logins and game saves safely preserved.",
                                tint = Color(0xFF10B981)
                            )
                            FunFeatureCard(
                                icon = Icons.Default.Archive,
                                title = "1-Tap Restoration",
                                subtitle = "Archived apps stay in your drawer with a cloud badge and restore on demand.",
                                tint = Color(0xFFF59E0B)
                            )
                            FunFeatureCard(
                                icon = Icons.Default.Tune,
                                title = "Smart Background Worker",
                                subtitle = "Cleans old unused apps automatically only when your device needs space.",
                                tint = Color(0xFF8B5CF6)
                            )
                        }
                    }
                    1 -> {
                        AnimatedShizukuStep(
                            shizukuState = shizukuState,
                            onRequestPermission = { viewModel.requestShizukuPermission() },
                            onRecheck = { viewModel.refresh() },
                            showTitleAndGraphic = false
                        )
                    }
                    2 -> {
                        AnimatedUsageStep(
                            hasAccess = hasUsageStats,
                            onGrant = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            },
                            onRecheck = { viewModel.refresh() },
                            showTitleAndGraphic = false
                        )
                    }
                    3 -> {
                        AnimatedNotificationStep(
                            hasPermission = hasNotificationPermission,
                            onRequest = onNotificationRequest,
                            showTitleAndGraphic = false
                        )
                    }
                    4 -> {
                        AnimatedRulesConfigurationStep(
                            settings = settings,
                            viewModel = viewModel,
                            showTitleAndGraphic = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedWelcomeStep() {
    val infiniteTransition = rememberInfiniteTransition(label = "VaultPulse")

    // Pulsing rotation of glowing rings
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VaultScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Vault Graphic
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scalePulse),
            contentAlignment = Alignment.Center
        ) {
            // Revolving neon gradient aura
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF38BDF8),
                            Color(0xFF818CF8),
                            Color(0xFFC084FC),
                            Color(0xFF38BDF8)
                        )
                    ),
                    radius = size.minDimension / 2,
                    center = center,
                    style = Stroke(width = 6.dp.toPx())
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(108.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Archivist",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Reclaim gigabytes of space while keeping your app accounts, saved files, and drawer icons intact.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        FunFeatureCard(
            icon = Icons.Default.AutoAwesome,
            title = "Zero Data Loss",
            subtitle = "Android 15 native archiving keeps logins and game saves safely preserved.",
            tint = Color(0xFF10B981)
        )

        Spacer(modifier = Modifier.height(10.dp))

        FunFeatureCard(
            icon = Icons.Default.Archive,
            title = "1-Tap Restoration",
            subtitle = "Archived apps stay in your drawer with a cloud badge and restore on demand.",
            tint = Color(0xFFF59E0B)
        )

        Spacer(modifier = Modifier.height(10.dp))

        FunFeatureCard(
            icon = Icons.Default.Tune,
            title = "Smart Background Worker",
            subtitle = "Cleans old unused apps automatically only when your device needs space.",
            tint = Color(0xFF8B5CF6)
        )
    }
}

@Composable
private fun AnimatedShizukuStep(
    shizukuState: ShizukuState,
    onRequestPermission: () -> Unit,
    onRecheck: () -> Unit,
    showTitleAndGraphic: Boolean = true
) {
    val context = LocalContext.current
    val isAuthorized = shizukuState is ShizukuState.Authorized

    val infiniteTransition = rememberInfiniteTransition(label = "KeyFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showTitleAndGraphic) {
            // Floating key graphic
            Surface(
                shape = CircleShape,
                color = if (isAuthorized) StatusEligible.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer { translationY = floatOffset }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isAuthorized) Icons.Default.CheckCircle else Icons.Default.Key,
                        contentDescription = null,
                        tint = if (isAuthorized) StatusEligible else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Privileged Execution",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Android 15's native package archiver requires elevated privileges. Shizuku provides this securely without needing full device root.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (shizukuState) {
                        is ShizukuState.Authorized -> "Shizuku Authorized (v${shizukuState.version})"
                        is ShizukuState.RunningUnauthorized -> "Service Running • Grant Authorization"
                        is ShizukuState.Dead -> "Shizuku Service Stopped"
                        is ShizukuState.NotInstalled -> "Shizuku App Not Installed"
                        is ShizukuState.Checking -> "Checking Status..."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isAuthorized) StatusEligible else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                when (shizukuState) {
                    is ShizukuState.RunningUnauthorized -> {
                        Button(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Shizuku Permission")
                        }
                    }
                    is ShizukuState.NotInstalled, is ShizukuState.Dead -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val launch = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                        ?: context.packageManager.getLaunchIntentForPackage("dev.rikka.shizuku")
                                    if (launch != null) context.startActivity(launch)
                                    else {
                                        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))
                                        context.startActivity(marketIntent)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open Shizuku")
                            }

                            FilledTonalButton(
                                onClick = onRecheck,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Recheck")
                            }
                        }
                    }
                    is ShizukuState.Authorized -> {
                        Text(
                            text = "Connected and ready to archive!",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusEligible,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun AnimatedUsageStep(
    hasAccess: Boolean,
    onGrant: () -> Unit,
    onRecheck: () -> Unit,
    showTitleAndGraphic: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showTitleAndGraphic) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (hasAccess) StatusEligible.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (hasAccess) Icons.Default.CheckCircle else Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = if (hasAccess) StatusEligible else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer { rotationZ = if (!hasAccess) sweepAngle else 0f }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Inactivity Radar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Archivist analyzes exact app usage dates so that frequently used apps are never touched.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hasAccess) "Usage Access Active" else "Grant Usage Stats Access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hasAccess) StatusEligible else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (!hasAccess) {
                    Button(
                        onClick = onGrant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Settings & Allow")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRecheck) {
                        Text("Recheck Status")
                    }
                } else {
                    Text(
                        text = "App activity tracking is enabled and accurate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusEligible,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedNotificationStep(
    hasPermission: Boolean,
    onRequest: () -> Unit,
    showTitleAndGraphic: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BellSwing")
    val swingAngle by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BellAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showTitleAndGraphic) {
            Surface(
                shape = CircleShape,
                color = if (hasPermission) StatusEligible.copy(alpha = 0.2f) else MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer { rotationZ = if (!hasPermission) swingAngle else 0f }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (hasPermission) StatusEligible else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Cleanup Alerts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Receive a friendly notification whenever the background archiver successfully frees up storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hasPermission) "Notifications Enabled" else "Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hasPermission) StatusEligible else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (!hasPermission) {
                    Button(
                        onClick = onRequest,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow Notifications")
                    }
                } else {
                    Text(
                        text = "You'll be notified whenever storage is reclaimed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusEligible,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedRulesConfigurationStep(
    settings: com.example.data.model.ArchiveRuleSettings,
    viewModel: MainViewModel,
    showTitleAndGraphic: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showTitleAndGraphic) {
            Text(
                text = "Tailor Your Preferences",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pick baseline thresholds to match how you use your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Inactivity presets
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Inactivity Duration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        Triple(1, InactivityUnit.MONTHS, "1 Month"),
                        Triple(3, InactivityUnit.MONTHS, "3 Months"),
                        Triple(6, InactivityUnit.MONTHS, "6 Months"),
                        Triple(1, InactivityUnit.YEARS, "1 Year")
                    )
                    presets.forEach { (amt, unit, label) ->
                        FilterChip(
                            selected = settings.inactivityAmount == amt && settings.inactivityUnit == unit,
                            onClick = { viewModel.updateInactivity(amt, unit) },
                            label = { Text(label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Minimum App Size (EXACTLY ALIGNED with RulesScreen presets including 250 MB!)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Minimum App Size", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sizePresets = listOf(
                        50L * 1024 * 1024 to "50 MB",
                        100L * 1024 * 1024 to "100 MB",
                        250L * 1024 * 1024 to "250 MB",
                        500L * 1024 * 1024 to "500 MB",
                        1024L * 1024 * 1024 to "1 GB",
                        5L * 1024 * 1024 * 1024 to "5 GB"
                    )
                    sizePresets.forEach { (bytes, label) ->
                        FilterChip(
                            selected = settings.minAppSizeBytes == bytes,
                            onClick = { viewModel.updateMinSize(bytes) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Auto-Archiving Master
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (settings.autoArchiveEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable Auto-Archiving", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Periodic background checks via WorkManager", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.Switch(
                    checked = settings.autoArchiveEnabled,
                    onCheckedChange = { viewModel.updateAutoArchiveEnabled(it) }
                )
            }
        }
    }
}

@Composable
private fun FunFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.18f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
