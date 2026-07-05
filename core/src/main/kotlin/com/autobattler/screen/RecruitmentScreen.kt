package com.autobattler.screen

import com.autobattler.AutobattlerGame
import com.autobattler.model.*
import com.autobattler.render.Fonts
import com.autobattler.render.SpriteGenerator
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport

/**
 * Full-screen recruitment barracks.
 *
 * Shows all purchasable units grouped by base type (Warrior / Archer / …),
 * with Bronze / Silver / Gold cards side-by-side.  Each card displays the
 * unit's sprite, stats, strength/weakness summary, and an action button:
 *   ✓  already recruited
 *   🔒  level requirement not met
 *   [RECRUIT Xg]  — spend gold to permanently unlock
 *   (not enough gold)  — dimmed info label
 *
 * Back button returns to [CampaignScreen].
 */
class RecruitmentScreen(private val game: AutobattlerGame) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())
    private lateinit var skin: Skin
    private lateinit var contentTable: Table

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun show() {
        Gdx.input.inputProcessor = stage
        skin = createSkin()
        buildLayout()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.06f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(w: Int, h: Int) = stage.viewport.update(w, h, true)

    override fun dispose() {
        stage.dispose()
        skin.dispose()
        Fonts.disposeAll()
    }

    // ─── Layout ───────────────────────────────────────────────────────────────

    private fun buildLayout() {
        stage.clear()
        val commander = game.commander ?: run { game.screen = MainMenuScreen(game); return }

        val root = Table()
        root.setFillParent(true)
        stage.addActor(root)

        // ── Header bar ───────────────────────────────────────────────────────
        val header = Table()
        header.background = skin.newDrawable("white", Color(0.05f, 0.07f, 0.14f, 1f))
        header.pad(12f, 24f, 12f, 24f)
        header.add(Label("⚔  BARRACKS — RECRUIT TROOPS", skin, "title")).expandX().left()
        header.add(Label("Level ${commander.level}", skin, "levelLabel")).padRight(28f)
        header.add(Label("${commander.gold}g", skin, "goldLabel"))
        root.add(header).fillX().row()

        // thin divider
        root.add(Image(skin.newDrawable("white", Color(0.18f, 0.22f, 0.35f, 1f)))).fillX().height(2f).row()

        // ── Scrollable content ───────────────────────────────────────────────
        contentTable = Table()
        contentTable.top().pad(18f, 24f, 30f, 24f)
        populateContent(commander)

        val scroll = ScrollPane(contentTable, skin)
        scroll.setFadeScrollBars(false)
        scroll.setScrollingDisabled(true, false)
        scroll.setOverscroll(false, false)
        root.add(scroll).expand().fill().row()

        // ── Footer ───────────────────────────────────────────────────────────
        val footer = Table()
        footer.background = skin.newDrawable("white", Color(0.04f, 0.04f, 0.08f, 1f))
        footer.pad(10f, 20f, 10f, 20f)
        val backBtn = TextButton("◀  BACK TO CAMP", skin, "back")
        backBtn.addListener(object : ClickListener() {
            override fun clicked(e: InputEvent?, x: Float, y: Float) {
                game.screen = CampaignScreen(game)
            }
        })
        footer.add(backBtn).width(240f).height(52f)
        root.add(footer).fillX().row()
    }

    // ─── Content table ────────────────────────────────────────────────────────

    private fun populateContent(commander: Commander) {
        contentTable.clearChildren()

        val orderedBaseIds = UnitType.purchasable.map { it.baseTypeId }.distinct()
        val groups = UnitType.purchasable.groupBy { it.baseTypeId }

        // Intro hint
        val hint = Label(
            "Each unit has three tiers: Bronze (base), Silver ★ (+upgrades), and Gold ✦ (elite).\n" +
            "Recruiting adds the unit permanently to your roster — then pay the deploy cost each battle.",
            skin, "hint"
        )
        hint.wrap = true
        contentTable.add(hint).minWidth(0f).growX().padBottom(20f).row()

        for (baseId in orderedBaseIds) {
            val group = groups[baseId] ?: continue
            val bronzeType = group.minByOrNull { it.tier.ordinal } ?: continue

            // ── Group header strip ─────────────────────────────────────────
            val groupName = unitGroupName(bronzeType)
            val groupBg   = Table()
            groupBg.background = skin.newDrawable("white", Color(0.1f, 0.12f, 0.2f, 1f))
            groupBg.pad(8f, 14f, 8f, 14f)

            // Small icon next to group name
            val tex = SpriteGenerator.getTexture(bronzeType)
            val icon = Image(TextureRegionDrawable(TextureRegion(tex)))
            groupBg.add(icon).size(28f).padRight(10f)
            groupBg.add(Label(groupName.uppercase(), skin, "groupHeader")).expandX().left()
            groupBg.add(Label(unitRole(bronzeType), skin, "role")).right()
            contentTable.add(groupBg).fillX().padBottom(4f).row()

            // Short flavour description below the strip
            val flavour = Label(unitFlavour(bronzeType), skin, "flavour")
            flavour.wrap = true
            contentTable.add(flavour).minWidth(0f).growX().padLeft(14f).padBottom(10f).row()

            // ── Tier cards (Bronze / Silver / Gold) side-by-side ──────────
            val cardRow = Table()
            cardRow.left()
            for (type in group.sortedBy { it.tier.ordinal }) {
                cardRow.add(buildCard(type, commander)).expandX().fillX().padRight(10f).top()
            }
            contentTable.add(cardRow).fillX().padBottom(24f).row()
        }
    }

    // ─── Single unit card ─────────────────────────────────────────────────────

    private fun buildCard(type: UnitType, commander: Commander): Table {
        val isUnlocked = type.id in commander.unlockedUnitTypeIds || type.unlockLevel == 1
        val levelMet   = commander.level >= type.unlockLevel
        val canAfford  = commander.gold >= type.unlockCost

        val tierBorderColor = when (type.tier) {
            UnitTier.BRONZE -> Color(0.50f, 0.33f, 0.12f, 1f)
            UnitTier.SILVER -> Color(0.55f, 0.58f, 0.64f, 1f)
            UnitTier.GOLD   -> Color(0.72f, 0.58f, 0.08f, 1f)
        }
        val tierBgColor = when (type.tier) {
            UnitTier.BRONZE -> Color(0.28f, 0.18f, 0.06f, 1f)
            UnitTier.SILVER -> Color(0.18f, 0.20f, 0.26f, 1f)
            UnitTier.GOLD   -> Color(0.24f, 0.20f, 0.06f, 1f)
        }
        val cardBg = when {
            isUnlocked -> Color(0.07f, 0.14f, 0.07f, 1f)
            !levelMet  -> Color(0.07f, 0.07f, 0.09f, 1f)
            else       -> Color(0.08f, 0.09f, 0.14f, 1f)
        }

        val inner = Table()
        inner.background = skin.newDrawable("white", cardBg)
        inner.top().pad(0f)

        // ── Tier header strip ────────────────────────────────────────────
        val strip = Table()
        strip.background = skin.newDrawable("white", tierBgColor)
        strip.pad(5f, 10f, 5f, 10f)
        val tierText = when (type.tier) {
            UnitTier.BRONZE -> "BRONZE"
            UnitTier.SILVER -> "★  SILVER"
            UnitTier.GOLD   -> "✦  GOLD"
        }
        strip.add(Label(tierText, skin, "tierStrip")).expandX().left()
        if (isUnlocked) strip.add(Label("✓", skin, "recruitedCheck")).right()
        inner.add(strip).fillX().row()

        // ── Icon + stats ─────────────────────────────────────────────────
        val midRow = Table()
        midRow.pad(10f, 10f, 6f, 10f)

        val iconTex = SpriteGenerator.getTexture(type)
        val iconImg = Image(TextureRegionDrawable(TextureRegion(iconTex)))
        midRow.add(iconImg).size(54f).padRight(12f).top()

        val stats = Table().left().top()
        stats.add(Label("HP    ${type.maxHp}", skin, "stat")).left().row()
        stats.add(Label("ATK   ${type.power}", skin, "stat")).left().row()
        stats.add(Label("SPD   ${type.speed}  CD ${type.cooldown}", skin, "stat")).left().row()
        if (type.size > 1)
            stats.add(Label("Size  ${type.size} tiles", skin, "statSmall")).left().row()
        if (type.canFly)
            stats.add(Label("✈ Flying", skin, "fly")).left().row()
        val ability = (type as? SpecialUnitType)?.ability
        if (ability != null)
            stats.add(Label(abilityLabel(ability), skin, "abilityLabel")).left().row()
        midRow.add(stats).expandX().fillX().top()
        inner.add(midRow).fillX().row()

        // ── Strengths & weaknesses ───────────────────────────────────────
        val sw = strengthsWeaknesses(type)
        val swBlock = Table()
        swBlock.pad(0f, 10f, 6f, 10f)
        val strLbl = Label("+ ${sw.first}", skin, "strength")
        strLbl.wrap = true
        swBlock.add(strLbl).minWidth(0f).growX().padBottom(2f).row()
        val wkLbl = Label("− ${sw.second}", skin, "weakness")
        wkLbl.wrap = true
        swBlock.add(wkLbl).minWidth(0f).growX().row()
        inner.add(swBlock).minWidth(0f).growX().row()

        // ── Deploy cost hint ─────────────────────────────────────────────
        inner.add(Label("${type.cost}g  per battle", skin, "deployHint"))
            .left().pad(0f, 10f, 8f, 10f).row()

        // ── Action row ───────────────────────────────────────────────────
        val actionBlock = Table()
        actionBlock.pad(0f, 8f, 10f, 8f)
        when {
            isUnlocked -> {
                actionBlock.add(Label("Recruited  ✓", skin, "recruitedLabel")).left()
            }
            !levelMet -> {
                actionBlock.add(Label("🔒  Need Level ${type.unlockLevel}", skin, "locked")).left()
            }
            canAfford -> {
                val btn = TextButton("RECRUIT  ${type.unlockCost}g", skin, "recruit")
                btn.addListener(object : ClickListener() {
                    override fun clicked(e: InputEvent?, x: Float, y: Float) {
                        val updated = commander.unlock(type)
                        game.commander = updated
                        game.saveManager.save(updated)
                        buildLayout()
                    }
                })
                actionBlock.add(btn).fillX().height(42f)
            }
            else -> {
                val short = type.unlockCost - commander.gold
                actionBlock.add(Label("Need +${short}g more", skin, "cantAfford")).left()
            }
        }
        inner.add(actionBlock).fillX().row()

        // Wrap with coloured border
        val wrapper = Table()
        wrapper.background = skin.newDrawable("white", tierBorderColor)
        wrapper.add(inner).minWidth(0f).expand().fill().pad(2f)
        return wrapper
    }

    // ─── Unit metadata ────────────────────────────────────────────────────────

    private fun unitGroupName(base: UnitType): String = when (base.baseTypeId) {
        "melee"        -> "Warrior"
        "ranged"       -> "Archer"
        "healer"       -> "Cleric"
        "farmer"       -> "Farmer"
        "knight"       -> "Knight"
        "wizard"       -> "Wizard"
        "griffin_rider"-> "Griffin Rider"
        else           -> base.displayName
    }

    private fun unitRole(base: UnitType): String = when (base.baseTypeId) {
        "melee"         -> "Melee  ·  Frontline tank"
        "ranged"        -> "Ranged  ·  Backline DPS"
        "healer"        -> "Melee  ·  Support healer"
        "farmer"        -> "Special  ·  Gold economy"
        "knight"        -> "Melee  ·  Heavy cavalry"
        "wizard"        -> "Ranged  ·  AoE caster"
        "griffin_rider" -> "Melee  ·  Flying striker"
        else            -> ""
    }

    private fun unitFlavour(base: UnitType): String = when (base.baseTypeId) {
        "melee"         ->
            "The backbone of any army. Warriors charge ahead and absorb punishment so your " +
            "archers and mages can work safely. Their AI actively flanks around obstacles."
        "ranged"        ->
            "Archers hang back and pepper enemies from a distance.  " +
            "Keep them behind your front line and they'll reward you with consistent damage."
        "healer"        ->
            "Clerics follow the battle and restore HP to wounded allies each round.  " +
            "A single Cleric can dramatically extend a fight — but they need protection."
        "farmer"        ->
            "Farmers rush to empty tiles, plant a Farm, and that Farm yields gold every round.  " +
            "Stack several Farmers for a powerful economic snowball — just keep them alive."
        "knight"        ->
            "Knights are mounted two-tile units that hit hard and weather sustained fire.  " +
            "Their high speed lets them charge through gaps and reach high-value targets fast."
        "wizard"        ->
            "Wizards hurl a Fireball at the densest enemy cluster, dealing AoE damage to a 3×3 grid.  " +
            "WARNING: the blast is indiscriminate — it hurts your own troops too!"
        "griffin_rider" ->
            "Griffin Riders fly above all terrain and can land anywhere on the board.  " +
            "Use them to bypass fortifications, kill exposed backline units, or flank instantly."
        else            -> ""
    }

    private fun strengthsWeaknesses(type: UnitType): Pair<String, String> = when (type.baseTypeId) {
        "melee"         -> "Tanks hits; blocks chokepoints; flanks around obstacles." to
                           "Short range; must close gap before attacking."
        "ranged"        -> "Safe damage from behind allied frontline." to
                           "Very low HP — dies fast in melee."
        "healer"        -> "Restores HP each round; multiplies effective army HP." to
                           "Zero damage; must be shielded."
        "farmer"        -> "Generates passive gold income during battle." to
                           "Cannot fight; first to die."
        "knight"        -> "Best HP/ATK ratio; fast for a heavy unit." to
                           "2-tile footprint blocks allies in corridors."
        "wizard"        -> "Massive 3×3 AoE fireball devastates clusters." to
                           "Hits allies too! Pitiful HP; long cooldown."
        "griffin_rider" -> "Flies over all terrain; reaches any tile instantly." to
                           "Expensive; 2-tile unit; vulnerable to ranged."
        else            -> "—" to "—"
    }

    private fun abilityLabel(ability: SpecialAbility): String = when (ability) {
        is SpecialAbility.PlantFarm    -> "⚡ Plants a gold farm"
        is SpecialAbility.Fireball     -> "🔥 Fireball ${ability.power} dmg / ${ability.radius*2+1}×${ability.radius*2+1} AoE"
        is SpecialAbility.YieldGold    -> "💰 Yields ${ability.goldPerRound}g/round"
        is SpecialAbility.Flying       -> "✈ Flying"
    }

    // ─── Skin ─────────────────────────────────────────────────────────────────

    private fun createSkin(): Skin {
        val s = Skin()
        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        s.add("white", Texture(px)); px.dispose()

        // Fonts
        val titleF      = Fonts.create(26, Color(0.95f, 0.88f, 0.3f,  1f), borderWidth = 1.5f)
        val goldF       = Fonts.create(22, Color(1f,    0.84f, 0f,    1f), borderWidth = 1f)
        val levelF      = Fonts.create(18, Color(0.7f,  0.8f,  1f,   1f))
        val groupHF     = Fonts.create(20, Color(0.95f, 0.9f,  0.55f, 1f), borderWidth = 1f)
        val roleF       = Fonts.create(13, Color(0.55f, 0.65f, 0.75f, 1f))
        val flavourF    = Fonts.create(14, Color(0.62f, 0.62f, 0.68f, 1f))
        val hintF       = Fonts.create(13, Color(0.55f, 0.62f, 0.72f, 1f))
        val tierStripF  = Fonts.create(15, Color(0.9f,  0.85f, 0.7f,  1f), borderWidth = 0.5f)
        val checkF      = Fonts.create(17, Color(0.3f,  0.9f,  0.3f,  1f), borderWidth = 1f)
        val statF       = Fonts.create(14, Color(0.85f, 0.85f, 0.9f,  1f))
        val statSmallF  = Fonts.create(12, Color(0.65f, 0.75f, 0.8f,  1f))
        val flyF        = Fonts.create(13, Color(0.5f,  0.82f, 1f,   1f))
        val abilityF    = Fonts.create(13, Color(1f,    0.65f, 0.2f,  1f))
        val strF        = Fonts.create(13, Color(0.38f, 0.85f, 0.42f, 1f))
        val wkF         = Fonts.create(13, Color(0.88f, 0.38f, 0.38f, 1f))
        val deployF     = Fonts.create(12, Color(0.5f,  0.5f,  0.56f, 1f))
        val recruitedF  = Fonts.create(14, Color(0.35f, 0.88f, 0.38f, 1f))
        val lockedF     = Fonts.create(14, Color(0.45f, 0.45f, 0.52f, 1f))
        val cantAffordF = Fonts.create(14, Color(0.75f, 0.42f, 0.15f, 1f))
        val btnF        = Fonts.create(16, Color.WHITE, borderWidth = 1f)
        val backF       = Fonts.create(18, Color.WHITE, borderWidth = 1f)

        s.add("default-font", btnF)

        // Label styles
        s.add("title",          Label.LabelStyle(titleF,     Color(0.95f, 0.88f, 0.3f,  1f)))
        s.add("goldLabel",      Label.LabelStyle(goldF,      Color(1f,    0.84f, 0f,    1f)))
        s.add("levelLabel",     Label.LabelStyle(levelF,     Color(0.7f,  0.8f,  1f,   1f)))
        s.add("groupHeader",    Label.LabelStyle(groupHF,    Color(0.95f, 0.9f,  0.55f, 1f)))
        s.add("role",           Label.LabelStyle(roleF,      Color(0.55f, 0.65f, 0.75f, 1f)))
        s.add("flavour",        Label.LabelStyle(flavourF,   Color(0.62f, 0.62f, 0.68f, 1f)))
        s.add("hint",           Label.LabelStyle(hintF,      Color(0.55f, 0.62f, 0.72f, 1f)))
        s.add("tierStrip",      Label.LabelStyle(tierStripF, Color(0.9f,  0.85f, 0.7f,  1f)))
        s.add("recruitedCheck", Label.LabelStyle(checkF,     Color(0.3f,  0.9f,  0.3f,  1f)))
        s.add("stat",           Label.LabelStyle(statF,      Color(0.85f, 0.85f, 0.9f,  1f)))
        s.add("statSmall",      Label.LabelStyle(statSmallF, Color(0.65f, 0.75f, 0.8f,  1f)))
        s.add("fly",            Label.LabelStyle(flyF,       Color(0.5f,  0.82f, 1f,   1f)))
        s.add("abilityLabel",   Label.LabelStyle(abilityF,   Color(1f,    0.65f, 0.2f,  1f)))
        s.add("strength",       Label.LabelStyle(strF,       Color(0.38f, 0.85f, 0.42f, 1f)))
        s.add("weakness",       Label.LabelStyle(wkF,        Color(0.88f, 0.38f, 0.38f, 1f)))
        s.add("deployHint",     Label.LabelStyle(deployF,    Color(0.5f,  0.5f,  0.56f, 1f)))
        s.add("recruitedLabel", Label.LabelStyle(recruitedF, Color(0.35f, 0.88f, 0.38f, 1f)))
        s.add("locked",         Label.LabelStyle(lockedF,    Color(0.45f, 0.45f, 0.52f, 1f)))
        s.add("cantAfford",     Label.LabelStyle(cantAffordF,Color(0.75f, 0.42f, 0.15f, 1f)))
        s.add("default",        Label.LabelStyle(btnF,       Color.WHITE))

        // Button drawable helpers
        fun bg(c: Color) = TextureRegionDrawable(TextureRegion(
            Texture(Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(c); fill() })
        ))

        // TextButton styles
        val recruitStyle = TextButton.TextButtonStyle().apply {
            font   = btnF
            fontColor  = Color.WHITE
            up     = bg(Color(0.15f, 0.35f, 0.15f, 1f))
            over   = bg(Color(0.20f, 0.48f, 0.20f, 1f))
            down   = bg(Color(0.10f, 0.25f, 0.10f, 1f))
        }
        val backStyle = TextButton.TextButtonStyle().apply {
            font   = backF
            fontColor  = Color.WHITE
            up     = bg(Color(0.15f, 0.18f, 0.30f, 1f))
            over   = bg(Color(0.22f, 0.26f, 0.42f, 1f))
            down   = bg(Color(0.10f, 0.12f, 0.20f, 1f))
        }
        val defaultBtnStyle = TextButton.TextButtonStyle().apply {
            font   = btnF
            fontColor  = Color.WHITE
            up     = bg(Color(0.12f, 0.12f, 0.18f, 1f))
            over   = bg(Color(0.18f, 0.18f, 0.26f, 1f))
            down   = bg(Color(0.08f, 0.08f, 0.12f, 1f))
        }
        s.add("default",  defaultBtnStyle)
        s.add("recruit",  recruitStyle)
        s.add("back",     backStyle)

        // Minimal ScrollPane style so Scene2D doesn't crash
        val scrollStyle = ScrollPane.ScrollPaneStyle().apply {
            val handleTex = bg(Color(0.3f, 0.3f, 0.4f, 0.7f))
            vScroll       = handleTex
            vScrollKnob   = bg(Color(0.5f, 0.5f, 0.65f, 0.9f))
        }
        s.add("default", scrollStyle)

        return s
    }
}
