package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.model.ScorePopup
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder

@Composable
fun GameBoard(
    gameState: GameState,
    ghostColorId: Int?,
    onCellTapped: (row: Int, col: Int) -> Unit,
    onBoardPositioned: (bounds: Rect, cellSizePx: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF00E5FF).copy(alpha = 0.25f))
            .clip(RoundedCornerShape(16.dp))
            .background(BoardBackground)
            .border(2.dp, BoardBorder, RoundedCornerShape(16.dp))
            .testTag("game_board_container")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .aspectRatio(1f)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    val cellSizePx = bounds.width / 8f
                    onBoardPositioned(bounds, cellSizePx)
                }
                .pointerInput(gameState.selectedShapeIndex) {
                    detectTapGestures { offset ->
                        val cellSize = size.width / 8f
                        val col = (offset.x / cellSize).toInt().coerceIn(0, 7)
                        val row = (offset.y / cellSize).toInt().coerceIn(0, 7)
                        onCellTapped(row, col)
                    }
                }
        ) {
            val cellSize = maxWidth / 8f

            // 8x8 Grid Matrix
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until 8) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (c in 0 until 8) {
                            val colorId = gameState.grid[r][c]
                            val isGhost = gameState.hoverPreviewCells.contains(Pair(r, c))
                            val isClearing = gameState.clearingCells.contains(Pair(r, c))
                            val isProspective = gameState.prospectiveRows.contains(r) ||
                                    gameState.prospectiveCols.contains(c)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            ) {
                                BlockCell(
                                    colorId = colorId,
                                    isGhostPreview = isGhost,
                                    ghostColorId = ghostColorId,
                                    isClearing = isClearing,
                                    isProspectiveClear = isProspective
                                )
                            }
                        }
                    }
                }
            }

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

@Composable
private fun FloatingScorePopup(
    popup: ScorePopup,
    cellSizeDp: androidx.compose.ui.unit.Dp
) {
    val animOffsetY = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }

    LaunchedEffect(popup.id) {
        animOffsetY.animateTo(
            targetValue = -60f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(popup.id) {
        animAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 800, delayMillis = 200)
        )
    }

    val posX = (popup.col * cellSizeDp.value).dp + (cellSizeDp / 2) - 30.dp
    val posY = (popup.row * cellSizeDp.value).dp + animOffsetY.value.dp

    Box(
        modifier = Modifier
            .offset { IntOffset(posX.roundToPx(), posY.roundToPx()) }
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (popup.isCombo) Color(0xFFFF9800).copy(alpha = animAlpha.value)
                else Color(0xFF00E5FF).copy(alpha = animAlpha.value)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = popup.text,
            color = Color.White.copy(alpha = animAlpha.value),
            fontSize = if (popup.isCombo) 16.sp else 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}
