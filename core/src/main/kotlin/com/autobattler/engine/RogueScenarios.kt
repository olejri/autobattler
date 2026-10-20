package com.autobattler.engine

import com.autobattler.model.*

/**
 * Converts a [RogueNode] into a fully-playable [ScenarioDefinition].
 *
 * Enemy positions are procedurally spread across the enemy half of the grid
 * (x = 9..15, y = 1..14) using a deterministic pattern based on enemy count.
 * Relics that modify enemy behaviour (Shadow Cloak, etc.) are handled in
 * the battle engine rather than here, so this builder stays data-only.
 */
object RogueScenarios {

    /**
     * Spreads up to [maxEnemies] enemies across standard enemy-zone positions.
     * The positions are carefully chosen to avoid extreme edges so units always
     * appear on valid grass/terrain regardless of the chosen map.
     */
    private val enemyPositionGrid: List<Position> = listOf(
        Position(11, 7),
        Position(12, 9),
        Position(12, 5),
        Position(13, 7),
        Position(11, 11),
        Position(11, 4),
        Position(13, 11),
        Position(14, 6),
        Position(14, 9),
        Position(12, 13),
        Position(10, 7),
        Position(14, 3)
    )

    /**
     * Build a [ScenarioDefinition] from a roguelike [node] in [world] during the run.
     *
     * @param run  The current run state (used to read the battle budget).
     * @param node The node being entered.
     * @param world The world this node belongs to.
     */
    fun buildScenario(run: RogueRun, node: RogueNode, world: RogueWorld): ScenarioDefinition {
        val worldNum   = world.worldIndex + 1
        val depthLabel = when (node.type) {
            RogueNodeType.BOSS  -> "BOSS"
            RogueNodeType.ELITE -> "ELITE"
            else                -> "Node ${node.depth + 1}"
        }
        val name = "World $worldNum — $depthLabel  [${node.type.shortLabel}]"

        val description = when (node.type) {
            RogueNodeType.BOSS  -> buildBossDescription(node, world.worldIndex)
            RogueNodeType.ELITE -> buildEliteDescription(node)
            RogueNodeType.COMBAT -> buildCombatDescription(node)
            else -> "Encounter — fight to advance."
        }

        val placements = buildEnemyPlacements(node)

        // Gold budget = run.battleBudget (includes relic bonuses).
        // We use goldReward == 0 here because gold earned from kills + node reward
        // is computed in the rogue callbacks on the result screen (not here).
        return ScenarioDefinition(
            name             = name,
            description      = description,
            mapPath          = node.mapPath,
            startingGold     = run.battleBudget,
            enemyPlacements  = placements,
            xpReward         = node.xpReward,
            goldReward       = 0,          // rogue mode pays out gold via RogueRewardScreen
            progressesCampaign = false     // never advances campaign state
        )
    }

    // ─── Enemy placement helpers ─────────────────────────────────────────────

    private fun buildEnemyPlacements(node: RogueNode): List<Pair<UnitType, Position>> {
        if (node.type == RogueNodeType.REST || node.type == RogueNodeType.SHOP ||
            node.type == RogueNodeType.EVENT || node.enemyPoolIds.isEmpty()) {
            return emptyList()
        }

        val positions = enemyPositionGrid.take(
            node.enemyPoolIds.size.coerceAtMost(enemyPositionGrid.size)
        )

        return node.enemyPoolIds
            .take(positions.size)
            .zip(positions)
            .mapNotNull { (typeId, pos) ->
                val unitType = runCatching { UnitType.fromId(typeId) }.getOrNull() ?: return@mapNotNull null
                unitType to pos
            }
    }

    // ─── Description builders ─────────────────────────────────────────────────

    private fun buildCombatDescription(node: RogueNode): String {
        val enemyNames = node.enemyPoolIds
            .mapNotNull { runCatching { UnitType.fromId(it).displayName }.getOrNull() }
            .distinct()
            .joinToString(", ")
        return "Combat encounter — defeat all enemies to continue.\n\nEnemies: $enemyNames"
    }

    private fun buildEliteDescription(node: RogueNode): String {
        val enemyNames = node.enemyPoolIds
            .mapNotNull { runCatching { UnitType.fromId(it).displayName }.getOrNull() }
            .distinct()
            .joinToString(", ")
        return "⚠ ELITE ENCOUNTER\nA tougher group defends this path.\nVictory brings a relic reward.\n\nEnemies: $enemyNames"
    }

    private fun buildBossDescription(node: RogueNode, worldIndex: Int): String {
        val bossName = node.enemyPoolIds.firstOrNull()
            ?.let { runCatching { UnitType.fromId(it).displayName }.getOrNull() } ?: "Boss"
        val worldFlavour = when (worldIndex) {
            0 -> "The frontier guardian stands in your way."
            1 -> "The warlord's champion bars the gate."
            else -> "The ancient terror awakens."
        }
        return "☠ BOSS BATTLE\n$worldFlavour\n\n$bossName leads the defence.\n⚠  Bring everything you have."
    }
}
