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

            for (troll in simUnits.filter { it.isAlive && it.type is MonsterUnitType.Troll }) {
                if (troll.currentHp < troll.type.maxHp) {
                    val regenAmt = 5
                    troll.heal(regenAmt)
                    events.add(BattleEvent.Heal(troll.id, troll.displayName, troll.id, troll.displayName, troll.position, regenAmt, troll.currentHp))
                }
            }

            val sortedUnits = simUnits.filter { it.isAlive }.sortedByDescending { it.type.speed }

            for (unit in sortedUnits) {
                if (!unit.isAlive) continue
                unit.tickCooldown()
                unit.tickSecondaryCooldown()

                when (unit.type) {
                    is NormalUnitType.Melee, is NormalUnitType.MeleeSilver, is NormalUnitType.MeleeGold -> processMelee(unit, simUnits, grid, events)
                    is NormalUnitType.Ranged, is NormalUnitType.RangedSilver, is NormalUnitType.RangedGold -> processRanged(unit, simUnits, grid, events)
                    is NormalUnitType.Healer, is NormalUnitType.HealerSilver, is NormalUnitType.HealerGold -> processHealer(unit, simUnits, grid, events)
                    is NormalUnitType.Knight, is NormalUnitType.KnightSilver, is NormalUnitType.KnightGold -> processMelee(unit, simUnits, grid, events)
                    is NormalUnitType.GriffinRider, is NormalUnitType.GriffinRiderSilver, is NormalUnitType.GriffinRiderGold -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.Bear    -> processBear(unit, simUnits, grid, events)
                    is MonsterUnitType.Wolf    -> processWolf(unit, simUnits, grid, events)
                    is MonsterUnitType.Goblin      -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.Skeleton    -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.DarkMage    -> processRanged(unit, simUnits, grid, events)
                    is MonsterUnitType.Troll       -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.Bandit      -> processRanged(unit, simUnits, grid, events)
                    is MonsterUnitType.OrcWarrior  -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.Spider      -> processMelee(unit, simUnits, grid, events)
                    is MonsterUnitType.Dragon      -> processDragon(unit, simUnits, grid, events)
                    is SpecialUnitType.Farmer, is SpecialUnitType.FarmerSilver, is SpecialUnitType.FarmerGold -> processFarmer(unit, simUnits, grid, events)
                    is SpecialUnitType.Wizard, is SpecialUnitType.WizardSilver, is SpecialUnitType.WizardGold -> processWizard(unit, simUnits, grid, events)
                    is StructureUnitType.Farm  -> { /* passive — gold generation handled above */ }
                    is StructureUnitType.Wall  -> { /* passive — blocks movement */ }
                    is StructureUnitType.Gate  -> { /* passive — blocks enemy movement */ }
                    is StructureUnitType.Tower -> processTower(unit, simUnits, grid, events)
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
        if (!unit.isAdjacentToUnit(target)) {
            val flankPos = pickFlankPosition(unit, target, allUnits, grid)
            if (flankPos != null) {
                // Move onto the chosen flank tile (exact destination, not adjacent-to it).
                moveOnto(unit, flankPos, allUnits, grid, events)
            } else {
                // No flank tile available (all occupied / unit boxed in) — fall back.
                moveToward(unit, target.position, allUnits, grid, events)
            }
        }

        val distToTarget = unit.distanceToUnit(target)
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
                allUnits.filter { it.isAlive && it.owner != unit.owner }
        }
        val combatTargets = enemies.filter { !it.type.isStructure }
        if (combatTargets.isEmpty())
            return enemies.minByOrNull { unit.distanceToUnit(it) }

        // If already adjacent to any combat target, attack it immediately.
        combatTargets.find { unit.isAdjacentToUnit(it) }?.let { return it }

        val bestCombatTarget = combatTargets.minByOrNull { enemy ->
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

        // If the chosen combat target is completely unreachable (all paths blocked — e.g. by a Wall),
        // break down the nearest blocking structure instead of standing idle.
        // Multiple attackers will converge on the same nearest structure, killing it faster.
        if (bestCombatTarget != null && !unit.isAdjacentToUnit(bestCombatTarget)) {
            val flank = pickFlankPosition(unit, bestCombatTarget, allUnits, grid)
            if (flank == null || findPathOntoIgnoringAllies(unit, flank, allUnits, grid).isEmpty()) {
                enemies.filter { it.type.blocksMovement }
                    .minByOrNull { unit.distanceToUnit(it) }
                    ?.let { return it }
            }
        }
        return bestCombatTarget
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
        if (unit.isAdjacentToUnit(target)) return null

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
            grid.isInBounds(t) && (unit.type.canFly || grid.isPassable(t))
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
            if (!other.isAlive || other.id == movingUnit.id) continue
            if (other.type is StructureUnitType && !blocksMovementFor(other, movingUnit)) continue
            if (other.type !is StructureUnitType && movingUnit.type.canFly && other.owner == movingUnit.owner) continue
            occupied.addAll(other.occupiedTiles())
        }

        fun canFitAt(pos: Position): Boolean = (0 until unitSize).all { dx -> (0 until unitSize).all { dy ->
            val t = Position(pos.x + dx, pos.y + dy)
            grid.isInBounds(t) && (movingUnit.type.canFly || grid.isPassable(t)) && t !in occupied
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
            if (!other.isAlive || other.id == movingUnit.id) continue
            if (other.type is StructureUnitType) {
                if (blocksMovementFor(other, movingUnit)) enemyOcc.addAll(other.occupiedTiles())
                continue
            }
            if (other.owner == movingUnit.owner) continue  // skip allies
            enemyOcc.addAll(other.occupiedTiles())
        }

        fun canFitAt(pos: Position): Boolean = (0 until unitSize).all { dx -> (0 until unitSize).all { dy ->
            val t = Position(pos.x + dx, pos.y + dy)
            grid.isInBounds(t) && (movingUnit.type.canFly || grid.isPassable(t)) && t !in enemyOcc
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
     * Wolf behaviour depends on its owner:
     *  - PLAYER/ENEMY: standard melee + pack-bonus damage (like any other fighter)
     *  - NATURAL: idle until an intruder enters [WOLF_AGGRO_RANGE].
     *    When ANY wolf in the pack aggros, it chain-spreads to all NATURAL wolves
     *    within [WOLF_AGGRO_RANGE] of any already-aggroed wolf (BFS flood-fill).
     *    Once aggroed a wolf stays aggroed forever (isAggroed flag).
     */
    private fun processWolf(unit: BattleUnit, allUnits: List<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {

        // ── Helper: apply pack-bonus attack ─────────────────────────────────────
        fun wolfAttack(target: BattleUnit) {
            val nearbyAllyWolf = allUnits.any {
                it.id != unit.id &&
                    it.isAlive &&
                    it.owner == unit.owner &&
                    it.type is MonsterUnitType.Wolf &&
                    unit.distanceToUnit(it) <= 1
            }
            val damage = if (nearbyAllyWolf) unit.type.power * 2 else unit.type.power
            target.takeDamage(damage)
            unit.startCooldown()
            events.add(BattleEvent.Attack(unit.id, unit.displayName, unit.position, target.id, target.displayName, target.position, damage, target.currentHp, unit.type))
            if (!target.isAlive) events.add(BattleEvent.Death(target.id, target.displayName))
        }

        if (unit.owner != BattleUnit.Owner.NATURAL) {
            // ── PLAYER / ENEMY wolf: behaves like a normal fighter ───────────────
            val target = pickReachableTarget(unit, allUnits, grid) ?: return
            if (!unit.isAdjacentToUnit(target)) {
                val flankPos = pickFlankPosition(unit, target, allUnits, grid)
                if (flankPos != null) moveOnto(unit, flankPos, allUnits, grid, events)
                else moveToward(unit, target.position, allUnits, grid, events)
            }
            if (unit.canAct && unit.distanceToUnit(target) in unit.type.attackRange) {
                wolfAttack(target)
            }
        } else {
            // ── NATURAL wolf: idle until aggroed, then chain-spread to the pack ──

            // Step 1: check if this wolf can directly detect an intruder
            val directIntruder = allUnits.any { other ->
                other.isAlive &&
                    other.owner != BattleUnit.Owner.NATURAL &&
                    unit.distanceToUnit(other) <= WOLF_AGGRO_RANGE
            }
            if (directIntruder) unit.isAggroed = true

            // Step 2: if this wolf just became aggroed (or already was), BFS-spread
            //         the aggro flag to all NATURAL wolves reachable through the pack.
            if (unit.isAggroed) {
                val naturalWolves = allUnits.filter {
                    it.isAlive && it.owner == BattleUnit.Owner.NATURAL && it.type is MonsterUnitType.Wolf
                }
                // BFS from every currently-aggroed wolf
                val aggroedIds = naturalWolves.filter { it.isAggroed }.map { it.id }.toMutableSet()
                val frontier = ArrayDeque(naturalWolves.filter { it.isAggroed })
                while (frontier.isNotEmpty()) {
                    val current = frontier.removeFirst()
                    for (neighbour in naturalWolves) {
                        if (neighbour.id !in aggroedIds &&
                            current.distanceToUnit(neighbour) <= WOLF_AGGRO_RANGE) {
                            neighbour.isAggroed = true
                            aggroedIds.add(neighbour.id)
                            frontier.add(neighbour)
                        }
                    }
                }
            }

            // Step 3: if not yet aggroed, stay idle
            if (!unit.isAggroed) return

            // Step 4: pursue and attack closest non-NATURAL target
            val target = allUnits
                .filter { it.isAlive && it.owner != BattleUnit.Owner.NATURAL }
                .minByOrNull { unit.distanceToUnit(it) } ?: return
            moveToward(unit, target.position, allUnits, grid, events)
            if (unit.canAct && unit.distanceToUnit(target) in unit.type.attackRange) {
                wolfAttack(target)
            }
        }
    }

    private fun processBear(unit: BattleUnit, allUnits: List<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {
        val target = pickReachableTarget(unit, allUnits, grid) ?: return

        if (!unit.isAdjacentToUnit(target)) {
            val flankPos = pickFlankPosition(unit, target, allUnits, grid)
            if (flankPos != null) {
                moveOnto(unit, flankPos, allUnits, grid, events)
            } else {
                moveToward(unit, target.position, allUnits, grid, events)
            }
        }

        val distToTarget = unit.distanceToUnit(target)
        if (unit.canAct && distToTarget in unit.type.attackRange) {
            val primaryDamage = unit.type.power
            target.takeDamage(primaryDamage)

            val splashDamage = unit.type.power / 2
            val splashTargets = allUnits.filter { candidate ->
                candidate.isAlive &&
                    candidate.id != unit.id &&
                    candidate.id != target.id &&
                    areUnitsAdjacent(candidate, target)
            }
            val splashHits = splashTargets.map { splashTarget ->
                splashTarget.takeDamage(splashDamage)
                BattleEvent.SplashHit(splashTarget.id, splashTarget.displayName, splashTarget.position, splashDamage, splashTarget.currentHp)
            }

            events.add(
                BattleEvent.Cleave(
                    attackerId = unit.id,
                    attackerName = unit.displayName,
                    attackerPos = unit.position,
                    primaryId = target.id,
                    primaryName = target.displayName,
                    primaryDmg = primaryDamage,
                    primaryHpAfter = target.currentHp,
                    splashHits = splashHits
                )
            )

            if (!target.isAlive) events.add(BattleEvent.Death(target.id, target.displayName))
            for (splashTarget in splashTargets) {
                if (!splashTarget.isAlive) events.add(BattleEvent.Death(splashTarget.id, splashTarget.displayName))
            }
            unit.startCooldown()
        }
    }

    private fun processDragon(unit: BattleUnit, allUnits: List<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {
        val target = pickReachableTarget(unit, allUnits, grid) ?: return
        val distanceToTarget = unit.distanceToUnit(target)

        if (unit.canAct && distanceToTarget in unit.type.attackRange) {
            performAttack(unit, target, events)
        }

        val closestEnemy = getPriorityTarget(unit, allUnits)
        if (unit.canUseAbility && closestEnemy != null) {
            val direction = dragonBreathDirection(unit, closestEnemy)
            val coneTiles = dragonBreathTiles(unit, direction, grid)
            val hitUnits = allUnits.filter { candidate ->
                candidate.isAlive &&
                    candidate.id != unit.id &&
                    candidate.occupiedTiles().any { it in coneTiles }
            }
            val damage = 10
            val hits = hitUnits.map { hitUnit ->
                hitUnit.takeDamage(damage)
                BattleEvent.FireBreathHit(hitUnit.id, hitUnit.displayName, hitUnit.position, damage, hitUnit.currentHp)
            }
            events.add(BattleEvent.FireBreath(unit.id, unit.displayName, unit.position, direction, coneTiles.toList(), hits))
            for (hitUnit in hitUnits) {
                if (!hitUnit.isAlive) events.add(BattleEvent.Death(hitUnit.id, hitUnit.displayName))
            }
            unit.startSecondaryCooldown(10)
        }

        if (distanceToTarget !in unit.type.attackRange) {
            moveToward(unit, target.position, allUnits, grid, events)
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
                && grid.hasLineOfSightWithUnits(unit.position, it.position, allUnits)
        }

        when {
            distance < unit.type.attackRange.first -> moveAway(unit, closestEnemy.position, allUnits, grid, events)
            combatTargetInRange != null             -> { /* stay — real target is in range */ }
            else                                    -> moveToward(unit, closestEnemy.position, allUnits, grid, events)
        }

        // After optional move: shoot best available target (structures as last resort)
        val shootTarget = enemies.find {
            unit.position.distanceTo(it.position) in unit.type.attackRange
                && grid.hasLineOfSightWithUnits(unit.position, it.position, allUnits)
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

    private fun processWizard(unit: BattleUnit, allUnits: MutableList<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {
        val enemies = getEnemiesOf(unit, allUnits)
        val closestEnemy = enemies.minByOrNull { unit.distanceToUnit(it) } ?: return

        // Find enemies in attack range (Chebyshev distance).
        val enemiesInRange = enemies.filter { enemy ->
            unit.distanceToUnit(enemy) in unit.type.attackRange
        }

        // Move only if no enemy is currently in range — otherwise hold position.
        if (enemiesInRange.isEmpty()) {
            moveToward(unit, closestEnemy.position, allUnits, grid, events)
            return
        }

        // On cooldown but already in range — stay still.
        if (!unit.canAct) return

        val fireball = (unit.type as SpecialUnitType).ability as SpecialAbility.Fireball

        // Pick the TARGET TILE (not a unit) that maximises total enemies hit by the 3×3 blast.
        val bestTile = enemiesInRange.maxByOrNull { candidate ->
            enemies.count { enemy ->
                enemy.occupiedTiles().any { tile ->
                    tile.distanceTo(candidate.position) <= fireball.radius
                }
            }
        }?.position ?: return

        // Blast area: all tiles within Chebyshev radius of bestTile.
        val blastTiles = buildSet {
            for (dx in -fireball.radius..fireball.radius)
                for (dy in -fireball.radius..fireball.radius)
                    add(Position(bestTile.x + dx, bestTile.y + dy))
        }

        // Collect ALL living units (friend + foe) whose footprint overlaps the blast.
        val hitUnits = allUnits.filter { target ->
            target.isAlive && target.id != unit.id &&
            target.occupiedTiles().any { it in blastTiles }
        }

        if (hitUnits.isEmpty()) return

        // Apply damage and record hits.
        val hits = hitUnits.map { target ->
            target.takeDamage(fireball.power)
            BattleEvent.FireballHit(target.id, target.displayName, target.position, fireball.power, target.currentHp)
        }

        events.add(BattleEvent.Fireball(
            unit.id, unit.displayName, unit.position, unit.type,
            bestTile, fireball.radius, hits
        ))

        // Death events for any units killed by the blast.
        for (target in hitUnits) {
            if (!target.isAlive) events.add(BattleEvent.Death(target.id, target.displayName))
        }

        unit.startCooldown()
    }

    // ─── TOWER ──────────────────────────────────────────────────────────────────

    private fun processTower(unit: BattleUnit, allUnits: List<BattleUnit>, grid: BattleGrid, events: MutableList<BattleEvent>) {
        if (!unit.canAct) return
        val enemies = getEnemiesOf(unit, allUnits).filter { !it.type.isStructure }
        val target = enemies
            .filter {
                unit.distanceToUnit(it) in unit.type.attackRange &&
                    grid.hasLineOfSightWithUnits(unit.position, it.position, allUnits)
            }
            .minByOrNull { unit.distanceToUnit(it) }
            ?: return
        performAttack(unit, target, events)
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
            if (other.type is StructureUnitType) {
                if (blocksMovementFor(other, movingUnit)) enemyOccupied.addAll(other.occupiedTiles())
                continue
            }
            if (other.owner == movingUnit.owner) continue  // regular allies are transparent
            enemyOccupied.addAll(other.occupiedTiles())
        }

        fun canFitAt(pos: Position): Boolean {
            for (dx in 0 until unitSize) for (dy in 0 until unitSize) {
                val tile = Position(pos.x + dx, pos.y + dy)
                if (!grid.isInBounds(tile) || (!movingUnit.type.canFly && !grid.isPassable(tile)) || tile in enemyOccupied) return false
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
            if (other.type is StructureUnitType && !blocksMovementFor(other, movingUnit)) continue
            if (other.type !is StructureUnitType && movingUnit.type.canFly && other.owner == movingUnit.owner) continue
            occupiedByOthers.addAll(other.occupiedTiles())
        }

        fun canFitAt(pos: Position): Boolean {
            for (dx in 0 until unitSize) for (dy in 0 until unitSize) {
                val tile = Position(pos.x + dx, pos.y + dy)
                if (!grid.isInBounds(tile) || (!movingUnit.type.canFly && !grid.isPassable(tile)) || tile in occupiedByOthers) return false
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
            if (other.type is StructureUnitType && !blocksMovementFor(other, unit)) continue
            if (other.type !is StructureUnitType && unit.type.canFly && other.owner == unit.owner) continue
            occupiedByOthers.addAll(other.occupiedTiles())
        }
        return getCardinalNeighbors(unit.position)
            .filter { pos ->
                (0 until unit.size).all { dx -> (0 until unit.size).all { dy ->
                    val tile = Position(pos.x + dx, pos.y + dy)
                    grid.isInBounds(tile) && (unit.type.canFly || grid.isPassable(tile)) && tile !in occupiedByOthers
                }}
            }
            .maxByOrNull { it.manhattanDistance(threat) }
    }

    /**
     * Returns true if [obstacle] blocks movement for [mover].
     * - Non-blocking structures (Farm): never block
     * - Gate (allowsFriendlyPassage): blocks enemies but not allies
     * - Wall, Tower: always block
     * - Regular non-structure units: handled separately (not via this helper)
     */
    private fun blocksMovementFor(obstacle: BattleUnit, mover: BattleUnit): Boolean {
        if (!obstacle.type.blocksMovement) return false
        if (obstacle.type.allowsFriendlyPassage && obstacle.owner == mover.owner) return false
        return true
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

    private fun areUnitsAdjacent(first: BattleUnit, second: BattleUnit): Boolean =
        first.occupiedTiles().any { firstTile ->
            second.occupiedTiles().any { secondTile ->
                firstTile.distanceTo(secondTile) <= 1
            }
        }

    private fun dragonBreathDirection(dragon: BattleUnit, target: BattleUnit): String {
        val dragonCenterX = dragon.position.x + (dragon.size - 1) / 2f
        val dragonCenterY = dragon.position.y + (dragon.size - 1) / 2f
        val targetCenterX = target.position.x + (target.size - 1) / 2f
        val targetCenterY = target.position.y + (target.size - 1) / 2f
        val dx = targetCenterX - dragonCenterX
        val dy = targetCenterY - dragonCenterY

        return if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
            if (dx >= 0f) "RIGHT" else "LEFT"
        } else {
            if (dy >= 0f) "UP" else "DOWN"
        }
    }

    private fun dragonBreathTiles(dragon: BattleUnit, direction: String, grid: BattleGrid): Set<Position> =
        buildSet {
            for (d in 1..4) {
                when (direction) {
                    "RIGHT" -> {
                        val x = dragon.position.x + 1 + d
                        val half = (d - 1)
                        for (y in dragon.position.y - half..dragon.position.y + 1 + half) {
                            val tile = Position(x, y)
                            if (grid.isInBounds(tile)) add(tile)
                        }
                    }
                    "LEFT" -> {
                        val x = dragon.position.x - d
                        val half = (d - 1)
                        for (y in dragon.position.y - half..dragon.position.y + 1 + half) {
                            val tile = Position(x, y)
                            if (grid.isInBounds(tile)) add(tile)
                        }
                    }
                    "UP" -> {
                        val y = dragon.position.y + 1 + d
                        val half = (d - 1)
                        for (x in dragon.position.x - half..dragon.position.x + 1 + half) {
                            val tile = Position(x, y)
                            if (grid.isInBounds(tile)) add(tile)
                        }
                    }
                    else -> {
                        val y = dragon.position.y - d
                        val half = (d - 1)
                        for (x in dragon.position.x - half..dragon.position.x + 1 + half) {
                            val tile = Position(x, y)
                            if (grid.isInBounds(tile)) add(tile)
                        }
                    }
                }
            }
        }

    // ─── Utility ────────────────────────────────────────────────────────────────

    /**
     * Returns enemies of [unit], with non-structure units first.
     * Structures (Farm) are last-resort targets.
     * PLAYER targets ENEMY + NATURAL; ENEMY targets PLAYER + NATURAL;
     * NATURAL targets PLAYER + ENEMY (all non-NATURAL).
     */
    private fun getEnemiesOf(unit: BattleUnit, allUnits: List<BattleUnit>): List<BattleUnit> {
        val enemies = when (unit.owner) {
            BattleUnit.Owner.NATURAL ->
                allUnits.filter { it.isAlive && it.owner != BattleUnit.Owner.NATURAL }
            else ->
                allUnits.filter { it.isAlive && it.owner != unit.owner }
        }
        return enemies.sortedBy { if (it.type.isStructure) 1 else 0 }
    }

    /**
     * Returns the highest-priority target for [unit]:
     * - Non-structure enemies first, by proximity
     * - Falls back to structures only if no other targets remain
     * PLAYER/ENEMY include NATURAL units as valid targets.
     */
    private fun getPriorityTarget(unit: BattleUnit, allUnits: List<BattleUnit>): BattleUnit? {
        val enemies = when (unit.owner) {
            BattleUnit.Owner.NATURAL ->
                allUnits.filter { it.isAlive && it.owner != BattleUnit.Owner.NATURAL }
            else ->
                allUnits.filter { it.isAlive && it.owner != unit.owner }
        }
        val combatTargets = enemies.filter { !it.type.isStructure }
        return if (combatTargets.isNotEmpty())
            combatTargets.minByOrNull { unit.distanceToUnit(it) }
        else
            enemies.minByOrNull { unit.distanceToUnit(it) }
    }

    private fun getAlliesOf(unit: BattleUnit, allUnits: List<BattleUnit>): List<BattleUnit> =
        allUnits.filter { it.isAlive && it.owner == unit.owner }

}
