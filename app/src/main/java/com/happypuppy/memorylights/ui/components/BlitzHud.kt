package com.happypuppy.memorylights.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.R

/**
 * Top-of-screen HUD shown only in Speed Blitz: progress (X / target) and a
 * live elapsed-time readout that ticks at 10 Hz so the tenths digit moves
 * visibly. The clock is read from `System.currentTimeMillis()` rather than
 * tracked in UiState to keep the ViewModel from emitting at 10 Hz.
 *
 * Extracted from `SimonGameScreen` (Wave 11) — layout, colors, tick rate,
 * and pause freeze are byte-equivalent to the previous private composable.
 * Elapsed display uses [formatBlitzTime] shared with [CenterDiscContent].
 */
@Composable
fun BlitzHud(
    level: Int,
    targetLevel: Int,
    bestTimeMs: Long,
    startTimeMs: Long,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startTimeMs, isPaused) {
        if (startTimeMs <= 0L || isPaused) return@LaunchedEffect
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(100L)
        }
    }
    val elapsed = if (startTimeMs > 0L) (nowMs - startTimeMs).coerceAtLeast(0L) else 0L

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.55f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = stringResource(R.string.blitz_level_progress, level, targetLevel),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = stringResource(R.string.blitz_separator),
            color = Color.Gray,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal)
        )
        Text(
            text = formatBlitzTime(elapsed),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        if (bestTimeMs > 0L) {
            Text(
                text = stringResource(R.string.blitz_separator),
                color = Color.Gray,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal)
            )
            Text(
                text = stringResource(R.string.game_best_time, formatBlitzTime(bestTimeMs)),
                color = Color(0xFFFFC107),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
