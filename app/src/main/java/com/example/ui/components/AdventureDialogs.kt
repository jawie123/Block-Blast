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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AdventureCatalog
import com.example.model.AdventureGoalType
import com.example.model.AdventureLevelSpec
import com.example.model.AdventureStarChest
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.GoldAccent

@Composable
fun AdventureLevelsDialog(
    currentLevelNumber: Int,
    maxUnlockedLevel: Int,
    getLevelStars: (Int) -> Int,
    onSelectLevel: (Int) -> Unit,
    onDismiss: () -> Unit,
    claimedStarChestsCount: Int = 0,
    onClaimStarChest: () -> Unit = {}
) {
    val totalStars = (1..AdventureCatalog.totalLevels).sumOf { getLevelStars(it) }
    val maxStars = AdventureCatalog.totalLevels * 3
    val nextChestTarget = AdventureStarChest.nextChestTarget(claimedStarChestsCount)
    val canClaimChest = AdventureStarChest.hasClaimableChest(totalStars, claimedStarChestsCount)
    val chapters = AdventureCatalog.allLevels.groupBy { it.chapter }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .heightIn(max = 640.dp)
                .testTag("adventure_levels_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BoardBackground),
            border = BorderStroke(1.5.dp, Color(0xFF00E676).copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🗺️ ADVENTURE MAP",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Break Ice ❄️ & Collect Gems 💎 across 20 stages",
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
                                .background(Color(0xFF282012))
                                .border(1.dp, GoldAccent.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "⭐ $totalStars / $maxStars",
                                fontSize = 12.sp,
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
                                .testTag("dismiss_adventure_map_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Adventure Map",
                                tint = Color(0xFFB4C0D8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Star Chest Milestone Reward Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (canClaimChest) {
                                Brush.horizontalGradient(listOf(Color(0xFF33260C), Color(0xFF281838)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF13192C), Color(0xFF111626)))
                            }
                        )
                        .border(
                            width = if (canClaimChest) 1.5.dp else 1.dp,
                            color = if (canClaimChest) GoldAccent else BoardBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "🎁", fontSize = 22.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (nextChestTarget != null) {
                                "STAR CHEST (${claimedStarChestsCount}/${AdventureStarChest.MAX_CHESTS}) • $totalStars/$nextChestTarget ⭐"
                            } else {
                                "ALL ${AdventureStarChest.MAX_CHESTS} STAR CHESTS CLAIMED!"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (canClaimChest) GoldAccent else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "+${AdventureStarChest.COINS_REWARD} 🪙  +${AdventureStarChest.HAMMER_REWARD} 🔨  +${AdventureStarChest.COLOR_BLAST_REWARD} 🎨",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9BA8C2)
                        )
                    }
                    Button(
                        onClick = onClaimStarChest,
                        enabled = canClaimChest,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("adventure_star_chest_button"),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            disabledContainerColor = Color(0xFF1F253B)
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = when {
                                canClaimChest -> "CLAIM!"
                                nextChestTarget == null -> "DONE"
                                else -> "LOCKED"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (canClaimChest) Color(0xFF0F111E) else Color(0xFF7D89A3),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    chapters.forEach { (chapterNum, levels) ->
                        val chapterTitle = levels.firstOrNull()?.chapterTitle ?: "Chapter $chapterNum"
                        val chapterAccent = when (chapterNum) {
                            1 -> Color(0xFF00E5FF)
                            2 -> Color(0xFF4FC3F7)
                            3 -> Color(0xFFE040FB)
                            else -> GoldAccent
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(chapterAccent.copy(alpha = 0.16f))
                                        .border(1.dp, chapterAccent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CHAPTER $chapterNum",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = chapterAccent
                                    )
                                }
                                Text(
                                    text = chapterTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (spec in levels) {
                                    val isUnlocked = spec.levelNumber <= maxUnlockedLevel
                                    val isCurrent = spec.levelNumber == currentLevelNumber
                                    val stars = getLevelStars(spec.levelNumber)
                                    AdventureLevelCard(
                                        spec = spec,
                                        isUnlocked = isUnlocked,
                                        isCurrent = isCurrent,
                                        stars = stars,
                                        chapterAccent = chapterAccent,
                                        onClick = {
                                            if (isUnlocked) {
                                                onSelectLevel(spec.levelNumber)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdventureLevelCard(
    spec: AdventureLevelSpec,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    chapterAccent: Color,
    onClick: () -> Unit
) {
    val goalSummary = when (spec.goalType) {
        AdventureGoalType.COLLECT_GEMS -> "💎 Collect ${spec.targetCount} Gems"
        AdventureGoalType.BREAK_ICE -> "❄️ Break ${spec.targetCount} Ice"
        AdventureGoalType.SCORE_TARGET -> "🎯 Score ${spec.targetCount}+"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isCurrent -> Color(0xFF14282C)
                    isUnlocked -> Color(0xFF101526)
                    else -> Color(0xFF0C0F1A)
                }
            )
            .border(
                width = if (isCurrent) 1.6.dp else 1.dp,
                color = when {
                    isCurrent -> Color(0xFF00E676)
                    isUnlocked -> chapterAccent.copy(alpha = 0.4f)
                    else -> BoardBorder.copy(alpha = 0.6f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = isUnlocked, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("adventure_level_card_${spec.levelNumber}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isUnlocked) chapterAccent.copy(alpha = 0.2f) else Color(0xFF191D2E)
                )
                .border(
                    1.dp,
                    if (isUnlocked) chapterAccent else BoardBorder,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                Text(
                    text = "${spec.levelNumber}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFF6B768C),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = spec.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isUnlocked) Color.White else Color(0xFF6B768C),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF00E676))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F111E)
                        )
                    }
                }
            }
            Text(
                text = "$goalSummary • +${spec.coinReward} 🪙",
                fontSize = 10.sp,
                color = if (isUnlocked) Color(0xFF9BA8C2) else Color(0xFF566075),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (s in 1..3) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (s <= stars) GoldAccent else Color(0xFF283047),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AdventureLevelCompleteDialog(
    levelSpec: AdventureLevelSpec,
    score: Int,
    earnedStars: Int,
    rewardCoins: Int,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onOpenMap: () -> Unit,
    onReplay: () -> Unit,
    onShareResult: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onReplay,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("adventure_level_complete_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BoardBackground),
            border = BorderStroke(2.dp, GoldAccent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF00E676), Color(0xFF00B0FF)))
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "CHAPTER ${levelSpec.chapter} • LEVEL ${levelSpec.levelNumber}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F111E),
                        letterSpacing = 0.6.sp
                    )
                }

                Text(
                    text = if (hasNextLevel) "STAGE CLEARED!" else "ADVENTURE CONQUERED!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = GoldAccent,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = levelSpec.title,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                // 3-Star Rating Display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (s in 1..3) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = if (s <= earnedStars) "Star earned" else "Star locked",
                            tint = if (s <= earnedStars) GoldAccent else Color(0xFF283047),
                            modifier = Modifier.size(if (s == 2) 44.dp else 36.dp)
                        )
                    }
                }

                // Score & Rewards Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF101424))
                        .border(1.dp, BoardBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "STAGE SCORE: $score",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    if (rewardCoins > 0) {
                        val extraBoosters = buildList {
                            if (levelSpec.hammerReward > 0) add("+${levelSpec.hammerReward} 🔨")
                            if (levelSpec.rotateReward > 0) add("+${levelSpec.rotateReward} ↻")
                        }.joinToString("  ")
                        Text(
                            text = if (extraBoosters.isNotEmpty()) {
                                "REWARD: +$rewardCoins 🪙   $extraBoosters"
                            } else {
                                "REWARD: +$rewardCoins 🪙"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00E676)
                        )
                    } else {
                        Text(
                            text = "STAGE CLEAR REWARD ALREADY CLAIMED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9BA8C2)
                        )
                    }
                }

                Button(
                    onClick = onNextLevel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("adventure_next_level_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                ) {
                    Text(
                        text = if (hasNextLevel) {
                            "NEXT LEVEL ${levelSpec.levelNumber + 1} →"
                        } else {
                            "PLAY LEVEL 1 AGAIN →"
                        },
                        color = Color(0xFF0F111E),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenMap,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("adventure_complete_map_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                    ) {
                        Text(
                            text = "LEVEL MAP",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("adventure_replay_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BoardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB4C0D8))
                    ) {
                        Text(
                            text = "REPLAY",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (onShareResult != null) {
                    OutlinedButton(
                        onClick = onShareResult,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("adventure_share_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.75f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent)
                    ) {
                        Text(
                            text = "📤 SHARE VICTORY",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
