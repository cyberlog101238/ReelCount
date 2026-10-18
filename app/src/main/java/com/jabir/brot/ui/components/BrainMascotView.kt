package com.jabir.brot.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jabir.brot.domain.BrainState

@Composable
fun BrainMascotView(
    todayTotal: Int,
    dailyTarget: Int,
    brainState: BrainState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotScale"
    )

    val stateColor = Color(brainState.colorHex)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val cardGradient = androidx.compose.runtime.remember(stateColor, surfaceVariant) {
        Brush.verticalGradient(
            listOf(
                stateColor.copy(alpha = 0.2f),
                surfaceVariant
            )
        )
    }
    val bubbleGradient = androidx.compose.runtime.remember(stateColor) {
        Brush.radialGradient(
            listOf(
                stateColor.copy(alpha = 0.8f),
                stateColor.copy(alpha = 0.3f)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mascot Animated Avatar Bubble using graphicsLayer to prevent recomposition churn
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clip(CircleShape)
                .background(bubbleGradient),
            contentAlignment = Alignment.Center
        ) {
            val emoji = when (brainState) {
                BrainState.FRESH -> "🧠✨"
                BrainState.BRUISED -> "🧠⚡"
                BrainState.FOGGY -> "🧠🌫️"
                BrainState.FRIED -> "🧠🔥"
                BrainState.MUSHY -> "🧠💧"
                BrainState.ROTTING -> "🧠☣️"
                BrainState.BRAIN_ROT -> "💀💥"
            }
            Text(
                text = emoji,
                fontSize = 42.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Count Display
        Text(
            text = "$todayTotal",
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "reels scrolled today",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Stage Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(stateColor.copy(alpha = 0.25f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(stateColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Brain State: ${brainState.stageName}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = stateColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = brainState.description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
