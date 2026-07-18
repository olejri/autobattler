package com.autobattler.model

/** Monster archetypes — used by boss/special enemy units. */
enum class MonsterClass { BEAST, NATURAL_PREDATOR, UNDEAD, HUMANOID_ENEMY, DEMON }

enum class UnitTier(val label: String, val symbol: String) {
    BRONZE("Bronze", ""),
    SILVER("Silver", "(S)"),
    GOLD("Gold", "(G)")
}

/**
 * Base type for all units. Shared stats only — no ability or monster fields here.
 *
 * Subtypes:
 *  - [NormalUnitType]    standard combat units
 *  - [SpecialUnitType]   units with a special ability
 *  - [StructureUnitType] immovable structures with a passive effect
 *  - [MonsterUnitType]   boss/monster units with a MonsterClass
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

    /** True for passive structures; combat structures like [StructureUnitType.Tower] may override this. */
    open val isStructure: Boolean get() = false
    open val tier: UnitTier get() = UnitTier.BRONZE
    open val baseTypeId: String get() = id
    open val canFly: Boolean get() = false
    /** True if this structure blocks unit movement (Wall, Gate, Tower). */
    open val blocksMovement: Boolean get() = false
    /** True if this structure blocks projectiles (Tower only). */
    open val blocksProjectiles: Boolean get() = false
    /** True if this structure allows friendly (same-owner) units to pass through (Gate only). */
    open val allowsFriendlyPassage: Boolean get() = false

    companion object {
        val purchasable: List<UnitType>
            get() = listOf(
                NormalUnitType.Melee, NormalUnitType.MeleeSilver, NormalUnitType.MeleeGold,
                NormalUnitType.Ranged, NormalUnitType.RangedSilver, NormalUnitType.RangedGold,
                NormalUnitType.Healer, NormalUnitType.HealerSilver, NormalUnitType.HealerGold,
                SpecialUnitType.Farmer, SpecialUnitType.FarmerSilver, SpecialUnitType.FarmerGold,
                NormalUnitType.Knight, NormalUnitType.KnightSilver, NormalUnitType.KnightGold,
                SpecialUnitType.Wizard, SpecialUnitType.WizardSilver, SpecialUnitType.WizardGold,
                NormalUnitType.GriffinRider, NormalUnitType.GriffinRiderSilver, NormalUnitType.GriffinRiderGold,
                StructureUnitType.Wall, StructureUnitType.Gate, StructureUnitType.Tower
            )

        val all: List<UnitType>
            get() = purchasable + listOf(
                StructureUnitType.Farm,
                MonsterUnitType.Bear, MonsterUnitType.Wolf,
                MonsterUnitType.Goblin, MonsterUnitType.Skeleton, MonsterUnitType.DarkMage,
                MonsterUnitType.Troll, MonsterUnitType.Bandit, MonsterUnitType.OrcWarrior,
                MonsterUnitType.Spider, MonsterUnitType.Dragon
            )

        fun fromId(id: String): UnitType = when (id) {
            "melee" -> NormalUnitType.Melee
            "melee_silver" -> NormalUnitType.MeleeSilver
            "melee_gold" -> NormalUnitType.MeleeGold
            "ranged" -> NormalUnitType.Ranged
            "ranged_silver" -> NormalUnitType.RangedSilver
            "ranged_gold" -> NormalUnitType.RangedGold
            "healer" -> NormalUnitType.Healer
            "healer_silver" -> NormalUnitType.HealerSilver
            "healer_gold" -> NormalUnitType.HealerGold
            "knight" -> NormalUnitType.Knight
            "knight_silver" -> NormalUnitType.KnightSilver
            "knight_gold" -> NormalUnitType.KnightGold
            "griffin_rider" -> NormalUnitType.GriffinRider
            "griffin_rider_silver" -> NormalUnitType.GriffinRiderSilver
            "griffin_rider_gold" -> NormalUnitType.GriffinRiderGold
            "farmer" -> SpecialUnitType.Farmer
            "farmer_silver" -> SpecialUnitType.FarmerSilver
            "farmer_gold" -> SpecialUnitType.FarmerGold
            "wizard" -> SpecialUnitType.Wizard
            "wizard_silver" -> SpecialUnitType.WizardSilver
            "wizard_gold" -> SpecialUnitType.WizardGold
            "bear" -> MonsterUnitType.Bear
            "wolf" -> MonsterUnitType.Wolf
            "goblin" -> MonsterUnitType.Goblin
            "skeleton" -> MonsterUnitType.Skeleton
            "dark_mage" -> MonsterUnitType.DarkMage
            "troll" -> MonsterUnitType.Troll
            "bandit" -> MonsterUnitType.Bandit
            "orc_warrior" -> MonsterUnitType.OrcWarrior
            "spider" -> MonsterUnitType.Spider
            "dragon" -> MonsterUnitType.Dragon
            "farm" -> StructureUnitType.Farm
            "wall" -> StructureUnitType.Wall
            "gate" -> StructureUnitType.Gate
            "tower" -> StructureUnitType.Tower
            else -> throw IllegalArgumentException("Unknown unit type: $id")
        }
    }
}

