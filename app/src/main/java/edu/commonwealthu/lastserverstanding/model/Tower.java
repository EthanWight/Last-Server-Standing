package edu.commonwealthu.lastserverstanding.model;

import android.graphics.PointF;

/**
 * Abstract base class for all tower types in the game.
 * Represents a defensive structure that attacks enemies within range.
 *
 * @author Ethan Wight
 */
public abstract class Tower {
    // Unique identifier for this tower instance
    protected String id;
    
    // Position on the game grid
    protected PointF position;
    
    // Current upgrade level (1-5)
    protected int level;
    
    // Combat stats
    protected float damage;
    protected float range;
    protected float fireRate; // Attacks per second
    
    // Economy
    protected int cost;
    protected int totalInvestment; // Total resources invested (base cost + upgrades)

    // Current target
    protected Enemy target;

    // Timing for attack rate
    protected long lastFireTime;

    /**
     * Constructor for tower base class.
     *
     * @param id Unique identifier for this tower instance.
     * @param position Position on the game grid.
     * @param level Current upgrade level (1-5).
     * @param damage Damage per shot.
     * @param range Attack range in units.
     * @param fireRate Attacks per second.
     * @param cost Cost to build this tower.
     */
    public Tower(String id, PointF position, int level, float damage, float range, float fireRate, int cost) {
        this.id = id;
        this.position = position;
        this.level = level;
        this.damage = damage;
        this.range = range;
        this.fireRate = fireRate;
        this.cost = cost;
        this.totalInvestment = cost; // Initialize with base cost
        this.lastFireTime = 0;
    }
    
    /**
     * Update tower state each frame.
     *
     * @param deltaTime Time since last update in seconds.
     */
    public abstract void update(float deltaTime);
    
    /**
     * Fire at the current target if possible.
     *
     * @return Projectile if fired, null otherwise.
     */
    public abstract Projectile fire();

    /**
     * Get the tower type name.
     *
     * @return The type name of this tower.
     */
    public abstract String getType();
    
    /**
     * Get the cost to upgrade this tower to the next level.
     * Exponential cost scaling: cost * (2.0 ^ level).
     *
     * @return Upgrade cost, or 0 if already at max level.
     */
    public int getUpgradeCost() {
        if (level >= 5) {
            return 0; // Already at max level
        }
        // Exponential cost: base_cost * (2.0^level) - more expensive upgrades
        return (int)(cost * Math.pow(2.0, level));
    }
    
    /**
     * Upgrade tower to next level.
     *
     * @param upgradeCost The cost paid for this upgrade.
     * @return True if upgrade successful.
     */
    public boolean upgrade(int upgradeCost) {
        if (level < 5) {
            level++;
            totalInvestment += upgradeCost; // Track total investment
            // Increase stats
            damage *= 1.2f;
            range *= 1.1f;
            fireRate *= 1.2f;
            return true;
        }
        return false;
    }
    
    /**
     * Check if enemy is outside tower's attack range.
     *
     * @param enemy The enemy to check.
     * @return True if the enemy is out of range.
     */
    protected boolean isOutOfRange(Enemy enemy) {
        if (enemy == null) return true;
        float dx = enemy.getPosition().x - position.x;
        float dy = enemy.getPosition().y - position.y;
        float distanceSquared = dx * dx + dy * dy;
        return distanceSquared > range * range;
    }
    
    /**
     * Check if tower is on cooldown and cannot fire yet.
     *
     * @return True if the tower is on cooldown.
     */
    protected boolean isOnCooldown() {
        long currentTime = System.currentTimeMillis();
        long fireInterval = (long)(1000 / fireRate);
        return currentTime - lastFireTime < fireInterval;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public PointF getPosition() { return position; }
    public int getLevel() { return level; }
    public float getDamage() { return damage; }
    public float getRange() { return range; }
    public float getFireRate() { return fireRate; }
    public int getCost() { return cost; }
    public Enemy getTarget() { return target; }
    public int getTotalInvestment() { return totalInvestment; }

    public void setTarget(Enemy target) { this.target = target; }
}
