package com.autobattler

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.autobattler.model.Commander
import com.autobattler.model.LocalSaveManager
import com.autobattler.model.RogueMeta
import com.autobattler.model.RogueRun
import com.autobattler.model.SaveManager
import com.autobattler.screen.MainMenuScreen

/**
 * Main game class for the Async Autobattler.
 * Manages screens and global game state.
 */
class AutobattlerGame : Game() {

    lateinit var battleEngine: BattleEngine
    val saveManager: SaveManager = LocalSaveManager()
    var commander: Commander? = null
    /** Active roguelike run, or null when not in rogue mode. */
    var rogueRun: RogueRun? = null
    /** Persistent meta-progression across all rogue runs. Never null — defaults on first launch. */
    var rogueMeta: RogueMeta = RogueMeta()

    override fun create() {
        battleEngine = BattleEngine()
        commander = saveManager.load()
        rogueRun  = saveManager.loadRogue()
        rogueMeta = saveManager.loadMeta() ?: RogueMeta()
        Gdx.app.log("Autobattler", "Game initialized" +
            (commander?.let { " — Commander: ${it.name} (Lvl ${it.level})" } ?: " — no save found"))
        setScreen(MainMenuScreen(this))
    }

    override fun dispose() {
        screen?.dispose()
        super.dispose()
    }
}
