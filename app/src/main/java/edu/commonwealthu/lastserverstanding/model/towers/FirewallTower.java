package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.model.Enemy;
import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Firewall Tower - Basic defense tower with high fire rate
 * Good all-around tower for early game
 */
public class FirewallTower extends Tower {
    
    // Special property for Firewall
    private float penetration; // Chance to damage multiple enemies
    
    /**
     * Constructor with default stats
     */
    public FirewallTower(PointF position) {
        super(
            UUID.randomUUID().toString(),
            position,
            1,  // level
            15f, // damage
            150f, // range
            2.0f, // fireRate (2 attacks per second)
            100 // cost
        );
        this.penetration = 0.0f; // No penetration at level 1
    }
    
    @Override
    public void update(float deltaTime) {
        // Update target validity
        if (target != null && (!target.isAlive() || !isInRange(target))) {
            target = null;
        }
    }
    
    @Override
    public Projectile fire() {
        if (target == null || !canFire() || isCorrupted) {
            return null;
        }
        
        lastFireTime = System.currentTimeMillis();
        return new Projectile(
            UUID.randomUUID().toString(),
            new PointF(position.x, position.y),
            target,
            damage,
            400f // Projectile speed
        );
    }
    
    @Override
    public void acquireTarget(List<Enemy> enemies) {
        if (target != null && target.isAlive() && isInRange(target)) {
            return; // Keep current target
        }
        
        // Find closest enemy in range
        Enemy closestEnemy = null;
        float closestDistance = Float.MAX_VALUE;
        
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;
            
            float dx = enemy.getPosition().x - position.x;
            float dy = enemy.getPosition().y - position.y;
            float distance = dx * dx + dy * dy; // Use squared distance to avoid sqrt
            
            if (distance <= range * range && distance < closestDistance) {
                closestEnemy = enemy;
                closestDistance = distance;
            }
        }
        
        target = closestEnemy;
    }
    
    @Override
    public String getType() {
        return "Firewall";
    }
    
    @Override
    public boolean upgrade() {
        boolean upgraded = super.upgrade();
        if (upgraded) {
            // Increase penetration with each level
            penetration = Math.min(penetration + 0.15f, 0.6f);
        }
        return upgraded;
    }
    
    public float getPenetration() {
        return penetration;
    }
}
