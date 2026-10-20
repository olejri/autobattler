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

/** Shown when the player defeats all 3 world bosses. */
class RogueVictoryScreen(private val game: AutobattlerGame) : ScreenAdapter() {
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

        root.add(Label("VICTORY!", skin, "title")).padBottom(12f).center()
        root.row()
        root.add(Label("You have conquered all three worlds!", skin, "sub")).padBottom(6f).center()
        root.row()

        if (run != null) {
            root.add(Label(
                "Commander: ${run.archetype.displayName}" +
                "   •   ${run.completedNodeKeys.size} nodes cleared" +
                "   •   ${run.relics.size} relics collected",
                skin, "stats"
            )).padBottom(8f).center()
            root.row()

            if (run.relics.isNotEmpty()) {
                val relicLbl = Label("Relics: ${run.relics.joinToString(" | ") { it.displayName }}", skin, "relics")
                relicLbl.wrap = true
                root.add(relicLbl).width(600f).padBottom(16f).center()
                root.row()
            }
        }

        // Show newly unlocked archetypes
        val meta = game.rogueMeta
        val justUnlocked = mutableListOf<String>()
        if (meta.isUnlocked(com.autobattler.model.RogueArchetype.FOREST_WARDENS))
            justUnlocked += "Forest Wardens"
        if (meta.isUnlocked(com.autobattler.model.RogueArchetype.SHADOW_CULT))
            justUnlocked += "Shadow Cult"
        // All three are now unlocked since player beat world 3
        root.add(Label("✦ All factions unlocked — every archetype is now available!", skin, "unlock"))
            .padBottom(4f).center()
        root.row()

        // Lifetime stats
        root.add(Label(
            "Total wins: ${meta.totalRunsCompleted + 1}  •  Best: ${meta.bestRunNodesCleared} nodes cleared",
            skin, "stats"
        )).padBottom(20f).center()
        root.row()

        addBtn(root, "NEW RUN") {
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
        Gdx.gl.glClearColor(0.05f, 0.08f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(w: Int, h: Int) = stage.viewport.update(w, h, true)
    override fun dispose() { stage.dispose(); skin.dispose(); Fonts.disposeAll() }

    private fun buildSkin(): Skin {
        val s = Skin()
        val tf  = Fonts.create(52, Color(0.9f, 0.95f, 0.3f, 1f), borderWidth = 2f)
        val sf  = Fonts.create(20, Color.WHITE, borderWidth = 1f)
        val stf = Fonts.create(15, Color(0.75f, 0.9f, 0.75f, 1f))
        val rf  = Fonts.create(13, Color(0.85f, 0.8f, 0.5f, 1f))
        val bf  = Fonts.create(20, Color.WHITE, borderWidth = 1f)
        val uf  = Fonts.create(16, Color(0.5f, 0.95f, 0.7f, 1f), borderWidth = 1f)
        s.add("default-font", bf)
        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        s.add("white", Texture(px)); px.dispose()
        s.add("title",   Label.LabelStyle(tf,  Color(0.9f, 0.95f, 0.3f, 1f)))
        s.add("sub",     Label.LabelStyle(sf,  Color.WHITE))
        s.add("stats",   Label.LabelStyle(stf, Color(0.75f, 0.9f, 0.75f, 1f)))
        s.add("relics",  Label.LabelStyle(rf,  Color(0.85f, 0.8f, 0.5f, 1f)))
        s.add("unlock",  Label.LabelStyle(uf,  Color(0.5f, 0.95f, 0.7f, 1f)))
        s.add("default", Label.LabelStyle(bf,  Color.WHITE))
        s.add("default", TextButton.TextButtonStyle().apply {
            up = s.newDrawable("white", Color(0.22f, 0.42f, 0.22f, 1f))
            over = s.newDrawable("white", Color(0.32f, 0.56f, 0.32f, 1f))
            down = s.newDrawable("white", Color(0.14f, 0.28f, 0.14f, 1f))
            font = bf; fontColor = Color.WHITE
        })
        return s
    }
}
