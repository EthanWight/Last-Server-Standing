package edu.commonwealthu.lastserverstanding.model;

import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all enemy types in the tower defense game.
 *
 * <p>This class represents a rogue AI system attacking the data center. Enemies follow
 * a predefined path from spawn point to the data center, taking damage from towers along
 * the way. Each enemy type has unique characteristics defined in subclasses.</p>
 *
 * <h2>Responsibilities:</h2>
 * <ul>
 *   <li>Path-following movement with configurable speed</li>
 *   <li>Health management and damage processing</li>
 *   <li>Status effect application and tracking (slow, stun, burn)</li>
 *   <li>Reward and damage value definitions for game economy</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Create a concrete enemy subclass
 * Enemy virus = new VirusEnemy("virus_001", pathPoints);
 *
 * // Update each frame
 * enemy.update(deltaTime);
 *
 * // Apply damage from towers
 * enemy.takeDamage(50.0f);
 *
 * // Check if enemy reached the data center
 * if (enemy.hasReachedEnd()) {
 *     dataCenter.takeDamage(enemy.getDamage());
 * }
 * }</pre>
 *
 * <h2>Subclasses must implement:</h2>
 * <ul>
 *   <li>{@link #getType()} - Returns the enemy type identifier</li>
 *   <li>{@link #getColor()} - Returns the rendering color</li>
 *   <li>{@link #getIconResId()} - Returns the drawable resource ID</li>
 * </ul>
 *
 * @author Ethan Wight
 * @see StatusEffect
 * @see Tower
 */
public abstract class Enemy {

    /**
     * Unique identifier for this enemy instance.
     * Used for tracking, debugging, and potential networking features.
     */
    protected final String id;

    /**
     * Current position of the enemy in game world coordinates.
     * Updated each frame as the enemy moves along its path.
     */
    protected PointF position;

    /**
     * The complete path this enemy will follow from spawn to data center.
     * Stored as a defensive copy to prevent external modification.
     */
    protected final List<PointF> path;

    /**
     * Index of the current waypoint the enemy is moving toward.
     * Increments as the enemy reaches each waypoint along the path.
     * When this equals path.size(), the enemy has reached the end.
     */
    protected int currentPathIndex;

    /**
     * Current health points of the enemy.
     * Decremented when taking damage. Enemy dies when this reaches zero.
     */
    protected float health;

    /**
     * Maximum health points this enemy can have.
     * Used for health bar rendering and percentage calculations.
     */
    protected final float maxHealth;

    /**
     * Base movement speed in game units per second.
     * May be modified by status effects (slow, stun) during gameplay.
     */
    protected final float speed;

    /**
     * Currency reward granted to the player upon defeating this enemy.
     * Used to purchase and upgrade towers.
     */
    protected final int reward;

    /**
     * Damage dealt to the data center if this enemy reaches the end of the path.
     * Higher values represent more dangerous enemy types.
     */
    protected final int damage;

    /**
     * List of currently active status effects on this enemy.
     * Effects are processed each frame and removed when expired.
     *
     * @see StatusEffect
     */
    protected final List<StatusEffect> statusEffects;

    /**
     * Whether this enemy is still alive.
     * Set to false when health reaches zero.
     */
    protected boolean isAlive;

    /**
     * Constructs a new enemy with the specified attributes.
     *
     * <p>The enemy is initialized at the first point of the provided path
     * with full health and no active status effects.</p>
     *
     * @param id        Unique identifier for this enemy instance
     * @param path      The waypoints this enemy will follow (defensively copied)
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
     *
     * <p>This method performs two main operations each frame:</p>
     * <ol>
     *   <li>Apply and update all active status effects (burn damage, duration countdown)</li>
     *   <li>Move toward the next waypoint if the path is not complete</li>
     * </ol>
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
     * <p>Used for display purposes, logging, and enemy-specific game logic.</p>
     *
     * @return The type name of this enemy (e.g., "Virus", "Trojan", "Worm")
     */
    public abstract String getType();

    /**
     * Returns the color used to render this enemy.
     *
     * <p>Each enemy type has a distinct color for visual identification.</p>
     *
     * @return Color value as an integer (use {@link android.graphics.Color} constants)
     */
    public abstract int getColor();

    /**
     * Returns the drawable resource ID for this enemy's icon.
     *
     * <p>Used for rendering the enemy sprite on the game canvas.</p>
     *
     * @return Android drawable resource ID (R.drawable.*)
     */
    public abstract int getIconResId();

    /**
     * Moves the enemy along its path toward the next waypoint.
     *
     * <p>Movement algorithm:</p>
     * <ol>
     *   <li>Calculate direction vector to current target waypoint</li>
     *   <li>Apply status effects to determine effective speed (slow/stun)</li>
     *   <li>Move by effective speed * deltaTime in the target direction</li>
     *   <li>If reached waypoint, snap to it and advance to next waypoint</li>
     * </ol>
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
     *
     * <p>Status effect priority:</p>
     * <ul>
     *   <li>STUN: Immediately returns 0 (complete movement stop)</li>
     *   <li>SLOW: Multiplies speed by (1 - strength), effects stack multiplicatively</li>
     * </ul>
     *
     * <p>A minimum speed floor of 10% base speed is enforced to prevent
     * enemies from being permanently immobilized by stacking slow effects.</p>
     *
     * @return The effective speed after applying all status effects, in units per second
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
     *
     * <p>Effect processing:</p>
     * <ul>
     *   <li>BURN: Deals damage equal to (strength * deltaTime) each frame</li>
     *   <li>All effects: Duration countdown and expiration check</li>
     * </ul>
     *
     * <p>Uses iterator for safe removal during iteration.</p>
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
     * <p>If damage reduces health to zero or below, the enemy is marked
     * as dead and will be removed from the game on the next cleanup pass.</p>
     *
     * @param damageAmount Amount of damage to apply (positive value)
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
     * <p>Multiple effects of the same type can stack. The effect will
     * be processed each frame until its duration expires.</p>
     *
     * @param effect The status effect to apply
     * @see StatusEffect
     */
    public void addStatusEffect(StatusEffect effect) {
        statusEffects.add(effect);
    }

    /**
     * Returns the list of currently active status effects.
     *
     * <p>Note: Returns the actual list, not a copy. Modifications will
     * affect the enemy's status effects.</p>
     *
     * @return List of active status effects (may be empty, never null)
     */
    public List<StatusEffect> getStatusEffects() {
        return statusEffects;
    }

    /**
     * Checks whether this enemy has reached the end of its path.
     *
     * <p>When true, the enemy should deal damage to the data center
     * and be removed from the game.</p>
     *
     * @return {@code true} if the enemy has completed its path
     */
    public boolean hasReachedEnd() {
        return currentPathIndex >= path.size();
    }

    /**
     * Calculates the current health as a percentage of maximum health.
     *
     * <p>Used for rendering health bars above enemies.</p>
     *
     * @return Health percentage as a value from 0.0 (dead) to 1.0 (full health)
     */
    public float getHealthPercentage() {
        return health / maxHealth;
    }

    // ==================== Getters ====================

    /**
     * Returns the unique identifier for this enemy.
     *
     * @return The enemy's unique ID string
     */
    @SuppressWarnings("unused") // Reserved for future debugging/networking
    public String getId() { return id; }

    /**
     * Returns the current position of this enemy.
     *
     * @return The enemy's position in game world coordinates
     */
    public PointF getPosition() { return position; }

    /**
     * Returns the path this enemy is following.
     *
     * @return List of waypoints from spawn to data center
     */
    public List<PointF> getPath() { return path; }

    /**
     * Returns the current health of this enemy.
     *
     * @return Current health points
     */
    public float getHealth() { return health; }

    /**
     * Returns the maximum health of this enemy.
     *
     * @return Maximum health points
     */
    public float getMaxHealth() { return maxHealth; }

    /**
     * Returns the currency reward for defeating this enemy.
     *
     * @return Reward amount in game currency
     */
    public int getReward() { return reward; }

    /**
     * Returns the damage this enemy deals if it reaches the data center.
     *
     * @return Damage amount
     */
    public int getDamage() { return damage; }

    /**
     * Checks whether this enemy is still alive.
     *
     * @return {@code true} if the enemy has health remaining
     */
    public boolean isAlive() { return isAlive; }

    /**
     * Returns the index of the current waypoint target.
     *
     * @return Current path index (0-based)
     */
    public int getCurrentPathIndex() { return currentPathIndex; }

    // ==================== Setters for Save/Load ====================

    /**
     * Sets the current path index for save/load functionality.
     *
     * @param index The path index to restore
     */
    public void setCurrentPathIndex(int index) { this.currentPathIndex = index; }

    /**
     * Sets the enemy's position for save/load functionality.
     *
     * @param newPos The position to restore
     */
    public void setPosition(PointF newPos) { this.position = newPos; }
}
