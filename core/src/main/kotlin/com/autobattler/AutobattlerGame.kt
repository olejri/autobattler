package com.autobattler

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.autobattler.model.Commander
import com.autobattler.model.LocalSaveManager
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

    override fun create() {
        battleEngine = BattleEngine()
        commander = saveManager.load()
        Gdx.app.log("Autobattler", "Game initialized" +
            (commander?.let { " — Commander: ${it.name} (Lvl ${it.level})" } ?: " — no save found"))
        setScreen(MainMenuScreen(this))
    }

    override fun dispose() {
        screen?.dispose()
        super.dispose()
    }
}
