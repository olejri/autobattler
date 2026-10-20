package com.autobattler.engine

import com.autobattler.model.*

/**
 * Procedurally generates the three worlds for a roguelike run.
 *
 * Each world has exactly 15 nodes (indices 0-14):
 *   - Node 0:     Always COMBAT — the run entry point.
 *   - Nodes 1-13: Mix of COMBAT / ELITE / REST / SHOP / EVENT, scaling with depth.
 *   - Node 14:    Always BOSS.
 *
 * Branching structure (fixed topology, content seeded per-world):
 *
 *   0 ──> 1,2
 *   1 ──> 3        2 ──> 3,4
 *   3 ──> 5        4 ──> 5,6
 *   5 ──> 7        6 ──> 7
 *   7 ──> 8,9                    ← mid-convergence
 *   8 ──> 10       9 ──> 10,11
 *   10 ──> 12     11 ──> 12
 *   12 ──> 13,14               ← final branch
 *   13 ──> 14
 *   14 ──> (none)  BOSS
 *
 * Enemy difficulty scales with (depth × worldIndex) so World III, late nodes are hard.
 */
object RogueWorldGenerator {

    // ─── Node topology (deterministic, same for all worlds) ───────────────────

    private data class Slot(val depth: Int, val connections: List<Int>)

    /** Fixed node topology: node id → (depth, [connections]). */
    private val TOPOLOGY: Map<Int, Slot> = mapOf(
        0  to Slot(0,  listOf(1, 2)),
        1  to Slot(1,  listOf(3)),
        2  to Slot(1,  listOf(3, 4)),
        3  to Slot(2,  listOf(5)),
        4  to Slot(2,  listOf(5, 6)),
        5  to Slot(3,  listOf(7)),
        6  to Slot(3,  listOf(7)),
        7  to Slot(4,  listOf(8, 9)),
        8  to Slot(5,  listOf(10)),
        9  to Slot(5,  listOf(10, 11)),
        10 to Slot(6,  listOf(12)),
        11 to Slot(6,  listOf(12)),
        12 to Slot(7,  listOf(13, 14)),
        13 to Slot(8,  listOf(14)),
        14 to Slot(9,  emptyList())
    )

    // ─── Enemy pools per difficulty tier ─────────────────────────────────────

    private val earlyEnemies  = listOf("melee", "melee", "ranged", "goblin", "bandit", "goblin")
    private val midEnemies    = listOf("orc_warrior", "skeleton", "goblin", "ranged", "dark_mage", "bandit")
    private val lateEnemies   = listOf("orc_warrior", "skeleton", "dark_mage", "troll", "spider", "wolf")
    private val eliteEnemies  = listOf("troll", "orc_warrior", "dark_mage", "bear", "spider", "skeleton")

    /** Boss + retinue per world (index 0/1/2). */
    private val bossLineups = listOf(
        listOf("bear", "melee", "melee", "goblin", "goblin"),
        listOf("troll", "orc_warrior", "dark_mage", "skeleton", "skeleton"),
        listOf("dragon", "troll", "troll", "dark_mage", "dark_mage")
    )

    // ─── Maps per world ───────────────────────────────────────────────────────

    private val worldMaps = listOf(
        listOf(
            "maps/tutorial_river.json",
            "maps/tutorial_high_forest.json",
            "maps/tutorial_sleeping_bear.json"
        ),
        listOf(
            "maps/world2_desert.json",
            "maps/world2_swamp.json",
            "maps/world2_orc_camp.json",
            "maps/world2_mountain.json"
        ),
        listOf(
            "maps/world3_lava.json",
            "maps/world3_spider_cave.json",
            "maps/world3_ice.json",
            "maps/world3_ruins.json"
        )
    )

    private val bossMaps = listOf(
        "maps/tutorial_sleeping_bear.json",
        "maps/world2_dark_fortress.json",
        "maps/world3_dragon_lair.json"
    )

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Generates all three worlds for a fresh run.
     * Seeds are derived from [baseSeed] with world-index offsets for independence.
     */
    fun generateWorlds(baseSeed: Long = System.currentTimeMillis()): List<RogueWorld> =
        List(3) { worldIdx -> generateWorld(worldIdx, baseSeed + worldIdx * 997L) }

