package com.autobattler.screen

import com.autobattler.AutobattlerGame
import com.autobattler.model.*
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

/**
 * Shown after winning an Elite or Boss node.
 * The player chooses one of three randomly selected relics.
 * After picking, they return to the map.
 */
class RogueRewardScreen(
    private val game: AutobattlerGame,
    private val sourceNode: RogueNode,
    private val isElite: Boolean
) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())
    private lateinit var skin: Skin

    override fun show() {
        Gdx.input.inputProcessor = stage
        skin = buildSkin()

        val run = game.rogueRun ?: run { game.screen = MainMenuScreen(game); return }

        val offeredRelics = pickRelicChoices(run)

        val root = Table()
        root.setFillParent(true)
        root.pad(40f)
        stage.addActor(root)

        val titleText = if (isElite) "ELITE CLEARED — Choose a Relic" else "BOSS DEFEATED — Choose a Relic"
        root.add(Label(titleText, skin, "title")).padBottom(10f).colspan(3).center()
        root.row()
        root.add(Label("Your forces prevailed. Pick one reward to carry forward.", skin, "sub"))
            .padBottom(30f).colspan(3).center()
        root.row()

        for (relic in offeredRelics) {
            root.add(buildRelicCard(relic, run)).pad(10f)
        }
        root.row().padTop(20f)

        // Option to skip (take gold instead)
        val skipGold = 80 + sourceNode.depth * 20
        val skipBtn = TextButton("SKIP — Take ${skipGold}g instead", skin, "skip")
        skipBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val updated = run.copy(gold = run.gold + skipGold)
                game.rogueRun = updated
                game.saveManager.saveRogue(updated)
                game.screen = RogueMapScreen(game)
            }
        })
        root.add(skipBtn).colspan(3).height(48f).padTop(8f)
    }

    private fun pickRelicChoices(run: RogueRun): List<RelicType> {
        val seed = run.worlds[run.currentWorldIndex].seed + sourceNode.id * 71L + run.completedNodeKeys.size * 13L
        val rng  = java.util.Random(seed)
        val already = run.relics.toSet()
        val available = RelicType.values().filter { it !in already }
        return if (available.size <= 3) {
            available.take(3)
        } else {
            available.shuffled(rng).take(3)
        }
    }

    private fun buildRelicCard(relic: RelicType, run: RogueRun): Table {
        val card = Table()
        card.background = skin.newDrawable("white", Color(0.1f, 0.12f, 0.15f, 1f))
        card.pad(16f)

        card.add(Label(relic.displayName, skin, "relicName")).padBottom(8f).left()
        card.row()

        val descLbl = Label(relic.description, skin, "relicDesc")
        descLbl.wrap = true
        card.add(descLbl).width(200f).padBottom(16f).left()
        card.row()

        val btn = TextButton("TAKE", skin)
        btn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val updated = run.copy(relics = run.relics + relic)
                game.rogueRun = updated
                game.saveManager.saveRogue(updated)
                game.screen = RogueMapScreen(game)
            }
        })
        card.add(btn).width(160f).height(46f)
        return card
    }

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

    private fun buildSkin(): Skin {
        val s = Skin()
        val titleFont    = Fonts.create(36, Color(0.95f, 0.88f, 0.3f, 1f), borderWidth = 2f)
        val subFont      = Fonts.create(18, Color.WHITE, borderWidth = 1f)
        val relicName    = Fonts.create(20, Color(0.9f, 0.8f, 0.45f, 1f), borderWidth = 1f)
        val relicDesc    = Fonts.create(14, Color(0.85f, 0.85f, 0.85f, 1f))
        val btnFont      = Fonts.create(18, Color.WHITE, borderWidth = 1f)

        s.add("default-font", btnFont)
        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        s.add("white", Texture(px)); px.dispose()

        s.add("title",     Label.LabelStyle(titleFont, Color(0.95f, 0.88f, 0.3f, 1f)))
        s.add("sub",       Label.LabelStyle(subFont,   Color.WHITE))
        s.add("relicName", Label.LabelStyle(relicName, Color(0.9f, 0.8f, 0.45f, 1f)))
        s.add("relicDesc", Label.LabelStyle(relicDesc, Color(0.85f, 0.85f, 0.85f, 1f)))
        s.add("default",   Label.LabelStyle(btnFont,   Color.WHITE))

        s.add("default", TextButton.TextButtonStyle().apply {
            up   = s.newDrawable("white", Color(0.2f, 0.38f, 0.2f, 1f))
            over = s.newDrawable("white", Color(0.3f, 0.52f, 0.3f, 1f))
            down = s.newDrawable("white", Color(0.14f, 0.26f, 0.14f, 1f))
            font = btnFont; fontColor = Color.WHITE
        })
        s.add("skip", TextButton.TextButtonStyle().apply {
            up   = s.newDrawable("white", Color(0.25f, 0.22f, 0.18f, 1f))
            over = s.newDrawable("white", Color(0.36f, 0.32f, 0.26f, 1f))
            down = s.newDrawable("white", Color(0.18f, 0.15f, 0.12f, 1f))
            font = btnFont; fontColor = Color(0.9f, 0.8f, 0.5f, 1f)
        })
        return s
    }
}
