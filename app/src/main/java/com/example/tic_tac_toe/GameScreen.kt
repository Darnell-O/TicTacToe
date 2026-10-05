package com.example.tic_tac_toe

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GameRoute(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GameScreen(
        state = state,
        onCellTapped = viewModel::onCellTapped,
        onRestart = viewModel::onRestart,
        modifier = modifier
    )
}

@Composable
fun GameScreen(
    state: GameUiState,
    onCellTapped: (Int) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val colors = gameColors
    val status = state.status
    val winLine = (status as? Status.Win)?.line.orEmpty()

    var gameOverFeedBackGiven by rememberSaveable { mutableStateOf(state.isOver) }
    LaunchedEffect(state.isOver) {
        if (state.isOver && !gameOverFeedBackGiven) {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                },
            )
            gameOverFeedBackGiven = true
        } else if (!state.isOver) {
            gameOverFeedBackGiven = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = statusText(state),
            style = MaterialTheme.typography.headlineMedium,
            color = if (status is Status.Win) colors.mark(status.player) else Color.Unspecified,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "X ${state.scores.xWins}   O ${state.scores.oWins}   Draws ${state.scores.draws}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        Board(
            board = state.board,
            winLine = winLine,
            enabled = !state.isOver,
            currentPlayer = state.currentPlayer,
            onCellTapped = { index ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onCellTapped(index)
            },
            // weight(fill = false) lets the board shrink to the space left
            // over in landscape; aspectRatio keeps it square either way.
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 400.dp)
                .aspectRatio(1f)
        )

        Spacer(Modifier.height(24.dp))
        Button(onClick = onRestart) {
            Text(if (state.isOver) "Play again" else "Restart")
        }
    }
}

/** 3×3 grid built from nested Row/Column; a lazy grid is overkill for 9 fixed cells. */
@Composable
private fun Board(
    board: List<Player?>,
    winLine: List<Int>,
    enabled: Boolean,
    currentPlayer: Player,
    onCellTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    Cell(
                        player = board[index],
                        row = row,
                        col = col,
                        isWinning = index in winLine,
                        isDimmed = winLine.isNotEmpty() && index !in winLine,
                        // Off once the game is over; occupied cells are off too,
                        // so there's no ripple on a tap that does nothing.
                        enabled = enabled && board[index] == null,
                        currentPlayer = currentPlayer,
                        onClick = { onCellTapped(index) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Cell(
    player: Player?,
    row: Int,
    col: Int,
    isWinning: Boolean,
    isDimmed: Boolean,
    enabled: Boolean,
    currentPlayer: Player,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val colors = gameColors

    val background by animateColorAsState(
        targetValue = if (isWinning && player != null) {
            colors.container(player)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300),
        label = "cellBackground"
    )

    val markAlpha by animateFloatAsState(
        targetValue = if (isDimmed) 0.35f else 1f,
        animationSpec = tween(300),
        label = "markAlpha"
    )

    val description = buildString {
        append("Row ${row + 1}, column ${col + 1},")
        append(
            when {
                player == null -> "empty"
                isWinning -> "${player.name},winning line"
                else -> player.name
            }
        )
    }

    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .semantics { contentDescription = description }
            .border(2.dp, MaterialTheme.colorScheme.onSurface, shape)
            .clickable(
                enabled = enabled,
                onClickLabel = "Place ${currentPlayer.name}",
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (player != null) {
            Mark(
                player = player,
                color = colors.mark(player),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = markAlpha }
            )
        }
    }
}

@Composable
private fun Mark(
    player: Player,
    color: Color,
    modifier: Modifier = Modifier
) {
    val inPreview = LocalInspectionMode.current
    var hasAnimated by rememberSaveable { mutableStateOf(inPreview) }
    val progress = remember { Animatable(if (hasAnimated) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            progress.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
            hasAnimated = true
        }
    }

    Canvas(modifier) {
        val p = progress.value
        val strokeWidth = size.minDimension * 0.1f
        val inset = size.minDimension * 0.27f

        when (player) {
            Player.X -> {
                val first = (p * 2f).coerceIn(0f, 1f)
                val second = (p * 2f - 1f).coerceIn(0f, 1f)

                val start1 = Offset(inset, inset)
                val end1 = Offset(size.width - inset, size.height - inset)
                if (first > 0f) {
                    drawLine(color, start1, lerp(start1, end1, first), strokeWidth, StrokeCap.Round)
                }

                val start2 = Offset(size.width - inset, inset)
                val end2 = Offset(inset, size.height - inset)
                if (second > 0f) {
                    drawLine(
                        color,
                        start2,
                        lerp(start2, end2, second),
                        strokeWidth,
                        StrokeCap.Round
                    )
                }
            }

            Player.O -> if (p > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * p,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - 2 * inset, size.height - 2 * inset),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
    }
}

private fun statusText(state: GameUiState): String = when (val status = state.status) {
    Status.InProgress -> "${state.currentPlayer.name}'s turn"
    is Status.Win -> "${status.player.name} wins!!"
    Status.Draw -> "It's a draw"
}

@Preview(showBackground = true)
@Composable
private fun GameScreenPreview() {
    MaterialTheme {
        GameScreen(
            state = GameUiState(
                board = listOf(
                    Player.X, null, Player.O,
                    null, Player.X, null,
                    Player.O, null, null,
                ),
                currentPlayer = Player.X,
                scores = Scores(xWins = 2, oWins = 1, draws = 1),
            ),
            onCellTapped = {},
            onRestart = {},
        )
    }
}