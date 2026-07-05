package com.autobattler.engine

import com.autobattler.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Writes detailed battle logs to disk for debugging.
 *
 * Each battle gets a numbered file in the `battle_logs/` folder:
 *   battle_logs/battle_1.log
 *   battle_logs/battle_2.log
 *   ...
 *
 * The log includes:
 * - Battle metadata (timestamp, map, units)
 * - Initial unit placements with unique IDs (e.g., ENEMY_WARRIOR_1)
 * - Every turn with each unit's decision, reasoning, movement, and actions
 * - Final outcome
 *
 * Usage: Call [BattleLogger.log] after simulation with the grid and outcome.
 */
object BattleLogger {

    private const val LOG_DIR = "battle_logs"
    private var battleCounter = 0

    /**
     * Generate a unique, readable tag for a unit.
     * Format: OWNER_TYPE_ID  (e.g., ENEMY_WARRIOR_1, PLAYER_ARCHER_3)
     */
    fun unitTag(unit: BattleUnit): String {
        val ownerTag = unit.owner.name
        val typeTag = unit.type.displayName.uppercase()
        return "${ownerTag}_${typeTag}_${unit.id}"
    }

    /**
     * Log a complete battle with full detail.
     *
     * @param grid The grid state BEFORE simulation (initial placements)
     * @param outcome The battle outcome (events + winner)
     * @param mapName Optional map name for context
     * @return The path to the written log file
     */
    fun log(grid: BattleGrid, outcome: BattleOutcome, mapName: String = "unknown"): String {
        battleCounter++
        val logDir = File(LOG_DIR)
        if (!logDir.exists()) logDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
        val fileName = "battle_${timestamp}.log"
        val file = File(logDir, fileName)

        val sb = StringBuilder()
        val displayTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())

        // ─── Header ─────────────────────────────────────────────────────────────
        sb.appendLine("╔══════════════════════════════════════════════════════════════╗")
        sb.appendLine("║  BATTLE LOG #$battleCounter")
        sb.appendLine("║  Timestamp: $displayTimestamp")
        sb.appendLine("║  Map: $mapName")
        sb.appendLine("║  Result: ${outcome.winner} in ${outcome.totalTurns} turns")
        sb.appendLine("╚══════════════════════════════════════════════════════════════╝")
        sb.appendLine()

        // ─── Initial State ──────────────────────────────────────────────────────
        sb.appendLine("┌─── INITIAL UNIT PLACEMENTS ───────────────────────────────────")
        sb.appendLine("│")

        val allUnits = grid.allUnits
        val playerUnits  = allUnits.filter { it.owner == BattleUnit.Owner.PLAYER }
        val enemyUnits   = allUnits.filter { it.owner == BattleUnit.Owner.ENEMY }
        val naturalUnits = allUnits.filter { it.owner == BattleUnit.Owner.NATURAL }

        sb.appendLine("│  PLAYER UNITS:")
        for (unit in playerUnits) {
            sb.appendLine("│    ${unitTag(unit)} at (${unit.position.x}, ${unit.position.y})")
            sb.appendLine("│      HP: ${unit.currentHp}/${unit.type.maxHp}  ATK: ${unit.type.power}  SPD: ${unit.type.speed}  CD: ${unit.type.cooldown}  Range: ${unit.type.attackRange}")
        }
        sb.appendLine("│")
        sb.appendLine("│  ENEMY UNITS:")
        for (unit in enemyUnits) {
            sb.appendLine("│    ${unitTag(unit)} at (${unit.position.x}, ${unit.position.y})")
            sb.appendLine("│      HP: ${unit.currentHp}/${unit.type.maxHp}  ATK: ${unit.type.power}  SPD: ${unit.type.speed}  CD: ${unit.type.cooldown}  Range: ${unit.type.attackRange}")
        }
        if (naturalUnits.isNotEmpty()) {
            sb.appendLine("│")
            sb.appendLine("│  NATURAL UNITS:")
            for (unit in naturalUnits) {
                sb.appendLine("│    ${unitTag(unit)} at (${unit.position.x}, ${unit.position.y})")
                sb.appendLine("│      HP: ${unit.currentHp}/${unit.type.maxHp}  ATK: ${unit.type.power}  SPD: ${unit.type.speed}  CD: ${unit.type.cooldown}  Range: ${unit.type.attackRange}")
            }
        }
        sb.appendLine("│")
        sb.appendLine("└───────────────────────────────────────────────────────────────")
        sb.appendLine()

