package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BlockPalette
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.CellEmpty
import com.example.ui.theme.CellEmptyBorder
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.JewelPalettes

enum class BoardThemeId {
    NEON_ARCADE,
    COZY_WOOD,
    CANDY_CRYSTAL,
    MIDNIGHT_OLED
}

enum class ShopBoosterType(
    val title: String,
    val emoji: String,
    val description: String,
    val coinCost: Int
) {
    HAMMER(
        title = "Hammer",
        emoji = "🔨",
        description = "Smash any single block on the board",
        coinCost = 50
    ),
    HINT(
        title = "Smart Hint",
        emoji = "💡",
        description = "Highlight the highest-scoring placement",
        coinCost = 35
    ),
    SHUFFLE(
        title = "Piece Shuffle",
        emoji = "🔄",
        description = "Reroll your tray for 3 fresh pieces",
        coinCost = 40
    ),
    BOMB(
        title = "3×3 Area Bomb",
        emoji = "💣",
        description = "Blast a 3×3 zone clear on the board",
        coinCost = 75
    )
}

@Immutable
data class BoardThemeSpec(
    val id: BoardThemeId,
    val name: String,
    val subtitle: String,
    val badgeEmoji: String,
    val costCoins: Int,
    val screenBackground: Color,
    val boardBackground: Color,
    val boardBorder: Color,
    val cellEmpty: Color,
    val cellEmptyBorder: Color,
    val accentColor: Color,
    val isWoodStyle: Boolean = false,
    val palettes: Map<Int, BlockPalette>
) {
    fun paletteFor(colorId: Int): BlockPalette =
        palettes[colorId] ?: palettes[1] ?: JewelPalettes[1]!!
}

object BoardThemeCatalog {

    val defaultTheme = BoardThemeSpec(
        id = BoardThemeId.NEON_ARCADE,
        name = "Neon Cyber",
        subtitle = "Electric arcade jewel glow",
        badgeEmoji = "⚡",
        costCoins = 0,
        screenBackground = DarkBackground,
        boardBackground = BoardBackground,
        boardBorder = BoardBorder,
        cellEmpty = CellEmpty,
        cellEmptyBorder = CellEmptyBorder,
        accentColor = Color(0xFF00E5FF),
        isWoodStyle = false,
        palettes = JewelPalettes
    )

    private val cozyWoodTheme = BoardThemeSpec(
        id = BoardThemeId.COZY_WOOD,
        name = "Cozy Wood",
        subtitle = "Warm handcrafted walnut & oak tiles",
        badgeEmoji = "🪵",
        costCoins = 120,
        screenBackground = Color(0xFF1A120D),
        boardBackground = Color(0xFF281C14),
        boardBorder = Color(0xFF5D4037),
        cellEmpty = Color(0xFF332319),
        cellEmptyBorder = Color(0xFF4A3426),
        accentColor = Color(0xFFFFB74D),
        isWoodStyle = true,
        palettes = mapOf(
            1 to BlockPalette(Color(0xFFC65D3B), Color(0xFFE07A57), Color(0xFF8C3B20), Color(0x80C65D3B)),
            2 to BlockPalette(Color(0xFFD98E3A), Color(0xFFF0AD5E), Color(0xFF9C5E1A), Color(0x80D98E3A)),
            3 to BlockPalette(Color(0xFFE0B44C), Color(0xFFF4CF73), Color(0xFFA67F21), Color(0x80E0B44C)),
            4 to BlockPalette(Color(0xFF6B8E4E), Color(0xFF8BAE6C), Color(0xFF476331), Color(0x806B8E4E)),
            5 to BlockPalette(Color(0xFF4E808E), Color(0xFF6FA3B1), Color(0xFF2F5661), Color(0x804E808E)),
            6 to BlockPalette(Color(0xFF8C5B7A), Color(0xFFAB7B9A), Color(0xFF5E3850), Color(0x808C5B7A)),
            7 to BlockPalette(Color(0xFFB84B5B), Color(0xFFD66C7C), Color(0xFF7D2A37), Color(0x80B84B5B)),
            8 to BlockPalette(Color(0xFF5DA389), Color(0xFF7EC2A9), Color(0xFF3A705C), Color(0x805DA389))
        )
    )

