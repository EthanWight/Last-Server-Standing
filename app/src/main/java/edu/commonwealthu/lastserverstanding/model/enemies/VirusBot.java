package edu.commonwealthu.lastserverstanding.model.enemies;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;

/**
 * Virus Bot - The mid-tier enemy representing infected systems in Last Server Standing.
 *
 * <p>This enemy represents virus-infected computer systems that have been compromised
 * and turned into attack vectors. Virus Bots are more resilient than Data Crawlers
 * and pose a significant threat to unprepared defenses.</p>
 *
 * <h2>Gameplay Role</h2>
 * <p>Virus Bots serve as the "standard" enemy that forces players to upgrade their
 * defenses. They cannot be easily one-shot by basic towers and require either
 * upgraded damage or sustained fire to eliminate. Their appearance marks the
 * transition from early-game to mid-game difficulty.</p>
 *
 * <h2>Spawn Behavior</h2>
 * <ul>
 *   <li>First appears at Wave 5 - signals difficulty increase</li>
 *   <li>Gradually replaces Data Crawlers as the primary threat</li>
 *   <li>Often mixed with Data Crawlers to test resource allocation</li>
 *   <li>May spawn in smaller groups than Data Crawlers due to higher individual threat</li>
 * </ul>
 *
 * <h2>Visual Representation</h2>
 * <p>Rendered in BLUE color with a virus icon ({@code ic_enemy_virus}), representing
 * the digital/cyber nature of computer viruses. Blue was chosen to contrast with
 * the red Data Crawler while maintaining threat visibility.</p>
 *
 * <h2>Balance Considerations</h2>
 * <p>Stats are designed as a 2x multiplier from the baseline Data Crawler:</p>
 * <ul>
 *   <li>2x health (100 vs 50) - requires more tower investment to kill</li>
 *   <li>Slightly slower (100 vs 120) - compensates for higher durability</li>
 *   <li>2x reward (10 vs 5) - fair compensation for difficulty</li>
 *   <li>2x damage (10 vs 5) - makes leaks more punishing</li>
 * </ul>
 *
 * @author Ethan Wight
 * @see Enemy
 * @see DataCrawler
 * @see TrojanHorse
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
     * Constructs a new Virus Bot enemy with predefined stats.
     *
     * <p>The Virus Bot is initialized with the following characteristics:</p>
     * <ul>
     *   <li><b>Health:</b> 100 - Medium durability, requires sustained damage</li>
     *   <li><b>Speed:</b> 100 - Moderate speed, balanced movement</li>
     *   <li><b>Reward:</b> 10 credits - Fair payout for increased difficulty</li>
     *   <li><b>Damage:</b> 10 - Significant damage to data center on leak</li>
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
     * <p>This name is used in UI elements such as wave previews, kill notifications,
     * and statistics tracking. The name "Virus Bot" represents compromised systems
     * that have been infected and weaponized against the player's server.</p>
     *
     * @return The string "Virus Bot" as the human-readable type identifier.
     */
    @Override
    public String getType() {
        return "Virus Bot";
    }

    /**
     * Returns the primary color used to render this enemy.
     *
     * <p>Virus Bots are rendered in BLUE ({@code 0xFF0000FF}), chosen to:</p>
     * <ul>
     *   <li>Contrast distinctly with red Data Crawlers for quick identification</li>
     *   <li>Evoke the "digital/cyber" aesthetic associated with computer viruses</li>
     *   <li>Provide a cool color to visually suggest calculated, systematic threat</li>
     *   <li>Maintain visibility while differentiating from green TrojanHorse</li>
     * </ul>
     *
     * <p>The color is applied to the enemy's body/shape in the game renderer
     * and may be used for health bar tinting or particle effects.</p>
     *
     * @return {@link android.graphics.Color#BLUE} (0xFF0000FF) for rendering.
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
     * <p>The virus icon ({@code ic_enemy_virus}) visually represents the infectious
     * and dangerous nature of this enemy type. This icon is used for:</p>
     * <ul>
     *   <li>Enemy rendering on the game canvas</li>
     *   <li>Wave preview displays showing upcoming threats</li>
     *   <li>Kill/defeat animations and effects</li>
     *   <li>Statistics screens and achievement tracking</li>
     * </ul>
     *
     * @return The resource ID {@code R.drawable.ic_enemy_virus} pointing to the
     *         virus-shaped vector drawable asset.
     */
    @Override
    public int getIconResId() {
        // Virus icon clearly communicates the "infected system" theme
        return R.drawable.ic_enemy_virus;
    }

}
