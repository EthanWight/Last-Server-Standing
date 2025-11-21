package edu.commonwealthu.lastserverstanding.model;

import android.graphics.PointF;

/**
 * Represents a projectile fired from a tower toward an enemy target.
 *
 * <p>Projectiles are homing missiles that track and follow their target enemy
 * until impact. They can optionally apply status effects on hit (burn, slow, stun).</p>
 *
 * <h2>Behavior:</h2>
 * <ul>
 *   <li>Continuously updates velocity to track moving targets</li>
 *   <li>Deals damage and applies status effects on impact</li>
 *   <li>Self-destructs if target dies before impact</li>
 *   <li>Has maximum travel distance to prevent infinite pursuit</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Create projectile from tower position toward target
 * StatusEffect burn = new StatusEffect(StatusEffect.Type.BURN, 10f, 3f);
 * Projectile proj = new Projectile(tower.getPosition(), enemy, 50f, 800f, burn);
 *
 * // Update each frame
 * proj.update(deltaTime);
 *
 * // Check if projectile should be removed
 * if (proj.hasHit()) {
 *     projectiles.remove(proj);
 * }
 * }</pre>
 *
 * <h2>Hit Detection:</h2>
 * <p>Uses squared distance comparison with a 25-pixel hit radius for efficient
 * collision detection without expensive sqrt operations.</p>
 *
 * @author Ethan Wight
 * @see Tower
 * @see Enemy
 * @see StatusEffect
 */
public class Projectile {

    /**
     * Current position of the projectile in game world coordinates.
     * Updated each frame based on velocity.
     */
    private final PointF position;

    /**
     * Current velocity vector (direction and speed combined).
     * Recalculated each frame to track moving targets.
     * Reused to avoid object allocation during updates.
     */
    private final PointF velocity;

    /**
     * The enemy this projectile is tracking.
     * Projectile continuously adjusts trajectory toward this target.
     */
    private final Enemy target;

    /**
     * Damage dealt to the target on impact.
     */
    private final float damage;

    /**
     * Movement speed in game units per second.
     */
    private final float speed;

    /**
     * Whether this projectile has hit its target or should be removed.
     * Set to true when: target hit, target died, or max distance exceeded.
     */
    private boolean hasHit;

    /**
     * Total distance traveled by this projectile.
     * Used to enforce maximum travel distance limit.
     */
    private float distanceTraveled;

    /**
     * Optional status effect to apply to the target on hit.
     * May be null for projectiles without special effects.
     *
     * @see StatusEffect
     */
    private final StatusEffect statusEffect;

    /**
     * Maximum distance a projectile can travel before being removed.
     * Prevents projectiles from pursuing targets indefinitely.
     * Value: 2000 game units.
     */
    private static final float MAX_TRAVEL_DISTANCE = 2000f;

    /**
     * Squared hit radius for collision detection.
     * Using squared value avoids expensive sqrt operations.
     * Value: 625 (25 pixels squared, meaning 25-pixel hit radius).
     */
    private static final float HIT_RADIUS_SQUARED = 625f;

    /**
     * Constructs a new projectile targeting an enemy.
     *
     * <p>The projectile is initialized at the start position with its velocity
     * already calculated toward the target. The velocity PointF is reused
     * throughout the projectile's lifetime to minimize object allocation.</p>
     *
     * @param start        Starting position (typically the tower's position)
     * @param target       The enemy to track and hit
     * @param damage       Damage to deal on impact
     * @param speed        Movement speed in game units per second
     * @param statusEffect Optional status effect to apply on hit (may be null)
     */
    public Projectile(PointF start, Enemy target, float damage, float speed, StatusEffect statusEffect) {
        this.position = new PointF(start.x, start.y);
        this.velocity = new PointF(0, 0); // Initialize velocity object once for reuse
        this.target = target;
        this.damage = damage;
        this.speed = speed;
        this.statusEffect = statusEffect;
        this.hasHit = false;
        this.distanceTraveled = 0f;

        // Calculate initial velocity toward target
        updateVelocity();
    }

