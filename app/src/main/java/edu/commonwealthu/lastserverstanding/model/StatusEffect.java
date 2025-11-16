package edu.commonwealthu.lastserverstanding.model;

/**
 * Represents a temporary status effect applied to an enemy
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
    
    public StatusEffect(Type type, float strength, float duration) {
        this.type = type;
        this.strength = strength;
        this.duration = duration;
    }
    
    /**
     * Update the effect's duration
     */
    public void update(float deltaTime) {
        duration -= deltaTime;
    }
    
    /**
     * Check if effect has expired
     */
    public boolean isExpired() {
        return duration <= 0;
    }
    
    // Getters
    public Type getType() { return type; }
    public float getStrength() { return strength; }
}
