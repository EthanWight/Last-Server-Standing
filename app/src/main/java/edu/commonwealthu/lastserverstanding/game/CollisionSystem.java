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
 * Spatial partitioning system for efficient collision detection.
 * Divides the game world into a grid to reduce collision checks.
 *
 * @author Ethan Wight
 */
public class CollisionSystem {
    
    private final int cellSize;
    private final int gridWidth;
    private final int gridHeight;
    
    // Grid cells containing entities (using Long keys to avoid string allocation)
    private final Map<Long, List<Tower>> towerGrid;
    private final Map<Long, List<Enemy>> enemyGrid;
    private final Map<Long, List<Projectile>> projectileGrid;

    // Object pools for ArrayLists (reduce per-frame allocations)
    private final List<List<Tower>> towerListPool;
    private final List<List<Enemy>> enemyListPool;
    private final List<List<Projectile>> projectileListPool;

    /**
     * Constructor.
     *
     * @param worldWidth Width of game world in pixels.
     * @param worldHeight Height of game world in pixels.
     * @param cellSize Size of each spatial partition cell.
     */
    public CollisionSystem(int worldWidth, int worldHeight, int cellSize) {
        this.cellSize = cellSize;
        this.gridWidth = (worldWidth / cellSize) + 1;
        this.gridHeight = (worldHeight / cellSize) + 1;

        towerGrid = new HashMap<>();
        enemyGrid = new HashMap<>();
        projectileGrid = new HashMap<>();

        towerListPool = new ArrayList<>();
        enemyListPool = new ArrayList<>();
        projectileListPool = new ArrayList<>();
    }
    
    /**
     * Update all entity positions in the spatial grid.
     * Should be called each frame.
     *
     * @param towers The list of towers.
     * @param enemies The list of enemies.
     * @param projectiles The list of projectiles.
     */
    public void update(List<Tower> towers, List<Enemy> enemies, List<Projectile> projectiles) {
        // Return lists to pool and clear grids
        for (List<Tower> list : towerGrid.values()) {
            list.clear();
            towerListPool.add(list);
        }
        towerGrid.clear();

        for (List<Enemy> list : enemyGrid.values()) {
            list.clear();
            enemyListPool.add(list);
        }
        enemyGrid.clear();

        for (List<Projectile> list : projectileGrid.values()) {
            list.clear();
            projectileListPool.add(list);
        }
        projectileGrid.clear();

        // Insert towers into grid (use pooled lists)
        for (Tower tower : towers) {
            long cellKey = getCellKey(tower.getPosition());
            List<Tower> list = towerGrid.get(cellKey);
            if (list == null) {
                list = towerListPool.isEmpty() ? new ArrayList<>() : towerListPool.remove(towerListPool.size() - 1);
                towerGrid.put(cellKey, list);
            }
            list.add(tower);
        }

        // Insert enemies into grid (use pooled lists)
        for (Enemy enemy : enemies) {
            long cellKey = getCellKey(enemy.getPosition());
            List<Enemy> list = enemyGrid.get(cellKey);
            if (list == null) {
                list = enemyListPool.isEmpty() ? new ArrayList<>() : enemyListPool.remove(enemyListPool.size() - 1);
                enemyGrid.put(cellKey, list);
            }
            list.add(enemy);
        }

        // Insert projectiles into grid (use pooled lists)
        for (Projectile projectile : projectiles) {
            long cellKey = getCellKey(projectile.getPosition());
            List<Projectile> list = projectileGrid.get(cellKey);
            if (list == null) {
                list = projectileListPool.isEmpty() ? new ArrayList<>() : projectileListPool.remove(projectileListPool.size() - 1);
                projectileGrid.put(cellKey, list);
            }
            list.add(projectile);
        }
    }
    
    /**
     * Get all enemies within range of a position.
     *
     * @param position Center position.
     * @param range Search radius.
     * @return List of enemies within range.
     */
    public List<Enemy> getEnemiesInRange(PointF position, float range) {
        List<Enemy> result = new ArrayList<>();

        // Calculate which cells to check based on range
        List<Long> cellsToCheck = getCellsInRange(position, range);

        float rangeSquared = range * range;

        for (Long cellKey : cellsToCheck) {
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
     * Get the closest enemy to a position within range.
     *
     * @param position Center position.
     * @param range Maximum search radius.
     * @return Closest enemy, or null if none in range.
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
     * Get all grid cells within range of a position (returns long keys).
     *
     * @param position The center position.
     * @param range The search radius.
     * @return List of cell keys within range.
     */
    private List<Long> getCellsInRange(PointF position, float range) {
        List<Long> cells = new ArrayList<>();

        int centerX = (int) (position.x / cellSize);
        int centerY = (int) (position.y / cellSize);
        int cellRadius = (int) Math.ceil(range / cellSize);

        for (int x = centerX - cellRadius; x <= centerX + cellRadius; x++) {
            for (int y = centerY - cellRadius; y <= centerY + cellRadius; y++) {
                if (x >= 0 && x < gridWidth && y >= 0 && y < gridHeight) {
                    // Encode x and y into a single long
                    cells.add(((long) x << 32) | (y & 0xFFFFFFFFL));
                }
            }
        }

        return cells;
    }
    
    /**
     * Get cell key for a position (uses long to avoid string allocation).
     * Encodes x and y coordinates into a single long value.
     *
     * @param position The position.
     * @return The cell key.
     */
    private long getCellKey(PointF position) {
        int x = (int) (position.x / cellSize);
        int y = (int) (position.y / cellSize);
        // Encode x and y into a single long: upper 32 bits = x, lower 32 bits = y
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }
    
    /**
     * Calculate squared distance between two points (faster than distance).
     *
     * @param a The first position.
     * @param b The second position.
     * @return The squared distance.
     */
    private float distanceSquared(PointF a, PointF b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        return dx * dx + dy * dy;
    }
}
