package edu.commonwealthu.lastserverstanding.game;

import android.graphics.PointF;
import android.graphics.RectF;

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
    
    private int cellSize;
    private int gridWidth;
    private int gridHeight;
    
    // Grid cells containing entities
    private Map<String, List<Tower>> towerGrid;
    private Map<String, List<Enemy>> enemyGrid;
    private Map<String, List<Projectile>> projectileGrid;
    
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
     * Get all towers within range of a position
     * @param position Center position
     * @param range Search radius
     * @return List of towers within range
     */
    public List<Tower> getTowersInRange(PointF position, float range) {
        List<Tower> result = new ArrayList<>();
        
        List<String> cellsToCheck = getCellsInRange(position, range);
        float rangeSquared = range * range;
        
        for (String cellKey : cellsToCheck) {
            List<Tower> cellTowers = towerGrid.get(cellKey);
            if (cellTowers != null) {
                for (Tower tower : cellTowers) {
                    if (distanceSquared(position, tower.getPosition()) <= rangeSquared) {
                        result.add(tower);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Check if a position is blocked by a tower
     * @param position Position to check
     * @param blockRadius Radius around towers that blocks
     * @return true if position is blocked
     */
    public boolean isPositionBlocked(PointF position, float blockRadius) {
        String cellKey = getCellKey(position);
        List<String> cellsToCheck = getCellsInRange(position, blockRadius);
        
        float radiusSquared = blockRadius * blockRadius;
        
        for (String cell : cellsToCheck) {
            List<Tower> cellTowers = towerGrid.get(cell);
            if (cellTowers != null) {
                for (Tower tower : cellTowers) {
                    if (distanceSquared(position, tower.getPosition()) <= radiusSquared) {
                        return true;
                    }
                }
            }
        }
        
        return false;
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
     * Get the enemy furthest along its path within range
     * Useful for "last" targeting strategy
     */
    public Enemy getFurthestEnemy(PointF position, float range) {
        List<Enemy> enemiesInRange = getEnemiesInRange(position, range);
        
        if (enemiesInRange.isEmpty()) {
            return null;
        }
        
        Enemy furthest = null;
        float furthestProgress = -1;
        
        for (Enemy enemy : enemiesInRange) {
            // Progress is measured by how far along the path the enemy is
            float progress = getPathProgress(enemy);
            if (progress > furthestProgress) {
                furthest = enemy;
                furthestProgress = progress;
            }
        }
        
        return furthest;
    }
    
    /**
     * Calculate how far an enemy has progressed along its path (0 to 1)
     */
    private float getPathProgress(Enemy enemy) {
        List<PointF> path = enemy.getPath();
        if (path.isEmpty()) return 0;
        
        // Calculate total path length
        float totalLength = 0;
        for (int i = 1; i < path.size(); i++) {
            totalLength += distance(path.get(i-1), path.get(i));
        }
        
        if (totalLength == 0) return 0;
        
        // Calculate how much of the path has been completed
        float completedLength = 0;
        PointF currentPos = enemy.getPosition();
        
        // Find which segment the enemy is on and add previous segments
        for (int i = 1; i < path.size(); i++) {
            float segmentLength = distance(path.get(i-1), path.get(i));
            
            // Check if enemy is on this segment
            if (isPointOnSegment(currentPos, path.get(i-1), path.get(i))) {
                completedLength += distance(path.get(i-1), currentPos);
                break;
            }
            
            completedLength += segmentLength;
        }
        
        return completedLength / totalLength;
    }
    
    /**
     * Check if a point is approximately on a line segment
     */
    private boolean isPointOnSegment(PointF point, PointF segmentStart, PointF segmentEnd) {
        float distToStart = distance(point, segmentStart);
        float distToEnd = distance(point, segmentEnd);
        float segmentLength = distance(segmentStart, segmentEnd);
        
        // Point is on segment if sum of distances equals segment length (with small tolerance)
        return Math.abs((distToStart + distToEnd) - segmentLength) < 10;
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
    
    /**
     * Calculate distance between two points
     */
    private float distance(PointF a, PointF b) {
        return (float) Math.sqrt(distanceSquared(a, b));
    }
    
    /**
     * Get collision bounds for debugging/visualization
     */
    public RectF getCellBounds(int cellX, int cellY) {
        return new RectF(
            cellX * cellSize,
            cellY * cellSize,
            (cellX + 1) * cellSize,
            (cellY + 1) * cellSize
        );
    }
}
