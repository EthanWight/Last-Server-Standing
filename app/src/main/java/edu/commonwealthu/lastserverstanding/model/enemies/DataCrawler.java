package edu.commonwealthu.lastserverstanding.model.enemies;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;

/**
 * Data Crawler - Fast, low health enemy.
 * Represents web scraping bots.
 *
 * @author Ethan Wight
 */
public class DataCrawler extends Enemy {
    
    /**
     * Constructor with default stats.
     *
     * @param path The path this enemy will follow.
     */
    public DataCrawler(List<PointF> path) {
        super(
            UUID.randomUUID().toString(),
            path,
            50f,  // maxHealth - low health
            120f, // speed - fast movement
            5,    // reward - reduced for balance
            5     // damage to data center
        );
    }

    /**
     * Get the enemy type name.
     *
     * @return The type name of this enemy.
     */
    @Override
    public String getType() {
        return "Data Crawler";
    }

    /**
     * Get the enemy's color for rendering.
     *
     * @return Color value (use android.graphics.Color constants).
     */
    @Override
    public int getColor() {
        return android.graphics.Color.RED;
    }

    /**
     * Get the enemy's icon resource ID for rendering.
     *
     * @return Drawable resource ID.
     */
    @Override
    public int getIconResId() {
        return R.drawable.ic_enemy_bug;
    }

}