        // ─── Map Layout ─────────────────────────────────────────────────────────
        sb.appendLine("┌─── MAP LAYOUT (16×16) ────────────────────────────────────────")
        sb.appendLine("│")

        // ASCII grid — y=15 at top (north), y=0 at bottom (south)
        // Legend appended after the grid
        val tileChar: (TileType) -> Char = {
            when (it) {
                TileType.GRASS         -> '.'
                TileType.ROCK          -> '#'
                TileType.WATER         -> '~'
                TileType.BRIDGE        -> '='
                TileType.TREE          -> 'T'
                TileType.SAND          -> 's'
                TileType.MUD           -> 'm'
                TileType.SHALLOW_WATER -> 'w'
                TileType.FORT          -> 'F'
                TileType.LAVA          -> 'L'
                TileType.ICE           -> 'i'
                TileType.RUINS         -> 'R'
            }
        }

        sb.appendLine("│     0 1 2 3 4 5 6 7 8 9 A B C D E F   ← x")
        sb.appendLine("│    ┌────────────────────────────────┐")
        for (y in grid.height - 1 downTo 0) {
            val rowLabel = if (y < 10) "$y " else "$y"
            sb.append("│  $rowLabel│")
            for (x in 0 until grid.width) {
                val tile = grid.getTile(x, y)
                sb.append(tileChar(tile))
                sb.append(' ')
            }
            sb.appendLine("│")
        }
        sb.appendLine("│    └────────────────────────────────┘")
        sb.appendLine("│    Legend: . grass  # rock  ~ water  = bridge  T tree")
        sb.appendLine("│            s sand   m mud   w shallow  F fort  L lava  i ice  R ruins")
        sb.appendLine("│")

        // Non-GRASS tile list (grouped by type, for easy parsing)
        val nonGrass = mutableMapOf<TileType, MutableList<Position>>()
        for (x in 0 until grid.width) {
            for (y in 0 until grid.height) {
                val t = grid.getTile(x, y)
                if (t != TileType.GRASS) nonGrass.getOrPut(t) { mutableListOf() }.add(Position(x, y))
            }
        }
        if (nonGrass.isEmpty()) {
            sb.appendLine("│  (all tiles are GRASS)")
        } else {
            sb.appendLine("│  NON-GRASS TILES:")
            for ((type, positions) in nonGrass.entries.sortedBy { it.key.name }) {
                val coords = positions.joinToString(" ") { "(${it.x},${it.y})" }
                sb.appendLine("│    ${type.name.padEnd(14)} $coords")
            }
        }
        sb.appendLine("│")
        sb.appendLine("└───────────────────────────────────────────────────────────────")
        sb.appendLine()


        sb.appendLine("┌─── BATTLE EVENTS ─────────────────────────────────────────────")
        sb.appendLine("│")

        // Build a unit lookup by ID for resolving names to tags
        val unitById = allUnits.associateBy { it.id }

