package com.autobattler.screen

import com.autobattler.AutobattlerGame
import com.autobattler.engine.ScenarioDefinition
import com.autobattler.model.*
import com.autobattler.render.Fonts
import com.autobattler.render.SpriteGenerator
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport

/**
 * Sandbox mode — two-phase setup tool that builds a ScenarioDefinition and
 * hands off entirely to BattleScreen for the actual battle.
 *
 * Phase 1 (MAP_EDITOR):  drag-paint tiles onto a 16×16 all-grass grid.
 * Phase 2 (UNIT_PLACEMENT): drag-drop units onto player/enemy/natural zones, free and all unlocked.
 *
 * On "START BATTLE" a ScenarioDefinition is constructed from the current grid
 * state and passed to BattleScreen.  BattleScreen calls onBattleEnd when the
 * player clicks "Back to Sandbox", returning here with the same setup intact.
 */
class SandboxScreen(private val game: AutobattlerGame) : ScreenAdapter() {

    private enum class SandboxPhase { TERRITORY_EDITOR, MAP_EDITOR, UNIT_PLACEMENT }

    private enum class PlacementTab(val owner: BattleUnit.Owner, val label: String, val color: Color) {
        PLAYER(BattleUnit.Owner.PLAYER,  "PLAYER",  Color(0.2f,  0.6f,  1f,   1f)),
        ENEMY( BattleUnit.Owner.ENEMY,   "ENEMY",   Color(1f,    0.3f,  0.2f, 1f)),
        NATURAL(BattleUnit.Owner.NATURAL,"NATURAL", Color(0.85f, 0.65f, 0.2f, 1f))
    }

    // ─── Grid (persists across phases and back-from-battle) ──────────────────
    private val grid = BattleGrid()
    private var nextUnitId = 1

    // ─── Rendering ───────────────────────────────────────────────────────────
    private val shapeRenderer = ShapeRenderer()
    private val batch = SpriteBatch()
    private lateinit var font: BitmapFont
    private val stage = Stage(ScreenViewport())
    private lateinit var skin: Skin

    // ─── Layout (matches BattleScreen) ────────────────────────────────────────
    private val cellSize    = 48f
    private val gridOffsetX = 220f
    private val gridOffsetY = 185f

    // ─── State ───────────────────────────────────────────────────────────────
    private var phase       = SandboxPhase.TERRITORY_EDITOR
    private var selectedTile = TileType.GRASS
    private var selectedTab  = PlacementTab.PLAYER

    // Territory (per-cell ownership). Initialised to the classic left/right split.
    private val playerTerritory: MutableSet<Position> = (0 until BattleGrid.ENEMY_ZONE_START)
        .flatMap { x -> (0 until BattleGrid.GRID_SIZE).map { y -> Position(x, y) } }
        .toMutableSet()
    private var paintingPlayerSide = true   // which side the brush paints

    // ─── Map editor ──────────────────────────────────────────────────────────
    private var isPainting = false

    // ─── Unit placement drag-and-drop ─────────────────────────────────────────
    private var draggingType: UnitType? = null
    private var dragScreenX = 0f
    private var dragScreenY = 0f
    private var isDragging  = false
    private var hoverGridX  = -1
    private var hoverGridY  = -1

    // ─── Unit panel scroll ────────────────────────────────────────────────────
    private var unitScrollOffset = 0  // how many unit slots are scrolled past the top

    // ─── All placed units (for rendering and scenario extraction) ────────────
    private val allUnitsById = mutableMapOf<Int, BattleUnit>()

    // ─── Tooltip state ───────────────────────────────────────────────────────
    private var tooltipVisible = false
    private var tooltipTitle = ""
    private var tooltipLines = listOf<String>()
    private var tooltipAnchorX = 0f
    private var tooltipAnchorY = 0f

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun show() {
        font = Fonts.create(17, borderWidth = 1f)
        skin = createSkin()
        Gdx.input.inputProcessor = InputMultiplexer(stage, createInputProcessor())
        rebuildUI()
    }

    // ─── Render ──────────────────────────────────────────────────────────────

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.06f, 0.06f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        drawGrid()
        drawUnits()
        if (isDragging) drawDragGhost()
        drawLeftPanel()

        if (tooltipVisible) drawTooltip()

