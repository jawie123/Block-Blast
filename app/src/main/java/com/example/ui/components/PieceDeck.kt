package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockShape
import com.example.model.BoardThemeCatalog
import com.example.model.BoardThemeSpec
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
    isRotateMode: Boolean = false,
    onSelectPiece: (index: Int?) -> Unit,
    onDragStart: (index: Int, touchPosRoot: Offset) -> Unit,
    onDragMove: (delta: Offset) -> Unit,
    onDragEnd: () -> Unit,
    compactMode: Boolean = false,
    theme: BoardThemeSpec = BoardThemeCatalog.defaultTheme,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val dragThresholdPx = with(density) { 4.dp.toPx() }
    var slotRootTopLeft by remember { mutableStateOf(Offset.Zero) }

    val currentShape by rememberUpdatedState(shape)
    val currentCanFit by rememberUpdatedState(canFit)
    val currentIsSelected by rememberUpdatedState(isSelected)
    val currentIsRotateMode by rememberUpdatedState(isRotateMode)
    val currentOnSelectPiece by rememberUpdatedState(onSelectPiece)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragMove by rememberUpdatedState(onDragMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val canRotateThis = isRotateMode && shape != null && !shape.isRotationSymmetric()

    Box(
        modifier = modifier
            .height(if (compactMode) 62.dp else 84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(theme.boardBackground)
            .border(
                width = if (isSelected || isDragging || canRotateThis) 2.dp else 1.dp,
                color = when {
                    canRotateThis -> Color(0xFFE040FB)
                    isDragging -> theme.accentColor.copy(alpha = 0.6f)
                    isSelected -> theme.accentColor
                    else -> theme.boardBorder
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
                    val rotateMode = currentIsRotateMode
                    if (activeShape == null || (!fits && !rotateMode)) {
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
                                    if (rotateMode) {
                                        currentOnSelectPiece(index)
                                    } else if (currentIsSelected) {
                                        currentOnSelectPiece(null)
                                    } else {
                                        currentOnSelectPiece(index)
                                    }
                                }
                                break
                            }

                            val delta = change.positionChange()
                            if (delta != Offset.Zero && fits && !rotateMode) {
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
                        if (isDragging) 0f else if (canFit || isRotateMode) 1.0f else 0.38f
                    )
                ) {
                    val maxDim = maxOf(shape.width, shape.height)
                    val unitDp = when {
                        maxDim >= 5 -> if (compactMode) 10.dp else 14.dp
                        maxDim == 4 -> if (compactMode) 11.dp else 15.dp
                        else -> if (compactMode) 12.dp else 16.dp
                    }
                    ShapeRenderer(
                        shape = shape,
                        cellUnitDp = unitDp,
                        theme = theme
                    )
                }
            }

            if (canRotateThis && !isDragging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE040FB))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                        .testTag("piece_rotate_badge_$index")
                ) {
                    Text(
                        text = "↻ TAP",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else if (!canFit && !isDragging) {
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
 * PieceDeck composable holding the Hold Piece Pocket and 3 PieceSlot containers.
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
    heldPiece: BlockShape? = null,
    canHoldPiece: Boolean = true,
    onHoldSlotClick: () -> Unit = {},
    isRotateMode: Boolean = false,
    compactMode: Boolean = false,
    theme: BoardThemeSpec = BoardThemeCatalog.defaultTheme,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compactMode) 68.dp else 90.dp)
            .padding(horizontal = 12.dp, vertical = if (compactMode) 2.dp else 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HoldPieceSlot(
            heldPiece = heldPiece,
            canHold = canHoldPiece,
            hasSelection = selectedShapeIndex != null,
            compactMode = compactMode,
            theme = theme,
            onClick = onHoldSlotClick,
            modifier = Modifier.weight(0.76f)
        )

        for (i in 0 until 3) {
            PieceSlot(
                index = i,
                shape = pieces.getOrNull(i),
                canFit = canFitList.getOrNull(i) ?: true,
                isSelected = selectedShapeIndex == i,
                isDragging = draggingPieceIndex == i,
                isRotateMode = isRotateMode,
                onSelectPiece = onSelectPiece,
                onDragStart = onDragStart,
                onDragMove = onDragMove,
                onDragEnd = onDragEnd,
                compactMode = compactMode,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HoldPieceSlot(
    heldPiece: BlockShape?,
    canHold: Boolean,
    hasSelection: Boolean,
    compactMode: Boolean,
    theme: BoardThemeSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when {
        !canHold -> Color(0xFF5D6A85)
        heldPiece != null || hasSelection -> Color(0xFF00E5FF)
        else -> Color(0xFF7C4DFF)
    }

    Box(
        modifier = modifier
            .testTag("hold_piece_slot")
            .height(if (compactMode) 62.dp else 84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (canHold) theme.boardBackground else theme.boardBackground.copy(alpha = 0.65f)
            )
            .border(
                width = if (canHold && (heldPiece != null || hasSelection)) 1.6.dp else 1.dp,
                color = accent.copy(alpha = if (canHold) 0.8f else 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(role = Role.Button) { onClick() }
            .semantics {
                contentDescription = if (heldPiece != null) {
                    "Hold pocket occupied, tap to swap or recall"
                } else {
                    "Hold pocket empty, tap to store a piece"
                }
            }
            .padding(vertical = 4.dp, horizontal = 3.dp)
    ) {
        Text(
            text = "HOLD",
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = accent,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 11.dp, bottom = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (heldPiece != null) {
                val maxDim = maxOf(heldPiece.width, heldPiece.height)
                val unitDp = when {
                    maxDim >= 5 -> if (compactMode) 6.dp else 8.dp
                    maxDim == 4 -> if (compactMode) 7.dp else 9.dp
                    else -> if (compactMode) 8.dp else 10.dp
                }
                Box(modifier = Modifier.alpha(if (canHold) 1f else 0.6f)) {
                    ShapeRenderer(
                        shape = heldPiece,
                        cellUnitDp = unitDp,
                        theme = theme
                    )
                }
            } else {
                Text(
                    text = if (hasSelection) "STORE" else "📦",
                    fontSize = if (hasSelection) 9.sp else 16.sp,
                    fontWeight = FontWeight.Black,
                    color = accent.copy(alpha = 0.85f),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Text(
            text = when {
                !canHold -> "WAIT"
                heldPiece != null -> "SWAP"
                else -> "TAP"
            },
            fontSize = 7.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accent.copy(alpha = 0.85f),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ShapeRenderer(
    shape: BlockShape,
    cellUnitDp: Dp = 22.dp,
    theme: BoardThemeSpec = BoardThemeCatalog.defaultTheme,
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
                                theme = theme,
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
