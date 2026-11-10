package com.example.model

data class ScorePopup(
    val id: Long = System.nanoTime() + (0..9999).random(),
    val text: String,
    val row: Float,
    val col: Float,
    val isCombo: Boolean = false,
    val comboMultiplier: Int = 1
)

data class GameState(
    val grid: List<List<Int>> = List(8) { List(8) { 0 } },
    val activePieces: List<BlockShape?> = emptyList(),
    val score: Int = 0,
    val highScore: Int = 0,
    val comboStreak: Int = 0,
    val maxComboSession: Int = 0,
    val linesClearedSession: Int = 0,
    val isGameOver: Boolean = false,
    val isNewHighScore: Boolean = false,
    val secondChanceAvailable: Boolean = true,
    val selectedShapeIndex: Int? = null,
    val hoverRow: Int? = null,
    val hoverCol: Int? = null,
    val hoverPreviewCells: Set<Pair<Int, Int>> = emptySet(),
    val prospectiveRows: Set<Int> = emptySet(),
    val prospectiveCols: Set<Int> = emptySet(),
    val clearingCells: Set<Pair<Int, Int>> = emptySet(),
    val comboBannerText: String? = null,
    val scorePopups: List<ScorePopup> = emptyList()
)
