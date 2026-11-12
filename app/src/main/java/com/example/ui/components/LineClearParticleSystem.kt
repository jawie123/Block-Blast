package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.LineClearBurst
import com.example.ui.theme.JewelPalettes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.isActive

/**
 * Visual variety of particles spawned during a line clear explosion.
 */
enum class ParticleType {
    GEM_SHARD,        // Rotating diamond-shaped gemstone crystal
    STAR_SPARK,       // Radiant 4-pointed twinkling star
    GLOW_ORB,         // Glowing circular energy particle with outer halo
    FIRE_EMBER,       // Fast, fiery high-velocity ember spark (boosted on combos)
    COMBO_STARBURST   // Multi-color rainbow/gold spark cluster for high multipliers
}

/**
 * Mutable state object for individual particles.
 */
class ActiveParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val glowColor: Color,
    val initialSize: Float,
    var size: Float,
    var alpha: Float = 1f,
    var rotation: Float = 0f,
    val rotationSpeed: Float = 0f,
    val type: ParticleType = ParticleType.GEM_SHARD,
    val maxLifeMs: Float,
    var ageMs: Float = 0f
) {
    val isDead: Boolean
        get() = ageMs >= maxLifeMs
}

/**
 * Expanding shockwave ring that ripples outward on combos.
 */
class ShockwaveRing(
    val centerX: Float,
    val centerY: Float,
    val maxRadius: Float,
    val color: Color,
    val maxLifeMs: Float = 420f,
    var ageMs: Float = 0f
) {
    val isDead: Boolean
        get() = ageMs >= maxLifeMs

    val progress: Float
        get() = (ageMs / maxLifeMs).coerceIn(0f, 1f)
}

/**
 * Laser-like shockwave beam that flashes across cleared rows/columns.
 */
class LineClearBeam(
    val isRow: Boolean,
    val index: Int,
    val color: Color,
    val maxLifeMs: Float = 320f,
    var ageMs: Float = 0f
) {
    val isDead: Boolean
        get() = ageMs >= maxLifeMs
}

/**
 * Highly optimized, arcade-grade Particle System Composable for line clear animations.
 * Provides rich visual feedback that scales dynamically with combo streaks:
 * - 1x: Crisp crystal shards and soft star sparkles in the tile's jewel palette.
 * - 2x: Electric cyan/purple aura with expanding shockwave rings across the line.
 * - 3x: Fiery crimson & sunset amber embers with concentric shockwave ripples.
 * - 4x+: Radiant gold, starlight white & rainbow fireworks with high-velocity starbursts!
 */
