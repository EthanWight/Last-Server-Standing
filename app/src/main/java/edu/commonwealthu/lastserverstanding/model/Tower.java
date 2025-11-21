package edu.commonwealthu.lastserverstanding.model;

import android.graphics.PointF;

/**
 * Abstract base class for all tower types in the tower defense game.
 *
 * <p>Towers are defensive structures placed on the game grid that automatically
 * target and attack enemies within their range. Each tower type has unique
 * characteristics defined in subclasses (damage type, projectile behavior, etc.).</p>
 *
 * <h2>Responsibilities:</h2>
 * <ul>
 *   <li>Position management on the game grid</li>
 *   <li>Combat statistics (damage, range, fire rate)</li>
 *   <li>Target acquisition and range checking</li>
 *   <li>Upgrade system with stat scaling</li>
 *   <li>Attack timing and cooldown management</li>
 *   <li>Investment tracking for sell-back calculations</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Create a concrete tower subclass
 * Tower firewall = new FirewallTower(gridPosition);
 *
 * // Set target (typically done by game manager)
 * tower.setTarget(nearestEnemy);
 *
 * // Update each frame
 * tower.update(deltaTime);
 *
 * // Fire if ready (returns projectile or null)
 * Projectile projectile = tower.fire();
 * if (projectile != null) {
 *     projectiles.add(projectile);
 * }
 *
 * // Upgrade tower
 * int cost = tower.getUpgradeCost();
 * if (player.canAfford(cost)) {
 *     tower.upgrade(cost);
 * }
 * }</pre>
 *
 * <h2>Upgrade System:</h2>
 * <p>Towers can be upgraded from level 1 to level 5. Each upgrade:</p>
 * <ul>
 *   <li>Costs: baseCost * 2^currentLevel (exponential scaling)</li>
 *   <li>Damage: +20% per level</li>
 *   <li>Range: +10% per level</li>
 *   <li>Fire Rate: +20% per level</li>
 * </ul>
 *
 * <h2>Subclasses must implement:</h2>
 * <ul>
 *   <li>{@link #update(float)} - Tower-specific update logic</li>
 *   <li>{@link #fire()} - Create and return tower-specific projectiles</li>
 *   <li>{@link #getType()} - Return the tower type identifier</li>
 * </ul>
 *
 * @author Ethan Wight
 * @see Projectile
 * @see Enemy
 */
public abstract class Tower {

    /**
     * Position of this tower on the game grid.
     * Towers are stationary once placed and cannot be moved.
     */
    protected final PointF position;

    /**
     * Current upgrade level of this tower.
     * Range: 1 (base) to 5 (max).
     * Higher levels increase damage, range, and fire rate.
     */
    protected int level;

    /**
     * Damage dealt per projectile hit.
     * Scales with upgrades: multiplied by 1.2 per level.
     */
    protected float damage;

    /**
     * Attack range in game units.
     * Enemies within this radius can be targeted.
     * Scales with upgrades: multiplied by 1.1 per level.
     */
    protected float range;

    /**
     * Number of attacks per second.
     * Determines the cooldown between shots.
     * Scales with upgrades: multiplied by 1.2 per level.
     */
    protected float fireRate;

    /**
     * Base cost to build this tower.
     * Used as the baseline for upgrade cost calculations.
     */
    protected final int cost;

    /**
     * Total resources invested in this tower (base cost + all upgrades).
     * Used for calculating sell-back value.
     */
    protected int totalInvestment;

    /**
     * The enemy this tower is currently targeting.
     * May be null if no valid target is in range.
     * Set by the game manager's target acquisition system.
     */
    protected Enemy target;

    /**
     * Timestamp of the last projectile fired (in milliseconds).
     * Used for cooldown calculations to enforce fire rate.
     */
    protected long lastFireTime;

    /**
     * Constructs a new tower with the specified attributes.
     *
     * <p>The tower is initialized at level 1 with no current target
     * and ready to fire immediately (no initial cooldown).</p>
     *
     * @param position Position on the game grid (immutable after construction)
     * @param level    Initial upgrade level (typically 1)
     * @param damage   Base damage per projectile hit
     * @param range    Attack range in game units
     * @param fireRate Attacks per second
     * @param cost     Base cost to build this tower
     */
    public Tower(PointF position, int level, float damage, float range, float fireRate, int cost) {
        this.position = position;
        this.level = level;
        this.damage = damage;
        this.range = range;
        this.fireRate = fireRate;
        this.cost = cost;
        this.totalInvestment = cost;
        this.lastFireTime = 0;
    }

    /**
     * Updates the tower state for the current frame.
     *
     * <p>Subclasses implement tower-specific behavior such as:</p>
     * <ul>
     *   <li>Target validation and re-acquisition</li>
     *   <li>Special ability cooldowns</li>
     *   <li>Animation state updates</li>
     * </ul>
     *
     * @param deltaTime Time elapsed since the last update, in seconds
     */
    @SuppressWarnings("unused") // Parameter needed for interface consistency
    public abstract void update(float deltaTime);

