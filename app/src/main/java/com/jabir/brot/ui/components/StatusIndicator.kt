package com.jabir.brot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jabir.brot.domain.model.TrackingStatus

@Composable
fun StatusIndicator(
    status: TrackingStatus,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (status) {
        is TrackingStatus.Active -> "Tracking Active (${status.platform.displayName})" to Color(0xFF43E97B)
        is TrackingStatus.PermissionMissing -> "Permission Missing (${status.permission})" to Color(0xFFFFA000)
        is TrackingStatus.UnsupportedScreen -> "Ready (Waiting for Reels/Shorts)" to Color(0xFF6C63FF)
        is TrackingStatus.DetectionUncertain -> "Detection Uncertain" to Color(0xFFFFB74D)
        is TrackingStatus.TrackingPaused -> "Tracking Paused" to Color(0xFF9E9E9E)
        is TrackingStatus.ServiceNotRunning -> "Accessibility Service Disabled" to Color(0xFFFF5252)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(statusColor.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = statusText,
            color = statusColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
