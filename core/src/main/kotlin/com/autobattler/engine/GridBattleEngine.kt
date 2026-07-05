package com.autobattler.engine

import com.autobattler.model.*

/**
 * Grid-based battle simulation engine.
 *
 * Runs turn-by-turn simulation where each unit:
 * 1. Ticks cooldown
 * 2. Moves toward a target (up to [UnitType.speed] tiles, respecting tile passability)
 * 3. Performs action if in range and has line-of-sight (for ranged)
 *
 * The simulation is deterministic for a given unit placement.
 *
 * Additional rules for Farmer/Farm:
 * - Farmer uses PlantFarm ability instead of attacking.
 * - Farms generate gold each turn and are targeted last by enemies.
 * - Structures (Farm) do NOT count toward the player-alive win condition.
 */
class GridBattleEngine {

    companion object {
        private const val MAX_TURNS = 50
        private const val WOLF_AGGRO_RANGE = 6
    }

    /** Counter for IDs assigned to dynamically spawned units (e.g. Farms). */
    private var nextDynamicId = 1000

    fun simulate(grid: BattleGrid): BattleOutcome {
        val simUnits = grid.allUnits.map { it.copy() }.toMutableList()
        val events = mutableListOf<BattleEvent>()
        var turn = 0
        var totalFarmGold = 0
        nextDynamicId = (simUnits.maxOfOrNull { it.id } ?: 0) + 100

        while (turn < MAX_TURNS) {
            turn++
            events.add(BattleEvent.TurnStart(turn))

            val operationalFarms = simUnits.filter {
                it.isAlive && it.type is StructureUnitType.Farm && it.owner == BattleUnit.Owner.PLAYER
            }
            for (farm in operationalFarms) {
                val goldPerRound = ((farm.type as StructureUnitType).ability as SpecialAbility.YieldGold).goldPerRound
                events.add(BattleEvent.FarmGold(farm.id, farm.position, goldPerRound))
                totalFarmGold += goldPerRound
            }

            val sortedUnits = simUnits.filter { it.isAlive }.sortedByDescending { it.type.speed }

            for (unit in sortedUnits) {
                if (!unit.isAlive) continue
                unit.tickCooldown()

                when (unit.type) {
                    is NormalUnitType.Melee   -> processMelee(unit, simUnits, grid, events)
                    is NormalUnitType.Ranged  -> processRanged(unit, simUnits, grid, events)
                    is NormalUnitType.Healer  -> processHealer(unit, simUnits, grid, events)
                    is MonsterUnitType.Bear   -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.Wolf   -> processWolf(unit, simUnits, grid, events)
                    is SpecialUnitType.Farmer -> processFarmer(unit, simUnits, grid, events)
                    is StructureUnitType.Farm -> { /* passive structure — no action */ }
                }
            }

            // ── Win condition: structures don't count as "alive"; NATURAL units are neutral ─
            val playersAlive = simUnits.any {
                it.isAlive && it.owner == BattleUnit.Owner.PLAYER && !it.type.isStructure
            }
            val enemiesAlive = simUnits.any { it.isAlive && it.owner == BattleUnit.Owner.ENEMY }

            if (!playersAlive) return BattleOutcome(BattleOutcome.Winner.ENEMY,  events, turn, totalFarmGold)
            if (!enemiesAlive) return BattleOutcome(BattleOutcome.Winner.PLAYER, events, turn, totalFarmGold)
        }

        return BattleOutcome(BattleOutcome.Winner.DRAW, events, turn, totalFarmGold)
    }

    // ─── MELEE (also used by Bear) ─────────────────────────────────────────────

