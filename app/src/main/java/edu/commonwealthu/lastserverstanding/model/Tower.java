package edu.commonwealthu.lastserverstanding.model;

import android.graphics.PointF;

/**
 * Abstract base class for all tower types in the tower defense game.
 * Towers are defensive structures that automatically target and attack enemies within range.
 * Each tower type has unique characteristics (damage, range, fire rate, special effects).
 * Supports upgrades from level 1-5 with stat scaling: damage +20%, range +10%, fire rate +20% per level.
 * Upgrade cost: baseCost * 2^level (exponential scaling).
 *
 * @author Ethan Wight
 */
public abstract class Tower {

    /** Tower position on the game grid (stationary once placed). */
    protected final PointF position;

    /** Current upgrade level (1-5). */
    protected int level;

    /** Damage per projectile hit (scales by 1.2x per level). */
    protected float damage;

    /** Attack range in game units (scales by 1.1x per level). */
    protected float range;

    /** Attacks per second (scales by 1.2x per level). */
    protected float fireRate;

    /** Base tower build cost (used for upgrade cost calculations). */
    protected final int cost;

    /** Total resources invested (base cost + upgrades, used for sell-back). */
    protected int totalInvestment;

    /** Current target enemy (null if no valid target in range). */
    protected Enemy target;

    /** Timestamp of last projectile fired in milliseconds (for cooldown). */
    protected long lastFireTime;

    /**
     * Constructs a new tower with specified attributes.
     *
     * @param position Tower position on game grid
     * @param level Initial upgrade level
     * @param damage Base damage per hit
     * @param range Attack range in game units
     * @param fireRate Attacks per second
     * @param cost Base build cost
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
     * Updates tower state each frame (target validation, cooldowns, etc).
     */
    public abstract void update();

    /**
     * Fires a projectile at current target if ready.
     *
     * @return New projectile if fired, null if unable (no target, on cooldown, out of range)
     */
    public abstract Projectile fire();

    /**
     * Returns tower type identifier (e.g., "Firewall", "Honeypot", "Jammer").
     *
     * @return Tower type name
     */
    public abstract String getType();

    /**
     * Calculates upgrade cost for next level (formula: baseCost * 2^level).
     *
     * @return Upgrade cost, or 0 if already at max level (5)
     */
    public int getUpgradeCost() {
        if (level >= 5) {
            return 0;
        }
        return (int)(cost * Math.pow(2.0, level));
    }

    /**
     * Upgrades tower to next level (damage +20%, range +10%, fire rate +20%).
     *
     * @param upgradeCost Cost paid for upgrade (added to total investment)
     * @return true if upgraded, false if already at max level
     */
    public boolean upgrade(int upgradeCost) {
        if (level < 5) {
            level++;
            totalInvestment += upgradeCost;
            damage *= 1.2f;
            range *= 1.1f;
            fireRate *= 1.2f;
            return true;
        }
        return false;
    }

    /**
     * Checks if enemy is outside attack range (uses squared distance for performance).
     *
     * @param enemy Enemy to check
     * @return true if enemy is null or out of range
     */
    protected boolean isOutOfRange(Enemy enemy) {
        if (enemy == null) return true;

        float dx = enemy.getPosition().x - position.x;
        float dy = enemy.getPosition().y - position.y;
        float distanceSquared = dx * dx + dy * dy;

        return distanceSquared > range * range;
    }

    /**
     * Checks if tower is on cooldown (cooldown = 1000ms / fireRate).
     *
     * @return true if cannot fire yet, false if ready
     */
    protected boolean isOnCooldown() {
        long currentTime = System.currentTimeMillis();
        long fireInterval = (long)(1000 / fireRate);
        return currentTime - lastFireTime < fireInterval;
    }

    /** @return Tower position on grid. */
    public PointF getPosition() { return position; }

    /** @return Current upgrade level (1-5). */
    public int getLevel() { return level; }

    /** @return Damage per projectile. */
    public float getDamage() { return damage; }

    /** @return Attack range in game units. */
    public float getRange() { return range; }

    /** @return Attacks per second. */
    public float getFireRate() { return fireRate; }

    /** @return Base build cost. */
    public int getCost() { return cost; }

    /** @return Current target enemy (null if none). */
    public Enemy getTarget() { return target; }

    /** @return Total resources invested (base cost + upgrades). */
    public int getTotalInvestment() { return totalInvestment; }

    /**
     * Sets target enemy for this tower.
     *
     * @param target Enemy to target (null to clear)
     */
    public void setTarget(Enemy target) { this.target = target; }
}
