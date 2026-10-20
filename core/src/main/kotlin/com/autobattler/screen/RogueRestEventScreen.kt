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

/** Generic text screen used for Rest Site and Event nodes. */
class RogueRestEventScreen(
    private val game: AutobattlerGame,
    private val title: String,
    private val body: String,
    private val onContinue: () -> Unit
) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())
    private lateinit var skin: Skin

    override fun show() {
        Gdx.input.inputProcessor = stage
        skin = buildSkin()

        val root = Table()
        root.setFillParent(true)
        root.pad(60f)
        stage.addActor(root)

        root.add(Label(title, skin, "title")).padBottom(20f).center()
        root.row()

        val bodyLbl = Label(body, skin, "body")
        bodyLbl.wrap = true
        root.add(bodyLbl).width(480f).padBottom(40f).center()
        root.row()

        val continueBtn = TextButton("CONTINUE", skin)
        continueBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { onContinue() }
        })
        root.add(continueBtn).width(220f).height(56f)
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
        val titleFont = Fonts.create(34, Color(0.95f, 0.88f, 0.3f, 1f), borderWidth = 2f)
        val bodyFont  = Fonts.create(18, Color.WHITE, borderWidth = 1f)
        val btnFont   = Fonts.create(20, Color.WHITE, borderWidth = 1f)
        s.add("default-font", btnFont)
        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        s.add("white", Texture(px)); px.dispose()
        s.add("title",   Label.LabelStyle(titleFont, Color(0.95f, 0.88f, 0.3f, 1f)))
        s.add("body",    Label.LabelStyle(bodyFont,  Color.WHITE))
        s.add("default", Label.LabelStyle(btnFont,   Color.WHITE))
        s.add("default", TextButton.TextButtonStyle().apply {
            up   = s.newDrawable("white", Color(0.22f, 0.3f, 0.22f, 1f))
            over = s.newDrawable("white", Color(0.32f, 0.46f, 0.32f, 1f))
            down = s.newDrawable("white", Color(0.14f, 0.2f, 0.14f, 1f))
            font = btnFont; fontColor = Color.WHITE
        })
        return s
    }
}
