package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.HapticEffect
import com.example.audio.SoundManager
import com.example.data.GameFirestoreRepository
import com.example.data.GamePreferences
import com.example.data.LocalSessionStorage
import com.example.model.Achievement
import com.example.model.AchievementCatalog
import com.example.model.AdventureCatalog
import com.example.model.AdventureGoalType
import com.example.model.BlockShape
import com.example.model.BoardThemeCatalog
import com.example.model.BoardThemeId
import com.example.model.DailyLoginRewardProfile
import com.example.model.DailyLoginRewardSchedule
import com.example.model.DailyLoginRewardTier
import com.example.model.DailyMission
import com.example.model.DailyMissionGenerator
import com.example.model.DailyPuzzleGenerator
import com.example.model.DifficultyTier
import com.example.model.GameMode
import com.example.model.GameOverReason
import com.example.model.GameState
import com.example.model.LineClearBurst
import com.example.model.MissionType
import com.example.model.PlacementHint
import com.example.model.SavedGameSessionData
import com.example.model.ScorePopup
import com.example.model.ShapeDefinitions
import com.example.model.ShopBoosterType
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * GameViewModel manages the 8x8 grid state (2D matrix/array), score tracking,
 * active piece pool, placement validation (canPlaceShape), game over detection,
 * procedural Daily Puzzle Challenge boards, Firestore Daily Leaderboard sync,
 * and automatic local + cloud session persistence & resume.
 */
