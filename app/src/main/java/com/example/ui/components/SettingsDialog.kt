package com.example.ui.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.Achievement
import com.example.model.AchievementCatalog
import com.example.model.BoardThemeCatalog
import com.example.model.BoardThemeId
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder

@Composable
fun SettingsDialog(
    isMusicEnabled: Boolean,
    isSoundEnabled: Boolean,
    isHapticsEnabled: Boolean,
    isPrivacyOptionsRequired: Boolean = false,
    achievements: List<Achievement> = AchievementCatalog.evaluateAll(0, 0, 0, 0, 0),
    selectedThemeId: String = BoardThemeId.NEON_ARCADE.name,
    unlockedThemeIds: Set<String> = setOf(
        BoardThemeId.NEON_ARCADE.name,
        BoardThemeId.CLASSIC_BLOCK.name
    ),
    onSelectTheme: (BoardThemeId) -> Unit = {},
    onOpenThemeShop: () -> Unit = {},
    onToggleMusic: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    isDailyRemindersEnabled: Boolean = false,
    onToggleReminders: () -> Unit = {},
    onShowPrivacyOptions: () -> Unit = {},
    onOpenTutorial: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val unlockedCount = achievements.count { it.isUnlocked }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .heightIn(max = 580.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("settings_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BoardBackground),
            border = BorderStroke(1.5.dp, BoardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Pinned Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.settings_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = stringResource(R.string.settings_subtitle),
                                fontSize = 11.sp,
                                color = Color(0xFF8E9BB0),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = Color(0xFF8E9BB0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Body: Audio Controls + Dedicated Achievements Section
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "AUDIO & FEEDBACK",
                        fontSize = 10.sp,
                        color = Color(0xFF8E9BB0),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Background Music Toggle Row
                    SettingsToggleRow(
                        title = stringResource(R.string.settings_music_label),
                        subtitle = stringResource(R.string.settings_music_desc),
                        checked = isMusicEnabled,
                        icon = if (isMusicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                        accentColor = Color(0xFF7C4DFF),
                        rowTag = "settings_music_row",
                        switchTag = "settings_music_switch",
                        onToggle = onToggleMusic
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sound Effects Toggle Row
                    SettingsToggleRow(
                        title = stringResource(R.string.settings_sfx_label),
                        subtitle = stringResource(R.string.settings_sfx_desc),
                        checked = isSoundEnabled,
                        icon = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        accentColor = Color(0xFF00E5FF),
                        rowTag = "settings_sfx_row",
                        switchTag = "settings_sfx_switch",
                        onToggle = onToggleSound
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Haptic Feedback Toggle Row
                    SettingsToggleRow(
                        title = stringResource(R.string.settings_haptics_label),
                        subtitle = stringResource(R.string.settings_haptics_desc),
                        checked = isHapticsEnabled,
                        icon = Icons.Default.Vibration,
                        accentColor = Color(0xFFFF9800),
                        rowTag = "settings_haptics_row",
                        switchTag = "settings_haptics_switch",
                        onToggle = onToggleHaptics
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opt-in Daily Streak & Spin Reminders Toggle Row
                    SettingsToggleRow(
                        title = stringResource(R.string.settings_reminders_label),
                        subtitle = stringResource(R.string.settings_reminders_desc),
                        checked = isDailyRemindersEnabled,
                        icon = if (isDailyRemindersEnabled) {
                            Icons.Default.NotificationsActive
                        } else {
                            Icons.Default.NotificationsOff
                        },
                        accentColor = Color(0xFF00E676),
                        rowTag = "settings_reminders_row",
                        switchTag = "settings_reminders_switch",
                        onToggle = onToggleReminders
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Board Theme Selector (Neon, Classic, Wood, Candy, Obsidian)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "BOARD THEME",
                                fontSize = 10.sp,
                                color = Color(0xFF8E9BB0),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "THEME SHOP →",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD700),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onOpenThemeShop() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("settings_open_theme_shop_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("settings_theme_selector_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (theme in BoardThemeCatalog.allThemes) {
                            val isSelected = selectedThemeId == theme.id.name
                            val isUnlocked = theme.costCoins == 0 || unlockedThemeIds.contains(theme.id.name)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) theme.accentColor.copy(alpha = 0.2f)
                                        else Color(0xFF0F111E)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) theme.accentColor else BoardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isUnlocked) onSelectTheme(theme.id) else onOpenThemeShop()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("settings_theme_chip_${theme.id.name}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(text = theme.badgeEmoji, fontSize = 13.sp)
                                Text(
                                    text = theme.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Color(0xFFB4C0D8),
                                    maxLines = 1
                                )
                                if (!isUnlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(11.dp)
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = theme.accentColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (isPrivacyOptionsRequired) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.settings_privacy_section_title),
                            fontSize = 10.sp,
                            color = Color(0xFF8E9BB0),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0F111E))
                                .border(1.dp, BoardBorder, RoundedCornerShape(16.dp))
                                .clickable(role = Role.Button, onClick = onShowPrivacyOptions)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("settings_privacy_options_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF).copy(alpha = 0.16f))
                                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PrivacyTip,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = stringResource(R.string.settings_privacy_options_label),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_privacy_options_desc),
                                        fontSize = 11.sp,
                                        color = Color(0xFF8E9BB0)
                                    )
                                }
                            }
                            Text(
                                text = "MANAGE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dedicated Achievements Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_achievements_section"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(15.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.achievements_section_title),
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.9.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.achievements_section_subtitle),
                                        fontSize = 10.sp,
                                        color = Color(0xFF8E9BB0),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (unlockedCount > 0) Color(0xFF00E676).copy(alpha = 0.16f)
                                        else Color(0xFF1A2036)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (unlockedCount > 0) Color(0xFF00E676).copy(alpha = 0.6f)
                                        else BoardBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("achievements_count_badge")
                            ) {
                                Text(
                                    text = "$unlockedCount / ${achievements.size} UNLOCKED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (unlockedCount > 0) Color(0xFF00E676) else Color(0xFF8E9BB0),
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }

                        achievements.forEach { achievement ->
                            AchievementCard(achievement = achievement)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pinned Bottom Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenTutorial()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("settings_tutorial_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BoardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "HOW TO PLAY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("settings_done_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        Text(
                            text = "DONE",
                            color = Color(0xFF0F111E),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    val accent = Color(achievement.accentHex)
    val isUnlocked = achievement.isUnlocked
    val borderColor = if (isUnlocked) Color(0xFF00E676).copy(alpha = 0.7f) else BoardBorder

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isUnlocked) Color(0xFF102022) else Color(0xFF0F111E))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("achievement_card_${achievement.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked) accent.copy(alpha = 0.22f)
                            else Color(0xFF1A1F33)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isUnlocked) accent else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = achievement.badge,
                        fontSize = 15.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = achievement.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        if (isUnlocked) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Unlocked",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Text(
                        text = achievement.description,
                        fontSize = 10.sp,
                        color = Color(0xFF8E9BB0),
                        maxLines = 2
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isUnlocked) Color(0xFF00E676).copy(alpha = 0.18f)
                        else Color(0xFF1A2036)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isUnlocked) "UNLOCKED" else "${achievement.current} / ${achievement.target}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isUnlocked) Color(0xFF00E676) else Color(0xFFCFD8DC)
                )
            }
        }

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1B2138))
        ) {
            val fillFraction = achievement.progressFraction.coerceIn(0f, 1f)
            if (fillFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (isUnlocked) {
                                    listOf(Color(0xFF00E676), Color(0xFF00E5FF))
                                } else {
                                    listOf(accent.copy(alpha = 0.7f), accent)
                                }
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector,
    accentColor: Color,
    rowTag: String,
    switchTag: String,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F111E))
            .border(
                width = 1.dp,
                color = if (checked) accentColor.copy(alpha = 0.55f) else BoardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(rowTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) accentColor.copy(alpha = 0.18f) else Color(0xFF1A1F33)
                    )
                    .border(
                        width = 1.dp,
                        color = if (checked) accentColor.copy(alpha = 0.7f) else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) accentColor else Color(0xFF6B768C),
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (checked) Color(0xFF00E676).copy(alpha = 0.16f)
                                else Color(0xFF232940)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (checked) "ON" else "OFF",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (checked) Color(0xFF00E676) else Color(0xFF8E9BB0)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF8E9BB0)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.testTag(switchTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                checkedBorderColor = accentColor,
                uncheckedThumbColor = Color(0xFF8E9BB0),
                uncheckedTrackColor = Color(0xFF1A2036),
                uncheckedBorderColor = BoardBorder
            )
        )
    }
}