    /** Generates a single world deterministically from [worldIndex] and [seed]. */
    fun generateWorld(worldIndex: Int, seed: Long): RogueWorld {
        val rng = java.util.Random(seed)

        val nodes = TOPOLOGY.entries
            .sortedBy { it.key }
            .map { (id, slot) ->
                buildNode(id = id, depth = slot.depth, connections = slot.connections,
                          worldIndex = worldIndex, rng = rng)
            }

        return RogueWorld(worldIndex = worldIndex, seed = seed, nodes = nodes)
    }

    // ─── Node construction ───────────────────────────────────────────────────

    private fun buildNode(
        id: Int,
        depth: Int,
        connections: List<Int>,
        worldIndex: Int,
        rng: java.util.Random
    ): RogueNode {
        val type = when {
            depth == 0  -> RogueNodeType.COMBAT  // always an entry combat
            depth == 9  -> RogueNodeType.BOSS    // always boss at max depth
            depth == 4  -> if (rng.nextFloat() < 0.5f) RogueNodeType.REST else RogueNodeType.SHOP
            depth == 7  -> if (rng.nextFloat() < 0.6f) RogueNodeType.REST else RogueNodeType.EVENT
            depth % 3 == 2 && rng.nextFloat() < 0.35f -> RogueNodeType.ELITE
            rng.nextFloat() < 0.15f -> RogueNodeType.EVENT
            rng.nextFloat() < 0.12f -> RogueNodeType.SHOP
            else -> RogueNodeType.COMBAT
        }

        val map = if (type == RogueNodeType.BOSS) {
            bossMaps[worldIndex.coerceIn(0, bossMaps.lastIndex)]
        } else {
            val maps = worldMaps[worldIndex.coerceIn(0, worldMaps.lastIndex)]
            maps[rng.nextInt(maps.size)]
        }

        val (enemies, count) = buildEnemyPool(type, depth, worldIndex, rng)

        return RogueNode(
            id            = id,
            depth         = depth,
            type          = type,
            connectionIds = connections,
            mapPath       = map,
            enemyPoolIds  = enemies,
            enemyCount    = count,
            goldReward    = goldRewardFor(depth, type, worldIndex),
            xpReward      = xpRewardFor(depth, type, worldIndex)
        )
    }

    // ─── Enemy pool ──────────────────────────────────────────────────────────

    private fun buildEnemyPool(
        type: RogueNodeType,
        depth: Int,
        worldIndex: Int,
        rng: java.util.Random
    ): Pair<List<String>, Int> {

        if (type == RogueNodeType.BOSS) {
            val lineup = bossLineups[worldIndex.coerceIn(0, bossLineups.lastIndex)]
            return lineup to lineup.size
        }
        if (type == RogueNodeType.REST || type == RogueNodeType.SHOP || type == RogueNodeType.EVENT) {
            return emptyList<String>() to 0
        }

        // Scale enemy count: more as depth + world increase
        val base = when {
            depth < 3  -> 2
            depth < 6  -> 3
            depth < 8  -> 4
            else       -> 5
        }
        val count = (base + worldIndex).coerceAtMost(8)

        val pool = when {
            type == RogueNodeType.ELITE           -> eliteEnemies
            depth < 3                             -> earlyEnemies
            depth < 6                             -> earlyEnemies + midEnemies
            depth < 8                             -> midEnemies
            else                                  -> lateEnemies
        }

        // Pick [count] enemies, allowing repeats for balance (enemies appear multiple times in pool)
        val chosen = (1..count).map { pool[rng.nextInt(pool.size)] }
        return chosen to count
    }

    // ─── Reward helpers ──────────────────────────────────────────────────────

    private fun goldRewardFor(depth: Int, type: RogueNodeType, world: Int): Int = when (type) {
        RogueNodeType.BOSS  -> 350 + depth * 25 + world * 50
        RogueNodeType.ELITE -> 130 + depth * 18 + world * 20
        RogueNodeType.REST,
        RogueNodeType.SHOP  -> 0
        RogueNodeType.EVENT -> 60
        else                -> 80 + depth * 12 + world * 10
    }

    private fun xpRewardFor(depth: Int, type: RogueNodeType, world: Int): Int = when (type) {
        RogueNodeType.BOSS  -> 250 + depth * 20 + world * 40
        RogueNodeType.ELITE -> 100 + depth * 10 + world * 15
        RogueNodeType.REST,
        RogueNodeType.SHOP,
        RogueNodeType.EVENT -> 20
        else                -> 40 + depth * 8 + world * 8
    }
}
