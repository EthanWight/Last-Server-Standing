package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;


import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.StatusEffect;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Firewall Tower - The foundational defense tower representing a network firewall.
 *
 * <p>This tower serves as the primary damage-dealing defensive structure in the game,
 * designed to be the player's first line of defense against incoming cyber threats.
 * The Firewall Tower excels at consistent, reliable damage output with its balanced
 * stats making it effective throughout all stages of the game.</p>
 *
 * <h3>Gameplay Role:</h3>
 * <ul>
 *   <li>Early game staple - affordable cost allows quick deployment</li>
 *   <li>Consistent DPS dealer with burn damage over time</li>
 *   <li>Upgradeable penetration allows damage to pass through to additional enemies</li>
 *   <li>Best placed at chokepoints where enemies cluster</li>
 * </ul>
 *
 * <h3>Balance Philosophy:</h3>
 * <p>The Firewall Tower is intentionally designed as a "jack-of-all-trades" tower.
 * Its moderate cost (200 credits) makes it accessible early while its upgrade path
 * (penetration) rewards investment for late-game scaling. The burn effect adds
 * sustained damage that complements other tower types.</p>
 *
 * <h3>Thematic Design:</h3>
 * <p>Represents a network firewall that "burns" malicious traffic. The burn
 * damage-over-time effect symbolizes how firewalls continue to filter and
 * block threats even after initial detection.</p>
 *
 * @author Ethan Wight
 * @see Tower
 * @see StatusEffect.Type#BURN
 */
public class FirewallTower extends Tower {

    /**
     * Penetration chance for projectiles to damage multiple enemies.
     *
     * <p>This value represents the probability (0.0 to 1.0) that a projectile
     * will pass through the initial target and potentially hit enemies behind it.
     * Penetration starts at 0% and increases by 15% per upgrade, capping at 60%.</p>
     *
     * <p>This mechanic rewards players who invest in upgrading their Firewall Towers,
     * making them more effective against grouped enemies in later waves.</p>
     */
    private float penetration;

    /**
     * Constructs a new Firewall Tower at the specified position with default stats.
     *
     * <p>Default statistics are balanced for early-game accessibility:</p>
     * <ul>
     *   <li><b>Damage (10):</b> Moderate base damage, supplemented by burn effect</li>
     *   <li><b>Range (150):</b> Medium range - requires strategic placement</li>
     *   <li><b>Fire Rate (2.0/sec):</b> Reliable attack speed for consistent damage</li>
     *   <li><b>Cost (200):</b> Affordable entry point, doubled from original 100 for balance</li>
     * </ul>
     *
     * <p>The cost was doubled during balancing to prevent early-game tower spam
     * and encourage more thoughtful placement decisions.</p>
     *
     * @param position The position to place this tower on the grid, in screen coordinates.
     *                 This position represents the center point of the tower.
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
        // Initialize penetration to 0 - players must upgrade to gain this ability
        this.penetration = 0.0f;
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
     * <p>Clearing invalid targets allows the targeting system to acquire new
     * enemies on the next targeting pass.</p>
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
     * <p>Creates a fire-based projectile that deals direct damage on impact
     * plus applies a burn status effect for damage over time. The burn effect
     * deals 50% of the tower's direct damage per second for 2.5 seconds.</p>
     *
     * <h4>Projectile Configuration:</h4>
     * <ul>
     *   <li><b>Speed (400):</b> Moderate projectile speed, balanced for visual clarity</li>
     *   <li><b>Burn DPS:</b> 50% of direct damage (5 DPS at base level)</li>
     *   <li><b>Burn Duration:</b> 2.5 seconds for total burn damage of 12.5 at base</li>
     * </ul>
     *
     * <p>Total damage per hit at base level: 10 (direct) + 12.5 (burn) = 22.5 damage</p>
     *
     * @return A new Projectile targeting the current enemy with burn effect attached,
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
     * <p>This identifier is used in the UI for tower selection, upgrade menus,
     * and player-facing text. The name "Firewall" reflects the cybersecurity
     * theme of the game.</p>
     *
     * @return The string "Firewall" as this tower's type identifier.
     */
    @Override
    public String getType() {
        return "Firewall";
    }

    /**
     * Upgrades the tower to the next level, enhancing stats and penetration.
     *
     * <p>In addition to the base stat upgrades provided by the parent class
     * (damage, range, fire rate), the Firewall Tower gains increased penetration
     * chance with each upgrade. This allows projectiles to potentially hit
     * multiple enemies, making upgraded Firewall Towers effective against
     * clustered enemy formations.</p>
     *
     * <h4>Penetration Progression:</h4>
     * <ul>
     *   <li>Level 1: 0% penetration (base)</li>
     *   <li>Level 2: 15% penetration</li>
     *   <li>Level 3: 30% penetration</li>
     *   <li>Level 4: 45% penetration</li>
     *   <li>Level 5+: 60% penetration (capped)</li>
     * </ul>
     *
     * @param upgradeCost The amount of credits deducted for this upgrade.
     *                    Used for tracking total investment in the tower.
     * @return {@code true} if the upgrade was successful, {@code false} if
     *         the tower is already at maximum level or upgrade failed.
     */
    @Override
    public boolean upgrade(int upgradeCost) {
        // Attempt base upgrade first (handles level cap and stat increases)
        boolean upgraded = super.upgrade(upgradeCost);
        if (upgraded) {
            // Increase penetration by 15% per level, capped at 60% maximum
            // This cap prevents the tower from becoming too powerful
            penetration = Math.min(penetration + 0.15f, 0.6f);
        }
        return upgraded;
    }

}
