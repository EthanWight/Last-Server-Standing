package edu.commonwealthu.lastserverstanding.model.enemies;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;

/**
 * Trojan Horse - The heavy, tank-class enemy in Last Server Standing.
 * Represents sophisticated malware with high health (200) and slow speed (80).
 * Stats are 4x multiplier from Data Crawler, requiring upgraded towers or concentrated fire.
 * Rendered in green with a horse icon, first appears at Wave 10.
 *
 * @author Ethan Wight
 */
public class TrojanHorse extends Enemy {

    /*
     * ===========================================
     * STAT DEFINITIONS AND BALANCE RATIONALE
     * ===========================================
     *
     * Health (200f):
     *   - 4x the Data Crawler's health (50f)
     *   - 2x the Virus Bot's health (100f)
     *   - Requires multiple tower attacks or high-damage towers
     *   - Designed to survive initial contact and test sustained DPS
     *   - Acts as a "gear check" for player's tower investment
     *
     * Speed (80f):
     *   - Slowest enemy in the game
     *   - 33% slower than Data Crawler (120f)
     *   - 20% slower than Virus Bot (100f)
     *   - Slow speed is intentional - compensates for massive health pool
     *   - Gives towers maximum time to deal damage
     *   - Creates tension: players see the threat coming but must output enough DPS
     *
     * Reward (20 credits):
     *   - 4x the Data Crawler reward (5)
     *   - 2x the Virus Bot reward (10)
     *   - High value makes them priority targets
     *   - Single Trojan kill equals significant income
     *   - Rewards players who invest in Trojan-killing capability
     *
     * Damage (20):
     *   - 4x the Data Crawler damage (5)
     *   - 2x the Virus Bot damage (10)
     *   - A single leak is catastrophic (20% of starting health typically)
     *   - Multiple Trojan leaks can end a run
     *   - High stakes create memorable moments and strategic depth
     */

    /**
     * Constructs a new Trojan Horse with predefined stats.
     * Initializes with health 200, speed 80, reward 20 credits, and damage 20.
     *
     * @param path List of waypoints defining the path this enemy will traverse
     */
    public TrojanHorse(List<PointF> path) {
        super(
            UUID.randomUUID().toString(),  // Unique identifier for tracking
            path,                           // Navigation waypoints
            200f,  // maxHealth - High: tank-class enemy, tests player's DPS output
            80f,   // speed - Low: slowest enemy, balanced by massive health pool
            20,    // reward - High: valuable target, rewards focused elimination
            20     // damage - High: leaks are devastating, creates high-stakes gameplay
        );
    }

    /**
     * Returns the display name for this enemy type.
     *
     * @return "Trojan Horse"
     */
    @Override
    public String getType() {
        return "Trojan Horse";
    }

    /**
     * Returns the primary color used to render this enemy.
     *
     * @return Green color for rendering
     */
    @Override
    public int getColor() {
        // Green completes RGB differentiation and ironically suggests "safe"
        // while being the most dangerous enemy - fitting the Trojan Horse theme
        return android.graphics.Color.GREEN;
    }

    /**
     * Returns the drawable resource ID for this enemy's icon.
     *
     * @return Horse icon resource ID
     */
    @Override
    public int getIconResId() {
        // Horse icon directly references the Trojan Horse mythology
        // Instantly communicates this is a high-threat, deceptive enemy
        return R.drawable.ic_enemy_horse;
    }

}
