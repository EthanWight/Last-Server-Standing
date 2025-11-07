package edu.commonwealthu.lastserverstanding.model;

import android.graphics.PointF;

/**
 * Represents a projectile fired from a tower toward an enemy
 */
public class Projectile {
    private String id;
    private PointF position;
    private PointF velocity;
    private Enemy target;
    private float damage;
    private float speed;
    private boolean hasHit;
    
    public Projectile(String id, PointF start, Enemy target, float damage, float speed) {
        this.id = id;
        this.position = new PointF(start.x, start.y);
        this.target = target;
        this.damage = damage;
        this.speed = speed;
        this.hasHit = false;
        updateVelocity();
    }
    
    /**
     * Update projectile position
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        if (hasHit || target == null || !target.isAlive()) {
            return;
        }
        
        // Update velocity to track target
        updateVelocity();
        
        // Move projectile
        position.x += velocity.x * deltaTime;
        position.y += velocity.y * deltaTime;
        
        // Check for collision with target
        float dx = position.x - target.getPosition().x;
        float dy = position.y - target.getPosition().y;
        float distanceSquared = dx * dx + dy * dy;
        
        if (distanceSquared < 1.0f) { // Hit threshold
            hit();
        }
    }
    
    /**
     * Calculate velocity toward target
     */
    private void updateVelocity() {
        if (target == null) return;
        
        PointF targetPos = target.getPosition();
        float dx = targetPos.x - position.x;
        float dy = targetPos.y - position.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0) {
            velocity = new PointF(
                (dx / distance) * speed,
                (dy / distance) * speed
            );
        } else {
            velocity = new PointF(0, 0);
        }
    }
    
    /**
     * Handle projectile hitting target
     */
    private void hit() {
        if (!hasHit && target != null && target.isAlive()) {
            target.takeDamage(damage);
            hasHit = true;
        }
    }
    
    // Getters
    public String getId() { return id; }
    public PointF getPosition() { return position; }
    public PointF getVelocity() { return velocity; }
    public Enemy getTarget() { return target; }
    public float getDamage() { return damage; }
    public boolean hasHit() { return hasHit; }
}
