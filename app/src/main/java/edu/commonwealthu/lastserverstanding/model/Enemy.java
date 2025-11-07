package edu.commonwealthu.lastserverstanding.model;

import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all enemy types in the game.
 * Represents a rogue AI system attacking the data center.
 */
public abstract class Enemy {
    // Unique identifier
    protected String id;
    
    // Position and movement
    protected PointF position;
    protected List<PointF> path;
    protected int currentPathIndex;
    
    // Stats
    protected float health;
    protected float maxHealth;
    protected float speed; // Units per second
    
    // Economy & Damage
    protected int reward;
    protected int damage; // Damage dealt to data center if reaches end
    
    // Status effects
    protected List<StatusEffect> statusEffects;
    
    // State
    protected boolean isAlive;
    
    /**
     * Constructor for enemy base class
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
     * Update enemy state each frame
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        // Apply status effects
        applyStatusEffects(deltaTime);
        
        // Move along path
        if (currentPathIndex < path.size()) {
            move(deltaTime);
        }
        
        // Custom update for specific enemy type
        updateSpecific(deltaTime);
    }
    
    /**
     * Type-specific update logic (override in subclasses)
     */
    protected abstract void updateSpecific(float deltaTime);
    
    /**
     * Get the enemy type name
     */
    public abstract String getType();
    
    /**
     * Special ability triggered on certain conditions
     */
    public abstract void triggerAbility();
    
    /**
     * Move enemy along its path
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
     * Calculate speed after applying status effects
     */
    private float getEffectiveSpeed() {
        float effectiveSpeed = speed;
        for (StatusEffect effect : statusEffects) {
            if (effect.getType() == StatusEffect.Type.SLOW) {
                effectiveSpeed *= (1 - effect.getStrength());
            }
        }
        return Math.max(effectiveSpeed, speed * 0.1f); // Minimum 10% speed
    }
    
    /**
     * Apply and update status effects
     */
    private void applyStatusEffects(float deltaTime) {
        List<StatusEffect> expiredEffects = new ArrayList<>();
        for (StatusEffect effect : statusEffects) {
            effect.update(deltaTime);
            if (effect.isExpired()) {
                expiredEffects.add(effect);
            }
        }
        statusEffects.removeAll(expiredEffects);
    }
    
    /**
     * Take damage from a tower
     * @param damageAmount Amount of damage to take
     * @return true if enemy was killed
     */
    public boolean takeDamage(float damageAmount) {
        health -= damageAmount;
        if (health <= 0) {
            health = 0;
            isAlive = false;
            onDeath();
            return true;
        }
        return false;
    }
    
    /**
     * Called when enemy dies
     */
    protected void onDeath() {
        // Can be overridden for special death effects
    }
    
    /**
     * Add a status effect to this enemy
     */
    public void addStatusEffect(StatusEffect effect) {
        statusEffects.add(effect);
    }
    
    /**
     * Check if enemy has reached the end of its path
     */
    public boolean hasReachedEnd() {
        return currentPathIndex >= path.size();
    }
    
    /**
     * Get health percentage
     */
    public float getHealthPercentage() {
        return health / maxHealth;
    }
    
    // Getters
    public String getId() { return id; }
    public PointF getPosition() { return position; }
    public List<PointF> getPath() { return path; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public float getSpeed() { return speed; }
    public int getReward() { return reward; }
    public int getDamage() { return damage; }
    public boolean isAlive() { return isAlive; }
    public List<StatusEffect> getStatusEffects() { return statusEffects; }
}
