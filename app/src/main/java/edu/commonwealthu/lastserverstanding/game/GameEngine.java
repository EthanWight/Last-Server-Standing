package edu.commonwealthu.lastserverstanding.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.commonwealthu.lastserverstanding.model.Enemy;
import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Core game engine managing all game logic and state
 */
public class GameEngine {
    
    // Game state
    private List<Tower> towers;
    private List<Enemy> enemies;
    private List<Projectile> projectiles;
    
    private int currentWave;
    private int resources;
    private int dataCenterHealth;
    private long score;
    private boolean isPaused;
    
    // FPS tracking
    private int fps;
    private long lastFpsTime;
    private int frameCount;
    
    // Game systems
    private CollisionSystem collisionSystem;
    private Pathfinding pathfinding;
    private WaveManager waveManager;
    
    // World dimensions
    private int worldWidth;
    private int worldHeight;
    private int gridSize;
    
    // Game constants
    private static final int STARTING_RESOURCES = 500;
    private static final int STARTING_HEALTH = 100;
    
    /**
     * Constructor
     */
    public GameEngine() {
        towers = new ArrayList<>();
        enemies = new ArrayList<>();
        projectiles = new ArrayList<>();
        
        currentWave = 0;
        resources = STARTING_RESOURCES;
        dataCenterHealth = STARTING_HEALTH;
        score = 0;
        isPaused = false;
        
        fps = 0;
        lastFpsTime = System.currentTimeMillis();
        frameCount = 0;
        
        // Initialize world dimensions (will be set by GameView)
        worldWidth = 1280;
        worldHeight = 720;
        gridSize = 64;
        
        // Initialize game systems
        collisionSystem = new CollisionSystem(worldWidth, worldHeight, gridSize * 2);
        pathfinding = new Pathfinding(worldWidth / gridSize, worldHeight / gridSize, gridSize);
        waveManager = new WaveManager();
    }
    
    /**
     * Set world dimensions (called by GameView after initialization)
     */
    public void setWorldDimensions(int width, int height, int cellSize) {
        this.worldWidth = width;
        this.worldHeight = height;
        this.gridSize = cellSize;
        
        // Reinitialize systems with correct dimensions
        collisionSystem = new CollisionSystem(width, height, cellSize * 2);
        pathfinding = new Pathfinding(width / cellSize, height / cellSize, cellSize);
    }
    
    /**
     * Update game state
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        if (isPaused) return;
        
        // Update FPS counter
        updateFPS();
        
        // Update collision system
        collisionSystem.update(towers, enemies, projectiles);
        
        // Update wave manager
        waveManager.update(deltaTime, this);
        
        // Update all towers with collision system for better targeting
        for (Tower tower : towers) {
            tower.update(deltaTime);
            
            // Use collision system for more efficient target acquisition
            if (tower.getTarget() == null || !tower.getTarget().isAlive()) {
                Enemy closestEnemy = collisionSystem.getClosestEnemy(tower.getPosition(), tower.getRange());
                tower.setTarget(closestEnemy);
            }
            
            // Fire if possible
            Projectile projectile = tower.fire();
            if (projectile != null) {
                projectiles.add(projectile);
            }
        }
        
        // Update all enemies
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            enemy.update(deltaTime);
            
            // Check if enemy reached end
            if (enemy.hasReachedEnd()) {
                dataCenterHealth -= enemy.getDamage();
                enemyIterator.remove();
            }
            
            // Remove dead enemies
            if (!enemy.isAlive()) {
                resources += enemy.getReward();
                score += enemy.getReward() * 10;
                enemyIterator.remove();
            }
        }
        
        // Update all projectiles
        Iterator<Projectile> projectileIterator = projectiles.iterator();
        while (projectileIterator.hasNext()) {
            Projectile projectile = projectileIterator.next();
            projectile.update(deltaTime);
            
            // Remove projectiles that have hit
            if (projectile.hasHit()) {
                projectileIterator.remove();
            }
        }
        
        // Check for game over
        if (dataCenterHealth <= 0) {
            gameOver();
        }
    }
    
    /**
     * Render all game elements
     * @param canvas Canvas to draw on
     * @param paint Paint object for drawing
     */
    public void render(Canvas canvas, Paint paint) {
        // Draw towers
        paint.setStyle(Paint.Style.FILL);
        for (Tower tower : towers) {
            drawTower(canvas, paint, tower);
        }
        
        // Draw enemies
        for (Enemy enemy : enemies) {
            drawEnemy(canvas, paint, enemy);
        }
        
        // Draw projectiles
        for (Projectile projectile : projectiles) {
            drawProjectile(canvas, paint, projectile);
        }
    }
    
