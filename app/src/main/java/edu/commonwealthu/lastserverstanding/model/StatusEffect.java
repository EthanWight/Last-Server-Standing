package edu.commonwealthu.lastserverstanding.model;

/**
 * Represents a temporary status effect applied to an enemy.
 *
 * @author Ethan Wight
 */
public class StatusEffect {
    
    public enum Type {
        SLOW,
        STUN,
        BURN
    }
    
    private final Type type;
    private final float strength; // 0.0 to 1.0 (percentage effect)
    private float duration; // Seconds remaining
    
    /**
     * Constructor for a status effect.
     *
     * @param type The type of status effect.
     * @param strength The strength of the effect (0.0 to 1.0).
     * @param duration Duration in seconds.
     */
    public StatusEffect(Type type, float strength, float duration) {
        this.type = type;
        this.strength = strength;
        this.duration = duration;
    }
    
    /**
     * Update the effect's duration.
     *
     * @param deltaTime Time since last update in seconds.
     */
    public void update(float deltaTime) {
        duration -= deltaTime;
    }
    
    /**
     * Check if effect has expired.
     *
     * @return True if the effect duration has expired.
     */
    public boolean isExpired() {
        return duration <= 0;
    }
    
    // Getters
    public Type getType() { return type; }
    public float getStrength() { return strength; }
}
