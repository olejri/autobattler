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
    private var warriorSilverTexture: Texture? = null
    private var warriorGoldTexture: Texture? = null
    private var archerTexture: Texture? = null
    private var archerSilverTexture: Texture? = null
    private var archerGoldTexture: Texture? = null
    private var clericTexture: Texture? = null
    private var clericSilverTexture: Texture? = null
    private var clericGoldTexture: Texture? = null
    private var knightTexture: Texture? = null
    private var knightSilverTexture: Texture? = null
    private var knightGoldTexture: Texture? = null
    private var wizardTexture: Texture? = null
    private var wizardSilverTexture: Texture? = null
    private var wizardGoldTexture: Texture? = null
    private var griffinRiderTexture: Texture? = null
    private var griffinRiderSilverTexture: Texture? = null
    private var griffinRiderGoldTexture: Texture? = null
    private var bearTexture: Texture? = null
    private var wolfTexture: Texture? = null
    private var commanderTexture: Texture? = null
    private var farmerTexture: Texture? = null
    private var farmerSilverTexture: Texture? = null
    private var farmerGoldTexture: Texture? = null
    private var farmTexture: Texture? = null
    private var goblinTexture: Texture? = null
    private var skeletonTexture: Texture? = null
    private var darkMageTexture: Texture? = null
    private var trollTexture: Texture? = null
    private var banditTexture: Texture? = null
    private var orcWarriorTexture: Texture? = null
    private var spiderTexture: Texture? = null
    private var dragonTexture: Texture? = null

    fun getTexture(type: UnitType): Texture = when (type) {
        is NormalUnitType.Melee              -> warriorTexture           ?: generateWarrior().also { warriorTexture = it }
        is NormalUnitType.MeleeSilver        -> warriorSilverTexture     ?: generateWarrior(UnitTier.SILVER).also { warriorSilverTexture = it }
        is NormalUnitType.MeleeGold          -> warriorGoldTexture       ?: generateWarrior(UnitTier.GOLD).also { warriorGoldTexture = it }
        is NormalUnitType.Ranged             -> archerTexture            ?: generateArcher().also { archerTexture = it }
        is NormalUnitType.RangedSilver       -> archerSilverTexture      ?: generateArcher(UnitTier.SILVER).also { archerSilverTexture = it }
        is NormalUnitType.RangedGold         -> archerGoldTexture        ?: generateArcher(UnitTier.GOLD).also { archerGoldTexture = it }
        is NormalUnitType.Healer             -> clericTexture            ?: generateCleric().also { clericTexture = it }
        is NormalUnitType.HealerSilver       -> clericSilverTexture      ?: generateCleric(UnitTier.SILVER).also { clericSilverTexture = it }
        is NormalUnitType.HealerGold         -> clericGoldTexture        ?: generateCleric(UnitTier.GOLD).also { clericGoldTexture = it }
        is NormalUnitType.Knight             -> knightTexture            ?: generateKnight().also { knightTexture = it }
        is NormalUnitType.KnightSilver       -> knightSilverTexture      ?: generateKnight(UnitTier.SILVER).also { knightSilverTexture = it }
        is NormalUnitType.KnightGold         -> knightGoldTexture        ?: generateKnight(UnitTier.GOLD).also { knightGoldTexture = it }
        is NormalUnitType.GriffinRider       -> griffinRiderTexture      ?: generateGriffinRider().also { griffinRiderTexture = it }
        is NormalUnitType.GriffinRiderSilver -> griffinRiderSilverTexture ?: generateGriffinRider(UnitTier.SILVER).also { griffinRiderSilverTexture = it }
        is NormalUnitType.GriffinRiderGold   -> griffinRiderGoldTexture  ?: generateGriffinRider(UnitTier.GOLD).also { griffinRiderGoldTexture = it }
        is MonsterUnitType.Bear        -> bearTexture       ?: generateBear().also       { bearTexture = it }
        is MonsterUnitType.Wolf        -> wolfTexture       ?: generateWolf().also       { wolfTexture = it }
        is MonsterUnitType.Goblin      -> goblinTexture     ?: generateGoblin().also     { goblinTexture = it }
        is MonsterUnitType.Skeleton    -> skeletonTexture   ?: generateSkeleton().also   { skeletonTexture = it }
        is MonsterUnitType.DarkMage    -> darkMageTexture   ?: generateDarkMage().also   { darkMageTexture = it }
        is MonsterUnitType.Troll       -> trollTexture      ?: generateTroll().also      { trollTexture = it }
        is MonsterUnitType.Bandit      -> banditTexture     ?: generateBandit().also     { banditTexture = it }
        is MonsterUnitType.OrcWarrior  -> orcWarriorTexture ?: generateOrcWarrior().also { orcWarriorTexture = it }
        is MonsterUnitType.Spider      -> spiderTexture     ?: generateSpider().also     { spiderTexture = it }
        is MonsterUnitType.Dragon      -> dragonTexture     ?: generateDragon().also     { dragonTexture = it }
        is SpecialUnitType.Farmer      -> farmerTexture       ?: generateFarmer().also { farmerTexture = it }
        is SpecialUnitType.FarmerSilver -> farmerSilverTexture ?: generateFarmer(UnitTier.SILVER).also { farmerSilverTexture = it }
        is SpecialUnitType.FarmerGold -> farmerGoldTexture   ?: generateFarmer(UnitTier.GOLD).also { farmerGoldTexture = it }
        is SpecialUnitType.Wizard     -> wizardTexture       ?: generateWizard().also { wizardTexture = it }
        is SpecialUnitType.WizardSilver -> wizardSilverTexture ?: generateWizard(UnitTier.SILVER).also { wizardSilverTexture = it }
        is SpecialUnitType.WizardGold -> wizardGoldTexture   ?: generateWizard(UnitTier.GOLD).also { wizardGoldTexture = it }
        is StructureUnitType.Farm      -> farmTexture       ?: generateFarm().also       { farmTexture = it }
    }

    fun getCommanderTexture(): Texture =
        commanderTexture ?: generateCommander().also { commanderTexture = it }

    fun dispose() {
        warriorTexture?.dispose(); warriorSilverTexture?.dispose(); warriorGoldTexture?.dispose()
        archerTexture?.dispose(); archerSilverTexture?.dispose(); archerGoldTexture?.dispose()
        clericTexture?.dispose(); clericSilverTexture?.dispose(); clericGoldTexture?.dispose()
        knightTexture?.dispose(); knightSilverTexture?.dispose(); knightGoldTexture?.dispose()
        wizardTexture?.dispose(); wizardSilverTexture?.dispose(); wizardGoldTexture?.dispose()
        griffinRiderTexture?.dispose(); griffinRiderSilverTexture?.dispose(); griffinRiderGoldTexture?.dispose()
        bearTexture?.dispose(); wolfTexture?.dispose(); commanderTexture?.dispose()
        farmerTexture?.dispose(); farmerSilverTexture?.dispose(); farmerGoldTexture?.dispose(); farmTexture?.dispose()
        goblinTexture?.dispose(); skeletonTexture?.dispose(); darkMageTexture?.dispose()
        trollTexture?.dispose(); banditTexture?.dispose(); orcWarriorTexture?.dispose()
        spiderTexture?.dispose(); dragonTexture?.dispose()
        warriorTexture = null; warriorSilverTexture = null; warriorGoldTexture = null
        archerTexture = null; archerSilverTexture = null; archerGoldTexture = null
        clericTexture = null; clericSilverTexture = null; clericGoldTexture = null
        knightTexture = null; knightSilverTexture = null; knightGoldTexture = null
        wizardTexture = null; wizardSilverTexture = null; wizardGoldTexture = null
        griffinRiderTexture = null; griffinRiderSilverTexture = null; griffinRiderGoldTexture = null
        bearTexture = null; wolfTexture = null; commanderTexture = null
        farmerTexture = null; farmerSilverTexture = null; farmerGoldTexture = null; farmTexture = null
        goblinTexture = null; skeletonTexture = null; darkMageTexture = null
        trollTexture = null; banditTexture = null; orcWarriorTexture = null
        spiderTexture = null; dragonTexture = null
    }

    // ─── Warrior: Shield + Sword ────────────────────────────────────────────────

    private fun generateWarrior(tier: UnitTier = UnitTier.BRONZE): Texture {
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
        pixmap.setColor(tierRingColor(tier, Color(0.3f, 0.62f, 1f, 1f)))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)

        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return texture
    }

    // ─── Archer: Bow + Arrow ────────────────────────────────────────────────────

    private fun generateArcher(tier: UnitTier = UnitTier.BRONZE): Texture {
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
        pixmap.setColor(tierRingColor(tier, Color(0f, 0.83f, 0.83f, 1f)))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)

        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return texture
    }

    // ─── Cleric: Staff + Holy Symbol ────────────────────────────────────────────

    private fun generateCleric(tier: UnitTier = UnitTier.BRONZE): Texture {
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
        pixmap.setColor(tierRingColor(tier, Color(0.25f, 0.88f, 0.3f, 1f)))
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


    // ─── Wolf: sleek predator token ─────────────────────────────────────────────

    private fun generateWolf(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)

        // Background — dark forest green
        pixmap.setColor(Color(0.08f, 0.1f, 0.07f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)

        // Wolf body (grey oval)
        pixmap.setColor(Color(0.55f, 0.55f, 0.55f, 1f))
        pixmap.fillCircle(CENTER, CENTER + 4, 16)

        // Wolf head
        pixmap.setColor(Color(0.6f, 0.6f, 0.6f, 1f))
        pixmap.fillCircle(CENTER, CENTER - 10, 13)

        // Pointed ears
        pixmap.setColor(Color(0.5f, 0.5f, 0.5f, 1f))
        pixmap.fillTriangle(CENTER - 10, CENTER - 16, CENTER - 4, CENTER - 26, CENTER - 2, CENTER - 16)
        pixmap.fillTriangle(CENTER + 2,  CENTER - 16, CENTER + 10, CENTER - 26, CENTER + 4, CENTER - 16)

        // Inner ears
        pixmap.setColor(Color(0.75f, 0.5f, 0.5f, 1f))
        pixmap.fillTriangle(CENTER - 8, CENTER - 17, CENTER - 5, CENTER - 23, CENTER - 3, CENTER - 17)
        pixmap.fillTriangle(CENTER + 3, CENTER - 17, CENTER + 7, CENTER - 23, CENTER + 6, CENTER - 17)

        // Snout (lighter grey)
        pixmap.setColor(Color(0.72f, 0.72f, 0.68f, 1f))
        pixmap.fillCircle(CENTER, CENTER - 6, 6)

        // Nose (dark)
        pixmap.setColor(Color(0.15f, 0.1f, 0.1f, 1f))
        pixmap.fillCircle(CENTER, CENTER - 9, 3)

        // Eyes (amber/yellow — predator)
        pixmap.setColor(Color(0.9f, 0.7f, 0.1f, 1f))
        pixmap.fillCircle(CENTER - 5, CENTER - 14, 3)
        pixmap.fillCircle(CENTER + 5, CENTER - 14, 3)
        // Pupils
        pixmap.setColor(Color(0.05f, 0.05f, 0.05f, 1f))
        pixmap.fillCircle(CENTER - 5, CENTER - 14, 1)
        pixmap.fillCircle(CENTER + 5, CENTER - 14, 1)

        // Tail (curved arc to the side)
        pixmap.setColor(Color(0.5f, 0.5f, 0.5f, 1f))
        drawThickLine(pixmap, CENTER + 12, CENTER + 8, CENTER + 22, CENTER - 4, 3)
        drawThickLine(pixmap, CENTER + 22, CENTER - 4, CENTER + 20, CENTER - 12, 3)

        // Outer ring — amber/yellow (natural, wild feel)
        pixmap.setColor(Color(0.75f, 0.6f, 0.1f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)

        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return texture
    }

    // ─── Farmer: straw hat + pitchfork ──────────────────────────────────────────

    private fun generateFarmer(tier: UnitTier = UnitTier.BRONZE): Texture {
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
        pixmap.setColor(tierRingColor(tier, Color(0.45f, 0.72f, 0.2f, 1f)))
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
                    drawThickLine(pixmap, col, row + 2, col, row - 2, 2)
                    pixmap.fillCircle(col, row - 2, 2)
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
        drawThickLine(pixmap, 6, 6, SIZE - 6, 6, 2)
        drawThickLine(pixmap, 6, SIZE - 6, SIZE - 6, SIZE - 6, 2)
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

    private fun generateKnight(tier: UnitTier = UnitTier.BRONZE): Texture {
        val size = 128
        val center = size / 2
        val radius = 56
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)

        pixmap.setColor(Color(0.08f, 0.1f, 0.18f, 1f))
        pixmap.fillCircle(center, center, radius)

        pixmap.setColor(Color(0.38f, 0.45f, 0.55f, 1f))
        pixmap.fillCircle(center, center + 18, 26)
        pixmap.setColor(Color(0.25f, 0.28f, 0.34f, 1f))
        pixmap.fillCircle(center - 22, center + 12, 15)
        pixmap.fillCircle(center + 22, center + 12, 15)

        pixmap.setColor(Color(0.72f, 0.75f, 0.8f, 1f))
        pixmap.fillCircle(center, center - 8, 18)
        drawThickLine(pixmap, center - 18, center + 10, center + 18, center + 10, 6)
        drawThickLine(pixmap, center, center - 24, center, center + 16, 5)

        pixmap.setColor(Color(0.2f, 0.35f, 0.7f, 1f))
        fillShield(pixmap, center - 24, center + 8, 16, 22)
        pixmap.setColor(Color(0.8f, 0.85f, 0.95f, 1f))
        drawShieldOutline(pixmap, center - 24, center + 8, 16, 22)

        pixmap.setColor(Color(0.75f, 0.6f, 0.2f, 1f))
        drawThickLine(pixmap, center + 10, center + 30, center + 34, center - 28, 4)
        pixmap.setColor(Color(0.9f, 0.9f, 0.95f, 1f))
        pixmap.fillTriangle(center + 34, center - 34, center + 24, center - 18, center + 40, center - 14)

        pixmap.setColor(Color(0.45f, 0.52f, 0.66f, 1f))
        drawCircleOutline(pixmap, center, center, radius, 3)
        if (tier != UnitTier.BRONZE) {
            pixmap.setColor(tierRingColor(tier, Color.WHITE))
            drawCircleOutline(pixmap, center, center, radius + 4, 3)
        }

        return textureFrom(pixmap)
    }

    private fun generateWizard(tier: UnitTier = UnitTier.BRONZE): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)

        pixmap.setColor(Color(0.12f, 0.06f, 0.2f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)

        pixmap.setColor(Color(0.45f, 0.18f, 0.7f, 1f))
        fillShield(pixmap, CENTER, CENTER + 7, 14, 18)
        pixmap.setColor(Color(0.3f, 0.1f, 0.5f, 1f))
        pixmap.fillTriangle(CENTER - 10, CENTER - 2, CENTER + 10, CENTER - 2, CENTER, CENTER - 22)

        pixmap.setColor(Color(0.7f, 0.5f, 0.18f, 1f))
        drawThickLine(pixmap, CENTER + 10, CENTER + 18, CENTER + 6, CENTER - 12, 2)
        pixmap.setColor(Color(0.55f, 0.8f, 1f, 1f))
        pixmap.fillCircle(CENTER + 6, CENTER - 15, 4)

        pixmap.setColor(Color(1f, 0.95f, 0.4f, 1f))
        pixmap.fillCircle(CENTER - 11, CENTER - 6, 1)
        pixmap.fillCircle(CENTER + 13, CENTER - 2, 1)
        pixmap.fillCircle(CENTER - 3, CENTER + 11, 1)
        pixmap.fillCircle(CENTER + 4, CENTER + 5, 1)

        pixmap.setColor(tierRingColor(tier, Color(0.72f, 0.28f, 1f, 1f)))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)

        return textureFrom(pixmap)
    }

    private fun generateGriffinRider(tier: UnitTier = UnitTier.BRONZE): Texture {
        val size = 128
        val center = size / 2
        val radius = 56
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)

        pixmap.setColor(Color(0.12f, 0.09f, 0.05f, 1f))
        pixmap.fillCircle(center, center, radius)

        pixmap.setColor(Color(0.72f, 0.56f, 0.22f, 1f))
        pixmap.fillCircle(center, center + 18, 26)
        pixmap.fillTriangle(center - 12, center + 4, center + 12, center + 4, center, center - 20)

        pixmap.setColor(Color(0.9f, 0.82f, 0.45f, 1f))
        pixmap.fillTriangle(center - 8, center + 6, center - 40, center - 18, center - 18, center + 18)
        pixmap.fillTriangle(center + 8, center + 6, center + 40, center - 18, center + 18, center + 18)
        drawThickLine(pixmap, center - 20, center + 14, center - 40, center - 8, 3)
        drawThickLine(pixmap, center + 20, center + 14, center + 40, center - 8, 3)

        pixmap.setColor(Color(0.82f, 0.72f, 0.3f, 1f))
        pixmap.fillCircle(center, center - 10, 16)
        pixmap.setColor(Color(0.9f, 0.75f, 0.18f, 1f))
        pixmap.fillTriangle(center, center - 24, center - 6, center - 10, center + 6, center - 10)

        pixmap.setColor(Color(0.28f, 0.38f, 0.7f, 1f))
        drawThickLine(pixmap, center - 8, center - 8, center + 10, center - 8, 4)
        drawThickLine(pixmap, center + 6, center - 8, center + 20, center + 16, 3)
        pixmap.setColor(Color(0.8f, 0.85f, 0.95f, 1f))
        pixmap.fillCircle(center - 4, center - 12, 5)

        pixmap.setColor(tierRingColor(tier, Color(0.95f, 0.74f, 0.2f, 1f)))
        drawCircleOutline(pixmap, center, center, radius, 3)

        return textureFrom(pixmap)
    }

    // ─── Drawing Helpers ────────────────────────────────────────────────────────

    private fun generateGoblin(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)
        // Lime-green bg
        pixmap.setColor(Color(0.05f, 0.15f, 0.05f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)
        // Goblin head (small, pointy-eared)
        pixmap.setColor(Color(0.25f, 0.65f, 0.15f, 1f))
        pixmap.fillCircle(CENTER, CENTER - 2, 14)
        // Pointy ears
        pixmap.fillTriangle(CENTER - 14, CENTER - 2, CENTER - 20, CENTER - 12, CENTER - 8, CENTER - 10)
        pixmap.fillTriangle(CENTER + 14, CENTER - 2, CENTER + 20, CENTER - 12, CENTER + 8, CENTER - 10)
        // Eyes (yellow, beady)
        pixmap.setColor(Color(1f, 0.9f, 0f, 1f))
        pixmap.fillCircle(CENTER - 5, CENTER - 4, 3)
        pixmap.fillCircle(CENTER + 5, CENTER - 4, 3)
        pixmap.setColor(Color(0f, 0f, 0f, 1f))
        pixmap.fillCircle(CENTER - 5, CENTER - 4, 1)
        pixmap.fillCircle(CENTER + 5, CENTER - 4, 1)
        // Dagger (small)
        pixmap.setColor(Color(0.8f, 0.8f, 0.8f, 1f))
        drawThickLine(pixmap, CENTER, CENTER + 4, CENTER, CENTER + 18, 2)
        // Ring — acid green
        pixmap.setColor(Color(0.3f, 0.9f, 0.1f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)
        val texture = Texture(pixmap); texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); pixmap.dispose(); return texture
    }

    private fun generateSkeleton(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)
        // Dark bg
        pixmap.setColor(Color(0.08f, 0.08f, 0.12f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)
        // Skull (bone white)
        pixmap.setColor(Color(0.88f, 0.88f, 0.82f, 1f))
        pixmap.fillCircle(CENTER, CENTER - 4, 14)
        // Eye sockets (dark hollows)
        pixmap.setColor(Color(0.05f, 0.05f, 0.1f, 1f))
        pixmap.fillCircle(CENTER - 5, CENTER - 6, 4)
        pixmap.fillCircle(CENTER + 5, CENTER - 6, 4)
        // Nasal cavity
        pixmap.fillTriangle(CENTER - 2, CENTER - 1, CENTER + 2, CENTER - 1, CENTER, CENTER + 3)
        // Jaw/teeth (white lines)
        pixmap.setColor(Color(0.88f, 0.88f, 0.82f, 1f))
        drawThickLine(pixmap, CENTER - 8, CENTER + 6, CENTER + 8, CENTER + 6, 2)
        for (tx in intArrayOf(CENTER - 7, CENTER - 3, CENTER + 1, CENTER + 5)) {
            drawThickLine(pixmap, tx, CENTER + 6, tx, CENTER + 11, 2)
        }
        // Ribcage lines (below)
        pixmap.setColor(Color(0.7f, 0.7f, 0.65f, 1f))
        drawThickLine(pixmap, CENTER - 6, CENTER + 14, CENTER + 6, CENTER + 14, 2)
        drawThickLine(pixmap, CENTER - 6, CENTER + 18, CENTER + 6, CENTER + 18, 2)
        // Ring — bone white
        pixmap.setColor(Color(0.75f, 0.75f, 0.7f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)
        val texture = Texture(pixmap); texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); pixmap.dispose(); return texture
    }

    private fun generateDarkMage(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)
        // Deep purple bg
        pixmap.setColor(Color(0.1f, 0.04f, 0.2f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)
        // Cloak (dark purple)
        pixmap.setColor(Color(0.28f, 0.1f, 0.45f, 1f))
        fillShield(pixmap, CENTER, CENTER + 6, 14, 16)
        // Hood shadow
        pixmap.setColor(Color(0.15f, 0.06f, 0.28f, 1f))
        pixmap.fillCircle(CENTER, CENTER - 4, 13)
        // Glowing eyes (red)
        pixmap.setColor(Color(1f, 0.1f, 0.1f, 1f))
        pixmap.fillCircle(CENTER - 5, CENTER - 5, 3)
        pixmap.fillCircle(CENTER + 5, CENTER - 5, 3)
        // Staff (left hand)
        pixmap.setColor(Color(0.55f, 0.3f, 0.7f, 1f))
        drawThickLine(pixmap, CENTER - 8, CENTER + 20, CENTER - 4, CENTER - 6, 2)
        // Orb at staff tip
        pixmap.setColor(Color(0.8f, 0.2f, 1f, 1f))
        pixmap.fillCircle(CENTER - 3, CENTER - 8, 4)
        // Ring — arcane violet
        pixmap.setColor(Color(0.7f, 0.2f, 1f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)
        val texture = Texture(pixmap); texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); pixmap.dispose(); return texture
    }

    private fun generateTroll(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)
        // Muddy brown bg
        pixmap.setColor(Color(0.12f, 0.1f, 0.06f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)
        // Body — bulky grey-green
        pixmap.setColor(Color(0.35f, 0.4f, 0.2f, 1f))
        pixmap.fillCircle(CENTER, CENTER + 4, 18)
        // Head (small on big body)
        pixmap.setColor(Color(0.4f, 0.45f, 0.22f, 1f))
        pixmap.fillCircle(CENTER, CENTER - 8, 11)
        // Small mean eyes
        pixmap.setColor(Color(0.9f, 0.5f, 0.1f, 1f))
        pixmap.fillCircle(CENTER - 4, CENTER - 9, 3)
        pixmap.fillCircle(CENTER + 4, CENTER - 9, 3)
        pixmap.setColor(Color(0f, 0f, 0f, 1f))
        pixmap.fillCircle(CENTER - 4, CENTER - 9, 1)
        pixmap.fillCircle(CENTER + 4, CENTER - 9, 1)
        // Club
        pixmap.setColor(Color(0.45f, 0.3f, 0.15f, 1f))
        drawThickLine(pixmap, CENTER + 8, CENTER + 22, CENTER + 14, CENTER - 4, 5)
        pixmap.fillCircle(CENTER + 15, CENTER - 6, 7)
        // Ring — earthy olive
        pixmap.setColor(Color(0.5f, 0.6f, 0.25f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)
        val texture = Texture(pixmap); texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); pixmap.dispose(); return texture
    }

    private fun generateBandit(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)
        // Dark leather bg
        pixmap.setColor(Color(0.1f, 0.08f, 0.06f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)
        // Leather vest (brown)
        pixmap.setColor(Color(0.45f, 0.28f, 0.12f, 1f))
        fillShield(pixmap, CENTER, CENTER + 4, 12, 14)
        // Hood (dark)
        pixmap.setColor(Color(0.2f, 0.18f, 0.14f, 1f))
        pixmap.fillCircle(CENTER, CENTER - 5, 13)
        // Eye slot (white sliver)
        pixmap.setColor(Color(0.9f, 0.85f, 0.75f, 1f))
        drawThickLine(pixmap, CENTER - 7, CENTER - 6, CENTER + 7, CENTER - 6, 3)
        // Dagger (right hand, at angle)
        pixmap.setColor(Color(0.85f, 0.85f, 0.85f, 1f))
        drawThickLine(pixmap, CENTER + 6, CENTER + 18, CENTER + 14, CENTER, 2)
        pixmap.fillTriangle(CENTER + 15, CENTER - 3, CENTER + 12, CENTER + 2, CENTER + 18, CENTER + 2)
        // Ring — worn copper
        pixmap.setColor(Color(0.7f, 0.5f, 0.2f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)
        val texture = Texture(pixmap); texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); pixmap.dispose(); return texture
    }

    private fun generateOrcWarrior(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)
        // Deep green bg
        pixmap.setColor(Color(0.05f, 0.1f, 0.04f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)
        // Body armour (dark iron)
        pixmap.setColor(Color(0.22f, 0.22f, 0.24f, 1f))
        fillShield(pixmap, CENTER, CENTER + 6, 14, 16)
        // Head (orc green)
        pixmap.setColor(Color(0.2f, 0.55f, 0.18f, 1f))
        pixmap.fillCircle(CENTER, CENTER - 5, 13)
        // Tusks
        pixmap.setColor(Color(0.92f, 0.88f, 0.78f, 1f))
        pixmap.fillTriangle(CENTER - 8, CENTER - 1, CENTER - 12, CENTER + 8, CENTER - 4, CENTER + 3)
        pixmap.fillTriangle(CENTER + 8, CENTER - 1, CENTER + 12, CENTER + 8, CENTER + 4, CENTER + 3)
        // Eyes (red)
        pixmap.setColor(Color(1f, 0.2f, 0.1f, 1f))
        pixmap.fillCircle(CENTER - 4, CENTER - 7, 3)
        pixmap.fillCircle(CENTER + 4, CENTER - 7, 3)
        // Battle axe
        pixmap.setColor(Color(0.6f, 0.6f, 0.65f, 1f))
        drawThickLine(pixmap, CENTER + 10, CENTER + 22, CENTER + 10, CENTER - 4, 3)
        pixmap.fillTriangle(CENTER + 10, CENTER - 6, CENTER + 18, CENTER - 2, CENTER + 18, CENTER + 8)
        // Ring — dark iron
        pixmap.setColor(Color(0.4f, 0.55f, 0.25f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)
        val texture = Texture(pixmap); texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); pixmap.dispose(); return texture
    }

    private fun generateSpider(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)
        // Almost-black bg
        pixmap.setColor(Color(0.06f, 0.04f, 0.08f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)
        // Body — dark abdomen (larger circle, low)
        pixmap.setColor(Color(0.15f, 0.1f, 0.2f, 1f))
        pixmap.fillCircle(CENTER, CENTER + 5, 11)
        // Cephalothorax (smaller circle, high)
        pixmap.setColor(Color(0.25f, 0.18f, 0.32f, 1f))
        pixmap.fillCircle(CENTER, CENTER - 4, 8)
        // Hourglass marking (red)
        pixmap.setColor(Color(0.9f, 0.1f, 0.1f, 1f))
        pixmap.fillTriangle(CENTER - 3, CENTER + 4, CENTER + 3, CENTER + 4, CENTER, CENTER + 8)
        pixmap.fillTriangle(CENTER - 3, CENTER + 13, CENTER + 3, CENTER + 13, CENTER, CENTER + 9)
        // Eyes (4 pairs, tiny)
        pixmap.setColor(Color(1f, 0.9f, 0f, 1f))
        for (ex in intArrayOf(CENTER - 5, CENTER - 2, CENTER + 2, CENTER + 5)) {
            pixmap.fillCircle(ex, CENTER - 5, 2)
        }
        // Legs (8 lines — 4 per side)
        pixmap.setColor(Color(0.4f, 0.3f, 0.5f, 1f))
        for (i in 0..3) {
            val legY = CENTER - 2 + i * 4
            val spread = (i + 2) * 3
            drawThickLine(pixmap, CENTER - 9, legY, CENTER - 9 - spread, legY - 5, 1)
            drawThickLine(pixmap, CENTER + 9, legY, CENTER + 9 + spread, legY - 5, 1)
        }
        // Ring — venomous purple
        pixmap.setColor(Color(0.55f, 0.2f, 0.8f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)
        val texture = Texture(pixmap); texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); pixmap.dispose(); return texture
    }

    private fun generateDragon(): Texture {
        val pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)
        // Infernal red-black bg
        pixmap.setColor(Color(0.15f, 0.04f, 0.04f, 1f))
        pixmap.fillCircle(CENTER, CENTER, RADIUS)
        // Dragon body (large dark red shape)
        pixmap.setColor(Color(0.55f, 0.12f, 0.08f, 1f))
        pixmap.fillCircle(CENTER, CENTER + 2, 18)
        // Head (reptile, pointed snout)
        pixmap.setColor(Color(0.7f, 0.18f, 0.1f, 1f))
        pixmap.fillCircle(CENTER, CENTER - 8, 11)
        pixmap.fillTriangle(CENTER - 6, CENTER - 14, CENTER + 6, CENTER - 14, CENTER, CENTER - 22)
        // Dragon eyes (glowing amber)
        pixmap.setColor(Color(1f, 0.75f, 0f, 1f))
        pixmap.fillCircle(CENTER - 5, CENTER - 10, 4)
        pixmap.fillCircle(CENTER + 5, CENTER - 10, 4)
        pixmap.setColor(Color(0f, 0f, 0f, 1f))
        pixmap.fillCircle(CENTER - 5, CENTER - 10, 2)
        pixmap.fillCircle(CENTER + 5, CENTER - 10, 2)
        // Wing hints (dark triangular shapes)
        pixmap.setColor(Color(0.35f, 0.08f, 0.05f, 1f))
        pixmap.fillTriangle(CENTER - 14, CENTER - 4, CENTER - 26, CENTER - 18, CENTER - 4, CENTER + 2)
        pixmap.fillTriangle(CENTER + 14, CENTER - 4, CENTER + 26, CENTER - 18, CENTER + 4, CENTER + 2)
        // Flame breath (orange-yellow burst)
        pixmap.setColor(Color(1f, 0.65f, 0.1f, 0.9f))
        pixmap.fillTriangle(CENTER - 2, CENTER - 20, CENTER + 2, CENTER - 20, CENTER, CENTER - 30)
        pixmap.setColor(Color(1f, 0.3f, 0f, 0.8f))
        pixmap.fillCircle(CENTER, CENTER - 24, 3)
        // Ring — dragon gold
        pixmap.setColor(Color(1f, 0.6f, 0.1f, 1f))
        drawCircleOutline(pixmap, CENTER, CENTER, RADIUS, 2)
        val texture = Texture(pixmap); texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); pixmap.dispose(); return texture
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
                if (x in 0 until pixmap.width && y in 0 until pixmap.height) {
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
            if (x in 0 until pixmap.width && y in 0 until pixmap.height) {
                pixmap.drawPixel(x, y)
                if (x + 1 < pixmap.width) pixmap.drawPixel(x + 1, y)
                if (y + 1 < pixmap.height) pixmap.drawPixel(x, y + 1)
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
            if (px in 0 until pixmap.width && py in 0 until pixmap.height) {
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

    private fun textureFrom(pixmap: Pixmap): Texture {
        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return texture
    }

    private fun tierRingColor(tier: UnitTier, bronzeColor: Color): Color = when (tier) {
        UnitTier.BRONZE -> bronzeColor
        UnitTier.SILVER -> Color(0.8f, 0.85f, 0.9f, 1f)
        UnitTier.GOLD -> Color(1f, 0.85f, 0.1f, 1f)
    }
}
