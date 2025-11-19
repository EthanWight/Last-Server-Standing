package edu.commonwealthu.lastserverstanding.model.enemies;

import android.graphics.Color;
import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;

/**
 * Trojan Horse - High health, slow but tanky enemy
 * Appears starting from wave 10
 * Represents sophisticated malware systems
 */
public class TrojanHorse extends Enemy {

    /**
     * Constructor with default stats
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

    @Override
    protected void updateSpecific(float deltaTime) {
        // Trojan Horses are slow but resilient
        // No special behavior during update
    }

    @Override
    public String getType() {
        return "Trojan Horse";
    }

    @Override
    public int getColor() {
        return Color.GREEN;
    }

    @Override
    public int getIconResId() {
        return R.drawable.ic_enemy_horse;
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        // Could add explosion effect or spawn mini enemies here later
    }
}
