package edu.commonwealthu.lastserverstanding.model.enemies;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;

/**
 * Data Crawler - The basic, fast-moving enemy type in Last Server Standing.
 *
 * <p>This enemy represents automated web scraping bots that continuously probe
 * and crawl through network infrastructure. Data Crawlers are the first enemy
 * type players encounter and serve as the foundational threat throughout the game.</p>
 *
 * <h2>Gameplay Role</h2>
 * <p>Data Crawlers are designed as "swarm" enemies - individually weak but dangerous
 * in large numbers. They test the player's ability to handle fast-moving targets
 * and encourage investment in towers with good tracking or area-of-effect damage.</p>
 *
 * <h2>Spawn Behavior</h2>
 * <ul>
 *   <li>Available from Wave 1 onwards</li>
 *   <li>Spawn frequency increases as waves progress</li>
 *   <li>Often appear in groups to overwhelm defenses</li>
 * </ul>
 *
 * <h2>Visual Representation</h2>
 * <p>Rendered in RED color with a bug icon ({@code ic_enemy_bug}), representing
 * their nature as annoying, persistent software bugs that crawl through systems.</p>
 *
 * <h2>Balance Considerations</h2>
 * <p>As the baseline enemy, all other enemy types are balanced relative to the
 * Data Crawler's stats. The low reward (5 credits) reflects their ease of defeat,
 * while the low damage (5) makes individual leaks manageable.</p>
 *
 * @author Ethan Wight
 * @see Enemy
 * @see VirusBot
 * @see TrojanHorse
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
     * Constructs a new Data Crawler enemy with predefined stats.
     *
     * <p>The Data Crawler is initialized with the following characteristics:</p>
     * <ul>
     *   <li><b>Health:</b> 50 - Low durability, easily destroyed</li>
     *   <li><b>Speed:</b> 120 - Fastest enemy type, requires quick targeting</li>
     *   <li><b>Reward:</b> 5 credits - Minimal payout for easy kills</li>
     *   <li><b>Damage:</b> 5 - Low damage to data center on leak</li>
     * </ul>
     *
     * <p>A unique identifier is automatically generated using {@link UUID#randomUUID()}
     * to ensure each enemy instance can be tracked individually by the game engine.</p>
     *
     * @param path The list of waypoints defining the path this enemy will traverse.
     *             Must not be null and should contain at least two points (start and end).
     *             The enemy will move sequentially through each point until reaching
     *             the data center or being destroyed.
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
     * <p>This name is used in UI elements such as wave previews, kill notifications,
     * and statistics tracking. The name "Data Crawler" evokes the image of web
     * crawlers and bots that systematically traverse network infrastructure.</p>
     *
     * @return The string "Data Crawler" as the human-readable type identifier.
     */
    @Override
    public String getType() {
        return "Data Crawler";
    }

    /**
     * Returns the primary color used to render this enemy.
     *
     * <p>Data Crawlers are rendered in RED ({@code 0xFFFF0000}), chosen to:</p>
     * <ul>
     *   <li>Provide high visibility against most backgrounds</li>
     *   <li>Convey a sense of danger/warning (traditional red = alert)</li>
     *   <li>Differentiate from VirusBot (blue) and TrojanHorse (green)</li>
     * </ul>
     *
     * <p>The color is applied to the enemy's body/shape in the game renderer
     * and may be used for health bar tinting or particle effects.</p>
     *
     * @return {@link android.graphics.Color#RED} (0xFFFF0000) for rendering.
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
     * <p>The bug icon ({@code ic_enemy_bug}) visually represents the Data Crawler's
     * nature as a software "bug" or automated crawler. This icon is used for:</p>
     * <ul>
     *   <li>Enemy rendering on the game canvas</li>
     *   <li>Wave preview displays</li>
     *   <li>Kill/defeat animations</li>
     *   <li>Statistics and UI elements</li>
     * </ul>
     *
     * @return The resource ID {@code R.drawable.ic_enemy_bug} pointing to the
     *         bug-shaped vector drawable asset.
     */
    @Override
    public int getIconResId() {
        // Bug icon represents the "crawler" nature - small, numerous, persistent
        return R.drawable.ic_enemy_bug;
    }

}
