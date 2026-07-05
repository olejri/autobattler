package com.autobattler.screen

import com.autobattler.AutobattlerGame
import com.autobattler.engine.BattleEvent
import com.autobattler.engine.BattleOutcome
import com.autobattler.engine.GridBattleEngine
import com.autobattler.model.BattleGrid
import com.autobattler.model.BattleUnit
import com.autobattler.model.MonsterUnitType
import com.autobattler.model.NormalUnitType
import com.autobattler.model.Position
import com.autobattler.model.SpecialUnitType
import com.autobattler.model.StructureUnitType
import com.autobattler.model.TileType
import com.autobattler.model.UnitType
import com.autobattler.render.BattleEffects
import com.autobattler.render.Fonts
import com.autobattler.render.SoundGenerator
import com.autobattler.render.SpriteGenerator
import com.badlogic.gdx.Gdx
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
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport

class SandboxScreen(private val game: AutobattlerGame) : ScreenAdapter() {

    private enum class SandboxPhase { MAP_EDITOR, UNIT_PLACEMENT, BATTLE }

    private enum class PlacementTab(val owner: BattleUnit.Owner, val label: String, val color: Color) {
        PLAYER(BattleUnit.Owner.PLAYER, "PLAYER", Color(0.2f, 0.6f, 1f, 1f)),
        ENEMY(BattleUnit.Owner.ENEMY, "ENEMY", Color(1f, 0.3f, 0.2f, 1f)),
        NATURAL(BattleUnit.Owner.NATURAL, "NATURAL", Color(0.85f, 0.65f, 0.2f, 1f))
    }

    private val grid = BattleGrid()
    private val engine = GridBattleEngine()

    private val shapeRenderer = ShapeRenderer()
    private val batch = SpriteBatch()
    private lateinit var font: BitmapFont
    private lateinit var bigFont: BitmapFont
    private lateinit var skin: Skin
    private val stage = Stage(ScreenViewport())

    private val cellSize = 48f
    private val gridOffsetX = 220f
    private val gridOffsetY = 185f
    private val leftPanelWidth = 220f
    private val rightHudX get() = gridOffsetX + grid.width * cellSize + 20f

    private var phase = SandboxPhase.MAP_EDITOR
    private var selectedTile = TileType.GRASS
    private var selectedTab = PlacementTab.PLAYER
    private var nextUnitId = 1

    private var isPainting = false
    private var draggingType: UnitType? = null
    private var dragScreenX = 0f
    private var dragScreenY = 0f
    private var isDragging = false
    private var hoverGridX = -1
    private var hoverGridY = -1

    private val renderPositions = mutableMapOf<Int, Vector2>()
    private val renderHp = mutableMapOf<Int, Int>()
    private val renderCooldown = mutableMapOf<Int, Int>()
    private val deadUnits = mutableSetOf<Int>()
    private val allUnitsById = mutableMapOf<Int, BattleUnit>()
    private var battleOutcome: BattleOutcome? = null
    private var battleEvents: List<BattleEvent> = emptyList()
    private var currentEventIndex = 0
    private var eventTimer = 0f
    private val eventDelay = 0.5f
    private val animDuration = 0.2f
    private var animating = false
    private var animUnitId = -1
    private var animFrom = Vector2()
    private var animTo = Vector2()
    private var animProgress = 0f
    private var flashUnitId = -1
    private var flashTimer = 0f
    private var flashColor = Color.WHITE.cpy()
    private val battleEffects = BattleEffects()
    private val battleLog = mutableListOf<String>()
    private var battleEnded = false

    override fun show() {
        Gdx.input.inputProcessor = InputMultiplexer(stage, createInputProcessor())
        font = Fonts.create(18, borderWidth = 1f)
        bigFont = Fonts.create(28, borderWidth = 1.5f)
        skin = createSkin()

        grid.clear()
        for (x in 0 until grid.width) {
            for (y in 0 until grid.height) {
                grid.setTile(x, y, TileType.GRASS)
            }
        }

        resetRenderStateFromGrid()
        rebuildUI()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.06f, 0.06f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (phase == SandboxPhase.BATTLE) {
            updateBattle(delta)
        }
        if (flashTimer > 0f) {
            flashTimer -= delta
        }
        battleEffects.update(delta)

        drawGrid()
        drawUnits()
        drawLeftPanel()
        drawHUD()
        drawDragGhost()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        battleEffects.render(shapeRenderer)
        Gdx.gl.glDisable(GL20.GL_BLEND)

        stage.act(delta)
        stage.draw()
    }

