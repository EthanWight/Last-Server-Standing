package edu.commonwealthu.lastserverstanding.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;
import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Core game engine managing all game logic and state
 */
public class GameEngine {
    
    // Game state
    private final List<Tower> towers;
    private final List<Enemy> enemies;
    private final List<Projectile> projectiles;
    
    private int currentWave;
    private int resources;
    private int dataCenterHealth;
    private int previousHealth;
    private long score;
    private boolean isPaused;
    
    // FPS tracking
    private int fps;
    private long lastFpsTime;
    private int frameCount;
    
    // Game systems
    private CollisionSystem collisionSystem;
    private Pathfinding pathfinding;
    private final WaveManager waveManager;
    private GameMap gameMap;

    // World dimensions
    private int worldWidth;
    private int worldHeight;
    private int gridSize;
    
    // Game constants
    private static final int STARTING_RESOURCES = 500;
    private static final int STARTING_HEALTH = 100;

    // Tower icon cache
    private final Map<String, Bitmap> towerIcons;
    private Context context;

    // Settings
    private boolean vibrationEnabled = true;
    private boolean soundEnabled = true;

    /**
     * Constructor
     */
    public GameEngine() {
        towerIcons = new HashMap<>();
        towers = new ArrayList<>();
        enemies = new ArrayList<>();
        projectiles = new ArrayList<>();
        
        currentWave = 0;
        resources = STARTING_RESOURCES;
        dataCenterHealth = STARTING_HEALTH;
        previousHealth = STARTING_HEALTH;
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
     * Set context for loading resources
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Set world dimensions (called by GameView after initialization)
     */
    public void setWorldDimensions(int width, int height, int cellSize) {
        if (width <= 0 || height <= 0 || cellSize <= 0) {
            throw new IllegalArgumentException(
                "World dimensions must be positive: width=" + width +
                ", height=" + height + ", cellSize=" + cellSize
            );
        }

        this.worldWidth = width;
        this.worldHeight = height;
        this.gridSize = cellSize;

        // Reinitialize systems with correct dimensions
        collisionSystem = new CollisionSystem(width, height, cellSize * 2);
        pathfinding = new Pathfinding(width / cellSize, height / cellSize, cellSize);

        // Initialize game map
        int gridWidth = width / cellSize;
        int gridHeight = height / cellSize;
        gameMap = GameMap.createSimpleMap(gridWidth, gridHeight, cellSize);

        // Center the map on screen
        if (gameMap != null) {
            gameMap.centerOnScreen(width, height);
        }
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
        synchronized (towers) {
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
                    synchronized (projectiles) {
                        projectiles.add(projectile);
                    }
                }
            }
        }

        // Update all enemies
        synchronized (enemies) {
            Iterator<Enemy> enemyIterator = enemies.iterator();
            while (enemyIterator.hasNext()) {
                Enemy enemy = enemyIterator.next();
                enemy.update(deltaTime);

                // Check if enemy reached end
                if (enemy.hasReachedEnd()) {
                    dataCenterHealth -= enemy.getDamage();
                    enemyIterator.remove();
                    continue;
                }

                // Remove dead enemies
                if (!enemy.isAlive()) {
                    resources += enemy.getReward();
                    score += enemy.getReward() * 10L;
                    enemyIterator.remove();
                }
            }
        }

        // Update all projectiles
        synchronized (projectiles) {
            Iterator<Projectile> projectileIterator = projectiles.iterator();
            while (projectileIterator.hasNext()) {
                Projectile projectile = projectileIterator.next();
                projectile.update(deltaTime);

                // Remove projectiles that have hit
                if (projectile.hasHit()) {
                    projectileIterator.remove();
                }
            }
        }

