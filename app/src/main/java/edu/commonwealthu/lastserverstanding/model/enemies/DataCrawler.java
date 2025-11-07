package edu.commonwealthu.lastserverstanding.model.enemies;

import android.graphics.PointF;

import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.model.Enemy;

/**
 * Data Crawler - Fast, low health enemy
 * Represents web scraping bots
 */
public class DataCrawler extends Enemy {
    
    /**
     * Constructor with default stats
     */
    public DataCrawler(List<PointF> path) {
        super(
            UUID.randomUUID().toString(),
            path,
            50f,  // maxHealth - low health
            120f, // speed - fast movement
            10,   // reward
            5     // damage to data center
        );
    }
    
    @Override
    protected void updateSpecific(float deltaTime) {
        // Data Crawlers don't have special behavior during update
        // They just move fast along their path
    }
    
    @Override
    public String getType() {
        return "Data Crawler";
    }
    
    @Override
    public void triggerAbility() {
        // Data Crawlers have no special ability
        // Their strength is in their speed and numbers
    }
    
    @Override
    protected void onDeath() {
        super.onDeath();
        // Could add particle effect or sound here later
    }
}
