package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockShape
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder

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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 3) {
            val shape = pieces.getOrNull(i)
            val canFit = canFitList.getOrNull(i) ?: true
            val isSelected = selectedShapeIndex == i
            val isDraggingThis = draggingPieceIndex == i

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BoardBackground)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFF00E5FF) else BoardBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .testTag("piece_slot_$i"),
                contentAlignment = Alignment.Center
            ) {
                if (shape != null && !isDraggingThis) {
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        label = "slotScale"
                    )

                    var slotRootBounds = Offset.Zero

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(scale)
                            .alpha(if (canFit) 1.0f else 0.38f)
                            .onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInRoot()
                                slotRootBounds = bounds.center
                            }
                            .pointerInput(shape.instanceId) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragStart(i, slotRootBounds + localOffset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDragMove(dragAmount)
                                    },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragEnd() }
                                )
                            }
                            .clickable {
                                if (canFit) {
                                    if (isSelected) {
                                        onSelectPiece(null)
                                    } else {
                                        onSelectPiece(i)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ShapeRenderer(
                            shape = shape,
                            cellUnitDp = 20.dp
                        )
                    }

                    if (!canFit) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NO FIT",
                                color = Color(0xFFFF5252),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
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
