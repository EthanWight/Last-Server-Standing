package edu.commonwealthu.lastserverstanding.model;

/**
 * Represents a temporary status effect applied to an enemy.
 * Effects include SLOW (reduces speed), STUN (stops movement), and BURN (damage over time).
 * Multiple effects can stack with different behaviors per type.
 *
 * @author Ethan Wight
 */
public class StatusEffect {

    /**
     * Enumeration of available status effect types.
     */
    public enum Type {
        /** Reduces enemy movement speed by a percentage. */
        SLOW,

        /** Completely stops enemy movement. */
        STUN,

        /** Deals damage over time to the enemy. */
        BURN
    }

    /** The type of this status effect. */
    private final Type type;

    /** The strength/intensity of this effect. */
    private final float strength;

    /** Remaining duration of this effect in seconds. */
    private float duration;

    /**
     * Constructs a new status effect with the specified parameters.
     *
     * @param type     The type of status effect
     * @param strength The strength of the effect
     * @param duration Duration in seconds before the effect expires
     */
    public StatusEffect(Type type, float strength, float duration) {
        this.type = type;
        this.strength = strength;
        this.duration = duration;
    }

    /**
     * Updates the effect's remaining duration.
     *
     * @param deltaTime Time elapsed since the last update, in seconds
     */
    public void update(float deltaTime) {
        duration -= deltaTime;
    }

    /**
     * Checks whether this effect has expired.
     *
     * @return true if the effect's duration has reached zero or below
     */
    public boolean isExpired() {
        return duration <= 0;
    }

    // ==================== Getters ====================

    /** @return The effect type */
    public Type getType() { return type; }

    /** @return The effect strength value */
    public float getStrength() { return strength; }
}
