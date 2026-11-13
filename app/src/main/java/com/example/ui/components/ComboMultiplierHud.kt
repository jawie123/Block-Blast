package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Real-time floating HUD component that pops up with spring physics and a radial shockwave
 * whenever the player clears lines, displaying the active combo multiplier and streak tier.
 */
@Composable
fun ComboMultiplierHud(
    comboStreak: Int,
    comboEventId: Long,
    lastClearedLinesCount: Int = 0,
    prospectiveLinesCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val isPreviewOnly = comboStreak == 0 && prospectiveLinesCount > 0
    val displayMultiplier = when {
        comboStreak > 0 && prospectiveLinesCount > 0 -> comboStreak + 1
        comboStreak > 0 -> comboStreak
        prospectiveLinesCount > 0 -> 1
        else -> 0
    }
    val isVisible = displayMultiplier > 0

    // Tier styling aligned with ComboStreakMeter
    val tierGradient = when {
        displayMultiplier >= 5 -> listOf(Color(0xFFFFD700), Color(0xFFFF6D00))
        displayMultiplier == 4 -> listOf(Color(0xFFFF9100), Color(0xFFFF3D00))
        displayMultiplier == 3 -> listOf(Color(0xFFFF007A), Color(0xFFC51162))
        displayMultiplier == 2 -> listOf(Color(0xFF9D46FF), Color(0xFF651FFF))
        else -> listOf(Color(0xFF00E5FF), Color(0xFF00B0FF))
    }

    val tierAccent = when {
        displayMultiplier >= 5 -> Color(0xFFFFD700)
        displayMultiplier == 4 -> Color(0xFFFF9100)
        displayMultiplier == 3 -> Color(0xFFFF4081)
        displayMultiplier == 2 -> Color(0xFFB388FF)
        else -> Color(0xFF00E5FF)
    }

    val tierTitle = when {
        isPreviewOnly -> "READY CLEAR"
        prospectiveLinesCount > 0 -> "NEXT COMBO"
        displayMultiplier >= 5 -> "GODLIKE STREAK"
        displayMultiplier == 4 -> "ULTRA BLAZE"
        displayMultiplier == 3 -> "MEGA COMBO"
        displayMultiplier == 2 -> "DOUBLE STREAK"
        else -> "COMBO ACTIVE"
    }

    // Pop-up spring overshoot animation triggered on every new line clear event or streak jump
    val popScale = remember { Animatable(1f) }
    val popOffsetY = remember { Animatable(0f) }
    val shockwaveProgress = remember { Animatable(1f) }

    LaunchedEffect(comboEventId, comboStreak) {
        if (comboStreak > 0) {
            launch {
                popScale.snapTo(0.45f)
                popScale.animateTo(
                    targetValue = 1.24f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                popScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                popOffsetY.snapTo(16f)
                popOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                shockwaveProgress.snapTo(0f)
                shockwaveProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    // Ambient breathing glow while streak is live
    val infiniteTransition = rememberInfiniteTransition(label = "combo_hud_glow")
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "combo_hud_ambient_pulse"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            initialScale = 0.4f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(tween(150)),
        exit = scaleOut(targetScale = 0.6f, animationSpec = tween(180)) + fadeOut(tween(180)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val combinedScale = popScale.value * if (comboStreak > 0) ambientPulse else 1f
                    scaleX = combinedScale
                    scaleY = combinedScale
                    translationY = popOffsetY.value
                }
                .drawBehind {
                    if (shockwaveProgress.value < 0.99f) {
                        val ringRadius = (size.maxDimension * 0.45f) + (shockwaveProgress.value * size.maxDimension * 0.45f)
                        val ringAlpha = (1f - shockwaveProgress.value).coerceIn(0f, 1f) * 0.75f
                        drawCircle(
                            color = tierAccent.copy(alpha = ringAlpha),
                            radius = ringRadius,
                            style = Stroke(width = (6f * (1f - shockwaveProgress.value)).coerceAtLeast(1.5f))
                        )
                    }
                }
                .shadow(
                    elevation = if (comboStreak >= 2) 18.dp else 12.dp,
                    shape = RoundedCornerShape(999.dp),
                    spotColor = tierAccent
                )
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xEE11162B),
                            Color(0xEE1A1F38)
                        )
                    )
                )
                .border(
                    width = 1.75.dp,
                    brush = Brush.horizontalGradient(tierGradient),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("combo_multiplier_hud"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tier Icon Pill
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(tierGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isPreviewOnly -> Icons.Default.AutoAwesome
                            displayMultiplier >= 3 -> Icons.Default.LocalFireDepartment
                            else -> Icons.Default.Bolt
                        },
                        contentDescription = "Combo Multiplier Icon",
                        tint = if (displayMultiplier >= 5) Color(0xFF121629) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Animated Real-Time Multiplier Readout
                AnimatedContent(
                    targetState = displayMultiplier,
                    transitionSpec = {
                        (scaleIn(
                            initialScale = 0.5f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        ) + slideInVertically { -it / 2 } + fadeIn()) togetherWith
                            (scaleOut(targetScale = 1.3f) + slideOutVertically { it / 2 } + fadeOut())
                    },
                    label = "combo_multiplier_value"
                ) { mult ->
                    Text(
                        text = "${mult}x",
                        color = tierAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.testTag("combo_hud_multiplier_text")
                    )
                }

                // Status Title + Line Clear Pill
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = tierTitle,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.7.sp
                    )
                    val subLabel = when {
                        prospectiveLinesCount > 0 -> "+$prospectiveLinesCount LINE${if (prospectiveLinesCount > 1) "S" else ""} ON DROP"
                        lastClearedLinesCount > 0 -> "+$lastClearedLinesCount LINE${if (lastClearedLinesCount > 1) "S" else ""} CLEARED!"
                        else -> "STREAK MULTIPLIER"
                    }
                    Text(
                        text = subLabel,
                        color = tierAccent.copy(alpha = 0.9f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
