package edu.commonwealthu.lastserverstanding.model;

import android.graphics.PointF;

/**
 * Represents a projectile fired from a tower toward an enemy
 */
public class Projectile {
    private final String id;
    private final PointF position;
    private final PointF velocity;
    private final Enemy target;
    private final float damage;
    private final float speed;
    private boolean hasHit;
    private float distanceTraveled;
    private final StatusEffect statusEffect; // Optional status effect to apply on hit
    private static final float MAX_TRAVEL_DISTANCE = 2000f; // Remove if travels too far
    private static final float HIT_RADIUS_SQUARED = 625f; // 25 pixel hit radius (25^2)

    public Projectile(String id, PointF start, Enemy target, float damage, float speed) {
        this(id, start, target, damage, speed, null);
    }

    public Projectile(String id, PointF start, Enemy target, float damage, float speed, StatusEffect statusEffect) {
        this.id = id;
        this.position = new PointF(start.x, start.y);
        this.velocity = new PointF(0, 0); // Initialize velocity once
        this.target = target;
        this.damage = damage;
        this.speed = speed;
        this.statusEffect = statusEffect;
        this.hasHit = false;
        this.distanceTraveled = 0f;
        updateVelocity();
    }
    
    /**
     * Update projectile position
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        if (hasHit || !target.isAlive()) {
            hasHit = true; // Mark as hit so it gets removed
            return;
        }

        // Update velocity to track target
        updateVelocity();

        // Calculate movement distance this frame
        float movementDistance = speed * deltaTime;
        distanceTraveled += movementDistance;

        // Move projectile
        position.x += velocity.x * deltaTime;
        position.y += velocity.y * deltaTime;

        // Check if projectile traveled too far (missed target)
        if (distanceTraveled > MAX_TRAVEL_DISTANCE) {
            hasHit = true; // Mark as hit so it gets removed
            return;
        }

        // Check for collision with target
        if (!target.isAlive()) {
            hasHit = true; // Mark as hit so it gets removed
            return;
        }

        float dx = position.x - target.getPosition().x;
        float dy = position.y - target.getPosition().y;
        float distanceSquared = dx * dx + dy * dy;

        if (distanceSquared < HIT_RADIUS_SQUARED) { // Hit threshold (25 pixel radius)
            hit();
        }
    }
    
    /**
     * Calculate velocity toward target (reuses velocity PointF to avoid allocation)
     */
    private void updateVelocity() {
        PointF targetPos = target.getPosition();
        float dx = targetPos.x - position.x;
        float dy = targetPos.y - position.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            // Reuse velocity object instead of creating new one
            velocity.x = (dx / distance) * speed;
            velocity.y = (dy / distance) * speed;
        } else {
            velocity.x = 0;
            velocity.y = 0;
        }
    }
    
    /**
     * Handle projectile hitting target
     */
    private void hit() {
        if (!hasHit && target.isAlive()) {
            target.takeDamage(damage);

            // Apply status effect if present
            if (statusEffect != null) {
                target.addStatusEffect(statusEffect);
            }

            hasHit = true;
        }
    }
    
    // Getters
    public String getId() { return id; }
    public PointF getPosition() { return position; }
    public float getDamage() { return damage; }
    public boolean hasHit() { return hasHit; }
}
