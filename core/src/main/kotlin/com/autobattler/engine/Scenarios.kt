package com.autobattler.engine

import com.autobattler.model.*

/**
 * Defines pre-built PvE scenarios for the tutorial and campaign.
 */
object Scenarios {

    /**
     * Tutorial fight 1: Enemy has 2 melee warriors advancing.
     * The player gets 1 melee, 1 ranged, 1 healer to learn positioning.
     */
    fun tutorial1(): ScenarioDefinition = ScenarioDefinition(
        name = "Tutorial: First Battle",
        description = "The enemy sends two Warriors at you.\n" +
            "You have 300 gold — spend wisely!\n\n" +
            "• Warrior 50g — tough frontliner\n" +
            "• Archer 100g — ranged damage (needs 3-6 tile gap)\n" +
            "• Cleric 200g — heals adjacent allies",
        mapPath = "maps/tutorial_river.json",
        startingGold = 300,
        enemyPlacements = listOf(
            NormalUnitType.Melee to Position(12, 7),
            NormalUnitType.Melee to Position(12, 9),
        )
    )

    /** Alias for backward compat. */
    fun tutorial(): ScenarioDefinition = tutorial1()

    fun tutorial2(): ScenarioDefinition = ScenarioDefinition(
        name = "Tutorial: High Forest",
        description = "A dense forest with a lake in the center.\n" +
            "Trees block both movement AND arrows!\n\n" +
            "You have 600 gold. Enemy: 3 Warriors + 1 Archer.",
        mapPath = "maps/tutorial_high_forest.json",
        startingGold = 600,
        enemyPlacements = listOf(
            NormalUnitType.Melee to Position(12, 5),
            NormalUnitType.Melee to Position(11, 8),
            NormalUnitType.Melee to Position(12, 11),
            NormalUnitType.Ranged to Position(14, 8),
        )
    )

    fun tutorial3(): ScenarioDefinition = ScenarioDefinition(
        name = "Tutorial: The Sleeping Bear",
        description = "A massive 2x2 bear guards its cave!\n" +
            "It can't fit through 1-tile tree gaps — use them to kite.\n\n" +
            "You have 450 gold. The bear hits hard.",
        mapPath = "maps/tutorial_sleeping_bear.json",
        startingGold = 450,
        enemyPlacements = listOf(
            MonsterUnitType.Bear to Position(11, 7),
        )
    )

    /** All tutorial scenarios in order. */
    val tutorialSequence: List<ScenarioDefinition> = listOf(tutorial1(), tutorial2(), tutorial3())

    // ─── Campaign: World 1 ───────────────────────────────────────────────────────

    fun world1Battle(level: Int): ScenarioDefinition? = when (level) {
        1 -> ScenarioDefinition(
            name = "1-1: The River Crossing",
            description = "Two Warriors block the ford.\nYour Warrior is ready — hold the line!\n\n• Warrior 50g",
            mapPath = "maps/tutorial_river.json",
            startingGold = 300,
            enemyPlacements = listOf(
                NormalUnitType.Melee to Position(12, 7),
                NormalUnitType.Melee to Position(12, 9),
            ),
            xpReward = 60,
            goldReward = 200   // flat completion bonus; enemy drops added separately in BattleScreen
        )
        2 -> ScenarioDefinition(
            name = "1-2: River Ambush",
            description = "Three Warriors and a scout push across.\nYou've earned an Archer — use the range!\n\n• Warrior 50g  • Archer 100g",
            mapPath = "maps/tutorial_river.json",
            startingGold = 450,
            enemyPlacements = listOf(
                NormalUnitType.Melee to Position(12, 5),
                NormalUnitType.Melee to Position(12, 8),
                NormalUnitType.Melee to Position(12, 11),
                NormalUnitType.Ranged to Position(14, 8),
            ),
            xpReward = 80,
            goldReward = 200
        )
        3 -> ScenarioDefinition(
            name = "1-3: High Forest",
            description = "A dense forest with a lake in the center.\nTrees block both movement AND arrows!\n\n• Warrior 50g  • Archer 100g  • Cleric 200g",
            mapPath = "maps/tutorial_high_forest.json",
            startingGold = 600,
            enemyPlacements = listOf(
                NormalUnitType.Melee to Position(12, 5),
                NormalUnitType.Melee to Position(11, 8),
                NormalUnitType.Melee to Position(12, 11),
                NormalUnitType.Ranged to Position(14, 8),
            ),
            xpReward = 100,
            goldReward = 200
        )
        4 -> ScenarioDefinition(
            name = "1-4: Forest Siege",
            description = "A heavy force storms the forest.\nTwo Archers pin you down — flank them!\n\n• Warrior 50g  • Archer 100g  • Cleric 200g",
            mapPath = "maps/tutorial_high_forest.json",
            startingGold = 700,
            enemyPlacements = listOf(
                NormalUnitType.Melee to Position(12, 5),
                NormalUnitType.Melee to Position(12, 11),
                NormalUnitType.Ranged to Position(14, 6),
                NormalUnitType.Ranged to Position(14, 10),
                NormalUnitType.Healer to Position(15, 8),
            ),
            xpReward = 120,
            goldReward = 200
        )
        5 -> ScenarioDefinition(
            name = "1-5: The Sleeping Bear  ☠",
            description = "BOSS BATTLE\nA massive 2×2 bear guards its den.\nIt can't fit through 1-tile gaps — use them!\n\n⚠  Bring everything you have.",
            mapPath = "maps/tutorial_sleeping_bear.json",
            startingGold = 800,
            enemyPlacements = listOf(
                MonsterUnitType.Bear to Position(11, 7),
                NormalUnitType.Melee to Position(13, 5),
                NormalUnitType.Melee to Position(13, 10),
            ),
            xpReward = 200,
            goldReward = 300   // boss bonus
        )
        else -> null
    }

    /** Returns the ScenarioDefinition for a given campaign world + level, or null if not yet implemented. */
    fun campaignBattle(world: Int, level: Int): ScenarioDefinition? = when (world) {
        1 -> world1Battle(level)
        else -> null
    }
}

/**
 * Data class defining a battle scenario.
 *
 * [startingGold] is the gold the player gets to spend in the buy phase.
 * [playerRoster] is optional — if set it bypasses the buy phase (used for quick-test).
 * [xpReward] / [goldReward] are awarded to the Commander on a campaign victory.
 */
data class ScenarioDefinition(
    val name: String,
    val description: String,
    val mapPath: String? = null,
    val startingGold: Int = 1000,
    val playerRoster: List<UnitType>? = null,  // null = player must buy
    val enemyPlacements: List<Pair<UnitType, Position>>,
    val xpReward: Int = 50,
    val goldReward: Int = 200,
    // Sandbox-only: pre-placed units that skip the buy phase entirely
    val playerPlacements: List<Pair<UnitType, Position>>? = null,
    val naturalPlacements: List<Pair<UnitType, Position>>? = null,
    // Sandbox-only: custom tile layout (applied instead of mapPath when set)
    val tileOverrides: List<Pair<Position, TileType>>? = null,
    // Custom player territory. null = default left/right split (x ≤ PLAYER_ZONE_END).
    // When set, only cells in this set belong to the player for placement purposes.
    val playerTerritory: Set<Position>? = null
)

