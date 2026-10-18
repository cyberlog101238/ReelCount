package com.jabir.brot.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jabir.brot.ReelCountApp
import com.jabir.brot.domain.GoalAlertManager
import com.jabir.brot.domain.model.Platform
import com.jabir.brot.service.DailyRecapReceiver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coordinator = ReelCountApp.instance.trackingCoordinator
    val diagnostics by coordinator.liveDiagnostics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Inspector", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = {
                        coordinator.resetDiagnostics()
                        Toast.makeText(context, "Diagnostics Reset", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Diagnostics")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Contributor Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Source Contributor Engine",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Real-time telemetry, heuristic state verification, and simulated test triggers to assist open-source developers contributing to ReelCount.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Real-time Pipeline Telemetry
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Live Accessibility Telemetry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        TelemetryRow("Accessibility Service", if (diagnostics.isServiceRunning) "RUNNING" else "STOPPED", if (diagnostics.isServiceRunning) Color(0xFF43E97B) else MaterialTheme.colorScheme.error)
                        TelemetryRow("Active App Package", diagnostics.lastPackage)
                        TelemetryRow("Window / Activity Class", diagnostics.lastActivity)
                        TelemetryRow("Detected Platform", diagnostics.lastPlatform)
                        TelemetryRow("Detector State", diagnostics.lastState, MaterialTheme.colorScheme.primary)
                        TelemetryRow("Last Fingerprint Hash", diagnostics.lastFingerprintHash)
                        TelemetryRow("Transition Confidence", "%.2f".format(diagnostics.lastConfidence))
                        TelemetryRow("Duplicates Suppressed", "${diagnostics.duplicateSuppressedCount}")
                        TelemetryRow("Total Processed Snapshots", "${diagnostics.totalProcessedSnapshots}")
                    }
                }
            }

            // Test Simulation Sandbox
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Event Simulation Sandbox",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Test app counters, alarm sounds, and notifications without needing third-party apps open.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    coordinator.simulateReelEvent(Platform.INSTAGRAM)
                                    Toast.makeText(context, "+1 Instagram Reel", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C))
                            ) {
                                Text("+1 IG Reel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    coordinator.simulateReelEvent(Platform.FACEBOOK)
                                    Toast.makeText(context, "+1 Facebook Reel", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                            ) {
                                Text("+1 FB Reel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    coordinator.simulateReelEvent(Platform.YOUTUBE)
                                    Toast.makeText(context, "+1 YouTube Short", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
                            ) {
                                Text("+1 YT Short", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                GoalAlertManager.triggerUrgentAlarm(context, 50, 50)
                                Toast.makeText(context, "Goal Alert Alarm Fired", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Goal Limit Alarm & Vibration", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                DailyRecapReceiver.sendRecapNotification(context, 42, 28)
                                Toast.makeText(context, "Daily Summary Notification Sent", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Daily Recap Notification", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                com.jabir.brot.ui.widget.ReelCountAppWidgetProvider.refreshWidgetData(context)
                                Toast.makeText(context, "Home Screen Widget Refreshed", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refresh Home Screen Widget", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Architecture Guide for GitHub Contributors
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Architecture & Technical Documentation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        DocBullet("Heuristic Detection Pipeline", "ReelCount uses an event-driven Android AccessibilityService that captures ScreenSnapshots containing view IDs, content descriptions, and container geometry. Bounded node traversal ensures zero UI lag.")
                        Spacer(modifier = Modifier.height(8.dp))
                        DocBullet("Meta Litho UI Traversal", "Facebook and Instagram use Litho / Bloks frameworks where View IDs are stripped. Detection evaluates activity classes, accessibility virtual labels ('Reel by ...', 'Browse more reels'), and full-screen vertical swipe containers.")
                        Spacer(modifier = Modifier.height(8.dp))
                        DocBullet("Adding New Platforms", "To add TikTok or Snapchat, implement PlatformDetector interface, register package name in Platform enum, and create a specialized transition detector.")
                        Spacer(modifier = Modifier.height(8.dp))
                        DocBullet("Privacy & Security Guarantee", "ReelCount is 100% offline. Zero internet permissions (android.permission.INTERNET is omitted), zero tracking analytics SDKs, and zero PACKAGE_USAGE_STATS flags.")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
    }
}

@Composable
private fun DocBullet(title: String, description: String) {
    Column {
        Text(
            text = "• $title",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
    }
}
