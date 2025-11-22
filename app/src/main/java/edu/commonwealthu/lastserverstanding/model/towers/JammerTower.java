package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;


import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.StatusEffect;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Jammer Tower - High-frequency disruptor tower with rapid attacks and stun capability.
 * Moderate damage (10), widest range (200), fastest fire rate (3.0/sec). Cost: 400 credits.
 * Applies brief stuns (0.5 sec) frequently for interrupt-based crowd control.
 * Best placed centrally to maximize range coverage and rapid target switching.
 *
 * @author Ethan Wight
 */
public class JammerTower extends Tower {

    /**
     * Constructs a new Jammer Tower with default stats.
     * Damage: 10, Range: 200, Fire Rate: 3.0/sec, Cost: 400.
     * Base DPS: 30 (highest sustained damage of basic towers).
     *
     * @param position The center position to place this tower in screen coordinates.
     */
    public JammerTower(PointF position) {
        super(
            position,
            1,  // level - all towers start at level 1
            10f, // damage - lower per-hit, but highest fire rate compensates
            200f, // range - widest coverage of basic towers for area control
            3.0f, // fireRate - 3 attacks/sec, fastest of all basic towers
            400 // cost - doubled from 200, premium price for premium capability
        );
    }

    /**
     * Updates the tower state each frame, clearing invalid targets.
     * A target is invalid if it's dead or out of range.
     */
    @Override
    public void update() {
        // Validate current target - clear if dead or out of range
        if (target != null && (!target.isAlive() || isOutOfRange(target))) {
            target = null;
        }
    }

    /**
     * Fires a fast projectile that deals damage and applies 0.5 second stun.
     * Frequent stuns create stuttering movement pattern on continuously targeted enemies.
     *
     * @return A new Projectile with stun effect, or null if no valid target or on cooldown.
     */
    @Override
    public Projectile fire() {
        // Guard clause: cannot fire without target or while on cooldown
        if (target == null || isOnCooldown()) {
            return null;
        }

        // Record fire time for cooldown tracking
        lastFireTime = System.currentTimeMillis();

        // Create stun effect - represents the jammer disrupting enemy systems.
        // The full stun (1.0 intensity) completely stops movement, but the short
        // duration (0.5 sec) ensures it's an interrupt rather than a lockdown.
        // This creates a "stuttering" effect on enemies under sustained fire.
        StatusEffect stunEffect = new StatusEffect(
            StatusEffect.Type.STUN,
            1.0f,  // Full stun intensity - complete movement halt
            0.5f   // Brief duration - interrupt, not lockdown (in seconds)
        );

        // Create and return the projectile with stun effect attached
        return new Projectile(
            new PointF(position.x, position.y), // Projectile origin at tower center
            target,                              // Target enemy to track
            damage,                              // Direct hit damage
            500f,                                // Fastest projectile speed (EM pulse)
            stunEffect                           // Status effect applied on hit
        );
    }

    /**
     * Returns the display name for this tower type.
     *
     * @return The string "Jammer".
     */
    @Override
    public String getType() {
        return "Jammer";
    }
}
