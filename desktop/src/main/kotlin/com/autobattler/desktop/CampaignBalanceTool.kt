package com.autobattler.desktop

import com.autobattler.engine.BalancingSimulator
import com.autobattler.engine.BalancingSimulator.ArmySpec
import com.autobattler.engine.GridBattleEngine
import com.autobattler.engine.BattleOutcome
import com.autobattler.engine.BattleEvent
import com.autobattler.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

// ─── Constants matching BattleScreen / Commander ────────────────────────────
private const val GOLD_PER_KILL    = 10
private const val LABOR_GOLD_MULT  = 100      // level * 100 per day
private const val AMBUSH_CHANCE    = 0.20
private const val FARM_GOLD_ROUND  = 20       // SpecialAbility.YieldGold.goldPerRound

// ─── World 1 enemy definitions (from Scenarios.kt) ─────────────────────────
private data class EnemyEntry(val type: UnitType, val count: Int)

private val ENEMIES_11 = listOf(EnemyEntry(NormalUnitType.Melee, 2))
private val ENEMIES_12 = listOf(EnemyEntry(NormalUnitType.Melee, 3), EnemyEntry(NormalUnitType.Ranged, 1))
private val ENEMIES_13 = listOf(EnemyEntry(NormalUnitType.Melee, 3), EnemyEntry(NormalUnitType.Ranged, 1))
private val ENEMIES_14 = listOf(EnemyEntry(NormalUnitType.Melee, 2), EnemyEntry(NormalUnitType.Ranged, 2), EnemyEntry(NormalUnitType.Healer, 1))
private val ENEMIES_15 = listOf(EnemyEntry(MonsterUnitType.Bear, 1), EnemyEntry(NormalUnitType.Melee, 2))

// ─── XP/gold rewards (from Scenarios.kt) ────────────────────────────────────
private data class LevelRewards(val xp: Int, val goldBonus: Int, val enemyCount: Int)
private val REWARDS = mapOf(
    "1-1" to LevelRewards(xp = 60,  goldBonus = 200, enemyCount = 2),
    "1-2" to LevelRewards(xp = 80,  goldBonus = 200, enemyCount = 4),
    "1-3" to LevelRewards(xp = 100, goldBonus = 200, enemyCount = 4),
    "1-4" to LevelRewards(xp = 120, goldBonus = 200, enemyCount = 5),
    "1-5" to LevelRewards(xp = 200, goldBonus = 300, enemyCount = 3),
)

// ─── Entry point ─────────────────────────────────────────────────────────────
fun main() {
    val report = StringBuilder()
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())

    println("╔══════════════════════════════════════════════════════════════════╗")
    println("║         AUTOBATTLER — WORLD 1 CAMPAIGN BALANCE ANALYSIS         ║")
    println("╚══════════════════════════════════════════════════════════════════╝")
    println()

    report.appendLine("WORLD 1 CAMPAIGN BALANCE REPORT — $timestamp")
    report.appendLine("=".repeat(70))
    report.appendLine()

    // ── Step 1: Simulate realistic gold/XP progression across the campaign ──
    val progressionText = simulateProgression()
    println(progressionText)
    report.appendLine(progressionText)

    // ── Step 2: For each level, test multiple army builds and report win rates ──
    simulateLevel11(report)
    simulateLevel12(report)
    simulateLevel13(report)
    simulateLevel14(report)
    simulateLevel15(report)

    // ── Step 3: Write report ─────────────────────────────────────────────────
    val dir = File("balance_reports"); if (!dir.exists()) dir.mkdirs()
    val outFile = File(dir, "campaign_balance_$timestamp.txt")
    outFile.writeText(report.toString())
    println()
    println("Full report saved to: ${outFile.absolutePath}")
}

// ─── Progression tracker ──────────────────────────────────────────────────────

/**
 * Simulates a "typical player path" through World 1 to establish realistic gold
 * and level at each battle entry. Assumes:
 *  - Player spends the "minimum needed to win" (estimated conservatively)
 *  - Player does 1 labor day between each battle
 *  - 20% bandit ambush chance handled as expected value (0.8 * laborGold)
 *  - Kill gold = all enemies killed (best case, assumed for progression)
 */
