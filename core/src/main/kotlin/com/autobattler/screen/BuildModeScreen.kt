package com.autobattler.screen

import com.autobattler.AutobattlerGame
import com.autobattler.model.*
import com.autobattler.render.Fonts
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
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport

/**
 * Build Mode Screen — the commander manages permanent camp defenses between missions.
 *
 * - BUY units by dragging from the buy panel onto the player-side (left half) of the map.
 * - SELL placed units by clicking them — refunds 50% of their cost.
 * - SAVE & RETURN commits the layout and gold to the Commander save.
 * - CANCEL discards all changes and returns to the Campaign screen.
 *
 * Placed units become the pre-positioned defense whenever a bandit ambush occurs.
 * Survivors persist across ambushes (at full HP); dead units are permanently lost.
 */
class BuildModeScreen(private val game: AutobattlerGame) : ScreenAdapter() {

    // ─── Grid + rendering ────────────────────────────────────────────────────────

    private val grid = BattleGrid()
    private val shapeRenderer = ShapeRenderer()
    private val batch = SpriteBatch()
    private lateinit var font: BitmapFont
    private lateinit var bigFont: BitmapFont
    private val stage = Stage(ScreenViewport())
    private lateinit var skin: Skin
    private lateinit var commanderPanel: CommanderPanel

    // Layout — matches BattleScreen constants for visual consistency
    private val cellSize = 48f
    private val gridOffsetX = 220f   // leaves room for CommanderPanel sidebar
    private val gridOffsetY = 185f   // leaves room for buy bar at the bottom

    // ─── Buy bar ─────────────────────────────────────────────────────────────────

    private val buyBarY = 10f
    private val buyBarH = 160f
    private val slotSize = 110f
    private val slotGap = 12f
    private val arrowW = 36f
    private var scrollOffset = 0

    private val maxVisibleSlots: Int
        get() {
            val usable = grid.width * cellSize - arrowW * 2
            return maxOf(1, (usable / (slotSize + slotGap)).toInt())
        }

    // ─── State ───────────────────────────────────────────────────────────────────

    /** Available unit types for purchase (filtered to what commander has unlocked). */
    private val shopTypes: List<UnitType>
        get() = UnitType.purchasable.filter { it.id in (game.commander?.unlockedUnitTypeIds ?: emptyList()) }

    /** Gold available in this session (committed to Commander on save). */
    private var gold = 0

    /** Units placed on the camp grid in this session. */
    private data class PlacedUnit(val id: Int, val type: UnitType, val gridX: Int, val gridY: Int)
    private val placedUnits = mutableListOf<PlacedUnit>()
    private var nextId = 1

    // Drag-and-drop
    private var draggingType: UnitType? = null
    private var dragScreenX = 0f
    private var dragScreenY = 0f
    private var isDragging = false
    private var hoverGridX = -1
    private var hoverGridY = -1

    // Status feedback (e.g., "Sold Warrior for 25g")
    private var statusMessage = ""
    private var statusTimer = 0f

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    override fun show() {
        Gdx.input.inputProcessor = InputMultiplexer(stage, createInputProcessor())

        font    = Fonts.create(18, borderWidth = 1f)
        bigFont = Fonts.create(24, borderWidth = 1.5f)
        skin    = createSkin()

        val commander = game.commander ?: run { game.screen = CampaignScreen(game); return }
        gold = commander.gold

        // Load the camp map (same map used for bandit ambushes)
        try {
            val mapDef = MapLoader.loadMap("maps/ambush_bandit_camp.json")
            MapLoader.applyToGrid(mapDef, grid)
        } catch (e: Exception) {
            Gdx.app.log("BuildMode", "Camp map load failed — using default: ${e.message}")
            MapLayouts.applyTutorialLayout(grid)
        }

        // Restore previously saved camp units into the local placed list
        for (campUnit in commander.campUnits) {
            val type = runCatching { UnitType.fromId(campUnit.typeId) }.getOrNull() ?: continue
            placedUnits.add(PlacedUnit(nextId++, type, campUnit.gridX, campUnit.gridY))
        }

        commanderPanel = CommanderPanel(commander.copy(gold = gold))
        rebuildUI()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.06f, 0.06f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (statusTimer > 0f) statusTimer -= delta

        drawGrid()
        drawPlacedUnits()
        drawBuyBar()
        if (isDragging) drawDragGhost()
        drawHUD()

        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) = stage.viewport.update(width, height, true)

