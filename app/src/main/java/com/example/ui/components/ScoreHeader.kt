package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import com.example.model.GameMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import com.example.model.BoardThemeCatalog
import com.example.model.BoardThemeSpec
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.GoldAccent

@Composable
fun ScoreHeader(
    score: Int,
    highScore: Int,
    comboStreak: Int,
    isSoundEnabled: Boolean = true,
    isHapticsEnabled: Boolean = true,
    canUndo: Boolean = false,
    isGuestMode: Boolean = false,
    gameMode: GameMode = GameMode.CLASSIC,
    dailyGlobalRecord: Int = 8540,
    dailyTargetScore: Int = 5000,
    dailyStreak: Int = 0,
    isDailyCompletedToday: Boolean = false,
    dailyPuzzleTitle: String = "",
    hammerCount: Int = 3,
    isHammerActive: Boolean = false,
    hintCount: Int = 2,
    isHintActive: Boolean = false,
    unclaimedMissions: Int = 0,
    coinsBalance: Int = 0,
    loginStreak: Int = 0,
    canClaimDailyReward: Boolean = false,
    difficultyLevel: Int = 1,
    difficultyTitle: String = "STARTER",
    difficultyProgress: Float = 0f,
    nextDifficultyScore: Int? = 500,
    unlockedShapeVariety: Int = 18,
    totalShapeVariety: Int = 58,
    timeRemainingMs: Long = 60_000L,
    timeMaxMs: Long = 99_000L,
    lastTimeBonusSeconds: Int = 0,
    onToggleSound: () -> Unit = {},
    onToggleHaptics: () -> Unit = {},
    onShowSettings: () -> Unit = {},
    onShowStats: () -> Unit,
    onShowTutorial: () -> Unit = {},
    onShowLeaderboard: () -> Unit = {},
    onShowDailyLeaderboard: () -> Unit = onShowLeaderboard,
    onSignOut: () -> Unit = {},
    onUndo: () -> Unit = {},
    onToggleHammer: () -> Unit = {},
    onUseHint: () -> Unit = {},
    onShowMissions: () -> Unit = {},
    onShowDailyRewards: () -> Unit = {},
    onOpenShop: () -> Unit = {},
    onSwitchMode: (GameMode) -> Unit = {},
    onRestart: () -> Unit,
    compactMode: Boolean = false,
    theme: BoardThemeSpec = BoardThemeCatalog.defaultTheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        // Top Toolbar: Brand & Quick Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title / Brand
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "8x8",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "BLOCK BLAST",
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isGuestMode) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x2600E5FF))
                            .border(0.8.dp, Color(0xFF00E5FF).copy(alpha = 0.65f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                            .testTag("guest_mode_badge")
                    ) {
                        Text(
                            text = "GUEST",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF),
                            letterSpacing = 0.4.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // Controls Toolbar (Compact 30dp buttons)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShowLeaderboard,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("leaderboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Global & Daily Leaderboards",
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onShowStats,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("stats_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Statistics",
                        tint = Color(0xFFB47CFF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onShowTutorial,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("help_tutorial_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "How to Play Tutorial",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("undo_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .then(
                                if (canUndo) {
                                    Modifier
                                        .clip(CircleShape)
                                        .background(Color(0x3300E5FF))
                                        .border(1.dp, Color(0xFF00E5FF), CircleShape)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo Last Move",
                            tint = if (canUndo) Color(0xFF00E5FF) else Color.Gray.copy(alpha = 0.35f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Game",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onShowSettings,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("settings_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0x2200E5FF))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Audio & Game Settings",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = if (isGuestMode) "Sign In / Switch Account" else "Sign Out",
                        tint = if (isGuestMode) Color(0xFF00E5FF) else Color(0xFFFF8A80),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Mode Switcher: Classic vs Time Attack vs Daily Challenge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF101424))
                .border(1.dp, BoardBorder, RoundedCornerShape(9.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val isClassic = gameMode == GameMode.CLASSIC
            // Classic Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(25.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (isClassic) {
                            Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF)))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable { onSwitchMode(GameMode.CLASSIC) }
                    .testTag("mode_tab_classic"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CLASSIC",
                    fontSize = 9.sp,
                    fontWeight = if (isClassic) FontWeight.Black else FontWeight.Bold,
                    color = if (isClassic) Color.White else Color(0xFF8E9BB0),
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Time Attack Tab
            val isTimeAttack = gameMode == GameMode.TIME_ATTACK
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(25.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (isTimeAttack) {
                            Brush.horizontalGradient(listOf(Color(0xFFFF1744), Color(0xFFFF6D00)))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable { onSwitchMode(GameMode.TIME_ATTACK) }
                    .testTag("mode_tab_time_attack"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (isTimeAttack) Color.White else Color(0xFFFF5252),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "TIME ATTACK",
                        fontSize = 9.sp,
                        fontWeight = if (isTimeAttack) FontWeight.Black else FontWeight.Bold,
                        color = if (isTimeAttack) Color.White else Color(0xFF8E9BB0),
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Daily Challenge Tab
            val isDaily = gameMode == GameMode.DAILY_CHALLENGE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(25.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (isDaily) {
                            Brush.horizontalGradient(listOf(Color(0xFFFF9800), Color(0xFFFFD600)))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable { onSwitchMode(GameMode.DAILY_CHALLENGE) }
                    .testTag("mode_tab_daily"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (isDaily) Color(0xFF0F111E) else Color(0xFFFF9800),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "DAILY",
                        fontSize = 9.sp,
                        fontWeight = if (isDaily) FontWeight.Black else FontWeight.Bold,
                        color = if (isDaily) Color(0xFF0F111E) else Color(0xFF8E9BB0),
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Time Attack Countdown HUD Banner
        if (gameMode == GameMode.TIME_ATTACK) {
            Spacer(modifier = Modifier.height(3.dp))
            val totalSeconds = ((timeRemainingMs + 999L) / 1000L).toInt().coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val formattedTime = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
            val isUrgent = timeRemainingMs in 1L..10_000L
            val isExpired = timeRemainingMs <= 0L
            val timerAccent = when {
                isExpired || isUrgent -> Color(0xFFFF5252)
                lastTimeBonusSeconds > 0 -> Color(0xFF00E676)
                else -> Color(0xFF00E5FF)
            }
            val progressFraction = if (timeRemainingMs <= 0L) {
                0f
            } else {
                (timeRemainingMs.toFloat() / 60_000f).coerceIn(0.02f, 1f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (isUrgent || isExpired) {
                                listOf(Color(0xFF2D0D16), Color(0xFF1F1220))
                            } else {
                                listOf(Color(0xFF0E1D2B), Color(0xFF15182E))
                            }
                        )
                    )
                    .border(1.dp, timerAccent.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("time_attack_timer_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Time Remaining",
                            tint = timerAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formattedTime,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = timerAccent,
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.testTag("time_attack_timer_text")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1A2138))
                    ) {
                        if (progressFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            if (isUrgent || isExpired) {
                                                listOf(Color(0xFFFF1744), Color(0xFFFF9100))
                                            } else {
                                                listOf(Color(0xFF00E5FF), Color(0xFF00E676))
                                            }
                                        )
                                    )
                            )
                        }
                    }

                    if (lastTimeBonusSeconds > 0) {
                        Text(
                            text = "+${lastTimeBonusSeconds}s!",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E676),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.testTag("time_attack_bonus_badge")
                        )
                    } else {
                        Text(
                            text = "+4s / LINE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Daily Challenge Info Banner (Compact clickable strip opening Daily Leaderboard)
        if (gameMode == GameMode.DAILY_CHALLENGE) {
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF261A08), Color(0xFF1B1428))
                        )
                    )
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFF9800))),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onShowDailyLeaderboard() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("daily_challenge_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Open Daily Leaderboard",
                            tint = GoldAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (dailyPuzzleTitle.isNotBlank()) {
                                "$dailyPuzzleTitle • #$dailyGlobalRecord"
                            } else {
                                "TOP: $dailyGlobalRecord"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (dailyStreak > 0) {
                            Text(
                                text = "🔥${dailyStreak}d",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFF9800),
                                maxLines = 1,
                                modifier = Modifier.testTag("daily_streak_badge")
                            )
                        }

                        Text(
                            text = if (isDailyCompletedToday) "✅ SOLVED" else "TARGET: $dailyTargetScore",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDailyCompletedToday) Color(0xFF00E676) else Color(0xFF00E5FF),
                            maxLines = 1,
                            modifier = Modifier.testTag("daily_target_status")
                        )
                    }

                    Text(
                        text = "🏆 RANKS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFB74D),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Row A: Clean Scores Banner (Current Score on left, Combo + Best High Score on right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Current Score Display
            Column {
                Text(
                    text = "SCORE",
                    fontSize = 8.sp,
                    color = Color(0xFF8E9BB0),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "$score",
                    fontSize = if (compactMode) 22.sp else 28.sp,
                    lineHeight = if (compactMode) 24.sp else 30.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.testTag("current_score_text")
                )
            }

            // Combo Streak & Best High Score (spacious, guaranteed single-line non-wrapping)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnimatedVisibility(
                    visible = comboStreak > 0,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF5252), Color(0xFFFF9800))
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("combo_streak_pill"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔥 x$comboStreak",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Best High Score Pill
                Box(
                    modifier = Modifier
                        .widthIn(min = 76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.boardBackground)
                        .border(1.dp, theme.boardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("high_score_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "High Score",
                            tint = GoldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Column {
                            Text(
                                text = when (gameMode) {
                                    GameMode.DAILY_CHALLENGE -> "RECORD"
                                    GameMode.TIME_ATTACK -> "TIME BEST"
                                    else -> "BEST"
                                },
                                fontSize = 8.sp,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "$highScore",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Row B: Equal-width Quick Strip for Coin Shop, Daily Login Gift, and Daily Missions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coins Wallet / Shop Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF282012), Color(0xFF1A182E))
                        )
                    )
                    .border(1.dp, GoldAccent.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .clickable { onOpenShop() }
                    .padding(horizontal = 6.dp)
                    .testTag("header_shop_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🪙 $coinsBalance • SHOP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldAccent,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Daily Login Rewards Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (canClaimDailyReward) {
                                listOf(Color(0xFF33260B), Color(0xFF241B10))
                            } else {
                                listOf(Color(0xFF1E2235), Color(0xFF161928))
                            }
                        )
                    )
                    .border(
                        width = if (canClaimDailyReward) 1.5.dp else 1.dp,
                        color = if (canClaimDailyReward) GoldAccent else theme.boardBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onShowDailyRewards() }
                    .padding(horizontal = 6.dp)
                    .testTag("daily_login_reward_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "🎁",
                        fontSize = 11.sp
                    )
                    Text(
                        text = when {
                            canClaimDailyReward -> "CLAIM GIFT"
                            loginStreak > 0 -> "DAY ${loginStreak}"
                            else -> "DAILY GIFT"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (canClaimDailyReward) GoldAccent else Color(0xFFFFE082),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Daily Missions Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1E2235), Color(0xFF161928))
                        )
                    )
                    .border(
                        width = if (unclaimedMissions > 0) 1.5.dp else 1.dp,
                        color = if (unclaimedMissions > 0) Color(0xFFFF5252) else theme.boardBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onShowMissions() }
                    .padding(horizontal = 6.dp)
                    .testTag("daily_missions_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🎯",
                        fontSize = 11.sp
                    )
                    Text(
                        text = "GOALS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (unclaimedMissions > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFFF1744))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$unclaimedMissions",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Progressive Difficulty Tier Strip
        val tierAccent = when (difficultyLevel) {
            1 -> Color(0xFF00E5FF)
            2 -> Color(0xFF00E676)
            3 -> Color(0xFFFFB300)
            4 -> Color(0xFFFF4081)
            else -> GoldAccent
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF101424))
                .border(1.dp, tierAccent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(tierAccent.copy(alpha = 0.16f))
                    .border(0.8.dp, tierAccent.copy(alpha = 0.7f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "LV $difficultyLevel $difficultyTitle",
                    fontSize = 9.sp,
                    color = tierAccent,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.testTag("difficulty_badge")
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1A2138))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(difficultyProgress.coerceIn(0.04f, 1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(tierAccent.copy(alpha = 0.75f), tierAccent)
                            )
                        )
                )
            }

            Text(
                text = if (nextDifficultyScore != null) "NEXT $nextDifficultyScore" else "MAX TIER",
                fontSize = 8.sp,
                color = Color(0xFF8E9BB0),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                maxLines = 1
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF1A2138))
                    .border(0.6.dp, tierAccent.copy(alpha = 0.45f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                    .semantics {
                        contentDescription = "$unlockedShapeVariety of $totalShapeVariety block shapes unlocked"
                    }
                    .testTag("difficulty_shape_variety_badge")
            ) {
                Text(
                    text = "${unlockedShapeVariety}/${totalShapeVariety}",
                    fontSize = 8.sp,
                    color = tierAccent,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }

            // Quick Gameplay Toggles: Sound Effects & Haptic Feedback
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val sfxAccent = Color(0xFF00E5FF)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSoundEnabled) sfxAccent.copy(alpha = 0.16f) else Color(0xFF1A2036)
                        )
                        .border(
                            0.8.dp,
                            if (isSoundEnabled) sfxAccent.copy(alpha = 0.65f) else BoardBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable(role = Role.Switch) { onToggleSound() }
                        .semantics {
                            contentDescription = if (isSoundEnabled) {
                                "Sound Effects On, tap to mute"
                            } else {
                                "Sound Effects Off, tap to unmute"
                            }
                        }
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                        .testTag("quick_sfx_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        contentDescription = null,
                        tint = if (isSoundEnabled) sfxAccent else Color(0xFF6B768C),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "SFX",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSoundEnabled) sfxAccent else Color(0xFF6B768C),
                        maxLines = 1
                    )
                }

                val hapticsAccent = Color(0xFFFF9800)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isHapticsEnabled) hapticsAccent.copy(alpha = 0.16f) else Color(0xFF1A2036)
                        )
                        .border(
                            0.8.dp,
                            if (isHapticsEnabled) hapticsAccent.copy(alpha = 0.65f) else BoardBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable(role = Role.Switch) { onToggleHaptics() }
                        .semantics {
                            contentDescription = if (isHapticsEnabled) {
                                "Haptic Feedback On, tap to disable"
                            } else {
                                "Haptic Feedback Off, tap to enable"
                            }
                        }
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                        .testTag("quick_haptics_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = if (isHapticsEnabled) hapticsAccent else Color(0xFF6B768C),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "HAPTIC",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isHapticsEnabled) hapticsAccent else Color(0xFF6B768C),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Compact Combo Streak Meter saving vertical space for the 8x8 board
        ComboStreakMeter(
            comboStreak = comboStreak,
            compact = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
