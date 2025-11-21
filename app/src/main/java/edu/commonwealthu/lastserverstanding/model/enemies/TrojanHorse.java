package edu.commonwealthu.lastserverstanding.model.enemies;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;

/**
 * Trojan Horse - High health, slow but tanky enemy.
 * Appears starting from wave 10.
 * Represents sophisticated malware systems.
 *
 * @author Ethan Wight
 */
public class TrojanHorse extends Enemy {

    /**
     * Constructor with default stats.
     *
     * @param path The path this enemy will follow.
     */
    public TrojanHorse(List<PointF> path) {
        super(
            UUID.randomUUID().toString(),
            path,
            200f,  // maxHealth - 4x the Data Crawler
            80f,   // speed - slower but very tanky
            20,    // reward - reduced for balance
            20     // damage to data center - 4x the Data Crawler
        );
    }

    /**
     * Get the enemy type name.
     *
     * @return The type name of this enemy.
     */
    @Override
    public String getType() {
        return "Trojan Horse";
    }

    /**
     * Get the enemy's color for rendering.
     *
     * @return Color value (use android.graphics.Color constants).
     */
    @Override
    public int getColor() {
        return android.graphics.Color.GREEN;
    }

    /**
     * Get the enemy's icon resource ID for rendering.
     *
     * @return Drawable resource ID.
     */
    @Override
    public int getIconResId() {
        return R.drawable.ic_enemy_horse;
    }

}