private fun simulateProgression(): String {
    data class State(val gold: Int, val level: Int, val xp: Int)

    val sb = StringBuilder()
    sb.appendLine("══════════════════════════════════════════════════════════════════════")
    sb.appendLine("  EXPECTED GOLD / LEVEL PROGRESSION (1 labor day between each battle)")
    sb.appendLine("  Assumptions: spends minimum viable army, 1 labor day between battles,")
    sb.appendLine("  80% of labor gold collected (20% bandit chance = gold at risk).")
    sb.appendLine("══════════════════════════════════════════════════════════════════════")
    sb.appendLine()
    sb.appendLine(String.format("  %-10s %-8s %-8s %-10s %-28s", "Battle", "Level", "XP", "Gold In", "Notes"))
    sb.appendLine("  " + "-".repeat(66))

    // Initial state
    var gold = 300
    var level = 1
    var xp = 0

    val levels = listOf("1-1", "1-2", "1-3", "1-4", "1-5")
    val spendEstimates = mapOf(
        "1-1" to 200, // 4× Warrior
        "1-2" to 250, // 5× Warrior
        "1-3" to 300, // 3× Warrior + 1× Archer (if unlocked) or 6× Warrior
        "1-4" to 400, // mix of warriors + archers
        "1-5" to 600  // full army
    )

    for (id in levels) {
        val rewards = REWARDS[id]!!
        val spend = spendEstimates[id]!!
        val note = when {
            id == "1-2" && level < 2 -> "Level ${level}, no Archer yet"
            id == "1-3" && level >= 2 -> "Level ${level}, Archer available (500g unlock)"
            id == "1-4" && level >= 3 -> "Level ${level}, Cleric available (1000g unlock)"
            else -> "Level ${level}, unlocked: ${unlockedNames(level)}"
        }
        sb.appendLine(String.format("  %-10s %-8d %-8d %-10d %s", id, level, xp, gold, note))

        // Simulate battle outcome
        val spentActual = minOf(spend, gold)
        val killGold = rewards.enemyCount * GOLD_PER_KILL
        gold = gold - spentActual + killGold + rewards.goldBonus

        // XP + level up
        xp += rewards.xp
        while (xp >= level * 100) { xp -= level * 100; level++ }

        // Labor day between battles (except after 1-5)
        if (id != "1-5") {
            val laborEarned = (level * LABOR_GOLD_MULT * (1.0 - AMBUSH_CHANCE)).toInt()
            gold += laborEarned
            sb.appendLine("  ${"".padEnd(10)} ${"".padEnd(8)} ${"".padEnd(8)} ${"".padEnd(10)} Labor day +${laborEarned}g (level $level, 80pct expected)")
        }
    }
    sb.appendLine()
    sb.appendLine(String.format("  After 1-5: Level $level, XP $xp, Gold ~${gold}g"))
    sb.appendLine()
    return sb.toString()
}

private fun unlockedNames(level: Int): String = buildString {
    append("Warrior, Farmer")
    if (level >= 2) append(", [Archer avail.]")
    if (level >= 3) append(", [Cleric avail.]")
    if (level >= 5) append(", [Knight avail.]")
}

// ─── Per-level simulations ────────────────────────────────────────────────────

