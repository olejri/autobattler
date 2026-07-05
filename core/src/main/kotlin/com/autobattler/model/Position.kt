package com.autobattler.model

/**
 * Represents a 2D position on the battle grid.
 */
data class Position(val x: Int, val y: Int) {

    fun distanceTo(other: Position): Int {
        return maxOf(kotlin.math.abs(x - other.x), kotlin.math.abs(y - other.y))
    }

    fun manhattanDistance(other: Position): Int {
        return kotlin.math.abs(x - other.x) + kotlin.math.abs(y - other.y)
    }

    fun isAdjacentTo(other: Position): Boolean = distanceTo(other) == 1

    fun neighborsWithin(range: Int, gridWidth: Int, gridHeight: Int): List<Position> {
        val result = mutableListOf<Position>()
        for (dx in -range..range) {
            for (dy in -range..range) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until gridWidth && ny in 0 until gridHeight) {
                    result.add(Position(nx, ny))
                }
            }
        }
        return result
    }
}

