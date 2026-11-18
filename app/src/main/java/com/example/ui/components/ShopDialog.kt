package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BoardThemeCatalog
import com.example.model.BoardThemeId
import com.example.model.BoardThemeSpec
import com.example.model.ShopBoosterType
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.GoldAccent

enum class ShopTab {
    THEMES,
    BOOSTERS
}

@Composable
fun ShopDialog(
    coinsBalance: Int,
    selectedThemeId: String,
    unlockedThemeIds: Set<String>,
    hammerCount: Int,
    hintCount: Int,
    shuffleCount: Int,
    bombCount: Int,
    isRewardedAdReady: Boolean = false,
    isRewardedAdLoading: Boolean = false,
    isAdShowing: Boolean = false,
    onSelectOrUnlockTheme: (BoardThemeId) -> Unit,
    onBuyBooster: (ShopBoosterType) -> Unit,
    onWatchAdForBooster: ((ShopBoosterType) -> Unit)? = null,
    onDismiss: () -> Unit,
    initialTab: ShopTab = ShopTab.THEMES
) {
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .heightIn(max = 640.dp)
                .testTag("shop_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BoardBackground),
            border = BorderStroke(1.5.dp, BoardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🛍️ COIN SHOP",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Unlock board themes & stock up boosters",
                            fontSize = 11.sp,
                            color = Color(0xFF9BA8C2)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2B2210))
                                .border(1.dp, GoldAccent.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("shop_coins_balance")
                        ) {
                            Text(
                                text = "🪙 $coinsBalance",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("dismiss_shop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Shop",
                                tint = Color(0xFFB4C0D8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F1220))
                        .border(1.dp, BoardBorder, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShopTabPill(
                        label = "🎨 BOARD THEMES",
                        selected = activeTab == ShopTab.THEMES,
                        testTag = "shop_tab_themes",
                        onClick = { activeTab = ShopTab.THEMES },
                        modifier = Modifier.weight(1f)
                    )
                    ShopTabPill(
                        label = "⚡ BOOSTERS",
                        selected = activeTab == ShopTab.BOOSTERS,
                        testTag = "shop_tab_boosters",
                        onClick = { activeTab = ShopTab.BOOSTERS },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeTab == ShopTab.THEMES) {
                        for (theme in BoardThemeCatalog.allThemes) {
                            val isUnlocked = theme.id == BoardThemeId.NEON_ARCADE ||
                                unlockedThemeIds.contains(theme.id.name)
                            val isEquipped = selectedThemeId == theme.id.name
                            val canAfford = coinsBalance >= theme.costCoins

                            ThemeShopCard(
                                theme = theme,
                                isUnlocked = isUnlocked,
                                isEquipped = isEquipped,
                                canAfford = canAfford,
                                onAction = { onSelectOrUnlockTheme(theme.id) }
                            )
                        }
                    } else {
                        for (booster in ShopBoosterType.entries) {
                            val owned = when (booster) {
                                ShopBoosterType.HAMMER -> hammerCount
                                ShopBoosterType.HINT -> hintCount
                                ShopBoosterType.SHUFFLE -> shuffleCount
                                ShopBoosterType.BOMB -> bombCount
                            }
                            BoosterShopCard(
                                booster = booster,
                                ownedCount = owned,
                                canAfford = coinsBalance >= booster.coinCost,
                                showWatchAd = onWatchAdForBooster != null && booster != ShopBoosterType.HINT,
                                isRewardedAdReady = isRewardedAdReady,
                                isRewardedAdLoading = isRewardedAdLoading,
                                isAdShowing = isAdShowing,
                                onBuyWithCoins = { onBuyBooster(booster) },
                                onWatchAd = { onWatchAdForBooster?.invoke(booster) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopTabPill(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) {
                    Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF)))
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (selected) Color.White else Color(0xFF8E9BB0),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ThemeShopCard(
    theme: BoardThemeSpec,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101322))
            .border(
                width = if (isEquipped) 1.8.dp else 1.dp,
                color = if (isEquipped) theme.accentColor else BoardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
            .testTag("theme_card_${theme.id.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 2x2 Mini Board Color Swatch Preview
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(theme.boardBackground)
                .border(1.5.dp, theme.boardBorder, RoundedCornerShape(10.dp))
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(theme.paletteFor(1).primary)
                    )
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(theme.paletteFor(3).primary)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(theme.paletteFor(5).primary)
                    )
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(theme.cellEmpty)
                            .border(0.8.dp, theme.cellEmptyBorder, RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(text = theme.badgeEmoji, fontSize = 14.sp)
                Text(
                    text = theme.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = theme.subtitle,
                fontSize = 11.sp,
                color = Color(0xFF9BA8C2),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Button(
            onClick = onAction,
            enabled = !isEquipped && (isUnlocked || canAfford),
            modifier = Modifier
                .height(40.dp)
                .testTag("theme_action_button_${theme.id.name}"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    isEquipped -> Color(0xFF1F3830)
                    isUnlocked -> theme.accentColor
                    canAfford -> GoldAccent
                    else -> Color(0xFF23283D)
                },
                disabledContainerColor = if (isEquipped) Color(0xFF1A3A2E) else Color(0xFF1D2234)
            )
        ) {
            if (isEquipped) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E676),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else if (isUnlocked) {
                Text(
                    text = "EQUIP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F111E),
                    maxLines = 1,
                    softWrap = false
                )
            } else {
                Text(
                    text = "🪙 ${theme.costCoins}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (canAfford) Color(0xFF0F111E) else Color(0xFF8E9BB0),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun BoosterShopCard(
    booster: ShopBoosterType,
    ownedCount: Int,
    canAfford: Boolean,
    showWatchAd: Boolean,
    isRewardedAdReady: Boolean,
    isRewardedAdLoading: Boolean,
    isAdShowing: Boolean,
    onBuyWithCoins: () -> Unit,
    onWatchAd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101322))
            .border(1.dp, BoardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag("booster_shop_card_${booster.name}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF191F36))
                    .border(1.dp, BoardBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = booster.emoji, fontSize = 22.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = booster.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1C2642))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OWNED: $ownedCount",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF80D8FF),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Text(
                    text = booster.description,
                    fontSize = 11.sp,
                    color = Color(0xFF9BA8C2)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onBuyWithCoins,
                enabled = canAfford,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("shop_buy_booster_${booster.name}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    disabledContainerColor = Color(0xFF1D2234)
                )
            ) {
                Text(
                    text = "BUY +1 • 🪙 ${booster.coinCost}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (canAfford) Color(0xFF0F111E) else Color(0xFF7D89A3),
                    maxLines = 1,
                    softWrap = false
                )
            }

            if (showWatchAd) {
                OutlinedButton(
                    onClick = onWatchAd,
                    enabled = !isAdShowing,
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("shop_watch_ad_${booster.name}"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (!isRewardedAdReady && isRewardedAdLoading) "LOADING" else "FREE +1",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
