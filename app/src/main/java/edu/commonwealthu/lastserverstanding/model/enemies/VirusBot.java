package edu.commonwealthu.lastserverstanding.model.enemies;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;

/**
 * Virus Bot - The mid-tier enemy representing infected systems in Last Server Standing.
 * Represents virus-infected systems with medium health (100) and moderate speed (100).
 * Stats are 2x multiplier from Data Crawler, requiring upgraded towers or sustained fire.
 * Rendered in blue with a virus icon, first appears at Wave 5.
 *
 * @author Ethan Wight
 */
public class VirusBot extends Enemy {

    /*
     * ===========================================
     * STAT DEFINITIONS AND BALANCE RATIONALE
     * ===========================================
     *
     * Health (100f):
     *   - Double the Data Crawler's health (50f)
     *   - Requires ~2 hits from early-game towers
     *   - Forces players to consider tower upgrades or placement density
     *   - Sweet spot: not too tanky to feel unfair, not too weak to ignore
     *
     * Speed (100f):
     *   - 17% slower than Data Crawler (120f)
     *   - 25% faster than TrojanHorse (80f)
     *   - Moderate pace allows towers more time to deal damage
     *   - Speed reduction balances the health increase
     *
     * Reward (10 credits):
     *   - Double the Data Crawler reward (5)
     *   - Maintains reward-per-health ratio consistency
     *   - Provides meaningful income for mid-game tower purchases
     *   - 10 Virus Bots = 1 mid-tier tower purchase
     *
     * Damage (10):
     *   - Double the Data Crawler damage (5)
     *   - Single leak is noticeable but survivable
     *   - Multiple leaks quickly become critical
     *   - Incentivizes prioritizing Virus Bots over Data Crawlers
     */

    /**
     * Constructs a new Virus Bot with predefined stats.
     * Initializes with health 100, speed 100, reward 10 credits, and damage 10.
     *
     * @param path List of waypoints defining the path this enemy will traverse
     */
    public VirusBot(List<PointF> path) {
        super(
            UUID.randomUUID().toString(),  // Unique identifier for tracking
            path,                           // Navigation waypoints
            100f,  // maxHealth - Medium: 2x Data Crawler, requires focused fire
            100f,  // speed - Medium: slightly slower to offset health increase
            10,    // reward - Medium: proportional to difficulty increase
            10     // damage - Medium: leaks hurt but aren't immediately fatal
        );
    }

    /**
     * Returns the display name for this enemy type.
     *
     * @return "Virus Bot"
     */
    @Override
    public String getType() {
        return "Virus Bot";
    }

    /**
     * Returns the primary color used to render this enemy.
     *
     * @return Blue color for rendering
     */
    @Override
    public int getColor() {
        // Blue represents the digital/cyber nature of viruses
        // Contrasts with red (Data Crawler) and green (TrojanHorse)
        return android.graphics.Color.BLUE;
    }

    /**
     * Returns the drawable resource ID for this enemy's icon.
     *
     * @return Virus icon resource ID
     */
    @Override
    public int getIconResId() {
        // Virus icon clearly communicates the "infected system" theme
        return R.drawable.ic_enemy_virus;
    }

}
