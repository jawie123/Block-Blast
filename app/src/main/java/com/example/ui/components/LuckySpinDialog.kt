package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.LuckySpinCatalog
import com.example.model.LuckySpinResult
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.GoldAccent
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun LuckySpinDialog(
    canClaimFreeSpin: Boolean,
    bonusSpinCount: Int,
    lastSpinReward: LuckySpinResult?,
    isRewardedAdReady: Boolean,
    isRewardedAdLoading: Boolean,
    isAdShowing: Boolean,
    onSpinWheel: () -> LuckySpinResult?,
    onWatchAdForSpin: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val slices = LuckySpinCatalog.slices
    val wheelRotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isSpinningAnim by remember { mutableStateOf(false) }

    val availableSpins = (if (canClaimFreeSpin) 1 else 0) + bonusSpinCount

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("lucky_spin_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BoardBackground),
            border = BorderStroke(1.5.dp, Color(0xFFE040FB).copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎡 LUCKY SPIN WHEEL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = when {
                                canClaimFreeSpin -> "1 Free Daily Spin Ready!"
                                bonusSpinCount > 0 -> "$bonusSpinCount Bonus Spin${if (bonusSpinCount > 1) "s" else ""} Ready!"
                                else -> "Come back tomorrow or watch an ad!"
                            },
                            fontSize = 11.sp,
                            color = if (availableSpins > 0) Color(0xFF00E676) else Color(0xFF9AA7C0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("lucky_spin_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Lucky Spin",
                            tint = Color.White
                        )
                    }
                }

                // Wheel + Top Pointer
                Box(
                    modifier = Modifier.size(216.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(204.dp)
                            .graphicsLayer { rotationZ = wheelRotation.value },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val sweep = 360f / slices.size
                            val diameter = size.minDimension
                            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                            val wheelSize = Size(diameter, diameter)

                            slices.forEachIndexed { idx, slice ->
                                val startAngle = -90f - (sweep / 2f) + (idx * sweep)
                                drawArc(
                                    color = Color(slice.accentHex).copy(alpha = 0.28f),
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = true,
                                    topLeft = topLeft,
                                    size = wheelSize
                                )
                                drawArc(
                                    color = Color(slice.accentHex),
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = true,
                                    topLeft = topLeft,
                                    size = wheelSize,
                                    style = Stroke(width = 2.5f)
                                )
                            }

                            drawCircle(
                                color = Color(0xFFFFD700),
                                radius = diameter / 2f,
                                style = Stroke(width = 5f)
                            )
                        }

                        // Slice emoji + compact amount labels around the wheel
                        slices.forEachIndexed { idx, slice ->
                            val angleDeg = idx * (360f / slices.size)
                            val rad = Math.toRadians((angleDeg - 90f).toDouble())
                            val radiusDp = 68.dp
                            val dx = (cos(rad) * radiusDp.value).dp
                            val dy = (sin(rad) * radiusDp.value).dp
                            Column(
                                modifier = Modifier.offset(x = dx, y = dy),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = slice.emoji, fontSize = 14.sp)
                                Text(
                                    text = if (slice.amount >= 25) "${slice.amount}" else "+${slice.amount}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Center hub
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(listOf(GoldAccent, Color(0xFFFF6D00)))
                                )
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "★",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Fixed top pointer arrow
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(width = 22.dp, height = 16.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        }
                        drawPath(path = path, color = Color(0xFFFFD700))
                        drawPath(path = path, color = Color.White, style = Stroke(width = 2f))
                    }
                }

                // Reward reveal banner
                AnimatedVisibility(visible = lastSpinReward != null) {
                    lastSpinReward?.let { reward ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(reward.slice.accentHex).copy(alpha = 0.28f),
                                            Color(0xFF1F233A)
                                        )
                                    )
                                )
                                .border(
                                    1.2.dp,
                                    Color(reward.slice.accentHex),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("lucky_spin_result_banner"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎉 WON ${reward.slice.emoji} ${reward.slice.label}!",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Primary Spin Action Button
                Button(
                    onClick = {
                        if (isSpinningAnim || availableSpins <= 0) return@Button
                        val result = onSpinWheel() ?: return@Button
                        val targetSliceAngle = -(result.slice.index * (360f / slices.size))
                        val currentMod = ((wheelRotation.value % 360f) + 360f) % 360f
                        val normalizedTarget = ((targetSliceAngle % 360f) + 360f) % 360f
                        val delta = ((normalizedTarget - currentMod + 360f) % 360f) + 720f
                        isSpinningAnim = true
                        scope.launch {
                            wheelRotation.animateTo(
                                targetValue = wheelRotation.value + delta,
                                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
                            )
                            isSpinningAnim = false
                        }
                    },
                    enabled = availableSpins > 0 && !isSpinningAnim,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("lucky_spin_trigger_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE040FB),
                        disabledContainerColor = Color(0xFF282D44)
                    )
                ) {
                    Text(
                        text = when {
                            isSpinningAnim -> "SPINNING..."
                            canClaimFreeSpin -> "🎡 SPIN FREE TODAY!"
                            bonusSpinCount > 0 -> "🎡 SPIN BONUS ($bonusSpinCount LEFT)"
                            else -> "NO SPINS LEFT TODAY"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                if (onWatchAdForSpin != null) {
                    OutlinedButton(
                        onClick = onWatchAdForSpin,
                        enabled = !isAdShowing && !isSpinningAnim,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("lucky_spin_ad_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, GoldAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (!isRewardedAdReady && isRewardedAdLoading) {
                                    "LOADING AD..."
                                } else {
                                    "WATCH AD • +1 BONUS SPIN"
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
