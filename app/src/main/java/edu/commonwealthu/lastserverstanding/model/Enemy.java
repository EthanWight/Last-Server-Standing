package edu.commonwealthu.lastserverstanding.model;

import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all enemy types in the tower defense game.
 * Enemies follow a predefined path from spawn to data center, taking damage and applying status effects.
 * Subclasses must implement getType(), getColor(), and getIconResId().
 *
 * @author Ethan Wight
 */
public abstract class Enemy {

    /** Unique identifier for this enemy instance. */
    protected final String id;

    /** Current position of the enemy in game world coordinates. */
    protected PointF position;

    /** The complete path this enemy will follow from spawn to data center. */
    protected final List<PointF> path;

    /** Index of the current waypoint the enemy is moving toward. */
    protected int currentPathIndex;

    /** Current health points of the enemy. */
    protected float health;

    /** Maximum health points this enemy can have. */
    protected final float maxHealth;

    /** Base movement speed in game units per second. */
    protected final float speed;

    /** Currency reward granted to the player upon defeating this enemy. */
    protected final int reward;

    /** Damage dealt to the data center if this enemy reaches the end of the path. */
    protected final int damage;

    /** List of currently active status effects on this enemy. */
    protected final List<StatusEffect> statusEffects;

    /** Whether this enemy is still alive. */
    protected boolean isAlive;

    /**
     * Constructs a new enemy with the specified attributes.
     *
     * @param id        Unique identifier for this enemy instance
     * @param path      The waypoints this enemy will follow
     * @param maxHealth Maximum health points for this enemy
     * @param speed     Movement speed in game units per second
     * @param reward    Currency reward when defeated
     * @param damage    Damage dealt to data center if enemy reaches the end
     */
    public Enemy(String id, List<PointF> path, float maxHealth, float speed, int reward, int damage) {
        this.id = id;
        this.path = new ArrayList<>(path);
        this.currentPathIndex = 0;
        this.position = path.isEmpty() ? new PointF(0, 0) : new PointF(path.get(0).x, path.get(0).y);
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.speed = speed;
        this.reward = reward;
        this.damage = damage;
        this.statusEffects = new ArrayList<>();
        this.isAlive = true;
    }

    /**
     * Updates the enemy state for the current frame.
     * Applies status effects and moves toward the next waypoint.
     *
     * @param deltaTime Time elapsed since the last update, in seconds
     */
    public void update(float deltaTime) {
        // Process status effects first (may deal damage or affect movement)
        applyStatusEffects(deltaTime);

        // Move toward next waypoint if path is not complete
        if (currentPathIndex < path.size()) {
            move(deltaTime);
        }
    }

    /**
     * Returns the type identifier for this enemy.
     *
     * @return The type name of this enemy
     */
    public abstract String getType();

    /**
     * Returns the color used to render this enemy.
     *
     * @return Color value as an integer
     */
    public abstract int getColor();

    /**
     * Returns the drawable resource ID for this enemy's icon.
     *
     * @return Android drawable resource ID
     */
    public abstract int getIconResId();

    /**
     * Moves the enemy along its path toward the next waypoint.
     * Calculates effective speed considering status effects.
     *
     * @param deltaTime Time elapsed since the last update, in seconds
     */
    private void move(float deltaTime) {
        // Safety check - already at end of path
        if (currentPathIndex >= path.size()) return;

        // Get the current target waypoint
        PointF target = path.get(currentPathIndex);

        // Calculate direction vector to target
        float dx = target.x - position.x;
        float dy = target.y - position.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Apply status effects to calculate actual movement speed
        float effectiveSpeed = getEffectiveSpeed();
        float moveDistance = effectiveSpeed * deltaTime;

        // Check if we'll reach or overshoot the waypoint this frame
        if (moveDistance >= distance) {
            // Snap to waypoint and advance to next
            position.set(target.x, target.y);
            currentPathIndex++;
        } else {
            // Move proportionally toward target
            float ratio = moveDistance / distance;
            position.x += dx * ratio;
            position.y += dy * ratio;
        }
    }

