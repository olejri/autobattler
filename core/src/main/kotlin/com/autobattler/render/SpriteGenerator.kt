package com.autobattler.render

import com.autobattler.model.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Generates unit token sprites programmatically at runtime.
 * Each sprite is a 64x64 pixel token drawn using Pixmap.
 *
 * This avoids needing external image files or conversion tools —
 * the SVG files in assets/sprites/ serve as the design reference.
 */
object SpriteGenerator {

    private const val SIZE = 64
    private const val CENTER = SIZE / 2
    private const val RADIUS = 26

    private var warriorTexture: Texture? = null
    private var archerTexture: Texture? = null
    private var clericTexture: Texture? = null
    private var bearTexture: Texture? = null
    private var commanderTexture: Texture? = null
    private var farmerTexture: Texture? = null
    private var farmTexture: Texture? = null

    fun getTexture(type: UnitType): Texture = when (type) {
        is NormalUnitType.Melee   -> warriorTexture ?: generateWarrior().also { warriorTexture = it }
        is NormalUnitType.Ranged  -> archerTexture  ?: generateArcher().also  { archerTexture = it }
        is NormalUnitType.Healer  -> clericTexture  ?: generateCleric().also  { clericTexture = it }
        is MonsterUnitType.Bear   -> bearTexture    ?: generateBear().also    { bearTexture = it }
        is SpecialUnitType.Farmer -> farmerTexture  ?: generateFarmer().also  { farmerTexture = it }
        is StructureUnitType.Farm -> farmTexture    ?: generateFarm().also    { farmTexture = it }
    }

    fun getCommanderTexture(): Texture =
        commanderTexture ?: generateCommander().also { commanderTexture = it }

    fun dispose() {
        warriorTexture?.dispose()
        archerTexture?.dispose()
        clericTexture?.dispose()
        bearTexture?.dispose()
        commanderTexture?.dispose()
        farmerTexture?.dispose()
        farmTexture?.dispose()
        warriorTexture = null; archerTexture = null; clericTexture = null
        bearTexture = null; commanderTexture = null; farmerTexture = null; farmTexture = null
    }

    // ─── Warrior: Shield + Sword ────────────────────────────────────────────────

    private fun generateWarrior(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)

