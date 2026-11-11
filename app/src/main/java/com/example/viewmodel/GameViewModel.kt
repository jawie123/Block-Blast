package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.HapticEffect
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.BlockShape
import com.example.model.GameMode
import com.example.model.GameState
import com.example.model.LineClearBurst
import com.example.model.ScorePopup
import com.example.model.ShapeDefinitions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * GameViewModel manages the 8x8 grid state (2D matrix/array), score tracking,
 * active piece pool, placement validation (canPlaceShape), and game over detection (checkGameOver).
 */
class GameViewModel(
    private val context: Context,
    val soundManager: SoundManager,
    private val preferences: GamePreferences
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
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
        soundManager.isSoundEnabled = preferences.isSoundEnabled
        soundManager.isHapticsEnabled = preferences.isHapticsEnabled
        startNewGame()
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
        val dailyBatchIndex: Int
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
     */
    fun startNewGame(mode: GameMode = _gameState.value.gameMode) {
        lastUndoSnapshot = null
        val isDaily = mode == GameMode.DAILY_CHALLENGE
        val dateStr = getTodayDateString()
        val dateFormatted = getTodayFormattedString()
        val seed = computeDailySeed()

        val initialShapes = if (isDaily) {
            ShapeDefinitions.getDailyShapeBatch(seed, 0)
        } else {
            ShapeDefinitions.getRandomShapeBatch()
        }

        val high = if (isDaily) {
            preferences.getDailyHighScore(dateStr)
        } else {
            preferences.highScore
        }

        if (!isDaily) {
            preferences.gamesPlayed = preferences.gamesPlayed + 1
        }

        val targetScore = 4500 + ((seed % 1000).toInt())
        val globalRecord = 8400 + ((seed % 1600).toInt())

        _gameState.value = GameState(
            grid = List(8) { List(8) { 0 } },
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
            comboBannerText = if (isDaily) "DAILY CHALLENGE: $dateFormatted" else null,
            scorePopups = emptyList(),
            gameMode = mode,
            dailyDate = dateStr,
            dailyFormattedDate = dateFormatted,
            dailyBatchIndex = 0,
            dailyTargetScore = targetScore,
            dailyGlobalRecord = globalRecord,
            canUndo = false
        )
    }

    fun switchGameMode(mode: GameMode) {
        if (_gameState.value.gameMode == mode) return
        startNewGame(mode)
    }

    /**
     * Reverts the last piece placement, restoring the previous grid state, score,
     * piece pool, and combo streak.
     */
    fun undoLastMove(): Boolean {
        val snapshot = lastUndoSnapshot ?: return false
        lastUndoSnapshot = null

        _gameState.update {
            it.copy(
                grid = snapshot.grid,
                activePieces = snapshot.activePieces,
                score = snapshot.score,
                highScore = snapshot.highScore,
                comboStreak = snapshot.comboStreak,
                maxComboSession = snapshot.maxComboSession,
                linesClearedSession = snapshot.linesClearedSession,
                isGameOver = snapshot.isGameOver,
                isNewHighScore = snapshot.isNewHighScore,
                dailyBatchIndex = snapshot.dailyBatchIndex,
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
                canUndo = false
            )
        }

        soundManager.playPickup()
        triggerHaptic(HapticEffect.Pickup)
        return true
    }

    fun getDailyStreak(): Int = preferences.dailyStreak

    fun toggleSound(): Boolean {
        val newVal = !soundManager.isSoundEnabled
        soundManager.isSoundEnabled = newVal
        preferences.isSoundEnabled = newVal
        return newVal
    }

    fun toggleHaptics(): Boolean {
        val newVal = !soundManager.isHapticsEnabled
        soundManager.isHapticsEnabled = newVal
        preferences.isHapticsEnabled = newVal
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

    /**
     * Validates whether [shape] can be placed at ([startRow], [startCol])
     * on the current active 8x8 grid.
     */
    fun canPlaceShape(shape: BlockShape, startRow: Int, startCol: Int): Boolean {
        return canPlaceShape(_gameState.value.grid, shape, startRow, startCol)
    }

    /**
     * Validates whether [shape] can be placed at ([startRow], [startCol])
     * on the provided 2D list grid.
     */
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

    /**
     * Overload for canPlaceShape using a 2D Array (Array<IntArray>).
     */
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

    /**
     * Checks if [shape] can fit anywhere across the 64 positions on the grid.
     */
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

    /**
     * Evaluates if any of the remaining shapes can fit anywhere on the 8x8 board.
     * Returns true if no valid move exists for any remaining piece.
     */
    fun checkGameOver(): Boolean {
        return checkGameOver(_gameState.value.grid, _gameState.value.activePieces)
    }

    /**
     * Evaluates if any piece in [remainingPieces] can fit anywhere on [grid].
     */
    fun checkGameOver(grid: List<List<Int>>, remainingPieces: List<BlockShape?>): Boolean {
        val nonNullPieces = remainingPieces.filterNotNull()
        if (nonNullPieces.isEmpty()) return false

        // Check if ANY remaining piece can fit ANYWHERE on the 8x8 grid
        for (piece in nonNullPieces) {
            if (canFitAnywhere(grid, piece)) {
                return false
            }
        }
        return true
    }

    /**
     * 2D Array overload for checkGameOver.
     */
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

            // Calculate prospective rows and cols that would clear
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

        // Save undo snapshot before applying the move
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
            dailyBatchIndex = currentState.dailyBatchIndex
        )

        // Place cells onto grid
        val newGrid = currentState.grid.map { it.toMutableList() }.toMutableList()
        val placedCells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until shape.height) {
            for (c in 0 until shape.width) {
                if (shape.matrix[r][c]) {
                    newGrid[targetRow + r][targetCol + c] = shape.colorId
                    placedCells.add(Pair(targetRow + r, targetCol + c))
                }
            }
        }

        // Base points
        val basePoints = shape.cellCount * 10
        soundManager.playPlace()

        // Check for completed rows and columns
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

            // Multi-line bonus
            val bonus = when (totalLines) {
                2 -> 100
                3 -> 300
                4 -> 600
                else -> if (totalLines > 4) 1000 else 0
            }
            linePoints += bonus

            // Collect clearing coordinates and generate particle bursts with block colors
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

            // Clear the cells
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
        var newHigh = maxOf(newScore, currentState.highScore)
        val isDaily = currentState.gameMode == GameMode.DAILY_CHALLENGE
        val isNewRecord = if (isDaily) {
            newScore > preferences.getDailyHighScore(currentState.dailyDate)
        } else {
            newScore > preferences.highScore
        }

        if (isNewRecord) {
            if (isDaily) {
                preferences.setDailyHighScore(currentState.dailyDate, newScore)
            } else {
                preferences.highScore = newScore
            }
        }
        if (newCombo > preferences.highestCombo) {
            preferences.highestCombo = newCombo
        }
        preferences.totalLinesCleared = preferences.totalLinesCleared + totalLines

        // Update pieces pool
        val updatedPieces = currentState.activePieces.toMutableList()
        updatedPieces[shapeIndex] = null

        var nextBatchIndex = currentState.dailyBatchIndex
        val finalPieces = if (updatedPieces.all { it == null }) {
            if (isDaily) {
                nextBatchIndex++
                ShapeDefinitions.getDailyShapeBatch(computeDailySeed(), nextBatchIndex)
            } else {
                ShapeDefinitions.getRandomShapeBatch()
            }
        } else {
            updatedPieces
        }

        // Popup and combo text
        val bannerText = when {
            totalLines >= 3 -> "MEGA BLAST! +$totalMovePoints"
            totalLines == 2 -> "DOUBLE LINE! +$totalMovePoints"
            newCombo >= 2 -> "COMBO x$newCombo! +$totalMovePoints"
            totalLines == 1 -> "LINE CLEAR! +$totalMovePoints"
            else -> null
        }

        // Floating text popups at coordinates of the clear
        val popups = mutableListOf<ScorePopup>()

        if (totalLines > 0) {
            val ptsPerLine = linePoints / totalLines

            // If rows and columns clear together, spawn at their intersection coordinates
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
                // Completed rows: center is (row, 3.5f)
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

                // Completed columns: center is (3.5f, col)
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
            // Standard placement points popup at placement position
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

        _gameState.update {
            it.copy(
                grid = frozenGrid,
                activePieces = finalPieces,
                score = newScore,
                highScore = newHigh,
                comboStreak = if (totalLines > 0) newCombo else 0,
                maxComboSession = maxOf(currentState.maxComboSession, newCombo),
                linesClearedSession = currentState.linesClearedSession + totalLines,
                isGameOver = isGameOver,
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
                dailyBatchIndex = nextBatchIndex
            )
        }

        if (isGameOver) {
            if (isDaily) {
                preferences.recordDailyGame(currentState.dailyDate, newScore)
            }
            soundManager.playGameOver()
            triggerHaptic(HapticEffect.GameOver)
        }

        // Reset clearing animation and popup after brief duration
        viewModelScope.launch {
            delay(400)
            _gameState.update { it.copy(clearingCells = emptySet()) }
            delay(400)
            _gameState.update { it.copy(lineClearBursts = emptyList()) }
            delay(600)
            _gameState.update { it.copy(comboBannerText = null, scorePopups = emptyList()) }
        }

        return true
    }

    fun useSecondChance() {
        val state = _gameState.value
        if (!state.secondChanceAvailable || !state.isGameOver) return

        soundManager.playPowerup()
        triggerHaptic(HapticEffect.PowerUp)

        // Clear center 3x3 (rows 2..4, cols 2..4) and any 1 crowded row/col to rescue player
        val newGrid = state.grid.map { it.toMutableList() }.toMutableList()
        for (r in 2..5) {
            for (c in 2..5) {
                newGrid[r][c] = 0
            }
        }

        val updatedGrid = newGrid.map { it.toList() }
        val stillGameOver = checkGameOver(updatedGrid, state.activePieces)

        // If center 3x3 wasn't enough for large pieces, also clear bottom two rows
        val finalGrid = if (stillGameOver) {
            val rescued = updatedGrid.map { it.toMutableList() }.toMutableList()
            for (c in 0 until 8) {
                rescued[6][c] = 0
                rescued[7][c] = 0
            }
            rescued.map { it.toList() }
        } else {
            updatedGrid
        }

        _gameState.update {
            it.copy(
                grid = finalGrid,
                isGameOver = false,
                secondChanceAvailable = false,
                comboBannerText = "SECOND CHANCE! BLAST REVIVE!"
            )
        }

        viewModelScope.launch {
            delay(1800)
            _gameState.update { it.copy(comboBannerText = null) }
        }
    }

    companion object {
        fun provideFactory(
            context: Context,
            soundManager: SoundManager,
            preferences: GamePreferences
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameViewModel(context, soundManager, preferences) as T
            }
        }
    }
}