    override fun dispose() {
        shapeRenderer.dispose()
        batch.dispose()
        stage.dispose()
        skin.dispose()
        if (::commanderPanel.isInitialized) commanderPanel.dispose()
        Fonts.disposeAll()
    }

    // ─── Grid drawing ────────────────────────────────────────────────────────────

    private fun drawGrid() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Tiles
        for (x in 0 until grid.width) {
            for (y in 0 until grid.height) {
                val sx = gridOffsetX + x * cellSize
                val sy = gridOffsetY + y * cellSize
                val tile = grid.getTile(x, y)
                shapeRenderer.color = tileColor(tile, x <= BattleGrid.PLAYER_ZONE_END)
                shapeRenderer.rect(sx, sy, cellSize, cellSize)

                if (tile == TileType.WATER) {
                    shapeRenderer.color = Color(0.15f, 0.3f, 0.55f, 1f)
                    val wo = ((x + y) % 3) * 4f
                    shapeRenderer.rect(sx + 4f + wo, sy + cellSize / 2 - 1f, cellSize * 0.4f, 2f)
                }
            }
        }

        // Grid lines
        shapeRenderer.color = Color(0f, 0f, 0f, 0.25f)
        for (x in 0..grid.width) shapeRenderer.rect(gridOffsetX + x * cellSize, gridOffsetY, 1f, grid.height * cellSize)
        for (y in 0..grid.height) shapeRenderer.rect(gridOffsetX, gridOffsetY + y * cellSize, grid.width * cellSize, 1f)

        // Territory border (yellow vertical line between player and enemy sides)
        val borderX = gridOffsetX + (BattleGrid.PLAYER_ZONE_END + 1) * cellSize
        shapeRenderer.color = Color(0.85f, 0.85f, 0.2f, 0.9f)
        shapeRenderer.rect(borderX - 2f, gridOffsetY, 4f, grid.height * cellSize)

        // Hover highlight during drag
        val dragType = draggingType
        if (isDragging && dragType != null &&
            hoverGridX in 0..BattleGrid.PLAYER_ZONE_END && hoverGridY in 0 until grid.height
        ) {
            val canPlace = canPlaceAt(hoverGridX, hoverGridY, dragType)
            shapeRenderer.color = if (canPlace) Color(0.3f, 1f, 0.3f, 0.4f) else Color(1f, 0.3f, 0.3f, 0.4f)
            val size = dragType.size
            shapeRenderer.rect(
                gridOffsetX + hoverGridX * cellSize,
                gridOffsetY + hoverGridY * cellSize,
                cellSize * size, cellSize * size
            )
        }

