package com.autobattler.model

import kotlinx.serialization.Serializable

private const val STARTING_GOLD = 300

/**
 * The Commander is the player's persistent identity across campaign sessions.
 * This is the complete save state — everything else is derived or dynamic at runtime.
 *
 * Unit unlocks (player-driven, not automatic):
 *   - Level 1:  Warrior always unlocked (starter)
 *   - Level 2+: Archer can be unlocked for 500g (5× battle cost)
 *   - Level 3+: Cleric can be unlocked for 1000g (5× battle cost)
 *
 * Progression:
 *   - World 1 has 5 levels (1-1 through 1-5, where 1-5 is the boss)
 *   - currentWorld / currentLevel track the next battle to fight
 */
@Serializable
data class Commander(
    val name: String,
    val level: Int = 1,
    val xp: Int = 0,
    val gold: Int = STARTING_GOLD,
    val unlockedUnitTypeIds: List<String> = listOf("melee", "farmer"),
    val currentWorld: Int = 1,
    val currentLevel: Int = 1,
    val completedBattleIds: List<String> = emptyList(),
    val saveVersion: Int = 1
) {
    /** e.g. "1-3" */
    val battleId: String get() = "$currentWorld-$currentLevel"

    /** XP required to reach the next commander level. */
    val xpForNextLevel: Int get() = level * 100

    val isBossLevel: Boolean get() = currentLevel == 5

    /** Live unit types based on unlock list. */
    val unlockedUnitTypes: List<UnitType>
        get() = unlockedUnitTypeIds.mapNotNull { id ->
            runCatching { UnitType.fromId(id) }.getOrNull()
        }

    /**
     * Returns a new Commander state after winning a battle.
     * Does NOT mutate — call this when the player clicks "Continue" on the result screen.
     *
     * @param xpReward    XP to award (from scenario)
     * @param goldEarned  Gold earned from enemy kills + completion bonus
     * @param goldSpent   Gold spent buying units in the setup phase (deducted from pool)
     */
    fun afterVictory(xpReward: Int, goldEarned: Int, goldSpent: Int = 0): Commander {
        // Mark battle as completed
        val newCompleted = if (battleId !in completedBattleIds)
            completedBattleIds + battleId else completedBattleIds

        // Advance to next battle
        val (nextWorld, nextLevel) = when {
            currentLevel < 5 -> currentWorld to currentLevel + 1
            else -> currentWorld + 1 to 1
        }

        // Commander level-up (can gain multiple levels at once)
        var lvl = level
        var remainingXp = xp + xpReward
        while (remainingXp >= lvl * 100) {
            remainingXp -= lvl * 100
            lvl++
        }

        return copy(
            level = lvl,
            xp = remainingXp,
            gold = gold - goldSpent + goldEarned,
            completedBattleIds = newCompleted,
            currentWorld = nextWorld,
            currentLevel = nextLevel
        )
    }

    /**
     * Whether the player can spend gold to unlock [type] right now.
     * Conditions: commander level is high enough, unit is not yet unlocked, and gold is sufficient.
     */
    fun canUnlock(type: UnitType): Boolean =
        type.unlockLevel > 1 &&
        level >= type.unlockLevel &&
        type.id !in unlockedUnitTypeIds &&
        gold >= type.unlockCost

    /**
     * Returns a new Commander with [type] added to the roster and [unlockCost] deducted from gold.
     * Only call after confirming [canUnlock] returns true.
     */
    fun unlock(type: UnitType): Commander {
        require(canUnlock(type)) { "Cannot unlock ${type.displayName}: requirements not met" }
        return copy(
            gold = gold - type.unlockCost,
            unlockedUnitTypeIds = unlockedUnitTypeIds + type.id
        )
    }

    /** Checks whether a specific battle has been completed before. */
    fun hasCompleted(world: Int, level: Int) = "$world-$level" in completedBattleIds
}