        // Background circle
        pixmap.setColor(Color(0.1f, 0.1f, 0.18f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)

        // Shield shape (filled diamond-ish)
        pixmap.setColor(Color(0.2f, 0.35f, 0.6f, 1f))
        fillShield(pixmap, CENTER, CENTER, 16, 22)

        // Shield border highlight
        pixmap.setColor(Color(0.4f, 0.65f, 1f, 1f))
        drawShieldOutline(pixmap, CENTER, CENTER, 16, 22)

        // Sword (vertical line)
        pixmap.setColor(Color(0.88f, 0.88f, 0.88f, 1f))
        drawThickLine(pixmap, CENTER, 8, CENTER, 54, 2)

        // Crossguard
        drawThickLine(pixmap, CENTER - 8, 24, CENTER + 8, 24, 2)

        // Pommel
        pixmap.setColor(Color(1f, 0.84f, 0f, 1f))
        pixmap.fillCircle(CENTER, 52, 3)

        // Sword tip
        pixmap.setColor(Color(0.9f, 0.9f, 0.9f, 1f))
        pixmap.fillTriangle(CENTER, 6, CENTER - 3, 13, CENTER + 3, 13)

        // Outer ring
        pixmap.setColor(Color(0.3f, 0.62f, 1f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)

        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return texture
    }

    // ─── Archer: Bow + Arrow ────────────────────────────────────────────────────

    private fun generateArcher(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)

        // Background circle
        pixmap.setColor(Color(0.1f, 0.18f, 0.18f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)

        // Bow (arc on left side)
        pixmap.setColor(Color(0.55f, 0.3f, 0.1f, 1f))
        drawBowArc(pixmap, 20, CENTER, 18)

        // Bow string
        pixmap.setColor(Color(0.8f, 0.8f, 0.8f, 1f))
        drawThickLine(pixmap, 20, 14, 20, 50, 1)

        // Arrow shaft
        pixmap.setColor(Color(0.8f, 0.63f, 0.1f, 1f))
        drawThickLine(pixmap, 20, CENTER, 52, CENTER, 2)

        // Arrow head
        pixmap.setColor(Color(0.88f, 0.88f, 0.88f, 1f))
        pixmap.fillTriangle(54, CENTER, 47, CENTER - 4, 47, CENTER + 4)

        // Arrow fletching
        pixmap.setColor(Color(0.8f, 0.15f, 0.15f, 1f))
        pixmap.fillTriangle(20, CENTER, 25, CENTER - 4, 25, CENTER + 4)

        // Outer ring
        pixmap.setColor(Color(0f, 0.83f, 0.83f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)

        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return texture
    }

    // ─── Cleric: Staff + Holy Symbol ────────────────────────────────────────────

    private fun generateCleric(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)

        // Background circle
        pixmap.setColor(Color(0.1f, 0.18f, 0.1f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)

        // Staff
        pixmap.setColor(Color(0.55f, 0.4f, 0.1f, 1f))
        drawThickLine(pixmap, CENTER, 14, CENTER, 54, 3)

        // Holy circle at top of staff
        pixmap.setColor(Color(1f, 0.84f, 0f, 1f))
        drawCircleOutline(pixmap, CENTER, 14, 5, 2)

        // Cross horizontal bar
        drawThickLine(pixmap, CENTER - 7, 22, CENTER + 7, 22, 2)

        // Glow ring
        pixmap.setColor(Color(0.5f, 1f, 0.5f, 0.5f))
        drawCircleOutline(pixmap, CENTER, 18, 10, 1)

        // Heal sparkles (small crosses)
        pixmap.setColor(Color(0.5f, 1f, 0.5f, 0.8f))
        // Left sparkle
        drawThickLine(pixmap, 16, 38, 16, 44, 1)
        drawThickLine(pixmap, 13, 41, 19, 41, 1)
        // Right sparkle
        drawThickLine(pixmap, 48, 36, 48, 42, 1)
        drawThickLine(pixmap, 45, 39, 51, 39, 1)

        // Outer ring
        pixmap.setColor(Color(0.25f, 0.88f, 0.3f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)

        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return texture
    }

    // ─── Bear: Large menacing beast ─────────────────────────────────────────────

    private fun generateBear(): Texture {
        // Bear sprite is 128x128 (for 2x2 cell rendering)
        val size = 128
        val center = size / 2
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)

        // Background
        pixmap.setColor(Color(0.1f, 0.08f, 0.06f, 1f))
        pixmap.fillCircle(center, center, 56)

        // Bear body (large brown oval)
        pixmap.setColor(Color(0.4f, 0.25f, 0.12f, 1f))
        pixmap.fillCircle(center, center + 5, 42)

        // Bear head
        pixmap.setColor(Color(0.45f, 0.3f, 0.15f, 1f))
        pixmap.fillCircle(center, center - 20, 24)

        // Ears
        pixmap.setColor(Color(0.35f, 0.2f, 0.1f, 1f))
        pixmap.fillCircle(center - 18, center - 36, 8)
        pixmap.fillCircle(center + 18, center - 36, 8)

        // Inner ears
        pixmap.setColor(Color(0.55f, 0.35f, 0.2f, 1f))
        pixmap.fillCircle(center - 18, center - 36, 4)
        pixmap.fillCircle(center + 18, center - 36, 4)

        // Snout
        pixmap.setColor(Color(0.5f, 0.35f, 0.2f, 1f))
        pixmap.fillCircle(center, center - 14, 10)

        // Nose
        pixmap.setColor(Color(0.15f, 0.1f, 0.08f, 1f))
        pixmap.fillCircle(center, center - 18, 5)

        // Eyes (angry)
        pixmap.setColor(Color(0.9f, 0.2f, 0.1f, 1f))
        pixmap.fillCircle(center - 10, center - 26, 4)
        pixmap.fillCircle(center + 10, center - 26, 4)

        // Claws at bottom
        pixmap.setColor(Color(0.8f, 0.8f, 0.7f, 1f))
        for (i in -2..2) {
            pixmap.fillCircle(center + i * 8, center + 44, 3)
        }

        // Outer ring (menacing red)
        pixmap.setColor(Color(0.8f, 0.2f, 0.1f, 1f))
        val ringSteps = 200
        for (s in 0 until ringSteps) {
            val angle = 2.0 * Math.PI * s / ringSteps
            for (r in 55..57) {
                val x = center + (r * Math.cos(angle)).toInt()
                val y = center + (r * Math.sin(angle)).toInt()
                if (x in 0 until size && y in 0 until size) pixmap.drawPixel(x, y)
            }
        }

        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return texture
    }

    // ─── Farmer: straw hat + pitchfork ──────────────────────────────────────────

    private fun generateFarmer(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)

        // Background circle — earthy olive
        pixmap.setColor(Color(0.14f, 0.18f, 0.08f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)

        // Straw hat (wide brim + dome)
        pixmap.setColor(Color(0.72f, 0.58f, 0.18f, 1f))
        // Brim (wide horizontal line)
        drawThickLine(pixmap, CENTER - 16, CENTER - 6, CENTER + 16, CENTER - 6, 4)
        // Dome (filled arc above brim)
        pixmap.fillTriangle(CENTER - 10, CENTER - 6, CENTER + 10, CENTER - 6, CENTER, CENTER - 20)
        drawThickLine(pixmap, CENTER - 10, CENTER - 6, CENTER + 10, CENTER - 6, 3)

        // Hat band
        pixmap.setColor(Color(0.5f, 0.35f, 0.1f, 1f))
        drawThickLine(pixmap, CENTER - 10, CENTER - 8, CENTER + 10, CENTER - 8, 2)

        // Pitchfork handle (brown diagonal)
        pixmap.setColor(Color(0.55f, 0.35f, 0.1f, 1f))
        drawThickLine(pixmap, CENTER + 4, CENTER - 2, CENTER + 14, CENTER + 20, 2)

        // Pitchfork tines (2 prongs, silver)
        pixmap.setColor(Color(0.75f, 0.75f, 0.75f, 1f))
        drawThickLine(pixmap, CENTER + 2, CENTER - 2, CENTER + 2, CENTER + 10, 2)
        drawThickLine(pixmap, CENTER + 8, CENTER - 4, CENTER + 8, CENTER + 8, 2)

        // Body (simple tunic shape)
        pixmap.setColor(Color(0.35f, 0.55f, 0.25f, 1f))
        drawThickLine(pixmap, CENTER - 4, CENTER + 2, CENTER - 4, CENTER + 18, 5)

        // Outer ring — warm green
        pixmap.setColor(Color(0.45f, 0.72f, 0.2f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)

        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return texture
    }

    // ─── Farm: planted field structure ──────────────────────────────────────────

    private fun generateFarm(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)

        // Background — soil brown (square, not circle — structures look different)
        pixmap.setColor(Color(0.28f, 0.18f, 0.08f, 1f))
        for (x in 6 until SIZE - 6) for (y in 6 until SIZE - 6) pixmap.drawPixel(x, y)

        // Ploughed furrows (darker horizontal lines across the field)
        pixmap.setColor(Color(0.2f, 0.12f, 0.05f, 1f))
        for (row in intArrayOf(14, 22, 30, 38, 46)) {
            drawThickLine(pixmap, 8, row, SIZE - 8, row, 2)
        }

        // Crop sprouts (small green shoots on each row)
        pixmap.setColor(Color(0.25f, 0.72f, 0.2f, 1f))
        for (row in intArrayOf(12, 20, 28, 36, 44)) {
            for (col in intArrayOf(12, 20, 28, 36, 44, 52)) {
                if (col < SIZE - 6 && row < SIZE - 6) {
                    drawThickLine(pixmap, col, row + 2, col, row - 2, 2)   // stem
                    pixmap.fillCircle(col, row - 2, 2)                      // leaf
                }
            }
        }

        // Gold coin symbol top-right (shows it generates gold)
        pixmap.setColor(Color(1f, 0.84f, 0f, 1f))
        pixmap.fillCircle(SIZE - 12, 12, 6)
        pixmap.setColor(Color(0.7f, 0.55f, 0f, 1f))
        pixmap.fillCircle(SIZE - 12, 12, 4)

        // Border — golden green
        pixmap.setColor(Color(0.6f, 0.82f, 0.2f, 1f))
        // Top + bottom
        drawThickLine(pixmap, 6, 6, SIZE - 6, 6, 2)
        drawThickLine(pixmap, 6, SIZE - 6, SIZE - 6, SIZE - 6, 2)
        // Left + right
        drawThickLine(pixmap, 6, 6, 6, SIZE - 6, 2)
        drawThickLine(pixmap, SIZE - 6, 6, SIZE - 6, SIZE - 6, 2)

        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return texture
    }

    // ─── Commander portrait token ────────────────────────────────────────────────

    private fun generateCommander(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)

        // Deep purple background circle
        pixmap.setColor(Color(0.12f, 0.06f, 0.22f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)

        // Crown — gold
        pixmap.setColor(Color(1f, 0.82f, 0f, 1f))
        // Base bar of crown
        drawThickLine(pixmap, CENTER - 13, CENTER + 7, CENTER + 13, CENTER + 7, 5)
        // Left prong
        drawThickLine(pixmap, CENTER - 10, CENTER - 2, CENTER - 10, CENTER + 7, 4)
        // Center prong (tallest)
        drawThickLine(pixmap, CENTER, CENTER - 13, CENTER, CENTER + 7, 4)
        // Right prong
        drawThickLine(pixmap, CENTER + 10, CENTER - 2, CENTER + 10, CENTER + 7, 4)

        // Crown jewels
        pixmap.setColor(Color(0.85f, 0.1f, 0.1f, 1f))
        pixmap.fillCircle(CENTER - 10, CENTER - 4, 2)
        pixmap.fillCircle(CENTER + 10, CENTER - 4, 2)
        pixmap.setColor(Color(0.2f, 0.5f, 1f, 1f))
        pixmap.fillCircle(CENTER, CENTER - 11, 3)  // sapphire at top

        // Outer ring — gold
        pixmap.setColor(Color(1f, 0.75f, 0f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)

        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return texture
    }

    // ─── Drawing Helpers ────────────────────────────────────────────────────────

    private fun drawThickLine(pixmap: Pixmap, x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int) {
        for (offset in -thickness / 2..thickness / 2) {
            if (x1 == x2) {
                // Vertical line
                pixmap.drawLine(x1 + offset, y1, x2 + offset, y2)
            } else if (y1 == y2) {
                // Horizontal line
                pixmap.drawLine(x1, y1 + offset, x2, y2 + offset)
            } else {
                pixmap.drawLine(x1 + offset, y1, x2 + offset, y2)
                pixmap.drawLine(x1, y1 + offset, x2, y2 + offset)
            }
        }
    }

    private fun drawCircleOutline(pixmap: Pixmap, cx: Int, cy: Int, radius: Int, thickness: Int) {
        for (r in (radius - thickness / 2)..(radius + thickness / 2)) {
            val steps = (r * 8).coerceAtLeast(32)
            for (i in 0 until steps) {
                val angle = 2.0 * Math.PI * i / steps
                val x = cx + (r * Math.cos(angle)).toInt()
                val y = cy + (r * Math.sin(angle)).toInt()
                if (x in 0 until SIZE && y in 0 until SIZE) {
                    pixmap.drawPixel(x, y)
                }
            }
        }
    }

    private fun drawBowArc(pixmap: Pixmap, cx: Int, cy: Int, bowRadius: Int) {
        val steps = 64
        for (i in 0 until steps) {
            val angle = -Math.PI / 2 + Math.PI * i / steps  // -90° to +90° (left-facing arc)
            val x = cx - (bowRadius * Math.cos(angle)).toInt()
            val y = cy + (bowRadius * Math.sin(angle)).toInt()
            if (x in 0 until SIZE && y in 0 until SIZE) {
                pixmap.drawPixel(x, y)
                if (x + 1 < SIZE) pixmap.drawPixel(x + 1, y)
                if (y + 1 < SIZE) pixmap.drawPixel(x, y + 1)
            }
        }
    }

    private fun fillShield(pixmap: Pixmap, cx: Int, cy: Int, halfWidth: Int, halfHeight: Int) {
        // Diamond/shield shape
        for (dy in -halfHeight..halfHeight) {
            val widthAtY = if (dy < 0) {
                (halfWidth * (halfHeight + dy) / halfHeight)
            } else {
                (halfWidth * (halfHeight - dy) / halfHeight)
            }
            for (dx in -widthAtY..widthAtY) {
                val px = cx + dx
                val py = cy + dy
                if (px in 0 until SIZE && py in 0 until SIZE) {
                    pixmap.drawPixel(px, py)
                }
            }
        }
    }

    private fun drawShieldOutline(pixmap: Pixmap, cx: Int, cy: Int, halfWidth: Int, halfHeight: Int) {
        val steps = 100
        // Top-left edge
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = cx - (halfWidth * (1 - t))
            val y = cy - halfHeight + (halfHeight * t)
            pixmap.drawPixel(x.toInt(), y.toInt())
        }
        // Top-right edge
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = cx + (halfWidth * (1 - t))
            val y = cy - halfHeight + (halfHeight * t)
            pixmap.drawPixel(x.toInt(), y.toInt())
        }
        // Bottom-left edge
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = cx - (halfWidth * (1 - t))
            val y = cy + (halfHeight * t)
            pixmap.drawPixel(x.toInt(), y.toInt())
        }
        // Bottom-right edge
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = cx + (halfWidth * (1 - t))
            val y = cy + (halfHeight * t)
            pixmap.drawPixel(x.toInt(), y.toInt())
        }
    }
}
