package com.autobattler.model

import kotlinx.serialization.Serializable

// ─── Commander Archetypes ─────────────────────────────────────────────────────

/**
 * One of three selectable faction archetypes for a Roguelike run.
 * Each archetype defines which units can be purchased during the run,
 * and grants a named passive bonus.
 */
enum class RogueArchetype(
    val displayName: String,
    val lore: String,
    val starterUnitIds: List<String>,
    val passiveName: String,
    val passiveDesc: String,
    val accentColor: String  // CSS/hex description used in UI labels
) {
    IRON_LEGION(
        displayName  = "Iron Legion",
        lore         = "Forged in the crucible of endless war, the Iron Legion holds every line it defends. " +
                        "Thick steel, iron will — they never retreat.",
        starterUnitIds = listOf("melee", "melee_silver", "knight", "healer", "ranged", "tower"),
        passiveName  = "Steel Skin",
        passiveDesc  = "Warriors and Knights start each battle with +3 bonus HP",
        accentColor  = "STEEL GREY"
    ),
    FOREST_WARDENS(
        displayName  = "Forest Wardens",
        lore         = "Children of the ancient wood. They strike before they are seen, " +
                        "heal before wounds fester, and ride the sky when the forest runs out.",
        starterUnitIds = listOf("ranged", "ranged_silver", "healer", "healer_silver", "griffin_rider", "farmer"),
        passiveName  = "Eagle Eye",
        passiveDesc  = "Archers and Griffin Riders deal +2 bonus attack damage",
        accentColor  = "FOREST GREEN"
    ),
    SHADOW_CULT(
        displayName  = "Shadow Cult",
        lore         = "They bartered daylight for dominion over flame. " +
                        "Their wizards incinerate ranks in an instant; their farmers feed the war machine.",
        starterUnitIds = listOf("wizard", "wizard_silver", "melee", "farmer", "ranged", "healer"),
        passiveName  = "Arcane Surge",
        passiveDesc  = "Wizards gain +3 power and fireball radius is increased by 1",
        accentColor  = "ARCANE PURPLE"
    )
}

// ─── Relics ──────────────────────────────────────────────────────────────────

/**
 * A passive bonus item collected during a run.
 * Multiple relics stack (except duplicates, which are prevented on the reward screen).
 */
@Serializable
enum class RelicType(val displayName: String, val description: String) {
    BATTLE_STANDARD  ("Battle Standard",   "+20 starting gold per battle"),
    WAR_DRUM         ("War Drum",          "All units gain +1 speed"),
    HEALING_SIGIL    ("Healing Sigil",     "Healers restore +2 extra HP per heal"),
    IRON_SHIELD      ("Iron Shield",       "+4 max HP to all melee units"),
    SHARPENING_STONE ("Sharpening Stone",  "+2 attack power to all ranged units"),
    COMMANDERS_HORN  ("Commander's Horn",  "+60 starting gold per battle"),
    ARCANE_FOCUS     ("Arcane Focus",      "Wizard fireball radius +1"),
    BOOTS_OF_SPEED   ("Boots of Speed",    "+1 speed to all non-structure units"),
    TROLL_HEART      ("Troll Heart",       "All melee units regenerate 1 HP per turn"),
    GRIFFIN_FEATHER  ("Griffin Feather",   "+2 speed to all flying units"),
    ANCIENT_TOME     ("Ancient Tome",      "+2 attack power to all casters (Wizard, Cleric)"),
    FORTRESS_PLANS   ("Fortress Plans",    "Walls and Gates gain +30 max HP"),
    SHADOW_CLOAK     ("Shadow Cloak",      "Enemy units are -1 speed (minimum 1)"),
    BLOODSTONE       ("Bloodstone",        "Earn +5 gold per enemy kill in addition to base"),
    VETERANS_BADGE   ("Veteran's Badge",   "+6 max HP to all Silver-tier units"),
    DRAGON_SCALE     ("Dragon Scale",      "+8 max HP to every unit"),
    SORCERERS_RING   ("Sorcerer's Ring",   "All Wizards gain +5 power"),
    IRON_WILL        ("Iron Will",         "Your units have a 10% chance to survive a killing blow with 1 HP"),
    LUCKY_CHARM      ("Lucky Charm",       "Earn 25% extra gold from combat rewards")
}

// ─── Node Types ───────────────────────────────────────────────────────────────

@Serializable
enum class RogueNodeType(val displayName: String, val shortLabel: String) {
    COMBAT ("Combat",    "⚔"),
    ELITE  ("Elite",     "💀"),
    REST   ("Rest Site", "⛺"),
    SHOP   ("Shop",      "$"),
    BOSS   ("Boss",      "☠"),
    EVENT  ("Event",     "?")
}

// ─── Node ─────────────────────────────────────────────────────────────────────