sealed class NormalUnitType(
    id: String, displayName: String, maxHp: Int, power: Int, speed: Int,
    cooldown: Int, attackRange: IntRange, size: Int = 1, cost: Int = 0, unlockLevel: Int = 1
) : UnitType(id, displayName, maxHp, power, speed, cooldown, attackRange, size, cost, unlockLevel) {

    data object Melee : NormalUnitType(
        "melee", "Warrior", maxHp = 10, power = 3, speed = 3, cooldown = 2,
        attackRange = 0..1, cost = 50
    )

    data object MeleeSilver : NormalUnitType(
        "melee_silver", "Warrior (S)", maxHp = 15, power = 5, speed = 3, cooldown = 2,
        attackRange = 0..1, cost = 100, unlockLevel = 5
    ) {
        override val tier get() = UnitTier.SILVER
        override val baseTypeId get() = "melee"
    }

    data object MeleeGold : NormalUnitType(
        "melee_gold", "Warrior (G)", maxHp = 20, power = 6, speed = 4, cooldown = 2,
        attackRange = 0..1, cost = 200, unlockLevel = 10
    ) {
        override val tier get() = UnitTier.GOLD
        override val baseTypeId get() = "melee"
    }

    data object Ranged : NormalUnitType(
        "ranged", "Archer", maxHp = 5, power = 5, speed = 1, cooldown = 3,
        attackRange = 3..6, cost = 100, unlockLevel = 2
    )

    data object RangedSilver : NormalUnitType(
        "ranged_silver", "Archer (S)", maxHp = 8, power = 7, speed = 2, cooldown = 3,
        attackRange = 3..6, cost = 200, unlockLevel = 6
    ) {
        override val tier get() = UnitTier.SILVER
        override val baseTypeId get() = "ranged"
    }

    data object RangedGold : NormalUnitType(
        "ranged_gold", "Archer (G)", maxHp = 12, power = 10, speed = 2, cooldown = 3,
        attackRange = 3..6, cost = 400, unlockLevel = 11
    ) {
        override val tier get() = UnitTier.GOLD
        override val baseTypeId get() = "ranged"
    }

    data object Healer : NormalUnitType(
        "healer", "Cleric", maxHp = 8, power = 4, speed = 2, cooldown = 2,
        attackRange = 0..1, cost = 200, unlockLevel = 3
    )

    data object HealerSilver : NormalUnitType(
        "healer_silver", "Cleric (S)", maxHp = 12, power = 6, speed = 3, cooldown = 2,
        attackRange = 0..1, cost = 400, unlockLevel = 7
    ) {
        override val tier get() = UnitTier.SILVER
        override val baseTypeId get() = "healer"
    }

    data object HealerGold : NormalUnitType(
        "healer_gold", "Cleric (G)", maxHp = 16, power = 8, speed = 3, cooldown = 2,
        attackRange = 0..1, cost = 800, unlockLevel = 12
    ) {
        override val tier get() = UnitTier.GOLD
        override val baseTypeId get() = "healer"
    }

    data object Knight : NormalUnitType(
        "knight", "Knight", maxHp = 20, power = 10, speed = 5, cooldown = 2,
        attackRange = 0..1, size = 2, cost = 200, unlockLevel = 5
    )

    data object KnightSilver : NormalUnitType(
        "knight_silver", "Knight (S)", maxHp = 30, power = 15, speed = 5, cooldown = 2,
        attackRange = 0..1, size = 2, cost = 400, unlockLevel = 9
    ) {
        override val tier get() = UnitTier.SILVER
        override val baseTypeId get() = "knight"
    }

    data object KnightGold : NormalUnitType(
        "knight_gold", "Knight (G)", maxHp = 40, power = 20, speed = 6, cooldown = 2,
        attackRange = 0..1, size = 2, cost = 800, unlockLevel = 14
    ) {
        override val tier get() = UnitTier.GOLD
        override val baseTypeId get() = "knight"
    }

    data object GriffinRider : NormalUnitType(
        "griffin_rider", "Griffin Rider", maxHp = 30, power = 15, speed = 8, cooldown = 2,
        attackRange = 0..1, size = 2, cost = 500, unlockLevel = 10
    ) {
        override val canFly get() = true
    }

    data object GriffinRiderSilver : NormalUnitType(
        "griffin_rider_silver", "Griffin Rider (S)", maxHp = 45, power = 22, speed = 8, cooldown = 2,
        attackRange = 0..1, size = 2, cost = 1000, unlockLevel = 14
    ) {
        override val tier get() = UnitTier.SILVER
        override val baseTypeId get() = "griffin_rider"
        override val canFly get() = true
    }

    data object GriffinRiderGold : NormalUnitType(
        "griffin_rider_gold", "Griffin Rider (G)", maxHp = 60, power = 30, speed = 9, cooldown = 2,
        attackRange = 0..1, size = 2, cost = 2000, unlockLevel = 19
    ) {
        override val tier get() = UnitTier.GOLD
        override val baseTypeId get() = "griffin_rider"
        override val canFly get() = true
    }
}

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

    data object FarmerSilver : SpecialUnitType(
        "farmer_silver", "Farmer (S)", maxHp = 2, power = 0, speed = 2, cooldown = 8,
        attackRange = 0..0, cost = 200, unlockLevel = 5,
        ability = SpecialAbility.PlantFarm
    ) {
        override val tier get() = UnitTier.SILVER
        override val baseTypeId get() = "farmer"
    }

    data object FarmerGold : SpecialUnitType(
        "farmer_gold", "Farmer (G)", maxHp = 3, power = 0, speed = 2, cooldown = 6,
        attackRange = 0..0, cost = 400, unlockLevel = 10,
        ability = SpecialAbility.PlantFarm
    ) {
        override val tier get() = UnitTier.GOLD
        override val baseTypeId get() = "farmer"
    }

    data object Wizard : SpecialUnitType(
        "wizard", "Wizard", maxHp = 5, power = 20, speed = 2, cooldown = 6,
        attackRange = 3..10, cost = 300, unlockLevel = 7,
        ability = SpecialAbility.Fireball(power = 20, radius = 1)
    )

    data object WizardSilver : SpecialUnitType(
        "wizard_silver", "Wizard (S)", maxHp = 7, power = 25, speed = 2, cooldown = 5,
        attackRange = 3..10, cost = 600, unlockLevel = 11,
        ability = SpecialAbility.Fireball(power = 25, radius = 1)
    ) {
        override val tier get() = UnitTier.SILVER
        override val baseTypeId get() = "wizard"
    }

    data object WizardGold : SpecialUnitType(
        "wizard_gold", "Wizard (G)", maxHp = 10, power = 30, speed = 3, cooldown = 4,
        attackRange = 3..10, cost = 1200, unlockLevel = 16,
        ability = SpecialAbility.Fireball(power = 30, radius = 1)
    ) {
        override val tier get() = UnitTier.GOLD
        override val baseTypeId get() = "wizard"
    }
}

