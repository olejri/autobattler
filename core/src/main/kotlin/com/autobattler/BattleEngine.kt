package com.autobattler

import com.autobattler.model.BattleResult
import com.autobattler.model.BattleState
import com.autobattler.model.Team
import com.autobattler.model.Unit

/**
 * Core battle simulation engine.
 * Runs deterministically so battles can be resolved async
 * and replayed identically on both clients.
 */
class BattleEngine {

    /**
     * Simulates a full battle between two teams.
     * This is deterministic given the same seed — enabling async play.
     */
    fun simulate(teamA: Team, teamB: Team, seed: Long = System.currentTimeMillis()): BattleResult {
        val state = BattleState(
            teamA = teamA.units.map { it.copy() }.toMutableList(),
            teamB = teamB.units.map { it.copy() }.toMutableList(),
            turnLog = mutableListOf(),
            seed = seed
        )

        var turn = 0
        val random = java.util.Random(seed)

        while (state.teamA.any { it.isAlive } && state.teamB.any { it.isAlive }) {
            turn++
            resolveTurn(state, random, turn)

            // Safety cap to prevent infinite battles
            if (turn >= 100) break
        }

        val winner = when {
            state.teamA.any { it.isAlive } -> BattleResult.Winner.TEAM_A
            state.teamB.any { it.isAlive } -> BattleResult.Winner.TEAM_B
            else -> BattleResult.Winner.DRAW
        }

        return BattleResult(winner = winner, totalTurns = turn, log = state.turnLog)
    }

    private fun resolveTurn(state: BattleState, random: java.util.Random, turnNumber: Int) {
        val aliveA = state.teamA.filter { it.isAlive }
        val aliveB = state.teamB.filter { it.isAlive }

        if (aliveA.isEmpty() || aliveB.isEmpty()) return

        // Each alive unit attacks a random enemy
        for (attacker in aliveA) {
            val target = aliveB.filter { it.isAlive }.randomOrNull(random) ?: break
            val damage = calculateDamage(attacker, target, random)
            target.currentHp -= damage
            state.turnLog.add("Turn $turnNumber: ${attacker.name} deals $damage damage to ${target.name} (${target.currentHp}/${target.maxHp} HP)")
        }

        for (attacker in aliveB) {
            val target = aliveA.filter { it.isAlive }.randomOrNull(random) ?: break
            val damage = calculateDamage(attacker, target, random)
            target.currentHp -= damage
            state.turnLog.add("Turn $turnNumber: ${attacker.name} deals $damage damage to ${target.name} (${target.currentHp}/${target.maxHp} HP)")
        }
    }

    private fun calculateDamage(attacker: Unit, defender: Unit, random: java.util.Random): Int {
        val baseDamage = (attacker.attack - defender.defense / 2).coerceAtLeast(1)
        // Add some variance: ±20%
        val variance = (baseDamage * 0.2).toInt()
        return baseDamage + random.nextInt(variance.coerceAtLeast(1) * 2 + 1) - variance
    }

    private fun <T> List<T>.randomOrNull(random: java.util.Random): T? {
        if (isEmpty()) return null
        return this[random.nextInt(size)]
    }
}

