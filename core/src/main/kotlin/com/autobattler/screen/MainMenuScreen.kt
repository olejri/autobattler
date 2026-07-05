package com.autobattler.screen

import com.autobattler.AutobattlerGame
import com.autobattler.engine.Scenarios
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

class MainMenuScreen(private val game: AutobattlerGame) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())
    private lateinit var skin: Skin

    override fun show() {
        Gdx.input.inputProcessor = stage

        // Build crisp fonts after GL context is ready
        skin = createSkin()

        val root = Table()
        root.setFillParent(true)
        stage.addActor(root)

        root.add(Label("AUTOBATTLER", skin, "title")).padBottom(30f)
        root.row()
        val welcomeText = game.commander?.let { "Welcome back, ${it.name}!" } ?: "Welcome, Commander!"
        root.add(Label(welcomeText, skin, "sub")).padBottom(50f)
        root.row()

        addButton(root, "PLAY", 240f, 64f) {
            if (game.commander != null) {
                game.screen = CampaignScreen(game)
            } else {
                game.screen = CommanderNameScreen(game)
            }
        }
        addButton(root, "TUTORIAL", 240f, 64f) {
            game.screen = BattleScreen(game, Scenarios.tutorialSequence[0])
        }
        addButton(root, "SANDBOX", 240f, 64f) {
            game.screen = SandboxScreen(game)
        }
        root.row()

        root.add(Label("[DEV] Quick Test:", skin, "small").also { it.color = Color.GRAY }).padTop(16f).padBottom(4f)
        root.row()
        for (i in Scenarios.tutorialSequence.indices) {
            addButton(root, "Tutorial ${i + 1}", 200f, 40f) {
                game.screen = BattleScreen(game, Scenarios.tutorialSequence[i])
            }
        }
        addButton(root, "QUIT", 240f, 64f) { Gdx.app.exit() }
    }

    private fun addButton(root: Table, label: String, w: Float, h: Float, onClick: () -> Unit) {
        val btn = TextButton(label, skin)
        btn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { onClick() }
        })
        root.add(btn).width(w).height(h).padBottom(12f)
        root.row()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.07f, 0.07f, 0.12f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
        Fonts.disposeAll()
    }

    private fun createSkin(): Skin {
        val skin = Skin()

        val titleFont = Fonts.create(52, Color(0.95f, 0.88f, 0.3f, 1f), borderWidth = 2f)
        val subFont   = Fonts.create(22, Color.WHITE, borderWidth = 1f)
        val btnFont   = Fonts.create(20, Color.WHITE, borderWidth = 1f)
        val smallFont = Fonts.create(16, Color.LIGHT_GRAY)

        skin.add("default-font", btnFont)

        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        skin.add("white", Texture(px)); px.dispose()

        skin.add("title",  Label.LabelStyle(titleFont, Color(0.95f, 0.88f, 0.3f, 1f)))
        skin.add("sub",    Label.LabelStyle(subFont,   Color.WHITE))
        skin.add("small",  Label.LabelStyle(smallFont, Color.LIGHT_GRAY))
        skin.add("default",Label.LabelStyle(btnFont,   Color.WHITE))

        skin.add("default", TextButton.TextButtonStyle().apply {
            up   = skin.newDrawable("white", Color(0.22f, 0.3f, 0.22f, 1f))
            over = skin.newDrawable("white", Color(0.32f, 0.46f, 0.32f, 1f))
            down = skin.newDrawable("white", Color(0.14f, 0.2f, 0.14f, 1f))
            font = btnFont
            fontColor = Color.WHITE
        })

        return skin
    }
}