private fun simulateLevel11(report: StringBuilder) {
    val title = "1-1: THE RIVER CROSSING — enemy: 2× Warrior"
    val enemy = buildEnemySpec(ENEMIES_11)

    // Player is level 1, 300g, has: Warrior (50g), Farmer (100g)
    // Farmer plants a Farm → 20g/round passive income
    val armies = listOf(
        // Bare minimum viable
        build("2× Warrior",               NormalUnitType.Melee to 2),                                      // 100g
        build("3× Warrior",               NormalUnitType.Melee to 3),                                      // 150g
        build("4× Warrior",               NormalUnitType.Melee to 4),                                      // 200g
        build("6× Warrior (all-in)",      NormalUnitType.Melee to 6),                                      // 300g
        build("2× Warrior + 1× Farmer",   NormalUnitType.Melee to 2, SpecialUnitType.Farmer to 1),         // 200g
        build("3× Warrior + 1× Farmer",   NormalUnitType.Melee to 3, SpecialUnitType.Farmer to 1),         // 250g
    )
    runLevelSection(title, enemy, armies, goldBudget = 300, report = report, note =
        "Player gold: 300g starting | Unlocked: Warrior(50g) Farmer(100g) | No Archer yet\n" +
        "Farmer plants a Farm → +20g/round passive (doesn't affect win condition, but earns gold for next level)")
}

private fun simulateLevel12(report: StringBuilder) {
    val title = "1-2: RIVER AMBUSH — enemy: 3× Warrior + 1× Archer"
    val enemy = buildEnemySpec(ENEMIES_12)

    // Player is still level 1, ~320-420g depending on 1-1 spend + labor
    // After this battle they hit level 2 and can unlock Archer (500g)
    val armies = listOf(
        build("3× Warrior",                 NormalUnitType.Melee to 3),                                    // 150g
        build("4× Warrior",                 NormalUnitType.Melee to 4),                                    // 200g
        build("5× Warrior",                 NormalUnitType.Melee to 5),                                    // 250g
        build("6× Warrior",                 NormalUnitType.Melee to 6),                                    // 300g
        build("7× Warrior",                 NormalUnitType.Melee to 7),                                    // 350g
        build("4× Warrior + 1× Farmer",     NormalUnitType.Melee to 4, SpecialUnitType.Farmer to 1),       // 300g
        build("5× Warrior + 1× Farmer",     NormalUnitType.Melee to 5, SpecialUnitType.Farmer to 1),       // 350g
    )
    runLevelSection(title, enemy, armies, goldBudget = 400, report = report, note =
        "Player gold: ~320-420g (after 1-1 + 1 labor day) | Still only Warrior + Farmer\n" +
        "After this victory: reach Level 2 → Archer becomes purchasable for 500g")
}

private fun simulateLevel13(report: StringBuilder) {
    val title = "1-3: HIGH FOREST — enemy: 3× Warrior + 1× Archer"
    val enemy = buildEnemySpec(ENEMIES_13)

    // Player is level 2, ~400-600g. Can unlock Archer for 500g if they have it.
    // NOTE: map has dense forest which blocks movement/arrows — flat grid simulation
    // will be MORE favorable to player (no terrain punishment). Mark this clearly.
    val armies = listOf(
        // Without Archer unlock (if player couldn't afford 500g yet)
        build("5× Warrior",                 NormalUnitType.Melee to 5),                                    // 250g
        build("6× Warrior",                 NormalUnitType.Melee to 6),                                    // 300g
        build("4× Warrior + 1× Farmer",     NormalUnitType.Melee to 4, SpecialUnitType.Farmer to 1),       // 300g
        // With Archer unlock (500g spent on unlock, fewer units to buy)
        build("2× Warrior + 2× Archer",     NormalUnitType.Melee to 2, NormalUnitType.Ranged to 2),        // 300g
        build("3× Warrior + 1× Archer",     NormalUnitType.Melee to 3, NormalUnitType.Ranged to 1),        // 250g
        build("4× Warrior + 1× Archer",     NormalUnitType.Melee to 4, NormalUnitType.Ranged to 1),        // 300g
        build("2× Warrior + 3× Archer",     NormalUnitType.Melee to 2, NormalUnitType.Ranged to 3),        // 400g
        build("3× Warrior + 1× Archer + 1× Farmer", NormalUnitType.Melee to 3, NormalUnitType.Ranged to 1, SpecialUnitType.Farmer to 1), // 350g
    )
    runLevelSection(title, enemy, armies, goldBudget = 600, report = report, note =
        "Player gold: ~400-600g | Level 2 | Archer available if 500g unlock was purchased\n" +
        "⚠ TERRAIN NOTE: Real map has dense forest (blocks move+arrows). Flat grid sim = EASIER than real.\n" +
        "Win rate here should be high (60%+) to account for terrain disadvantage in real play.")
}

