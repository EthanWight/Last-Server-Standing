package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;


import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.StatusEffect;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Honeypot Tower - A strategic control tower that slows enemy movement.
 *
 * <p>This tower represents a cybersecurity honeypot - a decoy system designed to
 * attract and trap malicious actors. In gameplay, it excels at crowd control by
 * significantly reducing enemy movement speed, giving other towers more time to
 * deal damage and allowing players to manage enemy flow through the map.</p>
 *
 * <h3>Gameplay Role:</h3>
 * <ul>
 *   <li>Primary crowd control tower - essential for wave management</li>
 *   <li>Force multiplier - increases effectiveness of all nearby damage towers</li>
 *   <li>Best placed at map entry points or before damage-dealing tower clusters</li>
 *   <li>Synergizes exceptionally well with Firewall Towers (more burn ticks)</li>
 * </ul>
 *
 * <h3>Balance Philosophy:</h3>
 * <p>The Honeypot Tower trades range and fire rate for powerful utility. Its
 * 50% slow effect is among the strongest crowd control abilities, justified by
 * its shorter range (120) and moderate attack speed (1.5/sec). The higher cost
 * (300 credits) reflects its strategic value - a well-placed Honeypot can
 * dramatically increase a defense's overall effectiveness.</p>
 *
 * <h3>Thematic Design:</h3>
 * <p>Honeypots in cybersecurity are trap systems that lure attackers, slowing
 * their progress while gathering intelligence. The slow effect represents how
 * enemies become "stuck" investigating the decoy system, giving defenders
 * time to neutralize them.</p>
 *
 * @author Ethan Wight
 * @see Tower
 * @see StatusEffect.Type#SLOW
 */
public class HoneypotTower extends Tower {

    /**
     * Constructs a new Honeypot Tower at the specified position with default stats.
     *
     * <p>Default statistics prioritize control utility over raw damage:</p>
     * <ul>
     *   <li><b>Damage (15):</b> Higher than Firewall to compensate for slower fire rate</li>
     *   <li><b>Range (120):</b> Short range requires careful placement near paths</li>
     *   <li><b>Fire Rate (1.5/sec):</b> Slower attacks, but slow effect has long duration</li>
     *   <li><b>Cost (300):</b> Premium price reflects high strategic value, doubled from 150</li>
     * </ul>
     *
     * <p>The short range is an intentional design choice - players must commit to
     * placing Honeypots in high-traffic areas where enemies will definitely pass,
     * creating interesting strategic decisions about tower placement.</p>
     *
     * @param position The position to place this tower on the grid, in screen coordinates.
     *                 This position represents the center point of the tower.
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
     * Updates the tower state each frame, managing target acquisition and validation.
     *
     * <p>This method performs essential housekeeping by clearing invalid targets.
     * A target becomes invalid when:</p>
     * <ul>
     *   <li>The enemy has been destroyed (no longer alive)</li>
     *   <li>The enemy has moved out of the tower's attack range</li>
     * </ul>
     *
     * <p>For Honeypot Towers, target validation is especially important because
     * the short range means enemies can quickly move out of attack radius.
     * Clearing stale targets ensures the tower rapidly reacquires new threats.</p>
     *
     * @param deltaTime Time elapsed since the last update call, in seconds.
     *                  Used for time-based calculations and animations.
     */
    @Override
    public void update(float deltaTime) {
        // Validate current target - clear if dead or out of range
        if (target != null && (!target.isAlive() || isOutOfRange(target))) {
            target = null;
        }
    }

    /**
     * Fires a projectile at the current target if conditions are met.
     *
     * <p>Creates a sticky projectile that deals moderate damage on impact
     * and applies a powerful slow status effect. The slow effect reduces
     * enemy movement speed by 50% for 3 seconds, providing excellent
     * crowd control and synergy with damage-focused towers.</p>
     *
     * <h4>Projectile Configuration:</h4>
     * <ul>
     *   <li><b>Speed (350):</b> Slower projectile speed, thematically "sticky"</li>
     *   <li><b>Slow Intensity:</b> 50% movement speed reduction (0.5 modifier)</li>
     *   <li><b>Slow Duration:</b> 3.0 seconds - long duration for sustained control</li>
     * </ul>
     *
     * <h4>Strategic Value:</h4>
     * <p>The 3-second slow duration exceeds the tower's attack cooldown (0.67 sec),
     * meaning a single Honeypot can maintain permanent slow on a target. Multiple
     * Honeypots don't stack the slow effect, but can cover larger areas.</p>
     *
     * @return A new Projectile targeting the current enemy with slow effect attached,
     *         or {@code null} if no valid target exists or the tower is on cooldown.
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
     * <p>This identifier is used in the UI for tower selection, upgrade menus,
     * and player-facing text. The name "Honeypot" reflects the cybersecurity
     * concept of a decoy system designed to attract and trap attackers.</p>
     *
     * @return The string "Honeypot" as this tower's type identifier.
     */
    @Override
    public String getType() {
        return "Honeypot";
    }
}
