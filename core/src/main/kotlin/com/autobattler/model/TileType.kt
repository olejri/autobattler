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
    ),

    /** Sand — passable desert terrain. Slows movement (cost 2). Arrows fly over. */
    SAND(
        id = "sand",
        displayName = "Sand",
        passable = true,
        projectilePassable = true,
        placeable = true,
        moveCost = 2
    ),

    /** Mud — swampy ground. Passable but very slow (cost 3). Arrows fly over. */
    MUD(
        id = "mud",
        displayName = "Mud",
        passable = true,
        projectilePassable = true,
        placeable = true,
        moveCost = 3
    ),

    /** Shallow water — wading is possible (cost 2) but placement is disallowed. Arrows fly over. */
    SHALLOW_WATER(
        id = "shallow_water",
        displayName = "Shallow Water",
        passable = true,
        projectilePassable = true,
        placeable = false,
        moveCost = 2
    ),

    /** Fort wall — impassable defensive structure. Blocks movement AND projectiles. */
    FORT(
        id = "fort",
        displayName = "Fort Wall",
        passable = false,
        projectilePassable = false,
        placeable = false
    ),

    /** Lava — completely impassable. Arrows can fly over. */
    LAVA(
        id = "lava",
        displayName = "Lava",
        passable = false,
        projectilePassable = true,
        placeable = false
    ),

    /** Ice — passable but slippery (cost 1, but units can't stop easily — flavour). Arrows fly over. */
    ICE(
        id = "ice",
        displayName = "Ice",
        passable = true,
        projectilePassable = true,
        placeable = true,
        moveCost = 1
    ),

    /** Ruins — crumbling stone. Passable (cost 2). Blocks projectiles for cover. */
    RUINS(
        id = "ruins",
        displayName = "Ruins",
        passable = true,
        projectilePassable = false,
        placeable = true,
        moveCost = 2
    );

    companion object {
        fun fromId(id: String): TileType = entries.first { it.id == id }
    }
}

