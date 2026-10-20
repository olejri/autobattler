package com.autobattler.screen

import com.autobattler.AutobattlerGame
import com.autobattler.engine.RogueScenarios
import com.autobattler.model.*
import com.autobattler.render.Fonts
import com.badlogic.gdx.Gdx
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
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport

/**
 * Roguelike map screen.
 *
 * Displays the current world's 15-node graph. The player sees:
 *   • Completed nodes (greyed out)
 *   • The current node (highlighted)
 *   • Reachable nodes they can navigate to (brighter)
 *   • Locked nodes (dim)
 *   • Connection lines between nodes
 *
 * Clicking a reachable node launches BattleScreen (for combat/elite/boss nodes)
 * or the appropriate special screen (rest / shop / event).
 *
 * Layout:
 *   Left sidebar  — run stats (archetype, world, relics, gold)
 *   Centre        — node map drawn with ShapeRenderer + BitmapFont
 *   Bottom bar    — world selector buttons (unlock next world after beating boss)
 */
class RogueMapScreen(private val game: AutobattlerGame) : ScreenAdapter() {

    private val stage  = Stage(ScreenViewport())
    private val shapes = ShapeRenderer()
    private val batch  = SpriteBatch()
    private lateinit var font:     BitmapFont
    private lateinit var bigFont:  BitmapFont
    private lateinit var skin:     Skin

    // Map drawing constants
    private val mapLeft   = 240f
    private val mapRight  get() = Gdx.graphics.width.toFloat() - 20f
    private val mapTop    get() = Gdx.graphics.height.toFloat() - 60f
    private val mapBottom = 80f
    private val nodeRadius = 22f

    private var nodePositions = mapOf<Int, Vector2>()

    // Tooltip
    private var hoveredNode: RogueNode? = null

    override fun show() {
        Gdx.input.inputProcessor = createInputProcessor()
        skin = buildSkin()
        font    = Fonts.create(16, borderWidth = 1f)
        bigFont = Fonts.create(22, Color(0.95f, 0.88f, 0.3f, 1f), borderWidth = 1.5f)

        val run = game.rogueRun ?: run { game.screen = MainMenuScreen(game); return }

        // If the run is complete, go to victory screen
        if (run.isComplete) {
            game.screen = RogueVictoryScreen(game)
            return
        }
        if (run.isDefeated) {
            game.screen = RogueDefeatScreen(game)
            return
        }

        nodePositions = computeNodePositions(run.currentWorld)
        buildSidebarUI(run)
    }

    // ─── Node positions ───────────────────────────────────────────────────────

    private fun computeNodePositions(world: RogueWorld): Map<Int, Vector2> {
        val maxDepth = world.maxDepth
        val positions = mutableMapOf<Int, Vector2>()

        // Group nodes by depth
        val byDepth = (0..maxDepth).map { d -> d to world.nodesAtDepth(d) }

        for ((depth, nodesAtDepth) in byDepth) {
            val x = mapLeft + (depth.toFloat() / maxDepth) * (mapRight - mapLeft - 40f) + 20f
            val count = nodesAtDepth.size
            for ((i, node) in nodesAtDepth.withIndex()) {
                val yFraction = if (count == 1) 0.5f else i.toFloat() / (count - 1)
                val y = mapBottom + yFraction * (mapTop - mapBottom - 40f) + 20f
                positions[node.id] = Vector2(x, y)
            }
        }
        return positions
    }

    // ─── Rendering ────────────────────────────────────────────────────────────

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.07f, 0.07f, 0.12f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val run = game.rogueRun ?: return
        val world = run.currentWorld
        val reachable = run.reachableNodeIds(run.currentWorldIndex)
        val completed = run.completedNodeKeys
            .filter { it.startsWith("${run.currentWorldIndex}:") }
            .map { it.substringAfter(":").toInt() }.toSet()

        drawConnections(world, completed, reachable)
        drawNodes(world, run, completed, reachable)