sealed class StructureUnitType(
    id: String, displayName: String, maxHp: Int,
    unlockLevel: Int = 99,
    power: Int = 0,
    cooldown: Int = 999,
    attackRange: IntRange = 0..0,
    cost: Int = 0,
    val ability: SpecialAbility = SpecialAbility.None
) : UnitType(id, displayName, maxHp, power = power, speed = 0, cooldown = cooldown,
             attackRange = attackRange, cost = cost, unlockLevel = unlockLevel) {

    override val isStructure: Boolean get() = true

    data object Farm : StructureUnitType(
        "farm", "Farm", maxHp = 1, unlockLevel = 99,
        ability = SpecialAbility.YieldGold(goldPerRound = 20)
    )

    /** Impassable wall — blocks all movement, arrows fly over it. Unlocks at commander level 5. */
    data object Wall : StructureUnitType(
        "wall", "Wall", maxHp = 50, unlockLevel = 5, cost = 50
    ) {
        override val blocksMovement: Boolean get() = true
    }

    /**
     * Gate — blocks enemy movement but friendly (same-owner) units can pass through.
     * Arrows fly over it. Unlocks at commander level 5.
     */
    data object Gate : StructureUnitType(
        "gate", "Gate", maxHp = 50, unlockLevel = 5, cost = 75
    ) {
        override val blocksMovement: Boolean get() = true
        override val allowsFriendlyPassage: Boolean get() = true
    }

    /**
     * Tower — impassable defensive turret. Attacks enemies in 0–4 range.
     * Blocks both movement and projectiles. Unlocks at commander level 6.
     */
    data object Tower : StructureUnitType(
        "tower", "Tower", maxHp = 20,
        power = 5, cooldown = 2, attackRange = 0..4,
        unlockLevel = 6, cost = 100
    ) {
        override val isStructure: Boolean get() = false
        override val blocksMovement: Boolean get() = true
        override val blocksProjectiles: Boolean get() = true
    }
}