/**
 * A single node in the roguelike run map.
 *
 * @param id           Unique per-world index (0..14).
 * @param depth        Visual depth/column on the map (0 = entry, 14 = boss column).
 * @param type         What kind of encounter this node is.
 * @param connectionIds Node IDs that can be reached *from* this node.
 * @param mapPath      Which battle map JSON to load for combat/elite/boss nodes.
 * @param enemyPoolIds Unit type IDs of enemies to place (for combat/elite/boss nodes).
 * @param enemyCount   How many enemies to place from the pool.
 * @param goldReward   Flat gold awarded on winning this node.
 * @param xpReward     XP awarded on winning this node.
 */
@Serializable
data class RogueNode(
    val id: Int,
    val depth: Int,
    val type: RogueNodeType,
    val connectionIds: List<Int>,
    val mapPath: String,
    val enemyPoolIds: List<String>,
    val enemyCount: Int,
    val goldReward: Int,
    val xpReward: Int
)

// ─── World ────────────────────────────────────────────────────────────────────

/**
 * One of three worlds in a run. Contains 15 nodes (depth 0-14) with a boss at depth 14.
 */
@Serializable
data class RogueWorld(
    val worldIndex: Int,
    val seed: Long,
    val nodes: List<RogueNode>
) {
    fun nodeById(id: Int): RogueNode? = nodes.firstOrNull { it.id == id }

    /** All nodes at a given depth. */
    fun nodesAtDepth(depth: Int): List<RogueNode> = nodes.filter { it.depth == depth }

    val maxDepth: Int get() = nodes.maxOf { it.depth }
}

// ─── Run State ────────────────────────────────────────────────────────────────

/**
 * Complete save-state for an active roguelike run.
 * Fully serializable — saved to rogue_run.json between sessions.
 *
 * Gold is persistent between nodes (spend it on units in battle setup).
 * Relics accumulate and their effects are applied during battles.
 */
@Serializable
data class RogueRun(
    val archetype: RogueArchetype,
    val worlds: List<RogueWorld>,
    val currentWorldIndex: Int = 0,
    /** The last node ID that was *entered* (i.e. is now being played or was just completed). */
    val currentNodeId: Int? = null,
    /** Completed node keys formatted as "worldIdx:nodeId". */
    val completedNodeKeys: List<String> = emptyList(),
    val relics: List<RelicType> = emptyList(),
    val gold: Int = 500,
    val runDay: Int = 1,
    val isComplete: Boolean = false,
    val isDefeated: Boolean = false,
    /** Extra unit type IDs unlocked mid-run (via shop or events). */
    val unlockedUnitTypeIds: List<String> = emptyList()
) {
    fun nodeKey(worldIdx: Int, nodeId: Int) = "$worldIdx:$nodeId"

    fun hasCompleted(worldIdx: Int, nodeId: Int) = nodeKey(worldIdx, nodeId) in completedNodeKeys

    val currentWorld: RogueWorld get() = worlds[currentWorldIndex]

    /** Full shop roster: archetype defaults + mid-run unlocks. */
    val shopUnitIds: List<String>
        get() = (archetype.starterUnitIds + unlockedUnitTypeIds).distinct()

    /** Starting gold available when entering a combat node (gold + relic bonuses). */
    val battleBudget: Int get() {
        var bonus = 0
        if (RelicType.BATTLE_STANDARD in relics) bonus += 20
        if (RelicType.COMMANDERS_HORN in relics) bonus += 60
        return gold + bonus
    }

    /** Returns a new run state after successfully completing a node. */
    fun afterNodeWin(worldIdx: Int, nodeId: Int, goldGained: Int): RogueRun {
        val key = nodeKey(worldIdx, nodeId)
        return copy(
            completedNodeKeys = if (key !in completedNodeKeys) completedNodeKeys + key else completedNodeKeys,
            gold = gold + goldGained,
            runDay = runDay + 1,
            currentNodeId = nodeId
        )
    }

    /**
     * Returns the set of node IDs that are now reachable (adjacent to any completed node,
     * or node 0 if no nodes have been completed yet).
     */
    fun reachableNodeIds(worldIdx: Int): Set<Int> {
        val world = worlds[worldIdx]
        val completed = completedNodeKeys.filter { it.startsWith("$worldIdx:") }
            .map { it.substringAfter(":").toInt() }.toSet()

        if (completed.isEmpty()) return setOf(0)  // always start at node 0

        return completed.flatMap { doneId ->
            world.nodeById(doneId)?.connectionIds ?: emptyList()
        }.filter { !hasCompleted(worldIdx, it) }.toSet()
    }

    /** True if all non-boss nodes in the current world (before the boss) are done or bypassed. */
    val currentWorldBossUnlocked: Boolean get() {
        val world = currentWorld
        val bossNode = world.nodes.firstOrNull { it.type == RogueNodeType.BOSS } ?: return false
        return bossNode.id in reachableNodeIds(currentWorldIndex)
    }

    /** World name flavour text. */
    val currentWorldName: String get() = when (currentWorldIndex) {
        0 -> "World I — The Frontier"
        1 -> "World II — The Wastes"
        else -> "World III — The Abyss"
    }
}
