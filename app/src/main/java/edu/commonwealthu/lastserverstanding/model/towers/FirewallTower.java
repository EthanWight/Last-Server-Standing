package edu.commonwealthu.lastserverstanding.model.towers;

import android.graphics.PointF;

import java.util.UUID;

import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.StatusEffect;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Firewall Tower - Basic defense tower with high fire rate
 * Good all-around tower for early game
 */
public class FirewallTower extends Tower {
    
    // Special property for Firewall
    private float penetration; // Chance to damage multiple enemies
    
    /**
     * Constructor with default stats
     */
    public FirewallTower(PointF position) {
        super(
            UUID.randomUUID().toString(),
            position,
            1,  // level
            15f, // damage
            150f, // range
            2.0f, // fireRate (2 attacks per second)
            200 // cost (doubled from 100)
        );
        this.penetration = 0.0f; // No penetration at level 1
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

        // Create burn effect (damage over time)
        StatusEffect burnEffect = new StatusEffect(
            StatusEffect.Type.BURN,
            damage * 0.5f, // Burn DPS (50% of direct damage)
            2.5f           // 2.5 seconds duration
        );

        return new Projectile(
            UUID.randomUUID().toString(),
            new PointF(position.x, position.y),
            target,
            damage,
            400f, // Projectile speed
            burnEffect
        );
    }

    @Override
    public String getType() {
        return "Firewall";
    }
    
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