    private val candyCrystalTheme = BoardThemeSpec(
        id = BoardThemeId.CANDY_CRYSTAL,
        name = "Candy Crystal",
        subtitle = "Vibrant prism & sugar-gem blocks",
        badgeEmoji = "💎",
        costCoins = 180,
        screenBackground = Color(0xFF170D24),
        boardBackground = Color(0xFF231538),
        boardBorder = Color(0xFF4A2B73),
        cellEmpty = Color(0xFF2C1B46),
        cellEmptyBorder = Color(0xFF402A63),
        accentColor = Color(0xFFFF66C4),
        isWoodStyle = false,
        palettes = mapOf(
            1 to BlockPalette(Color(0xFFFF5E99), Color(0xFFFF94BA), Color(0xFFC2185B), Color(0x80FF5E99)),
            2 to BlockPalette(Color(0xFFFF9E40), Color(0xFFFFBE7B), Color(0xFFD86B00), Color(0x80FF9E40)),
            3 to BlockPalette(Color(0xFFFFE047), Color(0xFFFFEE85), Color(0xFFCC9F00), Color(0x80FFE047)),
            4 to BlockPalette(Color(0xFF2FE6A6), Color(0xFF7AF5CA), Color(0xFF009F6B), Color(0x802FE6A6)),
            5 to BlockPalette(Color(0xFF3FC5FF), Color(0xFF8CE0FF), Color(0xFF0088CC), Color(0x803FC5FF)),
            6 to BlockPalette(Color(0xFFA76BFF), Color(0xFFC9A3FF), Color(0xFF6A29CC), Color(0x80A76BFF)),
            7 to BlockPalette(Color(0xFFFF4D6D), Color(0xFFFF859B), Color(0xFFB81232), Color(0x80FF4D6D)),
            8 to BlockPalette(Color(0xFF47FFE8), Color(0xFF99FFF2), Color(0xFF00B39E), Color(0x8047FFE8))
        )
    )

    private val midnightOledTheme = BoardThemeSpec(
        id = BoardThemeId.MIDNIGHT_OLED,
        name = "Obsidian Gold",
        subtitle = "True OLED black & champagne gold",
        badgeEmoji = "👑",
        costCoins = 250,
        screenBackground = Color(0xFF050608),
        boardBackground = Color(0xFF0D0F16),
        boardBorder = Color(0xFF8C7328),
        cellEmpty = Color(0xFF131620),
        cellEmptyBorder = Color(0xFF25293A),
        accentColor = Color(0xFFFFD700),
        isWoodStyle = false,
        palettes = mapOf(
            1 to BlockPalette(Color(0xFFE53935), Color(0xFFFF6F60), Color(0xFF9A0007), Color(0x80E53935)),
            2 to BlockPalette(Color(0xFFFFB300), Color(0xFFFFE54C), Color(0xFFC68400), Color(0x80FFB300)),
            3 to BlockPalette(Color(0xFFD4AF37), Color(0xFFFFE082), Color(0xFF8C6D14), Color(0x80D4AF37)),
            4 to BlockPalette(Color(0xFF00C853), Color(0xFF5EFC82), Color(0xFF009624), Color(0x8000C853)),
            5 to BlockPalette(Color(0xFF2979FF), Color(0xFF75A7FF), Color(0xFF004ECB), Color(0x802979FF)),
            6 to BlockPalette(Color(0xFFAA00FF), Color(0xFFE254FF), Color(0xFF7200CA), Color(0x80AA00FF)),
            7 to BlockPalette(Color(0xFFF50057), Color(0xFFFF5983), Color(0xFFBB002F), Color(0x80F50057)),
            8 to BlockPalette(Color(0xFF00B8D4), Color(0xFF62EBFF), Color(0xFF0088A3), Color(0x8000B8D4))
        )
    )

    val allThemes: List<BoardThemeSpec> = listOf(
        defaultTheme,
        cozyWoodTheme,
        candyCrystalTheme,
        midnightOledTheme
    )

    fun byId(themeId: String?): BoardThemeSpec {
        val parsed = runCatching {
            if (themeId.isNullOrBlank()) BoardThemeId.NEON_ARCADE else BoardThemeId.valueOf(themeId)
        }.getOrDefault(BoardThemeId.NEON_ARCADE)
        return allThemes.firstOrNull { it.id == parsed } ?: defaultTheme
    }
}
