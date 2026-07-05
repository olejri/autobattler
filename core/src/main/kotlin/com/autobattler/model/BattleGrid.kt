package com.autobattler.model

/**
 * Represents the 16x16 battle grid with tile properties.
 * The grid is split vertically: left half (x 0..7) is the player side,
 * right half (x 8..15) is the enemy side.
 */
class BattleGrid(
    val width: Int = GRID_SIZE,
    val height: Int = GRID_SIZE
) {
    companion object {
        const val GRID_SIZE = 16
        const val PLAYER_ZONE_END = 7    // x: 0..7
        const val ENEMY_ZONE_START = 8   // x: 8..15
    }

    /** Tile data for every cell. Defaults to GRASS. */
    private val tiles = Array(width) { Array(height) { TileType.GRASS } }

    private val units = mutableListOf<BattleUnit>()

    val allUnits: List<BattleUnit> get() = units.toList()

    val aliveUnits: List<BattleUnit> get() = units.filter { it.isAlive }

    fun playerUnits(): List<BattleUnit> =
        aliveUnits.filter { it.owner == BattleUnit.Owner.PLAYER }

    fun enemyUnits(): List<BattleUnit> =
        aliveUnits.filter { it.owner == BattleUnit.Owner.ENEMY }

    // ─── Tile Access ────────────────────────────────────────────────────────────

    fun getTile(pos: Position): TileType {
        if (!isInBounds(pos)) return TileType.ROCK  // Out of bounds = impassable
        return tiles[pos.x][pos.y]
    }

    fun getTile(x: Int, y: Int): TileType {
        if (x !in 0 until width || y !in 0 until height) return TileType.ROCK
        return tiles[x][y]
    }

    fun setTile(pos: Position, type: TileType) {
        if (isInBounds(pos)) tiles[pos.x][pos.y] = type
    }

    fun setTile(x: Int, y: Int, type: TileType) {
        if (x in 0 until width && y in 0 until height) tiles[x][y] = type
    }

    /** Whether a unit can walk onto this position. */
    fun isPassable(pos: Position): Boolean = isInBounds(pos) && getTile(pos).passable

    /** Whether a projectile can pass through this position. */
    fun isProjectilePassable(pos: Position): Boolean = isInBounds(pos) && getTile(pos).projectilePassable

    /**
     * Check line-of-sight for ranged attacks between two positions.
     * Returns true if no tile along the path blocks projectiles.
     * Uses Bresenham-style line to check each tile.
     */
    fun hasLineOfSight(from: Position, to: Position): Boolean {
        val points = getLineTiles(from, to)
        // Skip the first (attacker) and last (target) tiles
        for (i in 1 until points.size - 1) {
            if (!isProjectilePassable(points[i])) return false
        }
        return true
    }

    /** Get all tiles along a line from [from] to [to] using Bresenham's algorithm. */
    private fun getLineTiles(from: Position, to: Position): List<Position> {
        val result = mutableListOf<Position>()
        var x0 = from.x; var y0 = from.y
        val x1 = to.x; val y1 = to.y
        val dx = kotlin.math.abs(x1 - x0)
        val dy = kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy

        while (true) {
            result.add(Position(x0, y0))
            if (x0 == x1 && y0 == y1) break
            val e2 = 2 * err
            if (e2 > -dy) { err -= dy; x0 += sx }
            if (e2 < dx) { err += dx; y0 += sy }
        }
        return result
    }

    // ─── Unit Management ────────────────────────────────────────────────────────

    fun placeUnit(unit: BattleUnit): Boolean {
        if (!isValidPlacementForUnit(unit)) return false
        units.add(unit)
        return true
    }

    /** Find which unit (if any) occupies a given tile. Supports multi-tile units. */
    fun unitAt(position: Position): BattleUnit? =
        aliveUnits.find { it.occupies(position) }

    /** Check if a position is valid for placing a 1x1 unit of given owner. */
    fun isValidPlacement(position: Position, owner: BattleUnit.Owner): Boolean {
        if (!isInBounds(position)) return false
        val tile = getTile(position)
        if (!tile.placeable) return false
        if (unitAt(position) != null) return false
        return when (owner) {
            BattleUnit.Owner.PLAYER -> position.x <= PLAYER_ZONE_END
            BattleUnit.Owner.ENEMY -> position.x >= ENEMY_ZONE_START
        }
    }

    /** Check if all tiles for a multi-tile unit are valid. */
    fun isValidPlacementForUnit(unit: BattleUnit): Boolean {
        for (tile in unit.occupiedTiles()) {
            if (!isInBounds(tile)) return false
            if (!getTile(tile).placeable && unit.type.size == 1) return false
            // For large units, just check passability (they're placed by scenario, not player)
            if (unit.type.size > 1 && !isInBounds(tile)) return false
            if (aliveUnits.any { it.occupies(tile) }) return false
        }
        return true
    }

    /** Check if a multi-tile unit can fit at a given position. */
    fun canUnitFitAt(position: Position, unitSize: Int, excludeUnitId: Int = -1): Boolean {
        for (dx in 0 until unitSize) {
            for (dy in 0 until unitSize) {
                val pos = Position(position.x + dx, position.y + dy)
                if (!isInBounds(pos)) return false
                if (!isPassable(pos)) return false
                val occupant = aliveUnits.find { it.id != excludeUnitId && it.occupies(pos) }
                if (occupant != null) return false
            }
        }
        return true
    }

    fun isOccupied(position: Position): Boolean = unitAt(position) != null

    fun removeUnit(unit: BattleUnit) {
        units.remove(unit)
    }

    fun isInBounds(position: Position): Boolean =
        position.x in 0 until width && position.y in 0 until height

    fun clear() = units.clear()
}