private fun simulateLevel14(report: StringBuilder) {
    val title = "1-4: FOREST SIEGE — enemy: 2× Warrior + 2× Archer + 1× Cleric"
    val enemy = buildEnemySpec(ENEMIES_14)

    // Player is level 2, ~500-700g. Cleric won't be unlocked yet (needs level 3).
    val armies = listOf(
        build("5× Warrior",                 NormalUnitType.Melee to 5),                                    // 250g
        build("6× Warrior",                 NormalUnitType.Melee to 6),                                    // 300g
        build("4× Warrior + 2× Archer",     NormalUnitType.Melee to 4, NormalUnitType.Ranged to 2),        // 400g
        build("5× Warrior + 1× Archer",     NormalUnitType.Melee to 5, NormalUnitType.Ranged to 1),        // 350g
        build("3× Warrior + 3× Archer",     NormalUnitType.Melee to 3, NormalUnitType.Ranged to 3),        // 450g
        build("4× Warrior + 1× Archer + 1× Farmer", NormalUnitType.Melee to 4, NormalUnitType.Ranged to 1, SpecialUnitType.Farmer to 1), // 350g
        build("4× Warrior + 2× Archer + 1× Farmer", NormalUnitType.Melee to 4, NormalUnitType.Ranged to 2, SpecialUnitType.Farmer to 1), // 450g
        build("2× Warrior + 3× Archer + 1× Farmer", NormalUnitType.Melee to 2, NormalUnitType.Ranged to 3, SpecialUnitType.Farmer to 1), // 500g
    )
    runLevelSection(title, enemy, armies, goldBudget = 700, report = report, note =
        "Player gold: ~500-700g | Level 2 | Enemy has a Cleric (healer) — must kill it fast\n" +
        "After this victory: reach Level 3 → Cleric/Knight become purchasable\n" +
        "⚠ TERRAIN NOTE: Forest map, flat grid sim is more favorable to player")
}

private fun simulateLevel15(report: StringBuilder) {
    val title = "1-5: THE SLEEPING BEAR ★ BOSS — enemy: 1× Bear + 2× Warrior"
    val enemy = buildEnemySpec(ENEMIES_15)

    // Player is level 3 (or 2), ~600-900g. Cleric just became available (1000g unlock).
    // Knight available at level 5. Bear: HP=40, power=9 — this is the big threat.
    val armies = listOf(
        // No Cleric (can't afford 1000g unlock on top of army)
        build("6× Warrior",                 NormalUnitType.Melee to 6),                                    // 300g
        build("8× Warrior",                 NormalUnitType.Melee to 8),                                    // 400g
        build("4× Warrior + 2× Archer",     NormalUnitType.Melee to 4, NormalUnitType.Ranged to 2),        // 400g
        build("5× Warrior + 2× Archer",     NormalUnitType.Melee to 5, NormalUnitType.Ranged to 2),        // 450g
        build("4× Warrior + 3× Archer",     NormalUnitType.Melee to 4, NormalUnitType.Ranged to 3),        // 500g
        build("6× Warrior + 1× Archer",     NormalUnitType.Melee to 6, NormalUnitType.Ranged to 1),        // 400g
        build("4× Warrior + 2× Archer + 1× Farmer", NormalUnitType.Melee to 4, NormalUnitType.Ranged to 2, SpecialUnitType.Farmer to 1), // 500g
        build("5× Warrior + 2× Archer + 1× Farmer", NormalUnitType.Melee to 5, NormalUnitType.Ranged to 2, SpecialUnitType.Farmer to 1), // 550g
        // If player saved up for Cleric unlock (1000g) AND can buy army
        build("3× Warrior + 1× Archer + 1× Cleric", NormalUnitType.Melee to 3, NormalUnitType.Ranged to 1, NormalUnitType.Healer to 1), // 450g
        build("4× Warrior + 1× Archer + 1× Cleric", NormalUnitType.Melee to 4, NormalUnitType.Ranged to 1, NormalUnitType.Healer to 1), // 500g
        build("5× Warrior + 1× Cleric",     NormalUnitType.Melee to 5, NormalUnitType.Healer to 1),        // 450g
        build("6× Warrior + 1× Cleric",     NormalUnitType.Melee to 6, NormalUnitType.Healer to 1),        // 500g
    )
    runLevelSection(title, enemy, armies, goldBudget = 800, report = report, note =
        "Player gold: ~600-900g | Level 3 | Cleric available (if 1000g unlock saved up)\n" +
        "Bear: HP=40, power=9, size=2×2 — heavy hitter. Guard Warriors will die fast.\n" +
        "⚠ TERRAIN NOTE: Bear map has tree corridors — real Bear can be kited. Flat grid = harder for player.\n" +
        "Win rate target: 40-60% on flat grid (real map should be easier due to kiting).")
}

