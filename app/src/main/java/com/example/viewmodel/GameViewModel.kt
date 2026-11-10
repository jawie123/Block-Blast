package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.BlockShape
import com.example.model.GameState
import com.example.model.ScorePopup
import com.example.model.ShapeDefinitions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    private val context: Context,
    val soundManager: SoundManager,
    private val preferences: GamePreferences
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    init {
        soundManager.isSoundEnabled = preferences.isSoundEnabled
        soundManager.isHapticsEnabled = preferences.isHapticsEnabled
        startNewGame()
    }

    fun startNewGame() {
        val initialShapes = ShapeDefinitions.getRandomShapeBatch()
        val high = preferences.highScore
        preferences.gamesPlayed = preferences.gamesPlayed + 1

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
            comboBannerText = null,
            scorePopups = emptyList()
        )
    }

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

            // Collect clearing coordinates
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

            // Clear the cells
            for (coord in clearingCoords) {
                newGrid[coord.first][coord.second] = 0
            }

            if (newCombo >= 2) {
                soundManager.playComboCheer()
            } else {
                soundManager.playLineClear(newCombo)
            }
        }

        val totalMovePoints = basePoints + linePoints
        val newScore = currentState.score + totalMovePoints
        var newHigh = maxOf(newScore, currentState.highScore)
        val isNewRecord = newScore > preferences.highScore

        if (isNewRecord) {
            preferences.highScore = newScore
        }
        if (newCombo > preferences.highestCombo) {
            preferences.highestCombo = newCombo
        }
        preferences.totalLinesCleared = preferences.totalLinesCleared + totalLines

        // Update pieces pool
        val updatedPieces = currentState.activePieces.toMutableList()
        updatedPieces[shapeIndex] = null

        val finalPieces = if (updatedPieces.all { it == null }) {
            ShapeDefinitions.getRandomShapeBatch()
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

        val popup = ScorePopup(
            text = "+$totalMovePoints",
            row = targetRow.coerceIn(0, 7),
            col = targetCol.coerceIn(0, 7),
            isCombo = newCombo > 1
        )

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
                comboBannerText = bannerText,
                scorePopups = listOf(popup)
            )
        }

        if (isGameOver) {
            soundManager.playGameOver()
        }

        // Reset clearing animation and popup after brief duration
        viewModelScope.launch {
            delay(400)
            _gameState.update { it.copy(clearingCells = emptySet()) }
            delay(1200)
            _gameState.update { it.copy(comboBannerText = null, scorePopups = emptyList()) }
        }

        return true
    }

    private fun checkGameOver(grid: List<List<Int>>, remainingPieces: List<BlockShape?>): Boolean {
        val nonNullPieces = remainingPieces.filterNotNull()
        if (nonNullPieces.isEmpty()) return false

        // Check if ANY remaining piece can fit ANYWHERE
        for (piece in nonNullPieces) {
            if (canFitAnywhere(grid, piece)) {
                return false
            }
        }
        return true
    }

    fun useSecondChance() {
        val state = _gameState.value
        if (!state.secondChanceAvailable || !state.isGameOver) return

        soundManager.playPowerup()

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
