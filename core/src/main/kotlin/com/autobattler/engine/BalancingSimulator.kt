package com.autobattler.engine

import com.autobattler.model.*

/**
 * Headless batch-simulation tool for game balance analysis.
 *
 * Runs N deterministic battles between two army compositions on a flat 16×16 grass grid,
 * then returns win rates, average turns, and per-unit-type damage/kill/survival stats.
 *
 * Usage:
 *   val result = BalancingSimulator.runMatchup(
 *       armyA = ArmySpec(listOf(NormalUnitType.Melee to 3), label = "3× Warrior"),
 *       armyB = ArmySpec(listOf(MonsterUnitType.Wolf to 5),  label = "5× Wolf"),
 *       runs  = 200
 *   )
 *   println(result.summary())
 */
object BalancingSimulator {

    // ─── Army spec ──────────────────────────────────────────────────────────────

    /**
     * Describes one side of a matchup.
     *
     * @param units  List of (UnitType, count) pairs.
     * @param label  Human-readable name shown in reports (auto-generated if blank).
     */
    data class ArmySpec(
        val units: List<Pair<UnitType, Int>>,
        val label: String = ""
    ) {
        val totalUnits: Int  get() = units.sumOf { it.second }
        val totalCost: Int   get() = units.sumOf { (type, count) -> type.cost * count }
        val description: String get() = units.joinToString(" + ") { (type, count) ->
            if (count == 1) type.displayName else "${count}× ${type.displayName}"
        }
        val resolvedLabel: String get() = label.ifBlank { description }
    }

    // ─── Per-unit-type stats ─────────────────────────────────────────────────

    /**
     * Accumulated stats for one unit type across all simulations.
     *
     * @param countPerBattle Number of units of this type placed each battle.
     * @param runs           Total battles this type participated in.
     */
    data class UnitTypeStats(
        val type: UnitType,
        val countPerBattle: Int,
        val runs: Int,
        var totalDamageDealt: Int = 0,
        var totalKills: Int = 0,
        var totalSurvivors: Int = 0
    ) {
        /** Average total damage from all units of this type per battle. */
        val avgDamagePerBattle: Double get() = if (runs == 0) 0.0 else totalDamageDealt.toDouble() / runs
        /** Average kills from all units of this type per battle. */
        val avgKillsPerBattle: Double get() = if (runs == 0) 0.0 else totalKills.toDouble() / runs
        /** Fraction of unit instances that survive (0–1). */
        val survivalRate: Double get() {
            val total = countPerBattle * runs
            return if (total == 0) 0.0 else totalSurvivors.toDouble() / total
        }
    }

    // ─── Matchup result ──────────────────────────────────────────────────────

    /** Full result of a multi-run matchup simulation. */
    data class MatchupResult(
        val armyALabel: String,
        val armyBLabel: String,
        val runs: Int,
        val armyAWins: Int,
        val armyBWins: Int,
        val draws: Int,
        val avgTurns: Double,
        val minTurns: Int,
        val maxTurns: Int,
        val armyAStats: List<UnitTypeStats>,
        val armyBStats: List<UnitTypeStats>
    ) {
        val armyAWinRate: Double get() = if (runs == 0) 0.0 else armyAWins.toDouble() / runs * 100.0
        val armyBWinRate: Double get() = if (runs == 0) 0.0 else armyBWins.toDouble() / runs * 100.0
        val drawRate: Double     get() = if (runs == 0) 0.0 else draws.toDouble()     / runs * 100.0

        fun summary(): String = buildString {
            val aw = String.format("%.1f", armyAWinRate)
            val bw = String.format("%.1f", armyBWinRate)
            val dr = String.format("%.1f", drawRate)
            val at = String.format("%.1f", avgTurns)
            appendLine("╔══════════════════════════════════════════════════════════════════╗")
            appendLine("║  MATCHUP: $armyALabel  vs  $armyBLabel")
            appendLine("║  Simulations: $runs")
            appendLine("╠══════════════════════════════════════════════════════════════════╣")
            appendLine("║  $armyALabel wins : $armyAWins ($aw%)")
            appendLine("║  $armyBLabel wins : $armyBWins ($bw%)")
            appendLine("║  Draws              : $draws ($dr%)")
            appendLine("║  Turns              : avg=$at  min=$minTurns  max=$maxTurns")
            appendLine("╠══════════════════════════════════════════════════════════════════╣")
            appendLine("║  [$armyALabel] unit performance:")
            appendUnitStatsRows(armyAStats)
            appendLine("╠══════════════════════════════════════════════════════════════════╣")
            appendLine("║  [$armyBLabel] unit performance:")
            appendUnitStatsRows(armyBStats)
            appendLine("╚══════════════════════════════════════════════════════════════════╝")
        }

        private fun StringBuilder.appendUnitStatsRows(stats: List<UnitTypeStats>) {
            for (s in stats) {
                val name    = s.type.displayName.padEnd(22)
                val dmg     = String.format("%.1f", s.avgDamagePerBattle).padStart(7)
                val kills   = String.format("%.2f", s.avgKillsPerBattle).padStart(5)
                val surv    = String.format("%.0f", s.survivalRate * 100.0).padStart(3)
                appendLine("║    $name  dmg/battle=$dmg  kills/battle=$kills  survival=$surv%")
            }
        }
    }

