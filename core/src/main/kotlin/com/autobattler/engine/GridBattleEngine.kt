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
                    is NormalUnitType.Melee    -> processMelee(unit, simUnits, grid, events)
                    is NormalUnitType.Ranged   -> processRanged(unit, simUnits, grid, events)
                    is NormalUnitType.Healer   -> processHealer(unit, simUnits, grid, events)
                    is MonsterUnitType.Bear    -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.Wolf    -> processWolf(unit, simUnits, grid, events)
                    // New monster types — route to appropriate behaviour
                    is MonsterUnitType.Goblin      -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.Skeleton    -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.DarkMage    -> processRanged(unit, simUnits, grid, events)
                    is MonsterUnitType.Troll       -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.Bandit      -> processRanged(unit, simUnits, grid, events)
                    is MonsterUnitType.OrcWarrior  -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.Spider      -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.Dragon      -> processMelee(unit, simUnits, grid, events)
                    is SpecialUnitType.Farmer  -> processFarmer(unit, simUnits, grid, events)
                    is StructureUnitType.Farm  -> { /* passive structure — no action */ }
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
        // Pick the most REACHABLE enemy rather than just the closest by grid distance.
        // This lets flanking units go around obstacles to attack from the rear.
        val target = pickReachableTarget(unit, allUnits, grid) ?: return

        // If not yet adjacent, navigate to the best unoccupied flank position so
        // allies spread out rather than all queuing onto the same tile.
        if (!unit.isAdjacentToPosition(target.position)) {
            val flankPos = pickFlankPosition(unit, target, allUnits, grid)
            if (flankPos != null) {
                // Move onto the chosen flank tile (exact destination, not adjacent-to it).
                moveOnto(unit, flankPos, allUnits, grid, events)
            } else {
                // No flank tile available (all occupied / unit boxed in) — fall back.
                moveToward(unit, target.position, allUnits, grid, events)
            }
        }

        val distToTarget = unit.distanceToPosition(target.position)
        if (unit.canAct && distToTarget in unit.type.attackRange) {
            performAttack(unit, target, events)
        }
    }

    /**
     * Picks the best enemy target for a melee unit, considering actual path reachability.
     *
     * Scoring per enemy (lower = better):
     * 1. Ghost-path distance to the enemy's best flank position (terrain + enemy obstacles;
     *    allied units treated as transparent so we measure true terrain effort).
     * 2. +500 if the ghost path's FIRST STEP is blocked by an ally — this means the unit
     *    would immediately stall in a crowd. Units detour around to a more reachable enemy.
     *
     * This naturally routes flanking units (e.g. a warrior on y=8 blocked by rocks) to go
     * the long way around and attack from behind, rather than waiting indefinitely.
     */
    private fun pickReachableTarget(unit: BattleUnit, allUnits: List<BattleUnit>, grid: BattleGrid): BattleUnit? {
        val enemies = when (unit.owner) {
            BattleUnit.Owner.NATURAL ->
                allUnits.filter { it.isAlive && it.owner != BattleUnit.Owner.NATURAL }
            else ->
                allUnits.filter { it.isAlive && it.owner != unit.owner && it.owner != BattleUnit.Owner.NATURAL }
        }
        val combatTargets = enemies.filter { !it.type.isStructure }
        if (combatTargets.isEmpty())
            return enemies.minByOrNull { unit.distanceToPosition(it.position) }

        // If already adjacent to any combat target, attack it immediately.
        combatTargets.find { unit.isAdjacentToPosition(it.position) }?.let { return it }

        return combatTargets.minByOrNull { enemy ->
            val flank = pickFlankPosition(unit, enemy, allUnits, grid)
                ?: return@minByOrNull 0   // already adjacent — score 0 (best)
            val ghost = findPathOntoIgnoringAllies(unit, flank, allUnits, grid)
            if (ghost.isEmpty()) return@minByOrNull Int.MAX_VALUE  // completely unreachable

            // Add a heavy penalty if the very first step is blocked by an ally.
            // This steers the unit away from crowded corridors toward reachable flanks.
            val firstStep = ghost.first()
            val firstStepBlocked = allUnits.any {
                it.isAlive && it.id != unit.id && it.owner == unit.owner &&
                !it.type.isStructure && it.occupies(firstStep)
            }
            ghost.size + if (firstStepBlocked) 500 else 0
        }
    }

    /**
     * Returns the best unoccupied tile adjacent (Chebyshev-1) to [target] for [unit] to
     * stand on — i.e. the attack position it should navigate to.
     *
     * Scoring:
     * - Huge penalty (1000) for positions already occupied by allies (spread the attack).
     * - Among unoccupied slots, prefer the one closest (Manhattan) to the unit.
     *
     * Returns null if the unit is already adjacent or there are no valid candidates.
     */
    private fun pickFlankPosition(unit: BattleUnit, target: BattleUnit, allUnits: List<BattleUnit>, grid: BattleGrid): Position? {
        if (unit.isAdjacentToPosition(target.position)) return null

        val targetSize = target.size
        val unitSize   = unit.size

        // All tiles within Chebyshev-1 of any tile in the target's footprint.
        val candidateSet = mutableSetOf<Position>()
        for (tx in 0 until targetSize) for (ty in 0 until targetSize) {
            val tp = Position(target.position.x + tx, target.position.y + ty)
            for (dx in -1..1) for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                candidateSet.add(Position(tp.x + dx, tp.y + dy))
            }
        }
        // Exclude the target's own footprint tiles.
        for (tx in 0 until targetSize) for (ty in 0 until targetSize)
            candidateSet.remove(Position(target.position.x + tx, target.position.y + ty))

        // Allied positions to avoid crowding.
        val allyOccupied = allUnits
            .filter { it.isAlive && it.id != unit.id && it.owner == unit.owner && !it.type.isStructure }
            .flatMap { it.occupiedTiles() }.toSet()

        fun canFitAt(pos: Position): Boolean = (0 until unitSize).all { dx -> (0 until unitSize).all { dy ->
            val t = Position(pos.x + dx, pos.y + dy)
            grid.isInBounds(t) && grid.isPassable(t)
        }}

        val valid = candidateSet.filter { canFitAt(it) }
        if (valid.isEmpty()) return null

        return valid.minByOrNull { pos ->
            val crowdPenalty = if (pos in allyOccupied) 1000 else 0
            crowdPenalty + unit.position.manhattanDistance(pos)
        }
    }

    /**
     * Move [unit] onto exactly [destination] (not merely adjacent to it).
     * Uses corridor-patience: if the path is blocked mostly by allies, advance along
     * the ghost path (ignoring friendlies) and stop when an ally blocks the next step.
     */
    private fun moveOnto(unit: BattleUnit, destination: Position, allUnits: List<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {
        if (unit.position == destination) return
        val path = findPathOnto(unit, destination, allUnits, grid)

        // Corridor patience: compare real path to ghost path (ignoring allies).
        val ghostPath = findPathOntoIgnoringAllies(unit, destination, allUnits, grid)
        val detourTooLong = ghostPath.isNotEmpty() && (path.isEmpty() || path.size > ghostPath.size + 4)

        val stepsToTake = when {
            detourTooLong -> {
                val steps = mutableListOf<Position>()
                for (step in ghostPath.take(unit.type.speed)) {
                    val occ = allUnits.any { it.isAlive && it.id != unit.id && !it.type.isStructure && it.occupies(step) }
                    if (occ) break
                    steps.add(step)
                }
                steps
            }
            path.isEmpty() -> return
            else -> path.take(unit.type.speed)
        }

        for (step in stepsToTake) {
            if (unit.position == destination) break
            val from = unit.position
            unit.position = step
            events.add(BattleEvent.Move(unit.id, unit.displayName, from, step))
        }
    }

    /** BFS from [movingUnit] that tries to reach exactly [goal] (unit must fit at goal). */
    private fun findPathOnto(movingUnit: BattleUnit, goal: Position, allUnits: List<BattleUnit>, grid: BattleGrid): List<Position> {
        val start    = movingUnit.position
        val unitSize = movingUnit.size
        val occupied = mutableSetOf<Position>()
        for (other in allUnits) {
            if (!other.isAlive || other.id == movingUnit.id || other.type.isStructure) continue
            occupied.addAll(other.occupiedTiles())
        }

        fun canFitAt(pos: Position): Boolean = (0 until unitSize).all { dx -> (0 until unitSize).all { dy ->
            val t = Position(pos.x + dx, pos.y + dy)
            grid.isInBounds(t) && grid.isPassable(t) && t !in occupied
        }}

        if (goal == start) return emptyList()
        if (!canFitAt(goal)) return emptyList()

        val visited  = mutableSetOf(start)
        val queue    = ArrayDeque<Position>()
        val cameFrom = mutableMapOf<Position, Position>()
        queue.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == goal) return reconstructPath(cameFrom, start, current)
            for (nb in getCardinalNeighbors(current)) {
                if (nb in visited || !canFitAt(nb)) continue
                visited.add(nb); cameFrom[nb] = current; queue.add(nb)
            }
        }
        return emptyList()
    }

    /** Same as [findPathOnto] but treats friendly units as transparent (terrain + enemies only). */
    private fun findPathOntoIgnoringAllies(movingUnit: BattleUnit, goal: Position, allUnits: List<BattleUnit>, grid: BattleGrid): List<Position> {
        val start    = movingUnit.position
        val unitSize = movingUnit.size
        val enemyOcc = mutableSetOf<Position>()
        for (other in allUnits) {
            if (!other.isAlive || other.id == movingUnit.id || other.type.isStructure) continue
            if (other.owner == movingUnit.owner) continue  // skip allies
            enemyOcc.addAll(other.occupiedTiles())
        }

        fun canFitAt(pos: Position): Boolean = (0 until unitSize).all { dx -> (0 until unitSize).all { dy ->
            val t = Position(pos.x + dx, pos.y + dy)
            grid.isInBounds(t) && grid.isPassable(t) && t !in enemyOcc
        }}

        if (goal == start) return emptyList()
        if (!canFitAt(goal)) return emptyList()

        val visited  = mutableSetOf(start)
        val queue    = ArrayDeque<Position>()
        val cameFrom = mutableMapOf<Position, Position>()
        queue.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == goal) return reconstructPath(cameFrom, start, current)
            for (nb in getCardinalNeighbors(current)) {
                if (nb in visited || !canFitAt(nb)) continue
                visited.add(nb); cameFrom[nb] = current; queue.add(nb)
            }
        }
        return emptyList()
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

        // Corridor patience: avoid huge detours caused by allies blocking the direct path.
        // Compute a ghost path that ignores friendly units (terrain + enemies only).
        // If the real path would be more than 4 steps longer than the ghost path,
        // advance along the ghost path until bumping into an ally — this queues the unit
        // right behind the frontline fighter instead of running the long way around.
        val ghostPath = findGhostPath(unit, target, allUnits, grid)
        val detourTooLong = ghostPath.isNotEmpty() && (path.isEmpty() || path.size > ghostPath.size + 4)
        if (detourTooLong) {
            for (step in ghostPath.take(unit.type.speed)) {
                val occupied = allUnits.any {
                    it.isAlive && it.id != unit.id && !it.type.isStructure && it.occupies(step)
                }
                if (occupied) break  // stop right behind the frontline ally
                val from = unit.position
                unit.position = step
                events.add(BattleEvent.Move(unit.id, unit.displayName, from, step))
            }
            return
        }

        if (path.isEmpty()) return
        for (step in path.take(unit.type.speed)) {
            if (unit.isAdjacentToPosition(target)) break
            val from = unit.position
            unit.position = step
            events.add(BattleEvent.Move(unit.id, unit.displayName, from, step))
        }
    }

    /**
     * BFS that ignores friendly units as movement obstacles (only terrain and enemies block).
     * Used to compute the "ideal" direct path for corridor-patience checks.
     */
    private fun findGhostPath(movingUnit: BattleUnit, goal: Position, allUnits: List<BattleUnit>, grid: BattleGrid): List<Position> {
        val start = movingUnit.position
        val unitSize = movingUnit.size
        val enemyOccupied = mutableSetOf<Position>()
        for (other in allUnits) {
            if (!other.isAlive || other.id == movingUnit.id) continue
            if (other.type.isStructure) continue
            if (other.owner == movingUnit.owner) continue  // friendlies are transparent
            enemyOccupied.addAll(other.occupiedTiles())
        }

        fun canFitAt(pos: Position): Boolean {
            for (dx in 0 until unitSize) for (dy in 0 until unitSize) {
                val tile = Position(pos.x + dx, pos.y + dy)
                if (!grid.isInBounds(tile) || !grid.isPassable(tile) || tile in enemyOccupied) return false
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
