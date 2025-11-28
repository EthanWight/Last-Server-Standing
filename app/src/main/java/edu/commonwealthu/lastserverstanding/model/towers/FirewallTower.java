package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;


import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.StatusEffect;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Firewall Tower - Primary damage-dealing tower that applies burn damage over time.
 * Moderate damage (10), medium range (150), fast fire rate (2.0/sec). Cost: 200 credits.
 * Burn effect deals 50% of tower damage per second for 2.5 seconds.
 * Best placed along enemy paths for consistent damage output.
 *
 * @author Ethan Wight
 */
public class FirewallTower extends Tower {

    /**
     * Constructs a new Firewall Tower with default stats.
     * Damage: 10, Range: 150, Fire Rate: 2.0/sec, Cost: 200.
     *
     * @param position The center position to place this tower in screen coordinates.
     */
    public FirewallTower(PointF position) {
        super(
            position,
            1,  // level - all towers start at level 1
            10f, // damage - moderate base, burn effect adds additional DPS
            150f, // range - balanced for early game, not too far, not too close
            2.0f, // fireRate - 2 attacks per second provides steady damage output
            200 // cost - doubled from 100 to prevent early tower spam
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
     * Fires a projectile that deals direct damage and applies burn effect.
     * Burn deals 50% of direct damage per second for 2.5 seconds.
     *
     * @return A new Projectile with burn effect, or null if no valid target or on cooldown.
     */
    @Override
    public Projectile fire() {
        // Guard clause: cannot fire without target or while on cooldown
        if (target == null || isOnCooldown()) {
            return null;
        }

        // Record fire time for cooldown tracking
        lastFireTime = System.currentTimeMillis();

        // Create burn effect - represents the firewall's ability to continuously
        // damage threats. The burn deals damage over time, symbolizing ongoing
        // threat mitigation even after initial detection.
        StatusEffect burnEffect = new StatusEffect(
            StatusEffect.Type.BURN,
            damage * 0.5f, // Burn DPS scales with tower damage (50% of direct damage)
            2.5f           // 2.5 seconds duration - long enough to be impactful
        );

        // Create and return the projectile with burn effect attached
        return new Projectile(
            new PointF(position.x, position.y), // Projectile origin at tower center
            target,                              // Target enemy to track
            damage,                              // Direct hit damage
            400f,                                // Projectile travel speed in pixels/second
            burnEffect                           // Status effect applied on hit
        );
    }

    /**
     * Returns the display name for this tower type.
     *
     * @return The string "Firewall".
     */
    @Override
    public String getType() {
        return "Firewall";
    }

}
