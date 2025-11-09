package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.model.Enemy;
import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Jammer Tower - Fast attack speed with wide range
 * Best for hitting multiple targets
 */
public class JammerTower extends Tower {

    /**
     * Constructor with default stats
     */
    public JammerTower(PointF position) {
        super(
            UUID.randomUUID().toString(),
            position,
            1,  // level
            10f, // damage (lower damage)
            200f, // range (wider range)
            3.0f, // fireRate (3 attacks per second - very fast!)
            200 // cost
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
            500f // Projectile speed (very fast)
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
        return "Jammer";
    }
}
