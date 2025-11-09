package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.model.Enemy;
import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Honeypot Tower - Slows enemies and deals damage over time
 * Best for controlling enemy movement
 */
public class HoneypotTower extends Tower {

    /**
     * Constructor with default stats
     */
    public HoneypotTower(PointF position) {
        super(
            UUID.randomUUID().toString(),
            position,
            1,  // level
            25f, // damage
            120f, // range
            1.5f, // fireRate (1.5 attacks per second)
            150 // cost
        );
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
            350f // Projectile speed (slower than firewall)
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
        return "Honeypot";
    }
}
