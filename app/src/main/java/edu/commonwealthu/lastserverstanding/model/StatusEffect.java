package edu.commonwealthu.lastserverstanding.model;

/**
 * Represents a temporary status effect applied to an enemy.
 *
 * <p>Status effects modify enemy behavior for a limited duration. They are
 * typically applied by projectiles on impact and processed each frame by
 * the enemy's update loop.</p>
 *
 * <h2>Effect Types:</h2>
 * <ul>
 *   <li><b>SLOW</b> - Reduces enemy movement speed by a percentage</li>
 *   <li><b>STUN</b> - Completely stops enemy movement</li>
 *   <li><b>BURN</b> - Deals damage over time</li>
 * </ul>
 *
 * <h2>Stacking Behavior:</h2>
 * <p>Multiple effects of the same type can be applied to an enemy:</p>
 * <ul>
 *   <li>SLOW effects stack multiplicatively (50% + 50% = 75% reduction)</li>
 *   <li>STUN effects don't stack (one stun is enough for full stop)</li>
 *   <li>BURN effects stack additively (each burn deals its own damage)</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Create a 30% slow for 2 seconds
 * StatusEffect slow = new StatusEffect(StatusEffect.Type.SLOW, 0.3f, 2.0f);
 *
 * // Create a burn dealing 10 damage per second for 3 seconds
 * StatusEffect burn = new StatusEffect(StatusEffect.Type.BURN, 10.0f, 3.0f);
 *
 * // Apply to enemy
 * enemy.addStatusEffect(slow);
 *
 * // Update each frame (done by Enemy.update())
 * effect.update(deltaTime);
 *
 * // Check if expired
 * if (effect.isExpired()) {
 *     // Remove from enemy's effect list
 * }
 * }</pre>
 *
 * @author Ethan Wight
 * @see Enemy#addStatusEffect(StatusEffect)
 * @see Enemy#applyStatusEffects(float)
 */
public class StatusEffect {

    /**
     * Enumeration of available status effect types.
     *
     * <p>Each type has different behavior when applied to enemies:</p>
     * <ul>
     *   <li>{@link #SLOW} - Movement speed reduction (percentage-based)</li>
     *   <li>{@link #STUN} - Complete movement stop</li>
     *   <li>{@link #BURN} - Damage over time</li>
     * </ul>
     */
    public enum Type {
        /**
         * Reduces enemy movement speed by a percentage.
         * Strength value represents the percentage reduction (0.0 to 1.0).
         * Example: strength=0.5 means 50% slower movement.
         */
        SLOW,

        /**
         * Completely stops enemy movement.
         * Strength value is typically 1.0 (full effect).
         * Takes priority over SLOW effects.
         */
        STUN,

        /**
         * Deals damage over time to the enemy.
         * Strength value represents damage per second.
         * Example: strength=10.0 deals 10 damage per second.
         */
        BURN
    }

    /**
     * The type of this status effect.
     * Determines how the effect modifies enemy behavior.
     */
    private final Type type;

    /**
     * The strength/intensity of this effect.
     *
     * <p>Interpretation depends on effect type:</p>
     * <ul>
     *   <li>SLOW: Percentage reduction (0.0 to 1.0, where 0.5 = 50% slower)</li>
     *   <li>STUN: Typically 1.0 (full effect)</li>
     *   <li>BURN: Damage per second</li>
     * </ul>
     */
    private final float strength;

    /**
     * Remaining duration of this effect in seconds.
     * Decremented each frame. Effect expires when this reaches zero.
     */
    private float duration;

    /**
     * Constructs a new status effect with the specified parameters.
     *
     * @param type     The type of status effect (SLOW, STUN, or BURN)
     * @param strength The strength of the effect:
     *                 <ul>
     *                   <li>SLOW: 0.0-1.0 percentage (0.3 = 30% slow)</li>
     *                   <li>STUN: typically 1.0</li>
     *                   <li>BURN: damage per second</li>
     *                 </ul>
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
     * <p>Called each frame by the enemy's update loop. When duration
     * reaches zero, {@link #isExpired()} will return true and the
     * effect should be removed.</p>
     *
     * @param deltaTime Time elapsed since the last update, in seconds
     */
    public void update(float deltaTime) {
        duration -= deltaTime;
    }

    /**
     * Checks whether this effect has expired.
     *
     * <p>Expired effects should be removed from the enemy's status
     * effect list to stop their influence on enemy behavior.</p>
     *
     * @return {@code true} if the effect's duration has reached zero or below
     */
    public boolean isExpired() {
        return duration <= 0;
    }

    // ==================== Getters ====================

    /**
     * Returns the type of this status effect.
     *
     * @return The effect type (SLOW, STUN, or BURN)
     */
    public Type getType() { return type; }

    /**
     * Returns the strength/intensity of this effect.
     *
     * <p>Interpretation depends on {@link #getType()}:</p>
     * <ul>
     *   <li>SLOW: Percentage reduction (0.3 = 30% speed reduction)</li>
     *   <li>STUN: Full effect indicator (typically 1.0)</li>
     *   <li>BURN: Damage per second</li>
     * </ul>
     *
     * @return The effect strength value
     */
    public float getStrength() { return strength; }
}
