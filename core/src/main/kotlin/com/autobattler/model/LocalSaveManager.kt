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
    private val roguePath = "save/rogue_run.json"
    private val metaPath  = "save/rogue_meta.json"

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

    override fun saveRogue(run: RogueRun) {
        try {
            Gdx.files.local(roguePath).writeString(json.encodeToString(run), false)
        } catch (e: Exception) {
            Gdx.app.error("SaveManager", "Failed to save rogue run: ${e.message}")
        }
    }

    override fun loadRogue(): RogueRun? = try {
        val file = Gdx.files.local(roguePath)
        if (!file.exists()) null
        else json.decodeFromString(RogueRun.serializer(), file.readString())
    } catch (e: Exception) {
        Gdx.app.error("SaveManager", "Failed to load rogue run: ${e.message}")
        null
    }

    override fun hasRogueSave(): Boolean = Gdx.files.local(roguePath).exists()

    override fun deleteRogue() {
        Gdx.files.local(roguePath).delete()
    }

    override fun saveMeta(meta: RogueMeta) {
        try {
            Gdx.files.local(metaPath).writeString(json.encodeToString(meta), false)
        } catch (e: Exception) {
            Gdx.app.error("SaveManager", "Failed to save rogue meta: ${e.message}")
        }
    }

    override fun loadMeta(): RogueMeta? = try {
        val file = Gdx.files.local(metaPath)
        if (!file.exists()) null
        else json.decodeFromString(RogueMeta.serializer(), file.readString())
    } catch (e: Exception) {
        Gdx.app.error("SaveManager", "Failed to load rogue meta: ${e.message}")
        null
    }

    override fun hasMeta(): Boolean = Gdx.files.local(metaPath).exists()
}

