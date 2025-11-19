package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;

import java.util.UUID;

import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.StatusEffect;
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
            400 // cost (doubled from 200)
        );
    }

    @Override
    public void update(float deltaTime) {
        // Update target validity
        if (target != null && (!target.isAlive() || isOutOfRange(target))) {
            target = null;
        }
    }

    @Override
    public Projectile fire() {
        if (target == null || isOnCooldown() || isCorrupted) {
            return null;
        }

        lastFireTime = System.currentTimeMillis();

        // Create stun effect (neural jammer disrupts enemy movement)
        StatusEffect stunEffect = new StatusEffect(
            StatusEffect.Type.STUN,
            1.0f,  // Full stun
            0.5f   // 0.5 seconds duration (longer than firewall)
        );

        return new Projectile(
            UUID.randomUUID().toString(),
            new PointF(position.x, position.y),
            target,
            damage,
            500f, // Projectile speed (very fast)
            stunEffect
        );
    }

    @Override
    public String getType() {
        return "Jammer";
    }
}