    private fun drawGrid() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (x in 0 until grid.width) {
            for (y in 0 until grid.height) {
                val sx = gridOffsetX + x * cellSize
                val sy = gridOffsetY + y * cellSize
                val tile = grid.getTile(x, y)
                shapeRenderer.color = getTileColor(tile, x)
                shapeRenderer.rect(sx, sy, cellSize, cellSize)

                when (tile) {
                    TileType.WATER -> {
                        shapeRenderer.color = Color(0.15f, 0.3f, 0.55f, 1f)
                        val waveOffset = ((x + y) % 3) * 4f
                        shapeRenderer.rect(sx + 4f + waveOffset, sy + cellSize / 2 - 1f, cellSize * 0.4f, 2f)
                    }
                    TileType.ROCK -> {
                        shapeRenderer.color = Color(0.35f, 0.32f, 0.28f, 1f)
                        shapeRenderer.circle(sx + cellSize / 2, sy + cellSize / 2, cellSize * 0.3f)
                        shapeRenderer.color = Color(0.45f, 0.42f, 0.38f, 1f)
                        shapeRenderer.circle(sx + cellSize / 2 - 2f, sy + cellSize / 2 + 2f, cellSize * 0.18f)
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
                        shapeRenderer.color = Color(0.15f, 0.5f, 0.15f, 1f)
                        shapeRenderer.circle(sx + cellSize / 2 - 3f, sy + cellSize * 0.7f, cellSize * 0.2f)
                    }
                    else -> {}
                }
            }
        }

