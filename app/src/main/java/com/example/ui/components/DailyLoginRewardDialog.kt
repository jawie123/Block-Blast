package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DailyLoginCalendarDay
import com.example.model.DailyLoginDayStatus
import com.example.model.DailyLoginRewardSchedule
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.GoldAccent

@Composable
fun DailyLoginRewardDialog(
    calendarDays: List<DailyLoginCalendarDay>,
    loginStreak: Int,
    longestLoginStreak: Int,
    coinsBalance: Int,
    hammerCount: Int,
    canClaimToday: Boolean,
    nextCycleDay: Int,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    isGuestMode: Boolean = false,
    onClaimToday: () -> Unit,
    onExchangeCoinsForHammer: () -> Unit,
    onSignInWithGoogle: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "daily_reward_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "claim_pulse_scale"
    )

    val exchangeCost = DailyLoginRewardSchedule.COIN_TO_HAMMER_COST
    val canAffordHammer = coinsBalance >= exchangeCost

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("daily_login_reward_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BoardBackground),
            border = BorderStroke(1.5.dp, Color(0xFF2E395E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(GoldAccent, Color(0xFFFF6D00))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🎁", fontSize = 17.sp)
                        }
                        Column {
                            Text(
                                text = "DAILY REWARDS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.4.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!isGuestMode) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Synced to Cloud",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = if (isGuestMode) "Local Wallet • Sign in to Sync" else "Streak Synced to Firestore",
                                    fontSize = 10.sp,
                                    color = if (isGuestMode) Color(0xFFFFB74D) else Color(0xFF00E676),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_daily_login_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Daily Rewards",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Streak & Wallet Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Streak Box
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF14192A))
                            .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("login_streak_summary")
                    ) {
                        Text(
                            text = "🔥 ${loginStreak}d STREAK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFB74D),
                            maxLines = 1
                        )
                        Text(
                            text = "Best: ${longestLoginStreak}d",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8E9BB0),
                            maxLines = 1
                        )
                    }

                    // Coins Wallet
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF14192A))
                            .border(1.dp, GoldAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("coins_balance_badge"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🪙 $coinsBalance",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldAccent,
                            maxLines = 1
                        )
                        Text(
                            text = "COINS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E9BB0)
                        )
                    }

                    // Hammers Wallet
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF14192A))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("reward_hammer_balance_badge"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔨 $hammerCount",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF),
                            maxLines = 1
                        )
                        Text(
                            text = "HAMMERS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E9BB0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 7-Day Reward Calendar (Days 1-3, Days 4-6, Day 7 Jackpot)
                val firstRow = calendarDays.take(3)
                val secondRow = calendarDays.drop(3).take(3)
                val jackpotDay = calendarDays.getOrNull(6)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    firstRow.forEach { day ->
                        DayRewardTile(
                            day = day,
                            pulseScale = if (day.status == DailyLoginDayStatus.CLAIMABLE) pulseScale else 1f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    secondRow.forEach { day ->
                        DayRewardTile(
                            day = day,
                            pulseScale = if (day.status == DailyLoginDayStatus.CLAIMABLE) pulseScale else 1f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (jackpotDay != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    JackpotDayTile(
                        day = jackpotDay,
                        pulseScale = if (jackpotDay.status == DailyLoginDayStatus.CLAIMABLE) pulseScale else 1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 11.sp,
                        color = Color(0xFFFF8A80),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("daily_login_error_text")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Claim Reward CTA
                Button(
                    onClick = onClaimToday,
                    enabled = canClaimToday && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .then(if (canClaimToday && !isLoading) Modifier.scale(pulseScale) else Modifier)
                        .testTag("claim_daily_login_reward_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canClaimToday) GoldAccent else Color(0xFF1D243B),
                        disabledContainerColor = Color(0xFF1D243B),
                        contentColor = Color(0xFF0F111E),
                        disabledContentColor = Color(0xFF00E676)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = GoldAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (canClaimToday) {
                                "🎁 CLAIM DAY $nextCycleDay REWARD"
                            } else {
                                "✓ CLAIMED TODAY • COME BACK TOMORROW"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Coin-to-Hammer Craft Exchange
                OutlinedButton(
                    onClick = onExchangeCoinsForHammer,
                    enabled = canAffordHammer && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("exchange_coins_hammer_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (canAffordHammer) Color(0xFF00E5FF) else BoardBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (canAffordHammer) Color(0x1A00E5FF) else Color(0xFF121626),
                        contentColor = if (canAffordHammer) Color(0xFF00E5FF) else Color(0xFF6B7794)
                    )
                ) {
                    Text(
                        text = "🔨 CRAFT +1 HAMMER ($exchangeCost 🪙)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }

                if (isGuestMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF12182B))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("login_reward_guest_sync_note"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Back up your streak across devices",
                            fontSize = 10.sp,
                            color = Color(0xFFB4C0D8),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Button(
                            onClick = onSignInWithGoogle,
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("login_reward_guest_sign_in_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF0F111E)
                            )
                        ) {
                            Text(
                                text = "SIGN IN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRewardTile(
    day: DailyLoginCalendarDay,
    pulseScale: Float,
    modifier: Modifier = Modifier
) {
    val tier = day.tier
    val isClaimed = day.status == DailyLoginDayStatus.CLAIMED
    val isClaimable = day.status == DailyLoginDayStatus.CLAIMABLE

    val borderColor = when (day.status) {
        DailyLoginDayStatus.CLAIMABLE -> GoldAccent
        DailyLoginDayStatus.CLAIMED -> Color(0xFF00E676).copy(alpha = 0.65f)
        DailyLoginDayStatus.LOCKED -> BoardBorder
    }

    val bgColor = when (day.status) {
        DailyLoginDayStatus.CLAIMABLE -> Color(0xFF28200D)
        DailyLoginDayStatus.CLAIMED -> Color(0xFF0F241D)
        DailyLoginDayStatus.LOCKED -> Color(0xFF101424)
    }

    Column(
        modifier = modifier
            .scale(pulseScale)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(if (isClaimable) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag("daily_login_day_${tier.cycleDay}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "DAY ${tier.cycleDay}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = when (day.status) {
                    DailyLoginDayStatus.CLAIMABLE -> GoldAccent
                    DailyLoginDayStatus.CLAIMED -> Color(0xFF00E676)
                    DailyLoginDayStatus.LOCKED -> Color(0xFF8E9BB0)
                }
            )
            if (isClaimed) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Claimed",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(11.dp)
                )
            } else if (!isClaimable) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFF5A6782),
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        if (tier.coins > 0) {
            Text(
                text = "+${tier.coins} 🪙",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isClaimed) Color(0xFFA5F3C4) else GoldAccent,
                maxLines = 1
            )
        }
        if (tier.hammers > 0) {
            Text(
                text = "+${tier.hammers} 🔨",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isClaimed) Color(0xFFA5F3C4) else Color(0xFF00E5FF),
                maxLines = 1
            )
        }

        Text(
            text = when (day.status) {
                DailyLoginDayStatus.CLAIMABLE -> "TODAY"
                DailyLoginDayStatus.CLAIMED -> "CLAIMED"
                DailyLoginDayStatus.LOCKED -> "NEXT"
            },
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = when (day.status) {
                DailyLoginDayStatus.CLAIMABLE -> Color(0xFFFFE082)
                DailyLoginDayStatus.CLAIMED -> Color(0xFF00E676)
                DailyLoginDayStatus.LOCKED -> Color(0xFF5A6782)
            }
        )
    }
}

@Composable
private fun JackpotDayTile(
    day: DailyLoginCalendarDay,
    pulseScale: Float,
    modifier: Modifier = Modifier
) {
    val tier = day.tier
    val isClaimed = day.status == DailyLoginDayStatus.CLAIMED
    val isClaimable = day.status == DailyLoginDayStatus.CLAIMABLE

    val borderBrush = when (day.status) {
        DailyLoginDayStatus.CLAIMABLE -> Brush.horizontalGradient(listOf(GoldAccent, Color(0xFFFF4081)))
        DailyLoginDayStatus.CLAIMED -> Brush.horizontalGradient(listOf(Color(0xFF00E676), Color(0xFF00E5FF)))
        DailyLoginDayStatus.LOCKED -> Brush.horizontalGradient(listOf(Color(0xFF3E345A), Color(0xFF283254)))
    }

    Row(
        modifier = modifier
            .scale(pulseScale)
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.horizontalGradient(
                    when (day.status) {
                        DailyLoginDayStatus.CLAIMABLE -> listOf(Color(0xFF34210D), Color(0xFF2B1433))
                        DailyLoginDayStatus.CLAIMED -> listOf(Color(0xFF0E2921), Color(0xFF102034))
                        DailyLoginDayStatus.LOCKED -> listOf(Color(0xFF17132B), Color(0xFF111629))
                    }
                )
            )
            .border(if (isClaimable) 1.5.dp else 1.dp, borderBrush, RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("daily_login_day_${tier.cycleDay}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "👑", fontSize = 18.sp)
            Column {
                Text(
                    text = "DAY 7 MEGA JACKPOT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isClaimed) Color(0xFF00E676) else GoldAccent
                )
                Text(
                    text = when (day.status) {
                        DailyLoginDayStatus.CLAIMABLE -> "Ready to claim today!"
                        DailyLoginDayStatus.CLAIMED -> "Jackpot Claimed!"
                        DailyLoginDayStatus.LOCKED -> "Play 7 consecutive days"
                    },
                    fontSize = 9.sp,
                    color = Color(0xFFB4C0D8)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "+${tier.coins} 🪙",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = GoldAccent
            )
            Text(
                text = "+${tier.hammers} 🔨",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00E5FF)
            )
        }
    }
}
