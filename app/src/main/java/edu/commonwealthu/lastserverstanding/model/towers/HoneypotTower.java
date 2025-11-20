package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;

import java.util.UUID;

import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.StatusEffect;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Honeypot Tower - Slows enemies and deals damage over time.
 * Best for controlling enemy movement.
 *
 * @author Ethan Wight
 */
public class HoneypotTower extends Tower {

    /**
     * Constructor with default stats.
     *
     * @param position The position to place this tower on the grid.
     */
    public HoneypotTower(PointF position) {
        super(
            UUID.randomUUID().toString(),
            position,
            1,  // level
            15f, // damage
            120f, // range
            1.5f, // fireRate (1.5 attacks per second)
            300 // cost (doubled from 150)
        );
    }

    /**
     * Update tower state each frame.
     *
     * @param deltaTime Time since last update in seconds.
     */
    @Override
    public void update(float deltaTime) {
        // Update target validity
        if (target != null && (!target.isAlive() || isOutOfRange(target))) {
            target = null;
        }
    }

    /**
     * Fire at the current target if possible.
     *
     * @return Projectile if fired, null otherwise.
     */
    @Override
    public Projectile fire() {
        if (target == null || isOnCooldown()) {
            return null;
        }

        lastFireTime = System.currentTimeMillis();

        // Create slow effect (honeypot slows enemies)
        StatusEffect slowEffect = new StatusEffect(
            StatusEffect.Type.SLOW,
            0.5f,  // 50% speed reduction
            3.0f   // 3 seconds duration
        );

        return new Projectile(
            UUID.randomUUID().toString(),
            new PointF(position.x, position.y),
            target,
            damage,
            350f, // Projectile speed (slower than firewall)
            slowEffect
        );
    }

    /**
     * Get the tower type name.
     *
     * @return The type name of this tower.
     */
    @Override
    public String getType() {
        return "Honeypot";
    }
}
