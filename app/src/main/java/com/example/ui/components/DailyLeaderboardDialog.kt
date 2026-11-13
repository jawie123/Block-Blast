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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.example.model.DailyLeaderboardEntry
import com.example.model.GameMode
import com.example.model.GlobalLeaderboardEntry
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.GoldAccent
import java.text.NumberFormat
import java.util.Locale

enum class LeaderboardTab {
    ALL_TIME,
    DAILY
}

@Composable
fun DailyLeaderboardDialog(
    entries: List<DailyLeaderboardEntry>,
    isLoading: Boolean,
    errorMessage: String?,
    dailyFormattedDate: String,
    dailyPuzzleTitle: String,
    currentUserId: String,
    playerBestScore: Int,
    gameMode: GameMode,
    onPlayDaily: () -> Unit,
    onDismiss: () -> Unit,
    globalEntries: List<GlobalLeaderboardEntry> = emptyList(),
    isGlobalLoading: Boolean = false,
    globalErrorMessage: String? = null,
    playerTotalScore: Long = 0L,
    initialTab: LeaderboardTab = LeaderboardTab.ALL_TIME
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }

    val pulseTransition = rememberInfiniteTransition(label = "liveDot")
    val liveDotAlpha by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("daily_leaderboard_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BoardBackground),
            border = BorderStroke(1.5.dp, Color(0xFF2E395E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selectedTab == LeaderboardTab.ALL_TIME) {
                                        Brush.linearGradient(
                                            listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFFD600), Color(0xFFFF9800))
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == LeaderboardTab.ALL_TIME) {
                                    Icons.Default.Public
                                } else {
                                    Icons.Default.EmojiEvents
                                },
                                contentDescription = null,
                                tint = Color(0xFF101424),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (selectedTab == LeaderboardTab.ALL_TIME) {
                                        "GLOBAL LEADERBOARD"
                                    } else {
                                        "DAILY LEADERBOARD"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x2600E676))
                                        .border(0.8.dp, Color(0xFF00E676), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .alpha(liveDotAlpha)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E676))
                                        )
                                        Text(
                                            text = "LIVE",
                                            color = Color(0xFF00E676),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (selectedTab == LeaderboardTab.ALL_TIME) {
                                    "All-Time Cumulative Total Score"
                                } else {
                                    buildString {
                                        append(dailyFormattedDate.ifEmpty { "Today" })
                                        if (dailyPuzzleTitle.isNotBlank()) {
                                            append(" • ")
                                            append(dailyPuzzleTitle)
                                        }
                                    }
                                },
                                fontSize = 11.sp,
                                color = if (selectedTab == LeaderboardTab.ALL_TIME) {
                                    Color(0xFF00E5FF)
                                } else {
                                    Color(0xFFFFB74D)
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_leaderboard_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Leaderboard",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Switcher: ALL-TIME vs DAILY
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF101424))
                        .border(1.dp, BoardBorder, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isAllTime = selectedTab == LeaderboardTab.ALL_TIME
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isAllTime) {
                                    Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { selectedTab = LeaderboardTab.ALL_TIME }
                            .testTag("leaderboard_tab_all_time"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ALL-TIME",
                            fontSize = 11.sp,
                            fontWeight = if (isAllTime) FontWeight.Black else FontWeight.Bold,
                            color = if (isAllTime) Color.White else Color(0xFF8E9BB0),
                            letterSpacing = 0.5.sp
                        )
                    }

                    val isDaily = selectedTab == LeaderboardTab.DAILY
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDaily) {
                                    Brush.horizontalGradient(listOf(Color(0xFFFF9800), Color(0xFFFFD600)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { selectedTab = LeaderboardTab.DAILY }
                            .testTag("leaderboard_tab_daily"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DAILY",
                            fontSize = 11.sp,
                            fontWeight = if (isDaily) FontWeight.Black else FontWeight.Bold,
                            color = if (isDaily) Color(0xFF0F111E) else Color(0xFF8E9BB0),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Player Summary Bar
                val displayTotalScore = maxOf(
                    playerTotalScore,
                    globalEntries.firstOrNull { it.userId == currentUserId }?.totalScore ?: 0L
                )
                val myRank = if (selectedTab == LeaderboardTab.ALL_TIME) {
                    globalEntries.indexOfFirst { it.userId == currentUserId }.let {
                        if (it >= 0) "#${it + 1}" else "--"
                    }
                } else {
                    entries.indexOfFirst { it.userId == currentUserId }.let {
                        if (it >= 0) "#${it + 1}" else "--"
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF101424))
                        .border(1.dp, Color(0xFF283454), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (selectedTab == LeaderboardTab.ALL_TIME) {
                                "YOUR TOTAL SCORE"
                            } else {
                                "YOUR BEST TODAY"
                            },
                            fontSize = 9.sp,
                            color = Color(0xFF8E9BB0),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedTab == LeaderboardTab.ALL_TIME) {
                                numberFormat.format(displayTotalScore)
                            } else {
                                numberFormat.format(playerBestScore)
                            },
                            fontSize = 18.sp,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("leaderboard_player_summary_score")
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (selectedTab == LeaderboardTab.ALL_TIME) {
                                "GLOBAL RANK"
                            } else {
                                "WORLD RANK"
                            },
                            fontSize = 9.sp,
                            color = Color(0xFF8E9BB0),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = myRank,
                            fontSize = 18.sp,
                            color = GoldAccent,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Entries List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedTab == LeaderboardTab.ALL_TIME) {
                        when {
                            isGlobalLoading && globalEntries.isEmpty() -> {
                                LoadingLeaderboardState("Syncing global rankings...")
                            }

                            globalErrorMessage != null && globalEntries.isEmpty() -> {
                                ErrorLeaderboardState(globalErrorMessage)
                            }

                            globalEntries.isEmpty() -> {
                                Text(
                                    text = "No global scores recorded yet.\nPlace blocks in any mode to climb the all-time ranks!",
                                    color = Color(0xFF8E9BB0),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            else -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("global_leaderboard_list"),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(
                                        items = globalEntries,
                                        key = { _, item -> item.userId.ifEmpty { item.id } }
                                    ) { index, entry ->
                                        GlobalLeaderboardRow(
                                            rank = index + 1,
                                            entry = entry,
                                            isCurrentUser = entry.userId == currentUserId,
                                            numberFormat = numberFormat
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        when {
                            isLoading && entries.isEmpty() -> {
                                LoadingLeaderboardState("Loading daily rankings...")
                            }

                            errorMessage != null && entries.isEmpty() -> {
                                ErrorLeaderboardState(errorMessage)
                            }

                            entries.isEmpty() -> {
                                Text(
                                    text = "No daily scores posted yet.\nPlay today's 8×8 puzzle to claim #1!",
                                    color = Color(0xFF8E9BB0),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            else -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("daily_leaderboard_list"),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(
                                        items = entries,
                                        key = { _, item -> item.userId.ifEmpty { item.id } }
                                    ) { index, entry ->
                                        LeaderboardRow(
                                            rank = index + 1,
                                            entry = entry,
                                            isCurrentUser = entry.userId == currentUserId,
                                            numberFormat = numberFormat
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (gameMode != GameMode.DAILY_CHALLENGE) {
                    Button(
                        onClick = {
                            onPlayDaily()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("play_daily_from_leaderboard_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF0F111E),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "PLAY TODAY'S PUZZLE",
                            color = Color(0xFF0F111E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("dismiss_leaderboard_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2640))
                ) {
                    Text(
                        text = "CLOSE",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingLeaderboardState(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            color = Color(0xFF00E5FF),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color(0xFF8E9BB0),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ErrorLeaderboardState(errorMessage: String) {
    Text(
        text = errorMessage,
        color = Color(0xFFFF8A80),
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun GlobalLeaderboardRow(
    rank: Int,
    entry: GlobalLeaderboardEntry,
    isCurrentUser: Boolean,
    numberFormat: NumberFormat
) {
    val rankColor = when (rank) {
        1 -> GoldAccent
        2 -> Color(0xFFE0E0E0)
        3 -> Color(0xFFFF8A65)
        else -> Color(0xFF8E9BB0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrentUser) Color(0xFF192844) else Color(0xFF101424))
            .border(
                width = if (isCurrentUser) 1.5.dp else 1.dp,
                color = if (isCurrentUser) Color(0xFF00E5FF) else BoardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = if (rank <= 3) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = rankColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.displayName.ifBlank { "Player" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentUser) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00E5FF))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "YOU",
                                color = Color(0xFF0F111E),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                Text(
                    text = "Best ${numberFormat.format(entry.bestScore)} • ${entry.totalLinesCleared} lines • x${entry.highestCombo}",
                    color = Color(0xFF8E9BB0),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = numberFormat.format(entry.totalScore),
                color = if (rank == 1) GoldAccent else Color(0xFF00E5FF),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "TOTAL",
                color = Color(0xFF6B7792),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    entry: DailyLeaderboardEntry,
    isCurrentUser: Boolean,
    numberFormat: NumberFormat
) {
    val rankColor = when (rank) {
        1 -> GoldAccent
        2 -> Color(0xFFE0E0E0)
        3 -> Color(0xFFFF8A65)
        else -> Color(0xFF8E9BB0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrentUser) Color(0xFF192844) else Color(0xFF101424))
            .border(
                width = if (isCurrentUser) 1.5.dp else 1.dp,
                color = if (isCurrentUser) Color(0xFF00E5FF) else BoardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = if (rank <= 3) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = rankColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.displayName.ifBlank { "Player" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentUser) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00E5FF))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "YOU",
                                color = Color(0xFF0F111E),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                Text(
                    text = "${entry.linesCleared} lines • x${entry.maxCombo} max combo",
                    color = Color(0xFF8E9BB0),
                    fontSize = 10.sp
                )
            }
        }

        Text(
            text = numberFormat.format(entry.score),
            color = if (rank == 1) GoldAccent else Color(0xFF00E5FF),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
    }
}
