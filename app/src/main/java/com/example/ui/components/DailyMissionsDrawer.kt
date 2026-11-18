package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyMission
import com.example.model.MissionType
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.GoldAccent
import kotlin.math.roundToInt

@Composable
fun DailyMissionsDrawer(
    visible: Boolean,
    missions: List<DailyMission>,
    coinsBalance: Int,
    hintCount: Int = 0,
    hammerCount: Int,
    onClaimReward: (missionId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible) {
        onDismiss()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "claim_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            dragOffsetPx = 0f
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Dimmed scrim backdrop
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(240)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB3060812))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .testTag("missions_drawer_scrim")
            )
        }

        // Slide-in Drawer Panel anchored on the left edge
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                initialOffsetX = { -it }
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutHorizontally(
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                targetOffsetX = { -it }
            ) + fadeOut(animationSpec = tween(180)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            val completedCount = missions.count { it.isCompleted }
            val claimedCount = missions.count { it.isClaimed }
            val dailyCompletionFraction by animateFloatAsState(
                targetValue = if (missions.isNotEmpty()) {
                    completedCount.toFloat() / missions.size.toFloat()
                } else {
                    0f
                },
                animationSpec = tween(350),
                label = "daily_completion_progress"
            )

            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.86f)
                    .widthIn(min = 280.dp, max = 368.dp)
                    .offset { IntOffset(dragOffsetPx.roundToInt().coerceAtMost(0), 0) }
                    .pointerInput(onDismiss) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffsetPx < -90f) {
                                    onDismiss()
                                } else {
                                    dragOffsetPx = 0f
                                }
                            },
                            onDragCancel = { dragOffsetPx = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                val next = (dragOffsetPx + dragAmount).coerceAtMost(0f)
                                dragOffsetPx = next
                            }
                        )
                    }
                    .testTag("daily_missions_drawer"),
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                color = BoardBackground,
                border = BorderStroke(1.5.dp, Color(0xFF2E395E)),
                shadowElevation = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF151C36),
                                    BoardBackground,
                                    Color(0xFF0C1020)
                                )
                            )
                        )
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Header row
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
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(GoldAccent, Color(0xFFFF6D00))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🎯", fontSize = 16.sp)
                            }
                            Column {
                                Text(
                                    text = "DAILY MISSIONS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 0.6.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Complete goals for bonus coins & hints!",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9FB0CC),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("close_missions_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Daily Missions Drawer",
                                tint = Color(0xFFB4C0D8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Wallet & Daily Progress Strip (Coins, Hints, Hammers)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1.05f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF10162B))
                                .border(1.dp, GoldAccent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("missions_drawer_coins_balance"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🪙", fontSize = 12.sp)
                            Text(
                                text = "$coinsBalance",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldAccent,
                                maxLines = 1,
                                modifier = Modifier.testTag("missions_drawer_coins_value")
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(0.95f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF10162B))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("missions_drawer_hint_balance"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "💡", fontSize = 12.sp)
                            Text(
                                text = "$hintCount",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF80D8FF),
                                maxLines = 1,
                                modifier = Modifier.testTag("missions_drawer_hint_value")
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1.0f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF10162B))
                                .border(1.dp, Color(0xFFFFB74D).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("missions_drawer_hammer_balance"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🔨", fontSize = 12.sp)
                            Text(
                                text = "$hammerCount",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFCC80),
                                maxLines = 1,
                                modifier = Modifier.testTag("missions_drawer_hammer_value")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Overall Daily Progress Pill
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF101526))
                            .border(1.dp, BoardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TODAY'S OBJECTIVES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB4C0D8),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$completedCount / ${missions.size} Ready • $claimedCount Claimed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (completedCount == missions.size && missions.isNotEmpty()) {
                                    Color(0xFF00E676)
                                } else {
                                    GoldAccent
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF1E2640))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(dailyCompletionFraction)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(GoldAccent, Color(0xFF00E5FF))
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Scrollable Missions Feed
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("missions_drawer_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(missions, key = { it.id }) { mission ->
                            DailyMissionCard(
                                mission = mission,
                                pulseScale = pulseScale,
                                onClaimReward = { onClaimReward(mission.id) }
                            )
                        }
                        item(key = "dismiss_footer") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("dismiss_missions_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2640))
                            ) {
                                Text(
                                    text = "BACK TO GAME",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyMissionCard(
    mission: DailyMission,
    pulseScale: Float,
    onClaimReward: () -> Unit
) {
    val icon = when (mission.type) {
        MissionType.CLEAR_LINES -> Icons.Default.ViewWeek
        MissionType.CLEAR_LINES_SINGLE_MOVE -> Icons.Default.Bolt
        MissionType.REACH_COMBO -> Icons.Default.LocalFireDepartment
        MissionType.SCORE_POINTS -> Icons.Default.EmojiEvents
        MissionType.PLACE_BLOCKS -> Icons.Default.Widgets
        MissionType.USE_HAMMER -> Icons.Default.Build
    }

    val accentColor = when (mission.type) {
        MissionType.CLEAR_LINES -> Color(0xFF40C4FF)
        MissionType.CLEAR_LINES_SINGLE_MOVE -> Color(0xFF00E5FF)
        MissionType.REACH_COMBO -> Color(0xFFFF4081)
        MissionType.SCORE_POINTS -> GoldAccent
        MissionType.PLACE_BLOCKS -> Color(0xFF00E676)
        MissionType.USE_HAMMER -> Color(0xFFFF9800)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = mission.progressFraction,
        animationSpec = tween(300),
        label = "mission_progress_${mission.id}"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101424))
            .border(
                width = if (mission.isCompleted && !mission.isClaimed) 1.5.dp else 1.dp,
                color = if (mission.isCompleted && !mission.isClaimed) GoldAccent else BoardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(10.dp)
            .testTag("mission_item_${mission.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Top row: Icon + Title/Description (full width so text never wraps awkwardly)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = mission.title,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mission.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mission.description,
                        fontSize = 11.sp,
                        color = Color(0xFF9FB0CC),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Horizontal Reward Chips Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mission.coinReward > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(GoldAccent.copy(alpha = 0.16f))
                            .border(1.dp, GoldAccent.copy(alpha = 0.55f), RoundedCornerShape(7.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+${mission.coinReward} Coins",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldAccent
                        )
                    }
                }
                if (mission.hintReward > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.55f), RoundedCornerShape(7.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+${mission.hintReward} Hints",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF80D8FF)
                        )
                    }
                }
                if (mission.hammerReward > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFFFF9800).copy(alpha = 0.14f))
                            .border(1.dp, Color(0xFFFFB74D).copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+${mission.hammerReward} Hammer",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D)
                        )
                    }
                }
            }

            // Progress Bar & Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E253C))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accentColor.copy(alpha = 0.78f), accentColor)
                                )
                            )
                    )
                }

                Text(
                    text = "${mission.current} / ${mission.target}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (mission.isCompleted) Color(0xFF00E676) else Color.White
                )
            }

            // Status / Claim CTA
            when {
                mission.isClaimed -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Reward Claimed",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "CLAIMED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E676),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                mission.isCompleted -> {
                    val rewardTokens = buildList {
                        if (mission.coinReward > 0) add("+${mission.coinReward} 🪙")
                        if (mission.hintReward > 0) add("+${mission.hintReward} 💡")
                        if (mission.hammerReward > 0) add("+${mission.hammerReward} 🔨")
                    }.joinToString(" & ")
                    Button(
                        onClick = onClaimReward,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .scale(pulseScale)
                            .testTag("claim_reward_button_${mission.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = Color(0xFF121420)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🎁", fontSize = 13.sp)
                            Text(
                                text = "CLAIM $rewardTokens",
                                color = Color(0xFF121420),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = if (mission.type == MissionType.CLEAR_LINES_SINGLE_MOVE) {
                            "BEST MOVE: ${mission.current}/${mission.target} LINES"
                        } else {
                            "IN PROGRESS"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7B99),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
