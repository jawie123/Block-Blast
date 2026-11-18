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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.BoardThemeCatalog
import com.example.model.BoardThemeSpec
import com.example.ui.theme.CellEmpty
import com.example.ui.theme.CellEmptyBorder

@Composable
fun BlockCell(
    colorId: Int,
    isGhostPreview: Boolean = false,
    ghostColorId: Int? = null,
    isClearing: Boolean = false,
    isProspectiveClear: Boolean = false,
    isHammerTarget: Boolean = false,
    isBombTarget: Boolean = false,
    theme: BoardThemeSpec = BoardThemeCatalog.defaultTheme,
    emptyTileTint: Color? = null,
    cornerRadius: Dp = 6.dp,
    pulseAlpha: Float? = null,
    modifier: Modifier = Modifier
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val needsPulse = isProspectiveClear || isHammerTarget || isBombTarget
    val resolvedPulse = when {
        !needsPulse -> 1f
        pulseAlpha != null -> pulseAlpha
        else -> rememberLocalCellPulse(isFastTarget = isHammerTarget || isBombTarget)
    }

    when {
        // Clearing flash animation
        isClearing -> {
            Box(
                modifier = modifier
                    .padding(1.5.dp)
                    .clip(shape)
                    .drawWithCache {
                        val flashBrush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFFFD700), Color(0xFFFF8008))
                        )
                        onDrawBehind {
                            drawRect(brush = flashBrush)
                        }
                    }
                    .border(1.5.dp, Color.White, shape)
                    .fillMaxSize()
                    .testTag("block_cell_clearing")
            )
        }

        // Ghost placement preview
        isGhostPreview -> {
            val palette = theme.paletteFor(ghostColorId ?: 5)
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

        // Filled jewel or wood block
        colorId > 0 -> {
            val palette = theme.paletteFor(colorId)
            val borderHighlight = when {
                isBombTarget -> Color(0xFFFF5722).copy(alpha = resolvedPulse)
                isHammerTarget -> Color(0xFFFFB300).copy(alpha = resolvedPulse)
                isProspectiveClear -> Color.White.copy(alpha = resolvedPulse)
                else -> palette.light
            }
            val borderWidth = if (isHammerTarget || isBombTarget) 2.dp else 1.dp
            val isWood = theme.isWoodStyle

            Box(
                modifier = modifier
                    .padding(1.5.dp)
                    .clip(shape)
                    .drawWithCache {
                        val verticalBrush = Brush.verticalGradient(
                            colors = listOf(palette.light, palette.primary, palette.dark)
                        )
                        val glossBrush = if (!isWood) {
                            Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                                center = Offset(size.width * 0.25f, size.height * 0.25f),
                                radius = (size.width * 0.55f).coerceAtLeast(1f)
                            )
                        } else {
                            null
                        }
                        onDrawBehind {
                            drawRect(brush = verticalBrush)
                            if (isWood) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.18f),
                                    start = Offset(size.width * 0.14f, size.height * 0.28f),
                                    end = Offset(size.width * 0.86f, size.height * 0.28f),
                                    strokeWidth = 1.5f
                                )
                                drawLine(
                                    color = Color.Black.copy(alpha = 0.16f),
                                    start = Offset(size.width * 0.20f, size.height * 0.68f),
                                    end = Offset(size.width * 0.80f, size.height * 0.68f),
                                    strokeWidth = 1.2f
                                )
                            } else if (glossBrush != null) {
                                drawCircle(brush = glossBrush)
                            }
                        }
                    }
                    .border(borderWidth, borderHighlight, shape)
                    .fillMaxSize()
                    .testTag("block_cell_filled")
            )
        }

        // Empty tile cell on 8x8 grid
        else -> {
            val baseEmpty = if (theme == BoardThemeCatalog.defaultTheme) CellEmpty else theme.cellEmpty
            val baseEmptyBorder = if (theme == BoardThemeCatalog.defaultTheme) CellEmptyBorder else theme.cellEmptyBorder
            val emptyBg = when {
                isBombTarget -> Color(0xFFFF5722).copy(alpha = resolvedPulse * 0.16f)
                isProspectiveClear -> Color(0xFFFFD700).copy(alpha = resolvedPulse * 0.25f)
                emptyTileTint != null -> emptyTileTint.copy(alpha = 0.09f)
                else -> baseEmpty
            }
            val emptyBorder = when {
                isBombTarget -> Color(0xFFFF5722).copy(alpha = resolvedPulse * 0.7f)
                isProspectiveClear -> Color(0xFFFFD700).copy(alpha = resolvedPulse * 0.8f)
                emptyTileTint != null -> emptyTileTint.copy(alpha = 0.28f)
                else -> baseEmptyBorder
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

@Composable
private fun rememberLocalCellPulse(isFastTarget: Boolean): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "cell_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = if (isFastTarget) 0.5f else 0.35f,
        targetValue = if (isFastTarget) 1.0f else 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isFastTarget) 350 else 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cell_pulse_alpha"
    )
    return pulse
}
