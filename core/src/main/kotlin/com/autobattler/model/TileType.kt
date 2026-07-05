package com.autobattler.model

/**
 * Defines the properties of a tile on the battle grid.
 *
 * Each tile type specifies:
 * - [passable]: Can units walk on this tile?
 * - [projectilePassable]: Can ranged attacks (arrows) pass through?
 * - [placeable]: Can the player place units on this tile during setup?
 * - [moveCost]: How many movement points it costs to cross (1 = normal)
 * - [displayName]: Human-readable name
 */
enum class TileType(
    val id: String,
    val displayName: String,
    val passable: Boolean,
    val projectilePassable: Boolean,
    val placeable: Boolean,
    val moveCost: Int = 1
) {
    /** Default terrain — fully passable, no obstructions. */
    GRASS(
        id = "grass",
        displayName = "Grass",
        passable = true,
        projectilePassable = true,
        placeable = true
    ),

    /** Water — blocks movement but arrows can fly over. */
    WATER(
        id = "water",
        displayName = "Water",
        passable = false,
        projectilePassable = true,
        placeable = false
    ),

    /** Rock — blocks both movement AND projectiles. Provides cover. */
    ROCK(
        id = "rock",
        displayName = "Rock",
        passable = false,
        projectilePassable = false,
        placeable = false
    ),

    /** Bridge/plank — passable crossing over water. */
    BRIDGE(
        id = "bridge",
        displayName = "Bridge",
        passable = true,
        projectilePassable = true,
        placeable = false  // Can't place during setup (it's in the middle)
    ),

    /** Tree — blocks both movement AND projectiles. Dense forest cover. */
    TREE(
        id = "tree",
        displayName = "Tree",
        passable = false,
        projectilePassable = false,
        placeable = false
    );

    companion object {
        fun fromId(id: String): TileType = entries.first { it.id == id }
    }
}

