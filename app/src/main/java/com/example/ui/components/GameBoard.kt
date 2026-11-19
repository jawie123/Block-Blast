package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.model.BoardThemeCatalog
import com.example.model.BoardThemeSpec
import com.example.model.GameState
import com.example.model.ScorePopup
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder

/**
 * GridBoard composable using LazyVerticalGrid with GridCells.Fixed(8) for the 8x8 layout.
 */
@Composable
fun GridBoard(
    gameState: GameState,
    ghostColorId: Int?,
    onCellTapped: (row: Int, col: Int) -> Unit,
    onBoardPositioned: (bounds: Rect, cellSizePx: Float) -> Unit,
    theme: BoardThemeSpec = BoardThemeCatalog.defaultTheme,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val streak = gameState.comboStreak
    val isHighScoreStreak = gameState.isNewHighScore || streak >= 4
    val isBombMode = gameState.isBombActive

    // Dynamic background colors responding smoothly to combo streaks, multipliers, and themes
    val targetBgTop = when {
        isBombMode -> Color(0xFF281210)
        isHighScoreStreak -> Color(0xFF26190B) // Radiant Gold-Amber aura
        streak >= 3 -> Color(0xFF2A1028) // Fiery Crimson / Magenta aura
        streak == 2 -> Color(0xFF1B1438) // Electric Purple aura
        streak == 1 -> Color(0xFF101E33) // Electric Cyan aura
        else -> theme.boardBackground
    }

    val targetBgBottom = when {
        isBombMode -> Color(0xFF170B0A)
        isHighScoreStreak -> Color(0xFF181005)
        streak >= 3 -> Color(0xFF1A0A19)
        streak == 2 -> Color(0xFF110D24)
        streak == 1 -> Color(0xFF0C1422)
        else -> theme.boardBackground.copy(alpha = 0.94f)
    }

    val targetBorderColor = when {
        isBombMode -> Color(0xFFFF5722)
        isHighScoreStreak -> Color(0xFFFFD700) // Golden crown border
        streak >= 3 -> Color(0xFFFF007A) // Hot Pink / Neon Crimson
        streak == 2 -> Color(0xFF9C27B0) // Vivid Purple
        streak == 1 -> Color(0xFF00E5FF) // Electric Cyan
        else -> theme.boardBorder
    }

    val targetGlowColor = when {
        isBombMode -> Color(0xFFFF5722)
        isHighScoreStreak -> Color(0xFFFFB300)
        streak >= 3 -> Color(0xFFFF1744)
        streak == 2 -> Color(0xFF7C4DFF)
        streak == 1 -> Color(0xFF00E5FF)
        else -> theme.accentColor.copy(alpha = 0.25f)
    }

    // Smooth animated color transitions
    val animBgTop by animateColorAsState(
        targetValue = targetBgTop,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "animBgTop"
    )
    val animBgBottom by animateColorAsState(
        targetValue = targetBgBottom,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "animBgBottom"
    )
    val animBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "animBorderColor"
    )
    val animGlowColor by animateColorAsState(
        targetValue = targetGlowColor,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "animGlowColor"
    )

    val borderGlowWidth = if (streak > 0 || isHighScoreStreak) 2.5.dp else 1.5.dp

    Box(
        modifier = modifier
            .shadow(
                elevation = if (streak > 0 || isHighScoreStreak) 20.dp else 12.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = animGlowColor
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(animBgTop, animBgBottom)
                )
            )
            .border(borderGlowWidth, animBorderColor, RoundedCornerShape(16.dp))
            .testTag("grid_board_container")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    val cellSizePx = bounds.width / 8f
                    onBoardPositioned(bounds, cellSizePx)
                }
                .pointerInput(gameState.selectedShapeIndex, gameState.isHammerActive, gameState.isBombActive) {
                    detectTapGestures { offset ->
                        val cellSize = size.width / 8f
                        val col = (offset.x / cellSize).toInt().coerceIn(0, 7)
                        val row = (offset.y / cellSize).toInt().coerceIn(0, 7)
                        if (gameState.selectedShapeIndex != null || gameState.isHammerActive || gameState.isBombActive) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onCellTapped(row, col)
                    }
                }
        ) {
            val cellSize = maxWidth / 8f

            // Subtle ambient radial glow when on a streak or multiplier
            if (streak > 0 || isHighScoreStreak) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(animGlowColor.copy(alpha = 0.16f), Color.Transparent),
                                    radius = size.width * 0.75f
                                )
                            )
                        }
                )
            }

            val hoverMask = remember(gameState.hoverPreviewCells) {
                var mask = 0L
                for (cell in gameState.hoverPreviewCells) {
                    val r = cell.first
                    val c = cell.second
                    if (r in 0..7 && c in 0..7) {
                        mask = mask or (1L shl (r * 8 + c))
                    }
                }
                mask
            }
            val clearingMask = remember(gameState.clearingCells) {
                var mask = 0L
                for (cell in gameState.clearingCells) {
                    val r = cell.first
                    val c = cell.second
                    if (r in 0..7 && c in 0..7) {
                        mask = mask or (1L shl (r * 8 + c))
                    }
                }
                mask
            }

            // Single shared pulse animation, instantiated only while a cell actually pulses
            val needsTargetPulse = gameState.isHammerActive || gameState.isBombActive
            val needsProspectivePulse = gameState.prospectiveRows.isNotEmpty() || gameState.prospectiveCols.isNotEmpty()
            val sharedPulseAlpha: Float? = if (needsTargetPulse || needsProspectivePulse) {
                val pulseTransition = rememberInfiniteTransition(label = "board_cell_pulse")
                val pulse by pulseTransition.animateFloat(
                    initialValue = if (needsTargetPulse) 0.5f else 0.35f,
                    targetValue = if (needsTargetPulse) 1.0f else 0.85f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(if (needsTargetPulse) 350 else 450, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "board_cell_pulse_alpha"
                )
                pulse
            } else {
                null
            }

            val stableEmptyTint = if (streak > 0 || isHighScoreStreak) targetGlowColor else null

            // 8x8 Grid Matrix rendered directly with Column/Row (avoids LazyVerticalGrid subcomposition overhead)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("grid_board_lazy_grid")
            ) {
                for (r in 0 until 8) {
                    val rowGrid = gameState.grid[r]
                    val isRowProspective = gameState.prospectiveRows.contains(r)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (c in 0 until 8) {
                            val bit = 1L shl (r * 8 + c)
                            val colorId = rowGrid[c]
                            val isGhost = (hoverMask and bit) != 0L
                            val isClearing = (clearingMask and bit) != 0L
                            val isProspective = isRowProspective || gameState.prospectiveCols.contains(c)
                            val isHammerTarget = gameState.isHammerActive && colorId > 0
                            val isBombTarget = gameState.isBombActive && colorId > 0
                            val isGem = colorId > 0 && gameState.gemCells.isNotEmpty() && (r to c) in gameState.gemCells
                            val isLightning = colorId > 0 && gameState.lightningCells.isNotEmpty() && (r to c) in gameState.lightningCells
                            val iceLayers = if (colorId > 0 && gameState.iceCells.isNotEmpty()) {
                                gameState.iceCells[r to c] ?: 0
                            } else {
                                0
                            }
                            val cellPulse = if (isProspective || isHammerTarget || isBombTarget) {
                                sharedPulseAlpha
                            } else {
                                null
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .testTag("grid_cell_${r}_${c}")
                            ) {
                                BlockCell(
                                    colorId = colorId,
                                    isGhostPreview = isGhost,
                                    ghostColorId = ghostColorId,
                                    isClearing = isClearing,
                                    isProspectiveClear = isProspective,
                                    isHammerTarget = isHammerTarget,
                                    isBombTarget = isBombTarget,
                                    isGem = isGem,
                                    isLightning = isLightning,
                                    iceLayers = iceLayers,
                                    theme = theme,
                                    emptyTileTint = stableEmptyTint,
                                    pulseAlpha = cellPulse
                                )
                            }
                        }
                    }
                }
            }

            // Line Clear Particle System with combo visual feedback & shockwave ripples
            LineClearParticleSystem(
                bursts = gameState.lineClearBursts,
                modifier = Modifier.fillMaxSize()
            )

            // Score Popups Floating Layer
            for (popup in gameState.scorePopups) {
                FloatingScorePopup(
                    popup = popup,
                    cellSizeDp = cellSize
                )
            }

            // Combo Banner Overlay
            AnimatedVisibility(
                visible = !gameState.comboBannerText.isNullOrEmpty(),
                enter = scaleIn(tween(250, easing = FastOutSlowInEasing)) + fadeIn(),
                exit = scaleOut(tween(200)) + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                gameState.comboBannerText?.let { banner ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF7C4DFF).copy(alpha = 0.95f),
                                        Color(0xFFFF007A).copy(alpha = 0.95f),
                                        Color(0xFFFF9100).copy(alpha = 0.95f)
                                    )
                                )
                            )
                            .border(2.dp, Color.White, RoundedCornerShape(18.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .shadow(20.dp)
                            .testTag("combo_banner_badge")
                    ) {
                        Text(
                            text = banner,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * GameBoard compatibility alias delegating to GridBoard.
 */
@Composable
fun GameBoard(
    gameState: GameState,
    ghostColorId: Int?,
    onCellTapped: (row: Int, col: Int) -> Unit,
    onBoardPositioned: (bounds: Rect, cellSizePx: Float) -> Unit,
    theme: BoardThemeSpec = BoardThemeCatalog.defaultTheme,
    modifier: Modifier = Modifier
) {
    GridBoard(
        gameState = gameState,
        ghostColorId = ghostColorId,
        onCellTapped = onCellTapped,
        onBoardPositioned = onBoardPositioned,
        theme = theme,
        modifier = modifier
    )
}

@Composable
private fun FloatingScorePopup(
    popup: ScorePopup,
    cellSizeDp: androidx.compose.ui.unit.Dp
) {
    val animOffsetY = remember { Animatable(0f) }
    val animScale = remember { Animatable(0.35f) }
    val animAlpha = remember { Animatable(1f) }

    LaunchedEffect(popup.id) {
        // Pop-in bounce overshoot
        animScale.animateTo(
            targetValue = 1.25f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        )
        animScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 140)
        )
    }

    LaunchedEffect(popup.id) {
        // Smooth upward rise
        animOffsetY.animateTo(
            targetValue = -75f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(popup.id) {
        // Fade out toward the end of rise
        delay(450)
        animAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 450)
        )
    }

    val posX = (popup.col * cellSizeDp.value).dp + (cellSizeDp / 2) - 40.dp
    val posY = (popup.row * cellSizeDp.value).dp + animOffsetY.value.dp

    Box(
        modifier = Modifier
            .offset { IntOffset(posX.roundToPx(), posY.roundToPx()) }
            .scale(animScale.value)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (popup.isCombo) Color(0xFFFF9800) else Color(0xFF00E5FF)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    if (popup.isCombo) {
                        listOf(
                            Color(0xFFFF9800).copy(alpha = animAlpha.value * 0.95f),
                            Color(0xFFFF5252).copy(alpha = animAlpha.value * 0.95f)
                        )
                    } else {
                        listOf(
                            Color(0xFF00E5FF).copy(alpha = animAlpha.value * 0.95f),
                            Color(0xFF7C4DFF).copy(alpha = animAlpha.value * 0.95f)
                        )
                    }
                )
            )
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = animAlpha.value * 0.9f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("floating_score_popup"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = popup.text,
            color = Color.White.copy(alpha = animAlpha.value),
            fontSize = if (popup.isCombo) 15.sp else 13.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp
        )
    }
}
