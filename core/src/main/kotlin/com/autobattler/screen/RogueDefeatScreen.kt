package com.autobattler.screen

import com.autobattler.AutobattlerGame
import com.autobattler.render.Fonts
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport

/** Shown when the player loses a battle in rogue mode (permadeath). */
class RogueDefeatScreen(private val game: AutobattlerGame) : ScreenAdapter() {
    private val stage = Stage(ScreenViewport())
    private lateinit var skin: Skin

    override fun show() {
        Gdx.input.inputProcessor = stage
        skin = buildSkin()
        val run = game.rogueRun

        val root = Table()
        root.setFillParent(true)
        root.pad(60f)
        stage.addActor(root)

        root.add(Label("DEFEAT", skin, "title")).padBottom(12f).center()
        root.row()
        root.add(Label("Your forces were broken. The run is over.", skin, "sub")).padBottom(6f).center()
        root.row()

        if (run != null) {
            root.add(Label(
                "${run.archetype.displayName}  •  ${run.currentWorldName}" +
                "  •  ${run.completedNodeKeys.size} nodes cleared" +
                "  •  ${run.relics.size} relics",
                skin, "stats"
            )).padBottom(10f).center()
            root.row()
        }

        // Show any newly unlocked archetypes from meta
        val meta = game.rogueMeta
        val unlocked = meta.unlockedArchetypes - setOf(com.autobattler.model.RogueArchetype.IRON_LEGION)
        if (unlocked.isNotEmpty()) {
            val names = unlocked.joinToString(", ") { it.displayName }
            root.add(Label("✦ Unlocked: $names", skin, "unlock"))
                .padBottom(6f).center()
            root.row()
        }

        // Best run reminder
        if (meta.bestRunNodesCleared > 0) {
            root.add(Label(
                "Best run: ${meta.bestRunNodesCleared} nodes  •  ${meta.bestRunWorldsBeaten} worlds  •  runs: ${meta.totalRunsStarted}",
                skin, "stats"
            )).padBottom(20f).center()
            root.row()
        } else {
            root.row().padBottom(20f)
        }

        addBtn(root, "TRY AGAIN") {
            game.saveManager.deleteRogue()
            game.rogueRun = null
            game.screen = RogueCommanderSelectScreen(game)
        }
        root.row().padBottom(8f)
        addBtn(root, "MAIN MENU") {
            game.saveManager.deleteRogue()
            game.rogueRun = null
            game.screen = MainMenuScreen(game)
        }
    }

    private fun addBtn(table: Table, label: String, onClick: () -> Unit) {
        val btn = TextButton(label, skin)
        btn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { onClick() }
        })
        table.add(btn).width(240f).height(56f).padBottom(12f)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.04f, 0.04f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(w: Int, h: Int) = stage.viewport.update(w, h, true)
    override fun dispose() { stage.dispose(); skin.dispose(); Fonts.disposeAll() }

    private fun buildSkin(): Skin {
        val s = Skin()
        val tf  = Fonts.create(52, Color(0.9f, 0.2f, 0.2f, 1f), borderWidth = 2f)
        val sf  = Fonts.create(20, Color.WHITE, borderWidth = 1f)
        val stf = Fonts.create(15, Color(0.85f, 0.7f, 0.7f, 1f))
        val bf  = Fonts.create(20, Color.WHITE, borderWidth = 1f)
        val uf  = Fonts.create(15, Color(0.5f, 0.95f, 0.7f, 1f), borderWidth = 1f)
        s.add("default-font", bf)
        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        s.add("white", Texture(px)); px.dispose()
        s.add("title",   Label.LabelStyle(tf,  Color(0.9f, 0.2f, 0.2f, 1f)))
        s.add("sub",     Label.LabelStyle(sf,  Color.WHITE))
        s.add("stats",   Label.LabelStyle(stf, Color(0.85f, 0.7f, 0.7f, 1f)))
        s.add("unlock",  Label.LabelStyle(uf,  Color(0.5f, 0.95f, 0.7f, 1f)))
        s.add("default", Label.LabelStyle(bf,  Color.WHITE))
        s.add("default", TextButton.TextButtonStyle().apply {
            up   = s.newDrawable("white", Color(0.38f, 0.18f, 0.18f, 1f))
            over = s.newDrawable("white", Color(0.52f, 0.26f, 0.26f, 1f))
            down = s.newDrawable("white", Color(0.26f, 0.12f, 0.12f, 1f))
            font = bf; fontColor = Color.WHITE
        })
        return s
    }
}
