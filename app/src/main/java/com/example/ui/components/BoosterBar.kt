package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BoardThemeCatalog
import com.example.model.BoardThemeSpec

/**
 * Dedicated horizontal power-up dock above the PieceDeck keeping ScoreHeader uncluttered
 * and giving one-tap access to Hammer, Hint, Piece Shuffle, 3x3 Area Bomb, and Coin Shop.
 */
@Composable
fun BoosterBar(
    hammerCount: Int,
    isHammerActive: Boolean,
    hintCount: Int,
    isHintActive: Boolean,
    shuffleCount: Int,
    bombCount: Int,
    isBombActive: Boolean,
    coinsBalance: Int,
    onToggleHammer: () -> Unit,
    onUseHint: () -> Unit,
    onShuffle: () -> Unit,
    onToggleBomb: () -> Unit,
    onOpenShop: () -> Unit,
    compactMode: Boolean = false,
    theme: BoardThemeSpec = BoardThemeCatalog.defaultTheme,
    modifier: Modifier = Modifier
) {
    val barHeight = if (compactMode) 40.dp else 48.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = if (compactMode) 1.dp else 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Hammer
        BoosterPill(
            emoji = "🔨",
            badgeText = if (isHammerActive) "ON" else "$hammerCount",
            isActive = isHammerActive,
            hasInventory = hammerCount > 0,
            activeGradient = listOf(Color(0xFFFFD54F), Color(0xFFFF9800)),
            accentColor = Color(0xFFFFD54F),
            contentDesc = "Hammer Smash Booster, $hammerCount remaining",
            testTag = "hammer_powerup_button",
            barHeight = barHeight,
            theme = theme,
            onClick = onToggleHammer,
            modifier = Modifier.weight(1f)
        )

        // 2. Smart Hint
        BoosterPill(
            emoji = "💡",
            badgeText = if (isHintActive) "GO" else "$hintCount",
            isActive = isHintActive,
            hasInventory = hintCount > 0,
            activeGradient = listOf(Color(0xFF00E5FF), Color(0xFF2979FF)),
            accentColor = Color(0xFF80D8FF),
            contentDesc = "Smart Placement Hint, $hintCount remaining",
            testTag = "hint_powerup_button",
            barHeight = barHeight,
            theme = theme,
            onClick = onUseHint,
            modifier = Modifier.weight(1f)
        )

        // 3. Piece Shuffle
        BoosterPill(
            emoji = "🔄",
            badgeText = "$shuffleCount",
            isActive = false,
            hasInventory = shuffleCount > 0,
            activeGradient = listOf(Color(0xFF00E676), Color(0xFF00BFA5)),
            accentColor = Color(0xFF69F0AE),
            contentDesc = "Piece Shuffle Booster, $shuffleCount remaining",
            testTag = "shuffle_powerup_button",
            barHeight = barHeight,
            theme = theme,
            onClick = onShuffle,
            modifier = Modifier.weight(1f)
        )

        // 4. 3x3 Area Bomb
        BoosterPill(
            emoji = "💣",
            badgeText = if (isBombActive) "ON" else "$bombCount",
            isActive = isBombActive,
            hasInventory = bombCount > 0,
            activeGradient = listOf(Color(0xFFFF5252), Color(0xFFFF6D00)),
            accentColor = Color(0xFFFF8A65),
            contentDesc = "3 by 3 Area Bomb Booster, $bombCount remaining",
            testTag = "bomb_powerup_button",
            barHeight = barHeight,
            theme = theme,
            onClick = onToggleBomb,
            modifier = Modifier.weight(1f)
        )

        // 5. Coin & Theme Shop
        Box(
            modifier = Modifier
                .weight(1.15f)
                .height(barHeight)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF2B2210), Color(0xFF231736))
                    )
                )
                .border(1.2.dp, Color(0xFFFFD700).copy(alpha = 0.72f), RoundedCornerShape(11.dp))
                .clickable(role = Role.Button, onClick = onOpenShop)
                .semantics { contentDescription = "Open Theme and Booster Shop, $coinsBalance coins" }
                .padding(horizontal = 6.dp)
                .testTag("shop_open_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "🛍️",
                    fontSize = 13.sp
                )
                Text(
                    text = "SHOP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun BoosterPill(
    emoji: String,
    badgeText: String,
    isActive: Boolean,
    hasInventory: Boolean,
    activeGradient: List<Color>,
    accentColor: Color,
    contentDesc: String,
    testTag: String,
    barHeight: androidx.compose.ui.unit.Dp,
    theme: BoardThemeSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(barHeight)
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (isActive) {
                    Brush.horizontalGradient(activeGradient)
                } else {
                    Brush.horizontalGradient(
                        listOf(theme.boardBackground, theme.screenBackground.copy(alpha = 0.92f))
                    )
                }
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = when {
                    isActive -> Color.White
                    hasInventory -> accentColor.copy(alpha = 0.65f)
                    else -> theme.boardBorder
                },
                shape = RoundedCornerShape(11.dp)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = contentDesc }
            .padding(horizontal = 4.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 13.sp
            )
            Text(
                text = badgeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = when {
                    isActive -> Color(0xFF0F111E)
                    hasInventory -> accentColor
                    else -> Color(0xFF7D89A3)
                },
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