    // ─── Public API ─────────────────────────────────────────────────────────

    /**
     * Run [runs] headless battles: [armyA] (player side) vs [armyB] (enemy side).
     *
     * Units are auto-placed on a flat all-grass 16×16 grid.
     * Player units are packed toward x=6, enemy units toward x=9.
     *
     * @return Aggregated statistics across all runs.
     */
    fun runMatchup(
        armyA: ArmySpec,
        armyB: ArmySpec,
        runs: Int = 100
    ): MatchupResult {
        require(runs > 0) { "runs must be > 0" }

        val engine = GridBattleEngine()

        var aWins = 0; var bWins = 0; var draws = 0
        var totalTurns = 0; var minTurns = Int.MAX_VALUE; var maxTurns = 0

        // Pre-build per-unit-type stat buckets
        val aStats: Map<String, UnitTypeStats> = armyA.units.associate { (type, count) ->
            type.id to UnitTypeStats(type, countPerBattle = count, runs = runs)
        }
        val bStats: Map<String, UnitTypeStats> = armyB.units.associate { (type, count) ->
            type.id to UnitTypeStats(type, countPerBattle = count, runs = runs)
        }

        repeat(runs) {
            val grid = buildFlatGrid(armyA, armyB)
            val initialUnits = grid.allUnits.toList()

            val outcome = engine.simulate(grid)

            when (outcome.winner) {
                BattleOutcome.Winner.PLAYER -> aWins++
                BattleOutcome.Winner.ENEMY  -> bWins++
                BattleOutcome.Winner.DRAW   -> draws++
            }
            totalTurns += outcome.totalTurns
            if (outcome.totalTurns < minTurns) minTurns = outcome.totalTurns
            if (outcome.totalTurns > maxTurns) maxTurns = outcome.totalTurns

            accumulateStats(outcome, initialUnits, aStats, bStats)
        }

        return MatchupResult(
            armyALabel = armyA.resolvedLabel,
            armyBLabel = armyB.resolvedLabel,
            runs       = runs,
            armyAWins  = aWins,
            armyBWins  = bWins,
            draws      = draws,
            avgTurns   = totalTurns.toDouble() / runs,
            minTurns   = if (minTurns == Int.MAX_VALUE) 0 else minTurns,
            maxTurns   = maxTurns,
            armyAStats = aStats.values.toList(),
            armyBStats = bStats.values.toList()
        )
    }

    /**
     * Run a full cross-product matrix: every [sideA] type (1 unit each) against every
     * [sideB] type (1 unit each).  Returns one [MatchupResult] per pair.
     *
     * Useful for spotting dominant or weak unit types at a glance.
     */
    fun runMatrix(
        sideA: List<UnitType> = unitTypesCombat,
        sideB: List<UnitType> = unitTypesCombat,
        runs: Int = 50
    ): List<MatchupResult> {
        val results = mutableListOf<MatchupResult>()
        for (a in sideA) {
            for (b in sideB) {
                if (a == b) continue  // skip mirror matchups by default
                results += runMatchup(
                    armyA = ArmySpec(listOf(a to 1), label = a.displayName),
                    armyB = ArmySpec(listOf(b to 1), label = b.displayName),
                    runs  = runs
                )
            }
        }
        return results
    }

    /**
     * All non-structure combat unit types (player + monsters), sorted by cost then name.
     * Useful as a default set for matrix runs.
     */
    val unitTypesCombat: List<UnitType> by lazy {
        UnitType.all.filter { !it.isStructure }
    }

    // ─── Grid builder ────────────────────────────────────────────────────────

    /** Build a fresh flat all-grass 16×16 grid and auto-place both armies. */
    private fun buildFlatGrid(armyA: ArmySpec, armyB: ArmySpec): BattleGrid {
        val grid = BattleGrid()
        var nextId = 0

        val playerTypes = armyA.units.flatMap { (type, count) -> List(count) { type } }
        val enemyTypes  = armyB.units.flatMap { (type, count) -> List(count) { type } }

        nextId = placeArmy(grid, playerTypes, BattleUnit.Owner.PLAYER, startId = nextId)
        placeArmy(grid, enemyTypes, BattleUnit.Owner.ENEMY, startId = nextId)

        return grid
    }

