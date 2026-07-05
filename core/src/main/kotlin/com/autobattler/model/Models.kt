package com.autobattler.model

import kotlinx.serialization.Serializable

/**
 * Represents a single unit/character in the autobattler.
 */
@Serializable
data class Unit(
    val id: String,
    val name: String,
    val maxHp: Int,
    var currentHp: Int = maxHp,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val tier: Int = 1
) {
    val isAlive: Boolean get() = currentHp > 0
}

/**
 * A player's team composition.
 */
@Serializable
data class Team(
    val playerId: String,
    val units: List<Unit>
)

/**
 * Mutable state during battle simulation.
 */
data class BattleState(
    val teamA: MutableList<Unit>,
    val teamB: MutableList<Unit>,
    val turnLog: MutableList<String>,
    val seed: Long
)

/**
 * Result of a completed battle.
 */
@Serializable
data class BattleResult(
    val winner: Winner,
    val totalTurns: Int,
    val log: List<String>
) {
    enum class Winner { TEAM_A, TEAM_B, DRAW }
}

