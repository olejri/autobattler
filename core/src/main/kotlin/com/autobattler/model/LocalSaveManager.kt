package com.autobattler.model

import com.badlogic.gdx.Gdx
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Saves the Commander to a local JSON file via libGDX's local file API.
 *
 * On desktop: resolves to the working directory (or appdata when distributed).
 * Configure Steam Cloud in the Steamworks dashboard to sync this folder
 * — no code changes required for basic Steam Cloud support.
 */
class LocalSaveManager : SaveManager {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val path = "save/commander.json"

    override fun save(commander: Commander) {
        try {
            Gdx.files.local(path).writeString(json.encodeToString(commander), false)
        } catch (e: Exception) {
            Gdx.app.error("SaveManager", "Failed to save: ${e.message}")
        }
    }

    override fun load(): Commander? = try {
        val file = Gdx.files.local(path)
        if (!file.exists()) null
        else {
            var commander = json.decodeFromString(Commander.serializer(), file.readString())
            // Migration: ensure Farmer is unlocked for all existing commanders
            if ("farmer" !in commander.unlockedUnitTypeIds) {
                commander = commander.copy(unlockedUnitTypeIds = commander.unlockedUnitTypeIds + "farmer")
                save(commander)
            }
            commander
        }
    } catch (e: Exception) {
        Gdx.app.error("SaveManager", "Failed to load: ${e.message}")
        null
    }

    override fun hasSave(): Boolean = Gdx.files.local(path).exists()

    override fun deleteSave() {
        Gdx.files.local(path).delete()
    }
}

