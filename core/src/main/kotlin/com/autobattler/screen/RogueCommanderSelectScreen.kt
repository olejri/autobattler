package com.autobattler.screen

import com.autobattler.AutobattlerGame
import com.autobattler.engine.RogueWorldGenerator
import com.autobattler.model.RogueArchetype
import com.autobattler.model.RogueMeta
import com.autobattler.model.RogueRun
import com.autobattler.render.Fonts
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport

/**
 * Pre-run screen where the player picks one of three commander archetypes.
 *
 * If a rogue run is already in progress it shows a "Resume run" option instead
 * of the archetype selection.
 */
class RogueCommanderSelectScreen(private val game: AutobattlerGame) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())
    private lateinit var skin: Skin

    override fun show() {
        Gdx.input.inputProcessor = stage
        skin = buildSkin()

        val root = Table()
        root.setFillParent(true)
        root.pad(30f)
        stage.addActor(root)

        // Title
        root.add(Label("ROGUE RUN", skin, "title"))
            .padBottom(6f).colspan(3).center()
        root.row()
        root.add(Label("Choose your commander — each faction has unique units and a passive bonus.",
            skin, "sub"))
            .padBottom(30f).colspan(3).center()
        root.row()

        // If a run is already saved, offer to resume or abandon
        val existing = game.rogueRun
        if (existing != null && !existing.isComplete && !existing.isDefeated) {
            root.add(Label("An active run was found.", skin, "sub"))
                .padBottom(10f).colspan(3).center()
            root.row()

            val worldLabel = existing.currentWorldName
            val nodeCount  = existing.completedNodeKeys.size
            root.add(Label("$worldLabel  •  $nodeCount nodes cleared  •  ${existing.gold}g  •  ${existing.relics.size} relic(s)",
                skin, "small"))
                .padBottom(20f).colspan(3).center()
            root.row()

            addBtn(root, "RESUME RUN", Color(0.25f, 0.5f, 0.25f, 1f), 260f, 60f) {
                game.screen = RogueMapScreen(game)
            }
            root.row().padBottom(12f)
            addBtn(root, "ABANDON  &  START FRESH", Color(0.5f, 0.2f, 0.2f, 1f), 300f, 50f) {
                game.saveManager.deleteRogue()
                game.rogueRun = null
                game.screen = RogueCommanderSelectScreen(game)
            }
            root.row().padBottom(20f)
            addBtn(root, "BACK TO MENU", Color(0.2f, 0.2f, 0.3f, 1f), 220f, 45f) {
                game.screen = MainMenuScreen(game)
            }
            return
        }

        // ── Meta stats row ────────────────────────────────────────────────────
        val meta = game.rogueMeta
        if (meta.totalRunsStarted > 0) {
            val bestStr = if (meta.bestRunNodesCleared > 0)
                "Best: ${meta.bestRunNodesCleared} nodes · ${meta.bestRunWorldsBeaten} worlds · ${meta.bestRunRelicsCollected} relics"
            else "No runs completed yet"
            root.add(Label(
                "Runs: ${meta.totalRunsStarted}  •  Wins: ${meta.totalRunsCompleted}  •  $bestStr",
                skin, "small"))
                .padBottom(20f).colspan(3).center()
            root.row()
        }

        // ── Three archetype cards ──────────────────────────────────────────────
        for (arch in RogueArchetype.values()) {
            root.add(buildArchetypeCard(arch, meta)).padRight(20f).top()
        }
        root.row().padTop(30f)

        addBtn(root, "BACK TO MENU", Color(0.2f, 0.2f, 0.3f, 1f), 220f, 45f) {
            game.screen = MainMenuScreen(game)
        }
        root.row()
    }

    private fun buildArchetypeCard(arch: RogueArchetype, meta: RogueMeta): Table {
        val isUnlocked = meta.isUnlocked(arch)
        val card = Table()
        card.background = skin.newDrawable("white",
            if (isUnlocked) Color(0.1f, 0.12f, 0.14f, 1f) else Color(0.07f, 0.07f, 0.09f, 1f))
        card.pad(18f)

        val accentColor = when {
            !isUnlocked -> Color(0.4f, 0.4f, 0.4f, 1f)
            arch == RogueArchetype.IRON_LEGION    -> Color(0.8f, 0.82f, 0.88f, 1f)
            arch == RogueArchetype.FOREST_WARDENS -> Color(0.35f, 0.85f, 0.4f, 1f)
            else                                  -> Color(0.7f, 0.45f, 0.95f, 1f)
        }

        // Name
        val nameLbl = Label(if (isUnlocked) arch.displayName else "🔒  ${arch.displayName}", skin, "cardTitle")
        nameLbl.color = accentColor
        card.add(nameLbl).padBottom(8f).left()
        card.row()

        if (!isUnlocked) {
            // Locked — show unlock condition only
            val condLbl = Label("Unlock: ${meta.unlockCondition(arch)}", skin, "small")
            condLbl.color = Color(0.55f, 0.55f, 0.55f, 1f)
            condLbl.wrap = true
            card.add(condLbl).width(220f).padBottom(16f).left()
            card.row()

            val lockedBtn = TextButton("LOCKED", skin, "locked")
            lockedBtn.isDisabled = true
            card.add(lockedBtn).width(200f).height(52f)
            return card
        }

        // Lore (wrapped)
        val loreLbl = Label(arch.lore, skin, "small")
        loreLbl.wrap = true
        card.add(loreLbl).width(220f).padBottom(12f).left()
        card.row()

        // Passive
        card.add(Label("PASSIVE", skin, "label2")).padBottom(2f).left()
        card.row()
        val passiveLbl = Label("${arch.passiveName}\n${arch.passiveDesc}", skin, "small")
        passiveLbl.color = Color(1f, 0.9f, 0.5f, 1f)
        passiveLbl.wrap = true
        card.add(passiveLbl).width(220f).padBottom(12f).left()
        card.row()

        // Starter units
        card.add(Label("STARTER UNITS", skin, "label2")).padBottom(2f).left()
        card.row()
        val unitNames = arch.starterUnitIds
            .mapNotNull { runCatching { com.autobattler.model.UnitType.fromId(it).displayName }.getOrNull() }
            .distinct()
            .joinToString(" • ")
        val unitsLbl = Label(unitNames, skin, "small")
        unitsLbl.wrap = true
        card.add(unitsLbl).width(220f).padBottom(20f).left()
        card.row()

        // Select button
        val btn = TextButton("SELECT", skin)
        btn.color = accentColor
        btn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                startRun(arch)
            }
        })
        card.add(btn).width(200f).height(52f)

        return card
    }

    private fun startRun(arch: RogueArchetype) {
        val worlds  = RogueWorldGenerator.generateWorlds()
        val run     = RogueRun(archetype = arch, worlds = worlds)
        val newMeta = game.rogueMeta.afterRunStart()
        game.rogueRun   = run
        game.rogueMeta  = newMeta
        game.saveManager.saveRogue(run)
        game.saveManager.saveMeta(newMeta)
        game.screen = RogueMapScreen(game)
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private fun addBtn(
        table: Table, label: String, @Suppress("UNUSED_PARAMETER") bg: Color, w: Float, h: Float, onClick: () -> Unit
    ) {
        val btn = TextButton(label, skin)
        btn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { onClick() }
        })
        table.add(btn).width(w).height(h).padBottom(8f)
    }

    // ─── Rendering ────────────────────────────────────────────────────────────

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.07f, 0.07f, 0.12f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) = stage.viewport.update(width, height, true)

    override fun dispose() {
        stage.dispose()
        skin.dispose()
        Fonts.disposeAll()
    }

    // ─── Skin ─────────────────────────────────────────────────────────────────

    private fun buildSkin(): Skin {
        val s = Skin()
        val titleFont     = Fonts.create(46, Color(0.95f, 0.88f, 0.3f, 1f), borderWidth = 2f)
        val subFont       = Fonts.create(20, Color.WHITE, borderWidth = 1f)
        val cardTitleFont = Fonts.create(24, Color.WHITE, borderWidth = 1f)
        val label2Font    = Fonts.create(14, Color(0.6f, 0.75f, 0.6f, 1f))
        val smallFont     = Fonts.create(14, Color(0.85f, 0.85f, 0.85f, 1f))
        val btnFont       = Fonts.create(18, Color.WHITE, borderWidth = 1f)

        s.add("default-font", btnFont)

        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        s.add("white", Texture(px)); px.dispose()

        s.add("title",     Label.LabelStyle(titleFont,     Color(0.95f, 0.88f, 0.3f, 1f)))
        s.add("sub",       Label.LabelStyle(subFont,       Color.WHITE))
        s.add("cardTitle", Label.LabelStyle(cardTitleFont, Color.WHITE))
        s.add("label2",    Label.LabelStyle(label2Font,    Color(0.6f, 0.75f, 0.6f, 1f)))
        s.add("small",     Label.LabelStyle(smallFont,     Color(0.85f, 0.85f, 0.85f, 1f)))
        s.add("default",   Label.LabelStyle(btnFont,       Color.WHITE))

        s.add("default", TextButton.TextButtonStyle().apply {
            up       = s.newDrawable("white", Color(0.22f, 0.3f, 0.22f, 1f))
            over     = s.newDrawable("white", Color(0.32f, 0.46f, 0.32f, 1f))
            down     = s.newDrawable("white", Color(0.14f, 0.2f, 0.14f, 1f))
            font     = btnFont
            fontColor = Color.WHITE
        })

        s.add("locked", TextButton.TextButtonStyle().apply {
            up       = s.newDrawable("white", Color(0.15f, 0.15f, 0.17f, 1f))
            over     = s.newDrawable("white", Color(0.15f, 0.15f, 0.17f, 1f))
            down     = s.newDrawable("white", Color(0.15f, 0.15f, 0.17f, 1f))
            disabled = s.newDrawable("white", Color(0.13f, 0.13f, 0.15f, 1f))
            font     = btnFont
            fontColor         = Color(0.35f, 0.35f, 0.35f, 1f)
            disabledFontColor = Color(0.35f, 0.35f, 0.35f, 1f)
        })

        return s
    }
}
