package edu.commonwealthu.lastserverstanding.model;

import android.graphics.PointF;

/**
 * Represents a homing projectile fired from a tower toward an enemy target.
 * Tracks moving targets, deals damage on impact, and optionally applies status effects.
 * Uses squared distance collision detection for efficiency.
 *
 * @author Ethan Wight
 */
public class Projectile {

    /** Current position of the projectile in game world coordinates. */
    private final PointF position;

    /** Current velocity vector recalculated each frame to track moving targets. */
    private final PointF velocity;

    /** The enemy this projectile is tracking. */
    private final Enemy target;

    /** Damage dealt to the target on impact. */
    private final float damage;

    /** Movement speed in game units per second. */
    private final float speed;

    /** Whether this projectile has hit its target or should be removed. */
    private boolean hasHit;

    /** Total distance traveled by this projectile. */
    private float distanceTraveled;

    /** Optional status effect to apply to the target on hit. */
    private final StatusEffect statusEffect;

    /** Maximum distance a projectile can travel before being removed. */
    private static final float MAX_TRAVEL_DISTANCE = 2000f;

    /** Squared hit radius for collision detection. */
    private static final float HIT_RADIUS_SQUARED = 625f;

    /**
     * Constructs a new projectile targeting an enemy.
     *
     * @param start        Starting position
     * @param target       The enemy to track and hit
     * @param damage       Damage to deal on impact
     * @param speed        Movement speed in game units per second
     * @param statusEffect Optional status effect to apply on hit
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
     * Tracks target, moves position, checks collision, and enforces max travel distance.
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
     * Normalizes direction and scales by speed for homing behavior.
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
     * Deals damage, applies status effect if present, and marks for removal.
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

    /** @return The projectile's position in game world coordinates */
    public PointF getPosition() { return position; }

    /** @return true if the projectile should be removed from the game */
    public boolean hasHit() { return hasHit; }

    /** @return The status effect, or null if none */
    public StatusEffect getStatusEffect() { return statusEffect; }
}