// ─── Simulation runner ────────────────────────────────────────────────────────

private fun runLevelSection(
    title: String,
    enemy: ArmySpec,
    playerArmies: List<ArmySpec>,
    goldBudget: Int,
    report: StringBuilder,
    note: String,
    runs: Int = 200
) {
    val line = "═".repeat(70)
    val header = "\n$line\n  LEVEL $title\n$line\n  $note\n"
    println(header)
    report.appendLine(header)

    val costLine = "  Enemy army total cost equivalent: ${enemy.totalCost}g"
    println(costLine); report.appendLine(costLine)
    println()

    val colHeader = String.format("  %-38s %6s %6s %8s %8s  %s",
        "Army Composition", "Cost", "Win%", "AvgTrn", "Verdict", "")
    val divider = "  " + "-".repeat(80)
    println(colHeader); println(divider)
    report.appendLine(colHeader); report.appendLine(divider)

    val results = mutableListOf<Triple<ArmySpec, BalancingSimulator.MatchupResult, String>>()

    for (army in playerArmies) {
        val cost = army.totalCost
        val affordable = cost <= goldBudget
        val result = BalancingSimulator.runMatchup(army, enemy, runs)
        val verdict = when {
            !affordable                    -> "OVER BUDGET"
            result.armyAWinRate >= 70.0    -> "✓ GOOD"
            result.armyAWinRate >= 50.0    -> "~ FAIR"
            result.armyAWinRate >= 30.0    -> "✗ HARD"
            else                           -> "✗✗ TOO HARD"
        }
        val row = String.format("  %-38s %5dg %5.0f%% %8.1f  %-12s%s",
            army.resolvedLabel.take(38), cost,
            result.armyAWinRate, result.avgTurns,
            verdict, if (!affordable) "  ← can't afford" else "")
        println(row); report.appendLine(row)
        results += Triple(army, result, verdict)
    }

    // Summary: best affordable army
    val best = results.filter { it.first.totalCost <= goldBudget }
        .maxByOrNull { it.second.armyAWinRate }
    val bestLine = "\n  Best affordable army: ${best?.first?.resolvedLabel ?: "none"} " +
        "(${String.format("%.0f", best?.second?.armyAWinRate ?: 0.0)}% win rate)"
    println(bestLine); report.appendLine(bestLine)

    // Per-unit kill breakdown for best army
    if (best != null) {
        val details = buildString {
            appendLine("  Unit performance (best army, $runs runs):")
            for (s in best.second.armyAStats) {
                appendLine(String.format("    %-22s  dmg/battle=%6.1f  kills/battle=%4.1f  survival=%3.0f%%",
                    s.type.displayName, s.avgDamagePerBattle, s.avgKillsPerBattle, s.survivalRate * 100.0))
            }
        }
        println(details); report.appendLine(details)
    }
    report.appendLine()
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun build(label: String, vararg units: Pair<UnitType, Int>): ArmySpec =
    ArmySpec(units.toList(), label)

private fun buildEnemySpec(entries: List<EnemyEntry>): ArmySpec =
    ArmySpec(entries.map { it.type to it.count })
