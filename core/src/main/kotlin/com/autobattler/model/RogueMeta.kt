package com.autobattler.model

import kotlinx.serialization.Serializable

/**
 * Persistent meta-progression that survives across multiple roguelike runs.
 * Saved to `save/rogue_meta.json` — completely separate from campaign and
 * per-run saves.
 *
 * Unlock ladder:
 *   IRON_LEGION    — always available (starter faction)
 *   FOREST_WARDENS — unlock by defeating the World I boss for the first time
 *   SHADOW_CULT    — unlock by defeating the World II boss for the first time
 *
 * Stats accumulate lifetime across all runs and are used to surface a
 * "best run" record on the commander-select screen.
 */
@Serializable
data class RogueMeta(
    // ─── Unlocks ──────────────────────────────────────────────────────────────
    /** Which archetypes the player has permanently unlocked (by enum name). */
    val unlockedArchetypeNames: List<String> = listOf(RogueArchetype.IRON_LEGION.name),

    // ─── Lifetime stats ───────────────────────────────────────────────────────
    val totalRunsStarted:    Int = 0,
    val totalRunsCompleted:  Int = 0,
    val totalRunsDefeated:   Int = 0,
    val totalNodesCleared:   Int = 0,
    val totalBossesDefeated: Int = 0,

    // ─── Best run record ──────────────────────────────────────────────────────
    val bestRunNodesCleared:   Int = 0,
    val bestRunWorldsBeaten:   Int = 0,
    val bestRunRelicsCollected: Int = 0,
    val bestRunArchetypeName:  String = "",

    // ─── Per-world "first clear" flags ────────────────────────────────────────
    /** Set of world indices whose boss has been beaten at least once. */
    val clearedWorldIndices: List<Int> = emptyList(),

    // ─── Run history (last 10 runs) ───────────────────────────────────────────
    val runHistory: List<RunRecord> = emptyList()
) {
    /** Convenience: set of unlocked archetypes. */
    val unlockedArchetypes: Set<RogueArchetype>
        get() = unlockedArchetypeNames
            .mapNotNull { name -> RogueArchetype.values().firstOrNull { it.name == name } }
            .toSet()

    fun isUnlocked(arch: RogueArchetype): Boolean = arch.name in unlockedArchetypeNames

    /** Returns a meta copy updated with a completed world-boss kill. */
    fun afterWorldBoss(worldIndex: Int, @Suppress("UNUSED_PARAMETER") run: RogueRun): RogueMeta {
        val newCleared = if (worldIndex in clearedWorldIndices)
            clearedWorldIndices else clearedWorldIndices + worldIndex

        // Unlock next archetype
        val newUnlocks = unlockedArchetypeNames.toMutableList()
        when (worldIndex) {
            0 -> if (RogueArchetype.FOREST_WARDENS.name !in newUnlocks)
                newUnlocks += RogueArchetype.FOREST_WARDENS.name
            1 -> if (RogueArchetype.SHADOW_CULT.name !in newUnlocks)
                newUnlocks += RogueArchetype.SHADOW_CULT.name
        }

        return copy(
            unlockedArchetypeNames = newUnlocks,
            clearedWorldIndices    = newCleared,
            totalBossesDefeated    = totalBossesDefeated + 1
        )
    }

    /** Returns a meta copy updated at the end of any run (win or defeat). */
    fun afterRunEnd(run: RogueRun, won: Boolean): RogueMeta {
        val nodesCleared  = run.completedNodeKeys.size
        val worldsBeaten  = run.worlds.indices.count { wi ->
            val bossNode = run.worlds[wi].nodes.firstOrNull { it.type == RogueNodeType.BOSS }
            bossNode != null && run.hasCompleted(wi, bossNode.id)
        }
        val relics        = run.relics.size

        val record = RunRecord(
            archetypeName  = run.archetype.name,
            nodesCleared   = nodesCleared,
            worldsBeaten   = worldsBeaten,
            relicsCollected = relics,
            won            = won
        )

        val newBestNodes   = maxOf(bestRunNodesCleared,   nodesCleared)
        val newBestWorlds  = maxOf(bestRunWorldsBeaten,   worldsBeaten)
        val newBestRelics  = maxOf(bestRunRelicsCollected, relics)
        val newBestArch    = if (nodesCleared >= bestRunNodesCleared) run.archetype.name else bestRunArchetypeName

        val newHistory = (listOf(record) + runHistory).take(10)

        return copy(
            totalRunsStarted    = totalRunsStarted,    // already incremented at run start
            totalRunsCompleted  = if (won) totalRunsCompleted + 1 else totalRunsCompleted,
            totalRunsDefeated   = if (!won) totalRunsDefeated + 1 else totalRunsDefeated,
            totalNodesCleared   = totalNodesCleared + nodesCleared,
            bestRunNodesCleared    = newBestNodes,
            bestRunWorldsBeaten    = newBestWorlds,
            bestRunRelicsCollected = newBestRelics,
            bestRunArchetypeName   = newBestArch,
            runHistory             = newHistory
        )
    }

    /** Called when a new run is started; increments the run counter. */
    fun afterRunStart(): RogueMeta = copy(totalRunsStarted = totalRunsStarted + 1)

    /** Unlock condition flavour text for a locked archetype. */
    fun unlockCondition(arch: RogueArchetype): String = when (arch) {
        RogueArchetype.IRON_LEGION    -> "Always unlocked"
        RogueArchetype.FOREST_WARDENS -> "Defeat the World I boss"
        RogueArchetype.SHADOW_CULT    -> "Defeat the World II boss"
    }
}

/** Snapshot of a single completed run stored in run history. */
@Serializable
data class RunRecord(
    val archetypeName:   String,
    val nodesCleared:    Int,
    val worldsBeaten:    Int,
    val relicsCollected: Int,
    val won:             Boolean
)
