package com.autobattler.desktop

import com.autobattler.engine.BalancingSimulator
import com.autobattler.engine.BalancingSimulator.ArmySpec
import com.autobattler.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Standalone balance-testing tool — runs headlessly (no window, no GPU).
 *
 * Run via Gradle:
 *   ./gradlew :desktop:runBalanceTool
 *
 * Or pass arguments for a quick single matchup:
 *   ./gradlew :desktop:runBalanceTool --args="melee:3 vs wolf:5 --runs=200"
 *
 * Output is written to `balance_reports/<timestamp>.txt` and also printed to console.
 */
fun main(args: Array<String>) {
    println("╔══════════════════════════════════════════════════════════════════╗")
    println("║             AUTOBATTLER — BALANCE SIMULATION TOOL               ║")
    println("╚══════════════════════════════════════════════════════════════════╝")
    println()

    val report = StringBuilder()
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
    report.appendLine("AUTOBATTLER BALANCE REPORT — Generated $timestamp")
    report.appendLine("=".repeat(70))
    report.appendLine()

    // ── Parse optional CLI args: "typeId:count vs typeId:count --runs=N" ────
    val cliMatchup = parseCliMatchup(args)
    if (cliMatchup != null) {
        val (armyA, armyB, runs) = cliMatchup
        println("Running CLI matchup: ${armyA.resolvedLabel}  vs  ${armyB.resolvedLabel}  ($runs runs)")
        val result = BalancingSimulator.runMatchup(armyA, armyB, runs)
        val summary = result.summary()
        println(summary)
        report.appendLine(summary)
    } else {
        // ── Full predefined suite ────────────────────────────────────────────
        runFullSuite(report)
    }

    // ── Write report file ────────────────────────────────────────────────────
    val dir = File("balance_reports")
    if (!dir.exists()) dir.mkdirs()
    val outFile = File(dir, "balance_$timestamp.txt")
    outFile.writeText(report.toString())
    println()
    println("Report saved to: ${outFile.absolutePath}")
}

// ─── Full predefined test suite ──────────────────────────────────────────────

