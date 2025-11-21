package edu.commonwealthu.lastserverstanding.model;

import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all enemy types in the game.
 * Represents a rogue AI system attacking the data center.
 *
 * @author Ethan Wight
 */
public abstract class Enemy {
    // Unique identifier
    protected final String id;
    
    // Position and movement
    protected PointF position;
    protected final List<PointF> path;
    protected int currentPathIndex;
    
    // Stats
    protected float health;
    protected final float maxHealth;
    protected final float speed; // Units per second
    
    // Economy & Damage
    protected final int reward;
    protected final int damage; // Damage dealt to data center if reaches end
    
    // Status effects
    protected final List<StatusEffect> statusEffects;
    
    // State
    protected boolean isAlive;
    
    /**
     * Constructor for enemy base class.
     *
     * @param id Unique identifier for this enemy.
     * @param path The path this enemy will follow.
     * @param maxHealth Maximum health of this enemy.
     * @param speed Movement speed in units per second.
     * @param reward Reward for defeating this enemy.
     * @param damage Damage dealt if this enemy reaches the end.
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
     * Update enemy state each frame.
     *
     * @param deltaTime Time since last update in seconds.
     */
    public void update(float deltaTime) {
        // Apply status effects
        applyStatusEffects(deltaTime);
        
        // Move along path
        if (currentPathIndex < path.size()) {
            move(deltaTime);
        }
    }

    /**
     * Get the enemy type name.
     *
     * @return The type name of this enemy.
     */
    public abstract String getType();

    /**
     * Get the enemy's color for rendering.
     *
     * @return Color value (use android.graphics.Color constants).
     */
    public abstract int getColor();

    /**
     * Get the enemy's icon resource ID for rendering.
     *
     * @return Drawable resource ID.
     */
    public abstract int getIconResId();

    /**
     * Move enemy along its path.
     *
     * @param deltaTime Time since last update in seconds.
     */
    private void move(float deltaTime) {
        if (currentPathIndex >= path.size()) return;
        
        PointF target = path.get(currentPathIndex);
        float dx = target.x - position.x;
        float dy = target.y - position.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        // Calculate movement for this frame
        float effectiveSpeed = getEffectiveSpeed();
        float moveDistance = effectiveSpeed * deltaTime;
        
        if (moveDistance >= distance) {
            // Reached waypoint, move to next
            position.set(target.x, target.y);
            currentPathIndex++;
        } else {
            // Move toward waypoint
            float ratio = moveDistance / distance;
            position.x += dx * ratio;
            position.y += dy * ratio;
        }
    }
    
    /**
     * Calculate speed after applying status effects (optimized single-pass).
     *
     * @return The effective speed after applying all status effects.
     */
    private float getEffectiveSpeed() {
        float effectiveSpeed = speed;

        // Single loop to check all effects (stun overrides slow)
        for (StatusEffect effect : statusEffects) {
            if (effect.getType() == StatusEffect.Type.STUN) {
                return 0; // Completely immobilized
            } else if (effect.getType() == StatusEffect.Type.SLOW) {
                effectiveSpeed *= (1 - effect.getStrength());
            }
        }
        return Math.max(effectiveSpeed, speed * 0.1f); // Minimum 10% speed
    }
    
    /**
     * Apply and update status effects (optimized with iterator).
     *
     * @param deltaTime Time since last update in seconds.
     */
    private void applyStatusEffects(float deltaTime) {
        // Use iterator to remove expired effects without temporary list
        java.util.Iterator<StatusEffect> iterator = statusEffects.iterator();
        while (iterator.hasNext()) {
            StatusEffect effect = iterator.next();

            // Apply burn damage
            if (effect.getType() == StatusEffect.Type.BURN) {
                float burnDamage = effect.getStrength() * deltaTime;
                takeDamage(burnDamage);
            }

            effect.update(deltaTime);
            if (effect.isExpired()) {
                iterator.remove(); // Remove directly via iterator
            }
        }
    }
    
    /**
     * Take damage from a tower.
     *
     * @param damageAmount Amount of damage to take.
     */
    public void takeDamage(float damageAmount) {
        health -= damageAmount;
        if (health <= 0) {
            health = 0;
            isAlive = false;
        }
    }

    /**
     * Add a status effect to this enemy.
     *
     * @param effect The status effect to add.
     */
    public void addStatusEffect(StatusEffect effect) {
        statusEffects.add(effect);
    }

    /**
     * Get active status effects.
     *
     * @return A list of active status effects.
     */
    public List<StatusEffect> getStatusEffects() {
        return statusEffects;
    }
    
    /**
     * Check if enemy has reached the end of its path.
     *
     * @return True if the enemy has reached the end of its path.
     */
    public boolean hasReachedEnd() {
        return currentPathIndex >= path.size();
    }
    
    /**
     * Get health percentage.
     *
     * @return The health as a percentage (0.0 to 1.0).
     */
    public float getHealthPercentage() {
        return health / maxHealth;
    }
    
    // Getters
    @SuppressWarnings("unused") // Reserved for future debugging/networking
    public String getId() { return id; }
    public PointF getPosition() { return position; }
    public List<PointF> getPath() { return path; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public int getReward() { return reward; }
    public int getDamage() { return damage; }
    public boolean isAlive() { return isAlive; }
    public int getCurrentPathIndex() { return currentPathIndex; }

    // Setters for save/load functionality
    public void setCurrentPathIndex(int index) { this.currentPathIndex = index; }
    public void setPosition(PointF newPos) { this.position = newPos; }
}