    private fun processMelee(unit: BattleUnit, allUnits: List<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {
        val target = getPriorityTarget(unit, allUnits) ?: return
        moveToward(unit, target.position, allUnits, grid, events)

        val distToTarget = unit.distanceToPosition(target.position)
        if (unit.canAct && distToTarget in unit.type.attackRange) {
            performAttack(unit, target, events)
        }
    }

    // ─── WOLF ────────────────────────────────────────────────────────────────────

    /**
     * Wolf is a neutral ([BattleUnit.Owner.NATURAL]) predator.
     * It stays completely idle until any non-NATURAL unit enters within
     * [WOLF_AGGRO_RANGE] tiles.  Once aggroed it pursues and attacks the
     * closest intruder.
     */
    private fun processWolf(unit: BattleUnit, allUnits: List<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {
        val intruders = allUnits.filter { other ->
            other.isAlive &&
            other.owner != BattleUnit.Owner.NATURAL &&
            unit.distanceToPosition(other.position) <= WOLF_AGGRO_RANGE
        }
        if (intruders.isEmpty()) return   // no one nearby — stay idle

        val target = intruders.minByOrNull { unit.distanceToPosition(it.position) } ?: return
        moveToward(unit, target.position, allUnits, grid, events)

        val distToTarget = unit.distanceToPosition(target.position)
        if (unit.canAct && distToTarget in unit.type.attackRange) {
            performAttack(unit, target, events)
        }
    }

    // ─── RANGED ─────────────────────────────────────────────────────────────────

    private fun processRanged(unit: BattleUnit, allUnits: List<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {
        val enemies = getEnemiesOf(unit, allUnits)
        if (enemies.isEmpty()) return

        // Movement decisions are driven by combat (non-structure) targets only.
        // Structures are last resort — don't let them trick the archer into stopping.
        val combatEnemies = enemies.filter { !it.type.isStructure }
        val closestCombat  = combatEnemies.minByOrNull { unit.position.distanceTo(it.position) }
        val closestEnemy   = closestCombat ?: enemies.minByOrNull { unit.position.distanceTo(it.position) } ?: return
        val distance       = unit.position.distanceTo(closestEnemy.position)

        // A real (non-structure) target in range with clear LOS
        val combatTargetInRange = combatEnemies.find {
            unit.position.distanceTo(it.position) in unit.type.attackRange
                && grid.hasLineOfSight(unit.position, it.position)
        }

        when {
            distance < unit.type.attackRange.first -> moveAway(unit, closestEnemy.position, allUnits, grid, events)
            combatTargetInRange != null             -> { /* stay — real target is in range */ }
            else                                    -> moveToward(unit, closestEnemy.position, allUnits, grid, events)
        }

        // After optional move: shoot best available target (structures as last resort)
        val shootTarget = enemies.find {
            unit.position.distanceTo(it.position) in unit.type.attackRange
                && grid.hasLineOfSight(unit.position, it.position)
        }
        if (unit.canAct && shootTarget != null) {
            performAttack(unit, shootTarget, events)
        }
    }

    // ─── HEALER ─────────────────────────────────────────────────────────────────

    private fun processHealer(unit: BattleUnit, allUnits: List<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {
        val allies = getAlliesOf(unit, allUnits).filter { it.id != unit.id && it.currentHp < it.type.maxHp }

        if (allies.isEmpty()) {
            if (unit.canAct && unit.currentHp < unit.type.maxHp) { performSelfHeal(unit, events); return }
            val anyAlly = getAlliesOf(unit, allUnits).filter { it.id != unit.id }.minByOrNull {
                unit.position.distanceTo(it.position)
            }
            if (anyAlly != null) moveToward(unit, anyAlly.position, allUnits, grid, events)
            return
        }

        val target = allies.minByOrNull { unit.position.distanceTo(it.position) } ?: return
        moveToward(unit, target.position, allUnits, grid, events)

        if (unit.canAct && unit.position.isAdjacentTo(target.position)) {
            performHeal(unit, target, events)
        } else if (unit.canAct && unit.currentHp < unit.type.maxHp) {
            performSelfHeal(unit, events)
        }
    }

    // ─── FARMER ─────────────────────────────────────────────────────────────────

    private fun processFarmer(unit: BattleUnit, allUnits: MutableList<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {
        val combatEnemies = getEnemiesOf(unit, allUnits).filter { !it.type.isStructure }
        val nearestEnemy  = combatEnemies.minByOrNull { unit.position.distanceTo(it.position) }

        // ── Plant FIRST — never waste a ready cooldown ───────────────────────────
        if (unit.canAct) {
            val candidates = getCardinalNeighbors(unit.position).filter { pos ->
                grid.isInBounds(pos) && grid.isPassable(pos) &&
                allUnits.none { it.isAlive && it.occupies(pos) }
            }
            val plantPos = if (nearestEnemy != null)
                candidates.maxByOrNull { it.distanceTo(nearestEnemy.position) }
            else
                candidates.firstOrNull()

            if (plantPos != null) {
                val farmId = nextDynamicId++
                allUnits.add(BattleUnit(id = farmId, type = StructureUnitType.Farm, owner = BattleUnit.Owner.PLAYER, position = plantPos))
                unit.startCooldown()
                events.add(BattleEvent.FarmPlanted(unit.id, unit.displayName, plantPos, farmId))
            }
        }

        // ── Then flee from the nearest enemy ────────────────────────────────────
        if (nearestEnemy != null) {
            moveAway(unit, nearestEnemy.position, allUnits, grid, events)
        }
    }

    // ─── Movement ────────────────────────────────────────────────────────────────

    private fun moveToward(unit: BattleUnit, target: Position, allUnits: List<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {
        if (unit.isAdjacentToPosition(target)) return
        val path = findPathToAdjacent(unit, target, allUnits, grid)
        if (path.isEmpty()) return
        for (step in path.take(unit.type.speed)) {
            if (unit.isAdjacentToPosition(target)) break
            val from = unit.position
            unit.position = step
            events.add(BattleEvent.Move(unit.id, unit.displayName, from, step))
        }
    }

    private fun moveAway(unit: BattleUnit, threat: Position, allUnits: List<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {
        var stepsRemaining = unit.type.speed
        while (stepsRemaining > 0) {
            val nextStep = bestStepAway(unit, threat, allUnits, grid)
            if (nextStep == null || nextStep == unit.position) break
            val from = unit.position
            unit.position = nextStep
            events.add(BattleEvent.Move(unit.id, unit.displayName, from, nextStep))
            stepsRemaining--
        }
    }

    private fun findPathToAdjacent(movingUnit: BattleUnit, goal: Position, allUnits: List<BattleUnit>, grid: BattleGrid): List<Position> {
        val start = movingUnit.position
        val unitSize = movingUnit.size
        val occupiedByOthers = mutableSetOf<Position>()
        for (other in allUnits) {
            if (!other.isAlive || other.id == movingUnit.id) continue
            if (other.type.isStructure) continue  // structures are passable — units walk through them
            occupiedByOthers.addAll(other.occupiedTiles())
        }

        fun canFitAt(pos: Position): Boolean {
            for (dx in 0 until unitSize) for (dy in 0 until unitSize) {
                val tile = Position(pos.x + dx, pos.y + dy)
                if (!grid.isInBounds(tile) || !grid.isPassable(tile) || tile in occupiedByOthers) return false
            }
            return true
        }

        fun isAdjacentFromPos(pos: Position): Boolean {
            for (dx in 0 until unitSize) for (dy in 0 until unitSize) {
                if (Position(pos.x + dx, pos.y + dy).isAdjacentTo(goal)) return true
            }
            return false
        }

        if (isAdjacentFromPos(start)) return emptyList()

        val visited = mutableSetOf(start)
        val queue = ArrayDeque<Position>()
        val cameFrom = mutableMapOf<Position, Position>()
        queue.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (isAdjacentFromPos(current)) return reconstructPath(cameFrom, start, current)
            for (neighbor in getCardinalNeighbors(current)) {
                if (neighbor in visited || !canFitAt(neighbor)) continue
                visited.add(neighbor)
                cameFrom[neighbor] = current
                queue.add(neighbor)
            }
        }
        return emptyList()
    }

    private fun reconstructPath(cameFrom: Map<Position, Position>, start: Position, end: Position): List<Position> {
        val path = mutableListOf<Position>()
        var current = end
        while (current != start) { path.add(current); current = cameFrom[current] ?: break }
        path.reverse()
        return path
    }

    private fun bestStepAway(unit: BattleUnit, threat: Position, allUnits: List<BattleUnit>, grid: BattleGrid): Position? {
        val occupiedByOthers = mutableSetOf<Position>()
        for (other in allUnits) {
            if (!other.isAlive || other.id == unit.id) continue
            if (other.type.isStructure) continue  // structures are passable — can flee through them
            occupiedByOthers.addAll(other.occupiedTiles())
        }
        return getCardinalNeighbors(unit.position)
            .filter { pos ->
                (0 until unit.size).all { dx -> (0 until unit.size).all { dy ->
                    val tile = Position(pos.x + dx, pos.y + dy)
                    grid.isInBounds(tile) && grid.isPassable(tile) && tile !in occupiedByOthers
                }}
            }
            .maxByOrNull { it.manhattanDistance(threat) }
    }

    private fun getCardinalNeighbors(pos: Position): List<Position> =
        listOf(Position(pos.x, pos.y+1), Position(pos.x, pos.y-1), Position(pos.x+1, pos.y), Position(pos.x-1, pos.y))
            .filter { it.x in 0 until BattleGrid.GRID_SIZE && it.y in 0 until BattleGrid.GRID_SIZE }

    // ─── Combat ─────────────────────────────────────────────────────────────────

    private fun performAttack(attacker: BattleUnit, target: BattleUnit, events: MutableList<BattleEvent>) {
        val damage = attacker.type.power
        target.takeDamage(damage)
        attacker.startCooldown()
        events.add(BattleEvent.Attack(attacker.id, attacker.displayName, attacker.position, target.id, target.displayName, target.position, damage, target.currentHp, attacker.type))
        if (!target.isAlive) events.add(BattleEvent.Death(target.id, target.displayName))
    }

    private fun performHeal(healer: BattleUnit, target: BattleUnit, events: MutableList<BattleEvent>) {
        val healAmount = healer.type.power
        target.heal(healAmount)
        healer.startCooldown()
        events.add(BattleEvent.Heal(healer.id, healer.displayName, target.id, target.displayName, target.position, healAmount, target.currentHp))
    }

    private fun performSelfHeal(healer: BattleUnit, events: MutableList<BattleEvent>) {
        val healAmount = healer.type.power / 2
        if (healAmount <= 0) return
        healer.heal(healAmount)
        healer.startCooldown()
        events.add(BattleEvent.Heal(healer.id, healer.displayName, healer.id, healer.displayName, healer.position, healAmount, healer.currentHp))
    }

    // ─── Utility ────────────────────────────────────────────────────────────────

    /**
     * Returns enemies of [unit], with non-structure units first.
     * Structures (Farm) are last-resort targets.
     * NATURAL units are never in the enemy list of PLAYER/ENEMY — they are neutral.
     * A NATURAL unit sees all non-NATURAL units as potential targets (handled by processWolf).
     */
    private fun getEnemiesOf(unit: BattleUnit, allUnits: List<BattleUnit>): List<BattleUnit> {
        val enemies = when (unit.owner) {
            BattleUnit.Owner.NATURAL ->
                allUnits.filter { it.isAlive && it.owner != BattleUnit.Owner.NATURAL }
            else ->
                allUnits.filter { it.isAlive && it.owner != unit.owner && it.owner != BattleUnit.Owner.NATURAL }
        }
        return enemies.sortedBy { if (it.type.isStructure) 1 else 0 }
    }

    /**
     * Returns the highest-priority target for [unit]:
     * - Non-structure enemies first, by proximity
     * - Falls back to structures only if no other targets remain
     * - NATURAL units are excluded from PLAYER/ENEMY targeting
     */
    private fun getPriorityTarget(unit: BattleUnit, allUnits: List<BattleUnit>): BattleUnit? {
        val enemies = when (unit.owner) {
            BattleUnit.Owner.NATURAL ->
                allUnits.filter { it.isAlive && it.owner != BattleUnit.Owner.NATURAL }
            else ->
                allUnits.filter { it.isAlive && it.owner != unit.owner && it.owner != BattleUnit.Owner.NATURAL }
        }
        val combatTargets = enemies.filter { !it.type.isStructure }
        return if (combatTargets.isNotEmpty())
            combatTargets.minByOrNull { unit.distanceToPosition(it.position) }
        else
            enemies.minByOrNull { unit.distanceToPosition(it.position) }
    }

    private fun getAlliesOf(unit: BattleUnit, allUnits: List<BattleUnit>): List<BattleUnit> =
        allUnits.filter { it.isAlive && it.owner == unit.owner }
}
