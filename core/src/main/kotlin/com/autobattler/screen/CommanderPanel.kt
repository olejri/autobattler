package com.autobattler.screen

import com.autobattler.model.Commander
import com.autobattler.model.UnitType
import com.autobattler.render.Fonts
import com.autobattler.render.SpriteGenerator
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

/**
 * Reusable commander status sidebar widget.
 *
 * Fully self-contained — creates its own internal skin and fonts.
 * Drop into any screen's Stage. Call [refresh] to update state.
 * Call [dispose] in the parent screen's dispose() to free the internal skin texture.
 */
class CommanderPanel(commander: Commander) : Table() {

    private var current = commander
    private val panelSkin: Skin = buildSkin()

    init {
        background = panelSkin.newDrawable("white", Color(0.07f, 0.09f, 0.15f, 0.97f))
        pad(14f)
        top()
        rebuild()
    }

    /** Update displayed state without recreating the panel. */
    fun refresh(newCommander: Commander) {
        current = newCommander
        clearChildren()
        rebuild()
    }

    /** Dispose the internal skin (1×1 white Texture). Fonts are handled by Fonts.disposeAll(). */
    fun dispose() = panelSkin.dispose()

    // ─── Build ───────────────────────────────────────────────────────────────────

    private fun rebuild() {
        val skin = panelSkin

        // Portrait
        add(Image(TextureRegionDrawable(TextureRegion(SpriteGenerator.getCommanderTexture()))))
            .size(64f).padBottom(6f).row()

        // Name + level
        add(Label(current.name, skin, "name")).padBottom(2f).row()
        add(Label("Level  ${current.level}", skin, "level")).padBottom(10f).row()

        // XP bar
        val xpPct = (current.xp.toFloat() / current.xpForNextLevel).coerceIn(0f, 1f)
        val barW = 170f
        val xpBg = Table()
        xpBg.background = skin.newDrawable("white", Color(0.18f, 0.18f, 0.24f, 1f))
        val xpFill = Table()
        xpFill.background = skin.newDrawable("white", Color(0.22f, 0.55f, 1f, 1f))
        xpBg.add(xpFill).width(barW * xpPct).height(8f)
        xpBg.add().expandX()
        add(xpBg).width(barW).height(8f).padBottom(2f).row()
        add(Label("${current.xp} / ${current.xpForNextLevel} XP", skin, "small")).padBottom(12f).row()

        addSep()

        // Gold
        add(Label("${current.gold}g", skin, "gold")).padBottom(12f).row()

        addSep()

        // Roster — 3-column icon grid
        add(Label("ROSTER", skin, "section")).padBottom(8f).row()
        val rosterGrid = Table().left()
        for ((i, type) in current.unlockedUnitTypes.withIndex()) {
            val cell = Table()
            cell.background = skin.newDrawable("white", Color(0.12f, 0.2f, 0.12f, 1f))
            cell.add(Image(TextureRegionDrawable(TextureRegion(SpriteGenerator.getTexture(type))))).size(32f).pad(4f)
            rosterGrid.add(cell).size(40f).pad(2f)
            if ((i + 1) % 3 == 0) rosterGrid.row()
        }
        add(rosterGrid).padBottom(10f).row()

        // Unlock hint
        val recruitAvail = UnitType.purchasable.any { t ->
            t.unlockLevel > 1 && t.id !in current.unlockedUnitTypeIds && current.level >= t.unlockLevel
        }
        val nextLocked = UnitType.purchasable
            .filter { it.unlockLevel > 1 && it.id !in current.unlockedUnitTypeIds }
            .minByOrNull { it.unlockLevel }
        when {
            recruitAvail   -> add(Label("★ Recruit available!", skin, "hint")).row()
            nextLocked != null ->
                add(Label("Lvl ${nextLocked.unlockLevel}: ${nextLocked.displayName}", skin, "locked")).row()
        }
    }

    private fun addSep() {
        add(Image(panelSkin.newDrawable("white", Color(0.22f, 0.22f, 0.38f, 0.7f))))
            .width(170f).height(1f).padBottom(10f).row()
    }

    // ─── Skin ────────────────────────────────────────────────────────────────────

    private fun buildSkin(): Skin {
        val s = Skin()
        val px = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
        s.add("white", Texture(px)); px.dispose()

        val nameFont    = Fonts.create(17, Color(0.95f, 0.88f, 0.3f, 1f), borderWidth = 1f)
        val levelFont   = Fonts.create(14, Color(0.75f, 0.75f, 1f, 1f))
        val smallFont   = Fonts.create(12, Color.LIGHT_GRAY)
        val goldFont    = Fonts.create(17, Color(1f, 0.84f, 0f, 1f), borderWidth = 0.8f)
        val sectionFont = Fonts.create(11, Color(0.5f, 0.5f, 0.65f, 1f))
        val hintFont    = Fonts.create(13, Color(0.95f, 0.72f, 0.15f, 1f))
        val lockedFont  = Fonts.create(12, Color(0.38f, 0.38f, 0.48f, 1f))

        s.add("name",    Label.LabelStyle(nameFont,    Color(0.95f, 0.88f, 0.3f, 1f)))
        s.add("level",   Label.LabelStyle(levelFont,   Color(0.75f, 0.75f, 1f,   1f)))
        s.add("small",   Label.LabelStyle(smallFont,   Color.LIGHT_GRAY))
        s.add("gold",    Label.LabelStyle(goldFont,    Color(1f,    0.84f, 0f,   1f)))
        s.add("section", Label.LabelStyle(sectionFont, Color(0.5f,  0.5f,  0.65f, 1f)))
        s.add("hint",    Label.LabelStyle(hintFont,    Color(0.95f, 0.72f, 0.15f, 1f)))
        s.add("locked",  Label.LabelStyle(lockedFont,  Color(0.38f, 0.38f, 0.48f, 1f)))
        s.add("default", Label.LabelStyle(smallFont,   Color.WHITE))
        return s
    }
}
