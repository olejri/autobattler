package com.autobattler.model

/**
 * Special abilities that a unit can use in place of (or in addition to) its standard action.
 */
sealed class SpecialAbility {
    object PlantFarm : SpecialAbility()
    data class YieldGold(val goldPerRound: Int) : SpecialAbility()
    /** Fires an AoE ball dealing [power] damage in radius [radius] tiles around the target, hitting ALL units (friend + foe). */
    data class Fireball(val power: Int, val radius: Int = 1) : SpecialAbility()
    /** Unit can fly — ignores terrain passability when pathfinding. */
    object Flying : SpecialAbility()
}
