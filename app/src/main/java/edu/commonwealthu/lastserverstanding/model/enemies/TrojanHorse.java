package edu.commonwealthu.lastserverstanding.model.enemies;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;

/**
 * Trojan Horse - The heavy, tank-class enemy in Last Server Standing.
 *
 * <p>This enemy represents sophisticated malware that disguises itself as legitimate
 * software while carrying a devastating payload. Trojan Horses are the most dangerous
 * standard enemy type, requiring significant firepower to eliminate.</p>
 *
 * <h2>Gameplay Role</h2>
 * <p>Trojan Horses serve as "tank" enemies that test the player's overall DPS
 * (damage per second) capability. Their slow speed gives towers ample time to attack,
 * but their high health demands either upgraded towers or concentrated fire from
 * multiple sources. A leaking Trojan Horse is a major setback.</p>
 *
 * <h2>Spawn Behavior</h2>
 * <ul>
 *   <li>First appears at Wave 10 - marks late-game difficulty</li>
 *   <li>Spawns less frequently than weaker enemies</li>
 *   <li>Often used as "mini-boss" enemies at wave intervals</li>
 *   <li>May be mixed with faster enemies to split player attention</li>
 *   <li>Wave composition may feature Trojans as the primary threat</li>
 * </ul>
 *
 * <h2>Visual Representation</h2>
 * <p>Rendered in GREEN color with a horse icon ({@code ic_enemy_horse}), referencing
 * the mythological Trojan Horse. Green was chosen to suggest deception (money/greed)
 * and to complete the RGB color differentiation with other enemy types.</p>
 *
 * <h2>Balance Considerations</h2>
 * <p>Stats follow a 4x multiplier from baseline Data Crawler:</p>
 * <ul>
 *   <li>4x health (200 vs 50) - true tank that absorbs tower damage</li>
 *   <li>Slowest speed (80 vs 120) - necessary to allow counterplay</li>
 *   <li>4x reward (20 vs 5) - high-value target worth prioritizing</li>
 *   <li>4x damage (20 vs 5) - leaks are devastating, encourages focus fire</li>
 * </ul>
 *
 * <h2>Strategic Implications</h2>
 * <p>Players must decide whether to:</p>
 * <ul>
 *   <li>Focus fire on Trojans to prevent catastrophic leaks</li>
 *   <li>Let splash damage handle them while targeting faster enemies</li>
 *   <li>Build specialized high-damage towers for Trojan elimination</li>
 * </ul>
 *
 * @author Ethan Wight
 * @see Enemy
 * @see DataCrawler
 * @see VirusBot
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
     * Constructs a new Trojan Horse enemy with predefined stats.
     *
     * <p>The Trojan Horse is initialized with the following characteristics:</p>
     * <ul>
     *   <li><b>Health:</b> 200 - High durability, tank-class enemy</li>
     *   <li><b>Speed:</b> 80 - Slowest movement, allows sustained damage</li>
     *   <li><b>Reward:</b> 20 credits - High payout for difficult kill</li>
     *   <li><b>Damage:</b> 20 - Devastating damage to data center on leak</li>
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
     * <p>This name is used in UI elements such as wave previews, kill notifications,
     * and statistics tracking. The name "Trojan Horse" references the famous Greek
     * mythological deception, representing malware that appears harmless but carries
     * a destructive payload.</p>
     *
     * @return The string "Trojan Horse" as the human-readable type identifier.
     */
    @Override
    public String getType() {
        return "Trojan Horse";
    }

    /**
     * Returns the primary color used to render this enemy.
     *
     * <p>Trojan Horses are rendered in GREEN ({@code 0xFF00FF00}), chosen to:</p>
     * <ul>
     *   <li>Complete the RGB color scheme (Red-Blue-Green) for enemy types</li>
     *   <li>Suggest themes of deception and false security (green = "safe")</li>
     *   <li>Provide maximum contrast with both red and blue enemies</li>
     *   <li>Create visual hierarchy - green for the most dangerous standard enemy</li>
     * </ul>
     *
     * <p>The color is applied to the enemy's body/shape in the game renderer
     * and may be used for health bar tinting or particle effects.</p>
     *
     * @return {@link android.graphics.Color#GREEN} (0xFF00FF00) for rendering.
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
     * <p>The horse icon ({@code ic_enemy_horse}) directly references the mythological
     * Trojan Horse, making the enemy type instantly recognizable. This icon is used for:</p>
     * <ul>
     *   <li>Enemy rendering on the game canvas</li>
     *   <li>Wave preview displays to warn of incoming tanks</li>
     *   <li>Kill/defeat animations and celebration effects</li>
     *   <li>Statistics screens showing high-value kills</li>
     * </ul>
     *
     * @return The resource ID {@code R.drawable.ic_enemy_horse} pointing to the
     *         horse-shaped vector drawable asset.
     */
    @Override
    public int getIconResId() {
        // Horse icon directly references the Trojan Horse mythology
        // Instantly communicates this is a high-threat, deceptive enemy
        return R.drawable.ic_enemy_horse;
    }

}
