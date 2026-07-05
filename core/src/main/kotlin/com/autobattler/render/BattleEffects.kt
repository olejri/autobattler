package com.autobattler.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.*

/**
 * Manages visual battle effects: sword slashes, arrow projectiles, heal glows.
 * Effects are added and tick each frame until they expire.
 */
class BattleEffects {

    private val activeEffects = mutableListOf<Effect>()

    /** Whether any effect is currently playing (blocks event advancement). */
    val isPlaying: Boolean get() = activeEffects.any { it.blocksPlayback }

    fun update(delta: Float) {
        activeEffects.forEach { it.update(delta) }
        activeEffects.removeAll { it.isFinished }
    }

    fun render(shapeRenderer: ShapeRenderer) {
        if (activeEffects.isEmpty()) return
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        activeEffects.forEach { it.renderFilled(shapeRenderer) }
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        activeEffects.forEach { it.renderLine(shapeRenderer) }
        shapeRenderer.end()
    }

    // ─── Effect Factories ───────────────────────────────────────────────────────

    /**
     * Sword slash: Arc sweep at target position.
     */
    fun addSlash(targetScreenX: Float, targetScreenY: Float, facingLeft: Boolean) {
        activeEffects.add(SlashEffect(targetScreenX, targetScreenY, facingLeft))
    }

    /**
     * Arrow projectile: Flies from source to target.
     */
    fun addArrow(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        activeEffects.add(ArrowEffect(fromX, fromY, toX, toY))
    }

    /**
     * Heal glow: Green expanding ring + sparkles at target.
     */
    fun addHealGlow(targetScreenX: Float, targetScreenY: Float) {
        activeEffects.add(HealEffect(targetScreenX, targetScreenY))
    }

    fun clear() = activeEffects.clear()

    // ─── Base Effect ────────────────────────────────────────────────────────────

    private abstract class Effect(val duration: Float, val blocksPlayback: Boolean = false) {
        var elapsed = 0f
        val progress: Float get() = (elapsed / duration).coerceIn(0f, 1f)
        val isFinished: Boolean get() = elapsed >= duration

        fun update(delta: Float) { elapsed += delta }
        open fun renderFilled(shapeRenderer: ShapeRenderer) {}
        open fun renderLine(shapeRenderer: ShapeRenderer) {}
    }

    // ─── Sword Slash ────────────────────────────────────────────────────────────

    private class SlashEffect(
        val cx: Float, val cy: Float, val facingLeft: Boolean
    ) : Effect(duration = 0.3f) {

        override fun renderFilled(shapeRenderer: ShapeRenderer) {
            val alpha = 1f - progress
            val sweepProgress = smoothStep(progress)

            // Draw multiple arc segments to create a slash trail
            val startAngle = if (facingLeft) 120f else -60f
            val sweepAngle = 180f * sweepProgress

            val trailLength = 5
            for (i in 0 until trailLength) {
                val t = i.toFloat() / trailLength
                val angle = startAngle + sweepAngle * t
                val rad = Math.toRadians(angle.toDouble())

                val radius = 20f + progress * 8f
                val x = cx + (radius * cos(rad)).toFloat()
                val y = cy + (radius * sin(rad)).toFloat()

                val size = (4f - i * 0.6f) * (1f - progress * 0.5f)
                val a = alpha * (1f - t * 0.7f)

                shapeRenderer.color = Color(1f, 1f, 0.8f, a)
                shapeRenderer.circle(x, y, size)
            }

            // Bright slash tip
            val tipAngle = startAngle + sweepAngle
            val tipRad = Math.toRadians(tipAngle.toDouble())
            val tipRadius = 22f + progress * 8f
            val tipX = cx + (tipRadius * cos(tipRad)).toFloat()
            val tipY = cy + (tipRadius * sin(tipRad)).toFloat()

            shapeRenderer.color = Color(1f, 1f, 1f, alpha * 0.9f)
            shapeRenderer.circle(tipX, tipY, 3f * (1f - progress))
        }

        override fun renderLine(shapeRenderer: ShapeRenderer) {
            val alpha = (1f - progress) * 0.8f
            val sweepProgress = smoothStep(progress)
            val startAngle = if (facingLeft) 120f else -60f
            val sweepAngle = 180f * sweepProgress

            shapeRenderer.color = Color(1f, 0.9f, 0.5f, alpha)

            val radius = 20f + progress * 8f
            val segments = 12
            for (i in 0 until segments) {
                val a1 = startAngle + sweepAngle * i / segments
                val a2 = startAngle + sweepAngle * (i + 1) / segments
                val r1 = Math.toRadians(a1.toDouble())
                val r2 = Math.toRadians(a2.toDouble())

                shapeRenderer.line(
                    cx + (radius * cos(r1)).toFloat(),
                    cy + (radius * sin(r1)).toFloat(),
                    cx + (radius * cos(r2)).toFloat(),
                    cy + (radius * sin(r2)).toFloat()
                )
            }
        }
    }

    // ─── Arrow Projectile ───────────────────────────────────────────────────────

