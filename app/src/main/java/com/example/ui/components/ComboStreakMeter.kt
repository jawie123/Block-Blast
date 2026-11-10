package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder

// Tier colors for the 5 combo streak levels:
// 1x: Electric Cyan, 2x: Vivid Purple, 3x: Neon Pink/Magenta, 4x: Flame Amber, 5x+: Sunburst Gold
private val TierGradients = listOf(
    listOf(Color(0xFF00E5FF), Color(0xFF00B0FF)),       // 1x
    listOf(Color(0xFF9D46FF), Color(0xFF651FFF)),       // 2x
    listOf(Color(0xFFFF4081), Color(0xFFC51162)),       // 3x
    listOf(Color(0xFFFF9100), Color(0xFFFF3D00)),       // 4x
    listOf(Color(0xFFFFD700), Color(0xFFFF6D00))        // 5x+
)

private val TierPrimaryColors = listOf(
    Color(0xFF00E5FF),
    Color(0xFFB388FF),
    Color(0xFFFF4081),
    Color(0xFFFF9100),
    Color(0xFFFFD700)
)

private val TierLabels = listOf("1x", "2x", "3x", "4x", "5x+")

/**
 * Visual Combo Streak Meter displaying consecutive line clear progression
 * and the active score multiplier.
 */
@Composable
fun ComboStreakMeter(
    comboStreak: Int,
    modifier: Modifier = Modifier
) {
    val tierIndex = when {
        comboStreak <= 1 -> 0
        comboStreak == 2 -> 1
        comboStreak == 3 -> 2
        comboStreak == 4 -> 3
        else -> 4
    }

    val activeMultiplier = if (comboStreak >= 1) comboStreak else 1
    val isStreakActive = comboStreak > 0

    // Pulsing animations when combo streak is active
    val infiniteTransition = rememberInfiniteTransition(label = "combo_streak_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isStreakActive) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "multiplier_pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isStreakActive) 1f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val activeGradient = TierGradients[tierIndex]
    val activeColor = TierPrimaryColors[tierIndex]

    // Outer card border color animation
    val targetBorderColor = if (isStreakActive) {
        activeColor.copy(alpha = glowAlpha * 0.7f)
    } else {
        BoardBorder
    }
    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 300),
        label = "card_border_color"
    )

    Box(
        modifier = modifier
            .testTag("combo_streak_meter")
            .clip(RoundedCornerShape(12.dp))
            .background(BoardBackground)
            .border(1.dp, animatedBorderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Status title and Active Multiplier Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Icon + Label + Subtitle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isStreakActive) Icons.Default.LocalFireDepartment else Icons.Default.Bolt,
                        contentDescription = "Combo Streak Icon",
                        tint = if (isStreakActive) activeColor else Color(0xFF6B7894),
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = if (isStreakActive) "COMBO STREAK" else "COMBO METER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isStreakActive) Color.White else Color(0xFF8E9BB0),
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = when {
                                comboStreak == 0 -> "Clear lines to build multiplier"
                                comboStreak == 1 -> "1 Line Clear • Streak Active!"
                                comboStreak in 2..4 -> "${comboStreak}x Line Streak! 🔥"
                                else -> "ULTRA STREAK x$comboStreak 👑"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isStreakActive) activeColor else Color(0xFF5E6B85)
                        )
                    }
                }

                // Right: Multiplier Badge
                Box(
                    modifier = Modifier
                        .scale(if (isStreakActive) pulseScale else 1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isStreakActive) {
                                Brush.horizontalGradient(activeGradient)
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF141A2E), Color(0xFF182038))
                                )
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isStreakActive) Color.White.copy(alpha = 0.6f) else BoardBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("multiplier_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (isStreakActive) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = if (isStreakActive) {
                                "${activeMultiplier}x MULTIPLIER"
                            } else {
                                "1x BASE"
                            },
                            color = if (isStreakActive) Color.White else Color(0xFF8E9BB0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }

            // 5 Segmented Progress Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 5) {
                    val isFilled = comboStreak > i
                    val isCurrentPeak = comboStreak == i + 1 || (i == 4 && comboStreak >= 5)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isFilled) {
                                    Brush.horizontalGradient(TierGradients[i])
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF131728), Color(0xFF192035))
                                    )
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    isCurrentPeak -> Color.White.copy(alpha = glowAlpha)
                                    isFilled -> TierPrimaryColors[i].copy(alpha = 0.5f)
                                    else -> Color(0xFF222944)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) {
                        // Specular shine for filled segments
                        if (isFilled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }

            // Labels below segments showing the 5 multiplier levels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 5) {
                    val isReached = comboStreak > i
                    val isCurrentPeak = comboStreak == i + 1 || (i == 4 && comboStreak >= 5)

                    Text(
                        text = TierLabels[i],
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 9.sp,
                        fontWeight = if (isCurrentPeak) FontWeight.Black else FontWeight.Bold,
                        color = when {
                            isCurrentPeak -> TierPrimaryColors[i]
                            isReached -> TierPrimaryColors[i].copy(alpha = 0.85f)
                            else -> Color(0xFF5E6B85)
                        }
                    )
                }
            }
        }
    }
}