private fun runFullSuite(report: StringBuilder) {

    // ── Section 1: 1v1 unit comparison (same cost bracket) ──────────────────
    section("SECTION 1 — BRONZE TIER: 1v1 matchups", report)

    val bronzeMatchups = listOf(
        army(NormalUnitType.Melee)  vs army(NormalUnitType.Melee),
        army(NormalUnitType.Melee)  vs army(NormalUnitType.Ranged),
        army(NormalUnitType.Melee)  vs army(NormalUnitType.Healer),
        army(NormalUnitType.Ranged) vs army(NormalUnitType.Healer),
        army(NormalUnitType.Melee)  vs army(SpecialUnitType.Wizard),
        army(NormalUnitType.Ranged) vs army(SpecialUnitType.Wizard),
    )
    runMatchups(bronzeMatchups, runs = 100, report = report)

    // ── Section 2: Player armies vs common enemy groups ──────────────────────
    section("SECTION 2 — PLAYER ARMIES vs ENEMY GROUPS", report)

    val armyMatchups = listOf(
        ArmySpec(listOf(NormalUnitType.Melee to 3), "3× Warrior")
            vs ArmySpec(listOf(MonsterUnitType.Wolf to 3), "3× Wolf"),

        ArmySpec(listOf(NormalUnitType.Melee to 2, NormalUnitType.Ranged to 1), "2× Warrior + 1× Archer")
            vs ArmySpec(listOf(MonsterUnitType.Goblin to 4), "4× Goblin"),

        ArmySpec(listOf(NormalUnitType.Ranged to 2, NormalUnitType.Healer to 1), "2× Archer + 1× Cleric")
            vs ArmySpec(listOf(MonsterUnitType.Skeleton to 3), "3× Skeleton"),

        ArmySpec(listOf(NormalUnitType.Knight to 1, NormalUnitType.Ranged to 2), "Knight + 2× Archer")
            vs ArmySpec(listOf(MonsterUnitType.Troll to 1, MonsterUnitType.Goblin to 3), "Troll + 3× Goblin"),

        ArmySpec(listOf(SpecialUnitType.Wizard to 1, NormalUnitType.Melee to 2), "Wizard + 2× Warrior")
            vs ArmySpec(listOf(MonsterUnitType.OrcWarrior to 3), "3× Orc Warrior"),

        ArmySpec(listOf(NormalUnitType.GriffinRider to 1, NormalUnitType.Ranged to 2), "Griffin Rider + 2× Archer")
            vs ArmySpec(listOf(MonsterUnitType.Dragon to 1), "Dragon"),
    )
    runMatchups(armyMatchups, runs = 100, report = report)

    // ── Section 3: Tier comparison (Bronze vs Silver vs Gold) ────────────────
    section("SECTION 3 — TIER PROGRESSION: Bronze vs Silver vs Gold", report)

    val tierMatchups = listOf(
        army(NormalUnitType.Melee)       vs army(NormalUnitType.MeleeSilver),
        army(NormalUnitType.MeleeSilver) vs army(NormalUnitType.MeleeGold),
        army(NormalUnitType.Melee)       vs army(NormalUnitType.MeleeGold),
        army(NormalUnitType.Ranged)       vs army(NormalUnitType.RangedSilver),
        army(NormalUnitType.RangedSilver) vs army(NormalUnitType.RangedGold),
        army(NormalUnitType.Knight)       vs army(NormalUnitType.KnightSilver),
        army(NormalUnitType.KnightSilver) vs army(NormalUnitType.KnightGold),
        army(SpecialUnitType.Wizard)       vs army(SpecialUnitType.WizardSilver),
        army(SpecialUnitType.WizardSilver) vs army(SpecialUnitType.WizardGold),
    )
    runMatchups(tierMatchups, runs = 100, report = report)

    // ── Section 4: Cost-equalised matchups (spend same gold) ─────────────────
    section("SECTION 4 — COST-EQUALISED MATCHUPS (approx. same gold)", report)

    val costMatchups = listOf(
        // ~300 gold each side
        ArmySpec(listOf(NormalUnitType.Melee to 3), "3× Warrior (150g)")      // 50*3=150 — use 2 knights ~200
            vs ArmySpec(listOf(NormalUnitType.Ranged to 3), "3× Archer (300g)"),

        ArmySpec(listOf(NormalUnitType.Healer to 1, NormalUnitType.Melee to 2), "Cleric + 2× Warrior (300g)")
            vs ArmySpec(listOf(NormalUnitType.Knight to 1, NormalUnitType.Melee to 1), "Knight + Warrior (250g)"),

        ArmySpec(listOf(SpecialUnitType.Wizard to 1), "1× Wizard (300g)")
            vs ArmySpec(listOf(NormalUnitType.Ranged to 3), "3× Archer (300g)"),

        ArmySpec(listOf(NormalUnitType.Knight to 1), "1× Knight (200g)")
            vs ArmySpec(listOf(NormalUnitType.Melee to 4), "4× Warrior (200g)"),
    )
    runMatchups(costMatchups, runs = 100, report = report)

    // ── Section 5: Monster threat levels ─────────────────────────────────────
    section("SECTION 5 — MONSTER THREAT ASSESSMENT vs 3× Warrior", report)

    val baseline = ArmySpec(listOf(NormalUnitType.Melee to 3), "3× Warrior")
    val allMonsters = listOf(
        MonsterUnitType.Bear, MonsterUnitType.Wolf, MonsterUnitType.Goblin,
        MonsterUnitType.Skeleton, MonsterUnitType.DarkMage, MonsterUnitType.Troll,
        MonsterUnitType.Bandit, MonsterUnitType.OrcWarrior, MonsterUnitType.Spider,
        MonsterUnitType.Dragon
    )
    val monsterMatchups = allMonsters
        .map { monster -> baseline vs ArmySpec(listOf(monster to 1), "1× ${monster.displayName}") }

    runMatchups(monsterMatchups, runs = 100, report = report)

    // ── Section 6: Full 1v1 matrix (all combat types) ────────────────────────
    section("SECTION 6 — FULL 1v1 WIN-RATE MATRIX (50 runs each)", report)

    val matrixTypes = listOf(
        NormalUnitType.Melee, NormalUnitType.MeleeSilver, NormalUnitType.MeleeGold,
        NormalUnitType.Ranged, NormalUnitType.RangedSilver, NormalUnitType.RangedGold,
        NormalUnitType.Healer, NormalUnitType.Knight, NormalUnitType.KnightSilver,
        NormalUnitType.GriffinRider,
        SpecialUnitType.Wizard, SpecialUnitType.WizardSilver,
        MonsterUnitType.Bear, MonsterUnitType.Wolf, MonsterUnitType.Goblin,
        MonsterUnitType.Skeleton, MonsterUnitType.DarkMage, MonsterUnitType.Troll,
        MonsterUnitType.OrcWarrior, MonsterUnitType.Spider, MonsterUnitType.Dragon,
    )
    val matrix = BalancingSimulator.runMatrix(matrixTypes, matrixTypes, runs = 50)
    val matrixText = formatMatrix(matrix, matrixTypes)
    println(matrixText)
    report.appendLine(matrixText)
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun section(title: String, report: StringBuilder) {
    val line = "═".repeat(70)
    val block = "\n$line\n  $title\n$line\n"
    println(block)
    report.appendLine(block)
}

private fun runMatchups(
    pairs: List<Pair<ArmySpec, ArmySpec>>,
    runs: Int,
    report: StringBuilder
) {
    for ((a, b) in pairs) {
        print("  Simulating: ${a.resolvedLabel.padEnd(35)} vs  ${b.resolvedLabel.padEnd(35)}")
        System.out.flush()
        val result = BalancingSimulator.runMatchup(a, b, runs)
        println("  → ${String.format("%.0f", result.armyAWinRate)}% / ${String.format("%.0f", result.armyBWinRate)}%")
        report.appendLine(result.summary())
        report.appendLine()
    }
}

/**
 * Render a compact ASCII win-rate matrix.
 *
 * Rows = army A (player), Columns = army B (enemy).
 * Cell value = A win-rate (0–100).
 */
private fun formatMatrix(results: List<BalancingSimulator.MatchupResult>, types: List<UnitType>): String {
    val resultMap = results.associate { (it.armyALabel to it.armyBLabel) to it.armyAWinRate }
    val names = types.map { it.displayName }
    val colW = 6  // width per cell

    return buildString {
        appendLine("  Win-rate matrix — rows=A(player), cols=B(enemy), value=A wins %")
        appendLine()

        // Header row
        val nameW = 22
        append(" ".repeat(nameW + 2))
        for (n in names) append(n.take(colW - 1).padStart(colW))
        appendLine()
        append(" ".repeat(nameW + 2))
        append("-".repeat(names.size * colW))
        appendLine()

        for (rowType in types) {
            append(rowType.displayName.take(nameW).padEnd(nameW))
            append(" |")
            for (colType in types) {
                if (rowType == colType) {
                    append("  --- ")
                } else {
                    val wr = resultMap[rowType.displayName to colType.displayName]
                    if (wr == null) {
                        append("   ?  ")
                    } else {
                        append(String.format("%5.0f%%", wr))
                        append(" ")
                    }
                }
            }
            appendLine()
        }
    }
}

// ─── CLI argument parser ─────────────────────────────────────────────────────

/**
 * Parse optional CLI arguments in the form:
 *   `typeId:count[+typeId:count…] vs typeId:count[+typeId:count…] [--runs=N]`
 *
 * Examples:
 *   melee:3 vs wolf:5
 *   melee:2+ranged:1 vs goblin:4 --runs=200
 *
 * Returns null if args are empty or cannot be parsed (falls back to full suite).
 */
private data class CliMatchup(val armyA: ArmySpec, val armyB: ArmySpec, val runs: Int)

private fun parseCliMatchup(args: Array<String>): CliMatchup? {
    if (args.isEmpty()) return null
    val joined = args.joinToString(" ").trim()

    // Extract --runs=N
    val runsRegex = Regex("""--runs=(\d+)""")
    val runsMatch = runsRegex.find(joined)
    val runs = runsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 100
    val withoutRuns = joined.replace(runsRegex, "").trim()

    // Split on " vs "
    val parts = withoutRuns.split(Regex("""\s+vs\s+""", RegexOption.IGNORE_CASE))
    if (parts.size != 2) {
        println("Usage: typeId:count[+typeId:count…] vs typeId:count[+typeId:count…] [--runs=N]")
        println("Example: melee:3 vs wolf:5 --runs=200")
        println("Falling back to full balance suite.")
        return null
    }

    val armyA = parseArmySpec(parts[0].trim()) ?: return null
    val armyB = parseArmySpec(parts[1].trim()) ?: return null
    return CliMatchup(armyA, armyB, runs)
}

private fun parseArmySpec(spec: String): ArmySpec? {
    val unitPairs = spec.split("+").mapNotNull { token ->
        val parts = token.trim().split(":")
        val typeId = parts[0].trim().lowercase()
        val count  = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1
        val type   = UnitType.idToUnitTypeMap[typeId]
        if (type == null) {
            println("Unknown unit type: '$typeId'. Valid IDs: ${UnitType.idToUnitTypeMap.keys.sorted().joinToString()}")
            null
        } else {
            type to count
        }
    }
    if (unitPairs.isEmpty()) return null
    return ArmySpec(unitPairs)
}

// ─── Army builder DSL shortcuts ──────────────────────────────────────────────

private fun army(type: UnitType): ArmySpec = ArmySpec(listOf(type to 1))

private infix fun ArmySpec.vs(other: ArmySpec): Pair<ArmySpec, ArmySpec> = this to other