    private class ArrowEffect(
        val fromX: Float, val fromY: Float,
        val toX: Float, val toY: Float
    ) : Effect(duration = 0.35f, blocksPlayback = true) {

        private val direction = Vector2(toX - fromX, toY - fromY).nor()
        private val length = Vector2(toX - fromX, toY - fromY).len()

        override fun renderFilled(shapeRenderer: ShapeRenderer) {
            val t = smoothStep(progress)
            val currentX = MathUtils.lerp(fromX, toX, t)
            val currentY = MathUtils.lerp(fromY, toY, t)

            // Arrow head (triangle)
            val angle = atan2((toY - fromY).toDouble(), (toX - fromX).toDouble())
            val headSize = 6f

            val tipX = currentX + (headSize * cos(angle)).toFloat()
            val tipY = currentY + (headSize * sin(angle)).toFloat()
            val leftX = currentX + (headSize * cos(angle + 2.5)).toFloat()
            val leftY = currentY + (headSize * sin(angle + 2.5)).toFloat()
            val rightX = currentX + (headSize * cos(angle - 2.5)).toFloat()
            val rightY = currentY + (headSize * sin(angle - 2.5)).toFloat()

            shapeRenderer.color = Color(0.9f, 0.9f, 0.9f, 1f)
            shapeRenderer.triangle(tipX, tipY, leftX, leftY, rightX, rightY)

            // Arrow shaft (series of dots trailing behind)
            val shaftLength = 14f
            for (i in 1..6) {
                val offset = i * (shaftLength / 6f)
                val sx = currentX - (offset * cos(angle)).toFloat()
                val sy = currentY - (offset * sin(angle)).toFloat()
                val alpha = 1f - i * 0.12f
                shapeRenderer.color = Color(0.8f, 0.6f, 0.1f, alpha)
                shapeRenderer.circle(sx, sy, 1.5f)
            }

            // Trail particles
            if (progress > 0.1f) {
                val trailAlpha = (1f - progress) * 0.5f
                shapeRenderer.color = Color(1f, 0.8f, 0.3f, trailAlpha)
                val trailX = MathUtils.lerp(fromX, toX, t - 0.1f)
                val trailY = MathUtils.lerp(fromY, toY, t - 0.1f)
                shapeRenderer.circle(trailX, trailY, 2f)
            }
        }
    }

    // ─── Heal Glow ──────────────────────────────────────────────────────────────

    private class HealEffect(
        val cx: Float, val cy: Float
    ) : Effect(duration = 0.5f) {

        override fun renderFilled(shapeRenderer: ShapeRenderer) {
            val alpha = (1f - progress) * 0.7f

            // Expanding ring (filled with low alpha)
            val ringRadius = 8f + progress * 24f
            shapeRenderer.color = Color(0.2f, 1f, 0.4f, alpha * 0.3f)
            shapeRenderer.circle(cx, cy, ringRadius)

            // Inner glow
            val innerRadius = 6f + progress * 10f
            shapeRenderer.color = Color(0.4f, 1f, 0.6f, alpha * 0.5f)
            shapeRenderer.circle(cx, cy, innerRadius)

            // Rising sparkles
            val numSparkles = 6
            for (i in 0 until numSparkles) {
                val angle = (360f / numSparkles) * i + progress * 120f
                val rad = Math.toRadians(angle.toDouble())
                val dist = 10f + progress * 18f
                val sparkleX = cx + (dist * cos(rad)).toFloat()
                val sparkleY = cy + (dist * sin(rad)).toFloat() + progress * 12f

                val sparkleAlpha = alpha * (0.5f + 0.5f * sin((progress * PI * 3 + i).toFloat()))
                shapeRenderer.color = Color(0.6f, 1f, 0.7f, sparkleAlpha)
                shapeRenderer.circle(sparkleX, sparkleY, 2f * (1f - progress * 0.5f))
            }

            // Cross/plus symbol in center
            if (progress < 0.6f) {
                val crossAlpha = (1f - progress / 0.6f) * 0.9f
                shapeRenderer.color = Color(1f, 1f, 1f, crossAlpha)
                val crossSize = 5f * (1f - progress * 0.5f)
                shapeRenderer.rect(cx - 1f, cy - crossSize, 2f, crossSize * 2)
                shapeRenderer.rect(cx - crossSize, cy - 1f, crossSize * 2, 2f)
            }
        }

        override fun renderLine(shapeRenderer: ShapeRenderer) {
            val alpha = (1f - progress) * 0.6f
            val ringRadius = 8f + progress * 24f

            shapeRenderer.color = Color(0.3f, 1f, 0.5f, alpha)
            val segments = 24
            for (i in 0 until segments) {
                val a1 = 360f * i / segments
                val a2 = 360f * (i + 1) / segments
                val r1 = Math.toRadians(a1.toDouble())
                val r2 = Math.toRadians(a2.toDouble())
                shapeRenderer.line(
                    cx + (ringRadius * cos(r1)).toFloat(),
                    cy + (ringRadius * sin(r1)).toFloat(),
                    cx + (ringRadius * cos(r2)).toFloat(),
                    cy + (ringRadius * sin(r2)).toFloat()
                )
            }
        }
    }

    // ─── Utility ────────────────────────────────────────────────────────────────

    companion object {
        private fun smoothStep(t: Float): Float = t * t * (3f - 2f * t)
    }
}

