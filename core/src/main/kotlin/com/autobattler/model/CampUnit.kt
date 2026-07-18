package com.autobattler.model

import kotlinx.serialization.Serializable

/**
 * A single unit permanently stationed at the commander's camp.
 *
 * Camp units defend during bandit ambushes. They persist between ambushes:
 * survivors stay in the roster (HP restored to full after each fight);
 * units that die in battle are permanently removed.
 *
 * Positions are in BattleGrid coordinates (x 0..7 = player side).
 */
@Serializable
data class CampUnit(
    val typeId: String,
    val gridX: Int,
    val gridY: Int
)