    /**
     * Attempts to fire a projectile at the current target.
     *
     * <p>Subclasses implement tower-specific projectile creation,
     * including projectile type, status effects, and special behaviors.</p>
     *
     * <p>Should check {@link #isOnCooldown()} before creating a projectile.</p>
     *
     * @return A new projectile if fired successfully, {@code null} if unable to fire
     *         (no target, on cooldown, target out of range, etc.)
     */
    public abstract Projectile fire();

    /**
     * Returns the type identifier for this tower.
     *
     * <p>Used for display purposes, logging, and tower-specific game logic.</p>
     *
     * @return The type name of this tower (e.g., "Firewall", "Antivirus", "Encryption")
     */
    public abstract String getType();

    /**
     * Calculates the cost to upgrade this tower to the next level.
     *
     * <p>Upgrade cost formula: baseCost * 2^currentLevel</p>
     * <p>This creates exponential cost scaling:</p>
     * <ul>
     *   <li>Level 1 to 2: baseCost * 2</li>
     *   <li>Level 2 to 3: baseCost * 4</li>
     *   <li>Level 3 to 4: baseCost * 8</li>
     *   <li>Level 4 to 5: baseCost * 16</li>
     * </ul>
     *
     * @return The cost to upgrade, or 0 if already at maximum level (5)
     */
    public int getUpgradeCost() {
        // Maximum level is 5
        if (level >= 5) {
            return 0;
        }
        // Exponential cost scaling: cost * 2^level
        return (int)(cost * Math.pow(2.0, level));
    }

    /**
     * Upgrades this tower to the next level, improving all combat stats.
     *
     * <p>Stat improvements per upgrade:</p>
     * <ul>
     *   <li>Damage: +20% (multiplied by 1.2)</li>
     *   <li>Range: +10% (multiplied by 1.1)</li>
     *   <li>Fire Rate: +20% (multiplied by 1.2)</li>
     * </ul>
     *
     * <p>The upgrade cost is added to {@link #totalInvestment} for
     * sell-back value calculations.</p>
     *
     * @param upgradeCost The cost paid for this upgrade (added to total investment)
     * @return {@code true} if upgrade was successful, {@code false} if already at max level
     */
    public boolean upgrade(int upgradeCost) {
        // Check if already at maximum level
        if (level < 5) {
            level++;
            totalInvestment += upgradeCost;

            // Apply stat multipliers
            damage *= 1.2f;    // +20% damage
            range *= 1.1f;     // +10% range
            fireRate *= 1.2f;  // +20% fire rate
            return true;
        }
        return false;
    }

    /**
     * Checks if an enemy is outside this tower's attack range.
     *
     * <p>Uses squared distance comparison to avoid expensive sqrt operation.</p>
     *
     * @param enemy The enemy to check (may be null)
     * @return {@code true} if enemy is null or outside range, {@code false} if in range
     */
    protected boolean isOutOfRange(Enemy enemy) {
        // Null check - no enemy means "out of range"
        if (enemy == null) return true;

        // Calculate squared distance (avoids sqrt for performance)
        float dx = enemy.getPosition().x - position.x;
        float dy = enemy.getPosition().y - position.y;
        float distanceSquared = dx * dx + dy * dy;

        // Compare squared values to avoid sqrt
        return distanceSquared > range * range;
    }

    /**
     * Checks if this tower is on cooldown and cannot fire yet.
     *
     * <p>Cooldown is calculated as: 1000ms / fireRate</p>
     * <p>For example, a tower with fireRate=2.0 has a 500ms cooldown.</p>
     *
     * @return {@code true} if the tower cannot fire yet, {@code false} if ready
     */
    protected boolean isOnCooldown() {
        long currentTime = System.currentTimeMillis();

        // Calculate fire interval in milliseconds
        long fireInterval = (long)(1000 / fireRate);

        // Check if enough time has passed since last shot
        return currentTime - lastFireTime < fireInterval;
    }

    // ==================== Getters ====================

    /**
     * Returns the position of this tower on the game grid.
     *
     * @return The tower's grid position
     */
    public PointF getPosition() { return position; }

    /**
     * Returns the current upgrade level of this tower.
     *
     * @return Current level (1-5)
     */
    public int getLevel() { return level; }

    /**
     * Returns the current damage per projectile.
     *
     * @return Damage value (scales with upgrades)
     */
    public float getDamage() { return damage; }

    /**
     * Returns the current attack range.
     *
     * @return Range in game units (scales with upgrades)
     */
    public float getRange() { return range; }

    /**
     * Returns the current fire rate.
     *
     * @return Attacks per second (scales with upgrades)
     */
    public float getFireRate() { return fireRate; }

    /**
     * Returns the base cost of this tower.
     *
     * @return Base construction cost
     */
    public int getCost() { return cost; }

    /**
     * Returns the current target enemy.
     *
     * @return The targeted enemy, or {@code null} if no target
     */
    public Enemy getTarget() { return target; }

    /**
     * Returns the total resources invested in this tower.
     *
     * @return Base cost plus all upgrade costs
     */
    public int getTotalInvestment() { return totalInvestment; }

    // ==================== Setters ====================

    /**
     * Sets the target enemy for this tower.
     *
     * <p>Typically called by the game manager's target acquisition system.</p>
     *
     * @param target The enemy to target, or {@code null} to clear target
     */
    public void setTarget(Enemy target) { this.target = target; }
}
