package edu.commonwealthu.lastserverstanding.model.enemies;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;

/**
 * Virus Bot - Medium health, moderate speed enemy.
 * Appears starting from wave 5.
 * Represents virus-infected systems.
 *
 * @author Ethan Wight
 */
public class VirusBot extends Enemy {

    /**
     * Constructor with default stats.
     *
     * @param path The path this enemy will follow.
     */
    public VirusBot(List<PointF> path) {
        super(
            UUID.randomUUID().toString(),
            path,
            100f,  // maxHealth - double the Data Crawler
            100f,  // speed - slightly slower than Data Crawler
            10,    // reward - reduced for balance
            10     // damage to data center - double the Data Crawler
        );
    }

    /**
     * Type-specific update logic (override in subclasses).
     *
     * @param deltaTime Time since last update in seconds.
     */
    @Override
    protected void updateSpecific(float deltaTime) {
        // Virus Bots don't have special behavior during update
    }

    /**
     * Get the enemy type name.
     *
     * @return The type name of this enemy.
     */
    @Override
    public String getType() {
        return "Virus Bot";
    }

    /**
     * Get the enemy's color for rendering.
     *
     * @return Color value (use android.graphics.Color constants).
     */
    @Override
    public int getColor() {
        return android.graphics.Color.BLUE;
    }

    /**
     * Get the enemy's icon resource ID for rendering.
     *
     * @return Drawable resource ID.
     */
    @Override
    public int getIconResId() {
        return R.drawable.ic_enemy_virus;
    }

    /**
     * Called when enemy dies.
     */
    @Override
    protected void onDeath() {
        super.onDeath();
        // Could add special death effect here later
    }
}