        // Check if health decreased and trigger alert
        if (dataCenterHealth < previousHealth) {
            triggerEmergencyAlert();
            previousHealth = dataCenterHealth;
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
        // Draw map first (underneath everything)
        if (gameMap != null) {
            renderMap(canvas, paint);
        }

        // Create defensive copies to avoid ConcurrentModificationException
        // when UI thread modifies collections while render thread is iterating
        List<Tower> towersCopy;
        List<Enemy> enemiesCopy;
        List<Projectile> projectilesCopy;

        synchronized (towers) {
            towersCopy = new ArrayList<>(towers);
        }
        synchronized (enemies) {
            enemiesCopy = new ArrayList<>(enemies);
        }
        synchronized (projectiles) {
            projectilesCopy = new ArrayList<>(projectiles);
        }

        // Draw towers
        paint.setStyle(Paint.Style.FILL);
        for (Tower tower : towersCopy) {
            drawTower(canvas, paint, tower);
        }

        // Draw enemies
        for (Enemy enemy : enemiesCopy) {
            drawEnemy(canvas, paint, enemy);
        }

        // Draw projectiles
        for (Projectile projectile : projectilesCopy) {
            drawProjectile(canvas, paint, projectile);
        }
    }

    /**
     * Render the game map with different colors for different tile types
     */
    private void renderMap(Canvas canvas, Paint paint) {
        paint.setStyle(Paint.Style.FILL);

        float offsetX = gameMap.getOffsetX();
        float offsetY = gameMap.getOffsetY();

        for (int y = 0; y < gameMap.getHeight(); y++) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                TileType tile = gameMap.getTileAt(x, y);
                int color;

                switch (tile) {
                    case PATH:
                        color = Color.parseColor("#4A5568"); // Gray path
                        break;
                    case BUILDABLE:
                        color = Color.parseColor("#2D3748"); // Dark gray walls
                        break;
                    case SPAWN:
                        color = Color.parseColor("#48BB78"); // Green spawn
                        break;
                    case DATACENTER:
                        color = Color.parseColor("#4299E1"); // Blue datacenter
                        break;
                    case EMPTY:
                    default:
                        continue; // Don't draw empty tiles
                }

                paint.setColor(color);
                float left = x * gridSize + offsetX;
                float top = y * gridSize + offsetY;
                canvas.drawRect(left, top, left + gridSize, top + gridSize, paint);
            }
        }
    }
    
    /**
     * Get tower icon resource ID based on type
     */
    private int getTowerIconResource(String towerType) {
        switch (towerType) {
            case "Firewall":
                return R.drawable.ic_tower_firewall;
            case "Honeypot":
                return R.drawable.ic_tower_honeypot;
            case "Jammer":
                return R.drawable.ic_tower_jammer;
            default:
                // Unknown tower type, log warning and use firewall as fallback
                System.err.println("Unknown tower type: " + towerType);
                return R.drawable.ic_tower_firewall;
        }
    }

    /**
     * Load and cache tower icon
     * Converts vector drawables to bitmaps for rendering
     */
    private Bitmap getTowerIcon(String towerType) {
        if (!towerIcons.containsKey(towerType) && context != null) {
            try {
                int resourceId = getTowerIconResource(towerType);

                // Load the drawable (works for both vector and bitmap drawables)
                Drawable drawable = ContextCompat.getDrawable(context, resourceId);

                if (drawable != null) {
                    // Create a bitmap to draw the vector drawable into
                    int size = 48; // Icon size in pixels
                    Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);

                    // Set the bounds and draw
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);

                    towerIcons.put(towerType, bitmap);
                } else {
                    // If drawable is null, cache null to avoid repeated attempts
                    towerIcons.put(towerType, null);
                }
            } catch (Exception e) {
                // If loading fails, cache null to avoid repeated attempts
                towerIcons.put(towerType, null);
            }
        }
        return towerIcons.get(towerType);
    }

    /**
     * Draw a tower with its icon
     */
    private void drawTower(Canvas canvas, Paint paint, Tower tower) {
        if (tower == null) return;
        PointF pos = tower.getPosition();
        if (pos == null) return;

        // Get tower icon
        Bitmap icon = getTowerIcon(tower.getType());

        if (icon != null) {
            // Draw icon centered on position
            int halfSize = icon.getWidth() / 2;
            Rect destRect = new Rect(
                    (int) (pos.x - halfSize),
                    (int) (pos.y - halfSize),
                    (int) (pos.x + halfSize),
                    (int) (pos.y + halfSize)
            );

            // Apply corruption tint if corrupted
            if (tower.isCorrupted()) {
                paint.setColorFilter(new android.graphics.PorterDuffColorFilter(
                        Color.RED, android.graphics.PorterDuff.Mode.MULTIPLY));
            }

            canvas.drawBitmap(icon, null, destRect, paint);

            // Clear color filter
            paint.setColorFilter(null);
        } else {
            // Fallback to circle if icon not available
            paint.setColor(tower.isCorrupted() ? Color.RED : Color.CYAN);
            canvas.drawCircle(pos.x, pos.y, 24, paint);
        }

        // Draw range indicator (subtle)
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(30, 0, 255, 255));
        paint.setStrokeWidth(2);
        canvas.drawCircle(pos.x, pos.y, tower.getRange(), paint);

        // Reset paint to default state
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
    }
    
    /**
     * Draw an enemy (placeholder - will be enhanced later)
     */
    private void drawEnemy(Canvas canvas, Paint paint, Enemy enemy) {
        if (enemy == null) return;
        PointF pos = enemy.getPosition();
        if (pos == null) return;
        
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
        if (projectile == null) return;
        PointF pos = projectile.getPosition();
        if (pos == null) return;
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
     * Check if a tower can be placed at the given position
     * @param position World position to check
     * @return true if placement is valid
     */
    public boolean isValidTowerPlacement(PointF position) {
        if (gameMap == null) return false;

        // Check if position is on a buildable tile
        PointF gridPos = gameMap.worldToGrid(position);
        if (!gameMap.isBuildable(gridPos)) {
            return false;
        }

        // Check spacing from other towers
        synchronized (towers) {
            for (Tower existingTower : towers) {
                float dx = existingTower.getPosition().x - position.x;
                float dy = existingTower.getPosition().y - position.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                // Enforce minimum spacing between towers
                if (distance < Tower.getMinTowerSpacing()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Add a tower to the game
     */
    public boolean addTower(Tower tower) {
        if (tower == null) {
            return false;
        }

        // Check if tower can be placed at this position
        if (!isValidTowerPlacement(tower.getPosition())) {
            System.out.println("Cannot place tower - invalid position or too close to another tower");
            return false;
        }

        if (resources >= tower.getCost()) {
            synchronized (towers) {
                towers.add(tower);
            }
            resources -= tower.getCost();
            return true;
        }
        return false;
    }

    /**
     * Add an enemy to the game
     */
    public void addEnemy(Enemy enemy) {
        synchronized (enemies) {
            enemies.add(enemy);
        }
    }

    /**
     * Get tower at a specific position (grid coordinates)
     * @param worldPos World position to check
     * @return Tower at that position, or null if none found
     */
    public Tower getTowerAt(PointF worldPos) {
        if (gameMap == null) return null;

        PointF gridPos = gameMap.worldToGrid(worldPos);
        synchronized (towers) {
            for (Tower tower : towers) {
                PointF towerGridPos = gameMap.worldToGrid(tower.getPosition());
                if (Math.abs(towerGridPos.x - gridPos.x) < 0.5f &&
                    Math.abs(towerGridPos.y - gridPos.y) < 0.5f) {
                    return tower;
                }
            }
        }
        return null;
    }

    /**
     * Upgrade a tower
     * @param tower Tower to upgrade
     * @return true if upgrade successful
     */
    public boolean upgradeTower(Tower tower) {
        if (tower == null) return false;

        int upgradeCost = tower.getUpgradeCost();
        if (upgradeCost == 0) {
            return false; // Already at max level
        }

        if (resources >= upgradeCost) {
            if (tower.upgrade()) {
                resources -= upgradeCost;
                return true;
            }
        }
        return false;
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
        synchronized (towers) {
            for (Tower tower : towers) {
                state.towers.add(new edu.commonwealthu.lastserverstanding.data.models.GameState.TowerData(
                    tower.getType(),
                    tower.getPosition(),
                    tower.getLevel(),
                    tower.isCorrupted()
                ));
            }
        }

        // Capture enemy data
        synchronized (enemies) {
            for (Enemy enemy : enemies) {
                state.enemies.add(new edu.commonwealthu.lastserverstanding.data.models.GameState.EnemyData(
                    enemy.getType(),
                    enemy.getPosition(),
                    enemy.getHealth(),
                    enemy.getCurrentPathIndex(),
                    enemy.getPath()
                ));
            }
        }

        return state;
    }

    /**
     * Restore game state from saved data
     */
    public void restoreGameState(edu.commonwealthu.lastserverstanding.data.models.GameState state) {
        // Clear current state
        synchronized (towers) {
            towers.clear();
        }
        synchronized (enemies) {
            enemies.clear();
        }
        synchronized (projectiles) {
            projectiles.clear();
        }

        // Restore basic values
        currentWave = state.currentWave;
        resources = state.resources;
        dataCenterHealth = state.dataCenterHealth;
        previousHealth = state.dataCenterHealth; // Initialize to avoid false alert on load
        score = state.score;

        // Restore towers
        synchronized (towers) {
            for (edu.commonwealthu.lastserverstanding.data.models.GameState.TowerData towerData : state.towers) {
                Tower tower = createTowerFromData(towerData);
                if (tower != null) {
                    towers.add(tower);
                }
            }
        }

        // Restore enemies
        synchronized (enemies) {
            for (edu.commonwealthu.lastserverstanding.data.models.GameState.EnemyData enemyData : state.enemies) {
                Enemy enemy = createEnemyFromData(enemyData);
                if (enemy != null) {
                    enemies.add(enemy);
                }
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
        if (gameMap == null) return false;

        synchronized (towers) {
            for (Tower tower : towers) {
                PointF towerGrid = gameMap.worldToGrid(tower.getPosition());
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
        }
        return false;
    }

    /**
     * Convert world position to grid position
     * Delegates to gameMap if available for proper offset handling
     */
    private PointF worldToGrid(PointF worldPos) {
        if (gameMap != null) {
            return gameMap.worldToGrid(worldPos);
        }
        // Fallback for when map is not initialized
        return new PointF(
            (int)(worldPos.x / gridSize),
            (int)(worldPos.y / gridSize)
        );
    }

    /**
     * Trigger emergency alert with haptic and audio feedback
     */
    public void triggerEmergencyAlert() {
        System.out.println("Emergency alert triggered!");

        // Play alert sound if enabled
        if (soundEnabled) {
            playAlertSound();
        }

        // Trigger haptic feedback if enabled
        if (vibrationEnabled && context != null) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Modern vibration pattern for emergency alert
                    // Pattern: short-long-short (SOS-like)
                    long[] timings = {0, 200, 100, 400, 100, 200};
                    int[] amplitudes = {0, 255, 0, 255, 0, 255};
                    VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, -1);
                    vibrator.vibrate(effect);
                } else {
                    // Fallback for older devices
                    long[] pattern = {0, 200, 100, 400, 100, 200};
                    vibrator.vibrate(pattern, -1);
                }
            }
        }
    }

    /**
     * Play emergency alert sound using ToneGenerator
     */
    private void playAlertSound() {
        new Thread(() -> {
            try {
                android.media.ToneGenerator toneGen = new android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100);
                // Play a series of urgent tones (high pitched beeps)
                toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                Thread.sleep(300);
                toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400);
                Thread.sleep(500);
                toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                Thread.sleep(300);
                toneGen.release();
            } catch (Exception e) {
                // Silently fail if sound cannot be played
                System.err.println("Failed to play alert sound: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Set vibration enabled state from settings
     */
    public void setVibrationEnabled(boolean enabled) {
        this.vibrationEnabled = enabled;
    }

    /**
     * Set sound enabled state from settings
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
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

    public Pathfinding getPathfinding() { return pathfinding; }
    public WaveManager getWaveManager() { return waveManager; }
    public GameMap getGameMap() { return gameMap; }
}
