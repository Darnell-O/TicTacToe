package com.example.tic_tac_toe

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Immutable
data class GameColors(
    val x: Color,
    val xContainer: Color,
    val o: Color,
    val oContainer: Color
) {
    fun mark(player: Player): Color = if (player == Player.X) x else o
    fun container(player: Player): Color = if (player == Player.X) xContainer else oContainer
}

private val LightGameColors = GameColors(
    x = Color(0xFFC2410C),
    xContainer = Color(0xFFFFDBCB),
    o = Color(0xFF1D4ED8),
    oContainer = Color(0xFFD6E3FF),
)

private val DarkGameColors = GameColors(
    x = Color(0xFFFFB59A),
    xContainer = Color(0xFF7A2E0E),
    o = Color(0xFFADC6FF),
    oContainer = Color(0xFF1E3A8A),
)


val gameColors: GameColors
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        DarkGameColors
    } else {
        LightGameColors
    }