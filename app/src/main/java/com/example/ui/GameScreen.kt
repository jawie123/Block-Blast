package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AppHaptics
import com.example.audio.HapticEffect
import com.example.model.BlockShape
import com.example.ui.components.GridBoard
import com.example.ui.components.GameOverDialog
import com.example.ui.components.PieceDeck
import com.example.ui.components.PieceSlot
import com.example.ui.components.ScoreHeader
import com.example.ui.components.ShapeRenderer
import com.example.ui.components.StatsDialog
import com.example.ui.components.TutorialOverlay
import com.example.ui.theme.DarkBackground
import com.example.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    val showTutorial by viewModel.showTutorial.collectAsState()
    val density = LocalDensity.current

    // Haptic feedback & context
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    // Observe and perform tactile vibrations on placement, line clears, pickups, and game over
    LaunchedEffect(viewModel) {
        viewModel.hapticEvent.collect { effect ->
            AppHaptics.perform(context, effect, hapticFeedback)
        }
    }

    // Dialog state
    var showStatsDialog by remember { mutableStateOf(false) }

    // Board layout tracking for drag and drop
    var boardBounds by remember { mutableStateOf<Rect?>(null) }
    var boardCellSizePx by remember { mutableFloatStateOf(0f) }

    // Dragging state
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragTouchPos by remember { mutableStateOf(Offset.Zero) }

    // Calculate canFit list for active pieces
    val canFitList by remember(gameState.grid, gameState.activePieces) {
        derivedStateOf {
            gameState.activePieces.map { shape ->
                if (shape != null) viewModel.canFitAnywhere(gameState.grid, shape) else false
            }
        }
    }

    // Visual drag lift offset (in pixels) so thumb does not obscure the block
    val liftOffsetPx = with(density) { 90.dp.toPx() }

    // Active dragging shape
    val draggingShape = draggingIndex?.let { gameState.activePieces.getOrNull(it) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("game_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Section: Score & Controls
            ScoreHeader(
                score = gameState.score,
                highScore = gameState.highScore,
                comboStreak = gameState.comboStreak,
                isSoundEnabled = viewModel.soundManager.isSoundEnabled,
                isHapticsEnabled = viewModel.soundManager.isHapticsEnabled,
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onShowStats = { showStatsDialog = true },
                onShowTutorial = { viewModel.openTutorial() },
                onRestart = { viewModel.startNewGame() }
            )

            // 2. Center Section: 8x8 Game Board
            val ghostColorId = draggingShape?.colorId
                ?: gameState.selectedShapeIndex?.let { gameState.activePieces.getOrNull(it)?.colorId }

            GridBoard(
                gameState = gameState,
                ghostColorId = ghostColorId,
                onCellTapped = { row, col ->
                    val selectedIndex = gameState.selectedShapeIndex
                    if (selectedIndex != null) {
                        viewModel.placeShape(selectedIndex, row, col)
                    }
                },
                onBoardPositioned = { bounds, cellSizePx ->
                    boardBounds = bounds
                    boardCellSizePx = cellSizePx
                },
                modifier = Modifier.weight(1f, fill = false)
            )

            // 3. Bottom Section: Active Piece Pool (3 slots)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tap mode instruction if a piece is selected
                AnimatedVisibility(
                    visible = gameState.selectedShapeIndex != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2640))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Tap any cell on the grid to place",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                PieceDeck(
                    pieces = gameState.activePieces,
                    canFitList = canFitList,
                    selectedShapeIndex = gameState.selectedShapeIndex,
                    draggingPieceIndex = draggingIndex,
                    onSelectPiece = { idx -> viewModel.selectPieceForTap(idx) },
                    onDragStart = { idx, initialTouchPos ->
                        val shape = gameState.activePieces.getOrNull(idx)
                        if (shape != null && (canFitList.getOrNull(idx) == true)) {
                            viewModel.soundManager.playPickup()
                            draggingIndex = idx
                            dragTouchPos = initialTouchPos
                            viewModel.selectPieceForTap(null)
                        }
                    },
                    onDragMove = { delta ->
                        dragTouchPos += delta
                        val bounds = boardBounds
                        val shape = draggingShape
                        if (bounds != null && shape != null && boardCellSizePx > 0f) {
                            // Target location of shape top-left anchor
                            val pieceCenterX = dragTouchPos.x
                            val pieceCenterY = dragTouchPos.y - liftOffsetPx

                            val shapeWidthPx = shape.width * boardCellSizePx
                            val shapeHeightPx = shape.height * boardCellSizePx
                            val pieceLeftX = pieceCenterX - (shapeWidthPx / 2f)
                            val pieceTopY = pieceCenterY - (shapeHeightPx / 2f)

                            val relativeX = pieceLeftX - bounds.left
                            val relativeY = pieceTopY - bounds.top

                            val targetCol = Math.round(relativeX / boardCellSizePx)
                            val targetRow = Math.round(relativeY / boardCellSizePx)

                            if (targetRow in -1..8 && targetCol in -1..8) {
                                viewModel.updateHoverPreview(draggingIndex!!, targetRow, targetCol)
                            } else {
                                viewModel.updateHoverPreview(draggingIndex!!, null, null)
                            }
                        }
                    },
                    onDragEnd = {
                        val currentDragging = draggingIndex
                        val hoverR = gameState.hoverRow
                        val hoverC = gameState.hoverCol

                        if (currentDragging != null && hoverR != null && hoverC != null) {
                            viewModel.placeShape(currentDragging, hoverR, hoverC)
                        } else {
                            viewModel.updateHoverPreview(currentDragging ?: 0, null, null)
                        }
                        draggingIndex = null
                    }
                )

                // 4. Permanent Arcade / Ad Slot at the bottom
                ArcadeBannerSlot()
            }
        }

        // Floating Drag Overlay (renders above entire screen during drag gesture)
        if (draggingShape != null && boardCellSizePx > 0f) {
            val shape = draggingShape
            val shapeWidthPx = shape.width * boardCellSizePx
            val shapeHeightPx = shape.height * boardCellSizePx
            val pieceLeftX = dragTouchPos.x - (shapeWidthPx / 2f)
            val pieceTopY = (dragTouchPos.y - liftOffsetPx) - (shapeHeightPx / 2f)

            val cellUnitDp = with(density) { boardCellSizePx.toDp() }

            Box(
                modifier = Modifier
                    .offset { IntOffset(pieceLeftX.toInt(), pieceTopY.toInt()) }
                    .shadow(12.dp)
            ) {
                ShapeRenderer(
                    shape = shape,
                    cellUnitDp = cellUnitDp
                )
            }
        }

        // Game Over Modal
        if (gameState.isGameOver) {
            GameOverDialog(
                score = gameState.score,
                highScore = gameState.highScore,
                isNewHighScore = gameState.isNewHighScore,
                linesCleared = gameState.linesClearedSession,
                maxCombo = gameState.maxComboSession,
                secondChanceAvailable = gameState.secondChanceAvailable,
                onSecondChance = { viewModel.useSecondChance() },
                onRestart = { viewModel.startNewGame() }
            )
        }

        // Stats & Help Dialog
        if (showStatsDialog) {
            StatsDialog(
                highScore = gameState.highScore,
                highestCombo = gameState.maxComboSession,
                totalLines = gameState.linesClearedSession,
                gamesPlayed = 1,
                onDismiss = { showStatsDialog = false }
            )
        }

        // Interactive Semi-Transparent Tutorial Overlay for new players
        TutorialOverlay(
            visible = showTutorial,
            onDismiss = { viewModel.dismissTutorial() },
            soundManager = viewModel.soundManager
        )
    }
}

@Composable
private fun ArcadeBannerSlot() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF131728),
                        Color(0xFF1F263E),
                        Color(0xFF131728)
                    )
                )
            )
            .border(1.dp, Color(0xFF283050), RoundedCornerShape(10.dp))
            .testTag("admob_banner_slot"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "BLOCK BLAST ARENA • MASTER THE COMBOS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E9BB0),
                letterSpacing = 0.5.sp
            )
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
