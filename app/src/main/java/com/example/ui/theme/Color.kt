package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Background and Surface Colors (Deep arcade space)
val DarkBackground = Color(0xFF0F111E)
val BoardBackground = Color(0xFF16192B)
val BoardBorder = Color(0xFF252B46)
val CellEmpty = Color(0xFF1B2036)
val CellEmptyBorder = Color(0xFF282E4D)

// Accents
val GoldAccent = Color(0xFFFFD700)
val GoldGlow = Color(0xFFFFA000)
val GhostPreviewColor = Color(0x66FFFFFF)
val RowHighlightColor = Color(0x33FFEB3B)

// Block Jewel Colors with Primary, Gradient, Border and Shadow
data class BlockPalette(
    val primary: Color,
    val light: Color,
    val dark: Color,
    val glow: Color
)

val JewelPalettes = mapOf(
    1 to BlockPalette(
        primary = Color(0xFFFF5252),
        light = Color(0xFFFF867C),
        dark = Color(0xFFC50E29),
        glow = Color(0x80FF5252)
    ), // Ruby Red
    2 to BlockPalette(
        primary = Color(0xFFFF9800),
        light = Color(0xFFFFB74D),
        dark = Color(0xFFE65100),
        glow = Color(0x80FF9800)
    ), // Sunset Orange
    3 to BlockPalette(
        primary = Color(0xFFFFD600),
        light = Color(0xFFFFFF52),
        dark = Color(0xFFF57F17),
        glow = Color(0x80FFD600)
    ), // Golden Amber
    4 to BlockPalette(
        primary = Color(0xFF00E676),
        light = Color(0xFF69F0AE),
        dark = Color(0xFF00A152),
        glow = Color(0x8000E676)
    ), // Emerald Green
    5 to BlockPalette(
        primary = Color(0xFF00B0FF),
        light = Color(0xFF80D8FF),
        dark = Color(0xFF0081CB),
        glow = Color(0x8000B0FF)
    ), // Electric Cyan
    6 to BlockPalette(
        primary = Color(0xFF7C4DFF),
        light = Color(0xFFB47CFF),
        dark = Color(0xFF3F1DCB),
        glow = Color(0x807C4DFF)
    ), // Royal Purple
    7 to BlockPalette(
        primary = Color(0xFFFF4081),
        light = Color(0xFFFF80AB),
        dark = Color(0xFFC60055),
        glow = Color(0x80FF4081)
    ), // Hot Pink
    8 to BlockPalette(
        primary = Color(0xFF00E5FF),
        light = Color(0xFF84FFFF),
        dark = Color(0xFF00B2CC),
        glow = Color(0x8000E5FF)
    )  // Aqua Ice
)

fun getBlockPalette(colorId: Int): BlockPalette {
    return JewelPalettes[colorId] ?: JewelPalettes[1]!!
}
