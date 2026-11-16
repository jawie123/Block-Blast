package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockShape
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import kotlinx.coroutines.CancellationException

/**
 * PieceSlot composable to display an individual active random shape in the piece pool.
 * Supports immediate drag-and-drop pickup directly onto the board as well as tap-to-select.
 */
@Composable
fun PieceSlot(
    index: Int,
    shape: BlockShape?,
    canFit: Boolean,
    isSelected: Boolean,
    isDragging: Boolean,
    onSelectPiece: (index: Int?) -> Unit,
    onDragStart: (index: Int, touchPosRoot: Offset) -> Unit,
    onDragMove: (delta: Offset) -> Unit,
    onDragEnd: () -> Unit,
    compactMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val dragThresholdPx = with(density) { 4.dp.toPx() }
    var slotRootTopLeft by remember { mutableStateOf(Offset.Zero) }

    val currentShape by rememberUpdatedState(shape)
    val currentCanFit by rememberUpdatedState(canFit)
    val currentIsSelected by rememberUpdatedState(isSelected)
    val currentOnSelectPiece by rememberUpdatedState(onSelectPiece)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragMove by rememberUpdatedState(onDragMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier = modifier
            .height(if (compactMode) 68.dp else 96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BoardBackground)
            .border(
                width = if (isSelected || isDragging) 2.dp else 1.dp,
                color = when {
                    isDragging -> Color(0xFF00E5FF).copy(alpha = 0.6f)
                    isSelected -> Color(0xFF00E5FF)
                    else -> BoardBorder
                },
                shape = RoundedCornerShape(12.dp)
            )
            .onGloballyPositioned { coordinates ->
                slotRootTopLeft = coordinates.boundsInRoot().topLeft
            }
            .pointerInput(index) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val activeShape = currentShape
                    val fits = currentCanFit
                    if (activeShape == null || !fits) {
                        return@awaitEachGesture
                    }

                    var dragStarted = false
                    var accumulatedOffset = Offset.Zero

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()

                            if (change == null || !change.pressed || change.changedToUpIgnoreConsumed()) {
                                if (dragStarted) {
                                    currentOnDragEnd()
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (currentIsSelected) {
                                        currentOnSelectPiece(null)
                                    } else {
                                        currentOnSelectPiece(index)
                                    }
                                }
                                break
                            }

                            val delta = change.positionChange()
                            if (delta != Offset.Zero) {
                                accumulatedOffset += delta
                                if (!dragStarted && accumulatedOffset.getDistance() >= dragThresholdPx) {
                                    dragStarted = true
                                    change.consume()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    currentOnDragStart(index, slotRootTopLeft + change.position)
                                } else if (dragStarted) {
                                    change.consume()
                                    currentOnDragMove(delta)
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        if (dragStarted) {
                            currentOnDragEnd()
                        }
                        throw e
                    }
                }
            }
            .testTag("piece_slot_$index"),
        contentAlignment = Alignment.Center
    ) {
        if (shape != null) {
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.08f else 1.0f,
                label = "slotScale_$index"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                // Keep mounted during drag to sustain pointerInput events while hiding static piece
                Box(
                    modifier = Modifier.alpha(
                        if (isDragging) 0f else if (canFit) 1.0f else 0.38f
                    )
                ) {
                    ShapeRenderer(
                        shape = shape,
                        cellUnitDp = if (compactMode) 13.dp else 18.dp
                    )
                }
            }

            if (!canFit && !isDragging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NO FIT",
                        color = Color(0xFFFF5252),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * PieceDeck composable holding the 3 PieceSlot containers.
 */
@Composable
fun PieceDeck(
    pieces: List<BlockShape?>,
    canFitList: List<Boolean>,
    selectedShapeIndex: Int?,
    draggingPieceIndex: Int?,
    onSelectPiece: (index: Int?) -> Unit,
    onDragStart: (index: Int, touchPosRoot: Offset) -> Unit,
    onDragMove: (delta: Offset) -> Unit,
    onDragEnd: () -> Unit,
    compactMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compactMode) 76.dp else 106.dp)
            .padding(horizontal = 12.dp, vertical = if (compactMode) 2.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 3) {
            PieceSlot(
                index = i,
                shape = pieces.getOrNull(i),
                canFit = canFitList.getOrNull(i) ?: true,
                isSelected = selectedShapeIndex == i,
                isDragging = draggingPieceIndex == i,
                onSelectPiece = onSelectPiece,
                onDragStart = onDragStart,
                onDragMove = onDragMove,
                onDragEnd = onDragEnd,
                compactMode = compactMode,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ShapeRenderer(
    shape: BlockShape,
    cellUnitDp: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (r in 0 until shape.height) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (c in 0 until shape.width) {
                    if (shape.matrix[r][c]) {
                        Box(modifier = Modifier.size(cellUnitDp)) {
                            BlockCell(
                                colorId = shape.colorId,
                                cornerRadius = 4.dp
                            )
                        }
                    } else {
                        // Invisible placeholder spacer to maintain shape geometry
                        Box(modifier = Modifier.size(cellUnitDp))
                    }
                }
            }
        }
    }
}
