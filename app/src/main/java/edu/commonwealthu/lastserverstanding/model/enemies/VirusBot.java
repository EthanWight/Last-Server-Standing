package edu.commonwealthu.lastserverstanding.model.enemies;

import android.graphics.Color;
import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;

/**
 * Virus Bot - Medium health, moderate speed enemy
 * Appears starting from wave 5
 * Represents virus-infected systems
 */
public class VirusBot extends Enemy {

    /**
     * Constructor with default stats
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

    @Override
    protected void updateSpecific(float deltaTime) {
        // Virus Bots don't have special behavior during update
    }

    @Override
    public String getType() {
        return "Virus Bot";
    }

    @Override
    public int getColor() {
        return Color.BLUE;
    }

    @Override
    public int getIconResId() {
        return R.drawable.ic_enemy_virus;
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        // Could add special death effect here later
    }
}