        stage.act(delta)
        stage.draw()
    }

    // ─── Grid ────────────────────────────────────────────────────────────────

    private fun drawGrid() {
        val isTerritory = phase == SandboxPhase.TERRITORY_EDITOR
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (x in 0 until grid.width) {
            for (y in 0 until grid.height) {
                val sx   = gridOffsetX + x * cellSize
                val sy   = gridOffsetY + y * cellSize
                val inPlayer = Position(x, y) in playerTerritory
                if (isTerritory) {
                    // Territory editor: show only ownership, no tile details
                    shapeRenderer.color = if (inPlayer) Color(0.06f, 0.18f, 0.06f, 1f) else Color(0.2f, 0.05f, 0.05f, 1f)
                    shapeRenderer.rect(sx, sy, cellSize, cellSize)
                } else {
                    val tile = grid.getTile(x, y)
                    shapeRenderer.color = getTileColor(tile, inPlayer)
                    shapeRenderer.rect(sx, sy, cellSize, cellSize)
                    drawTileDetail(tile, sx, sy)
                }
            }
        }

        // Territory-editor hover brush
        if (isTerritory && isPainting) {
            val mx = Gdx.input.x; val my = Gdx.graphics.height - Gdx.input.y
            val gx = ((mx - gridOffsetX) / cellSize).toInt()
            val gy = ((my - gridOffsetY) / cellSize).toInt()
            if (gx in 0 until grid.width && gy in 0 until grid.height) {
                shapeRenderer.color = Color(1f, 1f, 1f, 0.2f)
                shapeRenderer.rect(gridOffsetX + gx * cellSize + 1, gridOffsetY + gy * cellSize + 1, cellSize - 2, cellSize - 2)
            }
        }

        // Hover highlight during tile painting
        if (phase == SandboxPhase.MAP_EDITOR && isPainting) {
            val mx = Gdx.input.x
            val my = Gdx.graphics.height - Gdx.input.y
            val gx = ((mx - gridOffsetX) / cellSize).toInt()
            val gy = ((my - gridOffsetY) / cellSize).toInt()
            if (gx in 0 until grid.width && gy in 0 until grid.height) {
                shapeRenderer.color = Color(1f, 1f, 1f, 0.15f)
                shapeRenderer.rect(gridOffsetX + gx * cellSize + 1, gridOffsetY + gy * cellSize + 1, cellSize - 2, cellSize - 2)
            }
        }

        // Hover highlight during unit drag
        if (phase == SandboxPhase.UNIT_PLACEMENT && isDragging && hoverGridX >= 0) {
            val owner   = selectedTab.owner
            val unitSize = draggingType?.size ?: 1
            val inBounds = hoverGridX in 0 until grid.width && hoverGridY in 0 until grid.height
            val valid    = inBounds && isValidZone(hoverGridX, hoverGridY, owner)
                && grid.canUnitFitAt(Position(hoverGridX, hoverGridY), unitSize)
                && grid.getTile(hoverGridX, hoverGridY).placeable
            // Highlight all tiles the unit would occupy
            for (dx in 0 until unitSize) for (dy in 0 until unitSize) {
                val hx = hoverGridX + dx; val hy = hoverGridY + dy
                if (hx in 0 until grid.width && hy in 0 until grid.height) {
                    shapeRenderer.color = if (valid) Color(0.3f, 0.7f, 0.3f, 0.5f) else Color(0.7f, 0.2f, 0.2f, 0.4f)
                    shapeRenderer.rect(gridOffsetX + hx * cellSize + 1, gridOffsetY + hy * cellSize + 1, cellSize - 2, cellSize - 2)
                }
            }
        }
        shapeRenderer.end()

        // Grid lines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color(0.2f, 0.2f, 0.25f, 1f)
        for (x in 0..grid.width) {
            val sx = gridOffsetX + x * cellSize
            shapeRenderer.line(sx, gridOffsetY, sx, gridOffsetY + grid.height * cellSize)
        }
        for (y in 0..grid.height) {
            val sy = gridOffsetY + y * cellSize
            shapeRenderer.line(gridOffsetX, sy, gridOffsetX + grid.width * cellSize, sy)
        }
        // Territory borders — yellow edges where player meets enemy cells
        shapeRenderer.color = Color(0.85f, 0.85f, 0.2f, 0.85f)
        for (x in 0 until grid.width) {
            for (y in 0 until grid.height) {
                if (Position(x, y) !in playerTerritory) continue
                val sx = gridOffsetX + x * cellSize; val sy = gridOffsetY + y * cellSize
                if (Position(x + 1, y) !in playerTerritory) shapeRenderer.line(sx + cellSize, sy, sx + cellSize, sy + cellSize)
                if (Position(x - 1, y) !in playerTerritory) shapeRenderer.line(sx, sy, sx, sy + cellSize)
                if (Position(x, y + 1) !in playerTerritory) shapeRenderer.line(sx, sy + cellSize, sx + cellSize, sy + cellSize)
                if (Position(x, y - 1) !in playerTerritory) shapeRenderer.line(sx, sy, sx + cellSize, sy)
            }
        }
        shapeRenderer.end()
    }

    private fun getTileColor(tile: TileType, playerSide: Boolean): Color = when (tile) {
        TileType.GRASS         -> if (playerSide) Color(0.08f, 0.16f, 0.08f, 1f) else Color(0.16f, 0.08f, 0.08f, 1f)
        TileType.WATER         -> Color(0.08f, 0.15f, 0.35f, 1f)
        TileType.ROCK          -> Color(0.25f, 0.22f, 0.20f, 1f)
        TileType.BRIDGE        -> Color(0.35f, 0.25f, 0.10f, 1f)
        TileType.TREE          -> Color(0.05f, 0.12f, 0.05f, 1f)
        TileType.SAND          -> if (playerSide) Color(0.28f, 0.22f, 0.08f, 1f) else Color(0.32f, 0.2f,  0.06f, 1f)
        TileType.MUD           -> if (playerSide) Color(0.14f, 0.12f, 0.06f, 1f) else Color(0.18f, 0.1f,  0.04f, 1f)
        TileType.SHALLOW_WATER -> Color(0.1f,  0.22f, 0.4f,  1f)
        TileType.FORT          -> Color(0.22f, 0.2f,  0.18f, 1f)
        TileType.LAVA          -> Color(0.5f,  0.1f,  0.02f, 1f)
        TileType.ICE           -> if (playerSide) Color(0.18f, 0.32f, 0.52f, 1f) else Color(0.22f, 0.22f, 0.42f, 1f)
        TileType.RUINS         -> if (playerSide) Color(0.18f, 0.15f, 0.12f, 1f) else Color(0.22f, 0.14f, 0.1f,  1f)
    }

    private fun drawTileDetail(tile: TileType, sx: Float, sy: Float) {
        when (tile) {
            TileType.WATER -> {
                shapeRenderer.color = Color(0.15f, 0.3f, 0.55f, 1f)
                shapeRenderer.rect(sx + 6f, sy + cellSize / 2 - 1f, cellSize * 0.4f, 2f)
            }
            TileType.ROCK -> {
                shapeRenderer.color = Color(0.35f, 0.32f, 0.28f, 1f)
                shapeRenderer.circle(sx + cellSize / 2, sy + cellSize / 2, cellSize * 0.3f)
            }
            TileType.BRIDGE -> {
                shapeRenderer.color = Color(0.5f, 0.35f, 0.15f, 1f)
                for (i in 0..3) {
                    val py = sy + 4f + i * (cellSize - 8f) / 3f
                    shapeRenderer.rect(sx + 3f, py, cellSize - 6f, 3f)
                }
            }
            TileType.TREE -> {
                shapeRenderer.color = Color(0.35f, 0.22f, 0.08f, 1f)
                shapeRenderer.rect(sx + cellSize / 2 - 3f, sy + 2f, 6f, cellSize * 0.4f)
                shapeRenderer.color = Color(0.1f, 0.4f, 0.12f, 1f)
                shapeRenderer.circle(sx + cellSize / 2, sy + cellSize * 0.65f, cellSize * 0.32f)
            }
            TileType.SAND -> {
                shapeRenderer.color = Color(0.45f, 0.36f, 0.14f, 0.6f)
                shapeRenderer.rect(sx + 6f, sy + cellSize * 0.35f, cellSize * 0.55f, 2f)
                shapeRenderer.rect(sx + 10f, sy + cellSize * 0.65f, cellSize * 0.45f, 2f)
            }
            TileType.MUD -> {
                shapeRenderer.color = Color(0.25f, 0.18f, 0.08f, 0.7f)
                shapeRenderer.circle(sx + cellSize * 0.35f, sy + cellSize * 0.4f, cellSize * 0.18f)
                shapeRenderer.circle(sx + cellSize * 0.65f, sy + cellSize * 0.6f, cellSize * 0.14f)
            }
            TileType.SHALLOW_WATER -> {
                shapeRenderer.color = Color(0.2f, 0.45f, 0.65f, 0.7f)
                shapeRenderer.rect(sx + 3f, sy + cellSize / 2 - 1f, cellSize * 0.5f, 2f)
            }
            TileType.FORT -> {
                shapeRenderer.color = Color(0.4f, 0.38f, 0.34f, 1f)
                shapeRenderer.rect(sx + 3f, sy + cellSize / 2 - 1f, cellSize - 6f, 3f)
                shapeRenderer.rect(sx + 3f, sy + cellSize * 0.75f - 1f, cellSize - 6f, 3f)
            }
            TileType.LAVA -> {
                shapeRenderer.color = Color(1f, 0.45f, 0.02f, 0.9f)
                shapeRenderer.circle(sx + cellSize * 0.4f, sy + cellSize * 0.45f, cellSize * 0.22f)
            }
            TileType.ICE -> {
                shapeRenderer.color = Color(0.55f, 0.75f, 0.9f, 0.7f)
                shapeRenderer.rect(sx + 4f, sy + cellSize * 0.3f, cellSize - 8f, 2f)
            }
            TileType.RUINS -> {
                shapeRenderer.color = Color(0.42f, 0.36f, 0.3f, 0.8f)
                shapeRenderer.rect(sx + 4f, sy + 4f, cellSize * 0.3f, cellSize * 0.25f)
                shapeRenderer.rect(sx + cellSize * 0.55f, sy + cellSize * 0.6f, cellSize * 0.35f, cellSize * 0.28f)
            }
            else -> {}
        }
    }

    // ─── Units ───────────────────────────────────────────────────────────────

    private fun drawUnits() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for ((_, unit) in allUnitsById) {
            val uc = unit.size.toFloat()
            val cx = gridOffsetX + unit.position.x * cellSize + cellSize * uc / 2
            val cy = gridOffsetY + unit.position.y * cellSize + cellSize * uc / 2
            val r  = cellSize * uc * 0.42f
            shapeRenderer.color = when (unit.owner) {
                BattleUnit.Owner.PLAYER  -> Color(0.2f, 0.6f, 1f,    0.7f)
                BattleUnit.Owner.ENEMY   -> Color(1f,   0.3f, 0.2f,  0.7f)
                BattleUnit.Owner.NATURAL -> Color(0.6f, 0.45f, 0.1f, 0.7f)
            }
            shapeRenderer.circle(cx, cy, r)
            shapeRenderer.color = Color(0.08f, 0.08f, 0.12f, 1f)
            shapeRenderer.circle(cx, cy, r - 3f)
        }
        shapeRenderer.end()

        batch.begin()
        for ((_, unit) in allUnitsById) {
            val uc = unit.size.toFloat()
            val ss = cellSize * uc - 8f
            val sx = gridOffsetX + unit.position.x * cellSize + (cellSize * uc - ss) / 2
            val sy = gridOffsetY + unit.position.y * cellSize + (cellSize * uc - ss) / 2 - 2f
            batch.setColor(when (unit.owner) {
                BattleUnit.Owner.ENEMY   -> Color(1f,   0.6f, 0.6f, 1f)
                BattleUnit.Owner.NATURAL -> Color(0.9f, 0.8f, 0.5f, 1f)
                else                     -> Color.WHITE
            })
            batch.draw(SpriteGenerator.getTexture(unit.type), sx, sy, ss, ss)
        }
        batch.setColor(Color.WHITE)
        batch.end()
    }

    private fun drawDragGhost() {
        val type = draggingType ?: return
        val ghostSize = cellSize - 4f
        batch.begin()
        batch.setColor(1f, 1f, 1f, 0.75f)
        batch.draw(SpriteGenerator.getTexture(type), dragScreenX - ghostSize / 2, dragScreenY - ghostSize / 2, ghostSize, ghostSize)
        batch.setColor(Color.WHITE)
        batch.end()
    }

    // ─── Left panel ──────────────────────────────────────────────────────────

    private fun drawLeftPanel() {
        val panelW = gridOffsetX
        val screenH = Gdx.graphics.height.toFloat()

        // Panel background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.08f, 0.09f, 0.12f, 1f)
        shapeRenderer.rect(0f, 0f, panelW, screenH)
        shapeRenderer.end()

        when (phase) {
            SandboxPhase.TERRITORY_EDITOR -> drawTerritoryPalette(panelW, screenH)
            SandboxPhase.MAP_EDITOR    -> drawTilePalette(panelW, screenH)
            SandboxPhase.UNIT_PLACEMENT -> drawUnitPanel(panelW, screenH)
        }
    }

    private fun drawTerritoryPalette(panelW: Float, screenH: Float) {
        batch.begin()
        font.color = Color(0.9f, 0.9f, 0.5f, 1f)
        font.draw(batch, "DEFINE TERRITORY", 8f, screenH - 12f)
        font.color = Color(0.6f, 0.6f, 0.6f, 1f)
        font.draw(batch, "Click/drag to paint.", 8f, screenH - 30f)
        batch.end()

        val btnW = panelW - 16f
        val btnH = 46f
        val playerY = screenH - 90f
        val enemyY  = playerY - btnH - 8f
        val resetY  = enemyY - btnH - 8f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        // Player button
        shapeRenderer.color = if (paintingPlayerSide) Color(0.15f, 0.5f, 0.15f, 1f) else Color(0.06f, 0.2f, 0.06f, 1f)
        shapeRenderer.rect(8f, playerY, btnW, btnH)
        // Enemy button
        shapeRenderer.color = if (!paintingPlayerSide) Color(0.55f, 0.12f, 0.12f, 1f) else Color(0.22f, 0.06f, 0.06f, 1f)
        shapeRenderer.rect(8f, enemyY, btnW, btnH)
        // Reset button
        shapeRenderer.color = Color(0.18f, 0.18f, 0.22f, 1f)
        shapeRenderer.rect(8f, resetY, btnW, btnH)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = if (paintingPlayerSide) Color.WHITE else Color(0.3f, 0.6f, 0.3f, 1f)
        shapeRenderer.rect(8f, playerY, btnW, btnH)
        shapeRenderer.color = if (!paintingPlayerSide) Color.WHITE else Color(0.6f, 0.3f, 0.3f, 1f)
        shapeRenderer.rect(8f, enemyY, btnW, btnH)
        shapeRenderer.color = Color(0.4f, 0.4f, 0.5f, 1f)
        shapeRenderer.rect(8f, resetY, btnW, btnH)
        shapeRenderer.end()

        batch.begin()
        font.color = if (paintingPlayerSide) Color.WHITE else Color(0.5f, 0.9f, 0.5f, 1f)
        font.draw(batch, if (paintingPlayerSide) "> PLAYER ZONE" else "PLAYER ZONE", 14f, playerY + btnH - 12f)
        font.color = Color(0.7f, 0.85f, 0.7f, 1f)
        font.draw(batch, "Green side", 14f, playerY + 10f)

        font.color = if (!paintingPlayerSide) Color.WHITE else Color(0.9f, 0.5f, 0.5f, 1f)
        font.draw(batch, if (!paintingPlayerSide) "> ENEMY ZONE" else "ENEMY ZONE", 14f, enemyY + btnH - 12f)
        font.color = Color(0.85f, 0.7f, 0.7f, 1f)
        font.draw(batch, "Red side", 14f, enemyY + 10f)

        font.color = Color(0.7f, 0.7f, 0.75f, 1f)
        font.draw(batch, "RESET DEFAULT", 14f, resetY + btnH - 12f)
        font.draw(batch, "Half/Half split", 14f, resetY + 10f)
        batch.end()
    }

    private fun territoryButtonBounds(screenH: Float): Triple<Float, Float, Float> {
        // Returns (playerY, enemyY, resetY)
        val btnH   = 46f
        val playerY = screenH - 90f
        return Triple(playerY, playerY - btnH - 8f, playerY - (btnH + 8f) * 2)
    }

    private fun drawTilePalette(panelW: Float, screenH: Float) {
        val slotH   = 46f
        val slotW   = panelW - 16f
        val startY  = screenH - 60f
        val tiles   = TileType.entries

        batch.begin()
        font.color = Color(0.9f, 0.9f, 0.5f, 1f)
        font.draw(batch, "TILE PALETTE", 8f, screenH - 12f)
        batch.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for ((i, tile) in tiles.withIndex()) {
            val sy = startY - i * (slotH + 6f)
            val col = getTileColor(tile, true)  // use player-side colour for preview
            shapeRenderer.color = if (tile == selectedTile) Color(col.r + 0.2f, col.g + 0.2f, col.b + 0.2f, 1f) else col
            shapeRenderer.rect(8f, sy, slotW, slotH)
        }
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        for ((i, tile) in tiles.withIndex()) {
            val sy = startY - i * (slotH + 6f)
            shapeRenderer.color = if (tile == selectedTile) Color.YELLOW else Color(0.4f, 0.4f, 0.4f, 1f)
            shapeRenderer.rect(8f, sy, slotW, slotH)
        }
        shapeRenderer.end()

        batch.begin()
        for ((i, tile) in tiles.withIndex()) {
            val sy = startY - i * (slotH + 6f)
            font.color = if (tile == selectedTile) Color.YELLOW else Color.WHITE
            font.draw(batch, tile.displayName, 14f, sy + slotH - 8f)
        }
        batch.end()
    }

    /** Returns the screen-space rect (x, y, w, h) of a tile palette slot. */
    private fun tilePaletteSlotBounds(index: Int): FloatArray {
        val slotH  = 46f
        val startY = Gdx.graphics.height.toFloat() - 60f
        val sy     = startY - index * (slotH + 6f)
        return floatArrayOf(8f, sy, gridOffsetX - 16f, slotH)
    }

    private fun drawUnitPanel(panelW: Float, screenH: Float) {
        // ── Header ───────────────────────────────────────────────────────────
        batch.begin()
        font.color = Color(0.9f, 0.9f, 0.5f, 1f)
        font.draw(batch, "PLACE UNITS", 8f, screenH - 12f)
        batch.end()

        // ── Faction buttons (full-width, stacked) ────────────────────────────
        val btnH = 40f
        val btnW = panelW - 12f
        for ((i, tab) in PlacementTab.entries.withIndex()) {
            val by = factionButtonY(i, screenH)
            val active = tab == selectedTab
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            shapeRenderer.color = if (active) tab.color else Color(tab.color.r * 0.25f, tab.color.g * 0.25f, tab.color.b * 0.25f, 1f)
            shapeRenderer.rect(6f, by, btnW, btnH)
            shapeRenderer.end()
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.color = if (active) Color.WHITE else tab.color
            shapeRenderer.rect(6f, by, btnW, btnH)
            shapeRenderer.end()
            batch.begin()
            font.color = if (active) Color.WHITE else Color(0.7f, 0.7f, 0.7f, 1f)
            val label = if (active) "> ${tab.label}" else tab.label
            font.draw(batch, label, 14f, by + btnH - 11f)
            batch.end()
        }

        // ── Zone hint ────────────────────────────────────────────────────────
        val hintY = factionButtonY(PlacementTab.entries.size - 1, screenH) - 20f
        batch.begin()
        font.color = Color(0.55f, 0.55f, 0.55f, 1f)
        val hint = when (selectedTab) {
            PlacementTab.PLAYER  -> "Drop on LEFT side"
            PlacementTab.ENEMY   -> "Drop on RIGHT side"
            PlacementTab.NATURAL -> "Drop anywhere"
        }
        font.draw(batch, hint, 8f, hintY)
        batch.end()

        // ── Unit slots (only those visible in the panel) ─────────────────────
        val slotH  = 54f
        val slotW  = panelW - 12f
        val units  = UnitType.all
        val maxVisible = maxVisibleSlots(screenH)

        for ((i, type) in units.withIndex()) {
            val sy = unitSlotY(i, screenH)
            if (sy + slotH < 40f || sy > hintY - 4f) continue  // outside visible band

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            shapeRenderer.color = Color(0.12f, 0.16f, 0.12f, 1f)
            shapeRenderer.rect(6f, sy, slotW, slotH)
            shapeRenderer.end()
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.color = Color(0.3f, 0.45f, 0.3f, 1f)
            shapeRenderer.rect(6f, sy, slotW, slotH)
            shapeRenderer.end()
            val iconS = slotH - 8f
            batch.begin()
            batch.setColor(Color.WHITE)
            batch.draw(SpriteGenerator.getTexture(type), 10f, sy + 4f, iconS, iconS)
            font.color = Color.WHITE
            font.draw(batch, type.displayName, 10f + iconS + 6f, sy + slotH - 8f)
            font.color = Color(0.65f, 0.82f, 0.65f, 1f)
            font.draw(batch, "HP:${type.maxHp} ATK:${type.power}", 10f + iconS + 6f, sy + slotH - 24f)
            batch.setColor(Color.WHITE)
            batch.end()
        }

        // ── Scroll arrows (shown when list overflows) ─────────────────────────
        if (units.size > maxVisible) {
            val arrW = slotW / 2f - 2f
            val arrH = 28f
            val arrY = 6f           // bottom of screen
            val canUp   = unitScrollOffset > 0
            val canDown = unitScrollOffset < units.size - maxVisible

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            shapeRenderer.color = if (canUp) Color(0.2f, 0.4f, 0.2f, 1f) else Color(0.1f, 0.1f, 0.1f, 1f)
            shapeRenderer.rect(6f, arrY, arrW, arrH)
            shapeRenderer.color = if (canDown) Color(0.2f, 0.4f, 0.2f, 1f) else Color(0.1f, 0.1f, 0.1f, 1f)
            shapeRenderer.rect(6f + arrW + 4f, arrY, arrW, arrH)
            shapeRenderer.end()

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.color = if (canUp) Color.WHITE else Color(0.3f, 0.3f, 0.3f, 1f)
            shapeRenderer.rect(6f, arrY, arrW, arrH)
            shapeRenderer.color = if (canDown) Color.WHITE else Color(0.3f, 0.3f, 0.3f, 1f)
            shapeRenderer.rect(6f + arrW + 4f, arrY, arrW, arrH)
            shapeRenderer.end()

            batch.begin()
            font.color = if (canUp) Color.WHITE else Color(0.35f, 0.35f, 0.35f, 1f)
            font.draw(batch, "UP", 6f + arrW / 2f - 14f, arrY + arrH - 6f)
            font.color = if (canDown) Color.WHITE else Color(0.35f, 0.35f, 0.35f, 1f)
            font.draw(batch, "DOWN", 6f + arrW + 4f + arrW / 2f - 22f, arrY + arrH - 6f)
            batch.end()
        }
    }

    /** How many unit slots fit in the panel at once. */
    private fun maxVisibleSlots(screenH: Float): Int {
        val btnH = 40f
        val factionBlockH = PlacementTab.entries.size * (btnH + 5f) + 24f
        val listAreaH = screenH - 50f - factionBlockH - 40f  // 40 = scroll-arrow strip + small margin
        val slotH = 54f + 4f
        return maxOf(1, (listAreaH / slotH).toInt())
    }

    private fun factionButtonY(index: Int, screenH: Float): Float {
        val btnH  = 40f
        val gap   = 5f
        val topY  = screenH - 50f
        return topY - index * (btnH + gap)
    }

    private fun unitSlotY(index: Int, screenH: Float): Float {
        val slotH  = 54f
        val gap    = 4f
        val btnH   = 40f
        val factionBlockH = PlacementTab.entries.size * (btnH + 5f) + 24f  // buttons + hint
        val startY = screenH - 50f - factionBlockH
        return startY - (index - unitScrollOffset) * (slotH + gap)
    }

    /** Y of the scroll-arrow button area at the bottom of the panel. */
    private fun scrollButtonsY(screenH: Float): Float {
        val btnH  = 40f
        val factionBlockH = PlacementTab.entries.size * (btnH + 5f) + 24f
        return screenH - 50f - factionBlockH  // same as unitSlotY start — buttons drawn below list
    }

    // ─── Tooltip drawing ─────────────────────────────────────────────────────

    private fun drawTooltip() {
        val padding = 10f
        val lineHeight = 17f
        val titleHeight = 22f
        val boxW = 200f
        val boxH = padding * 2 + titleHeight + (if (tooltipLines.isNotEmpty()) 4f + tooltipLines.size * lineHeight else 0f)

        var bx = tooltipAnchorX + 6f
        var by = tooltipAnchorY + 6f
        val screenW = Gdx.graphics.width.toFloat()
        val screenH = Gdx.graphics.height.toFloat()
        if (bx + boxW > screenW - 4f) bx = tooltipAnchorX - boxW - 6f
        if (by + boxH > screenH - 4f) by = tooltipAnchorY - boxH - 6f
        if (bx < 4f) bx = 4f
        if (by < 4f) by = 4f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.04f, 0.06f, 0.08f, 0.93f)
        shapeRenderer.rect(bx, by, boxW, boxH)
        shapeRenderer.end()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color(0.55f, 0.65f, 0.75f, 0.9f)
        shapeRenderer.rect(bx, by, boxW, boxH)
        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        batch.begin()
        font.color = Color(0.9f, 0.85f, 0.5f, 1f)
        font.draw(batch, tooltipTitle, bx + padding, by + boxH - padding)
        var lineY = by + boxH - padding - titleHeight - 4f
        for (line in tooltipLines) {
            font.color = Color(0.82f, 0.82f, 0.88f, 1f)
            font.draw(batch, line, bx + padding, lineY)
            lineY -= lineHeight
        }
        batch.end()
    }

    private fun showTileTooltip(tile: TileType, anchorX: Float, anchorY: Float) {
        tooltipTitle = tile.displayName
        val lines = mutableListOf<String>()
        if (!tile.passable) {
            lines += "Impassable — blocks movement"
        } else {
            when (tile.moveCost) {
                1    -> lines += "Normal movement"
                2    -> lines += "Slow (×2 move cost)"
                3    -> lines += "Very slow (×3 move cost)"
                else -> lines += "Move cost: ${tile.moveCost}"
            }
        }
        if (!tile.projectilePassable) lines += "Blocks projectiles (cover)"
        if (!tile.placeable && tile.passable) lines += "Cannot place units here"
        tooltipLines = lines
        tooltipAnchorX = anchorX
        tooltipAnchorY = anchorY
        tooltipVisible = true
    }

    private fun showUnitTooltip(unit: BattleUnit, anchorX: Float, anchorY: Float) {
        val ownerLabel = when (unit.owner) {
            BattleUnit.Owner.PLAYER  -> "Player"
            BattleUnit.Owner.ENEMY   -> "Enemy"
            BattleUnit.Owner.NATURAL -> "Natural"
        }
        tooltipTitle = "${unit.type.displayName}  [$ownerLabel]"
        val lines = mutableListOf<String>()
        lines += "HP:     ${unit.type.maxHp}"
        if (unit.type.power > 0)  lines += "Power:  ${unit.type.power}"
        if (unit.type.speed > 0)  lines += "Speed:  ${unit.type.speed}"
        val rangeMin = unit.type.attackRange.first
        val rangeMax = unit.type.attackRange.last
        lines += when {
            rangeMax == 0        -> "Melee only"
            rangeMin == 0        -> "Range: 0–$rangeMax tiles"
            rangeMin == rangeMax -> "Range: $rangeMin tiles"
            else                 -> "Range: $rangeMin–$rangeMax tiles"
        }
        if (unit.type.cooldown > 0) lines += "Cooldown: ${unit.type.cooldown} turns"
        if (unit.type.canFly)       lines += "Flying — ignores terrain"
        if (unit.type.tier != UnitTier.BRONZE) lines += "Tier: ${unit.type.tier.label}"
        if (unit.type is SpecialUnitType) {
            lines += when (val ab = unit.type.ability) {
                is SpecialAbility.None      -> ""
                is SpecialAbility.PlantFarm -> "Ability: Plant Farm"
                is SpecialAbility.YieldGold -> "Ability: Yields ${ab.goldPerRound}g/round"
                is SpecialAbility.Fireball  -> "Ability: Fireball (dmg ${ab.power}, r${ab.radius})"
                is SpecialAbility.Flying    -> "Ability: Flying"
            }
        }
        if (unit.type is StructureUnitType) {
            val desc = when (val ab = unit.type.ability) {
                is SpecialAbility.YieldGold -> "Yields ${ab.goldPerRound}g/round"
                else -> ""
            }
            if (desc.isNotEmpty()) lines += desc
        }
        if (unit.type is MonsterUnitType) lines += "Class: ${unit.type.monsterClass.name.lowercase().replace('_', ' ')}"
        tooltipLines = lines
        tooltipAnchorX = anchorX
        tooltipAnchorY = anchorY
        tooltipVisible = true
    }

    private fun dismissTooltip() { tooltipVisible = false }

    // ─── Input ───────────────────────────────────────────────────────────────

    private fun createInputProcessor() = object : InputAdapter() {

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val worldY = Gdx.graphics.height - screenY.toFloat()

            // ── Right-click: tooltip for unit or tile ─────────────────────────
            if (button == Input.Buttons.RIGHT) {
                val gridX = ((screenX - gridOffsetX) / cellSize).toInt()
                val gridY = ((worldY - gridOffsetY) / cellSize).toInt()
                if (gridX in 0 until grid.width && gridY in 0 until grid.height) {
                    val anchorX = gridOffsetX + (gridX + 1) * cellSize
                    val anchorY = gridOffsetY + gridY * cellSize
                    val unit = allUnitsById.values.firstOrNull { it.occupies(Position(gridX, gridY)) }
                    if (unit != null) {
                        showUnitTooltip(unit, anchorX, anchorY)
                    } else {
                        showTileTooltip(grid.getTile(gridX, gridY), anchorX, anchorY)
                    }
                } else {
                    dismissTooltip()
                }
                return true
            }

            // ── Left-click: dismiss tooltip, then normal handling ─────────────
            dismissTooltip()

            when (phase) {
                SandboxPhase.TERRITORY_EDITOR -> {
                    val screenH = Gdx.graphics.height.toFloat()
                    val btnW    = gridOffsetX - 16f
                    val btnH    = 46f
                    val (playerY, enemyY, resetY) = territoryButtonBounds(screenH)
                    when {
                        screenX >= 8f && screenX <= 8f + btnW && worldY >= playerY && worldY <= playerY + btnH -> {
                            paintingPlayerSide = true; return true
                        }
                        screenX >= 8f && screenX <= 8f + btnW && worldY >= enemyY && worldY <= enemyY + btnH -> {
                            paintingPlayerSide = false; return true
                        }
                        screenX >= 8f && screenX <= 8f + btnW && worldY >= resetY && worldY <= resetY + btnH -> {
                            playerTerritory.clear()
                            playerTerritory.addAll((0 until BattleGrid.ENEMY_ZONE_START)
                                .flatMap { x -> (0 until BattleGrid.GRID_SIZE).map { y -> Position(x, y) } })
                            return true
                        }
                    }
                    // Grid paint
                    val gx = ((screenX - gridOffsetX) / cellSize).toInt()
                    val gy = ((worldY - gridOffsetY) / cellSize).toInt()
                    if (gx in 0 until grid.width && gy in 0 until grid.height) {
                        paintTerritoryCell(gx, gy)
                        isPainting = true
                        return true
                    }
                }

                SandboxPhase.MAP_EDITOR -> {
                    // Tile palette click
                    for ((i, tile) in TileType.entries.withIndex()) {
                        val (bx, by, bw, bh) = tilePaletteSlotBounds(i)
                        if (screenX >= bx && screenX <= bx + bw && worldY >= by && worldY <= by + bh) {
                            selectedTile = tile
                            return true
                        }
                    }
                    // Grid paint
                    val gx = ((screenX - gridOffsetX) / cellSize).toInt()
                    val gy = ((worldY - gridOffsetY) / cellSize).toInt()
                    if (gx in 0 until grid.width && gy in 0 until grid.height) {
                        grid.setTile(gx, gy, selectedTile)
                        isPainting = true
                        return true
                    }
                }

                SandboxPhase.UNIT_PLACEMENT -> {
                    val screenH = Gdx.graphics.height.toFloat()
                    // Faction button click
                    for ((i, tab) in PlacementTab.entries.withIndex()) {
                        val by  = factionButtonY(i, screenH)
                        val btnW = gridOffsetX - 12f
                        if (screenX >= 6f && screenX <= 6f + btnW && worldY >= by && worldY <= by + 40f) {
                            selectedTab = tab
                            return true
                        }
                    }
                    // Scroll arrow buttons
                    val units = UnitType.all
                    val maxVis = maxVisibleSlots(screenH)
                    if (units.size > maxVis) {
                        val slotW  = gridOffsetX - 12f
                        val arrW   = slotW / 2f - 2f
                        val arrH   = 28f
                        val arrY   = 6f
                        if (worldY >= arrY && worldY <= arrY + arrH) {
                            if (screenX >= 6f && screenX <= 6f + arrW) {
                                // UP
                                if (unitScrollOffset > 0) unitScrollOffset--
                                return true
                            }
                            if (screenX >= 6f + arrW + 4f && screenX <= 6f + arrW + 4f + arrW) {
                                // DOWN
                                if (unitScrollOffset < units.size - maxVis) unitScrollOffset++
                                return true
                            }
                        }
                    }
                    // Unit slot drag start
                    for ((i, type) in units.withIndex()) {
                        val sy   = unitSlotY(i, screenH)
                        val slotW = gridOffsetX - 12f
                        val hintY = factionButtonY(PlacementTab.entries.size - 1, screenH) - 20f
                        if (sy + 54f < 40f || sy > hintY - 4f) continue  // not visible
                        if (screenX >= 6f && screenX <= 6f + slotW && worldY >= sy && worldY <= sy + 54f) {
                            draggingType = type
                            dragScreenX  = screenX.toFloat()
                            dragScreenY  = worldY
                            isDragging   = true
                            return true
                        }
                    }
                    // Click placed unit to remove
                    val gx = ((screenX - gridOffsetX) / cellSize).toInt()
                    val gy = ((worldY - gridOffsetY) / cellSize).toInt()
                    if (gx in 0 until grid.width && gy in 0 until grid.height) {
                        val unit = grid.unitAt(Position(gx, gy))
                        if (unit != null) {
                            grid.removeUnit(unit)
                            allUnitsById.remove(unit.id)
                            rebuildUI()
                            return true
                        }
                    }
                }
            }
            return false
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            val worldY = Gdx.graphics.height - screenY.toFloat()

            if (phase == SandboxPhase.TERRITORY_EDITOR && isPainting) {
                val gx = ((screenX - gridOffsetX) / cellSize).toInt()
                val gy = ((worldY - gridOffsetY) / cellSize).toInt()
                if (gx in 0 until grid.width && gy in 0 until grid.height) paintTerritoryCell(gx, gy)
                return true
            }

            if (phase == SandboxPhase.MAP_EDITOR && isPainting) {
                val gx = ((screenX - gridOffsetX) / cellSize).toInt()
                val gy = ((worldY - gridOffsetY) / cellSize).toInt()
                if (gx in 0 until grid.width && gy in 0 until grid.height) {
                    grid.setTile(gx, gy, selectedTile)
                }
                return true
            }

            if (isDragging) {
                dragScreenX = screenX.toFloat()
                dragScreenY = worldY
                hoverGridX  = ((screenX - gridOffsetX) / cellSize).toInt()
                hoverGridY  = ((worldY - gridOffsetY) / cellSize).toInt()
                return true
            }
            return false
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            isPainting = false

            if (!isDragging) return false
            val worldY = Gdx.graphics.height - screenY.toFloat()
            val gx     = ((screenX - gridOffsetX) / cellSize).toInt()
            val gy     = ((worldY - gridOffsetY) / cellSize).toInt()
            val pos    = Position(gx, gy)
            val type   = draggingType

            if (type != null && gx in 0 until grid.width && gy in 0 until grid.height
                && isValidZone(gx, gy, selectedTab.owner)
                && grid.getTile(gx, gy).placeable
                && grid.canUnitFitAt(Position(gx, gy), type.size)
            ) {
                val unit = BattleUnit(id = nextUnitId++, type = type, owner = selectedTab.owner, position = pos)
                if (grid.placeUnit(unit)) {
                    allUnitsById[unit.id] = unit
                    rebuildUI()
                }
            }

            draggingType = null
            isDragging   = false
            hoverGridX   = -1
            hoverGridY   = -1
            return true
        }

        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            if (phase == SandboxPhase.UNIT_PLACEMENT) {
                val screenH = Gdx.graphics.height.toFloat()
                val maxVis  = maxVisibleSlots(screenH)
                val maxOffset = maxOf(0, UnitType.all.size - maxVis)
                unitScrollOffset = (unitScrollOffset + amountY.toInt()).coerceIn(0, maxOffset)
                return true
            }
            return false
        }
    }

    private fun isValidZone(gridX: Int, gridY: Int, owner: BattleUnit.Owner) = when (owner) {
        BattleUnit.Owner.PLAYER  -> Position(gridX, gridY) in playerTerritory
        BattleUnit.Owner.ENEMY   -> Position(gridX, gridY) !in playerTerritory
        BattleUnit.Owner.NATURAL -> true
    }

    private fun paintTerritoryCell(gx: Int, gy: Int) {
        val pos = Position(gx, gy)
        if (paintingPlayerSide) playerTerritory.add(pos) else playerTerritory.remove(pos)
    }

    // ─── UI buttons ──────────────────────────────────────────────────────────

    private fun rebuildUI() {
        stage.clear()
        val table = Table().apply { setFillParent(true); bottom().right().pad(20f) }

        when (phase) {
            SandboxPhase.TERRITORY_EDITOR -> {
                val nextBtn = TextButton("NEXT: PAINT TILES >", skin)
                nextBtn.addListener(object : ClickListener() {
                    override fun clicked(e: InputEvent?, x: Float, y: Float) {
                        phase = SandboxPhase.MAP_EDITOR; rebuildUI()
                    }
                })
                table.add(nextBtn).width(230f).height(52f).pad(8f).row()
                addBackButton(table)
            }

            SandboxPhase.MAP_EDITOR -> {
                val nextBtn = TextButton("NEXT: PLACE UNITS >", skin)
                nextBtn.addListener(object : ClickListener() {
                    override fun clicked(e: InputEvent?, x: Float, y: Float) {
                        phase = SandboxPhase.UNIT_PLACEMENT; rebuildUI()
                    }
                })
                table.add(nextBtn).width(230f).height(52f).pad(8f).row()

                val backTerrBtn = TextButton("< EDIT TERRITORY", skin)
                backTerrBtn.addListener(object : ClickListener() {
                    override fun clicked(e: InputEvent?, x: Float, y: Float) {
                        phase = SandboxPhase.TERRITORY_EDITOR; rebuildUI()
                    }
                })
                table.add(backTerrBtn).width(210f).height(45f).pad(8f).row()
                addBackButton(table)
            }

            SandboxPhase.UNIT_PLACEMENT -> {
                val hasPlayer   = allUnitsById.values.any { it.owner == BattleUnit.Owner.PLAYER }
                val hasOpponent = allUnitsById.values.any { it.owner == BattleUnit.Owner.ENEMY || it.owner == BattleUnit.Owner.NATURAL }

                if (hasPlayer && hasOpponent) {
                    val startBtn = TextButton("START BATTLE >", skin)
                    startBtn.addListener(object : ClickListener() {
                        override fun clicked(e: InputEvent?, x: Float, y: Float) { launchBattle() }
                    })
                    table.add(startBtn).width(200f).height(55f).pad(8f).row()
                }

                val backMapBtn = TextButton("< EDIT MAP", skin)
                backMapBtn.addListener(object : ClickListener() {
                    override fun clicked(e: InputEvent?, x: Float, y: Float) {
                        phase = SandboxPhase.MAP_EDITOR; rebuildUI()
                    }
                })
                table.add(backMapBtn).width(180f).height(45f).pad(8f).row()
                addBackButton(table)
            }
        }

        stage.addActor(table)
    }

    private fun addBackButton(table: Table) {
        val btn = TextButton("BACK TO MENU", skin)
        btn.addListener(object : ClickListener() {
            override fun clicked(e: InputEvent?, x: Float, y: Float) { game.screen = MainMenuScreen(game) }
        })
        table.add(btn).width(180f).height(45f).pad(8f)
    }

    // ─── Launch battle ────────────────────────────────────────────────────────

    private fun launchBattle() {
        val playerPlacements  = allUnitsById.values.filter { it.owner == BattleUnit.Owner.PLAYER }.map { it.type to it.position }
        val enemyPlacements   = allUnitsById.values.filter { it.owner == BattleUnit.Owner.ENEMY  }.map { it.type to it.position }
        val naturalPlacements = allUnitsById.values.filter { it.owner == BattleUnit.Owner.NATURAL }.map { it.type to it.position }

        val tileOverrides = buildList {
            for (x in 0 until grid.width) for (y in 0 until grid.height) {
                val t = grid.getTile(x, y)
                if (t != TileType.GRASS) add(Position(x, y) to t)
            }
        }

        // Build a snapshot of the territory (only store non-default, or always store for sandbox)
        val territory: Set<Position>? = playerTerritory.toSet().takeUnless { set ->
            // Skip if it exactly matches the default left/right split (saves memory for normal scenarios)
            set.size == BattleGrid.ENEMY_ZONE_START * BattleGrid.GRID_SIZE &&
            set.all { it.x < BattleGrid.ENEMY_ZONE_START }
        }

        val scenario = ScenarioDefinition(
            name              = "Sandbox Battle",
            description       = "Custom sandbox scenario",
            enemyPlacements   = enemyPlacements,
            playerPlacements  = playerPlacements,
            naturalPlacements = naturalPlacements,
            tileOverrides     = tileOverrides,
            playerTerritory   = territory
        )

        game.screen = BattleScreen(game, scenario, onBattleEnd = { game.screen = this })
    }

    // ─── Skin ────────────────────────────────────────────────────────────────

    private fun createSkin(): Skin {
        val skin = Skin()
        val f = BitmapFont().apply { data.setScale(1.1f) }
        skin.add("default-font", f)
        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        skin.add("white", Texture(px)); px.dispose()
        skin.add("default", TextButton.TextButtonStyle().apply {
            up   = skin.newDrawable("white", Color(0.2f,  0.3f,  0.2f,  1f))
            over = skin.newDrawable("white", Color(0.3f,  0.45f, 0.3f,  1f))
            down = skin.newDrawable("white", Color(0.12f, 0.2f,  0.12f, 1f))
            font = f; fontColor = Color.WHITE
        })
        return skin
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun resize(width: Int, height: Int) { stage.viewport.update(width, height, true) }

    override fun dispose() {
        shapeRenderer.dispose()
        batch.dispose()
        stage.dispose()
        skin.dispose()
        Fonts.disposeAll()
    }
}