        shapeRenderer.end()
    }

    private fun tileColor(tile: TileType, playerSide: Boolean): Color = when (tile) {
        TileType.GRASS         -> if (playerSide) Color(0.08f, 0.16f, 0.08f, 1f) else Color(0.16f, 0.08f, 0.08f, 1f)
        TileType.WATER         -> Color(0.08f, 0.15f, 0.35f, 1f)
        TileType.ROCK          -> Color(0.25f, 0.22f, 0.20f, 1f)
        TileType.BRIDGE        -> Color(0.35f, 0.25f, 0.10f, 1f)
        TileType.TREE          -> Color(0.05f, 0.12f, 0.05f, 1f)
        TileType.SAND          -> if (playerSide) Color(0.28f, 0.22f, 0.08f, 1f) else Color(0.32f, 0.20f, 0.06f, 1f)
        TileType.MUD           -> if (playerSide) Color(0.14f, 0.12f, 0.06f, 1f) else Color(0.18f, 0.10f, 0.04f, 1f)
        TileType.SHALLOW_WATER -> Color(0.10f, 0.22f, 0.40f, 1f)
        TileType.FORT          -> Color(0.22f, 0.20f, 0.18f, 1f)
        TileType.LAVA          -> Color(0.50f, 0.10f, 0.02f, 1f)
        TileType.ICE           -> if (playerSide) Color(0.18f, 0.32f, 0.52f, 1f) else Color(0.22f, 0.22f, 0.42f, 1f)
        TileType.RUINS         -> if (playerSide) Color(0.18f, 0.15f, 0.12f, 1f) else Color(0.22f, 0.14f, 0.10f, 1f)
    }

    // ─── Units drawing ───────────────────────────────────────────────────────────

    private fun drawPlacedUnits() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (p in placedUnits) {
            val uc = p.type.size.toFloat()
            val cx = gridOffsetX + p.gridX * cellSize + cellSize * uc / 2
            val cy = gridOffsetY + p.gridY * cellSize + cellSize * uc / 2
            val radius = cellSize * uc * 0.42f
            // Blue tinted circle (player side)
            shapeRenderer.color = Color(0.2f, 0.6f, 1f, 0.7f)
            shapeRenderer.circle(cx, cy, radius)
            shapeRenderer.color = Color(0.08f, 0.08f, 0.12f, 1f)
            shapeRenderer.circle(cx, cy, radius - 3f)
            // Red sell dot in top-right corner
            shapeRenderer.color = Color(0.85f, 0.2f, 0.2f, 0.8f)
            shapeRenderer.circle(
                gridOffsetX + p.gridX * cellSize + cellSize * uc - 7f,
                gridOffsetY + p.gridY * cellSize + cellSize * uc - 7f,
                7f
            )
        }
        shapeRenderer.end()

        // Sprites
        batch.begin()
        for (p in placedUnits) {
            val uc = p.type.size.toFloat()
            val ss = cellSize * uc - 8f
            val sx = gridOffsetX + p.gridX * cellSize + (cellSize * uc - ss) / 2
            val sy = gridOffsetY + p.gridY * cellSize + (cellSize * uc - ss) / 2 - 2f
            batch.draw(SpriteGenerator.getTexture(p.type), sx, sy, ss, ss)
        }
        // Unit name abbreviation
        for (p in placedUnits) {
            font.color = Color(0.75f, 0.92f, 1f, 1f)
            val label = p.type.displayName.take(4)
            font.draw(batch, label, gridOffsetX + p.gridX * cellSize + 2f, gridOffsetY + p.gridY * cellSize + 14f)
        }
        batch.end()
    }

    // ─── Buy bar ─────────────────────────────────────────────────────────────────

    private fun drawBuyBar() {
        val barX = gridOffsetX
        val barW = grid.width * cellSize
        val needsScroll = shopTypes.size > maxVisibleSlots
        val arrowWeff = if (needsScroll) arrowW else 0f
        val slotsAreaX = barX + arrowWeff

        // Background panel
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.06f, 0.08f, 0.06f, 1f)
        shapeRenderer.rect(barX, buyBarY, barW, buyBarH)
        // Scroll arrow backgrounds
        if (needsScroll) {
            shapeRenderer.color = Color(0.12f, 0.16f, 0.12f, 1f)
            shapeRenderer.rect(barX, buyBarY, arrowW, buyBarH)
            shapeRenderer.rect(barX + barW - arrowW, buyBarY, arrowW, buyBarH)
        }
        // Unit slot backgrounds
        val visCount = minOf(maxVisibleSlots, shopTypes.size - scrollOffset)
        for (vi in 0 until visCount) {
            val type = shopTypes[scrollOffset + vi]
            val canAfford = gold >= type.cost
            val slotX = slotsAreaX + vi * (slotSize + slotGap) + slotGap
            val slotY = buyBarY + (buyBarH - slotSize) / 2
            shapeRenderer.color = if (canAfford) Color(0.1f, 0.18f, 0.1f, 1f) else Color(0.08f, 0.08f, 0.1f, 1f)
            shapeRenderer.rect(slotX, slotY, slotSize, slotSize)
        }
        shapeRenderer.end()

        // Arrow labels
        batch.begin()
        if (needsScroll) {
            font.color = Color.WHITE
            font.draw(batch, "<", barX + arrowW / 2 - 5f, buyBarY + buyBarH / 2 + 8f)
            font.draw(batch, ">", barX + barW - arrowW / 2 - 6f, buyBarY + buyBarH / 2 + 8f)
        }
        // Unit slots
        for (vi in 0 until visCount) {
            val type = shopTypes[scrollOffset + vi]
            val canAfford = gold >= type.cost
            val slotX = slotsAreaX + vi * (slotSize + slotGap) + slotGap
            val slotY = buyBarY + (buyBarH - slotSize) / 2

            val sSize = slotSize - 20f
            batch.setColor(if (canAfford) Color.WHITE else Color(0.4f, 0.4f, 0.4f, 1f))
            batch.draw(SpriteGenerator.getTexture(type), slotX + 10f, slotY + 20f, sSize, sSize)
            batch.setColor(Color.WHITE)

            font.color = if (canAfford) Color(1f, 0.84f, 0f, 1f) else Color(0.45f, 0.45f, 0.45f, 1f)
            font.draw(batch, "${type.cost}g", slotX + 4f, slotY + 17f)

            font.color = if (canAfford) Color.WHITE else Color(0.5f, 0.5f, 0.5f, 1f)
            font.draw(batch, type.displayName.take(10), slotX + 4f, slotY + slotSize - 4f)

            val sellPrice = type.cost / 2
            font.color = Color(0.65f, 0.65f, 0.65f, 1f)
            font.draw(batch, "sell:${sellPrice}g", slotX + 4f, slotY + slotSize / 2 + 4f)
        }

        // Hint / status message
        font.color = if (statusTimer > 0f) Color(0.4f, 1f, 0.4f, 1f) else Color.LIGHT_GRAY
        font.draw(
            batch,
            if (statusTimer > 0f) statusMessage
            else "Drag to place  |  Click unit on map to sell (50% refund)",
            barX + 4f,
            buyBarY + buyBarH + 16f
        )
        batch.end()
    }

    // ─── Drag ghost ──────────────────────────────────────────────────────────────

    private fun drawDragGhost() {
        val type = draggingType ?: return
        val ghostSize = cellSize - 4f
        batch.begin()
        batch.setColor(1f, 1f, 1f, 0.75f)
        batch.draw(SpriteGenerator.getTexture(type), dragScreenX - ghostSize / 2, dragScreenY - ghostSize / 2, ghostSize, ghostSize)
        batch.setColor(Color.WHITE)
        batch.end()
    }

    // ─── HUD (right sidebar info) ─────────────────────────────────────────────────

    private fun drawHUD() {
        val hudX = gridOffsetX + grid.width * cellSize + 20f
        var hudY = Gdx.graphics.height - 30f

        batch.begin()
        bigFont.color = Color(0.95f, 0.75f, 0.2f, 1f)
        bigFont.draw(batch, "CAMP BUILD MODE", hudX, hudY); hudY -= 44f

        font.color = Color(1f, 0.84f, 0f, 1f)
        font.draw(batch, "Gold: ${gold}g", hudX, hudY); hudY -= 26f

        font.color = Color(0.75f, 0.92f, 1f, 1f)
        font.draw(batch, "Camp Units: ${placedUnits.size}", hudX, hudY); hudY -= 36f

        font.color = Color(0.75f, 0.75f, 0.75f, 1f)
        for (line in listOf(
            "Place units on the",
            "left (green) side.",
            "",
            "These defenders",
            "fight bandits when",
            "an ambush strikes!",
            "",
            "Survivors persist.",
            "Dead units are",
            "gone forever."
        )) {
            font.draw(batch, line, hudX, hudY); hudY -= 20f
        }
        batch.end()
    }

    // ─── Placement logic ─────────────────────────────────────────────────────────

    /** Returns true if [type] can be placed with its bottom-left at ([gx], [gy]). */
    private fun canPlaceAt(gx: Int, gy: Int, type: UnitType): Boolean {
        for (dx in 0 until type.size) {
            for (dy in 0 until type.size) {
                val x = gx + dx; val y = gy + dy
                if (x > BattleGrid.PLAYER_ZONE_END) return false
                val pos = Position(x, y)
                if (!grid.isInBounds(pos)) return false
                if (!grid.getTile(pos).placeable) return false
                if (placedUnits.any { p -> p.type.size.let { s -> x in p.gridX until p.gridX + s && y in p.gridY until p.gridY + s } }) return false
            }
        }
        return true
    }

    /** Returns the placed unit occupying ([gx], [gy]), if any. */
    private fun unitAt(gx: Int, gy: Int): PlacedUnit? = placedUnits.find { p ->
        val s = p.type.size
        gx in p.gridX until p.gridX + s && gy in p.gridY until p.gridY + s
    }

    // ─── Input ───────────────────────────────────────────────────────────────────

    private fun createInputProcessor() = object : InputAdapter() {

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val worldY = Gdx.graphics.height - screenY.toFloat()

            // Scroll arrows in buy bar
            if (tryScrollArrow(screenX.toFloat(), worldY)) return true

            // Buy bar slot → start drag
            val slotType = slotAt(screenX.toFloat(), worldY)
            if (slotType != null) {
                if (gold >= slotType.cost) {
                    draggingType = slotType
                    dragScreenX  = screenX.toFloat()
                    dragScreenY  = worldY
                    isDragging   = true
                }
                return true
            }

            // Click placed unit on player side → sell for 50%
            val gx = ((screenX - gridOffsetX) / cellSize).toInt()
            val gy = ((worldY - gridOffsetY) / cellSize).toInt()
            if (gx in 0..BattleGrid.PLAYER_ZONE_END && gy in 0 until grid.height) {
                val existing = unitAt(gx, gy)
                if (existing != null) {
                    val sellPrice = existing.type.cost / 2
                    gold += sellPrice
                    placedUnits.remove(existing)
                    statusMessage = "Sold ${existing.type.displayName} for ${sellPrice}g"
                    statusTimer = 2.5f
                    refreshPanel()
                    return true
                }
            }
            return false
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (!isDragging) return false
            dragScreenX = screenX.toFloat()
            dragScreenY = Gdx.graphics.height - screenY.toFloat()
            hoverGridX  = ((screenX - gridOffsetX) / cellSize).toInt()
            hoverGridY  = ((dragScreenY - gridOffsetY) / cellSize).toInt()
            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (!isDragging) return false
            val worldY  = Gdx.graphics.height - screenY.toFloat()
            val gx      = ((screenX - gridOffsetX) / cellSize).toInt()
            val gy      = ((worldY - gridOffsetY) / cellSize).toInt()
            val type    = draggingType

            if (type != null && gx in 0..BattleGrid.PLAYER_ZONE_END && gy in 0 until grid.height) {
                if (canPlaceAt(gx, gy, type) && gold >= type.cost) {
                    gold -= type.cost
                    placedUnits.add(PlacedUnit(nextId++, type, gx, gy))
                    refreshPanel()
                }
            }

            draggingType = null; isDragging = false
            hoverGridX = -1; hoverGridY = -1
            return true
        }

        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            if (shopTypes.size > maxVisibleSlots) {
                val max = shopTypes.size - maxVisibleSlots
                scrollOffset = (scrollOffset + amountY.toInt()).coerceIn(0, max)
                return true
            }
            return false
        }
    }

    private fun slotAt(sx: Float, sy: Float): UnitType? {
        if (sy !in buyBarY..(buyBarY + buyBarH)) return null
        val barX = gridOffsetX
        val barW = grid.width * cellSize
        val needsScroll = shopTypes.size > maxVisibleSlots
        val arrowWeff = if (needsScroll) arrowW else 0f
        val slotsAreaX = barX + arrowWeff
        val slotsAreaEnd = barX + barW - arrowWeff
        if (needsScroll && (sx < slotsAreaX || sx > slotsAreaEnd)) return null
        val visCount = minOf(maxVisibleSlots, shopTypes.size - scrollOffset)
        for (vi in 0 until visCount) {
            val slotX = slotsAreaX + vi * (slotSize + slotGap) + slotGap
            val slotY = buyBarY + (buyBarH - slotSize) / 2
            if (sx in slotX..(slotX + slotSize) && sy in slotY..(slotY + slotSize))
                return shopTypes[scrollOffset + vi]
        }
        return null
    }

    private fun tryScrollArrow(sx: Float, sy: Float): Boolean {
        if (sy !in buyBarY..(buyBarY + buyBarH)) return false
        if (shopTypes.size <= maxVisibleSlots) return false
        val barX = gridOffsetX
        val barW = grid.width * cellSize
        val max = shopTypes.size - maxVisibleSlots
        if (sx in barX..(barX + arrowW)) {
            if (scrollOffset > 0) scrollOffset--
            return true
        }
        if (sx in (barX + barW - arrowW)..(barX + barW)) {
            if (scrollOffset < max) scrollOffset++
            return true
        }
        return false
    }

    // ─── UI stage ────────────────────────────────────────────────────────────────

    private fun refreshPanel() {
        val c = game.commander ?: return
        commanderPanel.refresh(c.copy(gold = gold))
    }

    private fun rebuildUI() {
        stage.clear()

        // Commander panel — left sidebar
        val panelContainer = Table().apply { setFillParent(true); top().left() }
        panelContainer.add(commanderPanel).width(210f).fillY()
        stage.addActor(panelContainer)

        // Action buttons — top right
        val btnTable = Table().apply { setFillParent(true); top().right().pad(16f) }

        val saveBtn = TextButton("SAVE & RETURN", skin, "save")
        saveBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { saveCampAndReturn() }
        })
        btnTable.add(saveBtn).width(210f).height(58f).padBottom(10f).row()

        val cancelBtn = TextButton("CANCEL", skin)
        cancelBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = CampaignScreen(game)
            }
        })
        btnTable.add(cancelBtn).width(210f).height(46f)
        stage.addActor(btnTable)
    }

    /** Commits the current camp layout and remaining gold to the Commander save. */
    private fun saveCampAndReturn() {
        val commander = game.commander ?: return
        val newCampUnits = placedUnits.map { CampUnit(it.type.id, it.gridX, it.gridY) }
        val updated = commander
            .withCampUnits(newCampUnits)
            .copy(gold = gold)
        game.commander = updated
        game.saveManager.save(updated)
        game.screen = CampaignScreen(game)
    }

    // ─── Skin ─────────────────────────────────────────────────────────────────────

    private fun createSkin(): Skin {
        val skin = Skin()
        val btnFont  = Fonts.create(19, Color.WHITE, borderWidth = 1f)
        val saveFont = Fonts.create(21, Color(0.8f, 1f, 0.8f, 1f), borderWidth = 1.5f)

        skin.add("default-font", btnFont)
        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        skin.add("white", Texture(px)); px.dispose()

        skin.add("default", Label.LabelStyle(btnFont, Color.WHITE))
        skin.add("default", TextButton.TextButtonStyle().apply {
            up   = skin.newDrawable("white", Color(0.22f, 0.30f, 0.22f, 1f))
            over = skin.newDrawable("white", Color(0.32f, 0.46f, 0.32f, 1f))
            down = skin.newDrawable("white", Color(0.14f, 0.20f, 0.14f, 1f))
            font = btnFont; fontColor = Color.WHITE
        })
        skin.add("save", TextButton.TextButtonStyle().apply {
            up   = skin.newDrawable("white", Color(0.16f, 0.38f, 0.16f, 1f))
            over = skin.newDrawable("white", Color(0.24f, 0.54f, 0.24f, 1f))
            down = skin.newDrawable("white", Color(0.10f, 0.24f, 0.10f, 1f))
            font = saveFont; fontColor = Color(0.85f, 1f, 0.85f, 1f)
        })
        return skin
    }
}