        if (phase == SandboxPhase.UNIT_PLACEMENT && isDragging && hoverGridX in 0 until grid.width && hoverGridY in 0 until grid.height) {
            val type = draggingType
            if (type != null) {
                val pos = Position(hoverGridX, hoverGridY)
                val canPlace = canPlaceUnit(type, selectedTab.owner, pos)
                shapeRenderer.color = if (canPlace) Color(0.3f, 0.7f, 0.3f, 0.45f) else Color(0.7f, 0.2f, 0.2f, 0.4f)
                shapeRenderer.rect(
                    gridOffsetX + hoverGridX * cellSize + 1f,
                    gridOffsetY + hoverGridY * cellSize + 1f,
                    cellSize * type.size - 2f,
                    cellSize * type.size - 2f
                )
            }
        }
        shapeRenderer.end()

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
        if (phase != SandboxPhase.BATTLE) {
            val dividerX = gridOffsetX + BattleGrid.ENEMY_ZONE_START * cellSize
            shapeRenderer.color = Color(1f, 0.9f, 0.25f, 1f)
            shapeRenderer.line(dividerX, gridOffsetY, dividerX, gridOffsetY + grid.height * cellSize)
            shapeRenderer.line(dividerX + 1f, gridOffsetY, dividerX + 1f, gridOffsetY + grid.height * cellSize)
        }
        shapeRenderer.end()
    }

    private fun getTileColor(tile: TileType, x: Int): Color = when (tile) {
        TileType.GRASS -> if (x <= BattleGrid.PLAYER_ZONE_END) Color(0.08f, 0.16f, 0.08f, 1f) else Color(0.16f, 0.08f, 0.08f, 1f)
        TileType.WATER -> Color(0.08f, 0.15f, 0.35f, 1f)
        TileType.ROCK -> Color(0.25f, 0.22f, 0.2f, 1f)
        TileType.BRIDGE -> Color(0.35f, 0.25f, 0.1f, 1f)
        TileType.TREE -> Color(0.05f, 0.12f, 0.05f, 1f)
    }

    private fun drawUnits() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for ((unitId, renderPos) in renderPositions) {
            if (unitId in deadUnits) continue
            val unit = allUnitsById[unitId] ?: continue
            val unitCells = unit.size.toFloat()
            val cx = gridOffsetX + renderPos.x * cellSize + cellSize * unitCells / 2f
            val cy = gridOffsetY + renderPos.y * cellSize + cellSize * unitCells / 2f
            val radius = cellSize * unitCells * 0.42f

            var color = when (unit.owner) {
                BattleUnit.Owner.PLAYER -> Color(0.2f, 0.6f, 1f, 0.6f)
                BattleUnit.Owner.ENEMY -> Color(1f, 0.3f, 0.2f, 0.6f)
                BattleUnit.Owner.NATURAL -> Color(0.6f, 0.45f, 0.1f, 0.6f)
            }
            if (unitId == flashUnitId && flashTimer > 0f) {
                val t = flashTimer / 0.3f
                color = Color(
                    MathUtils.lerp(color.r, flashColor.r, t),
                    MathUtils.lerp(color.g, flashColor.g, t),
                    MathUtils.lerp(color.b, flashColor.b, t),
                    1f
                )
            }

            shapeRenderer.color = color
            shapeRenderer.circle(cx, cy, radius)
            shapeRenderer.color = Color(0.08f, 0.08f, 0.12f, 1f)
            shapeRenderer.circle(cx, cy, radius - 3f)

            val hp = renderHp[unitId] ?: unit.currentHp
            val barWidth = cellSize * unitCells - 6f
            val barHeight = 5f
            val barX = gridOffsetX + renderPos.x * cellSize + 3f
            val barY = gridOffsetY + renderPos.y * cellSize + cellSize * unitCells - 8f
            shapeRenderer.color = Color(0.15f, 0.15f, 0.15f, 1f)
            shapeRenderer.rect(barX, barY, barWidth, barHeight)
            val hp01 = hp.toFloat() / unit.type.maxHp.toFloat()
            shapeRenderer.color = when {
                hp01 > 0.6f -> Color(0.2f, 0.8f, 0.2f, 1f)
                hp01 > 0.3f -> Color(0.9f, 0.8f, 0.1f, 1f)
                else -> Color(0.9f, 0.2f, 0.2f, 1f)
            }
            shapeRenderer.rect(barX, barY, barWidth * hp01, barHeight)

            if (phase == SandboxPhase.BATTLE && !unit.type.isStructure) {
                val maxCd = unit.type.cooldown
                val curCd = renderCooldown[unitId] ?: 0
                val ready01 = if (maxCd <= 0) 1f else (maxCd - curCd).toFloat() / maxCd.toFloat()
                val cdBarY = barY - 6f
                shapeRenderer.color = Color(0.1f, 0.1f, 0.1f, 1f)
                shapeRenderer.rect(barX, cdBarY, barWidth, 4f)
                shapeRenderer.color = if (ready01 >= 1f) Color(1f, 0.85f, 0f, 1f) else Color(0.2f, 0.75f, 0.9f, 1f)
                shapeRenderer.rect(barX, cdBarY, barWidth * ready01, 4f)
            }

            if (phase == SandboxPhase.UNIT_PLACEMENT) {
                shapeRenderer.color = Color(0.8f, 0.2f, 0.2f, 0.7f)
                shapeRenderer.circle(
                    gridOffsetX + renderPos.x * cellSize + cellSize * unitCells - 8f,
                    gridOffsetY + renderPos.y * cellSize + cellSize * unitCells - 8f,
                    7f
                )
            }
        }
        shapeRenderer.end()

        batch.begin()
        for ((unitId, renderPos) in renderPositions) {
            if (unitId in deadUnits) continue
            val unit = allUnitsById[unitId] ?: continue
            val unitCells = unit.size.toFloat()
            val spriteSize = cellSize * unitCells - 8f
            val spriteX = gridOffsetX + renderPos.x * cellSize + (cellSize * unitCells - spriteSize) / 2f
            val spriteY = gridOffsetY + renderPos.y * cellSize + (cellSize * unitCells - spriteSize) / 2f - 2f
            batch.color = when (unit.owner) {
                BattleUnit.Owner.ENEMY -> Color(1f, 0.6f, 0.6f, 1f)
                BattleUnit.Owner.NATURAL -> Color(0.9f, 0.8f, 0.5f, 1f)
                else -> Color.WHITE
            }
            batch.draw(SpriteGenerator.getTexture(unit.type), spriteX, spriteY, spriteSize, spriteSize)
        }
        batch.color = Color.WHITE
        batch.end()
    }

    private fun drawLeftPanel() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.07f, 0.08f, 0.1f, 1f)
        shapeRenderer.rect(0f, 0f, leftPanelWidth, Gdx.graphics.height.toFloat())
        shapeRenderer.color = Color(0.12f, 0.13f, 0.17f, 1f)
        shapeRenderer.rect(leftPanelWidth - 2f, 0f, 2f, Gdx.graphics.height.toFloat())
        shapeRenderer.end()

        when (phase) {
            SandboxPhase.MAP_EDITOR -> drawTilePalette()
            SandboxPhase.UNIT_PLACEMENT -> drawUnitPlacementPanel()
            SandboxPhase.BATTLE -> drawBattlePanel()
        }
    }

    private fun drawTilePalette() {
        batch.begin()
        bigFont.color = Color.WHITE
        bigFont.draw(batch, "Map Editor", 12f, Gdx.graphics.height - 24f)
        font.color = Color.LIGHT_GRAY
        font.draw(batch, "Select a tile and paint the map.", 12f, Gdx.graphics.height - 56f)
        font.draw(batch, "Click-drag across the grid.", 12f, Gdx.graphics.height - 78f)
        batch.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for ((index, tile) in TileType.values().withIndex()) {
            val rect = tilePaletteRect(index)
            shapeRenderer.color = if (tile == selectedTile) Color(0.9f, 0.8f, 0.25f, 0.95f) else Color(0.16f, 0.17f, 0.2f, 1f)
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
            shapeRenderer.color = getTileColor(tile, if (tile == TileType.GRASS) 0 else BattleGrid.ENEMY_ZONE_START)
            shapeRenderer.rect(rect.x + 6f, rect.y + 6f, 23f, rect.height - 12f)
        }
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        for ((index, tile) in TileType.values().withIndex()) {
            val rect = tilePaletteRect(index)
            shapeRenderer.color = if (tile == selectedTile) Color(1f, 0.95f, 0.5f, 1f) else Color(0.35f, 0.38f, 0.45f, 1f)
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
        }
        shapeRenderer.end()

        batch.begin()
        for ((index, tile) in TileType.values().withIndex()) {
            val rect = tilePaletteRect(index)
            font.color = if (tile == selectedTile) Color.BLACK else Color.WHITE
            font.draw(batch, tile.displayName, rect.x + 40f, rect.y + 23f)
        }
        batch.end()
    }

    private fun drawUnitPlacementPanel() {
        batch.begin()
        bigFont.color = Color.WHITE
        bigFont.draw(batch, "Unit Setup", 12f, Gdx.graphics.height - 24f)
        font.color = Color.LIGHT_GRAY
        font.draw(batch, "Select a team tab, then drag units", 12f, Gdx.graphics.height - 56f)
        font.draw(batch, "onto valid tiles. Click units to remove.", 12f, Gdx.graphics.height - 78f)
        batch.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for ((index, tab) in PlacementTab.values().withIndex()) {
            val rect = tabRect(index)
            shapeRenderer.color = if (tab == selectedTab) tab.color.cpy().also { it.a = 0.9f } else Color(0.15f, 0.16f, 0.2f, 1f)
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
        }

        val slotTint = selectedTab.color.cpy().also { it.a = 0.22f }
        for ((index, type) in UnitType.all.withIndex()) {
            val rect = unitSlotRect(index)
            shapeRenderer.color = if (draggingType == type && isDragging) selectedTab.color.cpy().also { it.a = 0.45f } else Color(0.12f, 0.14f, 0.17f, 1f)
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
            shapeRenderer.color = slotTint
            shapeRenderer.rect(rect.x + 1f, rect.y + 1f, rect.width - 2f, rect.height - 2f)
        }
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        for ((index, tab) in PlacementTab.values().withIndex()) {
            val rect = tabRect(index)
            shapeRenderer.color = if (tab == selectedTab) Color.WHITE else Color(0.35f, 0.38f, 0.45f, 1f)
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
        }
        for (index in UnitType.all.indices) {
            val rect = unitSlotRect(index)
            shapeRenderer.color = Color(0.3f, 0.34f, 0.4f, 1f)
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
        }
        shapeRenderer.end()

        batch.begin()
        for ((index, tab) in PlacementTab.values().withIndex()) {
            val rect = tabRect(index)
            font.color = if (tab == selectedTab) Color.BLACK else Color.WHITE
            font.draw(batch, tab.label, rect.x + 12f, rect.y + 27f)
        }

        for ((index, type) in UnitType.all.withIndex()) {
            val rect = unitSlotRect(index)
            batch.color = Color.WHITE
            batch.draw(SpriteGenerator.getTexture(type), rect.x + 8f, rect.y + 8f, 40f, 40f)
            font.color = Color.WHITE
            font.draw(batch, type.displayName, rect.x + 58f, rect.y + 35f)
            font.color = Color.LIGHT_GRAY
            val stats = "HP ${type.maxHp}  ATK ${type.power}  SPD ${type.speed}"
            font.draw(batch, stats, rect.x + 58f, rect.y + 16f)
        }
        batch.color = Color.WHITE
        batch.end()
    }

    private fun drawBattlePanel() {
        batch.begin()
        bigFont.color = Color.WHITE
        bigFont.draw(batch, "Sandbox Battle", 12f, Gdx.graphics.height - 24f)
        font.color = Color.LIGHT_GRAY
        font.draw(batch, if (battleEnded) "Battle finished." else "Battle in progress...", 12f, Gdx.graphics.height - 56f)
        font.draw(batch, "Use the buttons to replay or return.", 12f, Gdx.graphics.height - 78f)
        batch.end()
    }

    private fun drawHUD() {
        batch.begin()
        var hudY = Gdx.graphics.height - 30f

        bigFont.color = Color.WHITE
        bigFont.draw(batch, "Sandbox Mode", rightHudX, hudY)
        hudY -= 36f

        font.color = Color.GOLD
        font.draw(batch, "Phase: ${phaseLabel()}", rightHudX, hudY)
        hudY -= 24f

        val playerCount = grid.allUnits.count { it.owner == BattleUnit.Owner.PLAYER }
        val enemyCount = grid.allUnits.count { it.owner == BattleUnit.Owner.ENEMY }
        val naturalCount = grid.allUnits.count { it.owner == BattleUnit.Owner.NATURAL }
        font.color = Color(0.75f, 0.85f, 1f, 1f)
        font.draw(batch, "Player Units: $playerCount", rightHudX, hudY)
        hudY -= 18f
        font.color = Color(1f, 0.72f, 0.72f, 1f)
        font.draw(batch, "Enemy Units: $enemyCount", rightHudX, hudY)
        hudY -= 18f
        font.color = Color(0.95f, 0.85f, 0.5f, 1f)
        font.draw(batch, "Neutral Units: $naturalCount", rightHudX, hudY)
        hudY -= 30f

        when (phase) {
            SandboxPhase.MAP_EDITOR -> {
                font.color = Color.WHITE
                font.draw(batch, "Selected Tile:", rightHudX, hudY)
                hudY -= 20f
                font.color = Color.GOLD
                font.draw(batch, selectedTile.displayName, rightHudX, hudY)
                hudY -= 28f
                font.color = Color.LIGHT_GRAY
                font.draw(batch, "Paint the battlefield, then", rightHudX, hudY)
                hudY -= 18f
                font.draw(batch, "continue to place units.", rightHudX, hudY)
            }
            SandboxPhase.UNIT_PLACEMENT -> {
                font.color = Color.WHITE
                font.draw(batch, "Active Team:", rightHudX, hudY)
                hudY -= 20f
                font.color = selectedTab.color
                font.draw(batch, selectedTab.label, rightHudX, hudY)
                hudY -= 28f
                font.color = Color.LIGHT_GRAY
                font.draw(batch, "Place on valid zones only.", rightHudX, hudY)
                hudY -= 18f
                font.draw(batch, "Green hover = valid placement.", rightHudX, hudY)
                hudY -= 18f
                font.draw(batch, "Red hover = invalid placement.", rightHudX, hudY)
                hudY -= 30f
                if (!canStartBattle()) {
                    font.color = Color(1f, 0.8f, 0.4f, 1f)
                    font.draw(batch, "Need 1 player and 1 enemy", rightHudX, hudY)
                    hudY -= 18f
                    font.draw(batch, "or neutral unit to start.", rightHudX, hudY)
                }
            }
            SandboxPhase.BATTLE -> {
                if (battleEnded) {
                    val outcome = battleOutcome
                    val resultText = when (outcome?.winner) {
                        BattleOutcome.Winner.PLAYER -> "VICTORY"
                        BattleOutcome.Winner.ENEMY -> "DEFEAT"
                        else -> "DRAW"
                    }
                    bigFont.color = when (outcome?.winner) {
                        BattleOutcome.Winner.PLAYER -> Color(0.45f, 1f, 0.45f, 1f)
                        BattleOutcome.Winner.ENEMY -> Color(1f, 0.4f, 0.35f, 1f)
                        else -> Color(1f, 0.9f, 0.4f, 1f)
                    }
                    bigFont.draw(batch, resultText, rightHudX, hudY)
                    hudY -= 34f
                    font.color = Color.LIGHT_GRAY
                    font.draw(batch, "Turns: ${outcome?.totalTurns ?: 0}", rightHudX, hudY)
                    hudY -= 30f
                }

                font.color = Color.GOLD
                font.draw(batch, "Battle Log:", rightHudX, hudY)
                hudY -= 20f
                for (line in battleLog.takeLast(24)) {
                    font.color = when {
                        "──" in line -> Color(0.6f, 0.6f, 0.8f, 1f)
                        "deals" in line -> Color(1f, 0.6f, 0.5f, 1f)
                        "heals" in line -> Color(0.5f, 1f, 0.6f, 1f)
                        "died" in line -> Color(1f, 0.3f, 0.3f, 1f)
                        "moves" in line -> Color(0.7f, 0.7f, 0.7f, 1f)
                        "Farm" in line -> Color(1f, 0.9f, 0.3f, 1f)
                        else -> Color.WHITE
                    }
                    font.draw(batch, line, rightHudX, hudY)
                    hudY -= 17f
                }
            }
        }
        batch.end()
    }

    private fun drawDragGhost() {
        if (phase != SandboxPhase.UNIT_PLACEMENT) return
        val type = draggingType ?: return
        val ghostSize = cellSize * type.size - 4f
        batch.begin()
        batch.color = Color(1f, 1f, 1f, 0.8f)
        batch.draw(SpriteGenerator.getTexture(type), dragScreenX - ghostSize / 2f, dragScreenY - ghostSize / 2f, ghostSize, ghostSize)
        batch.color = Color.WHITE
        batch.end()
    }

    private fun updateBattle(delta: Float) {
        if (battleEnded) return

        if (animating) {
            animProgress += delta / animDuration
            if (animProgress >= 1f) {
                renderPositions[animUnitId] = Vector2(animTo)
                animating = false
                if (currentEventIndex < battleEvents.size) {
                    val next = battleEvents[currentEventIndex]
                    if (next is BattleEvent.Move && next.unitId == animUnitId) {
                        applyNextEvent()
                        return
                    }
                }
            } else {
                val t = smoothStep(animProgress)
                renderPositions[animUnitId] = Vector2(
                    MathUtils.lerp(animFrom.x, animTo.x, t),
                    MathUtils.lerp(animFrom.y, animTo.y, t)
                )
            }
            return
        }

        if (battleEffects.isPlaying) return

        eventTimer += delta
        if (eventTimer >= eventDelay && currentEventIndex < battleEvents.size) {
            eventTimer = 0f
            applyNextEvent()
        }

        if (currentEventIndex >= battleEvents.size && !animating && !battleEffects.isPlaying) {
            battleEnded = true
            when (battleOutcome?.winner) {
                BattleOutcome.Winner.PLAYER -> SoundGenerator.getVictory().play(0.6f)
                BattleOutcome.Winner.ENEMY -> SoundGenerator.getDefeat().play(0.6f)
                BattleOutcome.Winner.DRAW -> SoundGenerator.getDefeat().play(0.4f)
                null -> {}
            }
            rebuildUI()
        }
    }

    private fun applyNextEvent() {
        if (currentEventIndex >= battleEvents.size) return
        when (val event = battleEvents[currentEventIndex++]) {
            is BattleEvent.TurnStart -> {
                battleLog.add("── Turn ${event.turnNumber} ──")
                for (id in renderCooldown.keys.toList()) {
                    renderCooldown[id] = ((renderCooldown[id] ?: 0) - 1).coerceAtLeast(0)
                }
            }
            is BattleEvent.Move -> {
                val isLastMoveForUnit = currentEventIndex >= battleEvents.size ||
                    battleEvents[currentEventIndex] !is BattleEvent.Move ||
                    (battleEvents[currentEventIndex] as BattleEvent.Move).unitId != event.unitId
                if (isLastMoveForUnit) {
                    battleLog.add("${event.unitName} moves to (${event.to.x},${event.to.y})")
                }
                animating = true
                animUnitId = event.unitId
                animFrom = Vector2(event.from.x.toFloat(), event.from.y.toFloat())
                animTo = Vector2(event.to.x.toFloat(), event.to.y.toFloat())
                animProgress = 0f
            }
            is BattleEvent.Attack -> {
                battleLog.add("${event.attackerName} deals ${event.damage} to ${event.targetName} [${event.targetHpAfter} HP]")
                renderHp[event.targetId] = event.targetHpAfter
                renderCooldown[event.attackerId] = event.attackerType.cooldown
                flashUnitId = event.targetId
                flashTimer = 0.3f
                flashColor = Color.RED.cpy()
                val targetX = gridOffsetX + event.targetPos.x * cellSize + cellSize / 2f
                val targetY = gridOffsetY + event.targetPos.y * cellSize + cellSize / 2f
                val attackerX = gridOffsetX + event.attackerPos.x * cellSize + cellSize / 2f
                val attackerY = gridOffsetY + event.attackerPos.y * cellSize + cellSize / 2f
                when (event.attackerType) {
                    is NormalUnitType.Melee -> {
                        battleEffects.addSlash(targetX, targetY, event.attackerPos.x > event.targetPos.x)
                        SoundGenerator.getSwordClash().play(0.5f)
                    }
                    is NormalUnitType.Ranged -> {
                        battleEffects.addArrow(attackerX, attackerY, targetX, targetY)
                        SoundGenerator.getArrowShot().play(0.4f)
                    }
                    is MonsterUnitType.Bear -> {
                        battleEffects.addSlash(targetX, targetY, event.attackerPos.x > event.targetPos.x)
                        SoundGenerator.getSwordClash().play(0.7f)
                    }
                    is MonsterUnitType.Wolf -> {
                        battleEffects.addSlash(targetX, targetY, event.attackerPos.x > event.targetPos.x)
                        SoundGenerator.getSwordClash().play(0.55f)
                    }
                    is NormalUnitType.Healer -> battleEffects.addSlash(targetX, targetY, false)
                    is SpecialUnitType.Farmer -> {}
                    is StructureUnitType.Farm -> {}
                }
            }
            is BattleEvent.Heal -> {
                battleLog.add("${event.healerName} heals ${event.targetName} +${event.amount} [${event.targetHpAfter} HP]")
                renderHp[event.targetId] = event.targetHpAfter
                renderCooldown[event.healerId] = allUnitsById[event.healerId]?.type?.cooldown ?: 0
                flashUnitId = event.targetId
                flashTimer = 0.3f
                flashColor = Color.GREEN.cpy()
                val targetX = gridOffsetX + event.targetPos.x * cellSize + cellSize / 2f
                val targetY = gridOffsetY + event.targetPos.y * cellSize + cellSize / 2f
                battleEffects.addHealGlow(targetX, targetY)
                SoundGenerator.getHealChime().play(0.4f)
            }
            is BattleEvent.Death -> {
                battleLog.add("${event.unitName} died!")
                deadUnits.add(event.unitId)
                SoundGenerator.getDeath().play(0.5f)
            }
            is BattleEvent.FarmPlanted -> {
                battleLog.add("${event.farmerName} planted a Farm at (${event.position.x},${event.position.y})")
                renderCooldown[event.farmerId] = allUnitsById[event.farmerId]?.type?.cooldown ?: 0
                val farm = BattleUnit(event.farmId, StructureUnitType.Farm, BattleUnit.Owner.PLAYER, event.position)
                registerUnit(farm)
                flashUnitId = event.farmId
                flashTimer = 0.4f
                flashColor = Color(0.4f, 1f, 0.3f, 1f)
            }
            is BattleEvent.FarmGold -> {
                battleLog.add("Farm +${event.amount}g")
                flashUnitId = event.farmId
                flashTimer = 0.25f
                flashColor = Color(1f, 0.9f, 0.1f, 1f)
            }
        }
    }

    private fun startBattle() {
        if (!canStartBattle()) return
        phase = SandboxPhase.BATTLE
        battleOutcome = engine.simulate(grid)
        battleEvents = battleOutcome?.events ?: emptyList()
        currentEventIndex = 0
        eventTimer = 0f
        animating = false
        animUnitId = -1
        animProgress = 0f
        flashUnitId = -1
        flashTimer = 0f
        battleEnded = false
        battleLog.clear()
        battleEffects.clear()
        resetRenderStateFromGrid()
        for (id in allUnitsById.keys) {
            renderCooldown[id] = 0
        }
        rebuildUI()
    }

    private fun returnToPlacement() {
        for (unit in grid.allUnits) {
            unit.currentHp = unit.type.maxHp
            unit.cooldownTimer = 0
        }
        phase = SandboxPhase.UNIT_PLACEMENT
        battleOutcome = null
        battleEvents = emptyList()
        currentEventIndex = 0
        eventTimer = 0f
        animating = false
        flashTimer = 0f
        flashUnitId = -1
        battleEnded = false
        battleEffects.clear()
        battleLog.clear()
        resetRenderStateFromGrid()
        rebuildUI()
    }

    private fun resetRenderStateFromGrid() {
        renderPositions.clear()
        renderHp.clear()
        renderCooldown.clear()
        deadUnits.clear()
        allUnitsById.clear()
        for (unit in grid.allUnits) {
            registerUnit(unit)
            renderCooldown[unit.id] = unit.cooldownTimer
        }
    }

    private fun registerUnit(unit: BattleUnit) {
        allUnitsById[unit.id] = unit
        renderPositions[unit.id] = Vector2(unit.position.x.toFloat(), unit.position.y.toFloat())
        renderHp[unit.id] = unit.currentHp
    }

    private fun rebuildUI() {
        stage.clear()
        val table = Table().apply { setFillParent(true); bottom().right().pad(20f) }

        when (phase) {
            SandboxPhase.MAP_EDITOR -> {
                val nextButton = TextButton("NEXT: PLACE UNITS", skin)
                nextButton.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        phase = SandboxPhase.UNIT_PLACEMENT
                        rebuildUI()
                    }
                })
                table.add(nextButton).width(230f).height(55f).pad(8f).row()
                table.add(menuButton()).width(200f).height(45f).pad(8f)
            }
            SandboxPhase.UNIT_PLACEMENT -> {
                if (canStartBattle()) {
                    val startButton = TextButton("START BATTLE", skin)
                    startButton.addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            startBattle()
                        }
                    })
                    table.add(startButton).width(210f).height(55f).pad(8f).row()
                }

                val backButton = TextButton("BACK: EDIT MAP", skin)
                backButton.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        phase = SandboxPhase.MAP_EDITOR
                        rebuildUI()
                    }
                })
                table.add(backButton).width(210f).height(45f).pad(8f).row()
                table.add(menuButton()).width(200f).height(45f).pad(8f)
            }
            SandboxPhase.BATTLE -> {
                if (battleEnded) {
                    val outcomeText = when (battleOutcome?.winner) {
                        BattleOutcome.Winner.PLAYER -> "VICTORY"
                        BattleOutcome.Winner.ENEMY -> "DEFEAT"
                        else -> "DRAW"
                    }
                    table.add(Label(outcomeText, skin, "title")).padBottom(6f).row()

                    val backButton = TextButton("BACK TO SETUP", skin)
                    backButton.addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            returnToPlacement()
                        }
                    })
                    table.add(backButton).width(210f).height(50f).pad(8f).row()
                    table.add(menuButton()).width(200f).height(45f).pad(8f)
                } else {
                    table.add(Label("Battle Running", skin)).padBottom(8f).row()
                    val speedLabel = Label("Playback: Standard", skin)
                    table.add(speedLabel).padBottom(4f).row()
                    table.add(menuButton()).width(200f).height(45f).pad(8f)
                }
            }
        }

        stage.addActor(table)
    }

    private fun menuButton(): TextButton {
        val button = TextButton("BACK TO MENU", skin)
        button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = MainMenuScreen(game)
            }
        })
        return button
    }

    private fun createInputProcessor() = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val worldY = Gdx.graphics.height - screenY.toFloat()
            when (phase) {
                SandboxPhase.MAP_EDITOR -> {
                    val tile = getPaletteTileAt(screenX.toFloat(), worldY)
                    if (tile != null) {
                        selectedTile = tile
                        return true
                    }

                    val gridPos = screenToGrid(screenX.toFloat(), worldY)
                    if (gridPos != null) {
                        paintTile(gridPos)
                        isPainting = true
                        return true
                    }
                }
                SandboxPhase.UNIT_PLACEMENT -> {
                    val tab = getTabAt(screenX.toFloat(), worldY)
                    if (tab != null) {
                        selectedTab = tab
                        return true
                    }

                    val slotType = getUnitSlotAt(screenX.toFloat(), worldY)
                    if (slotType != null) {
                        draggingType = slotType
                        dragScreenX = screenX.toFloat()
                        dragScreenY = worldY
                        isDragging = true
                        hoverGridX = -1
                        hoverGridY = -1
                        return true
                    }

                    val gridPos = screenToGrid(screenX.toFloat(), worldY)
                    if (gridPos != null) {
                        val unit = grid.unitAt(gridPos)
                        if (unit != null) {
                            removeUnit(unit)
                            rebuildUI()
                            return true
                        }
                    }
                }
                SandboxPhase.BATTLE -> {}
            }
            return false
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            val worldY = Gdx.graphics.height - screenY.toFloat()
            when (phase) {
                SandboxPhase.MAP_EDITOR -> {
                    if (!isPainting) return false
                    val gridPos = screenToGrid(screenX.toFloat(), worldY)
                    if (gridPos != null) {
                        paintTile(gridPos)
                    }
                    return true
                }
                SandboxPhase.UNIT_PLACEMENT -> {
                    if (!isDragging) return false
                    dragScreenX = screenX.toFloat()
                    dragScreenY = worldY
                    val gridPos = screenToGrid(screenX.toFloat(), worldY)
                    hoverGridX = gridPos?.x ?: -1
                    hoverGridY = gridPos?.y ?: -1
                    return true
                }
                SandboxPhase.BATTLE -> return false
            }
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val worldY = Gdx.graphics.height - screenY.toFloat()
            if (phase == SandboxPhase.MAP_EDITOR) {
                isPainting = false
                return false
            }

            if (phase == SandboxPhase.UNIT_PLACEMENT && isDragging) {
                val type = draggingType
                val gridPos = screenToGrid(screenX.toFloat(), worldY)
                if (type != null && gridPos != null && canPlaceUnit(type, selectedTab.owner, gridPos)) {
                    placeUnit(type, selectedTab.owner, gridPos)
                    rebuildUI()
                }
                draggingType = null
                isDragging = false
                hoverGridX = -1
                hoverGridY = -1
                return true
            }
            return false
        }
    }

    private fun paintTile(pos: Position) {
        grid.setTile(pos, selectedTile)
    }

    private fun placeUnit(type: UnitType, owner: BattleUnit.Owner, position: Position) {
        val unit = BattleUnit(id = nextUnitId++, type = type, owner = owner, position = position)
        if (grid.placeUnit(unit)) {
            registerUnit(unit)
            renderCooldown[unit.id] = 0
        }
    }

    private fun removeUnit(unit: BattleUnit) {
        grid.removeUnit(unit)
        allUnitsById.remove(unit.id)
        renderPositions.remove(unit.id)
        renderHp.remove(unit.id)
        renderCooldown.remove(unit.id)
        deadUnits.remove(unit.id)
    }

    private fun canPlaceUnit(type: UnitType, owner: BattleUnit.Owner, position: Position): Boolean {
        for (dx in 0 until type.size) {
            for (dy in 0 until type.size) {
                val tilePos = Position(position.x + dx, position.y + dy)
                if (!grid.isInBounds(tilePos)) return false
                if (!grid.getTile(tilePos).placeable) return false
                if (grid.isOccupied(tilePos)) return false
                when (owner) {
                    BattleUnit.Owner.PLAYER -> if (tilePos.x > BattleGrid.PLAYER_ZONE_END) return false
                    BattleUnit.Owner.ENEMY -> if (tilePos.x < BattleGrid.ENEMY_ZONE_START) return false
                    BattleUnit.Owner.NATURAL -> {}
                }
            }
        }
        return true
    }

    private fun canStartBattle(): Boolean {
        val hasPlayer = grid.allUnits.any { it.owner == BattleUnit.Owner.PLAYER }
        val hasOpponent = grid.allUnits.any {
            it.owner == BattleUnit.Owner.ENEMY || it.owner == BattleUnit.Owner.NATURAL
        }
        return hasPlayer && hasOpponent
    }

    private fun screenToGrid(screenX: Float, worldY: Float): Position? {
        val gridX = ((screenX - gridOffsetX) / cellSize).toInt()
        val gridY = ((worldY - gridOffsetY) / cellSize).toInt()
        return if (gridX in 0 until grid.width && gridY in 0 until grid.height) Position(gridX, gridY) else null
    }

    private fun getPaletteTileAt(x: Float, y: Float): TileType? {
        for ((index, tile) in TileType.values().withIndex()) {
            if (tilePaletteRect(index).contains(x, y)) return tile
        }
        return null
    }

    private fun getTabAt(x: Float, y: Float): PlacementTab? {
        for ((index, tab) in PlacementTab.values().withIndex()) {
            if (tabRect(index).contains(x, y)) return tab
        }
        return null
    }

    private fun getUnitSlotAt(x: Float, y: Float): UnitType? {
        for ((index, type) in UnitType.all.withIndex()) {
            if (unitSlotRect(index).contains(x, y)) return type
        }
        return null
    }

    private fun tilePaletteRect(index: Int): Rectangle {
        val height = 35f
        val gap = 10f
        return Rectangle(5f, 500f - index * (height + gap), 210f, height)
    }

    private fun tabRect(index: Int): Rectangle {
        val margin = 8f
        val width = (leftPanelWidth - margin * 2f - 8f) / 3f
        return Rectangle(margin + index * (width + 4f), Gdx.graphics.height - 126f, width, 36f)
    }

    private fun unitSlotRect(index: Int): Rectangle {
        val topY = Gdx.graphics.height - 150f
        val height = 56f
        val gap = 6f
        return Rectangle(8f, topY - (index + 1) * (height + gap), 204f, height)
    }

    private fun phaseLabel(): String = when (phase) {
        SandboxPhase.MAP_EDITOR -> "Map Editor"
        SandboxPhase.UNIT_PLACEMENT -> "Unit Placement"
        SandboxPhase.BATTLE -> if (battleEnded) "Battle Result" else "Battle"
    }

    private fun smoothStep(t: Float): Float = t * t * (3f - 2f * t)

    private fun createSkin(): Skin {
        val result = Skin()
        val uiFont = Fonts.create(18, borderWidth = 1f)
        val titleFont = Fonts.create(22, Color.WHITE, borderWidth = 1.5f)
        result.add("default-font", uiFont)
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
            setColor(Color.WHITE)
            fill()
        }
        result.add("white", Texture(pixmap))
        pixmap.dispose()

        result.add("default", Label.LabelStyle(uiFont, Color.WHITE))
        result.add("title", Label.LabelStyle(titleFont, Color.GOLD))
        result.add("default", TextButton.TextButtonStyle().apply {
            up = result.newDrawable("white", Color(0.2f, 0.3f, 0.2f, 1f))
            over = result.newDrawable("white", Color(0.3f, 0.45f, 0.3f, 1f))
            down = result.newDrawable("white", Color(0.12f, 0.2f, 0.12f, 1f))
            font = uiFont
            fontColor = Color.WHITE
        })
        return result
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        shapeRenderer.dispose()
        batch.dispose()
        stage.dispose()
        skin.dispose()
        Fonts.disposeAll()
        SpriteGenerator.dispose()
        SoundGenerator.dispose()
    }
}
