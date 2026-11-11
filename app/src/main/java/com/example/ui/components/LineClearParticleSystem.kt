package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.LineClearBurst
import com.example.ui.theme.JewelPalettes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Visual variety of particles spawned during a line clear explosion.
 */
enum class ParticleType {
    GEM_SHARD,   // Rotating diamond-shaped gemstone crystal
    STAR_SPARK,  // Radiant 4-pointed twinkling star
    GLOW_ORB,    // Glowing circular energy particle with outer halo
    FIRE_EMBER   // Fast, fiery high-velocity ember spark (boosted on combos)
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
 * Laser-like shockwave beam that flashes across cleared rows/columns.
 */
class LineClearBeam(
    val isRow: Boolean,
    val index: Int,
    val color: Color,
    val maxLifeMs: Float = 300f,
    var ageMs: Float = 0f
) {
    val isDead: Boolean
        get() = ageMs >= maxLifeMs
}

/**
 * Highly optimized, arcade-grade Particle System Composable for line clear animations.
 * Renders hundreds of physics-based particles and laser shockwave beams on a single Canvas.
 */
@Composable
fun LineClearParticleSystem(
    bursts: List<LineClearBurst>,
    cellSizePx: Float,
    modifier: Modifier = Modifier
) {
    if (cellSizePx <= 0f) return

    val particles = remember { mutableStateListOf<ActiveParticle>() }
    val beams = remember { mutableStateListOf<LineClearBeam>() }
    val lastHandledBurstIds = remember { mutableSetOf<Long>() }

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

        // 1. Spawn laser shockwave beams for cleared rows & columns
        for (r in completedRows) {
            val sampleColor = JewelPalettes[newBursts.firstOrNull { it.row == r }?.colorId ?: 5]?.primary
                ?: Color(0xFF00E5FF)
            beams.add(LineClearBeam(isRow = true, index = r, color = sampleColor))
        }
        for (c in completedCols) {
            val sampleColor = JewelPalettes[newBursts.firstOrNull { it.col == c }?.colorId ?: 5]?.primary
                ?: Color(0xFF00E5FF)
            beams.add(LineClearBeam(isRow = false, index = c, color = sampleColor))
        }

        // 2. Spawn particles from each cleared cell center
        for (burst in newBursts) {
            val palette = JewelPalettes[burst.colorId] ?: JewelPalettes[1]!!
            val centerX = (burst.col + 0.5f) * cellSizePx
            val centerY = (burst.row + 0.5f) * cellSizePx
            val isCombo = burst.comboMultiplier > 1
            val particleCount = if (isCombo) 16 else 12

            for (i in 0 until particleCount) {
                val angle = (i.toFloat() / particleCount * 2f * PI.toFloat()) +
                        Random.nextFloat() * 0.5f - 0.25f
                val baseSpeed = Random.nextFloat() * 280f + 140f
                val speed = if (isCombo) baseSpeed * 1.25f else baseSpeed

                val vx = cos(angle) * speed
                val vy = sin(angle) * speed - (Random.nextFloat() * 90f + 30f) // slight upward blast

                val type = when {
                    i % 3 == 0 -> ParticleType.STAR_SPARK
                    i % 3 == 1 -> ParticleType.GEM_SHARD
                    isCombo && i % 4 == 0 -> ParticleType.FIRE_EMBER
                    else -> ParticleType.GLOW_ORB
                }

                val pColor = when (type) {
                    ParticleType.STAR_SPARK -> if (Random.nextBoolean()) Color.White else palette.light
                    ParticleType.FIRE_EMBER -> if (Random.nextBoolean()) Color(0xFFFFD700) else Color(0xFFFF3D00)
                    ParticleType.GEM_SHARD -> if (Random.nextBoolean()) palette.primary else palette.light
                    ParticleType.GLOW_ORB -> palette.primary
                }

                val size = when (type) {
                    ParticleType.STAR_SPARK -> Random.nextFloat() * 5f + 5f
                    ParticleType.GEM_SHARD -> Random.nextFloat() * 6f + 6f
                    ParticleType.FIRE_EMBER -> Random.nextFloat() * 3.5f + 3f
                    ParticleType.GLOW_ORB -> Random.nextFloat() * 4.5f + 4f
                }

                val life = Random.nextFloat() * 250f + (if (isCombo) 480f else 400f)

                particles.add(
                    ActiveParticle(
                        x = centerX + (Random.nextFloat() * 12f - 6f),
                        y = centerY + (Random.nextFloat() * 12f - 6f),
                        vx = vx,
                        vy = vy,
                        color = pColor,
                        glowColor = palette.glow,
                        initialSize = size,
                        size = size,
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() * 720f - 360f),
                        type = type,
                        maxLifeMs = life
                    )
                )
            }
        }
    }

    // Animation loop advancing physics at refresh rate
    LaunchedEffect(particles.size, beams.size) {
        if (particles.isEmpty() && beams.isEmpty()) return@LaunchedEffect

        var lastTimeNanos = withFrameMillis { it * 1_000_000L }

        while (particles.isNotEmpty() || beams.isNotEmpty()) {
            val currentNanos = withFrameMillis { it * 1_000_000L }
            val dtSec = ((currentNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            val dtMs = dtSec * 1000f
            lastTimeNanos = currentNanos

            // Update laser shockwave beams
            val beamIt = beams.iterator()
            while (beamIt.hasNext()) {
                val b = beamIt.next()
                b.ageMs += dtMs
                if (b.isDead) beamIt.remove()
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
                    p.vy += 320f * dtSec // gravity
                    p.vx *= 0.96f        // air drag
                    p.vy *= 0.96f
                    p.rotation += p.rotationSpeed * dtSec
                    p.alpha = (1f - progress).coerceIn(0f, 1f)
                    p.size = (p.initialSize * (1f - 0.45f * progress)).coerceAtLeast(0.5f)
                }
            }
        }
    }

    // Draw particles and laser shockwave beams on Canvas
    Canvas(modifier = modifier.fillMaxSize()) {
        val boardWidth = size.width
        val boardHeight = size.height

        // 1. Draw Laser Beams along completed lines
        for (beam in beams) {
            val beamProgress = (beam.ageMs / beam.maxLifeMs).coerceIn(0f, 1f)
            val beamAlpha = (1f - beamProgress) * 0.85f
            val beamThickness = (1f - beamProgress * 0.5f) * cellSizePx * 0.35f

            if (beam.isRow) {
                val centerY = (beam.index + 0.5f) * cellSizePx
                // Horizontal shockwave beam
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            beam.color.copy(alpha = beamAlpha * 0.6f),
                            Color.White.copy(alpha = beamAlpha),
                            beam.color.copy(alpha = beamAlpha * 0.6f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(0f, centerY - beamThickness / 2f),
                    size = Size(boardWidth, beamThickness)
                )
            } else {
                val centerX = (beam.index + 0.5f) * cellSizePx
                // Vertical shockwave beam
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            beam.color.copy(alpha = beamAlpha * 0.6f),
                            Color.White.copy(alpha = beamAlpha),
                            beam.color.copy(alpha = beamAlpha * 0.6f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(centerX - beamThickness / 2f, 0f),
                    size = Size(beamThickness, boardHeight)
                )
            }
        }

        // 2. Draw active particles
        for (p in particles) {
            val center = Offset(p.x, p.y)

            when (p.type) {
                ParticleType.GLOW_ORB -> {
                    // Outer diffuse glow halo
                    drawCircle(
                        color = p.glowColor.copy(alpha = p.alpha * 0.35f),
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
                        color = Color(0xFFFFB300).copy(alpha = p.alpha * 0.4f),
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
                            moveTo(p.x, p.y - p.size * 1.3f)
                            lineTo(p.x + p.size * 0.85f, p.y)
                            lineTo(p.x, p.y + p.size * 1.3f)
                            lineTo(p.x - p.size * 0.85f, p.y)
                            close()
                        }
                        // Jewel fill
                        drawPath(
                            path = path,
                            color = p.color.copy(alpha = p.alpha),
                            style = Fill
                        )
                        // Brilliant white facet highlight border
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = p.alpha * 0.75f),
                            style = Stroke(width = 1.2f)
                        )
                    }
                }

                ParticleType.STAR_SPARK -> {
                    // 4-pointed radiant star sparkle
                    rotate(degrees = p.rotation, pivot = center) {
                        val starPath = Path().apply {
                            val outer = p.size * 1.4f
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
                        // Soft star glow
                        drawPath(
                            path = starPath,
                            color = p.color.copy(alpha = p.alpha),
                            style = Fill
                        )
                        // White inner core
                        drawCircle(
                            color = Color.White.copy(alpha = p.alpha * 0.9f),
                            radius = p.size * 0.45f,
                            center = center
                        )
                    }
                }
            }
        }
    }
}