@Composable
fun LineClearParticleSystem(
    bursts: List<LineClearBurst>,
    cellSizePx: Float = 0f,
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableListOf<ActiveParticle>() }
    val beams = remember { mutableListOf<LineClearBeam>() }
    val rings = remember { mutableListOf<ShockwaveRing>() }
    val lastHandledBurstIds = remember { mutableSetOf<Long>() }

    var hasActiveEffects by remember { mutableStateOf(false) }
    var frameTick by remember { mutableLongStateOf(0L) }

    // Spawn new particles whenever new LineClearBursts arrive
    LaunchedEffect(bursts) {
        if (bursts.isEmpty()) return@LaunchedEffect

        val newBursts = bursts.filter { !lastHandledBurstIds.contains(it.id) }
        if (newBursts.isEmpty()) return@LaunchedEffect

        for (burst in newBursts) {
            lastHandledBurstIds.add(burst.id)
        }
        if (lastHandledBurstIds.size > 200) {
            lastHandledBurstIds.clear()
            lastHandledBurstIds.addAll(bursts.map { it.id })
        }

        val completedRows = newBursts.filter { it.isRowClear }.map { it.row }.distinct()
        val completedCols = newBursts.filter { it.isColClear }.map { it.col }.distinct()
        val maxCombo = newBursts.maxOfOrNull { it.comboMultiplier } ?: 1

        val effectiveCellSize = if (cellSizePx > 0f) cellSizePx else 40f

        // 1. Spawn laser shockwave beams for cleared rows & columns
        for (r in completedRows) {
            val sampleColor = when {
                maxCombo >= 4 -> Color(0xFFFFD700)
                maxCombo == 3 -> Color(0xFFFF007A)
                maxCombo == 2 -> Color(0xFF00E5FF)
                else -> JewelPalettes[newBursts.firstOrNull { it.row == r }?.colorId ?: 5]?.primary ?: Color(0xFF00E5FF)
            }
            beams.add(LineClearBeam(isRow = true, index = r, color = sampleColor))
        }
        for (c in completedCols) {
            val sampleColor = when {
                maxCombo >= 4 -> Color(0xFFFFD700)
                maxCombo == 3 -> Color(0xFFFF007A)
                maxCombo == 2 -> Color(0xFF00E5FF)
                else -> JewelPalettes[newBursts.firstOrNull { it.col == c }?.colorId ?: 5]?.primary ?: Color(0xFF00E5FF)
            }
            beams.add(LineClearBeam(isRow = false, index = c, color = sampleColor))
        }

        // 2. Spawn expanding shockwave rings on combo clears
        if (maxCombo >= 2) {
            val ringColor = when {
                maxCombo >= 4 -> Color(0xFFFFD700)
                maxCombo == 3 -> Color(0xFFFF1744)
                else -> Color(0xFF00E5FF)
            }

            // Spawn at intersection or center of cleared lines
            for (r in completedRows) {
                rings.add(
                    ShockwaveRing(
                        centerX = 4f * effectiveCellSize,
                        centerY = (r + 0.5f) * effectiveCellSize,
                        maxRadius = effectiveCellSize * 4.5f,
                        color = ringColor,
                        maxLifeMs = if (maxCombo >= 3) 500f else 380f
                    )
                )
            }
            for (c in completedCols) {
                rings.add(
                    ShockwaveRing(
                        centerX = (c + 0.5f) * effectiveCellSize,
                        centerY = 4f * effectiveCellSize,
                        maxRadius = effectiveCellSize * 4.5f,
                        color = ringColor,
                        maxLifeMs = if (maxCombo >= 3) 500f else 380f
                    )
                )
            }
        }

        // 3. Spawn particles from each cleared cell center
        for (burst in newBursts) {
            val palette = JewelPalettes[burst.colorId] ?: JewelPalettes[1]!!
            val centerX = (burst.col + 0.5f) * effectiveCellSize
            val centerY = (burst.row + 0.5f) * effectiveCellSize
            val combo = burst.comboMultiplier

            // Particle count scales with combo multiplier
            val count = when {
                combo >= 4 -> 28
                combo == 3 -> 22
                combo == 2 -> 16
                else -> 12
            }

            for (i in 0 until count) {
                val angle = (i.toFloat() / count * 2f * PI.toFloat()) + (Random.nextFloat() * 0.6f - 0.3f)
                val baseSpeed = Random.nextFloat() * 240f + 160f
                val comboSpeedMult = 1f + ((combo - 1) * 0.22f)
                val speed = baseSpeed * comboSpeedMult

                val vx = cos(angle) * speed
                val vy = sin(angle) * speed - (Random.nextFloat() * 110f + 40f) // initial upward lift

                val type = when {
                    combo >= 4 && i % 3 == 0 -> ParticleType.COMBO_STARBURST
                    combo >= 3 && i % 3 == 1 -> ParticleType.FIRE_EMBER
                    combo >= 2 && i % 3 == 0 -> ParticleType.STAR_SPARK
                    i % 4 == 0 -> ParticleType.STAR_SPARK
                    i % 4 == 1 -> ParticleType.GEM_SHARD
                    i % 4 == 2 -> ParticleType.FIRE_EMBER
                    else -> ParticleType.GLOW_ORB
                }

                val pColor = when (type) {
                    ParticleType.COMBO_STARBURST -> when (i % 4) {
                        0 -> Color(0xFFFFD700) // Golden
                        1 -> Color(0xFFFF007A) // Hot Pink
                        2 -> Color(0xFF00E5FF) // Electric Cyan
                        else -> Color.White
                    }
                    ParticleType.FIRE_EMBER -> if (combo >= 3) {
                        if (Random.nextBoolean()) Color(0xFFFFD700) else Color(0xFFFF3D00)
                    } else {
                        palette.light
                    }
                    ParticleType.STAR_SPARK -> if (Random.nextBoolean()) Color.White else palette.light
                    ParticleType.GEM_SHARD -> if (Random.nextBoolean()) palette.primary else palette.light
                    ParticleType.GLOW_ORB -> palette.primary
                }

                val pSize = when (type) {
                    ParticleType.COMBO_STARBURST -> Random.nextFloat() * 6f + 6f
                    ParticleType.STAR_SPARK -> Random.nextFloat() * 5.5f + 5f
                    ParticleType.GEM_SHARD -> Random.nextFloat() * 6.5f + 5.5f
                    ParticleType.FIRE_EMBER -> Random.nextFloat() * 4f + 3f
                    ParticleType.GLOW_ORB -> Random.nextFloat() * 4.5f + 4f
                } * (if (combo >= 3) 1.25f else 1.0f)

                val life = (Random.nextFloat() * 240f + 420f) * (if (combo >= 2) 1.2f else 1.0f)

                particles.add(
                    ActiveParticle(
                        x = centerX + (Random.nextFloat() * 10f - 5f),
                        y = centerY + (Random.nextFloat() * 10f - 5f),
                        vx = vx,
                        vy = vy,
                        color = pColor,
                        glowColor = palette.glow,
                        initialSize = pSize,
                        size = pSize,
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() * 720f - 360f),
                        type = type,
                        maxLifeMs = life
                    )
                )
            }
        }

        // Cap maximum particles for buttery smooth performance
        if (particles.size > 360) {
            val toRemove = particles.size - 360
            particles.subList(0, toRemove).clear()
        }

        hasActiveEffects = true
    }

    // Animation physics loop advancing continuously at frame rate
    LaunchedEffect(hasActiveEffects) {
        if (!hasActiveEffects) return@LaunchedEffect

        var lastTimeNanos = 0L

        while (isActive && (particles.isNotEmpty() || beams.isNotEmpty() || rings.isNotEmpty())) {
            withFrameNanos { nowNanos ->
                if (lastTimeNanos != 0L) {
                    val dtSec = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    val dtMs = dtSec * 1000f

                    // Update laser shockwave beams
                    val beamIt = beams.iterator()
                    while (beamIt.hasNext()) {
                        val b = beamIt.next()
                        b.ageMs += dtMs
                        if (b.isDead) beamIt.remove()
                    }

                    // Update shockwave rings
                    val ringIt = rings.iterator()
                    while (ringIt.hasNext()) {
                        val r = ringIt.next()
                        r.ageMs += dtMs
                        if (r.isDead) ringIt.remove()
                    }

                    // Update particles
                    val partIt = particles.iterator()
                    while (partIt.hasNext()) {
                        val p = partIt.next()
                        p.ageMs += dtMs
                        if (p.isDead) {
                            partIt.remove()
                        } else {
                            val progress = (p.ageMs / p.maxLifeMs).coerceIn(0f, 1f)
                            p.x += p.vx * dtSec
                            p.y += p.vy * dtSec
                            p.vy += 340f * dtSec // gravity acceleration
                            p.vx *= 0.965f       // air resistance
                            p.vy *= 0.965f
                            p.rotation += p.rotationSpeed * dtSec
                            p.alpha = (1f - progress * progress).coerceIn(0f, 1f)
                            p.size = (p.initialSize * (1f - 0.45f * progress)).coerceAtLeast(0.5f)
                        }
                    }
                }
                lastTimeNanos = nowNanos
                frameTick = nowNanos
            }
        }

        hasActiveEffects = false
    }

    // Draw particles, laser beams, and shockwave rings on Canvas
    Canvas(modifier = modifier.fillMaxSize()) {
        // Read frameTick to invalidate and redraw the Canvas on every animation frame
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTick

        val boardWidth = size.width
        val boardHeight = size.height
        val cellUnit = if (cellSizePx > 0f) cellSizePx else (boardWidth / 8f)

        // 1. Draw Laser Beams along completed lines
        for (beam in beams) {
            val beamProgress = (beam.ageMs / beam.maxLifeMs).coerceIn(0f, 1f)
            val beamAlpha = (1f - beamProgress) * 0.9f
            val beamThickness = (1f - beamProgress * 0.45f) * cellUnit * 0.38f

            if (beam.isRow) {
                val centerY = (beam.index + 0.5f) * cellUnit
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            beam.color.copy(alpha = beamAlpha * 0.65f),
                            Color.White.copy(alpha = beamAlpha),
                            beam.color.copy(alpha = beamAlpha * 0.65f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(0f, centerY - beamThickness / 2f),
                    size = Size(boardWidth, beamThickness)
                )
            } else {
                val centerX = (beam.index + 0.5f) * cellUnit
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            beam.color.copy(alpha = beamAlpha * 0.65f),
                            Color.White.copy(alpha = beamAlpha),
                            beam.color.copy(alpha = beamAlpha * 0.65f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(centerX - beamThickness / 2f, 0f),
                    size = Size(beamThickness, boardHeight)
                )
            }
        }

        // 2. Draw Expanding Combo Shockwave Rings
        for (ring in rings) {
            val ringProgress = ring.progress
            val radius = ring.maxRadius * ringProgress
            val ringAlpha = (1f - ringProgress) * 0.85f
            val strokeWidth = (1f - ringProgress) * 4f + 1.5f

            drawCircle(
                color = ring.color.copy(alpha = ringAlpha),
                radius = radius,
                center = Offset(ring.centerX, ring.centerY),
                style = Stroke(width = strokeWidth)
            )
            // Soft outer ripple
            drawCircle(
                color = Color.White.copy(alpha = ringAlpha * 0.5f),
                radius = (radius - 2f).coerceAtLeast(0f),
                center = Offset(ring.centerX, ring.centerY),
                style = Stroke(width = 1.2f)
            )
        }

        // 3. Draw Active Particles
        for (p in particles) {
            val center = Offset(p.x, p.y)

            when (p.type) {
                ParticleType.GLOW_ORB -> {
                    // Outer diffuse glow halo
                    drawCircle(
                        color = p.glowColor.copy(alpha = p.alpha * 0.4f),
                        radius = p.size * 2.2f,
                        center = center
                    )
                    // Solid core
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha),
                        radius = p.size,
                        center = center
                    )
                    // Center specular white gleam
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha * 0.85f),
                        radius = p.size * 0.4f,
                        center = center
                    )
                }

                ParticleType.FIRE_EMBER -> {
                    // Radiant intense ember
                    drawCircle(
                        color = Color(0xFFFFB300).copy(alpha = p.alpha * 0.45f),
                        radius = p.size * 2f,
                        center = center
                    )
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha),
                        radius = p.size,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha * 0.9f),
                        radius = p.size * 0.35f,
                        center = center
                    )
                }

                ParticleType.GEM_SHARD -> {
                    // Diamond-shaped gemstone fragment rotating
                    rotate(degrees = p.rotation, pivot = center) {
                        val path = Path().apply {
                            moveTo(p.x, p.y - p.size * 1.35f)
                            lineTo(p.x + p.size * 0.9f, p.y)
                            lineTo(p.x, p.y + p.size * 1.35f)
                            lineTo(p.x - p.size * 0.9f, p.y)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = p.color.copy(alpha = p.alpha),
                            style = Fill
                        )
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = p.alpha * 0.8f),
                            style = Stroke(width = 1.2f)
                        )
                    }
                }

                ParticleType.STAR_SPARK -> {
                    // 4-pointed radiant star sparkle
                    rotate(degrees = p.rotation, pivot = center) {
                        val starPath = Path().apply {
                            val outer = p.size * 1.5f
                            val inner = p.size * 0.35f
                            moveTo(p.x, p.y - outer)
                            lineTo(p.x + inner, p.y - inner)
                            lineTo(p.x + outer, p.y)
                            lineTo(p.x + inner, p.y + inner)
                            lineTo(p.x, p.y + outer)
                            lineTo(p.x - inner, p.y + inner)
                            lineTo(p.x - outer, p.y)
                            lineTo(p.x - inner, p.y - inner)
                            close()
                        }
                        drawPath(
                            path = starPath,
                            color = p.color.copy(alpha = p.alpha),
                            style = Fill
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = p.alpha * 0.95f),
                            radius = p.size * 0.45f,
                            center = center
                        )
                    }
                }

                ParticleType.COMBO_STARBURST -> {
                    // Radiant 6-pointed starburst for combos
                    rotate(degrees = p.rotation, pivot = center) {
                        val burstPath = Path().apply {
                            val outer = p.size * 1.8f
                            val inner = p.size * 0.4f
                            for (step in 0 until 6) {
                                val outAngle = step * (PI / 3).toFloat()
                                val inAngle = outAngle + (PI / 6).toFloat()
                                val outX = p.x + cos(outAngle) * outer
                                val outY = p.y + sin(outAngle) * outer
                                val inX = p.x + cos(inAngle) * inner
                                val inY = p.y + sin(inAngle) * inner
                                if (step == 0) moveTo(outX, outY) else lineTo(outX, outY)
                                lineTo(inX, inY)
                            }
                            close()
                        }
                        // Outer glowing halo
                        drawCircle(
                            color = p.color.copy(alpha = p.alpha * 0.35f),
                            radius = p.size * 2.4f,
                            center = center
                        )
                        drawPath(
                            path = burstPath,
                            color = p.color.copy(alpha = p.alpha),
                            style = Fill
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = p.alpha * 0.95f),
                            radius = p.size * 0.5f,
                            center = center
                        )
                    }
                }
            }
        }
    }
}