sealed class MonsterUnitType(
    id: String, displayName: String, maxHp: Int, power: Int, speed: Int,
    cooldown: Int, attackRange: IntRange, size: Int = 1,
    val monsterClass: MonsterClass
) : UnitType(id, displayName, maxHp, power, speed, cooldown, attackRange, size = size) {

    data object Bear : MonsterUnitType(
        "bear", "Bear", maxHp = 40, power = 9, speed = 2, cooldown = 2,
        attackRange = 0..1, size = 2, monsterClass = MonsterClass.BEAST
    )

    data object Wolf : MonsterUnitType(
        "wolf", "Wolf", maxHp = 15, power = 6, speed = 3, cooldown = 2,
        attackRange = 0..1, size = 1, monsterClass = MonsterClass.NATURAL_PREDATOR
    )

    data object Goblin : MonsterUnitType(
        "goblin", "Goblin", maxHp = 6, power = 3, speed = 4, cooldown = 1,
        attackRange = 0..1, size = 1, monsterClass = MonsterClass.HUMANOID_ENEMY
    )

    data object Skeleton : MonsterUnitType(
        "skeleton", "Skeleton", maxHp = 10, power = 4, speed = 2, cooldown = 2,
        attackRange = 0..1, size = 1, monsterClass = MonsterClass.UNDEAD
    )

    data object DarkMage : MonsterUnitType(
        "dark_mage", "Dark Mage", maxHp = 7, power = 7, speed = 1, cooldown = 3,
        attackRange = 3..7, size = 1, monsterClass = MonsterClass.HUMANOID_ENEMY
    )

    data object Troll : MonsterUnitType(
        "troll", "Troll", maxHp = 30, power = 8, speed = 1, cooldown = 3,
        attackRange = 0..1, size = 2, monsterClass = MonsterClass.HUMANOID_ENEMY
    )

    data object Bandit : MonsterUnitType(
        "bandit", "Bandit", maxHp = 8, power = 5, speed = 3, cooldown = 2,
        attackRange = 1..3, size = 1, monsterClass = MonsterClass.HUMANOID_ENEMY
    )

    data object OrcWarrior : MonsterUnitType(
        "orc_warrior", "Orc Warrior", maxHp = 16, power = 6, speed = 2, cooldown = 2,
        attackRange = 0..1, size = 1, monsterClass = MonsterClass.HUMANOID_ENEMY
    )

    data object Spider : MonsterUnitType(
        "spider", "Spider", maxHp = 9, power = 5, speed = 4, cooldown = 2,
        attackRange = 0..1, size = 1, monsterClass = MonsterClass.BEAST
    )

    data object Dragon : MonsterUnitType(
        "dragon", "Dragon", maxHp = 80, power = 12, speed = 2, cooldown = 2,
        attackRange = 0..2, size = 2, monsterClass = MonsterClass.DEMON
    )
}
