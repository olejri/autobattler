package com.autobattler.model

import com.badlogic.gdx.Gdx
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Loads map layouts from JSON files in the resource pool.
 * Maps are defined as JSON with tile overrides — any tile not listed defaults to GRASS.
 */
object MapLoader {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Load the map pool index and return all available map definitions.
     */
    fun loadMapPool(): List<MapDefinition> {
        val poolText = readResource("maps/map_pool.json")
        val pool = json.decodeFromString<MapPoolIndex>(poolText)
        return pool.maps.map { path ->
            val mapText = readResource(path)
            json.decodeFromString<MapDefinition>(mapText)
        }
    }

    /**
     * Load a single map by its file path (relative to internal/classpath assets).
     */
    fun loadMap(path: String): MapDefinition {
        val mapText = readResource(path)
        return json.decodeFromString<MapDefinition>(mapText)
    }

    /**
     * Apply a [MapDefinition] to a [BattleGrid], setting all tile types.
     */
    fun applyToGrid(map: MapDefinition, grid: BattleGrid) {
        for (tileDef in map.tiles) {
            val tileType = TileType.fromId(tileDef.type)
            grid.setTile(tileDef.x, tileDef.y, tileType)
        }
    }

    /** Read a resource file, trying internal first then classpath. */
    private fun readResource(path: String): String {
        val internal = Gdx.files.internal(path)
        if (internal.exists()) return internal.readString()
        return Gdx.files.classpath(path).readString()
    }
}

// ─── JSON Data Classes ──────────────────────────────────────────────────────

@Serializable
data class MapPoolIndex(
    val maps: List<String>
)

@Serializable
data class MapDefinition(
    val id: String,
    val name: String,
    val description: String = "",
    val width: Int = 16,
    val height: Int = 16,
    val tiles: List<TileDefinition> = emptyList()
)

@Serializable
data class TileDefinition(
    val type: String,
    val x: Int,
    val y: Int
)

