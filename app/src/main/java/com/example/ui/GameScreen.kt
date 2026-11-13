package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.audio.AppHaptics
import com.example.audio.HapticEffect
import com.example.model.BlockShape
import com.example.model.GameMode
import com.example.ui.components.ComboMultiplierHud
import com.example.ui.components.DailyLeaderboardDialog
import com.example.ui.components.DailyMissionsDialog
import com.example.ui.components.GridBoard
import com.example.ui.components.GameOverDialog
import com.example.ui.components.LeaderboardTab
import com.example.ui.components.PieceDeck
import com.example.ui.components.PieceSlot
import com.example.ui.components.ScoreHeader
import com.example.ui.components.SettingsDialog
import com.example.ui.components.ShapeRenderer
import com.example.ui.components.StatsDialog
import com.example.ui.components.TutorialOverlay
import com.example.ui.theme.DarkBackground
import com.example.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    val showTutorial by viewModel.showTutorial.collectAsState()
    val density = LocalDensity.current

    // Haptic feedback & context
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Dialog state
    var showStatsDialog by remember { mutableStateOf(false) }
    var showMissionsDialog by remember { mutableStateOf(false) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var leaderboardInitialTab by remember { mutableStateOf(LeaderboardTab.ALL_TIME) }
    var isLifecyclePaused by remember { mutableStateOf(false) }

    // Flush active game session, pause Time Attack countdown, and manage music when app backgrounds
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    isLifecyclePaused = true
                    viewModel.setTimeAttackPaused(true)
                    viewModel.setMusicForegrounded(false)
                    viewModel.flushSession(syncToCloud = true)
                }
                Lifecycle.Event.ON_RESUME -> {
                    isLifecyclePaused = false
                    viewModel.setMusicForegrounded(true)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.setMusicForegrounded(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Pause Time Attack clock while tutorial, dialogs, game over, or lifecycle pause are active
    LaunchedEffect(
        isLifecyclePaused,
        showTutorial,
        showStatsDialog,
        showMissionsDialog,
        showLeaderboardDialog,
        showSettingsDialog,
        gameState.isGameOver,
        gameState.gameMode
    ) {
        val shouldPause = isLifecyclePaused ||
            showTutorial ||
            showStatsDialog ||
            showMissionsDialog ||
            showLeaderboardDialog ||
            showSettingsDialog ||
            gameState.isGameOver
        viewModel.setTimeAttackPaused(shouldPause)
    }

    // Observe and perform tactile vibrations on placement, line clears, pickups, and game over
    LaunchedEffect(viewModel) {
        viewModel.hapticEvent.collect { effect ->
            AppHaptics.perform(context, effect, hapticFeedback)
        }
    }

    // Board layout tracking for drag and drop
    var boardBounds by remember { mutableStateOf<Rect?>(null) }
    var boardCellSizePx by remember { mutableFloatStateOf(0f) }

    // Root coordinate tracking of screen container to account for status/nav bar insets
    var containerRootTopLeft by remember { mutableStateOf(Offset.Zero) }

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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .onGloballyPositioned { coordinates ->
                containerRootTopLeft = coordinates.boundsInRoot().topLeft
            }
            .testTag("game_screen")
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Dynamic board sizing:
        // Reserve vertical space for compact ScoreHeader (~164dp + optional mode banner) and PieceDeck (~110dp)
        val reservedVerticalDp = if (gameState.gameMode != GameMode.CLASSIC) 316.dp else 294.dp
        val maxBoardByHeight = (screenHeight - reservedVerticalDp).coerceAtLeast(220.dp)
        val maxBoardByWidth = screenWidth - 24.dp
        val boardSizeDp = minOf(maxBoardByWidth, maxBoardByHeight).coerceIn(240.dp, 400.dp)

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
                isSoundEnabled = gameState.isSoundEnabled,
                isHapticsEnabled = gameState.isHapticsEnabled,
                canUndo = gameState.canUndo,
                gameMode = gameState.gameMode,
                dailyGlobalRecord = gameState.dailyGlobalRecord,
                dailyTargetScore = gameState.dailyTargetScore,
                dailyStreak = viewModel.getDailyStreak(),
                dailyPuzzleTitle = gameState.dailyPuzzleTitle,
                hammerCount = gameState.hammerCount,
                isHammerActive = gameState.isHammerActive,
                unclaimedMissions = gameState.unclaimedMissionsCount,
                difficultyLevel = gameState.difficultyLevel,
                difficultyTitle = gameState.difficultyTitle,
                difficultyProgress = gameState.difficultyProgress,
                nextDifficultyScore = gameState.nextDifficultyScore,
                timeRemainingMs = gameState.timeRemainingMs,
                timeMaxMs = gameState.timeMaxMs,
                lastTimeBonusSeconds = gameState.lastTimeBonusSeconds,
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onShowSettings = { showSettingsDialog = true },
                onShowStats = { showStatsDialog = true },
                onShowTutorial = { viewModel.openTutorial() },
                onShowLeaderboard = {
                    leaderboardInitialTab = LeaderboardTab.ALL_TIME
                    showLeaderboardDialog = true
                },
                onShowDailyLeaderboard = {
                    leaderboardInitialTab = LeaderboardTab.DAILY
                    showLeaderboardDialog = true
                },
                onSignOut = onSignOut,
                onUndo = { viewModel.undoLastMove() },
                onToggleHammer = { viewModel.toggleHammerMode() },
                onShowMissions = { showMissionsDialog = true },
                onSwitchMode = { mode -> viewModel.switchGameMode(mode) },
                onRestart = { viewModel.startNewGame() }
            )

            // 2. Center Section: 8x8 Game Board + Live Combo Multiplier HUD Overlay
            val ghostColorId = draggingShape?.colorId
                ?: gameState.selectedShapeIndex?.let { gameState.activePieces.getOrNull(it)?.colorId }

            val prospectiveLinesCount =
                gameState.prospectiveRows.size + gameState.prospectiveCols.size

            Box(
                modifier = Modifier.size(boardSizeDp),
                contentAlignment = Alignment.Center
            ) {
                GridBoard(
                    gameState = gameState,
                    ghostColorId = ghostColorId,
                    onCellTapped = { row, col ->
                        if (gameState.isHammerActive) {
                            viewModel.useHammerOnCell(row, col)
                        } else {
                            val selectedIndex = gameState.selectedShapeIndex
                            if (selectedIndex != null) {
                                viewModel.placeShape(selectedIndex, row, col)
                            }
                        }
                    },
                    onBoardPositioned = { bounds, cellSizePx ->
                        boardBounds = bounds
                        boardCellSizePx = cellSizePx
                    },
                    modifier = Modifier.fillMaxSize()
                )

                ComboMultiplierHud(
                    comboStreak = gameState.comboStreak,
                    comboEventId = gameState.comboEventId,
                    lastClearedLinesCount = gameState.lastClearedLinesCount,
                    prospectiveLinesCount = prospectiveLinesCount,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }

            // 3. Bottom Section: Active Piece Pool (3 slots)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hammer mode active banner
                AnimatedVisibility(
                    visible = gameState.isHammerActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFFF6F00))))
                            .clickable { viewModel.toggleHammerMode() }
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🔨 TAP ANY FILLED BLOCK TO SMASH (OR TAP TO CANCEL)",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Tap mode instruction if a piece is selected
                AnimatedVisibility(
                    visible = gameState.selectedShapeIndex != null && !gameState.isHammerActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2640))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Tap any cell on the grid to place",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
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
                        val currentDragging = draggingIndex
                        if (bounds != null && shape != null && boardCellSizePx > 0f && currentDragging != null) {
                            // Target location of shape top-left anchor in root coordinates
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
                                viewModel.updateHoverPreview(currentDragging, targetRow, targetCol)
                            } else {
                                viewModel.updateHoverPreview(currentDragging, null, null)
                            }
                        }
                    },
                    onDragEnd = {
                        val currentDragging = draggingIndex
                        val hoverR = gameState.hoverRow
                        val hoverC = gameState.hoverCol

                        if (currentDragging != null && hoverR != null && hoverC != null) {
                            val placed = viewModel.placeShape(currentDragging, hoverR, hoverC)
                            if (!placed) {
                                viewModel.updateHoverPreview(currentDragging, null, null)
                            }
                        } else if (currentDragging != null) {
                            viewModel.updateHoverPreview(currentDragging, null, null)
                        }
                        draggingIndex = null
                    }
                )
            }
        }

        // Floating Drag Overlay (renders above entire screen during drag gesture)
        if (draggingShape != null && boardCellSizePx > 0f) {
            val shape = draggingShape
            val shapeWidthPx = shape.width * boardCellSizePx
            val shapeHeightPx = shape.height * boardCellSizePx
            val localTouchX = dragTouchPos.x - containerRootTopLeft.x
            val localTouchY = dragTouchPos.y - containerRootTopLeft.y
            val overlayPieceLeftX = localTouchX - (shapeWidthPx / 2f)
            val overlayPieceTopY = (localTouchY - liftOffsetPx) - (shapeHeightPx / 2f)

            val cellUnitDp = with(density) { boardCellSizePx.toDp() }

            Box(
                modifier = Modifier
                    .offset { IntOffset(overlayPieceLeftX.toInt(), overlayPieceTopY.toInt()) }
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
                canUndo = gameState.canUndo,
                gameMode = gameState.gameMode,
                gameOverReason = gameState.gameOverReason,
                dailyTargetScore = gameState.dailyTargetScore,
                dailyGlobalRecord = gameState.dailyGlobalRecord,
                hammerCount = gameState.hammerCount,
                onUndo = { viewModel.undoLastMove() },
                onSecondChance = { viewModel.useSecondChance() },
                onUseHammer = { viewModel.activateHammerFromGameOver() },
                onShowLeaderboard = {
                    leaderboardInitialTab = LeaderboardTab.DAILY
                    showLeaderboardDialog = true
                },
                onShowGlobalLeaderboard = {
                    leaderboardInitialTab = LeaderboardTab.ALL_TIME
                    showLeaderboardDialog = true
                },
                onRestart = { viewModel.startNewGame() }
            )
        }

        // Global & Daily Leaderboard Dialog
        if (showLeaderboardDialog) {
            DailyLeaderboardDialog(
                entries = gameState.leaderboardEntries,
                isLoading = gameState.isLeaderboardLoading,
                errorMessage = gameState.leaderboardError,
                dailyFormattedDate = gameState.dailyFormattedDate,
                dailyPuzzleTitle = gameState.dailyPuzzleTitle,
                currentUserId = gameState.currentUserId,
                playerBestScore = if (gameState.gameMode == GameMode.DAILY_CHALLENGE) {
                    maxOf(gameState.highScore, gameState.score)
                } else {
                    gameState.leaderboardEntries.firstOrNull { it.userId == gameState.currentUserId }?.score?.toInt() ?: 0
                },
                gameMode = gameState.gameMode,
                onPlayDaily = { viewModel.switchGameMode(GameMode.DAILY_CHALLENGE) },
                onDismiss = { showLeaderboardDialog = false },
                globalEntries = gameState.globalLeaderboardEntries,
                isGlobalLoading = gameState.isGlobalLeaderboardLoading,
                globalErrorMessage = gameState.globalLeaderboardError,
                playerTotalScore = gameState.totalAllTimeScore,
                initialTab = leaderboardInitialTab
            )
        }

        // Daily Missions Dialog
        if (showMissionsDialog) {
            DailyMissionsDialog(
                missions = gameState.dailyMissions,
                hammerCount = gameState.hammerCount,
                onClaimReward = { missionId -> viewModel.claimMissionReward(missionId) },
                onDismiss = { showMissionsDialog = false }
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

        // Settings Menu Dialog (Background Music, Sound Effects & Haptics)
        if (showSettingsDialog) {
            SettingsDialog(
                isMusicEnabled = gameState.isMusicEnabled,
                isSoundEnabled = gameState.isSoundEnabled,
                isHapticsEnabled = gameState.isHapticsEnabled,
                onToggleMusic = { viewModel.toggleMusic() },
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onOpenTutorial = { viewModel.openTutorial() },
                onDismiss = { showSettingsDialog = false }
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