        // Hover tooltip
        val hn = hoveredNode
        if (hn != null) drawNodeTooltip(hn, run, reachable.contains(hn.id))

        stage.act(delta)
        stage.draw()
    }

    private fun drawConnections(
        world: RogueWorld, completed: Set<Int>, @Suppress("UNUSED_PARAMETER") reachable: Set<Int>
    ) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.begin(ShapeRenderer.ShapeType.Filled)

        for (node in world.nodes) {
            val fromPos = nodePositions[node.id] ?: continue
            for (nextId in node.connectionIds) {
                val toPos = nodePositions[nextId] ?: continue
                val isDone = node.id in completed
                val lineAlpha = if (isDone) 0.7f else 0.25f
                shapes.color = Color(0.5f, 0.6f, 0.5f, lineAlpha)
                shapes.rectLine(fromPos, toPos, 2.5f)
            }
        }
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawNodes(
        world: RogueWorld,
        run: RogueRun,
        completed: Set<Int>,
        reachable: Set<Int>
    ) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)

        for (node in world.nodes) {
            val pos = nodePositions[node.id] ?: continue
            val isCompleted = node.id in completed
            @Suppress("UNUSED_VARIABLE") val isCurrent   = node.id == run.currentNodeId
            val isReachable = node.id in reachable && !isCompleted

            val outerColor = when {
                node.type == RogueNodeType.BOSS   -> Color(0.9f, 0.2f, 0.2f, 1f)
                node.type == RogueNodeType.ELITE  -> Color(0.85f, 0.55f, 0.15f, 1f)
                node.type == RogueNodeType.REST   -> Color(0.25f, 0.6f, 0.35f, 1f)
                node.type == RogueNodeType.SHOP   -> Color(0.8f, 0.75f, 0.15f, 1f)
                node.type == RogueNodeType.EVENT  -> Color(0.45f, 0.45f, 0.85f, 1f)
                else                              -> Color(0.3f, 0.5f, 0.75f, 1f)
            }

            val alpha = when {
                isCompleted -> 0.35f
                isReachable -> 1f
                else        -> 0.45f
            }
            val displayColor = Color(outerColor.r, outerColor.g, outerColor.b, alpha)

            // Glow ring for reachable
            if (isReachable) {
                shapes.color = Color(1f, 1f, 1f, 0.25f)
                shapes.circle(pos.x, pos.y, nodeRadius + 8f)
            }
            // Completed tick ring
            if (isCompleted) {
                shapes.color = Color(0.4f, 0.7f, 0.4f, 0.5f)
                shapes.circle(pos.x, pos.y, nodeRadius + 3f)
            }

            // Node circle
            shapes.color = displayColor
            shapes.circle(pos.x, pos.y, nodeRadius)

            // Inner fill
            val innerColor = Color(displayColor.r * 0.5f, displayColor.g * 0.5f, displayColor.b * 0.5f, alpha)
            shapes.color = innerColor
            shapes.circle(pos.x, pos.y, nodeRadius - 4f)
        }

        shapes.end()

        // Draw icons and labels on top
        batch.begin()
        for (node in world.nodes) {
            val pos = nodePositions[node.id] ?: continue
            val isCompleted = node.id in completed
            val alpha = if (isCompleted) 0.4f else 1f

            font.color = Color(1f, 1f, 1f, alpha)
            val icon = when {
                isCompleted                   -> "✓"
                node.type == RogueNodeType.BOSS  -> "☠"
                node.type == RogueNodeType.ELITE -> "!"
                node.type == RogueNodeType.REST  -> "Z"
                node.type == RogueNodeType.SHOP  -> "$"
                node.type == RogueNodeType.EVENT -> "?"
                else                             -> "⚔"
            }

            // Center the icon
            val bounds = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, icon)
            font.draw(batch, icon, pos.x - bounds.width / 2, pos.y + bounds.height / 2)
        }
        batch.end()
    }

    private fun drawNodeTooltip(node: RogueNode, run: RogueRun, isReachable: Boolean) {
        val pos = nodePositions[node.id] ?: return
        val lines = buildList {
            add(node.type.displayName.uppercase())
            if (node.type == RogueNodeType.COMBAT || node.type == RogueNodeType.ELITE) {
                val names = node.enemyPoolIds
                    .mapNotNull { runCatching { UnitType.fromId(it).displayName }.getOrNull() }
                    .distinct()
                add("Enemies: ${names.joinToString(", ")}")
                add("Reward: ~${node.goldReward}g  +${node.xpReward}xp")
            }
            if (node.type == RogueNodeType.REST)  add("Restore 25% of your gold")
            if (node.type == RogueNodeType.SHOP)  add("Unlock a new unit type")
            if (node.type == RogueNodeType.EVENT) add("A random encounter")
            if (!isReachable && !run.hasCompleted(run.currentWorldIndex, node.id)) {
                add("[Locked]")
            }
        }

        val boxW = 240f
        val lineH = 18f
        val pad   = 10f
        val boxH  = pad * 2 + lines.size * lineH
        var bx = pos.x + nodeRadius + 6f
        var by = pos.y - boxH / 2
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        if (bx + boxW > sw - 4f) bx = pos.x - nodeRadius - boxW - 6f
        if (by < 4f) by = 4f
        if (by + boxH > sh) by = sh - boxH - 4f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.04f, 0.06f, 0.08f, 0.93f)
        shapes.rect(bx, by, boxW, boxH)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        batch.begin()
        var ly = by + boxH - pad
        for ((i, line) in lines.withIndex()) {
            font.color = if (i == 0) Color(0.95f, 0.88f, 0.3f, 1f) else Color(0.85f, 0.85f, 0.85f, 1f)
            font.draw(batch, line, bx + pad, ly)
            ly -= lineH
        }
        batch.end()
    }

    // ─── Sidebar UI ───────────────────────────────────────────────────────────

    private fun buildSidebarUI(run: RogueRun) {
        stage.clear()

        val root = Table()
        root.setFillParent(true)
        root.top().left().pad(10f)
        stage.addActor(root)

        val sidebar = Table()
        sidebar.background = skin.newDrawable("white", Color(0.08f, 0.1f, 0.12f, 1f))
        sidebar.pad(12f)

        // World name
        sidebar.add(Label(run.currentWorldName, skin, "worldTitle"))
            .padBottom(4f).left()
        sidebar.row()

        // Archetype
        sidebar.add(Label("Commander: ${run.archetype.displayName}", skin, "info"))
            .padBottom(2f).left()
        sidebar.row()
        sidebar.add(Label("Passive: ${run.archetype.passiveName}", skin, "small"))
            .padBottom(8f).left()
        sidebar.row()

        // Stats
        sidebar.add(Label("Gold:   ${run.gold}g", skin, "stat")).padBottom(2f).left()
        sidebar.row()
        sidebar.add(Label("Day:    ${run.runDay}", skin, "stat")).padBottom(2f).left()
        sidebar.row()
        sidebar.add(Label("Nodes:  ${run.completedNodeKeys.size}", skin, "stat")).padBottom(8f).left()
        sidebar.row()

        // Relics
        if (run.relics.isNotEmpty()) {
            sidebar.add(Label("RELICS", skin, "sectionHead")).padBottom(4f).left()
            sidebar.row()
            for (relic in run.relics) {
                val rl = Label("• ${relic.displayName}", skin, "small")
                rl.wrap = true
                sidebar.add(rl).width(200f).padBottom(2f).left()
                sidebar.row()
            }
            sidebar.row().padBottom(8f)
        }

        // World selector row (show all 3 world tabs at bottom)
        sidebar.add(Label("WORLDS", skin, "sectionHead")).padBottom(4f).left()
        sidebar.row()

        for (wi in 0..2) {
            val worldLabel = when (wi) {
                0 -> "I — Frontier"
                1 -> "II — Wastes"
                else -> "III — Abyss"
            }
            val isUnlocked = isWorldUnlocked(run, wi)
            val isCurrent  = wi == run.currentWorldIndex
            val btn = TextButton(worldLabel, skin, if (isCurrent) "worldActive" else "worldBtn")
            if (isUnlocked && !isCurrent) {
                btn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        navigateToWorld(wi)
                    }
                })
            }
            btn.isDisabled = !isUnlocked
            sidebar.add(btn).width(200f).height(36f).padBottom(4f).left()
            sidebar.row()
        }

        sidebar.row().padBottom(12f)
        val menuBtn = TextButton("MAIN MENU", skin)
        menuBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = MainMenuScreen(game)
            }
        })
        sidebar.add(menuBtn).width(200f).height(40f).padBottom(4f).left()

        root.add(sidebar).width(220f).fillY().top()
    }

    private fun isWorldUnlocked(run: RogueRun, worldIndex: Int): Boolean {
        if (worldIndex == 0) return true
        // World is unlocked when the boss of the previous world is completed
        val prevWorld = run.worlds[worldIndex - 1]
        val prevBoss  = prevWorld.nodes.firstOrNull { it.type == RogueNodeType.BOSS }
        return prevBoss != null && run.hasCompleted(worldIndex - 1, prevBoss.id)
    }

    private fun navigateToWorld(worldIndex: Int) {
        val run = game.rogueRun ?: return
        val updated = run.copy(currentWorldIndex = worldIndex)
        game.rogueRun = updated
        game.saveManager.saveRogue(updated)
        nodePositions = computeNodePositions(updated.currentWorld)
        buildSidebarUI(updated)
    }

    // ─── Input ────────────────────────────────────────────────────────────────

    private fun createInputProcessor() = object : com.badlogic.gdx.InputMultiplexer(stage) {
        init { addProcessor(createMapInputProcessor()) }
    }

    private fun createMapInputProcessor() = object : com.badlogic.gdx.InputAdapter() {
        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            val wx = screenX.toFloat()
            val wy = (Gdx.graphics.height - screenY).toFloat()
            val run = game.rogueRun ?: return false
            hoveredNode = nodePositions.entries
                .firstOrNull { (_, pos) ->
                    val dx = pos.x - wx; val dy = pos.y - wy
                    (dx * dx + dy * dy) < (nodeRadius + 8f) * (nodeRadius + 8f)
                }?.let { (id, _) -> run.currentWorld.nodeById(id) }
            return false
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val wx = screenX.toFloat()
            val wy = (Gdx.graphics.height - screenY).toFloat()
            val run = game.rogueRun ?: return false
            val reachable = run.reachableNodeIds(run.currentWorldIndex)

            val clickedNode = nodePositions.entries
                .firstOrNull { (_, pos) ->
                    val dx = pos.x - wx; val dy = pos.y - wy
                    (dx * dx + dy * dy) < nodeRadius * nodeRadius
                }?.let { (id, _) -> run.currentWorld.nodeById(id) }

            if (clickedNode != null && clickedNode.id in reachable) {
                enterNode(clickedNode, run)
                return true
            }
            return false
        }
    }

    private fun enterNode(node: RogueNode, run: RogueRun) {
        // Update currentNodeId immediately in the run
        val updatedRun = run.copy(currentNodeId = node.id)
        game.rogueRun = updatedRun
        game.saveManager.saveRogue(updatedRun)

        when (node.type) {
            RogueNodeType.COMBAT,
            RogueNodeType.ELITE,
            RogueNodeType.BOSS -> {
                val scenario = RogueScenarios.buildScenario(updatedRun, node, updatedRun.currentWorld)
                game.screen = BattleScreen(
                    game              = game,
                    scenario          = scenario,
                    rogueShopUnitIds  = updatedRun.shopUnitIds,
                    onRogueNodeWin    = { goldFromKills, goldFromFarms ->
                        onNodeWin(node, updatedRun, goldFromKills, goldFromFarms)
                    },
                    onRogueNodeLose   = {
                        onRunLost()
                    }
                )
            }
            RogueNodeType.REST  -> handleRestNode(node, updatedRun)
            RogueNodeType.SHOP  -> handleShopNode(node, updatedRun)
            RogueNodeType.EVENT -> handleEventNode(node, updatedRun)
        }
    }

    private fun onNodeWin(node: RogueNode, run: RogueRun, goldFromKills: Int, goldFromFarms: Int) {
        val totalGold = node.goldReward + goldFromKills + goldFromFarms
        var updated = run.afterNodeWin(run.currentWorldIndex, node.id, totalGold)

        // Update meta when a boss is beaten (unlocks next archetype, tracks stat)
        if (node.type == RogueNodeType.BOSS) {
            val newMeta = game.rogueMeta.afterWorldBoss(run.currentWorldIndex, updated)
            game.rogueMeta = newMeta
            game.saveManager.saveMeta(newMeta)

            if (run.currentWorldIndex >= 2) {
                // All 3 worlds beaten — run complete
                updated = updated.copy(isComplete = true)
                val finalMeta = game.rogueMeta.afterRunEnd(updated, won = true)
                game.rogueRun  = updated
                game.rogueMeta = finalMeta
                game.saveManager.saveRogue(updated)
                game.saveManager.saveMeta(finalMeta)
                game.screen = RogueVictoryScreen(game)
                return
            }
        }

        game.rogueRun = updated
        game.saveManager.saveRogue(updated)

        // After elite or boss, award a relic from the reward screen
        if (node.type == RogueNodeType.ELITE || node.type == RogueNodeType.BOSS) {
            game.screen = RogueRewardScreen(game, node, isElite = node.type == RogueNodeType.ELITE)
        } else {
            game.screen = RogueMapScreen(game)
        }
    }

    private fun onRunLost() {
        val run = game.rogueRun ?: return
        val updated = run.copy(isDefeated = true)
        game.rogueRun = updated
        game.saveManager.saveRogue(updated)
        // Record stats in meta (total runs defeated, nodes cleared, etc.)
        val newMeta = game.rogueMeta.afterRunEnd(updated, won = false)
        game.rogueMeta = newMeta
        game.saveManager.saveMeta(newMeta)
        game.screen = RogueDefeatScreen(game)
    }

    private fun handleRestNode(node: RogueNode, run: RogueRun) {
        // Rest: restore gold (20% bonus on top of current)
        val bonus = (run.gold * 0.2f).toInt().coerceAtLeast(50)
        var updated = run.afterNodeWin(run.currentWorldIndex, node.id, bonus)
        game.rogueRun = updated
        game.saveManager.saveRogue(updated)
        // Show a brief info and return to map
        game.screen = RogueRestEventScreen(game,
            title = "Rest Site",
            body  = "Your forces rest and recuperate.\n\nBonuses:\n+${bonus}g gold replenishment",
            onContinue = { game.screen = RogueMapScreen(game) }
        )
    }

    private fun handleShopNode(node: RogueNode, run: RogueRun) {
        var updated = run.afterNodeWin(run.currentWorldIndex, node.id, 0)
        game.rogueRun = updated
        game.saveManager.saveRogue(updated)
        game.screen = RogueShopScreen(game)
    }

    private fun handleEventNode(node: RogueNode, run: RogueRun) {
        // Deterministic random event based on seed + node id
        val rng = java.util.Random(run.worlds[run.currentWorldIndex].seed + node.id * 31L)
        val eventRoll = rng.nextInt(4)
        val (title, body, goldDelta) = when (eventRoll) {
            0 -> Triple("Merchant", "A travelling merchant offers you supplies.\n\n+80 gold", 80)
            1 -> Triple("Ambush!", "Bandits attack your camp in the night.\nYou drive them off but lose some supplies.\n\n-60 gold", -60)
            2 -> Triple("Buried Cache", "Your scouts find a buried chest!\n\n+120 gold", 120)
            else -> Triple("Wandering Hero", "A hero joins your cause for a price.\n\n+50 gold and a morale boost.", 50)
        }
        val goldDeltaFinal = goldDelta
        var updated = run.afterNodeWin(run.currentWorldIndex, node.id,
            goldDeltaFinal.coerceAtLeast(-run.gold))
        game.rogueRun = updated
        game.saveManager.saveRogue(updated)
        game.screen = RogueRestEventScreen(game,
            title = title,
            body  = body,
            onContinue = { game.screen = RogueMapScreen(game) }
        )
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        val run = game.rogueRun ?: return
        nodePositions = computeNodePositions(run.currentWorld)
        buildSidebarUI(run)
    }

    override fun dispose() {
        stage.dispose()
        shapes.dispose()
        batch.dispose()
        skin.dispose()
        Fonts.disposeAll()
    }

    // ─── Skin ─────────────────────────────────────────────────────────────────

    private fun buildSkin(): Skin {
        val s = Skin()
        val worldTitleFont = Fonts.create(20, Color(0.95f, 0.88f, 0.3f, 1f), borderWidth = 1f)
        val infoFont       = Fonts.create(15, Color.WHITE)
        val smallFont      = Fonts.create(13, Color(0.8f, 0.8f, 0.8f, 1f))
        val statFont       = Fonts.create(14, Color(0.75f, 0.95f, 0.75f, 1f))
        val sectionFont    = Fonts.create(12, Color(0.5f, 0.7f, 0.5f, 1f))
        val btnFont        = Fonts.create(14, Color.WHITE, borderWidth = 1f)

        s.add("default-font", btnFont)
        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        s.add("white", Texture(px)); px.dispose()

        s.add("worldTitle",  Label.LabelStyle(worldTitleFont, Color(0.95f, 0.88f, 0.3f, 1f)))
        s.add("info",        Label.LabelStyle(infoFont,       Color.WHITE))
        s.add("small",       Label.LabelStyle(smallFont,      Color(0.8f, 0.8f, 0.8f, 1f)))
        s.add("stat",        Label.LabelStyle(statFont,       Color(0.75f, 0.95f, 0.75f, 1f)))
        s.add("sectionHead", Label.LabelStyle(sectionFont,    Color(0.5f, 0.7f, 0.5f, 1f)))
        s.add("default",     Label.LabelStyle(btnFont,        Color.WHITE))

        val defBtn = TextButton.TextButtonStyle().apply {
            up       = s.newDrawable("white", Color(0.2f, 0.28f, 0.2f, 1f))
            over     = s.newDrawable("white", Color(0.3f, 0.42f, 0.3f, 1f))
            down     = s.newDrawable("white", Color(0.12f, 0.18f, 0.12f, 1f))
            font     = btnFont
            fontColor = Color.WHITE
        }
        s.add("default", defBtn)

        s.add("worldBtn", TextButton.TextButtonStyle().apply {
            up   = s.newDrawable("white", Color(0.18f, 0.22f, 0.3f, 1f))
            over = s.newDrawable("white", Color(0.28f, 0.34f, 0.45f, 1f))
            down = s.newDrawable("white", Color(0.12f, 0.15f, 0.22f, 1f))
            disabled = s.newDrawable("white", Color(0.12f, 0.12f, 0.14f, 1f))
            font = btnFont; fontColor = Color.WHITE; disabledFontColor = Color(0.4f, 0.4f, 0.4f, 1f)
        })
        s.add("worldActive", TextButton.TextButtonStyle().apply {
            up   = s.newDrawable("white", Color(0.35f, 0.45f, 0.6f, 1f))
            over = s.newDrawable("white", Color(0.45f, 0.55f, 0.7f, 1f))
            down = s.newDrawable("white", Color(0.25f, 0.35f, 0.5f, 1f))
            font = btnFont; fontColor = Color(0.9f, 0.95f, 1f, 1f)
        })

        return s
    }
}
