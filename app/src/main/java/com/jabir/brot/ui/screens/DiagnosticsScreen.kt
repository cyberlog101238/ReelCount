package com.jabir.brot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jabir.brot.domain.LoggedEvent
import com.jabir.brot.ui.viewmodel.DiagnosticsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detection Diagnostics", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.clearDiagnostics() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Diagnostics")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Live Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagMetric("Received", "${summary.totalEventsReceived}", Modifier.weight(1f))
                DiagMetric("Committed", "${summary.confirmedCount}", Modifier.weight(1f))
                DiagMetric("Suppressed", "${summary.duplicateSuppressedCount}", Modifier.weight(1f))
                DiagMetric("Uncertain", "${summary.uncertainCount}", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Live Detection Log (${summary.recentEvents.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Spacer(modifier = Modifier.height(8.dp))

            if (summary.recentEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No accessibility events processed yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(summary.recentEvents) { logged ->
                        EventLogCard(logged)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EventLogCard(logged: LoggedEvent) {
    val event = logged.event
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    val timeStr = sdf.format(Date(event.timestamp))

    val (statusColor, containerBg) = when (logged.statusText) {
        "COMMITTED" -> Color(0xFF43E97B) to Color(0xFF1E3A2B)
        "DUPLICATE SUPPRESSED" -> Color(0xFFFFA726) to Color(0xFF3E2D1A)
        "UNCERTAIN" -> Color(0xFFFF5252) to Color(0xFF3E1B1B)
        else -> Color(0xFF9E9E9E) to MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(event.platform.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(timeStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(event.evidence, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Confidence: ${(event.confidence * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = containerBg
                ) {
                    Text(
                        text = " ${logged.statusText} ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
