package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;


import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.StatusEffect;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Jammer Tower - A high-frequency disruptor tower with rapid attacks and stun capability.
 *
 * <p>This tower represents a signal jammer that disrupts enemy systems with rapid-fire
 * electromagnetic pulses. It excels at target saturation through its extremely high
 * attack speed, allowing it to engage multiple enemies quickly and apply stun effects
 * across a wide area. The Jammer is the most expensive basic tower, reflecting its
 * powerful combination of range, speed, and crowd control.</p>
 *
 * <h3>Gameplay Role:</h3>
 * <ul>
 *   <li>Area denial through rapid target switching and wide coverage</li>
 *   <li>Interrupt-based crowd control with short but frequent stuns</li>
 *   <li>Excellent against fast-moving enemies due to high fire rate</li>
 *   <li>Best placed centrally to maximize range coverage</li>
 * </ul>
 *
 * <h3>Balance Philosophy:</h3>
 * <p>The Jammer Tower is designed as the "premium" basic tower option. Its 400 credit
 * cost (doubled from 200) creates a meaningful investment decision. The tower's
 * strength lies in its combination of the widest range (200), fastest fire rate
 * (3.0/sec), and stun capability. However, the stun duration is intentionally short
 * (0.5 sec) to prevent it from being overpowered - it interrupts rather than locks
 * down enemies.</p>
 *
 * <h3>Thematic Design:</h3>
 * <p>Signal jammers in cybersecurity block or disrupt communications. The stun effect
 * represents how the tower's electromagnetic interference temporarily disables enemy
 * systems, causing them to "freeze" momentarily as they recover from the disruption.</p>
 *
 * <h3>Synergy Notes:</h3>
 * <p>Pairs well with Firewall Towers - the brief stuns interrupt enemy movement,
 * allowing burn effects more ticks of damage. Less synergy with Honeypot since
 * stun and slow effects serve similar purposes (movement denial).</p>
 *
 * @author Ethan Wight
 * @see Tower
 * @see StatusEffect.Type#STUN
 */
public class JammerTower extends Tower {

    /**
     * Constructs a new Jammer Tower at the specified position with default stats.
     *
     * <p>Default statistics emphasize speed and coverage over raw damage:</p>
     * <ul>
     *   <li><b>Damage (10):</b> Lower base damage, but high fire rate compensates</li>
     *   <li><b>Range (200):</b> Widest range of basic towers - excellent coverage</li>
     *   <li><b>Fire Rate (3.0/sec):</b> Fastest attack speed - rapid target engagement</li>
     *   <li><b>Cost (400):</b> Most expensive basic tower, doubled from 200 for balance</li>
     * </ul>
     *
     * <p>The high cost reflects the Jammer's versatility - it can engage enemies
     * across a large area with stunning frequency. Players should save for this
     * tower when they need to control a wide chokepoint or deal with fast enemies.</p>
     *
     * <h4>DPS Calculation:</h4>
     * <p>Base DPS: 10 damage x 3.0 attacks/sec = 30 DPS (highest sustained damage
     * of basic towers before accounting for special effects)</p>
     *
     * @param position The position to place this tower on the grid, in screen coordinates.
     *                 This position represents the center point of the tower.
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
     * Updates the tower state each frame, managing target acquisition and validation.
     *
     * <p>This method performs essential housekeeping by clearing invalid targets.
     * A target becomes invalid when:</p>
     * <ul>
     *   <li>The enemy has been destroyed (no longer alive)</li>
     *   <li>The enemy has moved out of the tower's attack range</li>
     * </ul>
     *
     * <p>For Jammer Towers, rapid target validation is crucial because the high
     * fire rate means the tower will attempt to fire frequently. Stale targets
     * would cause wasted attack attempts, reducing effective DPS.</p>
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
     * <p>Creates a fast-moving disruption pulse that deals damage on impact
     * and applies a brief stun effect. The stun completely halts enemy movement
     * for a short duration, creating opportunities for other towers to deal
     * damage to stationary targets.</p>
     *
     * <h4>Projectile Configuration:</h4>
     * <ul>
     *   <li><b>Speed (500):</b> Fastest projectile - represents electromagnetic pulse</li>
     *   <li><b>Stun Intensity:</b> 1.0 (full stun - complete movement stop)</li>
     *   <li><b>Stun Duration:</b> 0.5 seconds - brief but impactful interrupt</li>
     * </ul>
     *
     * <h4>Stun Mechanics:</h4>
     * <p>The 0.5-second stun occurs every 0.33 seconds (at 3.0 fire rate), meaning
     * a single target under continuous fire spends roughly 50% of its time stunned.
     * This creates a "stuttering" movement pattern rather than permanent lockdown,
     * which is more balanced and visually interesting.</p>
     *
     * <h4>Effective Crowd Control:</h4>
     * <p>Unlike Honeypot's sustained slow, Jammer's stuns are frequent but brief.
     * This makes Jammer better against single high-priority targets, while Honeypot
     * excels at slowing entire waves.</p>
     *
     * @return A new Projectile targeting the current enemy with stun effect attached,
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
     * <p>This identifier is used in the UI for tower selection, upgrade menus,
     * and player-facing text. The name "Jammer" reflects the tower's role as
     * a signal disruption device that interferes with enemy systems.</p>
     *
     * @return The string "Jammer" as this tower's type identifier.
     */
    @Override
    public String getType() {
        return "Jammer";
    }
}
