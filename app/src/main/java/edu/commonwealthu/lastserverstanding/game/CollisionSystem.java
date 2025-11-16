package edu.commonwealthu.lastserverstanding.game;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.commonwealthu.lastserverstanding.model.Enemy;
import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Spatial partitioning system for efficient collision detection
 * Divides the game world into a grid to reduce collision checks
 */
public class CollisionSystem {
    
    private final int cellSize;
    private final int gridWidth;
    private final int gridHeight;
    
    // Grid cells containing entities
    private final Map<String, List<Tower>> towerGrid;
    private final Map<String, List<Enemy>> enemyGrid;
    private final Map<String, List<Projectile>> projectileGrid;
    
    /**
     * Constructor
     * @param worldWidth Width of game world in pixels
     * @param worldHeight Height of game world in pixels
     * @param cellSize Size of each spatial partition cell
     */
    public CollisionSystem(int worldWidth, int worldHeight, int cellSize) {
        this.cellSize = cellSize;
        this.gridWidth = (worldWidth / cellSize) + 1;
        this.gridHeight = (worldHeight / cellSize) + 1;
        
        towerGrid = new HashMap<>();
        enemyGrid = new HashMap<>();
        projectileGrid = new HashMap<>();
    }
    
    /**
     * Update all entity positions in the spatial grid
     * Should be called each frame
     */
    public void update(List<Tower> towers, List<Enemy> enemies, List<Projectile> projectiles) {
        // Clear previous frame's data
        towerGrid.clear();
        enemyGrid.clear();
        projectileGrid.clear();
        
        // Insert towers into grid
        for (Tower tower : towers) {
            String cellKey = getCellKey(tower.getPosition());
            towerGrid.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(tower);
        }
        
        // Insert enemies into grid
        for (Enemy enemy : enemies) {
            String cellKey = getCellKey(enemy.getPosition());
            enemyGrid.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(enemy);
        }
        
        // Insert projectiles into grid
        for (Projectile projectile : projectiles) {
            String cellKey = getCellKey(projectile.getPosition());
            projectileGrid.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(projectile);
        }
    }
    
    /**
     * Get all enemies within range of a position
     * @param position Center position
     * @param range Search radius
     * @return List of enemies within range
     */
    public List<Enemy> getEnemiesInRange(PointF position, float range) {
        List<Enemy> result = new ArrayList<>();
        
        // Calculate which cells to check based on range
        List<String> cellsToCheck = getCellsInRange(position, range);
        
        float rangeSquared = range * range;
        
        for (String cellKey : cellsToCheck) {
            List<Enemy> cellEnemies = enemyGrid.get(cellKey);
            if (cellEnemies != null) {
                for (Enemy enemy : cellEnemies) {
                    if (distanceSquared(position, enemy.getPosition()) <= rangeSquared) {
                        result.add(enemy);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Get the closest enemy to a position within range
     * @param position Center position
     * @param range Maximum search radius
     * @return Closest enemy, or null if none in range
     */
    public Enemy getClosestEnemy(PointF position, float range) {
        List<Enemy> enemiesInRange = getEnemiesInRange(position, range);
        
        if (enemiesInRange.isEmpty()) {
            return null;
        }
        
        Enemy closest = null;
        float closestDistanceSquared = Float.MAX_VALUE;
        
        for (Enemy enemy : enemiesInRange) {
            float distSquared = distanceSquared(position, enemy.getPosition());
            if (distSquared < closestDistanceSquared) {
                closest = enemy;
                closestDistanceSquared = distSquared;
            }
        }
        
        return closest;
    }
    
    /**
     * Get all grid cells within range of a position
     */
    private List<String> getCellsInRange(PointF position, float range) {
        List<String> cells = new ArrayList<>();
        
        int centerX = (int) (position.x / cellSize);
        int centerY = (int) (position.y / cellSize);
        int cellRadius = (int) Math.ceil(range / cellSize);
        
        for (int x = centerX - cellRadius; x <= centerX + cellRadius; x++) {
            for (int y = centerY - cellRadius; y <= centerY + cellRadius; y++) {
                if (x >= 0 && x < gridWidth && y >= 0 && y < gridHeight) {
                    cells.add(x + "," + y);
                }
            }
        }
        
        return cells;
    }
    
    /**
     * Get cell key for a position
     */
    private String getCellKey(PointF position) {
        int x = (int) (position.x / cellSize);
        int y = (int) (position.y / cellSize);
        return x + "," + y;
    }
    
    /**
     * Calculate squared distance between two points (faster than distance)
     */
    private float distanceSquared(PointF a, PointF b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        return dx * dx + dy * dy;
    }
}
