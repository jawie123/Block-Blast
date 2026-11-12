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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
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
    isSoundEnabled: Boolean,
    isHapticsEnabled: Boolean,
    canUndo: Boolean = false,
    gameMode: GameMode = GameMode.CLASSIC,
    dailyGlobalRecord: Int = 8540,
    dailyTargetScore: Int = 5000,
    dailyStreak: Int = 0,
    hammerCount: Int = 3,
    isHammerActive: Boolean = false,
    unclaimedMissions: Int = 0,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onShowStats: () -> Unit,
    onShowTutorial: () -> Unit = {},
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
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }

            // Controls Toolbar (Compact 30dp buttons)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("toggle_sound_button")
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = if (isSoundEnabled) "Mute sound" else "Enable sound",
                        tint = if (isSoundEnabled) Color(0xFF00E5FF) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onToggleHaptics,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("toggle_haptics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = if (isHapticsEnabled) "Disable vibration" else "Enable vibration",
                        tint = if (isHapticsEnabled) Color(0xFFFF9800) else Color.Gray,
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
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Mode Switcher: Classic vs Daily Challenge
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
                    fontSize = 10.sp,
                    fontWeight = if (isClassic) FontWeight.Black else FontWeight.Bold,
                    color = if (isClassic) Color.White else Color(0xFF8E9BB0),
                    letterSpacing = 0.5.sp
                )
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
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "DAILY CHALLENGE",
                        fontSize = 10.sp,
                        fontWeight = if (isDaily) FontWeight.Black else FontWeight.Bold,
                        color = if (isDaily) Color(0xFF0F111E) else Color(0xFF8E9BB0),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Daily Challenge Info Banner (Compact inline strip)
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
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "RECORD: $dailyGlobalRecord",
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
                        text = "🔥 ${dailyStreak}d STREAK",
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
                                text = if (gameMode == GameMode.DAILY_CHALLENGE) "RECORD" else "BEST",
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

        // Visual Combo Streak Meter displaying multiplier and consecutive clear progress
        ComboStreakMeter(
            comboStreak = comboStreak,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
