package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.model.BlockShape
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.CellEmpty
import com.example.ui.theme.CellEmptyBorder
import com.example.ui.theme.GoldAccent
import kotlin.math.roundToInt

/**
 * Interactive, semi-transparent tutorial overlay for new players.
 * Explains and lets the player practice the core drag-and-drop mechanics,
 * grid placement rules, and combo multipliers.
 */
@Composable
fun TutorialOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    soundManager: SoundManager? = null,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val haptic = LocalHapticFeedback.current
    var currentStep by remember { mutableIntStateOf(0) }

    // Semi-transparent backdrop with animated entry
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE60A0D1B)) // ~90% dark translucent scrim
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* block touches through to the underlying game */ }
            .testTag("tutorial_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .padding(vertical = 16.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BoardBackground),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF)))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Badge + Title + Skip Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "HOW TO PLAY",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            soundManager?.playClick()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("tutorial_skip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Tutorial",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content for Current Step
                when (currentStep) {
                    0 -> StepDragAndDropPractice(
                        soundManager = soundManager,
                        onStepCompleted = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        }
                    )
                    1 -> StepGridPlacementRules(
                        soundManager = soundManager
                    )
                    else -> StepCombosAndMultipliers()
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Step Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 3) {
                        val isSelected = currentStep == i
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            label = "dotWidth_$i"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF252D48), Color(0xFF252D48))
                                        )
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Controls (Back / Next / Start Playing)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = {
                                soundManager?.playClick()
                                currentStep--
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("tutorial_prev_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BoardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF8E9BB0)
                            )
                        ) {
                            Text(
                                text = "BACK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (currentStep < 2) {
                        Button(
                            onClick = {
                                soundManager?.playClick()
                                currentStep++
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("tutorial_next_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C4DFF)
                            )
                        ) {
                            Text(
                                text = "NEXT",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                soundManager?.playClick()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("tutorial_start_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF)
                            )
                        ) {
                            Text(
                                text = "START PLAYING! 🚀",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFF0F111E)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 1: Interactive Touch & Drag Playground.
 * The player can physically drag a block upward into the target landing slot!
 */
@Composable
private fun StepDragAndDropPractice(
    soundManager: SoundManager?,
    onStepCompleted: () -> Unit
) {
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isTargetReached by remember { mutableStateOf(false) }

    // Idle guidance hand animation when user has not dragged yet
    val infiniteTransition = rememberInfiniteTransition(label = "hand_guide")
    val guideAnimY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -50f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "guideY"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "1. Touch & Drag Mechanics",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Hold any piece from the bottom pool and drag it upwards onto the board.",
            fontSize = 13.sp,
            color = Color(0xFF8E9BB0),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Sandbox Arena
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F1322))
                .border(1.dp, BoardBorder, RoundedCornerShape(16.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Target Drop Zone at top
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(90.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isTargetReached) Color(0x3300E676)
                        else if (isDragging && dragOffsetY < -35f) Color(0x3300E5FF)
                        else Color(0xFF161C2E)
                    )
                    .border(
                        1.5.dp,
                        if (isTargetReached) Color(0xFF00E676)
                        else if (isDragging && dragOffsetY < -35f) Color(0xFF00E5FF)
                        else Color(0xFF283250),
                        RoundedCornerShape(8.dp)
                    )
                    .testTag("tutorial_target_drop_zone"),
                contentAlignment = Alignment.Center
            ) {
                if (isTargetReached) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "DROPPED!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E676)
                        )
                    }
                } else {
                    Text(
                        text = "DROP HERE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7894)
                    )
                }
            }

            // Draggable Practice Piece
            val demoShape = remember {
                BlockShape(
                    id = "tut_piece",
                    name = "tut_piece",
                    matrix = listOf(listOf(true, true, true)),
                    colorId = 5 // Electric Cyan
                )
            }

            val currentOffsetY = if (isTargetReached) -55f else dragOffsetY

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, currentOffsetY.roundToInt()) }
                    .scale(if (isDragging) 1.15f else 1.0f)
                    .pointerInput(isTargetReached) {
                        if (isTargetReached) return@pointerInput
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                soundManager?.playPickup()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(-75f, 0f)
                            },
                            onDragEnd = {
                                isDragging = false
                                if (dragOffsetY < -40f) {
                                    isTargetReached = true
                                    soundManager?.playPlace()
                                    onStepCompleted()
                                } else {
                                    dragOffsetY = 0f
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffsetY = 0f
                            }
                        )
                    }
                    .testTag("interactive_tutorial_piece")
            ) {
                ShapeRenderer(
                    shape = demoShape,
                    cellUnitDp = 24.dp
                )
            }

            // Animated Hand Pointer Guide when not touched yet
            if (!isDragging && !isTargetReached) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Hand drag gesture demonstration",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset { IntOffset(35, guideAnimY.roundToInt()) }
                        .size(30.dp)
                )
            }

            // Reset Button if already placed
            if (isTargetReached) {
                IconButton(
                    onClick = {
                        isTargetReached = false
                        dragOffsetY = 0f
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Try Again",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Tip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161E34))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "💡 Tip: You can also tap any block to select it, then tap an empty cell on the grid to place it!",
                fontSize = 11.sp,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Medium,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Step 2: Grid Placement Validation & Ghost Preview explanation.
 */
@Composable
private fun StepGridPlacementRules(
    soundManager: SoundManager?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "2. Placement & Ghost Preview",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Hovering over the grid shows a bright preview of where your shape will land.",
            fontSize = 13.sp,
            color = Color(0xFF8E9BB0),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Mini 4x4 Grid Demonstration
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F1322))
                .border(1.dp, BoardBorder, RoundedCornerShape(12.dp))
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (r in 0 until 4) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (c in 0 until 4) {
                            val isOccupied = (r == 0 && c == 0) || (r == 0 && c == 1) || (r == 3 && c == 3)
                            val isGhostPreview = (r in 1..2 && c in 1..2)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            isOccupied -> Color(0xFFFF5252)
                                            isGhostPreview -> Color(0x66FFD600)
                                            else -> CellEmpty
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isGhostPreview -> Color(0xFFFFD600)
                                            isOccupied -> Color(0xFFFF5252)
                                            else -> CellEmptyBorder
                                        },
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rules List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RuleItem(
                iconTint = Color(0xFF00E676),
                title = "Empty Cells Only",
                desc = "Shapes cannot overlap existing blocks or go out of bounds."
            )
            RuleItem(
                iconTint = Color(0xFFFF9800),
                title = "Game Over Condition",
                desc = "Game ends if none of the 3 bottom pieces can fit anywhere."
            )
        }
    }
}

