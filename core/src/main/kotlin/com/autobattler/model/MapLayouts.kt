package com.autobattler.model

/**
 * Generates pre-built map layouts with terrain features.
 * Layouts are loaded from JSON files in resources/maps/.
 * The hardcoded fallback is kept for offline/test use.
 */
object MapLayouts {

    /**
     * Apply the tutorial map from JSON.
     * Falls back to hardcoded layout if JSON loading fails.
     */
    fun applyTutorialLayout(grid: BattleGrid) {
        try {
            val map = MapLoader.loadMap("maps/tutorial_river.json")
            MapLoader.applyToGrid(map, grid)
        } catch (e: Exception) {
            applyTutorialLayoutFallback(grid)
        }
    }

    /**
     * Hardcoded fallback for the tutorial map (used if JSON not available).
     */
    private fun applyTutorialLayoutFallback(grid: BattleGrid) {
        // River
        for (y in 0 until grid.height) {
            grid.setTile(7, y, TileType.WATER)
            grid.setTile(8, y, TileType.WATER)
        }
        // Bridges
        for (y in listOf(3, 8, 12)) {
            grid.setTile(7, y, TileType.BRIDGE)
            grid.setTile(8, y, TileType.BRIDGE)
        }
        // Rocks
        grid.setTile(3, 5, TileType.ROCK)
        grid.setTile(3, 6, TileType.ROCK)
        grid.setTile(5, 10, TileType.ROCK)
        grid.setTile(2, 13, TileType.ROCK)
        grid.setTile(12, 4, TileType.ROCK)
        grid.setTile(12, 5, TileType.ROCK)
        grid.setTile(10, 9, TileType.ROCK)
        grid.setTile(13, 13, TileType.ROCK)
        grid.setTile(6, 2, TileType.ROCK)
        grid.setTile(9, 4, TileType.ROCK)
        grid.setTile(6, 11, TileType.ROCK)
        grid.setTile(9, 13, TileType.ROCK)
    }
}
