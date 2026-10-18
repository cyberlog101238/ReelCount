package com.jabir.brot.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jabir.brot.ReelCountApp
import com.jabir.brot.ui.viewmodel.DailyBarItem
import com.jabir.brot.ui.viewmodel.HourlyItem
import com.jabir.brot.ui.viewmodel.StatisticsViewModel

// Bar Chart Gradient Palette matching user reference
private val RetroGoldTop = Color(0xFFFDE68A)
private val RetroAmberMid = Color(0xFFFBBF24)
private val RetroOrangeBottom = Color(0xFFF97316)
private val RetroBaselinePill = Color(0xFFF59E0B)

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val calculator = ReelCountApp.instance.statisticsCalculator
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 1. PROGRESS Header
        Text(
            text = "PROGRESS",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = colors.primary,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Segmented Pill ("Daily" | "Weekly")
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.surfaceVariant)
                .padding(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Daily Option
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (state.selectedTab == 0) colors.primaryContainer else Color.Transparent)
                        .clickable { viewModel.setSelectedTab(0) }
                        .padding(horizontal = 28.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Daily",
                        fontSize = 15.sp,
                        fontWeight = if (state.selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (state.selectedTab == 0) Color.White else colors.onSurface.copy(alpha = 0.55f)
                    )
                }

                // Weekly Option
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (state.selectedTab == 1) colors.primaryContainer else Color.Transparent)
                        .clickable { viewModel.setSelectedTab(1) }
                        .padding(horizontal = 28.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Weekly",
                        fontSize = 15.sp,
                        fontWeight = if (state.selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (state.selectedTab == 1) Color.White else colors.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 3. Date Navigator (<  Today  >)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.navigatePrevious() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "〈",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            }

            Spacer(modifier = Modifier.width(28.dp))

            Text(
                text = state.dateDisplayLabel,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            Spacer(modifier = Modifier.width(28.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(enabled = state.dayOffset > 0) {
                        viewModel.navigateNext()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "〉",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.dayOffset > 0) colors.onSurface else colors.onSurface.copy(alpha = 0.25f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Main Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
            ) {
                if (state.selectedTab == 0) {
                    ProgressHourlyGraph(
                        items = state.hourlyItems,
                        mutedColor = colors.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    ProgressWeeklyGraph(
                        items = state.weeklyItems,
                        mutedColor = colors.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Bottom Metric Card (Total Reels | Time spent)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp, horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Total Reels
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${state.displayTotalReels}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Reels",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface.copy(alpha = 0.55f)
                    )
                }

                // Vertical Separator
                Box(
                    modifier = Modifier
                        .height(46.dp)
                        .width(1.dp)
                        .background(colors.surfaceVariant)
                )

                // Right Column: Time Spent
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = calculator.formatDurationCompact(state.displayDurationMs),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Time spent",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun ProgressHourlyGraph(
    items: List<HourlyItem>,
    mutedColor: Color
) {
    val maxRaw = items.maxOfOrNull { it.count } ?: 0
    val maxScale = when {
        maxRaw > 50 -> ((maxRaw + 19) / 20) * 20
        maxRaw > 20 -> 50
        else -> 50
    }

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "graphProgress"
    )

    Row(modifier = Modifier.fillMaxWidth()) {
        // Y-Axis Labels Column on the left
        Column(
            modifier = Modifier
                .height(180.dp)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text("$maxScale", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mutedColor)
            Text("${maxScale / 2}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mutedColor)
            Text("0", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mutedColor)
        }

        // Main Graph & X-Axis Column
        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val baselineY = canvasHeight - 4.dp.toPx()
                val graphHeight = canvasHeight - 28.dp.toPx()

                val slotWidth = canvasWidth / 24f
                val barWidth = (slotWidth * 0.6f).coerceIn(4.dp.toPx(), 11.dp.toPx())
                val baselinePillHeight = 6.dp.toPx()

                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 12.sp.toPx()
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                val highestItem = items.maxByOrNull { it.count }

                items.forEachIndexed { index, item ->
                    val centerX = index * slotWidth + slotWidth / 2f
                    val left = centerX - barWidth / 2f

                    if (item.count > 0) {
                        val fraction = (item.count.toFloat() / maxScale).coerceIn(0f, 1f)
                        val barHeight = (fraction * graphHeight * animatedProgress).coerceAtLeast(baselinePillHeight)
                        val topY = baselineY - barHeight

                        val barBrush = Brush.verticalGradient(
                            listOf(RetroGoldTop, RetroAmberMid, RetroOrangeBottom),
                            startY = topY,
                            endY = baselineY
                        )

                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(left, topY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )

                        if (item === highestItem && item.count > 0 && animatedProgress >= 0.8f) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "${item.count}",
                                centerX,
                                topY - 7.dp.toPx(),
                                textPaint
                            )
                        }
                    } else {
                        drawRoundRect(
                            color = RetroBaselinePill.copy(alpha = 0.55f),
                            topLeft = Offset(left, baselineY - baselinePillHeight),
                            size = Size(barWidth, baselinePillHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // X-Axis Time Labels: 12am, 6am, 12pm, 6pm, 12am
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("12am", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mutedColor)
                Text("6am", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mutedColor)
                Text("12pm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mutedColor)
                Text("6pm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mutedColor)
                Text("12am", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mutedColor)
            }
        }
    }
}

@Composable
private fun ProgressWeeklyGraph(
    items: List<DailyBarItem>,
    mutedColor: Color
) {
    val maxRaw = items.maxOfOrNull { it.count } ?: 0
    val maxScale = when {
        maxRaw > 100 -> ((maxRaw + 49) / 50) * 50
        maxRaw > 50 -> 100
        else -> 50
    }

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "graphProgressWeekly"
    )

    Row(modifier = Modifier.fillMaxWidth()) {
        // Y-Axis Labels Column on the left
        Column(
            modifier = Modifier
                .height(180.dp)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text("$maxScale", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mutedColor)
            Text("${maxScale / 2}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mutedColor)
            Text("0", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mutedColor)
        }

        // Main Graph & X-Axis Column
        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val baselineY = canvasHeight - 4.dp.toPx()
                val graphHeight = canvasHeight - 28.dp.toPx()

                val slotWidth = canvasWidth / 7f
                val barWidth = (slotWidth * 0.45f).coerceIn(16.dp.toPx(), 26.dp.toPx())
                val baselinePillHeight = 6.dp.toPx()

                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 12.sp.toPx()
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                val highestItem = items.maxByOrNull { it.count }

                items.forEachIndexed { index, item ->
                    val centerX = index * slotWidth + slotWidth / 2f
                    val left = centerX - barWidth / 2f

                    if (item.count > 0) {
                        val fraction = (item.count.toFloat() / maxScale).coerceIn(0f, 1f)
                        val barHeight = (fraction * graphHeight * animatedProgress).coerceAtLeast(baselinePillHeight)
                        val topY = baselineY - barHeight

                        val barBrush = Brush.verticalGradient(
                            listOf(RetroGoldTop, RetroAmberMid, RetroOrangeBottom),
                            startY = topY,
                            endY = baselineY
                        )

                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(left, topY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )

                        if (item === highestItem && item.count > 0 && animatedProgress >= 0.8f) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "${item.count}",
                                centerX,
                                topY - 7.dp.toPx(),
                                textPaint
                            )
                        }
                    } else {
                        drawRoundRect(
                            color = RetroBaselinePill.copy(alpha = 0.55f),
                            topLeft = Offset(left, baselineY - baselinePillHeight),
                            size = Size(barWidth, baselinePillHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // X-Axis Day Labels: Mon, Tue, Wed, Thu, Fri, Sat, Sun
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                items.forEach { item ->
                    Text(
                        text = item.dayName.take(3),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = mutedColor
                    )
                }
            }
        }
    }
}