/**
 * Step 3: Clearing Lines & Combo Multipliers explanation.
 */
@Composable
private fun StepCombosAndMultipliers() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "3. Line Clears & Combos",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Fill full rows or columns to clear blocks and activate combo multipliers!",
            fontSize = 13.sp,
            color = Color(0xFF8E9BB0),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Combo Multiplier Visual Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF161B30), Color(0xFF221A3A))
                    )
                )
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFF9100), Color(0xFFFFD700))
                    ),
                    RoundedCornerShape(14.dp)
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9100),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "COMBO STREAK METER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF9100), Color(0xFFFFD700))
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "3x MULTIPLIER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F111E)
                        )
                    }
                }

                // Visual row clear preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (i in 0 until 8) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFFF8008), Color(0xFFFFC837))
                                    )
                                )
                        )
                    }
                }

                Text(
                    text = "Clear lines on consecutive drops to charge your multiplier from 1x up to 5x+!",
                    fontSize = 11.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        RuleItem(
            iconTint = Color(0xFF00E5FF),
            title = "Multi-Line Clears",
            desc = "Clear 2, 3, or 4 lines at once for huge combo bonuses."
        )
    }
}

@Composable
private fun RuleItem(
    iconTint: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF131728))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(iconTint)
        )
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color(0xFF8E9BB0),
                lineHeight = 14.sp
            )
        }
    }
}
