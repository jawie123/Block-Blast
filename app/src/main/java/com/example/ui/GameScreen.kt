package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ads.AdUiState
import com.example.ads.RewardedAdController
import com.example.ads.RewardedAdRewardType
import com.example.ads.findActivity
import com.example.audio.AppHaptics
import com.example.audio.HapticEffect
import com.example.model.BlockShape
import com.example.model.BoardThemeCatalog
import com.example.model.BoardThemeSpec
import com.example.model.DailyLoginRewardSchedule
import com.example.model.GameMode
import com.example.model.ShopBoosterType
import com.example.ui.components.BoosterBar
import com.example.ui.components.ComboMultiplierHud
import com.example.ui.components.DailyLeaderboardDialog
import com.example.ui.components.DailyLoginRewardDialog
import com.example.ui.components.DailyMissionsDrawer
import com.example.ui.components.GridBoard
import com.example.ui.components.GameOverDialog
import com.example.ui.components.LeaderboardTab
import com.example.ui.components.PieceDeck
import com.example.ui.components.PieceSlot
import com.example.ui.components.ScoreHeader
import com.example.ui.components.SettingsDialog
import com.example.ui.components.ShapeRenderer
import com.example.ui.components.ShopDialog
import com.example.ui.components.ShopTab
import com.example.ui.components.StatsDialog
import com.example.ui.components.TutorialOverlay
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.DarkBackground
import com.example.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    adCoordinator: RewardedAdController? = null,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    val showTutorial by viewModel.showTutorial.collectAsState()
    val adState = adCoordinator?.adState?.collectAsState()?.value ?: AdUiState()
    val density = LocalDensity.current

    // Haptic feedback & context
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Dialog & drawer state
    var showStatsDialog by remember { mutableStateOf(false) }
    var showMissionsDrawer by remember { mutableStateOf(false) }
    var showLoginRewardDialog by remember { mutableStateOf(false) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showShopDialog by remember { mutableStateOf(false) }
    var shopInitialTab by remember { mutableStateOf(ShopTab.THEMES) }
    var freeBoosterPromptType by remember { mutableStateOf<RewardedAdRewardType?>(null) }
    var leaderboardInitialTab by remember { mutableStateOf(LeaderboardTab.HIGH_SCORES) }
    var isLifecyclePaused by remember { mutableStateOf(false) }

    val activeTheme = remember(gameState.selectedThemeId) {
        BoardThemeCatalog.byId(gameState.selectedThemeId)
    }

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

    // Pause Time Attack clock while tutorial, dialogs, game over, ad playback, or lifecycle pause are active
    LaunchedEffect(
        isLifecyclePaused,
        adState.isAdShowing,
        showTutorial,
        showStatsDialog,
        showMissionsDrawer,
        showLoginRewardDialog,
        showLeaderboardDialog,
        showSettingsDialog,
        showShopDialog,
        freeBoosterPromptType,
        gameState.isGameOver,
        gameState.gameMode
    ) {
        val shouldPause = isLifecyclePaused ||
            adState.isAdShowing ||
            showTutorial ||
            showStatsDialog ||
            showMissionsDrawer ||
            showLoginRewardDialog ||
            showLeaderboardDialog ||
            showSettingsDialog ||
            showShopDialog ||
            freeBoosterPromptType != null ||
            gameState.isGameOver
        viewModel.setTimeAttackPaused(shouldPause)
    }

    val launchRewardedAd: (RewardedAdRewardType) -> Unit = { rewardType ->
        val activity = context.findActivity()
        if (adCoordinator != null && activity != null) {
            adCoordinator.showRewardedAd(
                activity = activity,
                rewardType = rewardType,
                onAdStarted = { viewModel.onRewardedAdStarted() },
                onRewardEarned = { earnedType ->
                    when (earnedType) {
                        RewardedAdRewardType.SECOND_CHANCE -> viewModel.grantSecondChanceAfterAd()
                        RewardedAdRewardType.FREE_HAMMER -> viewModel.grantFreeHammerAfterAd()
                        RewardedAdRewardType.FREE_SHUFFLE -> viewModel.grantFreeShuffleAfterAd()
                        RewardedAdRewardType.FREE_BOMB -> viewModel.grantFreeBombAfterAd()
                    }
                },
                onAdClosedOrFailed = { errorMsg ->
                    viewModel.onRewardedAdEnded(restoreMusicForeground = !isLifecyclePaused)
                    if (!errorMsg.isNullOrBlank()) {
                        viewModel.showTemporaryBanner(errorMsg)
                    }
                }
            )
        } else {
            if (rewardType == RewardedAdRewardType.SECOND_CHANCE) {
                viewModel.useSecondChance()
            } else {
                viewModel.showTemporaryBanner("Ads unavailable right now.")
            }
        }
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

    // Adaptive visual drag lift offset (in pixels) so thumb never obscures the block
    val liftOffsetPx = with(density) {
        if (boardCellSizePx > 0f) {
            (boardCellSizePx * 1.85f).coerceIn(48.dp.toPx(), 84.dp.toPx())
        } else {
            72.dp.toPx()
        }
    }

    fun updateDragHover(pieceIndex: Int, touchPosRoot: Offset) {
        val bounds = boardBounds ?: return
        if (boardCellSizePx <= 0f) return
        val latestState = viewModel.gameState.value
        val shape = latestState.activePieces.getOrNull(pieceIndex) ?: return

        val pieceCenterX = touchPosRoot.x
        val pieceCenterY = touchPosRoot.y - liftOffsetPx

        val shapeWidthPx = shape.width * boardCellSizePx
        val shapeHeightPx = shape.height * boardCellSizePx
        val pieceLeftX = pieceCenterX - (shapeWidthPx / 2f)
        val pieceTopY = pieceCenterY - (shapeHeightPx / 2f)

        val exactColFloat = (pieceLeftX - bounds.left) / boardCellSizePx
        val exactRowFloat = (pieceTopY - bounds.top) / boardCellSizePx
        val roundedCol = Math.round(exactColFloat)
        val roundedRow = Math.round(exactRowFloat)

        if (roundedRow in -1..8 && roundedCol in -1..8) {
            if (viewModel.canPlaceShape(latestState.grid, shape, roundedRow, roundedCol)) {
                viewModel.updateHoverPreview(pieceIndex, roundedRow, roundedCol)
                return
            }
            // Magnetic snap: check nearest valid placement within 0.72 cells
            var bestCandidate: Pair<Int, Int>? = null
            var bestDistSq = 0.72f * 0.72f
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val candRow = roundedRow + dr
                    val candCol = roundedCol + dc
                    val dRow = candRow - exactRowFloat
                    val dCol = candCol - exactColFloat
                    val distSq = dRow * dRow + dCol * dCol
                    if (distSq <= bestDistSq && viewModel.canPlaceShape(latestState.grid, shape, candRow, candCol)) {
                        bestDistSq = distSq
                        bestCandidate = Pair(candRow, candCol)
                    }
                }
            }
            if (bestCandidate != null) {
                viewModel.updateHoverPreview(pieceIndex, bestCandidate.first, bestCandidate.second)
            } else {
                viewModel.updateHoverPreview(pieceIndex, null, null)
            }
        } else {
            viewModel.updateHoverPreview(pieceIndex, null, null)
        }
    }

    // Active dragging shape
    val draggingShape = draggingIndex?.let { gameState.activePieces.getOrNull(it) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(activeTheme.screenBackground)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .onGloballyPositioned { coordinates ->
                    val topLeft = coordinates.boundsInRoot().topLeft
                    if (containerRootTopLeft != topLeft) {
                        containerRootTopLeft = topLeft
                    }
                }
                .testTag("game_screen")
        ) {
        val isCompactHeight = maxHeight < 580.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Section: Score & Controls
            ScoreHeader(
                score = gameState.score,
                highScore = gameState.highScore,
                comboStreak = gameState.comboStreak,
                isSoundEnabled = gameState.isSoundEnabled,
                isHapticsEnabled = gameState.isHapticsEnabled,
                canUndo = gameState.canUndo,
                isGuestMode = gameState.currentUserId.isBlank(),
                gameMode = gameState.gameMode,
                dailyGlobalRecord = gameState.dailyGlobalRecord,
                dailyTargetScore = gameState.dailyTargetScore,
                dailyStreak = gameState.dailyStreak,
                isDailyCompletedToday = gameState.isDailyCompletedToday,
                dailyPuzzleTitle = gameState.dailyPuzzleTitle,
                hammerCount = gameState.hammerCount,
                isHammerActive = gameState.isHammerActive,
                hintCount = gameState.hintCount,
                isHintActive = gameState.activeHint != null,
                unclaimedMissions = gameState.unclaimedMissionsCount,
                coinsBalance = gameState.coinsBalance,
                loginStreak = gameState.loginStreak,
                canClaimDailyReward = gameState.canClaimDailyLoginReward,
                difficultyLevel = gameState.difficultyLevel,
                difficultyTitle = gameState.difficultyTitle,
                difficultyProgress = gameState.difficultyProgress,
                nextDifficultyScore = gameState.nextDifficultyScore,
                unlockedShapeVariety = gameState.unlockedShapeVariety,
                totalShapeVariety = gameState.totalShapeVariety,
                timeRemainingMs = gameState.timeRemainingMs,
                timeMaxMs = gameState.timeMaxMs,
                lastTimeBonusSeconds = gameState.lastTimeBonusSeconds,
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onShowSettings = { showSettingsDialog = true },
                onShowStats = { showStatsDialog = true },
                onShowTutorial = { viewModel.openTutorial() },
                onShowLeaderboard = {
                    leaderboardInitialTab = LeaderboardTab.HIGH_SCORES
                    showLeaderboardDialog = true
                },
                onShowDailyLeaderboard = {
                    leaderboardInitialTab = LeaderboardTab.DAILY
                    showLeaderboardDialog = true
                },
                onSignOut = onSignOut,
                onUndo = { viewModel.undoLastMove() },
                onToggleHammer = {
                    if (gameState.hammerCount <= 0 && !gameState.isHammerActive) {
                        if (adCoordinator != null) {
                            freeBoosterPromptType = RewardedAdRewardType.FREE_HAMMER
                        } else {
                            viewModel.toggleHammerMode()
                        }
                    } else {
                        viewModel.toggleHammerMode()
                    }
                },
                onUseHint = { viewModel.useHint() },
                onShowMissions = { showMissionsDrawer = true },
                onShowDailyRewards = { showLoginRewardDialog = true },
                onOpenShop = {
                    shopInitialTab = ShopTab.THEMES
                    showShopDialog = true
                },
                onSwitchMode = { mode -> viewModel.switchGameMode(mode) },
                onRestart = { viewModel.startNewGame() },
                compactMode = isCompactHeight,
                theme = activeTheme
            )

            // 2. Center Section: 8x8 Game Board + Live Combo Multiplier HUD Overlay
            val ghostColorId = draggingShape?.colorId
                ?: gameState.selectedShapeIndex?.let { gameState.activePieces.getOrNull(it)?.colorId }

            val prospectiveLinesCount =
                gameState.prospectiveRows.size + gameState.prospectiveCols.size

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = if (isCompactHeight) 2.dp else 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val boardSizeDp = minOf(maxWidth, maxHeight).coerceIn(0.dp, 460.dp)

                Box(
                    modifier = Modifier.size(boardSizeDp),
                    contentAlignment = Alignment.Center
                ) {
                    GridBoard(
                        gameState = gameState,
                        ghostColorId = ghostColorId,
                        onCellTapped = { row, col ->
                            when {
                                gameState.isBombActive -> viewModel.useBombOnCell(row, col)
                                gameState.isHammerActive -> viewModel.useHammerOnCell(row, col)
                                else -> {
                                    val hint = gameState.activeHint
                                    if (hint != null && Pair(row, col) in hint.cells) {
                                        viewModel.placeShape(hint.pieceIndex, hint.row, hint.col)
                                    } else {
                                        val selectedIndex = gameState.selectedShapeIndex
                                        if (selectedIndex != null) {
                                            viewModel.placeShape(selectedIndex, row, col)
                                        }
                                    }
                                }
                            }
                        },
                        onBoardPositioned = { bounds, cellSizePx ->
                            if (boardBounds != bounds) boardBounds = bounds
                            if (boardCellSizePx != cellSizePx) boardCellSizePx = cellSizePx
                        },
                        theme = activeTheme,
                        modifier = Modifier.fillMaxSize()
                    )

                    ComboMultiplierHud(
                        comboStreak = gameState.comboStreak,
                        comboEventId = gameState.comboEventId,
                        lastClearedLinesCount = gameState.lastClearedLinesCount,
                        prospectiveLinesCount = prospectiveLinesCount,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                    )
                }

                // Floating instruction pill (Hammer / Bomb / Tap to Place)
                androidx.compose.animation.AnimatedVisibility(
                    visible = gameState.isHammerActive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFFF6F00))))
                            .clickable { viewModel.toggleHammerMode() }
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🔨 TAP ANY FILLED BLOCK TO SMASH (TAP TO CANCEL)",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = gameState.isBombActive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF5252), Color(0xFFFF6D00))))
                            .clickable { viewModel.toggleBombMode() }
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                            .testTag("bomb_active_instruction_pill")
                    ) {
                        Text(
                            text = "💣 TAP BOARD TO BLAST 3×3 ZONE (TAP TO CANCEL)",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = gameState.selectedShapeIndex != null && !gameState.isHammerActive && !gameState.isBombActive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xEE1E2640))
                            .border(1.dp, activeTheme.accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Tap any cell on the grid to place",
                            color = activeTheme.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 3. Dedicated Booster Dock (Hammer, Hint, Shuffle, 3x3 Bomb, Shop)
            BoosterBar(
                hammerCount = gameState.hammerCount,
                isHammerActive = gameState.isHammerActive,
                hintCount = gameState.hintCount,
                isHintActive = gameState.activeHint != null,
                shuffleCount = gameState.shuffleCount,
                bombCount = gameState.bombCount,
                isBombActive = gameState.isBombActive,
                coinsBalance = gameState.coinsBalance,
                onToggleHammer = {
                    if (gameState.hammerCount <= 0 && !gameState.isHammerActive) {
                        if (adCoordinator != null) {
                            freeBoosterPromptType = RewardedAdRewardType.FREE_HAMMER
                        } else {
                            viewModel.toggleHammerMode()
                        }
                    } else {
                        viewModel.toggleHammerMode()
                    }
                },
                onUseHint = { viewModel.useHint() },
                onShuffle = {
                    if (gameState.shuffleCount <= 0 && adCoordinator != null) {
                        freeBoosterPromptType = RewardedAdRewardType.FREE_SHUFFLE
                    } else {
                        viewModel.useShuffle()
                    }
                },
                onToggleBomb = {
                    if (gameState.bombCount <= 0 && !gameState.isBombActive && adCoordinator != null) {
                        freeBoosterPromptType = RewardedAdRewardType.FREE_BOMB
                    } else {
                        viewModel.toggleBombMode()
                    }
                },
                onOpenShop = {
                    shopInitialTab = ShopTab.THEMES
                    showShopDialog = true
                },
                compactMode = isCompactHeight,
                theme = activeTheme
            )

            // 4. Bottom Section: Active Piece Pool (3 slots)
            PieceDeck(
                pieces = gameState.activePieces,
                canFitList = canFitList,
                selectedShapeIndex = gameState.selectedShapeIndex,
                draggingPieceIndex = draggingIndex,
                onSelectPiece = { idx -> viewModel.selectPieceForTap(idx) },
                onDragStart = { idx, initialTouchPos ->
                    val shape = viewModel.gameState.value.activePieces.getOrNull(idx)
                    if (shape != null && viewModel.canFitAnywhere(viewModel.gameState.value.grid, shape)) {
                        viewModel.soundManager.playPickup()
                        viewModel.triggerHaptic(HapticEffect.Pickup)
                        draggingIndex = idx
                        dragTouchPos = initialTouchPos
                        viewModel.selectPieceForTap(null)
                        updateDragHover(idx, initialTouchPos)
                    }
                },
                onDragMove = { delta ->
                    val currentDragging = draggingIndex ?: return@PieceDeck
                    val nextTouchPos = dragTouchPos + delta
                    dragTouchPos = nextTouchPos
                    updateDragHover(currentDragging, nextTouchPos)
                },
                onDragEnd = {
                    val currentDragging = draggingIndex
                    val latestState = viewModel.gameState.value
                    val hoverR = latestState.hoverRow
                    val hoverC = latestState.hoverCol

                    if (currentDragging != null && hoverR != null && hoverC != null) {
                        val placed = viewModel.placeShape(currentDragging, hoverR, hoverC)
                        if (!placed) {
                            viewModel.updateHoverPreview(currentDragging, null, null)
                        }
                    } else if (currentDragging != null) {
                        viewModel.updateHoverPreview(currentDragging, null, null)
                    }
                    draggingIndex = null
                },
                compactMode = isCompactHeight,
                theme = activeTheme
            )
        }

        // Floating Drag Overlay (reads dragTouchPos only in Layout phase for 0-recomposition drag)
        if (draggingShape != null && boardCellSizePx > 0f) {
            DraggingPieceOverlay(
                shape = draggingShape,
                boardCellSizePx = boardCellSizePx,
                liftOffsetPx = liftOffsetPx,
                dragTouchPosProvider = { dragTouchPos },
                containerTopLeftProvider = { containerRootTopLeft },
                theme = activeTheme
            )
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
                isRewardedAdReady = adState.isRewardedAdReady,
                isRewardedAdLoading = adState.isRewardedAdLoading,
                isAdShowing = adState.isAdShowing,
                onUndo = { viewModel.undoLastMove() },
                onSecondChance = { launchRewardedAd(RewardedAdRewardType.SECOND_CHANCE) },
                onUseHammer = { viewModel.activateHammerFromGameOver() },
                onWatchAdForHammer = { launchRewardedAd(RewardedAdRewardType.FREE_HAMMER) },
                onShowLeaderboard = {
                    leaderboardInitialTab = LeaderboardTab.DAILY
                    showLeaderboardDialog = true
                },
                onShowGlobalLeaderboard = {
                    leaderboardInitialTab = LeaderboardTab.HIGH_SCORES
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
                highScoreEntries = gameState.highScoreLeaderboardEntries,
                isHighScoreLoading = gameState.isHighScoreLeaderboardLoading,
                highScoreErrorMessage = gameState.highScoreLeaderboardError,
                playerOverallBestScore = maxOf(gameState.playerOverallBestScore, gameState.highScore, gameState.score),
                initialTab = leaderboardInitialTab,
                isGuestMode = gameState.currentUserId.isBlank(),
                onSignInWithGoogle = onSignOut
            )
        }

        // Daily Missions Dialog replaced by dedicated root-level DailyMissionsDrawer below

        // Daily Login Rewards Dialog
        if (showLoginRewardDialog) {
            val todayDate = remember(gameState.lastLoginClaimDate) { viewModel.getTodayDateString() }
            val yesterdayDate = remember(gameState.lastLoginClaimDate) { viewModel.getYesterdayDateString() }
            val calendarDays = remember(
                gameState.lastLoginClaimDate,
                gameState.loginStreak,
                todayDate,
                yesterdayDate
            ) {
                DailyLoginRewardSchedule.buildCalendar(
                    lastClaimDate = gameState.lastLoginClaimDate,
                    storedStreak = gameState.loginStreak,
                    todayDate = todayDate,
                    yesterdayDate = yesterdayDate
                )
            }
            DailyLoginRewardDialog(
                calendarDays = calendarDays,
                loginStreak = gameState.loginStreak,
                longestLoginStreak = gameState.longestLoginStreak,
                coinsBalance = gameState.coinsBalance,
                hammerCount = gameState.hammerCount,
                canClaimToday = gameState.canClaimDailyLoginReward,
                nextCycleDay = gameState.dailyLoginNextCycleDay,
                isLoading = gameState.isDailyLoginLoading,
                errorMessage = gameState.dailyLoginError,
                isGuestMode = gameState.currentUserId.isBlank(),
                isRewardedAdReady = adState.isRewardedAdReady,
                isRewardedAdLoading = adState.isRewardedAdLoading,
                isAdShowing = adState.isAdShowing,
                onClaimToday = { viewModel.claimDailyLoginReward() },
                onExchangeCoinsForHammer = { viewModel.exchangeCoinsForHammer() },
                onWatchAdForHammer = { launchRewardedAd(RewardedAdRewardType.FREE_HAMMER) },
                onSignInWithGoogle = {
                    showLoginRewardDialog = false
                    onSignOut()
                },
                onDismiss = { showLoginRewardDialog = false }
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

        // Settings Menu Dialog (Background Music, Sound Effects, Haptics & Privacy)
        if (showSettingsDialog) {
            SettingsDialog(
                isMusicEnabled = gameState.isMusicEnabled,
                isSoundEnabled = gameState.isSoundEnabled,
                isHapticsEnabled = gameState.isHapticsEnabled,
                isPrivacyOptionsRequired = adState.isPrivacyOptionsRequired,
                achievements = gameState.achievements,
                onToggleMusic = { viewModel.toggleMusic() },
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onShowPrivacyOptions = {
                    val activity = context.findActivity()
                    if (activity != null) {
                        adCoordinator?.showPrivacyOptionsForm(activity) { err ->
                            if (!err.isNullOrBlank()) {
                                viewModel.showTemporaryBanner("Privacy options unavailable right now.")
                            }
                        }
                    }
                },
                onOpenTutorial = { viewModel.openTutorial() },
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Out-of-Boosters Rewarded Ad Prompt (Hammer, Shuffle, or 3x3 Bomb)
        freeBoosterPromptType?.let { promptType ->
            FreeBoosterAdPromptDialog(
                rewardType = promptType,
                isRewardedAdReady = adState.isRewardedAdReady,
                isRewardedAdLoading = adState.isRewardedAdLoading,
                isAdShowing = adState.isAdShowing,
                onWatchAd = {
                    val requested = promptType
                    freeBoosterPromptType = null
                    launchRewardedAd(requested)
                },
                onOpenShopOrRewards = {
                    freeBoosterPromptType = null
                    shopInitialTab = ShopTab.BOOSTERS
                    showShopDialog = true
                },
                onDismiss = { freeBoosterPromptType = null }
            )
        }

        // Theme & Booster Coin Shop Dialog
        if (showShopDialog) {
            ShopDialog(
                coinsBalance = gameState.coinsBalance,
                selectedThemeId = gameState.selectedThemeId,
                unlockedThemeIds = gameState.unlockedThemeIds,
                hammerCount = gameState.hammerCount,
                hintCount = gameState.hintCount,
                shuffleCount = gameState.shuffleCount,
                bombCount = gameState.bombCount,
                isRewardedAdReady = adState.isRewardedAdReady,
                isRewardedAdLoading = adState.isRewardedAdLoading,
                isAdShowing = adState.isAdShowing,
                onSelectOrUnlockTheme = { themeId -> viewModel.selectOrUnlockTheme(themeId) },
                onBuyBooster = { booster -> viewModel.buyBoosterWithCoins(booster) },
                onWatchAdForBooster = if (adCoordinator != null) { booster ->
                    showShopDialog = false
                    when (booster) {
                        ShopBoosterType.HAMMER -> launchRewardedAd(RewardedAdRewardType.FREE_HAMMER)
                        ShopBoosterType.SHUFFLE -> launchRewardedAd(RewardedAdRewardType.FREE_SHUFFLE)
                        ShopBoosterType.BOMB -> launchRewardedAd(RewardedAdRewardType.FREE_BOMB)
                        ShopBoosterType.HINT -> Unit
                    }
                } else null,
                onDismiss = { showShopDialog = false },
                initialTab = shopInitialTab
            )
        }

        // Interactive Semi-Transparent Tutorial Overlay for new players
        TutorialOverlay(
            visible = showTutorial,
            onDismiss = { viewModel.dismissTutorial() },
            soundManager = viewModel.soundManager
        )
        }

        // Dedicated Edge-to-Edge Slide-In Daily Missions Drawer
        DailyMissionsDrawer(
            visible = showMissionsDrawer,
            missions = gameState.dailyMissions,
            coinsBalance = gameState.coinsBalance,
            hintCount = gameState.hintCount,
            hammerCount = gameState.hammerCount,
            onClaimReward = { missionId -> viewModel.claimMissionReward(missionId) },
            onDismiss = { showMissionsDrawer = false }
        )
    }
}

@Composable
private fun FreeBoosterAdPromptDialog(
    rewardType: RewardedAdRewardType,
    isRewardedAdReady: Boolean,
    isRewardedAdLoading: Boolean,
    isAdShowing: Boolean,
    onWatchAd: () -> Unit,
    onOpenShopOrRewards: () -> Unit,
    onDismiss: () -> Unit
) {
    val isHammer = rewardType == RewardedAdRewardType.FREE_HAMMER
    val isShuffle = rewardType == RewardedAdRewardType.FREE_SHUFFLE
    val emoji = when {
        isHammer -> "🔨"
        isShuffle -> "🔄"
        else -> "💣"
    }
    val title = when {
        isHammer -> "OUT OF HAMMERS!"
        isShuffle -> "OUT OF SHUFFLES!"
        else -> "OUT OF BOMBS!"
    }
    val body = when {
        isHammer -> "Watch a short video ad to earn +1 Free Hammer instantly, or craft one with Coins in the Shop."
        isShuffle -> "Watch a short video ad to earn +1 Free Shuffle instantly, or buy one with Coins in the Shop."
        else -> "Watch a short video ad to earn +1 Free 3×3 Bomb instantly, or buy one with Coins in the Shop."
    }
    val watchCta = when {
        !isRewardedAdReady && isRewardedAdLoading -> "LOADING AD..."
        isHammer -> "WATCH AD • +1 HAMMER"
        isShuffle -> "WATCH AD • +1 SHUFFLE"
        else -> "WATCH AD • +1 BOMB"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag(if (isHammer) "free_hammer_ad_dialog" else "free_booster_ad_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BoardBackground),
            border = BorderStroke(1.5.dp, BoardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = emoji,
                    fontSize = 36.sp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = body,
                    fontSize = 12.sp,
                    color = Color(0xFFB4C0D8)
                )

                Button(
                    onClick = onWatchAd,
                    enabled = !isAdShowing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(if (isHammer) "free_hammer_watch_ad_button" else "free_booster_watch_ad_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF0F111E),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = watchCta,
                            color = Color(0xFF0F111E),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenShopOrRewards,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("free_hammer_daily_rewards_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                ) {
                    Text(
                        text = "OPEN COIN SHOP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("free_hammer_cancel_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BoardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8E9BB0))
                ) {
                    Text(
                        text = "NOT NOW",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggingPieceOverlay(
    shape: BlockShape,
    boardCellSizePx: Float,
    liftOffsetPx: Float,
    dragTouchPosProvider: () -> Offset,
    containerTopLeftProvider: () -> Offset,
    theme: BoardThemeSpec
) {
    val density = LocalDensity.current
    val cellUnitDp = remember(boardCellSizePx, density) {
        with(density) { boardCellSizePx.toDp() }
    }
    val halfWidthPx = remember(shape.width, boardCellSizePx) {
        (shape.width * boardCellSizePx) / 2f
    }
    val halfHeightPx = remember(shape.height, boardCellSizePx) {
        (shape.height * boardCellSizePx) / 2f
    }

    Box(
        modifier = Modifier
            .offset {
                val touch = dragTouchPosProvider()
                val containerTopLeft = containerTopLeftProvider()
                val leftPx = (touch.x - containerTopLeft.x) - halfWidthPx
                val topPx = (touch.y - containerTopLeft.y - liftOffsetPx) - halfHeightPx
                IntOffset(leftPx.toInt(), topPx.toInt())
            }
            .shadow(14.dp)
            .testTag("dragging_piece_overlay")
    ) {
        ShapeRenderer(
            shape = shape,
            cellUnitDp = cellUnitDp,
            theme = theme
        )
    }
}

