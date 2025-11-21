package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;


import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.StatusEffect;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Firewall Tower - Basic defense tower with high fire rate.
 * Good all-around tower for early game.
 *
 * @author Ethan Wight
 */
public class FirewallTower extends Tower {
    
    // Special property for Firewall
    private float penetration; // Chance to damage multiple enemies
    
    /**
     * Constructor with default stats.
     *
     * @param position The position to place this tower on the grid.
     */
    public FirewallTower(PointF position) {
        super(
            position,
            1,  // level
            10f, // damage
            150f, // range
            2.0f, // fireRate (2 attacks per second)
            200 // cost (doubled from 100)
        );
        this.penetration = 0.0f; // No penetration at level 1
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

        // Create burn effect (damage over time)
        StatusEffect burnEffect = new StatusEffect(
            StatusEffect.Type.BURN,
            damage * 0.5f, // Burn DPS (50% of direct damage)
            2.5f           // 2.5 seconds duration
        );

        return new Projectile(
            new PointF(position.x, position.y),
            target,
            damage,
            400f, // Projectile speed
            burnEffect
        );
    }

    /**
     * Get the tower type name.
     *
     * @return The type name of this tower.
     */
    @Override
    public String getType() {
        return "Firewall";
    }

    /**
     * Upgrade tower to next level.
     *
     * @param upgradeCost The cost paid for this upgrade.
     * @return True if upgrade successful.
     */
    @Override
    public boolean upgrade(int upgradeCost) {
        boolean upgraded = super.upgrade(upgradeCost);
        if (upgraded) {
            // Increase penetration with each level
            penetration = Math.min(penetration + 0.15f, 0.6f);
        }
        return upgraded;
    }

}
