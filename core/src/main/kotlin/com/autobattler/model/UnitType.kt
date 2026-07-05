package com.autobattler.model

/** Monster archetypes — used by boss/special enemy units. */
enum class MonsterClass { BEAST, NATURAL_PREDATOR }

/**
 * Base type for all units. Shared stats only — no ability or monster fields here.
 *
 * Subtypes:
 *  - [NormalUnitType]    standard combat units (Warrior, Archer, Cleric)
 *  - [SpecialUnitType]   units with a special ability (Farmer → PlantFarm)
 *  - [StructureUnitType] immovable structures with a passive effect (Farm → YieldGold)
 *  - [MonsterUnitType]   boss/monster units with a MonsterClass (Bear)
 *
 * All subtypes can be owned by player OR enemy — ownership is on [BattleUnit], not here.
 */
sealed class UnitType(
    val id: String,
    val displayName: String,
    val maxHp: Int,
    val power: Int,
    val speed: Int,
    val cooldown: Int,
    val attackRange: IntRange,
    val size: Int = 1,
    val cost: Int = 0,
    val unlockLevel: Int = 1,
) {
    /** Gold cost to permanently recruit this unit type for the Commander's roster. */
    val unlockCost: Int get() = cost * 5

    /** True for [StructureUnitType] — structures are passable and targeted last. */
    open val isStructure: Boolean get() = false

    companion object {
        val purchasable: List<UnitType>
            get() = listOf(NormalUnitType.Melee, NormalUnitType.Ranged, NormalUnitType.Healer, SpecialUnitType.Farmer)

        val all: List<UnitType>
            get() = listOf(NormalUnitType.Melee, NormalUnitType.Ranged, NormalUnitType.Healer,
                           MonsterUnitType.Bear, MonsterUnitType.Wolf, SpecialUnitType.Farmer, StructureUnitType.Farm)

        fun fromId(id: String): UnitType = when (id) {
            "melee"  -> NormalUnitType.Melee
            "ranged" -> NormalUnitType.Ranged
            "healer" -> NormalUnitType.Healer
            "bear"   -> MonsterUnitType.Bear
            "wolf"   -> MonsterUnitType.Wolf
            "farmer" -> SpecialUnitType.Farmer
            "farm"   -> StructureUnitType.Farm
            else     -> throw IllegalArgumentException("Unknown unit type: $id")
        }
    }
}

// ─── Normal units ────────────────────────────────────────────────────────────

/** Standard combat units — no special ability. */
sealed class NormalUnitType(
    id: String, displayName: String, maxHp: Int, power: Int, speed: Int,
    cooldown: Int, attackRange: IntRange, size: Int = 1, cost: Int = 0, unlockLevel: Int = 1
) : UnitType(id, displayName, maxHp, power, speed, cooldown, attackRange, size, cost, unlockLevel) {

    data object Melee : NormalUnitType(
        "melee", "Warrior", maxHp = 10, power = 3, speed = 3, cooldown = 2,
        attackRange = 0..1, cost = 50
    )
    data object Ranged : NormalUnitType(
        "ranged", "Archer", maxHp = 5, power = 5, speed = 1, cooldown = 3,
        attackRange = 3..6, cost = 100, unlockLevel = 2
    )
    data object Healer : NormalUnitType(
        "healer", "Cleric", maxHp = 8, power = 4, speed = 2, cooldown = 2,
        attackRange = 0..1, cost = 200, unlockLevel = 3
    )
}

// ─── Special units ───────────────────────────────────────────────────────────

/** Units that carry a [SpecialAbility] triggered on cooldown instead of attacking. */
sealed class SpecialUnitType(
    id: String, displayName: String, maxHp: Int, power: Int, speed: Int,
    cooldown: Int, attackRange: IntRange, cost: Int = 0, unlockLevel: Int = 1,
    val ability: SpecialAbility
) : UnitType(id, displayName, maxHp, power, speed, cooldown, attackRange,
             cost = cost, unlockLevel = unlockLevel) {

    data object Farmer : SpecialUnitType(
        "farmer", "Farmer", maxHp = 1, power = 0, speed = 1, cooldown = 10,
        attackRange = 0..0, cost = 100, unlockLevel = 1,
        ability = SpecialAbility.PlantFarm
    )
}

// ─── Structure units ─────────────────────────────────────────────────────────

/**
 * Immovable structures placed during battle.
 * Always [isStructure] = true — units can walk through them, enemies target them last.
 * Carries a [SpecialAbility] defining its passive effect (e.g. [SpecialAbility.YieldGold]).
 */
sealed class StructureUnitType(
    id: String, displayName: String, maxHp: Int,
    unlockLevel: Int = 99,
    val ability: SpecialAbility
) : UnitType(id, displayName, maxHp, power = 0, speed = 0, cooldown = 999,
             attackRange = 0..0, cost = 0, unlockLevel = unlockLevel) {

    override val isStructure: Boolean get() = true

    data object Farm : StructureUnitType(
        "farm", "Farm", maxHp = 1, unlockLevel = 99,
        ability = SpecialAbility.YieldGold(goldPerRound = 20)
    )
}

// ─── Monster units ───────────────────────────────────────────────────────────

/** Powerful units with a [MonsterClass] archetype — used as bosses or special enemies. */
sealed class MonsterUnitType(
    id: String, displayName: String, maxHp: Int, power: Int, speed: Int,
    cooldown: Int, attackRange: IntRange, size: Int = 1,
    val monsterClass: MonsterClass
) : UnitType(id, displayName, maxHp, power, speed, cooldown, attackRange, size = size) {

    data object Bear : MonsterUnitType(
        "bear", "Bear", maxHp = 40, power = 9, speed = 2, cooldown = 2,
        attackRange = 0..1, size = 2, monsterClass = MonsterClass.BEAST
    )

    /**
     * Neutral wolf — belongs to [BattleUnit.Owner.NATURAL].
     * Stays idle until a player or enemy unit enters within 6 tiles,
     * then pursues and attacks that unit.
     */
    data object Wolf : MonsterUnitType(
        "wolf", "Wolf", maxHp = 15, power = 6, speed = 3, cooldown = 2,
        attackRange = 0..1, size = 1, monsterClass = MonsterClass.NATURAL_PREDATOR
    )
}