class GameViewModel(
    private val context: Context,
    val soundManager: SoundManager,
    private val preferences: GamePreferences,
    private val localSessionStorage: LocalSessionStorage? = LocalSessionStorage(context),
    private val firestoreRepository: GameFirestoreRepository? = null,
    private val currentUserId: String = "",
    private val currentUserDisplayName: String = "Player"
) : ViewModel() {

    private val _gameState = MutableStateFlow(
        GameState(
            totalAllTimeScore = preferences.totalAllTimeScore,
            playerOverallBestScore = computeLocalOverallBestScore(),
            shuffleCount = preferences.shuffleCount,
            bombCount = preferences.bombCount,
            rotateCount = preferences.rotateCount,
            adventureLevelNumber = preferences.adventureCurrentLevel,
            adventureMaxUnlockedLevel = preferences.adventureMaxUnlockedLevel,
            selectedThemeId = preferences.selectedThemeId,
            unlockedThemeIds = preferences.getUnlockedThemeIds(),
            isMusicEnabled = preferences.isMusicEnabled,
            isSoundEnabled = preferences.isSoundEnabled,
            isHapticsEnabled = preferences.isHapticsEnabled,
            currentUserId = currentUserId,
            currentUserDisplayName = currentUserDisplayName
        )
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Observable individual score flows
    val score: StateFlow<Int> = _gameState.map { it.score }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val highScore: StateFlow<Int> = _gameState.map { it.highScore }
        .stateIn(viewModelScope, SharingStarted.Eagerly, preferences.highScore)

    // Haptic feedback event stream
    private val _hapticEvent = MutableSharedFlow<HapticEffect>(extraBufferCapacity = 16)
    val hapticEvent: SharedFlow<HapticEffect> = _hapticEvent.asSharedFlow()

    // Tutorial overlay state (shown automatically on first launch)
    private val _showTutorial = MutableStateFlow(!preferences.hasSeenTutorial)
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    private var hasUserInteractedThisSession = false
    private var lastAppliedSessionTimestamp = 0L
    private var leaderboardJob: Job? = null
    private var globalLeaderboardJob: Job? = null
    private var highScoreLeaderboardJob: Job? = null
    private var loginRewardJob: Job? = null
    private var timeAttackJob: Job? = null
    private val _isTimeAttackPaused = MutableStateFlow(false)

    fun setTimeAttackPaused(paused: Boolean) {
        _isTimeAttackPaused.value = paused
    }

    private fun stopTimeAttackTimer() {
        timeAttackJob?.cancel()
        timeAttackJob = null
    }

    private fun ensureTimeAttackTimer() {
        val state = _gameState.value
        if (state.gameMode == GameMode.TIME_ATTACK && !state.isGameOver && state.timeRemainingMs > 0L) {
            if (timeAttackJob?.isActive == true) return
            timeAttackJob = viewModelScope.launch {
                while (true) {
                    delay(100L)
                    val cur = _gameState.value
                    if (cur.gameMode != GameMode.TIME_ATTACK || cur.isGameOver) {
                        break
                    }
                    if (!_isTimeAttackPaused.value) {
                        tickTimeAttack(100L)
                    }
                }
            }
        } else {
            stopTimeAttackTimer()
        }
    }

    /**
     * Advances the Time Attack countdown clock by [elapsedMs].
     * Ends the game with [GameOverReason.TIME_UP] when remaining time reaches zero.
     */
    fun tickTimeAttack(elapsedMs: Long) {
        if (elapsedMs <= 0L) return
        val current = _gameState.value
        if (current.gameMode != GameMode.TIME_ATTACK || current.isGameOver) return

        val nextRemaining = (current.timeRemainingMs - elapsedMs).coerceAtLeast(0L)
        if (nextRemaining == 0L) {
            stopTimeAttackTimer()
            val isNewRecord = current.score > preferences.timeAttackHighScore
            if (isNewRecord) {
                preferences.timeAttackHighScore = current.score
            }
            val bestScore = maxOf(current.highScore, preferences.timeAttackHighScore)

            _gameState.update {
                it.copy(
                    timeRemainingMs = 0L,
                    isGameOver = true,
                    gameOverReason = GameOverReason.TIME_UP,
                    highScore = bestScore,
                    playerOverallBestScore = maxOf(it.playerOverallBestScore, bestScore),
                    isNewHighScore = it.isNewHighScore || isNewRecord,
                    isHammerActive = false,
                    isBombActive = false,
                    selectedShapeIndex = null,
                    hoverRow = null,
                    hoverCol = null,
                    hoverPreviewCells = emptySet(),
                    prospectiveRows = emptySet(),
                    prospectiveCols = emptySet()
                )
            }
            if (isNewRecord && current.score > 0) {
                syncLocalBestScoreToCloud()
            }
            soundManager.playGameOver()
            triggerHaptic(HapticEffect.GameOver)
            flushSession(syncToCloud = false)
        } else {
            _gameState.update { it.copy(timeRemainingMs = nextRemaining) }
        }
    }

    fun dismissTutorial() {
        preferences.hasSeenTutorial = true
        _showTutorial.value = false
    }

    fun openTutorial() {
        _showTutorial.value = true
    }

    val isHapticsEnabled: Boolean
        get() = soundManager.isHapticsEnabled

    fun triggerHaptic(effect: HapticEffect) {
        if (isHapticsEnabled) {
            _hapticEvent.tryEmit(effect)
        }
    }

    init {
        soundManager.isMusicEnabled = preferences.isMusicEnabled
        soundManager.isSoundEnabled = preferences.isSoundEnabled
        soundManager.isHapticsEnabled = preferences.isHapticsEnabled
        restoreSavedSessionOrStartNew()
        refreshLoginRewardStateFromPreferences()
        evaluateAndSyncAchievements(
            currentScore = _gameState.value.score,
            currentMaxCombo = _gameState.value.maxComboSession,
            notifyUnlocks = false
        )
        if (firestoreRepository != null && currentUserId.isNotBlank()) {
            observeDailyLeaderboard()
            observeGlobalLeaderboard()
            observeHighScoreLeaderboard()
            syncLocalBestScoreToCloud()
            syncAndObserveDailyLoginReward()
        }
    }

    /**
     * Restores the most recent unfinished local session on launch, and checks Firestore
     * asynchronously for a newer cloud-synced session (e.g. after reinstall or device switch).
     */
    private fun restoreSavedSessionOrStartNew() {
        val localSession = localSessionStorage?.loadLatestSession()
        if (isSessionResumable(localSession)) {
            applySavedSession(localSession!!, showResumedBanner = true)
        } else {
            startNewGame(mode = GameMode.CLASSIC, persistFreshState = false)
        }

        if (firestoreRepository != null && currentUserId.isNotBlank()) {
            viewModelScope.launch {
                val result = firestoreRepository.loadAllSessions()
                val sessions = result.getOrNull() ?: return@launch
                val today = getTodayDateString()
                val newestCloud = sessions
                    .filter { isSessionResumable(it, today) }
                    .maxByOrNull { it.clientUpdatedAt }
                    ?: return@launch

                if (!hasUserInteractedThisSession && newestCloud.clientUpdatedAt > lastAppliedSessionTimestamp) {
                    applySavedSession(newestCloud, showResumedBanner = true)
                    localSessionStorage?.saveSession(newestCloud)
                }
            }
        }
    }

    private fun isSessionResumable(
        session: SavedGameSessionData?,
        todayDate: String = getTodayDateString()
    ): Boolean {
        if (session == null) return false
        if (session.isGameOver) return false
        if (session.activePieces.none { it != null }) return false
        if (session.gameMode == GameMode.DAILY_CHALLENGE && session.dailyDate != todayDate) {
            return false
        }
        if (session.gameMode == GameMode.TIME_ATTACK && session.timeRemainingMs <= 0L) {
            return false
        }
        return true
    }

    private fun applySavedSession(session: SavedGameSessionData, showResumedBanner: Boolean) {
        lastUndoSnapshot = null
        lastAppliedSessionTimestamp = session.clientUpdatedAt
        val isDaily = session.gameMode == GameMode.DAILY_CHALLENGE
        val isTimeAttack = session.gameMode == GameMode.TIME_ATTACK
        val isAdventure = session.gameMode == GameMode.ADVENTURE
        val adventureLevelSpec = AdventureCatalog.level(session.adventureLevelNumber)
        if (isAdventure) {
            preferences.adventureCurrentLevel = adventureLevelSpec.levelNumber
        }
        val dateStr = session.dailyDate.ifEmpty { getTodayDateString() }
        val dateFormatted = getTodayFormattedString()
        val seed = computeDailySeed()
        val puzzleTitle = if (isDaily) DailyPuzzleGenerator.getPuzzleTitle(seed) else ""

        val savedHigh = when {
            isDaily -> maxOf(preferences.getDailyHighScore(dateStr), session.score)
            isTimeAttack -> maxOf(preferences.timeAttackHighScore, session.score)
            isAdventure -> maxOf(preferences.getAdventureHighScore(adventureLevelSpec.levelNumber), session.score)
            else -> maxOf(preferences.highScore, session.score)
        }

        val targetScore = 4500 + ((seed % 1000).toInt())
        val baseGlobalRecord = 8400 + ((seed % 1600).toInt())
        val currentState = _gameState.value
        val liveTopRecord = currentState.leaderboardEntries.firstOrNull()?.score?.toInt() ?: 0

        val dailyMissions = loadDailyMissions(getTodayDateString())
        val unclaimedCount = dailyMissions.count { it.isCompleted && !it.isClaimed }
        preferences.hammerCount = session.hammerCount

        val banner = when {
            showResumedBanner -> "SESSION RESUMED"
            isDaily -> "DAILY PUZZLE: $puzzleTitle"
            isTimeAttack -> "TIME ATTACK! 60s GO!"
            isAdventure -> "ADVENTURE LV ${adventureLevelSpec.levelNumber}: ${adventureLevelSpec.title.uppercase()}"
            else -> null
        }

        val resumedTier = DifficultyTier.fromScore(session.score)
        val resumedProgress = DifficultyTier.progressToNextTier(session.score)
        val resumedTimeMs = if (isTimeAttack) {
            session.timeRemainingMs.coerceIn(1_000L, TIME_ATTACK_MAX_MS)
        } else {
            TIME_ATTACK_INITIAL_MS
        }

        _gameState.value = GameState(
            grid = session.grid,
            activePieces = session.activePieces,
            score = session.score,
            highScore = savedHigh,
            comboStreak = session.comboStreak,
            maxComboSession = session.maxComboSession,
            linesClearedSession = session.linesClearedSession,
            isGameOver = session.isGameOver,
            isNewHighScore = false,
            secondChanceAvailable = session.secondChanceAvailable,
            selectedShapeIndex = null,
            hoverPreviewCells = emptySet(),
            prospectiveRows = emptySet(),
            prospectiveCols = emptySet(),
            clearingCells = emptySet(),
            comboBannerText = banner,
            scorePopups = emptyList(),
            gameMode = session.gameMode,
            dailyDate = dateStr,
            dailyFormattedDate = dateFormatted,
            dailyPuzzleTitle = puzzleTitle,
            dailyBatchIndex = session.dailyBatchIndex,
            dailyTargetScore = targetScore,
            dailyGlobalRecord = maxOf(baseGlobalRecord, liveTopRecord),
            canUndo = false,
            hammerCount = session.hammerCount,
            isHammerActive = false,
            hintCount = preferences.hintCount,
            activeHint = null,
            shuffleCount = preferences.shuffleCount,
            bombCount = preferences.bombCount,
            isBombActive = false,
            rotateCount = preferences.rotateCount,
            isRotateActive = false,
            gemCells = if (isAdventure) session.gemCells else emptySet(),
            iceCells = if (isAdventure) session.iceCells else emptyMap(),
            adventureLevelNumber = adventureLevelSpec.levelNumber,
            adventureMaxUnlockedLevel = preferences.adventureMaxUnlockedLevel,
            adventureGemsCollected = session.adventureGemsCollected,
            adventureIceBroken = session.adventureIceBroken,
            adventureInitialIceTotal = adventureLevelSpec.initialIceCells.size,
            adventureLevelCompleted = false,
            adventureEarnedStars = 0,
            adventureLastRewardCoins = 0,
            selectedThemeId = preferences.selectedThemeId,
            unlockedThemeIds = preferences.getUnlockedThemeIds(),
            dailyMissions = dailyMissions,
            unclaimedMissionsCount = unclaimedCount,
            difficultyLevel = resumedTier.level,
            difficultyTitle = resumedTier.title,
            difficultyProgress = resumedProgress,
            nextDifficultyScore = resumedTier.maxScoreExclusive,
            unlockedShapeVariety = ShapeDefinitions.getUnlockedVarietyCount(session.score),
            totalShapeVariety = ShapeDefinitions.totalShapeVarietyCount,
            timeRemainingMs = resumedTimeMs,
            timeMaxMs = TIME_ATTACK_MAX_MS,
            lastTimeBonusSeconds = 0,
            gameOverReason = GameOverReason.NO_MOVES,
            leaderboardEntries = currentState.leaderboardEntries,
            isLeaderboardLoading = currentState.isLeaderboardLoading,
            leaderboardError = currentState.leaderboardError,
            totalAllTimeScore = preferences.totalAllTimeScore,
            playerOverallBestScore = maxOf(currentState.playerOverallBestScore, computeLocalOverallBestScore(dateStr), savedHigh),
            globalLeaderboardEntries = currentState.globalLeaderboardEntries,
            isGlobalLeaderboardLoading = currentState.isGlobalLeaderboardLoading,
            globalLeaderboardError = currentState.globalLeaderboardError,
            highScoreLeaderboardEntries = currentState.highScoreLeaderboardEntries,
            isHighScoreLeaderboardLoading = currentState.isHighScoreLeaderboardLoading,
            highScoreLeaderboardError = currentState.highScoreLeaderboardError,
            isMusicEnabled = preferences.isMusicEnabled,
            isSoundEnabled = preferences.isSoundEnabled,
            isHapticsEnabled = preferences.isHapticsEnabled,
            dailyStreak = if (isDaily) {
                preferences.recordDailyParticipation(dateStr, getYesterdayDateString())
            } else {
                preferences.dailyStreak
            },
            isDailyCompletedToday = preferences.isDailyDateCompleted(dateStr),
            coinsBalance = preferences.coinsBalance,
            loginStreak = DailyLoginRewardSchedule.computeActiveStreak(
                lastClaimDate = preferences.lastLoginClaimDate,
                storedStreak = preferences.loginStreak,
                todayDate = dateStr,
                yesterdayDate = getYesterdayDateString()
            ),
            longestLoginStreak = preferences.longestLoginStreak,
            lastLoginClaimDate = preferences.lastLoginClaimDate,
            canClaimDailyLoginReward = DailyLoginRewardSchedule.canClaimToday(preferences.lastLoginClaimDate, dateStr),
            dailyLoginNextCycleDay = DailyLoginRewardSchedule.nextClaimCycleDay(
                lastClaimDate = preferences.lastLoginClaimDate,
                storedStreak = preferences.loginStreak,
                todayDate = dateStr,
                yesterdayDate = getYesterdayDateString()
            ),
            isDailyLoginLoading = currentState.isDailyLoginLoading,
            dailyLoginError = currentState.dailyLoginError,
            currentUserId = currentUserId,
            currentUserDisplayName = currentUserDisplayName
        )

        evaluateAndSyncAchievements(
            currentScore = session.score,
            currentMaxCombo = session.maxComboSession,
            notifyUnlocks = false
        )
        ensureTimeAttackTimer()

        if (showResumedBanner) {
            viewModelScope.launch {
                delay(1600)
                _gameState.update {
                    if (it.comboBannerText == "SESSION RESUMED") it.copy(comboBannerText = null) else it
                }
            }
        }
    }

    /**
     * Observes the global Daily Leaderboard in real time via Firestore snapshot listeners.
     */
    fun observeDailyLeaderboard() {
        val repo = firestoreRepository ?: return
        if (currentUserId.isBlank()) return
        val dateStr = getTodayDateString()
        leaderboardJob?.cancel()
        _gameState.update { it.copy(isLeaderboardLoading = true, leaderboardError = null) }

        leaderboardJob = viewModelScope.launch {
            repo.observeDailyLeaderboard(dateStr)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing daily leaderboard", rootError)
                    _gameState.update {
                        it.copy(
                            isLeaderboardLoading = false,
                            leaderboardError = rootError.message ?: "Unable to load leaderboard"
                        )
                    }
                }
                .collect { entries ->
                    val topScore = entries.firstOrNull()?.score?.toInt() ?: 0
                    _gameState.update {
                        it.copy(
                            leaderboardEntries = entries,
                            isLeaderboardLoading = false,
                            leaderboardError = null,
                            dailyGlobalRecord = maxOf(it.dailyGlobalRecord, topScore)
                        )
                    }
                }
        }
    }

    /**
     * Observes the All-Time Global Leaderboard in real time via Firestore snapshot listeners.
     */
    fun observeGlobalLeaderboard() {
        val repo = firestoreRepository ?: return
        if (currentUserId.isBlank()) return
        globalLeaderboardJob?.cancel()
        _gameState.update { it.copy(isGlobalLeaderboardLoading = true, globalLeaderboardError = null) }

        globalLeaderboardJob = viewModelScope.launch {
            repo.observeGlobalLeaderboard()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing global leaderboard", rootError)
                    _gameState.update {
                        it.copy(
                            isGlobalLeaderboardLoading = false,
                            globalLeaderboardError = rootError.message ?: "Unable to load global rankings"
                        )
                    }
                }
                .collect { entries ->
                    val cloudOwnTotal = entries.firstOrNull { it.userId == currentUserId }?.totalScore ?: 0L
                    if (cloudOwnTotal > preferences.totalAllTimeScore) {
                        preferences.totalAllTimeScore = cloudOwnTotal
                    }
                    _gameState.update {
                        it.copy(
                            globalLeaderboardEntries = entries,
                            totalAllTimeScore = maxOf(it.totalAllTimeScore, preferences.totalAllTimeScore),
                            isGlobalLeaderboardLoading = false,
                            globalLeaderboardError = null
                        )
                    }
                }
        }
    }

    /**
     * Observes the Global High Score Leaderboard (ordered by bestScore descending) in real time
     * so players can compare their single-game high score against other players worldwide.
     */
    fun observeHighScoreLeaderboard() {
        val repo = firestoreRepository ?: return
        if (currentUserId.isBlank()) return
        highScoreLeaderboardJob?.cancel()
        _gameState.update {
            it.copy(isHighScoreLeaderboardLoading = true, highScoreLeaderboardError = null)
        }

        highScoreLeaderboardJob = viewModelScope.launch {
            repo.observeHighScoreLeaderboard()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing high score leaderboard", rootError)
                    _gameState.update {
                        it.copy(
                            isHighScoreLeaderboardLoading = false,
                            highScoreLeaderboardError = rootError.message ?: "Unable to load high score rankings"
                        )
                    }
                }
                .collect { entries ->
                    val cloudOwnBest = entries.firstOrNull { it.userId == currentUserId }?.bestScore?.toInt() ?: 0
                    val localBest = computeLocalOverallBestScore()
                    _gameState.update {
                        it.copy(
                            highScoreLeaderboardEntries = entries,
                            playerOverallBestScore = maxOf(it.playerOverallBestScore, localBest, cloudOwnBest),
                            isHighScoreLeaderboardLoading = false,
                            highScoreLeaderboardError = null
                        )
                    }
                }
        }
    }

    private fun computeLocalOverallBestScore(dateStr: String = getTodayDateString()): Int {
        return maxOf(
            preferences.highScore,
            preferences.timeAttackHighScore,
            preferences.getDailyHighScore(dateStr)
        )
    }

    fun syncLocalBestScoreToCloud() {
        val repo = firestoreRepository ?: return
        if (currentUserId.isBlank()) return
        val localBest = maxOf(computeLocalOverallBestScore(), _gameState.value.score)
        val totalFloor = preferences.totalAllTimeScore
        val linesFloor = preferences.totalLinesCleared.toLong()
        val bestCombo = maxOf(preferences.highestCombo, _gameState.value.maxComboSession)
        if (localBest <= 0 && totalFloor <= 0L) return

        viewModelScope.launch {
            repo.syncHighScoreProfile(
                displayName = currentUserDisplayName,
                bestSingleGameScore = localBest,
                totalScoreFloor = totalFloor,
                totalLinesFloor = linesFloor,
                highestCombo = bestCombo
            )
        }
    }

    /**
     * Persists the active game session to both the local JSON file and Firestore.
     */
    fun flushSession(syncToCloud: Boolean = true) {
        val state = _gameState.value
        val now = System.currentTimeMillis()
        lastAppliedSessionTimestamp = now
        val session = SavedGameSessionData(
            gameMode = state.gameMode,
            grid = state.grid,
            activePieces = state.activePieces,
            score = state.score,
            comboStreak = state.comboStreak,
            maxComboSession = state.maxComboSession,
            linesClearedSession = state.linesClearedSession,
            isGameOver = state.isGameOver,
            secondChanceAvailable = state.secondChanceAvailable,
            dailyDate = state.dailyDate.ifEmpty { getTodayDateString() },
            dailyBatchIndex = state.dailyBatchIndex,
            hammerCount = state.hammerCount,
            timeRemainingMs = state.timeRemainingMs,
            clientUpdatedAt = now,
            adventureLevelNumber = state.adventureLevelNumber,
            adventureGemsCollected = state.adventureGemsCollected,
            adventureIceBroken = state.adventureIceBroken,
            gemCells = state.gemCells,
            iceCells = state.iceCells
        )
        localSessionStorage?.saveSessionBackground(session)

        if (syncToCloud &&
            state.gameMode != GameMode.TIME_ATTACK &&
            firestoreRepository != null &&
            currentUserId.isNotBlank()
        ) {
            viewModelScope.launch {
                firestoreRepository.saveSession(session)
            }
        }
    }

    /**
     * Returns the current 8x8 grid state as a 2D IntArray [Array<IntArray>].
     */
    fun getGridAs2DArray(): Array<IntArray> {
        val currentGrid = _gameState.value.grid
        return Array(8) { r ->
            IntArray(8) { c ->
                currentGrid.getOrNull(r)?.getOrNull(c) ?: 0
            }
        }
    }

    data class UndoSnapshot(
        val grid: List<List<Int>>,
        val activePieces: List<BlockShape?>,
        val score: Int,
        val highScore: Int,
        val comboStreak: Int,
        val maxComboSession: Int,
        val linesClearedSession: Int,
        val isGameOver: Boolean,
        val isNewHighScore: Boolean,
        val dailyBatchIndex: Int,
        val hammerCount: Int = 3,
        val shuffleCount: Int = 2,
        val bombCount: Int = 1,
        val rotateCount: Int = 3,
        val gemCells: Set<Pair<Int, Int>> = emptySet(),
        val iceCells: Map<Pair<Int, Int>, Int> = emptyMap(),
        val adventureGemsCollected: Int = 0,
        val adventureIceBroken: Int = 0,
        val adventureLevelCompleted: Boolean = false,
        val timeRemainingMs: Long = TIME_ATTACK_INITIAL_MS,
        val gameOverReason: GameOverReason = GameOverReason.NO_MOVES
    )

    private var lastUndoSnapshot: UndoSnapshot? = null

    fun computeDailySeed(): Long {
        val cal = Calendar.getInstance()
        return (cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)).toLong()
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun getTodayFormattedString(): String {
        return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
    }

    fun startAdventureLevel(levelNumber: Int) {
        val clamped = levelNumber.coerceIn(1, AdventureCatalog.totalLevels)
        val maxUnlocked = preferences.adventureMaxUnlockedLevel.coerceIn(1, AdventureCatalog.totalLevels)
        if (clamped > maxUnlocked) {
            showTemporaryBanner("COMPLETE LEVEL ${clamped - 1} TO UNLOCK!")
            return
        }
        preferences.adventureCurrentLevel = clamped
        stopTimeAttackTimer()
        startNewGame(mode = GameMode.ADVENTURE, persistFreshState = true, adventureLevelOverride = clamped)
    }

    fun advanceToNextAdventureLevel() {
        val currentLevel = _gameState.value.adventureLevelNumber
        val nextLevel = if (currentLevel < AdventureCatalog.totalLevels) currentLevel + 1 else 1
        startAdventureLevel(nextLevel)
    }

    fun getAdventureLevelStars(levelNumber: Int): Int {
        return preferences.getAdventureLevelStars(levelNumber)
    }

    /**
     * Initializes a new game for Classic, Time Attack, Daily Challenge, or Adventure mode.
     * In Daily Challenge mode, generates the deterministic procedural 8x8 board layout.
     */
    fun startNewGame(
        mode: GameMode = _gameState.value.gameMode,
        persistFreshState: Boolean = true,
        adventureLevelOverride: Int? = null
    ) {
        lastUndoSnapshot = null
        val isDaily = mode == GameMode.DAILY_CHALLENGE
        val isTimeAttack = mode == GameMode.TIME_ATTACK
        val isAdventure = mode == GameMode.ADVENTURE
        val dateStr = getTodayDateString()
        val dateFormatted = getTodayFormattedString()
        val seed = computeDailySeed()

        val adventureSpec = if (isAdventure) {
            val targetLv = (adventureLevelOverride ?: preferences.adventureCurrentLevel)
                .coerceIn(1, AdventureCatalog.totalLevels)
            preferences.adventureCurrentLevel = targetLv
            AdventureCatalog.level(targetLv)
        } else {
            AdventureCatalog.level(preferences.adventureCurrentLevel)
        }

        if (isDaily) {
            preferences.recordDailyParticipation(dateStr, getYesterdayDateString())
        }

        val initialGrid = when {
            isDaily -> DailyPuzzleGenerator.generateInitialBoard(seed)
            isAdventure -> adventureSpec.initialGrid
            else -> List(8) { List(8) { 0 } }
        }

        val puzzleTitle = if (isDaily) {
            DailyPuzzleGenerator.getPuzzleTitle(seed)
        } else {
            ""
        }

        val initialShapes = if (isDaily) {
            ShapeDefinitions.getDailyShapeBatch(seed, 0)
        } else {
            ShapeDefinitions.getRandomShapeBatch()
        }

        val high = when {
            isDaily -> preferences.getDailyHighScore(dateStr)
            isTimeAttack -> preferences.timeAttackHighScore
            isAdventure -> preferences.getAdventureHighScore(adventureSpec.levelNumber)
            else -> preferences.highScore
        }

        if (!isDaily) {
            preferences.gamesPlayed = preferences.gamesPlayed + 1
        }

        val targetScore = 4500 + ((seed % 1000).toInt())
        val baseGlobalRecord = 8400 + ((seed % 1600).toInt())
        val currentState = _gameState.value
        val liveTopRecord = currentState.leaderboardEntries.firstOrNull()?.score?.toInt() ?: 0

        val currentHammers = preferences.hammerCount
        val dailyMissions = loadDailyMissions(dateStr)
        val unclaimedCount = dailyMissions.count { it.isCompleted && !it.isClaimed }

        val banner = when {
            isDaily -> "DAILY PUZZLE: $puzzleTitle"
            isTimeAttack -> "TIME ATTACK! 60s GO!"
            isAdventure -> "LV ${adventureSpec.levelNumber} • ${adventureSpec.title.uppercase()}"
            else -> null
        }

        _gameState.value = GameState(
            grid = initialGrid,
            activePieces = initialShapes,
            score = 0,
            highScore = high,
            comboStreak = 0,
            maxComboSession = 0,
            linesClearedSession = 0,
            isGameOver = false,
            isNewHighScore = false,
            secondChanceAvailable = true,
            selectedShapeIndex = null,
            hoverPreviewCells = emptySet(),
            prospectiveRows = emptySet(),
            prospectiveCols = emptySet(),
            clearingCells = emptySet(),
            comboBannerText = banner,
            scorePopups = emptyList(),
            gameMode = mode,
            dailyDate = dateStr,
            dailyFormattedDate = dateFormatted,
            dailyPuzzleTitle = puzzleTitle,
            dailyBatchIndex = 0,
            dailyTargetScore = targetScore,
            dailyGlobalRecord = maxOf(baseGlobalRecord, liveTopRecord),
            canUndo = false,
            hammerCount = currentHammers,
            isHammerActive = false,
            hintCount = preferences.hintCount,
            activeHint = null,
            shuffleCount = preferences.shuffleCount,
            bombCount = preferences.bombCount,
            isBombActive = false,
            rotateCount = preferences.rotateCount,
            isRotateActive = false,
            gemCells = if (isAdventure) adventureSpec.initialGemCells else emptySet(),
            iceCells = if (isAdventure) adventureSpec.initialIceCells else emptyMap(),
            adventureLevelNumber = adventureSpec.levelNumber,
            adventureMaxUnlockedLevel = preferences.adventureMaxUnlockedLevel,
            adventureGemsCollected = 0,
            adventureIceBroken = 0,
            adventureInitialIceTotal = if (isAdventure) adventureSpec.initialIceCells.size else 0,
            adventureLevelCompleted = false,
            adventureEarnedStars = 0,
            adventureLastRewardCoins = 0,
            selectedThemeId = preferences.selectedThemeId,
            unlockedThemeIds = preferences.getUnlockedThemeIds(),
            dailyMissions = dailyMissions,
            unclaimedMissionsCount = unclaimedCount,
            difficultyLevel = DifficultyTier.STARTER.level,
            difficultyTitle = DifficultyTier.STARTER.title,
            difficultyProgress = 0f,
            nextDifficultyScore = DifficultyTier.STARTER.maxScoreExclusive,
            unlockedShapeVariety = ShapeDefinitions.getUnlockedVarietyCount(0),
            totalShapeVariety = ShapeDefinitions.totalShapeVarietyCount,
            timeRemainingMs = TIME_ATTACK_INITIAL_MS,
            timeMaxMs = TIME_ATTACK_MAX_MS,
            lastTimeBonusSeconds = 0,
            gameOverReason = GameOverReason.NO_MOVES,
            leaderboardEntries = currentState.leaderboardEntries,
            isLeaderboardLoading = currentState.isLeaderboardLoading,
            leaderboardError = currentState.leaderboardError,
            totalAllTimeScore = preferences.totalAllTimeScore,
            playerOverallBestScore = maxOf(currentState.playerOverallBestScore, computeLocalOverallBestScore(dateStr), high),
            globalLeaderboardEntries = currentState.globalLeaderboardEntries,
            isGlobalLeaderboardLoading = currentState.isGlobalLeaderboardLoading,
            globalLeaderboardError = currentState.globalLeaderboardError,
            highScoreLeaderboardEntries = currentState.highScoreLeaderboardEntries,
            isHighScoreLeaderboardLoading = currentState.isHighScoreLeaderboardLoading,
            highScoreLeaderboardError = currentState.highScoreLeaderboardError,
            isMusicEnabled = preferences.isMusicEnabled,
            isSoundEnabled = preferences.isSoundEnabled,
            isHapticsEnabled = preferences.isHapticsEnabled,
            dailyStreak = preferences.dailyStreak,
            isDailyCompletedToday = preferences.isDailyDateCompleted(dateStr),
            coinsBalance = preferences.coinsBalance,
            loginStreak = DailyLoginRewardSchedule.computeActiveStreak(
                lastClaimDate = preferences.lastLoginClaimDate,
                storedStreak = preferences.loginStreak,
                todayDate = dateStr,
                yesterdayDate = getYesterdayDateString()
            ),
            longestLoginStreak = preferences.longestLoginStreak,
            lastLoginClaimDate = preferences.lastLoginClaimDate,
            canClaimDailyLoginReward = DailyLoginRewardSchedule.canClaimToday(preferences.lastLoginClaimDate, dateStr),
            dailyLoginNextCycleDay = DailyLoginRewardSchedule.nextClaimCycleDay(
                lastClaimDate = preferences.lastLoginClaimDate,
                storedStreak = preferences.loginStreak,
                todayDate = dateStr,
                yesterdayDate = getYesterdayDateString()
            ),
            isDailyLoginLoading = currentState.isDailyLoginLoading,
            dailyLoginError = currentState.dailyLoginError,
            currentUserId = currentUserId,
            currentUserDisplayName = currentUserDisplayName
        )

        evaluateAndSyncAchievements(
            currentScore = 0,
            currentMaxCombo = 0,
            notifyUnlocks = false
        )
        ensureTimeAttackTimer()

        if (isTimeAttack) {
            viewModelScope.launch {
                delay(1400)
                _gameState.update {
                    if (it.comboBannerText == "TIME ATTACK! 60s GO!") it.copy(comboBannerText = null) else it
                }
            }
        }

        if (persistFreshState) {
            flushSession(syncToCloud = true)
        }
    }

    fun switchGameMode(mode: GameMode) {
        if (_gameState.value.gameMode == mode) return
        stopTimeAttackTimer()
        // Persist current mode state before switching
        flushSession(syncToCloud = false)

        val today = getTodayDateString()
        val savedForMode = localSessionStorage?.loadSession(mode)
        if (isSessionResumable(savedForMode, today)) {
            applySavedSession(savedForMode!!, showResumedBanner = true)
            return
        }

        startNewGame(mode = mode, persistFreshState = false)

        if (mode != GameMode.TIME_ATTACK && firestoreRepository != null && currentUserId.isNotBlank()) {
            viewModelScope.launch {
                val cloud = firestoreRepository.loadSession(mode).getOrNull()
                if (isSessionResumable(cloud, today) && _gameState.value.gameMode == mode && _gameState.value.score == 0) {
                    applySavedSession(cloud!!, showResumedBanner = true)
                    localSessionStorage?.saveSession(cloud)
                }
            }
        }
    }

    /**
     * Reverts the last piece placement, restoring the previous grid state, score,
     * piece pool, combo streak, and hammer count.
     */
    fun undoLastMove(): Boolean {
        val snapshot = lastUndoSnapshot ?: return false
        lastUndoSnapshot = null
        hasUserInteractedThisSession = true

        preferences.hammerCount = snapshot.hammerCount
        preferences.shuffleCount = snapshot.shuffleCount
        preferences.bombCount = snapshot.bombCount
        preferences.rotateCount = snapshot.rotateCount

        val revertedTier = DifficultyTier.fromScore(snapshot.score)
        val revertedProgress = DifficultyTier.progressToNextTier(snapshot.score)

        _gameState.update {
            it.copy(
                grid = snapshot.grid,
                activePieces = snapshot.activePieces,
                score = snapshot.score,
                highScore = snapshot.highScore,
                comboStreak = snapshot.comboStreak,
                comboEventId = 0L,
                lastClearedLinesCount = 0,
                maxComboSession = snapshot.maxComboSession,
                linesClearedSession = snapshot.linesClearedSession,
                isGameOver = snapshot.isGameOver,
                gameOverReason = snapshot.gameOverReason,
                isNewHighScore = snapshot.isNewHighScore,
                dailyBatchIndex = snapshot.dailyBatchIndex,
                hammerCount = snapshot.hammerCount,
                shuffleCount = snapshot.shuffleCount,
                bombCount = snapshot.bombCount,
                rotateCount = snapshot.rotateCount,
                gemCells = snapshot.gemCells,
                iceCells = snapshot.iceCells,
                adventureGemsCollected = snapshot.adventureGemsCollected,
                adventureIceBroken = snapshot.adventureIceBroken,
                adventureLevelCompleted = snapshot.adventureLevelCompleted,
                timeRemainingMs = snapshot.timeRemainingMs,
                lastTimeBonusSeconds = 0,
                isHammerActive = false,
                isBombActive = false,
                isRotateActive = false,
                activeHint = null,
                selectedShapeIndex = null,
                hoverRow = null,
                hoverCol = null,
                hoverPreviewCells = emptySet(),
                prospectiveRows = emptySet(),
                prospectiveCols = emptySet(),
                clearingCells = emptySet(),
                lineClearBursts = emptyList(),
                comboBannerText = "MOVE UNDONE",
                scorePopups = emptyList(),
                difficultyLevel = revertedTier.level,
                difficultyTitle = revertedTier.title,
                difficultyProgress = revertedProgress,
                nextDifficultyScore = revertedTier.maxScoreExclusive,
                unlockedShapeVariety = ShapeDefinitions.getUnlockedVarietyCount(snapshot.score),
                canUndo = false
            )
        }

        ensureTimeAttackTimer()
        soundManager.playPickup()
        triggerHaptic(HapticEffect.Pickup)
        flushSession(syncToCloud = true)
        return true
    }

    fun getDailyStreak(): Int = preferences.dailyStreak

    fun setMusicForegrounded(foregrounded: Boolean) {
        soundManager.setMusicForegrounded(foregrounded)
    }

    fun toggleMusic(): Boolean {
        val newVal = !preferences.isMusicEnabled
        preferences.isMusicEnabled = newVal
        soundManager.isMusicEnabled = newVal
        _gameState.update { it.copy(isMusicEnabled = newVal) }
        if (soundManager.isSoundEnabled) {
            soundManager.playClick()
        }
        return newVal
    }

    fun toggleSound(): Boolean {
        val newVal = !soundManager.isSoundEnabled
        soundManager.isSoundEnabled = newVal
        preferences.isSoundEnabled = newVal
        _gameState.update { it.copy(isSoundEnabled = newVal) }
        if (newVal) {
            soundManager.playClick()
        }
        return newVal
    }

    fun toggleHaptics(): Boolean {
        val newVal = !soundManager.isHapticsEnabled
        soundManager.isHapticsEnabled = newVal
        preferences.isHapticsEnabled = newVal
        _gameState.update { it.copy(isHapticsEnabled = newVal) }
        if (newVal) {
            triggerHaptic(HapticEffect.Pickup)
        }
        return newVal
    }

    fun selectPieceForTap(index: Int?) {
        val currentPiece = index?.let { _gameState.value.activePieces.getOrNull(it) }
        if (currentPiece != null) {
            soundManager.playPickup()
            triggerHaptic(HapticEffect.Pickup)
        }
        _gameState.update {
            it.copy(
                selectedShapeIndex = index,
                isBombActive = if (index != null) false else it.isBombActive,
                isHammerActive = if (index != null) false else it.isHammerActive,
                isRotateActive = if (index != null) false else it.isRotateActive,
                activeHint = null,
                hoverRow = null,
                hoverCol = null,
                hoverPreviewCells = emptySet(),
                prospectiveRows = emptySet(),
                prospectiveCols = emptySet()
            )
        }
    }

    fun canPlaceShape(shape: BlockShape, startRow: Int, startCol: Int): Boolean {
        return canPlaceShape(_gameState.value.grid, shape, startRow, startCol)
    }

    fun canPlaceShape(
        grid: List<List<Int>>,
        shape: BlockShape,
        startRow: Int,
        startCol: Int
    ): Boolean {
        for (r in 0 until shape.height) {
            for (c in 0 until shape.width) {
                if (shape.matrix[r][c]) {
                    val targetR = startRow + r
                    val targetC = startCol + c
                    if (targetR !in 0 until 8 || targetC !in 0 until 8) {
                        return false
                    }
                    if (grid[targetR][targetC] != 0) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun canPlaceShape(
        grid: Array<IntArray>,
        shape: BlockShape,
        startRow: Int,
        startCol: Int
    ): Boolean {
        for (r in 0 until shape.height) {
            for (c in 0 until shape.width) {
                if (shape.matrix[r][c]) {
                    val targetR = startRow + r
                    val targetC = startCol + c
                    if (targetR !in 0 until 8 || targetC !in 0 until 8) {
                        return false
                    }
                    if (grid[targetR][targetC] != 0) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun canFitAnywhere(grid: List<List<Int>>, shape: BlockShape): Boolean {
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                if (canPlaceShape(grid, shape, r, c)) {
                    return true
                }
            }
        }
        return false
    }

    fun checkGameOver(): Boolean {
        return checkGameOver(_gameState.value.grid, _gameState.value.activePieces)
    }

    fun checkGameOver(grid: List<List<Int>>, remainingPieces: List<BlockShape?>): Boolean {
        val nonNullPieces = remainingPieces.filterNotNull()
        if (nonNullPieces.isEmpty()) return false

        for (piece in nonNullPieces) {
            if (canFitAnywhere(grid, piece)) {
                return false
            }
        }
        return true
    }

    fun checkGameOver(grid: Array<IntArray>, remainingPieces: List<BlockShape?>): Boolean {
        val nonNullPieces = remainingPieces.filterNotNull()
        if (nonNullPieces.isEmpty()) return false

        for (piece in nonNullPieces) {
            var canFit = false
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    if (canPlaceShape(grid, piece, r, c)) {
                        canFit = true
                        break
                    }
                }
                if (canFit) break
            }
            if (canFit) return false
        }
        return true
    }

    fun updateHoverPreview(shapeIndex: Int, targetRow: Int?, targetCol: Int?) {
        val state = _gameState.value
        val shape = state.activePieces.getOrNull(shapeIndex) ?: return

        if (targetRow == null || targetCol == null) {
            if (state.hoverRow == null && state.hoverCol == null && state.hoverPreviewCells.isEmpty()) {
                return
            }
            _gameState.update {
                it.copy(
                    hoverRow = null,
                    hoverCol = null,
                    hoverPreviewCells = emptySet(),
                    prospectiveRows = emptySet(),
                    prospectiveCols = emptySet()
                )
            }
            return
        }

        // Fast-path: pointer moved within the same board cell
        if (state.hoverRow == targetRow && state.hoverCol == targetCol && state.activeHint == null) {
            return
        }

        if (canPlaceShape(state.grid, shape, targetRow, targetCol)) {
            val previewCells = HashSet<Pair<Int, Int>>(shape.cellCount)
            var previewMask = 0L
            for (r in 0 until shape.height) {
                for (c in 0 until shape.width) {
                    if (shape.matrix[r][c]) {
                        val rr = targetRow + r
                        val cc = targetCol + c
                        previewCells.add(Pair(rr, cc))
                        previewMask = previewMask or (1L shl (rr * 8 + cc))
                    }
                }
            }

            val prospectiveRows = HashSet<Int>(4)
            val prospectiveCols = HashSet<Int>(4)

            for (r in 0 until 8) {
                val rowGrid = state.grid[r]
                var full = true
                for (c in 0 until 8) {
                    if (rowGrid[c] == 0 && (previewMask and (1L shl (r * 8 + c))) == 0L) {
                        full = false
                        break
                    }
                }
                if (full) prospectiveRows.add(r)
            }

            for (c in 0 until 8) {
                var full = true
                for (r in 0 until 8) {
                    if (state.grid[r][c] == 0 && (previewMask and (1L shl (r * 8 + c))) == 0L) {
                        full = false
                        break
                    }
                }
                if (full) prospectiveCols.add(c)
            }

            _gameState.update {
                it.copy(
                    hoverRow = targetRow,
                    hoverCol = targetCol,
                    activeHint = null,
                    hoverPreviewCells = previewCells,
                    prospectiveRows = prospectiveRows,
                    prospectiveCols = prospectiveCols
                )
            }
        } else {
            if (state.hoverRow == targetRow &&
                state.hoverCol == targetCol &&
                state.hoverPreviewCells.isEmpty() &&
                state.activeHint == null
            ) {
                return
            }
            _gameState.update {
                it.copy(
                    hoverRow = targetRow,
                    hoverCol = targetCol,
                    activeHint = null,
                    hoverPreviewCells = emptySet(),
                    prospectiveRows = emptySet(),
                    prospectiveCols = emptySet()
                )
            }
        }
    }

    fun placeShape(shapeIndex: Int, targetRow: Int, targetCol: Int): Boolean {
        val currentState = _gameState.value
        val shape = currentState.activePieces.getOrNull(shapeIndex) ?: return false

        if (!canPlaceShape(currentState.grid, shape, targetRow, targetCol)) {
            return false
        }
        hasUserInteractedThisSession = true

        lastUndoSnapshot = UndoSnapshot(
            grid = currentState.grid,
            activePieces = currentState.activePieces,
            score = currentState.score,
            highScore = currentState.highScore,
            comboStreak = currentState.comboStreak,
            maxComboSession = currentState.maxComboSession,
            linesClearedSession = currentState.linesClearedSession,
            isGameOver = currentState.isGameOver,
            isNewHighScore = currentState.isNewHighScore,
            dailyBatchIndex = currentState.dailyBatchIndex,
            hammerCount = currentState.hammerCount,
            shuffleCount = currentState.shuffleCount,
            bombCount = currentState.bombCount,
            rotateCount = currentState.rotateCount,
            gemCells = currentState.gemCells,
            iceCells = currentState.iceCells,
            adventureGemsCollected = currentState.adventureGemsCollected,
            adventureIceBroken = currentState.adventureIceBroken,
            adventureLevelCompleted = currentState.adventureLevelCompleted,
            timeRemainingMs = currentState.timeRemainingMs,
            gameOverReason = currentState.gameOverReason
        )

        val newGrid = currentState.grid.map { it.toMutableList() }.toMutableList()
        for (r in 0 until shape.height) {
            for (c in 0 until shape.width) {
                if (shape.matrix[r][c]) {
                    newGrid[targetRow + r][targetCol + c] = shape.colorId
                }
            }
        }

        val basePoints = shape.cellCount * 10
        soundManager.playPlace()

        val completedRows = mutableListOf<Int>()
        val completedCols = mutableListOf<Int>()

        for (r in 0 until 8) {
            if ((0 until 8).all { c -> newGrid[r][c] != 0 }) {
                completedRows.add(r)
            }
        }
        for (c in 0 until 8) {
            if ((0 until 8).all { r -> newGrid[r][c] != 0 }) {
                completedCols.add(c)
            }
        }

        val totalLines = completedRows.size + completedCols.size
        var linePoints = 0
        var newCombo = 0
        val clearingCoords = mutableSetOf<Pair<Int, Int>>()
        val lineBursts = mutableListOf<LineClearBurst>()

        val mutableIce = currentState.iceCells.toMutableMap()
        val mutableGems = currentState.gemCells.toMutableSet()
        var gemsCollectedThisMove = 0
        var iceBrokenThisMove = 0
        var perfectBoardClear = false

        if (totalLines > 0) {
            newCombo = currentState.comboStreak + 1
            linePoints = totalLines * 100 * newCombo

            val bonus = when (totalLines) {
                2 -> 100
                3 -> 300
                4 -> 600
                else -> if (totalLines > 4) 1000 else 0
            }
            linePoints += bonus

            for (r in completedRows) {
                for (c in 0 until 8) {
                    clearingCoords.add(Pair(r, c))
                }
            }
            for (c in completedCols) {
                for (r in 0 until 8) {
                    clearingCoords.add(Pair(r, c))
                }
            }

            for (coord in clearingCoords) {
                val r = coord.first
                val c = coord.second
                val tileColor = newGrid[r][c]
                lineBursts.add(
                    LineClearBurst(
                        row = r,
                        col = c,
                        colorId = if (tileColor > 0) tileColor else shape.colorId,
                        comboMultiplier = newCombo,
                        isRowClear = completedRows.contains(r),
                        isColClear = completedCols.contains(c)
                    )
                )
            }

            for (coord in clearingCoords) {
                val r = coord.first
                val c = coord.second
                val iceHits = mutableIce[coord] ?: 0
                if (iceHits > 1) {
                    // Double-hit frost cracks to 1 layer; block remains until next clear
                    mutableIce[coord] = iceHits - 1
                } else {
                    if (iceHits == 1) {
                        mutableIce.remove(coord)
                        iceBrokenThisMove++
                    }
                    if (mutableGems.remove(coord)) {
                        gemsCollectedThisMove++
                    }
                    newGrid[r][c] = 0
                }
            }

            perfectBoardClear = newGrid.all { row -> row.all { it == 0 } }
            if (perfectBoardClear) {
                linePoints += PERFECT_BOARD_CLEAR_POINTS
                soundManager.playComboCheer()
                triggerHaptic(HapticEffect.PowerUp)
            } else if (newCombo >= 2) {
                soundManager.playComboCheer()
                triggerHaptic(HapticEffect.LineClear(newCombo))
            } else {
                soundManager.playLineClear(newCombo)
                triggerHaptic(HapticEffect.LineClear(newCombo))
            }
        } else {
            triggerHaptic(HapticEffect.Placement)
        }

        val gemCoinBonus = gemsCollectedThisMove * 5
        val boardClearCoinBonus = if (perfectBoardClear) PERFECT_BOARD_CLEAR_COINS else 0
        val earnedCoinBonus = gemCoinBonus + boardClearCoinBonus
        if (earnedCoinBonus > 0) {
            preferences.coinsBalance = preferences.coinsBalance + earnedCoinBonus
            preferences.totalCoinsEarned = preferences.totalCoinsEarned + earnedCoinBonus
        }

        val totalMovePoints = basePoints + linePoints
        val newScore = currentState.score + totalMovePoints
        val newHigh = maxOf(newScore, currentState.highScore)
        val isDaily = currentState.gameMode == GameMode.DAILY_CHALLENGE
        val isTimeAttack = currentState.gameMode == GameMode.TIME_ATTACK
        val isAdventure = currentState.gameMode == GameMode.ADVENTURE
        val isNewRecord = when {
            isDaily -> newScore > preferences.getDailyHighScore(currentState.dailyDate)
            isTimeAttack -> newScore > preferences.timeAttackHighScore
            isAdventure -> newScore > preferences.getAdventureHighScore(currentState.adventureLevelNumber)
            else -> newScore > preferences.highScore
        }

        if (isNewRecord) {
            when {
                isDaily -> preferences.setDailyHighScore(currentState.dailyDate, newScore)
                isTimeAttack -> preferences.timeAttackHighScore = newScore
                isAdventure -> preferences.setAdventureHighScore(currentState.adventureLevelNumber, newScore)
                else -> preferences.highScore = newScore
            }
        }
        if (newCombo > preferences.highestCombo) {
            preferences.highestCombo = newCombo
        }
        preferences.totalLinesCleared = preferences.totalLinesCleared + totalLines

        val updatedPieces = currentState.activePieces.toMutableList()
        updatedPieces[shapeIndex] = null

        var nextBatchIndex = currentState.dailyBatchIndex
        val finalPieces = if (updatedPieces.all { it == null }) {
            if (isDaily) {
                nextBatchIndex++
                ShapeDefinitions.getDailyShapeBatch(computeDailySeed(), nextBatchIndex, currentScore = newScore)
            } else {
                ShapeDefinitions.getRandomShapeBatch(currentScore = newScore)
            }
        } else {
            updatedPieces
        }

        val bonusMs = if (isTimeAttack && totalLines > 0) {
            (totalLines * TIME_ATTACK_BONUS_PER_LINE_MS) +
                (if (newCombo >= 2) TIME_ATTACK_COMBO_BONUS_MS else 0L)
        } else {
            0L
        }
        val updatedTimeMs = if (isTimeAttack) {
            (currentState.timeRemainingMs + bonusMs).coerceAtMost(TIME_ATTACK_MAX_MS)
        } else {
            currentState.timeRemainingMs
        }
        val bonusSeconds = (bonusMs / 1000L).toInt()

        val oldTier = DifficultyTier.fromScore(currentState.score)
        val newTier = DifficultyTier.fromScore(newScore)
        val tierProgress = DifficultyTier.progressToNextTier(newScore)
        val leveledUp = newTier.level > oldTier.level
        if (leveledUp && totalLines == 0) {
            soundManager.playPowerup()
            triggerHaptic(HapticEffect.PowerUp)
        }

        val baseBannerText = when {
            totalLines >= 3 -> "MEGA BLAST! +$totalMovePoints"
            totalLines == 2 -> "DOUBLE LINE! +$totalMovePoints"
            newCombo >= 2 -> "COMBO x$newCombo! +$totalMovePoints"
            totalLines == 1 -> "LINE CLEAR! +$totalMovePoints"
            else -> null
        }
        val withTimeBonusBanner = if (bonusSeconds > 0 && baseBannerText != null) {
            "+${bonusSeconds}s TIME! $baseBannerText"
        } else {
            baseBannerText
        }
        val newlyUnlockedShapes = if (leveledUp) {
            ShapeDefinitions.getNewShapesUnlockedCount(currentState.score, newScore)
        } else {
            0
        }
        val unlockSuffix = if (newlyUnlockedShapes > 0) " (+$newlyUnlockedShapes SHAPES)" else ""
        val bannerText = when {
            leveledUp && withTimeBonusBanner != null -> "LV ${newTier.level} ${newTier.title}!$unlockSuffix $withTimeBonusBanner"
            leveledUp -> "DIFFICULTY UP! LV ${newTier.level} ${newTier.title}$unlockSuffix"
            else -> withTimeBonusBanner
        }

        val popups = mutableListOf<ScorePopup>()
        if (totalLines > 0) {
            val ptsPerLine = linePoints / totalLines
            if (completedRows.isNotEmpty() && completedCols.isNotEmpty()) {
                for (r in completedRows) {
                    for (c in completedCols) {
                        val text = if (newCombo > 1) "+$totalMovePoints\nCOMBO x$newCombo!" else "+$totalMovePoints\nBLAST!"
                        popups.add(
                            ScorePopup(
                                text = text,
                                row = r.toFloat(),
                                col = c.toFloat(),
                                isCombo = true,
                                comboMultiplier = newCombo
                            )
                        )
                    }
                }
            } else {
                for (r in completedRows) {
                    val text = if (newCombo > 1) "+$ptsPerLine x$newCombo" else "+$ptsPerLine"
                    popups.add(
                        ScorePopup(
                            text = text,
                            row = r.toFloat(),
                            col = 3.5f,
                            isCombo = newCombo > 1,
                            comboMultiplier = newCombo
                        )
                    )
                }
                for (c in completedCols) {
                    val text = if (newCombo > 1) "+$ptsPerLine x$newCombo" else "+$ptsPerLine"
                    popups.add(
                        ScorePopup(
                            text = text,
                            row = 3.5f,
                            col = c.toFloat(),
                            isCombo = newCombo > 1,
                            comboMultiplier = newCombo
                        )
                    )
                }
            }
        } else {
            popups.add(
                ScorePopup(
                    text = "+$basePoints",
                    row = targetRow.toFloat().coerceIn(0f, 7f),
                    col = targetCol.toFloat().coerceIn(0f, 7f),
                    isCombo = false
                )
            )
        }

        val totalGemsCollected = currentState.adventureGemsCollected + gemsCollectedThisMove
        val totalIceBroken = currentState.adventureIceBroken + iceBrokenThisMove
        val adventureSpec = if (isAdventure) AdventureCatalog.level(currentState.adventureLevelNumber) else null

        val adventureCompletedNow = if (adventureSpec != null && !currentState.adventureLevelCompleted) {
            when (adventureSpec.goalType) {
                AdventureGoalType.COLLECT_GEMS -> totalGemsCollected >= adventureSpec.targetCount
                AdventureGoalType.BREAK_ICE -> mutableIce.isEmpty()
                AdventureGoalType.SCORE_TARGET -> newScore >= adventureSpec.targetCount
            }
        } else {
            currentState.adventureLevelCompleted
        }

        // Prevent soft-locks on Gem levels by placing replacement Gems onto existing blocks when needed
        if (adventureSpec != null &&
            adventureSpec.goalType == AdventureGoalType.COLLECT_GEMS &&
            !adventureCompletedNow
        ) {
            val neededTotalOnBoard = (adventureSpec.targetCount - totalGemsCollected).coerceAtLeast(0)
            if (mutableGems.size < neededTotalOnBoard) {
                val eligibleCells = mutableListOf<Pair<Int, Int>>()
                for (r in 0 until 8) {
                    for (c in 0 until 8) {
                        val coord = r to c
                        if (newGrid[r][c] > 0 && coord !in mutableGems && coord !in mutableIce) {
                            eligibleCells.add(coord)
                        }
                    }
                }
                eligibleCells.shuffle()
                val toSpawn = minOf(neededTotalOnBoard - mutableGems.size, eligibleCells.size)
                for (i in 0 until toSpawn) {
                    mutableGems.add(eligibleCells[i])
                }
            }
        }

        val adventureStars = if (adventureSpec != null && adventureCompletedNow && !currentState.adventureLevelCompleted) {
            AdventureCatalog.computeStars(adventureSpec, newScore)
        } else {
            currentState.adventureEarnedStars
        }

        var adventureRewardCoins = currentState.adventureLastRewardCoins
        if (adventureSpec != null && adventureCompletedNow && !currentState.adventureLevelCompleted) {
            preferences.setAdventureLevelStars(adventureSpec.levelNumber, adventureStars)
            val nextUnlocked = (adventureSpec.levelNumber + 1).coerceAtMost(AdventureCatalog.totalLevels)
            if (nextUnlocked > preferences.adventureMaxUnlockedLevel) {
                preferences.adventureMaxUnlockedLevel = nextUnlocked
            }
            val firstClear = !preferences.isAdventureLevelRewardClaimed(adventureSpec.levelNumber)
            adventureRewardCoins = if (firstClear) adventureSpec.coinReward else 20
            preferences.markAdventureLevelRewardClaimed(adventureSpec.levelNumber)
            preferences.coinsBalance = preferences.coinsBalance + adventureRewardCoins
            preferences.totalCoinsEarned = preferences.totalCoinsEarned + adventureRewardCoins
            if (firstClear) {
                if (adventureSpec.hammerReward > 0) {
                    preferences.hammerCount = preferences.hammerCount + adventureSpec.hammerReward
                }
                if (adventureSpec.rotateReward > 0) {
                    preferences.rotateCount = preferences.rotateCount + adventureSpec.rotateReward
                }
            }
            soundManager.playComboCheer()
            triggerHaptic(HapticEffect.PowerUp)
        }

        val boardClearBanner = if (perfectBoardClear) {
            "✨ PERFECT BOARD CLEAR! +$PERFECT_BOARD_CLEAR_POINTS PTS • +$PERFECT_BOARD_CLEAR_COINS 🪙"
        } else if (gemsCollectedThisMove > 0) {
            "💎 +$gemsCollectedThisMove GEM${if (gemsCollectedThisMove > 1) "S" else ""} • +${gemCoinBonus} 🪙"
        } else {
            null
        }
        val finalBannerText = boardClearBanner ?: bannerText
        if (perfectBoardClear) {
            popups.add(
                0,
                ScorePopup(
                    text = "BOARD CLEAR!\n+$PERFECT_BOARD_CLEAR_POINTS",
                    row = 3.5f,
                    col = 3.5f,
                    isCombo = true,
                    comboMultiplier = maxOf(newCombo, 3)
                )
            )
        }

        val frozenGrid = newGrid.map { it.toList() }
        val isGameOver = if (adventureCompletedNow) false else checkGameOver(frozenGrid, finalPieces)
        val updatedMaxCombo = maxOf(currentState.maxComboSession, newCombo)
        val updatedLines = currentState.linesClearedSession + totalLines
        val updatedAllTimeTotal = if (totalMovePoints > 0) {
            preferences.addToTotalAllTimeScore(totalMovePoints)
        } else {
            preferences.totalAllTimeScore
        }

        val overallBestAfterMove = maxOf(
            currentState.playerOverallBestScore,
            newHigh,
            computeLocalOverallBestScore(currentState.dailyDate.ifEmpty { getTodayDateString() })
        )
        val shouldShake = perfectBoardClear || totalLines >= 3
        _gameState.update {
            it.copy(
                grid = frozenGrid,
                activePieces = finalPieces,
                score = newScore,
                highScore = newHigh,
                totalAllTimeScore = updatedAllTimeTotal,
                playerOverallBestScore = overallBestAfterMove,
                coinsBalance = preferences.coinsBalance,
                hammerCount = preferences.hammerCount,
                rotateCount = preferences.rotateCount,
                comboStreak = if (totalLines > 0) newCombo else 0,
                comboEventId = if (totalLines > 0) currentState.comboEventId + 1L else currentState.comboEventId,
                lastClearedLinesCount = totalLines,
                maxComboSession = updatedMaxCombo,
                linesClearedSession = updatedLines,
                isGameOver = isGameOver,
                gameOverReason = if (isGameOver) GameOverReason.NO_MOVES else it.gameOverReason,
                isNewHighScore = isNewRecord,
                selectedShapeIndex = null,
                isHammerActive = false,
                isBombActive = false,
                isRotateActive = false,
                activeHint = null,
                hoverRow = null,
                hoverCol = null,
                hoverPreviewCells = emptySet(),
                prospectiveRows = emptySet(),
                prospectiveCols = emptySet(),
                clearingCells = clearingCoords,
                lineClearBursts = lineBursts,
                comboBannerText = finalBannerText,
                scorePopups = popups,
                canUndo = true,
                dailyBatchIndex = nextBatchIndex,
                difficultyLevel = newTier.level,
                difficultyTitle = newTier.title,
                difficultyProgress = tierProgress,
                nextDifficultyScore = newTier.maxScoreExclusive,
                unlockedShapeVariety = ShapeDefinitions.getUnlockedVarietyCount(newScore),
                totalShapeVariety = ShapeDefinitions.totalShapeVarietyCount,
                timeRemainingMs = updatedTimeMs,
                lastTimeBonusSeconds = bonusSeconds,
                gemCells = mutableGems.toSet(),
                iceCells = mutableIce.toMap(),
                adventureMaxUnlockedLevel = preferences.adventureMaxUnlockedLevel,
                adventureGemsCollected = totalGemsCollected,
                adventureIceBroken = totalIceBroken,
                adventureLevelCompleted = adventureCompletedNow,
                adventureEarnedStars = adventureStars,
                adventureLastRewardCoins = adventureRewardCoins,
                boardClearEventId = if (perfectBoardClear) it.boardClearEventId + 1L else it.boardClearEventId,
                shakeEventId = if (shouldShake) it.shakeEventId + 1L else it.shakeEventId
            )
        }

        val dailyDateStr = currentState.dailyDate.ifEmpty { getTodayDateString() }
        var newlySolvedDaily = false
        if (isDaily) {
            preferences.recordDailyParticipation(dailyDateStr, getYesterdayDateString())
            if (newScore >= currentState.dailyTargetScore) {
                newlySolvedDaily = preferences.markDailyDateCompleted(dailyDateStr)
            }
        }

        if (isGameOver) {
            stopTimeAttackTimer()
            if (isDaily) {
                preferences.recordDailyGame(dailyDateStr, newScore)
            }
            soundManager.playGameOver()
            triggerHaptic(HapticEffect.GameOver)
        } else if (newlySolvedDaily) {
            soundManager.playPowerup()
            triggerHaptic(HapticEffect.PowerUp)
            _gameState.update {
                it.copy(
                    comboBannerText = "🎉 DAILY PUZZLE SOLVED! STREAK: ${preferences.dailyStreak}d",
                    dailyStreak = preferences.dailyStreak,
                    isDailyCompletedToday = true
                )
            }
        }

        // Submit Daily Challenge score to global Firestore leaderboard on new record, solve, or completion
        if (isDaily && (isNewRecord || isGameOver || newlySolvedDaily) && newScore > 0 && firestoreRepository != null && currentUserId.isNotBlank()) {
            viewModelScope.launch {
                firestoreRepository.submitDailyScore(
                    dateStr = dailyDateStr,
                    displayName = currentUserDisplayName,
                    score = newScore,
                    linesCleared = updatedLines,
                    maxCombo = updatedMaxCombo
                )
            }
        }

        // Increment All-Time Global Leaderboard score & sync single-game High Score in real time
        if (totalMovePoints > 0 && firestoreRepository != null && currentUserId.isNotBlank()) {
            val bestSingleGame = maxOf(newHigh, computeLocalOverallBestScore(dailyDateStr))
            val bestAllTimeCombo = maxOf(updatedMaxCombo, preferences.highestCombo)
            viewModelScope.launch {
                firestoreRepository.submitAllTimeScoreDelta(
                    displayName = currentUserDisplayName,
                    scoreDelta = totalMovePoints,
                    bestSingleGameScore = bestSingleGame,
                    linesDelta = totalLines,
                    highestCombo = bestAllTimeCombo
                )
            }
        }

        // Record progress towards Daily Missions
        recordMissionEvent(MissionType.PLACE_BLOCKS, 1)
        if (totalLines > 0) {
            recordMissionEvent(MissionType.CLEAR_LINES, totalLines)
            recordMissionEvent(MissionType.CLEAR_LINES_SINGLE_MOVE, totalLines)
            recordMissionEvent(MissionType.REACH_COMBO, newCombo)
        }
        recordMissionEvent(MissionType.SCORE_POINTS, newScore)

        // Evaluate & persist Achievements (10-Line Combo, High Scorer, Daily Streak, etc.)
        evaluateAndSyncAchievements(
            currentScore = newScore,
            currentMaxCombo = updatedMaxCombo,
            notifyUnlocks = true
        )

        // Persist updated session locally and to Firestore
        flushSession(syncToCloud = true)

        viewModelScope.launch {
            delay(400)
            _gameState.update { it.copy(clearingCells = emptySet()) }
            delay(400)
            _gameState.update { it.copy(lineClearBursts = emptyList()) }
            delay(600)
            _gameState.update {
                it.copy(
                    comboBannerText = null,
                    scorePopups = emptyList(),
                    lastTimeBonusSeconds = 0
                )
            }
        }

        return true
    }

    fun useSecondChance() {
        val state = _gameState.value
        if (!state.secondChanceAvailable || !state.isGameOver) return
        hasUserInteractedThisSession = true

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        val isTimeAttack = state.gameMode == GameMode.TIME_ATTACK
        val isTimeUp = state.gameOverReason == GameOverReason.TIME_UP
        val boardWasBlocked = checkGameOver(state.grid, state.activePieces)

        val finalGrid = if (!isTimeUp || boardWasBlocked) {
            val newGrid = state.grid.map { it.toMutableList() }.toMutableList()
            for (r in 2..5) {
                for (c in 2..5) {
                    newGrid[r][c] = 0
                }
            }
            val updatedGrid = newGrid.map { it.toList() }
            if (checkGameOver(updatedGrid, state.activePieces)) {
                val rescued = updatedGrid.map { it.toMutableList() }.toMutableList()
                for (c in 0 until 8) {
                    rescued[6][c] = 0
                    rescued[7][c] = 0
                }
                rescued.map { it.toList() }
            } else {
                updatedGrid
            }
        } else {
            state.grid
        }

        val revivedTimeMs = if (isTimeAttack) {
            if (isTimeUp) {
                TIME_ATTACK_SECOND_CHANCE_MS
            } else {
                maxOf(state.timeRemainingMs, TIME_ATTACK_SECOND_CHANCE_MS)
            }
        } else {
            state.timeRemainingMs
        }

        val reviveBanner = if (isTimeAttack) {
            "SECOND CHANCE! +15s TIME!"
        } else {
            "SECOND CHANCE! BLAST REVIVE!"
        }

        _gameState.update {
            it.copy(
                grid = finalGrid,
                isGameOver = false,
                gameOverReason = GameOverReason.NO_MOVES,
                secondChanceAvailable = false,
                timeRemainingMs = revivedTimeMs,
                lastTimeBonusSeconds = if (isTimeAttack) (TIME_ATTACK_SECOND_CHANCE_MS / 1000L).toInt() else 0,
                comboBannerText = reviveBanner
            )
        }

        ensureTimeAttackTimer()
        flushSession(syncToCloud = true)

        viewModelScope.launch {
            delay(1800)
            _gameState.update { it.copy(comboBannerText = null, lastTimeBonusSeconds = 0) }
        }
    }

    fun toggleHammerMode() {
        val current = _gameState.value
        if (current.hammerCount <= 0 && !current.isHammerActive) {
            _gameState.update { it.copy(comboBannerText = "NO HAMMERS LEFT! COMPLETE DAILY MISSIONS") }
            viewModelScope.launch {
                delay(1500)
                _gameState.update { it.copy(comboBannerText = null) }
            }
            return
        }

        val willBeActive = !current.isHammerActive
        _gameState.update {
            it.copy(
                isHammerActive = willBeActive,
                isBombActive = if (willBeActive) false else it.isBombActive,
                selectedShapeIndex = if (willBeActive) null else it.selectedShapeIndex,
                activeHint = if (willBeActive) null else it.activeHint,
                hoverPreviewCells = if (willBeActive) emptySet() else it.hoverPreviewCells,
                prospectiveRows = if (willBeActive) emptySet() else it.prospectiveRows,
                prospectiveCols = if (willBeActive) emptySet() else it.prospectiveCols,
                comboBannerText = if (willBeActive) "HAMMER ACTIVE: TAP ANY FILLED BLOCK!" else null
            )
        }

        if (willBeActive) {
            soundManager.playPickup()
            triggerHaptic(HapticEffect.Pickup)
        }
    }

    fun useHint(): Boolean {
        val current = _gameState.value
        if (current.isGameOver) return false

        // If a hint is already highlighted for the current turn, re-display the banner without spending another hint
        val existingHint = current.activeHint
        if (existingHint != null) {
            val hintMsg = if (existingHint.clearedLines > 0) {
                "💡 HINT: CLEARS ${existingHint.clearedLines} LINE${if (existingHint.clearedLines > 1) "S" else ""}!"
            } else {
                "💡 HINT: TAP HIGHLIGHTED CELLS TO PLACE!"
            }
            _gameState.update {
                it.copy(
                    selectedShapeIndex = existingHint.pieceIndex,
                    comboBannerText = hintMsg
                )
            }
            viewModelScope.launch {
                delay(1500)
                _gameState.update {
                    if (it.comboBannerText == hintMsg) it.copy(comboBannerText = null) else it
                }
            }
            return true
        }

        if (current.hintCount <= 0) {
            val noHintBanner = "NO HINTS LEFT! COMPLETE DAILY MISSIONS"
            _gameState.update { it.copy(comboBannerText = noHintBanner) }
            viewModelScope.launch {
                delay(1500)
                _gameState.update {
                    if (it.comboBannerText == noHintBanner) it.copy(comboBannerText = null) else it
                }
            }
            return false
        }

        val bestHint = findBestPlacementHint(current.grid, current.activePieces) ?: return false
        val newHintCount = (current.hintCount - 1).coerceAtLeast(0)
        preferences.hintCount = newHintCount

        val prospectiveRows = mutableSetOf<Int>()
        val prospectiveCols = mutableSetOf<Int>()
        for (r in 0 until 8) {
            if ((0 until 8).all { c -> current.grid[r][c] != 0 || Pair(r, c) in bestHint.cells }) {
                prospectiveRows.add(r)
            }
        }
        for (c in 0 until 8) {
            if ((0 until 8).all { r -> current.grid[r][c] != 0 || Pair(r, c) in bestHint.cells }) {
                prospectiveCols.add(c)
            }
        }

        val hintBanner = if (bestHint.clearedLines > 0) {
            "💡 HINT: CLEARS ${bestHint.clearedLines} LINE${if (bestHint.clearedLines > 1) "S" else ""}!"
        } else {
            "💡 HINT: BEST MOVE HIGHLIGHTED!"
        }

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        _gameState.update {
            it.copy(
                hintCount = newHintCount,
                activeHint = bestHint,
                isHammerActive = false,
                isBombActive = false,
                selectedShapeIndex = bestHint.pieceIndex,
                hoverRow = bestHint.row,
                hoverCol = bestHint.col,
                hoverPreviewCells = bestHint.cells,
                prospectiveRows = prospectiveRows,
                prospectiveCols = prospectiveCols,
                comboBannerText = hintBanner
            )
        }

        viewModelScope.launch {
            delay(1800)
            _gameState.update {
                if (it.comboBannerText == hintBanner) it.copy(comboBannerText = null) else it
            }
        }
        return true
    }

    private fun findBestPlacementHint(
        grid: List<List<Int>>,
        activePieces: List<BlockShape?>
    ): PlacementHint? {
        var bestPieceIdx = -1
        var bestRow = 0
        var bestCol = 0
        var bestShape: BlockShape? = null
        var bestCleared = 0
        var bestScore = Int.MIN_VALUE

        for ((pieceIdx, shape) in activePieces.withIndex()) {
            if (shape == null) continue
            for (startR in 0..(8 - shape.height)) {
                for (startC in 0..(8 - shape.width)) {
                    if (!canPlaceShape(grid, shape, startR, startC)) continue

                    var placedMask = 0L
                    var neighborScore = 0
                    for (dr in 0 until shape.height) {
                        for (dc in 0 until shape.width) {
                            if (shape.matrix[dr][dc]) {
                                val rr = startR + dr
                                val cc = startC + dc
                                placedMask = placedMask or (1L shl (rr * 8 + cc))
                                if (rr == 0 || grid[rr - 1][cc] != 0) neighborScore += 25
                                if (rr == 7 || grid[rr + 1][cc] != 0) neighborScore += 25
                                if (cc == 0 || grid[rr][cc - 1] != 0) neighborScore += 25
                                if (cc == 7 || grid[rr][cc + 1] != 0) neighborScore += 25
                            }
                        }
                    }

                    var fullRows = 0
                    for (r in 0 until 8) {
                        val rowGrid = grid[r]
                        var full = true
                        for (c in 0 until 8) {
                            if (rowGrid[c] == 0 && (placedMask and (1L shl (r * 8 + c))) == 0L) {
                                full = false
                                break
                            }
                        }
                        if (full) fullRows++
                    }

                    var fullCols = 0
                    for (c in 0 until 8) {
                        var full = true
                        for (r in 0 until 8) {
                            if (grid[r][c] == 0 && (placedMask and (1L shl (r * 8 + c))) == 0L) {
                                full = false
                                break
                            }
                        }
                        if (full) fullCols++
                    }

                    val totalCleared = fullRows + fullCols
                    val candidateScore = (totalCleared * 10_000) + neighborScore + (shape.cellCount * 10)
                    if (candidateScore > bestScore) {
                        bestScore = candidateScore
                        bestPieceIdx = pieceIdx
                        bestRow = startR
                        bestCol = startC
                        bestShape = shape
                        bestCleared = totalCleared
                    }
                }
            }
        }

        val winningShape = bestShape ?: return null
        val placedCells = HashSet<Pair<Int, Int>>(winningShape.cellCount)
        for (dr in 0 until winningShape.height) {
            for (dc in 0 until winningShape.width) {
                if (winningShape.matrix[dr][dc]) {
                    placedCells.add(Pair(bestRow + dr, bestCol + dc))
                }
            }
        }
        return PlacementHint(
            pieceIndex = bestPieceIdx,
            row = bestRow,
            col = bestCol,
            cells = placedCells,
            clearedLines = bestCleared
        )
    }

    fun activateHammerFromGameOver() {
        val current = _gameState.value
        if (current.hammerCount <= 0 || current.gameOverReason == GameOverReason.TIME_UP) return

        _gameState.update {
            it.copy(
                isGameOver = false,
                isHammerActive = true,
                selectedShapeIndex = null,
                comboBannerText = "HAMMER RESCUE: TAP A BLOCK TO SMASH & CONTINUE!"
            )
        }
        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)
    }

    fun useHammerOnCell(row: Int, col: Int): Boolean {
        val currentState = _gameState.value
        if (!currentState.isHammerActive || currentState.hammerCount <= 0) return false
        val cellColor = currentState.grid.getOrNull(row)?.getOrNull(col) ?: 0
        if (cellColor == 0) return false
        hasUserInteractedThisSession = true

        lastUndoSnapshot = UndoSnapshot(
            grid = currentState.grid,
            activePieces = currentState.activePieces,
            score = currentState.score,
            highScore = currentState.highScore,
            comboStreak = currentState.comboStreak,
            maxComboSession = currentState.maxComboSession,
            linesClearedSession = currentState.linesClearedSession,
            isGameOver = currentState.isGameOver,
            isNewHighScore = currentState.isNewHighScore,
            dailyBatchIndex = currentState.dailyBatchIndex,
            hammerCount = currentState.hammerCount,
            shuffleCount = currentState.shuffleCount,
            bombCount = currentState.bombCount,
            rotateCount = currentState.rotateCount,
            gemCells = currentState.gemCells,
            iceCells = currentState.iceCells,
            adventureGemsCollected = currentState.adventureGemsCollected,
            adventureIceBroken = currentState.adventureIceBroken,
            adventureLevelCompleted = currentState.adventureLevelCompleted,
            timeRemainingMs = currentState.timeRemainingMs,
            gameOverReason = currentState.gameOverReason
        )

        val coord = row to col
        val newGrid = currentState.grid.map { it.toMutableList() }.toMutableList()
        newGrid[row][col] = 0
        val frozenGrid = newGrid.map { it.toList() }

        val mutableIce = currentState.iceCells.toMutableMap()
        val hadIce = mutableIce.remove(coord) != null
        val mutableGems = currentState.gemCells.toMutableSet()
        val hadGem = mutableGems.remove(coord)
        if (hadGem) {
            preferences.coinsBalance = preferences.coinsBalance + 5
            preferences.totalCoinsEarned = preferences.totalCoinsEarned + 5
        }
        val nextGemsCollected = currentState.adventureGemsCollected + if (hadGem) 1 else 0
        val nextIceBroken = currentState.adventureIceBroken + if (hadIce) 1 else 0

        val newHammerCount = (currentState.hammerCount - 1).coerceAtLeast(0)
        preferences.hammerCount = newHammerCount

        soundManager.playHammerSmash()
        triggerHaptic(HapticEffect.HammerSmash)

        val burst = LineClearBurst(
            row = row,
            col = col,
            colorId = cellColor,
            comboMultiplier = 1,
            isRowClear = true,
            isColClear = true
        )

        val adventureDone = evaluateAdventureClearAfterBooster(
            state = currentState,
            nextIce = mutableIce,
            nextGemsCollected = nextGemsCollected
        )
        val stillGameOver = if (adventureDone) false else checkGameOver(frozenGrid, currentState.activePieces)

        _gameState.update {
            it.copy(
                grid = frozenGrid,
                gemCells = mutableGems.toSet(),
                iceCells = mutableIce.toMap(),
                adventureGemsCollected = nextGemsCollected,
                adventureIceBroken = nextIceBroken,
                adventureLevelCompleted = adventureDone || it.adventureLevelCompleted,
                adventureEarnedStars = if (adventureDone && !it.adventureLevelCompleted) {
                    AdventureCatalog.computeStars(AdventureCatalog.level(it.adventureLevelNumber), it.score)
                } else it.adventureEarnedStars,
                adventureMaxUnlockedLevel = preferences.adventureMaxUnlockedLevel,
                coinsBalance = preferences.coinsBalance,
                hammerCount = newHammerCount,
                isHammerActive = false,
                isBombActive = false,
                isRotateActive = false,
                activeHint = null,
                isGameOver = stillGameOver,
                lineClearBursts = listOf(burst),
                comboBannerText = "BLOCK SMASHED! 🔨",
                canUndo = true
            )
        }

        recordMissionEvent(MissionType.USE_HAMMER, 1)
        flushSession(syncToCloud = true)

        viewModelScope.launch {
            delay(400)
            _gameState.update { it.copy(lineClearBursts = emptyList()) }
            delay(800)
            _gameState.update { it.copy(comboBannerText = null) }
        }

        return true
    }

    /**
     * Rerolls all currently active (non-null) tray pieces with fresh shapes, guaranteeing
     * at least one piece can be placed on the board when empty cells remain.
     */
    fun useShuffle(): Boolean {
        val currentState = _gameState.value
        if (currentState.isGameOver) return false
        if (currentState.activePieces.none { it != null }) return false

        if (currentState.shuffleCount <= 0) {
            showTemporaryBanner("NO SHUFFLES LEFT! VISIT SHOP OR WATCH AD")
            return false
        }

        hasUserInteractedThisSession = true
        lastUndoSnapshot = UndoSnapshot(
            grid = currentState.grid,
            activePieces = currentState.activePieces,
            score = currentState.score,
            highScore = currentState.highScore,
            comboStreak = currentState.comboStreak,
            maxComboSession = currentState.maxComboSession,
            linesClearedSession = currentState.linesClearedSession,
            isGameOver = currentState.isGameOver,
            isNewHighScore = currentState.isNewHighScore,
            dailyBatchIndex = currentState.dailyBatchIndex,
            hammerCount = currentState.hammerCount,
            shuffleCount = currentState.shuffleCount,
            bombCount = currentState.bombCount,
            timeRemainingMs = currentState.timeRemainingMs,
            gameOverReason = currentState.gameOverReason
        )

        var nextBatchIndex = currentState.dailyBatchIndex
        var candidateTray: List<BlockShape?> = emptyList()
        repeat(6) { attempt ->
            val freshBatch = if (currentState.gameMode == GameMode.DAILY_CHALLENGE) {
                nextBatchIndex++
                ShapeDefinitions.getDailyShapeBatch(
                    dateSeed = computeDailySeed() + attempt * 71L,
                    batchIndex = nextBatchIndex,
                    currentScore = currentState.score
                )
            } else {
                ShapeDefinitions.getRandomShapeBatch(currentScore = currentState.score)
            }
            val mapped = currentState.activePieces.mapIndexed { idx, existing ->
                if (existing != null) freshBatch.getOrNull(idx) ?: freshBatch.first() else null
            }
            candidateTray = mapped
            if (!checkGameOver(currentState.grid, mapped)) {
                return@repeat
            }
        }

        // Guarantee at least one piece fits if the board isn't completely full
        if (checkGameOver(currentState.grid, candidateTray)) {
            val fittingSpec = ShapeDefinitions.getAvailableSpecsForScore(currentState.score)
                .map { it.toBlockShape() }
                .firstOrNull { canFitAnywhere(currentState.grid, it) }
                ?: ShapeDefinitions.getAvailableSpecsForScore(0)
                    .map { it.toBlockShape() }
                    .firstOrNull { canFitAnywhere(currentState.grid, it) }

            if (fittingSpec != null) {
                val firstSlot = candidateTray.indexOfFirst { it != null }
                if (firstSlot >= 0) {
                    val mutable = candidateTray.toMutableList()
                    mutable[firstSlot] = fittingSpec
                    candidateTray = mutable
                }
            }
        }

        val nextShuffleCount = (currentState.shuffleCount - 1).coerceAtLeast(0)
        preferences.shuffleCount = nextShuffleCount
        val stillGameOver = checkGameOver(currentState.grid, candidateTray)

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        val shuffleBanner = "🔄 TRAY SHUFFLED!"
        _gameState.update {
            it.copy(
                activePieces = candidateTray,
                shuffleCount = nextShuffleCount,
                dailyBatchIndex = nextBatchIndex,
                isHammerActive = false,
                isBombActive = false,
                selectedShapeIndex = null,
                activeHint = null,
                hoverRow = null,
                hoverCol = null,
                hoverPreviewCells = emptySet(),
                prospectiveRows = emptySet(),
                prospectiveCols = emptySet(),
                isGameOver = stillGameOver,
                comboBannerText = shuffleBanner,
                canUndo = true
            )
        }

        flushSession(syncToCloud = true)
        viewModelScope.launch {
            delay(1400)
            _gameState.update {
                if (it.comboBannerText == shuffleBanner) it.copy(comboBannerText = null) else it
            }
        }
        return true
    }

    /**
     * Toggles the 3x3 Area Bomb targeting mode on or off.
     */
    fun toggleBombMode() {
        val current = _gameState.value
        if (current.bombCount <= 0 && !current.isBombActive) {
            showTemporaryBanner("NO BOMBS LEFT! VISIT SHOP OR WATCH AD")
            return
        }

        val willBeActive = !current.isBombActive
        _gameState.update {
            it.copy(
                isBombActive = willBeActive,
                isHammerActive = if (willBeActive) false else it.isHammerActive,
                selectedShapeIndex = if (willBeActive) null else it.selectedShapeIndex,
                activeHint = if (willBeActive) null else it.activeHint,
                hoverPreviewCells = if (willBeActive) emptySet() else it.hoverPreviewCells,
                prospectiveRows = if (willBeActive) emptySet() else it.prospectiveRows,
                prospectiveCols = if (willBeActive) emptySet() else it.prospectiveCols,
                comboBannerText = if (willBeActive) "💣 TAP BOARD TO BLAST 3×3 AREA!" else null
            )
        }

        if (willBeActive) {
            soundManager.playPickup()
            triggerHaptic(HapticEffect.Pickup)
        }
    }

    /**
     * Detonates a 3x3 bomb centered at ([row], [col]), clearing up to 9 cells.
     */
    fun useBombOnCell(row: Int, col: Int): Boolean {
        val currentState = _gameState.value
        if (!currentState.isBombActive || currentState.bombCount <= 0) return false
        if (row !in 0 until 8 || col !in 0 until 8) return false

        val minR = (row - 1).coerceAtLeast(0)
        val maxR = (row + 1).coerceAtMost(7)
        val minC = (col - 1).coerceAtLeast(0)
        val maxC = (col + 1).coerceAtMost(7)

        val bursts = mutableListOf<LineClearBurst>()
        for (r in minR..maxR) {
            for (c in minC..maxC) {
                val cellColor = currentState.grid[r][c]
                if (cellColor > 0) {
                    bursts.add(
                        LineClearBurst(
                            row = r,
                            col = c,
                            colorId = cellColor,
                            comboMultiplier = 3,
                            isRowClear = true,
                            isColClear = true
                        )
                    )
                }
            }
        }
        if (bursts.isEmpty()) return false

        hasUserInteractedThisSession = true
        lastUndoSnapshot = UndoSnapshot(
            grid = currentState.grid,
            activePieces = currentState.activePieces,
            score = currentState.score,
            highScore = currentState.highScore,
            comboStreak = currentState.comboStreak,
            maxComboSession = currentState.maxComboSession,
            linesClearedSession = currentState.linesClearedSession,
            isGameOver = currentState.isGameOver,
            isNewHighScore = currentState.isNewHighScore,
            dailyBatchIndex = currentState.dailyBatchIndex,
            hammerCount = currentState.hammerCount,
            shuffleCount = currentState.shuffleCount,
            bombCount = currentState.bombCount,
            rotateCount = currentState.rotateCount,
            gemCells = currentState.gemCells,
            iceCells = currentState.iceCells,
            adventureGemsCollected = currentState.adventureGemsCollected,
            adventureIceBroken = currentState.adventureIceBroken,
            adventureLevelCompleted = currentState.adventureLevelCompleted,
            timeRemainingMs = currentState.timeRemainingMs,
            gameOverReason = currentState.gameOverReason
        )

        val newGrid = currentState.grid.map { it.toMutableList() }.toMutableList()
        val mutableIce = currentState.iceCells.toMutableMap()
        val mutableGems = currentState.gemCells.toMutableSet()
        var gemsAdded = 0
        var iceAdded = 0
        for (b in bursts) {
            val coord = b.row to b.col
            newGrid[b.row][b.col] = 0
            if (mutableIce.remove(coord) != null) iceAdded++
            if (mutableGems.remove(coord)) gemsAdded++
        }
        if (gemsAdded > 0) {
            preferences.coinsBalance = preferences.coinsBalance + gemsAdded * 5
            preferences.totalCoinsEarned = preferences.totalCoinsEarned + gemsAdded * 5
        }
        val nextGemsCollected = currentState.adventureGemsCollected + gemsAdded
        val nextIceBroken = currentState.adventureIceBroken + iceAdded
        val frozenGrid = newGrid.map { it.toList() }

        val newBombCount = (currentState.bombCount - 1).coerceAtLeast(0)
        preferences.bombCount = newBombCount

        soundManager.playHammerSmash()
        triggerHaptic(HapticEffect.LineClear(3))

        val adventureDone = evaluateAdventureClearAfterBooster(
            state = currentState,
            nextIce = mutableIce,
            nextGemsCollected = nextGemsCollected
        )
        val stillGameOver = if (adventureDone) false else checkGameOver(frozenGrid, currentState.activePieces)
        val bombBanner = "💣 3×3 BLAST! ${bursts.size} BLOCKS CLEARED!"

        _gameState.update {
            it.copy(
                grid = frozenGrid,
                gemCells = mutableGems.toSet(),
                iceCells = mutableIce.toMap(),
                adventureGemsCollected = nextGemsCollected,
                adventureIceBroken = nextIceBroken,
                adventureLevelCompleted = adventureDone || it.adventureLevelCompleted,
                adventureEarnedStars = if (adventureDone && !it.adventureLevelCompleted) {
                    AdventureCatalog.computeStars(AdventureCatalog.level(it.adventureLevelNumber), it.score)
                } else it.adventureEarnedStars,
                adventureMaxUnlockedLevel = preferences.adventureMaxUnlockedLevel,
                coinsBalance = preferences.coinsBalance,
                bombCount = newBombCount,
                isBombActive = false,
                isHammerActive = false,
                isRotateActive = false,
                activeHint = null,
                isGameOver = stillGameOver,
                lineClearBursts = bursts,
                comboBannerText = bombBanner,
                shakeEventId = it.shakeEventId + 1L,
                canUndo = true
            )
        }

        flushSession(syncToCloud = true)

        viewModelScope.launch {
            delay(450)
            _gameState.update { it.copy(lineClearBursts = emptyList()) }
            delay(900)
            _gameState.update {
                if (it.comboBannerText == bombBanner) it.copy(comboBannerText = null) else it
            }
        }
        return true
    }

    /**
     * Unlocks (with Blast Coins) or equips a board theme from the Coin Shop.
     */
    fun selectOrUnlockTheme(themeId: BoardThemeId): Boolean {
        val spec = BoardThemeCatalog.allThemes.firstOrNull { it.id == themeId } ?: return false
        val alreadyUnlocked = preferences.isThemeUnlocked(themeId.name)

        if (alreadyUnlocked) {
            preferences.selectedThemeId = themeId.name
            soundManager.playPickup()
            triggerHaptic(HapticEffect.Pickup)
            val equippedBanner = "${spec.badgeEmoji} ${spec.name.uppercase()} EQUIPPED!"
            _gameState.update {
                it.copy(
                    selectedThemeId = themeId.name,
                    unlockedThemeIds = preferences.getUnlockedThemeIds(),
                    comboBannerText = equippedBanner
                )
            }
            viewModelScope.launch {
                delay(1400)
                _gameState.update {
                    if (it.comboBannerText == equippedBanner) it.copy(comboBannerText = null) else it
                }
            }
            return true
        }

        val currentCoins = preferences.coinsBalance
        if (currentCoins < spec.costCoins || _gameState.value.coinsBalance < spec.costCoins) {
            showTemporaryBanner("NEED ${spec.costCoins} 🪙 TO UNLOCK ${spec.name.uppercase()}!")
            return false
        }

        val nextCoins = (currentCoins - spec.costCoins).coerceAtLeast(0)
        preferences.coinsBalance = nextCoins
        preferences.unlockTheme(themeId.name)
        preferences.selectedThemeId = themeId.name

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        val unlockedBanner = "${spec.badgeEmoji} ${spec.name.uppercase()} UNLOCKED!"
        _gameState.update {
            it.copy(
                coinsBalance = nextCoins,
                selectedThemeId = themeId.name,
                unlockedThemeIds = preferences.getUnlockedThemeIds(),
                comboBannerText = unlockedBanner
            )
        }
        syncCoinSpendToCloud(spec.costCoins)

        viewModelScope.launch {
            delay(1600)
            _gameState.update {
                if (it.comboBannerText == unlockedBanner) it.copy(comboBannerText = null) else it
            }
        }
        return true
    }

    /**
     * Purchases a gameplay booster from the Coin Shop using Blast Coins.
     */
    fun buyBoosterWithCoins(booster: ShopBoosterType): Boolean {
        val currentCoins = preferences.coinsBalance
        if (currentCoins < booster.coinCost || _gameState.value.coinsBalance < booster.coinCost) {
            showTemporaryBanner("NEED ${booster.coinCost} 🪙 FOR ${booster.title.uppercase()}!")
            return false
        }

        val nextCoins = (currentCoins - booster.coinCost).coerceAtLeast(0)
        preferences.coinsBalance = nextCoins

        var nextHammer = preferences.hammerCount
        var nextHint = preferences.hintCount
        var nextShuffle = preferences.shuffleCount
        var nextBomb = preferences.bombCount
        var nextRotate = preferences.rotateCount

        when (booster) {
            ShopBoosterType.HAMMER -> {
                nextHammer += 1
                preferences.hammerCount = nextHammer
            }
            ShopBoosterType.HINT -> {
                nextHint += 1
                preferences.hintCount = nextHint
            }
            ShopBoosterType.SHUFFLE -> {
                nextShuffle += 1
                preferences.shuffleCount = nextShuffle
            }
            ShopBoosterType.BOMB -> {
                nextBomb += 1
                preferences.bombCount = nextBomb
            }
            ShopBoosterType.ROTATE -> {
                nextRotate += 1
                preferences.rotateCount = nextRotate
            }
        }

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        val boughtBanner = "+1 ${booster.emoji} ${booster.title.uppercase()}!"
        _gameState.update {
            it.copy(
                coinsBalance = nextCoins,
                hammerCount = nextHammer,
                hintCount = nextHint,
                shuffleCount = nextShuffle,
                bombCount = nextBomb,
                rotateCount = nextRotate,
                comboBannerText = boughtBanner
            )
        }
        flushSession(syncToCloud = true)
        syncCoinSpendToCloud(booster.coinCost)

        viewModelScope.launch {
            delay(1500)
            _gameState.update {
                if (it.comboBannerText == boughtBanner) it.copy(comboBannerText = null) else it
            }
        }
        return true
    }

    /**
     * Rotates the currently selected piece immediately, or toggles Rotate mode so the
     * player can tap any tray piece to rotate it 90° clockwise.
     */
    fun toggleRotateMode() {
        val current = _gameState.value
        if (current.isGameOver) return

        val selectedIdx = current.selectedShapeIndex
        if (selectedIdx != null && current.activePieces.getOrNull(selectedIdx) != null) {
            useRotateOnPiece(selectedIdx)
            return
        }

        if (current.rotateCount <= 0 && !current.isRotateActive) {
            showTemporaryBanner("NO ROTATES LEFT! VISIT SHOP OR WATCH AD")
            return
        }

        val willBeActive = !current.isRotateActive
        _gameState.update {
            it.copy(
                isRotateActive = willBeActive,
                isHammerActive = if (willBeActive) false else it.isHammerActive,
                isBombActive = if (willBeActive) false else it.isBombActive,
                activeHint = if (willBeActive) null else it.activeHint,
                hoverPreviewCells = if (willBeActive) emptySet() else it.hoverPreviewCells,
                prospectiveRows = if (willBeActive) emptySet() else it.prospectiveRows,
                prospectiveCols = if (willBeActive) emptySet() else it.prospectiveCols,
                comboBannerText = if (willBeActive) "↻ TAP ANY TRAY PIECE TO ROTATE 90°!" else null
            )
        }
        if (willBeActive) {
            soundManager.playPickup()
            triggerHaptic(HapticEffect.Pickup)
        }
    }

    fun useRotateOnPiece(pieceIndex: Int): Boolean {
        val currentState = _gameState.value
        if (currentState.isGameOver) return false
        val shape = currentState.activePieces.getOrNull(pieceIndex) ?: return false

        if (currentState.rotateCount <= 0) {
            showTemporaryBanner("NO ROTATES LEFT! VISIT SHOP OR WATCH AD")
            return false
        }

        if (shape.isRotationSymmetric()) {
            showTemporaryBanner("PIECE ALREADY SYMMETRIC — TAP ANOTHER!")
            return false
        }

        hasUserInteractedThisSession = true
        lastUndoSnapshot = UndoSnapshot(
            grid = currentState.grid,
            activePieces = currentState.activePieces,
            score = currentState.score,
            highScore = currentState.highScore,
            comboStreak = currentState.comboStreak,
            maxComboSession = currentState.maxComboSession,
            linesClearedSession = currentState.linesClearedSession,
            isGameOver = currentState.isGameOver,
            isNewHighScore = currentState.isNewHighScore,
            dailyBatchIndex = currentState.dailyBatchIndex,
            hammerCount = currentState.hammerCount,
            shuffleCount = currentState.shuffleCount,
            bombCount = currentState.bombCount,
            rotateCount = currentState.rotateCount,
            gemCells = currentState.gemCells,
            iceCells = currentState.iceCells,
            adventureGemsCollected = currentState.adventureGemsCollected,
            adventureIceBroken = currentState.adventureIceBroken,
            adventureLevelCompleted = currentState.adventureLevelCompleted,
            timeRemainingMs = currentState.timeRemainingMs,
            gameOverReason = currentState.gameOverReason
        )

        val rotatedShape = shape.rotate90Clockwise()
        val updatedTray = currentState.activePieces.toMutableList()
        updatedTray[pieceIndex] = rotatedShape

        val nextRotateCount = (currentState.rotateCount - 1).coerceAtLeast(0)
        preferences.rotateCount = nextRotateCount

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        val banner = "↻ PIECE ROTATED 90°!"
        _gameState.update {
            it.copy(
                activePieces = updatedTray,
                rotateCount = nextRotateCount,
                isRotateActive = false,
                selectedShapeIndex = pieceIndex,
                activeHint = null,
                hoverRow = null,
                hoverCol = null,
                hoverPreviewCells = emptySet(),
                prospectiveRows = emptySet(),
                prospectiveCols = emptySet(),
                comboBannerText = banner,
                canUndo = true
            )
        }

        flushSession(syncToCloud = true)
        viewModelScope.launch {
            delay(1300)
            _gameState.update {
                if (it.comboBannerText == banner) it.copy(comboBannerText = null) else it
            }
        }
        return true
    }

    private fun evaluateAdventureClearAfterBooster(
        state: GameState,
        nextIce: Map<Pair<Int, Int>, Int>,
        nextGemsCollected: Int
    ): Boolean {
        if (state.gameMode != GameMode.ADVENTURE || state.adventureLevelCompleted) return false
        val spec = AdventureCatalog.level(state.adventureLevelNumber)
        val cleared = when (spec.goalType) {
            AdventureGoalType.COLLECT_GEMS -> nextGemsCollected >= spec.targetCount
            AdventureGoalType.BREAK_ICE -> nextIce.isEmpty()
            AdventureGoalType.SCORE_TARGET -> state.score >= spec.targetCount
        }
        if (cleared) {
            val stars = AdventureCatalog.computeStars(spec, state.score)
            preferences.setAdventureLevelStars(spec.levelNumber, stars)
            val nextUnlocked = (spec.levelNumber + 1).coerceAtMost(AdventureCatalog.totalLevels)
            if (nextUnlocked > preferences.adventureMaxUnlockedLevel) {
                preferences.adventureMaxUnlockedLevel = nextUnlocked
            }
            val firstClear = !preferences.isAdventureLevelRewardClaimed(spec.levelNumber)
            val rewardCoins = if (firstClear) spec.coinReward else 20
            preferences.markAdventureLevelRewardClaimed(spec.levelNumber)
            preferences.coinsBalance = preferences.coinsBalance + rewardCoins
            preferences.totalCoinsEarned = preferences.totalCoinsEarned + rewardCoins
            soundManager.playComboCheer()
        }
        return cleared
    }

    private fun syncCoinSpendToCloud(costCoins: Int) {
        val repo = firestoreRepository ?: return
        if (currentUserId.isBlank() || costCoins <= 0) return
        viewModelScope.launch {
            val spendResult = repo.spendCoinsForHammer(costCoins)
            if (spendResult.isFailure) {
                repo.syncDailyLoginRewardProfile(buildLocalLoginProfile())
            }
        }
    }

    fun loadDailyMissions(dateStr: String): List<DailyMission> {
        val baseMissions = DailyMissionGenerator.generateMissionsForDate(dateStr)
        return baseMissions.map { mission ->
            val progress = preferences.getMissionProgress(mission.id)
            val claimed = preferences.isMissionClaimed(mission.id)
            mission.copy(
                current = progress,
                isCompleted = progress >= mission.target,
                isClaimed = claimed
            )
        }
    }

    fun refreshDailyMissions() {
        val dateStr = getTodayDateString()
        val missions = loadDailyMissions(dateStr)
        val unclaimed = missions.count { it.isCompleted && !it.isClaimed }
        _gameState.update {
            it.copy(
                dailyMissions = missions,
                unclaimedMissionsCount = unclaimed
            )
        }
    }

    fun recordMissionEvent(type: MissionType, amount: Int) {
        val dateStr = getTodayDateString()
        val currentMissions = _gameState.value.dailyMissions.ifEmpty {
            loadDailyMissions(dateStr)
        }

        var stateChanged = false
        val updated = currentMissions.map { mission ->
            if (mission.type == type && !mission.isClaimed) {
                val newProgress = when (type) {
                    MissionType.REACH_COMBO,
                    MissionType.CLEAR_LINES_SINGLE_MOVE,
                    MissionType.SCORE_POINTS -> maxOf(mission.current, amount)
                    else -> mission.current + amount
                }.coerceAtMost(mission.target)

                if (newProgress != mission.current) {
                    stateChanged = true
                    preferences.setMissionProgress(mission.id, newProgress)
                    mission.copy(
                        current = newProgress,
                        isCompleted = newProgress >= mission.target
                    )
                } else {
                    mission
                }
            } else {
                mission
            }
        }

        if (stateChanged) {
            val unclaimed = updated.count { it.isCompleted && !it.isClaimed }
            _gameState.update {
                it.copy(
                    dailyMissions = updated,
                    unclaimedMissionsCount = unclaimed
                )
            }
        }
    }

    fun claimMissionReward(missionId: String): Boolean {
        val currentState = _gameState.value
        val mission = currentState.dailyMissions.find { it.id == missionId } ?: return false
        if (!mission.isCompleted || mission.isClaimed) return false

        preferences.setMissionClaimed(missionId, true)
        val newCoinsBalance = preferences.coinsBalance + mission.coinReward
        val newTotalCoinsEarned = preferences.totalCoinsEarned + mission.coinReward
        preferences.coinsBalance = newCoinsBalance
        preferences.totalCoinsEarned = newTotalCoinsEarned

        val newHammerCount = currentState.hammerCount + mission.hammerReward
        preferences.hammerCount = newHammerCount

        val newHintCount = currentState.hintCount + mission.hintReward
        preferences.hintCount = newHintCount

        val updatedMissions = currentState.dailyMissions.map {
            if (it.id == missionId) it.copy(isClaimed = true) else it
        }
        val unclaimed = updatedMissions.count { it.isCompleted && !it.isClaimed }

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        val rewardParts = mutableListOf<String>()
        if (mission.coinReward > 0) rewardParts.add("+${mission.coinReward} 🪙")
        if (mission.hintReward > 0) rewardParts.add("+${mission.hintReward} 💡")
        if (mission.hammerReward > 0) rewardParts.add("+${mission.hammerReward} 🔨")
        val claimedBanner = "MISSION CLAIMED: ${rewardParts.joinToString(" & ")}!"

        _gameState.update {
            it.copy(
                coinsBalance = newCoinsBalance,
                hintCount = newHintCount,
                hammerCount = newHammerCount,
                dailyMissions = updatedMissions,
                unclaimedMissionsCount = unclaimed,
                comboBannerText = claimedBanner
            )
        }

        flushSession(syncToCloud = true)

        val repo = firestoreRepository
        if (repo != null && currentUserId.isNotBlank() && mission.coinReward > 0) {
            val localSeed = buildLocalLoginProfile()
            viewModelScope.launch {
                repo.creditBonusCoins(coins = mission.coinReward, localSeed = localSeed)
            }
        }

        viewModelScope.launch {
            delay(1600)
            _gameState.update {
                if (it.comboBannerText == claimedBanner) it.copy(comboBannerText = null) else it
            }
        }

        return true
    }

    fun evaluateAndSyncAchievements(
        currentScore: Int = _gameState.value.score,
        currentMaxCombo: Int = _gameState.value.maxComboSession,
        notifyUnlocks: Boolean = false
    ): List<Achievement> {
        val prevUnlocked = AchievementCatalog.definitions
            .filter { preferences.isAchievementUnlocked(it.id) }
            .map { it.id }
            .toSet()
        val prevProgress = AchievementCatalog.definitions
            .associate { it.id to preferences.getAchievementProgress(it.id) }

        val bestSingleRun = maxOf(
            preferences.highScore,
            preferences.timeAttackHighScore,
            preferences.getDailyHighScore(getTodayDateString()),
            currentScore
        )
        val evaluated = AchievementCatalog.evaluateAll(
            maxCombo = maxOf(preferences.highestCombo, currentMaxCombo),
            bestScore = bestSingleRun,
            dailyStreak = preferences.dailyStreak,
            totalLines = preferences.totalLinesCleared,
            dailyCompletedCount = preferences.dailyChallengesCompleted,
            previouslyUnlockedIds = prevUnlocked,
            previousProgressById = prevProgress
        )

        val newlyUnlocked = mutableListOf<Achievement>()
        for (ach in evaluated) {
            if (ach.current > (prevProgress[ach.id] ?: 0)) {
                preferences.setAchievementProgress(ach.id, ach.current)
            }
            if (ach.isUnlocked && ach.id !in prevUnlocked) {
                preferences.setAchievementUnlocked(ach.id, true)
                newlyUnlocked.add(ach)
            }
        }

        val unlockedCount = evaluated.count { it.isUnlocked }
        val todayStr = getTodayDateString()
        val isSolvedToday = preferences.isDailyDateCompleted(todayStr)

        if (notifyUnlocks && newlyUnlocked.isNotEmpty()) {
            soundManager.playPowerup()
            triggerHaptic(HapticEffect.PowerUp)
            val unlockedTitle = newlyUnlocked.first().title
            _gameState.update {
                it.copy(
                    achievements = evaluated,
                    unlockedAchievementsCount = unlockedCount,
                    dailyStreak = preferences.dailyStreak,
                    isDailyCompletedToday = isSolvedToday,
                    comboBannerText = "🏅 ACHIEVEMENT: $unlockedTitle!"
                )
            }
        } else {
            _gameState.update {
                it.copy(
                    achievements = evaluated,
                    unlockedAchievementsCount = unlockedCount,
                    dailyStreak = preferences.dailyStreak,
                    isDailyCompletedToday = isSolvedToday
                )
            }
        }
        return evaluated
    }

    private fun buildLocalLoginProfile(): DailyLoginRewardProfile {
        return DailyLoginRewardProfile(
            id = "daily_login",
            userId = currentUserId,
            loginStreak = preferences.loginStreak.toLong(),
            longestLoginStreak = preferences.longestLoginStreak.toLong(),
            lastClaimDate = preferences.lastLoginClaimDate,
            totalDaysClaimed = preferences.totalLoginDaysClaimed.toLong(),
            coinsBalance = preferences.coinsBalance.toLong(),
            totalCoinsEarned = preferences.totalCoinsEarned.toLong(),
            totalHammersEarned = preferences.totalLoginHammersEarned.toLong()
        )
    }

    private fun refreshLoginRewardStateFromPreferences() {
        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()
        val lastDate = preferences.lastLoginClaimDate
        val storedStreak = preferences.loginStreak
        val activeStreak = DailyLoginRewardSchedule.computeActiveStreak(lastDate, storedStreak, today, yesterday)
        val canClaim = DailyLoginRewardSchedule.canClaimToday(lastDate, today)
        val nextDay = DailyLoginRewardSchedule.nextClaimCycleDay(lastDate, storedStreak, today, yesterday)

        _gameState.update {
            it.copy(
                coinsBalance = preferences.coinsBalance,
                loginStreak = activeStreak,
                longestLoginStreak = maxOf(preferences.longestLoginStreak, activeStreak),
                lastLoginClaimDate = lastDate,
                canClaimDailyLoginReward = canClaim,
                dailyLoginNextCycleDay = nextDay
            )
        }
    }

    private fun applyCloudLoginProfile(profile: DailyLoginRewardProfile) {
        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()
        val cloudHammersTotal = profile.totalHammersEarned.toInt().coerceAtLeast(0)
        val hammerDelta = (cloudHammersTotal - preferences.totalLoginHammersEarned).coerceAtLeast(0)
        if (hammerDelta > 0) {
            preferences.hammerCount = preferences.hammerCount + hammerDelta
            preferences.totalLoginHammersEarned = cloudHammersTotal
        } else if (cloudHammersTotal > preferences.totalLoginHammersEarned) {
            preferences.totalLoginHammersEarned = cloudHammersTotal
        }

        preferences.coinsBalance = profile.coinsBalance.toInt().coerceAtLeast(0)
        preferences.loginStreak = profile.loginStreak.toInt().coerceAtLeast(0)
        preferences.longestLoginStreak = maxOf(
            preferences.longestLoginStreak,
            profile.longestLoginStreak.toInt().coerceAtLeast(0)
        )
        preferences.lastLoginClaimDate = profile.lastClaimDate
        preferences.totalLoginDaysClaimed = maxOf(
            preferences.totalLoginDaysClaimed,
            profile.totalDaysClaimed.toInt().coerceAtLeast(0)
        )
        preferences.totalCoinsEarned = maxOf(
            preferences.totalCoinsEarned,
            profile.totalCoinsEarned.toInt().coerceAtLeast(0)
        )

        val activeStreak = DailyLoginRewardSchedule.computeActiveStreak(
            lastClaimDate = profile.lastClaimDate,
            storedStreak = profile.loginStreak.toInt(),
            todayDate = today,
            yesterdayDate = yesterday
        )
        val canClaim = DailyLoginRewardSchedule.canClaimToday(profile.lastClaimDate, today)
        val nextDay = DailyLoginRewardSchedule.nextClaimCycleDay(
            lastClaimDate = profile.lastClaimDate,
            storedStreak = profile.loginStreak.toInt(),
            todayDate = today,
            yesterdayDate = yesterday
        )

        _gameState.update {
            it.copy(
                hammerCount = preferences.hammerCount,
                coinsBalance = preferences.coinsBalance,
                loginStreak = activeStreak,
                longestLoginStreak = preferences.longestLoginStreak,
                lastLoginClaimDate = profile.lastClaimDate,
                canClaimDailyLoginReward = canClaim,
                dailyLoginNextCycleDay = nextDay,
                isDailyLoginLoading = false,
                dailyLoginError = null
            )
        }
    }

    private fun syncAndObserveDailyLoginReward() {
        val repo = firestoreRepository ?: return
        if (currentUserId.isBlank()) return
        loginRewardJob?.cancel()

        viewModelScope.launch {
            val localProfile = buildLocalLoginProfile()
            if (localProfile.totalDaysClaimed > 0L || localProfile.coinsBalance > 0L) {
                repo.syncDailyLoginRewardProfile(localProfile).getOrNull()?.let { synced ->
                    applyCloudLoginProfile(synced)
                }
            }
        }

        loginRewardJob = viewModelScope.launch {
            repo.observeDailyLoginReward()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing daily login reward", rootError)
                    _gameState.update {
                        it.copy(
                            isDailyLoginLoading = false,
                            dailyLoginError = rootError.message ?: "Unable to sync daily rewards"
                        )
                    }
                }
                .collect { profile ->
                    if (profile != null) {
                        applyCloudLoginProfile(profile)
                    } else {
                        refreshLoginRewardStateFromPreferences()
                    }
                }
        }
    }

    private fun formatLoginRewardBanner(tier: DailyLoginRewardTier): String {
        val parts = mutableListOf<String>()
        if (tier.coins > 0) parts.add("+${tier.coins} 🪙")
        if (tier.hammers > 0) parts.add("+${tier.hammers} 🔨")
        return "DAY ${tier.cycleDay} BONUS: ${parts.joinToString(" & ")}!"
    }

    fun claimDailyLoginReward() {
        val current = _gameState.value
        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()
        if (current.isDailyLoginLoading) return
        if (!DailyLoginRewardSchedule.canClaimToday(preferences.lastLoginClaimDate, today)) {
            refreshLoginRewardStateFromPreferences()
            return
        }

        val repo = firestoreRepository
        if (repo == null || currentUserId.isBlank()) {
            val nextStreak = DailyLoginRewardSchedule.computeNextStreak(
                lastClaimDate = preferences.lastLoginClaimDate,
                storedStreak = preferences.loginStreak,
                todayDate = today,
                yesterdayDate = yesterday
            )
            val tier = DailyLoginRewardSchedule.tierForStreak(nextStreak)
            val nextLongest = maxOf(preferences.longestLoginStreak, nextStreak)
            val nextTotalDays = preferences.totalLoginDaysClaimed + 1
            val nextCoins = preferences.coinsBalance + tier.coins
            val nextTotalCoins = preferences.totalCoinsEarned + tier.coins
            val nextTotalHammers = preferences.totalLoginHammersEarned + tier.hammers
            val nextHammerCount = preferences.hammerCount + tier.hammers

            preferences.loginStreak = nextStreak
            preferences.longestLoginStreak = nextLongest
            preferences.lastLoginClaimDate = today
            preferences.totalLoginDaysClaimed = nextTotalDays
            preferences.coinsBalance = nextCoins
            preferences.totalCoinsEarned = nextTotalCoins
            preferences.totalLoginHammersEarned = nextTotalHammers
            preferences.hammerCount = nextHammerCount

            soundManager.playPowerup()
            triggerHaptic(HapticEffect.PowerUp)

            _gameState.update {
                it.copy(
                    hammerCount = nextHammerCount,
                    coinsBalance = nextCoins,
                    loginStreak = nextStreak,
                    longestLoginStreak = nextLongest,
                    lastLoginClaimDate = today,
                    canClaimDailyLoginReward = false,
                    dailyLoginNextCycleDay = tier.cycleDay,
                    isDailyLoginLoading = false,
                    dailyLoginError = null,
                    comboBannerText = formatLoginRewardBanner(tier)
                )
            }
            flushSession(syncToCloud = false)

            viewModelScope.launch {
                delay(1800)
                _gameState.update { it.copy(comboBannerText = null) }
            }
            return
        }

        _gameState.update { it.copy(isDailyLoginLoading = true, dailyLoginError = null) }
        val seedProfile = buildLocalLoginProfile()
        viewModelScope.launch {
            val result = repo.claimDailyLoginReward(
                todayDate = today,
                yesterdayDate = yesterday,
                localSeed = seedProfile
            )
            result.onSuccess { claimResult ->
                applyCloudLoginProfile(claimResult.profile)
                val grantedTier = claimResult.grantedTier
                if (claimResult.claimed && grantedTier != null) {
                    soundManager.playPowerup()
                    triggerHaptic(HapticEffect.PowerUp)
                    _gameState.update {
                        it.copy(comboBannerText = formatLoginRewardBanner(grantedTier))
                    }
                    flushSession(syncToCloud = true)
                    delay(1800)
                    _gameState.update { it.copy(comboBannerText = null) }
                }
            }.onFailure { error ->
                _gameState.update {
                    it.copy(
                        isDailyLoginLoading = false,
                        dailyLoginError = error.message ?: "Unable to claim daily reward"
                    )
                }
            }
        }
    }

    fun exchangeCoinsForHammer(costCoins: Int = DailyLoginRewardSchedule.COIN_TO_HAMMER_COST): Boolean {
        val current = _gameState.value
        if (current.coinsBalance < costCoins || preferences.coinsBalance < costCoins) {
            return false
        }
        val nextCoins = (preferences.coinsBalance - costCoins).coerceAtLeast(0)
        val nextHammers = preferences.hammerCount + 1
        preferences.coinsBalance = nextCoins
        preferences.hammerCount = nextHammers

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        _gameState.update {
            it.copy(
                coinsBalance = nextCoins,
                hammerCount = nextHammers,
                dailyLoginError = null,
                comboBannerText = "+1 🔨 CRAFTED!"
            )
        }
        flushSession(syncToCloud = true)

        val repo = firestoreRepository
        if (repo != null && currentUserId.isNotBlank()) {
            viewModelScope.launch {
                val spendResult = repo.spendCoinsForHammer(costCoins)
                if (spendResult.isFailure) {
                    // Ensure guest-earned coins already synced to Firestore also reflect the spend
                    repo.syncDailyLoginRewardProfile(buildLocalLoginProfile())
                }
            }
        }

        viewModelScope.launch {
            delay(1500)
            _gameState.update {
                if (it.comboBannerText == "+1 🔨 CRAFTED!") it.copy(comboBannerText = null) else it
            }
        }
        return true
    }

    fun onRewardedAdStarted() {
        soundManager.setMusicForegrounded(false)
        setTimeAttackPaused(true)
    }

    fun onRewardedAdEnded(restoreMusicForeground: Boolean = true) {
        soundManager.setMusicForegrounded(restoreMusicForeground)
    }

    fun grantSecondChanceAfterAd() {
        useSecondChance()
    }

    fun grantFreeHammerAfterAd() {
        val nextHammers = preferences.hammerCount + 1
        preferences.hammerCount = nextHammers

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        val earnedBanner = "+1 🔨 FREE HAMMER EARNED!"
        _gameState.update {
            it.copy(
                hammerCount = nextHammers,
                comboBannerText = earnedBanner
            )
        }
        flushSession(syncToCloud = true)

        viewModelScope.launch {
            delay(1600)
            _gameState.update {
                if (it.comboBannerText == earnedBanner) it.copy(comboBannerText = null) else it
            }
        }
    }

    fun grantFreeShuffleAfterAd() {
        val nextShuffles = preferences.shuffleCount + 1
        preferences.shuffleCount = nextShuffles

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        val earnedBanner = "+1 🔄 FREE SHUFFLE EARNED!"
        _gameState.update {
            it.copy(
                shuffleCount = nextShuffles,
                comboBannerText = earnedBanner
            )
        }
        flushSession(syncToCloud = true)

        viewModelScope.launch {
            delay(1600)
            _gameState.update {
                if (it.comboBannerText == earnedBanner) it.copy(comboBannerText = null) else it
            }
        }
    }

    fun grantFreeBombAfterAd() {
        val nextBombs = preferences.bombCount + 1
        preferences.bombCount = nextBombs

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        val earnedBanner = "+1 💣 FREE BOMB EARNED!"
        _gameState.update {
            it.copy(
                bombCount = nextBombs,
                comboBannerText = earnedBanner
            )
        }
        flushSession(syncToCloud = true)

        viewModelScope.launch {
            delay(1600)
            _gameState.update {
                if (it.comboBannerText == earnedBanner) it.copy(comboBannerText = null) else it
            }
        }
    }

    fun grantFreeRotateAfterAd() {
        val nextRotates = preferences.rotateCount + 1
        preferences.rotateCount = nextRotates

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        val earnedBanner = "+1 ↻ FREE ROTATE EARNED!"
        _gameState.update {
            it.copy(
                rotateCount = nextRotates,
                comboBannerText = earnedBanner
            )
        }
        flushSession(syncToCloud = true)

        viewModelScope.launch {
            delay(1600)
            _gameState.update {
                if (it.comboBannerText == earnedBanner) it.copy(comboBannerText = null) else it
            }
        }
    }

    fun showTemporaryBanner(message: String) {
        if (message.isBlank()) return
        _gameState.update { it.copy(comboBannerText = message) }
        viewModelScope.launch {
            delay(1800)
            _gameState.update {
                if (it.comboBannerText == message) it.copy(comboBannerText = null) else it
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.setMusicForegrounded(false)
        stopTimeAttackTimer()
        leaderboardJob?.cancel()
        globalLeaderboardJob?.cancel()
        highScoreLeaderboardJob?.cancel()
        loginRewardJob?.cancel()
    }

    companion object {
        private const val TAG = "GameViewModel"
        const val TIME_ATTACK_INITIAL_MS = 60_000L
        const val TIME_ATTACK_MAX_MS = 99_000L
        const val TIME_ATTACK_BONUS_PER_LINE_MS = 4_000L
        const val TIME_ATTACK_COMBO_BONUS_MS = 1_000L
        const val TIME_ATTACK_SECOND_CHANCE_MS = 15_000L
        const val PERFECT_BOARD_CLEAR_POINTS = 500
        const val PERFECT_BOARD_CLEAR_COINS = 25

        fun provideFactory(
            context: Context,
            soundManager: SoundManager,
            preferences: GamePreferences,
            localSessionStorage: LocalSessionStorage? = LocalSessionStorage(context),
            firestoreRepository: GameFirestoreRepository? = null,
            currentUserId: String = "",
            currentUserDisplayName: String = "Player"
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameViewModel(
                    context = context,
                    soundManager = soundManager,
                    preferences = preferences,
                    localSessionStorage = localSessionStorage,
                    firestoreRepository = firestoreRepository,
                    currentUserId = currentUserId,
                    currentUserDisplayName = currentUserDisplayName
                ) as T
            }
        }
    }
}
