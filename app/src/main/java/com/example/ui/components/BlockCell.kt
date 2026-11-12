package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CellEmpty
import com.example.ui.theme.CellEmptyBorder
import com.example.ui.theme.getBlockPalette

@Composable
fun BlockCell(
    colorId: Int,
    isGhostPreview: Boolean = false,
    ghostColorId: Int? = null,
    isClearing: Boolean = false,
    isProspectiveClear: Boolean = false,
    isHammerTarget: Boolean = false,
    emptyTileTint: Color? = null,
    cornerRadius: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(cornerRadius)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val prospectivePulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val hammerPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hammerPulseAlpha"
    )

    when {
        // Clearing flash animation
        isClearing -> {
            Box(
                modifier = modifier
                    .padding(1.5.dp)
                    .clip(shape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFFFD700), Color(0xFFFF8008))
                        )
                    )
                    .border(1.5.dp, Color.White, shape)
                    .fillMaxSize()
                    .testTag("block_cell_clearing")
            )
        }

        // Ghost placement preview
        isGhostPreview -> {
            val palette = getBlockPalette(ghostColorId ?: 5)
            Box(
                modifier = modifier
                    .padding(1.5.dp)
                    .clip(shape)
                    .background(palette.primary.copy(alpha = 0.55f))
                    .border(1.5.dp, palette.light, shape)
                    .fillMaxSize()
                    .testTag("block_cell_ghost")
            )
        }

        // Filled jewel block
        colorId > 0 -> {
            val palette = getBlockPalette(colorId)
            val borderHighlight = when {
                isHammerTarget -> Color(0xFFFFB300).copy(alpha = hammerPulse)
                isProspectiveClear -> Color.White.copy(alpha = prospectivePulse)
                else -> palette.light
            }
            val borderWidth = if (isHammerTarget) 2.dp else 1.dp

            Box(
                modifier = modifier
                    .padding(1.5.dp)
                    .clip(shape)
                    .drawBehind {
                        // Glossy top-to-bottom jewel gradient
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(palette.light, palette.primary, palette.dark)
                            )
                        )
                        // Diagonal gloss reflection
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                                center = Offset(size.width * 0.25f, size.height * 0.25f),
                                radius = size.width * 0.55f
                            )
                        )
                    }
                    .border(borderWidth, borderHighlight, shape)
                    .fillMaxSize()
                    .testTag("block_cell_filled")
            )
        }

        // Empty tile cell on 8x8 grid
        else -> {
            val emptyBg = when {
                isProspectiveClear -> Color(0xFFFFD700).copy(alpha = prospectivePulse * 0.25f)
                emptyTileTint != null -> emptyTileTint.copy(alpha = 0.09f)
                else -> CellEmpty
            }
            val emptyBorder = when {
                isProspectiveClear -> Color(0xFFFFD700).copy(alpha = prospectivePulse * 0.8f)
                emptyTileTint != null -> emptyTileTint.copy(alpha = 0.28f)
                else -> CellEmptyBorder
            }

            Box(
                modifier = modifier
                    .padding(1.5.dp)
                    .clip(shape)
                    .background(emptyBg)
                    .border(1.dp, emptyBorder, shape)
                    .fillMaxSize()
                    .testTag("block_cell_empty")
            )
        }
    }
}
