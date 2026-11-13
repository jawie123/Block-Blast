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
import com.example.model.BlockShape
import com.example.model.DailyMission
import com.example.model.DailyMissionGenerator
import com.example.model.DailyPuzzleGenerator
import com.example.model.DifficultyTier
import com.example.model.GameMode
import com.example.model.GameOverReason
import com.example.model.GameState
import com.example.model.LineClearBurst
import com.example.model.MissionType
import com.example.model.SavedGameSessionData
import com.example.model.ScorePopup
import com.example.model.ShapeDefinitions
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
                    isNewHighScore = it.isNewHighScore || isNewRecord,
                    isHammerActive = false,
                    selectedShapeIndex = null,
                    hoverRow = null,
                    hoverCol = null,
                    hoverPreviewCells = emptySet(),
                    prospectiveRows = emptySet(),
                    prospectiveCols = emptySet()
                )
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
        if (firestoreRepository != null && currentUserId.isNotBlank()) {
            observeDailyLeaderboard()
            observeGlobalLeaderboard()
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
        val dateStr = session.dailyDate.ifEmpty { getTodayDateString() }
        val dateFormatted = getTodayFormattedString()
        val seed = computeDailySeed()
        val puzzleTitle = if (isDaily) DailyPuzzleGenerator.getPuzzleTitle(seed) else ""

        val savedHigh = when {
            isDaily -> maxOf(preferences.getDailyHighScore(dateStr), session.score)
            isTimeAttack -> maxOf(preferences.timeAttackHighScore, session.score)
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
            dailyMissions = dailyMissions,
            unclaimedMissionsCount = unclaimedCount,
            difficultyLevel = resumedTier.level,
            difficultyTitle = resumedTier.title,
            difficultyProgress = resumedProgress,
            nextDifficultyScore = resumedTier.maxScoreExclusive,
            timeRemainingMs = resumedTimeMs,
            timeMaxMs = TIME_ATTACK_MAX_MS,
            lastTimeBonusSeconds = 0,
            gameOverReason = GameOverReason.NO_MOVES,
            leaderboardEntries = currentState.leaderboardEntries,
            isLeaderboardLoading = currentState.isLeaderboardLoading,
            leaderboardError = currentState.leaderboardError,
            totalAllTimeScore = preferences.totalAllTimeScore,
            globalLeaderboardEntries = currentState.globalLeaderboardEntries,
            isGlobalLeaderboardLoading = currentState.isGlobalLeaderboardLoading,
            globalLeaderboardError = currentState.globalLeaderboardError,
            isMusicEnabled = preferences.isMusicEnabled,
            isSoundEnabled = preferences.isSoundEnabled,
            isHapticsEnabled = preferences.isHapticsEnabled,
            currentUserId = currentUserId,
            currentUserDisplayName = currentUserDisplayName
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
            clientUpdatedAt = now
        )
        localSessionStorage?.saveSession(session)

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

    fun getTodayFormattedString(): String {
        return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
    }

    /**
     * Initializes a new game for Classic or Daily Challenge mode.
     * In Daily Challenge mode, generates the deterministic procedural 8x8 board layout.
     */
    fun startNewGame(
        mode: GameMode = _gameState.value.gameMode,
        persistFreshState: Boolean = true
    ) {
        lastUndoSnapshot = null
        val isDaily = mode == GameMode.DAILY_CHALLENGE
        val isTimeAttack = mode == GameMode.TIME_ATTACK
        val dateStr = getTodayDateString()
        val dateFormatted = getTodayFormattedString()
        val seed = computeDailySeed()

        val initialGrid = if (isDaily) {
            DailyPuzzleGenerator.generateInitialBoard(seed)
        } else {
            List(8) { List(8) { 0 } }
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
            dailyMissions = dailyMissions,
            unclaimedMissionsCount = unclaimedCount,
            difficultyLevel = DifficultyTier.STARTER.level,
            difficultyTitle = DifficultyTier.STARTER.title,
            difficultyProgress = 0f,
            nextDifficultyScore = DifficultyTier.STARTER.maxScoreExclusive,
            timeRemainingMs = TIME_ATTACK_INITIAL_MS,
            timeMaxMs = TIME_ATTACK_MAX_MS,
            lastTimeBonusSeconds = 0,
            gameOverReason = GameOverReason.NO_MOVES,
            leaderboardEntries = currentState.leaderboardEntries,
            isLeaderboardLoading = currentState.isLeaderboardLoading,
            leaderboardError = currentState.leaderboardError,
            totalAllTimeScore = preferences.totalAllTimeScore,
            globalLeaderboardEntries = currentState.globalLeaderboardEntries,
            isGlobalLeaderboardLoading = currentState.isGlobalLeaderboardLoading,
            globalLeaderboardError = currentState.globalLeaderboardError,
            isMusicEnabled = preferences.isMusicEnabled,
            isSoundEnabled = preferences.isSoundEnabled,
            isHapticsEnabled = preferences.isHapticsEnabled,
            currentUserId = currentUserId,
            currentUserDisplayName = currentUserDisplayName
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
                timeRemainingMs = snapshot.timeRemainingMs,
                lastTimeBonusSeconds = 0,
                isHammerActive = false,
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

        if (canPlaceShape(state.grid, shape, targetRow, targetCol)) {
            val previewCells = mutableSetOf<Pair<Int, Int>>()
            for (r in 0 until shape.height) {
                for (c in 0 until shape.width) {
                    if (shape.matrix[r][c]) {
                        previewCells.add(Pair(targetRow + r, targetCol + c))
                    }
                }
            }

            val prospectiveRows = mutableSetOf<Int>()
            val prospectiveCols = mutableSetOf<Int>()

            for (r in 0 until 8) {
                val full = (0 until 8).all { c ->
                    state.grid[r][c] != 0 || previewCells.contains(Pair(r, c))
                }
                if (full) prospectiveRows.add(r)
            }

            for (c in 0 until 8) {
                val full = (0 until 8).all { r ->
                    state.grid[r][c] != 0 || previewCells.contains(Pair(r, c))
                }
                if (full) prospectiveCols.add(c)
            }

            _gameState.update {
                it.copy(
                    hoverRow = targetRow,
                    hoverCol = targetCol,
                    hoverPreviewCells = previewCells,
                    prospectiveRows = prospectiveRows,
                    prospectiveCols = prospectiveCols
                )
            }
        } else {
            _gameState.update {
                it.copy(
                    hoverRow = targetRow,
                    hoverCol = targetCol,
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
                newGrid[coord.first][coord.second] = 0
            }

            if (newCombo >= 2) {
                soundManager.playComboCheer()
            } else {
                soundManager.playLineClear(newCombo)
            }
            triggerHaptic(HapticEffect.LineClear(newCombo))
        } else {
            triggerHaptic(HapticEffect.Placement)
        }

        val totalMovePoints = basePoints + linePoints
        val newScore = currentState.score + totalMovePoints
        val newHigh = maxOf(newScore, currentState.highScore)
        val isDaily = currentState.gameMode == GameMode.DAILY_CHALLENGE
        val isTimeAttack = currentState.gameMode == GameMode.TIME_ATTACK
        val isNewRecord = when {
            isDaily -> newScore > preferences.getDailyHighScore(currentState.dailyDate)
            isTimeAttack -> newScore > preferences.timeAttackHighScore
            else -> newScore > preferences.highScore
        }

        if (isNewRecord) {
            when {
                isDaily -> preferences.setDailyHighScore(currentState.dailyDate, newScore)
                isTimeAttack -> preferences.timeAttackHighScore = newScore
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
        val bannerText = when {
            leveledUp && withTimeBonusBanner != null -> "LV ${newTier.level} ${newTier.title}! $withTimeBonusBanner"
            leveledUp -> "DIFFICULTY UP! LV ${newTier.level} ${newTier.title}"
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

        val frozenGrid = newGrid.map { it.toList() }
        val isGameOver = checkGameOver(frozenGrid, finalPieces)
        val updatedMaxCombo = maxOf(currentState.maxComboSession, newCombo)
        val updatedLines = currentState.linesClearedSession + totalLines
        val updatedAllTimeTotal = if (totalMovePoints > 0) {
            preferences.addToTotalAllTimeScore(totalMovePoints)
        } else {
            preferences.totalAllTimeScore
        }

        _gameState.update {
            it.copy(
                grid = frozenGrid,
                activePieces = finalPieces,
                score = newScore,
                highScore = newHigh,
                totalAllTimeScore = updatedAllTimeTotal,
                comboStreak = if (totalLines > 0) newCombo else 0,
                comboEventId = if (totalLines > 0) currentState.comboEventId + 1L else currentState.comboEventId,
                lastClearedLinesCount = totalLines,
                maxComboSession = updatedMaxCombo,
                linesClearedSession = updatedLines,
                isGameOver = isGameOver,
                gameOverReason = if (isGameOver) GameOverReason.NO_MOVES else it.gameOverReason,
                isNewHighScore = isNewRecord,
                selectedShapeIndex = null,
                hoverRow = null,
                hoverCol = null,
                hoverPreviewCells = emptySet(),
                prospectiveRows = emptySet(),
                prospectiveCols = emptySet(),
                clearingCells = clearingCoords,
                lineClearBursts = lineBursts,
                comboBannerText = bannerText,
                scorePopups = popups,
                canUndo = true,
                dailyBatchIndex = nextBatchIndex,
                difficultyLevel = newTier.level,
                difficultyTitle = newTier.title,
                difficultyProgress = tierProgress,
                nextDifficultyScore = newTier.maxScoreExclusive,
                timeRemainingMs = updatedTimeMs,
                lastTimeBonusSeconds = bonusSeconds
            )
        }

        if (isGameOver) {
            stopTimeAttackTimer()
            if (isDaily) {
                preferences.recordDailyGame(currentState.dailyDate, newScore)
            }
            soundManager.playGameOver()
            triggerHaptic(HapticEffect.GameOver)
        }

        // Submit Daily Challenge score to global Firestore leaderboard on new record or completion
        if (isDaily && (isNewRecord || isGameOver) && newScore > 0 && firestoreRepository != null && currentUserId.isNotBlank()) {
            val dailyDate = currentState.dailyDate.ifEmpty { getTodayDateString() }
            viewModelScope.launch {
                firestoreRepository.submitDailyScore(
                    dateStr = dailyDate,
                    displayName = currentUserDisplayName,
                    score = newScore,
                    linesCleared = updatedLines,
                    maxCombo = updatedMaxCombo
                )
            }
        }

        // Increment All-Time Global Leaderboard score atomically in real time
        if (totalMovePoints > 0 && firestoreRepository != null && currentUserId.isNotBlank()) {
            val bestSingleGame = maxOf(newHigh, preferences.highScore)
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
            recordMissionEvent(MissionType.REACH_COMBO, newCombo)
        }
        recordMissionEvent(MissionType.SCORE_POINTS, totalMovePoints)

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
                selectedShapeIndex = if (willBeActive) null else it.selectedShapeIndex,
                comboBannerText = if (willBeActive) "HAMMER ACTIVE: TAP ANY FILLED BLOCK!" else null
            )
        }

        if (willBeActive) {
            soundManager.playPickup()
            triggerHaptic(HapticEffect.Pickup)
        }
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
            timeRemainingMs = currentState.timeRemainingMs,
            gameOverReason = currentState.gameOverReason
        )

        val newGrid = currentState.grid.map { it.toMutableList() }.toMutableList()
        newGrid[row][col] = 0
        val frozenGrid = newGrid.map { it.toList() }

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

        val stillGameOver = checkGameOver(frozenGrid, currentState.activePieces)

        _gameState.update {
            it.copy(
                grid = frozenGrid,
                hammerCount = newHammerCount,
                isHammerActive = false,
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
                    MissionType.REACH_COMBO -> maxOf(mission.current, amount)
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
        val newHammerCount = currentState.hammerCount + mission.hammerReward
        preferences.hammerCount = newHammerCount

        val updatedMissions = currentState.dailyMissions.map {
            if (it.id == missionId) it.copy(isClaimed = true) else it
        }
        val unclaimed = updatedMissions.count { it.isCompleted && !it.isClaimed }

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        _gameState.update {
            it.copy(
                hammerCount = newHammerCount,
                dailyMissions = updatedMissions,
                unclaimedMissionsCount = unclaimed,
                comboBannerText = "+${mission.hammerReward} 🔨 REWARD CLAIMED!"
            )
        }

        flushSession(syncToCloud = true)

        viewModelScope.launch {
            delay(1500)
            _gameState.update { it.copy(comboBannerText = null) }
        }

        return true
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.setMusicForegrounded(false)
        stopTimeAttackTimer()
        leaderboardJob?.cancel()
        globalLeaderboardJob?.cancel()
    }

    companion object {
        private const val TAG = "GameViewModel"
        const val TIME_ATTACK_INITIAL_MS = 60_000L
        const val TIME_ATTACK_MAX_MS = 99_000L
        const val TIME_ATTACK_BONUS_PER_LINE_MS = 4_000L
        const val TIME_ATTACK_COMBO_BONUS_MS = 1_000L
        const val TIME_ATTACK_SECOND_CHANCE_MS = 15_000L

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
