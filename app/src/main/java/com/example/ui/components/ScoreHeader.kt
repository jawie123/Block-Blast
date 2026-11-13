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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    gameMode: GameMode = GameMode.CLASSIC,
    dailyGlobalRecord: Int = 8540,
    dailyTargetScore: Int = 5000,
    dailyStreak: Int = 0,
    dailyPuzzleTitle: String = "",
    hammerCount: Int = 3,
    isHammerActive: Boolean = false,
    unclaimedMissions: Int = 0,
    difficultyLevel: Int = 1,
    difficultyTitle: String = "STARTER",
    difficultyProgress: Float = 0f,
    nextDifficultyScore: Int? = 500,
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
    onShowMissions: () -> Unit = {},
    onSwitchMode: (GameMode) -> Unit = {},
    onRestart: () -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title / Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                )
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
                        .then(
                            if (canUndo) {
                                Modifier
                                    .background(Color(0x3300E5FF), CircleShape)
                                    .border(1.dp, Color(0xFF00E5FF), CircleShape)
                            } else Modifier
                        )
                        .testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo Last Move",
                        tint = if (canUndo) Color(0xFF00E5FF) else Color.Gray.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
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
                        .background(Color(0x2200E5FF), CircleShape)
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f), CircleShape)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Audio & Game Settings",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = Color(0xFFFF8A80),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }

                    Text(
                        text = "TARGET: $dailyTargetScore",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00E5FF)
                    )

                    Text(
                        text = "🏆 RANKS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFB74D)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Scores Banner: Score, Combo & High Score in a clean, tight row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Current Score Display
            Column {
                Text(
                    text = "SCORE",
                    fontSize = 9.sp,
                    color = Color(0xFF8E9BB0),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "$score",
                    fontSize = 30.sp,
                    lineHeight = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.testTag("current_score_text")
                )
            }

            // Power-ups & Missions Quick Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Hammer Power-up Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isHammerActive) {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFD54F), Color(0xFFFF9800))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1E2235), Color(0xFF161928))
                                )
                            }
                        )
                        .border(
                            width = if (isHammerActive) 1.5.dp else 1.dp,
                            color = if (isHammerActive) Color(0xFFFFD700) else if (hammerCount > 0) Color(0xFFFFB300).copy(alpha = 0.6f) else BoardBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onToggleHammer() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("hammer_powerup_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🔨",
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isHammerActive) "ACTIVE" else "$hammerCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isHammerActive) Color(0xFF0F111E) else if (hammerCount > 0) Color(0xFFFFD54F) else Color.Gray
                        )
                    }
                }

                // Daily Missions Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1E2235), Color(0xFF161928))
                            )
                        )
                        .border(
                            width = if (unclaimedMissions > 0) 1.5.dp else 1.dp,
                            color = if (unclaimedMissions > 0) Color(0xFFFF5252) else BoardBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onShowMissions() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // High Score & Combo Streak
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Combo Streak Pill
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
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Best High Score Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BoardBackground)
                        .border(1.dp, BoardBorder, RoundedCornerShape(12.dp))
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
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$highScore",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
                letterSpacing = 0.3.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Visual Combo Streak Meter displaying multiplier and consecutive clear progress
        ComboStreakMeter(
            comboStreak = comboStreak,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