    /**
     * Auto-place [types] for [owner] on [grid], packing toward the center divide.
     *
     * Player units start at x=6 and fan left (6,5,4,…).
     * Enemy units start at x=9 and fan right (9,10,11,…).
     * Each column is filled top-to-bottom before moving to the next.
     *
     * Returns the next free unit ID.
     */
    private fun placeArmy(
        grid: BattleGrid,
        types: List<UnitType>,
        owner: BattleUnit.Owner,
        startId: Int
    ): Int {
        var id = startId

        // Column candidates in preference order (nearest center first)
        val xCandidates: List<Int> = when (owner) {
            BattleUnit.Owner.PLAYER  -> (6 downTo 0).toList()
            BattleUnit.Owner.ENEMY   -> (9..15).toList()
            BattleUnit.Owner.NATURAL -> (7..8).toList()
        }

        for (type in types) {
            val size = type.size
            var placed = false

            outer@ for (x in xCandidates) {
                // Verify all tiles of this unit fit within the owner's zone
                val xMax = x + size - 1
                val inZone = when (owner) {
                    BattleUnit.Owner.PLAYER  -> x >= 0 && xMax <= BattleGrid.PLAYER_ZONE_END
                    BattleUnit.Owner.ENEMY   -> x >= BattleGrid.ENEMY_ZONE_START && xMax < BattleGrid.GRID_SIZE
                    BattleUnit.Owner.NATURAL -> x >= 0 && xMax < BattleGrid.GRID_SIZE
                }
                if (!inZone) continue

                for (y in 0..BattleGrid.GRID_SIZE - size) {
                    val unit = BattleUnit(id, type, owner, Position(x, y))
                    if (grid.isValidPlacementForUnit(unit)) {
                        grid.placeUnit(unit)
                        id++
                        placed = true
                        break@outer
                    }
                }
            }

            if (!placed) {
                System.err.println("[BalancingSimulator] WARNING: Could not place ${type.displayName} for $owner")
            }
        }
        return id
    }

    // ─── Stats accumulation ──────────────────────────────────────────────────

    private fun accumulateStats(
        outcome: BattleOutcome,
        initialUnits: List<BattleUnit>,
        aStats: Map<String, UnitTypeStats>,
        bStats: Map<String, UnitTypeStats>
    ) {
        val unitMap = initialUnits.associateBy { it.id }

        fun statsFor(unitId: Int): UnitTypeStats? {
            val unit = unitMap[unitId] ?: return null
            return when (unit.owner) {
                BattleUnit.Owner.PLAYER  -> aStats[unit.type.id]
                BattleUnit.Owner.ENEMY   -> bStats[unit.type.id]
                BattleUnit.Owner.NATURAL -> null
            }
        }

        // Process damage and kill events
        for (event in outcome.events) {
            when (event) {
                is BattleEvent.Attack -> {
                    val s = statsFor(event.attackerId) ?: continue
                    s.totalDamageDealt += event.damage
                    if (event.targetHpAfter <= 0) s.totalKills++
                }
                is BattleEvent.Fireball -> {
                    val s = statsFor(event.casterId) ?: continue
                    s.totalDamageDealt += event.hits.sumOf { it.damage }
                    s.totalKills       += event.hits.count { it.hpAfter <= 0 }
                }
                is BattleEvent.FireBreath -> {
                    val s = statsFor(event.casterId) ?: continue
                    s.totalDamageDealt += event.hits.sumOf { it.damage }
                    s.totalKills       += event.hits.count { it.hpAfter <= 0 }
                }
                is BattleEvent.Cleave -> {
                    val s = statsFor(event.attackerId) ?: continue
                    s.totalDamageDealt += event.primaryDmg + event.splashHits.sumOf { it.damage }
                    if (event.primaryHpAfter <= 0) s.totalKills++
                    s.totalKills += event.splashHits.count { it.hpAfter <= 0 }
                }
                else -> Unit
            }
        }

        // Track survivors: any unit not in the Death events
        val deadIds = outcome.events
            .filterIsInstance<BattleEvent.Death>()
            .map { it.unitId }
            .toHashSet()

        for (unit in initialUnits) {
            if (unit.type.isStructure) continue
            if (unit.id !in deadIds) {
                when (unit.owner) {
                    BattleUnit.Owner.PLAYER  -> aStats[unit.type.id]?.let { it.totalSurvivors++ }
                    BattleUnit.Owner.ENEMY   -> bStats[unit.type.id]?.let { it.totalSurvivors++ }
                    BattleUnit.Owner.NATURAL -> Unit
                }
            }
        }
    }
}
