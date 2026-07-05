package com.autobattler.screen

import com.autobattler.AutobattlerGame
import com.autobattler.model.Commander
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
 * Shown the first time a player hits PLAY — asks for their commander name.
 * Creates a fresh Commander, saves it, then transitions to the CampaignScreen.
 */
class CommanderNameScreen(private val game: AutobattlerGame) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())
    private lateinit var skin: Skin
    private lateinit var nameField: TextField

    override fun show() {
        Gdx.input.inputProcessor = stage
        skin = createSkin()

        val root = Table()
        root.setFillParent(true)
        stage.addActor(root)

        root.add(Label("AUTOBATTLER", skin, "title")).padBottom(16f)
        root.row()
        root.add(Label("What's your name, Commander?", skin, "sub")).padBottom(40f)
        root.row()

        nameField = TextField("", skin)
        nameField.messageText = "Enter your name..."
        nameField.maxLength = 20
        root.add(nameField).width(320f).height(56f).padBottom(24f)
        root.row()

        val beginBtn = TextButton("BEGIN CAMPAIGN", skin, "begin")
        beginBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val name = nameField.text.trim()
                if (name.isNotEmpty()) {
                    val commander = Commander(name = name)
                    game.commander = commander
                    game.saveManager.save(commander)
                    game.screen = CampaignScreen(game)
                }
            }
        })
        root.add(beginBtn).width(280f).height(68f).padBottom(12f)
        root.row()

        val backBtn = TextButton("BACK", skin)
        backBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = MainMenuScreen(game)
            }
        })
        root.add(backBtn).width(200f).height(44f)

        // Auto-focus the text field
        stage.keyboardFocus = nameField
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

    private fun createSkin(): Skin {
        val skin = Skin()
        val titleFont  = Fonts.create(52, Color(0.95f, 0.88f, 0.3f, 1f), borderWidth = 2f)
        val subFont    = Fonts.create(22, Color.WHITE, borderWidth = 1f)
        val btnFont    = Fonts.create(20, Color.WHITE, borderWidth = 1f)
        val beginFont  = Fonts.create(24, Color.WHITE, borderWidth = 1.5f)
        val inputFont  = Fonts.create(20, Color.WHITE)

        skin.add("default-font", btnFont)
        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        skin.add("white", Texture(px)); px.dispose()

        skin.add("title",   Label.LabelStyle(titleFont, Color(0.95f, 0.88f, 0.3f, 1f)))
        skin.add("sub",     Label.LabelStyle(subFont,   Color.WHITE))
        skin.add("default", Label.LabelStyle(btnFont,   Color.WHITE))

        val btnStyle = TextButton.TextButtonStyle().apply {
            up   = skin.newDrawable("white", Color(0.22f, 0.3f, 0.22f, 1f))
            over = skin.newDrawable("white", Color(0.32f, 0.46f, 0.32f, 1f))
            down = skin.newDrawable("white", Color(0.14f, 0.2f, 0.14f, 1f))
            font = btnFont; fontColor = Color.WHITE
        }
        skin.add("default", btnStyle)
        skin.add("begin", TextButton.TextButtonStyle().apply {
            up   = skin.newDrawable("white", Color(0.18f, 0.35f, 0.18f, 1f))
            over = skin.newDrawable("white", Color(0.28f, 0.52f, 0.28f, 1f))
            down = skin.newDrawable("white", Color(0.12f, 0.22f, 0.12f, 1f))
            font = beginFont; fontColor = Color(0.85f, 1f, 0.85f, 1f)
        })

        skin.add("default", TextField.TextFieldStyle().apply {
            font = inputFont; fontColor = Color.WHITE
            background        = skin.newDrawable("white", Color(0.13f, 0.13f, 0.22f, 1f))
            focusedBackground = skin.newDrawable("white", Color(0.18f, 0.18f, 0.32f, 1f))
            cursor            = skin.newDrawable("white", Color.WHITE)
            selection         = skin.newDrawable("white", Color(0.3f, 0.3f, 0.6f, 0.8f))
            messageFontColor  = Color(0.45f, 0.45f, 0.55f, 1f)
            messageFont       = inputFont
        })

        return skin
    }
}

