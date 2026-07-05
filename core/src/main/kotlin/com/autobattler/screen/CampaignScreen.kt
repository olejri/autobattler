package com.autobattler.screen

import com.autobattler.AutobattlerGame
import com.autobattler.engine.Scenarios
import com.autobattler.model.Commander
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
 * Campaign hub screen.
 *
 * Layout:
 *   ┌─────────────────┬──────────────────────────────────────────┐
 *   │  CommanderPanel │  WORLD 1                                 │
 *   │  (left sidebar) │  [1-1]──[1-2]──[1-3]──[1-4]──[1-5]    │
 *   │                 │  ─────────────────────────────────────── │
 *   │  portrait       │  Selected level info                     │
 *   │  name / level   │  Recruitment                             │
 *   │  XP / gold      │  [DEPLOY]   [MENU]                      │
 *   │  roster icons   │                                          │
 *   └─────────────────┴──────────────────────────────────────────┘
 */
class CampaignScreen(private val game: AutobattlerGame) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())
    private lateinit var skin: Skin

    private var selectedLevel: Int = -1

    private lateinit var commanderPanel: CommanderPanel
    private lateinit var infoArea: Table

    // ─── Screen lifecycle ────────────────────────────────────────────────────────

    override fun show() {
        Gdx.input.inputProcessor = stage
        skin = createSkin()
        selectedLevel = game.commander?.currentLevel ?: 1
        buildLayout()
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
        commanderPanel.dispose()
        Fonts.disposeAll()
    }

    // ─── Full layout ─────────────────────────────────────────────────────────────

    private fun buildLayout() {
        stage.clear()
        val commander = game.commander ?: run { game.screen = MainMenuScreen(game); return }

        val root = Table()
        root.setFillParent(true)
        stage.addActor(root)

        // Left sidebar
        if (::commanderPanel.isInitialized) commanderPanel.dispose()
        commanderPanel = CommanderPanel(commander)
        root.add(commanderPanel).top().width(210f).fillY().pad(12f)

        // Right content
        val right = Table()
        right.top().padTop(24f).padLeft(16f).padRight(24f)
        root.add(right).expand().fill().top()

        right.add(Label("WORLD  ${commander.currentWorld}", skin, "worldTitle"))
            .padBottom(18f).row()

        right.add(buildNodeMap(commander)).padBottom(14f).row()

        val sep = Image(skin.newDrawable("white", Color(0.3f, 0.3f, 0.5f, 0.45f)))
        right.add(sep).fillX().height(1f).padBottom(16f).row()

        infoArea = Table()
        infoArea.top().left()
        right.add(infoArea).left().fillX().row()
        populateInfoArea(commander)
    }

    // ─── Node map ────────────────────────────────────────────────────────────────

    private fun buildNodeMap(commander: Commander): Table {
        val map = Table()
        for (level in 1..5) {
            map.add(buildNode(level, commander)).size(88f, 88f)
            if (level < 5) {
                val done = commander.hasCompleted(1, level)
                val lineColor = if (done) Color(0.3f, 0.65f, 0.3f, 1f) else Color(0.22f, 0.22f, 0.28f, 1f)
                map.add(Image(skin.newDrawable("white", lineColor))).width(44f).height(3f)
            }
        }
        return map
    }

    private fun buildNode(level: Int, commander: Commander): Table {
        val isCompleted = commander.hasCompleted(1, level)
        val isCurrent   = level == commander.currentLevel
        val isLocked    = level > commander.currentLevel
        val isBoss      = level == 5
        val isSelected  = level == selectedLevel

        val bgColor = when {
            isSelected && isBoss && isCurrent  -> Color(0.55f, 0.08f, 0.06f, 1f)
            isSelected && isCompleted          -> Color(0.14f, 0.44f, 0.14f, 1f)
            isSelected                         -> Color(0.48f, 0.34f, 0.05f, 1f)
            isCompleted                        -> Color(0.1f,  0.28f, 0.1f,  1f)
            isCurrent && isBoss                -> Color(0.35f, 0.05f, 0.04f, 1f)
            isCurrent                          -> Color(0.32f, 0.22f, 0.03f, 1f)
            else                               -> Color(0.1f,  0.1f,  0.14f, 1f)
        }
        val borderColor = when {
            isSelected && isBoss -> Color(1f,   0.3f,  0.2f,  1f)
            isSelected           -> Color(1f,   0.85f, 0.2f,  1f)
            isCompleted          -> Color(0.3f, 0.7f,  0.3f,  1f)
            isCurrent && isBoss  -> Color(0.8f, 0.2f,  0.1f,  1f)
            isCurrent            -> Color(0.85f,0.65f, 0.1f,  1f)
            else                 -> Color(0.22f,0.22f, 0.28f, 1f)
        }

        val inner = Table()
        inner.background = skin.newDrawable("white", bgColor)
        inner.center()

        val statusMark = when {
            isCompleted -> "✓"
            isLocked    -> "🔒"
            isBoss      -> "☠"
            else        -> "►"
        }
        inner.add(Label(statusMark, skin, if (isSelected) "nodeTextBright" else "nodeText"))
            .padTop(6f).row()
        inner.add(Label("1-$level", skin, if (isSelected) "nodeIdBright" else "nodeId")).row()
        inner.add(Label(nodeShortName(level), skin, "nodeName")).padBottom(4f).row()

        // Wrap with colored border
        val wrapper = Table()
        wrapper.background = skin.newDrawable("white", borderColor)
        wrapper.add(inner).expand().fill().pad(2f)

        if (!isLocked) {
            val lvl = level
            wrapper.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (selectedLevel != lvl) {
                        selectedLevel = lvl
                        buildLayout()   // rebuild node highlights + info
                    }
                }
            })
        }
        return wrapper
    }

    private fun nodeShortName(level: Int) = when (level) {
        1 -> "Ford"; 2 -> "Ambush"; 3 -> "Forest"; 4 -> "Siege"; 5 -> "BOSS"; else -> "$level"
    }

    // ─── Info area ───────────────────────────────────────────────────────────────

    private fun populateInfoArea(commander: Commander) {
        infoArea.clearChildren()

        val scenario = if (commander.currentWorld == 1)
            Scenarios.campaignBattle(1, selectedLevel)
        else null

        if (scenario == null) {
            infoArea.add(Label("World ${commander.currentWorld} — Coming Soon!", skin, "sub"))
                .padBottom(8f).row()
            infoArea.add(Label("You've conquered World 1. More content on the way!", skin, "small"))
                .padBottom(28f).row()
            addMenuButton()
            return
        }

        val isLocked    = selectedLevel > commander.currentLevel
        val isCompleted = commander.hasCompleted(1, selectedLevel)
        val isBoss      = selectedLevel == 5

        // Mission header
        infoArea.add(Label(scenario.name, skin, if (isBoss) "boss" else "header"))
            .left().padBottom(4f).row()

        val descLabel = Label(scenario.description, skin, "small")
        descLabel.wrap = true
        infoArea.add(descLabel).left().width(600f).padBottom(10f).row()

        // Budget + reward
        val enemyCount = scenario.enemyPlacements.size
        infoArea.add(Label(
            "Your gold: ${commander.gold}g   |   Win: +${scenario.goldReward}g  +${enemyCount * BattleScreen.GOLD_PER_KILL}g kills  +${scenario.xpReward} XP",
            skin, "reward"
        )).left().padBottom(8f).row()

        // Enemy preview
        val enemyGroups = scenario.enemyPlacements
            .groupBy { it.first.displayName }
            .map { (name, list) -> "${list.size}× $name" }
            .joinToString("   ")
        infoArea.add(Label("Enemies:  $enemyGroups", skin, "small")).left().padBottom(16f).row()

        // Separator
        infoArea.add(Image(skin.newDrawable("white", Color(0.25f, 0.25f, 0.4f, 0.4f))))
            .fillX().height(1f).padBottom(14f).row()

        // Recruitment
        infoArea.add(Label("Recruitment", skin, "header")).left().padBottom(8f).row()
        val recruitTable = Table().left()
        for (type in UnitType.purchasable) {
            val isUnlocked = type.id in commander.unlockedUnitTypeIds
            val levelMet   = commander.level >= type.unlockLevel
            val canAfford  = commander.gold >= type.unlockCost
            when {
                type.unlockLevel == 1 || isUnlocked ->
                    recruitTable.add(Label("✓  ${type.displayName}  (${type.cost}g/battle)", skin, "unlocked"))
                        .left().padBottom(4f).row()
                !levelMet ->
                    recruitTable.add(Label("🔒  ${type.displayName}  — Level ${type.unlockLevel} required", skin, "locked"))
                        .left().padBottom(4f).row()
                else -> {
                    val btn = TextButton(
                        "RECRUIT  ${type.displayName}   ${type.unlockCost}g",
                        skin, if (canAfford) "recruit" else "recruitDim"
                    )
                    if (canAfford) {
                        btn.addListener(object : ClickListener() {
                            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                                val updated = commander.unlock(type)
                                game.commander = updated
                                game.saveManager.save(updated)
                                commanderPanel.refresh(updated)
                                populateInfoArea(updated)
                            }
                        })
                    }
                    recruitTable.add(btn).left().width(360f).height(42f).padBottom(4f).row()
                }
            }
        }
        infoArea.add(recruitTable).left().padBottom(20f).row()

        // Buttons
        val btnRow = Table()
        if (!isLocked) {
            val deployLabel = if (isCompleted) "⚔  REPLAY  1-$selectedLevel" else "⚔  DEPLOY"
            val deployBtn = TextButton(deployLabel, skin, if (isCompleted) "deployAlt" else "deploy")
            deployBtn.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    game.screen = BattleScreen(game, scenario, commander)
                }
            })
            btnRow.add(deployBtn).width(260f).height(68f).padRight(16f)
        }
        val menuBtn = TextButton("MAIN MENU", skin)
        menuBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = MainMenuScreen(game)
            }
        })
        btnRow.add(menuBtn).width(180f).height(46f)
        infoArea.add(btnRow).left().padTop(4f).row()
    }

    private fun addMenuButton() {
        val menuBtn = TextButton("MAIN MENU", skin)
        menuBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = MainMenuScreen(game)
            }
        })
        infoArea.add(menuBtn).width(180f).height(46f).padTop(4f).row()
    }

    // ─── Skin ────────────────────────────────────────────────────────────────────

    private fun createSkin(): Skin {
        val skin = Skin()
        val worldTitleFont  = Fonts.create(36, Color(0.95f, 0.88f, 0.3f, 1f), borderWidth = 2f)
        val subFont         = Fonts.create(22, Color.WHITE, borderWidth = 1f)
        val headerFont      = Fonts.create(22, Color(0.9f,  0.78f, 0.35f, 1f), borderWidth = 1f)
        val bossFont        = Fonts.create(22, Color(1f,   0.32f, 0.2f,  1f), borderWidth = 1f)
        val smallFont       = Fonts.create(15, Color.LIGHT_GRAY)
        val rewardFont      = Fonts.create(15, Color(0.55f, 0.85f, 0.55f, 1f))
        val btnFont         = Fonts.create(19, Color.WHITE, borderWidth = 1f)
        val deployFont      = Fonts.create(24, Color.WHITE, borderWidth = 1.5f)
        val nodeTextFont    = Fonts.create(14, Color(0.65f, 0.65f, 0.7f, 1f))
        val nodeTextBright  = Fonts.create(14, Color.WHITE, borderWidth = 0.5f)
        val nodeIdFont      = Fonts.create(16, Color(0.8f,  0.8f,  0.85f, 1f), borderWidth = 0.5f)
        val nodeIdBrightF   = Fonts.create(16, Color(1f,   0.92f, 0.3f,  1f), borderWidth = 1f)
        val nodeNameFont    = Fonts.create(11, Color(0.55f, 0.55f, 0.6f, 1f))
        val panelNameFont   = Fonts.create(17, Color(0.95f, 0.88f, 0.3f, 1f), borderWidth = 1f)
        val panelLevelFont  = Fonts.create(14, Color(0.75f, 0.75f, 1f,   1f))
        val panelSmallFont  = Fonts.create(12, Color.LIGHT_GRAY)
        val panelGoldFont   = Fonts.create(17, Color(1f,   0.84f, 0f,   1f), borderWidth = 0.8f)
        val panelSecFont    = Fonts.create(11, Color(0.5f,  0.5f,  0.65f, 1f))
        val panelHintFont   = Fonts.create(13, Color(0.95f, 0.72f, 0.15f, 1f))
        val panelLockFont   = Fonts.create(12, Color(0.38f, 0.38f, 0.48f, 1f))

        skin.add("default-font", btnFont)
        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        skin.add("white", Texture(px)); px.dispose()

        skin.add("worldTitle",    Label.LabelStyle(worldTitleFont, Color(0.95f, 0.88f, 0.3f, 1f)))
        skin.add("sub",           Label.LabelStyle(subFont,        Color.WHITE))
        skin.add("header",        Label.LabelStyle(headerFont,     Color(0.9f,  0.78f, 0.35f, 1f)))
        skin.add("boss",          Label.LabelStyle(bossFont,       Color(1f,    0.32f, 0.2f,  1f)))
        skin.add("small",         Label.LabelStyle(smallFont,      Color.LIGHT_GRAY))
        skin.add("reward",        Label.LabelStyle(rewardFont,     Color(0.55f, 0.85f, 0.55f, 1f)))
        skin.add("unlocked",      Label.LabelStyle(smallFont,      Color(0.42f, 0.88f, 0.42f, 1f)))
        skin.add("locked",        Label.LabelStyle(smallFont,      Color(0.45f, 0.45f, 0.52f, 1f)))
        skin.add("default",       Label.LabelStyle(btnFont,        Color.WHITE))
        skin.add("nodeText",      Label.LabelStyle(nodeTextFont,   Color(0.65f, 0.65f, 0.7f, 1f)))
        skin.add("nodeTextBright",Label.LabelStyle(nodeTextBright, Color.WHITE))
        skin.add("nodeId",        Label.LabelStyle(nodeIdFont,     Color(0.8f,  0.8f,  0.85f, 1f)))
        skin.add("nodeIdBright",  Label.LabelStyle(nodeIdBrightF,  Color(1f,    0.92f, 0.3f,  1f)))
        skin.add("nodeName",      Label.LabelStyle(nodeNameFont,   Color(0.55f, 0.55f, 0.6f, 1f)))
        skin.add("panelName",     Label.LabelStyle(panelNameFont,  Color(0.95f, 0.88f, 0.3f, 1f)))
        skin.add("panelLevel",    Label.LabelStyle(panelLevelFont, Color(0.75f, 0.75f, 1f,   1f)))
        skin.add("panelSmall",    Label.LabelStyle(panelSmallFont, Color.LIGHT_GRAY))
        skin.add("panelGold",     Label.LabelStyle(panelGoldFont,  Color(1f,    0.84f, 0f,   1f)))
        skin.add("panelSection",  Label.LabelStyle(panelSecFont,   Color(0.5f,  0.5f,  0.65f, 1f)))
        skin.add("panelHint",     Label.LabelStyle(panelHintFont,  Color(0.95f, 0.72f, 0.15f, 1f)))
        skin.add("panelLocked",   Label.LabelStyle(panelLockFont,  Color(0.38f, 0.38f, 0.48f, 1f)))

        val defaultBtn = TextButton.TextButtonStyle().apply {
            up   = skin.newDrawable("white", Color(0.22f, 0.3f,  0.22f, 1f))
            over = skin.newDrawable("white", Color(0.32f, 0.46f, 0.32f, 1f))
            down = skin.newDrawable("white", Color(0.14f, 0.2f,  0.14f, 1f))
            font = btnFont; fontColor = Color.WHITE
        }
        skin.add("default", defaultBtn)
        skin.add("recruit", TextButton.TextButtonStyle().apply {
            up   = skin.newDrawable("white", Color(0.16f, 0.26f, 0.42f, 1f))
            over = skin.newDrawable("white", Color(0.24f, 0.38f, 0.58f, 1f))
            down = skin.newDrawable("white", Color(0.1f,  0.18f, 0.3f,  1f))
            font = btnFont; fontColor = Color(0.75f, 0.92f, 1f, 1f)
        })
        skin.add("recruitDim", TextButton.TextButtonStyle().apply {
            up   = skin.newDrawable("white", Color(0.16f, 0.16f, 0.2f, 1f))
            over = skin.newDrawable("white", Color(0.16f, 0.16f, 0.2f, 1f))
            down = skin.newDrawable("white", Color(0.16f, 0.16f, 0.2f, 1f))
            font = btnFont; fontColor = Color(0.4f, 0.4f, 0.46f, 1f)
        })
        skin.add("deploy", TextButton.TextButtonStyle().apply {
            up   = skin.newDrawable("white", Color(0.45f, 0.18f, 0.1f,  1f))
            over = skin.newDrawable("white", Color(0.65f, 0.26f, 0.14f, 1f))
            down = skin.newDrawable("white", Color(0.3f,  0.12f, 0.06f, 1f))
            font = deployFont; fontColor = Color(1f, 0.9f, 0.8f, 1f)
        })
        skin.add("deployAlt", TextButton.TextButtonStyle().apply {
            up   = skin.newDrawable("white", Color(0.18f, 0.26f, 0.42f, 1f))
            over = skin.newDrawable("white", Color(0.26f, 0.38f, 0.6f,  1f))
            down = skin.newDrawable("white", Color(0.12f, 0.18f, 0.3f,  1f))
            font = deployFont; fontColor = Color(0.8f, 0.9f, 1f, 1f)
        })
        return skin
    }
}

