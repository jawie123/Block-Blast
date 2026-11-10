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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
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
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onShowStats: () -> Unit,
    onShowTutorial: () -> Unit = {},
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Top Toolbar: Sound, Haptics, Stats, Restart
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title / Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "BLOCK BLAST",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("toggle_sound_button")
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = if (isSoundEnabled) "Mute sound" else "Enable sound",
                        tint = if (isSoundEnabled) Color(0xFF00E5FF) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToggleHaptics,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("toggle_haptics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = if (isHapticsEnabled) "Disable vibration" else "Enable vibration",
                        tint = if (isHapticsEnabled) Color(0xFFFF9800) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onShowStats,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("stats_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Statistics",
                        tint = Color(0xFFB47CFF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onShowTutorial,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("help_tutorial_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "How to Play Tutorial",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Game",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scores Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Current Score Display
            Column {
                Text(
                    text = "SCORE",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF8E9BB0),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.testTag("current_score_text")
                )
            }

            // High Score & Combo Streak
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Combo Streak Pill
                AnimatedVisibility(
                    visible = comboStreak > 0,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF5252), Color(0xFFFF9800))
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("combo_streak_pill"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔥 x$comboStreak COMBO",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Best High Score Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(BoardBackground)
                        .border(1.dp, BoardBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("high_score_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "High Score",
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "BEST",
                                fontSize = 10.sp,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$highScore",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Visual Combo Streak Meter displaying multiplier and consecutive clear progress
        ComboStreakMeter(
            comboStreak = comboStreak,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
