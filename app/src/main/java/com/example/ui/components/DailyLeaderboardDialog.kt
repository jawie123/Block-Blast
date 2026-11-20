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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.window.DialogProperties
import com.example.model.DailyLeaderboardEntry
import com.example.model.GameMode
import com.example.model.GlobalLeaderboardEntry
import com.example.model.PlayerAvatarCatalog
import com.example.model.RankTierCatalog
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.GoldAccent
import java.text.NumberFormat
import java.util.Locale

enum class LeaderboardTab {
    HIGH_SCORES,
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
    highScoreEntries: List<GlobalLeaderboardEntry> = emptyList(),
    isHighScoreLoading: Boolean = false,
    highScoreErrorMessage: String? = null,
    playerOverallBestScore: Int = 0,
    initialTab: LeaderboardTab = LeaderboardTab.HIGH_SCORES,
    isGuestMode: Boolean = false,
    onSignInWithGoogle: () -> Unit = {},
    selectedAvatarId: String = "rookie",
    playerTotalStars: Int = 0,
    onSelectAvatar: (String) -> Unit = {}
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .heightIn(max = 600.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("daily_leaderboard_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BoardBackground),
            border = BorderStroke(1.5.dp, Color(0xFF2E395E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isGlobalTab = selectedTab != LeaderboardTab.DAILY

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
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (selectedTab) {
                                        LeaderboardTab.HIGH_SCORES -> Brush.linearGradient(
                                            listOf(Color(0xFF00E5FF), Color(0xFF00E676))
                                        )
                                        LeaderboardTab.ALL_TIME -> Brush.linearGradient(
                                            listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                        )
                                        LeaderboardTab.DAILY -> Brush.linearGradient(
                                            listOf(Color(0xFFFFD600), Color(0xFFFF9800))
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isGlobalTab) {
                                    Icons.Default.Public
                                } else {
                                    Icons.Default.EmojiEvents
                                },
                                contentDescription = null,
                                tint = Color(0xFF101424),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isGlobalTab) {
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
                                text = when (selectedTab) {
                                    LeaderboardTab.HIGH_SCORES -> "Compare Single-Game High Scores"
                                    LeaderboardTab.ALL_TIME -> "All-Time Cumulative Total Score"
                                    LeaderboardTab.DAILY -> buildString {
                                        append(dailyFormattedDate.ifEmpty { "Today" })
                                        if (dailyPuzzleTitle.isNotBlank()) {
                                            append(" • ")
                                            append(dailyPuzzleTitle)
                                        }
                                    }
                                },
                                fontSize = 11.sp,
                                color = when (selectedTab) {
                                    LeaderboardTab.HIGH_SCORES -> Color(0xFF00E676)
                                    LeaderboardTab.ALL_TIME -> Color(0xFF00E5FF)
                                    LeaderboardTab.DAILY -> Color(0xFFFFB74D)
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

                Spacer(modifier = Modifier.height(6.dp))

                // Tab Switcher: HIGH SCORES vs ALL-TIME vs DAILY
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF101424))
                        .border(1.dp, BoardBorder, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isHighScores = selectedTab == LeaderboardTab.HIGH_SCORES
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isHighScores) {
                                    Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF00E676)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { selectedTab = LeaderboardTab.HIGH_SCORES }
                            .testTag("leaderboard_tab_high_scores"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HIGH SCORES",
                            fontSize = 10.sp,
                            fontWeight = if (isHighScores) FontWeight.Black else FontWeight.Bold,
                            color = if (isHighScores) Color(0xFF0F111E) else Color(0xFF8E9BB0),
                            maxLines = 1,
                            letterSpacing = 0.3.sp
                        )
                    }

                    val isAllTime = selectedTab == LeaderboardTab.ALL_TIME
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
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
                            fontSize = 10.sp,
                            fontWeight = if (isAllTime) FontWeight.Black else FontWeight.Bold,
                            color = if (isAllTime) Color.White else Color(0xFF8E9BB0),
                            maxLines = 1,
                            letterSpacing = 0.3.sp
                        )
                    }

                    val isDaily = selectedTab == LeaderboardTab.DAILY
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
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
                            fontSize = 10.sp,
                            fontWeight = if (isDaily) FontWeight.Black else FontWeight.Bold,
                            color = if (isDaily) Color(0xFF0F111E) else Color(0xFF8E9BB0),
                            maxLines = 1,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Player Summary Bar
                val displayBestHighScore = maxOf(
                    playerOverallBestScore,
                    highScoreEntries.firstOrNull { it.userId == currentUserId }?.bestScore?.toInt() ?: 0
                )
                val displayTotalScore = maxOf(
                    playerTotalScore,
                    globalEntries.firstOrNull { it.userId == currentUserId }?.totalScore ?: 0L
                )
                val myRank = when (selectedTab) {
                    LeaderboardTab.HIGH_SCORES -> highScoreEntries.indexOfFirst { it.userId == currentUserId }.let {
                        if (it >= 0) "#${it + 1}" else "--"
                    }
                    LeaderboardTab.ALL_TIME -> globalEntries.indexOfFirst { it.userId == currentUserId }.let {
                        if (it >= 0) "#${it + 1}" else "--"
                    }
                    LeaderboardTab.DAILY -> entries.indexOfFirst { it.userId == currentUserId }.let {
                        if (it >= 0) "#${it + 1}" else "--"
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF101424))
                        .border(1.dp, Color(0xFF283454), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (selectedTab) {
                                LeaderboardTab.HIGH_SCORES -> "YOUR HIGH SCORE"
                                LeaderboardTab.ALL_TIME -> "YOUR TOTAL SCORE"
                                LeaderboardTab.DAILY -> "YOUR BEST TODAY"
                            },
                            fontSize = 9.sp,
                            color = Color(0xFF8E9BB0),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (selectedTab) {
                                LeaderboardTab.HIGH_SCORES -> numberFormat.format(displayBestHighScore)
                                LeaderboardTab.ALL_TIME -> numberFormat.format(displayTotalScore)
                                LeaderboardTab.DAILY -> numberFormat.format(playerBestScore)
                            },
                            fontSize = 16.sp,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("leaderboard_player_summary_score")
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isGlobalTab) "GLOBAL RANK" else "WORLD RANK",
                            fontSize = 9.sp,
                            color = Color(0xFF8E9BB0),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = myRank,
                            fontSize = 16.sp,
                            color = GoldAccent,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Player Rank Tier & Avatar Badges Showcase (2-row compact strip)
                val currentRankTier = RankTierCatalog.fromProgress(displayTotalScore, displayBestHighScore)
                val nextRankTier = RankTierCatalog.nextTier(currentRankTier)
                val rankTierColor = Color(currentRankTier.colorHex)
                val rankProgress = RankTierCatalog.progressToNextTier(displayTotalScore, displayBestHighScore)
                val activeAvatar = PlayerAvatarCatalog.byId(selectedAvatarId)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF101424))
                        .border(1.dp, rankTierColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.testTag("player_rank_badge")
                        ) {
                            Text(text = activeAvatar.emoji, fontSize = 13.sp)
                            Text(
                                text = "${currentRankTier.badge} ${currentRankTier.title.uppercase()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = rankTierColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF1A2138))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(rankProgress.coerceIn(0.05f, 1f))
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(rankTierColor)
                            )
                        }
                        Text(
                            text = if (nextRankTier != null) {
                                "→ ${nextRankTier.badge} ${nextRankTier.title}"
                            } else {
                                "👑 MAX"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9BA8C2),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (spec in PlayerAvatarCatalog.allAvatars) {
                            val unlocked = spec.isUnlocked(currentRankTier, playerTotalStars)
                            val isSelected = spec.id == selectedAvatarId
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        when {
                                            isSelected -> rankTierColor.copy(alpha = 0.22f)
                                            unlocked -> Color(0xFF171C32)
                                            else -> Color(0xFF0C0F1C)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.4.dp else 1.dp,
                                        color = when {
                                            isSelected -> rankTierColor
                                            unlocked -> BoardBorder
                                            else -> Color(0xFF20263C)
                                        },
                                        shape = RoundedCornerShape(7.dp)
                                    )
                                    .clickable { onSelectAvatar(spec.id) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .testTag("avatar_badge_${spec.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(text = spec.emoji, fontSize = 11.sp)
                                if (!unlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFF6B768C),
                                        modifier = Modifier.size(9.dp)
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = rankTierColor,
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Direct Score Comparison Banner on High Scores Tab
                if (selectedTab == LeaderboardTab.HIGH_SCORES && highScoreEntries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    HighScoreComparisonBanner(
                        entries = highScoreEntries,
                        currentUserId = currentUserId,
                        playerBestScore = displayBestHighScore,
                        numberFormat = numberFormat
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Entries List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp, max = 230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectedTab) {
                        LeaderboardTab.HIGH_SCORES -> {
                            when {
                                isHighScoreLoading && highScoreEntries.isEmpty() -> {
                                    LoadingLeaderboardState("Syncing global high scores...")
                                }

                                highScoreErrorMessage != null && highScoreEntries.isEmpty() -> {
                                    ErrorLeaderboardState(highScoreErrorMessage)
                                }

                                isGuestMode && highScoreEntries.isEmpty() -> {
                                    GuestLeaderboardPrompt(
                                        onSignInWithGoogle = {
                                            onDismiss()
                                            onSignInWithGoogle()
                                        }
                                    )
                                }

                                highScoreEntries.isEmpty() -> {
                                    Text(
                                        text = "No global high scores posted yet.\nSet a high score to challenge players worldwide!",
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
                                            .testTag("high_score_leaderboard_list"),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        itemsIndexed(
                                            items = highScoreEntries,
                                            key = { _, item -> item.userId.ifEmpty { item.id } }
                                        ) { index, entry ->
                                            HighScoreLeaderboardRow(
                                                rank = index + 1,
                                                entry = entry,
                                                isCurrentUser = entry.userId == currentUserId,
                                                playerReferenceBestScore = displayBestHighScore,
                                                numberFormat = numberFormat
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        LeaderboardTab.ALL_TIME -> {
                            when {
                                isGlobalLoading && globalEntries.isEmpty() -> {
                                    LoadingLeaderboardState("Syncing global rankings...")
                                }

                                globalErrorMessage != null && globalEntries.isEmpty() -> {
                                    ErrorLeaderboardState(globalErrorMessage)
                                }

                                isGuestMode && globalEntries.isEmpty() -> {
                                    GuestLeaderboardPrompt(
                                        onSignInWithGoogle = {
                                            onDismiss()
                                            onSignInWithGoogle()
                                        }
                                    )
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
                        }

                        LeaderboardTab.DAILY -> {
                            when {
                                isLoading && entries.isEmpty() -> {
                                    LoadingLeaderboardState("Loading daily rankings...")
                                }

                                errorMessage != null && entries.isEmpty() -> {
                                    ErrorLeaderboardState(errorMessage)
                                }

                                isGuestMode && entries.isEmpty() -> {
                                    GuestLeaderboardPrompt(
                                        onSignInWithGoogle = {
                                            onDismiss()
                                            onSignInWithGoogle()
                                        }
                                    )
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
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (gameMode != GameMode.DAILY_CHALLENGE) {
                    Button(
                        onClick = {
                            onPlayDaily()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
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
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
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
private fun HighScoreComparisonBanner(
    entries: List<GlobalLeaderboardEntry>,
    currentUserId: String,
    playerBestScore: Int,
    numberFormat: NumberFormat
) {
    val myIndex = entries.indexOfFirst { it.userId == currentUserId }
    val leader = entries.first()
    val leaderName = leader.displayName.ifBlank { "Player" }
    val leaderScore = leader.bestScore.toInt()

    val comparisonText = when {
        myIndex == 0 && entries.size > 1 -> {
            val runnerUp = entries[1]
            val lead = (playerBestScore - runnerUp.bestScore.toInt()).coerceAtLeast(0)
            "Leading #2 ${runnerUp.displayName.ifBlank { "Player" }} by +${numberFormat.format(lead)} pts"
        }
        myIndex == 0 -> {
            "#1 Worldwide! Set an even higher score to defend your crown"
        }
        myIndex == 1 -> {
            val gapToFirst = (leaderScore - playerBestScore).coerceAtLeast(0)
            "${numberFormat.format(gapToFirst)} pts behind #1 $leaderName to take 1st!"
        }
        myIndex > 1 -> {
            val above = entries[myIndex - 1]
            val gapToAbove = (above.bestScore.toInt() - playerBestScore).coerceAtLeast(0)
            val gapToFirst = (leaderScore - playerBestScore).coerceAtLeast(0)
            "${numberFormat.format(gapToAbove)} pts to pass #${myIndex} ${above.displayName.ifBlank { "Player" }} • ${numberFormat.format(gapToFirst)} behind #1"
        }
        playerBestScore > 0 -> {
            val gapToFirst = (leaderScore - playerBestScore).coerceAtLeast(0)
            "${numberFormat.format(gapToFirst)} pts behind #1 $leaderName (${numberFormat.format(leaderScore)})"
        }
        else -> {
            "Top score to beat: ${numberFormat.format(leaderScore)} pts by $leaderName"
        }
    }

    val isLeading = myIndex == 0
    val accentColor = if (isLeading) GoldAccent else Color(0xFF00E5FF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("leaderboard_comparison_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = comparisonText,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
private fun GuestLeaderboardPrompt(onSignInWithGoogle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("leaderboard_guest_prompt"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Guest Mode Active",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Sign in with Google to sync your high score and compete on the live global & daily leaderboards.",
            color = Color(0xFF8E9BB0),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onSignInWithGoogle,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("leaderboard_guest_sign_in_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF121629)
            )
        ) {
            Text(
                text = "Sign in with Google",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
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
private fun HighScoreLeaderboardRow(
    rank: Int,
    entry: GlobalLeaderboardEntry,
    isCurrentUser: Boolean,
    playerReferenceBestScore: Int,
    numberFormat: NumberFormat
) {
    val rankColor = when (rank) {
        1 -> GoldAccent
        2 -> Color(0xFFE0E0E0)
        3 -> Color(0xFFFF8A65)
        else -> Color(0xFF8E9BB0)
    }
    val deltaVsYou = entry.bestScore.toInt() - playerReferenceBestScore
    val deltaText = when {
        isCurrentUser -> null
        deltaVsYou > 0 -> "+${numberFormat.format(deltaVsYou)} vs you"
        deltaVsYou < 0 -> "-${numberFormat.format(-deltaVsYou)} vs you"
        else -> "TIED"
    }
    val deltaColor = when {
        deltaVsYou > 0 -> Color(0xFFFF8A65)
        deltaVsYou < 0 -> Color(0xFF00E676)
        else -> Color(0xFF00E5FF)
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

            val entryTier = RankTierCatalog.fromProgress(entry.totalScore, entry.bestScore.toInt())
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = entryTier.badge,
                        fontSize = 12.sp
                    )
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
                    text = "Total ${numberFormat.format(entry.totalScore)} • ${entry.totalLinesCleared} lines • x${entry.highestCombo}",
                    color = Color(0xFF8E9BB0),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = numberFormat.format(entry.bestScore),
                color = if (rank == 1) GoldAccent else Color(0xFF00E5FF),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            if (deltaText != null) {
                Text(
                    text = deltaText,
                    color = deltaColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "HIGH SCORE",
                    color = Color(0xFF6B7792),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
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

            val entryTier = RankTierCatalog.fromProgress(entry.totalScore, entry.bestScore.toInt())
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = entryTier.badge,
                        fontSize = 12.sp
                    )
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