    /**
     * Calculates the effective movement speed after applying all status effects.
     * Stun stops movement, slow effects stack multiplicatively with a 10% minimum speed floor.
     *
     * @return The effective speed after applying all status effects
     */
    private float getEffectiveSpeed() {
        float effectiveSpeed = speed;

        // Single pass through effects - check for stun first, then apply slows
        for (StatusEffect effect : statusEffects) {
            if (effect.getType() == StatusEffect.Type.STUN) {
                // Stun completely stops movement
                return 0;
            } else if (effect.getType() == StatusEffect.Type.SLOW) {
                // Slow reduces speed by effect strength percentage
                effectiveSpeed *= (1 - effect.getStrength());
            }
        }

        // Enforce minimum speed floor (10% of base) to prevent permanent immobilization
        return Math.max(effectiveSpeed, speed * 0.1f);
    }

    /**
     * Processes all active status effects and removes expired ones.
     * Applies burn damage and updates effect durations.
     *
     * @param deltaTime Time elapsed since the last update, in seconds
     */
    private void applyStatusEffects(float deltaTime) {
        java.util.Iterator<StatusEffect> iterator = statusEffects.iterator();

        while (iterator.hasNext()) {
            StatusEffect effect = iterator.next();

            // Apply burn damage over time
            if (effect.getType() == StatusEffect.Type.BURN) {
                float burnDamage = effect.getStrength() * deltaTime;
                takeDamage(burnDamage);
            }

            // Update effect duration
            effect.update(deltaTime);

            // Remove expired effects
            if (effect.isExpired()) {
                iterator.remove();
            }
        }
    }

    /**
     * Applies damage to this enemy, potentially killing it.
     *
     * @param damageAmount Amount of damage to apply
     */
    public void takeDamage(float damageAmount) {
        health -= damageAmount;

        // Check for death
        if (health <= 0) {
            health = 0;
            isAlive = false;
        }
    }

    /**
     * Adds a status effect to this enemy.
     *
     * @param effect The status effect to apply
     */
    public void addStatusEffect(StatusEffect effect) {
        statusEffects.add(effect);
    }

    /**
     * Returns the list of currently active status effects.
     *
     * @return List of active status effects
     */
    public List<StatusEffect> getStatusEffects() {
        return statusEffects;
    }

    /**
     * Checks whether this enemy has reached the end of its path.
     *
     * @return true if the enemy has completed its path
     */
    public boolean hasReachedEnd() {
        return currentPathIndex >= path.size();
    }

    /**
     * Calculates the current health as a percentage of maximum health.
     *
     * @return Health percentage from 0.0 to 1.0
     */
    public float getHealthPercentage() {
        return health / maxHealth;
    }

    // ==================== Getters ====================

    /** @return The enemy's position in game world coordinates */
    public PointF getPosition() { return position; }

    /** @return List of waypoints from spawn to data center */
    public List<PointF> getPath() { return path; }

    /** @return Current health points */
    public float getHealth() { return health; }

    /** @return Maximum health points */
    public float getMaxHealth() { return maxHealth; }

    /** @return Reward amount in game currency */
    public int getReward() { return reward; }

    /** @return Damage dealt to data center if enemy reaches the end */
    public int getDamage() { return damage; }

    /** @return true if the enemy has health remaining */
    public boolean isAlive() { return isAlive; }

    /** @return Current path index */
    public int getCurrentPathIndex() { return currentPathIndex; }

    // ==================== Setters for Save/Load ====================

    /** @param index The path index to restore */
    public void setCurrentPathIndex(int index) { this.currentPathIndex = index; }

    /** @param newPos The position to restore */
    public void setPosition(PointF newPos) { this.position = newPos; }
}
