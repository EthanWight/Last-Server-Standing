package edu.commonwealthu.lastserverstanding.model.enemies;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;

/**
 * Data Crawler - The basic, fast-moving enemy type in Last Server Standing.
 * Represents automated web scraping bots with low health (50) and high speed (120).
 * Designed as swarm enemies that are individually weak but dangerous in large numbers.
 * Rendered in red with a bug icon, available from Wave 1 onwards.
 *
 * @author Ethan Wight
 */
public class DataCrawler extends Enemy {

    /*
     * ===========================================
     * STAT DEFINITIONS AND BALANCE RATIONALE
     * ===========================================
     *
     * Health (50f):
     *   - Lowest health of all enemy types
     *   - Can be one-shot by most upgraded towers
     *   - Balanced for early-game tower damage output
     *
     * Speed (120f):
     *   - Fastest enemy in the game
     *   - 20% faster than VirusBot (100f)
     *   - 50% faster than TrojanHorse (80f)
     *   - Requires towers with good projectile speed or instant-hit mechanics
     *
     * Reward (5 credits):
     *   - Lowest reward, reflecting low threat level
     *   - Players must kill 20 to afford a basic tower
     *   - Encourages efficient tower placement over kill-farming
     *
     * Damage (5):
     *   - Minimal damage per leak
     *   - Allows players to survive a few mistakes in early waves
     *   - A full wave leak would still be devastating (5 * wave_size)
     */

    /**
     * Constructs a new Data Crawler with predefined stats.
     * Initializes with health 50, speed 120, reward 5 credits, and damage 5.
     *
     * @param path List of waypoints defining the path this enemy will traverse
     */
    public DataCrawler(List<PointF> path) {
        super(
            UUID.randomUUID().toString(),  // Unique identifier for tracking
            path,                           // Navigation waypoints
            50f,   // maxHealth - Low: designed to be killed quickly by any tower
            120f,  // speed - High: fastest enemy, tests tower tracking ability
            5,     // reward - Low: reflects minimal threat, prevents early-game snowballing
            5      // damage - Low: forgiving for new players, but swarms are still dangerous
        );
    }

    /**
     * Returns the display name for this enemy type.
     *
     * @return "Data Crawler"
     */
    @Override
    public String getType() {
        return "Data Crawler";
    }

    /**
     * Returns the primary color used to render this enemy.
     *
     * @return Red color for rendering
     */
    @Override
    public int getColor() {
        // Red chosen for high visibility and "danger" association
        // Also differentiates from other enemy types in the color spectrum
        return android.graphics.Color.RED;
    }

    /**
     * Returns the drawable resource ID for this enemy's icon.
     *
     * @return Bug icon resource ID
     */
    @Override
    public int getIconResId() {
        // Bug icon represents the "crawler" nature - small, numerous, persistent
        return R.drawable.ic_enemy_bug;
    }

}
