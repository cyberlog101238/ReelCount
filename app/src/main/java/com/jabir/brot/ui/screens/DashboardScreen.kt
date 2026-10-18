package com.jabir.brot.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.jabir.brot.ReelCountApp
import com.jabir.brot.domain.model.Platform
import com.jabir.brot.ui.components.BrainMascotView
import com.jabir.brot.ui.components.PlatformCard
import com.jabir.brot.ui.components.StatusIndicator
import com.jabir.brot.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val calculator = ReelCountApp.instance.statisticsCalculator
    val userPrefs = ReelCountApp.instance.userPreferencesRepository
    val pureDark by userPrefs.pureDarkTheme.collectAsState(initial = true)
    val coroutineScope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val animatedTotal by animateIntAsState(
        targetValue = state.todayTotal,
        animationSpec = tween(400),
        label = "totalCountAnim"
    )
    val animatedIg by animateIntAsState(
        targetValue = state.instagramCount,
        animationSpec = tween(400),
        label = "igCountAnim"
    )
    val animatedFb by animateIntAsState(
        targetValue = state.facebookCount,
        animationSpec = tween(400),
        label = "fbCountAnim"
    )
    val animatedYt by animateIntAsState(
        targetValue = state.youtubeCount,
        animationSpec = tween(400),
        label = "ytCountAnim"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ReelCount",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp
                        )
                        if (state.isTrackingPaused) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "PAUSED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Quick Pure OLED Dark Theme button
                    IconButton(onClick = {
                        coroutineScope.launch {
                            userPrefs.setPureDarkTheme(!pureDark)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Toggle OLED Pure Dark",
                            tint = if (pureDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    // Pause / Resume Tracking button
                    IconButton(onClick = { viewModel.toggleTrackingPause() }) {
                        Icon(
                            imageVector = if (state.isTrackingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (state.isTrackingPaused) "Resume Tracking" else "Pause Tracking",
                            tint = if (state.isTrackingPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Live Status Indicator
            StatusIndicator(
                status = state.trackingStatus,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Permissions Alert Cards
            if (!state.isAccessibilityEnabled) {
                PermissionAlertCard(
                    title = "Accessibility Permission Required",
                    description = "Required to detect when Instagram Reels, Facebook Reels, or YouTube Shorts are on screen.",
                    actionLabel = "Enable Service",
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!state.isOverlayPermissionGranted) {
                PermissionAlertCard(
                    title = "Floating Counter Permission",
                    description = "Required to display the compact live reel counter floating over videos while watching.",
                    actionLabel = "Grant Permission",
                    onAction = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!state.isNotificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionAlertCard(
                    title = "Notifications Permission",
                    description = "Required for daily reel summaries and goal limit alarms.",
                    actionLabel = "Enable Alerts",
                    onAction = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.isAccessibilityEnabled && state.isOverlayPermissionGranted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF43E97B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tracking engine active & watching reel feeds",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Brain Mascot Hero View with animated counter
            BrainMascotView(
                todayTotal = animatedTotal,
                dailyTarget = state.dailyTarget,
                brainState = state.brainState
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Metrics Grid (Time Spent, Reels/Hr, Dwell Time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Time Spent",
                    value = calculator.formatDuration(state.durationMs),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Reels / Hr",
                    value = "%.1f".format(calculator.calculateReelsPerHour(state.todayTotal, state.durationMs)),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Avg Dwell",
                    value = "%.1fs".format(calculator.calculateSecondsPerReel(state.todayTotal, state.durationMs)),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Platform Breakdown",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            PlatformCard(
                platform = Platform.INSTAGRAM,
                count = animatedIg,
                target = (state.dailyTarget * 0.4).toInt().coerceAtLeast(1)
            )

            Spacer(modifier = Modifier.height(10.dp))

            PlatformCard(
                platform = Platform.FACEBOOK,
                count = animatedFb,
                target = (state.dailyTarget * 0.3).toInt().coerceAtLeast(1)
            )

            Spacer(modifier = Modifier.height(10.dp))

            PlatformCard(
                platform = Platform.YOUTUBE,
                count = animatedYt,
                target = (state.dailyTarget * 0.3).toInt().coerceAtLeast(1)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Add Widget to Home Screen Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Home Screen Widget",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Keep track of reels right from your launcher",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val pinned = com.jabir.brot.ui.widget.ReelCountAppWidgetProvider.requestPinWidget(context)
                            if (!pinned) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Long-press your home screen and select Widgets > ReelCount to add.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PermissionAlertCard(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