    /**
     * Draw a tower (placeholder - will be enhanced later)
     */
    private void drawTower(Canvas canvas, Paint paint, Tower tower) {
        PointF pos = tower.getPosition();
        
        // Draw tower body
        paint.setColor(tower.isCorrupted() ? Color.RED : Color.CYAN);
        canvas.drawCircle(pos.x, pos.y, 20, paint);
        
        // Draw range indicator if selected
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(50, 0, 255, 255));
        paint.setStrokeWidth(2);
        canvas.drawCircle(pos.x, pos.y, tower.getRange(), paint);
        paint.setStyle(Paint.Style.FILL);
    }
    
    /**
     * Draw an enemy (placeholder - will be enhanced later)
     */
    private void drawEnemy(Canvas canvas, Paint paint, Enemy enemy) {
        PointF pos = enemy.getPosition();
        
        // Draw enemy body
        paint.setColor(Color.RED);
        canvas.drawCircle(pos.x, pos.y, 15, paint);
        
        // Draw health bar
        paint.setColor(Color.GREEN);
        float healthBarWidth = 30 * enemy.getHealthPercentage();
        canvas.drawRect(
            pos.x - 15, 
            pos.y - 25, 
            pos.x - 15 + healthBarWidth, 
            pos.y - 20, 
            paint
        );
    }
    
    /**
     * Draw a projectile (placeholder - will be enhanced later)
     */
    private void drawProjectile(Canvas canvas, Paint paint, Projectile projectile) {
        PointF pos = projectile.getPosition();
        paint.setColor(Color.YELLOW);
        canvas.drawCircle(pos.x, pos.y, 5, paint);
    }
    
    /**
     * Update FPS counter
     */
    private void updateFPS() {
        frameCount++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFpsTime >= 1000) {
            fps = frameCount;
            frameCount = 0;
            lastFpsTime = currentTime;
        }
    }
    
    /**
     * Handle tap on game view
     */
    public void handleTap(PointF worldPos) {
        // Will be implemented for tower placement
        // For now, just log the position
        System.out.println("Tapped at: " + worldPos.x + ", " + worldPos.y);
    }
    
    /**
     * Add a tower to the game
     */
    public boolean addTower(Tower tower) {
        if (resources >= tower.getCost()) {
            towers.add(tower);
            resources -= tower.getCost();
            return true;
        }
        return false;
    }
    
    /**
     * Add an enemy to the game
     */
    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }
    
    /**
     * Add resources
     */
    public void addResources(int amount) {
        resources += amount;
    }
    
    /**
     * Add score
     */
    public void addScore(long amount) {
        score += amount;
    }
    
    /**
     * Handle game over
     */
    private void gameOver() {
        isPaused = true;
        // Game over logic will be implemented later
        System.out.println("Game Over! Final Score: " + score);
    }
    
    /**
     * Start next wave
     */
    public void startNextWave() {
        waveManager.startNextWave(this);
        currentWave = waveManager.getCurrentWave();
    }

    /**
     * Capture current game state for saving
     */
    public edu.commonwealthu.lastserverstanding.data.models.GameState captureGameState() {
        edu.commonwealthu.lastserverstanding.data.models.GameState state = new edu.commonwealthu.lastserverstanding.data.models.GameState();
        state.currentWave = currentWave;
        state.resources = resources;
        state.dataCenterHealth = dataCenterHealth;
        state.score = score;

        // Capture tower data
        for (Tower tower : towers) {
            state.towers.add(new edu.commonwealthu.lastserverstanding.data.models.GameState.TowerData(
                tower.getType(),
                tower.getPosition(),
                tower.getLevel(),
                tower.isCorrupted()
            ));
        }

        // Capture enemy data
        for (Enemy enemy : enemies) {
            state.enemies.add(new edu.commonwealthu.lastserverstanding.data.models.GameState.EnemyData(
                enemy.getType(),
                enemy.getPosition(),
                enemy.getHealth(),
                enemy.getCurrentPathIndex(),
                enemy.getPath()
            ));
        }

        return state;
    }

    /**
     * Restore game state from saved data
     */
    public void restoreGameState(edu.commonwealthu.lastserverstanding.data.models.GameState state) {
        // Clear current state
        towers.clear();
        enemies.clear();
        projectiles.clear();

        // Restore basic values
        currentWave = state.currentWave;
        resources = state.resources;
        dataCenterHealth = state.dataCenterHealth;
        score = state.score;

        // Restore towers
        for (edu.commonwealthu.lastserverstanding.data.models.GameState.TowerData towerData : state.towers) {
            Tower tower = createTowerFromData(towerData);
            if (tower != null) {
                towers.add(tower);
            }
        }

        // Restore enemies
        for (edu.commonwealthu.lastserverstanding.data.models.GameState.EnemyData enemyData : state.enemies) {
            Enemy enemy = createEnemyFromData(enemyData);
            if (enemy != null) {
                enemies.add(enemy);
            }
        }
    }

    /**
     * Helper to create tower from saved data
     */
    private Tower createTowerFromData(edu.commonwealthu.lastserverstanding.data.models.GameState.TowerData data) {
        PointF pos = new PointF(data.x, data.y);

        // For now, only support FirewallTower (expand later)
        if ("Firewall".equals(data.type)) {
            edu.commonwealthu.lastserverstanding.model.towers.FirewallTower tower = new edu.commonwealthu.lastserverstanding.model.towers.FirewallTower(pos);
            // Upgrade to saved level
            for (int i = 1; i < data.level; i++) {
                tower.upgrade();
            }
            tower.setCorrupted(data.isCorrupted);
            return tower;
        }

        return null;
    }

    /**
     * Helper to create enemy from saved data
     */
    private Enemy createEnemyFromData(edu.commonwealthu.lastserverstanding.data.models.GameState.EnemyData data) {
        // Reconstruct path
        List<PointF> path = new ArrayList<>();
        for (edu.commonwealthu.lastserverstanding.data.models.GameState.EnemyData.Point p : data.path) {
            path.add(new PointF(p.x, p.y));
        }

        // For now, only support DataCrawler (expand later)
        if ("Data Crawler".equals(data.type)) {
            edu.commonwealthu.lastserverstanding.model.enemies.DataCrawler enemy = new edu.commonwealthu.lastserverstanding.model.enemies.DataCrawler(path);
            // Set position and health
            enemy.setPosition(new PointF(data.x, data.y));
            enemy.setCurrentPathIndex(data.currentPathIndex);
            float damage = enemy.getMaxHealth() - data.health;
            enemy.takeDamage(damage);
            return enemy;
        }

        return null;
    }

    /**
     * Try to upgrade tower at grid position
     * @return true if upgrade successful
     */
    public boolean tryUpgradeTowerAt(PointF gridPos) {
        for (Tower tower : towers) {
            PointF towerGrid = worldToGrid(tower.getPosition());
            if (Math.abs(towerGrid.x - gridPos.x) < 0.5f && Math.abs(towerGrid.y - gridPos.y) < 0.5f) {
                int upgradeCost = tower.getUpgradeCost();
                if (resources >= upgradeCost) {
                    boolean upgraded = tower.upgrade();
                    if (upgraded) {
                        resources -= upgradeCost;
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Convert world position to grid position
     */
    private PointF worldToGrid(PointF worldPos) {
        return new PointF(
            (int)(worldPos.x / gridSize),
            (int)(worldPos.y / gridSize)
        );
    }

    /**
     * Trigger emergency alert
     */
    public void triggerEmergencyAlert() {
        // Set flag that GameFragment can check
        // TODO: Implement alert system
        System.out.println("Emergency alert triggered!");
    }

    // Getters and Setters
    public int getCurrentWave() { return currentWave; }
    public int getResources() { return resources; }
    public int getDataCenterHealth() { return dataCenterHealth; }
    public long getScore() { return score; }
    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { this.isPaused = paused; }
    public int getFPS() { return fps; }
    
    public List<Tower> getTowers() { return towers; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Projectile> getProjectiles() { return projectiles; }
    
    public CollisionSystem getCollisionSystem() { return collisionSystem; }
    public Pathfinding getPathfinding() { return pathfinding; }
    public WaveManager getWaveManager() { return waveManager; }
}