        for (event in outcome.events) {
            when (event) {
                is BattleEvent.TurnStart -> {
                    sb.appendLine("│")
                    sb.appendLine("│  ════════ TURN ${event.turnNumber} ════════")
                    sb.appendLine("│")
                }
                is BattleEvent.Move -> {
                    val tag = unitById[event.unitId]?.let { unitTag(it) } ?: event.unitName
                    sb.appendLine("│  [MOVE] $tag: (${event.from.x},${event.from.y}) → (${event.to.x},${event.to.y})")
                }
                is BattleEvent.Attack -> {
                    val attackerTag = unitById[event.attackerId]?.let { unitTag(it) } ?: event.attackerName
                    val targetTag = unitById[event.targetId]?.let { unitTag(it) } ?: event.targetName
                    sb.appendLine("│  [ATTACK] $attackerTag → $targetTag | ${event.damage} damage | Target HP: ${event.targetHpAfter}")
                    sb.appendLine("│           Attacker at (${event.attackerPos.x},${event.attackerPos.y}), Target at (${event.targetPos.x},${event.targetPos.y})")
                }
                is BattleEvent.Heal -> {
                    val healerTag = unitById[event.healerId]?.let { unitTag(it) } ?: event.healerName
                    val targetTag = unitById[event.targetId]?.let { unitTag(it) } ?: event.targetName
                    sb.appendLine("│  [HEAL] $healerTag → $targetTag | +${event.amount} HP | Target HP: ${event.targetHpAfter}")
                }
                is BattleEvent.Death -> {
                    val tag = unitById[event.unitId]?.let { unitTag(it) } ?: event.unitName
                    sb.appendLine("│  [DEATH] ☠ $tag has been eliminated!")
                }
                is BattleEvent.FarmPlanted -> {
                    val tag = unitById[event.farmerId]?.let { unitTag(it) } ?: event.farmerName
                    sb.appendLine("│  [FARM_PLANTED] $tag planted Farm #${event.farmId} at (${event.position.x},${event.position.y})")
                }
                is BattleEvent.FarmGold -> {
                    sb.appendLine("│  [FARM_GOLD] Farm #${event.farmId} yielded +${event.amount}g")
                }
                is BattleEvent.Fireball -> {
                    val casterTag = unitById[event.casterId]?.let { unitTag(it) } ?: event.casterName
                    val blastDesc = "(${event.targetTile.x},${event.targetTile.y}) radius ${event.radius}"
                    sb.appendLine("│  [FIREBALL] $casterTag → tile $blastDesc | ${event.hits.size} unit(s) hit")
                    for (hit in event.hits) {
                        val hitTag = unitById[hit.unitId]?.let { unitTag(it) } ?: hit.unitName
                        sb.appendLine("│             ☄ $hitTag at (${hit.unitPos.x},${hit.unitPos.y}) — ${hit.damage} dmg | HP: ${hit.hpAfter}")
                    }
                }
                is BattleEvent.FireBreath -> {
                    val casterTag = unitById[event.casterId]?.let { unitTag(it) } ?: event.casterName
                    sb.appendLine("│  [FIRE_BREATH] $casterTag breathes fire ${event.direction} | ${event.hits.size} unit(s) hit")
                    for (hit in event.hits) {
                        val hitTag = unitById[hit.unitId]?.let { unitTag(it) } ?: hit.unitName
                        sb.appendLine("│               * $hitTag at (${hit.unitPos.x},${hit.unitPos.y}) — ${hit.damage} dmg | HP: ${hit.hpAfter}")
                    }
                }
                is BattleEvent.Cleave -> {
                    val atkTag = unitById[event.attackerId]?.let { unitTag(it) } ?: event.attackerName
                    val primTag = unitById[event.primaryId]?.let { unitTag(it) } ?: event.primaryName
                    sb.appendLine("│  [CLEAVE] $atkTag cleaves $primTag for ${event.primaryDmg} dmg (HP: ${event.primaryHpAfter})")
                    for (s in event.splashHits) {
                        val sTag = unitById[s.unitId]?.let { unitTag(it) } ?: s.unitName
                        sb.appendLine("│           splash > $sTag at (${s.unitPos.x},${s.unitPos.y}) — ${s.damage} dmg | HP: ${s.hpAfter}")
                    }
                }
            }
        }

        sb.appendLine("│")
        sb.appendLine("└───────────────────────────────────────────────────────────────")
        sb.appendLine()

        // ─── Summary ────────────────────────────────────────────────────────────
        sb.appendLine("┌─── BATTLE SUMMARY ────────────────────────────────────────────")
        sb.appendLine("│")
        sb.appendLine("│  Winner: ${outcome.winner}")
        sb.appendLine("│  Total Turns: ${outcome.totalTurns}")
        sb.appendLine("│  Total Events: ${outcome.events.size}")

        val kills = outcome.events.count { it is BattleEvent.Death }
        val attacks = outcome.events.count { it is BattleEvent.Attack } +
                      outcome.events.count { it is BattleEvent.Fireball } +
                      outcome.events.count { it is BattleEvent.FireBreath } +
                      outcome.events.count { it is BattleEvent.Cleave }
        val heals = outcome.events.count { it is BattleEvent.Heal }
        val moves = outcome.events.count { it is BattleEvent.Move }

        sb.appendLine("│  Attacks: $attacks | Heals: $heals | Moves: $moves | Deaths: $kills")
        sb.appendLine("│")
        sb.appendLine("└───────────────────────────────────────────────────────────────")

        file.writeText(sb.toString())
        return file.absolutePath
    }

    /** Reset the battle counter (useful for testing). */
    fun resetCounter() {
        battleCounter = 0
    }
}
