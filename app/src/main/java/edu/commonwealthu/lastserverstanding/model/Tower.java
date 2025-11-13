package edu.commonwealthu.lastserverstanding.model;

import android.graphics.PointF;

/**
 * Abstract base class for all tower types in the game.
 * Represents a defensive structure that attacks enemies within range.
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

    // Current target
    protected Enemy target;

    // Status
    protected boolean isCorrupted;

    // Timing for attack rate
    protected long lastFireTime;

    // Spacing constants
    protected static final float TOWER_RADIUS = 32f; // Half of grid size (64/2)
    protected static final float MIN_TOWER_SPACING = 64f; // One grid cell spacing
    
    /**
     * Constructor for tower base class
     */
    public Tower(String id, PointF position, int level, float damage, float range, float fireRate, int cost) {
        this.id = id;
        this.position = position;
        this.level = level;
        this.damage = damage;
        this.range = range;
        this.fireRate = fireRate;
        this.cost = cost;
        this.isCorrupted = false;
        this.lastFireTime = 0;
    }
    
    /**
     * Update tower state each frame
     * @param deltaTime Time since last update in seconds
     */
    public abstract void update(float deltaTime);
    
    /**
     * Fire at the current target if possible
     * @return Projectile if fired, null otherwise
     */
    public abstract Projectile fire();
    
    /**
     * Find and acquire a target from the list of enemies
     * @param enemies List of all active enemies
     */
    public abstract void acquireTarget(java.util.List<Enemy> enemies);
    
    /**
     * Get the tower type name
     */
    public abstract String getType();
    
    /**
     * Get the cost to upgrade this tower to the next level
     * Exponential cost scaling: cost * (1.5 ^ level)
     * @return upgrade cost, or 0 if already at max level
     */
    public int getUpgradeCost() {
        if (level >= 5) {
            return 0; // Already at max level
        }
        // Exponential cost: base_cost * (1.5^level)
        return (int)(cost * Math.pow(1.5, level));
    }
    
    /**
     * Upgrade tower to next level
     * @return true if upgrade successful
     */
    public boolean upgrade() {
        if (level < 5) {
            level++;
            // Increase stats
            damage *= 2.0f;
            range *= 1.25f;
            fireRate *= 1.25f;
            return true;
        }
        return false;
    }
    
    /**
     * Check if enemy is within range
     */
    protected boolean isInRange(Enemy enemy) {
        if (enemy == null) return false;
        float dx = enemy.getPosition().x - position.x;
        float dy = enemy.getPosition().y - position.y;
        float distanceSquared = dx * dx + dy * dy;
        return distanceSquared <= range * range;
    }
    
    /**
     * Check if enough time has passed to fire again
     */
    protected boolean canFire() {
        long currentTime = System.currentTimeMillis();
        long fireInterval = (long)(1000 / fireRate);
        return currentTime - lastFireTime >= fireInterval;
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
    public boolean isCorrupted() { return isCorrupted; }

    public void setTarget(Enemy target) { this.target = target; }
    public void setCorrupted(boolean corrupted) { this.isCorrupted = corrupted; }

    // Static getters for constants
    public static float getTowerRadius() { return TOWER_RADIUS; }
    public static float getMinTowerSpacing() { return MIN_TOWER_SPACING; }
}
