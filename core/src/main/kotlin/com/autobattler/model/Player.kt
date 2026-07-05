package com.autobattler.model

import kotlinx.serialization.Serializable

/**
 * Represents a player in the async autobattler.
 */
@Serializable
data class Player(
    val id: String,
    val name: String,
    val team: Team,
    val wins: Int = 0,
    val losses: Int = 0,
    val gold: Int = 0
)

/**
 * Represents a pending async battle challenge.
 */
@Serializable
data class BattleChallenge(
    val id: String,
    val challengerId: String,
    val defenderId: String,
    val challengerTeam: Team,
    val seed: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val resolved: Boolean = false,
    val result: BattleResult? = null
)

