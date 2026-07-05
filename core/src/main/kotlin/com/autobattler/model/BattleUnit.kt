package com.autobattler.model

/**
 * A live unit instance on the battle grid.
 * Holds mutable runtime state (position, HP, cooldown timer).
 *
 * For multi-tile units (size > 1), [position] is the bottom-left corner.
 * A 2x2 unit at position (x,y) occupies: (x,y), (x+1,y), (x,y+1), (x+1,y+1).
 */
data class BattleUnit(
    val id: Int,
    val type: UnitType,
    val owner: Owner,
    var position: Position,
    var currentHp: Int = type.maxHp,
    var cooldownTimer: Int = 0,
    var secondaryCooldownTimer: Int = 0
) {
    val isAlive: Boolean get() = currentHp > 0

    val displayName: String get() = "${owner.label} ${type.displayName}"

    val size: Int get() = type.size

    /** Whether this unit can act (attack/heal) this turn. */
    val canAct: Boolean get() = cooldownTimer <= 0
    val canUseAbility: Boolean get() = secondaryCooldownTimer <= 0

    /** All tiles this unit occupies (based on size and position). */
    fun occupiedTiles(): List<Position> {
        val tiles = mutableListOf<Position>()
        for (dx in 0 until size) {
            for (dy in 0 until size) {
                tiles.add(Position(position.x + dx, position.y + dy))
            }
        }
        return tiles
    }

    /** Check if this unit occupies a specific tile. */
    fun occupies(pos: Position): Boolean {
        return pos.x in position.x until position.x + size
            && pos.y in position.y until position.y + size
    }

    /**
     * Check if another position is adjacent to any tile this unit occupies.
     * Used for melee attack range checks.
     */
    fun isAdjacentToPosition(pos: Position): Boolean {
        for (tile in occupiedTiles()) {
            if (tile.isAdjacentTo(pos)) return true
        }
        return false
    }

    /**
     * Minimum distance from any of this unit's tiles to a target position.
     */
    fun distanceToPosition(pos: Position): Int {
        return occupiedTiles().minOf { it.distanceTo(pos) }
    }

    /** Reset cooldown after performing an action. */
    fun startCooldown() {
        cooldownTimer = type.cooldown
    }

    fun startSecondaryCooldown(cd: Int) {
        secondaryCooldownTimer = cd
    }

    /** Tick the cooldown timer at start of turn. */
    fun tickCooldown() {
        if (cooldownTimer > 0) cooldownTimer--
    }

    fun tickSecondaryCooldown() {
        if (secondaryCooldownTimer > 0) secondaryCooldownTimer--
    }

    /** Apply damage, clamping HP to 0. */
    fun takeDamage(amount: Int) {
        currentHp = (currentHp - amount).coerceAtLeast(0)
    }

    /** Apply healing, clamping HP to max. */
    fun heal(amount: Int) {
        currentHp = (currentHp + amount).coerceAtMost(type.maxHp)
    }

    enum class Owner(val label: String) {
        PLAYER("Player"),
        ENEMY("Enemy"),
        /** Neutral wildlife — hostile to anyone who enters their territory. */
        NATURAL("Natural")
    }
}
