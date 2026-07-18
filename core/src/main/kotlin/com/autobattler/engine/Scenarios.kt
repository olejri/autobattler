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
                NormalUnitType.Melee to Position(12, 6),
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
                NormalUnitType.Ranged to Position(15, 10),
                NormalUnitType.Healer to Position(15, 8),
            ),
            xpReward = 120,
            goldReward = 200
        )
        5 -> ScenarioDefinition(
            name = "1-5: The Sleeping Bear  !",
            description = "BOSS BATTLE\nA massive 2×2 bear guards its den.\nIt can't fit through 1-tile gaps — use them!\n\n⚠  Bring everything you have.",
            mapPath = "maps/tutorial_sleeping_bear.json",
            startingGold = 800,
            enemyPlacements = listOf(
                MonsterUnitType.Bear to Position(11, 7),
                NormalUnitType.Melee to Position(13, 6),
                NormalUnitType.Melee to Position(13, 9),
            ),
            xpReward = 200,
            goldReward = 300   // boss bonus
        )
        else -> null
    }

    // ─── Campaign: World 2 ───────────────────────────────────────────────────────

    fun world2Battle(level: Int): ScenarioDefinition? = when (level) {
        1 -> ScenarioDefinition(
            name = "2-1: Desert Outpost",
            description = "Sun-scorched sands slow everyone down.\nGoblins are fast — don't let them swarm you!\n\n• Sand slows movement (cost 2)",
            mapPath = "maps/world2_desert.json",
            startingGold = 500,
            enemyPlacements = listOf(
                MonsterUnitType.Goblin to Position(10, 4),
                MonsterUnitType.Goblin to Position(10, 7),
                MonsterUnitType.Goblin to Position(10, 10),
                MonsterUnitType.Goblin to Position(11, 6),
                MonsterUnitType.OrcWarrior to Position(13, 7),
            ),
            xpReward = 90,
            goldReward = 220
        )
        2 -> ScenarioDefinition(
            name = "2-2: Swamp Crossing",
            description = "Mud drags at your boots.\nSkeletons don't tire — they'll catch you!\n\nStay on solid ground and hold bridges.",
            mapPath = "maps/world2_swamp.json",
            startingGold = 600,
            enemyPlacements = listOf(
                MonsterUnitType.Skeleton to Position(10, 5),
                MonsterUnitType.Skeleton to Position(10, 8),
                MonsterUnitType.Skeleton to Position(10, 11),
                MonsterUnitType.Goblin to Position(11, 3),
                MonsterUnitType.Goblin to Position(11, 12),
            ),
            xpReward = 110,
            goldReward = 220
        )
        3 -> ScenarioDefinition(
            name = "2-3: Orc Warcamp",
            description = "Fort walls channel you into kill zones.\nBreak through the gate — but watch for archers!\n\nOrc Warriors hit harder than regular troops.",
            mapPath = "maps/world2_orc_camp.json",
            startingGold = 700,
            enemyPlacements = listOf(
                MonsterUnitType.OrcWarrior to Position(11, 4),
                MonsterUnitType.OrcWarrior to Position(11, 11),
                MonsterUnitType.OrcWarrior to Position(12, 7),
                MonsterUnitType.Goblin to Position(14, 5),
                MonsterUnitType.Goblin to Position(14, 10),
                NormalUnitType.Ranged to Position(15, 7),
            ),
            xpReward = 130,
            goldReward = 240
        )
        4 -> ScenarioDefinition(
            name = "2-4: Mountain Pass",
            description = "Rocky terrain compresses the battlefield.\nTrolls are slow but unstoppable — hit-and-run!\n\nA Dark Mage rains spells from the rear.",
            mapPath = "maps/world2_mountain.json",
            startingGold = 800,
            enemyPlacements = listOf(
                MonsterUnitType.Troll to Position(11, 6),
                MonsterUnitType.OrcWarrior to Position(12, 3),
                MonsterUnitType.OrcWarrior to Position(12, 11),
                MonsterUnitType.DarkMage to Position(15, 7),
                MonsterUnitType.Goblin to Position(12, 8),
            ),
            xpReward = 150,
            goldReward = 260
        )
        5 -> ScenarioDefinition(
            name = "2-5: The Dark Fortress  !",
            description = "BOSS BATTLE\nThe dark lord's outer gate — heavily fortified!\nA Troll guards the breach, mages cover the flanks.\n\n⚠  Bring your full force.",
            mapPath = "maps/world2_dark_fortress.json",
            startingGold = 900,
            enemyPlacements = listOf(
                MonsterUnitType.Troll to Position(11, 7),
                MonsterUnitType.DarkMage to Position(14, 4),
                MonsterUnitType.DarkMage to Position(14, 11),
                MonsterUnitType.OrcWarrior to Position(13, 7),
                MonsterUnitType.Skeleton to Position(13, 5),
                MonsterUnitType.Skeleton to Position(13, 10),
            ),
            xpReward = 280,
            goldReward = 400
        )
        else -> null
    }

    // ─── Campaign: World 3 ───────────────────────────────────────────────────────

    fun world3Battle(level: Int): ScenarioDefinition? = when (level) {
        1 -> ScenarioDefinition(
            name = "3-1: Lava Fields",
            description = "Lava rivers split the battlefield into islands.\nOnly narrow land bridges connect them.\n\nOrc Warriors and Skeletons patrol every chokepoint.",
            mapPath = "maps/world3_lava.json",
            startingGold = 700,
            enemyPlacements = listOf(
                MonsterUnitType.OrcWarrior to Position(12, 3),
                MonsterUnitType.OrcWarrior to Position(12, 7),
                MonsterUnitType.OrcWarrior to Position(12, 12),
                MonsterUnitType.Skeleton to Position(14, 5),
                MonsterUnitType.Skeleton to Position(14, 10),
            ),
            xpReward = 150,
            goldReward = 280
        )
        2 -> ScenarioDefinition(
            name = "3-2: Spider's Lair",
            description = "The cave walls close in. Spiders lurk everywhere!\nThey're fast and can squeeze through tight gaps.\n\nWolves have gone feral inside — they attack anyone!",
            mapPath = "maps/world3_spider_cave.json",
            startingGold = 700,
            enemyPlacements = listOf(
                MonsterUnitType.Spider to Position(10, 2),
                MonsterUnitType.Spider to Position(11, 5),
                MonsterUnitType.Spider to Position(10, 8),
                MonsterUnitType.Spider to Position(11, 11),
                MonsterUnitType.Spider to Position(10, 14),
                MonsterUnitType.DarkMage to Position(14, 7),
            ),
            xpReward = 170,
            goldReward = 280
        )
        3 -> ScenarioDefinition(
            name = "3-3: Frozen Tundra",
            description = "The ice makes footing treacherous.\nSkeletons have adapted to the cold — they don't slow down.\n\nA Troll patrols the central corridor.",
            mapPath = "maps/world3_ice.json",
            startingGold = 800,
            enemyPlacements = listOf(
                MonsterUnitType.Troll to Position(12, 7),
                MonsterUnitType.Skeleton to Position(10, 3),
                MonsterUnitType.Skeleton to Position(10, 12),
                MonsterUnitType.DarkMage to Position(14, 7),
                MonsterUnitType.Spider to Position(12, 4),
                MonsterUnitType.Spider to Position(12, 11),
            ),
            xpReward = 190,
            goldReward = 300
        )
        4 -> ScenarioDefinition(
            name = "3-4: Cursed Ruins",
            description = "Dark magic permeates these ancient stones.\nDark Mages use the ruined walls for cover.\nSkeletons guard every archway.\n\n⚠  Kill the mages first — their range is deadly.",
            mapPath = "maps/world3_ruins.json",
            startingGold = 900,
            enemyPlacements = listOf(
                MonsterUnitType.DarkMage to Position(13, 3),
                MonsterUnitType.DarkMage to Position(13, 12),
                MonsterUnitType.Skeleton to Position(11, 5),
                MonsterUnitType.Skeleton to Position(11, 10),
                MonsterUnitType.OrcWarrior to Position(12, 7),
                MonsterUnitType.Spider to Position(14, 7),
                MonsterUnitType.Troll to Position(14, 4),
            ),
            xpReward = 220,
            goldReward = 350
        )
        5 -> ScenarioDefinition(
            name = "3-5: The Dragon  !!",
            description = "FINAL BOSS\nAncient and terrible, the Dragon awaits.\nLava rivers and fort walls protect its lair.\nIts guards are the most powerful yet.\n\n⚠⚠  Bring everything — this is the end.",
            mapPath = "maps/world3_dragon_lair.json",
            startingGold = 1200,
            enemyPlacements = listOf(
                MonsterUnitType.Dragon to Position(14, 6),
                MonsterUnitType.Troll to Position(14, 2),
                MonsterUnitType.Troll to Position(14, 11),
                MonsterUnitType.DarkMage to Position(15, 4),
                MonsterUnitType.DarkMage to Position(15, 10),
                MonsterUnitType.OrcWarrior to Position(10, 7),
                MonsterUnitType.Spider to Position(10, 4),
                MonsterUnitType.Spider to Position(10, 11),
            ),
            xpReward = 500,
            goldReward = 600
        )
        else -> null
    }

    // ─── Ambush ──────────────────────────────────────────────────────────────────

    /**
     * A surprise bandit raid on the camp, scaled to [commanderLevel].
     * Triggered with [Commander.AMBUSH_CHANCE_PCT]% probability when the commander does labor.
     *
     * When [campUnits] is non-empty, those units are pre-placed on the player side and the
     * player does NOT get a buy phase — the camp defends itself as configured.
     * When [campUnits] is empty, the player gets a full gold budget to buy defenders on the fly.
     *
     * [progressesCampaign] is false — winning this battle only awards XP/gold,
     * it does NOT advance the world/level tracker.
     */
    fun ambushBattle(commanderLevel: Int = 1, campUnits: List<CampUnit> = emptyList()): ScenarioDefinition {
        // Scale up enemy roster based on commander level
        val enemies = buildList {
            // Base bandit squad (always present)
            add(MonsterUnitType.Bandit to Position(11, 4))
            add(MonsterUnitType.Bandit to Position(12, 7))
            add(MonsterUnitType.Bandit to Position(11, 11))
            add(MonsterUnitType.Goblin to Position(13, 6))
            add(MonsterUnitType.Goblin to Position(13, 9))
            // Level 3+: add archers in the trees
            if (commanderLevel >= 3) {
                add(NormalUnitType.Ranged to Position(14, 5))
                add(NormalUnitType.Ranged to Position(14, 10))
            }
            // Level 5+: add orc warriors
            if (commanderLevel >= 5) {
                add(MonsterUnitType.OrcWarrior to Position(15, 7))
                add(MonsterUnitType.Bandit to Position(10, 3))
            }
            // Level 7+: add a dark mage commanding from the rear
            if (commanderLevel >= 7) {
                add(MonsterUnitType.DarkMage to Position(15, 4))
                add(MonsterUnitType.DarkMage to Position(15, 11))
            }
            // Level 10+: a troll leads the raid
            if (commanderLevel >= 10) {
                add(MonsterUnitType.Troll to Position(14, 7))
            }
        }

        val xp = 40 + commanderLevel * 8
        val gold = 80 + commanderLevel * 20

        // When the commander has pre-placed camp units, convert them to playerPlacements
        // and give NO buy budget — the camp fights as configured.
        // When the camp is empty, give a full gold budget so the player can buy defenders on the fly.
        val preplacedUnits: List<Pair<UnitType, Position>>? = if (campUnits.isNotEmpty()) {
            campUnits.mapNotNull { cu ->
                runCatching { UnitType.fromId(cu.typeId) }.getOrNull()
                    ?.let { type -> type to Position(cu.gridX, cu.gridY) }
            }
        } else null

        val campDesc = when {
            campUnits.isNotEmpty() -> "Your camp garrison defends!\n${campUnits.size} unit(s) on guard.\n"
            else -> "No camp garrison — spend your gold on defenders!\n"
        }

        return ScenarioDefinition(
            name = "* Bandit Ambush!  (Day Raiders — Lv.$commanderLevel)",
            description = "Bandits raid your camp while you rest!\n$campDesc\n" +
                "⚠  Survive to keep your labor gold.\n" +
                (if (commanderLevel >= 3) "• Archers in the treeline!\n" else "") +
                (if (commanderLevel >= 5) "• Orc enforcers spotted!\n" else "") +
                (if (commanderLevel >= 7) "• A Dark Mage leads them!\n" else "") +
                (if (commanderLevel >= 10) "• A Troll marches at their head!\n" else ""),
            mapPath = "maps/ambush_bandit_camp.json",
            startingGold = 0,
            enemyPlacements = enemies,
            playerPlacements = preplacedUnits,
            xpReward = xp,
            goldReward = gold,
            progressesCampaign = false   // does NOT advance world/level tracker
        )
    }

    /** Returns the ScenarioDefinition for a given campaign world + level, or null if not yet implemented. */
    fun campaignBattle(world: Int, level: Int): ScenarioDefinition? = when (world) {
        1 -> world1Battle(level)
        2 -> world2Battle(level)
        3 -> world3Battle(level)
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
    /**
     * If true, winning this battle advances the campaign world/level tracker and marks the
     * battle as completed. Set false for side-events like ambushes that should only award
     * XP/gold without changing the campaign progression.
     */
    val progressesCampaign: Boolean = true,
    // Sandbox-only: pre-placed units that skip the buy phase entirely
    val playerPlacements: List<Pair<UnitType, Position>>? = null,
    val naturalPlacements: List<Pair<UnitType, Position>>? = null,
    // Sandbox-only: custom tile layout (applied instead of mapPath when set)
    val tileOverrides: List<Pair<Position, TileType>>? = null,
    // Custom player territory. null = default left/right split (x ≤ PLAYER_ZONE_END).
    // When set, only cells in this set belong to the player for placement purposes.
    val playerTerritory: Set<Position>? = null
)

