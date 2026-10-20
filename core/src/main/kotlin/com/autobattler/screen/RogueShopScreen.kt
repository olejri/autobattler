package com.autobattler.screen

import com.autobattler.AutobattlerGame
import com.autobattler.model.UnitType
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
 * Shop node screen — the player can permanently unlock one extra unit type
 * to add to their run's shop roster, for a gold cost.
 */
class RogueShopScreen(private val game: AutobattlerGame) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())
    private lateinit var skin: Skin

    override fun show() {
        Gdx.input.inputProcessor = stage
        skin = buildSkin()

        val run = game.rogueRun ?: run { game.screen = MainMenuScreen(game); return }

        val root = Table()
        root.setFillParent(true)
        root.pad(40f)
        stage.addActor(root)

        root.add(Label("SHOP", skin, "title")).padBottom(8f).colspan(4).center()
        root.row()
        root.add(Label("Gold: ${run.gold}g", skin, "gold")).padBottom(6f).colspan(4).center()
        root.row()
        root.add(Label("Unlock a unit type to add to your battle shop for the rest of this run.", skin, "sub"))
            .padBottom(24f).colspan(4).center()
        root.row()

        // Offer units not yet in the shop
        val allUnlockable = listOf(
            "knight", "knight_silver", "healer_silver", "ranged_silver",
            "wizard", "wizard_silver", "griffin_rider", "farmer", "melee_silver",
            "tower", "wall", "gate"
        )
        val alreadyOwned = run.shopUnitIds.toSet()
        val forSale = allUnlockable.filter { it !in alreadyOwned }
            .take(6)

        for ((i, typeId) in forSale.withIndex()) {
            val uType = runCatching { UnitType.fromId(typeId) }.getOrNull() ?: continue
            val cost  = (uType.cost * 1.5f).toInt().coerceAtLeast(80)
            val canAfford = run.gold >= cost

            val card = Table()
            card.background = skin.newDrawable("white", Color(0.1f, 0.12f, 0.14f, 1f))
            card.pad(12f)

            card.add(Label(uType.displayName, skin, "itemName")).padBottom(4f).left()
            card.row()
            card.add(Label("HP:${uType.maxHp}  PWR:${uType.power}  SPD:${uType.speed}", skin, "stat"))
                .padBottom(8f).left()
            card.row()

            val btn = TextButton("${cost}g — BUY", skin, if (canAfford) "default" else "disabled")
            if (canAfford) {
                btn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        val updated = run.copy(
                            gold = run.gold - cost,
                            unlockedUnitTypeIds = run.unlockedUnitTypeIds + typeId
                        )
                        game.rogueRun = updated
                        game.saveManager.saveRogue(updated)
                        game.screen = RogueMapScreen(game)
                    }
                })
            }
            card.add(btn).width(180f).height(44f)

            root.add(card).pad(8f).top()
            if ((i + 1) % 3 == 0) root.row()
        }

        root.row().padTop(24f)
        val skipBtn = TextButton("LEAVE WITHOUT BUYING", skin, "skip")
        skipBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = RogueMapScreen(game)
            }
        })
        root.add(skipBtn).colspan(4).height(48f)
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
        val s   = Skin()
        val tf  = Fonts.create(38, Color(0.95f, 0.88f, 0.3f, 1f), borderWidth = 2f)
        val gf  = Fonts.create(22, Color(0.85f, 0.95f, 0.35f, 1f), borderWidth = 1f)
        val sf  = Fonts.create(17, Color.WHITE, borderWidth = 1f)
        val inf = Fonts.create(14, Color(0.8f, 0.8f, 0.8f, 1f))
        val bf  = Fonts.create(16, Color.WHITE, borderWidth = 1f)
        s.add("default-font", bf)
        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        s.add("white", Texture(px)); px.dispose()
        s.add("title",    Label.LabelStyle(tf,  Color(0.95f, 0.88f, 0.3f, 1f)))
        s.add("gold",     Label.LabelStyle(gf,  Color(0.85f, 0.95f, 0.35f, 1f)))
        s.add("sub",      Label.LabelStyle(sf,  Color.WHITE))
        s.add("itemName", Label.LabelStyle(sf,  Color.WHITE))
        s.add("stat",     Label.LabelStyle(inf, Color(0.7f, 0.85f, 0.7f, 1f)))
        s.add("default",  Label.LabelStyle(bf,  Color.WHITE))
        s.add("default", TextButton.TextButtonStyle().apply {
            up   = s.newDrawable("white", Color(0.22f, 0.42f, 0.22f, 1f))
            over = s.newDrawable("white", Color(0.32f, 0.56f, 0.32f, 1f))
            down = s.newDrawable("white", Color(0.14f, 0.28f, 0.14f, 1f))
            font = bf; fontColor = Color.WHITE
        })
        s.add("disabled", TextButton.TextButtonStyle().apply {
            up   = s.newDrawable("white", Color(0.18f, 0.18f, 0.18f, 1f))
            over = s.newDrawable("white", Color(0.18f, 0.18f, 0.18f, 1f))
            down = s.newDrawable("white", Color(0.18f, 0.18f, 0.18f, 1f))
            font = bf; fontColor = Color(0.4f, 0.4f, 0.4f, 1f)
        })
        s.add("skip", TextButton.TextButtonStyle().apply {
            up   = s.newDrawable("white", Color(0.22f, 0.22f, 0.26f, 1f))
            over = s.newDrawable("white", Color(0.32f, 0.32f, 0.38f, 1f))
            down = s.newDrawable("white", Color(0.15f, 0.15f, 0.18f, 1f))
            font = bf; fontColor = Color(0.75f, 0.75f, 0.75f, 1f)
        })
        return s
    }
}
