package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;


import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.StatusEffect;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Honeypot Tower - Strategic crowd control tower that slows enemy movement by 50%.
 * Higher damage (15), short range (120), moderate fire rate (1.5/sec). Cost: 300 credits.
 * Excels at force multiplication, giving other towers more time to deal damage.
 * Best placed at entry points or before damage-dealing tower clusters.
 *
 * @author Ethan Wight
 */
public class HoneypotTower extends Tower {

    /**
     * Constructs a new Honeypot Tower with default stats.
     * Damage: 15, Range: 120, Fire Rate: 1.5/sec, Cost: 300.
     *
     * @param position The center position to place this tower in screen coordinates.
     */
    public HoneypotTower(PointF position) {
        super(
            position,
            1,  // level - all towers start at level 1
            15f, // damage - higher base damage compensates for slower fire rate
            120f, // range - intentionally short to require strategic placement
            1.5f, // fireRate - 1.5 attacks/sec, slower but slow effect is powerful
            300 // cost - doubled from 150, premium price for premium utility
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
     * Fires a projectile that deals damage and applies 50% slow for 3 seconds.
     * The slow duration exceeds attack cooldown, allowing permanent slow on targets in range.
     *
     * @return A new Projectile with slow effect, or null if no valid target or on cooldown.
     */
    @Override
    public Projectile fire() {
        // Guard clause: cannot fire without target or while on cooldown
        if (target == null || isOnCooldown()) {
            return null;
        }

        // Record fire time for cooldown tracking
        lastFireTime = System.currentTimeMillis();

        // Create slow effect - represents the honeypot "trapping" enemies.
        // The 50% slow is powerful but balanced by the tower's short range.
        // Duration of 3 seconds ensures continuous slow if enemy stays in range.
        StatusEffect slowEffect = new StatusEffect(
            StatusEffect.Type.SLOW,
            0.5f,  // 50% speed reduction - enemy moves at half speed
            3.0f   // 3 seconds duration - exceeds fire cooldown for perma-slow
        );

        // Create and return the projectile with slow effect attached
        return new Projectile(
            new PointF(position.x, position.y), // Projectile origin at tower center
            target,                              // Target enemy to track
            damage,                              // Direct hit damage
            350f,                                // Slower projectile speed (thematic)
            slowEffect                           // Status effect applied on hit
        );
    }

    /**
     * Returns the display name for this tower type.
     *
     * @return The string "Honeypot".
     */
    @Override
    public String getType() {
        return "Honeypot";
    }
}