    /**
     * Updates the projectile state for the current frame.
     *
     * <p>Update sequence:</p>
     * <ol>
     *   <li>Check if already hit or target is dead - mark for removal if so</li>
     *   <li>Recalculate velocity to track moving target (homing behavior)</li>
     *   <li>Move position based on velocity and deltaTime</li>
     *   <li>Check max travel distance - remove if exceeded</li>
     *   <li>Check collision with target - apply damage and effects if hit</li>
     * </ol>
     *
     * @param deltaTime Time elapsed since the last update, in seconds
     */
    public void update(float deltaTime) {
        // Early exit if already resolved
        if (hasHit || !target.isAlive()) {
            hasHit = true;
            return;
        }

        // Recalculate velocity to track moving target (homing behavior)
        updateVelocity();

        // Calculate movement distance this frame
        float movementDistance = speed * deltaTime;
        distanceTraveled += movementDistance;

        // Update position based on velocity
        position.x += velocity.x * deltaTime;
        position.y += velocity.y * deltaTime;

        // Check if projectile has traveled too far (failsafe removal)
        if (distanceTraveled > MAX_TRAVEL_DISTANCE) {
            hasHit = true;
            return;
        }

        // Double-check target is still alive (may have died from another projectile)
        if (!target.isAlive()) {
            hasHit = true;
            return;
        }

        // Collision detection using squared distance (avoids sqrt)
        float dx = position.x - target.getPosition().x;
        float dy = position.y - target.getPosition().y;
        float distanceSquared = dx * dx + dy * dy;

        // Check if within hit radius
        if (distanceSquared < HIT_RADIUS_SQUARED) {
            hit();
        }
    }

    /**
     * Recalculates velocity vector to point toward the current target position.
     *
     * <p>This method implements the homing behavior by continuously adjusting
     * the projectile's direction. The velocity PointF object is reused to
     * avoid memory allocation during the game loop.</p>
     *
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Calculate direction vector to target</li>
     *   <li>Normalize to unit vector (divide by magnitude)</li>
     *   <li>Scale by speed to get velocity</li>
     * </ol>
     */
    private void updateVelocity() {
        PointF targetPos = target.getPosition();

        // Calculate direction vector
        float dx = targetPos.x - position.x;
        float dy = targetPos.y - position.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Normalize and scale by speed (avoid division by zero)
        if (distance > 0) {
            velocity.x = (dx / distance) * speed;
            velocity.y = (dy / distance) * speed;
        } else {
            // Already at target position
            velocity.x = 0;
            velocity.y = 0;
        }
    }

    /**
     * Handles the projectile hitting its target.
     *
     * <p>Impact effects:</p>
     * <ol>
     *   <li>Deal damage to the target enemy</li>
     *   <li>Apply status effect if present (burn, slow, stun)</li>
     *   <li>Mark projectile for removal</li>
     * </ol>
     *
     * <p>Guard conditions prevent double-hits and hitting dead enemies.</p>
     */
    private void hit() {
        // Guard against double-hit and hitting dead enemies
        if (!hasHit && target.isAlive()) {
            // Apply damage
            target.takeDamage(damage);

            // Apply status effect if present
            if (statusEffect != null) {
                target.addStatusEffect(statusEffect);
            }

            // Mark for removal
            hasHit = true;
        }
    }

    // ==================== Getters ====================

    /**
     * Returns the current position of this projectile.
     *
     * @return The projectile's position in game world coordinates
     */
    public PointF getPosition() { return position; }

    /**
     * Checks whether this projectile has hit its target or should be removed.
     *
     * <p>Returns true when:</p>
     * <ul>
     *   <li>Projectile successfully hit the target</li>
     *   <li>Target died before impact (from another source)</li>
     *   <li>Projectile exceeded maximum travel distance</li>
     * </ul>
     *
     * @return {@code true} if the projectile should be removed from the game
     */
    public boolean hasHit() { return hasHit; }

    /**
     * Returns the status effect this projectile applies on hit.
     *
     * @return The status effect, or {@code null} if none
     */
    public StatusEffect getStatusEffect() { return statusEffect; }
}
