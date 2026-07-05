package com.autobattler.model

/**
 * Special abilities that a unit can use in place of (or in addition to) its standard action.
 */
sealed class SpecialAbility {

    /**
     * The unit plants a Farm structure on an adjacent empty tile when its cooldown fires.
     * The farm generates [goldPerRound] gold per combat turn while alive.
     */
    object PlantFarm : SpecialAbility()
    data class YieldGold(val goldPerRound: Int) : SpecialAbility()
}

